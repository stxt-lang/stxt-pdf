package dev.stxt.pdf.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Images of the {@code images/} folder (reference/components/image.md), by their path relative to
 * it ({@code logos/acme.png}). Each file is read only once (except in a race between threads, which keeps one of the
 * reads), whatever the spelling of its path ({@code logo.png}, {@code ./logo.png} and, where the file system does not
 * tell capitals from small letters, {@code Logo.png} are the same image). It is thread-safe.
 *
 * <p>It lives as long as its {@link Resources}, which a service shares among all its conversions, so it does not grow
 * with what the documents write: the images are kept by their real path, one per file of the folder, and the spellings
 * already seen are only a shortcut to them, emptied when it reaches {@link #MAX_SPELLINGS}.</p>
 */
public final class ImageLibrary {

    /** Spellings kept in the shortcut before it is emptied. */
    static final int MAX_SPELLINGS = 1024;

    private final Resources resources;
    /** The images, by real path. */
    private final Map<Path, ImageFile> files = new ConcurrentHashMap<>();
    /** The image of each spelling already seen, to find it without going to the disk. */
    private final Map<String, ImageFile> spellings = new ConcurrentHashMap<>();

    ImageLibrary(Resources resources) {
        this.resources = resources;
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
            file = ImageFile.read(resolved, path, resources);
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

    /** The images read, one per file (for the tests). */
    int files() {
        return files.size();
    }

    /** The spellings in the shortcut (for the tests). */
    int spellings() {
        return spellings.size();
    }
}
