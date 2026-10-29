package dev.stxt.boxes.layout;

import dev.stxt.boxes.model.Decoration;
import dev.stxt.boxes.model.Insets;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reparto del alto, el borde y el relleno entre fragmentos (ESPECIFICACION.md §5.10). Todas las
 * cajas tienen borde 1 y relleno 4: 5 pt de decoración arriba y 5 abajo.
 */
class SliceTest {

    private static final Insets BORDER = Insets.all(1);
    private static final Insets PADDING = Insets.all(4);
    private static final OptionalDouble NONE = OptionalDouble.empty();
    private static final BoxModel AUTO = model(NONE, 0, NONE, Decoration.SLICE);
    private static final BoxModel AUTO_CLONE = model(NONE, 0, NONE, Decoration.CLONE);

    private static BoxModel model(OptionalDouble height, double min, OptionalDouble max, Decoration d) {
        return new BoxModel(Insets.ZERO, BORDER, PADDING, 100, height, min, max, d);
    }

    private static ConstraintSpace page(double remaining) {
        return ConstraintSpace.forChildren(100, NONE, OptionalDouble.of(remaining), false);
    }

    /** Borde o relleno con solo los lados indicados. */
    private static Insets sides(Insets all, boolean top, boolean bottom) {
        return new Insets(top ? all.top() : 0, all.right(), bottom ? all.bottom() : 0, all.left());
    }

    private static void assertSlice(Slice actual, double height, boolean top, boolean bottom, boolean finished,
                                    double consumed) {
        assertEquals(new Slice(height, sides(BORDER, top, bottom), sides(PADDING, top, bottom), finished, consumed),
                actual);
    }

    @Test
    void withoutFragmentationTheBoxIsWhole() {
        ConstraintSpace free = ConstraintSpace.free(100, 100, NONE, false);
        assertSlice(Slice.of(AUTO, true, 0, 30, true, free), 40, true, true, true, 0);
    }

    @Test
    void autoHeightSliceAcrossThreeFragments() {
        // Primero: decoración superior y el contenido; sin la inferior.
        Slice first = Slice.of(AUTO, true, 0, 90, false, page(100));
        assertSlice(first, 95, true, false, false, 95);
        // Intermedio: sin decoración.
        Slice middle = Slice.of(AUTO, false, first.consumed(), 100, false, page(100));
        assertSlice(middle, 100, false, false, false, 195);
        // Último: solo la decoración inferior.
        Slice last = Slice.of(AUTO, false, middle.consumed(), 20, true, page(100));
        assertSlice(last, 25, false, true, true, 220);
    }

    @Test
    void definiteHeightIsTheTotalOfAllFragments() {
        BoxModel m = model(OptionalDouble.of(300), 0, NONE, Decoration.SLICE);
        // El contenido termina, pero el alto propio no cabe: la página se llena y sigue.
        Slice first = Slice.of(m, true, 0, 20, true, page(100));
        assertSlice(first, 100, true, false, false, 100);
        // Lo que falta, con la decoración inferior.
        Slice last = Slice.of(m, false, first.consumed(), 0, true, page(762));
        assertSlice(last, 200, false, true, true, 300);
    }

    @Test
    void definiteHeightCutInsideTheContentFillsThePage() {
        BoxModel m = model(OptionalDouble.of(300), 0, NONE, Decoration.SLICE);
        assertSlice(Slice.of(m, true, 0, 50, false, page(100)), 100, true, false, false, 100);
        // Sin pasar de lo que le queda a la caja.
        assertSlice(Slice.of(m, false, 250, 10, false, page(100)), 50, false, false, false, 300);
    }

    @Test
    void minHeightAppliesToTheTotal() {
        BoxModel m = model(NONE, 200, NONE, Decoration.SLICE);
        Slice first = Slice.of(m, true, 0, 20, true, page(100));
        assertSlice(first, 100, true, false, false, 100);
        Slice last = Slice.of(m, false, first.consumed(), 0, true, page(762));
        assertSlice(last, 100, false, true, true, 200);
    }

    @Test
    void maxHeightAppliesToTheTotal() {
        BoxModel m = model(NONE, 0, OptionalDouble.of(50), Decoration.SLICE);
        assertSlice(Slice.of(m, true, 0, 80, true, page(200)), 50, true, true, true, 50);
    }

    @Test
    void cloneRepeatsTheDecorationInEveryFragment() {
        // 100 de página: 90 para el contenido.
        Slice first = Slice.of(AUTO_CLONE, true, 0, 90, false, page(100));
        assertSlice(first, 100, true, true, false, 90);
        // Con clone, consumed es el alto de la caja de contenido.
        Slice last = Slice.of(AUTO_CLONE, false, first.consumed(), 30, true, page(100));
        assertSlice(last, 40, true, true, true, 120);
    }

    @Test
    void cloneWithDefiniteHeightSplitsTheContentBoxHeight() {
        // Height 250: 240 de caja de contenido, repartidos en 90 + 90 + 60.
        BoxModel m = model(OptionalDouble.of(250), 0, NONE, Decoration.CLONE);
        Slice first = Slice.of(m, true, 0, 20, true, page(100));
        assertSlice(first, 100, true, true, false, 90);
        Slice second = Slice.of(m, false, first.consumed(), 0, true, page(100));
        assertSlice(second, 100, true, true, false, 180);
        Slice last = Slice.of(m, false, second.consumed(), 0, true, page(100));
        assertSlice(last, 70, true, true, true, 240);
        // Cortado dentro del contenido, llega al final de la página.
        assertSlice(Slice.of(m, true, 0, 20, false, page(100)), 100, true, true, false, 90);
    }

    @Test
    void topDecoration() {
        assertEquals(5, Slice.top(AUTO, true), 1e-9);
        assertEquals(0, Slice.top(AUTO, false), 1e-9);
        assertEquals(5, Slice.top(AUTO_CLONE, false), 1e-9);
    }

    @Test
    void contentRemaining() {
        // Con slice, la decoración inferior no se reserva (lo hace Engine.layout si la caja termina).
        assertEquals(95, Slice.contentRemaining(AUTO, true, page(100)), 1e-9);
        assertEquals(100, Slice.contentRemaining(AUTO, false, page(100)), 1e-9);
        // Con clone, sí: va en todos los fragmentos.
        assertEquals(90, Slice.contentRemaining(AUTO_CLONE, false, page(100)), 1e-9);
    }

    @Test
    void contentHeight() {
        assertEquals(90, new Slice(100, BORDER, PADDING, true, 0).contentHeight(), 1e-9);
        assertEquals(0, new Slice(6, BORDER, PADDING, true, 0).contentHeight(), 1e-9);
    }
}
