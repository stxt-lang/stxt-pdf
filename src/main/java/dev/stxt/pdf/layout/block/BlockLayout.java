package dev.stxt.pdf.layout.block;

import dev.stxt.pdf.fragment.BreakAppeal;
import dev.stxt.pdf.fragment.BreakRules;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.layout.AdjoiningMargins;
import dev.stxt.pdf.layout.BoxModel;
import dev.stxt.pdf.layout.ConstraintSpace;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.LayoutAlgorithm;
import dev.stxt.pdf.layout.LayoutContext;
import dev.stxt.pdf.layout.LayoutResult;
import dev.stxt.pdf.layout.MarginStrut;
import dev.stxt.pdf.layout.PendingOverlay;
import dev.stxt.pdf.layout.Stacking;
import dev.stxt.pdf.layout.Slice;
import dev.stxt.pdf.layout.position.FrameOffset;
import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Exclusion;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Side;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.model.Tolerance.EPSILON;
import static dev.stxt.pdf.model.Tolerance.fits;
import static dev.stxt.pdf.model.Tolerance.same;

/**
 * Layout {@code block}: stacks the in-flow children vertically (reference/components/block.md).
 *
 * <pre>
 * cursor = 0; strut = empty
 * for each child:
 *     if Overlay/Page overlay: record PendingOverlay with the static position; continue
 *     if Side: place it whole at its side, at the flow position, below the side boxes of that side; continue
 *     r = context.layout(child, content box as reference container, with what the side boxes take beside it)
 *     strut += top margin of the child + those that come out of it at the top
 *     if nothing yet between the top border of the box and the child: the strut comes out of the box
 *     else: cursor += strut                                            // merging
 *     place the fragment at (left margin, cursor)                      // auto margins = center
 *     if Frame: shift the fragment by Offset
 *     cursor += height; strut = bottom margin of the child + those that come out of it at the bottom
 * Height auto = cursor (+ strut if it does not come out at the bottom) + padding + border, or the bottom of the
 * side boxes if they go lower
 * </pre>
 *
 * <p>The margins that come out of the box are returned in {@link AdjoiningMargins}: the parent merges them
 * with the box's own margins. An empty child lets its margins through. The cursor and the margins are kept by
 * {@link Stacking}, which the footnote area uses too.</p>
 *
 * <p><strong>Side boxes</strong> ({@code Side}, reference/components/block.md). A child with {@code Side} does not
 * take part in the vertical flow: it is placed whole at the left or at the right of the content box, at the position
 * the flow has when it is reached (below a side box of the same side that is still beside the flow), and the cursor
 * does not move. From then on, while a child overlaps it vertically, the child runs beside it or goes below it:
 * a text, and a box whose children can run beside it ({@link Box#wrapsBesideSideBoxes}), receives what the side
 * boxes take from each of its sides as {@link ConstraintSpace#exclusions()}, measured from its own border box, and
 * shortens its lines; any other child (a table, a stack, a text with background or border, a child with
 * {@code Clear}, a list beside a side box on the left) is placed below the side boxes. The exclusions of the space of the box itself, which its parent
 * gives it when the box runs beside the side boxes of the parent, count like side boxes of its own, except for its
 * height. A side box is not split: if it does not fit in what is left of the page it moves to the next one, and the
 * break between it and the child that follows it is as bad as one that separates two boxes kept together, so that
 * the side box goes with the first line that runs beside it. The exclusions end with the page: on the next one,
 * the text has the whole width.</p>
 *
 * <p><strong>Fragmentation</strong> (reference/common/breaks.md). Each child receives what is left of the page
 * from its top border. Before each child, if something has already been placed in this fragment, there is a
 * possible break with its quality ({@link BreakRules#between}); the state is saved to be able to go back to it.
 * When a child does not fit (it places nothing, it is broken inside or it overflows without being broken), the
 * last break of highest quality among the previous ones and the one inside the child is chosen. Forced breaks
 * apply as soon as they appear, also after an empty child, except at the start of a page. A child whose top border
 * would fall past the end of the page (its merged margins push it there) is not laid out: the break goes before it. In a non-forced break, the margins that touch it are truncated; in a forced one,
 * the top margin of the child that starts the page is kept. A child is placed where the margins that will come out of
 * it at the top put it, predicted from the tree ({@link LeadingMargins}); if the ones its layout returns differ and
 * move it, it is laid out again at its position only if the result can change: it moves up, what it placed does not
 * fit in what is left from there, or there are side boxes beside it ({@link Run#holdsAt}).</p>
 *
 * <p>A {@code List item} also carries its marker in the first fragment, hanging to its
 * left ({@link MarkerLayout}).</p>
 *
 * <p>Simplification: it only goes back to breaks <em>between</em> children; the layout of a previous child is
 * never repeated with less space to break it inside.</p>
 */
