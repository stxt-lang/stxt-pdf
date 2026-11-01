package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.BarcodeFormat;
import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Direction;
import dev.stxt.pdf.model.Distribution;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.model.Value;

import java.util.List;
import java.util.Locale;

import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.label;
import static dev.stxt.pdf.tree.Nodes.position;
import static dev.stxt.pdf.tree.PropertyKey.ASPECT_RATIO;
import static dev.stxt.pdf.tree.PropertyKey.BREAK_AFTER;
import static dev.stxt.pdf.tree.PropertyKey.BREAK_BEFORE;
import static dev.stxt.pdf.tree.PropertyKey.KEEP_TOGETHER;
import static dev.stxt.pdf.tree.PropertyKey.KEEP_WITH_NEXT;
import static dev.stxt.pdf.tree.PropertyKey.KEEP_WITH_PREVIOUS;
import static dev.stxt.pdf.tree.PropertyKey.DECORATION;
import static dev.stxt.pdf.tree.PropertyKey.ERROR_CORRECTION;
import static dev.stxt.pdf.tree.PropertyKey.COLUMN_GAP;
import static dev.stxt.pdf.tree.PropertyKey.SEPARATOR;
import static dev.stxt.pdf.tree.PropertyKey.SEPARATOR_COLOR;
import static dev.stxt.pdf.tree.PropertyKey.STACK_GAP;
import static dev.stxt.pdf.tree.PropertyKey.STACK_LINE_DISTRIBUTION;
import static dev.stxt.pdf.tree.PropertyKey.FONT_SIZE;
import static dev.stxt.pdf.tree.PropertyKey.GRID_GAP;
import static dev.stxt.pdf.tree.PropertyKey.GRID_ROWS;
import static dev.stxt.pdf.tree.PropertyKey.HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.ITEM_BASE_SIZE;
import static dev.stxt.pdf.tree.PropertyKey.ITEM_GROW;
import static dev.stxt.pdf.tree.PropertyKey.ITEM_HORIZONTAL;
import static dev.stxt.pdf.tree.PropertyKey.ITEM_VERTICAL;
import static dev.stxt.pdf.tree.PropertyKey.MARGIN;
import static dev.stxt.pdf.tree.PropertyKey.MAX_HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.MAX_WIDTH;
import static dev.stxt.pdf.tree.PropertyKey.MIN_HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.MIN_WIDTH;
import static dev.stxt.pdf.tree.PropertyKey.OFFSET;
import static dev.stxt.pdf.tree.PropertyKey.ROTATE;
import static dev.stxt.pdf.tree.PropertyKey.ROTATE_ORIGIN;
import static dev.stxt.pdf.tree.PropertyKey.WIDTH;
import static dev.stxt.pdf.tree.ValueReader.amount;
import static dev.stxt.pdf.tree.ValueReader.comparable;
import static dev.stxt.pdf.tree.ValueReader.hasPercentage;

/**
 * Model rules that the schema cannot express (reference/common/errors.md) and that look at the box once read: offsets,
 * {@code auto} margins, minimum and maximum, vertical percentages and whether the height is definite
 * (reference/common/values.md), and the font of the texts.
 */
final class ModelValidator {

    private ModelValidator() {
    }

    /**
     * {@code Horizontal} and {@code Vertical}, {@code Line distribution} and {@code Gap} of a {@code Stack}: the one of
     * the main axis distributes and does not allow {@code stretch}; the one of the cross axis aligns and does not allow
     * {@code justify}, {@code around} or {@code distribute} (reference/components/stack.md).
     */
    static void validateStack(Style s) {
        String main = s.stack().isHorizontal() ? "Horizontal" : "Vertical";
        String cross = s.stack().isHorizontal() ? "Vertical" : "Horizontal";
        String direction = "Direction: " + (s.stack().isHorizontal() ? "horizontal" : "vertical");
        if (s.stack().mainAxis() == Distribution.STRETCH) {
            throw error(s.line(s.stack().mainAxisKey()), ErrorCode.VALUE_NOT_ALLOWED, main + " is the main axis of "
                    + "this Stack (" + direction + ") and distributes the free space: it does not allow stretch. What stretches the "
                    + "items is their Grow");
        }
        String spread = crossSpread(s);
        if (spread != null) {
            throw error(s.line(s.stack().crossAxisKey()), ErrorCode.VALUE_NOT_ALLOWED, cross + " is the cross axis of "
                    + "this Stack (" + direction + ") and aligns the items: it allows start, end, center or stretch and not "
                    + spread + ", which distributes the main axis (" + main + ")");
        }
        if (s.isDeclared(STACK_LINE_DISTRIBUTION) && !s.stack().wrap()) {
            throw error(s.line(STACK_LINE_DISTRIBUTION), ErrorCode.LINE_DISTRIBUTION_WITHOUT_WRAP,
                    "Line distribution only has an effect with Wrap: true, because without Wrap there is a single line");
        }
        // Without Wrap there is a single line: no gap between rows in horizontal or between columns in vertical.
        if (s.stack().gapPair() && !s.stack().wrap()) {
            String unused = s.stack().direction() == Direction.HORIZONTAL
                    ? "first, the gap between rows," : "second, the gap between columns,";
            throw error(s.line(STACK_GAP), ErrorCode.GAP_WITHOUT_WRAP, "Gap has two values, but without Wrap there is a single"
                    + " line and the " + unused + " has no effect: one value is enough, the gap between items");
        }
    }

