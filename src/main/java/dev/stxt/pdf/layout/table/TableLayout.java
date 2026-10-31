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
 * Layout of a {@code Table} (reference/components/table.md): that of its grid, with the rows of
 * {@code Header rows} and {@code Footer rows} repeated in each fragment.
 *
 * <p>{@code BoxTreeBuilder} translates the table into a grid with explicit placement; the first
 * {@link Box#headerRows()} rows of the grid are the header and the last {@link Box#footerRows()},
 * the footer. Here the whole grid is laid out without fragmenting ({@link GridLayout#arrange}) and only
 * the rows of the body are broken, with {@link UnitBreaker}: what is left of the page for them is the
 * rest after the header and the footer, which go whole in each fragment, the header on top and the footer
 * after the last placed row of the body. Without header or footer, it is the layout of the grid.</p>
 *
 * <p>If at the start of a page the header and the footer leave no room for the body, the table cannot
 * advance: it is an error ({@link ErrorCode#TABLE_REPEAT_OVERFLOW}). A row of the body that
 * does not fit in the room they leave is split in parallel, as in a grid.</p>
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
        // The header measures up to the start of the body and the footer from its end: both, with their gap.
        double bodyStart = all.get(head).start();
        double bodyBottom = all.get(bodyEnd - 1).end();
        double footHeight = box.footerRows() == 0 ? 0 : all.extent() - bodyBottom;
        if (head + box.footerRows() > 0 && space.fragmenting() && space.atFragmentainerStart()
                && Slice.contentRemaining(a.model(), from == null, space) - bodyStart - footHeight <= EPSILON) {
            throw new InvalidModelException(box.line(), ErrorCode.TABLE_REPEAT_OVERFLOW, "The header and footer of the "
                    + "table " + box.describe() + " measure " + pt(bodyStart + footHeight) + " and leave no room for the "
                    + "body on an empty page, and they repeat on every page: their height has to be reduced or rows "
                    + "removed from Header rows and Footer rows");
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
        // The bands of the rows: those of the header and the footer, already placed; those of the body, in their unit.
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
