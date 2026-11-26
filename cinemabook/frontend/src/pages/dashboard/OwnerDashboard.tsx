import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Film, DollarSign, Ticket, TrendingUp, Plus, LogOut, CreditCard } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { useOwnerStats, useOwnerFilms, usePublishFilm, useUnpublishFilm } from '../../hooks/useFilm';
import type { Film as FilmType } from '../../types/film';

export function OwnerDashboard() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(0);

  const { data: stats, isLoading: statsLoading } = useOwnerStats();
  const { data: filmsPage, isLoading: filmsLoading } = useOwnerFilms(page, 10, statusFilter || undefined);
  const publishMutation   = usePublishFilm();
  const unpublishMutation = useUnpublishFilm();

  const handleLogout = async () => { await logout(); navigate('/login'); };

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      {/* Header */}
      <header className="border-b border-neutral-800 px-6 py-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold">
          Cinema<span className="text-amber-400">Book</span>
          <span className="ml-3 text-sm font-normal text-neutral-400">Owner portal</span>
        </h1>
        <div className="flex items-center gap-3">
          <Link to="/owner/upload"
            className="flex items-center gap-2 bg-amber-400 hover:bg-amber-300
                       text-neutral-950 font-semibold px-4 py-2 rounded-lg text-sm transition-colors">
            <Plus className="w-4 h-4" /> Upload film
          </Link>
          <Link to="/owner/bank"
            className="flex items-center gap-2 border border-neutral-700 hover:border-neutral-500
                       text-neutral-300 hover:text-white px-4 py-2 rounded-lg text-sm transition-colors">
            <CreditCard className="w-4 h-4" />
            {stats?.bankAccountLinked ? 'Bank linked ✓' : 'Link bank'}
          </Link>
          <button onClick={handleLogout}
            className="flex items-center gap-1 text-sm text-neutral-400 hover:text-white transition-colors">
            <LogOut className="w-4 h-4" /> Sign out
          </button>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 py-8">
        {/* Stats */}
        <div className="grid grid-cols-4 gap-4 mb-8">
          <StatCard icon={<Film className="w-5 h-5" />} label="Total films"
            value={statsLoading ? '…' : String(stats?.totalFilms ?? 0)}
            sub={`${stats?.publishedFilms ?? 0} published`} />
          <StatCard icon={<Ticket className="w-5 h-5" />} label="Tickets sold"
            value={statsLoading ? '…' : String(stats?.totalTicketsSold ?? 0)} />
          <StatCard icon={<DollarSign className="w-5 h-5" />} label="Total revenue"
            value={statsLoading ? '…' : `$${Number(stats?.totalRevenue ?? 0).toFixed(2)}`} />
          <StatCard icon={<TrendingUp className="w-5 h-5" />} label="Bank account"
            value={stats?.bankAccountLinked ? 'Linked ✓' : 'Not linked'}
            highlight={!stats?.bankAccountLinked}
            action={!stats?.bankAccountLinked ? (
              <Link to="/owner/bank" className="text-xs text-amber-400 hover:underline mt-1 block">
                Connect now →
              </Link>) : undefined} />
        </div>

        {/* Films table */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl overflow-hidden">
          <div className="px-6 py-4 border-b border-neutral-800 flex items-center justify-between">
            <h2 className="font-semibold text-lg">My films</h2>
            <select value={statusFilter}
              onChange={e => { setStatusFilter(e.target.value); setPage(0); }}
              className="bg-neutral-800 border border-neutral-700 text-sm rounded-lg px-3 py-1.5
                         text-white focus:outline-none focus:border-amber-400">
              <option value="">All statuses</option>
              <option value="PROCESSING">Processing</option>
              <option value="READY">Ready</option>
              <option value="PUBLISHED">Published</option>
              <option value="UNPUBLISHED">Unpublished</option>
            </select>
          </div>

          {filmsLoading ? (
            <div className="px-6 py-12 text-center text-neutral-400">Loading…</div>
          ) : filmsPage?.films.length === 0 ? (
            <div className="px-6 py-12 text-center">
              <Film className="w-10 h-10 text-neutral-700 mx-auto mb-3" />
              <p className="text-neutral-400 mb-4">No films yet</p>
              <Link to="/owner/upload"
                className="inline-flex items-center gap-2 bg-amber-400 hover:bg-amber-300
                           text-neutral-950 font-semibold px-4 py-2 rounded-lg text-sm">
                <Plus className="w-4 h-4" /> Upload your first film
              </Link>
            </div>
          ) : (
            <table className="w-full">
              <thead>
                <tr className="text-left text-xs text-neutral-500 uppercase tracking-wider
                               border-b border-neutral-800">
                  <th className="px-6 py-3">Title</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Price</th>
                  <th className="px-6 py-3">Tickets</th>
                  <th className="px-6 py-3">Revenue</th>
                  <th className="px-6 py-3">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-800">
                {filmsPage?.films.map(film => (
                  <FilmRow key={film.id} film={film}
                    onPublish={() => publishMutation.mutate(film.id)}
                    onUnpublish={() => unpublishMutation.mutate(film.id)} />
                ))}
              </tbody>
            </table>
          )}

          {filmsPage && filmsPage.totalPages > 1 && (
            <div className="px-6 py-4 border-t border-neutral-800 flex items-center justify-between">
              <span className="text-sm text-neutral-400">
                Page {page + 1} of {filmsPage.totalPages}
              </span>
              <div className="flex gap-2">
                <button onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1.5 text-sm bg-neutral-800 rounded-lg disabled:opacity-40
                             hover:bg-neutral-700 transition-colors">Previous</button>
                <button onClick={() => setPage(p => Math.min(filmsPage.totalPages - 1, p + 1))}
                  disabled={page >= filmsPage.totalPages - 1}
                  className="px-3 py-1.5 text-sm bg-neutral-800 rounded-lg disabled:opacity-40
                             hover:bg-neutral-700 transition-colors">Next</button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

function StatCard({ icon, label, value, sub, highlight, action }: {
  icon: React.ReactNode; label: string; value: string;
  sub?: string; highlight?: boolean; action?: React.ReactNode;
}) {
  return (
    <div className={`bg-neutral-900 border rounded-xl p-5 ${highlight ? 'border-amber-400/40' : 'border-neutral-800'}`}>
      <div className="flex items-center gap-2 text-neutral-400 text-sm mb-3">{icon} {label}</div>
      <div className={`text-2xl font-bold ${highlight ? 'text-amber-400' : 'text-white'}`}>{value}</div>
      {sub    && <p className="text-xs text-neutral-500 mt-1">{sub}</p>}
      {action}
    </div>
  );
}

const STATUS_STYLES: Record<string, string> = {
  PUBLISHED:   'bg-green-900/40 text-green-400 border-green-800',
  READY:       'bg-blue-900/40 text-blue-400 border-blue-800',
  PROCESSING:  'bg-yellow-900/40 text-yellow-400 border-yellow-800',
  UNPUBLISHED: 'bg-neutral-800 text-neutral-400 border-neutral-700',
  REJECTED:    'bg-red-900/40 text-red-400 border-red-800',
};

function FilmRow({ film, onPublish, onUnpublish }: {
  film: FilmType; onPublish: () => void; onUnpublish: () => void;
}) {
  return (
    <tr className="hover:bg-neutral-800/50 transition-colors">
      <td className="px-6 py-4">
        <Link to={`/owner/films/${film.id}`}
          className="font-medium hover:text-amber-400 transition-colors">{film.title}</Link>
        {film.genre && <p className="text-xs text-neutral-500 mt-0.5">{film.genre}</p>}
      </td>
      <td className="px-6 py-4">
        <span className={`text-xs font-medium px-2 py-1 rounded-full border ${STATUS_STYLES[film.status]}`}>
          {film.status}
        </span>
      </td>
      <td className="px-6 py-4 text-sm">${Number(film.ticketPrice).toFixed(2)}</td>
      <td className="px-6 py-4 text-sm">{film.totalTicketsSold}</td>
      <td className="px-6 py-4 text-sm">${Number(film.totalRevenue).toFixed(2)}</td>
      <td className="px-6 py-4">
        <div className="flex items-center gap-2">
          {(film.status === 'READY' || film.status === 'UNPUBLISHED') && (
            <button onClick={onPublish}
              className="text-xs bg-green-800 hover:bg-green-700 text-green-200
                         px-3 py-1.5 rounded-lg transition-colors">Publish</button>
          )}
          {film.status === 'PUBLISHED' && (
            <button onClick={onUnpublish}
              className="text-xs bg-neutral-700 hover:bg-neutral-600 text-neutral-300
                         px-3 py-1.5 rounded-lg transition-colors">Unpublish</button>
          )}
          <Link to={`/owner/films/${film.id}/edit`}
            className="text-xs text-neutral-400 hover:text-white transition-colors">Edit</Link>
        </div>
      </td>
    </tr>
  );
}