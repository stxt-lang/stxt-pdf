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
 * {@link Element} y el schema dicen lo mismo: el schema es la única definición de la estructura,
 * así que un elemento nuevo en él falla aquí hasta que se añade al enum.
 */
class ElementTest {

    /**
     * Los elementos de caja del schema son los nodos que admiten {@code Class} e {@code Id}: una
     * {@code Row} admite {@code Class} para sus celdas, pero no es una caja.
     */
    @Test
    void writtenElementsAreTheBoxElementsOfTheSchema() {
        Set<String> schema = SchemaNodes.children().entrySet().stream()
                .filter(e -> e.getValue().contains("Class") && e.getValue().contains("Id"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        assertEquals(schema, Element.written().stream().map(Element::label).collect(Collectors.toSet()));
    }

    /** Los que pueden ser un ítem son los elementos que admite {@code Item}, y los mismos que {@code Area}. */
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
        // El marcador es generado y Marker es una propiedad de List: no es un elemento.
        assertTrue(Element.of("marker").isEmpty());
        assertTrue(Element.of("item").isEmpty());
        // Ni el flujo de columnas de un Multicol, que también es generado.
        assertTrue(Element.of("columns").isEmpty());
        // Generados: el marcador, el flujo de columnas, el bloque de código de un Markdown y, del índice,
        // seis entradas, el título, la guía y el número de página.
        assertEquals(Element.values().length - 12, Element.written().size());
        // El bloque de código lo nombran las reglas Element, pero no se escribe.
        assertEquals(Optional.of(Element.CODE_BLOCK), Element.of("code-block"));
        assertTrue(Element.selectable().contains(Element.CODE_BLOCK));
        assertTrue(!Element.written().contains(Element.CODE_BLOCK));
        assertTrue(Element.CODE_BLOCK.hasText());
        // Las entradas del índice las nombran las reglas Element, pero no se escriben.
        assertEquals(Optional.of(Element.CONTENTS_ENTRY_2), Element.of("contents-entry-2"));
        assertEquals(Element.CONTENTS_ENTRY_4, Element.contentsEntry(4));
        assertTrue(Element.of("contents-title").isEmpty());
    }

    @Test
    void headingLevels() {
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6), List.of(Element.PARAGRAPH, Element.HEADING_1, Element.HEADING_2,
                Element.HEADING_3, Element.HEADING_4, Element.HEADING_5, Element.HEADING_6).stream()
                .map(Element::headingLevel).toList());
    }
}
