/** Mirrors uz.infosec.risk.domain.AppModule */
export type AppModule =
  | 'ASSETS'
  | 'THREATS'
  | 'RISKS'
  | 'CONTROLS'
  | 'RISK_CONTROLS'
  | 'DICTIONARIES'
  | 'INFO_SYSTEMS';

export const APP_MODULES: AppModule[] = [
  'ASSETS',
  'THREATS',
  'RISKS',
  'CONTROLS',
  'RISK_CONTROLS',
  'DICTIONARIES',
  'INFO_SYSTEMS',
];

export type Action = 'CREATE' | 'READ' | 'UPDATE' | 'DELETE';

export type Role = 'ADMIN' | 'USER';

export interface User {
  id: number;
  username: string;
  fullName: string;
  email: string | null;
  role: Role;
  active: boolean;
  createdAt: string | null;
}

export interface ModulePermission {
  module: AppModule;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
}

export interface LoginResponse {
  token: string | null;
  user: User;
  permissions: ModulePermission[];
}
