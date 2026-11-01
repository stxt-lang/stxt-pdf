# Tipografía: `Typography`

El texto y cómo se reparte en líneas. **Es el único grupo que se hereda**: lo que declara un elemento vale para él y
para todos sus descendientes que no lo cambien. La base que hereda todo el documento se declara en la regla
`For element: Document` de `Styles` ([Styles](styles.md#la-tipografía-base)), y cada elemento declara solo lo que cambia.
Lo que una regla de `Styles` da a un elemento en `Typography` lo heredan sus descendientes como si lo hubiera declarado
él.

`Typography` se admite en todos los elementos de caja salvo `Image`, que no tiene texto, y en `Row` (lo heredan sus
celdas).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Font` | nombre de familia | sin valor | **Obligatoria** en todo texto, propia o heredada (`TEXT_WITHOUT_FONT`). Es la familia que declaran los ficheros TTF u OTF de la carpeta de fuentes ([Recursos](resources.md#fuentes)). |
| `Code font` | nombre de familia | sin valor | La familia del código, en línea y en bloques ([Formato del texto](text-formatting.md#código-en-línea)). Suele ser una monoespaciada. **Obligatoria** si hay código, propia o heredada (`CODE_WITHOUT_FONT`). |
| `Bold` | `true`, `false` | `false` | Negrita: `true` es `Weight: 700` y `false`, `Weight: 400`. No va con `Weight` en el mismo `Typography` (`WEIGHT_CONFLICT`). |
| `Weight` | de 100 a 900, de 100 en 100 | `400` | Peso de la fuente ([Pesos](#pesos-weight)). |
| `Italic` | `true`, `false` | `false` | Cursiva: la variante cursiva de la fuente. |
| `Size` | número > 0 | sin valor | Cuerpo de la fuente. **Obligatorio** en todo texto, propio o heredado (`TEXT_WITHOUT_FONT_SIZE`). |
| `Line height` | número o % | `120%` | **Un número es una medida** (`15` = 15 pt). Para un valor relativo al `Size`, porcentaje (`120%`). |
| `Align` | `left`, `right`, `center`, `justify` | `left` | La última línea de un párrafo justificado se alinea a la izquierda. |
| `Color` | color | `#000000` | Color del texto ([Valores](values.md#colores)). |
| `Line breaking` | `simple`, `balanced` | `simple` | Reparto en líneas: voraz (`simple`) o Knuth-Plass (`balanced`). |
| `Hyphenate` | `true`, `false` | `false` | Partir palabras con guion al final de la línea, con los patrones del idioma ([Guiones](#guiones-hyphenate)). |
| `Markup` | `none`, `markdown` | `none` | Formato dentro de la línea ([Formato del texto](text-formatting.md#formato-en-línea-markup)). |
| `Case` | `none`, `uppercase`, `lowercase`, `capitalize` | `none` | Mayúsculas al pintar ([Formato del texto](text-formatting.md#mayúsculas-case)). |
| `Letter spacing` | número | `0` | Interletraje; puede ser negativo ([Formato del texto](text-formatting.md#interletraje-letter-spacing)). |
| `Small caps` | `true`, `false` | `false` | Versalitas ([Formato del texto](text-formatting.md#versalitas-small-caps)). |
| `Ligatures` | `true`, `false` | `false` | Ligaduras de la fuente ([Formato del texto](text-formatting.md#ligaduras-ligatures)). |
| `Preformatted` | `false`, `true`, `wrap` | `false` | Texto preformateado ([Formato del texto](text-formatting.md#texto-preformateado)). |
| `Tab size` | entero ≥ 1 | `4` | Con `Preformatted: true` o `wrap`, las columnas de una tabulación. |
| `Numbered` | `true`, `false` | `false` | Si los encabezados llevan número ([Encabezados](../components/heading.md#numeración-numbered)). |
| `Number format` | `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | `decimal` | Cómo se escribe el número del nivel de un encabezado ([Encabezados](../components/heading.md#formato-y-etiqueta-number-format-number-label)). |
| `Number label` | texto con `{number}` | el número | Lo que va delante del título de un encabezado numerado ([Encabezados](../components/heading.md#formato-y-etiqueta-number-format-number-label)). |
| `Widows` | entero ≥ 1 | `2` | Mínimo de líneas de un párrafo que pasan a la página siguiente ([Cortes](breaks.md#elección-del-corte)). |
| `Orphans` | entero ≥ 1 | `2` | Mínimo de líneas de un párrafo que quedan al final de una página. |
| `Language` | etiqueta BCP 47 (`es`, `en-GB`, `ca`) | el `Language` de `Document` | Idioma del texto de la caja, cuando no es el del documento ([idioma](#idioma-language)). |

Lo que no tiene texto ignora lo que hereda sin error: un `Block` hereda `Size` para sus hijos, y un `Text` que hereda
`Numbered: true` no hace nada, porque solo lo lee un encabezado.

## Dónde hay texto

Tienen texto `Text`, `Paragraph` y una `Cell` con valor ([Text](../components/text.md)), y lo generan los encabezados
(sus `Text`), las entradas de un `Table of contents` y lo que traduce un `Markdown`. El texto de un `Text` va en su valor
(`Text: Un título`) o en un hijo `Content >>` con un bloque indentado, sin líneas en blanco: cada caja es un párrafo.

## Espacios y párrafos

Las secuencias de espacios y los saltos de línea simples equivalen a un espacio. Una **línea en blanco** es un error
(`TEXT_BLANK_LINE`): cada `Text` y cada `Paragraph` es un solo párrafo. Con `Preformatted: true` o `wrap` se
conservan los espacios y los saltos de línea, también las líneas en blanco de un `Text`
([Formato del texto](text-formatting.md#texto-preformateado)).

## El reparto en líneas

El texto se reparte en **líneas** del ancho de la caja de contenido:

- **`Line breaking: simple`**, algoritmo voraz: las palabras se añaden a la línea mientras quepan.
- **`Line breaking: balanced`**, **Knuth-Plass**: se eligen los saltos del párrafo que minimizan la suma de los deméritos de sus
  líneas, de modo que las líneas quedan más parejas. Los espacios se estiran hasta la mitad de su ancho y no se encogen,
  y la última línea no cuenta.

En los dos casos, una palabra más ancha que la línea es un error (`TEXT_OVERFLOW`, [Desbordamiento](overflow.md)),
también con `Hyphenate` si ninguna de sus partes cabe. Las palabras solo se parten con `Hyphenate` o donde el texto
lleva un guion blando ([Guiones](#guiones-hyphenate)).

## Pesos (`Weight`)

`Weight` elige el peso de la fuente: de 100, el más fino, a 900, el más grueso, de 100 en 100; otro número es un error
(`VALUE_NOT_ALLOWED`). El 400 es el normal y el 700, la negrita. Cada peso es un fichero de la familia en la carpeta de
fuentes: el repositorio trae IBM Plex Sans en 300, 400, 500, 600 y 700 ([Recursos](resources.md#fuentes)). Un peso que
la familia no tiene es un error (`FONT_VARIANT_MISSING`): no se sintetiza ni se usa el más cercano.

```stxt
Typography:
    Font: IBM Plex Sans
    Weight: 300
```

- **`Bold` es otra forma de escribir el peso**: `Bold: true` es `Weight: 700`, y `Bold: false`, `Weight: 400`. Son la
  misma propiedad: entre una regla, una antecesora y la caja gana la última declaración, como con el resto, así que
  `Bold: true` en una caja que hereda `Weight: 300` da el 700. Los dos en el mismo `Typography` son un error
  (`WEIGHT_CONFLICT`), con la línea del `Bold`.
- **Un tramo en negrita** (`**x**`) tiene el peso 700, o el de la caja si ya es 700 o más
  ([Formato del texto](text-formatting.md#tramos)).
- **El texto grande en negrita**, para el contraste, es el de peso 700 o más ([El PDF](pdf.md#contraste)).

> **Decisión:** un `Weight` numérico, como lo declaran las fuentes (`usWeightClass`), y `Bold` como atajo del 700.
> *Alternativa descartada:* nombres de peso (`light`, `medium`, `semibold`), que cada familia aplica a pesos distintos,
> o un `Bold` numérico.

> **Decisión:** `Bold` y `Weight` en el mismo `Typography` son un error, porque o se contradicen o uno sobra; entre
> reglas, antecesoras y la caja gana la última declaración. *Alternativa descartada:* el error también cuando vienen de
> sitios distintos, que obligaría a quitar el `Bold` de una regla para dar un `Weight` a una caja.

## Guiones (`Hyphenate`)

Con `Hyphenate: true` el reparto en líneas puede partir una palabra al final de la línea con un guion, siguiendo los
patrones de partición del idioma de la caja (`Language`). Es de `Typography`, se hereda y vale `false` por defecto.

- **Los patrones son los de TeX** (`hyph-utf8`), con el algoritmo de Liang, y van dentro del motor para `es`, `ca`,
  `en-US` y `en-GB` ([Recursos](resources.md#patrones-de-partición)). `en` sin región y cualquier otra región del
  inglés usan los americanos; `es` y `ca` con cualquier región, los suyos. `Hyphenate: true` en una caja con texto en
  otro idioma es un error (`HYPHENATION_LANGUAGE_UNSUPPORTED`), con la línea del `Hyphenate`, aunque venga de una
  antecesora o de una regla. Una caja sin texto no se comprueba.
- **Los mínimos son los de los patrones**: quedan al menos 2 letras antes del guion y 2 después en `es` y `ca`, y 2 y 3
  en inglés; una palabra más corta no se parte. Una palabra se parte solo por sus letras, que tienen que ir todas
  seguidas: lo que va delante (una comilla, un paréntesis) y detrás (un punto, una coma) se queda con la primera y la
  última parte y no cuenta para los mínimos. Una palabra con algo que no es una letra entre sus letras (una cifra, un
  guion, el punto volado de `col·lecció`) o con `{page}`, `{pages}` o una referencia no se parte. Las mayúsculas se
  parten como las minúsculas.
- **Un guion blando** (U+00AD) escrito en el texto es un punto de corte explícito, con o sin `Hyphenate`: una palabra
  que lo lleva se parte solo ahí, y los patrones no añaden más. No se pinta ni se mide donde la palabra no se parte. En
  un texto con `Preformatted: true` o `wrap` se descarta.
- **El guion** es el de la fuente (`-`, U+002D), que tiene que tenerlo (`GLYPH_MISSING`), mide lo que mide en ella y
  se pinta con la fuente y el formato de la letra que lo precede (en negrita si ella lo está; nunca en versalitas).
  Cuenta en el ancho de la línea.
- **En el reparto**, con `simple`, cuando una palabra no cabe entera se añade la parte más larga que cabe con su guion
  y el resto empieza la línea siguiente; con `balanced`, cada punto de corte es un salto posible con la penalización de
  TeX (50, al cuadrado, en los deméritos de la línea, y 10 000 más si dos líneas seguidas acaban en guion). Una palabra
  más ancha que la línea se parte en tantas partes como haga falta.
- **Tamaños intrínsecos**: el ancho más estrecho es la parte más ancha que no se puede partir más, con su guion.
- **Lo que se lee**: el texto que se copia o que lee un lector de pantalla no lleva el guion; en su lugar va un guion
  blando (U+00AD), que es como un PDF etiquetado dice que el guion lo puso el reparto, y la línea partida no lleva el
  espacio invisible del final ([El PDF por dentro](../implementation/pdf.md#texto-que-se-lee)).

> **Decisión:** los patrones de TeX como recursos del motor, convertidos a un formato propio de texto plano y sin
> dependencias; cuatro idiomas para empezar, y los demás son un error y no un silencio. *Alternativa descartada:*
> una biblioteca de partición, o partir sin patrones.

> **Decisión:** una sola propiedad, `Hyphenate`, con los mínimos que traen los patrones de cada idioma. *Alternativa
> descartada:* propiedades para la longitud mínima de la palabra y para las letras a cada lado, que casi nadie cambia.

> **Decisión:** una palabra con guion blando se parte solo donde lo lleva, como en TeX, donde quien escribe el corte
> decide. *Alternativa descartada:* sumar los puntos de los patrones.

## Altura de las líneas y línea base

Cada línea mide `Line height`, y la altura del contenido es el número de líneas por `Line height`. Dentro de la línea, la
caja de la fuente (su ascendente más su descendente) se centra en vertical, lo que da la línea base:

```
línea base = arriba + (Line height − (ascendente + descendente) · Size) / 2 + ascendente · Size
```

## Alineación

`left`, `right` y `center` desplazan cada línea. `justify` reparte el sobrante a partes iguales entre los espacios de la
línea. No se justifican la última línea de cada párrafo, las líneas de una sola palabra ni las que desbordan (los
espacios no se comprimen). La última línea de una página no es la última de su párrafo si este continúa en la página
siguiente, así que se justifica.

## Medida del texto

El ancho de un texto es la suma de los avances de sus glifos, redondeados a milésimas del cuerpo, sin kerning y con
ligaduras solo si se piden ([Formato del texto](text-formatting.md#ligaduras-ligatures)). Una palabra con partes de
varios tramos mide lo que suman sus partes. Lo que se mide es exactamente lo que se pinta
([detalles](../implementation/painting.md#texto)).

## Tamaños intrínsecos

El ancho más estrecho es la palabra más ancha (con `Hyphenate` o un guion blando, la parte más ancha de una palabra
con su guion), y el natural, el párrafo más largo en una sola línea (con `Preformatted: true` o `wrap`, ver
[Formato del texto](text-formatting.md#tamaños-intrínsecos)).

## Idioma (`Language`)

Una caja cuyo texto no está en el idioma del documento declara el suyo en `Typography` (`Language: en-GB`), también desde
una regla de `Styles` (`For style: english`). Se hereda. Un lector de pantalla cambia de voz al leer esa caja, y
`Case` usa las reglas de mayúsculas de ese idioma. Es una etiqueta BCP 47 (`VALUE_NOT_ALLOWED`) y `For element: Document`
no la admite (`STYLE_NOT_ALLOWED`): el idioma del documento es el `Language` de `Document`
([Document](../components/document.md#title-y-language)).

> **Decisión:** el idioma es de la caja y no de un tramo dentro de una línea ni de una parte de un `Markdown`.

## Lo que no hay

- Fuentes de sustitución, síntesis de negrita o cursiva, y el peso más cercano cuando falta el pedido
  ([Recursos](resources.md#fuentes)).
- Unidades relativas a la fuente: todo se mide en puntos, en milímetros o en porcentajes.
- Kerning y escritura vertical o de derecha a izquierda.
- Patrones de partición de otros idiomas que `es`, `ca`, `en-US` y `en-GB`, patrones o excepciones propios del documento,
  y cambiar los mínimos de letras a cada lado del guion.
- Color o tamaño distintos dentro de una línea: un tramo solo cambia negrita, cursiva, subrayado y tachado
  ([Formato del texto](text-formatting.md#tramos)).
