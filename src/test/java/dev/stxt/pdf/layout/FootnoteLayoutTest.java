package dev.stxt.pdf.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.model.Rect;

/**
 * The footnotes on the page (reference/components/footnote.md): pages of 200 pt, so the page area measures 515 × 120
 * from (40, 40) and ends at y = 160. Lines of 12 ({@code Size: 10}), 103 characters per line. The footnote area has, by
 * default, a margin of 8, a rule of 0.5 and a padding of 4 above its notes: its first note starts 4.5 below its top.
 */
class FootnoteLayoutTest {

    private static final String MARKDOWN = "\t\t\t\tMarkup: markdown";

    /** A text of {@code lines} lines of nine-letter words (ten per line, with the call in the first one). */
    private static String[] text(String id, int lines, String call) {
        String words = IntStream.range(0, lines * 10).mapToObj(i -> i == 0 ? "aaaaaaaaa" + call : "aaaaaaaaa")
                .collect(Collectors.joining(" "));
        return new String[] {"\tParagraph:", "\t\tId: " + id, "\t\tContent >>", "\t\t\t" + words};
    }

    private static String[] note(String id, int lines) {
        String words = IntStream.range(0, lines * 10 - 1).mapToObj(i -> "nnnnnnnnn").collect(Collectors.joining(" "));
        return new String[] {"\tFootnote:", "\t\tId: " + id, "\t\tParagraph:", "\t\t\tContent >>", "\t\t\t\t" + words};
    }

