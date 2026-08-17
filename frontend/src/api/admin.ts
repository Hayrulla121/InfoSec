import { api } from './client';
import type { ModulePermission, Role, User } from './types';

export interface CreateUserRequest {
  username: string;
  password: string;
  fullName: string;
  email?: string;
  role: Role;
}

export interface UpdateUserRequest {
  fullName?: string;
  email?: string;
  role?: Role;
  active?: boolean;
  newPassword?: string;
}

export async function listUsers(): Promise<User[]> {
  const { data } = await api.get<User[]>('/api/admin/users');
  return data;
}

export async function createUser(request: CreateUserRequest): Promise<User> {
  const { data } = await api.post<User>('/api/admin/users', request);
  return data;
}

export async function updateUser(id: number, request: UpdateUserRequest): Promise<User> {
  const { data } = await api.put<User>(`/api/admin/users/${id}`, request);
  return data;
}

export async function getUserPermissions(id: number): Promise<ModulePermission[]> {
  const { data } = await api.get<ModulePermission[]>(`/api/admin/users/${id}/permissions`);
  return data;
}

export async function updateUserPermissions(
  id: number,
  permissions: ModulePermission[],
): Promise<ModulePermission[]> {
  const { data } = await api.put<ModulePermission[]>(`/api/admin/users/${id}/permissions`, {
    permissions,
  });
  return data;
}
