# `Table`

Una `Table` coloca sus celdas en filas y columnas: sus filas (`Row`) van en una cabecera (`Head`, opcional), un cuerpo
(`Body`, obligatorio) y un pie (`Foot`, opcional), **en ese orden** (`TABLE_ORDER`), y cada fila tiene sus celdas (`Cell`)
de izquierda a derecha. La `Table` admite `Id`, `Class`, `Link`, `Box`, `Typography` y `Break`, y va donde va un elemento
en flujo, también dentro de una celda.

```
Styles:
	Element: Cell
		Box:
			Padding: 2 4
			Border: 0 0 0.5 0
Table:
	Columns: 120 1fr 1fr
	Head:
		Row:
			Typography:
				Weight: bold
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
| `Columns` | lista de pistas | — (obligatorio) | `Table` | Las columnas, como en `Grid` ([Grid](grid.md#dimensionado-de-pistas)). |
| `Column align` | una alineación por columna: `left`, `center`, `right` o `justify` | — | `Table` | La alineación por defecto de las celdas de cada columna. Tantas como `Columns` (`TABLE_COLUMN_ALIGN`). |
| `Gap` | 1 o 2 longitudes (filas columnas) | `0` | `Table` | Separación entre celdas, como en `Grid`. |
| `Span` | `filas columnas`, dos enteros positivos | `1 1` | `Cell` | Filas y columnas que ocupa la celda. |
| `Header` | `true`, `false` | `false` | `Cell` | La celda es la cabecera de su fila ([accesibilidad](#accesibilidad)). No vale en `Head` (`TABLE_HEADER`). |

No van en las reglas de `Styles`, que solo admiten los grupos.

## Una tabla es un grid

`Columns` (obligatorio) y `Gap` son los de `Grid`: las columnas se dimensionan igual, con `auto`, fracciones, `minmax`…, y
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
(`CELL_CONTENT`); un texto largo va en un `Paragraph`. Admite `Id`, `Class`, `Link`, `Box`, `Typography`, `Span` y
`Header`. Los bordes son los de cada celda, separados: una línea entre filas se dibuja con el borde inferior de las celdas.
Los márgenes de una celda la separan de su área, como en un `Grid`.

> **Decisión:** `Header: true` va en cada celda que es cabecera de su fila (el concepto de una factura, la hora de un
> horario): sirve también para la etiqueta de un pie de totales o la tercera columna de una agenda. *Alternativa
> descartada:* `Header columns: N` en la `Table`, que solo sirve para columnas enteras.

## Estilo de las celdas

Una regla de `Styles` ([Styles](../comun/estilos.md)) da estilo a las celdas de una tabla sin repetir una clase en cada
una:

- **Por posición.** `Element: Head cell`, `Body cell` y `Foot cell` son las celdas de cada grupo; `Odd row cell` y
  `Even row cell`, las de las filas impares y pares del **cuerpo**, contando desde la primera fila de `Body` como la 1 (la
  cebra; la cabecera y el pie no entran). La fila de una celda que ocupa varias es la **primera** de las que ocupa. Una
  celda cumple a la vez la de su grupo y la de su fila, y la regla `Element: Cell` de siempre.
- **Por columna.** `For column: 3` (o `For column: 2 4`, de la 2 a la 4) se aplica a las celdas que **empiezan** en esas
  columnas, en cualquier grupo: una celda con `Span` de varias columnas es de la columna donde empieza.
- **`Column align`.** Un `Column align: left right right` de la `Table` da una alineación a cada columna (tantas como
  `Columns`). Es solo el valor por defecto del `Align` de las celdas: lo sobrescriben las reglas de posición, de columna,
  de clase y de `Id`, y lo que declara la propia celda.
- **Por fila.** El `Class` de una `Row` da a cada una de sus celdas las reglas `For class` de esas clases (el pie de
  totales de una factura: `Class: total` en la `Row`, no en cada celda).

La prioridad de todas ellas está en [Styles](../comun/estilos.md#prioridad). Las celdas de las tablas de un `Markdown`
reciben estas mismas reglas; su `Align` sale de la fila de separación de la tabla (`:--`, `--:`), que lo declara cada celda
y gana a todo lo demás.

Si dos `For column` se solapan, se aplican en el orden de `Styles`, como las clases.

> **Decisión:** dentro de una celda, de menos a más fuerte: `Element: Cell`, `Column align`, las reglas de posición (grupo y
> luego fila), `For column`, las clases de la `Row`, las clases de la celda, `For id` y lo propio de la celda. Así la fila de
> totales gana a la cebra y a la columna, una cabecera centrada gana a la alineación de su columna y una celda suelta lo
> puede romper con su clase. *Alternativa descartada:* que la posición gane a la columna, o `Column align` por debajo de
> `Element: Cell`.

## Filas

Una `Row` no es una caja: no tiene `Box` ni `Id` ni se nombra en `Element`, como `Head`, `Body` y `Foot`. Su `Class` da a
sus celdas las reglas de sus clases. Su `Typography` lo heredan sus celdas (las reglas `Styles` de la celda le ganan, como
a cualquier herencia), y su `Break` es el de sus celdas: con él se fuerza o se evita el corte antes o después de la fila, o
dentro de ella. Una celda no declara `Break`, ni siquiera desde una regla (`STYLE_NOT_ALLOWED`, también la de la clase de
su `Row`: el `Break` de la fila lo declara la `Row`). Las filas de la tabla no tienen propiedad de tamaño: miden lo que su
celda más alta.

> **Decisión:** la fila no es una caja. *Alternativa descartada:* que `Row` admita un `Box`, que se repetiría en cada celda
> (un `Border` de fila saldría entre celdas), o selectores por posición de la fila (`Last row cell`), que identifican la
> fila de totales por su sitio y no por lo que es.

## Cortes

Una tabla se corta entre filas, como un grid ([Cortes](../comun/cortes.md#cortes-en-flex-grid-y-table)): una fila que no
cabe en lo que queda de página pasa entera a la siguiente, y solo se parte, con sus celdas en paralelo, si no cabe ni en una
página vacía.

Las filas de `Head` y `Foot` se **repiten** en cada página de la tabla: la cabecera arriba y el pie justo detrás de la
última fila del cuerpo de esa página, cada uno con el `Gap` que lo separa del cuerpo. No se parten nunca y no admiten
`Break` (`TABLE_ROW_BREAK`). Las filas del cuerpo se cortan en lo que dejan la cabecera y el pie: si no cabe ninguna, la
tabla entera pasa a la página siguiente, y la cabecera no se queda sola. Si al principio de una página la cabecera y el pie
no dejan sitio al cuerpo, la tabla no puede avanzar y es un error (`TABLE_REPEAT_OVERFLOW`); una fila del cuerpo que no
cabe en el sitio que dejan se parte en paralelo, como cualquier fila, y si no se puede partir desborda la página
(`PAGE_OVERFLOW`). Las cajas `Absolute` y `Fixed` de las celdas de la cabecera y el pie solo van en el primer fragmento de
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
- `Box` en una fila (un fondo o un borde de fila se da a cada celda) o en una columna, `Id` en una fila (no se puede
  enlazar a una fila ni darle estilo con `For id`) y un título de tabla aparte (un encabezado o un `Paragraph` antes de la
  tabla).
- Filas con menos celdas que columnas, o `Span` que pasan de un grupo a otro: son errores.
