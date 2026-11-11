package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
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
 *
 * <p>It also checks the readable text of a barcode ({@code Readable text}), which no check removes: each piece fits
 * under its modules, inside the content box and without running into the next one, and the text leaves some height
 * for the bars ({@code BARCODE_TEXT_OVERFLOW}).</p>
 */
public final class ModuleSizes {

    private ModuleSizes() {
    }

    /**
     * The flow, the footnote area and the {@code Page overlay} boxes of each page, stopping once {@code cancellation}
     * asks for it, which is asked at each page.
     *
     * @param modules whether the size of the modules is checked ({@link dev.stxt.pdf.model.Check#BARCODE_SIZE})
     * @throws InvalidModelException with the first drawing whose modules are too narrow or whose text does not fit
     */
    public static void check(List<Page> pages, Cancellation cancellation, boolean modules) {
        for (Page p : pages) {
            cancellation.check();
            fragment(p.content(), modules);
            if (p.footnotes() != null) {
                fragment(p.footnotes(), modules);
            }
            p.pageOverlays().forEach(f -> fragment(f, modules));
        }
    }

    /** The size of the modules and the readable texts of {@code pages}. */
    public static void check(List<Page> pages, Cancellation cancellation) {
        check(pages, cancellation, true);
    }

    private static void fragment(Fragment f, boolean size) {
        Modules modules = f.box().modules();
        if (modules != null) {
            if (size) {
                check(f, modules);
            }
            if (modules.text() != null) {
                text(f);
            }
        }
        f.children().forEach(child -> fragment(child, size));
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

    /** The readable text of a barcode, already placed in the line of its fragment. */
    private static void text(Fragment f) {
        for (TextLine line : f.lines()) {
            if (line.y() <= Tolerance.MODEL) {
                throw new InvalidModelException(f.box().line(), ErrorCode.BARCODE_TEXT_OVERFLOW, "The readable text "
                        + "of Barcode " + f.box().describe() + " is " + pt(line.height()) + " tall and its content "
                        + "box, " + pt(f.contentHeight()) + ": it leaves no height for the bars. The box has to be "
                        + "taller or the Size of the text smaller");
            }
            double end = 0;
            for (int i = 0; i < line.runs().size(); i++) {
                TextLine.Run run = line.runs().get(i);
                String what = line.runs().size() == 1 ? "'" + run.text() + "'"
                        : "'" + run.text() + "', the piece " + (i + 1) + " of '" + line.text() + "',";
                if (run.x() < -Tolerance.MODEL || run.x() + run.width() > f.contentWidth() + Tolerance.MODEL) {
                    throw new InvalidModelException(f.box().line(), ErrorCode.BARCODE_TEXT_OVERFLOW, "The readable "
                            + "text " + what + " of Barcode " + f.box().describe() + " is " + pt(run.width())
                            + " wide and does not fit in its content box, of " + pt(f.contentWidth())
                            + (run.x() < 0 && i == 0 && line.runs().size() > 1 ? " (the first digit of an EAN-13 "
                            + "goes in its left quiet zone, which needs 7 modules for it)" : "")
                            + ": the box has to be wider or the Size of the text smaller");
                }
                if (i > 0 && run.x() < end - Tolerance.MODEL) {
                    throw new InvalidModelException(f.box().line(), ErrorCode.BARCODE_TEXT_OVERFLOW, "The readable "
                            + "text " + what + " of Barcode " + f.box().describe() + " is " + pt(run.width())
                            + " wide and runs into the piece before it: the box has to be wider or the Size of the "
                            + "text smaller");
                }
                end = run.x() + run.width();
            }
        }
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
