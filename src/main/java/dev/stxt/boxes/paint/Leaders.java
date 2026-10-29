package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.Tolerance;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * La guía de las entradas de un índice (ESPECIFICACION.md §5.19): lo que se pinta en la caja
 * vacía entre el título y el número de página. Son rectángulos rellenos con el color del texto,
 * sobre la línea base de la entrada.
 */
final class Leaders {

    /** Lado de cada punto, relativo al tamaño de la fuente. */
    private static final double DOT = 0.09;
    /** Distancia entre dos puntos, relativa al tamaño de la fuente. */
    private static final double STEP = 0.5;
    /** Grosor de la línea, relativo al tamaño de la fuente. */
    private static final double RULE = 0.05;
    /** Grosor mínimo de un punto o de la línea, en puntos: por debajo no se ve. */
    private static final double MIN = 0.4;

    private Leaders() {
    }

    /**
     * Los rectángulos de una guía en la caja de contenido {@code content}, apoyados en la línea
     * base {@code baseline}. Los puntos van pegados al número, a la derecha, cada medio cuerpo, y
     * se quedan los que caben; la línea ocupa todo el ancho.
     */
    static List<Rect> shapes(Style.Leader leader, Rect content, double baseline, double fontSize) {
        List<Rect> shapes = new ArrayList<>();
        switch (leader) {
            case DOTS -> {
                double dot = Math.max(MIN, fontSize * DOT);
                double step = Math.max(2 * dot, fontSize * STEP);
                for (double x = content.right() - dot; x >= content.x() - Tolerance.EPSILON; x -= step) {
                    shapes.add(new Rect(x, baseline - dot, dot, dot));
                }
            }
            case RULE -> {
                double thickness = Math.max(MIN, fontSize * RULE);
                shapes.add(new Rect(content.x(), baseline - thickness, content.width(), thickness));
            }
            case NONE -> {
            }
        }
        return shapes;
    }
}
