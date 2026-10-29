# `Table`

Una tabla de filas y columnas. Su cabecera y su pie se repiten en cada página por la que pasa.

```
Document (dev.stxt.boxes):
	Language: es
	Title: Factura
	Media:
		Paper: A4
		Margin: 50
	Styles:
		Element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
		Element: Cell
			Box:
				Padding: 4 6
				Border: 0 0 0.5 0
				Border color: #C8C4BA
		Element: Head cell
			Typography:
				Weight: bold
				Color: #FFFFFF
			Box:
				Background: #12315A
		Element: Even row cell
			Box:
				Background: #F4F2EC
		For class: total
			Typography:
				Weight: bold
	Section:
		Table:
			Columns: 1fr 60 80 80
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
						Header: true
					Cell: 12
					Cell: 145,00
					Cell: 1740,00
				Row:
					Cell: Mesa de comedor
						Header: true
					Cell: 1
					Cell: 890,00
					Cell: 890,00
			Foot:
				Row:
					Class: total
					Cell: Total
						Header: true
						Span: 1 3
					Cell: 2630,00
```

## La estructura

- `Head` (opcional), `Body` (obligatorio) y `Foot` (opcional), en ese orden. Cada uno tiene una o varias `Row`, y cada fila,
  sus `Cell` de izquierda a derecha.
- **`Columns`** (obligatorio) son las columnas, con las mismas medidas que un `Grid`: puntos, porcentajes, `auto` (lo que
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

**`Header: true`** marca una celda como la cabecera de su fila (el concepto de una factura, la hora de un horario): un lector
de pantalla la lee con cada celda de la fila. Las celdas de `Head` ya son cabeceras de columna.

## Estilo de las celdas

Sin repetir una clase en cada celda:

| Regla | Celdas |
|---|---|
| `Element: Cell` | Todas |
| `Element: Head cell`, `Body cell`, `Foot cell` | Las de la cabecera, el cuerpo o el pie |
| `Element: Odd row cell`, `Even row cell` | Las de las filas impares o pares del cuerpo (la cebra) |
| `For column: 3` o `For column: 2 4` | Las que empiezan en esas columnas |
| `For class: total` con `Class: total` en la `Row` | Las de esa fila |
| `Column align` en la `Table` | Una alineación por columna, la más débil de todas |

La `Row` admite `Class`, `Typography` (que heredan sus celdas) y `Break` (el de la fila), pero no `Box`: un fondo o un borde
de fila se da a sus celdas. Una celda no admite `Break`: los cortes son de la fila.

## Entre páginas

- La tabla se corta entre filas: una fila que no cabe en lo que queda pasa entera a la siguiente página.
- La cabecera se repite arriba en cada página, y el pie, detrás de la última fila de cada página. Nunca se quedan solas.
- Una fila que no cabe ni en una página vacía se parte: sus celdas continúan en la página siguiente.
- Con `Break` en una `Row` del cuerpo puedes forzar o evitar un corte antes o después de ella.

Más detalle en la [referencia](../../referencia/componentes/table.md).
