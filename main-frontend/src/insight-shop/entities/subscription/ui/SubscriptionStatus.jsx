import React from 'react';
import { useSubscriptionSelectors } from '../model/subscriptionStore';
import { PremiumBadge } from './PremiumBadge';

export const SubscriptionStatus = ({ subscription, onCancel, onChangePayment }) => {
  const { isActive, endDate } = useSubscriptionSelectors();

  if (!subscription) return null;

  return (
    <div style={{ padding: '20px', background: '#1c1c1c', borderRadius: '12px', border: '1px solid #333' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div>
          <h3 style={{ margin: 0, color: '#fff' }}>Ваш план: {subscription.planName}</h3>
          <p style={{ margin: '8px 0 0', color: '#999', fontSize: '0.9rem' }}>
            Статус: {subscription.status === 'ACTIVE' ? <span style={{ color: '#4caf50' }}>Активна</span> : <span style={{ color: '#f44336' }}>Отменена</span>}
          </p>
        </div>
        {subscription.planId === 'insight_plus' && <PremiumBadge showText />}
      </div>
      
      {subscription.status === 'ACTIVE' && endDate && (
        <p style={{ margin: '0 0 16px', color: '#ccc' }}>
          Следующее списание: {new Date(endDate).toLocaleDateString()} — {subscription.price} ₸
        </p>
      )}

      <div style={{ display: 'flex', gap: '12px' }}>
        <button onClick={onChangePayment} className="btn" style={{ background: '#333', color: '#fff', border: 'none', padding: '8px 16px', borderRadius: '4px', cursor: 'pointer' }}>
          Сменить способ оплаты
        </button>
        {subscription.status === 'ACTIVE' && (
          <button onClick={onCancel} className="btn" style={{ background: 'transparent', color: '#f44336', border: '1px solid #f44336', padding: '8px 16px', borderRadius: '4px', cursor: 'pointer' }}>
            Отменить подписку
          </button>
        )}
      </div>
    </div>
  );
};
