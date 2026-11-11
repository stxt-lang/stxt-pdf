# `Figure`

Una `Figure` es una **figura con pie**: un bloque con exactamente un elemento (`Image`, `Barcode`, `Table` o `Block`) y
un `Caption`, el pie, que va debajo del contenido o, en una tabla, encima. El motor la numera con un contador propio,
escribe el número delante del pie («1 Esquema de conexión», o «Figura 1. Esquema de conexión» con `Number label`), lo da
con `{number of: id}` y la lista en un `Table of contents` con `Entries: figures` o `Entries: tables`
([Table of contents](table-of-contents.md#qué-lista-of)). Admite `Id`, `Style`, `Box`, `Typography`, `Break`,
`In table of contents`, `Side` y `Below side boxes`, y las propiedades de abajo. No admite `Link`: el enlace va en el elemento de
dentro.

```
Styles:
    For element: Figure
        Typography:
            Number label: Figura {number}.
        Box:
            Margin: 8 0 12 0
    For element: Caption
        Typography:
            Size: 8
            Color: #5E7479
        Box:
            Margin: 3 0 0 0
Figure:
    Id: wiring
    Caption: Esquema de conexión de los cuatro bornes.
    Image: examples/manual/es/wiring.svg
        Alt: Esquema de conexión de los bornes L, N, C y NO con los colores de los cables
        Box:
            Width: 100%
Paragraph: Los cables se conectan como muestra la figura {number of: wiring}.
```

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Caption position` | `top`, `bottom` | `bottom`; `top` con una `Table` | Dónde va el pie: encima o debajo del contenido, se escriba donde se escriba ([el pie](#el-pie-caption)). |
| `Figure counter` | `figures`, `tables` | `tables` con una `Table`; `figures` con lo demás | Con qué contador se numera y en qué índices sale ([numeración](#numeración)). |
| `Alt` | texto no vacío | sin valor; obligatorio con un `Block` | El texto alternativo de la figura entera, solo con un `Block` dentro: con una imagen, el `Alt` va en la imagen ([accesibilidad](#accesibilidad)). |
| `In table of contents` | `true`, `false` | `true` | Si la figura tiene entrada en los índices de figuras o de tablas. Con `false` sigue numerada. |

`Caption position`, `Figure counter` e `In table of contents` también pueden venir de una regla de `Styles`
([Propiedades de elemento](../common/styles.md#propiedades-de-elemento)); `Alt`, no.

`Numbered`, `Number format` y `Number label` son de `Typography` y valen también para las figuras ([numeración](#numeración)).

## El pie (`Caption`)

El `Caption` es un texto, en el valor (una línea) o en un bloque `Content >>`, como un `Text` ([Text](text.md)): admite
`Id`, `Style`, `Box` y `Typography`, hereda la tipografía de la `Figure`, y `Markup: markdown` le da tramos con formato.
`For element: Caption` estila todos los pies; una `Figure` tiene exactamente uno (el schema lo exige).

- **Dónde va.** Se escribe donde se quiera entre los hijos de la `Figure`, y el motor lo coloca **debajo** del contenido
  o, si el contenido es una `Table`, **encima**; `Caption position` lo cambia. Es la convención editorial: el pie de una
  figura va debajo y el título de una tabla, encima.
- **El número va delante.** Como en un encabezado ([Encabezados](heading.md#numeración-numbered)): el número, o su
  `Number label`, y un espacio se escriben delante del texto del pie, como si el autor los hubiera escrito. Miden lo que
  escriben, el pie puede pasar a otra línea, y salen en los índices y en lo que se lee.

> **Decisión:** el pie se coloca por `Caption position` y no por el orden en que se escribe, con un valor por defecto que
> depende del contenido (debajo, y encima de una tabla). *Alternativa descartada:* el orden del documento, que obliga a
> acordarse de la convención en cada figura.

## Numeración

Una `Figure` **lleva número por defecto**: una figura con pie casi siempre lo lleva. Lo quita `Numbered: false` declarado
en su `Typography` o en una regla que le llegue (`For element: Figure`, `For style` o `For id`); lo que hereda de una caja
de fuera no cuenta, así que un documento con `Numbered: false` en `For element: Document` sigue numerando sus figuras.
Una figura sin número no cuenta: la siguiente sigue el contador.

- **Dos contadores.** Las figuras y las tablas se numeran aparte: `Figure counter: figures` o `tables`. Sin declararlo,
  una `Figure` con una `Table` cuenta entre las tablas, y las demás, entre las figuras. Cada contador sube en orden del
  documento, a través de las secciones, y no vuelve a empezar, salvo en una sección que numera por capítulo
  ([abajo](#por-capítulo-figure-numbering)).
- **El formato y la etiqueta.** `Number format` escribe el número en cifras, letras o romanos, y `Number label` pone lo
  que va delante del pie, con `{number}` donde va el número: `Number label: Figura {number}.` da «Figura 3. Esquema».
  Son de `Typography`, se heredan y se dan en `For element: Figure` (o en una regla de las tablas, con `For style`). Sin
  `Number label`, el pie empieza por el número y un espacio, como un encabezado.
- **Se puede referir.** `{number of: id}` de una figura numerada da su número, sin la etiqueta y con el del capítulo
  si lo lleva: «la figura {number of: wiring}» escribe «la figura 3», o «la figura 2.3»
  ([Referencias](../common/references.md#number-of-id)). El de una figura sin número es `REFERENCE_NOT_NUMBERED`.

```
Styles:
    For element: Figure
        Typography:
            Number label: Figura {number}.
    For style: table
        Typography:
            Number label: Tabla {number}.
Figure:
    Caption: Vista frontal                # «Figura 1. Vista frontal»
    Image: examples/manual/es/thermostat.svg
        Alt: El termostato visto de frente
Figure:
    Style: table
    Caption: Referencia de los bornes     # «Tabla 1. Referencia de los bornes», encima de la tabla
    Table:
        Column sizes: 56 *
        Body rows:
            Row:
                Cell: L
                Cell: Fase
```

> **Decisión:** la figura va numerada por defecto, y solo el `Numbered: false` declarado en ella o en una regla suya la
> deja sin número; lo heredado no cuenta. `Numbered` se hereda para que una regla numere todos los encabezados de un
> nivel, y ese mismo valor heredado (falso casi siempre) dejaría sin número todas las figuras. *Alternativa descartada:*
> `Numbered` heredado como en los encabezados, que obliga a declararlo en cada documento con figuras.

> **Decisión:** sin `Number label`, el pie empieza por el número a secas, como el título de un encabezado, y la etiqueta
> («Figura», «Figure», «Tabla») se declara una vez en `For element: Figure`. *Alternativa descartada:* una etiqueta por
> defecto según el `Language`, que sería el primer texto en lenguaje natural que escribe el motor.

> **Decisión:** el contador lo decide el contenido, y `Figure counter` solo lo fuerza (un `Block` con una tabla dibujada
> que cuenta entre las tablas, una tabla que cuenta como figura). *Alternativa descartada:* un único contador, o uno
> declarado siempre.

### Por capítulo (`Figure numbering`)

`Figure numbering` es una propiedad de `Section` ([Document](document.md#section)): con `chapter`, las figuras y las
tablas de la sección vuelven a empezar en cada capítulo y llevan su número delante, con un punto: `2.3` es la tercera
figura del capítulo 2.

```
Styles:
    For element: Heading 1
        Typography:
            Numbered: true
    For element: Figure
        Typography:
            Number label: Figura {number}.
Section:
    Figure numbering: chapter
    Heading 1:
        Text: Instalación              # «1 Instalación»
    Figure:
        Caption: Vista frontal         # «Figura 1.1. Vista frontal»
        Image: examples/manual/es/thermostat.svg
            Alt: El termostato visto de frente
    Heading 1:
        Text: Conexión                 # «2 Conexión»
    Figure:
        Id: wiring
        Caption: Los cuatro bornes     # «Figura 2.1. Los cuatro bornes»
        Image: examples/manual/es/wiring.svg
            Alt: Esquema de conexión de los bornes L, N, C y NO con los colores de los cables
    Paragraph: Los cables se conectan como muestra la figura {number of: wiring}.     # «... la figura 2.1.»
```

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Figure numbering` | `continue`, `chapter` | `continue` | `continue` sigue los contadores del documento (`1`, `2`, `3`) y `chapter` vuelve a empezar en cada capítulo y escribe su número delante (`2.3`). Vale para los dos contadores. |

- **El capítulo** es el último encabezado numerado que no tiene otro numerado por encima: aquel cuyo número tiene una
  sola parte ([Encabezados](heading.md#numeración-numbered)). Con todos los niveles numerados es el `Heading 1`; con el
  título del documento en un `Heading 1` sin número y los capítulos en `Heading 2` numerados (`1`, `2`, `3`), el
  `Heading 2`. El número del capítulo se escribe como en su encabezado, con su `Number format`: las figuras de un anexo
  `A` son `A.1`, `A.2`. La parte de la figura lleva el `Number format` de la figura.
- **Fuera de un capítulo**, la figura lleva su número solo (`1`, `2`). Un encabezado sin número del nivel de los
  capítulos o superior acaba el capítulo y vuelve a empezar la cuenta: una introducción o unos créditos sin numerar
  tienen sus figuras `1`, `2`. Antes del primer capítulo, ese nivel es el del `Heading 1`.
- **Los dos contadores** vuelven a empezar a la vez: las tablas del capítulo 2 son `2.1`, `2.2`, como sus figuras.
- **Es de cada sección.** Las secciones con `continue` siguen los contadores del documento, que las figuras numeradas
  por capítulo no hacen subir: tres figuras en una sección preliminar y las de los capítulos dan `1`, `2`, `3`, `1.1`,
  `1.2`, `2.1`.
- El número completo es el que escribe el pie, el que da `{number of: id}` y el que sale en los índices de figuras y de
  tablas, y `{number}` de `Number label` es ese número: `Figura {number}.` da «Figura 2.3.».

> **Decisión:** el capítulo es el encabezado numerado más alto, sea del nivel que sea, y no siempre el `Heading 1`: un
> documento con el título en un `Heading 1` sin número numera sus capítulos en `Heading 2`, y sus figuras tienen que
> poder seguirlos. *Alternativa descartada:* el `Heading 1` numerado, más corto de explicar, que deja sin numeración
> por capítulo a esos documentos.

> **Decisión:** `Figure numbering` es de la `Section`, como `Page numbering` y `Footnote numbering`, y vale para las
> figuras y las tablas a la vez. *Alternativa descartada:* una propiedad de `Typography`, heredada como `Numbered`, que
> permite mezclar en un documento figuras por capítulo y seguidas, sin uso.

## El contenido

Una `Figure` contiene **exactamente un elemento** además de su `Caption`: una `Image`, un `Barcode`, una `Table` o un
`Block` (`FIGURE_CONTENT`, con la línea del segundo elemento o la de la `Figure` si no hay ninguno). Un `Block` sirve para
una figura compuesta: dos fotos en un `Stack`, un dibujo con `Overlay` encima, una tabla con una nota debajo.

El contenido y el pie se apilan en flujo de bloque, como en un `Block` ([Block](block.md)): el tamaño, los márgenes y el
`Alt` de la imagen van en la `Image`; el `Box` de la `Figure` da los márgenes, el fondo y el borde del conjunto, y su
`Typography`, la del pie.

## Cortes

Una `Figure` con una imagen, un `Barcode` o un `Block` **no se parte**: el motor la mantiene unida como si declarara
`Keep together: true` ([Cortes](../common/breaks.md)), que se relaja como los declarados cuando no hay un corte mejor. Una
`Figure` con una `Table` deja que la tabla se parta como siempre, y el pie va con su primera parte si está encima y con la
última si está debajo (`Keep with next` o `Keep with previous`, sin declararlos). `Break` se admite en la `Figure` para
lo demás: `Before`, `After`, `Keep with next`...

> **Decisión:** la figura no se parte, salvo la tabla que contiene, y el pie va pegado a la parte de la tabla que le
> toca. *Alternativa descartada:* dejar los cortes al autor, con un pie que puede quedar solo al principio de una página.

## Dónde va

Donde va un elemento en flujo, también como elemento de un `Item` o un `Area`, pero no dentro de un `Page overlay`, que
se repite en cada página, ni dentro de una `Footnote`. Con `Side` es una caja lateral, como un `Block`: necesita `Width` o
`Max width` (`SIDE_WITHOUT_WIDTH`) y el texto que la sigue corre a su lado, pie incluido
([Block](block.md#cajas-laterales-side)). `Below side boxes` la baja por debajo de las cajas laterales anteriores. Una `Figure` no
corre al lado de una caja lateral: va debajo de ella, como una tabla.

## Índices

Un `Table of contents` con `Entries: figures` lista las figuras que cuentan entre las figuras, y con `Entries: tables`, las que
cuentan entre las tablas, en orden del documento y con el texto de su pie (número o etiqueta incluidos), una guía y la
página, en entradas `TOC 1` clicables ([Table of contents](table-of-contents.md#qué-lista-of)). `In table of contents:
false` deja una figura fuera sin quitarle el número.

## Referencias y enlaces

Una `Figure` con `Id` se refiere con `{number of: id}` (su número) y `{page of: id}` (su página), y `Link: #id` lleva a
ella ([Referencias](../common/references.md), [Enlaces](../common/links.md)).

## Errores

| Código | Cuándo |
|---|---|
| `FIGURE_CONTENT` | Una `Figure` sin exactamente un elemento además de su `Caption` |
| `FIGURE_WITHOUT_ALT` | La `Image` o el `Barcode` de una `Figure` sin `Alt`, o una `Figure` con un `Block` sin `Alt` propio |
| `FIGURE_PROPERTY_WITHOUT_EFFECT` | `Alt` en una `Figure` con una imagen (el `Alt` va en la imagen) o con una `Table` (se lee sola) |
| `SIDE_WITHOUT_WIDTH` | Una `Figure` con `Side` sin `Width` ni `Max width` |
| `REFERENCE_NOT_NUMBERED` | `{number of: id}` de una `Figure` con `Numbered: false` |

## Accesibilidad

Una figura con pie es **contenido** y no decoración: la `Image` o el `Barcode` de dentro llevan `Alt` obligatorio, y una
`Figure` con un `Block`, el suyo, que describe el conjunto. En el PDF etiquetado la figura es un `Figure` con ese `Alt` y
su pie en un `Caption`; con una `Table`, la tabla lleva el `Caption` como primer o último hijo
([El PDF por dentro](../implementation/pdf.md#figuras)). Un lector de pantalla lee el pie con lo que describe. Las figuras
no tienen marcador del PDF.

## Lo que no hay

- Contadores propios del autor (listados, ecuaciones), y elegir el nivel de encabezado que reinicia la numeración
  por capítulo o el separador.
- Una etiqueta por defecto según el idioma: se declara con `Number label`.
- Subfiguras (`1a`, `1b`) con un pie cada una bajo un pie común.
- Pie en las imágenes de un `Markdown`, que siguen siendo `Image` sin pie.
- Marcadores del PDF para las figuras.
