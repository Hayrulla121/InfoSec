package uz.infosec.risk.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Risk matrix and dashboard aggregates. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = login();
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private JsonNode create(String url, String body) throws Exception {
        return json.readTree(mvc.perform(post(url)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode fetch(String url) throws Exception {
        return json.readTree(mvc.perform(get(url).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private long asset(String name, String criticality) throws Exception {
        return create("/api/assets", """
                {"name":"%s","criticality":"%s"}""".formatted(name, criticality)).get("id").asLong();
    }

    private long threat(String description, int each) throws Exception {
        return create("/api/threats", """
                {"description":"%s","discoverability":%d,"repeatability":%d,"exploitability":%d,
                 "affectedUsers":%d,"damage":%d}"""
                .formatted(description, each, each, each, each, each)).get("id").asLong();
    }

    private JsonNode cell(JsonNode matrix, int assetRating, int threatRating) {
        for (JsonNode c : matrix.get("cells")) {
            if (c.get("assetRating").asInt() == assetRating
                    && c.get("threatRating").asInt() == threatRating) {
                return c;
            }
        }
        throw new AssertionError("No cell " + assetRating + "x" + threatRating);
    }

    @Test
    void matrixIsAlwaysAFullFiveByFiveGrid() throws Exception {
        JsonNode m = fetch("/api/risk-matrix");

        assertThat(m.get("cells")).hasSize(25);
        // Rows descend 5..1 and columns ascend 1..5, as the sheet is laid out.
        assertThat(m.get("assetRatings").toString()).isEqualTo("[5,4,3,2,1]");
        assertThat(m.get("threatRatings").toString()).isEqualTo("[1,2,3,4,5]");
    }

    /**
     * Every cell carries the level the a x t algorithm gives for its
     * coordinates, so the grid is coloured even where no risks exist yet.
     */
    @Test
    void everyCellCarriesItsAlgorithmLevelEvenWhenEmpty() throws Exception {
        JsonNode m = fetch("/api/risk-matrix");

        assertThat(cell(m, 5, 5).get("riskLabel").asText()).isEqualTo("Критический");
        assertThat(cell(m, 1, 1).get("riskLabel").asText()).isEqualTo("Незначительный");
        assertThat(cell(m, 5, 2).get("riskLabel").asText()).isEqualTo("Средний");
        assertThat(cell(m, 5, 1).get("riskLabel").asText()).isEqualTo("Низкий");
        assertThat(cell(m, 2, 2).get("riskLabel").asText()).isEqualTo("Низкий");
    }

    /** Acceptance criterion 4: cell counts must match the register exactly. */
    @Test
    void cellCountsMatchTheRegisterAndTheCellDrillDownReturnsThoseRisks() throws Exception {
        // Criticality 3 (Средняя) x a threat scoring 5x3=15 -> rating 3.
        long assetId = asset("Matrix asset", "Средняя");
        long threatId = threat("Matrix threat", 3);
        create("/api/risks", """
                {"assetId":%d,"threatId":%d,"name":"Matrix risk"}"""
                .formatted(assetId, threatId));

        JsonNode m = fetch("/api/risk-matrix");
        JsonNode target = cell(m, 3, 3);
        int countInCell = target.get("count").asInt();
        assertThat(countInCell).isGreaterThanOrEqualTo(1);

        // Clicking the cell filters the register by the same two coordinates.
        JsonNode drill = fetch("/api/risks?assetRating=3&threatRating=3&size=100");
        assertThat(drill.get("totalElements").asInt()).isEqualTo(countInCell);
        for (JsonNode r : drill.get("content")) {
            assertThat(r.get("assetRating").asInt()).isEqualTo(3);
            assertThat(r.get("current").get("threatRating").asInt()).isEqualTo(3);
        }
    }

    @Test
    void emptyCellsReportNullRatherThanZero() throws Exception {
        JsonNode m = fetch("/api/risk-matrix");

        long nullCells = 0;
        for (JsonNode c : m.get("cells")) {
            if (c.get("count").isNull()) {
                nullCells++;
            }
        }
        // A handful of risks can never populate all 25 cells; the rest must be
        // null so the UI renders them blank, like the workbook does.
        assertThat(nullCells).isGreaterThan(0);
    }

    @Test
    void matrixTotalEqualsTheSumOfItsCells() throws Exception {
        JsonNode m = fetch("/api/risk-matrix");

        int sum = 0;
        for (JsonNode c : m.get("cells")) {
            if (!c.get("count").isNull()) {
                sum += c.get("count").asInt();
            }
        }
        assertThat(m.get("totalRisks").asInt()).isEqualTo(sum);
    }

    @Test
    void dashboardDistributionsCoverAllFiveLevelsAndSumToTheRiskCount() throws Exception {
        JsonNode d = fetch("/api/dashboard");

        assertThat(d.get("currentDistribution")).hasSize(5);
        assertThat(d.get("residualDistribution")).hasSize(5);
        // Descending, so Критический is first.
        assertThat(d.get("currentDistribution").get(0).get("level").asInt()).isEqualTo(5);
        assertThat(d.get("currentDistribution").get(0).get("label").asText())
                .isEqualTo("Критический");

        long sum = 0;
        for (JsonNode l : d.get("currentDistribution")) {
            sum += l.get("count").asLong();
        }
        assertThat(sum).isEqualTo(d.get("totalRisks").asLong());
    }

    @Test
    void gaugeNeedleIsTheWorstCurrentLevelOnThatAsset() throws Exception {
        long assetId = asset("Gauge asset", "Критичная");
        // Two risks on one asset: a mild one and a severe one.
        create("/api/risks", """
                {"assetId":%d,"threatId":%d,"name":"Mild"}"""
                .formatted(assetId, threat("Gauge mild threat", 1)));
        create("/api/risks", """
                {"assetId":%d,"threatId":%d,"name":"Severe"}"""
                .formatted(assetId, threat("Gauge severe threat", 5)));

        JsonNode d = fetch("/api/dashboard");
        JsonNode gauge = null;
        for (JsonNode g : d.get("assetGauges")) {
            if (g.get("assetId").asLong() == assetId) {
                gauge = g;
            }
        }

        assertThat(gauge).isNotNull();
        assertThat(gauge.get("riskCount").asInt()).isEqualTo(2);
        // a=5, t=5 -> Критический(5) is worse than a=5,t=1 -> Низкий(2).
        assertThat(gauge.get("worstCurrentLevel").asInt()).isEqualTo(5);
        assertThat(gauge.get("worstCurrentLabel").asText()).isEqualTo("Критический");
    }

    @Test
    void assetWithoutRisksStillGetsACard() throws Exception {
        long assetId = asset("Lonely asset", "Низкая");

        JsonNode d = fetch("/api/dashboard");
        JsonNode gauge = null;
        for (JsonNode g : d.get("assetGauges")) {
            if (g.get("assetId").asLong() == assetId) {
                gauge = g;
            }
        }

        assertThat(gauge).isNotNull();
        assertThat(gauge.get("riskCount").asInt()).isZero();
        assertThat(gauge.get("worstCurrentLevel").isNull()).isTrue();
    }

    @Test
    void implementedPercentReflectsTheControlLinks() throws Exception {
        long assetId = asset("Percent asset", "Средняя");
        long threatId = threat("Percent threat", 2);
        long riskId = create("/api/risks", """
                {"assetId":%d,"threatId":%d,"name":"Percent risk"}"""
                .formatted(assetId, threatId)).get("id").asLong();

        long c1 = create("/api/controls", """
                {"name":"Pct impl","treatmentMethod":"Снижение","reductionPct":0.1,
                 "implemented":true}""").get("id").asLong();
        long c2 = create("/api/controls", """
                {"name":"Pct plan","treatmentMethod":"Снижение","reductionPct":0.1,
                 "implemented":false}""").get("id").asLong();

        mvc.perform(post("/api/risks/" + riskId + "/controls")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"controlId":%d,"type":"IMPLEMENTED"}""".formatted(c1)));
        mvc.perform(post("/api/risks/" + riskId + "/controls")
                .header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"controlId":%d,"type":"PLANNED"}""".formatted(c2)));

        JsonNode d = fetch("/api/dashboard");
        assertThat(d.get("implementedControlLinks").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(d.get("plannedControlLinks").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(d.get("implementedPercent").asInt()).isBetween(0, 100);
    }

    @Test
    void overdueCountsOnlyPastDeadlinesThatAreNotDone() throws Exception {
        long assetId = asset("Overdue asset", "Высокая");

        create("/api/risks", """
                {"assetId":%d,"threatId":%d,"name":"Late and unfinished",
                 "implementationDeadline":"2020-01-01","measureStatus":"Задержка"}"""
                .formatted(assetId, threat("Overdue threat A", 2)));
        create("/api/risks", """
                {"assetId":%d,"threatId":%d,"name":"Late but finished",
                 "implementationDeadline":"2020-01-01","measureStatus":"Выполнено"}"""
                .formatted(assetId, threat("Overdue threat B", 2)));
        create("/api/risks", """
                {"assetId":%d,"threatId":%d,"name":"Future deadline",
                 "implementationDeadline":"2099-01-01","measureStatus":"Задержка"}"""
                .formatted(assetId, threat("Overdue threat C", 2)));

        JsonNode d = fetch("/api/dashboard");
        // Exactly one of the three qualifies.
        assertThat(d.get("overdueMeasures").asLong()).isEqualTo(1);
    }

    @Test
    void aggregatesRequireAuthenticationButNoModuleGrant() throws Exception {
        mvc.perform(get("/api/risk-matrix")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"matrix-viewer","password":"secret123",
                                 "fullName":"Matrix Viewer","role":"USER"}"""))
                .andExpect(status().isCreated());

        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"matrix-viewer","password":"secret123"}"""))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("token").asText();

        mvc.perform(get("/api/risk-matrix").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------- charts

    /**
     * The reduction chart needs three distributions of the same shape, or the
     * client cannot read them column-wise to build one series per level.
     */
    @Test
    void allThreeDistributionsAlwaysCarryTheSameFiveLevels() throws Exception {
        JsonNode d = fetch("/api/dashboard");

        for (String field : List.of("inherentDistribution", "currentDistribution", "residualDistribution")) {
            JsonNode dist = d.get(field);
            assertThat(dist).as(field).hasSize(5);
            // Descending 5..1, so index i is the same level in all three.
            assertThat(dist.get(0).get("level").asInt()).as(field).isEqualTo(5);
            assertThat(dist.get(4).get("level").asInt()).as(field).isEqualTo(1);
        }
    }

    /**
     * Controls can only ever reduce a risk, never raise it. So the population
     * cannot drift upward as stages are applied: the number of risks at or
     * above any level must not grow from inherent to current to residual.
     *
     * <p>Asserted as a property rather than fixed numbers because this class
     * shares its database with the rest of the suite - the invariant holds for
     * any data, which is what makes it worth testing at all.
     */
    @Test
    void riskNeverIncreasesFromInherentThroughCurrentToResidual() throws Exception {
        JsonNode d = fetch("/api/dashboard");

        for (int level = 5; level >= 2; level--) {
            long inherent = atOrAbove(d.get("inherentDistribution"), level);
            long current = atOrAbove(d.get("currentDistribution"), level);
            long residual = atOrAbove(d.get("residualDistribution"), level);

            assertThat(current).as("risks at level >= %d, current vs inherent", level)
                    .isLessThanOrEqualTo(inherent);
            assertThat(residual).as("risks at level >= %d, residual vs current", level)
                    .isLessThanOrEqualTo(current);
        }
    }

    private long atOrAbove(JsonNode distribution, int level) {
        long total = 0;
        for (JsonNode entry : distribution) {
            if (entry.get("level").asInt() >= level) {
                total += entry.get("count").asLong();
            }
        }
        return total;
    }

    /**
     * The timeline is the chart with real logic behind it, so its three
     * invariants are asserted directly: no month may be skipped, both lines are
     * running totals and therefore never fall, and "done" is a subset of "due".
     */
    @Test
    void remediationTimelineIsGapFreeCumulativeAndBounded() throws Exception {
        long assetId = asset("Timeline asset", "Высокая");
        long threatId = threat("Timeline threat", 4);

        // Deliberately far apart, and deliberately out of order: the service
        // must sort them and invent the empty months in between.
        createRiskWithDeadline(assetId, threatId, "Timeline late", "2031-09-30", "Задержка");
        long other = threat("Timeline threat two", 2);
        createRiskWithDeadline(assetId, other, "Timeline early", "2031-01-31", "Выполнено");

        JsonNode timeline = fetch("/api/dashboard").get("remediationTimeline");
        assertThat(timeline).isNotEmpty();

        YearMonth previousMonth = null;
        long previousDue = 0;
        long previousDone = 0;
        for (JsonNode point : timeline) {
            YearMonth month = YearMonth.parse(point.get("month").asText());
            if (previousMonth != null) {
                assertThat(month).as("months must be consecutive, no gaps")
                        .isEqualTo(previousMonth.plusMonths(1));
            }
            long due = point.get("dueTotal").asLong();
            long done = point.get("doneTotal").asLong();

            assertThat(due).as("due is cumulative").isGreaterThanOrEqualTo(previousDue);
            assertThat(done).as("done is cumulative").isGreaterThanOrEqualTo(previousDone);
            assertThat(done).as("done can never exceed due").isLessThanOrEqualTo(due);

            previousMonth = month;
            previousDue = due;
            previousDone = done;
        }

        // The two risks above are 8 months apart, so the gap-filling had to
        // invent the seven months between them.
        assertThat(timeline.size()).isGreaterThanOrEqualTo(9);
    }

    /** Ranked largest-first, so a legend reads in order of significance. */
    @Test
    void breakdownsAreSortedByCountDescending() throws Exception {
        long assetId = asset("Breakdown asset", "Высокая");
        createRiskWithDeadline(assetId, threat("Breakdown threat", 3),
                "Breakdown risk", "2032-04-30", "Выполнено");

        JsonNode d = fetch("/api/dashboard");
        for (String field : List.of("treatmentBreakdown", "statusBreakdown")) {
            JsonNode rows = d.get(field);
            assertThat(rows).as(field).isNotEmpty();
            long previous = Long.MAX_VALUE;
            for (JsonNode row : rows) {
                long count = row.get("count").asLong();
                assertThat(count).as(field).isLessThanOrEqualTo(previous);
                previous = count;
            }
        }
    }

    private void createRiskWithDeadline(long assetId, long threatId, String name,
                                        String deadline, String status) throws Exception {
        create("/api/risks", """
                {"assetId":%d,"threatId":%d,"name":"%s","treatmentMethod":"Снижение",
                 "measureStatus":"%s","implementationDeadline":"%s"}"""
                .formatted(assetId, threatId, name, status, deadline));
    }
}
