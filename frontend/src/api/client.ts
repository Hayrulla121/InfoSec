import axios, { AxiosError } from 'axios';
import { currentLang } from '../i18n/I18nContext';

const TOKEN_KEY = 'risk.jwt';

/**
 * The token lives in localStorage so a page refresh keeps you logged in.
 *
 * Trade-off worth knowing: localStorage is readable by any JavaScript on the
 * page, so a cross-site-scripting bug would leak the token. The alternative -
 * an HttpOnly cookie - is immune to that but needs CSRF protection instead.
 * For an internal tool behind a login this is the usual, accepted choice.
 */
export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
});

/**
 * Request interceptor: attach the bearer token to every outgoing call.
 * Doing it here rather than at each call site means no endpoint can ever
 * forget it.
 */
api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  // Tells the backend which language to answer in, so validation and business
  // messages arrive already translated rather than needing a second lookup here.
  config.headers['Accept-Language'] = currentLang();
  return config;
});

/**
 * Response interceptor: a 401 means the token is gone, expired, or the account
 * was deactivated. Drop it and bounce to the login page.
 *
 * The /api/auth/ exclusion matters: a failed LOGIN also returns 401, and
 * redirecting there would wipe the error message before the user could read it.
 */
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const url = error.config?.url ?? '';
    if (error.response?.status === 401 && !url.includes('/api/auth/login')) {
      tokenStore.clear();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);

/** Shape of the backend's ApiError record. */
export interface ApiError {
  timestamp: string;
  status: number;
  message: string;
  fieldErrors?: { field: string; message: string }[];
}

/** Pulls a human-readable message out of whatever axios threw. */
export function errorMessage(e: unknown, fallback = 'Unexpected error'): string {
  const err = e as AxiosError<ApiError>;
  const body = err.response?.data;
  if (body?.fieldErrors?.length) {
    return body.fieldErrors.map((f) => `${f.field}: ${f.message}`).join('; ');
  }
  return body?.message ?? err.message ?? fallback;
}
