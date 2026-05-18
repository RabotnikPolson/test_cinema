import asyncio
import logging
import aiofiles
from sqlalchemy.future import select
from core.database import AsyncSessionLocal
from models.db_models import MovieSubtitle, SubtitleChunk
from services.vectorizer import SubtitleVectorizer

logger = logging.getLogger(__name__)

# Initialize the vectorizer once to keep the model loaded in memory
vectorizer = SubtitleVectorizer()


async def _read_vtt_from_disk(local_path: str) -> str:
    """Read VTT content from the local filesystem."""
    async with aiofiles.open(local_path, mode="r", encoding="utf-8") as f:
        return await f.read()


async def process_subtitles_job():
    """Manually triggered job: vectorize downloaded subtitles."""
    logger.info("[Worker] Processing unvectorized subtitles...")

    async with AsyncSessionLocal() as session:
        stmt = select(MovieSubtitle).where(
            MovieSubtitle.is_downloaded == True,
            MovieSubtitle.local_path.isnot(None),
        )
        result = await session.execute(stmt)
        subtitles = result.scalars().all()

        if not subtitles:
            logger.info("[Worker] No subtitles to vectorize. Done.")
            return

        logger.info(f"[Worker] Found {len(subtitles)} subtitles to process.")

        for subtitle in subtitles:
            try:
                logger.info(f"[Worker] Processing subtitle ID {subtitle.id} for movie ID {subtitle.movie_id}")

                # 1. Read VTT content from local storage
                vtt_content = await _read_vtt_from_disk(subtitle.local_path)

                # 2. Parse and Clean
                clean_text = vectorizer.parse_vtt(vtt_content)
                if not clean_text:
                    logger.warning(f"[Worker] Subtitle ID {subtitle.id} produced empty text. Skipping.")
                    await session.commit()
                    continue

                # 3. Chunk
                chunks_text = vectorizer.chunk_text(clean_text, chunk_size=500, overlap=50)

                # 4. Vectorize and prepare DB models
                db_chunks = []
                for idx, text_chunk in enumerate(chunks_text):
                    embedding_vector = await vectorizer.vectorize(text_chunk)

                    db_chunk = SubtitleChunk(
                        subtitle_id=subtitle.id,
                        chunk_text=text_chunk,
                        chunk_index=idx,
                        embedding=embedding_vector,
                    )
                    db_chunks.append(db_chunk)

                # 5. Save chunks to DB
                session.add_all(db_chunks)

                await session.commit()
                logger.info(f"[Worker] Successfully vectorized {len(db_chunks)} chunks for subtitle ID {subtitle.id}")

            except Exception as e:
                await session.rollback()
                logger.error(f"[Worker] Failed to process subtitle ID {subtitle.id}: {e}", exc_info=True)
