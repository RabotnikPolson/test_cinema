//1
import { useEffect, useRef } from "react";
import api from "@/shared/api/http-client";

export default function WatchTracker({ url, movieId }) {
  const videoRef = useRef(null);
  const sessionIdRef = useRef(crypto.randomUUID());
  const lastSentTimeRef = useRef(0);
  const fakeTimeRef = useRef(0);

  const isEmbed = url && !url.includes(".m3u8") && !url.includes(".mp4");

  useEffect(() => {
    if (!url) return;

    let hls;
    let disposed = false;

    if (!isEmbed) {
      const video = videoRef.current;
      if (!video) return;

      const attachPlayer = async () => {
        if (url.includes(".m3u8")) {
          const { default: Hls } = await import("hls.js/light");
          if (disposed) {
            return;
          }

          if (Hls.isSupported()) {
            hls = new Hls();
            hls.loadSource(url);
            hls.attachMedia(video);
            return;
          }
        }

        video.src = url;
      };

      attachPlayer().catch((err) => {
        console.error(err);
        if (video) video.src = url;
      });
    }

    const interval = setInterval(() => {
      let currentPos = 0;
      let isPlaying = false;

      if (isEmbed) {
        fakeTimeRef.current += 5;
        currentPos = fakeTimeRef.current;
        isPlaying = true;
      } else {
        const video = videoRef.current;
        if (video && !video.paused && !video.ended) {
          currentPos = Math.floor(video.currentTime);
          isPlaying = true;
        }
      }

      if (isPlaying) {
        const delta = currentPos - lastSentTimeRef.current;

        if (delta > 0 && delta <= 30) {
          api.post("/watch-history/beat", {
            movieId,
            sessionId: sessionIdRef.current,
            currentPositionSec: currentPos,
            deltaSec: delta
          }).catch(err => console.error(err));
        }

        lastSentTimeRef.current = currentPos;
      }

    }, 5000);

    return () => {
      disposed = true;
      clearInterval(interval);
      if (hls) {
        hls.destroy();
      }
    };
  }, [url, movieId, isEmbed]);

  if (!url) return null;

  if (isEmbed) {
    return (
      <iframe
        src={url}
        allowFullScreen
        className="watch-embed-frame"
        style={{ width: "100%", background: "#000", aspectRatio: "16/9", border: "none" }}
      />
    );
  }

  return (
    <video
      ref={videoRef}
      controls
      className="watch-embed-frame"
      style={{ width: "100%", background: "#000", aspectRatio: "16/9" }}
    />
  );
}
