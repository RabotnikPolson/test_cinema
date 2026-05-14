from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, declarative_base
import logging
import os

DB_USER = os.getenv("DB_USER", "test_user")
DB_PASS = os.getenv("DB_PASS", "pass1")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "testdb")

DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def check_db_connection():
    try:
        with engine.connect() as connection:
            connection.execute(text("SELECT 1"))
            return True
    except:
        return False
