package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.tree.Box;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Caché de medidas y layouts (reference/common/box.md). Sin ella, stack y grid anidados hacen un número de
 * layouts que crece exponencialmente con la profundidad.
 *
 * <p>No se usa {@code computeIfAbsent} porque el cálculo es recursivo y vuelve a escribir en el
 * mismo mapa, lo que {@link HashMap} no admite.</p>
 */
public final class LayoutCache {

    private record Key(Box box, ConstraintSpace space, BreakToken from) {
    }

    private final Map<Box, IntrinsicSizes> intrinsics = new IdentityHashMap<>();
    private final Map<Box, IntrinsicSizes> contents = new IdentityHashMap<>();
    private final Map<Key, LayoutResult> layouts = new HashMap<>();
    private int hits;
    private int misses;

    /** Tamaños intrínsecos de la caja de margen. */
    public IntrinsicSizes intrinsic(Box box, Supplier<IntrinsicSizes> computation) {
        return cached(intrinsics, box, computation);
    }

    /** Tamaños intrínsecos del contenido. */
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
     * Olvida lo que se guardó de esas cajas: sus tamaños intrínsecos y todos sus layouts. Lo de las demás sigue
     * valiendo, porque el resultado de una caja solo depende de su subárbol y de la clave.
     */
    public void forget(Set<Box> boxes) {
        intrinsics.keySet().removeIf(boxes::contains);
        contents.keySet().removeIf(boxes::contains);
        layouts.keySet().removeIf(k -> boxes.contains(k.box()));
    }

    /** Layouts calculados porque no estaban guardados. */
    int misses() {
        return misses;
    }

    /** Cuántos resultados guarda ahora mismo. */
    int entries() {
        return intrinsics.size() + contents.size() + layouts.size();
    }

    /**
     * Para diagnosticar la eficacia de la caché ({@link Engine#toString}). Lo que la protege es
     * {@code LayoutCacheTest}, que mide el trabajo de documentos muy anidados.
     */
    public String stats() {
        return "hits=" + hits + " misses=" + misses;
    }
}
