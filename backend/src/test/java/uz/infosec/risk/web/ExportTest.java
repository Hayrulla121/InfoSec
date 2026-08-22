package uz.infosec.risk.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static uz.infosec.risk.TestCredentials.ADMIN_PASSWORD;
import static uz.infosec.risk.TestCredentials.ADMIN_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExportTest {

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

    private String download(String url) throws Exception {
        byte[] bytes = mvc.perform(get(url).header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andReturn().getResponse().getContentAsByteArray();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Test
    void threatExportStartsWithABomAndUsesSemicolons() throws Exception {
        mvc.perform(post("/api/threats")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Экспортируемая угроза","discoverability":2,
                                 "repeatability":2,"exploitability":4,"affectedUsers":3,"damage":2}"""))
                .andExpect(status().isCreated());

        String csv = download("/api/export/threats");

        // Without the BOM, Excel on Windows mangles every Cyrillic heading.
        assertThat(csv).startsWith("﻿");
        assertThat(csv).contains("Обнаружение;Повторение;Эксплуатирование");
        assertThat(csv).contains("Экспортируемая угроза");
        // Computed columns travel with the export.
        assertThat(csv).contains(";13;3;Средний");
        assertThat(csv).contains("\r\n");
    }

    /**
     * Threat descriptions and control names routinely contain the separator,
     * so RFC 4180 quoting is not optional.
     */
    @Test
    void valuesContainingTheSeparatorAreQuoted() throws Exception {
        mvc.perform(post("/api/controls")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Контроль; с точкой с запятой и \\"кавычками\\"",
                                 "treatmentMethod":"Снижение","reductionPct":0.25,
                                 "implemented":false}"""))
                .andExpect(status().isCreated());

        String csv = download("/api/export/controls");

        assertThat(csv).contains("\"Контроль; с точкой с запятой и \"\"кавычками\"\"\"");
        // Percentages use a comma decimal mark for these Excel locales.
        assertThat(csv).contains("0,25");
    }

    @Test
    void riskExportRebuildsTheTextJoinColumns() throws Exception {
        long assetId = json.readTree(mvc.perform(post("/api/assets")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Экспорт актив","criticality":"Критичная"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        long threatId = json.readTree(mvc.perform(post("/api/threats")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Экспорт угроза","discoverability":2,"repeatability":2,
                                 "exploitability":4,"affectedUsers":3,"damage":2}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        long riskId = json.readTree(mvc.perform(post("/api/risks")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetId":%d,"threatId":%d,"name":"Экспорт риск"}"""
                                .formatted(assetId, threatId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        long controlId = json.readTree(mvc.perform(post("/api/controls")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Экспортируемый контроль","treatmentMethod":"Снижение",
                                 "reductionPct":0.2,"implemented":true}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/risks/" + riskId + "/controls")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"controlId":%d,"type":"IMPLEMENTED"}""".formatted(controlId)))
                .andExpect(status().isOk());

        String csv = download("/api/export/risks");

        assertThat(csv).contains("Экспорт риск");
        // The joined control-name column, the equivalent of Excel's TEXTJOIN.
        assertThat(csv).contains("Экспортируемый контроль");
        // Computed labels, so the exported file needs no formulas at all.
        assertThat(csv).contains("Средний");
    }

    @Test
    void exportRequiresReadOnTheSameModule() throws Exception {
        mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"export-blocked","password":"secret123",
                                 "fullName":"Export Blocked","role":"USER"}"""))
                .andExpect(status().isCreated());

        String userToken = login("export-blocked", "secret123");

        // Default grid grants READ, so this works...
        mvc.perform(get("/api/export/threats").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // ...now revoke THREATS read entirely.
        String usersJson = mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        long userId = -1;
        for (var u : json.readTree(usersJson)) {
            if ("export-blocked".equals(u.get("username").asText())) {
                userId = u.get("id").asLong();
            }
        }

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/users/" + userId + "/permissions")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissions":[
                                  {"module":"THREATS","canCreate":false,"canRead":false,
                                   "canUpdate":false,"canDelete":false}]}"""))
                .andExpect(status().isOk());

        mvc.perform(get("/api/export/threats").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}
