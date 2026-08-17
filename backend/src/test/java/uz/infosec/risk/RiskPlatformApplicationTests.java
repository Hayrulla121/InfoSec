package uz.infosec.risk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the whole Spring context. If any bean fails to wire, any Flyway
 * migration is broken, or an @Entity does not match its table, this test fails.
 * Cheap insurance - keep it green at all times.
 *
 * @ActiveProfiles("test") layers application-test.yml on top of application.yml,
 * swapping the file database for an in-memory one.
 */
@SpringBootTest
@ActiveProfiles("test")
class RiskPlatformApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: the assertion is "the context started".
    }
}
