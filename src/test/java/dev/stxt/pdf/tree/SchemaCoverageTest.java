package dev.stxt.pdf.tree;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.stxt.DocumentReader;

/**
 * Every property and every group ({@code Box}, {@code Typography}, {@code Break}) that the schema
 * allows in a node is read by {@link BoxTreeBuilder}: the reading {@code switch} statements have no
 * {@code default} branch that assigns silently; they throw {@link IllegalStateException} instead.
 *
 * <p>It walks the schema and, for each (node, property) pair, builds the tree of a minimal
 * document that declares it in that node with a valid value. If the schema gains a property or
 * allows it in another node, this test fails until its sample is added to {@link #SAMPLES} (and, if
 * it is a new node, its template to {@link #TEMPLATES}): that is the reminder to read it in the builder.</p>
 */
class SchemaCoverageTest {

    /** Property groups: they are tested as children, like the properties. */
    private static final Set<String> GROUPS = Set.of("Box", "Typography", "Break");

    /** Valid value of each property and group, by its name (unique in the whole namespace). */
    private static final Map<String, String> SAMPLES = Map.ofEntries(
            // Document
            Map.entry("Title", "Title: Informe"),
            Map.entry("Subject", "Subject: Resultados del año"),
            Map.entry("Author", "Author: Ana Pérez"),
            Map.entry("Language", "Language: es"),
            // Section
            Map.entry("Page numbering", "Page numbering: restart"),
            Map.entry("Page number format", "Page number format: lower-roman"),
            Map.entry("Inherit page overlays", "Inherit page overlays: false"),
            Map.entry("Begin on", "Begin on: any"),
            Map.entry("Mirror margins", "Mirror margins: false"),
            Map.entry("Footnote numbering", "Footnote numbering: restart"),
            Map.entry("Footnote number format", "Footnote number format: lower-alpha"),
            // Elements
            Map.entry("Id", "Id: x"),
            Map.entry("Style", "Style: c"),
            Map.entry("Link", "Link: https://example.com/a"),
            Map.entry("In table of contents", "In table of contents: false"),
            Map.entry("Isolate margins", "Isolate margins: true"),
            Map.entry("Trim margins", "Trim margins: both"),
            Map.entry("Offset", "Offset: 10 auto auto 10"),
            Map.entry("Layer", "Layer: 1"),
            Map.entry("Pages", "Pages: not-first"),
            Map.entry("Rotate", "Rotate: -8"),
            Map.entry("Rotate origin", "Rotate: 15\nRotate origin: top left"),
            // A Block with Side needs a width; an image does not mind it.
            Map.entry("Side", "Side: left\nBox:\n\tWidth: 100"),
            Map.entry("Below side boxes", "Below side boxes: true"),
            Map.entry("Drop cap", "Drop cap: 3"),
            // Page
            Map.entry("Paper", "Paper: A4"),
            Map.entry("Orientation", "Orientation: landscape"),
            Map.entry("Content", "Content >>\n\tHola"),
            Map.entry("Span", "Span: 1 1"),
            Map.entry("Row header", "Row header: true"),
            // Groups
            // The Box of a Row only allows the look.
            Map.entry("Box", "Box:\n\tBackground: #EEEEEE"),
            Map.entry("Typography", "Typography:\n\tColor: #333333"),
            // Keep together is not allowed in an Image, which is not broken.
            Map.entry("Break", "Break:\n\tKeep with next: true"),
            // Box
            Map.entry("Width", "Width: 100"),
            Map.entry("Height", "Height: 100"),
            Map.entry("Min width", "Min width: 10"),
            Map.entry("Max width", "Max width: 200"),
            Map.entry("Min height", "Min height: 10"),
            Map.entry("Max height", "Max height: 200"),
            Map.entry("Aspect ratio", "Aspect ratio: 2"),
            Map.entry("Margin", "Margin: 5"),
            Map.entry("Padding", "Padding: 5"),
            Map.entry("Border", "Border: 1"),
            Map.entry("Border color", "Border color: #000000"),
            Map.entry("Corner radius", "Corner radius: 4"),
            Map.entry("Background", "Background: #FFFFFF"),
            Map.entry("Overflow", "Overflow: error"),
            // Typography
            Map.entry("Font", "Font: IBM Plex Sans"),
            Map.entry("Bold", "Bold: true"),
            Map.entry("Weight", "Weight: 600"),
            Map.entry("Italic", "Italic: true"),
            Map.entry("Size", "Size: 12"),
            Map.entry("Min size", "Min size: 6"),
            Map.entry("Line height", "Line height: 14"),
            Map.entry("Align", "Align: center"),
            Map.entry("Color", "Color: #333333"),
            Map.entry("Line breaking", "Line breaking: optimal"),
            Map.entry("Hyphenate", "Hyphenate: true"),
            Map.entry("Break long words", "Break long words: new-line"),
            Map.entry("First line indent", "First line indent: 12"),
            Map.entry("Markup", "Markup: markdown"),
            Map.entry("Case", "Case: uppercase"),
            Map.entry("Letter spacing", "Letter spacing: 1.5"),
            Map.entry("Small caps", "Small caps: true"),
            Map.entry("Ligatures", "Ligatures: true"),
            Map.entry("Kerning", "Kerning: true"),
            Map.entry("Code font", "Code font: IBM Plex Mono"),
            Map.entry("Preformatted", "Preformatted: wrap"),
            Map.entry("Tab size", "Tab size: 8"),
            Map.entry("Numbered", "Numbered: true"),
            Map.entry("Number format", "Number format: upper-roman"),
            Map.entry("Number label", "Number label: Anexo {number}."),
            Map.entry("Widows", "Widows: 3"),
            Map.entry("Orphans", "Orphans: 3"),
            // Break
            Map.entry("Before", "Before: page"),
            Map.entry("After", "After: page"),
            Map.entry("Keep with next", "Keep with next: true"),
            Map.entry("Keep with previous", "Keep with previous: true"),
            Map.entry("Keep together", "Keep together: true"),
            Map.entry("Edges", "Edges: closed"),
            // Stack and Item
            Map.entry("Direction", "Direction: vertical"),
            Map.entry("Wrap", "Wrap: true"),
            Map.entry("Split items", "Split items: false"),
            Map.entry("Split rows", "Split rows: false"),
            Map.entry("Merge borders", "Merge borders: false"),
            Map.entry("Gap", "Gap: 5"),
            Map.entry("Line distribution", "Line distribution: center"),
            Map.entry("Grow", "Grow: 1"),
            Map.entry("Shrink", "Shrink: 0"),
            Map.entry("Base size", "Base size: 50"),
            // Grid and Area
            Map.entry("Column sizes", "Column sizes: 100"),
            Map.entry("Row sizes", "Row sizes: 20"),
            Map.entry("Area map", "Area map >>\n\ta"),
            Map.entry("Fill order", "Fill order: columns"),
            Map.entry("Horizontal", "Horizontal: center"),
            Map.entry("Vertical", "Vertical: center"),
            // Lists
            Map.entry("Marker", "Marker: decimal"),
            Map.entry("First number", "First number: 3"),
            Map.entry("Number", "Number: 5"),
            // Columns
            Map.entry("Column count", "Column count: 3"),
            Map.entry("Min column width", "Min column width: 100"),
            Map.entry("Separator", "Separator: 1"),
            Map.entry("Separator color", "Separator color: #999999"),
            Map.entry("Balance", "Balance: false"),
            // Markdown
            Map.entry("Bullet markers", "Bullet markers: bullet dash"),
            Map.entry("Number markers", "Number markers: decimal lower-alpha"),
            Map.entry("Table column sizes", "Table column sizes: auto"),
            Map.entry("Column align", "Column align: right"),
            // Figure
            Map.entry("Caption position", "Caption position: top"),
            Map.entry("Figure counter", "Figure counter: tables"),
            // Table of contents
            Map.entry("Entries", "Entries: figures"),
            Map.entry("Levels", "Levels: 1 2"),
            Map.entry("Leader", "Leader: line"),
            // Image
            Map.entry("Alt", "Alt: Logotipo"),
            Map.entry("Fit", "Fit: crop"),
            Map.entry("PDF page", "PDF page: 2"),
            // Barcode
            Map.entry("Barcode format", "Barcode format: qr"),
            Map.entry("Error correction", "Error correction: high"),
            Map.entry("Quiet zone", "Quiet zone: 5"),
            // Styles
            Map.entry("Named color", "Named color: brand #12315A"));

