package dev.stxt.pdf.layout;

import dev.stxt.pdf.tree.Box;

/**
 * {@code absolute} or {@code fixed} box found during the in-flow layout, pending to be resolved when its
 * reference container knows its size. It goes up through the {@link LayoutResult}s to it; each box translates the
 * position to its own content box. Resolved by {@link dev.stxt.pdf.layout.position.OverlayLayout}.
 *
 * @param box     the absolute box
 * @param staticX position it would have had in flow (for left {@code auto}), relative to
 *                the content box of the box that returns the result
 * @param staticY likewise (for top {@code auto})
 */
public record PendingOverlay(Box box, double staticX, double staticY) {

    public PendingOverlay translated(double dx, double dy) {
        return new PendingOverlay(box, staticX + dx, staticY + dy);
    }
}
