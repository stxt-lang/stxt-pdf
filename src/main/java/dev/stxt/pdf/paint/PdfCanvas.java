package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.InlineImage;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.LineStyle;
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
import java.util.List;

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

    /** The canvas of a page of {@code document}, which is written to {@code out} and has its top at {@code pageHeight}. */
    PdfCanvas(PdfPageWriter document, PDPageContentStream out, double pageHeight, boolean debug) {
        this.doc = document.doc();
        this.page = new PdfContent(out, pageHeight);
        this.out = out;
        this.pageHeight = pageHeight;
        this.resources = document.resources();
        this.fonts = document.fonts();
        this.images = document.images();
        this.tagging = new PdfTagging(page, document.structure(), document.links());
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
     * different colors per side, once per side and clipped to its trapezoid. A border that is not solid is stroked
     * with the dash pattern of the PDF ({@link Dashes}).</p>
     */
    @Override
    public void border(Rect r, Insets w, Radii radii, Sides<String> colors, LineStyle style) {
        if (w.top() > 0 || w.right() > 0 || w.bottom() > 0 || w.left() > 0) {
            String uniform = Shapes.uniformColor(w, colors);
            if (style != LineStyle.SOLID) {
                io(() -> patternBorder(r, w, radii, uniform, colors, style));
            } else if (uniform != null && radii.isSquare() && !Colors.isTranslucent(uniform)) {
                squareBorder(r, w, uniform);
            } else {
                io(() -> ringBorder(r, w, radii, uniform, colors));
            }
        }
    }

    @Override
    public void line(Rect r, boolean vertical, String color, LineStyle style) {
        if (style == LineStyle.SOLID) {
            background(r, color);
        } else if (r.width() > 0 && r.height() > 0) {
            io(() -> {
                out.beginMarkedContent(COSName.ARTIFACT);
                stroke(Dashes.segment(r, vertical, style, 0, 0), color);
                out.endMarkedContent();
            });
        }
    }

    /**
     * A border that is not solid: with square corners, the line of each side; with rounded ones, the line along the
     * middle of the border, in one color ({@code color}) or, if it is {@code null}, once per side with a width,
     * clipped to its trapezoid and with its color.
     */
    private void patternBorder(Rect r, Insets w, Radii radii, String color, Sides<String> colors, LineStyle style)
            throws IOException {
        out.beginMarkedContent(COSName.ARTIFACT);
        if (radii.isSquare()) {
            for (Dashes.SideSegment side : Dashes.border(r, w, style)) {
                stroke(side.segment(), side.side().of(colors));
            }
        } else if (color != null) {
            stroke(r, w, radii, color, style);
        } else {
            for (Shapes.Side side : Shapes.Side.values()) {
                if (side.of(w) > 0) {
                    out.saveGraphicsState();
                    Shapes.sidePolygon(page.path(), side, r, w, radii.inset(w));
                    out.clip();
                    stroke(r, w, radii, side.of(colors), style);
                    out.restoreGraphicsState();
                }
            }
        }
        out.endMarkedContent();
    }

    /** Strokes the lines along the middle of a border with rounded corners, each with its pattern fitted to it. */
    private void stroke(Rect r, Insets w, Radii radii, String color, LineStyle style) throws IOException {
        double width = Math.max(Math.max(w.top(), w.bottom()), Math.max(w.left(), w.right()));
        for (Shapes.Run run : Shapes.centerlines(r, w, radii)) {
            double length = Shapes.length(run.path());
            Dashes.Pattern pattern = run.closed() ? Dashes.closed(style, length, width)
                    : Dashes.open(style, length, width);
            page.stroking(color, () -> {
                pen(width, pattern);
                run.path().accept(page.path());
                out.stroke();
            });
        }
    }

    /** Strokes a straight line with its pattern; nothing if there is no line. */
    private void stroke(Dashes.Segment s, String color) throws IOException {
        if (s != null) {
            page.stroking(color, () -> {
                pen(s.width(), s.pattern());
                out.moveTo((float) s.x1(), (float) (pageHeight - s.y1()));
                out.lineTo((float) s.x2(), (float) (pageHeight - s.y2()));
                out.stroke();
            });
        }
    }

    /** The width, the dash pattern and the ends of what is stroked next; without a pattern, a solid line. */
    private void pen(double width, Dashes.Pattern pattern) throws IOException {
        out.setLineWidth((float) width);
        if (pattern == null) {
            out.setLineDashPattern(new float[0], 0);
        } else {
            out.setLineDashPattern(new float[]{(float) pattern.on(), (float) pattern.off()}, (float) pattern.phase());
        }
        out.setLineCapStyle(pattern != null && pattern.round() ? 1 : 0);
    }

    /**
     * The ring of the border: in one color ({@code color}) or, if it is {@code null}, once per side with a width,
     * clipped to its trapezoid and with its color.
     */
    private void ringBorder(Rect r, Insets w, Radii radii, String color, Sides<String> colors) throws IOException {
        out.beginMarkedContent(COSName.ARTIFACT);
        if (color != null) {
            page.painting(color, () -> ring(r, w, radii));
        } else {
            for (Shapes.Side side : Shapes.Side.values()) {
                if (side.of(w) > 0) {
                    out.saveGraphicsState();
                    Shapes.sidePolygon(page.path(), side, r, w, radii.inset(w));
                    out.clip();
                    page.painting(side.of(colors), () -> ring(r, w, radii));
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
        Shapes.ring(page.path(), r, w, radii);
        out.fillEvenOdd();
    }

    /**
     * {@inheritDoc}
     *
     * <p>With {@code Alt}, the image is the content of its {@code Figure}; without it, an
     * <em>artifact</em> ({@link PdfStructure}).</p>
     */
    @Override
    public void image(Box box, Rect content, Radii radii, List<TextLine> text) {
        if (content.width() <= 0 || content.height() <= 0) {
            return;
        }
        if (box.modules() != null) {
            modules(box, content, radii, text);
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
     * <p>Drawn exactly in {@code area}, which already has the proportion of the file; a PDF page cannot be one
     * ({@link dev.stxt.pdf.layout.image.Images}).</p>
     */
    @Override
    public void inlineImage(Box box, InlineImage image, String link, Rect area) {
        if (area.width() <= 0 || area.height() <= 0) {
            return;
        }
        io(() -> {
            ImageFile file = resources.images().find(image.path());
            tagging.beginInlineImage(box, image, link, area);
            out.saveGraphicsState();
            if (file.isVector()) {
                out.transform(new Matrix((float) (area.width() / file.width()), 0, 0,
                        (float) (area.height() / file.height()), (float) area.x(),
                        (float) (pageHeight - area.bottom())));
                out.setLineDashPattern(new float[0], 0);
                out.drawForm(images.drawing(file));
            } else {
                out.drawImage(images.bitmap(file), (float) area.x(), (float) (pageHeight - area.bottom()),
                        (float) area.width(), (float) area.height());
            }
            out.restoreGraphicsState();
            out.endMarkedContent();
        });
    }

    /** The readable text of a barcode: each run of its line, with the baseline its font gives it in the line. */
    private void readableText(Box box, Rect content, List<TextLine> text) {
        for (TextLine line : text) {
            FontFace font = box.style().typography().fontFace();
            double baseline = vertical(font).baseline(content.y() + line.y(), line.height(), line.fontSize());
            for (TextLine.Run run : line.runs()) {
                text(content.x() + line.x() + run.x(), baseline, run.text(), font, line.fontSize(),
                        box.style().typography().color(), 0, 0);
            }
        }
    }

    /**
     * The modules of a generated image, such as a {@code Barcode} (reference/components/barcode.md), in the
     * {@code Color} of its {@code Typography}: one rectangle per run of dark modules of each row, all in a single path
     * that is filled once, so that no seam is seen between neighboring modules. The light ones are not painted: they
     * are the background of the box. The readable text of a barcode ({@code text}) goes in the same marked content, in
     * the same color: it is part of the figure, which its {@code Alt} describes.
     */
    private void modules(Box box, Rect content, Radii radii, List<TextLine> text) {
        List<Rect> modules = ImageFit.modules(box.modules(), content, text);
        io(() -> {
            tagging.beginImage(box, content);
            out.saveGraphicsState();
            Shapes.roundedRect(page.path(), content, radii);
            out.clip();
            page.painting(box.style().typography().color(), () -> {
                for (Rect m : modules) {
                    out.addRect((float) m.x(), (float) (pageHeight - m.bottom()), (float) m.width(), (float) m.height());
                }
                out.fill();
            });
            readableText(box, content, text);
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
                int[] kerning = font.kerning() ? resources.fonts().find(font).kerning(text, font.ligatures()) : null;
                if (wordSpacing == 0 && letterSpacing == 0 && kerning == null) {
                    out.showText(text);
                } else {
                    out.showTextWithPositioning(PdfFonts.spaced(text, wordSpacing, letterSpacing, fontSize, kerning));
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
