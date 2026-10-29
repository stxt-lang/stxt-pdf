package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Alignment;
import dev.stxt.boxes.model.Break;
import dev.stxt.boxes.model.Cell;
import dev.stxt.boxes.model.ColumnFill;
import dev.stxt.boxes.model.Corners;
import dev.stxt.boxes.model.Decoration;
import dev.stxt.boxes.model.Direction;
import dev.stxt.boxes.model.Distribution;
import dev.stxt.boxes.model.Fit;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.model.GridAreas;
import dev.stxt.boxes.model.Inline;
import dev.stxt.boxes.model.Layout;
import dev.stxt.boxes.model.Leader;
import dev.stxt.boxes.model.Marker;
import dev.stxt.boxes.model.Pages;
import dev.stxt.boxes.model.Paint;
import dev.stxt.boxes.model.Position;
import dev.stxt.boxes.model.Rotation;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.TextAlign;
import dev.stxt.boxes.model.TextWrap;
import dev.stxt.boxes.model.Transform;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.model.WhiteSpace;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Propiedades especificadas de una caja, con los valores por defecto de ESPECIFICACION.md §4.
 *
 * <p>No hay cascada: cada caja declara sus propiedades. Solo se <strong>heredan</strong> las
 * marcadas como heredables (texto y fragmentación), igual que en CSS; ver {@link #inheriting}.</p>
 *
 * <p>Los valores de las propiedades con un conjunto cerrado son enums de {@code model} ({@link TextAlign},
 * {@link Marker}…): los nombres de sus constantes coinciden con los valores del documento en mayúsculas y con
 * el guion como guion bajo ({@code lower-alpha} es {@code LOWER_ALPHA}), que es como los interpreta
 * {@link PropertyReader}. {@link Layout} y {@link Position} no son valores del documento: salen del elemento
 * ({@code Text} es {@code INLINE}; {@code Absolute}, un {@code BLOCK} {@code ABSOLUTE}).</p>
 *
 * <p>Cada propiedad se lee con el método del mismo nombre ({@code style.fontSize()}). Los campos son del
 * package {@code tree}, el único que escribe: fuera de él un estilo solo se lee.</p>
 *
 * <p><strong>Hilos:</strong> como {@link Box}, se escribe solo mientras {@link BoxTreeBuilder} construye el
 * árbol y después se lee.</p>
 */
public final class Style {

    /** Solo lo crea {@code tree}: el estilo de cada caja sale de {@link BoxTreeBuilder}. */
    Style() {
    }

    // --- Caja --------------------------------------------------------------------------------
    Layout layout = Layout.BLOCK;
    Position position = Position.STATIC;
    Sides<Value> offset = Sides.all(Value.AUTO);
    /** {@code null} = auto. */
    Integer layer;
    /** Solo en un {@code Fixed}: las páginas en que sale. */
    Pages pages = Pages.ALL;
    boolean isolate;
    /**
     * {@code Rotate} y {@code Rotate origin} (§5.6), solo en {@code Relative}, {@code Absolute},
     * {@code Fixed} e {@code Image}; {@code null} = sin giro. No se hereda.
     */
    Rotation rotation;

    // --- Pintado (no afecta al layout) ---------------------------------------------------------
    /** Heredable. */
    String color = "#000000";

    // --- Texto (heredables) ---------------------------------------------------------------------
    /** Familia; {@code null} = sin declarar, que es un error en una caja con texto (§6). */
    String font;
    FontWeight fontWeight = FontWeight.NORMAL;
    FontStyle fontStyle = FontStyle.NORMAL;
    /** {@code NaN} = sin declarar, que es un error en una caja con texto (§6). */
    double fontSize = Double.NaN;
    /** {@code null} = 120 % del tamaño. Un número son puntos; un porcentaje, relativo al tamaño. */
    Value lineHeight;
    TextAlign textAlign = TextAlign.LEFT;
    TextWrap textWrap = TextWrap.AUTO;
    Inline inline = Inline.NONE;
    Transform transform = Transform.NONE;
    /** Separación entre letras, en puntos; puede ser negativa. */
    double letterSpacing;
    boolean smallCaps;
    /** {@code Ligatures}: si el texto se mide y se pinta con las ligaduras de la fuente (§4.4). Se hereda. */
    boolean ligatures;
    WhiteSpace whiteSpace = WhiteSpace.NORMAL;
    /** Columnas de una tabulación en el texto preformateado ({@code Tab size}). */
    int tabSize = 4;
    /** {@code Numbered}: si los encabezados se numeran (§5.13). Se hereda: la lee el encabezado. */
    boolean numbered;
    /**
     * Idioma y configuración regional del texto: el de {@code Language} de {@code Document} (§4.1) en
     * la raíz y, si una caja declara {@code Language} en {@code Typography} (§4.4), el suyo. Se hereda.
     */
    Locale locale = Locale.ROOT;

    // --- Enlace ----------------------------------------------------------------------------------
    /**
     * Destino del {@code Link} de la caja ({@link dev.stxt.boxes.model.Links}, §5.20); {@code null}
     * = sin enlace. No se hereda: lo que hay dentro de una caja con enlace es clicable por ella,
     * pero no lleva enlace propio.
     */
    String link;
    /** {@code Header} de una {@code Cell}: es la cabecera de su fila (§5.15). No se hereda. */
    boolean header;
    /** {@code In contents} de un encabezado: si tiene entrada en los índices (§5.19). No se hereda. */
    boolean inContents = true;

    // --- Fragmentación ---------------------------------------------------------------------------
    /** Heredable. */
    int widows = 2;
    /** Heredable. */
    int orphans = 2;

    private final FlexStyle flex = new FlexStyle();

    private final GridStyle grid = new GridStyle();

    private final ItemStyle item = new ItemStyle();

    private final ListStyle list = new ListStyle();

    private final ContentsStyle contents = new ContentsStyle();

    private final ImageStyle image = new ImageStyle();

    private final MulticolStyle multicol = new MulticolStyle();

    private final BreakStyle breaks = new BreakStyle();

    private final BoxStyle box = new BoxStyle();

    // === Lectura: un método por propiedad, con el nombre del campo ===============================

    // --- Grupos de propiedades ------------------------------------------------------------------
    public BoxStyle box() {
            return box;
        }

        public BreakStyle breaks() {
            return breaks;
        }

        public MulticolStyle multicol() {
            return multicol;
        }

        public ImageStyle image() {
            return image;
        }

        public ContentsStyle contents() {
            return contents;
        }

        public ListStyle list() {
            return list;
        }

        public ItemStyle item() {
        return item;
    }

    public GridStyle grid() {
        return grid;
    }

    public FlexStyle flex() {
        return flex;
    }

    // --- Caja ------------------------------------------------------------------------------------
    public Layout layout() {
        return layout;
    }

    public Position position() {
        return position;
    }

    public Sides<Value> offset() {
        return offset;
    }

    public Integer layer() {
        return layer;
    }

    public Pages pages() {
        return pages;
    }

    public boolean isolate() {
        return isolate;
    }

    public Rotation rotation() {
        return rotation;
    }

    // --- Pintado (no afecta al layout) -----------------------------------------------------------
    public String color() {
        return color;
    }

    // --- Texto (heredables) ----------------------------------------------------------------------
    public String font() {
        return font;
    }

    public FontWeight fontWeight() {
        return fontWeight;
    }

    public FontStyle fontStyle() {
        return fontStyle;
    }

    public double fontSize() {
        return fontSize;
    }

    public Value lineHeight() {
        return lineHeight;
    }

    public TextAlign textAlign() {
        return textAlign;
    }

    public TextWrap textWrap() {
        return textWrap;
    }

    public Inline inline() {
        return inline;
    }

    public Transform transform() {
        return transform;
    }

    public double letterSpacing() {
        return letterSpacing;
    }

    public boolean smallCaps() {
        return smallCaps;
    }

    public boolean ligatures() {
        return ligatures;
    }

    public WhiteSpace whiteSpace() {
        return whiteSpace;
    }

    public int tabSize() {
        return tabSize;
    }

    public boolean numbered() {
        return numbered;
    }

    public Locale locale() {
        return locale;
    }

    // --- Enlace ----------------------------------------------------------------------------------
    public String link() {
        return link;
    }

    public boolean header() {
        return header;
    }

    public boolean inContents() {
        return inContents;
    }

    // --- Fragmentación ---------------------------------------------------------------------------
    public int widows() {
        return widows;
    }

    public int orphans() {
        return orphans;
    }

    /**
     * Línea del documento de cada propiedad declarada, por su clave ({@link PropertyKey}): el
     * nombre canónico ({@code "height"}; las de {@code Flex}, {@code Grid} e {@code Item}, con
     * prefijo: {@code "grid.rows"}). Las
     * de {@link #LINED_PROPERTIES} se heredan con su valor, para señalar dónde se declaró.
     */
    private final Map<String, Integer> lines = new HashMap<>();

    /**
     * Propiedades que se heredan con su línea, para señalarla en los errores: las de la fuente
     * (las de {@code Typography} que la eligen) y {@code Line height}, que fijan el alto de las
     * líneas (§5.12).
     */
    public static final List<String> LINED_PROPERTIES = List.of(PropertyKey.FONT, PropertyKey.FONT_WEIGHT,
            PropertyKey.FONT_STYLE, PropertyKey.FONT_SIZE, PropertyKey.LINE_HEIGHT);

    /** Estilo nuevo con los valores por defecto y las propiedades heredables del padre. */
    static Style inheriting(Style parent) {
        Style s = new Style();
        if (parent != null) {
            s.color = parent.color;
            s.font = parent.font;
            s.fontWeight = parent.fontWeight;
            s.fontStyle = parent.fontStyle;
            for (String p : LINED_PROPERTIES) {
                if (parent.lines.containsKey(p)) {
                    s.lines.put(p, parent.lines.get(p));
                }
            }
            s.fontSize = parent.fontSize;
            s.lineHeight = parent.lineHeight;
            s.textAlign = parent.textAlign;
            s.textWrap = parent.textWrap;
            s.inline = parent.inline;
            s.transform = parent.transform;
            s.letterSpacing = parent.letterSpacing;
            s.smallCaps = parent.smallCaps;
            s.ligatures = parent.ligatures;
            s.whiteSpace = parent.whiteSpace;
            s.tabSize = parent.tabSize;
            s.numbered = parent.numbered;
            s.locale = parent.locale;
            s.widows = parent.widows;
            s.orphans = parent.orphans;
        }
        return s;
    }

    /** Apunta la línea donde se declara una propiedad. */
    void declared(String property, int line) {
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

    /**
     * Fuente del texto. Solo en cajas con texto, que siempre tienen {@code Font} (§6). Las ligaduras se
     * desactivan con {@code Letter spacing} distinto de 0, como en CSS: separar las letras y unir dos en un
     * glifo se contradicen.
     */
    public FontFace fontFace() {
        return new FontFace(font, fontWeight, fontStyle, ligatures && letterSpacing == 0);
    }

    /** Altura de línea usada, en puntos. */
    public double usedLineHeight() {
        if (lineHeight == null) {
            return fontSize * 1.2;
        }
        return lineHeight.resolve(fontSize).orElse(fontSize * 1.2);
    }

    public boolean isPositioned() {
        return position != Position.STATIC;
    }

    public boolean isOutOfFlow() {
        return position == Position.ABSOLUTE || position == Position.FIXED;
    }

    /** Cierra el estilo con su caja ({@link Box#close}): ya no cambia. */
    void close() {
        box.close();
        breaks.close();
        multicol.close();
        image.close();
        contents.close();
        list.close();
        item.close();
        grid.close();
        flex.close();
    }
}
