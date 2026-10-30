package dev.stxt.pdf.layout;

import dev.stxt.pdf.layout.inline.PageNumbers;
import dev.stxt.pdf.model.NumberFormat;
import dev.stxt.pdf.tree.Section;

import java.util.Arrays;
import java.util.List;

/**
 * El número impreso de cada página física, el último de su tanda y cómo se escriben
 * (referencia/componentes/document.md): la tanda son las páginas desde el último {@code Page numbering: restart}, que
 * empieza en su {@code Start}; {@code none} no numera ni avanza el contador. Cada página se escribe con el {@code Page
 * style} de su sección. La página en blanco con que acaba una sección ({@code Begin on: odd}) es una página más de
 * ella.
 */
record Numbering(int[] printed, int[] totals, NumberFormat[] styles) {

    /**
     * @param results    el flujo de cada sección, un resultado por página
     * @param blankAfter si cada sección acaba con una página en blanco
     */
    static Numbering of(List<Section> sections, List<List<LayoutResult>> results, boolean[] blankAfter) {
        int pages = results.stream().mapToInt(List::size).sum();
        for (boolean blank : blankAfter) {
            pages += blank ? 1 : 0;
        }
        int[] printed = new int[pages];
        int[] totals = new int[pages];
        NumberFormat[] styles = new NumberFormat[pages];
        int counter = 0;
        int tandaFirst = 0;
        int i = 0;
        for (int k = 0; k < sections.size(); k++) {
            Section section = sections.get(k);
            Section.PageNumbering mode = section.numbering();
            if (mode == Section.PageNumbering.RESTART) {
                Arrays.fill(totals, tandaFirst, i, counter);
                counter = section.start() - 1;
                tandaFirst = i;
            }
            int own = results.get(k).size() + (blankAfter[k] ? 1 : 0);
            for (int j = 0; j < own; j++) {
                styles[i] = section.pageNumberFormat();
                printed[i++] = mode == Section.PageNumbering.NONE ? 0 : ++counter;
            }
        }
        Arrays.fill(totals, tandaFirst, i, counter);
        return new Numbering(printed, totals, styles);
    }

    int pages() {
        return printed.length;
    }

    /** Número impreso de la página física {@code number} (desde 1); 0 si no se numera. */
    int printed(int number) {
        return printed[number - 1];
    }

    /** Último número de la tanda de la página física {@code number}. */
    int total(int number) {
        return totals[number - 1];
    }

    /** Cómo se escribe el número de la página física {@code number}. */
    NumberFormat style(int number) {
        return styles[number - 1];
    }

    /** El número impreso de la página física {@code number}, escrito con su estilo ({@code {page}}, {@code iv}). */
    String label(int number) {
        return style(number).format(printed(number));
    }

    /** El último número de su tanda, escrito con el estilo de la página ({@code {pages}}). */
    String totalLabel(int number) {
        return style(number).format(total(number));
    }

    /**
     * El número de prueba que mide al menos lo que cualquier número impreso de las páginas numeradas: tantos caracteres
     * como el más largo, en cifras ({@code 0}) o, si hay romanos, con la letra más ancha ({@code M} o {@code m}).
     */
    String probe() {
        int length = 0;
        char widest = '0';
        for (int n = 1; n <= pages(); n++) {
            if (printed(n) == 0) {
                continue;
            }
            length = Math.max(length, Math.max(label(n).length(), totalLabel(n).length()));
            widest = PageNumbers.wider(widest, switch (style(n)) {
                case UPPER_ROMAN -> 'M';
                case LOWER_ROMAN -> 'm';
                default -> '0';
            });
        }
        return String.valueOf(widest).repeat(length);
    }
}
