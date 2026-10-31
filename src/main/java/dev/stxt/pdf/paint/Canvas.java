package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.tree.Box;

/**
 * Painting target: {@link SvgCanvas} (debugging) or {@link PdfCanvas}. Absolute coordinates in pt. It only draws;
 * saying what the drawing is (the marked text of the tagged PDF and the links) is done by its {@link #tagging()},
 * which on a canvas that does not understand it is {@link Tagging#NONE}.
 */
public interface Canvas {

    /** A plain rectangle of one color: column backgrounds, leaders, text lines. */
    void background(Rect r, String color);

    /**
     * The background of a box (reference/common/box.md): a color or a gradient that fills
     * {@code paddingBox} with the corners {@code radii}, which are those of the padding box.
     */
    void fill(Rect paddingBox, Radii radii, Paint paint);

    /**
     * The border around the border box {@code r}: the ring between it (with the corners
     * {@code radii}) and the padding box, which is shared among the sides with a width in {@code w}
     * (each one with its color, split at the diagonal of each corner).
     *
     * @param colors the color of each side, already resolved
     */
    void border(Rect r, Insets w, Radii radii, Sides<String> colors);

    /**
     * What the canvas does with the meaning of what it paints: the marked text of the tagged PDF and the
     * links; {@link Tagging#NONE} if it only draws.
     */
    Tagging tagging();

    /** Debugging outline of a border box, if the canvas paints in debugging mode ({@link #outlines()}). */
    void outline(Rect borderBox);

    /**
     * Whether it paints the debugging outlines: painting requests them separately, without the clips of the rounded
     * corners, so that the whole box is visible.
     */
    default boolean outlines() {
        return false;
    }

    /**
     * Text with the baseline at {@code baselineY}.
     *
     * @param wordSpacing   space added to each space between words ({@code justify})
     * @param letterSpacing space added after each character except the last ({@code Letter spacing})
     */
    void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
              double wordSpacing, double letterSpacing);

    /**
     * The image of an {@code Image} (reference/components/image.md) in its content box
     * {@code content}, placed according to its {@code Fit} ({@link ImageFit}) and clipped to the box with
     * the corners {@code radii} (those of the content box).
     */
    void image(Box box, Rect content, Radii radii);

    /**
     * Starts painting rotated (reference/common/layers-and-rotation.md): everything painted until
     * {@link #endRotation()}, link areas included, rotates {@code degrees} degrees clockwise around the
     * point ({@code x}, {@code y}) of the page. Rotations nest: one inside another adds to the outer
     * one.
     */
    void beginRotation(double degrees, double x, double y);

    /** Ends the rotation started with {@link #beginRotation}. */
    void endRotation();

    /**
     * Starts clipping (reference/common/box.md): what is painted until {@link #endClip()} is only visible inside
     * {@code r} with the corners {@code radii}, under the current rotations. Clips nest with rotations: each one ends
     * before whatever started before it.
     */
    void beginClip(Rect r, Radii radii);

    /** Ends the clip started with {@link #beginClip}. */
    void endClip();

    /**
     * Ascent and descent with which the font is painted, to place the baseline
     * (reference/common/typography.md); without the font, {@link VerticalMetrics#APPROXIMATE}.
     */
    VerticalMetrics vertical(FontFace font);

    /**
     * Position and thickness of the underline and the strikethrough of the font (reference/common/text-formatting.md);
     * without the font, {@link DecorationMetrics#APPROXIMATE}.
     */
    DecorationMetrics decoration(FontFace font);
}
