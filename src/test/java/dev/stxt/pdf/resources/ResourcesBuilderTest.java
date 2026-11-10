package dev.stxt.pdf.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;

/**
 * Resources that come from several places (reference/implementation/api.md): a folder of fonts, folders of images
 * with the prefix of their paths, fonts and images given as bytes, and the images added for one conversion.
 */
class ResourcesBuilderTest {

    private static final Path FONTS = Resources.DEFAULT_ROOT.resolve("fonts");
    private static final Path IMAGES = Resources.DEFAULT_ROOT.resolve("images/test");
    private static final Path SANS = FONTS.resolve("IBM Plex Sans/IBMPlexSans-Regular.ttf");
    private static final FontFace REGULAR = new FontFace("IBM Plex Sans", new FontWeight(400), FontStyle.NORMAL);

    private static byte[] image(String name) throws IOException {
        return Files.readAllBytes(IMAGES.resolve(name));
    }

    /** A folder with those images of the tests, each under that name. */
    private static Path folder(Path dir, String name, String... images) throws IOException {
        Path folder = Files.createDirectories(dir.resolve(name));
        for (int i = 0; i < images.length; i += 2) {
            Path target = folder.resolve(images[i]);
            Files.createDirectories(target.getParent());
            Files.copy(IMAGES.resolve(images[i + 1]), target);
        }
        return folder;
    }

    @Test
    void aSingleFolderIsItsFontsAndItsImages() {
        Resources resources = new Resources();
        assertEquals(Resources.DEFAULT_ROOT.toAbsolutePath(), resources.root());
        assertEquals(resources.root().resolve("fonts"), resources.fontsFolder());
        assertEquals(resources.root().resolve("images"), resources.imagesFolder());
        assertTrue(resources.imagePaths().contains("test/square.png"), resources.imagePaths().toString());
    }

    @Test
    void anImageGoesToTheFolderWithTheLongestPrefixOfItsPath(@TempDir Path dir) throws IOException {
        Path plain = folder(dir, "plain", "a.png", "square.png", "logos/b.png", "wide.png");
        Path logos = folder(dir, "logos", "b.png", "square.png", "big/c.png", "wide.png");
        Path big = folder(dir, "big", "c.png", "square.png");
        Resources resources = Resources.builder().fonts(FONTS).images(plain).images("logos", logos)
                .images("logos/big", big).build();
        assertNull(resources.root());
        assertEquals(plain.toAbsolutePath(), resources.imagesFolder());
        assertEquals(plain.resolve("a.png").toAbsolutePath(), resources.images().find("a.png").path());
        // The folder of the prefix, not the subfolder of the same name of the folder without prefix.
        assertEquals(logos.resolve("b.png").toAbsolutePath(), resources.images().find("logos/b.png").path());
        assertEquals(big.resolve("c.png").toAbsolutePath(), resources.images().find("logos/big/c.png").path());
        assertEquals(big.resolve("c.png").toAbsolutePath(), resources.images().find("./logos//big\\c.png").path());
        // Each path once, whatever the folders that have a file for it.
        assertEquals(List.of("a.png", "logos/b.png", "logos/big/c.png"), resources.imagePaths());
    }

    @Test
    void aPathWithoutFolderIsAResourceThatDoesNotExist(@TempDir Path dir) throws IOException {
        Path img = folder(dir, "img", "a.png", "square.png");
        Resources resources = Resources.builder().fonts(FONTS).images("img", img).build();
        assertNull(resources.imagesFolder());
        assertEquals(ImageFile.Format.PNG, resources.images().find("img/a.png").format());
        ResourceException e = assertThrows(ResourceException.class, () -> resources.images().find("a.png"));
        assertTrue(e.getMessage().contains("no folder of images has its prefix (img/)"), e.getMessage());
        e = assertThrows(ResourceException.class, () -> resources.images().find("img/b.png"));
        assertTrue(e.getMessage().contains("img/b.png does not exist"), e.getMessage());
        // A path does not leave its folder.
        e = assertThrows(ResourceException.class, () -> resources.images().find("img/../plain/a.png"));
        assertTrue(e.getMessage().contains("'..' is not allowed"), e.getMessage());
        ResourceException none = assertThrows(ResourceException.class,
                () -> Resources.builder().fonts(FONTS).build().images().find("a.png"));
        assertTrue(none.getMessage().contains("the resources have no folder of images"), none.getMessage());
        // And these resources have no folder of their own.
        assertThrows(ResourceException.class, () -> resources.resolve("img/a.png"));
    }

