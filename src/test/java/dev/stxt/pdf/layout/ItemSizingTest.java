package dev.stxt.pdf.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.model.Alignment;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element;
import dev.stxt.pdf.tree.StyleEditor;

/** Sizing of an item in the space its container gives it (reference/common/box.md), with hand-written styles. */
class ItemSizingTest {

    private static final OptionalDouble NONE = OptionalDouble.empty();
    /** The fit to the content is the only case that needs the intrinsic sizes. */
    private static final Supplier<IntrinsicSizes> UNUSED = () -> {
        throw new AssertionError("intrinsic sizes are not needed");
    };
    private static final Insets MARGIN = new Insets(5, 10, 15, 20);

    private final StyleEditor style = new StyleEditor();
    private final Box box = style.box("A", Element.BLOCK);

    private BoxModel probe(double area, OptionalDouble heightBase) {
        return BoxModel.resolve(box, ConstraintSpace.free(area, area, heightBase, false), UNUSED);
    }

    private double width(double area, boolean stretch, Supplier<IntrinsicSizes> intrinsic) {
        return ItemSizing.width(style.style(), probe(area, NONE), area, MARGIN, stretch, intrinsic);
    }

    @Test
    void aDeclaredWidthIsKeptWhetherStretchedOrNot() {
        style.width(new Value.Percentage(40));
        assertEquals(120, width(300, true, UNUSED), 1e-9);
        assertEquals(120, width(300, false, UNUSED), 1e-9);
    }

    @Test
    void aStretchedItemTakesTheSpaceWithoutItsMarginsUpToItsMaximum() {
        assertEquals(270, width(300, true, UNUSED), 1e-9);
        style.maxWidth(new Value.Percentage(50));
        assertEquals(150, width(300, true, UNUSED), 1e-9);
        // With more margins than space, 0, and then the minimum.
        style.maxWidth(null);
        style.minWidth(Value.pt(12));
        assertEquals(12, width(20, true, UNUSED), 1e-9);
    }

    @Test
    void otherwiseTheItemFitsItsContent() {
        // The intrinsic sizes are those of the margin box: fit(300) between 60 and 200 is 200, without margins 170.
        Supplier<IntrinsicSizes> intrinsic = () -> new IntrinsicSizes(60, 200);
        assertEquals(170, width(300, false, intrinsic), 1e-9);
        assertEquals(70, width(100, false, intrinsic), 1e-9);
        style.minWidth(Value.pt(100));
        assertEquals(100, width(100, false, intrinsic), 1e-9);
    }

    @Test
    void aStretchedWidthIsLimitedAgainstItsOwnBase() {
        assertEquals(OptionalDouble.of(170), ItemSizing.stretchedWidth(style.style(), 200, MARGIN, 400));
        // The space is the line; the base of Max width, the content box of the container.
        style.maxWidth(new Value.Percentage(25));
        assertEquals(OptionalDouble.of(100), ItemSizing.stretchedWidth(style.style(), 200, MARGIN, 400));
        style.width(Value.pt(50));
        assertEquals(NONE, ItemSizing.stretchedWidth(style.style(), 200, MARGIN, 400));
    }

    @Test
    void aStretchedHeightIsTheSpaceWithoutTheMarginsBetweenMinimumAndMaximum() {
        OptionalDouble base = OptionalDouble.of(200);
        assertEquals(OptionalDouble.of(80), ItemSizing.stretchedHeight(probe(300, base), 100, MARGIN));
        style.maxHeight(new Value.Percentage(30));
        assertEquals(OptionalDouble.of(60), ItemSizing.stretchedHeight(probe(300, base), 100, MARGIN));
        style.maxHeight(null);
        style.minHeight(Value.pt(30));
        assertEquals(OptionalDouble.of(30), ItemSizing.stretchedHeight(probe(300, base), 10, MARGIN));
    }

    @Test
    void anItemWithADefiniteHeightIsNotStretched() {
        style.height(new Value.Percentage(50));
        assertEquals(NONE, ItemSizing.stretchedHeight(probe(300, OptionalDouble.of(200)), 100, MARGIN));
        // Without a definite base the percentage is auto, and the item stretches.
        assertEquals(OptionalDouble.of(80), ItemSizing.stretchedHeight(probe(300, NONE), 100, MARGIN));
    }

    @Test
    void theOwnAlignmentWinsOverTheContainers() {
        assertEquals(Alignment.END, ItemSizing.effective(Alignment.END, Alignment.STRETCH));
        assertEquals(Alignment.STRETCH, ItemSizing.effective(null, Alignment.STRETCH));
    }

    @Test
    void theOutOfFlowChildrenWaitAtTheCornerOfTheContentBox() {
        Box inFlow = new StyleEditor().box("B", Element.BLOCK);
        Box frame = new StyleEditor().position(Position.FRAME).box("F", Element.FRAME);
        Box overlay = new StyleEditor().position(Position.OVERLAY).box("O", Element.OVERLAY);
        Box pageOverlay = new StyleEditor().position(Position.PAGE_OVERLAY).box("P", Element.PAGE_OVERLAY);
        StyleEditor.addChildren(box, overlay, inFlow, frame, pageOverlay);
        assertEquals(List.of(new PendingOverlay(overlay, 0, 0), new PendingOverlay(pageOverlay, 0, 0)),
                ItemSizing.outOfFlow(box));
        assertEquals(List.of(), ItemSizing.outOfFlow(inFlow));
    }
}
