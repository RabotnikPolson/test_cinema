import React from 'react';
import { CheckoutStepper } from '../../../widgets/checkout-stepper/ui/CheckoutStepper';
import { CartSummary } from '../../../widgets/cart-summary/ui/CartSummary';
import { Link, Navigate } from 'react-router-dom';
import { useCartStore } from '../../../entities/cart/model/cartStore';
import { ArrowLeft } from 'lucide-react';
import { useCheckout } from '../../../features/checkout/model/useCheckout';
import './CheckoutPage.css';

export default function CheckoutPage() {
  const items = useCartStore(state => state.items);
  
  if (items.length === 0) {
    return <Navigate to="/shop" />;
  }

  return (
    <div className="checkout-page-container">
      <Link to="/shop/cart" className="back-link">
        <ArrowLeft size={16} /> Назад в корзину
      </Link>
      
      <h1 style={{ margin: '0 0 40px', fontFamily: 'var(--font-heading)', fontSize: '2.5rem' }}>Оформление заказа</h1>
      
      <div className="checkout-layout">
        <div>
          <CheckoutStepper />
        </div>
        
        <div style={{ position: 'sticky', top: '100px' }}>
          <CartSummary isCheckoutPage={true} />
        </div>
      </div>
    </div>
  );
}
