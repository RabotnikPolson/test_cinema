import asyncio
import os
from processing.tag_preservator import TagPreservator
from processing.smart_chunker import SmartChunker, SubtitleEntry
from services.checkpoint_repository import CheckpointRepository
from schemas.checkpoint import TranslationJob, ChunkResult
import uuid
from datetime import datetime

async def run_sandbox():
    print("=== SANDBOX: Phase 1 & 2 ===\n")

    # 1. Simulate Subtitles
    print("1. Generating Fake Subtitles...")
    fake_subtitles = [
        SubtitleEntry(1, 0.0, 2.0, "<i>Привет!</i>"),
        SubtitleEntry(2, 2.5, 4.0, "- Как дела?\n- Нормально."),
        SubtitleEntry(3, 10.0, 12.0, "♪ Music playing ♪"),
        SubtitleEntry(4, 12.5, 15.0, "Я думаю, нам нужно..."),
        SubtitleEntry(5, 15.1, 18.0, "...поговорить о <b>важном</b>."),
        SubtitleEntry(6, 18.5, 20.0, "Хорошо."),
    ]
    for s in fake_subtitles:
        print(f"[{s.start_time}-{s.end_time}] {s.text}")
    print()

    # 2. Smart Chunking
    print("2. Smart Chunking (gap=4.0s, min=2, max=4)...")
    chunker = SmartChunker(gap_threshold_sec=4.0, min_lines_per_chunk=2, max_lines_per_chunk=4)
    chunks = chunker.chunk(fake_subtitles)
    for i, c in enumerate(chunks):
        print(f"Chunk {i}: {len(c)} lines")
        for s in c:
             print(f"  -> {s.text}")
    print()

    # 3. Tag Preservation
    print("3. Tag Preservation Pipeline...")
    preservator = TagPreservator()
    chunk_lines = [s.text for s in chunks[0]]
    print("Original Chunk 0:")
    for l in chunk_lines:
        print(f"  {l}")

    clean_lines, tag_map = preservator.strip(chunk_lines)
    print("\nStripped (Sent to LLM):")
    for l in clean_lines:
        print(f"  {l}")
    print(f"Tag Map: {tag_map}")

    # Fake LLM Translation (translating to Kazakh with placeholders preserved + hallucinated tag <99>)
    print("\nFake LLM Output (Translated):")
    fake_translation = [
        "<0>Сәлем!<1>",
        "<2>Қалайсың?<3><4>Қалыпты.<99>"
    ]
    for l in fake_translation:
         print(f"  {l}")

    restored_lines = preservator.restore(fake_translation, tag_map)
    print("\nRestored (Final Output):")
    for l in restored_lines:
        print(f"  {l}")
    print()

    # 4. SQLite Checkpoint Transactions
    print("4. SQLite Checkpointing...")
    db_path = "test_sandbox.db"
    if os.path.exists(db_path):
        os.remove(db_path)
    
    repo = CheckpointRepository(db_path)
    await repo.initialize()
    print("DB Initialized.")

    job = TranslationJob(
        job_id=str(uuid.uuid4()),
        movie_id=999,
        input_path="fake.vtt",
        output_path="fake_kk.vtt",
        movie_title="Sandbox Movie",
        total_lines=len(fake_subtitles),
        chunk_size=4
    )
    await repo.create_job(job)
    print(f"Job {job.job_id} created.")

    # Save Chunk 0
    chunk_res = ChunkResult(
        chunk_index=0,
        original_lines=chunk_lines,
        translated_lines=restored_lines,
        model_used="fake-model",
        provider="fake-provider",
        attempt=1
    )
    await repo.save_chunk(job.job_id, chunk_res)
    print("Chunk 0 saved. Transaction successful.")

    # Verify state
    active_job = await repo.get_active_job(job.movie_id)
    print(f"Verified Active Job next_chunk_index: {active_job.next_chunk_index}")
    print("\nSandbox Execution Completed Successfully.")

if __name__ == "__main__":
    asyncio.run(run_sandbox())
