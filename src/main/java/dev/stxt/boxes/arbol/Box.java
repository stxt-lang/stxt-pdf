package dev.stxt.boxes.arbol;

import dev.stxt.boxes.modelo.Insets;
import dev.stxt.boxes.modelo.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Una caja del árbol de cajas (módulo 0). No coincide 1:1 con los nodos {@code Caja} del
 * documento: puede ser anónima y algunas cajas contienen texto directamente.
 *
 * <p>La geometría que guarda es la del <em>último layout</em> y es relativa a la caja de
 * contenido del padre (módulo 2). El resultado definitivo, por páginas, son los
 * {@link dev.stxt.boxes.fragmentos.Fragment}.</p>
 *
 * <p>La igualdad es por identidad: la caché de layout usa la caja como clave.</p>
 */
public final class Box {

    public final String id;
    public final Estilo estilo;
    public final boolean anonima;
    /** Línea del documento de la que procede, para mensajes de error. */
    public final int linea;
    public final List<Box> hijos = new ArrayList<>();
    public Box padre;
    /** Contenido en línea, si la caja contiene texto directamente. */
    public String texto;

    // Geometría usada (caja de borde), relativa a la caja de contenido del padre.
    public double x;
    public double y;
    public double ancho;
    public double alto;
    public Insets margen = Insets.CERO;
    public Insets borde = Insets.CERO;
    public Insets relleno = Insets.CERO;

    public Box(String id, Estilo estilo, boolean anonima, int linea) {
        this.id = id;
        this.estilo = estilo;
        this.anonima = anonima;
        this.linea = linea;
    }

    public void anadirHijo(Box hijo) {
        hijo.padre = this;
        hijos.add(hijo);
    }

    /** Hijos que participan en el flujo del padre ({@code flujo} y {@code relativa}). */
    public List<Box> hijosEnFlujo() {
        return hijos.stream().filter(h -> !h.estilo.fueraDeFlujo()).toList();
    }

    public boolean tieneTexto() {
        return texto != null && !texto.isBlank();
    }

    public Rect cajaBorde() {
        return new Rect(x, y, ancho, alto);
    }

    public Rect cajaMargen() {
        return cajaBorde().expandir(margen);
    }

    public Rect cajaRelleno() {
        return cajaBorde().contraer(borde);
    }

    public Rect cajaContenido() {
        return cajaRelleno().contraer(relleno);
    }

    /** Nombre legible para mensajes y volcados. */
    public String describir() {
        if (id != null) {
            return id;
        }
        return anonima ? "(anónima)" : "(caja línea " + linea + ")";
    }

    @Override
    public String toString() {
        return "Box[" + describir() + ", " + estilo.layout + "]";
    }
}
