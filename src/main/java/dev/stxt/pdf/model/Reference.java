package dev.stxt.pdf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Una referencia a otra caja dentro de un texto (referencia/comun/referencias.md): {@code {page of: id}}
 * da la página en que empieza la caja con ese {@code Id} y {@code {number of: id}}, su número.
 * Es una pieza pura: busca las referencias de un texto y las sustituye, sin saber qué valen.
 *
 * @param kind  qué se pide de la caja
 * @param start posición del {@code {} de apertura en el texto
 * @param end   posición siguiente al {@code }} de cierre
 * @param id    el {@code Id} de la caja, sin espacios alrededor; puede estar vacío si el texto
 *              lo omite, y quien resuelve lo rechaza
 */
public record Reference(Kind kind, int start, int end, String id) {

    /** Qué se pide de la caja referida. */
    public enum Kind {
        /** El número de la página donde empieza. */
        PAGE("page"),
        /** Su número en el documento: el de un ítem de lista o el de un encabezado. */
        NUMBER("number");

        private final String keyword;

        Kind(String keyword) {
            this.keyword = keyword;
        }

        /** La palabra que lo nombra en el texto ({@code page}, {@code number}). */
        public String keyword() {
            return keyword;
        }
    }

    private static final Pattern PATTERN = Pattern.compile("\\{(page|number) of:([^{}]*)}");

    /** Si el texto contiene alguna referencia. */
    public static boolean contains(String text) {
        return text != null && text.contains(" of:") && PATTERN.matcher(text).find();
    }

    /** Las referencias de un texto, en orden. */
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

    /** La referencia que empieza justo en {@code index}, si la hay. */
    public static Reference at(String text, int index) {
        if (text.charAt(index) != '{') {
            return null;
        }
        Matcher m = PATTERN.matcher(text).region(index, text.length());
        return m.lookingAt() ? of(m) : null;
    }

    /**
     * El texto con cada referencia sustituida por el valor que da {@code value} (el tipo y el
     * {@code Id}); si da {@code null}, la referencia se deja como está.
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
