package dev.stxt.boxes.tree;

/**
 * Código estable de cada error del modelo (ESPECIFICACION.md §6). Los tests comparan el código y
 * la línea, no el texto del mensaje, que puede mejorar sin romperlos. Los errores de sintaxis
 * STXT y del schema llevan los códigos de la librería {@code stxt-core}
 * ({@code INVALID_VALUE}, {@code CHILD_NOT_DECLARED}…).
 */
public enum ErrorCode {

    // --- Documento y medio ------------------------------------------------------------------
    /** El documento no tiene un único nodo raíz {@code Document} con el namespace del modelo. */
    DOCUMENT_ROOT,
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
    /** Un {@code Item} o un {@code Cell} que no contiene exactamente un elemento. */
    SINGLE_ELEMENT,
    /** Un {@code Text} sin texto, o con texto en el valor y en {@code Content}. */
    TEXT_CONTENT,
    /** Texto sin {@code Font} en {@code Typography}, propia o heredada. */
    TEXT_WITHOUT_FONT,
    /** Texto sin {@code Size} en {@code Typography}, propio o heredado. */
    TEXT_WITHOUT_FONT_SIZE,

    // --- Encabezados y párrafos ----------------------------------------------------------------
    /** Un salto en la jerarquía de encabezados: el primero no es {@code Heading 1}, o uno sube más de un nivel. */
    HEADING_LEVEL,
    /** Un encabezado dentro de un {@code Fixed}. */
    HEADING_IN_FIXED,
    /** Un {@code Text} de un encabezado con {@code Break}: el motor ya evita el corte. */
    BREAK_IN_HEADING,
    /** Un {@code Paragraph} con líneas en blanco, que lo partirían en varios párrafos. */
    PARAGRAPH_BLANK_LINE,

    // --- Listas ----------------------------------------------------------------------------------
    /** Un {@code List item} sin ningún elemento. */
    LIST_ITEM_EMPTY,
    /** {@code Start} o {@code Number} en una lista que no numera ({@code disc}, {@code dash}, {@code none}). */
    LIST_NUMBER_NOT_ALLOWED,
    /** Un número que la numeración no puede escribir: negativo, 0 en letras o romanos, o más de 3999 en romanos. */
    LIST_NUMBER_RANGE,
    /** Un ítem con marcador cuyo contenido no empieza con un texto con el que alinearlo. */
    LIST_ITEM_WITHOUT_LINE,

    // --- Reglas reutilizables (Styles) ---------------------------------------------------------
    /** {@code Element} con un nombre que no es un elemento de caja. */
    STYLE_UNKNOWN_ELEMENT,
    /** {@code For id} con un {@code Id} que no tiene ningún elemento. */
    STYLE_UNKNOWN_ID,
    /** Dos reglas con el mismo selector. */
    STYLE_DUPLICATE,
    /**
     * Una regla aplica un grupo que el elemento no admite: {@code Break} en {@code Absolute},
     * {@code Fixed}, un encabezado o un {@code Text} de un encabezado.
     */
    STYLE_NOT_ALLOWED,
    /** Una clase de {@code Class} sin regla {@code For class}. */
    CLASS_WITHOUT_RULE,

    // --- Combinaciones sin efecto o contradictorias --------------------------------------------
    /** Un {@code Relative} con arriba y abajo, o con izquierda y derecha. */
    OFFSET_CONFLICT,
    /** Un {@code Absolute} o un {@code Fixed} con inicio, tamaño y fin declarados y sin márgenes {@code auto}. */
    OVERCONSTRAINED,
    /** Un margen {@code auto} donde no centra ni absorbe nada. */
    MARGIN_AUTO_WITHOUT_EFFECT,
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
    /** {@code Areas} no rectangulares o un valor de {@code Cell} no válido. */
    GRID_PLACEMENT,

    // --- Desbordamientos (al maquetar) -------------------------------------------------------
    /** Una palabra más ancha que su línea. */
    TEXT_OVERFLOW,
    /** Una caja que no cabe en su contenedor, contenido que pasa de un alto fijado, o un {@code Absolute} que sale de la página. */
    BOX_OVERFLOW,
    /** Algo del flujo más alto que la página que no se puede partir. */
    PAGE_OVERFLOW,
    /** El marcador de un ítem no cabe en la sangría: el relleno izquierdo de la lista más el margen del ítem. */
    LIST_MARKER_OVERFLOW,

    // --- Fuentes ------------------------------------------------------------------------------
    /** {@code Font} no está en la carpeta de fuentes. */
    FONT_UNKNOWN,
    /** La familia no tiene la variante de {@code Weight} y {@code Style}. */
    FONT_VARIANT_MISSING,
    /** La fuente no tiene un carácter del texto. */
    GLYPH_MISSING
}
