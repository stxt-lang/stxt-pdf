# `Block`

Un `Block` agrupa elementos **uno debajo de otro**. Sirve para dar un fondo, un borde o un margen a un grupo, para que un
grupo no se parta entre páginas o para que sus hijos hereden una tipografía.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Aviso
	Page:
		Paper: A4
		Margin: 40
	Styles:
		For element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
		For style: warning
			Box:
				Margin: 12 0
				Padding: 10 14
				Border: 0 0 0 4
				Border color: #C0392B
				Background: #FBEAE8
				Corner radius: 0 6 6 0
			Break:
				Inside: avoid
	Section:
		Paragraph: Antes de instalar el termostato, corta la corriente.
		Block:
			Style: warning
			Paragraph: Atención
				Typography:
					Bold: true
			Paragraph: Trabajar con la corriente conectada puede causar una descarga eléctrica.
		Paragraph: Después, retira la tapa con cuidado.
```

Admite `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Contain margins` y `Rotate` (girarlo con todo lo que contiene,
[Capas y giro](../comun/capas-y-giro.md#girar-una-caja-rotate)), y dentro cualquier elemento.

## Cómo coloca a sus hijos

- Cada hijo ocupa **todo el ancho** del `Block` (menos sus márgenes), salvo que declare su `Width`. Uno con `Width` y
  `Margin: 0 auto` queda centrado.
- Los hijos se apilan de arriba abajo, en el orden del documento.
- El `Block` mide de alto lo que suman sus hijos, más su relleno y su borde, salvo que declare `Height`.
- Entre dos hijos, los márgenes verticales no se suman: se queda el mayor ([La caja](../comun/caja.md#márgenes)).

Así colocan también sus hijos las secciones, los encabezados, las listas, las celdas con elementos y los elementos
posicionados.

## `Contain margins`

El margen de arriba del primer hijo (y el de abajo del último) sale fuera del `Block` si este no tiene ni borde ni relleno
en ese lado: queda como margen del propio `Block`. Con `Contain margins: true`, los márgenes de los hijos se quedan dentro. Un
`Block` que es ítem de un `Flex` o un `Grid` ya los deja dentro, así que ahí `Contain margins: true` es un error.

## Recetas

- **Un recuadro:** un `Block` con `Padding`, `Border` y `Background`.
- **Que no se parta:** `Break` con `Inside: avoid` (el bloque de firmas de un contrato).
- **Empezar en página nueva:** `Break` con `Before: page`.

Más detalle en la [referencia](../../referencia/componentes/block.md).
