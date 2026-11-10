package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Distribution;
import dev.stxt.pdf.model.Fit;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The element properties in the rules of {@code Styles} (reference/common/styles.md): {@code Gap}, {@code Fit},
 * {@code Marker}... are given to the element as if it declared them, with the same priority as the groups, and one that
 * the element could not declare is an error with the line of the rule.
 */
class RuleElementPropertiesTest {

    private static final DocumentReader READER = new DocumentReader();
    private static final String HEADER = Docs.header(595, 842);

    /**
     * The document: the base typography, these rules in its {@code Styles} (each line one more tab inside it) and, after
     * the line {@code ---}, its content. The rules start on line 12.
     */
    private static Box root(String... lines) {
        return root(List.of(), lines);
    }

    private static Box root(List<String> styles, String... lines) {
        StringBuilder doc = new StringBuilder(HEADER + Docs.TYPOGRAPHY);
        boolean content = false;
        for (String line : lines) {
            if (line.equals("---")) {
                content = true;
            } else {
                doc.append(content ? "\t" : "\t\t").append(line).append('\n');
            }
        }
        InlineNode document = READER.read(Docs.sectioned(doc.toString()));
        List<InlineNode> read = styles.stream().map(READER::readStyles).toList();
        return new BoxTreeBuilder().build(document, read).sections().get(0).box();
    }

    private static InvalidModelException error(String... lines) {
        return assertThrows(InvalidModelException.class, () -> root(lines));
    }

    private static Value pt(double v) {
        return Value.pt(v);
    }

    @Test
    void aRuleGivesAnElementPropertyAsIfTheElementDeclaredIt() {
        Box root = root(
                "For style: dish", "\tGap: 12",
                "For element: Image", "\tFit: crop",
                "---",
                "Stack:", "\tStyle: dish", "\tItem:", "\t\tText: a",
                "Image: test/wide.png");
        Box stack = root.children().get(0);
        assertEquals(pt(12), stack.style().stack().columnGap());
        assertEquals(pt(12), stack.style().stack().rowGap());
        // Its line is that of the rule: errors point to it.
        assertEquals(13, stack.style().line(PropertyKey.stack("gap")).number());
        assertEquals(Fit.CROP, root.children().get(1).style().image().fit());
    }

    @Test
    void theElementWinsOverItsRulesAndTheRulesKeepTheirPriority() {
        Box root = root(
                "For element: Stack", "\tGap: 1",
                "For style: a", "\tGap: 2",
                "For id: x", "\tGap: 3",
                "---",
                "Stack:", "\tItem:", "\t\tText: a",
                "Stack:", "\tStyle: a", "\tItem:", "\t\tText: a",
                "Stack:", "\tStyle: a", "\tId: x", "\tItem:", "\t\tText: a",
                "Stack:", "\tStyle: a", "\tId: y", "\tGap: 4", "\tItem:", "\t\tText: a");
        assertEquals(List.of(pt(1), pt(2), pt(3), pt(4)),
                root.inFlowChildren().stream().map(b -> b.style().stack().columnGap()).toList());
    }

    @Test
    void horizontalAndVerticalInARuleAreThoseOfTheStackNotThoseOfItsItem() {
        Box root = root(
                "For style: row", "\tHorizontal: justify",
                "---",
                "Stack:", "\tItem:", "\t\tStack:", "\t\t\tStyle: row", "\t\t\tItem:", "\t\t\t\tText: a");
        Box inner = root.children().get(0).children().get(0);
        assertEquals(Distribution.JUSTIFY, inner.style().stack().mainAxis());
        assertEquals(null, inner.style().item().horizontal());
    }