    /** Header of all the documents; it ends inside {@code Styles}, to add rules. */
    private static final String HEADER = Docs.header(595, 842) + Docs.TYPOGRAPHY
            + "\t\tFor style: c\n\t\t\tTypography:\n\t\t\t\tColor: #000000\n";

    /**
     * Minimal valid document of each node; the line {@code {}} is where the property goes.
     *
     * @param present children that the template already declares (required, or needed for the
     *                rest to be valid): they are tested with the template as is
     */
    private record Template(String document, Set<String> present) {
        Template(String document) {
            this(document, Set.of());
        }
    }

    private static final Map<String, Template> TEMPLATES = templates();

    private static Map<String, Template> templates() {
        Map<String, Template> t = new LinkedHashMap<>();
        t.put("Document", new Template(HEADER + "\t{}\n\tSection:\n\t\tBlock:", Set.of("Title", "Language")));
        // A section: what it allows besides its content. First number needs restart.
        t.put("Section", new Template(HEADER + "\tSection:\n\t\tPage numbering: restart\n\t\t{}\n\t\tBlock:",
                Set.of("Page numbering")));
        // Paper excludes Width and Height, which all the documents read (the header).
        t.put("Page", new Template("Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tPaper: A4\n\t\t{}\n\tSection:\n\t\tBlock:",
                Set.of("Paper", "Width", "Height")));
        for (String element : List.of("Block", "Frame", "Overlay")) {
            t.put(element, new Template(HEADER + "\t" + element + ":\n\t\t{}"));
        }
        // A section with only a Page overlay has no content.
        t.put("Page overlay", new Template(HEADER + "\tBlock:\n\tPage overlay:\n\t\t{}"));
        // Line distribution needs Wrap.
        t.put("Stack", new Template(HEADER + "\tStack:\n\t\tWrap: true\n\t\t{}\n\t\tItem:\n\t\t\tBlock:", Set.of("Wrap")));
        t.put("Grid", new Template(HEADER + "\tGrid:\n\t\t{}\n\t\tArea:\n\t\t\tBlock:"));
        for (String text : List.of("Text", "Paragraph")) {
            t.put(text, new Template(HEADER + "\t" + text + ":\n\t\t{}\n\t\tContent >>\n\t\t\tHola", Set.of("Content")));
        }
        // Each heading, after the previous levels (without skipping levels).
        StringBuilder previous = new StringBuilder();
        for (int level = 1; level <= 6; level++) {
            t.put("Heading " + level, new Template(HEADER + previous + "\tHeading " + level + ":\n\t\t{}\n\t\tText: B"));
            previous.append("\tHeading ").append(level).append(":\n\t\tText: A\n");
        }
        t.put("List", new Template(HEADER + "\tList:\n\t\tMarker: decimal\n\t\t{}\n\t\tList item:\n\t\t\tParagraph: Uno",
                Set.of("Marker")));
        t.put("List item", new Template(HEADER + "\tList:\n\t\tMarker: decimal\n\t\tList item:\n\t\t\t{}\n"
                + "\t\t\tParagraph: Uno"));
        // The children of a Marker image (Alt, Height), in a list with Marker: image.
        t.put("Marker image", new Template(HEADER + "\tList:\n\t\tMarker: image\n\t\tMarker image: test/square.png\n"
                + "\t\t\t{}\n\t\tList item:\n\t\t\tParagraph: Uno"));
        // Separator needs a Gap that contains it, and Separator color, a Separator.
        t.put("Columns", new Template(HEADER + "\tColumns:\n\t\tColumn count: 2\n\t\tGap: 10\n\t\tSeparator: 1\n"
                + "\t\t{}\n\t\tText: Hola", Set.of("Column count", "Gap", "Separator")));
        t.put("Markdown", new Template(HEADER + "\tMarkdown:\n\t\t{}\n\t\tContent >>\n\t\t\tHola",
                Set.of("Content")));
        // A footnote needs its call, and its Id is the name of the call.
        t.put("Footnote", new Template(HEADER + "\tParagraph: Hola[^n]\n\t\tTypography:\n\t\t\tMarkup: markdown\n"
                + "\tFootnote:\n\t\tId: n\n\t\t{}\n\t\tParagraph: Nota", Set.of("Id")));
        // A table of contents needs some heading to list, or some figure with Entries: figures.
        t.put("Table of contents", new Template(HEADER + "\tHeading 1:\n\t\tText: A\n\tFigure:\n\t\tCaption: C\n"
                + "\t\tImage: test/wide.png\n\t\t\tAlt: A\n\tTable of contents:\n\t\t{}"));
        // A figure with a Block carries its own Alt; with an image, the Alt would have no effect.
        t.put("Figure", new Template(HEADER + "\tFigure:\n\t\tAlt: Conjunto\n\t\t{}\n\t\tCaption: Pie\n\t\tBlock:\n"
                + "\t\t\tText: Hola", Set.of("Alt")));
        t.put("Caption", new Template(HEADER + "\tFigure:\n\t\tAlt: Conjunto\n\t\tBlock:\n\t\t\tText: Hola\n\t\tCaption:\n"
                + "\t\t\t{}\n\t\t\tContent >>\n\t\t\t\tPie", Set.of("Content")));
        t.put("Image", new Template(HEADER + "\tImage: test/wide.png\n\t\t{}"));
        t.put("Barcode", new Template(HEADER + "\tBarcode: https://example.com/a\n\t\tAlt: Enlace\n\t\t{}",
                Set.of("Alt")));
        // The data goes in the value or in Content, not in both.
        t.put("Barcode > Content", new Template(HEADER + "\tBarcode:\n\t\tAlt: Datos\n\t\t{}", Set.of("Alt")));
        t.put("Item", new Template(HEADER + "\tStack:\n\t\tItem:\n\t\t\t{}\n\t\t\tBlock:"));
        // An item is only aligned on the cross axis: Horizontal, in a vertical Stack.
        t.put("Item > Horizontal", new Template(HEADER + "\tStack:\n\t\tDirection: vertical\n\t\tItem:\n\t\t\t{}\n"
                + "\t\t\tBlock:"));
        t.put("Area", new Template(HEADER + "\tGrid:\n\t\tArea:\n\t\t\t{}\n\t\t\tBlock:"));
        String table = HEADER + "\tTable:\n\t\tColumn sizes: 100\n";
        t.put("Table", new Template(table + "\t\t{}\n\t\tBody rows:\n\t\t\tRow:\n\t\t\t\tCell: Hola", Set.of("Column sizes")));
        t.put("Row", new Template(table + "\t\tBody rows:\n\t\t\tRow:\n\t\t\t\t{}\n\t\t\t\tCell: Hola"));
        t.put("Cell", new Template(table + "\t\tBody rows:\n\t\t\tRow:\n\t\t\t\tCell:\n\t\t\t\t\t{}"));
        // The rules and the named colors go inside the Styles of the header.
        t.put("Styles", new Template(HEADER + "\t\t{}\n\tBlock:"));
        t.put("For element", new Template(HEADER + "\t\tFor element: Block\n\t\t\t{}\n\tBlock:"));
        t.put("For style", new Template(HEADER + "\t\tFor style: d\n\t\t\t{}\n\tBlock:"));
        t.put("For id", new Template(HEADER + "\t\tFor id: r\n\t\t\t{}\n\tBlock:\n\t\tId: r"));
        t.put("For column", new Template(HEADER + "\t\tFor column: 1\n\t\t\t{}\n\tBlock:"));
        t.put("Box", new Template(HEADER + "\tBlock:\n\t\tBox:\n\t\t\t{}"));
        // Merge borders written in the Box of an element only has an effect on a Table.
        t.put("Box > Merge borders", new Template(HEADER + "\tTable:\n\t\tColumn sizes: *\n\t\tBox:\n\t\t\t{}\n"
                + "\t\tBody rows:\n\t\t\tRow:\n\t\t\t\tCell: a"));
        t.put("Typography", new Template(HEADER + "\tText: Hola\n\t\tTypography:\n\t\t\t{}"));
        t.put("Break", new Template(HEADER + "\tBlock:\n\t\tBreak:\n\t\t\t{}"));
        // Below side boxes needs a side box before the element: the template of each element, with one in front.
        String side = "\tImage: test/square.png\n\t\tSide: left\n";
        for (String element : List.of("Block", "Text", "Paragraph", "Heading 1", "Heading 2", "Heading 3", "Heading 4",
                "Heading 5", "Heading 6", "Markdown", "Image", "Barcode", "Figure")) {
            Template base = t.get(element);
            t.put(element + " > Below side boxes", new Template(HEADER + side + base.document().substring(HEADER.length()),
                    base.present()));
        }
        return t;
    }

