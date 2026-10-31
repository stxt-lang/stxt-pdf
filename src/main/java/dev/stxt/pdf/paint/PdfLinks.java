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
 * The link annotations of the PDF (reference/implementation/pdf.md): one per clickable area, without a visible
 * border. A link to a URL carries its {@code URI} action; one to a box of the document ({@code
 * #id}) carries a destination with the page and the position of that box, which is not known until
 * all the pages have been painted (the link may point forward): it is recorded here and
 * {@link #resolve} completes it at the end.
 */
final class PdfLinks {

    private record Internal(PDAnnotationLink annotation, String id) {
    }

    private final List<Internal> internal = new ArrayList<>();

    /**
     * A link annotation in the rectangle {@code area} (PDF coordinates) towards
     * {@code destination}. {@code description} is what a screen reader reads; without it, the
     * destination itself.
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
     * Gives the internal links the destination of their box; with PDF/UA-2, as an action with the structure
     * destination ({@link PdfStructure#goTo}).
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
                // The builder already checked that it exists and is not in a Page overlay (LinkTargets).
                throw new InternalException("Link without a destination: #" + link.id());
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
