package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.Limits;
import dev.stxt.boxes.resources.Resources;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El texto de un SVG se dibuja con el fichero de {@code fonts/} que se comprobó al leerlo
 * (referencia/componentes/image.md), sin estado de la JVM: ni el idioma por defecto ni otra carpeta de recursos con la
 * misma familia lo cambian.
 */
class PdfImagesTest {

    private static final Path FONTS = Resources.DEFAULT_ROOT.resolve("fonts");
    private static final String NS = "xmlns=\"http://www.w3.org/2000/svg\"";

    @TempDir
    Path dir;

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    /** El contenido del formulario en que se dibuja el SVG {@code images/name} de los recursos. */
    private static byte[] draw(Resources resources, String name) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            return new PdfImages(doc, Limits.DEFAULT).drawing(resources.images().find(name))
                    .getContentStream().toByteArray();
        }
    }

    /** Una carpeta de recursos con esas fuentes de {@code resources/fonts/} y un SVG {@code image.svg}. */
    private Resources folder(String name, String svg, String... fonts) throws IOException {
        Path root = dir.resolve(name);
        Files.createDirectories(root.resolve("fonts/f"));
        Files.createDirectories(root.resolve("images"));
        for (String font : fonts) {
            Path file = FONTS.resolve(font);
            Files.copy(file, root.resolve("fonts/f").resolve(file.getFileName()));
        }
        Files.writeString(root.resolve("images/image.svg"), svg, StandardCharsets.UTF_8);
        return new Resources(root);
    }

    private static String text(String family) {
        return "<svg " + NS + " viewBox=\"0 0 120 30\"><text x=\"4\" y=\"20\" font-family=\"" + family
                + "\" font-size=\"16\">Nogal fina</text></svg>";
    }

    @Test
    void theLanguageOfTheJvmDoesNotChangeTheText() throws Exception {
        // En una JVM que arranca en turco, AWT no encuentra «IBM Plex Sans» por su nombre (la I mayúscula pasa a ı) y
        // JSVG dibujaba con Dialog. Cambiar el Locale en caliente no lo reproduce: cada dibujo va en su JVM.
        assertEquals(drawIn("en", "US"), drawIn("tr", "TR"));
    }

    /** La huella del dibujo de {@code test/label.svg} en una JVM nueva con ese idioma. */
    private static String drawIn(String language, String country) throws Exception {
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        Process process = new ProcessBuilder(java.toString(), "-Djava.awt.headless=true",
                "-Duser.language=" + language, "-Duser.country=" + country,
                "-cp", System.getProperty("java.class.path"), Draw.class.getName(), "test/label.svg")
                .redirectErrorStream(true).start();
        String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), out);
        return out.lines().filter(l -> l.startsWith("sha-256 ")).findFirst().orElseThrow(() -> new AssertionError(out));
    }

    /** Dibuja un SVG de {@code resources/images/} y escribe la huella del formulario. */
    static final class Draw {
        public static void main(String[] args) throws Exception {
            byte[] content = draw(new Resources(), args[0]);
            System.out.println("sha-256 " + java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(content)) + " " + content.length);
        }
    }

    @Test
    void twoFoldersWithTheSameFamilyDrawEachItsOwnFile() throws IOException {
        Resources sans = folder("sans", text("IBM Plex Sans"), "IBM Plex Sans/IBMPlexSans-Regular.ttf");
        // Otra carpeta con una «IBM Plex Sans» que en realidad es IBM Plex Serif con la tabla name cambiada.
        Resources fake = folder("fake", text("IBM Plex Sans"));
        Path serif = FONTS.resolve("IBM Plex Serif/IBMPlexSerif-Regular.ttf");
        Files.write(dir.resolve("fake/fonts/f/Fake-Regular.ttf"), renamed(Files.readAllBytes(serif), "IBM Plex Sans"));
        Resources real = folder("serif", text("IBM Plex Serif"), "IBM Plex Serif/IBMPlexSerif-Regular.ttf");

        byte[] first = draw(sans, "image.svg");
        byte[] second = draw(fake, "image.svg");
        assertFalse(java.util.Arrays.equals(first, second), "la segunda carpeta se dibuja con la fuente de la primera");
        assertArrayEquals(draw(real, "image.svg"), second);
        assertArrayEquals(first, draw(sans, "image.svg"));
    }

    @Test
    void theFontOfStyleAndOfTheAncestorsIsTheOneDrawn() throws IOException {
        String declared = "<svg " + NS + " viewBox=\"0 0 200 60\">"
                + "<text x=\"4\" y=\"20\" style=\"font-size: 14px; font-family: 'IBM Plex Serif'; font-weight: bold\">"
                + "Serif</text>"
                + "<g font-family=\"IBM Plex Mono\" style=\"font-style: italic\"><text x=\"4\" y=\"40\" font-size=\"14\">"
                + "Mono <tspan font-weight=\"700\">negrita</tspan> fin</text></g></svg>";
        String explicit = "<svg " + NS + " viewBox=\"0 0 200 60\">"
                + "<text x=\"4\" y=\"20\" font-size=\"14\" font-family=\"IBM Plex Serif\" font-weight=\"bold\">Serif</text>"
                + "<g><text x=\"4\" y=\"40\" font-size=\"14\" font-family=\"IBM Plex Mono\" font-style=\"italic\">"
                + "Mono <tspan font-family=\"IBM Plex Mono\" font-weight=\"bold\" font-style=\"italic\">negrita</tspan>"
                + " fin</text></g></svg>";
        String[] fonts = {"IBM Plex Serif/IBMPlexSerif-Bold.ttf", "IBM Plex Mono/IBMPlexMono-Italic.ttf",
                "IBM Plex Mono/IBMPlexMono-BoldItalic.ttf"};
        byte[] a = draw(folder("declared", declared, fonts), "image.svg");
        byte[] b = draw(folder("explicit", explicit, fonts), "image.svg");
        assertArrayEquals(b, a);
        assertTrue(a.length > 1000, "el texto se dibuja como contornos");
    }

    @Test
    void aTextRepeatedByAUseKeepsTheFontOfItsPlaceInTheFile() throws IOException {
        // El texto se comprueba con la fuente de su sitio en el fichero (Regular), y esa es la que se dibuja aunque
        // el <use> esté dentro de un grupo en negrita (referencia/componentes/image.md).
        String used = "<svg " + NS + " viewBox=\"0 0 120 30\"><defs><text id=\"t\" x=\"4\" y=\"20\" "
                + "font-family=\"IBM Plex Sans\" font-size=\"16\">Nogal</text></defs>"
                + "<g font-weight=\"bold\"><use href=\"#t\"/></g></svg>";
        String regular = used.replace("<g font-weight=\"bold\">", "<g>");
        String[] fonts = {"IBM Plex Sans/IBMPlexSans-Regular.ttf"};
        assertArrayEquals(draw(folder("regular", regular, fonts), "image.svg"),
                draw(folder("used", used, fonts), "image.svg"));
    }

    /**
     * Una copia de la fuente cuya tabla {@code name} dice que es de otra familia (Regular): la tabla nueva va al final
     * del fichero y la entrada del directorio apunta a ella. No recalcula las sumas de control, que no se comprueban.
     */
    static byte[] renamed(byte[] ttf, String family) {
        String[][] names = {{"1", family}, {"2", "Regular"}, {"4", family + " Regular"},
                {"6", family.replace(" ", "") + "-Regular"}, {"16", family}, {"17", "Regular"}};
        ByteBuffer strings = ByteBuffer.allocate(1024);
        ByteBuffer table = ByteBuffer.allocate(6 + 12 * names.length + 1024);
        table.putShort((short) 0).putShort((short) names.length).putShort((short) (6 + 12 * names.length));
        for (String[] name : names) {
            byte[] value = name[1].getBytes(StandardCharsets.UTF_16BE);
            table.putShort((short) 3).putShort((short) 1).putShort((short) 0x409).putShort(Short.parseShort(name[0]))
                    .putShort((short) value.length).putShort((short) strings.position());
            strings.put(value);
        }
        table.put(strings.array(), 0, strings.position());
        int length = table.position();
        int offset = (ttf.length + 3) & ~3;
        ByteBuffer out = ByteBuffer.allocate(offset + length);
        out.put(ttf).position(offset);
        out.put(table.array(), 0, length);
        int tables = Short.toUnsignedInt(out.getShort(4));
        for (int i = 0; i < tables; i++) {
            int entry = 12 + 16 * i;
            if (out.getInt(entry) == 0x6E616D65) { // 'name'
                out.putInt(entry + 8, offset).putInt(entry + 12, length);
            }
        }
        return out.array();
    }
}
