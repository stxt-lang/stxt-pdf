package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;

import java.util.List;

/**
 * Propiedades especificadas de una caja, con los valores por defecto de ESPECIFICACION.md §4.
 *
 * <p>No hay cascada: cada caja declara sus propiedades. Solo se <strong>heredan</strong> las
 * marcadas como heredables (texto y fragmentación), igual que en CSS; ver {@link #inheriting}.</p>
 *
 * <p>Los nombres de las constantes de los enums coinciden con los valores del documento en
 * mayúsculas: así los interpreta {@code BoxTreeBuilder}.</p>
 */
public final class Style {

    public enum Layout { BLOCK, INLINE, FLEX, GRID }

    public enum Position { STATIC, RELATIVE, ABSOLUTE, FIXED }

    public enum BoxSizing { BORDER, CONTENT }

    public enum TextAlign { LEFT, RIGHT, CENTER, JUSTIFY }

    /** Reparto en líneas: voraz ({@code auto}) o Knuth-Plass ({@code pretty}). */
    public enum TextWrap { AUTO, PRETTY }

    public enum Direction { ROW, COLUMN }

    /** Reparto en un eje: {@code Main axis} y {@code Lines}. */
    public enum Distribution { START, END, CENTER, BETWEEN, AROUND, EVENLY, STRETCH }

    /** Alineación de un ítem dentro de su línea o celda. */
    public enum Alignment { START, END, CENTER, STRETCH }

    public enum Break { AUTO, PAGE, AVOID }

    public enum Decoration { SLICE, CLONE }

    // --- Caja --------------------------------------------------------------------------------
    public Layout layout = Layout.BLOCK;
    public Position position = Position.STATIC;
    public Sides<Value> offset = Sides.all(Value.AUTO);
    /** {@code null} = auto. */
    public Integer layer;
    public boolean isolate;
    public BoxSizing boxSizing = BoxSizing.BORDER;

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
    public String font = "Helvetica";
    public double fontSize = 10;
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
    /** Texto de {@code Cell}, o {@code null} = colocación automática. */
    public String itemCell;
    /** {@code null} = la del contenedor. */
    public Alignment itemHorizontal;
    /** {@code null} = la del contenedor. */
    public Alignment itemVertical;

    // --- Fragmentación ---------------------------------------------------------------------------
    public Break breakBefore = Break.AUTO;
    public Break breakAfter = Break.AUTO;
    public boolean avoidBreakInside;
    /** Heredable. */
    public int widows = 2;
    /** Heredable. */
    public int orphans = 2;
    public Decoration decoration = Decoration.SLICE;

    /** Estilo nuevo con los valores por defecto y las propiedades heredables del padre. */
    public static Style inheriting(Style parent) {
        Style s = new Style();
        if (parent != null) {
            s.color = parent.color;
            s.font = parent.font;
            s.fontSize = parent.fontSize;
            s.lineHeight = parent.lineHeight;
            s.textAlign = parent.textAlign;
            s.textWrap = parent.textWrap;
            s.widows = parent.widows;
            s.orphans = parent.orphans;
        }
        return s;
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
