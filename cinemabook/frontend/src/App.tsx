import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProtectedRoute }            from './components/auth/ProtectedRoute';
import { useCurrentUser }            from './hooks/useCurrentUser';

// Auth
import { LoginPage }             from './pages/auth/LoginPage';
import { RegisterWatcherPage }   from './pages/auth/RegisterWatcherPage';
import { RegisterFilmOwnerPage } from './pages/auth/RegisterFilmOwnerPage';

// Watcher (Phase 3)
import { WatcherDashboard }  from './pages/dashboard/WatcherDashboard';
import { FilmDetailPage }    from './pages/watcher/FilmDetailPage';
import { WatchPage }         from './pages/watcher/WatchPage';

// Owner (Phase 2)
import { OwnerDashboard }   from './pages/dashboard/OwnerDashboard';
import { UploadFilmPage }   from './pages/owner/UploadFilmPage';
import { BankLinkPage }     from './pages/owner/BankLinkPage';
import { EditFilmPage }     from './pages/owner/EditFilmPage';

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
        <Route path="/dashboard"           element={<WatcherDashboard />} />
        <Route path="/films/:filmId"       element={<FilmDetailPage />} />
        <Route path="/watch/:filmId"       element={<WatchPage />} />
      </Route>

      {/* Film owner */}
      <Route element={<ProtectedRoute requiredRole="FILM_OWNER" />}>
        <Route path="/owner/dashboard"            element={<OwnerDashboard />} />
        <Route path="/owner/upload"               element={<UploadFilmPage />} />
        <Route path="/owner/bank"                 element={<BankLinkPage />} />
        <Route path="/owner/films/:filmId/edit"   element={<EditFilmPage />} />
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