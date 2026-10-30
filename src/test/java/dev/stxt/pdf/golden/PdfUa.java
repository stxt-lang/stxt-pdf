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
 * Validación de accesibilidad de un PDF contra PDF/UA-1 o PDF/UA-2 con veraPDF (solo para los tests). Da las
 * reglas que incumple, cada una con el número de veces y su mensaje. PDF/UA-2 se valida con el perfil de veraPDF que
 * añade las reglas de ISO/TS 32005 (qué elemento puede contener a cuál).
 */
public final class PdfUa {

    static {
        VeraGreenfieldFoundryProvider.initialise();
    }

    private PdfUa() {
    }

    /** Las reglas de PDF/UA-1 que incumple el PDF, por cláusula y número ({@code 7.18.5-1}), con su mensaje. */
    public static Map<String, String> failedRules(byte[] pdf) {
        return failedRules(pdf, 1);
    }

    /** Las reglas de la parte {@code part} de PDF/UA (1 o 2) que incumple el PDF. */
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
            throw new IllegalStateException("veraPDF no pudo validar el PDF", e);
        }
    }
}
