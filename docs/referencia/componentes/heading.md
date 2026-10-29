# Encabezados: `Heading 1` … `Heading 6`

Un `Heading N` es un **bloque** cuyo contenido son uno o varios `Text`, apilados como en un `Block`; además admite `Id`,
`Class`, `Link`, `In contents`, `Box` y `Typography`, y nada más. Su `Typography` lo heredan sus `Text`. Se maqueta con el
flujo de bloque de siempre; su significado se ve en las reglas `Element`, en los cortes, en la numeración, en los índices
y en el PDF (etiquetas y marcadores).

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

> **Decisión:** un encabezado es un bloque con hijos `Text`, no un `Text` con otro nombre: su texto, en el índice y en los
> marcadores, es el de sus `Text` unido por espacios. *Alternativa descartada:* el texto en el valor o en `Content`, como
> un `Text`.

## Cortes

El motor **evita siempre** cortar dentro de un encabezado y después de él, como si declarara `Inside: avoid` y
`After: avoid` ([Cortes](../comun/cortes.md)): un título no queda solo al final de una página ni se parte. Son `avoid`, así
que se relajan como cualquier otro cuando no hay un corte mejor. Por eso un encabezado no admite `Break`, ni sus `Text`
tampoco (`BREAK_IN_HEADING`), ni de una regla (`STYLE_NOT_ALLOWED`). El corte antes del encabezado no cambia.

> **Decisión:** el motor evita siempre el corte dentro y después de un encabezado, en lugar de dejarlo al autor con
> `Break`. Son `avoid` normales y el encabezado no admite `Break`. *Alternativa descartada:* un valor por defecto que el
> autor puede cambiar, que contradice que no haya estilos implícitos.

## Jerarquía

Los niveles tienen que ser correctos (`HEADING_LEVEL`). En orden del documento, a través de las secciones y de los
`Markdown`, el primer encabezado es `Heading 1`, y cada uno sube como mucho un nivel respecto al anterior: a un `Heading 1`
le puede seguir un `Heading 2`, pero no un `Heading 3`. Se puede bajar los niveles que se quiera.

> **Decisión:** los saltos de nivel son un error, y el primer encabezado tiene que ser `Heading 1`: así los marcadores y la
> estructura del PDF son siempre un árbol bien formado. *Alternativa descartada:* permitirlos y colgar el marcador del
> último de nivel menor.

## Dónde va

Donde va un elemento en flujo, también como ítem de `Flex` o `Grid`; nunca dentro de un `Fixed` (`HEADING_IN_FIXED`), que
se repite en cada página y no forma parte del contenido, ni dentro de un `List item` o una `Cell`.

> **Decisión:** un encabezado dentro de un `Fixed` es un error: el `Fixed` se repite en cada página y es un *artifact*, así
> que no tendría marcador ni etiqueta. *Alternativa descartada:* permitirlo como un bloque sin significado.

## Fuera del índice

`In contents: false` deja un encabezado fuera de los índices (`Contents`) sin dejar de ser un encabezado: sigue siendo
`H1`…`H6`, marcador, parte de la jerarquía y destino de `#id`. Es lo normal para el título de la portada o el del propio
índice. Es una propiedad del elemento, por defecto `true`, que no se hereda ni va en `Styles`.

> **Decisión:** `In contents` es una propiedad de cada encabezado, no de `Styles`. El nombre no puede ser `Contents`, que
> ya es un elemento. *Alternativa descartada:* que el índice eligiera por nivel (`Levels`) los que no lista, que no sirve
> cuando la portada y los capítulos son del mismo nivel.

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
  cualquier encabezado de un nivel superior, esté numerado o no. El número es el de los niveles 1 a N unidos por puntos, en
  decimal, **sin los niveles que no tienen número**. Un encabezado sin numerar no cuenta ni lleva número.
- **Un título sin número.** Lo normal: el título del documento es un `Heading 1` con `Numbered: false` y las secciones,
  `Heading 2` numerados, que son `1`, `2`, `3` (y sus `Heading 3`, `2.1`…) porque el título no aporta nivel. Un nuevo
  `Heading 1` sin número (otra parte, un anexo) hace que los contadores de debajo vuelvan a empezar.
- **Se escribe delante del título.** El número y un espacio van delante del primer `Text` del encabezado, como si el autor
  los hubiera escrito: miden lo que escriben, el texto puede pasar a una línea más y lo ven el índice, los marcadores y el
  PDF etiquetado. Un `Transform` no los cambia (son dígitos); `Letter spacing` y `Small caps`, sí.
- **Se puede referir.** `{number of: id}` de un encabezado con `Id` y numerado da su número completo (`1.1`): «la sección
  {number of: ventas}» escribe «la sección 1.1» ([Referencias](../comun/referencias.md#number-of-id)).

> **Decisión:** `Numbered` es una propiedad de `Typography`, booleana y heredable: una propiedad propia del encabezado no se
> podría escribir en una regla `Element: Heading 2`, y de `Typography` sale la herencia. Se llama `Numbered` y no
> `Numbering`, que ya es de `Markdown`. *Alternativa descartada:* `Numbering: decimal` en cada regla, con un formato por
> nivel.

> **Decisión:** solo decimal con puntos (`1`, `1.1`, `1.1.1`), con un contador por nivel que cuenta a través de las
> secciones y los `Markdown`. *Alternativa descartada:* un formato por nivel con los valores de `Marker`, o prefijos y
> sufijos propios (`Anexo A.`).

> **Decisión:** los encabezados sin numerar no aportan nivel al número de los de debajo: el título de un documento es un
> `Heading 1` sin número y sus secciones, `Heading 2` numerados, son `1`, `2`, `3`. *Alternativa descartada:* un error si
> un encabezado numerado está bajo uno sin número, que obliga a numerar el título o a quitarlo de los encabezados.

> **Decisión:** el número es parte del texto del primer `Text` del encabezado, escrito delante con un espacio, no una caja
> aparte como el marcador de una lista: lo miden el layout y el reparto en líneas, y lo ven el índice, los marcadores y el
> PDF etiquetado sin tratarlo aparte. Un encabezado largo continúa bajo el número (no queda sangrado), y el espacio puede
> cortar la línea justo después del número. *Alternativa descartada:* el número colgado a la izquierda, que alinearía los
> títulos pero obligaría a un hueco fijo.

> **Decisión:** `{number of: id}` de un encabezado da su número completo (`4.2`), el mismo que lleva escrito, y no el de su
> último nivel como el de un `List item`. Los encabezados de un `Markdown` se numeran pero no se pueden referir: no llevan
> `Id`.

## Salida PDF

Cada encabezado es un `H1` … `H6` del PDF etiquetado y un marcador del PDF, con el texto de sus `Text` unido por espacios,
anidado por nivel y que lleva a la esquina superior izquierda de su primer fragmento ([El PDF](../comun/pdf.md#marcadores)).
Sus `Text` son `Span` de su texto.

## Lo que no hay

- Estilos por defecto de los niveles: se declaran en `Styles`.
- Formatos de numeración distintos del decimal, prefijos y sufijos propios (`Anexo A.`), número colgado a la izquierda y
  referir los encabezados de un `Markdown`.
