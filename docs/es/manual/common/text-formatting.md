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
| `[texto]{precio}` | el texto con el color, la fuente y el resto de la regla `For style: precio` ([abajo](#partes-con-otro-color-u-otra-fuente)) |
| `![texto alternativo](ruta)` | una imagen pequeña dentro de la frase, tan alta como la letra ([abajo](#imágenes-en-la-frase)) |

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

La negrita y la cursiva se suman a las de la caja: dentro de un título en negrita, `**x**` sigue en negrita, y en un texto
ligero (`Weight: 300`), la negrita es el peso 700 ([Tipografía](typography.md#pesos)). El tamaño, el color y la fuente
son los de la caja, salvo en una [parte con estilo](#partes-con-otro-color-u-otra-fuente).

## Partes con otro color u otra fuente

Con `Markup: markdown`, un texto entre corchetes seguido del nombre de un estilo entre llaves, `[12,50 €]{precio}`, toma lo
que declara la regla `For style: precio` de `Styles`: el color, la fuente, el peso, la cursiva, las mayúsculas, las
versalitas o el interletraje. El tamaño y el interlineado siguen siendo los de la caja, así que la línea no cambia de alto.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Partes con estilo
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 11
                Markup: markdown
        For style: precio
            Typography:
                Color: #B3261E
                Bold: true
        For style: marca
            Typography:
                Font: IBM Plex Serif
                Italic: true
    Section:
        Paragraph: La suscripción a [La Gaceta]{marca} cuesta [12,50 €]{precio} al mes, con el IVA incluido.
```

- La regla es una regla `For style` como las demás: la misma puede dar estilo a una caja (`Style: precio`) y a una parte
  de un texto. En una parte solo valen `Font`, `Code font`, `Bold`, `Weight`, `Italic`, `Color`, `Case`, `Small caps`,
  `Letter spacing` y `Position`. Si la regla declara algo más, como `Size` o un `Box`, y se usa en una parte, es un error
  (`STYLE_NOT_ALLOWED_IN_RUN`).
- Las marcas se suman al estilo: `[**12,50 €**]{precio}` y `**[12,50 €]{precio}**` van en negrita aunque la regla diga
  `Weight: 300`. Un enlace puede llevar una parte con estilo (`[[ver precio]{precio}](https://...)`) y una parte con estilo
  puede llevar un enlace, pero una parte con estilo no puede ir dentro de otra.
- Un nombre sin su regla es un error (`STYLE_WITHOUT_RULE`). Unos corchetes que no van seguidos de un nombre entre llaves
  son texto, y `\[` los escribe sin más.

## Superíndices y subíndices (`Position`)

Una parte con estilo cuya regla declara `Position: superscript` o `subscript` se pinta más pequeña (al 70 % del tamaño)
y por encima o por debajo de la línea base: ordinales, unidades, fórmulas y marcas.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Superíndices y subíndices
    Page:
        Paper: A5
        Margin: 15mm
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 11
                Markup: markdown
        For style: sup
            Typography:
                Position: superscript
        For style: sub
            Typography:
                Position: subscript
    Section:
        Paragraph: El 1[er]{sup} piso, de 85 m[2]{sup}, emite 12 kg de CO[2]{sub} al año.
```

- La línea no cambia de alto, y lo que se copia del PDF es el texto escrito (`85 m2`).
- La regla puede llevar también un color o una fuente, y las marcas se suman: `[**2**]{sub}` es un subíndice en negrita.
- `Position` solo vale en una parte de un texto. En una caja (en su `Typography`, o con `Style: sup`) es un error
  (`POSITION_OUTSIDE_RUN`).
- El número de una [nota al pie](../components/footnote.md) ya va en superíndice, sin ninguna regla.

## Imágenes en la frase

Un icono entre las palabras, o un logotipo pequeño en un título, se escribe como en Markdown, con la ruta de la imagen
en `images/` de la carpeta de recursos ([Recursos](resources.md)):

```
Paragraph: Incluido ![Sí](icons/check.svg) en todos los planes.
	Typography:
		Markup: markdown
```

La imagen es tan alta como la letra y se alinea con ella; su ancho sale de su proporción. Se comporta como una palabra
más: si no cabe, pasa a la línea siguiente. Vale un PNG, un JPEG o un SVG. El texto entre corchetes es lo que lee un
lector de pantalla; con los corchetes vacíos, `![](ruta)`, la imagen es decorativa y no se lee. Dentro de un enlace,
`[![texto](ruta)](destino)`, se puede pulsar. Una imagen con su propio tamaño, o sola, es una
[`Image`](../components/image.md).

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
con código es un error. Dentro de una [parte con estilo](#partes-con-otro-color-u-otra-fuente) vale el `Code font` de su
regla, si lo declara.

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
letra es la suya. Con `Ligatures: true` se usan las de la fuente (IBM Plex Sans tiene `fi`, e IBM Plex Serif, `fi` y `fl`).
No cambian lo que se lee ni lo que se copia, y no se aplican con interletraje.

## Texto preformateado

Para código, salidas de terminal o columnas alineadas con espacios. Con `Preformatted: keep`, el texto **conserva sus espacios
y sus saltos de línea**: cada línea escrita es una línea en el PDF, también las vacías. Con `Preformatted: wrap`, además, una línea
demasiado larga se parte entre palabras. Lo normal es usar una fuente monoespaciada y escribir el texto en un `Content >>`:

```
Text:
    Typography:
        Font: IBM Plex Mono
        Size: 9
        Preformatted: keep
    Content >>
        $ termostato --estado
        temperatura   21,5 °C
        consigna      22,0 °C
```

- Con `Preformatted: keep`, una línea más ancha que la caja es un error. Hay que acortarla o usar `Preformatted: wrap`.
- `Tab size` (4 por defecto) es el número de columnas de un tabulador: cada tabulador lleva a la siguiente columna múltiplo
  de `Tab size`, como en un editor.
- Las líneas preformateadas no se justifican.
- Un bloque preformateado largo se parte entre páginas como cualquier texto, y cuenta como un solo párrafo para las viudas
  y huérfanas.

## Subrayado y tachado

Son rayas del color del texto, con la posición y el grosor que dice la fuente. Se escriben con `Markup: markdown` o en un
`Markdown`. No hay estilos de raya.

Más detalle en la [referencia](../../reference/common/text-formatting.md).
