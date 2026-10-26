package dev.stxt.boxes.layout;

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
 */
public record BoxModel(Insets margin, Insets border, Insets padding, double width,
                       OptionalDouble height, double minHeight, OptionalDouble maxHeight) {

    /**
     * Resuelve el modelo de caja de una caja en flujo de bloque.
     *
     * @param intrinsic tamaños intrínsecos de la caja, para {@code Width: min-content} y
     *                  similares; solo se calculan si hacen falta
     */
    public static BoxModel resolve(Box box, ConstraintSpace space, Supplier<IntrinsicSizes> intrinsic) {
        Style s = box.style;
        double baseWidth = space.percentBaseWidth();
        OptionalDouble baseHeight = space.percentBaseHeight();
        Insets margin = Insets.resolve(s.margin, baseWidth);
        Insets border = Insets.resolve(s.border, baseWidth);
        Insets padding = Insets.resolve(s.padding, baseWidth);
        double extraWidth = border.horizontal() + padding.horizontal();
        double extraHeight = border.vertical() + padding.vertical();

        double width;
        if (space.fixedWidth()) {
            width = space.availableWidth();
        } else {
            width = constrainWidth(s, specifiedWidth(s, space, margin, extraWidth, intrinsic), baseWidth, extraWidth);
        }

        double minHeight = borderBox(s.minHeight.resolve(baseHeight), s, extraHeight).orElse(0);
        OptionalDouble maxHeight = s.maxHeight == null
                ? OptionalDouble.empty() : borderBox(s.maxHeight.resolve(baseHeight), s, extraHeight);
        OptionalDouble height;
        if (space.fixedHeight()) {
            height = space.availableHeight();
        } else {
            height = borderBox(s.height.resolve(baseHeight), s, extraHeight);
            if (height.isEmpty() && s.aspectRatio != null) {
                // Simplificación: la proporción se aplica a la caja de borde.
                height = OptionalDouble.of(width / s.aspectRatio);
            }
            if (height.isPresent()) {
                height = OptionalDouble.of(Sizing.constrain(height.getAsDouble(), minHeight, maxHeight));
            }
        }
        return new BoxModel(margin, border, padding, width, height, minHeight, maxHeight);
    }

    /**
     * Limita un ancho de caja de borde por {@code Min width} y {@code Max width} (§5.9).
     *
     * @param baseWidth  base de los porcentajes
     * @param extraWidth borde y relleno horizontales, para {@code Box sizing: content}
     */
    public static double constrainWidth(Style s, double width, double baseWidth, double extraWidth) {
        return Sizing.constrain(width,
                borderBox(s.minWidth.resolve(baseWidth), s, extraWidth).orElse(0),
                s.maxWidth == null ? OptionalDouble.empty() : borderBox(s.maxWidth.resolve(baseWidth), s, extraWidth));
    }

    /** Ancho de caja de borde antes de mínimo y máximo. */
    private static double specifiedWidth(Style s, ConstraintSpace space, Insets margin, double extraWidth,
                                         Supplier<IntrinsicSizes> intrinsic) {
        OptionalDouble declared = borderBox(s.width.resolve(space.percentBaseWidth()), s, extraWidth);
        if (declared.isPresent()) {
            return declared.getAsDouble();
        }
        if (s.width instanceof Value.Keyword k && k != Value.Keyword.AUTO) {
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

    private static OptionalDouble borderBox(OptionalDouble declared, Style s, double extra) {
        return declared.isPresent()
                ? OptionalDouble.of(Sizing.toBorderBox(declared.getAsDouble(), s.boxSizing, extra))
                : declared;
    }

    /**
     * Tamaños intrínsecos de la caja de margen a partir de los de su contenido (§5.9). Una caja
     * con {@code Width} fijo tiene ese valor como min-content y max-content. Los porcentajes no
     * se pueden resolver sin containing block y cuentan como 0 (o como {@code auto}).
     */
    public static IntrinsicSizes intrinsic(Box box, IntrinsicSizes content) {
        Style s = box.style;
        Insets border = Insets.resolve(s.border, 0);
        Insets padding = Insets.resolve(s.padding, 0);
        double extra = border.horizontal() + padding.horizontal();
        OptionalDouble fixed = borderBox(s.width.resolve(OptionalDouble.empty()), s, extra);
        IntrinsicSizes r = fixed.isPresent()
                ? new IntrinsicSizes(fixed.getAsDouble(), fixed.getAsDouble())
                : content.plus(extra);
        double min = borderBox(s.minWidth.resolve(OptionalDouble.empty()), s, extra).orElse(0);
        OptionalDouble max = s.maxWidth == null
                ? OptionalDouble.empty() : borderBox(s.maxWidth.resolve(OptionalDouble.empty()), s, extra);
        r = new IntrinsicSizes(Sizing.constrain(r.minContent(), min, max), Sizing.constrain(r.maxContent(), min, max));
        return r.plus(Insets.resolve(s.margin, 0).horizontal());
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

    /** Nada separa el borde superior de la caja de su contenido: ni borde ni relleno. */
    public boolean openAtTop() {
        return border.top() == 0 && padding.top() == 0;
    }

    /** Ídem por abajo, y además el alto depende del contenido. */
    public boolean openAtBottom() {
        return border.bottom() == 0 && padding.bottom() == 0 && height.isEmpty();
    }

    /** Guarda la geometría usada en la caja (la leen el volcado y el pintado). */
    public void applyTo(Box box) {
        box.margin = margin;
        box.border = border;
        box.padding = padding;
        box.width = width;
    }

    /**
     * Espacio para los hijos en flujo: la caja de contenido es su containing block. En modo
     * paginado, lo que queda de página se mide desde el principio de la caja de contenido.
     *
     * @param first es el primer fragmento de la caja (los siguientes no llevan borde ni relleno
     *              superiores, ver {@link Slice})
     */
    public ConstraintSpace childSpace(ConstraintSpace space, boolean first) {
        double w = contentWidth();
        OptionalDouble h = contentHeight();
        OptionalDouble remaining = space.fragmenting()
                ? OptionalDouble.of(space.fragmentainerRemaining().getAsDouble() - Slice.top(this, first))
                : OptionalDouble.empty();
        return new ConstraintSpace(w, h, false, false, w, h, remaining, space.atFragmentainerStart());
    }
}