    @TestFactory
    Stream<DynamicTest> everyPropertyOfTheSchemaIsRead() {
        DocumentReader reader = new DocumentReader();
        return pairs().stream().map(pair -> DynamicTest.dynamicTest(pair.parent() + " > " + pair.child(), () -> {
            Template template = TEMPLATES.getOrDefault(pair.parent() + " > " + pair.child(),
                    TEMPLATES.get(pair.parent()));
            assertNotNull(template, () -> "SchemaCoverageTest needs a template for " + pair.parent());
            String sample = SAMPLES.get(pair.child());
            assertNotNull(sample, () -> "SchemaCoverageTest needs a sample value for " + pair.child());
            String document = RULES.contains(pair.parent()) && !GROUPS.contains(pair.child())
                    ? ruleDocument(pair.parent(), pair.child(), sample)
                    : document(template, pair.child(), sample);
            assertDoesNotThrow(() -> new BoxTreeBuilder().build(reader.read(Docs.sectioned(document))),
                    () -> pair.parent() + " > " + pair.child() + " in:\n" + document);
        }));
    }

    /** A node of the schema and a property or group that it allows. */
    private record Pair(String parent, String child) {
    }

    /** (node, child) pairs of the schema whose child is a property (a node without children) or a group. */
    private static List<Pair> pairs() {
        Map<String, List<String>> children = SchemaNodes.children();
        List<Pair> pairs = new ArrayList<>();
        children.forEach((parent, kids) -> kids.stream()
                .filter(kid -> GROUPS.contains(kid) || children.getOrDefault(kid, List.of()).isEmpty())
                .forEach(kid -> pairs.add(new Pair(parent, kid))));
        return pairs;
    }

