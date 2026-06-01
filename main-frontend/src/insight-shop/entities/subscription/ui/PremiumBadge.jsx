import React from 'react';
import { Flame } from 'lucide-react';

export const PremiumBadge = ({ showText = false, className = '' }) => {
  return (
    <div 
      className={`premium-badge ${className}`}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        padding: '2px 8px',
        background: 'rgba(201, 168, 76, 0.15)',
        border: '1px solid #C9A84C',
        color: '#C9A84C',
        borderRadius: '12px',
        fontSize: '0.75rem',
        fontWeight: 'bold',
        textTransform: 'uppercase'
      }}
    >
      <Flame size={12} fill="#C9A84C" />
      {showText && <span>Premium</span>}
    </div>
  );
};
