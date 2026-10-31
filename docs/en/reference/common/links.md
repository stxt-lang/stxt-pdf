# Links

A link takes whoever clicks it to a URL outside the document or to another box in it. It is written in two ways, which
produce the same in the PDF: the `Link` property of a box, which makes the whole box clickable, and the Markdown link
(`[text](target)`), which makes part of a line clickable, in a `Markdown` or in a text with `Markup: markdown`.

```
Text: Write to soporte@airemas.example
    Link: mailto:soporte@airemas.example

Image: examples/brand/nogal.png
    Link: https://tallernogal.example

Markdown:
    Content >>
        See [the specifications](#specs) or [the website](https://airemas.example).
```

## Target

A `https://...`, `http://...` or `mailto:...` URL (with a host or a recipient), or `#id`: the `Id` of a box of the
document. Anything else is `LINK_SCHEME`, with the line of the `Link` or, in Markdown, that of the block: an empty or
malformed target, one without a host, one with another scheme (`javascript:`, `file:` or `tel:`) or a relative path.

> **Decision:** only `https`, `http`, `mailto` and `#id`: a PDF should not carry a link that runs or reads something on
> the machine of whoever opens it. *Rejected alternative:* any URI, with a list of forbidden schemes.

## Targets in the document

`#id` leads to the page and to the top left corner of the box with that `Id` (of its first piece, if it is split), which
can be before or after the link. It is `LINK_UNKNOWN`, with the line of the `Link`, if it does not exist or if it is in a
`Page overlay`, which is repeated on every page and has no single place. A `Section` with an `Id` leads to its first page.
A heading of a `Markdown` is a target if it carries `{#id}` ([Markdown](../components/markdown.md#headings)).

## Where `Link` is allowed

In all elements except `Markdown` (its content carries its own links) and `Table of contents` (its entries already carry
theirs). It belongs to the element, like `Id`: it does not go in `Box`, `Typography` or `Break`, nor in the `Styles` rules,
and it is not inherited. What is inside a box with a link is clickable through it. If it also carries a link of its own,
the two areas overlap and the viewer decides which one is activated.

> **Decision:** the link belongs to the element: through a style rule, all the boxes with that style would lead to the
> same place, and inherited, a `Link` in `For element: Document` would link the whole document.

## Appearance

A link does not change the appearance of what it links: the author gives the color and the underline with their style.
The exception is the Markdown link, which comes out **underlined** without changing the color, because there is no other
way to style part of a line. It can be combined with bold and italics (`[the **website**](...)`). The link is one more
mark of the run ([Text formatting](text-formatting.md#runs)): it does not change how words or lines are measured.

> **Decision:** no default appearance, except the underline of the Markdown one. *Rejected alternative:* always blue and
> underlined, or a `For element: Link` rule.

## Areas

The clickable area of a box with `Link` is its **border box**, on each page it goes through. That of a Markdown link is
the rectangle of each piece of each line, with the height of the line. Two consecutive runs with the same target (a bold
part inside a link) are one area. A `Page overlay` with `Link` is clickable on every page, and so are the repeated rows of
a table.

## Accessibility

A screen reader announces each link once, even if it takes up several lines, with what it links: the text of the box (in
a `Block`, those inside joined by spaces, without the page number of a table of contents entry) or the `Alt` of an image.
Without text, it announces the target. That of a `Page overlay` or that of the repeated rows of a table is also announced
once, in its place in the document, even if it is clickable on every page. Links are traversed with the Tab key in reading
order. How it ends up in the PDF: [The PDF inside](../implementation/pdf.md#links).

## What is not supported

- Links with a title, separate link definitions (`[x][ref]`) and images inside a Markdown link: a clickable image is an
  `Image` with `Link`.
- A text of its own for the link, different from what it links.
