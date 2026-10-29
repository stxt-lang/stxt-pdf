# `Markdown`

Un `Markdown` es un bloque cuyo contenido se escribe en Markdown, en su `Content`. Al construir el árbol, ese contenido se
**traduce a los elementos que ya existen**, y lo generado se trata como si lo hubiera escrito el autor: pasa por el schema,
por las reglas de `Styles`, por la validación ([Errores](../comun/errores.md)) y por el PDF etiquetado y los marcadores. El
`Markdown` es la caja que lo contiene, un bloque como un `Block`: admite `Id`, `Class`, `Box`, `Typography` y `Break`, y lo
generado hereda su `Typography`. No admite `Link`: su contenido lleva sus propios enlaces.

```
Document (dev.stxt.boxes):
	Language: es
	Title: Informe
	Media:
		Paper: A4
		Margin: 40
	Styles:
		Element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
		Element: Heading 1
			Typography:
				Size: 18
		Element: List
			Box:
				Padding: 0 0 0 20
	Section:
		Markdown:
			Bullet: disc dash
			Numbering: decimal
			Table columns: equal
			Code font: IBM Plex Mono
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
| `Content` | bloque `>>` | — | El Markdown. Obligatorio. |
| `Bullet` | lista de `disc`, `dash`, `none` | — | El `Marker` de las listas con viñetas (`-`, `*`, `+`), uno por nivel de anidamiento; el último sirve para los niveles más profundos. Obligatorio si hay listas con viñetas. |
| `Numbering` | lista de `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | — | Igual, para las listas numeradas (`1.`, `1)`). Obligatorio si las hay. |
| `Table columns` | `equal`, `auto` | — | Las `Columns` de las tablas: `equal`, `repeat(n, 1fr)`; `auto`, `repeat(n, auto)`. Obligatorio si hay tablas. |
| `Code font` | nombre de familia | — | La familia de la fuente del código (`código` y bloques de código), tal como la declara su fichero TTF; suele ser una monoespaciada. Obligatoria si hay código. |

No van en las reglas de `Styles`, que solo admiten los grupos.

> **Decisión:** el Markdown se traduce a nodos STXT, no a cajas: lo generado (`Heading N`, `Paragraph`, `List`, `Table`…)
> se construye como cualquier otro elemento, así que las reglas de `Styles`, la validación, el PDF etiquetado y los
> marcadores no tienen ningún caso especial, y lo generado se valida además contra el schema. *Alternativa descartada:*
> construir las cajas directamente desde el árbol del parser de Markdown.

> **Decisión:** las propiedades que no se usan no son un error: un `Bullet` sin listas con viñetas, o un `Table columns`
> sin tablas, se aceptan, aunque en general lo que no tiene efecto es un error. El contenido de un Markdown cambia a menudo
> y obligaría a quitar y poner las propiedades; su valor sí se comprueba siempre. *Alternativa descartada:* el error de
> siempre de lo que no tiene efecto.

## El subconjunto

