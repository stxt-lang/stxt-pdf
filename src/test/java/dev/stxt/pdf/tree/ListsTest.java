package dev.stxt.pdf.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.model.MarkerSuffix;
import dev.stxt.pdf.stxt.DocumentReader;

/** Lists when building the tree (reference/components/list.md): elements, numbering and rules. */
class ListsTest {

    private static Box root(String... lines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n" + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(doc)))).sections().get(0).box();
    }

    private static List<String> markers(Box list) {
        return list.children().stream().map(b -> b.marker() == null ? null : b.marker().text()).toList();
    }

    @Test
    void listAndItemsAreBlocks() {
        Box list = root("\tList:", "\t\tMarker: bullet", "\t\tList item:", "\t\t\tParagraph: Uno",
                "\t\tList item:", "\t\t\tText: Dos", "\t\t\tText: y más").children().get(0);
        assertEquals(Element.LIST, list.element());
        assertEquals(Layout.BLOCK, list.style().layout());
        assertEquals(Marker.BULLET, list.style().list().marker());
        assertEquals(2, list.children().size());
        Box item = list.children().get(1);
        assertEquals(Element.LIST_ITEM, item.element());
        assertEquals(Layout.BLOCK, item.style().layout());
        assertEquals(2, item.children().size());
        assertEquals(List.of("• ", "• "), markers(list));
    }

    @Test
    void numberingStartsAtFirstNumberAndFollowsNumber() {
        Box list = root("\tList:", "\t\tMarker: decimal", "\t\tFirst number: 3",
                "\t\tList item:", "\t\t\tText: a",
                "\t\tList item:", "\t\t\tText: b",
                "\t\tList item:", "\t\t\tItem number: 10", "\t\t\tText: c",
                "\t\tList item:", "\t\t\tText: d").children().get(0);
        assertEquals(List.of("3. ", "4. ", "10. ", "11. "), markers(list));
    }

    @Test
    void markersWithAHyphen() {
        Box list = root("\tList:", "\t\tMarker: upper-roman", "\t\tFirst number: 4",
                "\t\tList item:", "\t\t\tText: a", "\t\tList item:", "\t\t\tText: b").children().get(0);
        assertEquals(Marker.UPPER_ROMAN, list.style().list().marker());
        assertEquals(List.of("IV. ", "V. "), markers(list));
    }

    @Test
    void theMarkerSuffixIsOfItsListAndNotOfTheNestedOnes() {
        Box outer = root("\tList:", "\t\tMarker: lower-alpha", "\t\tMarker suffix: parenthesis",
                "\t\tList item:", "\t\t\tId: clause", "\t\t\tText: a",
                "\t\t\tList:", "\t\t\t\tMarker: decimal",
                "\t\t\t\tList item:", "\t\t\t\t\tText: a1",
                "\t\tList item:", "\t\t\tText: b").children().get(0);
        assertEquals(MarkerSuffix.PARENTHESIS, outer.style().list().markerSuffix());
        assertEquals(List.of("a) ", "b) "), markers(outer));
        // What {number of: id} gives has no suffix.
        assertEquals("a", outer.children().get(0).number());
        Box inner = outer.children().get(0).children().get(1);
        assertEquals(MarkerSuffix.PERIOD, inner.style().list().markerSuffix());
        assertEquals(List.of("1. "), markers(inner));
    }

    @Test
    void theMarkerSuffixIsOnlyOfANumbering() {
        for (String marker : List.of("bullet", "dash", "none")) {
            InvalidModelException e = error("\tList:", "\t\tMarker: " + marker, "\t\tMarker suffix: parenthesis",
                    "\t\tList item:", "\t\t\tText: a");
            assertEquals(ErrorCode.LIST_SUFFIX_NOT_ALLOWED, e.errorCode());
            assertEquals(15, e.line());
        }
        assertEquals(ErrorCode.LIST_SUFFIX_NOT_ALLOWED, error("\tList:", "\t\tMarker: image",
                "\t\tMarker suffix: period", "\t\tMarker image: test/check.svg", "\t\tList item:", "\t\t\tText: a")
                .errorCode());
    }

    @Test
    void nestedListsNumberOnTheirOwn() {
        Box outer = root("\tList:", "\t\tMarker: decimal",
                "\t\tList item:", "\t\t\tText: a",
                "\t\t\tList:", "\t\t\t\tMarker: lower-alpha",
                "\t\t\t\tList item:", "\t\t\t\t\tText: a1",
                "\t\t\t\tList item:", "\t\t\t\t\tText: a2",
                "\t\tList item:", "\t\t\tText: b").children().get(0);
        assertEquals(List.of("1. ", "2. "), markers(outer));
        assertEquals(List.of("a. ", "b. "), markers(outer.children().get(0).children().get(1)));
    }

