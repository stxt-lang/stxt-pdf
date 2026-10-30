# `Table of contents`

El índice automático. Un `Table of contents` genera una entrada por cada encabezado del documento, con su título, una guía de puntos y
la página donde está. Cada entrada es clicable y lleva a su encabezado.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Informe
	Page:
		Paper: A4
		Margin: 50
	Styles:
		For element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
				Numbered: true
		For element: Heading 1
			Typography:
				Size: 18
				Bold: true
			Box:
				Margin: 0 0 12 0
		For element: Heading 2
			Typography:
				Size: 12
				Bold: true
			Box:
				Margin: 10 0 6 0
		For element: TOC 1
			Typography:
				Bold: true
			Box:
				Margin: 6 0 2 0
		For element: TOC 2
			Box:
				Padding: 0 0 0 16
	Section:
		Heading 1:
			In table of contents: false
			Typography:
				Numbered: false
			Text: Índice
		Table of contents:
			Levels: 1 2
	Section:
		Heading 1:
			Text: Resultados
		Heading 2:
			Text: Ventas
		Paragraph: Las ventas crecieron un 12 %.
		Heading 2:
			Text: Costes
		Paragraph: Los costes se mantienen.
		Heading 1:
			Text: Previsiones
		Paragraph: Se espera un año estable.
```

## Propiedades

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `Levels` | `1 3` | Los niveles de encabezado que lista, del primero al último: `Levels: 1 2` lista los `Heading 1` y `Heading 2` |
| `Leader` | `dots` | Lo que llena el hueco entre el título y la página: `dots` (puntos), `line` (una línea) o `none` |

Admite además `Id`, `Style`, `Box`, `Typography` y `Break`. No tiene hijos: las entradas las genera el motor.

## Las entradas

- Salen **todos** los encabezados de esos niveles, en orden, también los que están después del índice y los de un `Markdown`.
  Un encabezado con `In table of contents: false` no sale (el título de la portada, el del propio índice).
- Cada entrada tiene el texto del encabezado, con su número si lo lleva, la guía y el número de la página, el mismo que lleva
  impreso esa página.
- Se estilan con `For element: TOC 1` … `TOC 6`, una regla por nivel: la sangría de cada nivel es su
  `Padding` izquierdo, y la separación entre entradas, su `Margin`.
- Si un título no cabe en una línea, se parte, y el número queda en la primera línea.
- Un índice sin ningún encabezado en sus niveles es un error.

Más detalle en la [referencia](../../referencia/componentes/table-of-contents.md).
