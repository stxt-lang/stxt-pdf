package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.tree.Box;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import java.awt.geom.AffineTransform;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adónde lleva el PDF a quien busca una caja: la esquina superior izquierda de su primer
 * fragmento, en su página. Lo usan los marcadores de los encabezados ({@link PdfStructure}) y los
 * enlaces a una caja del documento ({@link PdfLinks}, referencia/comun/enlaces.md).
 *
 * <p>Tienen destino los encabezados y las cajas que se pueden señalar ({@link Box#key()}: con
 * {@code Id}, o los encabezados que lista un índice). Los {@code Page overlay} no: se repiten en todas
 * las páginas y no tienen un único destino.</p>
 */
final class PdfDestinations {

    private PdfDestinations() {
    }

    /**
     * El destino de cada caja que lo tiene, en orden del documento. {@code pdfPages} son sus
     * páginas del PDF, en el mismo orden que {@code pages}; cada una tiene el medio de su sección.
     */
    static Map<Box, PDPageXYZDestination> find(List<Page> pages, List<PDPage> pdfPages) {
        Map<Box, PDPageXYZDestination> targets = new LinkedHashMap<>();
        for (int i = 0; i < pages.size(); i++) {
            Rect area = pages.get(i).pageSetup().pageArea();
            find(pages.get(i).content(), area.x(), area.y(), new AffineTransform(), pdfPages.get(i),
                    pages.get(i).pageSetup().height(), targets);
        }
        return targets;
    }

    /**
     * {@code x}, {@code y}: la esquina de la caja de borde del fragmento en la página, sin girar;
     * {@code turn}, los giros de sus ancestros (referencia/comun/caja.md). La esquina de una caja girada, o de dentro
     * de una girada, es la del rectángulo que envuelve lo que se pinta.
     */
    private static void find(Fragment f, double x, double y, AffineTransform turn, PDPage page, double pageHeight,
                             Map<Box, PDPageXYZDestination> targets) {
        Box box = f.box();
        Rect borderBox = f.borderBox(x, y);
        Rotation rotation = box.style().rotation();
        if (rotation != null) {
            turn = new AffineTransform(turn);
            turn.concatenate(rotation.transform(borderBox));
        }
        if (f.first() && (box.headingLevel() > 0 || box.key() != null) && !targets.containsKey(box)) {
            Rect painted = turn.isIdentity() ? borderBox : Rotation.envelope(turn, borderBox);
            PDPageXYZDestination d = new PDPageXYZDestination();
            d.setPage(page);
            d.setLeft((int) Math.floor(painted.x()));
            d.setTop((int) Math.ceil(pageHeight - painted.y()));
            targets.put(box, d);
        }
        Rect content = borderBox.shrink(f.border()).shrink(f.padding());
        for (Fragment c : f.children()) {
            find(c, content.x() + c.x(), content.y() + c.y(), turn, page, pageHeight, targets);
        }
    }
}
