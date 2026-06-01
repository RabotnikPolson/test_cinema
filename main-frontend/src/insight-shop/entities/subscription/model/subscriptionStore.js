import { create } from 'zustand';
import { subscriptionApi } from '../api/subscriptionApi';
import { useUserStore } from '../../user/model/userStore';

export const useSubscriptionStore = create((set, get) => ({
  plans: [],
  mySubscription: null,
  isLoading: false,
  error: null,

  fetchPlans: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await subscriptionApi.getSubscriptionPlans();
      set({ plans: response.data, isLoading: false });
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  fetchMySubscription: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await subscriptionApi.getMySubscription();
      set({ mySubscription: response.data, isLoading: false });
      // Sync with userStore to enable premium features globally
      if (response.data && response.data.status === 'ACTIVE' && response.data.planId === 'insight_plus') {
        useUserStore.getState().setPremium(true);
      } else {
        useUserStore.getState().setPremium(false);
      }
    } catch (err) {
      set({ error: err.message, isLoading: false });
    }
  },

  subscribe: async (planId) => {
    set({ isLoading: true, error: null });
    try {
      const response = await subscriptionApi.createSubscription(planId);
      set({ mySubscription: response.data, isLoading: false });
      
      if (response.data && response.data.planId === 'insight_plus') {
        useUserStore.getState().setPremium(true);
      }
      return true;
    } catch (err) {
      set({ error: err.message, isLoading: false });
      return false;
    }
  },

  cancelSubscription: async () => {
    set({ isLoading: true, error: null });
    try {
      const response = await subscriptionApi.cancelSubscription();
      set({ mySubscription: response.data, isLoading: false });
      return true;
    } catch (err) {
      set({ error: err.message, isLoading: false });
      return false;
    }
  }
}));

export const useSubscriptionSelectors = () => {
  const mySubscription = useSubscriptionStore(state => state.mySubscription);
  return {
    isActive: mySubscription !== null && mySubscription.status === 'ACTIVE',
    endDate: mySubscription?.endDate
  };
};
