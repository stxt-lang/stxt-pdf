package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Reparte un texto en líneas con el algoritmo voraz (<em>first-fit</em>) de los navegadores
 * (módulo 2, ESPECIFICACION.md §5.4).
 *
 * <p>Tratamiento del espacio en blanco: los saltos de línea simples del bloque {@code Text} y
 * las secuencias de espacios valen un espacio; una línea en blanco separa párrafos y fuerza un
 * salto. Una palabra más ancha que la línea ocupa una línea propia y desborda.</p>
 */
public final class LineBreaker {

    private LineBreaker() {
    }

    /** Líneas del texto para un ancho disponible. */
    public static List<String> breakLines(String text, double available, TextMeasurer measurer,
                                          String font, double fontSize) {
        List<String> lines = new ArrayList<>();
        breakParagraphs(text, available, measurer, font, fontSize).forEach(lines::addAll);
        return lines;
    }

    /** Líneas de cada párrafo del texto para un ancho disponible (las viudas y huérfanas son por párrafo). */
    public static List<List<String>> breakParagraphs(String text, double available, TextMeasurer measurer,
                                                     String font, double fontSize) {
        List<List<String>> result = new ArrayList<>();
        for (List<String> paragraph : paragraphs(text)) {
            List<String> lines = new ArrayList<>();
            result.add(lines);
            StringBuilder current = new StringBuilder();
            for (String word : paragraph) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (current.length() == 0 || measurer.width(candidate, font, fontSize) <= available + 1e-9) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    lines.add(current.toString());
                    current.setLength(0);
                    current.append(word);
                }
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return result;
    }

    /** min-content (palabra más ancha) y max-content (párrafo más largo sin partir). */
    public static IntrinsicSizes intrinsics(String text, TextMeasurer measurer, String font, double fontSize) {
        double min = 0;
        double max = 0;
        for (List<String> paragraph : paragraphs(text)) {
            for (String word : paragraph) {
                min = Math.max(min, measurer.width(word, font, fontSize));
            }
            max = Math.max(max, measurer.width(String.join(" ", paragraph), font, fontSize));
        }
        return new IntrinsicSizes(min, max);
    }

    /** Desplazamiento horizontal de una línea según la alineación. */
    public static double offset(double lineWidth, double available, Style.TextAlign textAlign) {
        double remaining = Math.max(0, available - lineWidth);
        return switch (textAlign) {
            case LEFT, JUSTIFY -> 0;
            case RIGHT -> remaining;
            case CENTER -> remaining / 2;
        };
    }

    private static List<List<String>> paragraphs(String text) {
        List<List<String>> result = new ArrayList<>();
        for (String block : text.split("\\n\\s*\\n")) {
            String clean = block.strip();
            if (!clean.isEmpty()) {
                result.add(List.of(clean.split("\\s+")));
            }
        }
        return result;
    }
}
