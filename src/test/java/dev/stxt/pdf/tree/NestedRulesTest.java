package dev.stxt.pdf.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.stxt.DocumentReader;

/**
 * The nested rules of {@code Styles} (reference/common/styles.md): a rule written inside another applies to the
 * elements it names that are inside an element the outer one applies to, after the loose rules of its level, and
 * among nested rules the one of the closest ancestor wins.
 */
class NestedRulesTest {

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

    private static Sides<Value> padding(Box b) {
        return b.style().box().padding();
    }

    private static Sides<Value> all(double pt) {
        return Sides.all(Value.pt(pt));
    }

    @Test
    void aNestedRuleAppliesWithinTheElementsOfTheOuterOneAtAnyDepth() {
        Box root = root(
                "For style: card", "\tFor element: Paragraph", "\t\tBox:", "\t\t\tPadding: 3",
                "---",
                "Block:", "\tStyle: card", "\tParagraph: a", "\tStack:", "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tParagraph: b",
                "Paragraph: c");
        Box card = root.children().get(0);
        Box direct = card.children().get(0);
        Box deep = card.children().get(1).children().get(0).children().get(0);
        assertEquals(all(3), padding(direct));
        assertEquals(all(3), padding(deep));
        // Its line is that of the property in the nested rule.
        assertEquals(15, direct.style().line("padding").number());
        assertEquals(all(0), padding(root.children().get(1)));
    }

    @Test
    void withinALevelTheNestedRuleGoesAfterTheLooseOneAndTheLevelsDoNotChange() {
        Box root = root(
                "For element: Paragraph", "\tBox:", "\t\tPadding: 1",
                "For style: card", "\tFor element: Paragraph", "\t\tBox:", "\t\t\tPadding: 2",
                "For style: loose", "\tBox:", "\t\tPadding: 7",
                "---",
                "Block:", "\tStyle: card", "\tParagraph: a", "\tParagraph: b", "\t\tStyle: loose",
                "\tParagraph: c", "\t\tBox:", "\t\t\tPadding: 9",
                "Paragraph: d");
        Box card = root.children().get(0);
        // The nested For element beats the loose For element...
        assertEquals(all(2), padding(card.children().get(0)));
        // ...but not a loose For style, which is a higher level, nor what the element declares.
        assertEquals(all(7), padding(card.children().get(1)));
        assertEquals(all(9), padding(card.children().get(2)));
        assertEquals(all(1), padding(root.children().get(1)));
    }

    @Test
    void betweenNestedRulesTheOneOfTheClosestAncestorWinsWhateverTheirOrder() {
        String[] rules = {
                "For style: notice", "\tFor element: Paragraph", "\t\tBox:", "\t\t\tPadding: 2",
                "For style: card", "\tFor element: Paragraph", "\t\tBox:", "\t\t\tPadding: 1"};
        Box root = root(concat(rules,
                "---",
                "Block:", "\tStyle: card", "\tBlock:", "\t\tStyle: notice", "\t\tParagraph: a",
                "Block:", "\tStyle: notice", "\tBlock:", "\t\tStyle: card", "\t\tParagraph: b"));
        assertEquals(all(2), padding(root.children().get(0).children().get(0).children().get(0)));
        assertEquals(all(1), padding(root.children().get(1).children().get(0).children().get(0)));
    }

    @Test
    void withTheSameAncestorTheOneLaterInStylesWins() {
        Box root = root(
                "For style: card", "\tFor element: Paragraph", "\t\tBox:", "\t\t\tPadding: 1",
                "For style: panel", "\tFor element: Paragraph", "\t\tBox:", "\t\t\tPadding: 2",
                "---",
                "Block:", "\tStyle: panel card", "\tParagraph: a");
        assertEquals(all(2), padding(root.children().get(0).children().get(0)));
    }

    @Test
    void aNestedRuleGivesElementPropertiesToo() {
        // The Grow of each dish of a menu, given to the Block its Item wraps.
        Box stack = root(
                "For style: dish", "\tFor element: Block", "\t\tGrow: 1",
                "---",
                "Stack:", "\tStyle: dish", "\tItem:", "\t\tBlock:", "\t\t\tText: a", "\tItem:", "\t\tBlock:", "\t\t\tText: b",
                "Stack:", "\tItem:", "\t\tBlock:", "\t\t\tText: c").children().get(0);
        assertEquals(1, stack.children().get(0).style().item().grow(), 1e-9);
        assertEquals(1, stack.children().get(1).style().item().grow(), 1e-9);
        assertEquals(14, stack.children().get(0).style().line(PropertyKey.item("grow")).number());
    }

