package dev.stxt.pdf.tree;

/**
 * A line and the document it is in: the main one or one of the separate styles documents ({@code Styles},
 * reference/common/styles.md). It is what {@link Style} keeps for each declared property, because a property that comes
 * from a rule of a styles document has the line of that document, and the errors that point to it have to say so.
 *
 * @param number   the line number, from 1
 * @param styles   the separate styles document the line is in, by its place among those of the conversion, from 1;
 *                 0 if the line is in the main document
 * @param numbered whether the messages say which styles document it is: there are several
 */
public record SourceLine(int number, int styles, boolean numbered) {

    /** A line of the main document. */
    public static SourceLine of(int number) {
        return new SourceLine(number, 0, false);
    }

    /** A line of the only separate styles document, or of one that is read on its own. */
    public static SourceLine inStyles(int number) {
        return new SourceLine(number, 1, false);
    }

    /**
     * A line of one of the separate styles documents: the one in the place {@code styles}, from 1; {@code numbered},
     * if there are several and the messages have to say which one.
     */
    public static SourceLine inStyles(int number, int styles, boolean numbered) {
        if (styles < 1) {
            throw new IllegalArgumentException("The place of a styles document starts at 1: " + styles);
        }
        return new SourceLine(number, styles, numbered);
    }

    /** Whether the line is in a separate styles document. */
    public boolean inStyles() {
        return styles > 0;
    }

    /** Another line of the same document. */
    public SourceLine at(int line) {
        return new SourceLine(line, styles, numbered);
    }

    /**
     * For messages: {@code line 8}, {@code line 8 of the styles} or, if there are several styles documents,
     * {@code line 8 of styles 2}.
     */
    @Override
    public String toString() {
        return "line " + number + (styles == 0 ? "" : numbered ? " of styles " + styles : " of the styles");
    }
}
