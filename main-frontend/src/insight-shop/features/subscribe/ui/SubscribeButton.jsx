import React from 'react';
import { useSubscriptionSelectors } from '../../../entities/subscription/model/subscriptionStore';
import { useSubscribe } from '../model/useSubscribe';
import { SubscribeModal } from './SubscribeModal';

export const SubscribeButton = ({ plan }) => {
  const { isActive } = useSubscriptionSelectors();
  const { isModalOpen, openModal, closeModal, handleSubscribe, isLoading, error } = useSubscribe();

  // Если у юзера активна подписка, и это карточка Premium
  if (isActive && plan.isPremium) {
    return <button className="sub-plan-btn" style={{ background: '#333', color: '#888', cursor: 'not-allowed' }} disabled>Текущий план</button>;
  }
  
  // Если у юзера активна подписка, и это карточка Basic
  if (isActive && !plan.isPremium) {
    return <button className="sub-plan-btn" style={{ background: '#1a1a1a', color: '#555', cursor: 'not-allowed' }} disabled>Базовый тариф</button>;
  }

  // Если подписки нет, и это карточка Basic
  if (!isActive && !plan.isPremium) {
    return <button className="sub-plan-btn" style={{ background: '#333', color: '#888', cursor: 'not-allowed' }} disabled>Текущий план</button>;
  }

  return (
    <>
      <button className="sub-plan-btn" onClick={openModal}>
        Оформить {plan.name || 'Insight+'}
      </button>
      
      {isModalOpen && (
        <SubscribeModal 
          plan={plan} 
          onClose={closeModal} 
          onConfirm={() => handleSubscribe(plan.id)}
          isLoading={isLoading}
          error={error}
        />
      )}
    </>
  );
};
