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
 */
public final class FontLibrary {

    /** Typographic family in the {@code name} table. */
    private static final int TYPOGRAPHIC_FAMILY = 16;

    /** The variants of a family from the lightest to the heaviest, and at each weight the normal one first. */
    private static final Comparator<FontFace> BY_VARIANT = Comparator.comparingInt((FontFace f) -> f.weight().value())
            .thenComparing(FontFace::style);

    private final Path folder;
    private final Map<FontFace, FontFile> faces;
    /** All the families found, including those whose files cannot be used, in alphabetical order. */
    private final SortedSet<String> families;

    private FontLibrary(Path folder, Map<FontFace, FontFile> faces, Set<String> families) {
        this.folder = folder;
        this.faces = Map.copyOf(faces);
        this.families = Collections.unmodifiableSortedSet(new TreeSet<>(families));
    }

    static FontLibrary scan(Resources resources) {
        Path folder = resources.fontsFolder();
        if (!Files.isDirectory(folder)) {
            throw new ResourceException("The fonts folder " + folder + " does not exist");
        }
        List<Path> files;
        try (Stream<Path> walk = Files.walk(folder)) {
            files = walk.filter(FontLibrary::isFontFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<FontFace, FontFile> faces = new HashMap<>();
        Set<String> families = new TreeSet<>();
        for (Path file : files) {
            resources.checkInside(file);
            Entry entry = read(file);
            families.add(entry.family());
            if (entry.font() == null) {
                continue;
            }
            FontFile previous = faces.putIfAbsent(entry.font().face(), entry.font());
            if (previous != null) {
                throw new ResourceException("Two files for the font " + entry.font().face() + ": "
                        + folder.relativize(previous.path()) + " and " + folder.relativize(file));
            }
        }
        return new FontLibrary(folder, faces, families);
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
                    + " Families in " + folder + ": " + (families.isEmpty() ? "none" : String.join(", ", families)));
        }
        String variants = faces.keySet().stream()
                .filter(f -> f.family().equals(face.family()))
                .sorted(BY_VARIANT)
                .map(FontFace::variant)
                .collect(Collectors.joining(", "));
        throw new ResourceException("The font " + face.family() + " has no variant " + face.variant()
                + ". Variants in " + folder + ": " + (variants.isEmpty() ? "none" : variants));
    }

    /** The file of a font, or {@code null} if it is not there. */
    FontFile lookup(FontFace requested) {
        // A file serves with and without ligatures and kerning: it is identified without those options.
        return faces.get(requested.base());
    }

    /** All the fonts that can be used, in order of family and variant. */
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

    /** Family of a file and the font, or {@code null} if its weight cannot be requested. */
    private record Entry(String family, FontFile font) {
    }

    private static Entry read(Path file) {
        TrueTypeFont ttf;
        try {
            ttf = FontFile.parse(Files.readAllBytes(file));
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
                return new Entry(family, null);
            }
            return new Entry(family, new FontFile(file, new FontFace(family, new FontWeight(weightClass), style), ttf));
        } catch (IOException e) {
            throw new ResourceException("Cannot read the font " + file + ": " + e.getMessage(), e);
        }
    }
}
