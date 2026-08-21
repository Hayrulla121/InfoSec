import { useEffect, useRef, type RefObject } from 'react';
import { scopeGeometry, type Scope } from './scopeGeometry';

/**
 * Decorative "constellation" backdrop for the login screen: drifting points
 * joined by lines wherever they come close, with the field leaning toward the
 * cursor and lighting up red around it.
 *
 * Canvas rather than SVG or DOM nodes. At ~90 points this redraws ~4,000
 * pair-distance checks per frame; as SVG that would be thousands of elements
 * being restyled 60 times a second, which is exactly what canvas exists for.
 *
 * Colours are read from the theme tokens once at setup, so the network is the
 * same ink and the same signal red as the rest of the interface rather than
 * carrying its own palette.
 *
 * <b>It keeps out of the radar scope.</b> Two overlapping decorative systems
 * read as noise, not as depth - the constellation's links would cross the
 * bezel and tangle with the sweep. Given the card to measure against, the
 * field fades to nothing as it approaches the scope and suppresses any link
 * with an endpoint inside, leaving the instrument a clean field of its own.
 */

/** One point roughly per this many square pixels, so density feels even. */
const AREA_PER_POINT = 7600;
const MIN_POINTS = 44;
/** Linking is O(n^2): 180 points is ~16k distance checks a frame, which is
 *  comfortable. Raising this much further is where it would start to cost. */
const MAX_POINTS = 180;

/** Points closer than this get joined; the line fades out toward the limit. */
const LINK_DISTANCE = 118;
/** Points within this of the cursor join to it, in accent red. */
const CURSOR_DISTANCE = 175;

/** Clear of the bezel ticks and corner arcs, which reach radius + 24. */
const KEEP_OUT_MARGIN = 34;
/** Particles fade across this band rather than vanishing at a hard edge. */
const KEEP_OUT_FADE = 70;

/** How far the whole field leans toward the cursor, as a fraction of offset. */
const PARALLAX = 0.028;
/** Points are seeded beyond the edges by this much so the lean cannot expose a
 *  bare margin as the field shifts. */
const BLEED = 60;

interface Point {
  x: number;
  y: number;
  vx: number;
  vy: number;
  r: number;
}

