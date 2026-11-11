package dev.stxt.pdf.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.CaptionPosition;
import dev.stxt.pdf.model.FigureCounter;

/**
 * The figures in the box tree (reference/components/figure.md): the order of the caption and the content, the
 * numbering with the two counters, the label before the caption and the tables of contents that list them.
 */
class FiguresTest {

    private static final String HEADER = Docs.header(595, 842) + Docs.TYPOGRAPHY;

    private static final String IMAGE = "\t\tImage: test/wide.png\n\t\t\tAlt: Paisaje";
    private static final String TABLE = "\t\tTable:\n\t\t\tColumn sizes: *\n\t\t\tBody rows:\n\t\t\t\tRow:\n\t\t\t\t\tCell: a";

    private static BoxTreeBuilder.Result build(String... lines) {
        return new BoxTreeBuilder().build(Docs.read(HEADER, lines));
    }

    private static InvalidModelException error(String... lines) {
        return assertThrows(InvalidModelException.class, () -> build(lines));
    }

    /** The {@code Figure} boxes of the document, in order. */
    private static List<Box> figures(BoxTreeBuilder.Result tree) {
        List<Box> out = new ArrayList<>();
        collect(tree.root(), out);
        return out;
    }

    private static void collect(Box box, List<Box> out) {
        if (box.element() == Element.FIGURE) {
            out.add(box);
        }
        box.children().forEach(c -> collect(c, out));
    }

    @Test
    void aFigureIsNumberedByDefaultAndItsCaptionGoesBelow() {
        Box f = figures(build("\tSection:", "\t\tFigure:", "\t\t\tId: f", "\t\t\tCaption: Esquema", "\t" + IMAGE.replace("\n", "\n\t"))).get(0);
        assertEquals(List.of(Element.IMAGE, Element.CAPTION), f.children().stream().map(Box::element).toList());
        Box caption = f.caption();
        assertSame(f.children().get(1), caption);
        assertEquals("1 Esquema", caption.text());
        assertEquals("1 Esquema", f.headingText());
        assertEquals("1", f.number());
        assertEquals(Element.Role.FIGURE_GROUP, f.role());
        assertEquals(Element.Role.CAPTION, caption.role());
        assertEquals(CaptionPosition.BOTTOM, f.style().figure().captionPosition());
        assertEquals(FigureCounter.FIGURES, f.style().figure().counter());
        // It is not split across pages: the engine keeps it together, without the author declaring it.
        assertTrue(f.style().breaks().keepTogether());
        assertFalse(f.style().isDeclared(PropertyKey.KEEP_TOGETHER));
        // The caption inherits the typography of the figure.
        assertEquals(10, caption.style().typography().fontSize());
    }

    @Test
    void aTableFigureCountsAmongTheTablesAndItsCaptionGoesAbove() {
        BoxTreeBuilder.Result tree = build("\tSection:", "\t\tFigure:", "\t\t\tCaption: Esquema", "\t" + IMAGE.replace("\n", "\n\t"),
                "\t\tFigure:", "\t\t\tId: t", "\t\t\tCaption: Bornes", "\t" + TABLE.replace("\n", "\n\t"));
        Box t = figures(tree).get(1);
        assertEquals(List.of(Element.CAPTION, Element.TABLE), t.children().stream().map(Box::element).toList());
        assertEquals("1 Bornes", t.caption().text());
        assertEquals("1", t.number());
        assertEquals(CaptionPosition.TOP, t.style().figure().captionPosition());
        assertEquals(FigureCounter.TABLES, t.style().figure().counter());
        // The table breaks as always: the caption keeps with it, and the figure is not kept together.
        assertFalse(t.style().breaks().keepTogether());
        assertTrue(t.caption().style().breaks().keepWithNext());
        assertFalse(t.caption().style().breaks().keepWithPrevious());
    }

    @Test
    void captionPositionAndCounterCanBeDeclared() {
        BoxTreeBuilder.Result tree = build("\tSection:", "\t\tFigure:", "\t\t\tCaption position: top", "\t\t\tCaption: Esquema",
                "\t" + IMAGE.replace("\n", "\n\t"),
                "\t\tFigure:", "\t\t\tFigure counter: figures", "\t\t\tCaption position: bottom", "\t\t\tCaption: Bornes",
                "\t" + TABLE.replace("\n", "\n\t"));
        List<Box> figures = figures(tree);
        assertEquals(List.of(Element.CAPTION, Element.IMAGE), figures.get(0).children().stream().map(Box::element).toList());
        assertEquals(List.of(Element.TABLE, Element.CAPTION), figures.get(1).children().stream().map(Box::element).toList());
        // Both count among the figures: 1 and 2.
        assertEquals(List.of("1", "2"), figures.stream().map(Box::number).toList());
        assertEquals("2 Bornes", figures.get(1).caption().text());
        Box caption = figures.get(1).caption();
        assertFalse(caption.style().breaks().keepWithNext());
        assertTrue(caption.style().breaks().keepWithPrevious());
    }

