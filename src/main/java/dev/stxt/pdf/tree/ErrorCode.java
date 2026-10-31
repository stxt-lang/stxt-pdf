package dev.stxt.pdf.tree;

import dev.stxt.pdf.model.StxtPdfException.Kind;

/**
 * Stable code of each error of the model (reference/common/errors.md). The tests compare the code and
 * the line, not the text of the message, which can improve without breaking them. STXT syntax errors
 * and schema errors carry the codes of the {@code stxt-core} library
 * ({@code INVALID_VALUE}, {@code CHILD_NOT_DECLARED}...), in a {@link DocumentSyntaxException}.
 * Each code says whose fault it is ({@link #kind()}).
 */
public enum ErrorCode {

    // --- Document and medium ----------------------------------------------------------------
    /** The document does not have a single root node {@code Document} with the namespace of the model. */
    DOCUMENT_ROOT,
    /** An invalid size or margin of the medium, or margins that leave no page area. */
    PAGE_SIZE,
    /**
     * A {@code Section} without content (only {@code Page overlay}, which take no space): it would have no page.
     */
    SECTION_EMPTY,
    /** A text with {@code {page}} or {@code {pages}} on a page that has no number ({@code Page numbering: none}). */
    PAGE_NUMBER_UNAVAILABLE,

    // --- Values --------------------------------------------------------------------------------
    /** Syntax of a value (reference/common/values.md): number, percentage, function, list... */
    VALUE_SYNTAX,
    /** A well-written value that the property does not allow ({@code Width: *}, {@code Padding: -2}...). */
    VALUE_NOT_ALLOWED,
    /** {@code fit} as a grid track. */
    FIT_TRACK,

    // --- Structure ----------------------------------------------------------------------------
    /** Two boxes with the same {@code Id}. */
    DUPLICATE_ID,
    /** An {@code Item} or an {@code Area} that does not contain exactly one element. */
    SINGLE_ELEMENT,
    /** A {@code Text} without text, or with text in the value and in {@code Content}. */
    TEXT_CONTENT,
    /** Text without {@code Font} in {@code Typography}, its own or inherited. */
    TEXT_WITHOUT_FONT,
    /** Text without {@code Size} in {@code Typography}, its own or inherited. */
    TEXT_WITHOUT_FONT_SIZE,

    // --- Headings and paragraphs --------------------------------------------------------------
    /** A skip in the heading hierarchy: the first one is not {@code Heading 1}, or one goes up more than one level. */
    HEADING_LEVEL,
    /** A heading inside a {@code Page overlay}. */
    HEADING_IN_PAGE_OVERLAY,
    /** A {@code Text} of a heading with {@code Break}: the engine already avoids the break. */
    BREAK_IN_HEADING,
    /**
     * A blank line in a {@code Paragraph}, which would split it into several paragraphs, or in a {@code Text} that is
     * not preformatted.
     */
    TEXT_BLANK_LINE,

    // --- Lists -----------------------------------------------------------------------------------
    /** A {@code List item} without any element. */
    LIST_ITEM_EMPTY,
    /** {@code Start} or {@code Number} in a list that does not number ({@code bullet}, {@code dash}, {@code none}). */
    LIST_NUMBER_NOT_ALLOWED,
    /**
     * A number that the numbering cannot write: negative, 0 in letters or Roman numerals, or over 3999 in Roman
     * numerals.
     */
    LIST_NUMBER_RANGE,
    /** An item with a marker whose content does not start with a text to align it with. */
    LIST_ITEM_WITHOUT_LINE,

    // --- Tables -----------------------------------------------------------------------------------
    /** {@code Header rows}, {@code Body} and {@code Footer rows} out of that order. */
    TABLE_ORDER,
    /** A row that does not fill exactly the columns of the table. */
    TABLE_ROW_CELLS,
    /** A {@code Span} that goes outside the table or its group, or that overlaps another cell. */
    TABLE_SPAN,
    /** A {@code Cell} with text in the value and elements. */
    CELL_CONTENT,
    /** {@code Break} in a row of {@code Header rows} or {@code Footer rows}, which are repeated on each page. */
    TABLE_ROW_BREAK,
    /** A {@code Column align} with a number of alignments different from the number of columns of the table. */
    TABLE_COLUMN_ALIGN,
    /** {@code Row header} in a cell of {@code Header rows}, whose cells are already column headers. */
    ROW_HEADER_IN_HEADER_ROWS,

