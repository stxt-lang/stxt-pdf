package dev.stxt.boxes.tree;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Los nombres de posición que una regla {@code Element} de {@code Styles} puede dar a una celda de
 * tabla (ESPECIFICACION.md §4.10, §5.15). No son elementos: el elemento de la caja sigue siendo
 * {@code Cell}, y estas reglas se aplican además de la de {@code Cell}.
 */
enum CellSelector {

    HEAD_CELL("Head cell", p -> p.group() == CellPosition.HEAD),
    BODY_CELL("Body cell", p -> p.group() == CellPosition.BODY),
    FOOT_CELL("Foot cell", p -> p.group() == CellPosition.FOOT),
    /** Filas impares del cuerpo: la primera fila de {@code Body} es la 1. */
    ODD_ROW_CELL("Odd row cell", p -> p.group() == CellPosition.BODY && p.row() % 2 == 1),
    EVEN_ROW_CELL("Even row cell", p -> p.group() == CellPosition.BODY && p.row() % 2 == 0);

    private final String label;
    private final Predicate<CellPosition> matches;

    CellSelector(String label, Predicate<CellPosition> matches) {
        this.label = label;
        this.matches = matches;
    }

    /** El selector con ese nombre canónico ({@code head-cell}); vacío si no es uno. */
    static Optional<CellSelector> of(String canonicalName) {
        return Arrays.stream(values()).filter(s -> s.canonicalName().equals(canonicalName)).findFirst();
    }

    /** El nombre como se escribe ({@code Head cell}). */
    String label() {
        return label;
    }

    private String canonicalName() {
        return label.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    boolean matches(CellPosition position) {
        return matches.test(position);
    }
}
