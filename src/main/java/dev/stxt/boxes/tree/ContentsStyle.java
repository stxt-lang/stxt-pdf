package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Leader;

/**
 * Las propiedades de un {@code Contents} (ESPECIFICACION.md §4.16): los niveles que lista y la guía de sus entradas,
 * que también lleva la guía generada de cada una. No se heredan.
 */
public final class ContentsStyle extends StyleGroup {

    /** Primer y último nivel de encabezado que lista un {@code Contents} ({@code Levels}). */
    private int from = 1;
    private int to = 3;
    /** {@code Leader} de un {@code Contents}; también lo lleva la guía generada de cada entrada. */
    private Leader leader = Leader.DOTS;

    ContentsStyle() {
    }

    public int from() {
        return from;
    }

    void setFrom(int v) {
        writable();
        from = v;
    }

    public int to() {
        return to;
    }

    void setTo(int v) {
        writable();
        to = v;
    }

    public Leader leader() {
        return leader;
    }

    void setLeader(Leader v) {
        writable();
        leader = v;
    }
}
