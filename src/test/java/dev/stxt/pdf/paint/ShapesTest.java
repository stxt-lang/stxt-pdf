package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Corners;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Value;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The paths of the shapes that the canvases paint (reference/common/box.md). */
class ShapesTest {

    /** Records the path operations. */
    private static final class Recorder implements Shapes.Path {
        final List<String> ops = new ArrayList<>();

        static String n(double v) {
            return String.format(Locale.ROOT, "%.2f", v);
        }

        @Override
        public void moveTo(double x, double y) {
            ops.add("M " + n(x) + " " + n(y));
        }

        @Override
        public void lineTo(double x, double y) {
            ops.add("L " + n(x) + " " + n(y));
        }

        @Override
        public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
            ops.add("C " + n(x1) + " " + n(y1) + " " + n(x2) + " " + n(y2) + " " + n(x3) + " " + n(y3));
        }

        @Override
        public void close() {
            ops.add("Z");
        }

        @Override
        public void rect(double x, double y, double width, double height) {
            ops.add("R " + n(x) + " " + n(y) + " " + n(width) + " " + n(height));
        }
    }

    private static final Rect BOX = new Rect(10, 20, 100, 60);

    @Test
    void aSquareBoxIsARect() {
        Recorder path = new Recorder();
        Shapes.roundedRect(path, BOX, Radii.NONE);
        assertEquals(List.of("R 10.00 20.00 100.00 60.00"), path.ops);
    }

    @Test
    void theDefaultRectIsFourLines() {
        Shapes.Path path = new Shapes.Path() {
            final List<String> ops = new ArrayList<>();

            @Override
            public void moveTo(double x, double y) {
                ops.add("M");
            }

            @Override
            public void lineTo(double x, double y) {
                ops.add("L");
            }

            @Override
            public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
            }

            @Override
            public void close() {
                ops.add("Z");
                assertEquals(List.of("M", "L", "L", "L", "Z"), ops);
            }
        };
        path.rect(0, 0, 1, 1);
    }

    @Test
    void aRoundedBoxHasOneCurvePerRoundCornerAndNoZeroLengthLines() {
        Radii radii = Radii.resolve(new Corners<>(Value.pt(10), Value.pt(0), Value.pt(0), Value.pt(0)), 100, 60);
        Recorder path = new Recorder();
        Shapes.roundedRect(path, BOX, radii);
        assertEquals(List.of(
                "M 20.00 20.00",
                "L 110.00 20.00",
                "L 110.00 80.00",
                "L 10.00 80.00",
                "L 10.00 30.00",
                "C 10.00 24.48 14.48 20.00 20.00 20.00",
                "Z"), path.ops);
    }

    @Test
    void anEllipticalCornerUsesBothRadii() {
        Radii radii = Radii.resolve(Corners.all(new Value.Percentage(50)), 100, 60);
        Recorder path = new Recorder();
        Shapes.roundedRect(path, BOX, radii);
        // Radii of half the width and half the height: the straight sides run out and only the four curves remain.
        // Top right: from (60, 20) to (110, 50), with the control points at 0.5523 of each radius.
        assertEquals("C 87.61 20.00 110.00 33.43 110.00 50.00", path.ops.get(1));
        assertEquals(List.of("M", "C", "C", "C", "C", "Z"), path.ops.stream().map(o -> o.substring(0, 1)).toList());
    }

    @Test
    void withRoundedInnerCornersTheDiagonalsGoInsideToCoverTheCurve() {
        Insets w = Insets.all(4);
        Radii inner = Radii.resolve(Corners.all(Value.pt(14)), 100, 60).inset(w);
        Recorder top = new Recorder();
        Shapes.sidePolygon(top, Shapes.Side.TOP, BOX, w, inner);
        // Padding corners (14, 24) and (106, 24); the diagonal continues 2 · 10 = 20 pt inward.
        double d = 20 / Math.sqrt(2);
        assertEquals(List.of("M 10.00 20.00", "L 110.00 20.00", "L " + Recorder.n(106 - d) + " " + Recorder.n(24 + d),
                "L " + Recorder.n(14 + d) + " " + Recorder.n(24 + d), "Z"), top.ops);
        Recorder left = new Recorder();
        Shapes.sidePolygon(left, Shapes.Side.LEFT, BOX, w, inner);
        // They share the top left diagonal: the same point.
        assertEquals("L " + Recorder.n(14 + d) + " " + Recorder.n(24 + d), left.ops.get(2));
    }

    @Test
    void theSidesOfABorderMeetOnTheDiagonalOfTheCorners() {
        Insets w = new Insets(2, 4, 6, 8);
        Recorder top = new Recorder();
        Shapes.sidePolygon(top, Shapes.Side.TOP, BOX, w, Radii.NONE);
        assertEquals(List.of("M 10.00 20.00", "L 110.00 20.00", "L 106.00 22.00", "L 18.00 22.00", "Z"), top.ops);
        Recorder left = new Recorder();
        Shapes.sidePolygon(left, Shapes.Side.LEFT, BOX, w, Radii.NONE);
        assertEquals(List.of("M 10.00 80.00", "L 10.00 20.00", "L 18.00 22.00", "L 18.00 74.00", "Z"), left.ops);
        Recorder right = new Recorder();
        Shapes.sidePolygon(right, Shapes.Side.RIGHT, BOX, w, Radii.NONE);
        assertEquals(List.of("M 110.00 20.00", "L 110.00 80.00", "L 106.00 74.00", "L 106.00 22.00", "Z"), right.ops);
        Recorder bottom = new Recorder();
        Shapes.sidePolygon(bottom, Shapes.Side.BOTTOM, BOX, w, Radii.NONE);
        assertEquals(List.of("M 110.00 80.00", "L 10.00 80.00", "L 18.00 74.00", "L 106.00 74.00", "Z"), bottom.ops);
    }
}
