package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Estilo de las celdas de una tabla por su posición y su columna (ESPECIFICACION.md §4.10, §5.15):
 * {@code Head cell}, {@code Body cell}, {@code Foot cell}, {@code Odd row cell},
 * {@code Even row cell}, {@code For column} y {@code Column align}.
 */
class TableStylesTest {

    private static Box root(String... lines) {
        String doc = "Document (dev.stxt.boxes):\n\tMedia:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tTypography:\n\t\tFont: IBM Plex Sans\n\t\tSize: 10\n" + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(doc)).root();
    }

    /** Una tabla de dos columnas con una fila de cabecera, tres de cuerpo y una de pie. */
    private static final String[] TABLE = {
            "\tTable:", "\t\tColumns: 1fr 1fr",
            "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: h1", "\t\t\t\tCell: h2",
            "\t\tBody:",
            "\t\t\tRow:", "\t\t\t\tCell: a1", "\t\t\t\tCell: a2",
            "\t\t\tRow:", "\t\t\t\tCell: b1", "\t\t\t\tCell: b2",
            "\t\t\tRow:", "\t\t\t\tCell: c1", "\t\t\t\tCell: c2",
            "\t\tFoot:", "\t\t\tRow:", "\t\t\t\tCell: f1", "\t\t\t\tCell: f2"};

    private static String[] doc(String[] styles, String... table) {
        return java.util.stream.Stream.concat(java.util.Arrays.stream(styles), java.util.Arrays.stream(table))
                .toArray(String[]::new);
    }

    private static Box cell(Box table, String text) {
        return table.children.stream().filter(c -> text.equals(c.text.trim())).findFirst().orElseThrow();
    }

    private static List<String> background(Box table) {
        return table.children.stream().map(c -> c.style.background == null ? "-" : c.style.background.toString())
                .toList();
    }

    @Test
    void headBodyAndFootRulesApplyToTheCellsOfEachGroup() {
        Box table = root(doc(new String[]{"\tStyles:",
                "\t\tElement: Head cell", "\t\t\tTypography:", "\t\t\t\tWeight: bold",
                "\t\tElement: Foot cell", "\t\t\tTypography:", "\t\t\t\tWeight: bold", "\t\t\t\tAlign: right",
                "\t\tElement: Body cell", "\t\t\tBox:", "\t\t\t\tPadding: 3"}, TABLE)).children.get(0);
        assertEquals(FontWeight.BOLD, cell(table, "h2").style.fontWeight);
        assertEquals(FontWeight.BOLD, cell(table, "f1").style.fontWeight);
        assertEquals(Style.TextAlign.RIGHT, cell(table, "f1").style.textAlign);
        assertEquals(Style.TextAlign.LEFT, cell(table, "h1").style.textAlign);
        assertEquals(FontWeight.NORMAL, cell(table, "a1").style.fontWeight);
        assertEquals(Value.pt(3), ((Value.Points) cell(table, "b2").style.padding.top()));
        assertEquals(Value.pt(0), ((Value.Points) cell(table, "h1").style.padding.top()));
        // La línea de lo que aporta la regla es la de la regla.
        assertEquals(11, cell(table, "h1").style.line("font-weight"));
    }

    @Test
    void zebraCountsTheRowsOfTheBodyFromOne() {
        Box table = root(doc(new String[]{"\tStyles:",
                "\t\tElement: Odd row cell", "\t\t\tBox:", "\t\t\t\tBackground: #111111",
                "\t\tElement: Even row cell", "\t\t\tBox:", "\t\t\t\tBackground: #222222"}, TABLE)).children.get(0);
        // Ni la cabecera ni el pie entran en la cebra.
        assertEquals(List.of("-", "-", "#111111", "#111111", "#222222", "#222222", "#111111", "#111111", "-", "-"),
                background(table).stream().map(b -> b.equals("-") ? b : b.toLowerCase()).toList());
    }

    @Test
    void aCellWithRowSpanBelongsToItsFirstRow() {
        Box table = root("\tStyles:", "\t\tElement: Even row cell", "\t\t\tBox:", "\t\t\t\tBackground: #222222",
                "\tTable:", "\t\tColumns: 1fr 1fr", "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tCell: a", "\t\t\t\t\tSpan: 2 1", "\t\t\t\tCell: b",
                "\t\t\tRow:", "\t\t\t\tCell: c",
                "\t\t\tRow:", "\t\t\t\tCell: d", "\t\t\t\tCell: e").children.get(0);
        assertNull(cell(table, "a").style.background);
        assertNull(cell(table, "b").style.background);
        // c está en la fila 2 (par); d y e, en la 3.
        assertEquals("#222222", cell(table, "c").style.background.toString().toLowerCase());
        assertNull(cell(table, "d").style.background);
    }

    @Test
    void positionRulesAreMoreSpecificThanTheCellRule() {
        Box table = root(doc(new String[]{"\tStyles:",
                "\t\tElement: Cell", "\t\t\tBox:", "\t\t\t\tPadding: 1", "\t\t\t\tMargin: 1",
                "\t\tElement: Head cell", "\t\t\tBox:", "\t\t\t\tPadding: 2",
                "\t\tElement: Body cell", "\t\t\tBox:", "\t\t\t\tPadding: 3",
                "\t\tElement: Odd row cell", "\t\t\tBox:", "\t\t\t\tPadding: 4"}, TABLE)).children.get(0);
        assertEquals(Value.pt(2), cell(table, "h1").style.padding.top());
        assertEquals(Value.pt(4), cell(table, "a1").style.padding.top());
        assertEquals(Value.pt(3), cell(table, "b1").style.padding.top());
        assertEquals(Value.pt(1), cell(table, "f1").style.padding.top());
        assertEquals(Value.pt(1), cell(table, "f1").style.margin.top());
    }

    @Test
    void forColumnWinsOverPositionButNotOverClassesIdOrTheCell() {
        Box table = root("\tStyles:",
                "\t\tElement: Head cell", "\t\t\tTypography:", "\t\t\t\tAlign: center",
                "\t\tFor column: 2", "\t\t\tTypography:", "\t\t\t\tAlign: right",
                "\t\tFor class: left", "\t\t\tTypography:", "\t\t\t\tAlign: left",
                "\tTable:", "\t\tColumns: 1fr 1fr",
                "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: h1", "\t\t\t\tCell: h2",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: a1", "\t\t\t\tCell: a2",
                "\t\t\tRow:", "\t\t\t\tCell: b1", "\t\t\t\tCell: b2", "\t\t\t\t\tClass: left",
                "\t\t\tRow:", "\t\t\t\tCell: c1", "\t\t\t\tCell: c2", "\t\t\t\t\tTypography:", "\t\t\t\t\t\tAlign: center")
                .children.get(0);
        assertEquals(Style.TextAlign.CENTER, cell(table, "h1").style.textAlign);
        assertEquals(Style.TextAlign.RIGHT, cell(table, "h2").style.textAlign);
        assertEquals(Style.TextAlign.LEFT, cell(table, "a1").style.textAlign);
        assertEquals(Style.TextAlign.RIGHT, cell(table, "a2").style.textAlign);
        assertEquals(Style.TextAlign.LEFT, cell(table, "b2").style.textAlign);
        assertEquals(Style.TextAlign.CENTER, cell(table, "c2").style.textAlign);
    }

    @Test
    void forColumnTakesARangeAndMatchesTheColumnWhereTheCellStarts() {
        Box table = root("\tStyles:",
                "\t\tFor column: 2 3", "\t\t\tBox:", "\t\t\t\tBackground: #333333",
                "\tTable:", "\t\tColumns: 1fr 1fr 1fr 1fr",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: a", "\t\t\t\tCell: b", "\t\t\t\t\tSpan: 1 3",
                "\t\t\tRow:", "\t\t\t\tCell: c", "\t\t\t\tCell: d", "\t\t\t\tCell: e", "\t\t\t\tCell: f").children.get(0);
        assertEquals(List.of("-", "#333333", "-", "#333333", "#333333", "-"),
                background(table).stream().map(b -> b.equals("-") ? b : b.toLowerCase()).toList());
    }

    @Test
    void columnAlignIsTheDefaultOfEachColumnAndEverythingElseOverridesIt() {
        Box table = root("\tStyles:",
                "\t\tElement: Head cell", "\t\t\tTypography:", "\t\t\t\tAlign: center",
                "\t\tFor column: 3", "\t\t\tTypography:", "\t\t\t\tAlign: left",
                "\tTable:", "\t\tColumns: 1fr 1fr 1fr", "\t\tColumn align: left right right",
                "\t\tHead:", "\t\t\tRow:", "\t\t\t\tCell: h1", "\t\t\t\tCell: h2", "\t\t\t\tCell: h3",
                "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: a1", "\t\t\t\tCell: a2", "\t\t\t\tCell: a3",
                "\t\t\tRow:", "\t\t\t\tCell: b1", "\t\t\t\tCell: b2", "\t\t\t\tCell: b3",
                "\t\t\t\t\tTypography:", "\t\t\t\t\t\tAlign: center").children.get(0);
        assertEquals(Style.TextAlign.LEFT, cell(table, "a1").style.textAlign);
        assertEquals(Style.TextAlign.RIGHT, cell(table, "a2").style.textAlign);
        // For column pisa a Column align; la cabecera, también.
        assertEquals(Style.TextAlign.LEFT, cell(table, "a3").style.textAlign);
        assertEquals(Style.TextAlign.CENTER, cell(table, "h2").style.textAlign);
        assertEquals(Style.TextAlign.CENTER, cell(table, "b3").style.textAlign);
        // La línea de la alineación es la de Column align.
        assertEquals(17, cell(table, "a2").style.line("text-align"));
    }

    @Test
    void columnAlignIsNotInheritedByTheContentsOfAnotherTable() {
        Box root = root("\tTable:", "\t\tColumns: 1fr", "\t\tColumn align: right", "\t\tBody:",
                "\t\t\tRow:", "\t\t\t\tCell: a",
                "\tTable:", "\t\tColumns: 1fr", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: b");
        assertEquals(Style.TextAlign.RIGHT, cell(root.children.get(0), "a").style.textAlign);
        assertEquals(Style.TextAlign.LEFT, cell(root.children.get(1), "b").style.textAlign);
    }

    @Test
    void markdownTablesTakeTheRulesAndTheSeparatorRowAlignmentWins() {
        Box markdown = root("\tStyles:",
                "\t\tElement: Head cell", "\t\t\tTypography:", "\t\t\t\tWeight: bold",
                "\t\tFor column: 1", "\t\t\tTypography:", "\t\t\t\tAlign: right",
                "\t\tElement: Even row cell", "\t\t\tBox:", "\t\t\t\tBackground: #444444",
                "\tMarkdown:", "\t\tTable columns: equal",
                "\t\tContent >>",
                "\t\t\t| A | B |", "\t\t\t|:--|--:|", "\t\t\t| 1 | 2 |", "\t\t\t| 3 | 4 |").children.get(0);
        Box table = markdown.children.get(0);
        assertEquals(FontWeight.BOLD, cell(table, "A").style.fontWeight);
        assertEquals(FontWeight.NORMAL, cell(table, "1").style.fontWeight);
        // La primera columna es left en la fila de separación: gana a For column.
        assertEquals(Style.TextAlign.LEFT, cell(table, "A").style.textAlign);
        assertEquals(Style.TextAlign.LEFT, cell(table, "3").style.textAlign);
        assertEquals(Style.TextAlign.RIGHT, cell(table, "4").style.textAlign);
        assertEquals("#444444", cell(table, "4").style.background.toString().toLowerCase());
        assertNull(cell(table, "2").style.background);
    }
}
