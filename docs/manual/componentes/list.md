# `List`

Una lista con viñetas o numerada. Cada `List item` es un ítem, con un marcador (la viñeta o el número) que pone el motor.

```
Document (dev.stxt.pdf):
	Language: es
	Title: Pasos
	Page:
		Paper: A4
		Margin: 50
	Styles:
		For element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
		For element: List
			Box:
				Padding: 0 0 0 18
		For element: List item
			Box:
				Margin: 0 0 4 0
	Section:
		List:
			Marker: decimal
			List item:
				Paragraph: Corta la corriente.
			List item:
				Paragraph: Retira la tapa del termostato.
				List:
					Marker: dash
					List item:
						Paragraph: Guarda los tornillos.
					List item:
						Paragraph: No toques los cables.
			List item:
				Paragraph: Conecta el cable rojo al borne L.
```

## Propiedades

| Propiedad | Dónde | Qué hace |
|---|---|---|
| `Marker` | `List` | **Obligatorio.** `bullet` (•), `dash` (–), `none`, `decimal` (1.), `lower-alpha` (a.), `upper-alpha` (A.), `lower-roman` (i.), `upper-roman` (I.) |
| `Start` | `List` | El número del primer ítem (1 por defecto). Solo en las numeradas |
| `Number` | `List item` | El número de este ítem; los siguientes siguen desde él |

`List` y `List item` admiten además `Id`, `Style`, `Link`, `Box`, `Typography` y `Break`. Un ítem tiene uno o varios
elementos (un `Paragraph`, un `Text`, otra `List`…), pero no encabezados.

## La sangría

El marcador cuelga **a la izquierda del ítem**, en el hueco que deja el `Padding` izquierdo de la `List`. Sin ese relleno no
hay sitio para el marcador y es un error, así que lo normal es darlo en una regla `For element: List`, como en el ejemplo. Los
marcadores quedan alineados a la derecha, pegados al texto con un espacio.

## Cómo se comporta

- El marcador usa la tipografía del ítem y se alinea con su primera línea. Por eso un ítem tiene que empezar con un texto:
  si empieza con un `Flex`, un `Grid` o una imagen, es un error.
- Cada lista numera sus ítems; una lista dentro de otra empieza de nuevo.
- Si el ítem se parte entre páginas, el marcador va en la primera.
- `{number of: id}` escribe el número de un ítem con `Id` (`3`, `c`, `IV`): «véase la cláusula {number of: pago}».

Más detalle en la [referencia](../../referencia/componentes/list.md).
