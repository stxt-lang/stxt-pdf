package dev.stxt.pdf.golden;

import dev.stxt.pdf.model.PdfA;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Validation of a PDF against PDF/UA-1, PDF/UA-2 or a PDF/A with veraPDF (for the tests only). It gives the
 * rules the PDF fails, each with the number of times and its message. PDF/UA-2 is validated with the veraPDF profile
 * that adds the rules of ISO/TS 32005 (which element can contain which).
 *
 * <p>veraPDF validates with {@link Locale#ROOT} as the default locale of the JVM: it lowercases the value types of the
 * XMP extension schemas with the default locale ({@code ValidatorsContainer.getSimplifiedType} in 1.28.2), and in
 * Turkish {@code Integer}, the type of {@code pdfuaid:part}, becomes {@code ınteger} (dotless i), a type it does not
 * know, so every PDF/A fails 6.6.2.3.1 and 6.6.2.3.3 although the PDF is the same in every locale (the example tests
 * compare its bytes). The tests do not run in parallel, so the locale is swapped only for the validation.</p>
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
        return failedRules(pdf, part == 2 ? PDFAFlavour.PDFUA_2 : PDFAFlavour.PDFUA_1);
    }

    /** The veraPDF flavour of a PDF/A ({@code PDFA_3_B}...). */
    public static PDFAFlavour flavour(PdfA pdfA) {
        return switch (pdfA) {
            case A2B -> PDFAFlavour.PDFA_2_B;
            case A2A -> PDFAFlavour.PDFA_2_A;
            case A3B -> PDFAFlavour.PDFA_3_B;
            case A3A -> PDFAFlavour.PDFA_3_A;
        };
    }

    /** The rules of {@code flavour} (a PDF/UA or a PDF/A profile of veraPDF) that the PDF fails. */
    public static Map<String, String> failedRules(byte[] pdf, PDFAFlavour flavour) {
        Locale locale = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
        try {
            return validate(pdf, flavour);
        } finally {
            Locale.setDefault(locale);
        }
    }

    private static Map<String, String> validate(byte[] pdf, PDFAFlavour flavour) {
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
