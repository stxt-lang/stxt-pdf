# `Barcode`

A QR code or a barcode that the engine generates from its data: the verification address of a certificate, the payment
of an invoice, the reference of an order, the code of a product or the label of a shipment. It is not a file from `images/`: the engine draws it
with rectangles, sharp at any size. Otherwise it is an image like any other: it has its size, its margins, its border
and its background, and it is placed in the flow or in a `Stack`, a `Grid` or a positioned element.

```
Document (dev.stxt.pdf):
    Language: en
    Title: Ticket
    Page:
        Width: 420
        Height: 200
        Margin: 24
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
    Section:
        Stack:
            Gap: 20
            Vertical: center
            Item:
                Barcode: https://teatro.example/entradas/2025-1187
                    Alt: QR code of ticket 2025-1187
                    Box:
                        Width: 110
            Item:
                Grow: 1
                Block:
                    Text: Valley Theater
                        Typography:
                            Size: 16
                            Bold: true
                    Text: Saturday, November 15, 2025, 8:30 p.m.
                    Text: Stalls, row 7, seat 12
                    Barcode: TV-2025-1187
                        Alt: Barcode of ticket TV-2025-1187
                        Barcode format: code-128
                        Box:
                            Width: 180
                            Height: 36
                            Margin: 12 0 0 0
```

## Properties

