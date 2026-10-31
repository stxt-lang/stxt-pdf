package dev.stxt.pdf.paint;

import dev.stxt.pdf.layout.inline.PageNumbers;
import dev.stxt.pdf.layout.inline.TextTransform;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.Links;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element.Role;
import dev.stxt.pdf.tree.Metadata;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
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
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
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
 * Significado del documento en el PDF (referencia/implementacion/pdf.md): el árbol de estructura del PDF
 * etiquetado y los marcadores (<em>outline</em>) de los encabezados.
 *
 * <p>Árbol de estructura: un elemento por caja, en orden del documento, que es el orden de
 * lectura. {@code Document} para la raíz, {@code H1}…{@code H6} para los encabezados, {@code P} para {@code Paragraph}
 * y para {@code Text} ({@code Span} si es parte del texto de un encabezado), {@code L} y {@code LI} para las listas y
 * sus ítems (con el marcador en {@code Lbl} y el contenido en {@code LBody}, referencia/componentes/list.md), {@code
 * Table} con {@code THead}, {@code TBody}, {@code TFoot}, {@code TR}, {@code TH} y {@code TD} para las tablas
 * (referencia/componentes/table.md), {@code Figure} para las imágenes con {@code Alt} (con su texto alternativo y su
 * {@code BBox}, referencia/componentes/image.md) y {@code Div} para el resto, también un {@code Columns}
 * (referencia/componentes/columns.md), cuyas columnas no tienen elemento propio. El texto de cada fragmento es
 * contenido marcado ({@code BDC}/{@code EMC} con su {@code MCID}) que cuelga del elemento de su caja; una caja partida
 * entre páginas tiene contenido en varias. Los fondos, los bordes, las imágenes sin {@code Alt} (decorativas) y los
 * {@code Page overlay} (repetidos en cada página) son <em>artifacts</em>: no forman parte del contenido.</p>
 *
 * <p>El idioma de una caja que declara otro ({@code Language} en {@code Typography}, referencia/comun/tipografia.md) es
 * el {@code Lang} de su elemento, que heredan los de dentro.</p>
 *
 * <p>El título y el idioma del documento ({@code Title} y {@code Language}) van a la
 * información del documento, a los metadatos XMP y al catálogo.</p>
 *
 * <p>Un enlace (referencia/implementacion/pdf.md) es un {@code Link} que envuelve lo que se pulsa y lleva la anotación
 * del PDF como referencia a objeto ({@code OBJR}): el de una caja entera ({@code Link} del elemento), hijo del elemento
 * de la caja y padre de su contenido (una imagen cuelga de él, no al revés); el de un tramo de texto ({@code
 * [texto](destino)}), hijo del elemento de la caja con el contenido marcado de su texto, uno por enlace y fragmento
 * aunque ocupe varias líneas.</p>
 *
 * <p>Marcadores: uno por encabezado, anidados por nivel (la jerarquía es correcta, referencia/comun/errores.md), que
 * llevan a la esquina superior izquierda de su primer fragmento ({@link PdfDestinations}).</p>
 */
final class PdfStructure {

    private final PDStructureTreeRoot treeRoot = new PDStructureTreeRoot();
    /** Elemento de estructura de cada caja; las de un {@code Page overlay} no tienen. */
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
    /**
     * Los {@code Link} de los enlaces de lo que no es contenido (un {@code Page overlay}, una imagen decorativa), por
     * caja y destino: están en el sitio de la caja en el árbol, sin contenido, y reciben las anotaciones de todas las
     * páginas.
     */
    private final Map<Box, Map<String, PDStructureElement>> standIns = new HashMap<>();
    /**
     * Los {@code Link} de los enlaces de texto de las cajas que se repiten (la cabecera y el pie de una tabla), por
     * caja y destino: los de la primera página, que reciben las anotaciones de las repeticiones.
     */
    private final Map<Box, Map<String, PDStructureElement>> firstLinks = new HashMap<>();

    /** El espacio de nombres estándar de PDF 2.0 (ISO 32000-2, 14.8.6). */
    static final String PDF_2_NAMESPACE = "http://iso.org/pdf2/ssn";

    private static final String[] TABLE_GROUPS = {"THead", "TBody", "TFoot"};
    /** Los tipos que no están en el espacio de nombres de PDF 2.0: se quedan en el de PDF 1.7, el de omisión. */
    private static final Set<String> PDF_1_7_ONLY = Set.of("TOC", "TOCI", "Reference");

