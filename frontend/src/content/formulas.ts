import type { FormulaSpec, FormulaStep } from '../components/Formula';
import type { Dictionary } from '../i18n/ru';
import type { Threat } from '../api/registries';
import type { Risk, RiskControlLink, RiskStage } from '../api/risks';
import type { AssetGauge, Dashboard, MatrixCell } from '../api/analytics';

/**
 * Builders that turn an API payload into a displayable explanation.
 *
 * The rule every builder here follows: **the result comes from the server**.
 * These functions arrange inputs and quote the formula, they never evaluate it.
 * The one exception is {@link branchNote}, which identifies WHICH branch of the
 * classification algorithm fired - and it re-checks itself against the server's
 * answer before saying anything, so it can only ever be silent, never wrong.
 */

type F = Dictionary['formula'];
/** Translates a stored Russian label into the display language. */
type Translate = (label: string | null | undefined) => string;
/** The five DREAD criteria under their full names, from the threats dictionary. */
export type DreadNames = {
  discoverability: string;
  repeatability: string;
  exploitability: string;
  affectedUsers: string;
  damage: string;
};

/**
 * Rating 1..5 -> the Russian threat-level word the server stores, which the
 * `threat()` translator then renders in the display language. Kept here rather
 * than fetched because the ladder needs all five rungs, not just the one the
 * current threat sits on.
 */
const LEVEL_WORDS = ['', 'Незначительный', 'Низкий', 'Средний', 'Высокий', 'Очень высокий'];

/** The same idea for asset criticality, used by the matrix's bare coordinates. */
const ASSET_WORDS = ['', 'Очень низкая', 'Низкая', 'Средняя', 'Высокая', 'Критичная'];

// ---------------------------------------------------------------- formatting

/**
 * Scores are stored with four decimals, but "10.4000" reads like precision
 * that isn't there. Trailing zeros go; genuine decimals stay.
 */
export function num(value: number | string | null | undefined): string {
  if (value === null || value === undefined) return '—';
  const n = typeof value === 'string' ? Number(value) : value;
  if (Number.isNaN(n)) return String(value);
  return String(Number(n.toFixed(4)));
}

/** 0.2 -> "20%". Two decimals is the column's precision, so 0.125 -> "12.5%". */
export function pct(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—';
  return `${Number((value * 100).toFixed(2))}%`;
}

// ------------------------------------------------------------ classification

/**
 * The DREAD ladder, exactly as the workbook's Ma'lumot sheet tabulates it.
 *
 * Marking the active rung is a range comparison on a number the server already
 * sent, not a re-derivation: the rung is cross-checked against the server's own
 * rating below, and disagreement simply leaves nothing marked.
 */
const DREAD_BANDS: { range: string; lo: number; hi: number; value: number }[] = [
  { range: '0–5', lo: 0, hi: 5, value: 1 },
  { range: '6–10', lo: 6, hi: 10, value: 2 },
  { range: '11–15', lo: 11, hi: 15, value: 3 },
  { range: '16–20', lo: 16, hi: 20, value: 4 },
  { range: '21–25', lo: 21, hi: 25, value: 5 },
];

/**
 * Names the branch of the a x t algorithm that produced a level.
 *
 * Returns undefined when the branch it derives disagrees with the level the
 * server sent. That can only happen if the two implementations have drifted -
 * in which case the honest thing is to show the inputs and the answer without
 * a story tying them together, rather than to narrate a rule that did not fire.
 */
function branchNote(
  f: F,
  a: number,
  t: number,
  serverLevel: number,
  assetWord: string,
  threatWord: string,
): string | undefined {
  const p = a * t;
  let derived: number;
  let note: string;

  if (p >= 20) {
    derived = 5;
    note = f.whyCritical(assetWord, threatWord);
  } else if ((a === 1 && t < 3) || (t === 1 && a < 4)) {
    derived = 1;
    note = f.whyNegligible(assetWord, threatWord);
  } else if (t > 2 && p >= 10) {
    derived = 4;
    note = f.whyHigh(assetWord, threatWord);
  } else if ((t < 4 && p > 3 && p < 6) || (t === 3 && a < 3)) {
    derived = 2;
    note = f.whyLow(assetWord, threatWord);
  } else {
    derived = 3;
    note = f.whyMedium(assetWord, threatWord);
  }

  return derived === serverLevel ? note : undefined;
}

/**
 * The shared body of every "level = classify(a, t)" explanation.
 *
 * Note what is NOT here any more: a transcription of the nested IF. It was the
 * headline of this card and it was the thing nobody could read - five
 * conditions and two single-letter variables, with the variables only defined
 * further down. The rule is now shown three ways, none of them symbolic: a
 * sentence, the grid with your cell marked, and a spoken reason. The literal
 * formula moved to the Excel disclosure at the bottom.
 */
