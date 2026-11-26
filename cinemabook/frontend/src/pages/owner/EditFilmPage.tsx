import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ArrowLeft, Save, DollarSign, History, CheckCircle, AlertCircle } from 'lucide-react';
import { filmService } from '../../services/filmService';

const detailsSchema = z.object({
  title:       z.string().min(1, 'Title is required').max(255),
  description: z.string().max(5000).optional(),
  genre:       z.string().max(100).optional(),
  releaseYear: z.coerce.number().int().min(1888).max(2100).optional().or(z.literal('')),
  language:    z.string().max(50).optional(),
  ageRating:   z.string().max(10).optional(),
});

const priceSchema = z.object({
  newPrice: z.coerce.number().min(0.01, 'Price must be at least $0.01').max(999.99),
});

type DetailsForm = z.infer<typeof detailsSchema>;
type PriceForm   = z.infer<typeof priceSchema>;

export function EditFilmPage() {
  const { filmId } = useParams<{ filmId: string }>();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [savedMsg, setSavedMsg] = useState('');
  const [showHistory, setShowHistory] = useState(false);

  const { data: film, isLoading, isError } = useQuery({
    queryKey: ['ownerFilm', filmId],
    queryFn: () => filmService.getOwnerFilm(filmId!),
    enabled: !!filmId,
  });

  const { data: priceHistory } = useQuery({
    queryKey: ['priceHistory', filmId],
    queryFn: () => filmService.getPriceHistory(filmId!),
    enabled: !!filmId && showHistory,
  });

  const {
    register: registerDetails,
    handleSubmit: handleDetailsSubmit,
    reset: resetDetails,
    formState: { errors: detailsErrors, isSubmitting: detailsSubmitting },
  } = useForm<DetailsForm>({ resolver: zodResolver(detailsSchema) });

  const {
    register: registerPrice,
    handleSubmit: handlePriceSubmit,
    reset: resetPrice,
    formState: { errors: priceErrors, isSubmitting: priceSubmitting },
  } = useForm<PriceForm>({ resolver: zodResolver(priceSchema) });

  // Populate forms once film data loads
  useEffect(() => {
    if (film) {
      resetDetails({
        title:       film.title,
        description: film.description ?? '',
        genre:       film.genre ?? '',
        releaseYear: film.releaseYear ?? '',
        language:    film.language ?? '',
        ageRating:   film.ageRating ?? '',
      });
      resetPrice({ newPrice: film.ticketPrice });
    }
  }, [film, resetDetails, resetPrice]);

  const updateDetailsMutation = useMutation({
    mutationFn: (data: DetailsForm) => filmService.updateFilm(filmId!, {
      title:       data.title,
      description: data.description,
      genre:       data.genre,
      releaseYear: data.releaseYear ? Number(data.releaseYear) : undefined,
      language:    data.language,
      ageRating:   data.ageRating,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ownerFilm', filmId] });
      qc.invalidateQueries({ queryKey: ['ownerFilms'] });
      setSavedMsg('Film details updated successfully.');
      setTimeout(() => setSavedMsg(''), 3000);
    },
  });

  const updatePriceMutation = useMutation({
    mutationFn: (data: PriceForm) => filmService.updatePrice(filmId!, { newPrice: data.newPrice }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ownerFilm', filmId] });
      qc.invalidateQueries({ queryKey: ['ownerFilms'] });
      qc.invalidateQueries({ queryKey: ['priceHistory', filmId] });
      setSavedMsg('Ticket price updated successfully.');
      setTimeout(() => setSavedMsg(''), 3000);
    },
  });

  if (isLoading) {
    return (
      <div className="min-h-screen bg-neutral-950 flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-amber-400 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (isError || !film) {
    return (
      <div className="min-h-screen bg-neutral-950 text-white flex items-center justify-center">
        <div className="text-center">
          <AlertCircle className="w-10 h-10 text-red-400 mx-auto mb-3" />
          <p className="text-neutral-300 mb-4">Film not found, or you don't own this film.</p>
          <Link to="/owner/dashboard" className="text-amber-400 hover:underline text-sm">
            ← Back to dashboard
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      <header className="border-b border-neutral-800 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link to="/owner/dashboard"
            className="flex items-center gap-2 text-neutral-400 hover:text-white transition-colors text-sm">
            <ArrowLeft className="w-4 h-4" /> Back to dashboard
          </Link>
          <h1 className="text-xl font-bold">Cinema<span className="text-amber-400">Book</span></h1>
        </div>
        <span className={`text-xs font-medium px-3 py-1 rounded-full border
          ${film.status === 'PUBLISHED'   ? 'bg-green-900/40 text-green-400 border-green-800'
          : film.status === 'READY'       ? 'bg-blue-900/40 text-blue-400 border-blue-800'
          : film.status === 'PROCESSING'  ? 'bg-yellow-900/40 text-yellow-400 border-yellow-800'
          : 'bg-neutral-800 text-neutral-400 border-neutral-700'}`}>
          {film.status}
        </span>
      </header>

      <main className="max-w-3xl mx-auto px-6 py-10">
        <h2 className="text-2xl font-bold mb-1">Edit "{film.title}"</h2>
        <p className="text-neutral-400 text-sm mb-8">
          Update metadata or change the ticket price for this film.
        </p>

        {savedMsg && (
          <div className="flex items-center gap-2 bg-green-950 border border-green-800
                          rounded-lg px-4 py-3 mb-6">
            <CheckCircle className="w-4 h-4 text-green-400" />
            <p className="text-sm text-green-300">{savedMsg}</p>
          </div>
        )}

        {/* Film details form */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-8 mb-6">
          <h3 className="font-semibold text-lg mb-5">Film details</h3>

          <form onSubmit={handleDetailsSubmit(d => updateDetailsMutation.mutate(d))}
            className="space-y-5">

            <Field label="Title" error={detailsErrors.title?.message}>
              <input {...registerDetails('title')} className={inputCls} />
            </Field>

            <Field label="Description" error={detailsErrors.description?.message}>
              <textarea {...registerDetails('description')} rows={4}
                className={inputCls + ' resize-none'} />
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label="Genre" error={detailsErrors.genre?.message}>
                <select {...registerDetails('genre')} className={inputCls}>
                  <option value="">Select genre</option>
                  {['Action','Comedy','Drama','Horror','Romance',
                    'Sci-Fi','Thriller','Documentary','Animation','Other']
                    .map(g => <option key={g} value={g}>{g}</option>)}
                </select>
              </Field>
              <Field label="Age rating" error={detailsErrors.ageRating?.message}>
                <select {...registerDetails('ageRating')} className={inputCls}>
                  <option value="">Select rating</option>
                  {['G','PG','PG-13','R','NC-17'].map(r => (
                    <option key={r} value={r}>{r}</option>))}
                </select>
              </Field>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <Field label="Release year" error={detailsErrors.releaseYear?.message}>
                <input {...registerDetails('releaseYear')} type="number" className={inputCls} />
              </Field>
              <Field label="Language" error={detailsErrors.language?.message}>
                <input {...registerDetails('language')} className={inputCls} />
              </Field>
            </div>

            <button type="submit" disabled={detailsSubmitting}
              className="flex items-center gap-2 bg-amber-400 hover:bg-amber-300 disabled:opacity-50
                         text-neutral-950 font-semibold rounded-lg px-5 py-2.5 text-sm transition-colors">
              <Save className="w-4 h-4" />
              {detailsSubmitting ? 'Saving…' : 'Save details'}
            </button>
          </form>
        </div>

        {/* Price form */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-8 mb-6">
          <div className="flex items-center justify-between mb-5">
            <h3 className="font-semibold text-lg flex items-center gap-2">
              <DollarSign className="w-5 h-5 text-amber-400" /> Ticket price
            </h3>
            <button onClick={() => setShowHistory(s => !s)}
              className="flex items-center gap-1.5 text-xs text-neutral-400 hover:text-white transition-colors">
              <History className="w-3.5 h-3.5" /> {showHistory ? 'Hide' : 'View'} history
            </button>
          </div>

          <form onSubmit={handlePriceSubmit(d => updatePriceMutation.mutate(d))}
            className="flex items-end gap-4">
            <Field label="New price (USD)" error={priceErrors.newPrice?.message}>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400">$</span>
                <input {...registerPrice('newPrice')} type="number" step="0.01"
                  className={inputCls + ' pl-7 w-40'} />
              </div>
            </Field>
            <button type="submit" disabled={priceSubmitting}
              className="bg-neutral-800 hover:bg-neutral-700 disabled:opacity-50
                         text-white font-semibold rounded-lg px-5 py-2.5 text-sm
                         transition-colors mb-px">
              {priceSubmitting ? 'Updating…' : 'Update price'}
            </button>
          </form>

          {showHistory && (
            <div className="mt-6 border-t border-neutral-800 pt-5">
              {!priceHistory || priceHistory.length === 0 ? (
                <p className="text-sm text-neutral-500">No price changes recorded yet.</p>
              ) : (
                <div className="space-y-2">
                  {priceHistory.map((entry, i) => (
                    <div key={i} className="flex items-center justify-between text-sm
                                            bg-neutral-800/50 rounded-lg px-4 py-2.5">
                      <span className="text-neutral-300">
                        {entry.oldPrice != null
                          ? <>${Number(entry.oldPrice).toFixed(2)} → ${Number(entry.newPrice).toFixed(2)}</>
                          : <>Initial price: ${Number(entry.newPrice).toFixed(2)}</>}
                      </span>
                      <span className="text-neutral-500 text-xs">
                        {new Date(entry.changedAt).toLocaleString()}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Stats summary */}
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-5">
            <p className="text-neutral-400 text-sm mb-1">Tickets sold</p>
            <p className="text-2xl font-bold">{film.totalTicketsSold}</p>
          </div>
          <div className="bg-neutral-900 border border-neutral-800 rounded-xl p-5">
            <p className="text-neutral-400 text-sm mb-1">Total revenue</p>
            <p className="text-2xl font-bold">${Number(film.totalRevenue).toFixed(2)}</p>
          </div>
        </div>
      </main>
    </div>
  );
}

const inputCls = `w-full bg-neutral-800 border border-neutral-700 rounded-lg px-4 py-2.5
                  text-white placeholder-neutral-500 text-sm focus:outline-none
                  focus:border-amber-400 transition-colors`;

function Field({ label, error, children }: {
  label: string; error?: string; children: React.ReactNode;
}) {
  return (
    <div>
      <label className="block text-sm text-neutral-300 mb-1.5">{label}</label>
      {children}
      {error && <p className="mt-1 text-xs text-red-400">{error}</p>}
    </div>
  );
}