| Property | What it does |
|---|---|
| *(value)* | The data: an address, a reference, a number. If it has several lines, it goes in a `Content >>` block |
| `Barcode format` | The kind of code: `qr` (by default), `data-matrix`, `gs1-data-matrix`, `pdf417`, `code-128`, `gs1-128` or `ean-13` ([The formats](#the-formats)) |
| `Error correction` | Only in a QR code: how much of it can be damaged and still be read. `low`, `medium` (by default), `quartile` or `high`. The higher it is, the more modules for the same data |
| `Quiet zone` | The blank modules on each side of the code, from 0 to 40. By default, those of the standard of each format ([The size](#the-size)) |
| `Readable text` | `true` puts the data, as text, under the bars of a `code-128`, a `gs1-128` or an `ean-13` ([The text under the bars](#the-text-under-the-bars)) |
| `Alt` | What it is and where it leads, for whoever cannot see it. Required |
| `Rotate` | Rotating it about its center, like an image ([Layers and rotation](../common/layers-and-rotation.md#rotating-a-box-rotate)) |

It also accepts `Id`, `Style`, `Link`, `Box`, `Break` (without `Keep together` or `Edges`: it is not split) and, of
`Typography`, only `Color`, which is the color of the modules (with `Readable text`, also the font of the text). The
background is that of `Box`; without it, what is behind shows through.

## The formats

| `Barcode format` | Where it is used | Data |
|---|---|---|
| `qr` | Addresses, payments, verifications: what a phone reads | Any text, up to about 2300 characters with the `medium` correction (fewer if they carry accents) |
| `data-matrix` | Industrial and postal labels, small parts | Any text, up to about 1500 characters |
| `gs1-data-matrix` | Medicines and medical devices | GS1 data |
| `pdf417` | Boarding passes, government forms, identity documents | Any text, up to about 1800 characters |
| `code-128` | References of orders, delivery notes, files | The printable ASCII characters (letters without accents, digits, the space and the signs), up to 80 |
| `gs1-128` | Logistics labels: packages, pallets, boxes | GS1 data, up to 48 characters |
| `ean-13` | Products | 12 digits, and the engine adds the check digit, or the 13 with the check digit, which is checked |

Data that the format cannot encode is an error (`BARCODE_DATA`) that says what fails: an `ñ` in a Code 128, a check digit
that does not match, a text too long for a QR code.

**GS1 data** is written as it is read on the label, with each application identifier in parentheses before its value.
The parentheses are not encoded, and the engine puts the function characters (`FNC1`) that separate the values:

```
Barcode: (01)09501101530003(17)261231(10)AB-123
    Alt: Code of the product 09501101530003, batch AB-123, expiry 2026-12-31
    Barcode format: gs1-data-matrix
    Box:
        Width: 50
```

`(01)` is the code of the product (GTIN), `(17)` the expiry date and `(10)` the batch; `(00)` is the number of a package
(SSCC). The engine checks the digits of the identifiers of fixed length and the check digit of `(00)`, `(01)` and `(02)`.

Data of several lines (the payment of an invoice, a contact card) goes in a `Content` block, with the lines as they are.
This is the QR code for payment by transfer that banking apps read:

```
Barcode:
    Alt: QR code to pay invoice 2025-0147 by transfer
    Box:
        Width: 80
    Content >>
        BCD
        002
        1
        SCT
        CAIXESBBXXX
        Taller Nogal S.L.
        ES9121000418450200051332
        EUR13360.82


        Invoice 2025-0147
```

## The text under the bars

`Readable text: true` writes the data under the bars of a `code-128`, a `gs1-128` or an `ean-13`, with the font, the size
and the color of the box:

```
Document (dev.stxt.pdf):
    Language: en
    Title: Product label
    Page:
        Width: 260
        Height: 190
        Margin: 20
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 9
    Section:
        Text: Extra virgin olive oil, 500 ml
            Typography:
                Bold: true
        Barcode: 8412345678905
            Alt: Code of the product 8412345678905
            Barcode format: ean-13
            Readable text: true
            Box:
                Margin: 8 0 0 0
```

- In an **EAN-13** the digits go as on a product: the first one at the left of the bars and the others under them, in two
  groups of six, between the guard bars, which are longer.
- In a **Code 128** and a **GS1-128** it is one centered line. That of the GS1-128 has the parentheses.
- The text adds its height to that of the code. If the box has `Height`, it takes it from the bars.
- The font is changed in the `Typography` of the `Barcode`: `Font`, `Size`, `Bold`, `Weight`, `Italic` and `Color`.
- If the text does not fit under its bars (a digit wider than its room in a small EAN-13, or the text of a Code 128 wider
  than the code), it is an error (`BARCODE_TEXT_OVERFLOW`): it is fixed with a smaller `Size` or a wider box.
- In a QR code, a Data Matrix or a PDF417 there is no text: if it is needed, it goes in a `Text` beside it.

## The size

- Without `Width` or `Height`, it measures its **natural size**, that of its modules: 0.5 mm each module of a QR code or
  a Data Matrix, 0.33 mm wide by 1 mm high those of a PDF417, and 0.33 mm wide by 22.85 mm high the bars of a Code 128, a
  GS1-128 or an EAN-13. The blank margin that the reader needs around it (the quiet zone) is included: 4 modules on each
  side in a QR code, 1 in a Data Matrix, 2 in a PDF417, 10 in a Code 128 and a GS1-128, and 11 on the left and 7 on the
  right in an EAN-13.
- `Quiet zone` replaces that zone with another number of modules on each side. In a box of the same width, less zone
  leaves more room for the bars, which come out wider: `Quiet zone: 5` in a Code 128 is what many barcode generators
  leave. With less zone than the standard, some readers may not read it.
- With `Width` or `Height`, the other side follows from the aspect ratio, as in an image. The usual thing is to give only
  `Width`.
- A QR code and a Data Matrix are always square, and a PDF417 keeps its proportion: in a box that does not have it, they
  are drawn as large as they fit, centered. A barcode of one row is stretched to the box: with `Width` and `Height`, the
  bars take up the whole height.
- It is not split across pages: if it does not fit in what is left, it moves whole to the next one.
- It cannot shrink until it cannot be read: if the box leaves the modules below 0.25 mm, or below 0.264 mm in an EAN-13,
  it is an error (`BARCODE_TOO_SMALL`) that says which size it needs. A QR code of a short
  address (33 modules with the quiet zone) needs at least 8.25 mm on each side, without counting the `Padding`. In a
  barcode only the width counts.

For a phone to read it well, a printed QR code should not measure less than 2 cm (about 57 points), and the modules have
to contrast with the background: dark on light.

## Accessibility

`Alt` is required: a screen reader cannot read the code, and the alternative text says what it is and where it leads.
Inside a `Page overlay` it is decorative, like an image, and the text next to it should say the same.

More detail in the [`Barcode` reference](../../reference/components/barcode.md).
