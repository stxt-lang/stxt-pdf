package dev.stxt.boxes.tree;

import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Listas al construir el árbol (ESPECIFICACION.md §5.14): elementos, numeración y reglas. */
class ListsTest {

    private static Box root(String... lines) {
        String doc = "Document (dev.stxt.boxes):\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n" + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(doc)).root();
    }

    private static List<String> markers(Box list) {
        return list.children.stream().map(b -> b.marker).toList();
    }

    @Test
    void listAndItemsAreBlocks() {
        Box list = root("\tList:", "\t\tMarker: disc", "\t\tList item:", "\t\t\tParagraph: Uno",
                "\t\tList item:", "\t\t\tText: Dos", "\t\t\tText: y más").children.get(0);
        assertEquals("list", list.element);
        assertEquals(Style.Layout.BLOCK, list.style.layout);
        assertEquals(Style.Marker.DISC, list.style.listMarker);
        assertEquals(2, list.children.size());
        Box item = list.children.get(1);
        assertEquals("list-item", item.element);
        assertEquals(Style.Layout.BLOCK, item.style.layout);
        assertEquals(2, item.children.size());
        assertEquals(List.of("• ", "• "), markers(list));
    }

    @Test
    void numberingStartsAtStartAndFollowsNumber() {
        Box list = root("\tList:", "\t\tMarker: decimal", "\t\tStart: 3",
                "\t\tList item:", "\t\t\tText: a",
                "\t\tList item:", "\t\t\tText: b",
                "\t\tList item:", "\t\t\tNumber: 10", "\t\t\tText: c",
                "\t\tList item:", "\t\t\tText: d").children.get(0);
        assertEquals(List.of("3. ", "4. ", "10. ", "11. "), markers(list));
    }

    @Test
    void markersWithAHyphen() {
        Box list = root("\tList:", "\t\tMarker: upper-roman", "\t\tStart: 4",
                "\t\tList item:", "\t\t\tText: a", "\t\tList item:", "\t\t\tText: b").children.get(0);
        assertEquals(Style.Marker.UPPER_ROMAN, list.style.listMarker);
        assertEquals(List.of("IV. ", "V. "), markers(list));
    }

    @Test
    void nestedListsNumberOnTheirOwn() {
        Box outer = root("\tList:", "\t\tMarker: decimal",
                "\t\tList item:", "\t\t\tText: a",
                "\t\t\tList:", "\t\t\t\tMarker: lower-alpha",
                "\t\t\t\tList item:", "\t\t\t\t\tText: a1",
                "\t\t\t\tList item:", "\t\t\t\t\tText: a2",
                "\t\tList item:", "\t\t\tText: b").children.get(0);
        assertEquals(List.of("1. ", "2. "), markers(outer));
        assertEquals(List.of("a. ", "b. "), markers(outer.children.get(0).children.get(1)));
    }

    @Test
    void noneHasNoMarkerAndNeedsNoLine() {
        Box list = root("\tList:", "\t\tMarker: none", "\t\tList item:", "\t\t\tFlex:",
                "\t\t\t\tItem:", "\t\t\t\t\tText: a").children.get(0);
        assertNull(list.children.get(0).marker);
    }

    @Test
    void theFirstLineMayBeDeepInsideBlocks() {
        Box list = root("\tList:", "\t\tMarker: dash",
                "\t\tList item:", "\t\t\tBlock:", "\t\t\t\tAbsolute:", "\t\t\t\t\tText: fuera",
                "\t\t\t\tRelative:", "\t\t\t\t\tText: dentro",
                "\t\tList item:", "\t\t\tList:", "\t\t\t\tMarker: disc",
                "\t\t\t\tList item:", "\t\t\t\t\tText: anidado").children.get(0);
        assertEquals(List.of("– ", "– "), markers(list));
    }

    @Test
    void rulesNameListsAndItems() {
        Box list = root("\tStyles:",
                "\t\tElement: List", "\t\t\tBox:", "\t\t\t\tPadding: 0 0 0 18",
                "\t\tElement: List item", "\t\t\tTypography:", "\t\t\t\tWeight: bold",
                "\tList:", "\t\tMarker: disc", "\t\tList item:", "\t\t\tText: a").children.get(0);
        assertEquals(18, ((dev.stxt.boxes.model.Value.Points) list.style.padding.left()).pt());
        assertEquals(Style.FontWeight.BOLD, list.children.get(0).style.fontWeight);
    }

    @Test
    void aListCanBeAFlexItem() {
        Box flex = root("\tFlex:", "\t\tItem:", "\t\t\tList:", "\t\t\t\tMarker: disc",
                "\t\t\t\tList item:", "\t\t\t\t\tText: a").children.get(0);
        assertEquals("list", flex.children.get(0).element);
    }
}
