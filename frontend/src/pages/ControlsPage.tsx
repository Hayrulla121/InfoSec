import { useCallback, useEffect, useState } from 'react';
import { controlsApi, type Control, type ControlRequest, type Page } from '../api/registries';
import { getOptions } from '../api/dictionaries';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ConfirmDialog, DataTable, Modal, type Column } from '../components/DataTable';
import { ColumnFilters, useRegistryFilters, type FilterDef } from '../components/ColumnFilters';
import { IconPlus, IconSearch } from '../components/Icons';
import { useI18n } from '../i18n/I18nContext';

const EMPTY: ControlRequest = {
  name: '',
  description: '',
  treatmentMethod: '',
  reductionPct: 0.2,
  implemented: false,
};

export default function ControlsPage() {
  const { can } = useAuth();
  const { t, method } = useI18n();
  const [page, setPage] = useState<Page<Control> | null>(null);
  const [pageNo, setPageNo] = useState(0);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [methods, setMethods] = useState<string[]>([]);

  const [editing, setEditing] = useState<Control | null>(null);
  const [form, setForm] = useState<ControlRequest>(EMPTY);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState<Control | null>(null);

  const { filters, facets, setFilter, resetFilters, refreshFacets } = useRegistryFilters(
    controlsApi.facets,
    () => setPageNo(0),
  );

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setPage(await controlsApi.list({ page: pageNo, search, filters }));
      setError(null);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [pageNo, search, filters]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    getOptions('TREATMENT_METHOD').then(setMethods).catch(() => setMethods([]));
  }, []);

  function openCreate() {
    setEditing(null);
    setForm({ ...EMPTY, treatmentMethod: methods[0] ?? '' });
    setShowForm(true);
  }

  function openEdit(c: Control) {
    setEditing(c);
    setForm({
      name: c.name,
      description: c.description ?? '',
      treatmentMethod: c.treatmentMethod,
      reductionPct: c.reductionPct,
      implemented: c.implemented,
    });
    setShowForm(true);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (editing) {
        await controlsApi.update(editing.id, form);
      } else {
        await controlsApi.create(form);
      }
      setShowForm(false);
      await load();
      refreshFacets();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await controlsApi.remove(deleting.id);
      setDeleting(null);
      await load();
      refreshFacets();
    } catch (err) {
      setError(errorMessage(err));
      setDeleting(null);
    }
  }

  const filterDefs: FilterDef[] = [
    { field: 'treatmentMethod', label: t.controls.colMethod, translate: method },
    // Not a facet: a boolean has exactly two values, so they are fixed here
    // rather than fetched. Counts are omitted for the same reason.
    {
      field: 'implemented',
      label: t.controls.colImplemented,
      options: [
        { value: 'true', count: 0 },
        { value: 'false', count: 0 },
      ],
      translate: (v) => (v === 'true' ? t.controls.yes : t.controls.no),
    },
  ];

  const columns: Column<Control>[] = [
    { key: 'code', header: t.controls.colId, width: '70px', render: (c) => <strong>{c.code}</strong> },
    { key: 'name', header: t.controls.colName, render: (c) => <span className="cell-text">{c.name}</span> },
    {
      key: 'desc',
      header: t.controls.colDescription,
      render: (c) => <span className="cell-text muted">{c.description || t.common.none}</span>,
    },
    { key: 'method', header: t.controls.colMethod, width: '150px', render: (c) => method(c.treatmentMethod) },
    {
      key: 'pct',
      header: t.controls.colReduction,
      width: '110px',
      // Stored as a share of 1; shown as a percentage.
      render: (c) => <strong>{Math.round(Number(c.reductionPct) * 100)}%</strong>,
    },
    {
      key: 'impl',
      header: t.controls.colImplemented,
      width: '110px',
      render: (c) => (
        <span className={c.implemented ? 'badge badge-ok' : 'badge badge-off'}>
          {c.implemented ? t.controls.yes : t.controls.no}
        </span>
      ),
    },
  ];

  return (
    <div>
      <div className="page-header">
        <div className="page-heading">
          <h1>{t.controls.title}</h1>
          <p className="muted">
            {t.controls.subtitle}
          </p>
        </div>
        {can('CONTROLS', 'CREATE') && (
          <button onClick={openCreate}>
            <IconPlus size={16} />
            {t.controls.add}
          </button>
        )}
      </div>

      {error && <p className="form-error">{error}</p>}

      <section className="panel">
        <div className="toolbar">
          <div className="search-field">
            <IconSearch size={16} />
            <input
            placeholder={t.controls.search}
            value={search}
            onChange={(e) => {
              setPageNo(0);
              setSearch(e.target.value);
            }}
          />
          </div>
        </div>

        <ColumnFilters
          defs={filterDefs}
          facets={facets}
          values={filters}
          onChange={setFilter}
          onReset={resetFilters}
          matched={page?.totalElements}
        />

        <DataTable
          page={page}
          columns={columns}
          loading={loading}
          onPageChange={setPageNo}
          rowActions={(c) => (
            <>
              {can('CONTROLS', 'UPDATE') && (
                <button className="link-button" onClick={() => openEdit(c)}>
                  {t.action.edit}
                </button>
              )}
              {can('CONTROLS', 'DELETE') && (
                <button className="link-button danger-link" onClick={() => setDeleting(c)}>
                  {t.action.delete}
                </button>
              )}
            </>
          )}
        />
      </section>

      {showForm && (
        <Modal
          title={editing ? t.controls.editTitle(editing.code) : t.controls.newTitle}
          onClose={() => setShowForm(false)}
        >
          <form onSubmit={submit} className="form-grid">
            <label>
              {t.controls.fieldName}
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </label>

            <label>
              {t.controls.fieldDescription}
              <textarea
                rows={3}
                value={form.description ?? ''}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </label>

            <label>
              {t.controls.fieldMethod}
              <select
                value={form.treatmentMethod}
                onChange={(e) => setForm({ ...form, treatmentMethod: e.target.value })}
                required
              >
                <option value="" disabled>
                  {t.common.select}
                </option>
                {methods.map((m) => (
                  <option key={m} value={m}>
                    {method(m)}
                  </option>
                ))}
              </select>
            </label>

            <label>
              {t.controls.fieldReduction}
              <div className="pct-row">
                <input
                  type="range"
                  min={0}
                  max={100}
                  step={5}
                  value={Math.round(form.reductionPct * 100)}
                  onChange={(e) =>
                    setForm({ ...form, reductionPct: Number(e.target.value) / 100 })
                  }
                />
                <input
                  type="number"
                  min={0}
                  max={100}
                  step={1}
                  value={Math.round(form.reductionPct * 100)}
                  onChange={(e) =>
                    setForm({ ...form, reductionPct: Number(e.target.value) / 100 })
                  }
                  style={{ width: 80 }}
                />
                <span>%</span>
              </div>
              <span className="muted field-hint">
                {t.controls.reductionHint}
              </span>
            </label>

            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={form.implemented}
                onChange={(e) => setForm({ ...form, implemented: e.target.checked })}
              />
              {t.controls.implemented}
            </label>

            <div className="button-row">
              <button type="submit" disabled={saving}>
                {saving ? t.action.saving : t.action.save}
              </button>
              <button type="button" className="link-button" onClick={() => setShowForm(false)}>
                {t.action.cancel}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {deleting && (
        <ConfirmDialog
          message={t.controls.deleteConfirm(deleting.code)}
          onConfirm={() => void confirmDelete()}
          onCancel={() => setDeleting(null)}
        />
      )}
    </div>
  );
}
