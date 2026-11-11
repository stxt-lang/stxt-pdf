package dev.stxt.pdf.layout.grid;

import dev.stxt.pdf.fragment.BreakRules;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Unit;
import dev.stxt.pdf.layout.BoxModel;
import dev.stxt.pdf.layout.ConstraintSpace;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.ItemSizing;
import dev.stxt.pdf.layout.LayoutAlgorithm;
import dev.stxt.pdf.layout.LayoutContext;
import dev.stxt.pdf.layout.LayoutResult;
import dev.stxt.pdf.layout.PendingOverlay;
import dev.stxt.pdf.layout.Sizing;
import dev.stxt.pdf.layout.UnitBreaker;
import dev.stxt.pdf.layout.position.FrameOffset;
import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Area;
import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.FillOrder;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;

import static dev.stxt.pdf.tree.PropertyKey.ITEM_AREA;

/**
 * Layout {@code grid} (reference/components/grid.md). Steps:
 * <ol>
 *   <li>Placement: {@link dev.stxt.pdf.model.AreaMap}, {@link Cell} and {@link Placement}.</li>
 *   <li>Columns: {@link TrackSizing} with the narrowest and natural widths; those of a table share the free space in
 *       proportion to their content.</li>
 *   <li>Rows: {@link TrackSizing} with the height of each item at the width of its columns.</li>
 *   <li>Align each item in its area ({@code Horizontal}, {@code Vertical}); stretching = re-layout.</li>
 * </ol>
 *
 * <p>The percentages of an item resolve against its area: the width, from the start; the
 * height, in the final layout, once the rows have a size. The {@code auto} margins of the
 * items are 0. A percentage track over an indefinite size is 0.</p>
 *
 * <p>The {@code Overlay} and {@code Page overlay} children are not items: they are returned as
 * {@link PendingOverlay} with the corner of the content box as static position. The
 * {@code Frame} items are shifted with {@link FrameOffset} after being aligned.</p>
 *
 * <p>Across pages (reference/common/breaks.md) the break goes between rows with {@link UnitBreaker}; the items of a
 * row that does not fit in what is left of the page are split in parallel on that page ({@code Split rows}, by
 * default) or, with {@code Split rows: false}, only if the row does not fit on a whole page.</p>
 */
public final class GridLayout implements LayoutAlgorithm {

    private final LayoutContext context;

    public GridLayout(LayoutContext context) {
        this.context = context;
    }

    /** In-flow items, the area of each one and the tracks, explicit and implicit. */
    private record Grid(List<Box> items, List<Area> areas, List<Track> columns, List<Track> rows) {
    }

    private static Grid grid(Box box) {
        Style s = box.style();
        int explicitColumns = Math.max(s.grid().columnSizes().size(), s.grid().areaMap().columns());
        int explicitRows = Math.max(s.grid().rowSizes().size(), s.grid().areaMap().rows());
        List<Box> items = box.inFlowChildren();
        List<Cell> cells = items.stream().map(i -> i.style().item().area()).toList();
        Placement.Result placement = Placement.place(cells, explicitColumns, explicitRows, s.grid().fillOrder());
        if (placement instanceof Placement.NoRoom e) {
            throw noRoom(items.get(e.item()), e, s.grid().fillOrder());
        }
        List<Area> areas = ((Placement.Placed) placement).areas();
        int columns = Math.max(1, explicitColumns);
        int rows = explicitRows;
        for (Area a : areas) {
            columns = Math.max(columns, a.columnEnd());
            rows = Math.max(rows, a.rowEnd());
        }
        return new Grid(items, areas, tracks(s.grid().columnSizes(), columns), tracks(s.grid().rowSizes(), rows));
    }

    /**
     * The item asks for a row (a column, with {@code Fill order: columns}) that has no room left
     * (reference/components/grid.md).
     */
    private static InvalidModelException noRoom(Box item, Placement.NoRoom e, FillOrder order) {
        boolean row = order == FillOrder.ROWS;
        String track = row ? "row" : "column";
        String across = row ? "column" : "row";
        String hole = e.span() == 1 ? "1 free " + across : e.span() + " free " + across + "s";
        return new InvalidModelException(item.style().line(ITEM_AREA), ErrorCode.GRID_PLACEMENT, "In " + track + " "
                + e.track() + " there is no room of " + hole + " left for this item (the grid has " + e.tracks() + " "
                + across + (e.tracks() == 1 ? "" : "s") + "): another " + track + " has to be chosen or it has to be"
                + " left as auto");
    }

