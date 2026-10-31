package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Estilo de las celdas de una tabla por su posición y su columna (referencia/comun/estilos.md y
 * referencia/componentes/table.md): {@code Header cell}, {@code Body cell}, {@code Footer cell}, {@code Odd row cell},
 * {@code Even row cell}, {@code For column} y {@code Column align}.
 */
class TableStylesTest {

    private static Box root(String... lines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n" + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(doc)))).sections().get(0).box();
    }

    /** Una tabla de dos columnas con una fila de cabecera, tres de cuerpo y una de pie. */
    private static final String[] TABLE = {
            "\tTable:", "\t\tColumn sizes: * *",
            "\t\tHeader rows:", "\t\t\tRow:", "\t\t\t\tCell: h1", "\t\t\t\tCell: h2",
            "\t\tBody:",
            "\t\t\tRow:", "\t\t\t\tCell: a1", "\t\t\t\tCell: a2",
            "\t\t\tRow:", "\t\t\t\tCell: b1", "\t\t\t\tCell: b2",
            "\t\t\tRow:", "\t\t\t\tCell: c1", "\t\t\t\tCell: c2",
            "\t\tFooter rows:", "\t\t\tRow:", "\t\t\t\tCell: f1", "\t\t\t\tCell: f2"};

    private static String[] doc(String[] styles, String... table) {
        return java.util.stream.Stream.concat(java.util.Arrays.stream(styles), java.util.Arrays.stream(table))
                .toArray(String[]::new);
    }

    private static Box cell(Box table, String text) {
        return table.children().stream().filter(c -> text.equals(c.text().trim())).findFirst().orElseThrow();
    }

    /** El color liso del fondo de una celda. */
    private static String color(Box cell) {
        return ((dev.stxt.pdf.model.Paint.Solid) cell.style().box().background()).color();
    }

    private static List<String> background(Box table) {
        return table.children().stream().map(c -> c.style().box().background() == null ? "-" : color(c))
                .toList();
    }

    @Test
    void headerBodyAndFooterRulesApplyToTheCellsOfEachGroup() {
        Box table = root(doc(new String[]{"\tStyles:",
                "\t\tFor element: Header cell", "\t\t\tTypography:", "\t\t\t\tBold: true",
                "\t\tFor element: Footer cell", "\t\t\tTypography:", "\t\t\t\tBold: true", "\t\t\t\tAlign: right",
                "\t\tFor element: Body cell", "\t\t\tBox:", "\t\t\t\tPadding: 3"}, TABLE)).children().get(0);
        assertEquals(FontWeight.BOLD, cell(table, "h2").style().typography().fontWeight());
        assertEquals(FontWeight.BOLD, cell(table, "f1").style().typography().fontWeight());
        assertEquals(TextAlign.RIGHT, cell(table, "f1").style().typography().textAlign());
        assertEquals(TextAlign.LEFT, cell(table, "h1").style().typography().textAlign());
        assertEquals(FontWeight.NORMAL, cell(table, "a1").style().typography().fontWeight());
        assertEquals(Value.pt(3), ((Value.Points) cell(table, "b2").style().box().padding().top()));
        assertEquals(Value.pt(0), ((Value.Points) cell(table, "h1").style().box().padding().top()));
        // La línea de lo que aporta la regla es la de la regla.
        assertEquals(14, cell(table, "h1").style().line("font-weight").number());
    }

    @Test
    void zebraCountsTheRowsOfTheBodyFromOne() {
        Box table = root(doc(new String[]{"\tStyles:",
                "\t\tFor element: Odd row cell", "\t\t\tBox:", "\t\t\t\tBackground: #111111",
                "\t\tFor element: Even row cell", "\t\t\tBox:", "\t\t\t\tBackground: #222222"}, TABLE)).children().get(0);
        // Ni la cabecera ni el pie entran en la cebra.
        assertEquals(List.of("-", "-", "#111111", "#111111", "#222222", "#222222", "#111111", "#111111", "-", "-"),
                background(table).stream().map(b -> b.equals("-") ? b : b.toLowerCase()).toList());
    }

