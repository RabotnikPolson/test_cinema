import { create } from 'zustand';
import { cartApi } from '../api/cartApi';

export const useCartStore = create((set, get) => ({
  items: [],
  isLoading: false,
  error: null,

  fetchCart: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await cartApi.getCart();
      set({ items: response.data, isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  addItem: async (product, quantity = 1) => {
    set({ isLoading: true, error: null });
    try {
      const response = await cartApi.addItem(product, quantity);
      set({ items: response.data, isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  updateQuantity: async (itemId, quantity) => {
    set({ isLoading: true, error: null });
    try {
      const response = await cartApi.updateItem(itemId, quantity);
      set({ items: response.data, isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  removeItem: async (itemId) => {
    set({ isLoading: true, error: null });
    try {
      const response = await cartApi.removeItem(itemId);
      set({ items: response.data, isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  clearCart: async () => {
    set({ isLoading: true, error: null });
    try {
      await cartApi.clearCart();
      set({ items: [], isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },
  
  getCartTotalCount: () => {
    return get().items.reduce((total, item) => total + item.quantity, 0);
  }
}));
