# `Markdown`

Un `Markdown` es un bloque cuyo contenido se escribe en Markdown, en su `Content`. Ese contenido se **traduce a los
elementos que ya existen**, y lo generado se trata como si lo hubiera escrito el autor: las mismas reglas de `Styles`, los
mismos errores ([Errores](../common/errors.md)), los marcadores y la accesibilidad. El
`Markdown` es la caja que lo contiene, un bloque como un `Block`: admite `Id`, `Style`, `Box`, `Typography` y `Break`, y lo
generado hereda su `Typography`. No admite `Link`: su contenido lleva sus propios enlaces.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Informe
    Page:
        Paper: A4
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Heading 1
            Typography:
                Size: 18
        For element: List
            Box:
                Padding: 0 0 0 20
    Section:
        Markdown:
            Typography:
                Code font: IBM Plex Mono
            Bullet markers: bullet dash
            Number markers: decimal
            Table columns: equal
            Content >>
                # Resultados

                Las ventas **suben** y los costes *bajan*: un trimestre ++muy bueno++,
                con ~~pérdidas~~ beneficios. El informe sale de `mvn test`.

                - Ventas en alza
                - Costes:
                  - personal
                  - material

                1. Revisar
                2. Aprobar

                | Región | Ventas |
                |--------|-------:|
                | Norte  | 120    |
                | Sur    | **95** |