    @Test
    void aCellWithRowSpanBelongsToItsFirstRow() {
        Box table = root("\tStyles:", "\t\tFor element: Even row cell", "\t\t\tBox:", "\t\t\t\tBackground: #222222",
                "\tTable:", "\t\tColumn sizes: * *", "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tCell: a", "\t\t\t\t\tSpan: 2 1", "\t\t\t\tCell: b",
                "\t\t\tRow:", "\t\t\t\tCell: c",
                "\t\t\tRow:", "\t\t\t\tCell: d", "\t\t\t\tCell: e").children().get(0);
        assertNull(cell(table, "a").style().box().background());
        assertNull(cell(table, "b").style().box().background());
        // c está en la fila 2 (par); d y e, en la 3.
        assertEquals("#222222", color(cell(table, "c")).toLowerCase());
        assertNull(cell(table, "d").style().box().background());
    }

    @Test
    void positionRulesAreMoreSpecificThanTheCellRule() {
        Box table = root(doc(new String[]{"\tStyles:",
                "\t\tFor element: Cell", "\t\t\tBox:", "\t\t\t\tPadding: 1", "\t\t\t\tMargin: 1",
                "\t\tFor element: Header cell", "\t\t\tBox:", "\t\t\t\tPadding: 2",
                "\t\tFor element: Body cell", "\t\t\tBox:", "\t\t\t\tPadding: 3",
                "\t\tFor element: Odd row cell", "\t\t\tBox:", "\t\t\t\tPadding: 4"}, TABLE)).children().get(0);
        assertEquals(Value.pt(2), cell(table, "h1").style().box().padding().top());
        assertEquals(Value.pt(4), cell(table, "a1").style().box().padding().top());
        assertEquals(Value.pt(3), cell(table, "b1").style().box().padding().top());
        assertEquals(Value.pt(1), cell(table, "f1").style().box().padding().top());
        assertEquals(Value.pt(1), cell(table, "f1").style().box().margin().top());
    }

    @Test
    void forColumnWinsOverPositionButNotOverClassesIdOrTheCell() {
        Box table = root("\tStyles:",
                "\t\tFor element: Header cell", "\t\t\tTypography:", "\t\t\t\tAlign: center",
                "\t\tFor column: 2", "\t\t\tTypography:", "\t\t\t\tAlign: right",
                "\t\tFor style: left", "\t\t\tTypography:", "\t\t\t\tAlign: left",
                "\tTable:", "\t\tColumn sizes: * *",
                "\t\tHeader rows:", "\t\t\tRow:", "\t\t\t\tCell: h1", "\t\t\t\tCell: h2",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: a1", "\t\t\t\tCell: a2",
                "\t\t\tRow:", "\t\t\t\tCell: b1", "\t\t\t\tCell: b2", "\t\t\t\t\tStyle: left",
                "\t\t\tRow:", "\t\t\t\tCell: c1", "\t\t\t\tCell: c2", "\t\t\t\t\tTypography:", "\t\t\t\t\t\tAlign: center")
                .children().get(0);
        assertEquals(TextAlign.CENTER, cell(table, "h1").style().typography().textAlign());
        assertEquals(TextAlign.RIGHT, cell(table, "h2").style().typography().textAlign());
        assertEquals(TextAlign.LEFT, cell(table, "a1").style().typography().textAlign());
        assertEquals(TextAlign.RIGHT, cell(table, "a2").style().typography().textAlign());
        assertEquals(TextAlign.LEFT, cell(table, "b2").style().typography().textAlign());
        assertEquals(TextAlign.CENTER, cell(table, "c2").style().typography().textAlign());
    }

