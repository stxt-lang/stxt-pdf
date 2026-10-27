package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.runtime.STXT;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Element} y el schema dicen lo mismo: el schema es la única definición de la estructura,
 * así que un elemento nuevo en él falla aquí hasta que se añade al enum.
 */
class ElementTest {

    /** Los elementos de caja del schema son los nodos que admiten {@code Class}. */
    @Test
    void writtenElementsAreTheBoxElementsOfTheSchema() throws IOException {
        Set<String> schema = children().entrySet().stream()
                .filter(e -> e.getValue().contains("Class"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        assertEquals(schema, Element.written().stream().map(Element::label).collect(Collectors.toSet()));
    }

    /** Los que pueden ser un ítem son los elementos que admite {@code Item}, y los mismos que {@code Area}. */
    @Test
    void itemElementsAreTheElementsOfItemAndArea() throws IOException {
        Set<String> expected = Element.written().stream().filter(Element::canBeItem).map(Element::label)
                .collect(Collectors.toSet());
        for (String wrapper : List.of("Item", "Area")) {
            Set<String> elements = children().get(wrapper).stream()
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
        // El marcador es generado y Marker es una propiedad de List: no es un elemento.
        assertTrue(Element.of("marker").isEmpty());
        assertTrue(Element.of("item").isEmpty());
        assertEquals(Element.values().length - 1, Element.written().size());
    }

    @Test
    void headingLevels() {
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6), List.of(Element.PARAGRAPH, Element.HEADING_1, Element.HEADING_2,
                Element.HEADING_3, Element.HEADING_4, Element.HEADING_5, Element.HEADING_6).stream()
                .map(Element::headingLevel).toList());
    }

    private static String canonical(String label) {
        return label.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    /** Los hijos que admite cada nodo del schema, por su nombre. */
    private static Map<String, List<String>> children() throws IOException {
        String schema;
        try (InputStream in = ElementTest.class.getResourceAsStream("/stxt/dev.stxt.boxes.stxt")) {
            schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String, List<String>> children = new LinkedHashMap<>();
        for (Node n : ((InlineNode) STXT.rawParser().parse(schema).get(0)).getChildren()) {
            if (n.getCanonicalName().equals("node")) {
                children.put(n.getText().trim(), ((InlineNode) n).getChildren().stream()
                        .filter(c -> c.getCanonicalName().equals("children"))
                        .flatMap(c -> ((InlineNode) c).getChildren().stream())
                        .map(c -> c.getText().trim())
                        .toList());
            }
        }
        return children;
    }
}
