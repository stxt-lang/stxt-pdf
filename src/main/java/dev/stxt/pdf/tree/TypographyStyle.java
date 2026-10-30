package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Inline;
import dev.stxt.pdf.model.NumberStyle;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.TextWrap;
import dev.stxt.pdf.model.Transform;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.model.WhiteSpace;

import java.util.Locale;

/**
 * El grupo {@code Typography} (referencia/comun/tipografia.md): la fuente, el tamaño, el interlineado, la alineación,
 * el color, las mayúsculas, el interletraje y el espacio en blanco del texto, la numeración de los encabezados, el
 * idioma y las viudas y huérfanas. Es el único grupo que se <strong>hereda</strong>: cada caja empieza con una copia
 * del de su padre ({@link Style#inheriting}).
 */
public final class TypographyStyle extends StyleGroup {

    /** Familia; {@code null} = sin declarar, que es un error en una caja con texto (referencia/comun/errores.md). */
    private String font;
    /** {@code Code font}: la familia del código, en línea o en un bloque; {@code null} si no se declara. */
    private String codeFont;
    private FontWeight fontWeight = FontWeight.NORMAL;
    private FontStyle fontStyle = FontStyle.NORMAL;
    /** {@code NaN} = sin declarar, que es un error en una caja con texto (referencia/comun/errores.md). */
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
    /**
     * {@code Ligatures}: si el texto se mide y se pinta con las ligaduras de la fuente
     * (referencia/comun/formato-del-texto.md). Se hereda.
     */
    private boolean ligatures;
    private WhiteSpace whiteSpace = WhiteSpace.NORMAL;
    /** Columnas de una tabulación en el texto preformateado ({@code Tab size}). */
    private int tabSize = 4;
    /**
     * {@code Numbered}: si los encabezados se numeran (referencia/componentes/heading.md). Se hereda: la lee el
     * encabezado.
     */
    private boolean numbered;
    /** {@code Number format}: cómo se escribe el número del nivel del encabezado. */
    private NumberStyle numberStyle = NumberStyle.DECIMAL;
    /** {@code Number label}: lo que se escribe delante del título, con {@code {number}}; {@code null} = el número. */
    private String numberLabel;
    /**
     * Idioma y configuración regional del texto: el de {@code Language} de {@code Document}
     * (referencia/componentes/document.md) en la raíz y, si una caja declara {@code Language} en {@code Typography}
     * (referencia/comun/tipografia.md), el suyo. Se hereda.
     */
    private Locale locale = Locale.ROOT;
    /** {@code Color} del texto; también el de los bordes y la línea de un {@code Columns} que no declaran el suyo. */
    private String color = "#000000";
    private int widows = 2;
    private int orphans = 2;

    TypographyStyle() {
    }

    /** Una copia abierta del de {@code parent}: lo que hereda una caja nueva. */
    TypographyStyle(TypographyStyle parent) {
        font = parent.font;
        codeFont = parent.codeFont;
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
        numberStyle = parent.numberStyle;
        numberLabel = parent.numberLabel;
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

    public String codeFont() {
        return codeFont;
    }

    void setCodeFont(String v) {
        writable();
        codeFont = v;
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

    public NumberStyle numberStyle() {
        return numberStyle;
    }

    void setNumberStyle(NumberStyle v) {
        writable();
        numberStyle = v;
    }

    /** La plantilla de {@code Number label}, o {@code null} si no se declara. */
    public String numberLabel() {
        return numberLabel;
    }

    void setNumberLabel(String v) {
        writable();
        numberLabel = v;
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
     * Fuente del texto. Solo en cajas con texto, que siempre tienen {@code Font} (referencia/comun/errores.md). Las
     * ligaduras se desactivan con {@code Letter spacing} distinto de 0, como en CSS: separar las letras y unir dos en
     * un glifo se contradicen.
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
