package dev.stxt.pdf.model;

/**
 * Format of a run of text (reference/common/text-formatting.md): what changes within a line. Bold
 * and italic add to those of the box (a bold run has weight 700, or that of the box if it is already 700 or more,
 * {@link FontWeight#bolder()}); the size and {@code Line height} are always those of the box, and so are the color and
 * the family unless a styled run ({@code style}) or code ({@code family}) changes them. A superscript or a subscript
 * is painted smaller and off the baseline, within the line of the box.
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
 * of its code) is in {@code family}. Its {@code Position} makes it a superscript ({@link #superscript()}) or a
 * subscript ({@link #subscript()}), lowered {@link #SUBSCRIPT_DROP} of the size. The marks of the run (bold, italic) add to the style as they add to the box,
 * wherever they are written.</p>
 *
 * <p>{@code image} marks a run that is an image ({@link InlineImage}, reference/common/text-formatting.md), whose
 * text is the single {@link TextRun#OBJECT_REPLACEMENT}: the layout measures it as the image, as tall as the size of
 * the box, and the painting draws it in its place; it has no letter spacing. The rest of the format is kept
 * (a link, for an image that is clicked), but the font and the decorations do not apply to it.</p>
 *
 * @param bold      bold ({@code **text**}), added to the weight of the box
 * @param italic    italic ({@code *text*})
 * @param underline underlined ({@code ++text++}, and a Markdown link)
 * @param strike    struck through ({@code ~~text~~})
 * @param link      the target of the link of the run; {@code null} if it has none
 * @param smallCaps whether the letters are already small caps, set by the layout
 * @param code      whether it is inline code
 * @param family    the font family of the run; {@code null} for that of the box
 * @param footnote  the {@code Id} of the footnote the run calls; {@code null} if it calls none
 * @param label     whether it is the number that opens a footnote
 * @param style     the style of a styled run; {@code null} if it is not one
 * @param image     the image the run is; {@code null} if it is text
 */
