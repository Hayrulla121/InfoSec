import { useId, useMemo, useState } from 'react';

/**
 * Charts, drawn as inline SVG.
 *
 * No charting library, for the same reason the icons and the Gauge are
 * hand-rolled: Recharts pulls in D3 and adds roughly 100 KB gzipped to the
 * bundle, and what this dashboard needs is three chart types with no zooming,
 * no brushing and no animation. The whole file is smaller than the dependency's
 * README.
 *
 * Two conventions everything here follows:
 *
 * <ul>
 *   <li>Geometry is computed in a fixed viewBox coordinate space and the SVG is
 *       stretched to its container with CSS. That way the maths never has to
 *       know the pixel width, and the chart stays sharp at any size.
 *   <li>Colour comes from CSS classes, never from hard-coded hex, so the charts
 *       inherit the same --lvl-N ramp as the badges, the matrix and the gauges.
 *       A level-4 line is the same orange as a level-4 badge, always.
 * </ul>
 */

// ---------------------------------------------------------------- line chart

export interface Series {
  /** Stable key, also used to pick the CSS colour class. */
  key: string;
  label: string;
  values: number[];
  /** 1..5 to use the risk ramp; omitted falls back to the accent colour. */
  level?: number;
  /** Draw a soft fill under the line. Only sensible for one or two series. */
  area?: boolean;
  dashed?: boolean;
}

const W = 720;
const H = 300;
const PAD = { top: 18, right: 18, bottom: 40, left: 46 };
const PLOT_W = W - PAD.left - PAD.right;
const PLOT_H = H - PAD.top - PAD.bottom;

/**
 * Picks axis ticks that land on round numbers.
 *
 * Naively slicing the max into five equal parts gives an axis labelled 0, 2.6,
 * 5.2 - technically correct and unreadable. This walks a 1/2/5/10 ladder until
 * the step is big enough, which is how every charting library does it and why
 * their axes always look deliberate.
 */
function niceTicks(max: number, target = 4): number[] {
  if (max <= 0) return [0, 1];
  const rough = max / target;
  const magnitude = 10 ** Math.floor(Math.log10(rough));
  const normalised = rough / magnitude;
  const step = (normalised <= 1 ? 1 : normalised <= 2 ? 2 : normalised <= 5 ? 5 : 10) * magnitude;
  const ticks: number[] = [];
  for (let v = 0; v <= max + step / 2; v += step) ticks.push(Math.round(v * 100) / 100);
  return ticks;
}

