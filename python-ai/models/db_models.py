from sqlalchemy import Column, BigInteger, String, Integer, DateTime, Boolean, ForeignKey, SmallInteger, Text
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from pgvector.sqlalchemy import Vector
from core.database import Base

class Movie(Base):
    """Mapped to the Spring Boot 'movies' table."""
    __tablename__ = "movies"
    
    id = Column(BigInteger, primary_key=True, index=True)
    title = Column(String(255), nullable=False)
    # We only map the fields we actually need for RAG context
    description = Column(Text)
    genre_text = Column(String(255))
    year = Column(Integer)
    director = Column(String(100))
    actors = Column(String(500))
    
    subtitles = relationship("MovieSubtitle", back_populates="movie")


class MovieSubtitle(Base):
    """Mapped to the Spring Boot 'movie_subtitles' table."""
    __tablename__ = "movie_subtitles"
    
    id = Column(BigInteger, primary_key=True, index=True)
    movie_id = Column(BigInteger, ForeignKey("movies.id", ondelete="CASCADE"), nullable=False)
    language = Column(String(10), nullable=False)
    s3_path = Column(String(500))
    format = Column(String(10), nullable=False)
    
    is_downloaded = Column(Boolean, default=False, nullable=False)
    
    # Crucial flag: APScheduler looks for True 'is_downloaded' and False 'is_vectorized_for_ai'
    is_vectorized_for_ai = Column(Boolean, default=False, nullable=False)

    movie = relationship("Movie", back_populates="subtitles")
    chunks = relationship("SubtitleChunk", back_populates="subtitle", cascade="all, delete-orphan")


class SubtitleChunk(Base):
    """Mapped to the new 'subtitle_chunks' table (V22 DDL)."""
    __tablename__ = "subtitle_chunks"
    
    id = Column(BigInteger, primary_key=True, index=True)
    subtitle_id = Column(BigInteger, ForeignKey("movie_subtitles.id", ondelete="CASCADE"), nullable=False, index=True)
    
    chunk_text = Column(Text, nullable=False)
    chunk_index = Column(SmallInteger, nullable=False)
    
    # 384 dimensions matching sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2
    embedding = Column(Vector(384))
    
    created_at = Column(DateTime, server_default=func.now())

    subtitle = relationship("MovieSubtitle", back_populates="chunks")
