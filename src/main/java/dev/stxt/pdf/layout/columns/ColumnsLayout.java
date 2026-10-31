package dev.stxt.pdf.layout.columns;

import dev.stxt.pdf.fragment.BreakAppeal;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.layout.AdjoiningMargins;
import dev.stxt.pdf.layout.BoxModel;
import dev.stxt.pdf.layout.ConstraintSpace;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.LayoutAlgorithm;
import dev.stxt.pdf.layout.LayoutContext;
import dev.stxt.pdf.layout.LayoutResult;
import dev.stxt.pdf.layout.Slice;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.SourceLine;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Stream;

import static dev.stxt.pdf.layout.Messages.pt;
import static dev.stxt.pdf.model.Tolerance.fits;
import static dev.stxt.pdf.tree.PropertyKey.ASPECT_RATIO;
import static dev.stxt.pdf.tree.PropertyKey.COUNT;
import static dev.stxt.pdf.tree.PropertyKey.HEIGHT;
import static dev.stxt.pdf.tree.PropertyKey.MAX_HEIGHT;

/**
 * Layout of a {@code Columns} (reference/components/columns.md): its content fills the first column,
 * goes on in the next one and, when the columns of the page run out, continues in the first column
 * of the next page.
 *
 * <p>{@code BoxTreeBuilder} puts the content in the column flow, a block box that is
 * the only child of the {@code Columns}. Each column is a fragment of that box: it is laid out with
 * {@code context.layout}, with the height of the column as what is left of the fragmentainer and the token
 * of the previous column as starting point. The algorithms that already know how to break do the
 * rest.</p>
 *
 * <pre>
 * height of the columns = what is left of the page for the content
 *                         and, with Height or Max height, what is left of it
 * fill in order: column i from the token of i−1, until the content, the columns
 *                or a forced page break run out
 * if the content ends here and Fill is balance: the minimum height in which it fits without worse breaks
 *                between columns (binary search)
 * no page or height to fill: always balanced
 * height of the content = that of the tallest column
 * </pre>
 *
 * <p>Breaks. A column break is one more break inside the flow; a page break
 * ({@link BreakAppeal#FORCED_PAGE}) stops filling columns and goes to the parent. If the first
 * column places nothing, the {@code Columns} breaks before itself; if another column places nothing,
 * the rest goes to the next page. The quality of the break is that of the last column.</p>
 *
 * <p>The content that does not fit in the columns is an error ({@link ErrorCode#BOX_OVERFLOW}) if it cannot
 * continue on another page: when the own height of the {@code Columns} ends on this one, or
 * when, without a page or a height to fill, there are more column breaks than columns.</p>
 */
public final class ColumnsLayout implements LayoutAlgorithm {

    /**
     * Precision of the search for the balanced height: the breaks change in steps (a line, a
     * box), so below it the columns no longer change.
     */
    private static final double PRECISION = 1e-3;

    private final LayoutContext context;

    public ColumnsLayout(LayoutContext context) {
        this.context = context;
    }

