import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Film, Ticket, Play, LogOut, Search } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '../../store/authStore';
import { watcherService } from '../../services/watcherService';
import { filmService } from '../../services/filmService';
import type { Film as FilmType } from '../../types/film';
import type { Ticket as TicketType } from '../../types/watcher';

export function WatcherDashboard() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [tab, setTab] = useState<'browse' | 'library'>('browse');
  const [search, setSearch] = useState('');
  const [filmPage, setFilmPage] = useState(0);
  const [ticketPage, setTicketPage] = useState(0);

  const { data: filmsData, isLoading: filmsLoading } = useQuery({
    queryKey: ['publishedFilms', filmPage],
    queryFn: () => filmService.browsePublishedFilms(filmPage, 12),
    enabled: tab === 'browse',
  });

  const { data: ticketsData, isLoading: ticketsLoading } = useQuery({
    queryKey: ['myTickets', ticketPage],
    queryFn: () => watcherService.getMyTickets(ticketPage, 12),
    enabled: tab === 'library',
  });

  const handleLogout = async () => { await logout(); navigate('/login'); };

  const filteredFilms = filmsData?.films.filter(f =>
    f.title.toLowerCase().includes(search.toLowerCase()) ||
    (f.genre ?? '').toLowerCase().includes(search.toLowerCase())
  ) ?? [];

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      <header className="border-b border-neutral-800 px-6 py-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Cinema<span className="text-amber-400">Book</span></h1>
        <div className="flex items-center gap-4">
          <span className="text-neutral-400 text-sm">Hello, <span className="text-white">{user?.displayName}</span></span>
          <button onClick={handleLogout}
            className="flex items-center gap-1 text-sm text-neutral-400 hover:text-white transition-colors">
            <LogOut className="w-4 h-4" /> Sign out
          </button>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="flex items-center gap-1 mb-8 bg-neutral-900 border border-neutral-800 rounded-xl p-1 w-fit">
          <button onClick={() => setTab('browse')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors
                        ${tab === 'browse' ? 'bg-amber-400 text-neutral-950' : 'text-neutral-400 hover:text-white'}`}>
            <Film className="w-4 h-4" /> Browse films
          </button>
          <button onClick={() => setTab('library')}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors
                        ${tab === 'library' ? 'bg-amber-400 text-neutral-950' : 'text-neutral-400 hover:text-white'}`}>
            <Ticket className="w-4 h-4" /> My tickets
          </button>
        </div>

        {tab === 'browse' && (
          <>
            <div className="relative mb-6 max-w-md">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-neutral-500" />
              <input value={search} onChange={e => setSearch(e.target.value)}
                placeholder="Search by title or genre…"
                className="w-full bg-neutral-900 border border-neutral-800 rounded-lg pl-10 pr-4 py-2.5
                           text-sm text-white placeholder-neutral-500 focus:outline-none
                           focus:border-amber-400 transition-colors" />
            </div>
            {filmsLoading ? <GridSkeleton /> : filteredFilms.length === 0 ? (
              <div className="text-center py-16 text-neutral-500">
                <Film className="w-12 h-12 mx-auto mb-3 opacity-30" /><p>No films found</p>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 mb-8">
                  {filteredFilms.map(film => <FilmCard key={film.id} film={film} />)}
                </div>
                {filmsData && filmsData.totalPages > 1 && (
                  <Pager page={filmPage} total={filmsData.totalPages}
                    onPrev={() => setFilmPage(p => Math.max(0, p - 1))}
                    onNext={() => setFilmPage(p => Math.min(filmsData.totalPages - 1, p + 1))} />
                )}
              </>
            )}
          </>
        )}

        {tab === 'library' && (
          <>
            {ticketsLoading ? <GridSkeleton /> : ticketsData?.tickets.length === 0 ? (
              <div className="text-center py-16">
                <Ticket className="w-12 h-12 mx-auto mb-3 text-neutral-700" />
                <p className="text-neutral-400 mb-4">No tickets yet</p>
                <button onClick={() => setTab('browse')}
                  className="bg-amber-400 hover:bg-amber-300 text-neutral-950 font-semibold px-5 py-2.5 rounded-lg text-sm">
                  Browse films
                </button>
              </div>
            ) : (
              <>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 mb-8">
                  {ticketsData?.tickets.map(t => <TicketCard key={t.id} ticket={t} />)}
                </div>
                {ticketsData && ticketsData.totalPages > 1 && (
                  <Pager page={ticketPage} total={ticketsData.totalPages}
                    onPrev={() => setTicketPage(p => Math.max(0, p - 1))}
                    onNext={() => setTicketPage(p => Math.min(ticketsData.totalPages - 1, p + 1))} />
                )}
              </>
            )}
          </>
        )}
      </main>
    </div>
  );
}

