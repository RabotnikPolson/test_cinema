import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// Сохраняем состояние подписки (и юзера) в localStorage фронтенда
export const useUserStore = create(
  persist(
    (set, get) => ({
      user: {
        username: 'Demo User',
        email: 'demo@example.com',
      },
      isLoggedIn: true,
      isPremiumUser: false,

      setPremium: (isPremium) => set({ isPremiumUser: isPremium }),
      
      logout: () => set({ isLoggedIn: false, user: null, isPremiumUser: false })
    }),
    {
      name: 'insight-user-storage', // уникальное имя для ключа в localStorage
    }
  )
);
