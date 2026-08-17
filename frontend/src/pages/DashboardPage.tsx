import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getDashboard, type Dashboard } from '../api/analytics';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { Gauge } from '../components/Gauge';
import { BarChart, DonutChart, LineChart, type Series } from '../components/Charts';
import { LevelBadge } from '../components/DataTable';
import { useI18n } from '../i18n/I18nContext';

export default function DashboardPage() {
  const { user, isAdmin } = useAuth();
  const { t, level, criticality, method, status } = useI18n();
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getDashboard().then(setData).catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <p className="form-error">{error}</p>;
  if (!data) return <p className="muted">{t.action.loading}</p>;

  const maxCurrent = Math.max(1, ...data.currentDistribution.map((d) => d.count));

  /**
   * The reduction chart, transposed.
   *
   * The API returns one distribution per stage; a line chart needs one series
   * per LEVEL, each holding its count at the three stages. So the three arrays
   * are read column-wise. They are all produced by the same `distribution()`
   * helper on the server, which always emits five entries ordered 5..1, so the
   * indexes line up without any lookup by level.
   */
  const stages = [t.charts.stageInherent, t.charts.stageCurrent, t.charts.stageResidual];
  const reductionSeries: Series[] = data.inherentDistribution.map((d, i) => ({
    key: `lvl-${d.level}`,
    label: level(d.label),
    level: d.level,
    values: [d.count, data.currentDistribution[i].count, data.residualDistribution[i].count],
  }));

  /** "2026-03" -> "мар 2026" / "mar 2026". */
  const monthLabel = (iso: string) => {
    const [year, month] = iso.split('-');
    return `${t.charts.months[Number(month) - 1]} ${year}`;
  };

  const timelineSeries: Series[] = [
    {
      key: 'due',
      label: t.charts.timelineDue,
      values: data.remediationTimeline.map((p) => p.dueTotal),
      dashed: true,
    },
    {
      key: 'done',
      label: t.charts.timelineDone,
      level: 1,
      area: true,
      values: data.remediationTimeline.map((p) => p.doneTotal),
    },
  ];

  return (
    <div>
      <div className="page-header">
        <h1>{t.dashboard.title}</h1>
        <span className="muted">
          {user?.fullName} · {isAdmin ? t.role.admin : t.role.user}
        </span>
      </div>

      <section className="stat-row">
        <StatCard value={data.totalRisks} label={t.dashboard.risks} to="/risks" />
        <StatCard value={data.totalAssets} label={t.dashboard.assets} to="/assets" />
        <StatCard value={data.totalThreats} label={t.dashboard.threats} to="/threats" />
        <StatCard value={data.totalControls} label={t.dashboard.controls} to="/controls" />
        <StatCard
          value={`${data.implementedPercent}%`}
          label={t.dashboard.implementedPercent}
          hint={t.dashboard.implementedHint(
            data.implementedControlLinks,
            data.implementedControlLinks + data.plannedControlLinks,
          )}
        />
        <StatCard
          value={data.overdueMeasures}
          label={t.dashboard.overdue}
          danger={data.overdueMeasures > 0}
          hint={t.dashboard.overdueHint}
        />
      </section>

      <section className="panel">
        <h2>{t.dashboard.keyAssets}</h2>
        <p className="muted">{t.dashboard.gaugeHint}</p>
        {data.assetGauges.length === 0 ? (
          <p className="muted">{t.dashboard.noAssets}</p>
        ) : (
          <div className="gauge-grid">
            {data.assetGauges.map((g) => (
              <div className="gauge-card" key={g.assetId}>
                <div className="gauge-card-head">
                  <strong>{g.code}</strong>
                  <span className="muted"> · {criticality(g.criticality)}</span>
                </div>
                <div className="gauge-card-name" title={g.name}>
                  {g.name}
                </div>
                <Gauge
                  ariaLabel={`${g.code}: ${
                    g.worstCurrentLabel ? level(g.worstCurrentLabel) : t.dashboard.noRisks
                  }`}
                  level={g.worstCurrentLevel}
                  label={g.worstCurrentLabel ? level(g.worstCurrentLabel) : t.dashboard.noRisks}
                  caption={
                    g.riskCount === 0
                      ? t.dashboard.noRisksForAsset
                      : t.dashboard.assetRisks(
                          g.riskCount,
                          g.worstResidualLabel ? level(g.worstResidualLabel) : null,
                        )
                  }
                />
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="panel">
        <h2>{t.dashboard.distribution}</h2>
        <div className="dist-grid">
          <div>
            <h3 className="legend-title">{t.dashboard.currentLevel}</h3>
            {data.currentDistribution.map((d) => (
              <div className="dist-row" key={d.level}>
                <LevelBadge level={d.level} />
                <span className="dist-label">{level(d.label)}</span>
                <div className="dist-bar-track">
                  <div
                    className={`dist-bar level-${d.level}`}
                    style={{ width: `${(d.count / maxCurrent) * 100}%` }}
                  />
                </div>
                <strong className="dist-count">{d.count}</strong>
              </div>
            ))}
          </div>
          <div>
            <h3 className="legend-title">{t.dashboard.residualLevel}</h3>
            {data.residualDistribution.map((d) => (
              <div className="dist-row" key={d.level}>
                <LevelBadge level={d.level} />
                <span className="dist-label">{level(d.label)}</span>
                <div className="dist-bar-track">
                  <div
                    className={`dist-bar level-${d.level}`}
                    style={{ width: `${(d.count / maxCurrent) * 100}%` }}
                  />
                </div>
                <strong className="dist-count">{d.count}</strong>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="panel">
        <h2>{t.charts.reductionTitle}</h2>
        <p className="chart-note muted">{t.charts.reductionNote}</p>
        <LineChart
          labels={stages}
          series={reductionSeries}
          yLabel={t.charts.risksAxis}
          emptyText={t.charts.noData}
        />
      </section>

      <section className="panel">
        <h2>{t.charts.timelineTitle}</h2>
        <p className="chart-note muted">{t.charts.timelineNote}</p>
        <LineChart
          labels={data.remediationTimeline.map((p) => monthLabel(p.month))}
          series={timelineSeries}
          yLabel={t.charts.measuresAxis}
          emptyText={t.charts.noDeadlines}
        />
      </section>

      <div className="chart-grid-2">
        <section className="chart-card">
          <h2>{t.charts.treatmentTitle}</h2>
          <p className="chart-note">{t.charts.treatmentNote}</p>
          <DonutChart
            centerLabel={t.charts.treatmentCenter}
            emptyText={t.charts.noData}
            slices={data.treatmentBreakdown.map((s) => ({
              key: s.label,
              label: method(s.label),
              value: s.count,
            }))}
          />
        </section>

        <section className="chart-card">
          <h2>{t.charts.statusTitle}</h2>
          <p className="chart-note">{t.charts.statusNote}</p>
          <BarChart
            emptyText={t.charts.noData}
            bars={data.statusBreakdown.map((s) => ({
              key: s.label,
              label: status(s.label),
              value: s.count,
            }))}
          />
        </section>
      </div>
    </div>
  );
}

function StatCard({
  value,
  label,
  hint,
  to,
  danger,
}: {
  value: number | string;
  label: string;
  hint?: string;
  to?: string;
  danger?: boolean;
}) {
  const body = (
    <>
      <div className={danger ? 'stat-value stat-danger' : 'stat-value'}>{value}</div>
      <div className="stat-label">{label}</div>
      {hint && <div className="stat-hint muted">{hint}</div>}
    </>
  );
  return to ? (
    <Link className="stat-card stat-card-link" to={to}>
      {body}
    </Link>
  ) : (
    <div className="stat-card">{body}</div>
  );
}
