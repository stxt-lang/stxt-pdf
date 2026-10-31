package dev.stxt.pdf.fragment;

import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.tree.Box;

import java.util.List;
import java.util.function.ToIntFunction;

import static dev.stxt.pdf.model.Tolerance.EPSILON;
import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Break rules (reference/common/breaks.md): widows and orphans, quality of the breaks between
 * boxes, forced breaks and choice of the break. They are pure pieces; the layout algorithms
 * use them to decide where each box is broken.
 *
 * <p>The quality of a break is one of the constants of {@link BreakAppeal}: higher is better.
 * The <strong>last</strong> break of highest quality is chosen, which is the same as relaxing the
 * rules in the order of the reference (reference/common/breaks.md): first widows and orphans, then
 * {@code Keep with next} and {@code Keep with previous}, and last {@code Keep together} (of {@code Break}).</p>
 */
public final class BreakRules {

    private BreakRules() {
    }

    /**
     * How many lines of a paragraph to place in the available space.
     *
     * @param pending               lines left to place
     * @param capacity              lines that fit in what is left of the fragmentainer
     * @param widows                minimum number of lines that must move to the next one
     * @param orphans               minimum number of lines that must stay in this one
     * @param atFragmentainerStart  whether we are at the top of a page (progress guarantee)
     * @return lines to place here; 0 means "move the whole paragraph to the next one"
     */
    public static int linesThatFit(int pending, int capacity, int widows, int orphans,
                                   boolean atFragmentainerStart) {
        if (pending <= capacity) {
            return pending;
        }
        int k = capacity;
        if (pending - k < widows) {
            k = pending - widows;
        }
        if (k < orphans) {
            k = 0;
        }
        if (k <= 0 && atFragmentainerStart) {
            // Progress guarantee: at the top of a page something is always placed,
            // relaxing widows and orphans.
            k = Math.max(1, Math.min(capacity, pending));
        }
        return Math.max(k, 0);
    }

    /**
     * Lines to place when no break respects widows and orphans, but the parent may
     * prefer to break the paragraph rather than separate two boxes that are kept together: widows are respected
     * if possible (and orphans are violated) and, if not, the space is filled.
     *
     * @return between 1 and {@code capacity}; requires {@code 0 < capacity < pending}
     */
    public static int relaxedLines(int pending, int capacity, int widows) {
        int keepingWidows = pending - widows;
        return keepingWidows >= 1 ? Math.min(capacity, keepingWidows) : capacity;
    }

    /**
     * Break between the lines of a text, which is a single paragraph: those that fit from {@code start}, respecting
     * widows and orphans. If no break respects the rules, they are violated ({@link #relaxedLines}) so that the parent
     * can compare that break with breaking before the box.
     *
     * @param lines    lines of the text
     * @param start    first line of this fragment
     * @param capacity lines that fit in what is left of the page
     * @return first line that moves to the next fragment (equal to {@code start} if none is
     *         placed: break before the box) and the quality of the break
     */
    public static Cut cutLines(int lines, int start, int capacity, int widows, int orphans,
                               boolean atFragmentainerStart) {
        int pending = lines - start;
        if (pending <= capacity) {
            return new Cut(lines, BreakAppeal.PERFECT_BREAK);
        }
        int k = linesThatFit(pending, capacity, widows, orphans, false);
        if (k > 0) {
            return new Cut(start + k, BreakAppeal.PERFECT_BREAK);
        }
        if (capacity > 0) {
            return new Cut(start + relaxedLines(pending, capacity, widows), BreakAppeal.VIOLATES_WIDOWS);
        }
        // Not even one line fits: at the top of a page it is placed anyway (progress guarantee).
        return new Cut(atFragmentainerStart ? start + 1 : start, BreakAppeal.FORCED);
    }

    /** Capacity in lines of a vertical space. */
    public static int capacity(double space, double lineHeight) {
        if (Double.isInfinite(space)) {
            return Integer.MAX_VALUE;
        }
        if (space <= 0) {
            return 0;
        }
        return (int) Math.floor((space + EPSILON) / lineHeight);
    }

