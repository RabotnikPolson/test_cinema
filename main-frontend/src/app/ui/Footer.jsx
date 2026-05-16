import { Link } from "react-router-dom";
import "@/shared/styles/layout/Footer.css";

export default function Footer() {
  return (
    <footer className="app-footer glass">
      <div className="footer-content">
        <div className="footer-links">
          <Link to="/terms">Пользовательское соглашение</Link>
          <Link to="/privacy">Политика конфиденциальности</Link>
          <Link to="/contacts">Контакты</Link>
        </div>
        <div className="footer-verified">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="verified-icon"><path d="M12 22c5.523 0 10-4.477 10-10S17.523 2 12 2 2 6.477 2 12s4.477 10 10 10z"></path><path d="m9 12 2 2 4-4"></path></svg>
          Официальный подтвержденный сервис
        </div>
        <div className="footer-copy">
          &copy; {new Date().getFullYear()} CineVerse. Премиальная платформа для просмотра фильмов и сериалов.
        </div>
      </div>
    </footer>
  );
}
