package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.TextRuns;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.stxt.pdf.tree.TextRuns.link;
import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Painting order (reference/common/box.md), seen through the background colors that are painted. */
class StackingContextTest {

    private static final DocumentReader READER = new DocumentReader();

    /** Canvas that only records the background colors, in order. */
    private static final class Recorder implements Canvas, Tagging {
        final List<String> backgrounds = new ArrayList<>();
        final List<Rect> rects = new ArrayList<>();
        /** Backgrounds and texts in the order they are painted. */
        final List<String> events = new ArrayList<>();
        /** Each text with its size and its letter spacing: {@code 'text' size/spacing}. */
        final List<String> drawn = new ArrayList<>();

        @Override
        public void background(Rect paddingBox, String color) {
            backgrounds.add(color);
            rects.add(paddingBox);
            events.add("background " + color);
        }

        /** The corners of each background and each border, {@code rx/ry} of the four corners from the top left. */
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
            events.add("image");
        }

        @Override
        public void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
                         double wordSpacing, double letterSpacing) {
            events.add("text '" + text + "' " + font.variant());
            sequences.add("'" + text + "'");
            drawn.add("'" + text + "' " + fontSize + "/" + letterSpacing);
        }

        /** The clickable areas: destination, area and what they say. */
        final List<String> links = new ArrayList<>();

        @Override
        public void link(Box box, Rect area, String destination, String description) {
            links.add(destination + " " + round(area) + " " + description);
            events.add("link " + destination);
        }

        /**
         * The text sequences: {@code [text} or {@code [link}, the runs it paints and {@code ]}; inside, what carries
         * {@code ActualText} between {@code (actual 'text'} and {@code )}.
         */
        final List<String> sequences = new ArrayList<>();

        @Override
        public void beginText(Box box) {
            sequences.add("[text");
        }

        @Override
        public void beginLinkText(Box box) {
            sequences.add("[link");
        }

        @Override
        public void endText() {
            sequences.add("]");
        }

        @Override
        public void beginActualText(String text) {
            sequences.add("(actual '" + text + "'");
        }

        @Override
        public void endActualText() {
            sequences.add(")");
        }

        @Override
        public Tagging tagging() {
            return this;
        }

        @Override
        public void outline(Rect borderBox) {
        }

        @Override
        public VerticalMetrics vertical(FontFace font) {
            return VerticalMetrics.APPROXIMATE;
        }

        @Override
        public DecorationMetrics decoration(FontFace font) {
            return DecorationMetrics.APPROXIMATE;
        }

        @Override
        public void beginRotation(double degrees, double x, double y) {
            events.add(String.format(java.util.Locale.ROOT, "rotate %.0f %.1f %.1f", degrees, x, y));
        }

        @Override
        public void endRotation() {
            events.add("end rotate");
        }

        @Override
        public void beginClip(Rect r, Radii radii) {
            events.add(String.format(java.util.Locale.ROOT, "clip %.0f %.0f %.0f %.0f %s", r.x(), r.y(), r.width(),
                    r.height(), corners(radii)));
        }

        @Override
        public void endClip() {
            events.add("end clip");
        }
    }

    private static String corners(Radii r) {
        return String.format("%.0f/%.0f %.0f/%.0f %.0f/%.0f %.0f/%.0f", r.topLeft().rx(), r.topLeft().ry(),
                r.topRight().rx(), r.topRight().ry(), r.bottomRight().rx(), r.bottomRight().ry(),
                r.bottomLeft().rx(), r.bottomLeft().ry());
    }

    /** What is painted on page {@code index} of a 100 × 100 document with margins of 10 (page area of 80). */
    private static Recorder recordSmallPage(int index, String... boxLines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 100\n\t\tHeight: 100\n\t\tMargin: 10\n"
                + String.join("\n", boxLines);
        Page page = new Engine().layOutDocument(READER.read(Docs.sectioned(doc))).pages().get(index);
        Recorder canvas = new Recorder();
        StackingContext.build(page.content(), page.pageOverlays(), 10, 10).paint(canvas);
        return canvas;
    }

    /** The base font, for documents with text. */
    private static final String FONT = "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n"
            + "\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10";

    private static List<String> paint(String... boxLines) {
        return record(boxLines).backgrounds;
    }

    private static Recorder record(String... boxLines) {
        return record(Map.of(), boxLines);
    }

    private static Recorder record(Map<String, List<dev.stxt.pdf.model.TextRun>> runs, String... boxLines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n\t\tMargin: 40\n"
                + String.join("\n", boxLines);
        Page page = new Engine().layOutDocument(TextRuns.build(READER.read(Docs.sectioned(doc)), runs)).pages().get(0);
        Recorder canvas = new Recorder();
        StackingContext.build(page.content(), page.pageOverlays(), 40, 40).paint(canvas);
        return canvas;
    }

    /** The {@code Overlay} hangs from the fragment of its reference container, behind the {@code Frame}. */
    @Test
    void overlayBeforeALaterFrame() {
        List<String> order = paint(
                "\tFrame:", "\t\tBox:", "\t\t\tHeight: 100", "\t\t\tBackground: #000",
                "\t\tBlock:", "\t\t\tOverlay:", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 10", "\t\t\t\t\tHeight: 10",
                "\t\t\t\t\tBackground: #111",
                "\t\tFrame:", "\t\t\tBox:", "\t\t\t\tHeight: 10", "\t\t\t\tBackground: #222");
        assertEquals(List.of("#000", "#111", "#222"), order);
    }

    @Test
    void equalLayerInDocumentOrder() {
        List<String> order = paint(
                "\tFrame:", "\t\tBox:", "\t\t\tHeight: 100", "\t\t\tBackground: #000",
                "\t\tBlock:", "\t\t\tOverlay:", "\t\t\t\tLayer: 1", "\t\t\t\tBox:", "\t\t\t\t\tWidth: 10",
                "\t\t\t\t\tHeight: 10", "\t\t\t\t\tBackground: #111",
                "\t\tFrame:", "\t\t\tLayer: 1", "\t\t\tBox:", "\t\t\t\tHeight: 10", "\t\t\t\tBackground: #222");
        assertEquals(List.of("#000", "#111", "#222"), order);
    }

    /** The {@code Page overlay} ones are added to the root context at the end, but are painted in their place. */
    @Test
    void pageOverlaysInDocumentOrder() {
        List<String> order = paint(
                "\tPage overlay:", "\t\tBox:", "\t\t\tWidth: 10", "\t\t\tHeight: 10", "\t\t\tBackground: #111",
                "\tFrame:", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tBackground: #222");
        assertEquals(List.of("#111", "#222"), order);
    }

    @Test
    void layerStillWins() {
        List<String> order = paint(
                "\tFrame:", "\t\tLayer: 2", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tBackground: #111",
                "\tFrame:", "\t\tLayer: 1", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tBackground: #222",
                "\tFrame:", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tBackground: #333");
        assertEquals(List.of("#333", "#222", "#111"), order);
    }

    /**
     * The rule between columns goes with the background of the {@code Columns}, and only between columns with
     * content: three columns of 165 with gaps of 10 and two blocks, a single rule, centered in
     * the first gap and with the height of the tallest column.
     */
    @Test
    void separatorBetweenColumnsWithContent() {
        Recorder r = record("\tColumns:", "\t\tCount: 3", "\t\tGap: 10", "\t\tSeparator: 2",
                "\t\tSeparator color: #F00", "\t\tBox:", "\t\t\tBackground: #000",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\t\tBackground: #111", "\t\t\tBreak:",
                "\t\t\t\tKeep together: true",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 20", "\t\t\t\tBackground: #222", "\t\t\tBreak:",
                "\t\t\t\tKeep together: true");
        assertEquals(List.of("#000", "#F00", "#111", "#222"), r.backgrounds);
        assertEquals(new Rect(209, 40, 2, 30), r.rects.get(1));
    }

    @Test
    void ruleColorIsTheTextColorByDefault() {
        Recorder r = record("\tColumns:", "\t\tCount: 2", "\t\tGap: 10", "\t\tSeparator: 1",
                "\t\tTypography:", "\t\t\tColor: #00F",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\tBreak:", "\t\t\t\tKeep together: true",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 30", "\t\t\tBreak:", "\t\t\t\tKeep together: true");
        assertEquals(List.of("#00F"), r.backgrounds);
    }

    /**
     * Each run with its font; the underline before the text and the strikethrough after it
     * (reference/common/text-formatting.md).
     */
    @Test
    void runsWithTheirFontAndDecorations() {
        Recorder r = record(Map.of("T", List.of(run("uno ", "u"), run("dos", "bs"), run(" tres"))),
                "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10", "\t\t\t\tColor: #112233",
                "\tText: uno dos tres", "\t\tId: T");
        assertEquals(List.of("background #112233", "text 'uno ' normal", "text 'dos' bold", "text ' tres' normal",
                "background #112233"), r.events);
        // Line of 12 pt from y = 40: baseline at 40 + (12 − 10) / 2 + 8 = 49 (SVG approximation).
        assertEquals(new Rect(40, 50, 20, 0.6).toString(), round(r.rects.get(0)).toString());
        assertEquals(new Rect(60, 45.9, 15, 0.6).toString(), round(r.rects.get(1)).toString());
    }

    /**
     * Small caps are painted at 80% of the size and letter spacing reaches the canvas (reference/common/typography.md).
     */
    @Test
    void smallCapsAreSmallerAndLetterSpacingIsPassedOn() {
        Recorder r = record("\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10", "\t\t\t\tSmall caps: true",
                "\t\t\t\tLetter spacing: 1.5",
                "\tText: Ab c", "\t\tId: T");
        assertEquals(List.of("'A' 10.0/1.5", "'B' 8.0/1.5", "' ' 10.0/1.5", "'C' 8.0/1.5"), r.drawn);
    }

    /**
     * A box with {@code Link} is clickable as a whole; its area follows its own text (which the {@code Link} wraps) and
     * precedes that of its children, which are painted afterwards (reference/common/links.md).
     */
    @Test
    void aBoxWithLinkIsClickableWholeAndBeforeItsChildren() {
        Recorder r = record("\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\tBlock:", "\t\tLink: #fin", "\t\tBox:", "\t\t\tPadding: 5",
                "\t\tText: uno   dos", "\tText: fin", "\t\tId: fin");
        assertEquals(List.of("#fin " + new Rect(40, 40, 515, 22) + " uno dos"), r.links);
        assertEquals(List.of("link #fin", "text 'uno dos' normal", "text 'fin' normal"), r.events);
    }

    /**
     * A run with a link is an area as tall as the line; consecutive ones with the same destination, a single one
     * (reference/common/links.md).
     */
    @Test
    void linkedRunsAreOneZonePerLineAndDestination() {
        Recorder r = record(Map.of("T", List.of(run("uno "), link("dos", "b", "https://a.example"),
                        link(" tres", "", "https://a.example"), link(" cuatro", "u", "mailto:a@b.c"))),
                "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\tText: uno dos tres cuatro", "\t\tId: T");
        // "uno " is 20 pt; "dos tres" (8 characters, 40 pt) is one area; " cuatro" (35 pt), another.
        assertEquals(List.of("https://a.example " + new Rect(60, 40, 40, 12) + " dos tres",
                "mailto:a@b.c " + new Rect(100, 40, 35, 12) + "  cuatro"), r.links);
        // Each area is emitted inside the sequence of its link, before its text; the rest, a separate sequence.
        assertEquals(List.of("text 'uno ' normal", "link https://a.example", "text 'dos' bold", "text ' tres' normal",
                "link mailto:a@b.c", "text ' cuatro' normal"), r.events.stream()
                .filter(e -> e.startsWith("link") || e.startsWith("text")).toList());
        assertEquals(List.of("[text", "'uno '", "]", "[link", "'dos'", "' tres'", "]", "[link", "' cuatro'", "]"),
                r.sequences);
    }

    /** A link that spans several lines has an area on each one. */
    @Test
    void aLinkOverSeveralLinesHasAZoneInEach() {
        Recorder r = record(Map.of("T", List.of(link("aaaa bbbb cccc dddd", "", "https://a.example"))),
                "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\tBlock:", "\t\tBox:", "\t\t\tWidth: 30",
                "\t\tText: aaaa bbbb cccc dddd", "\t\t\tId: T");
        assertEquals(List.of("https://a.example " + new Rect(40, 40, 20, 12) + " aaaa",
                "https://a.example " + new Rect(40, 52, 20, 12) + " bbbb",
                "https://a.example " + new Rect(40, 64, 20, 12) + " cccc",
                "https://a.example " + new Rect(40, 76, 20, 12) + " dddd"), r.links);
        // But it is a single content: one link sequence for the four lines.
        assertEquals(List.of("[link", "'aaaa '", "'bbbb '", "'cccc '", "'dddd'", "]"), r.sequences);
    }

    /**
     * The lines that are not the last of the fragment carry an invisible space after the last word (the break
     * swallows it), so that the joined text of the lines does not glue words together; the last one and preformatted
     * text do not (reference/common/text-formatting.md).
     */
    @Test
    void aWrappedLineEndsWithASpaceExceptTheLastAndPreformatted() {
        String[] head = {"\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans",
                "\t\t\t\tSize: 10", "\tBlock:", "\t\tBox:", "\t\t\tWidth: 30"};
        Recorder normal = record(concat(head, "\t\tText: aaaa bbbb cccc"));
        assertEquals(List.of("[text", "'aaaa '", "'bbbb '", "'cccc'", "]"), normal.sequences);
        Recorder pre = record(concat(head, "\t\tText: aaaa", "\t\t\tTypography:", "\t\t\t\tPreformatted: wrap"));
        assertEquals(List.of("[text", "'aaaa'", "]"), pre.sequences);
        Recorder preWrapped = record(concat(head, "\t\tText: aaaa bbbb", "\t\t\tTypography:",
                "\t\t\t\tPreformatted: wrap"));
        assertEquals(List.of("[text", "'aaaa'", "'bbbb'", "]"), preWrapped.sequences);
    }

    private static String[] concat(String[] a, String... b) {
        String[] out = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /** The own text of a box with {@code Link} goes before its area (the {@code Link} wraps it). */
    @Test
    void theOwnTextOfALinkedBoxPrecedesItsZone() {
        Recorder r = record("\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\tText: uno dos", "\t\tLink: https://a.example");
        assertEquals(List.of("text 'uno dos' normal", "link https://a.example"), r.events);
        assertEquals(List.of("[text", "'uno dos'", "]"), r.sequences);
    }

    /** A link followed by more text on the same line splits the text into three sequences, in order. */
    @Test
    void textAroundALinkIsOtherSequences() {
        Recorder r = record(Map.of("T", List.of(run("ver "), link("la web", "", "https://a.example"), run(" hoy"))),
                "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\tText: ver la web hoy", "\t\tId: T");
        assertEquals(List.of("[text", "'ver '", "]", "[link", "'la web'", "]", "[text", "' hoy'", "]"), r.sequences);
    }

    /**
     * With letter spacing, consecutive runs of a line with the same link carry a single {@code ActualText}, with
     * the invisible space at the end of the line if they reach it (reference/implementation/pdf.md).
     */
    @Test
    void letterSpacedRunsShareOneActualTextPerLinkSequenceAndLine() {
        Recorder r = record(Map.of("T", List.of(run("ver "), run("la", "b"), run(" "), link("web", "", "https://a.example"),
                        run(" hoy y mañana"))),
                "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\tText: ver la web hoy y mañana", "\t\tId: T", "\t\tBox:", "\t\t\tWidth: 90",
                "\t\tTypography:", "\t\t\tLetter spacing: 1");
        assertEquals(List.of("[text", "(actual 'ver la '", "'ver '", "'la'", "' '", ")", "]",
                "[link", "(actual 'web'", "'web'", ")", "]",
                "[text", "(actual ' hoy '", "' hoy '", ")", "(actual 'y mañana'", "'y mañana'", ")", "]"), r.sequences);
    }

    @Test
    void noLinkMeansNoZone() {
        assertEquals(List.of(), record("\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10",
                "\tText: sin enlace").links);
    }

    /**
     * {@code Corner radius} belongs to the border box; the background, inside the border, loses its width; the image,
     * the border and the padding.
     */
    @Test
    void radiusRoundsTheBorderBoxAndTheInnerBoxesLoseTheBorder() {
        Recorder r = record("\tBlock:", "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tHeight: 50", "\t\t\tCorner radius: 10",
                "\t\t\tBorder: 2", "\t\t\tBackground: #CCC");
        assertEquals(List.of("8/8 8/8 8/8 8/8"), r.fillRadii);
        // The first border is the one of the document root.
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
                "\t\t\tCorner radius: 50%", "\t\t\tBorder: 5", "\t\t\tPadding: 5");
        // Radius of 25 minus 5 of border and 5 of padding.
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
        // A gradient is not a solid color: it does not go through background.
        assertEquals(List.of(), r.backgrounds);
    }

    /**
     * The band of a row is painted below its cells: its background goes before theirs, and its border with the color
     * of the text of the row (reference/components/table.md#rows).
     */
    @Test
    void theBandOfARowIsPaintedUnderItsCells() {
        Recorder r = record("\tTable:", "\t\tColumn sizes: 100 100", "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tBox:", "\t\t\t\t\tBackground: #EEE", "\t\t\t\tCell:",
                "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: 10", "\t\t\t\t\t\tBackground: #111", "\t\t\t\tCell:",
                "\t\t\tRow:", "\t\t\t\tCell:", "\t\t\t\t\tBox:", "\t\t\t\t\t\tHeight: 10",
                "\t\t\t\t\t\tBackground: #222", "\t\t\t\tCell:");
        assertEquals(List.of("background #EEE", "background #111", "background #222"), r.events);
    }

    /**
     * A box split with {@code slice} only rounds the top corners in the first fragment and the bottom ones in the
     * last.
     */
    @Test
    void aSlicedBoxRoundsOnlyItsOuterCorners() {
        String[] box = {"\tBlock:", "\t\tBox:", "\t\t\tHeight: 120", "\t\t\tCorner radius: 6", "\t\t\tBackground: #CCC"};
        assertEquals(List.of("6/6 6/6 0/0 0/0"), recordSmallPage(0, box).fillRadii);
        assertEquals(List.of("0/0 0/0 6/6 6/6"), recordSmallPage(1, box).fillRadii);
    }

    @Test
    void aClonedBoxRoundsEveryFragment() {
        String[] box = {"\tBlock:", "\t\tBox:", "\t\t\tHeight: 120", "\t\t\tCorner radius: 6", "\t\t\tBackground: #CCC",
                "\t\tBreak:", "\t\t\tEdges: closed"};
        assertEquals(List.of("6/6 6/6 6/6 6/6"), recordSmallPage(0, box).fillRadii);
        assertEquals(List.of("6/6 6/6 6/6 6/6"), recordSmallPage(1, box).fillRadii);
    }

    private static Rect round(Rect r) {
        return new Rect(round(r.x()), round(r.y()), round(r.width()), round(r.height()));
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    /**
     * A rotated positioned element is painted rotated with everything it contains, also an {@code Overlay} inside,
     * around its origin (reference/common/layers-and-rotation.md); an in-flow {@code Image}, around its center and in
     * its two steps.
     */
    @Test
    void rotatedBoxesWrapEverythingTheyPaint() {
        Recorder r = record(
                "\tFrame:", "\t\tRotate: 8", "\t\tRotate origin: top left", "\t\tBox:", "\t\t\tHeight: 100",
                "\t\t\tBackground: #000",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 10", "\t\t\t\tBackground: #111",
                "\t\tOverlay:", "\t\t\tOffset: 0 0 auto auto", "\t\t\tBox:", "\t\t\t\tWidth: 10", "\t\t\t\tHeight: 10",
                "\t\t\t\tBackground: #222",
                "\tImage: test/square.png", "\t\tRotate: 45", "\t\tBox:", "\t\t\tBackground: #333",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tBackground: #444");
        // First the flow (the 30 × 30 image takes the square that encloses it, 42.43 a side, at (40, 140), with its
        // center at (61.2, 161.2), rotated in the backgrounds step and in the content step) and then the positioned
        // element: each step of its own and of what it contains (also the Overlay), rotated.
        String turn = "rotate 8 40.0 40.0";
        assertEquals(List.of("rotate 45 61.2 161.2", "background #333", "end rotate", "background #444",
                "rotate 45 61.2 161.2", "image", "end rotate",
                turn, "background #000", "end rotate", turn, "background #111", "end rotate", turn, "end rotate",
                turn, "end rotate", turn, "background #222", "end rotate", turn, "end rotate"), r.events);
    }

    /**
     * A rotated in-flow {@code Block}: its descendants are painted in the steps of the context that contains it, each
     * with the rotation of the block, and so is the {@code Overlay} inside, which has it as reference container
     * (reference/common/layers-and-rotation.md).
     */
    @Test
    void aBlockRotatedInFlowTurnsItsDescendantsInEveryStep() {
        Recorder r = record(
                "\tBlock:", "\t\tRotate: 90", "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tHeight: 20",
                "\t\t\tBackground: #555",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tHeight: 10", "\t\t\t\tBackground: #666",
                "\t\tOverlay:", "\t\t\tOffset: 0 0 auto auto", "\t\t\tBox:", "\t\t\t\tWidth: 10", "\t\t\t\tHeight: 10",
                "\t\t\t\tBackground: #777",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 10", "\t\t\tBackground: #888");
        // The 100 × 20 block takes, rotated, 20 × 100 at (40, 40): its center is (50, 90).
        String rotate = "rotate 90 50.0 90.0";
        assertEquals(List.of(rotate, "background #555", "end rotate", rotate, "background #666", "end rotate",
                "background #888", rotate, "end rotate", rotate, "end rotate",
                rotate, "background #777", "end rotate", rotate, "end rotate"), r.events);
    }

    /**
     * A box with rounded corners clips, along the curve of its padding box, the backgrounds, borders and images of its
     * flow, also those of a {@code Frame}, but not its text or an {@code Overlay}
     * (reference/common/box.md).
     */
    @Test
    void roundedCornersClipTheBackgroundsOfTheFlowButNotTheText() {
        Recorder r = record(FONT,
                "\tBlock:", "\t\tBox:", "\t\t\tWidth: 100", "\t\t\tCorner radius: 12", "\t\t\tBorder: 2",
                "\t\t\tBackground: #000",
                "\t\tBlock:", "\t\t\tBox:", "\t\t\t\tBackground: #111", "\t\t\tText: hola",
                "\t\tFrame:", "\t\t\tBox:", "\t\t\t\tHeight: 10", "\t\t\t\tBackground: #222",
                "\t\tOverlay:", "\t\t\tOffset: -5 -5 auto auto", "\t\t\tBox:", "\t\t\t\tWidth: 10",
                "\t\t\t\tHeight: 10", "\t\t\t\tBackground: #333");
        // The padding box: the border box (at 40, 40, 100 wide and with 22 of content) minus the border of 2, with
        // radii of 10.
        String clip = "clip 42 42 96 22 10/10 10/10 10/10 10/10";
        // The text is painted without clipping, and the Overlay, which sticks out at the corner, is not clipped either.
        assertEquals(List.of("background #000", clip, "background #111", "end clip", "text 'hola' normal",
                clip, "background #222", "end clip", "background #333"),
                r.events.stream().filter(e -> !e.startsWith("background #FFF")).toList());
    }

    @Test
    void squareCornersDoNotClip() {
        Recorder r = record(FONT, "\tBlock:", "\t\tBox:", "\t\t\tBackground: #000", "\t\tBlock:", "\t\t\tBox:",
                "\t\t\t\tBackground: #111", "\t\t\tText: hola");
        assertTrue(r.events.stream().noneMatch(e -> e.startsWith("clip")), r.events.toString());
    }

    /** An image inside a rounded box is clipped along the curve; its alternative text changes nothing. */
    @Test
    void anImageInsideARoundedBoxIsClipped() {
        Recorder r = record("\tBlock:", "\t\tBox:", "\t\t\tCorner radius: 8", "\t\tImage: test/wide.png", "\t\t\tBox:",
                "\t\t\t\tWidth: 50");
        int image = r.events.indexOf("image");
        assertTrue(image > 0 && r.events.get(image - 1).startsWith("clip "), r.events.toString());
        assertEquals("end clip", r.events.get(image + 1));
    }
}
