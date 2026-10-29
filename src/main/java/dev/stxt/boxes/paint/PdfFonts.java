package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.resources.Resources;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Las fuentes de un PDF y cómo se escribe el texto con ellas (AR-06). Cada fuente TTF de la carpeta de recursos
 * (ESPECIFICACION.md §5.11) se incrusta una vez por documento y combinación de {@link FontFace}, como subconjunto
 * (solo los glifos usados) en una fuente Type 0 con codificación Identity-H: el PDF se ve igual en cualquier
 * máquina. Con Identity-H cada carácter ocupa dos bytes y el operador {@code Tw} no actúa, así que la
 * justificación y el interletraje van en los argumentos de {@code TJ} ({@link #spaced}).
 */
final class PdfFonts {

    private final PDDocument doc;
    private final Resources resources;
    /** Fuentes incrustadas en el documento, compartidas por todas sus páginas. */
    private final Map<FontFace, PDFont> fonts = new HashMap<>();

    PdfFonts(PDDocument doc, Resources resources) {
        this.doc = doc;
        this.resources = resources;
    }

    /** La fuente incrustada; se carga la primera vez que se usa en el documento. */
    PDFont font(FontFace font) throws IOException {
        PDFont f = fonts.get(font);
        if (f == null) {
            // Sin ligaduras se quita la tabla GSUB: PDFBox escribe un glifo por carácter, que es lo que se midió.
            TrueTypeFont ttf = new TTFParser().parse(new RandomAccessReadBuffer(
                    Files.readAllBytes(resources.fonts().find(font).path())));
            ttf.setEnableGsub(font.ligatures());
            f = PDType0Font.load(doc, ttf, true);
            fonts.put(font, f);
        }
        return f;
    }

    /**
     * Argumento de {@code TJ}: el texto partido tras cada espacio, con el espacio extra entre
     * los trozos. Un número en {@code TJ} desplaza a la izquierda, en milésimas del cuerpo.
     */
    static Object[] justified(String text, double wordSpacing, double fontSize) {
        return spaced(text, wordSpacing, 0, fontSize);
    }

    /**
     * Argumento de {@code TJ} con el espacio extra de {@code justify} tras cada espacio y el
     * interletraje tras cada carácter, salvo el último. Sin interletraje, el texto se parte solo
     * tras los espacios; con él, un trozo por carácter. Se usa {@code TJ} y no {@code Tc}/{@code Tw}
     * porque las fuentes son Type 0 Identity-H, donde {@code Tw} no actúa.
     */
    static Object[] spaced(String text, double wordSpacing, double letterSpacing, double fontSize) {
        if (letterSpacing != 0) {
            List<Object> parts = new ArrayList<>();
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                parts.add(new String(Character.toChars(cp)));
                if (i < text.length()) {
                    parts.add((float) (-(letterSpacing + (cp == ' ' ? wordSpacing : 0)) * 1000 / fontSize));
                }
            }
            return parts.toArray();
        }
        Float shift = (float) (-wordSpacing * 1000 / fontSize);
        List<Object> parts = new ArrayList<>();
        int start = 0;
        for (int i = text.indexOf(' '); i >= 0; i = text.indexOf(' ', i + 1)) {
            parts.add(text.substring(start, i + 1));
            parts.add(shift);
            start = i + 1;
        }
        parts.add(text.substring(start));
        return parts.toArray();
    }
}
