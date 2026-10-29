package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Direction;
import dev.stxt.pdf.model.Distribution;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.model.Value;

import java.util.List;

import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.label;
import static dev.stxt.pdf.tree.Nodes.position;
import static dev.stxt.pdf.tree.PropertyKey.ASPECT_RATIO;
import static dev.stxt.pdf.tree.PropertyKey.BREAK_AFTER;
import static dev.stxt.pdf.tree.PropertyKey.BREAK_BEFORE;
import static dev.stxt.pdf.tree.PropertyKey.BREAK_INSIDE;
import static dev.stxt.pdf.tree.PropertyKey.DECORATION;
import static dev.stxt.pdf.tree.PropertyKey.COLUMN_GAP;
import static dev.stxt.pdf.tree.PropertyKey.RULE;
import static dev.stxt.pdf.tree.PropertyKey.FLEX_GAP;
import static dev.stxt.pdf.tree.PropertyKey.FLEX_LINES;
import static dev.stxt.pdf.tree.PropertyKey.FLEX_MAIN_AXIS;
import static dev.stxt.pdf.tree.PropertyKey.FONT_SIZE;
import static dev.stxt.pdf.tree.PropertyKey.GRID_GAP;
import static dev.stxt.pdf.tree.PropertyKey.GRID_ROWS;
import static dev.stxt.pdf.tree.PropertyKey.HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.ITEM_BASIS;
import static dev.stxt.pdf.tree.PropertyKey.ITEM_GROW;
import static dev.stxt.pdf.tree.PropertyKey.MARGIN;
import static dev.stxt.pdf.tree.PropertyKey.MAX_HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.MAX_WIDTH;
import static dev.stxt.pdf.tree.PropertyKey.MIN_HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.MIN_WIDTH;
import static dev.stxt.pdf.tree.PropertyKey.OFFSET;
import static dev.stxt.pdf.tree.PropertyKey.ROTATE_ORIGIN;
import static dev.stxt.pdf.tree.PropertyKey.WIDTH;
import static dev.stxt.pdf.tree.ValueReader.amount;
import static dev.stxt.pdf.tree.ValueReader.comparable;
import static dev.stxt.pdf.tree.ValueReader.hasPercentage;

/**
 * Reglas del modelo que el schema no puede expresar (referencia/comun/errores.md) y que miran la caja
 * ya leída: desplazamientos, márgenes {@code auto}, mínimo y máximo, porcentajes verticales y si
 * el alto es definido (referencia/comun/valores.md), y la fuente de los textos.
 */
final class ModelValidator {

    private ModelValidator() {
    }

    /** {@code Main axis} y {@code Lines} de un {@code Flex}. */
    static void validateFlex(Style s) {
        if (s.flex().mainAxis() == Distribution.STRETCH) {
            throw error(s.line(FLEX_MAIN_AXIS), ErrorCode.VALUE_NOT_ALLOWED,
                    "Main axis no admite 'stretch'; usa Grow en los ítems");
        }
        if (s.isDeclared(FLEX_LINES) && !s.flex().wrap()) {
            throw error(s.line(FLEX_LINES), ErrorCode.LINES_WITHOUT_WRAP,
                    "Lines solo tiene efecto con Wrap: true, porque sin Wrap hay una sola línea");
        }
    }

    /**
     * Un {@code Multicol} fija sus columnas con {@code Count}, {@code Column width} o los dos, y
     * su línea entre columnas cabe en el hueco (referencia/componentes/multicol.md).
     */
    static void validateMulticol(Style s, int line) {
        if (s.multicol().count() == null && s.multicol().columnWidth() == null) {
            throw error(line, ErrorCode.MULTICOL_COLUMNS, "Un Multicol necesita Count, Column width o los dos: "
                    + "Count fija el número de columnas y Column width su ancho mínimo");
        }
        if (s.multicol().rule() > s.multicol().columnGap() + Tolerance.EPSILON) {
            String gap = s.isDeclared(COLUMN_GAP) ? "Column gap (línea " + s.line(COLUMN_GAP) + ")"
                    : "Column gap, que sin declarar es 0";
            throw error(s.line(RULE), ErrorCode.COLUMN_RULE_WIDTH, "Rule es más ancha que " + gap
                    + ": la línea se pintaría sobre las columnas; ensancha Column gap o adelgaza Rule");
        }
    }