    /**
     * {@code Number label} and {@code Number format} apply as to the headings; {@code Numbered: false} declared in the
     * figure or in a rule leaves it without a number and it does not count, and what it inherits does not count.
     */
    @Test
    void labelFormatAndNumberedFalse() {
        // The figures are inside a Block with Numbered: false, which they inherit and which does not count.
        BoxTreeBuilder.Result tree = build("\tStyles:",
                "\t\tFor element: Figure", "\t\t\tTypography:", "\t\t\t\tNumber label: Figura {number}.", "\t\t\t\tNumber format: upper-alpha",
                "\t\tFor style: plain", "\t\t\tTypography:", "\t\t\t\tNumbered: false",
                "\tSection:", "\t\tBlock:", "\t\t\tTypography:", "\t\t\t\tNumbered: false",
                "\t\t\tFigure:", "\t\t\t\tId: a", "\t\t\t\tCaption: Esquema", "\t\t" + IMAGE.replace("\n", "\n\t\t"),
                "\t\t\tFigure:", "\t\t\t\tStyle: plain", "\t\t\t\tCaption: Sin número", "\t\t" + IMAGE.replace("\n", "\n\t\t"),
                "\t\t\tFigure:", "\t\t\t\tTypography:", "\t\t\t\t\tNumbered: false", "\t\t\t\tCaption: Tampoco", "\t\t" + IMAGE.replace("\n", "\n\t\t"),
                "\t\t\tFigure:", "\t\t\t\tId: b", "\t\t\t\tCaption: Detalle", "\t\t" + IMAGE.replace("\n", "\n\t\t"),
                "\t\tText: Véanse las figuras {number of: a} y {number of: b}.");
        List<Box> figures = figures(tree);
        assertEquals(java.util.Arrays.asList("A", null, null, "B"), figures.stream().map(Box::number).toList());
        assertEquals(List.of("Figura A. Esquema", "Sin número", "Tampoco", "Figura B. Detalle"),
                figures.stream().map(Box::headingText).toList());
    }

    @Test
    void theCaptionCanBeABlockWithFormat() {
        Box f = figures(build("\tSection:", "\t\tFigure:", "\t\t\tCaption:", "\t\t\t\tTypography:", "\t\t\t\t\tMarkup: markdown",
                "\t\t\t\tContent >>", "\t\t\t\t\tUn **esquema** largo.", "\t" + IMAGE.replace("\n", "\n\t"))).get(0);
        assertEquals("1 Un esquema largo.", f.caption().text());
        assertEquals("1 ", f.caption().formattedRuns().get(0).text());
        assertTrue(f.caption().formattedRuns().get(2).format().bold());
    }

    @Test
    void tablesOfContentsListTheFiguresOrTheTables() {
        BoxTreeBuilder.Result tree = build("\tSection:",
                "\t\tTable of contents:", "\t\t\tId: figures", "\t\t\tEntries: figures",
                "\t\tTable of contents:", "\t\t\tId: tables", "\t\t\tEntries: tables",
                "\t\tFigure:", "\t\t\tCaption: Esquema", "\t" + IMAGE.replace("\n", "\n\t"),
                "\t\tFigure:", "\t\t\tId: t", "\t\t\tCaption: Bornes", "\t" + TABLE.replace("\n", "\n\t"),
                "\t\tFigure:", "\t\t\tIn table of contents: false", "\t\t\tCaption: Oculta", "\t" + IMAGE.replace("\n", "\n\t"),
                "\t\tFigure:", "\t\t\tId: d", "\t\t\tCaption: Detalle", "\t" + IMAGE.replace("\n", "\n\t"));
        Box section = tree.sections().get(0).box();
        Box figures = section.children().get(0);
        Box tables = section.children().get(1);
        assertEquals(2, figures.children().size());
        assertEquals(List.of(Element.TOC_1, Element.TOC_1), figures.children().stream().map(Box::element).toList());
        assertEquals(List.of("1 Esquema", "3 Detalle"), figures.children().stream()
                .map(e -> e.children().get(0).text()).toList());
        // The hidden figure keeps its number (2) and is not listed. A figure without Id gets an anchor.
        assertEquals(List.of("#(figure 1)", "#d"), figures.children().stream().map(e -> e.style().link()).toList());
        assertEquals("{page of: (figure 1)}", figures.children().get(0).children().get(2).text());
        assertEquals(1, tables.children().size());
        assertEquals("1 Bornes", tables.children().get(0).children().get(0).text());
        assertEquals("#t", tables.children().get(0).style().link());
        // The headings are listed as always, by the other tables of contents.
        assertNull(figures(tree).get(1).anchor());
    }

