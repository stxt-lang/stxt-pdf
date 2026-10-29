package dev.stxt.boxes.layout;

import dev.stxt.boxes.model.Insets;

import static dev.stxt.boxes.model.Tolerance.EPSILON;
import static dev.stxt.boxes.model.Tolerance.fits;

/**
 * Parte de una caja que cae en un fragmento (ESPECIFICACION.md §5.10): su alto, su borde y su
 * relleno. Es la parte común de la fragmentación de todos los algoritmos.
 *
 * <ul>
 *   <li>Con {@code Decoration: slice}, el primer fragmento no tiene borde ni relleno inferiores
 *       y los siguientes no tienen los superiores.</li>
 *   <li>Con {@code Decoration: clone}, cada fragmento tiene borde y relleno completos: el
 *       contenido de cada página cabe entre ellos.</li>
 *   <li>Un {@code Height} definido es el total de todos los fragmentos: si el contenido termina
 *       y el alto propio de la caja no cabe en la página, lo que falta pasa al fragmento
 *       siguiente. Un fragmento cortado dentro del contenido llega hasta el final de la página,
 *       sin pasar del alto que le queda a la caja, pero solo consume lo que ocupa su contenido: el
 *       hueco del final de la página no se le quita al contenido que sigue. Con {@code clone}, el
 *       total es el del contenido: el alto menos un borde y un relleno.</li>
 *   <li>Con alto {@code auto}, el fragmento mide lo que su contenido; {@code Min height} y
 *       {@code Max height} se aplican al total.</li>
 * </ul>
 *
 * @param height   alto de la caja de borde en este fragmento
 * @param border   bordes de este fragmento
 * @param padding  relleno de este fragmento
 * @param finished la caja termina en este fragmento
 * @param consumed alto de caja de borde de este fragmento y de los anteriores; con
 *                 {@code clone}, alto de su caja de contenido
 */
public record Slice(double height, Insets border, Insets padding, boolean finished, double consumed) {

    /** Borde y relleno superiores del fragmento: con {@code slice}, solo los tiene el primero. */
    public static double top(BoxModel model, boolean first) {
        return first || model.clones() ? model.border().top() + model.padding().top() : 0;
    }

    /**
     * Lo que queda de página para el contenido del fragmento: sin el borde y el relleno
     * superiores y, con {@code clone}, tampoco los inferiores. Con {@code slice}, los inferiores
     * no se reservan aquí: solo hacen falta si la caja termina en esta página, y de eso se
     * encarga {@link Engine#layout}.
     */
    public static double contentRemaining(BoxModel model, boolean first, ConstraintSpace space) {
        double bottom = model.clones() ? model.border().bottom() + model.padding().bottom() : 0;
        return space.fragmentainerRemaining().getAsDouble() - top(model, first) - bottom;
    }

    /**
     * Alto, borde y relleno de un fragmento.
     *
     * @param first           es el primer fragmento de la caja
     * @param consumed        alto consumido por los fragmentos anteriores (ver {@link #consumed()})
     * @param content         alto del contenido colocado en este fragmento
     * @param contentFinished el contenido termina en este fragmento
     * @param space           espacio de la caja; sin fragmentación no hay límite
     */
    public static Slice of(BoxModel model, boolean first, double consumed, double content,
                           boolean contentFinished, ConstraintSpace space) {
        if (!space.fragmenting()) {
            return new Slice(model.usedHeight(content), model.border(), model.padding(), true, 0);
        }
        if (model.clones()) {
            return cloned(model, consumed, content, contentFinished, space);
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
            if (!fits(need, remaining) && !fits(need, natural)) {
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
        // Llega hasta el final de la página, pero solo consume lo que ocupa su contenido: el hueco que dejan las
        // líneas o los hijos al final de la página no se le quita al contenido que sigue.
        return new Slice(h, sides(model.border(), first, false), sides(model.padding(), first, false), false,
                consumed + upToContent);
    }

    /**
     * {@code Decoration: clone}: el reparto se hace con el alto de la caja de contenido, y cada
     * fragmento le suma el borde y el relleno completos.
     */
    private static Slice cloned(BoxModel model, double consumed, double content, boolean contentFinished,
                                ConstraintSpace space) {
        double decoration = model.border().vertical() + model.padding().vertical();
        double available = space.fragmentainerRemaining().getAsDouble() - decoration;
        double h = content;
        boolean finished = contentFinished;
        if (contentFinished) {
            double total = Math.max(0, model.usedHeight(consumed + content) - decoration);
            h = Math.max(0, total - consumed);
            if (!fits(h, available) && !fits(h, content)) {
                // El contenido cabe, pero el alto propio de la caja no: el resto, a la página siguiente.
                double room = Math.max(available, content);
                if (room < h - EPSILON) {
                    h = room;
                    finished = false;
                }
            }
        } else if (model.height().isPresent()) {
            double total = Math.max(0, model.height().getAsDouble() - decoration);
            // Como con slice: hasta el final de la página, consumiendo solo el contenido.
            h = Math.max(h, Math.min(available, total - consumed));
            return new Slice(decoration + h, model.border(), model.padding(), false, consumed + content);
        }
        return new Slice(decoration + h, model.border(), model.padding(), finished, consumed + h);
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
