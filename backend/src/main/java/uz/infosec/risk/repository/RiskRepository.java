package uz.infosec.risk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.infosec.risk.domain.Risk;

import java.util.List;
import java.util.Optional;

public interface RiskRepository extends JpaRepository<Risk, Long> {

    @Query("select r.code from Risk r")
    List<String> findAllCodes();

    boolean existsByAssetIdAndThreatId(Long assetId, Long threatId);

    /** Everything needed to render a risk row, in one query. */
    @Query("""
            select r from Risk r
            join fetch r.asset
            join fetch r.threat
            where r.id = :id
            """)
    Optional<Risk> findByIdWithRefs(@Param("id") Long id);

    @Query(value = """
            select r from Risk r
            join fetch r.asset a
            join fetch r.threat t
            where (:q is null or :q = ''
                or lower(r.code)          like lower(concat('%', :q, '%'))
                or lower(r.name)          like lower(concat('%', :q, '%'))
                or lower(r.owner)         like lower(concat('%', :q, '%'))
                or lower(a.name)          like lower(concat('%', :q, '%'))
                or lower(t.description)   like lower(concat('%', :q, '%')))
              and (:assetRating is null or a.criticalityRating = :assetRating)
              and (:threatRating is null or r.currentThreatRating = :threatRating)
              and (:treatmentMethod is null or r.treatmentMethod = :treatmentMethod)
              and (:measureStatus is null or r.measureStatus = :measureStatus)
              and (:currentRiskLabel is null or r.currentRiskLabel = :currentRiskLabel)
              and (:residualRiskLabel is null or r.residualRiskLabel = :residualRiskLabel)
            """,
            countQuery = """
            select count(r) from Risk r
            join r.asset a
            join r.threat t
            where (:q is null or :q = ''
                or lower(r.code)          like lower(concat('%', :q, '%'))
                or lower(r.name)          like lower(concat('%', :q, '%'))
                or lower(r.owner)         like lower(concat('%', :q, '%'))
                or lower(a.name)          like lower(concat('%', :q, '%'))
                or lower(t.description)   like lower(concat('%', :q, '%')))
              and (:assetRating is null or a.criticalityRating = :assetRating)
              and (:threatRating is null or r.currentThreatRating = :threatRating)
              and (:treatmentMethod is null or r.treatmentMethod = :treatmentMethod)
              and (:measureStatus is null or r.measureStatus = :measureStatus)
              and (:currentRiskLabel is null or r.currentRiskLabel = :currentRiskLabel)
              and (:residualRiskLabel is null or r.residualRiskLabel = :residualRiskLabel)
            """)
    Page<Risk> search(@Param("q") String query,
                      @Param("assetRating") Integer assetRating,
                      @Param("threatRating") Integer threatRating,
                      @Param("treatmentMethod") String treatmentMethod,
                      @Param("measureStatus") String measureStatus,
                      @Param("currentRiskLabel") String currentRiskLabel,
                      @Param("residualRiskLabel") String residualRiskLabel,
                      Pageable pageable);

    // ---- recalculation fan-out: which risks does this change affect? ----

    List<Risk> findByAssetId(Long assetId);

    List<Risk> findByThreatId(Long threatId);

    @Query("select distinct rc.risk from RiskControl rc where rc.control.id = :controlId")
    List<Risk> findByLinkedControlId(@Param("controlId") Long controlId);

    /**
     * Risk matrix source: rows = asset criticality, cols = threat rating after
     * implemented controls. Excel does this with COUNTIFS over columns AF and BV.
     */
    @Query("""
            select a.criticalityRating, r.currentThreatRating, count(r)
            from Risk r join r.asset a
            where r.currentThreatRating is not null
            group by a.criticalityRating, r.currentThreatRating
            """)
    List<Object[]> matrixCounts();

    // ------------------------------------------------------- dashboard

    /** How many risks sit at each current level. One row per level present. */
    @Query("""
            select r.currentRiskLevel, count(r) from Risk r
            where r.currentRiskLevel is not null
            group by r.currentRiskLevel
            """)
    List<Object[]> countByCurrentLevel();

    @Query("""
            select r.residualRiskLevel, count(r) from Risk r
            where r.residualRiskLevel is not null
            group by r.residualRiskLevel
            """)
    List<Object[]> countByResidualLevel();

    /**
     * The starting point of the reduction chart: the level a risk would sit at
     * with no controls at all. Without it the dashboard can only show where
     * risks ended up, not how far the controls actually moved them.
     */
    @Query("""
            select r.inherentRiskLevel, count(r) from Risk r
            where r.inherentRiskLevel is not null
            group by r.inherentRiskLevel
            """)
    List<Object[]> countByInherentLevel();

    /**
     * Deadline and status for every risk that has a deadline, ungrouped.
     *
     * <p>Bucketing into months is done in Java on purpose. Truncating a date to
     * its month is spelled differently in every dialect (date_trunc in
     * PostgreSQL, formatdatetime in H2), and this project has to run on both.
     * The row count is one per risk - hundreds, not millions - so pulling two
     * columns and grouping in memory costs nothing and stays portable.
     */
    @Query("""
            select r.implementationDeadline, r.measureStatus from Risk r
            where r.implementationDeadline is not null
            """)
    List<Object[]> deadlinesWithStatus();

    /** Treatment strategy split: уменьшение / принятие / передача / избежание. */
    @Query("""
            select r.treatmentMethod, count(r) from Risk r
            where r.treatmentMethod is not null and r.treatmentMethod <> ''
            group by r.treatmentMethod
            """)
    List<Object[]> countByTreatmentMethod();

    /** Progress of the remediation plan, by status label. */
    @Query("""
            select r.measureStatus, count(r) from Risk r
            where r.measureStatus is not null and r.measureStatus <> ''
            group by r.measureStatus
            """)
    List<Object[]> countByMeasureStatus();

    /**
     * Per-asset summary for the gauge cards: risk count and the WORST (highest)
     * current and residual level on that asset.
     *
     * <p>Done as one grouped query rather than a loop over assets - the loop
     * would be an N+1 over however many assets the bank registers.
     */
    @Query("""
            select a.id, count(r), max(r.currentRiskLevel), max(r.residualRiskLevel)
            from Risk r join r.asset a
            group by a.id
            """)
    List<Object[]> assetRiskSummary();

    /** Deadline in the past and the measure is not finished yet. */
    @Query("""
            select count(r) from Risk r
            where r.implementationDeadline is not null
              and r.implementationDeadline < :today
              and (r.measureStatus is null or r.measureStatus <> :doneStatus)
            """)
    long countOverdue(@Param("today") java.time.LocalDate today,
                      @Param("doneStatus") String doneStatus);
}
