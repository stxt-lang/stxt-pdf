package dev.stxt.boxes.layout.flex;

import dev.stxt.boxes.fragment.BreakRules;
import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Unit;
import dev.stxt.boxes.layout.BoxModel;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.LayoutContext;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.PendingAbsolute;
import dev.stxt.boxes.layout.Sizing;
import dev.stxt.boxes.layout.UnitBreaker;
import dev.stxt.boxes.layout.position.RelativeOffset;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static dev.stxt.boxes.model.Tolerance.fits;
import static dev.stxt.boxes.model.Tolerance.same;

/**
 * Layout {@code flex} (ESPECIFICACION.md §5.7). Los ocho pasos:
 * <ol>
 *   <li>Espacio disponible en el eje principal.</li>
 *   <li>Base de cada ítem: {@code Basis}, el tamaño declarado o el max-content.</li>
 *   <li>Tamaño hipotético: base limitada por el mínimo (con {@code Min width: auto}, el tamaño
 *       del contenido, sin pasar del declarado).</li>
 *   <li>Repartir en líneas si {@code Wrap}.</li>
 *   <li>Resolver tamaños flexibles con {@link FlexResolver}.</li>
 *   <li>Tamaño cruzado: layout de cada ítem con su tamaño principal; la línea mide el mayor.</li>
 *   <li>Estirar los ítems con {@code stretch} y repetir su layout.</li>
 *   <li>Alinear en el eje principal, en el cruzado y las líneas.</li>
 * </ol>
 *
 * <p>Los tamaños de {@link FlexItem} son de caja de margen; los márgenes {@code auto} valen 0.
 * En un contenedor de una sola línea (sin {@code Wrap}) con tamaño cruzado definido, la línea
 * mide lo que el contenedor. En {@code column}, los ítems con {@code stretch} de un contenedor
 * de una sola línea se miden ya con el ancho del contenedor; en el resto de casos, con su ajuste
 * al contenido.</p>
 *
 * <p>Los hijos {@code absolute} y {@code fixed} no son ítems: se devuelven como
 * {@link PendingAbsolute} con la esquina de la caja de contenido como posición estática. Los
 * ítems {@code relative} se desplazan con {@link RelativeOffset} después de alinearlos.</p>
 *
 * <p>Simplificación: en {@code column}, el mínimo automático de un ítem con {@code Height}
 * declarado es ese alto, no el de su contenido.</p>
 *
 * <p>Entre páginas (§5.10) se corta entre líneas en {@code row} y entre ítems en
 * {@code column}, con {@link UnitBreaker}; una línea (en {@code column}, un ítem) que no cabe en
 * una página se parte en paralelo. Un contenedor {@code column} con {@code Wrap} no se parte.</p>
 */
public final class FlexLayout implements LayoutAlgorithm {

    private final LayoutContext context;

    public FlexLayout(LayoutContext context) {
        this.context = context;
    }

