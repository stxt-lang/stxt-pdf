# `Footnote`

Una `Footnote` es una **nota al pie**: el contenido de una nota que el motor coloca al pie de la página donde el texto
la llama por primera vez, bajo un filete, numerada en el orden de las llamadas. La llamada se escribe en el texto como `[^id]`, con
`Markup: markdown` o dentro de un `Markdown` ([Formato del texto](../common/text-formatting.md#formato-en-línea-markup)), y
se pinta en superíndice; la nota empieza con el mismo número. Una `Footnote` admite `Id` (obligatorio: es el nombre que
usa la llamada), `Style`, `Box` y `Typography`, y su contenido son `Paragraph`, `Text`, `Markdown`, `List`, `Table` y
`Block`. Se escribe donde va un elemento en flujo (en la `Section`, en un `Block`, en un `List item`, en una `Cell`, en un
`Columns` o en un `Frame`), antes o después de su llamada, y **no ocupa sitio ahí**: solo cuenta su `Id`.

```
Paragraph: El plazo es de seis semanas[^plazo] desde la firma.
    Typography:
        Markup: markdown
Footnote:
    Id: plazo
    Paragraph: Los plazos se cuentan en semanas naturales.
```

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Id` | texto | obligatorio | El nombre de la nota, el de su llamada `[^id]`. Único en el documento, como todo `Id`: también es el destino de `Link: #id` y de `{page of: id}`. |
| `Style` | nombres separados por espacios | sin valor | Le aplican las reglas `For style` con esos nombres. |
| `Box`, `Typography` | los grupos | sin valor | La caja y la tipografía de la nota, sobre lo que hereda del área ([abajo](#el-área-de-notas-footnotes)). |

No admite `Break`: no se parte donde se escribe, sino al pie de la página ([colocación](#colocación)).

## La llamada

`[^id]` es la llamada a la nota con ese `Id`. Solo se interpreta donde se interpreta el Markdown: en un texto con
`Markup: markdown` (un `Text`, un `Paragraph`, una `Cell`) o en un `Markdown`. En un texto literal, `[^id]` es texto.

- La llamada es un **tramo en superíndice** con el número de la nota: se mide y se pinta al 70 % del cuerpo, elevado un
  tercio del cuerpo, con la fuente, el color y la negrita o cursiva del texto que la rodea. No cambia el alto de la
  línea. Va pegada a la palabra anterior, como se escribe: `semanas[^plazo]`.
- **Las notas se numeran en el orden de su primera llamada**, con el formato de la sección
  ([numeración](#numeración)). El número es texto: mide lo que se pinta y lo lee un lector de pantalla.
- **Una nota puede llamarse varias veces.** La primera llamada le da el número, y las siguientes lo repiten: las
  celdas de una tabla que remiten a la misma nota llevan todas el mismo asterisco. La nota es una, y va al pie de la
  página de su primera llamada ([colocación](#colocación)).
- Una llamada sin `Footnote` con ese `Id` en su sección es `FOOTNOTE_UNKNOWN`, también una segunda llamada desde otra
  sección, y una `Footnote` que nadie llama, `FOOTNOTE_NOT_CALLED`.
- **Dónde no va una llamada** (`FOOTNOTE_CALL_NOT_ALLOWED`): en un `Page overlay`, que se repite en cada página; en un
  encabezado, cuyo texto copian los índices y los marcadores; y dentro de otra nota.

> **Decisión:** la llamada es la sintaxis de notas de Markdown, `[^id]`, y la nota es un elemento STXT con ese `Id`, no
> una definición `[^id]: ...` dentro del Markdown: así la nota tiene cajas, estilos y varios párrafos, como cualquier
> contenido, y se escribe también desde un `Paragraph` suelto. *Alternativa descartada:* un marcador propio
> (`{note: id}`) en cualquier texto, que no sería un tramo y no podría ir en superíndice.

> **Decisión:** una nota llamada varias veces se coloca una vez, en la página de su primera llamada, y las demás
> llamadas repiten su número aunque caigan en otra página, como `\footnotemark` de LaTeX. El uso corriente es una tabla
> o un párrafo, en una página. *Alternativa descartada:* repetir la nota al pie de cada página que la llama, que duplica
> la nota en el PDF etiquetado, donde un `Note` tiene un identificador único. Hasta la 0.16.0 una segunda llamada era
> un error (`FOOTNOTE_CALL_DUPLICATE`).

## La nota

La nota es un bloque: apila su contenido en flujo de bloque ([Block](block.md)). **Empieza con un `Paragraph`, un
`Text` o un `Markdown` cuyo primer bloque sea un párrafo**, porque ese texto recibe el número: el motor escribe delante
de él el número en superíndice y un espacio, como la llamada (`¹ Los plazos...`). Otra cosa en primer lugar es
`FOOTNOTE_CONTENT`. Después puede seguir lo que admite: párrafos, listas, tablas y bloques; su `Markdown` no puede
generar encabezados, imágenes ni bloques de código (`FOOTNOTE_CONTENT`).

La nota hereda la tipografía del **área de notas** de su sección, no de la caja donde se escribe: una nota escrita en
una celda alineada a la derecha no sale alineada a la derecha. `For element: Footnote` estila todas las notas (un
`Margin` inferior las separa), y `Style` o `Id`, una en concreto. Las notas de una página se apilan en el área como los
hijos de un bloque: sus márgenes y los que salen de ellas (el margen superior de su primer párrafo, por ejemplo) se
fusionan entre notas ([Fusión de márgenes](block.md#fusión-de-márgenes)).

Una `Footnote` no puede estar dentro de un `Page overlay`, que se repite en cada página, ni dentro de otra `Footnote`
(`FOOTNOTE_NOT_ALLOWED`). La nota va en la **sección de su llamada**: una llamada cuya nota está en otra sección es
`FOOTNOTE_UNKNOWN`.

> **Decisión:** el número va en superíndice al principio del primer texto, sin sangría francesa, como en Word y en LaTeX:
> las líneas siguientes vuelven al margen. *Alternativa descartada:* el número colgado a la izquierda, como el
> marcador de un `List item`, que pide un `Padding` en el área y una regla más.

> **Decisión:** la nota hereda del área y no de donde se escribe: lo que la rodea en el documento no tiene nada que ver
> con el pie de la página, donde se lee.

## El área de notas (`Footnote area`)

Las notas de una página van en el **área de notas**, una caja generada al pie del área útil, con el ancho del área
útil, que el motor coloca en cada página que tiene notas. Las reglas `For element: Footnote area` la estilan, y es lo que
las notas heredan:

- su `Typography` es la tipografía de todas las notas (`Size: 8`, por ejemplo);
- su `Box` da el **filete** (el `Border` superior, con su `Border color` y su `Border style`), la separación con el texto (el `Margin` superior) y el espacio hasta
  la primera nota (el `Padding` superior), más el fondo, los demás bordes y los márgenes laterales.

Por defecto el área tiene un margen superior de 8 pt, un filete de 0,5 pt del color del texto a todo el ancho y un
relleno superior de 4 pt. `Height`, `Min height`, `Max height`, `Aspect ratio`, `Rotate` y `Overflow` no tienen efecto en
ella y son un error (`FOOTNOTES_PROPERTY_WITHOUT_EFFECT`): mide lo que miden las notas de cada página.

```
Styles:
    For element: Footnote area
        Typography:
            Size: 8
            Line height: 11
        Box:
            Margin: 12 0 0 0
            Border: 0.75 0 0 0
            Border color: #999999
            Padding: 4 0 0 0
    For element: Footnote
        Box:
            Margin: 0 0 3 0
```

> **Decisión:** el filete es el borde superior del área, a todo el ancho, y no una propiedad aparte (`Footnote rule`):
> con `Border`, `Border color`, `Margin` y `Padding` se dibuja y se separa como cualquier caja. *Alternativa descartada:*
> un filete corto, de un tercio del ancho, como el de Word y LaTeX, que pedía una propiedad con su medida.

## Colocación

Las notas quitan sitio al texto de la página, y qué notas hay depende de cuánto texto cabe. La regla es esta:

- **Una nota va en la página de su primera llamada.** Las llamadas siguientes a esa nota, en la misma página o en
  otra, no traen nada al pie.
- **Una línea entra en la página solo si su nota cabe debajo**, entera o al menos su principio. Si no cabe, la línea pasa
  a la página siguiente con su nota, y con ella lo que la sigue. Lo que decide es la línea: una tabla o un `Block` con
  `Keep together` que no se parte se mueve entero con su nota.
- **Una nota puede partirse** entre el pie de su página y el de la siguiente, con las reglas de viudas y huérfanas y de
  `Keep` de su contenido ([Cortes](../common/breaks.md)); si el corte viola alguna, la línea pasa a la página siguiente.
  Lo que sigue de una nota va al principio del área de la página siguiente, antes de las notas propias de esa página.
  Una nota no se parte si el texto de su sección acaba en su página: se mueve con su llamada.
- El área ocupa **lo mínimo**: el texto llena la página hasta donde las notas de sus llamadas caben debajo, y el espacio
  que sobra, si lo hay, queda entre el texto y el filete.
- Una nota que no cabe en una página con el texto que la llama, ni partida, es `FOOTNOTE_TOO_TALL`.
- En un **`Columns`**, las notas van al pie de la página, a todo el ancho, no al pie de la columna.
- El texto de las notas se pinta con los `Page overlay` de la página, y `{page}` y `{pages}` en una nota valen lo que en
  la página.

> **Decisión:** las notas van al pie de la página también dentro de un `Columns`, como en las revistas: es más sencillo
> y deja las columnas iguales. *Alternativa descartada:* el pie de cada columna.

> **Decisión:** una nota no se parte si el texto de la sección acaba en su página: la nota entera pasa con su llamada a la
> página siguiente, en lugar de abrir una página solo con el resto de la nota.

## Numeración

Las notas se numeran por documento, en el orden de su primera llamada, con dos propiedades de `Section`:

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Footnote numbering` | `continue`, `restart` | `continue` | `continue` sigue el contador del documento y `restart` vuelve a empezar en 1 en esta sección. |
| `Footnote number format` | `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman`, `symbols` | `decimal` | Cómo se escriben los números de las notas de la sección, en la llamada y en la nota: `1`, `a`, `A`, `i`, `I`, `*`. |

Con `symbols`, las notas llevan un símbolo en lugar de un número: `*`, `†`, `‡` y `§`, en ese orden, y después
doblados (`**`, `††`, `‡‡`, `§§`), triplicados, etc. Es la nota de un documento comercial, con una o dos por sección:

```
Section:
    Footnote number format: symbols
    Paragraph: Cuota mensual: 49 €[^iva]. Alta: 20 €[^iva].       # «49 €*» y «20 €*»
        Typography:
            Markup: markdown
    Footnote:
        Id: iva
        Paragraph: IVA no incluido.                               # «* IVA no incluido.»
```

El símbolo va en superíndice, como el número, y la fuente tiene que tenerlo (`GLYPH_MISSING` si no). Con
`Footnote numbering: restart`, cada sección vuelve a empezar por el asterisco.

> **Decisión:** los símbolos son cuatro (`*`, `†`, `‡`, `§`) y después se doblan, como en Word, y siguen el contador de
> la sección. *Alternativa descartada:* reiniciarlos en cada página (`Footnote numbering: page`), que es como se usan en
> un texto largo y hace depender el símbolo de la paginación.

## Referencias y enlaces

Una `Footnote` tiene `Id`, así que `{page of: id}` da la página donde está (la de su primera llamada) y `Link: #id` lleva a
ella ([Referencias](../common/references.md), [Enlaces](../common/links.md)).

## Errores

| Código | Cuándo |
|---|---|
| `FOOTNOTE_UNKNOWN` | Una llamada sin `Footnote` con ese `Id` en su sección, o en otra sección que la primera llamada a esa nota |
| `FOOTNOTE_NOT_CALLED` | Una `Footnote` que ningún texto llama |
| `FOOTNOTE_CALL_NOT_ALLOWED` | Una llamada en un `Page overlay`, en un encabezado o dentro de una nota |
| `FOOTNOTE_NOT_ALLOWED` | Una `Footnote` dentro de un `Page overlay` o de otra `Footnote` |
| `FOOTNOTE_CONTENT` | Una nota que no empieza por un párrafo, o cuyo `Markdown` genera encabezados, imágenes o bloques de código |
| `FOOTNOTES_PROPERTY_WITHOUT_EFFECT` | Una propiedad de `For element: Footnote area` sin efecto en el área: su alto, `Rotate`, `Overflow` |
| `FOOTNOTE_TOO_TALL` | Una nota que no cabe en una página con el texto que la llama |

## Accesibilidad

En el PDF etiquetado, la nota es un `Note` (PDF/UA-1) o un `FENote` (PDF/UA-2), con el número en un `Lbl`, y va **justo
después del párrafo que la llama** por primera vez, no al final de la página: un lector de pantalla la lee tras su
párrafo. Cada llamada es un `Reference` dentro de su párrafo, que señala la nota con `Ref`, y la nota señala todas sus
llamadas
([El PDF por dentro](../implementation/pdf.md#árbol-de-estructura)).

## Lo que no hay

- **Numerar por página** (`Footnote numbering: page`): el número dependería de la paginación, como `{page}`.
- Un estilo propio de la llamada y del número de la nota (`For element: Footnote mark`): la llamada toma el formato
  del texto que la rodea. Tampoco otra lista de símbolos que la de `symbols`.
- Repetir una nota al pie de cada página que la llama, notas al final del documento o de la sección (notas finales) y
  notas al pie de una tabla.
- La llamada en un texto literal (sin `Markup: markdown`), en un `Page overlay` o en un encabezado.
