package dev.stxt.boxes.layout.inline;

import dev.stxt.boxes.resources.FontFile;
import dev.stxt.boxes.resources.FontLibrary;
import dev.stxt.boxes.resources.ResourceException;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.FontFace;
import dev.stxt.boxes.tree.InvalidModelException;

import java.util.Locale;

/**
 * Métricas reales de las fuentes TTF de la carpeta de recursos (ESPECIFICACION.md §5.11): es la
 * métrica que corresponde a la salida de {@link dev.stxt.boxes.paint.PdfCanvas}, que incrusta
 * esas mismas fuentes: {@code new Engine(new PdfMetrics(resources))}.
 *
 * <p>{@link #validate(Box)} comprueba antes de maquetar que cada texto tiene su fuente (familia,
 * peso y estilo) y que la fuente tiene todos sus caracteres; no hay fuentes de sustitución.</p>
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
        box.children.forEach(child -> validate(child, fonts));
    }

    private static void check(Box box, FontLibrary fonts) {
        try {
            FontFile font = fonts.find(box.style.fontFace());
            String text = box.text;
            if (PageNumbers.contains(text)) {
                text = text.replace(PageNumbers.PAGES, "").replace(PageNumbers.PAGE, "") + DIGITS;
            }
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                // Los espacios en blanco del documento se pintan como espacios (§5.4).
                int painted = Character.isWhitespace(cp) ? ' ' : cp;
                if (!font.has(painted)) {
                    throw new ResourceException("La fuente " + font.face() + " no tiene el carácter "
                            + describe(painted));
                }
                i += Character.charCount(cp);
            }
        } catch (ResourceException e) {
            throw new InvalidModelException(box.line, e.getMessage());
        }
    }

    /** {@code '→' (U+2192)}; los caracteres de control, solo con su código. */
    private static String describe(int cp) {
        String code = String.format(Locale.ROOT, "U+%04X", cp);
        return Character.isISOControl(cp) ? code : "'" + new String(Character.toChars(cp)) + "' (" + code + ")";
    }
}
