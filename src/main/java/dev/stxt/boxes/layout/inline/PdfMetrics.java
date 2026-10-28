package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.Reference;
import dev.stxt.boxes.model.TextRun;
import dev.stxt.boxes.model.VerticalMetrics;
import dev.stxt.boxes.resources.FontFile;
import dev.stxt.boxes.resources.FontLibrary;
import dev.stxt.boxes.resources.ResourceException;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dev.stxt.boxes.tree.PropertyKey.FONT;
import static dev.stxt.boxes.tree.PropertyKey.FONT_STYLE;
import static dev.stxt.boxes.tree.PropertyKey.FONT_WEIGHT;

/**
 * Métricas reales de las fuentes TTF de la carpeta de recursos (ESPECIFICACION.md §5.11): es la
 * métrica que corresponde a la salida de {@link dev.stxt.boxes.paint.PdfCanvas}, que incrusta
 * esas mismas fuentes: {@code new Engine(new PdfMetrics(resources))}.
 *
 * <p>{@link #validate(Box)} comprueba antes de maquetar que cada texto tiene su fuente (familia,
 * peso y estilo) y que la fuente tiene todos sus caracteres; no hay fuentes de sustitución. Un
 * texto con tramos (§5.4) necesita la variante de cada tramo, y cada tramo, sus caracteres en
 * ella.</p>
 */
public final class PdfMetrics implements TextMeasurer {

    /** Lo que pueden pintar {@code {page}} y {@code {pages}}. */
    private static final String DIGITS = "0123456789";

    private final Resources resources;

    /** Métricas de las fuentes de la carpeta de recursos por defecto. */
    public PdfMetrics() {
        this(new Resources());
    }

    public PdfMetrics(Resources resources) {
        this.resources = resources;
    }

    @Override
    public double width(String text, FontFace font, double fontSize) {
        return resources.fonts().find(font).width(text, fontSize);
    }

    /** Las de la tabla {@code hhea} de la fuente, las mismas con que pinta {@link dev.stxt.boxes.paint.PdfCanvas}. */
    @Override
    public VerticalMetrics vertical(FontFace font) {
        return resources.fonts().find(font).vertical();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Los errores de la propia carpeta (que no exista, un fichero ilegible…) no son del
     * documento: son {@link ResourceException}, sin línea.</p>
     */
    @Override
    public void validate(Box root) {
        validate(root, resources.fonts());
    }

    private static void validate(Box box, FontLibrary fonts) {
        if (box.hasText()) {
            check(box, fonts);
        }
        if (box.marker != null) {
            check(box.marker, fonts);
        }
        box.children.forEach(child -> validate(child, fonts));
    }

    private static void check(Box box, FontLibrary fonts) {
        Style s = box.style;
        int start = 0;
        for (TextRun run : box.runs()) {
            FontFile font = font(box, fonts, run.format().face(s.fontFace()), start);
            String text = run.text();
            for (int i = 0; i < text.length(); ) {
                String marker = text.startsWith(PageNumbers.PAGES, i) ? PageNumbers.PAGES
                        : text.startsWith(PageNumbers.PAGE, i) ? PageNumbers.PAGE : null;
                if (marker != null) {
                    // Se pinta como cifras.
                    for (char d : DIGITS.toCharArray()) {
                        requireGlyph(box, font, d, start + i);
                    }
                    i += marker.length();
                    continue;
                }
                Reference reference = Reference.at(text, i);
                if (reference != null) {
                    // Una página o un número, que se pintan con cifras (o letras, en las listas).
                    for (char d : DIGITS.toCharArray()) {
                        requireGlyph(box, font, d, start + i);
                    }
                    i = reference.end();
                    continue;
                }
                int cp = text.codePointAt(i);
                // Los espacios en blanco del documento se pintan como espacios (§5.4).
                requireGlyph(box, font, Character.isWhitespace(cp) ? ' ' : cp, start + i);
                i += Character.charCount(cp);
            }
            start += text.length();
        }
    }

    /**
     * La fuente de un tramo que empieza en el carácter {@code index} del texto. Si no existe la
     * variante de la caja, el error señala el texto y las declaraciones; si es la de un tramo con
     * formato, la línea del tramo.
     */
    private static FontFile font(Box box, FontLibrary fonts, FontFace face, int index) {
        Style s = box.style;
        try {
            return fonts.find(face);
        } catch (ResourceException e) {
            if (!fonts.families().contains(s.font)) {
                // El culpable es la declaración de Font, aunque esté en una antecesora.
                throw new InvalidModelException(s.line(FONT), ErrorCode.FONT_UNKNOWN, e.getMessage());
            }
            if (!face.equals(s.fontFace())) {
                throw new InvalidModelException(box.textLineAt(index), ErrorCode.FONT_VARIANT_MISSING,
                        e.getMessage() + " (texto con formato " + face.variant() + ")");
            }
            throw new InvalidModelException(box.textLine, ErrorCode.FONT_VARIANT_MISSING,
                    e.getMessage() + " (" + declarations(s) + ")");
        }
    }

    private static void requireGlyph(Box box, FontFile font, int cp, int index) {
        if (!font.has(cp)) {
            throw new InvalidModelException(box.textLineAt(index), ErrorCode.GLYPH_MISSING,
                    "La fuente " + font.face() + " no tiene el carácter " + describe(cp));
        }
    }

    /** Dónde se declaran la familia, el peso y el estilo, para los mensajes. */
    private static String declarations(Style s) {
        List<String> parts = new ArrayList<>();
        for (String p : List.of(FONT, FONT_WEIGHT, FONT_STYLE)) {
            if (s.isDeclared(p)) {
                String name = p.equals(FONT) ? "Font" : p.equals(FONT_WEIGHT) ? "Weight" : "Style";
                parts.add(name + " en la línea " + s.line(p));
            }
        }
        return String.join(", ", parts);
    }

    /** {@code '→' (U+2192)}; los caracteres de control, solo con su código. */
    private static String describe(int cp) {
        String code = String.format(Locale.ROOT, "U+%04X", cp);
        return Character.isISOControl(cp) ? code : "'" + new String(Character.toChars(cp)) + "' (" + code + ")";
    }
}
