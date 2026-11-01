package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Marker;

import java.util.List;
import java.util.Locale;

import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.PropertyKey.FONT_SIZE;
import static dev.stxt.pdf.tree.PropertyKey.NUMBER;
import static dev.stxt.pdf.tree.PropertyKey.START;

/**
 * Lists (reference/components/list.md): numbering of the {@code List item}, the generated box of their
 * marker and the rules of {@code Start} and {@code Number}.
 */
final class ListBuilder {

    private ListBuilder() {
    }

    /**
     * Number of a {@code List item} (reference/components/list.md): that of its {@code Number} or the one after that of
     * the previous item, starting with the {@code Start} of the list. In a list without numbering, 0.
     */
    static int itemNumber(BuildContext ctx, InlineNode n, Style s, Box list) {
        Marker marker = list.style().list().marker();
        if (!marker.numbered()) {
            if (s.isDeclared(NUMBER)) {
                throw error(s.line(NUMBER), ErrorCode.LIST_NUMBER_NOT_ALLOWED, "Number has no effect: the list "
                        + "(line " + list.line() + ") has Marker: " + markerName(marker) + ", which does not number its items");
            }
            return 0;
        }
        List<Box> previous = list.children();
        int number = s.list().number() != null ? s.list().number()
                : previous.isEmpty() ? list.style().list().start()
                : ctx.itemNumbers.get(previous.get(previous.size() - 1)) + 1;
        if (!ListMarkers.inRange(marker, number)) {
            throw error(s.list().number() != null ? s.line(NUMBER) : SourceLine.of(n.getLine()),
                    ErrorCode.LIST_NUMBER_RANGE,
                    "The number " + number + " cannot be written with Marker: " + markerName(marker)
                            + ", which numbers " + ListMarkers.range(marker));
        }
        return number;
    }

    /**
     * The generated box of the marker of a {@code List item} (reference/components/list.md): text with the typography
     * of the item, which needs, like any text, {@code Font} and {@code Size}.
     */
    static Box marker(InlineNode n, Box item, String text) {
        if (item.style().typography().font() == null) {
            throw error(n, ErrorCode.TEXT_WITHOUT_FONT, "The marker of the List item needs Font: it is declared in "
                    + "the Typography of the List item, of an ancestor or of Styles (For element: Document)");
        }
        if (!item.style().isDeclared(FONT_SIZE)) {
            throw error(n, ErrorCode.TEXT_WITHOUT_FONT_SIZE, "The marker of the List item needs Size: it is declared "
                    + "in the Typography of the List item, of an ancestor or of Styles (For element: Document)");
        }
        Style s = Style.inheriting(item.style());
        s.setLayout(Layout.INLINE);
        Box marker = new Box(null, Element.MARKER, s, false, n.getLine());
        marker.setText(text, n.getLine(), false);
        return marker;
    }

    /**
     * Checks that the marker of a {@code List item} has a line to align with
     * (reference/components/list.md): going down through the first in-flow child of each block leads to a text.
     */
    static void markerLine(Box item) {
        Box b = item;
        while (b.style().layout() != Layout.INLINE) {
            String problem = null;
            if (b.style().layout() == Layout.STACK || b.style().layout() == Layout.GRID) {
                problem = "starts with a " + b.element().label();
            } else if (b.isImage()) {
                problem = b.element() == Element.IMAGE ? "starts with an Image" : "starts with a " + b.element().label();
            } else if (b.inFlowChildren().isEmpty()) {
                problem = b == item ? "has no in-flow content" : "starts with an empty " + b.element().label();
            }
            if (problem != null) {
                throw error(b.line(), ErrorCode.LIST_ITEM_WITHOUT_LINE, "The marker of the List item at line "
                        + item.line() + " is aligned with its first line, and the item " + problem
                        + ": it has to start with a text, or have Marker: none");
            }
            b = b.inFlowChildren().get(0);
        }
    }

    /** {@code Start} only numbers, and within the range of the numbering. */
    static void validateList(Style s) {
        if (!s.isDeclared(START)) {
            return;
        }
        if (!s.list().marker().numbered()) {
            throw error(s.line(START), ErrorCode.LIST_NUMBER_NOT_ALLOWED, "Start has no effect with Marker: "
                    + markerName(s.list().marker()) + ", which does not number the items");
        }
        if (!ListMarkers.inRange(s.list().marker(), s.list().start())) {
            throw error(s.line(START), ErrorCode.LIST_NUMBER_RANGE, "Start: " + s.list().start() + " cannot be "
                    + "written with Marker: " + markerName(s.list().marker()) + ", which numbers "
                    + ListMarkers.range(s.list().marker()));
        }
    }

    /** A marker as it is written: {@code LOWER_ALPHA} → {@code lower-alpha}. */
    private static String markerName(Marker marker) {
        return marker.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
