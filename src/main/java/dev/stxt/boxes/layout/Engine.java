package dev.stxt.boxes.layout;

import dev.stxt.InlineNode;
import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.Page;
import dev.stxt.boxes.layout.block.BlockLayout;
import dev.stxt.boxes.layout.flex.FlexLayout;
import dev.stxt.boxes.layout.grid.GridLayout;
import dev.stxt.boxes.layout.image.ImageLayout;
import dev.stxt.boxes.layout.image.Images;
import dev.stxt.boxes.layout.image.NaturalSize;
import dev.stxt.boxes.layout.inline.FixedMetrics;
import dev.stxt.boxes.layout.inline.InlineLayout;
import dev.stxt.boxes.layout.inline.PageNumbers;
import dev.stxt.boxes.layout.inline.TextMeasurer;
import dev.stxt.boxes.layout.multicol.MulticolLayout;
import dev.stxt.boxes.layout.position.AbsoluteLayout;
import dev.stxt.boxes.layout.table.TableLayout;
import dev.stxt.boxes.resources.Resources;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.BoxTreeBuilder;
import dev.stxt.boxes.tree.Element;

import java.util.List;
import java.util.Map;

import static dev.stxt.boxes.model.Tolerance.fits;

/**
 * Maquetador y despachador de algoritmos por {@code Layout}. Todas las llamadas recursivas de layout
 * pasan por aquí para usar la caché. Quien solo quiere convertir un documento usa
 * {@link dev.stxt.boxes.convert.Converter}, que lo crea y lo configura.
 *
 * <p><strong>Hilos: un {@code Engine} no es seguro entre hilos y no se comparte.</strong> Guarda la caché del
 * layout (mapas sin sincronizar), el reparto en líneas memorizado y las páginas supuestas de las referencias
 * ({@link dev.stxt.boxes.layout.inline.InlineLayout}). Dos conversiones a la vez necesitan un motor cada una,
 * que es lo que hace el {@code Converter}; lo que se puede compartir es lo que no cambia:
 * {@link Resources}, {@link dev.stxt.boxes.stxt.DocumentReader} y el {@link LaidOutDocument} ya maquetado.
 * Sí se puede reutilizar en secuencia: {@link #layOutDocument(BoxTreeBuilder.Result)} vacía la caché y el
 * estado al terminar, de modo que un motor guardado no retiene el árbol de cajas del último documento.</p>
 *
 * <p>El protocolo con el {@link Paginator} (cifras reservadas, páginas supuestas, numeración y cajas fuera de
 * flujo) no es público: solo se usa desde este package.</p>
 */
public final class Engine {

    private final LayoutCache cache = new LayoutCache();
    private final TextMeasurer measurer;
    private final Images images;
    private final BlockLayout block;
    private final InlineLayout inline;
    private final FlexLayout flex;
    private final GridLayout grid;
    private final TableLayout table;
    private final MulticolLayout multicol;
    private final AbsoluteLayout absolute;
    private final ImageLayout image;

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

    /** Motor con esas métricas del texto y las imágenes de {@code resources} (§5.18). */
    public Engine(TextMeasurer measurer, Resources resources) {
        this.measurer = measurer;
        this.images = new Images(resources);
        this.block = new BlockLayout(this);
        this.inline = new InlineLayout(this, measurer);
        this.flex = new FlexLayout(this);
        this.grid = new GridLayout(this);
        this.table = new TableLayout(this, grid);
        this.multicol = new MulticolLayout(this);
        this.absolute = new AbsoluteLayout(this);
        this.image = new ImageLayout(this);
    }

    /**
     * Documento STXT → páginas, con el medio y los metadatos. Lo normal es leerlo con
     * {@link dev.stxt.boxes.stxt.DocumentReader}; si no está validado, {@link BoxTreeBuilder} lo
     * valida contra el schema. Antes de maquetar comprueba con el {@link TextMeasurer} que se
     * puede medir todo el texto (las fuentes, en la salida PDF) y que existen todas las imágenes,
     * y después que nada desborda ({@link Overflow}).
     */
    public LaidOutDocument layOutDocument(InlineNode document) {
        return layOutDocument(new BoxTreeBuilder().build(document));
    }

    /**
     * Como {@link #layOutDocument(InlineNode)}, con un documento de estilos aparte
     * ({@link dev.stxt.boxes.stxt.DocumentReader#readStyles}), cuyas reglas valen menos que las del
     * propio documento (§4.10). {@code styles} puede ser {@code null}.
     */
    public LaidOutDocument layOutDocument(InlineNode document, InlineNode styles) {
        return layOutDocument(new BoxTreeBuilder().build(document, styles));
    }

