package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakAppeal;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Unit;
import dev.stxt.pdf.layout.position.FrameOffset;
import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Edges;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element;
import dev.stxt.pdf.tree.StyleEditor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Break between units (reference/common/breaks.md) with synthetic units: a container 100 pt
 * wide without border or padding and block items with a {@code Height}, one per unit unless
 * stated otherwise.
 */
class UnitBreakerTest {

    private static final OptionalDouble NONE = OptionalDouble.empty();
    private static final int PERFECT = BreakAppeal.PERFECT_BREAK;

    private final Engine engine = new Engine();
    private final Box container = new StyleEditor().box("C", Element.STACK);
    private final BoxModel model = new BoxModel(Insets.ZERO, Insets.ZERO, Insets.ZERO, 100, NONE, 0, NONE,
            Edges.OPEN);

    private static Box item(String id, double height) {
        return new StyleEditor().height(Value.pt(height)).box(id, Element.BLOCK);
    }

    /** Item already laid out without fragmenting, at {@code y} of the content box of the container. */
    private static UnitBreaker.Placed placed(Box item, double y, int unit) {
        double h = ((Value.Points) item.style().box().height()).pt();
        LayoutResult r = LayoutResult.finished(new Fragment(item, 0, 0, 100, h, Insets.ZERO, Insets.ZERO,
                Insets.ZERO, List.of(), List.of(), true, true));
        return new UnitBreaker.Placed(item, r, ConstraintSpace.fixed(100, NONE, 100, NONE, false), 0, y, unit,
                FrameOffset.NONE);
    }

    /** Three units of 40, with one item each. */
    private static List<UnitBreaker.Placed> threeItems() {
        return List.of(placed(item("A", 40), 0, 0), placed(item("B", 40), 40, 1), placed(item("D", 40), 80, 2));
    }

    /** Three units of 40, with the break quality and the forced break before each one. */
    private static UnitBreaker.Units units(int[] appeal, boolean[] forced, boolean splittable) {
        List<Unit> units = new ArrayList<>();
        for (int u = 0; u < 3; u++) {
            units.add(new Unit(40 * u, 40 * u + 40, appeal[u], forced[u] ? Break.PAGE : Break.AUTO));
        }
        return new UnitBreaker.Units(units, 120, splittable, false);
    }

    private static final UnitBreaker.Units THREE = units(new int[]{PERFECT, PERFECT, PERFECT},
            new boolean[3], false);
    /** The same three units with {@code Split rows}. */
    private static final UnitBreaker.Units THREE_SPLIT = new UnitBreaker.Units(THREE.list(), 120, true, true);

    private static ConstraintSpace page(double remaining, boolean atStart) {
        return ConstraintSpace.forChildren(100, NONE, OptionalDouble.of(remaining), atStart);
    }

    private LayoutResult fragment(ConstraintSpace space, BreakToken from, List<UnitBreaker.Placed> placed,
                                  UnitBreaker.Units units, UnitBreaker.Repeat repeat) {
        return UnitBreaker.fragment(engine, container, space, from,
                new UnitBreaker.Arranged(model, placed, List.of(), units), repeat);
    }

    private static List<Double> ys(Fragment f) {
        return f.children().stream().map(Fragment::y).toList();
    }

    @Test
    void withoutFragmentationEverythingIsPlaced() {
        LayoutResult r = fragment(ConstraintSpace.free(100, 100, NONE, false), null, threeItems(),
                units(new int[]{PERFECT, PERFECT, PERFECT}, new boolean[]{false, true, false}, false),
                UnitBreaker.Repeat.NONE);
        assertNull(r.breakToken());
        assertEquals(120, r.fragment().height(), 1e-9);
        assertEquals(List.of(0.0, 40.0, 80.0), ys(r.fragment()));
    }

    @Test
    void cutsBetweenUnitsAndContinuesFromTheNextOne() {
        LayoutResult first = fragment(page(100, true), null, threeItems(), THREE, UnitBreaker.Repeat.NONE);
        assertEquals(new BreakToken.Container(2, List.of(), 80), first.breakToken());
        assertEquals(PERFECT, first.breakAppeal());
        assertEquals(80, first.fragment().height(), 1e-9);
        assertEquals(List.of(0.0, 40.0), ys(first.fragment()));

        // The next page starts at the first unit that remains.
        LayoutResult last = fragment(page(100, true), first.breakToken(), threeItems(), THREE,
                UnitBreaker.Repeat.NONE);
        assertNull(last.breakToken());
        assertEquals(40, last.fragment().height(), 1e-9);
        assertEquals(List.of(0.0), ys(last.fragment()));
        assertEquals("D", last.fragment().children().get(0).box().id());
    }

