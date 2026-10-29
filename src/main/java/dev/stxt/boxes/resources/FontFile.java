package dev.stxt.boxes.resources;

import dev.stxt.boxes.model.DecorationMetrics;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.VerticalMetrics;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.ttf.gsub.GsubWorker;
import org.apache.fontbox.ttf.gsub.GsubWorkerFactory;
import org.apache.fontbox.ttf.model.GsubData;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Un fichero TTF de la carpeta de fuentes, ya leído, con sus métricas (ESPECIFICACION.md §5.11).
 *
 * <p>Los anchos son los que escribe PDFBox en el PDF al incrustar la fuente (el avance de cada
 * glifo en milésimas del cuerpo, redondeado): así el texto mide al maquetar lo mismo que al
 * pintarlo. No se aplica el kerning. Con ligaduras (ver {@link #width(String, double, boolean)}) el texto
 * se mide con los mismos glifos que PDFBox escribe, porque se aplica la misma tabla GSUB.</p>
 *
 * <p>Es seguro entre hilos y no usa cerrojos al medir: al abrir la fuente se calcula el avance de
 * cada glifo y el de cada carácter se memoriza al pedirlo en una tabla de enteros, cuyas escrituras
 * son idempotentes (dos hilos escriben el mismo valor en la misma casilla, y un entero se escribe
 * entero).</p>
 */
public final class FontFile {

    /** Los caracteres del plano básico, que tienen su tabla de avances. */
    private static final int BMP = 0x10000;
    /** Palabras que se memorizan como mucho con ligaduras; al pasar de aquí se vacía la memoria. */
    private static final int MAX_LIGATURE_WORDS = 50_000;
    /** Avance aún sin calcular en {@link #bmpAdvance}. */
    private static final int UNKNOWN = -1;
    private static final byte HAS = 1;
    private static final byte LACKS = 2;

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
    /** Si la fuente tiene un glifo para cada carácter del plano básico: 0 sin mirar, {@link #HAS} o {@link #LACKS}. */
    private final byte[] bmpHas = new byte[BMP];
    /** Las palabras que ya se han medido con ligaduras, con su ancho en milésimas: aplicar GSUB es caro. */
    private final ConcurrentHashMap<String, Integer> ligatureWords = new ConcurrentHashMap<>();
    /** Lo mismo que aplica PDFBox al pintar; {@code null} hasta que se pide y si la fuente no tiene GSUB. */
    private volatile GsubWorker gsub;
    private volatile boolean gsubRead;
    /** Ver {@link #digest()}; {@code null} hasta que se pide. */
    private volatile String digest;

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

    /**
     * La huella del contenido del fichero (SHA-256 en hexadecimal, en minúsculas): dos ficheros con los mismos bytes
     * tienen la misma, estén donde estén, y dos distintos no. Se calcula la primera vez que se pide.
     *
     * @throws ResourceException si el fichero ya no se puede leer
     */
    public String digest() {
        String value = digest;
        if (value == null) {
            try {
                value = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
            } catch (IOException e) {
                throw new ResourceException("No se puede leer la fuente " + path + ": " + e.getMessage(), e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("La JVM no tiene SHA-256", e);
            }
            // Dos hilos pueden calcularla a la vez: los dos escriben la misma cadena.
            digest = value;
        }
        return value;
    }

    /** Ascendente y descendente de la tabla {@code hhea}. */
    public VerticalMetrics vertical() {
        return vertical;
    }

    /** Subrayado (tabla {@code post}) y tachado (tabla {@code OS/2}). */
    public DecorationMetrics decoration() {
        return decoration;
    }

    /**
     * Si la fuente tiene un glifo para el carácter. Los del plano básico se memorizan: se pregunta por cada carácter
     * del documento antes de maquetar (PF-06). Como {@link #bmpAdvance}, varios hilos pueden escribir la misma
     * respuesta a la vez.
     */
    public boolean has(int codePoint) {
        if (codePoint >= BMP) {
            return cmap.getGlyphId(codePoint) != 0;
        }
        byte known = bmpHas[codePoint];
        if (known == 0) {
            known = cmap.getGlyphId(codePoint) != 0 ? HAS : LACKS;
            bmpHas[codePoint] = known;
        }
        return known == HAS;
    }

    /** Ancho del texto en puntos, sin ligaduras. Los caracteres sin glifo miden lo que el glifo {@code .notdef}. */
    public double width(String text, double fontSize) {
        return width(text, fontSize, false);
    }

    /**
     * Ancho del texto en puntos. Con {@code ligatures}, cada palabra (lo que hay entre dos espacios en blanco)
     * se mide con los glifos que quedan después de aplicar la tabla GSUB de la fuente, que son los que
     * PDFBox escribe al pintar: {@code fi} puede ser un solo glifo, de otro ancho. Una fuente sin GSUB, o
     * una palabra con algún carácter sin glifo, se mide como siempre.
     */
    public double width(String text, double fontSize, boolean ligatures) {
        GsubWorker worker = ligatures ? ligatureWorker() : null;
        long thousandths = 0;
        for (int i = 0; i < text.length(); ) {
            if (worker != null && !isSpace(text.charAt(i))) {
                int end = i;
                while (end < text.length() && !isSpace(text.charAt(end))) {
                    end++;
                }
                thousandths += ligatureWord(worker, text.substring(i, end));
                i = end;
                continue;
            }
            int cp = text.codePointAt(i);
            thousandths += advance(cp);
            i += Character.charCount(cp);
        }
        return thousandths / 1000.0 * fontSize;
    }

    /** Lo que PDFBox toma por espacio al partir el texto en palabras antes de aplicar GSUB ({@code \s}). */
    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
    }

    /** El ancho de una palabra con las ligaduras de la fuente, en milésimas del cuerpo. */
    private int ligatureWord(GsubWorker worker, String word) {
        Integer known = ligatureWords.get(word);
        if (known != null) {
            return known;
        }
        int[] codePoints = word.codePoints().toArray();
        List<Integer> glyphs = new ArrayList<>(codePoints.length);
        boolean complete = true;
        for (int cp : codePoints) {
            int glyph = cmap.getGlyphId(cp);
            complete &= glyph > 0;
            glyphs.add(glyph);
        }
        int thousandths = 0;
        if (complete) {
            for (int glyph : worker.applyTransforms(glyphs)) {
                thousandths += glyphAdvance(glyph);
            }
        } else {
            for (int cp : codePoints) {
                thousandths += advance(cp);
            }
        }
        if (ligatureWords.size() > MAX_LIGATURE_WORDS) {
            ligatureWords.clear();
        }
        ligatureWords.put(word, thousandths);
        return thousandths;
    }

    /** El aplicador de GSUB de la fuente, o {@code null} si no tiene tabla GSUB. */
    private GsubWorker ligatureWorker() {
        if (!gsubRead) {
            synchronized (this) {
                if (!gsubRead) {
                    try {
                        GsubData data = ttf.getGsubData();
                        gsub = data == GsubData.NO_DATA_FOUND ? null
                                : new GsubWorkerFactory().getGsubWorker(cmap, data);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    gsubRead = true;
                }
            }
        }
        return gsub;
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
