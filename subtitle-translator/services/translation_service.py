import json
import logging
import os
import re
import uuid
from collections import deque
from typing import Callable, List, Optional

from schemas.translation import TranslateRequest
from schemas.webhook import WebhookPayload
from services.checkpoint_repository import CheckpointRepository
from schemas.checkpoint import TranslationJob, ChunkResult
from processing.smart_chunker import SmartChunker, SubtitleEntry
from processing.tag_preservator import TagPreservator
from services.fallback_router import FallbackRouter, ExhaustedProvidersError
from providers.openai_provider import OpenAIProvider

logger = logging.getLogger(__name__)


class TranslationService:
    """
    Core orchestrator.

    Architecture:
      1. FallbackRouter contains ONLY Gemini providers (sync, per-chunk).
      2. When ALL Gemini models are exhausted on any chunk,
         ALL remaining chunks are submitted to OpenAI Batch API at once.
      3. Worker is then released. Results arrive via webhook or BatchReconciler.
    """

    CONTEXT_WINDOW_LINES = 10

    def __init__(
        self,
        checkpoint_repo: CheckpointRepository,
        smart_chunker: SmartChunker,
        tag_preservator: TagPreservator,
        gemini_provider_factory: Callable,
        openai_provider_factory: Callable,
        webhook_client,
        settings,
    ):
        self._repo = checkpoint_repo
        self._smart_chunker = smart_chunker
        self._tag_preservator = tag_preservator
        self._gemini_provider_factory = gemini_provider_factory
        self._openai_provider_factory = openai_provider_factory
        self._webhook_client = webhook_client
        self._settings = settings

    # ═══════════════════════════════════════════
    #  SUBTITLE PARSING (built-in VTT/SRT)
    # ═══════════════════════════════════════════

    _TIMESTAMP_RE = re.compile(
        r"(\d{1,2}):(\d{2}):(\d{2})[.,](\d{3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[.,](\d{3})"
    )

    @classmethod
    def _ts_to_seconds(cls, h: str, m: str, s: str, ms: str) -> float:
        return int(h) * 3600 + int(m) * 60 + int(s) + int(ms) / 1000

    def _parse_subtitle_entries(self, input_path: str) -> List[SubtitleEntry]:
        """
        Parse a .vtt or .srt file into SubtitleEntry objects.
        Built-in parser — no external dependencies.
        """
        if not os.path.exists(input_path):
            raise FileNotFoundError(f"Subtitle file not found: {input_path}")

        with open(input_path, "r", encoding="utf-8") as f:
            content = f.read()

        entries: List[SubtitleEntry] = []
        blocks = re.split(r"\n\s*\n", content.strip())

        for block in blocks:
            lines = block.strip().splitlines()
            if not lines:
                continue

            ts_line_idx = None
            for idx, line in enumerate(lines):
                if self._TIMESTAMP_RE.search(line):
                    ts_line_idx = idx
                    break

            if ts_line_idx is None:
                continue

            match = self._TIMESTAMP_RE.search(lines[ts_line_idx])
            start = self._ts_to_seconds(
                match.group(1), match.group(2), match.group(3), match.group(4)
            )
            end = self._ts_to_seconds(
                match.group(5), match.group(6), match.group(7), match.group(8)
            )

            text = "\n".join(lines[ts_line_idx + 1 :]).strip()
            if not text:
                continue

            entries.append(
                SubtitleEntry(
                    index=len(entries), start_time=start, end_time=end, text=text,
                )
            )

        if not entries:
            raise ValueError(f"No subtitle entries found in {input_path}")

        return entries

    # ═══════════════════════════════════════════
    #  FILE ASSEMBLY
    # ═══════════════════════════════════════════

    def _assemble_file(
        self,
        chunk_results: List[ChunkResult],
        output_path: str,
        original_entries: List[SubtitleEntry],
    ) -> None:
        """Assemble translated lines back into a .vtt file with original timecodes."""
        flat_translated = []
        for res in sorted(chunk_results, key=lambda x: x.chunk_index):
            flat_translated.extend(res.translated_lines)

        output_dir = os.path.dirname(output_path)
        if output_dir and not os.path.exists(output_dir):
            os.makedirs(output_dir, exist_ok=True)

        with open(output_path, "w", encoding="utf-8") as f:
            f.write("WEBVTT\n\n")
            for i, entry in enumerate(original_entries):
                text = flat_translated[i] if i < len(flat_translated) else entry.text
                f.write(
                    f"{self._fmt_time(entry.start_time)} --> "
                    f"{self._fmt_time(entry.end_time)}\n{text}\n\n"
                )

    @staticmethod
    def _fmt_time(seconds: float) -> str:
        h = int(seconds // 3600)
        m = int((seconds % 3600) // 60)
        s = seconds % 60
        return f"{h:02d}:{m:02d}:{s:06.3f}"

    # ═══════════════════════════════════════════
    #  CONTEXT BUILDER
    # ═══════════════════════════════════════════

    def _build_context(self, context_buffer: deque) -> str:
        if not context_buffer:
            return ""
        return "\n".join(context_buffer)

    # ═══════════════════════════════════════════
    #  MAIN TRANSLATION LOOP
    # ═══════════════════════════════════════════

    async def translate(self, request: TranslateRequest) -> dict:
        logger.info(f"Starting translation for movie {request.movie_id}")

        # 1. Check for existing active job
        job = await self._repo.get_active_job(request.movie_id)
        if job and job.status == "awaiting_batch":
            logger.info(f"Job for movie {request.movie_id} is awaiting_batch.")
            return {"status": "awaiting_batch", "batch_id": job.openai_batch_id}

        # 2. Parse subtitles
        entries = self._parse_subtitle_entries(request.input_path)

        # 3. Smart Chunk
        chunks = self._smart_chunker.chunk(entries)
        logger.info(f"Parsed {len(entries)} entries into {len(chunks)} chunks.")

        # 4. Create or resume job
        if not job:
            job = TranslationJob(
                job_id=str(uuid.uuid4()),
                movie_id=request.movie_id,
                input_path=request.input_path,
                output_path=request.output_path,
                movie_title=request.movie_title,
                source_language=request.source_language,
                total_lines=len(entries),
                chunk_size=len(chunks),
            )
            await self._repo.create_job(job)
            logger.info(f"Created new job {job.job_id}")

        # 5. Build Gemini-only chain for FallbackRouter
        gemini_providers = self._gemini_provider_factory()
        router = FallbackRouter(gemini_providers)

        # 6. Rolling context buffer
        context_buffer: deque = deque(maxlen=self.CONTEXT_WINDOW_LINES)

        if job.next_chunk_index > 0:
            existing_chunks = await self._repo.get_chunks_for_job(job.job_id)
            for ec in existing_chunks:
                for line in ec.translated_lines:
                    context_buffer.append(line)

        # 7. Translation loop (Gemini only)
        for i in range(job.next_chunk_index, len(chunks)):
            chunk = chunks[i]
            lines = [entry.text for entry in chunk]
            context_str = self._build_context(context_buffer)

            try:
                result = await router.translate_chunk(
                    lines, context_str, job.movie_title, i,
                    source_language=request.source_language,
                    genre="general",
                    glossary={}
                )
                await self._repo.save_chunk(job.job_id, result)
                logger.info(
                    f"Chunk {i}/{len(chunks) - 1} saved. "
                    f"Model: {result.model_used}, Attempt: {result.attempt}"
                )
                for line in result.translated_lines:
                    context_buffer.append(line)

            except ExhaustedProvidersError:
                # ALL Gemini models failed → submit ALL remaining to OpenAI Batch
                logger.warning(
                    f"All Gemini models exhausted at chunk {i}/{len(chunks) - 1}. "
                    f"Falling back to OpenAI Batch for {len(chunks) - i} remaining chunks."
                )
                return await self._submit_batch_fallback(
                    job, chunks, i, context_buffer
                )

        # 8. All chunks completed synchronously — assemble file
        all_results = await self._repo.get_chunks_for_job(job.job_id)
        self._assemble_file(all_results, job.output_path, entries)
        await self._repo.update_job_status(job.job_id, "completed")

        await self._webhook_client.send_webhook(
            WebhookPayload(
                movie_id=job.movie_id,
                language="kk",
                status="success",
                output_path=job.output_path,
                lines_translated=job.total_lines,
            )
        )
        logger.info(f"Job {job.job_id} completed successfully!")
        return {"status": "success"}

    # ═══════════════════════════════════════════
    #  BATCH FALLBACK (OpenAI only)
    # ═══════════════════════════════════════════

    async def _submit_batch_fallback(
        self,
        job: TranslationJob,
        chunks: list,
        start_index: int,
        context_buffer: deque,
    ) -> dict:
        """
        Collect ALL remaining chunks (from start_index to end),
        strip tags, build one .jsonl, submit to OpenAI Batch API,
        save batch_id to DB, and release the worker.
        """
        openai_provider: Optional[OpenAIProvider] = self._openai_provider_factory()

        if not openai_provider:
            logger.error("No OpenAI provider configured for batch fallback.")
            await self._repo.update_job_status(job.job_id, "exhausted")
            await self._webhook_client.send_webhook(
                WebhookPayload(
                    movie_id=job.movie_id,
                    language="kk",
                    status="partial",
                    error_message="All Gemini exhausted, no OpenAI key configured",
                )
            )
            return {"status": "exhausted", "error": "No OpenAI provider"}

        # Build batch data for all remaining chunks
        chunks_data = []
        
        # The context buffer holds the LAST TRANSLATED lines (Kazakh) from the sync phase.
        # We can only realistically provide this context to the very first chunk in the batch.
        # For subsequent chunks, we have no translations yet, so context must be disabled
        # to avoid polluting the prompt with Russian source text.
        initial_context_str = self._build_context(context_buffer)

        for i in range(start_index, len(chunks)):
            chunk = chunks[i]
            lines = [entry.text for entry in chunk]
            clean_lines, _ = self._tag_preservator.strip(lines)
            
            # Use real context only for the first chunk in the batch
            context_str = initial_context_str if i == start_index else ""

            chunks_data.append((i, clean_lines, context_str, job.movie_title))

        batch_id, chunk_mapping = await openai_provider.submit_batch_multi(
            chunks_data
        )

        await self._repo.set_batch_status(job.job_id, batch_id, chunk_mapping)

        logger.info(
            f"OpenAI Batch {batch_id} submitted with {len(chunks_data)} chunks "
            f"for job {job.job_id}. Worker released."
        )
        return {"status": "awaiting_batch", "batch_id": batch_id}
