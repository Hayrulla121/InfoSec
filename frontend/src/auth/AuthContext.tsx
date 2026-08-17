import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { tokenStore } from '../api/client';
import * as authApi from '../api/auth';
import type { Action, AppModule, ModulePermission, User } from '../api/types';

interface AuthState {
  user: User | null;
  permissions: ModulePermission[];
  /** True until the initial "do we have a valid stored token?" check finishes. */
  initialising: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  /** UI-only convenience: should this button be visible? */
  can: (module: AppModule, action: Action) => boolean;
  isAdmin: boolean;
}

/**
 * React Context = one value made available to a whole subtree without passing
 * it through every intermediate component as a prop ("prop drilling").
 */
const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [permissions, setPermissions] = useState<ModulePermission[]>([]);
  const [initialising, setInitialising] = useState(true);

  // On first mount, if a token is already in localStorage, ask the server who
  // it belongs to. This is what keeps you logged in across a page refresh -
  // and it also silently discards tokens the server no longer accepts.
  useEffect(() => {
    if (!tokenStore.get()) {
      setInitialising(false);
      return;
    }
    authApi
      .me()
      .then((res) => {
        setUser(res.user);
        setPermissions(res.permissions);
      })
      .catch(() => {
        tokenStore.clear();
      })
      .finally(() => setInitialising(false));
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const res = await authApi.login(username, password);
    if (res.token) {
      tokenStore.set(res.token);
    }
    setUser(res.user);
    setPermissions(res.permissions);
  }, []);

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
    setPermissions([]);
  }, []);

  const can = useCallback(
    (module: AppModule, action: Action) => {
      if (user?.role === 'ADMIN') return true;
      const p = permissions.find((x) => x.module === module);
      if (!p) return false;
      switch (action) {
        case 'CREATE':
          return p.canCreate;
        case 'READ':
          return p.canRead;
        case 'UPDATE':
          return p.canUpdate;
        case 'DELETE':
          return p.canDelete;
      }
    },
    [user, permissions],
  );

  // useMemo stops this object being recreated on every render, which would
  // re-render every consumer of the context for no reason.
  const value = useMemo<AuthState>(
    () => ({
      user,
      permissions,
      initialising,
      login,
      logout,
      can,
      isAdmin: user?.role === 'ADMIN',
    }),
    [user, permissions, initialising, login, logout, can],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Throwing when the context is missing turns "I forgot the provider" from a
 * confusing null-reference crash deep in a component into an explicit message.
 */
export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return ctx;
}
