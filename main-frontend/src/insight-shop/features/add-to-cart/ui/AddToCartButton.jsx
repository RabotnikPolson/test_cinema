import React from 'react';
import { useAddToCart } from '../model/useAddToCart';

export const AddToCartButton = ({ product }) => {
  const { handleAddToCart, status } = useAddToCart();

  if (product.stock <= 0) {
    return (
      <button className="shop-buy-btn" disabled style={{ background: '#333', color: '#666', cursor: 'not-allowed' }}>
        Нет в наличии
      </button>
    );
  }

  if (status === 'success') {
    return (
      <button className="shop-buy-btn" style={{ background: '#4caf50', color: '#fff' }}>
        ✓ Добавлено
      </button>
    );
  }

  return (
    <button 
      className="shop-buy-btn" 
      onClick={() => handleAddToCart(product)}
      disabled={status === 'loading'}
    >
      {status === 'loading' ? 'Добавление...' : 'В корзину'}
    </button>
  );
};