    // --- Breaks between boxes ---------------------------------------------------------------------

    /**
     * Quality of the break between two in-flow sibling boxes: {@code Keep with next} on the first or {@code Keep with
     * previous} on the second make it worse, also when they come from inside ({@link #keepsWithNext}).
     */
    public static int between(Box before, Box after) {
        return keepsWithNext(before) || keepsWithPrevious(after)
                ? BreakAppeal.VIOLATES_KEEP_BETWEEN : BreakAppeal.PERFECT_BREAK;
    }

    /**
     * {@code Keep with next} of the box or, if it is a block, of its last in-flow child: like the forced breaks
     * ({@link #forcedAfter}), it propagates to the parent, because both end at the same point. A heading that ends a
     * block is not left alone at the end of the page.
     */
    private static boolean keepsWithNext(Box box) {
        if (box.style().breaks().keepWithNext()) {
            return true;
        }
        List<Box> children = propagates(box) ? box.inFlowChildren() : List.of();
        return !children.isEmpty() && keepsWithNext(children.get(children.size() - 1));
    }

    /** {@code Keep with previous} of the box or, if it is a block, of its first in-flow child ({@link #keepsWithNext}). */
    private static boolean keepsWithPrevious(Box box) {
        if (box.style().breaks().keepWithPrevious()) {
            return true;
        }
        List<Box> children = propagates(box) ? box.inFlowChildren() : List.of();
        return !children.isEmpty() && keepsWithPrevious(children.get(0));
    }

    /**
     * The forced break between two in-flow sibling boxes: {@code PAGE}, {@code COLUMN} (only
     * inside a {@code Columns}) or {@code AUTO} if there is none. If there are both, the page
     * break wins.
     */
    public static Break forcedBetween(Box before, Box after) {
        return stronger(forcedAfter(before), forcedBefore(after));
    }

    /**
     * The forced break before the box ({@code Before} of {@code Break}) or, if it is a block, before
     * its first in-flow child: the forced break of the first child propagates to the parent,
     * because both start at the same point. Only the page break comes out of a {@code Columns}:
     * the column break stays in its first column.
     *
     * @return {@code PAGE}, {@code COLUMN} or {@code AUTO}
     */
    public static Break forcedBefore(Box box) {
        Break own = forced(box.style().breaks().before());
        List<Box> children = propagates(box) ? box.inFlowChildren() : List.of();
        return children.isEmpty() ? own : stronger(own, fromInside(box, forcedBefore(children.get(0))));
    }

    /** The forced break after the box or, if it is a block, after its last in-flow child ({@link #forcedBefore}). */
    public static Break forcedAfter(Box box) {
        Break own = forced(box.style().breaks().after());
        List<Box> children = propagates(box) ? box.inFlowChildren() : List.of();
        return children.isEmpty() ? own
                : stronger(own, fromInside(box, forcedAfter(children.get(children.size() - 1))));
    }

    /** Whether there is a forced break: {@code PAGE} or {@code COLUMN}. */
    public static boolean isForced(Break forced) {
        return forced == Break.PAGE || forced == Break.COLUMN;
    }

    /**
     * Quality of a forced break: {@link BreakAppeal#FORCED_PAGE} for a page break, which must get out
     * of the columns, and {@link BreakAppeal#PERFECT_BREAK} for a column break.
     */
    public static int forcedAppeal(Break forced) {
        return forced == Break.PAGE ? BreakAppeal.FORCED_PAGE : BreakAppeal.PERFECT_BREAK;
    }

    /** The forced breaks of a block and of a {@code Columns} are also those of their first and last children. */
    private static boolean propagates(Box box) {
        return box.style().layout() == Layout.BLOCK || box.style().layout() == Layout.COLUMNS;
    }

    /** The forced break of a first or last child that leaves the box: from a {@code Columns}, only page breaks. */
    private static Break fromInside(Box box, Break forced) {
        return box.style().layout() == Layout.COLUMNS && forced == Break.COLUMN ? Break.AUTO : forced;
    }

