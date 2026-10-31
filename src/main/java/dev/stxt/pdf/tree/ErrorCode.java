package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.StxtPdfException.Kind;

/**
 * Código estable de cada error del modelo (referencia/comun/errores.md). Los tests comparan el código y
 * la línea, no el texto del mensaje, que puede mejorar sin romperlos. Los errores de sintaxis
 * STXT y del schema llevan los códigos de la librería {@code stxt-core}
 * ({@code INVALID_VALUE}, {@code CHILD_NOT_DECLARED}…), en un {@link DocumentSyntaxException}.
 * Cada código dice de quién es la culpa ({@link #kind()}).
 */
public enum ErrorCode {

    // --- Documento y medio ------------------------------------------------------------------
    /** El documento no tiene un único nodo raíz {@code Document} con el namespace del modelo. */
    DOCUMENT_ROOT,
    /** Un tamaño o un margen del medio no válido, o márgenes que no dejan área útil. */
    PAGE_SIZE,
    /**
     * Una {@code Section} sin contenido (solo {@code Page overlay}, que no ocupan sitio): no tendría ninguna página.
     */
    SECTION_EMPTY,
    /** Un texto con {@code {page}} o {@code {pages}} en una página que no tiene número ({@code Page numbering: none}). */
    PAGE_NUMBER_UNAVAILABLE,

    // --- Valores ------------------------------------------------------------------------------
    /** Sintaxis de un valor (referencia/comun/valores.md): número, porcentaje, función, lista… */
    VALUE_SYNTAX,
    /** Un valor bien escrito que la propiedad no admite ({@code Width: *}, {@code Padding: -2}…). */
    VALUE_NOT_ALLOWED,
    /** {@code fit} como pista de grid. */
    FIT_TRACK,

    // --- Estructura ---------------------------------------------------------------------------
    /** Dos cajas con el mismo {@code Id}. */
    DUPLICATE_ID,
    /** Un {@code Item} o un {@code Area} que no contiene exactamente un elemento. */
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
    /** Un encabezado dentro de un {@code Page overlay}. */
    HEADING_IN_PAGE_OVERLAY,
    /** Un {@code Text} de un encabezado con {@code Break}: el motor ya evita el corte. */
    BREAK_IN_HEADING,
    /**
     * Una línea en blanco en un {@code Paragraph}, que lo partiría en varios párrafos, o en un {@code Text} que no es
     * preformateado.
     */
    TEXT_BLANK_LINE,

    // --- Listas ----------------------------------------------------------------------------------
    /** Un {@code List item} sin ningún elemento. */
    LIST_ITEM_EMPTY,
    /** {@code Start} o {@code Number} en una lista que no numera ({@code bullet}, {@code dash}, {@code none}). */
    LIST_NUMBER_NOT_ALLOWED,
    /** Un número que la numeración no puede escribir: negativo, 0 en letras o romanos, o más de 3999 en romanos. */
    LIST_NUMBER_RANGE,
    /** Un ítem con marcador cuyo contenido no empieza con un texto con el que alinearlo. */
    LIST_ITEM_WITHOUT_LINE,

    // --- Tablas -----------------------------------------------------------------------------------
    /** {@code Header rows}, {@code Body} y {@code Footer rows} fuera de ese orden. */
    TABLE_ORDER,
    /** Una fila que no llena exactamente las columnas de la tabla. */
    TABLE_ROW_CELLS,
    /** Un {@code Span} que sale de la tabla o de su grupo, o que se solapa con otra celda. */
    TABLE_SPAN,
    /** Una {@code Cell} con texto en el valor y elementos. */
    CELL_CONTENT,
    /** {@code Break} en una fila de {@code Header rows} o de {@code Footer rows}, que se repiten en cada página. */
    TABLE_ROW_BREAK,
    /** Un {@code Column align} con distinto número de alineaciones que columnas tiene la tabla. */
    TABLE_COLUMN_ALIGN,
    /** {@code Row header} en una celda de {@code Header rows}, cuyas celdas ya son cabeceras de columna. */
    ROW_HEADER_IN_HEADER_ROWS,

    // --- Columnas ------------------------------------------------------------------------------
    /** Un {@code Columns} sin {@code Count} ni {@code Min column width}. */
    COLUMNS_WITHOUT_COUNT,
    /** {@code Separator} más ancho que {@code Gap}: la línea se pintaría sobre las columnas. */
    SEPARATOR_WIDTH,
    /** {@code Before} o {@code After: column} fuera de un {@code Columns}. */
    COLUMN_BREAK_OUTSIDE_COLUMNS,
    /** {@code Keep with next} con {@code After}, o {@code Keep with previous} con {@code Before}, en la misma caja. */
    KEEP_CONFLICT,
    /** Un {@code Overlay} o un {@code Page overlay} dentro de un {@code Columns}. */
    OVERLAY_IN_COLUMNS,

