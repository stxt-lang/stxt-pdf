package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.layout.image.NaturalSize;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.tree.Box;

/**
 * Lo que un algoritmo de layout necesita del motor: maquetar y medir otras cajas, que siempre pasa por
 * aquí para usar la caché, las métricas del texto y el tamaño natural de las imágenes. Lo implementa
 * {@link Engine}; los algoritmos dependen de esta interfaz y no del motor, que es quien los crea.
 */
public interface LayoutContext {

    /**
     * Layout de una caja con su algoritmo, a través de la caché. Toda llamada recursiva de layout pasa por aquí,
     * nunca directamente a otro algoritmo: sin la caché, stack y grid anidados crecen de forma exponencial.
     */
    LayoutResult layout(Box box, ConstraintSpace space, BreakToken from);

    /** Ancho más estrecho y ancho natural de la caja de margen (referencia/comun/caja.md). */
    IntrinsicSizes intrinsic(Box box);

    /**
     * Ancho más estrecho y ancho natural del contenido de la caja, sin su {@code Width}: el mínimo automático de un ítem
     * stack depende de ellos (referencia/componentes/stack.md).
     */
    IntrinsicSizes contentIntrinsic(Box box);

    /**
     * Las métricas del texto: el marcador de una lista se mide y se alinea con ellas (referencia/componentes/list.md).
     */
    TextMeasurer measurer();

    /** Tamaño natural de una {@code Image}, de su fichero (referencia/componentes/image.md). */
    NaturalSize natural(Box box);
}
