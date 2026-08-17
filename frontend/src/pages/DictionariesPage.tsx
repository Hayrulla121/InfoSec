import { useEffect, useState } from 'react';
import * as dictApi from '../api/dictionaries';
import type { DictionaryGroup, DictionaryItem } from '../api/dictionaries';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { useI18n } from '../i18n/I18nContext';

/**
 * Техническая страница — the editable dropdown sources.
 *
 * Each dictionary is edited and saved independently, matching the backend's
 * per-dict_type PUT: a stale tab cannot wipe a list it was not showing.
 */
export default function DictionariesPage() {
  const { can } = useAuth();
  const { t } = useI18n();
  const editable = can('DICTIONARIES', 'UPDATE');

  const [groups, setGroups] = useState<DictionaryGroup[]>([]);
  const [drafts, setDrafts] = useState<Record<string, DictionaryItem[]>>({});
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [savingType, setSavingType] = useState<string | null>(null);

  useEffect(() => {
    dictApi
      .getDictionaries()
      .then((data) => {
        setGroups(data);
        setDrafts(Object.fromEntries(data.map((g) => [g.dictType, g.items])));
      })
      .catch((e) => setError(errorMessage(e)));
  }, []);

  function patchItem(type: string, index: number, patch: Partial<DictionaryItem>) {
    setDrafts((prev) => ({
      ...prev,
      [type]: prev[type].map((item, i) => (i === index ? { ...item, ...patch } : item)),
    }));
  }

  function addItem(type: string) {
    setDrafts((prev) => ({
      ...prev,
      // id: null tells the backend "this is new, insert it".
      [type]: [...prev[type], { id: null, label: '', numericValue: null, sortOrder: 0 }],
    }));
  }

  function removeItem(type: string, index: number) {
    setDrafts((prev) => ({
      ...prev,
      [type]: prev[type].filter((_, i) => i !== index),
    }));
  }

  async function save(group: DictionaryGroup) {
    setError(null);
    setNotice(null);
    setSavingType(group.dictType);
    try {
      const saved = await dictApi.updateDictionary(group.dictType, drafts[group.dictType]);
      setGroups((prev) => prev.map((g) => (g.dictType === saved.dictType ? saved : g)));
      setDrafts((prev) => ({ ...prev, [saved.dictType]: saved.items }));
      setNotice(t.dictionaries.saved(t.dictTitle[group.dictType]));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSavingType(null);
    }
  }

  return (
    <div>
      <h1>{t.dictionaries.title}</h1>
      <p className="muted">{t.dictionaries.subtitle}</p>

      {error && <p className="form-error">{error}</p>}
      {notice && <p className="form-notice">{notice}</p>}
      {!editable && (
        <p className="muted">{t.dictionaries.readOnly}</p>
      )}

      {groups.map((group) => (
        <section className="panel" key={group.dictType}>
          <h2>
            {t.dictTitle[group.dictType]}{' '}
            <span className="muted" style={{ fontWeight: 400 }}>
              ({group.dictType})
            </span>
          </h2>

          <table className="data-table" style={{ maxWidth: 640 }}>
            <thead>
              <tr>
                <th style={{ width: 48 }}>{t.dictionaries.colIndex}</th>
                <th>{t.dictionaries.colValue}</th>
                {group.numericRequired && <th style={{ width: 110 }}>{t.dictionaries.colLevel}</th>}
                {editable && <th style={{ width: 90 }} />}
              </tr>
            </thead>
            <tbody>
              {(drafts[group.dictType] ?? []).map((item, index) => (
                // Rows without an id fall back to their position for the key.
                <tr key={item.id ?? `new-${index}`}>
                  <td className="muted">{index + 1}</td>
                  <td>
                    <input
                      value={item.label}
                      disabled={!editable}
                      onChange={(e) => patchItem(group.dictType, index, { label: e.target.value })}
                      style={{ width: '100%' }}
                    />
                  </td>
                  {group.numericRequired && (
                    <td>
                      <input
                        type="number"
                        min={1}
                        max={5}
                        disabled={!editable}
                        value={item.numericValue ?? ''}
                        onChange={(e) =>
                          patchItem(group.dictType, index, {
                            numericValue: e.target.value === '' ? null : Number(e.target.value),
                          })
                        }
                        style={{ width: 70 }}
                      />
                    </td>
                  )}
                  {editable && (
                    <td className="actions">
                      <button
                        className="link-button"
                        onClick={() => removeItem(group.dictType, index)}
                      >
                        {t.action.delete}
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>

          {editable && (
            <div className="button-row">
              <button className="link-button" onClick={() => addItem(group.dictType)}>
                {t.dictionaries.addValue}
              </button>
              <button
                onClick={() => void save(group)}
                disabled={savingType === group.dictType}
              >
                {savingType === group.dictType ? t.action.saving : t.action.save}
              </button>
            </div>
          )}
        </section>
      ))}
    </div>
  );
}