    @Test
    void unitsThatCannotBeSeparatedGoTogether() {
        // A break before unit 1 is not possible: the first two are a group of 80.
        UnitBreaker.Units grouped = units(new int[]{PERFECT, -1, PERFECT}, new boolean[3], false);
        LayoutResult r = fragment(page(60, false), null, threeItems(), grouped, UnitBreaker.Repeat.NONE);
        assertInstanceOf(BreakToken.Before.class, r.breakToken());

        // At the start of a page, the group is placed even if it overflows, and it breaks as soon as possible.
        LayoutResult forced = fragment(page(60, true), null, threeItems(), grouped, UnitBreaker.Repeat.NONE);
        assertEquals(new BreakToken.Container(2, List.of(), 80), forced.breakToken());
        assertEquals(BreakAppeal.FORCED, forced.breakAppeal());
        assertEquals(80, forced.fragment().height(), 1e-9);
    }

    @Test
    void forcedBreakCutsEvenIfEverythingFits() {
        UnitBreaker.Units forced = units(new int[]{PERFECT, PERFECT, PERFECT}, new boolean[]{false, true, false},
                false);
        LayoutResult r = fragment(page(500, true), null, threeItems(), forced, UnitBreaker.Repeat.NONE);
        assertEquals(new BreakToken.Container(1, List.of(), 40), r.breakToken());
        assertEquals(List.of(0.0), ys(r.fragment()));
    }

    @Test
    void repeatedHeadAndFootGoInEveryFragment() {
        Box head = item("H", 10);
        Box foot = item("F", 10);
        UnitBreaker.Repeat repeat = new UnitBreaker.Repeat(List.of(placed(head, 0, -1)), 10,
                List.of(placed(foot, 0, -1)), 10);
        // 100 of page: 80 for the units, between the header and the footer.
        LayoutResult first = fragment(page(100, true), null, threeItems(), THREE, repeat);
        assertEquals(100, first.fragment().height(), 1e-9);
        assertEquals(List.of(0.0, 10.0, 50.0, 90.0), ys(first.fragment()));
        assertEquals(new BreakToken.Container(2, List.of(), 100), first.breakToken());

        LayoutResult last = fragment(page(100, true), first.breakToken(), threeItems(), THREE, repeat);
        assertNull(last.breakToken());
        assertEquals(60, last.fragment().height(), 1e-9);
        assertEquals(List.of(0.0, 10.0, 50.0), ys(last.fragment()));
        assertEquals(List.of("H", "D", "F"), last.fragment().children().stream().map(f -> f.box().id()).toList());
    }

    @Test
    void unitTallerThanThePageSplitsItsItemsInParallel() {
        // A single unit with two items of 150 and 60, which does not fit even at the start of a page of 100.
        Box a = item("A", 150);
        Box b = item("B", 60);
        List<UnitBreaker.Placed> placed = List.of(placed(a, 0, 0), placed(b, 0, 0));
        UnitBreaker.Units one = new UnitBreaker.Units(List.of(new Unit(0, 150, PERFECT, Break.AUTO)), 150, true, false);

        LayoutResult first = fragment(page(100, true), null, placed, one, UnitBreaker.Repeat.NONE);
        BreakToken.Container token = assertInstanceOf(BreakToken.Container.class, first.breakToken());
        assertEquals(0, token.nextLine());
        assertNotNull(token.itemTokens().get(0));
        assertNull(token.itemTokens().get(1));
        assertEquals(100, first.fragment().height(), 1e-9);
        assertEquals(List.of(100.0, 60.0), first.fragment().children().stream().map(Fragment::height).toList());

        // Only A continues, from the top.
        LayoutResult last = fragment(page(100, true), token, placed, one, UnitBreaker.Repeat.NONE);
        assertNull(last.breakToken());
        assertEquals(50, last.fragment().height(), 1e-9);
        assertEquals(1, last.fragment().children().size());
        assertEquals("A", last.fragment().children().get(0).box().id());
        assertEquals(50, last.fragment().children().get(0).height(), 1e-9);
    }

