package dev.stxt.pdf.layout;

import dev.stxt.pdf.fragment.Page;
import dev.stxt.pdf.fragment.Placed;
import dev.stxt.pdf.resources.Resources;
import dev.stxt.pdf.tree.Box;
import dev.stxt.pdf.tree.PageSetup;
import dev.stxt.pdf.tree.Metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result of {@link Engine#layOutDocument}: what the output (SVG, PDF or the test dump) needs without building
 * the box tree again. The fragments of the pages point to the boxes of the single tree that was built.
 *
 * <p><strong>Threads:</strong> once laid out it does not change: the pages and fragments are immutable
 * {@code record}s and the engine does not write to the boxes they point to. It can be painted to PDF and SVG from
 * several threads at once with the same {@link dev.stxt.pdf.convert.Converter}; {@code ConcurrentConversionTest}
 * checks it. The boxes have fields that are not {@code final} ({@link Box}), so it has to be handed to the other
 * threads safely (a {@code Future}, an executor, a {@code volatile} field...) and not through a plain field.</p>
 *
 * @param pageSetup the medium of the document: that of the sections that do not declare their own; that of each
 *                  page is {@link Page#pageSetup()}
 * @param metadata  title and language of the document
 * @param root      the root of the document, from which its sections hang (reference/components/document.md)
 * @param pages     the pages, in order; never empty
 * @param resources the resources it was laid out with, a {@link Resources#snapshot()} when it comes from a
 *                  {@link dev.stxt.pdf.convert.Converter}: the PDF and the SVG are painted with the same version of
 *                  each font and each image; {@code null} if it is not known
 */
public record LaidOutDocument(PageSetup pageSetup, Metadata metadata, Box root, List<Page> pages,
        Resources resources) {

    /**
     * Copies {@code pages} into an unmodifiable list; it cannot be {@code null}.
     *
     * @param pageSetup the medium of the document: that of the sections that do not declare their own
     * @param metadata  title and language of the document
     * @param root      the root of the document, from which its sections hang
     * @param pages     the pages, in order; never empty
     * @param resources the resources it was laid out with; {@code null} if it is not known
     */
    public LaidOutDocument {
        pages = List.copyOf(pages);
    }

    /**
     * Without the resources it was laid out with: whoever paints it gives them.
     *
     * @param pageSetup the medium of the document: that of the sections that do not declare their own
     * @param metadata  title and language of the document
     * @param root      the root of the document, from which its sections hang
     * @param pages     the pages, in order; never empty
     */
    public LaidOutDocument(PageSetup pageSetup, Metadata metadata, Box root, List<Page> pages) {
        this(pageSetup, metadata, root, pages, null);
    }

    /**
     * Where the box with that {@code Id} ended up (reference/implementation/api.md): its fragments, one per page it
     * falls on, each with its boxes on the paper ({@link Placed}). A {@code Page overlay} gives one per page it
     * repeats on. It goes through the whole document once: whoever asks for many boxes asks for
     * {@link #placedById()} once instead.
     *
     * @param id the {@code Id} of the box; not {@code null}
     * @return its fragments, in page order; empty if no laid out box has that {@code Id}
     */
    public List<Placed> placed(String id) {
        Objects.requireNonNull(id, "id");
        List<Placed> found = new ArrayList<>();
        for (Page page : pages) {
            for (Placed root : page.placed()) {
                collect(root, id, found);
            }
        }
        return List.copyOf(found);
    }

    /**
     * {@return where every box with an {@code Id} ended up: its fragments ({@link #placed(String)}) by {@code Id}, in
     * the order the document is painted in. One pass through the document}
     */
    public Map<String, List<Placed>> placedById() {
        Map<String, List<Placed>> byId = new LinkedHashMap<>();
        for (Page page : pages) {
            for (Placed root : page.placed()) {
                index(root, byId);
            }
        }
        byId.replaceAll((id, placed) -> List.copyOf(placed));
        return Collections.unmodifiableMap(byId);
    }

    private static void collect(Placed placed, String id, List<Placed> found) {
        if (id.equals(placed.fragment().box().id())) {
            found.add(placed);
        }
        for (Placed child : placed.children()) {
            collect(child, id, found);
        }
    }

    private static void index(Placed placed, Map<String, List<Placed>> byId) {
        String id = placed.fragment().box().id();
        if (id != null) {
            byId.computeIfAbsent(id, k -> new ArrayList<>()).add(placed);
        }
        for (Placed child : placed.children()) {
            index(child, byId);
        }
    }
}
