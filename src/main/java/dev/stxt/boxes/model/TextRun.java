package dev.stxt.boxes.model;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Un tramo de texto con su formato (ESPECIFICACION.md §5.4). El texto de una caja es una lista de
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
}