function rgb(hex: string, fallback: [number, number, number]): [number, number, number] {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex.trim());
  if (!m) return fallback;
  const n = parseInt(m[1], 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

export function ParticleNetwork({
  avoidRef,
}: {
  /**
   * The login card. Used only to locate the radar scope, via the same helper
   * the radar itself uses, so the keep-out lands exactly on the instrument.
   * Omit it and the field covers the whole canvas.
   */
  avoidRef?: RefObject<HTMLElement | null>;
} = {}) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const avoid = avoidRef;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const styles = getComputedStyle(document.documentElement);
    const ink = rgb(styles.getPropertyValue('--n-900'), [13, 13, 11]);
    const accent = rgb(styles.getPropertyValue('--accent'), [229, 37, 26]);
    const inkRgb = `${ink[0]}, ${ink[1]}, ${ink[2]}`;
    const accentRgb = `${accent[0]}, ${accent[1]}, ${accent[2]}`;

    // Honoured as a hard branch, not a slower animation: the whole point of the
    // setting is that nothing moves on its own.
    const still = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    let width = 0;
    let height = 0;
    let points: Point[] = [];
    let frame = 0;
    let scope: Scope | null = null;

    /**
     * 0 inside the scope, 1 well outside it, easing across the fade band.
     * Applied to dots AND to link opacity, so nothing crosses the bezel.
     */
    function visibility(x: number, y: number): number {
      if (!scope) return 1;
      const d = Math.hypot(x - scope.cx, y - scope.cy);
      const inner = scope.radius + KEEP_OUT_MARGIN;
      if (d <= inner) return 0;
      if (d >= inner + KEEP_OUT_FADE) return 1;
      const t = (d - inner) / KEEP_OUT_FADE;
      // Smoothstep: no visible seam where the band starts and ends.
      return t * t * (3 - 2 * t);
    }

    // Where the cursor is, and where the field has eased to so far.
    const cursor = { x: -9999, y: -9999, active: false };
    const lean = { x: 0, y: 0 };

    function seed() {
      const count = Math.round(
        Math.min(MAX_POINTS, Math.max(MIN_POINTS, (width * height) / AREA_PER_POINT)),
      );
      points = Array.from({ length: count }, () => ({
        x: -BLEED + Math.random() * (width + BLEED * 2),
        y: -BLEED + Math.random() * (height + BLEED * 2),
        // Slow enough that the field reads as breathing rather than swarming.
        vx: (Math.random() - 0.5) * 0.22,
        vy: (Math.random() - 0.5) * 0.22,
        r: 1 + Math.random() * 1.6,
      }));
    }

    function resize() {
      const rect = canvas!.getBoundingClientRect();
      width = rect.width;
      height = rect.height;
      // Back the canvas with device pixels, then draw in CSS pixels, so the
      // dots stay crisp on a retina screen without any per-point maths.
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      canvas!.width = Math.round(width * dpr);
      canvas!.height = Math.round(height * dpr);
      ctx!.setTransform(dpr, 0, 0, dpr, 0, 0);
      scope = avoid ? scopeGeometry(canvas!, avoid.current, width, height) : null;
      seed();
    }

    function draw() {
      ctx!.clearRect(0, 0, width, height);

      if (cursor.active) {
        lean.x += ((cursor.x - width / 2) * PARALLAX - lean.x) * 0.06;
        lean.y += ((cursor.y - height / 2) * PARALLAX - lean.y) * 0.06;
      } else {
        lean.x += (0 - lean.x) * 0.04;
        lean.y += (0 - lean.y) * 0.04;
      }

      for (const p of points) {
        if (!still) {
          p.x += p.vx;
          p.y += p.vy;
          // Wrap through the bleed margin so a point never pops at an edge.
          if (p.x < -BLEED) p.x = width + BLEED;
          if (p.x > width + BLEED) p.x = -BLEED;
          if (p.y < -BLEED) p.y = height + BLEED;
          if (p.y > height + BLEED) p.y = -BLEED;
        }
      }

      // Links first so the dots sit on top of their own lines.
      for (let i = 0; i < points.length; i++) {
        const a = points[i];
        const ax = a.x + lean.x;
        const ay = a.y + lean.y;
        const av = visibility(ax, ay);
        if (av <= 0) continue;

        for (let j = i + 1; j < points.length; j++) {
          const b = points[j];
          const bx = b.x + lean.x;
          const by = b.y + lean.y;
          const dx = ax - bx;
          const dy = ay - by;
          const dist = Math.hypot(dx, dy);
          if (dist > LINK_DISTANCE) continue;
          // The dimmer end governs: a link is only as visible as its faintest
          // endpoint, so no line reaches into the scope from outside it.
          const v = Math.min(av, visibility(bx, by));
          if (v <= 0) continue;
          ctx!.strokeStyle = `rgba(${inkRgb}, ${0.16 * (1 - dist / LINK_DISTANCE) * v})`;
          ctx!.lineWidth = 1;
          ctx!.beginPath();
          ctx!.moveTo(ax, ay);
          ctx!.lineTo(bx, by);
          ctx!.stroke();
        }

        if (cursor.active) {
          const dist = Math.hypot(ax - cursor.x, ay - cursor.y);
          if (dist < CURSOR_DISTANCE) {
            const strength = (1 - dist / CURSOR_DISTANCE) * av;
            ctx!.strokeStyle = `rgba(${accentRgb}, ${0.5 * strength})`;
            ctx!.lineWidth = 1.1;
            ctx!.beginPath();
            ctx!.moveTo(ax, ay);
            ctx!.lineTo(cursor.x, cursor.y);
            ctx!.stroke();
          }
        }
      }

      for (const p of points) {
        const px = p.x + lean.x;
        const py = p.y + lean.y;
        const vis = visibility(px, py);
        if (vis <= 0) continue;
        const near = cursor.active
          ? Math.max(0, 1 - Math.hypot(px - cursor.x, py - cursor.y) / CURSOR_DISTANCE)
          : 0;
        // A point reddens as the cursor nears it, so the pointer feels like it
        // is touching the network rather than floating over it.
        ctx!.fillStyle =
          near > 0
            ? `rgba(${accentRgb}, ${(0.35 + 0.5 * near) * vis})`
            : `rgba(${inkRgb}, ${0.34 * vis})`;
        ctx!.beginPath();
        ctx!.arc(px, py, p.r + near * 1.1, 0, Math.PI * 2);
        ctx!.fill();
      }
    }

    function loop() {
      draw();
      frame = requestAnimationFrame(loop);
    }

    function onPointerMove(e: PointerEvent) {
      const rect = canvas!.getBoundingClientRect();
      cursor.x = e.clientX - rect.left;
      cursor.y = e.clientY - rect.top;
      cursor.active = true;
    }

    function onPointerLeave() {
      cursor.active = false;
      cursor.x = -9999;
      cursor.y = -9999;
    }

    resize();

    // Both, deliberately. ResizeObserver catches layout changes that leave the
    // window alone; the window listener covers it when ResizeObserver does not
    // deliver at all, which is exactly what happens in some embedded webviews.
    // Whichever fires, resize() is idempotent.
    const observer = new ResizeObserver(resize);
    observer.observe(canvas);
    window.addEventListener('resize', resize);

    if (still) {
      // One static frame: the constellation is still decoration, it just holds
      // its pose.
      draw();
    } else {
      // pointermove covers mouse, pen and touch-drag from one listener.
      window.addEventListener('pointermove', onPointerMove, { passive: true });
      window.addEventListener('pointerleave', onPointerLeave, { passive: true });
      frame = requestAnimationFrame(loop);
    }

    return () => {
      cancelAnimationFrame(frame);
      observer.disconnect();
      window.removeEventListener('resize', resize);
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('pointerleave', onPointerLeave);
    };
  }, []);

  return <canvas ref={canvasRef} className="particle-network" aria-hidden="true" />;
}
