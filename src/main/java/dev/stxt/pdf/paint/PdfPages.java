package dev.stxt.pdf.paint;

import dev.stxt.pdf.resources.PdfInspector;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorName;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

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
 * Copia páginas de otro PDF en el documento, cada una como un formulario (referencia/componentes/image.md).
 *
 * <p>{@link LayerUtility} copia el contenido de la página y sus recursos (un recurso compartido por varias páginas,
 * una vez); las anotaciones no se copian. Luego se le quita el etiquetado del PDF de origen, que en el documento no
 * significaría nada y chocaría con el suyo: el contenido marcado ({@code BMC}, {@code BDC}, {@code EMC}, {@code MP},
 * {@code DP}), salvo el del contenido opcional ({@code /OC}), que decide lo que se ve, y la entrada de los formularios
 * y las imágenes en el árbol de estructura ({@code StructParent}, {@code StructParents}). El formulario lo etiqueta
 * después la {@code Image} que lo dibuja, como figura o como decoración.</p>
 *
 * <p>El formulario tiene el recorte de la página ({@code BBox}) y una matriz que lo lleva, ya girado
 * ({@code Rotate}), a un rectángulo con la esquina de abajo a la izquierda en el origen, del tamaño que da
 * {@link dev.stxt.pdf.resources.ImageFile#width(int)}.</p>
 */
final class PdfPages {

    private static final COSName STRUCT_PARENT = COSName.getPDFName("StructParent");
    private static final COSName STRUCT_PARENTS = COSName.getPDFName("StructParents");
    private static final COSName OC = COSName.getPDFName("OC");
    private static final Set<String> MARKS = Set.of(OperatorName.BEGIN_MARKED_CONTENT,
            OperatorName.BEGIN_MARKED_CONTENT_SEQ, OperatorName.END_MARKED_CONTENT, OperatorName.MARKED_CONTENT_POINT,
            OperatorName.MARKED_CONTENT_POINT_WITH_PROPS);

    private final LayerUtility layers;
    /** Los formularios ya limpios: los recursos copiados se comparten entre las páginas de un mismo PDF. */
    private final Set<COSBase> cleaned = new HashSet<>();

    PdfPages(PDDocument doc) {
        this.layers = new LayerUtility(doc);
    }

    /** La página {@code number} (desde 1) de {@code source}, copiada en un formulario. */
    PDFormXObject page(PDDocument source, int number) throws IOException {
        PDPage page = source.getPage(number - 1);
        PDFormXObject form = layers.importPageAsForm(source, page);
        PDRectangle crop = page.getCropBox();
        form.setBBox(crop);
        form.setMatrix(placement(crop, PdfInspector.rotation(page)));
        clean(form);
        return form;
    }

    /**
     * La matriz que lleva el recorte de la página, girado {@code degrees} en el sentido de las agujas del reloj como
     * lo muestra un visor, a un rectángulo con la esquina de abajo a la izquierda en el origen.
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

    /** Quita el etiquetado del formulario y, una vez cada uno, de los formularios y las imágenes que usa. */
    private void clean(PDFormXObject form) throws IOException {
        if (!cleaned.add(form.getCOSObject())) {
            return;
        }
        form.getCOSObject().removeItem(STRUCT_PARENT);
        form.getCOSObject().removeItem(STRUCT_PARENTS);
        unmark(form);
        PDResources resources = form.getResources();
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
    }

    /** Reescribe el contenido del formulario sin las marcas de contenido, salvo las de contenido opcional. */
    private static void unmark(PDFormXObject form) throws IOException {
        List<Object> tokens = new PDFStreamParser(form).parse();
        List<Object> kept = new ArrayList<>(tokens.size());
        List<Object> operands = new ArrayList<>();
        // Por cada marca abierta, si se ha conservado (para conservar también su EMC).
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
                // Un EMC sin su principio no se escribe: el contenido quedaría desequilibrado.
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
        try (OutputStream out = form.getContentStream().createOutputStream(COSName.FLATE_DECODE)) {
            new ContentStreamWriter(out).writeTokens(kept);
        }
    }
}