    @Test
    void theRulesOfACellCanBeNestedInTheRuleOfItsTable() {
        Box root = root(
                "For style: pricing", "\tFor element: Header cell", "\t\tTypography:", "\t\t\tBold: true",
                "\tFor column: 2", "\t\tBox:", "\t\t\tPadding: 4",
                "---",
                "Table:", "\tStyle: pricing", "\tColumn sizes: * *",
                "\tHeader rows:", "\t\tRow:", "\t\t\tCell: a", "\t\t\tCell: b",
                "\tBody rows:", "\t\tRow:", "\t\t\tCell: c", "\t\t\tCell: d",
                "Table:", "\tColumn sizes: * *", "\tHeader rows:", "\t\tRow:", "\t\t\tCell: e", "\t\t\tCell: f",
                "\tBody rows:", "\t\tRow:", "\t\t\tCell: g", "\t\t\tCell: h");
        Box pricing = root.children().get(0);
        assertEquals(FontWeight.BOLD, pricing.children().get(0).style().typography().fontWeight());
        assertEquals(all(4), padding(pricing.children().get(1)));
        assertEquals(all(0), padding(pricing.children().get(0)));
        Box other = root.children().get(1);
        assertEquals(FontWeight.NORMAL, other.children().get(0).style().typography().fontWeight());
        assertEquals(all(0), padding(other.children().get(1)));
    }

    @Test
    void theRowOfACellAndASectionAreAncestorsWithTheirStyle() {
        Box table = root(
                "For style: total", "\tFor element: Text", "\t\tTypography:", "\t\t\tBold: true",
                "---",
                "Table:", "\tColumn sizes: *", "\tBody rows:", "\t\tRow:", "\t\t\tStyle: total", "\t\t\tCell:", "\t\t\t\tText: a",
                "\t\tRow:", "\t\t\tCell:", "\t\t\t\tText: b").children().get(0);
        assertEquals(FontWeight.BOLD, table.children().get(0).children().get(0).style().typography().fontWeight());
        assertEquals(FontWeight.NORMAL, table.children().get(1).children().get(0).style().typography().fontWeight());
        Box section = new BoxTreeBuilder().build(READER.read(HEADER + Docs.TYPOGRAPHY
                + "\t\tFor style: annex\n\t\t\tFor element: Text\n\t\t\t\tBox:\n\t\t\t\t\tPadding: 5\n"
                + "\tSection:\n\t\tStyle: annex\n\t\tText: a\n")).sections().get(0).box();
        assertEquals(all(5), padding(section.children().get(0)));
    }

    @Test
    void whatAMarkdownGeneratesIsInsideIt() {
        Box markdown = root(
                "For style: card", "\tFor element: Paragraph", "\t\tBox:", "\t\t\tPadding: 3",
                "---",
                "Markdown:", "\tStyle: card", "\tContent >>", "\t\tUno", "\t\t", "\t\tDos").children().get(0);
        assertEquals(all(3), padding(markdown.children().get(0)));
        assertEquals(all(3), padding(markdown.children().get(1)));
    }

    @Test
    void aStyleWithOnlyNestedRulesIsAnErrorOutsideItsContext() {
        Box root = root(
                "For style: card", "\tFor style: body", "\t\tBox:", "\t\t\tPadding: 3",
                "---",
                "Block:", "\tStyle: card", "\tBlock:", "\t\tStyle: body");
        assertEquals(all(3), padding(root.children().get(0).children().get(0)));
        InvalidModelException e = error(
                "For style: card", "\tFor style: body", "\t\tBox:", "\t\t\tPadding: 3",
                "---",
                "Block:", "\tStyle: body");
        assertEquals(ErrorCode.STYLE_WITHOUT_RULE, e.errorCode());
        assertEquals(18, e.line());
        assertTrue(e.getMessage().contains("inside For style: card (line 12)"), e.getMessage());
        // With a loose rule of the same name, the style is valid anywhere, and inside a card both apply.
        Box both = root(
                "For style: card", "\tFor style: body", "\t\tBox:", "\t\t\tPadding: 3",
                "For style: body", "\tBox:", "\t\tPadding: 1", "\t\tMargin: 2",
                "---",
                "Block:", "\tStyle: body",
                "Block:", "\tStyle: card", "\tBlock:", "\t\tStyle: body");
        assertEquals(all(1), padding(both.children().get(0)));
        Box inner = both.children().get(1).children().get(0);
        assertEquals(all(3), padding(inner));
        assertEquals(all(2), inner.style().box().margin());
    }

