import React from 'react';

export const SubscribeModal = ({ plan, onClose, onConfirm, isLoading, error }) => {
  return (
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.8)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
      <div style={{ background: '#1c1c1c', padding: '32px', borderRadius: '12px', width: '100%', maxWidth: '400px', border: '1px solid #333' }}>
        <h2 style={{ marginTop: 0, color: '#fff' }}>Оформление подписки</h2>
        <p style={{ color: '#ccc' }}>План: <strong>{plan.name}</strong></p>
        <p style={{ color: '#ccc' }}>К оплате: <strong style={{ fontSize: '1.2rem', color: '#C9A84C' }}>{plan.price} ₸</strong></p>
        
        {error && <div style={{ color: '#f44336', marginTop: '16px', fontSize: '0.9rem' }}>{error}</div>}
        
        <div style={{ display: 'flex', gap: '12px', marginTop: '32px' }}>
          <button onClick={onClose} disabled={isLoading} style={{ flex: 1, padding: '12px', background: 'transparent', color: '#fff', border: '1px solid #555', borderRadius: '4px', cursor: 'pointer' }}>
            Отмена
          </button>
          <button onClick={onConfirm} disabled={isLoading} style={{ flex: 2, padding: '12px', background: '#C9A84C', color: '#000', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold' }}>
            {isLoading ? 'Оформляем...' : 'Оплатить'}
          </button>
        </div>
      </div>
    </div>
  );
};
