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

    @Query("""
            select c from Control c
            where :q is null or :q = ''
               or lower(c.code)            like lower(concat('%', :q, '%'))
               or lower(c.name)            like lower(concat('%', :q, '%'))
               or lower(c.description)     like lower(concat('%', :q, '%'))
               or lower(c.treatmentMethod) like lower(concat('%', :q, '%'))
            """)
    Page<Control> search(@Param("q") String query, Pageable pageable);
}
