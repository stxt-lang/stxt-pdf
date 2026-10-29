package dev.stxt.boxes.layout.block;

import dev.stxt.boxes.fragment.Fragment;
import dev.stxt.boxes.fragment.TextLine;
import dev.stxt.boxes.layout.inline.TextMeasurer;
import dev.stxt.boxes.model.Insets;
import dev.stxt.boxes.tree.Box;
import dev.stxt.boxes.tree.Style;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Marcador de un {@code List item} (ESPECIFICACION.md §5.14): un fragmento de una línea que
 * cuelga a la izquierda de la caja de borde del ítem, con su línea base en la de la primera línea
 * del ítem.
 *
 * <p>La primera línea es la que se encuentra bajando por el primer hijo en flujo de cada
 * fragmento hasta uno con líneas; el árbol ya garantiza que existe ({@code LIST_ITEM_WITHOUT_LINE}).
 * Solo se busca en los primeros fragmentos: si el ítem se parte, el marcador va en el fragmento
 * con la primera línea, que es el primero del ítem.</p>
 */
final class MarkerLayout {

    private MarkerLayout() {
    }

    /**
     * El fragmento del marcador, relativo a la caja de contenido del ítem; vacío si los
     * fragmentos hijos no contienen la primera línea.
     *
     * @param children fragmentos hijos del primer fragmento del ítem, ya colocados
     * @param border   borde del fragmento del ítem
     * @param padding  relleno del fragmento del ítem
     */
    static Optional<Fragment> place(Box item, List<Fragment> children, Insets border, Insets padding,
                                    TextMeasurer measurer) {
        OptionalDouble baseline = firstBaseline(children, measurer);
        if (baseline.isEmpty()) {
            return Optional.empty();
        }
        Box marker = item.marker;
        Style s = marker.style;
        double width = measurer.width(marker.text, s.fontFace(), s.fontSize());
        double height = s.usedLineHeight();
        // Su borde derecho, con el espacio final, toca la caja de borde del ítem.
        double x = -border.left() - padding.left() - width;
        double y = baseline.getAsDouble() - measurer.vertical(s.fontFace()).baseline(0, height, s.fontSize());
        // La caja del marcador es generada: sin margen, borde ni relleno.
        return Optional.of(new Fragment(marker, x, y, width, height, Insets.ZERO, Insets.ZERO, Insets.ZERO,
                List.of(), List.of(new TextLine(0, 0, width, height, marker.text)), true, true));
    }

    /**
     * Línea base de la primera línea de unos fragmentos hijos, relativa a la caja de contenido de
     * su padre: la del primer hijo en flujo, bajando hasta un fragmento con líneas.
     */
    static OptionalDouble firstBaseline(List<Fragment> children, TextMeasurer measurer) {
        double top = 0;
        Optional<Fragment> next = firstInFlow(children);
        while (next.isPresent() && next.get().first()) {
            Fragment f = next.get();
            double contentTop = top + f.y() + f.border().top() + f.padding().top();
            if (!f.lines().isEmpty()) {
                TextLine l = f.lines().get(0);
                Style s = f.box().style;
                return OptionalDouble.of(measurer.vertical(s.fontFace())
                        .baseline(contentTop + l.y(), l.height(), s.fontSize()));
            }
            top = contentTop;
            next = firstInFlow(f.children());
        }
        return OptionalDouble.empty();
    }

    /** El primer fragmento en flujo, sin contar los marcadores. */
    private static Optional<Fragment> firstInFlow(List<Fragment> fragments) {
        return fragments.stream()
                .filter(f -> !f.box().style.isOutOfFlow() && !f.box().isMarker())
                .findFirst();
    }
}
