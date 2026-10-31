# Limits

A valid document can ask for more than one is willing to pay: an image of a few hundred KB that declares billions of
pixels, a 1 KB SVG whose `<use>` elements repeat 10⁹ times. The engine rejects it **before laying out**, with an
error of type `LIMIT` ([Errors](errors.md#error-types)) and, if it comes from an `Image`, with its line. The limits are changed
when converting ([Using from Java](../implementation/api.md#limits)):

| Limit | Default | What it counts | Error |
|---|---|---|---|
| `maxPages` | 2,000 | The pages of the whole document | `PAGE_LIMIT` |
| `maxImagePixels` | 25,000,000 | Width × height of a PNG or a JPEG | `IMAGE_TOO_LARGE` |
| `maxSvgElements` | 20,000 | The elements an SVG draws: those of the file plus those of what each `<use>` repeats | `SVG_TOO_COMPLEX` |
| `maxSvgDepth` | 64 | The nesting levels of an SVG, with the root `<svg>` | `SVG_TOO_COMPLEX` |
| `maxPdfBytes` | 20,000,000 | The bytes of the file of a PDF drawn by an `Image`, without opening it | `PDF_TOO_LARGE` |

The default values are those of a service with little memory. A PNG with transparency needs about 4 bytes per pixel
when decoded (25 Mpx are 100 MB), one more page takes as long as its content, and an SVG of 300,000 rectangles takes
about 5 s. Raising the limits also raises the memory that has to be reserved for each conversion. A 1 KB SVG with 5 levels of
`<use>` of 10 copies each is 111,111 elements. An SVG with more than 15 levels of nested `<use>` cannot be drawn
even if it meets these limits (`IMAGE_FORMAT`), and neither can a circular `<use>`. A PDF is read in full when it is opened, even if
only one of its pages is drawn: for one page of a large PDF, the best option is a PDF with only that page.

The text of the document also has its own limits: 10 MB, 100 nesting levels and 10,000 characters per line,
also with a `LIMIT` error.

> **Decision:** conservative limits, chosen by measuring what each input costs
> ([details](../implementation/api.md#limits)). *Rejected alternative:* a higher set (10,000 pages, 100 Mpx),
> which left a service exposed.
