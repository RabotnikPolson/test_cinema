import Mp4Player from "./Mp4Player";
import HlsPlayer from "./HlsPlayer";

export default function VideoPlayer(props) {
  const format = String(props?.stream?.videoFormat || props?.stream?.format || "MP4").toUpperCase();

  if (format === "HLS") {
    return <HlsPlayer {...props} />;
  }

  return <Mp4Player {...props} />;
}
