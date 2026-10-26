package dev.stxt.boxes.fragmentos;

import dev.stxt.boxes.arbol.Box;
import dev.stxt.boxes.arbol.Medio;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.Motor;

import java.util.ArrayList;
import java.util.List;

/**
 * Bucle de paginación (módulo 8). En modo continuo el espacio no tiene límite, el
 * {@link BreakToken} vuelve siempre como {@code null} y el bucle da una sola vuelta:
 * el mismo motor sirve para los dos modos.
 */
public final class Paginator {

    /** Límite de seguridad ante errores de implementación en los algoritmos. */
    private static final int MAX_PAGINAS = 10_000;

    private final Motor motor;

    public Paginator(Motor motor) {
        this.motor = motor;
    }

    public List<Pagina> paginar(Box raiz, Medio medio) {
        List<Pagina> paginas = new ArrayList<>();
        BreakToken token = null;
        do {
            ConstraintSpace espacio = ConstraintSpace.paraPagina(medio);
            LayoutResult r = motor.layout(raiz, espacio, token);
            if (r.breakToken() != null && r.breakToken().equals(token)) {
                throw new IllegalStateException("Sin progreso en la página " + (paginas.size() + 1)
                        + ": se ha incumplido la garantía de progreso (ESPECIFICACION.md §5.10)");
            }
            // TODO(hito 3): resolver aquí las cajas fija de cada página (r.pendientes() y medio.areaUtil()).
            paginas.add(new Pagina(paginas.size() + 1, r.fragment(), List.of()));
            token = r.breakToken();
            if (paginas.size() > MAX_PAGINAS) {
                throw new IllegalStateException("Se ha superado el máximo de " + MAX_PAGINAS + " páginas");
            }
        } while (token != null);
        return paginas;
    }
}
