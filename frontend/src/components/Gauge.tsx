/**
 * Speedometer gauge, drawn as inline SVG.
 *
 * No chart library: a five-zone semicircular dial is about forty lines of
 * trigonometry, and hand-rolling it avoids a dependency plus its bundle weight.
 *
 * Geometry: a 180-degree arc from 180deg (left) to 0deg (right). Level 1..5
 * maps onto that sweep, and the needle points at the middle of its zone.
 */

const ZONE_COLORS = ['#2e9e5b', '#94bf3a', '#f0c419', '#e8791e', '#cf3a2b'];

const CX = 100;
const CY = 92;
const R = 76;
const THICKNESS = 17;

/** Polar -> cartesian. 180deg is the left end of the dial, 0deg the right. */
function point(angleDeg: number, radius: number) {
  const rad = (angleDeg * Math.PI) / 180;
  return { x: CX + radius * Math.cos(rad), y: CY - radius * Math.sin(rad) };
}

/** One coloured band of the dial. */
function zonePath(index: number) {
  const start = 180 - index * 36;
  const end = start - 36;
  const outer = R;
  const inner = R - THICKNESS;

  const o1 = point(start, outer);
  const o2 = point(end, outer);
  const i2 = point(end, inner);
  const i1 = point(start, inner);

  // Arc flags: rx ry rotation large-arc sweep x y. Each zone is 36deg, so
  // large-arc is always 0; sweep 1 goes clockwise, sweep 0 back.
  return [
    `M ${o1.x} ${o1.y}`,
    `A ${outer} ${outer} 0 0 1 ${o2.x} ${o2.y}`,
    `L ${i2.x} ${i2.y}`,
    `A ${inner} ${inner} 0 0 0 ${i1.x} ${i1.y}`,
    'Z',
  ].join(' ');
}

export function Gauge({
  level,
  label,
  caption,
  ariaLabel,
}: {
  /** 1..5, or null when there is nothing to show. */
  level: number | null;
  label?: string | null;
  caption?: string;
  /** Accessible description; supplied by the caller so it is translatable. */
  ariaLabel?: string;
}) {
  // Needle points at the centre of its zone: level 1 -> 162deg, level 5 -> 18deg.
  const needleAngle = level == null ? 90 : 180 - (level - 0.5) * 36;
  const tip = point(needleAngle, R - THICKNESS - 6);

  return (
    <div className="gauge">
      {/* viewBox is taller than the dial so the value can sit BELOW the pivot.
          Putting it inside the arc collides with the needle at level 3, where
          the needle points straight up through the middle. */}
      <svg viewBox="0 0 200 126" role="img" aria-label={ariaLabel ?? String(level ?? '')}>
        {ZONE_COLORS.map((color, i) => (
          <path
            key={i}
            d={zonePath(i)}
            fill={color}
            // Dim every zone except the active one, so the needle's zone reads
            // at a glance even in a grid of many cards.
            opacity={level == null ? 0.28 : i + 1 === level ? 1 : 0.28}
          />
        ))}

        {level != null && (
          <>
            <line
              x1={CX}
              y1={CY}
              x2={tip.x}
              y2={tip.y}
              stroke="#1c2430"
              strokeWidth={3}
              strokeLinecap="round"
            />
            <circle cx={CX} cy={CY} r={5} fill="#1c2430" />
          </>
        )}

        <text x={CX} y={CY + 26} textAnchor="middle" className="gauge-value">
          {level ?? '—'}
        </text>
      </svg>

      {label && <div className={`gauge-label level-text-${level ?? 0}`}>{label}</div>}
      {caption && <div className="muted gauge-caption">{caption}</div>}
    </div>
  );
}
