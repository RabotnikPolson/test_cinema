const ABSOLUTE_URL_RE = /^(?:[a-z]+:)?\/\//i;

export function resolvePublicAsset(path) {
  if (!path) {
    return null;
  }

  if (ABSOLUTE_URL_RE.test(path)) {
    return path;
  }

  const baseUrl = import.meta.env.BASE_URL || "/";
  const normalizedBase = baseUrl.endsWith("/") ? baseUrl : `${baseUrl}/`;
  return new URL(path.replace(/^\/+/, ""), `${window.location.origin}${normalizedBase}`).toString();
}

function normalizeSubtitle(subtitle = {}) {
  return {
    lang: subtitle.lang || subtitle.language || "",
    label: subtitle.label || subtitle.lang || "Subtitle",
    url: resolvePublicAsset(subtitle.url),
  };
}

function normalizeSource(source = {}, fallback = {}) {
  return {
    id: source.id || source.quality || source.label || fallback.id || "source-default",
    label: source.label || source.quality || fallback.label || "Source",
    quality: source.quality || source.label || fallback.quality || "Source",
    width: Number(source.width) || Number(fallback.width) || 0,
    height: Number(source.height) || Number(fallback.height) || 0,
    format: String(source.format || fallback.format || "MP4").toUpperCase(),
    videoUrl: resolvePublicAsset(source.videoUrl || fallback.videoUrl),
  };
}

function normalizeStream(entry = {}) {
  const primaryFormat = String(entry.videoFormat || entry.format || "MP4").toUpperCase();
  const defaultSource = normalizeSource(
    {
      id: "source-default",
      label: entry.quality || primaryFormat,
      quality: entry.quality || primaryFormat,
      videoUrl: entry.videoUrl,
      format: primaryFormat,
    },
    {},
  );

  const sources = Array.isArray(entry.sources) && entry.sources.length > 0
    ? entry.sources.map((source) => normalizeSource(source, defaultSource))
    : [defaultSource];

  return {
    movieId: Number(entry.movieId) || null,
    videoUrl: defaultSource.videoUrl,
    posterUrl: resolvePublicAsset(entry.posterUrl),
    trailerUrl: resolvePublicAsset(entry.trailerUrl),
    videoFormat: primaryFormat,
    duration: Number(entry.duration) || 0,
    sources,
    subtitles: Array.isArray(entry.subtitles) ? entry.subtitles.map(normalizeSubtitle) : [],
  };
}

export async function getMovieStream(movieId) {
  if (!movieId) {
    return null;
  }

  const { default: http } = await import("@/shared/api/http-client");
  const response = await http.get(`/stream/${movieId}`);
  const data = response.data;

  const videoUrl = data.videoUrl || null;
  const format = videoUrl && videoUrl.includes(".m3u8") ? "HLS" : "MP4";
  // Proxy subtitle through Java to avoid MinIO CORS block on <track> elements
  const subtitleUrl = data.subtitleUrl
    ? `${http.defaults.baseURL}/stream/${movieId}/subtitle`
    : null;

  return normalizeStream({
    movieId,
    videoUrl,
    videoFormat: format,
    subtitles: subtitleUrl
      ? [{ lang: "kk", label: "Казахский", url: subtitleUrl }]
      : [],
  });
}
