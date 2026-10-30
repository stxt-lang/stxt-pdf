# `Flex`

Un `Flex` coloca sus ítems **en fila** (o en columna) y reparte entre ellos el espacio que sobra o que falta. Sirve para
poner cosas una al lado de otra: un logotipo y unos datos, tres cifras, un precio a la derecha de un plato.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Cifras
	Page:
		Paper: A4
		Margin: 50
	Styles:
		For element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
		For style: figure
			Box:
				Padding: 10
				Background: #F1EFE8
				Radius: 4
			Typography:
				Align: center
	Section:
		Flex:
			Gap: 12
			Item:
				Grow: 1
				Basis: 0
				Text: 12 % más de ventas
					Style: figure
			Item:
				Grow: 1
				Basis: 0
				Text: 3 tiendas nuevas
					Style: figure
			Item:
				Grow: 1
				Basis: 0
				Text: 98 % de clientes satisfechos
					Style: figure
		Flex:
			Horizontal: between
			Box:
				Margin: 20 0 0 0
			Item:
				Text: Risotto de setas
			Item:
				Text: 14,50 €
```

Arriba, tres ítems del mismo ancho; abajo, un plato a la izquierda y su precio a la derecha.

## `Item`

Cada hijo de un `Flex` va dentro de un `Item`, que envuelve **exactamente un elemento**. El `Item` dice cómo participa en el
`Flex`; el resto (tamaño, márgenes, `Id`, estilo) va en el elemento de dentro.

| Propiedad del `Item` | Por defecto | Qué hace |
|---|---|---|
| `Grow` | `0` | Qué parte del espacio que sobra recibe. Con `Grow: 1` en todos, se reparte a partes iguales |
| `Shrink` | `1` | Qué parte del espacio que falta cede |
| `Basis` | `auto` | El tamaño de partida: `auto` es su tamaño (el declarado o el de su contenido); `0` hace que todo el espacio se reparta según `Grow` |
| `Horizontal`, `Vertical` | el del `Flex` | Su alineación en el otro eje: `Vertical` en un `Flex` en fila, `Horizontal` en uno en columna. En la dirección del `Flex` es un error: ahí el ítem crece o encoge con `Grow` y `Shrink` |
| `Layer` | `auto` | Qué se pinta encima ([Capas y giro](../comun/capas-y-giro.md)) |

## Propiedades del `Flex`

| Propiedad | Valores | Por defecto | Qué hace |
|---|---|---|---|
| `Direction` | `horizontal`, `vertical` | `horizontal` | En fila (`horizontal`) o en columna (`vertical`) |
| `Gap` | 1 o 2 medidas | `0` | El hueco entre ítems, y entre líneas si hay varias. Con `Wrap`, puede tener dos: entre filas y entre columnas |
| `Wrap` | `true`, `false` | `false` | Si los ítems que no caben pasan a otra línea |
| `Horizontal` | `start`, `end`, `center`, `between`, `around`, `evenly`; en columna, `start`, `end`, `center`, `stretch` | `start`; en columna, `stretch` | En fila, dónde va lo que sobra: al principio, al final, a los dos lados, entre los ítems (`between`)… En columna, cómo se alinean los ítems: `stretch` los estira al ancho del `Flex` |
| `Vertical` | `start`, `end`, `center`, `stretch`; en columna, `start`, `end`, `center`, `between`, `around`, `evenly` | `stretch`; en columna, `start` | Al revés que `Horizontal`: en fila, cómo se alinean los ítems (`stretch` los estira al alto de la fila); en columna, dónde va lo que sobra |
| `Lines` | `start`, `end`, `center`, `between`, `around`, `stretch` | `stretch` | Con `Wrap`, cómo se reparten las líneas |

## Cómo se reparte el espacio

1. Cada ítem parte de su `Basis`: su tamaño declarado o, si no tiene, lo que mide su contenido sin partir líneas.
2. Si sobra espacio, se reparte según `Grow`; si falta, los ítems ceden según `Shrink` (y según su tamaño: uno grande cede
   más).
3. Un ítem nunca se estrecha más que su palabra más larga, salvo que declare `Min width: 0`.
4. Si aun así no caben, es un error.

El reparto de lo que sobra solo actúa si ningún ítem crece. En fila, los ítems se estiran al alto de la fila
(`Vertical: stretch`), salvo las imágenes, que conservan su proporción.

## Cosas a tener en cuenta

- `Grow` en un `Flex` en columna necesita que el `Flex` tenga un alto en puntos; si no, no hay nada que repartir y es un error.
- Los ítems no admiten `Margin: auto`: se alinean con `Horizontal` y `Vertical`.
- Entre páginas, un `Flex` en columna se corta entre ítems, y uno en fila, entre líneas; una fila que no cabe en una página
  entera se parte con sus ítems a la vez.

Más detalle en la [referencia](../../referencia/componentes/flex.md).
