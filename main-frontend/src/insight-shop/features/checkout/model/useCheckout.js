import { useState } from 'react';
import { useCartStore } from '../../../entities/cart/model/cartStore';

export const useCheckout = () => {
  const [step, setStep] = useState(1); // 1: Contacts, 2: Address, 3: Payment, 4: Success
  const [isProcessing, setIsProcessing] = useState(false);
  const clearCart = useCartStore(state => state.clearCart);

  const nextStep = () => setStep(s => Math.min(s + 1, 4));
  const prevStep = () => setStep(s => Math.max(s - 1, 1));

  const handleCheckout = async () => {
    setIsProcessing(true);
    // Mock API call
    await new Promise(res => setTimeout(res, 1500));
    setIsProcessing(false);
    clearCart();
    setStep(4);
  };

  return {
    step,
    nextStep,
    prevStep,
    handleCheckout,
    isProcessing,
  };
};
