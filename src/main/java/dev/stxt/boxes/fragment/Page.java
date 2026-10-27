package dev.stxt.boxes.fragment;

import java.util.List;

/**
 * Una página del resultado.
 *
 * @param number  número de página, desde 1
 * @param content fragmento raíz con el flujo de esta página
 * @param fixed   fragmentos de las cajas {@code fixed}, repetidos en cada página
 */
public record Page(int number, Fragment content, List<Fragment> fixed) {

    public Page {
        fixed = List.copyOf(fixed);
    }
}
