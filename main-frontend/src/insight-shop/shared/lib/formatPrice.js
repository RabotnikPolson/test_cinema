export const formatPrice = (price) => {
  if (typeof price !== 'number') return price;
  return `${price.toLocaleString('ru-RU')} ₸`;
};
