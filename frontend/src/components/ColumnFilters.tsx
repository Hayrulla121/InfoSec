import { useCallback, useEffect, useState } from 'react';
import { activeFilterCount, type FacetValue, type Facets, type Filters } from '../api/registries';
import { useI18n } from '../i18n/I18nContext';
import { IconClose } from './Icons';

/**
 * Filter state for one registry: the selections plus the dropdown options.
 *
 * @param loadFacets    the registry's facets() client call
 * @param onFilterChange run whenever a selection changes - every page passes
 *                       its setPageNo(0) here. Without it you can be sitting on
 *                       page 3, filter down to four rows, and see an empty
 *                       table because page 3 of one page does not exist.
 */
export function useRegistryFilters(
  loadFacets: () => Promise<Facets>,
  onFilterChange: () => void,
) {
  const [filters, setFilters] = useState<Filters>({});
  const [facets, setFacets] = useState<Facets | null>(null);

  const refreshFacets = useCallback(() => {
    // A failed facet load must not break the page: the dropdowns simply come
    // up empty and the table still works.
    loadFacets().then(setFacets).catch(() => setFacets(null));
    // loadFacets is a stable api-object method; re-running on identity changes
    // would refetch on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    refreshFacets();
  }, [refreshFacets]);

  const setFilter = useCallback(
    (field: string, value: string) => {
      setFilters((current) => ({ ...current, [field]: value }));
      onFilterChange();
    },
    [onFilterChange],
  );

  const resetFilters = useCallback(() => {
    setFilters({});
    onFilterChange();
  }, [onFilterChange]);

  return { filters, facets, setFilter, resetFilters, refreshFacets };
}

/**
 * One filterable column.
 *
 * @param field    the query-parameter name, which is also the facet key
 * @param label    translated column heading
 * @param translate optional mapper for values the server stores in Russian
 *                  (risk levels, treatment methods) so the dropdown reads in
 *                  the selected language while still SENDING the stored value
 */
export interface FilterDef {
  field: string;
  label: string;
  translate?: (value: string) => string;
  /** Fixed options, for columns that have no facet endpoint (e.g. booleans). */
  options?: FacetValue[];
}

/**
 * Column filters for a registry, rendered as a row of dropdowns.
 *
 * <p>Each option carries its row count — "Konfidensial ma'lumot (12)" — so the
 * question "how many assets hold confidential information" is answered by
 * opening the dropdown, without applying anything. That is the whole point of
 * the feature: aggregating, not just narrowing.
 *
 * <p>Counts come from the server and cover the WHOLE registry, not the current
 * filter. If they narrowed as you filtered, picking a value would collapse its
 * own dropdown to that single option and you could never switch to another.
 */
export function ColumnFilters({
  defs,
  facets,
  values,
  onChange,
  onReset,
  matched,
}: {
  defs: FilterDef[];
  facets: Facets | null;
  values: Filters;
  onChange: (field: string, value: string) => void;
  onReset: () => void;
  /** Rows matching the current filters; shown only when something is applied. */
  matched?: number;
}) {
  const { t } = useI18n();
  const applied = activeFilterCount(values);

  return (
    <div className="filter-bar">
      {defs.map((def) => {
        const options = def.options ?? facets?.[def.field] ?? [];
        const selected = values[def.field] ?? '';
        return (
          <label className="filter-field" key={def.field}>
            <span className="filter-label">{def.label}</span>
            <select
              className={selected ? 'filter-select filter-active' : 'filter-select'}
              value={selected}
              onChange={(e) => onChange(def.field, e.target.value)}
            >
              <option value="">{t.filter.all}</option>
              {options.map((o) => (
                <option key={o.value} value={o.value}>
                  {`${def.translate ? def.translate(o.value) : o.value} (${o.count})`}
                </option>
              ))}
            </select>
          </label>
        );
      })}

      {applied > 0 && (
        <div className="filter-summary">
          {matched !== undefined && <strong>{t.filter.matched(matched)}</strong>}
          <button type="button" className="filter-reset" onClick={onReset}>
            <IconClose size={14} />
            {t.filter.reset(applied)}
          </button>
        </div>
      )}
    </div>
  );
}
