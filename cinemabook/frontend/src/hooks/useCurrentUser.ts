import { useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { apiClient, tokenStorage } from '../services/apiClient';
import { useAuthStore } from '../store/authStore';
import type { ApiResponse, User } from '../types';

/**
 * Fetches the current user from /users/me on app startup.
 * Keeps the Zustand store in sync if the server-side user data changes.
 *
 * Only runs when an access token exists (i.e. user appears logged in).
 */
export function useCurrentUser() {
  const { setUser, clearAuth } = useAuthStore();
  const hasToken = Boolean(tokenStorage.getAccess());

  const query = useQuery({
    queryKey: ['currentUser'],
    queryFn: async () => {
      const res = await apiClient.get<ApiResponse<User>>('/users/me');
      return res.data.data!;
    },
    enabled: hasToken,
    staleTime: 5 * 60 * 1000,   // treat as fresh for 5 minutes
    retry: false,                 // don't retry 401s
  });

  useEffect(() => {
    if (query.data) {
      setUser(query.data);
    }
    if (query.isError) {
      // Token in storage is invalid — clear everything
      clearAuth();
    }
  }, [query.data, query.isError, setUser, clearAuth]);

  return query;
}
