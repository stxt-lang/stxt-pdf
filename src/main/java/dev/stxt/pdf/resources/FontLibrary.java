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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Índice de las fuentes de la carpeta {@code fonts/} (referencia/comun/recursos.md), por familia,
 * peso y estilo.
 *
 * <p>Se recorren la carpeta y sus subcarpetas buscando ficheros {@code .ttf}. De cada uno se
 * leen la familia (la tipográfica, {@code nameID} 16, o si no la hay la 1), el peso
 * ({@code usWeightClass} de la tabla {@code OS/2}: 400 es {@code normal} y 700 {@code bold}) y
 * el estilo (el bit de cursiva de {@code fsSelection}). Los ficheros de otros pesos (300, 500…)
 * se leen pero no se pueden usar, porque {@code Weight} solo admite {@code normal} y
 * {@code bold}.</p>
 *
 * <p>Son errores: que no exista la carpeta, un fichero que no se puede leer, una fuente
 * variable, dos ficheros con la misma familia, peso y estilo, y un enlace que sale de la
 * carpeta de recursos.</p>
 */
public final class FontLibrary {

    /** Familia tipográfica en la tabla {@code name}. */
    private static final int TYPOGRAPHIC_FAMILY = 16;

    private static final int NORMAL_WEIGHT = 400;
    private static final int BOLD_WEIGHT = 700;

    private final Path folder;
    private final Map<FontFace, FontFile> faces;
    /** Todas las familias encontradas, incluidas las que solo tienen otros pesos. */
    private final Set<String> families;

    private FontLibrary(Path folder, Map<FontFace, FontFile> faces, Set<String> families) {
        this.folder = folder;
        this.faces = Map.copyOf(faces);
        this.families = Set.copyOf(families);
    }

    static FontLibrary scan(Resources resources) {
        Path folder = resources.fontsFolder();
        if (!Files.isDirectory(folder)) {
            throw new ResourceException("No existe la carpeta de fuentes " + folder);
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
                throw new ResourceException("Dos ficheros para la fuente " + entry.font().face() + ": "
                        + folder.relativize(previous.path()) + " y " + folder.relativize(file));
            }
        }
        return new FontLibrary(folder, faces, families);
    }

    /**
     * El fichero de una fuente.
     *
     * @throws ResourceException si no hay ninguna fuente de esa familia o no tiene esa variante
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
                    .map(f -> " ¿Querías decir '" + f + "'?")
                    .orElse("");
            throw new ResourceException("Fuente desconocida: " + face.family() + "." + similar
                    + " Familias en " + folder + ": " + (families.isEmpty() ? "ninguna" : String.join(", ", families)));
        }
        String variants = faces.keySet().stream()
                .filter(f -> f.family().equals(face.family()))
                .map(FontFace::variant)
                .sorted()
                .collect(Collectors.joining(", "));
        throw new ResourceException("La fuente " + face.family() + " no tiene la variante " + face.variant()
                + ". Variantes en " + folder + ": " + (variants.isEmpty() ? "ninguna" : variants));
    }

    /** El fichero de una fuente, o {@code null} si no está. */
    FontFile lookup(FontFace requested) {
        // Un fichero sirve con y sin ligaduras: se identifica sin esa opción.
        return faces.get(requested.base());
    }

    /** Todas las fuentes que se pueden usar, en orden de familia y variante. */
    public List<FontFile> all() {
        return faces.values().stream()
                .sorted(java.util.Comparator.comparing((FontFile f) -> f.face().family())
                        .thenComparing(f -> f.face().variant()))
                .toList();
    }

    /** Familias encontradas, en orden alfabético. */
    public Set<String> families() {
        return new TreeSet<>(families);
    }

    /** Familia de un fichero y la fuente, o {@code null} si su peso no es 400 ni 700. */
    private record Entry(String family, FontFile font) {
    }

    private static Entry read(Path file) {
        TrueTypeFont ttf;
        try {
            ttf = new TTFParser().parse(new RandomAccessReadBuffer(Files.readAllBytes(file)));
        } catch (IOException e) {
            throw new ResourceException("No se puede leer la fuente " + file + ": " + e.getMessage(), e);
        }
        try {
            if (ttf.getTableMap().containsKey("fvar")) {
                throw new ResourceException("La fuente " + file + " es variable: usa los ficheros TTF estáticos de cada variante");
            }
            String family = ttf.getNaming().getName(TYPOGRAPHIC_FAMILY, NameRecord.PLATFORM_WINDOWS,
                    NameRecord.ENCODING_WINDOWS_UNICODE_BMP, NameRecord.LANGUAGE_WINDOWS_EN_US);
            if (family == null) {
                family = ttf.getNaming().getFontFamily();
            }
            if (family == null || family.isBlank()) {
                throw new ResourceException("La fuente " + file + " no declara su familia");
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
            throw new ResourceException("No se puede leer la fuente " + file + ": " + e.getMessage(), e);
        }
    }
}
