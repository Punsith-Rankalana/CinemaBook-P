import { apiClient } from './apiClient';
import type { ApiResponse } from '../types';
import type {
  Film, FilmPage, FilmUploadUrlResponse, OwnerStats,
  BankLinkResponse, BankLinkStatus, PriceHistoryEntry,
  CreateFilmRequest, UpdateFilmRequest, UpdatePriceRequest,
} from '../types/film';

export const filmService = {

  // ── Film CRUD (owner) ─────────────────────────────────────────────────────

  async createFilm(data: CreateFilmRequest): Promise<FilmUploadUrlResponse> {
    const res = await apiClient.post<ApiResponse<FilmUploadUrlResponse>>('/owner/films', data);
    return res.data.data!;
  },

  async confirmUpload(filmId: string, storageKey: string,
                      fileSizeBytes: number, contentType: string): Promise<Film> {
    const res = await apiClient.post<ApiResponse<Film>>(
      `/owner/films/${filmId}/confirm-upload`,
      { storageKey, fileSizeBytes, contentType });
    return res.data.data!;
  },

  async updateFilm(filmId: string, data: UpdateFilmRequest): Promise<Film> {
    const res = await apiClient.patch<ApiResponse<Film>>(`/owner/films/${filmId}`, data);
    return res.data.data!;
  },

  async updatePrice(filmId: string, data: UpdatePriceRequest): Promise<Film> {
    const res = await apiClient.patch<ApiResponse<Film>>(`/owner/films/${filmId}/price`, data);
    return res.data.data!;
  },

  async publishFilm(filmId: string): Promise<Film> {
    const res = await apiClient.post<ApiResponse<Film>>(`/owner/films/${filmId}/publish`);
    return res.data.data!;
  },

  async unpublishFilm(filmId: string): Promise<Film> {
    const res = await apiClient.post<ApiResponse<Film>>(`/owner/films/${filmId}/unpublish`);
    return res.data.data!;
  },

  async getOwnerFilms(page = 0, size = 20, status?: string): Promise<FilmPage> {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) params.set('status', status);
    const res = await apiClient.get<ApiResponse<FilmPage>>(`/owner/films?${params}`);
    return res.data.data!;
  },

  async getOwnerFilm(filmId: string): Promise<Film> {
    const res = await apiClient.get<ApiResponse<Film>>(`/owner/films/${filmId}`);
    return res.data.data!;
  },

  async getPriceHistory(filmId: string): Promise<PriceHistoryEntry[]> {
    const res = await apiClient.get<ApiResponse<PriceHistoryEntry[]>>(
      `/owner/films/${filmId}/price-history`);
    return res.data.data!;
  },

  // ── Public film browsing (Phase 3) ────────────────────────────────────────

  async browsePublishedFilms(page = 0, size = 20): Promise<FilmPage> {
    const res = await apiClient.get<ApiResponse<FilmPage>>(
      `/films?page=${page}&size=${size}`);
    return res.data.data!;
  },

  async getFilm(filmId: string): Promise<Film> {
    const res = await apiClient.get<ApiResponse<Film>>(`/films/${filmId}`);
    return res.data.data!;
  },

  // ── Stats ─────────────────────────────────────────────────────────────────

  async getOwnerStats(): Promise<OwnerStats> {
    const res = await apiClient.get<ApiResponse<OwnerStats>>('/owner/stats');
    return res.data.data!;
  },

  // ── Bank link ─────────────────────────────────────────────────────────────

  async initiateBankLink(): Promise<BankLinkResponse> {
    const res = await apiClient.post<ApiResponse<BankLinkResponse>>('/owner/bank/link');
    return res.data.data!;
  },

  async completeBankLink(): Promise<void> {
    await apiClient.post('/owner/bank/complete');
  },

  async getBankLinkStatus(): Promise<BankLinkStatus> {
    const res = await apiClient.get<ApiResponse<BankLinkStatus>>('/owner/bank/status');
    return res.data.data!;
  },

  // ── Upload to storage (direct PUT to pre-signed URL) ──────────────────────

  async uploadFileToStorage(
    uploadUrl: string,
    file: File,
    onProgress?: (percent: number) => void
  ): Promise<void> {
    await new Promise<void>((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable && onProgress) {
          onProgress(Math.round((e.loaded / e.total) * 100));
        }
      });
      xhr.addEventListener('load', () => {
        if (xhr.status >= 200 && xhr.status < 300) resolve();
        else reject(new Error(`Upload failed: HTTP ${xhr.status}`));
      });
      xhr.addEventListener('error', () => reject(new Error('Upload network error')));
      xhr.open('PUT', uploadUrl);
      xhr.setRequestHeader('Content-Type', file.type);
      xhr.send(file);
    });
  },
};