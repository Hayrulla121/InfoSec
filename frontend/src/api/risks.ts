import { api } from './client';
import type { Page } from './registries';

export type ControlType = 'IMPLEMENTED' | 'PLANNED';

export interface RiskStage {
  score: number | null;
  threatRating: number | null;
  riskLevel: number | null;
  riskLabel: string | null;
}

export interface RiskControlLink {
  linkId: number;
  controlId: number;
  controlCode: string;
  controlName: string;
  treatmentMethod: string;
  reductionPct: number;
  controlType: ControlType;
  applyOrder: number;
}

export interface Risk {
  id: number;
  code: string;
  assetId: number;
  assetCode: string;
  assetName: string;
  assetRating: number;
  threatId: number;
  threatCode: string;
  threatDescription: string;
  threatTotalScore: number;
  name: string;
  indicators: string | null;
  owner: string | null;
  treatmentMethod: string | null;
  measureStatus: string | null;
  implementationDeadline: string | null;
  comment: string | null;
  inherent: RiskStage;
  current: RiskStage;
  residual: RiskStage;
  implementedControls: RiskControlLink[];
  plannedControls: RiskControlLink[];
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface RiskRequest {
  assetId: number;
  threatId: number;
  name: string;
  indicators?: string | null;
  owner?: string | null;
  treatmentMethod?: string | null;
  measureStatus?: string | null;
  implementationDeadline?: string | null;
  comment?: string | null;
}

export interface RiskQuery {
  page?: number;
  size?: number;
  search?: string;
  /** Set by the risk-matrix drill-down. */
  assetRating?: number;
  threatRating?: number;
}

export const risksApi = {
  async list(query: RiskQuery = {}): Promise<Page<Risk>> {
    const { data } = await api.get<Page<Risk>>('/api/risks', {
      params: {
        page: query.page ?? 0,
        size: query.size ?? 20,
        search: query.search || undefined,
        assetRating: query.assetRating,
        threatRating: query.threatRating,
      },
    });
    return data;
  },
  async get(id: number): Promise<Risk> {
    const { data } = await api.get<Risk>(`/api/risks/${id}`);
    return data;
  },
  async create(body: RiskRequest): Promise<Risk> {
    const { data } = await api.post<Risk>('/api/risks', body);
    return data;
  },
  async update(id: number, body: RiskRequest): Promise<Risk> {
    const { data } = await api.put<Risk>(`/api/risks/${id}`, body);
    return data;
  },
  async remove(id: number): Promise<void> {
    await api.delete(`/api/risks/${id}`);
  },
  /** Both of these return the recalculated risk, so no refetch is needed. */
  async attachControl(riskId: number, controlId: number, type: ControlType): Promise<Risk> {
    const { data } = await api.post<Risk>(`/api/risks/${riskId}/controls`, { controlId, type });
    return data;
  },
  async detachControl(riskId: number, linkId: number): Promise<Risk> {
    const { data } = await api.delete<Risk>(`/api/risks/${riskId}/controls/${linkId}`);
    return data;
  },
};