    /** Declared tracks and, after them, the implicit {@code auto} ones. */
    private static List<Track> tracks(List<Value> declared, int count) {
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tracks.add(Track.of(i < declared.size() ? declared.get(i) : Value.AUTO));
        }
        return tracks;
    }

    /**
     * Sum of the columns sized for the narrowest width (no free space) and for the natural one (infinite space and
     * the items contributing their natural width), reference/common/box.md.
     */
    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        Grid g = grid(box);
        // Without a reference container, a percentage gap counts as 0.
        double gap = box.style().grid().columnGap().resolve(OptionalDouble.empty()).orElse(0);
        List<ItemContribution> minContributions = new ArrayList<>();
        List<ItemContribution> maxContributions = new ArrayList<>();
        for (int i = 0; i < g.items().size(); i++) {
            IntrinsicSizes is = context.intrinsic(g.items().get(i));
            Area a = g.areas().get(i);
            minContributions.add(new ItemContribution(a.column() - 1, a.columns(), is.narrowest(), is.natural()));
            maxContributions.add(new ItemContribution(a.column() - 1, a.columns(), is.natural(), is.natural()));
        }
        double gaps = gap * Math.max(0, g.columns().size() - 1);
        // Without a reference container, percentage columns count as auto (reference/common/values.md).
        List<Track> columns = g.columns().stream().map(Track::withoutPercentBase).toList();
        double min = Sizing.sum(TrackSizing.size(columns, minContributions, gaps, gap)) + gaps;
        double max = Sizing.sum(TrackSizing.size(columns, maxContributions, Double.POSITIVE_INFINITY, gap)) + gaps;
        return new IntrinsicSizes(min, Math.max(min, max));
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        return UnitBreaker.fragment(context, box, space, from, arrange(box, space));
    }

    /**
     * A grid laid out without fragmenting ({@link #arrange}) with its columns: where each one starts, from the content
     * box, and its width. The table draws its merged borders with them (reference/components/table.md#merged-borders).
     */
    public record ArrangedWithColumns(UnitBreaker.Arranged arranged, List<Double> columnStarts,
                                      List<Double> columnWidths) {

        public ArrangedWithColumns {
            columnStarts = List.copyOf(columnStarts);
            columnWidths = List.copyOf(columnWidths);
        }
    }

    /**
     * Places the items and sizes the tracks, without fragmenting: the rows are the units. The
     * table uses it to break only its body.
     */
    public UnitBreaker.Arranged arrange(Box box, ConstraintSpace space) {
        return arrangeWithColumns(box, space).arranged();
    }

    /**
     * Like {@link #arrange}, with the columns. The arrangement depends on the box and on its space without
     * fragmentation, not on what is left of the page: it is computed once per container and space
     * ({@link LayoutContext#arranged}) and every fragment breaks the same one.
     */
    public ArrangedWithColumns arrangeWithColumns(Box box, ConstraintSpace space) {
        ConstraintSpace key = space.unfragmented();
        return context.arranged(box, key, ArrangedWithColumns.class, () -> computeArrangement(box, key));
    }

    /** The arrangement of {@link #arrangeWithColumns}: placement, tracks, items at their sizes, bands. */
    private ArrangedWithColumns computeArrangement(Box box, ConstraintSpace space) {
        BoxModel model = BoxModel.resolve(box, space, context);
        Style s = box.style();
        double contentWidth = model.contentWidth();
        OptionalDouble contentHeight = model.contentHeight();
        double columnGap = s.grid().columnGap().resolve(contentWidth).orElse(0);
        double rowGap = s.grid().rowGap().resolve(contentHeight).orElse(0);
        boolean atStart = space.atFragmentainerStart();
        Grid g = grid(box);
        int n = g.items().size();

        // Columns.
        List<ItemContribution> widths = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            IntrinsicSizes is = context.intrinsic(g.items().get(i));
            Area a = g.areas().get(i);
            widths.add(new ItemContribution(a.column() - 1, a.columns(), is.narrowest(), is.natural()));
        }
        // The columns of a table share the space in proportion to their content (reference/components/table.md).
        double[] columns = TrackSizing.size(g.columns(), widths, contentWidth, columnGap,
                box.role() == Element.Role.TABLE ? TrackSizing.Distribution.CONTENT : TrackSizing.Distribution.EQUAL);
        double[] x = offsets(columns, columnGap);

        // Width of each item in its area and its height at that width.
        Insets[] margins = new Insets[n];
        double[] itemWidths = new double[n];
        double[] areaWidths = new double[n];
        ConstraintSpace[] spaces = new ConstraintSpace[n];
        LayoutResult[] results = new LayoutResult[n];
        List<ItemContribution> heights = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Box item = g.items().get(i);
            Area a = g.areas().get(i);
            areaWidths[i] = span(columns, a.column() - 1, a.columns(), columnGap);
            margins[i] = Insets.resolve(item.style().box().margin(), areaWidths[i]);
            itemWidths[i] = itemWidth(item, areaWidths[i], margins[i], horizontal(item, s), atStart);
            spaces[i] = ConstraintSpace.fixed(itemWidths[i], OptionalDouble.empty(), areaWidths[i],
                    OptionalDouble.empty(), atStart);
            results[i] = context.layout(item, spaces[i], null);
            double h = results[i].fragment().height() + margins[i].vertical();
            heights.add(new ItemContribution(a.row() - 1, a.rows(), h, h));
        }

        // Rows.
        double[] rows = TrackSizing.size(g.rows(), heights, contentHeight.orElse(Double.POSITIVE_INFINITY), rowGap);
        double[] y = offsets(rows, rowGap);

        // Align each item in its area.
        List<UnitBreaker.Placed> placed = new ArrayList<>();
        List<PendingOverlay> pending = ItemSizing.outOfFlow(box);
        for (int i = 0; i < n; i++) {
            Box item = g.items().get(i);
            Area a = g.areas().get(i);
            Insets margin = margins[i];
            double areaHeight = span(rows, a.row() - 1, a.rows(), rowGap);
            Alignment vertical = vertical(item, s);
            ConstraintSpace finalSpace = finalSpace(item, spaces[i], itemWidths[i], areaWidths[i],
                    areaHeight, margin, vertical, atStart);
            LayoutResult r = context.layout(item, finalSpace, null);
            Fragment f = r.fragment();
            double px = x[a.column() - 1] + margin.left()
                    + Sizing.align(horizontal(item, s), areaWidths[i], f.width() + margin.horizontal());
            double py = y[a.row() - 1] + margin.top()
                    + Sizing.align(vertical, areaHeight, f.height() + margin.vertical());
            FrameOffset frameOffset = FrameOffset.of(item.style(), contentWidth, contentHeight);
            // Only a stretched item is laid out with its height fixed (finalSpace).
            placed.add(new UnitBreaker.Placed(item, r, finalSpace, px + frameOffset.dx(), py + frameOffset.dy(),
                    a.row() - 1, frameOffset, finalSpace.fixedHeight()));
        }

        double extent = Sizing.sum(rows) + rowGap * Math.max(0, rows.length - 1);
        // The bands of the rows of a table, from the first column to the last (reference/components/table.md).
        double width = Sizing.sum(columns) + columnGap * Math.max(0, columns.length - 1);
        List<UnitBreaker.Band> bands = box.rows().stream().map(r -> new UnitBreaker.Band(r,
                r.style().item().area().row() - 1, 0, width, Insets.resolve(r.style().box().border(), contentWidth)))
                .toList();
        UnitBreaker.Arranged arranged = new UnitBreaker.Arranged(model, placed, pending,
                rowUnits(g, rows, y, extent, s.grid().splitRows()), bands);
        return new ArrangedWithColumns(arranged, Arrays.stream(x).boxed().toList(),
                Arrays.stream(columns).boxed().toList());
    }

    /**
     * The rows as fragmentation units (reference/common/breaks.md). There can be no break between two rows that the
     * same item spans; between the others, the quality of the break comes from the items that end
     * before and the ones that start after. One pass over the items sorts them by the row they end at and the one they
     * start at, and marks the rows an item spans: the cost is that of the items, not that of the items for each row.
     */
    private static UnitBreaker.Units rowUnits(Grid g, double[] rows, double[] y, double extent, boolean split) {
        int n = rows.length;
        List<List<Box>> endsAt = new ArrayList<>(n + 1);
        List<List<Box>> startsAt = new ArrayList<>(n + 1);
        for (int r = 0; r <= n; r++) {
            endsAt.add(new ArrayList<>());
            startsAt.add(new ArrayList<>());
        }
        boolean[] spanned = new boolean[n];
        for (int i = 0; i < g.items().size(); i++) {
            Area a = g.areas().get(i);
            int from = a.row() - 1;
            int to = from + a.rows();
            startsAt.get(from).add(g.items().get(i));
            endsAt.get(to).add(g.items().get(i));
            for (int r = from + 1; r < to; r++) {
                spanned[r] = true;
            }
        }
        List<Unit> units = new ArrayList<>(n);
        for (int r = 0; r < n; r++) {
            double start = y[r];
            double end = y[r] + rows[r];
            if (r == 0) {
                units.add(new Unit(start, end, 0, Break.AUTO));
            } else if (spanned[r]) {
                units.add(new Unit(start, end, -1, Break.AUTO));
            } else {
                units.add(new Unit(start, end, BreakRules.between(endsAt.get(r), startsAt.get(r)),
                        BreakRules.forcedBetween(endsAt.get(r), startsAt.get(r))));
            }
        }
        return new UnitBreaker.Units(units, extent, true, split);
    }

    /** Border box width of the item: the declared one, the area's ({@code stretch}) or the fit to the content. */
    private double itemWidth(Box item, double areaWidth, Insets margin, Alignment horizontal, boolean atStart) {
        ConstraintSpace free = ConstraintSpace.free(areaWidth, areaWidth, OptionalDouble.empty(), atStart);
        BoxModel probe = BoxModel.resolve(item, free, context);
        return ItemSizing.width(item.style(), probe, areaWidth, margin, horizontal == Alignment.STRETCH,
                () -> context.intrinsic(item));
    }

    /**
     * Space of the final layout of the item: with the height of the area as the base of its percentages
     * and, with {@code stretch} and {@code auto} height, stretched to the area. It is the measuring one if the item
     * is not stretched and does not declare its height as a percentage.
     */
    private ConstraintSpace finalSpace(Box item, ConstraintSpace measureSpace, double width,
                                       double areaWidth, double areaHeight, Insets margin, Alignment vertical,
                                       boolean atStart) {
        Style is = item.style();
        OptionalDouble base = OptionalDouble.of(areaHeight);
        OptionalDouble fixedHeight = OptionalDouble.empty();
        if (vertical == Alignment.STRETCH) {
            // The percentages of the item resolve against its area.
            ConstraintSpace probe = ConstraintSpace.fixed(width, OptionalDouble.empty(), areaWidth, base, atStart);
            fixedHeight = ItemSizing.stretchedHeight(BoxModel.resolve(item, probe, context), areaHeight, margin);
        }
        // A stretched item is always laid out with its height fixed, even if it measures the same: it is the
        // definite base of the percentages of its children (reference/common/values.md).
        if (fixedHeight.isEmpty() && !dependsOnHeightBase(is)) {
            return measureSpace;
        }
        return ConstraintSpace.fixed(width, fixedHeight, areaWidth, base, atStart);
    }

    /** The height of the item is declared with a percentage (or a function that contains one). */
    private static boolean dependsOnHeightBase(Style s) {
        return dependsOnBase(s.box().height()) || dependsOnBase(s.box().minHeight()) || (s.box().maxHeight() != null && dependsOnBase(s.box().maxHeight()));
    }

    private static boolean dependsOnBase(Value v) {
        return v.resolve(OptionalDouble.empty()).isEmpty() && v.resolve(1).isPresent();
    }

    private static Alignment horizontal(Box item, Style container) {
        return ItemSizing.effective(item.style().item().horizontal(), container.grid().horizontal());
    }

    private static Alignment vertical(Box item, Style container) {
        return ItemSizing.effective(item.style().item().vertical(), container.grid().vertical());
    }

    /** Position of each track from the start of the content box. */
    private static double[] offsets(double[] sizes, double gap) {
        double[] r = new double[sizes.length];
        double pos = 0;
        for (int i = 0; i < sizes.length; i++) {
            r[i] = pos;
            pos += sizes[i] + gap;
        }
        return r;
    }

    /** Size of {@code count} tracks from {@code from} with the inner gaps. */
    private static double span(double[] sizes, int from, int count, double gap) {
        double r = gap * (count - 1);
        for (int i = from; i < from + count; i++) {
            r += sizes[i];
        }
        return r;
    }
}
