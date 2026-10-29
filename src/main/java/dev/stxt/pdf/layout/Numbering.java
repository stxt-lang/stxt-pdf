package dev.stxt.pdf.layout;

import dev.stxt.pdf.tree.Section;

import java.util.Arrays;
import java.util.List;

/**
 * El número impreso de cada página física y el último de su tanda (referencia/componentes/document.md): las páginas
 * desde el último {@code Page numbering: restart}; {@code none} no numera ni avanza el contador.
 */
record Numbering(int[] printed, int[] totals) {

    static Numbering of(List<Section> sections, List<List<LayoutResult>> results) {
        int pages = results.stream().mapToInt(List::size).sum();
        int[] printed = new int[pages];
        int[] totals = new int[pages];
        int counter = 0;
        int tandaFirst = 0;
        int i = 0;
        for (int k = 0; k < sections.size(); k++) {
            Section.PageNumbering mode = sections.get(k).numbering();
            if (mode == Section.PageNumbering.RESTART) {
                Arrays.fill(totals, tandaFirst, i, counter);
                counter = 0;
                tandaFirst = i;
            }
            for (int j = 0; j < results.get(k).size(); j++) {
                printed[i++] = mode == Section.PageNumbering.NONE ? 0 : ++counter;
            }
        }
        Arrays.fill(totals, tandaFirst, i, counter);
        return new Numbering(printed, totals);
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
}
