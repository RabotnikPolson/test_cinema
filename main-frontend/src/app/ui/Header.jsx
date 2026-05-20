import React, { useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "@/features/auth";
import { useMovies } from "@/features/movies";
import "@/shared/styles/components/Header.css";

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
  const { user, logout } = useAuth();
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
      setScrolled(window.scrollY > 60);
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const dictionary = useMemo(() => {
    const terms = new Set();

    for (const movie of movies) {
      if (movie?.title) {
        terms.add(movie.title);
      }

      if (movie?.genre) {
        String(movie.genre)
          .split(",")
          .map((item) => item.trim())
          .forEach((genre) => terms.add(genre));
      }
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
      (value) => !prefix.some((match) => match.toLowerCase() === value.toLowerCase())
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

  const navigateWith = (value) => {
    const nextValue = value.trim();
    if (nextValue) {
      navigate(`/?q=${encodeURIComponent(nextValue)}`);
      return;
    }

    navigate("/");
  };

  const submit = (event) => {
    event.preventDefault();
    navigateWith(q);

    if (q.trim()) {
      const next = [q.trim(), ...history.filter((item) => item.toLowerCase() !== q.trim().toLowerCase())];
      setHistory(next);
      saveHistory(next);
    }

    setOpen(false);
  };

  const clearQuery = () => {
    setQ("");
    navigate("/", { replace: true });
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

      const next = [selected, ...history.filter((item) => item.toLowerCase() !== selected.toLowerCase())];
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
    <header className={`header ${scrolled ? 'scrolled' : ''}`}>
      <div className="header-inner">
        <div className="header-left">
          <Link to="/" className="header-title">
            <span className="logo-cine">CINE</span>
            <span className="logo-verse">VERSE</span>
          </Link>
        </div>

        <div className="header-center">
          <form onSubmit={submit} className="search-bar" role="search" onFocus={() => setOpen(true)}>
            <input
              ref={inputRef}
              type="text"
              className="search-input"
              placeholder="Введите запрос"
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
                x
              </button>
            )}
            <button type="submit" className="search-btn" aria-label="Поиск">
              Поиск
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
                    <span className="search-item-icon">{q.trim() ? "?" : ">"}</span>
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
          ☰
        </button>

        <div className="header-right">
          {user ? (
            <>
              {user?.profile && user?.profile?.roles && String(user.profile.roles).toLowerCase().includes("admin") ? (
                <Link to="/admin/movies" className="auth-link">
                  Админка
                </Link>
              ) : null}
              <Link to="/profile" className="profile-link">
                @{user.username}
              </Link>
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
