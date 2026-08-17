package uz.infosec.risk.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Demo seeding, and message localisation driven by Accept-Language.
 *
 * <p><b>Why the datasource override.</b> @SpringBootTest caches one Spring
 * context - and therefore one in-memory database - across every test class in
 * the run. Seeding refuses to touch a non-empty database, so it would fail on
 * whatever another class inserted first; and worse, a successful seed would
 * leak six risks with past deadlines into AnalyticsTest's overdue count.
 *
 * <p>Adding a property changes the context cache key, so this class gets a
 * fresh context with a database of its own. Cheaper and far less brittle than
 * @DirtiesContext, which would rebuild the context for everyone.
 *
 * <p>The first two methods are explicitly ordered. "Refuses a second seed" can
 * only be checked once something has seeded, so the dependency is real - and
 * stating it with @Order beats relying on JUnit's unspecified method order.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:demo-locale-test;MODE=PostgreSQL;"
                + "DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class DemoDataAndLocaleTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String login() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    @Order(1)
    void seedFillsEveryRegistryAndComputesRiskLevels() throws Exception {
        String token = login();

        String body = mvc.perform(post("/api/admin/demo-data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode summary = json.readTree(body);
        assertThat(summary.get("infoSystems").asInt()).isEqualTo(3);
        assertThat(summary.get("assets").asInt()).isEqualTo(4);
        assertThat(summary.get("threats").asInt()).isEqualTo(6);
        assertThat(summary.get("controls").asInt()).isEqualTo(8);
        assertThat(summary.get("risks").asInt()).isEqualTo(6);
        assertThat(summary.get("controlLinks").asInt()).isGreaterThan(0);

        // Seeding went through the real services, so the engine ran: every risk
        // must come back fully classified, not with null snapshot columns.
        String risks = mvc.perform(get("/api/risks?size=50")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode page = json.readTree(risks);
        assertThat(page.get("totalElements").asInt()).isEqualTo(6);
        for (JsonNode r : page.get("content")) {
            assertThat(r.get("current").get("riskLevel").isNull())
                    .as("risk %s has a computed level", r.get("code").asText()).isFalse();
            assertThat(r.get("residual").get("riskLevel").isNull()).isFalse();
            assertThat(r.get("assetRating").asInt()).isBetween(1, 5);
        }

        // R1 is the workbook's golden row: threat 13, one implemented control
        // at 20% -> 10.4, two planned at 20% and 50% -> 4.16.
        JsonNode r1 = null;
        for (JsonNode r : page.get("content")) {
            if ("R1".equals(r.get("code").asText())) {
                r1 = r;
            }
        }
        assertThat(r1).isNotNull();
        assertThat(r1.get("current").get("score").decimalValue()).isEqualByComparingTo("10.4");
        assertThat(r1.get("residual").get("score").decimalValue()).isEqualByComparingTo("4.16");
    }

    @Test
    @Order(2)
    void seedingTwiceIsRefusedRatherThanDuplicating() throws Exception {
        String token = login();

        // @Order(1) already seeded, so this call must be refused.
        String body = mvc.perform(post("/api/admin/demo-data")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        // The message is resolved from the bundle, not the raw key.
        assertThat(json.readTree(body).get("message").asText())
                .isNotEqualTo("demo.dataNotEmpty")
                .contains("Демо-данные");
    }

    /**
     * Regression: the guard used to count info_systems too. Because that
     * registry had no screen, clearing every visible registry still left the
     * seed blocked forever with nothing the user could delete to fix it.
     *
     * <p>Re-seeding must also reuse the existing inventory rather than piling
     * up a second copy of it.
     */
    @Test
    @Order(3)
    void reseedingWorksAfterClearingTheVisibleRegistries() throws Exception {
        String token = login();

        // @Order(1) seeded. Delete everything a user can reach in the UI,
        // deliberately leaving the info systems behind.
        for (String resource : new String[]{"risks", "assets", "threats", "controls"}) {
            String body = mvc.perform(get("/api/" + resource + "?size=200")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            for (JsonNode row : json.readTree(body).get("content")) {
                mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/" + resource + "/" + row.get("id").asLong())
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isNoContent());
            }
        }

        long systemsLeft = json.readTree(mvc.perform(get("/api/info-systems?size=1")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("totalElements").asLong();
        assertThat(systemsLeft).as("info systems survive; they are what used to block the seed")
                .isGreaterThan(0);

        // The seed must now succeed rather than reporting "data already exists".
        mvc.perform(post("/api/admin/demo-data").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        // ...and it must not have duplicated the inventory.
        long systemsAfter = json.readTree(mvc.perform(get("/api/info-systems?size=1")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("totalElements").asLong();
        assertThat(systemsAfter).isEqualTo(systemsLeft);
    }

    @Test
    void nonAdminCannotSeed() throws Exception {
        String admin = login();
        mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo-blocked","password":"secret123",
                                 "fullName":"Demo Blocked","role":"USER"}"""))
                .andExpect(status().isCreated());

        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo-blocked","password":"secret123"}"""))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("token").asText();

        mvc.perform(post("/api/admin/demo-data").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ locale

    @Test
    void validationMessagesFollowAcceptLanguage() throws Exception {
        String token = login();

        String ru = mvc.perform(post("/api/threats")
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"","discoverability":9,"repeatability":1,
                                 "exploitability":1,"affectedUsers":1,"damage":1}"""))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(ru).get("message").asText()).isEqualTo("Проверка не пройдена");
        assertThat(ru).contains("не больше 5");

        String uz = mvc.perform(post("/api/threats")
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "uz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"","discoverability":9,"repeatability":1,
                                 "exploitability":1,"affectedUsers":1,"damage":1}"""))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(uz).get("message").asText()).isEqualTo("Tekshiruvdan o'tmadi");
        // Placeholder substitution must survive translation.
        assertThat(uz).contains("5 dan ko'p bo'lmasligi kerak");
        assertThat(uz).contains("Maydonni to'ldirish shart");
    }

    @Test
    void notFoundMessageTranslatesBothTheTemplateAndTheEntityName() throws Exception {
        String token = login();

        String ru = mvc.perform(get("/api/assets/999999")
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(ru).get("message").asText()).isEqualTo("Актив 999999 не найден");

        String uz = mvc.perform(get("/api/assets/999999")
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "uz"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        // Both the sentence and the nested entity name switched language.
        assertThat(json.readTree(uz).get("message").asText()).isEqualTo("Aktiv 999999 topilmadi");
    }

    @Test
    void unknownLanguageFallsBackToRussian() throws Exception {
        String token = login();

        String body = mvc.perform(get("/api/assets/999999")
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "de"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(body).get("message").asText()).isEqualTo("Актив 999999 не найден");
    }

    @Test
    void badCredentialsMessageIsLocalised() throws Exception {
        String uz = mvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "uz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong"}"""))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(uz).get("message").asText())
                .isEqualTo("Login yoki parol noto'g'ri");
    }

    /**
     * A mistyped URL is the client's mistake, not the server's. Before this was
     * handled it fell through to the catch-all and returned 500 - which tells
     * the caller to retry, and writes a stack trace for every scanner probe.
     */
    @Test
    void unknownEndpointIsNotFoundRatherThanInternalError() throws Exception {
        String token = login();

        String body = mvc.perform(get("/api/dictionaries/ASSET_CRITICALITY")
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "uz"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(body).get("message").asText())
                .isEqualTo("Bunday manzil mavjud emas");
    }

    /** Right path, wrong verb: /api/dictionaries exists, but only for GET and PUT. */
    @Test
    void wrongHttpMethodIsMethodNotAllowed() throws Exception {
        String token = login();

        String body = mvc.perform(post("/api/dictionaries")
                        .header("Authorization", "Bearer " + token)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "ru")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(body).get("message").asText())
                .isEqualTo("Этот метод здесь не поддерживается");
    }
}
