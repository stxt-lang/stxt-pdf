package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.model.Reference;
import dev.stxt.pdf.tree.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * The last step of the literal references (reference/common/references.md). A reference or a page number written as
 * text ({@code {{page}}}, or <code>\{page}</code> in Markdown) travels through the layout with
 * {@link Reference#LITERAL_BRACE} as its brace, so that nothing replaces it: neither the references, nor the page
 * numbers, nor a running head that copies the text of a heading. It is measured as a brace
 * ({@link PdfMetrics#width}), and once the document is paginated and nothing is replaced any more, the lines get the
 * brace: what is painted, and what whoever reads the laid out pages finds, is the text.
 */
public final class LiteralBraces {

    private LiteralBraces() {
    }

    /**
     * The pages with the literal braces of their lines as braces; the same list if no text of the tree has one. The
     * positions and the widths do not change.
     */
    public static List<Page> shown(Box root, List<Page> pages) {
        if (!any(root)) {
            return pages;
        }
        List<Page> shown = new ArrayList<>(pages.size());
        for (Page p : pages) {
            Fragment content = shown(p.content());
            List<Fragment> overlays = p.pageOverlays().stream().map(LiteralBraces::shown).toList();
            Fragment footnotes = p.footnotes() == null ? null : shown(p.footnotes());
            shown.add(new Page(p.number(), content, overlays, p.pageSetup(), p.section(), p.printed(), p.style(),
                    p.blank(), footnotes));
        }
        return shown;
    }

    /** Whether some text of the tree has a literal brace. */
    private static boolean any(Box box) {
        if (has(box.text()) || (box.marker() != null && any(box.marker()))) {
            return true;
        }
        for (Box child : box.children()) {
            if (any(child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean has(String text) {
        return text != null && text.indexOf(Reference.LITERAL_BRACE) >= 0;
    }

    /** The fragment and its descendants with the braces in their lines; the same fragment if nothing changes. */
    static Fragment shown(Fragment f) {
        List<Fragment> children = new ArrayList<>(f.children().size());
        boolean changed = false;
        for (Fragment child : f.children()) {
            Fragment c = shown(child);
            changed |= c != child;
            children.add(c);
        }
        List<TextLine> lines = new ArrayList<>(f.lines().size());
        for (TextLine l : f.lines()) {
            if (!has(l.text())) {
                lines.add(l);
                continue;
            }
            changed = true;
            List<TextLine.Run> runs = l.runs().stream()
                    .map(r -> new TextLine.Run(r.x(), r.width(), Reference.shown(r.text()), r.format())).toList();
            lines.add(new TextLine(l.x(), l.y(), l.width(), l.height(), l.fontSize(), Reference.shown(l.text()),
                    l.wordSpacing(), runs, l.hyphenated(), l.left(), l.right(), l.splitsWord()));
        }
        if (!changed) {
            return f;
        }
        return new Fragment(f.box(), f.x(), f.y(), f.width(), f.height(), f.margin(), f.border(), f.padding(),
                children, lines, f.first(), f.last(), f.offsetX(), f.offsetY(), f.turned(), f.rules());
    }
}
