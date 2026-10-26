package dev.stxt.boxes.arbol;

import dev.stxt.boxes.modelo.Lados;
import dev.stxt.boxes.modelo.Valor;
import dev.stxt.boxes.stxt.LectorStxtMinimo;
import dev.stxt.boxes.stxt.NodoStxt;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxTreeBuilderTest {

    private static BoxTreeBuilder.Resultado construir(String doc) {
        NodoStxt raiz = new LectorStxtMinimo().leer(doc, "prueba");
        return new BoxTreeBuilder().construir(raiz);
    }

    private static String documento(String... cajas) {
        return "Documento (dev.stxt.boxes):\n\tMedio:\n\t\tTipo: continuo\n\t\tAncho: 595\n\t\tMargen: 40\n"
                + String.join("\n", cajas);
    }

    @Test
    void textoMezcladoConCajasCreaAnonima() {
        BoxTreeBuilder.Resultado r = construir(documento(
                "\tCaja:",
                "\t\tId: padre",
                "\t\tTexto: antes",
                "\t\tCaja:",
                "\t\t\tId: hija"));
        Box padre = r.raiz().hijos.get(0);
        assertEquals(2, padre.hijos.size());
        assertTrue(padre.hijos.get(0).anonima);
        assertEquals(Estilo.Layout.LINEA, padre.hijos.get(0).estilo.layout);
        assertEquals("antes", padre.hijos.get(0).texto);
        assertEquals("hija", padre.hijos.get(1).id);
    }

    @Test
    void bloqueSoloConTextoUsaLayoutLinea() {
        Box b = construir(documento("\tCaja:", "\t\tTexto: hola")).raiz().hijos.get(0);
        assertEquals(Estilo.Layout.LINEA, b.estilo.layout);
        assertFalse(b.anonima);
        assertEquals("hola", b.texto);
    }

    @Test
    void herenciaSoloDeTextoYFragmentacion() {
        Box hija = construir(documento(
                "\tCaja:",
                "\t\tTamano: 14",
                "\t\tViudas: 3",
                "\t\tRelleno: 10",
                "\t\tCaja:",
                "\t\t\tTexto: x")).raiz().hijos.get(0).hijos.get(0);
        assertEquals(14, hija.estilo.tamano, 1e-9);
        assertEquals(3, hija.estilo.viudas);
        assertEquals(16.8, hija.estilo.alturaLinea(), 1e-9);
        assertEquals(Lados.todos(Valor.pt(0)), hija.estilo.relleno); // Relleno no se hereda
    }

    @Test
    void reglasQueElTemplateNoExpresa() {
        assertThrows(ModeloInvalidoException.class, () -> construir(
                "Documento:\n\tMedio:\n\t\tTipo: paginado\n\t\tAncho: 595\n\tCaja:\n\t\tAlto: 10"));
        assertThrows(ModeloInvalidoException.class, () -> construir(documento(
                "\tCaja:", "\t\tFlex:", "\t\t\tHueco: 10")));
        assertThrows(ModeloInvalidoException.class, () -> construir(documento(
                "\tCaja:", "\t\tDesplazamiento: 10")));
        assertThrows(ModeloInvalidoException.class, () -> construir(documento(
                "\tCaja:", "\t\tLayout: grid", "\t\tGrid:", "\t\t\tAreas >>", "\t\t\t\ta a", "\t\t\t\ta b", "\t\t\t\tb b")));
        assertThrows(ModeloInvalidoException.class, () -> construir(documento(
                "\tCaja:", "\t\tViudas: 0")));
        ModeloInvalidoException e = assertThrows(ModeloInvalidoException.class, () -> construir(documento(
                "\tCaja:", "\t\tAncho: 12,5")));
        assertEquals(7, e.linea());
    }

    @Test
    void elEjemploDeLaEspecificacionEsValido() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/ejemplos/informe.stxt")) {
            String doc = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            BoxTreeBuilder.Resultado r = construir(doc);
            assertTrue(r.medio().paginado());
            assertEquals(515, r.medio().areaUtil().ancho(), 1e-9);
            assertEquals(762, r.medio().areaUtil().alto(), 1e-9);
        }
    }
}
