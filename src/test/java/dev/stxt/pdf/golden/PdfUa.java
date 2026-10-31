package dev.stxt.pdf.golden;

import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Accessibility validation of a PDF against PDF/UA-1 or PDF/UA-2 with veraPDF (for the tests only). It gives the
 * rules the PDF fails, each with the number of times and its message. PDF/UA-2 is validated with the veraPDF profile
 * that adds the rules of ISO/TS 32005 (which element can contain which).
 */
public final class PdfUa {

    static {
        VeraGreenfieldFoundryProvider.initialise();
    }

    private PdfUa() {
    }

    /** The PDF/UA-1 rules the PDF fails, by clause and number ({@code 7.18.5-1}), with their message. */
    public static Map<String, String> failedRules(byte[] pdf) {
        return failedRules(pdf, 1);
    }

    /** The rules of part {@code part} of PDF/UA (1 or 2) that the PDF fails. */
    public static Map<String, String> failedRules(byte[] pdf, int part) {
        PDFAFlavour flavour = part == 2 ? PDFAFlavour.PDFUA_2 : PDFAFlavour.PDFUA_1;
        try (PDFAParser parser = Foundries.defaultInstance().createParser(new ByteArrayInputStream(pdf), flavour)) {
            PDFAValidator validator = Foundries.defaultInstance().createValidator(flavour, false);
            Map<String, String> failed = new LinkedHashMap<>();
            Map<String, Integer> count = new LinkedHashMap<>();
            for (TestAssertion a : validator.validate(parser).getTestAssertions()) {
                String rule = a.getRuleId().getClause() + "-" + a.getRuleId().getTestNumber();
                count.merge(rule, 1, Integer::sum);
                failed.putIfAbsent(rule, a.getMessage());
            }
            count.forEach((rule, times) -> failed.computeIfPresent(rule, (r, message) -> message + " (x" + times + ")"));
            return failed;
        } catch (Exception e) {
            throw new IllegalStateException("veraPDF could not validate the PDF", e);
        }
    }
}
