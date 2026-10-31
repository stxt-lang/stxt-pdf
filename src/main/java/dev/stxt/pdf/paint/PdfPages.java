package dev.stxt.pdf.paint;

import dev.stxt.pdf.resources.PdfInspector;
import org.apache.pdfbox.contentstream.PDContentStream;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3CharProc;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Copies pages of another PDF into the document, each one as a form (reference/components/image.md).
 *
 * <p>{@link LayerUtility} copies the content of the page and its resources (a resource shared by several pages,
 * once); annotations are not copied. Then the tagging of the source PDF is removed, since in the document it would
 * mean nothing and would clash with the document's own: the marked content ({@code BMC}, {@code BDC}, {@code EMC},
 * {@code MP}, {@code DP}), except that of optional content ({@code /OC}), which decides what is seen, from the content
 * of the page and of the forms, tiling patterns and glyphs of Type 3 fonts it uses, and the entry of the forms and
 * images in the structure tree ({@code StructParent}, {@code StructParents}). The form is tagged later by the
 * {@code Image} that draws it, as a figure or as decoration.</p>
 *
 * <p>{@link LayerUtility} also copies to the catalog the layers of the source PDF (its optional content), which decide
 * what is seen; a layer configuration without a name receives one, that of the file, and loses its automatic states
 * ({@code AS}), because PDF/UA requires both (ISO 14289-1, 7.10).</p>
 *
 * <p>The form has the crop of the page ({@code BBox}) and a matrix that takes it, already rotated
 * ({@code Rotate}), to a rectangle with its bottom left corner at the origin, of the size given by
 * {@link dev.stxt.pdf.resources.ImageFile#width(int)}.</p>
 */
final class PdfPages {

    private static final COSName STRUCT_PARENT = COSName.getPDFName("StructParent");
    private static final COSName STRUCT_PARENTS = COSName.getPDFName("StructParents");
    private static final COSName OC = COSName.getPDFName("OC");
    private static final COSName AS = COSName.getPDFName("AS");
    private static final Set<String> MARKS = Set.of(OperatorName.BEGIN_MARKED_CONTENT,
            OperatorName.BEGIN_MARKED_CONTENT_SEQ, OperatorName.END_MARKED_CONTENT, OperatorName.MARKED_CONTENT_POINT,
            OperatorName.MARKED_CONTENT_POINT_WITH_PROPS);

    private final PDDocument doc;
    private final LayerUtility layers;
    /**
     * The forms, patterns and glyph procedures already cleaned: the copied resources are shared among the pages of the
     * same PDF.
     */
    private final Set<COSBase> cleaned = new HashSet<>();

    PdfPages(PDDocument doc) {
        this.doc = doc;
        this.layers = new LayerUtility(doc);
    }

    /**
     * Page {@code number} (from 1) of {@code source}, copied into a form; {@code name} is the name of the file, which
     * the layer configurations without a name receive.
     */
    PDFormXObject page(PDDocument source, int number, String name) throws IOException {
        PDPage page = source.getPage(number - 1);
        PDFormXObject form = layers.importPageAsForm(source, page);
        PDRectangle crop = page.getCropBox();
        form.setBBox(crop);
        form.setMatrix(placement(crop, PdfInspector.rotation(page)));
        clean(form);
        nameLayers(name);
        return form;
    }

    /**
     * Gives {@code name} to the layer configurations of the document that have no name, and removes their automatic
     * states ({@code AS}): the layers keep the state the configuration gives them, and a viewer no longer turns them on
     * or off by itself when zooming, printing or exporting.
     */
    private void nameLayers(String name) {
        COSDictionary properties = doc.getDocumentCatalog().getCOSObject().getCOSDictionary(COSName.OCPROPERTIES);
        if (properties == null) {
            return;
        }
        List<COSDictionary> configurations = new ArrayList<>();
        COSDictionary initial = properties.getCOSDictionary(COSName.D);
        if (initial != null) {
            configurations.add(initial);
        }
        COSArray others = properties.getCOSArray(COSName.getPDFName("Configs"));
        if (others != null) {
            for (int i = 0; i < others.size(); i++) {
                if (others.getObject(i) instanceof COSDictionary configuration) {
                    configurations.add(configuration);
                }
            }
        }
        for (COSDictionary configuration : configurations) {
            if (configuration.getString(COSName.NAME) == null) {
                configuration.setString(COSName.NAME, name);
            }
            configuration.removeItem(AS);
        }
    }

    /**
     * The matrix that takes the crop of the page, rotated {@code degrees} clockwise as a viewer shows it, to a
     * rectangle with its bottom left corner at the origin.
     */
    static AffineTransform placement(PDRectangle crop, int degrees) {
        double x = crop.getLowerLeftX();
        double y = crop.getLowerLeftY();
        double w = crop.getWidth();
        double h = crop.getHeight();
        // AffineTransform(a, b, c, d, e, f): x' = a·x + c·y + e, y' = b·x + d·y + f.
        return switch (degrees) {
            case 90 -> new AffineTransform(0, -1, 1, 0, -y, w + x);
            case 180 -> new AffineTransform(-1, 0, 0, -1, w + x, h + y);
            case 270 -> new AffineTransform(0, 1, -1, 0, h + y, -x);
            default -> new AffineTransform(1, 0, 0, 1, -x, -y);
        };
    }

    /** Removes the tagging of the form and, once each, of what it uses. */
    private void clean(PDFormXObject form) throws IOException {
        if (!cleaned.add(form.getCOSObject())) {
            return;
        }
        form.getCOSObject().removeItem(STRUCT_PARENT);
        form.getCOSObject().removeItem(STRUCT_PARENTS);
        unmark(form, form.getContentStream());
        clean(form.getResources());
    }

    /**
     * Removes, once each, the tagging of the forms and images of some resources, and the marks of their tiling patterns
     * and of the glyphs of their Type 3 fonts.
     */
    private void clean(PDResources resources) throws IOException {
        if (resources == null) {
            return;
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject object = resources.getXObject(name);
            if (object instanceof PDFormXObject inner) {
                clean(inner);
            } else if (object != null) {
                object.getCOSObject().removeItem(STRUCT_PARENT);
            }
        }
        for (COSName name : resources.getPatternNames()) {
            PDAbstractPattern pattern = resources.getPattern(name);
            if (pattern instanceof PDTilingPattern tiling && cleaned.add(tiling.getCOSObject())) {
                unmark(tiling, tiling.getContentStream());
                clean(tiling.getResources());
            }
        }
        for (COSName name : resources.getFontNames()) {
            PDFont font = resources.getFont(name);
            if (font instanceof PDType3Font type3 && cleaned.add(type3.getCOSObject())) {
                COSDictionary procedures = type3.getCharProcs();
                if (procedures != null) {
                    for (COSName glyph : procedures.keySet()) {
                        if (procedures.getDictionaryObject(glyph) instanceof COSStream stream) {
                            PDType3CharProc procedure = new PDType3CharProc(type3, stream);
                            unmark(procedure, procedure.getContentStream());
                        }
                    }
                }
                clean(type3.getResources());
            }
        }
    }

    /**
     * Rewrites a content stream (that of a form, a tiling pattern or a glyph) without the content marks, except those
     * of optional content.
     */
    private static void unmark(PDContentStream content, PDStream data) throws IOException {
        List<Object> tokens = new PDFStreamParser(content).parse();
        List<Object> kept = new ArrayList<>(tokens.size());
        List<Object> operands = new ArrayList<>();
        // For each open mark, whether it was kept (to keep its EMC as well).
        Deque<Boolean> open = new ArrayDeque<>();
        boolean changed = false;
        for (Object token : tokens) {
            if (!(token instanceof Operator operator)) {
                operands.add(token);
                continue;
            }
            String name = operator.getName();
            boolean keep = true;
            if (name.equals(OperatorName.BEGIN_MARKED_CONTENT_SEQ)) {
                keep = !operands.isEmpty() && OC.equals(operands.get(0));
                open.push(keep);
            } else if (name.equals(OperatorName.END_MARKED_CONTENT)) {
                // An EMC without its beginning is not written: the content would be unbalanced.
                keep = !open.isEmpty() && open.pop();
            } else if (MARKS.contains(name)) {
                keep = false;
                if (name.equals(OperatorName.BEGIN_MARKED_CONTENT)) {
                    open.push(false);
                }
            }
            if (keep) {
                kept.addAll(operands);
                kept.add(operator);
            } else {
                changed = true;
            }
            operands.clear();
        }
        kept.addAll(operands);
        while (!open.isEmpty()) {
            if (open.pop()) {
                kept.add(Operator.getOperator(OperatorName.END_MARKED_CONTENT));
            }
        }
        if (!changed) {
            return;
        }
        try (OutputStream out = data.createOutputStream(COSName.FLATE_DECODE)) {
            new ContentStreamWriter(out).writeTokens(kept);
        }
    }
}
