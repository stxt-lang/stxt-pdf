package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import java.awt.geom.AffineTransform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code Rotate} y {@code Rotate origin} (reference/common/layers-and-rotation.md). */
class RotationTest {

    private static final double E = 1e-9;

    @Test
    void originKeywordsInAnyOrder() {
        assertOrigin(50, 50, "center");
        assertOrigin(0, 0, "top left");
        assertOrigin(0, 0, "left top");
        assertOrigin(100, 100, "bottom   right");
        // El eje que no se nombra queda en el centro.
        assertOrigin(50, 100, "bottom");
        assertOrigin(100, 50, "right center");
        assertOrigin(50, 50, "center center");
    }

    /** Dos medidas: la horizontal y la vertical, en puntos o en porcentaje, también negativas. */
    @Test
    void originMeasures() {
        assertArrayEquals(new Value[] {Value.pt(20), new Value.Percentage(30)}, Rotation.origin("20 30%"));
        assertArrayEquals(new Value[] {new Value.Percentage(0), Value.pt(-5.5)}, Rotation.origin("0% -5.5"));
    }

    @Test
    void originRejectsWhatIsNotKeywordsOnDifferentAxesOrTwoMeasures() {
        assertThrows(IllegalArgumentException.class, () -> Rotation.origin("left right"));
        assertThrows(IllegalArgumentException.class, () -> Rotation.origin("top bottom"));
        assertThrows(IllegalArgumentException.class, () -> Rotation.origin("top left center"));
        assertThrows(IllegalArgumentException.class, () -> Rotation.origin("50%"));
        assertThrows(IllegalArgumentException.class, () -> Rotation.origin("left 20"));
        assertThrows(IllegalArgumentException.class, () -> Rotation.origin("20 auto"));
        assertThrows(IllegalArgumentException.class, () -> Rotation.origin("* 20"));
        assertThrows(IllegalArgumentException.class, () -> Rotation.origin("arriba 20"));
        assertThrows(IllegalArgumentException.class, () -> Rotation.origin("10 20 30"));
        assertThrows(IllegalArgumentException.class, () -> Rotation.origin(" "));
    }

    private static void assertOrigin(double x, double y, String text) {
        assertArrayEquals(new Value[] {new Value.Percentage(x), new Value.Percentage(y)}, Rotation.origin(text));
    }

    @Test
    void pivotInPointsAndPercentages() {
        Rotation r = new Rotation(30, Value.pt(15), new Value.Percentage(25));
        Rect box = new Rect(10, 20, 100, 40);
        assertEquals(25, r.pivotX(box), E);
        assertEquals(30, r.pivotY(box), E);
    }

    @Test
    void pivotIsTheOriginOfTheBorderBox() {
        Rotation r = new Rotation(30, 1, 0);
        Rect box = new Rect(10, 20, 100, 40);
        assertEquals(110, r.pivotX(box), E);
        assertEquals(20, r.pivotY(box), E);
    }

    /** Con la y hacia abajo, 90 grados positivos giran en el sentido de las agujas del reloj. */
    @Test
    void positiveDegreesTurnClockwise() {
        Rect box = new Rect(100, 100, 100, 20);
        AffineTransform t = new Rotation(90, 0.5, 0.5).transform(box);
        // La esquina de arriba a la izquierda pasa arriba a la derecha.
        double[] c = Rotation.corners(t, box);
        assertArrayEquals(new double[] {160, 60, 160, 160, 140, 160, 140, 60}, c, E);
        assertEquals(new Rect(140, 60, 20, 100).toString(), round(Rotation.envelope(t, box)).toString());
    }

    @Test
    void envelopeOfAQuarterTurnAroundACorner() {
        Rect box = new Rect(0, 0, 40, 10);
        AffineTransform t = new Rotation(-90, 0, 1).transform(box);
        // Alrededor de abajo a la izquierda, en contra de las agujas del reloj: queda de pie sobre
        // esa esquina, a su izquierda.
        assertEquals(new Rect(-10, -30, 10, 40).toString(), round(Rotation.envelope(t, box)).toString());
    }

    @Test
    void envelopeOfFortyFiveDegrees() {
        Rect box = new Rect(0, 0, 10, 10);
        Rect e = Rotation.envelope(new Rotation(45, 0.5, 0.5).transform(box), box);
        double side = 10 * Math.sqrt(2);
        assertEquals(side, e.width(), E);
        assertEquals(side, e.height(), E);
        assertEquals(5 - side / 2, e.x(), E);
        assertTrue(Rotation.envelope(new AffineTransform(), box).equals(box));
    }

    private static Rect round(Rect r) {
        return new Rect(Math.round(r.x() * 1000) / 1000.0, Math.round(r.y() * 1000) / 1000.0,
                Math.round(r.width() * 1000) / 1000.0, Math.round(r.height() * 1000) / 1000.0);
    }
}
