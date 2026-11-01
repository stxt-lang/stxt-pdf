package dev.stxt.pdf.tree;

import java.util.Map;

/**
 * Keys of the properties in {@link Style#declared}, {@link Style#isDeclared} and {@link Style#line}: the canonical name
 * of the property ({@code min-width}); those of {@code Stack}, {@code Grid} and {@code Item} (or {@code Area}), with a
 * prefix ({@code grid.rows}), because the same name is in several; and those of {@code Typography} and {@code Break}
 * with a short name, with their internal name ({@code Size} is {@code font-size}).
 *
 * <p>The ones the code looks up by name are here as constants, so that a typo does not compile;
 * {@code PropertyKeyTest} checks that each one is a property of the schema in its place.</p>
 */
public final class PropertyKey {

    // Box
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String MIN_WIDTH = "min-width";
    public static final String MAX_WIDTH = "max-width";
    public static final String MIN_HEIGHT = "min-height";
    public static final String MAX_HEIGHT = "max-height";
    public static final String ASPECT_RATIO = "aspect-ratio";
    public static final String MARGIN = "margin";
    public static final String PADDING = "padding";
    public static final String BORDER = "border";

    // Typography
    public static final String FONT = "font";
    public static final String FONT_WEIGHT = "font-weight";
    public static final String FONT_STYLE = "font-style";
    public static final String FONT_SIZE = "font-size";
    public static final String LINE_HEIGHT = "line-height";
    public static final String COLOR = "color";
    public static final String CODE_FONT = "code-font";
    public static final String HYPHENATE = "hyphenate";

    // Properties of the elements
    public static final String OFFSET = "offset";
    public static final String LAYER = "layer";
    public static final String ROTATE = "rotate";
    public static final String ROTATE_ORIGIN = "rotate-origin";
    public static final String START = "start";
    public static final String NUMBER = "number";
    public static final String PDF_PAGE = "pdf-page";
    public static final String ERROR_CORRECTION = "error-correction";

    // Columns
    public static final String COUNT = "count";
    public static final String COLUMN_GAP = "gap";
    public static final String SEPARATOR = "separator";
    public static final String SEPARATOR_COLOR = "separator-color";

    // Break
    public static final String BREAK_BEFORE = "break-before";
    public static final String BREAK_AFTER = "break-after";
    public static final String KEEP_WITH_NEXT = "keep-with-next";
    public static final String KEEP_WITH_PREVIOUS = "keep-with-previous";
    public static final String KEEP_TOGETHER = "keep-together";
    public static final String DECORATION = "edges";

    // Stack, Grid and Item
    public static final String STACK_HORIZONTAL = "stack.horizontal";
    public static final String STACK_VERTICAL = "stack.vertical";
    public static final String STACK_LINE_DISTRIBUTION = "stack.line-distribution";
    public static final String STACK_GAP = "stack.gap";
    public static final String GRID_ROWS = "grid.row-sizes";
    public static final String GRID_GAP = "grid.gap";
    public static final String ITEM_GROW = "item.grow";
    public static final String ITEM_BASE_SIZE = "item.base-size";
    public static final String ITEM_HORIZONTAL = "item.horizontal";
    public static final String ITEM_VERTICAL = "item.vertical";
    /** The placement of a grid item: the value of its {@code Area}, or the one a {@code Table} computes. */
    public static final String ITEM_AREA = "item.area";

    /**
     * Internal name of each property of {@code Typography} and {@code Break} whose name in the document is short.
     */
    private static final Map<String, String> INTERNAL = Map.of(
            "bold", FONT_WEIGHT, "weight", FONT_WEIGHT, "italic", FONT_STYLE, "size", FONT_SIZE,
            "align", "text-align", "before", BREAK_BEFORE, "after", BREAK_AFTER);

    private PropertyKey() {
    }

    /** Key of a property of {@code Typography} or {@code Break} by its canonical name ({@code size}). */
    static String internal(String canonicalName) {
        return INTERNAL.getOrDefault(canonicalName, canonicalName);
    }

    /** Key of a property of {@code Stack} ({@code gap} → {@code stack.gap}). */
    static String stack(String canonicalName) {
        return "stack." + canonicalName;
    }

    /** Key of a property of {@code Grid} ({@code rows} → {@code grid.rows}). */
    static String grid(String canonicalName) {
        return "grid." + canonicalName;
    }

    /** Key of a property of {@code Item} or {@code Area} ({@code grow} → {@code item.grow}). */
    static String item(String canonicalName) {
        return "item." + canonicalName;
    }
}
