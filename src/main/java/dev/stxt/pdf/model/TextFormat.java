package dev.stxt.pdf.model;

/**
 * Format of a run of text (reference/common/text-formatting.md): what changes within a line. Bold
 * and italic add to those of the box (a bold run has weight 700, or that of the box if it is already 700 or more,
 * {@link FontWeight#bolder()}); the size, the color and {@code Line height} are always those of the box, and so is the
 * family except in code ({@code family}).
 *
 * <p>{@code smallCaps} marks the lowercase letters painted as small caps (reference/common/text-formatting.md): the
 * text of the run is already in upper case and is measured and painted at {@link #SMALL_CAPS_SCALE} of the size. The
 * layout sets it when it applies {@code Small caps}; the document does not write it.</p>
 *
 * <p>{@code code} marks inline code (reference/common/text-formatting.md), which is written with the family of
 * {@code Code font}: when the box is built, which knows its {@code Code font}, it receives that family in
 * {@code family}. {@code family} is the family of the run when it is not that of the box; {@code null} = that of the
 * box. It keeps the weight and style of the box and those of the marks of the run.</p>
 *
 * <p>{@code link} is the target of the link of the run ({@link Links}, reference/common/links.md), or {@code null} if
 * it has none. It does not change how the text is measured or painted: the underline of a Markdown
 * link is the {@code underline} of its run.</p>
 *
 * <p>{@code footnote} is the {@code Id} of the {@code Footnote} the run calls ({@code [^id]},
 * reference/components/footnote.md), or {@code null}; its text is the number of the note. {@code label} marks the
 * number that opens the note itself. Both are superscripts ({@link #superscript()}): measured and painted at
 * {@link #SUPERSCRIPT_SCALE} of the size and raised {@link #SUPERSCRIPT_RISE} of it, without changing the height of
 * the line.</p>
 *
 * <p>{@code style} is that of a styled run, {@code [text]{name}} ({@link RunStyle}), or {@code null}: its color, its
 * weight, its style, its letter spacing, its case and its small caps replace those of the box, and its family (or that
 * of its code) is in {@code family}. The marks of the run (bold, italic) add to the style as they add to the box,
 * wherever they are written.</p>
 */
