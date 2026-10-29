package dev.stxt.boxes.layout.block;

import dev.stxt.boxes.fragment.BreakAppeal;
import dev.stxt.boxes.fragment.BreakRules;
import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.layout.AdjoiningMargins;
import dev.stxt.boxes.layout.BoxModel;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutContext;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.MarginStrut;
import dev.stxt.boxes.layout.PendingAbsolute;
import dev.stxt.boxes.layout.Sizing;
import dev.stxt.boxes.layout.Slice;
import dev.stxt.boxes.layout.position.RelativeOffset;
import dev.stxt.boxes.model.Break;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.tree.Box;

import java.util.ArrayList;
import java.util.List;

import static dev.stxt.boxes.model.Tolerance.EPSILON;
import static dev.stxt.boxes.model.Tolerance.fits;
import static dev.stxt.boxes.model.Tolerance.same;

/**
 * Layout {@code block}: apila los hijos en flujo en vertical (referencia/componentes/block.md).
 *
 * <pre>
 * cursor = 0; strut = vacío
 * para cada hijo:
 *     si es absolute/fixed: anotar PendingAbsolute con la posición estática; continuar
 *     r = context.layout(hijo, caja de contenido como containing block)
 *     strut += margen superior del hijo + los que salen de él por arriba
 *     si aún no hay nada entre el borde superior de la caja y el hijo: el strut sale de la caja
 *     si no: cursor += strut                                           // colapso
 *     colocar el fragmento en (margen izquierdo, cursor)               // márgenes auto = centrar
 *     si es relative: desplazar el fragmento según Offset
 *     cursor += alto; strut = margen inferior del hijo + los que salen de él por abajo
 * Height auto = cursor (+ strut si no sale por abajo) + relleno + borde
 * </pre>
 *
 * <p>Los márgenes que salen de la caja se devuelven en {@link AdjoiningMargins}: el padre los
 * colapsa con los márgenes propios de la caja. Un hijo vacío deja pasar sus márgenes.</p>
 *
 * <p><strong>Fragmentación</strong> (referencia/comun/cortes.md). Cada hijo recibe lo que queda de página desde su
 * borde superior. Delante de cada hijo, si ya se ha colocado algo en este fragmento, hay un corte
 * posible con su calidad ({@link BreakRules#between}); se guarda el estado para poder volver a él.
 * Cuando un hijo no cabe (no coloca nada, se parte por dentro o desborda sin partirse), se elige
 * el último corte de mayor calidad entre los anteriores y el de dentro del hijo. Los cortes
 * forzados se aplican en cuanto aparecen. En un corte no forzado, los márgenes que lo tocan se
 * truncan; en uno forzado, se conserva el margen superior del hijo que empieza la página.</p>
 *
 * <p>Un {@code List item} lleva además su marcador en el primer fragmento, colgado a su
 * izquierda ({@link MarkerLayout}).</p>
 *
 * <p>Simplificación: solo se vuelve atrás a cortes <em>entre</em> hijos; nunca se repite el
 * layout de un hijo anterior con menos espacio para cortarlo por dentro.</p>
 */
public final class BlockLayout implements LayoutAlgorithm {

    private final LayoutContext context;

