package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Cell;
import dev.stxt.boxes.model.Corners;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.model.GridAreas;
import dev.stxt.boxes.model.Paint;
import dev.stxt.boxes.model.Rotation;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;

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
 * <p>Los nombres de las constantes de los enums coinciden con los valores del documento en
 * mayúsculas y con el guion como guion bajo ({@code lower-alpha} es {@code LOWER_ALPHA}): así los
 * interpreta {@code BoxTreeBuilder}. {@link Layout} y {@link Position} no son
 * valores del documento: salen del elemento ({@code Text} es {@code INLINE}; {@code Absolute},
 * un {@code BLOCK} {@code ABSOLUTE}).</p>
 *
 * <p>Cada propiedad se lee con el método del mismo nombre ({@code style.fontSize()}). Los campos son del
 * package {@code tree}, el único que escribe: fuera de él un estilo solo se lee.</p>
 *
 * <p><strong>Hilos:</strong> como {@link Box}, se escribe solo mientras {@link BoxTreeBuilder} construye el
 * árbol y después se lee.</p>
 */
public final class Style {

    /** Cómo coloca la caja a sus hijos: lo da el elemento. Una {@code IMAGE} no tiene hijos. */
    public enum Layout { BLOCK, INLINE, FLEX, GRID, MULTICOL, IMAGE }

    /** Cómo participa la caja en el flujo de su padre: {@code Relative}, {@code Absolute} y {@code Fixed}; el resto, {@code STATIC}. */
    public enum Position { STATIC, RELATIVE, ABSOLUTE, FIXED }

    public enum TextAlign { LEFT, RIGHT, CENTER, JUSTIFY }

    /** Reparto en líneas: voraz ({@code auto}) o Knuth-Plass ({@code pretty}). */
    public enum TextWrap { AUTO, PRETTY }

    /** Formato dentro de la línea: el texto literal ({@code none}) o escrito en Markdown ({@code markdown}). */
    public enum Inline { NONE, MARKDOWN }

    /** Transformación de las mayúsculas del texto (§4.4): {@code none}, {@code uppercase}, {@code lowercase} o {@code capitalize}. */
    public enum Transform { NONE, UPPERCASE, LOWERCASE, CAPITALIZE }

    /**
     * Tratamiento del espacio en blanco (§4.4, §5.4): {@code normal} lo colapsa; {@code pre} conserva
     * los espacios y los saltos de línea y no parte las líneas; {@code pre-wrap} también, pero parte
     * las líneas demasiado anchas entre palabras.
     */
    public enum WhiteSpace {
        NORMAL, PRE, PRE_WRAP;

        /** Si conserva los espacios y los saltos de línea del texto. */
        public boolean preformatted() {
            return this != NORMAL;
        }
    }

    public enum Direction { ROW, COLUMN }

    /** Reparto en un eje: {@code Main axis} y {@code Lines}. */
    public enum Distribution { START, END, CENTER, BETWEEN, AROUND, EVENLY, STRETCH }

    /** Alineación de un ítem dentro de su línea o celda. */
    public enum Alignment { START, END, CENTER, STRETCH }

    /** {@code column} solo dentro de un {@code Multicol}: pasa a la columna siguiente. */
    public enum Break { AUTO, PAGE, COLUMN, AVOID }

    public enum Decoration { SLICE, CLONE }

    /** Reparto entre las columnas de un {@code Multicol}: equilibrado o en orden. */
    public enum Fill { BALANCE, AUTO }

    /** Guía entre el título y el número de página de las entradas de un {@code Contents} (§5.19). */
    public enum Leader { DOTS, RULE, NONE }

    /** Las páginas en que sale un {@code Fixed} (§5.5); la página 1 es la primera del documento. */
    public enum Pages {
        ALL, FIRST, NOT_FIRST, ODD, EVEN, LAST;

        /** Si un {@code Fixed} con este valor sale en la página {@code page} de un documento de {@code total}. */
        public boolean includes(int page, int total) {
            return switch (this) {
                case ALL -> true;
                case FIRST -> page == 1;
                case NOT_FIRST -> page != 1;
                case ODD -> page % 2 == 1;
                case EVEN -> page % 2 == 0;
                case LAST -> page == total;
            };
        }
    }

    /** Cómo llena una imagen su caja si tienen proporciones distintas (§5.18). */
    public enum Fit { CONTAIN, COVER, FILL }

    /** Marcador de los ítems de una lista: un símbolo, ninguno o una numeración. */
    public enum Marker {
        DISC, DASH, NONE, DECIMAL, LOWER_ALPHA, UPPER_ALPHA, LOWER_ROMAN, UPPER_ROMAN;

