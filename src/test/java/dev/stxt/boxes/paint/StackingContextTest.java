package dev.stxt.boxes.paint;

import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.stxt.DocumentReader;
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
        final List<Rect> rects = new ArrayList<>();

        @Override
        public void background(Rect paddingBox, String color) {
            backgrounds.add(color);
            rects.add(paddingBox);
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
        return record(boxLines).backgrounds;
    }

    private static Recorder record(String... boxLines) {
        String doc = "Document (dev.stxt.boxes):\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n\t\tMargin: 40\n"
                + String.join("\n", boxLines);
        Page page = new Engine().layOutDocument(READER.read(doc)).pages().get(0);
        Recorder canvas = new Recorder();
        StackingContext.build(page.content(), page.fixed(), 40, 40).paint(canvas);
        return canvas;
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

    /**
     * La línea entre columnas va con el fondo del {@code Multicol}, y solo entre columnas con
     * contenido: tres columnas de 165 con huecos de 10 y dos bloques, una sola línea, centrada en
     * el primer hueco y con el alto de la columna más alta.
     */
    @Test
    void multicolRuleBetweenColumnsWithContent() {
        Recorder r = record("\tMulticol:", "\t\tCount: 3", "\t\tColumn gap: 10", "\t\tRule: 2",
                "\t\tRule color: #F00", "\t\tBox:", "\t\t\tBackground: #000",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tBackground: #111", "\t\t\tBreak:",
                "\t\t\t\tInside: avoid",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 20", "\t\t\t\tBackground: #222", "\t\t\tBreak:",
                "\t\t\t\tInside: avoid");
        assertEquals(List.of("#000", "#F00", "#111", "#222"), r.backgrounds);
        assertEquals(new Rect(209, 40, 2, 30), r.rects.get(1));
    }

    @Test
    void ruleColorIsTheTextColorByDefault() {
        Recorder r = record("\tMulticol:", "\t\tCount: 2", "\t\tColumn gap: 10", "\t\tRule: 1",
                "\t\tTypography:", "\t\t\tColor: #00F",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\tBreak:", "\t\t\t\tInside: avoid",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\tBreak:", "\t\t\t\tInside: avoid");
        assertEquals(List.of("#00F"), r.backgrounds);
    }
}
