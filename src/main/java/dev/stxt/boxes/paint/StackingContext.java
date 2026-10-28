package dev.stxt.boxes.paint;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.model.DecorationMetrics;
import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Radii;
import dev.stxt.boxes.model.Rect;
import dev.stxt.boxes.model.Rotation;
import dev.stxt.boxes.model.Sides;
import dev.stxt.boxes.model.VerticalMetrics;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Element;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
 * <p>Una caja con {@code Rotate} (§5.6) es siempre posicionada o una {@code Image}: se pinta
 * girada con todo lo que contiene ({@link #paint}).</p>
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

    /**
     * Pinta el contexto. Si su caja está girada ({@code Rotate}, §5.6), lo pinta todo girado: la
     * caja y lo que contiene, también los contextos hijos. Una caja en flujo solo puede girar si es
     * una {@code Image}, que se gira en cada uno de sus dos pasos.
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

    /** Pinta {@code body} girado como la caja de {@code p}, si tiene {@code Rotate}. */
    private static void turning(Canvas canvas, Piece p, Runnable body) {
        Rotation rotation = p.f().box().style.rotation;
        if (rotation == null) {
            body.run();
            return;
        }
        Rect borderBox = p.borderBox();
        canvas.beginRotation(rotation.degrees(), rotation.pivotX(borderBox), rotation.pivotY(borderBox));
        body.run();
        canvas.endRotation();
    }

    private static void paintBackgroundAndBorder(Canvas canvas, Piece p) {
        Style s = p.f().box().style;
        Rect borderBox = p.borderBox();
        Radii radii = radii(p);
        if (s.background != null) {
            canvas.fill(borderBox.shrink(p.f().border()), radii.inset(p.f().border()), s.background);
        }
        canvas.border(borderBox, p.f().border(), radii, borderColors(s));
        if (s.multicolRule > 0) {
            paintRules(canvas, p);
        }
    }

    /**
     * Las esquinas de la caja de borde del fragmento (§5.6): {@code Radius} resuelto contra su
     * tamaño y, en una caja partida entre páginas con {@code Decoration: slice}, solo las de
     * arriba en el primer fragmento y las de abajo en el último.
     */
    private static Radii radii(Piece p) {
        Style s = p.f().box().style;
        Radii radii = Radii.resolve(s.radius, p.f().width(), p.f().height());
        if (s.decoration == Style.Decoration.CLONE) {
            return radii;
        }
        return radii.forFragment(p.f().first(), p.f().last());
    }

    /** El color de cada lado del borde: el suyo o, si no lo declara, el del texto. */
    private static Sides<String> borderColors(Style s) {
        if (s.borderColor == null) {
            return Sides.all(s.color);
        }
        return new Sides<>(orText(s.borderColor.top(), s), orText(s.borderColor.right(), s),
                orText(s.borderColor.bottom(), s), orText(s.borderColor.left(), s));
    }

    private static String orText(String color, Style s) {
        return color != null ? color : s.color;
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

    /** El contenido de una caja: sus líneas de texto o, en una {@code Image}, la imagen (§5.18), y después su enlace. */
    private static void paintContent(Canvas canvas, Piece p) {
        if (p.f().box().isImage()) {
            Insets inside = new Insets(p.f().border().top() + p.f().padding().top(),
                    p.f().border().right() + p.f().padding().right(),
                    p.f().border().bottom() + p.f().padding().bottom(),
                    p.f().border().left() + p.f().padding().left());
            canvas.image(p.f().box(), p.borderBox().shrink(inside), radii(p).inset(inside));
        } else if (p.f().box().element == Element.CONTENTS_LEADER) {
            paintLeader(canvas, p);
        } else {
            paintText(canvas, p);
        }
        paintLinks(canvas, p);
    }

    /**
     * La zona clicable de la caja entera (§5.20): su caja de borde si la caja tiene {@code Link}. Las de
     * los tramos con enlace de sus líneas de texto las pinta {@link #paintText}, dentro del contenido
     * del enlace. Una caja partida entre páginas tiene una zona por fragmento.
     */
    private static void paintLinks(Canvas canvas, Piece p) {
        Box box = p.f().box();
        if (box.style.link != null && p.f().width() > 0 && p.f().height() > 0) {
            canvas.link(box, p.borderBox(), box.style.link, describe(box));
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
     *
     * <p>El texto se pinta en secuencias de tramos seguidos con el mismo enlace, también de una línea
     * a la siguiente ({@link Canvas#beginText} si no tienen, {@link Canvas#beginLinkText} si lo
     * tienen): cada secuencia es un contenido marcado aparte, y el de un enlace cuelga del
     * {@code Link} del PDF etiquetado (§5.20). Dentro de cada una de enlace, una zona clicable por
     * línea ({@link #paintLinkZone}).</p>
     */
    private static void paintText(Canvas canvas, Piece p) {
        if (p.f().lines().isEmpty()) {
            return;
        }
        Box box = p.f().box();
        Style s = box.style;
        FontFace font = s.fontFace();
        VerticalMetrics metrics = canvas.vertical(font);
        Rect content = p.borderBox().shrink(p.f().border()).shrink(p.f().padding());
        paintDecorations(canvas, p, content, metrics, true);
        boolean open = false;
        String current = null;
        for (TextLine l : p.f().lines()) {
            double baseline = metrics.baseline(content.y() + l.y(), l.height(), s.fontSize);
            List<TextLine.Run> runs = l.runs();
            for (int i = 0; i < runs.size(); i++) {
                TextLine.Run r = runs.get(i);
                String link = r.format().link();
                if (!open || !Objects.equals(link, current)) {
                    if (open) {
                        canvas.endText();
                    }
                    if (link == null) {
                        canvas.beginText(box);
                    } else {
                        canvas.beginLinkText(box);
                    }
                    open = true;
                    current = link;
                }
                if (link != null && (i == 0 || !link.equals(runs.get(i - 1).format().link()))) {
                    paintLinkZone(canvas, box, content, l, i, link);
                }
                canvas.text(content.x() + l.x() + r.x(), baseline, r.text(), r.format().face(font),
                        s.fontSize * r.format().scale(), s.color, l.wordSpacing(), s.letterSpacing);
            }
        }
        if (open) {
            canvas.endText();
        }
        paintDecorations(canvas, p, content, metrics, false);
    }

    /**
     * La zona clicable (§5.20) de los tramos de la línea {@code l} que empiezan en {@code first} y van
     * seguidos con el mismo destino (una parte en negrita dentro de un enlace es una sola zona).
     */
    private static void paintLinkZone(Canvas canvas, Box box, Rect content, TextLine l, int first,
                                      String destination) {
        List<TextLine.Run> runs = l.runs();
        StringBuilder text = new StringBuilder();
        int end = first;
        while (end < runs.size() && destination.equals(runs.get(end).format().link())) {
            text.append(runs.get(end).text());
            end++;
        }
        TextLine.Run last = runs.get(end - 1);
        canvas.link(box, linkArea(content, l, runs.get(first).x(), last.x() + last.width()), destination,
                text.toString());
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