function FilmCard({ film }: { film: FilmType }) {
  return (
    <Link to={`/films/${film.id}`}
      className="group bg-neutral-900 border border-neutral-800 rounded-xl overflow-hidden hover:border-neutral-600 transition-all">
      <div className="aspect-video bg-neutral-800 relative overflow-hidden">
        {film.thumbnailUrl
          ? <img src={film.thumbnailUrl} alt={film.title} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
          : <div className="w-full h-full flex items-center justify-center"><Film className="w-8 h-8 text-neutral-700" /></div>}
        <div className="absolute inset-0 bg-black/0 group-hover:bg-black/20 transition-colors flex items-center justify-center">
          <Play className="w-10 h-10 text-white opacity-0 group-hover:opacity-100 transition-opacity drop-shadow-lg" />
        </div>
      </div>
      <div className="p-3">
        <h3 className="font-medium text-sm truncate mb-1">{film.title}</h3>
        <div className="flex items-center justify-between">
          <span className="text-xs text-neutral-500">{film.genre ?? 'Film'}</span>
          <span className="text-amber-400 text-sm font-semibold">${Number(film.ticketPrice).toFixed(2)}</span>
        </div>
      </div>
    </Link>
  );
}

function TicketCard({ ticket }: { ticket: TicketType }) {
  const pct = Math.round((ticket.clipSecondsUsed / 300) * 100);
  return (
    <Link to={`/watch/${ticket.filmId}`}
      className="group bg-neutral-900 border border-neutral-800 rounded-xl overflow-hidden hover:border-neutral-600 transition-all">
      <div className="aspect-video bg-neutral-800 relative flex items-center justify-center">
        {ticket.filmThumbnailUrl
          ? <img src={ticket.filmThumbnailUrl} alt={ticket.filmTitle} className="w-full h-full object-cover" />
          : <Film className="w-8 h-8 text-neutral-700" />}
        <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
          <div className="bg-amber-400 rounded-full p-2.5 group-hover:scale-110 transition-transform">
            <Play className="w-5 h-5 text-neutral-950 fill-current" />
          </div>
        </div>
        <span className={`absolute top-2 right-2 text-xs px-2 py-0.5 rounded-full font-medium
          ${ticket.status === 'ACTIVE' ? 'bg-green-900/80 text-green-400' : 'bg-neutral-800 text-neutral-400'}`}>
          {ticket.status}
        </span>
      </div>
      <div className="p-3">
        <h3 className="font-medium text-sm truncate mb-2">{ticket.filmTitle}</h3>
        <div className="flex justify-between text-xs text-neutral-500 mb-1">
          <span>Clip budget</span><span>{ticket.clipSecondsRemaining}s left</span>
        </div>
        <div className="h-1 bg-neutral-800 rounded-full overflow-hidden">
          <div className="h-full bg-amber-400 rounded-full" style={{ width: `${pct}%` }} />
        </div>
      </div>
    </Link>
  );
}

function GridSkeleton() {
  return (
    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="bg-neutral-900 border border-neutral-800 rounded-xl overflow-hidden animate-pulse">
          <div className="aspect-video bg-neutral-800" />
          <div className="p-3 space-y-2"><div className="h-3 bg-neutral-800 rounded w-3/4" /><div className="h-3 bg-neutral-800 rounded w-1/2" /></div>
        </div>
      ))}
    </div>
  );
}

function Pager({ page, total, onPrev, onNext }: { page: number; total: number; onPrev: () => void; onNext: () => void }) {
  return (
    <div className="flex items-center justify-center gap-4">
      <button onClick={onPrev} disabled={page === 0} className="px-4 py-2 text-sm bg-neutral-800 rounded-lg disabled:opacity-40 hover:bg-neutral-700 transition-colors">Previous</button>
      <span className="text-sm text-neutral-400">{page + 1} / {total}</span>
      <button onClick={onNext} disabled={page >= total - 1} className="px-4 py-2 text-sm bg-neutral-800 rounded-lg disabled:opacity-40 hover:bg-neutral-700 transition-colors">Next</button>
    </div>
  );
}