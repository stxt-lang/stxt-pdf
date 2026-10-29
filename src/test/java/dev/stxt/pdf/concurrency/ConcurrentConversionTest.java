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
 * La conversión es segura entre hilos con un {@code Engine} por conversión (que es lo que hace {@link Converter}):
 * varios hilos convierten los ejemplos a la vez, con las fuentes y las imágenes reales, y cada PDF es el mismo que da
 * la conversión secuencial.
 *
 * <p>Se comparan los bytes del PDF una vez quitadas las fechas y los identificadores, que son lo único
 * que cambia de una conversión a otra. Cubre los cuatro usos que importan: un {@link Resources} por hilo,
 * uno compartido por todos, un mismo {@link Converter} usado desde varios hilos y un mismo {@link LaidOutDocument} pintado desde varios hilos.</p>
 */
class ConcurrentConversionTest {

    private static final Path EXAMPLES = Path.of("test", "examples");
    private static final int THREADS = 4;
    private static final int ROUNDS = 3;

    /** Un ejemplo ya leído (la lectura no es lo que se prueba aquí) y su PDF de referencia. */
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
        assertFalse(EXAMPLE_LIST.isEmpty(), "no hay ejemplos en " + EXAMPLES);
    }

    /** El conversor de un ejemplo: con sus estilos aparte, si los tiene. */
    private static Converter converter(Example example, Resources resources) {
        Converter.Builder builder = Converter.builder().resources(resources);
        if (example.styles() != null) {
            builder.styles(example.styles());
        }
        return builder.build();
    }

    /** Documento y estilos → PDF, sin fechas ni identificadores. */
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
                    differences.put(example.name(), "ronda " + (i + 1));
                }
            }
            return differences;
        });
    }

    /** Un mismo {@link Converter} por ejemplo, compartido por todos los hilos: es inmutable y crea un motor por conversión. */
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
                        differences.put(e.name(), "ronda " + (i + 1));
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

    /** Convierte todos los ejemplos varias veces y devuelve los que no dan el PDF de referencia. */
    private static Map<String, String> check(Resources resources) throws IOException {
        Map<String, String> differences = new LinkedHashMap<>();
        for (int i = 0; i < ROUNDS; i++) {
            for (Example e : EXAMPLE_LIST) {
                if (!Arrays.equals(e.pdf(), convert(converter(e, resources), e.document()))) {
                    differences.put(e.name(), "ronda " + (i + 1));
                }
            }
        }
        return differences;
    }

    /** Lanza {@link #THREADS} hilos a la vez (arrancan juntos) y exige que ninguno informe de diferencias ni de excepciones. */
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
                    assertEquals(Map.of(), future.get(), "PDF distinto del de la conversión secuencial");
                } catch (ExecutionException e) {
                    throw new AssertionError("Un hilo ha fallado: " + e.getCause(), e.getCause());
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
