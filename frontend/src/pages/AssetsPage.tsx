import { useCallback, useEffect, useState } from 'react';
import { assetsApi, type Asset, type AssetRequest, type Page } from '../api/registries';
import { getOptions } from '../api/dictionaries';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ConfirmDialog, DataTable, LevelBadge, Modal, type Column } from '../components/DataTable';
import { ColumnFilters, useRegistryFilters, type FilterDef } from '../components/ColumnFilters';
import { IconPlus, IconSearch } from '../components/Icons';
import { useI18n } from '../i18n/I18nContext';

const EMPTY: AssetRequest = {
  name: '',
  scope: '',
  infoCategory: '',
  criticality: '',
  securityClass: '',
};

const SECURITY_CLASSES = ['IS1', 'IS2', 'IS3', 'IS4'];

export default function AssetsPage() {
  const { can } = useAuth();
  const { t, criticality } = useI18n();
  const [page, setPage] = useState<Page<Asset> | null>(null);
  const [pageNo, setPageNo] = useState(0);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  /** Dropdown source: the ASSET_CRITICALITY dictionary, not a hardcoded list. */
  const [criticalityOptions, setCriticalityOptions] = useState<string[]>([]);

  const { filters, facets, setFilter, resetFilters, refreshFacets } = useRegistryFilters(
    assetsApi.facets,
    () => setPageNo(0),
  );

  const [editing, setEditing] = useState<Asset | null>(null);
  const [form, setForm] = useState<AssetRequest>(EMPTY);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState<Asset | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setPage(await assetsApi.list({ page: pageNo, search, filters }));
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
    getOptions('ASSET_CRITICALITY').then(setCriticalityOptions).catch(() => setCriticalityOptions([]));
  }, []);

  function openCreate() {
    setEditing(null);
    setForm({ ...EMPTY, criticality: criticalityOptions[0] ?? '' });
    setShowForm(true);
  }

  function openEdit(a: Asset) {
    setEditing(a);
    setForm({
      name: a.name,
      scope: a.scope ?? '',
      infoCategory: a.infoCategory ?? '',
      criticality: a.criticality,
      securityClass: a.securityClass ?? '',
      infoSystemId: a.infoSystemId,
    });
    setShowForm(true);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (editing) {
        await assetsApi.update(editing.id, form);
      } else {
        await assetsApi.create(form);
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
      await assetsApi.remove(deleting.id);
      setDeleting(null);
      await load();
      refreshFacets();
    } catch (err) {
      setError(errorMessage(err));
      setDeleting(null);
    }
  }

  /**
   * Filterable columns. The `field` must match both the server's query
   * parameter and its facet key — they are the same name by design, so there
   * is no mapping table to keep in sync.
   *
   * `criticality` is stored in Russian (it comes from the dictionary), so it
   * gets a translator: the dropdown READS in the chosen language while still
   * SENDING the stored value. The other three are free text typed by users and
   * are shown exactly as entered.
   */
  const filterDefs: FilterDef[] = [
    { field: 'infoCategory', label: t.assets.colCategory },
    { field: 'criticality', label: t.assets.colCriticality, translate: criticality },
    { field: 'scope', label: t.assets.colScope },
    { field: 'securityClass', label: t.assets.colSecurityClass },
  ];

  const columns: Column<Asset>[] = [
    { key: 'code', header: t.assets.colId, width: '80px', render: (a) => <strong>{a.code}</strong> },
    { key: 'name', header: t.assets.colName, render: (a) => a.name },
    { key: 'scope', header: t.assets.colScope, render: (a) => a.scope ?? t.common.none },
    { key: 'cat', header: t.assets.colCategory, render: (a) => a.infoCategory ?? t.common.none },
    { key: 'crit', header: t.assets.colCriticality, width: '150px', render: (a) => criticality(a.criticality) },
    {
      key: 'rating',
      header: t.assets.colRating,
      width: '90px',
      render: (a) => <LevelBadge level={a.criticalityRating} title={a.criticality} />,
    },
    { key: 'class', header: t.assets.colSecurityClass, width: '100px', render: (a) => a.securityClass ?? t.common.none },
  ];

  return (
    <div>
      <div className="page-header">
        <div className="page-heading">
          <h1>{t.assets.title}</h1>
          <p className="muted">
            {t.assets.subtitle}
          </p>
        </div>
        {can('ASSETS', 'CREATE') && (
          <button onClick={openCreate}>
            <IconPlus size={16} />
            {t.assets.add}
          </button>
        )}
      </div>

      {error && <p className="form-error">{error}</p>}

      <section className="panel">
        <div className="toolbar">
          <div className="search-field">
            <IconSearch size={16} />
            <input
            placeholder={t.assets.search}
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
          rowActions={(a) => (
            <>
              {can('ASSETS', 'UPDATE') && (
                <button className="link-button" onClick={() => openEdit(a)}>
                  {t.action.edit}
                </button>
              )}
              {can('ASSETS', 'DELETE') && (
                <button className="link-button danger-link" onClick={() => setDeleting(a)}>
                  {t.action.delete}
                </button>
              )}
            </>
          )}
        />
      </section>

      {showForm && (
        <Modal
          title={editing ? t.assets.editTitle(editing.code) : t.assets.newTitle}
          onClose={() => setShowForm(false)}
        >
          <form onSubmit={submit} className="form-grid">
            <label>
              {t.assets.fieldName}
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </label>

            <label>
              {t.assets.fieldScope}
              <input
                value={form.scope ?? ''}
                onChange={(e) => setForm({ ...form, scope: e.target.value })}
              />
            </label>

            <label>
              {t.assets.fieldCategory}
              <input
                value={form.infoCategory ?? ''}
                onChange={(e) => setForm({ ...form, infoCategory: e.target.value })}
              />
            </label>

            <label>
              {t.assets.fieldCriticality}
              <select
                value={form.criticality}
                onChange={(e) => setForm({ ...form, criticality: e.target.value })}
                required
              >
                <option value="" disabled>
                  {t.common.select}
                </option>
                {criticalityOptions.map((o) => (
                  <option key={o} value={o}>
                    {criticality(o)}
                  </option>
                ))}
              </select>
              <span className="muted field-hint">
                {t.assets.criticalityHint}
              </span>
            </label>

            <label>
              {t.assets.fieldSecurityClass}
              <select
                value={form.securityClass ?? ''}
                onChange={(e) => setForm({ ...form, securityClass: e.target.value })}
              >
                <option value="">{t.common.none}</option>
                {SECURITY_CLASSES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
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
          message={t.assets.deleteConfirm(deleting.code, deleting.name)}
          onConfirm={() => void confirmDelete()}
          onCancel={() => setDeleting(null)}
        />
      )}
    </div>
  );
}
