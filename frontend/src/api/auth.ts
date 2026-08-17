import { api } from './client';
import type { LoginResponse } from './types';

export async function login(username: string, password: string): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/api/auth/login', { username, password });
  return data;
}

/** Restores the session on page reload using the stored token. */
export async function me(): Promise<LoginResponse> {
  const { data } = await api.get<LoginResponse>('/api/auth/me');
  return data;
}
