import { api } from './client';

export interface MatrixCell {
  assetRating: number;
  threatRating: number;
  /** null = no risks; rendered as an empty cell, like the workbook. */
  count: number | null;
  riskLevel: number;
  riskLabel: string;
}

export interface MatrixLegendItem {
  value: number;
  labelUz: string;
  labelRu: string;
}

export interface RiskMatrix {
  assetRatings: number[];
  threatRatings: number[];
  cells: MatrixCell[];
  totalRisks: number;
  assetLegend: MatrixLegendItem[];
  threatLegend: MatrixLegendItem[];
  riskLegend: MatrixLegendItem[];
}

export interface AssetGauge {
  assetId: number;
  code: string;
  name: string;
  criticality: string;
  criticalityRating: number;
  riskCount: number;
  worstCurrentLevel: number | null;
  worstCurrentLabel: string | null;
  worstResidualLevel: number | null;
  worstResidualLabel: string | null;
}

export interface LevelCount {
  level: number;
  label: string;
  count: number;
}

/** One month of the remediation plan. Counts are cumulative. */
export interface TimelinePoint {
  /** ISO yyyy-MM. */
  month: string;
  dueTotal: number;
  doneTotal: number;
}

export interface NamedCount {
  label: string;
  count: number;
}

export interface Dashboard {
  totalRisks: number;
  totalAssets: number;
  totalThreats: number;
  totalControls: number;
  currentDistribution: LevelCount[];
  residualDistribution: LevelCount[];
  /** Level with no controls applied — the baseline of the reduction chart. */
  inherentDistribution: LevelCount[];
  implementedControlLinks: number;
  plannedControlLinks: number;
  implementedPercent: number;
  overdueMeasures: number;
  assetGauges: AssetGauge[];
  remediationTimeline: TimelinePoint[];
  treatmentBreakdown: NamedCount[];
  statusBreakdown: NamedCount[];
}

export async function getRiskMatrix(): Promise<RiskMatrix> {
  const { data } = await api.get<RiskMatrix>('/api/risk-matrix');
  return data;
}

export async function getDashboard(): Promise<Dashboard> {
  const { data } = await api.get<Dashboard>('/api/dashboard');
  return data;
}
