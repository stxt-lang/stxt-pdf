package dev.stxt.boxes.paint;

import dev.stxt.boxes.layout.inline.PageNumbers;
import dev.stxt.boxes.layout.inline.TextTransform;
import dev.stxt.boxes.model.Cell;
import dev.stxt.boxes.model.Reference;
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
 * para {@code Paragraph} y para {@code Text} ({@code Span} si es parte del texto de un encabezado),
 * {@code L} y {@code LI} para las listas y sus ítems (con el marcador en {@code Lbl} y el contenido en {@code LBody}, §5.14),
 * {@code Table} con {@code THead}, {@code TBody}, {@code TFoot}, {@code TR}, {@code TH} y
 * {@code TD} para las tablas (§5.15), {@code Figure} para las imágenes con {@code Alt} (con su
 * texto alternativo y su {@code BBox}, §5.18) y {@code Div} para el resto, también un {@code Multicol}
 * (§5.16), cuyas columnas no tienen elemento propio. El texto de
 * cada fragmento es contenido marcado ({@code BDC}/{@code EMC} con su {@code MCID}) que cuelga del
 * elemento de su caja; una caja partida entre páginas tiene contenido en varias. Los fondos, los
 * bordes, las imágenes sin {@code Alt} (decorativas) y los {@code Fixed} (repetidos en cada
 * página) son <em>artifacts</em>: no forman parte del contenido.</p>
 *
 * <p>El idioma de una caja que declara otro ({@code Language} en {@code Typography}, §4.4) es el
 * {@code Lang} de su elemento, que heredan los de dentro.</p>
 *
 * <p>El título y el idioma del documento ({@code Title} y {@code Language}) van a la
 * información del documento, a los metadatos XMP y al catálogo.</p>
 *
 * <p>Un enlace (§5.20) es un {@code Link} que envuelve lo que se pulsa y lleva la anotación del PDF
 * como referencia a objeto ({@code OBJR}): el de una caja entera ({@code Link} del elemento), hijo del
 * elemento de la caja y padre de su contenido (una imagen cuelga de él, no al revés); el de un tramo de
 * texto ({@code [texto](destino)}), hijo del elemento de la caja con el contenido marcado de su texto,
 * uno por enlace y fragmento aunque ocupe varias líneas.</p>
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

    /** El {@code Link} de cada caja con {@code Link}: contiene su contenido y las anotaciones de su zona. */
    private final Map<Box, PDStructureElement> boxLinks = new HashMap<>();
    /** Donde cuelga el contenido propio (el texto) de las cajas con {@code Link}: su {@code Link}. */
    private final Map<Box, PDStructureElement> holders = new HashMap<>();
    /** Los {@code Link} de cajas sin texto ni figura, que no se leen por su contenido sino por su {@code Alt}. */
    private final Set<PDStructureElement> emptyLinks = new HashSet<>();
    /** El {@code Link} del enlace de texto que se está pintando, para colgarle la anotación de su zona. */
    private PDStructureElement inline;

    private static final String[] TABLE_GROUPS = {"THead", "TBody", "TFoot"};

    /** El elemento {@code Document}, raíz del árbol de estructura. */
    private final PDStructureElement document;

    /** La raíz del documento: es el «padre» de sus secciones, que no lo tienen (§5.21). */
    private final Box root;

    private PDPage page;
    private COSArray pageParents;

    PdfStructure(PDDocument doc, Box root) {
        this.root = root;
        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        catalog.setStructureTreeRoot(treeRoot);
        PDMarkInfo markInfo = new PDMarkInfo();
        markInfo.setMarked(true);
        catalog.setMarkInfo(markInfo);
        build(root, treeRoot);
        document = elements.get(root);
    }

    /**
     * Elementos de la caja y de sus descendientes, salvo los {@code Fixed}. El flujo de columnas
     * de un {@code Multicol} no tiene elemento: su contenido cuelga del {@code Multicol}.
     */
    private void build(Box box, PDStructureNode parent) {
        if (box.element() == Element.COLUMNS) {
            box.children().forEach(child -> build(child, parent));
            return;
        }
        if (box.isImage() && box.style().alt() == null || box.element() == Element.CONTENTS_LEADER) {
            // Decorativa (o la guía de un índice): no tiene elemento y se pinta como artifact.
            return;
        }
        // Una imagen con enlace es el contenido de su Link: Link → Figure.
        PDStructureNode owner = box.isImage() && box.style().link() != null ? newLink(box, parent) : parent;
        PDStructureElement e = new PDStructureElement(type(box), owner);
        owner.appendKid(e);
        elements.put(box, e);
        Box outer = box.element() == Element.SECTION ? root : box.parent();
        if (outer != null && !box.style().locale().equals(outer.style().locale())) {
            // Lang se hereda por el árbol de estructura: solo donde el idioma cambia respecto al de fuera.
            e.setLanguage(box.style().locale().toLanguageTag());
        }
        if (box.headingLevel() > 0) {
            headings.add(box);
        }
        if (box.isImage()) {
            e.setAlternateDescription(box.style().alt());
        }
        if (box.hasText() && TextTransform.applies(box.style()) && !Reference.contains(box.text())
                && !PageNumbers.contains(box.text())) {
            // Las mayúsculas son solo de pintado (§4.4): el texto de la estructura es el escrito. Si
            // lleva números de página o referencias, el texto cambia con la página y no se puede dar.
            e.setActualText(box.text());
        }
        if (box.element() == Element.LIST) {
            PDListAttributeObject numbering = new PDListAttributeObject();
            numbering.setListNumbering(listNumbering(box.style().listMarker()));
            e.addAttribute(numbering);
        }
        if (box.element() == Element.TABLE) {
            table(box, e);
            return;
        }
        PDStructureNode content = e;
        if (box.element() == Element.LIST_ITEM) {
            // LI: el marcador (Lbl) y el contenido (LBody).
            if (box.marker() != null) {
                PDStructureElement label = new PDStructureElement("Lbl", e);
                e.appendKid(label);
                elements.put(box.marker(), label);
            }
            PDStructureElement body = new PDStructureElement("LBody", e);
            e.appendKid(body);
            content = body;
        }
        content = linked(box, content);
        for (Box child : box.children()) {
            if (child.style().position() != Style.Position.FIXED) {
                build(child, content);
            }
        }
    }

    /**
     * Si la caja tiene {@code Link} (§5.20), el {@code Link} que envuelve su contenido (su texto y sus
     * hijos) y que recibe las anotaciones de su zona; si no, {@code content}. Una imagen es la excepción:
     * su {@code Link} envuelve a su {@code Figure} ({@link #build}).
     */
    private PDStructureNode linked(Box box, PDStructureNode content) {
        if (box.style().link() == null || box.isImage()) {
            return content;
        }
        PDStructureElement link = newLink(box, content);
        holders.put(box, link);
        return link;
    }

    /** Un {@code Link} hijo de {@code parent} para la caja. */
    private PDStructureElement newLink(Box box, PDStructureNode parent) {
        PDStructureElement link = new PDStructureElement("Link", parent);
        parent.appendKid(link);
        boxLinks.put(box, link);
        if (!hasContent(box)) {
            emptyLinks.add(link);
        }
        return link;
    }

    /** Si la caja o lo que contiene tiene algo que leer: texto, un marcador o una imagen con {@code Alt}. */
    private static boolean hasContent(Box box) {
        if (box.hasText() || box.marker() != null || box.isImage() && box.style().alt() != null) {
            return true;
        }
        return box.children().stream().anyMatch(c -> c.style().position() != Style.Position.FIXED && hasContent(c));
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
        for (Box cell : table.children()) {
            Cell p = cell.style().itemArea();
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
            if (group == 0 || cell.style().header() || p.rows() > 1 || p.columns() > 1) {
                PDTableAttributeObject attributes = new PDTableAttributeObject();
                if (group == 0) {
                    attributes.setScope(PDTableAttributeObject.SCOPE_COLUMN);
                } else if (cell.style().header()) {
                    attributes.setScope(PDTableAttributeObject.SCOPE_ROW);
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
            PDStructureNode content = linked(cell, c);
            for (Box child : cell.children()) {
                if (child.style().position() != Style.Position.FIXED) {
                    build(child, content);
                }
            }
        }
    }

    /** Apunta la caja y su descendencia (y el marcador de los ítems) como contenido que se repite. */
    private void repeats(Box box) {
        repeated.add(box);
        if (box.marker() != null) {
            repeated.add(box.marker());
        }
        box.children().forEach(this::repeats);
    }

    /** Grupo de una celda de tabla: 0 en la cabecera, 1 en el cuerpo, 2 en el pie. */
    private static int group(Box cell) {
        Box table = cell.parent();
        // Fila desde 0 de la celda en el grid de la tabla; la colocación la da TableBuilder.
        int row = cell.style().itemArea().row() - 1;
        if (row < table.headRows()) {
            return 0;
        }
        int rows = table.children().stream().mapToInt(c -> c.style().itemArea().row() - 1 + c.style().itemArea().rows())
                .max().orElse(0);
        return row >= rows - table.footRows() ? 2 : 1;
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

    /**
     * Paquete XMP mínimo con el título y la declaración de PDF/UA-1 ({@code pdfuaid:part}). Un
     * documento siempre tiene {@code Title} (obligatorio): sin título el PDF no cumpliría PDF/UA (7.1) y
     * no podría afirmarlo.
     */
    static String xmp(String title) {
        String escaped = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<?xpacket begin=\"﻿\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n"
                + "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">\n"
                + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "<rdf:Description rdf:about=\"\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"
                + "<dc:title><rdf:Alt><rdf:li xml:lang=\"x-default\">" + escaped + "</rdf:li></rdf:Alt></dc:title>\n"
                + "</rdf:Description>\n"
                + "<rdf:Description rdf:about=\"\" xmlns:pdfuaid=\"http://www.aiim.org/pdfua/ns/id/\">\n"
                + "<pdfuaid:part>1</pdfuaid:part>\n"
                + "</rdf:Description>\n"
                + "</rdf:RDF>\n"
                + "</x:xmpmeta>\n"
                + "<?xpacket end=\"w\"?>";
    }

    /** Tipo estándar de estructura de la caja (decisión G1 de la hoja de ruta). */
    static String type(Box box) {
        if (box.element() == null) {
            return "Document";
        }
        if (box.element() == Element.SECTION) {
            return "Sect";
        }
        if (box.headingLevel() > 0) {
            return "H" + box.headingLevel();
        }
        return switch (box.element()) {
            case PARAGRAPH, CODE_BLOCK -> "P";
            // Un Text es un bloque de texto, un párrafo; dentro de un encabezado es parte de su texto.
            case TEXT -> box.parent() != null && box.parent().headingLevel() > 0 ? "Span" : "P";
            case LIST -> "L";
            case LIST_ITEM -> "LI";
            case TABLE -> "Table";
            case CELL -> group(box) == 0 || box.style().header() ? "TH" : "TD";
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
     * es un <em>artifact</em> (dentro de un {@code Fixed}). Cuelga del elemento de la caja o, si la caja
     * tiene {@code Link}, de este.
     */
    int mark(Box box) {
        PDStructureElement e = holders.containsKey(box) ? holders.get(box) : elements.get(box);
        if (e == null || !isContent(box)) {
            return -1;
        }
        return reference(e);
    }

    /**
     * Apunta el texto de un enlace de la caja ({@code [texto](destino)}) como contenido de un
     * {@code Link} nuevo, hijo del elemento de la caja, en la página actual y da su {@code MCID}; -1 si es
     * un <em>artifact</em>. Las anotaciones de sus zonas cuelgan de él ({@link #inlineLink}).
     */
    int markLink(Box box) {
        inline = null;
        PDStructureElement holder = holders.containsKey(box) ? holders.get(box) : elements.get(box);
        if (holder == null || !isContent(box)) {
            return -1;
        }
        PDStructureElement link = new PDStructureElement("Link", holder);
        link.setPage(page);
        holder.appendKid(link);
        inline = link;
        return reference(link);
    }

    /** Un contenido marcado de la página actual que cuelga de {@code e}; su {@code MCID}. */
    private int reference(PDStructureElement e) {
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
     * Etiqueta la zona de una caja con {@code Link} (ESPECIFICACION.md §5.20, decisión 171): la
     * anotación es una referencia a objeto ({@code OBJR}) del {@code Link} que envuelve el contenido
     * de la caja. Un {@code Link} sin nada que leer dentro lleva su texto como {@code Alt}. En un
     * {@code Fixed}, en una imagen decorativa o en las filas repetidas de una tabla, donde la caja no
     * es contenido, el enlace va a un {@code Link} aparte ({@link #outside}).
     */
    void link(Box box, PDAnnotationLink annotation, String description) {
        PDStructureElement parent = elements.get(box);
        if (parent == null || !isContent(box)) {
            outside(annotation, description);
            return;
        }
        PDStructureElement link = boxLinks.get(box);
        if (link == null) {
            // Una caja que no puede envolver su contenido en un Link (una tabla): un Link aparte, hijo suyo.
            link = new PDStructureElement("Link", parent);
            link.setPage(page);
            parent.appendKid(link);
            emptyLinks.add(link);
        }
        if (emptyLinks.contains(link) && link.getAlternateDescription() == null) {
            link.setAlternateDescription(description != null ? description : annotation.getContents());
        }
        attach(link, annotation);
    }

    /** Etiqueta la zona de una línea del enlace de texto que empezó {@link #markLink}. */
    void inlineLink(PDAnnotationLink annotation, String description) {
        if (inline != null) {
            attach(inline, annotation);
        } else {
            outside(annotation, description);
        }
    }

    /**
     * Etiqueta el enlace de algo que no es contenido (un {@code Fixed}, una imagen decorativa, las filas
     * repetidas de una tabla): PDF/UA exige que toda anotación de enlace esté en un {@code Link}. Es un
     * {@code Link} hijo del {@code Document}, sin contenido marcado y con el texto del enlace como
     * {@code Alt}: un lector de pantalla lo anuncia, y lo que se repite en cada página (un pie con una
     * web) se anuncia en cada una.
     */
    private void outside(PDAnnotationLink annotation, String description) {
        PDStructureElement link = new PDStructureElement("Link", document);
        link.setPage(page);
        link.setAlternateDescription(description != null ? description : annotation.getContents());
        document.appendKid(link);
        attach(link, annotation);
    }

    private void attach(PDStructureElement link, PDAnnotationLink annotation) {
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
