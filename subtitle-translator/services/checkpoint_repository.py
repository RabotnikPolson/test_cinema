import aiosqlite
import json
from typing import Optional, List
from datetime import datetime
from schemas.checkpoint import TranslationJob, ChunkResult, PendingWebhook
import logging

logger = logging.getLogger(__name__)

class CheckpointRepository:
    def __init__(self, db_path: str):
        self._db_path = db_path

    async def initialize(self):
        """CREATE TABLE IF NOT EXISTS at startup. Enables WAL mode."""
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute("PRAGMA journal_mode=WAL;")
            
            await db.execute("""
                CREATE TABLE IF NOT EXISTS translation_jobs (
                    job_id            TEXT PRIMARY KEY,
                    movie_id          INTEGER NOT NULL,
                    input_path        TEXT NOT NULL,
                    output_path       TEXT NOT NULL,
                    movie_title       TEXT NOT NULL,
                    source_language   TEXT DEFAULT 'ru',
                    total_lines       INTEGER NOT NULL,
                    chunk_size        INTEGER NOT NULL,
                    next_chunk_index  INTEGER DEFAULT 0,
                    status            TEXT DEFAULT 'in_progress',
                    openai_batch_id   TEXT,
                    batch_chunk_mapping TEXT,
                    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """)
            await db.execute("CREATE INDEX IF NOT EXISTS idx_jobs_movie ON translation_jobs(movie_id)")
            await db.execute("CREATE INDEX IF NOT EXISTS idx_jobs_batch ON translation_jobs(openai_batch_id)")

            await db.execute("""
                CREATE TABLE IF NOT EXISTS chunk_results (
                    id                INTEGER PRIMARY KEY AUTOINCREMENT,
                    job_id            TEXT NOT NULL REFERENCES translation_jobs(job_id),
                    chunk_index       INTEGER NOT NULL,
                    original_lines    TEXT NOT NULL,
                    translated_lines  TEXT NOT NULL,
                    model_used        TEXT NOT NULL,
                    provider          TEXT NOT NULL,
                    attempt           INTEGER DEFAULT 1,
                    translated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(job_id, chunk_index)
                )
            """)

            await db.execute("""
                CREATE TABLE IF NOT EXISTS pending_webhooks (
                    id                INTEGER PRIMARY KEY AUTOINCREMENT,
                    payload           TEXT NOT NULL,
                    attempts          INTEGER DEFAULT 0,
                    max_attempts      INTEGER DEFAULT 15,
                    next_retry_at     TIMESTAMP NOT NULL,
                    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_error        TEXT
                )
            """)
            await db.execute("CREATE INDEX IF NOT EXISTS idx_webhooks_retry ON pending_webhooks(next_retry_at)")
            await db.commit()
            logger.info(f"Initialized SQLite Checkpoint DB at {self._db_path}")

    async def get_active_job(self, movie_id: int) -> Optional[TranslationJob]:
        async with aiosqlite.connect(self._db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                "SELECT * FROM translation_jobs WHERE movie_id = ? AND status IN ('in_progress', 'awaiting_batch')",
                (movie_id,)
            ) as cursor:
                row = await cursor.fetchone()
                if not row:
                    return None
                return self._row_to_job(row)

    async def get_job_by_batch_id(self, batch_id: str) -> Optional[TranslationJob]:
        async with aiosqlite.connect(self._db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                "SELECT * FROM translation_jobs WHERE openai_batch_id = ?",
                (batch_id,)
            ) as cursor:
                row = await cursor.fetchone()
                if not row:
                    return None
                return self._row_to_job(row)

    async def get_stale_batch_jobs(self, stale_minutes: int) -> List[TranslationJob]:
        async with aiosqlite.connect(self._db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                "SELECT * FROM translation_jobs WHERE status = 'awaiting_batch' AND updated_at < datetime('now', ?)",
                (f'-{stale_minutes} minutes',)
            ) as cursor:
                rows = await cursor.fetchall()
                return [self._row_to_job(row) for row in rows]

    def _row_to_job(self, row: aiosqlite.Row) -> TranslationJob:
        return TranslationJob(
            job_id=row["job_id"],
            movie_id=row["movie_id"],
            input_path=row["input_path"],
            output_path=row["output_path"],
            movie_title=row["movie_title"],
            source_language=row["source_language"],
            total_lines=row["total_lines"],
            chunk_size=row["chunk_size"],
            next_chunk_index=row["next_chunk_index"],
            status=row["status"],
            openai_batch_id=row["openai_batch_id"],
            batch_chunk_mapping=json.loads(row["batch_chunk_mapping"]) if row["batch_chunk_mapping"] else None,
            created_at=datetime.fromisoformat(row["created_at"]) if row["created_at"] else None,
            updated_at=datetime.fromisoformat(row["updated_at"]) if row["updated_at"] else None
        )

    async def create_job(self, job: TranslationJob) -> str:
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute(
                """INSERT INTO translation_jobs 
                   (job_id, movie_id, input_path, output_path, movie_title, source_language, total_lines, chunk_size, status)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (job.job_id, job.movie_id, job.input_path, job.output_path, job.movie_title, 
                 job.source_language, job.total_lines, job.chunk_size, job.status)
            )
            await db.commit()
            return job.job_id

    async def save_chunk(self, job_id: str, chunk: ChunkResult) -> None:
        """INSERT chunk + UPDATE next_chunk_index in a single transaction."""
        async with aiosqlite.connect(self._db_path) as db:
            try:
                await db.execute("BEGIN TRANSACTION")
                await db.execute(
                    """INSERT INTO chunk_results 
                       (job_id, chunk_index, original_lines, translated_lines, model_used, provider, attempt)
                       VALUES (?, ?, ?, ?, ?, ?, ?)""",
                    (job_id, chunk.chunk_index, json.dumps(chunk.original_lines, ensure_ascii=False),
                     json.dumps(chunk.translated_lines, ensure_ascii=False), chunk.model_used, chunk.provider, chunk.attempt)
                )
                await db.execute(
                    "UPDATE translation_jobs SET next_chunk_index = ?, updated_at = CURRENT_TIMESTAMP WHERE job_id = ?",
                    (chunk.chunk_index + 1, job_id)
                )
                await db.commit()
            except Exception as e:
                await db.rollback()
                raise e

    async def update_job_status(self, job_id: str, status: str) -> None:
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute("UPDATE translation_jobs SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE job_id = ?", (status, job_id))
            await db.commit()

    async def delete_job(self, job_id: str) -> None:
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute("DELETE FROM chunk_results WHERE job_id = ?", (job_id,))
            await db.execute("DELETE FROM translation_jobs WHERE job_id = ?", (job_id,))
            await db.commit()

    async def close(self):
        # aiosqlite creates connections on the fly per async with, no persistent connection pool managed here by default
        pass
