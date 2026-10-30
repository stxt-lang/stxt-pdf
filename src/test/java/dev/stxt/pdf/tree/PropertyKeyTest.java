package dev.stxt.pdf.tree;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.stxt.pdf.tree.SchemaNodes.canonical;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cada constante de {@link PropertyKey} es la clave de una propiedad que el schema admite en su sitio. */
class PropertyKeyTest {

    @Test
    void everyConstantIsAKeyOfTheSchema() throws IllegalAccessException {
        Set<String> keys = keys();
        int constants = 0;
        for (Field f : PropertyKey.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                String key = (String) f.get(null);
                assertTrue(keys.contains(key), () -> f.getName() + " = \"" + key + "\" no es una clave del schema");
                constants++;
            }
        }
        assertTrue(constants > 20);
    }

    @Test
    void shortNamesUseTheInternalName() {
        assertEquals(PropertyKey.FONT_SIZE, PropertyKey.internal("size"));
        assertEquals(PropertyKey.FONT_WEIGHT, PropertyKey.internal("bold"));
        assertEquals("break-inside", PropertyKey.internal("inside"));
        assertEquals(PropertyKey.LINE_HEIGHT, PropertyKey.internal("line-height"));
        assertEquals(PropertyKey.GRID_ROWS, PropertyKey.grid("row-sizes"));
        assertEquals(PropertyKey.ITEM_GROW, PropertyKey.item("grow"));
        assertEquals(PropertyKey.FLEX_GAP, PropertyKey.flex("gap"));
    }

    /** Las claves que puede guardar el builder: las propiedades, con su nombre interno o su prefijo. */
    private static Set<String> keys() {
        Map<String, List<String>> children = SchemaNodes.children();
        Set<String> keys = new HashSet<>();
        children.forEach((node, kids) -> {
            if (kids.isEmpty()) {
                keys.add(PropertyKey.internal(canonical(node)));
            }
        });
        for (String flex : children.get("Flex")) {
            if (children.get(flex).isEmpty()) {
                keys.add(PropertyKey.flex(canonical(flex)));
            }
        }
        for (String grid : children.get("Grid")) {
            if (children.get(grid).isEmpty()) {
                keys.add(PropertyKey.grid(canonical(grid)));
            }
        }
        for (String wrapper : List.of("Item", "Area")) {
            for (String item : children.get(wrapper)) {
                if (children.get(item).isEmpty()) {
                    keys.add(PropertyKey.item(canonical(item)));
                }
            }
        }
        // La colocación de un ítem de grid es el valor de Area, no un hijo.
        assertFalse(keys.contains(PropertyKey.ITEM_AREA));
        keys.add(PropertyKey.ITEM_AREA);
        return keys;
    }
}
