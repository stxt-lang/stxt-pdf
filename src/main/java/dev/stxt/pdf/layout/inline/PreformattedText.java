package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.TextRun;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Reparte en líneas un texto con {@code White space: pre} o {@code preserve-wrap}
 * (referencia/comun/formato-del-texto.md): los espacios y los saltos de línea del texto se conservan.
 *
 * <p>Cada línea del texto es una línea de la caja, también las vacías (una línea en blanco no
 * separa párrafos: todo el texto es uno solo para las viudas y huérfanas). Una tabulación lleva
 * hasta la siguiente columna múltiplo de {@code Tab size}, contando los caracteres desde el
 * principio de la línea; un salto de línea final no añade una línea vacía. Con {@code preserve} las
 * líneas no se parten, así que una más ancha que la caja desborda; con {@code preserve-wrap} se parten
 * entre palabras (los espacios del corte se pierden: no se ven) y una palabra más ancha que la
 * caja desborda.</p>
 */
final class PreformattedText {

    private PreformattedText() {
    }

    /** Las líneas del texto para un ancho disponible. */
    static List<LineBreaker.Line> lines(List<TextRun> runs, int tabSize, boolean wrap, double available,
                                        TextMeasurer measurer, FontFace font, double fontSize,
                                        double letterSpacing) {
        List<LineBreaker.Line> lines = new ArrayList<>();
        for (List<TextRun> source : sourceLines(runs, tabSize)) {
            if (wrap) {
                lines.addAll(wrap(source, available, measurer, font, fontSize, letterSpacing));
            } else {
                lines.add(new LineBreaker.Line(source));
            }
        }
        return lines;
    }

    /**
     * Ancho más estrecho y ancho natural: con {@code preserve}, la línea más ancha; con {@code preserve-wrap}, la
     * palabra más ancha y la línea más ancha.
     */
    static IntrinsicSizes intrinsics(List<TextRun> runs, int tabSize, boolean wrap, TextMeasurer measurer,
                                     FontFace font, double fontSize, double letterSpacing) {
        double min = 0;
        double max = 0;
        for (List<TextRun> source : sourceLines(runs, tabSize)) {
            double width = LineBreaker.width(source, measurer, font, fontSize, letterSpacing);
            max = Math.max(max, width);
            if (!wrap) {
                min = max;
                continue;
            }
            String text = TextRun.text(source);
            int[] starts = starts(source);
            for (int[] word : words(text)) {
                min = Math.max(min, LineBreaker.width(slice(source, starts, text, word[0], word[1]), measurer, font,
                        fontSize, letterSpacing));
            }
        }
        return new IntrinsicSizes(min, max);
    }

    /**
     * Las líneas del texto con las tabulaciones ya expandidas, cada una como sus tramos. Una línea
     * vacía no tiene tramos.
     */
    static List<List<TextRun>> sourceLines(List<TextRun> runs, int tabSize) {
        List<List<TextRun>> lines = new ArrayList<>();
        TextRun.Joiner line = new TextRun.Joiner();
        int column = 0;
        for (TextRun run : runs) {
            String text = run.text();
            StringBuilder piece = new StringBuilder();
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                if (cp == '\n') {
                    add(line, piece, run);
                    lines.add(line.build());
                    line = new TextRun.Joiner();
                    column = 0;
                } else if (cp == '\t') {
                    int columns = tabSize - column % tabSize;
                    piece.append(" ".repeat(columns));
                    column += columns;
                } else if (cp != '\r') {
                    piece.appendCodePoint(cp);
                    column++;
                }
            }
            add(line, piece, run);
        }
        lines.add(line.build());
        if (lines.size() > 1 && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    private static void add(TextRun.Joiner line, StringBuilder piece, TextRun run) {
        if (piece.length() > 0) {
            line.add(piece.toString(), run.format());
            piece.setLength(0);
        }
    }

    /**
     * Parte una línea de {@code preserve-wrap} con el algoritmo voraz: una palabra pasa a la línea
     * siguiente si, con ella, la línea no cabe y ya tenía otra palabra. Los espacios de antes de la
     * primera palabra (la sangría) se conservan; los del corte, no.
     */
    private static List<LineBreaker.Line> wrap(List<TextRun> source, double available, TextMeasurer measurer,
                                               FontFace font, double fontSize, double letterSpacing) {
        if (fits(LineBreaker.width(source, measurer, font, fontSize, letterSpacing), available)) {
            return List.of(new LineBreaker.Line(source));
        }
        String text = TextRun.text(source);
        int[] starts = starts(source);
        List<LineBreaker.Line> lines = new ArrayList<>();
        int lineStart = 0;
        int lastEnd = 0;
        for (int[] word : words(text)) {
            if (lastEnd > lineStart && !fits(LineBreaker.width(slice(source, starts, text, lineStart, word[1]),
                    measurer, font, fontSize, letterSpacing), available)) {
                lines.add(new LineBreaker.Line(slice(source, starts, text, lineStart, lastEnd)));
                lineStart = word[0];
            }
            lastEnd = word[1];
        }
        lines.add(new LineBreaker.Line(slice(source, starts, text, lineStart, text.length())));
        return lines;
    }

    /** Dónde empieza cada tramo en el texto de la línea, y dónde acaba el último. */
    private static int[] starts(List<TextRun> runs) {
        int[] starts = new int[runs.size() + 1];
        for (int i = 0; i < runs.size(); i++) {
            starts[i + 1] = starts[i] + runs.get(i).text().length();
        }
        return starts;
    }

    /** Los tramos del texto entre dos posiciones, cada uno con su formato. */
    private static List<TextRun> slice(List<TextRun> runs, int[] starts, String text, int from, int to) {
        return new LineBreaker.RunCursor(runs, starts).parts(text, from, to);
    }

    /** Las palabras de la línea (lo que no es un espacio), como {@code [inicio, fin)}. */
    private static List<int[]> words(String text) {
        List<int[]> words = new ArrayList<>();
        for (int i = 0; i < text.length(); ) {
            if (text.charAt(i) == ' ') {
                i++;
                continue;
            }
            int j = i;
            while (j < text.length() && text.charAt(j) != ' ') {
                j++;
            }
            words.add(new int[]{i, j});
            i = j;
        }
        return words;
    }
}
