package dev.stxt.boxes.layout.image;

import dev.stxt.boxes.fragment.BreakToken;
import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.layout.AdjoiningMargins;
import dev.stxt.boxes.layout.BoxModel;
import dev.stxt.boxes.layout.ConstraintSpace;
import dev.stxt.boxes.layout.LayoutContext;
import dev.stxt.boxes.layout.IntrinsicSizes;
import dev.stxt.boxes.layout.LayoutAlgorithm;
import dev.stxt.boxes.layout.LayoutResult;
import dev.stxt.boxes.layout.Sizing;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Style;

import java.util.List;
import java.util.OptionalDouble;

import static dev.stxt.boxes.model.Tolerance.fits;

/**
 * Layout de una {@code Image} (ESPECIFICACION.md §5.18): una caja sin hijos con el tamaño y la
 * proporción del fichero, como un elemento reemplazado de CSS. El tamaño lo resuelve
 * {@link BoxModel#resolve(Box, ConstraintSpace, LayoutContext)}, que también usan los padres flex, grid
 * y absolute para medirla; aquí solo se coloca.
 *
 * <p>No se parte entre páginas (§5.10): si no cabe en lo que queda de página, corta antes de
 * ella ({@link BreakToken#BEFORE}) y pasa entera a la siguiente; al principio de una página se
 * coloca aunque no quepa, y es {@code PAGE_OVERFLOW}.</p>
 */
public final class ImageLayout implements LayoutAlgorithm {

    private final LayoutContext context;

    public ImageLayout(LayoutContext context) {
        this.context = context;
    }

    /**
     * El ancho natural de la caja de contenido o, si la caja declara su {@code Height} en
     * puntos, el que sale de él con la proporción (sin porcentajes, que necesitan el containing
     * block).
     */
    @Override
    public IntrinsicSizes contentIntrinsic(Box box) {
        Style s = box.style();
        NaturalSize natural = context.natural(box);
        double ratio = s.aspectRatio() != null ? s.aspectRatio() : natural.ratio();
        OptionalDouble height = s.height().resolve(OptionalDouble.empty());
        double width = natural.width();
        if (height.isPresent()) {
            OptionalDouble max = s.maxHeight() == null ? OptionalDouble.empty() : s.maxHeight().resolve(OptionalDouble.empty());
            double h = Sizing.constrain(height.getAsDouble(), s.minHeight().resolve(OptionalDouble.empty()).orElse(0), max);
            double extra = Insets.resolve(s.border(), 0).vertical() + Insets.resolve(s.padding(), 0).vertical();
            width = Math.max(0, h - extra) * ratio;
        }
        return new IntrinsicSizes(width, width);
    }

    @Override
    public LayoutResult layout(Box box, ConstraintSpace space, BreakToken from) {
        BoxModel model = BoxModel.resolve(box, space, context);
        double height = model.height().getAsDouble();
        if (space.fragmenting() && !space.atFragmentainerStart()
                && !fits(height, space.fragmentainerRemaining().getAsDouble())) {
            return LayoutResult.before(box);
        }
        Fragment fragment = new Fragment(box, 0, 0, model.width(), height, model.margin(), model.border(),
                model.padding(), List.of(), List.of(), true, true);
        return LayoutResult.finished(fragment, List.of(), AdjoiningMargins.NONE);
    }
}
