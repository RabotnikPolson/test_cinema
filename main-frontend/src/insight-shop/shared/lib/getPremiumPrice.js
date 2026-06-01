export const getPremiumPrice = (price) => {
  if (typeof price !== 'number') return price;
  // 10% discount for premium
  return Math.round(price * 0.9);
};
