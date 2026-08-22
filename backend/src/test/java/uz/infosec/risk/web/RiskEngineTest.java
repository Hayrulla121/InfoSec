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

import static uz.infosec.risk.TestCredentials.ADMIN_PASSWORD;
import static uz.infosec.risk.TestCredentials.ADMIN_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The risk engine end to end: create a risk, attach controls, watch
 * inherent -> current -> residual move, and confirm every recalculation
 * trigger fires.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RiskEngineTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(username, password)))
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

    private JsonNode putJson(String url, String body) throws Exception {
        return json.readTree(mvc.perform(put(url)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode getRisk(long id) throws Exception {
        return json.readTree(mvc.perform(get("/api/risks/" + id)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    /** Asset with rating 5 (Критичная). */
    private long criticalAsset(String name) throws Exception {
        return create("/api/assets", """
                {"name":"%s","criticality":"Критичная"}""".formatted(name)).get("id").asLong();
    }

    /** Threat scoring 2+2+4+3+2 = 13 -> rating 3. */
    private long threat13(String description) throws Exception {
        return create("/api/threats", """
                {"description":"%s","discoverability":2,"repeatability":2,"exploitability":4,
                 "affectedUsers":3,"damage":2}""".formatted(description)).get("id").asLong();
    }

    private long control(String name, String pct) throws Exception {
        return create("/api/controls", """
                {"name":"%s","treatmentMethod":"Снижение","reductionPct":%s,"implemented":false}"""
                .formatted(name, pct)).get("id").asLong();
    }

    private long risk(long assetId, long threatId, String name) throws Exception {
        return create("/api/risks", """
                {"assetId":%d,"threatId":%d,"name":"%s"}"""
                .formatted(assetId, threatId, name)).get("id").asLong();
    }

    private void attach(long riskId, long controlId, String type) throws Exception {
        mvc.perform(post("/api/risks/" + riskId + "/controls")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"controlId":%d,"type":"%s"}""".formatted(controlId, type)))
                .andExpect(status().isOk());
    }

    /**
     * The per-link steps that let the UI show its work.
     *
     * <p>Each link reports the score going into it and coming out, so the
     * formula hint can render "13 - 13 x 20% = 10.4" without doing any
     * arithmetic of its own. Two invariants matter and are asserted here:
     * the chain is CONTIGUOUS (each step starts where the previous ended), and
     * it TERMINATES at the stage score already stored on the risk. If either
     * broke, the hint would narrate a derivation that does not reach the number
     * printed next to it.
     */
    @Test
    void controlLinksCarryTheirOwnStepOfTheReductionChain() throws Exception {
        long assetId = criticalAsset("Chain asset");
        long threatId = threat13("Chain threat");
        long riskId = risk(assetId, threatId, "Chain risk");

        attach(riskId, control("Chain impl 20%", "0.20"), "IMPLEMENTED");
        attach(riskId, control("Chain plan 20%", "0.20"), "PLANNED");
        attach(riskId, control("Chain plan 50%", "0.50"), "PLANNED");

        JsonNode r = getRisk(riskId);

        // Implemented chain starts at the raw threat score: 13 -> 10.4.
        JsonNode impl = r.get("implementedControls");
        assertThat(impl).hasSize(1);
        assertThat(impl.get(0).get("scoreBefore").decimalValue()).isEqualByComparingTo("13");
        assertThat(impl.get(0).get("scoreAfter").decimalValue()).isEqualByComparingTo("10.4");
        assertThat(r.get("current").get("score").decimalValue()).isEqualByComparingTo("10.4");

        // Planned chain CONTINUES from the current score, as Excel's BC starts
        // from AW rather than from AH: 10.4 -> 8.32 -> 4.16.
        JsonNode planned = r.get("plannedControls");
        assertThat(planned).hasSize(2);
        assertThat(planned.get(0).get("scoreBefore").decimalValue()).isEqualByComparingTo("10.4");
        assertThat(planned.get(0).get("scoreAfter").decimalValue()).isEqualByComparingTo("8.32");
        assertThat(planned.get(1).get("scoreBefore").decimalValue()).isEqualByComparingTo("8.32");
        assertThat(planned.get(1).get("scoreAfter").decimalValue()).isEqualByComparingTo("4.16");
        assertThat(r.get("residual").get("score").decimalValue()).isEqualByComparingTo("4.16");

        // Contiguity, stated as a rule rather than as three literals.
        for (int i = 1; i < planned.size(); i++) {
            assertThat(planned.get(i).get("scoreBefore").decimalValue())
                    .isEqualByComparingTo(planned.get(i - 1).get("scoreAfter").decimalValue());
        }
    }

    /** With nothing attached there are no steps, and no chain to explain. */
    @Test
    void aRiskWithoutControlsHasNoChainSteps() throws Exception {
        long riskId = risk(criticalAsset("Bare asset"), threat13("Bare threat"), "Bare risk");

        JsonNode r = getRisk(riskId);
        assertThat(r.get("implementedControls")).isEmpty();
        assertThat(r.get("plannedControls")).isEmpty();
        // The stage score is still the raw threat score, so the hint's "base"
        // and "result" simply coincide.
        assertThat(r.get("current").get("score").decimalValue()).isEqualByComparingTo("13");
    }

    /** The drawer explains the asset side too, so it needs the label, not just the rating. */
    @Test
    void riskCarriesTheAssetCriticalityLabelAlongsideItsRating() throws Exception {
        long riskId = risk(criticalAsset("Labelled asset"), threat13("Labelled threat"), "Labelled");

        JsonNode r = getRisk(riskId);
        assertThat(r.get("assetCriticality").asText()).isEqualTo("Критичная");
        assertThat(r.get("assetRating").asInt()).isEqualTo(5);
    }

    /**
     * ACCEPTANCE CRITERION 3, end to end.
     *
     * <p>Rating-5 asset x a threat scoring 13:
     * inherent 13 -> rating 3; implemented -20% -> 10.4 -> rating 2;
     * planned -20% then -50% -> 4.16 -> rating 1.
     */
    @Test
    void goldenScenarioFromTheSpecification() throws Exception {
        long assetId = criticalAsset("Golden asset");
        long threatId = threat13("Golden threat");
        long riskId = risk(assetId, threatId, "Golden risk");

        JsonNode r = getRisk(riskId);

        // Before any control: a=5, t=3 -> p=15, t>2 and p>=10 -> Высокий.
        assertThat(r.get("inherent").get("threatRating").asInt()).isEqualTo(3);
        assertThat(r.get("inherent").get("riskLabel").asText()).isEqualTo("Высокий");
        // With no controls, current and residual equal the inherent stage.
        assertThat(r.get("current").get("score").decimalValue()).isEqualByComparingTo("13");
        assertThat(r.get("current").get("threatRating").asInt()).isEqualTo(3);
        assertThat(r.get("residual").get("threatRating").asInt()).isEqualTo(3);

        // Implemented control, 20%: 13 -> 10.4 -> rating 2.
        long implemented = control("Implemented 20%", "0.20");
        attach(riskId, implemented, "IMPLEMENTED");

        r = getRisk(riskId);
        assertThat(r.get("current").get("score").decimalValue()).isEqualByComparingTo("10.4");
        assertThat(r.get("current").get("threatRating").asInt()).isEqualTo(2);
        // a=5, t=2 -> p=10, but t>2 is false -> Средний, not Высокий.
        assertThat(r.get("current").get("riskLabel").asText()).isEqualTo("Средний");
        // Inherent must NOT move: it is the "before controls" baseline.
        assertThat(r.get("inherent").get("threatRating").asInt()).isEqualTo(3);

        // Planned 20% then 50%: 10.4 -> 8.32 -> 4.16 -> rating 1.
        attach(riskId, control("Planned 20%", "0.20"), "PLANNED");
        attach(riskId, control("Planned 50%", "0.50"), "PLANNED");

        r = getRisk(riskId);
        assertThat(r.get("residual").get("score").decimalValue()).isEqualByComparingTo("4.16");
        assertThat(r.get("residual").get("threatRating").asInt()).isEqualTo(1);
        // a=5, t=1 -> p=5 -> the LOW branch (t<4 and 3<p<6).
        assertThat(r.get("residual").get("riskLabel").asText()).isEqualTo("Низкий");

        // Current is unaffected by planned work - that is the whole point of
        // keeping the two stages apart.
        assertThat(r.get("current").get("score").decimalValue()).isEqualByComparingTo("10.4");

        assertThat(r.get("implementedControls")).hasSize(1);
        assertThat(r.get("plannedControls")).hasSize(2);
    }

    @Test
    void detachingAControlRestoresTheScore() throws Exception {
        long riskId = risk(criticalAsset("Detach asset"), threat13("Detach threat"), "Detach risk");
        long controlId = control("Detachable 50%", "0.50");
        attach(riskId, controlId, "IMPLEMENTED");

        JsonNode r = getRisk(riskId);
        assertThat(r.get("current").get("score").decimalValue()).isEqualByComparingTo("6.5");
        long linkId = r.get("implementedControls").get(0).get("linkId").asLong();

        JsonNode after = json.readTree(mvc.perform(
                        delete("/api/risks/" + riskId + "/controls/" + linkId)
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        assertThat(after.get("current").get("score").decimalValue()).isEqualByComparingTo("13");
        assertThat(after.get("implementedControls")).isEmpty();
    }

    // ------------------------------------------------ recalculation triggers

    @Test
    void editingTheThreatRecalculatesItsRisks() throws Exception {
        long threatId = threat13("Mutable threat");
        long riskId = risk(criticalAsset("Trigger asset A"), threatId, "Threat-trigger risk");

        assertThat(getRisk(riskId).get("current").get("threatRating").asInt()).isEqualTo(3);

        // Raise every criterion to 5 -> score 25 -> rating 5.
        putJson("/api/threats/" + threatId, """
                {"description":"Mutable threat","discoverability":5,"repeatability":5,
                 "exploitability":5,"affectedUsers":5,"damage":5}""");

        JsonNode r = getRisk(riskId);
        assertThat(r.get("current").get("score").decimalValue()).isEqualByComparingTo("25");
        assertThat(r.get("current").get("threatRating").asInt()).isEqualTo(5);
        // a=5, t=5 -> p=25 -> Критический.
        assertThat(r.get("current").get("riskLabel").asText()).isEqualTo("Критический");
    }

    @Test
    void editingTheAssetCriticalityRecalculatesItsRisks() throws Exception {
        long assetId = criticalAsset("Mutable asset");
        long riskId = risk(assetId, threat13("Asset-trigger threat"), "Asset-trigger risk");

        // a=5, t=3 -> Высокий
        assertThat(getRisk(riskId).get("current").get("riskLabel").asText()).isEqualTo("Высокий");

        putJson("/api/assets/" + assetId, """
                {"name":"Mutable asset","criticality":"Очень низкая"}""");

        JsonNode r = getRisk(riskId);
        assertThat(r.get("assetRating").asInt()).isEqualTo(1);
        // a=1, t=3 -> the "t==3 && a<3" branch -> Низкий.
        assertThat(r.get("current").get("riskLabel").asText()).isEqualTo("Низкий");
    }

    @Test
    void editingAControlPercentageRecalculatesEveryRiskUsingIt() throws Exception {
        long controlId = control("Shared control", "0.20");

        long riskA = risk(criticalAsset("Shared A"), threat13("Shared threat A"), "Shared risk A");
        long riskB = risk(criticalAsset("Shared B"), threat13("Shared threat B"), "Shared risk B");
        attach(riskA, controlId, "IMPLEMENTED");
        attach(riskB, controlId, "IMPLEMENTED");

        assertThat(getRisk(riskA).get("current").get("score").decimalValue())
                .isEqualByComparingTo("10.4");

        // 20% -> 60%: 13 * 0.4 = 5.2 for BOTH risks.
        putJson("/api/controls/" + controlId, """
                {"name":"Shared control","treatmentMethod":"Снижение",
                 "reductionPct":0.60,"implemented":false}""");

        assertThat(getRisk(riskA).get("current").get("score").decimalValue())
                .isEqualByComparingTo("5.2");
        assertThat(getRisk(riskB).get("current").get("score").decimalValue())
                .isEqualByComparingTo("5.2");
    }

    // --------------------------------------------------------- constraints

    @Test
    void duplicateAssetThreatPairIsRejected() throws Exception {
        long assetId = criticalAsset("Dup asset");
        long threatId = threat13("Dup threat");
        risk(assetId, threatId, "First");

        mvc.perform(post("/api/risks")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetId":%d,"threatId":%d,"name":"Second"}"""
                                .formatted(assetId, threatId)))
                .andExpect(status().isConflict());
    }

    @Test
    void attachingTheSameControlTwiceIsRejected() throws Exception {
        long riskId = risk(criticalAsset("Twice asset"), threat13("Twice threat"), "Twice risk");
        long controlId = control("Only once", "0.10");
        attach(riskId, controlId, "IMPLEMENTED");

        mvc.perform(post("/api/risks/" + riskId + "/controls")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"controlId":%d,"type":"PLANNED"}""".formatted(controlId)))
                .andExpect(status().isConflict());
    }

    /** Excel caps controls at 7 implemented + 5 planned; this must not. */
    @Test
    void moreThanSevenImplementedControlsAreSupported() throws Exception {
        long riskId = risk(criticalAsset("Many asset"), threat13("Many threat"), "Many-control risk");

        for (int i = 1; i <= 9; i++) {
            attach(riskId, control("Bulk control " + i, "0.10"), "IMPLEMENTED");
        }

        JsonNode r = getRisk(riskId);
        assertThat(r.get("implementedControls")).hasSize(9);
        // 13 * 0.9^9 = 5.036466357, stored at scale 4 -> 5.0365. Below 6, so
        // rating 1. Nine controls on one risk is impossible in the workbook,
        // which stops at seven columns.
        assertThat(r.get("current").get("score").decimalValue())
                .isEqualByComparingTo("5.0365");
        assertThat(r.get("current").get("threatRating").asInt()).isEqualTo(1);
    }

    @Test
    void deletingARiskRemovesItsControlLinks() throws Exception {
        long riskId = risk(criticalAsset("Cascade asset"), threat13("Cascade threat"), "Cascade risk");
        attach(riskId, control("Cascade control", "0.25"), "IMPLEMENTED");

        mvc.perform(delete("/api/risks/" + riskId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/risks/" + riskId).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void attachingControlsNeedsTheRiskControlsGrant() throws Exception {
        long riskId = risk(criticalAsset("Perm asset"), threat13("Perm threat"), "Perm risk");
        long controlId = control("Perm control", "0.10");

        mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"risk-reader","password":"secret123",
                                 "fullName":"Risk Reader","role":"USER"}"""))
                .andExpect(status().isCreated());

        String token = login("risk-reader", "secret123");

        mvc.perform(get("/api/risks/" + riskId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(post("/api/risks/" + riskId + "/controls")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"controlId":%d,"type":"IMPLEMENTED"}""".formatted(controlId)))
                .andExpect(status().isForbidden());
    }
}
