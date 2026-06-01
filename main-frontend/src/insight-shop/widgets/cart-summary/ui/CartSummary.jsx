import React from 'react';
import { useCartStore } from '../../../entities/cart/model/cartStore';
import { useUserStore } from '../../../entities/user/model/userStore';
import { formatPrice } from '../../../shared/lib/formatPrice';
import { getPremiumPrice } from '../../../shared/lib/getPremiumPrice';

export const CartSummary = ({ onCheckout, isCheckoutPage = false }) => {
  const items = useCartStore(state => state.items);
  const isPremiumUser = useUserStore(state => state.isPremiumUser);

  const subtotal = items.reduce((acc, item) => acc + (item.product.price * item.quantity), 0);
  const total = isPremiumUser ? getPremiumPrice(subtotal) : subtotal;
  const discount = subtotal - total;

  if (items.length === 0) return null;

  return (
    <div style={{ 
      background: 'rgba(30, 30, 40, 0.4)', 
      backdropFilter: 'blur(20px)',
      padding: '32px', 
      borderRadius: '24px', 
      border: '1px solid rgba(255, 255, 255, 0.05)',
      boxShadow: '0 16px 40px rgba(0,0,0,0.3)'
    }}>
      <h3 style={{ margin: '0 0 24px', color: '#fff', fontSize: '1.5rem', fontFamily: 'var(--font-heading)' }}>Ваш заказ</h3>
      
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px', color: '#a0a0b0', fontWeight: '500' }}>
        <span>Товары ({items.reduce((a, i) => a + i.quantity, 0)})</span>
        <span>{formatPrice(subtotal)}</span>
      </div>

      {isPremiumUser && discount > 0 && (
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px', color: '#C9A84C', fontWeight: '600' }}>
          <span>Premium-скидка 10%</span>
          <span>− {formatPrice(discount)}</span>
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '24px', color: '#a0a0b0', fontWeight: '500' }}>
        <span>Доставка</span>
        <span>{isCheckoutPage ? 'Бесплатно' : 'Пока не рассчитано'}</span>
      </div>

      <div style={{ borderTop: '1px solid rgba(255, 255, 255, 0.1)', paddingTop: '24px', marginBottom: '32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <strong style={{ color: '#fff', fontSize: '1.4rem' }}>Итого</strong>
        <strong style={{ color: '#fff', fontSize: '1.6rem', fontFamily: 'var(--font-heading)' }}>{formatPrice(total)}</strong>
      </div>

      {!isCheckoutPage && (
        <button 
          onClick={onCheckout}
          style={{ 
            width: '100%', 
            padding: '16px', 
            background: '#C9A84C', 
            color: '#000', 
            border: 'none', 
            borderRadius: '12px', 
            cursor: 'pointer', 
            fontWeight: '700', 
            fontSize: '1.1rem',
            textTransform: 'uppercase',
            letterSpacing: '1px',
            transition: 'all 0.3s'
          }}
          onMouseOver={(e) => { e.target.style.background = '#e6c86a'; e.target.style.transform = 'translateY(-2px)'; }}
          onMouseOut={(e) => { e.target.style.background = '#C9A84C'; e.target.style.transform = 'translateY(0)'; }}
        >
          Оформить заказ
        </button>
      )}
    </div>
  );
};
