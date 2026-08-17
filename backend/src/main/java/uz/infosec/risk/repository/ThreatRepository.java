package uz.infosec.risk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.infosec.risk.domain.Threat;

import java.util.List;

public interface ThreatRepository extends JpaRepository<Threat, Long> {

    @Query("select t.code from Threat t")
    List<String> findAllCodes();

    @Query("""
            select t from Threat t
            where :q is null or :q = ''
               or lower(t.code)        like lower(concat('%', :q, '%'))
               or lower(t.description) like lower(concat('%', :q, '%'))
               or lower(t.levelLabel)  like lower(concat('%', :q, '%'))
            """)
    Page<Threat> search(@Param("q") String query, Pageable pageable);
}
