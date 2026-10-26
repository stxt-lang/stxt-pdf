package dev.stxt.boxes.fragmentos;

import java.util.List;

/**
 * Una página del resultado. En modo continuo hay una sola.
 *
 * @param numero    número de página, desde 1
 * @param contenido fragmento raíz con el flujo de esta página
 * @param fijas     fragmentos de las cajas {@code fija}, repetidos en cada página
 */
public record Pagina(int numero, Fragment contenido, List<Fragment> fijas) {

    public Pagina {
        fijas = List.copyOf(fijas);
    }
}
