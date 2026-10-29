package dev.stxt.pdf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Un tramo de texto con su formato (referencia/comun/formato-del-texto.md). El texto de una caja es una lista de
 * tramos seguidos; un texto sin formato es un único tramo {@link TextFormat#PLAIN}.
 */
public record TextRun(String text, TextFormat format) {

    public TextRun {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(format, "format");
    }

    /** Un texto sin formato. */
    public static List<TextRun> plain(String text) {
        return List.of(new TextRun(text, TextFormat.PLAIN));
    }

    /** El texto de los tramos, seguido. */
    public static String text(List<TextRun> runs) {
        return runs.size() == 1 ? runs.get(0).text() : runs.stream().map(TextRun::text).collect(Collectors.joining());
    }

    /** Si algún tramo tiene formato. */
    public static boolean formatted(List<TextRun> runs) {
        return runs.stream().anyMatch(r -> !r.format().equals(TextFormat.PLAIN));
    }

    /** Acumula tramos en orden, uniendo los seguidos que tienen el mismo formato. */
    public static final class Joiner {
        private final List<TextRun> runs = new ArrayList<>(1);
        private final StringBuilder text = new StringBuilder();
        private TextFormat format;

        public Joiner add(String s, TextFormat f) {
            if (format != null && !format.equals(f)) {
                flush();
            }
            format = f;
            text.append(s);
            return this;
        }

        /** Los tramos acumulados; vacía si no se ha añadido nada. */
        public List<TextRun> build() {
            if (format != null && text.length() > 0) {
                flush();
            }
            return List.copyOf(runs);
        }

        private void flush() {
            runs.add(new TextRun(text.toString(), format));
            text.setLength(0);
        }
    }
}
