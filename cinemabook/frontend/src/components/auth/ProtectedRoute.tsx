import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import type { UserType } from '../../types';

interface ProtectedRouteProps {
  /** If provided, only users with this role can access the route. */
  requiredRole?: UserType;
  /** Where to redirect unauthenticated users (default: /login). */
  redirectTo?: string;
}

/**
 * Wraps a route so only authenticated users can access it.
 * Optionally restricts to a specific user type (WATCHER or FILM_OWNER).
 *
 * Usage in router:
 *   <Route element={<ProtectedRoute />}>
 *     <Route path="/dashboard" element={<Dashboard />} />
 *   </Route>
 *
 *   <Route element={<ProtectedRoute requiredRole="FILM_OWNER" />}>
 *     <Route path="/owner/upload" element={<UploadFilm />} />
 *   </Route>
 */
export function ProtectedRoute({
  requiredRole,
  redirectTo = '/login',
}: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuthStore();
  const location = useLocation();

  if (!isAuthenticated || !user) {
    return <Navigate to={redirectTo} state={{ from: location }} replace />;
  }

  if (requiredRole && user.userType !== requiredRole) {
    // Authenticated but wrong role → send to their own dashboard
    const ownDashboard = user.userType === 'FILM_OWNER'
      ? '/owner/dashboard'
      : '/dashboard';
    return <Navigate to={ownDashboard} replace />;
  }

  return <Outlet />;
}
