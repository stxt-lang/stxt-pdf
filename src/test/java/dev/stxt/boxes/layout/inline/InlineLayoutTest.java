package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.layout.LayoutFixture;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.boxes.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Con {@link FixedMetrics}, cada carácter mide 5 pt con {@code Font size: 10}. */
class InlineLayoutTest {

    private static final String TEXT = "aaaa bbbb cccc dddd eeee";

    private static List<TextLine> lines(String... props) {
        String[] doc = new String[props.length + 2];
        doc[0] = "\tText: " + TEXT;
        doc[1] = "\t\tId: T";
        System.arraycopy(props, 0, doc, 2, props.length);
        return layOut(doc).fragment("T").lines();
    }

    @Test
    void linesAndHeight() {
        LayoutFixture l = layOut("\tText: " + TEXT, "\t\tId: T", "\t\tWidth: 100");
        Fragment f = l.fragment("T");
        assertEquals(2, f.lines().size());
        assertEquals(new TextLine(0, 0, 95, 12, "aaaa bbbb cccc dddd"), f.lines().get(0));
        assertEquals(new TextLine(0, 12, 20, 12, "eeee"), f.lines().get(1));
        assertEquals(24, f.height(), 1e-9);
    }

    @Test
    void textWrapPrettyIsInheritedAndUsesKnuthPlass() {
        // El ejemplo de LineBreakerTest.prettyMinimizesTheDemerits a escala 5: ancho 50.
        String text = "a bbb c ddddd e fff";
        LayoutFixture l = layOut("\tBlock:", "\t\tText wrap: pretty",
                "\t\tText: " + text, "\t\t\tId: P", "\t\t\tWidth: 50",
                "\t\tText: " + text, "\t\t\tId: G", "\t\t\tWidth: 50", "\t\t\tText wrap: auto");
        assertEquals(List.of("a bbb", "c ddddd e", "fff"),
                l.fragment("P").lines().stream().map(TextLine::text).toList());
        assertEquals(List.of("a bbb c", "ddddd e", "fff"),
                l.fragment("G").lines().stream().map(TextLine::text).toList());
    }

    @Test
    void justifyFillsAllLinesButTheLastOfEachParagraph() {
        // Primera línea: 95 pt y 3 espacios, el sobrante (5) se reparte entre ellos.
        LayoutFixture l = layOut("\tText:", "\t\tId: T", "\t\tWidth: 100", "\t\tText align: justify", "\t\tContent >>",
                "\t\t\t" + TEXT, "", "\t\t\t" + TEXT);
        List<TextLine> lines = l.fragment("T").lines();
        assertEquals(4, lines.size());
        assertEquals(new TextLine(0, 0, 100, 12, "aaaa bbbb cccc dddd", 5.0 / 3), lines.get(0));
        assertEquals(new TextLine(0, 12, 20, 12, "eeee", 0), lines.get(1));
        assertEquals(5.0 / 3, lines.get(2).wordSpacing(), 1e-9);
        assertEquals(0, lines.get(3).wordSpacing(), 1e-9);
    }

    @Test
    void paddingReducesTheLineWidth() {
        LayoutFixture l = layOut("\tText: " + TEXT, "\t\tId: T", "\t\tWidth: 100", "\t\tPadding: 10");
        Fragment f = l.fragment("T");
        assertEquals(List.of("aaaa bbbb cccc", "dddd eeee"), f.lines().stream().map(TextLine::text).toList());
        assertEquals(44, f.height(), 1e-9);
    }

    @Test
    void alignment() {
        List<TextLine> right = lines("\t\tWidth: 100", "\t\tText align: right");
        assertEquals(5, right.get(0).x(), 1e-9);
        assertEquals(80, right.get(1).x(), 1e-9);
        List<TextLine> center = lines("\t\tWidth: 100", "\t\tText align: center");
        assertEquals(2.5, center.get(0).x(), 1e-9);
        assertEquals(40, center.get(1).x(), 1e-9);
    }

    @Test
    void lineHeightInPointsOrPercentage() {
        assertEquals(15, lines("\t\tWidth: 100", "\t\tLine height: 15").get(1).y(), 1e-9);
        assertEquals(15, lines("\t\tWidth: 100", "\t\tLine height: 150%").get(1).y(), 1e-9);
        assertEquals(28, lines("\t\tWidth: 100", "\t\tFont size: 20", "\t\tLine height: 140%").get(1).height(), 1e-9);
    }

    @Test
    void blankLineSeparatesParagraphs() {
        LayoutFixture l = layOut("\tText:", "\t\tId: T", "\t\tContent >>", "\t\t\tuna dos", "\t\t\ttres", "",
                "\t\t\tcuatro");
        assertEquals(List.of("una dos tres", "cuatro"),
                l.fragment("T").lines().stream().map(TextLine::text).toList());
    }

    @Test
    void textMixedWithBoxesGoesInAnAnonymousBox() {
        LayoutFixture l = layOut(
                "\tBlock:", "\t\tId: P",
                "\t\tText: hola", "\t\tBlock:", "\t\t\tId: C", "\t\t\tHeight: 10");
        assertEquals(12, l.rect("C").y() - 40, 1e-9);
        assertEquals(22, l.rect("P").height(), 1e-9);
    }
}
