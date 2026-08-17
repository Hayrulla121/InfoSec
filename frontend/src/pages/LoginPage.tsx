import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { errorMessage } from '../api/client';
import { IconAlert, IconShield } from '../components/Icons';
import { useI18n } from '../i18n/I18nContext';
import { LanguageSwitcher } from '../components/LanguageSwitcher';

export default function LoginPage() {
  const { login } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();
  const location = useLocation();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Where ProtectedRoute wanted to send them before the login interruption.
  const from = (location.state as { from?: string } | null)?.from ?? '/';

  async function onSubmit(e: FormEvent) {
    // Without this the browser does a full page reload and the request is lost.
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username, password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(errorMessage(err, t.login.failed));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-shell">
      <form className="login-card" onSubmit={onSubmit}>
        <div className="login-brand">
          <span className="brand-mark" aria-hidden="true">
            <IconShield size={20} />
          </span>
          <h1>{t.brand.fullTitle}</h1>
        </div>
        <p className="muted">{t.login.prompt}</p>

        <div className="field">
          <label htmlFor="username">{t.login.username}</label>
          <input
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            autoFocus
            required
          />
        </div>

        <div className="field">
          <label htmlFor="password">{t.login.password}</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </div>

        {error && (
          <p className="form-error">
            <IconAlert size={16} />
            <span>{error}</span>
          </p>
        )}

        <button type="submit" disabled={submitting}>
          {submitting ? t.login.submitting : t.login.submit}
        </button>

        <p className="login-hint">{t.login.footer}</p>
        <div className="login-lang">
          <LanguageSwitcher />
        </div>
      </form>
    </div>
  );
}
