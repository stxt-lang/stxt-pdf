package dev.stxt.boxes.layout.grid;

import dev.stxt.boxes.fragment.BreakRules;
import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Unit;
import dev.stxt.boxes.layout.BoxModel;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.PendingAbsolute;
import dev.stxt.boxes.layout.Sizing;
import dev.stxt.boxes.layout.UnitBreaker;
import dev.stxt.boxes.layout.position.RelativeOffset;
import dev.stxt.boxes.model.Area;
import dev.stxt.boxes.model.Cell;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static dev.stxt.boxes.tree.PropertyKey.ITEM_AREA;

/**
 * Layout {@code grid} (ESPECIFICACION.md §5.8). Pasos:
 * <ol>
 *   <li>Colocación: {@link GridAreas}, {@link Cell} y {@link Placement}.</li>
 *   <li>Columnas: {@link TrackSizing} con min/max-content en ancho.</li>
 *   <li>Filas: {@link TrackSizing} con la altura de cada ítem al ancho de sus columnas.</li>
 *   <li>Alinear cada ítem en su área ({@code Horizontal}, {@code Vertical}); estirar = re-layout.</li>
 * </ol>
 *
 * <p>Los porcentajes de un ítem se resuelven contra su área: el ancho, desde el principio; el
 * alto, en el layout final, cuando las filas ya tienen tamaño. Los márgenes {@code auto} de los
 * ítems valen 0. Una pista en porcentaje sobre un tamaño indefinido vale 0.</p>
 *
 * <p>Los hijos {@code absolute} y {@code fixed} no son ítems: se devuelven como
 * {@link PendingAbsolute} con la esquina de la caja de contenido como posición estática. Los
 * ítems {@code relative} se desplazan con {@link RelativeOffset} después de alinearlos.</p>
 *
 * <p>Entre páginas (§5.10) se corta entre filas con {@link UnitBreaker}; los ítems de una
 * fila que no cabe en una página se parten en paralelo.</p>
 */
public final class GridLayout implements LayoutAlgorithm {

    private final Engine engine;

    public GridLayout(Engine engine) {
        this.engine = engine;
    }

    /** Ítems en flujo, el área de cada uno y las pistas, explícitas e implícitas. */
    private record Grid(List<Box> items, List<Area> areas, List<Track> columns, List<Track> rows) {
    }

    private static Grid grid(Box box) {
        Style s = box.style();
        int explicitColumns = Math.max(s.gridColumns().size(), s.gridAreas().columns());
        int explicitRows = Math.max(s.gridRows().size(), s.gridAreas().rows());
        List<Box> items = box.inFlowChildren();
        List<Cell> cells = items.stream().map(i -> i.style().itemArea()).toList();
        List<Area> areas;
        try {
            areas = Placement.place(cells, explicitColumns, explicitRows, s.gridFlow());
        } catch (Placement.NoRoom e) {
            throw noRoom(items.get(e.item), e, s.gridFlow());
        }
        int columns = Math.max(1, explicitColumns);
        int rows = explicitRows;
        for (Area a : areas) {
            columns = Math.max(columns, a.columnEnd());
            rows = Math.max(rows, a.rowEnd());
        }
        return new Grid(items, areas, tracks(s.gridColumns(), columns), tracks(s.gridRows(), rows));
    }

    /** El ítem pide una fila (una columna, con {@code Flow: column}) en la que no queda hueco (§5.8). */
    private static InvalidModelException noRoom(Box item, Placement.NoRoom e, Style.Direction flow) {
        boolean row = flow == Style.Direction.ROW;
        String track = row ? "fila" : "columna";
        String across = row ? "columna" : "fila";
        String hole = e.span == 1 ? "1 " + across + " libre" : e.span + " " + across + "s libres";
        return new InvalidModelException(item.style().line(ITEM_AREA), ErrorCode.GRID_PLACEMENT, "En la " + track + " "
                + e.track + " no queda un hueco de " + hole + " para este ítem (la rejilla tiene " + e.tracks + " "
                + across + (e.tracks == 1 ? "" : "s") + "): elige otra " + track + " o déjala en auto");
    }

