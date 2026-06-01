import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import ru from "./locales/ru";
import kk from "./locales/kk";

const STORAGE_KEY = "insight_lang";
const SUPPORTED = ["ru", "kk"];

function readInitialLang() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (SUPPORTED.includes(saved)) return saved;
  } catch {
    /* localStorage недоступен — игнорируем */
  }
  return "ru";
}

const initialLang = readInitialLang();

i18n.use(initReactI18next).init({
  resources: {
    ru: { translation: ru },
    kk: { translation: kk },
  },
  lng: initialLang,
  fallbackLng: "ru",
  supportedLngs: SUPPORTED,
  interpolation: { escapeValue: false },
  returnNull: false,
});

i18n.on("languageChanged", (lng) => {
  try {
    localStorage.setItem(STORAGE_KEY, lng);
  } catch {
    /* игнорируем недоступность localStorage */
  }
  document.documentElement.lang = lng;
});

document.documentElement.lang = initialLang;

export default i18n;
