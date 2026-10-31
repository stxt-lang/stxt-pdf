package dev.stxt.pdf.fragment;

/**
 * Quality of a break (reference/common/breaks.md): higher is better. {@link BreakRules#best} keeps
 * the last break of highest quality, which is the same as relaxing the rules in the order
 * of the reference.
 */
public final class BreakAppeal {

    /**
     * Forced page break ({@code Before} or {@code After: page} of {@code Break}): better than
     * any other, and different from a perfect break so that a {@code Columns} does not take it for
     * the end of a column and lets it through to the page (reference/components/columns.md).
     */
    public static final int FORCED_PAGE = 4;
    /** Break that violates no rule; also the forced column break. */
    public static final int PERFECT_BREAK = 3;
    /** Violates widows or orphans. */
    public static final int VIOLATES_WIDOWS = 2;
    /** Violates {@code Keep with next} or {@code Keep with previous} of {@code Break}. */
    public static final int VIOLATES_KEEP_BETWEEN = 1;
    /** Violates {@code Keep together} of {@code Break} or breaks by force. */
    public static final int FORCED = 0;

    private BreakAppeal() {
    }
}
