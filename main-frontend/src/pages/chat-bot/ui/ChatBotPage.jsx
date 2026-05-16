import { Link } from "react-router-dom";
import "@/shared/styles/pages/ChatBot.css";

export default function ChatBotPage() {
  return (
    <div className="chatbot-page glass">
      <div className="chatbot-panel">
        <h1>Чат-помощник CineVerse</h1>
        <p>
          В будущем здесь появится интеллектуальный чат-помощник для выбора фильмов, помощи в поиске и
          быстрых советов по просмотру.
        </p>
        <div className="chatbot-footer">
          <Link to="/" className="button button--ghost">
            Вернуться на главную
          </Link>
          <span>Пока что это заглушка.</span>
        </div>
      </div>
    </div>
  );
}