    @Test
    void noneHasNoMarkerAndNeedsNoLine() {
        Box list = root("\tList:", "\t\tMarker: none", "\t\tList item:", "\t\t\tStack:",
                "\t\t\t\tItem:", "\t\t\t\t\tText: a").children().get(0);
        assertNull(list.children().get(0).marker());
    }

    @Test
    void theFirstLineMayBeDeepInsideBlocks() {
        Box list = root("\tList:", "\t\tMarker: dash",
                "\t\tList item:", "\t\t\tBlock:", "\t\t\t\tOverlay:", "\t\t\t\t\tText: fuera",
                "\t\t\t\tFrame:", "\t\t\t\t\tText: dentro",
                "\t\tList item:", "\t\t\tList:", "\t\t\t\tMarker: bullet",
                "\t\t\t\tList item:", "\t\t\t\t\tText: anidado").children().get(0);
        assertEquals(List.of("– ", "– "), markers(list));
    }

    @Test
    void rulesNameListsAndItems() {
        Box list = root("\tStyles:",
                "\t\tFor element: List", "\t\t\tBox:", "\t\t\t\tPadding: 0 0 0 18",
                "\t\tFor element: List item", "\t\t\tTypography:", "\t\t\t\tBold: true",
                "\tList:", "\t\tMarker: bullet", "\t\tList item:", "\t\t\tText: a").children().get(0);
        assertEquals(18, ((dev.stxt.pdf.model.Value.Points) list.style().box().padding().left()).pt());
        assertEquals(FontWeight.BOLD, list.children().get(0).style().typography().fontWeight());
    }

    @Test
    void aListCanBeAStackItem() {
        Box stack = root("\tStack:", "\t\tItem:", "\t\t\tList:", "\t\t\t\tMarker: bullet",
                "\t\t\t\tList item:", "\t\t\t\t\tText: a").children().get(0);
        assertEquals(Element.LIST, stack.children().get(0).element());
    }

    private static InvalidModelException error(String... lines) {
        return assertThrows(InvalidModelException.class, () -> root(lines));
    }

    @Test
    void anImageMarkerIsAnImageBoxWithTheMarkerImageOfTheList() {
        Box list = root("\tList:", "\t\tMarker: image", "\t\tMarker image: test/check.svg",
                "\t\tList item:", "\t\t\tText: a", "\t\tList item:", "\t\t\tText: b").children().get(0);
        assertEquals(Marker.IMAGE, list.style().list().marker());
        for (Box item : list.children()) {
            Box marker = item.marker();
            assertTrue(marker.isMarker());
            assertTrue(marker.isImage());
            assertEquals("test/check.svg", marker.image());
            assertNull(marker.text());
            // Decorative: without Alt, and without Height, it takes the Size of the item.
            assertNull(marker.style().image().alt());
            assertNull(marker.style().list().markerHeight());
            assertEquals(Layout.INLINE, marker.style().layout());
            // The line of the Marker image, for the errors of the file.
            assertEquals(15, marker.line());
        }
        assertNull(list.children().get(0).number());
    }

    @Test
    void theMarkerImageOfAnItemReplacesThatOfTheListWhole() {
        Box list = root("\tList:", "\t\tMarker: image", "\t\tMarker image: test/check.svg", "\t\t\tAlt: Incluido",
                "\t\t\tHeight: 8",
                "\t\tList item:", "\t\t\tMarker image: test/cross.svg", "\t\t\t\tAlt: Excluido", "\t\t\tText: a",
                "\t\tList item:", "\t\t\tMarker image: test/square.png", "\t\t\tText: b",
                "\t\tList item:", "\t\t\tText: c").children().get(0);
        Box own = list.children().get(0).marker();
        assertEquals("test/cross.svg", own.image());
        assertEquals("Excluido", own.style().image().alt());
        assertNull(own.style().list().markerHeight());
        assertEquals(19, own.line());
        Box plain = list.children().get(1).marker();
        assertEquals("test/square.png", plain.image());
        assertNull(plain.style().image().alt());
        Box inherited = list.children().get(2).marker();
        assertEquals("test/check.svg", inherited.image());
        assertEquals("Incluido", inherited.style().image().alt());
        assertEquals(8, inherited.style().list().markerHeight());
    }

    @Test
    void markerImageNeedsMarkerImage() {
        InvalidModelException list = error("\tList:", "\t\tMarker: bullet", "\t\tMarker image: test/check.svg",
                "\t\tList item:", "\t\t\tText: a");
        assertEquals(ErrorCode.LIST_IMAGE_NOT_ALLOWED, list.errorCode());
        assertEquals(15, list.line());
        InvalidModelException item = error("\tList:", "\t\tMarker: decimal",
                "\t\tList item:", "\t\t\tMarker image: test/check.svg", "\t\t\tText: a");
        assertEquals(ErrorCode.LIST_IMAGE_NOT_ALLOWED, item.errorCode());
        assertEquals(16, item.line());
    }

