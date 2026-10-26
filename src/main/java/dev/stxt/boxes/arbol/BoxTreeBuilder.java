package dev.stxt.boxes.arbol;

import dev.stxt.boxes.layout.grid.Area;
import dev.stxt.boxes.layout.grid.AreasGrid;
import dev.stxt.boxes.layout.grid.Celda;
import dev.stxt.boxes.modelo.Insets;
import dev.stxt.boxes.modelo.Lados;
import dev.stxt.boxes.modelo.Valor;
import dev.stxt.boxes.modelo.ValorParser;
import dev.stxt.boxes.stxt.NodoStxt;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

/**
 * Construye el árbol de cajas a partir del documento (módulos 0 y 1, ESPECIFICACION.md §5.1):
 * interpreta los valores, aplica la herencia, crea las cajas anónimas y comprueba las reglas
 * que el template no puede expresar (§6).
 */
public final class BoxTreeBuilder {

    private static final Pattern COLOR = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})");

    /** Resultado: el medio y una caja raíz anónima que representa el área útil. */
    public record Resultado(Medio medio, Box raiz) {
    }

    public Resultado construir(NodoStxt documento) {
        if (!documento.es("Documento")) {
            throw error(documento, "La raíz del documento debe ser Documento");
        }
        NodoStxt nodoMedio = documento.hijo("Medio")
                .orElseThrow(() -> error(documento, "Falta el nodo Medio"));
        Medio medio = medio(nodoMedio);

        Box raiz = new Box(null, new Estilo(), true, documento.linea());
        construirContenido(documento, raiz);
        if (raiz.hijos.isEmpty()) {
            throw error(documento, "El documento necesita al menos una Caja");
        }
        return new Resultado(medio, raiz);
    }

    // ------------------------------------------------------------------------------------------
    // Medio
    // ------------------------------------------------------------------------------------------

    private Medio medio(NodoStxt n) {
        Medio.Tipo tipo = enumerado(Medio.Tipo.class, requerido(n, "Tipo"), n);
        double ancho = puntos(requerido(n, "Ancho"), n, "Ancho");
        OptionalDouble alto = n.contenidoDe("Alto")
                .map(v -> OptionalDouble.of(puntos(v, n, "Alto")))
                .orElse(OptionalDouble.empty());
        Insets margen = n.contenidoDe("Margen")
                .map(v -> margenMedio(v, n))
                .orElse(Insets.CERO);
        if (tipo == Medio.Tipo.PAGINADO && alto.isEmpty()) {
            throw error(n, "Con Tipo: paginado, Medio necesita Alto");
        }
        Medio medio = new Medio(tipo, ancho, alto, margen);
        if (medio.areaUtil().ancho() <= 0 || medio.areaUtil().alto() <= 0) {
            throw error(n, "Los márgenes del medio no dejan área útil");
        }
        return medio;
    }

    private Insets margenMedio(String texto, NodoStxt n) {
        List<Double> v = ValorParser.lista(texto).stream().map(x -> {
            if (!(x instanceof Valor.Puntos p)) {
                throw error(n, "El margen del medio solo admite puntos: " + texto);
            }
            return p.pt();
        }).toList();
        Lados<Double> l = Lados.abreviado(v);
        return new Insets(l.arriba(), l.derecha(), l.abajo(), l.izquierda());
    }

    // ------------------------------------------------------------------------------------------
    // Cajas
    // ------------------------------------------------------------------------------------------

    /** Hijos {@code Caja} y {@code Texto} en orden; crea cajas anónimas si hace falta. */
    private void construirContenido(NodoStxt nodo, Box caja) {
        List<NodoStxt> contenido = nodo.hijos().stream()
                .filter(h -> h.es("Caja") || h.es("Texto"))
                .toList();
        boolean hayCajas = contenido.stream().anyMatch(h -> h.es("Caja"));
        Estilo.Layout layout = caja.estilo.layout;

        if (layout == Estilo.Layout.LINEA && hayCajas) {
            throw error(nodo, "Una caja con Layout: linea solo puede contener Texto");
        }
        boolean textoEnAnonima = hayCajas || layout == Estilo.Layout.FLEX || layout == Estilo.Layout.GRID;

        for (NodoStxt h : contenido) {
            if (h.es("Caja")) {
                caja.anadirHijo(caja(h, caja));
            } else if (textoEnAnonima) {
                // Texto mezclado con cajas, o dentro de flex/grid: caja anónima (módulo 1, §6).
                Estilo e = Estilo.heredando(caja.estilo);
                e.layout = Estilo.Layout.LINEA;
                Box anonima = new Box(null, e, true, h.linea());
                anonima.texto = h.contenido();
                caja.anadirHijo(anonima);
            } else {
                caja.texto = h.contenido();
                if (layout == Estilo.Layout.BLOQUE) {
                    // Un bloque que solo contiene texto se maqueta con el algoritmo de línea.
                    caja.estilo.layout = Estilo.Layout.LINEA;
                }
            }
        }
    }

    private Box caja(NodoStxt n, Box padre) {
        Estilo e = Estilo.heredando(padre.estilo);
        String id = null;
        for (NodoStxt p : n.hijos()) {
            String v = p.contenido();
            try {
                switch (NodoStxt.canonico(p.nombre())) {
                    case "caja", "texto" -> {
                        // se procesan en construirContenido
                    }
                    case "id" -> id = v.trim();
                    case "layout" -> e.layout = enumerado(Estilo.Layout.class, v, p);
                    case "posicion" -> e.posicion = enumerado(Estilo.Posicion.class, v, p);
                    case "desplazamiento" -> e.desplazamiento = Lados.abreviado(ValorParser.lista(v));
                    case "capa" -> e.capa = entero(v, p);
                    case "aislar" -> e.aislar = booleano(v, p);
                    case "medida" -> e.medida = enumerado(Estilo.Medida.class, v, p);
                    case "ancho" -> e.ancho = ValorParser.valor(v);
                    case "alto" -> e.alto = ValorParser.valor(v);
                    case "min ancho" -> e.minAncho = ValorParser.valor(v);
                    case "max ancho" -> e.maxAncho = ValorParser.valor(v);
                    case "min alto" -> e.minAlto = ValorParser.valor(v);
                    case "max alto" -> e.maxAlto = ValorParser.valor(v);
                    case "proporcion" -> e.proporcion = proporcion(v, p);
                    case "margen" -> e.margen = Lados.abreviado(ValorParser.lista(v));
                    case "relleno" -> e.relleno = noNegativos(Lados.abreviado(ValorParser.lista(v)), p, "Relleno", true);
                    case "borde" -> e.borde = noNegativos(Lados.abreviado(ValorParser.lista(v)), p, "Borde", false);
                    case "fondo" -> e.fondo = color(v, p);
                    case "color" -> e.color = color(v, p);
                    case "color borde" -> e.colorBorde = color(v, p);
                    case "fuente" -> e.fuente = v.trim();
                    case "tamano" -> e.tamano = positivo(v, p);
                    case "interlineado" -> e.interlineado = ValorParser.valor(v);
                    case "alinear" -> e.alinear = enumerado(Estilo.AlineacionTexto.class, v, p);
                    case "flex" -> flex(p, e);
                    case "grid" -> grid(p, e);
                    case "item" -> item(p, e);
                    case "corte antes" -> e.corteAntes = enumerado(Estilo.Corte.class, v, p);
                    case "corte despues" -> e.corteDespues = enumerado(Estilo.Corte.class, v, p);
                    case "corte dentro" -> e.corteDentroEvitar = corteDentro(v, p);
                    case "viudas" -> e.viudas = enteroPositivo(v, p);
                    case "huerfanas" -> e.huerfanas = enteroPositivo(v, p);
                    case "decoracion" -> e.decoracion = enumerado(Estilo.Decoracion.class, v, p);
                    default -> throw error(p, "Propiedad desconocida en Caja: " + p.nombre());
                }
            } catch (IllegalArgumentException ex) {
                throw error(p, ex.getMessage());
            }
        }
        validarCaja(n, e, padre);

        Box caja = new Box(id, e, false, n.linea());
        construirContenido(n, caja);
        return caja;
    }

    private void flex(NodoStxt n, Estilo e) {
        for (NodoStxt p : n.hijos()) {
            String v = p.contenido();
            switch (NodoStxt.canonico(p.nombre())) {
                case "direccion" -> e.flexDireccion = enumerado(Estilo.Direccion.class, v, p);
                case "envolver" -> e.flexEnvolver = booleano(v, p);
                case "hueco" -> hueco(v, e);
                case "eje principal" -> e.flexEjePrincipal = enumerado(Estilo.Distribucion.class, v, p);
                case "eje cruzado" -> e.flexEjeCruzado = enumerado(Estilo.Alineacion.class, v, p);
                case "lineas" -> e.flexLineas = enumerado(Estilo.Distribucion.class, v, p);
                default -> throw error(p, "Propiedad desconocida en Flex: " + p.nombre());
            }
        }
        if (e.flexEjePrincipal == Estilo.Distribucion.ESTIRAR) {
            throw error(n, "Eje principal no admite 'estirar'; usa Crecer en los ítems");
        }
    }

    private void grid(NodoStxt n, Estilo e) {
        for (NodoStxt p : n.hijos()) {
            String v = p.contenido();
            switch (NodoStxt.canonico(p.nombre())) {
                case "columnas" -> e.gridColumnas = ValorParser.lista(v);
                case "filas" -> e.gridFilas = ValorParser.lista(v);
                case "hueco" -> hueco(v, e);
                case "areas" -> {
                    e.gridAreas = v;
                    AreasGrid.parsear(v); // valida rectángulos y filas de igual longitud
                }
                case "flujo" -> e.gridFlujo = enumerado(Estilo.Direccion.class, v, p);
                case "horizontal" -> e.gridHorizontal = enumerado(Estilo.Alineacion.class, v, p);
                case "vertical" -> e.gridVertical = enumerado(Estilo.Alineacion.class, v, p);
                default -> throw error(p, "Propiedad desconocida en Grid: " + p.nombre());
            }
        }
    }

    private void item(NodoStxt n, Estilo e) {
        for (NodoStxt p : n.hijos()) {
            String v = p.contenido();
            switch (NodoStxt.canonico(p.nombre())) {
                case "crecer" -> e.itemCrecer = noNegativo(v, p);
                case "encoger" -> e.itemEncoger = noNegativo(v, p);
                case "base" -> e.itemBase = ValorParser.valor(v);
                case "eje cruzado" -> e.itemEjeCruzado = enumerado(Estilo.Alineacion.class, v, p);
                case "celda" -> e.itemCelda = v.trim();
                case "horizontal" -> e.itemHorizontal = enumerado(Estilo.Alineacion.class, v, p);
                case "vertical" -> e.itemVertical = enumerado(Estilo.Alineacion.class, v, p);
                default -> throw error(p, "Propiedad desconocida en Item: " + p.nombre());
            }
        }
    }

    /** Reglas que el template no puede expresar (ESPECIFICACION.md §6). */
    private void validarCaja(NodoStxt n, Estilo e, Box padre) {
        if (n.hijo("Flex").isPresent() && e.layout != Estilo.Layout.FLEX) {
            throw error(n, "Flex solo se aplica con Layout: flex");
        }
        if (n.hijo("Grid").isPresent() && e.layout != Estilo.Layout.GRID) {
            throw error(n, "Grid solo se aplica con Layout: grid");
        }
        Estilo.Layout layoutPadre = padre.estilo.layout;
        boolean padreFlexOGrid = layoutPadre == Estilo.Layout.FLEX || layoutPadre == Estilo.Layout.GRID;
        if (n.hijo("Item").isPresent() && (!padreFlexOGrid || e.fueraDeFlujo())) {
            throw error(n, "Item solo tiene efecto en hijos en flujo de un contenedor flex o grid");
        }
        if (e.itemCelda != null) {
            if (layoutPadre != Estilo.Layout.GRID) {
                throw error(n, "Celda solo tiene efecto dentro de un contenedor grid");
            }
            Map<String, Area> areas = padre.estilo.gridAreas == null
                    ? Map.of() : AreasGrid.parsear(padre.estilo.gridAreas);
            Celda.parsear(e.itemCelda, areas);
        }
        if (e.posicion == Estilo.Posicion.FLUJO
                && e.desplazamiento.comoLista().stream().anyMatch(v -> !v.esAuto())) {
            throw error(n, "Desplazamiento no tiene efecto con Posicion: flujo");
        }
        if (e.corteAntes == Estilo.Corte.PAGINA || e.corteDespues == Estilo.Corte.PAGINA) {
            if (e.fueraDeFlujo()) {
                throw error(n, "Los cortes forzados solo se aplican a cajas en flujo");
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Utilidades de interpretación
    // ------------------------------------------------------------------------------------------

    private static void hueco(String texto, Estilo e) {
        List<Valor> v = ValorParser.lista(texto);
        if (v.size() > 2) {
            throw new IllegalArgumentException("Hueco admite 1 o 2 valores (filas columnas): " + texto);
        }
        e.huecoFilas = v.get(0);
        e.huecoColumnas = v.size() == 2 ? v.get(1) : v.get(0);
    }

    private static <E extends Enum<E>> E enumerado(Class<E> tipo, String texto, NodoStxt n) {
        try {
            return Enum.valueOf(tipo, texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw error(n, "Valor no permitido en " + n.nombre() + ": " + texto.trim());
        }
    }

    private static boolean corteDentro(String texto, NodoStxt n) {
        Estilo.Corte c = enumerado(Estilo.Corte.class, texto, n);
        if (c == Estilo.Corte.PAGINA) {
            throw error(n, "Corte dentro solo admite auto o evitar");
        }
        return c == Estilo.Corte.EVITAR;
    }

    private static boolean booleano(String texto, NodoStxt n) {
        return switch (texto.trim().toLowerCase(Locale.ROOT)) {
            case "true", "si", "sí" -> true;
            case "false", "no" -> false;
            default -> throw error(n, "Se esperaba true o false: " + texto.trim());
        };
    }

    private static int entero(String texto, NodoStxt n) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            throw error(n, "Se esperaba un entero: " + texto.trim());
        }
    }

    private static int enteroPositivo(String texto, NodoStxt n) {
        int v = entero(texto, n);
        if (v < 1) {
            throw error(n, n.nombre() + " debe ser al menos 1");
        }
        return v;
    }

    private static double puntos(String texto, NodoStxt n, String propiedad) {
        Valor v = ValorParser.valor(texto);
        if (!(v instanceof Valor.Puntos p) || p.pt() <= 0) {
            throw error(n, propiedad + " del medio debe ser un número de puntos positivo: " + texto);
        }
        return p.pt();
    }

    private static double positivo(String texto, NodoStxt n) {
        double v = noNegativo(texto, n);
        if (v == 0) {
            throw error(n, n.nombre() + " debe ser mayor que 0");
        }
        return v;
    }

    private static double noNegativo(String texto, NodoStxt n) {
        Valor v = ValorParser.valor(texto);
        if (!(v instanceof Valor.Puntos p) || p.pt() < 0) {
            throw error(n, n.nombre() + " debe ser un número no negativo: " + texto.trim());
        }
        return p.pt();
    }

    private static Lados<Valor> noNegativos(Lados<Valor> lados, NodoStxt n, String propiedad, boolean admitePorcentaje) {
        for (Valor v : lados.comoLista()) {
            boolean valido = (v instanceof Valor.Puntos p && p.pt() >= 0)
                    || (admitePorcentaje && v instanceof Valor.Porcentaje q && q.porcentaje() >= 0);
            if (!valido) {
                throw error(n, propiedad + (admitePorcentaje
                        ? " solo admite puntos o porcentajes no negativos"
                        : " solo admite puntos no negativos"));
            }
        }
        return lados;
    }

    private static Double proporcion(String texto, NodoStxt n) {
        String t = texto.trim();
        try {
            if (t.contains("/")) {
                String[] p = t.split("/");
                double r = Double.parseDouble(p[0].trim()) / Double.parseDouble(p[1].trim());
                if (r > 0 && Double.isFinite(r)) {
                    return r;
                }
            } else {
                double r = Double.parseDouble(t);
                if (r > 0) {
                    return r;
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            // se informa abajo
        }
        throw error(n, "Proporcion espera 'ancho/alto' o un número positivo: " + t);
    }

    private static String color(String texto, NodoStxt n) {
        String t = texto.trim();
        if (!COLOR.matcher(t).matches()) {
            throw error(n, "Color no válido (usa #RGB o #RRGGBB): " + t);
        }
        return t;
    }

    private static String requerido(NodoStxt n, String hijo) {
        return n.contenidoDe(hijo).orElseThrow(() -> error(n, n.nombre() + " necesita " + hijo));
    }

    private static ModeloInvalidoException error(NodoStxt n, String mensaje) {
        return new ModeloInvalidoException(n.linea(), mensaje);
    }
}
