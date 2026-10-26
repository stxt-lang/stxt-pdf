package dev.stxt.boxes.stxt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LectorStxtMinimoTest {

    private final LectorStxtMinimo lector = new LectorStxtMinimo();

    @Test
    void nodosBloquesYNamespace() {
        String doc = String.join("\n",
                "# comentario",
                "Documento (dev.stxt.boxes):",
                "\tCaja:",
                "\t\tId: principal",
                "\t\tTexto >>",
                "\t\t\tPrimera línea: con dos puntos",
                "",
                "\t\t\t\tSangrada",
                "\tCaja:",
                "\t\tId: otra");
        NodoStxt raiz = lector.leer(doc, "prueba");
        assertEquals("dev.stxt.boxes", raiz.namespace());
        assertEquals(2, raiz.hijos("Caja").size());
        NodoStxt caja = raiz.hijos("Caja").get(0);
        assertEquals("dev.stxt.boxes", caja.namespace());
        assertEquals("principal", caja.contenidoDe("Id").orElseThrow());
        assertEquals("Primera línea: con dos puntos\n\n\tSangrada", caja.contenidoDe("Texto").orElseThrow());
        assertNull(caja.hijo("Texto").orElseThrow().valor());
        assertEquals(5, caja.hijo("Texto").orElseThrow().linea());
    }

    @Test
    void espaciosEnMultiplosDeCuatro() {
        NodoStxt raiz = lector.leer("A:\n    B: 1\n        C: 2", "p");
        assertEquals("2", raiz.hijo("B").orElseThrow().contenidoDe("C").orElseThrow());
        assertThrows(ErrorStxt.class, () -> lector.leer("A:\n   B: 1", "p"));
    }

    @Test
    void erroresConLinea() {
        ErrorStxt e = assertThrows(ErrorStxt.class, () -> lector.leer("A:\n\t\tB: 1", "doc.stxt"));
        assertEquals(2, e.linea());
        assertThrows(ErrorStxt.class, () -> lector.leer("A:\n\tsin dos puntos", "p"));
        assertThrows(ErrorStxt.class, () -> lector.leer("A: 1\nB: 2", "p"));
    }
}
