package dev.stxt.pdf.layout.block;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.tree.Element;

/**
 * Where the block quotes and the dividers of a {@code Markdown} go (reference/components/markdown.md). They have no
 * {@code Id}, so they are looked up among the children of the fragment of their {@code Markdown}, whose content box
 * is 515 wide; the lines of text are 12 high.
 */
class MarkdownQuotesLayoutTest {

    private static void assertBox(double x, double y, double width, double height, Fragment f) {
        assertEquals(List.of(x, y, width, height), List.of(f.x(), f.y(), f.width(), f.height()));
    }

    @Test
    void aBlockQuoteSetsItsContentInFromItsLine() {
        Fragment md = layOut("\tMarkdown:", "\t\tId: M", "\t\tContent >>", "\t\t\tAntes.", "", "\t\t\t> Una cita.",
                "", "\t\t\tDespués.").fragment("M");
        Fragment quote = md.children().get(1);
        assertEquals(Element.BLOCK_QUOTE, quote.box().element());
        assertBox(0, 12, 515, 12, quote);
        assertEquals(new Insets(0, 0, 0, 2), quote.border());
        assertEquals(new Insets(0, 0, 0, 12), quote.padding());
        // Its paragraph, in its content box: 515 less the line of 2 and the padding of 12.
        assertBox(0, 0, 501, 12, quote.children().get(0));
        assertBox(0, 24, 515, 12, md.children().get(2));
    }

    @Test
    void aDividerIsALineAcrossTheWholeWidthWithNoSpaceOfItsOwn() {
        Fragment md = layOut("\tMarkdown:", "\t\tId: M", "\t\tContent >>", "\t\t\tAntes.", "", "\t\t\t---", "",
                "\t\t\tDespués.").fragment("M");
        Fragment divider = md.children().get(1);
        assertEquals(Element.DIVIDER, divider.box().element());
        assertBox(0, 12, 515, 0.5, divider);
        assertEquals(new Insets(0.5, 0, 0, 0), divider.border());
        assertBox(0, 12.5, 515, 12, md.children().get(2));
        assertEquals(24.5, md.height());
    }

    @Test
    void theMarginsOfTheParagraphsSeparateThemFromTheDivider() {
        // The margins of the paragraphs do not merge through the divider, which has a border.
        Fragment md = layOut("\tStyles:", "\t\tFor element: Paragraph", "\t\t\tBox:", "\t\t\t\tMargin: 6 0",
                "\tMarkdown:", "\t\tId: M", "\t\tContent >>", "\t\t\tAntes.", "", "\t\t\t---", "",
                "\t\t\tDespués.").fragment("M");
        assertBox(0, 18, 515, 0.5, md.children().get(1));
        assertBox(0, 24.5, 515, 12, md.children().get(2));
    }
}
