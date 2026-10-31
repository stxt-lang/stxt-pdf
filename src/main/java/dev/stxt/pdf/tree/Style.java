package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.Pages;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Propiedades especificadas de una caja, con los valores por defecto de reference/common/structure.md.
 *
 * <p>Las propiedades van por grupos, cada uno en su clase: los del documento ({@link TypographyStyle},
 * {@link BoxStyle} y {@link BreakStyle}, reference/common/structure.md) y los de un tipo de elemento ({@link
 * StackStyle}, {@link GridStyle}, {@link ItemStyle}, {@link ListStyle}, {@link ColumnsStyle}, {@link ImageStyle} y
 * {@link TableOfContentsStyle}). Un algoritmo lee solo los suyos: {@code style.stack().direction()}, {@code
 * style.box().margin()}. En el propio {@code Style} quedan las propiedades que definen el elemento
 * (reference/common/structure.md y reference/components/frame-and-overlays.md: el layout y la posición, que
 * salen del elemento, {@code Offset}, {@code Layer}, {@code Pages}, {@code Isolate margins}, el giro, {@code Link},
 * {@code Row header} e {@code In table of contents}) y la línea donde se declaró cada propiedad, con su documento
 * ({@link SourceLine}).</p>
 *
 * <p>No hay cascada: cada caja declara sus propiedades. Solo se <strong>hereda</strong> {@link TypographyStyle}:
 * cada caja empieza con una copia del de su padre ({@link #inheriting}), igual que en CSS.</p>
 *
 * <p>Los valores de las propiedades con un conjunto cerrado son enums de {@code model} ({@link TextAlign},
 * {@link Marker}…): los nombres de sus constantes coinciden con los valores del documento en mayúsculas y con
 * el guion como guion bajo ({@code lower-alpha} es {@code LOWER_ALPHA}), que es como los interpreta
 * {@link PropertyReader}. {@link Layout} y {@link Position} no son valores del documento: salen del elemento
 * ({@code Text} es {@code INLINE}; {@code Overlay}, un {@code BLOCK} {@code OVERLAY}).</p>
 *
 * <p>Solo {@code tree} escribe un estilo, mientras {@link BoxTreeBuilder} construye el árbol; al terminar se
 * cierra con su caja ({@link Box#close}) y ya solo se lee, también desde varios hilos.</p>
 */
public final class Style extends StyleGroup {

    private TypographyStyle typography = new TypographyStyle();
    private final BoxStyle box = new BoxStyle();
    private final BreakStyle breaks = new BreakStyle();
    private final StackStyle stack = new StackStyle();
    private final GridStyle grid = new GridStyle();
    private final ItemStyle item = new ItemStyle();
    private final ListStyle list = new ListStyle();
    private final ColumnsStyle columns = new ColumnsStyle();
    private final ImageStyle image = new ImageStyle();
    private final TableOfContentsStyle tableOfContents = new TableOfContentsStyle();

    // --- Propiedades del elemento ------------------------------------------------------------------
    private Layout layout = Layout.BLOCK;
    private Position position = Position.STATIC;
    private Sides<Value> offset = Sides.all(Value.AUTO);
    /** {@code null} = auto. */
    private Integer layer;
    /** Solo en un {@code Page overlay}: las páginas en que sale. */
    private Pages pages = Pages.ALL;
    private boolean isolateMargins;
    /**
     * {@code Rotate} y {@code Rotate origin} (reference/common/layers-and-rotation.md), solo en {@code Frame}, {@code
     * Overlay}, {@code Page overlay} e {@code Image}; {@code null} = sin giro. No se hereda.
     */
    private Rotation rotation;
    /**
     * Destino del {@code Link} de la caja ({@link dev.stxt.pdf.model.Links}, reference/common/links.md); {@code
     * null} = sin enlace. No se hereda: lo que hay dentro de una caja con enlace es clicable por ella, pero no lleva
     * enlace propio.
     */
    private String link;
    /**
     * {@code Row header} de una {@code Cell}: es la cabecera de su fila (reference/components/table.md). No se
     * hereda.
     */
    private boolean rowHeader;
    /**
     * {@code In table of contents} de un encabezado: si tiene entrada en los índices
     * (reference/components/table-of-contents.md). No se hereda.
     */
    private boolean inTableOfContents = true;

    /** Solo lo crea {@code tree}: el estilo de cada caja sale de {@link BoxTreeBuilder}. */
    Style() {
    }

    // --- Grupos ------------------------------------------------------------------------------------
    /** El grupo {@code Typography}: se hereda (reference/common/typography.md). */
    public TypographyStyle typography() {
        return typography;
    }

    /** El grupo {@code Box}: el modelo de caja (reference/common/box.md). */
    public BoxStyle box() {
        return box;
    }

    /** El grupo {@code Break}: los cortes (reference/common/breaks.md). */
    public BreakStyle breaks() {
        return breaks;
    }

    /** Las propiedades de {@code Stack} (reference/components/stack.md). */
    public StackStyle stack() {
        return stack;
    }

    /** Las propiedades de {@code Grid} y {@code Table} (reference/components/grid.md). */
    public GridStyle grid() {
        return grid;
    }

    /**
     * Las de {@code Item} y {@code Area}, o la celda de una tabla (reference/components/stack.md y
     * reference/components/grid.md).
     */
    public ItemStyle item() {
        return item;
    }

    /** Las de {@code List} y {@code List item} (reference/components/list.md). */
    public ListStyle list() {
        return list;
    }

    /** Las de {@code Columns} (reference/components/columns.md). */
    public ColumnsStyle columns() {
        return columns;
    }

    /** Las de {@code Image} (reference/components/image.md). */
    public ImageStyle image() {
        return image;
    }

    /** Las de {@code Table of contents} (reference/components/table-of-contents.md). */
    public TableOfContentsStyle tableOfContents() {
        return tableOfContents;
    }

    // --- Propiedades del elemento ------------------------------------------------------------------
    public Layout layout() {
        return layout;
    }

    void setLayout(Layout v) {
        writable();
        layout = v;
    }

    public Position position() {
        return position;
    }

    void setPosition(Position v) {
        writable();
        position = v;
    }

    public Sides<Value> offset() {
        return offset;
    }

    void setOffset(Sides<Value> v) {
        writable();
        offset = v;
    }

    public Integer layer() {
        return layer;
    }

    void setLayer(Integer v) {
        writable();
        layer = v;
    }

    public Pages pages() {
        return pages;
    }

    void setPages(Pages v) {
        writable();
        pages = v;
    }

    public boolean isolateMargins() {
        return isolateMargins;
    }

    void setIsolateMargins(boolean v) {
        writable();
        isolateMargins = v;
    }

    public Rotation rotation() {
        return rotation;
    }

    /**
     * Si la caja gira en flujo (reference/common/layers-and-rotation.md): un {@code Block}, un {@code Text}, un {@code
     * Paragraph} o una {@code Image} con {@code Rotate}, que ocupa el rectángulo que la envuelve girada.
     */
    public boolean rotatesInFlow() {
        return rotation != null && position == Position.STATIC;
    }

    void setRotation(Rotation v) {
        writable();
        rotation = v;
    }

    public String link() {
        return link;
    }

    void setLink(String v) {
        writable();
        link = v;
    }

    public boolean rowHeader() {
        return rowHeader;
    }

    void setRowHeader(boolean v) {
        writable();
        rowHeader = v;
    }

    public boolean inTableOfContents() {
        return inTableOfContents;
    }

    void setInTableOfContents(boolean v) {
        writable();
        inTableOfContents = v;
    }

    // --- Líneas de las propiedades -------------------------------------------------------------------
    /**
     * Línea de cada propiedad declarada, por su clave ({@link PropertyKey}): el nombre canónico ({@code "height"}; las
     * de {@code Stack}, {@code Grid} e {@code Item}, con prefijo: {@code "grid.row-sizes"}). Es del documento o, si la
     * propiedad llega de una regla suya, del documento de estilos aparte. Las de {@link #LINED_PROPERTIES} se heredan
     * con su valor, para señalar dónde se declaró.
     */
    private final Map<String, SourceLine> lines = new HashMap<>();
    /** Si lo que se lee ahora es una regla del documento de estilos aparte ({@link #readingStyles}). */
    private boolean readingStyles;

    /**
     * Propiedades que se heredan con su línea, para señalarla en los errores: las de la fuente
     * (las de {@code Typography} que la eligen, también la del código), {@code Line height}, que fijan el alto de las
     * líneas (reference/common/overflow.md), y {@code Color}, del contraste del texto (reference/common/pdf.md).
     */
    public static final List<String> LINED_PROPERTIES = List.of(PropertyKey.FONT, PropertyKey.FONT_WEIGHT,
            PropertyKey.FONT_STYLE, PropertyKey.FONT_SIZE, PropertyKey.LINE_HEIGHT, PropertyKey.CODE_FONT,
            PropertyKey.COLOR);

    /** Estilo nuevo con los valores por defecto y las propiedades heredables del padre. */
    static Style inheriting(Style parent) {
        Style s = new Style();
        if (parent != null) {
            s.typography = new TypographyStyle(parent.typography);
            for (String p : LINED_PROPERTIES) {
                if (parent.lines.containsKey(p)) {
                    s.lines.put(p, parent.lines.get(p));
                }
            }
        }
        return s;
    }

    /**
     * Las propiedades que se declaren mientras {@code reading} sea {@code true} vienen de una regla del documento de
     * estilos aparte, y su línea es de ese documento.
     */
    void readingStyles(boolean reading) {
        writable();
        readingStyles = reading;
    }

    /** Apunta la línea donde se declara una propiedad: del documento de estilos si se lee una regla suya. */
    void declared(String property, int line) {
        declared(property, new SourceLine(line, readingStyles));
    }

    /** Apunta la línea donde se declara una propiedad, con su documento. */
    void declared(String property, SourceLine line) {
        writable();
        lines.put(property, line);
    }

    /** Si la propiedad se declaró (en esta caja o, las de {@link #LINED_PROPERTIES}, en una antecesora). */
    public boolean isDeclared(String property) {
        return lines.containsKey(property);
    }

    /**
     * Línea donde se declaró la propiedad, con su documento.
     *
     * @throws IllegalStateException si no se declaró
     */
    public SourceLine line(String property) {
        SourceLine line = lines.get(property);
        if (line == null) {
            throw new IllegalStateException("Propiedad no declarada: " + property);
        }
        return line;
    }

    public boolean isPositioned() {
        return position != Position.STATIC;
    }

    public boolean isOutOfFlow() {
        return position == Position.OVERLAY || position == Position.PAGE_OVERLAY;
    }

    /** Cierra el estilo con su caja ({@link Box#close}): ya no cambia. */
    void close() {
        super.close();
        typography.close();
        box.close();
        breaks.close();
        stack.close();
        grid.close();
        item.close();
        list.close();
        columns.close();
        image.close();
        tableOfContents.close();
    }
}
