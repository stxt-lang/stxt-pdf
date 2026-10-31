package dev.stxt.pdf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A reference to another box inside a text (reference/common/references.md): {@code {page of: id}}
 * gives the page on which the box with that {@code Id} starts and {@code {number of: id}}, its number.
 * It is a pure piece: it finds the references of a text and replaces them, without knowing their values.
 *
 * @param kind  what is requested of the box
 * @param start position of the opening {@code {} in the text
 * @param end   position after the closing {@code }}
 * @param id    the {@code Id} of the box, without surrounding spaces; it can be empty if the text
 *              omits it, and the resolver rejects it
 */
public record Reference(Kind kind, int start, int end, String id) {

    /** What is requested of the referenced box. */
    public enum Kind {
        /** The number of the page where it starts. */
        PAGE("page"),
        /** Its number in the document: that of a list item or of a heading. */
        NUMBER("number");

        private final String keyword;

        Kind(String keyword) {
            this.keyword = keyword;
        }

        /** The word that names it in the text ({@code page}, {@code number}). */
        public String keyword() {
            return keyword;
        }
    }

    private static final Pattern PATTERN = Pattern.compile("\\{(page|number) of:([^{}]*)}");

    /** Whether the text contains any reference. */
    public static boolean contains(String text) {
        return text != null && text.contains(" of:") && PATTERN.matcher(text).find();
    }

    /** The references of a text, in order. */
    public static List<Reference> find(String text) {
        List<Reference> found = new ArrayList<>();
        if (text == null || !text.contains(" of:")) {
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
     * {@code Id}); if it gives {@code null}, the reference is left as it is.
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
        Kind kind = m.group(1).equals(Kind.PAGE.keyword) ? Kind.PAGE : Kind.NUMBER;
        return new Reference(kind, m.start(), m.end(), m.group(2).strip());
    }
}
