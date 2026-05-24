import { VideoPlayer } from "@/features/player/ui";
import { useMovieStream } from "@/features/player/model/useMovieStream";

function formatRuntime(seconds) {
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return null;
  }

  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return `${hours}h ${String(minutes).padStart(2, "0")}m`;
}

export default function MoviePlayer({ movie }) {
  const movieId = movie?.id ?? null;
  const streamQuery = useMovieStream(movieId);
  const stream = streamQuery.data || null;

  return (
    <section className="watch-player glass">
      <div className="watch-player-top">
        <span>{stream ? "Local VOD stream" : "Player connection"}</span>
        {stream?.videoFormat ? <span className="watch-quality">{stream.videoFormat}</span> : null}
      </div>

      {streamQuery.isLoading ? (
        <div className="watch-player-stage">
          <div className="watch-player-status">Preparing player...</div>
        </div>
      ) : null}

      {streamQuery.isError ? (
        <div className="watch-player-stage">
          <div className="watch-player-status watch-player-status-error">
            <strong>Player metadata failed to load.</strong>
            <span>{streamQuery.error?.message || "Unknown player error."}</span>
          </div>
        </div>
      ) : null}

      {!streamQuery.isLoading && !streamQuery.isError && !stream ? (
        <div className="watch-player-stage">
          <div className="watch-player-fallback">
            This movie does not have a local video source configured yet.
          </div>
        </div>
      ) : null}

      {!streamQuery.isLoading && !streamQuery.isError && stream ? (
        <div className="watch-video-card">
          <VideoPlayer movie={movie} stream={stream} />
        </div>
      ) : null}

      <div className="watch-player-bottom">
        <span className="watch-player-status">
          Progress is saved locally every 5 seconds and restored on the next visit.
        </span>
        <span className="watch-quality">
          {formatRuntime(stream?.duration) || `${movie?.runtime || "?"} min`}
        </span>
      </div>
    </section>
  );
}
