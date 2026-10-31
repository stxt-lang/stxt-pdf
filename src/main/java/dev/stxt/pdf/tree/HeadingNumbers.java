package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.NumberFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * The numbering of the headings (reference/components/heading.md): {@code 1}, {@code 1.1}, {@code II.a}...
 *
 * <p>It is counted in document order, across the sections and the {@code Markdown}: one
 * counter per level that goes up with each numbered heading of that level and goes back to 0 with any
 * heading of a higher level (numbered or not). A heading with {@code Numbered: false} neither
 * counts nor carries a number. The number of a heading of level N is the counters of the levels
 * 1 to N that are numbered, each one written with the {@code Number format} of its heading: under a title without a
 * number (a {@code Heading 1}), the numbered {@code Heading 2} are 1, 2, 3...</p>
 */
final class HeadingNumbers {

    /** What {@code Number label} replaces with the number. */
    static final String NUMBER = "{number}";

    /** The deepest heading level ({@code Heading 6}). */
    private static final int LEVELS = 6;

    /** Counter of each level (index 0 is not used). */
    private final int[] counters = new int[LEVELS + 1];
    /** Whether the last heading of each level is numbered: that of each previous level is an ancestor of the next. */
    private final boolean[] numbered = new boolean[LEVELS + 1];
    /** How the number of each level is written: the {@code Number format} of its last heading. */
    private final NumberFormat[] styles = new NumberFormat[LEVELS + 1];

    /**
     * Records a heading in document order and returns its number ({@code "4.2"}, {@code "II.a"}), or
     * {@code null} if it is not numbered.
     */
    String next(int level, boolean number, NumberFormat style) {
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
            // Ancestors without a number contribute nothing: their counter does not exist.
            if (l == level || numbered[l]) {
                parts.add(styles[l].format(counters[l]));
            }
        }
        return String.join(".", parts);
    }

    /**
     * Writes the number before the title of the heading, as one more piece of text: the layout, the
     * table of contents and the PDF bookmarks see it without treating it apart. It goes in the first {@code Text},
     * followed by a space; with {@code Number label}, the template with the number in its {@code {number}}.
     */
    static void prefix(Box heading, String number) {
        heading.setNumber(number);
        String label = heading.style().typography().numberLabel();
        heading.children().get(0).prependText((label == null ? number : label.replace(NUMBER, number)) + " ");
    }
}