    @Test
    void forColumnTakesARangeAndMatchesTheColumnWhereTheCellStarts() {
        Box table = root("\tStyles:",
                "\t\tFor column: 2 3", "\t\t\tBox:", "\t\t\t\tBackground: #333333",
                "\tTable:", "\t\tColumn sizes: * * * *",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: a", "\t\t\t\tCell: b", "\t\t\t\t\tSpan: 1 3",
                "\t\t\tRow:", "\t\t\t\tCell: c", "\t\t\t\tCell: d", "\t\t\t\tCell: e", "\t\t\t\tCell: f").children().get(0);
        assertEquals(List.of("-", "#333333", "-", "#333333", "#333333", "-"),
                background(table).stream().map(b -> b.equals("-") ? b : b.toLowerCase()).toList());
    }

    @Test
    void columnAlignIsTheDefaultOfEachColumnAndEverythingElseOverridesIt() {
        Box table = root("\tStyles:",
                "\t\tFor element: Header cell", "\t\t\tTypography:", "\t\t\t\tAlign: center",
                "\t\tFor column: 3", "\t\t\tTypography:", "\t\t\t\tAlign: left",
                "\tTable:", "\t\tColumn sizes: * * *", "\t\tColumn align: left right right",
                "\t\tHeader rows:", "\t\t\tRow:", "\t\t\t\tCell: h1", "\t\t\t\tCell: h2", "\t\t\t\tCell: h3",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: a1", "\t\t\t\tCell: a2", "\t\t\t\tCell: a3",
                "\t\t\tRow:", "\t\t\t\tCell: b1", "\t\t\t\tCell: b2", "\t\t\t\tCell: b3",
                "\t\t\t\t\tTypography:", "\t\t\t\t\t\tAlign: center").children().get(0);
        assertEquals(TextAlign.LEFT, cell(table, "a1").style().typography().textAlign());
        assertEquals(TextAlign.RIGHT, cell(table, "a2").style().typography().textAlign());
        // For column pisa a Column align; la cabecera, también.
        assertEquals(TextAlign.LEFT, cell(table, "a3").style().typography().textAlign());
        assertEquals(TextAlign.CENTER, cell(table, "h2").style().typography().textAlign());
        assertEquals(TextAlign.CENTER, cell(table, "b3").style().typography().textAlign());
        // La línea de la alineación es la de Column align.
        assertEquals(21, cell(table, "a2").style().line("text-align").number());
    }

