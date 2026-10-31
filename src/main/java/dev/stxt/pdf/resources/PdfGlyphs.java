package dev.stxt.pdf.resources;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.contentstream.operator.text.BeginText;
import org.apache.pdfbox.contentstream.operator.text.EndText;
import org.apache.pdfbox.contentstream.operator.text.MoveText;
import org.apache.pdfbox.contentstream.operator.text.MoveTextSetLeading;
import org.apache.pdfbox.contentstream.operator.text.NextLine;
import org.apache.pdfbox.contentstream.operator.text.SetCharSpacing;
import org.apache.pdfbox.contentstream.operator.text.SetFontAndSize;
import org.apache.pdfbox.contentstream.operator.text.SetTextHorizontalScaling;
import org.apache.pdfbox.contentstream.operator.text.SetTextLeading;
import org.apache.pdfbox.contentstream.operator.text.SetTextRenderingMode;
import org.apache.pdfbox.contentstream.operator.text.SetTextRise;
import org.apache.pdfbox.contentstream.operator.text.SetWordSpacing;
import org.apache.pdfbox.contentstream.operator.text.ShowText;
import org.apache.pdfbox.contentstream.operator.text.ShowTextAdjusted;
import org.apache.pdfbox.contentstream.operator.text.ShowTextLine;
import org.apache.pdfbox.contentstream.operator.text.ShowTextLineAndSpace;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3CharProc;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.font.PDVectorFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Walks the content of a page of a PDF, with its forms, the glyph procedures of its Type 3 fonts and its tiling
 * patterns, and finds the first glyph it draws that PDF/UA does not allow (ISO 14289-1, 7.21.5, 7.21.7 and 7.21.8):
 * the {@code .notdef} glyph, a character code that does not say which character it is, or a width that differs from
 * that of the font program by more than one unit in 1000. Each glyph of a font, each form, each glyph procedure and
 * each pattern are looked at once, so a PDF that draws the same thing many times costs what it draws once. The fonts
 * that are not embedded are left to {@link PdfInspector}, which rejects them before.
 */
final class PdfGlyphs extends PDFStreamEngine {

    /** The codes already looked at, per font. */
    private final Map<COSBase, Set<Integer>> seen = new HashMap<>();
    /** The forms, glyph procedures and patterns already walked. */
    private final Set<COSBase> walked = new HashSet<>();
    private String problem;

    private PdfGlyphs() {
        addOperator(new BeginText(this));
        addOperator(new EndText(this));
        addOperator(new SetFontAndSize(this));
        addOperator(new ShowText(this));
        addOperator(new ShowTextAdjusted(this));
        addOperator(new ShowTextLine(this));
        addOperator(new ShowTextLineAndSpace(this));
        addOperator(new MoveText(this));
        addOperator(new MoveTextSetLeading(this));
        addOperator(new NextLine(this));
        addOperator(new SetMatrix(this));
        addOperator(new SetCharSpacing(this));
        addOperator(new SetWordSpacing(this));
        addOperator(new SetTextHorizontalScaling(this));
        addOperator(new SetTextLeading(this));
        addOperator(new SetTextRenderingMode(this));
        addOperator(new SetTextRise(this));
        addOperator(new Save(this));
        addOperator(new Restore(this));
        addOperator(new Concatenate(this));
        addOperator(new SetGraphicsStateParameters(this));
        addOperator(new DrawObject(this));
    }

    /** Why a glyph of the page is not allowed, for the message; {@code null} if none. */
    static String problem(PDPage page) throws IOException {
        PdfGlyphs glyphs = new PdfGlyphs();
        glyphs.processPage(page);
        List<PDTilingPattern> patterns = new ArrayList<>();
        patterns(page.getResources(), patterns, new HashSet<>());
        for (PDTilingPattern pattern : patterns) {
            if (glyphs.problem != null) {
                break;
            }
            glyphs.processChildStream(pattern, page);
        }
        return glyphs.problem;
    }

    /**
     * The tiling patterns of some resources, of the forms, the Type 3 fonts and the patterns they use: they are not
     * drawn by walking the content, only when something is painted with them.
     */
    private static void patterns(PDResources resources, List<PDTilingPattern> found, Set<COSBase> visited)
            throws IOException {
        if (resources == null || !visited.add(resources.getCOSObject())) {
            return;
        }
        for (COSName name : resources.getPatternNames()) {
            PDAbstractPattern pattern = resources.getPattern(name);
            if (pattern instanceof PDTilingPattern tiling) {
                found.add(tiling);
                patterns(tiling.getResources(), found, visited);
            }
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject object = resources.getXObject(name);
            if (object instanceof PDFormXObject form) {
                patterns(form.getResources(), found, visited);
            }
        }
        for (COSName name : resources.getFontNames()) {
            if (resources.getFont(name) instanceof PDType3Font type3) {
                patterns(type3.getResources(), found, visited);
            }
        }
    }

    @Override
    public void showForm(PDFormXObject form) throws IOException {
        if (problem == null && walked.add(form.getCOSObject())) {
            super.showForm(form);
        }
    }

    @Override
    protected void showType3Glyph(Matrix matrix, PDType3Font font, int code, Vector displacement) throws IOException {
        if (problem != null || !firstTime(font, code)) {
            return;
        }
        if (!hasUnicode(font, code)) {
            problem = withoutUnicode(font, code);
            return;
        }
        PDType3CharProc procedure = font.getCharProc(code);
        if (procedure != null && walked.add(procedure.getCOSObject())) {
            super.showType3Glyph(matrix, font, code, displacement);
        }
    }

    @Override
    protected void showFontGlyph(Matrix matrix, PDFont font, int code, Vector displacement) throws IOException {
        if (problem != null || !font.isEmbedded() || !firstTime(font, code)) {
            return;
        }
        if (font instanceof PDVectorFont vector && !vector.hasGlyph(code)) {
            problem = "draws with the font " + font.getName() + " the character code " + code + ", which the font "
                    + "does not have (it would show its .notdef glyph): the PDF has to be created again with a font "
                    + "that has every character of its text";
        } else if (!hasUnicode(font, code)) {
            problem = withoutUnicode(font, code);
        } else if (getGraphicsState().getTextState().getRenderingMode() != RenderingMode.NEITHER
                && Math.abs(font.getWidth(code) - font.getWidthFromFont(code)) > 1) {
            problem = "uses the font " + font.getName() + " with a width for the character code " + code + " that is "
                    + "not the one of the font, which PDF/UA does not allow. The PDF has to be created again, for "
                    + "example as PDF/A";
        }
    }

    /** Whether the code of the font has not been looked at yet; it is recorded. */
    private boolean firstTime(PDFont font, int code) {
        return seen.computeIfAbsent(font.getCOSObject(), f -> new HashSet<>()).add(code);
    }

    /** Whether the font says which character the code is (ISO 32000-1, 9.10.2). */
    private static boolean hasUnicode(PDFont font, int code) throws IOException {
        String unicode = font.toUnicode(code);
        return unicode != null && !unicode.isEmpty();
    }

    private static String withoutUnicode(PDFont font, int code) {
        return "uses the font " + font.getName() + " without saying which character the code " + code + " is "
                + "(neither its ToUnicode nor its encoding do): its text could not be copied or read aloud. The PDF "
                + "has to be created again, for example as PDF/A";
    }
}
