# Tipografía: `Typography`

El texto y cómo se reparte en líneas. **Es el único grupo que se hereda**: lo que declara un elemento vale para él y
para todos sus descendientes que no lo cambien. La base que hereda todo el documento se declara en la regla
`For element: Document` de `Styles` ([Styles](styles.md#la-tipografía-base)), y cada elemento declara solo lo que cambia.
Lo que una regla de `Styles` da a un elemento en `Typography` lo heredan sus descendientes como si lo hubiera declarado
él.

`Typography` se admite en todos los elementos de caja salvo `Image`, que no tiene texto, y en `Row` (lo heredan sus
celdas). En un `Barcode` solo admite `Color`, el de sus módulos ([Barcode](../components/barcode.md#los-módulos)).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Font` | nombre de familia | sin valor | **Obligatoria** en todo texto, propia o heredada (`TEXT_WITHOUT_FONT`). Es la familia que declaran los ficheros TTF u OTF de la carpeta de fuentes ([Recursos](resources.md#fuentes)). |
| `Code font` | nombre de familia | sin valor | La familia del código, en línea y en bloques ([Formato del texto](text-formatting.md#código-en-línea)). Suele ser una monoespaciada. **Obligatoria** si hay código, propia o heredada (`CODE_WITHOUT_FONT`). |
| `Bold` | `true`, `false` | `false` | Negrita: `true` es `Weight: 700` y `false`, `Weight: 400`. No va con `Weight` en el mismo `Typography` (`WEIGHT_CONFLICT`). |
| `Weight` | de 100 a 900, de 100 en 100 | `400` | Peso de la fuente ([Pesos](#pesos-weight)). |
| `Italic` | `true`, `false` | `false` | Cursiva: la variante cursiva de la fuente. |
| `Size` | número > 0 | sin valor | Cuerpo de la fuente. **Obligatorio** en todo texto, propio o heredado (`TEXT_WITHOUT_FONT_SIZE`). |
| `Min size` | número > 0 | `6` | El menor `Size` al que `Overflow: shrink` reduce el texto ([Desbordamiento](overflow.md#encoger-el-texto-shrink)). |
| `Line height` | número o % | `120%` | **Un número es una medida** (`15` = 15 pt). Para un valor relativo al `Size`, porcentaje (`120%`). |
| `Align` | `left`, `right`, `center`, `justify` | `left` | La última línea de un párrafo justificado se alinea a la izquierda. |
| `Color` | color | `#000000` | Color del texto ([Valores](values.md#colores)). |
| `Line breaking` | `simple`, `optimal` | `simple` | Reparto en líneas: voraz (`simple`) o Knuth-Plass (`optimal`). |
| `Hyphenate` | `true`, `false` | `false` | Partir palabras con guion al final de la línea, con los patrones del idioma ([Guiones](#guiones-hyphenate)). |
| `Break long words` | `never`, `new-line`, `same-line` | `never` | Cortar sin guion la parte de una palabra que no cabe en una línea entera, y dónde empieza ([Palabras largas](#palabras-largas-break-long-words)). |
| `First line indent` | medida, sin % | `0` | Sangría de la primera línea del párrafo; puede ser negativa ([Sangría de primera línea](#sangría-de-primera-línea-first-line-indent)). |
| `Markup` | `none`, `markdown` | `none` | Formato dentro de la línea ([Formato del texto](text-formatting.md#formato-en-línea-markup)). |
| `Case` | `none`, `uppercase`, `lowercase`, `capitalize` | `none` | Mayúsculas al pintar ([Formato del texto](text-formatting.md#mayúsculas-case)). |
| `Letter spacing` | número | `0` | Interletraje; puede ser negativo ([Formato del texto](text-formatting.md#interletraje-letter-spacing)). |
| `Small caps` | `true`, `false` | `false` | Versalitas ([Formato del texto](text-formatting.md#versalitas-small-caps)). |
| `Position` | `normal`, `superscript`, `subscript` | `normal` | Superíndice o subíndice; solo en la regla de un tramo con estilo y no se hereda ([Formato del texto](text-formatting.md#superíndice-y-subíndice-position)). |
| `Ligatures` | `true`, `false` | `false` | Ligaduras de la fuente ([Formato del texto](text-formatting.md#ligaduras-ligatures)). |
| `Kerning` | `true`, `false` | `false` | Pares de kerning de la fuente ([Kerning](#kerning-kerning)). |
| `Preformatted` | `none`, `keep`, `wrap` | `none` | Texto preformateado ([Formato del texto](text-formatting.md#texto-preformateado)). |
| `Tab size` | entero ≥ 1 | `4` | Con `Preformatted: keep` o `wrap`, las columnas de una tabulación. |
| `Numbered` | `true`, `false` | `false` | Si los encabezados llevan número ([Encabezados](../components/heading.md#numeración-numbered)). Una `Figure` va numerada salvo que lo declare ella o una regla suya ([Figure](../components/figure.md#numeración)). |
| `Number format` | `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | `decimal` | Cómo se escribe el número del nivel de un encabezado, o el de una figura ([Encabezados](../components/heading.md#formato-y-etiqueta-number-format-number-label)). |
| `Number label` | texto con `{number}` | el número | Lo que va delante del título de un encabezado numerado, o del pie de una figura ([Encabezados](../components/heading.md#formato-y-etiqueta-number-format-number-label)). |
| `Widows` | entero ≥ 1 | `2` | Mínimo de líneas de un párrafo que pasan a la página siguiente ([Cortes](breaks.md#elección-del-corte)). |
| `Orphans` | entero ≥ 1 | `2` | Mínimo de líneas de un párrafo que quedan al final de una página. |
| `Language` | etiqueta BCP 47 (`es`, `en-GB`, `ca`) | el `Language` de `Document` | Idioma del texto de la caja, cuando no es el del documento ([idioma](#idioma-language)). |

Lo que no tiene texto ignora lo que hereda sin error: un `Block` hereda `Size` para sus hijos, y un `Text` que hereda
`Numbered: true` no hace nada, porque solo lo leen los encabezados y las figuras.

## Dónde hay texto

Tienen texto `Text`, `Paragraph` y una `Cell` con valor ([Text](../components/text.md)), y lo generan los encabezados
(sus `Text`), las entradas de un `Table of contents` y lo que traduce un `Markdown`. El texto de un `Text` va en su valor
(`Text: Un título`) o en un hijo `Content >>` con un bloque indentado, sin líneas en blanco: cada caja es un párrafo.

## Espacios y párrafos

Las secuencias de espacios y los saltos de línea simples equivalen a un espacio. Una **línea en blanco** es un error
(`TEXT_BLANK_LINE`): cada `Text` y cada `Paragraph` es un solo párrafo. Dentro de él, una línea se puede acabar a la
fuerza con el salto de línea de Markdown, `\` al final de la línea
([Markdown](../components/markdown.md#saltos-de-línea-forzados)). Con `Preformatted: keep` o `wrap` se
conservan los espacios y los saltos de línea, también las líneas en blanco de un `Text`
([Formato del texto](text-formatting.md#texto-preformateado)).

Son espacio el espacio, la tabulación, los saltos de línea, U+000B y U+000C. Cualquier otro espacio de Unicode, como el
de cuadratín (U+2003) o el ideográfico (U+3000), es un carácter más: no se colapsa, no separa palabras y la fuente tiene
que tenerlo (`GLYPH_MISSING`).

## El reparto en líneas

El texto se reparte en **líneas** del ancho de la caja de contenido:

- **`Line breaking: simple`**, algoritmo voraz: las palabras se añaden a la línea mientras quepan.
- **`Line breaking: optimal`**, **Knuth-Plass**: se eligen los saltos del párrafo que minimizan la suma de los deméritos de sus
  líneas, de modo que las líneas quedan más parejas. Los espacios se estiran hasta la mitad de su ancho y no se encogen,
  y la última línea no cuenta.

Las líneas junto a una caja lateral ([Block](../components/block.md#cajas-laterales-side)) o a una letra capitular
([Text](../components/text.md#letra-capitular-drop-cap)) son más cortas que la caja de contenido: los dos repartos trabajan
con el ancho de cada línea, y una palabra que no cabe en una línea corta la deja vacía y pasa a la siguiente. La
primera línea de un párrafo con `First line indent` también tiene otro ancho
([Sangría de primera línea](#sangría-de-primera-línea-first-line-indent)).

Una palabra se parte tras un guion escrito en ella, donde lleva un guion blando o, con `Hyphenate`, por los patrones
del idioma ([Guiones](#guiones-hyphenate)). En los dos repartos, una palabra más ancha que la línea, o una parte suya
que ya no se puede partir, es un error (`TEXT_OVERFLOW`, [Desbordamiento](overflow.md)), salvo con
`Break long words`, que la corta por donde cabe ([Palabras largas](#palabras-largas-break-long-words)).

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
  `en-US`, `en-GB`, `de`, `fr`, `it`, `nl` y `pt` ([Recursos](resources.md#patrones-de-partición)). `en` sin región y
  cualquier otra región del inglés usan los americanos; los demás idiomas, con cualquier región o variante, los suyos.
  Los del alemán son los de la ortografía reformada (`de-1996`), también con `de-CH`, `de-AT` o `de-1901`.
  `Hyphenate: true` en una caja con texto en otro idioma es un error (`HYPHENATION_LANGUAGE_UNSUPPORTED`), con la línea
  del `Hyphenate`, aunque venga de una antecesora o de una regla. Una caja sin texto no se comprueba.
- **Los mínimos son los de los patrones**: quedan al menos 2 letras antes del guion y 2 después en `es`, `ca`, `de`,
  `fr`, `it` y `nl`, y 2 y 3 en inglés y en `pt`; una palabra más corta no se parte. Una palabra se parte solo por sus
  letras, que tienen que ir todas seguidas: lo que va delante (una comilla, un paréntesis) y detrás (un punto, una
  coma) se queda con la primera y la última parte y no cuenta para los mínimos. Una palabra con algo que no es una
  letra entre sus letras (una cifra, un guion, el punto volado de `col·lecció`) o con `{page}`, `{pages}` o una
  referencia no se parte por los patrones. Las mayúsculas se parten como las minúsculas.
- **El apóstrofo** (`'` o `’`) entre las letras de una palabra es una letra más en los idiomas cuyos patrones lo
  incluyen, `fr` e `it`: `l'information` se parte `l'in-for-ma-tion` y `dell'amministrazione`,
  `del-l'am-mi-ni-stra-zio-ne`, y cuenta para los mínimos. En los demás idiomas una palabra con apóstrofo no se parte.
  Al principio o al final de la palabra es puntuación, como una comilla.
- **Un guion blando** (U+00AD) escrito en el texto es un punto de corte explícito, con o sin `Hyphenate`: una palabra
  que lo lleva se parte solo ahí, y los patrones no añaden más. No se pinta ni se mide donde la palabra no se parte. En
  un texto con `Preformatted: keep` o `wrap` se descarta.
- **Un guion escrito** (`-`, U+002D) con una letra o una cifra a cada lado es un punto de corte, con o sin
  `Hyphenate`: la palabra se parte **detrás de él** y no se añade otro guion (`REF-2026-` y `009812`, `físico-` y
  `químico`). No lo es al principio ni al final de la palabra, ni junto a otro guion o a un signo de puntuación. Las
  partes de una palabra así no se parten por los patrones; si además lleva guiones blandos, valen los dos. El guion
  de no separación (U+2011) no es un punto de corte: es la forma de mantener entera la palabra. No se aplica en un
  texto con `Preformatted: keep` o `wrap`.
- **El guion** es el de la fuente (`-`, U+002D), que tiene que tenerlo (`GLYPH_MISSING`), mide lo que mide en ella y
  se pinta con la fuente y el formato de la letra que lo precede (en negrita si ella lo está; nunca en versalitas).
  Cuenta en el ancho de la línea.
- **En el reparto**, con `simple`, cuando una palabra no cabe entera se añade la parte más larga que cabe con su guion
  y el resto empieza la línea siguiente; con `optimal`, cada punto de corte es un salto posible con la penalización de
  TeX (50, al cuadrado, en los deméritos de la línea, y 10 000 más si dos líneas seguidas acaban en guion). Una palabra
  más ancha que la línea se parte en tantas partes como haga falta. El corte tras un guion escrito es un punto de
  corte más en los dos repartos, y en `optimal` lleva la misma penalización y cuenta como una línea acabada en guion.
- **Tamaños intrínsecos**: el ancho más estrecho es la parte más ancha que no se puede partir más, con su guion: el
  que añade el reparto o, tras un guion escrito, el suyo.
- **Lo que se lee**: el texto que se copia o que lee un lector de pantalla no lleva el guion que añade el reparto; en
  su lugar va un guion blando (U+00AD), que es como un PDF etiquetado dice que el guion lo puso el reparto, y la línea
  partida no lleva el espacio invisible del final ([El PDF por dentro](../implementation/pdf.md#texto-que-se-lee)).
  Una línea que acaba tras un guion escrito se lee como se pinta, con su guion, y tampoco lleva ese espacio.

> **Decisión:** los patrones de TeX como recursos del motor, convertidos a un formato propio de texto plano y sin
> dependencias; nueve idiomas, y los demás son un error y no un silencio. *Alternativa descartada:* una biblioteca de
> partición, o partir sin patrones.

> **Decisión:** el alemán, solo con la ortografía reformada de 1996, la oficial, para cualquier región y variante: la
> tradicional casi no se usa en documentos nuevos y sus patrones costarían otros 120 KB en el jar. *Alternativa
> descartada:* los patrones de la ortografía tradicional (`hyph-de-1901.tex` y `hyph-de-ch-1901.tex`) para las
> variantes `de-1901` y `de-CH-1901`.

> **Decisión:** el apóstrofo es una letra donde los patrones lo incluyen, como en TeX, para que el francés y el
> italiano partan las palabras con elisión, que son muy frecuentes. *Alternativa descartada:* la regla general (una
> palabra con algo que no es una letra entre sus letras no se parte), que dejaría enteras `l'information` o
> `dell'amministrazione`.

> **Decisión:** una sola propiedad, `Hyphenate`, con los mínimos que traen los patrones de cada idioma. *Alternativa
> descartada:* propiedades para la longitud mínima de la palabra y para las letras a cada lado, que casi nadie cambia.

> **Decisión:** una palabra con guion blando se parte solo donde lo lleva, como en TeX, donde quien escribe el corte
> decide. *Alternativa descartada:* sumar los puntos de los patrones.

> **Decisión:** un guion escrito entre letras o cifras es un punto de corte siempre, sin propiedad, como en cualquier
> composición de texto: una referencia o un término compuesto no dejan de caber por llevar guiones. *Alternativa
> descartada:* una propiedad para activarlo, o cortar también tras `/`, `.`, `_` y `@`, que es lo que cubre
> `Break long words`.

## Palabras largas (`Break long words`)

Con `Break long words: new-line` o `same-line`, la parte de una palabra que ya no se puede partir y es **más ancha
que una línea entera de su caja** se corta por el último carácter que cabe, sin guion, tantas veces como haga falta.
Con `never`, el valor por defecto, no se corta y es un error (`TEXT_OVERFLOW`). Es de `Typography` y se hereda. Es para lo que llega en los datos sin un sitio por donde partirse: un IBAN, una
dirección de correo, una URL o un identificador en una celda estrecha.

```stxt
Styles:
    For element: Cell
        Typography:
            Break long words: new-line
```

Los dos valores que cortan se distinguen en dónde empieza la parte larga. En una línea de diez caracteres,
`ID ES762077002400` queda:

| Valor | Líneas | |
|---|---|---|
| `new-line` | `ID` / `ES76207700` / `2400` | La parte larga empieza en una línea propia, como en un procesador de textos |
| `same-line` | `ID ES76207` / `7002400` | Empieza en la línea en curso, tras las palabras anteriores |

- **Es el último recurso.** Una palabra que cabe en una línea vacía pasa a la línea siguiente, como sin la propiedad,
  y nunca se corta. Los guiones escritos, los blandos y los patrones de `Hyphenate` van antes: solo se corta la parte
  que, entre dos de esos puntos de corte, sigue sin caber en el ancho de la caja de contenido.
- **Dónde se corta.** La parte larga puede acabar una línea tras cualquiera de sus caracteres, y la acaba tras el
  último que cabe. Con `new-line` empieza en una línea propia: las palabras anteriores, y las partes de su palabra
  que van antes de ella (`REF-`), acaban la línea anterior. Con `same-line` empieza a continuación de ellas, si en
  su línea cabe alguno de sus caracteres. Con los dos, las palabras siguientes continúan en la línea de su último
  trozo. Junto a una
  caja lateral o a una letra capitular se corta al ancho de cada línea.
- **Un carácter** es lo que se lee como uno: una letra no se separa de sus marcas combinantes, ni las dos mitades de un
  par subrogado entre sí.
- **Sin guion.** El corte no añade nada, y el texto que se copia o que lee un lector de pantalla es la palabra
  entera: la línea cortada no lleva el espacio invisible del final
  ([El PDF por dentro](../implementation/pdf.md#texto-que-se-lee)).
- **En el reparto**, con `optimal`, una línea solo acaba en un corte así si no le cabe un carácter más. Esa línea no
  tiene maldad, porque está llena, y lleva la penalización de un guion, pero no la de dos líneas seguidas acabadas en
  guion.
- **Con `First line indent`**, la primera palabra del párrafo se mide contra su línea, la sangrada: si su primera
  parte no cabe en ella, se corta ahí, aunque quepa en una línea entera
  ([abajo](#sangría-de-primera-línea-first-line-indent)).
- **Lo que no se corta.** Una palabra con `{page}`, `{pages}` o una referencia. Si en la línea no cabe ni un carácter,
  es `TEXT_OVERFLOW`.
- **Tamaños intrínsecos**: no cambian ([abajo](#tamaños-intrínsecos)).
- **Con `Overflow: shrink`**, ninguna palabra es ya más ancha que la caja, así que el texto solo encoge si sus líneas
  no caben a lo alto ([Desbordamiento](overflow.md#encoger-el-texto-shrink)).
- **Con `Preformatted: keep` o `wrap`** no tiene efecto: las líneas son las escritas, o se parten solo entre palabras.
  Escrito por el propio texto preformateado es un error (`BREAK_LONG_WORDS_IN_PREFORMATTED`); heredado o de una regla
  no se comprueba, como el resto de `Typography` ([Errores](errors.md#lo-que-no-tiene-efecto)).

> **Decisión:** una propiedad, `never` por defecto: cortar una palabra por cualquier letra lo decide el autor.
> *Alternativa descartada:* hacerlo siempre, que cambiaría un error por un corte feo sin que nadie lo pida, y un valor
> más de `Overflow`, que es de la caja y no se hereda.

> **Decisión:** el umbral es la línea entera de la caja y no lo que queda de la línea en curso: una palabra que cabe
> en una línea propia nunca se corta, tampoco junto a una caja lateral, donde deja vacías las líneas cortas como
> cualquier otra. La excepción es la primera palabra de un párrafo sangrado, que no tiene una línea anterior en la
> que dejar un hueco: su umbral es la línea sangrada. *Alternativa descartada:* cortar toda palabra que no quepa en
> lo que queda de línea, que partiría palabras corrientes.

> **Decisión:** dónde empieza la parte larga lo elige el autor con el valor: `new-line`, en una línea propia, como
> en un procesador de textos y con `overflow-wrap` de CSS, para que el dato empiece siempre en el margen de su caja;
> `same-line`, en la línea en curso, que no deja una línea corta delante del dato. *Alternativa descartada:* un
> booleano con una sola de las dos formas: las dos tienen su uso y ninguna es la buena para todo documento.

> **Decisión:** `Break long words` no cambia el ancho más estrecho: una columna `auto` no se encoge hasta una letra
> por tenerlo, y solo actúa cuando el ancho final no da. *Alternativa descartada:* que el ancho más estrecho fuera el
> del carácter más ancho, con el que una tabla de columnas `auto` cortaría palabras que caben.

> **Decisión:** en un texto preformateado es un error solo si lo escribe él, con `Preformatted: keep` y con `wrap`. *Alternativa
> descartada:* cortar las palabras largas de `wrap`, que ningún documento ha pedido, y el error también cuando se
> hereda, que alcanzaría a cada bloque de código de un documento que lo declara en `For element: Document`.

## Sangría de primera línea (`First line indent`)

`First line indent` desplaza el principio de la **primera línea** de cada caja de texto y acorta esa línea en lo
mismo. Es de `Typography`, se hereda y vale `0` por defecto. Es una medida en puntos o en milímetros, sin
porcentajes (`VALUE_NOT_ALLOWED`).

```stxt
Styles:
    For element: Paragraph
        Typography:
            First line indent: 14
```

Con un valor negativo y `Padding` izquierdo es la **sangría francesa** de las cláusulas, las bibliografías y los
glosarios: la primera línea empieza antes que las demás.

```stxt
Styles:
    For style: clause
        Box:
            Padding: 0 0 0 30
        Typography:
            First line indent: -30
```

- **Dónde se declara.** Se hereda, así que vale para toda caja con texto que la reciba: desde
  `For element: Document` sangra también los encabezados, las celdas y el texto de los ítems de las listas.
  `For element: Paragraph` la da solo a los párrafos. El marcador de un `List item` no la recibe: queda donde
  estaría sin ella ([List](../components/list.md#posición)).
- **Solo la primera línea del párrafo.** No la que sigue a un salto de línea forzado
  ([Markdown](../components/markdown.md#saltos-de-línea-forzados)), ni la primera del fragmento cuando el párrafo
  continúa en otra página o columna.
- **Alineación.** La línea se alinea en lo que queda tras la sangría: con `Align: center` o `right` se centra o se
  lleva a la derecha en ese ancho, y con `justify` lo llena.
- **Negativa.** La primera línea empieza a la izquierda de la caja de contenido, dentro del `Padding` izquierdo de
  su caja, y mide eso de más. Si el valor absoluto pasa de ese `Padding`, la línea empezaría fuera de la caja: es un
  error (`FIRST_LINE_INDENT_OVERFLOW`), con la línea del `First line indent`, aunque esté en una antecesora o en una
  regla. Se compara con el relleno de la caja ya maquetada, así que un `Padding` en porcentaje cuenta por lo que
  mide.
- **Junto a una caja lateral** ([Block](../components/block.md#cajas-laterales-side)), la sangría se suma a lo que
  la caja quita a la primera línea. El resto de un párrafo que se vuelve a repartir en la página siguiente no se
  sangra.
- **Con `Drop cap`**, el párrafo no se sangra: la sangría que hereda o recibe de una regla no se le aplica, y su
  primera línea empieza tras la capitular. Una sangría distinta de 0 escrita en el propio párrafo es un error
  (`FIRST_LINE_INDENT_WITH_DROP_CAP`), con la línea del `Drop cap`: las dos mueven el principio del párrafo.
- **Con `Preformatted: keep` o `wrap`** se aplica igual, a la primera línea del texto; con `wrap`, esa línea se
  parte en lo que deja la sangría.
- **Lo que no cabe.** Una sangría que no deja sitio a la primera palabra es `TEXT_OVERFLOW`: la palabra no pasa a
  la línea siguiente. Con `Break long words: new-line` o `same-line`, esa palabra se corta por el último carácter que cabe tras la
  sangría y sigue en la línea siguiente; si no cabe ni un carácter, es `TEXT_OVERFLOW`.
- **Tamaños intrínsecos**: el ancho natural suma la sangría a la primera línea, y el más estrecho es el mayor entre
  la palabra más ancha y la primera palabra más la sangría ([abajo](#tamaños-intrínsecos)).

> **Decisión:** una propiedad de `Typography`, heredada como todo su grupo, que una regla
> `For element: Paragraph` da a los párrafos sin tocar encabezados ni celdas. *Alternativa descartada:* una
> propiedad de `Paragraph` no heredada, que no serviría para las cláusulas escritas como `Text` ni desde un
> `Block`.

> **Decisión:** el marcador de un `List item` no recibe la sangría que hereda su ítem: es una etiqueta generada, no
> un párrafo, y no tiene `Padding` en el que colgar una sangría negativa. *Alternativa descartada:* que la heredara
> como el resto de la tipografía del ítem, con lo que una sangría francesa declarada sobre una lista sería
> `FIRST_LINE_INDENT_OVERFLOW` por su marcador.

> **Decisión:** una sangría negativa cabe en el `Padding` izquierdo de su caja, y si no es un error: el texto no
> sale de su caja. Se comprueba con la caja ya maquetada, como lo que desborda, porque el relleno puede ser un
> porcentaje. *Alternativa descartada:* dejar que la línea salga de la caja, y comprobarlo al leer el documento, que
> no alcanza a un `Padding` en porcentaje.

> **Decisión:** la capitular anula la sangría que el párrafo hereda o recibe de una regla, y solo es un error la que
> escribe él, como `Break long words` en un texto preformateado: un párrafo con capitular no se sangra, y la sangría
> suele llegar de `For element: Paragraph`. *Alternativa descartada:* que la heredada también sea un error, que
> obliga a escribir `First line indent: 0` en cada párrafo con capitular de un documento con los párrafos sangrados.

> **Decisión:** una primera palabra que no cabe tras la sangría desborda, y con `Break long words` se corta en la
> línea sangrada. *Alternativas descartadas:* dejar vacía la primera línea y bajar la palabra, como junto a una caja
> lateral, que daría un párrafo sin sangría detrás de una línea en blanco; y que `Break long words` midiera también
> esa palabra contra la línea entera, con lo que un párrafo sangrado que empieza por una URL sería un error con la
> propiedad puesta.

## Kerning (`Kerning`)

Con `Kerning: true` el texto se mide y se pinta con los pares de kerning de la fuente, que acercan o separan dos letras
concretas según su forma (`AV`, `To`, `P.`). Es de `Typography`, se hereda y vale `false` por defecto: se declara una vez
en `For element: Document`.

- **Los pares son los de la fuente**: los de la característica `kern` de su tabla `GPOS` (la del sistema de escritura por
  defecto, `DFLT`, o, si no la tiene, la del latino, `latn`) o, en una fuente sin ella, los de su tabla `kern`. Una fuente
  sin pares se mide y se pinta como sin `Kerning`, sin error. IBM Plex Sans y Serif los traen en `GPOS`; IBM Plex Mono,
  monoespaciada, no tiene.
- **Un par** son dos caracteres seguidos de la misma palabra y del mismo tramo: no cruza un espacio ni el límite entre
  dos tramos (`**A**V`). Lo forman también la última letra de una palabra partida y su guion, que van en el mismo tramo
  salvo en versalitas. Con `Ligatures: true` el par es entre los glifos que quedan (la `o` y la ligadura `fi` de
  `Tofi`), nunca dentro de una ligadura.
- **La medida**: el ajuste de cada par se redondea a milésimas del cuerpo, como el avance de los glifos, y se suma al
  ancho de la palabra; lo que se mide es lo que se pinta. Con `Letter spacing`, el interletraje se suma al ajuste.
- **Lo que se lee**: el kerning no cambia el texto que se copia ni el que lee un lector de pantalla.

> **Decisión:** el motor lee los pares de `GPOS`, que la biblioteca de fuentes (fontbox) no lee, porque las fuentes
> actuales, IBM Plex incluida, solo los traen ahí; la tabla `kern` queda para las fuentes antiguas. *Alternativa
> descartada:* solo la tabla `kern`, con la que `Kerning` no haría nada con las fuentes del repositorio.

> **Decisión:** `false` por defecto, para que ningún documento existente cambie, y una sola propiedad, sin elegir tablas
> ni pares. *Alternativa descartada:* kerning siempre, que movería las líneas de todos los documentos.

> **Decisión:** de `GPOS` solo se aplica el kerning horizontal: el avance del primer glifo de cada par, sin saltar las
> marcas que separen dos letras ni los pares propios de un idioma. *Alternativa descartada:* un motor de composición
> completo, con todas las características, contextos y sistemas de escritura.

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

El ancho de un texto es la suma de los avances de sus glifos, redondeados a milésimas del cuerpo, con los ajustes de
kerning solo si se piden ([Kerning](#kerning-kerning)) y con ligaduras solo si se piden
([Formato del texto](text-formatting.md#ligaduras-ligatures)). Una palabra con partes de
varios tramos mide lo que suman sus partes. Lo que se mide es exactamente lo que se pinta
([detalles](../implementation/painting.md#texto)).

## Tamaños intrínsecos

El ancho más estrecho es la palabra más ancha (si tiene puntos de corte, por `Hyphenate`, por un guion blando o por
un guion escrito, la más ancha de sus partes, con su guion), y el natural, el párrafo más largo en una sola línea
(con `Preformatted: keep` o `wrap`, ver [Formato del texto](text-formatting.md#tamaños-intrínsecos)).
`Break long words` no cambia ninguno de los dos
([Palabras largas](#palabras-largas-break-long-words)). Con `First line indent`, el natural suma la sangría a la
primera línea y el más estrecho es el mayor entre la palabra más ancha y la primera palabra (o su primera parte) más
la sangría; con una sangría negativa, el natural no baja de la palabra más ancha
([Sangría de primera línea](#sangría-de-primera-línea-first-line-indent)).

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
- Escritura vertical o de derecha a izquierda, y de `GPOS`, lo que no es kerning horizontal (marcas, posiciones
  contextuales).
- Patrones de partición de otros idiomas que `es`, `ca`, `en-US`, `en-GB`, `de`, `fr`, `it`, `nl` y `pt`, los de la
  ortografía alemana tradicional, patrones o excepciones propios del documento, y cambiar los mínimos de letras a cada
  lado del guion.
- Puntos de corte tras otros caracteres que el guion (`/`, `.`, `_`, `@`), impedir el corte tras un guion escrito de
  otra forma que con el guion de no separación, y cortar las palabras largas de un texto preformateado.
- Sangría de primera línea en porcentaje, y sangrar la línea que sigue a un salto de línea forzado.
- Tamaño distinto dentro de una línea (salvo la letra capitular, la llamada de una nota al pie y un tramo en
  superíndice o en subíndice): un tramo cambia la familia, el peso, el estilo, el color, las mayúsculas y el
  interletraje ([Formato del texto](text-formatting.md#tramos-con-estilo)), pero no el tamaño.
