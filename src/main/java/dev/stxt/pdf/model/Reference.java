package dev.stxt.pdf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A reference inside a text (reference/common/references.md): {@code {page of: id}} gives the page on which the box
 * with that {@code Id} starts, {@code {number of: id}}, its number, and {@code {heading N}} (or
 * {@code {heading N: first}}), the text of the heading of level N in effect on the page, a running head. It is a pure
 * piece: it finds the references of a text and replaces them, without knowing their values.
 *
 * @param kind  what is requested
 * @param start position of the opening {@code {} in the text
 * @param end   position after the closing {@code }}
 * @param id    what the reference names, without surrounding spaces: the {@code Id} of the box, which can be empty if
 *              the text omits it, and the resolver rejects it; in a heading reference, its level and whether it asks
 *              for the first heading of the page, in the form {@code "2"} or {@code "2: first"} ({@link #headingLevel},
 *              {@link #first}), or whatever was written if it is not valid
 */
public record Reference(Kind kind, int start, int end, String id) {

    /** What is requested. */
    public enum Kind {
        /** The number of the page where the box starts. */
        PAGE("page"),
        /** The number of the box in the document: that of a list item or of a heading. */
        NUMBER("number"),
        /** The text of the heading of a level in effect on the page, in a {@code Page overlay}. */
        HEADING("heading");

        private final String keyword;

        Kind(String keyword) {
            this.keyword = keyword;
        }

        /** The word that names it in the text ({@code page}, {@code number}, {@code heading}). */
        public String keyword() {
            return keyword;
        }
    }

    /** The variant of a heading reference that asks for the first heading of the page. */
    public static final String FIRST = "first";

    /**
     * What a reference names: anything but braces, also the literal one ({@link #LITERAL_BRACE}), so that a reference
     * that is text inside something that looks like another one ({@code {page of: {{page}}}}) does not complete it.
     */
    private static final String NAMED = "([^{}" + (char) 0xFDD3 + "]*)";
    private static final Pattern PATTERN = Pattern.compile("\\{(page|number) of:" + NAMED + "}|\\{heading " + NAMED
            + "}");
    /** Everything a text replaces: the references and the page numbers, {@code {page}} and {@code {pages}}. */
    private static final Pattern REPLACED = Pattern.compile("\\{pages?}|" + PATTERN.pattern());
    /** One of them inside another pair of braces: it is text ({@link #literal}). */
    private static final Pattern WRAPPED = Pattern.compile("\\{(" + REPLACED.pattern() + ")}");

    /**
     * The opening brace of a reference or a page number that is text and not replaced
     * (reference/common/references.md): {@code {{page}}} in a document, or <code>\{page}</code> in Markdown, is kept
     * in the text of a box as this character and {@code page}}, which nothing takes for a reference. It is a
     * noncharacter of Unicode, which a text never has; the measurements count it as a brace and the laid out lines
     * have the brace ({@link #shown}).
     */
    public static final char LITERAL_BRACE = (char) 0xFDD3;

    /**
     * The text of a document with its literal references kept as text (reference/common/references.md): a reference
     * or a page number inside another pair of braces, {@code {{page}}} or {@code {{page of: id}}}, loses that pair and
     * its own opening brace becomes {@link #LITERAL_BRACE}. Only one pair is removed: {@code {{{page}}}} is the text
     * {@code {{page}}}. Anything else in braces is left as it is.
     */
    public static String literal(String text) {
        if (text == null || !text.contains("{{")) {
            return text;
        }
        Matcher m = WRAPPED.matcher(text);
        StringBuilder sb = new StringBuilder(text.length());
        int last = 0;
        while (m.find()) {
            sb.append(text, last, m.start()).append(LITERAL_BRACE).append(text, m.start() + 2, m.end() - 1);
            last = m.end();
        }
        return last == 0 ? text : sb.append(text, last, text.length()).toString();
    }

    /**
     * A text written so that a document shows it as it is (reference/common/references.md): each reference and each
     * page number it contains goes inside another pair of braces, {@code {page}} as {@code {{page}}}. It is what a
     * program that writes documents from data does with a text that is not its own and goes in a literal text; nothing
     * else is changed. A text that goes in Markdown is escaped as Markdown instead, with a backslash before each
     * punctuation sign, braces included: both together would double the braces.
     *
     * @param text the text to show, not {@code null}
     * @return the text to write in the document
     */
    public static String escape(String text) {
        if (text.indexOf('{') < 0) {
            return text;
        }
        Matcher m = REPLACED.matcher(text);
        StringBuilder sb = new StringBuilder(text.length() + 4);
        int last = 0;
        while (m.find()) {
            sb.append(text, last, m.start()).append('{').append(text, m.start(), m.end()).append('}');
            last = m.end();
        }
        return last == 0 ? text : sb.append(text, last, text.length()).toString();
    }

    /** The text as it is read and painted: each {@link #LITERAL_BRACE} is an opening brace. */
    public static String shown(String text) {
        return text == null || text.indexOf(LITERAL_BRACE) < 0 ? text : text.replace(LITERAL_BRACE, '{');
    }
    /** A valid heading reference: the level, 1 to 6, and the optional variant. */
    private static final Pattern HEADING = Pattern.compile("([1-6])(?:\\s*:\\s*(" + FIRST + "))?");

    /** Whether the text can contain a reference, cheaply: the slow check is the pattern. */
    private static boolean mayContain(String text) {
        return text != null && (text.contains(" of:") || text.contains("{heading "));
    }

    /** Whether the text contains any reference. */
    public static boolean contains(String text) {
        return mayContain(text) && PATTERN.matcher(text).find();
    }

    /** The references of a text, in order. */
    public static List<Reference> find(String text) {
        List<Reference> found = new ArrayList<>();
        if (!mayContain(text)) {
            return found;
        }
        Matcher m = PATTERN.matcher(text);
        while (m.find()) {
            found.add(of(m));
        }
        return found;
    }

    /** The reference that starts exactly at {@code index}, if there is one. */
    public static Reference at(String text, int index) {
        if (text.charAt(index) != '{') {
            return null;
        }
        Matcher m = PATTERN.matcher(text).region(index, text.length());
        return m.lookingAt() ? of(m) : null;
    }

    /**
     * The text with each reference replaced by the value that {@code value} gives (from the kind and the
     * {@code id}); if it gives {@code null}, the reference is left as it is.
     */
    public static String replace(String text, BiFunction<Kind, String, String> value) {
        if (!contains(text)) {
            return text;
        }
        Matcher m = PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            Reference r = of(m);
            String v = value.apply(r.kind(), r.id());
            sb.append(text, last, m.start()).append(v != null ? v : m.group());
            last = m.end();
        }
        return sb.append(text, last, text.length()).toString();
    }

    private static Reference of(Matcher m) {
        if (m.group(1) == null) {
            return new Reference(Kind.HEADING, m.start(), m.end(), headingId(m.group(3).strip()));
        }
        Kind kind = m.group(1).equals(Kind.PAGE.keyword) ? Kind.PAGE : Kind.NUMBER;
        return new Reference(kind, m.start(), m.end(), m.group(2).strip());
    }

    /** The id of a heading reference in its normal form ({@code "2"}, {@code "2: first"}), or as written if invalid. */
    private static String headingId(String written) {
        Matcher m = HEADING.matcher(written);
        if (!m.matches()) {
            return written;
        }
        return m.group(2) == null ? m.group(1) : m.group(1) + ": " + FIRST;
    }

    /** The level of a heading reference, 1 to 6; 0 if it is not one, or if its level is not valid. */
    public int headingLevel() {
        if (kind != Kind.HEADING || !HEADING.matcher(id).matches()) {
            return 0;
        }
        return id.charAt(0) - '0';
    }

    /** Whether it is a heading reference that asks for the first heading of its level that starts on the page. */
    public boolean first() {
        return kind == Kind.HEADING && id.endsWith(FIRST);
    }

    /** The reference as it is written in a text, for messages: {@code {page of: id}}, {@code {heading 2: first}}. */
    public String marker() {
        return "{" + kind.keyword() + (kind == Kind.HEADING ? " " : " of: ") + id + "}";
    }
}
