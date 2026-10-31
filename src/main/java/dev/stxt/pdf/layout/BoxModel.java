package dev.stxt.pdf.layout;

import dev.stxt.pdf.layout.image.NaturalSize;
import dev.stxt.pdf.model.Edges;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Style;

import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Used box model of a box in a given space (reference/common/box.md): margins, border and padding in points, the
 * width of the border box and its height if it is definite. It is the part common to all layout algorithms.
 *
 * <p>Percentages are resolved against the bases of the {@link ConstraintSpace}. An {@code auto} in the margins is
 * 0 here: the parent does the centering, since it places the box.</p>
 *
 * @param margin    used margins ({@code auto} = 0)
 * @param border    border widths
 * @param padding   padding
 * @param width     width of the border box, already limited by minimum and maximum
 * @param height    height of the border box if it is definite (a number, a percentage of a definite
 *                  height, imposed by the parent or deduced from {@code Aspect ratio}); empty
 *                  if it depends on the content
 * @param minHeight minimum height of the border box
 * @param maxHeight maximum height of the border box; empty = no maximum
 * @param decoration border and padding at the breaks ({@link Slice})
 */
public record BoxModel(Insets margin, Insets border, Insets padding, double width,
                       OptionalDouble height, double minHeight, OptionalDouble maxHeight,
                       Edges decoration) {

    /**
     * Resolves the box model of a box with the engine's intrinsic sizes and, if it is an
     * {@code Image}, its natural size.
     */
    public static BoxModel resolve(Box box, ConstraintSpace space, LayoutContext context) {
        return resolve(box, space, () -> context.intrinsic(box), box.isImage() ? context.natural(box) : null);
    }

    /**
     * Resolves the box model of a box that is not an {@code Image}.
     *
     * @param intrinsic intrinsic sizes of the box, for {@code Width: narrowest} and
     *                  similar; they are only computed if needed
     */
    public static BoxModel resolve(Box box, ConstraintSpace space, Supplier<IntrinsicSizes> intrinsic) {
        return resolve(box, space, intrinsic, null);
    }

    /**
     * Resolves the box model of a box.
     *
     * @param intrinsic intrinsic sizes of the box, for {@code Width: narrowest} and
     *                  similar; they are only computed if needed
     * @param natural   natural size of an {@code Image}, which is sized as a replaced
     *                  element ({@link #replaced}); {@code null} for the other boxes
     */
    public static BoxModel resolve(Box box, ConstraintSpace space, Supplier<IntrinsicSizes> intrinsic,
                                   NaturalSize natural) {
        Style s = box.style();
        double baseWidth = space.percentBaseWidth();
        OptionalDouble baseHeight = space.percentBaseHeight();
        Insets margin = Insets.resolve(s.box().margin(), baseWidth);
        Insets border = Insets.resolve(s.box().border(), baseWidth);
        Insets padding = Insets.resolve(s.box().padding(), baseWidth);

        double minHeight = s.box().minHeight().resolve(baseHeight).orElse(0);
        OptionalDouble maxHeight = s.box().maxHeight() == null
                ? OptionalDouble.empty() : s.box().maxHeight().resolve(baseHeight);
        if (natural != null) {
            return replaced(s, space, intrinsic, natural, margin, border, padding, minHeight, maxHeight);
        }

        double width;
        if (space.fixedWidth()) {
            width = space.availableWidth();
        } else {
            width = constrainWidth(s, specifiedWidth(s, space, margin, intrinsic), baseWidth);
        }

        OptionalDouble height;
        if (space.fixedHeight()) {
            height = space.availableHeight();
        } else if (space.contentHeight()) {
            height = OptionalDouble.empty();
        } else {
            height = s.box().height().resolve(baseHeight);
            if (height.isEmpty() && s.box().aspectRatio() != null) {
                // Simplification: the ratio applies to the border box.
                height = OptionalDouble.of(width / s.box().aspectRatio());
            }
            if (height.isPresent()) {
                height = OptionalDouble.of(Sizing.constrain(height.getAsDouble(), minHeight, maxHeight));
            }
        }
        return new BoxModel(margin, border, padding, width, height, minHeight, maxHeight, s.breaks().edges());
    }

    /**
     * An {@code Image} (reference/components/image.md): its content box has the ratio
     * of the file (or that of {@code Aspect ratio}, which replaces it). With {@code Width: auto} it measures its
     * natural width, also in block flow, or the one that follows from a declared height; the {@code auto} height
     * always follows from the width, so it is definite. If both are {@code auto} and the height hits its minimum or
     * maximum, the width is deduced again from the limited height. With {@code measuringContentHeight}, the height
     * is also that of the ratio.
     */
    private static BoxModel replaced(Style s, ConstraintSpace space, Supplier<IntrinsicSizes> intrinsic,
                                     NaturalSize natural, Insets margin, Insets border, Insets padding,
                                     double minHeight, OptionalDouble maxHeight) {
        double ratio = s.box().aspectRatio() != null ? s.box().aspectRatio() : natural.ratio();
        double extraWidth = border.horizontal() + padding.horizontal();
        double extraHeight = border.vertical() + padding.vertical();
        OptionalDouble declaredHeight;
        if (space.fixedHeight()) {
            declaredHeight = space.availableHeight();
        } else if (space.contentHeight()) {
            declaredHeight = OptionalDouble.empty();
        } else {
            declaredHeight = s.box().height().resolve(space.percentBaseHeight());
            if (declaredHeight.isPresent()) {
                declaredHeight = OptionalDouble.of(Sizing.constrain(declaredHeight.getAsDouble(), minHeight, maxHeight));
            }
        }

        double baseWidth = space.percentBaseWidth();
        boolean autoWidth = !space.fixedWidth() && s.box().width().isAuto();
        double width;
        if (space.fixedWidth()) {
            width = space.availableWidth();
        } else if (autoWidth) {
            double content = declaredHeight.isPresent()
                    ? Math.max(0, declaredHeight.getAsDouble() - extraHeight) * ratio : natural.width();
            width = constrainWidth(s, content + extraWidth, baseWidth);
        } else {
            width = constrainWidth(s, specifiedWidth(s, space, margin, intrinsic), baseWidth);
        }

        double height;
        if (declaredHeight.isPresent()) {
            height = declaredHeight.getAsDouble();
        } else {
            double proportional = Math.max(0, width - extraWidth) / ratio + extraHeight;
            height = Sizing.constrain(proportional, minHeight, maxHeight);
            if (autoWidth && height != proportional) {
                width = constrainWidth(s, Math.max(0, height - extraHeight) * ratio + extraWidth, baseWidth);
            }
        }
        return new BoxModel(margin, border, padding, width, OptionalDouble.of(height), minHeight, maxHeight,
                s.breaks().edges());
    }

    /**
     * Limits a border box width by {@code Min width} and {@code Max width} (reference/common/box.md).
     *
     * @param baseWidth base of the percentages
     */
    public static double constrainWidth(Style s, double width, double baseWidth) {
        return Sizing.constrain(width,
                s.box().minWidth().resolve(baseWidth).orElse(0),
                s.box().maxWidth() == null ? OptionalDouble.empty() : s.box().maxWidth().resolve(baseWidth));
    }

    /** Border box width before minimum and maximum. */
    private static double specifiedWidth(Style s, ConstraintSpace space, Insets margin,
                                         Supplier<IntrinsicSizes> intrinsic) {
        OptionalDouble declared = s.box().width().resolve(space.percentBaseWidth());
        if (declared.isPresent()) {
            return declared.getAsDouble();
        }
        if (s.box().width() instanceof Value.Keyword k && k != Value.Keyword.AUTO) {
            // The intrinsic sizes are those of the margin box.
            IntrinsicSizes i = intrinsic.get();
            double marginBox = switch (k) {
                case NARROWEST -> i.narrowest();
                case NATURAL -> i.natural();
                default -> i.fit(space.availableWidth());
            };
            return Math.max(0, marginBox - margin.horizontal());
        }
        // auto, or a value that depends on an indefinite base.
        return Sizing.autoWidthInFlow(space.availableWidth(), margin);
    }

    /**
     * Intrinsic sizes of the margin box from those of its content (reference/common/box.md). A box with a fixed
     * {@code Width} has that value as narrowest width and natural width. Percentages cannot be resolved without a
     * reference container and count as 0 (or as {@code auto}).
     */
    public static IntrinsicSizes intrinsic(Box box, IntrinsicSizes content) {
        Style s = box.style();
        Insets border = Insets.resolve(s.box().border(), 0);
        Insets padding = Insets.resolve(s.box().padding(), 0);
        double extra = border.horizontal() + padding.horizontal();
        OptionalDouble fixed = s.box().width().resolve(OptionalDouble.empty());
        IntrinsicSizes r = fixed.isPresent()
                ? new IntrinsicSizes(fixed.getAsDouble(), fixed.getAsDouble())
                : content.plus(extra);
        double min = s.box().minWidth().resolve(OptionalDouble.empty()).orElse(0);
        OptionalDouble max = s.box().maxWidth() == null
                ? OptionalDouble.empty() : s.box().maxWidth().resolve(OptionalDouble.empty());
        r = new IntrinsicSizes(Sizing.constrain(r.narrowest(), min, max), Sizing.constrain(r.natural(), min, max));
        return r.plus(Insets.resolve(s.box().margin(), 0).horizontal());
    }

    public double contentWidth() {
        return Math.max(0, width - border.horizontal() - padding.horizontal());
    }

    /** Height of the content box if the height is definite. */
    public OptionalDouble contentHeight() {
        return height.isPresent()
                ? OptionalDouble.of(Math.max(0, height.getAsDouble() - border.vertical() - padding.vertical()))
                : OptionalDouble.empty();
    }

    /**
     * Final height of the border box: the definite one or, with {@code auto}, that of the content, which is never
     * negative (negative margins can take more than the content measures, reference/components/block.md).
     */
    public double usedHeight(double contentHeight) {
        if (height.isPresent()) {
            return height.getAsDouble();
        }
        return Sizing.constrain(Math.max(0, contentHeight) + border.vertical() + padding.vertical(), minHeight,
                maxHeight);
    }

    /** Each fragment carries full border and padding ({@code Edges: closed}). */
    public boolean clones() {
        return decoration == Edges.CLOSED;
    }

    /** Nothing separates the top border of the box from its content: neither border nor padding. */
    public boolean openAtTop() {
        return border.top() == 0 && padding.top() == 0;
    }

    /** Likewise at the bottom, and in addition the height depends on the content. */
    public boolean openAtBottom() {
        return border.bottom() == 0 && padding.bottom() == 0 && height.isEmpty();
    }

    /**
     * Space for the in-flow children: the content box is their reference container. What is left of the page is
     * measured from the start of the content box (and, with {@code Edges: closed}, without the bottom border and
     * padding).
     *
     * @param first it is the first fragment of the box (with {@code open}, the following ones carry no
     *              top border or padding, see {@link Slice})
     */
    public ConstraintSpace childSpace(ConstraintSpace space, boolean first) {
        double w = contentWidth();
        OptionalDouble h = contentHeight();
        OptionalDouble remaining = space.fragmenting()
                ? OptionalDouble.of(Slice.contentRemaining(this, first, space))
                : OptionalDouble.empty();
        return ConstraintSpace.forChildren(w, h, remaining, space.atFragmentainerStart());
    }
}