    @Test
    void anImageGivenAsBytesHasNoFileAndGoesBeforeTheFolders(@TempDir Path dir) throws IOException {
        Path plain = folder(dir, "plain", "a.png", "square.png");
        byte[] wide = image("wide.png");
        Resources resources = Resources.builder().fonts(FONTS).images(plain).image("a.png", wide)
                .image("mem/logo.png", wide).build();
        ImageFile logo = resources.images().find("mem/logo.png");
        assertNull(logo.path());
        assertTrue(logo.inMemory());
        assertEquals("logo.png", logo.fileName());
        assertEquals(ImageFile.Format.PNG, logo.format());
        ImageFile inFolder = new Resources(Resources.DEFAULT_ROOT).images().find("test/wide.png");
        assertEquals(inFolder.pixelWidth(), logo.pixelWidth());
        assertEquals(inFolder.pixelWidth(), resources.images().find("a.png").pixelWidth());
        assertSame(logo, resources.images().find("./mem/logo.png"));
        // The bytes are copied when they are given, and each call gives a copy.
        wide[0] = 0;
        assertEquals((byte) 0x89, logo.content()[0]);
        // A snapshot does not read it again.
        assertSame(logo, resources.snapshot().images().find("mem/logo.png"));
        assertEquals(List.of("a.png", "mem/logo.png"), resources.imagePaths());
        assertEquals(0, resources.images().pdfBytes("mem/logo.png"));
        byte[] pdf = image("pages.pdf");
        assertEquals(pdf.length, Resources.builder().image("x.pdf", pdf).build().images().pdfBytes("x.pdf"));
    }

    @Test
    void anImageGivenAsBytesThatIsNotAnImageFailsWhenItIsUsed() {
        Resources resources = Resources.builder().image("nada.png", new byte[] {1, 2, 3}).build();
        ImageFormatException e = assertThrows(ImageFormatException.class, () -> resources.images().find("nada.png"));
        assertTrue(e.getMessage().contains("nada.png is not PNG, JPEG, SVG or PDF"), e.getMessage());
    }

    @Test
    void fontsGivenAsBytesAreIndexedWithThoseOfTheFolder(@TempDir Path dir) throws IOException {
        byte[] sans = Files.readAllBytes(SANS);
        Resources memory = Resources.builder().font("IBMPlexSans-Regular.ttf", sans).build();
        FontFile font = memory.fonts().find(REGULAR);
        assertNull(font.path());
        assertEquals("IBMPlexSans-Regular.ttf (in memory)", font.name());
        assertEquals(new Resources().fonts().find(REGULAR).digest(), font.digest());
        assertNull(memory.fontsFolder());
        // They do not change: a snapshot has the same index.
        assertSame(memory.fonts(), memory.snapshot().fonts());
        ResourceException unknown = assertThrows(ResourceException.class,
                () -> memory.fonts().find(new FontFace("Nada", new FontWeight(400), FontStyle.NORMAL)));
        assertTrue(unknown.getMessage().contains("Families in memory: IBM Plex Sans"), unknown.getMessage());

        // With a folder too: the same font twice is an error, as two files of the folder.
        Path folder = Files.createDirectories(dir.resolve("fonts"));
        Files.copy(FONTS.resolve("IBM Plex Mono/IBMPlexMono-Regular.ttf"), folder.resolve("Mono.ttf"));
        Resources both = Resources.builder().fonts(folder).font("Sans.ttf", sans).build();
        assertEquals(java.util.Set.of("IBM Plex Mono", "IBM Plex Sans"), both.fonts().families());
        Files.copy(SANS, folder.resolve("Sans.ttf"));
        ResourceException twice = assertThrows(ResourceException.class, () -> both.snapshot().fonts());
        assertTrue(twice.getMessage().contains("Two files for the font") && twice.getMessage().contains("Sans.ttf and "
                + "Sans.ttf (in memory)"), twice.getMessage());
    }