    /**
     * A {@code Columns} sets its columns with {@code Count}, {@code Min column width} or both; with a single column
     * there is no gap or rule between columns, {@code Separator color} needs {@code Separator} and the rule fits in the
     * gap (reference/components/columns.md).
     */
    static void validateColumns(Style s, int line) {
        if (s.columns().count() == null && s.columns().minColumnWidth() == null) {
            throw error(line, ErrorCode.COLUMNS_WITHOUT_COUNT, "A Columns needs Count, Min column width or both: "
                    + "Count sets the number of columns and Min column width their minimum width");
        }
        if (s.columns().count() != null && s.columns().count() == 1) {
            for (String p : List.of(COLUMN_GAP, SEPARATOR)) {
                if (s.isDeclared(p)) {
                    throw error(s.line(p), ErrorCode.SINGLE_COLUMN, (p.equals(SEPARATOR) ? "Separator" : "Gap")
                            + " has no effect with Count: 1, because there are not two columns to separate: it is not needed");
                }
            }
        }
        if (s.isDeclared(SEPARATOR_COLOR) && !s.isDeclared(SEPARATOR)) {
            throw error(s.line(SEPARATOR_COLOR), ErrorCode.SEPARATOR_COLOR_WITHOUT_SEPARATOR,
                    "Separator color has no effect without Separator, the thickness of the rule between columns: the "
                            + "thickness is missing or the color is not needed");
        }
        if (s.columns().separator() > s.columns().columnGap() + Tolerance.EPSILON) {
            String gap = s.isDeclared(COLUMN_GAP) ? "Gap (" + s.line(COLUMN_GAP) + ")"
                    : "Gap, which is 0 when not declared";
            throw error(s.line(SEPARATOR), ErrorCode.SEPARATOR_WIDTH, "Separator is wider than " + gap
                    + ": the rule would be painted over the columns. Gap has to be widened or Separator made thinner");
        }
    }

    /**
     * An {@code Image} or a {@code Barcode} does not split (reference/components/image.md): {@code Keep together} and
     * {@code Edges}, which only count when a box splits, have no effect (also if a rule of {@code Styles} gives them).
     *
     * @param element how the element is named in the message: {@code an Image}, {@code a Barcode}
     */
    static void validateImage(Style s, String element) {
        for (String p : List.of(KEEP_TOGETHER, DECORATION)) {
            if (s.isDeclared(p)) {
                throw error(s.line(p), ErrorCode.BREAK_IN_IMAGE, (p.equals(KEEP_TOGETHER) ? "Keep together" : "Edges")
                        + " has no effect on " + element + ", which does not split across pages: it is not needed");
            }
        }
    }

