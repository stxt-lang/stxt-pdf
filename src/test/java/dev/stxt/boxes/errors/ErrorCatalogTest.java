package dev.stxt.boxes.errors;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.inline.PdfMetrics;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.stxt.DocumentReader;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import dev.stxt.exceptions.ParseException;
import dev.stxt.runtime.STXT;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Catálogo de errores ({@code src/test/resources/errors/errors.stxt}): cada {@code Case} es un
 * documento que debe fallar con un código en una línea (ESPECIFICACION.md §6). Se lee, se
 * construye el árbol y se maqueta con las métricas reales, de modo que entran todos los errores:
 * los de sintaxis y schema ({@code stxt-core}), los del modelo y los de las fuentes.
 */
class ErrorCatalogTest {

    private static final File CATALOG = new File("src/test/resources/errors/errors.stxt");

    /** Códigos sin caso en el catálogo, con el motivo. */
    private static final Set<ErrorCode> UNCOVERED = EnumSet.of(
            // Las fuentes del repositorio tienen las cuatro variantes: lo prueba PdfMetricsTest.
            ErrorCode.FONT_VARIANT_MISSING);

    private final DocumentReader reader = new DocumentReader();

    private record Case(String name, String code, int line, String document) {
    }

    private static List<Case> cases() {
        InlineNode errors = (InlineNode) STXT.rawParser().parseFile(CATALOG).get(0);
        return errors.getChildren().stream().map(n -> {
            InlineNode c = (InlineNode) n;
            return new Case(c.getText().trim(), child(c, "Code"), Integer.parseInt(child(c, "Line")),
                    c.getChild("Document").getText());
        }).toList();
    }

    private static String child(InlineNode n, String name) {
        Node c = n.getChild(name);
        if (c == null) {
            throw new IllegalStateException("Falta " + name + " en el caso " + n.getText().trim());
        }
        return c.getText().trim();
    }

    @TestFactory
    Stream<DynamicTest> catalog() {
        return cases().stream().map(c -> DynamicTest.dynamicTest(c.name(), () -> check(c)));
    }

    private void check(Case c) {
        try {
            InlineNode document = reader.read(c.document());
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
