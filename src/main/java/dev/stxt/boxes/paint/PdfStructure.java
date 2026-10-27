package dev.stxt.boxes.paint;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Metadata;
import dev.stxt.boxes.tree.Style;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDParentTreeValue;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Significado del documento en el PDF (ESPECIFICACION.md §5.13): el árbol de estructura del PDF
 * etiquetado y los marcadores (<em>outline</em>) de los encabezados.
 *
 * <p>Árbol de estructura: un elemento por caja, en orden del documento, que es el orden de
 * lectura. {@code Document} para la raíz, {@code H1}…{@code H6} para los encabezados, {@code P}
 * para {@code Paragraph}, {@code Span} para {@code Text} y {@code Div} para el resto. El texto de
 * cada fragmento es contenido marcado ({@code BDC}/{@code EMC} con su {@code MCID}) que cuelga del
 * elemento de su caja; una caja partida entre páginas tiene contenido en varias. Los fondos, los
 * bordes y los {@code Fixed} (repetidos en cada página) son <em>artifacts</em>: no forman parte
 * del contenido.</p>
 *
 * <p>El título y el idioma del documento ({@code Title} y {@code Language}) van a la
 * información del documento, a los metadatos XMP y al catálogo.</p>
 *
 * <p>Marcadores: uno por encabezado, anidados por nivel (la jerarquía es correcta, §6), que
 * llevan a la esquina superior izquierda de su primer fragmento.</p>
 */
final class PdfStructure {

    private final PDStructureTreeRoot treeRoot = new PDStructureTreeRoot();
    /** Elemento de estructura de cada caja; las de un {@code Fixed} no tienen. */
    private final Map<Box, PDStructureElement> elements = new HashMap<>();
    /** Por página, los elementos a los que pertenece cada {@code MCID}: el árbol de padres. */
    private final Map<Integer, PDParentTreeValue> parents = new HashMap<>();
    /** Encabezados en orden del documento. */
    private final List<Box> headings = new ArrayList<>();

    private PDPage page;
    private COSArray pageParents;

    PdfStructure(PDDocument doc, Box root) {
        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        catalog.setStructureTreeRoot(treeRoot);
        PDMarkInfo markInfo = new PDMarkInfo();
        markInfo.setMarked(true);
        catalog.setMarkInfo(markInfo);
        build(root, treeRoot);
    }

    /** Elementos de la caja y de sus descendientes, salvo los {@code Fixed}. */
    private void build(Box box, PDStructureNode parent) {
        PDStructureElement e = new PDStructureElement(type(box), parent);
        parent.appendKid(e);
        elements.put(box, e);
        if (box.headingLevel() > 0) {
            headings.add(box);
        }
        for (Box child : box.children) {
            if (child.style.position != Style.Position.FIXED) {
                build(child, e);
            }
        }
    }

    /**
     * Título e idioma del documento. El título va en la información del documento y en los
     * metadatos XMP ({@code dc:title}), y el visor lo muestra en lugar del nombre del fichero; el
     * idioma ({@code Lang} del catálogo) es el de todo el contenido.
     */
    static void metadata(PDDocument doc, Metadata metadata) throws IOException {
        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        if (metadata.language() != null) {
            catalog.setLanguage(metadata.language());
        }
        if (metadata.title() == null) {
            return;
        }
        doc.getDocumentInformation().setTitle(metadata.title());
        PDViewerPreferences preferences = new PDViewerPreferences(new COSDictionary());
        preferences.setDisplayDocTitle(true);
        catalog.setViewerPreferences(preferences);
        PDMetadata xmp = new PDMetadata(doc);
        xmp.importXMPMetadata(xmp(metadata.title()).getBytes(StandardCharsets.UTF_8));
        catalog.setMetadata(xmp);
    }

