package uz.infosec.risk.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Phase 0 smoke test. Proves three things at once:
 * the web layer answers, the DataSource connects, and Flyway ran.
 *
 * @RestController = @Controller + @ResponseBody: every return value is
 * serialised straight to JSON by Jackson instead of being treated as a
 * view name.
 */
@RestController
@RequestMapping("/api")
public class PingController {

    private final JdbcTemplate jdbc;

    // Single constructor => Spring injects the JdbcTemplate automatically,
    // no @Autowired needed. Constructor injection keeps fields final and
    // makes the class trivially testable.
    public PingController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        // Flyway's own bookkeeping table. The first row can have a NULL version
        // (it records schema creation), hence COALESCE.
        List<String> migrations = jdbc.queryForList(
                "SELECT COALESCE(version, '-') || ' ' || description "
                        + "FROM flyway_schema_history ORDER BY installed_rank",
                String.class);

        return Map.of(
                "status", "UP",
                "time", Instant.now().toString(),
                "migrationsApplied", migrations
        );
    }
}
