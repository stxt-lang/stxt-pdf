package dev.stxt.boxes.layout.table;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.Slice;
import dev.stxt.boxes.layout.UnitBreaker;
import dev.stxt.boxes.layout.grid.GridLayout;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Layout de una {@code Table} (ESPECIFICACION.md §5.15): el de su grid, con las filas de
 * {@code Head} y {@code Foot} repetidas en cada fragmento.
 *
 * <p>{@code BoxTreeBuilder} traduce la tabla a un grid con colocación explícita; las primeras
 * {@link Box#headRows} filas de la rejilla son la cabecera y las últimas {@link Box#footRows},
 * el pie. Aquí se maqueta el grid entero sin fragmentar ({@link GridLayout#arrange}) y solo se
 * cortan las filas del cuerpo, con {@link UnitBreaker}: lo que queda de página para ellas es el
 * resto tras la cabecera y el pie, que van enteros en cada fragmento, la cabecera arriba y el pie
 * detrás de la última fila del cuerpo colocada. Sin cabecera ni pie, es el layout del grid.</p>
 *
 * <p>Si al principio de una página la cabecera y el pie no dejan sitio al cuerpo, la tabla no
 * puede avanzar: es un error ({@link ErrorCode#TABLE_REPEAT_OVERFLOW}). Una fila del cuerpo que
 * no cabe en el sitio que dejan se parte en paralelo, como en un grid.</p>
 */
public final class TableLayout implements LayoutAlgorithm {

    private static final double EPSILON = 1e-9;

    private final GridLayout grid;
    private final Engine engine;

    public TableLayout(Engine engine, GridLayout grid) {
        this.engine = engine;
        this.grid = grid;
    }

    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        return grid.contentIntrinsic(box);
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        GridLayout.Arranged a = grid.arrange(box, space);
        UnitBreaker.Units all = a.units();
        int head = box.headRows;
        int bodyEnd = all.starts().length - box.footRows;
        // La cabecera mide hasta el principio del cuerpo y el pie desde su final: los dos, con su hueco.
        double bodyStart = all.starts()[head];
        double bodyBottom = all.ends()[bodyEnd - 1];
        double footHeight = box.footRows == 0 ? 0 : all.extent() - bodyBottom;
        if (head + box.footRows > 0 && space.fragmenting() && space.atFragmentainerStart()
                && Slice.contentRemaining(a.model(), from == null, space) - bodyStart - footHeight <= EPSILON) {
            throw new InvalidModelException(box.line, ErrorCode.TABLE_REPEAT_OVERFLOW, "La cabecera y el pie de la "
                    + "tabla " + name(box) + " miden " + pt(bodyStart + footHeight) + " y no dejan sitio al cuerpo "
                    + "en una página vacía: se repiten en cada página; reduce su alto o quita filas de Head y Foot");
        }

        List<UnitBreaker.Placed> headItems = new ArrayList<>();
        List<UnitBreaker.Placed> body = new ArrayList<>();
        List<UnitBreaker.Placed> footItems = new ArrayList<>();
        for (UnitBreaker.Placed p : a.placed()) {
            if (p.unit() < head) {
                headItems.add(p);
            } else if (p.unit() < bodyEnd) {
                body.add(new UnitBreaker.Placed(p.box(), p.result(), p.space(), p.x(), p.y() - bodyStart,
                        p.unit() - head));
            } else {
                footItems.add(new UnitBreaker.Placed(p.box(), p.result(), p.space(), p.x(), p.y() - bodyBottom,
                        p.unit()));
            }
        }
        double[] starts = shifted(Arrays.copyOfRange(all.starts(), head, bodyEnd), bodyStart);
        double[] ends = shifted(Arrays.copyOfRange(all.ends(), head, bodyEnd), bodyStart);
        UnitBreaker.Units units = new UnitBreaker.Units(starts, ends, Arrays.copyOfRange(all.appeal(), head, bodyEnd),
                Arrays.copyOfRange(all.forced(), head, bodyEnd), bodyBottom - bodyStart, all.splittable());
        UnitBreaker.Repeat repeat = new UnitBreaker.Repeat(headItems, bodyStart, footItems, footHeight);
        return UnitBreaker.fragment(engine, box, a.model(), space, from, body, a.outOfFlow(), units, repeat);
    }

    private static double[] shifted(double[] values, double origin) {
        for (int i = 0; i < values.length; i++) {
            values[i] -= origin;
        }
        return values;
    }

    /** {@code t (línea 12)} o {@code de la línea 12}. */
    private static String name(Box box) {
        return box.id != null ? box.id + " (línea " + box.line + ")" : "de la línea " + box.line;
    }

    private static String pt(double v) {
        return String.valueOf(Math.round(v * 100) / 100.0).replaceAll("\\.0$", "");
    }
}
