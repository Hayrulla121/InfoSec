/**
 * Where the radar scope sits on the login screen.
 *
 * Two canvases need this answer: the radar draws itself here, and the particle
 * field keeps out of here. Deriving it twice from the same inputs would be two
 * chances to disagree — and a disagreement shows up as particles crawling over
 * the shield, which is precisely what the keep-out exists to prevent. So it is
 * computed once, in one function, and both layers call it.
 */

export interface Scope {
  /** Centre in CSS pixels, relative to the canvas. */
  cx: number;
  cy: number;
  /** Half-height unit the shield is drawn in. */
  scale: number;
  /** Radius of the outer bezel ring. */
  radius: number;
}

/** Below this much free space beside the card, the scope centres on the viewport. */
const MIN_FREE_WIDTH = 300;

export function scopeGeometry(
  canvas: HTMLCanvasElement,
  card: HTMLElement | null,
  width: number,
  height: number,
): Scope {
  const cardRect = card?.getBoundingClientRect();
  const canvasRect = canvas.getBoundingClientRect();
  const freeWidth = cardRect ? cardRect.left - canvasRect.left : width;

  // Claim the gap beside the card when there is one; otherwise fall back to the
  // whole viewport and let the card sit over the middle of the scope.
  const usable = freeWidth > MIN_FREE_WIDTH ? freeWidth : width;
  const scale = Math.min((usable * 0.6) / 1.72, (height * 0.6) / 2.16);

  return { cx: usable / 2, cy: height / 2, scale, radius: scale * 1.35 };
}
