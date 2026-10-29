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
 * El contrato de {@link Engine}: el protocolo con el {@link Paginator} no es
 * público, y un motor guardado entre conversiones no retiene nada del último documento ni cambia el resultado
 * del siguiente.
 */
class EngineContractTest {

    private static final Path EXAMPLES = Path.of("test", "examples");

    @Test
    void thePaginationProtocolIsNotPublic() {
        Set<String> publicMethods = Arrays.stream(Engine.class.getMethods()).map(Method::getName)
                .collect(Collectors.toSet());
        for (String internal : List.of("reservePageNumbers", "assume", "number", "absolute")) {
            assertFalse(publicMethods.contains(internal), internal + " es del Paginator y no debe ser público");
        }
        // Lo que sí es de quien usa el motor.
        assertTrue(publicMethods.containsAll(Set.of("layOutDocument", "layout", "intrinsic", "measurer")));
    }

    @Test
    void layingOutAnEngineReleasesItsCache() throws IOException {
        Path dir = firstExample();
        InlineNode document = new DocumentReader().read(dir.resolve("input.stxt"));
        Resources resources = new Resources();
        Engine engine = new Engine(new PdfMetrics(resources), resources);
        BoxTreeBuilder.Result tree = new BoxTreeBuilder().build(document, styles(dir));
        // Se llena la caché por fuera de layOutDocument, para ver que éste la vacía.
        engine.intrinsic(tree.root());
        assertTrue(engine.cachedEntries() > 0, "la prueba necesita una caché con algo dentro");
        engine.layOutDocument(tree);
        assertEquals(0, engine.cachedEntries());
    }

    /** Un motor guardado y reutilizado da lo mismo que uno nuevo: con {@code {pages}}, referencias e índices. */
    @Test
    void aReusedEngineGivesTheSameDocumentsAsFreshOnes() throws IOException {
        DocumentReader reader = new DocumentReader();
        Resources resources = new Resources();
        Engine reused = new Engine(new PdfMetrics(resources), resources);
        List<Path> dirs;
        try (Stream<Path> list = Files.list(EXAMPLES)) {
            dirs = list.filter(Files::isDirectory).sorted().toList();
        }
        assertFalse(dirs.isEmpty());
        // Dos vueltas, para que cada documento también siga a otro distinto del que lo precedió.
        for (int round = 0; round < 2; round++) {
            for (Path dir : dirs) {
                InlineNode document = reader.read(dir.resolve("input.stxt"));
                InlineNode styles = styles(dir);
                String fresh = StxtDump.dump(new Engine(new PdfMetrics(resources), resources)
                        .layOutDocument(document, styles).pages(), true);
                String again = StxtDump.dump(reused.layOutDocument(document, styles).pages(), true);
                assertEquals(fresh, again, dir.getFileName() + " (vuelta " + (round + 1) + ")");
            }
        }
    }

    private static Path firstExample() throws IOException {
        try (Stream<Path> list = Files.list(EXAMPLES)) {
            return list.filter(Files::isDirectory).sorted().findFirst().orElseThrow();
        }
    }

    private static InlineNode styles(Path dir) {
        Path file = dir.resolve("styles.stxt");
        return Files.exists(file) ? new DocumentReader().readStyles(file) : null;
    }

    /**
     * Un motor reutilizado olvida las cifras que reservó el documento anterior: tras uno de 101 páginas, «Página
     * {page}» se vuelve a medir con dos cifras (45) y cabe en una línea de su caja de 48 (pruebas de mutación).
     */
    @Test
    void aReusedEngineForgetsTheReservedDigits() {
        String head = "Document (dev.stxt.pdf):\n\tMedia:\n\t\tPaper: A4\n\t\tMargin: 40\n\tTitle: Cifras\n"
                + "\tLanguage: es\n\tStyles:\n\t\tElement: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n"
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
