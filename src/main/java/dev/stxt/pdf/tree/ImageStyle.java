package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Fit;

/**
 * Las propiedades de una {@code Image} (referencia/componentes/image.md): el texto alternativo, cómo llena su caja y la
 * página de un PDF. No se heredan.
 */
public final class ImageStyle extends StyleGroup {

    /** Texto alternativo de una {@code Image}; {@code null} = decorativa. */
    private String alt;
    private Fit fit = Fit.CONTAIN;
    /** Página de un PDF, desde 1. */
    private int page = 1;

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

    /** La página que se dibuja de un PDF, desde 1; 1 si no se declara. */
    public int page() {
        return page;
    }

    void setPage(int v) {
        writable();
        page = v;
    }
}
