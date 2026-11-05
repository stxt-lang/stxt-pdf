package dev.stxt.pdf.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Resources folder (reference/common/resources.md): fonts in {@code fonts/} and images in
 * {@code images/}. By default it is {@code resources/}, relative to the working directory; this
 * object only serves to change it.
 *
 * <p>Resources are always looked up <strong>downward</strong>: {@link #resolve(String)}
 * rejects absolute paths, {@code ..} segments and symbolic links that point
 * outside the folder.</p>
 *
 * <p><strong>Changes in the folder</strong> (reference/implementation/api.md): a {@code Resources} reads the fonts and
 * each image the first time they are requested and then keeps them while it lives, whatever happens to the files.
 * {@link #snapshot()} gives another one, which looks at the folder as it is then and shares with this one what was
 * already read: the fonts are indexed again if a file of {@code fonts/} was added, removed or replaced, and each image
 * is read again if its file changed. {@link dev.stxt.pdf.convert.Converter} takes a snapshot when it is created and
 * another one in {@link dev.stxt.pdf.convert.Converter#reloaded()}, or one for each conversion if it is
 * {@linkplain dev.stxt.pdf.convert.Converter.Builder#reloadResourcesEachConversion(boolean) built so}. An SVG is
 * painted with the bytes that were read; a PNG, a JPEG or a PDF that changed or was removed between laying out and
 * painting makes the conversion fail ({@link ImageFile#checkUnchanged()}), unless {@link #withImagesInMemory()} keeps
 * its bytes too.</p>
 *
 * <p><strong>Threads:</strong> it can be shared among all conversions and all threads, and so can its snapshots. Fonts
 * and images are read the first time they are requested, with double-checked locking, and are then read without a
 * lock.</p>
 */
public final class Resources {

    /** Default root, relative to the working directory. */
    public static final Path DEFAULT_ROOT = Path.of("resources");

    /** {@code C:}, {@code d:}...: Windows drive. */
    private static final Pattern DRIVE = Pattern.compile("^[A-Za-z]:");

    private final Path root;
    /** What this object and its snapshots share. */
    private final Shared shared;
    /** Created the first time they are requested and read without a lock (double-checked locking). */
    private volatile FontLibrary fonts;
    private volatile ImageLibrary images;

    /** Resources in {@link #DEFAULT_ROOT}. */
    public Resources() {
        this(DEFAULT_ROOT);
    }

    /**
     * Resources in {@code root}, which does not need to exist yet: it is looked at the first time a font or an image
     * is requested.
     *
     * @param root the resources folder, absolute or relative to the working directory
     */
    public Resources(Path root) {
        this(root.toAbsolutePath().normalize(), new Shared(false));
    }

    private Resources(Path root, Shared shared) {
        this.root = root;
        this.shared = shared;
    }

    /**
     * {@return resources of the same folder that keep in memory the bytes of each image they read, also of a PNG, a
     * JPEG or a PDF, and paint it with them} An image is then painted as it was read, even if its file changes or is
     * removed afterwards, without looking at the disk again (reference/implementation/api.md); it costs keeping in
     * memory every image read, as large as its file. By default only an SVG is kept, and a PNG, a JPEG or a PDF is
     * read again when painting. The new object and its snapshots do not share what this one already read.
     */
    public Resources withImagesInMemory() {
        return new Resources(root, new Shared(true));
    }

    /** {@return whether it keeps in memory the bytes of every image it reads ({@link #withImagesInMemory()})} */
    public boolean imagesInMemory() {
        return shared.imagesInMemory;
    }

    /**
     * {@return a view of the same folder, which shares with this object (and with its other snapshots) the fonts and
     * the images already read} Those whose files have not changed are not read again. Like any {@code Resources}, it
     * looks at the folder only the first time it is asked for its fonts or for each image, and then keeps what it found
     * while it lives, so that one conversion does not mix two versions of a file. Creating it costs nothing until then.
     */
    public Resources snapshot() {
        return new Resources(root, shared);
    }

    /** {@return the resources folder, absolute and normalized} */
    public Path root() {
        return root;
    }

    /** {@return the folder of the fonts, {@code fonts/} inside the root} */
    public Path fontsFolder() {
        return root.resolve("fonts");
    }

    /** {@return the folder of the images, {@code images/} inside the root} */
    public Path imagesFolder() {
        return root.resolve("images");
    }

    /**
     * Fonts of {@code fonts/}, looked for the first time they are requested: the last scan of this object or of its
     * snapshots if the files of the folder are still those it read, or a new scan.
     *
     * @throws ResourceException if the folder does not exist or some font is not valid
     */
    public FontLibrary fonts() {
        FontLibrary library = fonts;
        if (library == null) {
            synchronized (this) {
                library = fonts;
                if (library == null) {
                    library = shared.fonts(this);
                    fonts = library;
                }
            }
        }
        return library;
    }

    /**
     * Images of {@code images/}, each one looked for the first time it is requested: the one this object or its
     * snapshots already read if the file has not changed, or the file read again.
     */
    public ImageLibrary images() {
        ImageLibrary library = images;
        if (library == null) {
            synchronized (this) {
                library = images;
                if (library == null) {
                    library = new ImageLibrary(this, shared.images);
                    images = library;
                }
            }
        }
        return library;
    }

    /** What a {@code Resources} and its snapshots share: the last scan of the fonts and the images read. */
    private static final class Shared {

        /** The images read, by real path; the one whose file changed is replaced when it is read again. */
        final Map<Path, ImageFile> images = new ConcurrentHashMap<>();
        /** Whether every image keeps its bytes ({@link Resources#withImagesInMemory()}), the same for the whole family. */
        final boolean imagesInMemory;
        private volatile FontLibrary fonts;

        Shared(boolean imagesInMemory) {
            this.imagesInMemory = imagesInMemory;
        }

        /** The fonts of the last scan, scanning them the first time and again when the files are not those it read. */
        FontLibrary fonts(Resources resources) {
            FontLibrary library = fonts;
            if (library != null && library.stamps().equals(FontLibrary.stamps(resources))) {
                return library;
            }
            synchronized (this) {
                // If another thread scanned in the meantime, its scan is at least as recent as the look above.
                if (fonts == library) {
                    fonts = FontLibrary.scan(resources);
                }
                return fonts;
            }
        }
    }

    /**
     * Existing file of the resources folder, from a path relative to it
     * ({@code images/logo.png}). Accepts {@code /} and {@code \} as separators.
     *
     * @param relative the path inside the resources folder, not blank
     * @return the absolute, normalized path of the file (its links are checked but not resolved)
     * @throws ResourceException if the path is absolute, contains {@code ..}, leaves the folder
     *                           through a link or does not exist
     */
    public Path resolve(String relative) {
        return resolve("", relative);
    }

    /**
     * Existing file of a subfolder of the resources ({@code images}), from a path
     * relative to it ({@code logos/acme.png}), with the same checks as
     * {@link #resolve(String)}: the path cannot leave the subfolder.
     *
     * @param folder the subfolder, relative to the root; {@code ""} for the root itself
     * @param relative the path inside {@code folder}, not blank
     * @return the absolute, normalized path of the file (its links are checked but not resolved)
     * @throws ResourceException if the path is absolute, contains {@code ..}, leaves the
     *                           subfolder (also through a link) or does not exist
     */
    public Path resolve(String folder, String relative) {
        if (relative.isBlank()) {
            throw new ResourceException("Empty resource path");
        }
        if (relative.startsWith("/") || relative.startsWith("\\") || relative.startsWith("~")
                || DRIVE.matcher(relative).lookingAt()) {
            throw new ResourceException("Absolute resource path: " + relative
                    + " (paths are relative to the resources folder)");
        }
        for (String segment : relative.split("[/\\\\]")) {
            if (segment.equals("..")) {
                throw new ResourceException("Backward resource path: " + relative
                        + " ('..' is not allowed)");
            }
        }
        Path base = root.resolve(folder).normalize();
        Path path = base.resolve(relative.replace('\\', '/')).normalize();
        if (!path.startsWith(base)) {
            throw new ResourceException("The path " + relative + " leaves the folder " + base);
        }
        if (!Files.exists(path)) {
            throw new ResourceException("The resource " + relative + " does not exist in " + base);
        }
        return checkInside(path, base);
    }

    /**
     * Checks that a file of the folder, once symbolic links are resolved, is still
     * inside it.
     *
     * @throws ResourceException if it is a link that leaves the folder
     */
    Path checkInside(Path path) {
        return checkInside(path, root);
    }

    /** Like {@link #checkInside(Path)}, inside a subfolder of the resources. */
    private Path checkInside(Path path, Path base) {
        try {
            if (!path.toRealPath().startsWith(base.toRealPath())) {
                throw new ResourceException("The resource " + root.relativize(path)
                        + " is a link that leaves the " + (base.equals(root) ? "resources folder" : "folder " + base.getFileName())
                        + " (" + base + ")");
            }
            return path;
        } catch (IOException e) {
            throw new ResourceException("Cannot resolve the resource " + root.relativize(path) + ": " + e, e);
        }
    }
}
