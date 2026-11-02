package dev.stxt.pdf.output;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.fragment.Rule;
import dev.stxt.pdf.model.Rect;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Dumps the result of the layout to STXT (reference/implementation/testing.md). It is the format of the reference
 * tests: border box rectangles in absolute page coordinates, in pt.
 *
 * <pre>
 * Result:
 * 	Page: 1
 * 		Fragment: header
 * 			Rect: 40 40 515 60
 * </pre>
 *
 * <p>By default only the fragments with an {@code Id} are dumped; their descendants with an
 * {@code Id} are nested inside.</p>
 */
public final class StxtDump {

    private StxtDump() {
    }

    public static String dump(List<Page> pages, boolean includeWithoutId) {
        StringBuilder sb = new StringBuilder("Result:\n");
        for (Page p : pages) {
            // The coordinates are those of the page, with the page area of the medium of its section
            // (reference/components/document.md).
            Rect pageArea = p.pageSetup().pageArea();
            sb.append("\tPage: ").append(p.number()).append('\n');
            dumpChildren(sb, p.content(), pageArea.x(), pageArea.y(), 2, includeWithoutId);
            if (p.footnotes() != null) {
                // The footnote area has no Id: its notes, which always have one, come out at its level.
                dumpFragment(sb, p.footnotes(), pageArea.x(), pageArea.y(), 2, includeWithoutId);
            }
            for (Fragment overlay : p.pageOverlays()) {
                dumpFragment(sb, overlay, pageArea.x(), pageArea.y(), 2, includeWithoutId);
            }
        }
        return sb.toString();
    }

    private static void dumpChildren(StringBuilder sb, Fragment parent, double absX, double absY,
                                     int level, boolean includeWithoutId) {
        Rect content = parent.borderBox(absX, absY).shrink(parent.border()).shrink(parent.padding());
        for (Fragment h : parent.children()) {
            dumpFragment(sb, h, content.x(), content.y(), level, includeWithoutId);
        }
    }

    private static void dumpFragment(StringBuilder sb, Fragment f, double originX, double originY,
                                     int level, boolean includeWithoutId) {
        double x = originX + f.x();
        double y = originY + f.y();
        if (f.box().isMarker()) {
            // It goes inside the item: only if the item is dumped.
            if (f.box().parent().id() != null || includeWithoutId) {
                String tab = "\t".repeat(level);
                if (f.box().isImage()) {
                    sb.append(tab).append("Marker image: ").append(f.box().image()).append('\n');
                } else {
                    sb.append(tab).append("Marker: ").append(f.box().text().strip()).append('\n');
                }
                appendRect(sb, tab, x, y, f);
            }
            return;
        }
        boolean visible = f.box().id() != null || includeWithoutId;
        int childLevel = level;
        if (visible) {
            String tab = "\t".repeat(level);
            String name = f.box().id() != null ? f.box().id() : f.box().describe();
            sb.append(tab).append("Fragment: ").append(name).append('\n');
            appendRect(sb, tab, x, y, f);
            appendRules(sb, tab, x, y, f);
            childLevel = level + 1;
        }
        dumpChildren(sb, f, x, y, childLevel, includeWithoutId);
    }

    private static void appendRect(StringBuilder sb, String tab, double x, double y, Fragment f) {
        sb.append(tab).append("\tRect: ").append(format(x)).append(' ').append(format(y)).append(' ')
                .append(format(f.width())).append(' ').append(format(f.height())).append('\n');
    }

    /**
     * The lines of the fragment ({@link Fragment#rules}, the merged borders of a table), in the order they are painted:
     * {@code Rule: x y width height color}, in page coordinates like {@code Rect}.
     */
    private static void appendRules(StringBuilder sb, String tab, double x, double y, Fragment f) {
        Rect content = f.borderBox(x, y).shrink(f.border()).shrink(f.padding());
        for (Rule r : f.rules()) {
            sb.append(tab).append("\tRule: ").append(format(content.x() + r.x())).append(' ')
                    .append(format(content.y() + r.y())).append(' ').append(format(r.width())).append(' ')
                    .append(format(r.height())).append(' ').append(r.color()).append('\n');
        }
    }

    /** Up to 2 decimals, without trailing zeros. */
    static String format(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
