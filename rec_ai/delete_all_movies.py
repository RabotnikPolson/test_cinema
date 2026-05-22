import os
import urllib.request
from sqlalchemy import create_engine, text

DB_USER = os.getenv("DB_USER", "test_user")
DB_PASS = os.getenv("DB_PASS", "pass1")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "testdb")
DATABASE_URL = f"postgresql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}"

def delete_all():
    try:
        engine = create_engine(DATABASE_URL)
        with engine.begin() as conn:
            print("Deleting ALL movies from the database...")
            # Удаляем все записи из таблицы movies. 
            # Благодаря ON DELETE CASCADE в других таблицах (например, watch_history), 
            # связанные записи удалятся автоматически.
            result = conn.execute(text("DELETE FROM movies"))
            print(f"Successfully deleted {result.rowcount} movies.")
        
        print("Retraining model (clearing memory)...")
        req = urllib.request.Request("http://localhost:8000/api/v1/ml/retrain", method="POST")
        urllib.request.urlopen(req)
        print("ML model updated.")
        
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    delete_all()
