package dev.stxt.pdf.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The images of the resources (reference/components/image.md), by the path a document writes for them
 * ({@code logos/acme.png}), whatever the spelling of the path ({@code logo.png}, {@code ./logo.png} and, where the
 * file system does not tell capitals from small letters, {@code Logo.png} are the same image). It is thread-safe.
 *
 * <p>An image comes from the first of these that has its path: the images added for one conversion
 * ({@link Resources#withImages}), the images the resources were given as bytes ({@link Resources.Builder#image}) and
 * the folders of images, each for the paths with its prefix.</p>
 *
 * <p>Each {@link Resources} has its own, and a {@code Resources} and its snapshots ({@link Resources#snapshot()}) share
 * the images read, kept by their real path, one per file of the folder, so they do not grow with what the documents
 * write. A library looks at the file the first time it is asked for it: if another one of the same family already read
 * it and it has the size and the modification time it had then (and, if it is an SVG with text, the fonts have not been
 * indexed again), it uses that image; if not, it reads the file and the new image replaces the old one for the
 * libraries that come after. Then it keeps the image it found while it lives, whatever happens to the file (except in a
 * race between threads, which keeps one of the reads). The spellings already seen are a shortcut to the images, without
 * going to the disk, emptied when it reaches {@link #MAX_SPELLINGS}.</p>
 */
public final class ImageLibrary {

    /** Spellings kept in the shortcut before it is emptied. */
    static final int MAX_SPELLINGS = 1024;

    private final Resources resources;
    /** The images read from a file, by real path, shared by a {@code Resources} and its snapshots. */
    private final Map<Path, ImageFile> shared;
    /** The images given as bytes that have been read, by their path, shared like {@link #shared}. */
    private final Map<String, ImageFile> sharedMemory;
    /** The library of the resources these add the images of a conversion to; {@code null} if they add none. */
    private final ImageLibrary parent;
    /** The images added for a conversion, by their path; empty without {@link #parent}. */
    private final Map<String, byte[]> added;
    /** The images this library found in a file, by real path. */
    private final Map<Path, ImageFile> files = new ConcurrentHashMap<>();
    /** The images this library found among those given as bytes, by their path. */
    private final Map<String, ImageFile> memory = new ConcurrentHashMap<>();
    /** The image of each spelling already seen, to find it without going to the disk. */
    private final Map<String, ImageFile> spellings = new ConcurrentHashMap<>();

    ImageLibrary(Resources resources, Map<Path, ImageFile> shared, Map<String, ImageFile> sharedMemory) {
        this.resources = resources;
        this.shared = shared;
        this.sharedMemory = sharedMemory;
        this.parent = null;
        this.added = Map.of();
    }

    /** The images of {@code parent} and, before them, those of {@code added}. */
    ImageLibrary(Resources resources, ImageLibrary parent, Map<String, byte[]> added) {
        this.resources = resources;
        this.shared = Map.of();
        this.sharedMemory = Map.of();
        this.parent = parent;
        this.added = added;
    }

    /**
     * The bytes of the image at that path if it is a PDF, without opening it or reading it whole, to check its limit
     * before reading it with {@link #find}; 0 if it is of another format.
     *
     * @throws ResourceException if the path leaves its folder, does not exist or cannot be read
     */
    public long pdfBytes(String path) {
        byte[] content = inMemory(path);
        if (content != null) {
            return ImageFile.isPdf(content) ? content.length : 0;
        }
        if (parent != null) {
            return parent.pdfBytes(path);
        }
        Path file = resources.resolveImage(path);
        try {
            return ImageFile.isPdf(file) ? Files.size(file) : 0;
        } catch (IOException e) {
            throw new ResourceException("Cannot read the image " + path + ": " + e.getMessage(), e);
        }
    }

    /** The bytes this library has for that path: those added for a conversion or those the resources were given. */
    private byte[] inMemory(String path) {
        String key = Resources.key(path);
        if (key == null) {
            return null;
        }
        return parent != null ? added.get(key) : resources.imageInMemory(key);
    }

    /**
     * The image at that path of a document.
     *
     * @throws ImageFormatException if it is not PNG, JPEG, PDF or a supported SVG, or cannot be read
     * @throws ResourceException    if the path leaves its folder or does not exist
     */
    public ImageFile find(String path) {
        ImageFile file = spellings.get(path);
        if (file != null) {
            return file;
        }
        byte[] content = inMemory(path);
        if (content != null) {
            file = fromMemory(Resources.key(path), content, path);
        } else if (parent != null) {
            file = parent.find(path);
        } else {
            file = fromFolder(path);
        }
        if (spellings.size() >= MAX_SPELLINGS) {
            spellings.clear();
        }
        spellings.put(path, file);
        return file;
    }

    /**
     * An image given as bytes. Those of the resources are shared with their snapshots, like the files; those added for
     * a conversion are read by each library.
     */
    private ImageFile fromMemory(String key, byte[] content, String path) {
        ImageFile file = memory.get(key);
        if (file == null) {
            ImageFile known = sharedMemory.get(key);
            if (known != null && known.isCurrent(resources)) {
                file = known;
            } else {
                file = ImageFile.inMemory(content, path, resources);
                if (parent == null) {
                    sharedMemory.put(key, file);
                }
            }
            ImageFile previous = memory.putIfAbsent(key, file);
            if (previous != null) {
                file = previous;
            }
        }
        return file;
    }

    /** An image of a folder: the one this library already found for its file, or the file as it is now. */
    private ImageFile fromFolder(String path) {
        Path resolved = resources.resolveImage(path);
        Path real;
        try {
            real = resolved.toRealPath();
        } catch (IOException e) {
            throw new ResourceException("Cannot read the image " + path + ": " + e.getMessage(), e);
        }
        ImageFile file = files.get(real);
        if (file == null) {
            // Two threads that request the same image at the same time both read it and one is kept: they are equal.
            file = current(real, resolved, path);
            ImageFile previous = files.putIfAbsent(real, file);
            if (previous != null) {
                file = previous;
            }
        }
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

    /** The images read from a file, one per file, shared by a {@code Resources} and its snapshots (for the tests). */
    int files() {
        return shared.size();
    }

    /** The spellings in the shortcut (for the tests). */
    int spellings() {
        return spellings.size();
    }
}
