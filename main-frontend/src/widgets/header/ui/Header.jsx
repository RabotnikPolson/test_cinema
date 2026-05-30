import React, { useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { History, Menu, Search, Shield, User2, X } from "lucide-react";
import { logSearch } from "@/shared/api/metricsApi";
import { useAuth } from "@/features/auth";
import { useMovies } from "@/features/movies";
import { getMovieGenres } from "@/shared/lib/insight";
import "@/widgets/header/ui/Header.css";

const HISTORY_KEY = "search_history_v1";
const MAX_HISTORY = 20;
const MAX_SUGGESTIONS = 10;

function loadHistory() {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY) || "[]");
  } catch {
    return [];
  }
}

function saveHistory(arr) {
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(arr.slice(0, MAX_HISTORY)));
  } catch {}
}

export default function Header({ onMenuClick }) {
  const { user, logout, isAdmin } = useAuth();
  const { data: movies = [] } = useMovies();
  const [params] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const [open, setOpen] = useState(false);
  const [history, setHistory] = useState(loadHistory());
  const [highlight, setHighlight] = useState(-1);
  const [scrolled, setScrolled] = useState(false);
  const inputRef = useRef(null);
  const menuRef = useRef(null);

  useEffect(() => {
    const urlQ = params.get("q") || "";
    setQ(urlQ);
    setOpen(false);
    setHighlight(-1);
  }, [location.search, params]);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 40);
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const dictionary = useMemo(() => {
    const terms = new Set();
    for (const movie of movies) {
      if (movie?.title) terms.add(movie.title);
      getMovieGenres(movie).forEach((g) => terms.add(g));
    }
    return Array.from(terms);
  }, [movies]);

  const suggestions = useMemo(() => {
    const term = q.trim().toLowerCase();
    if (!term) {
      return history;
    }

    const prefix = dictionary
      .filter((value) => value.toLowerCase().startsWith(term))
      .slice(0, MAX_SUGGESTIONS);
    const historyOnly = history.filter(
      (value) => !prefix.some((match) => match.toLowerCase() === value.toLowerCase()),
    );

    return [...prefix, ...historyOnly].slice(0, MAX_SUGGESTIONS);
  }, [q, dictionary, history]);

  useEffect(() => {
    const onDoc = (event) => {
      if (!menuRef.current || !inputRef.current) {
        return;
      }

      if (
        !menuRef.current.contains(event.target) &&
        !inputRef.current.contains(event.target)
      ) {
        setOpen(false);
      }
    };

    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, []);

  const save = (value) => {
    const normalized = value.trim();
    const next = [
      normalized,
      ...history.filter((item) => item.toLowerCase() !== normalized.toLowerCase()),
    ];
    setHistory(next);
    saveHistory(next);
  };

  const navigateWith = (value) => {
    const nextValue = value.trim();
    if (nextValue) {
      logSearch(nextValue, user?.id);
      navigate(`/genres?q=${encodeURIComponent(nextValue)}`);
      return;
    }
    navigate("/genres");
  };

  const submit = (event) => {
    event.preventDefault();
    navigateWith(q);

    if (q.trim()) {
      const normalized = q.trim();
      const next = [
        normalized,
        ...history.filter((item) => item.toLowerCase() !== normalized.toLowerCase()),
      ];
      setHistory(next);
      saveHistory(next);
    }

    setOpen(false);
  };

  const clearQuery = () => {
    setQ("");
    navigate("/genres", { replace: true });
    setOpen(false);
    setHighlight(-1);
    inputRef.current?.focus();
  };

  const onKeyDown = (event) => {
    if (!open && (event.key === "ArrowDown" || event.key === "ArrowUp")) {
      setOpen(true);
      return;
    }

    if (!open || suggestions.length === 0) {
      return;
    }

    if (event.key === "ArrowDown") {
      event.preventDefault();
      setHighlight((value) => (value + 1) % suggestions.length);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setHighlight((value) => (value - 1 + suggestions.length) % suggestions.length);
    } else if (event.key === "Enter" && highlight >= 0 && suggestions[highlight]) {
      event.preventDefault();
      const selected = suggestions[highlight];
      setQ(selected);
      navigateWith(selected);

      const next = [
        selected,
        ...history.filter((item) => item.toLowerCase() !== selected.toLowerCase()),
      ];
      setHistory(next);
      saveHistory(next);
      setOpen(false);
    } else if (event.key === "Escape") {
      setOpen(false);
      setHighlight(-1);
    }
  };

  const clearHistory = () => {
    setHistory([]);
    saveHistory([]);
  };

  return (
    <header className={`header ${scrolled ? "scrolled" : ""}`}>
      <div className="header-inner">
        <div className="header-left">
          <Link to="/" className="header-title">
            <span className="header-logo-lockup">
              <span className="logo-verse">INsight</span>
            </span>
          </Link>
        </div>

        <div className="header-center">
          <form onSubmit={submit} className="search-bar" role="search" onFocus={() => setOpen(true)}>
            <span className="search-prefix" aria-hidden="true">
              <Search size={16} />
            </span>
            <input
              ref={inputRef}
              type="text"
              className="search-input"
              placeholder="Поиск фильмов и жанров"
              value={q}
              onChange={(event) => {
                setQ(event.target.value);
                setOpen(true);
                setHighlight(-1);
              }}
              onKeyDown={onKeyDown}
            />
            {q && (
              <button
                type="button"
                className="search-btn search-clear"
                aria-label="Очистить запрос"
                onClick={clearQuery}
              >
                <X size={16} />
              </button>
            )}
            <button type="submit" className="search-btn" aria-label="Поиск">
              <Search size={16} />
            </button>
          </form>

          {open && suggestions.length > 0 && (
            <div ref={menuRef} className="search-panel">
              <ul className="search-list">
                {suggestions.map((suggestion, index) => (
                  <li
                    key={`${suggestion}-${index}`}
                    className={`search-item ${index === highlight ? "active" : ""}`}
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => {
                      setQ(suggestion);
                      navigateWith(suggestion);
                      const next = [
                        suggestion,
                        ...history.filter((item) => item.toLowerCase() !== suggestion.toLowerCase()),
                      ];
                      setHistory(next);
                      saveHistory(next);
                      setOpen(false);
                    }}
                  >
                    <span className="search-item-icon">
                      {q.trim() ? <Search size={14} /> : <History size={14} />}
                    </span>
                    <span className="search-item-text">{suggestion}</span>
                  </li>
                ))}
              </ul>
              <div className="search-footer">
                <button className="clear-history" type="button" onClick={clearHistory}>
                  Очистить историю
                </button>
              </div>
            </div>
          )}
        </div>

        <button
          className="mobile-menu-toggle"
          onClick={onMenuClick}
          aria-label="Открыть меню"
        >
          <Menu size={18} />
        </button>

        <div className="header-right">
          {user ? (
            <>
              <Link to="/profile" className="profile-link">
                <span className="profile-link-icon">
                  <User2 size={16} />
                </span>
                <span>@{user.username}</span>
              </Link>
              {isAdmin ? (
                <Link to="/admin/movies" className="auth-link auth-link-admin">
                  <Shield size={14} />
                  <span>Админ</span>
                </Link>
              ) : null}
              <button type="button" className="logout-btn" onClick={logout}>
                Выйти
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="auth-link">
                Войти
              </Link>
              <Link to="/register" className="auth-link">
                Регистрация
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
