package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Position;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Elementos de caja del documento (referencia/comun/estructura.md), el marcador que el motor genera para cada {@code
 * List item} (referencia/componentes/list.md) y el flujo de columnas de cada {@code Multicol}
 * (referencia/componentes/multicol.md). Cada uno da su {@code Layout} y su {@code Position}, que no se declaran, y el
 * significado que usan las reglas {@code Element}, los marcadores y las etiquetas del PDF.
 *
 * <p>La estructura (qué hijos admite cada elemento) está solo en el schema; {@code ElementTest}
 * comprueba que este enum tiene los mismos elementos que él.</p>
 */
public enum Element {

    /**
     * Una sección del documento (referencia/componentes/document.md): un bloque que es la raíz de su propio layout, con
     * su {@code Media}. Solo es hijo de la raíz del documento.
     */
    SECTION("Section", Layout.BLOCK, Position.STATIC, false, false, 0),
    BLOCK("Block", Layout.BLOCK, Position.STATIC, true, false, 0),
    FLEX("Flex", Layout.FLEX, Position.STATIC, true, false, 0),
    GRID("Grid", Layout.GRID, Position.STATIC, true, false, 0),
    TEXT("Text", Layout.INLINE, Position.STATIC, true, true, 0),
    PARAGRAPH("Paragraph", Layout.INLINE, Position.STATIC, true, true, 0),
    HEADING_1("Heading 1", Layout.BLOCK, Position.STATIC, true, false, 1),
    HEADING_2("Heading 2", Layout.BLOCK, Position.STATIC, true, false, 2),
    HEADING_3("Heading 3", Layout.BLOCK, Position.STATIC, true, false, 3),
    HEADING_4("Heading 4", Layout.BLOCK, Position.STATIC, true, false, 4),
    HEADING_5("Heading 5", Layout.BLOCK, Position.STATIC, true, false, 5),
    HEADING_6("Heading 6", Layout.BLOCK, Position.STATIC, true, false, 6),
    LIST("List", Layout.BLOCK, Position.STATIC, true, false, 0),
    LIST_ITEM("List item", Layout.BLOCK, Position.STATIC, false, false, 0),
    /** Una tabla se maqueta como un grid con colocación explícita (referencia/componentes/table.md). */
    TABLE("Table", Layout.GRID, Position.STATIC, true, false, 0),
    /** Sin valor, un bloque; con valor, una caja de texto ({@link BoxTreeBuilder} cambia el {@code Layout}). */
    CELL("Cell", Layout.BLOCK, Position.STATIC, false, false, 0),
    /**
     * Bloque en columnas (referencia/componentes/multicol.md): su contenido va en el flujo de columnas, {@link
     * #COLUMNS}.
     */
    MULTICOL("Multicol", Layout.MULTICOL, Position.STATIC, true, false, 0),
    /**
     * Contenido en Markdown (referencia/componentes/markdown.md): un bloque cuyos hijos son los elementos que
     * {@link MarkdownBuilder} genera a partir de su {@code Content}.
     */
    MARKDOWN("Markdown", Layout.BLOCK, Position.STATIC, true, false, 0),
    /**
     * Imagen de la carpeta de recursos (referencia/componentes/image.md): una caja sin hijos con la ruta en {@link
     * Box#image()}.
     */
    IMAGE("Image", Layout.IMAGE, Position.STATIC, true, false, 0),
    /**
     * Índice automático (referencia/comun/referencias.md): un bloque cuyos hijos, generados al construir el árbol, son
     * una entrada por encabezado ({@link #CONTENTS_ENTRY_1} … {@link #CONTENTS_ENTRY_6}).
     */
    CONTENTS("Contents", Layout.BLOCK, Position.STATIC, true, false, 0),
    RELATIVE("Relative", Layout.BLOCK, Position.RELATIVE, true, false, 0),
    ABSOLUTE("Absolute", Layout.BLOCK, Position.ABSOLUTE, false, false, 0),
    FIXED("Fixed", Layout.BLOCK, Position.FIXED, false, false, 0),
    /** La caja generada del marcador de un {@code List item}: no se escribe en el documento. */
    MARKER(null, Layout.INLINE, Position.STATIC, false, false, 0),
    /**
     * El flujo de columnas de un {@code Multicol}, generado por el motor: un bloque con los hijos del {@code Multicol},
     * que es su único hijo. Cada columna es un fragmento suyo (referencia/componentes/multicol.md).
     */
    COLUMNS(null, Layout.BLOCK, Position.STATIC, false, false, 0),
    /**
     * Un bloque de código de un {@link #MARKDOWN} (referencia/componentes/markdown.md): un texto preformateado
     * generado. No se escribe en el documento, pero las reglas {@code Element} lo nombran ({@code Code block}) para
     * darle estilo aparte de los {@code Text} (referencia/comun/estilos.md).
     */
    CODE_BLOCK("Code block", Layout.INLINE, true),
    /**
     * Las entradas de un {@link #CONTENTS}, una por nivel de encabezado: un {@code Flex} generado con el título, la
     * guía y el número de página. No se escriben en el documento, pero las reglas {@code Element} las nombran ({@code
     * Contents entry 2}) para darles estilo (referencia/componentes/contents.md).
     */
    CONTENTS_ENTRY_1("Contents entry 1", Layout.FLEX),
    CONTENTS_ENTRY_2("Contents entry 2", Layout.FLEX),
    CONTENTS_ENTRY_3("Contents entry 3", Layout.FLEX),
    CONTENTS_ENTRY_4("Contents entry 4", Layout.FLEX),
    CONTENTS_ENTRY_5("Contents entry 5", Layout.FLEX),
    CONTENTS_ENTRY_6("Contents entry 6", Layout.FLEX),
    /** El título de una entrada del índice: el texto del encabezado. */
    CONTENTS_TITLE(null, Layout.INLINE, Position.STATIC, false, false, 0),
    /** La guía (puntos o línea) entre el título y el número de una entrada: una caja vacía que se pinta. */
    CONTENTS_LEADER(null, Layout.BLOCK, Position.STATIC, false, false, 0),
    /** El número de página de una entrada del índice: {@code {page of: …}} del encabezado. */
    CONTENTS_PAGE(null, Layout.INLINE, Position.STATIC, false, false, 0);

