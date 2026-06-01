import { useTranslation } from "react-i18next";
import "./LanguageSwitcher.css";

const LANGS = [
  { code: "ru", label: "РУ" },
  { code: "kk", label: "ҚАЗ" },
];

export default function LanguageSwitcher() {
  const { i18n, t } = useTranslation();
  const current = i18n.language?.startsWith("kk") ? "kk" : "ru";

  return (
    <div className="lang-switcher" role="group" aria-label={t("lang.switch")}>
      {LANGS.map((lang) => (
        <button
          key={lang.code}
          type="button"
          className={`lang-switcher-btn ${current === lang.code ? "active" : ""}`}
          onClick={() => i18n.changeLanguage(lang.code)}
          aria-pressed={current === lang.code}
        >
          {lang.label}
        </button>
      ))}
    </div>
  );
}
