import { useEffect, useRef, type RefObject } from 'react';
import { scopeGeometry } from './scopeGeometry';

/**
 * Radar scope backdrop for the login screen: a sweeping beam over a ranged
 * grid, contacts that light as the beam passes them, and a shield with a
 * keyhole standing in the middle of it.
 *
 * Canvas rather than SVG. The sweep repaints a 34-slice gradient wedge, 40
 * contacts and ~140 ticks every frame; as DOM that would be hundreds of nodes
 * being restyled 60 times a second.
 *
 * Colours come from the theme tokens, not from literals, so the scope is the
 * same signal red as the badges, the matrix and the gauges. Change --accent and
 * this changes with it.
 */

/** Contacts on the scope. 42% read as hostile and get a ping ring. */
const BLIP_COUNT = 40;
const HOSTILE_SHARE = 0.42;

/** Radians per second the beam turns. One revolution is about 12s. */
const SWEEP_SPEED = 0.5;

/**
 * Radians per second the four outer corner arcs turn - a lap takes just under
 * a minute, slow enough to read as drift rather than as a second sweep.
 *
 * Negative, so the bezel counter-rotates against the beam. Turning the same way
 * at a different rate reads as a sweep that is lagging or broken; turning the
 * other way reads unmistakably as a separate ring, which is what it is.
 */
const BEZEL_SPEED = -0.115;

/** Where the beam parks when animation is switched off, in seconds of "time". */
const STILL_TIME = -Math.PI / 3 / SWEEP_SPEED;

interface Blip {
  /** Angle on the scope, radians. */
  a: number;
  /** Distance from centre, pixels. */
  r: number;
  /** 1 when just swept, decaying to 0. */
  lit: number;
  hostile: boolean;
  drift: number;
}

function rgb(hex: string, fallback: [number, number, number]): [number, number, number] {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex.trim());
  if (!m) return fallback;
  const n = parseInt(m[1], 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

/**
 * The shield silhouette, as unit coordinates around its centre.
 *
 * Three quadratic curves: a top edge that dips toward the middle to give the
 * flared shoulders, then each side sweeping down to the point. Flattened to a
 * polyline once at startup rather than re-evaluated per frame - it is stroked
 * three times a frame (glow, line, clip) and the curve never changes.
 */
function shieldOutline(): [number, number][] {
  const pts: [number, number][] = [];
  const quad = (
    p0: [number, number],
    p1: [number, number],
    p2: [number, number],
    steps: number,
  ) => {
    for (let i = 1; i <= steps; i++) {
      const t = i / steps;
      const u = 1 - t;
      pts.push([
        u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0],
        u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1],
      ]);
    }
  };
  const TL: [number, number] = [-0.86, -1.0];
  const TR: [number, number] = [0.86, -1.0];
  const BOTTOM: [number, number] = [0, 1.16];
  quad(TL, [0, -0.74], TR, 30);
  quad(TR, [0.9, 0.42], BOTTOM, 60);
  quad(BOTTOM, [-0.9, 0.42], TL, 60);
  return pts;
}

