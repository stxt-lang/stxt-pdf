package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Marker;

import java.util.List;
import java.util.Locale;

import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.PropertyKey.FONT_SIZE;
import static dev.stxt.pdf.tree.PropertyKey.MARKER_IMAGE;
import static dev.stxt.pdf.tree.PropertyKey.NUMBER;
import static dev.stxt.pdf.tree.PropertyKey.FIRST_NUMBER;

/**
 * Lists (reference/components/list.md): numbering of the {@code List item}, the generated box of their
 * marker (text, or the image of {@code Marker image}) and the rules of {@code First number}, {@code Number} and
 * {@code Marker image}.
 */
final class ListBuilder {

    private ListBuilder() {
    }

    /**
     * Number of a {@code List item} (reference/components/list.md): that of its {@code Number} or the one after that of
     * the previous item, starting with the {@code First number} of the list. In a list without numbering, 0.
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
                : previous.isEmpty() ? list.style().list().firstNumber()
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
     * The generated box of the marker of a {@code List item} (reference/components/list.md), or {@code null} with
     * {@code Marker: none}: text with the typography of the item or, with {@code Marker: image}, the image of the
     * {@code Marker image} of the item (which replaces that of the list whole) or of the list. Both need
     * {@code Font} and {@code Size}: the text, like any text; the image, for the space that follows it and for its
     * height when it does not declare {@code Height}.
     */
    static Box marker(InlineNode n, Box item, Box list, int number) {
        Marker marker = list.style().list().marker();
        String text = ListMarkers.text(marker, number);
        if (text == null && marker != Marker.IMAGE) {
            return null;
        }
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
        if (marker == Marker.IMAGE) {
            Style owner = item.style().list().markerImage() != null ? item.style() : list.style();
            if (owner.list().markerImage() == null) {
                throw error(n, ErrorCode.LIST_IMAGE_MISSING, "The List item needs Marker image: the list (line "
                        + list.line() + ") has Marker: image and declares no Marker image for its items");
            }
            // The line of the Marker image: the errors of the file (IMAGE_UNKNOWN, IMAGE_FORMAT) point to it.
            Box image = new Box(null, Element.MARKER, s, false, owner.line(MARKER_IMAGE).number());
            s.list().setMarkerImage(owner.list());
            s.image().setAlt(owner.list().markerAlt());
            image.setImage(owner.list().markerImage());
            return image;
        }
        Box box = new Box(null, Element.MARKER, s, false, n.getLine());
        box.setText(text, n.getLine(), false);
        return box;
    }

    /** {@code Marker image} in a {@code List item} only with {@code Marker: image} in its list. */
    static void validateItem(Style s, Box list) {
        Marker marker = list.style().list().marker();
        if (s.isDeclared(MARKER_IMAGE) && marker != Marker.IMAGE) {
            throw error(s.line(MARKER_IMAGE), ErrorCode.LIST_IMAGE_NOT_ALLOWED, "Marker image has no effect: the list "
                    + "(line " + list.line() + ") has Marker: " + markerName(marker) + ", and only Marker: image "
                    + "draws an image");
        }
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

    /**
     * {@code Marker image} only with {@code Marker: image}; {@code First number} only numbers, and within the range of the
     * numbering.
     */
    static void validateList(Style s) {
        if (s.isDeclared(MARKER_IMAGE) && s.list().marker() != Marker.IMAGE) {
            throw error(s.line(MARKER_IMAGE), ErrorCode.LIST_IMAGE_NOT_ALLOWED, "Marker image has no effect with "
                    + "Marker: " + markerName(s.list().marker()) + ": only Marker: image draws an image");
        }
        if (!s.isDeclared(FIRST_NUMBER)) {
            return;
        }
        if (!s.list().marker().numbered()) {
            throw error(s.line(FIRST_NUMBER), ErrorCode.LIST_NUMBER_NOT_ALLOWED, "First number has no effect with Marker: "
                    + markerName(s.list().marker()) + ", which does not number the items");
        }
        if (!ListMarkers.inRange(s.list().marker(), s.list().firstNumber())) {
            throw error(s.line(FIRST_NUMBER), ErrorCode.LIST_NUMBER_RANGE, "First number: " + s.list().firstNumber() + " cannot be "
                    + "written with Marker: " + markerName(s.list().marker()) + ", which numbers "
                    + ListMarkers.range(s.list().marker()));
        }
    }

    /** A marker as it is written: {@code LOWER_ALPHA} → {@code lower-alpha}. */
    private static String markerName(Marker marker) {
        return marker.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
