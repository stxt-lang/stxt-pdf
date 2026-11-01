package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Rect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** {@code Mirror margins} (reference/components/document.md): the medium of each physical page. */
class PageSetupTest {

    private static final Insets MARGIN = new Insets(20, 30, 20, 50);

    @Test
    void evenPagesSwapTheLeftAndRightMargins() {
        PageSetup mirrored = new PageSetup(300, 200, MARGIN, true);
        assertSame(mirrored, mirrored.forPage(1));
        assertSame(mirrored, mirrored.forPage(3));
        PageSetup even = mirrored.forPage(2);
        assertEquals(new Insets(20, 50, 20, 30), even.margin());
        assertEquals(true, even.mirrorMargins());
        assertEquals(new Rect(50, 20, 220, 160), mirrored.pageArea());
        assertEquals(new Rect(30, 20, 220, 160), even.pageArea());
        // The page area measures the same on both sides: the flow does not depend on the page.
        assertEquals(mirrored.pageArea().width(), even.pageArea().width());
    }

    @Test
    void withoutMirrorMarginsEveryPageHasTheSameMedium() {
        PageSetup plain = new PageSetup(300, 200, MARGIN);
        assertEquals(false, plain.mirrorMargins());
        assertSame(plain, plain.forPage(1));
        assertSame(plain, plain.forPage(2));
    }
}
