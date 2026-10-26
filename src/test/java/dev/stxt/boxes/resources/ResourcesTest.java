package dev.stxt.boxes.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ResourcesTest {

    @TempDir
    Path dir;

    private Resources resources() throws IOException {
        Path root = Files.createDirectories(dir.resolve("resources"));
        Files.createDirectories(root.resolve("images/logos"));
        Files.writeString(root.resolve("images/logos/a.png"), "x");
        Files.writeString(dir.resolve("secret.txt"), "x");
        return new Resources(root);
    }

    @Test
    void resolvesDownwards() throws IOException {
        Resources r = resources();
        assertEquals(r.root().resolve("images/logos/a.png"), r.resolve("images/logos/a.png"));
        assertEquals(r.root().resolve("images/logos/a.png"), r.resolve("images\\logos\\a.png"));
        assertEquals(r.root().resolve("images/logos/a.png"), r.resolve("./images/logos/a.png"));
    }

    @Test
    void neverBackwards() throws IOException {
        Resources r = resources();
        for (String path : new String[] {"../secret.txt", "images/../../secret.txt", "images/../images/logos/a.png",
                "images\\..\\..\\secret.txt", ".."}) {
            ResourceException e = assertThrows(ResourceException.class, () -> r.resolve(path), path);
            assertTrue(e.getMessage().startsWith("Ruta de recurso hacia atrás"), e.getMessage());
        }
    }

    @Test
    void neverAbsolute() throws IOException {
        Resources r = resources();
        for (String path : new String[] {dir.resolve("secret.txt").toString(), "/etc/passwd", "\\secret.txt",
                "C:\\secret.txt", "c:secret.txt", "~/secret.txt"}) {
            ResourceException e = assertThrows(ResourceException.class, () -> r.resolve(path), path);
            assertTrue(e.getMessage().startsWith("Ruta de recurso absoluta"), e.getMessage());
        }
    }

    @Test
    void missingOrEmpty() throws IOException {
        Resources r = resources();
        assertThrows(ResourceException.class, () -> r.resolve(""));
        ResourceException e = assertThrows(ResourceException.class, () -> r.resolve("images/b.png"));
        assertTrue(e.getMessage().startsWith("No existe el recurso images/b.png"), e.getMessage());
    }

    @Test
    void symbolicLinksCannotLeaveTheFolder() throws IOException {
        Resources r = resources();
        Path link = r.root().resolve("images/secret.txt");
        try {
            Files.createSymbolicLink(link, dir.resolve("secret.txt"));
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "El sistema no admite enlaces simbólicos");
        }
        ResourceException e = assertThrows(ResourceException.class, () -> r.resolve("images/secret.txt"));
        assertTrue(e.getMessage().contains("es un enlace que sale de la carpeta de recursos"), e.getMessage());

        Files.createSymbolicLink(r.root().resolve("images/inside.png"), r.root().resolve("images/logos/a.png"));
        assertEquals(r.root().resolve("images/inside.png"), r.resolve("images/inside.png"));
    }

    @Test
    void defaultRootIsTheResourcesFolder() {
        assertEquals(Path.of("resources").toAbsolutePath().normalize(), new Resources().root());
        assertEquals(new Resources().root().resolve("fonts"), new Resources().fontsFolder());
    }
}