export function LineChart({
  labels,
  series,
  yLabel,
  formatValue = (v) => String(v),
  emptyText,
}: {
  labels: string[];
  series: Series[];
  yLabel?: string;
  formatValue?: (v: number) => string;
  emptyText: string;
}) {
  const clipId = useId();
  const [hover, setHover] = useState<number | null>(null);

  const { ticks, xAt, yAt } = useMemo(() => {
    const rawMax = Math.max(1, ...series.flatMap((s) => s.values));
    const t = niceTicks(rawMax);
    const top = t[t.length - 1];
    return {
      ticks: t,
      // A single point would divide by zero; park it in the middle instead.
      xAt: (i: number) =>
        labels.length <= 1 ? PAD.left + PLOT_W / 2 : PAD.left + (i / (labels.length - 1)) * PLOT_W,
      yAt: (v: number) => PAD.top + PLOT_H - (v / top) * PLOT_H,
    };
  }, [labels.length, series]);

  if (labels.length === 0 || series.length === 0) {
    return <p className="muted chart-empty">{emptyText}</p>;
  }

  const path = (values: number[]) =>
    values.map((v, i) => `${i === 0 ? 'M' : 'L'} ${xAt(i)} ${yAt(v)}`).join(' ');

  const areaPath = (values: number[]) =>
    `${path(values)} L ${xAt(values.length - 1)} ${PAD.top + PLOT_H} L ${xAt(0)} ${
      PAD.top + PLOT_H
    } Z`;

  /**
   * Show every label when they fit, otherwise every nth. Rotating them or
   * letting them overlap both look worse than simply showing fewer.
   */
  const labelStep = Math.ceil(labels.length / 8);

  return (
    <div className="chart">
      <svg
        viewBox={`0 0 ${W} ${H}`}
        className="chart-svg"
        role="img"
        aria-label={yLabel}
        onMouseLeave={() => setHover(null)}
      >
        <defs>
          <clipPath id={clipId}>
            <rect x={PAD.left} y={PAD.top} width={PLOT_W} height={PLOT_H} />
          </clipPath>
        </defs>

        {ticks.map((v) => (
          <g key={v}>
            <line
              className="chart-grid"
              x1={PAD.left}
              y1={yAt(v)}
              x2={PAD.left + PLOT_W}
              y2={yAt(v)}
            />
            <text className="chart-tick" x={PAD.left - 9} y={yAt(v) + 4} textAnchor="end">
              {formatValue(v)}
            </text>
          </g>
        ))}

        {labels.map((label, i) =>
          i % labelStep === 0 || i === labels.length - 1 ? (
            <text
              key={label + i}
              className="chart-tick"
              x={xAt(i)}
              y={PAD.top + PLOT_H + 22}
              // The end labels sit exactly on the plot edges, so centring them
              // hangs half the text outside the chart - where the panel clips
              // it. Anchoring the ends inward keeps long stage names readable.
              textAnchor={i === 0 ? 'start' : i === labels.length - 1 ? 'end' : 'middle'}
            >
              {label}
            </text>
          ) : null,
        )}

        {/* Hover guide sits under the lines so it never hides a data point. */}
        {hover !== null && (
          <line
            className="chart-guide"
            x1={xAt(hover)}
            y1={PAD.top}
            x2={xAt(hover)}
            y2={PAD.top + PLOT_H}
          />
        )}

        <g clipPath={`url(#${clipId})`}>
          {series.map((s) =>
            s.area ? (
              <path
                key={`a-${s.key}`}
                className={`chart-area ${s.level ? `series-${s.level}` : 'series-accent'}`}
                d={areaPath(s.values)}
              />
            ) : null,
          )}
          {series.map((s) => (
            <path
              key={s.key}
              className={`chart-line ${s.level ? `series-${s.level}` : 'series-accent'} ${
                s.dashed ? 'chart-line-dashed' : ''
              }`}
              d={path(s.values)}
            />
          ))}
        </g>

        {series.map((s) =>
          s.values.map((v, i) => (
            <circle
              key={`${s.key}-${i}`}
              className={`chart-dot ${s.level ? `series-${s.level}` : 'series-accent'}`}
              cx={xAt(i)}
              cy={yAt(v)}
              r={hover === i ? 5 : 3}
            />
          )),
        )}

        {/* Invisible full-height columns: a much bigger hover target than the
            2px line itself, which is nearly impossible to hit with a mouse. */}
        {labels.map((label, i) => (
          <rect
            key={`hit-${label}-${i}`}
            x={xAt(i) - PLOT_W / Math.max(labels.length, 2) / 2}
            y={PAD.top}
            width={PLOT_W / Math.max(labels.length, 2)}
            height={PLOT_H}
            fill="transparent"
            onMouseEnter={() => setHover(i)}
          />
        ))}
      </svg>

      <div className="chart-legend">
        {series.map((s) => (
          <span className="chart-legend-item" key={s.key}>
            <i className={`chart-swatch ${s.level ? `series-${s.level}` : 'series-accent'}`} />
            {s.label}
          </span>
        ))}
      </div>

      {hover !== null && (
        <div className="chart-readout" role="status">
          <strong>{labels[hover]}</strong>
          {series.map((s) => (
            <span key={s.key}>
              <i className={`chart-swatch ${s.level ? `series-${s.level}` : 'series-accent'}`} />
              {s.label}: <strong>{formatValue(s.values[hover])}</strong>
            </span>
          ))}
        </div>
      )}

      {/* The same numbers as a table, for screen readers and for anyone who
          wants the exact figures rather than the shape.

          The hiding class goes on a WRAPPER, not on the table. A table ignores
          an explicit 1px width and grows to fit its content, so hiding it
          directly leaves a full-width invisible element that still counts
          towards scrollWidth - which shows up as a phantom horizontal
          scrollbar on narrow screens. A div honours the width and clips. */}
      <div className="visually-hidden">
        <table>
          <caption>{yLabel}</caption>
          <thead>
            <tr>
              <th />
              {labels.map((l, i) => (
                <th key={`${l}-${i}`}>{l}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {series.map((s) => (
              <tr key={s.key}>
                <th>{s.label}</th>
                {s.values.map((v, i) => (
                  <td key={i}>{formatValue(v)}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// -------------------------------------------------------------------- donut

export interface Slice {
  key: string;
  label: string;
  value: number;
  /** 1..5 for the risk ramp, otherwise a rotating palette class is used. */
  level?: number;
}

const DONUT = { cx: 90, cy: 90, r: 70, thickness: 26 };

function arc(startFrac: number, endFrac: number) {
  const { cx, cy, r, thickness } = DONUT;
  const inner = r - thickness;
  // -90deg so the first slice starts at twelve o'clock rather than three.
  const a0 = startFrac * 2 * Math.PI - Math.PI / 2;
  const a1 = endFrac * 2 * Math.PI - Math.PI / 2;
  const large = endFrac - startFrac > 0.5 ? 1 : 0;

  const p = (angle: number, radius: number) => ({
    x: cx + radius * Math.cos(angle),
    y: cy + radius * Math.sin(angle),
  });
  const o0 = p(a0, r);
  const o1 = p(a1, r);
  const i1 = p(a1, inner);
  const i0 = p(a0, inner);

  return [
    `M ${o0.x} ${o0.y}`,
    `A ${r} ${r} 0 ${large} 1 ${o1.x} ${o1.y}`,
    `L ${i1.x} ${i1.y}`,
    `A ${inner} ${inner} 0 ${large} 0 ${i0.x} ${i0.y}`,
    'Z',
  ].join(' ');
}

export function DonutChart({
  slices,
  centerLabel,
  emptyText,
}: {
  slices: Slice[];
  centerLabel: string;
  emptyText: string;
}) {
  const total = slices.reduce((sum, s) => sum + s.value, 0);
  if (total === 0) return <p className="muted chart-empty">{emptyText}</p>;

  let cursor = 0;
  const drawn = slices.map((s, i) => {
    const start = cursor;
    cursor += s.value / total;
    return {
      ...s,
      d: arc(start, cursor),
      // A single slice would draw a zero-length arc and disappear, so a full
      // ring is drawn instead of an arc when one category holds everything.
      full: s.value === total,
      cls: s.level ? `series-${s.level}` : `series-p${(i % 5) + 1}`,
      percent: Math.round((s.value / total) * 100),
    };
  });

  return (
    <div className="chart chart-donut">
      <svg viewBox="0 0 180 180" role="img" aria-label={centerLabel}>
        {drawn.map((s) =>
          s.full ? (
            <circle
              key={s.key}
              className={`donut-ring ${s.cls}`}
              cx={DONUT.cx}
              cy={DONUT.cy}
              r={DONUT.r - DONUT.thickness / 2}
              strokeWidth={DONUT.thickness}
              fill="none"
            />
          ) : (
            <path key={s.key} className={`donut-slice ${s.cls}`} d={s.d} />
          ),
        )}
        <text className="donut-total" x={DONUT.cx} y={DONUT.cy + 2} textAnchor="middle">
          {total}
        </text>
        <text className="donut-caption" x={DONUT.cx} y={DONUT.cy + 20} textAnchor="middle">
          {centerLabel}
        </text>
      </svg>

      <ul className="chart-legend chart-legend-list">
        {drawn.map((s) => (
          <li className="chart-legend-item" key={s.key}>
            <i className={`chart-swatch ${s.cls}`} />
            <span className="chart-legend-label">{s.label}</span>
            <strong>{s.value}</strong>
            <span className="muted">{s.percent}%</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

// ------------------------------------------------------------- horizontal bar

export function BarChart({
  bars,
  emptyText,
}: {
  bars: { key: string; label: string; value: number; level?: number }[];
  emptyText: string;
}) {
  if (bars.length === 0) return <p className="muted chart-empty">{emptyText}</p>;
  const max = Math.max(1, ...bars.map((b) => b.value));

  return (
    <div className="hbar-chart">
      {bars.map((b, i) => (
        <div className="hbar-row" key={b.key}>
          <span className="hbar-label" title={b.label}>
            {b.label}
          </span>
          <span className="hbar-track">
            <span
              className={`hbar-fill ${b.level ? `series-${b.level}` : `series-p${(i % 5) + 1}`}`}
              style={{ width: `${(b.value / max) * 100}%` }}
            />
          </span>
          <strong className="hbar-value">{b.value}</strong>
        </div>
      ))}
    </div>
  );
}
