import React from 'react';
import { ProductCard } from './ProductCard';

export const ProductGrid = ({ products, isLoading }) => {
  if (isLoading) {
    return <div style={{ padding: '40px', textAlign: 'center', color: '#ccc' }}>Загрузка товаров...</div>;
  }

  if (!products || products.length === 0) {
    return <div style={{ padding: '40px', textAlign: 'center', color: '#ccc' }}>Товары не найдены</div>;
  }

  return (
    <div className="shop-grid">
      {products.map(product => (
        <ProductCard key={product.id} product={product} />
      ))}
    </div>
  );
};
