import { useState, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Upload, ArrowLeft, Film } from 'lucide-react';
import { filmService } from '../../services/filmService';

const schema = z.object({
  title:       z.string().min(1, 'Title is required').max(255),
  description: z.string().max(5000).optional(),
  genre:       z.string().max(100).optional(),
  ticketPrice: z.coerce.number().min(0.01, 'Price must be at least $0.01').max(999.99),
  currency:    z.string().default('USD'),
  releaseYear: z.coerce.number().int().min(1888).max(2100).optional().or(z.literal('')),
  language:    z.string().max(50).optional(),
  ageRating:   z.string().max(10).optional(),
});

type FormData = z.infer<typeof schema>;

type Step = 'details' | 'upload' | 'done';

export function UploadFilmPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [step, setStep] = useState<Step>('details');
  const [filmId, setFilmId] = useState('');
  const [uploadUrl, setUploadUrl] = useState('');
  const [storageKey, setStorageKey] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { currency: 'USD' },
  });

  // Step 1: Submit film details → get upload URL
  const onSubmitDetails = async (data: FormData) => {
    setError('');
    try {
      const res = await filmService.createFilm({
        title:       data.title,
        description: data.description,
        genre:       data.genre,
        ticketPrice: data.ticketPrice,
        currency:    data.currency,
        releaseYear: data.releaseYear ? Number(data.releaseYear) : undefined,
        language:    data.language,
        ageRating:   data.ageRating,
      });
      setFilmId(res.filmId);
      setUploadUrl(res.uploadUrl);
      setStorageKey(res.storageKey);
      setStep('upload');
    } catch (e: any) {
      setError(e?.response?.data?.error ?? 'Failed to create film. Please try again.');
    }
  };

  // Step 2: Upload file to storage
  const handleUpload = async () => {
    if (!selectedFile) { setError('Please select a video file'); return; }
    setError('');
    setUploading(true);

    try {
      await filmService.uploadFileToStorage(uploadUrl, selectedFile, setUploadProgress);
      await filmService.confirmUpload(filmId, storageKey, selectedFile.size, selectedFile.type);
      setStep('done');
    } catch (e: any) {
      setError(e?.message ?? 'Upload failed. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      <header className="border-b border-neutral-800 px-6 py-4 flex items-center gap-4">
        <Link to="/owner/dashboard"
          className="flex items-center gap-2 text-neutral-400 hover:text-white transition-colors text-sm">
          <ArrowLeft className="w-4 h-4" /> Back to dashboard
        </Link>
        <h1 className="text-xl font-bold">
          Cinema<span className="text-amber-400">Book</span>
        </h1>
      </header>

      <main className="max-w-2xl mx-auto px-6 py-12">

        {/* Progress steps */}
        <div className="flex items-center gap-4 mb-10">
          {(['details', 'upload', 'done'] as Step[]).map((s, i) => (
            <div key={s} className="flex items-center gap-2">
              <div className={`w-7 h-7 rounded-full flex items-center justify-center text-sm font-bold
                              ${step === s ? 'bg-amber-400 text-neutral-950'
                                : ['upload','done'].includes(step) && i === 0 ? 'bg-green-600 text-white'
                                : step === 'done' && i === 1 ? 'bg-green-600 text-white'
                                : 'bg-neutral-800 text-neutral-500'}`}>
                {i + 1}
              </div>
              <span className={`text-sm capitalize ${step === s ? 'text-white' : 'text-neutral-500'}`}>
                {s === 'details' ? 'Film details' : s === 'upload' ? 'Upload video' : 'Complete'}
              </span>
              {i < 2 && <div className="w-12 h-px bg-neutral-800 mx-1" />}
            </div>
          ))}
        </div>

        {/* ── Step 1: Details ── */}
        {step === 'details' && (
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-8">
            <h2 className="text-xl font-semibold mb-6">Film details</h2>

            <form onSubmit={handleSubmit(onSubmitDetails)} className="space-y-5">
              <Field label="Title *" error={errors.title?.message}>
                <input {...register('title')} placeholder="My Film Title"
                  className={inputCls} />
              </Field>

              <Field label="Description" error={errors.description?.message}>
                <textarea {...register('description')} rows={3}
                  placeholder="A brief description of your film…"
                  className={inputCls + ' resize-none'} />
              </Field>

              <div className="grid grid-cols-2 gap-4">
                <Field label="Genre" error={errors.genre?.message}>
                  <select {...register('genre')} className={inputCls}>
                    <option value="">Select genre</option>
                    {['Action','Comedy','Drama','Horror','Romance',
                      'Sci-Fi','Thriller','Documentary','Animation','Other']
                      .map(g => <option key={g} value={g}>{g}</option>)}
                  </select>
                </Field>
                <Field label="Age rating" error={errors.ageRating?.message}>
                  <select {...register('ageRating')} className={inputCls}>
                    <option value="">Select rating</option>
                    {['G','PG','PG-13','R','NC-17'].map(r => (
                      <option key={r} value={r}>{r}</option>))}
                  </select>
                </Field>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <Field label="Ticket price (USD) *" error={errors.ticketPrice?.message}>
                  <div className="relative">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400">$</span>
                    <input {...register('ticketPrice')} type="number" step="0.01"
                      placeholder="9.99" className={inputCls + ' pl-7'} />
                  </div>
                </Field>
                <Field label="Release year" error={errors.releaseYear?.message}>
                  <input {...register('releaseYear')} type="number"
                    placeholder={String(new Date().getFullYear())} className={inputCls} />
                </Field>
              </div>

              <Field label="Language" error={errors.language?.message}>
                <input {...register('language')} placeholder="English" className={inputCls} />
              </Field>

              {error && <ErrBox message={error} />}

              <button type="submit" disabled={isSubmitting}
                className="w-full bg-amber-400 hover:bg-amber-300 disabled:opacity-50
                           text-neutral-950 font-semibold rounded-lg py-3 text-sm transition-colors">
                {isSubmitting ? 'Saving…' : 'Continue to upload →'}
              </button>
            </form>
          </div>
        )}

        {/* ── Step 2: Upload ── */}
        {step === 'upload' && (
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-8">
            <h2 className="text-xl font-semibold mb-2">Upload video file</h2>
            <p className="text-neutral-400 text-sm mb-6">
              Supported: MP4, MKV, MOV, AVI — up to 10 GB
            </p>

            {/* Drop zone */}
            <div
              onClick={() => fileInputRef.current?.click()}
              className="border-2 border-dashed border-neutral-700 hover:border-amber-400/60
                         rounded-xl p-10 text-center cursor-pointer transition-colors mb-6">
              <Film className="w-10 h-10 text-neutral-600 mx-auto mb-3" />
              {selectedFile ? (
                <div>
                  <p className="text-white font-medium">{selectedFile.name}</p>
                  <p className="text-neutral-400 text-sm mt-1">
                    {(selectedFile.size / 1_073_741_824).toFixed(2)} GB
                  </p>
                </div>
              ) : (
                <div>
                  <p className="text-neutral-400 mb-1">Click to select your video file</p>
                  <p className="text-neutral-600 text-sm">or drag and drop</p>
                </div>
              )}
              <input ref={fileInputRef} type="file" className="hidden"
                accept="video/mp4,video/x-matroska,video/quicktime,video/x-msvideo"
                onChange={e => setSelectedFile(e.target.files?.[0] ?? null)} />
            </div>

            {/* Progress bar */}
            {uploading && (
              <div className="mb-6">
                <div className="flex justify-between text-sm mb-1">
                  <span className="text-neutral-400">Uploading…</span>
                  <span className="text-amber-400">{uploadProgress}%</span>
                </div>
                <div className="h-2 bg-neutral-800 rounded-full overflow-hidden">
                  <div className="h-full bg-amber-400 transition-all duration-300 rounded-full"
                    style={{ width: `${uploadProgress}%` }} />
                </div>
              </div>
            )}

            {error && <ErrBox message={error} />}

            <button onClick={handleUpload}
              disabled={!selectedFile || uploading}
              className="w-full bg-amber-400 hover:bg-amber-300 disabled:opacity-50
                         text-neutral-950 font-semibold rounded-lg py-3 text-sm
                         transition-colors flex items-center justify-center gap-2">
              <Upload className="w-4 h-4" />
              {uploading ? `Uploading ${uploadProgress}%…` : 'Upload film'}
            </button>
          </div>
        )}

        {/* ── Step 3: Done ── */}
        {step === 'done' && (
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-10 text-center">
            <div className="text-5xl mb-4">🎬</div>
            <h2 className="text-2xl font-semibold mb-2">Film uploaded!</h2>
            <p className="text-neutral-400 mb-6">
              Your film is now ready. Go to your dashboard to publish it and make it available to watchers.
            </p>
            <div className="flex gap-3 justify-center">
              <Link to="/owner/dashboard"
                className="bg-amber-400 hover:bg-amber-300 text-neutral-950 font-semibold
                           px-6 py-3 rounded-lg text-sm transition-colors">
                Go to dashboard
              </Link>
              <button onClick={() => { setStep('details'); setSelectedFile(null); setUploadProgress(0); }}
                className="border border-neutral-700 hover:border-neutral-500 text-neutral-300
                           hover:text-white px-6 py-3 rounded-lg text-sm transition-colors">
                Upload another
              </button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

// Helpers
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

function ErrBox({ message }: { message: string }) {
  return (
    <div className="bg-red-950 border border-red-800 rounded-lg px-4 py-3">
      <p className="text-sm text-red-300">{message}</p>
    </div>
  );
}