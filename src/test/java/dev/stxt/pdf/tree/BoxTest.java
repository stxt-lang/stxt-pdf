package dev.stxt.pdf.tree;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The box tree is written only in {@code tree} and does not change after it is built. */
class BoxTest {

    /** A block with a numbered list and an out-of-flow box. */
    private static final String[] BLOCK = {"\tBlock:", "\t\tList:", "\t\t\tMarker: decimal", "\t\t\tList item:",
            "\t\t\t\tParagraph: Uno", "\t\tOverlay:", "\t\t\tText: fuera"};

    private static BoxTreeBuilder.Result build(String... lines) {
        String doc = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n\t\tWidth: 595\n\t\tHeight: 842\n"
                + "\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n"
                + String.join("\n", lines);
        return new BoxTreeBuilder().build(new DocumentReader().read(Docs.sectioned(Docs.mergeStyles(doc))));
    }

    @Test
    void theBuiltTreeIsClosed() {
        BoxTreeBuilder.Result tree = build(BLOCK);
        Box section = tree.sections().get(0).box();
        Box list = section.children().get(0).children().get(0);
        Box item = list.children().get(0);
        Box text = item.children().get(0);
        assertThrows(UnsupportedOperationException.class, () -> list.children().add(item));
        assertThrows(UnsupportedOperationException.class, () -> tree.root().children().clear());
        assertThrows(IllegalStateException.class, () -> list.addChild(item));
        assertThrows(IllegalStateException.class, () -> text.setText("otro", 1, false));
        assertThrows(IllegalStateException.class, () -> text.prependText("1 "));
        // The marker is not a child and it is closed too.
        assertThrows(IllegalStateException.class, () -> item.marker().setText("2. ", 1, false));
        assertThrows(IllegalStateException.class, () -> tree.root().addSection(section));
    }

    @Test
    void describeNamesTheBoxForMessages() {
        Box section = build("\tBlock:", "\t\tId: main", "\tBlock:").sections().get(0).box();
        Box named = section.children().get(0);
        Box unnamed = section.children().get(1);
        assertEquals("main (line " + named.line() + ")", named.describe());
        assertEquals("at line " + unnamed.line(), unnamed.describe());
        assertEquals("(page area)", new Box(null, Element.BLOCK, new Style(), true, 0).describe());
    }

    @Test
    void theInFlowChildrenAreComputedOnceWhenClosing() {
        Box block = build(BLOCK).sections().get(0).box().children().get(0);
        assertEquals(2, block.children().size());
        assertEquals(List.of(block.children().get(0)), block.inFlowChildren());
        assertSame(block.inFlowChildren(), block.inFlowChildren());
    }

    @Test
    void anOpenBoxCanStillBeWritten() {
        StyleEditor style = new StyleEditor();
        Box parent = style.box("P", Element.BLOCK);
        Box child = style.box("C", Element.BLOCK);
        parent.addChild(child);
        assertSame(parent, child.parent());
        assertEquals(List.of(child), parent.inFlowChildren());
        assertThrows(UnsupportedOperationException.class, () -> parent.children().add(child));
        parent.close();
        assertThrows(IllegalStateException.class, () -> parent.addChild(style.box("D", Element.BLOCK)));
    }

    @Test
    void theStyleIsClosedWithItsBox() {
        Box section = build(BLOCK).sections().get(0).box();
        Box list = section.children().get(0).children().get(0);
        Box text = list.children().get(0).children().get(0);
        Style s = list.style();
        assertThrows(IllegalStateException.class, () -> s.setIsolateMargins(true));
        assertThrows(IllegalStateException.class, () -> s.declared(PropertyKey.FONT_SIZE, 3));
        assertThrows(IllegalStateException.class, () -> s.list().setStart(4));
        assertThrows(IllegalStateException.class, () -> s.box().setHeight(Value.pt(10)));
        // The group that a box inherits is a copy of its own, and it is closed too.
        assertNotSame(s.typography(), text.style().typography());
        assertThrows(IllegalStateException.class, () -> text.style().typography().setFontSize(12));
        assertEquals(10, text.style().typography().fontSize());
    }

    /**
     * Outside {@code tree}, {@code Box}, {@code Style} and their groups are only read: the fields of {@code Box} are
     * not public (except the constants) and those of {@code Style} and its groups are private, written through a
     * method that checks that the style is not closed.
     */
    @Test
    void noFieldIsPublicButTheConstants() {
        for (Class<?> c : List.of(Box.class, Style.class)) {
            List<String> open = Arrays.stream(c.getDeclaredFields())
                    .filter(f -> Modifier.isPublic(f.getModifiers()) && !isConstant(f))
                    .map(Field::getName).toList();
            assertEquals(List.of(), open, c.getSimpleName());
        }
        for (Class<?> c : List.of(Style.class, TypographyStyle.class, BoxStyle.class, BreakStyle.class, StackStyle.class,
                GridStyle.class, ItemStyle.class, ListStyle.class, ColumnsStyle.class, ImageStyle.class,
                TableOfContentsStyle.class)) {
            List<String> open = Arrays.stream(c.getDeclaredFields())
                    .filter(f -> !Modifier.isPrivate(f.getModifiers()) && !isConstant(f))
                    .map(Field::getName).toList();
            assertEquals(List.of(), open, c.getSimpleName());
        }
    }

    private static boolean isConstant(Field f) {
        return Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers());
    }
}
