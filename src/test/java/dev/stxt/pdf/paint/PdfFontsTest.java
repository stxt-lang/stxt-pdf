package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.Pdfs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.golden.PdfUa;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.stxt.DocumentReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fonts of the PDF (reference/implementation/pdf.md): each one embedded once as a Type 0 font, as a subset when
 * PDFBox can; and the arguments of {@code TJ} that justify, space and kern the text.
 */
class PdfFontsTest {

    private static final String DOCUMENT = Docs.header(300, 200, 20) + Docs.TYPOGRAPHY;

    private final DocumentReader reader = new DocumentReader();

    @Test
    void embedsTheFontsAsSubsets() throws Exception {
        try (PDDocument pdf = Loader.loadPDF(Pdfs.of(reader.read(Path.of("test", "spec", "report.stxt"))))) {
            PDResources resources = pdf.getPage(0).getResources();
            List<String> names = new ArrayList<>();
            for (COSName name : resources.getFontNames()) {
                PDFont font = resources.getFont(name);
                assertInstanceOf(PDType0Font.class, font);
                assertTrue(font.isEmbedded(), font.getName());
                names.add(font.getName().substring(font.getName().indexOf('+') + 1));
            }
            names.sort(null);
            // The title in bold; the rest, normal.
            assertEquals(List.of("IBMPlexSans", "IBMPlexSans-Bold"), names);
        }
    }

