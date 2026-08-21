import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { ru, type Dictionary } from './ru';
import { uz } from './uz';

export type Lang = 'ru' | 'uz';

const STORAGE_KEY = 'risk.lang';

const DICTIONARIES: Record<Lang, Dictionary> = { ru, uz };

interface I18nApi {
  lang: Lang;
  setLang: (lang: Lang) => void;
  /** The active dictionary; accessed as `t.nav.home`, not `t('nav.home')`. */
  t: Dictionary;
  /** Translates a value the server stored in Russian, for display only. */
  level: (label: string | null | undefined) => string;
  /** Threat levels: the same five words as `level` bar the top one. */
  threat: (label: string | null | undefined) => string;
  criticality: (label: string | null | undefined) => string;
  method: (label: string | null | undefined) => string;
  status: (label: string | null | undefined) => string;
}

const I18nContext = createContext<I18nApi | null>(null);

/** Reads the saved choice, else follows the browser, else Russian. */
function initialLang(): Lang {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved === 'ru' || saved === 'uz') {
    return saved;
  }
  return navigator.language?.toLowerCase().startsWith('uz') ? 'uz' : 'ru';
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [lang, setLangState] = useState<Lang>(initialLang);

  const setLang = useCallback((next: Lang) => {
    setLangState(next);
    localStorage.setItem(STORAGE_KEY, next);
    // Screen readers and browser features (hyphenation, spell-check) key off
    // this attribute, so it has to move with the UI language.
    document.documentElement.lang = next;
  }, []);

  const value = useMemo<I18nApi>(() => {
    const t = DICTIONARIES[lang];

    /**
     * Maps a stored Russian label to the display language.
     *
     * Falls back to the original string when there is no mapping — a value an
     * administrator typed into the dictionary editor is data we do not know how
     * to translate, and showing it untouched is better than showing nothing.
     */
    const translate = (table: Record<string, string>) => (label: string | null | undefined) =>
      label ? (table[label] ?? label) : t.common.none;

    return {
      lang,
      setLang,
      t,
      level: translate(t.riskLevel),
      threat: translate(t.threatLevel),
      criticality: translate(t.criticality),
      method: translate(t.treatmentMethod),
      status: translate(t.measureStatus),
    };
  }, [lang, setLang]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nApi {
  const ctx = useContext(I18nContext);
  if (!ctx) {
    throw new Error('useI18n must be used inside <I18nProvider>');
  }
  return ctx;
}

/**
 * Read outside React, for the axios interceptor that sets Accept-Language.
 * Backend validation and business messages then arrive already translated.
 */
export function currentLang(): Lang {
  const saved = localStorage.getItem(STORAGE_KEY);
  return saved === 'uz' ? 'uz' : 'ru';
}