    // --- Markdown ------------------------------------------------------------------------------
    /** Algo de Markdown fuera del subconjunto admitido: enlaces, imágenes dentro de un texto, código, citas, HTML… */
    MARKDOWN_UNSUPPORTED,
    /**
     * Una lista o una tabla de un {@code Markdown} sin su {@code Bullet markers}, {@code Number markers} o {@code Table
     * columns}.
     */
    MARKDOWN_PROPERTY_MISSING,
    /** Código, en línea o en un bloque, sin {@code Code font} en {@code Typography}, propia o heredada. */
    CODE_WITHOUT_FONT,

    // --- Imágenes -------------------------------------------------------------------------------
    /** Una {@code Image} sin ruta, con una ruta que sale de la carpeta o que no existe. */
    IMAGE_UNKNOWN,
    /**
     * Una {@code Image} que no es PNG, JPEG, PDF ni un SVG admitido, que no se puede leer o cuya página de un PDF no se
     * puede dibujar.
     */
    IMAGE_FORMAT,
    /**
     * {@code Pdf page} en una {@code Image} que no es un PDF, o una página que el PDF no tiene (línea de {@code Pdf
     * page}).
     */
    IMAGE_PAGE,
    /** {@code Keep together} o {@code Edges} en una {@code Image}, que no se parte. */
    BREAK_IN_IMAGE,

    // --- Referencias --------------------------------------------------------------------------
    /** Una referencia ({@code {page of: id}}) sin {@code Id}, o con un {@code Id} que no tiene ninguna caja. */
    REFERENCE_UNKNOWN,
    /**
     * Una referencia a la página de una caja que no tiene una única página: la de un {@code Page overlay} o una de sus
     * cajas.
     */
    REFERENCE_NOT_PLACEABLE,
    /** Una referencia a una caja que no tiene número: ni es un ítem de una lista numerada ni un encabezado. */
    REFERENCE_NOT_NUMBERED,
    /** Las páginas a las que se refiere el texto no se estabilizan: cada pasada de paginación las cambia. */
    REFERENCE_UNSTABLE,

    // --- Enlaces ------------------------------------------------------------------------------
    /** Un destino de enlace vacío, mal escrito o con un esquema que no es https, http, mailto ni {@code #id}. */
    LINK_SCHEME,
    /**
     * Un enlace {@code #id} a un {@code Id} que no existe o que está en un {@code Page overlay}, que se repite en todas
     * las páginas.
     */
    LINK_UNKNOWN,

    // --- Índice -------------------------------------------------------------------------------
    /** Un {@code Table of contents} sin ningún encabezado en los niveles de {@code Levels}. */
    TABLE_OF_CONTENTS_EMPTY,

    // --- Reglas reutilizables (Styles) ---------------------------------------------------------
    /** {@code For element} con un nombre que no es un elemento de caja. */
    STYLE_UNKNOWN_ELEMENT,
    /** Dos reglas con el mismo selector. */
    STYLE_DUPLICATE,
    /**
     * Una regla aplica un grupo que el elemento no admite: {@code Break} en {@code Overlay},
     * {@code Page overlay}, un encabezado, un {@code Text} de un encabezado o una {@code Cell}.
     */
    STYLE_NOT_ALLOWED,
    /** Una clase de {@code Style} sin regla {@code For style}. */
    STYLE_WITHOUT_RULE,

