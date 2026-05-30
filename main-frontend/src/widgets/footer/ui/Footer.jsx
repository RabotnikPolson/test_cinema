import { Link } from "react-router-dom";
import "@/widgets/footer/ui/Footer.css";

export default function Footer() {
  return (
    <footer className="app-footer">
      <div className="footer-content">
        <div className="footer-links">
          <Link to="/terms">Пользовательское соглашение</Link>
          <Link to="/privacy">Политика конфиденциальности</Link>
          <Link to="/contacts">Контакты</Link>
        </div>
        <div className="footer-verified">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="verified-icon"
          >
            <path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z" />
            <path d="m9 12 2 2 4-4" />
          </svg>
          Официальный подтверждённый сервис
        </div>
        <div className="footer-copy">
          © {new Date().getFullYear()} Insight. Платформа для тихого домашнего кинозала.
        </div>
      </div>
    </footer>
  );
}
