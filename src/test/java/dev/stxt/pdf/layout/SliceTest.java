package dev.stxt.pdf.layout;

import dev.stxt.pdf.model.Edges;
import dev.stxt.pdf.model.Insets;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Distribution of the height, the border and the padding between fragments (reference/common/breaks.md). All the
 * boxes have border 1 and padding 4: 5 pt of decoration at the top and 5 at the bottom.
 */
class SliceTest {

    private static final Insets BORDER = Insets.all(1);
    private static final Insets PADDING = Insets.all(4);
    private static final OptionalDouble NONE = OptionalDouble.empty();
    private static final BoxModel AUTO = model(NONE, 0, NONE, Edges.OPEN);
    private static final BoxModel AUTO_CLONE = model(NONE, 0, NONE, Edges.CLOSED);

    private static BoxModel model(OptionalDouble height, double min, OptionalDouble max, Edges d) {
        return new BoxModel(Insets.ZERO, BORDER, PADDING, 100, height, min, max, d);
    }

    private static ConstraintSpace page(double remaining) {
        return ConstraintSpace.forChildren(100, NONE, OptionalDouble.of(remaining), false);
    }

    /** Border or padding with only the given sides. */
    private static Insets sides(Insets all, boolean top, boolean bottom) {
        return new Insets(top ? all.top() : 0, all.right(), bottom ? all.bottom() : 0, all.left());
    }

    private static void assertSlice(Slice actual, double height, boolean top, boolean bottom, boolean finished,
                                    double consumed) {
        assertEquals(new Slice(height, sides(BORDER, top, bottom), sides(PADDING, top, bottom), finished, consumed),
                actual);
    }

    @Test
    void withoutFragmentationTheBoxIsWhole() {
        ConstraintSpace free = ConstraintSpace.free(100, 100, NONE, false);
        assertSlice(Slice.of(AUTO, true, 0, 30, true, free), 40, true, true, true, 0);
    }

    @Test
    void autoHeightSliceAcrossThreeFragments() {
        // First: top decoration and the content; without the bottom one.
        Slice first = Slice.of(AUTO, true, 0, 90, false, page(100));
        assertSlice(first, 95, true, false, false, 95);
        // Middle: no decoration.
        Slice middle = Slice.of(AUTO, false, first.consumed(), 100, false, page(100));
        assertSlice(middle, 100, false, false, false, 195);
        // Last: only the bottom decoration.
        Slice last = Slice.of(AUTO, false, middle.consumed(), 20, true, page(100));
        assertSlice(last, 25, false, true, true, 220);
    }

    @Test
    void definiteHeightIsTheTotalOfAllFragments() {
        BoxModel m = model(OptionalDouble.of(300), 0, NONE, Edges.OPEN);
        // The content ends, but the box's own height does not fit: the page fills up and it continues.
        Slice first = Slice.of(m, true, 0, 20, true, page(100));
        assertSlice(first, 100, true, false, false, 100);
        // What is missing, with the bottom decoration.
        Slice last = Slice.of(m, false, first.consumed(), 0, true, page(762));
        assertSlice(last, 200, false, true, true, 300);
    }

    @Test
    void definiteHeightCutInsideTheContentFillsThePage() {
        BoxModel m = model(OptionalDouble.of(300), 0, NONE, Edges.OPEN);
        // It reaches the end of the page, but only consumes its top border and padding (5) and what the content takes
        // up (50): the rest of the height is left for the content that follows.
        assertSlice(Slice.of(m, true, 0, 50, false, page(100)), 100, true, false, false, 55);
        // Without going beyond what the box has left.
        assertSlice(Slice.of(m, false, 250, 10, false, page(100)), 50, false, false, false, 260);
    }

    @Test
    void minHeightAppliesToTheTotal() {
        BoxModel m = model(NONE, 200, NONE, Edges.OPEN);
        Slice first = Slice.of(m, true, 0, 20, true, page(100));
        assertSlice(first, 100, true, false, false, 100);
        Slice last = Slice.of(m, false, first.consumed(), 0, true, page(762));
        assertSlice(last, 100, false, true, true, 200);
    }

    @Test
    void maxHeightAppliesToTheTotal() {
        BoxModel m = model(NONE, 0, OptionalDouble.of(50), Edges.OPEN);
        assertSlice(Slice.of(m, true, 0, 80, true, page(200)), 50, true, true, true, 50);
    }

    @Test
    void closedRepeatsTheEdgesInEveryFragment() {
        // 100 of page: 90 for the content.
        Slice first = Slice.of(AUTO_CLONE, true, 0, 90, false, page(100));
        assertSlice(first, 100, true, true, false, 90);
        // With clone, consumed is the height of the content box.
        Slice last = Slice.of(AUTO_CLONE, false, first.consumed(), 30, true, page(100));
        assertSlice(last, 40, true, true, true, 120);
    }

    @Test
    void cloneWithDefiniteHeightSplitsTheContentBoxHeight() {
        // Height 250: 240 of content box, distributed as 90 + 90 + 60.
        BoxModel m = model(OptionalDouble.of(250), 0, NONE, Edges.CLOSED);
        Slice first = Slice.of(m, true, 0, 20, true, page(100));
        assertSlice(first, 100, true, true, false, 90);
        Slice second = Slice.of(m, false, first.consumed(), 0, true, page(100));
        assertSlice(second, 100, true, true, false, 180);
        Slice last = Slice.of(m, false, second.consumed(), 0, true, page(100));
        assertSlice(last, 70, true, true, true, 240);
        // Broken inside the content, it reaches the end of the page and consumes only the content.
        assertSlice(Slice.of(m, true, 0, 20, false, page(100)), 100, true, true, false, 20);
    }

    @Test
    void topDecoration() {
        assertEquals(5, Slice.top(AUTO, true), 1e-9);
        assertEquals(0, Slice.top(AUTO, false), 1e-9);
        assertEquals(5, Slice.top(AUTO_CLONE, false), 1e-9);
    }

    @Test
    void contentRemaining() {
        // With slice, the bottom decoration is not reserved (Engine.layout does it if the box ends).
        assertEquals(95, Slice.contentRemaining(AUTO, true, page(100)), 1e-9);
        assertEquals(100, Slice.contentRemaining(AUTO, false, page(100)), 1e-9);
        // With clone, it is: it goes in every fragment.
        assertEquals(90, Slice.contentRemaining(AUTO_CLONE, false, page(100)), 1e-9);
    }

    @Test
    void contentHeight() {
        assertEquals(90, new Slice(100, BORDER, PADDING, true, 0).contentHeight(), 1e-9);
        assertEquals(0, new Slice(6, BORDER, PADDING, true, 0).contentHeight(), 1e-9);
    }
}
