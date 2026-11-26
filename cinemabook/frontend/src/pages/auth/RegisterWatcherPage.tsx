import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { authService } from '../../services/authService';

const schema = z.object({
  email:       z.string().email('Must be a valid email'),
  username:    z.string()
                 .min(3, 'At least 3 characters')
                 .max(50)
                 .regex(/^[a-zA-Z0-9_]+$/, 'Letters, numbers, and underscores only'),
  password:    z.string().min(8, 'At least 8 characters'),
  confirm:     z.string(),
  displayName: z.string().optional(),
}).refine(d => d.password === d.confirm, {
  message: 'Passwords do not match',
  path: ['confirm'],
});

type Form = z.infer<typeof schema>;

export function RegisterWatcherPage() {
  const navigate = useNavigate();
  const [success, setSuccess] = useState('');

  const {
    register, handleSubmit,
    setError, formState: { errors, isSubmitting },
  } = useForm<Form>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: Form) => {
    try {
      const res = await authService.registerWatcher({
        email:       data.email,
        username:    data.username,
        password:    data.password,
        displayName: data.displayName,
      });
      setSuccess(res.message);
      setTimeout(() => navigate('/login'), 3000);
    } catch (err: any) {
      const message = err?.response?.data?.error ?? 'Registration failed';
      setError('root', { message });
    }
  };

  return (
    <div className="min-h-screen bg-neutral-950 flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md">

        <div className="text-center mb-10">
          <h1 className="text-4xl font-bold text-white tracking-tight">
            Cinema<span className="text-amber-400">Book</span>
          </h1>
          <p className="text-neutral-400 mt-2 text-sm">Create your watcher account</p>
        </div>

        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-8">

          {success ? (
            <div className="text-center">
              <div className="text-4xl mb-4">🎬</div>
              <p className="text-green-400 font-medium">{success}</p>
              <p className="text-neutral-400 text-sm mt-2">Redirecting to login…</p>
            </div>
          ) : (
            <>
              <h2 className="text-xl font-semibold text-white mb-6">Create account</h2>

              <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">

                {[
                  { id: 'email',       label: 'Email',        type: 'email',    placeholder: 'you@example.com' },
                  { id: 'username',    label: 'Username',     type: 'text',     placeholder: 'cinefan42' },
                  { id: 'displayName', label: 'Display name (optional)', type: 'text', placeholder: 'Your name' },
                  { id: 'password',   label: 'Password',     type: 'password', placeholder: '8+ characters' },
                  { id: 'confirm',    label: 'Confirm password', type: 'password', placeholder: 'Repeat password' },
                ].map(({ id, label, type, placeholder }) => (
                  <div key={id}>
                    <label className="block text-sm text-neutral-300 mb-1.5">{label}</label>
                    <input
                      {...register(id as keyof Form)}
                      type={type}
                      placeholder={placeholder}
                      className="w-full bg-neutral-800 border border-neutral-700 rounded-lg
                                 px-4 py-2.5 text-white placeholder-neutral-500 text-sm
                                 focus:outline-none focus:border-amber-400 transition-colors"
                    />
                    {errors[id as keyof Form] && (
                      <p className="mt-1 text-xs text-red-400">
                        {errors[id as keyof Form]?.message}
                      </p>
                    )}
                  </div>
                ))}

                {errors.root && (
                  <div className="bg-red-950 border border-red-800 rounded-lg px-4 py-3">
                    <p className="text-sm text-red-300">{errors.root.message}</p>
                  </div>
                )}

                <button
                  type="submit" disabled={isSubmitting}
                  className="w-full bg-amber-400 hover:bg-amber-300 disabled:opacity-50
                             text-neutral-950 font-semibold rounded-lg py-2.5 text-sm
                             transition-colors mt-2"
                >
                  {isSubmitting ? 'Creating account…' : 'Create account'}
                </button>

              </form>

              <p className="mt-6 text-center text-sm text-neutral-400">
                Already have an account?{' '}
                <Link to="/login" className="text-amber-400 hover:text-amber-300">Sign in</Link>
              </p>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
