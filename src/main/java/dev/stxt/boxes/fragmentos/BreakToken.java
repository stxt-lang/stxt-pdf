package dev.stxt.boxes.fragmentos;

import java.util.List;

/**
 * Dónde se cortó una caja y desde dónde hay que reanudar su layout en el siguiente
 * fragmentainer (módulo 8). Un {@code null} en su lugar significa "la caja ha terminado".
 *
 * <p>Son {@code record}: tienen {@code equals}, y por eso pueden formar parte de la clave de la
 * caché de layout y el paginador puede detectar que no hay progreso.</p>
 */
public sealed interface BreakToken {

    /** Bloque: siguiente hijo a colocar y, si el corte fue dentro de él, su propio token. */
    record Bloque(int siguienteHijo, BreakToken tokenHijo, double altoConsumido) implements BreakToken {
    }

    /** Texto: siguiente línea a colocar. */
    record Linea(int siguienteLinea) implements BreakToken {
    }

    /** Flex o grid: siguiente línea o fila y, por ítem, el token del corte en paralelo. */
    record Contenedor(int siguienteLinea, List<BreakToken> tokensItems) implements BreakToken {
        public Contenedor {
            tokensItems = List.copyOf(tokensItems);
        }
    }
}
