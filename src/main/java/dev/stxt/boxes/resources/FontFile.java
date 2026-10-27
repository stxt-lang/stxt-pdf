package dev.stxt.boxes.resources;

import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.VerticalMetrics;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TrueTypeFont;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * Un fichero TTF de la carpeta de fuentes, ya leído, con sus métricas (ESPECIFICACION.md §5.11).
 *
 * <p>Los anchos son los que escribe PDFBox en el PDF al incrustar la fuente (el avance de cada
 * glifo en milésimas del cuerpo, redondeado): así el texto mide al maquetar lo mismo que al
 * pintarlo. No se aplican el kerning ni las ligaduras.</p>
 */
public final class FontFile {

    private final Path path;
    private final FontFace face;
    private final TrueTypeFont ttf;
    private final CmapLookup cmap;
    private final double unitsPerEm;
    private final VerticalMetrics vertical;

    FontFile(Path path, FontFace face, TrueTypeFont ttf) throws IOException {
        this.path = path;
        this.face = face;
        this.ttf = ttf;
        this.cmap = ttf.getUnicodeCmapLookup();
        this.unitsPerEm = ttf.getUnitsPerEm();
        this.vertical = new VerticalMetrics(ttf.getHorizontalHeader().getAscender() / unitsPerEm,
                -ttf.getHorizontalHeader().getDescender() / unitsPerEm);
    }

    public Path path() {
        return path;
    }

    public FontFace face() {
        return face;
    }

    /** Ascendente y descendente de la tabla {@code hhea}. */
    public VerticalMetrics vertical() {
        return vertical;
    }

    /** Si la fuente tiene un glifo para el carácter. */
    public boolean has(int codePoint) {
        return cmap.getGlyphId(codePoint) != 0;
    }

    /** Ancho del texto en puntos. Los caracteres sin glifo miden lo que el glifo {@code .notdef}. */
    public synchronized double width(String text, double fontSize) {
        try {
            long thousandths = 0;
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                thousandths += Math.round(ttf.getAdvanceWidth(cmap.getGlyphId(cp)) * 1000 / unitsPerEm);
                i += Character.charCount(cp);
            }
            return thousandths / 1000.0 * fontSize;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