    /**
     * With {@code Count}, that number of columns of the narrowest or the natural width of the content,
     * plus the gaps; with {@code Min column width} the minimum is one column, because the number of
     * columns adapts to the width, and the maximum, columns of at least that width.
     */
    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        IntrinsicSizes content = context.intrinsic(flow(box));
        Style s = box.style();
        int count = s.columns().count() != null ? s.columns().count() : 1;
        int fewest = s.columns().minColumnWidth() == null ? count : 1;
        double width = s.columns().minColumnWidth() != null ? s.columns().minColumnWidth() : 0;
        return new IntrinsicSizes(fewest * content.narrowest() + (fewest - 1) * s.columns().columnGap(),
                count * Math.max(content.natural(), width) + (count - 1) * s.columns().columnGap());
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        BoxModel model = BoxModel.resolve(box, space, context);
        return new Run(box, model, space, (BreakToken.Block) from).layOut();
    }

    /** The column flow: the only child of the {@code Columns}. */
    private static Box flow(Box columnsBox) {
        return columnsBox.children().get(0);
    }

    /**
     * The columns filled with a height.
     *
     * @param columns fragments of the flow, one per column with content, already placed
     * @param token   where the flow continues; {@code null} if it has finished
     * @param appeal  quality of the break of the last column
     * @param worst   quality of the worst break between columns
     * @param before  the first column has placed nothing
     * @param height  height of the tallest column
     */
    private record Fill(List<Fragment> columns, BreakToken token, int appeal, int worst, boolean before,
                        double height) {

        boolean finished() {
            return !before && token == null;
        }

        /**
         * The content ends in the columns without exceeding {@code limit} and without breaks between
         * columns worse than {@code worst}: balancing does not break what can be left whole.
         */
        boolean fitsIn(double limit, int worst) {
            return finished() && fits(height, limit) && this.worst >= worst;
        }
    }

    /** Layout of one fragment of the {@code Columns}. */
    private final class Run {
        final Box box;
        final Box flow;
        final BoxModel model;
        final ConstraintSpace space;
        final boolean first;
        final double consumed;
        final BreakToken resume;
        /** The content finished on a previous page: only the box's own height is left. */
        final boolean contentDone;
        final Columns columns;

        Run(Box box, BoxModel model, ConstraintSpace space, BreakToken.Block from) {
            this.box = box;
            this.flow = flow(box);
            this.model = model;
            this.space = space;
            this.first = from == null;
            this.consumed = BreakToken.consumed(from);
            this.resume = first ? null : from.childToken();
            this.contentDone = !first && from.nextChild() > 0;
            Style s = box.style();
            this.columns = Columns.of(s.columns().count(), s.columns().minColumnWidth(), s.columns().columnGap(), model.contentWidth());
        }

        LayoutResult layOut() {
            if (contentDone) {
                return result(new Fill(List.of(), null, BreakAppeal.PERFECT_BREAK, BreakAppeal.FORCED_PAGE, false, 0));
            }
            double page = space.fragmenting() ? Slice.contentRemaining(model, first, space) : Double.POSITIVE_INFINITY;
            double own = ownRemaining();
            double limit = Math.min(page, own);
            if (Double.isInfinite(limit)) {
                // No page or height to fill: it is balanced, also with Fill: auto.
                double single = context.layout(flow, columnSpace(OptionalDouble.empty(), true), resume)
                        .fragment().height();
                Fill fill = fill(single, true);
                if (!fill.fitsIn(single, BreakAppeal.FORCED)) {
                    throw tooManyColumnBreaks();
                }
                return result(balance(fill, single, true));
            }
            // Without fragmentation (a stack item with Height), the columns are the only page there is.
            boolean atStart = !space.fragmenting() || space.atFragmentainerStart();
            Fill fill = fill(limit, atStart);
            if (fill.before()) {
                return LayoutResult.before(box);
            }
            if (!fill.finished() && own <= page) {
                // The Columns ends on this page and its content does not.
                throw tooTall(limit);
            }
            if (fill.fitsIn(limit, BreakAppeal.FORCED) && box.style().columns().balance()) {
                fill = balance(fill, limit, atStart);
            }
            return result(fill);
        }

        /**
         * What is left for the content of the box's own height: its {@code Height} (or the one the
         * parent imposes on it) or, if it has none, its {@code Max height}; infinite if it has neither.
         */
        double ownRemaining() {
            OptionalDouble cap = model.height().isPresent() ? model.height() : model.maxHeight();
            if (cap.isEmpty()) {
                return Double.POSITIVE_INFINITY;
            }
            // With clone, Slice.top is the top border and padding in all fragments, and the consumed height, the
            // height of the content of the previous ones; with slice, the consumed height includes the top of the
            // first one.
            return cap.getAsDouble() - consumed - Slice.top(model, first) - model.border().bottom()
                    - model.padding().bottom();
        }

        /**
         * Fills the columns in order with the height {@code height}, until the content ends,
         * the columns run out, a column places nothing or there is a forced page break.
         *
         * @param atStart the columns start the page: they must place something even if it does not fit
         */
        Fill fill(double height, boolean atStart) {
            List<Fragment> placed = new ArrayList<>();
            BreakToken token = resume;
            int appeal = BreakAppeal.PERFECT_BREAK;
            int worst = BreakAppeal.FORCED_PAGE;
            double tallest = 0;
            for (int i = 0; i < columns.count(); i++) {
                LayoutResult r = context.layout(flow, columnSpace(OptionalDouble.of(height), atStart), token);
                if (r.breakToken() instanceof BreakToken.Before) {
                    // What follows does not fit in a column of this height: nor in the others.
                    return new Fill(placed, token, appeal, worst, i == 0, tallest);
                }
                placed.add(r.fragment().moved(columns.x(i), 0));
                tallest = Math.max(tallest, r.fragment().height());
                token = r.breakToken();
                appeal = r.breakAppeal();
                if (token == null || appeal == BreakAppeal.FORCED_PAGE) {
                    break;
                }
                worst = Math.min(worst, appeal);
            }
            return new Fill(placed, token, appeal, worst, false, tallest);
        }

        /**
         * The distribution with the minimum height in which the content fits in the columns without breaks between
         * columns that violate more rules than those of {@code known}, the distribution that already fits in
         * {@code height} (if it has no breaks, none). It is a binary search between 0 and
         * {@code height}.
         */
        Fill balance(Fill known, double height, boolean atStart) {
            int worst = Math.min(known.worst(), BreakAppeal.PERFECT_BREAK);
            Fill best = known;
            double low = 0;
            double high = height;
            while (high - low > PRECISION) {
                double mid = (low + high) / 2;
                Fill f = fill(mid, atStart);
                if (f.fitsIn(mid, worst)) {
                    best = f;
                    high = mid;
                } else {
                    low = mid;
                }
            }
            return best;
        }

        /**
         * Space of a column: its width, without a definite height and, with fragmentation, the height of the
         * column as what is left of the fragmentainer.
         */
        ConstraintSpace columnSpace(OptionalDouble height, boolean atStart) {
            return ConstraintSpace.forChildren(columns.width(), OptionalDouble.empty(), height, atStart);
        }

        LayoutResult result(Fill fill) {
            boolean contentFinished = fill.finished();
            Slice slice = Slice.of(model, first, consumed, fill.height(), contentFinished, space);
            Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(),
                    slice.border(), slice.padding(), fill.columns(), List.of(), first, slice.finished());
            // The margins of the content do not come out of the columns (reference/components/block.md).
            if (slice.finished()) {
                return LayoutResult.finished(fragment, List.of(), AdjoiningMargins.NONE);
            }
            if (contentFinished) {
                // The content has finished, but the box's own height continues on the next page.
                return new LayoutResult(fragment, new BreakToken.Block(1, null, slice.consumed()),
                        BreakAppeal.PERFECT_BREAK, List.of(), AdjoiningMargins.NONE);
            }
            return new LayoutResult(fragment, new BreakToken.Block(0, fill.token(), slice.consumed()), fill.appeal(),
                    List.of(), AdjoiningMargins.NONE);
        }

        /** The content does not fit in the columns of the height left to the box, which ends on this page. */
        InvalidModelException tooTall(double limit) {
            Style s = box.style();
            String property = Stream.of(HEIGHT, MAX_HEIGHT, ASPECT_RATIO).filter(s::isDeclared).findFirst().orElse(null);
            return new InvalidModelException(property != null ? s.line(property) : SourceLine.of(box.line()),
                    ErrorCode.BOX_OVERFLOW,
                    "The content of Columns " + box.describe() + " does not fit in its height, which leaves "
                            + columns.count() + (columns.count() == 1 ? " column" : " columns") + " of "
                            + pt(Math.max(0, limit)) + ": its height has to be increased or the content reduced");
        }

        /** Without a page or a height to fill, the column breaks need more columns than there are. */
        InvalidModelException tooManyColumnBreaks() {
            Style s = box.style();
            return new InvalidModelException(s.isDeclared(COUNT) ? s.line(COUNT) : SourceLine.of(box.line()),
                    ErrorCode.BOX_OVERFLOW,
                    "Columns " + box.describe() + " has " + columns.count() + (columns.count() == 1 ? " column" : " columns")
                            + " and its column breaks need more: without a page or a height to fill, what does not "
                            + "fit cannot continue on another page. There are too many column breaks or too few "
                            + "columns");
        }
    }
}
