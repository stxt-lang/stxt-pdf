package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Media;
import dev.stxt.pdf.tree.Metadata;

import java.util.List;

/**
 * Resultado de {@link Engine#layOutDocument}: lo que necesita la salida (SVG, PDF o el volcado de
 * los tests) sin volver a construir el árbol de cajas. Los fragmentos de las páginas apuntan a
 * las cajas del único árbol construido.
 *
 * <p><strong>Hilos:</strong> una vez maquetado no cambia: las páginas y los fragmentos son {@code record}
 * inmutables y el motor no escribe en las cajas a las que apuntan. Se puede pintar a PDF y a SVG desde varios
 * hilos a la vez con el mismo {@link dev.stxt.pdf.convert.Converter}; lo comprueba
 * {@code ConcurrentConversionTest}. Las cajas tienen campos que no son {@code final} ({@link Box}), así que hay
 * que entregarlo a los otros hilos de forma segura (un {@code Future}, un ejecutor, un campo {@code volatile}…)
 * y no por un campo corriente.</p>
 *
 * @param media    el medio del documento: el de las secciones que no declaran el suyo; el de cada
 *                 página es {@link Page#media()}
 * @param metadata título e idioma del documento
 * @param root     la raíz del documento, de la que cuelgan sus secciones (referencia/componentes/document.md)
 * @param pages    las páginas, en orden; nunca vacía
 */
public record LaidOutDocument(Media media, Metadata metadata, Box root, List<Page> pages) {

    public LaidOutDocument {
        pages = List.copyOf(pages);
    }
}
