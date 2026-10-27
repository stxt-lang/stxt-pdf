package dev.stxt.boxes.layout;

import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.tree.Media;
import dev.stxt.boxes.tree.Metadata;

import java.util.List;

/**
 * Resultado de {@link Engine#layOutDocument}: lo que necesita la salida (SVG, PDF o el volcado de
 * los tests) sin volver a construir el árbol de cajas. Los fragmentos de las páginas apuntan a
 * las cajas del único árbol construido.
 *
 * @param media    tamaño de página y márgenes
 * @param metadata título e idioma del documento
 * @param pages    las páginas, en orden; nunca vacía
 */
public record LaidOutDocument(Media media, Metadata metadata, List<Page> pages) {

    public LaidOutDocument {
        pages = List.copyOf(pages);
    }
}