    // --- Columns -------------------------------------------------------------------------------
    /** A {@code Columns} without {@code Count} or {@code Min column width}. */
    COLUMNS_WITHOUT_COUNT,
    /** {@code Separator} wider than {@code Gap}: the line would be painted over the columns. */
    SEPARATOR_WIDTH,
    /** {@code Before} or {@code After: column} outside a {@code Columns}. */
    COLUMN_BREAK_OUTSIDE_COLUMNS,
    /**
     * {@code Keep with next} with {@code After}, or {@code Keep with previous} with {@code Before}, in the same box.
     */
    KEEP_CONFLICT,
    /** An {@code Overlay} or a {@code Page overlay} inside a {@code Columns}. */
    OVERLAY_IN_COLUMNS,

    // --- Markdown ------------------------------------------------------------------------------
    /** Some Markdown outside the supported subset: links, images inside a text, code, quotes, HTML... */
    MARKDOWN_UNSUPPORTED,
    /**
     * A list or a table of a {@code Markdown} without its {@code Bullet markers}, {@code Number markers} or
     * {@code Table columns}.
     */
    MARKDOWN_PROPERTY_MISSING,
    /** Code, inline or in a block, without {@code Code font} in {@code Typography}, its own or inherited. */
    CODE_WITHOUT_FONT,

    // --- Images ---------------------------------------------------------------------------------
    /** An {@code Image} without a path, with a path that goes outside the folder or that does not exist. */
    IMAGE_UNKNOWN,
    /**
     * An {@code Image} that is not PNG, JPEG, PDF or a supported SVG, that cannot be read or whose page of a PDF cannot
     * be drawn.
     */
    IMAGE_FORMAT,
    /**
     * {@code Pdf page} in an {@code Image} that is not a PDF, or a page that the PDF does not have (line of {@code Pdf
     * page}).
     */
    IMAGE_PAGE,
    /** {@code Keep together} or {@code Edges} in an {@code Image}, which is not split. */
    BREAK_IN_IMAGE,

    // --- References ----------------------------------------------------------------------------
    /** A reference ({@code {page of: id}}) without {@code Id}, or with an {@code Id} that no box has. */
    REFERENCE_UNKNOWN,
    /**
     * A reference to the page of a box that does not have a single page: that of a {@code Page overlay} or one of its
     * boxes.
     */
    REFERENCE_NOT_PLACEABLE,
    /** A reference to a box that has no number: it is neither an item of a numbered list nor a heading. */
    REFERENCE_NOT_NUMBERED,
    /** The pages the text refers to do not stabilize: each pagination pass changes them. */
    REFERENCE_UNSTABLE,

    // --- Links --------------------------------------------------------------------------------
    /** A link target that is empty, malformed or with a scheme that is not https, http, mailto or {@code #id}. */
    LINK_SCHEME,
    /**
     * A {@code #id} link to an {@code Id} that does not exist or that is in a {@code Page overlay}, which is repeated
     * on all the pages.
     */
    LINK_UNKNOWN,

    // --- Table of contents --------------------------------------------------------------------
    /** A {@code Table of contents} without any heading in the levels of {@code Levels}. */
    TABLE_OF_CONTENTS_EMPTY,

    // --- Reusable rules (Styles) ---------------------------------------------------------------
    /** {@code For element} with a name that is not a box element. */
    STYLE_UNKNOWN_ELEMENT,
    /** Two rules with the same selector. */
    STYLE_DUPLICATE,
    /**
     * A rule applies a group that the element does not accept: {@code Break} in {@code Overlay},
     * {@code Page overlay}, a heading, a {@code Text} of a heading or a {@code Cell}.
     */
    STYLE_NOT_ALLOWED,
    /** A {@code Style} class without a {@code For style} rule. */
    STYLE_WITHOUT_RULE,

