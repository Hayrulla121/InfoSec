import { useState } from 'react';
import { api, errorMessage } from '../api/client';
import { IconDownload } from './Icons';
import { useToast } from './Toast';
import { useI18n } from '../i18n/I18nContext';

/**
 * Downloads the full Excel workbook - all eight sheets with live formulas.
 *
 * A plain <a href> will not do: the endpoint needs the Authorization header,
 * and a browser navigation sends none. So the file is fetched as a blob
 * through the axios instance (which attaches the token) and handed to a
 * temporary object URL.
 */
export function ExportWorkbookButton() {
  const toast = useToast();
  const { t } = useI18n();
  const [busy, setBusy] = useState(false);

  async function download() {
    setBusy(true);
    try {
      const response = await api.get('/api/export/workbook', { responseType: 'blob' });

      // RFC 5987: the Cyrillic name travels in filename*=UTF-8''<percent-encoded>
      const disposition = String(response.headers['content-disposition'] ?? '');
      const utf8 = disposition.match(/filename\*=UTF-8''([^;]+)/);
      const plain = disposition.match(/filename="([^"]+)"/);
      const filename = utf8
        ? decodeURIComponent(utf8[1])
        : (plain?.[1] ?? 'risk-assessment.xlsx');

      const url = URL.createObjectURL(response.data as Blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);

      // Anything the Excel layout could not represent comes back in a header
      // rather than being silently dropped.
      const warnings = response.headers['x-export-warnings'];
      if (warnings) {
        toast.error(t.toast.exportedWithWarnings(decodeURIComponent(String(warnings))));
      } else {
        toast.success(t.toast.exported);
      }
    } catch (e) {
      toast.error(errorMessage(e, t.toast.exportFailed));
    } finally {
      setBusy(false);
    }
  }

  return (
    <button className="sidebar-action" onClick={() => void download()} disabled={busy}>
      <IconDownload size={16} />
      <span>{busy ? t.sidebar.exporting : t.sidebar.exportExcel}</span>
    </button>
  );
}
