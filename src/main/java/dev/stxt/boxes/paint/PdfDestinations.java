package dev.stxt.boxes.paint;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Box;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adónde lleva el PDF a quien busca una caja: la esquina superior izquierda de su primer
 * fragmento, en su página. Lo usan los marcadores de los encabezados ({@link PdfStructure}) y los
 * enlaces a una caja del documento ({@link PdfLinks}, ESPECIFICACION.md §5.20).
 *
 * <p>Tienen destino los encabezados y las cajas que se pueden señalar ({@link Box#key()}: con
 * {@code Id}, o los encabezados que lista un índice). Los {@code Fixed} no: se repiten en todas
 * las páginas y no tienen un único destino.</p>
 */
final class PdfDestinations {

    private PdfDestinations() {
    }

    /**
     * El destino de cada caja que lo tiene, en orden del documento. {@code area} es el área útil
     * de la página y {@code pdfPages}, sus páginas del PDF, en el mismo orden que {@code pages}.
     */
    static Map<Box, PDPageXYZDestination> find(List<Page> pages, List<PDPage> pdfPages, Rect area,
                                               double pageHeight) {
        Map<Box, PDPageXYZDestination> targets = new LinkedHashMap<>();
        for (int i = 0; i < pages.size(); i++) {
            find(pages.get(i).content(), area.x(), area.y(), pdfPages.get(i), pageHeight, targets);
        }
        return targets;
    }

    /** {@code x}, {@code y}: la esquina de la caja de borde del fragmento en la página. */
    private static void find(Fragment f, double x, double y, PDPage page, double pageHeight,
                             Map<Box, PDPageXYZDestination> targets) {
        Box box = f.box();
        if (f.first() && (box.headingLevel() > 0 || box.key() != null) && !targets.containsKey(box)) {
            PDPageXYZDestination d = new PDPageXYZDestination();
            d.setPage(page);
            d.setLeft((int) Math.floor(x));
            d.setTop((int) Math.ceil(pageHeight - y));
            targets.put(box, d);
        }
        Rect content = new Rect(x, y, f.width(), f.height()).shrink(f.border()).shrink(f.padding());
        for (Fragment c : f.children()) {
            find(c, content.x() + c.x(), content.y() + c.y(), page, pageHeight, targets);
        }
    }
}
