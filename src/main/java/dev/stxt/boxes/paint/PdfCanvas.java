package dev.stxt.boxes.paint;

import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.LaidOutDocument;
import dev.stxt.boxes.model.Colors;
import dev.stxt.boxes.model.DecorationMetrics;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Paint;
import dev.stxt.boxes.model.Radii;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.Rotation;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.VerticalMetrics;
import dev.stxt.boxes.resources.ImageFile;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Media;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDPageLabelRange;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDFormContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.function.PDFunction;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroup;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.util.Matrix;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Salida PDF con PDFBox: una página PDF por página del resultado, en pt, del tamaño del medio.
 *
 * <p>El texto se pinta con las fuentes TTF de la carpeta de recursos (ESPECIFICACION.md §5.11),
 * incrustadas como subconjunto (solo los glifos usados) en una fuente Type 0 con codificación
 * Identity-H: el PDF se ve igual en cualquier máquina. Para que las líneas midan lo mismo que al
 * maquetar, el layout debe usar las mismas métricas: {@code new Engine(new PdfMetrics(resources))}.
 * PDF tiene el origen abajo a la izquierda: las coordenadas verticales se invierten al pintar.</p>
 *
 * <p>La justificación no puede usar el operador {@code Tw} de PDF, que solo afecta al byte 32 y
 * con Identity-H cada carácter ocupa dos: el espacio extra se añade tras cada espacio con
 * {@code TJ}.</p>
 *
 * <p>Cada imagen (ESPECIFICACION.md §5.18) se incrusta una sola vez aunque se use en varias
 * páginas, sin remuestrear: el JPEG tal cual y el PNG sin pérdida, con su transparencia; el SVG,
 * como dibujo vectorial ({@link PdfImages}).</p>
 *
 * <p>El PDF va etiquetado (árbol de estructura y contenido marcado) y lleva los marcadores de
 * los encabezados: ver {@link PdfStructure}.</p>
 *
 * <p>Con {@code debug}, como {@link SvgCanvas}, dibuja además el contorno de cada caja de borde
 * (lo usan las imágenes de los tests de referencia).</p>
 */
public final class PdfCanvas implements Canvas {

    private final PDDocument doc;
    private final PDPageContentStream out;
    private final double pageHeight;
    private final Resources resources;
    /** Fuentes incrustadas en el documento, compartidas por todas sus páginas. */
    private final Map<FontFace, PDFont> fonts;
    /** Imágenes incrustadas en el documento, por fichero, compartidas por todas sus páginas. */
    private final PdfImages images;
    /** Árbol de estructura del documento, compartido por todas sus páginas. */
    private final PdfStructure structure;
    /** Anotaciones de enlace del documento, compartidas por todas sus páginas. */
    private final PdfLinks links;
    /** Las anotaciones de esta página, que se le ponen al terminar de pintarla. */
    private final List<PDAnnotation> annotations = new ArrayList<>();
    private final boolean debug;
    /** Estados gráficos de opacidad de esta página, por opacidad (en milésimas). */
    private final Map<Integer, PDExtendedGraphicsState> opacities = new HashMap<>();
    /**
     * Los giros en curso ({@link #beginRotation}), compuestos, en coordenadas de la página (con la
     * y hacia abajo); lo necesitan las zonas de los enlaces y el {@code BBox} de las figuras, que
     * no siguen la matriz del contenido.
     */
    private AffineTransform turn = new AffineTransform();
    private final Deque<AffineTransform> turns = new ArrayDeque<>();

    private PdfCanvas(PDDocument doc, PDPageContentStream out, double pageHeight, Resources resources,
                      Map<FontFace, PDFont> fonts, PdfImages images, PdfStructure structure,
                      PdfLinks links, boolean debug) {
        this.doc = doc;
        this.out = out;
        this.links = links;
        this.pageHeight = pageHeight;
        this.resources = resources;
        this.fonts = fonts;
        this.images = images;
        this.structure = structure;
        this.debug = debug;
    }

