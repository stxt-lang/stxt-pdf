package dev.stxt.boxes.golden;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.output.StxtDump;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.boxes.tree.BoxTreeBuilder;
import dev.stxt.runtime.STXT;
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
 * {@code Rect} con una tolerancia de 0.01 pt. La entrada se valida contra el template; el
 * volcado no tiene namespace y se lee sin validación.
 */
@Disabled("Activar al completar el hito 2 (BlockLayout) y el hito 4 (FlexLayout)")
class GoldenTest {

    private static final double TOLERANCE = 0.01;
    private final DocumentReader reader = new DocumentReader();

    @TestFactory
    Stream<DynamicTest> cases() throws IOException, URISyntaxException {
        Path root = Path.of(getClass().getResource("/golden").toURI());
        try (Stream<Path> dirs = Files.list(root)) {
            List<Path> cases = dirs.filter(Files::isDirectory).sorted().toList();
            return cases.stream().map(dir -> DynamicTest.dynamicTest(dir.getFileName().toString(), () -> {
                InlineNode input = reader.read(dir.resolve("input.stxt"));
                Node expected = STXT.rawParser().parseFile(dir.resolve("expected.stxt").toFile()).get(0);
                List<Page> pages = new Engine().layOutDocument(input);
                var media = new BoxTreeBuilder().build(input).media();
                String dump = StxtDump.dump(pages, media.pageArea(), false);
                compare(expected, STXT.rawParser().parse(dump).get(0), dir.getFileName() + "/");
            }));
        }
    }

    private static void compare(Node expected, Node actual, String path) {
        String here = path + expected.getName();
        assertEquals(expected.getCanonicalName(), actual.getCanonicalName(), here);
        if (expected.getCanonicalName().equals("rect")) {
            double[] e = numbers(expected.getText());
            double[] a = numbers(actual.getText());
            assertEquals(4, a.length, here);
            for (int i = 0; i < 4; i++) {
                assertEquals(e[i], a[i], TOLERANCE, here + " (componente " + i + ")");
            }
        } else {
            assertEquals(expected.getText().trim(), actual.getText().trim(), here);
        }
        List<Node> e = children(expected);
        List<Node> a = children(actual);
        assertEquals(e.size(), a.size(), here + ": número de hijos");
        for (int i = 0; i < e.size(); i++) {
            compare(e.get(i), a.get(i), here + "/");
        }
    }

    private static List<Node> children(Node n) {
        return n instanceof InlineNode i ? i.getChildren() : List.of();
    }

    private static double[] numbers(String s) {
        return Stream.of(s.trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
    }
}