    @Test
    void resourcesWithoutFontsFailWhenAFontIsAskedFor() {
        ResourceException e = assertThrows(ResourceException.class, () -> Resources.builder().build().fonts());
        assertTrue(e.getMessage().contains("The resources have no fonts"), e.getMessage());
    }

    @Test
    void whatTheBuilderIsGivenTwiceOrWrongIsRejected(@TempDir Path dir) {
        Resources.Builder b = Resources.builder().fonts(dir).images(dir).images("a", dir).image("x.png", new byte[1])
                .font("f.ttf", new byte[1]);
        assertThrows(IllegalArgumentException.class, () -> b.fonts(dir));
        assertThrows(IllegalArgumentException.class, () -> b.images(dir));
        assertThrows(IllegalArgumentException.class, () -> b.images("./a/", dir));
        assertThrows(IllegalArgumentException.class, () -> b.image("./x.png", new byte[1]));
        assertThrows(IllegalArgumentException.class, () -> b.font("f.ttf", new byte[1]));
        assertThrows(IllegalArgumentException.class, () -> b.font(" ", new byte[1]));
        for (String path : new String[] {"", " ", "/a", "..", "a/../b", "C:/a", "~/a", "."}) {
            assertThrows(IllegalArgumentException.class, () -> Resources.builder().images(path, dir), path);
            assertThrows(IllegalArgumentException.class, () -> Resources.builder().image(path, new byte[1]), path);
            assertThrows(IllegalArgumentException.class, () -> new Resources().withImages(Map.of(path, new byte[1])),
                    path);
        }
    }

    @Test
    void theImagesAddedForAConversionGoBeforeTheRestAndReadNothingAgain(@TempDir Path dir) throws IOException {
        Path plain = folder(dir, "plain", "a.png", "square.png", "b.png", "square.png");
        Resources resources = Resources.builder().fonts(FONTS).images(plain).image("m.png", image("square.png"))
                .build();
        ImageFile b = resources.images().find("b.png");
        Resources added = resources.withImages(Map.of("a.png", image("wide.png"), "extra/c.png", image("wide.png")));
        // Its own images, and those of the resources as they were found.
        assertNull(added.images().find("a.png").path());
        assertEquals(image("wide.png").length, added.images().find("extra/c.png").content().length);
        assertSame(b, added.images().find("b.png"));
        assertSame(resources.images().find("m.png"), added.images().find("m.png"));
        assertSame(resources.fonts(), added.fonts());
        assertEquals(List.of("a.png", "b.png", "extra/c.png", "m.png"), added.imagePaths());
        // The resources they are added to do not have them.
        assertEquals(plain.resolve("a.png").toAbsolutePath(), resources.images().find("a.png").path());
        assertThrows(ResourceException.class, () -> resources.images().find("extra/c.png"));
        // Each addition reads its images, and a second one keeps those of the first.
        Resources again = resources.withImages(Map.of("a.png", image("wide.png")));
        assertNotSame(added.images().find("a.png"), again.images().find("a.png"));
        Resources more = added.withImages(Map.of("d.png", image("square.png")));
        assertEquals(List.of("a.png", "b.png", "d.png", "extra/c.png", "m.png"), more.imagePaths());
        assertFalse(more.imagesInMemory());
        assertTrue(more.withImagesInMemory().imagesInMemory());
        assertNull(more.snapshot().images().find("d.png").path());
    }
}