    /**
     * Como {@link #layOutDocument(InlineNode)}, con el árbol ya construido. Sirve para retocar el
     * árbol antes de maquetarlo (los tests ponen así tramos con formato en un texto, §5.4); el
     * árbol no se puede cambiar después.
     */
    public LaidOutDocument layOutDocument(BoxTreeBuilder.Result tree) {
        measurer.validate(tree.root());
        images.validate(tree.root());
        reservePageDigits(PageNumbers.DIGITS);
        try {
            List<Page> pages = new Paginator(this).paginate(tree);
            Overflow.check(pages);
            return new LaidOutDocument(tree.media(), tree.metadata(), tree.root(), pages);
        } finally {
            release();
        }
    }

    /** Suelta lo que el motor memoriza de un documento: la caché, el reparto en líneas y las páginas supuestas. */
    private void release() {
        cache.clear();
        inline.assume(Map.of(), Map.of());
    }

    /** Entradas de la caché ahora mismo, para comprobar que {@link #release} la vacía. */
    int cachedEntries() {
        return cache.entries();
    }

    /**
     * Cifras que reservan {@code {page}} y {@code {pages}} al maquetar ({@link PageNumbers}).
     * Vacía la caché, porque cambian las medidas del texto.
     */
    void reservePageDigits(int digits) {
        inline.reservePageDigits(digits);
        cache.clear();
    }

    /**
     * Lo que valen las referencias a otras cajas en los textos (§5.19): la página de cada caja
     * referida con {@code {page of: id}}, que el {@link Paginator} supone y comprueba, y su número
     * con {@code {number of: id}}. Vacía la caché, porque cambian las medidas del texto.
     */
    void assume(Map<String, String> pages, Map<String, String> numbers) {
        inline.assume(pages, numbers);
        cache.clear();
    }

    /** Las métricas del texto: el marcador de una lista se mide y se alinea con ellas (§5.14). */
    public TextMeasurer measurer() {
        return measurer;
    }

    /** Tamaño natural de una {@code Image}, de su fichero (§5.18). */
    public NaturalSize natural(Box box) {
        return images.natural(box);
    }

    /** min-content y max-content de la caja de margen (§5.9). */
    public IntrinsicSizes intrinsic(Box box) {
        return cache.intrinsic(box, () -> BoxModel.intrinsic(box, contentIntrinsic(box)));
    }

    /**
     * min-content y max-content del contenido de la caja, sin su {@code Width}: el mínimo
     * automático de un ítem flex depende de ellos (§5.7).
     */
    public IntrinsicSizes contentIntrinsic(Box box) {
        return cache.contentIntrinsic(box, () -> algorithm(box).contentIntrinsic(box));
    }

    /**
     * Layout de una caja con su algoritmo. Si la caja es el containing block de cajas
     * {@code absolute} de su interior, las resuelve al terminar (ESPECIFICACION.md §5.5).
     *
     * <p>Si la caja termina en esta página y solo se salen de ella su borde y su relleno
     * inferiores, se repite el layout con ese espacio reservado (§5.10): la caja termina dentro
     * de la página o se corta antes, y el final pasa a la siguiente con el borde y el relleno.</p>
     *
     * <p>Una caja con {@code Rotate} no se parte, como una imagen: se maqueta sin fragmentación y,
     * si no cabe en lo que queda de página, devuelve {@code BEFORE}; al principio de una página se
     * coloca aunque no quepa, y el desbordamiento lo señala {@link Overflow}.</p>
     */
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        if (space.fragmenting() && box.style.rotation != null) {
            // Una caja girada no se parte (§5.10): entera o, si no cabe, en la página siguiente.
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

    private LayoutResult layoutOnce(Box box, ConstraintSpace space, BreakToken from) {
        return cache.layout(box, space, from,
                () -> absolute.resolvePending(box, algorithm(box).layout(box, space, from)));
    }

    /**
     * Sustituye {@code {page}} y {@code {pages}} en las líneas de un fragmento ya maquetado y en
     * las de sus descendientes; el {@link Paginator} lo usa al terminar de paginar.
     */
    Fragment number(Fragment fragment, int page, int pages) {
        return inline.number(fragment, page, pages);
    }

    /** Layout de las cajas fuera de flujo; el {@link Paginator} lo usa para las {@code fixed}. */
    AbsoluteLayout absolute() {
        return absolute;
    }

    private LayoutAlgorithm algorithm(Box box) {
        return switch (box.style.layout) {
            case BLOCK -> block;
            case INLINE -> inline;
            case FLEX -> flex;
            // Una tabla es un grid que repite su cabecera y su pie en cada página (§5.15).
            case GRID -> box.element == Element.TABLE ? table : grid;
            case MULTICOL -> multicol;
            case IMAGE -> image;
        };
    }

    @Override
    public String toString() {
        return "Engine[" + cache.stats() + "]";
    }
}
