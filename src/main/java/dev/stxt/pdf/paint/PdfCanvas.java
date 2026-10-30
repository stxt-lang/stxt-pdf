package dev.stxt.pdf.paint;

import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Paint;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.resources.ImageFile;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.tree.Box;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;

import static dev.stxt.pdf.paint.PdfContent.io;

/**
 * El lienzo de una página PDF, con PDFBox: las primitivas del dibujo y, con su {@link PdfTagging}, el
 * contenido marcado del PDF etiquetado y las anotaciones de los enlaces. El documento lo crea y lo guarda
 * {@link PdfDocumentWriter}; las fuentes son de {@link PdfFonts}, las imágenes de {@link PdfImages}, los
 * degradados de {@link PdfGradients} y la estructura de {@link PdfStructure}, todos del documento y compartidos
 * por sus páginas. PDF tiene el origen abajo a la izquierda: las coordenadas verticales se invierten al pintar.
 *
 * <p>El texto se pinta con las fuentes TTF de la carpeta de recursos (referencia/comun/recursos.md). Para que las
 * líneas midan lo mismo que al maquetar, el layout debe usar las mismas métricas:
 * {@code new Engine(new PdfMetrics(resources))}.</p>
 *
 * <p>Cada imagen (referencia/componentes/image.md) se incrusta una sola vez aunque se use en varias
 * páginas, sin remuestrear: el JPEG tal cual y el PNG sin pérdida, con su transparencia; el SVG,
 * como dibujo vectorial ({@link PdfImages}).</p>
 *
 * <p>Con {@code debug}, como {@link SvgCanvas}, dibuja además el contorno de cada caja de borde
 * (lo usan las imágenes de los tests de referencia).</p>
 *
 * <p><strong>Hilos:</strong> una instancia es de una página y no se comparte.</p>
 */
public final class PdfCanvas implements Canvas {

    private final PDDocument doc;
    /** El contenido de la página: el flujo, los caminos, el color y los giros. */
    private final PdfContent page;
    private final PDPageContentStream out;
    private final double pageHeight;
    private final Resources resources;
    /** Fuentes incrustadas en el documento, compartidas por todas sus páginas. */
    private final PdfFonts fonts;
    /** Imágenes incrustadas en el documento, por fichero, compartidas por todas sus páginas. */
    private final PdfImages images;
    private final PdfTagging tagging;
    private final boolean debug;

    PdfCanvas(PDDocument doc, PDPageContentStream out, double pageHeight, Resources resources, PdfFonts fonts,
              PdfImages images, PdfStructure structure, PdfLinks links, boolean debug) {
        this.doc = doc;
        this.page = new PdfContent(out, pageHeight);
        this.out = out;
        this.pageHeight = pageHeight;
        this.resources = resources;
        this.fonts = fonts;
        this.images = images;
        this.tagging = new PdfTagging(page, structure, links);
        this.debug = debug;
    }

    /** El contenido marcado del PDF etiquetado y las anotaciones de los enlaces de la página. */
    @Override
    public PdfTagging tagging() {
        return tagging;
    }

