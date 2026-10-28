package dev.stxt.boxes.paint;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.model.DecorationMetrics;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.VerticalMetrics;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Element;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Árbol de contextos de apilamiento (módulo 4, ESPECIFICACION.md §5.6), construido sobre los
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
 * <p>El "orden del árbol" es el del documento, no el de los fragmentos: las cajas
 * {@code absolute} cuelgan del fragmento de su containing block, detrás de sus hijos en flujo, y
 * las {@code fixed} se añaden al final, así que los contextos se ordenan por su caja.</p>
 */
public final class StackingContext {

    /** Fragmento con su origen absoluto ya calculado. */
    private record Piece(Fragment f, double x, double y) {
        Rect borderBox() {
            return new Rect(x, y, f.width(), f.height());
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
        StackingContext sc = new StackingContext(new Piece(root, originX, originY), 0);
        sc.walkChildren(sc.root);
        for (Fragment f : fixed) {
            sc.addContext(new Piece(f, originX + f.x(), originY + f.y()));
        }
        sc.sort();
        return sc;
    }

    private void addContext(Piece p) {
        Style s = p.f().box().style;
        StackingContext child = new StackingContext(p, s.layer != null ? s.layer : 0);
        child.walkChildren(p);
        child.sort();
        if (child.layer < 0) {
            negative.add(child);
        } else if (child.layer > 0) {
            positive.add(child);
        } else {
            positionedZero.add(child);
        }
    }

    private void walkChildren(Piece parent) {
        Rect content = parent.borderBox().shrink(parent.f().border()).shrink(parent.f().padding());
        for (Fragment h : parent.f().children()) {
            Piece p = new Piece(h, content.x() + h.x(), content.y() + h.y());
            Style s = h.box().style;
            if (createsContext(h)) {
                addContext(p);
            } else if (s.isPositioned()) {
                StackingContext pseudo = new StackingContext(p, 0);
                pseudo.walkChildren(p);
                pseudo.sort();
                positionedZero.add(pseudo);
            } else {
                inFlow.add(p);
                walkChildren(p);
            }
        }
    }

    private static boolean createsContext(Fragment f) {
        Style s = f.box().style;
        if (s.position == Style.Position.FIXED) {
            return true;
        }
        if (s.layer == null) {
            return false;
        }
        Style.Layout parent = f.box().parent != null ? f.box().parent.style.layout : null;
        boolean item = parent == Style.Layout.FLEX || parent == Style.Layout.GRID;
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
        if (i == pathA.size() || i == pathB.size()) {
            return Integer.compare(pathA.size(), pathB.size());
        }
        List<Box> siblings = pathA.get(i - 1).children;
        return Integer.compare(siblings.indexOf(pathA.get(i)), siblings.indexOf(pathB.get(i)));
    }

    /** La caja y sus ancestros, desde la raíz. */
    private static List<Box> ancestry(Box box) {
        List<Box> path = new ArrayList<>();
        for (Box b = box; b != null; b = b.parent) {
            path.add(b);
        }
        Collections.reverse(path);
        return path;
    }

    public void paint(Canvas canvas) {
        paintBackgroundAndBorder(canvas, root);
        negative.forEach(s -> s.paint(canvas));
        inFlow.forEach(p -> paintBackgroundAndBorder(canvas, p));
        paintContent(canvas, root);
        inFlow.forEach(p -> paintContent(canvas, p));
        positionedZero.forEach(s -> s.paint(canvas));
        positive.forEach(s -> s.paint(canvas));
    }

    private static void paintBackgroundAndBorder(Canvas canvas, Piece p) {
        Style s = p.f().box().style;
        Rect borderBox = p.borderBox();
        if (s.background != null) {
            canvas.background(borderBox.shrink(p.f().border()), s.background);
        }
        canvas.border(borderBox, p.f().border(), s.borderColor != null ? s.borderColor : s.color);
        if (s.multicolRule > 0) {
            paintRules(canvas, p);
        }
    }

    /**
     * La línea entre cada dos columnas con contenido de un fragmento de un {@code Multicol}
     * (§5.16): centrada en el hueco, desde arriba de la caja de contenido y con el alto de la
     * columna más alta. Sus hijos son las columnas, en orden.
     */
    private static void paintRules(Canvas canvas, Piece p) {
        Style s = p.f().box().style;
        List<Fragment> columns = p.f().children();
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        double height = columns.stream().mapToDouble(Fragment::height).max().orElse(0);
        String color = s.multicolRuleColor != null ? s.multicolRuleColor : s.color;
        for (int i = 1; i < columns.size(); i++) {
            Fragment left = columns.get(i - 1);
            double middle = (left.x() + left.width() + columns.get(i).x()) / 2;
            canvas.background(new Rect(content.x() + middle - s.multicolRule / 2, content.y(), s.multicolRule, height),
                    color);
        }
    }

    /** El contenido de una caja: sus líneas de texto o, en una {@code Image}, la imagen (§5.18), y sus enlaces. */
    private static void paintContent(Canvas canvas, Piece p) {
        paintLinks(canvas, p);
        if (p.f().box().isImage()) {
            canvas.image(p.f().box(), p.borderBox().shrink(p.f().border()).shrink(p.f().padding()));
        } else if (p.f().box().element == Element.CONTENTS_LEADER) {
            paintLeader(canvas, p);
        } else {
            paintText(canvas, p);
        }
    }

