package dev.stxt.pdf.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.LineStyle;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;

/** The pattern of a line that is not solid (reference/common/box.md#border-style). */
class DashesTest {

    private static final double DELTA = 1e-9;

    @Test
    void aSolidLineHasNoPattern() {
        assertNull(Dashes.open(LineStyle.SOLID, 100, 1));
        assertNull(Dashes.closed(LineStyle.SOLID, 100, 1));
    }

    /** 100 long and 2 wide: dashes of 6; 9 of them leave 46 for 8 gaps of 5.75, and the last dash ends at 100. */
    @Test
    void aDashedLineStartsAndEndsWithAWholeDash() {
        Dashes.Pattern p = Dashes.open(LineStyle.DASHED, 100, 2);
        assertEquals(6, p.on(), DELTA);
        assertEquals(5.75, p.off(), DELTA);
        assertEquals(0, p.phase(), DELTA);
        assertFalse(p.round());
        assertEquals(100, 9 * p.on() + 8 * p.off(), DELTA);
    }

    /** With the ideal length, the gap is as long as the dash. */
    @Test
    void theGapIsAsLongAsTheDashWhenTheLengthAllowsIt() {
        Dashes.Pattern p = Dashes.open(LineStyle.DASHED, 27, 1);
        assertEquals(3, p.on(), DELTA);
        assertEquals(3, p.off(), DELTA);
    }

    /** Less than two dashes with a gap between them: the line is solid. */
    @Test
    void aDashedLineTooShortForTwoDashesIsSolid() {
        assertNull(Dashes.open(LineStyle.DASHED, 5, 1));
        assertNull(Dashes.open(LineStyle.DASHED, 6, 1));
        assertEquals(1, Dashes.open(LineStyle.DASHED, 7, 1).off(), DELTA);
    }

    /**
     * 100 long and 2 wide: the centers go from 1 to 99, and 26 dots leave 3.92 between them. A dot is a dash of length 0
     * with round ends, and the pattern starts with the gap that is left before the first center.
     */
    @Test
    void aDottedLineStartsAndEndsWithAWholeDot() {
        Dashes.Pattern p = Dashes.open(LineStyle.DOTTED, 100, 2);
        assertEquals(0, p.on(), DELTA);
        assertEquals(98.0 / 25, p.off(), DELTA);
        assertTrue(p.round());
        // The first dot is centered 1 from the start: that much of the gap is left.
        assertEquals(1, p.off() - p.phase(), DELTA);
    }

    @Test
    void aDottedLineTooShortForTwoDotsHasOneInTheMiddle() {
        Dashes.Pattern p = Dashes.open(LineStyle.DOTTED, 3, 2);
        assertEquals(1.5, p.off() - p.phase(), DELTA);
        assertTrue(p.off() > 3, "the second dot is beyond the end of the line");
    }

    /** A closed line of 120 and 2 wide: 10 dashes of 6 with gaps of 6, or 30 dots 4 apart. */
    @Test
    void aClosedLineHasAWholeNumberOfDashesOrDots() {
        Dashes.Pattern dashed = Dashes.closed(LineStyle.DASHED, 120, 2);
        assertEquals(6, dashed.on(), DELTA);
        assertEquals(6, dashed.off(), DELTA);
        Dashes.Pattern uneven = Dashes.closed(LineStyle.DASHED, 130, 2);
        assertEquals(130, 11 * (uneven.on() + uneven.off()), DELTA);
        Dashes.Pattern dotted = Dashes.closed(LineStyle.DOTTED, 122, 2);
        assertEquals(122, 31 * dotted.off(), 1e-6);
        assertNull(Dashes.closed(LineStyle.DASHED, 5, 2));
    }

    /** A horizontal line runs along the middle of its rectangle, and a vertical one too. */
    @Test
    void aSegmentRunsAlongTheMiddleOfItsRectangle() {
        Dashes.Segment h = Dashes.segment(new Rect(10, 20, 100, 2), false, LineStyle.DASHED, 0, 0);
        assertEquals(List.of(10.0, 21.0, 110.0, 21.0, 2.0), List.of(h.x1(), h.y1(), h.x2(), h.y2(), h.width()));
        Dashes.Segment v = Dashes.segment(new Rect(10, 20, 2, 100), true, LineStyle.DASHED, 0, 0);
        assertEquals(List.of(11.0, 20.0, 11.0, 120.0, 2.0), List.of(v.x1(), v.y1(), v.x2(), v.y2(), v.width()));
        assertEquals(h.pattern(), v.pattern());
    }

    /** What is trimmed from a dashed line is cut out of its pattern, which does not move. */
    @Test
    void trimmingADashedLineKeepsItsPatternInPlace() {
        Dashes.Segment whole = Dashes.segment(new Rect(0, 0, 2, 100), true, LineStyle.DASHED, 0, 0);
        Dashes.Segment trimmed = Dashes.segment(new Rect(0, 0, 2, 100), true, LineStyle.DASHED, 2, 2);
        assertEquals(2, trimmed.y1(), DELTA);
        assertEquals(98, trimmed.y2(), DELTA);
        assertEquals(whole.pattern().on(), trimmed.pattern().on(), DELTA);
        assertEquals(2, trimmed.pattern().phase(), DELTA);
    }