```

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Content` | bloque `>>` | sin valor | El Markdown. Obligatorio. |
| `Bullet markers` | lista de `bullet`, `dash`, `none` | sin valor | El `Marker` de las listas con viñetas (`-`, `*`, `+`), uno por nivel de anidamiento. El último sirve para los niveles más profundos. Obligatorio si hay listas con viñetas. |
| `Number markers` | lista de `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | sin valor | Igual, para las listas numeradas (`1.`, `1)`). Obligatorio si las hay. |
| `Table columns` | `equal`, `auto` | sin valor | Las `Column sizes` de las tablas: `equal` es `repeat(n, *)`, y `auto`, `repeat(n, auto)`. Obligatorio si hay tablas. |

No van en las reglas de `Styles`, que solo admiten los grupos.

> **Decisión:** las propiedades que no se usan no son un error (un `Bullet markers` sin listas), aunque en general lo que no tiene
> efecto lo es: el contenido de un Markdown cambia a menudo y obligaría a quitar y poner las propiedades. Su valor sí se
> comprueba siempre.

## El subconjunto

El Markdown es [CommonMark](https://commonmark.org) con las tablas y el tachado de GFM y el subrayado `++...++`. Se traduce
así:

| Markdown | Elemento |
|---|---|
| `#` a `######` (y los subrayados con `===` y `---`) | `Heading 1` a `Heading 6`, con un `Text`, y con `{#id}` al final del texto (`## Ventas {#ventas}`), su `Id` |
| Párrafo | `Paragraph` |
| `-`, `*`, `+` | `List` con el `Marker` de `Bullet markers` para su nivel, con un `List item` por ítem |
| `1.`, `1)` | `List` con el `Marker` de `Number markers` para su nivel y `Start`, el primer número si no es 1 |
| Tabla | `Table` con las `Column sizes` de `Table columns`, con la fila de cabecera en `Header rows` y el resto en `Body` |
| `**x**`, `__x__` | tramo en negrita ([Formato del texto](../common/text-formatting.md#tramos)) |
| `*x*`, `_x_` | tramo en cursiva |
| `++x++` | tramo subrayado |
| `~~x~~` | tramo tachado (con una sola tilde no se tacha) |
| `[x](destino)`, `<destino>` | tramo subrayado con un enlace a ese destino ([Enlaces](../common/links.md)) |
| `` `x` `` | tramo de código, con la familia de `Code font` |
| `[x]{nombre}` | tramo con estilo: con lo que declara la regla `For style: nombre` ([Formato del texto](../common/text-formatting.md#tramos-con-estilo)) |
| `[^id]` | la llamada a la nota al pie con ese `Id`: un tramo en superíndice con su número ([Footnote](footnote.md)) |
| `\` al final de la línea | un salto de línea forzado: la línea acaba ahí, dentro del mismo párrafo ([abajo](#saltos-de-línea-forzados)) |
| Bloque de código (con vallas ```` ``` ```` o sangrado cuatro espacios) | `Code block`: un `Text` con `Preformatted: true` cuya fuente es su `Code font`, con el código en su `Content` |
| `![Alt](ruta)`, sola en su párrafo | `Image` con esa ruta y su `Alt`, o decorativa con `![](ruta)` ([Image](image.md)) |
| `![Alt](ruta)`, dentro de un texto | imagen en línea: un tramo que es la imagen, tan alta como la letra ([Formato del texto](../common/text-formatting.md#imágenes-en-línea)) |

Todo lo demás es un error (`MARKDOWN_UNSUPPORTED`): enlaces con título (`[x](destino "título")`), imágenes con título
(`![Alt](ruta "título")`), la etiqueta de lenguaje de un bloque de código
(```` ```java ````: no hay resaltado), citas, HTML, líneas horizontales, un salto de línea forzado sin texto antes en su
línea ([abajo](#saltos-de-línea-forzados)) y definiciones de enlaces (`[x][ref]` con `[ref]: destino`: el destino se
escribe en el propio enlace).
Un encabezado tampoco puede ir dentro de un ítem de lista, como en un `List item`, ni una tabla quedarse sin filas debajo de
la cabecera. Un `Markdown` sin contenido, o un encabezado o un párrafo sin texto, es `TEXT_CONTENT`.

Un `Bullet markers` o un `Number markers` con un marcador del otro tipo es `VALUE_NOT_ALLOWED`. Una imagen clicable es una `Image` con
`Link` o, dentro de un texto, una imagen en línea dentro de un enlace (`[![Alt](ruta)](destino)`).

> **Decisión:** lo que no tendría efecto (la etiqueta de lenguaje de un bloque de código, el título de un enlace) es un
> error y no se ignora.

## Texto

Un salto de línea simple dentro de un párrafo vale un espacio, como en un `Text`. El formato dentro de la línea se suma:
`***x***` es negrita y cursiva. Esa misma sintaxis en línea, sin el resto de elementos, la admite `Markup: markdown` en un
`Text`, un `Paragraph` o una `Cell` ([Formato del texto](../common/text-formatting.md#formato-en-línea-markup)), que dentro
de un `Markdown` no actúa: sus textos ya vienen interpretados.

Los marcadores de las [referencias](../common/references.md) se sustituyen en el texto de un `Markdown` como en
cualquier otro, también en su código. Para escribirlos como texto, `\{page\}` o `{{page}}`
([Referencias como texto](../common/references.md#referencias-como-texto)).

### Saltos de línea forzados

Una `\` al final de una línea es un **salto de línea forzado**: la línea acaba ahí y el texto sigue en la siguiente, dentro
del mismo párrafo, sin el margen de un párrafo nuevo. Sirve para lo que va en líneas seguidas sin ser párrafos distintos,
como una firma con su cargo:

```
Marta Vidal\
Administración
```

- En los dos repartos en líneas (`Line breaking: simple` y `balanced`) el salto es un final de línea obligatorio, y
  ninguna línea lo cruza.
- La línea que acaba en un salto forzado **no se justifica**, como la última del párrafo.
- Vale en párrafos, encabezados e ítems de lista, y con `Markup: markdown` en un `Text`, un `Paragraph` o una `Cell`.
- En el texto de un encabezado que copian el índice, los marcadores y los títulos corridos, y en el texto alternativo de
  una imagen, el salto es un espacio.
- Un salto sin texto antes en su línea (al principio del párrafo o justo después de otro) es `MARKDOWN_UNSUPPORTED`.
- Los dos espacios al final de la línea, la otra forma que admite CommonMark, no son un salto: STXT quita los espacios
  del final de cada línea de un bloque `>>`, así que no llegan al Markdown.
- En el texto, el salto es el carácter U+2028 (separador de línea). Escrito tal cual en cualquier texto, también sin
  `Markup`, hace lo mismo. En un texto preformateado es un salto de línea más
  ([Formato del texto](../common/text-formatting.md#texto-preformateado)).

> **Decisión:** un salto sin texto antes en su línea es un error. *Alternativa descartada:* pintarlo como una línea vacía,
> que se confunde con una línea en blanco entre párrafos, que es otra cosa (el texto que sigue es un párrafo nuevo, con su
> margen).

## Código

El código en línea es un tramo de código ([Formato del texto](../common/text-formatting.md#código-en-línea)), con la
familia de `Code font`: conserva el peso y el estilo del texto que lo rodea y se suma a la negrita o la cursiva del
párrafo (``**`x`**`` es código en negrita), y puede estar dentro de un enlace. El espacio en blanco de una línea con
código en línea se colapsa como en cualquier párrafo. Lo que conserva los espacios es el bloque.

Un bloque de código es un `Text` con `Preformatted: true`
([Formato del texto](../common/text-formatting.md#texto-preformateado)) cuya fuente es su `Code font`, así
que se parte entre líneas entre páginas y una línea más ancha que la caja es un error (`TEXT_OVERFLOW`). No tiene `Id` ni
`Style`, pero es el elemento generado `Code block` y lo nombra la regla
`For element: Code block` ([Styles](../common/styles.md)), con `Box`, `Typography` y `Break` (un fondo, un relleno, un
tamaño menor o `Keep together: true`). No lo alcanza `For element: Text` (que sigue valiendo para los `Text` de los encabezados de
un Markdown y para los que se escriben). Un `Text` escrito a mano con `Preformatted: true` tampoco es un `Code block`. Su
`Typography` es el del `Markdown`, así que `Size`, `Color`, `Tab size` o `Code font` se declaran en el `Typography` del
`Markdown`, de un antecesor o en `For element: Code block`, que puede darle otra `Code font` que al código en línea. Un
`Font` de `For element: Code block` no cambia nada, porque su fuente es su `Code font`. Se usa el `Line height` de la caja
y no el de la fuente del código, así que las líneas con código en línea no cambian de alto.

`Code font` es de `Typography` ([Tipografía](../common/typography.md)) y se hereda: se declara una vez, en
`For element: Document`, y vale para el código de los `Markdown` y de los textos con `Markup: markdown`. Sin ella, el código
es un error (`CODE_WITHOUT_FONT`), con la línea del párrafo o la del bloque. Si la familia no existe, es `FONT_UNKNOWN`,
con la línea de `Code font`.

> **Decisión:** `Code font` es de `Typography`, heredable: así el código en línea de un texto con `Markup: markdown`
> también tiene fuente, y se declara una vez para todo el documento. *Alternativa descartada:* una propiedad del
> `Markdown`, que dejaba sin código a los textos con `Markup: markdown` (la factura de los ejemplos usaba un `Markdown`
> solo para escribir el IBAN y el BIC).

> **Decisión:** el bloque de código es un elemento generado que solo nombran las reglas. *Alternativa descartada:* un
> `Code block` también escribible, o un estilo automático `code`, que reservaría un nombre de estilo al autor.

## Listas

El nivel de una lista es el número de listas de su mismo tipo que la contienen: la primera lista con viñetas usa el primer
marcador de `Bullet markers`, una con viñetas dentro de esa, el segundo, y así hasta el último, que sirve para los niveles más
profundos. Las numeradas cuentan aparte, con `Number markers`. Cada párrafo de un ítem es un `Paragraph`, y una lista anidada,
una `List` dentro del `List item`. Las listas compactas y las separadas por líneas en blanco se traducen igual: el espacio
entre ítems lo dan las reglas de `Styles`. Como cualquier lista, la sangría que deja sitio al marcador es su `Padding`
izquierdo, que se suele declarar en la regla `For element: List` ([List](list.md)).

> **Decisión:** el nivel se cuenta por tipo: una lista numerada dentro de una con viñetas usa el primer marcador de
> `Number markers`. *Alternativa descartada:* un nivel común a los dos tipos.

## Tablas

Cada columna tiene la alineación de la fila de separación (`:--`, `:-:`, `--:`) como `Align` de sus celdas. Sin alineación,
tienen la de la caja. Una celda con texto es una `Cell` con valor, y una vacía, una `Cell` vacía. A una fila con menos celdas que la
cabecera se le añaden celdas vacías, como en GFM. Una con más es `MARKDOWN_UNSUPPORTED`, con la línea de la fila (una `\|`
escapada no separa celdas). Las celdas reciben las reglas de las celdas de cualquier tabla
([Table](table.md#estilo-de-las-celdas)). Las tablas de Markdown no tienen cabeceras de fila (`Row header`).

> **Decisión:** las celdas que faltan se completan, como en GFM, porque una celda vacía al final de la fila se escribe a
> menudo sin su barra. Las que sobran son un error, porque GFM las descarta sin avisar. *Alternativa descartada:* seguir
> a GFM también con las que sobran.

## Estilo

Lo generado no tiene `Style`, ni `Id` salvo los encabezados que lo escriben ([abajo](#encabezados)): se le da estilo con
las reglas `For element` de `Styles` (como `For element: Heading 1`, `For element: Paragraph` o `For element: Code block`), que se aplican a
todo el documento, y con el `Typography` del `Markdown`, que hereda.

## Encabezados

`#` es siempre `Heading 1`, y la jerarquía se comprueba en orden del documento junto con los encabezados escritos fuera del
`Markdown` ([Encabezados](heading.md#jerarquía)): después de un `Heading 1`, un Markdown puede empezar por `##`. Se numeran
como los demás y entran en los índices. Dentro de un `Page overlay`, un Markdown no puede tener encabezados.

Un encabezado lleva `Id` si lo escribe al final de su texto, `{#id}`: `## Ventas {#ventas}` es un `Heading 2` con
`Id: ventas` y el texto «Ventas». Es su `Id` como si lo hubiera escrito el autor: único en el documento
(`DUPLICATE_ID`), destino de `Link: #id` y de `[texto](#id)`, y lo refieren `{page of: id}` y, si está numerado,
`{number of: id}`. No se pinta. Vacío o con espacios (`{#mis ventas}`) es `VALUE_NOT_ALLOWED`, con la línea del
encabezado. Es lo único de lo generado que lleva `Id`.

> **Decisión:** el `Id` de un encabezado se escribe con la sintaxis de Pandoc y kramdown, `{#id}`, que conocen quienes
> escriben Markdown. *Alternativa descartada:* un `Id` sacado del texto (`ventas-del-año`), que cambia al corregir el
> título y puede chocar con los del autor.

## Dónde va

Donde va un elemento en flujo: en una sección, `Block`, `Frame`, `Overlay`, `Page overlay`, `Columns`, `List item`, `Cell` y
como elemento de un `Item` o un `Area`.

## Errores

Los errores de lo generado señalan la línea del documento donde empieza su bloque de Markdown (la del ítem, la de la fila de
la tabla). Los del marcador de una lista señalan la de `Bullet markers` o `Number markers`, y los de las columnas de una tabla, la de
`Table columns`. Una lista o una tabla sin su propiedad es un error con la línea de la lista o la tabla
(`MARKDOWN_PROPERTY_MISSING`). Una propiedad que no se usa no lo es. El código sin `Code font` es `CODE_WITHOUT_FONT`
([Código](#código)).

Los errores de un párrafo de varias líneas señalan la primera, y los de una celda, la de su fila.

## Accesibilidad

Lo generado se lee como lo escrito a mano: encabezados, párrafos, listas y tablas, y sus encabezados son marcadores.

## Lo que no hay

- Citas, HTML, líneas horizontales, listas de tareas y resaltado de código. Las notas al pie se
  llaman con `[^id]`, pero su contenido es una `Footnote`, no una definición `[^id]: ...` ([Footnote](footnote.md)).
- Enlaces con título o con definición aparte, e imágenes con título.
- Dos espacios al final de la línea como salto de línea forzado: STXT los quita.
- `Id` en lo generado, salvo en los encabezados (`{#id}`), y `Style`.
