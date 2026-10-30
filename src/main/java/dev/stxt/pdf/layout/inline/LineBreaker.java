package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.TextWrap;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.stxt.pdf.model.Tolerance.EPSILON;
import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Reparte un texto en líneas (referencia/comun/tipografia.md). Con {@code Line breaking: simple},
 * con el algoritmo voraz (<em>first-fit</em>) de los navegadores; con {@code balanced}, con
 * Knuth-Plass ({@link #optimal}).
 *
 * <p>Tratamiento del espacio en blanco: los saltos de línea simples del bloque {@code Text} y
 * las secuencias de espacios valen un espacio; una línea en blanco separa párrafos y fuerza un
 * salto. Una palabra más ancha que la línea ocupa una línea propia y desborda.</p>
 *
 * <p>El texto es una lista de tramos con formato ({@link TextRun}). Una palabra puede tener
 * partes de varios tramos ({@code **neg**rita}) y mide lo que suman sus partes, cada una con su
 * fuente; el espacio entre dos palabras tiene el formato del tramo en que está el primero de sus
 * espacios en blanco. Un texto sin formato es un único tramo y se mide igual que una cadena.</p>
 *
 * <p>Cada carácter avanza su ancho más el interletraje ({@code Letter spacing}, referencia/comun/formato-del-texto.md);
 * el último de la línea no lo lleva, así que el ancho de una línea es lo que suman sus caracteres menos un
 * interletraje. Un tramo en versalitas se mide al {@link TextFormat#SMALL_CAPS_SCALE} del tamaño.</p>
 */
public final class LineBreaker {

    /** Deméritos de cada línea además de los de su badness: favorece los repartos con menos líneas. */
    private static final double LINE_PENALTY = 10;
    /** Badness de una línea que no se puede estirar lo suficiente o que desborda. */
    private static final double MAX_BADNESS = 10_000;
    /** Cuánto se puede estirar cada espacio, en proporción a su ancho (el de TeX). */
    private static final double STRETCH = 0.5;

    /**
     * Una palabra: sus partes, con los tramos seguidos del mismo formato unidos, y el formato
     * del espacio que la separa de la siguiente.
     */
    record Word(List<TextRun> parts, TextFormat gap) {
    }

    /** Una línea del reparto: sus tramos, con los seguidos del mismo formato unidos. */
    public record Line(List<TextRun> runs) {

        public Line {
            runs = List.copyOf(runs);
        }

        public String text() {
            return TextRun.text(runs);
        }
    }

    private LineBreaker() {
    }

    /** Líneas del texto para un ancho disponible, con el algoritmo voraz. */
    public static List<String> breakLines(String text, double available, TextMeasurer measurer,
                                          FontFace font, double fontSize) {
        List<String> lines = new ArrayList<>();
        breakParagraphs(text, available, measurer, font, fontSize).forEach(lines::addAll);
        return lines;
    }

    /** Líneas de cada párrafo con el algoritmo voraz (ver {@link #breakParagraphs(List, double, TextMeasurer, FontFace, double, TextWrap)}). */
    public static List<List<String>> breakParagraphs(String text, double available, TextMeasurer measurer,
                                                     FontFace font, double fontSize) {
        return breakParagraphs(text, available, measurer, font, fontSize, TextWrap.SIMPLE);
    }

    /** Como {@link #breakParagraphs(List, double, TextMeasurer, FontFace, double, TextWrap)}, para un texto sin formato. */
    public static List<List<String>> breakParagraphs(String text, double available, TextMeasurer measurer,
                                                     FontFace font, double fontSize, TextWrap wrap) {
        return breakParagraphs(TextRun.plain(text), available, measurer, font, fontSize, wrap).stream()
                .map(p -> p.stream().map(Line::text).toList()).toList();
    }

    /**
     * Líneas de cada párrafo del texto para un ancho disponible (las viudas y huérfanas son por
     * párrafo). {@code font} es la fuente de la caja; cada tramo la cambia con su formato.
     */
    public static List<List<Line>> breakParagraphs(List<TextRun> runs, double available, TextMeasurer measurer,
                                                   FontFace font, double fontSize, TextWrap wrap) {
        return breakParagraphs(runs, available, measurer, font, fontSize, 0, wrap);
    }

    /** Como {@link #breakParagraphs(List, double, TextMeasurer, FontFace, double, TextWrap)}, con interletraje. */
    public static List<List<Line>> breakParagraphs(List<TextRun> runs, double available, TextMeasurer measurer,
                                                   FontFace font, double fontSize, double letterSpacing,
                                                   TextWrap wrap) {
        List<List<Line>> result = new ArrayList<>();
        for (List<Word> paragraph : paragraphs(runs)) {
            if (wrap == TextWrap.BALANCED) {
                result.add(optimal(paragraph, available, measurer, font, fontSize, letterSpacing));
                continue;
            }
            result.add(greedy(paragraph, available, measurer, font, fontSize, letterSpacing));
        }
        return result;
    }

    /**
     * El reparto voraz de un párrafo: cada palabra va a la línea en curso si cabe y, si no, empieza otra. El ancho de
     * la línea en curso se lleva sumando los avances de sus palabras y espacios, cada uno medido una vez; solo
     * cuando la suma queda a menos de {@link Tolerance#MODEL} del ancho disponible se construye la línea y se mide
     * entera, así que la decisión es la misma que midiéndola siempre, sin que importe el redondeo de la suma.
     */
    private static List<Line> greedy(List<Word> words, double available, TextMeasurer measurer, FontFace font,
                                     double fontSize, double letterSpacing) {
        Map<TextFormat, Double> spaces = new HashMap<>();
        List<Line> lines = new ArrayList<>();
        int start = 0;
        // Avance de las palabras start..i-1 y de sus espacios, con el interletraje de todos sus caracteres.
        double advance = 0;
        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);
            double own = advance(word.parts(), measurer, font, fontSize, letterSpacing);
            if (i == start) {
                advance = own;
                continue;
            }
            double candidate = advance + spaces.computeIfAbsent(words.get(i - 1).gap(),
                    f -> advance(" ", f, measurer, font, fontSize, letterSpacing)) + own;
            double width = candidate - letterSpacing;
            if (Math.abs(width - available) <= Tolerance.MODEL) {
                width = width(line(words.subList(start, i + 1)).runs(), measurer, font, fontSize, letterSpacing);
            }
            if (fits(width, available)) {
                advance = candidate;
            } else {
                lines.add(line(words.subList(start, i)));
                start = i;
                advance = own;
            }
        }
        if (start < words.size()) {
            lines.add(line(words.subList(start, words.size())));
        }
        return lines;
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
    static List<Line> optimal(List<Word> words, double available, TextMeasurer measurer,
                              FontFace font, double fontSize, double letterSpacing) {
        int n = words.size();
        Map<TextFormat, Double> spaces = new HashMap<>();
        // prefix[i]: avance de las i primeras palabras; gaps[i]: el de los i primeros espacios (los dos,
        // con el interletraje de cada carácter, también el del último).
        double[] prefix = new double[n + 1];
        double[] gaps = new double[n];
        for (int i = 0; i < n; i++) {
            Word w = words.get(i);
            prefix[i + 1] = prefix[i] + advance(w.parts(), measurer, font, fontSize, letterSpacing);
            if (i + 1 < n) {
                gaps[i + 1] = gaps[i] + spaces.computeIfAbsent(w.gap(),
                        f -> advance(" ", f, measurer, font, fontSize, letterSpacing));
            }
        }
        // best[j]: deméritos mínimos de las líneas con las j primeras palabras; from[j]: primera
        // palabra de la última de esas líneas.
        double[] best = new double[n + 1];
        int[] from = new int[n + 1];
        Arrays.fill(best, Double.POSITIVE_INFINITY);
        best[0] = 0;
        for (int j = 1; j <= n; j++) {
            for (int i = j - 1; i >= 0; i--) {
                boolean spaced = j - i > 1;
                double space = gaps[j - 1] - gaps[i];
                double width = prefix[j] - prefix[i] + space - letterSpacing;
                if (spaced && !fits(width, available)) {
                    // Con más palabras sería aún más ancha; una sola sí puede desbordar.
                    break;
                }
                double d = best[i] + demerits(available - width, space * STRETCH, j == n);
                if (d < best[j]) {
                    best[j] = d;
                    from[j] = i;
                }
            }
        }
        List<Line> lines = new ArrayList<>();
        for (int j = n; j > 0; j = from[j]) {
            lines.add(0, line(words.subList(from[j], j)));
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

    /** Como {@link #intrinsics(List, TextMeasurer, FontFace, double)}, para un texto sin formato. */
    public static IntrinsicSizes intrinsics(String text, TextMeasurer measurer, FontFace font, double fontSize) {
        return intrinsics(TextRun.plain(text), measurer, font, fontSize);
    }

    /** min-content (palabra más ancha) y max-content (párrafo más largo sin partir). */
    public static IntrinsicSizes intrinsics(List<TextRun> runs, TextMeasurer measurer, FontFace font,
                                            double fontSize) {
        return intrinsics(runs, measurer, font, fontSize, 0);
    }

    /** Como {@link #intrinsics(List, TextMeasurer, FontFace, double)}, con interletraje. */
    public static IntrinsicSizes intrinsics(List<TextRun> runs, TextMeasurer measurer, FontFace font,
                                            double fontSize, double letterSpacing) {
        double min = 0;
        double max = 0;
        for (List<Word> paragraph : paragraphs(runs)) {
            for (Word word : paragraph) {
                min = Math.max(min, width(word.parts(), measurer, font, fontSize, letterSpacing));
            }
            max = Math.max(max, width(line(paragraph).runs(), measurer, font, fontSize, letterSpacing));
        }
        return new IntrinsicSizes(min, max);
    }

    /** Desplazamiento horizontal de una línea según la alineación. */
    public static double offset(double lineWidth, double available, TextAlign textAlign) {
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
    public static double wordSpacing(String line, double lineWidth, double available, TextAlign textAlign,
                                     boolean last) {
        if (textAlign != TextAlign.JUSTIFY || last) {
            return 0;
        }
        long spaces = spaces(line);
        return spaces == 0 ? 0 : Math.max(0, available - lineWidth) / spaces;
    }

    /** Como {@link #place(List, TextMeasurer, Style, double, boolean, double, double)}, para un texto sin formato. */
    public static TextLine place(String text, TextMeasurer measurer, Style s, double available, boolean justify,
                                 double y, double height) {
        return place(TextRun.plain(text), measurer, s, available, justify, y, height);
    }

    /**
     * Una línea colocada en la caja de contenido (referencia/comun/formato-del-texto.md): cada tramo medido con la
     * fuente de {@code s} y su formato, la línea alineada en {@code available} y, si se justifica, con el espacio extra
     * entre palabras sumado a su ancho y al de cada tramo.
     *
     * @param runs    tramos de la línea, con las palabras separadas por un espacio
     * @param justify si la línea se puede justificar: no es la última de su párrafo (al numerar
     *                las páginas, si ya estaba justificada)
     */
    public static TextLine place(List<TextRun> runs, TextMeasurer measurer, Style s, double available,
                                 boolean justify, double y, double height) {
        FontFace font = s.typography().fontFace();
        double[] widths = new double[runs.size()];
        double natural = 0;
        for (int i = 0; i < widths.length; i++) {
            TextRun r = runs.get(i);
            widths[i] = advance(r.text(), r.format(), measurer, font, s.typography().fontSize(), s.typography().letterSpacing());
            natural += widths[i];
        }
        // El último carácter de la línea no lleva interletraje (una línea vacía, de un texto preformateado, no tiene).
        if (!runs.isEmpty()) {
            natural -= s.typography().letterSpacing();
        }
        String text = TextRun.text(runs);
        double x = offset(natural, available, s.typography().textAlign());
        double spacing = wordSpacing(text, natural, available, s.typography().textAlign(), !justify);
        List<TextLine.Run> placed = new ArrayList<>(runs.size());
        double at = 0;
        for (int i = 0; i < widths.length; i++) {
            TextRun r = runs.get(i);
            double w = widths[i] + spacing * spaces(r.text()) - (i == widths.length - 1 ? s.typography().letterSpacing() : 0);
            placed.add(new TextLine.Run(at, w, r.text(), r.format()));
            at += w;
        }
        return new TextLine(x, y, natural + spacing * spaces(text), height, text, spacing, placed);
    }

    /**
     * Ancho de unos tramos como línea: cada uno con su fuente y su tamaño, y el interletraje entre
     * sus caracteres pero no tras el último.
     */
    static double width(List<TextRun> runs, TextMeasurer measurer, FontFace font, double fontSize,
                        double letterSpacing) {
        return advance(runs, measurer, font, fontSize, letterSpacing) - letterSpacing;
    }

    /** Avance de unos tramos: su ancho más el interletraje de cada carácter, también el del último. */
    static double advance(List<TextRun> runs, TextMeasurer measurer, FontFace font, double fontSize,
                          double letterSpacing) {
        double width = 0;
        for (TextRun r : runs) {
            width += advance(r.text(), r.format(), measurer, font, fontSize, letterSpacing);
        }
        return width;
    }

    /** Avance de un texto con ese formato. */
    static double advance(String text, TextFormat format, TextMeasurer measurer, FontFace font, double fontSize,
                          double letterSpacing) {
        return measurer.width(text, format.face(font), fontSize * format.scale())
                + letterSpacing * text.codePointCount(0, text.length());
    }

    /**
     * Las palabras de cada párrafo del texto (ver la descripción de la clase): lo que hay entre los espacios en blanco
     * ({@link #isSpace}, el {@code \s} de las expresiones regulares), con un recorrido del texto.
     */
    static List<List<Word>> paragraphs(List<TextRun> runs) {
        String text = TextRun.text(runs);
        // starts[i]: posición en el texto donde empieza el tramo i.
        int[] starts = new int[runs.size() + 1];
        for (int i = 0; i < runs.size(); i++) {
            starts[i + 1] = starts[i] + runs.get(i).text().length();
        }
        RunCursor cursor = new RunCursor(runs, starts);
        List<List<Word>> result = new ArrayList<>();
        List<Word> paragraph = new ArrayList<>();
        List<TextRun> previous = null;
        int previousEnd = 0;
        int n = text.length();
        int i = 0;
        while (true) {
            int newlines = 0;
            while (i < n && isSpace(text.charAt(i))) {
                if (text.charAt(i) == '\n') {
                    newlines++;
                }
                i++;
            }
            if (i == n) {
                break;
            }
            int start = i;
            while (i < n && !isSpace(text.charAt(i))) {
                i++;
            }
            if (previous != null) {
                // Una línea en blanco: dos saltos de línea con solo espacio en blanco entre ellos.
                boolean blankLine = newlines > 1;
                paragraph.add(new Word(previous, blankLine ? TextFormat.PLAIN : cursor.format(previousEnd)));
                if (blankLine) {
                    result.add(paragraph);
                    paragraph = new ArrayList<>();
                }
            }
            previous = cursor.parts(text, start, i);
            previousEnd = i;
        }
        if (previous != null) {
            paragraph.add(new Word(previous, TextFormat.PLAIN));
            result.add(paragraph);
        }
        return result;
    }

    /** Los espacios en blanco que separan palabras: los de {@code \s} ({@code [ \t\n\x0B\f\r]}). */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
    }

    /** Recorre los tramos hacia delante: las posiciones que se le piden nunca retroceden. */
    static final class RunCursor {
        private final List<TextRun> runs;
        private final int[] starts;
        private int run;

        RunCursor(List<TextRun> runs, int[] starts) {
            this.runs = runs;
            this.starts = starts;
        }

        /** Formato del carácter en la posición {@code index}. */
        TextFormat format(int index) {
            if (runs.size() == 1) {
                // Un texto sin formato, que es lo normal: no hay tramo que buscar.
                return runs.get(0).format();
            }
            while (starts[run + 1] <= index) {
                run++;
            }
            return runs.get(run).format();
        }

        /** Las partes del texto entre {@code start} y {@code end}, cada una con su formato. */
        List<TextRun> parts(String text, int start, int end) {
            TextFormat first = format(start);
            if (end <= starts[run + 1]) {
                // Todo en un tramo, que es lo normal: sin unir nada.
                return List.of(new TextRun(text.substring(start, end), first));
            }
            TextRun.Joiner parts = new TextRun.Joiner();
            for (int i = start; i < end; ) {
                TextFormat f = format(i);
                int stop = Math.min(end, starts[run + 1]);
                parts.add(text.substring(i, stop), f);
                i = stop;
            }
            return parts.build();
        }
    }

    /** Las palabras en una línea, separadas por un espacio con el formato de la palabra anterior. */
    static Line line(List<Word> words) {
        TextRun.Joiner runs = new TextRun.Joiner();
        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                runs.add(" ", words.get(i - 1).gap());
            }
            for (TextRun part : words.get(i).parts()) {
                runs.add(part.text(), part.format());
            }
        }
        return new Line(runs.build());
    }

    private static long spaces(String text) {
        return text.chars().filter(c -> c == ' ').count();
    }
}
