package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakAppeal;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.model.Edges;
import dev.stxt.pdf.tree.Box;

import java.util.List;

/**
 * Immutable output of the layout of a box (modules 7 and 8).
 *
 * @param fragment    the fragment produced in this fragmentainer
 * @param breakToken  where it was broken; {@code null} if the box has finished and
 *                    {@link BreakToken#BEFORE} if nothing of it fits (the parent breaks before)
 * @param breakAppeal quality of the chosen break (one of the constants of {@link BreakAppeal})
 * @param pending     Overlay boxes whose reference container is an ancestor of this box, with the
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

    /**
     * Height of this fragment that counts as taken up in the content of the parent: the height of the fragment or,
     * in a box with a definite height that is broken while its content goes on, only what that content takes up
     * (reference/common/breaks.md#definite-height-and-breaks): the fragment reaches the end of the page, but the gap
     * it leaves after its last line or child is not consumed, neither by the box nor by the boxes that contain it.
     *
     * @param from token with which this fragment of the box started; {@code null} in its first one
     */
    public double occupied(BreakToken from) {
        if (isFinished()) {
            return fragment.height();
        }
        double consumed = BreakToken.consumed(breakToken) - BreakToken.consumed(from);
        // With Edges: closed the consumed height is that of the content box: each fragment adds its decoration.
        if (fragment.box().style().breaks().edges() == Edges.CLOSED) {
            consumed += fragment.border().vertical() + fragment.padding().vertical();
        }
        return Math.min(fragment.height(), Math.max(0, consumed));
    }
}
