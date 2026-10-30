# `Image`

Una imagen PNG, JPEG o SVG de la carpeta `images/` de los recursos. Es una caja como cualquier otra: tiene su tamaño, sus
márgenes, su borde, sus esquinas, y se coloca en el flujo o en un `Flex`, un `Grid` o un posicionado.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Catálogo
	Media:
		Paper: A4
		Margin: 40
	Styles:
		Element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
	Section:
		Image: examples/brand/nogal.svg
			Alt: Taller Nogal
			Box:
				Width: 120
				Margin: 0 0 20 0
		Grid:
			Columns: 1fr 1fr
			Gap: 16
			Area:
				Image: examples/catalog/silla.jpg
					Alt: Silla de nogal con asiento de lana
					Fit: cover
					Box:
						Height: 160
						Radius: 6
			Area:
				Image: examples/catalog/mesa.jpg
					Alt: Mesa de comedor de nogal
					Fit: cover
					Box:
						Height: 160
						Radius: 6
```

## Propiedades

| Propiedad | Qué hace |
|---|---|
| *(valor)* | La ruta del fichero dentro de `images/`: `Image: logos/acme.png`. Obligatoria |
| `Alt` | El texto alternativo, para quien no ve la imagen. Sin `Alt`, la imagen es decorativa y el lector de pantalla la salta |
| `Fit` | Cómo llena la imagen su caja si no tienen la misma proporción: `contain` (entera, con espacio a los lados), `cover` (llena la caja y recorta lo que sobra) o `fill` (se deforma) |
| `Rotate` | Girar la imagen sobre su centro; ocupa el rectángulo que la envuelve girada ([Capas y giro](../comun/capas-y-giro.md#girar-una-caja-rotate)) |

Admite además `Id`, `Class`, `Link`, `Box` y `Break` (sin `Inside` ni `Decoration`: no se parte). No admite `Typography`.

## El tamaño

- Sin `Width` ni `Height`, una imagen mide su **tamaño natural**: cada píxel, 0,75 puntos. Una imagen de 400 × 300 píxeles
  mide 300 × 225 puntos. Un SVG mide lo que dicen su `width` y su `height` (o su `viewBox`).
- No se estira sola al ancho de la página: si no cabe, es un error. Para que ocupe todo el ancho, `Width: 100%`.
- Con `Width` o `Height`, el otro lado sale de la proporción de la imagen. Con los dos, la caja mide eso y `Fit` decide cómo
  la llena la imagen.
- `Margin: 0 auto` la centra.
- El relleno y el borde van alrededor: no deforman la imagen.
- En un `Flex` en fila, una imagen no se estira a lo alto: conserva su proporción.

## Entre páginas

Una imagen no se parte: si no cabe en lo que queda de la página, pasa entera a la siguiente. Una imagen más alta que una
página vacía es un error.

## SVG

Un SVG va en el PDF como dibujo vectorial: se ve nítido a cualquier tamaño y pesa poco. Se admite un subconjunto cerrado:
formas, rutas, grupos, degradados, recortes, `use` y texto. Lo demás (scripts, filtros, máscaras, patrones, imágenes, hojas de
estilo, referencias a otros ficheros) es un error, así que un SVG exportado de un editor a veces hay que limpiarlo. El texto
de un SVG tiene que usar una fuente de `fonts/`, y se dibuja como contorno: se ve igual, pero no se puede seleccionar.

## Accesibilidad

Una imagen con `Alt` es una figura del PDF etiquetado, con ese texto alternativo. Sin `Alt` es decoración: un fondo, un
adorno. Escribe `Alt` en toda imagen que diga algo.

## Límites

Por seguridad, una imagen no puede tener más de 25 millones de píxeles, y un SVG, más de 20 000 elementos ni 64 niveles de
anidado ([Uso desde Java](../comun/uso-desde-java.md) para cambiarlo).

Más detalle en la [referencia](../../referencia/componentes/image.md).
