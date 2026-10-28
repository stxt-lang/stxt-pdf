package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.tree.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageFitTest {

    /** Una caja de 100 × 100 en (10, 20) y una imagen de proporción 2. */
    private static final Rect BOX = new Rect(10, 20, 100, 100);

    @Test
    void containFitsTheWholeImageCentered() {
        assertEquals(new Rect(10, 45, 100, 50), ImageFit.place(Style.Fit.CONTAIN, BOX, 150, 75));
    }

    @Test
    void coverFillsTheBoxCentered() {
        assertEquals(new Rect(-40, 20, 200, 100), ImageFit.place(Style.Fit.COVER, BOX, 150, 75));
    }

    @Test
    void fillIsTheBox() {
        assertEquals(BOX, ImageFit.place(Style.Fit.FILL, BOX, 150, 75));
    }

    @Test
    void withTheSameRatioTheyAreTheBox() {
        for (Style.Fit fit : Style.Fit.values()) {
            assertEquals(new Rect(10, 20, 100, 100), ImageFit.place(fit, BOX, 30, 30), fit.name());
        }
    }
}
