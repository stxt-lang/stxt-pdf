package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakAppeal;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.tree.Box;

import java.util.List;

/**
 * Immutable output of the layout of a box (modules 7 and 8).
 *
 * @param fragment    the fragment produced in this fragmentainer
 * @param breakToken  where it was broken; {@code null} if the box has finished and
 *                    {@link BreakToken#BEFORE} if nothing of it fits (the parent breaks before)
 * @param breakAppeal quality of the chosen break (one of the constants of {@link BreakAppeal})
 * @param pending     absolute boxes whose reference container is an ancestor of this box, with the
 *                    static position relative to the content box of this box
 * @param adjoining   margins of the descendants that merge outside the box (reference/components/block.md)
 */
public record LayoutResult(Fragment fragment, BreakToken breakToken, int breakAppeal,
                           List<PendingOverlay> pending, AdjoiningMargins adjoining) {

    public LayoutResult {
        pending = List.copyOf(pending);
    }

    /** Nothing of the box fits in what is left of the page: the parent must break before it. */
    public static LayoutResult before(Box box) {
        return new LayoutResult(Fragment.empty(box), BreakToken.BEFORE, BreakAppeal.PERFECT_BREAK,
                List.of(), AdjoiningMargins.NONE);
    }

    public static LayoutResult finished(Fragment fragment) {
        return finished(fragment, List.of(), AdjoiningMargins.NONE);
    }

    public static LayoutResult finished(Fragment fragment, List<PendingOverlay> pending, AdjoiningMargins adjoining) {
        return new LayoutResult(fragment, null, BreakAppeal.PERFECT_BREAK, pending, adjoining);
    }

    public boolean isFinished() {
        return breakToken == null;
    }
}
