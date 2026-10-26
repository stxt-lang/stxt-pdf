package dev.stxt.boxes.layout.inline;

/** Mide el ancho de un texto en puntos (módulo 2). */
public interface TextMeasurer {

    double width(String text, String font, double fontSize);
}
