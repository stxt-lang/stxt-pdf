package dev.stxt.boxes;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dependencias entre los packages de {@code dev.stxt.boxes}, leídas del código fuente: los
 * {@code import} y los nombres cualificados fuera de los comentarios. Cada package cuenta con sus
 * subpackages ({@code layout.grid} es {@code layout}).
 *
 * <p>El flujo de datos va en un sentido (lectura → árbol → layout → páginas → pintado): un ciclo
 * o un árbol que depende del layout es un error de diseño, no de un documento.</p>
 */
class ArchitectureTest {

    private static final Path SOURCES = Path.of("src", "main", "java", "dev", "stxt", "boxes");
    private static final Pattern REFERENCE = Pattern.compile("\\bdev\\.stxt\\.boxes\\.([a-z]+)\\.");
    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);
    private static final Pattern STRING = Pattern.compile("\"(?:\\\\.|[^\"\\\\\\n])*\"");

    /** Package → packages de los que depende. */
    private static Map<String, Set<String>> dependencies() {
        Map<String, Set<String>> graph = new TreeMap<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String from = SOURCES.relativize(file).getName(0).toString();
                Set<String> to = graph.computeIfAbsent(from, k -> new TreeSet<>());
                String code = STRING.matcher(COMMENT.matcher(Files.readString(file)).replaceAll(" ")).replaceAll("\"\"");
                Matcher m = REFERENCE.matcher(code);
                while (m.find()) {
                    if (!m.group(1).equals(from)) {
                        to.add(m.group(1));
                    }
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return graph;
    }

    @Test
    void readsEveryPackage() {
        assertEquals(Set.of("convert", "fragment", "layout", "model", "output", "paint", "resources", "stxt", "tree"),
                dependencies().keySet());
    }

    @Test
    void noCyclesBetweenPackages() {
        Map<String, Set<String>> graph = dependencies();
        for (String start : graph.keySet()) {
            List<String> cycle = cycle(graph, start, new ArrayList<>(List.of(start)));
            assertTrue(cycle.isEmpty(), () -> "Ciclo de packages: " + String.join(" -> ", cycle));
        }
    }

    /** Un camino de {@code path} que vuelve a su primer package, o vacío si no hay. */
    private static List<String> cycle(Map<String, Set<String>> graph, String current, List<String> path) {
        for (String next : graph.getOrDefault(current, Set.of())) {
            if (next.equals(path.get(0))) {
                List<String> cycle = new ArrayList<>(path);
                cycle.add(next);
                return cycle;
            }
            if (!path.contains(next)) {
                path.add(next);
                List<String> found = cycle(graph, next, path);
                if (!found.isEmpty()) {
                    return found;
                }
                path.remove(path.size() - 1);
            }
        }
        return List.of();
    }

    /** {@code model} son tipos puros: no depende de nada del motor. */
    @Test
    void modelDependsOnNothing() {
        assertEquals(Set.of(), dependencies().get("model"));
    }

    /** El árbol de cajas se construye y se valida sin saber nada del layout ni del pintado. */
    @Test
    void treeDependsOnlyOnModel() {
        assertEquals(Set.of("model"), dependencies().get("tree"));
    }

    /** {@code convert} es la fachada: la usan los clientes y las pruebas, ninguna otra pieza del motor. */
    @Test
    void nothingInTheEngineDependsOnTheFacade() {
        dependencies().forEach((from, to) ->
                assertFalse(to.contains("convert"), () -> from + " depende de convert"));
    }
}
