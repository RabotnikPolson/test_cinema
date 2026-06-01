import React from 'react';
import { useUserStore } from '../../user/model/userStore';
import { formatPrice } from '../../../shared/lib/formatPrice';
import { getPremiumPrice } from '../../../shared/lib/getPremiumPrice';

export const ProductPrice = ({ price, className = '' }) => {
  const isPremiumUser = useUserStore(state => state.isPremiumUser);

  if (isPremiumUser) {
    const discounted = getPremiumPrice(price);
    return (
      <div className={`product-price discounted ${className}`}>
        <span className="old-price">
          {formatPrice(price)}
        </span>
        <span className="current-price">
          {formatPrice(discounted)}
        </span>
      </div>
    );
  }

  return (
    <div className={`product-price ${className}`}>
      {formatPrice(price)}
    </div>
  );
};
