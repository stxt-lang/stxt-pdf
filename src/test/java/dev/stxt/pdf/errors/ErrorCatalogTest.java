package dev.stxt.pdf.errors;

import dev.stxt.InlineNode;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.model.StxtPdfException;
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
 * Error catalog ({@code test/errors/<group>/*.stxt}): each file is a document that must
 * fail with a code on a line (reference/common/errors.md). It starts with three comment lines
 * ({@code # Case:}, {@code # Code:} and {@code # Line:}) and the expected line is the one in the file
 * itself, header included. It is read and laid out with the default {@link Converter}, with the real metrics, so
 * that every error is covered: syntax and schema errors ({@code stxt-core}), model errors, font errors and the
 * contrast error.
 */
class ErrorCatalogTest {

    private static final Path CATALOG = Path.of("test", "errors");
    private static final Pattern HEADER = Pattern.compile("# (Case|Code|Line): (.+)");

    /** Codes without a case in the catalog, with the reason. */
    private static final Set<ErrorCode> UNCOVERED = EnumSet.of(
            // With the same metrics for every digit, pagination always converges: it is a safety
            // net that PageReferencesTest tests with a measurer that forces it.
            ErrorCode.REFERENCE_UNSTABLE,
            // A PDF of more than 20 MB does not fit in the repository: LimitsTest tests it with a lower limit.
            ErrorCode.PDF_TOO_LARGE);

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
                throw new IllegalStateException("'# " + key + ":' is missing in the first three lines of " + file);
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
            Converter.create().layOut(document);
        } catch (StxtPdfException e) {
            // Any engine error: those of the model and those of the library, with their code and line.
            assertEquals(c.code() + " on line " + c.line(), e.code() + " on line " + e.line(), e.getMessage());
            return;
        }
        fail("Expected " + c.code() + " on line " + c.line() + " and the document is valid");
    }

    @Test
    void everyCodeHasACase() {
        Set<String> used = cases().stream().map(Case::code).collect(Collectors.toSet());
        List<ErrorCode> missing = Arrays.stream(ErrorCode.values())
                .filter(code -> !used.contains(code.name()) && !UNCOVERED.contains(code))
                .toList();
        assertTrue(missing.isEmpty(), "Codes without a case in the catalog: " + missing);
    }
}
