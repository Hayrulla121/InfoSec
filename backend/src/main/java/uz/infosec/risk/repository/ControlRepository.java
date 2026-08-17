package uz.infosec.risk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.infosec.risk.domain.Control;

import java.util.List;

public interface ControlRepository extends JpaRepository<Control, Long> {

    @Query("select c.code from Control c")
    List<String> findAllCodes();

    /**
     * {@code implemented} is a Boolean, not a boolean: null has to mean "either",
     * which a primitive cannot express - it would default to false and silently
     * hide every implemented control.
     */
    @Query("""
            select c from Control c
            where (:q is null or :q = ''
                or lower(c.code)            like lower(concat('%', :q, '%'))
                or lower(c.name)            like lower(concat('%', :q, '%'))
                or lower(c.description)     like lower(concat('%', :q, '%'))
                or lower(c.treatmentMethod) like lower(concat('%', :q, '%')))
              and (:treatmentMethod is null or c.treatmentMethod = :treatmentMethod)
              and (:implemented     is null or c.implemented     = :implemented)
            """)
    Page<Control> search(@Param("q") String query,
                         @Param("treatmentMethod") String treatmentMethod,
                         @Param("implemented") Boolean implemented,
                         Pageable pageable);
}
