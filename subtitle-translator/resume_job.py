"""
Resume a stuck/exhausted translation job.
Usage: python resume_job.py <movie_id>
"""
import asyncio
import sys
from services.checkpoint_repository import CheckpointRepository


async def main():
    if len(sys.argv) < 2:
        print("Usage: python resume_job.py <movie_id>")
        return

    movie_id = int(sys.argv[1])
    repo = CheckpointRepository("./data/translator_state.db")

    # Find job (any status)
    import aiosqlite
    async with aiosqlite.connect(repo._db_path) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute(
            "SELECT * FROM translation_jobs WHERE movie_id = ? ORDER BY created_at DESC LIMIT 1",
            (movie_id,),
        ) as cursor:
            row = await cursor.fetchone()

    if not row:
        print(f"No job found for movie_id={movie_id}")
        return

    print(f"Found job: {row['job_id']}")
    print(f"  Status: {row['status']}")
    print(f"  Progress: chunk {row['next_chunk_index']}/{row['chunk_size']}")
    print(f"  Total lines: {row['total_lines']}")

    if row["status"] in ("exhausted", "completed"):
        await repo.update_job_status(row["job_id"], "in_progress")
        print(f"\n✅ Job reset to 'in_progress'. It will resume from chunk {row['next_chunk_index']}.")
        print(f"Now send a new POST /api/translate request for movie_id={movie_id}.")
    else:
        print(f"\nJob is already '{row['status']}', no reset needed.")


if __name__ == "__main__":
    asyncio.run(main())
