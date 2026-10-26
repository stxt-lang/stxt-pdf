package dev.stxt.boxes.modelo;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValorParserTest {

    @Test
    void numerosPorcentajesYFracciones() {
        assertEquals(new Valor.Puntos(12.5), ValorParser.valor("12.5"));
        assertEquals(new Valor.Porcentaje(50), ValorParser.valor(" 50% "));
        assertEquals(new Valor.Fraccion(2), ValorParser.valor("2fr"));
        assertEquals(Valor.AUTO, ValorParser.valor("auto"));
        assertEquals(Valor.Palabra.MIN_CONTENT, ValorParser.valor("min-content"));
    }

    @Test
    void comaDecimalNoValida() {
        assertThrows(IllegalArgumentException.class, () -> ValorParser.valor("12,5"));
    }

    @Test
    void funciones() {
        Valor clamp = ValorParser.valor("clamp(200, 50%, 400)");
        assertEquals(300, clamp.resolver(600).getAsDouble(), 1e-9);
        assertEquals(200, clamp.resolver(100).getAsDouble(), 1e-9);
        assertEquals(400, clamp.resolver(1000).getAsDouble(), 1e-9);
        assertTrue(clamp.resolver(OptionalDouble.empty()).isEmpty());

        Valor mm = ValorParser.valor("minmax(100, 1fr)");
        assertEquals(new Valor.MinMax(new Valor.Puntos(100), new Valor.Fraccion(1)), mm);
        assertThrows(IllegalArgumentException.class, () -> ValorParser.valor("minmax(1fr, 100)"));
    }

    @Test
    void listasConRepetir() {
        List<Valor> v = ValorParser.lista("150 repetir(2, 1fr auto) minmax(100, 2fr)");
        assertEquals(6, v.size());
        assertEquals(new Valor.Fraccion(1), v.get(1));
        assertEquals(Valor.AUTO, v.get(4));
    }

    @Test
    void porcentajeSobreBaseIndefinida() {
        assertTrue(ValorParser.valor("50%").resolver(OptionalDouble.empty()).isEmpty());
        assertEquals(257.5, ValorParser.valor("50%").resolver(515).getAsDouble(), 1e-9);
    }
}
