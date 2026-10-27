package dev.stxt.boxes.layout.multicol;

import dev.stxt.boxes.fragment.BreakAppeal;
import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.layout.AdjoiningMargins;
import dev.stxt.boxes.layout.BoxModel;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.Engine;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.Slice;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.ErrorCode;
import dev.stxt.boxes.tree.InvalidModelException;
import dev.stxt.boxes.tree.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Stream;

import static dev.stxt.boxes.layout.Messages.name;
import static dev.stxt.boxes.layout.Messages.pt;
import static dev.stxt.boxes.model.Tolerance.fits;
import static dev.stxt.boxes.tree.PropertyKey.ASPECT_RATIO;
import static dev.stxt.boxes.tree.PropertyKey.COUNT;
import static dev.stxt.boxes.tree.PropertyKey.HEIGHT;
import static dev.stxt.boxes.tree.PropertyKey.MAX_HEIGHT;

/**
 * Layout de un {@code Multicol} (ESPECIFICACION.md §5.16): su contenido llena la primera columna,
 * sigue en la siguiente y, al acabar las columnas de la página, continúa en la primera columna
 * de la página siguiente.
 *
 * <p>{@code BoxTreeBuilder} mete el contenido en el flujo de columnas, una caja de bloque que es
 * el único hijo del {@code Multicol}. Cada columna es un fragmento de esa caja: se maqueta con
 * {@code engine.layout}, con el alto de la columna como lo que queda de fragmentainer y el token
 * de la columna anterior como punto de partida. Los algoritmos que ya saben partir hacen el
 * resto.</p>
 *
 * <pre>
 * alto de las columnas = lo que queda de página para el contenido
 *                        y, con Height o Max height, lo que queda de él
 * llenar en orden: columna i desde el token de la i−1, hasta acabar el contenido, las columnas
 *                  o un corte forzado de página
 * si el contenido termina aquí y Fill es balance: el alto mínimo en que cabe sin cortes peores
 *                  entre columnas (búsqueda binaria)
 * sin página ni alto que llenar: se equilibra siempre
 * alto del contenido = el de la columna más alta
 * </pre>
 *
 * <p>Cortes. Un corte de columna es un corte más dentro del flujo; uno de página
 * ({@link BreakAppeal#FORCED_PAGE}) deja de llenar columnas y pasa al padre. Si la primera
 * columna no coloca nada, el {@code Multicol} corta antes de él; si no coloca nada otra columna,
 * el resto pasa a la página siguiente. La calidad del corte es la de la última columna.</p>
 *
 * <p>El contenido que no cabe en las columnas es un error ({@link ErrorCode#BOX_OVERFLOW}) si no
 * puede seguir en otra página: cuando el alto propio del {@code Multicol} termina en esta, o
 * cuando, sin página ni alto que llenar, hay más cortes de columna que columnas.</p>
 */
public final class MulticolLayout implements LayoutAlgorithm {

    /**
     * Precisión de la búsqueda del alto equilibrado: los cortes cambian a saltos (una línea, una
     * caja), así que por debajo de ella las columnas ya no cambian.
     */
    private static final double PRECISION = 1e-3;

    private final Engine engine;

    public MulticolLayout(Engine engine) {
        this.engine = engine;
    }

