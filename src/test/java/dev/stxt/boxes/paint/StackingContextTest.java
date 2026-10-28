package dev.stxt.boxes.paint;

import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Paint;
import dev.stxt.boxes.model.Radii;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.TextRuns;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.stxt.boxes.tree.TextRuns.link;
import static dev.stxt.boxes.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Orden de pintado (ESPECIFICACION.md §5.6), visto por los colores de fondo que se pintan. */
class StackingContextTest {

    private static final DocumentReader READER = new DocumentReader();

    /** Lienzo que solo apunta los colores de fondo, en orden. */
    private static final class Recorder implements Canvas {
        final List<String> backgrounds = new ArrayList<>();
        final List<Rect> rects = new ArrayList<>();
        /** Fondos y textos en el orden en que se pintan. */
        final List<String> events = new ArrayList<>();
        /** Cada texto con su tamaño y su interletraje: {@code 'texto' tamaño/interletraje}. */
        final List<String> drawn = new ArrayList<>();

        @Override
        public void background(Rect paddingBox, String color) {
            backgrounds.add(color);
            rects.add(paddingBox);
            events.add("background " + color);
        }

        /** Las esquinas de cada fondo y de cada borde, {@code rx/ry} de las cuatro esquinas desde arriba a la izquierda. */
        final List<String> fillRadii = new ArrayList<>();
        final List<String> borderRadii = new ArrayList<>();
        final List<Sides<String>> borderColors = new ArrayList<>();
        final List<String> imageRadii = new ArrayList<>();
        final List<Paint> fills = new ArrayList<>();

        @Override
        public void fill(Rect paddingBox, Radii radii, Paint paint) {
            fills.add(paint);
            fillRadii.add(corners(radii));
            if (paint instanceof Paint.Solid solid) {
                background(paddingBox, solid.color());
            }
        }

        @Override
        public void border(Rect borderBox, Insets widths, Radii radii, Sides<String> colors) {
            borderRadii.add(corners(radii));
            borderColors.add(colors);
        }

        @Override
        public void image(Box box, Rect content, Radii radii) {
            imageRadii.add(corners(radii));
        }

        @Override
        public void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
                         double wordSpacing, double letterSpacing) {
            events.add("text '" + text + "' " + font.variant());
            drawn.add("'" + text + "' " + fontSize + "/" + letterSpacing);
        }

        /** Las zonas clicables: destino, zona y lo que dicen. */
        final List<String> links = new ArrayList<>();

