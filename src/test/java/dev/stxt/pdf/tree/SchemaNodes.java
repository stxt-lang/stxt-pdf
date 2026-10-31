package dev.stxt.pdf.tree;

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

/** The schema read as data, for the tests that compare it with the code. */
final class SchemaNodes {

    private SchemaNodes() {
    }

    /** The children that each node of the schema allows, by their name and in file order. */
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

    /** Canonical name, like {@code Node.getCanonicalName()}: {@code Min width} → {@code min-width}. */
    static String canonical(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private static String schema() {
        try (InputStream in = SchemaNodes.class.getResourceAsStream("/stxt/dev.stxt.pdf.stxt")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