    /**
     * Con {@code Count}, ese número de columnas del min-content o el max-content del contenido,
     * más los huecos; con {@code Column width} el mínimo es una columna, porque el número de
     * columnas se adapta al ancho, y el máximo, columnas de al menos ese ancho.
     */
    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        IntrinsicSizes content = engine.intrinsic(flow(box));
        Style s = box.style;
        int count = s.multicolCount != null ? s.multicolCount : 1;
        int narrowest = s.multicolWidth == null ? count : 1;
        double width = s.multicolWidth != null ? s.multicolWidth : 0;
        return new IntrinsicSizes(narrowest * content.minContent() + (narrowest - 1) * s.multicolGap,
                count * Math.max(content.maxContent(), width) + (count - 1) * s.multicolGap);
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        BoxModel model = BoxModel.resolve(box, space, () -> engine.intrinsic(box));
        return new Run(box, model, space, (BreakToken.Block) from).layOut();
    }

    /** El flujo de columnas: el único hijo del {@code Multicol}. */
    private static Box flow(Box multicol) {
        return multicol.children.get(0);
    }

    /**
     * Las columnas llenas con un alto.
     *
     * @param columns fragmentos del flujo, uno por columna con contenido, ya colocados
     * @param token   por dónde sigue el flujo; {@code null} si ha terminado
     * @param appeal  calidad del corte de la última columna
     * @param worst   calidad del peor corte entre columnas
     * @param before  la primera columna no ha colocado nada
     * @param height  alto de la columna más alta
     */
    private record Fill(List<Fragment> columns, BreakToken token, int appeal, int worst, boolean before,
                        double height) {

        boolean finished() {
            return !before && token == null;
        }

        /**
         * El contenido termina en las columnas sin pasar de {@code limit} y sin cortes entre
         * columnas peores que {@code worst}: equilibrar no parte lo que se puede dejar entero.
         */
        boolean fitsIn(double limit, int worst) {
            return finished() && fits(height, limit) && this.worst >= worst;
        }
    }

    /** Layout de un fragmento del {@code Multicol}. */
    private final class Run {
        final Box box;
        final Box flow;
        final BoxModel model;
        final ConstraintSpace space;
        final boolean first;
        final double consumed;
        final BreakToken resume;
        /** El contenido terminó en una página anterior: solo queda el alto propio de la caja. */
        final boolean contentDone;
        final Columns columns;

        Run(Box box, BoxModel model, ConstraintSpace space, BreakToken.Block from) {
            this.box = box;
            this.flow = flow(box);
            this.model = model;
            this.space = space;
            this.first = from == null;
            this.consumed = BreakToken.consumed(from);
            this.resume = first ? null : from.childToken();
            this.contentDone = !first && from.nextChild() > 0;
            Style s = box.style;
            this.columns = Columns.of(s.multicolCount, s.multicolWidth, s.multicolGap, model.contentWidth());
        }

        LayoutResult layOut() {
            if (contentDone) {
                return result(new Fill(List.of(), null, BreakAppeal.PERFECT_BREAK, BreakAppeal.FORCED_PAGE, false, 0));
            }
            double page = space.fragmenting() ? Slice.contentRemaining(model, first, space) : Double.POSITIVE_INFINITY;
            double own = ownRemaining();
            double limit = Math.min(page, own);
            if (Double.isInfinite(limit)) {
                // Sin página ni alto que llenar: se equilibra, también con Fill: auto.
                double single = engine.layout(flow, columnSpace(OptionalDouble.empty(), true), resume)
                        .fragment().height();
                Fill fill = fill(single, true);
                if (!fill.fitsIn(single, BreakAppeal.FORCED)) {
                    throw tooManyColumnBreaks();
                }
                return result(balance(fill, single, true));
            }
            // Sin fragmentación (un ítem de flex con Height), las columnas son la única página que hay.
            boolean atStart = !space.fragmenting() || space.atFragmentainerStart();
            Fill fill = fill(limit, atStart);
            if (fill.before()) {
                return LayoutResult.before(box);
            }
            if (!fill.finished() && own <= page) {
                // El Multicol termina en esta página y su contenido no.
                throw tooTall(limit);
            }
            if (fill.fitsIn(limit, BreakAppeal.FORCED) && box.style.multicolFill == Style.Fill.BALANCE) {
                fill = balance(fill, limit, atStart);
            }
            return result(fill);
        }

        /**
         * Lo que queda para el contenido del alto propio de la caja: su {@code Height} (o el que le
         * impone el padre) o, si no tiene, su {@code Max height}; infinito si no tiene ninguno.
         */
        double ownRemaining() {
            OptionalDouble cap = model.height().isPresent() ? model.height() : model.maxHeight();
            if (cap.isEmpty()) {
                return Double.POSITIVE_INFINITY;
            }
            double decoration = model.border().vertical() + model.padding().vertical();
            if (model.clones()) {
                // Con clone, lo consumido es el alto del contenido de los fragmentos anteriores.
                return cap.getAsDouble() - decoration - consumed;
            }
            return cap.getAsDouble() - consumed - Slice.top(model, first) - model.border().bottom()
                    - model.padding().bottom();
        }

        /**
         * Llena las columnas en orden con el alto {@code height}, hasta que termina el contenido,
         * se acaban las columnas, una columna no coloca nada o hay un corte forzado de página.
         *
         * @param atStart las columnas empiezan la página: deben colocar algo aunque no quepa
         */
        Fill fill(double height, boolean atStart) {
            List<Fragment> placed = new ArrayList<>();
            BreakToken token = resume;
            int appeal = BreakAppeal.PERFECT_BREAK;
            int worst = BreakAppeal.FORCED_PAGE;
            double tallest = 0;
            for (int i = 0; i < columns.count(); i++) {
                LayoutResult r = engine.layout(flow, columnSpace(OptionalDouble.of(height), atStart), token);
                if (r.breakToken() instanceof BreakToken.Before) {
                    // Lo siguiente no cabe en una columna de este alto: tampoco en las demás.
                    return new Fill(placed, token, appeal, worst, i == 0, tallest);
                }
                placed.add(r.fragment().moved(columns.x(i), 0));
                tallest = Math.max(tallest, r.fragment().height());
                token = r.breakToken();
                appeal = r.breakAppeal();
                if (token == null || appeal == BreakAppeal.FORCED_PAGE) {
                    break;
                }
                worst = Math.min(worst, appeal);
            }
            return new Fill(placed, token, appeal, worst, false, tallest);
        }

        /**
         * El reparto con el alto mínimo en que el contenido cabe en las columnas sin cortes entre
         * columnas que incumplan más reglas que los de {@code known}, el reparto que ya cabe en
         * {@code height} (si no tiene cortes, ninguna). Es una búsqueda binaria entre 0 y
         * {@code height}.
         */
        Fill balance(Fill known, double height, boolean atStart) {
            int worst = Math.min(known.worst(), BreakAppeal.PERFECT_BREAK);
            Fill best = known;
            double low = 0;
            double high = height;
            while (high - low > PRECISION) {
                double mid = (low + high) / 2;
                Fill f = fill(mid, atStart);
                if (f.fitsIn(mid, worst)) {
                    best = f;
                    high = mid;
                } else {
                    low = mid;
                }
            }
            return best;
        }

        /**
         * Espacio de una columna: su ancho, sin alto definido y, con fragmentación, el alto de la
         * columna como lo que queda de fragmentainer.
         */
        ConstraintSpace columnSpace(OptionalDouble height, boolean atStart) {
            return ConstraintSpace.forChildren(columns.width(), OptionalDouble.empty(), height, atStart);
        }

        LayoutResult result(Fill fill) {
            boolean contentFinished = fill.finished();
            Slice slice = Slice.of(model, first, consumed, fill.height(), contentFinished, space);
            Fragment fragment = new Fragment(box, 0, 0, model.width(), slice.height(), model.margin(),
                    slice.border(), slice.padding(), fill.columns(), List.of(), first, slice.finished());
            // Los márgenes del contenido no salen de las columnas (§5.3).
            if (slice.finished()) {
                return LayoutResult.finished(fragment, List.of(), AdjoiningMargins.NONE);
            }
            if (contentFinished) {
                // El contenido ha terminado, pero el alto propio de la caja sigue en la página siguiente.
                return new LayoutResult(fragment, new BreakToken.Block(1, null, slice.consumed()),
                        BreakAppeal.PERFECT_BREAK, List.of(), AdjoiningMargins.NONE);
            }
            return new LayoutResult(fragment, new BreakToken.Block(0, fill.token(), slice.consumed()), fill.appeal(),
                    List.of(), AdjoiningMargins.NONE);
        }

        /** El contenido no cabe en las columnas del alto que le queda a la caja, que termina en esta página. */
        InvalidModelException tooTall(double limit) {
            Style s = box.style;
            String property = Stream.of(HEIGHT, MAX_HEIGHT, ASPECT_RATIO).filter(s::isDeclared).findFirst().orElse(null);
            return new InvalidModelException(property != null ? s.line(property) : box.line, ErrorCode.BOX_OVERFLOW,
                    "El contenido del Multicol " + name(box) + " no cabe en su alto: " + columns.count()
                            + (columns.count() == 1 ? " columna" : " columnas") + " de " + pt(Math.max(0, limit))
                            + " no bastan; aumenta su alto o reduce el contenido");
        }

        /** Sin página ni alto que llenar, los cortes de columna necesitan más columnas de las que hay. */
        InvalidModelException tooManyColumnBreaks() {
            Style s = box.style;
            return new InvalidModelException(s.isDeclared(COUNT) ? s.line(COUNT) : box.line, ErrorCode.BOX_OVERFLOW,
                    "El Multicol " + name(box) + " tiene " + columns.count() + (columns.count() == 1 ? " columna" : " columnas")
                            + " y sus cortes de columna necesitan más: sin página ni alto que llenar, lo que no "
                            + "cabe no puede seguir en otra página; quita cortes de columna o añade columnas");
        }
    }
}