    /**
     * Las zonas clicables del fragmento (§5.20): su caja de borde entera si la caja tiene
     * {@code Link}, y el rectángulo de cada tramo con enlace de cada línea de texto. Los tramos
     * seguidos de una línea con el mismo destino (una parte en negrita dentro de un enlace) son
     * una sola zona; un enlace que ocupa varias líneas, una por línea. Una caja partida entre
     * páginas tiene una zona por fragmento.
     */
    private static void paintLinks(Canvas canvas, Piece p) {
        Box box = p.f().box();
        if (box.style.link != null && p.f().width() > 0 && p.f().height() > 0) {
            canvas.link(box, p.borderBox(), box.style.link, describe(box));
        }
        if (p.f().lines().isEmpty()) {
            return;
        }
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        for (TextLine l : p.f().lines()) {
            String destination = null;
            double from = 0;
            double to = 0;
            StringBuilder text = new StringBuilder();
            for (TextLine.Run r : l.runs()) {
                String link = r.format().link();
                if (destination != null && !destination.equals(link)) {
                    canvas.link(box, linkArea(content, l, from, to), destination, text.toString());
                    destination = null;
                    text.setLength(0);
                }
                if (link != null) {
                    if (destination == null) {
                        destination = link;
                        from = r.x();
                    }
                    to = r.x() + r.width();
                    text.append(r.text());
                }
            }
            if (destination != null) {
                canvas.link(box, linkArea(content, l, from, to), destination, text.toString());
            }
        }
    }

    /**
     * Lo que dice la caja con enlace, para quien no la ve: el texto alternativo de una imagen o el
     * texto de la caja y de lo que contiene (sin el número de página de las entradas de un
     * índice); {@code null} si no tiene texto.
     */
    private static String describe(Box box) {
        if (box.style.alt != null) {
            return box.style.alt;
        }
        StringBuilder out = new StringBuilder();
        collectText(box, out);
        String text = out.toString().replaceAll("\\s+", " ").strip();
        return text.isEmpty() ? null : text;
    }

    private static void collectText(Box box, StringBuilder out) {
        if (box.element == Element.CONTENTS_PAGE) {
            return;
        }
        if (box.hasText()) {
            out.append(box.text).append(' ');
        }
        box.children.forEach(child -> collectText(child, out));
    }

    /** Zona de un tramo de la línea {@code l}, desde {@code from} hasta {@code to} (relativos a la línea). */
    private static Rect linkArea(Rect content, TextLine l, double from, double to) {
        return new Rect(content.x() + l.x() + from, content.y() + l.y(), to - from, l.height());
    }

    /**
     * La guía de una entrada de índice (§5.19): puntos o una línea sobre la línea base del texto
     * de su entrada, con su color. Como los fondos, no es contenido del PDF etiquetado.
     */
    private static void paintLeader(Canvas canvas, Piece p) {
        Style s = p.f().box().style;
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        double baseline = canvas.vertical(s.fontFace()).baseline(content.y(), s.usedLineHeight(), s.fontSize);
        for (Rect shape : Leaders.shapes(s.contentsLeader, content, baseline, s.fontSize)) {
            canvas.background(shape, s.color);
        }
    }

    /**
     * Las líneas de texto, cada tramo con su fuente (§5.4). Como en CSS, el subrayado se pinta
     * antes que el texto y el tachado después; los dos fuera del contenido marcado, porque no son
     * texto (en el PDF etiquetado, <em>artifacts</em>).
     */
    private static void paintText(Canvas canvas, Piece p) {
        if (p.f().lines().isEmpty()) {
            return;
        }
        Style s = p.f().box().style;
        FontFace font = s.fontFace();
        VerticalMetrics metrics = canvas.vertical(font);
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        paintDecorations(canvas, p, content, metrics, true);
        canvas.beginText(p.f().box());
        for (TextLine l : p.f().lines()) {
            double baseline = metrics.baseline(content.y() + l.y(), l.height(), s.fontSize);
            for (TextLine.Run r : l.runs()) {
                canvas.text(content.x() + l.x() + r.x(), baseline, r.text(), r.format().face(font), s.fontSize,
                        s.color, l.wordSpacing());
            }
        }
        canvas.endText();
        paintDecorations(canvas, p, content, metrics, false);
    }

    /** Rayas de subrayado ({@code underline}) o de tachado de los tramos, con el color del texto. */
    private static void paintDecorations(Canvas canvas, Piece p, Rect content, VerticalMetrics metrics,
                                         boolean underline) {
        Style s = p.f().box().style;
        for (TextLine l : p.f().lines()) {
            double baseline = metrics.baseline(content.y() + l.y(), l.height(), s.fontSize);
            for (TextLine.Run r : l.runs()) {
                if (underline ? !r.format().underline() : !r.format().strike()) {
                    continue;
                }
                DecorationMetrics d = canvas.decoration(r.format().face(s.fontFace()));
                double x = content.x() + l.x() + r.x();
                canvas.background(underline ? d.underline(x, baseline, r.width(), s.fontSize)
                        : d.strikeout(x, baseline, r.width(), s.fontSize), s.color);
            }
        }
    }
}