    @Override
    public void background(Rect r, String color) {
        if (r.width() <= 0 || r.height() <= 0) {
            return;
        }
        io(() -> {
            // Fondos y bordes no son contenido: artifacts del PDF etiquetado.
            out.beginMarkedContent(COSName.ARTIFACT);
            page.painting(color, () -> {
                out.addRect((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
                out.fill();
            });
            out.endMarkedContent();
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Un color liso se rellena con su opacidad; un degradado es un sombreado axial recortado a
     * la forma. Si algún color del degradado no es opaco, las opacidades van en una máscara
     * suave de luminosidad con el mismo degradado en grises: PDF no tiene opacidad por color en
     * un sombreado.</p>
     */
    @Override
    public void fill(Rect r, Radii radii, Paint paint) {
        if (r.width() <= 0 || r.height() <= 0) {
            return;
        }
        io(() -> {
            out.beginMarkedContent(COSName.ARTIFACT);
            if (paint instanceof Paint.Solid solid) {
                page.painting(solid.color(), () -> {
                    Shapes.roundedRect(page.path(), r, radii);
                    out.fill();
                });
            } else {
                gradient((Paint.Linear) paint, r, radii);
            }
            out.endMarkedContent();
        });
    }

    private void gradient(Paint.Linear linear, Rect r, Radii radii) throws IOException {
        double[] line = linear.line(r);
        double[] pdfLine = {line[0], pageHeight - line[1], line[2], pageHeight - line[3]};
        out.saveGraphicsState();
        Shapes.roundedRect(page.path(), r, radii);
        out.clip();
        if (linear.isTranslucent()) {
            out.setGraphicsStateParameters(PdfGradients.opacityMask(doc, linear, page.pdfRect(r), pdfLine));
        }
        out.shadingFill(PdfGradients.axial(PDDeviceRGB.INSTANCE, pdfLine, linear.stops(), false));
        out.restoreGraphicsState();
    }

    /**
     * {@inheritDoc}
     *
     * <p>El anillo entre la caja de borde y la de relleno, rellenado con regla par-impar; con
     * colores distintos por lado, una vez por lado y recortado a su trapecio.</p>
     */
    @Override
    public void border(Rect r, Insets w, Radii radii, Sides<String> colors) {
        double[] widths = {w.top(), w.right(), w.bottom(), w.left()};
        String[] sideColors = {colors.top(), colors.right(), colors.bottom(), colors.left()};
        if (w.top() > 0 || w.right() > 0 || w.bottom() > 0 || w.left() > 0) {
            String uniform = null;
            boolean same = true;
            for (int i = 0; i < 4; i++) {
                if (widths[i] > 0) {
                    uniform = uniform == null ? sideColors[i] : uniform;
                    same &= uniform.equals(sideColors[i]);
                }
            }
            if (same && radii.isSquare() && !Colors.isTranslucent(uniform)) {
                squareBorder(r, w, uniform);
            } else {
                String color = same ? uniform : null;
                io(() -> ringBorder(r, w, radii, color, widths, sideColors));
            }
        }
    }

    /**
     * El anillo del borde: de un color ({@code color}) o, si es {@code null}, una vez por lado con ancho,
     * recortado a su trapecio y con su color.
     */
    private void ringBorder(Rect r, Insets w, Radii radii, String color, double[] widths, String[] sideColors)
            throws IOException {
        out.beginMarkedContent(COSName.ARTIFACT);
        if (color != null) {
            page.painting(color, () -> ring(r, w, radii));
        } else {
            Shapes.Side[] sides = Shapes.Side.values();
            for (int i = 0; i < 4; i++) {
                if (widths[i] > 0) {
                    out.saveGraphicsState();
                    Shapes.sidePolygon(page.path(), sides[i], r, w, radii.inset(w));
                    out.clip();
                    page.painting(sideColors[i], () -> ring(r, w, radii));
                    out.restoreGraphicsState();
                }
            }
        }
        out.endMarkedContent();
    }

    /**
     * Un borde liso y opaco de esquinas cuadradas: un rectángulo por lado con ancho, los de los
     * lados verticales encima de los otros. Los rectángulos solapados no dejan costuras en los
     * píxeles de borde, que el anillo par-impar sí deja, y es como se pintaba antes de
     * {@code Radius}.
     */
    private void squareBorder(Rect r, Insets w, String color) {
        if (w.top() > 0) {
            background(new Rect(r.x(), r.y(), r.width(), w.top()), color);
        }
        if (w.bottom() > 0) {
            background(new Rect(r.x(), r.bottom() - w.bottom(), r.width(), w.bottom()), color);
        }
        if (w.left() > 0) {
            background(new Rect(r.x(), r.y(), w.left(), r.height()), color);
        }
        if (w.right() > 0) {
            background(new Rect(r.right() - w.right(), r.y(), w.right(), r.height()), color);
        }
    }

    /** Rellena el anillo del borde: la caja de borde menos la de relleno, que si no queda nada no se resta. */
    private void ring(Rect r, Insets w, Radii radii) throws IOException {
        Shapes.roundedRect(page.path(), r, radii);
        Rect inner = r.shrink(w);
        if (inner.width() > 0 && inner.height() > 0) {
            Shapes.roundedRect(page.path(), inner, radii.inset(w));
        }
        out.fillEvenOdd();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Con {@code Alt}, la imagen es el contenido de su {@code Figure}; sin él, un
     * <em>artifact</em> ({@link PdfStructure}).</p>
     */
    @Override
    public void image(Box box, Rect content, Radii radii) {
        if (content.width() <= 0 || content.height() <= 0) {
            return;
        }
        io(() -> {
            ImageFile file = resources.images().find(box.image());
            int number = box.style().image().page();
            double width = file.width(number);
            double height = file.height(number);
            Rect target = ImageFit.place(box.style().image().fit(), content, width, height);
            tagging.beginImage(box, content);
            out.saveGraphicsState();
            Shapes.roundedRect(page.path(), content, radii);
            out.clip();
            if (file.isVector()) {
                // El dibujo mide lo que el SVG o la página: se escala a donde va (Fit) y sigue siendo vectorial.
                out.transform(new Matrix((float) (target.width() / width), 0, 0, (float) (target.height() / height),
                        (float) target.x(), (float) (pageHeight - target.bottom())));
                // Un formulario hereda el estado gráfico y el dibujo no fija los guiones: sin guiones.
                out.setLineDashPattern(new float[0], 0);
                out.drawForm(file.format() == ImageFile.Format.PDF ? images.page(file, number) : images.drawing(file));
            } else {
                out.drawImage(images.bitmap(file), (float) target.x(), (float) (pageHeight - target.bottom()),
                        (float) target.width(), (float) target.height());
            }
            out.restoreGraphicsState();
            out.endMarkedContent();
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Una matriz {@code cm} dentro de un {@code q}/{@code Q}. Con el origen abajo, el giro en el
     * sentido de las agujas del reloj es un ángulo negativo.</p>
     */
    @Override
    public void beginRotation(double degrees, double x, double y) {
        io(() -> page.rotate(degrees, x, y));
    }

    @Override
    public void endRotation() {
        io(page::unrotate);
    }

    /** Un recorte dentro de un estado gráfico propio ({@code q}/{@code Q}), con el camino de la forma ({@code W n}). */
    @Override
    public void beginClip(Rect r, Radii radii) {
        io(() -> {
            out.saveGraphicsState();
            Shapes.roundedRect(page.path(), r, radii);
            out.clip();
        });
    }

    @Override
    public void endClip() {
        io(out::restoreGraphicsState);
    }

    @Override
    public boolean outlines() {
        return debug;
    }

    @Override
    public void outline(Rect r) {
        if (debug) {
            io(() -> {
                out.beginMarkedContent(COSName.ARTIFACT);
                out.setStrokingColor(0xDD / 255f, 0x33 / 255f, 0x33 / 255f);
                out.setLineWidth(0.3f);
                out.setLineDashPattern(new float[]{2, 2}, 0);
                out.addRect((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
                out.stroke();
                out.endMarkedContent();
            });
        }
    }

    @Override
    public void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
                     double wordSpacing, double letterSpacing) {
        io(() -> {
            page.painting(color, () -> {
                out.beginText();
                out.setFont(fonts.font(font), (float) fontSize);
                out.newLineAtOffset((float) x, (float) (pageHeight - baselineY));
                if (wordSpacing == 0 && letterSpacing == 0) {
                    out.showText(text);
                } else {
                    out.showTextWithPositioning(PdfFonts.spaced(text, wordSpacing, letterSpacing, fontSize));
                }
                out.endText();
            });
        });
    }

    @Override
    public VerticalMetrics vertical(FontFace font) {
        return resources.fonts().find(font).vertical();
    }

    @Override
    public DecorationMetrics decoration(FontFace font) {
        return resources.fonts().find(font).decoration();
    }

    /** {@code #RGB}, {@code #RGBA}, {@code #RRGGBB} o {@code #RRGGBBAA} (ya validado) en componentes de 0 a 1. */
    static float[] rgb(String color) {
        float[] rgba = Colors.rgba(color);
        return new float[]{rgba[0], rgba[1], rgba[2]};
    }
}
