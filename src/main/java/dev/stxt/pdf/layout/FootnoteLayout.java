package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakAppeal;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.InternalException;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.Section;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * A page of a section with its footnotes (reference/components/footnote.md): the flow of the page and, at the bottom
 * of the page area, the footnote area of the section with the notes called on it.
 *
 * <p>The notes take room from the flow, and which notes there are depends on how much flow fits: the page is laid out
 * reserving a height {@code R} for the notes, the calls that fall in it are looked up in its lines, and the reservation
 * is adjusted until it is the <strong>smallest</strong> height in which the notes of those calls fit
 * ({@link #reserve}). A line enters the page only if its note starts under it: a note may continue on the next page,
 * before the notes of that page, if the break respects its widows, orphans and keeps; what does not fit otherwise
 * moves to the next page with the line that calls it. The remaining room, if the notes are shorter than the
 * reservation, stays between the text and the notes.</p>
 *
 * <p>Each probe lays out the page again with another reservation. Less room means less flow, so the calls of a
 * larger reservation are a subset of those of a smaller one: the first reservation that fits is found going up to the
 * full height of the notes, and then it is tightened going down, with a binary search where a smaller one brings a
 * call back ({@link #PRECISION}). Each note is laid out with the algorithms of the flow, so the layout cache answers
 * the repeated probes.</p>
 */
final class FootnoteLayout {

    /** Precision of the search for the smallest reservation: below it, no line changes page. */
    private static final double PRECISION = 1e-3;
    /** Probes a page admits before its reservation is considered not to settle. */
    private static final int MAX_PROBES = 60;

    /**
     * A note broken at the end of a page: the rest continues at the top of the footnote area of the next one.
     *
     * @param note  the box of the note
     * @param token where its layout continues
     */
    record Carry(Box note, BreakToken token) {
    }

    /**
     * A page: the flow and, if the page has footnotes, their area.
     *
     * @param body      the result of the flow of the section on this page
     * @param footnotes fragment of the footnote area, relative to the page area; {@code null} without notes
     * @param carry     the note that continues on the next page; {@code null} if none
     */
    record Result(LayoutResult body, Fragment footnotes, Carry carry) {
    }

    /**
     * A probe: the page laid out with a reservation and the notes stacked in it, if they fit.
     *
     * @param overflows the flow does not fit in what the reservation leaves it: at the start of a page something is
     *                  placed even if it does not fit (reference/common/breaks.md), so the reservation is too big
     * @param stacked   the notes of the calls of the page, stacked in the reservation; {@code null} if they do not fit
     */
    private record Probe(double reserved, LayoutResult body, List<String> calls, boolean overflows, Stacked stacked) {

        boolean feasible() {
            return !overflows && stacked != null;
        }

        /** The reservation the stacked notes need: less if they are shorter, the same if one continues. */
        double need(BoxModel model) {
            if (stacked.fragments().isEmpty()) {
                return 0;
            }
            if (stacked.carry() != null) {
                return reserved;
            }
            return model.margin().vertical() + model.border().vertical() + model.padding().vertical()
                    + stacked.height();
        }
    }

    /**
     * The notes stacked in the content box of the area.
     *
     * @param fragments the fragments of the notes, placed in the content box of the area
     * @param height    height of the content they take up
     * @param carry     the note that does not end here, if any
     */
    private record Stacked(List<Fragment> fragments, double height, Carry carry) {
    }

    private final Engine engine;
    private final Section section;
    private final Box area;
    private final Map<String, Box> notes;
    /** Probes of the page being laid out, against {@link #MAX_PROBES}. */
    private int probes;

    FootnoteLayout(Engine engine, Section section) {
        this.engine = engine;
        this.section = section;
        this.area = section.footnotes();
        this.notes = area.children().stream().collect(Collectors.toMap(Box::id, Function.identity()));
    }

    /**
     * Lays out a page of the section from {@code from}, with {@code carry}, the note that continues from the previous
     * page, if any.
     */
    Result page(BreakToken from, Carry carry) {
        if (!section.hasFootnotes()) {
            return new Result(engine.layout(section.box(), ConstraintSpace.forPage(section.pageSetup()), from), null,
                    null);
        }
        Rect pageArea = section.pageSetup().pageArea();
        BoxModel model = BoxModel.resolve(area, ConstraintSpace.fixed(pageArea.width(), OptionalDouble.empty(),
                pageArea.width(), OptionalDouble.of(pageArea.height()), true), engine);
        probes = 0;
        Probe p = reserve(from, carry, model, pageArea.height());
        if (p.stacked().fragments().isEmpty()) {
            return new Result(p.body(), null, null);
        }
        Stacked stacked = p.stacked();
        // The area is as tall as its notes, at the bottom of the page: the room the reservation leaves over, if any,
        // stays between the text and the rule.
        double height = model.border().vertical() + model.padding().vertical() + stacked.height();
        double x = Sizing.horizontalMargins(pageArea.width(), model.width(), area.style().box().margin().left(),
                area.style().box().margin().right())[0];
        double y = pageArea.height() - model.margin().bottom() - height;
        Fragment fragment = new Fragment(area, x, y, model.width(), height, model.margin(), model.border(),
                model.padding(), stacked.fragments(), List.of(), true, true);
        return new Result(p.body(), fragment, stacked.carry());
    }

    /**
     * The smallest reservation in which the notes called on the page fit. Without a reservation the page is as
     * always; if its notes do not fit, the full height of those notes is a reservation in which the notes of any
     * larger one fit, unless it leaves the flow no room (then the largest the flow tolerates is tried). From a
     * reservation that fits, it is tightened: down to what its notes need and, where a smaller one brings back a call
     * that does not fit, by a binary search between the two.
     */
    private Probe reserve(BreakToken from, Carry carry, BoxModel model, double pageHeight) {
        Probe low = probe(0, from, carry, model, pageHeight);
        if (low.feasible()) {
            return low;
        }
        if (low.overflows()) {
            throw tooTall(carry, low.calls());
        }
        double hi = Math.min(pageHeight, total(carry, low.calls(), model));
        Probe high = probe(hi, from, carry, model, pageHeight);
        if (high.overflows()) {
            // The flow does not fit in what the notes leave: the largest reservation that does not force it.
            double a = 0;
            double b = hi;
            high = low;
            while (b - a > PRECISION) {
                double mid = (a + b) / 2;
                Probe m = probe(mid, from, carry, model, pageHeight);
                if (m.overflows()) {
                    b = mid;
                } else {
                    a = mid;
                    high = m;
                }
            }
        }
        if (!high.feasible()) {
            throw tooTall(carry, high.calls());
        }
        // Down to what the notes need, while it still fits.
        double lo = 0;
        Probe best = high;
        while (true) {
            double need = best.need(model);
            if (need >= best.reserved() - PRECISION) {
                return best;
            }
            Probe m = probe(need, from, carry, model, pageHeight);
            if (!m.feasible()) {
                lo = need;
                break;
            }
            best = m;
        }
        // Between a reservation that does not fit and one that does: the smallest that does.
        double b = best.reserved();
        while (b - lo > PRECISION) {
            double mid = (lo + b) / 2;
            Probe m = probe(mid, from, carry, model, pageHeight);
            if (m.feasible()) {
                best = m;
                b = mid;
            } else {
                lo = mid;
            }
        }
        return best;
    }

    /** The page with {@code reserved} points for the notes, and the notes of its calls stacked in them if they fit. */
    private Probe probe(double reserved, BreakToken from, Carry carry, BoxModel model, double pageHeight) {
        if (++probes > MAX_PROBES) {
            throw new InternalException("The reservation for the footnotes does not settle after " + MAX_PROBES
                    + " probes (reference/components/footnote.md)");
        }
        LayoutResult body = engine.layout(section.box(), ConstraintSpace.forPage(section.pageSetup(), reserved), from);
        List<String> calls = new ArrayList<>(calls(body.fragment(), new LinkedHashSet<>()));
        boolean overflows = overflows(body.fragment(), pageHeight - reserved);
        if (overflows) {
            return new Probe(reserved, body, calls, true, null);
        }
        if (carry == null && calls.isEmpty()) {
            return new Probe(reserved, body, calls, false, new Stacked(List.of(), 0, null));
        }
        double content = reserved - model.margin().vertical() - model.border().vertical() - model.padding().vertical();
        Stacked stacked = stack(carry, calls, model.contentWidth(), content, body.breakToken() != null);
        return new Probe(reserved, body, calls, false, stacked);
    }

    /**
     * Whether some child of the flow of the page sticks out of the {@code available} height the reservation leaves
     * it: placed by the progress guarantee, because the reservation left it no room.
     */
    private static boolean overflows(Fragment page, double available) {
        double height = available - page.border().vertical() - page.padding().vertical();
        for (Fragment c : page.children()) {
            if (!c.box().style().isOutOfFlow() && c.height() > 0 && c.y() + c.height() > height + Tolerance.MODEL) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stacks the notes in the content box of the area, in order: the one carried from the previous page and then
     * those of {@code calls}, each with its margins, which merge with those of the previous one. Only the last one may
     * continue on the next page, with a break that violates no rule, and only if the flow continues too
     * ({@code bodyContinues}). {@code null} if they do not fit that way.
     */
    private Stacked stack(Carry carry, List<String> calls, double width, double available, boolean bodyContinues) {
        List<Box> boxes = new ArrayList<>();
        List<BreakToken> tokens = new ArrayList<>();
        if (carry != null) {
            boxes.add(carry.note());
            tokens.add(carry.token());
        }
        for (String id : calls) {
            boxes.add(notes.get(id));
            tokens.add(null);
        }
        List<Fragment> fragments = new ArrayList<>();
        MarginStrut strut = MarginStrut.EMPTY;
        double cursor = 0;
        Carry next = null;
        for (int i = 0; i < boxes.size(); i++) {
            Box note = boxes.get(i);
            Insets margin = Insets.resolve(note.style().box().margin(), width);
            // The first note touches the padding of the area: its top margin counts; a continued note has none.
            double top = cursor + strut.add(tokens.get(i) != null ? 0 : margin.top()).resolve();
            if (!fits(top, available) && i > 0) {
                return null;
            }
            ConstraintSpace space = ConstraintSpace.forChildren(width, OptionalDouble.empty(),
                    OptionalDouble.of(available - top), i == 0);
            LayoutResult r = engine.layout(note, space, tokens.get(i));
            if (r.breakToken() instanceof BreakToken.Before) {
                return null;
            }
            if (r.breakToken() != null) {
                if (i < boxes.size() - 1 || r.breakAppeal() < BreakAppeal.PERFECT_BREAK || !bodyContinues) {
                    return null;
                }
                next = new Carry(note, r.breakToken());
            }
            Fragment f = r.fragment();
            double x = Sizing.horizontalMargins(width, f.width(), note.style().box().margin().left(),
                    note.style().box().margin().right())[0];
            fragments.add(f.moved(x, top));
            cursor = top + f.height();
            strut = r.adjoining().bottom().add(margin.bottom());
        }
        if (!fits(cursor, available)) {
            return null;
        }
        return new Stacked(fragments, cursor, next);
    }

    /**
     * The full height of the notes, laid out without breaking and stacked with their margins, which merge between
     * notes as in block flow: the reservation in which they surely fit.
     */
    private double total(Carry carry, List<String> calls, BoxModel model) {
        double width = model.contentWidth();
        ConstraintSpace space = ConstraintSpace.forChildren(width, OptionalDouble.empty(), OptionalDouble.empty(), true);
        MarginStrut strut = MarginStrut.EMPTY;
        double cursor = 0;
        if (carry != null) {
            LayoutResult r = engine.layout(carry.note(), space, carry.token());
            cursor = r.fragment().height();
            strut = r.adjoining().bottom().add(Insets.resolve(carry.note().style().box().margin(), width).bottom());
        }
        for (String id : calls) {
            Box note = notes.get(id);
            Insets margin = Insets.resolve(note.style().box().margin(), width);
            LayoutResult r = engine.layout(note, space, null);
            double top = cursor + strut.add(margin.top()).resolve();
            cursor = top + r.fragment().height();
            strut = r.adjoining().bottom().add(margin.bottom());
        }
        return model.margin().vertical() + model.border().vertical() + model.padding().vertical() + cursor;
    }

    /** The {@code Id}s of the footnotes called in the lines of a fragment and its descendants, in order. */
    static Set<String> calls(Fragment f, Set<String> found) {
        for (TextLine l : f.lines()) {
            for (TextLine.Run r : l.runs()) {
                if (r.format().footnote() != null) {
                    found.add(r.format().footnote());
                }
            }
        }
        for (Fragment child : f.children()) {
            calls(child, found);
        }
        return found;
    }

    /** The notes of the page do not leave room for the text that calls them, even broken. */
    private InvalidModelException tooTall(Carry carry, List<String> calls) {
        Box culprit = carry != null ? carry.note() : notes.get(calls.get(calls.size() - 1));
        return new InvalidModelException(culprit.line(), ErrorCode.FOOTNOTE_TOO_TALL, "The footnote " + culprit.id()
                + " does not fit on a page with the text that calls it: it has to be shorter, or split into several "
                + "notes");
    }
}