        @Override
        public void link(Box box, Rect area, String destination, String description) {
            links.add(destination + " " + round(area) + " " + description);
            events.add("link " + destination);
        }
    }

    private static String corners(Radii r) {
        return String.format("%.0f/%.0f %.0f/%.0f %.0f/%.0f %.0f/%.0f", r.topLeft().rx(), r.topLeft().ry(),
                r.topRight().rx(), r.topRight().ry(), r.bottomRight().rx(), r.bottomRight().ry(),
                r.bottomLeft().rx(), r.bottomLeft().ry());
    }

    /** Lo que se pinta en la página {@code index} de un documento de 100 × 100 con márgenes de 10 (área útil de 80). */
    private static Recorder recordSmallPage(int index, String... boxLines) {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 100\n\t\tHeight: 100\n\t\tMargin: 10\n"
                + String.join("\n", boxLines);
        Page page = new Engine().layOutDocument(READER.read(doc)).pages().get(index);
        Recorder canvas = new Recorder();
        StackingContext.build(page.content(), page.fixed(), 10, 10).paint(canvas);
        return canvas;
    }

    private static List<String> paint(String... boxLines) {
        return record(boxLines).backgrounds;
    }

    private static Recorder record(String... boxLines) {
        return record(Map.of(), boxLines);
    }

    private static Recorder record(Map<String, List<dev.stxt.boxes.model.TextRun>> runs, String... boxLines) {
        String doc = "Document (dev.stxt.boxes):\n\tLanguage: es\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n\t\tMargin: 40\n"
                + String.join("\n", boxLines);
        Page page = new Engine().layOutDocument(TextRuns.build(READER.read(doc), runs)).pages().get(0);
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

    /** Cada tramo con su fuente; el subrayado antes del texto y el tachado después (§5.4). */
    @Test
    void runsWithTheirFontAndDecorations() {
        Recorder r = record(Map.of("T", List.of(run("uno ", "u"), run("dos", "bs"), run(" tres"))),
                "\tStyles:", "\t\tElement: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10", "\t\t\t\tColor: #112233",
                "\tText: uno dos tres", "\t\tId: T");
        assertEquals(List.of("background #112233", "text 'uno ' normal", "text 'dos' bold", "text ' tres' normal",
                "background #112233"), r.events);
        // Línea de 12 pt desde y = 40: línea base en 40 + (12 − 10) / 2 + 8 = 49 (aproximación del SVG).
        assertEquals(new Rect(40, 50, 20, 0.6).toString(), round(r.rects.get(0)).toString());
        assertEquals(new Rect(60, 45.9, 15, 0.6).toString(), round(r.rects.get(1)).toString());
    }

    /** Las versalitas se pintan al 80 % del tamaño y el interletraje llega al lienzo (§4.4). */
    @Test
    void smallCapsAreSmallerAndLetterSpacingIsPassedOn() {
        Recorder r = record("\tStyles:", "\t\tElement: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10", "\t\t\t\tSmall caps: true",
                "\t\t\t\tLetter spacing: 1.5",
                "\tText: Ab c", "\t\tId: T");
        assertEquals(List.of("'A' 10.0/1.5", "'B' 8.0/1.5", "' ' 10.0/1.5", "'C' 8.0/1.5"), r.drawn);
    }

    /** Una caja con {@code Link} es clicable entera, antes que su contenido (§5.20). */
    @Test
    void aBoxWithLinkIsClickableWholeAndBeforeItsContent() {
        Recorder r = record("\tStyles:", "\t\tElement: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\tBlock:", "\t\tLink: #fin", "\t\tBox:", "\t\t\tPadding: 5",
                "\t\tText: uno   dos", "\tText: fin", "\t\tId: fin");
        assertEquals(List.of("#fin " + new Rect(40, 40, 515, 22) + " uno dos"), r.links);
        assertEquals(List.of("link #fin", "text 'uno dos' normal", "text 'fin' normal"), r.events);
    }

    /** Un tramo con enlace es una zona del alto de la línea; los seguidos con el mismo destino, una sola (§5.20). */
    @Test
    void linkedRunsAreOneZonePerLineAndDestination() {
        Recorder r = record(Map.of("T", List.of(run("uno "), link("dos", "b", "https://a.example"),
                        link(" tres", "", "https://a.example"), link(" cuatro", "u", "mailto:a@b.c"))),
                "\tStyles:", "\t\tElement: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\tText: uno dos tres cuatro", "\t\tId: T");
        // "uno " son 20 pt; "dos tres" (8 caracteres, 40 pt) es una zona; " cuatro" (35 pt), otra.
        assertEquals(List.of("https://a.example " + new Rect(60, 40, 40, 12) + " dos tres",
                "mailto:a@b.c " + new Rect(100, 40, 35, 12) + "  cuatro"), r.links);
        // Las zonas se emiten antes que el texto de la caja.
        assertEquals(List.of("link https://a.example", "link mailto:a@b.c", "text 'uno ' normal"),
                r.events.stream().filter(e -> e.startsWith("link") || e.equals("text 'uno ' normal")).toList());
    }

    /** Un enlace que ocupa varias líneas tiene una zona en cada una. */
    @Test
    void aLinkOverSeveralLinesHasAZoneInEach() {
        Recorder r = record(Map.of("T", List.of(link("aaaa bbbb cccc dddd", "", "https://a.example"))),
                "\tStyles:", "\t\tElement: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\tBlock:", "\t\tBox:", "\t\t\tWidth: 30",
                "\t\tText: aaaa bbbb cccc dddd", "\t\t\tId: T");
        assertEquals(List.of("https://a.example " + new Rect(40, 40, 20, 12) + " aaaa",
                "https://a.example " + new Rect(40, 52, 20, 12) + " bbbb",
                "https://a.example " + new Rect(40, 64, 20, 12) + " cccc",
                "https://a.example " + new Rect(40, 76, 20, 12) + " dddd"), r.links);
    }

    @Test
    void noLinkMeansNoZone() {
        assertEquals(List.of(), record("\tStyles:", "\t\tElement: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\tText: sin enlace").links);
    }

    /** {@code Radius} es de la caja de borde; el fondo, dentro del borde, pierde su grosor; la imagen, el borde y el relleno. */
    @Test
    void radiusRoundsTheBorderBoxAndTheInnerBoxesLoseTheBorder() {
        Recorder r = record("\tBlock:", "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tHeight: 50", "\t\t\tRadius: 10",
                "\t\t\tBorder: 2", "\t\t\tBackground: #CCC");
        assertEquals(List.of("8/8 8/8 8/8 8/8"), r.fillRadii);
        // El primer borde es el de la raíz del documento.
        assertEquals("10/10 10/10 10/10 10/10", r.borderRadii.get(r.borderRadii.size() - 1));
    }

    @Test
    void aBoxWithoutRadiusHasSquareCorners() {
        Recorder r = record("\tBlock:", "\t\tBox:", "\t\t\tHeight: 50", "\t\t\tBackground: #CCC");
        assertEquals(List.of("0/0 0/0 0/0 0/0"), r.fillRadii);
    }

    @Test
    void imagesAreClippedWithTheRadiiOfTheirContentBox() {
        Recorder r = record("\tImage: test/wide.png", "\t\tBox:", "\t\t\tWidth: 50", "\t\t\tHeight: 50",
                "\t\t\tRadius: 50%", "\t\t\tBorder: 5", "\t\t\tPadding: 5");
        // Radio de 25 menos 5 de borde y 5 de relleno.
        assertEquals(List.of("15/15 15/15 15/15 15/15"), r.imageRadii);
        assertEquals("25/25 25/25 25/25 25/25", r.borderRadii.get(r.borderRadii.size() - 1));
    }

    @Test
    void eachSideOfTheBorderHasItsOwnColor() {
        Recorder r = record("\tBlock:", "\t\tBox:", "\t\t\tHeight: 50", "\t\t\tBorder: 2",
                "\t\t\tBorder color: #111 #222 #333 #444");
        assertEquals(List.of(new Sides<>("#111", "#222", "#333", "#444")), r.borderColors.subList(r.borderColors.size() - 1,
                r.borderColors.size()));
    }

    @Test
    void withoutBorderColorTheBorderTakesTheTextColor() {
        Recorder r = record("\tBlock:", "\t\tTypography:", "\t\t\tColor: #123456", "\t\tBox:", "\t\t\tHeight: 50",
                "\t\t\tBorder: 2");
        assertEquals(Sides.all("#123456"), r.borderColors.get(r.borderColors.size() - 1));
    }

    @Test
    void gradientsAreHandedToTheCanvasWhole() {
        Recorder r = record("\tBlock:", "\t\tBox:", "\t\t\tHeight: 50", "\t\t\tBackground: linear(90, #000, #FFF)");
        assertEquals(List.of(new Paint.Linear(90, List.of("#000", "#FFF"))), r.fills);
        // Un degradado no es un color liso: no pasa por background.
        assertEquals(List.of(), r.backgrounds);
    }

    /** Una caja partida con {@code slice} solo redondea las esquinas de arriba en el primer fragmento y las de abajo en el último. */
    @Test
    void aSlicedBoxRoundsOnlyItsOuterCorners() {
        String[] box = {"\tBlock:", "\t\tBox:", "\t\t\tHeight: 120", "\t\t\tRadius: 6", "\t\t\tBackground: #CCC"};
        assertEquals(List.of("6/6 6/6 0/0 0/0"), recordSmallPage(0, box).fillRadii);
        assertEquals(List.of("0/0 0/0 6/6 6/6"), recordSmallPage(1, box).fillRadii);
    }

    @Test
    void aClonedBoxRoundsEveryFragment() {
        String[] box = {"\tBlock:", "\t\tBox:", "\t\t\tHeight: 120", "\t\t\tRadius: 6", "\t\t\tBackground: #CCC",
                "\t\tBreak:", "\t\t\tDecoration: clone"};
        assertEquals(List.of("6/6 6/6 6/6 6/6"), recordSmallPage(0, box).fillRadii);
        assertEquals(List.of("6/6 6/6 6/6 6/6"), recordSmallPage(1, box).fillRadii);
    }

    private static Rect round(Rect r) {
        return new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height()));
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
