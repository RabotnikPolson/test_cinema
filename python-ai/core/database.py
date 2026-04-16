import os
from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from sqlalchemy.orm import declarative_base

# Fallback to local testdb if not provided
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql+asyncpg://test_user:test_password@localhost:5432/testdb")

# Create the async engine
# Note: pool_pre_ping=True checks the connection before checking out from pool
engine = create_async_engine(
    DATABASE_URL,
    echo=False,  # Set to True for SQL query debugging
    pool_size=10,
    max_overflow=20,
    pool_pre_ping=True
)

# Async session factory
AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False
)

Base = declarative_base()

async def get_db():
    """Dependency for acquiring a database session."""
    async with AsyncSessionLocal() as session:
        yield session
