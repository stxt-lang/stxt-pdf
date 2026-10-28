package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Cell;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.model.GridAreas;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;

import java.util.HashMap;
import java.util.List;
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
 */
public final class Style {

    /** Cómo coloca la caja a sus hijos: lo da el elemento. Una {@code IMAGE} no tiene hijos. */
    public enum Layout { BLOCK, INLINE, FLEX, GRID, MULTICOL, IMAGE }

    /** Cómo participa la caja en el flujo de su padre: {@code Relative}, {@code Absolute} y {@code Fixed}; el resto, {@code STATIC}. */
    public enum Position { STATIC, RELATIVE, ABSOLUTE, FIXED }

    public enum TextAlign { LEFT, RIGHT, CENTER, JUSTIFY }

    /** Reparto en líneas: voraz ({@code auto}) o Knuth-Plass ({@code pretty}). */
    public enum TextWrap { AUTO, PRETTY }

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

    // --- Caja --------------------------------------------------------------------------------
    public Layout layout = Layout.BLOCK;
    public Position position = Position.STATIC;
    public Sides<Value> offset = Sides.all(Value.AUTO);
    /** {@code null} = auto. */
    public Integer layer;
    /** Solo en un {@code Fixed}: las páginas en que sale. */
    public Pages pages = Pages.ALL;
    public boolean isolate;

    // --- Dimensiones ---------------------------------------------------------------------------
    public Value width = Value.AUTO;
    public Value height = Value.AUTO;
    public Value minWidth = Value.AUTO;
    public Value minHeight = Value.AUTO;
    /** {@code null} = sin máximo. */
    public Value maxWidth;
    /** {@code null} = sin máximo. */
    public Value maxHeight;
    /** Ancho / alto; {@code null} = sin proporción. */
    public Double aspectRatio;

    // --- Modelo de caja ------------------------------------------------------------------------
    public Sides<Value> margin = Sides.all(Value.pt(0));
    public Sides<Value> padding = Sides.all(Value.pt(0));
    public Sides<Value> border = Sides.all(Value.pt(0));

    // --- Pintado (no afecta al layout) ---------------------------------------------------------
    /** {@code null} = transparente. */
    public String background;
    /** Heredable. */
    public String color = "#000000";
    /** {@code null} = igual que {@link #color}. */
    public String borderColor;

    // --- Texto (heredables) ---------------------------------------------------------------------
    /** Familia; {@code null} = sin declarar, que es un error en una caja con texto (§6). */
    public String font;
    public FontWeight fontWeight = FontWeight.NORMAL;
    public FontStyle fontStyle = FontStyle.NORMAL;
    /** {@code NaN} = sin declarar, que es un error en una caja con texto (§6). */
    public double fontSize = Double.NaN;
    /** {@code null} = 120 % del tamaño. Un número son puntos; un porcentaje, relativo al tamaño. */
    public Value lineHeight;
    public TextAlign textAlign = TextAlign.LEFT;
    public TextWrap textWrap = TextWrap.AUTO;

    // --- Flex ------------------------------------------------------------------------------------
    public Direction flexDirection = Direction.ROW;
    public boolean flexWrap;
    public Distribution flexMainAxis = Distribution.START;
    public Alignment flexCrossAxis = Alignment.STRETCH;
    public Distribution flexLines = Distribution.STRETCH;

    // --- Hueco (flex y grid) -------------------------------------------------------------------
    public Value rowGap = Value.pt(0);
    public Value columnGap = Value.pt(0);

    // --- Grid ------------------------------------------------------------------------------------
    public List<Value> gridColumns = List.of();
    public List<Value> gridRows = List.of();
    /** {@code Areas} ya interpretado; {@link GridAreas#NONE} si no se declara. */
    public GridAreas gridAreas = GridAreas.NONE;
    public Direction gridFlow = Direction.ROW;
    public Alignment gridHorizontal = Alignment.STRETCH;
    public Alignment gridVertical = Alignment.STRETCH;

    // --- Item (como hijo de flex o grid) -----------------------------------------------------
    public double itemGrow = 0;
    public double itemShrink = 1;
    public Value itemBasis = Value.AUTO;
    /** {@code null} = la del contenedor. */
    public Alignment itemCrossAxis;
    /**
     * Colocación pedida por {@code Area}, con los nombres ya resueltos, o la que da la tabla a
     * cada {@code Cell}; {@link Cell#AUTO} = colocación automática.
     */
    public Cell itemArea = Cell.AUTO;
    /** {@code null} = la del contenedor. */
    public Alignment itemHorizontal;
    /** {@code null} = la del contenedor. */
    public Alignment itemVertical;

    // --- Lista ---------------------------------------------------------------------------------
    /** Marcador de los ítems de una {@code List}; {@code null} en el resto de cajas. */
    public Marker listMarker;
    /** Número del primer ítem de una {@code List}. */
    public int listStart = 1;
    /** Número que fija un {@code List item}; {@code null} = el siguiente al del ítem anterior. */
    public Integer listNumber;

    // --- Enlace ----------------------------------------------------------------------------------
    /**
     * Destino del {@code Link} de la caja ({@link dev.stxt.boxes.model.Links}, §5.20); {@code null}
     * = sin enlace. No se hereda: lo que hay dentro de una caja con enlace es clicable por ella,
     * pero no lleva enlace propio.
     */
    public String link;

    // --- Imagen ----------------------------------------------------------------------------------
    /** Texto alternativo de una {@code Image}; {@code null} = decorativa. */
    public String alt;
    public Fit fit = Fit.CONTAIN;

    // --- Contents --------------------------------------------------------------------------------
    /** Primer y último nivel de encabezado que lista un {@code Contents} ({@code Levels}). */
    public int contentsFrom = 1;
    public int contentsTo = 3;
    /** {@code Leader} de un {@code Contents}; también lo lleva la guía generada de cada entrada. */
    public Leader contentsLeader = Leader.DOTS;

    // --- Multicol --------------------------------------------------------------------------------
    /** {@code Count}; {@code null} = sin declarar ({@code Column width} fija entonces las columnas). */
    public Integer multicolCount;
    /** {@code Column width}, en puntos; {@code null} = sin declarar. */
    public Double multicolWidth;
    /** {@code Column gap}, en puntos. */
    public double multicolGap = 0;
    /** Grosor de la línea entre columnas ({@code Rule}); 0 = sin línea. */
    public double multicolRule = 0;
    /** {@code Rule color}; {@code null} = igual que {@link #color}. */
    public String multicolRuleColor;
    public Fill multicolFill = Fill.BALANCE;

    // --- Fragmentación ---------------------------------------------------------------------------
    public Break breakBefore = Break.AUTO;
    public Break breakAfter = Break.AUTO;
    public boolean avoidBreakInside;
    /** Heredable. */
    public int widows = 2;
    /** Heredable. */
    public int orphans = 2;
    public Decoration decoration = Decoration.SLICE;

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
    public static Style inheriting(Style parent) {
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
            s.widows = parent.widows;
            s.orphans = parent.orphans;
        }
        return s;
    }

    /** Apunta la línea donde se declara una propiedad. */
    public void declared(String property, int line) {
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

    /** Fuente del texto. Solo en cajas con texto, que siempre tienen {@code Font} (§6). */
    public FontFace fontFace() {
        return new FontFace(font, fontWeight, fontStyle);
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
