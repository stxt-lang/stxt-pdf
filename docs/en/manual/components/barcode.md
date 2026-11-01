# `Barcode`

A QR code or a barcode that the engine generates from its data: the verification address of a certificate, the payment
of an invoice, the reference of an order or the code of a product. It is not a file from `images/`: the engine draws it
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
                        Format: code-128
                        Box:
                            Width: 180
                            Height: 36
                            Margin: 12 0 0 0
```

## Properties

| Property | What it does |
|---|---|
| *(value)* | The data: an address, a reference, a number. If it has several lines, it goes in a `Content >>` block |
| `Format` | `qr` (by default), a QR code; `code-128`, a barcode of letters, digits and signs; `ean-13`, the barcode of products |
| `Error correction` | Only in a QR code: how much of it can be damaged and still be read. `low`, `medium` (by default), `quartile` or `high`. The higher it is, the more modules for the same data |
| `Alt` | What it is and where it leads, for whoever cannot see it. Required |
| `Rotate` | Rotating it about its center, like an image ([Layers and rotation](../common/layers-and-rotation.md#rotating-a-box-rotate)) |

It also accepts `Id`, `Style`, `Link`, `Box`, `Break` (without `Keep together` or `Edges`: it is not split) and, of
`Typography`, only `Color`, which is the color of the modules. The background is that of `Box`; without it, what is
behind shows through.

## The data

- **QR:** any text, up to about 2300 characters with the `medium` correction (fewer if they carry accents). An address is
  the most common.
- **Code 128:** the printable ASCII characters (letters without accents, digits, the space and the signs), up to 80.
- **EAN-13:** 12 digits, and the engine adds the check digit, or the 13 with the check digit, which is checked.

Data that the format cannot encode is an error (`BARCODE_DATA`) that says what fails: an `ñ` in a Code 128, a check digit
that does not match, a text too long for a QR code.

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

## The size

- Without `Width` or `Height`, it measures its **natural size**, that of its modules: 0.5 mm each module of a QR code,
  and 0.33 mm wide by 22.85 mm high the bars of a Code 128 or an EAN-13. The blank margin that the reader needs around it
  (the quiet zone) is included.
- With `Width` or `Height`, the other side follows from the aspect ratio, as in an image. The usual thing is to give only
  `Width`.
- A QR code is always square: in a box that is not, it is drawn as large as it fits, centered. A barcode is stretched to
  the box: with `Width` and `Height`, the bars take up the whole height.
- It is not split across pages: if it does not fit in what is left, it moves whole to the next one.

For a phone to read it well, a printed QR code should not measure less than 2 cm (about 57 points), and the modules have
to contrast with the background: dark on light.

## Accessibility

`Alt` is required: a screen reader cannot read the code, and the alternative text says what it is and where it leads.
Inside a `Page overlay` it is decorative, like an image, and the text next to it should say the same.

More detail in the [`Barcode` reference](../../reference/components/barcode.md).
