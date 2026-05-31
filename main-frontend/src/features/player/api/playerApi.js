import http from "@/shared/api/http-client";

function detectFormat(url) {
  if (!url) return "MP4";
  const path = url.split("?")[0].toLowerCase();
  if (path.endsWith(".m3u8")) return "HLS";
  return "MP4";
}

export async function getMovieStream(movieId) {
  if (!movieId) return null;

  const { data } = await http.get(`/stream/${movieId}`);
  if (!data?.videoUrl) return null;

  const format = detectFormat(data.videoUrl);

  const subtitles = data.subtitleUrl
    ? [{ lang: "kk", label: "Қазақша", url: `/stream/${movieId}/subtitle` }]
    : [];

  return {
    movieId: Number(movieId),
    videoUrl: data.videoUrl,
    posterUrl: null,
    videoFormat: format,
    duration: 0,
    sources: [
      {
        id: "source-default",
        label: "Auto",
        quality: "Auto",
        videoUrl: data.videoUrl,
        format,
        width: 0,
        height: 0,
      },
    ],
    subtitles,
  };
}
