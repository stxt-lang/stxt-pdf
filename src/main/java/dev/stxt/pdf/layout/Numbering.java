package dev.stxt.pdf.layout;

import dev.stxt.pdf.layout.inline.PageNumbers;
import dev.stxt.pdf.model.NumberFormat;
import dev.stxt.pdf.tree.Section;

import java.util.Arrays;
import java.util.List;

/**
 * The printed number of each physical page, the last one of its page sequence and how they are written
 * (reference/components/document.md): the page sequence is the pages since the last {@code Page numbering: restart},
 * which starts at its {@code Start}; {@code none} neither numbers nor advances the counter. Each page is written with
 * the {@code Page style} of its section. The blank page with which a section ends ({@code Begin on: odd}) is one more
 * page of it.
 */
record Numbering(int[] printed, int[] totals, NumberFormat[] styles) {

    /**
     * @param results    the flow of each section, one result per page
     * @param blankAfter whether each section ends with a blank page
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

    /** Printed number of the physical page {@code number} (from 1); 0 if it is not numbered. */
    int printed(int number) {
        return printed[number - 1];
    }

    /** Last number of the page sequence of the physical page {@code number}. */
    int total(int number) {
        return totals[number - 1];
    }

    /** How the number of the physical page {@code number} is written. */
    NumberFormat style(int number) {
        return styles[number - 1];
    }

    /** The printed number of the physical page {@code number}, written with its style ({@code {page}}, {@code iv}). */
    String label(int number) {
        return style(number).format(printed(number));
    }

    /** The last number of its page sequence, written with the style of the page ({@code {pages}}). */
    String totalLabel(int number) {
        return style(number).format(total(number));
    }

    /**
     * The probe number that measures at least as much as any printed number of the numbered pages: as many characters
     * as the longest, in digits ({@code 0}) or, if there are Roman numerals, with the widest letter ({@code M} or
     * {@code m}).
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
