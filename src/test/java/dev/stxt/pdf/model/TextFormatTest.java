package dev.stxt.pdf.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFormatTest {

    private static final FontFace REGULAR = new FontFace("F", FontWeight.NORMAL, FontStyle.NORMAL);
    private static final FontFace BOLD = new FontFace("F", FontWeight.BOLD, FontStyle.NORMAL);

    @Test
    void boldAndItalicAddToTheFontOfTheBox() {
        assertSame(REGULAR, TextFormat.PLAIN.face(REGULAR));
        assertEquals(BOLD, new TextFormat(true, false, false, false).face(REGULAR));
        assertEquals(new FontFace("F", FontWeight.BOLD, FontStyle.ITALIC),
                new TextFormat(false, true, true, false).face(BOLD));
        // Bold inside bold is still bold.
        assertEquals(BOLD, new TextFormat(true, false, false, false).face(BOLD));
    }

    /** A bold run has weight 700, or that of the box if it is already 700 or more; italic keeps the weight. */
    @Test
    void aBoldRunIsWeight700UnlessTheBoxIsHeavier() {
        TextFormat bold = new TextFormat(true, false, false, false);
        assertEquals(BOLD, bold.face(new FontFace("F", new FontWeight(300), FontStyle.NORMAL)));
        assertEquals(BOLD, bold.face(new FontFace("F", new FontWeight(600), FontStyle.NORMAL)));
        FontFace black = new FontFace("F", new FontWeight(900), FontStyle.NORMAL);
        assertEquals(black, bold.face(black));
        FontFace light = new FontFace("F", new FontWeight(300), FontStyle.NORMAL);
        assertEquals(new FontFace("F", new FontWeight(300), FontStyle.ITALIC),
                new TextFormat(false, true, false, false).face(light));
    }

    @Test
    void variantNamesTheWeightAndTheStyle() {
        assertEquals("normal", REGULAR.variant());
        assertEquals("bold", BOLD.variant());
        assertEquals("italic", new FontFace("F", FontWeight.NORMAL, FontStyle.ITALIC).variant());
        assertEquals("bold italic", new FontFace("F", FontWeight.BOLD, FontStyle.ITALIC).variant());
        assertEquals("weight 300", new FontFace("F", new FontWeight(300), FontStyle.NORMAL).variant());
        assertEquals("weight 600 italic", new FontFace("F", new FontWeight(600), FontStyle.ITALIC).variant());
    }

    @Test
    void aFamilyReplacesTheOneOfTheBoxAndKeepsTheRest() {
        TextFormat code = TextFormat.PLAIN.withFamily("M");
        assertEquals(new FontFace("M", FontWeight.NORMAL, FontStyle.NORMAL), code.face(REGULAR));
        assertEquals(new FontFace("M", FontWeight.BOLD, FontStyle.NORMAL), code.face(BOLD));
        assertEquals(new FontFace("M", FontWeight.BOLD, FontStyle.ITALIC),
                new TextFormat(true, true, false, false).withFamily("M").face(REGULAR));
        // The other changes of the format keep it.
        assertEquals("M", code.withLink("https://example.com/a").withSmallCaps(true).family());
        assertEquals("https://example.com/a", code.withLink("https://example.com/a").link());
    }

    @Test
    void codeIsKeptByTheOtherChanges() {
        TextFormat code = TextFormat.PLAIN.asCode();
        assertTrue(code.withLink("https://example.com/a").withSmallCaps(true).withFamily("M").code());
        assertFalse(TextFormat.PLAIN.withFamily("M").code());
        // Without its family yet, it is painted with the one of the box.
        assertEquals(REGULAR, code.face(REGULAR));
    }

    @Test
    void decorations() {
        assertFalse(new TextFormat(true, true, false, false).decorated());
        assertTrue(new TextFormat(false, false, true, false).decorated());
        assertTrue(new TextFormat(false, false, false, true).decorated());
    }

    @Test
    void runsJoinTheirText() {
        assertEquals("uno dos", TextRun.text(List.of(new TextRun("uno ", TextFormat.PLAIN),
                new TextRun("dos", new TextFormat(true, false, false, false)))));
        assertEquals(List.of(new TextRun("uno", TextFormat.PLAIN)), TextRun.plain("uno"));
    }

    @Test
    void decorationRectsFromTheBaseline() {
        // Positions of the top of the line, upwards from the baseline.
        DecorationMetrics d = new DecorationMetrics(-0.1, 0.05, 0.3, 0.04);
        assertEquals(new Rect(5, 101, 20, 0.5), d.underline(5, 100, 20, 10));
        assertEquals(new Rect(5, 97, 20, 0.4), d.strikeout(5, 100, 20, 10));
    }

    @Test
    void aLinkIsPartOfTheFormatButDoesNotChangeTheFont() {
        TextFormat plain = new TextFormat(false, false, false, false);
        TextFormat link = plain.withLink("https://stxt.dev");
        assertEquals("https://stxt.dev", link.link());
        assertNull(plain.link());
        assertEquals(REGULAR, link.face(REGULAR));
        assertFalse(link.decorated());
        // Two different links are not merged into one run, even if everything else is equal.
        assertNotEquals(link, plain.withLink("https://otro.example"));
        assertNotEquals(link, plain);
    }

    /** The call and the label of a footnote are superscripts: smaller and raised, in the font of the text around them. */
    @Test
    void footnoteCallsAndLabelsAreSuperscripts() {
        TextFormat call = new TextFormat(true, false, false, false).withFootnote("n1");
        assertEquals("n1", call.footnote());
        assertFalse(call.label());
        assertTrue(call.superscript());
        assertEquals(TextFormat.SUPERSCRIPT_SCALE, call.scale());
        assertEquals(TextFormat.SUPERSCRIPT_RISE, call.rise());
        // The bold of the text reaches the call.
        assertEquals(BOLD, call.face(REGULAR));
        TextFormat label = TextFormat.PLAIN.asLabel();
        assertTrue(label.label());
        assertNull(label.footnote());
        assertTrue(label.superscript());
        assertEquals(TextFormat.SUPERSCRIPT_SCALE, label.scale());
        // Neither small caps nor the link change a superscript, and the plain text is not raised.
        assertEquals(TextFormat.SUPERSCRIPT_SCALE, call.withSmallCaps(true).scale());
        assertEquals("n1", call.withLink("https://a.example").footnote());
        assertEquals(0, TextFormat.PLAIN.rise());
        assertFalse(TextFormat.PLAIN.superscript());
        assertNotEquals(call, label);
    }
}
