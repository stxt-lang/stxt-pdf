package dev.stxt.pdf.tree;

/**
 * Una sección del documento (referencia/componentes/document.md): su caja, que es la raíz de su layout (una
 * {@code Box} sin padre, como la raíz anónima de un documento de una sola sección), su medio y lo
 * que dice de las páginas que ocupa.
 *
 * @param box          la caja: el área útil de cada una de sus páginas; es {@link Element#SECTION}
 * @param media        su medio: el del documento con lo que ella declara encima
 * @param numbering    cómo numera sus páginas
 * @param inheritFixed si lleva los {@code Fixed} del documento
 */
public record Section(Box box, Media media, PageNumbering numbering, boolean inheritFixed) {

    /** {@code Page numbering} (referencia/componentes/document.md): qué hace la sección con el contador de páginas. */
    public enum PageNumbering {
        /** Sus páginas siguen el contador del documento. */
        CONTINUE,
        /** Su primera página vuelve a ser la 1. */
        RESTART,
        /** Sus páginas no tienen número y no avanzan el contador. */
        NONE
    }
}
