package dev.stxt.boxes.tree;

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

    /** Cómo coloca la caja a sus hijos: lo da el elemento. */
    public enum Layout { BLOCK, INLINE, FLEX, GRID }

    /** Cómo participa la caja en el flujo de su padre: {@code Relative}, {@code Absolute} y {@code Fixed}; el resto, {@code STATIC}. */
    public enum Position { STATIC, RELATIVE, ABSOLUTE, FIXED }

    public enum TextAlign { LEFT, RIGHT, CENTER, JUSTIFY }

    public enum FontWeight { NORMAL, BOLD }

    public enum FontStyle { NORMAL, ITALIC }

    /** Reparto en líneas: voraz ({@code auto}) o Knuth-Plass ({@code pretty}). */
    public enum TextWrap { AUTO, PRETTY }

    public enum Direction { ROW, COLUMN }

    /** Reparto en un eje: {@code Main axis} y {@code Lines}. */
    public enum Distribution { START, END, CENTER, BETWEEN, AROUND, EVENLY, STRETCH }

    /** Alineación de un ítem dentro de su línea o celda. */
    public enum Alignment { START, END, CENTER, STRETCH }

    public enum Break { AUTO, PAGE, AVOID }

    public enum Decoration { SLICE, CLONE }

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
    /** Bloque de texto de {@code Areas}, o {@code null}. */
    public String gridAreas;
    public Direction gridFlow = Direction.ROW;
    public Alignment gridHorizontal = Alignment.STRETCH;
    public Alignment gridVertical = Alignment.STRETCH;

    // --- Item (como hijo de flex o grid) -----------------------------------------------------
    public double itemGrow = 0;
    public double itemShrink = 1;
    public Value itemBasis = Value.AUTO;
    /** {@code null} = la del contenedor. */
    public Alignment itemCrossAxis;
    /** Texto de {@code Area}, o {@code null} = colocación automática. */
    public String itemArea;
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
