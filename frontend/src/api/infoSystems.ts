import { api } from './client';
import { activeFilters, type Facets, type Page, type PageQuery } from './registries';

export interface InfoSystem {
  id: number;
  code: string;
  name: string;
  description: string | null;
  hosting: string | null;
  usagePurpose: string | null;
  dataFormat: string | null;
  confidentiality: string | null;
  integrity: string | null;
  availability: string | null;
  updateFrequency: string | null;
  usersInfo: string | null;
  owner: string | null;
  createdAt: string | null;
  createdBy: string | null;
  updatedAt: string | null;
  updatedBy: string | null;
}

export interface InfoSystemRequest {
  name: string;
  description?: string | null;
  hosting?: string | null;
  usagePurpose?: string | null;
  dataFormat?: string | null;
  confidentiality?: string | null;
  integrity?: string | null;
  availability?: string | null;
  updateFrequency?: string | null;
  usersInfo?: string | null;
  owner?: string | null;
}

export const infoSystemsApi = {
  async list(query: PageQuery = {}): Promise<Page<InfoSystem>> {
    const { data } = await api.get<Page<InfoSystem>>('/api/info-systems', {
      params: {
        page: query.page ?? 0,
        size: query.size ?? 20,
        search: query.search || undefined,
        ...activeFilters(query.filters),
      },
    });
    return data;
  },
  async facets(): Promise<Facets> {
    const { data } = await api.get<Facets>('/api/info-systems/facets');
    return data;
  },
  async create(body: InfoSystemRequest): Promise<InfoSystem> {
    const { data } = await api.post<InfoSystem>('/api/info-systems', body);
    return data;
  },
  async update(id: number, body: InfoSystemRequest): Promise<InfoSystem> {
    const { data } = await api.put<InfoSystem>(`/api/info-systems/${id}`, body);
    return data;
  },
  async remove(id: number): Promise<void> {
    await api.delete(`/api/info-systems/${id}`);
  },
};
