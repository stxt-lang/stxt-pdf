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
 * Dependencies between the packages of {@code dev.stxt.pdf}, read from the source code: the
 * {@code import} statements and the qualified names outside comments. Each package counts with its
 * subpackages ({@code layout.grid} is {@code layout}).
 *
 * <p>Data flows in one direction (reading → tree → layout → pages → painting): a cycle, or a tree
 * that depends on the layout, is a design error, not a document error.</p>
 */
class ArchitectureTest {

    private static final Path SOURCES = Path.of("src", "main", "java", "dev", "stxt", "pdf");
    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/|//[^\\n]*", Pattern.DOTALL);
    private static final Pattern STRING = Pattern.compile("\"(?:\\\\.|[^\"\\\\\\n])*\"");

    /**
     * What each package may use: today's dependencies, written by hand. A new one is not an
     * accident: it is decided and recorded here.
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
     * What each {@code layout} algorithm may use from the others: the block places list markers (text) and
     * positioned boxes, stack and grid place their positioned items, and the table is a grid. All of them use the
     * pieces of {@code layout}, which do not count.
     */
    private static final Map<String, Set<String>> ALLOWED_IN_LAYOUT = Map.of(
            "block", Set.of("inline", "position"),
            "stack", Set.of("position"),
            "grid", Set.of("position"),
            "table", Set.of("grid"));

    /** Package → packages it depends on. */
    private static Map<String, Set<String>> dependencies() {
        return dependencies(SOURCES, "dev.stxt.pdf");
    }

    /**
     * Subpackage of {@code sources} → subpackages of the same level it depends on, looking for the names that
     * start with {@code prefix}. The files that are in {@code sources} and not in a subpackage do not count.
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

    /** The code of a file without comments or strings. */
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
            assertTrue(cycle.isEmpty(), () -> "Package cycle: " + String.join(" -> ", cycle));
        }
    }

    /** A path from {@code path} that returns to its first package, or empty if there is none. */
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

    /** {@code model} holds pure types: it depends on nothing in the engine. */
    @Test
    void modelDependsOnNothing() {
        assertEquals(Set.of(), dependencies().get("model"));
    }

    /** The box tree is built and validated without knowing anything about layout or painting. */
    @Test
    void treeDependsOnlyOnModel() {
        assertEquals(Set.of("model"), dependencies().get("tree"));
    }

    /** {@code convert} is the facade: clients and tests use it, no other piece of the engine does. */
    @Test
    void nothingInTheEngineDependsOnTheFacade() {
        dependencies().forEach((from, to) ->
                assertFalse(to.contains("convert"), () -> from + " depends on convert"));
    }

    /** No package depends on another one it is not allowed to use ({@link #ALLOWED}). */
    @Test
    void everyDependencyIsAllowed() {
        assertEquals(ALLOWED.keySet(), dependencies().keySet());
        dependencies().forEach((from, to) -> {
            Set<String> extra = new TreeSet<>(to);
            extra.removeAll(ALLOWED.get(from));
            assertTrue(extra.isEmpty(), () -> from + " depends on " + extra + ", which is not allowed");
        });
    }

    /** The {@code layout} algorithms do not use each other except in the cases of {@link #ALLOWED_IN_LAYOUT}. */
    @Test
    void layoutAlgorithmsOnlyUseTheAllowedOnes() {
        dependencies(SOURCES.resolve("layout"), "dev.stxt.pdf.layout").forEach((from, to) -> {
            Set<String> extra = new TreeSet<>(to);
            extra.removeAll(ALLOWED_IN_LAYOUT.getOrDefault(from, Set.of()));
            assertTrue(extra.isEmpty(), () -> "layout." + from + " depends on " + extra + ", which is not allowed");
        });
    }

    /**
     * The algorithms depend on {@code LayoutContext}, not on the engine that creates them: nothing in the
     * subpackages of {@code layout} names {@code Engine} outside comments.
     */
    @Test
    void noLayoutAlgorithmUsesTheEngine() throws IOException {
        Pattern engine = Pattern.compile("\\bEngine\\b");
        Path layout = SOURCES.resolve("layout");
        try (Stream<Path> files = Files.walk(layout)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (layout.relativize(file).getNameCount() > 1) {
                    assertFalse(engine.matcher(code(file)).find(), () -> file + " uses Engine instead of LayoutContext");
                }
            }
        }
    }

    /**
     * Layout and painting ask the box what it means ({@code Box.role()}), not which element it is: outside
     * {@code tree} no {@code For element} constant is named except in the dispatch of {@code Engine} (the table
     * has its own algorithm). A new element with a role that already exists touches nothing outside {@code tree}.
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
