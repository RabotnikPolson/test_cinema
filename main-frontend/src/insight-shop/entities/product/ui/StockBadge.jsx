import React from 'react';

export const StockBadge = ({ stock }) => {
  if (stock <= 0) {
    return <span style={{ color: '#f44336', fontSize: '0.85rem' }}>Нет в наличии</span>;
  }
  if (stock < 10) {
    return <span style={{ color: '#ff9800', fontSize: '0.85rem' }}>Осталось {stock} шт.</span>;
  }
  return <span style={{ color: '#4caf50', fontSize: '0.85rem' }}>В наличии</span>;
};
