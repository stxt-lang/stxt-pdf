package dev.stxt.boxes.pintado;

import dev.stxt.boxes.arbol.Estilo;
import dev.stxt.boxes.fragmentos.Fragment;
import dev.stxt.boxes.fragmentos.LineaTexto;
import dev.stxt.boxes.modelo.Rect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Árbol de contextos de apilamiento (módulo 4, ESPECIFICACION.md §5.6), construido sobre los
 * fragmentos de una página. Es independiente del árbol de cajas.
 *
 * <p>Orden de pintado dentro de un contexto, de abajo arriba:</p>
 * <ol>
 *   <li>Fondo y borde de la caja que crea el contexto.</li>
 *   <li>Contextos hijos con {@code Capa} negativa, de menor a mayor.</li>
 *   <li>Cajas en flujo en orden del árbol: primero fondos y bordes, después el texto.</li>
 *   <li>Posicionadas sin {@code Capa} (o con 0), en orden del árbol.</li>
 *   <li>Contextos hijos con {@code Capa} positiva, de menor a mayor.</li>
 * </ol>
 *
 * <p>Simplificación: una caja posicionada sin {@code Capa} se pinta completa en el paso 4, con
 * sus descendientes; CSS promovería al contexto padre los descendientes que crean contexto.</p>
 */
public final class StackingContext {

    /** Fragmento con su origen absoluto ya calculado. */
    private record Pieza(Fragment f, double x, double y) {
        Rect cajaBorde() {
            return new Rect(x, y, f.ancho(), f.alto());
        }
    }

    private final Pieza raiz;
    private final int capa;
    private final List<StackingContext> negativos = new ArrayList<>();
    private final List<Pieza> enFlujo = new ArrayList<>();
    private final List<StackingContext> posicionadosCero = new ArrayList<>();
    private final List<StackingContext> positivos = new ArrayList<>();

    private StackingContext(Pieza raiz, int capa) {
        this.raiz = raiz;
        this.capa = capa;
    }

    /** Contexto raíz de una página; {@code origenX/Y} es la esquina del área útil. */
    public static StackingContext construir(Fragment raiz, double origenX, double origenY) {
        StackingContext sc = new StackingContext(new Pieza(raiz, origenX, origenY), 0);
        sc.recorrerHijos(sc.raiz);
        sc.ordenar();
        return sc;
    }

    private void recorrerHijos(Pieza padre) {
        Rect contenido = padre.cajaBorde().contraer(padre.f().borde()).contraer(padre.f().caja().relleno);
        for (Fragment h : padre.f().hijos()) {
            Pieza p = new Pieza(h, contenido.x() + h.x(), contenido.y() + h.y());
            Estilo e = h.caja().estilo;
            if (creaContexto(h)) {
                StackingContext hijo = new StackingContext(p, e.capa != null ? e.capa : 0);
                hijo.recorrerHijos(p);
                hijo.ordenar();
                if (hijo.capa < 0) {
                    negativos.add(hijo);
                } else if (hijo.capa > 0) {
                    positivos.add(hijo);
                } else {
                    posicionadosCero.add(hijo);
                }
            } else if (e.estaPosicionada()) {
                StackingContext pseudo = new StackingContext(p, 0);
                pseudo.recorrerHijos(p);
                posicionadosCero.add(pseudo);
            } else {
                enFlujo.add(p);
                recorrerHijos(p);
            }
        }
    }

    private static boolean creaContexto(Fragment f) {
        Estilo e = f.caja().estilo;
        if (e.posicion == Estilo.Posicion.FIJA) {
            return true;
        }
        if (e.capa == null) {
            return false;
        }
        Estilo.Layout padre = f.caja().padre != null ? f.caja().padre.estilo.layout : null;
        boolean item = padre == Estilo.Layout.FLEX || padre == Estilo.Layout.GRID;
        return e.estaPosicionada() || item;
    }

    private void ordenar() {
        // Orden estable: a igual Capa, se conserva el orden del árbol.
        negativos.sort(Comparator.comparingInt(s -> s.capa));
        positivos.sort(Comparator.comparingInt(s -> s.capa));
    }

    public void pintar(Canvas canvas) {
        pintarFondoYBorde(canvas, raiz);
        negativos.forEach(s -> s.pintar(canvas));
        enFlujo.forEach(p -> pintarFondoYBorde(canvas, p));
        pintarTexto(canvas, raiz);
        enFlujo.forEach(p -> pintarTexto(canvas, p));
        posicionadosCero.forEach(s -> s.pintar(canvas));
        positivos.forEach(s -> s.pintar(canvas));
    }

    private static void pintarFondoYBorde(Canvas canvas, Pieza p) {
        Estilo e = p.f().caja().estilo;
        Rect borde = p.cajaBorde();
        if (e.fondo != null) {
            canvas.fondo(borde.contraer(p.f().borde()), e.fondo);
        }
        canvas.borde(borde, p.f().borde(), e.colorBorde != null ? e.colorBorde : e.color);
    }

    private static void pintarTexto(Canvas canvas, Pieza p) {
        if (p.f().lineas().isEmpty()) {
            return;
        }
        Estilo e = p.f().caja().estilo;
        Rect contenido = p.cajaBorde().contraer(p.f().borde()).contraer(p.f().caja().relleno);
        for (LineaTexto l : p.f().lineas()) {
            // Línea base aproximada: centrada en la line box con el 80 % del tamaño por encima.
            double base = contenido.y() + l.y() + (l.alto() - e.tamano) / 2 + e.tamano * 0.8;
            canvas.texto(contenido.x() + l.x(), base, l.texto(), e.fuente, e.tamano, e.color);
        }
    }
}