    /** The rules that give element properties besides the groups (reference/common/styles.md). */
    private static final Set<String> RULES = Set.of("For element", "For style", "For id");

    /** The element properties of a rule that are of the {@code Item} that wraps the element. */
    private static final Set<String> ITEM_PROPERTIES = Set.of("Grow", "Shrink", "Base size");

    /**
     * A document with the element property {@code child} in a rule {@code rule} that applies to the first element of
     * {@link #TEMPLATES} that can declare it: the rule goes in the {@code Styles} of the header and, on the line
     * {@code {}}, the element says which rule applies to it ({@code Style: d}, {@code Id: r}, nothing for
     * {@code For element}). {@code Grow}, {@code Shrink} and {@code Base size} go to the {@code Block} of an
     * {@code Item}.
     */
    private static String ruleDocument(String rule, String child, String sample) {
        Map<String, List<String>> children = SchemaNodes.children();
        String element = "Block";
        String body = "\tStack:\n\t\tItem:\n\t\t\tBlock:\n\t\t\t\t{}";
        if (!ITEM_PROPERTIES.contains(child)) {
            element = TEMPLATES.keySet().stream()
                    // Not a Section, which declares Trim margins but takes no element property from a rule.
                    .filter(e -> !e.contains(" > ") && !e.equals("Item") && !e.equals("Area") && !e.equals("Section"))
                    .filter(e -> children.getOrDefault(e, List.of()).contains(child))
                    .findFirst().orElseThrow(() -> new AssertionError("No element template declares " + child));
            body = TEMPLATES.getOrDefault(element + " > " + child, TEMPLATES.get(element)).document()
                    .substring(HEADER.length());
        }
        String mark = switch (rule) {
            case "For element" -> null;
            case "For style" -> "Style: d";
            default -> "Id: r";
        };
        StringBuilder document = new StringBuilder(HEADER).append("\t\t").append(rule).append(": ")
                .append(mark == null ? element : mark.substring(mark.indexOf(' ') + 1)).append('\n');
        for (String s : sample.split("\n")) {
            document.append("\t\t\t").append(s).append('\n');
        }
        for (String line : body.split("\n")) {
            int marker = line.indexOf("{}");
            if (marker < 0) {
                document.append(line).append('\n');
            } else if (mark != null) {
                document.append(line, 0, marker).append(mark).append('\n');
            }
        }
        return document.toString();
    }

    /** The template with the property on the line {@code {}}, with its indent; without it if it already declares it. */
    private static String document(Template template, String child, String sample) {
        List<String> lines = new ArrayList<>();
        for (String line : template.document().split("\n")) {
            int marker = line.indexOf("{}");
            if (marker < 0) {
                lines.add(line);
            } else if (!template.present().contains(child)) {
                String indent = line.substring(0, marker);
                for (String s : sample.split("\n")) {
                    lines.add(indent + s);
                }
            }
        }
        return String.join("\n", lines);
    }
}