El Markdown es [CommonMark](https://commonmark.org) con las tablas y el tachado de GFM y el subrayado `++…++`. Se traduce
así:

| Markdown | Elemento |
|---|---|
| `#` … `######` (y los subrayados con `===` y `---`) | `Heading 1` … `Heading 6`, con un `Text` |
| Párrafo | `Paragraph` |
| `-`, `*`, `+` | `List` con el `Marker` de `Bullet` para su nivel; un `List item` por ítem |
| `1.`, `1)` | `List` con el `Marker` de `Numbering` para su nivel y `Start`, el primer número si no es 1 |
| Tabla | `Table` con las `Columns` de `Table columns`; la fila de cabecera en `Head` y el resto en `Body` |
| `**x**`, `__x__` | tramo en negrita ([Formato del texto](../comun/formato-del-texto.md#tramos)) |
| `*x*`, `_x_` | tramo en cursiva |
| `++x++` | tramo subrayado |
| `~~x~~` | tramo tachado (con una sola tilde no se tacha) |
| `[x](destino)`, `<destino>` | tramo subrayado con un enlace a ese destino ([Enlaces](../comun/enlaces.md)) |
| `` `x` `` | tramo con la fuente de `Code font` |
| Bloque de código (con vallas ```` ``` ```` o sangrado cuatro espacios) | `Code block`: un `Text` con la fuente de `Code font` y `White space: pre`, con el código en su `Content` |
| `![Alt](ruta)`, sola en su párrafo | `Image` con esa ruta y su `Alt`; con `![](ruta)`, decorativa ([Image](image.md)) |

Todo lo demás es un error (`MARKDOWN_UNSUPPORTED`): enlaces con título (`[x](destino "título")`), imágenes dentro de un texto
(también dentro de un enlace) o con título (`![Alt](ruta "título")`), la etiqueta de lenguaje de un bloque de código
(```` ```java ````: no hay resaltado), citas, HTML, líneas horizontales, saltos de línea forzados (dos espacios o `\` al
final de la línea) y definiciones de enlaces (`[x][ref]` con `[ref]: destino`: el destino se escribe en el propio enlace).
Un encabezado tampoco puede ir dentro de un ítem de lista, como en un `List item`, ni una tabla quedarse sin filas debajo de
la cabecera. Un `Markdown` sin contenido, o un encabezado o un párrafo sin texto, es `TEXT_CONTENT`.

> **Decisión:** el tachado exige dos tildes (`~x~` queda como texto), como el resto de la sintaxis, que dobla el
> delimitador. *Alternativa descartada:* el tachado con una tilde.

> **Decisión:** lo vacío usa `TEXT_CONTENT` y lo que el subconjunto no puede traducir, `MARKDOWN_UNSUPPORTED`, también un
> encabezado dentro de un ítem de lista y una tabla sin filas debajo de la cabecera; un `Bullet` o un `Numbering` con un
> marcador del otro tipo es `VALUE_NOT_ALLOWED`. *Alternativa descartada:* un código para cada caso.

> **Decisión:** la etiqueta de lenguaje de un bloque de código es un error: no hay resaltado y no tendría efecto, como el
> título de un enlace o de una imagen. *Alternativa descartada:* ignorarla.

> **Decisión:** los enlaces con título y las definiciones de enlaces aparte son errores: el destino se escribe en el propio
> enlace, y una imagen dentro de un enlace también lo es, porque una imagen va sola en su párrafo; una imagen clicable es
> una `Image` con `Link`. *Alternativa descartada:* admitir las definiciones.

## Texto

Un salto de línea simple dentro de un párrafo vale un espacio, como en un `Text`. El formato dentro de la línea se suma:
`***x***` es negrita y cursiva. Esa misma sintaxis en línea, sin el resto de elementos, la admite `Inline: markdown` en un
`Text`, un `Paragraph` o una `Cell` ([Formato del texto](../comun/formato-del-texto.md#formato-en-línea-inline)), que dentro
de un `Markdown` no actúa: sus textos ya vienen interpretados.

## Código

El código en línea es un tramo con la familia de `Code font`: conserva el peso y el estilo del texto que lo rodea y se suma
a la negrita o la cursiva del párrafo (``**`x`**`` es código en negrita), y puede estar dentro de un enlace. El espacio en
blanco de una línea con código en línea se colapsa como en cualquier párrafo; lo que conserva los espacios es el bloque.

Un bloque de código es un `Text` con `White space: pre` ([Formato del texto](../comun/formato-del-texto.md#texto-preformateado-white-space))
y la familia de `Code font`, así que se parte entre líneas entre páginas y una línea más ancha que la caja es un error
(`TEXT_OVERFLOW`); no tiene `Id` ni `Class`, pero es el elemento generado `Code block` y lo nombra la regla
`Element: Code block` ([Styles](../comun/estilos.md)), con `Box`, `Typography` y `Break` (un fondo, un relleno, un tamaño
menor, `Inside: avoid`…). No lo alcanza `Element: Text` (que sigue valiendo para los `Text` de los encabezados de un
Markdown y para los que se escriben); un `Text` escrito a mano con `White space: pre` tampoco es un `Code block`. Su
`Typography` es el del `Markdown` con la fuente cambiada, así que `Size`, `Color` o `Tab size` se declaran en el
`Typography` del `Markdown` o en `Element: Code block`. Se usa el `Line height` de la caja, no el de la fuente del código,
así que las líneas con código en línea no cambian de alto.

`Code font` solo es obligatoria si hay código (`MARKDOWN_PROPERTY_MISSING`); si la familia no existe, el error es
`FONT_UNKNOWN`, con la línea de `Code font` para un bloque y la del párrafo para el código en línea. Un texto con
`Inline: markdown` no tiene `Code font`, así que su código sigue siendo `MARKDOWN_UNSUPPORTED`.

> **Decisión:** `Code font` es una propiedad del `Markdown`, como `Bullet` (obligatoria solo si hay código), no de
> `Typography`: por eso un texto con `Inline: markdown` no tiene código. *Alternativa descartada:* `Code font` en
> `Typography`, heredable, que valdría también en `Inline: markdown`.

> **Decisión:** el código en línea es un tramo con otra familia, no un elemento: se suma a la negrita y la cursiva que lo
> rodean, puede ir dentro de un enlace y no cambia el alto de la línea ni la línea base, que son los de la fuente de la
> caja. *Alternativa descartada:* un código en línea que conserve los espacios, o con la altura de línea de su fuente.

> **Decisión:** el bloque de código es el elemento generado `Code block`, que las reglas `Element` nombran como las entradas
> de un `Contents` y que no se escribe en el documento. Sigue siendo un texto (un `P` en el PDF etiquetado), sin caja
> envoltorio: su fondo y su relleno los da `Element: Code block`, y `Element: Text` ya no lo alcanza. *Alternativa
> descartada:* un `Code block` también escribible, o una clase automática `code`, que reservaría un nombre de clase al
> autor.

## Listas

El nivel de una lista es el número de listas de su mismo tipo que la contienen: la primera lista con viñetas usa el primer
marcador de `Bullet`; una con viñetas dentro de esa, el segundo, y así hasta el último, que sirve para los niveles más
profundos. Las numeradas cuentan aparte, con `Numbering`. Cada párrafo de un ítem es un `Paragraph`, y una lista anidada,
una `List` dentro del `List item`. Las listas compactas y las separadas por líneas en blanco se traducen igual: el espacio
entre ítems lo dan las reglas de `Styles`. Como cualquier lista, la sangría que deja sitio al marcador es su `Padding`
izquierdo, que se suele declarar en la regla `Element: List` ([List](list.md)).

> **Decisión:** el nivel de una lista se cuenta por tipo: una lista numerada dentro de una con viñetas usa el primer marcador
> de `Numbering`, y `Start` solo se genera si el primer número no es 1. *Alternativa descartada:* un nivel común a los dos
> tipos.

## Tablas

Cada columna tiene la alineación de la fila de separación (`:--`, `:-:`, `--:`) como `Align` de sus celdas; sin alineación,
la de la caja. Una celda con texto es una `Cell` con valor; una vacía, una `Cell` vacía. A una fila con menos celdas que la
cabecera se le añaden celdas vacías y las que sobran se descartan. Las celdas reciben las reglas de las celdas de cualquier
tabla ([Table](table.md#estilo-de-las-celdas)); las tablas de Markdown no tienen cabeceras de fila (`Header`).

> **Decisión:** las filas con celdas de menos o de más siguen a GFM: se completan con celdas vacías y las que sobran se
> descartan, sin error, a diferencia de una `Table` escrita en STXT. El parser no conserva las celdas que sobran.
> *Alternativa descartada:* contar las celdas de cada línea en el texto y dar el error.

## Estilo

Lo generado no tiene `Id` ni `Class`: se le da estilo con las reglas `Element` de `Styles` (`Element: Heading 1`,
`Element: Paragraph`, `Element: Code block`…), que se aplican a todo el documento, y con el `Typography` del `Markdown`, que
hereda.

## Encabezados

`#` es siempre `Heading 1`, y la jerarquía se comprueba en orden del documento junto con los encabezados escritos fuera del
`Markdown` ([Encabezados](heading.md#jerarquía)): después de un `Heading 1`, un Markdown puede empezar por `##`. Se numeran
como los demás y entran en los índices, pero no se pueden referir ni enlazar a mano, porque no tienen `Id`. Dentro de un
`Fixed`, un Markdown no puede tener encabezados.

## Dónde va

Donde va un elemento en flujo: en una sección, `Block`, `Relative`, `Absolute`, `Fixed`, `Multicol`, `List item`, `Cell` y
como elemento de un `Item` o un `Area`.

## Errores

Los errores de lo generado señalan la línea del documento donde empieza su bloque de Markdown (la del ítem, la de la fila de
la tabla); los del marcador de una lista, la de `Bullet` o `Numbering`, y los de las columnas de una tabla, la de
`Table columns`. Una lista o una tabla sin su propiedad, o código sin `Code font`, es un error con la línea de la lista, la
tabla o el código (`MARKDOWN_PROPERTY_MISSING`); una propiedad que no se usa no lo es.

> **Decisión:** las líneas de los errores son las del principio de cada bloque: los de un párrafo de varias líneas señalan
> la primera, y los de una celda, la de su fila, porque el parser no da la línea de cada celda. *Alternativa descartada:*
> posiciones dentro del bloque.

## Salida

En el PDF etiquetado, el `Markdown` es un `Div` y lo generado cuelga de él con sus etiquetas (`H1`, `P`, `L`, `Table`…); sus
encabezados son marcadores. En el volcado solo sale el `Markdown`, si tiene `Id`: lo generado no lo tiene.

> **Decisión:** el `Markdown` es un `Div` en el PDF etiquetado, como un `Block`, y lo generado cuelga de él. *Alternativa
> descartada:* que no tenga elemento propio y lo generado cuelgue de su padre.

## Lo que no hay

- Citas, HTML, líneas horizontales, saltos de línea forzados, notas al pie, listas de tareas y resaltado de código.
- Imágenes dentro de un texto y enlaces con título o con definición aparte.
- `Id` y `Class` en lo generado.
