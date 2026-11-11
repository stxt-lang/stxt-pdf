# `Barcode`

A `Barcode` puts in the document a QR code or a barcode that the engine generates from its data. It is a generated
image: it does not come from a file but from its value, and the engine draws it with rectangles (the **modules**).
Otherwise it is laid out, broken across pages and painted like an [`Image`](image.md), with its `Box` (margins, padding,
border, background and corners) and its `Break` (without `Keep together` or `Edges`, because it is not split). It also
accepts `Id`, `Style`, `Link`, `Rotate` and, of `Typography`, only `Color` (and with
[`Readable text`](#readable-text), those of the font of that text).

```
Document (dev.stxt.pdf):
    Language: en
    Title: Delivery note
    Page:
        Paper: A5
        Margin: 30
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
    Section:
        Stack:
            Horizontal: justify
            Vertical: bottom
            Item:
                Block:
                    Text: Delivery note AL-2025-0388
                        Typography:
                            Bold: true
                    Barcode: AL-2025-0388
                        Alt: Barcode of delivery note AL-2025-0388
                        Barcode format: code-128
                        Box:
                            Width: 150
                            Height: 30
                            Margin: 6 0 0 0
            Item:
                Barcode: https://tallernogal.example/albaranes/AL-2025-0388
                    Alt: QR code with the tracking of the delivery note
                    Error correction: quartile
                    Box:
                        Width: 72
                        Padding: 4
                        Border: 0.5
                        Border color: #A65C1B
                    Typography:
                        Color: #3B2A1E
```

(A delivery note with its number in Code 128 on the left and, on the right, a QR code with the tracking address, with
dark brown modules inside a frame.)

| Property | Values | Default | Notes |
|---|---|---|---|
| *(value)* | the data | required (or `Content`) | What it encodes: `Barcode: https://example.com/verify/1234`. |
| `Content` | `>>` block | none | The data of several lines, with the lines as they are. Either the value or `Content`, one of the two. |
| `Barcode format` | `qr`, `data-matrix`, `gs1-data-matrix`, `pdf417`, `code-128`, `gs1-128`, `ean-13` | `qr` | The kind of code ([Formats](#formats)). |
| `Error correction` | `low`, `medium`, `quartile`, `high` | `medium` | Only in a QR code: the part of the modules that can be damaged while the code can still be read, 7 %, 15 %, 25 % and 30 %. In another format it is an error. |
| `Quiet zone` | integer from 0 to 40 | that of the format | The light modules on each side of the code ([The modules](#the-modules)). |
| `Readable text` | boolean | `false` | Only in `code-128`, `gs1-128` and `ean-13`: the data, as text, under the bars ([Readable text](#readable-text)). In another format it is an error. |
| `Alt` | non-empty text | required | What it is and where it leads, for whoever cannot see it ([Accessibility](#accessibility)). |
| `Rotate` | see [Layers and rotation](../common/layers-and-rotation.md#rotation-in-flow-block-text-paragraph-and-image) | none | Rotates it about its center, like an `Image`. |
| `Side` | `left`, `right` | none | Makes it a side box, like an `Image`: the text that follows it runs beside it ([Block](block.md#side-boxes-side)). |
| `Below side boxes` | boolean | `false` | Moves it below the side boxes before it ([Block](block.md#side-boxes-side)). |

`Barcode format`, `Error correction`, `Quiet zone`, `Readable text`, `Side` and `Below side boxes` can also come from a rule of `Styles`
([Element properties](../common/styles.md#element-properties)); the data, `Alt` and `Rotate` cannot.

## Formats

| `Barcode format` | What it is | Data |
|---|---|---|
| `qr` | QR code (ISO/IEC 18004): a square matrix of modules | Any text. If it is all ASCII it goes as is; if not, in UTF-8, with the ECI designator that tells the reader so. 2331 bytes fit with `Error correction: medium`, 2953 with `low` and 1273 with `high` (more if they are only digits or capital letters). |
| `data-matrix` | Data Matrix ECC 200 (ISO/IEC 16022), square: the code of industrial and postal labels | Any text, with the same rule as the QR code: ASCII as is and, if not, UTF-8 with its ECI designator. 1555 bytes fit (more if they are only digits or capital letters). |
| `gs1-data-matrix` | GS1 DataMatrix: a Data Matrix with GS1 data, the one of medicines and medical devices | A list of application identifiers ([GS1 data](#gs1-data)). |
| `pdf417` | PDF417 (ISO/IEC 15438): stacked rows of bars, the one of boarding passes and government forms | Any text, with the same rule: ASCII as is and, if not, UTF-8 with its ECI designator. About 1800 characters of text fit. |
| `code-128` | Code 128 (ISO/IEC 15417): a row of bars | The printable ASCII characters, from the space to `~`, from 1 to 80. |
| `gs1-128` | GS1-128: a Code 128 with GS1 data, the one of logistics labels | A list of application identifiers ([GS1 data](#gs1-data)), of at most 48 characters. |
| `ean-13` | EAN-13 (ISO/IEC 15420), the code of products | 12 digits, to which the engine adds the check digit, or 13 with the check digit, which has to match. |

Data that is empty, in the value and in `Content` at the same time, or that the format cannot encode is an error
(`BARCODE_DATA`, with the line of the `Barcode`), with what fails: the character that Code 128 does not accept, the check
digit that an EAN-13 should have, the bytes that do not fit in the QR code or the GS1 identifier that is not well written.
`Error correction` outside a `qr` is `BARCODE_PROPERTY_WITHOUT_EFFECT`: a Data Matrix has its own, fixed, and a PDF417
takes the level that its standard recommends for the length of the data (2 up to 40 characters, 3 up to 160, 4 up to 320
and 5 from there on).

> **Decision:** a Data Matrix is always square, and the engine chooses the smallest one that holds the data. *Rejected
> alternative:* the rectangular symbols of the standard and a property to ask for them, which only very narrow labels
> use.

> **Decision:** `Error correction` is only of the QR code, and the engine sets the level of a PDF417 with the table of
> its standard. *Rejected alternative:* the nine levels of PDF417 (0 to 8) in the same property, which has four named
> values, or in another one.

The value of a line is taken without the blanks at its ends. In `Content` the lines are separated by a line feed (`LF`),
with their relative indent and the empty lines in between: it is what data with a format by lines needs, such as the QR
code for payment by transfer (EPC) or a contact card.

> **Decision:** the data of several lines goes in `Content`, like the text of a `Text`. *Rejected alternative:* an escape
> sequence in the value (`\n`), which STXT does not have and which would have to be invented just here.

> **Decision:** a QR code with data that is not ASCII goes in UTF-8 with its ECI designator, and one that is, without it.
> *Rejected alternative:* the designator always, which some old readers do not understand and which changes nothing in
> ASCII.

### GS1 data

The data of a `gs1-128` and of a `gs1-data-matrix` is a list of **application identifiers**, each one in parentheses
before its value, as they are read on the label:

```
Barcode: (01)09501101530003(17)261231(10)AB-123
    Alt: Code of the product 09501101530003, batch AB-123, expiry 2026-12-31
    Barcode format: gs1-data-matrix
```

The parentheses are not encoded. The engine puts the function character `FNC1` at the start of the symbol, which is what
tells the reader that the data is GS1, and another one after each value of variable length that is not the last; the
identifiers of predefined length do not have it. What is checked:

- the form: each identifier, 2 to 4 digits in parentheses, with a value after it;
- those of **predefined length** (the ones that start with `00` to `04`, `11` to `20`, `31` to `36` and `41`): the
  digits of the identifier and those of its value, which is only digits. `(01)`, a GTIN, has 14; `(00)`, an SSCC, 18;
  `(17)`, a date, 6;
- the **check digit** of `(00)`, `(01)` and `(02)`, which has to match;
- the other values: 1 to 90 characters, which are digits, unaccented letters and the signs `!"%&'*+,-./:;<=>?_`.

What does not match is `BARCODE_DATA`, with what fails. The format of the value of each identifier of variable length
(that `(10)` has at most 20 characters, or that `(15)` is a date that exists) is not checked.

> **Decision:** GS1 data is written with the identifiers in parentheses, which is how the label itself prints them and
> how whoever generates them gives them, and the engine puts the `FNC1` characters. *Rejected alternative:* an escape for
> `FNC1` in the value of a `code-128`, which makes whoever writes know which identifiers have it after them.

> **Decision:** `gs1-128` and `gs1-data-matrix` are formats, with the names of GS1. *Rejected alternative:* a boolean
> property (`GS1: true`) on `code-128` and `data-matrix`, which would be an error in the other formats and leaves the
> format half said.

> **Decision:** a `gs1-data-matrix` is encoded in the ASCII encodation of the standard (one character per codeword and
> two digits per codeword), in the engine, and ZXing adds the error correction and places the modules. *Rejected
> alternative:* the compact encoder of ZXing 3.5.4, its only one that knows `FNC1`, which writes it wrong inside a run of
> capital letters and digits. GS1 data is mostly digits, which the ASCII encodation packs in pairs.

## The modules

The drawing of a `Barcode` is a matrix of modules, each one dark or light, which includes the **quiet zone**: the light
margin that the reader needs around the code. It is part of the content box.

| `Barcode format` | Quiet zone | Natural module |
|---|---|---|
| `qr` | 4 modules on each side | 0.5 × 0.5 mm |
| `data-matrix`, `gs1-data-matrix` | 1 module on each side | 0.5 × 0.5 mm |
| `pdf417` | 2 modules on each side | 0.33 mm wide; each row, 1 mm high (three modules) |
| `code-128`, `gs1-128` | 10 modules on each side | 0.33 mm wide; the bars, 22.85 mm high |
| `ean-13` | 11 modules on the left and 7 on the right | 0.33 mm wide; the bars, 22.85 mm high |

Those zones are the ones of each standard, and they apply if no other is declared. `Quiet zone: N` puts `N` light modules
on each side, in all the formats (in an EAN-13, the same on the left and on the right; in the matrix ones and the
PDF417, also above and below), from 0 to 40; outside that
range it is `VALUE_NOT_ALLOWED`. As the zone is part of the natural size, in a box of a given width fewer modules of zone
give wider modules: a Code 128 150 points wide with `Quiet zone: 5` has wider bars than with the 10 of the standard. With
`Quiet zone: 0`, the code reaches the edge of the content box, and the light margin has to come from the `Padding` with a
light background or from whatever is around. A zone smaller than that of the standard can prevent some readers from
reading it: the engine does not check it.

> **Decision:** `Quiet zone` counts modules, not millimeters, and works the same in all the formats. The standards give
> it in modules, and so the zone scales with the code. *Rejected alternative:* a length (`Quiet zone: 2mm`), which with the
> same value would give a different zone depending on the width of the box, and a property only for Code 128, which would
> leave the QR code and the EAN-13 without the same possibility.

> **Decision:** a zone smaller than that of the standard is accepted, without a warning. Many readers read with less
> (ZXing, when it draws a Code 128, leaves 5 modules on each side), and whoever prints the code is who knows what reads it.
> *Rejected alternative:* a minimum per format, which would prevent reproducing documents that are already printed with a
> smaller zone.

The dark modules are painted in the `Color` of the typography, its own or inherited (black if nobody declares it), and the
light ones are not painted: the background of the box (`Background`) shows or, without it, whatever is behind. Of its
`Typography`, a `Barcode` only accepts `Color` and, with [`Readable text`](#readable-text), those of the font of the text:
any other property is `BARCODE_PROPERTY_WITHOUT_EFFECT`. What a rule of
`Styles` gives is not checked, as in any box without text.

The engine does not check the contrast of the modules with the background ([Contrast](../common/pdf.md#contrast) is only
for text): a light code on a light background cannot be read.

> **Decision:** the color of the modules is the `Color` of `Typography`, which is inherited and goes in the rules like that
> of the text. *Rejected alternative:* a property of its own (`Module color`), another name for the same thing.

> **Decision:** the engine generates the codes with ZXing (Apache 2.0), which is pure Java and does not draw: it gives the
> matrix, and the canvas paints it with rectangles. *Rejected alternative:* a QR encoder of its own (Reed-Solomon, masks and
> versions), some five hundred lines to maintain for the same thing.

## Readable text

`Readable text: true` puts the data, as text, under the bars of a `code-128`, a `gs1-128` or an `ean-13`. In a `qr`, a
`data-matrix`, a `gs1-data-matrix` or a `pdf417` it is `BARCODE_PROPERTY_WITHOUT_EFFECT` (with `false`, which is the
default, it is not).

```
Document (dev.stxt.pdf):
    Language: en
    Title: Label
    Page:
        Width: 300
        Height: 220
        Margin: 20
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 9
        For element: Barcode
            Readable text: true
    Section:
        Barcode: 8412345678905
            Alt: Code of the product 8412345678905
            Barcode format: ean-13
        Barcode: (00)395011015300000011
            Alt: Code of the package 395011015300000011
            Barcode format: gs1-128
            Box:
                Width: 220
                Height: 50
                Margin: 12 0 0 0
            Typography:
                Size: 8
                Font: IBM Plex Mono
```

(An EAN-13 with its digits as on a product and, below it, a GS1-128 with its text centered in a monospaced font.)

Which text it is and where it goes:

| `Barcode format` | Text | Arrangement |
|---|---|---|
| `ean-13` | The 13 digits, with the check digit | As on a product: the first digit at the left of the bars, in the quiet zone (centered in its last 7 modules), and each of the other twelve centered under the 7 modules that encode it. The guard bars (those at the start, the center and the end) go down to the middle of the text. |
| `code-128` | The data | One line centered under the code, quiet zone included. |
| `gs1-128` | The data, with the parentheses of its identifiers | One centered line, the same. |

The text is painted with the font of the box: `Font`, `Size`, `Bold` or `Weight`, `Italic` and `Color`, from its
`Typography` or inherited. With `Readable text`, the `Typography` of the `Barcode` accepts those properties; the others
(`Align`, `Letter spacing`, `Case`...) are still `BARCODE_PROPERTY_WITHOUT_EFFECT`. It needs a font and a size, like any
text (`TEXT_WITHOUT_FONT`, `TEXT_WITHOUT_FONT_SIZE`, with the line of `Readable text`), and the font has to have its
characters (`GLYPH_MISSING`).

The text takes, at the bottom of the content box, the height of its font (the ascent plus the descent, at its `Size`),
and the bars, the rest:

- without `Height`, that height **is added** to the natural one of the code: an EAN-13 with a text of 9 pt in IBM Plex
  Sans measures 37.29 mm wide and 22.85 mm plus the height of the text, and its bars still measure 22.85 mm;
- with `Height`, or with `Width` and the aspect ratio, the text **is taken** from the box and the bars get shorter.

A text that does not fit is an error (`BARCODE_TEXT_OVERFLOW`, with the line of the `Barcode`), which is checked with the
final size of the box, like the [minimum module](#minimum-module), and which no check removes:

- a piece wider than the content box or that goes out of it (the text of a Code 128 wider than the code);
- a digit of an EAN-13 wider than its 7 modules, which runs into the one before it;
- the first digit of an EAN-13 with a `Quiet zone` of fewer than 7 modules, which leaves it no room;
- a text as tall as the content box, which leaves no height for the bars.

It is fixed with a smaller `Size` or a larger box. In an EAN-13 of natural size, each digit has 2.31 mm (6.55 pt): those
of IBM Plex Sans fit up to 10 pt.

In the tagged PDF, the text is part of the figure of the code, which is read with its `Alt`: a screen reader does not
announce it separately. It can be selected and searched.

> **Decision:** `Readable text` is a boolean and the format sets the arrangement: that of the standard in an EAN-13 and
> one centered line in the others. *Rejected alternative:* a property with the position (`above`, `below`) and the
> alignment, which the standards do not let choose in an EAN-13 and which in a Code 128 is solved with a `Text` beside
> it.

> **Decision:** the text is painted with the font of the box, the one the document chooses. *Rejected alternative:* the
> OCR-B of the standard of EAN-13, one more font to ship with the engine; whoever needs it puts it in `fonts/` and
> declares it in `Font`.

> **Decision:** the height of the text is added to the natural size, and the bars keep theirs. *Rejected alternative:*
> always taking it from the bars, which changes the height of the bars of a code when the text is added.

> **Decision:** a text that does not fit is an error. *Rejected alternative:* shrinking it until it fits, which gives
> digits of a size the document has not asked for, and clipping it, which is a half result.

## Size

A `Barcode` is sized like an [`Image`](image.md#sizing) whose natural size is that of its modules, with the quiet zone
(that of the standard or that of `Quiet zone`): a QR code of 25 modules (33 with the zone) measures 16.5 × 16.5 mm, and an EAN-13 (113 modules), 37.29 × 22.85 mm. With
`Width` or `Height`, the other side follows from that aspect ratio, and `Aspect ratio`, if declared, replaces it. The
intrinsic sizes are those of an image.

The content box does not have to have the aspect ratio of the code. If it does not:

- a **QR code**, a **Data Matrix** and a **PDF417** are drawn whole, as large as they fit and centered, with the
  proportion of their modules (like `Fit: inside`): square in the first two, and three times as tall as wide in a
  PDF417;
- a **barcode of one row** (`code-128`, `gs1-128`, `ean-13`) is stretched to the box (like `Fit: stretch`): the bars take
  up the whole height, minus that of its [readable text](#readable-text) if it has one, and all the modules have the same
  width.

A `Barcode` does not accept `Fit`.

> **Decision:** the natural size comes from a fixed module: 0.5 mm in a QR code and a Data Matrix, and the nominal size
> of EAN-13 (0.33 × 22.85 mm) in the barcodes; in a PDF417, that width and rows three modules high, the minimum of its
> standard. So a `Barcode` without a size is seen and read, and it is measured in a `Stack` or a
> `Grid` like an image. *Rejected alternative:* requiring `Width` or `Height`, which leaves without an intrinsic size the one
> that declares it as a percentage.

> **Decision:** a QR code in a box that is not square is drawn centered, without an error as long as its modules do not go
> below the [minimum](#minimum-module). *Rejected alternative:* an error if `Width` and `Height` are not equal, which with
> percentages cannot be known before the layout.

### Minimum module

A code shrunk until it cannot be read is an error. Each format has a minimum module:

| `Barcode format` | Minimum module | Where it comes from |
|---|---|---|
| `qr` | 0.25 mm | About three dots of a 300 dpi printer. ISO/IEC 18004 does not set one |
| `data-matrix`, `gs1-data-matrix`, `pdf417` | 0.25 mm | The same criterion |
| `code-128`, `gs1-128` | 0.25 mm | The same criterion. ISO/IEC 15417 does not set one |
| `ean-13` | 0.264 mm | 80 % of the nominal module (0.33 mm), the smallest that ISO/IEC 15420 allows |

If the content box, once laid out, leaves narrower modules, it is an error (`BARCODE_TOO_SMALL`, with the line of the
`Barcode`) that gives the module that is left and the minimum size of the content box. In a QR code, a Data Matrix and a PDF417 the shorter side
counts (in a wide and low box, the height); in a barcode of one row, only the width, because stretching the bars in height
does not change the module. The quiet zone is part of the drawing: a QR code of 25 modules plus 4 of zone on each side (33) needs a content
box of 8.25 × 8.25 mm (23.39 pt), and the `Padding` does not count. It is checked with the final size, after pagination: in
a `Stack` or a `Grid` a code is measured several times, and only the last one counts.

Whoever converts can remove the check (`Check.BARCODE_SIZE`, [Using from Java](../implementation/api.md#checks)), and then
the code is drawn as small as its box.

> **Decision:** the minimum is fixed per format and there is no property in the document to change it; whoever needs to
> draw a smaller code removes the check when converting. *Rejected alternative:* a property of the `Barcode`
> (`Min module`), another value to write in each document for a rare case; and drawing the shrunk code without a warning,
> as up to 0.9.0, which gives a half result.

> **Decision:** 0.25 mm in all the formats but EAN-13, the smallest module that a 300 dpi office printer draws with three dots.
> *Rejected alternative:* the minimums of GS1 (0.396 mm in a point-of-sale QR code and 0.495 mm in a logistics GS1-128),
> meant for checkout and warehouse scanners, which would rule out the codes that invoices and certificates carry. Whoever
> prints labels for those scanners gives the box the size they ask for.

## Page breaks

Like an image: it is not split, and if it does not fit in what is left of the page (or of the column), it moves whole to
the next one. `Keep together` and `Edges` are an error (`BREAK_IN_IMAGE`), also if a rule of `Styles` gives them.

## Where it goes

Where an `Image` goes: in a section, `Block`, `Frame`, `Overlay`, `Page overlay`, `Columns`, `List item` (but not at the
start, `LIST_ITEM_WITHOUT_LINE`), `Cell` and as the element of an `Item` or an `Area`. A `Markdown` does not generate them.

## Accessibility

`Alt` is required (`BARCODE_WITHOUT_ALT`, with the line of the `Barcode`): with it, the code is a figure (`Figure`) that a
screen reader announces with that text. Inside a `Page overlay` it is always decorative, like an image, and in the
repeated rows of a table it is only read the first time.

> **Decision:** `Alt` is required. *Rejected alternative:* a decorative `Barcode` without `Alt`, like an `Image`: a code
> carries information that only its data says, and whoever cannot see it has no other way of knowing what it is.

## Output

In the PDF, each row of modules is rectangles (one for each run of consecutive dark modules), all in a single path that is
filled once, so that no line is seen between neighboring modules. It is a vector drawing, sharp at any size and at any
print resolution. The same data in the same format always gives the same modules, and the same PDF.

## What is not supported

- Other formats: Aztec, Code 39, EAN-8, UPC-A, or GS1 data in a QR code.
- A rectangular Data Matrix, and choosing the size of the symbol.
- Choosing the columns, the rows or the correction level of a PDF417, or its compact and Macro variants.
- Checking the format of the value of each GS1 identifier of variable length, and a parenthesis inside a value.
- The readable text anywhere but under the bars, with another alignment or in a matrix code: it goes in a `Text`.
- The `>` sign that some EAN-13 codes have on the right to keep the quiet zone.
- Choosing the version or the mask of the QR code.
- A different quiet zone on each side: `Quiet zone` is the same on both.
- A minimum height of the bars: a low barcode is drawn without a warning.
- Changing the minimum module from the document.
- A logo in the center of a QR code.
- Checking the contrast of the modules with the background.
