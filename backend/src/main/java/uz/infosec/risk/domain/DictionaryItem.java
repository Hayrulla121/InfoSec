package uz.infosec.risk.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One dropdown value. Replaces Excel's data-validation lists.
 */
@Entity
@Table(name = "dictionary_items")
@Getter
@Setter
@NoArgsConstructor
public class DictionaryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dict_type", nullable = false, length = 32)
    private DictType dictType;

    @Column(nullable = false, length = 64)
    private String label;

    /**
     * Boxed Integer, not int: NULL is meaningful here (methods and statuses
     * have no numeric weight). A primitive would silently turn NULL into 0.
     */
    @Column(name = "numeric_value")
    private Integer numericValue;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
