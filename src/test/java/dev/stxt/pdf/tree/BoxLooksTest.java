package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Corners;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** {@code Corner radius}, {@code Border color} por lado y {@code Background} con degradado (referencia/comun/caja.md). */
class BoxLooksTest {

    private static Box block(String... boxLines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tBlock:\n\t\tBox:\n\t\t\t" + String.join("\n\t\t\t", boxLines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(doc))).sections().get(0).box().children().get(0);
    }

    @Test
    void radiusFollowsTheCornerOrderOfCss() {
        Style s = block("Corner radius: 1 2 3 4").style();
        assertEquals(new Corners<>(Value.pt(1), Value.pt(2), Value.pt(3), Value.pt(4)), s.box().radius());
        assertEquals(Corners.all(Value.pt(8)), block("Corner radius: 8").style().box().radius());
        assertEquals(new Corners<>(Value.pt(0), Value.pt(20), Value.pt(0), Value.pt(20)),
                block("Corner radius: 0 20").style().box().radius());
    }

    @Test
    void radiusAcceptsPercentages() {
        assertEquals(Corners.all(new Value.Percentage(50)), block("Corner radius: 50%").style().box().radius());
    }

    @Test
    void withoutRadiusTheCornersAreSquare() {
        assertEquals(Corners.all(Value.pt(0)), block("Padding: 1").style().box().radius());
    }

    @Test
    void borderColorTakesOneToFourColors() {
        assertEquals(Sides.all("#112233"), block("Border color: #112233").style().box().borderColor());
        assertEquals(new Sides<>("#111", "#222", "#111", "#222"), block("Border color: #111 #222").style().box().borderColor());
        assertEquals(new Sides<>("#111", "#222", "#333", "#444"),
                block("Border color: #111 #222 #333 #444").style().box().borderColor());
        assertNull(block("Border: 1").style().box().borderColor());
    }

    @Test
    void backgroundIsAColorOrALinearGradient() {
        assertEquals(new Paint.Solid("#F1EFE8"), block("Background: #F1EFE8").style().box().background());
        assertEquals(new Paint.Solid("#F1EFE880"), block("Background: #F1EFE880").style().box().background());
        assertEquals(new Paint.Linear(180, List.of("#12315A00", "#12315A")),
                block("Background: linear(180, #12315A00, #12315A)").style().box().background());
        assertEquals(new Paint.Linear(-45.5, List.of("#000", "#FFF", "#F00")),
                block("Background: linear( -45.5 , #000 , #FFF , #F00 )").style().box().background());
        assertNull(block("Padding: 1").style().box().background());
    }

    @Test
    void theRulesOfStylesCanSetThem() {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Block\n\t\t\tBox:\n\t\t\t\tCorner radius: 6\n\t\t\t\tBackground: linear(90, #000, #FFF)\n"
                + "\tBlock:\n\t\tBox:\n\t\t\tBackground: #CCC\n\tBlock:\n";
        Box root = new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(doc))).sections().get(0).box();
        // Lo propio gana a la regla propiedad a propiedad: el color, no el degradado; el radio de la regla se mantiene.
        assertEquals(new Paint.Solid("#CCC"), root.children().get(0).style().box().background());
        assertEquals(Corners.all(Value.pt(6)), root.children().get(0).style().box().radius());
        assertEquals(new Paint.Linear(90, List.of("#000", "#FFF")), root.children().get(1).style().box().background());
    }
}
