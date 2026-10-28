package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.TextRun;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Una caja del árbol de cajas (ESPECIFICACION.md §5.1): cada elemento de caja del documento
 * ({@code Block}, {@code Flex}, {@code Text}…) es una; la raíz, anónima, es el área útil. Un
 * {@code Text} guarda su texto directamente.
 *
 * <p>No guarda geometría: el resultado del layout, por páginas, son los
 * {@link dev.stxt.boxes.fragment.Fragment}.</p>
 *
 * <p>La igualdad es por identidad: la caché de layout usa la caja como clave.</p>
 *
 * <p><strong>Hilos:</strong> la caja es mutable (campos públicos que no son {@code final}) mientras
 * {@link BoxTreeBuilder} construye el árbol, y después solo se lee: el layout, el pintado y el volcado no
 * escriben en ella. Un árbol ya construido se puede leer desde varios hilos si se entrega de forma segura
 * (un {@code Future}, un ejecutor, un campo {@code volatile}…); escribir en él después de entregarlo rompe esa
 * garantía.</p>
 */
public final class Box {

    public final String id;
    /**
     * Elemento del que procede; {@code null} en la raíz. Da el significado: reglas
     * {@code Element}, marcadores y etiquetas del PDF.
     */
    public final Element element;
    public final Style style;
    public final boolean anonymous;
    /** Línea del documento de la que procede, para mensajes de error. */
    public final int line;
    public final List<Box> children = new ArrayList<>();
    public Box parent;
    /** Contenido en línea, si la caja contiene texto directamente. */
    public String text;
    /**
     * Tramos del texto con su formato (ESPECIFICACION.md §5.4), cuyo texto seguido es
     * {@link #text}; {@code null} si el texto no tiene formato. Ver {@link #runs()}.
     */
    public List<TextRun> runs;
    /**
     * Ruta de la imagen de una {@code Image} (ESPECIFICACION.md §5.18), relativa a la carpeta
     * {@code images/} de los recursos; {@code null} en el resto de cajas.
     */
    public String image;
    /**
     * Nombre con que se refiere a la caja un índice (§5.19) cuando no tiene {@code Id}: el de un
     * encabezado que lista un {@code Contents}. {@code null} en el resto.
     */
    public String anchor;
    /**
     * Número de la caja en el documento, tal como lo da {@code {number of: id}}
     * (ESPECIFICACION.md §5.19); {@code null} si no tiene.
     */
    public String number;
    /** Línea del nodo {@code Text}. */
    public int textLine;
    /** El texto es un bloque ({@code Text >>}): su contenido empieza en la línea siguiente. */
    public boolean textBlock;
    /**
     * Caja del marcador de un {@code List item} (ESPECIFICACION.md §5.14), generada por el motor:
     * una caja de texto ({@link Element#MARKER}) con la tipografía del ítem y el texto del
     * marcador más el espacio que lo separa del contenido ({@code "3. "}, {@code "• "}). No está
     * entre los hijos: el layout del ítem la cuelga a su izquierda. {@code null} si la caja no
     * lleva marcador.
     */
    public Box marker;
    /**
     * Filas de la rejilla de una {@code Table} que son de su {@code Head} y de su {@code Foot}
     * (ESPECIFICACION.md §5.15): las primeras y las últimas. 0 en el resto de cajas.
     */
    public int headRows;
    public int footRows;

    public Box(String id, Element element, Style style, boolean anonymous, int line) {
        this.id = id;
        this.element = element;
        this.style = style;
        this.anonymous = anonymous;
        this.line = line;
    }

    /** Cómo la nombra una referencia (§5.19): su {@code Id} o, si no tiene, su {@link #anchor}; puede ser {@code null}. */
    public String key() {
        return id != null ? id : anchor;
    }

    /** Nivel de un encabezado ({@code Heading 1} … {@code Heading 6}); 0 si no lo es. */
    public int headingLevel() {
        return element == null ? 0 : element.headingLevel();
    }

    /** Texto de un encabezado para el índice: el de sus {@code Text}, unido y con los espacios normalizados. */
    public String headingText() {
        return children.stream().filter(Box::hasText).map(b -> b.text.strip())
                .collect(Collectors.joining(" ")).replaceAll("\\s+", " ");
    }

    public void addChild(Box child) {
        child.parent = this;
        children.add(child);
    }

    /** Hijos que participan en el flujo del padre ({@code static} y {@code relative}). */
    public List<Box> inFlowChildren() {
        return children.stream().filter(h -> !h.style.isOutOfFlow()).toList();
    }

    /**
     * Crea un contexto de formato de bloque, que aísla el colapso de márgenes
     * (ESPECIFICACION.md §5.3): el área útil (la raíz), las cajas {@code absolute} y
     * {@code fixed}, los ítems de flex y grid y las cajas con {@code Isolate: true}.
     */
    public boolean establishesBlockFormattingContext() {
        if (parent == null || style.isolate || style.isOutOfFlow()) {
            return true;
        }
        Style.Layout l = parent.style.layout;
        return l == Style.Layout.FLEX || l == Style.Layout.GRID;
    }

    /**
     * Containing block de una caja fuera de flujo (ESPECIFICACION.md §5.2): para {@code absolute},
     * el ancestro más cercano con {@code Position} distinta de {@code static}; si no hay ninguno,
     * o si la caja es {@code fixed}, la raíz, que representa el área útil.
     */
    public Box outOfFlowContainingBlock() {
        Box b = parent;
        boolean fixed = style.position == Style.Position.FIXED;
        while (b.parent != null && (fixed || !b.style.isPositioned())) {
            b = b.parent;
        }
        return b;
    }

    /** Línea del documento donde está el carácter {@code index} del texto. */
    public int textLineAt(int index) {
        if (!textBlock) {
            return textLine;
        }
        int line = textLine + 1;
        for (int i = 0; i < index; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /** Si es una {@code Image}. */
    public boolean isImage() {
        return element == Element.IMAGE;
    }

    /** Si es la caja generada del marcador de un {@code List item}. */
    public boolean isMarker() {
        return element == Element.MARKER;
    }

    /** Tramos del texto: los de {@link #runs} o, sin formato, un único tramo con todo el texto. */
    public List<TextRun> runs() {
        return runs != null ? runs : TextRun.plain(text);
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    /** Nombre legible para mensajes y volcados. */
    public String describe() {
        if (id != null) {
            return id;
        }
        return anonymous ? "(área útil)" : "(caja línea " + line + ")";
    }

    @Override
    public String toString() {
        return "Box[" + describe() + ", " + style.layout + "]";
    }
}
