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
 * The canvas of a PDF page, with PDFBox: the drawing primitives and, with its {@link PdfTagging}, the marked
 * content of the tagged PDF and the link annotations. The document is created and saved by
 * {@link PdfDocumentWriter}; the fonts come from {@link PdfFonts}, the images from {@link PdfImages}, the
 * gradients from {@link PdfGradients} and the structure from {@link PdfStructure}, all of them belonging to the
 * document and shared by its pages. PDF has the origin at the bottom left: vertical coordinates are inverted when
 * painting.
 *
 * <p>Text is painted with the TTF and OTF fonts of the resources folder (reference/common/resources.md). For the
 * lines to measure the same as during layout, the layout must use the same metrics:
 * {@code new Engine(new PdfMetrics(resources))}.</p>
 *
 * <p>Each image (reference/components/image.md) is embedded only once even if it is used on several
 * pages, without resampling: the JPEG as is and the PNG lossless, with its transparency; the SVG,
 * as a vector drawing ({@link PdfImages}).</p>
 *
 * <p>With {@code debug}, like {@link SvgCanvas}, it also draws the outline of each border box
 * (used by the images of the reference tests).</p>
 *
 * <p><strong>Threads:</strong> an instance belongs to one page and is not shared.</p>
 */
public final class PdfCanvas implements Canvas {

    private final PDDocument doc;
    /** The content of the page: the stream, the paths, the color and the rotations. */
    private final PdfContent page;
    private final PDPageContentStream out;
    private final double pageHeight;
    private final Resources resources;
    /** Fonts embedded in the document, shared by all its pages. */
    private final PdfFonts fonts;
    /** Images embedded in the document, by file, shared by all its pages. */
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

    /** The marked content of the tagged PDF and the link annotations of the page. */
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
            // Backgrounds and borders are not content: artifacts of the tagged PDF.
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
     * <p>A plain color is filled with its opacity; a gradient is an axial shading clipped to
     * the shape. If some color of the gradient is not opaque, the opacities go in a luminosity
     * soft mask with the same gradient in grays: PDF has no per-color opacity in a
     * shading.</p>
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
     * <p>The ring between the border box and the padding box, filled with the even-odd rule; with
     * different colors per side, once per side and clipped to its trapezoid.</p>
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
     * The ring of the border: in one color ({@code color}) or, if it is {@code null}, once per side with a width,
     * clipped to its trapezoid and with its color.
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
     * A plain, opaque border with square corners: one rectangle per side with a width, those of the
     * vertical sides on top of the others. Overlapping rectangles leave no seams in the edge
     * pixels, which the even-odd ring does leave, and it is how it was painted before
     * {@code Corner radius}.
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

    /** Fills the ring of the border: the border box minus the padding box, not subtracted if nothing is left of it. */
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
     * <p>With {@code Alt}, the image is the content of its {@code Figure}; without it, an
     * <em>artifact</em> ({@link PdfStructure}).</p>
     */
    @Override
    public void image(Box box, Rect content, Radii radii) {
        if (content.width() <= 0 || content.height() <= 0) {
            return;
        }
        io(() -> {
            ImageFile file = resources.images().find(box.image());
            int number = box.style().image().pdfPage();
            double width = file.width(number);
            double height = file.height(number);
            Rect target = ImageFit.place(box.style().image().fit(), content, width, height);
            tagging.beginImage(box, content);
            out.saveGraphicsState();
            Shapes.roundedRect(page.path(), content, radii);
            out.clip();
            if (file.isVector()) {
                // The drawing has the size of the SVG or the page: it is scaled to its place (Fit) and stays vector.
                out.transform(new Matrix((float) (target.width() / width), 0, 0, (float) (target.height() / height),
                        (float) target.x(), (float) (pageHeight - target.bottom())));
                // A form inherits the graphics state and the drawing does not set the dashes: no dashes.
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
     * <p>A {@code cm} matrix inside a {@code q}/{@code Q}. With the origin at the bottom, a clockwise
     * rotation is a negative angle.</p>
     */
    @Override
    public void beginRotation(double degrees, double x, double y) {
        io(() -> page.rotate(degrees, x, y));
    }

    @Override
    public void endRotation() {
        io(page::unrotate);
    }

    /** A clip inside its own graphics state ({@code q}/{@code Q}), with the path of the shape ({@code W n}). */
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

    /** {@code #RGB}, {@code #RGBA}, {@code #RRGGBB} or {@code #RRGGBBAA} (already validated) as components 0 to 1. */
    static float[] rgb(String color) {
        float[] rgba = Colors.rgba(color);
        return new float[]{rgba[0], rgba[1], rgba[2]};
    }
}
