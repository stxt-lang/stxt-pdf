package dev.stxt.boxes.layout;

/**
 * Acumulador de márgenes verticales pendientes de colapsar (módulo 2, ESPECIFICACION.md §5.3).
 * Cada margen encontrado se añade; se resuelve cuando aparece algo que corta el colapso
 * (borde, relleno, contenido o el final de un contexto).
 */
public final class MarginStrut {

    private double positivo;
    private double negativo;
    private boolean vacio = true;

    public void add(double margen) {
        if (margen > 0) {
            positivo = Math.max(positivo, margen);
        } else {
            negativo = Math.min(negativo, margen);
        }
        vacio = false;
    }

    /** Mayor positivo más el más negativo. */
    public double resolve() {
        return positivo + negativo;
    }

    public double resolveAndReset() {
        double r = resolve();
        reset();
        return r;
    }

    public void reset() {
        positivo = 0;
        negativo = 0;
        vacio = true;
    }

    public boolean vacio() {
        return vacio;
    }

    /** Colapsa una serie de márgenes adyacentes. */
    public static double colapsar(double... margenes) {
        MarginStrut s = new MarginStrut();
        for (double m : margenes) {
            s.add(m);
        }
        return s.resolve();
    }
}