    @Test
    void splitUnitsBreakTheUnitThatDoesNotFitOnThisPage() {
        // Units of 40 on a page of 100: the third is broken, 20 + 20, instead of moving whole.
        LayoutResult first = fragment(page(100, true), null, threeItems(), THREE_SPLIT, UnitBreaker.Repeat.NONE);
        BreakToken.Container token = assertInstanceOf(BreakToken.Container.class, first.breakToken());
        assertEquals(2, token.nextLine());
        assertEquals(1, token.itemTokens().size());
        assertNotNull(token.itemTokens().get(0));
        assertEquals(PERFECT, first.breakAppeal());
        assertEquals(100, first.fragment().height(), 1e-9);
        assertEquals(List.of(0.0, 40.0, 80.0), ys(first.fragment()));
        assertEquals(20, first.fragment().children().get(2).height(), 1e-9);

        LayoutResult last = fragment(page(100, true), token, threeItems(), THREE_SPLIT, UnitBreaker.Repeat.NONE);
        assertNull(last.breakToken());
        assertEquals(20, last.fragment().height(), 1e-9);
        assertEquals("D", last.fragment().children().get(0).box().id());
        assertEquals(20, last.fragment().children().get(0).height(), 1e-9);
    }

    @Test
    void splitUnitsRespectABetterBreakBetweenUnits() {
        // D cannot be broken (Keep together): the break before it is better, and D moves whole.
        Box d = new StyleEditor().height(Value.pt(40)).keepTogether(true).box("D", Element.BLOCK);
        List<UnitBreaker.Placed> placed = List.of(placed(item("A", 40), 0, 0), placed(item("B", 40), 40, 1),
                placed(d, 80, 2));
        LayoutResult r = fragment(page(100, true), null, placed, THREE_SPLIT, UnitBreaker.Repeat.NONE);
        assertEquals(new BreakToken.Container(2, List.of(), 80), r.breakToken());
        assertEquals(80, r.fragment().height(), 1e-9);
    }

    @Test
    void splitUnitsDoNotBreakAnItemThatCannotPlaceAnything() {
        // Nothing of D fits in the 0 left after two units of 40: the unit moves whole.
        LayoutResult r = fragment(page(80, true), null, threeItems(), THREE_SPLIT, UnitBreaker.Repeat.NONE);
        assertEquals(new BreakToken.Container(2, List.of(), 80), r.breakToken());
        assertEquals(80, r.fragment().height(), 1e-9);
    }

    @Test
    void splitUnitsBreakAGroupOnThisPage() {
        // A break before unit 1 is not possible: the group of 80 does not fit in the 60 of a page that is not at its
        // start and is broken on it, A (40) whole and B as 20 + 20, instead of moving the box to the next page.
        UnitBreaker.Units grouped = new UnitBreaker.Units(
                units(new int[]{PERFECT, -1, PERFECT}, new boolean[3], true).list(), 120, true, true);
        LayoutResult first = fragment(page(60, false), null, threeItems(), grouped, UnitBreaker.Repeat.NONE);
        BreakToken.Container token = assertInstanceOf(BreakToken.Container.class, first.breakToken());
        assertEquals(0, token.nextLine());
        assertEquals(2, token.itemTokens().size());
        assertNull(token.itemTokens().get(0));
        assertNotNull(token.itemTokens().get(1));
        assertEquals(60, first.fragment().height(), 1e-9);
        assertEquals(List.of(0.0, 40.0), ys(first.fragment()));

        // B continues from the top and D goes after it.
        LayoutResult last = fragment(page(100, true), token, threeItems(), grouped, UnitBreaker.Repeat.NONE);
        assertNull(last.breakToken());
        assertEquals(List.of(0.0, 20.0), ys(last.fragment()));
        assertEquals(60, last.fragment().height(), 1e-9);
    }

    @Test
    void sliceShiftsTheUnitsAndEndsAtTheLastOne() {
        UnitBreaker.Units all = units(new int[]{PERFECT, BreakAppeal.FORCED, -1}, new boolean[]{false, true, false},
                true);
        UnitBreaker.Units body = all.slice(1, 3, 40);
        assertEquals(List.of(new Unit(0, 40, BreakAppeal.FORCED, Break.PAGE), new Unit(40, 80, -1, Break.AUTO)), body.list());
        assertEquals(80, body.extent(), 1e-9);
        assertEquals(true, body.splittable());
        assertEquals(false, body.split());
    }

    @Test
    void unsplittableUnitsAreNotSplitInParallel() {
        Box a = item("A", 150);
        List<UnitBreaker.Placed> placed = List.of(placed(a, 0, 0));
        UnitBreaker.Units one = new UnitBreaker.Units(List.of(new Unit(0, 150, PERFECT, Break.AUTO)), 150, false, false);
        // It is placed whole, overflowing (progress guarantee).
        LayoutResult r = fragment(page(100, true), null, placed, one, UnitBreaker.Repeat.NONE);
        assertNull(r.breakToken());
        assertEquals(150, r.fragment().height(), 1e-9);
    }
}