    @Test
    void aFigureHasExactlyOneElementBesidesItsCaption() {
        InvalidModelException two = error("\tSection:", "\t\tFigure:", "\t\t\tCaption: Pie", "\t" + IMAGE.replace("\n", "\n\t"),
                "\t" + IMAGE.replace("\n", "\n\t"));
        assertEquals(ErrorCode.FIGURE_CONTENT, two.errorCode());
        assertEquals(17, two.line());
        InvalidModelException none = error("\tSection:", "\t\tFigure:", "\t\t\tCaption: Pie");
        assertEquals(ErrorCode.FIGURE_CONTENT, none.errorCode());
        assertEquals(13, none.line());
    }

    @Test
    void aFigureIsContentAndNeedsItsAlt() {
        InvalidModelException image = error("\tSection:", "\t\tFigure:", "\t\t\tCaption: Pie", "\t\t\tImage: test/wide.png");
        assertEquals(ErrorCode.FIGURE_WITHOUT_ALT, image.errorCode());
        assertEquals(15, image.line());
        InvalidModelException block = error("\tSection:", "\t\tFigure:", "\t\t\tCaption: Pie", "\t\t\tBlock:", "\t\t\t\tText: Hola");
        assertEquals(ErrorCode.FIGURE_WITHOUT_ALT, block.errorCode());
        assertEquals(13, block.line());
        InvalidModelException table = error("\tSection:", "\t\tFigure:", "\t\t\tAlt: Tabla", "\t\t\tCaption: Pie",
                "\t" + TABLE.replace("\n", "\n\t"));
        assertEquals(ErrorCode.FIGURE_PROPERTY_WITHOUT_EFFECT, table.errorCode());
        assertEquals(14, table.line());
        // With a Block, the figure carries its own Alt.
        Box f = figures(build("\tSection:", "\t\tFigure:", "\t\t\tAlt: Dos fotos", "\t\t\tCaption: Pie", "\t\t\tBlock:",
                "\t\t\t\tText: Hola")).get(0);
        assertEquals("Dos fotos", f.style().image().alt());
    }

    @Test
    void aTableOfContentsOfFiguresHasNoLevelsAndNeedsSomeFigure() {
        InvalidModelException levels = error("\tSection:", "\t\tFigure:", "\t\t\tCaption: Pie", "\t" + IMAGE.replace("\n", "\n\t"),
                "\t\tTable of contents:", "\t\t\tEntries: figures", "\t\t\tLevels: 1 2");
        assertEquals(ErrorCode.LEVELS_WITHOUT_HEADINGS, levels.errorCode());
        assertEquals(19, levels.line());
        InvalidModelException empty = error("\tSection:", "\t\tFigure:", "\t\t\tCaption: Pie", "\t" + IMAGE.replace("\n", "\n\t"),
                "\t\tTable of contents:", "\t\t\tEntries: tables");
        assertEquals(ErrorCode.TABLE_OF_CONTENTS_EMPTY, empty.errorCode());
        assertEquals(17, empty.line());
    }

    @Test
    void anUnnumberedFigureHasNoNumberToRefer() {
        InvalidModelException e = error("\tSection:", "\t\tFigure:", "\t\t\tId: f", "\t\t\tTypography:", "\t\t\t\tNumbered: false",
                "\t\t\tCaption: Pie", "\t" + IMAGE.replace("\n", "\n\t"), "\t\tText: La figura {number of: f}.");
        assertEquals(ErrorCode.REFERENCE_NOT_NUMBERED, e.errorCode());
    }

    /** A figure with an image, or with a table, as a child of a {@code Section}. */
    private static String[] figure(String caption, boolean table) {
        return new String[] {"\t\tFigure:", "\t\t\tCaption: " + caption, "\t" + (table ? TABLE : IMAGE).replace("\n", "\n\t")};
    }

