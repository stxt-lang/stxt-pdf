package dev.stxt.boxes.layout;

/**
 * Acumulador de márgenes verticales pendientes de colapsar (módulo 2, ESPECIFICACION.md §5.3).
 * Cada margen encontrado se añade; se resuelve cuando aparece algo que corta el colapso
 * (borde, relleno, contenido o el final de un contexto).
 */
public final class MarginStrut {

    private double positive;
    private double negative;
    private boolean empty = true;

    public void add(double margin) {
        if (margin > 0) {
            positive = Math.max(positive, margin);
        } else {
            negative = Math.min(negative, margin);
        }
        empty = false;
    }

    /** Mayor positivo más el más negativo. */
    public double resolve() {
        return positive + negative;
    }

    public double resolveAndReset() {
        double r = resolve();
        reset();
        return r;
    }

    public void reset() {
        positive = 0;
        negative = 0;
        empty = true;
    }

    public boolean isEmpty() {
        return empty;
    }

    /** Colapsa una serie de márgenes adyacentes. */
    public static double collapse(double... margins) {
        MarginStrut s = new MarginStrut();
        for (double m : margins) {
            s.add(m);
        }
        return s.resolve();
    }
}
