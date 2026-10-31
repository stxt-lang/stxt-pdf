package dev.stxt.pdf.fragment;

import dev.stxt.pdf.model.Break;

/**
 * A unit of the fragmentation of a container (reference/common/breaks.md): a line of a
 * stack container in {@code row}, an item of one in {@code column} or a row of a grid. Breaks
 * happen between units, never inside one (except by breaking its items in parallel).
 *
 * @param start        start of the unit, from the content box of the container
 * @param end          end of the unit
 * @param appealBefore quality of the break before the unit ({@link BreakAppeal}); negative if
 *                     no break is possible there (a grid item that spans this row and the previous one).
 *                     That of the first unit is not used
 * @param forcedBefore forced break before the unit: {@code PAGE}, {@code COLUMN} or {@code AUTO}
 *                     if there is none ({@link BreakRules#forcedBetween(java.util.List, java.util.List)})
 */
public record Unit(double start, double end, int appealBefore, Break forcedBefore) {

    /** Whether a break is possible before the unit. */
    public boolean breakableBefore() {
        return appealBefore >= 0;
    }

    /** The same unit with the positions measured from {@code origin}. */
    public Unit shifted(double origin) {
        return new Unit(start - origin, end - origin, appealBefore, forcedBefore);
    }
}
