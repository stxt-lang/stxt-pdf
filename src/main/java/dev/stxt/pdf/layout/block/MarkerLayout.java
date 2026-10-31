package dev.stxt.pdf.layout.block;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Style;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Marker of a {@code List item} (reference/components/list.md): a one-line fragment that
 * hangs to the left of the border box of the item, with its baseline on that of the first line
 * of the item.
 *
 * <p>The first line is the one found by going down through the first in-flow child of each
 * fragment until one with lines; the tree already guarantees that it exists ({@code LIST_ITEM_WITHOUT_LINE}).
 * It is only searched for in the first fragments: if the item is broken, the marker goes in the fragment
 * with the first line, which is the first one of the item.</p>
 */
final class MarkerLayout {

    private MarkerLayout() {
    }

    /**
     * The fragment of the marker, relative to the content box of the item; empty if the
     * child fragments do not contain the first line.
     *
     * @param children child fragments of the first fragment of the item, already placed
     * @param border   border of the fragment of the item
     * @param padding  padding of the fragment of the item
     */
    static Optional<Fragment> place(Box item, List<Fragment> children, Insets border, Insets padding,
                                    TextMeasurer measurer) {
        OptionalDouble baseline = firstBaseline(children, measurer);
        if (baseline.isEmpty()) {
            return Optional.empty();
        }
        Box marker = item.marker();
        Style s = marker.style();
        double width = measurer.width(marker.text(), s.typography().fontFace(), s.typography().fontSize());
        double height = s.typography().usedLineHeight();
        // Its right edge, with the trailing space, touches the border box of the item.
        double x = -border.left() - padding.left() - width;
        double y = baseline.getAsDouble() - measurer.vertical(s.typography().fontFace()).baseline(0, height, s.typography().fontSize());
        // The marker box is generated: without margin, border or padding.
        return Optional.of(new Fragment(marker, x, y, width, height, Insets.ZERO, Insets.ZERO, Insets.ZERO,
                List.of(), List.of(new TextLine(0, 0, width, height, marker.text())), true, true));
    }

    /**
     * Baseline of the first line of some child fragments, relative to the content box of
     * their parent: that of the first in-flow child, going down to a fragment with lines.
     */
    static OptionalDouble firstBaseline(List<Fragment> children, TextMeasurer measurer) {
        double top = 0;
        Optional<Fragment> next = firstInFlow(children);
        while (next.isPresent() && next.get().first()) {
            Fragment f = next.get();
            double contentTop = top + f.y() + f.border().top() + f.padding().top();
            if (!f.lines().isEmpty()) {
                TextLine l = f.lines().get(0);
                Style s = f.box().style();
                return OptionalDouble.of(measurer.vertical(s.typography().fontFace())
                        .baseline(contentTop + l.y(), l.height(), s.typography().fontSize()));
            }
            top = contentTop;
            next = firstInFlow(f.children());
        }
        return OptionalDouble.empty();
    }

    /** The first in-flow fragment, not counting the markers. */
    private static Optional<Fragment> firstInFlow(List<Fragment> fragments) {
        return fragments.stream()
                .filter(f -> !f.box().style().isOutOfFlow() && !f.box().isMarker())
                .findFirst();
    }
}
