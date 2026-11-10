package dev.stxt.pdf.layout.block;

import dev.stxt.pdf.layout.BoxModel;
import dev.stxt.pdf.layout.ConstraintSpace;
import dev.stxt.pdf.layout.LayoutContext;
import dev.stxt.pdf.layout.MarginStrut;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.tree.Box;

/**
 * The margins that will come out of a box at the top when it is laid out as its first fragment
 * (reference/components/block.md), predicted from the tree before laying it out: those of the chain of its first
 * in-flow descendants while each box is open at the top (block flow, without border or padding at the top, not
 * isolating margins nor trimming them), plus those of the empty children before each of them, which let both of theirs through. Side
 * boxes and out-of-flow boxes do not take part in the vertical flow and are skipped, as in {@link BlockLayout}.
 *
 * <p>It is a prediction, not the rule: a text counts as non-empty (whether it has lines is only known once it is laid
 * out) and a block as empty only when it has no in-flow children. {@link BlockLayout} places the child where the
 * prediction says, so that a chapter longer than the page is laid out once on the page where it starts, and checks it
 * against what the layout returns ({@code AdjoiningMargins}): when they differ, the child is moved and laid out again
 * if needed, as it always was. The prediction only saves work; the result is the layout's.</p>
 */
final class LeadingMargins {

    private LeadingMargins() {
    }

    /**
     * The margins predicted to come out of {@code box} at the top (without the box's own top margin) when it is laid
     * out in {@code space} as its first fragment.
     */
    static MarginStrut predict(Box box, ConstraintSpace space, LayoutContext context) {
        MarginStrut out = MarginStrut.EMPTY;
        Box current = box;
        ConstraintSpace s = space;
        // A box that trims its margins drops those of its first child: nothing comes out of it.
        while (current.style().layout() == Layout.BLOCK && !current.isolatesMargins() && !current.style().trimMargins()) {
            BoxModel model = BoxModel.resolve(current, s, context);
            if (!model.openAtTop()) {
                return out;
            }
            double width = model.contentWidth();
            ConstraintSpace childSpace = model.childSpace(s, true);
            Box next = null;
            for (Box child : current.children()) {
                if (child.style().isOutOfFlow() || child.style().isSideBox()) {
                    continue;
                }
                Insets margin = Insets.resolve(child.style().box().margin(), width);
                out = out.add(margin.top());
                if (!isEmpty(child, childSpace, context)) {
                    next = child;
                    break;
                }
                // An empty child lets its bottom margin through too, and the next child still touches the top.
                out = out.add(margin.bottom());
            }
            if (next == null) {
                return out;
            }
            current = next;
            s = childSpace;
        }
        return out;
    }

    /**
     * A block that will let its margins merge with each other ({@code AdjoiningMargins.collapsesThrough}): no in-flow
     * children, nothing between its content and its edges and no minimum height. A text is taken as non-empty.
     */
    private static boolean isEmpty(Box box, ConstraintSpace space, LayoutContext context) {
        if (box.style().layout() != Layout.BLOCK || box.isolatesMargins() || !box.inFlowChildren().isEmpty()) {
            return false;
        }
        BoxModel model = BoxModel.resolve(box, space, context);
        return model.openAtTop() && model.openAtBottom() && model.minHeight() <= 0;
    }
}
