package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Reglas reutilizables de {@code Styles}: a quién se aplican, con qué prioridad y cómo se mezclan. */
class StylesTest {

    private static Box root(String... lines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n" + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(doc)))).sections().get(0).box();
    }

    private static double padding(Box b) {
        return ((Value.Points) b.style().box().padding().top()).pt();
    }

    @Test
    void elementRuleAppliesToEveryElementOfItsType() {
        Box root = root(
                "\tStyles:", "\t\tElement: Block", "\t\t\tBox:", "\t\t\t\tPadding: 5",
                "\tBlock:", "\t\tBlock:", "\tFlex:");
        Box outer = root.children().get(0);
        assertEquals(5, padding(outer), 1e-9);
        assertEquals(5, padding(outer.children().get(0)), 1e-9);
        assertEquals(Sides.all(Value.pt(0)), root.children().get(1).style().box().padding());
        // La línea es la de la regla: los errores la señalan.
        assertEquals(14, outer.style().line("padding"));
    }

    @Test
    void ownPropertiesWinAndMergeWithTheRuleOneByOne() {
        Box text = root(
                "\tStyles:", "\t\tElement: Text", "\t\t\tTypography:", "\t\t\t\tSize: 12", "\t\t\t\tWeight: bold",
                "\tText: hola", "\t\tTypography:", "\t\t\tSize: 14").children().get(0);
        assertEquals(14, text.style().typography().fontSize(), 1e-9);
        assertEquals(FontWeight.BOLD, text.style().typography().fontWeight());
        assertEquals(19, text.style().line("font-size"));
        assertEquals(15, text.style().line("font-weight"));
    }

    @Test
    void aRuleWinsOverInheritance() {
        Box block = root(
                "\tStyles:", "\t\tElement: Text", "\t\t\tTypography:", "\t\t\t\tSize: 12",
                "\tBlock:", "\t\tTypography:", "\t\t\tSize: 20", "\t\tText: hola", "\t\tBlock:").children().get(0);
        assertEquals(12, block.children().get(0).style().typography().fontSize(), 1e-9);
        // Lo que la regla no declara se sigue heredando.
        assertEquals(20, block.children().get(1).style().typography().fontSize(), 1e-9);
    }

    @Test
    void priorityIsElementThenClassesInStylesOrderThenId() {
        Box root = root(
                "\tStyles:",
                "\t\tElement: Block", "\t\t\tBox:", "\t\t\t\tPadding: 1", "\t\t\t\tMargin: 1",
                "\t\tFor class: a", "\t\t\tBox:", "\t\t\t\tPadding: 2",
                "\t\tFor class: b", "\t\t\tBox:", "\t\t\t\tPadding: 3",
                "\t\tFor id: x", "\t\t\tBox:", "\t\t\t\tPadding: 4",
                "\t\tFor id: y", "\t\t\tBox:", "\t\t\t\tPadding: 4",
                "\tBlock:",
                "\tBlock:", "\t\tClass: a",
                "\tBlock:", "\t\tClass: b a",
                "\tBlock:", "\t\tClass: a", "\t\tId: x",
                "\tBlock:", "\t\tClass: a b", "\t\tId: y", "\t\tBox:", "\t\t\tPadding: 5");
        assertEquals(List.of(1.0, 2.0, 3.0, 4.0, 5.0), root.children().stream().map(StylesTest::padding).toList());
        // La regla del tipo sigue aportando lo que las demás no declaran.
        root.children().forEach(b -> assertEquals(Sides.all(Value.pt(1)), b.style().box().margin()));
    }

    @Test
    void rulesAreAppliedToTheElementInsideAnItem() {
        Box block = root(
                "\tStyles:", "\t\tFor class: card", "\t\t\tBox:", "\t\t\t\tBackground: #F1EFE8",
                "\tFlex:", "\t\tItem:", "\t\t\tGrow: 1", "\t\t\tBlock:", "\t\t\t\tClass: card")
                .children().get(0).children().get(0);
        assertEquals(new Paint.Solid("#F1EFE8"), block.style().box().background());
        assertEquals(1, block.style().item().grow(), 1e-9);
    }

    @Test
    void ruleSelectorsAreCanonical() {
        Box root = root(
                "\tStyles:", "\t\tElement: text", "\t\t\tTypography:", "\t\t\t\tAlign: center",
                "\tText: hola");
        assertEquals(TextAlign.CENTER, root.children().get(0).style().typography().textAlign());
    }

    @Test
    void unusedRulesAreAllowed() {
        Box root = root(
                "\tStyles:", "\t\tElement: Grid", "\t\t\tBox:", "\t\t\t\tPadding: 5",
                "\t\tFor class: unused", "\t\t\tBox:", "\t\t\t\tPadding: 5",
                "\tBlock:");
        assertEquals(Sides.all(Value.pt(0)), root.children().get(0).style().box().padding());
    }
}
