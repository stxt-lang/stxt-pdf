package dev.stxt.pdf.paint;

import dev.stxt.pdf.layout.inline.PageNumbers;
import dev.stxt.pdf.layout.inline.TextTransform;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.Links;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element.Role;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDStructureElementNameTreeNode;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDDefaultAttributeObject;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Meaning of the document in the PDF (reference/implementation/pdf.md): the structure tree of the tagged PDF and the
 * bookmarks (<em>outline</em>) of the headings.
 *
 * <p>Structure tree: one element per box, in document order, which is the reading
 * order. {@code Document} for the root, {@code H1} to {@code H6} for the headings, {@code P} for {@code Paragraph}
 * and for {@code Text} ({@code Span} if it is part of the text of a heading), {@code L} and {@code LI} for lists and
 * their items (with the marker in {@code Lbl} and the content in {@code LBody}, reference/components/list.md), {@code
 * Table} with {@code THead}, {@code TBody}, {@code TFoot}, {@code TR}, {@code TH} and {@code TD} for tables
 * (reference/components/table.md), {@code Figure} for images with {@code Alt} (with their alternative text and their
 * {@code BBox}, reference/components/image.md) and {@code Div} for the rest, a {@code Columns} included
 * (reference/components/columns.md), whose columns have no element of their own. The text of each fragment is
 * marked content ({@code BDC}/{@code EMC} with its {@code MCID}) that hangs from the element of its box; a box broken
 * across pages has content on several. Backgrounds, borders, images without {@code Alt} (decorative) and
 * {@code Page overlay} boxes (repeated on every page) are <em>artifacts</em>: they are not part of the content.</p>
 *
 * <p>The language of a box that declares a different one ({@code Language} in {@code Typography},
 * reference/common/typography.md) is the {@code Lang} of its element, inherited by the ones inside.</p>
 *
 * <p>A link (reference/implementation/pdf.md) is a {@code Link} that wraps what is clicked and carries the PDF
 * annotation as an object reference ({@code OBJR}): that of a whole box ({@code Link} of the element), child of the
 * element of the box and parent of its content (an image hangs from it, not the other way round); that of a text run
 * ({@code [text](destination)}), child of the element of the box with the marked content of its text, one per link
 * and fragment even if it takes several lines.</p>
 *
 * <p>Bookmarks: one per heading, nested by level (the hierarchy is correct, reference/common/errors.md), that
 * lead to the top left corner of their first fragment ({@link PdfDestinations}).</p>
 */
final class PdfStructure {

    private final PDStructureTreeRoot treeRoot = new PDStructureTreeRoot();
    /** Structure element of each box; those of a {@code Page overlay} have none. */
    private final Map<Box, PDStructureElement> elements = new HashMap<>();
    /** Per page, the elements each {@code MCID} belongs to: the parent tree. */
    private final Map<Integer, PDParentTreeValue> parents = new HashMap<>();
    /** Headings in document order. */
    private final List<Box> headings = new ArrayList<>();
    /** Boxes of the header and the footer of tables, which repeat on every page of their table. */
    private final Set<Box> repeated = new HashSet<>();
    /** First page with content of each box of {@link #repeated}: on the others it is an <em>artifact</em>. */
    private final Map<Box, PDPage> firstPage = new HashMap<>();

    /** The {@code Link} of each box with {@code Link}: it holds its content and the annotations of its area. */
    private final Map<Box, PDStructureElement> boxLinks = new HashMap<>();
    /** Where the own content (the text) of the boxes with {@code Link} hangs: their {@code Link}. */
    private final Map<Box, PDStructureElement> holders = new HashMap<>();
    /** The {@code Link} elements of boxes without text or figure, read by their {@code Alt}, not by their content. */
    private final Set<PDStructureElement> emptyLinks = new HashSet<>();
    /** The {@code Link} of the text link being painted, to hang the annotation of its area from it. */
    private PDStructureElement inline;
    /**
     * The {@code Link} elements of the links of what is not content (a {@code Page overlay}, a decorative image), by
     * box and destination: they are in the place of the box in the tree, without content, and receive the annotations
     * of all the pages.
     */
    private final Map<Box, Map<String, PDStructureElement>> standIns = new HashMap<>();
    /**
     * The {@code Link} elements of the text links of the boxes that repeat (the header and the footer of a table), by
     * box and destination: those of the first page, which receive the annotations of the repetitions.
     */
    private final Map<Box, Map<String, PDStructureElement>> firstLinks = new HashMap<>();