    /**
     * Fonts with TrueType and with CFF outlines (reference/implementation/pdf.md): Light and Medium are the TTF of the
     * repository and are embedded as subsets; SemiBold is the OTF with CFF outlines of the resources of the tests,
     * which PDFBox cannot subset: it is embedded whole, as a {@code CIDFontType0} with the file in {@code FontFile3}
     * and subtype {@code OpenType}. The dictionaries are read as they are written, without {@code PDFontFactory},
     * which would change them when loading. The text is extracted and the PDF passes PDF/UA-1 and PDF/UA-2.
     */
    @Test
    void embedsTheTrueTypeFontsAsSubsetsAndTheCffOnesWhole(@TempDir Path dir) throws Exception {
        Path fonts = Files.createDirectories(dir.resolve("fonts"));
        for (String variant : List.of("Light", "Medium")) {
            Files.copy(Resources.DEFAULT_ROOT.resolve("fonts/IBM Plex Sans/IBMPlexSans-" + variant + ".ttf"),
                    fonts.resolve(variant + ".ttf"));
        }
        Files.copy(Path.of("test/resources/fonts/IBM Plex Sans/IBMPlexSans-SemiBold.otf"),
                fonts.resolve("SemiBold.otf"));
        Resources resources = new Resources(dir);
        StringBuilder texts = new StringBuilder();
        for (int weight : List.of(300, 500, 600)) {
            texts.append("\tText: Peso ").append(weight).append(" con eñe\n\t\tTypography:\n\t\t\tWeight: ")
                    .append(weight).append('\n');
        }
        byte[] bytes = null;
        for (int ua : List.of(1, 2)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Converter.builder().resources(resources).pdfUa(ua).build()
                    .toPdf(reader.read(Docs.sectioned(DOCUMENT + texts)), out);
            assertEquals(Map.of(), PdfUa.failedRules(out.toByteArray(), ua), "PDF/UA-" + ua);
            bytes = bytes == null ? out.toByteArray() : bytes;
        }
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            COSDictionary dictionaries = pdf.getPage(0).getResources().getCOSObject().getCOSDictionary(COSName.FONT);
            Map<String, String> kinds = new TreeMap<>();
            for (COSName name : dictionaries.keySet()) {
                COSDictionary font = dictionaries.getCOSDictionary(name);
                COSDictionary descendant = (COSDictionary) font.getCOSArray(COSName.DESCENDANT_FONTS).getObject(0);
                COSDictionary descriptor = descendant.getCOSDictionary(COSName.FONT_DESC);
                COSName fileKey = descriptor.containsKey(COSName.FONT_FILE3) ? COSName.FONT_FILE3 : COSName.FONT_FILE2;
                COSStream file = (COSStream) descriptor.getDictionaryObject(fileKey);
                String subtype = file.getNameAsString(COSName.SUBTYPE);
                kinds.put(font.getNameAsString(COSName.BASE_FONT), descendant.getNameAsString(COSName.SUBTYPE) + " "
                        + fileKey.getName() + (subtype == null ? "" : " " + subtype));
            }
            Map<String, String> byWeight = new TreeMap<>();
            kinds.forEach((base, kind) -> byWeight.put(base.substring(base.indexOf('+') + 1), kind));
            assertEquals(Map.of("IBMPlexSans-Light", "CIDFontType2 FontFile2",
                    "IBMPlexSans-Medm", "CIDFontType2 FontFile2",
                    "IBMPlexSans-SmBld", "CIDFontType0 FontFile3 OpenType"), byWeight);
            // The TrueType ones are subsets (a prefix of six letters and +); the CFF one is not.
            assertTrue(kinds.keySet().stream().filter(k -> k.contains("Light") || k.contains("Medm"))
                    .allMatch(k -> k.matches("[A-Z]{6}\\+.*")), kinds.toString());
            assertTrue(kinds.containsKey("IBMPlexSans-SmBld"), kinds.toString());
            String text = new PDFTextStripper().getText(pdf);
            for (int weight : List.of(300, 500, 600)) {
                assertTrue(text.contains("Peso " + weight + " con eñe"), text);
            }
        }
    }

    @Test
    void letterSpacingIsATjShiftBetweenCharactersButNotAfterTheLast() {
        // 1.5 pt at size 10: 150 thousandths; a positive number in TJ brings closer, a negative one separates.
        assertArrayEquals(new Object[]{"a", -150f, "b", -150f, "c"}, PdfFonts.spaced("abc", 0, 1.5, 10, null));
        // With justify, the extra space is added after a space.
        assertArrayEquals(new Object[]{"a", -150f, " ", -650f, "b"}, PdfFonts.spaced("a b", 5, 1.5, 10, null));
        // Without letter spacing it is the usual TJ.
        assertArrayEquals(PdfFonts.justified("uno dos", 5, 10), PdfFonts.spaced("uno dos", 5, 0, 10, null));
    }

    @Test
    void justifiedTextUsesTj() {
        assertArrayEquals(new Object[]{"uno ", -500f, "dos ", -500f, "tres"},
                PdfFonts.justified("uno dos tres", 5, 10));
        assertArrayEquals(new Object[]{"palabra"}, PdfFonts.justified("palabra", 5, 10));
    }

    @Test
    void kerningIsATjShiftBetweenThePairs() {
        // -41 thousandths between A and V: in TJ, 41 brings V closer.
        int[] kerning = {0, -41, 0, 0};
        assertArrayEquals(new Object[]{"A", 41f, "V a"}, PdfFonts.spaced("AV a", 0, 0, 10, kerning));
        // With justify and with letter spacing, the kerning is added to the shift of its pair.
        assertArrayEquals(new Object[]{"A", 41f, "V ", -500f, "a"}, PdfFonts.spaced("AV a", 5, 0, 10, kerning));
        assertArrayEquals(new Object[]{"A", -109f, "V", -150f, " ", -650f, "a"},
                PdfFonts.spaced("AV a", 5, 1.5, 10, kerning));
    }

    /**
     * With {@code Kerning} (reference/common/typography.md), the pairs are shifts of {@code TJ} between the glyphs of
     * the same embedded font as without it; the text is extracted as it is written, and the PDF is still PDF/UA, also
     * with ligatures and with a word split with a hyphen.
     */
    @Test
    void kernedTextIsExtractedWholeWithTheSameFont() throws Exception {
        byte[] bytes = Pdfs.of(DOCUMENT + "\t\t\t\tKerning: true\n"
                + "\tParagraph: AVATAR Tofi\n"
                + "\tParagraph: AVATAR Tofi\n\t\tTypography:\n\t\t\tKerning: false\n"
                + "\tParagraph: Tofi Tofi\n\t\tTypography:\n\t\t\tLigatures: true\n"
                + "\tParagraph: la programación\n\t\tBox:\n\t\t\tWidth: 60\n\t\tTypography:\n\t\t\tHyphenate: true\n");
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            assertEquals(List.of("AVATAR Tofi", "AVATAR Tofi", "Tofi Tofi", "la programa", "ción"),
                    new PDFTextStripper().getText(pdf).strip().lines().map(String::stripTrailing).toList());
            PDPage page = pdf.getPage(0);
            // The regular font with and without kerning, and with ligatures: the same file, twice.
            assertEquals(2, page.getResources().getCOSObject().getCOSDictionary(COSName.FONT).size());
            List<List<Object>> tj = tjArguments(page);
            // The first paragraph: A-V, V-A, A-T and T-A of AVATAR (41, 41, 65, 65) and T-o and o-f of Tofi (65, 6).
            assertEquals(List.of(41f, 41f, 65f, 65f, 65f, 6f), numbers(tj.get(0)));
            // Without kerning, a plain Tj; with ligatures, the pair is o and the fi ligature, with its own value.
            assertEquals(List.of(65f, 6f, 65f, 6f), numbers(tj.get(1)));
        }
        assertEquals(Map.of(), PdfUa.failedRules(bytes));
    }

    /** The arguments of each {@code TJ} of the page, in order. */
    private static List<List<Object>> tjArguments(PDPage page) throws IOException {
        List<List<Object>> result = new ArrayList<>();
        PDFStreamParser parser = new PDFStreamParser(page);
        List<Object> operands = new ArrayList<>();
        Object token;
        while ((token = parser.parseNextToken()) != null) {
            if (!(token instanceof Operator op)) {
                operands.add(token);
                continue;
            }
            if (op.getName().equals("TJ") && operands.get(0) instanceof COSArray array) {
                List<Object> items = new ArrayList<>();
                array.forEach(items::add);
                result.add(items);
            }
            operands.clear();
        }
        return result;
    }

    /** The shifts of the arguments of a {@code TJ}. */
    private static List<Float> numbers(List<Object> tj) {
        return tj.stream().filter(COSNumber.class::isInstance).map(n -> ((COSNumber) n).floatValue()).toList();
    }
}
