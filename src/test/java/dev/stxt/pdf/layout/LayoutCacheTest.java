package dev.stxt.pdf.layout;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.layout.inline.FixedMetrics;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the architecture rule (reference/architecture.md): every recursion of layout and intrinsic sizes
 * goes through {@code engine.layout}/{@code engine.intrinsic}, which use the {@link LayoutCache}. Without
 * it, each level of stack or grid lays out its children several times (measurement, intrinsic sizes, final
 * layout) and the work grows exponentially with depth. The work is measured by the number of
 * times the text of the leaf is measured.
 */
class LayoutCacheTest {

    private static final DocumentReader READER = new DocumentReader();

    /** {@link FixedMetrics} that counts the measurements. */
    private static final class CountingMetrics implements TextMeasurer {
        private final TextMeasurer metrics = new FixedMetrics();
        int calls;

        @Override
        public double width(String text, FontFace font, double fontSize) {
            calls++;
            return metrics.width(text, font, fontSize);
        }
    }

    /** Alternating {@code Stack} and {@code Grid}, {@code depth} levels, each one with a text beside it. */
    private static String nested(int depth) {
        List<String> lines = new ArrayList<>(List.of("Document (dev.stxt.pdf):", "\tLanguage: es", "\tTitle: Prueba", "\tPage:", "\t\tWidth: 595",
                "\t\tHeight: 842", "\t\tMargin: 40", "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10"));
        String indent = "\t";
        for (int level = depth; level > 0; level--) {
            boolean stack = level % 2 == 1;
            String wrapper = stack ? "Item:" : "Area:";
            lines.add(indent + (stack ? "Stack:" : "Grid:"));
            if (!stack) {
                lines.add(indent + "\tColumn sizes: auto *");
            }
            lines.add(indent + "\t" + wrapper);
            lines.add(indent + "\t\tText: hoja " + level);
            lines.add(indent + "\t" + wrapper);
            indent += "\t\t";
        }
        lines.add(indent + "Text: uno dos tres");
        return String.join("\n", lines);
    }

    private static int measurements(int depth) {
        CountingMetrics metrics = new CountingMetrics();
        new Engine(metrics).layOutDocument(READER.read(Docs.sectioned(nested(depth))));
        return metrics.calls;
    }

    @Test
    void deepNestingGrowsLinearly() {
        int four = measurements(4);
        int eight = measurements(8);
        int twelve = measurements(12);
        // Linear: twice the levels, a little more than twice the work. Exponential (base 2 or more):
        // at least 16 times more with every 4 levels.
        assertTrue(eight < 4 * four, () -> "4 levels: " + four + " measurements; 8: " + eight);
        assertTrue(twelve < 3 * eight, () -> "8 levels: " + eight + " measurements; 12: " + twelve);
    }
}
