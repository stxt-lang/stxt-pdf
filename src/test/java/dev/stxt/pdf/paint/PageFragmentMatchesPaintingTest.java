package dev.stxt.pdf.paint;

import dev.stxt.InlineNode;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.fragment.PageFragment;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PageFragment} says where each fragment is with its own walk through the page, and the painting with that of
 * {@link StackingContext}: the two have to agree, or whoever draws over a box with what {@code PageFragment} says would
 * draw somewhere else. Every golden case ({@code test/golden}: each layout algorithm, the overlays, the rotations, the
 * footnotes) is laid out and, page by page, each fragment of the flow and of the footnote area has to have the same
 * border box and the same enclosing rectangle in both.
 */
class PageFragmentMatchesPaintingTest {

    private static final Path GOLDEN = Path.of("test", "golden");
    private static final double TOLERANCE = 1e-6;

    private final DocumentReader reader = new DocumentReader();

    /** A fragment with where it is: its border box, unrotated, and the rectangle that encloses it as painted. */
    private record Seen(Fragment fragment, Rect borderBox, Rect bounds) {
    }

    @TestFactory
    Stream<DynamicTest> everyGoldenCase() throws IOException {
        try (Stream<Path> dirs = Files.list(GOLDEN)) {
            List<Path> cases = dirs.filter(Files::isDirectory).sorted().toList();
            assertTrue(cases.size() > 40, "The golden cases were not found in " + GOLDEN);
            return cases.stream().map(dir -> DynamicTest.dynamicTest(dir.getFileName().toString(), () -> check(dir)));
        }
    }

    private void check(Path dir) throws IOException {
        InlineNode input = reader.read(dir.resolve("input.stxt"));
        Path stylesFile = dir.resolve("styles.stxt");
        InlineNode styles = Files.exists(stylesFile) ? reader.readStyles(stylesFile) : null;
        LaidOutDocument document = new Engine().layOutDocument(input, styles);
        for (Page page : document.pages()) {
            List<Seen> painted = new ArrayList<>();
            Rect area = page.pageSetup().pageArea();
            StackingContext.build(page.content(), page.footnotes(), List.of(), area.x(), area.y())
                    .visit((f, borderBox, turns) -> painted.add(new Seen(f, borderBox, turns.envelope(borderBox))));

            List<Seen> asked = new ArrayList<>();
            // The flow and the footnote area: the painting does not walk through the Page overlay boxes of the page.
            List<PageFragment> roots = page.fragments();
            for (PageFragment root : roots.subList(0, page.footnotes() != null ? 2 : 1)) {
                collect(root, asked);
            }

            String where = dir.getFileName() + ", page " + page.number();
            assertEquals(painted.size(), asked.size(), where);
            for (int i = 0; i < painted.size(); i++) {
                Seen expected = painted.get(i);
                Seen actual = asked.get(i);
                String which = where + ", fragment " + i + " (" + expected.fragment().box().describe() + ")";
                assertSame(expected.fragment(), actual.fragment(), which);
                assertRect(expected.borderBox(), actual.borderBox(), which + ", border box");
                assertRect(expected.bounds(), actual.bounds(), which + ", bounds");
            }
        }
    }

    /** Document order: an ancestor before its descendants. */
    private static void collect(PageFragment node, List<Seen> seen) {
        seen.add(new Seen(node.fragment(), node.borderBox(), node.bounds()));
        for (PageFragment child : node.children()) {
            collect(child, seen);
        }
    }

    private static void assertRect(Rect expected, Rect actual, String which) {
        assertEquals(expected.x(), actual.x(), TOLERANCE, which);
        assertEquals(expected.y(), actual.y(), TOLERANCE, which);
        assertEquals(expected.width(), actual.width(), TOLERANCE, which);
        assertEquals(expected.height(), actual.height(), TOLERANCE, which);
    }
}
