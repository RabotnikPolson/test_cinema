import React from "react";
import { useAuth } from "@/features/auth";
import { useSubscription } from "@/features/subscription";
import "@/shared/styles/pages/Subscription.css";

export default function SubscriptionPage() {
  const { user } = useAuth();
  const username = user?.username;
  const { data: sub, isLoading, subscribe, cancel } = useSubscription(username);

  if (!username) {
    return (
      <div className="container subscription-page">
        <p>Войдите для управления подпиской</p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="container subscription-page">
        <p>Загрузка...</p>
      </div>
    );
  }

  const onSubscribe = async () => {
    try {
      await subscribe({ plan: "pro" });
    } catch {}
  };

  const onCancel = async () => {
    try {
      await cancel();
    } catch {}
  };

  return (
    <div className="container subscription-page">
      <h1>Подписка</h1>
      <div className="sub-card">
        <div>
          Текущий план: <strong>{sub?.plan || "free"}</strong>
        </div>
        <div>
          Статус: <strong>{sub?.status || "none"}</strong>
        </div>

        {sub?.status === "active" ? (
          <button onClick={onCancel} className="button button--ghost">
            Отменить подписку
          </button>
        ) : (
          <button onClick={onSubscribe} className="button">
            Оформить PRO
          </button>
        )}
      </div>
    </div>
  );
}
