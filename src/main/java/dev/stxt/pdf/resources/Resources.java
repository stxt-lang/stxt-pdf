package dev.stxt.pdf.resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * <p><strong>Threads:</strong> it can be shared among all conversions and all threads. Fonts and
 * images are read the first time they are requested, with double-checked locking, and are then read without a
 * lock; the folder is scanned only once, so files added later are not seen.</p>
 */
public final class Resources {

    /** Default root, relative to the working directory. */
    public static final Path DEFAULT_ROOT = Path.of("resources");

    /** {@code C:}, {@code d:}...: Windows drive. */
    private static final Pattern DRIVE = Pattern.compile("^[A-Za-z]:");

    private final Path root;
    /** Created the first time they are requested and read without a lock (double-checked locking). */
    private volatile FontLibrary fonts;
    private volatile ImageLibrary images;

    /** Resources in {@link #DEFAULT_ROOT}. */
    public Resources() {
        this(DEFAULT_ROOT);
    }

    public Resources(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    /** Folder of the fonts. */
    public Path fontsFolder() {
        return root.resolve("fonts");
    }

    /** Folder of the images. */
    public Path imagesFolder() {
        return root.resolve("images");
    }

    /**
     * Fonts of {@code fonts/}. They are scanned the first time they are requested.
     *
     * @throws ResourceException if the folder does not exist or some font is not valid
     */
    public FontLibrary fonts() {
        FontLibrary library = fonts;
        if (library == null) {
            synchronized (this) {
                library = fonts;
                if (library == null) {
                    library = FontLibrary.scan(this);
                    fonts = library;
                }
            }
        }
        return library;
    }

    /** Images of {@code images/}, read the first time they are requested. */
    public ImageLibrary images() {
        ImageLibrary library = images;
        if (library == null) {
            synchronized (this) {
                library = images;
                if (library == null) {
                    library = new ImageLibrary(this);
                    images = library;
                }
            }
        }
        return library;
    }

    /**
     * Existing file of the resources folder, from a path relative to it
     * ({@code images/logo.png}). Accepts {@code /} and {@code \} as separators.
     *
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
            throw new UncheckedIOException(e);
        }
    }
}