    /** The standard namespace of PDF 2.0 (ISO 32000-2, 14.8.6). */
    static final String PDF_2_NAMESPACE = "http://iso.org/pdf2/ssn";

    private static final String[] TABLE_GROUPS = {"THead", "TBody", "TFoot"};
    /** The types that are not in the PDF 2.0 namespace: they stay in that of PDF 1.7, the default one. */
    private static final Set<String> PDF_1_7_ONLY = Set.of("TOC", "TOCI", "Reference");

    /** The part of PDF/UA the PDF complies with: 1 or 2 (reference/implementation/pdf.md). */
    private final int ua;
    /** With PDF/UA-2, the PDF 2.0 namespace of the elements; with PDF/UA-1, {@code null}. */
    private final COSDictionary pdf2;
    /** With PDF/UA-2, the {@code TOCI} of each table of contents entry, pointing to its heading with {@code Ref}. */
    private final Map<Box, PDStructureElement> entries = new HashMap<>();
    /** The {@code Footnote} boxes of the document, by {@code Id} (reference/components/footnote.md). */
    private final Map<String, Box> notes = new HashMap<>();
    /** The {@code Lbl} of each footnote: the number that opens it. */
    private final Map<Box, PDStructureElement> labels = new HashMap<>();
    /** The element of each footnote, by its {@code Id}, for the {@code IDTree} that PDF/UA-1 asks for a {@code Note}. */
    private final Map<String, PDStructureElement> noteIds = new TreeMap<>();

    /**
     * The root of the document: it is the "parent" of its sections, which have none (reference/components/document.md).
     */
    private final Box root;

    private PDPage page;
    private COSArray pageParents;

