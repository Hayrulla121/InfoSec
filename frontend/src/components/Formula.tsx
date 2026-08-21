import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useI18n } from '../i18n/I18nContext';

/**
 * The "how was this number calculated?" hint.
 *
 * Every figure in this application that is derived rather than typed gets one
 * of these next to it. The workbook this platform replaced made its arithmetic
 * inspectable by definition - you clicked a cell and read the formula bar - and
 * losing that was the single biggest thing users gave up in the move to a
 * database. This puts it back.
 *
 * <b>The hint never computes anything.</b> Every value it shows arrives from
 * the API already calculated; the component only lays the inputs, the formula
 * and the answer out next to each other. That is deliberate: a tooltip that
 * re-derived the result in TypeScript would be a second implementation of the
 * risk engine, free to drift from the Java one, and the first symptom would be
 * a tooltip confidently explaining a number the page does not show.
 *
 * <b>It opens on click, never on hover.</b> These figures live in dense tables
 * and a 5x5 grid, so a pointer crossing the page passes over many of them; on
 * hover the card kept appearing over whatever the reader was actually looking
 * at. A click is unambiguous, behaves the same on touch, and leaves the card
 * open long enough to read a paragraph and select text out of it.
 */

/** One line of worked arithmetic, e.g. the effect of a single control. */
export interface FormulaStep {
  /** Left-hand side as written out, e.g. "13 − 13 × 20%". */
  expr: string;
  /** What that expression evaluates to. */
  value: string;
  /** Optional caption - which control, which stage. */
  note?: string;
}

export interface FormulaInput {
  label: string;
  /** The number. */
  value: string;
  /**
   * The same thing in words - "Критичная" beside the 5.
   *
   * A bare rating is jargon: 5 means nothing until you know it is Критичная.
   * Every input that HAS a word form shows both, so the card can be read
   * without first learning the scale.
   */
  word?: string;
}

/** Where a pair of ratings lands on the workbook's 5x5 grid. */
export interface FormulaMatrix {
  assetRating: number;
  threatRating: number;
  /** Level of the highlighted cell - from the server, never derived here. */
  level: number;
}

/** One rung of a threshold ladder, e.g. the DREAD sum bands. */
export interface FormulaBand {
  /** "11–15" */
  range: string;
  /** "Средний" */
  label: string;
  /** 1..5 */
  value: number;
  /** True for the rung the actual value fell into. */
  active: boolean;
}

export interface FormulaSpec {
  /** What is being computed. */
  title: string;
  /** Where it comes from in the original workbook. */
  source: string;
  /**
   * What happens, in a sentence of ordinary language.
   *
   * This replaced a transcription of the nested IF. Symbolic notation is the
   * most precise way to state the rule and the worst way to READ it: it forces
   * you to hold five conditions and two single-letter variables in your head
   * before anything means anything. The rule is still shown - as the grid
   * below, and verbatim in the Excel disclosure - but the headline is a
   * sentence.
   */
  lead: string;
  /** The named values actually substituted in. */
  inputs: FormulaInput[];
  /** Renders the 5x5 grid with one cell ringed. */
  matrix?: FormulaMatrix;
  /** Renders a threshold ladder with the matching rung marked. */
  bands?: FormulaBand[];
  /** Optional worked chain, shown between inputs and result. */
  steps?: FormulaStep[];
  /** The answer - the same value the page is displaying. */
  result: string;
  /** 1..5, colours the result chip to match the badges. */
  resultLevel?: number | null;
  /** Optional closing sentence, in plain language. */
  note?: string;
  /** The verbatim workbook formula, folded away for anyone auditing. */
  excel?: string;
}

/**
 * The 5x5 grid, with the cell this value landed on ringed.
 *
 * Only that one cell is coloured. Colouring all 25 would mean evaluating
 * classify(a, t) twenty-five times in the browser - a second copy of the risk
 * engine, exactly what this component refuses to be. The ringed position plus
 * the axis labels already answer "where am I?", which is the question.
 */