    /** Tabla de ESPECIFICACION.md §5.9. */
    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        Style s = box.style();
        // Sin containing block, un hueco en porcentaje cuenta como 0.
        double gap = s.columnGap().resolve(OptionalDouble.empty()).orElse(0);
        List<Box> items = box.inFlowChildren();
        double maxMin = 0;
        double maxMax = 0;
        double sumMin = 0;
        double sumMax = 0;
        for (Box item : items) {
            IntrinsicSizes i = context.intrinsic(item);
            maxMin = Math.max(maxMin, i.minContent());
            maxMax = Math.max(maxMax, i.maxContent());
            sumMin += i.minContent();
            sumMax += i.maxContent();
        }
        double gaps = gap * Math.max(0, items.size() - 1);
        IntrinsicSizes content;
        if (s.flexDirection() == Style.Direction.COLUMN) {
            content = new IntrinsicSizes(maxMin, maxMax);
        } else if (s.flexWrap()) {
            content = new IntrinsicSizes(maxMin, sumMax + gaps);
        } else {
            content = new IntrinsicSizes(sumMin + gaps, sumMax + gaps);
        }
        return content;
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        BoxModel model = BoxModel.resolve(box, space, context);
        return new Run(box, model, space).layOut(from);
    }

    /** Un ítem y su estado durante el layout. */
    private static final class Item {
        final Box box;
        final Insets margin;
        final Style.Alignment align;
        FlexItem flex;
        /** En column: ancho de caja de borde con el que se mide el ítem. */
        double crossWidth;
        /** Tamaño principal final, de caja de margen. */
        double main;
        /** Tamaño cruzado, de caja de margen. */
        double cross;
        LayoutResult result;
        /** Espacio de {@link #result}. */
        ConstraintSpace space;

        Item(Box box, Insets margin, Style.Alignment align) {
            this.box = box;
            this.margin = margin;
            this.align = align;
        }
    }

    /** Tamaño cruzado de cada línea y lo que sobra en el eje cruzado (paso 6). */
    private record CrossSizes(double[] lines, double free) {
    }

    /** Lo que ocupa una unidad en el eje en que se corta, y sus ítems. */
    private record Span(double start, double end, List<Box> boxes) {
    }

    /** Layout de un contenedor concreto. */
    private final class Run {
        final Box box;
        final Style s;
        final BoxModel model;
        final boolean row;
        final boolean multiLine;
        final double contentWidth;
        final OptionalDouble contentHeight;
        final OptionalDouble mainSize;
        final OptionalDouble crossSize;
        final double mainGap;
        final double crossGap;
        final boolean atStart;
        final ConstraintSpace space;

        Run(Box box, BoxModel model, ConstraintSpace space) {
            this.box = box;
            this.s = box.style();
            this.model = model;
            this.row = s.flexDirection() == Style.Direction.ROW;
            this.multiLine = s.flexWrap();
            this.contentWidth = model.contentWidth();
            this.contentHeight = model.contentHeight();
            this.mainSize = row ? OptionalDouble.of(contentWidth) : contentHeight;
            this.crossSize = row ? contentHeight : OptionalDouble.of(contentWidth);
            // Base de los porcentajes: la caja de contenido en el eje de cada hueco (§3.6).
            double columnGap = s.columnGap().resolve(contentWidth).orElse(0);
            double rowGap = s.rowGap().resolve(contentHeight).orElse(0);
            this.mainGap = row ? columnGap : rowGap;
            this.crossGap = row ? rowGap : columnGap;
            this.atStart = space.atFragmentainerStart();
            this.space = space;
        }

        LayoutResult layOut(BreakToken from) {
            List<PendingAbsolute> pending = new ArrayList<>();
            List<Item> items = new ArrayList<>();
            for (Box child : box.children()) {
                if (child.style().isOutOfFlow()) {
                    pending.add(new PendingAbsolute(child, 0, 0));
                } else {
                    items.add(row ? rowItem(child) : columnItem(child));
                }
            }

            // Pasos 4 y 5: líneas y tamaños flexibles.
            List<List<Item>> lines = breakLines(items);
            for (List<Item> line : lines) {
                resolveMainSizes(line);
                line.forEach(it -> layOutItem(it, OptionalDouble.empty()));
            }

            CrossSizes cross = crossSizes(lines);
            stretchItems(lines, cross.lines());
            return UnitBreaker.fragment(context, box, space, from, arrange(lines, cross, pending));
        }

        /** Paso 6: tamaño cruzado de cada línea y lo que sobra en el eje cruzado. */
        CrossSizes crossSizes(List<List<Item>> lines) {
            int n = lines.size();
            double[] lineCross = new double[n];
            for (int l = 0; l < n; l++) {
                for (Item it : lines.get(l)) {
                    lineCross[l] = Math.max(lineCross[l], it.cross);
                }
            }
            if (!multiLine && n == 1 && crossSize.isPresent()) {
                lineCross[0] = crossSize.getAsDouble();
            }
            double crossFree = 0;
            if (multiLine && crossSize.isPresent()) {
                crossFree = crossSize.getAsDouble() - Sizing.sum(lineCross) - crossGap * Math.max(0, n - 1);
            }
            if (s.flexLines() == Style.Distribution.STRETCH && crossFree > 0) {
                for (int l = 0; l < n; l++) {
                    lineCross[l] += crossFree / n;
                }
                crossFree = 0;
            }
            return new CrossSizes(lineCross, crossFree);
        }

        /** Paso 7: estirar los ítems con {@code stretch} al tamaño cruzado de su línea. */
        void stretchItems(List<List<Item>> lines, double[] lineCross) {
            for (int l = 0; l < lines.size(); l++) {
                for (Item it : lines.get(l)) {
                    if (it.align == Style.Alignment.STRETCH) {
                        stretch(it, lineCross[l]);
                    }
                }
            }
        }

        /**
         * Paso 8: alinear. Las unidades de la fragmentación son las líneas en row y los ítems en
         * column (con Wrap, el contenedor entero).
         */
        UnitBreaker.Arranged arrange(List<List<Item>> lines, CrossSizes cross, List<PendingAbsolute> pending) {
            int n = lines.size();
            double[] lineCross = cross.lines();
            List<UnitBreaker.Placed> placed = new ArrayList<>();
            List<Span> spans = new ArrayList<>();
            double[] lineDistribution = distribute(multiLine ? s.flexLines() : Style.Distribution.START, cross.free(), n);
            double crossPos = lineDistribution[0];
            double mainExtent = 0;
            for (int l = 0; l < n; l++) {
                List<Item> line = lines.get(l);
                double used = mainGap * (line.size() - 1);
                for (Item it : line) {
                    used += it.main;
                }
                mainExtent = Math.max(mainExtent, used);
                double free = mainSize.isPresent() ? mainSize.getAsDouble() - used : 0;
                double[] d = distribute(s.flexMainAxis(), free, line.size());
                double mainPos = d[0];
                if (row) {
                    spans.add(new Span(crossPos, crossPos + lineCross[l], line.stream().map(it -> it.box).toList()));
                }
                for (Item it : line) {
                    double offset = Sizing.align(it.align, lineCross[l], it.cross);
                    int unit = row ? l : multiLine ? 0 : spans.size();
                    if (!row && !multiLine) {
                        spans.add(new Span(mainPos, mainPos + it.main, List.of(it.box)));
                    }
                    placed.add(place(it, mainPos, crossPos + offset, unit));
                    mainPos += it.main + mainGap + d[1];
                }
                crossPos += lineCross[l] + crossGap + lineDistribution[1];
            }

            double crossExtent = Sizing.sum(lineCross) + crossGap * Math.max(0, n - 1);
            double extent = row ? crossExtent : mainExtent;
            if (!row && multiLine && n > 0) {
                spans.add(new Span(0, extent, List.of()));
            }
            return new UnitBreaker.Arranged(model, placed, pending, units(spans, extent));
        }

        /** Las unidades, con la calidad del corte entre cada una y la anterior según sus ítems. */
        UnitBreaker.Units units(List<Span> spans, double extent) {
            List<Unit> units = new ArrayList<>();
            for (int u = 0; u < spans.size(); u++) {
                Span span = spans.get(u);
                if (u == 0) {
                    units.add(new Unit(span.start(), span.end(), 0, Style.Break.AUTO));
                    continue;
                }
                List<Box> before = spans.get(u - 1).boxes();
                units.add(new Unit(span.start(), span.end(), BreakRules.between(before, span.boxes()),
                        BreakRules.forcedBetween(before, span.boxes())));
            }
            return new UnitBreaker.Units(units, extent, row || !multiLine);
        }

        // --- Pasos 2 y 3: base, mínimo y máximo de cada ítem ------------------------------------

        Item rowItem(Box b) {
            Style is = b.style();
            Item it = new Item(b, Insets.resolve(is.margin(), contentWidth), alignment(is));
            double margins = it.margin.horizontal();
            BoxModel probe = BoxModel.resolve(b, probeSpace(), context);
            double extra = probe.border().horizontal() + probe.padding().horizontal();
            IntrinsicSizes intrinsic = context.intrinsic(b);

            OptionalDouble declared = is.width().isAuto() ? OptionalDouble.empty() : OptionalDouble.of(probe.width());
            OptionalDouble basis = declaredBasis(is);
            double base = basis.isPresent() ? basis.getAsDouble() + margins
                    : declared.isPresent() ? declared.getAsDouble() + margins
                    : intrinsic.maxContent();
            double max = is.maxWidth() == null ? Double.POSITIVE_INFINITY
                    : is.maxWidth().resolve(contentWidth).orElse(Double.POSITIVE_INFINITY) + margins;
            double min;
            if (is.minWidth().isAuto()) {
                // Mínimo automático (§5.7): el min-content de su contenido, sin pasar de Max width,
                // y nunca más que el ancho declarado.
                double content = Math.min(context.contentIntrinsic(b).minContent() + extra + margins, max);
                min = declared.isPresent() ? Math.min(content, declared.getAsDouble() + margins) : content;
            } else {
                min = is.minWidth().resolve(contentWidth).orElse(0) + margins;
            }
            it.flex = new FlexItem(base, is.itemGrow(), is.itemShrink(), min, max);
            return it;
        }

        Item columnItem(Box b) {
            Style is = b.style();
            Item it = new Item(b, Insets.resolve(is.margin(), contentWidth), alignment(is));
            double margins = it.margin.vertical();
            BoxModel probe = BoxModel.resolve(b, probeSpace(), context);

            // Ancho con el que se mide: el declarado, el del contenedor (stretch) o el ajuste al contenido.
            double width;
            if (!is.width().isAuto()) {
                width = probe.width();
            } else if (it.align == Style.Alignment.STRETCH && !multiLine) {
                width = Math.max(0, contentWidth - it.margin.horizontal());
            } else {
                width = Math.max(0, context.intrinsic(b).fitContent(contentWidth) - it.margin.horizontal());
            }
            it.crossWidth = BoxModel.constrainWidth(is, width, contentWidth);

            ConstraintSpace measure = itemSpace(it.crossWidth, OptionalDouble.empty());
            BoxModel sized = BoxModel.resolve(b, measure, context);
            double contentSize = context.layout(b, measure, null).fragment().height();

            OptionalDouble basis = declaredBasis(is);
            double base = basis.orElse(sized.height().orElse(contentSize)) + margins;
            double min;
            if (is.minHeight().isAuto()) {
                // Mínimo automático (§5.7): el alto de su contenido, sin pasar de Max height, y
                // nunca más que el alto declarado.
                double content = sized.height().isPresent()
                        ? context.layout(b, measure.measuringContentHeight(), null).fragment().height()
                        : contentSize;
                min = Math.min(contentSize, content) + margins;
            } else {
                min = is.minHeight().resolve(contentHeight).orElse(0) + margins;
            }
            double max = is.maxHeight() == null ? Double.POSITIVE_INFINITY
                    : is.maxHeight().resolve(contentHeight).orElse(Double.POSITIVE_INFINITY) + margins;
            it.flex = new FlexItem(base, is.itemGrow(), is.itemShrink(), min, max);
            return it;
        }

        /** {@code Basis} en caja de borde; vacío si es {@code auto} o un porcentaje de un tamaño indefinido. */
        OptionalDouble declaredBasis(Style is) {
            return is.itemBasis().resolve(mainSize);
        }

        Style.Alignment alignment(Style is) {
            return is.itemCrossAxis() != null ? is.itemCrossAxis() : s.flexCrossAxis();
        }

        // --- Pasos 4 a 7 ----------------------------------------------------------------------------

        List<List<Item>> breakLines(List<Item> items) {
            List<List<Item>> lines = new ArrayList<>();
            if (items.isEmpty()) {
                return lines;
            }
            if (!multiLine || mainSize.isEmpty()) {
                lines.add(items);
                return lines;
            }
            double limit = mainSize.getAsDouble();
            List<Item> current = new ArrayList<>();
            double used = 0;
            for (Item it : items) {
                double h = it.flex.hypothetical();
                if (!current.isEmpty() && !fits(used + mainGap + h, limit)) {
                    lines.add(current);
                    current = new ArrayList<>();
                    used = 0;
                }
                used += (current.isEmpty() ? 0 : mainGap) + h;
                current.add(it);
            }
            lines.add(current);
            return lines;
        }

        void resolveMainSizes(List<Item> line) {
            if (mainSize.isEmpty()) {
                // Sin tamaño principal definido no hay espacio libre: Grow no tiene efecto.
                line.forEach(it -> it.main = it.flex.hypothetical());
                return;
            }
            double available = mainSize.getAsDouble() - mainGap * (line.size() - 1);
            double[] sizes = FlexResolver.resolve(line.stream().map(it -> it.flex).toList(), available);
            for (int i = 0; i < sizes.length; i++) {
                line.get(i).main = sizes[i];
            }
        }

        /**
         * Layout del ítem con su tamaño principal final.
         *
         * @param cross tamaño cruzado impuesto (caja de borde), al estirar; vacío si no
         */
        void layOutItem(Item it, OptionalDouble cross) {
            if (row) {
                double width = Math.max(0, it.main - it.margin.horizontal());
                it.space = itemSpace(width, cross);
                it.result = context.layout(it.box, it.space, null);
                it.cross = it.result.fragment().height() + it.margin.vertical();
            } else {
                double height = Math.max(0, it.main - it.margin.vertical());
                double width = cross.orElse(it.crossWidth);
                it.space = itemSpace(width, OptionalDouble.of(height));
                it.result = context.layout(it.box, it.space, null);
                it.cross = it.result.fragment().width() + it.margin.horizontal();
            }
        }

        /** Un ítem sin tamaño cruzado declarado pasa a medir lo que la línea (paso 7). */
        void stretch(Item it, double lineCross) {
            Style is = it.box.style();
            double target;
            if (row) {
                Fragment f = it.result.fragment();
                BoxModel m = BoxModel.resolve(it.box, itemSpace(f.width(), OptionalDouble.empty()), context);
                if (m.height().isPresent()) {
                    return;
                }
                target = Sizing.constrain(Math.max(0, lineCross - it.margin.vertical()), m.minHeight(), m.maxHeight());
            } else {
                if (!is.width().isAuto()) {
                    return;
                }
                target = BoxModel.constrainWidth(is, Math.max(0, lineCross - it.margin.horizontal()), contentWidth);
            }
            if (!same(target + (row ? it.margin.vertical() : it.margin.horizontal()), it.cross)) {
                layOutItem(it, OptionalDouble.of(target));
            }
        }

        // --- Paso 8 ----------------------------------------------------------------------------------

        /** Coloca el ítem; {@code mainPos} y {@code crossPos} son de su caja de margen. */
        UnitBreaker.Placed place(Item it, double mainPos, double crossPos, int unit) {
            double x = (row ? mainPos : crossPos) + it.margin.left();
            double y = (row ? crossPos : mainPos) + it.margin.top();
            RelativeOffset offset = RelativeOffset.of(it.box.style(), contentWidth, contentHeight);
            return new UnitBreaker.Placed(it.box, it.result, it.space, x + offset.dx(), y + offset.dy(), unit,
                    offset);
        }

        /**
         * Espacio de un ítem con el ancho y, si lo hay, el alto impuestos. Los porcentajes de los
         * ítems se resuelven contra la caja de contenido del contenedor.
         */
        ConstraintSpace itemSpace(double width, OptionalDouble fixedHeight) {
            return ConstraintSpace.fixed(width, fixedHeight, contentWidth, contentHeight, atStart);
        }

        /** Espacio de la sonda que resuelve márgenes, borde, relleno y tamaños declarados del ítem. */
        ConstraintSpace probeSpace() {
            return ConstraintSpace.free(contentWidth, contentWidth, contentHeight, atStart);
        }
    }

    /**
     * Reparto del sobrante {@code free} entre {@code n} elementos (tabla de {@code Main axis},
     * §5.7). Si falta sitio, {@code between} se alinea al principio y {@code around} y
     * {@code evenly} se centran, como en CSS.
     *
     * @return {antes del primero, extra entre elementos}
     */
    static double[] distribute(Style.Distribution d, double free, int n) {
        if (n == 0) {
            return new double[]{0, 0};
        }
        if (free < 0) {
            d = switch (d) {
                case BETWEEN -> Style.Distribution.START;
                case AROUND, EVENLY -> Style.Distribution.CENTER;
                default -> d;
            };
        }
        return switch (d) {
            case START, STRETCH -> new double[]{0, 0};
            case END -> new double[]{free, 0};
            case CENTER -> new double[]{free / 2, 0};
            case BETWEEN -> new double[]{0, n > 1 ? free / (n - 1) : 0};
            case AROUND -> new double[]{free / (2 * n), free / n};
            case EVENLY -> new double[]{free / (n + 1), free / (n + 1)};
        };
    }
}
