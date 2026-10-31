package dev.stxt.pdf.layout;

import dev.stxt.InlineNode;
import dev.stxt.pdf.fragment.BreakToken;
import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.layout.block.BlockLayout;
import dev.stxt.pdf.layout.stack.StackLayout;
import dev.stxt.pdf.layout.grid.GridLayout;
import dev.stxt.pdf.layout.image.ImageLayout;
import dev.stxt.pdf.layout.image.Images;
import dev.stxt.pdf.layout.image.NaturalSize;
import dev.stxt.pdf.layout.inline.FixedMetrics;
import dev.stxt.pdf.layout.inline.InlineLayout;
import dev.stxt.pdf.layout.inline.PageNumbers;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import dev.stxt.pdf.layout.columns.ColumnsLayout;
import dev.stxt.pdf.layout.position.OverlayLayout;
import dev.stxt.pdf.layout.table.TableLayout;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.Insets;
import dev.stxt.pdf.model.Layout;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.model.Rect;
import dev.stxt.pdf.model.Rotation;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.BoxTreeBuilder;
import dev.stxt.pdf.tree.Element;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

import static dev.stxt.pdf.model.Tolerance.fits;

/**
 * Maquetador y despachador de algoritmos por {@code Layout}. Todas las llamadas recursivas de layout
 * pasan por aquí para usar la caché: los algoritmos lo ven como su {@link LayoutContext}, sin depender del
 * motor. Quien solo quiere convertir un documento usa
 * {@link dev.stxt.pdf.convert.Converter}, que lo crea y lo configura.
 *
 * <p><strong>Hilos: un {@code Engine} no es seguro entre hilos y no se comparte.</strong> Guarda la caché del
 * layout (mapas sin sincronizar), el reparto en líneas memorizado y las páginas supuestas de las referencias
 * ({@link dev.stxt.pdf.layout.inline.InlineLayout}). Dos conversiones a la vez necesitan un motor cada una,
 * que es lo que hace el {@code Converter}; lo que se puede compartir es lo que no cambia:
 * {@link Resources}, {@link dev.stxt.pdf.stxt.DocumentReader} y el {@link LaidOutDocument} ya maquetado.
 * Sí se puede reutilizar en secuencia: {@link #layOutDocument(BoxTreeBuilder.Result)} vacía la caché y el
 * estado al terminar, de modo que un motor guardado no retiene el árbol de cajas del último documento.</p>
 *
 * <p>El protocolo con el {@link Paginator} (cifras reservadas, páginas supuestas, numeración y cajas fuera de
 * flujo) no es público: solo se usa desde este package.</p>
 */
public final class Engine implements LayoutContext {

    private final LayoutCache cache = new LayoutCache();
    private final TextMeasurer measurer;
    private final Images images;
    private final Limits limits;
    private final InlineLayout inline;
    private final TableLayout table;
    private final OverlayLayout absolute;
    /** El algoritmo de cada {@code Layout}; una tabla, que es un grid, tiene el suyo ({@link #algorithm}). */
    private final Map<Layout, LayoutAlgorithm> algorithms = new EnumMap<>(Layout.class);

    /**
     * Motor con {@link FixedMetrics}, para que los tests y los casos golden sean deterministas y
     * calculables a mano. No necesita la carpeta de fuentes; las imágenes, si las hay, son las
     * de la carpeta de recursos por defecto. El PDF se maqueta con
     * {@code new Engine(new PdfMetrics(resources), resources)}.
     */
    public Engine() {
        this(new FixedMetrics());
    }

    /** Motor con esas métricas del texto y las imágenes de la carpeta de recursos por defecto. */
    public Engine(TextMeasurer measurer) {
        this(measurer, new Resources());
    }

    /**
     * Motor con esas métricas del texto y las imágenes de {@code resources} (referencia/componentes/image.md), con los
     * {@link Limits#DEFAULT}.
     */
    public Engine(TextMeasurer measurer, Resources resources) {
        this(measurer, resources, Limits.DEFAULT);
    }

    /** Motor con esas métricas, esas imágenes y esos {@link Limits} (referencia/comun/limites.md). */
    public Engine(TextMeasurer measurer, Resources resources, Limits limits) {
        this(measurer, resources, limits, Set.of());
    }

