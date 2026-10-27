package dev.stxt.boxes.paint;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.VerticalMetrics;
import dev.stxt.boxes.tree.Box;
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
 *   <li>Fondo y borde de la caja que crea el contexto.</li>
 *   <li>Contextos hijos con {@code Layer} negativa, de menor a mayor.</li>
 *   <li>Cajas en flujo en orden del árbol: primero fondos y bordes, después el texto.</li>
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
        paintText(canvas, root);
        inFlow.forEach(p -> paintText(canvas, p));
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
    }

    private static void paintText(Canvas canvas, Piece p) {
        if (p.f().lines().isEmpty()) {
            return;
        }
        Style s = p.f().box().style;
        FontFace font = s.fontFace();
        VerticalMetrics metrics = canvas.vertical(font);
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        canvas.beginText(p.f().box());
        for (TextLine l : p.f().lines()) {
            double baseline = metrics.baseline(content.y() + l.y(), l.height(), s.fontSize);
            canvas.text(content.x() + l.x(), baseline, l.text(), font, s.fontSize, s.color, l.wordSpacing());
        }
        canvas.endText();
    }
}
