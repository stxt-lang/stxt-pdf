package dev.stxt.pdf.layout.image;

/**
 * Tamaño natural de una imagen en puntos (referencia/componentes/image.md): el de sus píxeles, a 96 por
 * pulgada. Es el de su caja de contenido si no se declara otro.
 */
public record NaturalSize(double width, double height) {

    /** Proporción ancho / alto. */
    public double ratio() {
        return width / height;
    }
}
