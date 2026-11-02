package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakAppeal;
import dev.stxt.pdf.fragment.BreakRules;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Rule;
import dev.stxt.pdf.fragment.Unit;
import dev.stxt.pdf.layout.position.FrameOffset;
import dev.stxt.pdf.model.Edges;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.tree.Box;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Fragmentation of the containers whose items are grouped in <em>units</em>
 * (reference/common/breaks.md): the lines of a stack container in {@code row}, the items of one in
 * {@code column} and the rows of a grid.
 *
 * <p>The container does its complete layout without fragmenting and then {@link #fragment} keeps
 * the units that fit on the page ({@link BreakRules#cutUnits}), shifted so that the
 * fragment starts at the first one. A unit that does not fit in what is left of the page is broken
 * <strong>in parallel</strong> on that page ({@code Split rows} and {@code Split items}, {@link Units#split}): each
 * item is laid out with fragmentation from its position, and the unit continues on the next page with the items that
 * have not finished, all from the top and without their top margin; it measures what the longest of them does and the
 * following units go after it. The units between which no break is possible (the rows taken up by the same item) are
 * broken together, as a <em>group</em>. The quality of the break is the worst of the items that are broken, and the
 * break between units wins if it is better: a unit whose items cannot respect their widows and orphans or a
 * {@code Keep together}, or in which some item cannot place anything, goes whole to the next page.</p>
 *
 * <p>With {@code Split rows: false} ({@code Split items: false}) every unit that does not fit goes whole to the next
 * page, and its items are only broken in parallel if it does not fit at the start of a page either, as the last
 * resort before overflowing ({@link Units#splittable}).</p>
 *
 * <p>A table repeats its header and footer in each fragment ({@link Repeat}): they go outside
 * the units, whole, and the units are broken in what they leave. The rows of a table with appearance or
 * {@code Id} are bands ({@link Band}) that follow their unit: whole, clipped at the end of the page if their group
 * is broken, and in the continuation of the group, that of its last unit.</p>
 */
public final class UnitBreaker {

    private UnitBreaker() {
    }

    /**
     * An item already placed in the unfragmented layout.
     *
     * @param box         the item
     * @param result      its layout
     * @param space       space of that layout, to repeat it with fragmentation if the item is broken
     * @param x           position of its border box, relative to the content box of the container
     *                    (includes the offset of a {@code Frame})
     * @param y           likewise
     * @param unit        unit it belongs to
     * @param frameOffset offset of a {@code Frame} included in {@code x} and {@code y}
     */
    public record Placed(Box box, LayoutResult result, ConstraintSpace space, double x, double y, int unit,
                         FrameOffset frameOffset) {
    }

    /**
     * The units of the container, with the positions of the unfragmented layout.
     *
     * @param list       the units, in order
     * @param extent     height of the complete content
     * @param splittable the items of a unit that does not fit at the start of a page can be broken in parallel, as the
     *                   last resort
     * @param split      {@code Split rows} or {@code Split items}: a unit that does not fit in what is left of the page
     *                   is broken in parallel on that page instead of going whole to the next one
     */
    public record Units(List<Unit> list, double extent, boolean splittable, boolean split) {

        public Units {
            list = List.copyOf(list);
        }

        public int size() {
            return list.size();
        }

        public Unit get(int index) {
            return list.get(index);
        }

        /**
         * The units {@code [from, to)} with the positions from {@code origin}; the content
         * ends at the end of the last one. The table uses it to break only its body.
         */
        public Units slice(int from, int to, double origin) {
            List<Unit> slice = list.subList(from, to).stream().map(u -> u.shifted(origin)).toList();
            return new Units(slice, list.get(to - 1).end() - origin, splittable, split);
        }
    }

    /**
     * A container laid out without fragmenting, before breaking it: produced by stack and grid.
     *
     * @param model     box model of the container
     * @param placed    placed items, in tree order
     * @param outOfFlow {@code Overlay} and {@code Page overlay} boxes of the container; they go with the first fragment
     * @param units     the units in which the items are grouped
     */
    public record Arranged(BoxModel model, List<Placed> placed, List<PendingOverlay> outOfFlow, Units units,
                           List<Band> bands) {

        public Arranged {
            bands = List.copyOf(bands);
        }

        /** A container without bands: those of stack and grid. */
        public Arranged(BoxModel model, List<Placed> placed, List<PendingOverlay> outOfFlow, Units units) {
            this(model, placed, outOfFlow, units, List.of());
        }
    }

    /**
     * The band of a unit: the row of a table with {@code Id} or {@code Box}
     * (reference/components/table.md#rows), which is painted under its cells with the height of the unit in each
     * fragment.
     *
     * @param box    the box of the row
     * @param unit   its unit
     * @param x      its left edge, from the content box of the container: that of the first column
     * @param width  its width: from the first column to the last
     * @param border its border
     */
    public record Band(Box box, int unit, double x, double width, Insets border) {

        /**
         * The fragment of the band at {@code y}, {@code height} high. When broken, with {@code Edges: open} (that
         * of its {@code Break}), the first piece has no bottom border and the following ones have no top border.
         */
        public Fragment fragment(double y, double height, boolean first, boolean last) {
            boolean clone = box.style().breaks().edges() == Edges.CLOSED;
            Insets sliced = clone ? border : new Insets(first ? border.top() : 0, border.right(),
                    last ? border.bottom() : 0, border.left());
            return new Fragment(box, x, y, width, height, Insets.ZERO, sliced, Insets.ZERO, List.of(), List.of(),
                    first, last);
        }

        /** The same band in another unit. */
        public Band inUnit(int unit) {
            return new Band(box, unit, x, width, border);
        }
    }

    /**
     * A unit as a fragment of the container shows it: whole, the part that fits at the end of the page if its group is
     * broken, or the continuation of a broken group (that of its last unit, like its band).
     *
     * @param unit   the unit
     * @param top    its top edge, from the content box of the fragment (under the repeated header)
     * @param height its height in the fragment
     * @param first  it starts in this fragment: it is not the continuation of a group broken on the previous page
     * @param last   it ends in this fragment
     */
    public record UnitPiece(int unit, double top, double height, boolean first, boolean last) {
    }

    /** The lines that a container draws in each of its fragments ({@link Fragment#rules}): those of a table. */
    @FunctionalInterface
    public interface Rules {
        /**
         * The lines of a fragment.
         *
         * @param units      the units placed in the fragment, in order
         * @param unitsEnd   the end of those units, from the content box: the repeated footer goes after it
         * @param children   the fragments of the items, of the bands and of the repeated rows, already placed
         */
        List<Rule> of(List<UnitPiece> units, double unitsEnd, List<Fragment> children);
    }

    /**
     * Items that are repeated whole in each fragment, outside the units: the header of a
     * table at the top and its footer under the placed units (reference/components/table.md). They are never
     * broken, and what is left of the page for the units is the rest.
     *
     * @param head       items of the header, with their position from the content box
     * @param headHeight height of the header, with the gap that separates it from the units
     * @param foot       items of the footer, with their position from the end of the units
     * @param footHeight height of the footer, with the gap that separates it from the units
     * @param headBands  bands of the header rows, placed like their items
     * @param footBands  bands of the footer rows, placed like their items
     * @param rules      the lines of each fragment, those of the merged borders of a table
     *                   (reference/components/table.md#merged-borders); {@code null} without them
     */
    public record Repeat(List<Placed> head, double headHeight, List<Placed> foot, double footHeight,
                         List<Fragment> headBands, List<Fragment> footBands, Rules rules) {
        public static final Repeat NONE = new Repeat(List.of(), 0, List.of(), 0);

        /** Without bands. */
        public Repeat(List<Placed> head, double headHeight, List<Placed> foot, double footHeight) {
            this(head, headHeight, foot, footHeight, List.of(), List.of());
        }

        /** Without lines. */
        public Repeat(List<Placed> head, double headHeight, List<Placed> foot, double footHeight,
                      List<Fragment> headBands, List<Fragment> footBands) {
            this(head, headHeight, foot, footHeight, headBands, footBands, null);
        }
    }

    /**
     * Fragment of the container in what is left of the page, or the whole container without
     * fragmentation.
     *
     * @param from     token of the container; {@code null} in its first fragment
     * @param arranged the container laid out without fragmenting
     */
    public static LayoutResult fragment(LayoutContext context, Box box, ConstraintSpace space, BreakToken from,
                                        Arranged arranged) {
        return fragment(context, box, space, from, arranged, Repeat.NONE);
    }

    /**
     * Like {@link #fragment(LayoutContext, Box, ConstraintSpace, BreakToken, Arranged)}, with items that are repeated
     * in each fragment.
     */
    public static LayoutResult fragment(LayoutContext context, Box box, ConstraintSpace space, BreakToken from,
                                        Arranged arranged, Repeat repeat) {
        return new Run(context, box, space, (BreakToken.Container) from, arranged, repeat).layOut();
    }

    /** An item in this fragment: its fragment already placed and its Overlay boxes. */
    private record Piece(Fragment fragment, List<PendingOverlay> pending) {
    }

    /**
     * The items of a group broken in parallel.
     *
     * @param pieces   index in {@code placed} → item placed on this page
     * @param tokens   token of each item of the group, in order; {@code null} if it has finished
     * @param bottom   end of the longest one, from the content box of the fragment
     * @param occupied end of what the items take up ({@link LayoutResult#occupied}); at most {@code bottom}
     * @param broken   some item continues on the next page
     */
    private record Split(Piece[] pieces, List<BreakToken> tokens, int appeal, double bottom, double occupied,
                         boolean broken) {
    }

    /** A fragment of a container. */
    private static final class Run {
        final LayoutContext context;
        final Box box;
        final BoxModel model;
        final ConstraintSpace space;
        final BreakToken.Container from;
        final boolean first;
        final List<Placed> placed;
        final List<PendingOverlay> outOfFlow;
        final Units units;
        final List<Band> bands;
        final Repeat repeat;
        final int n;
        /** What is left of the page for the units; without fragmentation there is no limit. */
        final double remaining;
        /** Item of each index of {@code placed} placed in this fragment. */
        final Piece[] pieces;
        /** Fragment of each band of {@code bands} in this fragment. */
        final Fragment[] bandPieces;
        /** What this fragment shows of each unit; {@code null} if nothing. */
        final UnitPiece[] unitPieces;
        /** First unit left to place. */
        int next;
        /**
         * Position in the fragment = unfragmented position − origin. The first fragment keeps
         * the initial gap (alignment); the following ones start at their first unit or, if only
         * the box's own height is left, at the end of the content.
         */
        double origin;
        /** End of the continuation of the group broken on the previous page; 0 if there is none. */
        double top;
        /** The fragment starts with the continuation of a broken group. */
        boolean continued;

        Run(LayoutContext context, Box box, ConstraintSpace space, BreakToken.Container from, Arranged arranged,
            Repeat repeat) {
            this.context = context;
            this.box = box;
            this.model = arranged.model();
            this.space = space;
            this.from = from;
            this.first = from == null;
            this.placed = arranged.placed();
            this.outOfFlow = arranged.outOfFlow();
            this.units = arranged.units();
            this.bands = arranged.bands();
            this.repeat = repeat;
            this.n = units.size();
            this.remaining = space.fragmenting()
                    ? Slice.contentRemaining(model, first, space) - repeat.headHeight() - repeat.footHeight()
                    : Double.POSITIVE_INFINITY;
            this.pieces = new Piece[placed.size()];
            this.bandPieces = new Fragment[bands.size()];
            this.unitPieces = new UnitPiece[n];
        }

        LayoutResult layOut() {
            next = first ? 0 : from.nextLine();
            origin = first ? 0 : next < n ? units.get(next).start() : units.extent();
            if (!first && !from.itemTokens().isEmpty()) {
                LayoutResult r = continueGroup();
                if (r != null) {
                    return r;
                }
            }
            if (!space.fragmenting() || next == n) {
                return place(n, BreakAppeal.PERFECT_BREAK);
            }
            return cut();
        }

        /**
         * The group broken on the previous page continues from the top, and the following units
         * go after the longest item. The result if the fragment ends with the group (or nothing
         * fits); {@code null} if it goes on with the following units.
         */
        LayoutResult continueGroup() {
            int end = groupEnd(next);
            Split s = split(next, end, from.itemTokens(), 0, space.atFragmentainerStart());
            if (s == null) {
                return LayoutResult.before(box);
            }
            commit(s);
            // The continuation of the group carries the band of its last unit.
            for (int b = 0; b < bands.size(); b++) {
                if (bands.get(b).unit() == end - 1) {
                    bandPieces[b] = bands.get(b).fragment(repeat.headHeight(), s.bottom(), false, !s.broken());
                }
            }
            unitPieces[end - 1] = new UnitPiece(end - 1, repeat.headHeight(), s.bottom(), false, !s.broken());
            if (s.broken()) {
                return result(s.bottom(), s.occupied(), false, next, s.tokens(), s.appeal());
            }
            origin = bottom(next, end) - s.bottom();
            top = s.bottom();
            next = end;
            continued = true;
            return null;
        }

        /**
         * The units from {@code next} that fit in what is left of the page. If the first group
         * does not fit even at the start of a page, its items are broken in parallel; with {@link Units#split}, so
         * is the group that does not fit in what is left of the page.
         */
        LayoutResult cut() {
            boolean mustPlace = space.atFragmentainerStart() && !continued;
            int groupEnd = groupEnd(next);
            if (mustPlace && units.splittable() && !fits(bottom(next, groupEnd) - origin, remaining)) {
                Split s = split(next, groupEnd, null, origin, space.atFragmentainerStart());
                if (s != null && s.broken()) {
                    commit(s);
                    splitBands(next, groupEnd, s.bottom());
                    return result(s.bottom(), s.occupied(), false, next, s.tokens(), s.appeal());
                }
            }
            BreakRules.Cut cut = BreakRules.cutUnits(units.list(), next,
                    remaining - (units.get(next).start() - origin), mustPlace);
            if (units.split() && units.splittable() && cut.end() < n) {
                LayoutResult r = splitOnThisPage(cut, mustPlace);
                if (r != null) {
                    return r;
                }
            }
            if (cut.end() == next) {
                // Only the continuation of the group fits, if there is one: the break goes after it.
                return continued
                        ? result(top, top, false, next, List.of(), units.get(next).appealBefore())
                        : LayoutResult.before(box);
            }
            return place(cut.end(), cut.appeal());
        }

        /**
         * {@code Split rows} and {@code Split items}: the group with the first unit that does not fit in what is
         * left of the page is broken in parallel on this page, with the units before it placed whole, if every item
         * can place something and the break is at least as good as {@code cut}, the best one between units: being
         * the last one, it then wins (reference/common/breaks.md). {@code null} if the units are cut as {@code cut}
         * says.
         *
         * @param mustPlace the first group was already tried at the start of the page, as the last resort
         */
        LayoutResult splitOnThisPage(BreakRules.Cut cut, boolean mustPlace) {
            int first = next;
            while (first < n && fits(units.get(first).end() - origin, remaining)) {
                first++;
            }
            if (first == n) {
                return null;
            }
            int start = first;
            while (start > next && !units.get(start).breakableBefore()) {
                start--;
            }
            if (start == next && mustPlace) {
                return null;
            }
            int end = groupEnd(start);
            Split s = split(start, end, null, origin, false);
            if (s == null || !s.broken() || s.appeal() < cut.appeal()) {
                return null;
            }
            placeWhole(next, start);
            commit(s);
            splitBands(start, end, s.bottom());
            return result(s.bottom(), s.occupied(), false, start, s.tokens(), s.appeal());
        }

        /** Places the units {@code [from, to)} whole, with their bands. */
        void placeWhole(int from, int to) {
            for (int k = 0; k < placed.size(); k++) {
                Placed p = placed.get(k);
                if (p.unit() >= from && p.unit() < to) {
                    pieces[k] = piece(p, p.result(), p.y() - origin + repeat.headHeight());
                }
            }
            for (int b = 0; b < bands.size(); b++) {
                int u = bands.get(b).unit();
                if (u >= from && u < to) {
                    Unit unit = units.get(u);
                    bandPieces[b] = bands.get(b).fragment(unit.start() - origin + repeat.headHeight(),
                            unit.end() - unit.start(), true, true);
                }
            }
            for (int u = from; u < to; u++) {
                Unit unit = units.get(u);
                unitPieces[u] = new UnitPiece(u, unit.start() - origin + repeat.headHeight(), unit.end() - unit.start(),
                        true, true);
            }
        }

        /** Places the units {@code [next, end)} whole and closes the fragment. */
        LayoutResult place(int end, int appeal) {
            placeWhole(next, end);
            boolean contentFinished = end == n;
            double contentHeight = contentFinished
                    ? Math.max(top, units.extent() - origin)
                    : bottom(next, end) - origin;
            return result(contentHeight, contentHeight, contentFinished, end, List.of(), appeal);
        }

        /**
         * Breaks in parallel the items of the units {@code [start, end)}; {@code null} if any of them
         * cannot place anything.
         *
         * @param tokens  token of each item to resume them from the top; {@code null} if the group
         *                starts on this page, each item at its position
         * @param origin  see {@link #origin}; only if the group starts on this page
         * @param atStart the items start the page: they have to place something even if it overflows
         */
        Split split(int start, int end, List<BreakToken> tokens, double origin, boolean atStart) {
            Piece[] split = new Piece[placed.size()];
            List<BreakToken> next = new ArrayList<>();
            int appeal = BreakAppeal.PERFECT_BREAK;
            double bottom = 0;
            double occupied = 0;
            boolean broken = false;
            int j = 0;
            for (int k = 0; k < placed.size(); k++) {
                Placed p = placed.get(k);
                if (p.unit() < start || p.unit() >= end) {
                    continue;
                }
                BreakToken token = tokens == null ? null : tokens.get(j);
                j++;
                if (tokens != null && token == null) {
                    // It finished on a previous page.
                    next.add(null);
                    continue;
                }
                // The item is broken at its place in the flow, without the offset of a Frame, which is applied
                // afterwards (reference/components/frame-and-overlays.md).
                double dy = p.frameOffset().dy();
                double y = tokens == null ? p.y() - dy - origin : 0;
                LayoutResult r = context.layout(p.box(), p.space().fragmented(remaining - y, atStart), token);
                if (r.breakToken() instanceof BreakToken.Before) {
                    return null;
                }
                split[k] = piece(p, r, y + dy + repeat.headHeight());
                next.add(r.breakToken());
                if (!r.isFinished()) {
                    broken = true;
                    appeal = Math.min(appeal, BreakRules.inside(p.box(), r.breakAppeal()));
                }
                bottom = Math.max(bottom, y + r.fragment().height());
                occupied = Math.max(occupied, y + r.occupied(token));
            }
            return new Split(split, next, appeal, bottom, occupied, broken);
        }

        /**
         * The bands of the units {@code [start, end)} of a group that is broken on this page and ends at
         * {@code bottom}: each one from its start, clipped at {@code bottom}.
         */
        void splitBands(int start, int end, double bottom) {
            for (int b = 0; b < bands.size(); b++) {
                int u = bands.get(b).unit();
                if (u < start || u >= end) {
                    continue;
                }
                double top = units.get(u).start() - origin;
                double unitBottom = units.get(u).end() - origin;
                if (fits(bottom, top)) {
                    continue;
                }
                bandPieces[b] = bands.get(b).fragment(top + repeat.headHeight(), Math.min(unitBottom, bottom) - top,
                        true, fits(unitBottom, bottom));
            }
            for (int u = start; u < end; u++) {
                double top = units.get(u).start() - origin;
                double unitBottom = units.get(u).end() - origin;
                if (!fits(bottom, top)) {
                    unitPieces[u] = new UnitPiece(u, top + repeat.headHeight(), Math.min(unitBottom, bottom) - top,
                            true, fits(unitBottom, bottom));
                }
            }
        }

        void commit(Split s) {
            for (int k = 0; k < pieces.length; k++) {
                if (s.pieces()[k] != null) {
                    pieces[k] = s.pieces()[k];
                }
            }
        }

        /** Places the fragment of an item at {@code y} of the container's fragment. */
        Piece piece(Placed p, LayoutResult r, double y) {
            Fragment f = r.fragment();
            double dx = p.x() + f.border().left() + f.padding().left();
            double dy = y + f.border().top() + f.padding().top();
            return new Piece(f.moved(p.x(), y, p.frameOffset().dx(), p.frameOffset().dy()),
                    r.pending().stream().map(q -> q.translated(dx, dy)).toList());
        }

        /**
         * An item that is repeated: its fragment goes in all of them, but its Overlay boxes only in the
         * first one, like those of the container.
         */
        void repeated(Piece piece, List<Fragment> fragments, List<PendingOverlay> pending) {
            fragments.add(piece.fragment());
            if (first) {
                pending.addAll(piece.pending());
            }
        }

        /** First unit after the group that starts at {@code start}: the next one before which a break is possible. */
        int groupEnd(int start) {
            int end = start + 1;
            while (end < n && !units.get(end).breakableBefore()) {
                end++;
            }
            return end;
        }

        /** End of the lowest unit of {@code [start, end)} in the unfragmented layout. */
        double bottom(int start, int end) {
            double bottom = units.get(start).start();
            for (int u = start; u < end; u++) {
                bottom = Math.max(bottom, units.get(u).end());
            }
            return bottom;
        }

        /**
         * The fragment with the placed items.
         *
         * @param contentHeight end of the items placed, from the content box of the fragment
         * @param occupied      end of what they take up ({@link LayoutResult#occupied}); at most {@code contentHeight}
         * @param nextLine      unit at which the container continues if it does not finish
         * @param itemTokens    tokens of the items of a group broken in parallel, or empty
         */
        LayoutResult result(double contentHeight, double occupied, boolean contentFinished, int nextLine,
                            List<BreakToken> itemTokens, int appeal) {
            List<Fragment> fragments = new ArrayList<>();
            List<PendingOverlay> pending = new ArrayList<>(first ? outOfFlow : List.of());
            double units = Math.max(0, contentHeight);
            // The bands of the rows, before the cells of their group so they are painted underneath.
            fragments.addAll(repeat.headBands());
            for (Placed p : repeat.head()) {
                repeated(piece(p, p.result(), p.y()), fragments, pending);
            }
            for (Fragment band : bandPieces) {
                if (band != null) {
                    fragments.add(band);
                }
            }
            for (Piece piece : pieces) {
                if (piece != null) {
                    fragments.add(piece.fragment());
                    pending.addAll(piece.pending());
                }
            }
            for (Fragment band : repeat.footBands()) {
                fragments.add(band.moved(band.x(), repeat.headHeight() + units + band.y()));
            }
            for (Placed p : repeat.foot()) {
                repeated(piece(p, p.result(), repeat.headHeight() + units + p.y()), fragments, pending);
            }
            Slice slice = Slice.of(model, first, BreakToken.consumed(from),
                    repeat.headHeight() + units + repeat.footHeight(),
                    repeat.headHeight() + Math.max(0, occupied) + repeat.footHeight(), contentFinished, space);
            Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(),
                    slice.border(), slice.padding(), fragments, List.of(), first, slice.finished());
            if (repeat.rules() != null) {
                List<UnitPiece> shown = new ArrayList<>();
                for (UnitPiece u : unitPieces) {
                    if (u != null) {
                        shown.add(u);
                    }
                }
                fragment = fragment.withRules(repeat.rules().of(shown, repeat.headHeight() + units, fragments));
            }
            // The margins of the items never merge with those of the container (reference/components/block.md).
            if (slice.finished()) {
                return LayoutResult.finished(fragment, pending, AdjoiningMargins.NONE);
            }
            BreakToken token = new BreakToken.Container(nextLine, itemTokens, slice.consumed());
            return new LayoutResult(fragment, token, contentFinished ? BreakAppeal.PERFECT_BREAK : appeal,
                    pending, AdjoiningMargins.NONE);
        }
    }
}
