# Formato del texto

Todas estas propiedades son de `Typography`: se heredan y se pueden poner en una regla de `Styles`.

## Negrita, cursiva y más dentro de una frase (`Markup`)

Por defecto el texto es literal: un asterisco es un asterisco. Con `Markup: markdown`, el texto de un `Text`, un
`Paragraph` o una `Cell` entiende las marcas de Markdown dentro de la línea:

| Se escribe | Sale |
|---|---|
| `**negrita**` | **negrita** |
| `*cursiva*` | *cursiva* |
| `++subrayado++` | subrayado |
| `~~tachado~~` | ~~tachado~~ |
| `[texto](https://...)` | un enlace, subrayado ([Referencias y enlaces](references-and-links.md)) |
| `` `código` `` | código, en la fuente de `Code font` ([abajo](#código)) |

```
Styles:
    For element: Paragraph
        Typography:
            Markup: markdown
Paragraph: El pago se hace en **30 días**, salvo *acuerdo* en contra.
```

Las marcas se pueden combinar (`***negrita y cursiva***`) y no se ven en el PDF. Un asterisco literal se escribe con una
barra delante (`\*`). Solo vale lo que cabe dentro de una línea: un título, una lista o una tabla van en un
[`Markdown`](../components/markdown.md). Lo demás es texto tal cual: `2024. Un buen año` o `- 5 % de descuento` se ven
como se escriben, sin escapar nada. Un texto concreto vuelve a ser literal con `Markup: none`.

La negrita y la cursiva se suman a las de la caja: dentro de un título en negrita, `**x**` sigue en negrita. El tamaño, el
color y la fuente son los de la caja.

## Código

Con `Markup: markdown`, lo que va entre comillas invertidas es código y se escribe en la fuente de `Code font`, que
suele ser una monoespaciada. `Code font` va en `Typography` y se hereda: se declara una vez para todo el documento, y
vale también para el código de un [`Markdown`](../components/markdown.md).

```
Styles:
    For element: Document
        Typography:
            Font: IBM Plex Sans
            Size: 10
            Markup: markdown
            Code font: IBM Plex Mono
Paragraph: Ejecuta `mvn test` antes de publicar.
```

El código conserva la negrita y la cursiva de lo que lo rodea y no cambia el alto de la línea. Sin `Code font`, un texto
con código es un error.

## Mayúsculas (`Case`)

`Case: uppercase` pinta el texto en mayúsculas, `lowercase` en minúsculas y `capitalize` pone en mayúscula la primera
letra de cada palabra. El texto se escribe normal y la regla lo transforma, así que un lector de pantalla lee «Emisor» y no
«EMISOR». Las reglas de cada letra son las del idioma de la caja.

```
Styles:
    For style: label
        Typography:
            Case: uppercase
            Letter spacing: 1
            Size: 8
Text: Datos del cliente
    Style: label
```

## Versalitas (`Small caps`)

`Small caps: true` pinta las minúsculas como mayúsculas más pequeñas (al 80 % del tamaño). Las mayúsculas, las cifras y los
signos no cambian.

## Interletraje (`Letter spacing`)

Puntos que se añaden entre cada dos letras. Puede ser negativo para apretar el texto. La última letra de cada línea no lo
lleva, así que un texto alineado a la derecha acaba justo en la última letra.

## Ligaduras (`Ligatures`)

Algunas fuentes tienen glifos especiales para pares de letras (`fi`, `fl`): las **ligaduras**. Por defecto no se usan y cada
letra es la suya. Con `Ligatures: true` se usan las de la fuente (IBM Plex Sans y Serif tienen `fi`, `fl`, `ffi` y `ffl`).
No cambian lo que se lee ni lo que se copia, y no se aplican con interletraje.

## Texto preformateado

Para código, salidas de terminal o columnas alineadas con espacios. Con `Preformatted: true`, el texto **conserva sus espacios
y sus saltos de línea**: cada línea escrita es una línea en el PDF, también las vacías. Con `Preformatted: wrap`, además, una línea
demasiado larga se parte entre palabras. Lo normal es usar una fuente monoespaciada y escribir el texto en un `Content >>`:

```
Text:
    Typography:
        Font: IBM Plex Mono
        Size: 9
        Preformatted: true
    Content >>
        $ termostato --estado
        temperatura   21,5 °C
        consigna      22,0 °C
```

- Con `Preformatted: true`, una línea más ancha que la caja es un error. Hay que acortarla o usar `Preformatted: wrap`.
- `Tab size` (4 por defecto) es el número de columnas de un tabulador: cada tabulador lleva a la siguiente columna múltiplo
  de `Tab size`, como en un editor.
- Las líneas preformateadas no se justifican.
- Un bloque preformateado largo se parte entre páginas como cualquier texto, y cuenta como un solo párrafo para las viudas
  y huérfanas.

## Subrayado y tachado

Son rayas del color del texto, con la posición y el grosor que dice la fuente. Se escriben con `Markup: markdown` o en un
`Markdown`. No hay estilos de raya.

Más detalle en la [referencia](../../reference/common/text-formatting.md).
