package dev.stxt.pdf.layout.stack;

import dev.stxt.pdf.fragment.BreakRules;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Unit;
import dev.stxt.pdf.layout.BoxModel;
import dev.stxt.pdf.layout.ConstraintSpace;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.LayoutAlgorithm;
import dev.stxt.pdf.layout.LayoutContext;
import dev.stxt.pdf.layout.LayoutResult;
import dev.stxt.pdf.layout.PendingOverlay;
import dev.stxt.pdf.layout.Sizing;
import dev.stxt.pdf.layout.UnitBreaker;
import dev.stxt.pdf.layout.position.FrameOffset;
import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Direction;
import dev.stxt.pdf.model.Distribution;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static dev.stxt.pdf.model.Tolerance.fits;
import static dev.stxt.pdf.model.Tolerance.same;

/**
 * Layout {@code stack} (reference/components/stack.md). The eight steps:
 * <ol>
 *   <li>Available space on the main axis.</li>
 *   <li>Base of each item: {@code Base size}, the declared size or the natural width.</li>
 *   <li>Hypothetical size: base limited by the minimum (with {@code Min width: auto}, the size
 *       of the content, not exceeding the declared one).</li>
 *   <li>Break into lines if {@code Wrap}.</li>
 *   <li>Resolve flexible sizes with {@link StackResolver}.</li>
 *   <li>Cross size: layout of each item with its main size; the line measures the largest.</li>
 *   <li>Stretch the items with {@code stretch} and repeat their layout.</li>
 *   <li>Align on the main axis, on the cross axis and the lines.</li>
 * </ol>
 *
 * <p>The sizes of {@link StackItem} are margin box sizes; the {@code auto} margins are 0.
 * In a single-line container (without {@code Wrap}) with a definite cross size, the line
 * measures the same as the container. In vertical, the items with {@code stretch} of a single-line
 * container are measured already at the width of the container; in all other cases, with their fit
 * to the content.</p>
 *
 * <p>The {@code Overlay} and {@code Page overlay} children are not items: they are returned as
 * {@link PendingOverlay} with the corner of the content box as static position. The
 * {@code Frame} items are shifted with {@link FrameOffset} after being aligned.</p>
 *
 * <p>Simplification: in vertical, the automatic minimum of an item with a declared {@code Height}
 * is that height, not the height of its content.</p>
 *
 * <p>Across pages (reference/common/breaks.md) the break goes between lines in horizontal and between items in
 * vertical, with {@link UnitBreaker}; a line (in vertical, an item) that does not fit on
 * a page is split in parallel. A vertical container with {@code Wrap} is not split.</p>
 */
public final class StackLayout implements LayoutAlgorithm {

    private final LayoutContext context;

    public StackLayout(LayoutContext context) {
        this.context = context;
    }

