package dev.stxt.pdf.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** How the number of a footnote is written (reference/components/footnote.md). */
class FootnoteNumberFormatTest {

    /**
     * Every format of {@link NumberFormat} is one of a footnote, with the same name, which is how the footnote finds
     * it, and writes the same; a footnote has one more, the symbols.
     */
    @Test
    void theFormatsOfANumberAreThoseOfAFootnoteAndWriteTheSame() {
        Set<String> names = new HashSet<>();
        for (NumberFormat format : NumberFormat.values()) {
            names.add(format.name());
            FootnoteNumberFormat footnote = FootnoteNumberFormat.valueOf(format.name());
            for (int n : new int[]{1, 4, 9, 27, 49}) {
                assertEquals(format.format(n), footnote.format(n), format + " " + n);
            }
        }
        names.add("SYMBOLS");
        Set<String> footnotes = new HashSet<>();
        Arrays.stream(FootnoteNumberFormat.values()).forEach(f -> footnotes.add(f.name()));
        assertEquals(names, footnotes);
    }

    /** The four symbols in order, and then doubled, tripled and so on. */
    @Test
    void theSymbolsGoRoundAndAreRepeated() {
        List<String> marks = List.of("*", "†", "‡", "§", "**", "††", "‡‡", "§§", "***");
        for (int n = 1; n <= marks.size(); n++) {
            assertEquals(marks.get(n - 1), FootnoteNumberFormat.SYMBOLS.format(n));
        }
    }
}
