# Encabezados: `Heading 1` … `Heading 6`

Un `Heading N` es un **bloque** cuyo contenido son uno o varios `Text`, apilados como en un `Block`; además admite `Id`,
`Class`, `Link`, `In contents`, `Box` y `Typography`, y nada más. Su `Typography` lo heredan sus `Text`. Se maqueta con el
flujo de bloque de siempre; su significado se ve en las reglas `Element`, en los cortes, en la numeración, en los índices,
en los marcadores del PDF y en lo que anuncia un lector de pantalla.

```
Styles:
	Element: Heading 1
		Typography:
			Size: 20
			Weight: bold
		Box:
			Margin: 0 0 10 0
	Element: Paragraph
		Box:
			Margin: 0 0 6 0
Heading 1:
	Id: results
	Text: Resultados del trimestre
Paragraph:
	Content >>
		Las ventas crecieron un 12 %.
```

**Sin estilos implícitos.** Un `Heading 1` no es más grande que un `Heading 2` por sí mismo: el estilo de cada nivel se
declara una vez en `Styles`, igual que el de cualquier otro elemento.

Su texto, en el índice y en los marcadores, es el de sus `Text` unido por espacios.

## Cortes

El motor **evita siempre** cortar dentro de un encabezado y después de él, como si declarara `Inside: avoid` y
`After: avoid` ([Cortes](../comun/cortes.md)): un título no queda solo al final de una página ni se parte. Son `avoid`, así
que se relajan como cualquier otro cuando no hay un corte mejor. Por eso un encabezado no admite `Break`, ni sus `Text`
tampoco (`BREAK_IN_HEADING`), ni de una regla (`STYLE_NOT_ALLOWED`). El corte antes del encabezado no cambia.

> **Decisión:** el motor lo evita siempre, en lugar de dejarlo al autor. *Alternativa descartada:* un valor por defecto
> que el autor puede cambiar, que contradice que no haya estilos implícitos.

## Jerarquía

Los niveles tienen que ser correctos (`HEADING_LEVEL`). En orden del documento, a través de las secciones y de los
`Markdown`, el primer encabezado es `Heading 1`, y cada uno sube como mucho un nivel respecto al anterior: a un `Heading 1`
le puede seguir un `Heading 2`, pero no un `Heading 3`. Se puede bajar los niveles que se quiera.

> **Decisión:** los saltos de nivel son un error, para que los marcadores sean siempre un árbol bien formado.
> *Alternativa descartada:* permitirlos y colgar el marcador del último de nivel menor.

## Dónde va

Donde va un elemento en flujo, también como ítem de `Flex` o `Grid`; nunca dentro de un `Fixed` (`HEADING_IN_FIXED`), que
se repite en cada página y no forma parte del contenido, ni dentro de un `List item` o una `Cell`.

> **Decisión:** dentro de un `Fixed` sería un encabezado sin marcador que un lector de pantalla no anuncia: es un error.
> *Alternativa descartada:* permitirlo como un bloque sin significado.

## Fuera del índice

`In contents: false` deja un encabezado fuera de los índices (`Contents`) sin dejar de ser un encabezado: sigue teniendo
marcador, contando en la jerarquía y siendo destino de `#id`. Es lo normal para el título de la portada o el del propio
índice. Es una propiedad del elemento, por defecto `true`, que no se hereda ni va en `Styles`.

> **Decisión:** se elige en cada encabezado. *Alternativa descartada:* que el índice eligiera por nivel (`Levels`) los que
> no lista, que no sirve cuando la portada y los capítulos son del mismo nivel.

## Numeración (`Numbered`)

Con `Numbered: true` el encabezado lleva número: `1`, `1.1`, `1.1.1`… Como ningún estilo es implícito, se declara; es una
propiedad de `Typography`, así que **se hereda** y se escribe donde convenga: en `Element: Document` numera todos los
niveles, en una regla `Element: Heading 1` solo los de ese nivel, en una regla `For class` los de una clase, y en la
`Typography` de un encabezado, solo en él (`Numbered: false` lo deja sin número).

```
Styles:
	Element: Document
		Typography:
			Numbered: true
Heading 1:
	Id: portada
	Text: Informe del trimestre
	Typography:
		Numbered: false
Heading 1:
	Text: Resultados            # «1 Resultados»
Heading 2:
	Id: ventas
	Text: Ventas                # «1.1 Ventas»
Paragraph: Los datos de la sección {number of: ventas} lo confirman.
```

