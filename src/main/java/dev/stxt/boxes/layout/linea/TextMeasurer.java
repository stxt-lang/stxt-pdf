package dev.stxt.boxes.layout.linea;

/** Mide el ancho de un texto en puntos (módulo 2). */
public interface TextMeasurer {

    double ancho(String texto, String fuente, double tamano);
}
