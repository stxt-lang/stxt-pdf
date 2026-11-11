package dev.stxt.pdf.layout;

import static dev.stxt.pdf.layout.LayoutFixture.layOut;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.layout.inline.FixedMetrics;
import dev.stxt.pdf.layout.inline.TextMeasurer;

/**
 * The baseline of the first line of text of a fragment ({@link Baselines}). With the test metrics the baseline of a
 * line is 80 % of the size below the top of the text, which is centered in the line: 9 in a text of size 10 in lines
 * of 12.
 */
class BaselinesTest {

    private static final TextMeasurer METRICS = new FixedMetrics();

    private static final String[] TYPOGRAPHY = {"		Typography:", "			Line height: 12"};

    /** The first line of a text, from the top of its border box: below its border and its padding. */
    @Test
    void theBaselineOfATextIsThatOfItsFirstLine() {
        LayoutFixture l = layOut("\tText: Una línea", "\t\tId: T", TYPOGRAPHY[0], TYPOGRAPHY[1],
                "\tText: Otra", "\t\tId: P", TYPOGRAPHY[0], TYPOGRAPHY[1], "\t\tBox:", "\t\t\tPadding: 5",
                "\t\t\tBorder: 2");
        assertEquals(9, Baselines.first(l.fragment("T"), METRICS).getAsDouble(), 1e-9);
        assertEquals(2 + 5 + 9, Baselines.first(l.fragment("P"), METRICS).getAsDouble(), 1e-9);
    }

    /**
     * Without lines of its own, the baseline is that of its first child in flow, going down: a block with a padding
     * of 4 whose first child is a block with a margin of 3 and the text.
     */
    @Test
    void theBaselineOfABoxIsThatOfItsFirstTextGoingDown() {
        LayoutFixture l = layOut("\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tPadding: 4",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tMargin: 3 0 0 0", "\t\t\t\tBorder: 1",
                "\t\t\tText: Dentro", "\t\t\t\tTypography:", "\t\t\t\t\tLine height: 12",
                "\t\tText: Después");
        assertEquals(4 + 3 + 1 + 9, Baselines.first(l.fragment("B"), METRICS).getAsDouble(), 1e-9);
        // The same, from the children of the box and relative to its content box.
        assertEquals(3 + 1 + 9, Baselines.first(l.fragment("B").children(), METRICS).getAsDouble(), 1e-9);
    }

    /** A box whose first thing is not a text has no baseline: an empty box before the text, or nothing at all. */
    @Test
    void aBoxThatDoesNotStartWithATextHasNoBaseline() {
        LayoutFixture l = layOut("\tBlock:", "\t\tId: B", "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 10",
                "\t\tText: Después", "\tBlock:", "\t\tId: E", "\t\tBox:", "\t\t\tHeight: 10");
        assertTrue(Baselines.first(l.fragment("B"), METRICS).isEmpty());
        assertTrue(Baselines.first(l.fragment("E"), METRICS).isEmpty());
        assertTrue(Baselines.first(List.of(), METRICS).isEmpty());
    }
}
