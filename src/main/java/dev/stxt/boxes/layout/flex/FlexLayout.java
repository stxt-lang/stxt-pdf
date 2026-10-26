package dev.stxt.boxes.layout.flex;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.layout.AdjoiningMargins;
import dev.stxt.boxes.layout.BoxModel;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.PendingAbsolute;
import dev.stxt.boxes.layout.Sizing;
import dev.stxt.boxes.layout.position.RelativeOffset;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Layout {@code flex} (ESPECIFICACION.md §5.7). Los ocho pasos:
 * <ol>
 *   <li>Espacio disponible en el eje principal.</li>
 *   <li>Base de cada ítem: {@code Basis}, el tamaño declarado o el max-content.</li>
 *   <li>Tamaño hipotético: base limitada por el mínimo (min-content si {@code Min width: auto}).</li>
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
 */
public final class FlexLayout implements LayoutAlgorithm {

    private final Engine engine;

    public FlexLayout(Engine engine) {
        this.engine = engine;
    }

    /** Tabla de ESPECIFICACION.md §5.9. */
    @Override
    public IntrinsicSizes intrinsic(Box box) {
        Style s = box.style;
        // Sin containing block, un hueco en porcentaje cuenta como 0.
        double gap = s.columnGap.resolve(OptionalDouble.empty()).orElse(0);
        List<Box> items = box.inFlowChildren();
        double maxMin = 0;
        double maxMax = 0;
        double sumMin = 0;
        double sumMax = 0;
        for (Box item : items) {
            IntrinsicSizes i = engine.intrinsic(item);
            maxMin = Math.max(maxMin, i.minContent());
            maxMax = Math.max(maxMax, i.maxContent());
            sumMin += i.minContent();
            sumMax += i.maxContent();
        }
        double gaps = gap * Math.max(0, items.size() - 1);
        IntrinsicSizes content;
        if (s.flexDirection == Style.Direction.COLUMN) {
            content = new IntrinsicSizes(maxMin, maxMax);
        } else if (s.flexWrap) {
            content = new IntrinsicSizes(maxMin, sumMax + gaps);
        } else {
            content = new IntrinsicSizes(sumMin + gaps, sumMax + gaps);
        }
        return BoxModel.intrinsic(box, content);
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        // TODO(hito 6): reanudar desde from (BreakToken.Container) y cortar entre líneas en row
        //   o entre ítems en column (§5.10).
        BoxModel model = BoxModel.resolve(box, space, () -> engine.intrinsic(box));
        model.applyTo(box);
        return new Run(box, model, space).layOut();
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

        Item(Box box, Insets margin, Style.Alignment align) {
            this.box = box;
            this.margin = margin;
            this.align = align;
        }
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

        Run(Box box, BoxModel model, ConstraintSpace space) {
            this.box = box;
            this.s = box.style;
            this.model = model;
            this.row = s.flexDirection == Style.Direction.ROW;
            this.multiLine = s.flexWrap;
            this.contentWidth = model.contentWidth();
            this.contentHeight = model.contentHeight();
            this.mainSize = row ? OptionalDouble.of(contentWidth) : contentHeight;
            this.crossSize = row ? contentHeight : OptionalDouble.of(contentWidth);
            // Base de los porcentajes: la caja de contenido en el eje de cada hueco (§3.6).
            double columnGap = s.columnGap.resolve(contentWidth).orElse(0);
            double rowGap = s.rowGap.resolve(contentHeight).orElse(0);
            this.mainGap = row ? columnGap : rowGap;
            this.crossGap = row ? rowGap : columnGap;
            this.atStart = space.atFragmentainerStart();
        }

        LayoutResult layOut() {
            List<PendingAbsolute> pending = new ArrayList<>();
            List<Item> items = new ArrayList<>();
            for (Box child : box.children) {
                if (child.style.isOutOfFlow()) {
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

            // Paso 6: tamaño cruzado de cada línea.
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
                crossFree = crossSize.getAsDouble() - sum(lineCross) - crossGap * Math.max(0, n - 1);
            }
            if (s.flexLines == Style.Distribution.STRETCH && crossFree > 0) {
                for (int l = 0; l < n; l++) {
                    lineCross[l] += crossFree / n;
                }
                crossFree = 0;
            }

            // Paso 7: estirar.
            for (int l = 0; l < n; l++) {
                for (Item it : lines.get(l)) {
                    if (it.align == Style.Alignment.STRETCH) {
                        stretch(it, lineCross[l]);
                    }
                }
            }

            // Paso 8: alinear.
            List<Fragment> fragments = new ArrayList<>();
            double[] lineDistribution = distribute(multiLine ? s.flexLines : Style.Distribution.START, crossFree, n);
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
                double[] d = distribute(s.flexMainAxis, free, line.size());
                double mainPos = d[0];
                for (Item it : line) {
                    double offset = switch (it.align) {
                        case START, STRETCH -> 0;
                        case END -> lineCross[l] - it.cross;
                        case CENTER -> (lineCross[l] - it.cross) / 2;
                    };
                    place(it, mainPos, crossPos + offset, fragments, pending);
                    mainPos += it.main + mainGap + d[1];
                }
                crossPos += lineCross[l] + crossGap + lineDistribution[1];
            }

            double crossExtent = sum(lineCross) + crossGap * Math.max(0, n - 1);
            double height = model.usedHeight(row ? crossExtent : mainExtent);
            box.height = height;
            Fragment fragment = Fragment.complete(box, 0, 0, model.width(), height, fragments, List.of());
            // Los márgenes de los ítems nunca colapsan con los del contenedor (§5.3).
            return LayoutResult.finished(fragment, pending, AdjoiningMargins.NONE);
        }

        // --- Pasos 2 y 3: base, mínimo y máximo de cada ítem ------------------------------------

        Item rowItem(Box b) {
            Style is = b.style;
            Item it = new Item(b, Insets.resolve(is.margin, contentWidth), alignment(is));
            double margins = it.margin.horizontal();
            BoxModel probe = BoxModel.resolve(b, itemSpace(contentWidth, false, OptionalDouble.empty()),
                    () -> engine.intrinsic(b));
            double extra = probe.border().horizontal() + probe.padding().horizontal();
            IntrinsicSizes intrinsic = engine.intrinsic(b);

            OptionalDouble declared = is.width.isAuto() ? OptionalDouble.empty() : OptionalDouble.of(probe.width());
            OptionalDouble basis = declaredBasis(is, extra);
            double base = basis.isPresent() ? basis.getAsDouble() + margins
                    : declared.isPresent() ? declared.getAsDouble() + margins
                    : intrinsic.maxContent();
            double min;
            if (is.minWidth.isAuto()) {
                // Un ítem no encoge por debajo de su min-content (ni de su ancho declarado, si es menor).
                min = declared.isPresent()
                        ? Math.min(intrinsic.minContent(), declared.getAsDouble() + margins)
                        : intrinsic.minContent();
            } else {
                min = borderBox(is.minWidth.resolve(contentWidth), is, extra).orElse(0) + margins;
            }
            double max = is.maxWidth == null ? Double.POSITIVE_INFINITY
                    : borderBox(is.maxWidth.resolve(contentWidth), is, extra).orElse(Double.POSITIVE_INFINITY) + margins;
            it.flex = new FlexItem(base, is.itemGrow, is.itemShrink, min, max);
            return it;
        }

        Item columnItem(Box b) {
            Style is = b.style;
            Item it = new Item(b, Insets.resolve(is.margin, contentWidth), alignment(is));
            double margins = it.margin.vertical();
            BoxModel probe = BoxModel.resolve(b, itemSpace(contentWidth, false, OptionalDouble.empty()),
                    () -> engine.intrinsic(b));
            double extraWidth = probe.border().horizontal() + probe.padding().horizontal();

            // Ancho con el que se mide: el declarado, el del contenedor (stretch) o el ajuste al contenido.
            double width;
            if (!is.width.isAuto()) {
                width = probe.width();
            } else if (it.align == Style.Alignment.STRETCH && !multiLine) {
                width = Math.max(0, contentWidth - it.margin.horizontal());
            } else {
                width = Math.max(0, engine.intrinsic(b).fitContent(contentWidth) - it.margin.horizontal());
            }
            it.crossWidth = BoxModel.constrainWidth(is, width, contentWidth, extraWidth);

            ConstraintSpace measure = itemSpace(it.crossWidth, true, OptionalDouble.empty());
            BoxModel sized = BoxModel.resolve(b, measure, () -> engine.intrinsic(b));
            double extra = sized.border().vertical() + sized.padding().vertical();
            double contentSize = engine.layout(b, measure, null).fragment().height();

            OptionalDouble basis = declaredBasis(is, extra);
            double base = basis.orElse(sized.height().orElse(contentSize)) + margins;
            double min = is.minHeight.isAuto() ? contentSize + margins
                    : borderBox(is.minHeight.resolve(contentHeight), is, extra).orElse(0) + margins;
            double max = is.maxHeight == null ? Double.POSITIVE_INFINITY
                    : borderBox(is.maxHeight.resolve(contentHeight), is, extra).orElse(Double.POSITIVE_INFINITY) + margins;
            it.flex = new FlexItem(base, is.itemGrow, is.itemShrink, min, max);
            return it;
        }

        /** {@code Basis} en caja de borde; vacío si es {@code auto} o un porcentaje de un tamaño indefinido. */
        OptionalDouble declaredBasis(Style is, double extra) {
            return borderBox(is.itemBasis.resolve(mainSize), is, extra);
        }

        Style.Alignment alignment(Style is) {
            return is.itemCrossAxis != null ? is.itemCrossAxis : s.flexCrossAxis;
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
                if (!current.isEmpty() && used + mainGap + h > limit + 1e-9) {
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
                it.result = engine.layout(it.box, itemSpace(width, true, cross), null);
                it.cross = it.result.fragment().height() + it.margin.vertical();
            } else {
                double height = Math.max(0, it.main - it.margin.vertical());
                double width = cross.orElse(it.crossWidth);
                it.result = engine.layout(it.box, itemSpace(width, true, OptionalDouble.of(height)), null);
                it.cross = it.result.fragment().width() + it.margin.horizontal();
            }
        }

        /** Un ítem sin tamaño cruzado declarado pasa a medir lo que la línea (paso 7). */
        void stretch(Item it, double lineCross) {
            Style is = it.box.style;
            double target;
            if (row) {
                Fragment f = it.result.fragment();
                BoxModel m = BoxModel.resolve(it.box, itemSpace(f.width(), true, OptionalDouble.empty()),
                        () -> engine.intrinsic(it.box));
                if (m.height().isPresent()) {
                    return;
                }
                target = Sizing.constrain(Math.max(0, lineCross - it.margin.vertical()), m.minHeight(), m.maxHeight());
            } else {
                if (!is.width.isAuto()) {
                    return;
                }
                Insets border = Insets.resolve(is.border, contentWidth);
                Insets padding = Insets.resolve(is.padding, contentWidth);
                target = BoxModel.constrainWidth(is, Math.max(0, lineCross - it.margin.horizontal()),
                        contentWidth, border.horizontal() + padding.horizontal());
            }
            if (Math.abs(target + (row ? it.margin.vertical() : it.margin.horizontal()) - it.cross) > 1e-9) {
                layOutItem(it, OptionalDouble.of(target));
            }
        }

        // --- Paso 8 ----------------------------------------------------------------------------------

        /** Coloca el ítem; {@code mainPos} y {@code crossPos} son de su caja de margen. */
        void place(Item it, double mainPos, double crossPos, List<Fragment> fragments, List<PendingAbsolute> pending) {
            Fragment f = it.result.fragment();
            double x = (row ? mainPos : crossPos) + it.margin.left();
            double y = (row ? crossPos : mainPos) + it.margin.top();
            RelativeOffset offset = RelativeOffset.of(it.box.style, contentWidth, contentHeight);
            x += offset.dx();
            y += offset.dy();
            it.box.x = x;
            it.box.y = y;
            fragments.add(f.moved(x, y));
            Insets padding = Insets.resolve(it.box.style.padding, contentWidth);
            double dx = x + f.border().left() + padding.left();
            double dy = y + f.border().top() + padding.top();
            it.result.pending().forEach(p -> pending.add(p.translated(dx, dy)));
        }

        /** Los porcentajes de los ítems se resuelven contra la caja de contenido del contenedor. */
        ConstraintSpace itemSpace(double width, boolean fixedWidth, OptionalDouble fixedHeight) {
            return new ConstraintSpace(width, fixedHeight, fixedWidth, fixedHeight.isPresent(),
                    contentWidth, contentHeight, OptionalDouble.empty(), atStart);
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

    private static OptionalDouble borderBox(OptionalDouble declared, Style s, double extra) {
        return declared.isPresent()
                ? OptionalDouble.of(Sizing.toBorderBox(declared.getAsDouble(), s.boxSizing, extra))
                : declared;
    }

    private static double sum(double[] v) {
        double r = 0;
        for (double x : v) {
            r += x;
        }
        return r;
    }
}
