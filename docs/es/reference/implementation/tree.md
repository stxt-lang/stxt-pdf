# El árbol de cajas

Cómo pasa el documento leído a un árbol de `Box` (package `tree`). Lo que dice cada regla está en las páginas de
comportamiento, y aquí, dónde vive en el código.

## Los nodos de `stxt-core`

El motor trabaja directamente con `dev.stxt.Node`, `InlineNode` (valor en línea e hijos) y `TextNode` (bloque `>>`);
`getText()` da el contenido en los dos casos. Los nombres se comparan por `getCanonicalName()`: minúsculas y los espacios
convertidos en guiones (`Min width` → `min-width`). Los errores de sintaxis y del schema son `ParseException` de la
librería, con código (como `INVALID_VALUE` o `CHILD_NOT_DECLARED`) y línea, que `DocumentReader` y `DocumentSchema` convierten
en `DocumentSyntaxException` ([Uso desde Java](api.md#errores)).

## Validar contra el schema

La estructura solo está en el schema: `DocumentSchema` lo compila una vez, `DocumentReader` valida con él al leer y
`BoxTreeBuilder` vuelve a validar el árbol al empezar, así que el resto del builder da la estructura por buena.

En el schema:

- los elementos contenedores y los grupos son `GROUP` (sin valor);
- `Text`, `Paragraph`, `Cell`, `Image`, `Barcode`, `Area`, `Caption`, `Marker image` y las reglas de `Styles`, `INLINE` con
  hijos;
- `Content`, `BLOCK`, y `Area map`, `TEXT`;
- los interruptores (como `Isolate margins`, `Wrap items`, `Row header`, `In table of contents`, `Merge borders` o `Hyphenate`),
  `BOOLEAN`;
- los enteros (como `Layer`, `Widows`, `Orphans`, `First number`, `Number`, `Column count`, `Weight`, `Drop cap` o `Quiet zone`), `INTEGER`,
  y `Rotate`, `NUMBER`;
- las de valores cerrados (como `Fit`, `Leader`, `Pages` o `Paper`), `ENUM`;
- el resto, `INLINE` sin hijos cuya forma comprueba `ValueReader`.

Lo que el schema no puede expresar lo comprueba `BoxTreeBuilder`: que `Page` declare su tamaño de una sola forma, que
`Item`, `Area` y `Figure` tengan exactamente un elemento y un `List item` al menos uno (el schema cuenta por tipo), que un `Text`
tenga su texto en un sitio y solo uno, que la raíz sea `Document`, que el `Text` de un encabezado no tenga `Break`, la
jerarquía y el sitio de los encabezados y, en una `Table`, el orden `Header rows`, `Body rows`, `Footer rows`, que una `Cell`
no tenga a la vez valor y elementos y que cada fila llene sus columnas. Lo que genera un `Markdown` pasa por el schema
como lo que escribe el autor.

> **Decisión:** lo que ya rechaza el schema no tiene código propio del motor: `Direction: row`, `Before: avoid` o un
> bloque `>>` donde van hijos son errores del schema, con su código de `stxt-core`.

> **Decisión:** `BoxTreeBuilder` valida contra el schema al empezar, aunque el documento venga de `DocumentReader`: un árbol
> construido sin validar (`STXT.rawParser()`) da los mismos errores que el lector, con los códigos de `stxt-core`, en lugar de
> interpretar mal una propiedad fuera de su sitio. Cuesta validar dos veces lo que viene del lector. *Alternativa
> descartada:* una copia de la tabla de hijos en el builder, o no comprobar nada sin validar.

## `BoxTreeBuilder`

Traduce cada elemento a una `Box` (`Style.layout` y `Style.position` salen del elemento, y las propiedades de `Item` y
`Area` pasan al elemento que envuelven), interpreta valores, aplica la herencia y comprueba las reglas que el schema no puede
expresar. Los errores son `InvalidModelException` con un `ErrorCode` y la **línea del nodo culpable**: la de la propiedad
(`Style.line(HEIGHT)`, con las claves de `PropertyKey`) y no la de su caja. `PropertyKey.internal` da el nombre interno de
los nombres cortos (`Size` → `font-size`), que es la clave de `Style.line`. La línea es un `SourceLine`: su número y de qué
documento es, el principal o uno de los documentos de estilos aparte (por el lugar en que se pasó, desde 1), porque una
propiedad que llega de una regla de uno de ellos tiene la línea de ese documento (`StyleRules` marca el `Style` mientras
la lee). Se escribe en los mensajes tal cual (`line 8 of the styles` o, si hay varios, `line 8 of styles 2`), y el error
que se construye con ella lo dice también en `inStyles()` y en `stylesDocument()`.

`BoxTreeBuilder` es el orquestador y no guarda estado: lo de cada llamada va en un `BuildContext`. `BoxTreeBuilder.element`
es el orden común a todos los elementos (el sitio, la herencia, las reglas, lo que declara su envoltorio, las propiedades y
sus comprobaciones, la caja y el contenido) y lo propio de cada uno lo hace su familia, `ElementReader` (un enum: `TEXT`, `CODE_BLOCK`, `IMAGE`, `BARCODE`,
`CELL`, `TABLE`, `GRID`, `STACK`, `LIST`, `COLUMNS`, `MARKDOWN`, `TABLE_OF_CONTENTS`, `FOOTNOTE`, `FIGURE` y `CONTAINER`, elegida
con `ElementReader.of`): el layout de la
caja, lo que le da su envoltorio (la `Typography` de la `Row` de una celda antes de las reglas, como herencia, con
`inherited`; las propiedades del `Item` o el `Area` después, para que ganen a las reglas, con `wrapper`), las propiedades de
su tipo, lo que comprueba antes de crear la caja y su contenido. Un
elemento nuevo sin nada propio es un `CONTAINER`, y uno con algo propio, una familia nueva, sin tocar `element`. La familia
`FIGURE` construye el elemento y el `Caption` de una `Figure`, los ordena según `Caption position`, la numera
(`Figures`) y la deja sin partir. Cada familia reclama los hijos que son suyos (`ElementReader.property`): las propiedades
de su tipo, que lee, y los que lee su contenido, como los grupos de filas y `Column align` de una `Table`, `Span` de una
`Cell` o los marcadores y `Table column sizes` de un `Markdown`. El orquestador solo aparta los de cualquier elemento (`Item`,
`Area`, `Content` y `Style`).

Las propiedades de elemento de una regla ([Styles](../common/styles.md#propiedades-de-elemento)) se leen con el mismo código
que las del elemento: `StyleRules` las da a la familia del elemento (`ElementReader.ruleProperty`, que lee las de su tipo y
las sueltas con `PropertyReader.single`), y `Grow`, `Shrink` y `Base size`, a `PropertyReader.itemProperty`. Si el elemento
no puede declarar una, es `STYLE_NOT_ALLOWED`: lo que puede declarar lo dice el schema (`DocumentSchema.allows`), así que
no hay una segunda tabla de qué admite cada elemento, solo la lista cerrada de las que van en reglas
(`StyleRules.ELEMENT_PROPERTIES`). Las que lee el contenido del elemento desde su nodo (`Column align`, los marcadores y
`Table column sizes` de un `Markdown`) no se leen ahí: `StyleRules.apply` las devuelve, con su documento (`PropertyNode`), y
`BuildContext.property` da la del elemento o, si no la declara, la de su regla. Lo que un `Markdown` genera a partir de una
de un documento de estilos aparte (el `Marker` de una lista, las `Column sizes` de una tabla) lleva la línea de ese
documento (`Translation.sources`, en `BuildContext.sources`).

Las reglas anidadas ([Styles](../common/styles.md#reglas-anidadas)) necesitan los ancestros del elemento en el documento:
`BuildContext` lleva la pila (`Ancestor`: el tipo, los nombres del `Style` y el `Id`; `BuildContext.inside` la apila mientras
se construye el contenido de un elemento, de una `Section` o de las celdas de una `Row`, y `ancestors()` la da, el más
cercano primero). `StyleRules.load` lee las reglas de dentro de cada regla con la de fuera (`Placed.outer()`, y `Outer`, a
qué se aplica la de fuera), y `select` las ordena en cada nivel: las sueltas, y después las anidadas cuya regla de fuera se
aplica a un ancestro, la del más lejano primero (`depth`). `checkStyle` da el `STYLE_WITHOUT_RULE` de un nombre solo
anidado fuera de su contexto, también para los tramos (`runStyle` recibe los ancestros).

Dentro de qué cajas se construye cada contenido (un `Page overlay`, un encabezado, un `Columns`, un `Markdown`, una
`Footnote` o una caja que no se parte) lo dice `BuildContext.Scope`, un `record` que solo cambia mientras se construye el
contenido de una caja (`BuildContext.within`) y que se recupera después. Por qué una caja no se parte entre páginas lo dice
una sola clasificación, `Style.unsplittable()` (el giro, `Overflow: clip`, `Side` o la posición, en ese orden): la usan
`ModelValidator`, para rechazar `Keep together` y `Edges` en ella, y el `Scope`, para nombrarla en
`BREAK_IN_UNBREAKABLE`.

Las piezas:

| Clase | Qué hace |
|---|---|
| `PageSetupReader` | `Page` (con la herencia por grupos de las secciones), `Title`, `Language`, `Subject`, `Author` y `Keywords` |
| `SectionReader` | Las propiedades de una `Section` (`Id`, `Page`, la numeración de páginas y de notas, `Inherit page overlays`, `Begin on`) y sus reglas; la caja, el área de notas y `SECTION_EMPTY` quedan en `BoxTreeBuilder.section` |
| `ValueReader` | Interpreta los valores de cada propiedad y comprueba sus formas |
| `PropertyReader` | Los grupos, las propiedades de `Stack`, `Grid` e `Item` y las propias de los elementos, con tablas `Map<String, Property>` |
| `StyleRules` | Las reglas de `Styles` de los dos orígenes, su prioridad, sus propiedades de elemento y las reglas anidadas |
| `NamedColors` | Los colores con nombre (`Named color`) de los dos orígenes, leídos antes que las reglas y sustituidos por su color al leer cada propiedad (`COLOR_UNKNOWN`, `COLOR_DUPLICATE`) |
| `TableBuilder`, `CellPosition`, `CellSelector` | Las tablas: la colocación de las celdas, su posición para las reglas y la caja de cada `Row` con `Id` o `Box` (`Element.ROW`, en `Box.rows()` de la tabla: la tiene de padre, pero no es su hija) |
| `TableBorders` | Los bordes fusionados de una tabla con `Merge borders`: guarda el borde que declara cada celda (`Box.mergedBorder()`, con `model.Stroke`) y cambia su `Border` por el sitio que deja para las líneas |
| `ListBuilder`, `ListMarkers` | Las listas: la numeración y la caja del marcador |
| `MarkdownBuilder`, `RunMarks` | Traduce un `Markdown` (y el texto con `Markup: markdown`) a nodos STXT; `RunMarks` marca antes sus tramos con estilo |
| `TableOfContentsBuilder` | Las entradas de los índices (de encabezados, de figuras o de tablas), al final de la construcción |
| `HeadingNumbers` | Los números de los encabezados, y la escritura del número delante del título o del pie |
| `Figures` | Las figuras: los dos contadores, si una `Figure` va numerada, dónde va su pie, con qué contador cuenta y su `Alt` |
| `References`, `LinkTargets` | Comprueban las referencias y los enlaces `#id` del árbol entero |
| `Footnotes` | Las notas al pie: el área de cada sección, las llamadas `[^id]` numeradas al leerlas y, al final, que cada llamada tiene su nota y cada nota su llamada, y el número delante del primer texto de cada nota |
| `ModelValidator` | Las reglas que miran la caja ya leída (como los márgenes, los mínimos y máximos, el alto definido o el giro) |
| `Nodes` | El acceso a los nodos y los errores |

Ningún método de `tree` pasa de 65 líneas ni de complejidad 20.

> **Decisión:** las familias de elementos son un enum con métodos por defecto y no una interfaz con una clase por familia:
> son pocas, están cerradas (las da el schema) y comparten casi todo. *Alternativa descartada:* una interfaz con una clase
> por familia, o las familias como métodos privados del builder.

## `Box` y `Style`

`Box` tiene los campos privados, se lee con métodos (como `text()`, `children()` o `formattedRuns()`) y se escribe con
métodos `set...` del package. `Style` se parte en un grupo por cada grupo del documento (`TypographyStyle`, `BoxStyle`, `BreakStyle`)
y por cada tipo de elemento con propiedades propias (`StackStyle`, `GridStyle`, `ItemStyle`, `ListStyle`, `ColumnsStyle`,
`ImageStyle`, `FigureStyle`, `TableOfContentsStyle`). En `Style` quedan las propiedades que definen el elemento y las líneas. Fuera de `tree` se
lee a través del grupo: `style.typography().fontSize()`, `style.stack().direction()`, `style.box().margin()`. Solo se hereda
`TypographyStyle`: `Style.inheriting` copia el grupo del padre. Una propiedad nueva va en el grupo de su sitio en el
documento, con su lectura y su `set...`.

`BoxTreeBuilder.build` cierra el árbol al terminar (`Box.close`, que cierra también el estilo): los hijos pasan a una lista
inmutable, los que están en flujo se calculan una vez y escribir en una caja o un estilo cerrados es un
`IllegalStateException`. Un test de otro package que necesita un estilo hecho a mano usa `StyleEditor` (en el package
`tree` de los tests). La igualdad de `Box` es por identidad: es la clave de la caché del layout.

> **Decisión:** el árbol de cajas se cierra al construirlo y solo `tree` lo escribe: así «el layout no escribe en el árbol»
> deja de ser una convención y un documento maquetado se puede leer desde varios hilos. *Alternativa descartada:* accesores
> sin cierre, o un `StyleBuilder` aparte.

> **Decisión:** los grupos de `Style` son clases que se cierran con la caja y no `record`: el builder escribe las propiedades
> de una en una (los grupos, después las reglas por prioridad y después lo del elemento), y con `record` cada escritura
> rehace un registro de hasta 19 componentes. *Alternativa descartada:* `record` inmutables por grupo.

> **Decisión:** los enums de los valores están en `model`, uno por fichero, porque los usan varios grupos y `model` no
> puede depender de `tree`. Las constantes se llaman como el valor del documento (`TextAlign.JUSTIFY` ↔ `justify`), porque
> se leen con `Enum.valueOf` sobre el texto en mayúsculas y con el guion como guion bajo.

> **Decisión:** `Box.describe()` es la única forma de nombrar una caja en los mensajes (`main (line 12)`), y el volcado
> escribe el `Id`.

## Las secciones y la raíz

`BoxTreeBuilder.Result` da la raíz del documento y la lista de `Section` (`tree/Section`: caja, `PageSetup`, `Page numbering`,
`Page number format`, `First number`, `Inherit page overlays`, `Begin on`, `Footnote numbering`, `Footnote number format` y el área de
notas, `footnotes()`). La **caja de cada sección es hija de la raíz del documento** (`Element.SECTION`, añadida tras los
`Page overlay` del documento para que estos la precedan en el recorrido en preorden) y es la **raíz de su layout**
(`Box.isLayoutRoot()`): los recorridos de todo el árbol (referencias, enlaces, índices, fuentes, imágenes, `Id`, estructura PDF)
parten de la raíz y ven un árbol uniforme donde todo hijo tiene a su padre en `parent()`, mientras que el layout, el aislamiento
de márgenes y el contenedor de referencia de los `Overlay` se detienen en `isLayoutRoot()`. Los `Page overlay` hijos de
`Document` tampoco se maquetan en el flujo: los coloca el `Paginator`.

> **Decisión:** las secciones son hijas de la raíz del documento (`section.parent() == root`) y `Box.isLayoutRoot()` distingue la
> raíz de cada layout, de modo que se mantiene en todo el árbol la invariante «el padre de mi hijo soy yo» sin que el layout ni
> los `Overlay` salgan de su sección. *Alternativa descartada:* colgar las secciones de `root.children()` con `parent == null`,
> que rompía la invariante en el primer nivel bajo `Document` y obligaba a tratar `SECTION` aparte al subir por `parent()`.

## Texto y tramos

`Box.text()` es siempre el texto entero y `Box.formattedRuns()` (`null` sin formato) lo parte en `TextRun` con un
`TextFormat`. `Box.runs()` da un único tramo si no hay formato. Los tests de layout ponen tramos en el árbol antes de
cerrarlo con `TextRuns`.

Un **tramo con estilo** (`[x]{nombre}`, [Formato del texto](../common/text-formatting.md#tramos-con-estilo)) sale de
`MarkdownBuilder` con su `TextFormat.style()` a medias, un `RunStyle` con solo el nombre (`RunStyle.named`), porque
`MarkdownBuilder` no conoce las reglas. `ElementReader.runStyles` se lo completa al construir la caja, antes que el
`Code font` del código: `StyleRules.runStyle` lee las reglas `For style` de ese nombre sobre un `Style` que no hereda nada
(así lo declarado en él es lo que declaran las reglas, cada propiedad con su línea), rechaza lo que no está en
`StyleRules.RUN_PROPERTIES` (`STYLE_NOT_ALLOWED_IN_RUN`), y el `RunStyle` guarda lo declarado (`null` lo demás). La
familia de `Font` va además en `TextFormat.family`, como la del código. La caja guarda el `Style` de cada nombre
(`Box.runStyle`) para los errores que salen después con la línea de la regla: `FONT_UNKNOWN` en `PdfMetrics` y
`CONTRAST_LOW` en `ContrastCheck`. Fuera de `tree` nadie mira el nombre: `TextFormat.face`, `color`, `letterSpacing`,
`textCase` y `smallCaps` dan el valor del tramo o, si su estilo no lo declara, el de la caja que se les pasa.

> **Decisión:** el texto de la caja sigue siendo una cadena y los tramos son una partición suya: las líneas del documento,
> los mensajes, los marcadores del PDF y `{page}` trabajan sobre el texto entero, y un texto sin formato se mide y se pinta
> igual que antes. *Alternativa descartada:* guardar solo los tramos y derivar el texto.

## Elementos generados

- **Marcador de lista:** `ListBuilder` crea en `Box.marker` la caja del marcador, generada y fuera de `children`
  (`Element.MARKER`, `Box.isMarker()`): texto con la tipografía del ítem y el espacio final incluido (`"3. "`) o, con
  `Marker: image`, una caja de imagen (`Box.isImage()`, con la ruta en `Box.image()` y el `Alt` y el `Height` del
  `Marker image` en su `ListStyle`), con la línea del `Marker image` del ítem o, si no tiene, de la lista.
  `PropertyReader.markerImage` lee el `Marker image` con sus hijos, `ListBuilder.validateList` y `validateItem`
  comprueban que solo va con `Marker: image` (`LIST_IMAGE_NOT_ALLOWED`) y `ListBuilder.marker`, que con `Marker: image` hay
  una imagen (`LIST_IMAGE_MISSING`).
- **Flujo de columnas:** el contenido de un `Columns` va en una caja de bloque generada (`Element.COLUMN_FLOW`), su único hijo.
- **Entradas de índice:** `TableOfContentsBuilder` genera al final una caja `Stack` por encabezado (`Element.TOC_1` a `Element.TOC_6`)
  o, con `Entries: figures` o `tables`, por `Figure` de ese contador (`Element.TOC_1`), con el título o el pie, la guía y el
  número. Un encabezado o una figura sin `Id` recibe un nombre generado (`Box.anchor`), y `Box.key()` da el `Id` o ese
  nombre.
- **Área de notas:** `Footnotes.beginSection` crea en cada sección la caja `Footnote area` (`Element.FOOTNOTES`,
  `Position.FOOTNOTES`, fuera del flujo: `Style.isOutOfFlow()`), hija de la sección después de su contenido, con el
  aspecto por defecto del filete y las reglas `For element: Footnote area` encima. Cada `Footnote` escrita en la sección
  (`buildContent`, `BoxTreeBuilder.footnote`) se construye con la familia `FOOTNOTE` de `ElementReader` y el área como
  padre, del que hereda, y es hija del área y no de la caja donde se escribe. `BlockLayout` no la ve; `PdfStructure` la
  coloca tras el texto que la llama ([El PDF por dentro](pdf.md#árbol-de-estructura)).
- **Bloque de código:** `MarkdownBuilder` marca el nodo `Text` que genera (`Translation.codeBlocks`) y
  `BoxTreeBuilder.element` le da el elemento `Element.CODE_BLOCK`.

Los elementos que una regla `For element` puede nombrar son `Element.selectable()`: los escritos y, de los generados, los
bloques de código, las entradas de índice y el área de notas; `Element.isWritten()` distingue los escritos.

## Imágenes generadas

Un `Barcode` es una imagen que no sale de un fichero: el árbol la genera a partir de su valor al construirse. La familia
`BARCODE` de `ElementReader` comprueba sus propiedades (`ModelValidator.validateBarcode`) y le pide el dibujo a
`Barcodes`, que codifica los datos con ZXing 3.5.4 y devuelve un `Modules` (`model`): la matriz de módulos oscuros y
claros, con la zona de silencio, el tamaño natural de un módulo y el más estrecho que se puede leer
(`Modules.minModule()`, que comprueba `ModuleSizes` tras maquetar). La caja lo guarda en `Box.modules()`; su elemento tiene
`Layout.IMAGE` y el papel `FIGURE`, y `Box.isImage()` es cierto, así que el layout lo dimensiona con el tamaño natural de
los módulos (`Images.natural`) y el pintado lo dibuja sin saber de qué elemento viene (`Canvas.image`, que mira
`Box.modules()`).

Es el **punto de extensión** de las imágenes generadas: `ImageGenerator` (`tree`) recibe el nodo y su estilo ya leído y
devuelve un `Modules`, y `Barcodes` es su primera implementación. Otro generador es otro elemento con su familia en
`ElementReader`, y no cambia nada fuera de `tree`.

> **Decisión:** el dibujo se genera al construir el árbol, en `tree`, que es donde están el nodo y su línea: los errores de
> los datos (`BARCODE_DATA`) salen con los demás errores del modelo, antes de maquetar, y el layout y el pintado reciben
> una matriz ya hecha. ZXing es Java puro, como commonmark-java en `MarkdownBuilder`. *Alternativa descartada:* generarlo en
> `layout` o en `resources`, como las imágenes de fichero, que obliga a pasar el formato y los datos por el árbol para
> usarlos después y a dar la línea del error desde fuera.

> **Decisión:** el contrato es una matriz de módulos, sin una lista de formas ni un sistema de *plugins* (`ServiceLoader`).
> Cubre los códigos de una y dos dimensiones, y el día que haga falta otra cosa se amplía. *Alternativa descartada:* una
> interfaz pública para generadores de fuera del motor, que fija una API antes de tener un segundo caso.

## Markdown

> **Decisión:** el Markdown se traduce a nodos STXT y no a cajas: lo generado se construye como cualquier otro elemento, así
> que las reglas de `Styles`, la validación, el PDF etiquetado y los marcadores no tienen ningún caso especial, y lo generado
> se valida además contra el schema. *Alternativa descartada:* construir las cajas desde el árbol del parser.

commonmark-java descarta las celdas que sobran de una fila de tabla, así que `MarkdownBuilder` las cuenta en el texto
de la fila. Tampoco da la línea de cada celda, así que los errores de una celda señalan la de su fila.

`MarkdownBuilder` parsea el `Content` con commonmark-java 0.30.1 (con las extensiones de tablas, tachado y subrayado) y lo
traduce a nodos STXT (`InlineNode` con la línea de su bloque), que valida con el schema. Los tramos de cada texto van aparte,
por nodo (`Translation.runs`, en `BuildContext.runs`). La familia `MARKDOWN` de `ElementReader` construye esos nodos con `element(...)` como
hijos de la caja del `Markdown`. El texto con `Markup: markdown` pasa por `MarkdownBuilder.inline`, el mismo parser y la
misma traducción en modo `inlineOnly`. Dentro de un `Markdown` no actúa (`BuildContext.Scope.inMarkdown`).

Antes de parsear, `RunMarks.mark` cambia cada tramo con estilo por marcas, no-caracteres de Unicode que commonmark-java
toma por letras: `RunMarks.OPEN`, los dígitos del número del nombre y `RunMarks.END` en lugar del `[`, y `RunMarks.CLOSE`
en lugar de `]{nombre}`.
Recorre el texto como el Markdown en línea (los escapes, el código entre comillas invertidas, el destino de un enlace, las
imágenes y las líneas en blanco) para dejar fuera lo que no es texto. `Translator.text` las encuentra en los nodos `Text` y
da el estilo a todo lo que va entre ellas, también a las marcas, los enlaces y el código, y el código, en línea o en bloque, recupera
su texto con `RunMarks.unmark`.

> **Decisión:** las marcas antes del parser y no un `LinkProcessor` de commonmark-java, que trata el tramo como un enlace:
> no deja que un enlace lo contenga (`disallowPreviousLinks`) ni lo procesa si su texto tiene corchetes, y
> `[[x]{a}](destino)` y `[[x](destino)]{a}` saldrían como texto sin ningún aviso. *Alternativa descartada:* un
> `LinkProcessor`, o reconocer los corchetes en los nodos `Text` ya parseados, donde un `\[` escapado no se distingue de
> uno sin escapar.