    /**
     * Motor con esas métricas, esas imágenes y esos {@link Limits}, sin las comprobaciones de {@code excluded} que
     * se hacen al maquetar (las de las imágenes de PDF, {@link Check}).
     */
    public Engine(TextMeasurer measurer, Resources resources, Limits limits, Set<Check> excluded) {
        this.measurer = measurer;
        this.limits = limits;
        this.images = new Images(resources, limits, excluded);
        this.inline = new InlineLayout(this, measurer);
        GridLayout grid = new GridLayout(this);
        this.table = new TableLayout(this, grid);
        this.absolute = new OverlayLayout(this);
        algorithms.put(Layout.BLOCK, new BlockLayout(this));
        algorithms.put(Layout.INLINE, inline);
        algorithms.put(Layout.STACK, new StackLayout(this));
        algorithms.put(Layout.GRID, grid);
        algorithms.put(Layout.COLUMNS, new ColumnsLayout(this));
        algorithms.put(Layout.IMAGE, new ImageLayout(this));
    }

    /**
     * Documento STXT → páginas, con el medio y los metadatos. Lo normal es leerlo con
     * {@link dev.stxt.pdf.stxt.DocumentReader}; si no está validado, {@link BoxTreeBuilder} lo
     * valida contra el schema. Antes de maquetar comprueba con el {@link TextMeasurer} que se
     * puede medir todo el texto (las fuentes, en la salida PDF) y que existen todas las imágenes,
     * y después que nada desborda ({@link Overflow}).
     */
    public LaidOutDocument layOutDocument(InlineNode document) {
        return layOutDocument(new BoxTreeBuilder().build(document));
    }

    /**
     * Como {@link #layOutDocument(InlineNode)}, con un documento de estilos aparte
     * ({@link dev.stxt.pdf.stxt.DocumentReader#readStyles}), cuyas reglas valen menos que las del
     * propio documento (referencia/comun/estilos.md). {@code styles} puede ser {@code null}.
     */
    public LaidOutDocument layOutDocument(InlineNode document, InlineNode styles) {
        return layOutDocument(new BoxTreeBuilder().build(document, styles));
    }

    /**
     * Como {@link #layOutDocument(InlineNode)}, con el árbol ya construido. Sirve para retocar el árbol antes de
     * maquetarlo (los tests ponen así tramos con formato en un texto, referencia/comun/formato-del-texto.md); el árbol
     * no se puede cambiar después.
     */
    public LaidOutDocument layOutDocument(BoxTreeBuilder.Result tree) {
        measurer.validate(tree.root());
        images.validate(tree.root());
        release();
        try {
            List<Page> pages = new Paginator(this).paginate(tree);
            Overflow.check(pages);
            return new LaidOutDocument(tree.pageSetup(), tree.metadata(), tree.root(), pages);
        } finally {
            release();
        }
    }

    /**
     * Suelta lo que el motor memoriza de un documento: la caché, el reparto en líneas, las cifras reservadas y las
     * páginas supuestas.
     */
    private void release() {
        cache.clear();
        inline.reset();
    }

    /** Los límites de este motor, que el {@link Paginator} aplica al número de páginas. */
    Limits limits() {
        return limits;
    }

    /** Layouts que ha calculado este motor desde que se creó, para medir lo que ahorra la caché. */
    int cacheMisses() {
        return cache.misses();
    }

    /** Entradas de la caché ahora mismo, para comprobar que {@link #release} la vacía. */
    int cachedEntries() {
        return cache.entries();
    }

    /**
     * Cifras que reservan {@code {page}} y {@code {pages}} al maquetar ({@link PageNumbers}). Cambian las medidas
     * de {@code changed}, los textos con esos marcadores: se olvida lo que la caché guarda de ellos y de sus
     * antepasados ({@link #forget}).
     */
    void reservePageNumbers(String probe, Collection<Box> changed) {
        inline.reservePageNumbers(probe);
        forget(changed);
    }

