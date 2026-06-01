import React from 'react';
import { ProductPrice } from '../../product/ui/ProductPrice';

export const CartItem = ({ item, quantityControlsSlot }) => {
  return (
    <div style={{ 
      display: 'flex', gap: '20px', padding: '24px', 
      borderBottom: '1px solid rgba(255,255,255,0.05)',
      background: 'rgba(30, 30, 40, 0.2)'
    }}>
      <div 
        style={{ 
          width: '100px', height: '100px', borderRadius: '12px', 
          background: item.product.imageColor, display: 'flex', alignItems: 'center', justifyContent: 'center',
          overflow: 'hidden'
        }}
      >
        {item.product.image ? (
          <img src={item.product.image} alt={item.product.name} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        ) : (
          <span style={{ fontSize: '32px', opacity: 0.5 }}>📦</span>
        )}
      </div>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
        <div>
          <div style={{ color: '#a0a0b0', fontSize: '0.8rem', textTransform: 'uppercase', marginBottom: '4px' }}>
            {item.product.category || 'Мерч'}
          </div>
          <h4 style={{ margin: '0 0 8px', color: '#fff', fontSize: '1.2rem', fontWeight: '600' }}>{item.product.name}</h4>
          <ProductPrice price={item.product.price} />
        </div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {quantityControlsSlot}
      </div>
    </div>
  );
};
