import { Fragment, useState, type ComponentType } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useI18n } from '../i18n/I18nContext';
import { ExportWorkbookButton } from '../components/ExportWorkbookButton';
import { LoadDemoDataButton } from '../components/LoadDemoDataButton';
import { LanguageSwitcher } from '../components/LanguageSwitcher';
import {
  IconAlert,
  IconBook,
  IconClipboard,
  IconDatabase,
  IconGrid,
  IconHome,
  IconLogout,
  IconServer,
  IconShield,
  IconSliders,
  IconUsers,
} from '../components/Icons';
import type { Dictionary } from '../i18n/ru';

/**
 * Navigation items. The label is a function of the dictionary rather than a
 * literal, so the whole menu re-renders in the selected language without any
 * per-item conditionals.
 */
const NAV_ITEMS: {
  to: string;
  label: (t: Dictionary) => string;
  icon: ComponentType<{ size?: number }>;
}[] = [
  { to: '/threat-model', label: (t) => t.nav.threatModel, icon: IconBook },
  { to: '/assets', label: (t) => t.nav.assets, icon: IconDatabase },
  { to: '/threats', label: (t) => t.nav.threats, icon: IconAlert },
  { to: '/risk-matrix', label: (t) => t.nav.riskMatrix, icon: IconGrid },
  { to: '/risks', label: (t) => t.nav.risks, icon: IconClipboard },
  { to: '/controls', label: (t) => t.nav.controls, icon: IconShield },
  { to: '/dictionaries', label: (t) => t.nav.dictionaries, icon: IconSliders },
  { to: '/info-systems', label: (t) => t.nav.infoSystems, icon: IconServer },
];

/**
 * The persistent shell: left navigation panel + scrolling content area.
 *
 * <Outlet /> is React Router's placeholder for "whichever child route matched",
 * which is what keeps the sidebar mounted while only the main pane changes.
 */
export default function AppLayout() {
  const { user, isAdmin, logout } = useAuth();
  const { t } = useI18n();
  const navigate = useNavigate();

  /**
   * Bumped after the demo seed. It is used as a React `key` below, and changing
   * a key remounts the subtree — which re-runs every page's data-loading effect.
   * Navigating to the current path would not: same route, no unmount, no refetch.
   */
  const [dataVersion, setDataVersion] = useState(0);

  function onLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  /** Initials for the avatar chip, e.g. "Риск-аналитик" -> "РА". */
  const initials = (user?.fullName ?? '?')
    .split(/[\s-]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase())
    .join('');

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            <IconShield size={20} />
          </span>
          <span className="brand-text">
            <span className="brand-title">{t.brand.title}</span>
            <span className="brand-sub">{t.brand.subtitle}</span>
          </span>
        </div>

        <nav aria-label={t.nav.modules}>
          <NavLink to="/" end className="nav-link">
            <IconHome size={18} />
            <span>{t.nav.home}</span>
          </NavLink>

          <div className="nav-section">{t.nav.modules}</div>
          {NAV_ITEMS.map(({ to, label, icon: Glyph }) => (
            <NavLink key={to} to={to} className="nav-link">
              <Glyph size={18} />
              <span>{label(t)}</span>
            </NavLink>
          ))}

          {isAdmin && (
            <>
              <div className="nav-section">{t.nav.administration}</div>
              <NavLink to="/admin/users" className="nav-link">
                <IconUsers size={18} />
                <span>{t.nav.users}</span>
              </NavLink>
            </>
          )}
        </nav>

        <div className="sidebar-footer">
          <div className="user-chip">
            <span className="avatar" aria-hidden="true">
              {initials}
            </span>
            <span className="user-meta">
              <span className="user-name">{user?.fullName}</span>
              <span className="user-role">
                {user?.role === 'ADMIN' ? t.role.admin : t.role.user}
              </span>
            </span>
          </div>

          <LanguageSwitcher />

          {/* Seeding writes across every registry, so it is admin-only. */}
          {isAdmin && <LoadDemoDataButton onLoaded={() => setDataVersion((v) => v + 1)} />}

          <ExportWorkbookButton />

          <button className="sidebar-action" onClick={onLogout}>
            <IconLogout size={16} />
            <span>{t.action.logout}</span>
          </button>
        </div>
      </aside>

      <main className="content">
        {/* A keyed Fragment remounts the page without adding a DOM node. */}
        <Fragment key={dataVersion}>
          <Outlet />
        </Fragment>
      </main>
    </div>
  );
}
