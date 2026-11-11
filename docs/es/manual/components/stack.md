# `Stack`

Un `Stack` coloca sus ítems **en fila** (o en columna) y reparte entre ellos el espacio que sobra o que falta. Sirve para
poner cosas una al lado de otra: un logotipo y unos datos, tres cifras, un precio a la derecha de un plato.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Cifras
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For style: figure
            Box:
                Padding: 10
                Background: #F1EFE8
                Corner radius: 4
            Typography:
                Align: center
    Section:
        Stack:
            Gap: 12
            Item:
                Grow: 1
                Base size: 0
                Text: 12 % más de ventas
                    Style: figure
            Item:
                Grow: 1
                Base size: 0
                Text: 3 tiendas nuevas
                    Style: figure
            Item:
                Grow: 1
                Base size: 0
                Text: 98 % de clientes satisfechos
                    Style: figure
        Stack:
            Horizontal: justify
            Box:
                Margin: 20 0 0 0
            Item:
                Text: Risotto de setas
            Item:
                Text: 14,50 €
```

Arriba, tres ítems del mismo ancho. Abajo, un plato a la izquierda y su precio a la derecha.

## `Item`

Cada hijo de un `Stack` va dentro de un `Item`, que envuelve **exactamente un elemento**. El `Item` dice cómo participa en el
`Stack`, y el resto (tamaño, márgenes, `Id`, estilo) va en el elemento de dentro.

| Propiedad del `Item` | Por defecto | Qué hace |
|---|---|---|
| `Grow` | `0` | Qué parte del espacio que sobra recibe. Con `Grow: 1` en todos, se reparte a partes iguales |
| `Shrink` | `1` | Qué parte del espacio que falta cede |
| `Base size` | `auto` | El tamaño de partida: `auto` es su tamaño (el declarado o el de su contenido) y `0` hace que todo el espacio se reparta según `Grow` |
| `Horizontal`, `Vertical` | el del `Stack` | Su alineación en el otro eje: `Vertical` en un `Stack` en fila, `Horizontal` en uno en columna. En la dirección del `Stack` es un error: ahí el ítem crece o encoge con `Grow` y `Shrink` |
| `Layer` | `auto` | Qué se pinta encima ([Capas y giro](../common/layers-and-rotation.md)) |

## Propiedades del `Stack`

| Propiedad | Valores | Por defecto | Qué hace |
|---|---|---|---|
| `Direction` | `horizontal`, `vertical` | `horizontal` | En fila (`horizontal`) o en columna (`vertical`) |
| `Gap` | 1 o 2 medidas | `0` | El hueco entre ítems, y entre líneas si hay varias. Con `Wrap items`, puede tener dos: entre filas y entre columnas |
| `Wrap items` | `true`, `false` | `false` | Si los ítems que no caben pasan a otra línea |
| `Horizontal` | `left`, `right`, `center`, `justify`, `distribute`. En columna: `left`, `right`, `center`, `stretch` | `left`. En columna: `stretch` | En fila, dónde va lo que sobra: al principio, al final, a los dos lados, entre los ítems (`justify`) o en huecos iguales, también en los bordes (`distribute`). En columna, cómo se alinean los ítems: `stretch` los estira al ancho del `Stack` |
| `Vertical` | `top`, `bottom`, `center`, `stretch`, `baseline`. En columna: `top`, `bottom`, `center`, `justify`, `distribute` | `stretch`. En columna: `top` | Al revés que `Horizontal`. En fila, cómo se alinean los ítems (`stretch` los estira al alto de la fila y `baseline` los alinea por la línea base de su texto). En columna, dónde va lo que sobra |
| `Line distribution` | `top`, `bottom` (en horizontal) o `left`, `right` (en vertical), `center`, `justify`, `distribute`, `stretch` | `stretch` | Con `Wrap items`, cómo se reparten las líneas |

## Cómo se reparte el espacio

1. Cada ítem parte de su `Base size`: su tamaño declarado o, si no tiene, lo que mide su contenido sin partir líneas.
2. Si sobra espacio, se reparte según `Grow`. Si falta, los ítems ceden según `Shrink` (y según su tamaño: uno grande cede
   más).
3. Un ítem nunca se estrecha más que su palabra más larga, salvo que declare `Min width: 0`.
4. Si aun así no caben, es un error.

El reparto de lo que sobra solo actúa si ningún ítem crece. En fila, los ítems se estiran al alto de la fila
(`Vertical: stretch`), salvo las imágenes, que conservan su proporción.

## Textos de distinto tamaño en una fila (`Vertical: baseline`)

Un rótulo pequeño junto a una cifra grande se alinea por la línea base del texto, la línea sobre la que se apoyan las
letras, con `Vertical: baseline` en un `Stack` en fila:

```
Document (dev.stxt.pdf):
    Language: es
    Title: Total
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
    Section:
        Stack:
            Gap: 6
            Vertical: baseline
            Item:
                Text: TOTAL
            Item:
                Text: 1.234,56
                    Typography:
                        Size: 24
                        Bold: true
            Item:
                Text: EUR
```

- Cuenta la primera línea de cada ítem, también si el texto está dentro de un `Block`.
- Un ítem sin texto, como una imagen, apoya su borde inferior en esa línea.
- Un `Item` puede pedirlo solo para él (`Vertical: baseline` en el `Item`) o salirse de él (`Vertical: top`).
- Solo vale en un `Stack` en fila: en uno en columna, en un `Grid` o en una celda es un error.

## Cosas a tener en cuenta

- `Grow` en un `Stack` en columna necesita que el `Stack` tenga un alto en puntos o en milímetros. Si no, no hay nada
  que repartir y es un error.
- Los ítems no admiten `Margin: auto`: se alinean con `Horizontal` y `Vertical`.
- Entre páginas, un `Stack` en columna se corta entre ítems, y uno en fila, entre líneas. Una fila que no cabe en una página
  entera se parte con sus ítems a la vez.

Más detalle en la [referencia](../../reference/components/stack.md).
