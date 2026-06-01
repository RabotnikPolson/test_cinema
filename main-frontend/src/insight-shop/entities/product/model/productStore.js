import { create } from 'zustand';
import { productApi } from '../api/productApi';

export const useProductStore = create((set, get) => ({
  products: {},
  isLoading: false,
  error: null,
  
  filters: {
    category: 'all',
    priceRange: [0, 50000],
    inStockOnly: false,
    sortBy: 'popular', // popular, new, priceAsc, priceDesc
  },

  setFilter: (key, value) => {
    set(state => ({
      filters: { ...state.filters, [key]: value }
    }));
  },
  
  resetFilters: () => {
    set({
      filters: {
        category: 'all',
        priceRange: [0, 50000],
        inStockOnly: false,
        sortBy: 'popular',
      }
    });
  },

  fetchProducts: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await productApi.getProducts();
      const productsMap = {};
      response.data.forEach(p => {
        productsMap[p.id] = p;
      });
      set({ products: productsMap, isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  getProductBySlug: async (slug) => {
    // Check if we already have it
    const existing = Object.values(get().products).find(p => p.slug === slug);
    if (existing) return existing;

    set({ isLoading: true, error: null });
    try {
      const response = await productApi.getProductBySlug(slug);
      set(state => ({
        products: { ...state.products, [response.data.id]: response.data },
        isLoading: false
      }));
      return response.data;
    } catch (err) {
      set({ error: err.message, isLoading: false });
      return null;
    }
  }
}));