    /** Pistas declaradas y, detrás, las implícitas {@code auto}. */
    private static List<Track> tracks(List<Value> declared, int count) {
        List<Track> tracks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tracks.add(Track.of(i < declared.size() ? declared.get(i) : Value.AUTO));
        }
        return tracks;
    }

    /**
     * Suma de las columnas dimensionadas con restricción min-content (sin espacio libre) y con
     * restricción max-content (espacio infinito y los ítems aportando su max-content), §5.9.
     */
    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        Grid g = grid(box);
        // Sin containing block, un hueco en porcentaje cuenta como 0.
        double gap = box.style().columnGap().resolve(OptionalDouble.empty()).orElse(0);
        List<ItemContribution> minContributions = new ArrayList<>();
        List<ItemContribution> maxContributions = new ArrayList<>();
        for (int i = 0; i < g.items().size(); i++) {
            IntrinsicSizes is = engine.intrinsic(g.items().get(i));
            Area a = g.areas().get(i);
            minContributions.add(new ItemContribution(a.column() - 1, a.columns(), is.minContent(), is.maxContent()));
            maxContributions.add(new ItemContribution(a.column() - 1, a.columns(), is.maxContent(), is.maxContent()));
        }
        double gaps = gap * Math.max(0, g.columns().size() - 1);
        // Sin containing block, las columnas en porcentaje cuentan como auto (§3.1).
        List<Track> columns = g.columns().stream().map(Track::withoutPercentBase).toList();
        double min = Sizing.sum(TrackSizing.size(columns, minContributions, gaps, gap)) + gaps;
        double max = Sizing.sum(TrackSizing.size(columns, maxContributions, Double.POSITIVE_INFINITY, gap)) + gaps;
        return new IntrinsicSizes(min, Math.max(min, max));
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        return UnitBreaker.fragment(engine, box, space, from, arrange(box, space));
    }

    /**
     * Coloca los ítems y dimensiona las pistas, sin fragmentar: las filas son las unidades. La
     * tabla lo usa para cortar solo su cuerpo.
     */
    public UnitBreaker.Arranged arrange(Box box, ConstraintSpace space) {
        BoxModel model = BoxModel.resolve(box, space, engine);
        Style s = box.style();
        double contentWidth = model.contentWidth();
        OptionalDouble contentHeight = model.contentHeight();
        double columnGap = s.columnGap().resolve(contentWidth).orElse(0);
        double rowGap = s.rowGap().resolve(contentHeight).orElse(0);
        boolean atStart = space.atFragmentainerStart();
        Grid g = grid(box);
        int n = g.items().size();

        // Columnas.
        List<ItemContribution> widths = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            IntrinsicSizes is = engine.intrinsic(g.items().get(i));
            Area a = g.areas().get(i);
            widths.add(new ItemContribution(a.column() - 1, a.columns(), is.minContent(), is.maxContent()));
        }
        double[] columns = TrackSizing.size(g.columns(), widths, contentWidth, columnGap);
        double[] x = offsets(columns, columnGap);

        // Ancho de cada ítem en su área y su altura con ese ancho.
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
            margins[i] = Insets.resolve(item.style().margin(), areaWidths[i]);
            itemWidths[i] = itemWidth(item, areaWidths[i], margins[i], horizontal(item, s), atStart);
            spaces[i] = ConstraintSpace.fixed(itemWidths[i], OptionalDouble.empty(), areaWidths[i],
                    OptionalDouble.empty(), atStart);
            results[i] = engine.layout(item, spaces[i], null);
            double h = results[i].fragment().height() + margins[i].vertical();
            heights.add(new ItemContribution(a.row() - 1, a.rows(), h, h));
        }

        // Filas.
        double[] rows = TrackSizing.size(g.rows(), heights, contentHeight.orElse(Double.POSITIVE_INFINITY), rowGap);
        double[] y = offsets(rows, rowGap);

        // Alinear cada ítem en su área.
        List<UnitBreaker.Placed> placed = new ArrayList<>();
        List<PendingAbsolute> pending = new ArrayList<>();
        for (Box child : box.children()) {
            if (child.style().isOutOfFlow()) {
                pending.add(new PendingAbsolute(child, 0, 0));
            }
        }
        for (int i = 0; i < n; i++) {
            Box item = g.items().get(i);
            Area a = g.areas().get(i);
            Insets margin = margins[i];
            double areaHeight = span(rows, a.row() - 1, a.rows(), rowGap);
            Style.Alignment vertical = vertical(item, s);
            ConstraintSpace finalSpace = finalSpace(item, spaces[i], itemWidths[i], areaWidths[i],
                    areaHeight, margin, vertical, atStart);
            LayoutResult r = engine.layout(item, finalSpace, null);
            Fragment f = r.fragment();
            double px = x[a.column() - 1] + margin.left()
                    + Sizing.align(horizontal(item, s), areaWidths[i], f.width() + margin.horizontal());
            double py = y[a.row() - 1] + margin.top()
                    + Sizing.align(vertical, areaHeight, f.height() + margin.vertical());
            RelativeOffset relative = RelativeOffset.of(item.style(), contentWidth, contentHeight);
            placed.add(new UnitBreaker.Placed(item, r, finalSpace, px + relative.dx(), py + relative.dy(), a.row() - 1,
                    relative));
        }

        double extent = Sizing.sum(rows) + rowGap * Math.max(0, rows.length - 1);
        return new UnitBreaker.Arranged(model, placed, pending, rowUnits(g, rows, y, extent));
    }

    /**
     * Las filas como unidades de la fragmentación (§5.10). No se puede cortar entre dos filas que
     * ocupa un mismo ítem; entre las demás, la calidad del corte sale de los ítems que acaban
     * antes y de los que empiezan después.
     */
    private static UnitBreaker.Units rowUnits(Grid g, double[] rows, double[] y, double extent) {
        List<Unit> units = new ArrayList<>();
        for (int r = 0; r < rows.length; r++) {
            units.add(r == 0 ? new Unit(y[r], y[r] + rows[r], 0, Style.Break.AUTO) : rowUnit(g, r, y[r], y[r] + rows[r]));
        }
        return new UnitBreaker.Units(units, extent, true);
    }

    /** La fila de índice {@code r} (no la primera) como unidad, con la calidad del corte delante de ella. */
    private static Unit rowUnit(Grid g, int r, double start, double end) {
        List<Box> before = new ArrayList<>();
        List<Box> after = new ArrayList<>();
        boolean spanned = false;
        for (int i = 0; i < g.items().size(); i++) {
            Area a = g.areas().get(i);
            int from = a.row() - 1;
            int to = from + a.rows();
            spanned |= from < r && to > r;
            if (to == r) {
                before.add(g.items().get(i));
            } else if (from == r) {
                after.add(g.items().get(i));
            }
        }
        if (spanned) {
            return new Unit(start, end, -1, Style.Break.AUTO);
        }
        return new Unit(start, end, BreakRules.between(before, after), BreakRules.forcedBetween(before, after));
    }

    /** Ancho de caja de borde del ítem: el declarado, el del área ({@code stretch}) o el ajuste al contenido. */
    private double itemWidth(Box item, double areaWidth, Insets margin, Style.Alignment horizontal, boolean atStart) {
        Style is = item.style();
        ConstraintSpace free = ConstraintSpace.free(areaWidth, areaWidth, OptionalDouble.empty(), atStart);
        BoxModel probe = BoxModel.resolve(item, free, engine);
        if (!is.width().isAuto()) {
            return probe.width();
        }
        double width = horizontal == Style.Alignment.STRETCH
                ? areaWidth - margin.horizontal()
                : engine.intrinsic(item).fitContent(areaWidth) - margin.horizontal();
        return BoxModel.constrainWidth(is, Math.max(0, width), areaWidth);
    }

    /**
     * Espacio del layout definitivo del ítem: con el alto del área como base de sus porcentajes
     * y, con {@code stretch} y alto {@code auto}, estirado al área. Es el de la medida si el ítem
     * no se estira ni declara su alto en porcentaje.
     */
    private ConstraintSpace finalSpace(Box item, ConstraintSpace measureSpace, double width,
                                       double areaWidth, double areaHeight, Insets margin, Style.Alignment vertical,
                                       boolean atStart) {
        Style is = item.style();
        OptionalDouble base = OptionalDouble.of(areaHeight);
        OptionalDouble fixedHeight = OptionalDouble.empty();
        if (vertical == Style.Alignment.STRETCH) {
            ConstraintSpace probe = ConstraintSpace.fixed(width, OptionalDouble.empty(), areaWidth, base, atStart);
            BoxModel m = BoxModel.resolve(item, probe, engine);
            if (m.height().isEmpty()) {
                fixedHeight = OptionalDouble.of(Sizing.constrain(
                        Math.max(0, areaHeight - margin.vertical()), m.minHeight(), m.maxHeight()));
            }
        }
        // Un ítem estirado se maqueta siempre con su alto fijado, aunque mida lo mismo: es la base
        // definida de los porcentajes de sus hijos (§3.6).
        if (fixedHeight.isEmpty() && !dependsOnHeightBase(is)) {
            return measureSpace;
        }
        return ConstraintSpace.fixed(width, fixedHeight, areaWidth, base, atStart);
    }

    /** El alto del ítem se declara con un porcentaje (o una función que lo contiene). */
    private static boolean dependsOnHeightBase(Style s) {
        return dependsOnBase(s.height()) || dependsOnBase(s.minHeight()) || (s.maxHeight() != null && dependsOnBase(s.maxHeight()));
    }

    private static boolean dependsOnBase(Value v) {
        return v.resolve(OptionalDouble.empty()).isEmpty() && v.resolve(1).isPresent();
    }

    private static Style.Alignment horizontal(Box item, Style container) {
        return item.style().itemHorizontal() != null ? item.style().itemHorizontal() : container.gridHorizontal();
    }

    private static Style.Alignment vertical(Box item, Style container) {
        return item.style().itemVertical() != null ? item.style().itemVertical() : container.gridVertical();
    }

    /** Posición de cada pista desde el principio de la caja de contenido. */
    private static double[] offsets(double[] sizes, double gap) {
        double[] r = new double[sizes.length];
        double pos = 0;
        for (int i = 0; i < sizes.length; i++) {
            r[i] = pos;
            pos += sizes[i] + gap;
        }
        return r;
    }

    /** Tamaño de {@code count} pistas desde {@code from} con los huecos internos. */
    private static double span(double[] sizes, int from, int count, double gap) {
        double r = gap * (count - 1);
        for (int i = from; i < from + count; i++) {
            r += sizes[i];
        }
        return r;
    }
}
