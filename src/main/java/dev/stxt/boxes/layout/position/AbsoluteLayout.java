package dev.stxt.boxes.layout.position;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.layout.AdjoiningMargins;
import dev.stxt.boxes.layout.BoxModel;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.PendingAbsolute;
import dev.stxt.boxes.layout.Sizing;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Layout de las cajas {@code absolute} y {@code fixed} (ESPECIFICACION.md §5.5). Se resuelven
 * cuando su containing block ya conoce su tamaño, después del layout en flujo:
 *
 * <ul>
 *   <li>Las {@code absolute} suben como {@link PendingAbsolute} hasta su containing block, y
 *       {@link Engine#layout} las resuelve con {@link #resolvePending} al terminar el layout de
 *       esa caja. Sus fragmentos se añaden al final de los hijos del fragmento del containing
 *       block, no de su padre en el árbol de cajas.</li>
 *   <li>Las {@code fixed} suben hasta la raíz y las resuelve el
 *       {@link dev.stxt.boxes.fragment.Paginator} en cada página, con {@link #layOut}.</li>
 * </ul>
 *
 * <p>En cada eje se resuelve la ecuación de {@link AbsoluteEquation}. El ancho automático es el
 * ajuste al contenido de la caja de borde, limitado después por {@code Min width} y
 * {@code Max width}; si el límite lo cambia, se vuelve a resolver con ese ancho. El alto
 * automático es el del contenido, salvo que arriba y abajo estén declarados: entonces la caja se
 * estira entre ellos. Una caja fuera de flujo no se fragmenta.</p>
 */
public final class AbsoluteLayout {

    private final Engine engine;

    public AbsoluteLayout(Engine engine) {
        this.engine = engine;
    }

    /**
     * Resuelve las cajas {@code absolute} pendientes cuyo containing block es {@code box} y añade
     * sus fragmentos al de {@code box}. Las demás siguen subiendo.
     *
     * @param r resultado del layout en flujo de {@code box}
     */
    public LayoutResult resolvePending(Box box, LayoutResult r) {
        if (r.pending().stream().noneMatch(p -> resolvesHere(p, box))) {
            return r;
        }
        Fragment f = r.fragment();
        // El containing block es la caja de relleno.
        double cbWidth = f.width() - f.border().horizontal();
        double cbHeight = f.height() - f.border().vertical();
        List<Fragment> placed = new ArrayList<>();
        List<PendingAbsolute> rest = new ArrayList<>();
        for (PendingAbsolute p : r.pending()) {
            if (resolvesHere(p, box)) {
                LayoutResult a = layOut(p, cbWidth, cbHeight, f.padding());
                placed.add(a.fragment());
                rest.addAll(a.pending());
            } else {
                rest.add(p);
            }
        }
        return new LayoutResult(f.withMoreChildren(placed), r.breakToken(), r.breakAppeal(), rest, r.adjoining());
    }

    private static boolean resolvesHere(PendingAbsolute p, Box box) {
        return p.box().style.position == Style.Position.ABSOLUTE && p.box().outOfFlowContainingBlock() == box;
    }

    /**
     * Maqueta una caja fuera de flujo en su containing block.
     *
     * @param p         la caja, con la posición estática relativa a la caja de contenido del
     *                  containing block
     * @param cbWidth   ancho de la caja de relleno del containing block
     * @param cbHeight  alto de la caja de relleno del containing block
     * @param cbPadding relleno del containing block
     * @return el fragmento colocado respecto a la caja de contenido del containing block y las
     *         cajas {@code fixed} de su interior, con la posición estática relativa a esa misma caja
     */
    public LayoutResult layOut(PendingAbsolute p, double cbWidth, double cbHeight, Insets cbPadding) {
        Box box = p.box();
        Style s = box.style;
        OptionalDouble cbHeightBase = OptionalDouble.of(cbHeight);
        Supplier<IntrinsicSizes> intrinsic = () -> engine.intrinsic(box);
        // La ecuación trabaja en la caja de relleno del containing block.
        double staticX = p.staticX() + cbPadding.left();
        double staticY = p.staticY() + cbPadding.top();

        // Márgenes, borde, relleno y ancho declarado; los porcentajes son del containing block.
        ConstraintSpace probe = ConstraintSpace.free(cbWidth, cbWidth, cbHeightBase, false);
        BoxModel model = BoxModel.resolve(box, probe, intrinsic);
        Insets margin = model.margin();

        OptionalDouble left = s.offset.left().resolve(cbWidth);
        OptionalDouble right = s.offset.right().resolve(cbWidth);
        OptionalDouble width = s.width.isAuto() ? OptionalDouble.empty() : OptionalDouble.of(model.width());
        OptionalDouble marginLeft = term(s.margin.left(), margin.left());
        OptionalDouble marginRight = term(s.margin.right(), margin.right());
        // Tamaños intrínsecos de la caja de borde: los de la caja de margen sin sus márgenes.
        IntrinsicSizes content = width.isPresent()
                ? IntrinsicSizes.ZERO : intrinsic.get().plus(-Insets.resolve(s.margin, 0).horizontal());
        AbsoluteEquation.Solution h = AbsoluteEquation.horizontal(left, width, right, marginLeft, marginRight,
                cbWidth, staticX, content);
        if (width.isEmpty()) {
            double constrained = BoxModel.constrainWidth(s, h.size(), cbWidth);
            if (constrained != h.size()) {
                h = AbsoluteEquation.horizontal(left, OptionalDouble.of(constrained), right, marginLeft, marginRight,
                        cbWidth, staticX, content);
            }
        }

        OptionalDouble top = s.offset.top().resolve(cbHeightBase);
        OptionalDouble bottom = s.offset.bottom().resolve(cbHeightBase);
        OptionalDouble marginTop = term(s.margin.top(), margin.top());
        OptionalDouble marginBottom = term(s.margin.bottom(), margin.bottom());
        ConstraintSpace space = ConstraintSpace.fixed(h.size(), OptionalDouble.empty(), cbWidth, cbHeightBase, false);
        BoxModel sized = BoxModel.resolve(box, space, intrinsic);
        OptionalDouble height = sized.height();
        if (height.isEmpty() && top.isPresent() && bottom.isPresent()) {
            // Solo el alto es auto: la caja se estira entre arriba y abajo.
            double stretched = Math.max(0, cbHeight - top.getAsDouble() - bottom.getAsDouble()
                    - marginTop.orElse(0) - marginBottom.orElse(0));
            height = OptionalDouble.of(Sizing.constrain(stretched, sized.minHeight(), sized.maxHeight()));
            space = ConstraintSpace.fixed(h.size(), height, cbWidth, cbHeightBase, false);
        }
        LayoutResult r = engine.layout(box, space, null);
        Fragment f = r.fragment();
        AbsoluteEquation.Solution v = AbsoluteEquation.vertical(top, height, bottom, marginTop, marginBottom,
                cbHeight, staticY, f.height());

        double x = h.start() + h.marginStart() - cbPadding.left();
        double y = v.start() + v.marginStart() - cbPadding.top();
        // Las cajas fixed del interior pasan de la caja de contenido de esta a la del containing block.
        double dx = x + f.border().left() + sized.padding().left();
        double dy = y + f.border().top() + sized.padding().top();
        List<PendingAbsolute> fixed = r.pending().stream().map(q -> q.translated(dx, dy)).toList();
        return LayoutResult.finished(f.moved(x, y), fixed, AdjoiningMargins.NONE);
    }

    /** Un margen {@code auto} es un término libre de la ecuación (vacío). */
    private static OptionalDouble term(Value specified, double used) {
        return specified.isAuto() ? OptionalDouble.empty() : OptionalDouble.of(used);
    }
}
