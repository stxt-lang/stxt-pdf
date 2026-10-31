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

/** Each constant of {@link PropertyKey} is the key of a property that the schema allows in its place. */
class PropertyKeyTest {

    @Test
    void everyConstantIsAKeyOfTheSchema() throws IllegalAccessException {
        Set<String> keys = keys();
        int constants = 0;
        for (Field f : PropertyKey.class.getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                String key = (String) f.get(null);
                assertTrue(keys.contains(key), () -> f.getName() + " = \"" + key + "\" is not a key of the schema");
                constants++;
            }
        }
        assertTrue(constants > 20);
    }

    @Test
    void shortNamesUseTheInternalName() {
        assertEquals(PropertyKey.FONT_SIZE, PropertyKey.internal("size"));
        assertEquals(PropertyKey.FONT_WEIGHT, PropertyKey.internal("bold"));
        assertEquals(PropertyKey.BREAK_AFTER, PropertyKey.internal("after"));
        assertEquals(PropertyKey.LINE_HEIGHT, PropertyKey.internal("line-height"));
        assertEquals(PropertyKey.GRID_ROWS, PropertyKey.grid("row-sizes"));
        assertEquals(PropertyKey.ITEM_GROW, PropertyKey.item("grow"));
        assertEquals(PropertyKey.STACK_GAP, PropertyKey.stack("gap"));
    }

    /** The keys that the builder can store: the properties, with their internal name or their prefix. */
    private static Set<String> keys() {
        Map<String, List<String>> children = SchemaNodes.children();
        Set<String> keys = new HashSet<>();
        children.forEach((node, kids) -> {
            if (kids.isEmpty()) {
                keys.add(PropertyKey.internal(canonical(node)));
            }
        });
        for (String stack : children.get("Stack")) {
            if (children.get(stack).isEmpty()) {
                keys.add(PropertyKey.stack(canonical(stack)));
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
        // The placement of a grid item is the value of Area, not a child.
        assertFalse(keys.contains(PropertyKey.ITEM_AREA));
        keys.add(PropertyKey.ITEM_AREA);
        return keys;
    }
}
