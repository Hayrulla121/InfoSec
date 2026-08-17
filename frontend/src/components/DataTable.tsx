import type { ReactNode } from 'react';
import type { Page } from '../api/registries';
import { IconChevronLeft, IconChevronRight, IconClose, IconInbox } from './Icons';
import { useI18n } from '../i18n/I18nContext';

export interface Column<T> {
  key: string;
  header: string;
  /** Cell renderer. Returning a node (not a string) allows badges and chips. */
  render: (row: T) => ReactNode;
  width?: string;
}

/** Shimmer placeholder that keeps the table's shape while data loads. */
function SkeletonRows({ columns, rows = 5 }: { columns: number; rows?: number }) {
  return (
    <div aria-hidden="true">
      {Array.from({ length: rows }).map((_, r) => (
        <div className="skeleton-row" key={r}>
          {Array.from({ length: columns }).map((__, c) => (
            <div
              className="skeleton-bar"
              key={c}
              style={{
                // Varying widths so it reads as text, not a progress bar
                flex: c === 0 ? '0 0 56px' : c % 3 === 0 ? '2' : '1',
                opacity: 1 - r * 0.13,
              }}
            />
          ))}
        </div>
      ))}
    </div>
  );
}

export function EmptyState({ title, hint }: { title: string; hint?: string }) {
  return (
    <div className="empty-state">
      <span className="empty-icon">
        <IconInbox size={22} />
      </span>
      <strong>{title}</strong>
      {hint && <span>{hint}</span>}
    </div>
  );
}

/**
 * Presentational table with server-side paging.
 *
 * It owns no data of its own - the page object and the callbacks come from the
 * parent. That keeps it reusable across all four registries and trivial to
 * reason about.
 */
export function DataTable<T extends { id: number }>({
  page,
  columns,
  loading,
  onPageChange,
  rowActions,
  emptyMessage,
  emptyHint,
}: {
  page: Page<T> | null;
  columns: Column<T>[];
  loading?: boolean;
  onPageChange: (page: number) => void;
  rowActions?: (row: T) => ReactNode;
  emptyMessage?: string;
  emptyHint?: string;
}) {
  const { t } = useI18n();
  if (loading && !page) {
    return <SkeletonRows columns={Math.min(columns.length, 6)} />;
  }
  if (!page || page.content.length === 0) {
    return <EmptyState title={emptyMessage ?? t.table.empty} hint={emptyHint ?? t.table.emptyHint} />;
  }

  return (
    <>
      <div className="table-scroll">
        <table className="data-table">
          <thead>
            <tr>
              {columns.map((c) => (
                <th key={c.key} style={c.width ? { width: c.width } : undefined}>
                  {c.header}
                </th>
              ))}
              {rowActions && <th className="actions">{t.action.actions}</th>}
            </tr>
          </thead>
          <tbody>
            {page.content.map((row) => (
              <tr key={row.id}>
                {columns.map((c) => (
                  <td key={c.key}>{c.render(row)}</td>
                ))}
                {rowActions && <td className="actions">{rowActions(row)}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {page.totalPages > 1 ? (
        <div className="pager">
          <button
            className="secondary-button"
            disabled={page.number === 0}
            onClick={() => onPageChange(page.number - 1)}
          >
            <IconChevronLeft size={15} />
            {t.action.back}
          </button>
          <span>{t.table.page(page.number + 1, page.totalPages, page.totalElements)}</span>
          <button
            className="secondary-button"
            disabled={page.number >= page.totalPages - 1}
            onClick={() => onPageChange(page.number + 1)}
          >
            {t.action.forward}
            <IconChevronRight size={15} />
          </button>
        </div>
      ) : (
        <p className="pager">{t.table.total(page.totalElements)}</p>
      )}
    </>
  );
}

/** 1 green .. 5 red, matching the risk matrix colours. */
export function LevelBadge({ level, title }: { level: number; title?: string }) {
  return (
    <span className={`level-badge level-${level}`} title={title}>
      {level}
    </span>
  );
}

/** Level chip plus its label, the pairing used in every registry table. */
export function LevelWithLabel({ level, label }: { level: number | null; label: string | null }) {
  if (!level) {
    return <span className="muted">—</span>;
  }
  return (
    <span className="level-pair">
      <LevelBadge level={level} />
      <span>{label}</span>
    </span>
  );
}

/** Simple modal used by every registry's create/edit form. */
export function Modal({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  return (
    // Clicking the dark backdrop closes; stopPropagation keeps clicks inside
    // the dialog from bubbling up and closing it too.
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h2>{title}</h2>
          <button className="icon-button" onClick={onClose} aria-label="Close">
            <IconClose size={16} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function ConfirmDialog({
  message,
  onConfirm,
  onCancel,
}: {
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  const { t } = useI18n();
  return (
    <Modal title={t.action.confirm} onClose={onCancel}>
      <p>{message}</p>
      <div className="button-row">
        <button className="danger" onClick={onConfirm}>
          {t.action.delete}
        </button>
        <button className="secondary-button" onClick={onCancel}>
          {t.action.cancel}
        </button>
      </div>
    </Modal>
  );
}