    /** Los elementos que se nombran, por su nombre canónico ({@code list-item}): los escritos y las entradas del índice. */
    private static final Map<String, Element> BY_NAME = Arrays.stream(values())
            .filter(e -> e.label != null)
            .collect(Collectors.toUnmodifiableMap(Element::canonicalName, Function.identity()));

    private final String label;
    private final Layout layout;
    private final Position position;
    private final boolean item;
    private final boolean text;
    private final int headingLevel;
    /** Generado por el motor, pero con nombre para las reglas {@code Element}. */
    private final boolean selectorOnly;

    Element(String label, Layout layout, Position position, boolean item, boolean text, int headingLevel) {
        this(label, layout, position, item, text, headingLevel, false);
    }

    /** Un elemento generado que las reglas {@code Element} pueden nombrar. */
    Element(String label, Layout layout) {
        this(label, layout, false);
    }

    /** Un elemento generado que las reglas {@code Element} pueden nombrar; {@code text}: lleva texto. */
    Element(String label, Layout layout, boolean text) {
        this(label, layout, Position.STATIC, false, text, 0, true);
    }

    private Element(String label, Layout layout, Position position, boolean item, boolean text, int headingLevel,
                    boolean selectorOnly) {
        this.label = label;
        this.layout = layout;
        this.position = position;
        this.item = item;
        this.text = text;
        this.headingLevel = headingLevel;
        this.selectorOnly = selectorOnly;
    }

    /** El elemento con ese nombre canónico ({@code heading-2}); vacío si no es un elemento de caja. */
    public static Optional<Element> of(String canonicalName) {
        return Optional.ofNullable(BY_NAME.get(canonicalName));
    }

    /** Los elementos que se escriben en el documento, en el orden de la referencia. */
    public static List<Element> written() {
        return Arrays.stream(values()).filter(Element::isWritten).toList();
    }

    /** Los elementos que una regla {@code Element} puede nombrar: los escritos, los bloques de código y las entradas del índice. */
    public static List<Element> selectable() {
        return Arrays.stream(values()).filter(e -> e.label != null).toList();
    }

