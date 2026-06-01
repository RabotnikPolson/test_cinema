import aiosqlite
import json
from typing import Optional, List
from datetime import datetime
from schemas.checkpoint import TranslationJob, ChunkResult, PendingWebhook
import logging

logger = logging.getLogger(__name__)


class CheckpointRepository:
    """
    Single source of truth for all SQLite operations.
    No other module should import aiosqlite or write raw SQL.
    Uses WAL mode for concurrent read safety.
    All datetime values use SQLite's datetime('now') for format consistency.
    """

    def __init__(self, db_path: str):
        self._db_path = db_path

    # ═══════════════════════════════════════════
    #  INITIALIZATION
    # ═══════════════════════════════════════════

    async def initialize(self):
        """Create tables if not exist. Enable WAL mode."""
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
                    created_at        TIMESTAMP DEFAULT (datetime('now')),
                    updated_at        TIMESTAMP DEFAULT (datetime('now'))
                )
            """)
            await db.execute(
                "CREATE INDEX IF NOT EXISTS idx_jobs_movie ON translation_jobs(movie_id)"
            )
            await db.execute(
                "CREATE INDEX IF NOT EXISTS idx_jobs_batch ON translation_jobs(openai_batch_id)"
            )

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
                    translated_at     TIMESTAMP DEFAULT (datetime('now')),
                    UNIQUE(job_id, chunk_index)
                )
            """)

            await db.execute("""
                CREATE TABLE IF NOT EXISTS pending_webhooks (
                    id                INTEGER PRIMARY KEY AUTOINCREMENT,
                    payload           TEXT NOT NULL,
                    status            TEXT DEFAULT 'pending',
                    attempts          INTEGER DEFAULT 0,
                    max_attempts      INTEGER DEFAULT 15,
                    next_retry_at     TIMESTAMP NOT NULL DEFAULT (datetime('now')),
                    created_at        TIMESTAMP DEFAULT (datetime('now')),
                    last_error        TEXT
                )
            """)
            await db.execute(
                "CREATE INDEX IF NOT EXISTS idx_webhooks_retry ON pending_webhooks(next_retry_at)"
            )
            await db.commit()
            logger.info(f"Initialized SQLite Checkpoint DB at {self._db_path}")

    # ═══════════════════════════════════════════
    #  TRANSLATION JOBS
    # ═══════════════════════════════════════════

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
            batch_chunk_mapping=(
                json.loads(row["batch_chunk_mapping"])
                if row["batch_chunk_mapping"]
                else None
            ),
            created_at=(
                datetime.fromisoformat(row["created_at"])
                if row["created_at"]
                else None
            ),
            updated_at=(
                datetime.fromisoformat(row["updated_at"])
                if row["updated_at"]
                else None
            ),
        )

    async def get_active_job(self, movie_id: int) -> Optional[TranslationJob]:
        async with aiosqlite.connect(self._db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                "SELECT * FROM translation_jobs WHERE movie_id = ? AND status IN ('in_progress', 'awaiting_batch')",
                (movie_id,),
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
                (batch_id,),
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
                (f"-{stale_minutes} minutes",),
            ) as cursor:
                rows = await cursor.fetchall()
                return [self._row_to_job(row) for row in rows]

    async def create_job(self, job: TranslationJob) -> str:
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute(
                """INSERT INTO translation_jobs
                   (job_id, movie_id, input_path, output_path, movie_title,
                    source_language, total_lines, chunk_size, status)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (
                    job.job_id, job.movie_id, job.input_path, job.output_path,
                    job.movie_title, job.source_language, job.total_lines,
                    job.chunk_size, job.status,
                ),
            )
            await db.commit()
            return job.job_id

    async def save_chunk(self, job_id: str, chunk: ChunkResult) -> None:
        """INSERT chunk result + UPDATE next_chunk_index in a single atomic transaction."""
        async with aiosqlite.connect(self._db_path) as db:
            try:
                await db.execute("BEGIN TRANSACTION")
                await db.execute(
                    """INSERT OR REPLACE INTO chunk_results
                       (job_id, chunk_index, original_lines, translated_lines,
                        model_used, provider, attempt)
                       VALUES (?, ?, ?, ?, ?, ?, ?)""",
                    (
                        job_id, chunk.chunk_index,
                        json.dumps(chunk.original_lines, ensure_ascii=False),
                        json.dumps(chunk.translated_lines, ensure_ascii=False),
                        chunk.model_used, chunk.provider, chunk.attempt,
                    ),
                )
                await db.execute(
                    "UPDATE translation_jobs SET next_chunk_index = ?, updated_at = datetime('now') WHERE job_id = ?",
                    (chunk.chunk_index + 1, job_id),
                )
                await db.commit()
            except Exception:
                await db.rollback()
                raise

    async def set_batch_status(
        self, job_id: str, batch_id: str, chunk_mapping: dict
    ) -> None:
        """Atomically set job to awaiting_batch with batch metadata."""
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute(
                """UPDATE translation_jobs
                   SET status = 'awaiting_batch',
                       openai_batch_id = ?,
                       batch_chunk_mapping = ?,
                       updated_at = datetime('now')
                   WHERE job_id = ?""",
                (batch_id, json.dumps(chunk_mapping), job_id),
            )
            await db.commit()

    async def update_job_status(self, job_id: str, status: str) -> None:
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute(
                "UPDATE translation_jobs SET status = ?, updated_at = datetime('now') WHERE job_id = ?",
                (status, job_id),
            )
            await db.commit()

    async def get_chunks_for_job(self, job_id: str) -> List[ChunkResult]:
        """Retrieve all saved chunk results for a job, ordered by chunk_index."""
        async with aiosqlite.connect(self._db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                "SELECT * FROM chunk_results WHERE job_id = ? ORDER BY chunk_index",
                (job_id,),
            ) as cursor:
                rows = await cursor.fetchall()
                return [
                    ChunkResult(
                        chunk_index=row["chunk_index"],
                        original_lines=json.loads(row["original_lines"]),
                        translated_lines=json.loads(row["translated_lines"]),
                        model_used=row["model_used"],
                        provider=row["provider"],
                        attempt=row["attempt"],
                    )
                    for row in rows
                ]

    async def get_job_status(self, job_id: str) -> str | None:
        async with aiosqlite.connect(self._db_path) as db:
            async with db.execute(
                "SELECT status FROM translation_jobs WHERE job_id = ?", (job_id,)
            ) as cursor:
                row = await cursor.fetchone()
                return row[0] if row else None

    async def cancel_active_job(self, movie_id: int) -> bool:
        """Mark in_progress/awaiting_batch job as cancelled. Returns True if found."""
        async with aiosqlite.connect(self._db_path) as db:
            cursor = await db.execute(
                """UPDATE translation_jobs SET status = 'cancelled', updated_at = datetime('now')
                   WHERE movie_id = ? AND status IN ('in_progress', 'awaiting_batch')""",
                (movie_id,),
            )
            await db.commit()
            return cursor.rowcount > 0

    async def delete_job(self, job_id: str) -> None:
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute("DELETE FROM chunk_results WHERE job_id = ?", (job_id,))
            await db.execute("DELETE FROM translation_jobs WHERE job_id = ?", (job_id,))
            await db.commit()

    # ═══════════════════════════════════════════
    #  PENDING WEBHOOKS (Persistent Queue)
    # ═══════════════════════════════════════════

    async def enqueue_webhook(self, payload_json: str) -> None:
        """Persist a failed webhook payload for later retry."""
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute(
                "INSERT INTO pending_webhooks (payload, next_retry_at) VALUES (?, datetime('now'))",
                (payload_json,),
            )
            await db.commit()

    async def get_due_webhooks(self) -> List[PendingWebhook]:
        """Get all pending webhooks whose retry time has arrived."""
        async with aiosqlite.connect(self._db_path) as db:
            db.row_factory = aiosqlite.Row
            async with db.execute(
                "SELECT * FROM pending_webhooks WHERE status = 'pending' AND next_retry_at <= datetime('now')"
            ) as cursor:
                rows = await cursor.fetchall()
                return [
                    PendingWebhook(
                        id=row["id"],
                        payload=row["payload"],
                        attempts=row["attempts"],
                        max_attempts=row["max_attempts"],
                        next_retry_at=datetime.fromisoformat(row["next_retry_at"]),
                        created_at=(
                            datetime.fromisoformat(row["created_at"])
                            if row["created_at"]
                            else None
                        ),
                        last_error=row["last_error"],
                    )
                    for row in rows
                ]

    async def delete_webhook(self, webhook_id: int) -> None:
        """Delete a successfully delivered webhook."""
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute("DELETE FROM pending_webhooks WHERE id = ?", (webhook_id,))
            await db.commit()

    async def bump_webhook_retry(
        self, webhook_id: int, delay_seconds: int, error: str
    ) -> None:
        """Increment attempt counter and schedule next retry."""
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute(
                """UPDATE pending_webhooks
                   SET attempts = attempts + 1,
                       next_retry_at = datetime('now', ?),
                       last_error = ?
                   WHERE id = ?""",
                (f"+{delay_seconds} seconds", error, webhook_id),
            )
            await db.commit()

    async def mark_webhook_failed(self, webhook_id: int) -> None:
        """Mark webhook as permanently failed (Dead Letter). Does NOT delete data."""
        async with aiosqlite.connect(self._db_path) as db:
            await db.execute(
                "UPDATE pending_webhooks SET status = 'failed' WHERE id = ?",
                (webhook_id,),
            )
            await db.commit()

    # ═══════════════════════════════════════════
    #  CLEANUP
    # ═══════════════════════════════════════════

    async def close(self):
        pass
