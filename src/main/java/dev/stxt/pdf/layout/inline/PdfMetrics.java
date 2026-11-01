package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.resources.FontFile;
import dev.stxt.pdf.resources.FontLibrary;
import dev.stxt.pdf.resources.ResourceException;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dev.stxt.pdf.tree.PropertyKey.CODE_FONT;
import static dev.stxt.pdf.tree.PropertyKey.FONT;
import static dev.stxt.pdf.tree.PropertyKey.FONT_STYLE;
import static dev.stxt.pdf.tree.PropertyKey.FONT_WEIGHT;

/**
 * Real metrics of the TTF and OTF fonts of the resources folder (reference/common/resources.md): they are the
 * metrics that match the output of {@link dev.stxt.pdf.paint.PdfCanvas}, which embeds
 * those same fonts: {@code new Engine(new PdfMetrics(resources))}.
 *
 * <p>{@link #validate(Box)} checks before the layout that each text has its font (family,
 * weight and style) and that the font has all its characters; there are no fallback fonts. A text with runs
 * (reference/common/text-formatting.md) needs the variant of each run, and each run, its characters in it. A text
 * that may be split with a hyphen ({@code Hyphenate: true}, or a soft hyphen in it) needs the hyphen
 * ({@link LineBreaker#HYPHEN}) in the font of each run, and the soft hyphen itself is never painted, so it is not
 * looked for.</p>
 */
public final class PdfMetrics implements TextMeasurer {

    /** What {@code {page}} and {@code {pages}} can paint. */
    private static final String DIGITS = "0123456789";

    private final Resources resources;
    /**
     * The last font requested and its file: the text of a box is almost always measured with the same one, and
     * looking it up in {@link FontLibrary} for each word was noticeable. It can be read and written from several
     * threads without a lock: the entry is a {@code record}, with final fields, and in the worst case it is looked up
     * again.
     */
    private Found last;

    private record Found(FontFace face, FontFile file) {
    }

    /** Metrics of the fonts of the default resources folder. */
    public PdfMetrics() {
        this(new Resources());
    }

    public PdfMetrics(Resources resources) {
        this.resources = resources;
    }

    @Override
    public double width(String text, FontFace font, double fontSize) {
        return file(font).width(text, fontSize, font.ligatures());
    }

    private FontFile file(FontFace font) {
        Found found = last;
        if (found == null || !found.face().equals(font)) {
            found = new Found(font, resources.fonts().find(font));
            last = found;
        }
        return found.file();
    }

    /** Those of the {@code hhea} table of the font, the same ones {@link dev.stxt.pdf.paint.PdfCanvas} paints with. */
    @Override
    public VerticalMetrics vertical(FontFace font) {
        return file(font).vertical();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The errors of the folder itself (that it does not exist, an unreadable file...) do not belong to the
     * document: they are {@link ResourceException}, without a line.</p>
     */
    @Override
    public void validate(Box root) {
        validate(root, resources.fonts());
    }

    private static void validate(Box box, FontLibrary fonts) {
        if (box.hasText()) {
            check(box, fonts);
        }
        if (box.marker() != null) {
            check(box.marker(), fonts);
        }
        box.children().forEach(child -> validate(child, fonts));
    }

    private static void check(Box box, FontLibrary fonts) {
        Style s = box.style();
        boolean hyphens = s.typography().hyphenate() || box.text().indexOf(LineBreaker.SOFT_HYPHEN) >= 0;
        int start = 0;
        for (TextRun run : box.runs()) {
            FontFile font = font(box, fonts, run.format().face(s.typography().fontFace()), start);
            String text = run.text();
            if (hyphens) {
                requireGlyph(box, font, LineBreaker.HYPHEN.charAt(0), start);
            }
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                if (cp == LineBreaker.SOFT_HYPHEN) {
                    i++;
                    continue;
                }
                if (cp == '{') {
                    // {page}, {pages} and the references start with a brace: only then are they looked for.
                    int end = markerEnd(text, i);
                    if (end > i) {
                        // A page or a number, painted with digits (or letters, in lists).
                        for (char d : DIGITS.toCharArray()) {
                            requireGlyph(box, font, d, start + i);
                        }
                        i = end;
                        continue;
                    }
                }
                // The white-space characters of the document are painted as spaces (reference/common/typography.md).
                requireGlyph(box, font, Character.isWhitespace(cp) ? ' ' : cp, start + i);
                i += Character.charCount(cp);
            }
            start += text.length();
        }
    }

    /** Where the {@code {page}}, {@code {pages}} or reference starting at {@code i} ends; {@code i} if none. */
    private static int markerEnd(String text, int i) {
        if (text.startsWith(PageNumbers.PAGES, i)) {
            return i + PageNumbers.PAGES.length();
        }
        if (text.startsWith(PageNumbers.PAGE, i)) {
            return i + PageNumbers.PAGE.length();
        }
        Reference reference = Reference.at(text, i);
        return reference != null ? reference.end() : i;
    }

    /**
     * The font of a run that starts at character {@code index} of the text. If the variant of the box
     * does not exist, the error points to the text and the declarations; if it is the one of a formatted
     * run, to the line of the run.
     */
    private static FontFile font(Box box, FontLibrary fonts, FontFace face, int index) {
        Style s = box.style();
        try {
            return fonts.find(face);
        } catch (ResourceException e) {
            if (!fonts.families().contains(face.family())) {
                if (!face.family().equals(s.typography().font())) {
                    // The family of a code run: the culprit is the declaration of Code font.
                    throw new InvalidModelException(s.line(CODE_FONT), ErrorCode.FONT_UNKNOWN,
                            e.getMessage() + " (the code font, Code font)");
                }
                // The culprit is the declaration of Font, even if it is in an ancestor.
                throw new InvalidModelException(s.line(FONT), ErrorCode.FONT_UNKNOWN, e.getMessage());
            }
            if (!face.equals(s.typography().fontFace())) {
                throw new InvalidModelException(box.textLineAt(index), ErrorCode.FONT_VARIANT_MISSING,
                        e.getMessage() + " (text with " + face.variant() + " formatting)");
            }
            throw new InvalidModelException(box.textLine(), ErrorCode.FONT_VARIANT_MISSING,
                    e.getMessage() + " (" + declarations(s) + ")");
        }
    }

    private static void requireGlyph(Box box, FontFile font, int cp, int index) {
        if (!font.has(cp)) {
            throw new InvalidModelException(box.textLineAt(index), ErrorCode.GLYPH_MISSING,
                    "The font " + font.face() + " has no character " + describe(cp));
        }
    }

    /** Where the family, the weight and the style are declared, for the messages. */
    private static String declarations(Style s) {
        List<String> parts = new ArrayList<>();
        for (String p : List.of(FONT, FONT_WEIGHT, FONT_STYLE)) {
            if (s.isDeclared(p)) {
                String name = p.equals(FONT) ? "Font" : p.equals(FONT_WEIGHT) ? "Bold or Weight" : "Italic";
                parts.add(name + " on " + s.line(p));
            }
        }
        return String.join(", ", parts);
    }

    /** {@code '→' (U+2192)}; control characters, only with their code. */
    private static String describe(int cp) {
        String code = String.format(Locale.ROOT, "U+%04X", cp);
        return Character.isISOControl(cp) ? code : "'" + new String(Character.toChars(cp)) + "' (" + code + ")";
    }
}
