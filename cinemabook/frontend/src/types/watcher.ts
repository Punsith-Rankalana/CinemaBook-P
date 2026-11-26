// ============================================================
// CinemaBook — Watcher / Streaming types (Phase 3)
// ============================================================

export interface Ticket {
  id: string;
  filmId: string;
  filmTitle: string;
  filmThumbnailUrl?: string;
  pricePaid: number;
  currency: string;
  status: 'ACTIVE' | 'USED' | 'EXPIRED' | 'REFUNDED';
  clipSecondsUsed: number;
  clipSecondsRemaining: number;
  watchPositionSec: number;
  purchasedAt: string;
  expiresAt?: string;
  lastWatchedAt?: string;
}

export interface TicketPage {
  tickets: Ticket[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface StreamSession {
  sessionId: string;
  streamUrl: string;
  filmTitle: string;
  durationSeconds: number;
  resumePositionSec: number;
  clipSecondsRemaining: number;
  expiresAt: string;
  widevineEnabled: boolean;
}

export interface ClipJob {
  jobId: string;
  filmId: string;
  status: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';
  clipUrl?: string;
  startSeconds: number;
  durationSeconds: number;
  errorMessage?: string;
  createdAt: string;
}

export interface PaymentConfirmResponse {
  ticketId: string;
  filmTitle: string;
  pricePaid: number;
  currency: string;
  purchasedAt: string;
  message: string;
}

// ── Requests ──────────────────────────────────────────────────

export interface PurchaseTicketRequest {
  filmId: string;
  paymentToken: string;
}

export interface RequestClipRequest {
  startSeconds: number;
  durationSeconds: number;
}