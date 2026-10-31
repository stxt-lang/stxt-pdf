package dev.stxt.pdf.tree;

import dev.stxt.Node;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Leader;
import dev.stxt.pdf.model.Links;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.Value;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.pdf.tree.Nodes.error;

/**
 * The automatic table of contents (reference/components/table-of-contents.md): reads {@code Levels} and {@code Leader}
 * of a {@code Table of contents} and, when the tree is built and all the headings are known, generates its entries.
 *
 * <p>Each entry is a generated {@code Stack} box ({@link Element#TOC_1} ... by the level of the heading), with three
 * children: the title (the text of the heading), the leader, which grows to fill the gap, and the page number, which
 * is a {@code {page of: ...}} of the heading: the same mechanism the author writes, so it is resolved by paginating
 * until it converges. Since a heading of a {@code Markdown} has no {@code Id}, it gets a {@link Box#anchor()} so that
 * it can be referenced. The entries are styled with the {@code For element: TOC N} rules.</p>
 */
final class TableOfContentsBuilder {

    /** How far the leader is from the title and from the number, in points. */
    private static final double LEADER_GAP = 4;

    private TableOfContentsBuilder() {
    }

    /** {@code Levels}: the first and the last level, from 1 to 6. */
    static void levels(Node n, Style s) {
        String[] parts = n.getText().trim().split("\\s+");
        int[] levels = new int[2];
        try {
            if (parts.length != 2) {
                throw new NumberFormatException();
            }
            for (int i = 0; i < 2; i++) {
                levels[i] = Integer.parseInt(parts[i]);
            }
        } catch (NumberFormatException e) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Levels needs the first and the last heading "
                    + "level, as in Levels: 1 3: '" + n.getText().trim() + "'");
        }
        if (levels[0] < 1 || levels[1] > 6 || levels[0] > levels[1]) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "Levels goes from one level to an equal or higher one, between 1 and 6: '"
                    + n.getText().trim() + "'");
        }
        s.tableOfContents().setFrom(levels[0]);
        s.tableOfContents().setTo(levels[1]);
    }

    /** Generates the entries of every {@code Table of contents} in the document. */
    static void build(BuildContext ctx, Box root) {
        if (ctx.contents.isEmpty()) {
            return;
        }
        List<Box> headings = new ArrayList<>();
        collect(root, headings);
        int n = 0;
        for (Box heading : headings) {
            n++;
            if (heading.id() == null) {
                heading.setAnchor("(heading " + n + ")");
            }
        }
        for (Box contents : ctx.contents) {
            fill(ctx, contents, headings);
        }
    }

    /** Headings of the tree with an entry in tables of contents ({@code In table of contents}), in document order. */
    private static void collect(Box box, List<Box> headings) {
        if (box.headingLevel() > 0 && box.style().inTableOfContents()) {
            headings.add(box);
        }
        box.children().forEach(child -> collect(child, headings));
    }

    private static void fill(BuildContext ctx, Box contents, List<Box> headings) {
        Style s = contents.style();
        if (s.typography().font() == null) {
            throw error(contents.line(), ErrorCode.TEXT_WITHOUT_FONT, "The table of contents needs Font: it is declared in the "
                    + "Typography of the Table of contents, of an ancestor or of Styles (For element: Document)");
        }
        if (!s.isDeclared(PropertyKey.FONT_SIZE)) {
            throw error(contents.line(), ErrorCode.TEXT_WITHOUT_FONT_SIZE, "The table of contents needs Size: it is declared in the "
                    + "Typography of the Table of contents, of an ancestor or of Styles (For element: Document)");
        }
        List<Box> listed = headings.stream()
                .filter(h -> h.headingLevel() >= s.tableOfContents().from() && h.headingLevel() <= s.tableOfContents().to()).toList();
        if (listed.isEmpty()) {
            throw error(contents.line(), ErrorCode.TABLE_OF_CONTENTS_EMPTY, "The table of contents has no heading of levels "
                    + s.tableOfContents().from() + " to " + s.tableOfContents().to() + " (Levels): it would have no entries");
        }
        for (Box heading : listed) {
            contents.addChild(entry(ctx, contents, heading));
        }
    }

    private static Box entry(BuildContext ctx, Box contents, Box heading) {
        Style es = Style.inheriting(contents.style());
        es.setLayout(Layout.STACK);
        ctx.rules.apply(Element.tocEntry(heading.headingLevel()), es);
        ModelValidator.validateBox(ctx, es, contents);
        Box entry = new Box(null, Element.tocEntry(heading.headingLevel()), es, false, heading.line());
        ctx.definiteHeight.put(entry, false);
        // The entry links to its heading (reference/common/links.md): all of it is clickable.
        es.setLink(Links.to(heading.key()));

        boolean leader = contents.style().tableOfContents().leader() != Leader.NONE;
        int line = heading.children().isEmpty() ? heading.line() : heading.children().get(0).textLine();

        // Without a leader, the title fills the gap and leaves the number on the right.
        Box title = generated(ctx, entry, Element.TOC_TITLE, Layout.INLINE, heading.line());
        title.setText(heading.headingText(), line, false);
        title.style().item().setGrow(leader ? 0 : 1);
        entry.addChild(title);

        if (leader) {
            Box guide = generated(ctx, entry, Element.TOC_LEADER, Layout.BLOCK, heading.line());
            guide.style().tableOfContents().setLeader(contents.style().tableOfContents().leader());
            guide.style().item().setGrow(1);
            guide.style().box().setHeight(Value.pt(es.typography().usedLineHeight()));
            guide.style().box().setMargin(new Sides<>(Value.pt(0), Value.pt(LEADER_GAP), Value.pt(0), Value.pt(LEADER_GAP)));
            entry.addChild(guide);
        }

        Box page = generated(ctx, entry, Element.TOC_PAGE, Layout.INLINE, heading.line());
        page.setText("{page of: " + heading.key() + "}", line, false);
        page.style().item().setShrink(0);
        entry.addChild(page);
        return entry;
    }

    /** A generated box of an entry: it inherits the typography of the entry, and its kind gives the layout. */
    private static Box generated(BuildContext ctx, Box entry, Element kind, Layout layout, int line) {
        Style s = Style.inheriting(entry.style());
        s.setLayout(layout);
        Box box = new Box(null, kind, s, false, line);
        ctx.definiteHeight.put(box, false);
        return box;
    }
}
