package dev.stxt.pdf.layout.block;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.Baselines;
import dev.stxt.pdf.layout.LayoutContext;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Style;
import dev.stxt.pdf.tree.TypographyStyle;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Marker of a {@code List item} (reference/components/list.md): a fragment that hangs to the left of the border box
 * of the item, aligned with its first line: a one-line text fragment with its baseline on that of the line or,
 * with {@code Marker: image} or a marker the engine draws ({@code circle}, {@code square}, {@code checkbox}), an image
 * or a shape centered on the capitals of the item's typography on that baseline.
 *
 * <p>The first line is the one found by going down through the first in-flow child of each
 * fragment until one with lines; the tree already guarantees that it exists ({@code LIST_ITEM_WITHOUT_LINE}).
 * It is only searched for in the first fragments: if the item is broken, the marker goes in the fragment
 * with the first line, which is the first one of the item.</p>
 */
final class MarkerLayout {

    /** The side of a circle or a square marker, as a part of the height of the capitals of the item. */
    private static final double SMALL_SHAPE = 0.5;

    private MarkerLayout() {
    }

    /**
     * The fragment of the marker, relative to the content box of the item; empty if the
     * child fragments do not contain the first line.
     *
     * @param children child fragments of the first fragment of the item, already placed
     * @param border   border of the fragment of the item
     * @param padding  padding of the fragment of the item
     * @param context  the text metrics, and the natural size of the image of an image marker
     */
    static Optional<Fragment> place(Box item, List<Fragment> children, Insets border, Insets padding,
                                    LayoutContext context) {
        TextMeasurer measurer = context.measurer();
        OptionalDouble baseline = Baselines.first(children, measurer);
        if (baseline.isEmpty()) {
            return Optional.empty();
        }
        Box marker = item.marker();
        Style s = marker.style();
        TypographyStyle t = s.typography();
        if (marker.isImage() || marker.isMarkerShape()) {
            double capitals = measurer.vertical(t.fontFace()).capHeight() * t.fontSize();
            // An image is as tall as its Height or the Size of the item and as wide as its proportion gives; a shape
            // is a square, as tall as the capitals of the item if it is a checkbox and half of them if not. Both are
            // followed by the space of the item's font, which is their right padding: the content box is the drawing.
            double height = marker.isMarkerShape() ? capitals * (s.list().marker() == Marker.CHECKBOX ? 1 : SMALL_SHAPE)
                    : s.list().markerHeight() != null ? s.list().markerHeight() : t.fontSize();
            double width = marker.isMarkerShape() ? height : height * context.natural(marker).ratio();
            double space = measurer.width(" ", t.fontFace(), t.fontSize());
            double x = -border.left() - padding.left() - width - space;
            // Its center is the middle of the capitals of the item's typography on the baseline of the first line.
            double y = baseline.getAsDouble() - capitals / 2 - height / 2;
            return Optional.of(new Fragment(marker, x, y, width + space, height, Insets.ZERO, Insets.ZERO,
                    new Insets(0, space, 0, 0), List.of(), List.of(), true, true));
        }
        double width = measurer.width(marker.text(), t.fontFace(), t.fontSize());
        double height = t.usedLineHeight();
        // Its right edge, with the trailing space, touches the border box of the item.
        double x = -border.left() - padding.left() - width;
        double y = baseline.getAsDouble() - measurer.vertical(t.fontFace()).baseline(0, height, t.fontSize());
        // The marker box is generated: without margin, border or padding.
        return Optional.of(new Fragment(marker, x, y, width, height, Insets.ZERO, Insets.ZERO, Insets.ZERO,
                List.of(), List.of(new TextLine(0, 0, width, height, t.fontSize(), marker.text())), true, true));
    }
}
