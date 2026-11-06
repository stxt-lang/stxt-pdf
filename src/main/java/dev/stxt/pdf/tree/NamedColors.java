package dev.stxt.pdf.tree;

import static dev.stxt.pdf.tree.Nodes.child;
import static dev.stxt.pdf.tree.Nodes.error;
import static dev.stxt.pdf.tree.Nodes.inline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.stxt.InlineNode;
import dev.stxt.Node;
import dev.stxt.pdf.model.Colors;

/**
 * The colors with a name of {@code Styles} (reference/common/styles.md): {@code Named color: brand #12315A} declares
 * {@code brand} once, and from then on it is written wherever a color goes ({@code Color}, {@code Border color},
 * {@code Background}, also inside {@code linear(...)}, and {@code Separator color}). The name is replaced by its color
 * when the property is read, so the rest of the engine only sees {@code #...} colors.
 *
 * <p>They come from the same places as the rules: the separate styles documents and the {@code Styles} of the
 * document. The names of all of them form a single set, and if several declare a name, the color of the last one
 * wins (the styles documents in the order they are given, and the document after them), also in the rules of the
 * others. A repeated name is an error within one source, not between two.</p>
 */
final class NamedColors {

    /** Without names: a document without {@code Named color}. */
    static final NamedColors NONE = new NamedColors(Map.of());

    /** The color of each name, already validated. */
    private final Map<String, String> colors;

    private NamedColors(Map<String, String> colors) {
        this.colors = colors;
    }

    /**
     * Reads the {@code Named color} of the separate styles documents ({@code styles}, each with root {@code Styles},
     * in the order they were given: each one overrides those before it) and those of the {@code Styles} of the
     * document, which override them all.
     */
    static NamedColors read(InlineNode document, List<InlineNode> styles) {
        Map<String, String> colors = new HashMap<>();
        for (int i = 0; i < styles.size(); i++) {
            colors.putAll(load(styles.get(i), StyleRules.source(i, styles.size())));
        }
        child(document, "styles").map(Nodes::inline).ifPresent(own -> colors.putAll(load(own, null)));
        return colors.isEmpty() ? NONE : new NamedColors(Map.copyOf(colors));
    }

    /**
     * The names of one source; a repeated name is an error within it. {@code source} is the separate styles document
     * they are in, for the errors; {@code null} if they are of the document.
     */
    private static Map<String, String> load(InlineNode styles, SourceLine source) {
        Map<String, String> colors = new HashMap<>();
        Map<String, Node> seen = new HashMap<>();
        for (Node n : styles.getChildren()) {
            if (!n.getCanonicalName().equals("named-color")) {
                continue;
            }
            try {
                String[] parts = definition(inline(n));
                Node previous = seen.putIfAbsent(parts[0], n);
                if (previous != null) {
                    throw error(n, ErrorCode.COLOR_DUPLICATE, "Repeated Named color: " + parts[0] + " (already on line "
                            + previous.getLine() + ")");
                }
                colors.put(parts[0], parts[1]);
            } catch (InvalidModelException e) {
                throw source != null ? e.in(source) : e;
            }
        }
        return colors;
    }

    /** The name and the color of a {@code Named color}: {@code brand #12315A}. */
    private static String[] definition(InlineNode n) {
        String text = n.getText().trim();
        String[] parts = text.split("\\s+");
        if (parts.length != 2) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "Named color expects a name and a color separated by a space "
                    + "(brand #12315A): '" + text + "'");
        }
        if (!Colors.isName(parts[0])) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "The name of a Named color is written in lowercase letters and digits, "
                    + "starting with a letter, with words joined by hyphens (brand, dark-gray): '" + parts[0] + "'");
        }
        if (parts[0].equals("linear")) {
            throw error(n, ErrorCode.VALUE_NOT_ALLOWED, "linear cannot be the name of a Named color: it is the gradient "
                    + "of Background");
        }
        if (!Colors.isValid(parts[1])) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "The color of a Named color is written #RGB, #RGBA, #RRGGBB or "
                    + "#RRGGBBAA, not with another name: '" + parts[1] + "'");
        }
        return parts;
    }

    /**
     * The color that a property writes ({@code color}, a part of its value): the same text if it is a {@code #...}
     * color, or the color of its name. {@code where} completes the message of an invalid color ({@code ""} or
     * {@code " in linear"}).
     */
    String resolve(Node n, String color, String where) {
        if (Colors.isValid(color)) {
            return color;
        }
        if (!Colors.isName(color)) {
            throw error(n, ErrorCode.VALUE_SYNTAX, "Invalid color" + where + " (it is written #RGB, #RGBA, #RRGGBB, "
                    + "#RRGGBBAA or with the name of a Named color): " + color);
        }
        String named = colors.get(color);
        if (named == null) {
            throw error(n, ErrorCode.COLOR_UNKNOWN, "Unknown color: " + color + " (no Named color in Styles declares it)");
        }
        return named;
    }
}