    /**
     * The properties of a {@code Barcode} (reference/components/barcode.md): {@code Alt} is required, {@code Error
     * correction} only has an effect on a QR code and, of its own {@code Typography}, only {@code Color} has one (the
     * color of the modules). What a rule of {@code Styles} gives it is not checked, as with any box without text.
     */
    static void validateBarcode(InlineNode n, Style s) {
        if (s.image().alt() == null) {
            throw error(n, ErrorCode.BARCODE_WITHOUT_ALT, "The Barcode needs Alt, the text that says what it is and "
                    + "where it leads to whoever cannot see it, as in Alt: QR code of the invoice verification");
        }
        if (s.isDeclared(ERROR_CORRECTION) && s.image().format() != BarcodeFormat.QR) {
            throw error(s.line(ERROR_CORRECTION), ErrorCode.BARCODE_PROPERTY_WITHOUT_EFFECT, "Error correction only "
                    + "has an effect on a QR code, and the Barcode is " + s.image().format().name().toLowerCase(Locale.ROOT)
                    .replace('_', '-') + ": it is not needed");
        }
        Nodes.child(n, "typography").map(Nodes::inline).ifPresent(t -> t.getChildren().stream()
                .filter(p -> !p.getCanonicalName().equals("color")).findFirst().ifPresent(p -> {
                    throw error(p, ErrorCode.BARCODE_PROPERTY_WITHOUT_EFFECT, p.getName() + " has no effect on a "
                            + "Barcode, which has no text: of its Typography, only Color has one, the color of the "
                            + "modules");
                }));
    }

    /**
     * The rotation of a box (reference/common/layers-and-rotation.md): {@code Rotate origin} without {@code Rotate} has
     * no effect, and a rotated {@code Frame} or a box rotated in flow does not split across pages, so neither do
     * {@code Keep together} and {@code Edges} (also if a rule of {@code Styles} gives them). A box rotated in flow
     * already isolates margins: {@code Isolate margins} has no effect on it.
     */
    static void validateRotation(Style s) {
        if (s.rotation() == null) {
            if (s.isDeclared(ROTATE_ORIGIN)) {
                throw error(s.line(ROTATE_ORIGIN), ErrorCode.ROTATE_ORIGIN_WITHOUT_ROTATE, "Rotate origin has no "
                        + "effect without Rotate: the rotation is missing or Rotate origin is not needed");
            }
            return;
        }
        if (s.position() != Position.FRAME && !s.rotatesInFlow()) {
            return;
        }
        String what = s.rotatesInFlow() ? "a rotated box" : "a rotated Frame";
        for (String p : List.of(KEEP_TOGETHER, DECORATION)) {
            if (s.isDeclared(p)) {
                throw error(s.line(p), ErrorCode.BREAK_IN_ROTATED, (p.equals(KEEP_TOGETHER) ? "Keep together" : "Edges")
                        + " has no effect on " + what + ", which does not split across pages: it is not needed");
            }
        }
        if (s.rotatesInFlow() && s.isolateMargins()) {
            throw error(s.line("isolate-margins"), ErrorCode.ISOLATE_MARGINS_IN_ROTATED, "Isolate margins has no effect on a rotated box, "
                    + "which already stops margin merging with its children: it is not needed");
        }
    }

    /**
     * The rules of a box already read, before it is created: placement, offset, margins, sizes, percentages and column
     * breaks.
     */
    static void validateBox(BuildContext ctx, Style s, Box parent) {
        Layout parentLayout = parent.style().layout();
        // The in-flow children of a Stack or a Grid are items (with their Item or their Area), and the cells
        // of a Table, items of its grid.
        boolean stackItem = !s.isOutOfFlow() && parentLayout == Layout.STACK;
        boolean gridItem = !s.isOutOfFlow() && parentLayout == Layout.GRID;
        if ((stackItem || gridItem) && s.rotatesInFlow()) {
            // An item takes up the place its container gives it (reference/common/layers-and-rotation.md).
            throw error(s.line(ROTATE), ErrorCode.ROTATED_ITEM, "A " + (stackItem ? "Stack" : "Grid")
                    + " item cannot rotate: it takes up the place its container gives it. What rotates is a box inside the item (a "
                    + "Block with the rotated box inside)");
        }

        if (stackItem) {
            validateStackItem(s, parent);
        }
        validateOffset(s);
        validateMargins(s, parentLayout, stackItem, gridItem);
        if (s.isDeclared(ASPECT_RATIO) && !s.box().width().isAuto() && !s.box().height().isAuto()) {
            throw error(s.line(ASPECT_RATIO), ErrorCode.ASPECT_RATIO_CONFLICT,
                    "Aspect ratio has no effect if Width and Height are declared");
        }
        validateMinMax(s, WIDTH, s.box().width(), MIN_WIDTH, s.box().minWidth(), MAX_WIDTH, s.box().maxWidth());
        validateMinMax(s, HEIGHT, s.box().height(), MIN_HEIGHT, s.box().minHeight(), MAX_HEIGHT, s.box().maxHeight());
        validatePercentages(ctx, s, parent, stackItem, gridItem);
        validateColumnBreaks(ctx, s);
        validateKeeps(s);
    }

