package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Una caja del árbol de cajas (referencia/comun/estructura.md): cada elemento de caja del documento
 * ({@code Block}, {@code Flex}, {@code Text}…) es una; la raíz, anónima, es el área útil. Un
 * {@code Text} guarda su texto directamente.
 *
 * <p>No guarda geometría: el resultado del layout, por páginas, son los
 * {@link dev.stxt.pdf.fragment.Fragment}.</p>
 *
 * <p>La igualdad es por identidad: la caché de layout usa la caja como clave.</p>
 *
 * <p><strong>Escritura:</strong> los campos son privados y solo el package {@code tree} los escribe, con los
 * métodos {@code set…} y {@link #addChild}, mientras {@link BoxTreeBuilder} construye el árbol. Al terminar,
 * {@link #close()} lo cierra entero: los hijos pasan a una lista inmutable, los que están en flujo se calculan
 * una vez y cualquier escritura posterior es un {@link IllegalStateException}. Fuera de {@code tree} una caja
 * solo se lee.</p>
 *
 * <p><strong>Hilos:</strong> un árbol cerrado no cambia. Se puede leer desde varios hilos si se entrega de forma
 * segura (un {@code Future}, un ejecutor, un campo {@code volatile}…).</p>
 */
public final class Box {

    private final String id;
    private final Element element;
    private final Style style;
    private final boolean anonymous;
    private final int line;
    /** Una {@code ArrayList} mientras se construye; al cerrar, una lista inmutable. */
    private List<Box> children = new ArrayList<>();
    /** Los hijos en flujo, calculados al cerrar; {@code null} antes. */
    private List<Box> inFlowChildren;
    private Box parent;
    private String text;
    private List<TextRun> runs;
    private String image;
    private String anchor;
    private String number;
    private int textLine;
    private boolean textBlock;
    private Box marker;
    private int headRows;
    private int footRows;
    private boolean closed;

    Box(String id, Element element, Style style, boolean anonymous, int line) {
        this.id = id;
        this.element = element;
        this.style = style;
        this.anonymous = anonymous;
        this.line = line;
    }

    // --- Lectura ---------------------------------------------------------------------------------------

    /** El {@code Id} de la caja; {@code null} si no tiene. */
    public String id() {
        return id;
    }

    /**
     * Elemento del que procede; {@code null} en la raíz. Da el significado: reglas
     * {@code Element}, marcadores y etiquetas del PDF.
     */
    public Element element() {
        return element;
    }

    public Style style() {
        return style;
    }

    /** Si es la raíz anónima, que representa el área útil. */
    public boolean anonymous() {
        return anonymous;
    }

    /** Línea del documento de la que procede, para mensajes de error. */
    public int line() {
        return line;
    }

    /**
     * Los hijos, en orden del documento; la lista no se puede cambiar. En la raíz del documento, también las
     * secciones, que cuelgan de ella sin ser sus hijas (referencia/componentes/document.md).
     */
    public List<Box> children() {
        return closed ? children : Collections.unmodifiableList(children);
    }

    /** La caja padre; {@code null} en la raíz y en la caja de una sección, que es la raíz de su layout. */
    public Box parent() {
        return parent;
    }

    /** Contenido en línea, si la caja contiene texto directamente; {@code null} si no. */
    public String text() {
        return text;
    }

    /**
     * Tramos del texto con su formato (referencia/comun/formato-del-texto.md), cuyo texto seguido es
     * {@link #text()}; {@code null} si el texto no tiene formato. Ver {@link #runs()}.
     */
    public List<TextRun> formattedRuns() {
        return runs;
    }

    /**
     * Ruta de la imagen de una {@code Image} (referencia/componentes/image.md), relativa a la carpeta
     * {@code images/} de los recursos; {@code null} en el resto de cajas.
     */
    public String image() {
        return image;
    }

    /**
     * Nombre con que se refiere a la caja un índice (referencia/componentes/contents.md) cuando no tiene {@code Id}: el
     * de un encabezado que lista un {@code Contents}. {@code null} en el resto.
     */
    public String anchor() {
        return anchor;
    }

    /**
     * Número de la caja en el documento, tal como lo da {@code {number of: id}}
     * (referencia/comun/referencias.md); {@code null} si no tiene.
     */
    public String number() {
        return number;
    }

    /** Línea del nodo {@code Text}. */
    public int textLine() {
        return textLine;
    }

    /** El texto es un bloque ({@code Text >>}): su contenido empieza en la línea siguiente. */
    public boolean textBlock() {
        return textBlock;
    }

    /**
     * Caja del marcador de un {@code List item} (referencia/componentes/list.md), generada por el motor:
     * una caja de texto ({@link Element#MARKER}) con la tipografía del ítem y el texto del
     * marcador más el espacio que lo separa del contenido ({@code "3. "}, {@code "• "}). No está
     * entre los hijos: el layout del ítem la cuelga a su izquierda. {@code null} si la caja no
     * lleva marcador.
     */
    public Box marker() {
        return marker;
    }

    /**
     * Filas de la rejilla de una {@code Table} que son de su {@code Head} (referencia/componentes/table.md): las
     * primeras. 0 en el resto de cajas.
     */
    public int headRows() {
        return headRows;
    }

    /** Filas de la rejilla de una {@code Table} que son de su {@code Foot}: las últimas. 0 en el resto de cajas. */
    public int footRows() {
        return footRows;
    }

    // --- Escritura, solo en tree mientras se construye -------------------------------------------------

    void addChild(Box child) {
        writable();
        child.parent = this;
        children.add(child);
    }

    /**
     * Cuelga la caja de una sección de la raíz del documento sin ser su hija: no le pone padre
     * (referencia/componentes/document.md).
     */
    void addSection(Box section) {
        writable();
        children.add(section);
    }

    void setText(String text, int line, boolean block) {
        writable();
        this.text = text;
        this.textLine = line;
        this.textBlock = block;
    }

    /** Pone los tramos con formato del texto; {@code null} si no tiene formato. */
    void setRuns(List<TextRun> runs) {
        writable();
        this.runs = runs;
    }

    /** Escribe {@code prefix} delante del texto; si el texto tiene formato, como un tramo sin formato. */
    void prependText(String prefix) {
        writable();
        text = prefix + text;
        if (runs != null) {
            List<TextRun> prefixed = new ArrayList<>(runs.size() + 1);
            prefixed.add(new TextRun(prefix, TextFormat.PLAIN));
            prefixed.addAll(runs);
            runs = List.copyOf(prefixed);
        }
    }

    void setImage(String image) {
        writable();
        this.image = image;
    }

    void setAnchor(String anchor) {
        writable();
        this.anchor = anchor;
    }

    void setNumber(String number) {
        writable();
        this.number = number;
    }

    /** Pone el marcador del ítem, que tiene la caja por padre sin ser su hijo. */
    void setMarker(Box marker) {
        writable();
        marker.parent = this;
        this.marker = marker;
    }

    void setHeadRows(int rows) {
        writable();
        this.headRows = rows;
    }

    void setFootRows(int rows) {
        writable();
        this.footRows = rows;
    }

    /**
     * Cierra la caja, su marcador y todo su subárbol: los hijos pasan a una lista inmutable, los que están en
     * flujo se calculan una vez y ya no se puede escribir en ellos.
     */
    void close() {
        if (closed) {
            return;
        }
        children = List.copyOf(children);
        inFlowChildren = children.stream().filter(h -> !h.style.isOutOfFlow()).toList();
        closed = true;
        style.close();
        children.forEach(Box::close);
        if (marker != null) {
            marker.close();
        }
    }

    private void writable() {
        if (closed) {
            throw new IllegalStateException("La caja ya está cerrada: el árbol no cambia después de construirlo ("
                    + describe() + ")");
        }
    }

    // --- Consultas -------------------------------------------------------------------------------------

    /**
     * Cómo la nombra una referencia (referencia/comun/referencias.md): su {@code Id} o, si no tiene, su {@link
     * #anchor}; puede ser {@code null}.
     */
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

    /** Hijos que participan en el flujo del padre ({@code static} y {@code relative}). */
    public List<Box> inFlowChildren() {
        if (inFlowChildren != null) {
            return inFlowChildren;
        }
        return children.stream().filter(h -> !h.style.isOutOfFlow()).toList();
    }

    /**
     * Crea un contexto de formato de bloque, que aísla el colapso de márgenes
     * (referencia/componentes/block.md): el área útil (la raíz), las cajas {@code absolute} y
     * {@code fixed}, los ítems de flex y grid, las cajas con {@code Isolate: true} y las giradas en flujo.
     */
    public boolean establishesBlockFormattingContext() {
        if (parent == null || style.isolate() || style.isOutOfFlow() || style.rotatesInFlow()) {
            return true;
        }
        Layout l = parent.style.layout();
        return l == Layout.FLEX || l == Layout.GRID;
    }

    /**
     * Containing block de una caja fuera de flujo (referencia/comun/caja.md): para {@code absolute},
     * el ancestro más cercano con {@code Position} distinta de {@code static}; si no hay ninguno,
     * o si la caja es {@code fixed}, la raíz, que representa el área útil.
     */
    public Box outOfFlowContainingBlock() {
        Box b = parent;
        boolean fixed = style.position() == Position.FIXED;
        // Una caja girada en flujo también lo es, como las posicionadas (referencia/comun/capas-y-giro.md).
        while (b.parent != null && (fixed || !(b.style.isPositioned() || b.style.rotatesInFlow()))) {
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

    /** Lo que significa la caja ({@link Element#role()}); la raíz anónima del documento es {@link Element.Role#DOCUMENT}. */
    public Element.Role role() {
        return element == null ? Element.Role.DOCUMENT : element.role();
    }

    /** Si es una {@code Image}. */
    public boolean isImage() {
        return element == Element.IMAGE;
    }

    /** Si es la caja generada del marcador de un {@code List item}. */
    public boolean isMarker() {
        return element == Element.MARKER;
    }

    /** Tramos del texto: los de {@link #formattedRuns()} o, sin formato, un único tramo con todo el texto. */
    public List<TextRun> runs() {
        return runs != null ? runs : TextRun.plain(text);
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    /**
     * La caja en un mensaje, la única forma de nombrarla: {@code main (línea 12)} si tiene {@code Id},
     * {@code de la línea 12} si no y {@code (área útil)} la raíz anónima.
     */
    public String describe() {
        if (anonymous) {
            return "(área útil)";
        }
        return id != null ? id + " (línea " + line + ")" : "de la línea " + line;
    }

    @Override
    public String toString() {
        return "Box[" + describe() + ", " + style.layout() + "]";
    }
}
