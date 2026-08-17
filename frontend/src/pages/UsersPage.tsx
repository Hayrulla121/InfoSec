import { useCallback, useEffect, useState } from 'react';
import * as adminApi from '../api/admin';
import { errorMessage } from '../api/client';
import { APP_MODULES, type ModulePermission, type User } from '../api/types';
import { useI18n } from '../i18n/I18nContext';

const ACTIONS = [
  { key: 'canCreate', label: 'Create' },
  { key: 'canRead', label: 'Read' },
  { key: 'canUpdate', label: 'Update' },
  { key: 'canDelete', label: 'Delete' },
] as const;

const EMPTY_FORM = { username: '', password: '', fullName: '', email: '' };

export default function UsersPage() {
  const { t } = useI18n();
  const [users, setUsers] = useState<User[]>([]);
  const [selected, setSelected] = useState<User | null>(null);
  const [grid, setGrid] = useState<ModulePermission[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    try {
      setUsers(await adminApi.listUsers());
    } catch (e) {
      setError(errorMessage(e));
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function selectUser(user: User) {
    setSelected(user);
    setError(null);
    setNotice(null);
    try {
      setGrid(await adminApi.getUserPermissions(user.id));
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  async function onCreate(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setNotice(null);
    setSaving(true);
    try {
      await adminApi.createUser({
        username: form.username,
        password: form.password,
        fullName: form.fullName,
        email: form.email || undefined,
        role: 'USER',
      });
      setForm(EMPTY_FORM);
      setNotice(t.users.created);
      await load();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  async function toggleActive(user: User) {
    setError(null);
    try {
      await adminApi.updateUser(user.id, { active: !user.active });
      await load();
    } catch (e) {
      setError(errorMessage(e));
    }
  }

  /**
   * Updates one checkbox in local state only. Nothing is sent until "Сохранить",
   * so an admin can set a whole row before committing.
   *
   * Note the immutable update: we build a NEW array with .map instead of
   * mutating grid[i]. React compares by reference, so an in-place edit would
   * not trigger a re-render.
   */
  function toggleCell(module: string, key: (typeof ACTIONS)[number]['key']) {
    setGrid((prev) =>
      prev.map((row) => (row.module === module ? { ...row, [key]: !row[key] } : row)),
    );
  }

  async function savePermissions() {
    if (!selected) return;
    setError(null);
    setNotice(null);
    setSaving(true);
    try {
      setGrid(await adminApi.updateUserPermissions(selected.id, grid));
      setNotice(t.users.permissionsSaved);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div>
      <h1>{t.users.title}</h1>

      {error && <p className="form-error">{error}</p>}
      {notice && <p className="form-notice">{notice}</p>}

      <section className="panel">
        <h2>{t.users.accounts}</h2>
        <table className="data-table">
          <thead>
            <tr>
              <th>{t.users.colLogin}</th>
              <th>{t.users.colFullName}</th>
              <th>{t.users.colEmail}</th>
              <th>{t.users.colRole}</th>
              <th>{t.users.colStatus}</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id} className={selected?.id === u.id ? 'row-selected' : undefined}>
                <td>{u.username}</td>
                <td>{u.fullName}</td>
                <td>{u.email ?? t.common.none}</td>
                <td>{u.role === 'ADMIN' ? t.role.admin : t.role.user}</td>
                <td>
                  <span className={u.active ? 'badge badge-ok' : 'badge badge-off'}>
                    {u.active ? t.users.active : t.users.inactive}
                  </span>
                </td>
                <td className="actions">
                  <button className="link-button" onClick={() => void selectUser(u)}>
                    {t.users.permissions}
                  </button>
                  <button className="link-button" onClick={() => void toggleActive(u)}>
                    {u.active ? t.users.deactivate : t.users.activate}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="panel">
        <h2>{t.users.newUser}</h2>
        <form className="inline-form" onSubmit={onCreate}>
          <input
            placeholder={t.users.fieldLogin}
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            required
            minLength={3}
          />
          <input
            placeholder={t.users.fieldFullName}
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            required
          />
          <input
            placeholder={t.users.fieldEmail}
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
          <input
            placeholder={t.users.fieldPassword}
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
            minLength={6}
          />
          <button type="submit" disabled={saving}>
            {t.users.create}
          </button>
        </form>
      </section>

      {selected && (
        <section className="panel">
          <h2>
            {t.users.permissionsFor(selected.fullName, selected.username)}
          </h2>

          {selected.role === 'ADMIN' ? (
            <p className="muted">{t.users.adminNote}</p>
          ) : (
            <>
              <table className="data-table permission-grid">
                <thead>
                  <tr>
                    <th>{t.users.colModule}</th>
                    {ACTIONS.map((a) => (
                      <th key={a.key}>{a.label}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {APP_MODULES.map((module) => {
                    const row = grid.find((r) => r.module === module);
                    return (
                      <tr key={module}>
                        <td>{t.modules[module]}</td>
                        {ACTIONS.map((a) => (
                          <td key={a.key} className="checkbox-cell">
                            <input
                              type="checkbox"
                              checked={row ? row[a.key] : false}
                              onChange={() => toggleCell(module, a.key)}
                              aria-label={`${t.modules[module]} ${a.label}`}
                            />
                          </td>
                        ))}
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              <button onClick={() => void savePermissions()} disabled={saving}>
                {saving ? t.action.saving : t.action.save}
              </button>
            </>
          )}
        </section>
      )}
    </div>
  );
}
