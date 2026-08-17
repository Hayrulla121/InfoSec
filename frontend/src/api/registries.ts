import { api } from './client';

/** Mirrors Spring Data's Page<T> JSON. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
}

export interface Asset {
  id: number;
  code: string;
  name: string;
  scope: string | null;
  infoCategory: string | null;
  criticality: string;
  criticalityRating: number;
  securityClass: string | null;
  infoSystemId: number | null;
  infoSystemName: string | null;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface Threat {
  id: number;
  code: string;
  description: string;
  discoverability: number;
  repeatability: number;
  exploitability: number;
  affectedUsers: number;
  damage: number;
  totalScore: number;
  rating: number;
  levelLabel: string;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface Control {
  id: number;
  code: string;
  name: string;
  description: string | null;
  treatmentMethod: string;
  reductionPct: number;
  implemented: boolean;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
}

/**
 * One generic CRUD client, instantiated per resource.
 *
 * Four registries share exactly this shape, so writing it once means a change
 * to paging or error handling happens in a single place.
 */
function crud<TResponse, TRequest>(basePath: string) {
  return {
    async list(query: PageQuery = {}): Promise<Page<TResponse>> {
      const { data } = await api.get<Page<TResponse>>(basePath, {
        params: {
          page: query.page ?? 0,
          size: query.size ?? 20,
          sort: query.sort,
          // Omit an empty search rather than sending search=""
          search: query.search || undefined,
        },
      });
      return data;
    },
    async get(id: number): Promise<TResponse> {
      const { data } = await api.get<TResponse>(`${basePath}/${id}`);
      return data;
    },
    async create(body: TRequest): Promise<TResponse> {
      const { data } = await api.post<TResponse>(basePath, body);
      return data;
    },
    async update(id: number, body: TRequest): Promise<TResponse> {
      const { data } = await api.put<TResponse>(`${basePath}/${id}`, body);
      return data;
    },
    async remove(id: number): Promise<void> {
      await api.delete(`${basePath}/${id}`);
    },
  };
}

export interface AssetRequest {
  name: string;
  scope?: string | null;
  infoCategory?: string | null;
  criticality: string;
  securityClass?: string | null;
  infoSystemId?: number | null;
}

export interface ThreatRequest {
  description: string;
  discoverability: number;
  repeatability: number;
  exploitability: number;
  affectedUsers: number;
  damage: number;
}

export interface ControlRequest {
  name: string;
  description?: string | null;
  treatmentMethod: string;
  reductionPct: number;
  implemented: boolean;
}

export const assetsApi = crud<Asset, AssetRequest>('/api/assets');
export const threatsApi = crud<Threat, ThreatRequest>('/api/threats');
export const controlsApi = crud<Control, ControlRequest>('/api/controls');
