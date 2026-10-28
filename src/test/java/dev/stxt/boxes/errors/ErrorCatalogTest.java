package dev.stxt.boxes.errors;

import dev.stxt.InlineNode;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import dev.stxt.exceptions.ParseException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Catálogo de errores ({@code test/errors/<grupo>/*.stxt}): cada fichero es un documento que debe
 * fallar con un código en una línea (ESPECIFICACION.md §6). Empieza con tres líneas de comentario
 * ({@code # Case:}, {@code # Code:} y {@code # Line:}) y la línea esperada es la del propio
 * fichero, cabecera incluida. Se lee, se construye el árbol y se maqueta con las métricas
 * reales, de modo que entran todos los errores: los de sintaxis y schema ({@code stxt-core}),
 * los del modelo y los de las fuentes.
 */
class ErrorCatalogTest {

    private static final Path CATALOG = Path.of("test", "errors");
    private static final Pattern HEADER = Pattern.compile("# (Case|Code|Line): (.+)");

    /** Códigos sin caso en el catálogo, con el motivo. */
    private static final Set<ErrorCode> UNCOVERED = EnumSet.of(
            // Las fuentes del repositorio tienen las cuatro variantes: lo prueba PdfMetricsTest.
            ErrorCode.FONT_VARIANT_MISSING);

    private final DocumentReader reader = new DocumentReader();

    private record Case(Path file, String name, String code, int line) {
    }

    private static List<Case> cases() {
        try (Stream<Path> files = Files.walk(CATALOG)) {
            return files.filter(f -> f.toString().endsWith(".stxt")).sorted().map(ErrorCatalogTest::read).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Case read(Path file) {
        Map<String, String> header = new HashMap<>();
        try {
            for (String line : Files.readAllLines(file).subList(0, 3)) {
                Matcher m = HEADER.matcher(line);
                if (m.matches()) {
                    header.put(m.group(1), m.group(2).trim());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String key : List.of("Case", "Code", "Line")) {
            if (!header.containsKey(key)) {
                throw new IllegalStateException("Falta '# " + key + ":' en las tres primeras líneas de " + file);
            }
        }
        return new Case(file, header.get("Case"), header.get("Code"), Integer.parseInt(header.get("Line")));
    }

    @TestFactory
    Stream<DynamicTest> catalog() {
        return cases().stream().map(c -> DynamicTest.dynamicTest(CATALOG.relativize(c.file()) + ": " + c.name(), () -> check(c)));
    }

    private void check(Case c) {
        try {
            InlineNode document = reader.read(c.file());
            new Engine(new PdfMetrics(new Resources())).layOutDocument(document);
        } catch (InvalidModelException e) {
            assertEquals(c.code() + " en la línea " + c.line(), e.code() + " en la línea " + e.line(), e.getMessage());
            return;
        } catch (ParseException e) {
            assertEquals(c.code() + " en la línea " + c.line(), e.getCode() + " en la línea " + e.getLine(), e.getMessage());
            return;
        }
        fail("Se esperaba " + c.code() + " en la línea " + c.line() + " y el documento es válido");
    }

    @Test
    void everyCodeHasACase() {
        Set<String> used = cases().stream().map(Case::code).collect(Collectors.toSet());
        List<ErrorCode> missing = Arrays.stream(ErrorCode.values())
                .filter(code -> !used.contains(code.name()) && !UNCOVERED.contains(code))
                .toList();
        assertTrue(missing.isEmpty(), "Códigos sin caso en el catálogo: " + missing);
    }
}
