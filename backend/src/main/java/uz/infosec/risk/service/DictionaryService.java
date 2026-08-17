package uz.infosec.risk.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.domain.DictType;
import uz.infosec.risk.domain.DictionaryItem;
import uz.infosec.risk.error.ConflictException;
import uz.infosec.risk.error.NotFoundException;
import uz.infosec.risk.repository.DictionaryItemRepository;
import uz.infosec.risk.web.dto.DictionaryDtos.DictionaryGroupDto;
import uz.infosec.risk.web.dto.DictionaryDtos.DictionaryItemDto;

import java.util.*;

@Service
public class DictionaryService {

    private final DictionaryItemRepository repository;

    public DictionaryService(DictionaryItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<DictionaryGroupDto> findAllGrouped() {
        Map<DictType, List<DictionaryItem>> byType = new EnumMap<>(DictType.class);
        for (DictionaryItem item : repository.findAllByOrderByDictTypeAscSortOrderAsc()) {
            byType.computeIfAbsent(item.getDictType(), k -> new ArrayList<>()).add(item);
        }
        // Iterate the enum, not the map, so an empty dictionary still appears
        // in the UI as an editable (empty) section instead of vanishing.
        return Arrays.stream(DictType.values())
                .map(type -> new DictionaryGroupDto(
                        type,
                        type.getTitle(),
                        type.isNumericRequired(),
                        byType.getOrDefault(type, List.of()).stream()
                                .map(DictionaryService::toDto)
                                .toList()))
                .toList();
    }

    /**
     * Replaces one dictionary's contents: rows with an id are updated, rows
     * without are inserted, and stored rows absent from the payload are deleted.
     */
    @Transactional
    public DictionaryGroupDto replace(DictType dictType, List<DictionaryItemDto> requested) {
        validate(dictType, requested);

        List<DictionaryItem> existing = repository.findByDictTypeOrderBySortOrderAsc(dictType);
        Map<Long, DictionaryItem> byId = new HashMap<>();
        existing.forEach(item -> byId.put(item.getId(), item));

        Set<Long> keptIds = new HashSet<>();
        for (DictionaryItemDto dto : requested) {
            if (dto.id() != null) {
                if (!byId.containsKey(dto.id())) {
                    // The id belongs to a different dictionary, or to a row
                    // someone else already deleted.
                    throw NotFoundException.of("entity.dictionaryItem", dto.id());
                }
                keptIds.add(dto.id());
            }
        }

        // Deletions must reach the database BEFORE any insert or rename.
        // Hibernate's flush order within one transaction is inserts, then
        // updates, then deletes - so without this explicit flush, re-adding a
        // label that is being deleted in the same request (or renaming a row
        // onto that label) hits the UNIQUE(dict_type, label) index.
        List<DictionaryItem> removed = existing.stream()
                .filter(item -> !keptIds.contains(item.getId()))
                .toList();
        if (!removed.isEmpty()) {
            repository.deleteAll(removed);
            repository.flush();
        }

        List<DictionaryItem> toSave = new ArrayList<>();
        int order = 1;
        for (DictionaryItemDto dto : requested) {
            DictionaryItem item;
            if (dto.id() == null) {
                item = new DictionaryItem();
                item.setDictType(dictType);
            } else {
                item = byId.get(dto.id());
            }
            item.setLabel(dto.label().trim());
            item.setNumericValue(dto.numericValue());
            // Renumber server-side so the stored order always matches the order
            // the admin sees, without trusting client-supplied indexes.
            item.setSortOrder(order++);
            toSave.add(item);
        }
        repository.saveAll(toSave);

        return findAllGrouped().stream()
                .filter(g -> g.dictType() == dictType)
                .findFirst()
                .orElseThrow();
    }

    /** Resolves a criticality/level label to its 1-5 weight. Used from Phase 3 on. */
    @Transactional(readOnly = true)
    public int numericValueOf(DictType dictType, String label) {
        DictionaryItem item = repository.findByDictTypeAndLabel(dictType, label)
                .orElseThrow(() -> NotFoundException.code(
                        "dictionary.noSuchEntry", dictType.getTitleCode(), label));
        if (item.getNumericValue() == null) {
            throw ConflictException.of("dictionary.noNumericValue", label);
        }
        return item.getNumericValue();
    }

    private void validate(DictType dictType, List<DictionaryItemDto> items) {
        if (items.isEmpty()) {
            throw ConflictException.of("dictionary.cannotBeEmpty", dictType.getTitleCode());
        }

        Set<String> labels = new HashSet<>();
        Set<Integer> numbers = new HashSet<>();
        for (DictionaryItemDto dto : items) {
            String label = dto.label() == null ? "" : dto.label().trim();
            if (!labels.add(label.toLowerCase(Locale.ROOT))) {
                throw ConflictException.of("dictionary.duplicateValue", label);
            }
            if (dictType.isNumericRequired()) {
                if (dto.numericValue() == null) {
                    throw ConflictException.of("dictionary.levelRequired", label);
                }
                // A duplicated weight would make the a x t risk formula
                // ambiguous, so it is rejected rather than silently accepted.
                if (!numbers.add(dto.numericValue())) {
                    throw ConflictException.of("dictionary.levelDuplicate", dto.numericValue());
                }
            }
        }
    }

    private static DictionaryItemDto toDto(DictionaryItem item) {
        return new DictionaryItemDto(item.getId(), item.getLabel(),
                item.getNumericValue(), item.getSortOrder());
    }
}
