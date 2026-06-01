import { useTranslation } from "react-i18next";
import { VideoPlayer } from "@/features/player/ui";
import { useMovieStream } from "@/features/player/model/useMovieStream";

export default function MoviePlayer({ movie }) {
  const { t } = useTranslation();
  const movieId = movie?.id ?? null;
  const streamQuery = useMovieStream(movieId);
  const stream = streamQuery.data || null;

  return (
    <section className="watch-player glass">
      {streamQuery.isLoading ? (
        <div className="watch-player-stage">
          <div className="watch-player-status">{t("player.preparing")}</div>
        </div>
      ) : null}

      {streamQuery.isError ? (
        <div className="watch-player-stage">
          <div className="watch-player-status watch-player-status-error">
            <strong>{t("player.metadataFailed")}</strong>
            <span>{streamQuery.error?.message || t("player.unknownError")}</span>
          </div>
        </div>
      ) : null}

      {!streamQuery.isLoading && !streamQuery.isError && !stream ? (
        <div className="watch-player-stage">
          <div className="watch-player-fallback">
            {t("player.noSource")}
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
