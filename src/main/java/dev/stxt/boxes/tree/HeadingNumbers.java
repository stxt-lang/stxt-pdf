package dev.stxt.boxes.tree;

import dev.stxt.Node;
import dev.stxt.boxes.model.TextFormat;
import dev.stxt.boxes.model.TextRun;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.boxes.tree.Nodes.error;

/**
 * La numeración de los encabezados (ESPECIFICACION.md §5.13): {@code 1}, {@code 1.1}, {@code 1.1.1}…
 *
 * <p>Se cuenta en orden del documento, atravesando las secciones y los {@code Markdown}: un
 * contador por nivel que sube con cada encabezado numerado de ese nivel y vuelve a 0 con cualquier
 * encabezado de un nivel superior (numerado o no). Un encabezado con {@code Numbered: false} no
 * cuenta ni lleva número. El número de un encabezado de nivel N son los contadores de los niveles
 * 1 a N, así que su encabezado padre también tiene que estar numerado.</p>
 */
final class HeadingNumbers {

    /** El nivel más profundo de encabezado ({@code Heading 6}). */
    private static final int LEVELS = 6;

    /** Contador de cada nivel (el índice 0 no se usa). */
    private final int[] counters = new int[LEVELS + 1];
    /** Si el último encabezado de cada nivel está numerado: el del nivel anterior es el padre del siguiente. */
    private final boolean[] numbered = new boolean[LEVELS + 1];

    /**
     * Registra un encabezado en orden del documento y devuelve su número ({@code "4.2"}), o
     * {@code null} si no se numera.
     *
     * @throws InvalidModelException {@code HEADING_NUMBER_PARENT} si el encabezado se numera y su padre no
     */
    String next(Node heading, int level, boolean number) {
        for (int deeper = level + 1; deeper <= LEVELS; deeper++) {
            counters[deeper] = 0;
        }
        numbered[level] = number;
        if (!number) {
            return null;
        }
        if (level > 1 && !numbered[level - 1]) {
            throw error(heading, ErrorCode.HEADING_NUMBER_PARENT, "Un " + heading.getName() + " numerado necesita "
                    + "que su encabezado padre (Heading " + (level - 1) + ") también lo esté, porque su número "
                    + "empieza por el del padre: numera el padre o declara Numbered: false en este");
        }
        counters[level]++;
        List<String> parts = new ArrayList<>(level);
        for (int l = 1; l <= level; l++) {
            parts.add(Integer.toString(counters[l]));
        }
        return String.join(".", parts);
    }

    /**
     * Escribe el número delante del título del encabezado, como un texto más: lo ven el layout, el
     * índice y los marcadores del PDF sin tratarlo aparte. Va en el primer {@code Text}, seguido de
     * un espacio.
     */
    static void prefix(Box heading, String number) {
        heading.number = number;
        Box first = heading.children.get(0);
        String prefix = number + " ";
        first.text = prefix + first.text;
        if (first.runs != null) {
            List<TextRun> runs = new ArrayList<>(first.runs.size() + 1);
            runs.add(new TextRun(prefix, TextFormat.PLAIN));
            runs.addAll(first.runs);
            first.runs = List.copyOf(runs);
        }
    }
}