    /** Table of reference/common/box.md. */
    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        Style s = box.style();
        // Without a reference container, a percentage gap counts as 0.
        double gap = s.stack().columnGap().resolve(OptionalDouble.empty()).orElse(0);
        List<Box> items = box.inFlowChildren();
        double maxMin = 0;
        double maxMax = 0;
        double sumMin = 0;
        double sumMax = 0;
        for (Box item : items) {
            IntrinsicSizes i = context.intrinsic(item);
            maxMin = Math.max(maxMin, i.narrowest());
            maxMax = Math.max(maxMax, i.natural());
            sumMin += i.narrowest();
            sumMax += i.natural();
        }
        double gaps = gap * Math.max(0, items.size() - 1);
        IntrinsicSizes content;
        if (s.stack().direction() == Direction.VERTICAL) {
            content = new IntrinsicSizes(maxMin, maxMax);
        } else if (s.stack().wrap()) {
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

    /** An item and its state during the layout. */
    private static final class Item {
        final Box box;
        final Insets margin;
        final Alignment align;
        StackItem stack;
        /** In vertical: border box width the item is measured with. */
        double crossWidth;
        /** Final main size, of the margin box. */
        double main;
        /** Cross size, of the margin box. */
        double cross;
        LayoutResult result;
        /** Space of {@link #result}. */
        ConstraintSpace space;

        Item(Box box, Insets margin, Alignment align) {
            this.box = box;
            this.margin = margin;
            this.align = align;
        }
    }

    /** Cross size of each line and the space left over on the cross axis (step 6). */
    private record CrossSizes(double[] lines, double free) {
    }

    /** What a unit occupies on the axis of the break, and its items. */
    private record Span(double start, double end, List<Box> boxes) {
    }

    /** Layout of a specific container. */
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
            this.row = s.stack().direction() == Direction.HORIZONTAL;
            this.multiLine = s.stack().wrap();
            this.contentWidth = model.contentWidth();
            this.contentHeight = model.contentHeight();
            this.mainSize = row ? OptionalDouble.of(contentWidth) : contentHeight;
            this.crossSize = row ? contentHeight : OptionalDouble.of(contentWidth);
            // Base of the percentages: the content box on the axis of each gap (reference/common/values.md).
            double columnGap = s.stack().columnGap().resolve(contentWidth).orElse(0);
            double rowGap = s.stack().rowGap().resolve(contentHeight).orElse(0);
            this.mainGap = row ? columnGap : rowGap;
            this.crossGap = row ? rowGap : columnGap;
            this.atStart = space.atFragmentainerStart();
            this.space = space;
        }

        LayoutResult layOut(BreakToken from) {
            List<PendingOverlay> pending = new ArrayList<>();
            List<Item> items = new ArrayList<>();
            for (Box child : box.children()) {
                if (child.style().isOutOfFlow()) {
                    pending.add(new PendingOverlay(child, 0, 0));
                } else {
                    items.add(row ? rowItem(child) : columnItem(child));
                }
            }

            // Steps 4 and 5: lines and flexible sizes.
            List<List<Item>> lines = breakLines(items);
            for (List<Item> line : lines) {
                resolveMainSizes(line);
                line.forEach(it -> layOutItem(it, OptionalDouble.empty()));
            }

            CrossSizes cross = crossSizes(lines);
            stretchItems(lines, cross.lines());
            return UnitBreaker.fragment(context, box, space, from, arrange(lines, cross, pending));
        }

