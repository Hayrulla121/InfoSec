package uz.infosec.risk.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Points Bean Validation at messages.properties so constraint violations are
 * reported in Russian instead of Hibernate Validator's English defaults.
 *
 * <p>Both beans are needed: the MessageSource loads the file, and the
 * LocalValidatorFactoryBean tells the validator to resolve its messages
 * through that MessageSource rather than its own bundled one.
 */
@Configuration
public class ValidationMessagesConfig {

    /**
     * Picks the response language from the request's Accept-Language header.
     *
     * <p>Only ru and uz are offered; anything else falls back to Russian, which
     * is what messages.properties holds. Spring Boot would default to the JVM's
     * locale otherwise — meaning the language of the server, not the user.
     */
    @Bean
    public org.springframework.web.servlet.LocaleResolver localeResolver() {
        var resolver = new org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(java.util.List.of(
                java.util.Locale.of("ru"), java.util.Locale.of("uz")));
        resolver.setDefaultLocale(java.util.Locale.of("ru"));
        return resolver;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        // MUST stay false. Hibernate Validator resolves a placeholder like
        // {value} by first asking this bundle whether "value" is a message key
        // and expecting a MissingResourceException before it falls back to the
        // constraint's own attribute. With useCodeAsDefaultMessage=true the
        // bundle answers with the literal string "value", so the message reads
        // "не больше value" instead of "не больше 5".
        source.setUseCodeAsDefaultMessage(false);
        return source;
    }

    @Bean
    public LocalValidatorFactoryBean getValidator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
