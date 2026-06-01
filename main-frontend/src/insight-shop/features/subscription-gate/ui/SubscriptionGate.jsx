import React from 'react';
import { useUserStore } from '../../../entities/user/model/userStore';
import { Link } from 'react-router-dom';

export const SubscriptionGate = ({ children, fallback }) => {
  const isPremiumUser = useUserStore(state => state.isPremiumUser);

  if (isPremiumUser) {
    return <>{children}</>;
  }

  if (fallback) {
    return <>{fallback}</>;
  }

  return (
    <div style={{ padding: '24px', background: 'rgba(201, 168, 76, 0.1)', border: '1px solid #C9A84C', borderRadius: '8px', textAlign: 'center' }}>
      <h3 style={{ color: '#C9A84C', margin: '0 0 12px 0' }}>Доступно только в Insight+</h3>
      <p style={{ color: '#fff', margin: '0 0 16px 0' }}>Оформите подписку, чтобы получить доступ к этому контенту.</p>
      <Link to="/subscription" style={{ display: 'inline-block', padding: '10px 20px', background: '#C9A84C', color: '#000', textDecoration: 'none', borderRadius: '4px', fontWeight: 'bold' }}>
        Узнать больше
      </Link>
    </div>
  );
};