    @Test
    void anItemOfAnImageListNeedsSomeMarkerImage() {
        InvalidModelException e = error("\tList:", "\t\tMarker: image",
                "\t\tList item:", "\t\t\tMarker image: test/check.svg", "\t\t\tText: a",
                "\t\tList item:", "\t\t\tText: b");
        assertEquals(ErrorCode.LIST_IMAGE_MISSING, e.errorCode());
        assertEquals(18, e.line());
    }

    @Test
    void markerImageNeedsAPathAndAPositiveHeight() {
        assertEquals(ErrorCode.IMAGE_UNKNOWN, error("\tList:", "\t\tMarker: image", "\t\tMarker image:",
                "\t\tList item:", "\t\t\tText: a").errorCode());
        InvalidModelException height = error("\tList:", "\t\tMarker: image", "\t\tMarker image: test/check.svg",
                "\t\t\tHeight: 50%", "\t\tList item:", "\t\t\tText: a");
        assertEquals(ErrorCode.VALUE_NOT_ALLOWED, height.errorCode());
        assertEquals(16, height.line());
    }

    @Test
    void anImageListDoesNotNumber() {
        assertEquals(ErrorCode.LIST_NUMBER_NOT_ALLOWED, error("\tList:", "\t\tMarker: image", "\t\tFirst number: 2",
                "\t\tMarker image: test/check.svg", "\t\tList item:", "\t\t\tText: a").errorCode());
    }

    @Test
    void aDrawnMarkerIsAShapeBoxWithoutText() {
        Box list = root("\tList:", "\t\tMarker: circle", "\t\tList item:", "\t\t\tText: a",
                "\t\t\tList:", "\t\t\t\tMarker: square", "\t\t\t\tList item:", "\t\t\t\t\tText: b",
                "\t\t\tList:", "\t\t\t\tMarker: bullet", "\t\t\t\tList item:", "\t\t\t\t\tText: c").children().get(0);
        Box item = list.children().get(0);
        Box circle = item.marker();
        assertTrue(circle.isMarkerShape());
        assertFalse(circle.hasText());
        assertFalse(circle.isImage());
        assertEquals(Marker.CIRCLE, circle.style().list().marker());
        // The shape belongs to the list of the item, not to the lists inside it.
        Box square = item.children().get(1).children().get(0).marker();
        assertEquals(Marker.SQUARE, square.style().list().marker());
        assertFalse(square.style().list().checked());
        // A marker of the font is text, not a shape.
        Box bullet = item.children().get(2).children().get(0).marker();
        assertFalse(bullet.isMarkerShape());
        assertEquals("• ", bullet.text());
    }

    @Test
    void aCheckboxIsCheckedInTheItemsThatDeclareIt() {
        Box list = root("\tList:", "\t\tMarker: checkbox",
                "\t\tList item:", "\t\t\tChecked: true", "\t\t\tText: a",
                "\t\tList item:", "\t\t\tText: b",
                "\t\tList item:", "\t\t\tChecked: false", "\t\t\tText: c").children().get(0);
        assertEquals(List.of(true, false, false),
                list.children().stream().map(i -> i.marker().style().list().checked()).toList());
        assertEquals(List.of(Marker.CHECKBOX, Marker.CHECKBOX, Marker.CHECKBOX),
                list.children().stream().map(i -> i.marker().style().list().marker()).toList());
    }

    @Test
    void checkedIsOnlyOfAnItemOfACheckboxList() {
        InvalidModelException e = error("\tList:", "\t\tMarker: square",
                "\t\tList item:", "\t\t\tChecked: false", "\t\t\tText: a");
        assertEquals(ErrorCode.LIST_CHECKED_NOT_ALLOWED, e.errorCode());
        assertEquals(16, e.line());
        assertTrue(e.getMessage().contains("Marker: square"), e.getMessage());
    }

    @Test
    void aDrawnMarkerNeedsALineAndDoesNotNumber() {
        assertEquals(ErrorCode.LIST_ITEM_WITHOUT_LINE, error("\tList:", "\t\tMarker: checkbox",
                "\t\tList item:", "\t\t\tImage: test/square.png").errorCode());
        assertEquals(ErrorCode.LIST_NUMBER_NOT_ALLOWED, error("\tList:", "\t\tMarker: circle", "\t\tFirst number: 2",
                "\t\tList item:", "\t\t\tText: a").errorCode());
        assertEquals(ErrorCode.LIST_SUFFIX_NOT_ALLOWED, error("\tList:", "\t\tMarker: checkbox",
                "\t\tMarker suffix: parenthesis", "\t\tList item:", "\t\t\tText: a").errorCode());
    }
}
