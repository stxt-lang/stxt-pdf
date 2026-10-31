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
import dev.stxt.pdf.layout.Sizing;
import dev.stxt.pdf.layout.Slice;
import dev.stxt.pdf.layout.position.FrameOffset;
import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.tree.Box;

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
 *     r = context.layout(child, content box as reference container)
 *     strut += top margin of the child + those that come out of it at the top
 *     if nothing yet between the top border of the box and the child: the strut comes out of the box
 *     else: cursor += strut                                            // merging
 *     place the fragment at (left margin, cursor)                      // auto margins = center
 *     if Frame: shift the fragment by Offset
 *     cursor += height; strut = bottom margin of the child + those that come out of it at the bottom
 * Height auto = cursor (+ strut if it does not come out at the bottom) + padding + border
 * </pre>
 *
 * <p>The margins that come out of the box are returned in {@link AdjoiningMargins}: the parent merges them
 * with the box's own margins. An empty child lets its margins through.</p>
 *
 * <p><strong>Fragmentation</strong> (reference/common/breaks.md). Each child receives what is left of the page
 * from its top border. Before each child, if something has already been placed in this fragment, there is a
 * possible break with its quality ({@link BreakRules#between}); the state is saved to be able to go back to it.
 * When a child does not fit (it places nothing, it is broken inside or it overflows without being broken), the
 * last break of highest quality among the previous ones and the one inside the child is chosen. Forced breaks
 * apply as soon as they appear, also after an empty child, except at the start of a page. A child whose top border
 * would fall past the end of the page (its merged margins push it there) is not laid out: the break goes before it. In a non-forced break, the margins that touch it are truncated; in a forced one,
 * the top margin of the child that starts the page is kept.</p>
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
        return new Run(box, model, space, (BreakToken.Block) from).layOut();
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
    private record Mark(int index, BreakToken childToken, int appeal, double cursor, int fragments, int pending) {
    }

    /** Layout of one fragment of a box. */
    private final class Run {
        final Box box;
        final BoxModel model;
        final ConstraintSpace space;
        final boolean first;
        final int start;
        final BreakToken resume;
        final double consumed;
        final boolean fragmenting;
        final ConstraintSpace childSpace;
        final double contentWidth;
        /** What is left of the page from the start of the content box. */
        final double contentRemaining;

        final List<Fragment> fragments = new ArrayList<>();
        final List<PendingOverlay> pending = new ArrayList<>();
        final List<Mark> marks = new ArrayList<>();
        MarginStrut strut = MarginStrut.EMPTY;
        MarginStrut topOut = MarginStrut.EMPTY;
        /** While it is true, nothing separates the top border of the box from the current child. */
        boolean atTop;
        double cursor;
        /** There is already an in-flow child with content in this fragment (it does not let its margins through). */
        boolean progress;
        /** Last in-flow child placed in this fragment, for the quality of the next break. */
        Box previous;

        Run(Box box, BoxModel model, ConstraintSpace space, BreakToken.Block from) {
            this.box = box;
            this.model = model;
            this.space = space;
            this.first = from == null;
            this.start = first ? 0 : from.nextChild();
            this.resume = first ? null : from.childToken();
            this.consumed = BreakToken.consumed(from);
            this.fragmenting = space.fragmenting();
            this.childSpace = model.childSpace(space, first);
            this.contentWidth = model.contentWidth();
            this.contentRemaining = fragmenting
                    ? childSpace.fragmentainerRemaining().getAsDouble() : Double.POSITIVE_INFINITY;
            // In a fragment that continues the box no margin comes out at the top.
            this.atTop = first && !box.isolatesMargins() && model.openAtTop();
        }

        LayoutResult layOut() {
            for (int i = start; i < box.children().size(); i++) {
                Box child = box.children().get(i);
                if (child.style().isOutOfFlow()) {
                    // Static position: where it would start if it were in flow
                    // (reference/components/frame-and-overlays.md). The box is resolved in its reference container
                    // (OverlayLayout).
                    pending.add(new PendingOverlay(child, 0, atTop ? 0 : cursor + strut.resolve()));
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
                        marks.add(mark(i, BreakRules.between(previous, child)));
                    }
                }
                LayoutResult early = place(i, child);
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

            double y = top(MarginStrut.EMPTY.add(noTopMargin ? 0 : margin.top()));
            if (noRoom(y)) {
                return breakBefore();
            }
            LayoutResult r = layOutChild(child, y, childToken);
            if (r.breakToken() instanceof BreakToken.Before) {
                return breakBefore();
            }
            AdjoiningMargins adjoining = r.adjoining();
            MarginStrut childTop = noTopMargin ? MarginStrut.EMPTY : adjoining.top().add(margin.top());
            // An empty box also lets its bottom margin merge with those around it.
            double flowY = adjoining.collapsesThrough()
                    ? top(childTop.add(margin.bottom()))
                    : top(childTop);
            if (fragmenting && !same(flowY, y)) {
                // The margins that come out of the child at the top have moved it down: what is left of the page
                // for it changes, and it may have run out.
                if (noRoom(flowY)) {
                    return breakBefore();
                }
                r = layOutChild(child, flowY, childToken);
                if (r.breakToken() instanceof BreakToken.Before) {
                    return breakBefore();
                }
            }
            Fragment f = r.fragment();

            if (adjoining.collapsesThrough()) {
                // Empty box: its two margins merge with those around it and the cursor does not advance.
                strut = strut.add(childTop).add(margin.bottom());
            } else {
                strut = strut.add(childTop);
                if (atTop) {
                    topOut = strut;
                    atTop = false;
                } else {
                    cursor += strut.resolve();
                }
                cursor += f.height();
                strut = adjoining.bottom().add(margin.bottom());
            }

            double x = Sizing.horizontalMargins(contentWidth, f.width(),
                    child.style().box().margin().left(), child.style().box().margin().right())[0];
            // A Frame is shifted after it is placed; the cursor is not affected
            // (reference/components/frame-and-overlays.md).
            FrameOffset offset = FrameOffset.of(child.style(), contentWidth, childSpace.percentBaseHeight());
            double childX = x + offset.dx();
            double childY = flowY + offset.dy();
            fragments.add(f.moved(childX, childY, offset.dx(), offset.dy()));
            double dx = childX + f.border().left() + f.padding().left();
            double dy = childY + f.border().top() + f.padding().top();
            r.pending().forEach(p -> pending.add(p.translated(dx, dy)));

            boolean hadProgress = progress;
            previous = child;
            progress |= !adjoining.collapsesThrough();
            if (!fragmenting) {
                return null;
            }
            if (r.breakToken() != null) {
                // Break inside the child: it competes with the previous breaks between children.
                marks.add(new Mark(i, r.breakToken(), BreakRules.inside(child, r.breakAppeal()),
                        cursor, fragments.size(), pending.size()));
                return broken(BreakRules.best(marks, Mark::appeal));
            }
            if (hadProgress && !fits(flowY + f.height(), contentRemaining)) {
                // The child is not broken and does not fit: the break goes before it (or earlier, if that is better).
                return broken(BreakRules.best(marks, Mark::appeal));
            }
            return null;
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
         * Position of the top border of a child with these top margins, merged with the
         * current strut. If nothing separates it from the top border of the box, its margins come out and
         * it starts at 0.
         */
        double top(MarginStrut childTop) {
            return atTop ? 0 : cursor + strut.add(childTop).resolve();
        }

        LayoutResult layOutChild(Box child, double y, BreakToken childToken) {
            ConstraintSpace s = fragmenting
                    ? childSpace.withRemaining(contentRemaining - y, space.atFragmentainerStart() && !progress)
                    : childSpace;
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
            return new Mark(index, null, appeal, cursor, fragments.size(), pending.size());
        }

        /** Breaks the box at {@code m}: what follows goes to the next fragment. */
        LayoutResult broken(Mark m) {
            List<Fragment> kept = fragments.subList(0, m.fragments());
            List<PendingOverlay> keptPending = pending.subList(0, m.pending());
            // The margins that touch the break are truncated: the content ends at the cursor.
            Slice slice = Slice.of(model, first, consumed, m.cursor(), false, space);
            Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(),
                    slice.border(), slice.padding(), withMarker(kept, slice), List.of(), first, false);
            BreakToken token = new BreakToken.Block(m.index(), m.childToken(), slice.consumed());
            return new LayoutResult(fragment, token, m.appeal(), keptPending,
                    new AdjoiningMargins(topOut, MarginStrut.EMPTY, false));
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
            return MarkerLayout.place(box, children, slice.border(), slice.padding(), context.measurer())
                    .map(marker -> {
                        List<Fragment> all = new ArrayList<>(children);
                        all.add(0, marker);
                        return all;
                    })
                    .orElse(children);
        }

        /** All children placed. */
        LayoutResult finished() {
            boolean openAtBottom = !box.isolatesMargins() && model.openAtBottom();
            double contentHeight;
            AdjoiningMargins out;
            if (atTop) {
                // No child with content: the margins of the empty children come out at the top.
                contentHeight = 0;
                boolean empty = openAtBottom && model.usedHeight(0) == 0;
                out = new AdjoiningMargins(strut, MarginStrut.EMPTY, empty);
            } else if (openAtBottom) {
                contentHeight = cursor;
                out = new AdjoiningMargins(topOut, strut, false);
            } else {
                // The border, the padding, a fixed height or an own context contain the last margin.
                contentHeight = cursor + strut.resolve();
                out = new AdjoiningMargins(topOut, MarginStrut.EMPTY, false);
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
    }
}
