package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.NumberStyle;

/**
 * Una sección del documento (referencia/componentes/document.md): su caja, que es la raíz de su layout (una
 * {@code Box} sin padre, como la raíz anónima de un documento de una sola sección), su medio y lo
 * que dice de las páginas que ocupa.
 *
 * @param box          la caja: el área útil de cada una de sus páginas; es {@link Element#SECTION}
 * @param media        su medio: el del documento con lo que ella declara encima
 * @param numbering    cómo numera sus páginas
 * @param pageStyle    cómo se escriben sus números de página ({@code Page number format})
 * @param start        el número de su primera página con {@code Page numbering: restart} ({@code Start}); 1 si no lo
 *                     declara
 * @param inheritFixed si lleva los {@code Page overlay} del documento
 * @param beginOn      en qué página física empieza ({@code Begin on})
 */
public record Section(Box box, Media media, PageNumbering numbering, NumberStyle pageStyle, int start,
                      boolean inheritFixed, BeginOn beginOn) {

    /**
     * {@code Begin on} (referencia/componentes/document.md): en qué página física empieza la sección. Si no le toca, la
     * anterior acaba con una página en blanco.
     */
    public enum BeginOn {
        /** En la página siguiente, sea cual sea. */
        ANY,
        /** En una página impar: a la derecha, al imprimir a doble cara. */
        ODD
    }

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
