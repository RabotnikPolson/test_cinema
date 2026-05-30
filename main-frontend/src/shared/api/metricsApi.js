import http from "@/shared/api/http-client";

export const logClick = async (userId, movieId, source = "browse") => {
  try {
    await http.post("/metrics/clicks", { movieId: Number(movieId) });
  } catch (e) {
    console.warn("logClick failed:", e);
  }
};

export const logSearch = async (query, userId = null, resultCount = 0) => {
  try {
    await http.post("/metrics/searches", { query, resultCount });
  } catch (e) {
    console.warn("logSearch failed:", e);
  }
};

export const logSubtitleEvent = async (userId, movieId, action) => {
  try {
    let backendAction;
    let lang = null;
    if (action === "disabled") {
      backendAction = "disable";
    } else if (action.startsWith("enabled_lang_")) {
      backendAction = "enable";
      lang = action.replace("enabled_lang_", "");
    } else {
      backendAction = "enable";
    }
    await http.post("/metrics/subtitles", { movieId: Number(movieId), action: backendAction, lang });
  } catch (e) {
    console.warn("logSubtitleEvent failed:", e);
  }
};
