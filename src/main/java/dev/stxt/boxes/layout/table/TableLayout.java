package dev.stxt.boxes.layout.table;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.LayoutContext;
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
import java.util.List;

import static dev.stxt.boxes.layout.Messages.pt;
import static dev.stxt.boxes.model.Tolerance.EPSILON;

/**
 * Layout de una {@code Table} (ESPECIFICACION.md §5.15): el de su grid, con las filas de
 * {@code Head} y {@code Foot} repetidas en cada fragmento.
 *
 * <p>{@code BoxTreeBuilder} traduce la tabla a un grid con colocación explícita; las primeras
 * {@link Box#headRows()} filas de la rejilla son la cabecera y las últimas {@link Box#footRows()},
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

    private final GridLayout grid;
    private final LayoutContext context;

    public TableLayout(LayoutContext context, GridLayout grid) {
        this.context = context;
        this.grid = grid;
    }

    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        return grid.contentIntrinsic(box);
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        UnitBreaker.Arranged a = grid.arrange(box, space);
        UnitBreaker.Units all = a.units();
        int head = box.headRows();
        int bodyEnd = all.size() - box.footRows();
        // La cabecera mide hasta el principio del cuerpo y el pie desde su final: los dos, con su hueco.
        double bodyStart = all.get(head).start();
        double bodyBottom = all.get(bodyEnd - 1).end();
        double footHeight = box.footRows() == 0 ? 0 : all.extent() - bodyBottom;
        if (head + box.footRows() > 0 && space.fragmenting() && space.atFragmentainerStart()
                && Slice.contentRemaining(a.model(), from == null, space) - bodyStart - footHeight <= EPSILON) {
            throw new InvalidModelException(box.line(), ErrorCode.TABLE_REPEAT_OVERFLOW, "La cabecera y el pie de la "
                    + "tabla " + box.describe() + " miden " + pt(bodyStart + footHeight) + " y no dejan sitio al cuerpo "
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
                        p.unit() - head, p.relative()));
            } else {
                footItems.add(new UnitBreaker.Placed(p.box(), p.result(), p.space(), p.x(), p.y() - bodyBottom,
                        p.unit(), p.relative()));
            }
        }
        UnitBreaker.Arranged bodyOnly = new UnitBreaker.Arranged(a.model(), body, a.outOfFlow(),
                all.slice(head, bodyEnd, bodyStart));
        UnitBreaker.Repeat repeat = new UnitBreaker.Repeat(headItems, bodyStart, footItems, footHeight);
        return UnitBreaker.fragment(context, box, space, from, bodyOnly, repeat);
    }
}
