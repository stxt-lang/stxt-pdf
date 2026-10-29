package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Decoration;

/**
 * El grupo {@code Break} (referencia/comun/cortes.md): los cortes de página o de columna antes, después y dentro de la
 * caja y su decoración en un corte. No se hereda; las viudas y las huérfanas son de {@code Typography}.
 */
public final class BreakStyle extends StyleGroup {

    private Break before = Break.AUTO;
    private Break after = Break.AUTO;
    private boolean avoidInside;
    private Decoration decoration = Decoration.SLICE;

    BreakStyle() {
    }

    public Break before() {
        return before;
    }

    void setBefore(Break v) {
        writable();
        before = v;
    }

    public Break after() {
        return after;
    }

    void setAfter(Break v) {
        writable();
        after = v;
    }

    public boolean avoidInside() {
        return avoidInside;
    }

    void setAvoidInside(boolean v) {
        writable();
        avoidInside = v;
    }

    public Decoration decoration() {
        return decoration;
    }

    void setDecoration(Decoration v) {
        writable();
        decoration = v;
    }
}
