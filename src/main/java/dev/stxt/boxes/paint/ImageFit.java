package dev.stxt.boxes.paint;

import dev.stxt.boxes.model.Fit;
import dev.stxt.boxes.model.Rect;

/**
 * Dónde se dibuja una imagen en su caja de contenido según {@code Fit}
 * (referencia/componentes/image.md). Con proporciones iguales, las tres formas dan la caja de contenido.
 */
final class ImageFit {

    private ImageFit() {
    }

    /**
     * Rectángulo de la imagen, en las coordenadas de {@code content}: con {@code contain} cabe
     * entera y centrada; con {@code cover} la cubre, centrada, y se recorta a la caja de
     * contenido; con {@code fill} es la caja de contenido.
     *
     * @param width  ancho natural de la imagen (solo cuenta su proporción)
     * @param height alto natural de la imagen
     */
    static Rect place(Fit fit, Rect content, double width, double height) {
        if (fit == Fit.FILL) {
            return content;
        }
        double sx = content.width() / width;
        double sy = content.height() / height;
        double scale = fit == Fit.CONTAIN ? Math.min(sx, sy) : Math.max(sx, sy);
        double w = width * scale;
        double h = height * scale;
        return new Rect(content.x() + (content.width() - w) / 2, content.y() + (content.height() - h) / 2, w, h);
    }
}
