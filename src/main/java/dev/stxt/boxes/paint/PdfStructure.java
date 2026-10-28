package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.Cell;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Element;
import dev.stxt.boxes.tree.Metadata;
import dev.stxt.boxes.tree.Style;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDObjectReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDParentTreeValue;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDLayoutAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDTableAttributeObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
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
 * {@code TD} para las tablas (§5.15), {@code Figure} para las imágenes con {@code Alt} (con su
 * texto alternativo y su {@code BBox}, §5.18) y {@code Div} para el resto, también un {@code Multicol}
 * (§5.16), cuyas columnas no tienen elemento propio. El texto de
 * cada fragmento es contenido marcado ({@code BDC}/{@code EMC} con su {@code MCID}) que cuelga del
 * elemento de su caja; una caja partida entre páginas tiene contenido en varias. Los fondos, los
 * bordes, las imágenes sin {@code Alt} (decorativas) y los {@code Fixed} (repetidos en cada
 * página) son <em>artifacts</em>: no forman parte del contenido.</p>
 *
 * <p>El título y el idioma del documento ({@code Title} y {@code Language}) van a la
 * información del documento, a los metadatos XMP y al catálogo.</p>
 *
 * <p>Un enlace (§5.20) es un {@code Link} hijo del elemento de su caja, con la anotación del PDF
 * como referencia a objeto ({@code OBJR}).</p>
 *
 * <p>Marcadores: uno por encabezado, anidados por nivel (la jerarquía es correcta, §6), que
 * llevan a la esquina superior izquierda de su primer fragmento ({@link PdfDestinations}).</p>
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

    /**
     * Elementos de la caja y de sus descendientes, salvo los {@code Fixed}. El flujo de columnas
     * de un {@code Multicol} no tiene elemento: su contenido cuelga del {@code Multicol}.
     */
    private void build(Box box, PDStructureNode parent) {
        if (box.element == Element.COLUMNS) {
            box.children.forEach(child -> build(child, parent));
            return;
        }
        if (box.isImage() && box.style.alt == null || box.element == Element.CONTENTS_LEADER) {
            // Decorativa (o la guía de un índice): no tiene elemento y se pinta como artifact.
            return;
        }
        PDStructureElement e = new PDStructureElement(type(box), parent);
        parent.appendKid(e);
        elements.put(box, e);
        if (box.headingLevel() > 0) {
            headings.add(box);
        }
        if (box.isImage()) {
            e.setAlternateDescription(box.style.alt);
        }
        if (box.element == Element.LIST) {
            PDListAttributeObject numbering = new PDListAttributeObject();
            numbering.setListNumbering(listNumbering(box.style.listMarker));
            e.addAttribute(numbering);
        }
        if (box.element == Element.TABLE) {
            table(box, e);
            return;
        }
        PDStructureNode content = e;
        if (box.element == Element.LIST_ITEM) {
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
            Cell p = cell.style.itemArea;
            int group = group(cell);
            if (groups[group] == null) {
                groups[group] = new PDStructureElement(TABLE_GROUPS[group], e);
                e.appendKid(groups[group]);
            }
            if (p.row() != rowIndex) {
                row = new PDStructureElement("TR", groups[group]);
                groups[group].appendKid(row);
                rowIndex = p.row();
            }
            PDStructureElement c = new PDStructureElement(type(cell), row);
            row.appendKid(c);
            elements.put(cell, c);
            if (group == 0 || p.rows() > 1 || p.columns() > 1) {
                PDTableAttributeObject attributes = new PDTableAttributeObject();
                if (group == 0) {
                    attributes.setScope(PDTableAttributeObject.SCOPE_COLUMN);
                }
                if (p.rows() > 1) {
                    attributes.setRowSpan(p.rows());
                }
                if (p.columns() > 1) {
                    attributes.setColSpan(p.columns());
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

    /** Grupo de una celda de tabla: 0 en la cabecera, 1 en el cuerpo, 2 en el pie. */
    private static int group(Box cell) {
        Box table = cell.parent;
        // Fila desde 0 de la celda en el grid de la tabla; la colocación la da TableBuilder.
        int row = cell.style.itemArea.row() - 1;
        if (row < table.headRows) {
            return 0;
        }
        int rows = table.children.stream().mapToInt(c -> c.style.itemArea.row() - 1 + c.style.itemArea.rows())
                .max().orElse(0);
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
            case PARAGRAPH -> "P";
            case TEXT -> "Span";
            case LIST -> "L";
            case LIST_ITEM -> "LI";
            case TABLE -> "Table";
            case CELL -> group(box) == 0 ? "TH" : "TD";
            case IMAGE -> "Figure";
            case CONTENTS -> "TOC";
            case CONTENTS_ENTRY_1, CONTENTS_ENTRY_2, CONTENTS_ENTRY_3, CONTENTS_ENTRY_4, CONTENTS_ENTRY_5,
                 CONTENTS_ENTRY_6 -> "TOCI";
            case CONTENTS_TITLE, CONTENTS_PAGE -> "Span";
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
        if (e == null || !isContent(box)) {
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

    /**
     * Si la caja es contenido en la página actual: la cabecera y el pie de una tabla lo son en la
     * primera página en que salen, y en las demás, donde se repiten, no.
     */
    private boolean isContent(Box box) {
        if (repeated.contains(box)) {
            PDPage first = firstPage.putIfAbsent(box, page);
            return first == null || first == page;
        }
        return true;
    }

    /**
     * Etiqueta un enlace (ESPECIFICACION.md §5.20, decisión L5): un {@code Link} hijo del elemento
     * de la caja, con la anotación como referencia a objeto ({@code OBJR}) y su texto como
     * {@code Alt}. En un {@code Fixed}, en una imagen decorativa o en las filas repetidas de una
     * tabla, donde la caja no es contenido, el enlace tampoco: la anotación sigue siendo clicable.
     */
    void link(Box box, PDAnnotationLink annotation, String description) {
        PDStructureElement parent = elements.get(box);
        if (parent == null || !isContent(box)) {
            return;
        }
        PDStructureElement link = new PDStructureElement("Link", parent);
        link.setPage(page);
        link.setAlternateDescription(description != null ? description : annotation.getContents());
        parent.appendKid(link);
        PDObjectReference reference = new PDObjectReference();
        reference.setReferencedObject(annotation);
        reference.setPage(page);
        link.appendKid(reference);
        // La anotación se ve desde el árbol de padres con su propia clave, junto a las de las páginas.
        int key = parents.size();
        annotation.setStructParent(key);
        parents.put(key, new PDParentTreeValue(link.getCOSObject()));
    }

    /**
     * El {@code BBox} del {@code Figure} de una imagen: su caja de contenido en la página, en
     * coordenadas del PDF. Una imagen no se parte, así que solo está en una página.
     */
    void figureBox(Box box, PDRectangle r) {
        PDStructureElement e = elements.get(box);
        if (e == null) {
            return;
        }
        PDLayoutAttributeObject layout = new PDLayoutAttributeObject();
        layout.setBBox(r);
        e.addAttribute(layout);
    }

    /**
     * Termina: el árbol de padres y los marcadores de los encabezados, que llevan a su destino de
     * {@code destinations}.
     */
    void finish(PDDocument doc, Map<Box, PDPageXYZDestination> destinations) {
        PDNumberTreeNode parentTree = new PDNumberTreeNode(PDParentTreeValue.class);
        parentTree.setNumbers(parents);
        treeRoot.setParentTree(parentTree);
        treeRoot.setParentTreeNextKey(parents.size());
        if (headings.isEmpty()) {
            return;
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
            item.setDestination(destinations.get(h));
            parent.addLast(item);
            open.push(item);
            levels.push(h.headingLevel());
        }
        outline.openNode();
        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        catalog.setDocumentOutline(outline);
        catalog.setPageMode(PageMode.USE_OUTLINES);
    }
}
