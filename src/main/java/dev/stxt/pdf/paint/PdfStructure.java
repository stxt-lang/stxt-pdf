package dev.stxt.pdf.paint;

import dev.stxt.pdf.layout.inline.PageNumbers;
import dev.stxt.pdf.layout.inline.TextTransform;
import dev.stxt.pdf.model.Cell;
import dev.stxt.pdf.model.InlineImage;
import dev.stxt.pdf.model.Links;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element.Role;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDStructureElementNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDLayoutAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDListAttributeObject;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDTableAttributeObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Meaning of the document in the PDF (reference/implementation/pdf.md): the structure tree of the tagged PDF.
 *
 * <p>Structure tree: one element per box, in document order, which is the reading
 * order. {@code Document} for the root, {@code H1} to {@code H6} for the headings, {@code P} for {@code Paragraph}
 * and for {@code Text} ({@code Span} if it is part of the text of a heading), {@code L} and {@code LI} for lists and
 * their items (with the marker in {@code Lbl} and the content in {@code LBody}, reference/components/list.md), {@code
 * Table} with {@code THead}, {@code TBody}, {@code TFoot}, {@code TR}, {@code TH} and {@code TD} for tables
 * (reference/components/table.md), {@code Figure} for images with {@code Alt} (with their alternative text and their
 * {@code BBox}, reference/components/image.md) and for the {@code Figure} elements with their {@code Caption}
 * (reference/components/figure.md; with a table inside, the {@code Table} carries the {@code Caption}) and {@code Div}
 * for the rest, a {@code Columns} included
 * (reference/components/columns.md), whose columns have no element of their own. The text of each fragment is
 * marked content ({@code BDC}/{@code EMC} with its {@code MCID}) that hangs from the element of its box; a box broken
 * across pages has content on several. Backgrounds, borders, images without {@code Alt} (decorative) and
 * {@code Page overlay} boxes (repeated on every page) are <em>artifacts</em>: they are not part of the content.</p>
 *
 * <p>The language of a box that declares a different one ({@code Language} in {@code Typography},
 * reference/common/typography.md) is the {@code Lang} of its element, inherited by the ones inside.</p>
 *
 * <p>The {@code Link} elements and the annotations that hang from them are {@link PdfStructureLinks}; the way back
 * from each page to the tree (the parent tree), {@link PdfParentTree}; what PDF/UA-2 does differently from PDF/UA-1,
 * {@link UaProfile}; and the bookmarks of the headings ({@link #headings}), {@link PdfOutline}.</p>
 */
final class PdfStructure {

    private final PDStructureTreeRoot treeRoot = new PDStructureTreeRoot();
    /** Structure element of each box; those of a {@code Page overlay} have none. */
    private final Map<Box, PDStructureElement> elements = new HashMap<>();
    /** Headings in document order. */
    private final List<Box> headings = new ArrayList<>();
    /** Boxes of the header and the footer of tables, which repeat on every page of their table. */
    private final Set<Box> repeated = new HashSet<>();
    /** Rows of the grid of each table ({@link #rows}). */
    private final Map<Box, Integer> tableRows = new HashMap<>();
    /** First page with content of each box of {@link #repeated}: on the others it is an <em>artifact</em>. */
    private final Map<Box, PDPage> firstPage = new HashMap<>();

    private static final String[] TABLE_GROUPS = {"THead", "TBody", "TFoot"};

    /** What the part of PDF/UA the PDF complies with changes in the tree (reference/implementation/pdf.md). */
    private final UaProfile profile;
    /**
     * Whether nothing is tagged ({@link #none}): the pages are painted on a document that is not the engine's
     * ({@link PdfPainter}), whose structure tree, if it has one, is not touched, so every content is an
     * <em>artifact</em> and the link annotations hang from no {@code Link}.
     */
    private final boolean untagged;
    /** The parent tree: per page, the element of each marked content, and the {@code Link} of each annotation. */
    private final PdfParentTree parentTree = new PdfParentTree();
    /** The {@code Link} elements and the annotations that hang from them. */
    private final PdfStructureLinks links;
    /** With PDF/UA-2, the {@code TOCI} of each table of contents entry, pointing to its heading with {@code Ref}. */
    private final Map<Box, PDStructureElement> entries = new HashMap<>();
    /** The {@code Footnote} boxes of the document, by {@code Id} (reference/components/footnote.md). */
    private final Map<String, Box> notes = new HashMap<>();
    /** The {@code Lbl} of each footnote: the number that opens it. */
    private final Map<Box, PDStructureElement> labels = new HashMap<>();
    /** The element of each footnote, by its {@code Id}, for the {@code IDTree} that PDF/UA-1 asks for a {@code Note}. */
    private final Map<String, PDStructureElement> noteIds = new TreeMap<>();

    /** The root of the document. */
    private final Box root;

    /** The structure of pages painted on a document that is not the engine's: nothing is tagged. */
    static PdfStructure none() {
        return new PdfStructure();
    }

    private PdfStructure() {
        this.root = null;
        this.profile = UaProfile.of(1);
        this.untagged = true;
        this.links = new PdfStructureLinks(profile, parentTree);
    }

    /** The structure of the document with root {@code root}, for the part of PDF/UA of {@code profile}. */
    PdfStructure(PDDocument doc, Box root, UaProfile profile) {
        this.root = root;
        this.profile = profile;
        this.untagged = false;
        this.links = new PdfStructureLinks(profile, parentTree);
        profile.declare(treeRoot);
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
            links.standIns(box, parent);
            return;
        }
        if (box.role() == Role.FIGURE_GROUP) {
            figure(box, parent);
            return;
        }
        // An image with a link is the content of its Link: Link → Figure.
        PDStructureNode owner = box.isImage() && box.style().link() != null ? links.newLink(box, parent) : parent;
        PDStructureElement e = profile.element(elementType(box), owner);
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
            profile.describeNote(e);
            PDStructureElement label = profile.element("Lbl", e);
            e.appendKid(label);
            labels.put(box, label);
        }
        if (box.role() == Role.LIST_ITEM) {
            // LI: the marker (Lbl) and the content (LBody). An image marker with Alt is a Figure inside the Lbl;
            // without Alt it is decorative, painted as an artifact, and the item has no Lbl.
            Box marker = box.marker();
            if (marker != null && (!marker.isImage() || marker.style().image().alt() != null)) {
                PDStructureElement label = profile.element("Lbl", e);
                e.appendKid(label);
                if (marker.isImage()) {
                    PDStructureElement figure = profile.element("Figure", label);
                    label.appendKid(figure);
                    figure.setAlternateDescription(marker.style().image().alt());
                    elements.put(marker, figure);
                } else {
                    elements.put(marker, label);
                }
            }
            PDStructureElement body = profile.element("LBody", e);
            e.appendKid(body);
            content = body;
        }
        if (profile.tocEntryWrapsReference() && box.role() == Role.TOC_ENTRY) {
            // PDF/UA-2: a TOCI does not contain a Link, but a Reference that contains it; and it points to its heading.
            entries.put(box, e);
            PDStructureElement reference = profile.element("Reference", e);
            e.appendKid(reference);
            content = reference;
        }
        content = links.wrap(box, content);
        children(box, content);
    }

    /**
     * A {@code Figure} element (reference/components/figure.md): with an image or a {@code Block} inside, it is a
     * {@code Figure} with the alternative text (that of the image, or its own) and the {@code Caption}, in the order
     * they are laid out; the image inside has no element of its own, its content hangs from the {@code Figure} (or
     * from its {@code Link}), which gets the {@code BBox}. With a {@code Table} inside, the table carries the
     * {@code Caption}, as its first or its last child, and the figure has no element. The footnotes called from the
     * caption go after the figure.
     */
    private void figure(Box figure, PDStructureNode parent) {
        Box caption = figure.caption();
        Box content = figure.children().stream().filter(c -> c != caption).findFirst().orElseThrow();
        boolean captionFirst = figure.children().get(0) == caption;
        if (content.role() == Role.TABLE) {
            build(content, parent);
            PDStructureElement table = elements.get(content);
            // What points to the figure (a table of contents entry, a link, a bookmark destination) finds the table.
            elements.put(figure, table);
            PDStructureElement cap = profile.element("Caption", table);
            if (captionFirst && !table.getKids().isEmpty()) {
                table.insertBefore(cap, table.getKids().get(0));
            } else {
                table.appendKid(cap);
            }
            elements.put(caption, cap);
            attributes(caption, cap);
            captionNotes(caption, parent);
            return;
        }
        PDStructureElement e = profile.element("Figure", parent);
        parent.appendKid(e);
        elements.put(figure, e);
        attributes(figure, e);
        e.setAlternateDescription(content.isImage() ? content.style().image().alt() : figure.style().image().alt());
        for (Box child : figure.children()) {
            if (child == caption) {
                PDStructureElement cap = profile.element("Caption", e);
                e.appendKid(cap);
                elements.put(caption, cap);
                attributes(caption, cap);
            } else if (child.isImage()) {
                // Its content is the content of the Figure or, with a Link, of a Link inside it: Figure → Link → image.
                elements.put(child, child.style().link() != null ? links.newLink(child, e) : e);
            } else {
                build(child, e);
            }
        }
        captionNotes(caption, parent);
    }

    /** The footnotes called from the caption of a figure (reference/components/footnote.md): after the figure. */
    private void captionNotes(Box caption, PDStructureNode after) {
        for (TextRun run : caption.runs()) {
            if (run.format().footnote() != null && notes.containsKey(run.format().footnote())) {
                build(notes.get(run.format().footnote()), after);
            }
        }
    }

    /**
     * The elements of the children of the box; of a {@code Page overlay}, which is not content, only its links. The
     * footnote area of a section has no element: each of its notes goes right after the text that calls it
     * (reference/components/footnote.md), as its sibling, or inside the cell whose text calls it.
     */
    private void children(Box box, PDStructureNode content) {
        for (Box child : box.children()) {
            if (child.style().position() == Position.PAGE_OVERLAY) {
                links.standIns(child, content);
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
     * What the element says about the box besides its type: its language if it changes, the alternative text of an
     * image, the written text if it is painted transformed, and the numbering of a list.
     */
    private void attributes(Box box, PDStructureElement e) {
        Box outer = box.parent();
        if (outer != null && !box.style().typography().locale().equals(outer.style().typography().locale())) {
            // Lang is inherited down the structure tree: only where the language changes from the outer one.
            e.setLanguage(box.style().typography().locale().toLanguageTag());
        }
        if (box.isImage()) {
            e.setAlternateDescription(box.style().image().alt());
        }
        if (box.hasText() && TextTransform.applies(box.runs(), box.style()) && !Reference.contains(box.text())
                && !PageNumbers.contains(box.text())) {
            // Upper case is only a painting matter (reference/common/text-formatting.md): the text of the structure
            // is the written one. If it has page numbers or references, the text changes with the page and cannot be
            // given.
            e.setActualText(Reference.shown(box.text()));
        }
        if (box.role() == Role.LIST) {
            PDListAttributeObject numbering = new PDListAttributeObject();
            numbering.setListNumbering(profile.listNumbering(box.style().list().marker()));
            e.addAttribute(numbering);
        }
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
                groups[group] = profile.element(TABLE_GROUPS[group], e);
                e.appendKid(groups[group]);
            }
            if (p.row() != rowIndex) {
                row = profile.element("TR", groups[group]);
                groups[group].appendKid(row);
                rowIndex = p.row();
            }
            PDStructureElement c = profile.element(type(cell), row);
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
            children(cell, links.wrap(cell, c));
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
    private int group(Box cell) {
        Box table = cell.parent();
        // Row from 0 of the cell in the grid of the table; the placement comes from TableBuilder.
        int row = cell.style().item().area().row() - 1;
        if (row < table.headerRows()) {
            return 0;
        }
        return row >= rows(table) - table.footerRows() ? 2 : 1;
    }

    /**
     * Rows of the grid of a table: the last one any cell reaches. Computed once per table, because it is asked for
     * every cell and for every marked content of a cell, and a table of thousands of cells would go through all of them
     * each time.
     */
    private int rows(Box table) {
        Integer rows = tableRows.get(table);
        if (rows == null) {
            rows = table.children().stream()
                    .mapToInt(c -> c.style().item().area().row() - 1 + c.style().item().area().rows()).max().orElse(0);
            tableRows.put(table, rows);
        }
        return rows;
    }

    /** Standard structure type of the box (reference/implementation/pdf.md); of an image marker, its {@code Figure}. */
    String type(Box box) {
        if (box.isMarker() && box.isImage()) {
            return "Figure";
        }
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
            case FIGURE, FIGURE_GROUP -> "Figure";
            case CAPTION -> "Caption";
            case TABLE_OF_CONTENTS -> "TOC";
            case TOC_ENTRY -> "TOCI";
            case SPAN, PAGE_NUMBER -> "Span";
            // A footnote: Note in PDF 1.7, FENote in PDF 2.0 (PDF/UA-2); its element takes the type of the profile.
            case NOTE -> "Note";
            case LEADER, FLOW, BLOCK, FOOTNOTES -> "Div";
        };
    }

    /** The type of the element of the box: that of a footnote is the profile's ({@link UaProfile#noteType}). */
    private String elementType(Box box) {
        return box.role() == Role.NOTE ? profile.noteType() : type(box);
    }

    /** The headings of the document, in document order: those with a bookmark ({@link PdfOutline}). */
    List<Box> headings() {
        return Collections.unmodifiableList(headings);
    }

    /** Starts the content of a page. */
    void startPage(PDPage page) {
        if (untagged) {
            return;
        }
        parentTree.startPage(page);
    }

    /** Where the own content of the box hangs: its {@code Link} if it has one, or its element. */
    private PDStructureElement holder(Box box) {
        PDStructureElement link = links.holder(box);
        return link != null ? link : elements.get(box);
    }

    /**
     * Records the text of a fragment of the box on the current page and gives its {@code MCID}; -1 if
     * it is an <em>artifact</em> (inside a {@code Page overlay}). It hangs from the element of the box or, if the box
     * has {@code Link}, from that.
     */
    int mark(Box box) {
        PDStructureElement e = holder(box);
        if (untagged || e == null || !isContent(box)) {
            return -1;
        }
        return parentTree.mark(e);
    }

    /**
     * Records the text of a link of the box ({@code [text](destination)}) as content of a new
     * {@code Link}, child of the element of the box, on the current page and gives its {@code MCID}; -1 if it is
     * an <em>artifact</em>. The annotations of its areas hang from it ({@link #inlineLink}).
     */
    int markLink(Box box) {
        PDStructureElement holder = holder(box);
        boolean content = !untagged && holder != null && isContent(box);
        PDStructureElement link = links.beginInline(content ? holder : null, false);
        return link == null ? -1 : parentTree.mark(link);
    }

    /**
     * Records an image inside the text of the box (reference/common/text-formatting.md) on the current page and
     * gives its {@code MCID}: with alternative text, the content of a new {@code Figure}, with that text as
     * {@code Alt} and its {@code BBox}, child of the element of the box (or of its {@code Link}); -1 if the image is
     * decorative or the text is not content. In a link ({@code linked}), a new {@code Link} is the parent of the
     * figure and receives the annotation of the image ({@link #inlineLink}); a decorative image in a link has the
     * {@code Link} alone, read by its {@code Alt}.
     */
    int markInlineImage(Box box, InlineImage image, boolean linked, PDRectangle bbox) {
        PDStructureElement holder = holder(box);
        boolean content = !untagged && holder != null && isContent(box);
        PDStructureElement link = links.beginInline(content && linked ? holder : null, image.decorative());
        if (!content) {
            return -1;
        }
        if (link != null) {
            holder = link;
        }
        if (image.decorative()) {
            return -1;
        }
        PDStructureElement figure = profile.element("Figure", holder);
        figure.setPage(parentTree.page());
        figure.setAlternateDescription(image.alt());
        PDLayoutAttributeObject layout = new PDLayoutAttributeObject();
        layout.setBBox(bbox);
        figure.addAttribute(layout);
        holder.appendKid(figure);
        return parentTree.mark(figure);
    }

    /**
     * Records the call of a footnote in the text of the box (reference/components/footnote.md) as the content of a
     * new {@code Reference}, child of the element of the box, that points with {@code Ref} to the note, which points
     * back to it; gives its {@code MCID}, or -1 if the text is not content.
     */
    int markFootnoteCall(Box box, String footnote) {
        PDStructureElement holder = holder(box);
        PDStructureElement note = elements.get(notes.get(footnote));
        if (untagged || holder == null || note == null || !isContent(box)) {
            return -1;
        }
        PDStructureElement reference = profile.element("Reference", holder);
        reference.setPage(parentTree.page());
        holder.appendKid(reference);
        refer(reference, note);
        refer(note, reference);
        return parentTree.mark(reference);
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
        if (untagged) {
            return -1;
        }
        for (Box b = box; b != null; b = b.parent()) {
            if (labels.containsKey(b)) {
                return parentTree.mark(labels.get(b));
            }
        }
        return -1;
    }

    /**
     * Whether the box is content on the current page: the header and the footer of a table are on the
     * first page they appear on, and not on the others, where they repeat.
     */
    private boolean isContent(Box box) {
        if (repeated.contains(box)) {
            PDPage first = firstPage.putIfAbsent(box, parentTree.page());
            return first == null || first == parentTree.page();
        }
        return true;
    }

    /**
     * Tags the area of a box with {@code Link} (reference/implementation/pdf.md): the annotation hangs from the
     * {@code Link} of the box ({@link PdfStructureLinks#link}).
     */
    void link(Box box, String destination, PDAnnotationLink annotation, String description) {
        if (untagged) {
            return;
        }
        links.link(box, elements.get(box), destination, annotation, description);
    }

    /**
     * Tags the area of a line of the text link started by {@link #markLink}, or of the image started by
     * {@link #markInlineImage} ({@link PdfStructureLinks#inlineLink}).
     */
    void inlineLink(Box box, String destination, PDAnnotationLink annotation, String description) {
        if (untagged) {
            return;
        }
        links.inlineLink(box, destination, annotation, description);
    }

    /** The boxes with an element that have a key ({@link Box#key()}), by key; built once, when the tree is complete. */
    private Map<String, Box> byKey() {
        Map<String, Box> byKey = new HashMap<>();
        for (Box box : elements.keySet()) {
            if (box.key() != null) {
                byKey.put(box.key(), box);
            }
        }
        return byKey;
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
     * With PDF/UA-2 ({@link UaProfile#structureDestinations}), the action that leads to the box: a structure
     * destination ({@code SD}, the element of the box with the position of {@code destination}) and, for viewers that
     * do not understand it, the destination of its page ({@code D}); with PDF/UA-1, {@code null}, and the page
     * destination is used.
     */
    PDActionGoTo goTo(Box box, PDPageXYZDestination destination) {
        if (!profile.structureDestinations()) {
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

    /**
     * The {@code BBox} of the {@code Figure} of an image: its content box on the page, in
     * PDF coordinates. An image is not broken, so it is only on one page. Inside a {@code Figure} element, the image
     * hangs from the {@code Figure} of the element, or from a {@code Link} inside it: the {@code BBox} goes to the
     * {@code Figure}.
     */
    void figureBox(Box box, PDRectangle r) {
        PDStructureElement e = elements.get(box);
        if (untagged || e == null) {
            return;
        }
        if (!"Figure".equals(e.getStructureType()) && e.getParent() instanceof PDStructureElement figure) {
            e = figure;
        }
        PDLayoutAttributeObject layout = new PDLayoutAttributeObject();
        layout.setBBox(r);
        e.addAttribute(layout);
    }

    /**
     * Finishes the tree once every page is painted: the {@code Link} elements that got no annotation go, each table
     * of contents entry points to its heading (PDF/UA-2), and the parent tree and the {@code IDTree} of the footnotes
     * are written. The bookmarks are written apart ({@link PdfOutline}).
     */
    void finish() {
        links.finish();
        // PDF/UA-2: each table of contents entry points with Ref to the element of its heading, or of the nearest
        // ancestor of the box with its key that has one.
        Map<String, Box> byKey = entries.isEmpty() ? Map.of() : byKey();
        entries.forEach((entry, toci) -> {
            Box box = byKey.get(Links.target(entry.style().link()));
            PDStructureElement heading = box == null ? null : element(box);
            if (heading != null) {
                COSArray ref = new COSArray();
                ref.add(heading);
                toci.getCOSObject().setItem(COSName.getPDFName("Ref"), ref);
            }
        });
        parentTree.write(treeRoot);
        if (!noteIds.isEmpty()) {
            // The identifiers of the footnotes (Note needs one in PDF/UA-1), in the tree that maps them.
            PDStructureElementNameTreeNode idTree = new PDStructureElementNameTreeNode();
            idTree.setNames(noteIds);
            treeRoot.setIDTree(idTree);
        }
    }
}
