package uz.infosec.risk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.infosec.risk.domain.InfoSystem;

import java.util.List;

public interface InfoSystemRepository extends JpaRepository<InfoSystem, Long> {

    @Query("select s.code from InfoSystem s")
    List<String> findAllCodes();

    @Query("""
            select s from InfoSystem s
            where (:q is null or :q = ''
                or lower(s.code)        like lower(concat('%', :q, '%'))
                or lower(s.name)        like lower(concat('%', :q, '%'))
                or lower(s.description) like lower(concat('%', :q, '%'))
                or lower(s.owner)       like lower(concat('%', :q, '%')))
              and (:confidentiality is null or s.confidentiality = :confidentiality)
              and (:integrity       is null or s.integrity       = :integrity)
              and (:availability    is null or s.availability    = :availability)
              and (:dataFormat      is null or s.dataFormat      = :dataFormat)
            """)
    Page<InfoSystem> search(@Param("q") String query,
                            @Param("confidentiality") String confidentiality,
                            @Param("integrity") String integrity,
                            @Param("availability") String availability,
                            @Param("dataFormat") String dataFormat,
                            Pageable pageable);
}
