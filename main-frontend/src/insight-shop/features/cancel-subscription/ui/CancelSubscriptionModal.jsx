import React from 'react';

export const CancelSubscriptionModal = ({ onClose, onConfirm, isLoading, error }) => {
  return (
    <div style={{ 
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, 
      background: 'rgba(10, 12, 16, 0.85)', 
      backdropFilter: 'blur(16px)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 
    }}>
      <div style={{ 
        background: 'rgba(30, 30, 40, 0.6)', 
        backdropFilter: 'blur(20px)',
        padding: '40px', 
        borderRadius: '24px', 
        width: '100%', maxWidth: '440px', 
        border: '1px solid rgba(255, 255, 255, 0.05)',
        boxShadow: '0 24px 64px rgba(0,0,0,0.5)',
        textAlign: 'center'
      }}>
        <div style={{ width: '64px', height: '64px', background: 'rgba(244, 67, 54, 0.1)', color: '#f44336', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 24px', fontSize: '32px' }}>
          😢
        </div>
        
        <h2 style={{ marginTop: 0, color: '#fff', fontFamily: 'var(--font-heading)', fontSize: '1.8rem', marginBottom: '16px' }}>Отмена подписки</h2>
        <p style={{ color: '#a0a0b0', lineHeight: '1.6', marginBottom: '24px' }}>Вы уверены, что хотите отказаться от Insight Premium? Вы потеряете доступ к эксклюзивным фильмам и скидку 20% в магазине мерча.</p>
        
        {error && <div style={{ color: '#f44336', marginBottom: '16px', fontSize: '0.9rem' }}>{error}</div>}
        
        <div style={{ display: 'flex', gap: '16px', flexDirection: 'column' }}>
          <button onClick={onClose} disabled={isLoading} style={{ 
            padding: '16px', background: '#C9A84C', color: '#000', border: 'none', borderRadius: '12px', cursor: 'pointer', fontWeight: 'bold', fontSize: '1rem', transition: 'all 0.3s' 
          }}>
            Передумал, остаюсь
          </button>
          <button onClick={onConfirm} disabled={isLoading} style={{ 
            padding: '16px', background: 'transparent', color: '#a0a0b0', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '12px', cursor: 'pointer', transition: 'all 0.3s' 
          }}>
            {isLoading ? 'Отменяем...' : 'Всё равно отменить'}
          </button>
        </div>
      </div>
    </div>
  );
};
