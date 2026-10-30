# `Table`

Una tabla de filas y columnas. Su cabecera y su pie se repiten en cada página por la que pasa.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Factura
	Page:
		Paper: A4
		Margin: 50
	Styles:
		For element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
		For element: Cell
			Box:
				Padding: 4 6
				Border: 0 0 0.5 0
				Border color: #C8C4BA
		For element: Head cell
			Typography:
				Bold: true
				Color: #FFFFFF
			Box:
				Background: #12315A
		For element: Even row cell
			Box:
				Background: #F4F2EC
		For style: total
			Typography:
				Bold: true
	Section:
		Table:
			Column sizes: 1fr 60 80 80
			Column align: left right right right
			Head:
				Row:
					Cell: Concepto
					Cell: Uds.
					Cell: Precio
					Cell: Importe
			Body:
				Row:
					Cell: Silla de nogal
						Row header: true
					Cell: 12
					Cell: 145,00
					Cell: 1740,00
				Row:
					Cell: Mesa de comedor
						Row header: true
					Cell: 1
					Cell: 890,00
					Cell: 890,00
			Foot:
				Row:
					Style: total
					Cell: Total
						Row header: true
						Span: 1 3
					Cell: 2630,00
```

## La estructura

- `Head` (opcional), `Body` (obligatorio) y `Foot` (opcional), en ese orden. Cada uno tiene una o varias `Row`, y cada fila,
  sus `Cell` de izquierda a derecha.
- **`Column sizes`** (obligatorio) son las columnas, con las mismas medidas que un `Grid`: puntos, porcentajes, `auto` (lo que
  pida su contenido) y fracciones (`1fr`: una parte de lo que sobra) ([Grid](grid.md)).
- Cada fila llena **exactamente** todas las columnas: una celda vacía se escribe `Cell:`. Una fila con celdas de más o de
  menos es un error.
- **`Span: filas columnas`** hace que una celda ocupe varias: `Span: 1 3` son tres columnas. Una celda que ocupa varias filas
  deja ocupada su columna en las filas de debajo.
- Las filas miden lo que su celda más alta, y todas las celdas de una fila se estiran a ese alto.

## Las celdas

Una celda con valor (`Cell: 145,00`) es un texto. Sin valor, tiene elementos dentro (un `Paragraph`, una `List`, otra
`Table`…), pero no las dos cosas. Cada celda tiene su propio borde: una línea entre filas es el borde de abajo de las celdas.
`Gap` en la `Table` separa las celdas.

**`Row header: true`** marca una celda como la cabecera de su fila (el concepto de una factura, la hora de un horario): un lector
de pantalla la lee con cada celda de la fila. Las celdas de `Head` ya son cabeceras de columna.

## Estilo de las celdas

Sin repetir un estilo en cada celda:

| Regla | Celdas |
|---|---|
| `For element: Cell` | Todas |
| `For element: Head cell`, `Body cell`, `Foot cell` | Las de la cabecera, el cuerpo o el pie |
| `For element: Odd row cell`, `Even row cell` | Las de las filas impares o pares del cuerpo (la cebra) |
| `For column: 3` o `For column: 2 4` | Las que empiezan en esas columnas |
| `For style: total` con `Style: total` en la `Row` | Las de esa fila |
| `Column align` en la `Table` | Una alineación por columna, la más débil de todas |

La `Row` admite `Style` e `Id` (sus reglas `For style` y `For id` se aplican a sus celdas), `Typography` (que heredan sus
celdas) y `Break` (el de la fila). Una celda no admite `Break`: los cortes son de la fila.

## El aspecto de una fila

Con `Box`, una fila tiene un fondo y un borde propios: una **franja** de la primera columna a la última, debajo de sus
celdas. Un borde de fila es una línea entera, también por encima del hueco entre columnas, y el fondo de una celda tapa el
de su fila. El `Box` de una `Row` solo admite `Background`, `Border`, `Border color` y `Corner radius`: la fila mide lo que sus
celdas, así que un `Padding` o un `Height` en ella es un error. El `Id` de una fila sirve también para enlazarla
(`Link: #total`) o para citar su página (`{page of: total}`).

```
Document (dev.stxt.pdf):
	Language: es
	Title: Presupuesto
	Page:
		Paper: A5
		Margin: 40
	Styles:
		For element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
		For element: Cell
			Box:
				Padding: 4 6
		For style: total
			Typography:
				Bold: true
	Section:
		Table:
			Column sizes: 1fr 80
			Column align: left right
			Gap: 0 8
			Head:
				Row:
					Box:
						Border: 0 0 1 0
					Typography:
						Bold: true
					Cell: Concepto
					Cell: Importe
			Body:
				Row:
					Cell: Diseño
					Cell: 1200,00
				Row:
					Cell: Maquetación
					Cell: 800,00
			Foot:
				Row:
					Id: total
					Style: total
					Box:
						Background: #F4F2EC
						Border: 1 0 0 0
						Corner radius: 0 0 4 4
					Cell: Total
					Cell: 2000,00
```

## Entre páginas

- La tabla se corta entre filas: una fila que no cabe en lo que queda pasa entera a la siguiente página.
- La cabecera se repite arriba en cada página, y el pie, detrás de la última fila de cada página. Nunca se quedan solas.
- Una fila que no cabe ni en una página vacía se parte: sus celdas continúan en la página siguiente.
- Con `Break` en una `Row` del cuerpo puedes forzar o evitar un corte antes o después de ella.

Más detalle en la [referencia](../../referencia/componentes/table.md).
