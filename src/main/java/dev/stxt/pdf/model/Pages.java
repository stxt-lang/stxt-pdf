package dev.stxt.pdf.model;

/**
 * Las páginas en que sale un {@code Fixed} (referencia/componentes/relative-absolute-fixed.md); la página 1 es la
 * primera del documento.
 */
public enum Pages {
    ALL, FIRST, NOT_FIRST, ODD, EVEN, LAST;

    /** Si un {@code Fixed} con este valor sale en la página {@code page} de un documento de {@code total}. */
    public boolean includes(int page, int total) {
        return switch (this) {
            case ALL -> true;
            case FIRST -> page == 1;
            case NOT_FIRST -> page != 1;
            case ODD -> page % 2 == 1;
            case EVEN -> page % 2 == 0;
            case LAST -> page == total;
        };
    }
}
