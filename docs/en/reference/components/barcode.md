# `Barcode`

A `Barcode` puts in the document a QR code or a barcode that the engine generates from its data. It is a generated
image: it does not come from a file but from its value, and the engine draws it with rectangles (the **modules**).
Otherwise it is laid out, broken across pages and painted like an [`Image`](image.md), with its `Box` (margins, padding,
border, background and corners) and its `Break` (without `Keep together` or `Edges`, because it is not split). It also
accepts `Id`, `Style`, `Link`, `Rotate` and, of `Typography`, only `Color`.

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
            Vertical: end
            Item:
                Block:
                    Text: Delivery note AL-2025-0388
                        Typography:
                            Bold: true
                    Barcode: AL-2025-0388
                        Alt: Barcode of delivery note AL-2025-0388
                        Format: code-128
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
| `Format` | `qr`, `code-128`, `ean-13` | `qr` | The kind of code ([Formats](#formats)). |
| `Error correction` | `low`, `medium`, `quartile`, `high` | `medium` | Only in a QR code: the part of the modules that can be damaged while the code can still be read, 7 %, 15 %, 25 % and 30 %. In another format it is an error. |
| `Quiet zone` | integer from 0 to 40 | that of the format | The light modules on each side of the code ([The modules](#the-modules)). |
| `Alt` | non-empty text | required | What it is and where it leads, for whoever cannot see it ([Accessibility](#accessibility)). |
| `Rotate` | see [Layers and rotation](../common/layers-and-rotation.md#rotation-in-flow-block-text-paragraph-and-image) | none | Rotates it about its center, like an `Image`. |
| `Side` | `left`, `right` | none | Makes it a side box, like an `Image`: the text that follows it runs beside it ([Block](block.md#side-boxes-side)). |
| `Clear` | boolean | `false` | Moves it below the side boxes before it ([Block](block.md#side-boxes-side)). |

`Format`, `Error correction`, `Quiet zone`, `Side` and `Clear` can also come from a rule of `Styles`
([Element properties](../common/styles.md#element-properties)); the data, `Alt` and `Rotate` cannot.

## Formats

| `Format` | What it is | Data |
|---|---|---|
| `qr` | QR code (ISO/IEC 18004): a square matrix of modules | Any text. If it is all ASCII it goes as is; if not, in UTF-8, with the ECI designator that tells the reader so. 2331 bytes fit with `Error correction: medium`, 2953 with `low` and 1273 with `high` (more if they are only digits or capital letters). |
| `code-128` | Code 128 (ISO/IEC 15417): a row of bars | The printable ASCII characters, from the space to `~`, from 1 to 80. |
| `ean-13` | EAN-13 (ISO/IEC 15420), the code of products | 12 digits, to which the engine adds the check digit, or 13 with the check digit, which has to match. |

Data that is empty, in the value and in `Content` at the same time, or that the format cannot encode is an error
(`BARCODE_DATA`, with the line of the `Barcode`), with what fails: the character that Code 128 does not accept, the check
digit that an EAN-13 should have or the bytes that do not fit in the QR code. `Error correction` in a `code-128` or an
`ean-13` is `BARCODE_PROPERTY_WITHOUT_EFFECT`.

The value of a line is taken without the blanks at its ends. In `Content` the lines are separated by a line feed (`LF`),
with their relative indent and the empty lines in between: it is what data with a format by lines needs, such as the QR
code for payment by transfer (EPC) or a contact card.

> **Decision:** the data of several lines goes in `Content`, like the text of a `Text`. *Rejected alternative:* an escape
> sequence in the value (`\n`), which STXT does not have and which would have to be invented just here.

> **Decision:** a QR code with data that is not ASCII goes in UTF-8 with its ECI designator, and one that is, without it.
> *Rejected alternative:* the designator always, which some old readers do not understand and which changes nothing in
> ASCII.

## The modules

The drawing of a `Barcode` is a matrix of modules, each one dark or light, which includes the **quiet zone**: the light
margin that the reader needs around the code. It is part of the content box.

| `Format` | Quiet zone | Natural module |
|---|---|---|
| `qr` | 4 modules on each side | 0.5 × 0.5 mm |
| `code-128` | 10 modules on each side | 0.33 mm wide; the bars, 22.85 mm high |
| `ean-13` | 11 modules on the left and 7 on the right | 0.33 mm wide; the bars, 22.85 mm high |

Those zones are the ones of each standard, and they apply if no other is declared. `Quiet zone: N` puts `N` light modules
on each side, in the three formats (in an EAN-13, the same on the left and on the right), from 0 to 40; outside that
range it is `VALUE_NOT_ALLOWED`. As the zone is part of the natural size, in a box of a given width fewer modules of zone
give wider modules: a Code 128 150 points wide with `Quiet zone: 5` has wider bars than with the 10 of the standard. With
`Quiet zone: 0`, the code reaches the edge of the content box, and the light margin has to come from the `Padding` with a
light background or from whatever is around. A zone smaller than that of the standard can prevent some readers from
reading it: the engine does not check it.

> **Decision:** `Quiet zone` counts modules, not millimeters, and works the same in the three formats. The standards give
> it in modules, and so the zone scales with the code. *Rejected alternative:* a length (`Quiet zone: 2mm`), which with the
> same value would give a different zone depending on the width of the box, and a property only for Code 128, which would
> leave the QR code and the EAN-13 without the same possibility.

> **Decision:** a zone smaller than that of the standard is accepted, without a warning. Many readers read with less
> (ZXing, when it draws a Code 128, leaves 5 modules on each side), and whoever prints the code is who knows what reads it.
> *Rejected alternative:* a minimum per format, which would prevent reproducing documents that are already printed with a
> smaller zone.

The dark modules are painted in the `Color` of the typography, its own or inherited (black if nobody declares it), and the
light ones are not painted: the background of the box (`Background`) shows or, without it, whatever is behind. Of its
`Typography`, a `Barcode` only accepts `Color`: any other property is `BARCODE_PROPERTY_WITHOUT_EFFECT`. What a rule of
`Styles` gives is not checked, as in any box without text.

The engine does not check the contrast of the modules with the background ([Contrast](../common/pdf.md#contrast) is only
for text): a light code on a light background cannot be read.

> **Decision:** the color of the modules is the `Color` of `Typography`, which is inherited and goes in the rules like that
> of the text. *Rejected alternative:* a property of its own (`Module color`), another name for the same thing.

> **Decision:** the engine generates the codes with ZXing (Apache 2.0), which is pure Java and does not draw: it gives the
> matrix, and the canvas paints it with rectangles. *Rejected alternative:* a QR encoder of its own (Reed-Solomon, masks and
> versions), some five hundred lines to maintain for the same thing.

## Size

A `Barcode` is sized like an [`Image`](image.md#sizing) whose natural size is that of its modules, with the quiet zone
(that of the standard or that of `Quiet zone`): a QR code of 25 modules (33 with the zone) measures 16.5 × 16.5 mm, and an EAN-13 (113 modules), 37.29 × 22.85 mm. With
`Width` or `Height`, the other side follows from that aspect ratio, and `Aspect ratio`, if declared, replaces it. The
intrinsic sizes are those of an image.

The content box does not have to have the aspect ratio of the code. If it does not:

- a **QR code** is drawn whole, as large as it fits and centered, with square modules (like `Fit: inside`);
- a **barcode** is stretched to the box (like `Fit: stretch`): the bars take up the whole height, and all the modules have
  the same width.

A `Barcode` does not accept `Fit`.

> **Decision:** the natural size comes from a fixed module: 0.5 mm in a QR code and the nominal size of EAN-13
> (0.33 × 22.85 mm) in the barcodes. So a `Barcode` without a size is seen and read, and it is measured in a `Stack` or a
> `Grid` like an image. *Rejected alternative:* requiring `Width` or `Height`, which leaves without an intrinsic size the one
> that declares it as a percentage.

> **Decision:** a QR code in a box that is not square is drawn centered, without an error as long as its modules do not go
> below the [minimum](#minimum-module). *Rejected alternative:* an error if `Width` and `Height` are not equal, which with
> percentages cannot be known before the layout.

### Minimum module

A code shrunk until it cannot be read is an error. Each format has a minimum module:

| `Format` | Minimum module | Where it comes from |
|---|---|---|
| `qr` | 0.25 mm | About three dots of a 300 dpi printer. ISO/IEC 18004 does not set one |
| `code-128` | 0.25 mm | The same criterion. ISO/IEC 15417 does not set one |
| `ean-13` | 0.264 mm | 80 % of the nominal module (0.33 mm), the smallest that ISO/IEC 15420 allows |

If the content box, once laid out, leaves narrower modules, it is an error (`BARCODE_TOO_SMALL`, with the line of the
`Barcode`) that gives the module that is left and the minimum size of the content box. In a QR code the shorter side counts
(in a wide and low box, the height); in a barcode, only the width, because stretching the bars in height does not change
the module. The quiet zone is part of the drawing: a QR code of 25 modules plus 4 of zone on each side (33) needs a content
box of 8.25 × 8.25 mm (23.39 pt), and the `Padding` does not count. It is checked with the final size, after pagination: in
a `Stack` or a `Grid` a code is measured several times, and only the last one counts.

Whoever converts can remove the check (`Check.BARCODE_SIZE`, [Using from Java](../implementation/api.md#checks)), and then
the code is drawn as small as its box.

> **Decision:** the minimum is fixed per format and there is no property in the document to change it; whoever needs to
> draw a smaller code removes the check when converting. *Rejected alternative:* a property of the `Barcode`
> (`Min module`), another value to write in each document for a rare case; and drawing the shrunk code without a warning,
> as up to 0.9.0, which gives a half result.

> **Decision:** 0.25 mm in QR and Code 128, the smallest module that a 300 dpi office printer draws with three dots.
> *Rejected alternative:* the minimums of GS1 (0.396 mm in a point-of-sale QR code and 0.495 mm in a logistics GS1-128),
> meant for checkout and warehouse scanners, which would rule out the codes that invoices and certificates carry.

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

- Other formats: Data Matrix, PDF417, Aztec, Code 39, EAN-8, UPC-A, or the function characters of GS1-128.
- The human-readable text under the bars of an EAN-13 or a Code 128: it goes in a `Text`.
- Choosing the version or the mask of the QR code.
- A different quiet zone on each side: `Quiet zone` is the same on both.
- A minimum height of the bars: a low barcode is drawn without a warning.
- Changing the minimum module from the document.
- A logo in the center of a QR code.
- Checking the contrast of the modules with the background.