    /** La parte de PDF/UA que cumple el PDF: 1 o 2 (referencia/implementacion/pdf.md). */
    private final int ua;
    /** Con PDF/UA-2, el espacio de nombres de PDF 2.0 de los elementos; con PDF/UA-1, {@code null}. */
    private final COSDictionary pdf2;
    /** Con PDF/UA-2, el {@code TOCI} de cada entrada de índice, que señala con {@code Ref} a su encabezado. */
    private final Map<Box, PDStructureElement> entries = new HashMap<>();

    /** La raíz del documento: es el «padre» de sus secciones, que no lo tienen (referencia/componentes/document.md). */
    private final Box root;

    private PDPage page;
    private COSArray pageParents;

    /** {@code ua}: la parte de PDF/UA, 1 o 2. */
    PdfStructure(PDDocument doc, Box root, int ua) {
        this.root = root;
        this.ua = ua;
        if (ua == 2) {
            pdf2 = new COSDictionary();
            pdf2.setItem(COSName.TYPE, COSName.getPDFName("Namespace"));
            pdf2.setString(COSName.getPDFName("NS"), PDF_2_NAMESPACE);
            COSArray namespaces = new COSArray();
            namespaces.add(pdf2);
            treeRoot.getCOSObject().setItem(COSName.getPDFName("Namespaces"), namespaces);
        } else {
            pdf2 = null;
        }
        PDDocumentCatalog catalog = doc.getDocumentCatalog();
        catalog.setStructureTreeRoot(treeRoot);
        PDMarkInfo markInfo = new PDMarkInfo();
        markInfo.setMarked(true);
        catalog.setMarkInfo(markInfo);
        build(root, treeRoot);
    }

    /**
     * Elementos de la caja y de sus descendientes, salvo los {@code Page overlay}. El flujo de columnas
     * de un {@code Columns} no tiene elemento: su contenido cuelga del {@code Columns}.
     */
    private void build(Box box, PDStructureNode parent) {
        if (box.role() == Role.FLOW) {
            box.children().forEach(child -> build(child, parent));
            return;
        }
        if (box.isImage() && box.style().image().alt() == null || box.role() == Role.LEADER) {
            // Decorativa (o la guía de un índice): no tiene elemento y se pinta como artifact; su enlace, sí.
            standIns(box, parent);
            return;
        }
        // Una imagen con enlace es el contenido de su Link: Link → Figure.
        PDStructureNode owner = box.isImage() && box.style().link() != null ? newLink(box, parent) : parent;
        PDStructureElement e = element(type(box), owner);
        owner.appendKid(e);
        elements.put(box, e);
        if (box.headingLevel() > 0) {
            headings.add(box);
        }
        attributes(box, e);
        if (box.role() == Role.TABLE) {
            table(box, e);
            return;
        }
        PDStructureNode content = e;
        if (box.role() == Role.LIST_ITEM) {
            // LI: el marcador (Lbl) y el contenido (LBody).
            if (box.marker() != null) {
                PDStructureElement label = element("Lbl", e);
                e.appendKid(label);
                elements.put(box.marker(), label);
            }
            PDStructureElement body = element("LBody", e);
            e.appendKid(body);
            content = body;
        }
        if (pdf2 != null && box.role() == Role.TOC_ENTRY) {
            // PDF/UA-2: un TOCI no contiene un Link, sino una Reference que lo contiene; y señala a su encabezado.
            entries.put(box, e);
            PDStructureElement reference = element("Reference", e);
            e.appendKid(reference);
            content = reference;
        }
        content = linked(box, content);
        children(box, content);
    }

    /**
     * Un elemento de estructura nuevo de ese tipo; con PDF/UA-2, en el espacio de nombres de PDF 2.0 si el tipo está
     * en él. Lo añade a su padre quien lo crea.
     */
    private PDStructureElement element(String type, PDStructureNode parent) {
        PDStructureElement e = new PDStructureElement(type, parent);
        if (pdf2 != null && !PDF_1_7_ONLY.contains(type)) {
            e.getCOSObject().setItem(COSName.getPDFName("NS"), pdf2);
        }
        return e;
    }

    /** Los elementos de los hijos de la caja; de un {@code Page overlay}, que no es contenido, solo sus enlaces. */
    private void children(Box box, PDStructureNode content) {
        for (Box child : box.children()) {
            if (child.style().position() == Position.PAGE_OVERLAY) {
                standIns(child, content);
            } else {
                build(child, content);
            }
        }
    }

