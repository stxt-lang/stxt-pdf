package dev.stxt.boxes.tree;

import dev.stxt.boxes.tree.Style.Layout;
import dev.stxt.boxes.tree.Style.Position;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Elementos de caja del documento (ESPECIFICACION.md §2, §9) y el marcador que el motor genera
 * para cada {@code List item} (§5.14). Cada uno da su {@code Layout} y su {@code Position}, que
 * no se declaran, y el significado que usan las reglas {@code Element}, los marcadores y las
 * etiquetas del PDF.
 *
 * <p>La estructura (qué hijos admite cada elemento) está solo en el schema; {@code ElementTest}
 * comprueba que este enum tiene los mismos elementos que él.</p>
 */
public enum Element {

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
    /** Una tabla se maqueta como un grid con colocación explícita (§5.15). */
    TABLE("Table", Layout.GRID, Position.STATIC, true, false, 0),
    /** Sin valor, un bloque; con valor, una caja de texto ({@link BoxTreeBuilder} cambia el {@code Layout}). */
    CELL("Cell", Layout.BLOCK, Position.STATIC, false, false, 0),
    RELATIVE("Relative", Layout.BLOCK, Position.RELATIVE, true, false, 0),
    ABSOLUTE("Absolute", Layout.BLOCK, Position.ABSOLUTE, false, false, 0),
    FIXED("Fixed", Layout.BLOCK, Position.FIXED, false, false, 0),
    /** La caja generada del marcador de un {@code List item}: no se escribe en el documento. */
    MARKER(null, Layout.INLINE, Position.STATIC, false, false, 0);

    /** Los elementos que se escriben en el documento, por su nombre canónico ({@code list-item}). */
    private static final Map<String, Element> BY_NAME = Arrays.stream(values())
            .filter(e -> e.label != null)
            .collect(Collectors.toUnmodifiableMap(Element::canonicalName, Function.identity()));

    private final String label;
    private final Layout layout;
    private final Position position;
    private final boolean item;
    private final boolean text;
    private final int headingLevel;

    Element(String label, Layout layout, Position position, boolean item, boolean text, int headingLevel) {
        this.label = label;
        this.layout = layout;
        this.position = position;
        this.item = item;
        this.text = text;
        this.headingLevel = headingLevel;
    }

    /** El elemento con ese nombre canónico ({@code heading-2}); vacío si no es un elemento de caja. */
    public static Optional<Element> of(String canonicalName) {
        return Optional.ofNullable(BY_NAME.get(canonicalName));
    }

    /** Los elementos que se escriben en el documento, en el orden de la especificación. */
    public static List<Element> written() {
        return Arrays.stream(values()).filter(e -> e.label != null).toList();
    }

    /** El nombre como se escribe ({@code List item}); {@code null} en el marcador. */
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

    /** Si lleva el texto en el valor o en {@code Content} ({@code Text} y {@code Paragraph}). */
    public boolean hasText() {
        return text;
    }

    /** Nivel del encabezado ({@code Heading 2} → 2); 0 si no es un encabezado. */
    public int headingLevel() {
        return headingLevel;
    }
}
