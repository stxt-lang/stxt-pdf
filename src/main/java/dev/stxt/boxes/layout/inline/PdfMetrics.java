package dev.stxt.boxes.layout.inline;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Métricas reales de las 14 fuentes estándar de PDF, con PDFBox:
 * {@code ancho = font.getStringWidth(texto) / 1000 × tamaño}. Es la métrica que corresponde a
 * la salida de {@link dev.stxt.boxes.paint.PdfCanvas}: {@code new Engine(new PdfMetrics())}.
 *
 * <p>{@code Font} admite los nombres de las fuentes estándar ({@code Helvetica},
 * {@code Helvetica-Bold}, {@code Times-Roman}, {@code Courier}…) y los alias que reconoce
 * PDFBox ({@code Arial}, {@code TimesNewRoman}, {@code CourierNew}…). Los caracteres que la
 * fuente no puede codificar (WinAnsi) se sustituyen por {@code ?}, aquí y al pintar.</p>
 */
public final class PdfMetrics implements TextMeasurer {

    private final Map<String, PDFont> fonts = new ConcurrentHashMap<>();

    @Override
    public double width(String text, String font, double fontSize) {
        PDFont f = fonts.computeIfAbsent(font, PdfMetrics::standardFont);
        try {
            return f.getStringWidth(encodable(f, text)) / 1000 * fontSize;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Una de las 14 fuentes estándar, por su nombre o un alias.
     *
     * @throws IllegalArgumentException si no es una fuente estándar
     */
    public static PDFont standardFont(String name) {
        Standard14Fonts.FontName standard = Standard14Fonts.getMappedFontName(name);
        if (standard == null) {
            throw new IllegalArgumentException("Fuente desconocida: " + name
                    + " (en la salida PDF se admiten las 14 fuentes estándar: Helvetica, Times-Roman, Courier…)");
        }
        return new PDType1Font(standard);
    }

    /** El texto con {@code ?} en lugar de los caracteres que la fuente no puede codificar. */
    public static String encodable(PDFont font, String text) {
        StringBuilder sb = new StringBuilder(text.length());
        text.codePoints().forEach(cp -> {
            String c = new String(Character.toChars(cp));
            try {
                font.encode(c);
                sb.append(c);
            } catch (IllegalArgumentException | IOException e) {
                sb.append('?');
            }
        });
        return sb.toString();
    }
}
