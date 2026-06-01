import { useState } from 'react';
import { useSubscriptionStore } from '../../../entities/subscription/model/subscriptionStore';

export const useCancelSubscription = () => {
  const [isModalOpen, setModalOpen] = useState(false);
  const cancelSubscription = useSubscriptionStore(state => state.cancelSubscription);
  const isLoading = useSubscriptionStore(state => state.isLoading);
  const [error, setError] = useState('');

  const handleCancel = async () => {
    setError('');
    const success = await cancelSubscription();
    if (success) {
      setModalOpen(false);
    } else {
      setError('Ошибка при отмене подписки. Попробуйте еще раз.');
    }
  };

  return {
    isModalOpen,
    openModal: () => setModalOpen(true),
    closeModal: () => setModalOpen(false),
    handleCancel,
    isLoading,
    error
  };
};
