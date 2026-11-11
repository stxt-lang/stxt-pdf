package dev.stxt.pdf.paint;

import static dev.stxt.pdf.PdfStructures.section;
import static dev.stxt.pdf.PdfStructures.types;
import static dev.stxt.pdf.tree.TextRuns.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import dev.stxt.InlineNode;
import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.layout.Engine;
import dev.stxt.pdf.layout.inline.PdfMetrics;
import dev.stxt.pdf.model.Limits;
import dev.stxt.pdf.model.TextRun;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.TextRuns;

/**
 * The marked content of a page (reference/implementation/pdf.md): the text of a box in one marked content whatever
 * its runs, the decorations as artifacts, and the {@code ActualText} that gives whoever extracts the text what was
 * written when what is painted is not it.
 */
class PdfTaggingTest {

    private static final String DOCUMENT = Docs.header(300, 200, 20) + Docs.TYPOGRAPHY;

    private static final String RUNS = "Uno negrita, cursiva, las dos, subrayado y tachado. Una palabra en "
            + "negrita a medias y un texto justificado que ocupa varias líneas con sus tramos.";

    /** The PDF of {@code document} with the runs of each text by its {@code Id}. */
    private static byte[] render(InlineNode document, Map<String, List<TextRun>> runs) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Resources resources = new Resources();
        PdfDocumentWriter.write(new Engine(new PdfMetrics(resources)).layOutDocument(TextRuns.build(document, runs)),
                resources, Limits.DEFAULT, out, false);
        return out.toByteArray();
    }

    /**
     * Each run with its font, in the same marked content of the paragraph; the underline and the strikethrough,
     * <em>artifacts</em> (reference/common/text-formatting.md).
     */
    @Test
    void runsUseTheirFontsAndDecorationsAreArtifacts() throws Exception {
        InlineNode doc = new DocumentReader().read(Docs.sectioned(Docs.header(300, 200, 40)
                + Docs.TYPOGRAPHY.replace("Size: 10", "Size: 12") + "\t\t\t\tAlign: justify\n"
                + "\tParagraph: " + RUNS + "\n\t\tId: P\n"));
        List<TextRun> runs = List.of(run("Uno "), run("negrita", "b"), run(", "), run("cursiva", "i"), run(", "),
                run("las dos", "bi"), run(", "), run("subrayado", "u"), run(" y "), run("tachado", "s"),
                run(". Una palabra en "), run("neg", "b"), run("rita a medias y un "),
                run("texto justificado que ocupa varias líneas con sus tramos", "u"), run("."));
        try (PDDocument pdf = Loader.loadPDF(render(doc, Map.of("P", runs)))) {
            List<String> names = new ArrayList<>();
            PDResources resources = pdf.getPage(0).getResources();
            for (COSName name : resources.getFontNames()) {
                String font = resources.getFont(name).getName();
                names.add(font.substring(font.indexOf('+') + 1));
            }
            names.sort(null);
            assertEquals(List.of("IBMPlexSans", "IBMPlexSans-Bold", "IBMPlexSans-BoldItalic", "IBMPlexSans-Italic"),
                    names);
            String text = new PDFTextStripper().getText(pdf).replaceAll("\\s+", " ");
            assertTrue(text.contains("Uno negrita, cursiva, las dos, subrayado y tachado."), text);
            assertTrue(text.contains("negrita a medias"), text);
            // A single P with a single marked content: the runs have no element of their own.
            PDStructureElement document = section(pdf);
            assertEquals(List.of("P"), types(document));
            PDStructureElement p = (PDStructureElement) document.getKids().get(0);
            assertEquals(1, p.getKids().size());
            String content = new String(pdf.getPage(0).getContents().readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("/Artifact BMC"), content);
        }
    }

    /**
     * With large letter spacing, an extractor would put spaces between the letters; each run carries in its marked
     * content the {@code ActualText} with its text (reference/implementation/pdf.md), which pdfbox respects.
     */
    @Test
    void letterSpacedTextIsExtractedWhole() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(DOCUMENT
                + "\tParagraph: Informe del trimestre\n\t\tTypography:\n\t\t\tLetter spacing: 3\n"
                + "\tParagraph: Informe **del** trimestre\n\t\tTypography:\n\t\t\tLetter spacing: 3\n\t\t\tMarkup: markdown\n"))) {
            // pdfbox leaves spaces after the ActualText of each sequence: the text is that of each line without them.
            assertEquals(List.of("Informe del trimestre", "Informe del trimestre"),
                    new PDFTextStripper().getText(pdf).strip().lines().map(String::stripTrailing).toList());
        }
    }

    /**
     * A word split at the end of a line (reference/common/typography.md) is painted with a hyphen, but the text that
     * is read has the soft hyphen U+00AD in its place and no space after it: the two parts are one word for whoever
     * extracts it (reference/implementation/pdf.md), and the PDF is still PDF/UA.
     */
    @Test
    void hyphenatedWordsAreReadWithASoftHyphen() throws Exception {
        byte[] bytes = Pdfs.of(DOCUMENT + "\t\t\t\tHyphenate: true\n"
                + "\tParagraph: la programación extraordinaria\n\t\tBox:\n\t\t\tWidth: 60\n");
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            // pdfbox reads the ActualText and drops its soft hyphen: each line is its part of the word, bare.
            assertEquals(List.of("la programa", "ción extraor", "dinaria"),
                    new PDFTextStripper().getText(pdf).strip().lines().map(String::stripTrailing).toList());
            // The sequence that ends each split line carries its text with the soft hyphen.
            assertEquals(List.of("la programa­", "ción extraor­"), actualTexts(pdf.getPage(0)));
        }
        assertEquals(Map.of(), PdfUa.failedRules(bytes));
    }

    /**
     * A word split after a hyphen written in it, or cut with {@code Break long words}, is painted as it is written:
     * no hyphen is added, so there is no {@code ActualText}, and no space after the line, so whoever extracts the
     * text can join its parts (reference/implementation/pdf.md).
     */
    @Test
    void wordsSplitWithoutAnAddedHyphenAreReadAsTheyAreWritten() throws Exception {
        byte[] bytes = Pdfs.of(DOCUMENT + "\t\t\t\tBreak long words: new-line\n"
                + "\tParagraph: REF-2026-009812 ES7620770024003102575766\n\t\tBox:\n\t\t\tWidth: 60\n");
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            List<String> lines = new PDFTextStripper().getText(pdf).strip().lines().toList();
            // The lines that end inside a word have nothing after their last character; the others, the space.
            assertEquals("REF-2026-", lines.get(0));
            assertEquals("REF-2026-009812ES7620770024003102575766",
                    String.join("", lines).replace(" ", ""));
            assertTrue(lines.size() > 3, lines.toString());
            for (String line : lines.subList(2, lines.size() - 1)) {
                assertEquals(line.stripTrailing(), line, lines.toString());
            }
            assertEquals(List.of(), actualTexts(pdf.getPage(0)));
        }
        assertEquals(Map.of(), PdfUa.failedRules(bytes));
    }

    /** The {@code ActualText} of each marked content of the page, in order. */
    private static List<String> actualTexts(PDPage page) throws IOException {
        List<String> texts = new ArrayList<>();
        PDFStreamParser parser = new PDFStreamParser(page);
        List<Object> operands = new ArrayList<>();
        Object token;
        while ((token = parser.parseNextToken()) != null) {
            if (!(token instanceof Operator op)) {
                operands.add(token);
                continue;
            }
            if (op.getName().equals("BDC") && operands.size() == 2 && operands.get(1) instanceof COSName name) {
                PDPropertyList properties = page.getResources().getProperties(name);
                String actual = properties == null ? null : properties.getCOSObject().getString(COSName.ACTUAL_TEXT);
                if (actual != null) {
                    texts.add(actual);
                }
            }
            operands.clear();
        }
        return texts;
    }
}