export function RadarField({
  cardRef,
  statusLabel,
}: {
  /** The login card, so the scope can centre itself in the space beside it. */
  cardRef: RefObject<HTMLElement | null>;
  /** "SCANNING", in the interface language. */
  statusLabel: string;
}) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  // Read through a ref so a language switch does not restart the animation.
  const statusRef = useRef(statusLabel);
  statusRef.current = statusLabel;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const styles = getComputedStyle(document.documentElement);
    const accent = rgb(styles.getPropertyValue('--accent'), [229, 37, 26]);
    const ink = rgb(styles.getPropertyValue('--n-900'), [13, 13, 11]);
    const ac = (alpha: number) => `rgba(${accent[0]},${accent[1]},${accent[2]},${alpha})`;
    const inkA = (alpha: number) => `rgba(${ink[0]},${ink[1]},${ink[2]},${alpha})`;

    const outline = shieldOutline();
    let blips: Blip[] = [];
    let cx = 0;
    let cy = 0;
    let scale = 0;
    let radius = 0;
    let width = 0;
    let height = 0;

    function resize() {
      if (!canvas || !ctx) return;
      const dpr = Math.min(2, window.devicePixelRatio || 1);
      width = canvas.clientWidth;
      height = canvas.clientHeight;
      canvas.width = Math.round(width * dpr);
      canvas.height = Math.round(height * dpr);
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

      // Shared with the particle field, which keeps out of this circle.
      ({ cx, cy, scale, radius } = scopeGeometry(canvas, cardRef.current, width, height));

      blips = Array.from({ length: BLIP_COUNT }, () => ({
        a: Math.random() * Math.PI * 2,
        r: (0.16 + Math.random() * 0.86) * radius,
        lit: 0,
        hostile: Math.random() < HOSTILE_SHARE,
        drift: (Math.random() - 0.5) * 0.0006,
      }));
    }

    function shieldPath() {
      if (!ctx) return;
      ctx.beginPath();
      ctx.moveTo(cx + outline[0][0] * scale, cy + outline[0][1] * scale);
      for (const p of outline) ctx.lineTo(cx + p[0] * scale, cy + p[1] * scale);
      ctx.closePath();
    }

    function draw(time: number, animating: boolean) {
      if (!ctx) return;
      const R = radius;
      // Both angles derive from one clock, so they can never drift apart.
      const sweep = time * SWEEP_SPEED;
      const bezel = time * BEZEL_SPEED;
      ctx.clearRect(0, 0, width, height);
      ctx.lineCap = 'butt';
      ctx.lineJoin = 'round';

      // Ambient bloom, so the scope sits in light rather than on a flat field.
      const amb = ctx.createRadialGradient(cx, cy, 0, cx, cy, R * 1.5);
      amb.addColorStop(0, ac(0.09));
      amb.addColorStop(0.6, ac(0.025));
      amb.addColorStop(1, 'rgba(0,0,0,0)');
      ctx.fillStyle = amb;
      ctx.fillRect(cx - R * 1.6, cy - R * 1.6, R * 3.2, R * 3.2);

      // Range rings and bearing spokes.
      ctx.lineWidth = 1.1;
      ctx.strokeStyle = ac(0.42);
      for (let i = 1; i <= 5; i++) {
        ctx.beginPath();
        ctx.arc(cx, cy, R * (i / 5) * 0.92, 0, Math.PI * 2);
        ctx.stroke();
      }
      for (let i = 0; i < 12; i++) {
        const a = (i / 12) * Math.PI * 2;
        ctx.strokeStyle = ac(i % 3 === 0 ? 0.46 : 0.26);
        ctx.beginPath();
        ctx.moveTo(cx + Math.cos(a) * R * 0.1, cy + Math.sin(a) * R * 0.1);
        ctx.lineTo(cx + Math.cos(a) * R * 0.92, cy + Math.sin(a) * R * 0.92);
        ctx.stroke();
      }

      // Graduated bezel: 120 ticks, every tenth longer, plus four corner arcs.
      ctx.strokeStyle = ac(0.55);
      ctx.lineWidth = 1.4;
      ctx.beginPath();
      ctx.arc(cx, cy, R, 0, Math.PI * 2);
      ctx.stroke();
      for (let i = 0; i < 120; i++) {
        const a = (i / 120) * Math.PI * 2;
        const long = i % 10 === 0;
        const r0 = R + 4;
        const r1 = R + (long ? 14 : 8);
        ctx.strokeStyle = ac(long ? 0.7 : 0.4);
        ctx.lineWidth = long ? 1.6 : 1;
        ctx.beginPath();
        ctx.moveTo(cx + Math.cos(a) * r0, cy + Math.sin(a) * r0);
        ctx.lineTo(cx + Math.cos(a) * r1, cy + Math.sin(a) * r1);
        ctx.stroke();
      }
      ctx.strokeStyle = ac(0.4);
      ctx.lineWidth = 2.2;
      for (let q = 0; q < 4; q++) {
        const a0 = bezel + (q * Math.PI) / 2 + 0.34;
        ctx.beginPath();
        ctx.arc(cx, cy, R + 24, a0, a0 + 0.62);
        ctx.stroke();
      }

      // The beam: a wedge built from slices whose alpha falls off along the
      // tail, which is how a phosphor sweep actually decays.
      ctx.save();
      ctx.beginPath();
      ctx.arc(cx, cy, R * 0.94, 0, Math.PI * 2);
      ctx.clip();
      const TAIL = 1.35;
      const SLICES = 34;
      for (let i = 0; i < SLICES; i++) {
        const f = i / SLICES;
        const a1 = sweep - TAIL * f;
        const a0 = sweep - TAIL * (f + 1 / SLICES) - 0.004;
        ctx.fillStyle = ac(0.2 * Math.pow(1 - f, 2.1));
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.arc(cx, cy, R * 0.94, a0, a1);
        ctx.closePath();
        ctx.fill();
      }
      ctx.strokeStyle = ac(0.85);
      ctx.lineWidth = 1.6;
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(cx + Math.cos(sweep) * R * 0.94, cy + Math.sin(sweep) * R * 0.94);
      ctx.stroke();
      ctx.restore();

      // Contacts. Standing still they would all be lit at once, so a frozen
      // scope shows them at a fixed dim value instead of decaying to nothing.
      for (const b of blips) {
        if (animating) {
          b.a += b.drift;
          let d = sweep - b.a;
          d = ((d % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
          if (d < 0.05) b.lit = 1;
          b.lit = Math.max(0, b.lit - 0.006);
        } else {
          b.lit = 0.55;
        }
        if (b.lit <= 0.001) continue;
        const x = cx + Math.cos(b.a) * b.r;
        const y = cy + Math.sin(b.a) * b.r;
        ctx.fillStyle = b.hostile ? ac(b.lit) : inkA(b.lit * 0.6);
        ctx.beginPath();
        ctx.arc(x, y, b.hostile ? 5.4 : 3.6, 0, Math.PI * 2);
        ctx.fill();
        if (b.hostile) {
          ctx.strokeStyle = ac(b.lit * 0.6);
          ctx.lineWidth = 1.6;
          ctx.beginPath();
          ctx.arc(x, y, 7 + (1 - b.lit) * 30, 0, Math.PI * 2);
          ctx.stroke();
        }
      }

      // The shield: a soft outer glow, then a crisp line over it.
      ctx.save();
      ctx.shadowColor = ac(0.35);
      ctx.shadowBlur = 18;
      ctx.strokeStyle = ac(0.2);
      ctx.lineWidth = 9;
      shieldPath();
      ctx.stroke();
      ctx.shadowBlur = 0;
      ctx.strokeStyle = ac(1);
      ctx.lineWidth = 3;
      shieldPath();
      ctx.stroke();
      ctx.restore();

      ctx.save();
      shieldPath();
      ctx.clip();
      const inner = ctx.createLinearGradient(cx, cy - scale, cx, cy + scale);
      inner.addColorStop(0, ac(0.04));
      inner.addColorStop(1, ac(0.1));
      ctx.fillStyle = inner;
      ctx.fill();
      ctx.restore();

      // Keyhole: circle and stem drawn as one path so the join is clean.
      const kr = scale * 0.24;
      const ky = cy - scale * 0.22;
      const stemW = scale * 0.125;
      const stemBottom = cy + scale * 0.46;
      const hx = stemW / 2;
      const hy = Math.sqrt(Math.max(0, kr * kr - hx * hx));
      const angR = Math.atan2(hy, hx);
      ctx.save();
      ctx.shadowColor = ac(0.3);
      ctx.shadowBlur = 10;
      ctx.strokeStyle = inkA(1);
      ctx.lineWidth = scale * 0.05;
      ctx.lineJoin = 'round';
      ctx.beginPath();
      ctx.moveTo(cx + hx, stemBottom);
      ctx.lineTo(cx + hx, ky + hy);
      ctx.arc(cx, ky, kr, angR, Math.PI - angR, true);
      ctx.lineTo(cx - hx, stemBottom);
      ctx.closePath();
      ctx.stroke();
      ctx.restore();

      // Instrument readout under the bezel.
      ctx.font = `500 ${Math.max(9, scale * 0.075).toFixed(0)}px 'IBM Plex Mono', ui-monospace, monospace`;
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillStyle = ac(0.75);
      ctx.fillText(statusRef.current, cx, cy + R + 30);
      ctx.fillStyle = inkA(0.45);
      const bearing = (((sweep * 180) / Math.PI + 90) % 360).toFixed(0).padStart(3, '0');
      ctx.fillText(`${bearing}°`, cx, cy + R + 48);
    }

    const still = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    let raf = 0;
    const start = performance.now();

    const onResize = () => {
      resize();
      // A frozen scope has no loop to repaint it, so redraw on demand.
      if (still) draw(STILL_TIME, false);
    };

    resize();

    if (still) {
      // One static frame, beam parked off the vertical.
      draw(STILL_TIME, false);
    } else {
      const loop = (now: number) => {
        draw((now - start) / 1000, true);
        raf = requestAnimationFrame(loop);
      };
      raf = requestAnimationFrame(loop);
    }

    window.addEventListener('resize', onResize);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener('resize', onResize);
    };
  }, [cardRef]);

  return <canvas ref={canvasRef} className="radar-field" aria-hidden="true" />;
}
