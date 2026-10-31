# Errors

The engine is **strict**: a document that asks for something that has no effect, or for two things that contradict each
other, is an error and not a case resolved with a default rule. This way the result is predictable: what is read in the
document is what is obtained.

- **Each error has a stable code** and a message that says what fails and how to fix it. Messages can improve, but codes
  do not change.
- **The line is that of the culprit node:** that of the property (and not that of its box or its group), that of the
  character inside a `Content >>` block or, if the family does not exist, that of the `Font` declared in an ancestor. If
  the problem is the combination of two properties, the message names the line of the other one. Messages with a line
  start with `línea N:`.
- **It stops at the first error**: accumulating them would give cascading errors (a badly defined box causes others in its
  children).

## What has no effect

What an element declares and cannot have an effect because of how it is written is an error: `Line distribution` or a two-value `Gap`
without `Wrap`, `Rotate origin` without `Rotate`, `Separator color` without `Separator`, an `auto` margin that does not center, or a `Break` inside
an `Overlay`. These are not errors, on purpose:

- **Writing the default value:** `Span: 1 1`, `Isolate margins: false`, `Start: 1`. It contradicts nothing.
- **What is inherited:** a `Typography` property applies to the descendants, so it is not checked whether the box that
  declares it uses it (`Numbered` in a `Block`, `Small caps` with `Case: uppercase`).
- **The appearance that depends on another property:** `Border color` without `Border` and `Corner radius` without a
  background, border or image. They usually come from a style rule shared by boxes with and without a border.
