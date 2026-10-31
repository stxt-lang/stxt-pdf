package dev.stxt.pdf.fragment;

import dev.stxt.pdf.model.NumberFormat;
import dev.stxt.pdf.tree.PageSetup;

import java.util.List;

/**
 * A page of the result.
 *
 * @param number       physical page number, from 1, across the whole document
 * @param content      root fragment with the flow of this page: that of the box of its section
 * @param pageOverlays fragments of the {@code Page overlay} boxes that appear on this page
 * @param pageSetup    the medium of the page: that of its section (reference/components/document.md)
 * @param section      position of its section in the document, from 0
 * @param printed      printed number of the page (what {@code {page}} and the tables of contents say), from 1;
 *                     0 if the page is not numbered ({@code Page numbering: none})
 * @param style        how its number is written ({@code Page number format} of its section)
 * @param blank        it is the blank page that ends its section so that the next one starts on an odd page
 *                     ({@code Begin on: odd}): without content or {@code Page overlay}
 */
public record Page(int number, Fragment content, List<Fragment> pageOverlays, PageSetup pageSetup, int section,
                   int printed, NumberFormat style, boolean blank) {

    public Page {
        pageOverlays = List.copyOf(pageOverlays);
    }

    /** Whether the page carries a printed number. */
    public boolean numbered() {
        return printed > 0;
    }
}
