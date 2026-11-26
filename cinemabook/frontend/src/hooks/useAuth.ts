import { useAuthStore } from '../store/authStore';
import type { UserType } from '../types';

/**
 * Convenience hook for auth state and helpers.
 * Components import this instead of the store directly —
 * easier to mock in tests and cleaner component code.
 *
 * Open/Closed: add new helpers here without touching the store.
 */
export function useAuth() {
  const { user, isAuthenticated, isLoading, login, logout } = useAuthStore();

  const isWatcher   = user?.userType === 'WATCHER';
  const isFilmOwner = user?.userType === 'FILM_OWNER';
  const isActive    = user?.accountStatus === 'ACTIVE';
  const isVerified  = user?.emailVerified === true;

  function hasRole(role: UserType): boolean {
    return user?.userType === role;
  }

  return {
    user,
    isAuthenticated,
    isLoading,
    isWatcher,
    isFilmOwner,
    isActive,
    isVerified,
    hasRole,
    login,
    logout,
  };
}
