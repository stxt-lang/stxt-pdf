package dev.stxt.pdf.tree;

/**
 * A line and the document it is in: the main one or the separate styles document ({@code Styles},
 * reference/common/styles.md). It is what {@link Style} keeps for each declared property, because a property that comes
 * from a rule of the styles document has the line of that document, and the errors that point to it have to say so.
 *
 * @param number   the line number, from 1
 * @param inStyles whether the line is in the separate styles document
 */
public record SourceLine(int number, boolean inStyles) {

    /** A line of the main document. */
    public static SourceLine of(int number) {
        return new SourceLine(number, false);
    }

    /** A line of the separate styles document. */
    public static SourceLine inStyles(int number) {
        return new SourceLine(number, true);
    }

    /** {@code line 8} or {@code line 8 of the styles}, for messages. */
    @Override
    public String toString() {
        return "line " + number + (inStyles ? " of the styles" : "");
    }
}