        /** Si numera los ítems: solo entonces tienen efecto {@code Start} y {@code Number}. */
        public boolean numbered() {
            return compareTo(DECIMAL) >= 0;
        }
    }

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

    // --- Dimensiones ---------------------------------------------------------------------------
    Value width = Value.AUTO;
    Value height = Value.AUTO;
    Value minWidth = Value.AUTO;
    Value minHeight = Value.AUTO;
    /** {@code null} = sin máximo. */
    Value maxWidth;
    /** {@code null} = sin máximo. */
    Value maxHeight;
    /** Ancho / alto; {@code null} = sin proporción. */
    Double aspectRatio;

    // --- Modelo de caja ------------------------------------------------------------------------
    Sides<Value> margin = Sides.all(Value.pt(0));
    Sides<Value> padding = Sides.all(Value.pt(0));
    Sides<Value> border = Sides.all(Value.pt(0));

    // --- Pintado (no afecta al layout) ---------------------------------------------------------
    /** {@code null} = transparente. */
    Paint background;
    /** Heredable. */
    String color = "#000000";
    /** Un color por lado; {@code null} = igual que {@link #color}. */
    Sides<String> borderColor;
    /** Esquinas redondeadas, de la caja de borde; solo pintado. */
    Corners<Value> radius = Corners.all(Value.pt(0));

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

    // --- Flex ------------------------------------------------------------------------------------
    Direction flexDirection = Direction.ROW;
    boolean flexWrap;
    Distribution flexMainAxis = Distribution.START;
    Alignment flexCrossAxis = Alignment.STRETCH;
    Distribution flexLines = Distribution.STRETCH;

    // --- Hueco (flex y grid) -------------------------------------------------------------------
    Value rowGap = Value.pt(0);
    Value columnGap = Value.pt(0);

    // --- Grid ------------------------------------------------------------------------------------
    List<Value> gridColumns = List.of();
    List<Value> gridRows = List.of();
    /** {@code Areas} ya interpretado; {@link GridAreas#NONE} si no se declara. */
    GridAreas gridAreas = GridAreas.NONE;
    Direction gridFlow = Direction.ROW;
    Alignment gridHorizontal = Alignment.STRETCH;
    Alignment gridVertical = Alignment.STRETCH;

    // --- Item (como hijo de flex o grid) -----------------------------------------------------
    double itemGrow = 0;
    double itemShrink = 1;
    Value itemBasis = Value.AUTO;
    /** {@code null} = la del contenedor. */
    Alignment itemCrossAxis;
    /**
     * Colocación pedida por {@code Area}, con los nombres ya resueltos, o la que da la tabla a
     * cada {@code Cell}; {@link Cell#AUTO} = colocación automática.
     */
    Cell itemArea = Cell.AUTO;
    /** {@code null} = la del contenedor. */
    Alignment itemHorizontal;
    /** {@code null} = la del contenedor. */
    Alignment itemVertical;

    // --- Lista ---------------------------------------------------------------------------------
    /** Marcador de los ítems de una {@code List}; {@code null} en el resto de cajas. */
    Marker listMarker;
    /** Número del primer ítem de una {@code List}. */
    int listStart = 1;
    /** Número que fija un {@code List item}; {@code null} = el siguiente al del ítem anterior. */
    Integer listNumber;

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

    // --- Imagen ----------------------------------------------------------------------------------
    /** Texto alternativo de una {@code Image}; {@code null} = decorativa. */
    String alt;
    Fit fit = Fit.CONTAIN;

    // --- Contents --------------------------------------------------------------------------------
    /** Primer y último nivel de encabezado que lista un {@code Contents} ({@code Levels}). */
    int contentsFrom = 1;
    int contentsTo = 3;
    /** {@code Leader} de un {@code Contents}; también lo lleva la guía generada de cada entrada. */
    Leader contentsLeader = Leader.DOTS;

    // --- Multicol --------------------------------------------------------------------------------
    /** {@code Count}; {@code null} = sin declarar ({@code Column width} fija entonces las columnas). */
    Integer multicolCount;
    /** {@code Column width}, en puntos; {@code null} = sin declarar. */
    Double multicolWidth;
    /** {@code Column gap}, en puntos. */
    double multicolGap = 0;
    /** Grosor de la línea entre columnas ({@code Rule}); 0 = sin línea. */
    double multicolRule = 0;
    /** {@code Rule color}; {@code null} = igual que {@link #color}. */
    String multicolRuleColor;
    Fill multicolFill = Fill.BALANCE;

