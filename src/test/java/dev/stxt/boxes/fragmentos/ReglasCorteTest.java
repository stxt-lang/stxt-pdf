package dev.stxt.boxes.fragmentos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReglasCorteTest {

    @Test
    void ejercicioModulo8() {
        // Área útil 762, interlineado 15, párrafo de 53 líneas que empieza en y = 720.
        int capacidad1 = ReglasCorte.capacidad(762 - 720, 15);
        assertEquals(2, capacidad1);
        int p1 = ReglasCorte.lineasQueCaben(53, capacidad1, 2, 2, false);
        int p2 = ReglasCorte.lineasQueCaben(53 - p1, ReglasCorte.capacidad(762, 15), 2, 2, true);
        int p3 = ReglasCorte.lineasQueCaben(53 - p1 - p2, ReglasCorte.capacidad(762, 15), 2, 2, true);
        assertEquals(2, p1);
        assertEquals(49, p2);
        assertEquals(2, p3);
    }

    @Test
    void huerfanaMueveElParrafoEntero() {
        assertEquals(0, ReglasCorte.lineasQueCaben(10, 1, 2, 2, false));
    }

    @Test
    void garantiaDeProgresoAlInicioDePagina() {
        assertEquals(1, ReglasCorte.lineasQueCaben(10, 1, 2, 2, true));
    }

    @Test
    void continuoNoTieneLimite() {
        assertEquals(Integer.MAX_VALUE, ReglasCorte.capacidad(Double.POSITIVE_INFINITY, 15));
    }
}
