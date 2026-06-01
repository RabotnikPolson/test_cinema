import React from 'react';
import { Check, X } from 'lucide-react';
import { formatPrice } from '../../../shared/lib/formatPrice';

export const PlanCard = ({ plan, actionSlot, isYearly }) => {
  const price = isYearly ? Math.floor(plan.price * 12 * 0.8) : plan.price;
  const period = isYearly ? 'год' : plan.period;

  return (
    <article className={`sub-plan ${plan.isPremium ? 'sub-plan--pro' : ''}`}>
      <div className="sub-plan-name">{plan.name}</div>
      <div className="sub-plan-price">
        {price === 0 ? '0' : formatPrice(price).replace(' ₸', '')} 
        <span>₸ / {period}</span>
      </div>
      <ul className="sub-plan-features">
        {plan.features.map(f => (
          <li key={f.id} className={`sub-plan-feature ${!f.included ? 'disabled' : ''}`}>
            {f.included ? <Check size={18} strokeWidth={3} /> : <X size={18} strokeWidth={3} />}
            {f.text}
          </li>
        ))}
      </ul>
      {actionSlot}
    </article>
  );
};