    // --- Combinaciones sin efecto o contradictorias --------------------------------------------
    /** Un {@code Frame} con arriba y abajo, o con izquierda y derecha. */
    OFFSET_CONFLICT,
    /** Un {@code Overlay} o un {@code Page overlay} con inicio, tamaño y fin declarados y sin márgenes {@code auto}. */
    OVERCONSTRAINED,
    /** Un margen {@code auto} donde no centra ni absorbe nada. */
    MARGIN_AUTO_WITHOUT_EFFECT,
    /** {@code Start} en una {@code Section} sin {@code Page numbering: restart}. */
    START_WITHOUT_RESTART,
    /**
     * Algo que no es el aspecto ({@code Background}, {@code Border}, {@code Border color}, {@code Corner radius}) en el
     * {@code Box} de una {@code Row}.
     */
    ROW_BOX,
    /** {@code Page number format} en una {@code Section} con {@code Page numbering: none}. */
    PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS,
    /** {@code Begin on: odd} en la primera {@code Section}, que ya empieza en la página 1. */
    BEGIN_ON_FIRST_SECTION,
    /** {@code Line distribution} sin {@code Wrap: true}. */
    LINE_DISTRIBUTION_WITHOUT_WRAP,
    /** {@code Gap} de dos valores en un {@code Stack} sin {@code Wrap: true}. */
    GAP_WITHOUT_WRAP,
    /** {@code Grow} en un {@code Stack} vertical sin alto definido. */
    GROW_WITHOUT_EFFECT,
    /** {@code Horizontal} o {@code Vertical} de un {@code Item} en el eje principal de su {@code Stack}. */
    ITEM_MAIN_AXIS,
    /** {@code Aspect ratio} con {@code Width} y {@code Height} declarados. */
    ASPECT_RATIO_CONFLICT,
    /** Mínimo mayor que el máximo, o un tamaño fuera de su mínimo y su máximo. */
    MIN_MAX_CONFLICT,
    /** Un porcentaje vertical cuya base no es un alto definido. */
    INDEFINITE_PERCENTAGE,
    /** {@code Areas} no rectangulares o un valor de {@code Area} no válido. */
    GRID_PLACEMENT,
    /** {@code Rotate origin} sin {@code Rotate}. */
    ROTATE_ORIGIN_WITHOUT_ROTATE,
    /** {@code Gap} o {@code Separator} en un {@code Columns} con {@code Count: 1}. */
    SINGLE_COLUMN,
    /** {@code Separator color} sin {@code Separator}. */
    SEPARATOR_COLOR_WITHOUT_SEPARATOR,
    /** {@code Isolate margins: true} en un ítem de {@code Stack} o {@code Grid}, que ya aísla. */
    ISOLATE_MARGINS_IN_ITEM,
    /**
     * Un {@code Break} propio dentro de un {@code Overlay}, un {@code Page overlay}, un {@code Frame} girado o una caja
     * girada en flujo.
     */
    BREAK_IN_UNBREAKABLE,
    /**
     * {@code Keep together} o {@code Edges} en un {@code Frame} girado o en una caja girada en flujo, que no se parten.
     */
    BREAK_IN_ROTATED,
    /** {@code Isolate margins: true} en una caja girada en flujo, que ya aísla. */
    ISOLATE_MARGINS_IN_ROTATED,
    /** {@code Rotate} en un {@code Block}, un {@code Text}, un {@code Paragraph} o una {@code Image} que es un ítem de {@code Stack} o {@code Grid}. */
    ROTATED_ITEM,

    // --- Desbordamientos (al maquetar) -------------------------------------------------------
    /** Una palabra más ancha que su línea. */
    TEXT_OVERFLOW,
    /**
     * Una caja que no cabe en su contenedor, contenido que pasa de un alto fijado, o un {@code Overlay} que sale de la
     * página.
     */
    BOX_OVERFLOW,
    /** Algo del flujo más alto que la página que no se puede partir. */
    PAGE_OVERFLOW,
    /** El marcador de un ítem no cabe en la sangría: el relleno izquierdo de la lista más el margen del ítem. */
    LIST_MARKER_OVERFLOW,
    /** La cabecera y el pie de una tabla, que se repiten en cada página, no dejan sitio al cuerpo en una página vacía. */
    TABLE_REPEAT_OVERFLOW,

    // --- Fuentes ------------------------------------------------------------------------------
    /** {@code Font} no está en la carpeta de fuentes. */
    FONT_UNKNOWN,
    /** La familia no tiene la variante de {@code Bold} e {@code Italic}. */
    FONT_VARIANT_MISSING,
    /** La fuente no tiene un carácter del texto. */
    GLYPH_MISSING,

    // --- Accesibilidad (al convertir) -------------------------------------------------------
    /** El texto tiene poco contraste con lo que hay debajo; se puede desactivar al convertir. */
    CONTRAST_LOW,

    // --- Límites ------------------------------------------------------------------------------
    /** El documento ocupa más páginas que {@code Limits.maxPages}; no es de ninguna línea. */
    PAGE_LIMIT(Kind.LIMIT),
    /** Una imagen PNG o JPEG con más píxeles que {@code Limits.maxImagePixels} (línea de la {@code Image}). */
    IMAGE_TOO_LARGE(Kind.LIMIT),
    /** Un SVG que dibuja más elementos o anida más niveles que {@code Limits.maxSvgElements} y {@code maxSvgDepth} (línea de la {@code Image}). */
    SVG_TOO_COMPLEX(Kind.LIMIT),
    /** Un PDF de más bytes que {@code Limits.maxPdfBytes} (línea de la {@code Image}). */
    PDF_TOO_LARGE(Kind.LIMIT);

    private final Kind kind;

    ErrorCode() {
        this(Kind.INVALID_DOCUMENT);
    }

    ErrorCode(Kind kind) {
        this.kind = kind;
    }

    /** De quién es la culpa: del documento ({@link Kind#INVALID_DOCUMENT}) salvo los límites ({@link Kind#LIMIT}). */
    public Kind kind() {
        return kind;
    }
}
