package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.TextFormat;
import dev.stxt.boxes.model.TextRun;
import dev.stxt.boxes.stxt.DocumentReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los enlaces en el árbol de cajas (ESPECIFICACION.md §5.20). */
class LinksTest {

    private static final DocumentReader READER = new DocumentReader();

    /** Un documento con estas líneas de elementos, sin sangrar, tras la cabecera (línea 8 en adelante). */
    private static Box build(String... body) {
        List<String> lines = new ArrayList<>(List.of("Document (dev.stxt.boxes):", "\tLanguage: es", "\tMedia:", "\t\tWidth: 595",
                "\t\tHeight: 842", "\tTypography:", "\t\tFont: IBM Plex Sans", "\t\tSize: 10"));
        lines.addAll(List.of(body));
        return new BoxTreeBuilder().build(READER.read(String.join("\n", lines))).root();
    }

    private static InvalidModelException error(String... body) {
        return assertThrows(InvalidModelException.class, () -> build(body));
    }

    @Test
    void linkIsAPropertyOfTheElementAndIsNotInherited() {
        Box root = build("\tBlock:", "\t\tLink: https://stxt.dev", "\t\tText: dentro", "\t\tImage: test/wide.png",
                "\t\t\tLink: mailto:a@b.c");
        Box block = root.children.get(0);
        assertEquals("https://stxt.dev", block.style.link);
        assertEquals(10, block.style.line("link"));
        // Lo que hay dentro de una caja con enlace es clicable por ella, pero no lleva enlace propio.
        assertNull(block.children.get(0).style.link);
        assertEquals("mailto:a@b.c", block.children.get(1).style.link);
        assertNull(root.children.get(0).children.get(0).style.link);
    }

    @Test
    void linkIsAdmittedInTheElementsWithContent() {
        Box root = build("\tText: a", "\t\tLink: https://a.example", "\tParagraph: b", "\t\tLink: https://b.example",
                "\tHeading 1:", "\t\tLink: https://c.example", "\t\tText: C",
                "\tFlex:", "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tLink: https://d.example",
                "\tTable:", "\t\tColumns: 100", "\t\tBody:", "\t\t\tRow:", "\t\t\t\tCell: e", "\t\t\t\t\tLink: https://e.example");
        assertEquals("https://a.example", root.children.get(0).style.link);
        assertEquals("https://b.example", root.children.get(1).style.link);
        assertEquals("https://c.example", root.children.get(2).style.link);
        assertEquals("https://d.example", root.children.get(3).children.get(0).style.link);
        assertEquals("https://e.example", root.children.get(4).children.get(0).style.link);
    }

    @Test
    void anInternalLinkMayGoForwardOrBackward() {
        Box root = build("\tText: antes", "\t\tId: a", "\t\tLink: #b", "\tText: después", "\t\tId: b", "\t\tLink: #a");
        assertEquals("#b", root.children.get(0).style.link);
        assertEquals("#a", root.children.get(1).style.link);
    }

    @Test
    void linkCanBeNeitherEmptyNorOutsideTheAllowedSchemes() {
        InvalidModelException scheme = error("\tText: a", "\t\tLink: javascript:alert(1)");
        assertEquals(ErrorCode.LINK_SCHEME, scheme.code());
        assertEquals(10, scheme.line());
        assertEquals(ErrorCode.LINK_SCHEME, error("\tText: a", "\t\tLink: docs/a.pdf").code());
        assertEquals(ErrorCode.LINK_SCHEME, error("\tText: a", "\t\tLink: #").code());
    }

    @Test
    void anInternalLinkNeedsABoxWithThatId() {
        InvalidModelException unknown = error("\tText: a", "\t\tLink: #nada");
        assertEquals(ErrorCode.LINK_UNKNOWN, unknown.code());
        // La línea del Link, no la de su caja.
        assertEquals(10, unknown.line());
        InvalidModelException fixed = error("\tFixed:", "\t\tText: cabecera", "\t\t\tId: cabecera",
                "\tText: a", "\t\tLink: #cabecera");
        assertEquals(ErrorCode.LINK_UNKNOWN, fixed.code());
        assertTrue(fixed.getMessage().contains("Fixed"), fixed.getMessage());
    }

    @Test
    void theRunsOfATextAreCheckedToo() {
        Box root = build("\tText: Véase el anexo", "\t\tId: t", "\tText: Anexo", "\t\tId: anexo");
        TextRuns.set(root, "t", List.of(TextRuns.run("Véase el "),
                new TextRun("anexo", TextFormat.PLAIN.withLink("#anexo"))));
        LinkTargets.validate(root);
        TextRuns.set(root, "t", List.of(TextRuns.run("Véase el "),
                new TextRun("anexo", TextFormat.PLAIN.withLink("#nada"))));
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> LinkTargets.validate(root));
        assertEquals(ErrorCode.LINK_UNKNOWN, e.code());
        // Sin una línea propia para el tramo, la del texto.
        assertEquals(9, e.line());
    }

    @Test
    void markdownLinksAreCheckedWithTheLineOfTheirBlock() {
        InvalidModelException e = error("\tMarkdown:", "\t\tContent >>", "\t\t\tUn párrafo.", "", "\t\t\tVéase [el anexo](#anexo).");
        assertEquals(ErrorCode.LINK_UNKNOWN, e.code());
        assertEquals(13, e.line());
    }

    @Test
    void everyContentsEntryLinksToItsHeading() {
        Box root = build("\tContents:", "\tHeading 1:", "\t\tId: uno", "\t\tText: Uno",
                "\tMarkdown:", "\t\tContent >>", "\t\t\t# Dos", "", "\t\t\t## Dos punto uno");
        Box contents = root.children.get(0);
        assertEquals(3, contents.children.size());
        // Un encabezado con Id se enlaza por él; uno de Markdown, por el nombre que le da el índice.
        assertEquals("#uno", contents.children.get(0).style.link);
        Box markdownHeading = root.children.get(2).children.get(0);
        assertEquals("#" + markdownHeading.key(), contents.children.get(1).style.link);
        assertTrue(markdownHeading.key() != null);
        // Sus hijos (título, guía y número) no llevan enlace: lo lleva la entrada.
        contents.children.get(0).children.forEach(c -> assertNull(c.style.link));
    }
}
