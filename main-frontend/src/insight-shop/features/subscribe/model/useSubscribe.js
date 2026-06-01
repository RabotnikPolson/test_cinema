import { useState } from 'react';
import { useSubscriptionStore } from '../../../entities/subscription/model/subscriptionStore';

export const useSubscribe = () => {
  const [isModalOpen, setModalOpen] = useState(false);
  const subscribe = useSubscriptionStore(state => state.subscribe);
  const isLoading = useSubscriptionStore(state => state.isLoading);
  const [error, setError] = useState('');

  const handleSubscribe = async (planId) => {
    setError('');
    const success = await subscribe(planId);
    if (success) {
      setModalOpen(false);
    } else {
      setError('Ошибка при оформлении подписки. Попробуйте еще раз.');
    }
  };

  return {
    isModalOpen,
    openModal: () => setModalOpen(true),
    closeModal: () => setModalOpen(false),
    handleSubscribe,
    isLoading,
    error
  };
};
