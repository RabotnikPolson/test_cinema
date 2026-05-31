import { VideoPlayer } from "@/features/player/ui";
import { useMovieStream } from "@/features/player/model/useMovieStream";

export default function MoviePlayer({ movie }) {
  const movieId = movie?.id ?? null;
  const streamQuery = useMovieStream(movieId);
  const stream = streamQuery.data || null;

  return (
    <section className="watch-player glass">
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
    </section>
  );
}