    /**
     * Los {@code Link} de los enlaces de una caja que no es contenido y de lo que contiene, en su sitio del árbol: el
     * de la caja ({@code Link} del elemento) y uno por destino de sus enlaces de texto, en orden.
     */
    private void standIns(Box box, PDStructureNode parent) {
        if (box.style().link() != null) {
            standIn(box, box.style().link(), parent);
        }
        if (box.formattedRuns() != null) {
            for (var run : box.formattedRuns()) {
                if (run.format().link() != null) {
                    standIn(box, run.format().link(), parent);
                }
            }
        }
        box.children().forEach(child -> standIns(child, parent));
    }

    private void standIn(Box box, String destination, PDStructureNode parent) {
        standIns.computeIfAbsent(box, k -> new HashMap<>()).computeIfAbsent(destination, k -> {
            PDStructureElement link = element("Link", parent);
            parent.appendKid(link);
            return link;
        });
    }

    /**
     * Lo que el elemento dice de la caja además de su tipo: su idioma si cambia, el texto alternativo de una
     * imagen, el texto escrito si se pinta transformado y la numeración de una lista.
     */
    private void attributes(Box box, PDStructureElement e) {
        Box outer = box.role() == Role.SECTION ? root : box.parent();
        if (outer != null && !box.style().typography().locale().equals(outer.style().typography().locale())) {
            // Lang se hereda por el árbol de estructura: solo donde el idioma cambia respecto al de fuera.
            e.setLanguage(box.style().typography().locale().toLanguageTag());
        }
        if (box.isImage()) {
            e.setAlternateDescription(box.style().image().alt());
        }
        if (box.hasText() && TextTransform.applies(box.style()) && !Reference.contains(box.text())
                && !PageNumbers.contains(box.text())) {
            // Las mayúsculas son solo de pintado (referencia/comun/formato-del-texto.md): el texto de la estructura es
            // el escrito. Si lleva números de página o referencias, el texto cambia con la página y no se puede dar.
            e.setActualText(box.text());
        }
        if (box.role() == Role.LIST) {
            PDListAttributeObject numbering = new PDListAttributeObject();
            numbering.setListNumbering(listNumbering(box.style().list().marker(), ua));
            e.addAttribute(numbering);
        }
    }

    /**
     * Si la caja tiene {@code Link} (referencia/implementacion/pdf.md), el {@code Link} que envuelve su contenido (su
     * texto y sus hijos) y que recibe las anotaciones de su zona; si no, {@code content}. Una imagen es la excepción:
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
        PDStructureElement link = element("Link", parent);
        parent.appendKid(link);
        boxLinks.put(box, link);
        if (!hasContent(box)) {
            emptyLinks.add(link);
        }
        return link;
    }

    /** Si la caja o lo que contiene tiene algo que leer: texto, un marcador o una imagen con {@code Alt}. */
    private static boolean hasContent(Box box) {
        if (box.hasText() || box.marker() != null || box.isImage() && box.style().image().alt() != null) {
            return true;
        }
        return box.children().stream().anyMatch(c -> c.style().position() != Position.PAGE_OVERLAY && hasContent(c));
    }

