package dev.stxt.boxes.paint;

import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.resources.ImageFile;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Media;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Locale;

/**
 * Salida SVG para depurar (hito 2): una página por fichero, en pt. Si {@code debug} está
 * activo, dibuja además el contorno de cada caja de borde.
 *
 * <p>No incrusta las fuentes: el visor usa las suyas, y la línea base es la aproximada
 * ({@link dev.stxt.boxes.model.VerticalMetrics#APPROXIMATE}). Las imágenes (§5.18) van
 * incrustadas ({@code data:}) si se le da la carpeta de recursos; si no, solo se ve su caja.</p>
 */
public final class SvgCanvas implements Canvas {

    private final StringBuilder svg = new StringBuilder();
    private final boolean debug;
    /** Carpeta de las imágenes; {@code null} = no se pintan. */
    private final Resources resources;

    public SvgCanvas(boolean debug) {
        this(debug, null);
    }

    public SvgCanvas(boolean debug, Resources resources) {
        this.debug = debug;
        this.resources = resources;
    }

    /** SVG completo de una página, del tamaño del medio, sin imágenes. */
    public static String page(Page page, Media media, boolean debug) {
        return page(page, media, debug, null);
    }

    /** SVG completo de una página, del tamaño del medio, con las imágenes de {@code resources}. */
    public static String page(Page page, Media media, boolean debug, Resources resources) {
        double height = media.height();
        SvgCanvas c = new SvgCanvas(debug, resources);
        Rect area = media.pageArea();
        StackingContext.build(page.content(), page.fixed(), area.x(), area.y()).paint(c);
        return String.format(Locale.ROOT,
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%.2fpt\" height=\"%.2fpt\" viewBox=\"0 0 %.2f %.2f\">%n"
                        + "<rect width=\"100%%\" height=\"100%%\" fill=\"#ffffff\"/>%n%s</svg>%n",
                media.width(), height, media.width(), height, c.svg);
    }

    @Override
    public void background(Rect r, String color) {
        svg.append(String.format(Locale.ROOT, "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"%s\"/>%n",
                r.x(), r.y(), r.width(), r.height(), color));
    }

    @Override
    public void outline(Rect r) {
        if (debug) {
            svg.append(String.format(Locale.ROOT,
                    "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" fill=\"none\" stroke=\"#d33\" stroke-width=\"0.3\" stroke-dasharray=\"2 2\"/>%n",
                    r.x(), r.y(), r.width(), r.height()));
        }
    }

    @Override
    public void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
                     double wordSpacing) {
        String spacing = wordSpacing == 0 ? "" : String.format(Locale.ROOT, " word-spacing=\"%.2f\"", wordSpacing);
        String weight = font.weight() == FontWeight.BOLD ? " font-weight=\"bold\"" : "";
        String style = font.style() == FontStyle.ITALIC ? " font-style=\"italic\"" : "";
        svg.append(String.format(Locale.ROOT,
                "<text x=\"%.2f\" y=\"%.2f\" font-family=\"%s\"%s%s font-size=\"%.2f\" fill=\"%s\"%s>%s</text>%n",
                x, baselineY, escape(font.family()), weight, style, fontSize, color, spacing, escape(text)));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Un {@code <svg>} anidado del tamaño de la caja de contenido recorta la imagen, que va
     * dentro sin conservar su proporción: la colocación ya la da {@link ImageFit}.</p>
     */
    @Override
    public void image(Box box, Rect content) {
        if (resources == null) {
            return;
        }
        ImageFile file = resources.images().find(box.image);
        Rect target = ImageFit.place(box.style.fit, content, file.width(), file.height());
        String data;
        try {
            data = Base64.getEncoder().encodeToString(Files.readAllBytes(file.path()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String mime = file.format() == ImageFile.Format.PNG ? "image/png" : "image/jpeg";
        svg.append(String.format(Locale.ROOT, "<svg x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\">"
                        + "<image x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" preserveAspectRatio=\"none\" "
                        + "href=\"data:%s;base64,%s\"/></svg>%n",
                content.x(), content.y(), content.width(), content.height(), target.x() - content.x(),
                target.y() - content.y(), target.width(), target.height(), mime, data));
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
