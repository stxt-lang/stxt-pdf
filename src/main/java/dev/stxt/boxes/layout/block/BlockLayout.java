package dev.stxt.boxes.layout.block;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.layout.AdjoiningMargins;
import dev.stxt.boxes.layout.BoxModel;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.MarginStrut;
import dev.stxt.boxes.layout.PendingAbsolute;
import dev.stxt.boxes.layout.Sizing;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.tree.Box;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout {@code block}: apila los hijos en flujo en vertical (ESPECIFICACION.md §5.3).
 *
 * <pre>
 * cursor = 0; strut = vacío
 * para cada hijo:
 *     si es absolute/fixed: anotar PendingAbsolute con la posición estática; continuar
 *     r = engine.layout(hijo, caja de contenido como containing block)
 *     strut += margen superior del hijo + los que salen de él por arriba
 *     si aún no hay nada entre el borde superior de la caja y el hijo: el strut sale de la caja
 *     si no: cursor += strut                                           // colapso
 *     colocar el fragmento en (margen izquierdo, cursor)               // márgenes auto = centrar
 *     cursor += alto; strut = margen inferior del hijo + los que salen de él por abajo
 * Height auto = cursor (+ strut si no sale por abajo) + relleno + borde
 * </pre>
 *
 * <p>Los márgenes que salen de la caja se devuelven en {@link AdjoiningMargins}: el padre los
 * colapsa con los márgenes propios de la caja. Un hijo vacío deja pasar sus márgenes.</p>
 */
public final class BlockLayout implements LayoutAlgorithm {

    private final Engine engine;

    public BlockLayout(Engine engine) {
        this.engine = engine;
    }

    @Override
    public IntrinsicSizes intrinsic(Box box) {
        double min = 0;
        double max = 0;
        for (Box child : box.inFlowChildren()) {
            IntrinsicSizes i = engine.intrinsic(child);
            min = Math.max(min, i.minContent());
            max = Math.max(max, i.maxContent());
        }
        return BoxModel.intrinsic(box, new IntrinsicSizes(min, max));
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        // TODO(hito 6): reanudar desde from (BreakToken.Block), elegir corte (§5.10) cuando un
        //   hijo no quepa en fragmentainerRemaining, cortes forzados y truncado de márgenes.
        BoxModel model = BoxModel.resolve(box, space, () -> engine.intrinsic(box));
        model.applyTo(box);
        ConstraintSpace childSpace = model.childSpace(space);
        double contentWidth = model.contentWidth();

        boolean isolated = box.establishesBlockFormattingContext();
        boolean openAtTop = !isolated && model.openAtTop();
        boolean openAtBottom = !isolated && model.openAtBottom();

        List<Fragment> fragments = new ArrayList<>();
        List<PendingAbsolute> pending = new ArrayList<>();
        MarginStrut strut = MarginStrut.EMPTY;
        MarginStrut topOut = MarginStrut.EMPTY;
        // Mientras sea cierto, nada separa el borde superior de la caja del hijo actual.
        boolean atTop = openAtTop;
        double cursor = 0;

        for (Box child : box.children) {
            if (child.style.isOutOfFlow()) {
                // Posición estática: donde empezaría si estuviera en flujo (§5.5). La resuelve el hito 3.
                pending.add(new PendingAbsolute(child, 0, atTop ? 0 : cursor + strut.resolve()));
                continue;
            }
            // TODO(hito 3): desplazar las cajas relative según Offset.
            LayoutResult r = engine.layout(child, childSpace, null);
            Fragment f = r.fragment();
            AdjoiningMargins adjoining = r.adjoining();
            Insets margin = Insets.resolve(child.style.margin, contentWidth);
            MarginStrut childTop = adjoining.top().add(margin.top());

            double y;
            if (adjoining.collapsesThrough()) {
                // Caja vacía: sus dos márgenes colapsan con los de alrededor y el cursor no avanza.
                strut = strut.add(childTop).add(margin.bottom());
                y = atTop ? 0 : cursor + strut.resolve();
            } else {
                strut = strut.add(childTop);
                if (atTop) {
                    topOut = strut;
                    atTop = false;
                } else {
                    cursor += strut.resolve();
                }
                y = cursor;
                cursor += f.height();
                strut = adjoining.bottom().add(margin.bottom());
            }

            double x = Sizing.horizontalMargins(contentWidth, f.width(),
                    child.style.margin.left(), child.style.margin.right())[0];
            child.x = x;
            child.y = y;
            fragments.add(f.moved(x, y));
            Insets childPadding = Insets.resolve(child.style.padding, contentWidth);
            double dx = x + f.border().left() + childPadding.left();
            double dy = y + f.border().top() + childPadding.top();
            r.pending().forEach(p -> pending.add(p.translated(dx, dy)));
        }

        double contentHeight;
        AdjoiningMargins out;
        if (atTop) {
            // Ningún hijo con contenido: los márgenes de los hijos vacíos salen por arriba.
            contentHeight = 0;
            boolean empty = openAtBottom && model.usedHeight(0) == 0;
            out = new AdjoiningMargins(strut, MarginStrut.EMPTY, empty);
        } else if (openAtBottom) {
            contentHeight = cursor;
            out = new AdjoiningMargins(topOut, strut, false);
        } else {
            // El borde, el relleno, un alto fijo o un contexto propio contienen el último margen.
            contentHeight = cursor + strut.resolve();
            out = new AdjoiningMargins(topOut, MarginStrut.EMPTY, false);
        }

        double height = model.usedHeight(contentHeight);
        box.height = height;
        Fragment fragment = Fragment.complete(box, 0, 0, model.width(), height, fragments, List.of());
        return LayoutResult.finished(fragment, pending, out);
    }
}
