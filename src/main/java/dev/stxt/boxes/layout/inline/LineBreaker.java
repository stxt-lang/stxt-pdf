package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.stxt.boxes.model.Tolerance.EPSILON;
import static dev.stxt.boxes.model.Tolerance.fits;

/**
 * Reparte un texto en líneas (módulo 2, ESPECIFICACION.md §5.4). Con {@code Text wrap: auto},
 * con el algoritmo voraz (<em>first-fit</em>) de los navegadores; con {@code pretty}, con
 * Knuth-Plass ({@link #optimal}).
 *
 * <p>Tratamiento del espacio en blanco: los saltos de línea simples del bloque {@code Text} y
 * las secuencias de espacios valen un espacio; una línea en blanco separa párrafos y fuerza un
 * salto. Una palabra más ancha que la línea ocupa una línea propia y desborda.</p>
 */
public final class LineBreaker {

    /** Deméritos de cada línea además de los de su badness: favorece los repartos con menos líneas. */
    private static final double LINE_PENALTY = 10;
    /** Badness de una línea que no se puede estirar lo suficiente o que desborda. */
    private static final double MAX_BADNESS = 10_000;
    /** Cuánto se puede estirar cada espacio, en proporción a su ancho (el de TeX). */
    private static final double STRETCH = 0.5;

    private LineBreaker() {
    }

    /** Líneas del texto para un ancho disponible, con el algoritmo voraz. */
    public static List<String> breakLines(String text, double available, TextMeasurer measurer,
                                          FontFace font, double fontSize) {
        List<String> lines = new ArrayList<>();
        breakParagraphs(text, available, measurer, font, fontSize).forEach(lines::addAll);
        return lines;
    }

    /** Líneas de cada párrafo con el algoritmo voraz (ver {@link #breakParagraphs(String, double, TextMeasurer, FontFace, double, Style.TextWrap)}). */
    public static List<List<String>> breakParagraphs(String text, double available, TextMeasurer measurer,
                                                     FontFace font, double fontSize) {
        return breakParagraphs(text, available, measurer, font, fontSize, Style.TextWrap.AUTO);
    }

    /** Líneas de cada párrafo del texto para un ancho disponible (las viudas y huérfanas son por párrafo). */
    public static List<List<String>> breakParagraphs(String text, double available, TextMeasurer measurer,
                                                     FontFace font, double fontSize, Style.TextWrap wrap) {
        List<List<String>> result = new ArrayList<>();
        for (List<String> paragraph : paragraphs(text)) {
            if (wrap == Style.TextWrap.PRETTY) {
                result.add(optimal(paragraph, available, measurer, font, fontSize));
                continue;
            }
            List<String> lines = new ArrayList<>();
            result.add(lines);
            StringBuilder current = new StringBuilder();
            for (String word : paragraph) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (current.length() == 0 || fits(measurer.width(candidate, font, fontSize), available)) {
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

    /**
     * Knuth-Plass simplificado: los saltos de un párrafo que minimizan la suma de los deméritos
     * de sus líneas, por programación dinámica. Sin guiones ni encogimiento de los espacios
     * (las líneas justificadas no se comprimen):
     *
     * <ul>
     *   <li>{@code badness = 100 × r³}, con {@code r} = sobrante / estiramiento de la línea
     *       (cada espacio se estira hasta la mitad de su ancho), hasta {@value #MAX_BADNESS}.
     *       Una línea sin espacios que no llena el ancho, o una palabra que desborda, tiene la
     *       badness máxima. La última línea del párrafo no tiene badness.</li>
     *   <li>{@code deméritos = (penalización por línea + badness)²}.</li>
     * </ul>
     *
     * <p>El ancho de una línea es la suma de los de sus palabras y espacios.</p>
     */
    static List<String> optimal(List<String> words, double available, TextMeasurer measurer,
                                FontFace font, double fontSize) {
        int n = words.size();
        double space = measurer.width(" ", font, fontSize);
        double[] prefix = new double[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + measurer.width(words.get(i), font, fontSize);
        }
        // best[j]: deméritos mínimos de las líneas con las j primeras palabras; from[j]: primera
        // palabra de la última de esas líneas.
        double[] best = new double[n + 1];
        int[] from = new int[n + 1];
        Arrays.fill(best, Double.POSITIVE_INFINITY);
        best[0] = 0;
        for (int j = 1; j <= n; j++) {
            for (int i = j - 1; i >= 0; i--) {
                int spaces = j - i - 1;
                double width = prefix[j] - prefix[i] + space * spaces;
                if (spaces > 0 && !fits(width, available)) {
                    // Con más palabras sería aún más ancha; una sola sí puede desbordar.
                    break;
                }
                double d = best[i] + demerits(available - width, spaces * space * STRETCH, j == n);
                if (d < best[j]) {
                    best[j] = d;
                    from[j] = i;
                }
            }
        }
        List<String> lines = new ArrayList<>();
        for (int j = n; j > 0; j = from[j]) {
            lines.add(0, String.join(" ", words.subList(from[j], j)));
        }
        return lines;
    }

    /** Deméritos de una línea con este sobrante y este estiramiento posible. */
    private static double demerits(double slack, double stretch, boolean last) {
        double badness;
        if (slack < -EPSILON) {
            badness = MAX_BADNESS;
        } else if (last || slack <= EPSILON) {
            badness = 0;
        } else if (stretch <= 0) {
            badness = MAX_BADNESS;
        } else {
            badness = Math.min(MAX_BADNESS, 100 * Math.pow(slack / stretch, 3));
        }
        return (LINE_PENALTY + badness) * (LINE_PENALTY + badness);
    }

    /** min-content (palabra más ancha) y max-content (párrafo más largo sin partir). */
    public static IntrinsicSizes intrinsics(String text, TextMeasurer measurer, FontFace font, double fontSize) {
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

    /**
     * Espacio extra por cada espacio entre palabras con {@code justify}: el sobrante de la línea
     * repartido entre sus espacios. La última línea de cada párrafo y las líneas de una sola
     * palabra no se justifican (se alinean a la izquierda), y una línea que desborda no se
     * comprime.
     *
     * @param line      texto de la línea, con las palabras separadas por un espacio
     * @param lineWidth ancho natural de la línea
     * @param last      es la última línea de su párrafo
     */
    public static double wordSpacing(String line, double lineWidth, double available, Style.TextAlign textAlign,
                                     boolean last) {
        if (textAlign != Style.TextAlign.JUSTIFY || last) {
            return 0;
        }
        long spaces = line.chars().filter(c -> c == ' ').count();
        return spaces == 0 ? 0 : Math.max(0, available - lineWidth) / spaces;
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