    /**
     * Una {@code Image} no se parte (referencia/componentes/image.md): {@code Inside} y {@code Decoration}, que solo
     * cuentan al partir una caja, no tienen efecto (también si los da una regla de {@code Styles}).
     */
    static void validateImage(Style s) {
        for (String p : List.of(BREAK_INSIDE, DECORATION)) {
            if (s.isDeclared(p)) {
                throw error(s.line(p), ErrorCode.BREAK_IN_IMAGE, (p.equals(BREAK_INSIDE) ? "Inside" : "Decoration")
                        + " no tiene efecto en una Image, que no se parte entre páginas: quítalo");
            }
        }
    }

    /**
     * El giro de una caja (referencia/comun/capas-y-giro.md): {@code Rotate origin} sin {@code Rotate} no tiene efecto,
     * y un {@code Relative} girado no se parte entre páginas, así que {@code Inside} y {@code Decoration} tampoco
     * (también si los da una regla de {@code Styles}).
     */
    static void validateRotation(Style s) {
        if (s.rotation() == null) {
            if (s.isDeclared(ROTATE_ORIGIN)) {
                throw error(s.line(ROTATE_ORIGIN), ErrorCode.ROTATE_ORIGIN_WITHOUT_ROTATE, "Rotate origin no tiene "
                        + "efecto sin Rotate: declara el giro o quítalo");
            }
            return;
        }
        if (s.position() != Position.RELATIVE) {
            return;
        }
        for (String p : List.of(BREAK_INSIDE, DECORATION)) {
            if (s.isDeclared(p)) {
                throw error(s.line(p), ErrorCode.BREAK_IN_ROTATED, (p.equals(BREAK_INSIDE) ? "Inside" : "Decoration")
                        + " no tiene efecto en un Relative girado, que no se parte entre páginas: quítalo");
            }
        }
    }

    /** Las reglas de una caja ya leída, antes de crearla: colocación, desplazamiento, márgenes, tamaños, porcentajes y cortes de columna. */
    static void validateBox(BuildContext ctx, Style s, Box parent) {
        Layout parentLayout = parent.style().layout();
        // Los hijos en flujo de un Flex o un Grid son ítems (con su Item o su Area), y las celdas
        // de una Table, ítems de su grid.
        boolean flexItem = !s.isOutOfFlow() && parentLayout == Layout.FLEX;
        boolean gridItem = !s.isOutOfFlow() && parentLayout == Layout.GRID;

        validateOffset(s);
        validateMargins(s, parentLayout, flexItem, gridItem);
        if (s.isDeclared(ASPECT_RATIO) && !s.box().width().isAuto() && !s.box().height().isAuto()) {
            throw error(s.line(ASPECT_RATIO), ErrorCode.ASPECT_RATIO_CONFLICT,
                    "Aspect ratio no tiene efecto si Width y Height están declarados");
        }
        validateMinMax(s, WIDTH, s.box().width(), MIN_WIDTH, s.box().minWidth(), MAX_WIDTH, s.box().maxWidth());
        validateMinMax(s, HEIGHT, s.box().height(), MIN_HEIGHT, s.box().minHeight(), MAX_HEIGHT, s.box().maxHeight());
        validatePercentages(ctx, s, parent, flexItem, gridItem);
        validateColumnBreaks(ctx, s);
    }

    /** {@code Before} y {@code After: column} solo dentro de un {@code Multicol}, que es quien tiene columnas. */
    private static void validateColumnBreaks(BuildContext ctx, Style s) {
        if (ctx.insideMulticol) {
            return;
        }
        String property = s.breaks().before() == Break.COLUMN ? BREAK_BEFORE
                : s.breaks().after() == Break.COLUMN ? BREAK_AFTER : null;
        if (property != null) {
            throw error(s.line(property), ErrorCode.COLUMN_BREAK_OUTSIDE_MULTICOL, (property.equals(BREAK_BEFORE)
                    ? "Before" : "After") + ": column solo tiene efecto dentro de un Multicol: usa page para "
                    + "cortar la página");
        }
    }

    private static void validateOffset(Style s) {
        if (!s.isDeclared(OFFSET)) {
            return;
        }
        int line = s.line(OFFSET);
        Sides<Value> o = s.offset();
        if (s.position() == Position.RELATIVE) {
            if (!o.top().isAuto() && !o.bottom().isAuto()) {
                throw error(line, ErrorCode.OFFSET_CONFLICT, "Un Relative no puede desplazarse a la vez "
                        + "desde arriba y desde abajo: deja uno de los dos en auto");
            }
            if (!o.left().isAuto() && !o.right().isAuto()) {
                throw error(line, ErrorCode.OFFSET_CONFLICT, "Un Relative no puede desplazarse a la vez "
                        + "desde la izquierda y desde la derecha: deja uno de los dos en auto");
            }
            return;
        }
        // Absolute y Fixed: inicio + tamaño + fin no puede fijar la posición dos veces.
        if (!o.left().isAuto() && !s.box().width().isAuto() && !o.right().isAuto()
                && !s.box().margin().left().isAuto() && !s.box().margin().right().isAuto()) {
            throw error(line, ErrorCode.OVERCONSTRAINED, "Izquierda, Width y derecha declarados fijan la "
                    + "posición dos veces: deja uno en auto, o usa Margin auto para centrar");
        }
        if (!o.top().isAuto() && !s.box().height().isAuto() && !o.bottom().isAuto()
                && !s.box().margin().top().isAuto() && !s.box().margin().bottom().isAuto()) {
            throw error(line, ErrorCode.OVERCONSTRAINED, "Arriba, Height y abajo declarados fijan la "
                    + "posición dos veces: deja uno en auto, o usa Margin auto para centrar");
        }
    }

