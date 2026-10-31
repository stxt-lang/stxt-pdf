package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Break;
import dev.stxt.pdf.model.Edges;

/**
 * The {@code Break} group (reference/common/breaks.md): the page or column breaks before and after the box, what is
 * kept together ({@code Keep with next}, {@code Keep with previous}, {@code Keep together}) and its decoration at a
 * break. It is not inherited; widows and orphans belong to {@code Typography}.
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
