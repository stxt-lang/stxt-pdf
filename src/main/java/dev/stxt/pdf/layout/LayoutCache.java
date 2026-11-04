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
 * of layouts that grows exponentially with the depth. It also memoizes what a stack, a grid or a table arranges before
 * breaking it between its units ({@link #arranged}), which does not depend on the page: without that, every fragment
 * of a long table placed and sized the whole table again.
 *
 * <p>{@code computeIfAbsent} is not used because the computation is recursive and writes again to the
 * same map, which {@link HashMap} does not allow.</p>
 */
public final class LayoutCache {

    private record Key(Box box, ConstraintSpace space, BreakToken from) {
    }

    /** The container, its space without fragmentation and the type of what its algorithm memoizes. */
    private record ArrangedKey(Box box, ConstraintSpace space, Class<?> type) {
    }

    private final Map<Box, IntrinsicSizes> intrinsics = new IdentityHashMap<>();
    private final Map<Box, IntrinsicSizes> contents = new IdentityHashMap<>();
    private final Map<Key, LayoutResult> layouts = new HashMap<>();
    private final Map<ArrangedKey, Object> arrangements = new HashMap<>();
    private int hits;
    private int misses;
    private int arranged;

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

    /**
     * What a container computes before breaking it between its units ({@link LayoutContext#arranged}), by container,
     * space without fragmentation and type: the first call with a key computes it and the following ones get the same
     * object, so it has to be immutable. The type is part of the key because a table memoizes its own preparation on
     * top of the arrangement of its grid, both for the same box.
     */
    public <T> T arranged(Box box, ConstraintSpace space, Class<T> type, Supplier<T> computation) {
        ArrangedKey key = new ArrangedKey(box, space, type);
        Object r = arrangements.get(key);
        if (r == null) {
            arranged++;
            r = computation.get();
            arrangements.put(key, r);
        }
        return type.cast(r);
    }

    public void clear() {
        intrinsics.clear();
        contents.clear();
        layouts.clear();
        arrangements.clear();
    }

    /**
     * Forgets what was stored for those boxes: their intrinsic sizes, all their layouts and their arrangements. What
     * belongs to the others is still valid, because the result of a box only depends on its subtree and on the key.
     */
    public void forget(Set<Box> boxes) {
        intrinsics.keySet().removeIf(boxes::contains);
        contents.keySet().removeIf(boxes::contains);
        layouts.keySet().removeIf(k -> boxes.contains(k.box()));
        arrangements.keySet().removeIf(k -> boxes.contains(k.box()));
    }

    /** Layouts computed because they were not stored. */
    int misses() {
        return misses;
    }

    /** Arrangements computed because they were not stored ({@link #arranged}). */
    int arrangements() {
        return arranged;
    }

    /** How many results it stores right now. */
    int entries() {
        return intrinsics.size() + contents.size() + layouts.size() + arrangements.size();
    }

    /**
     * To diagnose the effectiveness of the cache ({@link Engine#toString}). What protects it is
     * {@code LayoutCacheTest}, which measures the work of deeply nested documents, and {@code ArrangementCacheTest},
     * which counts the arrangements of a long container.
     */
    public String stats() {
        return "hits=" + hits + " misses=" + misses + " arranged=" + arranged;
    }
}
