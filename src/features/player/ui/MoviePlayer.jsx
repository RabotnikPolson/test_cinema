import { useCallback, useEffect, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import http from "@/shared/api/http-client";
import "./MoviePlayer.css";

const S3_BASE = "http://localhost:9000/project-cinema/movies";

const SUBTITLE_OPTIONS = [
  { lang: "kk", label: "\u049a\u0430\u0437\u0430\u049b\u0448\u0430" },
  { lang: "ru", label: "\u0420\u0443\u0441\u0441\u043a\u0438\u0439" },
  { lang: "en", label: "English" },
];

function fmt(s) {
  if (!Number.isFinite(s) || s < 0) return "0:00";
  const m = Math.floor(s / 60);
  const sec = Math.floor(s % 60);
  return `${m}:${sec.toString().padStart(2, "0")}`;
}

const guestSessionId =
  typeof crypto !== "undefined" && crypto.randomUUID
    ? crypto.randomUUID()
    : Math.random().toString(36).slice(2);

/* ── SVG icons ──────────────────────────────────────────────── */
const PlayIcon = () => (
  <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
);
const PauseIcon = () => (
  <svg viewBox="0 0 24 24"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" /></svg>
);
const VolumeIcon = () => (
  <svg viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z" /></svg>
);
const MuteIcon = () => (
  <svg viewBox="0 0 24 24"><path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z" /></svg>
);
const FullscreenIcon = () => (
  <svg viewBox="0 0 24 24"><path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z" /></svg>
);
const TheaterIcon = () => (
  <svg viewBox="0 0 24 24"><path d="M19 6H5c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 10H5V8h14v8z" /></svg>
);
const CcIcon = () => (
  <svg viewBox="0 0 24 24"><path d="M19 4H5c-1.11 0-2 .9-2 2v12c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-8 7H9.5v-.5h-2v3h2V13H11v1c0 .55-.45 1-1 1H7c-.55 0-1-.45-1-1v-4c0-.55.45-1 1-1h3c.55 0 1 .45 1 1v1zm7 0h-1.5v-.5h-2v3h2V13H18v1c0 .55-.45 1-1 1h-3c-.55 0-1-.45-1-1v-4c0-.55.45-1 1-1h3c.55 0 1 .45 1 1v1z" /></svg>
);

export default function MoviePlayer({ movieId }) {
  const videoRef = useRef(null);
  const wrapperRef = useRef(null);
  const timelineRef = useRef(null);

  const [playing, setPlaying] = useState(false);
  const [currentTime, setCurrent] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(1);
  const [muted, setMuted] = useState(false);
  const [activeSub, setActiveSub] = useState("off");
  const [ccOpen, setCcOpen] = useState(false);
  const [theater, setTheater] = useState(false);
  const [controlsVisible, setControlsVisible] = useState(true);

  const hideTimer = useRef(null);

  const videoSrc = `${S3_BASE}/${movieId}/video.mp4`;

  /* ── Metrics mutations ────────────────────────────────────── */
  const clickMut = useMutation({
    mutationFn: () =>
      http.post("/metrics/movie-clicks", { movieId, guestSessionId }),
  });
  const subMut = useMutation({
    mutationFn: (payload) => http.post("/metrics/subtitle-events", payload),
  });

  /* ── Play / Pause ─────────────────────────────────────────── */
  const togglePlay = useCallback(() => {
    const v = videoRef.current;
    if (!v) return;
    if (v.paused) {
      v.play();
      clickMut.mutate();
    } else {
      v.pause();
    }
  }, []);

  /* ── Fullscreen ───────────────────────────────────────────── */
  const toggleFullscreen = useCallback(() => {
    const el = wrapperRef.current;
    if (!el) return;
    if (document.fullscreenElement) {
      document.exitFullscreen();
    } else {
      el.requestFullscreen();
    }
  }, []);

  /* ── Theater ──────────────────────────────────────────────── */
  const toggleTheater = useCallback(() => {
    if (document.fullscreenElement) document.exitFullscreen();
    setTheater((t) => !t);
  }, []);

  /* ── Mute ─────────────────────────────────────────────────── */
  const toggleMute = useCallback(() => {
    const v = videoRef.current;
    if (!v) return;
    v.muted = !v.muted;
    setMuted(v.muted);
  }, []);

  /* ── Subtitles ────────────────────────────────────────────── */
  const selectSub = useCallback(
    (lang) => {
      setActiveSub(lang);
      setCcOpen(false);
      const v = videoRef.current;
      if (!v) return;
      for (let i = 0; i < v.textTracks.length; i++) {
        v.textTracks[i].mode =
          v.textTracks[i].language === lang ? "showing" : "hidden";
      }
      if (lang === "off") {
        for (let i = 0; i < v.textTracks.length; i++) {
          v.textTracks[i].mode = "hidden";
        }
        subMut.mutate({
          movieId,
          action: "DISABLE",
          lang: "",
          guestSessionId,
        });
      } else {
        subMut.mutate({
          movieId,
          action: "ENABLE",
          lang,
          guestSessionId,
        });
      }
    },
    [movieId],
  );

  /* ── Timeline seek ────────────────────────────────────────── */
  const seekFromEvent = useCallback(
    (e) => {
      const v = videoRef.current;
      const bar = timelineRef.current;
      if (!v || !bar) return;
      const rect = bar.getBoundingClientRect();
      const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
      v.currentTime = pct * duration;
    },
    [duration],
  );

  /* ── Video events ─────────────────────────────────────────── */
  useEffect(() => {
    const v = videoRef.current;
    if (!v) return;
    const onPlay = () => setPlaying(true);
    const onPause = () => setPlaying(false);
    const onTime = () => setCurrent(v.currentTime);
    const onMeta = () => setDuration(v.duration);
    const onVolChange = () => {
      setVolume(v.volume);
      setMuted(v.muted);
    };

    v.addEventListener("play", onPlay);
    v.addEventListener("pause", onPause);
    v.addEventListener("timeupdate", onTime);
    v.addEventListener("loadedmetadata", onMeta);
    v.addEventListener("volumechange", onVolChange);
    return () => {
      v.removeEventListener("play", onPlay);
      v.removeEventListener("pause", onPause);
      v.removeEventListener("timeupdate", onTime);
      v.removeEventListener("loadedmetadata", onMeta);
      v.removeEventListener("volumechange", onVolChange);
    };
  }, []);

  /* ── Keyboard shortcuts ───────────────────────────────────── */
  useEffect(() => {
    const handler = (e) => {
      if (
        e.target.tagName === "INPUT" ||
        e.target.tagName === "TEXTAREA" ||
        e.target.isContentEditable
      )
        return;

      const v = videoRef.current;
      switch (e.code) {
        case "Space":
          e.preventDefault();
          togglePlay();
          break;
        case "KeyF":
          e.preventDefault();
          toggleFullscreen();
          break;
        case "KeyT":
          e.preventDefault();
          toggleTheater();
          break;
        case "KeyM":
          e.preventDefault();
          toggleMute();
          break;
        case "Escape":
          if (document.fullscreenElement) {
            document.exitFullscreen();
          } else if (theater) {
            setTheater(false);
          }
          break;
        case "ArrowLeft":
          e.preventDefault();
          if (v) v.currentTime = Math.max(0, v.currentTime - 5);
          break;
        case "ArrowRight":
          e.preventDefault();
          if (v) v.currentTime = Math.min(duration, v.currentTime + 5);
          break;
        default:
          break;
      }
    };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [togglePlay, toggleFullscreen, toggleTheater, toggleMute, theater, duration]);

  /* ── Auto-hide controls ───────────────────────────────────── */
  const showControls = useCallback(() => {
    setControlsVisible(true);
    clearTimeout(hideTimer.current);
    if (playing) {
      hideTimer.current = setTimeout(() => setControlsVisible(false), 3000);
    }
  }, [playing]);

  useEffect(() => {
    if (!playing) {
      setControlsVisible(true);
      clearTimeout(hideTimer.current);
    }
  }, [playing]);

  /* ── Volume slider handler ────────────────────────────────── */
  const onVolumeChange = useCallback((e) => {
    const v = videoRef.current;
    if (!v) return;
    const val = parseFloat(e.target.value);
    v.volume = val;
    v.muted = val === 0;
  }, []);

  const pct = duration > 0 ? (currentTime / duration) * 100 : 0;
  const remaining = duration - currentTime;

  const wrapperCls = [
    "cv-player-wrapper",
    theater && "theater-mode",
    !playing && "cv-paused",
    controlsVisible && "cv-controls-visible",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div
      ref={wrapperRef}
      className={wrapperCls}
      onMouseMove={showControls}
      onMouseLeave={() => playing && setControlsVisible(false)}
    >
      <video
        ref={videoRef}
        src={videoSrc}
        crossOrigin="anonymous"
        onClick={togglePlay}
        playsInline
      >
        {SUBTITLE_OPTIONS.map((opt) => (
          <track
            key={opt.lang}
            kind="subtitles"
            src={`${S3_BASE}/${movieId}/sub_${opt.lang}.vtt`}
            srcLang={opt.lang}
            label={opt.label}
          />
        ))}
      </video>

      {/* Big play button when paused */}
      <div className="cv-big-play" onClick={togglePlay}>
        <div className="cv-big-play-circle">
          <PlayIcon />
        </div>
      </div>

      {/* Controls */}
      <div className="cv-controls">
        {/* Timeline */}
        <div
          className="cv-timeline"
          ref={timelineRef}
          onClick={seekFromEvent}
        >
          <div className="cv-timeline-filled" style={{ width: `${pct}%` }} />
        </div>

        {/* Bottom row */}
        <div className="cv-controls-row">
          <button className="cv-btn" onClick={togglePlay} title={playing ? "Pause" : "Play"}>
            {playing ? <PauseIcon /> : <PlayIcon />}
          </button>

          <div className="cv-volume-group">
            <button className="cv-btn" onClick={toggleMute} title={muted ? "Unmute" : "Mute"}>
              {muted || volume === 0 ? <MuteIcon /> : <VolumeIcon />}
            </button>
            <input
              type="range"
              className="cv-volume-slider"
              min="0"
              max="1"
              step="0.05"
              value={muted ? 0 : volume}
              onChange={onVolumeChange}
            />
          </div>

          <span className="cv-time">
            {fmt(currentTime)} / -{fmt(remaining)}
          </span>

          <div className="cv-controls-right">
            {/* CC */}
            <div className="cv-cc-wrapper">
              <button
                className="cv-btn"
                onClick={() => setCcOpen((o) => !o)}
                title="Subtitles"
                style={activeSub !== "off" ? { color: "var(--gold)" } : undefined}
              >
                <CcIcon />
              </button>
              {ccOpen && (
                <div className="cv-cc-menu">
                  {SUBTITLE_OPTIONS.map((opt) => (
                    <button
                      key={opt.lang}
                      className={activeSub === opt.lang ? "cv-cc-active" : ""}
                      onClick={() => selectSub(opt.lang)}
                    >
                      {opt.label}
                    </button>
                  ))}
                  <button
                    className={activeSub === "off" ? "cv-cc-active" : ""}
                    onClick={() => selectSub("off")}
                  >
                    Выкл
                  </button>
                </div>
              )}
            </div>

            <button className="cv-btn" onClick={toggleTheater} title="Theater (T)">
              <TheaterIcon />
            </button>

            <button className="cv-btn" onClick={toggleFullscreen} title="Fullscreen (F)">
              <FullscreenIcon />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