    /**
     * The value of the cross axis of a {@code Stack} if it is a distribution ({@code justify}...), which is not valid
     * there; otherwise, {@code null}.
     */
    private static String crossSpread(Style s) {
        Distribution v = s.stack().crossDeclared();
        return v == Distribution.JUSTIFY || v == Distribution.AROUND || v == Distribution.DISTRIBUTE
                ? v.name().toLowerCase(Locale.ROOT) : null;
    }

    /**
     * In a {@code Stack} item, {@code Horizontal} or {@code Vertical} only on the cross axis: on the main one the
     * {@code Stack} distributes, with its own property, and the item grows or shrinks with {@code Grow} and
     * {@code Shrink} (reference/components/stack.md).
     */
    private static void validateStackItem(Style s, Box parent) {
        boolean horizontal = parent.style().stack().isHorizontal();
        String key = horizontal ? ITEM_HORIZONTAL : ITEM_VERTICAL;
        if (s.isDeclared(key)) {
            String main = horizontal ? "Horizontal" : "Vertical";
            String cross = horizontal ? "Vertical" : "Horizontal";
            throw error(s.line(key), ErrorCode.ITEM_MAIN_AXIS, main + " has no effect on an item of a Stack with "
                    + "Direction: " + main.toLowerCase(Locale.ROOT) + ", because it is its main axis: the distribution "
                    + "comes from the " + main + " of the Stack and the size from Grow and Shrink, and the item is aligned with " + cross);
        }
    }

    /** {@code Before} and {@code After: column} only inside a {@code Columns}, which is what has columns. */
    private static void validateColumnBreaks(BuildContext ctx, Style s) {
        if (ctx.insideColumns) {
            return;
        }
        String property = s.breaks().before() == Break.COLUMN ? BREAK_BEFORE
                : s.breaks().after() == Break.COLUMN ? BREAK_AFTER : null;
        if (property != null) {
            throw error(s.line(property), ErrorCode.COLUMN_BREAK_OUTSIDE_COLUMNS, (property.equals(BREAK_BEFORE)
                    ? "Before" : "After") + ": column only has an effect inside a Columns: a page "
                    + "break uses page");
        }
    }

    /**
     * {@code Keep with next} with a break after the box, or {@code Keep with previous} with one before: the box cannot
     * go with the other one and be separated from it at the same time (reference/common/breaks.md).
     */
    private static void validateKeeps(Style s) {
        BreakStyle b = s.breaks();
        if (b.keepWithNext() && b.after() != Break.AUTO) {
            throw error(s.line(KEEP_WITH_NEXT), ErrorCode.KEEP_CONFLICT, "Keep with next keeps the box with the "
                    + "next one and After: " + value(b.after()) + " separates it from it: one of the two is not needed");
        }
        if (b.keepWithPrevious() && b.before() != Break.AUTO) {
            throw error(s.line(KEEP_WITH_PREVIOUS), ErrorCode.KEEP_CONFLICT, "Keep with previous keeps the box with "
                    + "the previous one and Before: " + value(b.before()) + " separates it from it: one of the two is not needed");
        }
    }

    private static String value(Break b) {
        return b.name().toLowerCase(Locale.ROOT);
    }

    private static void validateOffset(Style s) {
        if (!s.isDeclared(OFFSET)) {
            return;
        }
        SourceLine line = s.line(OFFSET);
        Sides<Value> o = s.offset();
        if (s.position() == Position.FRAME) {
            if (!o.top().isAuto() && !o.bottom().isAuto()) {
                throw error(line, ErrorCode.OFFSET_CONFLICT, "A Frame cannot be offset both "
                        + "from the top and from the bottom: one of the two has to stay auto");
            }
            if (!o.left().isAuto() && !o.right().isAuto()) {
                throw error(line, ErrorCode.OFFSET_CONFLICT, "A Frame cannot be offset both "
                        + "from the left and from the right: one of the two has to stay auto");
            }
            return;
        }
        // Overlay and Page overlay: start + size + end cannot set the position twice.
        if (!o.left().isAuto() && !s.box().width().isAuto() && !o.right().isAuto()
                && !s.box().margin().left().isAuto() && !s.box().margin().right().isAuto()) {
            throw error(line, ErrorCode.OVERCONSTRAINED, "Left, Width and right, all declared, set the "
                    + "position twice: one has to stay auto, or the box is centered with Margin auto");
        }
        if (!o.top().isAuto() && !s.box().height().isAuto() && !o.bottom().isAuto()
                && !s.box().margin().top().isAuto() && !s.box().margin().bottom().isAuto()) {
            throw error(line, ErrorCode.OVERCONSTRAINED, "Top, Height and bottom, all declared, set the "
                    + "position twice: one has to stay auto, or the box is centered with Margin auto");
        }
    }