    private static String[] join(String[]... parts) {
        return java.util.Arrays.stream(parts).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    @Test
    void theNotesGoAtTheBottomOfThePageUnderARule() {
        LayoutFixture f = LayoutFixture.layOutPaged(200, join(new String[] {MARKDOWN},
                text("p", 1, "[^n]"), note("n", 1)));
        assertEquals(1, f.pages());
        assertEquals(new Rect(40, 40, 515, 12), f.rect("p"));
        // The area measures 0.5 + 4 + 12 = 16.5 and ends at the bottom of the page area: the note starts at 148.
        assertEquals(new Rect(40, 148, 515, 12), f.rect("n"));
    }

    /**
     * The notes are stacked like the children of a block: the top margin of the first child of a note comes out of
     * it and separates it from what is above, as in the flow. Until the 2026-11-04 it was lost and the notes touched.
     */
    @Test
    void theMarginsThatComeOutOfANoteSeparateItAsInTheFlow() {
        LayoutFixture f = LayoutFixture.layOutPaged(200, MARKDOWN,
                "\tParagraph: aaaa[^a] bbbb[^b]", "\t\tId: p",
                "\tFootnote:", "\t\tId: a", "\t\tParagraph: nnnnnnnnn", "\t\t\tBox:", "\t\t\t\tMargin: 10 0 0 0",
                "\tFootnote:", "\t\tId: b", "\t\tParagraph: nnnnnnnnn", "\t\t\tBox:", "\t\t\t\tMargin: 10 0 0 0");
        assertEquals(1, f.pages());
        // The area measures 0.5 + 4 + (10 + 12) + (10 + 12) = 48.5 and starts at 111.5: its content, at 116.
        assertEquals(new Rect(40, 126, 515, 12), f.rect("a"));
        assertEquals(new Rect(40, 148, 515, 12), f.rect("b"));
    }

    /**
     * The side margins of the area narrow it, like those of a block: its notes are as wide as what they leave. Until
     * the 2026-11-13 the left one moved the area without narrowing it and the right one did nothing.
     */
    @Test
    void theSideMarginsOfTheAreaNarrowIt() {
        LayoutFixture f = LayoutFixture.layOutPaged(200, MARKDOWN, "\t\tFor element: Footnote area", "\t\t\tBox:",
                "\t\t\t\tMargin: 8 30 0 20",
                "\tParagraph: aaaa[^n]", "\t\tId: p", "\tFootnote:", "\t\tId: n", "\t\tParagraph: nnnnnnnnn");
        assertEquals(1, f.pages());
        assertEquals(new Rect(60, 148, 465, 12), f.rect("n"));
    }

    /** A line enters the page only if its note fits under it; otherwise it moves to the next page with the note. */
    @Test
    void aLineMovesToTheNextPageWithItsNote() {
        LayoutFixture f = LayoutFixture.layOutPaged(200, join(new String[] {MARKDOWN,
                "\tBlock:", "\t\tId: filler", "\t\tBox:", "\t\t\tHeight: 100", "\t\tBreak:", "\t\t\tKeep together: true"},
                text("p", 1, "[^n]"), note("n", 1)));
        assertEquals(2, f.pages());
        // Together they would take 112, but the note needs 24.5 more: the paragraph goes with it to page 2.
        assertEquals(new Rect(40, 40, 515, 100), f.rect(1, "filler"));
        f.assertAbsent(1, "p");
        f.assertAbsent(1, "n");
        assertEquals(new Rect(40, 40, 515, 12), f.rect(2, "p"));
        assertEquals(new Rect(40, 148, 515, 12), f.rect(2, "n"));
    }

    /**
     * Two lines with a call each: the first note fits under them, but not both (24.5 + 12 more than the 120 of the
     * page area with 60 + 24 of flow), so the second line moves to the next page with its note.
     */
    @Test
    void aSecondNoteThatDoesNotFitTakesItsLineToTheNextPage() {
        LayoutFixture f = LayoutFixture.layOutPaged(200, join(new String[] {MARKDOWN,
                "\tBlock:", "\t\tId: filler", "\t\tBox:", "\t\t\tHeight: 60"},
                text("p1", 1, "[^a]"), note("a", 1), text("p2", 1, "[^b]"), note("b", 1)));
        assertEquals(2, f.pages());
        assertEquals(new Rect(40, 100, 515, 12), f.rect(1, "p1"));
        assertEquals(new Rect(40, 148, 515, 12), f.rect(1, "a"));
        f.assertAbsent(1, "p2");
        f.assertAbsent(1, "b");
        assertEquals(new Rect(40, 40, 515, 12), f.rect(2, "p2"));
        assertEquals(new Rect(40, 148, 515, 12), f.rect(2, "b"));
    }

    /**
     * A note that does not fit under its line continues on the next page, before the notes of that page, respecting
     * widows and orphans; the line stays with the start of its note.
     */
    @Test
    void aLongNoteIsSplitAcrossPages() {
        LayoutFixture f = LayoutFixture.layOutPaged(200, join(new String[] {MARKDOWN}, text("q", 6, "[^n]"),
                note("n", 12)));
        assertEquals(3, f.pages());
        // Page 1: the first line of q and 7 lines of the note (the most that leave a line to the text: 120 − 12 − 12.5).
        assertEquals(new Rect(40, 40, 515, 12), f.rect(1, "q"));
        assertEquals(new Rect(40, 160 - 84, 515, 84), f.rect(1, "n"));
        // Page 2: the end of the note (5 lines) takes 72.5 and q gets 47.5: 3 lines, leaving 2 for page 3.
        assertEquals(new Rect(40, 40, 515, 36), f.rect(2, "q"));
        assertEquals(new Rect(40, 160 - 60, 515, 60), f.rect(2, "n"));
        assertEquals(new Rect(40, 40, 515, 24), f.rect(3, "q"));
        f.assertAbsent(3, "n");
    }

    /** Inside a {@code Columns}, the notes go at the bottom of the page, across its whole width. */
    @Test
    void inColumnsTheNotesGoAtTheFootOfThePage() {
        LayoutFixture f = LayoutFixture.layOutPaged(200, join(new String[] {MARKDOWN,
                "\tColumns:", "\t\tId: cols", "\t\tColumn count: 2", "\t\tGap: 15",
                "\t\tParagraph: Uno[^a].", "\t\t\tId: c1", "\t\tParagraph: Dos[^b].", "\t\t\tId: c2"},
                note("a", 1), note("b", 1)));
        assertEquals(1, f.pages());
        assertEquals(new Rect(40, 40, 250, 12), f.rect("c1"));
        assertEquals(new Rect(305, 40, 250, 12), f.rect("c2"));
        // Two notes of 12: the area measures 28.5 and its notes span the page area.
        assertEquals(new Rect(40, 136, 515, 12), f.rect("a"));
        assertEquals(new Rect(40, 148, 515, 12), f.rect("b"));
        assertTrue(f.rect("a").y() > f.rect("cols").y() + f.rect("cols").height());
    }

    /**
     * A note called several times goes once, on the page of its first call: a second call on the same page does not
     * repeat it, and one on a later page brings nothing to the bottom of that page.
     */
    @Test
    void aNoteCalledSeveralTimesGoesOnThePageOfItsFirstCall() {
        LayoutFixture f = LayoutFixture.layOutPaged(200, join(new String[] {MARKDOWN,
                "\tParagraph: aaaa[^n] bbbb[^n]", "\t\tId: p",
                "\tBlock:", "\t\tId: filler", "\t\tBox:", "\t\t\tHeight: 80"},
                text("q", 3, "[^n]"), note("n", 1)));
        assertEquals(2, f.pages());
        assertEquals(new Rect(40, 40, 515, 12), f.rect(1, "p"));
        // One note, 12 tall, under the two calls of the first page.
        assertEquals(new Rect(40, 148, 515, 12), f.rect(1, "n"));
        // The third call is on page 2, whose text takes the whole page area: the note is not there.
        assertEquals(new Rect(40, 40, 515, 36), f.rect(2, "q"));
        f.assertAbsent(2, "n");
    }
}
