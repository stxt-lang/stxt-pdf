package dev.stxt.boxes.layout;

import dev.stxt.boxes.layout.image.NaturalSize;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Style;

import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Modelo de caja usado de una caja en un espacio concreto (ESPECIFICACION.md §5.2 y §5.9):
 * márgenes, borde y relleno en puntos, el ancho de la caja de borde y su alto si es definido.
 * Es la parte común a todos los algoritmos de layout.
 *
 * <p>Los porcentajes se resuelven contra las bases del {@link ConstraintSpace}. Un {@code auto}
 * en los márgenes vale 0 aquí: el centrado lo hace el padre, que es quien coloca la caja.</p>
 *
 * @param margin    márgenes usados ({@code auto} = 0)
 * @param border    grosores del borde
 * @param padding   relleno
 * @param width     ancho de la caja de borde, ya limitado por mínimo y máximo
 * @param height    alto de la caja de borde si es definido (número, porcentaje de un alto
 *                  definido, impuesto por el padre o deducido de {@code Aspect ratio}); vacío
 *                  si depende del contenido
 * @param minHeight alto mínimo de la caja de borde
 * @param maxHeight alto máximo de la caja de borde; vacío = sin máximo
 * @param decoration borde y relleno en los cortes ({@link Slice})
 */
public record BoxModel(Insets margin, Insets border, Insets padding, double width,
                       OptionalDouble height, double minHeight, OptionalDouble maxHeight,
                       Style.Decoration decoration) {

    /**
     * Resuelve el modelo de caja de una caja con los tamaños intrínsecos del motor y, si es una
     * {@code Image}, su tamaño natural.
     */
    public static BoxModel resolve(Box box, ConstraintSpace space, Engine engine) {
        return resolve(box, space, () -> engine.intrinsic(box), box.isImage() ? engine.natural(box) : null);
    }

    /**
     * Resuelve el modelo de caja de una caja que no es una {@code Image}.
     *
     * @param intrinsic tamaños intrínsecos de la caja, para {@code Width: min-content} y
     *                  similares; solo se calculan si hacen falta
     */
    public static BoxModel resolve(Box box, ConstraintSpace space, Supplier<IntrinsicSizes> intrinsic) {
        return resolve(box, space, intrinsic, null);
    }

    /**
     * Resuelve el modelo de caja de una caja.
     *
     * @param intrinsic tamaños intrínsecos de la caja, para {@code Width: min-content} y
     *                  similares; solo se calculan si hacen falta
     * @param natural   tamaño natural de una {@code Image}, que se dimensiona como un elemento
     *                  reemplazado ({@link #replaced}); {@code null} en el resto de cajas
     */
    public static BoxModel resolve(Box box, ConstraintSpace space, Supplier<IntrinsicSizes> intrinsic,
                                   NaturalSize natural) {
        Style s = box.style();
        double baseWidth = space.percentBaseWidth();
        OptionalDouble baseHeight = space.percentBaseHeight();
        Insets margin = Insets.resolve(s.margin(), baseWidth);
        Insets border = Insets.resolve(s.border(), baseWidth);
        Insets padding = Insets.resolve(s.padding(), baseWidth);

        double minHeight = s.minHeight().resolve(baseHeight).orElse(0);
        OptionalDouble maxHeight = s.maxHeight() == null
                ? OptionalDouble.empty() : s.maxHeight().resolve(baseHeight);
        if (natural != null) {
            return replaced(s, space, intrinsic, natural, margin, border, padding, minHeight, maxHeight);
        }

        double width;
        if (space.fixedWidth()) {
            width = space.availableWidth();
        } else {
            width = constrainWidth(s, specifiedWidth(s, space, margin, intrinsic), baseWidth);
        }

        OptionalDouble height;
        if (space.fixedHeight()) {
            height = space.availableHeight();
        } else if (space.contentHeight()) {
            height = OptionalDouble.empty();
        } else {
            height = s.height().resolve(baseHeight);
            if (height.isEmpty() && s.aspectRatio() != null) {
                // Simplificación: la proporción se aplica a la caja de borde.
                height = OptionalDouble.of(width / s.aspectRatio());
            }
            if (height.isPresent()) {
                height = OptionalDouble.of(Sizing.constrain(height.getAsDouble(), minHeight, maxHeight));
            }
        }
        return new BoxModel(margin, border, padding, width, height, minHeight, maxHeight, s.decoration());
    }

    /**
     * Una {@code Image} (§5.18), como un elemento reemplazado de CSS: su caja de contenido tiene
     * la proporción del fichero (o la de {@code Aspect ratio}, que la sustituye). Con
     * {@code Width: auto} mide su ancho natural, también en flujo de bloque, o el que sale de un
     * alto declarado; el alto {@code auto} sale siempre del ancho, así que es definido. Si los
     * dos son {@code auto} y el alto choca con su mínimo o su máximo, el ancho se vuelve a
     * deducir del alto limitado. Con {@code measuringContentHeight}, el alto es también el de la
     * proporción.
     */
    private static BoxModel replaced(Style s, ConstraintSpace space, Supplier<IntrinsicSizes> intrinsic,
                                     NaturalSize natural, Insets margin, Insets border, Insets padding,
                                     double minHeight, OptionalDouble maxHeight) {
        double ratio = s.aspectRatio() != null ? s.aspectRatio() : natural.ratio();
        double extraWidth = border.horizontal() + padding.horizontal();
        double extraHeight = border.vertical() + padding.vertical();
        OptionalDouble declaredHeight;
        if (space.fixedHeight()) {
            declaredHeight = space.availableHeight();
        } else if (space.contentHeight()) {
            declaredHeight = OptionalDouble.empty();
        } else {
            declaredHeight = s.height().resolve(space.percentBaseHeight());
            if (declaredHeight.isPresent()) {
                declaredHeight = OptionalDouble.of(Sizing.constrain(declaredHeight.getAsDouble(), minHeight, maxHeight));
            }
        }

        double baseWidth = space.percentBaseWidth();
        boolean autoWidth = !space.fixedWidth() && s.width().isAuto();
        double width;
        if (space.fixedWidth()) {
            width = space.availableWidth();
        } else if (autoWidth) {
            double content = declaredHeight.isPresent()
                    ? Math.max(0, declaredHeight.getAsDouble() - extraHeight) * ratio : natural.width();
            width = constrainWidth(s, content + extraWidth, baseWidth);
        } else {
            width = constrainWidth(s, specifiedWidth(s, space, margin, intrinsic), baseWidth);
        }

        double height;
        if (declaredHeight.isPresent()) {
            height = declaredHeight.getAsDouble();
        } else {
            double proportional = Math.max(0, width - extraWidth) / ratio + extraHeight;
            height = Sizing.constrain(proportional, minHeight, maxHeight);
            if (autoWidth && height != proportional) {
                width = constrainWidth(s, Math.max(0, height - extraHeight) * ratio + extraWidth, baseWidth);
            }
        }
        return new BoxModel(margin, border, padding, width, OptionalDouble.of(height), minHeight, maxHeight,
                s.decoration());
    }

    /**
     * Limita un ancho de caja de borde por {@code Min width} y {@code Max width} (§5.9).
     *
     * @param baseWidth base de los porcentajes
     */
    public static double constrainWidth(Style s, double width, double baseWidth) {
        return Sizing.constrain(width,
                s.minWidth().resolve(baseWidth).orElse(0),
                s.maxWidth() == null ? OptionalDouble.empty() : s.maxWidth().resolve(baseWidth));
    }

    /** Ancho de caja de borde antes de mínimo y máximo. */
    private static double specifiedWidth(Style s, ConstraintSpace space, Insets margin,
                                         Supplier<IntrinsicSizes> intrinsic) {
        OptionalDouble declared = s.width().resolve(space.percentBaseWidth());
        if (declared.isPresent()) {
            return declared.getAsDouble();
        }
        if (s.width() instanceof Value.Keyword k && k != Value.Keyword.AUTO) {
            // Los intrínsecos son de la caja de margen.
            IntrinsicSizes i = intrinsic.get();
            double marginBox = switch (k) {
                case MIN_CONTENT -> i.minContent();
                case MAX_CONTENT -> i.maxContent();
                default -> i.fitContent(space.availableWidth());
            };
            return Math.max(0, marginBox - margin.horizontal());
        }
        // auto, o un valor que depende de una base indefinida.
        return Sizing.autoWidthInFlow(space.availableWidth(), margin);
    }


    /**
     * Tamaños intrínsecos de la caja de margen a partir de los de su contenido (§5.9). Una caja
     * con {@code Width} fijo tiene ese valor como min-content y max-content. Los porcentajes no
     * se pueden resolver sin containing block y cuentan como 0 (o como {@code auto}).
     */
    public static IntrinsicSizes intrinsic(Box box, IntrinsicSizes content) {
        Style s = box.style();
        Insets border = Insets.resolve(s.border(), 0);
        Insets padding = Insets.resolve(s.padding(), 0);
        double extra = border.horizontal() + padding.horizontal();
        OptionalDouble fixed = s.width().resolve(OptionalDouble.empty());
        IntrinsicSizes r = fixed.isPresent()
                ? new IntrinsicSizes(fixed.getAsDouble(), fixed.getAsDouble())
                : content.plus(extra);
        double min = s.minWidth().resolve(OptionalDouble.empty()).orElse(0);
        OptionalDouble max = s.maxWidth() == null
                ? OptionalDouble.empty() : s.maxWidth().resolve(OptionalDouble.empty());
        r = new IntrinsicSizes(Sizing.constrain(r.minContent(), min, max), Sizing.constrain(r.maxContent(), min, max));
        return r.plus(Insets.resolve(s.margin(), 0).horizontal());
    }

    public double contentWidth() {
        return Math.max(0, width - border.horizontal() - padding.horizontal());
    }

    /** Alto de la caja de contenido si el alto es definido. */
    public OptionalDouble contentHeight() {
        return height.isPresent()
                ? OptionalDouble.of(Math.max(0, height.getAsDouble() - border.vertical() - padding.vertical()))
                : OptionalDouble.empty();
    }

    /** Alto final de la caja de borde: el definido o, con {@code auto}, el del contenido. */
    public double usedHeight(double contentHeight) {
        if (height.isPresent()) {
            return height.getAsDouble();
        }
        return Sizing.constrain(contentHeight + border.vertical() + padding.vertical(), minHeight, maxHeight);
    }

    /** Cada fragmento lleva borde y relleno completos ({@code Decoration: clone}). */
    public boolean clones() {
        return decoration == Style.Decoration.CLONE;
    }

    /** Nada separa el borde superior de la caja de su contenido: ni borde ni relleno. */
    public boolean openAtTop() {
        return border.top() == 0 && padding.top() == 0;
    }

    /** Ídem por abajo, y además el alto depende del contenido. */
    public boolean openAtBottom() {
        return border.bottom() == 0 && padding.bottom() == 0 && height.isEmpty();
    }

    /**
     * Espacio para los hijos en flujo: la caja de contenido es su containing block. Lo que
     * queda de página se mide desde el principio de la caja de contenido (y,
     * con {@code Decoration: clone}, sin el borde y el relleno inferiores).
     *
     * @param first es el primer fragmento de la caja (con {@code slice}, los siguientes no llevan
     *              borde ni relleno superiores, ver {@link Slice})
     */
    public ConstraintSpace childSpace(ConstraintSpace space, boolean first) {
        double w = contentWidth();
        OptionalDouble h = contentHeight();
        OptionalDouble remaining = space.fragmenting()
                ? OptionalDouble.of(Slice.contentRemaining(this, first, space))
                : OptionalDouble.empty();
        return ConstraintSpace.forChildren(w, h, remaining, space.atFragmentainerStart());
    }
}
