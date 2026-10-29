package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.layout.image.NaturalSize;
import dev.stxt.boxes.layout.inline.TextMeasurer;
import dev.stxt.boxes.tree.Box;

/**
 * Lo que un algoritmo de layout necesita del motor: maquetar y medir otras cajas, que siempre pasa por
 * aquí para usar la caché, las métricas del texto y el tamaño natural de las imágenes. Lo implementa
 * {@link Engine}; los algoritmos dependen de esta interfaz y no del motor, que es quien los crea.
 */
public interface LayoutContext {

    /**
     * Layout de una caja con su algoritmo, a través de la caché. Toda llamada recursiva de layout pasa por aquí,
     * nunca directamente a otro algoritmo: sin la caché, flex y grid anidados crecen de forma exponencial.
     */
    LayoutResult layout(Box box, ConstraintSpace space, BreakToken from);

    /** min-content y max-content de la caja de margen (referencia/comun/caja.md). */
    IntrinsicSizes intrinsic(Box box);

    /**
     * min-content y max-content del contenido de la caja, sin su {@code Width}: el mínimo automático de un ítem
     * flex depende de ellos (referencia/componentes/flex.md).
     */
    IntrinsicSizes contentIntrinsic(Box box);

    /**
     * Las métricas del texto: el marcador de una lista se mide y se alinea con ellas (referencia/componentes/list.md).
     */
    TextMeasurer measurer();

    /** Tamaño natural de una {@code Image}, de su fichero (referencia/componentes/image.md). */
    NaturalSize natural(Box box);
}
