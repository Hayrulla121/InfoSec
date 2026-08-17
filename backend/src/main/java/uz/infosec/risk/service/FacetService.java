package uz.infosec.risk.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.infosec.risk.web.dto.FacetDtos.FacetValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Distinct values and their counts for the filterable columns of a registry.
 *
 * <p><b>Why this exists.</b> Columns like "Axborot toifasi" are free text, so a
 * hard-coded dropdown would drift the moment somebody types a new category. The
 * options are therefore read from the data itself: whatever is actually stored
 * is exactly what the filter offers.
 *
 * <p><b>The counts are the point.</b> Each option carries how many rows hold
 * that value, so "how many assets hold confidential information" is answered by
 * opening the dropdown - no filtering needed. Filtering then narrows the table
 * to those rows.
 *
 * <p><b>Counts ignore the currently applied filters, deliberately.</b> If they
 * were recomputed against the active filter, selecting a value would collapse
 * its own dropdown to that single option and there would be no way to switch to
 * another one. Stable options, whole-registry counts; the filtered total is
 * what the table's own row count shows.
 *
 * <p><b>On the interpolated field name.</b> The JPQL below is built with
 * String.format, which is normally how injection happens. It is safe here for
 * one reason only: the entity and field names can never come from the request -
 * they are compile-time constants in {@link #REGISTRIES}, and a registry key
 * that is not in that map is rejected before any query is built. Nothing a
 * caller sends ever reaches the query string.
 */
@Service
public class FacetService {

    /** One registry: its entity name and the columns that may be faceted. */
    private record Registry(String entity, List<String> fields) {
    }

    private static final Map<String, Registry> REGISTRIES = Map.of(
            "assets", new Registry("Asset",
                    List.of("infoCategory", "criticality", "scope", "securityClass")),
            "threats", new Registry("Threat",
                    List.of("levelLabel")),
            "controls", new Registry("Control",
                    List.of("treatmentMethod")),
            "risks", new Registry("Risk",
                    List.of("treatmentMethod", "measureStatus", "currentRiskLabel", "residualRiskLabel")),
            "info-systems", new Registry("InfoSystem",
                    List.of("confidentiality", "integrity", "availability", "dataFormat")));

    private final EntityManager entityManager;

    public FacetService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * @param registry one of the keys of {@link #REGISTRIES}; anything else is
     *                 a programming error, never user input reaching a query
     * @return field name -> its distinct values, most frequent first
     */
    @Transactional(readOnly = true)
    public Map<String, List<FacetValue>> facets(String registry) {
        Registry target = REGISTRIES.get(registry);
        if (target == null) {
            throw new IllegalArgumentException("Unknown registry: " + registry);
        }

        Map<String, List<FacetValue>> result = new LinkedHashMap<>();
        for (String field : target.fields()) {
            result.put(field, valuesOf(target.entity(), field));
        }
        return result;
    }

    private List<FacetValue> valuesOf(String entity, String field) {
        // Blank strings are excluded alongside NULL: an empty cell is "not set",
        // and offering "" as a filter option would be meaningless.
        String jpql = """
                select e.%s, count(e) from %s e
                where e.%s is not null and e.%s <> ''
                group by e.%s
                order by count(e) desc, e.%s asc
                """.formatted(field, entity, field, field, field, field);

        List<?> rows = entityManager.createQuery(jpql, Object[].class).getResultList();
        return rows.stream()
                .map(row -> (Object[]) row)
                .map(row -> new FacetValue((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }
}
