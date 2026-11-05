package dev.stxt.pdf.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Images of the {@code images/} folder (reference/components/image.md), by their path relative to
 * it ({@code logos/acme.png}), whatever the spelling of the path ({@code logo.png}, {@code ./logo.png} and, where the
 * file system does not tell capitals from small letters, {@code Logo.png} are the same image). It is thread-safe.
 *
 * <p>Each {@link Resources} has its own, and a {@code Resources} and its snapshots ({@link Resources#snapshot()}) share
 * the images read, kept by their real path, one per file of the folder, so they do not grow with what the documents
 * write. A library looks at the file the first time it is asked for it: if another one of the same family already read
 * it and it has the size and the modification time it had then (and, if it is an SVG with text, the fonts have not been
 * scanned again), it uses that image; if not, it reads the file and the new image replaces the old one for the
 * libraries that come after. Then it keeps the image it found while it lives, whatever happens to the file (except in a
 * race between threads, which keeps one of the reads). The spellings already seen are a shortcut to the images, without
 * going to the disk, emptied when it reaches {@link #MAX_SPELLINGS}.</p>
 */
public final class ImageLibrary {

    /** Spellings kept in the shortcut before it is emptied. */
    static final int MAX_SPELLINGS = 1024;

    private final Resources resources;
    /** The images read, by real path, shared by a {@code Resources} and its snapshots. */
    private final Map<Path, ImageFile> shared;
    /** The images this library found, by real path. */
    private final Map<Path, ImageFile> files = new ConcurrentHashMap<>();
    /** The image of each spelling already seen, to find it without going to the disk. */
    private final Map<String, ImageFile> spellings = new ConcurrentHashMap<>();

    ImageLibrary(Resources resources, Map<Path, ImageFile> shared) {
        this.resources = resources;
        this.shared = shared;
    }

    /**
     * The bytes of the file at that path if it is a PDF, without opening it or reading it whole, to check its limit
     * before reading it with {@link #find}; 0 if it is of another format.
     *
     * @throws ResourceException if the path leaves the folder, does not exist or cannot be read
     */
    public long pdfBytes(String path) {
        Path file = resources.resolve("images", path);
        try {
            return ImageFile.isPdf(file) ? Files.size(file) : 0;
        } catch (IOException e) {
            throw new ResourceException("Cannot read the image " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * The image at that path, relative to {@code images/}.
     *
     * @throws ImageFormatException if it is not PNG, JPEG, PDF or a supported SVG, or cannot be read
     * @throws ResourceException    if the path leaves the folder or does not exist
     */
    public ImageFile find(String path) {
        ImageFile file = spellings.get(path);
        if (file != null) {
            return file;
        }
        Path resolved = resources.resolve("images", path);
        Path real;
        try {
            real = resolved.toRealPath();
        } catch (IOException e) {
            throw new ResourceException("Cannot read the image " + path + ": " + e.getMessage(), e);
        }
        file = files.get(real);
        if (file == null) {
            // Two threads that request the same image at the same time both read it and one is kept: they are equal.
            file = current(real, resolved, path);
            ImageFile previous = files.putIfAbsent(real, file);
            if (previous != null) {
                file = previous;
            }
        }
        if (spellings.size() >= MAX_SPELLINGS) {
            spellings.clear();
        }
        spellings.put(path, file);
        return file;
    }

    /** The image as the file is now: the one already read if it has not changed, or the file read again. */
    private ImageFile current(Path real, Path resolved, String path) {
        ImageFile known = shared.get(real);
        if (known != null && known.isCurrent(resources)) {
            return known;
        }
        ImageFile file = ImageFile.read(resolved, path, resources);
        shared.put(real, file);
        return file;
    }

    /** The images read, one per file, shared by a {@code Resources} and its snapshots (for the tests). */
    int files() {
        return shared.size();
    }

    /** The spellings in the shortcut (for the tests). */
    int spellings() {
        return spellings.size();
    }
}