    /**
     * Lo que valen las referencias a otras cajas en los textos (referencia/comun/referencias.md): la página de cada
     * caja referida con {@code {page of: id}}, que el {@link Paginator} supone y comprueba, y su número con {@code
     * {number of: id}}. Cambian las medidas de {@code changed}, los textos cuyas referencias valen otra cosa que en la
     * pasada anterior: se olvida lo que la caché guarda de ellos y de sus antepasados.
     */
    void assume(Map<String, String> pages, Map<String, String> numbers, Collection<Box> changed) {
        inline.assume(pages, numbers);
        forget(changed);
    }

    /**
     * Olvida lo que la caché y el reparto en líneas guardan de esos textos y de sus antepasados, cuyo layout depende
     * de lo que miden. Lo demás sigue valiendo: el resultado de una caja solo depende de su subárbol, así que
     * el cuerpo de un documento no se vuelve a maquetar porque cambie el número de su pie de página.
     */
    private void forget(Collection<Box> changed) {
        Set<Box> stale = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Box box : changed) {
            for (Box b = box; b != null && stale.add(b); b = b.parent()) {
                // Sube hasta la raíz, o hasta un antepasado ya apuntado.
            }
        }
        if (!stale.isEmpty()) {
            cache.forget(stale);
            inline.forget(stale);
        }
    }

    @Override
    public TextMeasurer measurer() {
        return measurer;
    }

    @Override
    public NaturalSize natural(Box box) {
        return images.natural(box);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Una caja girada en flujo aporta el ancho del rectángulo que la envuelve, maquetada con el ancho que tendría
     * sin contenedor: su {@code Width} o, si no lo tiene, su ancho natural (referencia/comun/capas-y-giro.md). Los
     * porcentajes cuentan como en cualquier caja ({@link BoxModel#intrinsic}).</p>
     */
    @Override
    public IntrinsicSizes intrinsic(Box box) {
        return cache.intrinsic(box, () -> {
            IntrinsicSizes own = BoxModel.intrinsic(box, contentIntrinsic(box));
            if (!box.style().rotatesInFlow()) {
                return own;
            }
            double margins = Insets.resolve(box.style().box().margin(), 0).horizontal();
            ConstraintSpace space = ConstraintSpace.fixed(own.natural() - margins, OptionalDouble.empty(), 0,
                    OptionalDouble.empty(), false);
            double width = turned(box, layoutOnce(box, space, null).fragment()).width() + margins;
            return new IntrinsicSizes(width, width);
        });
    }

    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        return cache.contentIntrinsic(box, () -> algorithm(box).contentIntrinsic(box));
    }

    /**
     * Layout de una caja con su algoritmo. Si la caja es el containing block de cajas
     * {@code absolute} de su interior, las resuelve al terminar (referencia/componentes/frame-and-overlays.md).
     *
     * <p>Si la caja termina en esta página y solo se salen de ella su borde y su relleno
     * inferiores, se repite el layout con ese espacio reservado (referencia/comun/cortes.md): la caja termina dentro
     * de la página o se corta antes, y el final pasa a la siguiente con el borde y el relleno.</p>
     *
     * <p>Una caja con {@code Rotate} no se parte, como una imagen: se maqueta sin fragmentación y,
     * si no cabe en lo que queda de página, devuelve {@code BEFORE}; al principio de una página se
     * coloca aunque no quepa, y el desbordamiento lo señala {@link Overflow}.</p>
     */
    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        if (box.style().rotatesInFlow()) {
            return rotatedInFlow(box, space);
        }
        if (space.fragmenting() && box.style().rotation() != null) {
            // Una caja girada no se parte (referencia/comun/cortes.md): entera o, si no cabe, en la página siguiente.
            LayoutResult whole = layoutOnce(box, space.unfragmented(), from);
            if (!space.atFragmentainerStart()
                    && !fits(whole.fragment().height(), space.fragmentainerRemaining().getAsDouble())) {
                return LayoutResult.before(box);
            }
            return whole;
        }
        LayoutResult r = layoutOnce(box, space, from);
        if (!space.fragmenting() || r.breakToken() != null) {
            return r;
        }
        Fragment f = r.fragment();
        double bottom = f.border().bottom() + f.padding().bottom();
        double remaining = space.fragmentainerRemaining().getAsDouble();
        if (bottom > 0 && !fits(f.height(), remaining) && fits(f.height() - bottom, remaining)) {
            return layoutOnce(box, space.withRemaining(remaining - bottom, space.atFragmentainerStart()), from);
        }
        return r;
    }

    /**
     * Una caja girada en flujo (referencia/comun/capas-y-giro.md): se maqueta sin girar con su ancho, que no depende
     * del contenedor ({@link #turnedWidth}), y ocupa el rectángulo que envuelve su caja de borde girada sobre su
     * centro. No se parte: entera o, si no cabe en lo que queda de página, en la siguiente. Aísla los márgenes: no
     * salen de ella los de sus hijos.
     */
    private LayoutResult rotatedInFlow(Box box, ConstraintSpace space) {
        ConstraintSpace own = ConstraintSpace.fixed(turnedWidth(box, space), OptionalDouble.empty(),
                space.percentBaseWidth(), space.percentBaseHeight(), space.atFragmentainerStart());
        LayoutResult r = layoutOnce(box, own, null);
        Fragment f = turned(box, r.fragment());
        if (space.fragmenting() && !space.atFragmentainerStart()
                && !fits(f.height(), space.fragmentainerRemaining().getAsDouble())) {
            return LayoutResult.before(box);
        }
        // Lo que sube de dentro (los Page overlay) tiene su posición estática en la caja sin girar.
        List<PendingOverlay> pending = r.pending().stream()
                .map(p -> p.translated(f.turned().x(), f.turned().y())).toList();
        return LayoutResult.finished(f, pending, AdjoiningMargins.NONE);
    }

    /**
     * El ancho de la caja de borde de una caja girada en flujo: el que da su {@code Width} o, si es {@code auto}, su
     * ancho natural, sin estirarse ni partir sus líneas por el ancho del contenedor; una {@code Image}, el suyo de
     * siempre.
     */
    private double turnedWidth(Box box, ConstraintSpace space) {
        if (box.isImage() || !box.style().box().width().isAuto()) {
            return BoxModel.resolve(box, ConstraintSpace.free(space.availableWidth(), space.percentBaseWidth(),
                    space.percentBaseHeight(), space.atFragmentainerStart()), this).width();
        }
        Insets border = Insets.resolve(box.style().box().border(), space.percentBaseWidth());
        Insets padding = Insets.resolve(box.style().box().padding(), space.percentBaseWidth());
        return BoxModel.constrainWidth(box.style(),
                contentIntrinsic(box).natural() + border.horizontal() + padding.horizontal(),
                space.percentBaseWidth());
    }

    /** El fragmento sin girar de una caja girada en flujo, con el rectángulo que la envuelve girada sobre su centro. */
    private static Fragment turned(Box box, Fragment f) {
        Rect border = new Rect(0, 0, f.width(), f.height());
        Rotation rotation = box.style().rotation();
        Rect envelope = Rotation.envelope(rotation.transform(border), border);
        return f.turned(new Rect(-envelope.x(), -envelope.y(), f.width(), f.height()), envelope.width(),
                envelope.height());
    }

    private LayoutResult layoutOnce(Box box, ConstraintSpace space, BreakToken from) {
        return cache.layout(box, space, from,
                () -> absolute.resolvePending(box, algorithm(box).layout(box, space, from)));
    }

    /**
     * Sustituye {@code {page}} y {@code {pages}} en las líneas de un fragmento ya maquetado y en
     * las de sus descendientes; el {@link Paginator} lo usa al terminar de paginar.
     */
    Fragment number(Fragment fragment, String page, String pages) {
        return inline.number(fragment, page, pages);
    }

    /** Layout de las cajas fuera de flujo; el {@link Paginator} lo usa para las {@code fixed}. */
    OverlayLayout absolute() {
        return absolute;
    }

    /** El algoritmo de la caja: el de su {@code Layout} o, si es una tabla, el de las tablas. */
    private LayoutAlgorithm algorithm(Box box) {
        // Una tabla es un grid que repite su cabecera y su pie en cada página (referencia/componentes/table.md).
        return box.element() == Element.TABLE ? table : algorithms.get(box.style().layout());
    }

    @Override
    public String toString() {
        return "Engine[" + cache.stats() + "]";
    }
}
