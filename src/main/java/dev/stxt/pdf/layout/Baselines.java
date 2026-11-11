package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Fragment;
import dev.stxt.pdf.fragment.TextLine;
import dev.stxt.pdf.layout.inline.TextMeasurer;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The baseline of the first line of text of what is already laid out: where the marker of a list item goes
 * (reference/components/list.md) and what {@code Vertical: baseline} aligns in a horizontal {@code Stack}
 * (reference/components/stack.md).
 */
public final class Baselines {

    private Baselines() {
    }

    /**
     * Baseline of the first line of a fragment, from the top of its border box: its own first line or, without
     * lines, that of its first in-flow child, going down to a fragment with lines.
     *
     * @return empty if the first thing in it is not a text: an image, an empty box
     */
    public static OptionalDouble first(Fragment fragment, TextMeasurer measurer) {
        OptionalDouble baseline = first(List.of(fragment), measurer);
        return baseline.isPresent() ? OptionalDouble.of(baseline.getAsDouble() - fragment.y()) : baseline;
    }

    /**
     * Baseline of the first line of some child fragments, relative to the content box of
     * their parent: that of the first in-flow child, going down to a fragment with lines.
     */
    public static OptionalDouble first(List<Fragment> children, TextMeasurer measurer) {
        double top = 0;
        Optional<Fragment> next = firstInFlow(children);
        while (next.isPresent() && next.get().first()) {
            Fragment f = next.get();
            double contentTop = top + f.y() + f.border().top() + f.padding().top();
            if (!f.lines().isEmpty()) {
                TextLine l = f.lines().get(0);
                return OptionalDouble.of(measurer.vertical(f.box().style().typography().fontFace())
                        .baseline(contentTop + l.y(), l.height(), l.fontSize()));
            }
            top = contentTop;
            next = firstInFlow(f.children());
        }
        return OptionalDouble.empty();
    }

    /** The first in-flow fragment, not counting the markers. */
    private static Optional<Fragment> firstInFlow(List<Fragment> fragments) {
        return fragments.stream()
                .filter(f -> !f.box().style().isOutOfFlow() && !f.box().isMarker())
                .findFirst();
    }
}
