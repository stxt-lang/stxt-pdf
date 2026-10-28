package dev.stxt.boxes.layout;

import dev.stxt.boxes.Docs;
import dev.stxt.boxes.layout.inline.FixedMetrics;
import dev.stxt.boxes.layout.inline.TextMeasurer;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresión de la regla de {@code CLAUDE.md}: toda recursión de layout y de tamaños intrínsecos
 * pasa por {@code engine.layout}/{@code engine.intrinsic}, que usan la {@link LayoutCache}. Sin
 * ella, cada nivel de flex o grid maqueta a sus hijos varias veces (medida, intrínsecos, layout
 * final) y el trabajo crece de forma exponencial con la profundidad. El trabajo se mide por las
 * veces que se mide el texto de la hoja.
 */
class LayoutCacheTest {

    private static final DocumentReader READER = new DocumentReader();

    /** {@link FixedMetrics} que cuenta las mediciones. */
    private static final class CountingMetrics implements TextMeasurer {
        private final TextMeasurer metrics = new FixedMetrics();
        int calls;

        @Override
        public double width(String text, FontFace font, double fontSize) {
            calls++;
            return metrics.width(text, font, fontSize);
        }
    }

    /** {@code Flex} y {@code Grid} alternos, {@code depth} niveles, cada uno con un texto al lado. */
    private static String nested(int depth) {
        List<String> lines = new ArrayList<>(List.of("Document (dev.stxt.boxes):", "\tLanguage: es", "\tTitle: Prueba", "\tMedia:", "\t\tWidth: 595",
                "\t\tHeight: 842", "\t\tMargin: 40", "\tStyles:", "\t\tElement: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10"));
        String indent = "\t";
        for (int level = depth; level > 0; level--) {
            boolean flex = level % 2 == 1;
            String wrapper = flex ? "Item:" : "Area:";
            lines.add(indent + (flex ? "Flex:" : "Grid:"));
            if (!flex) {
                lines.add(indent + "\tColumns: auto 1fr");
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
        // Lineal: el doble de niveles, algo más del doble de trabajo. Exponencial (base 2 o más):
        // al menos 16 veces más con cada 4 niveles.
        assertTrue(eight < 4 * four, () -> "4 niveles: " + four + " mediciones; 8: " + eight);
        assertTrue(twelve < 3 * eight, () -> "8 niveles: " + eight + " mediciones; 12: " + twelve);
    }
}