    // --- Fragmentación ---------------------------------------------------------------------------
    Break breakBefore = Break.AUTO;
    Break breakAfter = Break.AUTO;
    boolean avoidBreakInside;
    /** Heredable. */
    int widows = 2;
    /** Heredable. */
    int orphans = 2;
    Decoration decoration = Decoration.SLICE;

    // === Lectura: un método por propiedad, con el nombre del campo ===============================

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


    // --- Dimensiones -----------------------------------------------------------------------------
    public Value width() {
        return width;
    }

    public Value height() {
        return height;
    }

    public Value minWidth() {
        return minWidth;
    }

    public Value minHeight() {
        return minHeight;
    }

    public Value maxWidth() {
        return maxWidth;
    }

    public Value maxHeight() {
        return maxHeight;
    }

    public Double aspectRatio() {
        return aspectRatio;
    }


    // --- Modelo de caja --------------------------------------------------------------------------
    public Sides<Value> margin() {
        return margin;
    }

    public Sides<Value> padding() {
        return padding;
    }

    public Sides<Value> border() {
        return border;
    }


    // --- Pintado (no afecta al layout) -----------------------------------------------------------
    public Paint background() {
        return background;
    }

    public String color() {
        return color;
    }

    public Sides<String> borderColor() {
        return borderColor;
    }

    public Corners<Value> radius() {
        return radius;
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


    // --- Flex ------------------------------------------------------------------------------------
    public Direction flexDirection() {
        return flexDirection;
    }

    public boolean flexWrap() {
        return flexWrap;
    }

    public Distribution flexMainAxis() {
        return flexMainAxis;
    }

    public Alignment flexCrossAxis() {
        return flexCrossAxis;
    }

    public Distribution flexLines() {
        return flexLines;
    }


    // --- Hueco (flex y grid) ---------------------------------------------------------------------
    public Value rowGap() {
        return rowGap;
    }

    public Value columnGap() {
        return columnGap;
    }


    // --- Grid ------------------------------------------------------------------------------------
    public List<Value> gridColumns() {
        return gridColumns;
    }

    public List<Value> gridRows() {
        return gridRows;
    }

    public GridAreas gridAreas() {
        return gridAreas;
    }

    public Direction gridFlow() {
        return gridFlow;
    }

    public Alignment gridHorizontal() {
        return gridHorizontal;
    }

    public Alignment gridVertical() {
        return gridVertical;
    }


    // --- Item (como hijo de flex o grid) ---------------------------------------------------------
    public double itemGrow() {
        return itemGrow;
    }

    public double itemShrink() {
        return itemShrink;
    }

    public Value itemBasis() {
        return itemBasis;
    }

    public Alignment itemCrossAxis() {
        return itemCrossAxis;
    }

    public Cell itemArea() {
        return itemArea;
    }

    public Alignment itemHorizontal() {
        return itemHorizontal;
    }

    public Alignment itemVertical() {
        return itemVertical;
    }


    // --- Lista -----------------------------------------------------------------------------------
    public Marker listMarker() {
        return listMarker;
    }

    public int listStart() {
        return listStart;
    }

    public Integer listNumber() {
        return listNumber;
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


    // --- Imagen ----------------------------------------------------------------------------------
    public String alt() {
        return alt;
    }

    public Fit fit() {
        return fit;
    }


    // --- Contents --------------------------------------------------------------------------------
    public int contentsFrom() {
        return contentsFrom;
    }

    public int contentsTo() {
        return contentsTo;
    }

    public Leader contentsLeader() {
        return contentsLeader;
    }


    // --- Multicol --------------------------------------------------------------------------------
    public Integer multicolCount() {
        return multicolCount;
    }

    public Double multicolWidth() {
        return multicolWidth;
    }

    public double multicolGap() {
        return multicolGap;
    }

    public double multicolRule() {
        return multicolRule;
    }

    public String multicolRuleColor() {
        return multicolRuleColor;
    }

    public Fill multicolFill() {
        return multicolFill;
    }


    // --- Fragmentación ---------------------------------------------------------------------------
    public Break breakBefore() {
        return breakBefore;
    }

    public Break breakAfter() {
        return breakAfter;
    }

    public boolean avoidBreakInside() {
        return avoidBreakInside;
    }

    public int widows() {
        return widows;
    }

    public int orphans() {
        return orphans;
    }

    public Decoration decoration() {
        return decoration;
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
}
