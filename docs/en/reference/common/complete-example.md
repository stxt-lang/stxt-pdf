# Complete example

A one-page A4 document with a repeated header and footer, a title that is not separated from the body, the body in two
columns and an overlaid stamp. It is the file [`test/spec/report.stxt`](../../../../test/spec/report.stxt) and the case
[`05-report`](../../../../test/golden/05-report) of the reference tests (with `Width` and `Height` instead of `Paper`), which
checks each measurement below.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Informe
    Page:
        Paper: A4
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Page overlay
            Box:
                Height: 20
            Typography:
                Size: 8
        For element: Paragraph
            Typography:
                Align: justify
        For style: panel
            Box:
                Background: #F1EFE8
                Padding: 10
            Typography:
                Align: left
            Break:
                Keep together: true
        For id: title
            Box:
                Margin: 0 0 16 0
            Typography:
                Bold: true
                Size: 20
                Line height: 24
            Break:
                Keep with next: true
    Section:
        Page overlay:
            Id: header
            Offset: -30 0 auto 0
            Typography:
                Color: #5F5E5A
            Text: Informe trimestral
        Page overlay:
            Id: footer
            Offset: auto 0 -30 0
            Text: Página {page} de {pages}
                Typography:
                    Align: right
        Text: Resultados del trimestre
            Id: title
        Grid:
            Id: body
            Column sizes: 150 *
            Gap: 20
            Area:
                Text:
                    Id: side
                    Style: panel
                    Content >>
                        Resumen de las cifras clave
                        del trimestre.
            Area:
                Block:
                    Id: main
                    Paragraph:
                        Content >>
                            Primer párrafo del informe. Los saltos de línea simples
                            dentro del bloque equivalen a espacios.
                    Paragraph: Cada párrafo es un Paragraph.
        Overlay:
            Id: stamp
            Offset: 0 0 auto auto
            Box:
                Width: 80
                Height: 30
                Border: 1
                Border color: #A32D2D
            Layer: 1
            Text: BORRADOR
                Typography:
                    Align: center
```

**How it is laid out:**

- **Page area:** `Paper: A4` measures 595 × 842, so 595 − 2·40 = **515 pt** of width and 842 − 2·40 = **762 pt** of height remain, from
  (40, 40).
- **Fonts:** the `For element: Document` rule declares `Font` and `Size`, which all boxes inherit.
- **Rules** ([Styles](styles.md)): `For element: Page overlay` gives the header and the footer a height of 20 and a size of 8,
  which their `Text` elements inherit. `For element: Paragraph` justifies the paragraphs of the body, those of `main`. The
  standalone texts (title, header, footer and stamp) are `Text` and it does not reach them. The footer and the stamp are
  aligned with their own `Align`, and the `panel` style of `side`, to the left. The `For id: title` rule carries all the
  style of the title, which in the document only declares its text and its `Id`. The result is the same as if each element
  declared those properties.
- **`header`** (`Page overlay`): left and right 0 with `auto` width, so it stretches to 515 pt.
  Top −30 places it at y = 40 − 30 = **10**, inside the top margin. It is repeated on each
  page.
- **`footer`** (`Page overlay`): bottom −30 with height 20 gives top = 762 + 30 − 20 = 772, that is,
  y = 40 + 772 = **812**, inside the bottom margin. `{page}` and `{pages}` are replaced
  on each page.
- **`title`**: first in-flow element, at y = **40**, with a 24 pt line. Its bottom
  margin leaves the body at y = 40 + 24 + 16 = **80**. With `Keep with next: true` in its `Break`, it is never left
  alone at the end of a page.
- **`body`** (`Grid`): 515 − 20 of gap = 495 for tracks. The fixed column measures **150** and the
  fraction receives **345**. The two `Area` elements have no value and are placed automatically, one in
  each column: `side` starts at x = 40 and `main` at x = 40 + 150 + 20 = **210**. `side` is not
  split (`Keep together: true`). `main` can be split across pages respecting widows and
  orphans (2 by default).
- **`stamp`** (`Overlay`): there is no positioned ancestor, so its reference container is the
  page area. Top 0 and right 0 with width 80 anchor it to the top right corner:
  x = 40 + 515 − 80 = **475**, y = **40**. With `Layer: 1` it is painted above the title.
