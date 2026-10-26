package dev.stxt.boxes.pintado;

import dev.stxt.boxes.arbol.Medio;
import dev.stxt.boxes.fragmentos.Pagina;
import dev.stxt.boxes.modelo.Insets;
import dev.stxt.boxes.modelo.Rect;

import java.util.Locale;

/**
 * Salida SVG para depurar (hito 2): una página por fichero, en pt. Si {@code depurar} está
 * activo, dibuja además el contorno de cada caja de borde.
 */
public final class SvgCanvas implements Canvas {

    private final StringBuilder svg = new StringBuilder();
    private final boolean depurar;

    public SvgCanvas(boolean depurar) {
        this.depurar = depurar;
    }

    /** SVG completo de una página. En modo continuo usa {@code altoContenido} como alto. */
    public static String pagina(Pagina pagina, Medio medio, double altoContenido, boolean depurar) {
        double alto = medio.paginado() ? medio.alto().getAsDouble() : altoContenido + medio.margen().vertical();
        SvgCanvas c = new SvgCanvas(depurar);
        Rect util = medio.areaUtil();
        StackingContext.construir(pagina.contenido(), util.x(), util.y()).pintar(c);
        return String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.2fpt\" height=\"%.2fpt\" viewBox=\"0 0 %.2f %.2f\">%n"
                        + "<rect width=\"100%%\" height=\"100%%\" fill=\"#ffffff\"/>%n%s</svg>%n",
                medio.ancho(), alto, medio.ancho(), alto, c.svg);
    }

    @Override
    public void fondo(Rect r, String color) {
        svg.append(String.format(Locale.ROOT, "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"%s\"/>%n",
                r.x(), r.y(), r.ancho(), r.alto(), color));
    }

    @Override
    public void borde(Rect r, Insets g, String color) {
        if (g.arriba() > 0) {
            fondo(new Rect(r.x(), r.y(), r.ancho(), g.arriba()), color);
        }
        if (g.abajo() > 0) {
            fondo(new Rect(r.x(), r.abajo() - g.abajo(), r.ancho(), g.abajo()), color);
        }
        if (g.izquierda() > 0) {
            fondo(new Rect(r.x(), r.y(), g.izquierda(), r.alto()), color);
        }
        if (g.derecha() > 0) {
            fondo(new Rect(r.derecha() - g.derecha(), r.y(), g.derecha(), r.alto()), color);
        }
        if (depurar) {
            svg.append(String.format(Locale.ROOT,
                    "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"none\" stroke=\"#d33\" stroke-width=\"0.3\" stroke-dasharray=\"2 2\"/>%n",
                    r.x(), r.y(), r.ancho(), r.alto()));
        }
    }

    @Override
    public void texto(double x, double yBase, String texto, String fuente, double tamano, String color) {
        svg.append(String.format(Locale.ROOT,
                "<text x=\"%.2f\" y=\"%.2f\" font-family=\"%s\" font-size=\"%.2f\" fill=\"%s\">%s</text>%n",
                x, yBase, escapar(fuente), tamano, color, escapar(texto)));
    }

    private static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
