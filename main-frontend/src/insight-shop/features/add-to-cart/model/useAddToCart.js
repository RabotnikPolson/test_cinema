import { useState } from 'react';
import { useCartStore } from '../../../entities/cart/model/cartStore';

export const useAddToCart = () => {
  const addItem = useCartStore(state => state.addItem);
  const [status, setStatus] = useState('idle'); // idle | loading | success

  const handleAddToCart = async (product) => {
    setStatus('loading');
    await addItem(product);
    setStatus('success');
    
    // Simulate toast
    alert(`Товар "${product.name}" добавлен в корзину`);
    
    setTimeout(() => {
      setStatus('idle');
    }, 2000);
  };

  return {
    handleAddToCart,
    status
  };
};