    public BlockLayout(LayoutContext context) {
        this.context = context;
    }

    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        double min = 0;
        double max = 0;
        for (Box child : box.inFlowChildren()) {
            IntrinsicSizes i = context.intrinsic(child);
            min = Math.max(min, i.minContent());
            max = Math.max(max, i.maxContent());
        }
        return new IntrinsicSizes(min, max);
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        BoxModel model = BoxModel.resolve(box, space, context);
        return new Run(box, model, space, (BreakToken.Block) from).layOut();
    }

    /**
     * Un corte posible y el estado del fragmento en ese punto, para volver a él.
     *
     * @param index      siguiente hijo a colocar
     * @param childToken token del hijo {@code index} si el corte es dentro de él; {@code null}
     *                   si el corte es antes de él
     * @param cursor     alto del contenido hasta el corte, sin los márgenes que lo tocan
     * @param fragments  fragmentos hijos que se conservan
     * @param pending    cajas absolutas que se conservan
     */
    private record Mark(int index, BreakToken childToken, int appeal, double cursor, int fragments, int pending) {
    }

    /** Layout de un fragmento de una caja. */
    private final class Run {
        final Box box;
        final BoxModel model;
        final ConstraintSpace space;
        final boolean first;
        final int start;
        final BreakToken resume;
        final double consumed;
        final boolean fragmenting;
        final ConstraintSpace childSpace;
        final double contentWidth;
        /** Lo que queda de página desde el principio de la caja de contenido. */
        final double contentRemaining;

        final List<Fragment> fragments = new ArrayList<>();
        final List<PendingAbsolute> pending = new ArrayList<>();
        final List<Mark> marks = new ArrayList<>();
        MarginStrut strut = MarginStrut.EMPTY;
        MarginStrut topOut = MarginStrut.EMPTY;
        /** Mientras sea cierto, nada separa el borde superior de la caja del hijo actual. */
        boolean atTop;
        double cursor;
        /** Ya hay en este fragmento un hijo en flujo con contenido (que no deja pasar sus márgenes). */
        boolean progress;
        /** Último hijo en flujo colocado en este fragmento, para la calidad del corte siguiente. */
        Box previous;

        Run(Box box, BoxModel model, ConstraintSpace space, BreakToken.Block from) {
            this.box = box;
            this.model = model;
            this.space = space;
            this.first = from == null;
            this.start = first ? 0 : from.nextChild();
            this.resume = first ? null : from.childToken();
            this.consumed = BreakToken.consumed(from);
            this.fragmenting = space.fragmenting();
            this.childSpace = model.childSpace(space, first);
            this.contentWidth = model.contentWidth();
            this.contentRemaining = fragmenting
                    ? childSpace.fragmentainerRemaining().getAsDouble() : Double.POSITIVE_INFINITY;
            // En un fragmento que continúa la caja no sale ningún margen por arriba.
            this.atTop = first && !box.establishesBlockFormattingContext() && model.openAtTop();
        }

        LayoutResult layOut() {
            for (int i = start; i < box.children().size(); i++) {
                Box child = box.children().get(i);
                if (child.style().isOutOfFlow()) {
                    // Posición estática: donde empezaría si estuviera en flujo
                    // (referencia/componentes/relative-absolute-fixed.md). La caja se resuelve en su containing block
                    // (AbsoluteLayout).
                    pending.add(new PendingAbsolute(child, 0, atTop ? 0 : cursor + strut.resolve()));
                    continue;
                }
                if (fragmenting && progress) {
                    Break forced = BreakRules.forcedBetween(previous, child);
                    if (BreakRules.isForced(forced)) {
                        return broken(mark(i, BreakRules.forcedAppeal(forced)));
                    }
                    marks.add(mark(i, BreakRules.between(previous, child)));
                }
                LayoutResult early = place(i, child);
                if (early != null) {
                    return early;
                }
            }
            return finished();
        }

        /** Coloca un hijo en flujo; devuelve el resultado de la caja si hay que cortar aquí. */
        LayoutResult place(int i, Box child) {
            BreakToken childToken = i == start ? resume : null;
            Insets margin = Insets.resolve(child.style().box().margin(), contentWidth);
            // El hijo que empieza un fragmento de continuación toca el corte: sus márgenes se
            // truncan, salvo si el corte fue forzado. Un hijo reanudado ya no tiene margen superior.
            boolean noTopMargin = childToken != null || (!first && i == start && !forcedBeforeStart());

            double y = top(MarginStrut.EMPTY.add(noTopMargin ? 0 : margin.top()));
            if (fragmenting && progress && contentRemaining - y <= EPSILON) {
                return broken(BreakRules.best(marks, Mark::appeal));
            }
            LayoutResult r = layOutChild(child, y, childToken);
            if (r.breakToken() instanceof BreakToken.Before) {
                return progress ? broken(BreakRules.best(marks, Mark::appeal)) : LayoutResult.before(box);
            }
            AdjoiningMargins adjoining = r.adjoining();
            MarginStrut childTop = noTopMargin ? MarginStrut.EMPTY : adjoining.top().add(margin.top());
            // Una caja vacía deja colapsar también su margen inferior con los de alrededor.
            double flowY = adjoining.collapsesThrough()
                    ? top(childTop.add(margin.bottom()))
                    : top(childTop);
            if (fragmenting && !same(flowY, y)) {
                // Los márgenes que salen del hijo por arriba lo han bajado: lo que le queda de página cambia.
                r = layOutChild(child, flowY, childToken);
                if (r.breakToken() instanceof BreakToken.Before) {
                    return progress ? broken(BreakRules.best(marks, Mark::appeal)) : LayoutResult.before(box);
                }
            }
            Fragment f = r.fragment();

            if (adjoining.collapsesThrough()) {
                // Caja vacía: sus dos márgenes colapsan con los de alrededor y el cursor no avanza.
                strut = strut.add(childTop).add(margin.bottom());
            } else {
                strut = strut.add(childTop);
                if (atTop) {
                    topOut = strut;
                    atTop = false;
                } else {
                    cursor += strut.resolve();
                }
                cursor += f.height();
                strut = adjoining.bottom().add(margin.bottom());
            }

            double x = Sizing.horizontalMargins(contentWidth, f.width(),
                    child.style().box().margin().left(), child.style().box().margin().right())[0];
            // Una caja relative se desplaza después de colocarla; el cursor no se entera
            // (referencia/componentes/relative-absolute-fixed.md).
            RelativeOffset offset = RelativeOffset.of(child.style(), contentWidth, childSpace.percentBaseHeight());
            double childX = x + offset.dx();
            double childY = flowY + offset.dy();
            fragments.add(f.moved(childX, childY, offset.dx(), offset.dy()));
            double dx = childX + f.border().left() + f.padding().left();
            double dy = childY + f.border().top() + f.padding().top();
            r.pending().forEach(p -> pending.add(p.translated(dx, dy)));

            boolean hadProgress = progress;
            previous = child;
            progress |= !adjoining.collapsesThrough();
            if (!fragmenting) {
                return null;
            }
            if (r.breakToken() != null) {
                // Corte dentro del hijo: compite con los cortes anteriores entre hijos.
                marks.add(new Mark(i, r.breakToken(), BreakRules.inside(child, r.breakAppeal()),
                        cursor, fragments.size(), pending.size()));
                return broken(BreakRules.best(marks, Mark::appeal));
            }
            if (hadProgress && !fits(flowY + f.height(), contentRemaining)) {
                // El hijo no se parte y no cabe: se corta antes de él (o antes, si es mejor).
                return broken(BreakRules.best(marks, Mark::appeal));
            }
            return null;
        }

        /**
         * Posición del borde superior de un hijo con estos márgenes superiores, colapsados con el
         * strut actual. Si nada lo separa del borde superior de la caja, sus márgenes salen y
         * empieza en 0.
         */
        double top(MarginStrut childTop) {
            return atTop ? 0 : cursor + strut.add(childTop).resolve();
        }

        LayoutResult layOutChild(Box child, double y, BreakToken childToken) {
            ConstraintSpace s = fragmenting
                    ? childSpace.withRemaining(contentRemaining - y, space.atFragmentainerStart() && !progress)
                    : childSpace;
            return context.layout(child, s, childToken);
        }

        /** El corte que abre este fragmento de continuación fue forzado. */
        boolean forcedBeforeStart() {
            Box next = box.children().get(start);
            for (int j = start - 1; j >= 0; j--) {
                Box before = box.children().get(j);
                if (!before.style().isOutOfFlow()) {
                    return BreakRules.isForced(BreakRules.forcedBetween(before, next));
                }
            }
            return BreakRules.isForced(BreakRules.forcedBefore(next));
        }

        Mark mark(int index, int appeal) {
            return new Mark(index, null, appeal, cursor, fragments.size(), pending.size());
        }

        /** Corta la caja en {@code m}: lo posterior pasa al fragmento siguiente. */
        LayoutResult broken(Mark m) {
            List<Fragment> kept = fragments.subList(0, m.fragments());
            List<PendingAbsolute> keptPending = pending.subList(0, m.pending());
            // Los márgenes que tocan el corte se truncan: el contenido acaba en el cursor.
            Slice slice = Slice.of(model, first, consumed, m.cursor(), false, space);
            Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(),
                    slice.border(), slice.padding(), withMarker(kept, slice), List.of(), first, false);
            BreakToken token = new BreakToken.Block(m.index(), m.childToken(), slice.consumed());
            return new LayoutResult(fragment, token, m.appeal(), keptPending,
                    new AdjoiningMargins(topOut, MarginStrut.EMPTY, false));
        }

        /**
         * Los fragmentos hijos y, en el primer fragmento de un {@code List item}, su marcador
         * delante (referencia/componentes/list.md): va con la primera línea, que está en este fragmento si hay alguno.
         */
        List<Fragment> withMarker(List<Fragment> children, Slice slice) {
            if (box.marker() == null || !first) {
                return children;
            }
            return MarkerLayout.place(box, children, slice.border(), slice.padding(), context.measurer())
                    .map(marker -> {
                        List<Fragment> all = new ArrayList<>(children);
                        all.add(0, marker);
                        return all;
                    })
                    .orElse(children);
        }

        /** Todos los hijos colocados. */
        LayoutResult finished() {
            boolean openAtBottom = !box.establishesBlockFormattingContext() && model.openAtBottom();
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

            Slice slice = Slice.of(model, first, consumed, contentHeight, true, space);
            Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(),
                    slice.border(), slice.padding(), withMarker(fragments, slice), List.of(), first, slice.finished());
            if (slice.finished()) {
                return LayoutResult.finished(fragment, pending, out);
            }
            // El contenido ha terminado, pero el alto propio de la caja sigue en la página siguiente.
            BreakToken token = new BreakToken.Block(box.children().size(), null, slice.consumed());
            return new LayoutResult(fragment, token, BreakAppeal.PERFECT_BREAK, pending,
                    new AdjoiningMargins(out.top(), MarginStrut.EMPTY, false));
        }
    }
}
