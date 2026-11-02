package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Leader;
import dev.stxt.pdf.model.Listing;

/**
 * The properties of a {@code Table of contents} (reference/components/table-of-contents.md): what it lists, the
 * levels of the headings it lists and the leader of its entries, which the generated leader of each entry also
 * carries. They are not inherited.
 */
public final class TableOfContentsStyle extends StyleGroup {

    /** {@code Of}: the headings (by default), the figures or the tables. */
    private Listing of = Listing.HEADINGS;
    /** First and last heading level listed by a {@code Table of contents} ({@code Levels}). */
    private int from = 1;
    private int to = 3;
    /** {@code Leader} of a {@code Table of contents}; the generated leader of each entry also carries it. */
    private Leader leader = Leader.DOTS;

    TableOfContentsStyle() {
    }

    public Listing of() {
        return of;
    }

    void setOf(Listing v) {
        writable();
        of = v;
    }

    public int from() {
        return from;
    }

    void setFrom(int v) {
        writable();
        from = v;
    }

    public int to() {
        return to;
    }

    void setTo(int v) {
        writable();
        to = v;
    }

    public Leader leader() {
        return leader;
    }

    void setLeader(Leader v) {
        writable();
        leader = v;
    }
}
