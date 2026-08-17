package uz.infosec.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the backend.
 *
 * {@code @SpringBootApplication} is three annotations in one:
 *  - @Configuration      : this class may declare @Bean methods
 *  - @EnableAutoConfiguration : Spring inspects the classpath and wires up
 *                          Tomcat, Hibernate, Flyway, Security... automatically
 *  - @ComponentScan      : scan this package and everything below it for
 *                          @Component / @Service / @Repository / @RestController
 */
@SpringBootApplication
public class RiskPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskPlatformApplication.class, args);
    }
}
