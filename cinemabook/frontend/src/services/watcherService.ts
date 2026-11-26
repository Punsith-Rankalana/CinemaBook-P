import { apiClient } from './apiClient';
import type { ApiResponse } from '../types';
import type {
  Ticket, TicketPage, StreamSession, ClipJob,
  PaymentConfirmResponse, PurchaseTicketRequest, RequestClipRequest,
} from '../types/watcher';

export const watcherService = {

  // ── Tickets ──────────────────────────────────────────────────────────────

  async purchaseTicket(data: PurchaseTicketRequest): Promise<PaymentConfirmResponse> {
    const res = await apiClient.post<ApiResponse<PaymentConfirmResponse>>(
      '/watcher/tickets/purchase', data);
    return res.data.data!;
  },

  async getMyTickets(page = 0, size = 20): Promise<TicketPage> {
    const res = await apiClient.get<ApiResponse<TicketPage>>(
      `/watcher/tickets?page=${page}&size=${size}`);
    return res.data.data!;
  },

  async getTicket(ticketId: string): Promise<Ticket> {
    const res = await apiClient.get<ApiResponse<Ticket>>(
      `/watcher/tickets/${ticketId}`);
    return res.data.data!;
  },

  async updateProgress(ticketId: string, positionSeconds: number,
                       eventType: 'START' | 'PROGRESS' | 'PAUSE' | 'COMPLETE'
  ): Promise<void> {
    await apiClient.patch(`/watcher/tickets/${ticketId}/progress`,
      { positionSeconds, eventType });
  },

  // ── Streaming ─────────────────────────────────────────────────────────────

  async startStream(filmId: string): Promise<StreamSession> {
    const res = await apiClient.post<ApiResponse<StreamSession>>(
      '/watcher/stream/start', { filmId });
    return res.data.data!;
  },

  // ── Clips ─────────────────────────────────────────────────────────────────

  async requestClip(filmId: string, data: RequestClipRequest): Promise<ClipJob> {
    const res = await apiClient.post<ApiResponse<ClipJob>>(
      `/watcher/clips/request?filmId=${filmId}`, data);
    return res.data.data!;
  },

  async getClipHistory(filmId: string): Promise<ClipJob[]> {
    const res = await apiClient.get<ApiResponse<ClipJob[]>>(
      `/watcher/clips/history?filmId=${filmId}`);
    return res.data.data!;
  },
};