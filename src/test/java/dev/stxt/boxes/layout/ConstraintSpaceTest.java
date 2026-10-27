package dev.stxt.boxes.layout;

import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.tree.Media;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstraintSpaceTest {

    /** Un espacio con todos los campos distintos de los valores por defecto. */
    private static final ConstraintSpace FULL = new ConstraintSpace(100, OptionalDouble.of(50), true, true,
            80, OptionalDouble.of(40), OptionalDouble.of(30), true, true);

    @Test
    void withRemainingKeepsEverythingElse() {
        ConstraintSpace s = FULL.withRemaining(12, false);
        assertEquals(new ConstraintSpace(100, OptionalDouble.of(50), true, true,
                80, OptionalDouble.of(40), OptionalDouble.of(12), false, true), s);
    }

    @Test
    void fragmentedKeepsEverythingElse() {
        ConstraintSpace free = ConstraintSpace.free(100, 80, OptionalDouble.of(40), false).measuringContentHeight();
        assertEquals(new ConstraintSpace(100, OptionalDouble.empty(), false, false,
                80, OptionalDouble.of(40), OptionalDouble.of(25), true, true), free.fragmented(25, true));
    }

    @Test
    void measuringContentHeightKeepsEverythingElse() {
        ConstraintSpace s = new ConstraintSpace(100, OptionalDouble.of(50), true, true,
                80, OptionalDouble.of(40), OptionalDouble.of(30), true, false);
        assertEquals(FULL, s.measuringContentHeight());
    }

    @Test
    void withRemainingRequiresFragmentation() {
        ConstraintSpace free = ConstraintSpace.free(100, 100, OptionalDouble.empty(), false);
        assertThrows(IllegalStateException.class, () -> free.withRemaining(10, true));
    }

    @Test
    void factories() {
        assertEquals(new ConstraintSpace(100, OptionalDouble.empty(), false, false,
                        80, OptionalDouble.of(40), OptionalDouble.empty(), true, false),
                ConstraintSpace.free(100, 80, OptionalDouble.of(40), true));
        assertEquals(new ConstraintSpace(100, OptionalDouble.empty(), true, false,
                        80, OptionalDouble.empty(), OptionalDouble.empty(), false, false),
                ConstraintSpace.fixed(100, OptionalDouble.empty(), 80, OptionalDouble.empty(), false));
        assertEquals(new ConstraintSpace(100, OptionalDouble.of(50), true, true,
                        80, OptionalDouble.of(40), OptionalDouble.empty(), true, false),
                ConstraintSpace.fixed(100, OptionalDouble.of(50), 80, OptionalDouble.of(40), true));
        assertEquals(new ConstraintSpace(100, OptionalDouble.of(50), false, false,
                        100, OptionalDouble.of(50), OptionalDouble.of(30), true, false),
                ConstraintSpace.forChildren(100, OptionalDouble.of(50), OptionalDouble.of(30), true));
    }

    @Test
    void forPageIsTheWholePageArea() {
        ConstraintSpace s = ConstraintSpace.forPage(new Media(595, 842, new Insets(40, 40, 40, 40)));
        assertEquals(new ConstraintSpace(515, OptionalDouble.of(762), true, true,
                515, OptionalDouble.of(762), OptionalDouble.of(762), true, false), s);
        assertTrue(s.fragmenting());
        assertFalse(ConstraintSpace.free(1, 1, OptionalDouble.empty(), true).fragmenting());
    }
}
