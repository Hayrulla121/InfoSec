import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './AuthContext';
import { useI18n } from '../i18n/I18nContext';

/**
 * Wraps routes that require a login.
 *
 * This is a UX guard, not a security boundary: anyone can edit JavaScript in
 * their browser. The real enforcement is the backend returning 401/403.
 */
export function ProtectedRoute({ children, adminOnly = false }: {
  children: ReactNode;
  adminOnly?: boolean;
}) {
  const { user, initialising, isAdmin } = useAuth();
  const { t } = useI18n();
  const location = useLocation();

  // Without this the app would flash the login page for a moment on every
  // refresh, before /api/auth/me came back.
  if (initialising) {
    return <div className="centered-message">{t.action.loading}</div>;
  }

  if (!user) {
    // `state` remembers where they were heading so login can send them back.
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (adminOnly && !isAdmin) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
