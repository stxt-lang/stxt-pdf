package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.FontFace;
import dev.stxt.pdf.model.FontStyle;
import dev.stxt.pdf.model.FontWeight;
import dev.stxt.pdf.model.Markup;
import dev.stxt.pdf.model.NumberFormat;
import dev.stxt.pdf.model.TextAlign;
import dev.stxt.pdf.model.LineBreaking;
import dev.stxt.pdf.model.TextCase;
import dev.stxt.pdf.model.Value;
import dev.stxt.pdf.model.Preformatted;

import java.util.Locale;

/**
 * The {@code Typography} group (reference/common/typography.md): the font, the size, the line height, the alignment,
 * the color, the case, the letter spacing and the white space of the text, the numbering of the headings, the language
 * and the widows and orphans. It is the only group that is <strong>inherited</strong>: each box starts with a copy of
 * its parent's ({@link Style#inheriting}).
 */
public final class TypographyStyle extends StyleGroup {

    /** Family; {@code null} = not declared, which is an error in a box with text (reference/common/errors.md). */
    private String font;
    /** {@code Code font}: the family of code, inline or in a block; {@code null} if it is not declared. */
    private String codeFont;
    private FontWeight fontWeight = FontWeight.NORMAL;
    private FontStyle fontStyle = FontStyle.NORMAL;
    /** {@code NaN} = not declared, which is an error in a box with text (reference/common/errors.md). */
    private double fontSize = Double.NaN;
    /** {@code null} = 120% of the size. A number is points; a percentage, relative to the size. */
    private Value lineHeight;
    private TextAlign textAlign = TextAlign.LEFT;
    private LineBreaking lineBreaking = LineBreaking.SIMPLE;
    private Markup markup = Markup.NONE;
    private TextCase textCase = TextCase.NONE;
    /** Spacing between letters, in points; it can be negative. */
    private double letterSpacing;
    private boolean smallCaps;
    /**
     * {@code Ligatures}: whether the text is measured and painted with the ligatures of the font
     * (reference/common/text-formatting.md). Inherited.
     */
    private boolean ligatures;
    private Preformatted preformatted = Preformatted.FALSE;
    /** Columns of a tab in preformatted text ({@code Tab size}). */
    private int tabSize = 4;
    /**
     * {@code Numbered}: whether the headings are numbered (reference/components/heading.md). Inherited: the heading
     * reads it.
     */
    private boolean numbered;
    /** {@code Number format}: how the number of the heading level is written. */
    private NumberFormat numberFormat = NumberFormat.DECIMAL;
    /** {@code Number label}: what is written before the title, with {@code {number}}; {@code null} = the number. */
    private String numberLabel;
    /**
     * Language and locale of the text: the one of {@code Language} of {@code Document}
     * (reference/components/document.md) at the root and, if a box declares {@code Language} in {@code Typography}
     * (reference/common/typography.md), its own. Inherited.
     */
    private Locale locale = Locale.ROOT;
    /**
     * {@code Color} of the text; also that of the borders and of the rule of a {@code Columns} that do not declare
     * their own.
     */
    private String color = "#000000";
    private int widows = 2;
    private int orphans = 2;

    TypographyStyle() {
    }

    /** An open copy of the one of {@code parent}: what a new box inherits. */
    TypographyStyle(TypographyStyle parent) {
        font = parent.font;
        codeFont = parent.codeFont;
        fontWeight = parent.fontWeight;
        fontStyle = parent.fontStyle;
        fontSize = parent.fontSize;
        lineHeight = parent.lineHeight;
        textAlign = parent.textAlign;
        lineBreaking = parent.lineBreaking;
        markup = parent.markup;
        textCase = parent.textCase;
        letterSpacing = parent.letterSpacing;
        smallCaps = parent.smallCaps;
        ligatures = parent.ligatures;
        preformatted = parent.preformatted;
        tabSize = parent.tabSize;
        numbered = parent.numbered;
        numberFormat = parent.numberFormat;
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

    public LineBreaking lineBreaking() {
        return lineBreaking;
    }

    void setLineBreaking(LineBreaking v) {
        writable();
        lineBreaking = v;
    }

    public Markup markup() {
        return markup;
    }

    void setMarkup(Markup v) {
        writable();
        markup = v;
    }

    public TextCase textCase() {
        return textCase;
    }

    void setTextCase(TextCase v) {
        writable();
        textCase = v;
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

    public Preformatted preformatted() {
        return preformatted;
    }

    void setPreformatted(Preformatted v) {
        writable();
        preformatted = v;
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

    public NumberFormat numberFormat() {
        return numberFormat;
    }

    void setNumberFormat(NumberFormat v) {
        writable();
        numberFormat = v;
    }

    /** The template of {@code Number label}, or {@code null} if it is not declared. */
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
     * Font of the text. Only in boxes with text, which always have {@code Font} (reference/common/errors.md). Ligatures
     * are turned off with a {@code Letter spacing} other than 0, as in CSS: spacing the letters and joining two into
     * one glyph contradict each other.
     */
    public FontFace fontFace() {
        return new FontFace(font, fontWeight, fontStyle, ligatures && letterSpacing == 0);
    }

    /** Used line height, in points. */
    public double usedLineHeight() {
        if (lineHeight == null) {
            return fontSize * 1.2;
        }
        return lineHeight.resolve(fontSize).orElse(fontSize * 1.2);
    }
}