- **Cómo se cuenta.** En orden del documento, a través de las `Section` y de los `Markdown` (sus encabezados `#`, `##`… se
  numeran como los demás), con un contador por nivel: sube con cada encabezado numerado de ese nivel y vuelve a 0 con
  cualquier encabezado de un nivel superior, esté numerado o no. El número es el de los niveles 1 a N unidos por puntos,
  **sin los niveles que no tienen número**, cada uno escrito con el `Number style` de su encabezado (abajo). Un
  encabezado sin numerar no cuenta ni lleva número.
- **Un título sin número.** Lo normal: el título del documento es un `Heading 1` con `Numbered: false` y las secciones,
  `Heading 2` numerados, que son `1`, `2`, `3` (y sus `Heading 3`, `2.1`…) porque el título no aporta nivel. Un nuevo
  `Heading 1` sin número (otra parte, un anexo) hace que los contadores de debajo vuelvan a empezar.
- **Se escribe delante del título.** El número (o su `Number label`, abajo) y un espacio van delante del primer `Text`
  del encabezado, como si el autor los hubiera escrito: miden lo que escriben, el texto puede pasar a una línea más
  (continúa bajo el número, sin sangría) y salen en el índice, en los marcadores y en lo que se lee. `Transform`,
  `Letter spacing` y `Small caps` los cambian como al resto del texto (`Transform: uppercase` escribe `ANEXO A.`).
- **Se puede referir.** `{number of: id}` de un encabezado con `Id` y numerado da su número completo (`1.1`): «la sección
  {number of: ventas}» escribe «la sección 1.1» ([Referencias](../comun/referencias.md#number-of-id)).

### Formato y etiqueta (`Number style`, `Number label`)

Dos propiedades de `Typography`, que se heredan y se dan como `Numbered`: en `Element: Heading N` para un nivel o en
`For class` para unos encabezados.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Number style` | `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | `decimal` | Cómo se escribe el contador del nivel del encabezado: `1`, `a`, `A`, `i`, `I`. Las letras siguen tras la `z` con `aa`; los romanos, tras 3999, con más `M`. |
| `Number label` | texto con `{number}` una vez | el número | Lo que va delante del título: `{number}` es el número completo (`Number label: Anexo {number}.` da `Anexo A.`). Sin `{number}`, o con él dos veces, es `VALUE_NOT_ALLOWED`. |

Cada nivel del número completo se escribe con el `Number style` del último encabezado de ese nivel: capítulos
`upper-roman` y apartados `decimal` dan `II.1`. `{number of: id}` da el número, sin la etiqueta: «el anexo
{number of: fees}» escribe «el anexo A». Los anexos se numeran aparte con un encabezado sin número por encima, que
reinicia los contadores de debajo: un `Heading 1` «Anexos» y sus `Heading 2` con `Number style: upper-alpha` son `A`,
`B`…

```
Styles:
	For class: annex
		Typography:
			Numbered: true
			Number style: upper-alpha
			Number label: Anexo {number}.
Heading 1:
	Text: Anexos
Heading 2:
	Id: fees
	Class: annex
	Text: Honorarios            # «Anexo A. Honorarios»
Paragraph: Los importes del anexo {number of: fees}.
```

> **Decisión:** `Numbered`, `Number style` y `Number label` van en `Typography` para poder darlos en una regla y
> heredarlos; el formato es del nivel de cada encabezado y la etiqueta, de todo el número. *Alternativa descartada:* una
> plantilla con el número de cada nivel por separado (`{1}.{2}`), más difícil de escribir y de leer.

> **Decisión:** los romanos pasados de 3999 se escriben con más `M` (4000 es `MMMM`), en lugar de un error que no se
> daría nunca en un documento real. *Alternativa descartada:* volver a cifras, como hacen otros motores, que mezcla
> formatos sin avisar.

> **Decisión:** los encabezados sin numerar no aportan nivel al número de los de debajo. *Alternativa descartada:* un error
> si un encabezado numerado está bajo uno sin número, que obliga a numerar el título.

> **Decisión:** el número es texto del primer `Text`, no una caja aparte como el marcador de una lista. *Alternativa
> descartada:* el número colgado a la izquierda, que alinea los títulos pero obliga a un hueco fijo.

`{number of: id}` da el número completo (`4.2`), no el de su último nivel como el de un `List item`. Los encabezados de un
`Markdown` se numeran pero no se pueden referir: no llevan `Id`.

## Marcadores y accesibilidad

Cada encabezado es un marcador del PDF, anidado por nivel ([El PDF](../comun/pdf.md#marcadores)), y un lector de pantalla
lo anuncia como encabezado de su nivel.

## Lo que no hay

- Estilos por defecto de los niveles: se declaran en `Styles`.
- Número colgado a la izquierda y referir los encabezados de un `Markdown`.
