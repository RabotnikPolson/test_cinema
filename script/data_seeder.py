import random
import uuid
import time
from datetime import datetime, timedelta
import asyncio
import aiohttp

# Configuration
API_BASE_URL = "http://localhost:8080"
TOTAL_USERS = 200
MOVIES_TO_CREATE = 30  # Synthetic movies if enough don't exist
MAX_WATCH_HISTORY_PER_USER = 25
DAYS_HISTORY = 180

# Synthetic Data Setup
ROLES = ["USER"]
GENRES = ["Drama", "Comedy", "Action", "Thriller", "Sci-Fi", "Documentary"]

# Persona Distribution based on TDD
PERSONAS = {
    "PATRIOT": {"weight": 0.40, "domestic_pref": 0.8},
    "MAINSTREAM": {"weight": 0.30, "domestic_pref": 0.3},
    "ARTHOUSE": {"weight": 0.15, "domestic_pref": 0.5},
    "EXPLORER": {"weight": 0.15, "domestic_pref": 0.5},  # pure random
}

def generate_user(index):
    return {
        "email": f"synthetic_user_{index}@qazaqcinema.kz",
        "password": "password123", # simple default
        "fullName": f"Test User {index}"
    }

async def register_user(session, user_data):
    url = f"{API_BASE_URL}/api/v1/auth/register"
    async with session.post(url, json=user_data) as response:
        if response.status in [200, 201]:
            resp_json = await response.json()
            return resp_json.get("token")
        elif response.status == 400: # Already exists maybe? Try login
             login_url = f"{API_BASE_URL}/api/v1/auth/login"
             async with session.post(login_url, json={"email": user_data["email"], "password": user_data["password"]}) as login_resp:
                 if login_resp.status == 200:
                     resp_json = await login_resp.json()
                     return resp_json.get("token")
        print(f"Failed to register/login {user_data['email']}: {response.status}")
        return None

async def send_watch_beat(session, token, movie_id, session_id, delta_sec, is_paused=False, client_ts=None):
    url = f"{API_BASE_URL}/api/v1/stream/beat"
    headers = {"Authorization": f"Bearer {token}"}
    payload = {
        "movieId": movie_id,
        "sessionId": session_id,
        "deltaSec": delta_sec,
        "paused": is_paused,
        "clientTs": client_ts.isoformat() + "Z" if client_ts else datetime.utcnow().isoformat() + "Z"
    }
    async with session.post(url, json=payload, headers=headers) as response:
        return response.status

async def main():
    print("🚀 Starting Data Seeding for Qazaq Cinema AI Model")
    
    # Needs to be implemented properly, currently just a placeholder 
    # since we don't have direct DB access or movie endpoint access here in the script yet.
    # In a real scenario, this script would connect to Postgres directly using psycopg2
    # or use protected internal API endpoints to seed data.
    
    print("This script is a blueprint based on the TDD.")
    print("To execute fully, it requires a direct Postgres connection (psycopg2) or Movie Admin API endpoints.")
    print("Next step: Establish DB connection in python or run native SQL inserts.")

if __name__ == "__main__":
    asyncio.run(main())