    /** {@code ua}: the part of PDF/UA, 1 or 2. */
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
        footnotes(root);
        build(root, treeRoot);
    }

    /**
     * The footnotes of the document: the children of the footnote area of each section, which hangs from the section
     * out of its flow. Their elements are built after the text that calls them ({@link #children}), in reading order.
     */
    private void footnotes(Box root) {
        for (Box section : root.children()) {
            for (Box child : section.children()) {
                if (child.role() == Role.FOOTNOTES) {
                    child.children().forEach(note -> notes.put(note.id(), note));
                }
            }
        }
    }

    /**
     * Elements of the box and of its descendants, except {@code Page overlay} boxes. The column flow
     * of a {@code Columns} has no element: its content hangs from the {@code Columns}.
     */
    private void build(Box box, PDStructureNode parent) {
        if (box.role() == Role.FLOW) {
            box.children().forEach(child -> build(child, parent));
            return;
        }
        if (box.isImage() && box.style().image().alt() == null || box.role() == Role.LEADER) {
            // Decorative (or the leader of a table of contents): no element, painted as an artifact; its link has one.
            standIns(box, parent);
            return;
        }
        // An image with a link is the content of its Link: Link → Figure.
        PDStructureNode owner = box.isImage() && box.style().link() != null ? newLink(box, parent) : parent;
        PDStructureElement e = element(type(box, ua), owner);
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
        if (box.role() == Role.NOTE) {
            // Note (PDF/UA-1) or FENote (PDF/UA-2): its identifier and the label with its number, before its content.
            e.setElementIdentifier("footnote-" + box.id());
            noteIds.put("footnote-" + box.id(), e);
            if (pdf2 != null) {
                COSDictionary noteType = new COSDictionary();
                noteType.setName(COSName.O, "FENote");
                noteType.setName(COSName.getPDFName("NoteType"), "Footnote");
                e.addAttribute(new PDDefaultAttributeObject(noteType));
            }
            PDStructureElement label = element("Lbl", e);
            e.appendKid(label);
            labels.put(box, label);
        }
        if (box.role() == Role.LIST_ITEM) {
            // LI: the marker (Lbl) and the content (LBody).
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
            // PDF/UA-2: a TOCI does not contain a Link, but a Reference that contains it; and it points to its heading.
            entries.put(box, e);
            PDStructureElement reference = element("Reference", e);
            e.appendKid(reference);
            content = reference;
        }
        content = linked(box, content);
        children(box, content);
    }

    /**
     * A new structure element of that type; with PDF/UA-2, in the PDF 2.0 namespace if the type is in it. Whoever
     * creates it adds it to its parent.
     */
    private PDStructureElement element(String type, PDStructureNode parent) {
        PDStructureElement e = new PDStructureElement(type, parent);
        if (pdf2 != null && !PDF_1_7_ONLY.contains(type)) {
            e.getCOSObject().setItem(COSName.getPDFName("NS"), pdf2);
        }
        return e;
    }

    /**
     * The elements of the children of the box; of a {@code Page overlay}, which is not content, only its links. The
     * footnote area of a section has no element: each of its notes goes right after the text that calls it
     * (reference/components/footnote.md), as its sibling, or inside the cell whose text calls it.
     */
    private void children(Box box, PDStructureNode content) {
        for (Box child : box.children()) {
            if (child.style().position() == Position.PAGE_OVERLAY) {
                standIns(child, content);
            } else if (child.role() != Role.FOOTNOTES) {
                build(child, content);
                if (child.hasText()) {
                    PDStructureNode parent = child.role() == Role.CELL ? elements.get(child) : content;
                    for (TextRun run : child.runs()) {
                        if (run.format().footnote() != null && notes.containsKey(run.format().footnote())) {
                            build(notes.get(run.format().footnote()), parent);
                        }
                    }
                }
            }
        }
    }

    /**
     * The {@code Link} elements of the links of a box that is not content and of what it contains, in its place in
     * the tree: that of the box ({@code Link} of the element) and one per destination of its text links, in order.
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
     * What the element says about the box besides its type: its language if it changes, the alternative text of an
     * image, the written text if it is painted transformed, and the numbering of a list.
     */
    private void attributes(Box box, PDStructureElement e) {
        Box outer = box.role() == Role.SECTION ? root : box.parent();
        if (outer != null && !box.style().typography().locale().equals(outer.style().typography().locale())) {
            // Lang is inherited down the structure tree: only where the language changes from the outer one.
            e.setLanguage(box.style().typography().locale().toLanguageTag());
        }
        if (box.isImage()) {
            e.setAlternateDescription(box.style().image().alt());
        }
        if (box.hasText() && TextTransform.applies(box.style()) && !Reference.contains(box.text())
                && !PageNumbers.contains(box.text())) {
            // Upper case is only a painting matter (reference/common/text-formatting.md): the text of the structure
            // is the written one. If it has page numbers or references, the text changes with the page and cannot be
            // given.
            e.setActualText(box.text());
        }
        if (box.role() == Role.LIST) {
            PDListAttributeObject numbering = new PDListAttributeObject();
            numbering.setListNumbering(listNumbering(box.style().list().marker(), ua));
            e.addAttribute(numbering);
        }
    }

    /**
     * If the box has {@code Link} (reference/implementation/pdf.md), the {@code Link} that wraps its content (its
     * text and its children) and that receives the annotations of its area; otherwise, {@code content}. An image is
     * the exception: its {@code Link} wraps its {@code Figure} ({@link #build}).
     */
    private PDStructureNode linked(Box box, PDStructureNode content) {
        if (box.style().link() == null || box.isImage()) {
            return content;
        }
        PDStructureElement link = newLink(box, content);
        holders.put(box, link);
        return link;
    }

    /** A {@code Link} child of {@code parent} for the box. */
    private PDStructureElement newLink(Box box, PDStructureNode parent) {
        PDStructureElement link = element("Link", parent);
        parent.appendKid(link);
        boxLinks.put(box, link);
        if (!hasContent(box)) {
            emptyLinks.add(link);
        }
        return link;
    }

    /** Whether the box or what it contains has something to read: text, a marker or an image with {@code Alt}. */
    private static boolean hasContent(Box box) {
        if (box.hasText() || box.marker() != null || box.isImage() && box.style().image().alt() != null) {
            return true;
        }
        return box.children().stream().anyMatch(c -> c.style().position() != Position.PAGE_OVERLAY && hasContent(c));
    }

    /**
     * A table (reference/components/table.md): its cells, in row order, in {@code THead}, {@code TBody} and
     * {@code TFoot}, one {@code TR} per row. Each cell is {@code TH} (with {@code Scope:
     * Column}) in the header and {@code TD} in the rest, with {@code RowSpan} and {@code ColSpan} if
     * it spans more than one row or column. The cells of the header and the footer, and what they contain,
     * repeat on every page: only the first time are they content.
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

    /** Records the box and its descendants (and the marker of the items) as content that repeats. */
    private void repeats(Box box) {
        repeated.add(box);
        if (box.marker() != null) {
            repeated.add(box.marker());
        }
        box.children().forEach(this::repeats);
    }

    /** Group of a table cell: 0 in the header, 1 in the body, 2 in the footer. */
    private static int group(Box cell) {
        Box table = cell.parent();
        // Row from 0 of the cell in the grid of the table; the placement comes from TableBuilder.
        int row = cell.style().item().area().row() - 1;
        if (row < table.headerRows()) {
            return 0;
        }
        int rows = table.children().stream().mapToInt(c -> c.style().item().area().row() - 1 + c.style().item().area().rows())
                .max().orElse(0);
        return row >= rows - table.footerRows() ? 2 : 1;
    }

    /**
     * {@code ListNumbering} of a list; {@code dash} has no equivalent in PDF/UA-1 and is {@code None}, and in
     * PDF/UA-2 it is {@code Unordered}, from PDF 2.0.
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

    /** Standard structure type of the box (decision G1 of the roadmap). */
    static String type(Box box) {
        return switch (box.role()) {
            case DOCUMENT -> "Document";
            case SECTION -> "Sect";
            case HEADING -> "H" + box.headingLevel();
            case PARAGRAPH -> "P";
            // A Text is a block of text, a paragraph; inside a heading it is part of its text.
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
            // A footnote: Note in PDF 1.7, FENote in PDF 2.0 (PDF/UA-2); the choice is made by whoever has the part.
            case NOTE -> "Note";
            case LEADER, FLOW, BLOCK, FOOTNOTES -> "Div";
        };
    }

    /** The structure type of a footnote: {@code Note}, or {@code FENote} with PDF/UA-2, which is PDF 2.0. */
    private static String type(Box box, int ua) {
        return box.role() == Role.NOTE && ua == 2 ? "FENote" : type(box);
    }

    /** Starts the content of a page. */
    void startPage(PDPage page) {
        this.page = page;
        pageParents = new COSArray();
        int key = parents.size();
        page.setStructParents(key);
        parents.put(key, new PDParentTreeValue(pageParents));
    }

    /**
     * Records the text of a fragment of the box on the current page and gives its {@code MCID}; -1 if
     * it is an <em>artifact</em> (inside a {@code Page overlay}). It hangs from the element of the box or, if the box
     * has {@code Link}, from that.
     */
    int mark(Box box) {
        PDStructureElement e = holders.containsKey(box) ? holders.get(box) : elements.get(box);
        if (e == null || !isContent(box)) {
            return -1;
        }
        return reference(e);
    }

    /**
     * Records the text of a link of the box ({@code [text](destination)}) as content of a new
     * {@code Link}, child of the element of the box, on the current page and gives its {@code MCID}; -1 if it is
     * an <em>artifact</em>. The annotations of its areas hang from it ({@link #inlineLink}).
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

    /**
     * Records the call of a footnote in the text of the box (reference/components/footnote.md) as the content of a
     * new {@code Reference}, child of the element of the box, that points with {@code Ref} to the note, which points
     * back to it; gives its {@code MCID}, or -1 if the text is not content.
     */
    int markFootnoteCall(Box box, String footnote) {
        PDStructureElement holder = holders.containsKey(box) ? holders.get(box) : elements.get(box);
        PDStructureElement note = elements.get(notes.get(footnote));
        if (holder == null || note == null || !isContent(box)) {
            return -1;
        }
        PDStructureElement reference = element("Reference", holder);
        reference.setPage(page);
        holder.appendKid(reference);
        refer(reference, note);
        refer(note, reference);
        return reference(reference);
    }

    /** Adds {@code target} to the {@code Ref} of {@code e}: the elements it refers to. */
    private static void refer(PDStructureElement e, PDStructureElement target) {
        COSName key = COSName.getPDFName("Ref");
        COSArray ref = e.getCOSObject().getCOSArray(key);
        if (ref == null) {
            ref = new COSArray();
            e.getCOSObject().setItem(key, ref);
        }
        ref.add(target);
    }

    /**
     * Records the number that opens a footnote, in its first text, as the content of the {@code Lbl} of the note
     * (reference/components/footnote.md); gives its {@code MCID}, or -1 if it is not inside a note.
     */
    int markFootnoteLabel(Box box) {
        for (Box b = box; b != null; b = b.parent()) {
            if (labels.containsKey(b)) {
                return reference(labels.get(b));
            }
        }
        return -1;
    }

    /** A marked content of the current page that hangs from {@code e}; its {@code MCID}. */
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
     * Whether the box is content on the current page: the header and the footer of a table are on the
     * first page they appear on, and not on the others, where they repeat.
     */
    private boolean isContent(Box box) {
        if (repeated.contains(box)) {
            PDPage first = firstPage.putIfAbsent(box, page);
            return first == null || first == page;
        }
        return true;
    }

    /**
     * Tags the area of a box with {@code Link} (reference/implementation/pdf.md): the
     * annotation is an object reference ({@code OBJR}) of the {@code Link} that wraps the content
     * of the box. A {@code Link} with nothing to read inside carries its text as {@code Alt}. Where the box is not
     * content, the link goes to the {@code Link} that stands in for it in its place ({@link #standIns}) or, in the
     * repeated rows of a table, to that of the first page.
     */
    void link(Box box, String destination, PDAnnotationLink annotation, String description) {
        PDStructureElement parent = elements.get(box);
        if (parent == null) {
            outside(box, destination, annotation, description);
            return;
        }
        PDStructureElement link = boxLinks.get(box);
        if (link == null) {
            // A box that cannot wrap its content in a Link (a table): a separate Link, a child of it.
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
     * Tags the area of a line of the text link started by {@link #markLink}; if the text is not content, it goes to
     * the {@code Link} of its box and its destination that stands in for it in its place ({@link #standIns}) or, in
     * the repeated rows of a table, to that of the first page.
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
     * Tags the link of a box that is not content (in a {@code Page overlay}, a decorative image): PDF/UA
     * requires every link annotation to be in a {@code Link}. It is the {@code Link} of the box and its destination
     * placed in its place in the tree when building it, without marked content and with the text of the link as
     * {@code Alt}: a screen reader announces it once, in its reading order, even if it repeats on every page (a footer
     * with a website).
     */
    private void outside(Box box, String destination, PDAnnotationLink annotation, String description) {
        PDStructureElement link = standIns.getOrDefault(box, Map.of()).get(destination);
        if (link == null) {
            throw new IllegalStateException("Link without a Link element in the structure: " + destination);
        }
        describe(link, annotation, description);
        attach(link, annotation);
    }

    /**
     * The {@code Alt} of a {@code Link} with nothing to read inside: the text of its first area. With PDF/UA-2, the
     * annotation does not also carry its text ({@code Contents}): what is read is the {@code Alt}, and the two
     * would have to match.
     */
    private void describe(PDStructureElement link, PDAnnotationLink annotation, String description) {
        if (link.getAlternateDescription() == null) {
            link.setAlternateDescription(description != null ? description : annotation.getContents());
        }
        if (ua == 2) {
            annotation.getCOSObject().removeItem(COSName.CONTENTS);
        }
    }

    /** The element of the box with that key ({@link Box#key()}), or of the nearest ancestor that has one. */
    private PDStructureElement target(String key) {
        for (Box box : elements.keySet()) {
            if (key.equals(box.key())) {
                return element(box);
            }
        }
        return null;
    }

    /** The element of the box or, if it has none, that of the nearest ancestor that has one. */
    private PDStructureElement element(Box box) {
        for (Box b = box; b != null; b = b.parent()) {
            if (elements.containsKey(b)) {
                return elements.get(b);
            }
        }
        return elements.get(root);
    }

    /**
     * With PDF/UA-2, the action that leads to the box: a structure destination ({@code SD}, the element of the box
     * with the position of {@code destination}) and, for viewers that do not understand it, the destination of its
     * page ({@code D}); with PDF/UA-1, {@code null}, and the page destination is used.
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
        // The annotation is visible from the parent tree with its own key, next to those of the pages.
        int key = parents.size();
        annotation.setStructParent(key);
        parents.put(key, new PDParentTreeValue(link.getCOSObject()));
    }

    /**
     * The {@code BBox} of the {@code Figure} of an image: its content box on the page, in
     * PDF coordinates. An image is not broken, so it is only on one page.
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
     * Finishes: the parent tree and the bookmarks of the headings, which lead to their destination in
     * {@code destinations}.
     */
    void finish(PDDocument doc, Map<Box, PDPageXYZDestination> destinations) {
        // A Link without annotations (that of a Page overlay that appears on no page) cannot be left empty.
        for (Map<String, PDStructureElement> links : standIns.values()) {
            for (PDStructureElement link : links.values()) {
                if (link.getKids().isEmpty()) {
                    ((PDStructureNode) link.getParent()).removeKid(link);
                }
            }
        }
        // PDF/UA-2: each table of contents entry points with Ref to the element of its heading.
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
        if (!noteIds.isEmpty()) {
            // The identifiers of the footnotes (Note needs one in PDF/UA-1), in the tree that maps them.
            PDStructureElementNameTreeNode idTree = new PDStructureElementNameTreeNode();
            idTree.setNames(noteIds);
            treeRoot.setIDTree(idTree);
        }
        if (headings.isEmpty()) {
            return;
        }
        PDDocumentOutline outline = new PDDocumentOutline();
        // Stack of the open headings: the parent of each one is the last one of a lower level.
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
