import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useCartStore } from '../../../entities/cart/model/cartStore';
import { CartItem } from '../../../entities/cart/ui/CartItem';
import { CartSummary } from '../../../widgets/cart-summary/ui/CartSummary';
import { ArrowLeft, Trash2, ShoppingBag } from 'lucide-react';
import './CartPage.css';

export default function CartPage() {
  const items = useCartStore(state => state.items);
  const updateQuantity = useCartStore(state => state.updateQuantity);
  const removeItem = useCartStore(state => state.removeItem);
  const navigate = useNavigate();

  if (items.length === 0) {
    return (
      <div className="cart-page-container">
        <Link to="/shop" className="back-link">
          <ArrowLeft size={16} /> Назад в каталог
        </Link>
        <div className="cart-empty-state">
          <div className="cart-empty-icon">
            <ShoppingBag size={64} />
          </div>
          <h2 className="cart-empty-title">Ваша корзина пуста</h2>
          <p className="cart-empty-desc">Самое время добавить в неё что-нибудь классное из нашего премиум магазина.</p>
          <Link to="/shop" className="cart-empty-btn">
            Перейти в магазин
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="cart-page-container">
      <Link to="/shop" className="back-link">
        <ArrowLeft size={16} /> Назад в каталог
      </Link>
      
      <h1 style={{ margin: '0 0 40px', fontFamily: 'var(--font-heading)', fontSize: '2.5rem' }}>Корзина</h1>
      
      <div className="cart-layout">
        <div className="cart-items-container">
          {items.map(item => (
            <CartItem 
              key={item.id} 
              item={item} 
              quantityControlsSlot={
                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                  <div className="qty-controls">
                    <button 
                      onClick={() => updateQuantity(item.id, Math.max(1, item.quantity - 1))}
                      className="qty-btn"
                    >−</button>
                    <span className="qty-value">{item.quantity}</span>
                    <button 
                      onClick={() => updateQuantity(item.id, item.quantity + 1)}
                      className="qty-btn"
                    >+</button>
                  </div>
                  <button 
                    onClick={() => removeItem(item.id)}
                    className="remove-item-btn"
                    title="Удалить"
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
              }
            />
          ))}
        </div>
        
        <div style={{ position: 'sticky', top: '100px' }}>
          <CartSummary onCheckout={() => navigate('/shop/checkout')} />
        </div>
      </div>
    </div>
  );
}
