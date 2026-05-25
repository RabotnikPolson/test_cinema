import http from "./http-client.js";

export const logClick = async (movieId) => {
  try {
    await http.post("/metrics/clicks", { movieId });
  } catch (e) {
    console.warn("logClick failed:", e);
  }
};

export const logSearch = async (query, resultCount = 0) => {
  try {
    await http.post("/metrics/searches", { query, resultCount });
  } catch (e) {
    console.warn("logSearch failed:", e);
  }
};

export const logSubtitleEvent = async (movieId, action, lang = null) => {
  try {
    await http.post("/metrics/subtitles", { movieId, action, lang });
  } catch (e) {
    console.warn("logSubtitleEvent failed:", e);
  }
};