package dev.stxt.pdf.model;

import java.awt.geom.AffineTransform;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The rotations that a box is painted inside (reference/common/layers-and-rotation.md), composed, in page coordinates
 * with y pointing down: each rotated box pushes its rotation when its painting, or the walk through its fragments,
 * starts and pops it when it ends. It is for what does not follow the content matrix of the PDF and has to know where a
 * rectangle ends up: the areas of the links, the {@code BBox} of the figures, the destinations, the contrast check and
 * the overflow check of what is rotated.
 */
public final class Turns {

    private final Deque<AffineTransform> outer = new ArrayDeque<>();
    private AffineTransform current = new AffineTransform();

    /**
     * Adds the rotation of {@code degrees} degrees around ({@code x}, {@code y}) inside the current ones; a positive
     * angle rotates, as seen, clockwise.
     */
    public void push(double degrees, double x, double y) {
        outer.push(current);
        current = new AffineTransform(current);
        current.rotate(Math.toRadians(degrees), x, y);
    }

    /** Removes the last rotation added with {@link #push}. */
    public void pop() {
        if (outer.isEmpty()) {
            throw new IllegalStateException("No rotation to end");
        }
        current = outer.pop();
    }

    /** The current rotations, composed; the identity if there are none. A copy: changing it does not change them. */
    public AffineTransform current() {
        return new AffineTransform(current);
    }

    /**
     * The rectangle that encloses {@code r} with the current rotations ({@link Rotation#envelope}); {@code r} itself if
     * there are none.
     */
    public Rect envelope(Rect r) {
        return current.isIdentity() ? r : Rotation.envelope(current, r);
    }
}
