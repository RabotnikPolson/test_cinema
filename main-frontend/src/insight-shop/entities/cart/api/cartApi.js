const delay = (ms) => new Promise(res => setTimeout(res, ms));

let mockCart = [];

export const cartApi = {
  getCart: async () => {
    await delay(300);
    return { data: [...mockCart] };
  },
  
  addItem: async (product, quantity = 1) => {
    await delay(400);
    const existing = mockCart.find(item => item.product.id === product.id);
    if (existing) {
      existing.quantity += quantity;
    } else {
      mockCart.push({
        id: `ci_${Math.random().toString(36).substring(7)}`,
        product,
        quantity
      });
    }
    return { data: [...mockCart] };
  },
  
  updateItem: async (itemId, quantity) => {
    await delay(300);
    const item = mockCart.find(i => i.id === itemId);
    if (item) {
      item.quantity = quantity;
    }
    return { data: [...mockCart] };
  },
  
  removeItem: async (itemId) => {
    await delay(300);
    mockCart = mockCart.filter(i => i.id !== itemId);
    return { data: [...mockCart] };
  },
  
  clearCart: async () => {
    await delay(300);
    mockCart = [];
    return { data: [] };
  }
};
