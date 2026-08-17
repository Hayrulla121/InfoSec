import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getRiskMatrix, type MatrixCell, type RiskMatrix } from '../api/analytics';
import { errorMessage } from '../api/client';
import { useI18n, type Lang } from '../i18n/I18nContext';

/**
 * The 5x5 heat map.
 *
 * Colour comes from the cell's own a x t level, not from how many risks landed
 * in it - the grid means the same thing whether the register is full or empty.
 *
 * Note the loop variables are named `assetRating` / `threatRating` rather than
 * `a` / `t`: a parameter called `t` would shadow the translation dictionary.
 */
export default function RiskMatrixPage() {
  const navigate = useNavigate();
  const { t, lang, level } = useI18n();
  const [matrix, setMatrix] = useState<RiskMatrix | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getRiskMatrix().then(setMatrix).catch((e) => setError(errorMessage(e)));
  }, []);

  if (error) return <p className="form-error">{error}</p>;
  if (!matrix) return <p className="muted">{t.action.loading}</p>;

  const at = (assetRating: number, threatRating: number): MatrixCell | undefined =>
    matrix.cells.find(
      (c) => c.assetRating === assetRating && c.threatRating === threatRating,
    );

  function openCell(cell: MatrixCell) {
    if (!cell.count) return;
    navigate(`/risks?assetRating=${cell.assetRating}&threatRating=${cell.threatRating}`);
  }

  return (
    <div>
      <h1>{t.matrix.title}</h1>
      <p className="muted">{t.matrix.subtitle(matrix.totalRisks)}</p>

      <section className="panel">
        <div className="matrix-wrap">
          <div className="matrix-y-title">{t.matrix.yAxis}</div>

          <table className="matrix">
            <tbody>
              {matrix.assetRatings.map((assetRating) => (
                <tr key={assetRating}>
                  <th className="matrix-head">{assetRating}</th>
                  {matrix.threatRatings.map((threatRating) => {
                    const cell = at(assetRating, threatRating);
                    if (!cell) return <td key={threatRating} />;
                    return (
                      <td
                        key={threatRating}
                        className={`matrix-cell level-${cell.riskLevel} ${
                          cell.count ? 'matrix-cell-clickable' : ''
                        }`}
                        title={t.matrix.cellTitle(
                          assetRating,
                          threatRating,
                          level(cell.riskLabel),
                          cell.count,
                        )}
                        onClick={() => openCell(cell)}
                      >
                        {cell.count ?? ''}
                      </td>
                    );
                  })}
                </tr>
              ))}
              <tr>
                <th />
                {matrix.threatRatings.map((threatRating) => (
                  <th key={threatRating} className="matrix-head">
                    {threatRating}
                  </th>
                ))}
              </tr>
            </tbody>
          </table>
        </div>
        <div className="matrix-x-title">{t.matrix.xAxis}</div>
      </section>

      <section className="panel">
        <h2>{t.matrix.legend}</h2>
        <div className="legend-grid">
          <LegendColumn title={t.matrix.legendAsset} items={matrix.assetLegend} lang={lang} />
          <LegendColumn title={t.matrix.legendThreat} items={matrix.threatLegend} lang={lang} />
          <LegendColumn
            title={t.matrix.legendRisk}
            items={matrix.riskLegend}
            lang={lang}
            coloured
          />
        </div>
      </section>
    </div>
  );
}

/**
 * The server sends each legend entry in both languages, so the column simply
 * picks the side that matches the current UI language.
 */
function LegendColumn({
  title,
  items,
  lang,
  coloured = false,
}: {
  title: string;
  items: { value: number; labelUz: string; labelRu: string }[];
  lang: Lang;
  coloured?: boolean;
}) {
  return (
    <div>
      <h3 className="legend-title">{title}</h3>
      <table className="data-table">
        <tbody>
          {items.map((i) => (
            <tr key={i.value}>
              <td style={{ width: 44 }}>
                <span className={coloured ? `level-badge level-${i.value}` : 'muted'}>
                  {i.value}
                </span>
              </td>
              <td>{lang === 'uz' ? i.labelUz : i.labelRu}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
