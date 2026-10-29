package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.Transform;
import dev.stxt.pdf.tree.StyleEditor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TextTransformTest {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final TextFormat SMALL = TextFormat.PLAIN.withSmallCaps(true);

    private static List<TextRun> apply(List<TextRun> runs, Transform t, boolean smallCaps, Locale l) {
        return TextTransform.apply(runs, t, smallCaps, l);
    }

    @Test
    void withoutTransformNorSmallCapsTheRunsAreTheSame() {
        List<TextRun> runs = List.of(run("Hola"), run("mundo", "b"));
        assertSame(runs, TextTransform.apply(runs, new StyleEditor().style()));
    }

    @Test
    void uppercaseAndLowercase() {
        assertEquals(List.of(run("ÓRDENES")), apply(List.of(run("Órdenes")), Transform.UPPERCASE, false, ES));
        assertEquals(List.of(run("órdenes")), apply(List.of(run("ÓRDENES")), Transform.LOWERCASE, false, ES));
    }

    @Test
    void theFormatOfEachRunIsKept() {
        assertEquals(List.of(run("UNO "), run("DOS", "b")),
                apply(List.of(run("uno "), run("dos", "b")), Transform.UPPERCASE, false, ES));
    }

    @Test
    void theRulesAreThoseOfTheLanguage() {
        assertEquals(List.of(run("STRASSE")), apply(List.of(run("Straße")), Transform.UPPERCASE, false, ES));
        assertEquals(List.of(run("İĞNE")),
                apply(List.of(run("iğne")), Transform.UPPERCASE, false, Locale.forLanguageTag("tr")));
        assertEquals(List.of(run("IĞNE")), apply(List.of(run("iğne")), Transform.UPPERCASE, false, Locale.ROOT));
    }

    @Test
    void capitalizeOnlyChangesTheFirstLetterOfEachWord() {
        assertEquals(List.of(run("Hola Mundo De IPhone")),
                apply(List.of(run("hola mundo de iPhone")), Transform.CAPITALIZE, false, ES));
        assertEquals(List.of(run("Dos\nLíneas")), apply(List.of(run("dos\nlíneas")), Transform.CAPITALIZE, false, ES));
        // Una cifra o un signo delante no impiden que la palabra empiece por una letra después de un espacio.
        assertEquals(List.of(run("3 Ratones")), apply(List.of(run("3 ratones")), Transform.CAPITALIZE, false, ES));
    }

    @Test
    void capitalizeContinuesAcrossRuns() {
        // «neg|rita» es una sola palabra: solo la primera letra va en mayúscula.
        assertEquals(List.of(run("Una "), run("Neg", "b"), run("rita")),
                apply(List.of(run("una "), run("neg", "b"), run("rita")), Transform.CAPITALIZE, false, ES));
        // Tras un espacio al final de un tramo, el siguiente empieza palabra.
        assertEquals(List.of(run("Uno "), run("Dos", "b")),
                apply(List.of(run("uno "), run("dos", "b")), Transform.CAPITALIZE, false, ES));
    }

    @Test
    void smallCapsTurnLowercaseLettersIntoMarkedCapitals() {
        // El espacio y las cifras no son minúsculas: no se reducen.
        assertEquals(List.of(run("A"), new TextRun("BC", SMALL), run(" D1")),
                apply(List.of(run("Abc D1")), Transform.NONE, true, ES));
    }

    @Test
    void smallCapsKeepTheRestOfTheFormatAndMergeEqualNeighbours() {
        TextFormat boldSmall = new TextFormat(true, false, false, false).withSmallCaps(true);
        assertEquals(List.of(new TextRun("HOLA", boldSmall)),
                apply(List.of(run("hola", "b")), Transform.NONE, true, ES));
    }

    @Test
    void smallCapsWithTransform() {
        // lowercase deja todo en minúsculas, así que todo son versalitas; uppercase no deja ninguna.
        assertEquals(List.of(new TextRun("ABC", SMALL)), apply(List.of(run("AbC")), Transform.LOWERCASE, true, ES));
        assertEquals(List.of(run("ABC")), apply(List.of(run("abc")), Transform.UPPERCASE, true, ES));
        // capitalize: la inicial es una mayúscula de verdad y el resto son versalitas.
        assertEquals(List.of(run("H"), new TextRun("OLA", SMALL), run(" M"), new TextRun("UNDO", SMALL)),
                apply(List.of(run("hola mundo")), Transform.CAPITALIZE, true, ES));
    }

    @Test
    void letterWithoutCapitalIsNotMarked() {
        // «ª» es minúscula pero no tiene mayúscula distinta.
        assertEquals(List.of(run("1ª")), apply(List.of(run("1ª")), Transform.NONE, true, ES));
    }

    @Test
    void pageMarkersAreLeftAlone() {
        assertEquals(List.of(run("PÁGINA {page} DE {pages}")),
                apply(List.of(run("Página {page} de {pages}")), Transform.UPPERCASE, false, ES));
        assertEquals(List.of(run("P"), new TextRun("ÁGINA", SMALL), run(" {page}")),
                apply(List.of(run("Página {page}")), Transform.NONE, true, ES));
    }
}