public final class BlockLayout implements LayoutAlgorithm {

    private final LayoutContext context;

    public BlockLayout(LayoutContext context) {
        this.context = context;
    }

    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        double min = 0;
        double max = 0;
        for (Box child : box.inFlowChildren()) {
            IntrinsicSizes i = context.intrinsic(child);
            min = Math.max(min, i.narrowest());
            max = Math.max(max, i.natural());
        }
        return new IntrinsicSizes(min, max);
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        BoxModel model = BoxModel.resolve(box, space, context);
        return new Run(box, model, space, (BreakToken.Block) from, 0).layOut();
    }

    /**
     * A possible break and the state of the fragment at that point, to go back to it.
     *
     * @param index      next child to place
     * @param childToken token of the child {@code index} if the break is inside it; {@code null}
     *                   if the break is before it
     * @param cursor     height of the content up to the break, without the margins that touch it
     * @param fragments  child fragments that are kept
     * @param pending    Overlay boxes that are kept
     */
    private record Mark(int index, BreakToken childToken, int appeal, double cursor, double occupied, int fragments,
                        int pending) {
    }

    /**
     * What a side box takes beside the flow, in the coordinates of the content box: the band from {@code top} to
     * {@code bottom} and the room from the left edge ({@code left}) and from the right edge ({@code right}) of the
     * content box, margins included (reference/components/block.md). The exclusions the box itself receives from its
     * parent are here too, without a fragment.
     *
     * @param fragment index of the fragment of the side box among the child fragments; -1 if it comes from the parent
     */
    private record Beside(double top, double bottom, double left, double right, int fragment) {

        boolean own() {
            return fragment >= 0;
        }

        boolean onLeft() {
            return left > EPSILON;
        }

        boolean onRight() {
            return right > EPSILON;
        }

        /** The band as an exclusion, measured from the content box of the block. */
        Exclusion exclusion() {
            return new Exclusion(top, bottom, left, right);
        }
    }

    /** Layout of one fragment of a box. */
    private final class Run {
        final Box box;
        final BoxModel model;
        final ConstraintSpace space;
        final BreakToken.Block from;
        final boolean first;
        final int start;
        final BreakToken resume;
        final double consumed;
        final boolean fragmenting;
        final ConstraintSpace childSpace;
        final double contentWidth;
        /**
         * What is left of the page from the start of the content box, for the children: without {@link #reserved}.
         */
        final double contentRemaining;
        /**
         * Room kept free at the end of the page for the bottom margin of the last child, when the box is laid out
         * again to break that child inside ({@link #finished}); 0 in the first layout.
         */
        final double reserved;

        final List<Fragment> fragments = new ArrayList<>();
        final List<PendingOverlay> pending = new ArrayList<>();
        final List<Mark> marks = new ArrayList<>();
        /** The side boxes beside the flow, own and inherited from the parent, in the order they were placed. */
        final List<Beside> beside = new ArrayList<>();
        /**
         * The cursor and the margins of the flow; while {@link Stacking#atTop}, nothing separates the top border of the
         * box from the current child.
         */
        final Stacking stack;
        /**
         * {@code Trim margins} (reference/components/block.md): the margins that touch the top of the box in its first
         * fragment ({@code trimTop}) and the bottom in its last one ({@code trimBottom}) are dropped; they neither stay
         * inside the box nor come out of it.
         */
        final boolean trimTop;
        final boolean trimBottom;
        /** There is already an in-flow child with content in this fragment (it does not let its margins through). */
        boolean progress;
        /** Last in-flow child placed in this fragment, for the quality of the next break. */
        Box previous;

        Run(Box box, BoxModel model, ConstraintSpace space, BreakToken.Block from, double reserved) {
            this.box = box;
            this.model = model;
            this.space = space;
            this.from = from;
            this.reserved = reserved;
            this.first = from == null;
            this.start = first ? 0 : from.nextChild();
            this.resume = first ? null : from.childToken();
            this.consumed = BreakToken.consumed(from);
            this.fragmenting = space.fragmenting();
            this.childSpace = model.childSpace(space, first);
            this.contentWidth = model.contentWidth();
            this.contentRemaining = fragmenting
                    ? childSpace.fragmentainerRemaining().getAsDouble() - reserved : Double.POSITIVE_INFINITY;
            // In a fragment that continues the box no margin comes out at the top.
            this.trimTop = box.style().trimMargins().top();
            this.trimBottom = box.style().trimMargins().bottom();
            // Trimming the top, the first child also starts at 0, as when its margins come out: they are dropped
            // (topOut).
            this.stack = new Stacking(first && (trimTop || (!box.isolatesMargins() && model.openAtTop())));
            inherit();
        }

        /**
         * The exclusions of the space of the box as side boxes of the content box ({@link
         * BoxModel#exclusionsInContent}), inherited: none is a fragment of this box.
         */
        void inherit() {
            for (Exclusion e : model.exclusionsInContent(space, first)) {
                beside.add(new Beside(e.top(), e.bottom(), e.left(), e.right(), -1));
            }
        }

        LayoutResult layOut() {
            for (int i = start; i < box.children().size(); i++) {
                Box child = box.children().get(i);
                if (child.style().position() == Position.FOOTNOTES) {
                    // The footnote area of a section: the paginator places it at the bottom of each page.
                    continue;
                }
                if (child.style().isOutOfFlow()) {
                    // Static position: where it would start if it were in flow
                    // (reference/components/frame-and-overlays.md). The box is resolved in its reference container
                    // (OverlayLayout).
                    pending.add(new PendingOverlay(child, 0, stack.top(MarginStrut.EMPTY)));
                    continue;
                }
                if (fragmenting && previous != null) {
                    // A forced break applies as soon as it appears, also after an empty child; only at the start of
                    // a page, with nothing placed before it, is it ignored (reference/common/breaks.md).
                    Break forced = BreakRules.forcedBetween(previous, child);
                    if (BreakRules.isForced(forced) && (progress || !space.atFragmentainerStart())) {
                        return broken(mark(i, BreakRules.forcedAppeal(forced)));
                    }
                    if (progress) {
                        // A side box goes with the child that follows it: the break between them is as bad as one
                        // between boxes kept together (reference/components/block.md).
                        int appeal = previous.style().isSideBox()
                                ? Math.min(BreakRules.between(previous, child), BreakAppeal.VIOLATES_KEEP_BETWEEN)
                                : BreakRules.between(previous, child);
                        marks.add(mark(i, appeal));
                    }
                }
                LayoutResult early = child.style().isSideBox() ? placeSide(child) : place(i, child);
                if (early != null) {
                    return early;
                }
            }
            return finished();
        }

        /** Places an in-flow child; returns the result of the box if it has to break here. */
        LayoutResult place(int i, Box child) {
            BreakToken childToken = i == start ? resume : null;
            Insets margin = Insets.resolve(child.style().box().margin(), contentWidth);
            // The child that starts a continuation fragment touches the break: its margins are
            // truncated, unless the break was forced. A resumed child no longer has a top margin.
            boolean noTopMargin = childToken != null || (!first && i == start && !forcedBeforeStart());
            // The child is placed where the margins that come out of it at the top will put it, predicted from the
            // tree (LeadingMargins), so that it is usually laid out once; what the layout returns decides, below.
            MarginStrut own = MarginStrut.EMPTY.add(noTopMargin ? 0 : margin.top());
            MarginStrut predicted = noTopMargin ? own : own.add(LeadingMargins.predict(child, childSpace, context));
            // A child that does not run beside the side boxes goes below them (reference/components/block.md).
            double flow = stack.top(predicted);
            boolean wraps = wraps(child, flow);
            double clear = beside.isEmpty() || wraps ? Double.NEGATIVE_INFINITY : clearance();
            double y = Math.max(flow, clear);
            if (noRoom(y)) {
                // Only the margins the layout confirms break before a child: a prediction that overestimates them (a
                // negative margin that does not come out) starts, as before, from the child's own margin.
                flow = stack.top(own);
                wraps = wraps(child, flow);
                clear = beside.isEmpty() || wraps ? Double.NEGATIVE_INFINITY : clearance();
                y = Math.max(flow, clear);
                if (noRoom(y)) {
                    return breakBefore();
                }
            }
            LayoutResult r = layOutChild(child, y, childToken, wraps);
            if (r.breakToken() instanceof BreakToken.Before) {
                return breakBefore();
            }
            AdjoiningMargins adjoining = r.adjoining();
            MarginStrut childTop = noTopMargin ? MarginStrut.EMPTY : adjoining.top().add(margin.top());
            // An empty box also lets its bottom margin merge with those around it.
            double flowY = Math.max(adjoining.collapsesThrough()
                    ? stack.top(childTop.add(margin.bottom()))
                    : stack.top(childTop), clear);
            if (!same(flowY, y) && (fragmenting || wraps)) {
                // The margins that come out of the child at the top are not the predicted ones and move it: what is
                // left of the page for it, and what is beside it, change, and the page may have run out. Moved up,
                // it has more room and may place more, so it is always laid out again.
                if (noRoom(flowY)) {
                    return breakBefore();
                }
                if (flowY < y || !holdsAt(r, flowY, wraps)) {
                    r = layOutChild(child, flowY, childToken, wraps);
                    if (r.breakToken() instanceof BreakToken.Before) {
                        return breakBefore();
                    }
                }
            }
            Fragment f = r.fragment();

            if (adjoining.collapsesThrough()) {
                // Empty box: its two margins merge with those around it and the cursor does not advance.
                stack.empty(childTop.add(margin.bottom()));
            } else {
                // Where the child ends: its top merged with the strut or, if it was cleared, the clearance.
                stack.place(childTop, flowY + f.height(), adjoining.bottom().add(margin.bottom()));
            }

            double x = Stacking.x(contentWidth, f.width(), child);
            // A Frame is shifted after it is placed; the cursor is not affected
            // (reference/components/frame-and-overlays.md).
            FrameOffset offset = FrameOffset.of(child.style(), contentWidth, childSpace.percentBaseHeight());
            place(r.placedAt(x + offset.dx(), flowY + offset.dy(), offset.dx(), offset.dy()));

            boolean hadProgress = progress;
            previous = child;
            progress |= !adjoining.collapsesThrough();
            if (!fragmenting) {
                return null;
            }
            if (r.breakToken() != null) {
                // Break inside the child: it competes with the previous breaks between children.
                // What the child takes up may be less than it measures (LayoutResult.occupied).
                marks.add(new Mark(i, r.breakToken(), BreakRules.inside(child, r.breakAppeal()),
                        stack.cursor(), flowY + r.occupied(childToken), fragments.size(), pending.size()));
                return broken(BreakRules.best(marks, Mark::appeal));
            }
            if (hadProgress && !fits(flowY + f.height(), contentRemaining)) {
                // The child is not broken and does not fit: the break goes before it (or earlier, if that is better).
                return broken(BreakRules.best(marks, Mark::appeal));
            }
            return null;
        }

        /** A child already placed: its fragment and the Overlay boxes it brings up. */
        void place(LayoutResult.Placement placed) {
            fragments.add(placed.fragment());
            pending.addAll(placed.pending());
        }

        /**
         * Places a side box (reference/components/block.md): whole, at its side of the content box, where the flow is
         * now or below the side boxes of that side (and, with {@code Clear}, of both), with its margins as the
         * separation from the text. The cursor does not move. If it does not fit in what is left of the page, the
         * box breaks before it, as before any child that does not fit.
         */
        LayoutResult placeSide(Box child) {
            Insets margin = Insets.resolve(child.style().box().margin(), contentWidth);
            boolean onLeft = child.style().side() == Side.LEFT;
            double y = Math.max(stack.top(MarginStrut.EMPTY), clearance(onLeft));
            if (child.style().clear()) {
                y = Math.max(y, clearance());
            }
            LayoutResult r = context.layout(child, childSpace.unfragmented(), null);
            Fragment f = r.fragment();
            double height = margin.top() + f.height() + margin.bottom();
            if (fragmenting && !fits(y + height, contentRemaining) && !(space.atFragmentainerStart() && !progress)) {
                return breakBefore();
            }
            double x = onLeft ? margin.left() : contentWidth - margin.right() - f.width();
            place(r.placedAt(x, y + margin.top()));
            beside.add(new Beside(y, y + height, onLeft ? x + f.width() + margin.right() : 0,
                    onLeft ? 0 : contentWidth - x + margin.left(), fragments.size() - 1));
            previous = child;
            progress = true;
            return null;
        }

        /**
         * Whether the child runs beside the side boxes ({@link Box#wrapsBesideSideBoxes}) from position {@code y}: a
         * list only beside the ones on the right, because its markers hang at its left, where a side box on the left
         * would be (reference/components/block.md).
         */
        boolean wraps(Box child, double y) {
            if (!child.wrapsBesideSideBoxes()) {
                return false;
            }
            if (child.role() == Element.Role.LIST) {
                for (Beside b : beside) {
                    if (b.onLeft() && b.bottom() > y + EPSILON) {
                        return false;
                    }
                }
            }
            return true;
        }

        /** The bottom of the side boxes beside the flow: where a child that does not run beside them goes. */
        double clearance() {
            double bottom = Double.NEGATIVE_INFINITY;
            for (Beside b : beside) {
                bottom = Math.max(bottom, b.bottom());
            }
            return bottom;
        }

        /** The bottom of the side boxes on one side: where a new side box of that side goes. */
        double clearance(boolean onLeft) {
            double bottom = Double.NEGATIVE_INFINITY;
            for (Beside b : beside) {
                if (onLeft ? b.onLeft() : b.onRight()) {
                    bottom = Math.max(bottom, b.bottom());
                }
            }
            return bottom;
        }

        /** The bottom of the side boxes of this fragment among its first {@code count} child fragments, or 0. */
        double sidesBottom(int count) {
            double bottom = 0;
            for (Beside b : beside) {
                if (b.own() && b.fragment() < count) {
                    bottom = Math.max(bottom, b.bottom());
                }
            }
            return bottom;
        }

        /**
         * What the side boxes take beside a child whose border box starts at ({@code childX}, {@code childY}) and is
         * {@code childWidth} wide, measured from that border box: the bands that reach below its top.
         */
        List<Exclusion> exclusionsFor(double childX, double childY, double childWidth) {
            List<Exclusion> exclusions = new ArrayList<>();
            for (Beside b : beside) {
                if (b.bottom() <= childY + EPSILON) {
                    continue;
                }
                Exclusion e = b.exclusion().shifted(childX, childY, contentWidth, childWidth);
                if (e.intrudes()) {
                    exclusions.add(e);
                }
            }
            return exclusions;
        }

        /**
         * Nothing of a child whose top border would be at {@code y} fits on this page. At the start of a page, with
         * nothing placed before it, something is placed anyway (progress guarantee): then there is always room.
         */
        boolean noRoom(double y) {
            return fragmenting && contentRemaining - y <= EPSILON && !(space.atFragmentainerStart() && !progress);
        }

        /**
         * The next child does not fit at all: the box breaks at the best break between the children already placed
         * or, if none has been placed in this fragment, before itself.
         */
        LayoutResult breakBefore() {
            return progress ? broken(BreakRules.best(marks, Mark::appeal)) : LayoutResult.before(box);
        }

        /**
         * Whether the layout of a child made higher up is also its layout with its top border at {@code y}: what it
         * placed fits in what is left of the page from {@code y}, and there is nothing beside it (the exclusions of
         * the side boxes depend on where the child is placed). With less room nothing new fits and what fitted still
         * fits, so the algorithms take the same decisions, also in a child that was broken: laying it out again
         * would compute the same subtree, with another key in the cache. A fragment that reaches the end of the page
         * (a definite height, a split row, the columns of a {@code Columns}, a broken child that fills the page) does
         * not fit from lower down, and the child is laid out again.
         */
        boolean holdsAt(LayoutResult r, double y, boolean wraps) {
            return !(wraps && !beside.isEmpty()) && fits(y + r.fragment().height(), contentRemaining);
        }

        /**
         * Layout of a child whose top border is at {@code y}; if it runs beside the side boxes ({@code wraps}), with
         * what they take from its sides, which depends on where it is placed.
         */
        LayoutResult layOutChild(Box child, double y, BreakToken childToken, boolean wraps) {
            ConstraintSpace s = fragmenting
                    ? childSpace.withRemaining(contentRemaining - y, space.atFragmentainerStart() && !progress)
                    : childSpace;
            if (wraps && !beside.isEmpty()) {
                double width = BoxModel.resolve(child, s, context).width();
                double x = Stacking.x(contentWidth, width, child);
                List<Exclusion> exclusions = exclusionsFor(x, y, width);
                if (!exclusions.isEmpty()) {
                    s = s.withExclusions(exclusions);
                }
            }
            return context.layout(child, s, childToken);
        }

        /** The break that opens this continuation fragment was forced. */
        boolean forcedBeforeStart() {
            Box next = box.children().get(start);
            for (int j = start - 1; j >= 0; j--) {
                Box before = box.children().get(j);
                if (!before.style().isOutOfFlow()) {
                    return BreakRules.isForced(BreakRules.forcedBetween(before, next));
                }
            }
            return BreakRules.isForced(BreakRules.forcedBefore(next));
        }

        Mark mark(int index, int appeal) {
            return new Mark(index, null, appeal, stack.cursor(), stack.cursor(), fragments.size(), pending.size());
        }

        /**
         * Breaks the box at {@code m}: what follows goes to the next fragment. The fragment reaches the bottom of its
         * side boxes if they go lower than the content placed.
         */
        LayoutResult broken(Mark m) {
            List<Fragment> kept = fragments.subList(0, m.fragments());
            List<PendingOverlay> keptPending = pending.subList(0, m.pending());
            // The margins that touch the break are truncated: the content ends at the cursor.
            double sides = sidesBottom(m.fragments());
            double content = Math.max(m.cursor(), sides);
            Slice slice = Slice.of(model, first, consumed, content, Math.max(m.occupied(), sides), false, space);
            Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(),
                    slice.border(), slice.padding(), withMarker(kept, slice), List.of(), first, false);
            BreakToken token = new BreakToken.Block(m.index(), m.childToken(), slice.consumed());
            return new LayoutResult(fragment, token, m.appeal(), keptPending,
                    new AdjoiningMargins(topOut(), MarginStrut.EMPTY, false));
        }

        /**
         * The child fragments and, in the first fragment of a {@code List item}, its marker
         * before them (reference/components/list.md): it goes with the first line, which is in this fragment if there
         * is any.
         */
        List<Fragment> withMarker(List<Fragment> children, Slice slice) {
            if (box.marker() == null || !first) {
                return children;
            }
            return MarkerLayout.place(box, children, slice.border(), slice.padding(), context)
                    .map(marker -> {
                        List<Fragment> all = new ArrayList<>(children);
                        all.add(0, marker);
                        return all;
                    })
                    .orElse(children);
        }

        /** The margins that come out of the box at the top: none if it trims those of its top, which drops them. */
        MarginStrut topOut() {
            return trimTop ? MarginStrut.EMPTY : stack.topOut();
        }

        /** All children placed. */
        LayoutResult finished() {
            boolean openAtBottom = !box.isolatesMargins() && model.openAtBottom();
            double cursor = stack.cursor();
            MarginStrut strut = stack.strut();
            if (!openAtBottom && !trimBottom && !stack.atTop() && fragmenting && reserved == 0 && !box.isLayoutRoot()
                    && fits(cursor, contentRemaining) && !fits(cursor + strut.resolve(), contentRemaining)
                    && endsWithContent(cursor + strut.resolve())) {
                // The box contains the margin at the end of its content, so the margin has to fit on the page like the
                // content does (reference/common/breaks.md): the break goes before the last child or, if nothing can
                // break, before the box. Not the root: its bottom is the end of the page, and the margin is truncated.
                if (!marks.isEmpty()) {
                    return broken(BreakRules.best(marks, Mark::appeal));
                }
                if (!space.atFragmentainerStart()) {
                    return LayoutResult.before(box);
                }
                // At the start of a page nothing can go before the box: the last child is broken inside, laying the
                // box out again with the room of the margin kept free for it. If the child cannot be broken either,
                // the box stays as it is, and whoever checks the overflow reports it.
                LayoutResult again = new Run(box, model, space, from, strut.resolve()).layOut();
                if (again.breakToken() != null && !(again.breakToken() instanceof BreakToken.Before)) {
                    return again;
                }
            }
            double contentHeight;
            AdjoiningMargins out;
            if (stack.atTop()) {
                // No child with content: the margins of the empty children come out at the top.
                contentHeight = 0;
                // (A box that trims the margins of its top is also here when it is not open at the top: it is not
                // empty then.) Those margins touch the top and the bottom of the box at once: trimming either side
                // drops them.
                boolean empty = openAtBottom && !box.isolatesMargins() && model.openAtTop() && model.usedHeight(0) == 0;
                out = new AdjoiningMargins(trimTop || trimBottom ? MarginStrut.EMPTY : strut, MarginStrut.EMPTY, empty);
            } else if (trimBottom) {
                // The margin below the last child is dropped: it neither stays inside nor comes out.
                contentHeight = cursor;
                out = new AdjoiningMargins(topOut(), MarginStrut.EMPTY, false);
            } else if (openAtBottom) {
                contentHeight = cursor;
                out = new AdjoiningMargins(topOut(), strut, false);
            } else {
                // The border, the padding, a fixed height or an own context contain the last margin.
                contentHeight = cursor + strut.resolve();
                out = new AdjoiningMargins(topOut(), MarginStrut.EMPTY, false);
            }
            double sides = sidesBottom(fragments.size());
            if (sides > 0 && sides >= contentHeight + (out.bottom().resolve()) - EPSILON) {
                // The side boxes go lower than the flow: the box reaches their bottom, and no margin comes out of
                // it below them (reference/components/block.md).
                contentHeight = Math.max(contentHeight, sides);
                out = new AdjoiningMargins(out.top(), MarginStrut.EMPTY, false);
            }

            Slice slice = Slice.of(model, first, consumed, contentHeight, true, space);
            if (!slice.finished() && !space.atFragmentainerStart()
                    && slice.height() <= Slice.top(model, first) + EPSILON) {
                // Nothing of the box's own height fits (what is left of the page is 0 or negative): a fragment with
                // only its top border and padding would be placed beyond the page. Away from the start of a page,
                // the parent breaks before the box instead.
                return LayoutResult.before(box);
            }
            Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(),
                    slice.border(), slice.padding(), withMarker(fragments, slice), List.of(), first, slice.finished());
            if (slice.finished()) {
                return LayoutResult.finished(fragment, pending, out);
            }
            // The content has finished, but the box's own height continues on the next page.
            BreakToken token = new BreakToken.Block(box.children().size(), null, slice.consumed());
            return new LayoutResult(fragment, token, BreakAppeal.PERFECT_BREAK, pending,
                    new AdjoiningMargins(out.top(), MarginStrut.EMPTY, false));
        }

        /**
         * Whether the box would end on this page with its content, {@code content} high with the margin at its end:
         * a box with {@code auto} height, always; one whose height its container gives it (an item stretched to its
         * row, reference/common/breaks.md), if that height does not go on after the content. A box with a
         * {@code Height} of its own has the height it has, whatever its content does.
         */
        boolean endsWithContent(double content) {
            if (model.height().isEmpty()) {
                return true;
            }
            return space.fixedHeight() && Slice.of(model, first, consumed, content, true, space).finished();
        }
    }
}
