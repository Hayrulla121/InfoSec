import { useCallback, useEffect, useState } from 'react';
import { infoSystemsApi, type InfoSystem, type InfoSystemRequest } from '../api/infoSystems';
import type { Page } from '../api/registries';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ConfirmDialog, DataTable, Modal, type Column } from '../components/DataTable';
import { IconPlus, IconSearch } from '../components/Icons';
import { useI18n } from '../i18n/I18nContext';

const EMPTY: InfoSystemRequest = {
  name: '',
  description: '',
  hosting: '',
  usagePurpose: '',
  dataFormat: '',
  confidentiality: '',
  integrity: '',
  availability: '',
  updateFrequency: '',
  usersInfo: '',
  owner: '',
};

/** Values the workbook uses in these columns. */
const INTEGRITY_LEVELS = ['В', 'Н', 'Б'];

/**
 * Перечень инфосистем Банка — the detailed system inventory.
 *
 * This registry had an API and a permission module from Phase 3 but no screen,
 * which meant records existed that nobody could see or delete. That is how the
 * demo seed ended up permanently blocked by three invisible rows.
 */
export default function InfoSystemsPage() {
  const { can } = useAuth();
  const { t } = useI18n();

  const [page, setPage] = useState<Page<InfoSystem> | null>(null);
  const [pageNo, setPageNo] = useState(0);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [editing, setEditing] = useState<InfoSystem | null>(null);
  const [form, setForm] = useState<InfoSystemRequest>(EMPTY);
  const [showForm, setShowForm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState<InfoSystem | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setPage(await infoSystemsApi.list({ page: pageNo, search }));
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

  function openCreate() {
    setEditing(null);
    setForm(EMPTY);
    setShowForm(true);
  }

  function openEdit(s: InfoSystem) {
    setEditing(s);
    setForm({
      name: s.name,
      description: s.description ?? '',
      hosting: s.hosting ?? '',
      usagePurpose: s.usagePurpose ?? '',
      dataFormat: s.dataFormat ?? '',
      confidentiality: s.confidentiality ?? '',
      integrity: s.integrity ?? '',
      availability: s.availability ?? '',
      updateFrequency: s.updateFrequency ?? '',
      usersInfo: s.usersInfo ?? '',
      owner: s.owner ?? '',
    });
    setShowForm(true);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      if (editing) {
        await infoSystemsApi.update(editing.id, form);
      } else {
        await infoSystemsApi.create(form);
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
      await infoSystemsApi.remove(deleting.id);
      setDeleting(null);
      await load();
    } catch (err) {
      // A 409 here means assets still point at this system; the message from
      // the server says so, in the user's language.
      setError(errorMessage(err));
      setDeleting(null);
    }
  }

  const columns: Column<InfoSystem>[] = [
    { key: 'code', header: t.infoSystems.colId, width: '80px', render: (s) => <strong>{s.code}</strong> },
    { key: 'name', header: t.infoSystems.colName, render: (s) => <span className="cell-text">{s.name}</span> },
    {
      key: 'desc',
      header: t.infoSystems.colDescription,
      render: (s) => <span className="cell-text muted">{s.description || t.common.none}</span>,
    },
    { key: 'fmt', header: t.infoSystems.colFormat, width: '90px', render: (s) => s.dataFormat ?? t.common.none },
    {
      key: 'conf',
      header: t.infoSystems.colConfidentiality,
      width: '110px',
      render: (s) => s.confidentiality ?? t.common.none,
    },
    { key: 'int', header: t.infoSystems.colIntegrity, width: '100px', render: (s) => s.integrity ?? t.common.none },
    { key: 'avail', header: t.infoSystems.colAvailability, width: '110px', render: (s) => s.availability ?? t.common.none },
    { key: 'owner', header: t.infoSystems.colOwner, render: (s) => <span className="cell-text">{s.owner ?? t.common.none}</span> },
  ];

  return (
    <div>
      <div className="page-header">
        <div className="page-heading">
          <h1>{t.infoSystems.title}</h1>
          <p className="muted">{t.infoSystems.subtitle}</p>
        </div>
        {can('INFO_SYSTEMS', 'CREATE') && (
          <button onClick={openCreate}>
            <IconPlus size={16} />
            {t.infoSystems.add}
          </button>
        )}
      </div>

      {error && <p className="form-error">{error}</p>}

      <section className="panel">
        <div className="toolbar">
          <div className="search-field">
            <IconSearch size={16} />
            <input
              placeholder={t.infoSystems.search}
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
          rowActions={(s) => (
            <>
              {can('INFO_SYSTEMS', 'UPDATE') && (
                <button className="link-button" onClick={() => openEdit(s)}>
                  {t.action.edit}
                </button>
              )}
              {can('INFO_SYSTEMS', 'DELETE') && (
                <button className="link-button danger-link" onClick={() => setDeleting(s)}>
                  {t.action.delete}
                </button>
              )}
            </>
          )}
        />
      </section>

      {showForm && (
        <Modal
          title={editing ? t.infoSystems.editTitle(editing.code) : t.infoSystems.newTitle}
          onClose={() => setShowForm(false)}
        >
          <form onSubmit={submit} className="form-grid">
            <label>
              {t.infoSystems.fieldName}
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </label>

            <label>
              {t.infoSystems.fieldDescription}
              <textarea
                rows={2}
                value={form.description ?? ''}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </label>

            <label>
              {t.infoSystems.fieldHosting}
              <textarea
                rows={2}
                value={form.hosting ?? ''}
                onChange={(e) => setForm({ ...form, hosting: e.target.value })}
              />
            </label>

            <label>
              {t.infoSystems.fieldUsage}
              <input
                value={form.usagePurpose ?? ''}
                onChange={(e) => setForm({ ...form, usagePurpose: e.target.value })}
              />
            </label>

            <label>
              {t.infoSystems.fieldFormat}
              <input
                value={form.dataFormat ?? ''}
                onChange={(e) => setForm({ ...form, dataFormat: e.target.value })}
              />
            </label>

            <label>
              {t.infoSystems.fieldConfidentiality}
              <input
                value={form.confidentiality ?? ''}
                onChange={(e) => setForm({ ...form, confidentiality: e.target.value })}
              />
            </label>

            <label>
              {t.infoSystems.fieldIntegrity}
              <select
                value={form.integrity ?? ''}
                onChange={(e) => setForm({ ...form, integrity: e.target.value })}
              >
                <option value="">{t.common.none}</option>
                {INTEGRITY_LEVELS.map((v) => (
                  <option key={v} value={v}>
                    {v}
                  </option>
                ))}
              </select>
            </label>

            <label>
              {t.infoSystems.fieldAvailability}
              <select
                value={form.availability ?? ''}
                onChange={(e) => setForm({ ...form, availability: e.target.value })}
              >
                <option value="">{t.common.none}</option>
                {INTEGRITY_LEVELS.map((v) => (
                  <option key={v} value={v}>
                    {v}
                  </option>
                ))}
              </select>
            </label>

            <label>
              {t.infoSystems.fieldUpdateFrequency}
              <input
                value={form.updateFrequency ?? ''}
                onChange={(e) => setForm({ ...form, updateFrequency: e.target.value })}
              />
            </label>

            <label>
              {t.infoSystems.fieldUsers}
              <textarea
                rows={2}
                value={form.usersInfo ?? ''}
                onChange={(e) => setForm({ ...form, usersInfo: e.target.value })}
              />
            </label>

            <label>
              {t.infoSystems.fieldOwner}
              <input
                value={form.owner ?? ''}
                onChange={(e) => setForm({ ...form, owner: e.target.value })}
              />
            </label>

            <div className="button-row">
              <button type="submit" disabled={saving}>
                {saving ? t.action.saving : t.action.save}
              </button>
              <button type="button" className="secondary-button" onClick={() => setShowForm(false)}>
                {t.action.cancel}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {deleting && (
        <ConfirmDialog
          message={t.infoSystems.deleteConfirm(deleting.code, deleting.name)}
          onConfirm={() => void confirmDelete()}
          onCancel={() => setDeleting(null)}
        />
      )}
    </div>
  );
}
