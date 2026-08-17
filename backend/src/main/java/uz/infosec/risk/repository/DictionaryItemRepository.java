package uz.infosec.risk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.infosec.risk.domain.DictType;
import uz.infosec.risk.domain.DictionaryItem;

import java.util.List;
import java.util.Optional;

public interface DictionaryItemRepository extends JpaRepository<DictionaryItem, Long> {

    List<DictionaryItem> findByDictTypeOrderBySortOrderAsc(DictType dictType);

    List<DictionaryItem> findAllByOrderByDictTypeAscSortOrderAsc();

    Optional<DictionaryItem> findByDictTypeAndLabel(DictType dictType, String label);
}
