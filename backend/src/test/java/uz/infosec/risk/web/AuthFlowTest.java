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

import static uz.infosec.risk.TestCredentials.ADMIN_PASSWORD;
import static uz.infosec.risk.TestCredentials.ADMIN_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end HTTP tests for authentication and the permission grid.
 *
 * <p>@AutoConfigureMockMvc gives us a fake HTTP client that drives the real
 * filter chain, controllers and database - no network port involved, so tests
 * run in milliseconds and cannot collide on a port.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

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
    void seededAdminCanLogInAndReceivesFullPermissionGrid() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}"""
                                .formatted(ADMIN_USERNAME, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();

        JsonNode permissions = json.readTree(body).get("permissions");
        assertThat(permissions).hasSize(7);
        permissions.forEach(p -> {
            assertThat(p.get("canCreate").asBoolean()).isTrue();
            assertThat(p.get("canDelete").asBoolean()).isTrue();
        });
    }

    @Test
    void wrongPasswordIs401AndDoesNotRevealWhetherUserExists() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Неверный логин или пароль"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ghost","password":"wrong"}"""))
                .andExpect(status().isUnauthorized())
                // Identical message: no account enumeration.
                .andExpect(jsonPath("$.message").value("Неверный логин или пароль"));
    }

    @Test
    void protectedEndpointRequiresToken() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsCurrentUserFromToken() throws Exception {
        String token = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    void newUserDefaultsToReadOnlyEverywhere() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        String created = mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"analyst","password":"secret123",
                                 "fullName":"Risk Analyst","email":"analyst@bank.local","role":"USER"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = json.readTree(created).get("id").asLong();

        String grid = mvc.perform(get("/api/admin/users/" + id + "/permissions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode permissions = json.readTree(grid);
        assertThat(permissions).hasSize(7);
        permissions.forEach(p -> {
            assertThat(p.get("canRead").asBoolean())
                    .as("read is granted by default on %s", p.get("module").asText()).isTrue();
            assertThat(p.get("canCreate").asBoolean()).isFalse();
            assertThat(p.get("canUpdate").asBoolean()).isFalse();
            assertThat(p.get("canDelete").asBoolean()).isFalse();
        });
    }

    @Test
    void nonAdminCannotReachAdminEndpoints() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"plainuser","password":"secret123",
                                 "fullName":"Plain User","role":"USER"}"""))
                .andExpect(status().isCreated());

        String userToken = login("plainuser", "secret123");

        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivatedUserCannotLogInAndExistingTokenStopsWorking() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        String created = mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"leaver","password":"secret123",
                                 "fullName":"Departing Employee","role":"USER"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        // Token issued while the account was still active.
        String leaverToken = login("leaver", "secret123");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + leaverToken))
                .andExpect(status().isOk());

        mvc.perform(put("/api/admin/users/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}"""))
                .andExpect(status().isOk());

        // Cannot obtain a new token...
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"leaver","password":"secret123"}"""))
                .andExpect(status().isUnauthorized());

        // ...and the token they already hold is rejected too, because the JWT
        // filter re-checks `active` against the database on every request.
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + leaverToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validationFailureReturnsFieldErrors() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        mvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"x","password":"short","fullName":"","role":"USER"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Проверка не пройдена"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    void lastActiveAdminCannotBeDeactivated() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        String body = mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();

        // Find the admin explicitly - findAll() gives no ordering guarantee.
        long adminId = -1;
        for (JsonNode u : json.readTree(body)) {
            if ("admin".equals(u.get("username").asText())) {
                adminId = u.get("id").asLong();
            }
        }
        assertThat(adminId).isPositive();

        mvc.perform(put("/api/admin/users/" + adminId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}"""))
                .andExpect(status().isConflict());
    }
}
