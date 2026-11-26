// ============================================================
// CinemaBook — Film domain types (Phase 2)
// ============================================================

export type FilmStatus =
  | 'PROCESSING'
  | 'READY'
  | 'PUBLISHED'
  | 'UNPUBLISHED'
  | 'REJECTED';

export interface Film {
  id: string;
  ownerId: string;
  ownerUsername: string;
  title: string;
  description?: string;
  genre?: string;
  tags?: string[];
  durationSeconds?: number;
  ticketPrice: number;
  currency: string;
  status: FilmStatus;
  thumbnailUrl?: string;
  trailerUrl?: string;
  releaseYear?: number;
  language?: string;
  ageRating?: string;
  totalTicketsSold: number;
  totalRevenue: number;
  createdAt: string;
  updatedAt: string;
}

export interface FilmPage {
  films: Film[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FilmUploadUrlResponse {
  filmId: string;
  uploadUrl: string;
  storageKey: string;
  expiresInSeconds: number;
}

export interface OwnerStats {
  totalFilms: number;
  publishedFilms: number;
  totalTicketsSold: number;
  totalRevenue: number;
  bankAccountLinked: boolean;
  payoutEnabled: boolean;
  stripeAccountId?: string;
}

export interface BankLinkResponse {
  onboardingUrl: string;
  stripeAccountId: string;
}

export interface BankLinkStatus {
  bankAccountLinked: boolean;
  payoutEnabled: boolean;
  stripeAccountCreated: boolean;
}

export interface PriceHistoryEntry {
  oldPrice?: number;
  newPrice: number;
  currency: string;
  changedAt: string;
}

// ── Request shapes ────────────────────────────────────────────

export interface CreateFilmRequest {
  title: string;
  description?: string;
  genre?: string;
  tags?: string[];
  ticketPrice: number;
  currency?: string;
  releaseYear?: number;
  language?: string;
  ageRating?: string;
}

export interface UpdateFilmRequest {
  title?: string;
  description?: string;
  genre?: string;
  tags?: string[];
  releaseYear?: number;
  language?: string;
  ageRating?: string;
}

export interface UpdatePriceRequest {
  newPrice: number;
}