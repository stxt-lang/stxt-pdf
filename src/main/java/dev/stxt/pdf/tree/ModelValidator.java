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
import java.util.Locale;

import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.label;
import static dev.stxt.pdf.tree.Nodes.position;
import static dev.stxt.pdf.tree.PropertyKey.ASPECT_RATIO;
import static dev.stxt.pdf.tree.PropertyKey.BREAK_AFTER;
import static dev.stxt.pdf.tree.PropertyKey.BREAK_BEFORE;
import static dev.stxt.pdf.tree.PropertyKey.KEEP_TOGETHER;
import static dev.stxt.pdf.tree.PropertyKey.KEEP_WITH_NEXT;
import static dev.stxt.pdf.tree.PropertyKey.KEEP_WITH_PREVIOUS;
import static dev.stxt.pdf.tree.PropertyKey.DECORATION;
import static dev.stxt.pdf.tree.PropertyKey.COLUMN_GAP;
import static dev.stxt.pdf.tree.PropertyKey.SEPARATOR;
import static dev.stxt.pdf.tree.PropertyKey.SEPARATOR_COLOR;
import static dev.stxt.pdf.tree.PropertyKey.STACK_GAP;
import static dev.stxt.pdf.tree.PropertyKey.STACK_LINE_DISTRIBUTION;
import static dev.stxt.pdf.tree.PropertyKey.FONT_SIZE;
import static dev.stxt.pdf.tree.PropertyKey.GRID_GAP;
import static dev.stxt.pdf.tree.PropertyKey.GRID_ROWS;
import static dev.stxt.pdf.tree.PropertyKey.HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.ITEM_BASE_SIZE;
import static dev.stxt.pdf.tree.PropertyKey.ITEM_GROW;
import static dev.stxt.pdf.tree.PropertyKey.ITEM_HORIZONTAL;
import static dev.stxt.pdf.tree.PropertyKey.ITEM_VERTICAL;
import static dev.stxt.pdf.tree.PropertyKey.MARGIN;
import static dev.stxt.pdf.tree.PropertyKey.MAX_HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.MAX_WIDTH;
import static dev.stxt.pdf.tree.PropertyKey.MIN_HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.MIN_WIDTH;
import static dev.stxt.pdf.tree.PropertyKey.OFFSET;
import static dev.stxt.pdf.tree.PropertyKey.ROTATE;
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

    /**
     * {@code Horizontal} y {@code Vertical}, {@code Line distribution} y {@code Gap} de un {@code Stack}: la del eje principal
     * reparte y no admite {@code stretch}; la del eje cruzado alinea y no admite {@code justify}, {@code around} ni
     * {@code distribute} (referencia/componentes/stack.md).
     */
    static void validateStack(Style s) {
        String main = s.stack().isHorizontal() ? "Horizontal" : "Vertical";
        String cross = s.stack().isHorizontal() ? "Vertical" : "Horizontal";
        String direction = "Direction: " + (s.stack().isHorizontal() ? "horizontal" : "vertical");
        if (s.stack().mainAxis() == Distribution.STRETCH) {
            throw error(s.line(s.stack().mainAxisKey()), ErrorCode.VALUE_NOT_ALLOWED, main + " es el eje principal de "
                    + "este Stack (" + direction + ") y reparte el espacio sobrante: no admite stretch. Lo que estira los "
                    + "ítems es su Grow");
        }
        String spread = crossSpread(s);
        if (spread != null) {
            throw error(s.line(s.stack().crossAxisKey()), ErrorCode.VALUE_NOT_ALLOWED, cross + " es el eje cruzado de "
                    + "este Stack (" + direction + ") y alinea los ítems: admite start, end, center o stretch y no "
                    + spread + ", que reparte el eje principal (" + main + ")");
        }
        if (s.isDeclared(STACK_LINE_DISTRIBUTION) && !s.stack().wrap()) {
            throw error(s.line(STACK_LINE_DISTRIBUTION), ErrorCode.LINE_DISTRIBUTION_WITHOUT_WRAP,
                    "Line distribution solo tiene efecto con Wrap: true, porque sin Wrap hay una sola línea");
        }
        // Sin Wrap hay una sola línea: no hay hueco entre filas en horizontal ni entre columnas en vertical.
        if (s.stack().gapPair() && !s.stack().wrap()) {
            String unused = s.stack().direction() == Direction.HORIZONTAL
                    ? "primero, el hueco entre filas," : "segundo, el hueco entre columnas,";
            throw error(s.line(STACK_GAP), ErrorCode.GAP_WITHOUT_WRAP, "Gap tiene dos valores, pero sin Wrap hay una sola"
                    + " línea y el " + unused + " no tiene efecto: va un solo valor, el hueco entre ítems");
        }
    }

    /**
     * Un {@code Columns} fija sus columnas con {@code Count}, {@code Min column width} o los dos; con una sola columna
     * no hay hueco ni línea entre columnas, {@code Separator color} necesita {@code Separator} y la línea cabe en el
     * hueco (referencia/componentes/columns.md).
     */
    static void validateColumns(Style s, int line) {
        if (s.columns().count() == null && s.columns().minColumnWidth() == null) {
            throw error(line, ErrorCode.COLUMNS_WITHOUT_COUNT, "Un Columns necesita Count, Min column width o los dos: "
                    + "Count fija el número de columnas y Min column width su ancho mínimo");
        }
        if (s.columns().count() != null && s.columns().count() == 1) {
            for (String p : List.of(COLUMN_GAP, SEPARATOR)) {
                if (s.isDeclared(p)) {
                    throw error(s.line(p), ErrorCode.SINGLE_COLUMN, (p.equals(SEPARATOR) ? "Separator" : "Gap")
                            + " no tiene efecto con Count: 1, porque no hay dos columnas que separar: sobra");
                }
            }
        }
        if (s.isDeclared(SEPARATOR_COLOR) && !s.isDeclared(SEPARATOR)) {
            throw error(s.line(SEPARATOR_COLOR), ErrorCode.SEPARATOR_COLOR_WITHOUT_SEPARATOR,
                    "Separator color no tiene efecto sin Separator, el grosor de la línea entre columnas: falta el "
                            + "grosor o sobra el color");
        }
        if (s.columns().separator() > s.columns().columnGap() + Tolerance.EPSILON) {
            String gap = s.isDeclared(COLUMN_GAP) ? "Gap (" + s.line(COLUMN_GAP) + ")"
                    : "Gap, que sin declarar es 0";
            throw error(s.line(SEPARATOR), ErrorCode.SEPARATOR_WIDTH, "Separator es más ancho que " + gap
                    + ": la línea se pintaría sobre las columnas. Hay que ensanchar Gap o adelgazar Separator");
        }
    }

    /**
     * Una {@code Image} no se parte (referencia/componentes/image.md): {@code Keep together} y {@code Edges}, que solo
     * cuentan al partir una caja, no tienen efecto (también si los da una regla de {@code Styles}).
     */
    static void validateImage(Style s) {
        for (String p : List.of(KEEP_TOGETHER, DECORATION)) {
            if (s.isDeclared(p)) {
                throw error(s.line(p), ErrorCode.BREAK_IN_IMAGE, (p.equals(KEEP_TOGETHER) ? "Keep together" : "Edges")
                        + " no tiene efecto en una Image, que no se parte entre páginas: sobra");
            }
        }
    }

    /**
     * El giro de una caja (referencia/comun/capas-y-giro.md): {@code Rotate origin} sin {@code Rotate} no tiene efecto,
     * y un {@code Frame} girado o una caja girada en flujo no se parten entre páginas, así que {@code Keep together} y
     * {@code Edges} tampoco (también si los da una regla de {@code Styles}). Una caja girada en flujo ya aísla los
     * márgenes: {@code Isolate margins} no tiene efecto en ella.
     */
    static void validateRotation(Style s) {
        if (s.rotation() == null) {
            if (s.isDeclared(ROTATE_ORIGIN)) {
                throw error(s.line(ROTATE_ORIGIN), ErrorCode.ROTATE_ORIGIN_WITHOUT_ROTATE, "Rotate origin no tiene "
                        + "efecto sin Rotate: falta el giro o sobra Rotate origin");
            }
            return;
        }
        if (s.position() != Position.FRAME && !s.rotatesInFlow()) {
            return;
        }
        String what = s.rotatesInFlow() ? "una caja girada" : "un Frame girado";
        for (String p : List.of(KEEP_TOGETHER, DECORATION)) {
            if (s.isDeclared(p)) {
                throw error(s.line(p), ErrorCode.BREAK_IN_ROTATED, (p.equals(KEEP_TOGETHER) ? "Keep together" : "Edges")
                        + " no tiene efecto en " + what + ", que no se parte entre páginas: sobra");
            }
        }
        if (s.rotatesInFlow() && s.isolateMargins()) {
            throw error(s.line("isolate-margins"), ErrorCode.ISOLATE_MARGINS_IN_ROTATED, "Isolate margins no tiene efecto en una caja girada, "
                    + "que ya corta la fusión de márgenes con sus hijos: sobra");
        }
    }

    /** Las reglas de una caja ya leída, antes de crearla: colocación, desplazamiento, márgenes, tamaños, porcentajes y cortes de columna. */
    static void validateBox(BuildContext ctx, Style s, Box parent) {
        Layout parentLayout = parent.style().layout();
        // Los hijos en flujo de un Stack o un Grid son ítems (con su Item o su Area), y las celdas
        // de una Table, ítems de su grid.
        boolean stackItem = !s.isOutOfFlow() && parentLayout == Layout.STACK;
        boolean gridItem = !s.isOutOfFlow() && parentLayout == Layout.GRID;
        if ((stackItem || gridItem) && s.rotatesInFlow()) {
            // Un ítem ocupa el sitio que le da su contenedor (referencia/comun/capas-y-giro.md).
            throw error(s.line(ROTATE), ErrorCode.ROTATED_ITEM, "Un ítem de " + (stackItem ? "Stack" : "Grid")
                    + " no puede girar: ocupa el sitio que le da su contenedor. Lo que gira es una caja dentro del ítem (un "
                    + "Block con la caja girada dentro)");
        }

        if (stackItem) {
            validateStackItem(s, parent);
        }
        validateOffset(s);
        validateMargins(s, parentLayout, stackItem, gridItem);
        if (s.isDeclared(ASPECT_RATIO) && !s.box().width().isAuto() && !s.box().height().isAuto()) {
            throw error(s.line(ASPECT_RATIO), ErrorCode.ASPECT_RATIO_CONFLICT,
                    "Aspect ratio no tiene efecto si Width y Height están declarados");
        }
        validateMinMax(s, WIDTH, s.box().width(), MIN_WIDTH, s.box().minWidth(), MAX_WIDTH, s.box().maxWidth());
        validateMinMax(s, HEIGHT, s.box().height(), MIN_HEIGHT, s.box().minHeight(), MAX_HEIGHT, s.box().maxHeight());
        validatePercentages(ctx, s, parent, stackItem, gridItem);
        validateColumnBreaks(ctx, s);
        validateKeeps(s);
    }

    /**
     * El valor del eje cruzado de un {@code Stack} si es un reparto ({@code justify}…), que no vale ahí; si no, {@code
     * null}.
     */
    private static String crossSpread(Style s) {
        Distribution v = s.stack().crossDeclared();
        return v == Distribution.JUSTIFY || v == Distribution.AROUND || v == Distribution.DISTRIBUTE
                ? v.name().toLowerCase(Locale.ROOT) : null;
    }

    /**
     * En un ítem de {@code Stack}, {@code Horizontal} o {@code Vertical} solo en el eje cruzado: en el principal reparte
     * el {@code Stack}, con su propia propiedad, y el ítem crece o se encoge con {@code Grow} y {@code Shrink}
     * (referencia/componentes/stack.md).
     */
    private static void validateStackItem(Style s, Box parent) {
        boolean horizontal = parent.style().stack().isHorizontal();
        String key = horizontal ? ITEM_HORIZONTAL : ITEM_VERTICAL;
        if (s.isDeclared(key)) {
            String main = horizontal ? "Horizontal" : "Vertical";
            String cross = horizontal ? "Vertical" : "Horizontal";
            throw error(s.line(key), ErrorCode.ITEM_MAIN_AXIS, main + " no tiene efecto en un ítem de un Stack con "
                    + "Direction: " + main.toLowerCase(Locale.ROOT) + ", porque es su eje principal: el reparto "
                    + "lo dan el " + main + " del Stack y el tamaño, Grow y Shrink, y el ítem se alinea con " + cross);
        }
    }

    /** {@code Before} y {@code After: column} solo dentro de un {@code Columns}, que es quien tiene columnas. */
    private static void validateColumnBreaks(BuildContext ctx, Style s) {
        if (ctx.insideColumns) {
            return;
        }
        String property = s.breaks().before() == Break.COLUMN ? BREAK_BEFORE
                : s.breaks().after() == Break.COLUMN ? BREAK_AFTER : null;
        if (property != null) {
            throw error(s.line(property), ErrorCode.COLUMN_BREAK_OUTSIDE_COLUMNS, (property.equals(BREAK_BEFORE)
                    ? "Before" : "After") + ": column solo tiene efecto dentro de un Columns: para "
                    + "cortar la página se usa page");
        }
    }

    /**
     * {@code Keep with next} con un salto después de la caja, o {@code Keep with previous} con uno antes: la caja no
     * puede ir con la otra y separarse de ella a la vez (referencia/comun/cortes.md).
     */
    private static void validateKeeps(Style s) {
        BreakStyle b = s.breaks();
        if (b.keepWithNext() && b.after() != Break.AUTO) {
            throw error(s.line(KEEP_WITH_NEXT), ErrorCode.KEEP_CONFLICT, "Keep with next mantiene la caja con la "
                    + "siguiente y After: " + value(b.after()) + " la separa de ella: sobra uno de los dos");
        }
        if (b.keepWithPrevious() && b.before() != Break.AUTO) {
            throw error(s.line(KEEP_WITH_PREVIOUS), ErrorCode.KEEP_CONFLICT, "Keep with previous mantiene la caja con "
                    + "la anterior y Before: " + value(b.before()) + " la separa de ella: sobra uno de los dos");
        }
    }

    private static String value(Break b) {
        return b.name().toLowerCase(Locale.ROOT);
    }

    private static void validateOffset(Style s) {
        if (!s.isDeclared(OFFSET)) {
            return;
        }
        SourceLine line = s.line(OFFSET);
        Sides<Value> o = s.offset();
        if (s.position() == Position.FRAME) {
            if (!o.top().isAuto() && !o.bottom().isAuto()) {
                throw error(line, ErrorCode.OFFSET_CONFLICT, "Un Frame no puede desplazarse a la vez "
                        + "desde arriba y desde abajo: uno de los dos tiene que quedar en auto");
            }
            if (!o.left().isAuto() && !o.right().isAuto()) {
                throw error(line, ErrorCode.OFFSET_CONFLICT, "Un Frame no puede desplazarse a la vez "
                        + "desde la izquierda y desde la derecha: uno de los dos tiene que quedar en auto");
            }
            return;
        }
        // Overlay y Page overlay: inicio + tamaño + fin no puede fijar la posición dos veces.
        if (!o.left().isAuto() && !s.box().width().isAuto() && !o.right().isAuto()
                && !s.box().margin().left().isAuto() && !s.box().margin().right().isAuto()) {
            throw error(line, ErrorCode.OVERCONSTRAINED, "Izquierda, Width y derecha declarados fijan la "
                    + "posición dos veces: uno tiene que quedar en auto, o centrar con Margin auto");
        }
        if (!o.top().isAuto() && !s.box().height().isAuto() && !o.bottom().isAuto()
                && !s.box().margin().top().isAuto() && !s.box().margin().bottom().isAuto()) {
            throw error(line, ErrorCode.OVERCONSTRAINED, "Arriba, Height y abajo declarados fijan la "
                    + "posición dos veces: uno tiene que quedar en auto, o centrar con Margin auto");
        }
    }

    /**
     * Los márgenes {@code auto} solo centran: en flujo de bloque, en horizontal y con ancho limitado; en Overlay y Page
     * overlay, con inicio, tamaño y fin.
     */
    private static void validateMargins(Style s, Layout parentLayout, boolean stackItem, boolean gridItem) {
        Sides<Value> m = s.box().margin();
        boolean horizontalAuto = m.left().isAuto() || m.right().isAuto();
        boolean verticalAuto = m.top().isAuto() || m.bottom().isAuto();
        if (!horizontalAuto && !verticalAuto) {
            return;
        }
        SourceLine line = s.line(MARGIN);
        if (stackItem || gridItem) {
            throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto no tiene efecto en ítems de Stack o "
                    + "Grid: se alinean con Horizontal y Vertical");
        }
        if (s.isOutOfFlow()) {
            validateOutOfFlowMargins(s, line, horizontalAuto, verticalAuto);
            return;
        }
        if (parentLayout == Layout.BLOCK) {
            if (verticalAuto) {
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT,
                        "Margin auto en vertical no tiene efecto en flujo de bloque: va un número");
            }
            // Una imagen con Width auto mide su ancho natural, y una caja girada en flujo, su contenido: no todo el
            // ancho, así que se pueden centrar.
            if (s.box().width().isAuto() && s.box().maxWidth() == null && s.layout() != Layout.IMAGE
                    && !s.rotatesInFlow()) {
                throw error(line, ErrorCode.MARGIN_AUTO_WITHOUT_EFFECT, "Margin auto no centra una caja que ocupa "
                        + "todo el ancho: le falta Width o Max width");
            }
        }
    }

    /**
     * Un {@code Overlay} o un {@code Page overlay} solo se centra con {@code Margin auto} si declara inicio, tamaño y
     * fin.
     */
    private static void validateOutOfFlowMargins(Style s, SourceLine line, boolean horizontalAuto,
                                                 boolean verticalAuto) {
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
                    label(min) + " es mayor que " + label(max) + " (" + s.line(max) + ")");
        }
        if (comparable(value, minValue) && amount(value) < amount(minValue)) {
            throw error(s.line(size), ErrorCode.MIN_MAX_CONFLICT,
                    label(size) + " es menor que " + label(min) + " (" + s.line(min) + ")");
        }
        if (maxValue != null && comparable(value, maxValue) && amount(value) > amount(maxValue)) {
            throw error(s.line(size), ErrorCode.MIN_MAX_CONFLICT,
                    label(size) + " es mayor que " + label(max) + " (" + s.line(max) + ")");
        }
    }

    /**
     * Porcentajes verticales de la caja: su base debe ser un alto definido (referencia/comun/valores.md). La base de
     * {@code Height}, {@code Min height} y {@code Max height} es el containing block (siempre
     * definido para Overlay y Page overlay) o el área (los ítems de grid); la de los desplazamientos
     * de un Frame y de {@code Base size} en un stack {@code column}, la caja de contenido del padre.
     */
    private static void validatePercentages(BuildContext ctx, Style s, Box parent, boolean stackItem,
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
        if (s.position() == Position.FRAME && !parentDefinite
                && (hasPercentage(s.offset().top()) || hasPercentage(s.offset().bottom()))) {
            throw indefinite(s.line(OFFSET), "El desplazamiento vertical de Offset");
        }
        boolean column = parent.style().stack().direction() == Direction.VERTICAL;
        if (stackItem && column && !parentDefinite) {
            if (hasPercentage(s.item().baseSize())) {
                throw indefinite(s.line(ITEM_BASE_SIZE), "Base size en un Stack vertical");
            }
            if (s.item().grow() > 0) {
                throw error(s.line(ITEM_GROW), ErrorCode.GROW_WITHOUT_EFFECT, "Grow no tiene efecto: el Stack "
                        + "vertical del padre no tiene un alto definido que repartir: le falta su Height");
            }
        }
    }

    /** Porcentajes verticales del propio contenedor: filas y hueco entre filas. */
    static void validateContainer(BuildContext ctx, Style s, Box box) {
        if (ctx.isDefinite(box)) {
            return;
        }
        if (s.grid().rowSizes().stream().anyMatch(ValueReader::hasPercentage)) {
            throw indefinite(s.line(GRID_ROWS), "Row sizes");
        }
        // En stack, el hueco entre filas solo cuenta en vertical o con varias líneas.
        boolean rowGapUsed = s.layout() == Layout.GRID
                || (s.layout() == Layout.STACK && (s.stack().wrap() || s.stack().direction() == Direction.VERTICAL));
        Value rowGap = s.layout() == Layout.GRID ? s.grid().rowGap() : s.stack().rowGap();
        if (rowGapUsed && hasPercentage(rowGap)) {
            String gap = s.layout() == Layout.GRID ? GRID_GAP : STACK_GAP;
            throw indefinite(s.line(gap), "El hueco entre filas de Gap");
        }
    }

    /**
     * Si el alto de la caja de contenido es definido, sin maquetar: un {@code Height} que se
     * resuelve (puntos, o porcentaje de una base definida), {@code Aspect ratio} con alto
     * {@code auto}, un Overlay o un Page overlay estirado entre arriba y abajo, o un ítem de grid
     * estirado ({@code Vertical: stretch}) en su área.
     */
    static boolean definiteHeight(BuildContext ctx, Style s, Box parent) {
        boolean gridItem = !s.isOutOfFlow() && parent.style().layout() == Layout.GRID;
        boolean base = s.isOutOfFlow() || gridItem || ctx.isDefinite(parent);
        Value h = s.box().height();
        if (!h.isAuto()) {
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

    private static InvalidModelException indefinite(SourceLine line, String what) {
        return error(line, ErrorCode.INDEFINITE_PERCENTAGE, what + " usa un porcentaje de un alto que no es "
                + "definido: hace falta el Height de su contenedor de referencia, o una medida en puntos");
    }

    /** Un texto necesita {@code Font} y {@code Size}, propios o heredados; el error señala {@code source}. */
    static void font(InlineNode n, Node source, Box box) {
        if (box.style().typography().font() == null) {
            throw error(source, ErrorCode.TEXT_WITHOUT_FONT, "El texto necesita Font: se declara en el Typography del "
                    + n.getName() + ", de un antecesor o de Styles (For element: Document)");
        }
        if (!box.style().isDeclared(FONT_SIZE)) {
            throw error(source, ErrorCode.TEXT_WITHOUT_FONT_SIZE, "El texto necesita Size: se declara en el Typography del "
                    + n.getName() + ", de un antecesor o de Styles (For element: Document)");
        }
    }
}
