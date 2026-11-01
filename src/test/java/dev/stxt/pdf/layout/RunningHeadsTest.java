package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.TextLine;
import org.junit.jupiter.api.Test;

import static dev.stxt.pdf.layout.LayoutFixture.layOutPaged;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Running heads, {@code {heading N}} and {@code {heading N: first}} in a {@code Page overlay}
 * (reference/common/references.md). Pages of 180 pt with margins of 40: page area of 515 × 100 from (40, 40); with
 * {@code FixedMetrics} and {@code Size: 10}, each character measures 5 pt and each line, 12. The headings have no
 * style of their own, so they are one line of 12.
 */
class RunningHeadsTest {

    private static final double PAGE = 180;

    /** A header in the top margin, anchored to the left and as wide as its text, with that text. */
    private static String[] header(String text) {
        return new String[]{"\tPage overlay:", "\t\tId: head", "\t\tOffset: -15 auto auto 0", "\t\tText: " + text,
                "\t\t\tId: head-text"};
    }

    /** A footer in the bottom margin, anchored to the right and as wide as its text. */
    private static String[] footer(String text) {
        return new String[]{"\tPage overlay:", "\t\tId: foot", "\t\tOffset: auto 0 -15 auto", "\t\tText: " + text,
                "\t\t\tId: foot-text"};
    }

    private static String[] heading(int level, String text) {
        return new String[]{"\tHeading " + level + ":", "\t\tText: " + text};
    }

    private static String[] block(double height) {
        return new String[]{"\tBlock:", "\t\tBox:", "\t\t\tHeight: " + height};
    }

    private static String[] concat(String[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    private static String text(LayoutFixture l, int page, String id) {
        return l.fragment(page, id).lines().stream().map(TextLine::text).reduce((a, b) -> a + "|" + b).orElse("");
    }

    @Test
    void theHeadingInEffectOnEachPageIsTheLastOneThatHasStarted() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                header("{heading 1}"), footer("{heading 1}"),
                block(100),
                heading(1, "Uno"), block(88),
                heading(1, "Tres"), block(188)));
        assertEquals(4, l.pages());
        // Before the first heading there is nothing to write: the text is empty and takes up no line.
        assertEquals("", text(l, 1, "head-text"));
        assertEquals(0, l.fragment(1, "head-text").lines().size());
        assertEquals("Uno", text(l, 2, "head-text"));
        assertEquals("Tres", text(l, 3, "head-text"));
        // A page without a heading keeps the one of the previous pages.
        assertEquals("Tres", text(l, 4, "head-text"));
        // Each page is laid out with its own text: the box is as wide as it, and the footer moves with it.
        assertEquals(15, l.rect(2, "head").width(), 1e-9);
        assertEquals(20, l.rect(3, "head").width(), 1e-9);
        assertEquals(40, l.rect(2, "head").x(), 1e-9);
        assertEquals(555 - 15, l.rect(2, "foot").x(), 1e-9);
        assertEquals(555 - 20, l.rect(3, "foot").x(), 1e-9);
        assertEquals(155 - 12, l.rect(3, "foot").y(), 1e-9);
    }

    @Test
    void firstGivesTheFirstHeadingThatStartsOnThePage() {
        // Page 1: Uno (12), 20, Dos (12), 44 and 12 of the last block; page 2: its other 100.
        LayoutFixture l = layOutPaged(PAGE, concat(
                header("{heading 1: first} - {heading 1}"),
                heading(1, "Uno"), block(20),
                heading(1, "Dos"), block(44),
                block(112)));
        assertEquals(2, l.pages());
        assertEquals("Uno - Dos", text(l, 1, "head-text"));
        // Where no heading of the level starts, first is the heading in effect.
        assertEquals("Dos - Dos", text(l, 2, "head-text"));
    }

    @Test
    void aNumberedHeadingIsWrittenWithItsNumber() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                header("{heading 1}"),
                new String[]{"\tHeading 1:", "\t\tTypography:", "\t\t\tNumbered: true", "\t\tText: Uno"},
                block(88),
                new String[]{"\tHeading 1:", "\t\tTypography:", "\t\t\tNumbered: true", "\t\tText: Dos"}));
        assertEquals("1 Uno", text(l, 1, "head-text"));
        assertEquals("2 Dos", text(l, 2, "head-text"));
    }

    @Test
    void aHigherLevelHeadingLeavesTheLevelsBelowWithoutAHeading() {
        // Page 1: A (12), A.1 (12) and 76; page 2: B (12) and 88; page 3: B.1 and the rest.
        LayoutFixture l = layOutPaged(PAGE, concat(
                header("{heading 2}"), footer("{heading 1}"),
                heading(1, "A"), heading(2, "A.1"), block(76),
                heading(1, "B"), block(88),
                heading(2, "B.1"), block(10)));
        assertEquals(3, l.pages());
        assertEquals("A.1", text(l, 1, "head-text"));
        // The new chapter has no section yet: the running head of level 2 is empty, not the one of chapter A.
        assertEquals("", text(l, 2, "head-text"));
        assertEquals("B", text(l, 2, "foot-text"));
        assertEquals("B.1", text(l, 3, "head-text"));
    }

    @Test
    void theRunningHeadIsTransformedWithCaseAndMixesWithPageNumbers() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tPage overlay:", "\t\tId: head", "\t\tOffset: -15 auto auto 0",
                        "\t\tText: {heading 1} · {page}", "\t\t\tId: head-text", "\t\t\tTypography:",
                        "\t\t\t\tCase: uppercase"},
                heading(1, "Uno"), block(88),
                heading(1, "Dos"), block(10)));
        assertEquals("UNO · 1", text(l, 1, "head-text"));
        assertEquals("DOS · 2", text(l, 2, "head-text"));
    }

    @Test
    void aRunningHeadInsideAStackOfTheOverlayIsLaidOutPerPage() {
        LayoutFixture l = layOutPaged(PAGE, concat(
                new String[]{"\tPage overlay:", "\t\tId: head", "\t\tOffset: -15 0 auto 0", "\t\tStack:",
                        "\t\t\tHorizontal: justify", "\t\t\tItem:", "\t\t\t\tText: Manual", "\t\t\tItem:",
                        "\t\t\t\tText: {heading 1}", "\t\t\t\t\tId: head-text"},
                heading(1, "Uno"), block(88),
                heading(1, "Dos"), block(10)));
        assertEquals("Uno", text(l, 1, "head-text"));
        assertEquals("Dos", text(l, 2, "head-text"));
        assertTrue(l.rect(2, "head-text").x() > l.rect(1, "head-text").x() - 1e-9);
        assertEquals(555 - 15, l.rect(1, "head-text").x(), 1e-9);
    }
}
