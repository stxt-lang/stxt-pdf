package dev.stxt.pdf.layout.block;

import dev.stxt.pdf.layout.ConstraintSpace;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.MarginStrut;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.BoxTreeBuilder;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The margins predicted to come out of a block at the top ({@link LeadingMargins}) are the ones its layout returns
 * ({@code AdjoiningMargins.top}), in every case the prediction covers: each test checks both.
 */
class LeadingMarginsTest {

    private static final String HEAD = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n"
            + "\t\tWidth: 595\n\t\tHeight: 842\n\t\tMargin: 40\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n"
            + "\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n\tSection:\n";

    private static final ConstraintSpace SPACE = ConstraintSpace.forChildren(500, OptionalDouble.empty(),
            OptionalDouble.empty(), true);

    private static Box find(Box box, String id) {
        if (id.equals(box.id())) {
            return box;
        }
        for (Box child : box.children()) {
            Box found = find(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** The margins predicted for the box {@code B} of a section with {@code body}, checked against its layout. */
    private static MarginStrut predicted(String body) {
        Box box = find(new BoxTreeBuilder().build(new DocumentReader().read(HEAD + body)).root(), "B");
        assertNotNull(box);
        Engine engine = new Engine();
        MarginStrut predicted = LeadingMargins.predict(box, SPACE, engine);
        assertEquals(engine.layout(box, SPACE, null).adjoining().top(), predicted, "the layout returns other margins");
        return predicted;
    }

    @Test
    void theTopMarginOfTheFirstDescendantComesOut() {
        assertEquals(new MarginStrut(24, 0), predicted("\t\tBlock:\n\t\t\tId: B\n\t\t\tHeading 1:\n\t\t\t\tText: Capítulo\n"
                + "\t\t\t\tBox:\n\t\t\t\t\tMargin: 24 0 12 0\n\t\t\tParagraph: Texto\n"));
    }

    @Test
    void aChainOfOpenBlocksMergesTheirMargins() {
        assertEquals(new MarginStrut(24, 0), predicted("\t\tBlock:\n\t\t\tId: B\n\t\t\tBlock:\n\t\t\t\tBox:\n"
                + "\t\t\t\t\tMargin: 5 0 0 0\n\t\t\t\tHeading 1:\n\t\t\t\t\tText: Capítulo\n\t\t\t\t\tBox:\n"
                + "\t\t\t\t\t\tMargin: 24 0 12 0\n\t\t\t\tParagraph: Texto\n"));
    }

    @Test
    void paddingAtTheTopKeepsTheMarginsInside() {
        assertEquals(MarginStrut.EMPTY, predicted("\t\tBlock:\n\t\t\tId: B\n\t\t\tBox:\n\t\t\t\tPadding: 2 0 0 0\n"
                + "\t\t\tHeading 1:\n\t\t\t\tText: Capítulo\n\t\t\t\tBox:\n\t\t\t\t\tMargin: 24 0 12 0\n"));
    }

    @Test
    void isolatingTheMarginsKeepsThemInside() {
        assertEquals(MarginStrut.EMPTY, predicted("\t\tBlock:\n\t\t\tId: B\n\t\t\tIsolate margins: true\n"
                + "\t\t\tHeading 1:\n\t\t\t\tText: Capítulo\n\t\t\t\tBox:\n\t\t\t\t\tMargin: 24 0 12 0\n"));
    }

    @Test
    void trimmingTheMarginsDropsThem() {
        assertEquals(MarginStrut.EMPTY, predicted("\t\tBlock:\n\t\t\tId: B\n\t\t\tTrim margins: both\n"
                + "\t\t\tHeading 1:\n\t\t\t\tText: Capítulo\n\t\t\t\tBox:\n\t\t\t\t\tMargin: 24 0 12 0\n"));
    }

    @Test
    void trimmingOnlyTheTopDropsThemToo() {
        assertEquals(MarginStrut.EMPTY, predicted("\t\tBlock:\n\t\t\tId: B\n\t\t\tTrim margins: top\n"
                + "\t\t\tHeading 1:\n\t\t\t\tText: Capítulo\n\t\t\t\tBox:\n\t\t\t\t\tMargin: 24 0 12 0\n"));
    }

    @Test
    void trimmingOnlyTheBottomLetsThoseOfTheTopOut() {
        assertEquals(new MarginStrut(24, 0), predicted("\t\tBlock:\n\t\t\tId: B\n\t\t\tTrim margins: bottom\n"
                + "\t\t\tHeading 1:\n\t\t\t\tText: Capítulo\n\t\t\t\tBox:\n\t\t\t\t\tMargin: 24 0 12 0\n"));
    }

    @Test
    void anEmptyChildLetsBothOfItsMarginsThrough() {
        assertEquals(new MarginStrut(10, -4), predicted("\t\tBlock:\n\t\t\tId: B\n\t\t\tBlock:\n\t\t\t\tBox:\n"
                + "\t\t\t\t\tMargin: 10 0 -4 0\n\t\t\tParagraph: Texto\n\t\t\t\tBox:\n\t\t\t\t\tMargin: 8 0 0 0\n"));
    }

    @Test
    void aSideBoxDoesNotTakePartInTheFlow() {
        assertEquals(new MarginStrut(6, 0), predicted("\t\tBlock:\n\t\t\tId: B\n\t\t\tBlock:\n\t\t\t\tSide: left\n"
                + "\t\t\t\tBox:\n\t\t\t\t\tWidth: 100\n\t\t\t\t\tMargin: 20 0 0 0\n\t\t\t\tText: Al lado\n"
                + "\t\t\tParagraph: Texto\n\t\t\t\tBox:\n\t\t\t\t\tMargin: 6 0 0 0\n"));
    }

    @Test
    void aTextHasNothingThatComesOut() {
        assertEquals(MarginStrut.EMPTY, predicted("\t\tParagraph: Texto\n\t\t\tId: B\n\t\t\tBox:\n"
                + "\t\t\t\tMargin: 6 0 0 0\n"));
    }
}
