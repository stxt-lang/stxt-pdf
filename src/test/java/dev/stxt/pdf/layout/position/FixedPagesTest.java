package dev.stxt.pdf.layout.position;

import dev.stxt.pdf.layout.LayoutFixture;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code Pages} de un {@code Page overlay} (reference/components/frame-and-overlays.md): en qué páginas sale. */
class FixedPagesTest {

    /**
     * Un documento de {@code pages} páginas (una por bloque) y un {@code Page overlay} por cada valor de {@code Pages}.
     */
    private static LayoutFixture document(int pages, String... fixedValues) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < pages; i++) {
            lines.addAll(List.of("\tBlock:", "\t\tBreak:", "\t\t\tAfter: page", "\t\tBox:", "\t\t\tHeight: 60"));
        }
        for (String value : fixedValues) {
            lines.addAll(List.of("\tPage overlay:", "\t\tId: " + value, "\t\tPages: " + value, "\t\tOffset: 0 auto auto 0",
                    "\t\tBox:", "\t\t\tWidth: 10", "\t\t\tHeight: 10"));
        }
        // Con 180 pt de alto el área útil mide 100, así que cada bloque de 60 ocupa su página.
        return LayoutFixture.layOutPaged(180, lines.toArray(String[]::new));
    }

    private static List<Integer> pagesOf(LayoutFixture l, String id) {
        return java.util.stream.IntStream.rangeClosed(1, l.pages()).filter(p -> l.has(p, id)).boxed().toList();
    }

    @Test
    void everyValueSelectsItsPages() {
        LayoutFixture l = document(5, "all", "first", "not-first", "odd", "even", "last");
        assertEquals(5, l.pages());
        assertEquals(List.of(1, 2, 3, 4, 5), pagesOf(l, "all"));
        assertEquals(List.of(1), pagesOf(l, "first"));
        assertEquals(List.of(2, 3, 4, 5), pagesOf(l, "not-first"));
        assertEquals(List.of(1, 3, 5), pagesOf(l, "odd"));
        assertEquals(List.of(2, 4), pagesOf(l, "even"));
        assertEquals(List.of(5), pagesOf(l, "last"));
    }

    @Test
    void withoutPagesAPageOverlayGoesInEveryPage() {
        LayoutFixture l = LayoutFixture.layOutPaged(180,
                "\tBlock:", "\t\tBreak:", "\t\t\tAfter: page", "\t\tBox:", "\t\t\tHeight: 60",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 60",
                "\tPage overlay:", "\t\tId: F", "\t\tBox:", "\t\t\tHeight: 10");
        assertEquals(List.of(1, 2), pagesOf(l, "F"));
    }

    @Test
    void aOnePageDocumentIsBothFirstAndLast() {
        LayoutFixture l = document(1, "first", "not-first", "last", "even", "odd");
        assertEquals(List.of(1), pagesOf(l, "first"));
        assertEquals(List.of(1), pagesOf(l, "last"));
        assertEquals(List.of(1), pagesOf(l, "odd"));
        assertEquals(List.of(), pagesOf(l, "not-first"));
        assertEquals(List.of(), pagesOf(l, "even"));
    }

    @Test
    void aFixedNeverMovesTheFlowWhereItIsShownOrNot() {
        String[] flow = {"\tBlock:", "\t\tBreak:", "\t\t\tAfter: page", "\t\tBox:", "\t\t\tHeight: 60",
                "\tBlock:", "\t\tId: B", "\t\tBox:", "\t\t\tHeight: 60"};
        String[] fixed = {"\tPage overlay:", "\t\tId: F", "\t\tPages: first", "\t\tBox:", "\t\t\tHeight: 50"};
        LayoutFixture without = LayoutFixture.layOutPaged(180, flow);
        String[] both = java.util.stream.Stream.concat(java.util.Arrays.stream(flow), java.util.Arrays.stream(fixed))
                .toArray(String[]::new);
        LayoutFixture with = LayoutFixture.layOutPaged(180, both);
        assertEquals(2, with.pages());
        assertEquals(without.rect(2, "B"), with.rect(2, "B"));
        assertTrue(with.has(1, "F"));
        assertFalse(with.has(2, "F"));
    }

    @Test
    void aNestedFixedFollowsItsParentsPagesAndItsOwn() {
        LayoutFixture l = LayoutFixture.layOutPaged(180,
                "\tBlock:", "\t\tBreak:", "\t\t\tAfter: page", "\t\tBox:", "\t\t\tHeight: 60",
                "\tBlock:", "\t\tBreak:", "\t\t\tAfter: page", "\t\tBox:", "\t\t\tHeight: 60",
                "\tBlock:", "\t\tBox:", "\t\t\tHeight: 60",
                "\tPage overlay:", "\t\tId: outer", "\t\tPages: not-first", "\t\tBox:", "\t\t\tHeight: 30",
                "\t\tPage overlay:", "\t\t\tId: inner", "\t\t\tPages: last", "\t\t\tBox:", "\t\t\t\tHeight: 10");
        assertEquals(List.of(2, 3), pagesOf(l, "outer"));
        assertEquals(List.of(3), pagesOf(l, "inner"));
        assertFalse(l.has(1, "inner"));
        assertTrue(l.has(3, "outer"));
    }
}
