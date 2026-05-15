import asyncio
import logging
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from sqlalchemy.future import select
from core.database import AsyncSessionLocal
from models.db_models import MovieSubtitle, SubtitleChunk
from services.vectorizer import SubtitleVectorizer

logger = logging.getLogger(__name__)

# Initialize the vectorizer once to keep the model loaded in memory
vectorizer = SubtitleVectorizer()

async def _mock_download_from_s3(s3_path: str) -> str:
    """Mock for downloading VTT content from S3."""
    # Simulate network delay
    await asyncio.sleep(0.5)
    return """WEBVTT

1
00:00:01.000 --> 00:00:04.000
<i>Привет, как дела?</i>

2
00:00:04.500 --> 00:00:07.200
Всё отлично. Что сегодня делаем?

3
00:00:08.000 --> 00:00:11.000
<c.colorE5E5E5>Мы идем на охоту.</c> 
Это будет весело.
"""

async def process_subtitles_job():
    logger.info("[Worker] Waking up to process unvectorized subtitles...")
    
    async with AsyncSessionLocal() as session:
        # Find all subtitles that are downloaded but not yet vectorized
        stmt = select(MovieSubtitle).where(
            MovieSubtitle.is_downloaded == True,
            MovieSubtitle.is_vectorized_for_ai == False
        )
        result = await session.execute(stmt)
        subtitles = result.scalars().all()
        
        if not subtitles:
            logger.info("[Worker] No subtitles to vectorize. Going back to sleep.")
            return

        logger.info(f"[Worker] Found {len(subtitles)} subtitles to process.")
        
        for subtitle in subtitles:
            try:
                logger.info(f"[Worker] Processing subtitle ID {subtitle.id} for movie ID {subtitle.movie_id}")
                
                # 1. Download VTT content (Mocked for now)
                vtt_content = await _mock_download_from_s3(subtitle.s3_path)
                
                # 2. Parse and Clean
                clean_text = vectorizer.parse_vtt(vtt_content)
                if not clean_text:
                    logger.warning(f"[Worker] Subtitle ID {subtitle.id} produced empty text. Skipping.")
                    subtitle.is_vectorized_for_ai = True
                    await session.commit()
                    continue
                
                # 3. Chunk
                chunks_text = vectorizer.chunk_text(clean_text, chunk_size=500, overlap=50)
                
                # 4. Vectorize and prepare DB models
                db_chunks = []
                for idx, text_chunk in enumerate(chunks_text):
                    # Offload to thread pool to not block the event loop
                    embedding_vector = await vectorizer.vectorize(text_chunk)
                    
                    db_chunk = SubtitleChunk(
                        subtitle_id=subtitle.id,
                        chunk_text=text_chunk,
                        chunk_index=idx,
                        embedding=embedding_vector
                    )
                    db_chunks.append(db_chunk)
                
                # 5. Save chunks to DB
                session.add_all(db_chunks)
                
                # 6. Mark subtitle as vectorized
                subtitle.is_vectorized_for_ai = True
                
                # Commit exactly one subtitle at a time
                await session.commit()
                logger.info(f"[Worker] Successfully vectorized and saved {len(db_chunks)} chunks for subtitle ID {subtitle.id}")
                
            except Exception as e:
                # Rollback this specific subtitle's transaction if error occurs
                await session.rollback()
                logger.error(f"[Worker] Failed to process subtitle ID {subtitle.id}: {e}", exc_info=True)
                # Continue to the next subtitle, we do NOT crash the whole worker

def start_worker():
    """Configures and starts the APScheduler background worker."""
    scheduler = AsyncIOScheduler()
    # Run every 5 minutes in production, but here we run it more frequently for context
    scheduler.add_job(process_subtitles_job, 'interval', minutes=5, id='vectorization_job')
    scheduler.start()
    logger.info("[Worker] APScheduler started.")
    return scheduler
