import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { filmService } from '../services/filmService';

export function useOwnerStats() {
  return useQuery({
    queryKey: ['ownerStats'],
    queryFn: filmService.getOwnerStats,
    staleTime: 30_000,
  });
}

export function useOwnerFilms(page = 0, size = 20, status?: string) {
  return useQuery({
    queryKey: ['ownerFilms', page, size, status],
    queryFn: () => filmService.getOwnerFilms(page, size, status),
  });
}

export function usePublishFilm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (filmId: string) => filmService.publishFilm(filmId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ownerFilms'] });
      qc.invalidateQueries({ queryKey: ['ownerStats'] });
    },
  });
}

export function useUnpublishFilm() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (filmId: string) => filmService.unpublishFilm(filmId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ownerFilms'] });
      qc.invalidateQueries({ queryKey: ['ownerStats'] });
    },
  });
}

export function useBankLinkStatus() {
  return useQuery({
    queryKey: ['bankLinkStatus'],
    queryFn: filmService.getBankLinkStatus,
    staleTime: 60_000,
  });
}

export function useMarkReady() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (filmId: string) => filmService.markReady(filmId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ownerFilms'] });
      qc.invalidateQueries({ queryKey: ['ownerStats'] });
    },
  });
}