    /**
     * A dotted line of 100 and 2 wide has its dots centered on 1, 4.92, 8.84... Trimmed by 2, the first one, which
     * the trim would cut, is left out: the line starts just before the center of the second, with the pattern there.
     */
    @Test
    void trimmingADottedLineLeavesOutTheDotsItWouldCut() {
        Dashes.Segment trimmed = Dashes.segment(new Rect(0, 0, 2, 100), true, LineStyle.DOTTED, 2, 2);
        double pitch = 98.0 / 25;
        assertTrue(trimmed.y1() > 1 && trimmed.y1() < 1 + pitch, "starts between the first and the second dot");
        assertTrue(trimmed.y2() < 99 && trimmed.y2() > 99 - pitch, "ends between the last two dots");
        // From the start of the line, the next dot is the second one.
        double toNext = (trimmed.pattern().off() - trimmed.pattern().phase()) % trimmed.pattern().off();
        assertEquals(1 + pitch, trimmed.y1() + toNext, 1e-6);
    }

    @Test
    void aSegmentTrimmedAwayIsNoLine() {
        assertNull(Dashes.segment(new Rect(0, 0, 2, 4), true, LineStyle.DASHED, 2, 2));
        assertNull(Dashes.segment(new Rect(0, 0, 2, 4), true, LineStyle.DOTTED, 2, 2));
    }

    /** Without a pattern, a short dashed line is still a line. */
    @Test
    void aShortDashedSegmentIsSolid() {
        Dashes.Segment s = Dashes.segment(new Rect(0, 0, 5, 1), false, LineStyle.DASHED, 0, 0);
        assertNull(s.pattern());
        assertEquals(5, s.x2(), DELTA);
    }

    /**
     * The four sides of a border of 2 around a box of 100 × 60: the horizontal ones take the whole width, corners
     * included, and the vertical ones go between them.
     */
    @Test
    void theCornersOfABorderBelongToItsHorizontalSides() {
        List<Dashes.SideSegment> sides = Dashes.border(new Rect(10, 20, 100, 60), Insets.all(2), LineStyle.DASHED);
        assertEquals(List.of(Shapes.Side.TOP, Shapes.Side.BOTTOM, Shapes.Side.LEFT, Shapes.Side.RIGHT),
                sides.stream().map(Dashes.SideSegment::side).toList());
        Dashes.Segment top = sides.get(0).segment();
        assertEquals(List.of(10.0, 21.0, 110.0, 21.0), List.of(top.x1(), top.y1(), top.x2(), top.y2()));
        Dashes.Segment bottom = sides.get(1).segment();
        assertEquals(79, bottom.y1(), DELTA);
        Dashes.Segment left = sides.get(2).segment();
        assertEquals(List.of(11.0, 22.0, 11.0, 78.0), List.of(left.x1(), left.y1(), left.x2(), left.y2()));
        Dashes.Segment right = sides.get(3).segment();
        assertEquals(109, right.x1(), DELTA);
    }

    /** A side without width has no line, and a vertical side with no horizontal one beside it takes its whole height. */
    @Test
    void onlyTheSidesWithAWidthAreDrawn() {
        List<Dashes.SideSegment> sides = Dashes.border(new Rect(10, 20, 100, 60), new Insets(0, 0, 0, 3),
                LineStyle.DOTTED);
        assertEquals(1, sides.size());
        Dashes.Segment left = sides.get(0).segment();
        assertEquals(Shapes.Side.LEFT, sides.get(0).side());
        assertEquals(List.of(11.5, 20.0, 11.5, 80.0, 3.0),
                List.of(left.x1(), left.y1(), left.x2(), left.y2(), left.width()));
    }

    /** The line of a rectangle has its pattern along its whole length, and an empty rectangle draws no line. */
    @Test
    void theLineOfARectangleIsFittedToItAndAnEmptyOneIsNoLine() {
        Dashes.Segment line = Dashes.line(new Rect(10, 20, 2, 100), true, LineStyle.DASHED);
        assertEquals(11, line.x1(), DELTA);
        assertEquals(20, line.y1(), DELTA);
        assertEquals(120, line.y2(), DELTA);
        assertEquals(2, line.width(), DELTA);
        assertEquals(Dashes.open(LineStyle.DASHED, 100, 2), line.pattern());
        assertNull(Dashes.line(new Rect(10, 20, 0, 100), true, LineStyle.DASHED));
        assertNull(Dashes.line(new Rect(10, 20, 100, 0), false, LineStyle.DOTTED));
    }

    /**
     * A border of 4 on its four sides is one closed line, 2 × (96 + 56) = 304 long along its middle, with the pattern
     * of a closed line; without its bottom side it is an open line, with that of a line with two ends. The line is
     * as wide as the widest side.
     */
    @Test
    void theLinesOfARoundedBorderHaveTheirPatternFittedToTheirLength() {
        Rect box = new Rect(10, 20, 100, 60);
        List<Dashes.Curve> whole = Dashes.rounded(box, Insets.all(4), Radii.NONE, LineStyle.DASHED);
        assertEquals(1, whole.size());
        assertEquals(4, whole.get(0).width(), DELTA);
        assertEquals(Dashes.closed(LineStyle.DASHED, 304, 4), whole.get(0).pattern());

        List<Dashes.Curve> open = Dashes.rounded(box, new Insets(4, 2, 0, 2), Radii.NONE, LineStyle.DOTTED);
        assertEquals(1, open.size());
        assertEquals(4, open.get(0).width(), DELTA);
        double length = Shapes.length(open.get(0).path());
        assertEquals(Dashes.open(LineStyle.DOTTED, length, 4), open.get(0).pattern());
        assertEquals(List.of(), Dashes.rounded(box, Insets.ZERO, Radii.NONE, LineStyle.DASHED));
    }
}
