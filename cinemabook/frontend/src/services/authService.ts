import { apiClient, deviceId, tokenStorage } from './apiClient';
import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  RegisterFilmOwnerRequest,
  RegisterResponse,
  RegisterWatcherRequest,
} from '../types';

export const authService = {

  async registerWatcher(data: RegisterWatcherRequest): Promise<RegisterResponse> {
    const res = await apiClient.post<ApiResponse<RegisterResponse>>(
      '/auth/register/watcher', data
    );
    return res.data.data!;
  },

  async registerFilmOwner(data: RegisterFilmOwnerRequest): Promise<RegisterResponse> {
    const res = await apiClient.post<ApiResponse<RegisterResponse>>(
      '/auth/register/film-owner', data
    );
    return res.data.data!;
  },

  async login(data: Omit<LoginRequest, 'deviceId' | 'deviceInfo'>): Promise<AuthResponse> {
    const payload: LoginRequest = {
      ...data,
      deviceId,
      deviceInfo: navigator.userAgent,
    };
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', payload);
    const auth = res.data.data!;

    // Persist tokens immediately
    tokenStorage.setTokens(auth.accessToken, auth.refreshToken);
    return auth;
  },

  async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout');
    } finally {
      tokenStorage.clearTokens();
    }
  },
};
