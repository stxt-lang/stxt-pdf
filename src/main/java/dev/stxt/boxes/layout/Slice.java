package dev.stxt.boxes.layout;

import dev.stxt.boxes.model.Insets;

/**
 * Parte de una caja que cae en un fragmento (ESPECIFICACION.md §5.10): su alto, su borde y su
 * relleno. Es la parte común de la fragmentación de todos los algoritmos.
 *
 * <ul>
 *   <li>Con {@code Decoration: slice}, el primer fragmento no tiene borde ni relleno inferiores
 *       y los siguientes no tienen los superiores.</li>
 *   <li>Un {@code Height} definido es el total de todos los fragmentos: si el contenido termina
 *       y el alto propio de la caja no cabe en la página, lo que falta pasa al fragmento
 *       siguiente. Un fragmento cortado dentro del contenido llega hasta el final de la página,
 *       sin pasar del alto que le queda a la caja.</li>
 *   <li>Con alto {@code auto}, el fragmento mide lo que su contenido; {@code Min height} y
 *       {@code Max height} se aplican al total.</li>
 * </ul>
 *
 * @param height   alto de la caja de borde en este fragmento
 * @param border   bordes de este fragmento
 * @param padding  relleno de este fragmento
 * @param finished la caja termina en este fragmento
 * @param consumed alto de caja de borde de este fragmento y de los anteriores
 */
public record Slice(double height, Insets border, Insets padding, boolean finished, double consumed) {

    private static final double EPSILON = 1e-9;

    // TODO(hito 7): Decoration: clone, con borde y relleno completos en cada fragmento.

    /** Borde y relleno superiores del fragmento: solo los tiene el primero. */
    public static double top(BoxModel model, boolean first) {
        return first ? model.border().top() + model.padding().top() : 0;
    }

    /**
     * Alto, borde y relleno de un fragmento.
     *
     * @param first           es el primer fragmento de la caja
     * @param consumed        alto de caja de borde de los fragmentos anteriores
     * @param content         alto del contenido colocado en este fragmento
     * @param contentFinished el contenido termina en este fragmento
     * @param space           espacio de la caja; sin fragmentación no hay límite
     */
    public static Slice of(BoxModel model, boolean first, double consumed, double content,
                           boolean contentFinished, ConstraintSpace space) {
        if (!space.fragmenting()) {
            return new Slice(model.usedHeight(content), model.border(), model.padding(), true, 0);
        }
        double remaining = space.fragmentainerRemaining().getAsDouble();
        double top = top(model, first);
        double upToContent = top + content;
        if (contentFinished) {
            double natural = upToContent + model.border().bottom() + model.padding().bottom();
            double total = model.height().isPresent()
                    ? model.height().getAsDouble()
                    : Sizing.constrain(consumed + natural, model.minHeight(), model.maxHeight());
            double need = Math.max(0, total - consumed);
            if (need > remaining + EPSILON && need > natural + EPSILON) {
                // El contenido cabe, pero el alto propio de la caja no: el resto, a la página siguiente.
                double h = Math.max(remaining, upToContent);
                if (h < need - EPSILON) {
                    return open(model, first, h, consumed);
                }
            }
            return new Slice(need, sides(model.border(), first, true), sides(model.padding(), first, true),
                    true, consumed + need);
        }
        double h = upToContent;
        if (model.height().isPresent()) {
            h = Math.max(h, Math.min(remaining, model.height().getAsDouble() - consumed));
        }
        return open(model, first, h, consumed);
    }

    /** Fragmento que no es el último: sin borde ni relleno inferiores. */
    private static Slice open(BoxModel model, boolean first, double height, double consumed) {
        return new Slice(height, sides(model.border(), first, false), sides(model.padding(), first, false),
                false, consumed + height);
    }

    private static Insets sides(Insets all, boolean top, boolean bottom) {
        return new Insets(top ? all.top() : 0, all.right(), bottom ? all.bottom() : 0, all.left());
    }

    /** Alto de la caja de contenido de este fragmento. */
    public double contentHeight() {
        return Math.max(0, height - border.vertical() - padding.vertical());
    }
}
