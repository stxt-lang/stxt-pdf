package dev.stxt.pdf.paint;

import dev.stxt.pdf.Docs;
import dev.stxt.pdf.convert.Converter;
import dev.stxt.pdf.model.Check;
import dev.stxt.pdf.model.Colors;
import dev.stxt.pdf.stxt.DocumentReader;
import dev.stxt.pdf.tree.ErrorCode;
import dev.stxt.pdf.tree.InvalidModelException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El contraste del texto con lo que hay debajo (referencia/comun/pdf.md): 4,5:1, o 3:1 en el texto grande, con los
 * fondos pintados antes que el texto mezclados con su opacidad sobre la página blanca.
 */
class ContrastCheckTest {

    /** Cabecera y base tipográfica en 12 líneas, y la sección en la 13: su contenido empieza en la línea 14. */
    private static final String HEADER = "Document (dev.stxt.pdf):\n\tLanguage: es\n\tTitle: Prueba\n\tPage:\n"
            + "\t\tWidth: 400\n\t\tHeight: 300\n\t\tMargin: 20\n\tStyles:\n\t\tFor element: Document\n\t\t\tTypography:\n"
            + "\t\t\t\tFont: IBM Plex Sans\n\t\t\t\tSize: 10\n";

    private static void layOut(String... lines) {
        Converter.create().layOut(new DocumentReader().read(Docs.sectioned(HEADER + String.join("\n", lines))));
    }

    private static InvalidModelException error(String... lines) {
        InvalidModelException e = assertThrows(InvalidModelException.class, () -> layOut(lines));
        assertEquals(ErrorCode.CONTRAST_LOW, e.errorCode());
        return e;
    }

    @Test
    void theRatioIsTheOneOfTheGuidelines() {
        assertEquals(21, ContrastCheck.ratio(Colors.rgba("#000000"), Colors.rgba("#FFFFFF")), 1e-6);
        assertEquals(1, ContrastCheck.ratio(Colors.rgba("#777777"), Colors.rgba("#777777")), 1e-6);
        assertEquals(4.48, ContrastCheck.ratio(Colors.rgba("#777777"), Colors.rgba("#FFFFFF")), 0.01);
        // La mitad de negro sobre blanco es el gris medio.
        float[] half = ContrastCheck.over(Colors.rgba("#00000080"), Colors.rgba("#FFFFFF"));
        assertEquals(0.5, half[0], 0.01);
        assertEquals(1, half[3], 1e-6);
    }

    @Test
    void lightGreyOnWhiteIsAnErrorAtTheLineOfItsColor() {
        InvalidModelException e = error("\tText: hola", "\t\tTypography:", "\t\t\tColor: #999999");
        assertEquals(16, e.line());
        assertTrue(e.getMessage().contains("contraste de 2,84:1 con su fondo (#999999 sobre #FFFFFF) y necesita 4,5:1"),
                e.getMessage());
    }

    @Test
    void theInheritedColorPointsToWhereItIsDeclared() {
        InvalidModelException e = error("\tBlock:", "\t\tTypography:", "\t\t\tColor: #999999", "\t\tText: hola");
        assertEquals(16, e.line());
    }

    @Test
    void largeTextNeedsThreeToOne() {
        // #949494 sobre blanco: 3,03:1.
        error("\tText: hola", "\t\tTypography:", "\t\t\tColor: #949494", "\t\t\tSize: 17");
        assertDoesNotThrow(() -> layOut("\tText: hola", "\t\tTypography:", "\t\t\tColor: #949494", "\t\t\tSize: 18"));
        error("\tText: hola", "\t\tTypography:", "\t\t\tColor: #949494", "\t\t\tSize: 13", "\t\t\tBold: true");
        assertDoesNotThrow(() -> layOut("\tText: hola", "\t\tTypography:", "\t\t\tColor: #949494", "\t\t\tSize: 14",
                "\t\t\tBold: true"));
    }

    @Test
    void theBackgroundOfTheBoxIsWhatIsBelow() {
        assertDoesNotThrow(() -> layOut("\tBlock:", "\t\tBox:", "\t\t\tBackground: #1A2B3C", "\t\tText: hola",
                "\t\t\tTypography:", "\t\t\t\tColor: #FFFFFF"));
        InvalidModelException e = error("\tBlock:", "\t\tBox:", "\t\t\tBackground: #1A2B3C", "\t\tText: hola");
        assertTrue(e.getMessage().contains("(#000000 sobre #1A2B3C)"), e.getMessage());
    }

    @Test
    void aTranslucentBackgroundMixesWithWhatIsBelow() {
        // Negro a la mitad sobre la página blanca es gris medio: el blanco encima no llega.
        InvalidModelException e = error("\tBlock:", "\t\tBox:", "\t\t\tBackground: #00000080", "\t\tText: hola",
                "\t\t\tTypography:", "\t\t\t\tColor: #FFFFFF");
        assertTrue(e.getMessage().contains("(#FFFFFF sobre #7F7F7F)"), e.getMessage());
    }

    @Test
    void everyColorOfAGradientCounts() {
        InvalidModelException e = error("\tBlock:", "\t\tBox:", "\t\t\tBackground: linear(90, #FFFFFF, #000000)",
                "\t\tText: hola");
        assertTrue(e.getMessage().contains("(#000000 sobre #000000)"), e.getMessage());
    }

    @Test
    void textOverAnImageIsNotChecked() {
        // Lo que hay debajo es la imagen: no se sabe.
        assertDoesNotThrow(() -> layOut("\tBlock:", "\t\tImage: test/wide.png", "\t\t\tBox:", "\t\t\t\tWidth: 300",
                "\t\tOverlay:", "\t\t\tOffset: 0 auto auto 0", "\t\t\tText: hola", "\t\t\t\tTypography:",
                "\t\t\t\t\tColor: #FFFFFF"));
        // Con un fondo opaco encima de la imagen, se vuelve a saber.
        error("\tBlock:", "\t\tImage: test/wide.png", "\t\t\tBox:", "\t\t\t\tWidth: 300",
                "\t\tOverlay:", "\t\t\tOffset: 0 auto auto 0", "\t\t\tBox:", "\t\t\t\tBackground: #EEEEEE",
                "\t\t\tText: hola", "\t\t\t\tTypography:", "\t\t\t\t\tColor: #FFFFFF");
    }

    @Test
    void theCheckCanBeTurnedOff() {
        String document = HEADER + "\tSection:\n\t\tText: hola\n\t\t\tTypography:\n\t\t\t\tColor: #EEEEEE\n";
        assertDoesNotThrow(() -> Converter.builder().exclude(Check.CONTRAST).build()
                .layOut(new DocumentReader().read(document)));
    }
}
