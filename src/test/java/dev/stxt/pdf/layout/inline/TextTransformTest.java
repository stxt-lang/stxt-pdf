package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.TextCase;
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

    private static List<TextRun> apply(List<TextRun> runs, TextCase t, boolean smallCaps, Locale l) {
        return TextTransform.apply(runs, t, smallCaps, l);
    }

    @Test
    void withoutTransformNorSmallCapsTheRunsAreTheSame() {
        List<TextRun> runs = List.of(run("Hola"), run("mundo", "b"));
        assertSame(runs, TextTransform.apply(runs, new StyleEditor().style()));
    }

    @Test
    void uppercaseAndLowercase() {
        assertEquals(List.of(run("ÓRDENES")), apply(List.of(run("Órdenes")), TextCase.UPPERCASE, false, ES));
        assertEquals(List.of(run("órdenes")), apply(List.of(run("ÓRDENES")), TextCase.LOWERCASE, false, ES));
    }

    @Test
    void theFormatOfEachRunIsKept() {
        assertEquals(List.of(run("UNO "), run("DOS", "b")),
                apply(List.of(run("uno "), run("dos", "b")), TextCase.UPPERCASE, false, ES));
    }

    @Test
    void theRulesAreThoseOfTheLanguage() {
        assertEquals(List.of(run("STRASSE")), apply(List.of(run("Straße")), TextCase.UPPERCASE, false, ES));
        assertEquals(List.of(run("İĞNE")),
                apply(List.of(run("iğne")), TextCase.UPPERCASE, false, Locale.forLanguageTag("tr")));
        assertEquals(List.of(run("IĞNE")), apply(List.of(run("iğne")), TextCase.UPPERCASE, false, Locale.ROOT));
    }

    @Test
    void capitalizeOnlyChangesTheFirstLetterOfEachWord() {
        assertEquals(List.of(run("Hola Mundo De IPhone")),
                apply(List.of(run("hola mundo de iPhone")), TextCase.CAPITALIZE, false, ES));
        assertEquals(List.of(run("Dos\nLíneas")), apply(List.of(run("dos\nlíneas")), TextCase.CAPITALIZE, false, ES));
        // A digit or a sign in front does not prevent the word from starting with a letter after a space.
        assertEquals(List.of(run("3 Ratones")), apply(List.of(run("3 ratones")), TextCase.CAPITALIZE, false, ES));
    }

    @Test
    void capitalizeContinuesAcrossRuns() {
        // "neg|rita" is a single word: only the first letter goes in uppercase.
        assertEquals(List.of(run("Una "), run("Neg", "b"), run("rita")),
                apply(List.of(run("una "), run("neg", "b"), run("rita")), TextCase.CAPITALIZE, false, ES));
        // After a space at the end of a run, the next one starts a word.
        assertEquals(List.of(run("Uno "), run("Dos", "b")),
                apply(List.of(run("uno "), run("dos", "b")), TextCase.CAPITALIZE, false, ES));
    }

    @Test
    void smallCapsTurnLowercaseLettersIntoMarkedCapitals() {
        // The space and the digits are not lowercase: they are not reduced.
        assertEquals(List.of(run("A"), new TextRun("BC", SMALL), run(" D1")),
                apply(List.of(run("Abc D1")), TextCase.NONE, true, ES));
    }

    @Test
    void smallCapsKeepTheRestOfTheFormatAndMergeEqualNeighbours() {
        TextFormat boldSmall = new TextFormat(true, false, false, false).withSmallCaps(true);
        assertEquals(List.of(new TextRun("HOLA", boldSmall)),
                apply(List.of(run("hola", "b")), TextCase.NONE, true, ES));
    }

    @Test
    void smallCapsWithTransform() {
        // lowercase leaves everything in lowercase, so everything is small caps; uppercase leaves none.
        assertEquals(List.of(new TextRun("ABC", SMALL)), apply(List.of(run("AbC")), TextCase.LOWERCASE, true, ES));
        assertEquals(List.of(run("ABC")), apply(List.of(run("abc")), TextCase.UPPERCASE, true, ES));
        // capitalize: the initial is a real capital and the rest are small caps.
        assertEquals(List.of(run("H"), new TextRun("OLA", SMALL), run(" M"), new TextRun("UNDO", SMALL)),
                apply(List.of(run("hola mundo")), TextCase.CAPITALIZE, true, ES));
    }

    @Test
    void letterWithoutCapitalIsNotMarked() {
        // "ª" is lowercase but has no distinct uppercase.
        assertEquals(List.of(run("1ª")), apply(List.of(run("1ª")), TextCase.NONE, true, ES));
    }

    @Test
    void pageMarkersAreLeftAlone() {
        assertEquals(List.of(run("PÁGINA {page} DE {pages}")),
                apply(List.of(run("Página {page} de {pages}")), TextCase.UPPERCASE, false, ES));
        assertEquals(List.of(run("P"), new TextRun("ÁGINA", SMALL), run(" {page}")),
                apply(List.of(run("Página {page}")), TextCase.NONE, true, ES));
    }
}
