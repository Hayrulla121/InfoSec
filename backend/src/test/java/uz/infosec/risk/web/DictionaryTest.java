package uz.infosec.risk.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uz.infosec.risk.domain.DictType;
import uz.infosec.risk.service.DictionaryService;

import static uz.infosec.risk.TestCredentials.ADMIN_PASSWORD;
import static uz.infosec.risk.TestCredentials.ADMIN_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DictionaryTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    DictionaryService dictionaryService;

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    @Test
    void seedMatchesTheExcelTechnicalSheet() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        String body = mvc.perform(get("/api/dictionaries").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode groups = json.readTree(body);
        assertThat(groups).hasSize(4);

        JsonNode criticality = groups.get(0);
        assertThat(criticality.get("dictType").asText()).isEqualTo("ASSET_CRITICALITY");
        assertThat(criticality.get("numericRequired").asBoolean()).isTrue();
        assertThat(criticality.get("items")).hasSize(5);
        assertThat(criticality.get("items").get(0).get("label").asText()).isEqualTo("Очень низкая");
        assertThat(criticality.get("items").get(4).get("label").asText()).isEqualTo("Критичная");
        assertThat(criticality.get("items").get(4).get("numericValue").asInt()).isEqualTo(5);
    }

    /** The lookup that replaces Excel's VLOOKUP into the Техническая страница. */
    @Test
    void criticalityLabelsResolveToTheExcelRatings() {
        assertThat(dictionaryService.numericValueOf(DictType.ASSET_CRITICALITY, "Очень низкая")).isEqualTo(1);
        assertThat(dictionaryService.numericValueOf(DictType.ASSET_CRITICALITY, "Низкая")).isEqualTo(2);
        assertThat(dictionaryService.numericValueOf(DictType.ASSET_CRITICALITY, "Средняя")).isEqualTo(3);
        assertThat(dictionaryService.numericValueOf(DictType.ASSET_CRITICALITY, "Высокая")).isEqualTo(4);
        assertThat(dictionaryService.numericValueOf(DictType.ASSET_CRITICALITY, "Критичная")).isEqualTo(5);
    }

    @Test
    void treatmentMethodsMatchTheWorkbook() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        String body = mvc.perform(get("/api/dictionaries").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();

        JsonNode methods = null;
        for (JsonNode g : json.readTree(body)) {
            if ("TREATMENT_METHOD".equals(g.get("dictType").asText())) {
                methods = g;
            }
        }
        assertThat(methods).isNotNull();
        assertThat(methods.get("items")).hasSize(4);
        assertThat(methods.get("items").get(0).get("label").asText()).isEqualTo("Снижение");
        assertThat(methods.get("items").get(3).get("label").asText()).isEqualTo("Принятие");
        // Methods carry no weight in the risk formula.
        assertThat(methods.get("items").get(0).get("numericValue").isNull()).isTrue();
    }

    @Test
    void adminCanAddAStatusAndItComesBackOrdered() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        String body = mvc.perform(put("/api/dictionaries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"MEASURE_STATUS","items":[
                                  {"id":null,"label":"Не начато","numericValue":null,"sortOrder":0},
                                  {"id":null,"label":"Выполнено","numericValue":null,"sortOrder":0}
                                ]}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode items = json.readTree(body).get("items");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("label").asText()).isEqualTo("Не начато");
        // sortOrder is assigned server-side from list position, never trusted
        // from the client.
        assertThat(items.get(0).get("sortOrder").asInt()).isEqualTo(1);
        assertThat(items.get(1).get("sortOrder").asInt()).isEqualTo(2);
    }

    @Test
    void duplicateLevelIsRejected() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        mvc.perform(put("/api/dictionaries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"ASSET_CRITICALITY","items":[
                                  {"id":null,"label":"Низкая","numericValue":2,"sortOrder":0},
                                  {"id":null,"label":"Средняя","numericValue":2,"sortOrder":0}
                                ]}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void criticalityWithoutLevelIsRejected() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        mvc.perform(put("/api/dictionaries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"ASSET_CRITICALITY","items":[
                                  {"id":null,"label":"Непонятная","numericValue":null,"sortOrder":0}
                                ]}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void emptyingADictionaryIsRejected() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        mvc.perform(put("/api/dictionaries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"TREATMENT_METHOD","items":[]}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void userWithoutUpdateGrantCannotEditDictionaries() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"dict-reader","password":"secret123",
                                 "fullName":"Dictionary Reader","role":"USER"}"""))
                .andExpect(status().isCreated());

        String userToken = login("dict-reader", "secret123");

        // Reading is open to any authenticated user...
        mvc.perform(get("/api/dictionaries").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // ...writing is not, because the default grid grants READ only.
        mvc.perform(put("/api/dictionaries")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dictType":"MEASURE_STATUS","items":[
                                  {"id":null,"label":"Взломано","numericValue":null,"sortOrder":0}
                                ]}"""))
                .andExpect(status().isForbidden());
    }
}
