package dev.stxt.boxes.tree;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.runtime.STXT;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** El schema leído como datos, para los tests que lo comparan con el código. */
final class SchemaNodes {

    private SchemaNodes() {
    }

    /** Los hijos que admite cada nodo del schema, por su nombre y en el orden del fichero. */
    static Map<String, List<String>> children() {
        Map<String, List<String>> children = new LinkedHashMap<>();
        for (Node n : ((InlineNode) STXT.rawParser().parse(schema()).get(0)).getChildren()) {
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

    /** Nombre canónico, como {@code Node.getCanonicalName()}: {@code Min width} → {@code min-width}. */
    static String canonical(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private static String schema() {
        try (InputStream in = SchemaNodes.class.getResourceAsStream("/stxt/dev.stxt.boxes.stxt")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
