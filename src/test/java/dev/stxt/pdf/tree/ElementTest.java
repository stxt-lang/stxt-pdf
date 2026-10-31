package dev.stxt.pdf.tree;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.stxt.pdf.tree.SchemaNodes.canonical;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Element} and the schema say the same: the schema is the only definition of the structure,
 * so a new element in it fails here until it is added to the enum.
 */
class ElementTest {

    /**
     * The box elements of the schema are the nodes that allow {@code Style} and {@code Id}, except {@code Row}: it
     * allows {@code Style} and {@code Id} for its cells and its band, but it is not an element that is laid out.
     */
    @Test
    void writtenElementsAreTheBoxElementsOfTheSchema() {
        Set<String> schema = SchemaNodes.children().entrySet().stream()
                .filter(e -> e.getValue().contains("Style") && e.getValue().contains("Id"))
                .filter(e -> !e.getKey().equals("Row"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        assertEquals(schema, Element.written().stream().map(Element::label).collect(Collectors.toSet()));
    }

    /** The ones that can be an item are the elements that {@code Item} allows, and the same as {@code Area}. */
    @Test
    void itemElementsAreTheElementsOfItemAndArea() {
        Set<String> expected = Element.written().stream().filter(Element::canBeItem).map(Element::label)
                .collect(Collectors.toSet());
        for (String wrapper : List.of("Item", "Area")) {
            Set<String> elements = SchemaNodes.children().get(wrapper).stream()
                    .filter(c -> Element.of(canonical(c)).isPresent())
                    .collect(Collectors.toSet());
            assertEquals(expected, elements, wrapper);
        }
    }

    @Test
    void lookupByCanonicalName() {
        for (Element e : Element.written()) {
            assertEquals(Optional.of(e), Element.of(canonical(e.label())));
        }
        assertEquals(Optional.of(Element.LIST_ITEM), Element.of("list-item"));
        assertEquals(Optional.of(Element.HEADING_3), Element.of("heading-3"));
        // The marker is generated and Marker is a property of List: it is not an element.
        assertTrue(Element.of("marker").isEmpty());
        assertTrue(Element.of("item").isEmpty());
        // The column flow of a Columns is generated and has no name: "columns" is the Columns itself.
        assertEquals(Optional.of(Element.COLUMNS), Element.of("columns"));
        // Generated: the marker, the band of a row, the column flow, the code block of a Markdown and, from the table
        // of contents, six entries, the title, the leader and the page number.
        assertEquals(Element.values().length - 13, Element.written().size());
        // The code block is named by For element rules, but it is not written.
        assertEquals(Optional.of(Element.CODE_BLOCK), Element.of("code-block"));
        assertTrue(Element.selectable().contains(Element.CODE_BLOCK));
        assertTrue(!Element.written().contains(Element.CODE_BLOCK));
        assertTrue(Element.CODE_BLOCK.hasText());
        // The table of contents entries are named by For element rules, but they are not written.
        assertEquals(Optional.of(Element.TOC_2), Element.of("toc-2"));
        assertEquals(Element.TOC_4, Element.tocEntry(4));
        assertTrue(Element.of("contents-title").isEmpty());
    }

    @Test
    void headingLevels() {
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6), List.of(Element.PARAGRAPH, Element.HEADING_1, Element.HEADING_2,
                Element.HEADING_3, Element.HEADING_4, Element.HEADING_5, Element.HEADING_6).stream()
                .map(Element::headingLevel).toList());
    }
}
