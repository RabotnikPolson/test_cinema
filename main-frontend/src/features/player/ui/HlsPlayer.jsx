import { useCallback } from "react";
import PlayerSurface from "./PlayerSurface";

export default function HlsPlayer({ stream, ...rest }) {
  const attachSource = useCallback(
    async (video, source) => {
      const sourceUrl = source?.videoUrl || stream?.videoUrl || "";
      if (!sourceUrl) {
        return () => {};
      }

      if (video.canPlayType("application/vnd.apple.mpegurl")) {
        video.src = sourceUrl;
        video.load();
        return () => {
          video.pause();
        };
      }

      const HlsModule = await import("hls.js");
      const Hls = HlsModule.default;

      if (!Hls.isSupported()) {
        video.src = sourceUrl;
        video.load();
        return () => {
          video.pause();
        };
      }

      const hls = new Hls();
      hls.loadSource(sourceUrl);
      hls.attachMedia(video);

      return () => {
        hls.destroy();
      };
    },
    [stream?.videoUrl],
  );

  return <PlayerSurface {...rest} stream={stream} attachSource={attachSource} />;
}
