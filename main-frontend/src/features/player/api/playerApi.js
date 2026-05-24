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

  const response = await fetch(resolvePublicAsset("storage/videos/catalog.json"), {
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error("Unable to load local stream catalog.");
  }

  const catalog = await response.json();
  const entry = catalog?.movies?.[String(movieId)];

  if (!entry) {
    return null;
  }

  return normalizeStream(entry);
}
