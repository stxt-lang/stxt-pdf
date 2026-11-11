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
 *
 * <p>It also says in which <strong>chapter</strong> the document is, for the figures numbered by chapter
 * ({@code Figure numbering: chapter}, reference/components/figure.md): the chapter is the last numbered heading with
 * no numbered heading above it, the one whose number has a single part. A heading without a number of the level of
 * the chapters or above ({@code Heading 1} while there has been no chapter) ends it: what follows is in no chapter
 * until the next one.</p>
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
    /** The number of the chapter the document is in; {@code null} outside any chapter. */
    private String chapter;
    /** The level of the headings that are chapters: that of the last one, and 1 before the first. */
    private int chapterLevel = 1;
    /** How many stretches there have been: it goes up when a chapter starts and when one ends. */
    private int stretch;

    /** The number of the chapter the document is in ({@code "2"}, {@code "B"}); {@code null} outside any chapter. */
    String chapter() {
        return chapter;
    }

    /**
     * The stretch of the document the reader is in: it changes with each chapter and with each heading that ends one,
     * which is when a counter by chapter starts again.
     */
    int stretch() {
        return stretch;
    }

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
            if (level <= chapterLevel) {
                chapter = null;
                stretch++;
            }
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
        if (parts.size() == 1) {
            chapter = parts.get(0);
            chapterLevel = level;
            stretch++;
        }
        return String.join(".", parts);
    }

    /**
     * Writes the number of a heading before its title, or that of a figure before its caption
     * (reference/components/figure.md), as one more piece of text: the layout, the table of contents and the PDF
     * bookmarks see it without treating it apart. It goes in {@code text} (the first {@code Text} of the heading, the
     * {@code Caption} of the figure), followed by a space; with {@code Number label}, the template with the number in
     * its {@code {number}}.
     */
    static void prefix(Box numbered, Box text, String number) {
        numbered.setNumber(number);
        String label = numbered.style().typography().numberLabel();
        text.prependText((label == null ? number : label.replace(NUMBER, number)) + " ");
    }
}
