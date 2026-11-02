# `Figure`

Una `Figure` es una **figura con pie**: una imagen, un código, una tabla o un bloque con su `Caption`. El motor la numera
(«Figura 1.», «Tabla 1.»), pone el número delante del pie, lo escribe donde se le pide con `{number of: id}` y la lista en
un índice de figuras o de tablas.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Catálogo
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Paragraph
            Box:
                Margin: 0 0 8 0
        For element: Figure
            Typography:
                Number label: Figura {number}.
            Box:
                Margin: 6 0 14 0
        For element: Caption
            Typography:
                Size: 8.5
                Color: #5E7479
            Box:
                Margin: 4 0 0 0
        For element: TOC 1
            Box:
                Margin: 0 0 3 0
    Section:
        Paragraph: Las dos piezas de la colección se ven en las figuras {number of: silla} y {number of: mesa}.
        Figure:
            Id: silla
            Caption: Silla de nogal con asiento de lana.
            Image: examples/catalog/silla.jpg
                Alt: Silla de nogal con asiento de lana
                Box:
                    Width: 160
        Figure:
            Id: mesa
            Caption: Mesa de comedor de nogal, para seis.
            Image: examples/catalog/mesa.jpg
                Alt: Mesa de comedor de nogal
                Box:
                    Width: 160
        Figure:
            Caption: Medidas de las piezas.
            Table:
                Column sizes: * * *
                Header rows:
                    Row:
                        Cell: Pieza
                        Cell: Ancho
                        Cell: Alto
                Body:
                    Row:
                        Cell: Silla
                        Cell: 45 cm
                        Cell: 82 cm
                    Row:
                        Cell: Mesa
                        Cell: 180 cm
                        Cell: 75 cm
        Paragraph: Índice de figuras
        Table of contents:
            Of: figures
```

## Propiedades

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `Caption position` | `bottom`; `top` con una tabla | Dónde va el pie: `top`, encima del contenido, o `bottom`, debajo |
| `Figure counter` | `tables` con una tabla; `figures` con lo demás | Con qué contador se numera: `figures` o `tables`. Las figuras y las tablas se numeran aparte |
| `Alt` | obligatorio con un `Block` | El texto alternativo de la figura entera, solo cuando contiene un `Block`. Con una imagen, el `Alt` va en la imagen |
| `In table of contents` | `true` | Con `false`, la figura no sale en los índices, pero sigue numerada |

Admite además `Id`, `Style`, `Box`, `Typography`, `Break`, `Side` y `Clear`. `Numbered: false` (en su `Typography` o en una
regla) la deja sin número; `Number label` y `Number format` escriben el número como en los encabezados
([Encabezados](heading.md#numeración-numbered)).

## Cómo se comporta

- Contiene **exactamente un elemento** (`Image`, `Barcode`, `Table` o `Block`) y un `Caption`. Para una figura con varias
  imágenes, se agrupan en un `Block`, y el `Alt` del conjunto va en la `Figure`.
- El `Caption` es un texto como un `Text`: una línea en el valor o un bloque `Content >>`, con `Markup: markdown` si lleva
  formato. Se escribe donde se quiera dentro de la `Figure`: el motor lo coloca debajo del contenido, o encima de una
  tabla.
- El número va delante del pie, con un espacio: «1 Esquema». Con `Number label: Figura {number}.` en
  `For element: Figure`, «Figura 1. Esquema». Las tablas cuentan aparte: la primera `Figure` con una `Table` es la tabla 1.
- `{number of: id}` escribe el número de una figura con `Id`, sin la etiqueta: «la figura 2».
- La figura **no se parte** entre páginas: si no cabe, pasa entera a la siguiente. Una tabla dentro sí se parte, y el pie
  se queda con su primera parte (o con la última, si va debajo).
- Un `Table of contents` con `Of: figures` lista las figuras, y con `Of: tables`, las tablas, con el texto de su pie y su
  página. Se estila con `For element: TOC 1`.
- La imagen o el código de una figura necesitan `Alt`: una figura con pie es contenido y un lector de pantalla la lee con
  su pie.
- Va donde va un elemento en flujo, también en un `Item` o un `Area` y como caja lateral con `Side` (con `Width`), pero no
  en un `Page overlay`.

Más detalle en la [referencia](../../reference/components/figure.md).
