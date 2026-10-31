package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.tree.Element;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code Table of contents} (reference/components/table-of-contents.md). Pages of 180 pt with margins of 40: page
 * area of 515 × 100; with {@code FixedMetrics} and {@code Size: 10}, each character measures 5 pt and each line, 12.
 */
class TableOfContentsTest {

    private static final double PAGE = 180;

    /** The table of contents and, after it, three headings separated by 100 pt: they fall on pages 1, 2 and 3. */
    private static String[] document(String contents, String... before) {
        String[] body = {
                "\tTable of contents:", "\t\tId: toc", contents,
                "\tHeading 1:", "\t\tText: Intro",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tHeading 2:", "\t\tText: Detalle",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tHeading 1:", "\t\tId: last", "\t\tText: Final"};
        return java.util.stream.Stream.concat(java.util.Arrays.stream(before), java.util.Arrays.stream(body))
                .toArray(String[]::new);
    }

    private static List<Fragment> entries(LayoutFixture l) {
        return l.fragment("toc").children();
    }

    private static String text(Fragment f) {
        return f.lines().stream().map(x -> x.text()).reduce((a, b) -> a + "|" + b).orElse("");
    }

    /**
     * A heading with {@code In table of contents: false} is still a heading (the table of contents sees it in its place
     * and the pages do not change) but it has no entry (reference/components/table-of-contents.md).
     */
    @Test
    void aHeadingWithInTableOfContentsFalseHasNoEntry() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 2",
                "\tHeading 1:", "\t\tIn table of contents: false", "\t\tText: Portada"));
        List<Fragment> entries = entries(l);
        assertEquals(3, entries.size());
        assertEquals(List.of("Intro", "Detalle", "Final"), entries.stream().map(e -> text(e.children().get(0))).toList());
    }

    @Test
    void oneEntryPerHeadingWithItsTitleALeaderAndItsPage() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 2"));
        assertEquals(3, l.pages());
        List<Fragment> entries = entries(l);
        assertEquals(3, entries.size());
        String[] titles = {"Intro", "Detalle", "Final"};
        String[] pages = {"1", "2", "3"};
        for (int i = 0; i < 3; i++) {
            Fragment entry = entries.get(i);
            assertEquals(Element.tocEntry(i == 1 ? 2 : 1), entry.box().element());
            assertEquals(12 * i, entry.y(), 1e-9);
            assertEquals(3, entry.children().size());
            Fragment title = entry.children().get(0);
            Fragment leader = entry.children().get(1);
            Fragment page = entry.children().get(2);
            assertEquals(titles[i], text(title));
            assertEquals(pages[i], text(page));
            assertEquals(Element.TOC_LEADER, leader.box().element());
            // The title and the number measure what they write; the leader fills the gap, with 4 pt on each side.
            double titleWidth = 5 * titles[i].length();
            assertEquals(0, title.x(), 1e-9);
            assertEquals(titleWidth, title.width(), 1e-9);
            assertEquals(titleWidth + 4, leader.x(), 1e-9);
            assertEquals(515 - 5 - titleWidth - 8, leader.width(), 1e-9);
            assertEquals(12, leader.height(), 1e-9);
            assertEquals(510, page.x(), 1e-9);
        }
    }

    @Test
    void theLevelsLimitTheEntries() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 1"));
        assertEquals(List.of("Intro", "Final"),
                entries(l).stream().map(e -> text(e.children().get(0))).toList());
    }

    @Test
    void withoutALeaderTheTitleFillsTheGapAndThePageStaysOnTheRight() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 1\n\t\tLeader: none"));
        Fragment entry = entries(l).get(0);
        assertEquals(2, entry.children().size());
        assertEquals(510, entry.children().get(0).width() + entry.children().get(0).x(), 1e-9);
        assertEquals(510, entry.children().get(1).x(), 1e-9);
    }

    @Test
    void aHeadingWithoutIdIsReferredWithAGeneratedName() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 2"));
        // The last one has an Id (last): its entry uses it; the others, a generated name.
        assertEquals("(heading 1)", l.fragment("toc").box().children().get(0).children().get(2).text()
                .replace("{page of: ", "").replace("}", ""));
        assertEquals("{page of: last}", l.fragment("toc").box().children().get(2).children().get(2).text());
    }

    @Test
    void entryRulesStyleEachLevel() {
        LayoutFixture l = layOutPaged(PAGE, document("\t\tLevels: 1 2",
                "\tStyles:", "\t\tFor element: TOC 2", "\t\t\tBox:", "\t\t\t\tPadding: 0 0 0 20",
                "\t\t\tTypography:", "\t\t\t\tSize: 10"));
        List<Fragment> entries = entries(l);
        // Only level 2 has an indent: its children, relative to its content box, are 20 pt
        // narrower and the number stays against the right edge.
        assertEquals(20, entries.get(1).padding().left(), 1e-9);
        assertEquals(0, entries.get(0).padding().left(), 1e-9);
        assertEquals(510, entries.get(0).children().get(2).x(), 1e-9);
        assertEquals(490, entries.get(1).children().get(2).x(), 1e-9);
    }

    @Test
    void theIndexMayComeAfterItsHeadings() {
        LayoutFixture l = layOutPaged(PAGE,
                "\tHeading 1:", "\t\tText: Uno",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 100",
                "\tTable of contents:", "\t\tId: toc");
        // The heading is on page 1; the table of contents, which does not fit, on page 2.
        assertTrue(l.has(2, "toc"));
        assertEquals("1", text(l.fragment(2, "toc").children().get(0).children().get(2)));
    }
}