    private static String[] lines(Object... parts) {
        List<String> out = new ArrayList<>();
        for (Object p : parts) {
            out.addAll(p instanceof String[] a ? List.of(a) : List.of((String) p));
        }
        return out.toArray(String[]::new);
    }

    private static String numbered(int level) {
        return "\t\tFor element: Heading " + level + "\n\t\t\tTypography:\n\t\t\t\tNumbered: true\n";
    }

    private static final String NUMBERED_HEADINGS = "\tStyles:\n" + numbered(1) + numbered(2) + numbered(3);
    private static final String UNNUMBERED = "\t\t\tTypography:\n\t\t\t\tNumbered: false";

    /**
     * {@code Figure numbering: chapter}: the counters of the figures and of the tables start again in each chapter,
     * and the number of the chapter goes first.
     */
    @Test
    void figuresAreNumberedByChapter() {
        BoxTreeBuilder.Result tree = build(lines(NUMBERED_HEADINGS, "\tSection:", "\t\tFigure numbering: chapter",
                "\t\tHeading 1:", "\t\t\tText: Uno", figure("a", false), figure("b", false), figure("t", true),
                "\t\tHeading 2:", "\t\t\tText: Apartado", figure("c", false),
                "\t\tHeading 1:", "\t\t\tText: Dos", figure("d", false), figure("u", true),
                "\t\tText: Véase la figura {number of: f}.",
                "\t\tFigure:", "\t\t\tId: f", "\t\t\tCaption: e", "\t" + IMAGE.replace("\n", "\n\t")));
        assertEquals(List.of("1.1", "1.2", "1.1", "1.3", "2.1", "2.1", "2.2"),
                figures(tree).stream().map(Box::number).toList());
        assertEquals("2.2 e", figures(tree).get(6).caption().text());
    }

    /**
     * The chapter is the numbered heading with no numbered heading above it: under a title without a number, the
     * numbered {@code Heading 2}, each written with its {@code Number format}. A heading without a number of the level
     * of the chapters or above ends the chapter: its figures carry their number alone, and start again.
     */
    @Test
    void theChapterIsTheNumberedHeadingWithNoneAbove() {
        BoxTreeBuilder.Result tree = build(lines(NUMBERED_HEADINGS,
                "\t\tFor style: annex", "\t\t\tTypography:", "\t\t\t\tNumber format: upper-alpha",
                "\t\tFor element: Figure", "\t\t\tTypography:", "\t\t\t\tNumber label: Figura {number}.",
                "\tSection:", "\t\tFigure numbering: chapter",
                figure("antes", false),
                "\t\tHeading 1:", "\t\t\tText: Manual", UNNUMBERED, figure("portada", false),
                "\t\tHeading 2:", "\t\t\tText: Instalación", figure("a", false),
                "\t\tHeading 3:", "\t\t\tText: Bornes", figure("b", false),
                "\t\tHeading 2:", "\t\t\tText: Uso", figure("c", false),
                "\t\tHeading 2:", "\t\t\tText: Créditos", UNNUMBERED, figure("d", false), figure("e", false),
                "\t\tHeading 1:", "\t\t\tText: Anexos", UNNUMBERED, figure("f", false),
                "\t\tHeading 2:", "\t\t\tText: Tarifas", "\t\t\tStyle: annex", figure("g", false), figure("h", false)));
        assertEquals(List.of("1", "1", "1.1", "1.2", "2.1", "1", "2", "1", "A.1", "A.2"),
                figures(tree).stream().map(Box::number).toList());
        assertEquals("Figura A.2. h", figures(tree).get(9).caption().text());
    }

    /**
     * {@code Figure numbering} is of each section: the sections that number in a row follow the counters of the
     * document, which the figures numbered by chapter do not advance.
     */
    @Test
    void figureNumberingIsOfEachSection() {
        BoxTreeBuilder.Result tree = build(lines(NUMBERED_HEADINGS,
                "\tSection:", figure("a", false), figure("b", false),
                "\tSection:", "\t\tFigure numbering: chapter", "\t\tHeading 1:", "\t\t\tText: Uno", figure("c", false), figure("d", false),
                "\tSection:", "\t\tFigure numbering: continue", figure("e", false)));
        assertEquals(List.of("1", "2", "1.1", "1.2", "3"), figures(tree).stream().map(Box::number).toList());
    }
}
