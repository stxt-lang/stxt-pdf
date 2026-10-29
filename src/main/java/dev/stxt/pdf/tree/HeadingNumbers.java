package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.NumberStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * La numeración de los encabezados (referencia/componentes/heading.md): {@code 1}, {@code 1.1}, {@code II.a}…
 *
 * <p>Se cuenta en orden del documento, atravesando las secciones y los {@code Markdown}: un
 * contador por nivel que sube con cada encabezado numerado de ese nivel y vuelve a 0 con cualquier
 * encabezado de un nivel superior (numerado o no). Un encabezado con {@code Numbered: false} no
 * cuenta ni lleva número. El número de un encabezado de nivel N son los contadores de los niveles
 * 1 a N que están numerados, cada uno escrito con el {@code Number style} de su encabezado: bajo un título sin número
 * (un {@code Heading 1}), los {@code Heading 2} numerados son 1, 2, 3…</p>
 */
final class HeadingNumbers {

    /** Lo que {@code Number label} sustituye por el número. */
    static final String NUMBER = "{number}";

    /** El nivel más profundo de encabezado ({@code Heading 6}). */
    private static final int LEVELS = 6;

    /** Contador de cada nivel (el índice 0 no se usa). */
    private final int[] counters = new int[LEVELS + 1];
    /** Si el último encabezado de cada nivel está numerado: el de cada nivel anterior es un antecesor del siguiente. */
    private final boolean[] numbered = new boolean[LEVELS + 1];
    /** Cómo se escribe el número de cada nivel: el {@code Number style} de su último encabezado. */
    private final NumberStyle[] styles = new NumberStyle[LEVELS + 1];

    /**
     * Registra un encabezado en orden del documento y devuelve su número ({@code "4.2"}, {@code "II.a"}), o
     * {@code null} si no se numera.
     */
    String next(int level, boolean number, NumberStyle style) {
        for (int deeper = level + 1; deeper <= LEVELS; deeper++) {
            counters[deeper] = 0;
        }
        numbered[level] = number;
        styles[level] = style;
        if (!number) {
            return null;
        }
        counters[level]++;
        List<String> parts = new ArrayList<>(level);
        for (int l = 1; l <= level; l++) {
            // Los antecesores sin número no aportan: su contador no existe.
            if (l == level || numbered[l]) {
                parts.add(ListMarkers.number(styles[l].marker(), counters[l]));
            }
        }
        return String.join(".", parts);
    }

    /**
     * Escribe el número delante del título del encabezado, como un texto más: lo ven el layout, el
     * índice y los marcadores del PDF sin tratarlo aparte. Va en el primer {@code Text}, seguido de
     * un espacio; con {@code Number label}, la plantilla con el número en su {@code {number}}.
     */
    static void prefix(Box heading, String number) {
        heading.setNumber(number);
        String label = heading.style().typography().numberLabel();
        heading.children().get(0).prependText((label == null ? number : label.replace(NUMBER, number)) + " ");
    }
}