    @Test
    void columnAlignIsNotInheritedByTheContentsOfAnotherTable() {
        Box root = root("\tTable:", "\t\tColumn sizes: *", "\t\tColumn align: right", "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tCell: a",
                "\tTable:", "\t\tColumn sizes: *", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: b");
        assertEquals(TextAlign.RIGHT, cell(root.children().get(0), "a").style().typography().textAlign());
        assertEquals(TextAlign.LEFT, cell(root.children().get(1), "b").style().typography().textAlign());
    }

    @Test
    void markdownTablesTakeTheRulesAndTheSeparatorRowAlignmentWins() {
        Box markdown = root("\tStyles:",
                "\t\tFor element: Header cell", "\t\t\tTypography:", "\t\t\t\tBold: true",
                "\t\tFor column: 1", "\t\t\tTypography:", "\t\t\t\tAlign: right",
                "\t\tFor element: Even row cell", "\t\t\tBox:", "\t\t\t\tBackground: #444444",
                "\tMarkdown:", "\t\tTable columns: equal",
                "\t\tContent >>",
                "\t\t\t| A | B |", "\t\t\t|:--|--:|", "\t\t\t| 1 | 2 |", "\t\t\t| 3 | 4 |").children().get(0);
        Box table = markdown.children().get(0);
        assertEquals(FontWeight.BOLD, cell(table, "A").style().typography().fontWeight());
        assertEquals(FontWeight.NORMAL, cell(table, "1").style().typography().fontWeight());
        // La primera columna es left en la fila de separación: gana a For column.
        assertEquals(TextAlign.LEFT, cell(table, "A").style().typography().textAlign());
        assertEquals(TextAlign.LEFT, cell(table, "3").style().typography().textAlign());
        assertEquals(TextAlign.RIGHT, cell(table, "4").style().typography().textAlign());
        assertEquals("#444444", color(cell(table, "4")).toLowerCase());
        assertNull(cell(table, "2").style().box().background());
    }

    /**
     * referencia/componentes/table.md: el {@code Style} de una {@code Row} aplica sus reglas {@code For style} a cada
     * celda de la fila.
     */
    @Test
    void aRowClassGivesItsRulesToEveryCellOfTheRow() {
        Box table = root("\tStyles:",
                "\t\tFor style: total", "\t\t\tTypography:", "\t\t\t\tBold: true",
                "\t\t\tBox:", "\t\t\t\tBackground: #EFE4D2",
                "\tTable:", "\t\tColumn sizes: * * *",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: a", "\t\t\t\tCell: b", "\t\t\t\tCell: c",
                "\t\tFooter rows:", "\t\t\tRow:", "\t\t\t\tStyle: total", "\t\t\t\tCell: t1", "\t\t\t\t\tSpan: 1 2",
                "\t\t\t\tCell: t2").children().get(0);
        for (String total : List.of("t1", "t2")) {
            assertEquals(FontWeight.BOLD, cell(table, total).style().typography().fontWeight(), total);
            assertEquals("#efe4d2", color(cell(table, total)).toLowerCase(), total);
        }
        for (String other : List.of("a", "b", "c")) {
            assertEquals(FontWeight.NORMAL, cell(table, other).style().typography().fontWeight(), other);
            assertNull(cell(table, other).style().box().background(), other);
        }
        // La línea de lo que aporta la clase es la de su regla.
        assertEquals(14, cell(table, "t2").style().line("font-weight").number());
    }

    /** Gana a las reglas de columna y de posición y pierde con las clases de la propia celda. */
    @Test
    void aRowClassIsBetweenTheCellClassesAndTheColumnAndPositionRules() {
        Box table = root("\tStyles:",
                "\t\tFor element: Odd row cell", "\t\t\tBox:", "\t\t\t\tBackground: #111111",
                "\t\tFor column: 2", "\t\t\tBox:", "\t\t\t\tBackground: #333333",
                "\t\tFor style: row", "\t\t\tBox:", "\t\t\t\tBackground: #444444",
                "\t\tFor style: own", "\t\t\tBox:", "\t\t\t\tBackground: #555555",
                "\tTable:", "\t\tColumn sizes: * *",
                "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tStyle: row", "\t\t\t\tCell: a1", "\t\t\t\t\tStyle: own", "\t\t\t\tCell: a2",
                "\t\t\tRow:", "\t\t\t\tCell: b1", "\t\t\t\tCell: b2").children().get(0);
        // a1: su clase; a2: la de su fila, que gana a las impares y a la columna; b1 (fila par): nada; b2: la columna.
        assertEquals(List.of("#555555", "#444444", "-", "#333333"),
                background(table).stream().map(String::toLowerCase).toList());
    }

    /**
     * Como el {@code Style} de una caja: una clase sin regla {@code For style} es un error, con la línea del {@code
     * Style}.
     */
    @Test
    void aRowClassWithoutARuleIsAnError() {
        InvalidModelException e = org.junit.jupiter.api.Assertions.assertThrows(InvalidModelException.class,
                () -> root("\tTable:", "\t\tColumn sizes: *", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tStyle: nope",
                        "\t\t\t\tCell: a"));
        assertEquals(ErrorCode.STYLE_WITHOUT_RULE, e.errorCode());
        assertEquals(17, e.line());
    }

    /** Los cortes de una fila son los de su {@code Row}: una clase de fila no los da, como la clase de una celda. */
    @Test
    void aRowClassCannotGiveBreaksToTheCells() {
        InvalidModelException e = org.junit.jupiter.api.Assertions.assertThrows(InvalidModelException.class,
                () -> root("\tStyles:", "\t\tFor style: keep", "\t\t\tBreak:", "\t\t\t\tKeep together: true",
                        "\tTable:", "\t\tColumn sizes: *", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tStyle: keep",
                        "\t\t\t\tCell: a"));
        assertEquals(ErrorCode.STYLE_NOT_ALLOWED, e.errorCode());
    }
}
