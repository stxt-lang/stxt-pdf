package dev.stxt.pdf.paint;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.model.Decoration;
import dev.stxt.pdf.model.DecorationMetrics;
import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.model.Radii;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.model.Sides;
import dev.stxt.pdf.model.VerticalMetrics;
import dev.stxt.pdf.model.WhiteSpace;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.Element.Role;
import dev.stxt.pdf.tree.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Árbol de contextos de apilamiento (referencia/comun/capas-y-giro.md), construido sobre los
 * fragmentos de una página. Es independiente del árbol de cajas.
 *
 * <p>Orden de pintado dentro de un contexto, de abajo arriba:</p>
 * <ol>
 *   <li>Fondo y borde de la caja que crea el contexto (y, en un {@code Multicol}, la línea entre
 *       columnas).</li>
 *   <li>Contextos hijos con {@code Layer} negativa, de menor a mayor.</li>
 *   <li>Cajas en flujo en orden del árbol: primero fondos y bordes, después el texto y las
 *       imágenes.</li>
 *   <li>Posicionadas sin {@code Layer} (o con 0), en orden del árbol.</li>
 *   <li>Contextos hijos con {@code Layer} positiva, de menor a mayor.</li>
 * </ol>
 *
 * <p>Simplificación: una caja posicionada sin {@code Layer} se pinta completa en el paso 4, con
 * sus descendientes; CSS promovería al contexto padre los descendientes que crean contexto.</p>
 *
 * <p>Una caja con {@code Rotate} (referencia/comun/capas-y-giro.md) se pinta girada con todo lo que contiene ({@link
 * #paint}). Si es posicionada, crea un contexto, que se gira entero; si gira en flujo, sus descendientes se reparten
 * entre los pasos del contexto que la contiene, y cada uno se pinta con los giros de sus ancestros girados en flujo
 * ({@code Piece.turners}).</p>
 *
 * <p>El "orden del árbol" es el del documento, no el de los fragmentos: las cajas
 * {@code absolute} cuelgan del fragmento de su containing block, detrás de sus hijos en flujo, y
 * las {@code fixed} se añaden al final, así que los contextos se ordenan por su caja.</p>
 */
public final class StackingContext {

    /**
     * Fragmento con su origen absoluto ya calculado y sus ancestros girados en flujo dentro del contexto, de fuera
     * adentro, cuyos giros se le aplican al pintarlo.
     */
    private record Piece(Fragment f, double x, double y, List<Piece> turners) {
        Rect borderBox() {
            return f.borderBox(x, y);
        }
    }

    private final Piece root;
    private final int layer;
    private final List<StackingContext> negative = new ArrayList<>();
    private final List<Piece> inFlow = new ArrayList<>();
    private final List<StackingContext> positionedZero = new ArrayList<>();
    private final List<StackingContext> positive = new ArrayList<>();

    private StackingContext(Piece root, int layer) {
        this.root = root;
        this.layer = layer;
    }

    /** Contexto raíz de una página; {@code originX/Y} es la esquina del área útil. */
    public static StackingContext build(Fragment root, double originX, double originY) {
        return build(root, List.of(), originX, originY);
    }

    /**
     * Contexto raíz de una página con sus cajas {@code fixed}, que crean contextos hijos del
     * raíz; sus posiciones son relativas al área útil.
     */
    public static StackingContext build(Fragment root, List<Fragment> fixed, double originX, double originY) {
        StackingContext sc = new StackingContext(new Piece(root, originX, originY, List.of()), 0);
        sc.walkChildren(sc.root, List.of());
        for (Fragment f : fixed) {
            sc.addContext(new Piece(f, originX + f.x(), originY + f.y(), List.of()));
        }
        sc.sort();
        return sc;
    }

    private void addContext(Piece p) {
        Style s = p.f().box().style();
        StackingContext child = new StackingContext(p, s.layer() != null ? s.layer() : 0);
        child.walkChildren(p, List.of());
        child.sort();
        if (child.layer < 0) {
            negative.add(child);
        } else if (child.layer > 0) {
            positive.add(child);
        } else {
            positionedZero.add(child);
        }
    }

    /** Reparte los descendientes de {@code parent}, que heredan los giros en flujo {@code turners}. */
    private void walkChildren(Piece parent, List<Piece> turners) {
        Rect content = parent.borderBox().shrink(parent.f().border()).shrink(parent.f().padding());
        for (Fragment h : parent.f().children()) {
            Piece p = new Piece(h, content.x() + h.x(), content.y() + h.y(), turners);
            Style s = h.box().style();
            if (createsContext(h)) {
                addContext(p);
            } else if (s.isPositioned()) {
                StackingContext pseudo = new StackingContext(p, 0);
                pseudo.walkChildren(p, List.of());
                pseudo.sort();
                positionedZero.add(pseudo);
            } else {
                inFlow.add(p);
                if (s.rotatesInFlow()) {
                    List<Piece> inner = new ArrayList<>(turners);
                    inner.add(p);
                    walkChildren(p, List.copyOf(inner));
                } else {
                    walkChildren(p, turners);
                }
            }
        }
    }

    private static boolean createsContext(Fragment f) {
        Style s = f.box().style();
        if (s.position() == Position.FIXED) {
            return true;
        }
        if (s.layer() == null) {
            return false;
        }
        Layout parent = f.box().parent() != null ? f.box().parent().style().layout() : null;
        boolean item = parent == Layout.FLEX || parent == Layout.GRID;
        return s.isPositioned() || item;
    }

    private void sort() {
        // A igual Layer, en orden del documento.
        Comparator<StackingContext> byLayer =
                Comparator.<StackingContext>comparingInt(s -> s.layer).thenComparing(TREE_ORDER);
        negative.sort(byLayer);
        positionedZero.sort(TREE_ORDER);
        positive.sort(byLayer);
    }

    private static final Comparator<StackingContext> TREE_ORDER =
            (a, b) -> compareTreeOrder(a.root.f().box(), b.root.f().box());

    /** Orden del documento (recorrido en preorden del árbol de cajas): un ancestro va antes. */
    static int compareTreeOrder(Box a, Box b) {
        List<Box> pathA = ancestry(a);
        List<Box> pathB = ancestry(b);
        int i = 0;
        while (i < pathA.size() && i < pathB.size() && pathA.get(i) == pathB.get(i)) {
            i++;
        }
        if (i == 0) {
            // Raíces distintas: un Fixed del documento y uno de una sección, que es la raíz de su layout
            // (referencia/componentes/document.md). Los del documento van antes.
            return Boolean.compare(pathA.get(0).element() != null, pathB.get(0).element() != null);
        }
        if (i == pathA.size() || i == pathB.size()) {
            return Integer.compare(pathA.size(), pathB.size());
        }
        List<Box> siblings = pathA.get(i - 1).children();
        return Integer.compare(siblings.indexOf(pathA.get(i)), siblings.indexOf(pathB.get(i)));
    }

    /** La caja y sus ancestros, desde la raíz. */
    private static List<Box> ancestry(Box box) {
        List<Box> path = new ArrayList<>();
        for (Box b = box; b != null; b = b.parent()) {
            path.add(b);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * Pinta el contexto. Si su caja está girada ({@code Rotate}, referencia/comun/capas-y-giro.md), lo pinta todo
     * girado: la caja y lo que contiene, también los contextos hijos. Una caja girada en flujo, y lo que contiene, se
     * gira en cada uno de los pasos en que se pinta.
     */
    public void paint(Canvas canvas) {
        turning(canvas, root, () -> {
            paintBackgroundAndBorder(canvas, root);
            negative.forEach(s -> s.paint(canvas));
            inFlow.forEach(p -> turning(canvas, p, () -> paintBackgroundAndBorder(canvas, p)));
            paintContent(canvas, root);
            inFlow.forEach(p -> turning(canvas, p, () -> paintContent(canvas, p)));
            positionedZero.forEach(s -> s.paint(canvas));
            positive.forEach(s -> s.paint(canvas));
        });
    }

    /** Pinta {@code body} con los giros de los ancestros girados en flujo de {@code p} y el suyo, si tiene {@code Rotate}. */
    private static void turning(Canvas canvas, Piece p, Runnable body) {
        int turns = 0;
        for (Piece t : p.turners()) {
            turns += beginRotation(canvas, t);
        }
        turns += beginRotation(canvas, p);
        body.run();
        for (int i = 0; i < turns; i++) {
            canvas.endRotation();
        }
    }

    /** Empieza el giro de la caja de {@code p}, si tiene {@code Rotate}; devuelve cuántos giros ha empezado. */
    private static int beginRotation(Canvas canvas, Piece p) {
        Rotation rotation = p.f().box().style().rotation();
        if (rotation == null) {
            return 0;
        }
        Rect borderBox = p.borderBox();
        canvas.beginRotation(rotation.degrees(), rotation.pivotX(borderBox), rotation.pivotY(borderBox));
        return 1;
    }

    private static void paintBackgroundAndBorder(Canvas canvas, Piece p) {
        Style s = p.f().box().style();
        Rect borderBox = p.borderBox();
        Radii radii = radii(p);
        if (s.box().background() != null) {
            canvas.fill(borderBox.shrink(p.f().border()), radii.inset(p.f().border()), s.box().background());
        }
        canvas.border(borderBox, p.f().border(), radii, borderColors(s));
        if (s.multicol().rule() > 0) {
            paintRules(canvas, p);
        }
    }

    /**
     * Las esquinas de la caja de borde del fragmento (referencia/comun/caja.md): {@code Radius} resuelto contra su
     * tamaño y, en una caja partida entre páginas con {@code Decoration: slice}, solo las de
     * arriba en el primer fragmento y las de abajo en el último.
     */
    private static Radii radii(Piece p) {
        Style s = p.f().box().style();
        Radii radii = Radii.resolve(s.box().radius(), p.borderBox().width(), p.borderBox().height());
        if (s.breaks().decoration() == Decoration.CLONE) {
            return radii;
        }
        return radii.forFragment(p.f().first(), p.f().last());
    }

    /** El color de cada lado del borde: el suyo o, si no lo declara, el del texto. */
    private static Sides<String> borderColors(Style s) {
        if (s.box().borderColor() == null) {
            return Sides.all(s.typography().color());
        }
        return new Sides<>(orText(s.box().borderColor().top(), s), orText(s.box().borderColor().right(), s),
                orText(s.box().borderColor().bottom(), s), orText(s.box().borderColor().left(), s));
    }

    private static String orText(String color, Style s) {
        return color != null ? color : s.typography().color();
    }

    /**
     * La línea entre cada dos columnas con contenido de un fragmento de un {@code Multicol}
     * (referencia/componentes/multicol.md): centrada en el hueco, desde arriba de la caja de contenido y con el alto de
     * la columna más alta. Sus hijos son las columnas, en orden.
     */
    private static void paintRules(Canvas canvas, Piece p) {
        Style s = p.f().box().style();
        List<Fragment> columns = p.f().children();
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        double height = columns.stream().mapToDouble(Fragment::height).max().orElse(0);
        String color = s.multicol().ruleColor() != null ? s.multicol().ruleColor() : s.typography().color();
        for (int i = 1; i < columns.size(); i++) {
            Fragment left = columns.get(i - 1);
            double middle = (left.x() + left.width() + columns.get(i).x()) / 2;
            canvas.background(new Rect(content.x() + middle - s.multicol().rule() / 2, content.y(), s.multicol().rule(), height),
                    color);
        }
    }

    /**
     * El contenido de una caja: sus líneas de texto o, en una {@code Image}, la imagen
     * (referencia/componentes/image.md), y después su enlace.
     */
    private static void paintContent(Canvas canvas, Piece p) {
        if (p.f().box().isImage()) {
            Insets inside = new Insets(p.f().border().top() + p.f().padding().top(),
                    p.f().border().right() + p.f().padding().right(),
                    p.f().border().bottom() + p.f().padding().bottom(),
                    p.f().border().left() + p.f().padding().left());
            canvas.image(p.f().box(), p.borderBox().shrink(inside), radii(p).inset(inside));
        } else if (p.f().box().role() == Role.LEADER) {
            paintLeader(canvas, p);
        } else {
            paintText(canvas, p);
        }
        paintLinks(canvas, p);
    }

    /**
     * La zona clicable de la caja entera (referencia/comun/enlaces.md): su caja de borde si la caja tiene {@code Link}.
     * Las de los tramos con enlace de sus líneas de texto las pinta {@link #paintText}, dentro del contenido del
     * enlace. Una caja partida entre páginas tiene una zona por fragmento.
     */
    private static void paintLinks(Canvas canvas, Piece p) {
        Box box = p.f().box();
        if (box.style().link() != null && p.f().width() > 0 && p.f().height() > 0) {
            canvas.tagging().link(box, p.borderBox(), box.style().link(), describe(box));
        }
    }

    /**
     * Lo que dice la caja con enlace, para quien no la ve: el texto alternativo de una imagen o el
     * texto de la caja y de lo que contiene (sin el número de página de las entradas de un
     * índice); {@code null} si no tiene texto.
     */
    private static String describe(Box box) {
        if (box.style().image().alt() != null) {
            return box.style().image().alt();
        }
        StringBuilder out = new StringBuilder();
        collectText(box, out);
        String text = out.toString().replaceAll("\\s+", " ").strip();
        return text.isEmpty() ? null : text;
    }

    private static void collectText(Box box, StringBuilder out) {
        if (box.role() == Role.PAGE_NUMBER) {
            return;
        }
        if (box.hasText()) {
            out.append(box.text()).append(' ');
        }
        box.children().forEach(child -> collectText(child, out));
    }

    /** Zona de un tramo de la línea {@code l}, desde {@code from} hasta {@code to} (relativos a la línea). */
    private static Rect linkArea(Rect content, TextLine l, double from, double to) {
        return new Rect(content.x() + l.x() + from, content.y() + l.y(), to - from, l.height());
    }

    /**
     * La guía de una entrada de índice (referencia/componentes/contents.md): puntos o una línea sobre la línea base del
     * texto de su entrada, con su color. Como los fondos, no es contenido del PDF etiquetado.
     */
    private static void paintLeader(Canvas canvas, Piece p) {
        Style s = p.f().box().style();
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        double baseline = canvas.vertical(s.typography().fontFace()).baseline(content.y(), s.typography().usedLineHeight(), s.typography().fontSize());
        for (Rect shape : Leaders.shapes(s.contents().leader(), content, baseline, s.typography().fontSize())) {
            canvas.background(shape, s.typography().color());
        }
    }

    /**
     * Las líneas de texto, cada tramo con su fuente (referencia/comun/formato-del-texto.md). Como en CSS, el subrayado
     * se pinta antes que el texto y el tachado después; los dos fuera del contenido marcado, porque no son texto (en el
     * PDF etiquetado, <em>artifacts</em>).
     *
     * <p>El texto se pinta en secuencias de tramos seguidos con el mismo enlace, también de una línea
     * a la siguiente ({@link Canvas#beginText} si no tienen, {@link Canvas#beginLinkText} si lo tienen): cada secuencia
     * es un contenido marcado aparte, y el de un enlace cuelga del {@code Link} del PDF etiquetado
     * (referencia/comun/enlaces.md). Dentro de cada una de enlace, una zona clicable por línea ({@link
     * #paintLinkZone}).</p>
     */
    private static void paintText(Canvas canvas, Piece p) {
        if (p.f().lines().isEmpty()) {
            return;
        }
        Box box = p.f().box();
        Style s = box.style();
        FontFace font = s.typography().fontFace();
        VerticalMetrics metrics = canvas.vertical(font);
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        paintDecorations(canvas, p, content, metrics, true);
        Tagging tags = canvas.tagging();
        // Con interletraje, quien extrae el texto adivina espacios entre las letras: cada secuencia de tramos de una
        // línea lleva su texto aparte (referencia/implementacion/pdf.md).
        boolean spaced = s.typography().letterSpacing() != 0;
        boolean open = false;
        String current = null;
        List<TextLine> lines = p.f().lines();
        for (int n = 0; n < lines.size(); n++) {
            TextLine l = lines.get(n);
            // El corte de línea se come el espacio: se escribe invisible tras la última palabra, para que quien
            // une el texto de las líneas (lector de pantalla, extracción, búsqueda) no pegue las palabras.
            boolean spaceAfter = n < lines.size() - 1 && s.typography().whiteSpace() == WhiteSpace.NORMAL;
            double baseline = metrics.baseline(content.y() + l.y(), l.height(), s.typography().fontSize());
            List<TextLine.Run> runs = l.runs();
            for (int i = 0; i < runs.size(); i++) {
                TextLine.Run r = runs.get(i);
                String link = r.format().link();
                if (!open || !Objects.equals(link, current)) {
                    if (open) {
                        tags.endText();
                    }
                    if (link == null) {
                        tags.beginText(box);
                    } else {
                        tags.beginLinkText(box);
                    }
                    open = true;
                    current = link;
                }
                boolean first = i == 0 || !Objects.equals(link, runs.get(i - 1).format().link());
                if (link != null && first) {
                    paintLinkZone(tags, box, content, l, i, link);
                }
                if (spaced && first) {
                    tags.beginActualText(sameLinkText(runs, i, spaceAfter));
                }
                String shown = spaceAfter && i == runs.size() - 1 ? r.text() + " " : r.text();
                canvas.text(content.x() + l.x() + r.x(), baseline, shown, r.format().face(font),
                        s.typography().fontSize() * r.format().scale(), s.typography().color(), l.wordSpacing(), s.typography().letterSpacing());
                if (spaced && (i == runs.size() - 1 || !Objects.equals(link, runs.get(i + 1).format().link()))) {
                    tags.endActualText();
                }
            }
        }
        if (open) {
            tags.endText();
        }
        paintDecorations(canvas, p, content, metrics, false);
    }

    /**
     * El texto de los tramos de una línea que empiezan en {@code first} y van seguidos con el mismo enlace, con el
     * espacio invisible del final si llegan al final de la línea y lo lleva ({@code spaceAfter}).
     */
    private static String sameLinkText(List<TextLine.Run> runs, int first, boolean spaceAfter) {
        StringBuilder text = new StringBuilder();
        String link = runs.get(first).format().link();
        int end = first;
        while (end < runs.size() && Objects.equals(link, runs.get(end).format().link())) {
            text.append(runs.get(end).text());
            end++;
        }
        return spaceAfter && end == runs.size() ? text.append(' ').toString() : text.toString();
    }

    /**
     * La zona clicable (referencia/comun/enlaces.md) de los tramos de la línea {@code l} que empiezan en {@code first}
     * y van seguidos con el mismo destino (una parte en negrita dentro de un enlace es una sola zona).
     */
    private static void paintLinkZone(Tagging tags, Box box, Rect content, TextLine l, int first,
                                      String destination) {
        List<TextLine.Run> runs = l.runs();
        StringBuilder text = new StringBuilder();
        int end = first;
        while (end < runs.size() && destination.equals(runs.get(end).format().link())) {
            text.append(runs.get(end).text());
            end++;
        }
        TextLine.Run last = runs.get(end - 1);
        tags.link(box, linkArea(content, l, runs.get(first).x(), last.x() + last.width()), destination,
                text.toString());
    }

    /** Rayas de subrayado ({@code underline}) o de tachado de los tramos, con el color del texto. */
    private static void paintDecorations(Canvas canvas, Piece p, Rect content, VerticalMetrics metrics,
                                         boolean underline) {
        Style s = p.f().box().style();
        for (TextLine l : p.f().lines()) {
            double baseline = metrics.baseline(content.y() + l.y(), l.height(), s.typography().fontSize());
            for (TextLine.Run r : l.runs()) {
                if (underline ? !r.format().underline() : !r.format().strike()) {
                    continue;
                }
                DecorationMetrics d = canvas.decoration(r.format().face(s.typography().fontFace()));
                double x = content.x() + l.x() + r.x();
                canvas.background(underline ? d.underline(x, baseline, r.width(), s.typography().fontSize())
                        : d.strikeout(x, baseline, r.width(), s.typography().fontSize()), s.typography().color());
            }
        }
    }
}
