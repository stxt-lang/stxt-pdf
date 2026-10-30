package dev.stxt.pdf.tree;

import java.util.Map;

/**
 * Claves de las propiedades en {@link Style#declared}, {@link Style#isDeclared} y
 * {@link Style#line}: el nombre canónico de la propiedad ({@code min-width}); las de
 * {@code Flex}, {@code Grid} e {@code Item} (o {@code Area}), con prefijo ({@code grid.rows}),
 * porque el mismo nombre está en varios; y las de {@code Typography} y {@code Break} con nombre
 * corto, con su nombre interno ({@code Size} es {@code font-size}).
 *
 * <p>Las que el código consulta por su nombre están aquí como constantes, para que un error
 * tipográfico no compile; {@code PropertyKeyTest} comprueba que cada una es una propiedad del
 * schema en su sitio.</p>
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

    // Propiedades de los elementos
    public static final String OFFSET = "offset";
    public static final String LAYER = "layer";
    public static final String ROTATE = "rotate";
    public static final String ROTATE_ORIGIN = "rotate-origin";
    public static final String START = "start";
    public static final String NUMBER = "number";
    public static final String PDF_PAGE = "pdf-page";

    // Columns
    public static final String COUNT = "count";
    public static final String COLUMN_GAP = "gap";
    public static final String SEPARATOR = "separator";
    public static final String SEPARATOR_COLOR = "separator-color";

    // Break
    public static final String BREAK_BEFORE = "break-before";
    public static final String BREAK_AFTER = "break-after";
    public static final String BREAK_INSIDE = "break-inside";
    public static final String DECORATION = "decoration";

    // Flex, Grid e Item
    public static final String FLEX_HORIZONTAL = "flex.horizontal";
    public static final String FLEX_VERTICAL = "flex.vertical";
    public static final String FLEX_LINES = "flex.lines";
    public static final String FLEX_GAP = "flex.gap";
    public static final String GRID_ROWS = "grid.row-sizes";
    public static final String GRID_GAP = "grid.gap";
    public static final String ITEM_GROW = "item.grow";
    public static final String ITEM_BASIS = "item.basis";
    public static final String ITEM_HORIZONTAL = "item.horizontal";
    public static final String ITEM_VERTICAL = "item.vertical";
    /** La colocación de un ítem de grid: el valor de su {@code Area}, o la que calcula una {@code Table}. */
    public static final String ITEM_AREA = "item.area";

    /**
     * Nombre interno de cada propiedad de {@code Typography} y {@code Break} cuyo nombre en el
     * documento es corto.
     */
    private static final Map<String, String> INTERNAL = Map.of(
            "bold", FONT_WEIGHT, "italic", FONT_STYLE, "size", FONT_SIZE, "align", "text-align",
            "before", BREAK_BEFORE, "after", BREAK_AFTER, "inside", BREAK_INSIDE);

    private PropertyKey() {
    }

    /** Clave de una propiedad de {@code Typography} o {@code Break} por su nombre canónico ({@code size}). */
    static String internal(String canonicalName) {
        return INTERNAL.getOrDefault(canonicalName, canonicalName);
    }

    /** Clave de una propiedad de {@code Flex} ({@code gap} → {@code flex.gap}). */
    static String flex(String canonicalName) {
        return "flex." + canonicalName;
    }

    /** Clave de una propiedad de {@code Grid} ({@code rows} → {@code grid.rows}). */
    static String grid(String canonicalName) {
        return "grid." + canonicalName;
    }

    /** Clave de una propiedad de {@code Item} o {@code Area} ({@code grow} → {@code item.grow}). */
    static String item(String canonicalName) {
        return "item." + canonicalName;
    }
}
