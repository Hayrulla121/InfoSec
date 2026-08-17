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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end tests for the Assets / Threats / Controls registries. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistryTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = login("admin", "admin");
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

    private JsonNode createEntity(String url, String body) throws Exception {
        String res = mvc.perform(post(url)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res);
    }

    // ------------------------------------------------------------- threats

    /** Acceptance criterion 2 from the spec. */
    @Test
    void threatWithScores22432ShowsTotal13Rating3Sredniy() throws Exception {
        JsonNode t = createEntity("/api/threats", """
                {"description":"Некорректное прогнозирование потребности в мощности оборудования",
                 "discoverability":2,"repeatability":2,"exploitability":4,
                 "affectedUsers":3,"damage":2}""");

        assertThat(t.get("totalScore").asInt()).isEqualTo(13);
        assertThat(t.get("rating").asInt()).isEqualTo(3);
        assertThat(t.get("levelLabel").asText()).isEqualTo("Средний");
        assertThat(t.get("code").asText()).startsWith("У");
    }

    /** Threat У2 from the workbook: 3,1,1,2,2 -> 9 -> Низкий. */
    @Test
    void secondWorkbookThreatMatches() throws Exception {
        JsonNode t = createEntity("/api/threats", """
                {"description":"Изменения в организационной структуре",
                 "discoverability":3,"repeatability":1,"exploitability":1,
                 "affectedUsers":2,"damage":2}""");

        assertThat(t.get("totalScore").asInt()).isEqualTo(9);
        assertThat(t.get("rating").asInt()).isEqualTo(2);
        assertThat(t.get("levelLabel").asText()).isEqualTo("Низкий");
    }

    @Test
    void updatingScoresRecomputesEverything() throws Exception {
        JsonNode t = createEntity("/api/threats", """
                {"description":"tmp","discoverability":1,"repeatability":1,
                 "exploitability":1,"affectedUsers":1,"damage":1}""");
        assertThat(t.get("totalScore").asInt()).isEqualTo(5);
        assertThat(t.get("rating").asInt()).isEqualTo(1);

        String res = mvc.perform(put("/api/threats/" + t.get("id").asLong())
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"tmp","discoverability":5,"repeatability":5,
                                 "exploitability":5,"affectedUsers":5,"damage":5}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode updated = json.readTree(res);
        assertThat(updated.get("totalScore").asInt()).isEqualTo(25);
        assertThat(updated.get("rating").asInt()).isEqualTo(5);
        assertThat(updated.get("levelLabel").asText()).isEqualTo("Очень высокий");
    }

    @Test
    void dreadScoreAboveFiveIsRejected() throws Exception {
        mvc.perform(post("/api/threats")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"bad","discoverability":9,"repeatability":1,
                                 "exploitability":1,"affectedUsers":1,"damage":1}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("discoverability"));
    }

    /**
     * Validation messages are Russian AND their placeholders are substituted.
     *
     * <p>The second half matters: a misconfigured MessageSource silently
     * renders "не больше value" instead of "не больше 5", which still returns
     * 400 and would pass a status-only assertion.
     */
    @Test
    void validationMessagesAreLocalisedWithRealValues() throws Exception {
        String body = mvc.perform(post("/api/threats")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"","discoverability":9,"repeatability":1,
                                 "exploitability":1,"affectedUsers":1,"damage":1}"""))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        JsonNode error = json.readTree(body);
        assertThat(error.get("message").asText()).isEqualTo("Проверка не пройдена");

        String messages = error.get("fieldErrors").toString();
        assertThat(messages).contains("Поле обязательно для заполнения");
        assertThat(messages).contains("не больше 5");
        assertThat(messages).doesNotContain("value");
    }

    /** Computed fields are server-owned: a client cannot dictate them. */
    @Test
    void clientSuppliedRatingIsIgnored() throws Exception {
        JsonNode t = createEntity("/api/threats", """
                {"description":"trying to cheat","discoverability":1,"repeatability":1,
                 "exploitability":1,"affectedUsers":1,"damage":1,
                 "totalScore":25,"rating":5,"levelLabel":"Очень высокий"}""");

        assertThat(t.get("totalScore").asInt()).isEqualTo(5);
        assertThat(t.get("rating").asInt()).isEqualTo(1);
        assertThat(t.get("levelLabel").asText()).isEqualTo("Незначительный");
    }

    // -------------------------------------------------------------- assets

    @Test
    void assetCriticalityResolvesToRatingFromTheDictionary() throws Exception {
        JsonNode a = createEntity("/api/assets", """
                {"name":"Государственный реестр кредитной информации",
                 "scope":"В масштабе республики",
                 "infoCategory":"Konfidensial ma'lumot",
                 "criticality":"Критичная","securityClass":"IS4"}""");

        assertThat(a.get("criticalityRating").asInt()).isEqualTo(5);
        assertThat(a.get("code").asText()).startsWith("КИА");

        JsonNode low = createEntity("/api/assets", """
                {"name":"Второстепенная система","criticality":"Низкая"}""");
        assertThat(low.get("criticalityRating").asInt()).isEqualTo(2);
    }

    @Test
    void unknownCriticalityIsRejected() throws Exception {
        mvc.perform(post("/api/assets")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"X","criticality":"Небывалая"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void assetCodesIncrement() throws Exception {
        JsonNode first = createEntity("/api/assets", """
                {"name":"A","criticality":"Средняя"}""");
        JsonNode second = createEntity("/api/assets", """
                {"name":"B","criticality":"Средняя"}""");

        int firstNum = codeNumber(first);
        assertThat(codeNumber(second)).isEqualTo(firstNum + 1);
    }

    /**
     * The reason the rule is "max + 1" and not "count + 1".
     *
     * <p>Delete a code from the MIDDLE of the range and the count drops, so
     * count+1 would regenerate a number that is still in use and hit the UNIQUE
     * constraint. Taking the maximum sidesteps that entirely.
     *
     * <p>(Deleting the highest code does free that number for reuse - that is
     * what the spec asks for, and it is harmless because nothing still holds it.)
     */
    @Test
    void deletingAMiddleCodeDoesNotCauseACollision() throws Exception {
        JsonNode a = createEntity("/api/assets", """
                {"name":"gap-A","criticality":"Средняя"}""");
        JsonNode b = createEntity("/api/assets", """
                {"name":"gap-B","criticality":"Средняя"}""");
        JsonNode c = createEntity("/api/assets", """
                {"name":"gap-C","criticality":"Средняя"}""");

        assertThat(codeNumber(b)).isEqualTo(codeNumber(a) + 1);
        assertThat(codeNumber(c)).isEqualTo(codeNumber(b) + 1);

        // Remove the middle one.
        mvc.perform(delete("/api/assets/" + b.get("id").asLong())
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());

        // count+1 would have produced c's number again and thrown a 409.
        JsonNode d = createEntity("/api/assets", """
                {"name":"gap-D","criticality":"Средняя"}""");
        assertThat(codeNumber(d)).isEqualTo(codeNumber(c) + 1);
    }

    private int codeNumber(JsonNode asset) {
        return Integer.parseInt(asset.get("code").asText().substring("КИА".length()));
    }

    // ------------------------------------------------------------ controls

    @Test
    void controlStoresReductionAsExactDecimal() throws Exception {
        JsonNode c = createEntity("/api/controls", """
                {"name":"Регулярный аудит текущей и прогнозной вычислительной нагрузки",
                 "description":"Аудит нагрузки","treatmentMethod":"Снижение",
                 "reductionPct":0.2,"implemented":true}""");

        assertThat(c.get("reductionPct").decimalValue()).isEqualByComparingTo("0.20");
        assertThat(c.get("code").asText()).startsWith("C");
        assertThat(c.get("implemented").asBoolean()).isTrue();
    }

    @Test
    void reductionOutsideZeroToOneIsRejected() throws Exception {
        mvc.perform(post("/api/controls")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Too strong","treatmentMethod":"Снижение",
                                 "reductionPct":1.5,"implemented":false}"""))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/controls")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Negative","treatmentMethod":"Снижение",
                                 "reductionPct":-0.1,"implemented":false}"""))
                .andExpect(status().isBadRequest());
    }

    // --------------------------------------------------- audit & permission

    @Test
    void auditColumnsRecordTheActingUser() throws Exception {
        JsonNode c = createEntity("/api/controls", """
                {"name":"Audited control","treatmentMethod":"Снижение",
                 "reductionPct":0.3,"implemented":false}""");

        assertThat(c.get("createdBy").asText()).isEqualTo("admin");
        assertThat(c.get("createdAt").isNull()).isFalse();
    }

    @Test
    void searchFiltersAcrossTextColumns() throws Exception {
        createEntity("/api/assets", """
                {"name":"Уникальное имя системы Alpha","criticality":"Высокая"}""");

        String res = mvc.perform(get("/api/assets")
                        .header("Authorization", "Bearer " + admin)
                        .param("search", "alpha"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode page = json.readTree(res);
        assertThat(page.get("totalElements").asInt()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("name").asText()).contains("Alpha");
    }

    @Test
    void readOnlyUserCannotWriteToRegistries() throws Exception {
        mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"registry-reader","password":"secret123",
                                 "fullName":"Registry Reader","role":"USER"}"""))
                .andExpect(status().isCreated());

        String token = login("registry-reader", "secret123");

        mvc.perform(get("/api/threats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(post("/api/threats")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"nope","discoverability":1,"repeatability":1,
                                 "exploitability":1,"affectedUsers":1,"damage":1}"""))
                .andExpect(status().isForbidden());
    }
}