    private static Break forced(Break b) {
        return isForced(b) ? b : Break.AUTO;
    }

    private static Break stronger(Break a, Break b) {
        if (a == Break.PAGE || b == Break.PAGE) {
            return Break.PAGE;
        }
        return a == Break.COLUMN || b == Break.COLUMN ? Break.COLUMN : Break.AUTO;
    }

    /**
     * Quality of the break between two groups of boxes, such as two stack lines or two rows of a
     * grid: the worst of all the pairs.
     *
     * @param before boxes that end before the break
     * @param after  boxes that start after it
     */
    public static int between(List<Box> before, List<Box> after) {
        int appeal = BreakAppeal.PERFECT_BREAK;
        for (Box b : before) {
            for (Box a : after) {
                appeal = Math.min(appeal, between(b, a));
            }
        }
        return appeal;
    }

    /** The forced break between two groups of boxes (as {@link #between(List, List)} and {@link #forcedBetween}). */
    public static Break forcedBetween(List<Box> before, List<Box> after) {
        Break forced = Break.AUTO;
        for (Box b : before) {
            forced = stronger(forced, forcedAfter(b));
        }
        for (Box a : after) {
            forced = stronger(forced, forcedBefore(a));
        }
        return forced;
    }

    /** Quality of a break inside a box: {@code Keep together} lowers it to {@link BreakAppeal#FORCED}. */
    public static int inside(Box box, int appeal) {
        return box.style().breaks().keepTogether() ? BreakAppeal.FORCED : appeal;
    }

    // --- Choice of the break ----------------------------------------------------------------------

    /**
     * The last of the breaks of highest quality; {@code null} if there is none.
     *
     * @param candidates possible breaks, in order
     * @param appeal     quality of each break (constants of {@link BreakAppeal})
     */
    public static <T> T best(List<T> candidates, ToIntFunction<T> appeal) {
        T best = null;
        for (T c : candidates) {
            if (best == null || appeal.applyAsInt(c) >= appeal.applyAsInt(best)) {
                best = c;
            }
        }
        return best;
    }

    /**
     * Result of {@link #cutUnits}.
     *
     * @param end    first unit that moves to the next fragment; equal to the number of units if
     *               all fit, and equal to the first if none fits (break before the box)
     * @param appeal quality of the break
     */
    public record Cut(int end, int appeal) {
    }

    /**
     * Break between units that are not broken ({@link Unit}). The positions are those of the unfragmented
     * layout, from the start of the content box.
     *
     * @param units     the units of the container
     * @param first     first unit of this fragment
     * @param space     height available from the start of unit {@code first}
     * @param mustPlace progress guarantee: at least one unit has to be placed even if it overflows
     */
    public static Cut cutUnits(List<Unit> units, int first, double space, boolean mustPlace) {
        int n = units.size();
        if (first >= n) {
            return new Cut(n, BreakAppeal.PERFECT_BREAK);
        }
        double origin = units.get(first).start();
        int best = -1;
        boolean overflowing = false;
        for (int i = first; i < n; i++) {
            Unit u = units.get(i);
            if (i > first) {
                if (isForced(u.forcedBefore())) {
                    return new Cut(i, forcedAppeal(u.forcedBefore()));
                }
                if (u.breakableBefore()) {
                    if (overflowing) {
                        // What could not be separated was placed by force: break as soon as possible.
                        return new Cut(i, BreakAppeal.FORCED);
                    }
                    if (best < 0 || u.appealBefore() >= units.get(best).appealBefore()) {
                        best = i;
                    }
                }
            }
            if (!overflowing && !fits(u.end() - origin, space)) {
                if (best >= 0) {
                    return new Cut(best, units.get(best).appealBefore());
                }
                if (!mustPlace) {
                    return new Cut(first, BreakAppeal.FORCED);
                }
                overflowing = true;
            }
        }
        return new Cut(n, BreakAppeal.PERFECT_BREAK);
    }
}
