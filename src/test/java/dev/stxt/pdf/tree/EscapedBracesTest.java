package dev.stxt.pdf.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.model.Reference;

/**
 * The escaped braces of a Markdown text (reference/common/references.md): {@link RunMarks} marks them before the
 * parser gives back a brace like any other, so that <code>\{page}</code> is text.
 */
class EscapedBracesTest {

    private static final String B = String.valueOf(Reference.LITERAL_BRACE);

    private static String marked(String text) {
        return RunMarks.mark(text, 1).text();
    }

    @Test
    void anEscapedOpeningBraceIsMarked() {
        assertEquals("a " + B + "page} b", marked("a \\{page} b"));
        assertEquals(B + "page\\} y " + B, marked("\\{page\\} y \\{"));
        // The closing one alone starts nothing, and another escape is left to the parser.
        assertSame("{page\\} \\* \\[x\\]", marked("{page\\} \\* \\[x\\]"));
    }

    @Test
    void whatIsNotMarkdownTextKeepsItsBackslash() {
        // Inline code, the target of a link and a backslash that is escaped itself.
        assertSame("`\\{page}`", marked("`\\{page}`"));
        assertSame("[x](a\\{b)", marked("[x](a\\{b)"));
        assertSame("\\\\{page}", marked("\\\\{page}"));
    }

    @Test
    void itLivesWithTheStyledRuns() {
        RunMarks.Marked m = RunMarks.mark("[12 \\{x}]{price} y [z]\\{nada}", 1);
        assertEquals(List.of("price"), m.names());
        assertEquals("12 " + B + "x}", m.text().substring(m.text().indexOf(RunMarks.END) + 2,
                m.text().indexOf(RunMarks.CLOSE) - 1));
        assertEquals(" y [z]" + B + "nada}", m.text().substring(m.text().indexOf(RunMarks.CLOSE) + 1));
    }

    @Test
    void aCodeBlockGetsItsBackslashBack() {
        RunMarks.Marked m = RunMarks.mark("\\{page} [x]{a}", 1);
        assertEquals("\\{page} [x]{a}", RunMarks.unmark(m.text(), m.names()));
        assertEquals("\\{page}", RunMarks.unmark(marked("\\{page}"), List.of()));
        assertSame("nada", RunMarks.unmark("nada", List.of()));
    }

    @Test
    void aTextThatAlreadyHasTheMarkIsNotMarkdown() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> RunMarks.mark("a" + B + "b", 7));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, e.errorCode());
        assertEquals(7, e.line());
    }
}
