package dev.stxt.pdf.resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The resources of the documents (reference/common/resources.md): the fonts and the images. By default they are a
 * folder, {@code resources/} relative to the working directory, with the fonts in {@code fonts/} and the images in
 * {@code images/}; {@link #Resources(Path)} changes the folder, and {@link #builder()} puts them together from
 * several places: a folder of fonts, one or more folders of images, each with the prefix its paths start with, and
 * fonts and images given as bytes (reference/implementation/api.md). {@link #withImages} adds the images of one
 * conversion.
 *
 * <p>Files are always looked up <strong>downward</strong>: the path of an image cannot be absolute, have {@code ..}
 * segments or leave its folder through a symbolic link.</p>
 *
 * <p><strong>Changes in the folders</strong> (reference/implementation/api.md): a {@code Resources} reads the fonts
 * and each image the first time they are requested and then keeps them while it lives, whatever happens to the files.
 * {@link #snapshot()} gives another one, which looks at the folders as they are then and shares with this one what was
 * already read: the fonts are indexed again if a file of the folder of fonts was added, removed or replaced, and each
 * image is read again if its file changed. {@link dev.stxt.pdf.convert.Converter} takes a snapshot when it is created
 * and another one in {@link dev.stxt.pdf.convert.Converter#reloaded()}, or one for each conversion if it is
 * {@linkplain dev.stxt.pdf.convert.Converter.Builder#reloadResourcesEachConversion(boolean) built so}. An SVG is
 * painted with the bytes that were read; a PNG, a JPEG or a PDF that changed or was removed between laying out and
 * painting makes the conversion fail ({@link ImageFile#checkUnchanged()}), unless {@link #withImagesInMemory()} keeps
 * its bytes too. What was given as bytes does not change.</p>
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

    /** A folder of images and the prefix of the paths that are looked up in it; {@code ""} for those without one. */
    private record Mount(String prefix, Path folder) {
    }

    /**
     * Where the fonts and the images come from, the same for a {@code Resources} and its snapshots.
     *
     * @param root         the single folder of {@link Resources#Resources(Path)}; {@code null} if built from parts
     * @param fontsFolder  the folder of the fonts; {@code null} if there is none
     * @param imageFolders the folders of the images, the longest prefix first
     * @param fonts        the fonts given as bytes, by their name
     * @param images       the images given as bytes, by their path ({@link #key})
     */
    private record Sources(Path root, Path fontsFolder, List<Mount> imageFolders, Map<String, byte[]> fonts,
                           Map<String, byte[]> images) {
    }

    private final Sources sources;
    /** What this object and its snapshots share. */
    private final Shared shared;
    /** The resources these ones add the images of a conversion to ({@link #withImages}); {@code null} if none. */
    private final Resources base;
    /** The images of a conversion, by their path ({@link #key}); empty without {@link #base}. */
    private final Map<String, byte[]> added;
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
        this(rooted(root.toAbsolutePath().normalize()), new Shared(false), null, Map.of());
    }

    private Resources(Sources sources, Shared shared, Resources base, Map<String, byte[]> added) {
        this.sources = sources;
        this.shared = shared;
        this.base = base;
        this.added = added;
    }

    private static Sources rooted(Path root) {
        return new Sources(root, root.resolve("fonts"), List.of(new Mount("", root.resolve("images"))), Map.of(),
                Map.of());
    }

    /**
     * {@return a builder of resources that come from several places} A folder of fonts, folders of images with the
     * prefix of their paths, and fonts and images given as bytes (reference/implementation/api.md).
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@return resources of the same places that keep in memory the bytes of each image they read, also of a PNG, a
     * JPEG or a PDF, and paint it with them} An image is then painted as it was read, even if its file changes or is
     * removed afterwards, without looking at the disk again (reference/implementation/api.md); it costs keeping in
     * memory every image read, as large as its file. By default only an SVG is kept, and a PNG, a JPEG or a PDF is
     * read again when painting. The new object and its snapshots do not share what this one already read.
     */
    public Resources withImagesInMemory() {
        if (base != null) {
            return new Resources(sources, shared, base.withImagesInMemory(), added);
        }
        return new Resources(sources, new Shared(true), null, Map.of());
    }

    /** {@return whether it keeps in memory the bytes of every image it reads ({@link #withImagesInMemory()})} */
    public boolean imagesInMemory() {
        return base != null ? base.imagesInMemory() : shared.imagesInMemory;
    }

    /**
     * {@return a view of the same places, which shares with this object (and with its other snapshots) the fonts and
     * the images already read} Those whose files have not changed are not read again. Like any {@code Resources}, it
     * looks at the folders only the first time it is asked for its fonts or for each image, and then keeps what it
     * found while it lives, so that one conversion does not mix two versions of a file. Creating it costs nothing
     * until then.
     */
    public Resources snapshot() {
        if (base != null) {
            return new Resources(sources, shared, base.snapshot(), added);
        }
        return new Resources(sources, shared, null, Map.of());
    }

    /**
     * {@return these resources with the images of {@code images} added, each at the path a document writes for it}
     * They are the images of one conversion (reference/implementation/api.md): the bytes of what a document brings
     * with it, which are in no folder. An added image goes before one of these resources with the same path. The rest
     * is these resources as they are: the same fonts, and the same images, already read or not, without looking at
     * the folders again. Each added image is read when a document uses it, and an error in it is of that document.
     *
     * <p>The map is copied, but not its arrays, which must not change afterwards:
     * {@link dev.stxt.pdf.convert.Converter#withImages} copies them.</p>
     *
     * @param images the content of each image by its path, relative and without {@code ..}; not {@code null}
     * @throws IllegalArgumentException if a path is blank, absolute or has {@code ..}
     */
    public Resources withImages(Map<String, byte[]> images) {
        Map<String, byte[]> all = new LinkedHashMap<>(added);
        images.forEach((path, content) -> all.put(imageKey(path), Objects.requireNonNull(content, path)));
        return new Resources(sources, shared, base != null ? base : this, Map.copyOf(all));
    }

    /**
     * {@return the single folder of the resources, absolute and normalized} {@code null} if they come from several
     * places ({@link #builder()}).
     */
    public Path root() {
        return sources.root();
    }

    /** {@return the folder of the fonts: {@code fonts/} inside the root, or the one given; {@code null} if none} */
    public Path fontsFolder() {
        return sources.fontsFolder();
    }

    /**
     * {@return the folder of the images whose path has no prefix: {@code images/} inside the root, or the one given}
     * {@code null} if every folder of images has a prefix, or there is none.
     */
    public Path imagesFolder() {
        for (Mount mount : sources.imageFolders()) {
            if (mount.prefix().isEmpty()) {
                return mount.folder();
            }
        }
        return null;
    }

    /**
     * {@return the path of every image a document can use now, as the document writes it, in order} Those of the
     * folders, with their prefix, and those given as bytes. A folder that does not exist or cannot be walked gives
     * none.
     */
    public List<String> imagePaths() {
        TreeSet<String> paths = new TreeSet<>();
        for (Mount mount : sources.imageFolders()) {
            if (!Files.isDirectory(mount.folder())) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(mount.folder())) {
                for (Path file : walk.filter(Files::isRegularFile).toList()) {
                    String relative = mount.folder().relativize(file).toString().replace('\\', '/');
                    paths.add(mount.prefix().isEmpty() ? relative : mount.prefix() + "/" + relative);
                }
            } catch (IOException | UncheckedIOException e) {
                // A folder that cannot be walked is reported by the documents that use its images.
            }
        }
        paths.addAll(sources.images().keySet());
        paths.addAll(added.keySet());
        return List.copyOf(paths);
    }

    /**
     * The fonts, looked for the first time they are requested: the last index of this object or of its snapshots if
     * the files of the folder are still those it read, or a new one.
     *
     * @throws ResourceException if there are no fonts, the folder does not exist or some font is not valid
     */
    public FontLibrary fonts() {
        if (base != null) {
            return base.fonts();
        }
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
     * The images, each one looked for the first time it is requested: the one this object or its snapshots already
     * read if the file has not changed, or the file read again.
     */
    public ImageLibrary images() {
        ImageLibrary library = images;
        if (library == null) {
            synchronized (this) {
                library = images;
                if (library == null) {
                    library = base != null ? new ImageLibrary(this, base.images(), added)
                            : new ImageLibrary(this, shared.images, shared.memory);
                    images = library;
                }
            }
        }
        return library;
    }

    /** The fonts given as bytes, by their name. */
    Map<String, byte[]> fontsInMemory() {
        return sources.fonts();
    }

    /** The content of the image given as bytes for that path of a document; {@code null} if there is none. */
    byte[] imageInMemory(String key) {
        return sources.images().get(key);
    }

    /** What a {@code Resources} and its snapshots share: the last index of the fonts and the images read. */
    private static final class Shared {

        /** The images read from a file, by real path; the one whose file changed is replaced when it is read again. */
        final Map<Path, ImageFile> images = new ConcurrentHashMap<>();
        /** The images given as bytes that have been read, by their path. */
        final Map<String, ImageFile> memory = new ConcurrentHashMap<>();
        /** Whether every image keeps its bytes ({@link Resources#withImagesInMemory()}), the same for the whole family. */
        final boolean imagesInMemory;
        private volatile FontLibrary fonts;

        Shared(boolean imagesInMemory) {
            this.imagesInMemory = imagesInMemory;
        }

        /** The fonts of the last index, indexing them the first time and again when the files are not those it read. */
        FontLibrary fonts(Resources resources) {
            FontLibrary library = fonts;
            if (library != null && library.stamps().equals(FontLibrary.stamps(resources))) {
                return library;
            }
            synchronized (this) {
                // If another thread indexed in the meantime, its index is at least as recent as the look above.
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
     *                           through a link or does not exist, or if the resources have no single folder
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
     *                           subfolder (also through a link) or does not exist, or if the resources have no
     *                           single folder
     */
    public Path resolve(String folder, String relative) {
        if (sources.root() == null) {
            throw new ResourceException("These resources come from several places and have no folder of their own "
                    + "to look up " + relative + " in");
        }
        return inside(sources.root().resolve(folder).normalize(), relative, relative);
    }

    /**
     * The file of an image of a folder, from the path a document writes for it: in the folder with the longest prefix
     * the path starts with, without the prefix, or in the folder without a prefix.
     *
     * @throws ResourceException if the path is absolute, contains {@code ..}, has no folder, leaves it (also through
     *                           a link) or does not exist
     */
    Path resolveImage(String path) {
        check(path);
        String normalized = normalize(path);
        for (Mount mount : sources.imageFolders()) {
            if (mount.prefix().isEmpty()) {
                return inside(mount.folder(), normalized.isEmpty() ? path : normalized, path);
            }
            if (normalized.startsWith(mount.prefix() + "/")) {
                return inside(mount.folder(), normalized.substring(mount.prefix().length() + 1), path);
            }
        }
        List<String> prefixes = sources.imageFolders().stream().map(m -> m.prefix() + "/").toList();
        throw new ResourceException("The resource " + path + " does not exist: "
                + (prefixes.isEmpty() ? "the resources have no folder of images"
                : "no folder of images has its prefix (" + String.join(", ", prefixes) + ")"));
    }

    /** Existing file of {@code base} from a path relative to it; {@code shown} is the path in the messages. */
    private Path inside(Path base, String relative, String shown) {
        check(relative);
        Path path = base.resolve(relative.replace('\\', '/')).normalize();
        if (!path.startsWith(base)) {
            throw new ResourceException("The path " + shown + " leaves the folder " + base);
        }
        if (!Files.exists(path)) {
            throw new ResourceException("The resource " + shown + " does not exist in " + base);
        }
        return checkInside(path, base);
    }

    /** That a path is not blank or absolute and does not go backward. */
    private static void check(String relative) {
        String problem = problem(relative);
        if (problem != null) {
            throw new ResourceException(problem);
        }
    }

    /** Why a path cannot be that of a resource; {@code null} if it can. */
    private static String problem(String relative) {
        if (relative.isBlank()) {
            return "Empty resource path";
        }
        if (relative.startsWith("/") || relative.startsWith("\\") || relative.startsWith("~")
                || DRIVE.matcher(relative).lookingAt()) {
            return "Absolute resource path: " + relative + " (paths are relative to the resources folder)";
        }
        for (String segment : relative.split("[/\\\\]")) {
            if (segment.equals("..")) {
                return "Backward resource path: " + relative + " ('..' is not allowed)";
            }
        }
        return null;
    }

    /** The path with {@code /} as separator and without its empty and {@code .} segments. */
    private static String normalize(String path) {
        StringBuilder out = new StringBuilder(path.length());
        for (String segment : path.split("[/\\\\]")) {
            if (!segment.isEmpty() && !segment.equals(".")) {
                out.append(out.length() == 0 ? "" : "/").append(segment);
            }
        }
        return out.toString();
    }

    /**
     * The path of a document as the images given as bytes are kept by: with {@code /} and without {@code .} segments;
     * {@code null} if it cannot be the path of a resource, which the folders report.
     */
    static String key(String path) {
        return problem(path) == null ? normalize(path) : null;
    }

    /** {@link #key} of the path of an image that is given as bytes, which has to be valid. */
    private static String imageKey(String path) {
        Objects.requireNonNull(path, "path");
        String problem = problem(path);
        String key = problem == null ? normalize(path) : "";
        if (key.isEmpty()) {
            throw new IllegalArgumentException(problem != null ? problem : "Empty resource path: " + path);
        }
        return key;
    }

    /**
     * Checks that a font file, once symbolic links are resolved, is still inside the resources folder or, if the
     * resources come from several places, inside the folder of the fonts.
     *
     * @throws ResourceException if it is a link that leaves the folder
     */
    Path checkInside(Path path) {
        return checkInside(path, sources.root() != null ? sources.root() : sources.fontsFolder());
    }

    /** Checks that a file, once symbolic links are resolved, is still inside {@code base}. */
    private Path checkInside(Path path, Path base) {
        // In the messages, the path from the single folder if there is one.
        Path shown = (sources.root() != null ? sources.root() : base).relativize(path);
        try {
            if (!path.toRealPath().startsWith(base.toRealPath())) {
                throw new ResourceException("The resource " + shown + " is a link that leaves the "
                        + (base.equals(sources.root()) ? "resources folder" : "folder " + base.getFileName())
                        + " (" + base + ")");
            }
            return path;
        } catch (IOException e) {
            throw new ResourceException("Cannot resolve the resource " + shown + ": " + e, e);
        }
    }

    /**
     * Puts together resources that come from several places (reference/implementation/api.md): a folder of fonts,
     * folders of images and fonts and images given as bytes, for what is not in a folder (the files of the classpath
     * of an application, what it has generated). Nothing is read until a conversion asks for it.
     */
    public static final class Builder {

        private Path fontsFolder;
        private final Map<String, Path> imageFolders = new LinkedHashMap<>();
        private final Map<String, byte[]> fonts = new LinkedHashMap<>();
        private final Map<String, byte[]> images = new LinkedHashMap<>();
        private boolean imagesInMemory;

        private Builder() {
        }

        /**
         * The folder of the fonts, walked with its subfolders like {@code fonts/}. It does not need to exist yet.
         *
         * @param folder the folder, absolute or relative to the working directory
         * @return this builder
         * @throws IllegalArgumentException if a folder of fonts was already given
         */
        public Builder fonts(Path folder) {
            if (fontsFolder != null) {
                throw new IllegalArgumentException("The resources already have a folder of fonts: " + fontsFolder);
            }
            fontsFolder = folder.toAbsolutePath().normalize();
            return this;
        }

        /**
         * A font given as bytes, a {@code .ttf} or {@code .otf} file as it is. It is indexed with those of the folder,
         * by the family, the weight and the style it declares.
         *
         * @param name    how the messages call it (its file name), different for each font; not blank
         * @param content the bytes of the file, which are copied
         * @return this builder
         * @throws IllegalArgumentException if the name is blank or was already given
         */
        public Builder font(String name, byte[] content) {
            if (name.isBlank() || fonts.containsKey(name)) {
                throw new IllegalArgumentException(name.isBlank() ? "A font in memory needs a name"
                        : "Two fonts in memory with the name " + name);
            }
            fonts.put(name, content.clone());
            return this;
        }

        /**
         * The folder of the images whose path has none of the prefixes, like {@code images/}: {@code Image: a/b.png}
         * is its file {@code a/b.png}. It does not need to exist yet.
         *
         * @param folder the folder, absolute or relative to the working directory
         * @return this builder
         * @throws IllegalArgumentException if a folder without prefix was already given
         */
        public Builder images(Path folder) {
            return mount("", folder);
        }

        /**
         * A folder of images for the paths that start with {@code prefix}: with the prefix {@code logos},
         * {@code Image: logos/a/b.png} is its file {@code a/b.png}. A path goes to the folder with the longest prefix
         * it starts with.
         *
         * @param prefix one or more path segments, without {@code ..}; not blank
         * @param folder the folder, absolute or relative to the working directory
         * @return this builder
         * @throws IllegalArgumentException if the prefix is not valid or was already given
         */
        public Builder images(String prefix, Path folder) {
            return mount(imageKey(prefix), folder);
        }

        private Builder mount(String prefix, Path folder) {
            if (imageFolders.containsKey(prefix)) {
                throw new IllegalArgumentException("The resources already have a folder of images "
                        + (prefix.isEmpty() ? "without prefix" : "with the prefix " + prefix) + ": "
                        + imageFolders.get(prefix));
            }
            imageFolders.put(prefix, folder.toAbsolutePath().normalize());
            return this;
        }

        /**
         * An image given as bytes: a PNG, a JPEG, an SVG or a PDF as it is, at the path a document writes for it.
         * It goes before a file of a folder with the same path. It is read when a document uses it.
         *
         * @param path    the path of the image in a document, relative and without {@code ..}
         * @param content the bytes of the file, which are copied
         * @return this builder
         * @throws IllegalArgumentException if the path is not valid or was already given
         */
        public Builder image(String path, byte[] content) {
            String key = imageKey(path);
            if (images.containsKey(key)) {
                throw new IllegalArgumentException("Two images in memory with the path " + key);
            }
            images.put(key, content.clone());
            return this;
        }

        /**
         * Whether the resources keep in memory the bytes of every image they read from a folder
         * ({@link Resources#withImagesInMemory()}). Off by default.
         *
         * @param keep {@code true} to keep them
         * @return this builder
         */
        public Builder imagesInMemory(boolean keep) {
            imagesInMemory = keep;
            return this;
        }

        /** {@return the resources, which read nothing until they are asked for a font or an image} */
        public Resources build() {
            List<Mount> mounts = new ArrayList<>();
            imageFolders.forEach((prefix, folder) -> mounts.add(new Mount(prefix, folder)));
            // The longest prefix first, and the folder without prefix last: a path goes to the first that fits.
            mounts.sort(Comparator.comparingInt((Mount m) -> m.prefix().length()).reversed());
            return new Resources(new Sources(null, fontsFolder, List.copyOf(mounts), Map.copyOf(fonts),
                    Map.copyOf(images)), new Shared(imagesInMemory), null, Map.of());
        }
    }
}
