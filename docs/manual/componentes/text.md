# `Text` y `Paragraph`

`Text` y `Paragraph` son cajas de texto: reparten su texto en líneas del ancho que tienen. La diferencia es de
significado: un `Paragraph` es **un párrafo** (y así lo etiqueta el PDF para la accesibilidad), y un `Text` es texto sin
más, que puede tener varios párrafos.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Carta
	Page:
		Paper: A4
		Margin: 60
	Styles:
		For element: Document
			Typography:
				Font: IBM Plex Serif
				Size: 11
				Line height: 16
		For element: Paragraph
			Box:
				Margin: 0 0 10 0
			Typography:
				Align: justify
				Line breaking: balanced
				Markup: markdown
	Section:
		Text: Madrid, 3 de marzo
			Typography:
				Align: right
			Box:
				Margin: 0 0 24 0
		Paragraph: Estimada Sra. García:
		Paragraph:
			Content >>
				Le escribimos para confirmar el pedido de **doce sillas** de nogal,
				que le entregaremos en *dos semanas* en la dirección que nos indicó.
		Paragraph: Un saludo cordial.
```

## El texto

Va en el valor si es corto (`Paragraph: Un saludo cordial.`) o en un bloque `Content >>` si es largo. Uno de los dos, nunca
los dos. Dentro del bloque:

- los saltos de línea simples son espacios, así que puedes partir las líneas como quieras;
- en un `Text`, una línea en blanco empieza otro párrafo; en un `Paragraph`, es un error: para varios párrafos, varios
  `Paragraph`.

El texto es literal salvo que pidas formato con `Markup: markdown` ([Formato del texto](../comun/formato-del-texto.md)).
Puede llevar números de página y referencias: `{page}`, `{pages}`, `{page of: id}`, `{number of: id}`
([Referencias y enlaces](../comun/referencias-y-enlaces.md)).

## Propiedades

Admiten `Id`, `Style`, `Link`, `Box`, `Typography`, `Break` y `Rotate`. Necesitan `Font` y `Size`, propios o heredados
([Tipografía](../comun/tipografia.md)). Con `Rotate: -90`, el texto va en vertical y ocupa su sitio girado
([Capas y giro](../comun/capas-y-giro.md#girar-una-caja-rotate)).

## Cómo se comportan

- Ocupan todo el ancho disponible, salvo que declaren `Width`, y miden de alto lo que sus líneas.
- Se parten entre páginas entre dos líneas, sin dejar líneas sueltas (`Widows` y `Orphans`).
- Una palabra más ancha que la línea es un error: no se parten palabras.
- Como ítem de un `Flex` en fila, un texto parte de lo que mide sin partir ninguna línea y se estrecha si no hay sitio
  ([Flex](flex.md)).

## ¿`Text` o `Paragraph`?

Usa `Paragraph` para los párrafos del contenido y `Text` para lo demás: un rótulo, una cifra, una línea de una cabecera, el
texto de un encabezado. Así el PDF etiquetado tiene la estructura correcta, y una regla `For element: Paragraph` estila todos los
párrafos sin tocar el resto.

Más detalle en la [referencia](../../referencia/componentes/text.md).
