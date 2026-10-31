package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Edges;

/**
 * El grupo {@code Break} (reference/common/breaks.md): los saltos de página o de columna antes y después de la caja, lo
 * que se mantiene junto ({@code Keep with next}, {@code Keep with previous}, {@code Keep together}) y su decoración en
 * un corte. No se hereda; las viudas y las huérfanas son de {@code Typography}.
 */
public final class BreakStyle extends StyleGroup {

    private Break before = Break.AUTO;
    private Break after = Break.AUTO;
    private boolean keepWithNext;
    private boolean keepWithPrevious;
    private boolean keepTogether;
    private Edges edges = Edges.OPEN;

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

    public boolean keepWithNext() {
        return keepWithNext;
    }

    void setKeepWithNext(boolean v) {
        writable();
        keepWithNext = v;
    }

    public boolean keepWithPrevious() {
        return keepWithPrevious;
    }

    void setKeepWithPrevious(boolean v) {
        writable();
        keepWithPrevious = v;
    }

    public boolean keepTogether() {
        return keepTogether;
    }

    void setKeepTogether(boolean v) {
        writable();
        keepTogether = v;
    }

    public Edges edges() {
        return edges;
    }

    void setEdges(Edges v) {
        writable();
        edges = v;
    }
}