public record TextFormat(boolean bold, boolean italic, boolean underline, boolean strike, String link,
                         boolean smallCaps, boolean code, String family, String footnote, boolean label,
                         RunStyle style) {

    /** Size of the small caps relative to that of the box. */
    public static final double SMALL_CAPS_SCALE = 0.8;

    /** Size of a superscript (the call and the label of a footnote) relative to that of the box. */
    public static final double SUPERSCRIPT_SCALE = 0.7;

    /** How much a superscript is raised above the baseline, relative to the size of the box. */
    public static final double SUPERSCRIPT_RISE = 0.33;

    /** No format: the text as the box declares it. */
    public static final TextFormat PLAIN = new TextFormat(false, false, false, false);

    /** A format without link or small caps. */
    public TextFormat(boolean bold, boolean italic, boolean underline, boolean strike) {
        this(bold, italic, underline, strike, null);
    }

    /** A format without small caps. */
    public TextFormat(boolean bold, boolean italic, boolean underline, boolean strike, String link) {
        this(bold, italic, underline, strike, link, false);
    }

    /** A format that is not code, with the family of the box. */
    public TextFormat(boolean bold, boolean italic, boolean underline, boolean strike, String link,
                      boolean smallCaps) {
        this(bold, italic, underline, strike, link, smallCaps, false, null);
    }

    /** A format that is not a footnote call nor a footnote label. */
    public TextFormat(boolean bold, boolean italic, boolean underline, boolean strike, String link,
                      boolean smallCaps, boolean code, String family) {
        this(bold, italic, underline, strike, link, smallCaps, code, family, null, false);
    }

    /** A format without the style of a styled run. */
    public TextFormat(boolean bold, boolean italic, boolean underline, boolean strike, String link,
                      boolean smallCaps, boolean code, String family, String footnote, boolean label) {
        this(bold, italic, underline, strike, link, smallCaps, code, family, footnote, label, null);
    }

    /**
     * The font of the run, derived from that of the box: with the family, the weight and the style of its
     * {@link #style()}, if it declares them, and its marks on top. A run with letter spacing has no ligatures, like a
     * box (reference/common/text-formatting.md).
     */
    public FontFace face(FontFace base) {
        if (!bold && !italic && family == null && style == null) {
            return base;
        }
        FontWeight weight = style != null && style.weight() != null ? style.weight() : base.weight();
        FontStyle fontStyle = style != null && style.style() != null ? style.style() : base.style();
        boolean ligatures = base.ligatures() && (style == null || style.letterSpacing() == null
                || style.letterSpacing() == 0);
        return new FontFace(family != null ? family : base.family(), bold ? weight.bolder() : weight,
                italic ? FontStyle.ITALIC : fontStyle, ligatures, base.kerning());
    }

    /** The color of the text of the run: that of its {@link #style()} or, if it does not declare one, {@code box}. */
    public String color(String box) {
        return style != null && style.color() != null ? style.color() : box;
    }

    /**
     * The letter spacing of the run, in points: that of its {@link #style()} or, if it does not declare one,
     * {@code box}.
     */
    public double letterSpacing(double box) {
        return style != null && style.letterSpacing() != null ? style.letterSpacing() : box;
    }

    /** The case of the run: that of its {@link #style()} or, if it does not declare one, {@code box}. */
    public TextCase textCase(TextCase box) {
        return style != null && style.textCase() != null ? style.textCase() : box;
    }

    /**
     * Whether the run is painted in small caps ({@code Small caps}): its {@link #style()} or, if it does not declare
     * it, {@code box}. It is not {@link #smallCaps()}, which marks the letters the layout already turned into them.
     */
    public boolean smallCaps(boolean box) {
        return style != null && style.smallCaps() != null ? style.smallCaps() : box;
    }

    /** The same format with those marks: bold, italic, underline and strikethrough. */
    public TextFormat withMarks(boolean b, boolean i, boolean u, boolean s) {
        return new TextFormat(b, i, u, s, link, smallCaps, code, family, footnote, label, style);
    }

    /** The same format with the style of a styled run ({@code [text]{name}}). */
    public TextFormat withStyle(RunStyle runStyle) {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, code, family, footnote, label,
                runStyle);
    }

    /** Whether it has underline or strikeout, which are painted apart from the text. */
    public boolean decorated() {
        return underline || strike;
    }

    /** The same format with that link. */
    public TextFormat withLink(String destination) {
        return new TextFormat(bold, italic, underline, strike, destination, smallCaps, code, family, footnote, label, style);
    }

    /** The same format with small caps turned on or off. */
    public TextFormat withSmallCaps(boolean small) {
        return small == smallCaps ? this
                : new TextFormat(bold, italic, underline, strike, link, small, code, family, footnote, label, style);
    }

    /** The same format as inline code, still without its family. */
    public TextFormat asCode() {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, true, family, footnote, label, style);
    }

    /** The same format with that font family. */
    public TextFormat withFamily(String fontFamily) {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, code, fontFamily, footnote, label, style);
    }

    /** The same format as the call of the footnote with that {@code Id}: a superscript. */
    public TextFormat withFootnote(String id) {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, code, family, id, false, style);
    }

    /** The same format as the label that opens a footnote: a superscript. */
    public TextFormat asLabel() {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, code, family, null, true, style);
    }

    /** Whether the run is a superscript: the call or the label of a footnote. */
    public boolean superscript() {
        return footnote != null || label;
    }

    /** The factor applied to the size of the box to paint the run. */
    public double scale() {
        if (superscript()) {
            return SUPERSCRIPT_SCALE;
        }
        return smallCaps ? SMALL_CAPS_SCALE : 1;
    }

    /** How much the run is raised above the baseline of the line, as a factor of the size of the box. */
    public double rise() {
        return superscript() ? SUPERSCRIPT_RISE : 0;
    }
}