function MiniMatrix({
  matrix,
  assetAxis,
  threatAxis,
  hereLabel,
}: {
  matrix: FormulaMatrix;
  assetAxis: string;
  threatAxis: string;
  hereLabel: string;
}) {
  const rows = [5, 4, 3, 2, 1];
  const cols = [1, 2, 3, 4, 5];
  return (
    <div className="formula-matrix">
      <div className="formula-matrix-axis-y">{assetAxis}</div>
      <table>
        <tbody>
          {rows.map((a) => (
            <tr key={a}>
              <th>{a}</th>
              {cols.map((t) => {
                const here = a === matrix.assetRating && t === matrix.threatRating;
                return (
                  <td
                    key={t}
                    className={here ? `is-here level-${matrix.level}` : undefined}
                    aria-label={here ? hereLabel : undefined}
                  />
                );
              })}
            </tr>
          ))}
          <tr>
            <th />
            {cols.map((t) => (
              <th key={t}>{t}</th>
            ))}
          </tr>
        </tbody>
      </table>
      <div className="formula-matrix-axis-x">{threatAxis}</div>
    </div>
  );
}

/** The threshold ladder: five ranges, the matching one marked. */
function Bands({ bands }: { bands: FormulaBand[] }) {
  return (
    <ul className="formula-bands">
      {bands.map((b) => (
        <li key={b.range} className={b.active ? 'is-active' : undefined}>
          <span className="formula-band-range">{b.range}</span>
          <span className="formula-band-arrow" aria-hidden="true">
            →
          </span>
          <span className={b.active ? `formula-band-label level-text-${b.value}` : 'formula-band-label'}>
            {b.label}
          </span>
          <span className="formula-band-value">{b.value}</span>
        </li>
      ))}
    </ul>
  );
}

/** Keeps the card on screen when the trigger sits near an edge. */
const MARGIN = 10;
const CARD_W = 330;

