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
 * Propiedades especificadas de una caja, con los valores por defecto de referencia/comun/estructura.md.
 *
 * <p>Las propiedades van por grupos, cada uno en su clase: los del documento ({@link TypographyStyle},
 * {@link BoxStyle} y {@link BreakStyle}, referencia/comun/estructura.md) y los de un tipo de elemento ({@link
 * FlexStyle}, {@link GridStyle}, {@link ItemStyle}, {@link ListStyle}, {@link MulticolStyle}, {@link ImageStyle} y
 * {@link ContentsStyle}). Un algoritmo lee solo los suyos: {@code style.flex().direction()}, {@code
 * style.box().margin()}. En el propio {@code Style} quedan las propiedades que definen el elemento
 * (referencia/comun/estructura.md y referencia/componentes/relative-absolute-fixed.md: el layout y la posición, que
 * salen del elemento, {@code Offset}, {@code Layer}, {@code Pages}, {@code Isolate}, el giro, {@code Link}, {@code
 * Header} e {@code In contents}) y la línea donde se declaró cada propiedad.</p>
 *
 * <p>No hay cascada: cada caja declara sus propiedades. Solo se <strong>hereda</strong> {@link TypographyStyle}:
 * cada caja empieza con una copia del de su padre ({@link #inheriting}), igual que en CSS.</p>
 *
 * <p>Los valores de las propiedades con un conjunto cerrado son enums de {@code model} ({@link TextAlign},
 * {@link Marker}…): los nombres de sus constantes coinciden con los valores del documento en mayúsculas y con
 * el guion como guion bajo ({@code lower-alpha} es {@code LOWER_ALPHA}), que es como los interpreta
 * {@link PropertyReader}. {@link Layout} y {@link Position} no son valores del documento: salen del elemento
 * ({@code Text} es {@code INLINE}; {@code Absolute}, un {@code BLOCK} {@code ABSOLUTE}).</p>
 *
 * <p>Solo {@code tree} escribe un estilo, mientras {@link BoxTreeBuilder} construye el árbol; al terminar se
 * cierra con su caja ({@link Box#close}) y ya solo se lee, también desde varios hilos.</p>
 */
public final class Style extends StyleGroup {

    private TypographyStyle typography = new TypographyStyle();
    private final BoxStyle box = new BoxStyle();
    private final BreakStyle breaks = new BreakStyle();
    private final FlexStyle flex = new FlexStyle();
    private final GridStyle grid = new GridStyle();
    private final ItemStyle item = new ItemStyle();
    private final ListStyle list = new ListStyle();
    private final MulticolStyle multicol = new MulticolStyle();
    private final ImageStyle image = new ImageStyle();
    private final ContentsStyle contents = new ContentsStyle();

    // --- Propiedades del elemento ------------------------------------------------------------------
    private Layout layout = Layout.BLOCK;
    private Position position = Position.STATIC;
    private Sides<Value> offset = Sides.all(Value.AUTO);
    /** {@code null} = auto. */
    private Integer layer;
    /** Solo en un {@code Fixed}: las páginas en que sale. */
    private Pages pages = Pages.ALL;
    private boolean isolate;
    /**
     * {@code Rotate} y {@code Rotate origin} (referencia/comun/capas-y-giro.md), solo en {@code Relative}, {@code
     * Absolute}, {@code Fixed} e {@code Image}; {@code null} = sin giro. No se hereda.
     */
    private Rotation rotation;
    /**
     * Destino del {@code Link} de la caja ({@link dev.stxt.pdf.model.Links}, referencia/comun/enlaces.md); {@code
     * null} = sin enlace. No se hereda: lo que hay dentro de una caja con enlace es clicable por ella, pero no lleva
     * enlace propio.
     */
    private String link;
    /**
     * {@code Header} de una {@code Cell}: es la cabecera de su fila (referencia/componentes/table.md). No se hereda.
     */
    private boolean header;
    /**
     * {@code In contents} de un encabezado: si tiene entrada en los índices (referencia/componentes/contents.md). No se
     * hereda.
     */
    private boolean inContents = true;

    /** Solo lo crea {@code tree}: el estilo de cada caja sale de {@link BoxTreeBuilder}. */
    Style() {
    }

    // --- Grupos ------------------------------------------------------------------------------------
    /** El grupo {@code Typography}: se hereda (referencia/comun/tipografia.md). */
    public TypographyStyle typography() {
        return typography;
    }

    /** El grupo {@code Box}: el modelo de caja (referencia/comun/caja.md). */
    public BoxStyle box() {
        return box;
    }

    /** El grupo {@code Break}: los cortes (referencia/comun/cortes.md). */
    public BreakStyle breaks() {
        return breaks;
    }

    /** Las propiedades de {@code Flex} (referencia/componentes/flex.md). */
    public FlexStyle flex() {
        return flex;
    }

    /** Las propiedades de {@code Grid} y {@code Table} (referencia/componentes/grid.md). */
    public GridStyle grid() {
        return grid;
    }

    /**
     * Las de {@code Item} y {@code Area}, o la celda de una tabla (referencia/componentes/flex.md y
     * referencia/componentes/grid.md).
     */
    public ItemStyle item() {
        return item;
    }

    /** Las de {@code List} y {@code List item} (referencia/componentes/list.md). */
    public ListStyle list() {
        return list;
    }

    /** Las de {@code Multicol} (referencia/componentes/multicol.md). */
    public MulticolStyle multicol() {
        return multicol;
    }

    /** Las de {@code Image} (referencia/componentes/image.md). */
    public ImageStyle image() {
        return image;
    }

    /** Las de {@code Contents} (referencia/componentes/contents.md). */
    public ContentsStyle contents() {
        return contents;
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

    public boolean isolate() {
        return isolate;
    }

    void setIsolate(boolean v) {
        writable();
        isolate = v;
    }

    public Rotation rotation() {
        return rotation;
    }

    /**
     * Si la caja gira en flujo (referencia/comun/capas-y-giro.md): un {@code Block}, un {@code Text}, un {@code
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

    public boolean header() {
        return header;
    }

    void setHeader(boolean v) {
        writable();
        header = v;
    }

    public boolean inContents() {
        return inContents;
    }

    void setInContents(boolean v) {
        writable();
        inContents = v;
    }

    // --- Líneas de las propiedades -------------------------------------------------------------------
    /**
     * Línea del documento de cada propiedad declarada, por su clave ({@link PropertyKey}): el
     * nombre canónico ({@code "height"}; las de {@code Flex}, {@code Grid} e {@code Item}, con
     * prefijo: {@code "grid.rows"}). Las
     * de {@link #LINED_PROPERTIES} se heredan con su valor, para señalar dónde se declaró.
     */
    private final Map<String, Integer> lines = new HashMap<>();

    /**
     * Propiedades que se heredan con su línea, para señalarla en los errores: las de la fuente
     * (las de {@code Typography} que la eligen, también la del código) y {@code Line height}, que fijan el alto de las
     * líneas (referencia/comun/desbordamiento.md).
     */
    public static final List<String> LINED_PROPERTIES = List.of(PropertyKey.FONT, PropertyKey.FONT_WEIGHT,
            PropertyKey.FONT_STYLE, PropertyKey.FONT_SIZE, PropertyKey.LINE_HEIGHT, PropertyKey.CODE_FONT);

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

    /** Apunta la línea donde se declara una propiedad. */
    void declared(String property, int line) {
        writable();
        lines.put(property, line);
    }

    /** Si la propiedad se declaró (en esta caja o, las de {@link #LINED_PROPERTIES}, en una antecesora). */
    public boolean isDeclared(String property) {
        return lines.containsKey(property);
    }

    /**
     * Línea donde se declaró la propiedad.
     *
     * @throws IllegalStateException si no se declaró
     */
    public int line(String property) {
        Integer line = lines.get(property);
        if (line == null) {
            throw new IllegalStateException("Propiedad no declarada: " + property);
        }
        return line;
    }

    public boolean isPositioned() {
        return position != Position.STATIC;
    }

    public boolean isOutOfFlow() {
        return position == Position.ABSOLUTE || position == Position.FIXED;
    }

    /** Cierra el estilo con su caja ({@link Box#close}): ya no cambia. */
    void close() {
        super.close();
        typography.close();
        box.close();
        breaks.close();
        flex.close();
        grid.close();
        item.close();
        list.close();
        multicol.close();
        image.close();
        contents.close();
    }
}
