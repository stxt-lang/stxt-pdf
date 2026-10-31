package dev.stxt.pdf.layout;

import dev.stxt.pdf.model.Insets;

import static dev.stxt.pdf.model.Tolerance.EPSILON;
import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Part of a box that falls in a fragment (reference/common/breaks.md): its height, its border and its
 * padding. It is the part of fragmentation common to all algorithms.
 *
 * <ul>
 *   <li>With {@code Edges: open}, the first fragment has no bottom border or padding
 *       and the following ones have no top ones.</li>
 *   <li>With {@code Edges: closed}, each fragment has full border and padding: the
 *       content of each page fits between them.</li>
 *   <li>A definite {@code Height} is the total of all fragments: if the content ends
 *       and the box's own height does not fit on the page, what is missing goes to the next
 *       fragment. A fragment broken inside the content reaches the end of the page,
 *       without exceeding the height left to the box, but it only consumes what its content takes up: the
 *       gap at the end of the page is not taken away from the content that follows. With {@code closed}, the
 *       total is that of the content: the height minus one border and one padding.</li>
 *   <li>With {@code auto} height, the fragment measures what its content does; {@code Min height} and
 *       {@code Max height} apply to the total.</li>
 * </ul>
 *
 * @param height   height of the border box in this fragment
 * @param border   borders of this fragment
 * @param padding  padding of this fragment
 * @param finished the box ends in this fragment
 * @param consumed border box height of this fragment and the previous ones; with
 *                 {@code closed}, height of its content box
 */
public record Slice(double height, Insets border, Insets padding, boolean finished, double consumed) {

    /** Top border and padding of the fragment: with {@code open}, only the first one has them. */
    public static double top(BoxModel model, boolean first) {
        return first || model.clones() ? model.border().top() + model.padding().top() : 0;
    }

    /**
     * What is left of the page for the content of the fragment: without the top border and
     * padding and, with {@code closed}, without the bottom ones either. With {@code open}, the bottom ones
     * are not reserved here: they are only needed if the box ends on this page, and
     * {@link Engine#layout} takes care of that.
     */
    public static double contentRemaining(BoxModel model, boolean first, ConstraintSpace space) {
        double bottom = model.clones() ? model.border().bottom() + model.padding().bottom() : 0;
        return space.fragmentainerRemaining().getAsDouble() - top(model, first) - bottom;
    }

    /**
     * Height, border and padding of a fragment.
     *
     * @param first           it is the first fragment of the box
     * @param consumed        height consumed by the previous fragments (see {@link #consumed()})
     * @param content         height of the content placed in this fragment; a negative one (negative margins) counts
     *                        as 0
     * @param contentFinished the content ends in this fragment
     * @param space           space of the box; without fragmentation there is no limit
     */
    public static Slice of(BoxModel model, boolean first, double consumed, double content,
                           boolean contentFinished, ConstraintSpace space) {
        content = Math.max(0, content);
        if (!space.fragmenting()) {
            return new Slice(model.usedHeight(content), model.border(), model.padding(), true, 0);
        }
        if (model.clones()) {
            return cloned(model, consumed, content, contentFinished, space);
        }
        double remaining = space.fragmentainerRemaining().getAsDouble();
        double top = top(model, first);
        double upToContent = top + content;
        if (contentFinished) {
            double natural = upToContent + model.border().bottom() + model.padding().bottom();
            double total = model.height().isPresent()
                    ? model.height().getAsDouble()
                    : Sizing.constrain(consumed + natural, model.minHeight(), model.maxHeight());
            double need = Math.max(0, total - consumed);
            if (!fits(need, remaining) && !fits(need, natural)) {
                // The content fits, but the box's own height does not: the rest goes to the next page.
                double h = Math.max(remaining, upToContent);
                if (h < need - EPSILON) {
                    return open(model, first, h, consumed);
                }
            }
            return new Slice(need, sides(model.border(), first, true), sides(model.padding(), first, true),
                    true, consumed + need);
        }
        double h = upToContent;
        if (model.height().isPresent()) {
            h = Math.max(h, Math.min(remaining, model.height().getAsDouble() - consumed));
        }
        // It reaches the end of the page, but only consumes what its content takes up: the gap that the lines or
        // the children leave at the end of the page is not taken away from the content that follows.
        return new Slice(h, sides(model.border(), first, false), sides(model.padding(), first, false), false,
                consumed + upToContent);
    }

    /**
     * {@code Edges: closed}: the distribution is done with the height of the content box, and each
     * fragment adds the full border and padding to it.
     */
    private static Slice cloned(BoxModel model, double consumed, double content, boolean contentFinished,
                                ConstraintSpace space) {
        double decoration = model.border().vertical() + model.padding().vertical();
        double available = space.fragmentainerRemaining().getAsDouble() - decoration;
        double h = content;
        boolean finished = contentFinished;
        if (contentFinished) {
            double total = Math.max(0, model.usedHeight(consumed + content) - decoration);
            h = Math.max(0, total - consumed);
            if (!fits(h, available) && !fits(h, content)) {
                // The content fits, but the box's own height does not: the rest goes to the next page.
                double room = Math.max(available, content);
                if (room < h - EPSILON) {
                    h = room;
                    finished = false;
                }
            }
        } else if (model.height().isPresent()) {
            double total = Math.max(0, model.height().getAsDouble() - decoration);
            // As with open: up to the end of the page, consuming only the content.
            h = Math.max(h, Math.min(available, total - consumed));
            return new Slice(decoration + h, model.border(), model.padding(), false, consumed + content);
        }
        return new Slice(decoration + h, model.border(), model.padding(), finished, consumed + h);
    }

    /** Fragment that is not the last one: without bottom border or padding. */
    private static Slice open(BoxModel model, boolean first, double height, double consumed) {
        return new Slice(height, sides(model.border(), first, false), sides(model.padding(), first, false),
                false, consumed + height);
    }

    private static Insets sides(Insets all, boolean top, boolean bottom) {
        return new Insets(top ? all.top() : 0, all.right(), bottom ? all.bottom() : 0, all.left());
    }

    /** Height of the content box of this fragment. */
    public double contentHeight() {
        return Math.max(0, height - border.vertical() - padding.vertical());
    }
}
