import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Play, Ticket, Clock, Star, AlertCircle } from 'lucide-react';
import { filmService } from '../../services/filmService';
import { watcherService } from '../../services/watcherService';

export function FilmDetailPage() {
  const { filmId } = useParams<{ filmId: string }>();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [buyError, setBuyError] = useState('');

  const { data: film, isLoading } = useQuery({
    queryKey: ['film', filmId],
    queryFn: () => filmService.browsePublishedFilms(0, 1).then(() =>
      filmService.getFilm ? fetch(`/api/v1/films/${filmId}`)
        .then(r => r.json()).then(d => d.data) : null),
    enabled: !!filmId,
  });

  const { data: tickets } = useQuery({
    queryKey: ['myTickets', 0],
    queryFn: () => watcherService.getMyTickets(0, 100),
  });

  const alreadyOwns = tickets?.tickets.some(t => t.filmId === filmId);

  const buyMutation = useMutation({
    mutationFn: () => watcherService.purchaseTicket({
      filmId: filmId!,
      paymentToken: 'mock_pm_' + Math.random().toString(36).slice(2),
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['myTickets'] });
      navigate(`/watch/${filmId}`);
    },
    onError: (err: any) => {
      setBuyError(err?.response?.data?.error ?? 'Purchase failed. Please try again.');
    },
  });

  if (isLoading) {
    return (
      <div className="min-h-screen bg-neutral-950 flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-amber-400 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!film) {
    return (
      <div className="min-h-screen bg-neutral-950 text-white flex items-center justify-center">
        <div className="text-center">
          <p className="text-neutral-400 mb-4">Film not found</p>
          <Link to="/dashboard" className="text-amber-400 hover:underline">← Back to browse</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      <header className="border-b border-neutral-800 px-6 py-4 flex items-center gap-4">
        <Link to="/dashboard"
          className="flex items-center gap-2 text-neutral-400 hover:text-white transition-colors text-sm">
          <ArrowLeft className="w-4 h-4" /> Browse
        </Link>
        <h1 className="text-xl font-bold">Cinema<span className="text-amber-400">Book</span></h1>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-10">
        <div className="grid md:grid-cols-3 gap-8">

          {/* Thumbnail / trailer */}
          <div className="md:col-span-2">
            <div className="aspect-video bg-neutral-900 border border-neutral-800 rounded-2xl
                            overflow-hidden flex items-center justify-center mb-6 relative group">
              {film.thumbnailUrl ? (
                <img src={film.thumbnailUrl} alt={film.title}
                  className="w-full h-full object-cover" />
              ) : (
                <div className="flex flex-col items-center text-neutral-700">
                  <Play className="w-16 h-16 mb-2" />
                  <p className="text-sm">No preview available</p>
                </div>
              )}
            </div>

            <h1 className="text-3xl font-bold mb-3">{film.title}</h1>

            <div className="flex flex-wrap gap-3 mb-4">
              {film.genre && <Badge>{film.genre}</Badge>}
              {film.ageRating && <Badge>{film.ageRating}</Badge>}
              {film.releaseYear && <Badge>{film.releaseYear}</Badge>}
              {film.language && <Badge>{film.language}</Badge>}
              {film.durationSeconds && (
                <Badge icon={<Clock className="w-3 h-3" />}>
                  {Math.floor(film.durationSeconds / 60)}m
                </Badge>
              )}
            </div>

            {film.description && (
              <p className="text-neutral-300 leading-relaxed">{film.description}</p>
            )}

            {film.tags && film.tags.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-4">
                {film.tags.map(tag => (
                  <span key={tag} className="text-xs bg-neutral-800 text-neutral-400
                                             px-2 py-1 rounded-full">#{tag}</span>
                ))}
              </div>
            )}
          </div>

          {/* Purchase panel */}
          <div className="md:col-span-1">
            <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 sticky top-6">
              <div className="text-3xl font-bold text-amber-400 mb-1">
                ${Number(film.ticketPrice).toFixed(2)}
              </div>
              <p className="text-neutral-400 text-sm mb-6">One-time ticket — watch anytime</p>

              <div className="space-y-3 mb-6">
                <Feature icon={<Play className="w-4 h-4" />} text="Stream in full HD" />
                <Feature icon={<Ticket className="w-4 h-4" />} text="5 minutes of shareable clips" />
                <Feature icon={<Star className="w-4 h-4" />} text="Resume where you left off" />
              </div>

              {buyError && (
                <div className="flex items-start gap-2 bg-red-950 border border-red-800
                                rounded-lg px-3 py-2.5 mb-4">
                  <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0 mt-0.5" />
                  <p className="text-sm text-red-300">{buyError}</p>
                </div>
              )}

              {alreadyOwns ? (
                <Link to={`/watch/${filmId}`}
                  className="w-full flex items-center justify-center gap-2 bg-green-700
                             hover:bg-green-600 text-white font-semibold rounded-xl py-3 text-sm
                             transition-colors">
                  <Play className="w-4 h-4" /> Watch now
                </Link>
              ) : (
                <button
                  onClick={() => buyMutation.mutate()}
                  disabled={buyMutation.isPending}
                  className="w-full flex items-center justify-center gap-2 bg-amber-400
                             hover:bg-amber-300 disabled:opacity-50 text-neutral-950
                             font-semibold rounded-xl py-3 text-sm transition-colors">
                  <Ticket className="w-4 h-4" />
                  {buyMutation.isPending ? 'Processing…' : 'Buy ticket'}
                </button>
              )}

              <p className="text-xs text-neutral-500 text-center mt-3">
                Secure payment via Stripe
              </p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

function Badge({ children, icon }: { children: React.ReactNode; icon?: React.ReactNode }) {
  return (
    <span className="flex items-center gap-1 text-xs bg-neutral-800 text-neutral-300
                     px-3 py-1 rounded-full">
      {icon}{children}
    </span>
  );
}

function Feature({ icon, text }: { icon: React.ReactNode; text: string }) {
  return (
    <div className="flex items-center gap-3 text-sm text-neutral-300">
      <span className="text-amber-400">{icon}</span> {text}
    </div>
  );
}