    /**
     * Pinta un documento maquetado con {@code new PdfMetrics(resources)}, con el título y el
     * idioma de sus metadatos, y escribe el PDF en {@code target}, que no se cierra. Para maquetar
     * y pintar en una llamada está {@link PdfWriter}.
     */
    public static void write(LaidOutDocument document, Resources resources, OutputStream target, boolean debug)
            throws IOException {
        List<Page> pages = document.pages();
        try (PDDocument doc = new PDDocument()) {
            Map<FontFace, PDFont> fonts = new HashMap<>();
            PdfImages images = new PdfImages(doc, resources);
            PdfStructure structure = new PdfStructure(doc, document.root());
            PdfLinks links = new PdfLinks();
            PdfStructure.metadata(doc, document.metadata());
            // Todas las páginas existen antes de pintar ninguna: un enlace puede ir hacia delante.
            List<PDPage> pdfPages = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                Media media = pages.get(i).media();
                PDPage page = new PDPage(new PDRectangle((float) media.width(), (float) media.height()));
                doc.addPage(page);
                pdfPages.add(page);
            }
            for (int i = 0; i < pages.size(); i++) {
                Page p = pages.get(i);
                // Cada página tiene el medio de su sección (§5.21).
                Media media = p.media();
                Rect area = media.pageArea();
                PDPage page = pdfPages.get(i);
                structure.startPage(page);
                PdfCanvas canvas;
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    canvas = new PdfCanvas(doc, cs, media.height(), resources, fonts, images, structure, links,
                            debug);
                    StackingContext.build(p.content(), p.fixed(), area.x(), area.y()).paint(canvas);
                }
                if (!canvas.annotations.isEmpty()) {
                    page.setAnnotations(canvas.annotations);
                    // Las anotaciones se recorren en el orden de la estructura (PDF/UA, 7.18.3).
                    page.getCOSObject().setName(COSName.getPDFName("Tabs"), "S");
                }
            }
            labels(doc, pages);
            Map<Box, PDPageXYZDestination> destinations = PdfDestinations.find(pages, pdfPages);
            links.resolve(destinations);
            structure.finish(doc, destinations);
            doc.save(target);
        }
    }

    /**
     * Las etiquetas de las páginas (§5.21): el visor muestra el número impreso de cada una, y ninguno en las
     * que no se numeran. Solo se escriben si alguna página no es su número físico; si no, no añaden nada.
     * Una etiqueta nueva empieza donde el número no sigue al de la página anterior.
     */
    private static void labels(PDDocument doc, List<Page> pages) {
        if (pages.stream().allMatch(p -> p.printed() == p.number())) {
            return;
        }
        PDPageLabels labels = new PDPageLabels(doc);
        Page previous = null;
        for (int i = 0; i < pages.size(); i++) {
            Page p = pages.get(i);
            boolean follows = previous != null
                    && (p.numbered() ? previous.numbered() && p.printed() == previous.printed() + 1 : !previous.numbered());
            if (!follows) {
                PDPageLabelRange range = new PDPageLabelRange();
                if (p.numbered()) {
                    range.setStyle(PDPageLabelRange.STYLE_DECIMAL);
                    range.setStart(p.printed());
                }
                labels.setLabelItem(i, range);
            }
            previous = p;
        }
        doc.getDocumentCatalog().setPageLabels(labels);
    }

    @Override
    public void background(Rect r, String color) {
        if (r.width() <= 0 || r.height() <= 0) {
            return;
        }
        try {
            // Fondos y bordes no son contenido: artifacts del PDF etiquetado.
            out.beginMarkedContent(COSName.ARTIFACT);
            painting(color, () -> {
                out.addRect((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
                out.fill();
            });
            out.endMarkedContent();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        try {
            out.beginMarkedContent(COSName.ARTIFACT);
            if (paint instanceof Paint.Solid solid) {
                painting(solid.color(), () -> {
                    Shapes.roundedRect(path(), r, radii);
                    out.fill();
                });
            } else {
                gradient((Paint.Linear) paint, r, radii);
            }
            out.endMarkedContent();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void gradient(Paint.Linear linear, Rect r, Radii radii) throws IOException {
        double[] line = linear.line(r);
        double[] pdfLine = {line[0], pageHeight - line[1], line[2], pageHeight - line[3]};
        out.saveGraphicsState();
        Shapes.roundedRect(path(), r, radii);
        out.clip();
        if (linear.isTranslucent()) {
            out.setGraphicsStateParameters(opacityMask(linear, r, pdfLine));
        }
        out.shadingFill(axial(PDDeviceRGB.INSTANCE, pdfLine, linear.stops(), false));
        out.restoreGraphicsState();
    }

    /** Un estado gráfico con una máscara suave de luminosidad: el degradado de las opacidades en grises. */
    private PDExtendedGraphicsState opacityMask(Paint.Linear linear, Rect r, double[] pdfLine) throws IOException {
        PDTransparencyGroup form = new PDTransparencyGroup(doc);
        form.setBBox(pdfRect(r));
        form.setResources(new PDResources());
        COSDictionary group = new COSDictionary();
        group.setItem(COSName.S, COSName.getPDFName("Transparency"));
        group.setItem(COSName.getPDFName("CS"), COSName.DEVICEGRAY);
        form.getCOSObject().setItem(COSName.getPDFName("Group"), group);
        try (PDFormContentStream mask = new PDFormContentStream(form)) {
            mask.shadingFill(axial(PDDeviceGray.INSTANCE, pdfLine, linear.stops(), true));
        }
        COSDictionary softMask = new COSDictionary();
        softMask.setItem(COSName.TYPE, COSName.getPDFName("Mask"));
        softMask.setItem(COSName.S, COSName.getPDFName("Luminosity"));
        softMask.setItem(COSName.getPDFName("G"), form.getCOSObject());
        PDExtendedGraphicsState state = new PDExtendedGraphicsState();
        state.getCOSObject().setItem(COSName.getPDFName("SMask"), softMask);
        return state;
    }

    /**
     * Un sombreado axial de {@code line} ({@code x1 y1 x2 y2}, en coordenadas del PDF) con los
     * colores repartidos a partes iguales y extendido más allá de los extremos; con
     * {@code opacities}, en grises con la opacidad de cada color.
     */
    private static PDShading axial(PDColorSpace space, double[] line, java.util.List<String> stops,
                                   boolean opacities) throws IOException {
        PDShadingType2 shading = new PDShadingType2(new COSDictionary());
        shading.setShadingType(PDShading.SHADING_TYPE2);
        shading.setColorSpace(space);
        COSArray coords = new COSArray();
        for (double c : line) {
            coords.add(new org.apache.pdfbox.cos.COSFloat((float) c));
        }
        shading.setCoords(coords);
        COSArray extend = new COSArray();
        extend.add(COSBoolean.TRUE);
        extend.add(COSBoolean.TRUE);
        shading.setExtend(extend);
        shading.setFunction(PDFunction.create(stitched(stops, opacities)));
        return shading;
    }

    /** Los componentes de un color en el espacio del sombreado: RGB, o su opacidad en gris. */
    private static float[] components(String color, boolean opacity) {
        float[] rgba = Colors.rgba(color);
        return opacity ? new float[]{rgba[3]} : new float[]{rgba[0], rgba[1], rgba[2]};
    }

    /** Una función por tramo entre dos colores, unidas en una de tipo 3 (o la del único tramo). */
    private static COSDictionary stitched(java.util.List<String> stops, boolean opacities) {
        int n = stops.size();
        COSArray domain = floats(0, 1);
        java.util.List<COSDictionary> parts = new ArrayList<>();
        for (int i = 0; i + 1 < n; i++) {
            COSDictionary f = new COSDictionary();
            f.setInt(COSName.getPDFName("FunctionType"), 2);
            f.setItem(COSName.getPDFName("Domain"), domain);
            f.setItem(COSName.getPDFName("C0"), floats(components(stops.get(i), opacities)));
            f.setItem(COSName.getPDFName("C1"), floats(components(stops.get(i + 1), opacities)));
            f.setInt(COSName.getPDFName("N"), 1);
            parts.add(f);
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        COSDictionary f = new COSDictionary();
        f.setInt(COSName.getPDFName("FunctionType"), 3);
        f.setItem(COSName.getPDFName("Domain"), domain);
        COSArray functions = new COSArray();
        COSArray bounds = new COSArray();
        COSArray encode = new COSArray();
        for (int i = 0; i < parts.size(); i++) {
            functions.add(parts.get(i));
            if (i > 0) {
                bounds.add(new org.apache.pdfbox.cos.COSFloat((float) i / parts.size()));
            }
            encode.add(new org.apache.pdfbox.cos.COSFloat(0));
            encode.add(new org.apache.pdfbox.cos.COSFloat(1));
        }
        f.setItem(COSName.getPDFName("Functions"), functions);
        f.setItem(COSName.getPDFName("Bounds"), bounds);
        f.setItem(COSName.getPDFName("Encode"), encode);
        return f;
    }

    private static COSArray floats(float... values) {
        COSArray array = new COSArray();
        for (float v : values) {
            array.add(new org.apache.pdfbox.cos.COSFloat(v));
        }
        return array;
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
        try {
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
                    outline(r);
                    return;
                }
                out.beginMarkedContent(COSName.ARTIFACT);
                if (same) {
                    String color = uniform;
                    painting(color, () -> ring(r, w, radii));
                } else {
                    Shapes.Side[] sides = Shapes.Side.values();
                    for (int i = 0; i < 4; i++) {
                        if (widths[i] > 0) {
                            Shapes.Side side = sides[i];
                            out.saveGraphicsState();
                            Shapes.sidePolygon(path(), side, r, w, radii.inset(w));
                            out.clip();
                            painting(sideColors[i], () -> ring(r, w, radii));
                            out.restoreGraphicsState();
                        }
                    }
                }
                out.endMarkedContent();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        outline(r);
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
        Shapes.roundedRect(path(), r, radii);
        Rect inner = r.shrink(w);
        if (inner.width() > 0 && inner.height() > 0) {
            Shapes.roundedRect(path(), inner, radii.inset(w));
        }
        out.fillEvenOdd();
    }

    /** Un camino que se escribe en el contenido de la página, con el origen abajo. */
    private Shapes.Path path() {
        return new Shapes.Path() {
            @Override
            public void moveTo(double x, double y) {
                run(() -> out.moveTo((float) x, (float) (pageHeight - y)));
            }

            @Override
            public void lineTo(double x, double y) {
                run(() -> out.lineTo((float) x, (float) (pageHeight - y)));
            }

            @Override
            public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
                run(() -> out.curveTo((float) x1, (float) (pageHeight - y1), (float) x2, (float) (pageHeight - y2),
                        (float) x3, (float) (pageHeight - y3)));
            }

            @Override
            public void close() {
                run(out::closePath);
            }

            @Override
            public void rect(double x, double y, double width, double height) {
                run(() -> out.addRect((float) x, (float) (pageHeight - y - height), (float) width, (float) height));
            }
        };
    }

    /** Una operación del PDF que puede fallar al escribir. */
    @FunctionalInterface
    private interface Operation {
        void run() throws IOException;
    }

    private static void run(Operation operation) {
        try {
            operation.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Pinta con el color {@code color} lo que haga {@code body}; si el color no es opaco, dentro de
     * un estado gráfico propio con su opacidad.
     */
    private void painting(String color, Operation body) throws IOException {
        float[] rgba = Colors.rgba(color);
        boolean translucent = rgba[3] < 1f;
        if (translucent) {
            out.saveGraphicsState();
            out.setGraphicsStateParameters(opacity(rgba[3]));
        }
        out.setNonStrokingColor(rgba[0], rgba[1], rgba[2]);
        body.run();
        if (translucent) {
            out.restoreGraphicsState();
        }
    }

    private PDExtendedGraphicsState opacity(float alpha) {
        return opacities.computeIfAbsent(Math.round(alpha * 1000), key -> {
            PDExtendedGraphicsState state = new PDExtendedGraphicsState();
            state.setNonStrokingAlphaConstant(alpha);
            return state;
        });
    }

    /** Si lo que se pinta es el texto de un enlace ({@link #beginLinkText}): sus zonas cuelgan de su {@code Link}. */
    private boolean linkText;

    @Override
    public void beginLinkText(Box box) {
        try {
            int mcid = structure.markLink(box);
            linkText = true;
            if (mcid < 0) {
                out.beginMarkedContent(COSName.ARTIFACT);
            } else {
                out.beginMarkedContent(COSName.getPDFName("Link"), mcid);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void beginText(Box box) {
        try {
            int mcid = structure.mark(box);
            if (mcid < 0) {
                out.beginMarkedContent(COSName.ARTIFACT);
            } else {
                out.beginMarkedContent(COSName.getPDFName(PdfStructure.type(box)), mcid);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        try {
            ImageFile file = resources.images().find(box.image);
            Rect target = ImageFit.place(box.style.fit, content, file.width(), file.height());
            int mcid = structure.mark(box);
            if (mcid < 0) {
                out.beginMarkedContent(COSName.ARTIFACT);
            } else {
                out.beginMarkedContent(COSName.getPDFName(PdfStructure.type(box)), mcid);
                structure.figureBox(box, turnedRect(content));
            }
            out.saveGraphicsState();
            Shapes.roundedRect(path(), content, radii);
            out.clip();
            if (file.isVector()) {
                // El dibujo mide lo que el SVG: se escala a donde va (Fit) y vuelve a ser vectorial.
                out.transform(new Matrix((float) (target.width() / file.width()), 0, 0,
                        (float) (target.height() / file.height()), (float) target.x(),
                        (float) (pageHeight - target.bottom())));
                // Un formulario hereda el estado gráfico y el dibujo no fija los guiones: sin guiones.
                out.setLineDashPattern(new float[0], 0);
                out.drawForm(images.drawing(file));
            } else {
                out.drawImage(images.bitmap(file), (float) target.x(), (float) (pageHeight - target.bottom()),
                        (float) target.width(), (float) target.height());
            }
            out.restoreGraphicsState();
            out.endMarkedContent();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Una matriz {@code cm} dentro de un {@code q}/{@code Q}. Con el origen abajo, el giro en el
     * sentido de las agujas del reloj es un ángulo negativo.</p>
     */
    @Override
    public void beginRotation(double degrees, double x, double y) {
        turns.push(turn);
        turn = new AffineTransform(turn);
        turn.rotate(Math.toRadians(degrees), x, y);
        try {
            out.saveGraphicsState();
            out.transform(new Matrix(AffineTransform.getRotateInstance(-Math.toRadians(degrees), x, pageHeight - y)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void endRotation() {
        turn = turns.pop();
        try {
            out.restoreGraphicsState();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** El rectángulo de la página que ocupa {@code r} con los giros en curso, en coordenadas del PDF. */
    private PDRectangle turnedRect(Rect r) {
        return pdfRect(turn.isIdentity() ? r : Rotation.envelope(turn, r));
    }

    /** Un rectángulo en coordenadas del PDF, con el origen abajo. */
    private PDRectangle pdfRect(Rect r) {
        return new PDRectangle((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Una anotación {@code Link} sin borde; la estructura la cuelga de la caja ({@link PdfStructure#link}).
     * Girada, su rectángulo es el que envuelve la zona y sus {@code QuadPoints}, la zona girada: un
     * visor que no los entiende la hace clicable entera.</p>
     */
    @Override
    public void link(Box box, Rect area, String destination, String description) {
        PDAnnotationLink annotation = links.create(turnedRect(area), destination, description);
        if (!turn.isIdentity()) {
            // Las cuatro esquinas en sentido antihorario desde abajo a la izquierda, con el origen abajo.
            double[] c = Rotation.corners(turn, area);
            float[] quad = new float[8];
            int[] order = {3, 2, 1, 0};
            for (int i = 0; i < 4; i++) {
                quad[2 * i] = (float) c[2 * order[i]];
                quad[2 * i + 1] = (float) (pageHeight - c[2 * order[i] + 1]);
            }
            annotation.setQuadPoints(quad);
        }
        annotations.add(annotation);
        if (linkText) {
            structure.inlineLink(annotation, description);
        } else {
            structure.link(box, annotation, description);
        }
    }

    @Override
    public void endText() {
        linkText = false;
        try {
            out.endMarkedContent();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void outline(Rect r) {
        if (debug) {
            try {
                out.beginMarkedContent(COSName.ARTIFACT);
                out.setStrokingColor(0xDD / 255f, 0x33 / 255f, 0x33 / 255f);
                out.setLineWidth(0.3f);
                out.setLineDashPattern(new float[]{2, 2}, 0);
                out.addRect((float) r.x(), (float) (pageHeight - r.bottom()), (float) r.width(), (float) r.height());
                out.stroke();
                out.endMarkedContent();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public void text(double x, double baselineY, String text, FontFace font, double fontSize, String color,
                     double wordSpacing, double letterSpacing) {
        try {
            painting(color, () -> {
                out.beginText();
                out.setFont(pdfFont(font), (float) fontSize);
                out.newLineAtOffset((float) x, (float) (pageHeight - baselineY));
                if (wordSpacing == 0 && letterSpacing == 0) {
                    out.showText(text);
                } else {
                    out.showTextWithPositioning(spaced(text, wordSpacing, letterSpacing, fontSize));
                }
                out.endText();
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public VerticalMetrics vertical(FontFace font) {
        return resources.fonts().find(font).vertical();
    }

    @Override
    public DecorationMetrics decoration(FontFace font) {
        return resources.fonts().find(font).decoration();
    }

    /** La fuente incrustada; se carga la primera vez que se usa en el documento. */
    private PDFont pdfFont(FontFace font) throws IOException {
        PDFont f = fonts.get(font);
        if (f == null) {
            f = PDType0Font.load(doc, resources.fonts().find(font).path().toFile());
            fonts.put(font, f);
        }
        return f;
    }

    /**
     * Argumento de {@code TJ}: el texto partido tras cada espacio, con el espacio extra entre
     * los trozos. Un número en {@code TJ} desplaza a la izquierda, en milésimas del cuerpo.
     */
    static Object[] justified(String text, double wordSpacing, double fontSize) {
        return spaced(text, wordSpacing, 0, fontSize);
    }

    /**
     * Argumento de {@code TJ} con el espacio extra de {@code justify} tras cada espacio y el
     * interletraje tras cada carácter, salvo el último. Sin interletraje, el texto se parte solo
     * tras los espacios; con él, un trozo por carácter. Se usa {@code TJ} y no {@code Tc}/{@code Tw}
     * porque las fuentes son Type 0 Identity-H, donde {@code Tw} no actúa.
     */
    static Object[] spaced(String text, double wordSpacing, double letterSpacing, double fontSize) {
        if (letterSpacing != 0) {
            List<Object> parts = new ArrayList<>();
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                parts.add(new String(Character.toChars(cp)));
                if (i < text.length()) {
                    parts.add((float) (-(letterSpacing + (cp == ' ' ? wordSpacing : 0)) * 1000 / fontSize));
                }
            }
            return parts.toArray();
        }
        Float shift = (float) (-wordSpacing * 1000 / fontSize);
        List<Object> parts = new ArrayList<>();
        int start = 0;
        for (int i = text.indexOf(' '); i >= 0; i = text.indexOf(' ', i + 1)) {
            parts.add(text.substring(start, i + 1));
            parts.add(shift);
            start = i + 1;
        }
        parts.add(text.substring(start));
        return parts.toArray();
    }

    /** {@code #RGB}, {@code #RGBA}, {@code #RRGGBB} o {@code #RRGGBBAA} (ya validado) en componentes de 0 a 1. */
    static float[] rgb(String color) {
        float[] rgba = Colors.rgba(color);
        return new float[]{rgba[0], rgba[1], rgba[2]};
    }
}
