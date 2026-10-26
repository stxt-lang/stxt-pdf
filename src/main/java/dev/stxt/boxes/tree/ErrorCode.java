package dev.stxt.boxes.tree;

/**
 * Código estable de cada error del modelo (ESPECIFICACION.md §6). Los tests comparan el código y
 * la línea, no el texto del mensaje, que puede mejorar sin romperlos. Los errores de sintaxis
 * STXT y del template llevan los códigos de la librería {@code stxt-core}
 * ({@code INVALID_VALUE}, {@code TOO_FEW_CHILDREN}…).
 */
public enum ErrorCode {

    // --- Documento y medio ------------------------------------------------------------------
    /** El documento no tiene un único nodo raíz {@code Document} con el namespace del modelo. */
    DOCUMENT_ROOT,
    /**
     * Estructura que el template ya rechaza (falta {@code Media}, un nodo con hijos escrito como
     * bloque {@code >>}…): solo aparece si se construye el árbol sin validar el documento.
     */
    DOCUMENT_STRUCTURE,
    /** Con {@code Type: paged}, {@code Media} necesita {@code Height}. */
    MEDIA_HEIGHT_REQUIRED,
    /** {@code Height} del medio con {@code Type: continuous}, donde no tiene efecto. */
    MEDIA_HEIGHT_CONTINUOUS,
    /** Un tamaño o un margen del medio no válido, o márgenes que no dejan área útil. */
    MEDIA_SIZE,

    // --- Valores ------------------------------------------------------------------------------
    /** Sintaxis de un valor (§3): número, porcentaje, función, lista… */
    VALUE_SYNTAX,
    /** Un valor bien escrito que la propiedad no admite ({@code Width: 1fr}, {@code Padding: -2}…). */
    VALUE_NOT_ALLOWED,
    /** {@code fit-content} como pista de grid. */
    FIT_CONTENT_TRACK,

    // --- Estructura ---------------------------------------------------------------------------
    /** Dos cajas con el mismo {@code Id}. */
    DUPLICATE_ID,
    /** Una caja {@code Layout: inline} con cajas hijas. */
    INLINE_WITH_BOXES,
    /** Texto sin {@code Font}, propia o heredada. */
    TEXT_WITHOUT_FONT,

    // --- Combinaciones sin efecto o contradictorias --------------------------------------------
    /** {@code Flex} sin {@code Layout: flex} o {@code Grid} sin {@code Layout: grid}. */
    CONTAINER_MISMATCH,
    /** {@code Item} o una de sus propiedades en una caja que no es ítem del contenedor que la usa. */
    ITEM_MISMATCH,
    /** {@code Offset} con {@code Position: static}. */
    OFFSET_ON_STATIC,
    /** Una {@code relative} con arriba y abajo, o con izquierda y derecha. */
    OFFSET_CONFLICT,
    /** Una {@code absolute} o {@code fixed} con inicio, tamaño y fin declarados y sin márgenes {@code auto}. */
    OVERCONSTRAINED,
    /** Un margen {@code auto} donde no centra ni absorbe nada. */
    MARGIN_AUTO_WITHOUT_EFFECT,
    /** {@code Layer} en una caja que no crea contexto de apilamiento. */
    LAYER_WITHOUT_EFFECT,
    /** {@code Lines} sin {@code Wrap: true}. */
    LINES_WITHOUT_WRAP,
    /** {@code Grow} en un contenedor {@code column} sin alto definido. */
    GROW_WITHOUT_EFFECT,
    /** {@code Aspect ratio} con {@code Width} y {@code Height} declarados. */
    ASPECT_RATIO_CONFLICT,
    /** Mínimo mayor que el máximo, o un tamaño fuera de su mínimo y su máximo. */
    MIN_MAX_CONFLICT,
    /** Un porcentaje vertical cuya base no es un alto definido. */
    INDEFINITE_PERCENTAGE,
    /** Un corte forzado ({@code Break before/after: page}) en una caja fuera de flujo. */
    BREAK_NOT_ALLOWED,
    /** {@code Areas} no rectangulares o {@code Cell} no válida. */
    GRID_PLACEMENT,

    // --- Fuentes ------------------------------------------------------------------------------
    /** {@code Font} no está en la carpeta de fuentes. */
    FONT_UNKNOWN,
    /** La familia no tiene la variante de {@code Font weight} y {@code Font style}. */
    FONT_VARIANT_MISSING,
    /** La fuente no tiene un carácter del texto. */
    GLYPH_MISSING
}
