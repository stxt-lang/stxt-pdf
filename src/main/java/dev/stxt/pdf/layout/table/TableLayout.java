package dev.stxt.pdf.layout.table;

import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Unit;
import dev.stxt.pdf.layout.ConstraintSpace;
import dev.stxt.pdf.layout.LayoutContext;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.LayoutAlgorithm;
import dev.stxt.pdf.layout.LayoutResult;
import dev.stxt.pdf.layout.Slice;
import dev.stxt.pdf.layout.UnitBreaker;
import dev.stxt.pdf.layout.grid.GridLayout;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.layout.Messages.pt;
import static dev.stxt.pdf.model.Tolerance.EPSILON;

/**
 * Layout de una {@code Table} (reference/components/table.md): el de su grid, con las filas de
 * {@code Header rows} y {@code Footer rows} repetidas en cada fragmento.
 *
 * <p>{@code BoxTreeBuilder} traduce la tabla a un grid con colocación explícita; las primeras
 * {@link Box#headerRows()} filas de la rejilla son la cabecera y las últimas {@link Box#footerRows()},
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
        int head = box.headerRows();
        int bodyEnd = all.size() - box.footerRows();
        // La cabecera mide hasta el principio del cuerpo y el pie desde su final: los dos, con su hueco.
        double bodyStart = all.get(head).start();
        double bodyBottom = all.get(bodyEnd - 1).end();
        double footHeight = box.footerRows() == 0 ? 0 : all.extent() - bodyBottom;
        if (head + box.footerRows() > 0 && space.fragmenting() && space.atFragmentainerStart()
                && Slice.contentRemaining(a.model(), from == null, space) - bodyStart - footHeight <= EPSILON) {
            throw new InvalidModelException(box.line(), ErrorCode.TABLE_REPEAT_OVERFLOW, "La cabecera y el pie de la "
                    + "tabla " + box.describe() + " miden " + pt(bodyStart + footHeight) + " y no dejan sitio al cuerpo "
                    + "en una página vacía, y se repiten en cada página: hay que reducir su alto o quitar filas "
                    + "de Header rows y Footer rows");
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
        // Las franjas de las filas: las de la cabecera y el pie, ya colocadas; las del cuerpo, en su unidad.
        List<Fragment> headBands = new ArrayList<>();
        List<UnitBreaker.Band> bodyBands = new ArrayList<>();
        List<Fragment> footBands = new ArrayList<>();
        for (UnitBreaker.Band b : a.bands()) {
            Unit u = all.get(b.unit());
            if (b.unit() < head) {
                headBands.add(b.fragment(u.start(), u.end() - u.start(), true, true));
            } else if (b.unit() < bodyEnd) {
                bodyBands.add(b.inUnit(b.unit() - head));
            } else {
                footBands.add(b.fragment(u.start() - bodyBottom, u.end() - u.start(), true, true));
            }
        }
        UnitBreaker.Arranged bodyOnly = new UnitBreaker.Arranged(a.model(), body, a.outOfFlow(),
                all.slice(head, bodyEnd, bodyStart), bodyBands);
        UnitBreaker.Repeat repeat = new UnitBreaker.Repeat(headItems, bodyStart, footItems, footHeight, headBands,
                footBands);
        return UnitBreaker.fragment(context, box, space, from, bodyOnly, repeat);
    }
}
