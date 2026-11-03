package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.model.Cancellation;
import dev.stxt.pdf.model.Fit;
import dev.stxt.pdf.model.Modules;
import dev.stxt.pdf.model.Tolerance;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;

import java.util.List;
import java.util.Locale;

import static dev.stxt.pdf.layout.Messages.pt;

/**
 * Checks, once paginated, that the modules of each generated image, such as a {@code Barcode}
 * (reference/components/barcode.md), are not narrower than its minimum ({@link Modules#minModule()}): a code shrunk
 * until it cannot be read is an error with the line of its box ({@code BARCODE_TOO_SMALL}), not a half result. It looks
 * at the final size of the content box, not at the measuring passes. A drawing stretched in height does not change its
 * modules. The caller can remove it ({@link dev.stxt.pdf.model.Check#BARCODE_SIZE}).
 */
public final class ModuleSizes {

    private ModuleSizes() {
    }

    /**
     * The flow, the footnote area and the {@code Page overlay} boxes of each page, stopping once {@code cancellation}
     * asks for it, which is asked at each page.
     *
     * @throws InvalidModelException with the first drawing whose modules are too narrow
     */
    public static void check(List<Page> pages, Cancellation cancellation) {
        for (Page p : pages) {
            cancellation.check();
            fragment(p.content());
            if (p.footnotes() != null) {
                fragment(p.footnotes());
            }
            p.pageOverlays().forEach(ModuleSizes::fragment);
        }
    }

    private static void fragment(Fragment f) {
        Modules modules = f.box().modules();
        if (modules != null) {
            check(f, modules);
        }
        f.children().forEach(ModuleSizes::fragment);
    }

    private static void check(Fragment f, Modules modules) {
        double[] minimum = modules.minimumSize();
        boolean stretched = modules.fit() == Fit.STRETCH;
        boolean narrow = f.contentWidth() < minimum[0] - Tolerance.MODEL;
        boolean low = !stretched && f.contentHeight() < minimum[1] - Tolerance.MODEL;
        if (!narrow && !low) {
            return;
        }
        double used = modules.usedModule(f.contentWidth(), f.contentHeight());
        String box = stretched ? width(f.contentWidth()) : size(f.contentWidth(), f.contentHeight());
        String needed = stretched ? width(minimum[0]) : size(minimum[0], minimum[1]);
        throw new InvalidModelException(f.box().line(), ErrorCode.BARCODE_TOO_SMALL, "The modules of Barcode "
                + f.box().describe() + " measure " + number(used / Value.POINTS_PER_MM, 3)
                + " mm and the narrowest that can be read is " + number(modules.minModule() / Value.POINTS_PER_MM, 3)
                + " mm: its content box measures " + box + " and needs at least " + needed
                + " (or the check BARCODE_SIZE is removed when converting)");
    }

    /** A width in millimeters and in points: {@code 50 mm wide (141.73 pt)}. */
    private static String width(double points) {
        return number(points / Value.POINTS_PER_MM, 2) + " mm wide (" + pt(points) + ")";
    }

    /** A size in millimeters and in points: {@code 8.25 × 8.25 mm (23.39 × 23.39 pt)}. */
    private static String size(double width, double height) {
        return number(width / Value.POINTS_PER_MM, 2) + " × " + number(height / Value.POINTS_PER_MM, 2) + " mm ("
                + number(width, 2) + " × " + pt(height) + ")";
    }

    /** A number with at most {@code decimals} decimals and no trailing zeros. */
    private static String number(double v, int decimals) {
        String s = String.format(Locale.ROOT, "%." + decimals + "f", v);
        return s.contains(".") ? s.replaceAll("\\.?0+$", "") : s;
    }
}
