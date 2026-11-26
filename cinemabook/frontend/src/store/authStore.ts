import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '../types';
import { authService } from '../services/authService';
import { tokenStorage } from '../services/apiClient';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  // Actions
  login: (login: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  setUser: (user: User) => void;
  clearAuth: () => void;
}

/**
 * Global auth store using Zustand.
 * Persists `user` to localStorage so the UI survives a page refresh.
 * Tokens are stored separately in tokenStorage (also localStorage).
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user:            null,
      isAuthenticated: false,
      isLoading:       false,

      login: async (login, password) => {
        set({ isLoading: true });
        try {
          const auth = await authService.login({ login, password });
          set({
            user:            auth.user,
            isAuthenticated: true,
            isLoading:       false,
          });
        } catch (err) {
          set({ isLoading: false });
          throw err;
        }
      },

      logout: async () => {
        set({ isLoading: true });
        await authService.logout();
        set({ user: null, isAuthenticated: false, isLoading: false });
      },

      setUser: (user) => set({ user, isAuthenticated: true }),

      clearAuth: () => {
        tokenStorage.clearTokens();
        set({ user: null, isAuthenticated: false });
      },
    }),
    {
      name: 'cb-auth',
      // Only persist the user object; tokens stay in tokenStorage
      partialize: (state) => ({ user: state.user, isAuthenticated: state.isAuthenticated }),
    }
  )
);
