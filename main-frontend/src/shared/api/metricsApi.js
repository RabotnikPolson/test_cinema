const REC_AI_URL = import.meta.env.VITE_REC_AI_URL || "http://localhost:8000";

export const logClick = async (userId, movieId, source = "browse") => {
  try {
    await fetch(`${REC_AI_URL}/api/v1/metrics/click`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ user_id: userId, movie_id: movieId, source }),
    });
  } catch (e) {
    console.warn("logClick failed:", e);
  }
};

export const logSearch = async (query, userId = null) => {
  try {
    await fetch(`${REC_AI_URL}/api/v1/metrics/search`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ query, user_id: userId }),
    });
  } catch (e) {
    console.warn("logSearch failed:", e);
  }
};

export const logSubtitleEvent = async (userId, movieId, action) => {
  try {
    await fetch(`${REC_AI_URL}/api/v1/metrics/subtitle`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ user_id: userId, movie_id: movieId, action }),
    });
  } catch (e) {
    console.warn("logSubtitleEvent failed:", e);
  }
};
