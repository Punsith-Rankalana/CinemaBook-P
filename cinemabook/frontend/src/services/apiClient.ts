import axios, { AxiosInstance, InternalAxiosRequestConfig } from 'axios';
import { AuthResponse } from '../types';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8082/api/v1';

// ── Token storage keys ────────────────────────────────────────
const ACCESS_TOKEN_KEY  = 'cb_access_token';
const REFRESH_TOKEN_KEY = 'cb_refresh_token';
const DEVICE_ID_KEY     = 'cb_device_id';

// ── Device ID: stable per browser ────────────────────────────
function getOrCreateDeviceId(): string {
  let id = localStorage.getItem(DEVICE_ID_KEY);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(DEVICE_ID_KEY, id);
  }
  return id;
}

export const deviceId = getOrCreateDeviceId();

// ── Token helpers ─────────────────────────────────────────────
export const tokenStorage = {
  getAccess:      ()      => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefresh:     ()      => localStorage.getItem(REFRESH_TOKEN_KEY),
  setTokens:      (a: string, r: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, a);
    localStorage.setItem(REFRESH_TOKEN_KEY, r);
  },
  clearTokens:    ()      => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};

// ── Axios instance ────────────────────────────────────────────
export const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// ── Request interceptor: attach access token ──────────────────
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenStorage.getAccess();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    config.headers['X-Device-Id'] = deviceId;
    return config;
  },
  (error) => Promise.reject(error)
);

// ── Response interceptor: auto-refresh on 401 ────────────────
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token);
  });
  failedQueue = [];
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status !== 401 || originalRequest._retry) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then((token) => {
        originalRequest.headers.Authorization = `Bearer ${token}`;
        return apiClient(originalRequest);
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const refreshToken = tokenStorage.getRefresh();
      if (!refreshToken) throw new Error('No refresh token');

      const { data } = await axios.post<{ data: AuthResponse }>(
        `${BASE_URL}/auth/refresh`,
        { refreshToken },
        { headers: { 'X-Device-Id': deviceId } }
      );

      const { accessToken, refreshToken: newRefresh } = data.data;
      tokenStorage.setTokens(accessToken, newRefresh);

      processQueue(null, accessToken);
      originalRequest.headers.Authorization = `Bearer ${accessToken}`;
      return apiClient(originalRequest);

    } catch (refreshError) {
      processQueue(refreshError, null);
      tokenStorage.clearTokens();
      // Redirect to login
      window.location.href = '/login';
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);
