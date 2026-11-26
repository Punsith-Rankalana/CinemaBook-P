import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProtectedRoute }             from './components/auth/ProtectedRoute';
import { useCurrentUser }             from './hooks/useCurrentUser';

// Pages
import { LoginPage }              from './pages/auth/LoginPage';
import { RegisterWatcherPage }    from './pages/auth/RegisterWatcherPage';
import { RegisterFilmOwnerPage }  from './pages/auth/RegisterFilmOwnerPage';
import { WatcherDashboard }       from './pages/dashboard/WatcherDashboard';
import { OwnerDashboard }         from './pages/dashboard/OwnerDashboard';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 60_000, retry: 1 },
  },
});

// Separate component so useCurrentUser can access QueryClient context
function AppRoutes() {
  useCurrentUser();   // syncs /users/me into the auth store on startup

  return (
    <Routes>

      {/* Public */}
      <Route path="/login"               element={<LoginPage />} />
      <Route path="/register"            element={<RegisterWatcherPage />} />
      <Route path="/register/film-owner" element={<RegisterFilmOwnerPage />} />
      <Route path="/"                    element={<Navigate to="/login" replace />} />

      {/* Watcher only */}
      <Route element={<ProtectedRoute requiredRole="WATCHER" />}>
        <Route path="/dashboard"         element={<WatcherDashboard />} />
        {/* Phase 3: /films, /films/:id, /watch/:ticketId  */}
        {/* Phase 4: /social, /social/post/:id             */}
        {/* Phase 5: /messages, /messages/:conversationId  */}
      </Route>

      {/* Film owner only */}
      <Route element={<ProtectedRoute requiredRole="FILM_OWNER" />}>
        <Route path="/owner/dashboard"   element={<OwnerDashboard />} />
        {/* Phase 2: /owner/upload, /owner/films, /owner/earnings */}
      </Route>

      {/* Any authenticated user */}
      <Route element={<ProtectedRoute />}>
        {/* Phase 6: /profile/:username, /settings */}
      </Route>

      {/* 404 */}
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
