import { useAuthStore } from '../../store/authStore';
import { useNavigate } from 'react-router-dom';

export function WatcherDashboard() {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-neutral-950 text-white">
      <header className="border-b border-neutral-800 px-6 py-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold">
          Cinema<span className="text-amber-400">Book</span>
        </h1>
        <div className="flex items-center gap-4">
          <span className="text-neutral-400 text-sm">
            Hello, <span className="text-white">{user?.displayName}</span>
          </span>
          <button
            onClick={handleLogout}
            className="text-sm text-neutral-400 hover:text-white transition-colors"
          >
            Sign out
          </button>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-12">
        <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-10 text-center">
          <div className="text-5xl mb-4">🎬</div>
          <h2 className="text-2xl font-semibold mb-2">Welcome to CinemaBook</h2>
          <p className="text-neutral-400">
            Phase 2 coming soon — browse and buy film tickets here.
          </p>
          <div className="mt-6 inline-flex items-center gap-2 bg-amber-400/10 border border-amber-400/30
                          text-amber-400 text-sm px-4 py-2 rounded-full">
            ✓ Phase 1 complete — you're logged in as a Watcher
          </div>
        </div>
      </main>
    </div>
  );
}
