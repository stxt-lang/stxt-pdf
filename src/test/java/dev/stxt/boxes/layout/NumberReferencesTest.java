package dev.stxt.boxes.layout;

import org.junit.jupiter.api.Test;

import static dev.stxt.boxes.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@code {number of: id}} (referencia/comun/referencias.md): el número que escribe el marcador de un ítem. */
class NumberReferencesTest {

    private static String text(LayoutFixture l, String id) {
        return l.fragment(id).lines().stream().map(x -> x.text()).reduce((a, b) -> a + "|" + b).orElse("");
    }

    @Test
    void anItemNumberIsWrittenAsItsMarkerDoes() {
        LayoutFixture l = layOut(
                "\tText: Véase la cláusula {number of: c} ({number of: d}), el apartado {number of: r}.",
                "\t\tId: ref",
                "\tList:", "\t\tBox:", "\t\t\tPadding: 0 0 0 30", "\t\tMarker: decimal", "\t\tStart: 3",
                "\t\tList item:", "\t\t\tText: Tres", "\t\t\t\tId: t3",
                "\t\tList item:", "\t\t\tId: c", "\t\t\tText: Cuatro",
                "\tList:", "\t\tBox:", "\t\t\tPadding: 0 0 0 30", "\t\tMarker: lower-alpha",
                "\t\tList item:", "\t\t\tText: a", "\t\tList item:", "\t\t\tText: b",
                "\t\tList item:", "\t\t\tId: d", "\t\t\tText: c",
                "\tList:", "\t\tBox:", "\t\t\tPadding: 0 0 0 30", "\t\tMarker: upper-roman", "\t\tStart: 4",
                "\t\tList item:", "\t\t\tId: r", "\t\t\tText: Cuatro romano");
        assertEquals("Véase la cláusula 4 (c), el apartado IV.", text(l, "ref"));
    }

    @Test
    void theItemNumberFollowsNumberAndStart() {
        LayoutFixture l = layOut(
                "\tText: {number of: x}", "\t\tId: ref",
                "\tList:", "\t\tBox:", "\t\t\tPadding: 0 0 0 30", "\t\tMarker: decimal",
                "\t\tList item:", "\t\t\tText: Uno",
                "\t\tList item:", "\t\t\tNumber: 10", "\t\t\tText: Diez",
                "\t\tList item:", "\t\t\tId: x", "\t\t\tText: Once");
        assertEquals("11", text(l, "ref"));
    }
}
