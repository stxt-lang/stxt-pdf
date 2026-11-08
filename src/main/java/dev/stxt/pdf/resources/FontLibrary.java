package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import org.apache.fontbox.ttf.NameRecord;
import org.apache.fontbox.ttf.TrueTypeFont;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Index of the fonts of the {@code fonts/} folder (reference/common/resources.md), by family,
 * weight and style.
 *
 * <p>The folder and its subfolders are walked looking for {@code .ttf} and {@code .otf} files. From each one the
 * family (the typographic one, {@code nameID} 16, or else 1), the weight ({@code usWeightClass} of the {@code OS/2}
 * table, as it is: 400 is the normal one and 700 the bold one) and the style (the italic bit of {@code fsSelection})
 * are read. A file whose weight is outside 1 to 1000 is read but cannot be used, and neither can one whose weight
 * the document cannot write ({@code Weight} goes from 100 to 900 in steps of 100), such as 450.</p>
 *
 * <p>These are errors: a missing folder, a file that cannot be read, a variable
 * font, two files with the same family, weight and style, and a link that leaves the
 * resources folder.</p>
 *
 * <p>It keeps the size and the modification time of each file as it read them ({@link #stamps()}), so that a
 * {@link Resources} can tell whether the folder changed since another one of the same family scanned it
 * ({@link Resources#snapshot()}), without reading the fonts again.</p>
 */
public final class FontLibrary {

    /** Typographic family in the {@code name} table. */
    private static final int TYPOGRAPHIC_FAMILY = 16;

    /** The variants of a family from the lightest to the heaviest, and at each weight the normal one first. */
    private static final Comparator<FontFace> BY_VARIANT = Comparator.comparingInt((FontFace f) -> f.weight().value())
            .thenComparing(FontFace::style);

    /** Where the fonts come from, for the messages: the folder, the memory or both. */
    private final String origin;
    private final Map<FontFace, FontFile> faces;
    /** All the families found, including those whose files cannot be used, in alphabetical order. */
    private final SortedSet<String> families;
    private final List<Stamp> stamps;

    /** A font file as it was when it was read: its path, its size and its modification time. */
    record Stamp(Path path, long size, FileTime modified) {
    }

    private FontLibrary(String origin, Map<FontFace, FontFile> faces, Set<String> families, List<Stamp> stamps) {
        this.origin = origin;
        this.faces = Map.copyOf(faces);
        this.families = Collections.unmodifiableSortedSet(new TreeSet<>(families));
        this.stamps = List.copyOf(stamps);
    }

    static FontLibrary scan(Resources resources) {
        Map<FontFace, FontFile> faces = new HashMap<>();
        Set<String> families = new TreeSet<>();
        List<Stamp> stamps = new ArrayList<>();
        Path folder = resources.fontsFolder();
        Map<String, byte[]> memory = resources.fontsInMemory();
        if (folder == null && memory.isEmpty()) {
            throw new ResourceException("The resources have no fonts: neither a folder of fonts nor fonts in memory");
        }
        List<Entry> entries = new ArrayList<>();
        if (folder != null) {
            for (Path file : files(resources)) {
                resources.checkInside(file);
                // Before reading it: a change while it is being read gives another modification time.
                stamps.add(stamp(file));
                byte[] content;
                try {
                    content = Files.readAllBytes(file);
                } catch (IOException e) {
                    throw new ResourceException("Cannot read the font " + file + ": " + e.getMessage(), e);
                }
                entries.add(read(file, file.toString(), folder.relativize(file).toString(), content));
            }
        }
        // The fonts given as bytes, in the order of their names: what the index has does not depend on how they
        // were given.
        for (String name : new TreeSet<>(memory.keySet())) {
            entries.add(read(null, name + " (in memory)", name + " (in memory)", memory.get(name)));
        }
        Map<FontFace, String> labels = new HashMap<>();
        for (Entry entry : entries) {
            families.add(entry.family());
            if (entry.font() == null) {
                continue;
            }
            FontFile previous = faces.putIfAbsent(entry.font().face(), entry.font());
            if (previous != null) {
                throw new ResourceException("Two files for the font " + entry.font().face() + ": "
                        + labels.get(entry.font().face()) + " and " + entry.label());
            }
            labels.put(entry.font().face(), entry.label());
        }
        String origin = folder == null ? "memory" : memory.isEmpty() ? folder.toString() : folder + " and memory";
        return new FontLibrary(origin, faces, families, stamps);
    }

    /**
     * The font files of the folder as they are now, without reading them, in the order of {@link #stamps()}: what
     * changes when a file is added, removed or replaced.
     *
     * @throws ResourceException if the folder does not exist or cannot be walked
     */
    static List<Stamp> stamps(Resources resources) {
        List<Stamp> stamps = new ArrayList<>();
        if (resources.fontsFolder() == null) {
            // Only fonts given as bytes, which do not change.
            return stamps;
        }
        for (Path file : files(resources)) {
            stamps.add(stamp(file));
        }
        return stamps;
    }

    /** The size and the modification time of each file when it was read, in the order of the path. */
    List<Stamp> stamps() {
        return stamps;
    }

    private static List<Path> files(Resources resources) {
        Path folder = resources.fontsFolder();
        if (!Files.isDirectory(folder)) {
            throw new ResourceException("The fonts folder " + folder + " does not exist");
        }
        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.filter(FontLibrary::isFontFile)
                    .sorted()
                    .toList();
        } catch (IOException | UncheckedIOException e) {
            // The walk reports what it cannot read while it walks as an UncheckedIOException.
            throw new ResourceException("Cannot read the fonts folder " + folder + ": " + e.getMessage(), e);
        }
    }

    private static Stamp stamp(Path file) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            return new Stamp(file, attributes.size(), attributes.lastModifiedTime());
        } catch (IOException e) {
            throw new ResourceException("Cannot read the font " + file + ": " + e.getMessage(), e);
        }
    }

    /** Whether the file is a font by its extension: {@code .ttf} or {@code .otf}, in upper or lower case. */
    private static boolean isFontFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".ttf") || name.endsWith(".otf");
    }

    /**
     * The file of a font.
     *
     * @throws ResourceException if there is no font of that family or it does not have that variant
     */
    public FontFile find(FontFace requested) {
        FontFile file = lookup(requested);
        if (file != null) {
            return file;
        }
        FontFace face = requested.base();
        if (!families.contains(face.family())) {
            String similar = families.stream()
                    .filter(f -> f.equalsIgnoreCase(face.family()))
                    .findFirst()
                    .map(f -> " The closest name is '" + f + "'.")
                    .orElse("");
            throw new ResourceException("Unknown font: " + face.family() + "." + similar
                    + " Families in " + origin + ": " + (families.isEmpty() ? "none" : String.join(", ", families)));
        }
        String variants = faces.keySet().stream()
                .filter(f -> f.family().equals(face.family()))
                .sorted(BY_VARIANT)
                .map(FontFace::variant)
                .collect(Collectors.joining(", "));
        throw new ResourceException("The font " + face.family() + " has no variant " + face.variant()
                + ". Variants in " + origin + ": " + (variants.isEmpty() ? "none" : variants));
    }

    /** The file of a font, or {@code null} if it is not there. */
    FontFile lookup(FontFace requested) {
        // A file serves with and without ligatures and kerning: it is identified without those options.
        return faces.get(requested.base());
    }

    /**
     * {@return every font of the index, one file per family, weight and style, in order of family and variant} It is
     * what {@link #find(FontFace)} can give, plus a file whose weight a document cannot ask for (450), which is indexed
     * but not used: those who list the fonts of a server use it instead of asking for each weight.
     */
    public List<FontFile> all() {
        return faces.values().stream()
                .sorted(Comparator.comparing(FontFile::face,
                        Comparator.comparing(FontFace::family).thenComparing(BY_VARIANT)))
                .toList();
    }


    /** Families found, in alphabetical order. */
    public Set<String> families() {
        return new TreeSet<>(families);
    }

    /**
     * Family of a file and the font, or {@code null} if its weight cannot be requested; {@code label} is how the
     * message of two files for the same font calls it.
     */
    private record Entry(String family, FontFile font, String label) {
    }

    /**
     * @param path  the file; {@code null} for a font given as bytes
     * @param file  how the messages call it
     * @param label how the message of two files for the same font calls it
     */
    private static Entry read(Path path, String file, String label, byte[] content) {
        TrueTypeFont ttf;
        try {
            ttf = FontFile.parse(content);
        } catch (IOException e) {
            throw new ResourceException("Cannot read the font " + file + ": " + e.getMessage(), e);
        }
        try {
            if (ttf.getTableMap().containsKey("fvar")) {
                throw new ResourceException("The font " + file
                        + " is variable: the static files of each variant are needed");
            }
            String family = ttf.getNaming().getName(TYPOGRAPHIC_FAMILY, NameRecord.PLATFORM_WINDOWS,
                    NameRecord.ENCODING_WINDOWS_UNICODE_BMP, NameRecord.LANGUAGE_WINDOWS_EN_US);
            if (family == null) {
                family = ttf.getNaming().getFontFamily();
            }
            if (family == null || family.isBlank()) {
                throw new ResourceException("The font " + file + " does not declare its family");
            }
            family = family.trim();
            int weightClass = ttf.getOS2Windows().getWeightClass();
            FontStyle style = (ttf.getOS2Windows().getFsSelection() & 1) != 0
                    ? FontStyle.ITALIC : FontStyle.NORMAL;
            if (weightClass < FontWeight.MIN || weightClass > FontWeight.MAX) {
                return new Entry(family, null, label);
            }
            return new Entry(family, new FontFile(path, file, new FontFace(family, new FontWeight(weightClass), style),
                    ttf), label);
        } catch (IOException e) {
            throw new ResourceException("Cannot read the font " + file + ": " + e.getMessage(), e);
        }
    }
}
