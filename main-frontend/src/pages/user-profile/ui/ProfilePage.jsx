import React, { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/features/auth";
import { useUserProfile } from "@/features/user-profile";
import http from "@/shared/api/http-client";
import "@/shared/styles/pages/Profile.css";

function safeInitials(value) {
  if (!value) {
    return "U";
  }

  const normalized = String(value).trim();
  if (!normalized) {
    return "U";
  }

  const parts = normalized.split(/\s+/).filter(Boolean);
  if (parts.length >= 2) {
    return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
  }

  return normalized.slice(0, 2).toUpperCase();
}

function isProbablyUrl(value) {
  if (!value) {
    return true;
  }

  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

function formatSeconds(total) {
  const seconds = Number(total || 0);
  if (!isFinite(seconds) || seconds <= 0) {
    return "0 мин";
  }

  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);

  if (hours <= 0) {
    return `${minutes} мин`;
  }

  if (minutes <= 0) {
    return `${hours} ч`;
  }

  return `${hours} ч ${minutes} мин`;
}

export default function ProfilePage() {
  const { user } = useAuth();
  const username = user?.username;
  const {
    data: profile,
    isLoading,
    isError,
    error,
    saveProfile,
    saveStatus,
    saveError,
  } = useUserProfile(username);

  const [form, setForm] = useState({
    avatarUrl: "",
    bio: "",
    isPrivate: false,
  });
  const [initialForm, setInitialForm] = useState(null);
  const [avatarBroken, setAvatarBroken] = useState(false);
  const [avatarModalOpen, setAvatarModalOpen] = useState(false);
  const [avatarDraft, setAvatarDraft] = useState("");
  const [avatarDraftError, setAvatarDraftError] = useState("");
  const [toast, setToast] = useState(null);

  const analytics = useQuery({
    queryKey: ["analytics", "me", "summary"],
    queryFn: async () => {
      const response = await http.get("/analytics/me/summary");
      return response.data;
    },
    enabled: !!username,
    retry: 1,
  });

  useEffect(() => {
    if (!profile) {
      return;
    }

    const next = {
      avatarUrl: profile.avatarUrl || "",
      bio: profile.bio || "",
      isPrivate: !!profile.isPrivate,
    };

    setForm(next);
    setInitialForm(next);
    setAvatarBroken(false);
  }, [profile]);

  const displayName = useMemo(
    () => profile?.nickname || profile?.username || profile?.email || username || "Пользователь",
    [profile, username]
  );
  const email = useMemo(() => profile?.email || user?.email || "", [profile, user]);
  const initials = useMemo(() => safeInitials(displayName), [displayName]);
  const hasChanges = useMemo(() => {
    if (!initialForm) {
      return false;
    }

    return (
      (form.avatarUrl || "") !== (initialForm.avatarUrl || "") ||
      (form.bio || "") !== (initialForm.bio || "") ||
      !!form.isPrivate !== !!initialForm.isPrivate
    );
  }, [form, initialForm]);

  const avatarSrc = useMemo(() => (form.avatarUrl || "").trim(), [form.avatarUrl]);

  const openAvatarModal = () => {
    setAvatarDraft(form.avatarUrl || "");
    setAvatarDraftError("");
    setAvatarModalOpen(true);
  };

  const closeAvatarModal = () => {
    setAvatarModalOpen(false);
    setAvatarDraftError("");
  };

  const applyAvatarDraft = () => {
    const nextValue = (avatarDraft || "").trim();
    if (!isProbablyUrl(nextValue)) {
      setAvatarDraftError("Ссылка должна начинаться с http:// или https://");
      return;
    }

    setForm((state) => ({ ...state, avatarUrl: nextValue }));
    setAvatarBroken(false);
    setAvatarModalOpen(false);
  };

  const onSave = async () => {
    setToast(null);

    const payload = {
      avatarUrl: (form.avatarUrl || "").trim() || null,
      bio: (form.bio || "").trim() || null,
      isPrivate: !!form.isPrivate,
    };

    try {
      await saveProfile(payload);
      setInitialForm({
        avatarUrl: payload.avatarUrl || "",
        bio: payload.bio || "",
        isPrivate: payload.isPrivate,
      });
      setToast({ type: "ok", text: "Сохранено" });
      setTimeout(() => setToast(null), 1800);
    } catch (saveProfileError) {
      const msg =
        saveProfileError?.response?.data?.message ||
        saveProfileError?.response?.data ||
        saveProfileError?.message ||
        "Ошибка сохранения";
      setToast({ type: "err", text: String(msg) });
      setTimeout(() => setToast(null), 3500);
    }
  };

  if (!username) {
    return (
      <div className="container profile-page">
        <div className="profile-shell">
          <h1 className="profile-title">Профиль</h1>
          <div className="profile-card">
            <p className="profile-muted">Войдите, чтобы управлять профилем.</p>
          </div>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="container profile-page">
        <div className="profile-shell">
          <h1 className="profile-title">Профиль</h1>
          <div className="profile-card">
            <p className="profile-muted">Загрузка...</p>
          </div>
        </div>
      </div>
    );
  }

  if (isError) {
    const msg =
      error?.response?.data?.message ||
      error?.response?.data ||
      error?.message ||
      "Ошибка загрузки";

    return (
      <div className="container profile-page">
        <div className="profile-shell">
          <h1 className="profile-title">Профиль</h1>
          <div className="profile-card">
            <p className="profile-error">Не удалось загрузить профиль: {String(msg)}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container profile-page">
      <div className="profile-shell">
        <div className="profile-header">
          <h1 className="profile-title">Профиль</h1>

          <div className="profile-header-actions">
            {toast ? (
              <div className={`profile-toast ${toast.type === "ok" ? "ok" : "err"}`}>
                {toast.text}
              </div>
            ) : null}

            <button
              className="profile-btn primary"
              onClick={onSave}
              disabled={!hasChanges || saveStatus === "pending"}
              title={!hasChanges ? "Нет изменений" : "Сохранить"}
            >
              {saveStatus === "pending" ? "Сохранение..." : "Сохранить"}
            </button>
          </div>
        </div>

        <div className="profile-grid">
          <div className="profile-card">
            <div className="profile-main">
              <div className="profile-avatar">
                {avatarSrc && !avatarBroken ? (
                  <img src={avatarSrc} alt="Аватар" onError={() => setAvatarBroken(true)} />
                ) : (
                  <div className="profile-avatar-fallback" aria-label="Аватар по умолчанию">
                    {initials}
                  </div>
                )}
              </div>

              <div className="profile-main-info">
                <div className="profile-name">{displayName}</div>
                {email ? <div className="profile-sub">{email}</div> : null}

                <div className="profile-main-actions">
                  <button className="profile-btn" onClick={openAvatarModal}>
                    Изменить фото
                  </button>

                  {hasChanges ? (
                    <span className="profile-badge">Есть несохраненные изменения</span>
                  ) : (
                    <span className="profile-badge subtle">Все сохранено</span>
                  )}
                </div>
              </div>
            </div>

            <div className="profile-section">
              <div className="profile-section-title">О себе</div>
              <textarea
                className="profile-textarea"
                value={form.bio}
                onChange={(event) => setForm((state) => ({ ...state, bio: event.target.value }))}
                placeholder="Напишите пару слов..."
                maxLength={350}
                rows={4}
              />
              <div className="profile-hint">{String(form.bio || "").length}/350</div>
            </div>

            <div className="profile-section">
              <div className="profile-section-title">Приватность</div>

              <label className="profile-toggle">
                <input
                  type="checkbox"
                  checked={!!form.isPrivate}
                  onChange={(event) =>
                    setForm((state) => ({ ...state, isPrivate: event.target.checked }))
                  }
                />
                <span className="profile-toggle-ui" />
                <span className="profile-toggle-text">Приватный профиль</span>
              </label>

              <div className="profile-hint">
                Если включено, другие пользователи не увидят вашу активность.
              </div>
            </div>

            {saveError ? (
              <div className="profile-inline-error">
                {String(
                  saveError?.response?.data?.message ||
                    saveError?.response?.data ||
                    saveError?.message ||
                    "Ошибка сохранения"
                )}
              </div>
            ) : null}
          </div>

          <div className="profile-side">
            <div className="profile-card">
              <div className="profile-section-title">Активность</div>
              <button className="profile-btn" onClick={() => (window.location.href = `/activity/${username}`)}>
                Посмотреть недавнюю активность
              </button>
            </div>

            <div className="profile-card">
              <div className="profile-section-title">Моя активность</div>

              {analytics.isLoading ? (
                <p className="profile-muted">Загрузка...</p>
              ) : analytics.isError ? (
                <p className="profile-muted">Пока нет данных.</p>
              ) : (
                <div className="profile-stats">
                  <div className="profile-stat">
                    <div className="profile-stat-label">Время просмотра</div>
                    <div className="profile-stat-value">
                      {formatSeconds(analytics.data?.totalSeconds)}
                    </div>
                  </div>

                  <div className="profile-stat">
                    <div className="profile-stat-label">Топ жанры</div>
                    <div className="profile-chips">
                      {(analytics.data?.topGenres || []).slice(0, 6).map((genre) => (
                        <span key={genre.genre} className="profile-chip">
                          {genre.genre} · {genre.count}
                        </span>
                      ))}
                      {(!analytics.data?.topGenres || analytics.data.topGenres.length === 0) && (
                        <span className="profile-muted">Нет жанров</span>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="profile-card">
              <div className="profile-section-title">Фото профиля</div>
              <p className="profile-muted">
                Сейчас фото хранится как ссылка. Если нужна загрузка файлов, это требует отдельной задачи на бэкенде.
              </p>
              <button className="profile-btn" onClick={openAvatarModal}>
                Поменять фото
              </button>
            </div>
          </div>
        </div>

        {avatarModalOpen ? (
          <div className="profile-modal-backdrop" onMouseDown={closeAvatarModal}>
            <div className="profile-modal" onMouseDown={(event) => event.stopPropagation()}>
              <div className="profile-modal-head">
                <div className="profile-modal-title">Изменить фото</div>
                <button className="profile-btn icon" onClick={closeAvatarModal} aria-label="Закрыть">
                  x
                </button>
              </div>

              <div className="profile-modal-body">
                <div className="profile-modal-preview">
                  {avatarDraft && isProbablyUrl(avatarDraft) ? (
                    <img src={avatarDraft} alt="Предпросмотр" />
                  ) : (
                    <div className="profile-modal-preview-fallback">{initials}</div>
                  )}
                </div>

                <div className="profile-modal-fields">
                  <label className="profile-label">
                    Ссылка на изображение
                    <input
                      className="profile-input"
                      value={avatarDraft}
                      onChange={(event) => {
                        setAvatarDraft(event.target.value);
                        setAvatarDraftError("");
                      }}
                      placeholder="https://..."
                    />
                  </label>

                  {avatarDraftError ? (
                    <div className="profile-inline-error">{avatarDraftError}</div>
                  ) : null}

                  <div className="profile-hint">
                    Поддерживаются обычные http/https ссылки.
                  </div>
                </div>
              </div>

              <div className="profile-modal-actions">
                <button className="profile-btn" onClick={closeAvatarModal}>
                  Отмена
                </button>
                <button className="profile-btn primary" onClick={applyAvatarDraft}>
                  Применить
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </div>
  );
}
