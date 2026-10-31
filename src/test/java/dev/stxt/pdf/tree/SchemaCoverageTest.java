package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
            Map.entry("Language", "Language: es"),
            // Section
            Map.entry("Page numbering", "Page numbering: restart"),
            Map.entry("Page number format", "Page number format: lower-roman"),
            Map.entry("Inherit page overlays", "Inherit page overlays: false"),
            Map.entry("Begin on", "Begin on: any"),
            // Elements
            Map.entry("Id", "Id: x"),
            Map.entry("Style", "Style: c"),
            Map.entry("Link", "Link: https://example.com/a"),
            Map.entry("In table of contents", "In table of contents: false"),
            Map.entry("Isolate margins", "Isolate margins: true"),
            Map.entry("Offset", "Offset: 10 auto auto 10"),
            Map.entry("Layer", "Layer: 1"),
            Map.entry("Pages", "Pages: not-first"),
            Map.entry("Rotate", "Rotate: -8"),
            Map.entry("Rotate origin", "Rotate: 15\nRotate origin: top left"),
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
            // Typography
            Map.entry("Font", "Font: IBM Plex Sans"),
            Map.entry("Bold", "Bold: true"),
            Map.entry("Italic", "Italic: true"),
            Map.entry("Size", "Size: 12"),
            Map.entry("Line height", "Line height: 14"),
            Map.entry("Align", "Align: center"),
            Map.entry("Color", "Color: #333333"),
            Map.entry("Line breaking", "Line breaking: balanced"),
            Map.entry("Markup", "Markup: markdown"),
            Map.entry("Case", "Case: uppercase"),
            Map.entry("Letter spacing", "Letter spacing: 1.5"),
            Map.entry("Small caps", "Small caps: true"),
            Map.entry("Ligatures", "Ligatures: true"),
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
            Map.entry("Gap", "Gap: 5"),
            Map.entry("Line distribution", "Line distribution: center"),
            Map.entry("Grow", "Grow: 1"),
            Map.entry("Shrink", "Shrink: 0"),
            Map.entry("Base size", "Base size: 50"),
            // Grid and Area
            Map.entry("Column sizes", "Column sizes: 100"),
            Map.entry("Row sizes", "Row sizes: 20"),
            Map.entry("Areas", "Areas >>\n\ta"),
            Map.entry("Flow", "Flow: vertical"),
            Map.entry("Horizontal", "Horizontal: center"),
            Map.entry("Vertical", "Vertical: center"),
            // Lists
            Map.entry("Marker", "Marker: decimal"),
            Map.entry("Start", "Start: 3"),
            Map.entry("Number", "Number: 5"),
            // Columns
            Map.entry("Count", "Count: 3"),
            Map.entry("Min column width", "Min column width: 100"),
            Map.entry("Separator", "Separator: 1"),
            Map.entry("Separator color", "Separator color: #999999"),
            Map.entry("Balance", "Balance: false"),
            // Markdown
            Map.entry("Bullet markers", "Bullet markers: bullet dash"),
            Map.entry("Number markers", "Number markers: decimal lower-alpha"),
            Map.entry("Table columns", "Table columns: auto"),
            Map.entry("Column align", "Column align: right"),
            // Table of contents
            Map.entry("Levels", "Levels: 1 2"),
            Map.entry("Leader", "Leader: line"),
            // Image
            Map.entry("Alt", "Alt: Logotipo"),
            Map.entry("Fit", "Fit: fill"),
            Map.entry("Pdf page", "Pdf page: 2"));

    /** Header of all the documents; it ends inside {@code Styles}, to add rules. */
    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
            + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
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
        // A section: what it allows besides its content. Start needs restart.
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
        // Separator needs a Gap that contains it, and Separator color, a Separator.
        t.put("Columns", new Template(HEADER + "\tColumns:\n\t\tCount: 2\n\t\tGap: 10\n\t\tSeparator: 1\n"
                + "\t\t{}\n\t\tText: Hola", Set.of("Count", "Gap", "Separator")));
        t.put("Markdown", new Template(HEADER + "\tMarkdown:\n\t\t{}\n\t\tContent >>\n\t\t\tHola",
                Set.of("Content")));
        // A table of contents needs some heading to list.
        t.put("Table of contents", new Template(HEADER + "\tHeading 1:\n\t\tText: A\n\tTable of contents:\n\t\t{}"));
        t.put("Image", new Template(HEADER + "\tImage: test/wide.png\n\t\t{}"));
        t.put("Item", new Template(HEADER + "\tStack:\n\t\tItem:\n\t\t\t{}\n\t\t\tBlock:"));
        // An item is only aligned on the cross axis: Horizontal, in a vertical Stack.
        t.put("Item > Horizontal", new Template(HEADER + "\tStack:\n\t\tDirection: vertical\n\t\tItem:\n\t\t\t{}\n"
                + "\t\t\tBlock:"));
        t.put("Area", new Template(HEADER + "\tGrid:\n\t\tArea:\n\t\t\t{}\n\t\t\tBlock:"));
        String table = HEADER + "\tTable:\n\t\tColumn sizes: 100\n";
        t.put("Table", new Template(table + "\t\t{}\n\t\tBody:\n\t\t\tRow:\n\t\t\t\tCell: Hola", Set.of("Column sizes")));
        t.put("Row", new Template(table + "\t\tBody:\n\t\t\tRow:\n\t\t\t\t{}\n\t\t\t\tCell: Hola"));
        t.put("Cell", new Template(table + "\t\tBody:\n\t\t\tRow:\n\t\t\t\tCell:\n\t\t\t\t\t{}"));
        // The rules go inside the Styles of the header.
        t.put("For element", new Template(HEADER + "\t\tFor element: Block\n\t\t\t{}\n\tBlock:"));
        t.put("For style", new Template(HEADER + "\t\tFor style: d\n\t\t\t{}\n\tBlock:"));
        t.put("For id", new Template(HEADER + "\t\tFor id: r\n\t\t\t{}\n\tBlock:\n\t\tId: r"));
        t.put("For column", new Template(HEADER + "\t\tFor column: 1\n\t\t\t{}\n\tBlock:"));
        t.put("Box", new Template(HEADER + "\tBlock:\n\t\tBox:\n\t\t\t{}"));
        t.put("Typography", new Template(HEADER + "\tText: Hola\n\t\tTypography:\n\t\t\t{}"));
        t.put("Break", new Template(HEADER + "\tBlock:\n\t\tBreak:\n\t\t\t{}"));
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
            String document = document(template, pair.child(), sample);
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