        /** Step 6: cross size of each line and the space left over on the cross axis. */
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
            if (s.stack().lineDistribution() == Distribution.STRETCH && crossFree > 0) {
                for (int l = 0; l < n; l++) {
                    lineCross[l] += crossFree / n;
                }
                crossFree = 0;
            }
            return new CrossSizes(lineCross, crossFree);
        }

        /** Step 7: stretch the items with {@code stretch} to the cross size of their line. */
        void stretchItems(List<List<Item>> lines, double[] lineCross) {
            for (int l = 0; l < lines.size(); l++) {
                for (Item it : lines.get(l)) {
                    if (it.align == Alignment.STRETCH) {
                        stretch(it, lineCross[l]);
                    }
                }
            }
        }

        /**
         * Step 8: align. The fragmentation units are the lines in horizontal and the items in
         * vertical (with Wrap, the whole container).
         */
        UnitBreaker.Arranged arrange(List<List<Item>> lines, CrossSizes cross, List<PendingOverlay> pending) {
            int n = lines.size();
            double[] lineCross = cross.lines();
            List<UnitBreaker.Placed> placed = new ArrayList<>();
            List<Span> spans = new ArrayList<>();
            double[] lineDistribution = distribute(multiLine ? s.stack().lineDistribution() : Distribution.START, cross.free(), n);
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
                double[] d = distribute(s.stack().mainAxis(), free, line.size());
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

        /** The units, with the quality of the break between each one and the previous one according to their items. */
        UnitBreaker.Units units(List<Span> spans, double extent) {
            List<Unit> units = new ArrayList<>();
            for (int u = 0; u < spans.size(); u++) {
                Span span = spans.get(u);
                if (u == 0) {
                    units.add(new Unit(span.start(), span.end(), 0, Break.AUTO));
                    continue;
                }
                List<Box> before = spans.get(u - 1).boxes();
                units.add(new Unit(span.start(), span.end(), BreakRules.between(before, span.boxes()),
                        BreakRules.forcedBetween(before, span.boxes())));
            }
            return new UnitBreaker.Units(units, extent, row || !multiLine);
        }

        // --- Steps 2 and 3: base, minimum and maximum of each item ------------------------------

        Item rowItem(Box b) {
            Style is = b.style();
            Item it = new Item(b, Insets.resolve(is.box().margin(), contentWidth), alignment(is));
            double margins = it.margin.horizontal();
            BoxModel probe = BoxModel.resolve(b, probeSpace(), context);
            double extra = probe.border().horizontal() + probe.padding().horizontal();
            IntrinsicSizes intrinsic = context.intrinsic(b);

            OptionalDouble declared = is.box().width().isAuto() ? OptionalDouble.empty() : OptionalDouble.of(probe.width());
            OptionalDouble basis = declaredBasis(is);
            double base = basis.isPresent() ? basis.getAsDouble() + margins
                    : declared.isPresent() ? declared.getAsDouble() + margins
                    : intrinsic.natural();
            double max = is.box().maxWidth() == null ? Double.POSITIVE_INFINITY
                    : is.box().maxWidth().resolve(contentWidth).orElse(Double.POSITIVE_INFINITY) + margins;
            double min;
            if (is.box().minWidth().isAuto()) {
                // Automatic minimum (reference/components/stack.md): the narrowest width of its content, not exceeding
                // Max width, and never more than the declared width.
                double content = Math.min(context.contentIntrinsic(b).narrowest() + extra + margins, max);
                min = declared.isPresent() ? Math.min(content, declared.getAsDouble() + margins) : content;
            } else {
                min = is.box().minWidth().resolve(contentWidth).orElse(0) + margins;
            }
            it.stack = new StackItem(base, is.item().grow(), is.item().shrink(), min, max, margins);
            return it;
        }

        Item columnItem(Box b) {
            Style is = b.style();
            Item it = new Item(b, Insets.resolve(is.box().margin(), contentWidth), alignment(is));
            double margins = it.margin.vertical();
            BoxModel probe = BoxModel.resolve(b, probeSpace(), context);

            // Width it is measured with: the declared one, the container's (stretch) or the fit to the content.
            double width;
            if (!is.box().width().isAuto()) {
                width = probe.width();
            } else if (it.align == Alignment.STRETCH && !multiLine) {
                width = Math.max(0, contentWidth - it.margin.horizontal());
            } else {
                width = Math.max(0, context.intrinsic(b).fit(contentWidth) - it.margin.horizontal());
            }
            it.crossWidth = BoxModel.constrainWidth(is, width, contentWidth);

            ConstraintSpace measure = itemSpace(it.crossWidth, OptionalDouble.empty());
            BoxModel sized = BoxModel.resolve(b, measure, context);
            double contentSize = context.layout(b, measure, null).fragment().height();

            OptionalDouble basis = declaredBasis(is);
            double base = basis.orElse(sized.height().orElse(contentSize)) + margins;
            double min;
            if (is.box().minHeight().isAuto()) {
                // Automatic minimum (reference/components/stack.md): the height of its content, not exceeding Max
                // height, and never more than the declared height.
                double content = sized.height().isPresent()
                        ? context.layout(b, measure.measuringContentHeight(), null).fragment().height()
                        : contentSize;
                min = Math.min(contentSize, content) + margins;
            } else {
                min = is.box().minHeight().resolve(contentHeight).orElse(0) + margins;
            }
            double max = is.box().maxHeight() == null ? Double.POSITIVE_INFINITY
                    : is.box().maxHeight().resolve(contentHeight).orElse(Double.POSITIVE_INFINITY) + margins;
            it.stack = new StackItem(base, is.item().grow(), is.item().shrink(), min, max, margins);
            return it;
        }

        /** {@code Base size} as border box; empty if it is {@code auto} or a percentage of an indefinite size. */
        OptionalDouble declaredBasis(Style is) {
            return is.item().baseSize().resolve(mainSize);
        }

        Alignment alignment(Style is) {
            Alignment own = s.stack().isHorizontal() ? is.item().vertical() : is.item().horizontal();
            return own != null ? own : s.stack().crossAxis();
        }

        // --- Steps 4 to 7 -----------------------------------------------------------------------------

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
                double h = it.stack.hypothetical();
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
                // Without a definite main size there is no free space: Grow has no effect.
                line.forEach(it -> it.main = it.stack.hypothetical());
                return;
            }
            double available = mainSize.getAsDouble() - mainGap * (line.size() - 1);
            double[] sizes = StackResolver.resolve(line.stream().map(it -> it.stack).toList(), available);
            for (int i = 0; i < sizes.length; i++) {
                line.get(i).main = sizes[i];
            }
        }

        /**
         * Layout of the item with its final main size.
         *
         * @param cross imposed cross size (border box), when stretching; empty otherwise
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

        /** An item without a declared cross size takes the size of the line (step 7). */
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
                if (!is.box().width().isAuto()) {
                    return;
                }
                target = BoxModel.constrainWidth(is, Math.max(0, lineCross - it.margin.horizontal()), contentWidth);
            }
            if (!same(target + (row ? it.margin.vertical() : it.margin.horizontal()), it.cross)) {
                layOutItem(it, OptionalDouble.of(target));
            }
        }

        // --- Step 8 ----------------------------------------------------------------------------------

        /** Places the item; {@code mainPos} and {@code crossPos} refer to its margin box. */
        UnitBreaker.Placed place(Item it, double mainPos, double crossPos, int unit) {
            double x = (row ? mainPos : crossPos) + it.margin.left();
            double y = (row ? crossPos : mainPos) + it.margin.top();
            FrameOffset offset = FrameOffset.of(it.box.style(), contentWidth, contentHeight);
            return new UnitBreaker.Placed(it.box, it.result, it.space, x + offset.dx(), y + offset.dy(), unit,
                    offset);
        }

        /**
         * Space of an item with the imposed width and, if there is one, the imposed height. The percentages of the
         * items resolve against the content box of the container.
         */
        ConstraintSpace itemSpace(double width, OptionalDouble fixedHeight) {
            return ConstraintSpace.fixed(width, fixedHeight, contentWidth, contentHeight, atStart);
        }

        /** Space of the probe that resolves the margins, border, padding and declared sizes of the item. */
        ConstraintSpace probeSpace() {
            return ConstraintSpace.free(contentWidth, contentWidth, contentHeight, atStart);
        }
    }

    /**
     * Distribution of the leftover {@code free} among {@code n} elements (main axis table,
     * reference/components/stack.md). If space is lacking, {@code justify} aligns to the start and {@code around} and
     * {@code distribute} are centered, as in CSS.
     *
     * @return {before the first, extra between elements}
     */
    static double[] distribute(Distribution d, double free, int n) {
        if (n == 0) {
            return new double[]{0, 0};
        }
        if (free < 0) {
            d = switch (d) {
                case JUSTIFY -> Distribution.START;
                case AROUND, DISTRIBUTE -> Distribution.CENTER;
                default -> d;
            };
        }
        return switch (d) {
            case START, STRETCH -> new double[]{0, 0};
            case END -> new double[]{free, 0};
            case CENTER -> new double[]{free / 2, 0};
            case JUSTIFY -> new double[]{0, n > 1 ? free / (n - 1) : 0};
            case AROUND -> new double[]{free / (2 * n), free / n};
            case DISTRIBUTE -> new double[]{free / (n + 1), free / (n + 1)};
        };
    }
}
