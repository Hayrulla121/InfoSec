import { useI18n, type Lang } from '../i18n/I18nContext';

const OPTIONS: { code: Lang; short: string }[] = [
  { code: 'ru', short: 'RU' },
  { code: 'uz', short: "O'Z" },
];

/**
 * Two-state language toggle.
 *
 * A segmented control rather than a dropdown: with exactly two options both are
 * visible at once, so switching is one click and the current choice is readable
 * without opening anything.
 */
export function LanguageSwitcher() {
  const { lang, setLang, t } = useI18n();

  return (
    <div className="lang-switch" role="group" aria-label={t.lang.switchTo}>
      {OPTIONS.map((option) => (
        <button
          key={option.code}
          type="button"
          className={option.code === lang ? 'lang-option lang-active' : 'lang-option'}
          // Communicates the selected state to assistive tech, which cannot see
          // the highlight.
          aria-pressed={option.code === lang}
          onClick={() => setLang(option.code)}
        >
          {option.short}
        </button>
      ))}
    </div>
  );
}
