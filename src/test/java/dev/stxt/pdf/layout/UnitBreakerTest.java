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
 * Corte entre unidades (referencia/comun/cortes.md) con unidades sintéticas: un contenedor de 100 pt
 * de ancho sin borde ni relleno y ítems de bloque con un {@code Height}, uno por unidad salvo
 * que se diga otra cosa.
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

    /** Ítem ya maquetado sin fragmentar, en {@code y} de la caja de contenido del contenedor. */
    private static UnitBreaker.Placed placed(Box item, double y, int unit) {
        double h = ((Value.Points) item.style().box().height()).pt();
        LayoutResult r = LayoutResult.finished(new Fragment(item, 0, 0, 100, h, Insets.ZERO, Insets.ZERO,
                Insets.ZERO, List.of(), List.of(), true, true));
        return new UnitBreaker.Placed(item, r, ConstraintSpace.fixed(100, NONE, 100, NONE, false), 0, y, unit,
                FrameOffset.NONE);
    }

    /** Tres unidades de 40, con un ítem cada una. */
    private static List<UnitBreaker.Placed> threeItems() {
        return List.of(placed(item("A", 40), 0, 0), placed(item("B", 40), 40, 1), placed(item("D", 40), 80, 2));
    }

    /** Tres unidades de 40, con la calidad del corte y el corte forzado delante de cada una. */
    private static UnitBreaker.Units units(int[] appeal, boolean[] forced, boolean splittable) {
        List<Unit> units = new ArrayList<>();
        for (int u = 0; u < 3; u++) {
            units.add(new Unit(40 * u, 40 * u + 40, appeal[u], forced[u] ? Break.PAGE : Break.AUTO));
        }
        return new UnitBreaker.Units(units, 120, splittable);
    }

    private static final UnitBreaker.Units THREE = units(new int[]{PERFECT, PERFECT, PERFECT},
            new boolean[3], false);

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

        // La página siguiente empieza en la primera unidad que queda.
        LayoutResult last = fragment(page(100, true), first.breakToken(), threeItems(), THREE,
                UnitBreaker.Repeat.NONE);
        assertNull(last.breakToken());
        assertEquals(40, last.fragment().height(), 1e-9);
        assertEquals(List.of(0.0), ys(last.fragment()));
        assertEquals("D", last.fragment().children().get(0).box().id());
    }

    @Test
    void unitsThatCannotBeSeparatedGoTogether() {
        // No se puede cortar delante de la unidad 1: las dos primeras son un grupo de 80.
        UnitBreaker.Units grouped = units(new int[]{PERFECT, -1, PERFECT}, new boolean[3], false);
        LayoutResult r = fragment(page(60, false), null, threeItems(), grouped, UnitBreaker.Repeat.NONE);
        assertInstanceOf(BreakToken.Before.class, r.breakToken());

        // Al principio de página, el grupo se coloca aunque desborde y se corta en cuanto se puede.
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
        // 100 de página: 80 para las unidades, entre la cabecera y el pie.
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
        // Una sola unidad con dos ítems de 150 y 60, que no cabe ni al principio de una página de 100.
        Box a = item("A", 150);
        Box b = item("B", 60);
        List<UnitBreaker.Placed> placed = List.of(placed(a, 0, 0), placed(b, 0, 0));
        UnitBreaker.Units one = new UnitBreaker.Units(List.of(new Unit(0, 150, PERFECT, Break.AUTO)), 150, true);

        LayoutResult first = fragment(page(100, true), null, placed, one, UnitBreaker.Repeat.NONE);
        BreakToken.Container token = assertInstanceOf(BreakToken.Container.class, first.breakToken());
        assertEquals(0, token.nextLine());
        assertNotNull(token.itemTokens().get(0));
        assertNull(token.itemTokens().get(1));
        assertEquals(100, first.fragment().height(), 1e-9);
        assertEquals(List.of(100.0, 60.0), first.fragment().children().stream().map(Fragment::height).toList());

        // Solo continúa A, desde arriba.
        LayoutResult last = fragment(page(100, true), token, placed, one, UnitBreaker.Repeat.NONE);
        assertNull(last.breakToken());
        assertEquals(50, last.fragment().height(), 1e-9);
        assertEquals(1, last.fragment().children().size());
        assertEquals("A", last.fragment().children().get(0).box().id());
        assertEquals(50, last.fragment().children().get(0).height(), 1e-9);
    }

    @Test
    void sliceShiftsTheUnitsAndEndsAtTheLastOne() {
        UnitBreaker.Units all = units(new int[]{PERFECT, BreakAppeal.FORCED, -1}, new boolean[]{false, true, false},
                true);
        UnitBreaker.Units body = all.slice(1, 3, 40);
        assertEquals(List.of(new Unit(0, 40, BreakAppeal.FORCED, Break.PAGE), new Unit(40, 80, -1, Break.AUTO)), body.list());
        assertEquals(80, body.extent(), 1e-9);
        assertEquals(true, body.splittable());
    }

    @Test
    void unsplittableUnitsAreNotSplitInParallel() {
        Box a = item("A", 150);
        List<UnitBreaker.Placed> placed = List.of(placed(a, 0, 0));
        UnitBreaker.Units one = new UnitBreaker.Units(List.of(new Unit(0, 150, PERFECT, Break.AUTO)), 150, false);
        // Se coloca entera, desbordando (garantía de progreso).
        LayoutResult r = fragment(page(100, true), null, placed, one, UnitBreaker.Repeat.NONE);
        assertNull(r.breakToken());
        assertEquals(150, r.fragment().height(), 1e-9);
    }
}
