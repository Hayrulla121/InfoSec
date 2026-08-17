import { api } from './client';

export type DictType =
  | 'ASSET_CRITICALITY'
  | 'THREAT_LEVEL'
  | 'TREATMENT_METHOD'
  | 'MEASURE_STATUS';

export interface DictionaryItem {
  id: number | null;
  label: string;
  numericValue: number | null;
  sortOrder: number;
}

export interface DictionaryGroup {
  dictType: DictType;
  title: string;
  numericRequired: boolean;
  items: DictionaryItem[];
}

export async function getDictionaries(): Promise<DictionaryGroup[]> {
  const { data } = await api.get<DictionaryGroup[]>('/api/dictionaries');
  return data;
}

export async function updateDictionary(
  dictType: DictType,
  items: DictionaryItem[],
): Promise<DictionaryGroup> {
  const { data } = await api.put<DictionaryGroup>('/api/dictionaries', { dictType, items });
  return data;
}

/** Convenience for the forms in later phases: just the labels of one dictionary. */
export async function getOptions(dictType: DictType): Promise<string[]> {
  const groups = await getDictionaries();
  return groups.find((g) => g.dictType === dictType)?.items.map((i) => i.label) ?? [];
}