public record TextFormat(boolean bold, boolean italic, boolean underline, boolean strike, String link,
                         boolean smallCaps, boolean code, String family, String footnote, boolean label,
                         RunStyle style, InlineImage image) {

    /** Size of the small caps relative to that of the box. */
    public static final double SMALL_CAPS_SCALE = 0.8;

    /**
     * Size of a superscript (the call and the label of a footnote, a run with {@code Position: superscript}) and of a
     * subscript, relative to that of the box.
     */
    public static final double SUPERSCRIPT_SCALE = 0.7;

    /** How much a superscript is raised above the baseline, relative to the size of the box. */
    public static final double SUPERSCRIPT_RISE = 0.33;

    /** How much a subscript is lowered below the baseline, relative to the size of the box. */
    public static final double SUBSCRIPT_DROP = 0.15;

    /** No format: the text as the box declares it. */
    public static final TextFormat PLAIN = new TextFormat(false, false, false, false);

    /**
     * The marks alone: no link, small caps, code, family, footnote, label, style or image. The rest is added with the
     * {@code with...} methods.
     *
     * @param bold      bold, added to the weight of the box
     * @param italic    italic
     * @param underline underlined
     * @param strike    struck through
     */
    public TextFormat(boolean bold, boolean italic, boolean underline, boolean strike) {
        this(bold, italic, underline, strike, null, false, false, null, null, false, null, null);
    }

    /**
     * The font of the run, derived from that of the box: with the family, the weight and the style of its
     * {@link #style()}, if it declares them, and its marks on top. A run with letter spacing has no ligatures, like a
     * box (reference/common/text-formatting.md).
     *
     * @param base the font of the box
     * @return {@code base} itself if the run changes nothing of it, or the font of the run
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

    /**
     * {@return the color of the text of the run: that of its {@link #style()} or, if it does not declare one,
     * {@code box}}
     *
     * @param box the color of the box
     */
    public String color(String box) {
        return style != null && style.color() != null ? style.color() : box;
    }

    /**
     * {@return the letter spacing of the run, in points: that of its {@link #style()} or, if it does not declare one,
     * {@code box}; 0 in an image}
     *
     * @param box the letter spacing of the box, in points
     */
    public double letterSpacing(double box) {
        if (image != null) {
            return 0;
        }
        return style != null && style.letterSpacing() != null ? style.letterSpacing() : box;
    }

    /**
     * {@return the case of the run: that of its {@link #style()} or, if it does not declare one, {@code box}}
     *
     * @param box the case of the box
     */
    public TextCase textCase(TextCase box) {
        return style != null && style.textCase() != null ? style.textCase() : box;
    }

    /**
     * {@return whether the run is painted in small caps ({@code Small caps}): its {@link #style()} or, if it does not
     * declare it, {@code box}} It is not {@link #smallCaps()}, which marks the letters the layout already turned into
     * them.
     *
     * @param box whether the box is painted in small caps
     */
    public boolean smallCaps(boolean box) {
        return style != null && style.smallCaps() != null ? style.smallCaps() : box;
    }

    /**
     * {@return the same format with those marks: bold, italic, underline and strikethrough} They replace those of this
     * format.
     *
     * @param b bold
     * @param i italic
     * @param u underlined
     * @param s struck through
     */
    public TextFormat withMarks(boolean b, boolean i, boolean u, boolean s) {
        return new TextFormat(b, i, u, s, link, smallCaps, code, family, footnote, label, style, image);
    }

    /**
     * {@return the same format with the style of a styled run ({@code [text]{name}})}
     *
     * @param runStyle the style, or {@code null} for none
     */
    public TextFormat withStyle(RunStyle runStyle) {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, code, family, footnote, label,
                runStyle, image);
    }

    /** {@return whether it has underline or strikeout, which are painted apart from the text} */
    public boolean decorated() {
        return underline || strike;
    }

    /**
     * {@return the same format with that link}
     *
     * @param destination the target of the link ({@link Links}), or {@code null} for none
     */
    public TextFormat withLink(String destination) {
        return new TextFormat(bold, italic, underline, strike, destination, smallCaps, code, family, footnote, label,
                style, image);
    }

    /**
     * The same format with small caps turned on or off.
     *
     * @param small whether the letters of the run are small caps
     * @return this format if it already has that value, or a copy with it
     */
    public TextFormat withSmallCaps(boolean small) {
        return small == smallCaps ? this
                : new TextFormat(bold, italic, underline, strike, link, small, code, family, footnote, label, style,
                        image);
    }

    /** {@return the same format as inline code, still without its family} */
    public TextFormat asCode() {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, true, family, footnote, label, style,
                image);
    }

    /**
     * {@return the same format with that font family}
     *
     * @param fontFamily the family of the run, or {@code null} for that of the box
     */
    public TextFormat withFamily(String fontFamily) {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, code, fontFamily, footnote, label,
                style, image);
    }

    /**
     * {@return the same format as the call of the footnote with that {@code Id}: a superscript} It is no longer the
     * label of a note.
     *
     * @param id the {@code Id} of the {@code Footnote} the run calls
     */
    public TextFormat withFootnote(String id) {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, code, family, id, false, style, image);
    }

    /**
     * {@return the same format as the label that opens a footnote: a superscript} It no longer calls a note.
     */
    public TextFormat asLabel() {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, code, family, null, true, style, image);
    }

    /**
     * {@return the same format as a run that is that image, whose text is {@link TextRun#IMAGE}}
     *
     * @param inlineImage the image, or {@code null} for a run of text
     */
    public TextFormat withImage(InlineImage inlineImage) {
        return new TextFormat(bold, italic, underline, strike, link, smallCaps, code, family, footnote, label, style,
                inlineImage);
    }

    /**
     * {@return whether the run is a superscript: the call or the label of a footnote, or a styled run with
     * {@code Position: superscript}}
     */
    public boolean superscript() {
        return footnote != null || label || style != null && style.position() == TextPosition.SUPERSCRIPT;
    }

    /**
     * {@return whether the run is a subscript: a styled run with {@code Position: subscript}} The call of a footnote
     * inside it is still a superscript.
     */
    public boolean subscript() {
        return footnote == null && !label && style != null && style.position() == TextPosition.SUBSCRIPT;
    }

    /**
     * {@return the factor applied to the size of the box to paint the run: {@link #SUPERSCRIPT_SCALE} in a superscript
     * and in a subscript, {@link #SMALL_CAPS_SCALE} or 1}
     */
    public double scale() {
        if (superscript() || subscript()) {
            return SUPERSCRIPT_SCALE;
        }
        return smallCaps ? SMALL_CAPS_SCALE : 1;
    }

    /**
     * {@return how much the run is raised above the baseline of the line, as a factor of the size of the box:
     * {@link #SUPERSCRIPT_RISE}, minus {@link #SUBSCRIPT_DROP} in a subscript, which is lowered, or 0}
     */
    public double rise() {
        if (superscript()) {
            return SUPERSCRIPT_RISE;
        }
        return subscript() ? -SUBSCRIPT_DROP : 0;
    }
}
