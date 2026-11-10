package dev.stxt.pdf.paint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.model.Fit;
import dev.stxt.pdf.model.Rect;

class ImageFitTest {

    /** A 100 × 100 box at (10, 20) and an image with ratio 2. */
    private static final Rect BOX = new Rect(10, 20, 100, 100);

    @Test
    void insideFitsTheWholeImageCentered() {
        assertEquals(new Rect(10, 45, 100, 50), ImageFit.place(Fit.INSIDE, BOX, 150, 75));
    }

    @Test
    void fillCoversTheBoxCentered() {
        assertEquals(new Rect(-40, 20, 200, 100), ImageFit.place(Fit.CROP, BOX, 150, 75));
    }

    @Test
    void stretchIsTheBox() {
        assertEquals(BOX, ImageFit.place(Fit.STRETCH, BOX, 150, 75));
    }

    @Test
    void withTheSameRatioTheyAreTheBox() {
        for (Fit fit : Fit.values()) {
            assertEquals(new Rect(10, 20, 100, 100), ImageFit.place(fit, BOX, 30, 30), fit.name());
        }
    }
}
