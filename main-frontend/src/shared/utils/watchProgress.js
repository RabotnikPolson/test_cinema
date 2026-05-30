const KEY = "watch_progress_guest_v1";

function readStore() {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? JSON.parse(raw) : {};
  } catch {
    return {};
  }
}

function writeStore(store) {
  try {
    localStorage.setItem(KEY, JSON.stringify(store));
  } catch {}
}

function toMovieKey(movieId) {
  if (movieId === undefined || movieId === null) {
    return null;
  }

  return String(movieId);
}

export function getSavedMovieProgress(movieId) {
  const key = toMovieKey(movieId);
  if (!key) {
    return null;
  }

  return readStore()[key] || null;
}

export function getSavedMovieProgressPercent(movieId) {
  const saved = getSavedMovieProgress(movieId);
  if (!saved || saved.completed) {
    return 0;
  }

  return Number(saved.progressPercent) || 0;
}

export function saveMovieProgress({
  movieId,
  currentTime = 0,
  duration = 0,
  completed = false,
  title = "",
  posterUrl = "",
}) {
  const key = toMovieKey(movieId);
  if (!key) {
    return null;
  }

  const safeDuration = Number(duration) > 0 ? Number(duration) : 0;
  const safeCurrentTime = Math.max(0, Number(currentTime) || 0);
  const progressPercent =
    safeDuration > 0
      ? Math.min(100, Math.round((safeCurrentTime / safeDuration) * 100))
      : 0;

  const store = readStore();
  store[key] = {
    movieId: Number(movieId) || movieId,
    currentTime: safeCurrentTime,
    duration: safeDuration,
    progressPercent,
    completed: Boolean(completed),
    title,
    posterUrl,
    updatedAt: Date.now(),
  };

  writeStore(store);
  return store[key];
}

export function clearMovieProgress(movieId) {
  const key = toMovieKey(movieId);
  if (!key) {
    return;
  }

  const store = readStore();
  delete store[key];
  writeStore(store);
}
