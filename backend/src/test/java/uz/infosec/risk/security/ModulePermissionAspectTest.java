package uz.infosec.risk.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import uz.infosec.risk.domain.Action;
import uz.infosec.risk.domain.AppModule;

import static uz.infosec.risk.TestCredentials.ADMIN_PASSWORD;
import static uz.infosec.risk.TestCredentials.ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves @RequireModulePermission is actually enforced, using a throwaway
 * controller that exists only in this test's context.
 *
 * <p>Worth writing now rather than in Phase 3: if the aspect silently did not
 * fire (wrong retention policy, missing spring-boot-starter-aop, self-invocation),
 * every module built on top of it would be wide open, and nothing would fail.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ModulePermissionAspectTest.ProbeController.class)
class ModulePermissionAspectTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String adminToken;

    @TestConfiguration
    @RestController
    @RequestMapping("/api/test-probe")
    static class ProbeController {

        @GetMapping
        @RequireModulePermission(module = AppModule.RISKS, action = Action.READ)
        String read() {
            return "read-ok";
        }

        @PostMapping
        @RequireModulePermission(module = AppModule.RISKS, action = Action.CREATE)
        String create() {
            return "create-ok";
        }

        @DeleteMapping
        @RequireModulePermission(module = AppModule.RISKS, action = Action.DELETE)
        String delete() {
            return "delete-ok";
        }

        /** No annotation: reachable by any authenticated user. */
        @GetMapping("/open")
        String open() {
            return "open-ok";
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
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

    private long createUser(String username) throws Exception {
        String created = mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"secret123",
                                 "fullName":"Probe User","role":"USER"}""".formatted(username)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(created).get("id").asLong();
    }

    /** Grants exactly the listed actions on RISKS, nothing anywhere else. */
    private void grantOnRisks(long userId, boolean create, boolean read, boolean update, boolean delete)
            throws Exception {
        StringBuilder rows = new StringBuilder();
        for (AppModule m : AppModule.values()) {
            if (!rows.isEmpty()) {
                rows.append(",");
            }
            boolean isRisks = m == AppModule.RISKS;
            rows.append("""
                    {"module":"%s","canCreate":%s,"canRead":%s,"canUpdate":%s,"canDelete":%s}"""
                    .formatted(m,
                            isRisks && create, isRisks && read,
                            isRisks && update, isRisks && delete));
        }
        mvc.perform(put("/api/admin/users/" + userId + "/permissions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[" + rows + "]}"))
                .andExpect(status().isOk());
    }

    @Test
    void adminBypassesTheGridEntirely() throws Exception {
        mvc.perform(get("/api/test-probe").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/test-probe").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    /** The acceptance criterion from the spec: CREATE+UPDATE granted, DELETE not. */
    @Test
    void userGetsExactlyTheGrantedActions() throws Exception {
        long id = createUser("probe-partial");
        grantOnRisks(id, true, true, true, false);
        String token = login("probe-partial", "secret123");

        mvc.perform(get("/api/test-probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(post("/api/test-probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/test-probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokingReadBlocksEvenReading() throws Exception {
        long id = createUser("probe-noread");
        grantOnRisks(id, false, false, false, false);
        String token = login("probe-noread", "secret123");

        mvc.perform(get("/api/test-probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // ...but an unannotated endpoint stays reachable: the aspect only
        // guards what is explicitly marked.
        mvc.perform(get("/api/test-probe/open").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void deniedResponseUsesTheStandardErrorBody() throws Exception {
        long id = createUser("probe-shape");
        grantOnRisks(id, false, false, false, false);
        String token = login("probe-shape", "secret123");

        String body = mvc.perform(get("/api/test-probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        JsonNode error = json.readTree(body);
        org.assertj.core.api.Assertions.assertThat(error.get("status").asInt()).isEqualTo(403);
        // The denial names the missing grant, translated into the request's
        // language rather than echoing the enum constants.
        org.assertj.core.api.Assertions.assertThat(error.get("message").asText())
                .contains("просмотр").contains("Реестр рисков");
        org.assertj.core.api.Assertions.assertThat(error.has("timestamp")).isTrue();
    }
}