export function FormulaHint({ spec, label }: { spec: FormulaSpec; label: string }) {
  // The chrome around the content (axis captions, the disclosure's summary)
  // belongs to the component, not to each spec builder, so it reads the
  // dictionary itself rather than making every caller thread four more strings.
  const { t } = useI18n();
  const axisAsset = t.formula.matrixAxisAsset;
  const axisThreat = t.formula.matrixAxisThreat;
  const hereLabel = t.formula.matrixYouAreHere;
  const excelToggle = t.formula.excelToggle;

  const [open, setOpen] = useState(false);
  const [pos, setPos] = useState<{ top: number; left: number } | null>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const cardRef = useRef<HTMLDivElement>(null);
  const id = useId();

  /**
   * Anchors the card to its trigger.
   *
   * Positioned against the viewport, not the document: the card is portalled
   * to <body> precisely so a table's overflow:auto cannot clip it, and a fixed
   * element is the only kind that survives that escape unshifted. The flip side
   * is that it has to be re-placed whenever the page scrolls under it.
   */
  const place = useCallback(() => {
    const trigger = triggerRef.current;
    if (!trigger) return;
    const r = trigger.getBoundingClientRect();

    // Scrolled past the thing it explains: there is nothing left to point at.
    if (r.bottom < 0 || r.top > window.innerHeight) {
      setOpen(false);
      return;
    }

    const height = cardRef.current?.offsetHeight ?? 220;

    let left = r.left + r.width / 2 - CARD_W / 2;
    left = Math.max(MARGIN, Math.min(left, window.innerWidth - CARD_W - MARGIN));

    // Prefer below; flip above when there is no room and above has more.
    const below = r.bottom + 8;
    const roomBelow = window.innerHeight - below - MARGIN;
    const top = roomBelow < height && r.top - 8 - height > MARGIN ? r.top - 8 - height : below;

    setPos({ top, left });
  }, []);

  useLayoutEffect(() => {
    if (!open) {
      setPos(null);
      return;
    }
    place();
    // Depends on primitives, never on `spec` itself: the builders construct a
    // fresh object on every render, so an object dependency would re-run this
    // effect each time, and since setPos always allocates a new object React
    // could never bail out of the resulting render loop.
  }, [open, spec.title, spec.result, place]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpen(false);
        triggerRef.current?.focus();
      }
    };
    // Clicking away dismisses, the usual popover contract.
    const onDown = (e: MouseEvent) => {
      const target = e.target as Node;
      if (!cardRef.current?.contains(target) && !triggerRef.current?.contains(target)) {
        setOpen(false);
      }
    };
    // Follow the trigger rather than closing: the card is opened deliberately
    // now, so a scroll to read the row underneath should not throw it away.
    document.addEventListener('keydown', onKey);
    document.addEventListener('mousedown', onDown);
    window.addEventListener('scroll', place, true);
    window.addEventListener('resize', place);
    return () => {
      document.removeEventListener('keydown', onKey);
      document.removeEventListener('mousedown', onDown);
      window.removeEventListener('scroll', place, true);
      window.removeEventListener('resize', place);
    };
  }, [open, place]);

  return (
    <>
      <button
        ref={triggerRef}
        type="button"
        className={`formula-trigger${open ? ' is-open' : ''}`}
        aria-label={label}
        aria-expanded={open}
        aria-controls={open ? id : undefined}
        // Click, not hover. Opening on hover meant the card ambushed anyone
        // whose pointer merely crossed a table cell, and these figures sit in
        // dense grids where the pointer crosses a lot of them. Deliberate
        // action in, deliberate card out. It also makes touch and mouse behave
        // identically, and lets the card's text be selected and read at length.
        onClick={(e) => {
          // The matrix cell underneath navigates on click; this is not that.
          e.stopPropagation();
          setOpen((o) => !o);
        }}
      >
        <span aria-hidden="true">ƒ</span>
      </button>

      {open &&
        createPortal(
          <div
            ref={cardRef}
            id={id}
            role="group"
            aria-label={label}
            className="formula-card"
            style={{ top: pos?.top ?? -9999, left: pos?.left ?? -9999, width: CARD_W }}
          >
            <div className="formula-head">
              <span className="formula-title">{spec.title}</span>
              <span className="formula-source">{spec.source}</span>
            </div>

            <p className="formula-lead">{spec.lead}</p>

            {spec.inputs.length > 0 && (
              <dl className="formula-inputs">
                {spec.inputs.map((i) => (
                  <div className="formula-input" key={i.label}>
                    <dt>{i.label}</dt>
                    <dd>
                      {i.word && <span className="formula-input-word">{i.word}</span>}
                      <span className="formula-input-value">{i.value}</span>
                    </dd>
                  </div>
                ))}
              </dl>
            )}

            {spec.matrix && (
              <MiniMatrix
                matrix={spec.matrix}
                assetAxis={axisAsset}
                threatAxis={axisThreat}
                hereLabel={hereLabel}
              />
            )}

            {spec.bands && <Bands bands={spec.bands} />}

            {spec.steps && spec.steps.length > 0 && (
              <ol className="formula-steps">
                {spec.steps.map((s, idx) => (
                  <li key={idx}>
                    <div className="formula-step-line">
                      <span className="formula-step-expr">{s.expr}</span>
                      <span className="formula-step-value">{s.value}</span>
                    </div>
                    {s.note && <div className="formula-step-note">{s.note}</div>}
                  </li>
                ))}
              </ol>
            )}

            <div className="formula-result">
              <span className="formula-result-label">=</span>
              <span
                className={
                  spec.resultLevel
                    ? `formula-result-value level-text-${spec.resultLevel}`
                    : 'formula-result-value'
                }
              >
                {spec.result}
              </span>
            </div>

            {spec.note && <div className="formula-note">{spec.note}</div>}

            {/* The literal workbook formula, for whoever is reconciling this
                screen against the spreadsheet. Folded away: it is reference
                material for a minority, and putting it in the body was what
                made these cards unreadable for everyone else. */}
            {spec.excel && (
              <details className="formula-excel">
                <summary>{excelToggle}</summary>
                <pre>{spec.excel}</pre>
              </details>
            )}
          </div>,
          document.body,
        )}
    </>
  );
}