    @Test
    void growComesFromTheRuleOfTheElementTheItemWrapsAndTheItemWins() {
        Box stack = root(
                "For style: main", "\tGrow: 1", "\tShrink: 0", "\tBase size: 40",
                "---",
                "Stack:", "\tItem:", "\t\tBlock:", "\t\t\tStyle: main",
                "\tItem:", "\t\tGrow: 2", "\t\tBlock:", "\t\t\tStyle: main").children().get(0);
        ItemStyle first = stack.children().get(0).style().item();
        assertEquals(1, first.grow(), 1e-9);
        assertEquals(0, first.shrink(), 1e-9);
        assertEquals(pt(40), first.baseSize());
        assertEquals(13, stack.children().get(0).style().line(PropertyKey.item("grow")).number());
        ItemStyle second = stack.children().get(1).style().item();
        assertEquals(2, second.grow(), 1e-9);
        assertEquals(0, second.shrink(), 1e-9);
    }

    @Test
    void growToAnElementThatIsNotInAnItemIsAnError() {
        InvalidModelException e = error(
                "For style: main", "\tGrow: 1",
                "---",
                "Block:", "\tStyle: main");
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, e.errorCode());
        assertEquals(13, e.line());
        // Nor in the Area of a Grid, whose items do not grow.
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, error(
                "For style: main", "\tGrow: 1",
                "---",
                "Grid:", "\tArea:", "\t\tBlock:", "\t\t\tStyle: main").errorCode());
    }

    @Test
    void aPropertyThatTheElementCannotDeclareIsAnErrorWhenTheRuleIsApplied() {
        InvalidModelException e = error(
                "For style: dish", "\tGap: 12",
                "---",
                "Stack:", "\tStyle: dish", "\tItem:", "\t\tText: a",
                "Text: b", "\tStyle: dish");
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, e.errorCode());
        assertEquals(13, e.line());
        assertTrue(e.getMessage().contains("Text (line 19)"), e.getMessage());
    }

    @Test
    void aPropertyThatTheTypeCannotDeclareIsAnErrorWhenTheRuleIsRead() {
        // Even if no Text uses it.
        InvalidModelException e = error("For element: Text", "\tGap: 12", "---", "Block:");
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, e.errorCode());
        assertEquals(13, e.line());
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, error("For element: Section", "\tGrow: 1", "---", "Block:").errorCode());
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, error("For element: Header cell", "\tColumn sizes: *", "---",
                "Block:").errorCode());
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, error("For element: Code block", "\tClear: true", "---",
                "Block:").errorCode());
        // The base typography of these documents is already a For element: Document: this one goes apart.
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, assertThrows(InvalidModelException.class, () -> root(
                List.of("Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tGap: 12\n"), "---", "Block:"))
                .errorCode());
    }

    @Test
    void theValueIsCheckedWhenTheRuleIsReadAgainstTheTypesThatCanDeclareIt() {
        // No element allows three gaps, even if no element uses the style.
        InvalidModelException e = error("For style: x", "\tGap: 1 2 3", "---", "Block:");
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, e.errorCode());
        assertEquals(13, e.line());
        // justify is valid in a Stack: the rule is read, and the Grid that uses it gives the error.
        root("For style: x", "\tHorizontal: justify", "---", "Block:");
        InvalidModelException grid = error("For style: x", "\tHorizontal: justify",
                "---", "Grid:", "\tStyle: x", "\tArea:", "\t\tText: a");
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, grid.errorCode());
        assertEquals(13, grid.line());
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, error("For style: x", "\tColumn align: middle", "---",
                "Block:").errorCode());
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, error("For style: x", "\tBullet markers: decimal", "---",
                "Block:").errorCode());
    }

    @Test
    void aSectionOrAStyledRunTakeNoElementProperty() {
        InvalidModelException section = assertThrows(InvalidModelException.class, () -> new BoxTreeBuilder().build(
                READER.read(HEADER + Docs.TYPOGRAPHY + "\t\tFor style: x\n\t\t\tClear: true\n"
                        + "\tSection:\n\t\tStyle: x\n\t\tBlock:")));
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, section.errorCode());
        assertEquals(13, section.line());
        InvalidModelException run = error(
                "For style: x", "\tClear: true",
                "---",
                "Text: un [tramo]{x}", "\tTypography:", "\t\tMarkup: markdown");
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED_IN_RUN, run.errorCode());
        assertEquals(13, run.line());
    }

    @Test
    void columnAlignComesFromARuleAndHasOneAlignmentPerColumnOfEachTable() {
        Box table = root(
                "For element: Table", "\tColumn align: left right",
                "---",
                "Table:", "\tColumn sizes: * *", "\tBody rows:", "\t\tRow:", "\t\t\tCell: a", "\t\t\tCell: b").children().get(0);
        assertEquals(TextAlign.RIGHT, table.children().get(1).style().typography().textAlign());
        assertEquals(13, table.children().get(1).style().line(PropertyKey.internal("align")).number());
        InvalidModelException e = error(
                "For element: Table", "\tColumn align: left right",
                "---",
                "Table:", "\tColumn sizes: *", "\tBody rows:", "\t\tRow:", "\t\t\tCell: a");
        assertEquals(ErrorCode.TABLE_COLUMN_ALIGN, e.errorCode());
        assertEquals(13, e.line());
    }

    @Test
    void theMarkersAndTheTableColumnsOfAMarkdownComeFromARule() {
        Box markdown = root(
                "For element: Markdown", "\tBullet markers: dash", "\tTable column sizes: equal",
                "---",
                "Markdown:", "\tContent >>", "\t\t- uno", "\t\t", "\t\t| a | b |", "\t\t|---|---|", "\t\t| 1 | 2 |")
                .children().get(0);
        Box list = markdown.children().get(0);
        assertEquals(Marker.DASH, list.style().list().marker());
        assertEquals(13, list.style().line("marker").number());
        Box table = markdown.children().get(1);
        assertEquals(2, table.style().grid().columnSizes().size());
    }

    @Test
    void whatAMarkdownGeneratesFromARuleOfAStylesDocumentKeepsItsLine() {
        Box markdown = root(List.of("Styles (dev.stxt.pdf):\n\tFor element: Markdown\n\t\tBullet markers: dash\n"),
                "---",
                "Markdown:", "\tContent >>", "\t\t- uno").children().get(0);
        assertEquals(SourceLine.inStyles(3), markdown.children().get(0).style().line("marker"));
    }

    @Test
    void theMarkerOfAListAndTheColumnSizesOfATableCanComeOnlyFromARule() {
        Box root = root(
                "For element: List", "\tMarker: dash",
                "For style: two", "\tColumn sizes: * 80",
                "---",
                "List:", "\tList item:", "\t\tParagraph: Uno",
                "Table:", "\tStyle: two", "\tBody rows:", "\t\tRow:", "\t\t\tCell: a", "\t\t\tCell: b");
        assertEquals(Marker.DASH, root.children().get(0).style().list().marker());
        assertEquals(List.of(new Value.Fraction(1), pt(80)), root.children().get(1).style().grid().columnSizes());
        // Without them, the engine says so: the schema no longer requires them.
        assertEquals(ErrorCode.LIST_MARKER_MISSING, error("---", "List:", "\tList item:", "\t\tParagraph: Uno")
                .errorCode());
        InvalidModelException table = error("---", "Table:", "\tBody rows:", "\t\tRow:", "\t\t\tCell: a");
        assertEquals(ErrorCode.TABLE_COLUMN_SIZES_MISSING, table.errorCode());
        assertEquals(13, table.line());
    }

    @Test
    void aMarkerImageComesFromTheRuleOfAnItem() {
        Box list = root(
                "For style: yes", "\tMarker image: test/check.svg", "\t\tAlt: Sí",
                "---",
                "List:", "\tMarker: image", "\tList item:", "\t\tStyle: yes", "\t\tParagraph: Cubierto").children().get(0);
        Box item = list.children().get(0);
        assertEquals("test/check.svg", item.style().list().markerImage());
        assertEquals("Sí", item.style().list().markerAlt());
    }
}