function levelSpec(
  f: F,
  level: Translate,
  opts: {
    title: string;
    source: string;
    a: number;
    t: number;
    /** Criticality in words, e.g. "Критичная". */
    assetWord: string;
    /** Threat level in words, e.g. "Низкий". */
    threatWord: string;
    riskLevel: number;
    riskLabel: string | null;
    extraNote?: string;
  },
): FormulaSpec {
  const { a, t, riskLevel, assetWord, threatWord } = opts;
  const branch = branchNote(f, a, t, riskLevel, assetWord, threatWord);
  return {
    title: opts.title,
    source: opts.source,
    lead: f.riskLevelLead,
    inputs: [
      { label: f.riskLevelAsset, value: String(a), word: assetWord },
      { label: f.riskLevelThreat, value: String(t), word: threatWord },
    ],
    matrix: { assetRating: a, threatRating: t, level: riskLevel },
    result: `${level(opts.riskLabel)} (${riskLevel})`,
    resultLevel: riskLevel,
    note: [branch, opts.extraNote].filter(Boolean).join(' ') || undefined,
    excel: f.riskLevelExcel,
  };
}

// ------------------------------------------------------------------- assets

export function assetRatingFormula(
  f: F,
  criticality: Translate,
  asset: { criticality: string; criticalityRating: number },
): FormulaSpec {
  return {
    title: f.assetRatingTitle,
    source: `${f.srcAssets} · ${f.column('H')}`,
    lead: f.assetRatingLead,
    inputs: [
      {
        label: f.assetRatingInput,
        value: String(asset.criticalityRating),
        word: criticality(asset.criticality),
      },
    ],
    result: String(asset.criticalityRating),
    excel: f.assetRatingExcel,
  };
}

// ------------------------------------------------------------------ threats

export function threatSumFormula(f: F, names: DreadNames, threat: Threat): FormulaSpec {
  const parts: [string, number][] = [
    [names.discoverability, threat.discoverability],
    [names.repeatability, threat.repeatability],
    [names.exploitability, threat.exploitability],
    [names.affectedUsers, threat.affectedUsers],
    [names.damage, threat.damage],
  ];
  return {
    title: f.dreadSumTitle,
    source: `${f.srcThreats} · ${f.column('Q')}`,
    lead: f.dreadSumLead,
    // Each criterion by its full name. The workbook's single-letter headers
    // (О П Э М У) fit a spreadsheet column; they do not explain anything.
    inputs: parts.map(([label, value]) => ({ label, value: String(value) })),
    steps: [
      { expr: parts.map(([, v]) => v).join(' + '), value: String(threat.totalScore) },
    ],
    result: String(threat.totalScore),
    excel: f.dreadSumExcel,
  };
}

export function threatRatingFormula(f: F, threatWord: Translate, threat: Threat): FormulaSpec {
  return {
    title: f.dreadRatingTitle,
    source: `${f.srcThreats} · ${f.column('R')}`,
    lead: f.dreadRatingLead,
    inputs: [{ label: f.dreadSumInput, value: String(threat.totalScore) }],
    // The ladder, with the rung the sum landed on marked. Reading five ranges
    // top to bottom takes a second; parsing IF(Q2<6;1;IF(Q2<11;2;...)) does not.
    bands: DREAD_BANDS.map((b) => ({
      range: b.range,
      label: threatWord(LEVEL_WORDS[b.value]),
      value: b.value,
      // Marked from the SERVER's rating, not from where the sum falls, so the
      // highlight can never contradict the answer printed below it.
      active: b.value === threat.rating,
    })),
    result: `${threatWord(threat.levelLabel)} (${threat.rating})`,
    resultLevel: threat.rating,
    excel: f.dreadRatingExcel,
  };
}

// -------------------------------------------------------------------- risks

/** The reduction chain for one set of links, replayed from the server's steps. */
export function chainFormula(
  f: F,
  opts: {
    kind: 'IMPLEMENTED' | 'PLANNED';
    links: RiskControlLink[];
    baseScore: number | null;
    finalScore: number | null;
  },
): FormulaSpec {
  const implemented = opts.kind === 'IMPLEMENTED';
  const steps: FormulaStep[] = opts.links.map((l) => ({
    expr: `${num(l.scoreBefore)} − ${num(l.scoreBefore)} × ${pct(l.reductionPct)}`,
    value: num(l.scoreAfter),
    note: `${l.controlCode} · ${l.controlName}`,
  }));

  return {
    title: implemented ? f.chainTitleImplemented : f.chainTitlePlanned,
    source: `${f.srcRisks} · ${f.column(implemented ? 'AQ:AW' : 'BC:BG')}`,
    lead: implemented ? f.chainLeadImplemented : f.chainLeadPlanned,
    inputs: [
      {
        label: implemented ? f.chainBase : f.chainBaseCurrent,
        value: num(opts.baseScore),
      },
    ],
    steps,
    result: num(opts.finalScore),
    note: steps.length === 0 ? f.chainNone : f.chainNote,
    excel: implemented ? f.chainExcelImplemented : f.chainExcelPlanned,
  };
}

