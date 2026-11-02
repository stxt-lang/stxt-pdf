package dev.stxt.pdf.fragment;

import dev.stxt.pdf.model.Exclusion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where a box was broken and where its layout has to resume in the next
 * fragmentainer (reference/common/breaks.md). A {@code null} in its place means "the box has
 * finished".
 *
 * <p>They are {@code record}s: they have {@code equals}, and so they can be part of the key of the
 * layout cache and the paginator can detect that there is no progress.</p>
 */
public sealed interface BreakToken {

    /**
     * The box does not fit in what is left of the page and has placed nothing: the parent must break
     * before it. It is only returned away from the top of the page (progress guarantee) and
     * is never passed as a resume token: the box starts again from {@code null}.
     */
    BreakToken BEFORE = new Before();

    /** See {@link #BEFORE}. */
    record Before() implements BreakToken {
    }

    /**
     * Block: next child to place and, if the break was inside it, its own token.
     *
     * @param consumedHeight border box height of the previous fragments of the box (with
     *                       {@code Edges: closed}, of its content box); a definite
     *                       {@code Height} is the total of all the fragments
     */
    record Block(int nextChild, BreakToken childToken, double consumedHeight) implements BreakToken {
    }

    /**
     * Text: next line to place and height of the previous fragments (see {@link Block}). The lines are those of the
     * line breaking the fragment that returned the token was placed with: the one that starts at position
     * {@code from} of the text (0 is its start) with those {@code exclusions} (reference/components/block.md). A
     * fragment whose exclusions are different, or that has some, breaks the rest of the text again from the start of
     * line {@code nextLine}, because the lines beside a side box are not the lines of the text elsewhere.
     *
     * @param from       position of the text where that line breaking starts; 0 for the whole text
     * @param exclusions the exclusions of the box that line breaking was made with; empty if it had none
     */
    record Line(int nextLine, double consumedHeight, int from, List<Exclusion> exclusions) implements BreakToken {

        public Line {
            exclusions = List.copyOf(exclusions);
        }

        /** The token of a text broken into lines from its start, without exclusions. */
        public Line(int nextLine, double consumedHeight) {
            this(nextLine, consumedHeight, 0, List.of());
        }
    }

    /**
     * Stack or grid: next line or row (in a vertical stack, next item) and height of the
     * previous fragments.
     *
     * @param itemTokens empty if the break is before {@code nextLine}. If it is inside (a row that
     *                   does not fit on a page and whose items are broken in parallel), the token of
     *                   each item of the row in tree order, with {@code null} for those that have
     *                   already finished
     */
    record Container(int nextLine, List<BreakToken> itemTokens, double consumedHeight) implements BreakToken {
        public Container {
            // List.copyOf does not allow null.
            itemTokens = Collections.unmodifiableList(new ArrayList<>(itemTokens));
        }
    }

    /** Height consumed by the previous fragments of the box that returned the token. */
    static double consumed(BreakToken token) {
        // Java 17 has no pattern matching in switch.
        if (token instanceof Block b) {
            return b.consumedHeight();
        }
        if (token instanceof Line l) {
            return l.consumedHeight();
        }
        if (token instanceof Container c) {
            return c.consumedHeight();
        }
        return 0;
    }
}
