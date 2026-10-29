# Tipografía: `Typography`

El texto y cómo se reparte en líneas. **Es el único grupo que se hereda**: lo que declara un elemento vale para él y
para todos sus descendientes que no lo cambien. La base que hereda todo el documento se declara en la regla
`Element: Document` de `Styles` ([Styles](estilos.md#la-tipografía-base)), y cada elemento declara solo lo que cambia.
Lo que una regla de `Styles` da a un elemento en `Typography` lo heredan sus descendientes como si lo hubiera declarado
él.

`Typography` se admite en todos los elementos de caja salvo `Image`, que no tiene texto, y en `Row` (lo heredan sus
celdas).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Font` | nombre de familia | — | **Obligatoria** en todo texto, propia o heredada (`TEXT_WITHOUT_FONT`). Es la familia que declara el fichero TTF de la carpeta de fuentes ([Recursos](recursos.md#fuentes)). |
| `Weight` | `normal`, `bold` | `normal` | Peso: `normal` es el 400 de la fuente y `bold`, el 700. |
| `Style` | `normal`, `italic` | `normal` | |
| `Size` | número > 0 | — | Cuerpo de la fuente. **Obligatorio** en todo texto, propio o heredado (`TEXT_WITHOUT_FONT_SIZE`). |
| `Line height` | número o % | `120%` | **Un número son puntos** (`15` = 15 pt). Para un valor relativo al `Size`, porcentaje (`120%`). |
| `Align` | `left`, `right`, `center`, `justify` | `left` | La última línea de un párrafo justificado se alinea a la izquierda. |
| `Color` | color | `#000000` | Color del texto ([Valores](valores.md#colores)). |
| `Text wrap` | `auto`, `pretty` | `auto` | Reparto en líneas: voraz (`auto`) o Knuth-Plass (`pretty`). |
| `Inline` | `none`, `markdown` | `none` | Formato dentro de la línea ([Formato del texto](formato-del-texto.md#formato-en-línea-inline)). |
| `Transform` | `none`, `uppercase`, `lowercase`, `capitalize` | `none` | Mayúsculas al pintar ([Formato del texto](formato-del-texto.md#mayúsculas-transform)). |
| `Letter spacing` | número (puntos) | `0` | Interletraje; puede ser negativo ([Formato del texto](formato-del-texto.md#interletraje-letter-spacing)). |
| `Small caps` | `true`, `false` | `false` | Versalitas ([Formato del texto](formato-del-texto.md#versalitas-small-caps)). |
| `Ligatures` | `true`, `false` | `false` | Ligaduras de la fuente ([Formato del texto](formato-del-texto.md#ligaduras-ligatures)). |
| `White space` | `normal`, `pre`, `pre-wrap` | `normal` | Texto preformateado ([Formato del texto](formato-del-texto.md#texto-preformateado-white-space)). |
| `Tab size` | entero ≥ 1 | `4` | Con `pre` o `pre-wrap`, las columnas de una tabulación. |
| `Numbered` | `true`, `false` | `false` | Si los encabezados llevan número ([Encabezados](../componentes/heading.md#numeración-numbered)). |
| `Widows` | entero ≥ 1 | `2` | Mínimo de líneas de un párrafo que pasan a la página siguiente ([Cortes](cortes.md#elección-del-corte)). |
| `Orphans` | entero ≥ 1 | `2` | Mínimo de líneas de un párrafo que quedan al final de una página. |
| `Language` | etiqueta BCP 47 (`es`, `en-GB`, `ca`…) | el `Language` de `Document` | Idioma del texto de la caja, cuando no es el del documento ([idioma](#idioma-language)). |

Lo que no tiene texto ignora lo que hereda sin error: un `Block` hereda `Size` para sus hijos, y un `Text` que hereda
`Numbered: true` no hace nada, porque solo lo lee un encabezado.

## Dónde hay texto

Tienen texto `Text`, `Paragraph` y una `Cell` con valor ([Text](../componentes/text.md)), y lo generan los encabezados
(sus `Text`), las entradas de un `Contents` y lo que traduce un `Markdown`. El texto de un `Text` va en su valor
(`Text: Un título`) o en un hijo `Content >>` con un bloque indentado, que admite párrafos.

## Espacios y párrafos

Las secuencias de espacios y los saltos de línea simples equivalen a un espacio. Una **línea en blanco** separa párrafos
y fuerza un salto. Con `White space: pre` o `pre-wrap` se conservan
([Formato del texto](formato-del-texto.md#texto-preformateado-white-space)).

## El reparto en líneas

El texto se reparte en **líneas** del ancho de la caja de contenido:

- **`Text wrap: auto`**, algoritmo voraz: las palabras se añaden a la línea mientras quepan.
- **`Text wrap: pretty`**, **Knuth-Plass**: se eligen los saltos del párrafo que minimizan la suma de los deméritos de sus
  líneas, de modo que las líneas quedan más parejas (sin guiones; los espacios se estiran hasta la mitad de su ancho y no
  se encogen; la última línea no cuenta).

En los dos casos, una palabra más ancha que la línea es un error (`TEXT_OVERFLOW`, [Desbordamiento](desbordamiento.md)).
No se parten palabras ni se ponen guiones.

## Altura de las líneas y línea base

Cada línea mide `Line height`, y la altura del contenido es el número de líneas por `Line height`. La línea base no
afecta al layout, solo al pintado: la caja de la fuente (su ascendente más su descendente, de la tabla `hhea` del
fichero TTF) se centra en la línea:

```
línea base = arriba + (Line height − (ascendente + descendente) · Size) / 2 + ascendente · Size
```

> **Decisión:** el ascendente y el descendente salen de la tabla `hhea` de la fuente, no de los de `OS/2` (`sTypo*`, ni
> cuando la fuente marca `USE_TYPO_METRICS`; ni `usWin*`). En IBM Plex Sans, `hhea` da 1025 y 275 por 1000, igual que
> `usWin*`; `sTypo*` da 780 y 220. *Alternativa descartada:* `sTypo*` cuando la fuente marca `USE_TYPO_METRICS`.

## Alineación

`left`, `right` y `center` desplazan cada línea; `justify` reparte el sobrante a partes iguales entre los espacios de la
línea. No se justifican la última línea de cada párrafo, las líneas de una sola palabra ni las que desbordan (los
espacios no se comprimen). La última línea de una página no es la última de su párrafo si este continúa en la página
siguiente, así que se justifica.

## Medida del texto

El ancho de un texto es la suma de los avances de sus glifos, redondeados a milésimas del cuerpo; sin kerning, y con
ligaduras solo si se piden ([Formato del texto](formato-del-texto.md#ligaduras-ligatures)). Una palabra con partes de
varios tramos mide lo que suman sus partes. Lo que se mide es exactamente lo que se pinta.

> **Decisión:** el avance de cada glifo se redondea a milésimas del cuerpo, que es lo que escribe el PDF en la tabla de
> anchos de la fuente incrustada, así que maquetar y pintar miden lo mismo con fuentes de cualquier `unitsPerEm`; el
> kerning no se aplica. *Alternativa descartada:* aplicar el kerning de las tablas `kern`/`GPOS`, que obligaría a
> colocar cada glifo por separado en el PDF.

## Tamaños intrínsecos

min-content = la palabra más ancha; max-content = el párrafo más largo en una sola línea (con `pre` y `pre-wrap`, ver
[Formato del texto](formato-del-texto.md#tamaños-intrínsecos)).

## Idioma (`Language`)

Una caja cuyo texto no está en el idioma del documento declara el suyo en `Typography` (`Language: en-GB`), también desde
una regla de `Styles` (`For class: english`). Se hereda, y lo que hay dentro lo usa para las reglas de las letras de
`Transform`. En el PDF etiquetado es el atributo `Lang` del elemento de la caja, solo donde el idioma cambia respecto al
de fuera (los elementos de dentro lo heredan), para que un lector de pantalla cambie de voz. Es una etiqueta BCP 47
(`VALUE_NOT_ALLOWED`) y `Element: Document` no la admite (`STYLE_NOT_ALLOWED`): el idioma del documento es el `Language`
de `Document` ([Document](../componentes/document.md#title-y-language)).

> **Decisión:** `Language` en `Typography` reutiliza el nombre y la definición del `Language` de `Document` (la misma
> etiqueta y el mismo error), cambia las reglas de las letras de `Transform` de lo que contiene y es el `Lang` de su
> elemento del PDF etiquetado. No hay idioma por tramo dentro de una línea ni en Markdown. *Alternativa descartada:* un
> idioma por tramo, que parte el contenido marcado del PDF.

## Lo que no hay

- Fuentes de sustitución, síntesis de negrita o cursiva, pesos distintos de 400 y 700 ([Recursos](recursos.md#fuentes)).
- Unidades relativas a la fuente: todo se mide en puntos o en porcentajes.
- Guiones al partir palabras, kerning y escritura vertical o de derecha a izquierda.
- Color o tamaño distintos dentro de una línea: un tramo solo cambia negrita, cursiva, subrayado y tachado
  ([Formato del texto](formato-del-texto.md#tramos)).