    /**
     * {@code auto} margins only center: in block flow, horizontally and with a limited width; in Overlay and Page
     * overlay, with start, size and end.
     */
    private static void validateMargins(Style s, Layout parentLayout, boolean stackItem, boolean gridItem) {
        Sides<Value> m = s.box().margin();
        boolean horizontalAuto = m.left().isAuto() || m.right().isAuto();
        boolean verticalAuto = m.top().isAuto() || m.bottom().isAuto();
        if (!horizontalAuto && !verticalAuto) {
            return;
        }
        SourceLine line = s.line(MARGIN);
        if (stackItem || gridItem) {
            throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto has no effect on items of a Stack or "
                    + "Grid: they are aligned with Horizontal and Vertical");
        }
        if (s.isOutOfFlow()) {
            validateOutOfFlowMargins(s, line, horizontalAuto, verticalAuto);
            return;
        }
        if (parentLayout == Layout.BLOCK) {
            if (verticalAuto) {
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT,
                        "Vertical Margin auto has no effect in block flow: a number is needed");
            }
            // An image with Width auto measures its natural width, and a box rotated in flow, its content: not the
            // whole width, so they can be centered.
            if (s.box().width().isAuto() && s.box().maxWidth() == null && s.layout() != Layout.IMAGE
                    && !s.rotatesInFlow()) {
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto does not center a box that takes up "
                        + "the whole width: Width or Max width is missing");
            }
        }
    }

    /**
     * An {@code Overlay} or a {@code Page overlay} is only centered with {@code Margin auto} if it declares start, size
     * and end.
     */
    private static void validateOutOfFlowMargins(Style s, SourceLine line, boolean horizontalAuto,
                                                 boolean verticalAuto) {
        Sides<Value> o = s.offset();
        if (horizontalAuto && (o.left().isAuto() || s.box().width().isAuto() || o.right().isAuto())) {
            throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Horizontal Margin auto only centers the "
                    + position(s) + " when left, Width and right are declared");
        }
        if (verticalAuto && (o.top().isAuto() || s.box().height().isAuto() || o.bottom().isAuto())) {
            throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Vertical Margin auto only centers the "
                    + position(s) + " when top, Height and bottom are declared");
        }
    }

    /** Comparable size, minimum and maximum (both in points or both in percentage) that contradict each other. */
    private static void validateMinMax(Style s, String size, Value value, String min, Value minValue,
                                       String max, Value maxValue) {
        if (maxValue != null && comparable(minValue, maxValue) && amount(minValue) > amount(maxValue)) {
            throw error(s.line(min), ErrorCode.MIN_MAX_CONFLICT,
                    label(min) + " is greater than " + label(max) + " (" + s.line(max) + ")");
        }
        if (comparable(value, minValue) && amount(value) < amount(minValue)) {
            throw error(s.line(size), ErrorCode.MIN_MAX_CONFLICT,
                    label(size) + " is less than " + label(min) + " (" + s.line(min) + ")");
        }
        if (maxValue != null && comparable(value, maxValue) && amount(value) > amount(maxValue)) {
            throw error(s.line(size), ErrorCode.MIN_MAX_CONFLICT,
                    label(size) + " is greater than " + label(max) + " (" + s.line(max) + ")");
        }
    }

    /**
     * Vertical percentages of the box: their base must be a definite height (reference/common/values.md). The base of
     * {@code Height}, {@code Min height} and {@code Max height} is the reference container (always definite for Overlay
     * and Page overlay) or the area (grid items); that of the offsets of a Frame and of {@code Base size} in a
     * {@code column} stack, the content box of the parent.
     */
    private static void validatePercentages(BuildContext ctx, Style s, Box parent, boolean stackItem,
                                            boolean gridItem) {
        boolean ownBase = s.isOutOfFlow() || gridItem || ctx.isDefinite(parent);
        for (String p : List.of(HEIGHT, MIN_HEIGHT, MAX_HEIGHT)) {
            Value v = switch (p) {
                case HEIGHT -> s.box().height();
                case MIN_HEIGHT -> s.box().minHeight();
                default -> s.box().maxHeight();
            };
            if (!ownBase && v != null && hasPercentage(v)) {
                throw indefinite(s.line(p), label(p));
            }
        }
        boolean parentDefinite = ctx.isDefinite(parent);
        if (s.position() == Position.FRAME && !parentDefinite
                && (hasPercentage(s.offset().top()) || hasPercentage(s.offset().bottom()))) {
            throw indefinite(s.line(OFFSET), "The vertical offset of Offset");
        }
        boolean column = parent.style().stack().direction() == Direction.VERTICAL;
        if (stackItem && column && !parentDefinite) {
            if (hasPercentage(s.item().baseSize())) {
                throw indefinite(s.line(ITEM_BASE_SIZE), "Base size in a vertical Stack");
            }
            if (s.item().grow() > 0) {
                throw error(s.line(ITEM_GROW), ErrorCode.GROW_WITHOUT_EFFECT, "Grow has no effect: the vertical "
                        + "Stack of the parent has no definite height to distribute: its Height is missing");
            }
        }
    }

    /** Vertical percentages of the container itself: rows and gap between rows. */
    static void validateContainer(BuildContext ctx, Style s, Box box) {
        if (ctx.isDefinite(box)) {
            return;
        }
        if (s.grid().rowSizes().stream().anyMatch(ValueReader::hasPercentage)) {
            throw indefinite(s.line(GRID_ROWS), "Row sizes");
        }
        // In a stack, the gap between rows only counts in vertical or with several lines.
        boolean rowGapUsed = s.layout() == Layout.GRID
                || (s.layout() == Layout.STACK && (s.stack().wrap() || s.stack().direction() == Direction.VERTICAL));
        Value rowGap = s.layout() == Layout.GRID ? s.grid().rowGap() : s.stack().rowGap();
        if (rowGapUsed && hasPercentage(rowGap)) {
            String gap = s.layout() == Layout.GRID ? GRID_GAP : STACK_GAP;
            throw indefinite(s.line(gap), "The gap between rows of Gap");
        }
    }

    /**
     * Whether the height of the content box is definite, without laying out: a {@code Height} that resolves (points, or
     * a percentage of a definite base), {@code Aspect ratio} with {@code auto} height, an Overlay or a Page overlay
     * stretched between top and bottom, or a grid item stretched ({@code Vertical: stretch}) in its area.
     */
    static boolean definiteHeight(BuildContext ctx, Style s, Box parent) {
        boolean gridItem = !s.isOutOfFlow() && parent.style().layout() == Layout.GRID;
        boolean base = s.isOutOfFlow() || gridItem || ctx.isDefinite(parent);
        Value h = s.box().height();
        if (!h.isAuto()) {
            return base || !hasPercentage(h);
        }
        if (h.isAuto() && s.box().aspectRatio() != null) {
            return true;
        }
        if (h.isAuto() && s.isOutOfFlow() && !s.offset().top().isAuto() && !s.offset().bottom().isAuto()) {
            return true;
        }
        Alignment vertical = s.item().vertical() != null ? s.item().vertical() : parent.style().grid().vertical();
        return h.isAuto() && gridItem && vertical == Alignment.STRETCH;
    }

    private static InvalidModelException indefinite(SourceLine line, String what) {
        return error(line, ErrorCode.INDEFINITE_PERCENTAGE, what + " uses a percentage of a height that is not "
                + "definite: the Height of its reference container is needed, or a length in points or millimeters");
    }

    /** A text needs {@code Font} and {@code Size}, its own or inherited; the error points to {@code source}. */
    static void font(InlineNode n, Node source, Box box) {
        if (box.style().typography().font() == null) {
            throw error(source, ErrorCode.TEXT_WITHOUT_FONT, "The text needs Font: it is declared in the Typography of the "
                    + n.getName() + ", of an ancestor or of Styles (For element: Document)");
        }
        if (!box.style().isDeclared(FONT_SIZE)) {
            throw error(source, ErrorCode.TEXT_WITHOUT_FONT_SIZE, "The text needs Size: it is declared in the Typography of the "
                    + n.getName() + ", of an ancestor or of Styles (For element: Document)");
        }
    }
}
