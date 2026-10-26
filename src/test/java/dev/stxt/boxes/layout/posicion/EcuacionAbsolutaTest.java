package dev.stxt.boxes.layout.posicion;

import dev.stxt.boxes.layout.IntrinsicSizes;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EcuacionAbsolutaTest {

    private static final OptionalDouble AUTO = OptionalDouble.empty();
    private static final IntrinsicSizes CONTENIDO = new IntrinsicSizes(80, 180);

    private static OptionalDouble v(double x) {
        return OptionalDouble.of(x);
    }

    @Test
    void ejercicioModulo4AncladaALaDerecha() {
        EcuacionAbsoluta.Solucion s = EcuacionAbsoluta.horizontal(AUTO, v(180), v(0), AUTO, AUTO, 400, 20, CONTENIDO);
        assertEquals(220, s.inicio(), 1e-9);
    }

    @Test
    void derechaAutoSeDespeja() {
        EcuacionAbsoluta.Solucion s = EcuacionAbsoluta.horizontal(v(40), v(180), AUTO, AUTO, AUTO, 400, 20, CONTENIDO);
        assertEquals(180, s.fin(), 1e-9);
    }

    @Test
    void todoAutoUsaPosicionEstaticaYAjusteAlContenido() {
        EcuacionAbsoluta.Solucion s = EcuacionAbsoluta.horizontal(AUTO, AUTO, AUTO, AUTO, AUTO, 400, 20, CONTENIDO);
        assertEquals(20, s.inicio(), 1e-9);
        assertEquals(180, s.tamano(), 1e-9);
    }

    @Test
    void anchoAutoEntreIzquierdaYDerechaSeEstira() {
        EcuacionAbsoluta.Solucion s = EcuacionAbsoluta.horizontal(v(10), AUTO, v(10), AUTO, AUTO, 400, 0, CONTENIDO);
        assertEquals(380, s.tamano(), 1e-9);
    }

    @Test
    void margenesAutoCentran() {
        EcuacionAbsoluta.Solucion s = EcuacionAbsoluta.horizontal(v(0), v(200), v(0), AUTO, AUTO, 400, 0, CONTENIDO);
        assertEquals(100, s.margenInicio(), 1e-9);
        assertEquals(100, s.margenFin(), 1e-9);
    }

    @Test
    void sobreRestringidaIgnoraElFin() {
        EcuacionAbsoluta.Solucion s = EcuacionAbsoluta.horizontal(v(50), v(200), v(50), v(0), v(0), 400, 0, CONTENIDO);
        assertEquals(150, s.fin(), 1e-9);
    }

    @Test
    void verticalUsaLaAlturaDelContenido() {
        EcuacionAbsoluta.Solucion s = EcuacionAbsoluta.vertical(AUTO, AUTO, v(0), AUTO, AUTO, 300, 0, 45);
        assertEquals(45, s.tamano(), 1e-9);
        assertEquals(255, s.inicio(), 1e-9);
    }
}
