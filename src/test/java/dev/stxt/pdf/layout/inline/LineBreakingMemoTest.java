package dev.stxt.pdf.layout.inline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import dev.stxt.pdf.layout.LayoutFixture;
import dev.stxt.pdf.tree.Box;

/** The memorized line breaking is made once per box and key, and forgotten box by box. */
class LineBreakingMemoTest {

    private static final InlineLayout.BrokenKey KEY = new InlineLayout.BrokenKey(100, OptionalDouble.empty(),
            List.of(), 0);

    private final LineBreakingMemo memo = new LineBreakingMemo();
    private final LayoutFixture fixture = LayoutFixture.layOut("\tText: a", "\t\tId: A", "\tText: b", "\t\tId: B");
    private final Box a = fixture.fragment("A").box();
    private final Box b = fixture.fragment("B").box();
    private final AtomicInteger made = new AtomicInteger();

    private InlineLayout.Broken broken(Box box, InlineLayout.BrokenKey key) {
        return memo.broken(box, key, () -> {
            made.incrementAndGet();
            return new InlineLayout.Broken(List.of(), 10, LineWidths.uniform(100), null);
        });
    }

    @Test
    void theLineBreakingOfATextIsMadeOncePerKey() {
        InlineLayout.Broken first = broken(a, KEY);
        assertSame(first, broken(a, KEY));
        assertEquals(1, made.get());
        broken(a, new InlineLayout.BrokenKey(50, OptionalDouble.empty(), List.of(), 0));
        assertEquals(2, made.get());
    }

    @Test
    void forgettingABoxKeepsTheOthers() {
        broken(a, KEY);
        broken(b, KEY);
        memo.forget(Set.of(a));
        assertEquals(1, memo.size());
        broken(b, KEY);
        assertEquals(2, made.get());
        broken(a, KEY);
        assertEquals(3, made.get());
    }

    @Test
    void clearForgetsEverything() {
        broken(a, KEY);
        broken(b, KEY);
        memo.clear();
        assertEquals(0, memo.size());
    }
}
