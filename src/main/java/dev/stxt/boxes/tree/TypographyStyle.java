package dev.stxt.boxes.tree;

import dev.stxt.boxes.model.FontFace;
import dev.stxt.boxes.model.FontStyle;
import dev.stxt.boxes.model.FontWeight;
import dev.stxt.boxes.model.Inline;
import dev.stxt.boxes.model.TextAlign;
import dev.stxt.boxes.model.TextWrap;
import dev.stxt.boxes.model.Transform;
import dev.stxt.boxes.model.Value;
import dev.stxt.boxes.model.WhiteSpace;

import java.util.Locale;

/**
 * El grupo {@code Typography} (ESPECIFICACION.md §4.4): la fuente, el tamaño, el interlineado, la alineación, el color,
 * las mayúsculas, el interletraje y el espacio en blanco del texto, la numeración de los encabezados, el idioma y las
 * viudas y huérfanas. Es el único grupo que se <strong>hereda</strong>: cada caja empieza con una copia del de su
 * padre ({@link Style#inheriting}).
 */
public final class TypographyStyle extends StyleGroup {

    /** Familia; {@code null} = sin declarar, que es un error en una caja con texto (§6). */
    private String font;
    private FontWeight fontWeight = FontWeight.NORMAL;
    private FontStyle fontStyle = FontStyle.NORMAL;
    /** {@code NaN} = sin declarar, que es un error en una caja con texto (§6). */
    private double fontSize = Double.NaN;
    /** {@code null} = 120 % del tamaño. Un número son puntos; un porcentaje, relativo al tamaño. */
    private Value lineHeight;
    private TextAlign textAlign = TextAlign.LEFT;
    private TextWrap textWrap = TextWrap.AUTO;
    private Inline inline = Inline.NONE;
    private Transform transform = Transform.NONE;
    /** Separación entre letras, en puntos; puede ser negativa. */
    private double letterSpacing;
    private boolean smallCaps;
    /** {@code Ligatures}: si el texto se mide y se pinta con las ligaduras de la fuente (§4.4). Se hereda. */
    private boolean ligatures;
    private WhiteSpace whiteSpace = WhiteSpace.NORMAL;
    /** Columnas de una tabulación en el texto preformateado ({@code Tab size}). */
    private int tabSize = 4;
    /** {@code Numbered}: si los encabezados se numeran (§5.13). Se hereda: la lee el encabezado. */
    private boolean numbered;
    /**
     * Idioma y configuración regional del texto: el de {@code Language} de {@code Document} (§4.1) en
     * la raíz y, si una caja declara {@code Language} en {@code Typography} (§4.4), el suyo. Se hereda.
     */
    private Locale locale = Locale.ROOT;
    /** {@code Color} del texto; también el de los bordes y la línea de un {@code Multicol} que no declaran el suyo. */
    private String color = "#000000";
    private int widows = 2;
    private int orphans = 2;

    TypographyStyle() {
    }

    /** Una copia abierta del de {@code parent}: lo que hereda una caja nueva. */
    TypographyStyle(TypographyStyle parent) {
        font = parent.font;
        fontWeight = parent.fontWeight;
        fontStyle = parent.fontStyle;
        fontSize = parent.fontSize;
        lineHeight = parent.lineHeight;
        textAlign = parent.textAlign;
        textWrap = parent.textWrap;
        inline = parent.inline;
        transform = parent.transform;
        letterSpacing = parent.letterSpacing;
        smallCaps = parent.smallCaps;
        ligatures = parent.ligatures;
        whiteSpace = parent.whiteSpace;
        tabSize = parent.tabSize;
        numbered = parent.numbered;
        locale = parent.locale;
        color = parent.color;
        widows = parent.widows;
        orphans = parent.orphans;
    }

    public String font() {
        return font;
    }

    void setFont(String v) {
        writable();
        font = v;
    }

    public FontWeight fontWeight() {
        return fontWeight;
    }

    void setFontWeight(FontWeight v) {
        writable();
        fontWeight = v;
    }

    public FontStyle fontStyle() {
        return fontStyle;
    }

    void setFontStyle(FontStyle v) {
        writable();
        fontStyle = v;
    }

    public double fontSize() {
        return fontSize;
    }

    void setFontSize(double v) {
        writable();
        fontSize = v;
    }

    public Value lineHeight() {
        return lineHeight;
    }

    void setLineHeight(Value v) {
        writable();
        lineHeight = v;
    }

    public TextAlign textAlign() {
        return textAlign;
    }

    void setTextAlign(TextAlign v) {
        writable();
        textAlign = v;
    }

    public TextWrap textWrap() {
        return textWrap;
    }

    void setTextWrap(TextWrap v) {
        writable();
        textWrap = v;
    }

    public Inline inline() {
        return inline;
    }

    void setInline(Inline v) {
        writable();
        inline = v;
    }

    public Transform transform() {
        return transform;
    }

    void setTransform(Transform v) {
        writable();
        transform = v;
    }

    public double letterSpacing() {
        return letterSpacing;
    }

    void setLetterSpacing(double v) {
        writable();
        letterSpacing = v;
    }

    public boolean smallCaps() {
        return smallCaps;
    }

    void setSmallCaps(boolean v) {
        writable();
        smallCaps = v;
    }

    public boolean ligatures() {
        return ligatures;
    }

    void setLigatures(boolean v) {
        writable();
        ligatures = v;
    }

    public WhiteSpace whiteSpace() {
        return whiteSpace;
    }

    void setWhiteSpace(WhiteSpace v) {
        writable();
        whiteSpace = v;
    }

    public int tabSize() {
        return tabSize;
    }

    void setTabSize(int v) {
        writable();
        tabSize = v;
    }

    public boolean numbered() {
        return numbered;
    }

    void setNumbered(boolean v) {
        writable();
        numbered = v;
    }

    public Locale locale() {
        return locale;
    }

    void setLocale(Locale v) {
        writable();
        locale = v;
    }

    public String color() {
        return color;
    }

    void setColor(String v) {
        writable();
        color = v;
    }

    public int widows() {
        return widows;
    }

    void setWidows(int v) {
        writable();
        widows = v;
    }

    public int orphans() {
        return orphans;
    }

    void setOrphans(int v) {
        writable();
        orphans = v;
    }

    /**
     * Fuente del texto. Solo en cajas con texto, que siempre tienen {@code Font} (§6). Las ligaduras se
     * desactivan con {@code Letter spacing} distinto de 0, como en CSS: separar las letras y unir dos en un
     * glifo se contradicen.
     */
    public FontFace fontFace() {
        return new FontFace(font, fontWeight, fontStyle, ligatures && letterSpacing == 0);
    }

    /** Altura de línea usada, en puntos. */
    public double usedLineHeight() {
        if (lineHeight == null) {
            return fontSize * 1.2;
        }
        return lineHeight.resolve(fontSize).orElse(fontSize * 1.2);
    }
}
