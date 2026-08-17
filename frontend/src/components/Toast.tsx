import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { IconAlert, IconCheck, IconClose } from './Icons';

type ToastKind = 'success' | 'error' | 'info';

interface Toast {
  id: number;
  kind: ToastKind;
  message: string;
}

interface ToastApi {
  success: (message: string) => void;
  error: (message: string) => void;
  info: (message: string) => void;
}

const ToastContext = createContext<ToastApi | null>(null);

/**
 * Transient feedback for actions ("saved", "export failed").
 *
 * Deliberately NOT used for form validation - a field error belongs next to
 * the field it describes, where the user is already looking. Toasts are for
 * things that happen away from the cursor.
 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const push = useCallback(
    (kind: ToastKind, message: string) => {
      // Date.now() collides when two toasts fire in the same millisecond, so
      // mix in a random component for the React key.
      const id = Date.now() + Math.random();
      setToasts((prev) => [...prev, { id, kind, message }]);
      // Errors stay longer: they usually carry something worth reading.
      window.setTimeout(() => dismiss(id), kind === 'error' ? 7000 : 4000);
    },
    [dismiss],
  );

  const api = useMemo<ToastApi>(
    () => ({
      success: (m) => push('success', m),
      error: (m) => push('error', m),
      info: (m) => push('info', m),
    }),
    [push],
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      {/* aria-live: screen readers announce new toasts without stealing focus */}
      <div className="toast-stack" role="status" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.kind}`}>
            <span className="toast-icon">
              {t.kind === 'success' ? <IconCheck size={16} /> : <IconAlert size={16} />}
            </span>
            <span className="toast-message">{t.message}</span>
            <button className="toast-close" onClick={() => dismiss(t.id)} aria-label="Close">
              <IconClose size={14} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error('useToast must be used inside <ToastProvider>');
  }
  return ctx;
}
