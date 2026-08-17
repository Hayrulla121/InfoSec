import { useEffect, useState } from 'react';
import { risksApi, type ControlType, type Risk, type RiskStage } from '../api/risks';
import { controlsApi, type Control } from '../api/registries';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { LevelBadge } from '../components/DataTable';
import { useI18n } from '../i18n/I18nContext';

/** One stage of the inherent -> current -> residual progression. */
function StageCard({
  title,
  stage,
  note,
  levelLabel,
  scoreText,
  ratingText,
  dash,
}: {
  title: string;
  stage: RiskStage;
  note: string;
  levelLabel: string;
  scoreText: string | null;
  ratingText: string;
  dash: string;
}) {
  return (
    <div className="stage-card">
      <div className="stage-title">{title}</div>
      <div className="stage-body">
        {stage.riskLevel ? (
          <>
            <LevelBadge level={stage.riskLevel} />
            <span className="stage-label">{levelLabel}</span>
          </>
        ) : (
          <span className="muted">{dash}</span>
        )}
      </div>
      <div className="muted stage-note">
        {scoreText && <>{scoreText} · </>}
        {ratingText}
      </div>
      <div className="muted stage-note">{note}</div>
    </div>
  );
}

/**
 * Slide-over panel for one risk: the three computed stages plus the two control
 * tabs. Every attach/detach returns the recalculated risk, so the numbers above
 * move the instant a control changes - the live equivalent of watching Excel
 * recalculate.
 */
export default function RiskDetailDrawer({
  risk: initial,
  onClose,
  onChanged,
}: {
  risk: Risk;
  onClose: () => void;
  onChanged: (risk: Risk) => void;
}) {
  const { can } = useAuth();
  const { t, level } = useI18n();
  const mayAttach = can('RISK_CONTROLS', 'CREATE');
  const mayDetach = can('RISK_CONTROLS', 'DELETE');

  const [risk, setRisk] = useState<Risk>(initial);
  const [tab, setTab] = useState<ControlType>('IMPLEMENTED');
  const [catalog, setCatalog] = useState<Control[]>([]);
  const [selected, setSelected] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    // size 500: the whole catalog, so the picker needs no paging of its own.
    controlsApi
      .list({ size: 500 })
      .then((p) => setCatalog(p.content))
      .catch(() => setCatalog([]));
  }, []);

  function applyUpdate(updated: Risk) {
    setRisk(updated);
    onChanged(updated);
  }

  async function attach() {
    if (!selected) return;
    setBusy(true);
    setError(null);
    try {
      applyUpdate(await risksApi.attachControl(risk.id, Number(selected), tab));
      setSelected('');
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  async function detach(linkId: number) {
    setBusy(true);
    setError(null);
    try {
      applyUpdate(await risksApi.detachControl(risk.id, linkId));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  const attached = tab === 'IMPLEMENTED' ? risk.implementedControls : risk.plannedControls;
  const attachedIds = new Set([
    ...risk.implementedControls.map((c) => c.controlId),
    ...risk.plannedControls.map((c) => c.controlId),
  ]);
  // A control can only be attached to a risk once, in one of the two roles.
  const available = catalog.filter((c) => !attachedIds.has(c.id));

  return (
    <div className="drawer-backdrop" onClick={onClose}>
      <aside className="drawer" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>
            {risk.code} · {risk.name}
          </h2>
          <button className="link-button" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>

        <div className="drawer-meta">
          <div>
            <span className="muted">{t.drawer.asset}: </span>
            {risk.assetCode} · {risk.assetName} <LevelBadge level={risk.assetRating} />
          </div>
          <div>
            <span className="muted">{t.drawer.threat}: </span>
            {risk.threatCode} · {risk.threatDescription}{' '}
            <span className="muted">({t.drawer.score(risk.threatTotalScore)})</span>
          </div>
          {risk.owner && (
            <div>
              <span className="muted">{t.drawer.owner}: </span>
              {risk.owner}
            </div>
          )}
        </div>

        <div className="stages">
          <StageCard
            title={t.drawer.inherent}
            stage={risk.inherent}
            note={t.drawer.inherentNote}
            levelLabel={level(risk.inherent.riskLabel)}
            scoreText={null}
            ratingText={t.drawer.threatRating(risk.inherent.threatRating ?? t.common.none)}
            dash={t.common.none}
          />
          <span className="stage-arrow">→</span>
          <StageCard
            title={t.drawer.current}
            stage={risk.current}
            note={t.drawer.currentNote}
            levelLabel={level(risk.current.riskLabel)}
            scoreText={risk.current.score != null ? t.drawer.score(Number(risk.current.score)) : null}
            ratingText={t.drawer.threatRating(risk.current.threatRating ?? t.common.none)}
            dash={t.common.none}
          />
          <span className="stage-arrow">→</span>
          <StageCard
            title={t.drawer.residual}
            stage={risk.residual}
            note={t.drawer.residualNote}
            levelLabel={level(risk.residual.riskLabel)}
            scoreText={risk.residual.score != null ? t.drawer.score(Number(risk.residual.score)) : null}
            ratingText={t.drawer.threatRating(risk.residual.threatRating ?? t.common.none)}
            dash={t.common.none}
          />
        </div>

        {error && <p className="form-error">{error}</p>}

        <div className="tabs">
          <button
            className={tab === 'IMPLEMENTED' ? 'tab tab-active' : 'tab'}
            onClick={() => setTab('IMPLEMENTED')}
          >
            {t.drawer.tabImplemented(risk.implementedControls.length)}
          </button>
          <button
            className={tab === 'PLANNED' ? 'tab tab-active' : 'tab'}
            onClick={() => setTab('PLANNED')}
          >
            {t.drawer.tabPlanned(risk.plannedControls.length)}
          </button>
        </div>

        {attached.length === 0 ? (
          <p className="muted">{t.drawer.noControls}</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th style={{ width: 60 }}>{t.drawer.colId}</th>
                <th>{t.drawer.colName}</th>
                <th style={{ width: 90 }}>{t.drawer.colReduction}</th>
                {mayDetach && <th style={{ width: 80 }} />}
              </tr>
            </thead>
            <tbody>
              {attached.map((c) => (
                <tr key={c.linkId}>
                  <td>
                    <strong>{c.controlCode}</strong>
                  </td>
                  <td className="cell-text">{c.controlName}</td>
                  <td>{Math.round(Number(c.reductionPct) * 100)}%</td>
                  {mayDetach && (
                    <td className="actions">
                      <button
                        className="link-button danger-link"
                        disabled={busy}
                        onClick={() => void detach(c.linkId)}
                      >
                        {t.drawer.detach}
                      </button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {mayAttach && (
          <div className="toolbar" style={{ marginTop: '1rem' }}>
            <select
              value={selected}
              onChange={(e) => setSelected(e.target.value)}
              style={{ flex: 1, minWidth: 260 }}
            >
              <option value="">{t.drawer.pickControl}</option>
              {available.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.code} · {c.name} ({Math.round(Number(c.reductionPct) * 100)}%)
                </option>
              ))}
            </select>
            <button onClick={() => void attach()} disabled={!selected || busy}>
              {t.drawer.attach}
            </button>
          </div>
        )}
      </aside>
    </div>
  );
}
