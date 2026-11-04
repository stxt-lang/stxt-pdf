package dev.stxt.pdf.layout.inline;

import dev.stxt.pdf.tree.Box;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The memorized line breaking of the texts of a document (reference/implementation/layout.md, "Long text"): each page
 * lays out a text from another line, so the cache of the {@link dev.stxt.pdf.layout.LayoutContext} does not help
 * and, without this memory, a text of N pages would be broken N times (quadratic cost). The line breaking of a text
 * depends on what it measures ({@link TextValues}): when that changes, the texts that change are forgotten
 * ({@link #forget}) and broken again the next time. By box first, so that forgetting a box does not go through the
 * others. It belongs to the {@link dev.stxt.pdf.layout.Engine}, which empties it with its cache;
 * {@link InlineLayout} fills it and reads it.
 */
public final class LineBreakingMemo {

    private final Map<Box, Map<InlineLayout.BrokenKey, InlineLayout.Broken>> broken = new IdentityHashMap<>();

    /** The line breaking of the text of the box with that key, made with {@code breaking} if it is the first time. */
    InlineLayout.Broken broken(Box box, InlineLayout.BrokenKey key, Supplier<InlineLayout.Broken> breaking) {
        return broken.computeIfAbsent(box, b -> new HashMap<>()).computeIfAbsent(key, k -> breaking.get());
    }

    /** Forgets the memorized line breaking of those boxes. */
    public void forget(Set<Box> boxes) {
        for (Box box : boxes) {
            broken.remove(box);
        }
    }

    /** Forgets everything: the document is laid out. */
    public void clear() {
        broken.clear();
    }

    /** The boxes with a memorized line breaking, to check what is kept and what is forgotten. */
    public int size() {
        return broken.size();
    }
}
