# Estilos: `Styles`

Las reglas de `Styles` dan propiedades a muchos elementos a la vez, para no repetirlas en cada uno. `Styles` va en el
`Document`, antes de las secciones, o en un [fichero aparte](#estilos-en-un-fichero-aparte).

```
Styles:
    For element: Document
        Typography:
            Font: IBM Plex Sans
            Size: 10
    For element: Paragraph
        Box:
            Margin: 0 0 8 0
    For style: note
        Box:
            Background: #F1EFE8
            Padding: 8
    For id: title
        Typography:
            Size: 20
            Bold: true
```

## Reglas

| Regla | A qué se aplica |
|---|---|
| `For element: Document` | A todo el documento: aquí va la tipografía base. Solo admite `Typography` |
| `For element: Paragraph` (o cualquier otro elemento) | A todos los elementos de ese tipo |
| `For style: note` | A los elementos con `Style: note` (un elemento puede tener varios estilos: `Style: note wide`) y a las partes de un texto escritas `[texto]{note}` ([Formato del texto](text-formatting.md#partes-con-otro-color-u-otra-fuente)) |
| `For id: title` | Al elemento con `Id: title` |
| `For element: Header cell`, `Body cell`, `Footer cell`, `Odd row cell`, `Even row cell` | A las celdas de una tabla según dónde están ([Table](../components/table.md)) |
| `For column: 3` (o `For column: 2 4`) | A las celdas que empiezan en esa columna (o columnas) de una tabla |
| `For element: Code block` | A los bloques de código de un `Markdown` |
| `For element: TOC 1` a `TOC 6` | A las entradas de un índice, por nivel ([Table of contents](../components/table-of-contents.md)) |

Cada regla admite los grupos `Box`, `Typography` y `Break`, y las [propiedades propias de un elemento](#propiedades-de-un-elemento)
que dicen cómo se ve o cómo reparte su contenido.

## Propiedades de un elemento

Una regla también da las propiedades propias de un elemento que dicen cómo se ve o cómo reparte su contenido: el `Gap` de
un `Stack`, las `Column sizes` de un `Grid` o de una `Table`, el `Fit` de una `Image`, el `Marker` de una `List`... En
esta carta, cada plato es un `Stack` con `Style: dish`, y su `Gap` va una sola vez, en la regla:

```
Document (dev.stxt.pdf):
    Language: es
    Title: Carta
    Page:
        Paper: A5
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For style: dish
            Gap: 12
            Box:
                Margin: 0 0 6 0
        For style: name
            Grow: 1
        For style: price
            Typography:
                Bold: true
    Section:
        Stack:
            Style: dish
            Item:
                Text: Croquetas de gamba roja
                    Style: name
            Item:
                Text: 9,50 €
                    Style: price
        Stack:
            Style: dish
            Item:
                Text: Alcachofas confitadas con jamón
                    Style: name
            Item:
                Text: 12,00 €
                    Style: price
```

- **Solo lo que el elemento admite.** Una regla que da a un elemento una propiedad que no tiene, como un `Gap` a un
  `Text`, es un error (`STYLE_NOT_ALLOWED`) con la línea de la regla.
- **`Grow`, `Shrink` y `Base size` son del `Item`.** Los da la regla del elemento que va dentro del `Item` (aquí,
  `For style: name`). Si el `Item` también los declara, gana el `Item`.
- **`Horizontal` y `Vertical`, en una regla, son las del `Stack` o el `Grid`.** La alineación de un ítem se escribe en
  su `Item`.
- **Lo que identifica a un elemento o es su contenido no va en reglas:** `Id`, `Link`, `Alt`, `Offset`, `Rotate`, los
  números de una lista (`Start`, `Number`), `Span`, `Row header` y el texto. La lista completa, por elemento, está en la
  [referencia](../../reference/common/styles.md#propiedades-de-elemento).
- El `Marker` de una `List` y las `Column sizes` de una `Table` son obligatorios, y pueden venir de una regla:
  `For element: List` con `Marker: bullet` da viñetas a todas las listas del documento.

## Quién gana

Si varias cosas dan la misma propiedad, gana, de más a menos fuerte:

1. Lo que declara el propio elemento.
2. Su regla `For id`.
3. Sus reglas `For style` (entre varias, la que va **después** en `Styles`).
4. En las celdas de una tabla, las reglas de su fila, de su columna y de su posición.
5. Su regla `For element`.
6. Lo que hereda de su padre (solo `Typography`).
7. El valor por defecto.

Las reglas se mezclan propiedad a propiedad: si una regla da `Size` y el elemento declara `Bold`, se aplican las dos. Y
una regla gana a la herencia: con `For element: Text` y `Size: 10`, un texto mide 10 aunque su padre diga `Size: 20`.

Un nombre de `Style` que no tiene su regla `For style` es un error, para que una errata no pase sin verse. Una regla que
no se aplica a nada no es un error, para poder reutilizar el mismo `Styles` en muchos documentos.

## Colores con nombre

Un color que se repite (el de la marca, el de los filetes) se declara una vez en `Styles` con `Named color`: el nombre y
el color, separados por un espacio. Después, el nombre se escribe donde iría el color, en las reglas y en los elementos:
`Color`, `Border color`, `Background` (también dentro de un degradado) y `Separator color`.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Colores con nombre
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        Named color: brand #12315A
        Named color: mist #EEF3F8
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 11
                Color: brand
        For style: panel
            Box:
                Background: mist
                Padding: 10
                Border: 0 0 0 3
                Border color: brand
    Section:
        Heading 1:
            Text: Informe del trimestre
        Text: Las ventas han crecido un 12 % respecto al trimestre anterior.
            Style: panel
```

El nombre va en minúsculas, empieza por una letra y une sus palabras con un guion (`brand`, `dark-gray`, `gray-2`). Un
nombre que no está declarado es un error (`COLOR_UNKNOWN`), como lo es declarar dos veces el mismo (`COLOR_DUPLICATE`).
Cambiar la paleta de un documento es cambiar estas líneas.

## La tipografía base

`Font` y `Size` no tienen valor por defecto, así que todo documento los declara, normalmente en `For element: Document`. Todo
lo demás lo hereda de ahí.

## Estilos en un fichero aparte

Las reglas y los colores con nombre pueden ir en un documento propio, con raíz `Styles`, para compartirlos entre varios
documentos o dejar el documento solo con el contenido:

```
Styles (dev.stxt.pdf):
    Named color: brand #12315A
    For element: Document
        Typography:
            Font: IBM Plex Sans
            Size: 10
    For style: panel
        Box:
            Padding: 10
            Border: 0 0 0 3
            Border color: brand
```

El documento no dice dónde está su fichero de estilos: se pasa al convertirlo ([Uso desde Java](java-usage.md)). Si el
documento tiene también su `Styles`, en cada nivel de la lista de arriba gana el del documento, así que sacar el `Styles`
a un fichero no cambia nada. Lo mismo vale para los colores: si el documento declara un `Named color` con el nombre de
uno del fichero, gana el suyo, y todas las reglas que usan ese nombre, también las del fichero, toman su color. Un error en el fichero de estilos empieza por `line 8 of the styles:`, también si se ve al
maquetar, como una fuente que no existe o una caja que no cabe.

Se pueden pasar varios ficheros de estilos, en un orden: los de la empresa y después los de las facturas, por ejemplo.
Si dos declaran lo mismo, gana el que se pasa después, y el documento gana a todos. Con los colores pasa igual: vale el
`Named color` del último fichero que declara ese nombre. Con varios ficheros, el error dice de cuál es la línea:
`line 8 of styles 2:` es la línea 8 del segundo.

Más detalle en la [referencia](../../reference/common/styles.md).
