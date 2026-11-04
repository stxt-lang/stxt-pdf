package dev.stxt.pdf.layout;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Style;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Sizing rules of an item placed by its container in a space of its own: a {@code Stack} item in its line, a
 * {@code Grid} item in its area and, for the stretched height, an {@code Overlay} between top and bottom
 * (reference/common/box.md, reference/components/stack.md and reference/components/grid.md).
 *
 * <p>The caller resolves the {@link BoxModel} of the item with its own percentage bases (reference/common/values.md):
 * the content box of the container for a {@code Stack} item, its area for a {@code Grid} item, the reference
 * container for an {@code Overlay}. The rules are the same; the bases are not.</p>
 */
public final class ItemSizing {

    private ItemSizing() {
    }

    /**
     * Border box width of an item on the cross axis of a space {@code area} wide: the declared one, the space
     * minus its margins ({@code stretch}) or its fit to the content minus its margins, limited by {@code Min width}
     * and {@code Max width}.
     *
     * @param probe     box model of the item, which resolves its declared {@code Width}
     * @param area      width of the space of the item, which is also the base of its percentages
     * @param stretch   the item takes up the space
     * @param intrinsic intrinsic sizes of the item; they are only computed for the fit to the content
     */
    public static double width(Style s, BoxModel probe, double area, Insets margin, boolean stretch,
                               Supplier<IntrinsicSizes> intrinsic) {
        if (!s.box().width().isAuto()) {
            return probe.width();
        }
        double width = stretch ? area - margin.horizontal() : intrinsic.get().fit(area) - margin.horizontal();
        return BoxModel.constrainWidth(s, Math.max(0, width), area);
    }

    /**
     * Border box width of an item stretched to a space {@code space} wide: the space minus its margins, limited by
     * {@code Min width} and {@code Max width}; empty if the item declares its {@code Width}.
     *
     * @param base base of the percentages of {@code Min width} and {@code Max width}
     */
    public static OptionalDouble stretchedWidth(Style s, double space, Insets margin, double base) {
        if (!s.box().width().isAuto()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(BoxModel.constrainWidth(s, Math.max(0, space - margin.horizontal()), base));
    }

    /**
     * Border box height of an item stretched to a space {@code space} high: the space minus its margins, limited by
     * {@code Min height} and {@code Max height}; empty if the item has a definite height of its own.
     *
     * @param model box model of the item, resolved with the base of its percentages
     */
    public static OptionalDouble stretchedHeight(BoxModel model, double space, Insets margin) {
        if (model.height().isPresent()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Sizing.constrain(Math.max(0, space - margin.vertical()), model.minHeight(),
                model.maxHeight()));
    }

    /** The alignment of an item: its own one or, without it, the one of its container. */
    public static Alignment effective(Alignment own, Alignment container) {
        return own != null ? own : container;
    }

    /**
     * The {@code Overlay} and {@code Page overlay} children of a container that places its items itself, with the
     * corner of its content box as static position.
     */
    public static List<PendingOverlay> outOfFlow(Box box) {
        return box.children().stream()
                .filter(child -> child.style().isOutOfFlow())
                .map(child -> new PendingOverlay(child, 0, 0))
                .toList();
    }
}
