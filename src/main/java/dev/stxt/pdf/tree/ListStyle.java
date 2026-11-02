package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.Marker;

/**
 * The properties of lists (reference/components/list.md): the {@code Marker}, the {@code Marker image} and the
 * {@code Start} of a {@code List} and the {@code Number} and the {@code Marker image} of a {@code List item}. They are
 * not inherited.
 */
public final class ListStyle extends StyleGroup {

    /** Marker of the items of a {@code List}; {@code null} in the other boxes. */
    private Marker marker;
    /** Number of the first item of a {@code List}. */
    private int start = 1;
    /** Number that a {@code List item} sets; {@code null} = the one after that of the previous item. */
    private Integer number;
    /** Path of the {@code Marker image}, relative to {@code images/}; {@code null} if it does not declare one. */
    private String markerImage;
    /** {@code Alt} of the {@code Marker image}; {@code null} = decorative. */
    private String markerAlt;
    /** {@code Height} of the {@code Marker image}, in points; {@code null} = the {@code Size} of the item. */
    private Double markerHeight;

    ListStyle() {
    }

    public Marker marker() {
        return marker;
    }

    void setMarker(Marker v) {
        writable();
        marker = v;
    }

    public int start() {
        return start;
    }

    void setStart(int v) {
        writable();
        start = v;
    }

    public Integer number() {
        return number;
    }

    void setNumber(Integer v) {
        writable();
        number = v;
    }

    /**
     * Path of the {@code Marker image} of a {@code List} or a {@code List item}, relative to {@code images/}, and in
     * the generated box of an image marker, that of its image; {@code null} if there is none.
     */
    public String markerImage() {
        return markerImage;
    }

    void setMarkerImage(String v) {
        writable();
        markerImage = v;
    }

    /** {@code Alt} of the {@code Marker image}; {@code null} if the image is decorative. */
    public String markerAlt() {
        return markerAlt;
    }

    void setMarkerAlt(String v) {
        writable();
        markerAlt = v;
    }

    /** {@code Height} of the {@code Marker image}, in points; {@code null} if it takes the {@code Size} of the item. */
    public Double markerHeight() {
        return markerHeight;
    }

    void setMarkerHeight(Double v) {
        writable();
        markerHeight = v;
    }

    /** Copies the {@code Marker image} of another style: the one of an item replaces the one of its list whole. */
    void setMarkerImage(ListStyle from) {
        setMarkerImage(from.markerImage);
        setMarkerAlt(from.markerAlt);
        setMarkerHeight(from.markerHeight);
    }
}
