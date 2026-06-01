import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth } from "@/features/auth";
import { useUserSettings } from "@/features/user-profile";
import "@/pages/settings/ui/Settings.css";

export default function SettingsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const username = user?.username || localStorage.getItem("username") || "";
  const userId = username || "user1";

  const {
    data: settingsResp,
    isLoading,
    isError,
    save: saveSettings,
    saveStatus,
  } = useUserSettings(username);

  const initial = useMemo(
    () => ({
      language: "ru",
      ...(settingsResp?.data || {}),
    }),
    [settingsResp],
  );

  const [settings, setSettings] = useState(initial);

  useEffect(() => setSettings(initial), [initial]);

  const handleSave = async () => {
    try {
      const payload = { ...settings };
      delete payload.region;
      delete payload.newsletter;
      await saveSettings(payload);
    } catch {}
  };

  const handleLogout = () => {
    logout?.();
    ["favorites_", "profile_", "history_"].forEach((prefix) =>
      localStorage.removeItem(`${prefix}${userId}`),
    );
    navigate("/login");
  };

  return (
    <div className="settings-page">
      {isLoading ? <div className="loading">Загрузка...</div> : null}
      {isError ? <div className="error">Ошибка загрузки настроек</div> : null}

      <div className="settings-container">
        <div className="settings-section">
          <h2>Настройки просмотра</h2>

          <div className="settings-grid">
            <div className="setting-row">
              <label className="setting-label">Язык интерфейса</label>
              <select
                className="setting-input"
                value={settings.language}
                onChange={(event) =>
                  setSettings((state) => ({ ...state, language: event.target.value }))
                }
              >
                <option value="ru">Русский</option>
                <option value="kk">Қазақша</option>
                <option value="en">English</option>
              </select>
            </div>

            <button onClick={handleSave} className="btn btn-primary" disabled={saveStatus === "pending"}>
              {saveStatus === "pending" ? "Сохраняем..." : "Сохранить настройки"}
            </button>
          </div>
        </div>

        <div className="settings-section">
          <h2>Прочее</h2>
          <div className="settings-grid">
            <div className="setting-tile setting-tile--stub">Подписка</div>
            <div className="setting-tile setting-tile--stub">Привязанная карта</div>
            <div className="setting-tile setting-tile--stub">FAQ</div>
          </div>
        </div>

        <div className="settings-section">
          <h2>Управление аккаунтом</h2>
          <div className="settings-grid">
            <button className="btn btn-danger" onClick={handleLogout}>
              Выйти
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
