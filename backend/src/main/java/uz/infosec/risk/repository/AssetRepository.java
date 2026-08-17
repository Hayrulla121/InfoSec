package uz.infosec.risk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.infosec.risk.domain.Asset;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    /** Codes only, for the КИА{n} generator. Cheap: one indexed column. */
    @Query("select a.code from Asset a")
    List<String> findAllCodes();

    /**
     * Case-insensitive search across the text columns, paged.
     *
     * <p>LOWER(...) LIKE is portable across H2 and PostgreSQL (unlike ILIKE,
     * which is PostgreSQL-only). The parameter is bound, never concatenated -
     * string-building a query is how SQL injection happens.
     */
    /**
     * "left join fetch" loads each asset's InfoSystem in the SAME query.
     * Without it, mapping a page of 20 assets to DTOs would fire 20 extra
     * SELECTs - the N+1 problem. LEFT (not inner) because the link is optional.
     *
     * <p>A fetch join needs its own countQuery: the default count derivation
     * cannot handle the join, and paging requires a row count.
     */
    @Query(value = """
            select a from Asset a
            left join fetch a.infoSystem
            where :q is null or :q = ''
               or lower(a.code)         like lower(concat('%', :q, '%'))
               or lower(a.name)         like lower(concat('%', :q, '%'))
               or lower(a.scope)        like lower(concat('%', :q, '%'))
               or lower(a.infoCategory) like lower(concat('%', :q, '%'))
               or lower(a.criticality)  like lower(concat('%', :q, '%'))
            """,
            countQuery = """
            select count(a) from Asset a
            where :q is null or :q = ''
               or lower(a.code)         like lower(concat('%', :q, '%'))
               or lower(a.name)         like lower(concat('%', :q, '%'))
               or lower(a.scope)        like lower(concat('%', :q, '%'))
               or lower(a.infoCategory) like lower(concat('%', :q, '%'))
               or lower(a.criticality)  like lower(concat('%', :q, '%'))
            """)
    Page<Asset> search(@Param("q") String query, Pageable pageable);

    /** Used from Phase 4 to recompute risks when an asset's criticality changes. */
    boolean existsByInfoSystemId(Long infoSystemId);

    List<Asset> findByCriticality(String criticality);
}
