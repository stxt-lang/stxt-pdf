# Formato del texto

Todas estas propiedades son de `Typography`: se heredan y se pueden poner en una regla de `Styles`.

## Negrita, cursiva y más dentro de una frase (`Inline`)

Por defecto el texto es literal: un asterisco es un asterisco. Con `Inline: markdown`, el texto de un `Text`, un
`Paragraph` o una `Cell` entiende las marcas de Markdown dentro de la línea:

| Se escribe | Sale |
|---|---|
| `**negrita**` | **negrita** |
| `*cursiva*` | *cursiva* |
| `++subrayado++` | subrayado |
| `~~tachado~~` | ~~tachado~~ |
| `[texto](https://…)` | un enlace, subrayado ([Referencias y enlaces](referencias-y-enlaces.md)) |

```
Styles:
	Element: Paragraph
		Typography:
			Inline: markdown
Paragraph: El pago se hace en **30 días**, salvo *acuerdo* en contra.
```

Las marcas se pueden combinar (`***negrita y cursiva***`) y no se ven en el PDF. Para escribir un asterisco literal, ponle
una barra delante (`\*`). Solo vale lo que cabe dentro de una línea: un título, una lista o una tabla van en un
[`Markdown`](../componentes/markdown.md). Si un párrafo con `Inline: markdown` empieza por `-`, `#`, `>` o `1.`, escapa el
primer carácter (`\-`). Un texto concreto vuelve a ser literal con `Inline: none`.

La negrita y la cursiva se suman a las de la caja: dentro de un título en negrita, `**x**` sigue en negrita. El tamaño, el
color y la fuente son los de la caja.

## Mayúsculas (`Transform`)

`Transform: uppercase` pinta el texto en mayúsculas, `lowercase` en minúsculas y `capitalize` pone en mayúscula la primera
letra de cada palabra. El texto se escribe normal y la regla lo transforma, así que un lector de pantalla lee «Emisor» y no
«EMISOR». Las reglas de cada letra son las del idioma de la caja.

```
Styles:
	For class: label
		Typography:
			Transform: uppercase
			Letter spacing: 1
			Size: 8
Text: Datos del cliente
	Class: label
```

## Versalitas (`Small caps`)

`Small caps: true` pinta las minúsculas como mayúsculas más pequeñas (al 80 % del tamaño). Las mayúsculas, las cifras y los
signos no cambian.

## Interletraje (`Letter spacing`)

Puntos que se añaden entre cada dos letras; puede ser negativo para apretar el texto. La última letra de cada línea no lo
lleva, así que un texto alineado a la derecha acaba justo en la última letra.

## Ligaduras (`Ligatures`)

Algunas fuentes tienen glifos especiales para pares de letras (`fi`, `fl`): las **ligaduras**. Por defecto no se usan y cada
letra es la suya. Con `Ligatures: true` se usan las de la fuente (IBM Plex Sans y Serif tienen `fi`, `fl`, `ffi` y `ffl`).
No cambian lo que se lee ni lo que se copia, y no se aplican con interletraje.

## Texto preformateado (`White space`)

Para código, salidas de terminal o columnas alineadas con espacios. Con `White space: pre`, el texto **conserva sus espacios
y sus saltos de línea**: cada línea escrita es una línea en el PDF, también las vacías. Con `pre-wrap`, además, una línea
demasiado larga se parte entre palabras. Lo normal es usar una fuente monoespaciada y escribir el texto en un `Content >>`:

```
Text:
	Typography:
		Font: IBM Plex Mono
		Size: 9
		White space: pre
	Content >>
		$ termostato --estado
		temperatura   21,5 °C
		consigna      22,0 °C
```

- Con `pre`, una línea más ancha que la caja es un error: hazla más corta o usa `pre-wrap`.
- `Tab size` (4 por defecto) es el número de columnas de un tabulador: cada tabulador lleva a la siguiente columna múltiplo
  de `Tab size`, como en un editor.
- Las líneas preformateadas no se justifican.
- Un bloque preformateado largo se parte entre páginas como cualquier texto, y cuenta como un solo párrafo para las viudas
  y huérfanas.

## Subrayado y tachado

Son rayas del color del texto, con la posición y el grosor que dice la fuente. Se escriben con `Inline: markdown` o en un
`Markdown`; no hay estilos de raya.

Más detalle en la [referencia](../../referencia/comun/formato-del-texto.md).
