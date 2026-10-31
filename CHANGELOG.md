# Change Log

All notable changes to `dev.stxt:stxt-pdf` are documented in this file, newest first.

The format follows [Keep a Changelog](http://keepachangelog.com/). While the version starts with `0.`, the names of the
`dev.stxt.pdf` namespace and the Java API may change between minor versions.

## [Unreleased]

- A child whose merged top margins push it past the end of the page is no longer laid out there: the block breaks
  before it. Before, an empty box with a fixed `Height` in that position was `PAGE_OVERFLOW` «it is 0 high».
- An empty box (no height, border, padding or content) never overflows vertically: its merged margins can leave it past
  the end of its parent's content, at the end of a block or just before a page break, and that was `BOX_OVERFLOW`.
- The `After: page` (or `column`) of an empty box, and the forced break before the child that follows it, apply also
  when the empty box opens its block. Before, they were lost unless something with content had been placed before.
- The `Keep with next` of the last in-flow child of a block, and the `Keep with previous` of the first one, also apply to
  the block, like the forced breaks: a heading that ends a `Block` is no longer left alone at the end of the page.
- The height of the content of a box is never negative. A box whose negative margins take away more than its children
  measure measures its border and its padding, and the child that sticks out is `BOX_OVERFLOW` at the line of the
  negative `Margin`. Before, the box was lower than its own border and the message gave a negative height.
- An in-flow box that the `Offset` of a `Frame` or a negative `Margin` takes off the physical page is `BOX_OVERFLOW`, at
  the line of that `Offset` or `Margin`. Before, it was laid out without an error and did not show.

## [0.8.0] - 2026-10-31

First version published on Maven Central.

- Reads documents in the `dev.stxt.pdf` namespace, validates them against their `@stxt.schema` with `stxt-core` 1.0.3,
  lays them out on fixed-size pages and writes a tagged PDF (PDF/UA-1, or PDF/UA-2 with `pdfUa(2)`), with embedded
  fonts and one bookmark per heading.
- Elements: `Document`, `Section`, `Block`, `Text`, `Paragraph`, `Heading 1` to `Heading 6`, `List`, `Table`, `Stack`,
  `Grid`, `Columns`, `Frame`, `Overlay`, `Page overlay`, `Image` (PNG, JPEG, SVG and a page of a PDF), `Markdown` and
  `Table of contents`.
- Reusable styles (`Styles`, `For element`, `For style`), in the document or in a separate file.
- Internal references and links, heading and page numbering, rotation, rounded corners and balanced columns.
- The same document, with the same resources and options, gives the same bytes: the PDF carries no dates and its file
  identifier (`/ID`) is made from its content.
- Errors with a code and a line (`InvalidModelException` with its `ErrorCode`), a WCAG AA contrast check
  (`CONTRAST_LOW`) and limits for hostile input (`PAGE_LIMIT`, `IMAGE_TOO_LARGE`, `SVG_TOO_COMPLEX`, `PDF_TOO_LARGE`).
- Messages, comments and javadoc in English. The documentation is in English (`docs/en`) and Spanish (`docs/es`), and
  so are the eleven examples of `test/examples` (one `en/` and one `es/` folder per example).
- The jar includes no fonts: they are read from the resources folder (`resources/` in the working directory, or the one
  passed with `Converter.builder().resources(...)`).
