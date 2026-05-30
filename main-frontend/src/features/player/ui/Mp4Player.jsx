import { useCallback } from "react";
import PlayerSurface from "./PlayerSurface";

export default function Mp4Player({ stream, ...rest }) {
  const attachSource = useCallback(
    (video, source) => {
      video.src = source?.videoUrl || stream?.videoUrl || "";
      video.load();

      return () => {
        video.pause();
      };
    },
    [stream?.videoUrl],
  );

  return <PlayerSurface {...rest} stream={stream} attachSource={attachSource} />;
}
