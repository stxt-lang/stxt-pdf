package dev.stxt.pdf.concurrency;

import dev.stxt.InlineNode;
import dev.stxt.pdf.PdfBytes;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.layout.LaidOutDocument;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Conversion is thread-safe with one {@code Engine} per conversion (which is what {@link Converter} does):
 * several threads convert the examples at the same time, with the real fonts and images, and each PDF is the same as
 * the one the sequential conversion gives.
 *
 * <p>The PDF bytes are compared once the dates and identifiers are removed, which are the only things
 * that change from one conversion to another. It covers the four uses that matter: one {@link Resources} per thread,
 * one shared by all, one {@link Converter} used from several threads and one {@link LaidOutDocument} painted from
 * several threads.</p>
 */
class ConcurrentConversionTest {

    private static final Path EXAMPLES = Path.of("test", "examples");
    private static final int THREADS = 4;
    private static final int ROUNDS = 3;

    /** An example already read (reading is not what is tested here) and its reference PDF. */
    private record Example(String name, InlineNode document, InlineNode styles, byte[] pdf) {
    }

    private static final List<Example> EXAMPLE_LIST = new ArrayList<>();

    @BeforeAll
    static void readExamplesAndConvertThemInSequence() throws IOException {
        DocumentReader reader = new DocumentReader();
        Resources resources = new Resources();
        List<Path> dirs;
        try (Stream<Path> list = Files.list(EXAMPLES)) {
            dirs = list.filter(Files::isDirectory).sorted().toList();
        }
        for (Path dir : dirs) {
            InlineNode document = reader.read(dir.resolve("input.stxt"));
            Path styles = dir.resolve("styles.stxt");
            InlineNode rules = Files.exists(styles) ? reader.readStyles(styles) : null;
            Example pending = new Example(dir.getFileName().toString(), document, rules, null);
            EXAMPLE_LIST.add(new Example(pending.name(), document, rules,
                    convert(converter(pending, resources), document)));
        }
        assertFalse(EXAMPLE_LIST.isEmpty(), "no examples in " + EXAMPLES);
    }

    /** The converter of an example: with its separate styles, if it has them. */
    private static Converter converter(Example example, Resources resources) {
        Converter.Builder builder = Converter.builder().resources(resources);
        if (example.styles() != null) {
            builder.styles(example.styles());
        }
        return builder.build();
    }

    /** Document and styles → PDF, without dates or identifiers. */
    private static byte[] convert(Converter converter, InlineNode document) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        converter.toPdf(document, out);
        return PdfBytes.withoutVolatileFields(out.toByteArray());
    }

    @Test
    void theSequentialConversionIsDeterministic() throws IOException {
        Resources resources = new Resources();
        for (Example e : EXAMPLE_LIST) {
            assertArrayEquals(e.pdf(), convert(converter(e, resources), e.document()), e.name());
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void eachThreadWithItsOwnResourcesGetsTheSamePdf() throws Exception {
        runConcurrently(() -> {
            Resources own = new Resources();
            return check(own);
        });
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void threadsSharingResourcesGetTheSamePdf() throws Exception {
        Resources shared = new Resources();
        runConcurrently(() -> check(shared));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void oneLaidOutDocumentCanBePaintedFromSeveralThreads() throws Exception {
        Resources resources = new Resources();
        Example example = EXAMPLE_LIST.stream().filter(e -> e.name().startsWith("04-")).findFirst()
                .orElse(EXAMPLE_LIST.get(0));
        Converter converter = converter(example, resources);
        LaidOutDocument laidOut = converter.layOut(example.document());
        byte[] expected = paint(converter, laidOut);
        runConcurrently(() -> {
            Map<String, String> differences = new LinkedHashMap<>();
            for (int i = 0; i < ROUNDS; i++) {
                if (!Arrays.equals(expected, paint(converter, laidOut))) {
                    differences.put(example.name(), "round " + (i + 1));
                }
            }
            return differences;
        });
    }

    /**
     * One {@link Converter} per example, shared by all the threads: it is immutable and creates one engine per
     * conversion.
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void threadsSharingTheConvertersGetTheSamePdf() throws Exception {
        Resources shared = new Resources();
        Map<String, Converter> converters = new LinkedHashMap<>();
        for (Example e : EXAMPLE_LIST) {
            converters.put(e.name(), converter(e, shared));
        }
        runConcurrently(() -> {
            Map<String, String> differences = new LinkedHashMap<>();
            for (int i = 0; i < ROUNDS; i++) {
                for (Example e : EXAMPLE_LIST) {
                    if (!Arrays.equals(e.pdf(), convert(converters.get(e.name()), e.document()))) {
                        differences.put(e.name(), "round " + (i + 1));
                    }
                }
            }
            return differences;
        });
    }

    private static byte[] paint(Converter converter, LaidOutDocument document) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            converter.toPdf(document, out);
            return PdfBytes.withoutVolatileFields(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Converts all the examples several times and returns the ones that do not give the reference PDF. */
    private static Map<String, String> check(Resources resources) throws IOException {
        Map<String, String> differences = new LinkedHashMap<>();
        for (int i = 0; i < ROUNDS; i++) {
            for (Example e : EXAMPLE_LIST) {
                if (!Arrays.equals(e.pdf(), convert(converter(e, resources), e.document()))) {
                    differences.put(e.name(), "round " + (i + 1));
                }
            }
        }
        return differences;
    }

    /**
     * Launches {@link #THREADS} threads at once (they start together) and requires that none reports differences or
     * exceptions.
     */
    private static void runConcurrently(Callable<Map<String, String>> work) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            List<Future<Map<String, String>>> futures = new ArrayList<>();
            for (int t = 0; t < THREADS; t++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return work.call();
                }));
            }
            start.countDown();
            for (Future<Map<String, String>> future : futures) {
                try {
                    assertEquals(Map.of(), future.get(), "PDF different from the sequential conversion");
                } catch (ExecutionException e) {
                    throw new AssertionError("A thread failed: " + e.getCause(), e.getCause());
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
