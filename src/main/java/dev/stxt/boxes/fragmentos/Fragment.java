package dev.stxt.boxes.fragmentos;

import dev.stxt.boxes.arbol.Box;
import dev.stxt.boxes.modelo.Insets;

import java.util.List;

/**
 * El trozo de una caja que cae en un fragmentainer (módulo 8). En modo continuo cada caja
 * produce un único fragmento; en modo paginado puede producir varios.
 *
 * @param caja        caja de origen (sus propiedades de pintado se leen de su estilo)
 * @param x           posición de la caja de borde, relativa a la caja de contenido del padre
 * @param y           ídem
 * @param ancho       ancho de la caja de borde
 * @param alto        alto de la caja de borde <em>en este fragmento</em>
 * @param borde       bordes pintados en este fragmento (con {@code Decoracion: cortar}, el
 *                    primero no tiene borde inferior y los siguientes no tienen superior)
 * @param hijos       fragmentos hijos, en orden del árbol
 * @param lineas      líneas de texto, si la caja contiene texto
 * @param primero     es el primer fragmento de la caja
 * @param ultimo      es el último fragmento de la caja
 */
public record Fragment(Box caja, double x, double y, double ancho, double alto, Insets borde,
                       List<Fragment> hijos, List<LineaTexto> lineas,
                       boolean primero, boolean ultimo) {

    public Fragment {
        hijos = List.copyOf(hijos);
        lineas = List.copyOf(lineas);
    }

    /** Fragmento único (modo continuo o caja que no se ha partido). */
    public static Fragment completo(Box caja, double x, double y, double ancho, double alto,
                                    List<Fragment> hijos, List<LineaTexto> lineas) {
        return new Fragment(caja, x, y, ancho, alto, caja.borde, hijos, lineas, true, true);
    }

    public Fragment movido(double nuevaX, double nuevaY) {
        return new Fragment(caja, nuevaX, nuevaY, ancho, alto, borde, hijos, lineas, primero, ultimo);
    }
}
