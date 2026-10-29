package dev.stxt.boxes.tree;

import dev.stxt.boxes.Docs;
import dev.stxt.boxes.model.Corners;
import dev.stxt.boxes.model.Paint;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** {@code Radius}, {@code Border color} por lado y {@code Background} con degradado (§4.3). */
class BoxLooksTest {

    private static Box block(String... boxLines) {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tBlock:\n\t\tBox:\n\t\t\t" + String.join("\n\t\t\t", boxLines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(doc))).sections().get(0).box().children().get(0);
    }

    @Test
    void radiusFollowsTheCornerOrderOfCss() {
        Style s = block("Radius: 1 2 3 4").style();
        assertEquals(new Corners<>(Value.pt(1), Value.pt(2), Value.pt(3), Value.pt(4)), s.radius);
        assertEquals(Corners.all(Value.pt(8)), block("Radius: 8").style().radius);
        assertEquals(new Corners<>(Value.pt(0), Value.pt(20), Value.pt(0), Value.pt(20)),
                block("Radius: 0 20").style().radius);
    }

    @Test
    void radiusAcceptsPercentages() {
        assertEquals(Corners.all(new Value.Percentage(50)), block("Radius: 50%").style().radius);
    }

    @Test
    void withoutRadiusTheCornersAreSquare() {
        assertEquals(Corners.all(Value.pt(0)), block("Padding: 1").style().radius);
    }

    @Test
    void borderColorTakesOneToFourColors() {
        assertEquals(Sides.all("#112233"), block("Border color: #112233").style().borderColor);
        assertEquals(new Sides<>("#111", "#222", "#111", "#222"), block("Border color: #111 #222").style().borderColor);
        assertEquals(new Sides<>("#111", "#222", "#333", "#444"),
                block("Border color: #111 #222 #333 #444").style().borderColor);
        assertNull(block("Border: 1").style().borderColor);
    }

    @Test
    void backgroundIsAColorOrALinearGradient() {
        assertEquals(new Paint.Solid("#F1EFE8"), block("Background: #F1EFE8").style().background);
        assertEquals(new Paint.Solid("#F1EFE880"), block("Background: #F1EFE880").style().background);
        assertEquals(new Paint.Linear(180, List.of("#12315A00", "#12315A")),
                block("Background: linear(180, #12315A00, #12315A)").style().background);
        assertEquals(new Paint.Linear(-45.5, List.of("#000", "#FFF", "#F00")),
                block("Background: linear( -45.5 , #000 , #FFF , #F00 )").style().background);
        assertNull(block("Padding: 1").style().background);
    }

    @Test
    void theRulesOfStylesCanSetThem() {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tElement: Block\n\t\t\tBox:\n\t\t\t\tRadius: 6\n\t\t\t\tBackground: linear(90, #000, #FFF)\n"
                + "\tBlock:\n\t\tBox:\n\t\t\tBackground: #CCC\n\tBlock:\n";
        Box root = new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(doc))).sections().get(0).box();
        // Lo propio gana a la regla propiedad a propiedad: el color, no el degradado; el radio de la regla se mantiene.
        assertEquals(new Paint.Solid("#CCC"), root.children().get(0).style().background);
        assertEquals(Corners.all(Value.pt(6)), root.children().get(0).style().radius);
        assertEquals(new Paint.Linear(90, List.of("#000", "#FFF")), root.children().get(1).style().background);
    }
}