    /**
     * Una tabla (referencia/componentes/table.md): sus celdas, en orden de filas, en {@code THead}, {@code TBody} y
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
            Cell p = cell.style().item().area();
            int group = group(cell);
            if (groups[group] == null) {
                groups[group] = element(TABLE_GROUPS[group], e);
                e.appendKid(groups[group]);
            }
            if (p.row() != rowIndex) {
                row = element("TR", groups[group]);
                groups[group].appendKid(row);
                rowIndex = p.row();
            }
            PDStructureElement c = element(type(cell), row);
            row.appendKid(c);
            elements.put(cell, c);
            if (group == 0 || cell.style().rowHeader() || p.rows() > 1 || p.columns() > 1) {
                PDTableAttributeObject attributes = new PDTableAttributeObject();
                if (group == 0) {
                    attributes.setScope(PDTableAttributeObject.SCOPE_COLUMN);
                } else if (cell.style().rowHeader()) {
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
            children(cell, linked(cell, c));
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
        int row = cell.style().item().area().row() - 1;
        if (row < table.headRows()) {
            return 0;
        }
        int rows = table.children().stream().mapToInt(c -> c.style().item().area().row() - 1 + c.style().item().area().rows())
                .max().orElse(0);
        return row >= rows - table.footRows() ? 2 : 1;
    }

    /**
     * {@code ListNumbering} de una lista; {@code dash} no tiene equivalente en PDF/UA-1 y es {@code None}, y en
     * PDF/UA-2 es {@code Unordered}, de PDF 2.0.
     */
    private static String listNumbering(Marker marker, int ua) {
        return switch (marker) {
            case BULLET -> PDListAttributeObject.LIST_NUMBERING_DISC;
            case DASH -> ua == 2 ? "Unordered" : PDListAttributeObject.LIST_NUMBERING_NONE;
            case NONE -> PDListAttributeObject.LIST_NUMBERING_NONE;
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
    static void metadata(PDDocument doc, Metadata metadata, int ua) throws IOException {
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
        xmp.importXMPMetadata(xmp(metadata.title(), ua).getBytes(StandardCharsets.UTF_8));
        catalog.setMetadata(xmp);
    }

    /**
     * Paquete XMP mínimo con el título y la declaración de PDF/UA ({@code pdfuaid:part}, 1 o 2; la 2 con la revisión
     * de la norma, {@code pdfuaid:rev}). Un documento siempre tiene {@code Title} (obligatorio): sin título el PDF no
     * cumpliría PDF/UA (7.1) y no podría afirmarlo.
     */
    static String xmp(String title, int ua) {
        String escaped = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<?xpacket begin=\"﻿\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n"
                + "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">\n"
                + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "<rdf:Description rdf:about=\"\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"
                + "<dc:title><rdf:Alt><rdf:li xml:lang=\"x-default\">" + escaped + "</rdf:li></rdf:Alt></dc:title>\n"
                + "</rdf:Description>\n"
                + "<rdf:Description rdf:about=\"\" xmlns:pdfuaid=\"http://www.aiim.org/pdfua/ns/id/\">\n"
                + "<pdfuaid:part>" + ua + "</pdfuaid:part>\n"
                + (ua == 2 ? "<pdfuaid:rev>2024</pdfuaid:rev>\n" : "")
                + "</rdf:Description>\n"
                + "</rdf:RDF>\n"
                + "</x:xmpmeta>\n"
                + "<?xpacket end=\"w\"?>";
    }

    /** Tipo estándar de estructura de la caja (decisión G1 de la hoja de ruta). */
    static String type(Box box) {
        return switch (box.role()) {
            case DOCUMENT -> "Document";
            case SECTION -> "Sect";
            case HEADING -> "H" + box.headingLevel();
            case PARAGRAPH -> "P";
            // Un Text es un bloque de texto, un párrafo; dentro de un encabezado es parte de su texto.
            case TEXT -> box.parent() != null && box.parent().headingLevel() > 0 ? "Span" : "P";
            case LIST -> "L";
            case LIST_ITEM -> "LI";
            case TABLE -> "Table";
            case ROW -> "TR";
            case CELL -> group(box) == 0 || box.style().rowHeader() ? "TH" : "TD";
            case FIGURE -> "Figure";
            case TABLE_OF_CONTENTS -> "TOC";
            case TOC_ENTRY -> "TOCI";
            case SPAN, PAGE_NUMBER -> "Span";
            case LEADER, FLOW, BLOCK -> "Div";
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
     * es un <em>artifact</em> (dentro de un {@code Page overlay}). Cuelga del elemento de la caja o, si la caja
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
        PDStructureElement link = element("Link", holder);
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
     * Etiqueta la zona de una caja con {@code Link} (referencia/implementacion/pdf.md): la
     * anotación es una referencia a objeto ({@code OBJR}) del {@code Link} que envuelve el contenido
     * de la caja. Un {@code Link} sin nada que leer dentro lleva su texto como {@code Alt}. Donde la caja no es
     * contenido, el enlace va al {@code Link} que la sustituye en su sitio ({@link #standIns}) o, en las filas
     * repetidas de una tabla, al de la primera página.
     */
    void link(Box box, String destination, PDAnnotationLink annotation, String description) {
        PDStructureElement parent = elements.get(box);
        if (parent == null) {
            outside(box, destination, annotation, description);
            return;
        }
        PDStructureElement link = boxLinks.get(box);
        if (link == null) {
            // Una caja que no puede envolver su contenido en un Link (una tabla): un Link aparte, hijo suyo.
            link = element("Link", parent);
            link.setPage(page);
            parent.appendKid(link);
            emptyLinks.add(link);
            boxLinks.put(box, link);
        }
        if (emptyLinks.contains(link)) {
            describe(link, annotation, description);
        }
        attach(link, annotation);
    }

    /**
     * Etiqueta la zona de una línea del enlace de texto que empezó {@link #markLink}; si el texto no es contenido, va
     * al {@code Link} de su caja y su destino que lo sustituye en su sitio ({@link #standIns}) o, en las filas
     * repetidas de una tabla, al de la primera página.
     */
    void inlineLink(Box box, String destination, PDAnnotationLink annotation, String description) {
        if (inline != null) {
            firstLinks.computeIfAbsent(box, k -> new HashMap<>()).putIfAbsent(destination, inline);
            attach(inline, annotation);
            return;
        }
        PDStructureElement first = firstLinks.getOrDefault(box, Map.of()).get(destination);
        if (first != null) {
            attach(first, annotation);
        } else {
            outside(box, destination, annotation, description);
        }
    }

    /**
     * Etiqueta el enlace de una caja que no es contenido (en un {@code Page overlay}, una imagen decorativa): PDF/UA
     * exige que toda anotación de enlace esté en un {@code Link}. Es el {@code Link} de la caja y su destino que se
     * puso en su sitio del árbol al construirlo, sin contenido marcado y con el texto del enlace como {@code Alt}: un
     * lector de pantalla lo anuncia una vez, en su orden de lectura, aunque se repita en cada página (un pie con una
     * web).
     */
    private void outside(Box box, String destination, PDAnnotationLink annotation, String description) {
        PDStructureElement link = standIns.getOrDefault(box, Map.of()).get(destination);
        if (link == null) {
            throw new IllegalStateException("Enlace sin Link en la estructura: " + destination);
        }
        describe(link, annotation, description);
        attach(link, annotation);
    }

    /**
     * El {@code Alt} de un {@code Link} sin nada que leer dentro: el texto de su primera zona. Con PDF/UA-2, la
     * anotación no lleva además su texto ({@code Contents}): lo que se lee es el {@code Alt}, y los dos
     * tendrían que coincidir.
     */
    private void describe(PDStructureElement link, PDAnnotationLink annotation, String description) {
        if (link.getAlternateDescription() == null) {
            link.setAlternateDescription(description != null ? description : annotation.getContents());
        }
        if (ua == 2) {
            annotation.getCOSObject().removeItem(COSName.CONTENTS);
        }
    }

    /** El elemento de la caja con esa clave ({@link Box#key()}), o del antecesor más cercano que lo tiene. */
    private PDStructureElement target(String key) {
        for (Box box : elements.keySet()) {
            if (key.equals(box.key())) {
                return element(box);
            }
        }
        return null;
    }

    /** El elemento de la caja o, si no tiene, el del antecesor más cercano que lo tiene. */
    private PDStructureElement element(Box box) {
        for (Box b = box; b != null; b = b.parent()) {
            if (elements.containsKey(b)) {
                return elements.get(b);
            }
        }
        return elements.get(root);
    }

    /**
     * Con PDF/UA-2, la acción que lleva a la caja: un destino de estructura ({@code SD}, el elemento de la caja con la
     * posición de {@code destination}) y, para los visores que no lo entienden, el destino de su página ({@code D});
     * con PDF/UA-1, {@code null}, y se usa el destino de la página.
     */
    PDActionGoTo goTo(Box box, PDPageXYZDestination destination) {
        if (ua != 2) {
            return null;
        }
        COSArray sd = new COSArray();
        sd.add(element(box));
        COSArray d = destination.getCOSObject();
        for (int i = 1; i < d.size(); i++) {
            sd.add(d.get(i));
        }
        PDActionGoTo goTo = new PDActionGoTo();
        goTo.setDestination(destination);
        goTo.getCOSObject().setItem(COSName.getPDFName("SD"), sd);
        return goTo;
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
        // Un Link sin anotaciones (el de un Page overlay que no sale en ninguna página) no puede quedar vacío.
        for (Map<String, PDStructureElement> links : standIns.values()) {
            for (PDStructureElement link : links.values()) {
                if (link.getKids().isEmpty()) {
                    ((PDStructureNode) link.getParent()).removeKid(link);
                }
            }
        }
        // PDF/UA-2: cada entrada de índice señala con Ref el elemento de su encabezado.
        entries.forEach((entry, toci) -> {
            PDStructureElement heading = target(Links.target(entry.style().link()));
            if (heading != null) {
                COSArray ref = new COSArray();
                ref.add(heading);
                toci.getCOSObject().setItem(COSName.getPDFName("Ref"), ref);
            }
        });
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
            PDActionGoTo goTo = goTo(h, destinations.get(h));
            if (goTo != null) {
                item.setAction(goTo);
            } else {
                item.setDestination(destinations.get(h));
            }
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
