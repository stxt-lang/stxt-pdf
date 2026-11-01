package dev.stxt.pdf.layout.inline.hyphenation;

import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.PropertyKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The hyphenation patterns the engine has, by language (reference/common/typography.md): the patterns of hyph-utf8
 * for Spanish, Catalan, American English and British English, as resources of the jar
 * (reference/common/resources.md). Each one is read the first time a document in its language asks for it and is
 * shared from then on.
 *
 * <p>The language of a text is the {@link Locale} of its {@code Typography} ({@code Language}): the region chooses
 * between American and British English ({@code en-GB} is British, any other English is American), and Spanish and
 * Catalan have one set of patterns whatever the region. A text with {@code Hyphenate: true} in another language is
 * an error ({@link ErrorCode#HYPHENATION_LANGUAGE_UNSUPPORTED}, checked before the layout by {@link #validate}).</p>
 */
public final class Hyphenations {

    /** Resource of each language, by the key of {@link #key}. */
    private static final Map<String, String> RESOURCES = Map.of(
            "es", "es.txt",
            "ca", "ca.txt",
            "en-US", "en-us.txt",
            "en-GB", "en-gb.txt");

    /** The supported languages for the messages, in the order of the documentation. */
    private static final String SUPPORTED = "es, ca, en-US and en-GB";

    private static final ConcurrentHashMap<String, Hyphenator> LOADED = new ConcurrentHashMap<>();

    private Hyphenations() {
    }

    /** Whether the engine has patterns for the language. */
    public static boolean supports(Locale locale) {
        return key(locale) != null;
    }

    /**
     * The hyphenator of the language; {@code null} if the engine has no patterns for it ({@link #supports}).
     */
    public static Hyphenator forLocale(Locale locale) {
        String key = key(locale);
        return key == null ? null : LOADED.computeIfAbsent(key, Hyphenations::load);
    }

    /** The key of the patterns of a language, or {@code null} if there are none. */
    private static String key(Locale locale) {
        String language = locale.getLanguage();
        switch (language) {
            case "es", "ca":
                return language;
            case "en":
                return locale.getCountry().equals("GB") ? "en-GB" : "en-US";
            default:
                return null;
        }
    }

    private static Hyphenator load(String key) {
        String name = RESOURCES.get(key);
        try (InputStream in = Hyphenations.class.getResourceAsStream(resourcePath(name))) {
            if (in == null) {
                throw new IllegalStateException("Missing hyphenation patterns resource: " + resourcePath(name));
            }
            return Hyphenator.read(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String resourcePath(String name) {
        return "/dev/stxt/pdf/hyphenation/" + name;
    }

    /**
     * Checks, before the layout, that every box with text and {@code Hyphenate: true} is in a language with
     * patterns. The error carries the line of the {@code Hyphenate} declaration, which may be in an ancestor or in a
     * rule of {@code Styles}, like {@code FONT_UNKNOWN} with {@code Font}.
     *
     * @throws InvalidModelException {@link ErrorCode#HYPHENATION_LANGUAGE_UNSUPPORTED}
     */
    public static void validate(Box root) {
        if (root.hasText() && root.style().typography().hyphenate()
                && !supports(root.style().typography().locale())) {
            throw new InvalidModelException(root.style().line(PropertyKey.HYPHENATE),
                    ErrorCode.HYPHENATION_LANGUAGE_UNSUPPORTED, "Hyphenate: true needs hyphenation patterns for the "
                    + "language '" + root.style().typography().locale().toLanguageTag() + "', and the engine has them "
                    + "for " + SUPPORTED + ": the text needs Hyphenate: false, or one of those languages in Language");
        }
        for (Box child : root.children()) {
            validate(child);
        }
    }

    /** The languages with patterns, for the documentation and the tests. */
    public static List<String> supported() {
        return List.of("es", "ca", "en-US", "en-GB");
    }
}
