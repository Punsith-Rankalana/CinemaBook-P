import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

const loginSchema = z.object({
  login:    z.string().min(1, 'Email or username is required'),
  password: z.string().min(1, 'Password is required'),
});

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const navigate  = useNavigate();
  const location  = useLocation();
  const { login, isLoading } = useAuthStore();

  const from = (location.state as { from?: Location })?.from?.pathname ?? '/dashboard';

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (data: LoginForm) => {
    try {
      await login(data.login, data.password);
      navigate(from, { replace: true });
    } catch (err: any) {
      const message = err?.response?.data?.error ?? 'Login failed. Please try again.';
      setError('root', { message });
    }
  };

  return (
    <div className="min-h-screen bg-neutral-950 flex items-center justify-center px-4">
      <div className="w-full max-w-md">

        {/* Brand */}
        <div className="text-center mb-10">
          <h1 className="text-4xl font-bold text-white tracking-tight">
            Cinema<span className="text-amber-400">Book</span>
          </h1>
          <p className="text-neutral-400 mt-2 text-sm">Your cinema. Your world.</p>
        </div>

        {/* Card */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-8">
          <h2 className="text-xl font-semibold text-white mb-6">Sign in</h2>

          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">

            <div>
              <label className="block text-sm text-neutral-300 mb-1.5">
                Email or username
              </label>
              <input
                {...register('login')}
                type="text"
                autoComplete="username"
                placeholder="you@example.com"
                className="w-full bg-neutral-800 border border-neutral-700 rounded-lg
                           px-4 py-2.5 text-white placeholder-neutral-500 text-sm
                           focus:outline-none focus:border-amber-400 transition-colors"
              />
              {errors.login && (
                <p className="mt-1 text-xs text-red-400">{errors.login.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm text-neutral-300 mb-1.5">
                Password
              </label>
              <input
                {...register('password')}
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                className="w-full bg-neutral-800 border border-neutral-700 rounded-lg
                           px-4 py-2.5 text-white placeholder-neutral-500 text-sm
                           focus:outline-none focus:border-amber-400 transition-colors"
              />
              {errors.password && (
                <p className="mt-1 text-xs text-red-400">{errors.password.message}</p>
              )}
            </div>

            {errors.root && (
              <div className="bg-red-950 border border-red-800 rounded-lg px-4 py-3">
                <p className="text-sm text-red-300">{errors.root.message}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-amber-400 hover:bg-amber-300 disabled:opacity-50
                         text-neutral-950 font-semibold rounded-lg py-2.5 text-sm
                         transition-colors"
            >
              {isLoading ? 'Signing in…' : 'Sign in'}
            </button>

          </form>

          <div className="mt-6 pt-6 border-t border-neutral-800 text-center space-y-2">
            <p className="text-sm text-neutral-400">
              New to CinemaBook?{' '}
              <Link to="/register" className="text-amber-400 hover:text-amber-300">
                Create a watcher account
              </Link>
            </p>
            <p className="text-sm text-neutral-400">
              Own films?{' '}
              <Link to="/register/film-owner" className="text-amber-400 hover:text-amber-300">
                Register as a film owner
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
