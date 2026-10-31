package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.tree.Box;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Cache of measurements and layouts (reference/common/box.md). Without it, nested stack and grid perform a number
 * of layouts that grows exponentially with the depth.
 *
 * <p>{@code computeIfAbsent} is not used because the computation is recursive and writes again to the
 * same map, which {@link HashMap} does not allow.</p>
 */
public final class LayoutCache {

    private record Key(Box box, ConstraintSpace space, BreakToken from) {
    }

    private final Map<Box, IntrinsicSizes> intrinsics = new IdentityHashMap<>();
    private final Map<Box, IntrinsicSizes> contents = new IdentityHashMap<>();
    private final Map<Key, LayoutResult> layouts = new HashMap<>();
    private int hits;
    private int misses;

    /** Intrinsic sizes of the margin box. */
    public IntrinsicSizes intrinsic(Box box, Supplier<IntrinsicSizes> computation) {
        return cached(intrinsics, box, computation);
    }

    /** Intrinsic sizes of the content. */
    public IntrinsicSizes contentIntrinsic(Box box, Supplier<IntrinsicSizes> computation) {
        return cached(contents, box, computation);
    }

    private static IntrinsicSizes cached(Map<Box, IntrinsicSizes> map, Box box, Supplier<IntrinsicSizes> computation) {
        IntrinsicSizes r = map.get(box);
        if (r == null) {
            r = computation.get();
            map.put(box, r);
        }
        return r;
    }

    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from,
                               Supplier<LayoutResult> computation) {
        Key key = new Key(box, space, from);
        LayoutResult r = layouts.get(key);
        if (r != null) {
            hits++;
            return r;
        }
        misses++;
        r = computation.get();
        layouts.put(key, r);
        return r;
    }

    public void clear() {
        intrinsics.clear();
        contents.clear();
        layouts.clear();
    }

    /**
     * Forgets what was stored for those boxes: their intrinsic sizes and all their layouts. What belongs to the
     * others is still valid, because the result of a box only depends on its subtree and on the key.
     */
    public void forget(Set<Box> boxes) {
        intrinsics.keySet().removeIf(boxes::contains);
        contents.keySet().removeIf(boxes::contains);
        layouts.keySet().removeIf(k -> boxes.contains(k.box()));
    }

    /** Layouts computed because they were not stored. */
    int misses() {
        return misses;
    }

    /** How many results it stores right now. */
    int entries() {
        return intrinsics.size() + contents.size() + layouts.size();
    }

    /**
     * To diagnose the effectiveness of the cache ({@link Engine#toString}). What protects it is
     * {@code LayoutCacheTest}, which measures the work of deeply nested documents.
     */
    public String stats() {
        return "hits=" + hits + " misses=" + misses;
    }
}
