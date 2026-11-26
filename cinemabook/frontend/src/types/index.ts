// ============================================================
// CinemaBook — Core TypeScript Types (Phase 1)
// ============================================================

export type UserType = 'WATCHER' | 'FILM_OWNER';

export type AccountStatus =
  | 'PENDING_VERIFICATION'
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'DEACTIVATED';

export interface User {
  id: string;
  email: string;
  username: string;
  displayName: string;
  userType: UserType;
  accountStatus: AccountStatus;
  emailVerified: boolean;
  avatarUrl?: string;
}

// ── Auth ─────────────────────────────────────────────────────

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresInMs: number;
}

export interface AuthResponse extends AuthTokens {
  user: User;
}

export interface LoginRequest {
  login: string;      // email or username
  password: string;
  deviceId: string;
  deviceInfo?: string;
}

export interface RegisterWatcherRequest {
  email: string;
  username: string;
  password: string;
  displayName?: string;
}

export interface RegisterFilmOwnerRequest {
  email: string;
  username: string;
  password: string;
  companyName?: string;
  displayName?: string;
}

export interface RegisterResponse {
  message: string;
  userId: string;
}

// ── API envelope ─────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  timestamp: string;
}
