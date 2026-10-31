package dev.stxt.pdf.layout.position;

import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.tree.Style;

import java.util.OptionalDouble;

/**
 * Visual offset of a {@code Frame} (reference/components/frame-and-overlays.md). It is applied after placing
 * it in flow: its space is kept and its siblings do not move. Top and bottom, or left and right, are never both
 * declared ({@code OFFSET_CONFLICT}); if they were, top and left would win.
 *
 * @param dx horizontal offset
 * @param dy vertical offset
 */
public record FrameOffset(double dx, double dy) {

    public static final FrameOffset NONE = new FrameOffset(0, 0);

    /**
     * @param cbWidth  width of the reference container, base of left and right
     * @param cbHeight height of the reference container, base of top and bottom; empty = indefinite, and
     *                 then a percentage behaves as {@code auto}
     */
    public static FrameOffset of(Style s, double cbWidth, OptionalDouble cbHeight) {
        if (s.position() != Position.FRAME) {
            return NONE;
        }
        OptionalDouble left = s.offset().left().resolve(cbWidth);
        OptionalDouble right = s.offset().right().resolve(cbWidth);
        OptionalDouble top = s.offset().top().resolve(cbHeight);
        OptionalDouble bottom = s.offset().bottom().resolve(cbHeight);
        double dx = left.isPresent() ? left.getAsDouble() : -right.orElse(0);
        double dy = top.isPresent() ? top.getAsDouble() : -bottom.orElse(0);
        return new FrameOffset(dx, dy);
    }
}
