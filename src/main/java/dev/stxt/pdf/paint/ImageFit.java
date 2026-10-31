package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Fit;
import dev.stxt.pdf.model.Rect;

/**
 * Where an image is drawn in its content box according to {@code Fit}
 * (reference/components/image.md). With equal proportions, the three modes give the content box.
 */
final class ImageFit {

    private ImageFit() {
    }

    /**
     * Rectangle of the image, in the coordinates of {@code content}: with {@code inside} it fits
     * whole and centered; with {@code fill} it covers it, centered, and is clipped to the content
     * box; with {@code stretch} it is the content box.
     *
     * @param width  natural width of the image (only its proportion counts)
     * @param height natural height of the image
     */
    static Rect place(Fit fit, Rect content, double width, double height) {
        if (fit == Fit.STRETCH) {
            return content;
        }
        double sx = content.width() / width;
        double sy = content.height() / height;
        double scale = fit == Fit.INSIDE ? Math.min(sx, sy) : Math.max(sx, sy);
        double w = width * scale;
        double h = height * scale;
        return new Rect(content.x() + (content.width() - w) / 2, content.y() + (content.height() - h) / 2, w, h);
    }
}
