package dev.stxt.boxes.layout;

import dev.stxt.boxes.arbol.Box;
import dev.stxt.boxes.fragmentos.BreakToken;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Caché de medidas y layouts (módulo 7). Sin ella, flex y grid anidados hacen un número de
 * layouts que crece exponencialmente con la profundidad.
 *
 * <p>No se usa {@code computeIfAbsent} porque el cálculo es recursivo y vuelve a escribir en el
 * mismo mapa, lo que {@link HashMap} no admite.</p>
 */
public final class LayoutCache {

    private record Clave(Box caja, ConstraintSpace espacio, BreakToken desde) {
    }

    private final Map<Box, IntrinsicSizes> intrinsecos = new IdentityHashMap<>();
    private final Map<Clave, LayoutResult> layouts = new HashMap<>();
    private int aciertos;
    private int fallos;

    public IntrinsicSizes intrinsic(Box caja, Supplier<IntrinsicSizes> calculo) {
        IntrinsicSizes r = intrinsecos.get(caja);
        if (r == null) {
            r = calculo.get();
            intrinsecos.put(caja, r);
        }
        return r;
    }

    public LayoutResult layout(Box caja, ConstraintSpace espacio, BreakToken desde,
                               Supplier<LayoutResult> calculo) {
        Clave clave = new Clave(caja, espacio, desde);
        LayoutResult r = layouts.get(clave);
        if (r != null) {
            aciertos++;
            return r;
        }
        fallos++;
        r = calculo.get();
        layouts.put(clave, r);
        return r;
    }

    public void vaciar() {
        intrinsecos.clear();
        layouts.clear();
    }

    /** Para diagnosticar la eficacia de la caché en los tests de anidamiento profundo. */
    public String estadisticas() {
        return "aciertos=" + aciertos + " fallos=" + fallos;
    }
}
