package dev.stxt.pdf.model;

import java.util.Objects;

/**
 * The style of a styled run, {@code [text]{name}} (reference/common/text-formatting.md): what the {@code For style}
 * rule of that name declares of the properties a run allows. Each value is that of the rule, or {@code null} if the
 * rule does not declare it and the run keeps that of its box. The size, the line height and the alignment are always
 * those of the box, so the lines do not change their height.
 *
 * <p>The Markdown of the text only knows the name ({@link #named}); the box, which knows the rules, gives it its
 * values when it is built. The family of the run ({@code Font}) and that of its code ({@code Code font}) go in
 * {@link TextFormat#family()}, as that of inline code: here they are only kept to tell them apart.</p>
 *
 * @param name          the name of the style, as written in the run
 * @param color         {@code Color}, a hexadecimal color
 * @param family        {@code Font}
 * @param codeFamily    {@code Code font}: the family of the inline code inside the run
 * @param weight        {@code Weight} or {@code Bold}
 * @param style         {@code Italic}
 * @param letterSpacing {@code Letter spacing}, in points
 * @param textCase      {@code Case}
 * @param smallCaps     {@code Small caps}
 */
public record RunStyle(String name, String color, String family, String codeFamily, FontWeight weight,
                       FontStyle style, Double letterSpacing, TextCase textCase, Boolean smallCaps) {

    public RunStyle {
        Objects.requireNonNull(name, "name");
    }

    /** The style of a run with only its name: the values come when its box is built. */
    public static RunStyle named(String name) {
        return new RunStyle(name, null, null, null, null, null, null, null, null);
    }
}
