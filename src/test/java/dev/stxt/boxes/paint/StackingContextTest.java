package dev.stxt.boxes.paint;

import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.boxes.tree.FontFace;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Orden de pintado (ESPECIFICACION.md §5.6), visto por los colores de fondo que se pintan. */
class StackingContextTest {

    private static final DocumentReader READER = new DocumentReader();

    /** Lienzo que solo apunta los colores de fondo, en orden. */
    private static final class Recorder implements Canvas {
        final List<String> backgrounds = new ArrayList<>();

        @Override
        public void background(Rect paddingBox, String color) {
            backgrounds.add(color);
        }

        @Override
        public void border(Rect borderBox, Insets widths, String color) {
        }

        @Override
        public void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
                         double wordSpacing) {
        }
    }

    private static List<String> paint(String... boxLines) {
        String doc = "Document (dev.stxt.boxes):\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n\t\tMargin: 40\n"
                + String.join("\n", boxLines);
        Page page = new Engine().layOutDocument(READER.read(doc)).pages().get(0);
        Recorder canvas = new Recorder();
        StackingContext.build(page.content(), page.fixed(), 40, 40).paint(canvas);
        return canvas.backgrounds;
    }

    /** La {@code absolute} cuelga del fragmento de su containing block, detrás de la {@code relative}. */
    @Test
    void absoluteBeforeLaterRelative() {
        List<String> order = paint(
                "\tRelative:", "\t\tBox:", "\t\t\tHeight: 100", "\t\t\tBackground: #000",
                "\t\tBlock:", "\t\t\tAbsolute:", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 10", "\t\t\t\t\tHeight: 10",
                "\t\t\t\t\tBackground: #111",
                "\t\tRelative:", "\t\t\tBox:", "\t\t\t\tHeight: 10", "\t\t\t\tBackground: #222");
        assertEquals(List.of("#000", "#111", "#222"), order);
    }

    @Test
    void equalLayerInDocumentOrder() {
        List<String> order = paint(
                "\tRelative:", "\t\tBox:", "\t\t\tHeight: 100", "\t\t\tBackground: #000",
                "\t\tBlock:", "\t\t\tAbsolute:", "\t\t\t\tLayer: 1", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 10",
                "\t\t\t\t\tHeight: 10", "\t\t\t\t\tBackground: #111",
                "\t\tRelative:", "\t\t\tLayer: 1", "\t\t\tBox:", "\t\t\t\tHeight: 10", "\t\t\t\tBackground: #222");
        assertEquals(List.of("#000", "#111", "#222"), order);
    }

    /** Las {@code fixed} se añaden al contexto raíz al final, pero se pintan en su sitio. */
    @Test
    void fixedInDocumentOrder() {
        List<String> order = paint(
                "\tFixed:", "\t\tBox:", "\t\t\tWidth: 10", "\t\t\tHeight: 10", "\t\t\tBackground: #111",
                "\tRelative:", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tBackground: #222");
        assertEquals(List.of("#111", "#222"), order);
    }

    @Test
    void layerStillWins() {
        List<String> order = paint(
                "\tRelative:", "\t\tLayer: 2", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tBackground: #111",
                "\tRelative:", "\t\tLayer: 1", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tBackground: #222",
                "\tRelative:", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tBackground: #333");
        assertEquals(List.of("#333", "#222", "#111"), order);
    }
}
