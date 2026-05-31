import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  getSavedMovieProgress,
  saveMovieProgress,
  useHistoryStorage,
} from "@/shared/utils";
import { useAuth } from "@/features/auth";
import { logSubtitleEvent } from "@/shared/api/metricsApi";
import http from "@/shared/api/http-client";
import "./player.css";

const SEEK_STEP = 5;

function formatTime(seconds) {
  if (!Number.isFinite(seconds) || seconds < 0) {
    return "0:00";
  }

  const totalSeconds = Math.floor(seconds);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const secs = totalSeconds % 60;

  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, "0")}:${String(secs).padStart(2, "0")}`;
  }

  return `${minutes}:${String(secs).padStart(2, "0")}`;
}

const PlayIcon = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M8 5v14l11-7z" />
  </svg>
);

const PauseIcon = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
  </svg>
);

const VolumeIcon = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z" />
  </svg>
);

const MuteIcon = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z" />
  </svg>
);

const FullscreenIcon = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z" />
  </svg>
);

const CaptionIcon = () => (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M19 4H5c-1.11 0-2 .9-2 2v12c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-8 7H9.5v-.5h-2v3h2V13H11v1c0 .55-.45 1-1 1H7c-.55 0-1-.45-1-1v-4c0-.55.45-1 1-1h3c.55 0 1 .45 1 1v1zm7 0h-1.5v-.5h-2v3h2V13H18v1c0 .55-.45 1-1 1h-3c-.55 0-1-.45-1-1v-4c0-.55.45-1 1-1h3c.55 0 1 .45 1 1v1z" />
  </svg>
);

export default function PlayerSurface({ movie, stream, attachSource }) {
  const { user } = useAuth();
  const videoRef = useRef(null);
  const wrapperRef = useRef(null);
  const timelineRef = useRef(null);
  const resumeAppliedRef = useRef(false);
  const hideTimerRef = useRef(null);
  const historyTrackedRef = useRef(false);
  const pendingSourceStateRef = useRef(null);
  const scrubbingRef = useRef(false);
  const lastPointerRef = useRef({ x: -1, y: -1 });

  const { push } = useHistoryStorage();

  const [playing, setPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(Number(stream?.duration) || 0);
  const [volume, setVolume] = useState(1);
  const [muted, setMuted] = useState(false);
  const [buffering, setBuffering] = useState(false);
  const [controlsVisible, setControlsVisible] = useState(true);
  const [captionMenuOpen, setCaptionMenuOpen] = useState(false);
  const [activeSubtitle, setActiveSubtitle] = useState("off");
  const [playerError, setPlayerError] = useState("");
  const [subtitleNotice, setSubtitleNotice] = useState("");
  const [qualityMenuOpen, setQualityMenuOpen] = useState(false);
  const [isScrubbing, setIsScrubbing] = useState(false);
  const [selectedSourceId, setSelectedSourceId] = useState(() => stream?.sources?.[0]?.id || "source-default");
  const [subtitles, setSubtitles] = useState([]);

  const movieId = movie?.id ?? stream?.movieId ?? null;
  const posterUrl = stream?.posterUrl || movie?.poster || movie?.posterUrl || null;
  const sources = Array.isArray(stream?.sources) && stream.sources.length > 0 ? stream.sources : [];
  const selectedSource =
    sources.find((source) => source.id === selectedSourceId) ||
    sources[0] ||
    null;

  const savedProgress = useMemo(() => getSavedMovieProgress(movieId), [movieId]);
  const resumeTime =
    savedProgress && !savedProgress.completed ? Number(savedProgress.currentTime) || 0 : 0;
  const canResume = resumeTime > 5;

  useEffect(() => {
    const nextId = stream?.sources?.[0]?.id || "source-default";
    setSelectedSourceId(nextId);
  }, [stream?.movieId, stream?.sources]);

  // Subtitles are fetched via axios (CORS-enabled) and exposed as blob: URLs,
  // so the native <track> loads them same-origin without CORS issues.
  useEffect(() => {
    const sourceTracks = Array.isArray(stream?.subtitles)
      ? stream.subtitles.filter((item) => item?.url)
      : [];

    if (sourceTracks.length === 0) {
      setSubtitles([]);
      return undefined;
    }

    let cancelled = false;
    const objectUrls = [];

    Promise.all(
      sourceTracks.map(async (item) => {
        try {
          const response = await http.get(item.url, { responseType: "blob" });
          const blobUrl = URL.createObjectURL(response.data);
          objectUrls.push(blobUrl);
          return { ...item, url: blobUrl };
        } catch {
          return null;
        }
      }),
    ).then((resolved) => {
      if (cancelled) {
        objectUrls.forEach((url) => URL.revokeObjectURL(url));
        return;
      }
      setSubtitles(resolved.filter(Boolean));
    });

    return () => {
      cancelled = true;
      objectUrls.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [stream?.subtitles]);

  const saveProgressSnapshot = useCallback(
    (completed = false) => {
      const video = videoRef.current;
      if (!video || !movieId) {
        return;
      }

      saveMovieProgress({
        movieId,
        currentTime: completed ? video.duration || 0 : video.currentTime || 0,
        duration: video.duration || duration || 0,
        completed,
        title: movie?.title || "",
        posterUrl: posterUrl || "",
      });
    },
    [duration, movie?.title, movieId, posterUrl],
  );

  const showControls = useCallback(() => {
    setControlsVisible(true);
    clearTimeout(hideTimerRef.current);

    if (playing) {
      hideTimerRef.current = setTimeout(() => setControlsVisible(false), 2500);
    }
  }, [playing]);

  // Chrome fires synthetic mousemove (same coords) when content under a still
  // cursor changes — e.g. a subtitle cue appears or cursor:none is applied.
  // Only react to real cursor movement, otherwise controls never auto-hide.
  const handlePointerMove = useCallback(
    (event) => {
      const last = lastPointerRef.current;
      if (event.clientX === last.x && event.clientY === last.y) {
        return;
      }
      lastPointerRef.current = { x: event.clientX, y: event.clientY };
      showControls();
    },
    [showControls],
  );

  const togglePlay = useCallback(() => {
    const video = videoRef.current;
    if (!video) {
      return;
    }

    if (video.paused) {
      video.play().catch(() => {
        setPlayerError("Playback could not be started in the browser.");
      });
      return;
    }

    video.pause();
  }, []);

  const toggleMute = useCallback(() => {
    const video = videoRef.current;
    if (!video) {
      return;
    }

    video.muted = !video.muted;
    setMuted(video.muted);
  }, []);

  const toggleFullscreen = useCallback(() => {
    const element = wrapperRef.current;
    if (!element) {
      return;
    }

    if (document.fullscreenElement) {
      document.exitFullscreen().catch(() => {});
      return;
    }

    element.requestFullscreen?.().catch(() => {});
  }, []);

  const selectSubtitle = useCallback((lang) => {
    const video = videoRef.current;
    if (!video) {
      return;
    }

    let selectedTrack = null;
    for (let index = 0; index < video.textTracks.length; index += 1) {
      const track = video.textTracks[index];
      const active = lang !== "off" && track.language === lang;
      track.mode = active ? "showing" : "hidden";
      if (active) {
        selectedTrack = track;
      }
    }

    setActiveSubtitle(lang);
    setSubtitleNotice("");

    if (lang !== "off") {
      logSubtitleEvent(user?.id, movieId, `enabled_lang_${lang}`);

      // Cues load async — check after browser had time to parse the VTT
      if (selectedTrack) {
        setTimeout(() => {
          if (selectedTrack.cues == null || selectedTrack.cues.length === 0) {
            setSubtitleNotice("Субтитры не загрузились. Проверьте файл.");
          }
        }, 1500);
      }
    } else {
      logSubtitleEvent(user?.id, movieId, "disabled");
    }

    setCaptionMenuOpen(false);
  }, [movieId, user?.id]);

  const seekFromClientX = useCallback(
    (clientX) => {
      const video = videoRef.current;
      const timeline = timelineRef.current;
      if (!video || !timeline || !duration) {
        return;
      }

      const bounds = timeline.getBoundingClientRect();
      const ratio = Math.max(0, Math.min(1, (clientX - bounds.left) / bounds.width));
      const nextTime = ratio * duration;
      video.currentTime = nextTime;
      setCurrentTime(nextTime);
    },
    [duration],
  );

  const beginScrubbing = useCallback(
    (event) => {
      scrubbingRef.current = true;
      setIsScrubbing(true);
      showControls();
      seekFromClientX(event.clientX);
    },
    [seekFromClientX, showControls],
  );

  const selectQuality = useCallback(
    (sourceId) => {
      if (!sourceId || sourceId === selectedSourceId) {
        setQualityMenuOpen(false);
        return;
      }

      const video = videoRef.current;
      pendingSourceStateRef.current = {
        currentTime: video?.currentTime || 0,
        shouldContinuePlaying: Boolean(video && !video.paused),
      };

      setSelectedSourceId(sourceId);
      setQualityMenuOpen(false);
    },
    [selectedSourceId],
  );

  const onVolumeChange = useCallback((event) => {
    const video = videoRef.current;
    if (!video) {
      return;
    }

    const nextVolume = Number(event.target.value);
    video.volume = nextVolume;
    video.muted = nextVolume === 0;
    setVolume(nextVolume);
    setMuted(video.muted);
  }, []);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) {
      return undefined;
    }

    let disposed = false;
    let detach = () => {};

    resumeAppliedRef.current = false;
    historyTrackedRef.current = false;
    setPlayerError("");
    setCurrentTime(0);
    setDuration(Number(stream?.duration) || 0);
    setBuffering(false);

    Promise.resolve(attachSource?.(video, selectedSource))
      .then((cleanup) => {
        if (disposed) {
          cleanup?.();
          return;
        }

        detach = typeof cleanup === "function" ? cleanup : () => {};
      })
      .catch(() => {
        setPlayerError("Video source could not be attached.");
      });

    return () => {
      disposed = true;
      detach();
      video.pause();
      video.removeAttribute("src");
      video.load();
    };
  }, [attachSource, selectedSource, stream?.duration]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) {
      return undefined;
    }

    const onPlay = () => {
      setPlaying(true);
      setBuffering(false);

      if (!historyTrackedRef.current && movieId) {
        push({
          imdbId: String(movieId),
          title: movie?.title || "Movie",
          posterUrl: posterUrl || "",
          timestamp: Date.now(),
        });
        historyTrackedRef.current = true;
      }
    };

    const onPause = () => {
      setPlaying(false);
      saveProgressSnapshot(false);
    };

    const onTimeUpdate = () => {
      setCurrentTime(video.currentTime || 0);
    };

    const onLoadedMetadata = () => {
      const metadataDuration = Number(video.duration) || Number(stream?.duration) || 0;
      setDuration(metadataDuration);
      setPlayerError("");
      setSubtitleNotice("");

      for (let index = 0; index < video.textTracks.length; index += 1) {
        video.textTracks[index].mode = "hidden";
      }

      const pendingSwitchState = pendingSourceStateRef.current;
      if (pendingSwitchState && metadataDuration > 0) {
        const nextTime = Math.min(pendingSwitchState.currentTime || 0, Math.max(0, metadataDuration - 1));
        video.currentTime = nextTime;
        setCurrentTime(nextTime);
        pendingSourceStateRef.current = null;

        if (pendingSwitchState.shouldContinuePlaying) {
          video.play().catch(() => {});
        }
      } else if (
        !resumeAppliedRef.current &&
        canResume &&
        metadataDuration > 0 &&
        resumeTime < metadataDuration - 15
      ) {
        video.currentTime = resumeTime;
        setCurrentTime(resumeTime);
      }

      resumeAppliedRef.current = true;

      if (activeSubtitle !== "off") {
        selectSubtitle(activeSubtitle);
      }
    };

    const onVolumeChangeEvent = () => {
      setVolume(video.volume);
      setMuted(video.muted);
    };

    const onWaiting = () => setBuffering(true);
    const onCanPlay = () => setBuffering(false);
    const onPlaying = () => setBuffering(false);
    const onEnded = () => {
      setPlaying(false);
      saveProgressSnapshot(true);
    };
    const onError = () => {
      setPlayerError("The browser could not decode this video source.");
      setBuffering(false);
      setPlaying(false);
    };

    video.addEventListener("play", onPlay);
    video.addEventListener("pause", onPause);
    video.addEventListener("timeupdate", onTimeUpdate);
    video.addEventListener("loadedmetadata", onLoadedMetadata);
    video.addEventListener("volumechange", onVolumeChangeEvent);
    video.addEventListener("waiting", onWaiting);
    video.addEventListener("canplay", onCanPlay);
    video.addEventListener("playing", onPlaying);
    video.addEventListener("ended", onEnded);
    video.addEventListener("error", onError);

    return () => {
      video.removeEventListener("play", onPlay);
      video.removeEventListener("pause", onPause);
      video.removeEventListener("timeupdate", onTimeUpdate);
      video.removeEventListener("loadedmetadata", onLoadedMetadata);
      video.removeEventListener("volumechange", onVolumeChangeEvent);
      video.removeEventListener("waiting", onWaiting);
      video.removeEventListener("canplay", onCanPlay);
      video.removeEventListener("playing", onPlaying);
      video.removeEventListener("ended", onEnded);
      video.removeEventListener("error", onError);
    };
  }, [
    activeSubtitle,
    canResume,
    movie?.title,
    movieId,
    posterUrl,
    push,
    resumeTime,
    saveProgressSnapshot,
    selectSubtitle,
    stream?.duration,
  ]);

  useEffect(() => {
    if (!playing) {
      setControlsVisible(true);
      clearTimeout(hideTimerRef.current);
      return undefined;
    }

    const timer = setInterval(() => {
      saveProgressSnapshot(false);
    }, 5000);

    return () => clearInterval(timer);
  }, [playing, saveProgressSnapshot]);

  useEffect(() => {
    const onFullscreenChange = () => {
      showControls();
    };

    document.addEventListener("fullscreenchange", onFullscreenChange);
    return () => document.removeEventListener("fullscreenchange", onFullscreenChange);
  }, [showControls]);

  useEffect(() => {
    if (!isScrubbing) {
      return undefined;
    }

    const onMove = (event) => {
      seekFromClientX(event.clientX);
    };

    const onUp = (event) => {
      seekFromClientX(event.clientX);
      scrubbingRef.current = false;
      setIsScrubbing(false);
    };

    window.addEventListener("pointermove", onMove);
    window.addEventListener("pointerup", onUp);
    return () => {
      window.removeEventListener("pointermove", onMove);
      window.removeEventListener("pointerup", onUp);
    };
  }, [isScrubbing, seekFromClientX]);

  useEffect(() => {
    const onKeyDown = (event) => {
      if (
        event.target instanceof HTMLElement &&
        (event.target.tagName === "INPUT" ||
          event.target.tagName === "TEXTAREA" ||
          event.target.isContentEditable)
      ) {
        return;
      }

      const video = videoRef.current;
      if (!video) {
        return;
      }

      switch (event.code) {
        case "Space":
          event.preventDefault();
          togglePlay();
          break;
        case "KeyF":
          event.preventDefault();
          toggleFullscreen();
          break;
        case "KeyM":
          event.preventDefault();
          toggleMute();
          break;
        case "ArrowLeft":
          event.preventDefault();
          video.currentTime = Math.max(0, video.currentTime - SEEK_STEP);
          showControls();
          break;
        case "ArrowRight":
          event.preventDefault();
          video.currentTime = Math.min(video.duration || duration, video.currentTime + SEEK_STEP);
          showControls();
          break;
        case "ArrowUp":
          event.preventDefault();
          video.volume = Math.min(1, Math.round((video.volume + 0.1) * 10) / 10);
          video.muted = false;
          setVolume(video.volume);
          setMuted(false);
          showControls();
          break;
        case "ArrowDown":
          event.preventDefault();
          video.volume = Math.max(0, Math.round((video.volume - 0.1) * 10) / 10);
          setVolume(video.volume);
          setMuted(video.volume === 0);
          showControls();
          break;
        default:
          break;
      }
    };

    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [duration, showControls, toggleFullscreen, toggleMute, togglePlay]);

  useEffect(() => {
    return () => {
      clearTimeout(hideTimerRef.current);
      saveProgressSnapshot(false);
    };
  }, [saveProgressSnapshot]);

  const playedPercent = duration > 0 ? (currentTime / duration) * 100 : 0;
  const wrapperClassName = [
    "vp-shell",
    !playing && "vp-shell-paused",
    controlsVisible && "vp-shell-controls-visible",
    playing && !controlsVisible && "vp-shell--hide-cursor",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div
      ref={wrapperRef}
      className={wrapperClassName}
      onMouseMove={handlePointerMove}
      onMouseLeave={() => playing && setControlsVisible(false)}
    >
      <video ref={videoRef} poster={posterUrl || undefined} playsInline preload="metadata" onClick={togglePlay}>
        {subtitles.map((subtitle) => (
          <track
            key={`${subtitle.lang}-${subtitle.url}`}
            kind="subtitles"
            src={subtitle.url}
            srcLang={subtitle.lang}
            label={subtitle.label}
          />
        ))}
      </video>

      {buffering && (
        <div className="vp-state-overlay">
          <span className="vp-state-pill">Buffering...</span>
        </div>
      )}

      {playerError && (
        <div className="vp-state-overlay">
          <div className="vp-error-card">
            <strong>Playback error</strong>
            <span>{playerError}</span>
          </div>
        </div>
      )}

      {subtitleNotice && activeSubtitle !== "off" && !playerError && (
        <div className="vp-subtitle-notice">{subtitleNotice}</div>
      )}

      {!playing && !playerError && (
        <button className="vp-big-play" type="button" onClick={togglePlay} aria-label="Play movie">
          <PlayIcon />
        </button>
      )}

      {canResume && currentTime < 1 && !playing && !playerError && (
        <div className="vp-resume-badge">Resume from {formatTime(resumeTime)}</div>
      )}



      <div className="vp-controls">
        <div
          className={`vp-progress-track ${isScrubbing ? "vp-progress-track-scrubbing" : ""}`}
          ref={timelineRef}
          onPointerDown={beginScrubbing}
          role="presentation"
        >
          <div className="vp-progress-fill" style={{ width: `${playedPercent}%` }} />
        </div>

        <div className="vp-controls-row">
          <div className="vp-controls-group">
            <button className="vp-icon-button" type="button" onClick={togglePlay} aria-label={playing ? "Pause movie" : "Play movie"}>
              {playing ? <PauseIcon /> : <PlayIcon />}
            </button>

            <div className="vp-volume-group">
              <button className="vp-icon-button" type="button" onClick={toggleMute} aria-label={muted ? "Unmute" : "Mute"}>
                {muted || volume === 0 ? <MuteIcon /> : <VolumeIcon />}
              </button>
              <input
                className="vp-volume-slider"
                type="range"
                min="0"
                max="1"
                step="0.05"
                value={muted ? 0 : volume}
                onChange={onVolumeChange}
                aria-label="Volume"
              />
            </div>

            <span className="vp-time-label">
              {formatTime(currentTime)} / {formatTime(duration)}
            </span>
          </div>

          <div className="vp-controls-group vp-controls-group-right">
            {sources.length > 0 && (
              <div className="vp-quality-menu">
                <button
                  className="vp-text-button"
                  type="button"
                  onClick={() => setQualityMenuOpen((value) => !value)}
                  aria-label="Quality"
                >
                  {selectedSource?.quality || "Source"}
                </button>

                {qualityMenuOpen && (
                  <div className="vp-quality-popover">
                    {sources.map((source) => (
                      <button
                        key={source.id}
                        type="button"
                        className={selectedSource?.id === source.id ? "vp-captions-active" : ""}
                        onClick={() => selectQuality(source.id)}
                      >
                        {source.label || source.quality}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}

            {subtitles.length > 0 && (
              <div className="vp-captions-menu">
                <button
                  className="vp-icon-button"
                  type="button"
                  onClick={() => setCaptionMenuOpen((value) => !value)}
                  aria-label="Subtitles"
                >
                  <CaptionIcon />
                </button>

                {captionMenuOpen && (
                  <div className="vp-captions-popover">
                    {subtitles.map((subtitle) => (
                      <button
                        key={subtitle.lang}
                        type="button"
                        className={activeSubtitle === subtitle.lang ? "vp-captions-active" : ""}
                        onClick={() => selectSubtitle(subtitle.lang)}
                      >
                        {subtitle.label}
                      </button>
                    ))}
                    <button
                      type="button"
                      className={activeSubtitle === "off" ? "vp-captions-active" : ""}
                      onClick={() => selectSubtitle("off")}
                    >
                      Off
                    </button>
                  </div>
                )}
              </div>
            )}

            <button className="vp-icon-button" type="button" onClick={toggleFullscreen} aria-label="Fullscreen">
              <FullscreenIcon />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
