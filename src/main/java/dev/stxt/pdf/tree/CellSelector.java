package dev.stxt.pdf.tree;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The position names that a {@code For element} rule of {@code Styles} can give to a table cell
 * (reference/common/styles.md and reference/components/table.md). They are not elements: the element of the box is
 * still {@code Cell}, and these rules apply in addition to that of {@code Cell}.
 */
enum CellSelector {

    HEADER_CELL("Header cell", p -> p.group() == CellPosition.HEADER),
    BODY_CELL("Body cell", p -> p.group() == CellPosition.BODY),
    FOOTER_CELL("Footer cell", p -> p.group() == CellPosition.FOOTER),
    /** Odd rows of the body: the first row of {@code Body} is row 1. */
    ODD_ROW_CELL("Odd row cell", p -> p.group() == CellPosition.BODY && p.row() % 2 == 1),
    EVEN_ROW_CELL("Even row cell", p -> p.group() == CellPosition.BODY && p.row() % 2 == 0);

    private final String label;
    private final Predicate<CellPosition> matches;

    CellSelector(String label, Predicate<CellPosition> matches) {
        this.label = label;
        this.matches = matches;
    }

    /** The selector with that canonical name ({@code header-cell}); empty if it is not one. */
    static Optional<CellSelector> of(String canonicalName) {
        return Arrays.stream(values()).filter(s -> s.canonicalName().equals(canonicalName)).findFirst();
    }

    /** The name as it is written ({@code Header cell}). */
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
