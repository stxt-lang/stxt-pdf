package dev.stxt.pdf.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Images of the {@code images/} folder (reference/components/image.md), by their path relative to
 * it ({@code logos/acme.png}). Each file is read only once (except in a race between threads, which keeps one of the
 * reads). It is thread-safe.
 */
public final class ImageLibrary {

    private final Resources resources;
    private final Map<String, ImageFile> files = new ConcurrentHashMap<>();

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
        ImageFile file = files.get(path);
        if (file == null) {
            // Two threads that request the same image at the same time both read it and one is kept: they are equal.
            file = ImageFile.read(resources.resolve("images", path), path, resources);
            ImageFile previous = files.putIfAbsent(path, file);
            if (previous != null) {
                file = previous;
            }
        }
        return file;
    }
}
