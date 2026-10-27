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
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDTableAttributeObject;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Significado del documento en el PDF (ESPECIFICACION.md §5.13): el árbol de estructura del PDF
 * etiquetado y los marcadores (<em>outline</em>) de los encabezados.
 *
 * <p>Árbol de estructura: un elemento por caja, en orden del documento, que es el orden de
 * lectura. {@code Document} para la raíz, {@code H1}…{@code H6} para los encabezados, {@code P}
 * para {@code Paragraph}, {@code Span} para {@code Text}, {@code L} y {@code LI} para las listas
 * y sus ítems (con el marcador en {@code Lbl} y el contenido en {@code LBody}, §5.14),
 * {@code Table} con {@code THead}, {@code TBody}, {@code TFoot}, {@code TR}, {@code TH} y
 * {@code TD} para las tablas (§5.15) y {@code Div} para el resto. El texto de
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
    /** Cajas de la cabecera y el pie de las tablas, que se repiten en cada página de su tabla. */
    private final Set<Box> repeated = new HashSet<>();
    /** Primera página con contenido de cada caja de {@link #repeated}: en las demás es un <em>artifact</em>. */
    private final Map<Box, PDPage> firstPage = new HashMap<>();

    private static final String[] TABLE_GROUPS = {"THead", "TBody", "TFoot"};

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
        if ("list".equals(box.element)) {
            PDListAttributeObject numbering = new PDListAttributeObject();
            numbering.setListNumbering(listNumbering(box.style.listMarker));
            e.addAttribute(numbering);
        }
        if ("table".equals(box.element)) {
            table(box, e);
            return;
        }
        PDStructureNode content = e;
        if ("list-item".equals(box.element)) {
            // LI: el marcador (Lbl) y el contenido (LBody).
            if (box.marker != null) {
                PDStructureElement label = new PDStructureElement("Lbl", e);
                e.appendKid(label);
                elements.put(box.marker, label);
            }
            PDStructureElement body = new PDStructureElement("LBody", e);
            e.appendKid(body);
            content = body;
        }
        for (Box child : box.children) {
            if (child.style.position != Style.Position.FIXED) {
                build(child, content);
            }
        }
    }

    /**
     * Una tabla (§5.15): sus celdas, en orden de filas, en {@code THead}, {@code TBody} y
     * {@code TFoot}, una {@code TR} por fila. Cada celda es {@code TH} (con {@code Scope:
     * Column}) en la cabecera y {@code TD} en el resto, con {@code RowSpan} y {@code ColSpan} si
     * ocupa más de una fila o columna. Las celdas de la cabecera y el pie, y lo que contienen, se
     * repiten en cada página: solo la primera vez son contenido.
     */
    private void table(Box table, PDStructureElement e) {
        PDStructureElement[] groups = new PDStructureElement[TABLE_GROUPS.length];
        PDStructureElement row = null;
        int rowIndex = -1;
        for (Box cell : table.children) {
            int[] p = placement(cell);
            int group = group(cell);
            if (groups[group] == null) {
                groups[group] = new PDStructureElement(TABLE_GROUPS[group], e);
                e.appendKid(groups[group]);
            }
            if (p[0] != rowIndex) {
                row = new PDStructureElement("TR", groups[group]);
                groups[group].appendKid(row);
                rowIndex = p[0];
            }
            PDStructureElement c = new PDStructureElement(type(cell), row);
            row.appendKid(c);
            elements.put(cell, c);
            if (group == 0 || p[1] > 1 || p[2] > 1) {
                PDTableAttributeObject attributes = new PDTableAttributeObject();
                if (group == 0) {
                    attributes.setScope(PDTableAttributeObject.SCOPE_COLUMN);
                }
                if (p[1] > 1) {
                    attributes.setRowSpan(p[1]);
                }
                if (p[2] > 1) {
                    attributes.setColSpan(p[2]);
                }
                c.addAttribute(attributes);
            }
            if (group != 1) {
                repeats(cell);
            }
            for (Box child : cell.children) {
                if (child.style.position != Style.Position.FIXED) {
                    build(child, c);
                }
            }
        }
    }

    /** Apunta la caja y su descendencia (y el marcador de los ítems) como contenido que se repite. */
    private void repeats(Box box) {
        repeated.add(box);
        if (box.marker != null) {
            repeated.add(box.marker);
        }
        box.children.forEach(this::repeats);
    }

    /** Fila (desde 0), filas y columnas de una celda: su colocación en el grid de la tabla. */
    private static int[] placement(Box cell) {
        String[] t = cell.style.itemArea.split(" ");
        return new int[]{Integer.parseInt(t[0]) - 1, Integer.parseInt(t[2]), Integer.parseInt(t[3])};
    }

    /** Grupo de una celda de tabla: 0 en la cabecera, 1 en el cuerpo, 2 en el pie. */
    private static int group(Box cell) {
        Box table = cell.parent;
        int row = placement(cell)[0];
        if (row < table.headRows) {
            return 0;
        }
        int rows = table.children.stream().mapToInt(c -> placement(c)[0] + placement(c)[1]).max().orElse(0);
        return row >= rows - table.footRows ? 2 : 1;
    }

    /** {@code ListNumbering} de una lista; {@code dash} no tiene equivalente y es {@code None}. */
    private static String listNumbering(Style.Marker marker) {
        return switch (marker) {
            case DISC -> PDListAttributeObject.LIST_NUMBERING_DISC;
            case DASH, NONE -> PDListAttributeObject.LIST_NUMBERING_NONE;
            case DECIMAL -> PDListAttributeObject.LIST_NUMBERING_DECIMAL;
            case LOWER_ALPHA -> PDListAttributeObject.LIST_NUMBERING_LOWER_ALPHA;
            case UPPER_ALPHA -> PDListAttributeObject.LIST_NUMBERING_UPPER_ALPHA;
            case LOWER_ROMAN -> PDListAttributeObject.LIST_NUMBERING_LOWER_ROMAN;
            case UPPER_ROMAN -> PDListAttributeObject.LIST_NUMBERING_UPPER_ROMAN;
        };
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
            case "list" -> "L";
            case "list-item" -> "LI";
            case "table" -> "Table";
            case "cell" -> group(box) == 0 ? "TH" : "TD";
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
        if (repeated.contains(box)) {
            // La cabecera y el pie de una tabla son contenido en la primera página en que salen.
            PDPage first = firstPage.putIfAbsent(box, page);
            if (first != null && first != page) {
                return -1;
            }
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
