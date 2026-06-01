import React, { useEffect } from 'react';
import { useSubscriptionStore } from '../../../entities/subscription/model/subscriptionStore';
import { PlanCard } from '../../../entities/subscription/ui/PlanCard';
import { SubscribeButton } from '../../../features/subscribe/ui/SubscribeButton';

export const PlanComparisonTable = ({ isYearly = false }) => {
  const plans = useSubscriptionStore(state => state.plans);
  const isLoading = useSubscriptionStore(state => state.isLoading);
  const fetchPlans = useSubscriptionStore(state => state.fetchPlans);

  useEffect(() => {
    if (plans.length === 0) {
      fetchPlans();
    }
  }, [plans, fetchPlans]);

  if (isLoading) {
    return <div style={{ textAlign: 'center', padding: '40px', color: '#ccc' }}>Загрузка тарифов...</div>;
  }

  return (
    <div className="sub-grid">
      {plans.map(plan => (
        <PlanCard 
          key={plan.id} 
          plan={plan} 
          isYearly={isYearly}
          actionSlot={<SubscribeButton plan={plan} />}
        />
      ))}
    </div>
  );
};
