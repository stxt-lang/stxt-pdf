package dev.stxt.boxes.golden;

import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.output.StxtDump;
import dev.stxt.boxes.stxt.MinimalStxtReader;
import dev.stxt.boxes.stxt.StxtNode;
import dev.stxt.boxes.tree.BoxTreeBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de referencia (ESPECIFICACION.md §7): cada carpeta de {@code golden/} contiene
 * {@code input.stxt} y {@code expected.stxt}. Se comparan nombres y valores; los números de
 * {@code Rect} con una tolerancia de 0.01 pt.
 */
@Disabled("Activar al completar el hito 2 (BlockLayout) y el hito 4 (FlexLayout)")
class GoldenTest {

    private static final double TOLERANCE = 0.01;
    private final MinimalStxtReader reader = new MinimalStxtReader();

    @TestFactory
    Stream<DynamicTest> cases() throws IOException, URISyntaxException {
        Path root = Path.of(getClass().getResource("/golden").toURI());
        try (Stream<Path> dirs = Files.list(root)) {
            List<Path> cases = dirs.filter(Files::isDirectory).sorted().toList();
            return cases.stream().map(dir -> DynamicTest.dynamicTest(dir.getFileName().toString(), () -> {
                StxtNode input = reader.read(dir.resolve("input.stxt"));
                StxtNode expected = reader.read(dir.resolve("expected.stxt"));
                List<Page> pages = new Engine().layOutDocument(input);
                var media = new BoxTreeBuilder().build(input).media();
                String dump = StxtDump.dump(pages, media.pageArea(), false);
                compare(expected, reader.read(dump, "dump"), dir.getFileName() + "/");
            }));
        }
    }

    private static void compare(StxtNode expected, StxtNode actual, String path) {
        String here = path + expected.name();
        assertEquals(StxtNode.canonical(expected.name()), StxtNode.canonical(actual.name()), here);
        if (expected.is("Rect")) {
            double[] e = numbers(expected.content());
            double[] a = numbers(actual.content());
            assertEquals(4, a.length, here);
            for (int i = 0; i < 4; i++) {
                assertEquals(e[i], a[i], TOLERANCE, here + " (componente " + i + ")");
            }
        } else {
            assertEquals(expected.content().trim(), actual.content().trim(), here);
        }
        assertEquals(expected.children().size(), actual.children().size(), here + ": número de hijos");
        for (int i = 0; i < expected.children().size(); i++) {
            compare(expected.children().get(i), actual.children().get(i), here + "/");
        }
    }

    private static double[] numbers(String s) {
        return Stream.of(s.trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
    }
}
