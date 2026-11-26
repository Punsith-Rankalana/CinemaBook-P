import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProtectedRoute }            from './components/auth/ProtectedRoute';
import { useCurrentUser }            from './hooks/useCurrentUser';

// Auth pages
import { LoginPage }             from './pages/auth/LoginPage';
import { RegisterWatcherPage }   from './pages/auth/RegisterWatcherPage';
import { RegisterFilmOwnerPage } from './pages/auth/RegisterFilmOwnerPage';

// Watcher pages
import { WatcherDashboard }  from './pages/dashboard/WatcherDashboard';

// Owner pages (Phase 2)
import { OwnerDashboard }   from './pages/dashboard/OwnerDashboard';
import { UploadFilmPage }   from './pages/owner/UploadFilmPage';
import { BankLinkPage }     from './pages/owner/BankLinkPage';

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 60_000, retry: 1 } },
});

function AppRoutes() {
  useCurrentUser();

  return (
    <Routes>
      {/* Public */}
      <Route path="/login"               element={<LoginPage />} />
      <Route path="/register"            element={<RegisterWatcherPage />} />
      <Route path="/register/film-owner" element={<RegisterFilmOwnerPage />} />
      <Route path="/"                    element={<Navigate to="/login" replace />} />

      {/* Watcher */}
      <Route element={<ProtectedRoute requiredRole="WATCHER" />}>
        <Route path="/dashboard" element={<WatcherDashboard />} />
        {/* Phase 3: /films, /films/:id, /watch/:ticketId */}
      </Route>

      {/* Film owner */}
      <Route element={<ProtectedRoute requiredRole="FILM_OWNER" />}>
        <Route path="/owner/dashboard" element={<OwnerDashboard />} />
        <Route path="/owner/upload"    element={<UploadFilmPage />} />
        <Route path="/owner/bank"      element={<BankLinkPage />} />
        {/* Phase 2 additions: /owner/films/:id, /owner/films/:id/edit */}
      </Route>

      {/* Any authenticated */}
      <Route element={<ProtectedRoute />}>
        {/* Phase 6: /profile/:username, /settings */}
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </QueryClientProvider>
  );
}