import React, { useState } from "react";
import { Check, X } from "lucide-react";
import "@/pages/subscription/ui/Subscription.css";

export default function SubscriptionPage() {
  const [demoMessage, setDemoMessage] = useState("");

  const handleSubscribe = () => {
    setDemoMessage("Ой! Это демо-витрина. Но мы оценили ваш вкус!");
    setTimeout(() => setDemoMessage(""), 4000);
  };

  return (
    <div className="sub-page">
      <header className="sub-header">
        <h1 className="sub-title">Выберите план</h1>
        <p className="sub-desc">Получите доступ к лучшим фильмам без ограничений. Поддержите развитие авторского кино в Казахстане.</p>
      </header>

      {demoMessage && (
        <div style={{
          marginBottom: "2rem",
          padding: "1rem",
          background: "rgba(201, 168, 76, 0.1)",
          border: "1px solid #C9A84C",
          color: "#D4D8E8",
          borderRadius: "4px",
          fontFamily: "var(--cv-font-body, 'DM Sans', sans-serif)",
          textAlign: "center"
        }}>
          {demoMessage}
        </div>
      )}

      <div className="sub-grid">
        <article className="sub-plan">
          <div className="sub-plan-name">Базовый</div>
          <div className="sub-plan-price">0 <span>₸ / мес</span></div>
          <ul className="sub-plan-features">
            <li className="sub-plan-feature"><Check size={18} /> Доступ к каталогу</li>
            <li className="sub-plan-feature"><Check size={18} /> Стандартное качество</li>
            <li className="sub-plan-feature disabled"><X size={18} /> Без рекламы</li>
            <li className="sub-plan-feature disabled"><X size={18} /> Оффлайн просмотр</li>
          </ul>
          <button className="sub-plan-btn" onClick={handleSubscribe}>Текущий план</button>
        </article>

        <article className="sub-plan sub-plan--pro">
          <div className="sub-plan-name">CineVerse+</div>
          <div className="sub-plan-price">1 990 <span>₸ / мес</span></div>
          <ul className="sub-plan-features">
            <li className="sub-plan-feature"><Check size={18} /> Полный каталог</li>
            <li className="sub-plan-feature"><Check size={18} /> 4K HDR качество</li>
            <li className="sub-plan-feature"><Check size={18} /> Без рекламы</li>
            <li className="sub-plan-feature"><Check size={18} /> Оффлайн просмотр</li>
          </ul>
          <button className="sub-plan-btn" onClick={handleSubscribe}>Оформить подписку</button>
        </article>
      </div>
    </div>
  );
}