- **A `Break` that comes from a rule** to an element inside a box that is not split: a rule by type
  (`For element: Paragraph`) would give an error in each paragraph of a footer ([Breaks](breaks.md#forced-breaks)).
- **What can come later:** a rule that does not apply to any element, and `Inherit page overlays: false` without a `Page overlay` in the
  document. A template can add them.
- **What depends on layout:** a minimum and a maximum in different units that clash (the minimum wins,
  [The box](box.md#minimums-and-maximums)) and `Balance: false` in a `Columns` with no height to fill.
- **The properties of a `Markdown` that its content does not use** (`Bullet markers` without lists): the content changes
  often ([Markdown](../components/markdown.md)).

## Validation levels

1. **STXT syntax and schema** ([Structure](structure.md#schema)): which children each element allows, how many, empty
   values, closed values, integers and booleans. This is where a property outside its element is detected (`Offset` in a
   `Block`, `Column sizes` in a `Stack`), with the STXT codes (`CHILD_NOT_DECLARED`, `TOO_MANY_CHILDREN`,
   `TOO_FEW_CHILDREN` or `INVALID_VALUE`, among others).
2. **Model:** the rules that the schema cannot express, with the codes below.
3. **Resources** ([Resources](resources.md)): fonts and images ([Image](../components/image.md)).
4. **Overflows** ([Overflow](overflow.md)): on the laid-out result.
5. **Contrast** ([The PDF](pdf.md#contrast)): on what is painted, when converting. The caller can turn it off.

## Codes

| Code | Rule | Message (example) |
|---|---|---|
| `DOCUMENT_ROOT` | A single root node, `Document`, with the namespace `dev.stxt.pdf` (the schema accepts any element as root); in a separate styles document, `Styles` | `El nodo raíz debe ser Document (dev.stxt.pdf):` |
| `PAGE_SIZE` | The size of `Page`, with `Paper` (and `Orientation`) or with `Width` and `Height`, never both; `Width` and `Height`, positive points; `Margin`, only points, and it leaves a page area; a `Section` that only rotates (`Orientation`) a document with paper ([Document](../components/document.md#page)) | `Los márgenes de la página no dejan área útil` |
| `SECTION_EMPTY` | A `Section` has content: some element that is not a `Page overlay` (line of the section) | `La sección no tiene contenido: sin elementos (los Page overlay no cuentan...) no tiene ninguna página` |
| `PAGE_NUMBER_UNAVAILABLE` | A text with `{page}` or `{pages}` does not appear on a page without a number (`Page numbering: none`); line of the text | `El texto de la caja pie tiene {page} o {pages} y sale en una página sin número...` |
| `VALUE_SYNTAX` | Syntax of the values ([Values](values.md)), colors (`#RGB`, `#RGBA`, `#RRGGBB` or `#RRGGBBAA`), malformed `linear(...)`, more than four colors in `Border color` or four values in `Corner radius`, `Aspect ratio`, `For column` that is not a number | `Valor no válido: 12,5 (el decimal se escribe con punto...)` |
| `VALUE_NOT_ALLOWED` | Forms and signs that each property allows ([Values](values.md#value-types)); `Gap` with 1 or 2 values (in `Columns`, 1); `Size` > 0; `Count` ≥ 1; `Min column width` and `Separator` > 0; `Letter spacing` in points; `Tab size` ≥ 1; `Title` and `Alt` not empty and `Language` a BCP 47 tag; `Bullet markers` with unnumbered markers and `Number markers` with numberings; `Levels` with two levels from 1 to 6, the first not greater than the last; `Rotate` between −360 and 360, without 0 or whole turns; `Rotate origin` with one or two keywords, at most one per axis; `For column` from lowest to highest; `Number label` with `{number}` once | `Width no admite *: admite un número...` |
| `FIT_TRACK` | `fit` is not a grid track | `fit no es una pista de grid: la que se ajusta al contenido es auto` |
| `DUPLICATE_ID` | `Id` values are unique | `Id repetido: a (ya se usa en la línea 6)` |
| `SINGLE_ELEMENT` | `Item` and `Area` contain exactly one element | `Un Area contiene exactamente un elemento, y este tiene Block (línea 8) y Text (línea 10)` |
| `TEXT_CONTENT` | A `Text` has its text in the value or in `Content`, one of the two; a `Markdown` has content, and its headings and paragraphs, text | `Text tiene el texto en el valor y en Content (línea 9): va en uno solo` |
| `TEXT_WITHOUT_FONT` | Text, and a `Table of contents`, need `Font` in `Typography`, their own or inherited | `El texto necesita Font: se declara en el Typography del Text, de un antecesor o de Styles (For element: Document)` |
| `TEXT_WITHOUT_FONT_SIZE` | Text needs `Size` in `Typography`, its own or inherited | `El texto necesita Size: se declara en el Typography del Text, de un antecesor o de Styles (For element: Document)` |
| `STYLE_UNKNOWN_ELEMENT` | `For element` names a box element, a generated element or a position in a table ([Styles](styles.md)) | `For element: 'Item' no es un elemento de caja (Block, Stack...)` |
| `STYLE_DUPLICATE` | Two rules of the same source do not have the same selector | `Regla repetida: For element: block (ya está en la línea 6)` |
| `STYLE_NOT_ALLOWED` | A rule that reaches a `Section` only gives `Typography`; a rule (also a position rule, a column rule or a rule of the style of a `Row`) does not give `Break` to an `Overlay`, a `Page overlay`, a heading, a `Text` of a heading or a `Cell`; `For element: Document` only allows `Typography`, and in it not `Language` | `La regla For style: page aplica Break a un Overlay (línea 11)...` |
| `HEADING_LEVEL` | The first heading is `Heading 1` and each one goes up at most one level ([Headings](../components/heading.md#hierarchy)) | `Salto de nivel: Heading 3 después de Heading 1; como mucho, Heading 2` |
| `HEADING_IN_PAGE_OVERLAY` | A heading does not go inside a `Page overlay` | `Un Heading 1 no puede estar dentro de un Page overlay...: ahí va un Text` |
| `BREAK_IN_HEADING` | A `Text` of a heading does not declare `Break` | `Un Text de un encabezado no admite Break: el motor ya evita cortar...` |
| `TEXT_BLANK_LINE` | A `Paragraph` has no blank lines in its `Content`, nor does a `Text` that is not preformatted (line of the blank line) ([Text](../components/text.md)) | `Un Paragraph es un solo párrafo: sobra la línea en blanco...` |
| `LIST_ITEM_EMPTY` | A `List item` contains at least one element ([List](../components/list.md)) | `Un List item necesita al menos un elemento: un Paragraph, un Text, un Block...` |
| `LIST_NUMBER_NOT_ALLOWED` | `Start` and `Number` only in numbered lists | `Start no tiene efecto con Marker: bullet, que no numera los ítems` |
| `LIST_NUMBER_RANGE` | The number fits in the numbering: `decimal` from 0, letters from 1, Roman numerals from 1 to 3999 (line of the `Start`, of the `Number` or of the item) | `El número 4000 no se puede escribir con Marker: upper-roman, que numera de 1 a 3999` |
| `LIST_ITEM_WITHOUT_LINE` | An item with a marker starts with a text to align it with (line of the box that prevents it) | `El marcador del List item de la línea 10 se alinea con su primera línea, y el ítem empieza con un Stack...` |
| `TABLE_ORDER` | `Header rows`, `Body` and `Footer rows` go in that order ([Table](../components/table.md)) | `Body va después de Footer rows: los grupos de una Table van en el orden Header rows, Body, Footer rows` |
| `TABLE_ROW_CELLS` | Each row fills exactly the columns of `Column sizes`, counting the `Span` of the rows above (line of the row, or of the extra cell) | `La fila deja libre la columna 2 de 2 (Columns): falta una Cell, vacía si hace falta, o un Span` |
| `TABLE_SPAN` | A `Span` does not go outside the table, does not go from one group to another and does not overlap another cell | `La celda ocupa de la columna 1 a la 3, y la tabla tiene 2 (Columns)` |
| `TABLE_COLUMN_ALIGN` | `Column align` gives one alignment per column of `Column sizes` (line of the `Column align`) | `Column align da 2 alineaciones y la tabla tiene 3 columnas (Columns): va una por columna` |
| `ROW_HEADER_IN_HEADER_ROWS` | `Row header: true` is not declared in a cell of `Header rows`, whose cells are already column headers (line of the `Row header`) | `Row header no tiene efecto en una celda de Header rows: las celdas de la cabecera ya son cabeceras de columna` |
| `CELL_CONTENT` | A `Cell` has text in the value or elements, not both | `Una Cell tiene texto en el valor (línea 12) o elementos y no las dos cosas: el texto va en un Paragraph` |
| `TABLE_ROW_BREAK` | The rows of `Header rows` and `Footer rows`, which are repeated on each page, do not declare `Break` | `Las filas de Header rows se repiten en cada página de la tabla y no se cortan: sobra el Break` |
| `COLUMNS_WITHOUT_COUNT` | A `Columns` declares `Count`, `Min column width` or both ([Columns](../components/columns.md)) | `Un Columns necesita Count, Min column width o los dos...` |
| `SEPARATOR_WIDTH` | `Separator` is not wider than `Gap` | `Separator es más ancho que Gap, que sin declarar es 0: la línea se pintaría sobre las columnas...` |
| `SINGLE_COLUMN` | A `Columns` with `Count: 1` does not declare `Gap` or `Separator`, which separate columns ([Columns](../components/columns.md)) | `Gap no tiene efecto con Count: 1, porque no hay dos columnas que separar: sobra` |
| `SEPARATOR_COLOR_WITHOUT_SEPARATOR` | `Separator color` goes with `Separator` | `Separator color no tiene efecto sin Separator, el grosor de la línea entre columnas...` |
| `COLUMN_BREAK_OUTSIDE_COLUMNS` | `Before` and `After: column` only inside a `Columns` (line of the `Before` or the `After`, also from a rule) | `After: column solo tiene efecto dentro de un Columns: para cortar la página se usa page` |
| `KEEP_CONFLICT` | `Keep with next: true` does not go with `After: page` or `column`, nor `Keep with previous: true` with `Before: page` or `column` (line of the `Keep`, also from a rule) ([Breaks](breaks.md#the-break-group)) | `Keep with next mantiene la caja con la siguiente y After: page la separa de ella: sobra uno de los dos` |
| `OVERLAY_IN_COLUMNS` | An `Overlay` or a `Page overlay` does not go inside a `Columns` | `Un Page overlay no puede estar dentro de un Columns...` |
| `MARKDOWN_UNSUPPORTED` | Markdown only uses the subset of [Markdown](../components/markdown.md#the-subset), and a table row has no more cells than the header; a text with `Markup: markdown` has no images, HTML or forced line breaks within the line ([Text formatting](text-formatting.md#inline-formatting-markup)) (line of the block or of the paragraph) | `Markdown no admite imágenes dentro de un texto (una imagen va sola en su párrafo): solo admite encabezados, párrafos, listas, tablas, bloques de código y...` |
| `MARKDOWN_PROPERTY_MISSING` | A `Markdown` with bulleted lists declares `Bullet markers`; with numbered lists, `Number markers`; with tables, `Table columns` (line of the list or the table) | `La lista necesita Bullet markers en su Markdown (línea 8): el marcador de cada nivel, como Bullet markers: bullet dash` |
| `CODE_WITHOUT_FONT` | Code, inline or in a block, needs `Code font` in `Typography`, its own or inherited ([Text formatting](text-formatting.md#inline-code)); line of the text, of the paragraph or of the block | `El código necesita Code font: se declara en el Typography del texto, de un antecesor o de Styles (For element: Document)...` |
| `LINK_SCHEME` | The target of a link is an `https://`, `http://` (with a host) or `mailto:` (with a recipient) URL or `#id`; it is not empty or malformed ([Links](links.md)) | `Link: 'javascript:alert(1)' no tiene un esquema admitido: solo https://, http://, mailto: y #id` |
| `LINK_UNKNOWN` | An `#id` link leads to a box that exists and that is not in a `Page overlay` or inside one | `El enlace #nada lleva a un Id que no existe: ninguna caja tiene el Id nada` |
| `IMAGE_UNKNOWN` | An `Image` has a path, and it is that of a file in `images/` (line of the `Image`) | `No existe el recurso logos/acme.png en .../resources/images` |
| `IMAGE_FORMAT` | An `Image` is a PNG, a JPEG, an SVG of the supported subset or an unencrypted PDF, which can be read, and the page it draws from a PDF has no visible annotations or fonts that are not embedded or lack `ToUnicode`, unless the caller removes those checks (line of the `Image`) ([Image](../components/image.md#svg)) | `La imagen dibujo.gif no es PNG, JPEG, SVG ni PDF (es GIF): hay que convertirla a uno de los cuatro` |
| `IMAGE_PAGE` | `Pdf page` goes in the `Image` of a PDF and is a page that the PDF has (line of `Pdf page`) ([Image](../components/image.md#pdf)) | `El PDF planos.pdf tiene 3 páginas y no tiene la 4` |
| `BREAK_IN_IMAGE` | An `Image` does not declare `Keep together` or `Edges`, not in a rule either | `Keep together no tiene efecto en una Image, que no se parte entre páginas: sobra` |
| `TABLE_OF_CONTENTS_EMPTY` | A `Table of contents` has some heading in the levels of `Levels` (line of the `Table of contents`) | `El índice no tiene ningún encabezado de los niveles 2 a 3 (Levels): no tendría entradas` |
| `REFERENCE_UNKNOWN` | A reference has an `Id`, and it is that of a box of the document (line of the reference, [References](references.md)) | `{page of: nada}: ninguna caja tiene el Id nada` |
| `REFERENCE_NOT_PLACEABLE` | The page of a box, which is not in a `Page overlay`, not inside one and not on a page without a number | `{page of: pie}: pie está en un Page overlay, que se repite en todas las páginas` |
| `REFERENCE_NOT_NUMBERED` | The number of a box that has one: a `List item` of a numbered list or a heading with `Numbered: true` | `{number of: nota}: nota no tiene número (solo lo tienen los ítems de una lista numerada y los encabezados con Numbered: true)` |
| `REFERENCE_UNSTABLE` | The page of the referred boxes stabilizes within 10 pagination passes | `La página de x no se estabiliza: tras 10 pasadas sigue cambiando...` |
| `STYLE_WITHOUT_RULE` | Each `Style` name has its `For style` rule | `El estilo aside no tiene regla: falta 'For style: aside' en Styles` |
| `OFFSET_CONFLICT` | A `Frame` is not shifted from the top and the bottom at the same time, nor from the left and the right | `Un Frame no puede desplazarse a la vez desde arriba y desde abajo...` |
| `OVERCONSTRAINED` | An `Overlay` or a `Page overlay` with start, size and end needs some `auto` margin | `Izquierda, Width y derecha declarados fijan la posición dos veces...` |
| `MARGIN_AUTO_WITHOUT_EFFECT` | `Margin: auto` only where it centers ([The box](box.md#auto-margins)) | `Margin auto no centra una caja que ocupa todo el ancho...` |
| `ISOLATE_MARGINS_IN_ITEM` | `Isolate margins: true` does not go in an item of a `Stack` or `Grid`, which already stops margin merging ([Block](../components/block.md#boxes-that-isolate-margins)) | `Isolate margins no tiene efecto en un ítem de Stack o Grid, que ya corta la fusión de márgenes con sus hijos: sobra` |
| `START_WITHOUT_RESTART` | `Start` in a `Section` only with `Page numbering: restart` ([Document](../components/document.md#page-numbering)) | `Start solo tiene efecto con Page numbering: restart...` |
| `ROW_BOX` | The `Box` of a `Row` only gives its appearance: `Background`, `Border`, `Border color` and `Corner radius` ([Table](../components/table.md#rows)) | `Padding no tiene efecto en una Row, que no es una caja del layout...` |
| `PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS` | `Page number format` does not go in a `Section` with `Page numbering: none` | `Page number format no tiene efecto con Page numbering: none...` |
| `BEGIN_ON_FIRST_SECTION` | `Begin on: odd` does not go in the first `Section`, which already starts on page 1 ([Document](../components/document.md#beginning-on-an-odd-page)) | `Begin on: odd no tiene efecto en la primera sección...` |
| `LINE_DISTRIBUTION_WITHOUT_WRAP` | `Line distribution` only with `Wrap: true` | `Line distribution solo tiene efecto con Wrap: true...` |
| `GAP_WITHOUT_WRAP` | A two-value `Gap` in a `Stack`, only with `Wrap: true` ([Stack](../components/stack.md#properties-of-the-stack)) | `Gap tiene dos valores, pero sin Wrap hay una sola línea y el primero, el hueco entre filas, no tiene efecto: va un solo valor...` |
| `GROW_WITHOUT_EFFECT` | `Grow` in a vertical `Stack` needs a definite height in the container | `Grow no tiene efecto: el Stack vertical del padre no tiene un alto definido...` |
| `ITEM_MAIN_AXIS` | An `Item` is only aligned on the cross axis of its `Stack`: `Vertical` in a horizontal one and `Horizontal` in a vertical one ([Stack](../components/stack.md#item)) | `Horizontal no tiene efecto en un ítem de un Stack con Direction: horizontal, porque es su eje principal...` |
| `ASPECT_RATIO_CONFLICT` | `Aspect ratio` is not combined with declared `Width` and `Height` | `Aspect ratio no tiene efecto si Width y Height están declarados` |
| `MIN_MAX_CONFLICT` | Comparable minimum, maximum and size do not contradict each other ([The box](box.md#minimums-and-maximums)) | `Min width es mayor que Max width (línea 7)` |
| `INDEFINITE_PERCENTAGE` | A vertical percentage needs a definite height ([Values](values.md#definite-height)) | `Height usa un porcentaje de un alto que no es definido...` |
| `ROTATE_ORIGIN_WITHOUT_ROTATE` | `Rotate origin` only with `Rotate` | `Rotate origin no tiene efecto sin Rotate: falta el giro o sobra Rotate origin` |
| `BREAK_IN_ROTATED` | A rotated `Frame` or a rotated in-flow box does not declare `Keep together` or `Edges`, not in a rule either ([Layers and rotation](layers-and-rotation.md#rotation-rotate)) | `Keep together no tiene efecto en un Frame girado, que no se parte entre páginas: sobra` |
| `BREAK_IN_UNBREAKABLE` | An element inside an `Overlay`, a `Page overlay`, a rotated `Frame` or a rotated in-flow box, which are not split, does not write `Break` (nor does the `Row` of a cell); what comes from a rule is not checked ([Breaks](breaks.md#forced-breaks)) | `Before no tiene efecto dentro de un Overlay, que no se parte entre páginas: sobra` |
| `ISOLATE_MARGINS_IN_ROTATED` | `Isolate margins: true` does not go in a rotated in-flow box, which already stops margin merging ([Layers and rotation](layers-and-rotation.md#rotation-in-flow-block-text-paragraph-and-image)) | `Isolate margins no tiene efecto en una caja girada, que ya corta la fusión de márgenes con sus hijos: sobra` |
| `ROTATED_ITEM` | A rotated `Block`, `Text`, `Paragraph` or `Image` is not an item of a `Stack` or `Grid` ([Layers and rotation](layers-and-rotation.md#rotation-in-flow-block-text-paragraph-and-image)) | `Un ítem de Stack no puede girar: ocupa el sitio que le da su contenedor. Lo que gira es una caja dentro del ítem...` |
| `GRID_PLACEMENT` | Rectangular `Areas` with rows of equal length; a valid `Area` value with existing areas; an item with a fixed row fits in it ([Grid](../components/grid.md#placement)) | `Área desconocida en Area: foot` |
| `FONT_UNKNOWN` | `Font` exists in the fonts folder (line of the `Font`); also `Code font` (line of the `Code font`) | `Fuente desconocida: Arial. Familias en .../resources/fonts: IBM Plex Mono, IBM Plex Sans, IBM Plex Serif` |
| `FONT_VARIANT_MISSING` | The family has the variant of `Bold` and `Italic`, and that of each formatted run (line of the text, or that of the run) | `La fuente IBM Plex Sans no tiene la variante bold italic... (Font en la línea 6, ...)` |
| `GLYPH_MISSING` | The font has all the characters of the text (line of the character) | `La fuente IBM Plex Sans normal no tiene el carácter '漢' (U+6F22)` |
| `CONTRAST_LOW` | The text has a contrast of 4.5:1 with what is under it, or of 3:1 if it is large ([The PDF](pdf.md#contrast)); line of the `Color` of the text, or of its box if it has none | `El texto de la caja de la línea 12 tiene un contraste de 2,84:1 con su fondo (#999999 sobre #FFFFFF) y necesita 4,5:1...` |
| `TEXT_OVERFLOW` | No word is wider than its line, nor is a line of a text with `Preformatted: true` (line of the word) | `La palabra 'supercalifragilística' no cabe en la línea: la línea mide 138.69 y la caja deja 100...` |
| `BOX_OVERFLOW` | Boxes fit in their container, content in its height (also in the columns of a `Columns`), and `Overlay` elements, `Page overlay` elements and what is rotated, on the page | `El contenido de la caja de la línea 7 no cabe en su alto: su texto llega a 24 y la caja deja 20` |
| `LIST_MARKER_OVERFLOW` | The marker of an item, with its space, fits in the left `Padding` of the list plus the left margin of the item (line of the item) | `El marcador '1.' del List item de la línea 12 mide 11.45 con su espacio, y la sangría deja 5...` |
| `TABLE_REPEAT_OVERFLOW` | The header and the footer of a table leave room for the body on an empty page (line of the `Table`) | `La cabecera y el pie de la tabla de la línea 8 miden 900 y no dejan sitio al cuerpo en una página vacía...` |
| `PAGE_OVERFLOW` | What cannot be split fits on a page | `La caja de la línea 9 no cabe en la página y no se puede partir: mide 900 de alto...` |
| `PAGE_LIMIT` | The document does not take up more pages than the limit, 2000 by default ([Limits](limits.md); it belongs to no line) | `El documento tiene más de 2000 páginas, el máximo que admite el motor (Limits.maxPages lo cambia)` |
| `IMAGE_TOO_LARGE` | A PNG or a JPEG does not have more pixels than the limit, 25 million by default (line of the `Image`) | `La imagen test/limit-huge.png tiene 6000 × 6000 píxeles (36,0 Mpx) y el máximo es 25,0 Mpx: hay que reducir su resolución...` |
| `SVG_TOO_COMPLEX` | An SVG does not draw more elements (counting what each `<use>` repeats) or nest more levels than the limit, 20,000 and 64 by default (line of the `Image`) | `El SVG test/limit-depth.svg anida 72 niveles y el máximo es 64: hay que aplanarlo...` |
| `PDF_TOO_LARGE` | A PDF does not take up more bytes than the limit, 20 MB by default (line of the `Image`) | `El PDF planos.pdf ocupa 31,4 MB y el máximo es 20,0 MB: hace falta un PDF más pequeño...` |

[`test/errors/`](../../../../test/errors) has an example document for each code, with the line it must give
([Testing](../implementation/testing.md#error-catalog-errorcatalogtest)).

## Error types

Each error says, besides its code and its line, **who is to blame**, which is what a service needs in order to respond:

| Type | What it is | A service responds with |
|---|---|---|
| `INVALID_DOCUMENT` | The document or its styles are not valid: syntax, schema, a model rule, an overflow, a resource that the document names and does not exist | a client error (4xx), with the line |
| `RESOURCE` | The resources folder is not usable (it does not exist, a broken font or image) or the document file cannot be read | a failure of its configuration (5xx) |
| `LIMIT` | The document is valid but asks for more than is allowed ([Limits](limits.md)): more pages, an image with too many pixels, an SVG with too many elements or levels, a PDF that is too large, or an STXT text that is too large or too deeply nested | a client error for excess (413 or 422) |
| `INTERNAL` | A failure of the engine and not of the document or the environment | a server failure (500), which should be reported |

The code is that of the table above, the STXT one for syntax and schema errors (`CHILD_NOT_DECLARED`), `RESOURCE`,
`IMAGE_FORMAT` or `INTERNAL`. The line is `0` if the error does not belong to a line (a resource, a limit or an internal
failure). If it belongs to the [separate styles document](styles.md#separate-styles-document), the message says so
(`línea 8 de los estilos: ...`). How they are seen from Java is in [Using from Java](../implementation/api.md#errors).

> **Decision:** a failure the engine should have avoided (a page that does not advance, a link without a target) is
> `INTERNAL`: it is a defect of the engine and not of the document.
