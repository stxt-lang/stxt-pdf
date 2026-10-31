package dev.stxt.pdf.resources;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import org.apache.fontbox.ttf.NameRecord;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
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
 * <p>The folder and its subfolders are walked looking for {@code .ttf} files. From each one the
 * family (the typographic one, {@code nameID} 16, or else 1), the weight
 * ({@code usWeightClass} of the {@code OS/2} table: 400 is {@code normal} and 700 {@code bold}) and
 * the style (the italic bit of {@code fsSelection}) are read. Files of other weights (300, 500...)
 * are read but cannot be used, because {@code Bold} only distinguishes
 * normal (400) and bold (700).</p>
 *
 * <p>These are errors: a missing folder, a file that cannot be read, a variable
 * font, two files with the same family, weight and style, and a link that leaves the
 * resources folder.</p>
 */
public final class FontLibrary {

    /** Typographic family in the {@code name} table. */
    private static final int TYPOGRAPHIC_FAMILY = 16;

    private static final int NORMAL_WEIGHT = 400;
    private static final int BOLD_WEIGHT = 700;

    private final Path folder;
    private final Map<FontFace, FontFile> faces;
    /** All the families found, including those that only have other weights, in alphabetical order. */
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
            files = walk.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ttf"))
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
                .map(FontFace::variant)
                .sorted()
                .collect(Collectors.joining(", "));
        throw new ResourceException("The font " + face.family() + " has no variant " + face.variant()
                + ". Variants in " + folder + ": " + (variants.isEmpty() ? "none" : variants));
    }

    /** The file of a font, or {@code null} if it is not there. */
    FontFile lookup(FontFace requested) {
        // A file serves with and without ligatures: it is identified without that option.
        return faces.get(requested.base());
    }

    /** All the fonts that can be used, in order of family and variant. */
    public List<FontFile> all() {
        return faces.values().stream()
                .sorted(java.util.Comparator.comparing((FontFile f) -> f.face().family())
                        .thenComparing(f -> f.face().variant()))
                .toList();
    }

    /** Families found, in alphabetical order. */
    public Set<String> families() {
        return new TreeSet<>(families);
    }

    /** Family of a file and the font, or {@code null} if its weight is neither 400 nor 700. */
    private record Entry(String family, FontFile font) {
    }

    private static Entry read(Path file) {
        TrueTypeFont ttf;
        try {
            ttf = new TTFParser().parse(new RandomAccessReadBuffer(Files.readAllBytes(file)));
        } catch (IOException e) {
            throw new ResourceException("Cannot read the font " + file + ": " + e.getMessage(), e);
        }
        try {
            if (ttf.getTableMap().containsKey("fvar")) {
                throw new ResourceException("The font " + file + " is variable: the static TTF files of each variant are needed");
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
            FontWeight weight = switch (weightClass) {
                case NORMAL_WEIGHT -> FontWeight.NORMAL;
                case BOLD_WEIGHT -> FontWeight.BOLD;
                default -> null;
            };
            return new Entry(family, weight == null ? null : new FontFile(file, new FontFace(family, weight, style), ttf));
        } catch (IOException e) {
            throw new ResourceException("Cannot read the font " + file + ": " + e.getMessage(), e);
        }
    }
}
