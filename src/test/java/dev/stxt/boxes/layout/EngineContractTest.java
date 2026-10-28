package dev.stxt.boxes.layout;

import dev.stxt.InlineNode;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.output.StxtDump;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.boxes.tree.BoxTreeBuilder;
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
 * El contrato de {@link Engine} (ROADMAP-QA-ARQ.md, TS-01): el protocolo con el {@link Paginator} no es
 * público, y un motor guardado entre conversiones no retiene nada del último documento ni cambia el resultado
 * del siguiente.
 */
class EngineContractTest {

    private static final Path EXAMPLES = Path.of("test", "examples");

    @Test
    void thePaginationProtocolIsNotPublic() {
        Set<String> publicMethods = Arrays.stream(Engine.class.getMethods()).map(Method::getName)
                .collect(Collectors.toSet());
        for (String internal : List.of("reservePageDigits", "assume", "number", "absolute")) {
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
}
