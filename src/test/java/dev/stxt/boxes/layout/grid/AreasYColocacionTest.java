package dev.stxt.boxes.layout.grid;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AreasYColocacionTest {

    @Test
    void areasDeLaPaginaDelModulo6() {
        Map<String, Area> a = AreasGrid.parsear("cab cab\nlat pri\npie pie");
        assertEquals(new Area(1, 1, 1, 2), a.get("cab"));
        assertEquals(new Area(2, 1, 1, 1), a.get("lat"));
        assertEquals(new Area(2, 2, 1, 1), a.get("pri"));
        assertEquals(new Area(3, 1, 1, 2), a.get("pie"));
    }

    @Test
    void areaNoRectangularOFilasDesiguales() {
        assertThrows(IllegalArgumentException.class, () -> AreasGrid.parsear("a a\na b\nb b"));
        assertThrows(IllegalArgumentException.class, () -> AreasGrid.parsear("a a\nb"));
        assertEquals(new Area(1, 2, 1, 1), AreasGrid.parsear(". a").get("a"));
    }

    @Test
    void sintaxisDeCelda() {
        Map<String, Area> a = AreasGrid.parsear("cab cab");
        assertEquals(new Celda(1, 1, 1, 2), Celda.parsear("cab", a));
        assertEquals(new Celda(2, 3, 1, 1), Celda.parsear("2 3", a));
        assertEquals(new Celda(null, null, 1, 2), Celda.parsear("auto auto 1 2", a));
        assertThrows(IllegalArgumentException.class, () -> Celda.parsear("pie", a));
        assertThrows(IllegalArgumentException.class, () -> Celda.parsear("0 1", a));
    }

    @Test
    void automaticosRodeanAlExplicito() {
        List<Celda> c = List.of(Celda.AUTOMATICA, new Celda(1, 2, 1, 1), Celda.AUTOMATICA,
                Celda.AUTOMATICA, Celda.AUTOMATICA);
        List<Area> r = Colocacion.colocar(c, 3);
        assertEquals(new Area(1, 1, 1, 1), r.get(0));
        assertEquals(new Area(1, 2, 1, 1), r.get(1));
        assertEquals(new Area(1, 3, 1, 1), r.get(2));
        assertEquals(new Area(2, 1, 1, 1), r.get(3));
        assertEquals(new Area(2, 2, 1, 1), r.get(4));
    }

    @Test
    void modoDispersoDejaHuecosAtras() {
        // Un ítem de 2 columnas no cabe en (1,3): pasa a la fila 2 y el siguiente
        // continúa desde el cursor, sin volver al hueco (1,3).
        List<Celda> c = List.of(Celda.AUTOMATICA, Celda.AUTOMATICA,
                new Celda(null, null, 1, 2), Celda.AUTOMATICA);
        List<Area> r = Colocacion.colocar(c, 3);
        assertEquals(new Area(2, 1, 1, 2), r.get(2));
        assertEquals(new Area(2, 3, 1, 1), r.get(3));
    }
}
