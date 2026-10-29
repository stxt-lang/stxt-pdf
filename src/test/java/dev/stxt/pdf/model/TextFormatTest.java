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
        // Negrita dentro de negrita sigue siendo negrita.
        assertEquals(BOLD, new TextFormat(true, false, false, false).face(BOLD));
    }

    @Test
    void aFamilyReplacesTheOneOfTheBoxAndKeepsTheRest() {
        TextFormat code = TextFormat.PLAIN.withFamily("M");
        assertEquals(new FontFace("M", FontWeight.NORMAL, FontStyle.NORMAL), code.face(REGULAR));
        assertEquals(new FontFace("M", FontWeight.BOLD, FontStyle.NORMAL), code.face(BOLD));
        assertEquals(new FontFace("M", FontWeight.BOLD, FontStyle.ITALIC),
                new TextFormat(true, true, false, false).withFamily("M").face(REGULAR));
        // Los demás cambios del formato la conservan.
        assertEquals("M", code.withLink("https://example.com/a").withSmallCaps(true).family());
        assertEquals("https://example.com/a", code.withLink("https://example.com/a").link());
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
        // Posiciones de la parte de arriba de la raya, hacia arriba desde la línea base.
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
        // Dos enlaces distintos no se unen en un tramo, aunque lo demás sea igual.
        assertNotEquals(link, plain.withLink("https://otro.example"));
        assertNotEquals(link, plain);
    }
}
