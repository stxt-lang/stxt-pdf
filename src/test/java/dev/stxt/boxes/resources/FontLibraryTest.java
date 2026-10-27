package dev.stxt.boxes.resources;

import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sobre las fuentes del repositorio ({@code resources/fonts}) y copias suyas en carpetas temporales. */
class FontLibraryTest {

    private static final Path PLEX = Resources.DEFAULT_ROOT.resolve("fonts/IBM Plex Sans");

    @TempDir
    Path dir;

    private static FontFace plex(FontWeight weight, FontStyle style) {
        return new FontFace("IBM Plex Sans", weight, style);
    }

    /** Recursos temporales con algunas variantes de Plex, en {@code fonts/<carpeta>/}. */
    private Resources copy(String folder, String... variants) throws IOException {
        Path fonts = Files.createDirectories(dir.resolve("resources/fonts").resolve(folder));
        for (String v : variants) {
            Files.copy(PLEX.resolve("IBMPlexSans-" + v + ".ttf"), fonts.resolve(v + ".ttf"));
        }
        return new Resources(dir.resolve("resources"));
    }

    @Test
    void indexByFamilyWeightAndStyle() {
        FontLibrary fonts = new Resources().fonts();
        assertEquals(Set.of("IBM Plex Sans"), fonts.families());
        assertEquals("IBMPlexSans-Regular.ttf",
                fonts.find(plex(FontWeight.NORMAL, FontStyle.NORMAL)).path().getFileName().toString());
        assertEquals("IBMPlexSans-Bold.ttf",
                fonts.find(plex(FontWeight.BOLD, FontStyle.NORMAL)).path().getFileName().toString());
        assertEquals("IBMPlexSans-Italic.ttf",
                fonts.find(plex(FontWeight.NORMAL, FontStyle.ITALIC)).path().getFileName().toString());
        assertEquals("IBMPlexSans-BoldItalic.ttf",
                fonts.find(plex(FontWeight.BOLD, FontStyle.ITALIC)).path().getFileName().toString());
    }

    @Test
    void verticalMetricsFromHhea() {
        FontFile f = new Resources().fonts().find(plex(FontWeight.NORMAL, FontStyle.NORMAL));
        assertEquals(1.025, f.vertical().ascent(), 1e-9);
        assertEquals(0.275, f.vertical().descent(), 1e-9);
    }

    @Test
    void missingVariant() throws IOException {
        Resources r = copy("plex", "Regular", "Bold");
        ResourceException e = assertThrows(ResourceException.class,
                () -> r.fonts().find(plex(FontWeight.BOLD, FontStyle.ITALIC)));
        assertEquals("La fuente IBM Plex Sans no tiene la variante bold italic. Variantes en "
                + r.fontsFolder() + ": bold, normal", e.getMessage());
    }

    @Test
    void unknownFamily() throws IOException {
        Resources r = copy("plex", "Regular");
        ResourceException e = assertThrows(ResourceException.class, () -> r.fonts().find(
                new FontFace("Helvetica", FontWeight.NORMAL, FontStyle.NORMAL)));
        assertEquals("Fuente desconocida: Helvetica. Familias en " + r.fontsFolder() + ": IBM Plex Sans",
                e.getMessage());
    }

    @Test
    void twoFilesForTheSameFont() throws IOException {
        Resources r = copy("a", "Regular");
        Files.copy(PLEX.resolve("IBMPlexSans-Regular.ttf"), r.fontsFolder().resolve("b.ttf"));
        ResourceException e = assertThrows(ResourceException.class, r::fonts);
        assertTrue(e.getMessage().startsWith("Dos ficheros para la fuente IBM Plex Sans normal"), e.getMessage());
    }

    @Test
    void unreadableFile() throws IOException {
        Resources r = copy("plex", "Regular");
        Files.writeString(r.fontsFolder().resolve("broken.ttf"), "no es una fuente");
        ResourceException e = assertThrows(ResourceException.class, r::fonts);
        assertTrue(e.getMessage().startsWith("No se puede leer la fuente"), e.getMessage());
    }

    @Test
    void missingFolder() {
        ResourceException e = assertThrows(ResourceException.class, () -> new Resources(dir).fonts());
        assertEquals("No existe la carpeta de fuentes " + dir.resolve("fonts"), e.getMessage());
    }
}
