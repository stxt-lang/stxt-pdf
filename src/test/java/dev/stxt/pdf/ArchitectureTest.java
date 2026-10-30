package dev.stxt.pdf;

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
 * Dependencias entre los packages de {@code dev.stxt.pdf}, leídas del código fuente: los
 * {@code import} y los nombres cualificados fuera de los comentarios. Cada package cuenta con sus
 * subpackages ({@code layout.grid} es {@code layout}).
 *
 * <p>El flujo de datos va en un sentido (lectura → árbol → layout → páginas → pintado): un ciclo
 * o un árbol que depende del layout es un error de diseño, no de un documento.</p>
 */
class ArchitectureTest {

    private static final Path SOURCES = Path.of("src", "main", "java", "dev", "stxt", "pdf");
    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);
    private static final Pattern STRING = Pattern.compile("\"(?:\\\\.|[^\"\\\\\\n])*\"");

    /**
     * Lo que puede usar cada package: las dependencias de hoy, escritas a mano. Una nueva no es un
     * accidente: se decide y se apunta aquí.
     */
    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "model", Set.of(),
            "tree", Set.of("model"),
            "fragment", Set.of("model", "tree"),
            "resources", Set.of("model"),
            "stxt", Set.of("model", "resources", "tree"),
            "layout", Set.of("fragment", "model", "resources", "tree"),
            "output", Set.of("fragment", "model"),
            "paint", Set.of("fragment", "layout", "model", "resources", "tree"),
            "convert", Set.of("fragment", "layout", "model", "paint", "resources", "stxt"));

    /**
     * Lo que puede usar cada algoritmo de {@code layout} de los demás: el bloque coloca marcadores de lista
     * (texto) y cajas posicionadas, flex y grid colocan sus ítems posicionados y la tabla es un grid. Todos usan
     * las piezas de {@code layout}, que no cuentan.
     */
    private static final Map<String, Set<String>> ALLOWED_IN_LAYOUT = Map.of(
            "block", Set.of("inline", "position"),
            "flex", Set.of("position"),
            "grid", Set.of("position"),
            "table", Set.of("grid"));

    /** Package → packages de los que depende. */
    private static Map<String, Set<String>> dependencies() {
        return dependencies(SOURCES, "dev.stxt.pdf");
    }

    /**
     * Subpackage de {@code sources} → subpackages del mismo nivel de los que depende, buscando los nombres que
     * empiezan por {@code prefix}. Los ficheros que están en {@code sources} y no en un subpackage no cuentan.
     */
    private static Map<String, Set<String>> dependencies(Path sources, String prefix) {
        Pattern reference = Pattern.compile("\\b" + Pattern.quote(prefix) + "\\.([a-z]+)\\.");
        Map<String, Set<String>> graph = new TreeMap<>();
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Path relative = sources.relativize(file);
                if (relative.getNameCount() < 2) {
                    continue;
                }
                String from = relative.getName(0).toString();
                Set<String> to = graph.computeIfAbsent(from, k -> new TreeSet<>());
                Matcher m = reference.matcher(code(file));
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

    /** El código de un fichero sin comentarios ni cadenas. */
    private static String code(Path file) throws IOException {
        return STRING.matcher(COMMENT.matcher(Files.readString(file)).replaceAll(" ")).replaceAll("\"\"");
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

    /** Ningún package depende de otro que no tenga permitido ({@link #ALLOWED}). */
    @Test
    void everyDependencyIsAllowed() {
        assertEquals(ALLOWED.keySet(), dependencies().keySet());
        dependencies().forEach((from, to) -> {
            Set<String> extra = new TreeSet<>(to);
            extra.removeAll(ALLOWED.get(from));
            assertTrue(extra.isEmpty(), () -> from + " depende de " + extra + ", que no tiene permitido");
        });
    }

    /** Los algoritmos de {@code layout} no se usan entre sí salvo los casos de {@link #ALLOWED_IN_LAYOUT}. */
    @Test
    void layoutAlgorithmsOnlyUseTheAllowedOnes() {
        dependencies(SOURCES.resolve("layout"), "dev.stxt.pdf.layout").forEach((from, to) -> {
            Set<String> extra = new TreeSet<>(to);
            extra.removeAll(ALLOWED_IN_LAYOUT.getOrDefault(from, Set.of()));
            assertTrue(extra.isEmpty(), () -> "layout." + from + " depende de " + extra + ", que no tiene permitido");
        });
    }

    /**
     * Los algoritmos dependen de {@code LayoutContext}, no del motor que los crea: nada de los subpackages
     * de {@code layout} nombra {@code Engine} fuera de los comentarios.
     */
    @Test
    void noLayoutAlgorithmUsesTheEngine() throws IOException {
        Pattern engine = Pattern.compile("\\bEngine\\b");
        Path layout = SOURCES.resolve("layout");
        try (Stream<Path> files = Files.walk(layout)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (layout.relativize(file).getNameCount() > 1) {
                    assertFalse(engine.matcher(code(file)).find(), () -> file + " usa Engine: usa LayoutContext");
                }
            }
        }
    }

    /**
     * El layout y el pintado preguntan a la caja lo que significa ({@code Box.role()}), no qué elemento es:
     * fuera de {@code tree} no se nombra ninguna constante de {@code For element} salvo en el despacho de
     * {@code Engine} (la tabla tiene su algoritmo). Un elemento nuevo con un papel que ya existe no toca nada fuera
     * de {@code tree}.
     */
    @Test
    void onlyTheDispatchNamesAnElement() throws IOException {
        Pattern constant = Pattern.compile("\\bElement\\.(?!Role\\b)[A-Z_]+\\b");
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String from = SOURCES.relativize(file).getName(0).toString();
                if (from.equals("tree") || file.endsWith(Path.of("layout", "Engine.java"))) {
                    continue;
                }
                Matcher m = constant.matcher(code(file));
                while (m.find()) {
                    found.add(SOURCES.relativize(file) + ": " + m.group());
                }
            }
        }
        assertEquals(List.of(), found);
    }
}
