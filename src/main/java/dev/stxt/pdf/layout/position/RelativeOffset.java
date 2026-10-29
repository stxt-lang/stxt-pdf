package dev.stxt.pdf.layout.position;

import dev.stxt.pdf.model.Position;
import dev.stxt.pdf.tree.Style;

import java.util.OptionalDouble;

/**
 * Desplazamiento visual de una caja {@code relative} (referencia/componentes/relative-absolute-fixed.md). Se aplica
 * después de colocarla como si fuera {@code static}: su hueco se conserva y sus hermanos no se
 * mueven. Si se declaran arriba y abajo, gana arriba; si se declaran izquierda y derecha, gana
 * izquierda.
 *
 * @param dx desplazamiento horizontal
 * @param dy desplazamiento vertical
 */
public record RelativeOffset(double dx, double dy) {

    public static final RelativeOffset NONE = new RelativeOffset(0, 0);

    /**
     * @param cbWidth  ancho del containing block, base de izquierda y derecha
     * @param cbHeight alto del containing block, base de arriba y abajo; vacío = indefinido, y
     *                 entonces un porcentaje se comporta como {@code auto}
     */
    public static RelativeOffset of(Style s, double cbWidth, OptionalDouble cbHeight) {
        if (s.position() != Position.RELATIVE) {
            return NONE;
        }
        OptionalDouble left = s.offset().left().resolve(cbWidth);
        OptionalDouble right = s.offset().right().resolve(cbWidth);
        OptionalDouble top = s.offset().top().resolve(cbHeight);
        OptionalDouble bottom = s.offset().bottom().resolve(cbHeight);
        double dx = left.isPresent() ? left.getAsDouble() : -right.orElse(0);
        double dy = top.isPresent() ? top.getAsDouble() : -bottom.orElse(0);
        return new RelativeOffset(dx, dy);
    }
}
