package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.model.Paint;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDFormContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.function.PDFunction;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroup;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The gradients of the PDF ({@code Background: linear(...)}, reference/common/box.md): the axial shading with
 * its colors and, if any of them is not opaque, the luminosity soft mask with the opacities, because PDF has no
 * per-color opacity in a shading. It only builds the dictionaries; {@link PdfCanvas} paints them.
 */
final class PdfGradients {

    private PdfGradients() {
    }

    /**
     * A graphics state with a luminosity soft mask: the gradient of the opacities in grays, in
     * {@code bbox} (the box being painted, in PDF coordinates).
     */
    static PDExtendedGraphicsState opacityMask(PDDocument doc, Paint.Linear linear, PDRectangle bbox,
                                               double[] pdfLine) throws IOException {
        PDTransparencyGroup form = new PDTransparencyGroup(doc);
        form.setBBox(bbox);
        form.setResources(new PDResources());
        COSDictionary group = new COSDictionary();
        group.setItem(COSName.S, COSName.getPDFName("Transparency"));
        group.setItem(COSName.getPDFName("CS"), COSName.DEVICEGRAY);
        form.getCOSObject().setItem(COSName.getPDFName("Group"), group);
        try (PDFormContentStream mask = new PDFormContentStream(form)) {
            mask.shadingFill(axial(PDDeviceGray.INSTANCE, pdfLine, linear.stops(), true));
        }
        COSDictionary softMask = new COSDictionary();
        softMask.setItem(COSName.TYPE, COSName.getPDFName("Mask"));
        softMask.setItem(COSName.S, COSName.getPDFName("Luminosity"));
        softMask.setItem(COSName.getPDFName("G"), form.getCOSObject());
        PDExtendedGraphicsState state = new PDExtendedGraphicsState();
        state.getCOSObject().setItem(COSName.getPDFName("SMask"), softMask);
        return state;
    }

    /**
     * An axial shading along {@code line} ({@code x1 y1 x2 y2}, in PDF coordinates) with the
     * colors evenly spaced and extended beyond the ends; with
     * {@code opacities}, in grays with the opacity of each color.
     */
    static PDShading axial(PDColorSpace space, double[] line, List<String> stops,
                                   boolean opacities) throws IOException {
        PDShadingType2 shading = new PDShadingType2(new COSDictionary());
        shading.setShadingType(PDShading.SHADING_TYPE2);
        shading.setColorSpace(space);
        COSArray coords = new COSArray();
        for (double c : line) {
            coords.add(new COSFloat((float) c));
        }
        shading.setCoords(coords);
        COSArray extend = new COSArray();
        extend.add(COSBoolean.TRUE);
        extend.add(COSBoolean.TRUE);
        shading.setExtend(extend);
        shading.setFunction(PDFunction.create(stitched(stops, opacities)));
        return shading;
    }

    /** The components of a color in the color space of the shading: RGB, or its opacity in gray. */
    private static float[] components(String color, boolean opacity) {
        float[] rgba = Colors.rgba(color);
        return opacity ? new float[]{rgba[3]} : new float[]{rgba[0], rgba[1], rgba[2]};
    }

    /** One function per segment between two colors, stitched into one of type 3 (or that of the only segment). */
    private static COSDictionary stitched(List<String> stops, boolean opacities) {
        int n = stops.size();
        COSArray domain = floats(0, 1);
        List<COSDictionary> parts = new ArrayList<>();
        for (int i = 0; i + 1 < n; i++) {
            COSDictionary f = new COSDictionary();
            f.setInt(COSName.getPDFName("FunctionType"), 2);
            f.setItem(COSName.getPDFName("Domain"), domain);
            f.setItem(COSName.getPDFName("C0"), floats(components(stops.get(i), opacities)));
            f.setItem(COSName.getPDFName("C1"), floats(components(stops.get(i + 1), opacities)));
            f.setInt(COSName.getPDFName("N"), 1);
            parts.add(f);
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        COSDictionary f = new COSDictionary();
        f.setInt(COSName.getPDFName("FunctionType"), 3);
        f.setItem(COSName.getPDFName("Domain"), domain);
        COSArray functions = new COSArray();
        COSArray bounds = new COSArray();
        COSArray encode = new COSArray();
        for (int i = 0; i < parts.size(); i++) {
            functions.add(parts.get(i));
            if (i > 0) {
                bounds.add(new COSFloat((float) i / parts.size()));
            }
            encode.add(new COSFloat(0));
            encode.add(new COSFloat(1));
        }
        f.setItem(COSName.getPDFName("Functions"), functions);
        f.setItem(COSName.getPDFName("Bounds"), bounds);
        f.setItem(COSName.getPDFName("Encode"), encode);
        return f;
    }

    private static COSArray floats(float... values) {
        COSArray array = new COSArray();
        for (float v : values) {
            array.add(new COSFloat(v));
        }
        return array;
    }
}
