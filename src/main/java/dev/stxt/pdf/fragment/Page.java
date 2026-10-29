package dev.stxt.pdf.fragment;

import dev.stxt.pdf.tree.Media;

import java.util.List;

/**
 * Una página del resultado.
 *
 * @param number  número de página física, desde 1, en todo el documento
 * @param content fragmento raíz con el flujo de esta página: el de la caja de su sección
 * @param fixed   fragmentos de las cajas {@code fixed} que salen en esta página
 * @param media   el medio de la página: el de su sección (referencia/componentes/document.md)
 * @param section posición de su sección en el documento, desde 0
 * @param printed número impreso de la página (lo que dicen {@code {page}} y los índices), desde 1;
 *                0 si la página no se numera ({@code Page numbering: none})
 */
public record Page(int number, Fragment content, List<Fragment> fixed, Media media, int section, int printed) {

    public Page {
        fixed = List.copyOf(fixed);
    }

    /** Si la página lleva número impreso. */
    public boolean numbered() {
        return printed > 0;
    }
}
