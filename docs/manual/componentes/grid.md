# `Grid`

Un `Grid` es una **rejilla**: declaras sus columnas (y, si quieres, sus filas) y cada ítem va en una celda o en un área de
varias. Sirve para composiciones de página: una barra lateral y el cuerpo, fichas en dos columnas, un currículum.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Ficha
	Page:
		Paper: A4
		Margin: 40
	Styles:
		For element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
		For style: panel
			Box:
				Padding: 10
				Background: #F1EFE8
	Section:
		Grid:
			Column sizes: 150 *
			Gap: 20
			Areas >>
				head head
				side main
			Area: head
				Text: Silla de nogal
					Typography:
						Size: 18
						Bold: true
			Area: side
				Text: Ref. 0412
					Style: panel
			Area: main
				Paragraph:
					Content >>
						Madera maciza de nogal, acabado al aceite. Asiento tapizado
						en lana natural.
```

## `Area`

Cada hijo de un `Grid` va dentro de un `Area`, que envuelve **exactamente un elemento**. El valor del `Area` dice dónde va:

| Valor | Dónde va |
|---|---|
| *(nada)* | En el siguiente hueco libre, recorriendo la rejilla por filas |
| `side` | En el área con ese nombre de `Areas` |
| `2 3` | En la fila 2, columna 3 (se cuenta desde 1) |
| `1 1 1 2` | En la fila 1, desde la columna 1, ocupando 1 fila y 2 columnas |
| `auto auto 1 2` | En el siguiente hueco donde quepan 2 columnas |

El `Area` admite además `Horizontal` y `Vertical` (la alineación del ítem en su área) y `Layer`. El tamaño, los márgenes y el
`Id` van en el elemento de dentro.

## Propiedades del `Grid`

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `Column sizes` | una columna | El ancho de cada columna |
| `Row sizes` | lo que mida cada fila | El alto de cada fila |
| `Gap` | `0` | El hueco entre celdas (con dos valores: entre filas y entre columnas) |
| `Areas` | — | Un dibujo de la rejilla con nombres: una línea por fila, un nombre por celda, `.` para una celda vacía |
| `Flow` | `horizontal` | Si los ítems sin sitio fijo se colocan por filas (`horizontal`) o por columnas (`vertical`) |
| `Horizontal`, `Vertical` | `stretch` | La alineación de los ítems en su celda: `start`, `end`, `center` o `stretch` (ocupar toda la celda) |

Cada nombre de `Areas` tiene que formar un rectángulo. Si un ítem cae fuera de las filas declaradas, se añaden filas nuevas.

## Las medidas de las columnas y las filas

| Se escribe | Mide |
|---|---|
| `150` | 150 puntos |
| `25%` | una parte del ancho del `Grid` (sin descontar los huecos) |
| `auto` | lo que pida su contenido |
| `narrowest`, `natural` | lo más estrecho que puede ser su contenido, o lo que mide sin partir ninguna línea |
| `*`, `2*` | una parte de lo que sobra; nunca menos que la palabra más larga de su contenido |
| `between(100, *)` | entre 100 y una parte de lo que sobra |
| `repeat(3, *)` | tres columnas `*` |

Primero se colocan las columnas fijas y las `auto`, y lo que sobra se reparte entre las fracciones. Las filas miden lo que su
ítem más alto, salvo que declares `Row sizes`; las filas en porcentaje necesitan un `Height` en el `Grid`.

## Cómo se comporta

- Por defecto, cada ítem se estira a toda su celda o su área; con `start`, `end` o `center`, mide lo suyo y se alinea.
- Los ítems no admiten `Margin: auto`: se alinean con `Horizontal` y `Vertical`.
- Entre páginas, un `Grid` se corta entre filas; una fila que no cabe en una página entera se parte con sus ítems a la vez.
- Una fila de tamaño fijo cuyo contenido no cabe es un error.

Más detalle en la [referencia](../../referencia/componentes/grid.md).
