package dev.stxt.boxes.resources;

import dev.stxt.boxes.model.DecorationMetrics;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.VerticalMetrics;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TrueTypeFont;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Un fichero TTF de la carpeta de fuentes, ya leído, con sus métricas (ESPECIFICACION.md §5.11).
 *
 * <p>Los anchos son los que escribe PDFBox en el PDF al incrustar la fuente (el avance de cada
 * glifo en milésimas del cuerpo, redondeado): así el texto mide al maquetar lo mismo que al
 * pintarlo. No se aplica el kerning.</p>
 *
 * <p>Es seguro entre hilos y no usa cerrojos al medir: al abrir la fuente se calcula el avance de
 * cada glifo y el de cada carácter se memoriza al pedirlo en una tabla de enteros, cuyas escrituras
 * son idempotentes (dos hilos escriben el mismo valor en la misma casilla, y un entero se escribe
 * entero).</p>
 */
public final class FontFile {

    /** Los caracteres del plano básico, que tienen su tabla de avances. */
    private static final int BMP = 0x10000;
    /** Avance aún sin calcular en {@link #bmpAdvance}. */
    private static final int UNKNOWN = -1;

    private final Path path;
    private final FontFace face;
    private final TrueTypeFont ttf;
    private final CmapLookup cmap;
    private final double unitsPerEm;
    private final VerticalMetrics vertical;
    private final DecorationMetrics decoration;
    /** Avance de cada glifo en milésimas del cuerpo, redondeado como lo escribe PDFBox. */
    private final int[] glyphAdvance;
    /** Avance de cada carácter del plano básico, o {@link #UNKNOWN}. */
    private final int[] bmpAdvance = new int[BMP];

    FontFile(Path path, FontFace face, TrueTypeFont ttf) throws IOException {
        this.path = path;
        this.face = face;
        this.ttf = ttf;
        this.cmap = ttf.getUnicodeCmapLookup();
        this.unitsPerEm = ttf.getUnitsPerEm();
        this.vertical = new VerticalMetrics(ttf.getHorizontalHeader().getAscender() / unitsPerEm,
                -ttf.getHorizontalHeader().getDescender() / unitsPerEm);
        this.decoration = new DecorationMetrics(ttf.getPostScript().getUnderlinePosition() / unitsPerEm,
                ttf.getPostScript().getUnderlineThickness() / unitsPerEm,
                ttf.getOS2Windows().getStrikeoutPosition() / unitsPerEm,
                ttf.getOS2Windows().getStrikeoutSize() / unitsPerEm);
        this.glyphAdvance = new int[ttf.getNumberOfGlyphs()];
        for (int glyph = 0; glyph < glyphAdvance.length; glyph++) {
            glyphAdvance[glyph] = (int) Math.round(ttf.getAdvanceWidth(glyph) * 1000 / unitsPerEm);
        }
        Arrays.fill(bmpAdvance, UNKNOWN);
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

    /** Subrayado (tabla {@code post}) y tachado (tabla {@code OS/2}). */
    public DecorationMetrics decoration() {
        return decoration;
    }

    /** Si la fuente tiene un glifo para el carácter. */
    public boolean has(int codePoint) {
        return cmap.getGlyphId(codePoint) != 0;
    }

    /** Ancho del texto en puntos. Los caracteres sin glifo miden lo que el glifo {@code .notdef}. */
    public double width(String text, double fontSize) {
        long thousandths = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            thousandths += advance(cp);
            i += Character.charCount(cp);
        }
        return thousandths / 1000.0 * fontSize;
    }

    /** Avance de un carácter, en milésimas del cuerpo. */
    private int advance(int codePoint) {
        if (codePoint >= BMP) {
            return glyphAdvance(cmap.getGlyphId(codePoint));
        }
        int advance = bmpAdvance[codePoint];
        if (advance == UNKNOWN) {
            advance = glyphAdvance(cmap.getGlyphId(codePoint));
            bmpAdvance[codePoint] = advance;
        }
        return advance;
    }

    /** Avance de un glifo, en milésimas del cuerpo. */
    private int glyphAdvance(int glyph) {
        if (glyph >= 0 && glyph < glyphAdvance.length) {
            return glyphAdvance[glyph];
        }
        // Un glifo que no existe en la fuente: lo que contesta fontbox, que no se puede usar entre hilos.
        synchronized (ttf) {
            try {
                return (int) Math.round(ttf.getAdvanceWidth(glyph) * 1000 / unitsPerEm);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