    /** Paquete XMP mínimo con el título. */
    static String xmp(String title) {
        String escaped = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<?xpacket begin=\"﻿\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n"
                + "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">\n"
                + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "<rdf:Description rdf:about=\"\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"
                + "<dc:title><rdf:Alt><rdf:li xml:lang=\"x-default\">" + escaped + "</rdf:li></rdf:Alt></dc:title>\n"
                + "</rdf:Description>\n"
                + "</rdf:RDF>\n"
                + "</x:xmpmeta>\n"
                + "<?xpacket end=\"w\"?>";
    }

    /** Tipo estándar de estructura de la caja (decisión G1 de la hoja de ruta). */
    static String type(Box box) {
        if (box.element == null) {
            return "Document";
        }
        if (box.headingLevel() > 0) {
            return "H" + box.headingLevel();
        }
        return switch (box.element) {
            case "paragraph" -> "P";
            case "text" -> "Span";
            default -> "Div";
        };
    }

    /** Empieza el contenido de una página. */
    void startPage(PDPage page) {
        this.page = page;
        pageParents = new COSArray();
        int key = parents.size();
        page.setStructParents(key);
        parents.put(key, new PDParentTreeValue(pageParents));
    }

    /**
     * Apunta el texto de un fragmento de la caja en la página actual y da su {@code MCID}; -1 si
     * es un <em>artifact</em> (dentro de un {@code Fixed}).
     */
    int mark(Box box) {
        PDStructureElement e = elements.get(box);
        if (e == null) {
            return -1;
        }
        int mcid = pageParents.size();
        pageParents.add(e);
        PDMarkedContentReference ref = new PDMarkedContentReference();
        ref.setMCID(mcid);
        ref.setPage(page);
        e.appendKid(ref);
        return mcid;
    }

    /** Termina: el árbol de padres y los marcadores de los encabezados. */
    void finish(PDDocument doc, List<Page> pages, List<PDPage> pdfPages, Rect area, double pageHeight) {
        PDNumberTreeNode parentTree = new PDNumberTreeNode(PDParentTreeValue.class);
        parentTree.setNumbers(parents);
        treeRoot.setParentTree(parentTree);
        treeRoot.setParentTreeNextKey(parents.size());
        if (headings.isEmpty()) {
            return;
        }
        Map<Box, PDPageXYZDestination> targets = new LinkedHashMap<>();
        for (int i = 0; i < pages.size(); i++) {
            find(pages.get(i).content(), area.x(), area.y(), pdfPages.get(i), pageHeight, targets);
        }
        PDDocumentOutline outline = new PDDocumentOutline();
        // Pila de los encabezados abiertos: el padre de cada uno es el último de nivel menor.
        Deque<PDOutlineItem> open = new ArrayDeque<>();
        Deque<Integer> levels = new ArrayDeque<>();
        for (Box h : headings) {
            while (!levels.isEmpty() && levels.peek() >= h.headingLevel()) {
                open.pop();
                levels.pop();
            }
            PDOutlineNode parent = open.isEmpty() ? outline : open.peek();
            PDOutlineItem item = new PDOutlineItem();
            item.setTitle(h.headingText());
            item.setDestination(targets.get(h));
            parent.addLast(item);
            open.push(item);
            levels.push(h.headingLevel());
        }
        outline.openNode();
        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        catalog.setDocumentOutline(outline);
        catalog.setPageMode(PageMode.USE_OUTLINES);
    }

    /** El destino de cada encabezado: la esquina de su primer fragmento. {@code x}, {@code y}: su caja de borde. */
    private static void find(Fragment f, double x, double y, PDPage page, double pageHeight,
                             Map<Box, PDPageXYZDestination> targets) {
        if (f.first() && f.box().headingLevel() > 0 && !targets.containsKey(f.box())) {
            PDPageXYZDestination d = new PDPageXYZDestination();
            d.setPage(page);
            d.setLeft((int) Math.floor(x));
            d.setTop((int) Math.ceil(pageHeight - y));
            targets.put(f.box(), d);
        }
        Rect content = new Rect(x, y, f.width(), f.height()).shrink(f.border()).shrink(f.padding());
        for (Fragment c : f.children()) {
            find(c, content.x() + c.x(), content.y() + c.y(), page, pageHeight, targets);
        }
    }
}
