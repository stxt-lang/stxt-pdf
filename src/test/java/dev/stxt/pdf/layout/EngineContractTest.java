package dev.stxt.pdf.layout;

import dev.stxt.InlineNode;
import dev.stxt.pdf.layout.inline.PdfMetrics;
import dev.stxt.pdf.output.StxtDump;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.BoxTreeBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The contract of {@link Engine}: the protocol with the {@link Paginator} is not
 * public, and an engine kept between conversions retains nothing of the last document and does not change the result
 * of the next one.
 */
class EngineContractTest {

    private static final Path EXAMPLES = Path.of("test", "examples");

    @Test
    void thePaginationProtocolIsNotPublic() {
        Set<String> publicMethods = Arrays.stream(Engine.class.getMethods()).map(Method::getName)
                .collect(Collectors.toSet());
        for (String internal : List.of("reservePageNumbers", "assume", "number", "absolute")) {
            assertFalse(publicMethods.contains(internal), internal + " belongs to the Paginator and must not be public");
        }
        // What does belong to the caller.
        assertTrue(publicMethods.containsAll(Set.of("layOutDocument", "layout", "intrinsic", "measurer")));
    }

    @Test
    void layingOutAnEngineReleasesItsCache() throws IOException {
        Path dir = exampleDocuments().get(0);
        InlineNode document = new DocumentReader().read(dir.resolve("input.stxt"));
        Resources resources = new Resources();
        Engine engine = new Engine(new PdfMetrics(resources), resources);
        BoxTreeBuilder.Result tree = new BoxTreeBuilder().build(document, styles(dir));
        // The cache is filled outside layOutDocument, to check that the latter empties it.
        engine.intrinsic(tree.root());
        assertTrue(engine.cachedEntries() > 0, "the test needs a cache with something in it");
        engine.layOutDocument(tree);
        assertEquals(0, engine.cachedEntries());
    }

    /**
     * An engine kept and reused gives the same as a new one: with {@code {pages}}, references and tables of contents.
     */
    @Test
    void aReusedEngineGivesTheSameDocumentsAsFreshOnes() throws IOException {
        DocumentReader reader = new DocumentReader();
        Resources resources = new Resources();
        Engine reused = new Engine(new PdfMetrics(resources), resources);
        List<Path> dirs = exampleDocuments();
        assertFalse(dirs.isEmpty());
        // Two rounds, so that each document also follows a different one from the one that preceded it.
        for (int round = 0; round < 2; round++) {
            for (Path dir : dirs) {
                InlineNode document = reader.read(dir.resolve("input.stxt"));
                InlineNode styles = styles(dir);
                String fresh = StxtDump.dump(new Engine(new PdfMetrics(resources), resources)
                        .layOutDocument(document, styles).pages(), true);
                String again = StxtDump.dump(reused.layOutDocument(document, styles).pages(), true);
                assertEquals(fresh, again, EXAMPLES.relativize(dir) + " (round " + (round + 1) + ")");
            }
        }
    }

    /** The folders of the documents of the examples, one per example and language ({@code 01-invoice/en}), in order. */
    private static List<Path> exampleDocuments() throws IOException {
        try (Stream<Path> found = Files.find(EXAMPLES, 3, (p, a) -> p.getFileName().toString().equals("input.stxt"))) {
            return found.map(Path::getParent).sorted().toList();
        }
    }

    /** The separate styles of an example, shared by its languages: in the folder of the example. */
    private static InlineNode styles(Path dir) {
        Path file = dir.getParent().resolve("styles.stxt");
        return Files.exists(file) ? new DocumentReader().readStyles(file) : null;
    }

    /**
     * A reused engine forgets the figures that the previous document reserved: after one of 101 pages, "Página
     * {page}" is measured again with two figures (45) and fits on one line of its box of 48 (mutation testing).
     */
    @Test
    void aReusedEngineForgetsTheReservedDigits() {
        String head = "Document (dev.stxt.pdf):\n\tPage:\n\t\tPaper: A4\n\t\tMargin: 40\n\tTitle: Cifras\n"
                + "\tLanguage: es\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n"
                + "\t\t\t\tSize: 10\n\tSection:\n\t\tText: Página {page}\n\t\t\tId: P\n\t\t\tBox:\n\t\t\t\tWidth: 48\n";
        StringBuilder big = new StringBuilder(head);
        for (int i = 0; i < 100; i++) {
            big.append("\t\tBlock:\n\t\t\tBox:\n\t\t\t\tHeight: 10\n\t\t\tBreak:\n\t\t\t\tBefore: page\n");
        }
        DocumentReader reader = new DocumentReader();
        Engine engine = new Engine();
        assertEquals(101, engine.layOutDocument(reader.read(big.toString())).pages().size());
        LaidOutDocument small = engine.layOutDocument(reader.read(head));
        assertEquals(1, small.pages().get(0).content().children().get(0).lines().size());
    }
}
