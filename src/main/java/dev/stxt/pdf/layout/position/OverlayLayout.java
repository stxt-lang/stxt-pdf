package dev.stxt.pdf.layout.position;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.layout.AdjoiningMargins;
import dev.stxt.pdf.layout.BoxModel;
import dev.stxt.pdf.layout.ConstraintSpace;
import dev.stxt.pdf.layout.IntrinsicSizes;
import dev.stxt.pdf.layout.LayoutContext;
import dev.stxt.pdf.layout.LayoutResult;
import dev.stxt.pdf.layout.PendingOverlay;
import dev.stxt.pdf.layout.Sizing;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Layout of the {@code Overlay} and {@code Page overlay} boxes (reference/components/frame-and-overlays.md). They are
 * resolved when their reference container already knows its size, after the in-flow layout:
 *
 * <ul>
 *   <li>The {@code Overlay} ones go up as {@link PendingOverlay} to their reference container, and
 *       {@link LayoutContext#layout} resolves them with {@link #resolvePending} when the layout of
 *       that box finishes. Their fragments are added at the end of the children of the fragment of the reference
 *       container, not of their parent in the box tree.</li>
 *   <li>The {@code Page overlay} ones go up to the root and the
 *       {@link dev.stxt.pdf.layout.Paginator} resolves them on each page, with {@link #layOut}.</li>
 * </ul>
 *
 * <p>On each axis the equation of {@link OverlayEquation} is solved. The automatic width is the
 * shrink-to-fit of the border box, limited afterwards by {@code Min width} and
 * {@code Max width}; if the limit changes it, it is solved again with that width. The automatic
 * height is that of the content, unless top and bottom are declared: then the box
 * stretches between them. An out-of-flow box is not fragmented.</p>
 */
public final class OverlayLayout {

    private final LayoutContext context;

    public OverlayLayout(LayoutContext context) {
        this.context = context;
    }

    /**
     * Resolves the pending {@code Overlay} boxes whose reference container is {@code box} and adds
     * their fragments to that of {@code box}. The others keep going up.
     *
     * @param r result of the in-flow layout of {@code box}
     */
    public LayoutResult resolvePending(Box box, LayoutResult r) {
        if (r.pending().stream().noneMatch(p -> resolvesHere(p, box))) {
            return r;
        }
        Fragment f = r.fragment();
        // The reference container is the padding box.
        double containerWidth = f.width() - f.border().horizontal();
        double containerHeight = f.height() - f.border().vertical();
        List<Fragment> placed = new ArrayList<>();
        List<PendingOverlay> rest = new ArrayList<>();
        for (PendingOverlay p : r.pending()) {
            if (resolvesHere(p, box)) {
                LayoutResult a = layOut(p, containerWidth, containerHeight, f.padding());
                placed.add(a.fragment());
                rest.addAll(a.pending());
            } else {
                rest.add(p);
            }
        }
        return new LayoutResult(f.withMoreChildren(placed), r.breakToken(), r.breakAppeal(), rest, r.adjoining());
    }

    private static boolean resolvesHere(PendingOverlay p, Box box) {
        return p.box().style().position() == Position.OVERLAY && p.box().referenceContainer() == box;
    }

    /**
     * Lays out an out-of-flow box in its reference container.
     *
     * @param p                the box, with the static position relative to the content box of the
     *                         reference container
     * @param containerWidth   width of the padding box of the reference container
     * @param containerHeight  height of the padding box of the reference container
     * @param containerPadding padding of the reference container
     * @return the fragment placed relative to the content box of the reference container and the
     *         {@code Page overlay} boxes inside it, with the static position relative to that same box
     */
    public LayoutResult layOut(PendingOverlay p, double containerWidth, double containerHeight,
                               Insets containerPadding) {
        Box box = p.box();
        Style s = box.style();
        OptionalDouble containerHeightBase = OptionalDouble.of(containerHeight);
        Supplier<IntrinsicSizes> intrinsic = () -> context.intrinsic(box);
        // The equation works in the padding box of the reference container.
        double staticX = p.staticX() + containerPadding.left();
        double staticY = p.staticY() + containerPadding.top();

        // Margins, border, padding and declared width; the percentages are of the reference container.
        ConstraintSpace probe = ConstraintSpace.free(containerWidth, containerWidth, containerHeightBase, false);
        BoxModel model = BoxModel.resolve(box, probe, intrinsic);
        Insets margin = model.margin();

        OptionalDouble left = s.offset().left().resolve(containerWidth);
        OptionalDouble right = s.offset().right().resolve(containerWidth);
        OptionalDouble width = s.box().width().isAuto() ? OptionalDouble.empty() : OptionalDouble.of(model.width());
        OptionalDouble marginLeft = term(s.box().margin().left(), margin.left());
        OptionalDouble marginRight = term(s.box().margin().right(), margin.right());
        // Intrinsic sizes of the border box: those of the margin box without its margins.
        IntrinsicSizes content = width.isPresent()
                ? IntrinsicSizes.ZERO : intrinsic.get().plus(-Insets.resolve(s.box().margin(), 0).horizontal());
        OverlayEquation.Solution h = OverlayEquation.horizontal(left, width, right, marginLeft, marginRight,
                containerWidth, staticX, content);
        if (width.isEmpty()) {
            double constrained = BoxModel.constrainWidth(s, h.size(), containerWidth);
            if (constrained != h.size()) {
                h = OverlayEquation.horizontal(left, OptionalDouble.of(constrained), right, marginLeft, marginRight,
                        containerWidth, staticX, content);
            }
        }

        OptionalDouble top = s.offset().top().resolve(containerHeightBase);
        OptionalDouble bottom = s.offset().bottom().resolve(containerHeightBase);
        OptionalDouble marginTop = term(s.box().margin().top(), margin.top());
        OptionalDouble marginBottom = term(s.box().margin().bottom(), margin.bottom());
        ConstraintSpace space = ConstraintSpace.fixed(h.size(), OptionalDouble.empty(), containerWidth,
                containerHeightBase, false);
        BoxModel sized = BoxModel.resolve(box, space, intrinsic);
        OptionalDouble height = sized.height();
        if (height.isEmpty() && top.isPresent() && bottom.isPresent()) {
            // Only the height is auto: the box stretches between top and bottom.
            double stretched = Math.max(0, containerHeight - top.getAsDouble() - bottom.getAsDouble()
                    - marginTop.orElse(0) - marginBottom.orElse(0));
            height = OptionalDouble.of(Sizing.constrain(stretched, sized.minHeight(), sized.maxHeight()));
            space = ConstraintSpace.fixed(h.size(), height, containerWidth, containerHeightBase, false);
        }
        LayoutResult r = context.layout(box, space, null);
        Fragment f = r.fragment();
        OverlayEquation.Solution v = OverlayEquation.vertical(top, height, bottom, marginTop, marginBottom,
                containerHeight, staticY, f.height());

        double x = h.start() + h.marginStart() - containerPadding.left();
        double y = v.start() + v.marginStart() - containerPadding.top();
        // The Page overlay boxes inside go from the content box of this one to that of the reference container.
        double dx = x + f.border().left() + sized.padding().left();
        double dy = y + f.border().top() + sized.padding().top();
        List<PendingOverlay> pageOverlays = r.pending().stream().map(q -> q.translated(dx, dy)).toList();
        return LayoutResult.finished(f.moved(x, y), pageOverlays, AdjoiningMargins.NONE);
    }

    /** An {@code auto} margin is a free term of the equation (empty). */
    private static OptionalDouble term(Value specified, double used) {
        return specified.isAuto() ? OptionalDouble.empty() : OptionalDouble.of(used);
    }
}
