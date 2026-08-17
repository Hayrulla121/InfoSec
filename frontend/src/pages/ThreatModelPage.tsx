import {
  ASSESSMENT_PRINCIPLES,
  DREAD_CRITERIA,
  DREAD_HEADERS,
  DREAD_SCORING_NOTE,
  DREAD_TITLE,
  PRINCIPLES_TITLE,
  THREAT_LEVEL_HEADERS,
  THREAT_LEVEL_TABLE,
} from '../content/threatModel';
import { useI18n } from '../i18n/I18nContext';
import { LevelBadge } from '../components/DataTable';

/**
 * Read-only reference page. No API call: the content is static by design.
 *
 * The Uzbek text is the workbook's original; the Russian is a translation of
 * it. Both live in the content module, and this component just picks a side.
 */
export default function ThreatModelPage() {
  const { lang, t } = useI18n();
  const uz = lang === 'uz';
  const h = DREAD_HEADERS[lang];
  const lh = THREAT_LEVEL_HEADERS[lang];

  return (
    <div>
      <h1>{t.nav.threatModel}</h1>

      <section className="panel">
        <h2>{DREAD_TITLE[lang]}</h2>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th style={{ width: '14%' }}>{h.criterion}</th>
                <th style={{ width: '17%' }}>{h.name}</th>
                <th>{h.note}</th>
                <th style={{ width: '9%' }}>{h.s0}</th>
                <th style={{ width: '9%' }}>{h.s5}</th>
                <th style={{ width: '22%' }}>{h.tips}</th>
              </tr>
            </thead>
            <tbody>
              {DREAD_CRITERIA.map((c) => (
                <tr key={c.code}>
                  <td>
                    <strong>{c.code}</strong>
                  </td>
                  <td>{uz ? c.nameUz : c.nameRu}</td>
                  <td className="cell-text">{uz ? c.descriptionUz : c.descriptionRu}</td>
                  <td>{uz ? c.score0Uz : c.score0Ru}</td>
                  <td>{uz ? c.score5Uz : c.score5Ru}</td>
                  <td className="cell-text muted">
                    {uz ? c.recommendationUz : c.recommendationRu}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <h2>{lh.level}</h2>
        <p className="cell-text">{DREAD_SCORING_NOTE[lang]}</p>
        <div className="table-scroll">
          <table className="data-table" style={{ maxWidth: 560 }}>
            <thead>
              <tr>
                <th>{lh.sum}</th>
                <th>{lh.level}</th>
                <th>{lh.rating}</th>
              </tr>
            </thead>
            <tbody>
              {THREAT_LEVEL_TABLE.map((row) => (
                <tr key={row.rating}>
                  <td>
                    <code>{row.range}</code>
                  </td>
                  <td>{uz ? row.levelUz : row.levelRu}</td>
                  <td>
                    <LevelBadge level={row.rating} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <h2>{PRINCIPLES_TITLE[lang]}</h2>
        <ol className="principles">
          {ASSESSMENT_PRINCIPLES[lang].map((p, i) => (
            <li key={i}>{p}</li>
          ))}
        </ol>
      </section>
    </div>
  );
}
