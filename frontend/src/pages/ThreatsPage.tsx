import { useCallback, useEffect, useState } from 'react';
import { threatsApi, type Page, type Threat, type ThreatRequest } from '../api/registries';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ConfirmDialog, DataTable, LevelBadge, Modal, type Column } from '../components/DataTable';
import { IconPlus, IconSearch } from '../components/Icons';
import { useI18n } from '../i18n/I18nContext';

const EMPTY: ThreatRequest = {
  description: '',
  discoverability: 0,
  repeatability: 0,
  exploitability: 0,
  affectedUsers: 0,
  damage: 0,
};

/** The five DREAD criteria, labelled as in the workbook. */
const DREAD_KEYS: { key: keyof ThreatRequest; en: string }[] = [
  { key: 'discoverability', en: 'Discoverability' },
  { key: 'repeatability', en: 'Repeatability' },
  { key: 'exploitability', en: 'Exploitability' },
  { key: 'affectedUsers', en: 'Affected users' },
  { key: 'damage', en: 'Damage' },
];

export default function ThreatsPage() {
  const { can } = useAuth();
  const { t, level } = useI18n();

  /** DREAD criterion label in the active language, keyed by field name. */
  const dreadLabel = (key: keyof ThreatRequest) =>
    ({
      discoverability: t.threats.dreadDiscoverability,
      repeatability: t.threats.dreadRepeatability,
      exploitability: t.threats.dreadExploitability,
      affectedUsers: t.threats.dreadAffectedUsers,
      damage: t.threats.dreadDamage,
    })[key as string] ?? String(key);
  const [page, setPage] = useState<Page<Threat> | null>(null);
  const [pageNo, setPageNo] = useState(0);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [editing, setEditing] = useState<Threat | null>(null);
  const [form, setForm] = useState<ThreatRequest>(EMPTY);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState<Threat | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setPage(await threatsApi.list({ page: pageNo, search }));
      setError(null);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [pageNo, search]);

  useEffect(() => {
    void load();
  }, [load]);

  /**
   * Live preview of what the server will compute. Deliberately duplicates the
   * backend thresholds for instant feedback while typing - the value shown
   * after saving always comes from the server, which stays authoritative.
   */
  const previewTotal =
    form.discoverability + form.repeatability + form.exploitability + form.affectedUsers + form.damage;
  const previewRating =
    previewTotal < 6 ? 1 : previewTotal < 11 ? 2 : previewTotal < 16 ? 3 : previewTotal < 21 ? 4 : 5;
  const previewLabel = level(
    ['Незначительный', 'Низкий', 'Средний', 'Высокий', 'Очень высокий'][previewRating - 1],
  );

  function openCreate() {
    setEditing(null);
    setForm(EMPTY);
    setShowForm(true);
  }

  function openEdit(threat: Threat) {
    setEditing(threat);
    setForm({
      description: threat.description,
      discoverability: threat.discoverability,
      repeatability: threat.repeatability,
      exploitability: threat.exploitability,
      affectedUsers: threat.affectedUsers,
      damage: threat.damage,
    });
    setShowForm(true);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (editing) {
        await threatsApi.update(editing.id, form);
      } else {
        await threatsApi.create(form);
      }
      setShowForm(false);
      await load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await threatsApi.remove(deleting.id);
      setDeleting(null);
      await load();
    } catch (err) {
      setError(errorMessage(err));
      setDeleting(null);
    }
  }

  const columns: Column<Threat>[] = [
    { key: 'code', header: t.threats.colCode, width: '70px', render: (row) => <strong>{row.code}</strong> },
    { key: 'description', header: t.threats.colDescription, render: (row) => <span className="cell-text">{row.description}</span> },
    { key: 'd', header: t.threats.dreadDiscoverability.charAt(0), width: '48px', render: (row) => row.discoverability },
    { key: 'r', header: t.threats.dreadRepeatability.charAt(0), width: '48px', render: (row) => row.repeatability },
    { key: 'e', header: t.threats.dreadExploitability.charAt(0), width: '48px', render: (row) => row.exploitability },
    { key: 'a', header: t.threats.dreadAffectedUsers.charAt(0), width: '48px', render: (row) => row.affectedUsers },
    { key: 'dm', header: t.threats.dreadDamage.charAt(0), width: '48px', render: (row) => row.damage },
    { key: 'sum', header: t.threats.colSum, width: '70px', render: (row) => <strong>{row.totalScore}</strong> },
    {
      key: 'level',
      header: t.threats.colLevel,
      width: '170px',
      render: (row) => (
        <>
          <LevelBadge level={row.rating} /> {level(row.levelLabel)}
        </>
      ),
    },
  ];

  return (
    <div>
      <div className="page-header">
        <div className="page-heading">
          <h1>{t.threats.title}</h1>
          <p className="muted">
            {t.threats.subtitle}
          </p>
        </div>
        {can('THREATS', 'CREATE') && (
          <button onClick={openCreate}>
            <IconPlus size={16} />
            {t.threats.add}
          </button>
        )}
      </div>

      {error && <p className="form-error">{error}</p>}

      <section className="panel">
        <div className="toolbar">
          <div className="search-field">
            <IconSearch size={16} />
            <input
            placeholder={t.threats.search}
            value={search}
            onChange={(e) => {
              setPageNo(0);
              setSearch(e.target.value);
            }}
          />
          </div>
        </div>

        <DataTable
          page={page}
          columns={columns}
          loading={loading}
          onPageChange={setPageNo}
          rowActions={(row) => (
            <>
              {can('THREATS', 'UPDATE') && (
                <button className="link-button" onClick={() => openEdit(row)}>
                  {t.action.edit}
                </button>
              )}
              {can('THREATS', 'DELETE') && (
                <button className="link-button danger-link" onClick={() => setDeleting(row)}>
                  {t.action.delete}
                </button>
              )}
            </>
          )}
        />
      </section>

      {showForm && (
        <Modal
          title={editing ? t.threats.editTitle(editing.code) : t.threats.newTitle}
          onClose={() => setShowForm(false)}
        >
          <form onSubmit={submit} className="form-grid">
            <label>
              {t.threats.fieldDescription}
              <textarea
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                required
                rows={3}
              />
            </label>

            <fieldset className="dread-fieldset">
              <legend>{t.threats.dreadLegend}</legend>
              {DREAD_KEYS.map((f) => (
                <label key={f.key} className="dread-row">
                  <span>
                    {dreadLabel(f.key)} <span className="muted">({f.en})</span>
                  </span>
                  <input
                    type="number"
                    min={0}
                    max={5}
                    required
                    value={form[f.key] as number}
                    onChange={(e) =>
                      setForm({ ...form, [f.key]: Number(e.target.value) })
                    }
                  />
                </label>
              ))}
            </fieldset>

            <div className="computed-preview">
              {t.threats.previewSum}: <strong>{previewTotal}</strong> · {t.threats.previewLevel}:{' '}
              <LevelBadge level={previewRating} /> {previewLabel}
              <div className="muted" style={{ fontSize: '0.8rem' }}>
                {t.threats.previewHint}
              </div>
            </div>

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
          message={t.threats.deleteConfirm(deleting.code)}
          onConfirm={() => void confirmDelete()}
          onCancel={() => setDeleting(null)}
        />
      )}
    </div>
  );
}
