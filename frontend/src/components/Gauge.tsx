import { useId } from 'react';

/**
 * Speedometer gauge, drawn as inline SVG.
 *
 * No chart library: a five-zone semicircular dial is about forty lines of
 * trigonometry, and hand-rolling it avoids a dependency plus its bundle weight.
 *
 * Geometry: a 180-degree arc from 180deg (left) to 0deg (right). Level 1..5
 * maps onto that sweep, and the needle points at the middle of its zone.
 */

/**
 * Five zones, coloured from the shared risk ramp rather than from hex literals
 * here. The dial has to agree with the badges, the matrix and the charts — a
 * level-5 needle and a level-5 badge must be the same colour, or the whole
 * "same number, same value, everywhere" rule breaks the moment the theme
 * changes. `series-N` sets --series, which the .gauge-zone rule paints with.
 */
const ZONE_COUNT = 5;

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

/**
 * The alert glow: a single stroked arc riding the dial's OUTER edge.
 *
 * On the outer edge rather than the band's centreline because the visible half
 * of the stroke has to fall outside the dial - everything inside is masked away
 * (see the mask in the render), so a centreline arc would mostly be thrown out.
 */
function haloPath() {
  const radius = R;
  const left = point(180, radius);
  const right = point(0, radius);
  // sweep 1 = clockwise, which for SVG's y-down axes traces over the top
  return `M ${left.x} ${left.y} A ${radius} ${radius} 0 0 1 ${right.x} ${right.y}`;
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
  const maskId = useId();

  // Needle points at the centre of its zone: level 1 -> 162deg, level 5 -> 18deg.
  const needleAngle = level == null ? 90 : 180 - (level - 0.5) * 36;
  const tip = point(needleAngle, R - THICKNESS - 6);

  return (
    <div className="gauge">
      {/* viewBox is taller than the dial so the value can sit BELOW the pivot.
          Putting it inside the arc collides with the needle at level 3, where
          the needle points straight up through the middle. */}
      <svg viewBox="0 0 200 126" role="img" aria-label={ariaLabel ?? String(level ?? '')}>
        {/* Alert glow at the top of the scale. Sits BEHIND the dial in document
            order, so the blurred red bloom reads as light spilling out from
            under the band rather than smearing the band itself. aria-hidden:
            the level is already in the label, and a "glow" is not information
            a screen reader can use. */}
        {level === ZONE_COUNT && (
          <>
            {/* The bands are drawn at 0.28 opacity when they are not the active
                one, so anything painted behind them shows THROUGH and tints the
                green and amber red. Masking the glow to the area outside the
                dial keeps it a rim halo: the ramp stays readable and only the
                surround lights up.

                Order matters here - SVG applies the filter first and the mask
                after, so the blur happens and then everything inside radius R
                is cut away, leaving a soft outer edge and a clean inner one. */}
            <defs>
              <mask
                id={maskId}
                maskUnits="userSpaceOnUse"
                x="-40"
                y="-40"
                width="280"
                height="220"
              >
                <rect x="-40" y="-40" width="280" height="220" fill="#fff" />
                {/* inside the dial */}
                <circle cx={CX} cy={CY} r={R} fill="#000" />
                {/* and below its horizontal ends: the bands stop dead at y = CY,
                    so the blur spilling under that line was the glow appearing
                    to start before the green band and run past the red one.
                    Cutting on the same line makes both ends flush with them. */}
                <rect x="-40" y={CY} width="280" height="220" fill="#000" />
              </mask>
            </defs>
            <path
              className="gauge-alert"
              aria-hidden="true"
              mask={`url(#${maskId})`}
              d={haloPath()}
            />
          </>
        )}

        {Array.from({ length: ZONE_COUNT }, (_, i) => (
          <path
            key={i}
            className={`gauge-zone series-${i + 1}`}
            d={zonePath(i)}
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
              className="gauge-needle"
              strokeWidth={3}
              strokeLinecap="round"
            />
            <circle className="gauge-pivot" cx={CX} cy={CY} r={5} />
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
