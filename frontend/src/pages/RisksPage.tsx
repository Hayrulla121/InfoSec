import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { risksApi, type Risk, type RiskRequest } from '../api/risks';
import { assetsApi, threatsApi, type Asset, type Page, type Threat } from '../api/registries';
import { getOptions } from '../api/dictionaries';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ConfirmDialog, DataTable, LevelBadge, Modal, type Column } from '../components/DataTable';
import { ColumnFilters, useRegistryFilters, type FilterDef } from '../components/ColumnFilters';
import { IconPlus, IconSearch } from '../components/Icons';
import { useI18n } from '../i18n/I18nContext';
import RiskDetailDrawer from './RiskDetailDrawer';

const EMPTY: RiskRequest = {
  assetId: 0,
  threatId: 0,
  name: '',
  indicators: '',
  owner: '',
  treatmentMethod: '',
  measureStatus: '',
  implementationDeadline: null,
  comment: '',
};

export default function RisksPage() {
  const { can } = useAuth();
  const { t, level, method, status } = useI18n();

  /**
   * Matrix drill-down: /risks?assetRating=5&threatRating=3 arrives from a
   * clicked cell. Keeping the filter in the URL means the view is shareable
   * and survives a refresh.
   */
  const [params, setParams] = useSearchParams();
  const assetRating = params.get('assetRating') ? Number(params.get('assetRating')) : undefined;
  const threatRating = params.get('threatRating') ? Number(params.get('threatRating')) : undefined;

  const [page, setPage] = useState<Page<Risk> | null>(null);
  const [pageNo, setPageNo] = useState(0);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [assets, setAssets] = useState<Asset[]>([]);
  const [threats, setThreats] = useState<Threat[]>([]);
  const [methods, setMethods] = useState<string[]>([]);
  const [statuses, setStatuses] = useState<string[]>([]);

  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<Risk | null>(null);
  const [form, setForm] = useState<RiskRequest>(EMPTY);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState<Risk | null>(null);
  const [detail, setDetail] = useState<Risk | null>(null);

  const { filters, facets, setFilter, resetFilters, refreshFacets } = useRegistryFilters(
    risksApi.facets,
    () => setPageNo(0),
  );

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setPage(await risksApi.list({ page: pageNo, search, assetRating, threatRating, filters }));
      setError(null);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [pageNo, search, assetRating, threatRating, filters]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    assetsApi.list({ size: 500 }).then((p) => setAssets(p.content)).catch(() => setAssets([]));
    threatsApi.list({ size: 500 }).then((p) => setThreats(p.content)).catch(() => setThreats([]));
    getOptions('TREATMENT_METHOD').then(setMethods).catch(() => setMethods([]));
    getOptions('MEASURE_STATUS').then(setStatuses).catch(() => setStatuses([]));
  }, []);

  function openCreate() {
    setEditing(null);
    setForm({
      ...EMPTY,
      assetId: assets[0]?.id ?? 0,
      threatId: threats[0]?.id ?? 0,
      treatmentMethod: methods[0] ?? '',
    });
    setShowForm(true);
  }

  function openEdit(r: Risk) {
    setEditing(r);
    setForm({
      assetId: r.assetId,
      threatId: r.threatId,
      name: r.name,
      indicators: r.indicators ?? '',
      owner: r.owner ?? '',
      treatmentMethod: r.treatmentMethod ?? '',
      measureStatus: r.measureStatus ?? '',
      implementationDeadline: r.implementationDeadline,
      comment: r.comment ?? '',
    });
    setShowForm(true);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const body = {
        ...form,
        implementationDeadline: form.implementationDeadline || null,
      };
      if (editing) {
        await risksApi.update(editing.id, body);
      } else {
        await risksApi.create(body);
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
      await risksApi.remove(deleting.id);
      setDeleting(null);
      await load();
      refreshFacets();
    } catch (err) {
      setError(errorMessage(err));
      setDeleting(null);
    }
  }

  /** Keeps the table row in sync after the drawer attaches/detaches a control. */
  function onRiskChanged(updated: Risk) {
    setPage((prev) =>
      prev
        ? { ...prev, content: prev.content.map((r) => (r.id === updated.id ? updated : r)) }
        : prev,
    );
  }

  const filterDefs: FilterDef[] = [
    { field: 'currentRiskLabel', label: t.risks.colCurrent, translate: level },
    { field: 'residualRiskLabel', label: t.risks.colResidual, translate: level },
    { field: 'treatmentMethod', label: t.risks.fieldMethod, translate: method },
    { field: 'measureStatus', label: t.risks.colStatus, translate: status },
  ];

  const columns: Column<Risk>[] = [
    { key: 'code', header: t.risks.colId, width: '70px', render: (r) => <strong>{r.code}</strong> },
    {
      key: 'asset',
      header: t.risks.colAsset,
      width: '160px',
      render: (r) => (
        <>
          <div>{r.assetCode}</div>
          <div className="muted cell-small">{r.assetName}</div>
        </>
      ),
    },
    {
      key: 'threat',
      header: t.risks.colThreat,
      width: '160px',
      render: (r) => (
        <>
          <div>{r.threatCode}</div>
          <div className="muted cell-small">{r.threatDescription}</div>
        </>
      ),
    },
    { key: 'name', header: t.risks.colName, render: (r) => <span className="cell-text">{r.name}</span> },
    {
      key: 'controls',
      header: t.risks.colControls,
      render: (r) =>
        r.implementedControls.length === 0 ? (
          <span className="muted">{t.common.none}</span>
        ) : (
          // Replaces Excel's TEXTJOIN column H.
          <div className="chips">
            {r.implementedControls.map((c) => (
              <span className="chip" key={c.linkId} title={c.controlName}>
                {c.controlCode} −{Math.round(Number(c.reductionPct) * 100)}%
              </span>
            ))}
          </div>
        ),
    },
    {
      key: 'current',
      header: t.risks.colCurrent,
      width: '150px',
      render: (r) =>
        r.current.riskLevel ? (
          <>
            <LevelBadge level={r.current.riskLevel} /> {level(r.current.riskLabel)}
          </>
        ) : (
          '—'
        ),
    },
    {
      key: 'residual',
      header: t.risks.colResidual,
      width: '150px',
      render: (r) =>
        r.residual.riskLevel ? (
          <>
            <LevelBadge level={r.residual.riskLevel} /> {level(r.residual.riskLabel)}
          </>
        ) : (
          '—'
        ),
    },
    { key: 'status', header: t.risks.colStatus, width: '140px', render: (r) => status(r.measureStatus) },
  ];

  const filtered = assetRating != null || threatRating != null;

  return (
    <div>
      <div className="page-header">
        <div className="page-heading">
          <h1>{t.risks.title}</h1>
          <p className="muted">
            {t.risks.subtitle}
          </p>
        </div>
        {can('RISKS', 'CREATE') && (
          <button onClick={openCreate}>
            <IconPlus size={16} />
            {t.risks.add}
          </button>
        )}
      </div>

      {error && <p className="form-error">{error}</p>}

      {filtered && (
        <p className="form-notice">
          {t.risks.matrixFilter(assetRating ?? t.common.none, threatRating ?? t.common.none)}{' '}
          <button
            className="link-button"
            onClick={() => {
              setParams({});
              setPageNo(0);
            }}
          >
            {t.action.reset}
          </button>
        </p>
      )}

      <section className="panel">
        <div className="toolbar">
          <div className="search-field">
            <IconSearch size={16} />
            <input
            placeholder={t.risks.search}
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
          emptyMessage={filtered ? t.risks.emptyCell : t.table.empty}
          rowActions={(r) => (
            <>
              <button className="link-button" onClick={() => setDetail(r)}>
                {t.risks.controlsButton}
              </button>
              {can('RISKS', 'UPDATE') && (
                <button className="link-button" onClick={() => openEdit(r)}>
                  {t.action.edit}
                </button>
              )}
              {can('RISKS', 'DELETE') && (
                <button className="link-button danger-link" onClick={() => setDeleting(r)}>
                  {t.action.delete}
                </button>
              )}
            </>
          )}
        />
      </section>

      {detail && (
        <RiskDetailDrawer
          risk={detail}
          onClose={() => setDetail(null)}
          onChanged={(updated) => {
            setDetail(updated);
            onRiskChanged(updated);
          }}
        />
      )}

      {showForm && (
        <Modal
          title={editing ? t.risks.editTitle(editing.code) : t.risks.newTitle}
          onClose={() => setShowForm(false)}
        >
          <form onSubmit={submit} className="form-grid">
            <label>
              {t.risks.fieldAsset}
              <select
                value={form.assetId || ''}
                onChange={(e) => setForm({ ...form, assetId: Number(e.target.value) })}
                required
                disabled={!!editing}
              >
                <option value="" disabled>
                  {t.common.select}
                </option>
                {assets.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.code} · {a.name} ({t.risks.assetRating(a.criticalityRating)})
                  </option>
                ))}
              </select>
            </label>

            <label>
              {t.risks.fieldThreat}
              <select
                value={form.threatId || ''}
                onChange={(e) => setForm({ ...form, threatId: Number(e.target.value) })}
                required
                disabled={!!editing}
              >
                <option value="" disabled>
                  {t.common.select}
                </option>
                {threats.map((th) => (
                  <option key={th.id} value={th.id}>
                    {th.code} · {th.description} ({t.risks.threatScore(th.totalScore)})
                  </option>
                ))}
              </select>
              {editing && (
                <span className="muted field-hint">{t.risks.pairLocked}</span>
              )}
            </label>

            <label>
              {t.risks.fieldName}
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </label>

            <label>
              {t.risks.fieldIndicators}
              <textarea
                rows={2}
                value={form.indicators ?? ''}
                onChange={(e) => setForm({ ...form, indicators: e.target.value })}
              />
            </label>

            <label>
              {t.risks.fieldOwner}
              <input
                value={form.owner ?? ''}
                onChange={(e) => setForm({ ...form, owner: e.target.value })}
              />
            </label>

            <label>
              {t.risks.fieldMethod}
              <select
                value={form.treatmentMethod ?? ''}
                onChange={(e) => setForm({ ...form, treatmentMethod: e.target.value })}
              >
                <option value="">{t.common.none}</option>
                {methods.map((m) => (
                  <option key={m} value={m}>
                    {method(m)}
                  </option>
                ))}
              </select>
            </label>

            <label>
              {t.risks.fieldStatus}
              <select
                value={form.measureStatus ?? ''}
                onChange={(e) => setForm({ ...form, measureStatus: e.target.value })}
              >
                <option value="">{t.common.none}</option>
                {statuses.map((s) => (
                  <option key={s} value={s}>
                    {status(s)}
                  </option>
                ))}
              </select>
            </label>

            <label>
              {t.risks.fieldDeadline}
              <input
                type="date"
                value={form.implementationDeadline ?? ''}
                onChange={(e) => setForm({ ...form, implementationDeadline: e.target.value })}
              />
            </label>

            <label>
              {t.risks.fieldComment}
              <textarea
                rows={2}
                value={form.comment ?? ''}
                onChange={(e) => setForm({ ...form, comment: e.target.value })}
              />
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
          message={t.risks.deleteConfirm(deleting.code)}
          onConfirm={() => void confirmDelete()}
          onCancel={() => setDeleting(null)}
        />
      )}
    </div>
  );
}
