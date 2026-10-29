package dev.stxt.pdf.layout;

/**
 * Márgenes verticales adyacentes pendientes de colapsar (referencia/componentes/block.md). Guarda por
 * separado el mayor positivo y el más negativo, porque el colapso de varios márgenes es su suma
 * y no se puede calcular a partir de resultados parciales: {@code 20} y {@code −5} dan 15, pero
 * añadir {@code −10} da 10, no 5.
 *
 * <p>Es inmutable: forma parte de {@link AdjoiningMargins}, que viaja en {@link LayoutResult}.</p>
 *
 * @param positive el mayor margen positivo, o 0
 * @param negative el margen más negativo, o 0
 */
public record MarginStrut(double positive, double negative) {

    public static final MarginStrut EMPTY = new MarginStrut(0, 0);

    public MarginStrut add(double margin) {
        return new MarginStrut(Math.max(positive, margin), Math.min(negative, margin));
    }

    public MarginStrut add(MarginStrut other) {
        return new MarginStrut(Math.max(positive, other.positive), Math.min(negative, other.negative));
    }

    /** Mayor positivo más el más negativo. */
    public double resolve() {
        return positive + negative;
    }

    /** Colapsa una serie de márgenes adyacentes. */
    public static double collapse(double... margins) {
        MarginStrut s = EMPTY;
        for (double m : margins) {
            s = s.add(m);
        }
        return s.resolve();
    }
}
