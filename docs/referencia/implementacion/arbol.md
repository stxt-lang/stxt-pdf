# El árbol de cajas

Cómo pasa el documento leído a un árbol de `Box` (package `tree`). Lo que dice cada regla está en las páginas de
comportamiento; aquí, dónde vive en el código.

## Los nodos de `stxt-core`

El motor trabaja directamente con `dev.stxt.Node`, `InlineNode` (valor en línea e hijos) y `TextNode` (bloque `>>`);
`getText()` da el contenido en los dos casos. Los nombres se comparan por `getCanonicalName()`: minúsculas y los espacios
convertidos en guiones (`Min width` → `min-width`). Los errores de sintaxis y del schema son `ParseException` de la
librería, con código (`INVALID_VALUE`, `CHILD_NOT_DECLARED`…) y línea, que `DocumentReader` y `DocumentSchema` convierten
en `DocumentSyntaxException` ([Uso desde Java](api.md#errores)).

## Validar contra el schema

La estructura solo está en el schema: `DocumentSchema` lo compila una vez, `DocumentReader` valida con él al leer y
`BoxTreeBuilder` vuelve a validar el árbol al empezar, así que el resto del builder da la estructura por buena.

En el schema, los elementos contenedores y los grupos son `GROUP` (sin valor); `Text`, `Paragraph`, `Cell`, `Image`,
`Area` y las reglas de `Styles`, `INLINE`; `Content`, `BLOCK`; `Areas`, `TEXT`; `Isolate`, `Wrap`, `Header` e
`In contents`, `BOOLEAN`; `Layer`, `Widows`, `Orphans`, `Start`, `Number` y `Count`, `INTEGER`; `Rotate`, `NUMBER`; las de
valores cerrados (`Fit`, `Leader`, `Pages`, `Paper`…), `ENUM`, y el resto, `INLINE` sin hijos cuya forma comprueba
`ValueReader`.

Lo que el schema no puede expresar lo comprueba `BoxTreeBuilder`: que `Media` declare su tamaño de una sola forma, que
`Item` y `Area` tengan exactamente un elemento y un `List item` al menos uno (el schema cuenta por tipo), que un `Text`
tenga su texto en un sitio y solo uno, que la raíz sea `Document`, que el `Text` de un encabezado no tenga `Break`, la
jerarquía y el sitio de los encabezados y, en una `Table`, el orden `Head`, `Body`, `Foot`, que una `Cell` no tenga a la
vez valor y elementos y que cada fila llene sus columnas. Lo que genera un `Markdown` pasa por el schema como lo que
escribe el autor.

> **Decisión:** lo que ya rechaza el schema no tiene código propio del motor: `Main axis: stretch`, `Inside: page` o un
> bloque `>>` donde van hijos son errores del schema, con su código de `stxt-core`.

> **Decisión:** `BoxTreeBuilder` valida contra el schema al empezar, aunque el documento venga de `DocumentReader`: un árbol
> construido sin validar (`STXT.rawParser()`) da los mismos errores que el lector, con los códigos de `stxt-core`, en lugar de
> interpretar mal una propiedad fuera de su sitio. Cuesta validar dos veces lo que viene del lector. *Alternativa
> descartada:* una copia de la tabla de hijos en el builder, o no comprobar nada sin validar.

## `BoxTreeBuilder`

Traduce cada elemento a una `Box` (`Style.layout` y `Style.position` salen del elemento; las propiedades de `Item` y `Area`
pasan al elemento que envuelven), interpreta valores, aplica la herencia y comprueba las reglas que el schema no puede
expresar. Los errores son `InvalidModelException` con un `ErrorCode` y la **línea del nodo culpable**: la de la propiedad
(`Style.line(HEIGHT)`, con las claves de `PropertyKey`), no la de su caja. `PropertyKey.internal` da el nombre interno de
los nombres cortos (`Size` → `font-size`), que es la clave de `Style.line`.

`BoxTreeBuilder` es el orquestador y no guarda estado: lo de cada llamada va en un `BuildContext`. `BoxTreeBuilder.element`
es el orden común a todos los elementos (el sitio, la herencia, las reglas, las propiedades y sus comprobaciones, la caja y
el contenido) y lo propio de cada uno lo hace su familia, `ElementReader` (un enum: `TEXT`, `IMAGE`, `CELL`, `TABLE`,
`GRID`, `FLEX`, `LIST`, `MULTICOL`, `MARKDOWN`, `CONTENTS` y `CONTAINER`, elegida con `ElementReader.of`): el layout de la
caja, lo que le da su envoltorio, las propiedades de su tipo, lo que comprueba antes de crear la caja y su contenido. Un
elemento nuevo sin nada propio es un `CONTAINER`; con algo propio, una familia nueva, sin tocar `element`.

Las piezas:

| Clase | Qué hace |
|---|---|
| `MediaReader` | `Media` (con la herencia por grupos de las secciones), `Title` y `Language` |
| `ValueReader` | Interpreta los valores de cada propiedad y comprueba sus formas |
| `PropertyReader` | Los grupos, las propiedades de `Flex`, `Grid` e `Item` y las propias de los elementos, con tablas `Map<String, Property>` |
| `StyleRules` | Las reglas de `Styles` de los dos orígenes y su prioridad |
| `TableBuilder`, `CellPosition`, `CellSelector` | Las tablas: la colocación de las celdas y su posición para las reglas |
| `ListBuilder`, `ListMarkers` | Las listas: la numeración y la caja del marcador |
| `MarkdownBuilder` | Traduce un `Markdown` (y el texto con `Inline: markdown`) a nodos STXT |
| `ContentsBuilder` | Las entradas de los índices, al final de la construcción |
| `HeadingNumbers` | Los números de los encabezados |
| `References`, `LinkTargets` | Comprueban las referencias y los enlaces `#id` del árbol entero |
| `ModelValidator` | Las reglas que miran la caja ya leída (márgenes, mínimos y máximos, alto definido, giro…) |
| `Nodes` | El acceso a los nodos y los errores |

Ningún método de `tree` pasa de 60 líneas ni de complejidad 20.

> **Decisión:** las familias de elementos son un enum con métodos por defecto, no una interfaz con una clase por familia:
> son pocas, están cerradas (las da el schema) y comparten casi todo. *Alternativa descartada:* una interfaz con una clase
> por familia, o las familias como métodos privados del builder.

## `Box` y `Style`

`Box` tiene los campos privados, se lee con métodos (`text()`, `children()`, `formattedRuns()`…) y se escribe con métodos
`set…` del package. `Style` se parte en un grupo por cada grupo del documento (`TypographyStyle`, `BoxStyle`, `BreakStyle`)
y por cada tipo de elemento con propiedades propias (`FlexStyle`, `GridStyle`, `ItemStyle`, `ListStyle`, `MulticolStyle`,
`ImageStyle`, `ContentsStyle`); en `Style` quedan las propiedades que definen el elemento y las líneas. Fuera de `tree` se
lee a través del grupo: `style.typography().fontSize()`, `style.flex().direction()`, `style.box().margin()`. Solo se hereda
`TypographyStyle`: `Style.inheriting` copia el grupo del padre. Una propiedad nueva va en el grupo de su sitio en el
documento, con su lectura y su `set…`.

`BoxTreeBuilder.build` cierra el árbol al terminar (`Box.close`, que cierra también el estilo): los hijos pasan a una lista
inmutable, los que están en flujo se calculan una vez y escribir en una caja o un estilo cerrados es un
`IllegalStateException`. Un test de otro package que necesita un estilo hecho a mano usa `StyleEditor` (en el package
`tree` de los tests). La igualdad de `Box` es por identidad: es la clave de la caché del layout.

> **Decisión:** el árbol de cajas se cierra al construirlo y solo `tree` lo escribe: así «el layout no escribe en el árbol»
> deja de ser una convención y un documento maquetado se puede leer desde varios hilos. *Alternativa descartada:* accesores
> sin cierre, o un `StyleBuilder` aparte.

> **Decisión:** los grupos de `Style` son clases que se cierran con la caja, no `record`: el builder escribe las propiedades
> de una en una (los grupos, después las reglas por prioridad y después lo del elemento), y con `record` cada escritura
> rehace un registro de hasta 19 componentes. *Alternativa descartada:* `record` inmutables por grupo.

> **Decisión:** los enums de los valores están en `model`, uno por fichero, porque los usan varios grupos y `model` no
> puede depender de `tree`. Las constantes se llaman como el valor del documento (`TextAlign.JUSTIFY` ↔ `justify`), porque
> se leen con `Enum.valueOf` sobre el texto en mayúsculas y con el guion como guion bajo. El de `Fill` de `Multicol` se
> llama `ColumnFill`.

> **Decisión:** `Box.describe()` es la única forma de nombrar una caja en los mensajes (`main (línea 12)`), y el volcado
> escribe el `Id`.

## Las secciones y la raíz

`BoxTreeBuilder.Result` da la raíz del documento y la lista de `Section` (`tree/Section`: caja, `Media`, `Page numbering` e
`Inherit fixed`). La **caja de cada sección es la raíz de su layout** (`Element.SECTION`, una `Box` **sin `parent`**) y
**cuelga de `root.children()` sin ser hija** (`root.addSection`, no `addChild`): los recorridos de todo el árbol
(referencias, enlaces, índices, fuentes, imágenes, `Id`) parten de la raíz y las ven, y el layout, los `Fixed` y subir por
`parent` se quedan dentro de la sección. Los `Fixed` hijos de `Document` también son hijos de la raíz (con `parent`) y no se
maquetan en el flujo: los coloca el `Paginator`.

> **Decisión:** una sección es la raíz de su layout, sin padre, y cuelga de la raíz del documento sin ser hija, así que el
> layout, la caché y todo lo que miraba `parent == null` siguen igual, y los recorridos del árbol entero la ven. Lo que se
> rompe es la invariante «el padre de mi hijo soy yo»: solo vale para las secciones. *Alternativa descartada:* pasar a los
> recorridos la lista de secciones.

## Texto y tramos

`Box.text()` es siempre el texto entero y `Box.formattedRuns()` (`null` sin formato) lo parte en `TextRun` con un
`TextFormat`; `Box.runs()` da un único tramo si no hay formato. Los tests de layout ponen tramos en el árbol antes de
cerrarlo con `TextRuns`.

> **Decisión:** el texto de la caja sigue siendo una cadena y los tramos son una partición suya: las líneas del documento,
> los mensajes, los marcadores del PDF y `{page}` trabajan sobre el texto entero, y un texto sin formato se mide y se pinta
> igual que antes. *Alternativa descartada:* guardar solo los tramos y derivar el texto.

## Elementos generados

- **Marcador de lista:** `ListBuilder` crea en `Box.marker` la caja del marcador, generada y fuera de `children`
  (`Element.MARKER`, `Box.isMarker()`): texto con la tipografía del ítem y el espacio final incluido (`"3. "`).
- **Flujo de columnas:** el contenido de un `Multicol` va en una caja de bloque generada (`Element.COLUMNS`), su único hijo.
- **Entradas de índice:** `ContentsBuilder` genera al final una caja `Flex` por encabezado (`Element.CONTENTS_ENTRY_1`…`6`)
  con el título, la guía y el número. Un encabezado sin `Id` recibe un nombre generado (`Box.anchor`); `Box.key()` da el
  `Id` o ese nombre.
- **Bloque de código:** `MarkdownBuilder` marca el nodo `Text` que genera (`Translation.codeBlocks`) y
  `BoxTreeBuilder.element` le da el elemento `Element.CODE_BLOCK`.

Los elementos generados que se nombran en `Styles` sin escribirse en el documento son `Element.selectable()`.

## Markdown

> **Decisión:** el Markdown se traduce a nodos STXT, no a cajas: lo generado se construye como cualquier otro elemento, así
> que las reglas de `Styles`, la validación, el PDF etiquetado y los marcadores no tienen ningún caso especial, y lo generado
> se valida además contra el schema. *Alternativa descartada:* construir las cajas desde el árbol del parser.

commonmark-java no conserva las celdas que sobran de una fila de tabla ni da la línea de cada celda: los errores de una
celda señalan la de su fila.

`MarkdownBuilder` parsea el `Content` con commonmark-java 0.30.1 (con las extensiones de tablas, tachado y subrayado) y lo
traduce a nodos STXT (`InlineNode` con la línea de su bloque), que valida con el schema; los tramos de cada texto van aparte,
por nodo (`Translation.runs`, en `BuildContext.runs`). `BoxTreeBuilder.markdown` construye esos nodos con `element(...)` como
hijos de la caja del `Markdown`. El texto con `Inline: markdown` pasa por `MarkdownBuilder.inline`, el mismo parser y la
misma traducción en modo `inlineOnly`; dentro de un `Markdown` no actúa (`BuildContext.insideMarkdown`).