    // --- Combinations without effect or contradictory ----------------------------------------
    /** A {@code Frame} with top and bottom, or with left and right. */
    OFFSET_CONFLICT,
    /**
     * An {@code Overlay} or a {@code Page overlay} with start, size and end declared and without {@code auto} margins.
     */
    OVERCONSTRAINED,
    /** An {@code auto} margin where it neither centers nor absorbs anything. */
    MARGIN_AUTO_WITHOUT_EFFECT,
    /** {@code Start} in a {@code Section} without {@code Page numbering: restart}. */
    START_WITHOUT_RESTART,
    /**
     * Something that is not the appearance ({@code Background}, {@code Border}, {@code Border color},
     * {@code Corner radius}) in the {@code Box} of a {@code Row}.
     */
    ROW_BOX,
    /** {@code Page number format} in a {@code Section} with {@code Page numbering: none}. */
    PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS,
    /** {@code Begin on: odd} in the first {@code Section}, which already starts on page 1. */
    BEGIN_ON_FIRST_SECTION,
    /** {@code Line distribution} without {@code Wrap: true}. */
    LINE_DISTRIBUTION_WITHOUT_WRAP,
    /** Two-value {@code Gap} in a {@code Stack} without {@code Wrap: true}. */
    GAP_WITHOUT_WRAP,
    /** {@code Grow} in a vertical {@code Stack} without a definite height. */
    GROW_WITHOUT_EFFECT,
    /** {@code Horizontal} or {@code Vertical} of an {@code Item} on the main axis of its {@code Stack}. */
    ITEM_MAIN_AXIS,
    /** {@code Aspect ratio} with {@code Width} and {@code Height} declared. */
    ASPECT_RATIO_CONFLICT,
    /** Minimum greater than the maximum, or a size outside its minimum and its maximum. */
    MIN_MAX_CONFLICT,
    /** A vertical percentage whose base is not a definite height. */
    INDEFINITE_PERCENTAGE,
    /** Non-rectangular {@code Areas} or an invalid {@code Area} value. */
    GRID_PLACEMENT,
    /** {@code Rotate origin} without {@code Rotate}. */
    ROTATE_ORIGIN_WITHOUT_ROTATE,
    /** {@code Gap} or {@code Separator} in a {@code Columns} with {@code Count: 1}. */
    SINGLE_COLUMN,
    /** {@code Separator color} without {@code Separator}. */
    SEPARATOR_COLOR_WITHOUT_SEPARATOR,
    /** {@code Isolate margins: true} on an item of {@code Stack} or {@code Grid}, which already isolates. */
    ISOLATE_MARGINS_IN_ITEM,
    /**
     * An own {@code Break} inside an {@code Overlay}, a {@code Page overlay}, a rotated {@code Frame} or a rotated
     * in-flow box.
     */
    BREAK_IN_UNBREAKABLE,
    /**
     * {@code Keep together} or {@code Edges} in a rotated {@code Frame} or in a rotated in-flow box, which are not
     * split.
     */
    BREAK_IN_ROTATED,
    /** {@code Isolate margins: true} in a rotated in-flow box, which already isolates. */
    ISOLATE_MARGINS_IN_ROTATED,
    /**
     * {@code Rotate} in a {@code Block}, a {@code Text}, a {@code Paragraph} or an {@code Image} that is an item of
     * {@code Stack} or {@code Grid}.
     */
    ROTATED_ITEM,

    // --- Overflows (during layout) -----------------------------------------------------------
    /** A word wider than its line. */
    TEXT_OVERFLOW,
    /**
     * A box that does not fit in its container, content that exceeds a fixed height, or an {@code Overlay} that goes
     * outside the page.
     */
    BOX_OVERFLOW,
    /** Something in the flow taller than the page that cannot be split. */
    PAGE_OVERFLOW,
    /** The marker of an item does not fit in the indent: the left padding of the list plus the margin of the item. */
    LIST_MARKER_OVERFLOW,
    /**
     * The header and the footer of a table, which are repeated on each page, leave no room for the body on an empty
     * page.
     */
    TABLE_REPEAT_OVERFLOW,

    // --- Fonts --------------------------------------------------------------------------------
    /** {@code Font} is not in the fonts folder. */
    FONT_UNKNOWN,
    /** The family does not have the variant of {@code Bold} and {@code Italic}. */
    FONT_VARIANT_MISSING,
    /** The font does not have a character of the text. */
    GLYPH_MISSING,

    // --- Accessibility (during conversion) ---------------------------------------------------
    /** The text has low contrast with what is under it; the check can be disabled when converting. */
    CONTRAST_LOW,

    // --- Limits -------------------------------------------------------------------------------
    /** The document takes up more pages than {@code Limits.maxPages}; it belongs to no line. */
    PAGE_LIMIT(Kind.LIMIT),
    /** A PNG or JPEG image with more pixels than {@code Limits.maxImagePixels} (line of the {@code Image}). */
    IMAGE_TOO_LARGE(Kind.LIMIT),
    /**
     * An SVG that draws more elements or nests more levels than {@code Limits.maxSvgElements} and
     * {@code maxSvgDepth} (line of the {@code Image}).
     */
    SVG_TOO_COMPLEX(Kind.LIMIT),
    /** A PDF of more bytes than {@code Limits.maxPdfBytes} (line of the {@code Image}). */
    PDF_TOO_LARGE(Kind.LIMIT);

    private final Kind kind;

    ErrorCode() {
        this(Kind.INVALID_DOCUMENT);
    }

    ErrorCode(Kind kind) {
        this.kind = kind;
    }

    /** Whose fault it is: the document's ({@link Kind#INVALID_DOCUMENT}) except for the limits ({@link Kind#LIMIT}). */
    public Kind kind() {
        return kind;
    }
}
