# `Table`

Una `Table` coloca sus celdas en filas y columnas: sus filas (`Row`) van en una cabecera (`Head`, opcional), un cuerpo
(`Body`, obligatorio) y un pie (`Foot`, opcional), **en ese orden** (`TABLE_ORDER`), y cada fila tiene sus celdas (`Cell`)
de izquierda a derecha. La `Table` admite `Id`, `Style`, `Link`, `Box`, `Typography` y `Break`, y va donde va un elemento
en flujo, también dentro de una celda.

```
Styles:
	For element: Cell
		Box:
			Padding: 2 4
			Border: 0 0 0.5 0
Table:
	Column sizes: 120 1fr 1fr
	Head:
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
	Foot:
		Row:
			Cell: Total
				Span: 1 3
```

| Propiedad | Valores | Por defecto | Elementos | Notas |
|---|---|---|---|---|
| `Column sizes` | lista de pistas | — (obligatorio) | `Table` | Las columnas, como en `Grid` ([Grid](grid.md#dimensionado-de-pistas)). |
| `Column align` | una alineación por columna: `left`, `center`, `right` o `justify` | — | `Table` | La alineación por defecto de las celdas de cada columna. Tantas como `Column sizes` (`TABLE_COLUMN_ALIGN`). |
| `Gap` | 1 o 2 longitudes (filas columnas) | `0` | `Table` | Separación entre celdas, como en `Grid`. |
| `Span` | `filas columnas`, dos enteros positivos | `1 1` | `Cell` | Filas y columnas que ocupa la celda. |
| `Header` | `true`, `false` | `false` | `Cell` | La celda es la cabecera de su fila ([accesibilidad](#accesibilidad)). No vale en `Head` (`TABLE_HEADER`). |
| `Id` | texto | — | `Row` | Destino de enlaces y de `{page of: id}`; su regla `For id` da estilo a las celdas de la fila ([Filas](#filas)). |
| `Box` | `Background`, `Border`, `Border color`, `Radius` | — | `Row` | El aspecto de la franja de la fila ([Filas](#filas)). Nada más (`ROW_BOX`). |

No van en las reglas de `Styles`, que solo admiten los grupos.

## Una tabla es un grid

`Column sizes` (obligatorio) y `Gap` son los de `Grid`: las columnas se dimensionan igual, con `auto`, fracciones, `minmax`…, y
`Gap` separa las celdas. Las filas son pistas `auto`: cada una mide lo que su celda más alta, y las celdas se estiran a su
fila. Cada celda es un ítem del grid colocado explícitamente: va en la primera columna libre de su fila, y ocupa las filas
y columnas de su `Span` (`Span: 1 3` ocupa tres columnas). Una columna está ocupada si la ocupa una celda de una fila de
arriba con un `Span` de varias filas. Cada fila llena **exactamente** todas las columnas (`TABLE_ROW_CELLS`): una celda
vacía se escribe `Cell:`. Un `Span` no se sale de la tabla, no pasa de un grupo a otro (de `Head` a `Body`, de `Body` a
`Foot`) ni se solapa con otra celda (`TABLE_SPAN`).

`Span: 1 1` explícito se admite: es el valor por defecto escrito, no una contradicción.

## Celdas

Una `Cell` es una caja con dos formas, como un `Text`: con valor (`Cell: 100`), una caja de texto de una línea; sin valor,
un bloque con elementos (cualquiera salvo los encabezados), o vacía si no tiene ninguno. Las dos cosas a la vez son un error
(`CELL_CONTENT`); un texto largo va en un `Paragraph`. Admite `Id`, `Style`, `Link`, `Box`, `Typography`, `Span` y
`Header`. Los bordes son los de cada celda, separados: una línea entre filas se dibuja con el borde inferior de las celdas.
Los márgenes de una celda la separan de su área, como en un `Grid`.

> **Decisión:** `Header: true` va en cada celda que es cabecera de su fila (el concepto de una factura, la hora de un
> horario): sirve también para la etiqueta de un pie de totales o la tercera columna de una agenda. *Alternativa
> descartada:* `Header columns: N` en la `Table`, que solo sirve para columnas enteras.

## Estilo de las celdas

Una regla de `Styles` ([Styles](../comun/estilos.md)) da estilo a las celdas de una tabla sin repetir un estilo en cada
una:

- **Por posición.** `For element: Head cell`, `Body cell` y `Foot cell` son las celdas de cada grupo; `Odd row cell` y
  `Even row cell`, las de las filas impares y pares del **cuerpo**, contando desde la primera fila de `Body` como la 1 (la
  cebra; la cabecera y el pie no entran). La fila de una celda que ocupa varias es la **primera** de las que ocupa. Una
  celda cumple a la vez la de su grupo y la de su fila, y la regla `For element: Cell` de siempre.
- **Por columna.** `For column: 3` (o `For column: 2 4`, de la 2 a la 4) se aplica a las celdas que **empiezan** en esas
  columnas, en cualquier grupo: una celda con `Span` de varias columnas es de la columna donde empieza.
- **`Column align`.** Un `Column align: left right right` de la `Table` da una alineación a cada columna (tantas como
  `Column sizes`). Es solo el valor por defecto del `Align` de las celdas: lo sobrescriben las reglas de posición, de columna,
  de estilo y de `Id`, y lo que declara la propia celda.
- **Por fila.** El `Style` de una `Row` da a cada una de sus celdas las reglas `For style` de esos estilos (el pie de
  totales de una factura: `Style: total` en la `Row`, no en cada celda), y la regla `For id` de su `Id`, la suya.

La prioridad de todas ellas está en [Styles](../comun/estilos.md#prioridad). Las celdas de las tablas de un `Markdown`
reciben estas mismas reglas; su `Align` sale de la fila de separación de la tabla (`:--`, `--:`), que lo declara cada celda
y gana a todo lo demás.

Si dos `For column` se solapan, se aplican en el orden de `Styles`, como los estilos.

> **Decisión:** dentro de una celda, de menos a más fuerte: `For element: Cell`, `Column align`, las reglas de posición (grupo y
> luego fila), `For column`, los estilos de la `Row`, los estilos de la celda, el `For id` de la `Row`, el `For id` de la
> celda y lo propio de la celda. Así la fila de
> totales gana a la cebra y a la columna, una cabecera centrada gana a la alineación de su columna y una celda suelta lo
> puede romper con su estilo. *Alternativa descartada:* que la posición gane a la columna, o `Column align` por debajo de
> `For element: Cell`.

## Filas

Una `Row` no es una caja del layout: no ocupa sitio propio ni se nombra en `For element`, como `Head`, `Body` y `Foot`; las
filas de la tabla no tienen propiedad de tamaño y miden lo que su celda más alta. Lo que declara la fila es para sus celdas
o para su **franja**:

- **`Style` e `Id`.** Las reglas `For style` de sus estilos y la regla `For id` de su `Id` se aplican a cada una de sus
  celdas ([Estilo de las celdas](#estilo-de-las-celdas)).
- **`Typography`.** Lo heredan sus celdas (las reglas `Styles` de la celda le ganan, como a cualquier herencia).
- **`Break`.** Es el de sus celdas: con él se fuerza o se evita el corte antes o después de la fila, o dentro de ella. Una
  celda no declara `Break`, ni siquiera desde una regla (`STYLE_NOT_ALLOWED`, también la de el estilo o el `Id` de su
  `Row`: el `Break` de la fila lo declara la `Row`).
- **`Box`.** Solo el aspecto de la franja: `Background`, `Border`, `Border color` y `Radius`. Cualquier otra propiedad
  (`Padding`, `Margin`, `Height`…) no tendría efecto en una fila y es `ROW_BOX`.

Una fila con `Box` o `Id` tiene una **franja**: un rectángulo que va de la primera columna a la última, con el `Gap` entre
ellas, y del principio de la fila a su final; el `Gap` entre filas no es de ninguna. Se pinta **debajo de sus celdas**: el
fondo de una celda tapa el de la fila, y una celda que ocupa varias filas, las franjas de todas. El color de su borde, si
no declara `Border color`, es el del texto de la fila (su `Typography`, o el de la tabla). La franja de una fila de `Head`
o `Foot` se repite con ella.

```
Foot:
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
([Referencias](../comun/referencias.md)), que llevan a su franja; la de una fila repetida, a la primera de sus páginas.

> **Decisión:** la fila no es una caja del layout, pero su aspecto es suyo: una franja debajo de sus celdas. Así un
> `Border` de fila es una línea entera, no un borde en cada celda, y un fondo cubre también el `Gap`. *Alternativa
> descartada:* que el `Box` de la `Row` se repita en cada celda (un `Border` de fila saldría entre celdas), o una fila
> con `Padding` y tamaño, que contradiría las columnas.

> **Decisión:** el `Id` de una fila da estilo a sus celdas, como su `Style`: el aspecto de la franja solo lo da su `Box`.
> *Alternativa descartada:* que `For id` con el `Id` de una fila diera el aspecto de la franja, que haría distinto el
> `For id` de la fila y su `Style`.

> **Decisión:** nada de selectores por posición de la fila (`Last row cell`), que identifican la fila de totales por su
> sitio y no por lo que es: para eso están su `Style` y su `Id`.

## Cortes

Una tabla se corta entre filas, como un grid ([Cortes](../comun/cortes.md#cortes-en-flex-grid-y-table)): una fila que no
cabe en lo que queda de página pasa entera a la siguiente, y solo se parte, con sus celdas en paralelo, si no cabe ni en una
página vacía.

Una fila que se parte entre páginas parte también su franja: en cada página va del principio al final de lo que hay de
la fila, y su borde se corta como el de cualquier caja, según el `Decoration` de su `Break` ([Cortes](../comun/cortes.md)).
Si se parte un grupo de filas que una celda ocupa a la vez, cada franja se recorta en la primera página, y en la siguiente
la continuación lleva la franja de la última fila del grupo.

> **Decisión:** la continuación de un grupo de filas partido lleva la franja de su última fila: lo que sigue en la
> página siguiente no se reparte entre las filas del grupo. *Alternativa descartada:* repartirlo por las alturas sin
> partir, que no corresponden a lo que se coloca.

Las filas de `Head` y `Foot` se **repiten** en cada página de la tabla: la cabecera arriba y el pie justo detrás de la
última fila del cuerpo de esa página, cada uno con el `Gap` que lo separa del cuerpo. No se parten nunca y no admiten
`Break` (`TABLE_ROW_BREAK`). Las filas del cuerpo se cortan en lo que dejan la cabecera y el pie: si no cabe ninguna, la
tabla entera pasa a la página siguiente, y la cabecera no se queda sola. Si al principio de una página la cabecera y el pie
no dejan sitio al cuerpo, la tabla no puede avanzar y es un error (`TABLE_REPEAT_OVERFLOW`); una fila del cuerpo que no
cabe en el sitio que dejan se parte en paralelo, como cualquier fila, y si no se puede partir desborda la página
(`PAGE_OVERFLOW`). Las cajas `Overlay` y `Page overlay` de las celdas de la cabecera y el pie solo van en el primer fragmento de
la tabla.

> **Decisión:** una celda con un párrafo largo en una tabla con cabecera no es un error: se parte. *Alternativa
> descartada:* exigir una fila entera del cuerpo en cada página.

> **Decisión:** el pie va detrás de la última fila del cuerpo, no pegado al final del área útil. *Alternativa descartada:*
> estirar la tabla hasta el final de cada página.

## Accesibilidad

Un lector de pantalla lee la tabla como tabla: las celdas de `Head` son las cabeceras de su columna y las que declaran
`Header: true`, las de su fila, y las anuncia con cada celda. La cabecera y el pie repetidos se leen una sola vez, en la
primera página en que salen.

## Lo que no hay

- Bordes fusionados entre celdas: cada celda tiene el suyo.
- `Box` en una columna, una fila con tamaño o relleno propios y un título de tabla aparte (un encabezado o un `Paragraph`
  antes de la tabla).
- Filas con menos celdas que columnas, o `Span` que pasan de un grupo a otro: son errores.
