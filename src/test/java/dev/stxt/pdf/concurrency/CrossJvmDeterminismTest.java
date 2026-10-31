package dev.stxt.pdf.concurrency;

import dev.stxt.InlineNode;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The PDF of each example and of each golden case is the same in another JVM, byte for byte. In another JVM the {@code identityHashCode} of the boxes changes, and with it the order in which an
 * {@code IdentityHashMap} or a {@code HashSet} of boxes is traversed: if the result depended on that order, it would
 * show here. Within the same JVM, {@link ConcurrentConversionTest} checks it.
 */
class CrossJvmDeterminismTest {

    @Test
    void anotherJvmGivesTheSamePdfs() throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Process process = new ProcessBuilder(java.toString(), "-Djava.awt.headless=true", "-cp",
                System.getProperty("java.class.path"), Hashes.class.getName()).redirectErrorStream(true).start();
        String other = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), other);
        assertEquals(Hashes.all(), other.lines().filter(l -> l.startsWith("test")).toList());
    }

    /** Writes the hash of the PDF of each document, one per line. */
    static final class Hashes {
        public static void main(String[] args) throws Exception {
            all().forEach(System.out::println);
        }

        static List<String> all() throws Exception {
            DocumentReader reader = new DocumentReader();
            // One document per example and language (test/examples/01-invoice/en) and per golden case.
            List<Path> dirs;
            try (Stream<Path> examples = Files.find(Path.of("test", "examples"), 3,
                    (p, a) -> p.getFileName().toString().equals("input.stxt"));
                 Stream<Path> golden = Files.list(Path.of("test", "golden"))) {
                dirs = Stream.concat(examples.map(Path::getParent), golden.filter(Files::isDirectory)).sorted().toList();
            }
            List<String> hashes = new java.util.ArrayList<>();
            for (Path dir : dirs) {
                InlineNode document = reader.read(dir.resolve("input.stxt"));
                // The styles of an example are in its folder, shared by its languages.
                Path styles = (dir.startsWith(Path.of("test", "examples")) ? dir.getParent() : dir).resolve("styles.stxt");
                // The colors of a golden case only tell the boxes apart: no contrast is required of them.
                Converter.Builder builder = Converter.builder();
                if (dir.startsWith(Path.of("test", "golden"))) {
                    builder.exclude(Check.CONTRAST);
                }
                if (Files.exists(styles)) {
                    builder.styles(reader.readStyles(styles));
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                builder.build().toPdf(document, out);
                byte[] pdf = out.toByteArray();
                hashes.add(dir.toString().replace('\\', '/') + " "
                        + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdf)));
            }
            return hashes;
        }
    }
}
