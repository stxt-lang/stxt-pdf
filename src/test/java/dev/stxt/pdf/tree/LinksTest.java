package dev.stxt.pdf.tree;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.model.TextFormat;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Links in the box tree (reference/common/links.md). */
class LinksTest {

    private static final DocumentReader READER = new DocumentReader();

    /** A document with these element lines, unindented, after the header (line 8 onward). */
    private static InlineNode document(String... body) {
        List<String> lines = new ArrayList<>(List.of("Document (dev.stxt.pdf):", "\tLanguage: es", "\tTitle: Prueba", "\tPage:", "\t\tWidth: 595",
                "\t\tHeight: 842", "\tStyles:", "\t\tFor element: Document", "\t\t\tTypography:", "\t\t\t\tFont: IBM Plex Sans", "\t\t\t\tSize: 10"));
        lines.addAll(List.of(body));
        return READER.read(Docs.sectioned(Docs.mergeStyles(String.join("\n", lines))));
    }

    private static Box build(String... body) {
        return new BoxTreeBuilder().build(document(body)).sections().get(0).box();
    }

    private static InvalidModelException error(String... body) {
        return assertThrows(InvalidModelException.class, () -> build(body));
    }

    @Test
    void linkIsAPropertyOfTheElementAndIsNotInherited() {
        Box root = build("\tBlock:", "\t\tLink: https://stxt.dev", "\t\tText: dentro", "\t\tImage: test/wide.png",
                "\t\t\tLink: mailto:a@b.c");
        Box block = root.children().get(0);
        assertEquals("https://stxt.dev", block.style().link());
        assertEquals(14, block.style().line("link").number());
        // What is inside a box with a link is clickable through it, but carries no link of its own.
        assertNull(block.children().get(0).style().link());
        assertEquals("mailto:a@b.c", block.children().get(1).style().link());
        assertNull(root.children().get(0).children().get(0).style().link());
    }

    @Test
    void linkIsAdmittedInTheElementsWithContent() {
        Box root = build("\tText: a", "\t\tLink: https://a.example", "\tParagraph: b", "\t\tLink: https://b.example",
                "\tHeading 1:", "\t\tLink: https://c.example", "\t\tText: C",
                "\tStack:", "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tLink: https://d.example",
                "\tTable:", "\t\tColumn sizes: 100", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: e", "\t\t\t\t\tLink: https://e.example");
        assertEquals("https://a.example", root.children().get(0).style().link());
        assertEquals("https://b.example", root.children().get(1).style().link());
        assertEquals("https://c.example", root.children().get(2).style().link());
        assertEquals("https://d.example", root.children().get(3).children().get(0).style().link());
        assertEquals("https://e.example", root.children().get(4).children().get(0).style().link());
    }

    @Test
    void anInternalLinkMayGoForwardOrBackward() {
        Box root = build("\tText: antes", "\t\tId: a", "\t\tLink: #b", "\tText: después", "\t\tId: b", "\t\tLink: #a");
        assertEquals("#b", root.children().get(0).style().link());
        assertEquals("#a", root.children().get(1).style().link());
    }

    @Test
    void linkCanBeNeitherEmptyNorOutsideTheAllowedSchemes() {
        InvalidModelException scheme = error("\tText: a", "\t\tLink: javascript:alert(1)");
        assertEquals(ErrorCode.LINK_SCHEME, scheme.errorCode());
        assertEquals(14, scheme.line());
        assertEquals(ErrorCode.LINK_SCHEME, error("\tText: a", "\t\tLink: docs/a.pdf").errorCode());
        assertEquals(ErrorCode.LINK_SCHEME, error("\tText: a", "\t\tLink: #").errorCode());
    }

    @Test
    void anInternalLinkNeedsABoxWithThatId() {
        InvalidModelException unknown = error("\tText: a", "\t\tLink: #nada");
        assertEquals(ErrorCode.LINK_UNKNOWN, unknown.errorCode());
        // The line of the Link, not that of its box.
        assertEquals(14, unknown.line());
        InvalidModelException fixed = error("\tPage overlay:", "\t\tText: cabecera", "\t\t\tId: cabecera",
                "\tText: a", "\t\tLink: #cabecera");
        assertEquals(ErrorCode.LINK_UNKNOWN, fixed.errorCode());
        assertTrue(fixed.getMessage().contains("Page overlay"), fixed.getMessage());
    }

    @Test
    void theRunsOfATextAreCheckedToo() {
        // The runs are set before closing the tree, which does not change afterwards.
        new BoxTreeBuilder().build(document("\tText: Véase el anexo", "\t\tId: t", "\tText: Anexo", "\t\tId: anexo"),
                null, root -> {
                    TextRuns.set(root, "t", List.of(TextRuns.run("Véase el "),
                            new TextRun("anexo", TextFormat.PLAIN.withLink("#anexo"))));
                    LinkTargets.validate(root);
                    TextRuns.set(root, "t", List.of(TextRuns.run("Véase el "),
                            new TextRun("anexo", TextFormat.PLAIN.withLink("#nada"))));
                    InvalidModelException e = assertThrows(InvalidModelException.class,
                            () -> LinkTargets.validate(root));
                    assertEquals(ErrorCode.LINK_UNKNOWN, e.errorCode());
                    // Without a line of its own for the run, that of the text.
                    assertEquals(13, e.line());
                });
    }

    @Test
    void markdownLinksAreCheckedWithTheLineOfTheirBlock() {
        InvalidModelException e = error("\tMarkdown:", "\t\tContent >>", "\t\t\tUn párrafo.", "", "\t\t\tVéase [el anexo](#anexo).");
        assertEquals(ErrorCode.LINK_UNKNOWN, e.errorCode());
        assertEquals(17, e.line());
    }

    @Test
    void everyContentsEntryLinksToItsHeading() {
        Box root = build("\tTable of contents:", "\tHeading 1:", "\t\tId: uno", "\t\tText: Uno",
                "\tMarkdown:", "\t\tContent >>", "\t\t\t# Dos", "", "\t\t\t## Dos punto uno");
        Box contents = root.children().get(0);
        assertEquals(3, contents.children().size());
        // A heading with Id is linked by it; one from Markdown, by the name the table of contents gives it.
        assertEquals("#uno", contents.children().get(0).style().link());
        Box markdownHeading = root.children().get(2).children().get(0);
        assertEquals("#" + markdownHeading.key(), contents.children().get(1).style().link());
        assertTrue(markdownHeading.key() != null);
        // Its children (title, leader and number) carry no link: the entry carries it.
        contents.children().get(0).children().forEach(c -> assertNull(c.style().link()));
    }
}
