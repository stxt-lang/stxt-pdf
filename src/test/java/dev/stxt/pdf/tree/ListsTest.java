package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Marker;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
    void numberingStartsAtStartAndFollowsNumber() {
        Box list = root("\tList:", "\t\tMarker: decimal", "\t\tStart: 3",
                "\t\tList item:", "\t\t\tText: a",
                "\t\tList item:", "\t\t\tText: b",
                "\t\tList item:", "\t\t\tNumber: 10", "\t\t\tText: c",
                "\t\tList item:", "\t\t\tText: d").children().get(0);
        assertEquals(List.of("3. ", "4. ", "10. ", "11. "), markers(list));
    }

    @Test
    void markersWithAHyphen() {
        Box list = root("\tList:", "\t\tMarker: upper-roman", "\t\tStart: 4",
                "\t\tList item:", "\t\t\tText: a", "\t\tList item:", "\t\t\tText: b").children().get(0);
        assertEquals(Marker.UPPER_ROMAN, list.style().list().marker());
        assertEquals(List.of("IV. ", "V. "), markers(list));
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
}
