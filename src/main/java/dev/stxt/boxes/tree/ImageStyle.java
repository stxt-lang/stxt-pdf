package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.Fit;

/**
 * Las propiedades de una {@code Image} (ESPECIFICACION.md §4.15): el texto alternativo y cómo llena su caja. No se
 * heredan.
 */
public final class ImageStyle extends StyleGroup {

    /** Texto alternativo de una {@code Image}; {@code null} = decorativa. */
    private String alt;
    private Fit fit = Fit.CONTAIN;

    ImageStyle() {
    }

    public String alt() {
        return alt;
    }

    void setAlt(String v) {
        writable();
        alt = v;
    }

    public Fit fit() {
        return fit;
    }

    void setFit(Fit v) {
        writable();
        fit = v;
    }
}
