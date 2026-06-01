import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Check, X } from "lucide-react";
import "@/pages/subscription/ui/Subscription.css";

export default function SubscriptionPage() {
  const { t } = useTranslation();
  const [demoMessage, setDemoMessage] = useState("");

  const handleSubscribe = () => {
    setDemoMessage(t("subscription.demoMessage"));
    setTimeout(() => setDemoMessage(""), 4000);
  };

  return (
    <div className="sub-page">
      <header className="sub-header">
        <h1 className="sub-title">{t("subscription.title")}</h1>
        <p className="sub-desc">{t("subscription.desc")}</p>
      </header>

      {demoMessage && (
        <div style={{
          marginBottom: "2rem",
          padding: "1rem",
          background: "rgba(201, 168, 76, 0.1)",
          border: "1px solid #C9A84C",
          color: "#E0E0E0",
          borderRadius: "4px",
          fontFamily: "var(--cv-font-body, 'DM Sans', sans-serif)",
          textAlign: "center"
        }}>
          {demoMessage}
        </div>
      )}

      <div className="sub-grid">
        <article className="sub-plan">
          <div className="sub-plan-name">{t("subscription.basicName")}</div>
          <div className="sub-plan-price">0 <span>{t("subscription.perMonth")}</span></div>
          <ul className="sub-plan-features">
            <li className="sub-plan-feature"><Check size={18} /> {t("subscription.fCatalog")}</li>
            <li className="sub-plan-feature"><Check size={18} /> {t("subscription.fStandard")}</li>
            <li className="sub-plan-feature disabled"><X size={18} /> {t("subscription.fNoAds")}</li>
            <li className="sub-plan-feature disabled"><X size={18} /> {t("subscription.fOffline")}</li>
          </ul>
          <button className="sub-plan-btn" onClick={handleSubscribe}>{t("subscription.currentPlan")}</button>
        </article>

        <article className="sub-plan sub-plan--pro">
          <div className="sub-plan-name">Insight+</div>
          <div className="sub-plan-price">1 990 <span>{t("subscription.perMonth")}</span></div>
          <ul className="sub-plan-features">
            <li className="sub-plan-feature"><Check size={18} /> {t("subscription.fFullCatalog")}</li>
            <li className="sub-plan-feature"><Check size={18} /> {t("subscription.f4k")}</li>
            <li className="sub-plan-feature"><Check size={18} /> {t("subscription.fNoAds")}</li>
            <li className="sub-plan-feature"><Check size={18} /> {t("subscription.fOffline")}</li>
          </ul>
          <button className="sub-plan-btn" onClick={handleSubscribe}>{t("subscription.subscribe")}</button>
        </article>
      </div>
    </div>
  );
}

