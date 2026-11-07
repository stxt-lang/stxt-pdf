# `Table`

Una `Table` coloca sus celdas en filas y columnas: sus filas (`Row`) van en una cabecera (`Header rows`, opcional), un
cuerpo (`Body`, obligatorio) y un pie (`Footer rows`, opcional), **en ese orden** (`TABLE_ORDER`), y cada fila tiene sus
celdas (`Cell`) de izquierda a derecha. La `Table` admite `Id`, `Style`, `Link`, `Box`, `Typography` y `Break`, y va
donde va un elemento en flujo, también dentro de una celda.

```
Styles:
    For element: Cell
        Box:
            Padding: 2 4
            Border: 0 0 0.5 0
Table:
    Column sizes: 120 * *
    Header rows:
        Row:
            Typography:
                Bold: true
            Cell: Concepto
            Cell: T1
            Cell: T2
    Body:
        Row:
            Cell: Ventas
            Cell: 100
            Cell: 120
    Footer rows:
        Row:
            Cell: Total
                Span: 1 3
```

| Propiedad | Valores | Por defecto | Elementos | Notas |
|---|---|---|---|---|
| `Column sizes` | lista de pistas | obligatorio | `Table` | Las columnas, como en `Grid` ([Grid](grid.md#dimensionado-de-pistas)), con el espacio repartido por su contenido ([Ancho de las columnas](#ancho-de-las-columnas)). |
| `Column align` | una alineación por columna: `left`, `center`, `right` o `justify` | sin valor | `Table` | La alineación por defecto de las celdas de cada columna. Tantas como `Column sizes` (`TABLE_COLUMN_ALIGN`). |
| `Gap` | 1 o 2 longitudes (filas columnas) | `0` | `Table` | Separación entre celdas, como en `Grid`. |
| `Split rows` | `true`, `false` | `true` | `Table` | Una fila que no cabe en lo que queda de página se parte en esa página, con sus celdas en paralelo; con `false`, pasa entera a la siguiente ([Cortes](#cortes)). |
| `Span` | `filas columnas`, dos enteros positivos | `1 1` | `Cell` | Filas y columnas que ocupa la celda. |
| `Row header` | `true`, `false` | `false` | `Cell` | La celda es la cabecera de su fila ([accesibilidad](#accesibilidad)). No vale en `Header rows` (`ROW_HEADER_IN_HEADER_ROWS`). |
| `Id` | texto | sin valor | `Row` | Destino de enlaces y de `{page of: id}`. Su regla `For id` da estilo a las celdas de la fila ([Filas](#filas)). |
| `Box` | `Background`, `Border`, `Border color`, `Corner radius` | sin valor | `Row` | El aspecto de la franja de la fila ([Filas](#filas)). Nada más (`ROW_BOX`). |

`Column sizes`, `Column align`, `Gap` y `Split rows` también pueden venir de una regla de `Styles` ([Propiedades de elemento](../common/styles.md#propiedades-de-elemento)); `Span` y
`Row header`, no. Sin `Column sizes`, propio o de una regla, es un error (`TABLE_COLUMN_SIZES_MISSING`). `Merge borders` (`true` o `false`, por defecto `false`)
va en el `Box` de la `Table`, así que también vale en una regla: el borde entre dos celdas se dibuja una sola vez, el más
ancho de los dos ([Bordes fusionados](#bordes-fusionados)).

> **Decisión:** los grupos de filas que se repiten se llaman `Header rows` y `Footer rows`, los nombres de las
> herramientas de maquetación. *Alternativa descartada:* `Header` y `Footer`, que en un procesador de textos son la
> cabecera y el pie de la página (aquí, un `Page overlay`) y dejarían ocupado ese nombre en todo el schema.

## Una tabla es un grid

`Column sizes` (obligatorio) y `Gap` son los de `Grid`: las columnas se dimensionan igual, con `auto`, fracciones o
`between`, y `Gap` separa las celdas. Las filas son pistas `auto`: cada una mide lo que su celda más alta, y las celdas
se estiran a su fila. Cada celda es un ítem del grid colocado explícitamente: va en la primera columna libre de su fila,
y ocupa las filas y columnas de su `Span` (`Span: 1 3` ocupa tres columnas). Una columna está ocupada si la ocupa una
celda de una fila de arriba con un `Span` de varias filas. Cada fila llena **exactamente** todas las columnas
(`TABLE_ROW_CELLS`): una celda vacía se escribe `Cell:`. Un `Span` no se sale de la tabla, no pasa de un grupo a otro
(de `Header rows` a `Body`, de `Body` a `Footer rows`) ni se solapa con otra celda (`TABLE_SPAN`).

`Span: 1 1` explícito se admite: es el valor por defecto escrito y no una contradicción.

### Ancho de las columnas

Las columnas se dimensionan con los pasos de `Grid` ([dimensionado de pistas](grid.md#dimensionado-de-pistas)), salvo en
cómo se reparte el espacio, que en una tabla va **en proporción al contenido** y no a partes iguales:

- **Maximizar** (paso 3): el espacio libre se reparte entre las columnas no flexibles en proporción a lo que le falta a cada
  una hasta su límite, de modo que todas recorren la misma fracción del camino de su ancho más estrecho a su ancho
  natural. Con dos columnas `auto` que van de 60 a 140 y de 50 a 90 y 60 libres, la primera recibe 40 y la segunda 20.
- **Estirar** (paso 5): sin fracciones, lo que sobra se reparte entre las columnas `auto` en proporción a su ancho
  natural. Con `auto auto`, anchos naturales de 20 y 60 y 180 de sobra, las columnas miden 65 y 195. Si todas están
  vacías, a partes iguales.

Los anchos fijos, los porcentajes, `between(a, b)` y las fracciones no cambian: `Column sizes: 40mm auto auto` deja la
primera columna en 40 mm y reparte el resto entre las otras dos por su contenido, y `auto *` da a la `auto` su ancho
natural y a la fracción lo que queda. Las filas se dimensionan como en `Grid`.

> **Decisión:** las columnas de una tabla reparten el espacio en proporción a su contenido, y las de un `Grid`, a partes
> iguales. En una tabla, una columna con textos largos necesita más sitio que una de etiquetas cortas; en un `Grid` de
> maquetación, dos columnas `auto` iguales se esperan iguales. No hay propiedad para elegir: quien quiere otro reparto
> fija los anchos o usa fracciones. *Alternativa descartada:* una propiedad en `Table` para elegir el reparto, o el
> mismo reparto en `Grid`.

## Celdas

Una `Cell` es una caja con dos formas, como un `Text`: con valor (`Cell: 100`), es una caja de texto de una línea, y sin
valor, un bloque con elementos (cualquiera salvo los encabezados), o vacía si no tiene ninguno. Las dos cosas a la vez son
un error (`CELL_CONTENT`). Un texto largo va en un `Paragraph`. Admite `Id`, `Style`, `Link`, `Box`, `Typography`, `Span` y
`Row header`. Los bordes son los de cada celda, separados: una línea entre filas se dibuja con el borde inferior de las celdas.
Con `Merge borders: true`, el borde entre dos celdas se dibuja una sola vez ([Bordes fusionados](#bordes-fusionados)).
Los márgenes de una celda la separan de su área, como en un `Grid`.

> **Decisión:** `Row header: true` va en cada celda que es cabecera de su fila (el concepto de una factura, la hora de un
> horario): sirve también para la etiqueta de un pie de totales o la tercera columna de una agenda. *Alternativa
> descartada:* `Row header columns: N` en la `Table`, que solo sirve para columnas enteras.

## Estilo de las celdas

Una regla de `Styles` ([Styles](../common/styles.md)) da estilo a las celdas de una tabla sin repetir un estilo en cada
una:

- **Por posición.** `For element: Header cell`, `Body cell` y `Footer cell` son las celdas de cada grupo, y
  `Odd row cell` y `Even row cell`, las de las filas impares y pares del **cuerpo**, contando desde la primera fila de
  `Body` como la 1 (la cebra, en la que no entran la cabecera ni el pie). La fila de una celda que ocupa varias es la
  **primera** de las que ocupa. Una celda cumple a la vez la de su grupo y la de su fila, y la regla
  `For element: Cell` de siempre.
- **Por columna.** `For column: 3` (o `For column: 2 4`, de la 2 a la 4) se aplica a las celdas que **empiezan** en esas
  columnas, en cualquier grupo: una celda con `Span` de varias columnas es de la columna donde empieza.
- **`Column align`.** Un `Column align: left right right` de la `Table` da una alineación a cada columna (tantas como
  `Column sizes`). Es solo el valor por defecto del `Align` de las celdas: lo sobrescriben las reglas de posición, de columna,
  de estilo y de `Id`, y lo que declara la propia celda.
- **Por fila.** El `Style` de una `Row` da a cada una de sus celdas las reglas `For style` de esos estilos (el pie de
  totales de una factura: `Style: total` en la `Row` y no en cada celda), y la regla `For id` de su `Id`, la suya.
- **Por tabla.** Una regla de celda escrita dentro de la regla de una `Table` (`For style: pricing` →
  `For element: Header cell`) solo da estilo a las celdas de esa tabla ([Styles](../common/styles.md#reglas-anidadas)).

La prioridad de todas ellas está en [Styles](../common/styles.md#prioridad). Las celdas de las tablas de un `Markdown`
reciben estas mismas reglas. Su `Align` sale de la fila de separación de la tabla (`:--`, `--:`), que lo declara cada celda
y gana a todo lo demás.

Si dos `For column` se solapan, se aplican en el orden de `Styles`, como los estilos.

> **Decisión:** dentro de una celda, de menos a más fuerte: `For element: Cell`, `Column align`, las reglas de posición (grupo y
> luego fila), `For column`, los estilos de la `Row`, los estilos de la celda, el `For id` de la `Row`, el `For id` de la
> celda y lo propio de la celda. Así la fila de totales gana a la cebra y a la columna, una cabecera centrada gana a la alineación de su columna y una celda suelta lo
> puede romper con su estilo. *Alternativa descartada:* que la posición gane a la columna, o `Column align` por debajo de
> `For element: Cell`.

## Filas

Una `Row` no es una caja del layout: no ocupa sitio propio ni se nombra en `For element`, como `Header rows`, `Body` y
`Footer rows`. Las filas de la tabla no tienen propiedad de tamaño y miden lo que su celda más alta. Lo que declara la
fila es para sus celdas o para su **franja**:

- **`Style` e `Id`.** Las reglas `For style` de sus estilos y la regla `For id` de su `Id` se aplican a cada una de sus
  celdas ([Estilo de las celdas](#estilo-de-las-celdas)).
- **`Typography`.** Lo heredan sus celdas (las reglas `Styles` de la celda le ganan, como a cualquier herencia).
- **`Break`.** Es el de sus celdas: con él se fuerza o se evita el corte antes o después de la fila, o dentro de ella. Una
  celda no declara `Break`, ni siquiera desde una regla (`STYLE_NOT_ALLOWED`, también la del estilo o el `Id` de su
  `Row`: el `Break` de la fila lo declara la `Row`).
- **`Box`.** Solo el aspecto de la franja: `Background`, `Border`, `Border color` y `Corner radius`. Cualquier otra propiedad
  (`Padding`, `Margin` o `Height`) no tendría efecto en una fila y es `ROW_BOX`.

Una fila con `Box` o `Id` tiene una **franja**: un rectángulo que va de la primera columna a la última, con el `Gap`
entre ellas, y del principio de la fila a su final. El `Gap` entre filas no es de ninguna. Se pinta **debajo de sus
celdas**: el fondo de una celda tapa el de la fila, y una celda que ocupa varias filas, las franjas de todas. El color
de su borde, si no declara `Border color`, es el del texto de la fila (su `Typography`, o el de la tabla). La franja de
una fila de `Header rows` o `Footer rows` se repite con ella.

```
Footer rows:
    Row:
        Id: total
        Style: total
        Box:
            Background: #F4F2EC
            Border: 1 0 0 0
        Cell: Total
            Span: 1 3
        Cell: 2630,00
```

El `Id` de una fila es un `Id` del documento, único como cualquier otro: es destino de `Link: #id` y de `{page of: id}`
([Referencias](../common/references.md)), que llevan a su franja. La de una fila repetida lleva a la primera de sus páginas.

> **Decisión:** la fila no es una caja del layout, pero su aspecto es suyo: una franja debajo de sus celdas. Así un
> `Border` de fila es una línea entera y no un borde en cada celda, y un fondo cubre también el `Gap`. *Alternativa
> descartada:* que el `Box` de la `Row` se repita en cada celda (un `Border` de fila saldría entre celdas), o una fila
> con `Padding` y tamaño, que contradiría las columnas.

> **Decisión:** el `Id` de una fila da estilo a sus celdas, como su `Style`: el aspecto de la franja solo lo da su `Box`.
> *Alternativa descartada:* que `For id` con el `Id` de una fila diera el aspecto de la franja, que haría distinto el
> `For id` de la fila y su `Style`.

> **Decisión:** nada de selectores por posición de la fila (`Last row cell`), que identifican la fila de totales por su
> sitio y no por lo que es: para eso están su `Style` y su `Id`.

## Bordes fusionados

Sin más, cada celda pinta su borde, y dos celdas vecinas con `Border: 0.5` dibujan dos líneas juntas, de 1 en total. Con
`Merge borders: true` en el `Box` de la `Table`, el borde entre dos celdas se dibuja **una sola vez**:

```
Styles:
    For element: Table
        Box:
            Merge borders: true
    For element: Cell
        Box:
            Padding: 3 5
            Border: 0.5
            Border color: #8A8478
    For element: Header cell
        Box:
            Border: 0.5 0.5 1.5 0.5
            Border color: #12315A
Table:
    Column sizes: * 60 60
    Header rows:
        Row:
            Cell: Tarifa
            Cell: Mes
            Cell: Año
    Body:
        Row:
            Cell: Básica
            Cell: 9,00
            Cell: 90,00
```

- **Dónde se escribe.** `Merge borders` es del grupo `Box`, junto a `Border`: se escribe en el `Box` de la `Table` o en
  una regla de `Styles` ([Styles](../common/styles.md)), que también llega a las tablas de un `Markdown`. En el ejemplo,
  `For element: Table` fusiona los bordes de todas las tablas del documento, y una tabla los separa con
  `Merge borders: false` en su `Box`. Solo tiene efecto en una `Table`: escrito en el `Box` de otro elemento es
  `MERGE_BORDERS_WITHOUT_EFFECT`, y si llega de una regla a otra caja (un `For style` que usan una tabla y un `Block`),
  esa caja no hace nada con él.
- **La línea.** Entre dos celdas vecinas, la línea es el más ancho de los dos lados que se tocan, con su color
  (`Border color`, o el del texto de la celda). Con el mismo ancho, gana el de la celda de arriba o el de la izquierda. Un
  lado sin borde no aporta nada: la línea es la del otro, y si ninguno tiene, no hay línea. En el ejemplo, la línea bajo
  la cabecera es la de las celdas de la cabecera (1,5), y las demás, de 0,5.
- **Por tramos.** Una celda que ocupa varias filas o columnas tiene un tramo de línea por cada celda que tiene al lado,
  cada uno resuelto con esa celda. Dentro de una celda que ocupa varias filas o columnas no hay línea.
- **Dónde va.** Las líneas van sobre las líneas de la rejilla: la de entre dos celdas, centrada en ella; la del borde
  exterior de la tabla, entera dentro de la tabla. Donde se cruzan dos líneas, la horizontal entra en la vertical la mitad
  del ancho de esta, y la más ancha se pinta encima.
- **El sitio de cada celda.** El `Border` de una celda ya no se pinta: es el sitio que la celda deja para las líneas. En
  un lado entre dos celdas, la mitad de la línea más ancha de ese lado; en un lado del borde de la tabla, su borde entero.
  El relleno y el contenido empiezan después, así que el alto de las filas y el ancho de las columnas cuentan con él.
- **Qué tapa a qué.** Las líneas se pintan encima de los fondos de las celdas y de lo que contienen, y debajo de su
  texto. La franja de una fila (su `Box`, [Filas](#filas)) no se fusiona: se pinta debajo de las celdas, como siempre.
- **Lo que no admite.** `Gap` en la tabla o `Corner radius` en una de sus celdas, también si `Merge borders` o el
  `Corner radius` llegan de una regla, son `MERGE_BORDERS_CONFLICT`: el borde entre dos celdas no sería una línea.
  `Merge borders: true` en el `Box` de una tabla ninguna de cuyas celdas tiene borde es `MERGE_BORDERS_WITHOUT_EFFECT`;
  desde una regla, esa tabla queda como está.

Los cortes de página tienen sus reglas en [Cortes](#cortes).

> **Decisión:** el borde exterior de la tabla, y el de arriba y el de abajo de cada página por la que pasa, van enteros
> dentro de la tabla: la tabla no pinta fuera de su caja. *Alternativa descartada:* centrarlos sobre el borde, como los
> procesadores de textos, que deja media línea fuera de la tabla, en el margen o encima de lo que la sigue.

> **Decisión:** el sitio de cada celda se calcula con las celdas que tiene al lado en la tabla escrita. Una línea que
> solo sale en un corte de página (la del fragmento, o la que hay bajo una cabecera repetida) y es más ancha que ese
> sitio entra en el relleno de la celda lo que le falta. *Alternativa descartada:* reservar en cada lado la línea más
> ancha posible en cualquier página, que ensancharía todas las filas por una línea que solo sale en algunas.

> **Decisión:** `Merge borders` es un booleano, `false` por defecto: sin él, ninguna tabla cambia. *Alternativa
> descartada:* `Borders: separate | collapse`, con un término que no es de maquetación, y fusionar los bordes siempre,
> como un procesador de textos, que movería todas las tablas con bordes.

> **Decisión:** `Merge borders` va en el grupo `Box`, aunque solo tenga efecto en una tabla, para que una regla de
> `Styles` lo dé a todas las tablas de un documento o de un documento de estilos aparte, también a las de un `Markdown`.
> Como `Overflow: shrink`, lo que escribe el propio elemento se comprueba y lo que llega de una regla a donde no tiene
> efecto no hace nada. *Alternativa descartada:* una propiedad propia de la `Table`, como `Split rows`, que las reglas no
> admiten y que dejaba sin bordes fusionados las tablas de un `Markdown`.

## Cortes

Una tabla se corta entre filas, como un grid ([Cortes](../common/breaks.md#cortes-en-stack-grid-y-table)): una fila que no
cabe en lo que queda de página se parte en esa página, con sus celdas en paralelo, salvo que su `Row` declare
`Keep together: true` (el `Break` de la fila es el de sus celdas) o que alguna celda no pueda colocar nada: entonces pasa
entera a la siguiente. Con `Split rows: false` en la `Table`, toda fila que no cabe pasa entera, y solo se parte si no cabe
ni en una página vacía.

Una fila que se parte entre páginas parte también su franja: en cada página va del principio al final de lo que hay de
la fila, y su borde se corta como el de cualquier caja, según el `Edges` de su `Break` ([Cortes](../common/breaks.md)).
Si se parte un grupo de filas que una celda ocupa a la vez, cada franja se recorta en la primera página, y en la siguiente
la continuación lleva la franja de la última fila del grupo.

> **Decisión:** la continuación de un grupo de filas partido lleva la franja de su última fila: lo que sigue en la
> página siguiente no se reparte entre las filas del grupo. *Alternativa descartada:* repartirlo por las alturas sin
> partir, que no corresponden a lo que se coloca.

Con `Merge borders` ([Bordes fusionados](#bordes-fusionados)), la línea de arriba y la de abajo de cada página van dentro
de la tabla, como su borde exterior. Si el corte cae entre dos filas, la línea entre ellas se dibuja en las dos páginas,
igual en cada una. Si cae dentro de una fila que se parte, el lado de cada celda partida solo se dibuja con
`Edges: closed` (el del `Break` de su `Row`). Bajo la cabecera repetida, la línea es la más ancha entre la cabecera y la
fila del cuerpo que la sigue en esa página. Una celda que ya terminó en una página anterior no dibuja nada en la
continuación de su grupo.

Las filas de `Header rows` y `Footer rows` se **repiten** en cada página de la tabla: la cabecera arriba y el pie justo
detrás de la última fila del cuerpo de esa página, cada uno con el `Gap` que lo separa del cuerpo. No se parten nunca y
no admiten `Break` (`TABLE_ROW_BREAK`). Las filas del cuerpo se cortan en lo que dejan la cabecera y el pie: si no cabe
ninguna, la tabla entera pasa a la página siguiente, y la cabecera no se queda sola. Si al principio de una página la
cabecera y el pie no dejan sitio al cuerpo, la tabla no puede avanzar y es un error (`TABLE_REPEAT_OVERFLOW`). Una fila
del cuerpo que no cabe en el sitio que dejan se parte en paralelo, como cualquier fila, y si no se puede partir desborda
la página (`PAGE_OVERFLOW`). Las cajas `Overlay` y `Page overlay` de las celdas de la cabecera y el pie solo van en el
primer fragmento de la tabla.

> **Decisión:** una celda con un párrafo largo en una tabla con cabecera no es un error: se parte. *Alternativa
> descartada:* exigir una fila entera del cuerpo en cada página.

> **Decisión:** el pie va detrás de la última fila del cuerpo y no pegado al final del área útil. *Alternativa descartada:*
> estirar la tabla hasta el final de cada página.

## Accesibilidad

Un lector de pantalla lee la tabla como tabla: las celdas de `Header rows` son las cabeceras de su columna y las que
declaran `Row header: true`, las de su fila, y las anuncia con cada celda. La cabecera y el pie repetidos se leen una
sola vez, en la primera página en que salen.

## Lo que no hay

- `Box` en una columna y una fila con tamaño o relleno propios. El título de una tabla («Tabla 3. Bornes») es el
  `Caption` de una [`Figure`](figure.md) con la tabla dentro.
- Filas con menos celdas que columnas, o `Span` que pasan de un grupo a otro: son errores.