    /** Si se escribe en el documento (no es generado). */
    public boolean isWritten() {
        return label != null && !selectorOnly;
    }

    /** La entrada de un {@link #CONTENTS} para los encabezados de ese nivel (1 a 6). */
    public static Element contentsEntry(int level) {
        return values()[CONTENTS_ENTRY_1.ordinal() + level - 1];
    }

    /** El nombre como se escribe ({@code List item}); {@code null} en las cajas generadas. */
    public String label() {
        return label;
    }

    /** El nombre en forma canónica, como lo da {@code Node.getCanonicalName()} ({@code list-item}). */
    public String canonicalName() {
        return label == null ? null : label.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    public Layout layout() {
        return layout;
    }

    public Position position() {
        return position;
    }

    /** Si puede ser el elemento de un {@code Item} o un {@code Area}: está en flujo y no es parte de otro. */
    public boolean canBeItem() {
        return item;
    }

    /** Si lleva el texto en el valor o en {@code Content} ({@code Text}, {@code Paragraph} y {@link #CODE_BLOCK}). */
    public boolean hasText() {
        return text;
    }

    /** Nivel del encabezado ({@code Heading 2} → 2); 0 si no es un encabezado. */
    public int headingLevel() {
        return headingLevel;
    }

    /**
     * Lo que significa una caja para quien lee el documento: el papel con que la etiqueta el PDF
     * (referencia/comun/pdf.md) y con que la tratan el pintado y la comprobación de desbordamientos. Varios elementos
     * comparten uno ({@code Paragraph} y el bloque de código son párrafos; {@code Block}, {@code Flex} o {@code Fixed},
     * un bloque sin más significado), así que un elemento nuevo solo tiene que decir el suyo ({@link #role()}).
     */
    public enum Role {
        /** La raíz anónima del documento, que no tiene elemento. */
        DOCUMENT,
        SECTION,
        HEADING,
        /** Un texto de un solo párrafo: {@code Paragraph} y el bloque de código. */
        PARAGRAPH,
        /** Un {@code Text}: un párrafo o, dentro de un encabezado, parte de su texto. */
        TEXT,
        /** Una lista, cuyos ítems llevan su marcador a la izquierda (referencia/componentes/list.md). */
        LIST,
        LIST_ITEM,
        TABLE,
        CELL,
        /** Una imagen. */
        FIGURE,
        /** Un índice ({@code Contents}, referencia/componentes/contents.md). */
        CONTENTS,
        CONTENTS_ENTRY,
        /** Un trozo de texto dentro de otro: el título de una entrada del índice. */
        SPAN,
        /** El número de página de una entrada del índice: un trozo de texto que no se lee en su enlace. */
        PAGE_NUMBER,
        /**
         * La guía de una entrada del índice: no tiene texto, se pinta (referencia/componentes/contents.md) y no es
         * contenido.
         */
        LEADER,
        /** El flujo de columnas de un {@code Multicol}: no tiene significado propio, su contenido es el del {@code Multicol}. */
        FLOW,
        /** Una caja sin más significado. */
        BLOCK
    }

    /** El papel de la caja ({@link Role}). */
    public Role role() {
        if (headingLevel > 0) {
            return Role.HEADING;
        }
        return switch (this) {
            case SECTION -> Role.SECTION;
            case PARAGRAPH, CODE_BLOCK -> Role.PARAGRAPH;
            case TEXT -> Role.TEXT;
            case LIST -> Role.LIST;
            case LIST_ITEM -> Role.LIST_ITEM;
            case TABLE -> Role.TABLE;
            case CELL -> Role.CELL;
            case IMAGE -> Role.FIGURE;
            case CONTENTS -> Role.CONTENTS;
            case CONTENTS_ENTRY_1, CONTENTS_ENTRY_2, CONTENTS_ENTRY_3, CONTENTS_ENTRY_4, CONTENTS_ENTRY_5,
                 CONTENTS_ENTRY_6 -> Role.CONTENTS_ENTRY;
            case CONTENTS_TITLE -> Role.SPAN;
            case CONTENTS_PAGE -> Role.PAGE_NUMBER;
            case CONTENTS_LEADER -> Role.LEADER;
            case COLUMNS -> Role.FLOW;
            default -> Role.BLOCK;
        };
    }
}
