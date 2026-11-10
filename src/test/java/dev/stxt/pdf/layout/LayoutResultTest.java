package dev.stxt.pdf.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element;
import dev.stxt.pdf.tree.StyleEditor;

class LayoutResultTest {

    private final StyleEditor style = new StyleEditor();
    private final Box box = style.box("A", Element.BLOCK);
    private final Box overlay = style.box("B", Element.OVERLAY);

    /** A fragment of 100 × 50 with a border of 1 and a padding of 4 on every side, with one pending Overlay. */
    private LayoutResult result() {
        Fragment f = new Fragment(box, 0, 0, 100, 50, Insets.ZERO, new Insets(1, 1, 1, 1), new Insets(4, 4, 4, 4),
                List.of(), List.of(), true, true);
        return LayoutResult.finished(f, List.of(new PendingOverlay(overlay, 3, 7)), AdjoiningMargins.NONE);
    }

    @Test
    void aPlacedResultMovesItsFragmentAndItsOverlaysToTheParent() {
        LayoutResult.Placement p = result().placedAt(20, 30);
        assertEquals(20, p.fragment().x());
        assertEquals(30, p.fragment().y());
        // The static position goes from the content box of the box to that of its parent.
        assertEquals(List.of(new PendingOverlay(overlay, 3 + 20 + 5, 7 + 30 + 5)), p.pending());
    }

    @Test
    void aFrameKeepsItsOffsetInThePlacedFragment() {
        LayoutResult.Placement p = result().placedAt(26, 32, 6, 2);
        assertEquals(6, p.fragment().offsetX());
        assertEquals(2, p.fragment().offsetY());
        assertEquals(List.of(new PendingOverlay(overlay, 3 + 26 + 5, 7 + 32 + 5)), p.pending());
    }
}