    @Test
    void aStyledRunTakesTheNestedRulesOfItsContext() {
        Box root = root(
                "For style: card", "\tFor style: price", "\t\tTypography:", "\t\t\tBold: true",
                "---",
                "Block:", "\tStyle: card", "\tText: Son [9 €]{price}", "\t\tTypography:", "\t\t\tMarkup: markdown");
        Box text = root.children().get(0).children().get(0);
        assertEquals(FontWeight.BOLD, text.formattedRuns().get(1).format().style().weight());
        InvalidModelException e = error(
                "For style: card", "\tFor style: price", "\t\tTypography:", "\t\t\tBold: true",
                "---",
                "Text: Son [9 €]{price}", "\tTypography:", "\t\tMarkup: markdown");
        assertEquals(ErrorCode.STYLE_WITHOUT_RULE, e.errorCode());
        assertEquals(17, e.line());
    }

    @Test
    void rulesNestOneLevelAndOnlyInsideARuleOfAnElementOfTheDocument() {
        InvalidModelException deep = error("For style: a", "\tFor style: b", "\t\tFor style: c", "\t\t\tBox:",
                "\t\t\t\tPadding: 1", "---", "Block:");
        assertEquals(ErrorCode.STYLE_NESTED_TOO_DEEP, deep.errorCode());
        assertEquals(14, deep.line());
        // A For column cannot contain rules either: the schema does not allow it.
        for (String outer : List.of("For element: Header cell", "For element: TOC 1", "For element: Footnote area",
                "For element: Code block")) {
            InvalidModelException e = error(outer, "\tFor element: Text", "\t\tBox:", "\t\t\tPadding: 1",
                    "---", "Block:");
            assertEquals(ErrorCode.STYLE_NOT_ALLOWED, e.errorCode(), outer);
            assertEquals(13, e.line(), outer);
        }
        // The base typography of these documents is already a For element: Document: this one goes apart.
        InvalidModelException document = assertThrows(InvalidModelException.class, () -> root(
                List.of("Styles (dev.stxt.pdf):\n\tFor element: Document\n\t\tFor element: Text\n\t\t\tBox:\n"
                        + "\t\t\t\tPadding: 1\n"), "---", "Block:"));
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, document.errorCode());
        assertEquals(3, document.line());
        for (String inner : List.of("For element: Document", "For element: Section", "For element: TOC 1",
                "For element: Footnote area")) {
            InvalidModelException e = error("For style: a", "\t" + inner, "\t\tTypography:", "\t\t\tBold: true",
                    "---", "Block:");
            assertEquals(ErrorCode.STYLE_NOT_ALLOWED, e.errorCode(), inner);
            assertEquals(13, e.line(), inner);
        }
        // A Section contains nested rules; a Code block, generated inside its Markdown, is reached by them.
        root("For element: Section", "\tFor element: Code block", "\t\tBox:", "\t\t\tPadding: 1",
                "For style: a", "\tFor element: Code block", "\t\tBox:", "\t\t\tPadding: 1", "---", "Block:");
    }

    @Test
    void theSameSelectorTwiceInsideARuleIsAnErrorButNotInTwoRules() {
        InvalidModelException e = error("For style: a", "\tFor element: Text", "\t\tBox:", "\t\t\tPadding: 1",
                "\tFor element: text", "\t\tBox:", "\t\t\tPadding: 2", "---", "Block:");
        assertEquals(ErrorCode.STYLE_DUPLICATE, e.errorCode());
        assertEquals(16, e.line());
        root("For element: Text", "\tBox:", "\t\tPadding: 1",
                "For style: a", "\tFor element: Text", "\t\tBox:", "\t\t\tPadding: 2",
                "For style: b", "\tFor element: Text", "\t\tBox:", "\t\t\tPadding: 3", "---", "Block:");
    }

    @Test
    void aNestedRuleOfASeparateStylesDocumentSaysSoInItsErrors() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> root(
                List.of("Styles (dev.stxt.pdf):\n\tFor style: card\n\t\tFor element: Text\n\t\t\tGap: 1\n"),
                "---", "Block:"));
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, e.errorCode());
        assertEquals(4, e.line());
        assertTrue(e.inStyles());
        Box root = root(
                List.of("Styles (dev.stxt.pdf):\n\tFor style: card\n\t\tFor element: Text\n\t\t\tBox:\n\t\t\t\tPadding: 3\n"),
                "---", "Block:", "\tStyle: card", "\tText: a");
        Box text = root.children().get(0).children().get(0);
        assertEquals(all(3), padding(text));
        assertEquals(SourceLine.inStyles(5), text.style().line("padding"));
    }

    private static String[] concat(String[] first, String... rest) {
        String[] all = new String[first.length + rest.length];
        System.arraycopy(first, 0, all, 0, first.length);
        System.arraycopy(rest, 0, all, first.length, rest.length);
        return all;
    }
}
