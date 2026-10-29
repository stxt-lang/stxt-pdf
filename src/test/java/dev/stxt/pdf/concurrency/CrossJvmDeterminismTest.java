package dev.stxt.pdf.concurrency;

import dev.stxt.InlineNode;
import dev.stxt.pdf.PdfBytes;
import dev.stxt.pdf.convert.Converter;
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
 * El PDF de cada ejemplo y de cada caso golden es el mismo en otra JVM, sin fechas ni
 * identificadores. En otra JVM cambian los {@code identityHashCode} de las cajas, y con ellos el orden en que se
 * recorre un {@code IdentityHashMap} o un {@code HashSet} de cajas: si el resultado dependiera de ese orden, aquí se
 * vería. Dentro de una misma JVM lo comprueba {@link ConcurrentConversionTest}.
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

    /** Escribe la huella del PDF de cada documento, uno por línea. */
    static final class Hashes {
        public static void main(String[] args) throws Exception {
            all().forEach(System.out::println);
        }

        static List<String> all() throws Exception {
            DocumentReader reader = new DocumentReader();
            List<Path> dirs;
            try (Stream<Path> examples = Files.list(Path.of("test", "examples"));
                 Stream<Path> golden = Files.list(Path.of("test", "golden"))) {
                dirs = Stream.concat(examples, golden).filter(Files::isDirectory).sorted().toList();
            }
            List<String> hashes = new java.util.ArrayList<>();
            for (Path dir : dirs) {
                InlineNode document = reader.read(dir.resolve("input.stxt"));
                Path styles = dir.resolve("styles.stxt");
                Converter.Builder builder = Converter.builder();
                if (Files.exists(styles)) {
                    builder.styles(reader.readStyles(styles));
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                builder.build().toPdf(document, out);
                byte[] pdf = PdfBytes.withoutVolatileFields(out.toByteArray());
                hashes.add(dir.toString().replace('\\', '/') + " "
                        + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdf)));
            }
            return hashes;
        }
    }
}
