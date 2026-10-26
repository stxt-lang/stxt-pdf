package dev.stxt.boxes.arbol;

import dev.stxt.boxes.modelo.Lados;
import dev.stxt.boxes.modelo.Valor;

import java.util.List;

/**
 * Propiedades especificadas de una caja, con los valores por defecto de ESPECIFICACION.md §4.
 *
 * <p>No hay cascada: cada caja declara sus propiedades. Solo se <strong>heredan</strong> las
 * marcadas como heredables (texto y fragmentación), igual que en CSS; ver {@link #heredando}.</p>
 */
public final class Estilo {

    public enum Layout { BLOQUE, LINEA, FLEX, GRID }

    public enum Posicion { FLUJO, RELATIVA, ABSOLUTA, FIJA }

    public enum Medida { BORDE, CONTENIDO }

    public enum AlineacionTexto { IZQUIERDA, DERECHA, CENTRO, JUSTIFICADO }

    public enum Direccion { FILA, COLUMNA }

    /** Reparto en un eje: {@code Eje principal} y {@code Lineas}. */
    public enum Distribucion { INICIO, FIN, CENTRO, ENTRE, ALREDEDOR, UNIFORME, ESTIRAR }

    /** Alineación de un ítem dentro de su línea o celda. */
    public enum Alineacion { INICIO, FIN, CENTRO, ESTIRAR }

    public enum Corte { AUTO, PAGINA, EVITAR }

    public enum Decoracion { CORTAR, REPETIR }

    // --- Caja --------------------------------------------------------------------------------
    public Layout layout = Layout.BLOQUE;
    public Posicion posicion = Posicion.FLUJO;
    public Lados<Valor> desplazamiento = Lados.todos(Valor.AUTO);
    /** {@code null} = auto. */
    public Integer capa;
    public boolean aislar;
    public Medida medida = Medida.BORDE;

    // --- Dimensiones ---------------------------------------------------------------------------
    public Valor ancho = Valor.AUTO;
    public Valor alto = Valor.AUTO;
    public Valor minAncho = Valor.AUTO;
    public Valor minAlto = Valor.AUTO;
    /** {@code null} = sin máximo. */
    public Valor maxAncho;
    /** {@code null} = sin máximo. */
    public Valor maxAlto;
    /** Ancho / alto; {@code null} = sin proporción. */
    public Double proporcion;

    // --- Modelo de caja ------------------------------------------------------------------------
    public Lados<Valor> margen = Lados.todos(Valor.pt(0));
    public Lados<Valor> relleno = Lados.todos(Valor.pt(0));
    public Lados<Valor> borde = Lados.todos(Valor.pt(0));

    // --- Pintado (no afecta al layout) ---------------------------------------------------------
    /** {@code null} = transparente. */
    public String fondo;
    /** Heredable. */
    public String color = "#000000";
    /** {@code null} = igual que {@link #color}. */
    public String colorBorde;

    // --- Texto (heredables) ---------------------------------------------------------------------
    public String fuente = "Helvetica";
    public double tamano = 10;
    /** {@code null} = 120 % del tamaño. Un número son puntos; un porcentaje, relativo al tamaño. */
    public Valor interlineado;
    public AlineacionTexto alinear = AlineacionTexto.IZQUIERDA;

    // --- Flex ------------------------------------------------------------------------------------
    public Direccion flexDireccion = Direccion.FILA;
    public boolean flexEnvolver;
    public Distribucion flexEjePrincipal = Distribucion.INICIO;
    public Alineacion flexEjeCruzado = Alineacion.ESTIRAR;
    public Distribucion flexLineas = Distribucion.ESTIRAR;

    // --- Hueco (flex y grid) -------------------------------------------------------------------
    public Valor huecoFilas = Valor.pt(0);
    public Valor huecoColumnas = Valor.pt(0);

    // --- Grid ------------------------------------------------------------------------------------
    public List<Valor> gridColumnas = List.of();
    public List<Valor> gridFilas = List.of();
    /** Bloque de texto de {@code Areas}, o {@code null}. */
    public String gridAreas;
    public Direccion gridFlujo = Direccion.FILA;
    public Alineacion gridHorizontal = Alineacion.ESTIRAR;
    public Alineacion gridVertical = Alineacion.ESTIRAR;

    // --- Item (como hijo de flex o grid) -----------------------------------------------------
    public double itemCrecer = 0;
    public double itemEncoger = 1;
    public Valor itemBase = Valor.AUTO;
    /** {@code null} = la del contenedor. */
    public Alineacion itemEjeCruzado;
    /** Texto de {@code Celda}, o {@code null} = colocación automática. */
    public String itemCelda;
    /** {@code null} = la del contenedor. */
    public Alineacion itemHorizontal;
    /** {@code null} = la del contenedor. */
    public Alineacion itemVertical;

    // --- Fragmentación ---------------------------------------------------------------------------
    public Corte corteAntes = Corte.AUTO;
    public Corte corteDespues = Corte.AUTO;
    public boolean corteDentroEvitar;
    /** Heredable. */
    public int viudas = 2;
    /** Heredable. */
    public int huerfanas = 2;
    public Decoracion decoracion = Decoracion.CORTAR;

    /** Estilo nuevo con los valores por defecto y las propiedades heredables del padre. */
    public static Estilo heredando(Estilo padre) {
        Estilo e = new Estilo();
        if (padre != null) {
            e.color = padre.color;
            e.fuente = padre.fuente;
            e.tamano = padre.tamano;
            e.interlineado = padre.interlineado;
            e.alinear = padre.alinear;
            e.viudas = padre.viudas;
            e.huerfanas = padre.huerfanas;
        }
        return e;
    }

    /** Altura de línea usada, en puntos. */
    public double alturaLinea() {
        if (interlineado == null) {
            return tamano * 1.2;
        }
        return interlineado.resolver(tamano).orElse(tamano * 1.2);
    }

    public boolean estaPosicionada() {
        return posicion != Posicion.FLUJO;
    }

    public boolean fueraDeFlujo() {
        return posicion == Posicion.ABSOLUTA || posicion == Posicion.FIJA;
    }
}
