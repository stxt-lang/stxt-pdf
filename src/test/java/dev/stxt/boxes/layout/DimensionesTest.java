package dev.stxt.boxes.layout;

import dev.stxt.boxes.arbol.Estilo;
import dev.stxt.boxes.modelo.Insets;
import dev.stxt.boxes.modelo.Valor;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DimensionesTest {

    @Test
    void elMinimoGanaAlMaximo() {
        // Módulo 7: Ancho 50 % de 515 = 257.5; Max 250; Min 300 → 300.
        assertEquals(300, Dimensiones.limitar(257.5, 300, OptionalDouble.of(250)), 1e-9);
        assertEquals(250, Dimensiones.limitar(257.5, 0, OptionalDouble.of(250)), 1e-9);
    }

    @Test
    void cajaDeModulo1() {
        // 515 de containing block, margen 20 → caja de borde 475; contenido 451.
        double borde = Dimensiones.anchoAutoEnFlujo(515, Insets.todos(20));
        assertEquals(475, borde, 1e-9);
        assertEquals(451, borde - 2 * 2 - 2 * 10, 1e-9);
    }

    @Test
    void ejercicioModulo1() {
        // Ancho 50 % (border-box), relleno 10 20, borde 1 → contenido 215.5.
        double borde = new Valor.Porcentaje(50).resolver(515).getAsDouble();
        assertEquals(215.5, borde - 2 - 40, 1e-9);
        assertEquals(257.5 + 42, Dimensiones.aCajaDeBorde(257.5, Estilo.Medida.CONTENIDO, 42), 1e-9);
    }

    @Test
    void margenesAutoCentran() {
        // Módulo 2: C de 200 pt en 515 → 157.5 a cada lado.
        assertArrayEquals(new double[]{157.5, 157.5},
                Dimensiones.margenesHorizontales(515, 200, Valor.AUTO, Valor.AUTO), 1e-9);
    }
}
