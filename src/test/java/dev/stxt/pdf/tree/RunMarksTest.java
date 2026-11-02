package dev.stxt.pdf.tree;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The marks of the styled runs of a Markdown text before parsing it (reference/common/text-formatting.md). */
class RunMarksTest {

    /** The marked text with the marks written as {@code <n>} (start of the run with name n) and {@code </>}. */
    private static String marked(String text) {
        RunMarks.Marked m = RunMarks.mark(text, 1);
        StringBuilder out = new StringBuilder();
        String s = m.text();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == RunMarks.OPEN) {
                int end = s.indexOf(RunMarks.END, i);
                out.append('<').append(m.names().get(RunMarks.number(s, i + 1, end))).append('>');
                i = end;
            } else if (c == RunMarks.CLOSE) {
                out.append("</>");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    @Test
    void aStyledRunIsMarked() {
        assertEquals("Total: <price>12 €</>.", marked("Total: [12 €]{price}."));
        assertEquals("<a>x</> y <b>z</>", marked("[x]{a} y [z]{b}"));
    }

    @Test
    void linksAndMarksInsideOrAroundStayAsTheyAre() {
        assertEquals("[<price>precio</>](https://a.example)", marked("[[precio]{price}](https://a.example)"));
        assertEquals("<price>Ver [aquí](https://a.example)</>", marked("[Ver [aquí](https://a.example)]{price}"));
        assertEquals("**<a>b</>**", marked("**[b]{a}**"));
        assertEquals("<a>**b** c</>", marked("[**b** c]{a}"));
    }

    @Test
    void whatIsNotAStyledRunIsNotMarked() {
        String text = "\\[a\\]{x} `[b]{x}` ![c]{x} [d](e){x} [f] {x} [g]{h i} [j]{} [k]{page of: l} [^1] [m]{page} "
                + "[n]{pages}";
        RunMarks.Marked m = RunMarks.mark(text, 1);
        assertSame(text, m.text());
        assertEquals(List.of(), m.names());
    }

    @Test
    void theBracketsOfALinkTargetAreNotStyledRuns() {
        assertEquals("[a](https://a.example/[b]{x}) <y>c</>", marked("[a](https://a.example/[b]{x}) [c]{y}"));
    }

    @Test
    void aBlankLineSeparatesTheBrackets() {
        assertEquals("[a\n\nb]{x}", marked("[a\n\nb]{x}"));
        assertEquals("<x>a\nb</>", marked("[a\nb]{x}"));
    }

    @Test
    void longCodeIsSkippedWithItsBackticks() {
        assertEquals("``[a]{x} ` y`` <z>b</>", marked("``[a]{x} ` y`` [b]{z}"));
        // Backticks that nothing closes are text.
        assertEquals("` <z>b</>", marked("` [b]{z}"));
    }

    @Test
    void unmarkWritesTheRunsAgain() {
        String text = "Uno [dos [tres]{a}]{b} y [cuatro]{c}";
        RunMarks.Marked m = RunMarks.mark(text, 1);
        assertEquals(text, RunMarks.unmark(m.text(), m.names()));
    }

    @Test
    void aTextWithANoncharacterIsAnError() {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> RunMarks.mark("a\uFDD0b", 7));
        assertEquals(ErrorCode.MARKDOWN_UNSUPPORTED, e.errorCode());
        assertEquals(7, e.line());
    }
}
