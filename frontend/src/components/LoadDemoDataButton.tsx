import { useState } from 'react';
import { api, errorMessage } from '../api/client';
import { useI18n } from '../i18n/I18nContext';
import { IconDatabase } from './Icons';
import { useToast } from './Toast';

interface DemoSummary {
  infoSystems: number;
  assets: number;
  threats: number;
  controls: number;
  risks: number;
  controlLinks: number;
}

/**
 * Seeds a demonstration dataset so the platform can be shown without entering
 * thirty records by hand.
 *
 * <p>The server refuses when any registry already holds data, and that 409 is
 * surfaced as-is: demo rows mixed into a real register would be impossible to
 * tell apart from ones a colleague entered.
 */
export function LoadDemoDataButton({ onLoaded }: { onLoaded?: () => void }) {
  const { t } = useI18n();
  const toast = useToast();
  const [busy, setBusy] = useState(false);

  async function load() {
    setBusy(true);
    try {
      const { data } = await api.post<DemoSummary>('/api/admin/demo-data');
      toast.success(t.toast.demoLoaded(data.risks, data.assets, data.threats));
      // The caller reloads whatever it is showing; otherwise the user stares at
      // an empty table until they navigate away and back.
      onLoaded?.();
    } catch (e) {
      toast.error(errorMessage(e, t.toast.demoFailed));
    } finally {
      setBusy(false);
    }
  }

  return (
    <button className="sidebar-action" onClick={() => void load()} disabled={busy}>
      <IconDatabase size={16} />
      <span>{busy ? t.sidebar.loadingDemo : t.sidebar.loadDemo}</span>
    </button>
  );
}
