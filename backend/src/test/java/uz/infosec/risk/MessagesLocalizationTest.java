package uz.infosec.risk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the two rules that make the message bundles trustworthy, without
 * starting Spring: these are assertions about two text files, so they run in
 * milliseconds and fail with a precise list rather than one broken endpoint.
 *
 * <p><b>Rule 1 — key parity.</b> A key missing from the Uzbek bundle does not
 * throw; it silently falls back to the Russian default. That is invisible in
 * testing and shows up in production as a Russian sentence in the middle of an
 * Uzbek screen, which is exactly the mixed-language problem the bundles exist
 * to remove.
 *
 * <p><b>Rule 2 — apostrophe escaping.</b> MessageFormat uses {@code '} as its
 * escape character. Uzbek is full of apostrophes (bog'langan, ma'lumot,
 * ro'yxat), so a lone one is swallowed on render and "bog'langan" is displayed
 * as "boglangan". The fix is to double it — but only where MessageFormat
 * actually runs, which is the subtlety worth a test:
 *
 * <ul>
 *   <li>positional placeholders ({@code {0}}) → MessageFormat runs → double it;
 *   <li>no placeholder → Spring skips MessageFormat entirely → leave it single;
 *   <li>named placeholders ({@code {min}}) → these are Bean Validation
 *       messages, interpolated by Hibernate Validator → leave it single.
 * </ul>
 *
 * Both directions are asserted, because over-escaping prints the apostrophe
 * twice and is just as wrong as under-escaping.
 */
class MessagesLocalizationTest {

    /** {0}, {1} … — the ones MessageFormat consumes. */
    private static final Pattern POSITIONAL = Pattern.compile("\\{\\d+}");

    /** A single ' that is not part of a doubled ''. */
    private static final Pattern LONE_APOSTROPHE = Pattern.compile("(?<!')'(?!')");

    private static Properties bundle(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = MessagesLocalizationTest.class.getResourceAsStream("/" + resource)) {
            assertThat(in).as("resource %s is on the test classpath", resource).isNotNull();
            // Spring reads these as UTF-8 (see ValidationMessagesConfig), so the
            // test must too - the default ISO-8859-1 would mangle Cyrillic.
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties;
    }

    @Test
    @DisplayName("every Russian key has an Uzbek translation, and vice versa")
    void bundlesHaveIdenticalKeySets() throws IOException {
        Properties ru = bundle("messages.properties");
        Properties uz = bundle("messages_uz.properties");

        assertThat(new TreeSet<>(uz.stringPropertyNames()))
                .as("keys present in Russian but missing from Uzbek fall back to Russian silently")
                .containsExactlyInAnyOrderElementsOf(new TreeSet<>(ru.stringPropertyNames()));

        assertThat(ru).as("bundle should not be empty - a wrong path would also pass every other check")
                .hasSizeGreaterThan(40);
    }

    @Test
    @DisplayName("apostrophes are doubled in MessageFormat patterns and single everywhere else")
    void apostrophesAreEscapedExactlyWhereMessageFormatRuns() throws IOException {
        List<String> underEscaped = new ArrayList<>();
        List<String> overEscaped = new ArrayList<>();

        for (String resource : List.of("messages.properties", "messages_uz.properties")) {
            Properties properties = bundle(resource);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                if (!value.contains("'")) {
                    continue;
                }
                String where = resource + " -> " + key + " = " + value;
                if (POSITIONAL.matcher(value).find()) {
                    if (LONE_APOSTROPHE.matcher(value).find()) {
                        underEscaped.add(where);
                    }
                } else if (value.contains("''")) {
                    overEscaped.add(where);
                }
            }
        }

        assertThat(underEscaped)
                .as("MessageFormat will swallow these apostrophes - double them ('')")
                .isEmpty();
        assertThat(overEscaped)
                .as("MessageFormat never runs on these, so '' is printed literally - use one")
                .isEmpty();
    }

    @Test
    @DisplayName("the escaped Uzbek patterns render with their apostrophes intact")
    void escapedPatternsSurviveRendering() throws IOException {
        Properties uz = bundle("messages_uz.properties");

        // The message that surfaced the bug: deleting an info system still
        // referenced by assets. It rendered as "boglangan" before the fix.
        String rendered = MessageFormat.format(uz.getProperty("infoSystem.linkedToAssets"), "ИС1");
        assertThat(rendered).isEqualTo("ИС1 tizimi aktivlarga bog'langan; avval ularni ajrating");

        // Two placeholders and three apostrophes in one pattern.
        assertThat(MessageFormat.format(uz.getProperty("dictionary.noSuchEntry"), "Aktiv turi", "Server"))
                .contains("ma'lumotnomasida")
                .doesNotContain("''");

        // And the other half of the rule: an unescaped, placeholder-free message
        // is passed through untouched, so its single apostrophes are correct.
        assertThat(uz.getProperty("demo.dataNotEmpty"))
                .contains("ma'lumotlar")
                .doesNotContain("''");
    }
}
