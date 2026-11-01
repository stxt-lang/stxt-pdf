# References and links

## References

A text can write data that the engine works out:

| Written | Writes |
|---|---|
| `{page}` | The number of the page where the text is |
| `{pages}` | The total number of pages of the current count |
| `{page of: id}` | The page where the box with that `Id` starts |
| `{number of: id}` | The number of an item of a numbered list, or that of a numbered heading |
| `{heading 1}` | The title of the last `Heading 1` that has started, in the header or the footer of each page |
| `{heading 1: first}` | That of the first `Heading 1` that starts on the page |

```
Paragraph: The wiring diagram is on page {page of: wiring}. See clause {number of: pago}.
Block:
    Id: wiring
List:
    Marker: decimal
    List item:
        Id: pago
        Paragraph: Payment will be made within 30 days.
```

- The reference can go before or after the box it names.
- The page number is the one printed on that page (the one in its footer), not the position of the sheet in the PDF.
- A reference to an `Id` that does not exist is an error, and so is the page of something that does not have a single
  page (what is inside a `Page overlay`, which is repeated on all of them) or of something that is on a page without a
  number.
- `{number of: id}` of an item gives only its own (`3`, `c`, `IV`), and of a numbered heading, the complete one (`4.2`).
  Nothing else has a number.
- `{heading N}` (1 to 6) is the **running head** of a book or a manual: the current chapter or section in the header. It
  only goes in a `Page overlay`, which then changes from one page to the next. If the heading is numbered, it carries its
  number (`3 Installation`). A new chapter leaves the section empty until the first one starts, and before the first
  heading of the level it writes nothing.

The engine writes the number before breaking the text into lines, so "page 9" and "page 10" take up the space they need.
If that moves the content, it paginates again until all the numbers are correct.

## Links

A link makes an area of the PDF clickable. It leads to a website, to an email address or to another part of the
document.

**A whole box**, with `Link`:

```
Text: Write to us at soporte@airemas.example
    Link: mailto:soporte@airemas.example
Image: examples/brand/nogal.svg
    Alt: Taller Nogal
    Link: https://tallernogal.example
    Box:
        Width: 80
Paragraph: See the specifications
    Link: #specs
```

**Part of a line**, with the Markdown syntax, in a [`Markdown`](../components/markdown.md) or in a text with
`Markup: markdown`: `See [the specifications](#specs) or [the website](https://airemas.example).`

- The target is an `https://...`, `http://...` or `mailto:...` address, or `#id`: the box with that `Id`, which can be
  anywhere in the document. Anything else is an error.
- A link does not change the look of what it links: the color and the underline are given by its style. The Markdown
  link is the exception: it is underlined.
- `Link` is not inherited and does not go in `Styles`: it belongs to each element.
- The entries of a table of contents already lead to their heading ([Table of contents](../components/table-of-contents.md)).
- In the PDF, each link carries as its text what the box says, for those who cannot see it.

More detail in the reference: [references](../../reference/common/references.md) and [links](../../reference/common/links.md).