    /** Los márgenes {@code auto} solo centran: en flujo de bloque, en horizontal y con ancho limitado; en Absolute y Fixed, con inicio, tamaño y fin. */
    private static void validateMargins(Style s, Layout parentLayout, boolean flexItem, boolean gridItem) {
        Sides<Value> m = s.box().margin();
        boolean horizontalAuto = m.left().isAuto() || m.right().isAuto();
        boolean verticalAuto = m.top().isAuto() || m.bottom().isAuto();
        if (!horizontalAuto && !verticalAuto) {
            return;
        }
        int line = s.line(MARGIN);
        if (flexItem || gridItem) {
            throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto no tiene efecto en ítems de flex o "
                    + "grid; alinea con Main axis, Cross axis, Horizontal o Vertical");
        }
        if (s.isOutOfFlow()) {
            validateOutOfFlowMargins(s, line, horizontalAuto, verticalAuto);
            return;
        }
        if (parentLayout == Layout.BLOCK) {
            if (verticalAuto) {
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT,
                        "Margin auto en vertical no tiene efecto en flujo de bloque: usa un número");
            }
            // Una imagen con Width auto mide su ancho natural, no todo el ancho: se puede centrar.
            if (s.box().width().isAuto() && s.box().maxWidth() == null && s.layout() != Layout.IMAGE) {
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto no centra una caja que ocupa "
                        + "todo el ancho: declara Width o Max width");
            }
        }
    }

    /** Un {@code Absolute} o un {@code Fixed} solo se centra con {@code Margin auto} si declara inicio, tamaño y fin. */
    private static void validateOutOfFlowMargins(Style s, int line, boolean horizontalAuto, boolean verticalAuto) {
        Sides<Value> o = s.offset();
        if (horizontalAuto && (o.left().isAuto() || s.box().width().isAuto() || o.right().isAuto())) {
            throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto en horizontal solo centra un "
                    + position(s) + " con izquierda, Width y derecha declarados");
        }
        if (verticalAuto && (o.top().isAuto() || s.box().height().isAuto() || o.bottom().isAuto())) {
            throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto en vertical solo centra un "
                    + position(s) + " con arriba, Height y abajo declarados");
        }
    }

    /** Tamaño, mínimo y máximo comparables (los dos en puntos o los dos en porcentaje) que se contradicen. */
    private static void validateMinMax(Style s, String size, Value value, String min, Value minValue,
                                       String max, Value maxValue) {
        if (maxValue != null && comparable(minValue, maxValue) && amount(minValue) > amount(maxValue)) {
            throw error(s.line(min), ErrorCode.MIN_MAX_CONFLICT,
                    label(min) + " es mayor que " + label(max) + " (línea " + s.line(max) + ")");
        }
        if (comparable(value, minValue) && amount(value) < amount(minValue)) {
            throw error(s.line(size), ErrorCode.MIN_MAX_CONFLICT,
                    label(size) + " es menor que " + label(min) + " (línea " + s.line(min) + ")");
        }
        if (maxValue != null && comparable(value, maxValue) && amount(value) > amount(maxValue)) {
            throw error(s.line(size), ErrorCode.MIN_MAX_CONFLICT,
                    label(size) + " es mayor que " + label(max) + " (línea " + s.line(max) + ")");
        }
    }

    /**
     * Porcentajes verticales de la caja: su base debe ser un alto definido (referencia/comun/valores.md). La base de
     * {@code Height}, {@code Min height} y {@code Max height} es el containing block (siempre
     * definido para Absolute y Fixed) o el área (los ítems de grid); la de los desplazamientos
     * de un Relative y de {@code Basis} en un flex {@code column}, la caja de contenido del padre.
     */
    private static void validatePercentages(BuildContext ctx, Style s, Box parent, boolean flexItem,
                                            boolean gridItem) {
        boolean ownBase = s.isOutOfFlow() || gridItem || ctx.isDefinite(parent);
        for (String p : List.of(HEIGHT, MIN_HEIGHT, MAX_HEIGHT)) {
            Value v = switch (p) {
                case HEIGHT -> s.box().height();
                case MIN_HEIGHT -> s.box().minHeight();
                default -> s.box().maxHeight();
            };
            if (!ownBase && v != null && hasPercentage(v)) {
                throw indefinite(s.line(p), label(p));
            }
        }
        boolean parentDefinite = ctx.isDefinite(parent);
        if (s.position() == Position.RELATIVE && !parentDefinite
                && (hasPercentage(s.offset().top()) || hasPercentage(s.offset().bottom()))) {
            throw indefinite(s.line(OFFSET), "El desplazamiento vertical de Offset");
        }
        boolean column = parent.style().flex().direction() == Direction.COLUMN;
        if (flexItem && column && !parentDefinite) {
            if (hasPercentage(s.item().basis())) {
                throw indefinite(s.line(ITEM_BASIS), "Basis en un flex column");
            }
            if (s.item().grow() > 0) {
                throw error(s.line(ITEM_GROW), ErrorCode.GROW_WITHOUT_EFFECT, "Grow no tiene efecto: el flex "
                        + "column del padre no tiene un alto definido que repartir; declara su Height");
            }
        }
    }

    /** Porcentajes verticales del propio contenedor: filas y hueco entre filas. */
    static void validateContainer(BuildContext ctx, Style s, Box box) {
        if (ctx.isDefinite(box)) {
            return;
        }
        if (s.grid().rows().stream().anyMatch(ValueReader::hasPercentage)) {
            throw indefinite(s.line(GRID_ROWS), "Rows");
        }
        // En flex, el hueco entre filas solo cuenta en column o con varias líneas.
        boolean rowGapUsed = s.layout() == Layout.GRID
                || (s.layout() == Layout.FLEX && (s.flex().wrap() || s.flex().direction() == Direction.COLUMN));
        Value rowGap = s.layout() == Layout.GRID ? s.grid().rowGap() : s.flex().rowGap();
        if (rowGapUsed && hasPercentage(rowGap)) {
            String gap = s.layout() == Layout.GRID ? GRID_GAP : FLEX_GAP;
            throw indefinite(s.line(gap), "El hueco entre filas de Gap");
        }
    }

    /**
     * Si el alto de la caja de contenido es definido, sin maquetar: un {@code Height} que se
     * resuelve (puntos, o porcentaje de una base definida), {@code Aspect ratio} con alto
     * {@code auto}, un Absolute o un Fixed estirado entre arriba y abajo, o un ítem de grid
     * estirado ({@code Vertical: stretch}) en su área.
     */
    static boolean definiteHeight(BuildContext ctx, Style s, Box parent) {
        boolean gridItem = !s.isOutOfFlow() && parent.style().layout() == Layout.GRID;
        boolean base = s.isOutOfFlow() || gridItem || ctx.isDefinite(parent);
        Value h = s.box().height();
        if (!h.isAuto() && !h.isIntrinsic()) {
            return base || !hasPercentage(h);
        }
        if (h.isAuto() && s.box().aspectRatio() != null) {
            return true;
        }
        if (h.isAuto() && s.isOutOfFlow() && !s.offset().top().isAuto() && !s.offset().bottom().isAuto()) {
            return true;
        }
        Alignment vertical = s.item().vertical() != null ? s.item().vertical() : parent.style().grid().vertical();
        return h.isAuto() && gridItem && vertical == Alignment.STRETCH;
    }

    private static InvalidModelException indefinite(int line, String what) {
        return error(line, ErrorCode.INDEFINITE_PERCENTAGE, what + " usa un porcentaje de un alto que no es "
                + "definido: declara el Height del containing block, o usa puntos");
    }

    /** Un texto necesita {@code Font} y {@code Size}, propios o heredados; el error señala {@code source}. */
    static void font(InlineNode n, Node source, Box box) {
        if (box.style().typography().font() == null) {
            throw error(source, ErrorCode.TEXT_WITHOUT_FONT, "El texto necesita Font: declárala en el Typography del "
                    + n.getName() + ", de un antecesor o de Styles (Element: Document)");
        }
        if (!box.style().isDeclared(FONT_SIZE)) {
            throw error(source, ErrorCode.TEXT_WITHOUT_FONT_SIZE, "El texto necesita Size: decláralo en el Typography del "
                    + n.getName() + ", de un antecesor o de Styles (Element: Document)");
        }
    }
}
