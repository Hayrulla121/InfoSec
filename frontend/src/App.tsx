import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ToastProvider } from './components/Toast';
import { I18nProvider } from './i18n/I18nContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import AppLayout from './layout/AppLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import UsersPage from './pages/UsersPage';
import ThreatModelPage from './pages/ThreatModelPage';
import DictionariesPage from './pages/DictionariesPage';
import InfoSystemsPage from './pages/InfoSystemsPage';
import AssetsPage from './pages/AssetsPage';
import ThreatsPage from './pages/ThreatsPage';
import ControlsPage from './pages/ControlsPage';
import RisksPage from './pages/RisksPage';
import RiskMatrixPage from './pages/RiskMatrixPage';

/**
 * Route table.
 *
 * The nesting matters: every route inside the <Route element={<AppLayout/>}>
 * block renders into that layout's <Outlet/>, so the sidebar is mounted once
 * and survives navigation instead of being torn down and rebuilt per page.
 */
export default function App() {
  return (
    <I18nProvider>
      <ToastProvider>
      <AuthProvider>
        <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route path="/" element={<DashboardPage />} />
            <Route path="/threat-model" element={<ThreatModelPage />} />
            <Route path="/assets" element={<AssetsPage />} />
            <Route path="/threats" element={<ThreatsPage />} />
            <Route path="/risk-matrix" element={<RiskMatrixPage />} />
            <Route path="/risks" element={<RisksPage />} />
            <Route path="/controls" element={<ControlsPage />} />
            <Route path="/dictionaries" element={<DictionariesPage />} />
            <Route path="/info-systems" element={<InfoSystemsPage />} />

            <Route
              path="/admin/users"
              element={
                <ProtectedRoute adminOnly>
                  <UsersPage />
                </ProtectedRoute>
              }
            />
          </Route>

          {/* Anything unrecognised goes home rather than showing a blank page. */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
      </ToastProvider>
    </I18nProvider>
  );
}
