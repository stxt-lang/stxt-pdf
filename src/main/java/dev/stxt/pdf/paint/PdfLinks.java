package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.InternalException;
import dev.stxt.pdf.model.Links;
import dev.stxt.pdf.tree.Box;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Las anotaciones de enlace del PDF (referencia/implementacion/pdf.md): una por zona clicable, sin borde
 * visible. Un enlace a una URL lleva su acción {@code URI}; uno a una caja del documento ({@code
 * #id}) lleva un destino con la página y la posición de esa caja, que no se conoce hasta que se
 * han pintado todas las páginas (el enlace puede ir hacia delante): se apunta aquí y
 * {@link #resolve} lo completa al final.
 */
final class PdfLinks {

    private record Internal(PDAnnotationLink annotation, String id) {
    }

    private final List<Internal> internal = new ArrayList<>();

    /**
     * Una anotación de enlace en el rectángulo {@code area} (coordenadas del PDF) hacia
     * {@code destination}. {@code description} es lo que lee un lector de pantalla; sin él, el
     * propio destino.
     */
    PDAnnotationLink create(PDRectangle area, String destination, String description) {
        PDAnnotationLink annotation = new PDAnnotationLink();
        annotation.setRectangle(area);
        PDBorderStyleDictionary border = new PDBorderStyleDictionary();
        border.setWidth(0);
        annotation.setBorderStyle(border);
        annotation.setContents(description != null ? description : destination);
        annotation.setPrinted(true);
        if (Links.internal(destination)) {
            internal.add(new Internal(annotation, Links.target(destination)));
        } else {
            PDActionURI action = new PDActionURI();
            action.setURI(destination);
            annotation.setAction(action);
        }
        return annotation;
    }

    /**
     * Pone a los enlaces internos el destino de su caja; con PDF/UA-2, como una acción con el destino de estructura
     * ({@link PdfStructure#goTo}).
     */
    void resolve(Map<Box, PDPageXYZDestination> destinations, PdfStructure structure) {
        Map<String, Box> byKey = new java.util.HashMap<>();
        destinations.forEach((box, d) -> {
            if (box.key() != null) {
                byKey.put(box.key(), box);
            }
        });
        for (Internal link : internal) {
            Box box = byKey.get(link.id());
            if (box == null) {
                // El builder ya comprobó que existe y que no está en un Fixed (LinkTargets).
                throw new InternalException("Enlace sin destino: #" + link.id());
            }
            PDActionGoTo goTo = structure.goTo(box, destinations.get(box));
            if (goTo != null) {
                link.annotation().setAction(goTo);
            } else {
                link.annotation().setDestination(destinations.get(box));
            }
        }
    }
}