/** Level of one stage of one risk. */
export function riskStageFormula(
  f: F,
  level: Translate,
  threatWord: Translate,
  criticality: Translate,
  risk: Risk,
  which: 'inherent' | 'current' | 'residual',
): FormulaSpec | null {
  const stage: RiskStage = risk[which];
  // `== null` catches undefined as well as null: a client running against a
  // server that predates these fields must fall back to showing no hint, never
  // to rendering "a x t = 5 x undefined".
  if (stage.riskLevel == null || stage.threatRating == null) return null;

  const titles = {
    inherent: f.inherentTitle,
    current: f.currentTitle,
    residual: f.residualTitle,
  } as const;
  const columns = { inherent: 'AI', current: 'BW', residual: 'BU' } as const;

  return levelSpec(f, level, {
    title: titles[which],
    source: `${f.srcRisks} · ${f.column(columns[which])}`,
    a: risk.assetRating,
    t: stage.threatRating,
    assetWord: criticality(risk.assetCriticality),
    threatWord: threatWord(stage.threatLabel ?? LEVEL_WORDS[stage.threatRating]),
    riskLevel: stage.riskLevel,
    riskLabel: stage.riskLabel,
  });
}

// ---------------------------------------------------------------- dashboard

/**
 * Why the needle sits where it does.
 *
 * This is the hint the whole feature exists for: a card headed "КИА2 ·
 * Критичная" with a needle on "Средний" looks wrong until you can see that the
 * asset rating is only one of the two operands.
 */
export function gaugeFormula(
  f: F,
  level: Translate,
  threatWord: Translate,
  criticality: Translate,
  gauge: AssetGauge,
): FormulaSpec {
  if (gauge.worstCurrentLevel == null || gauge.worstCurrentThreatRating == null) {
    return {
      title: f.gaugeTitle,
      source: f.srcMatrix,
      lead: f.riskLevelLead,
      inputs: [
        {
          label: f.riskLevelAsset,
          value: String(gauge.criticalityRating),
          word: criticality(gauge.criticality),
        },
      ],
      result: '—',
      note: f.gaugeNoRisks,
    };
  }

  return levelSpec(f, level, {
    title: f.gaugeTitle,
    source: `${f.srcRisks} · ${f.column('BW')}`,
    a: gauge.criticalityRating,
    t: gauge.worstCurrentThreatRating,
    assetWord: criticality(gauge.criticality),
    threatWord: threatWord(
      gauge.worstCurrentThreatLabel ?? LEVEL_WORDS[gauge.worstCurrentThreatRating],
    ),
    riskLevel: gauge.worstCurrentLevel,
    riskLabel: gauge.worstCurrentLabel,
    extraNote: `${f.gaugeNote(gauge.riskCount)} ${f.gaugeWhySame}`,
  });
}

export function implementedPercentFormula(f: F, d: Dashboard): FormulaSpec {
  const total = d.implementedControlLinks + d.plannedControlLinks;
  return {
    title: f.percentTitle,
    source: f.srcRisks,
    lead: f.percentLead,
    inputs: [
      { label: f.percentImplemented, value: String(d.implementedControlLinks) },
      { label: f.percentPlanned, value: String(d.plannedControlLinks) },
      { label: f.percentTotal, value: String(total) },
    ],
    steps: [
      {
        expr: `${d.implementedControlLinks} ÷ ${total} × 100`,
        value: `${d.implementedPercent}%`,
      },
    ],
    result: `${d.implementedPercent}%`,
    excel: f.percentExcel,
  };
}

export function overdueFormula(f: F, d: Dashboard, today: string): FormulaSpec {
  return {
    title: f.overdueTitle,
    source: `${f.srcRisks} · ${f.column('O / N')}`,
    lead: f.overdueLead,
    inputs: [{ label: f.overdueToday, value: today }],
    result: String(d.overdueMeasures),
    note: f.overdueNote,
    excel: f.overdueExcel,
  };
}

// ------------------------------------------------------------------- matrix

export function matrixCellFormula(
  f: F,
  level: Translate,
  threatWord: Translate,
  criticality: Translate,
  cell: MatrixCell,
): FormulaSpec {
  const spec = levelSpec(f, level, {
    title: f.matrixCellTitle,
    source: `${f.srcMatrix} · ${f.column('C2:G6')}`,
    a: cell.assetRating,
    t: cell.threatRating,
    assetWord: criticality(ASSET_WORDS[cell.assetRating]),
    threatWord: threatWord(LEVEL_WORDS[cell.threatRating]),
    riskLevel: cell.riskLevel,
    riskLabel: cell.riskLabel,
  });

  // The colour is a property of the coordinates; the number in the cell is a
  // separate COUNTIFS. Both belong in the hint, so show them together.
  return {
    ...spec,
    inputs: [
      ...spec.inputs,
      { label: f.matrixCount, value: cell.count == null ? f.matrixEmpty : String(cell.count) },
    ],
  };
}
