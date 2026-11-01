# Errores

El motor es **estricto**: un documento que pide algo que no tiene efecto, o dos cosas que se contradicen, es un error y no
un caso que se resuelve con una regla por defecto. Así el resultado es predecible: lo que se lee en el documento es lo que
se obtiene.

- **Cada error tiene un código estable** y un mensaje que dice qué falla y cómo arreglarlo. Los mensajes pueden mejorar,
  pero los códigos no cambian.
- **La línea es la del nodo culpable:** la de la propiedad (y no la de su caja ni la de su grupo), la del carácter
  dentro de un bloque `Content >>` o, si la familia no existe, la de la `Font` declarada en una antecesora. Si el problema es la
  combinación de dos propiedades, el mensaje nombra la línea de la otra. Los mensajes con línea empiezan por `line N:`.
- **Se para en el primer error**: acumularlos daría errores en cascada (una caja mal definida provoca otros en sus
  hijos).

## Lo que no tiene efecto

Lo que un elemento declara y no puede tener efecto por cómo está escrito es un error: `Line distribution` o un `Gap` de dos valores
sin `Wrap`, `Rotate origin` sin `Rotate`, `Separator color` sin `Separator`, un margen `auto` que no centra o un `Break` dentro de
un `Overlay`. No lo es, a propósito:

- **Escribir el valor por defecto:** `Span: 1 1`, `Isolate margins: false`, `Start: 1`. No contradice nada.
- **Lo que se hereda:** una propiedad de `Typography` vale para los descendientes, así que no se comprueba si la caja que
  la declara la usa (`Numbered` en un `Block`, `Small caps` con `Case: uppercase`).
- **El aspecto que depende de otra propiedad:** `Border color` sin `Border` y `Corner radius` sin fondo, borde ni imagen. Suelen
  venir de una regla de estilo que comparten cajas con y sin borde.
- **Un `Break` que llega de una regla** a un elemento dentro de una caja que no se parte: una regla por tipo
  (`For element: Paragraph`) daría un error en cada párrafo de un pie ([Cortes](breaks.md#cortes-forzados)).
- **Lo que puede llegar después:** una regla que no se aplica a ningún elemento e `Inherit page overlays: false` sin `Page overlay` en el
  documento. Una plantilla puede añadirlos.
- **Lo que depende de la maquetación:** un mínimo y un máximo en unidades distintas que chocan (gana el mínimo,
  [La caja](box.md#mínimos-y-máximos)) y `Balance: false` en un `Columns` sin alto que llenar.
- **Las propiedades de un `Markdown` que su contenido no usa** (`Bullet markers` sin listas): el contenido cambia a menudo
  ([Markdown](../components/markdown.md)).

## Niveles de validación

1. **Sintaxis STXT y schema** ([Estructura](structure.md#schema)): qué hijos admite cada elemento, cuántos, valores
   vacíos, valores cerrados, enteros y booleanos. Es donde se detecta una propiedad fuera de su elemento (`Offset` en un
   `Block`, `Column sizes` en un `Stack`), con los códigos de STXT (`CHILD_NOT_DECLARED`, `TOO_MANY_CHILDREN`,
   `TOO_FEW_CHILDREN` o `INVALID_VALUE`, entre otros).
2. **Modelo:** las reglas que el schema no puede expresar, con los códigos de abajo.
3. **Recursos** ([Recursos](resources.md)): las fuentes y las imágenes ([Image](../components/image.md)).
4. **Desbordamientos** ([Desbordamiento](overflow.md)): sobre el resultado maquetado.
5. **Contraste** ([El PDF](pdf.md#contraste)): sobre lo que se pinta, al convertir. Quien convierte lo puede desactivar.

## Códigos

| Código | Regla | Mensaje (ejemplo) |
|---|---|---|
| `DOCUMENT_ROOT` | Un único nodo raíz, `Document`, con el namespace `dev.stxt.pdf` (el schema acepta cualquier elemento como raíz); en un documento de estilos aparte, `Styles` | `The root node must be Document (dev.stxt.pdf):` |
| `PAGE_SIZE` | El tamaño de `Page`, con `Paper` (y `Orientation`) o con `Width` y `Height`, nunca las dos cosas; `Width` y `Height`, medidas positivas; `Margin`, solo medidas, y deja área útil; una `Section` que solo gira (`Orientation`) un documento con papel ([Document](../components/document.md#page)) | `The margins of the page leave no page area` |
| `SECTION_EMPTY` | Una `Section` tiene contenido: algún elemento que no sea un `Page overlay` (línea de la sección) | `The section has no content: without elements (Page overlays do not count...) it has no page` |
| `PAGE_NUMBER_UNAVAILABLE` | Un texto con `{page}` o `{pages}` no sale en una página sin número (`Page numbering: none`); línea del texto | `The text of box pie has {page} or {pages} and appears on a page without a number...` |
| `VALUE_SYNTAX` | Sintaxis de los valores ([Valores](values.md)), colores (`#RGB`, `#RGBA`, `#RRGGBB`, `#RRGGBBAA` o un nombre), `linear(...)` mal escrito, un `Named color` que no es un nombre y un color hexadecimal, más de cuatro colores en `Border color` o de cuatro valores en `Corner radius`, `Aspect ratio`, `For column` que no es un número, una unidad que no es `mm` o separada del número, una unidad en `Grow` o `Shrink` | `Invalid value: 12,5 (the decimal separator is a point...)` |
| `VALUE_NOT_ALLOWED` | Formas y signos que admite cada propiedad ([Valores](values.md#tipos-de-valor)); `Gap` con 1 o 2 valores (en `Columns`, 1); `Size` > 0; `Count` ≥ 1; `Min column width` y `Separator` > 0; `Letter spacing` sin porcentajes; `Tab size` ≥ 1; `Weight` de 100 a 900, de 100 en 100; `Title` y `Alt` no vacíos y `Language` una etiqueta BCP 47; `Bullet markers` con marcadores sin numerar y `Number markers` con numeraciones; `Levels` con dos niveles de 1 a 6, el primero no mayor que el último; `Rotate` entre −360 y 360, sin 0 ni las vueltas enteras; `Rotate origin` con una o dos palabras clave, como mucho una por eje; `For column` de menor a mayor; `Number label` con `{number}` una vez; `linear` no es el nombre de un `Named color` | `Width does not allow *: it allows a length or a percentage...` |
| `FIT_TRACK` | `fit` no es una pista de grid | `fit is not a grid track: the one that fits the content is auto` |
| `DUPLICATE_ID` | Los `Id` son únicos | `Duplicate Id: a (already used on line 6)` |
| `SINGLE_ELEMENT` | `Item` y `Area` contienen exactamente un elemento | `An Area contains exactly one element, and this one has 2` |
| `TEXT_CONTENT` | Un `Text` o un `Paragraph` tiene su texto en el valor o en `Content`, uno de los dos; un `Markdown` tiene contenido, y sus encabezados y párrafos, texto | `Text has the text in the value and in Content (line 9): it goes in only one` |
| `TEXT_WITHOUT_FONT` | El texto, y un `Table of contents`, necesitan `Font` en `Typography`, propia o heredada | `The text needs Font: it is declared in the Typography of the Text, of an ancestor or of Styles (For element: Document)` |
| `TEXT_WITHOUT_FONT_SIZE` | El texto necesita `Size` en `Typography`, propio o heredado | `The text needs Size: it is declared in the Typography of the Text, of an ancestor or of Styles (For element: Document)` |
| `STYLE_UNKNOWN_ELEMENT` | `For element` nombra un elemento de caja, un elemento generado o una posición en una tabla ([Styles](styles.md)) | `For element: 'Item' is not a box element or a position in a table (Document, Section, Block, Stack...)` |
| `STYLE_DUPLICATE` | Dos reglas del mismo origen no tienen el mismo selector | `Repeated rule: For element: block (already on line 6)` |
| `STYLE_NOT_ALLOWED` | Una regla que llega a una `Section` solo da `Typography`; una regla (también de posición, de columna o del estilo de una `Row`) no da `Break` a un `Overlay`, un `Page overlay`, un encabezado, un `Text` de un encabezado o una `Cell`; `For element: Document` solo admite `Typography`, y en ella no `Language` | `The rule For style: page applies Break to the Overlay (line 11)...` |
| `HEADING_LEVEL` | El primer encabezado es `Heading 1` y cada uno sube como mucho un nivel ([Encabezados](../components/heading.md#jerarquía)) | `Level skip: Heading 3 after Heading 1; at most, Heading 2` |
| `HEADING_IN_PAGE_OVERLAY` | Un encabezado no va dentro de un `Page overlay` | `A Heading 1 cannot be inside a Page overlay...: a Text belongs there` |
| `BREAK_IN_HEADING` | Un `Text` de un encabezado no declara `Break` | `A Text of a heading does not accept Break: the engine already avoids breaking...` |
| `TEXT_BLANK_LINE` | Un `Paragraph` no tiene líneas en blanco en su `Content`, ni un `Text` que no es preformateado (línea de la línea en blanco) ([Text](../components/text.md)) | `A Paragraph is a single paragraph: the blank line is not needed...` |
| `LIST_ITEM_EMPTY` | Un `List item` contiene al menos un elemento ([List](../components/list.md)) | `A List item needs at least one element: a Paragraph, a Text, a Block...` |
| `LIST_NUMBER_NOT_ALLOWED` | `Start` y `Number` solo en las listas numeradas | `Start has no effect with Marker: bullet, which does not number the items` |
| `LIST_NUMBER_RANGE` | El número cabe en la numeración: `decimal` desde 0, letras desde 1, romanos de 1 a 3999 (línea del `Start`, del `Number` o del ítem) | `The number 4000 cannot be written with Marker: upper-roman, which numbers from 1 to 3999` |
| `LIST_ITEM_WITHOUT_LINE` | Un ítem con marcador empieza con un texto con el que alinearlo (línea de la caja que lo impide) | `The marker of the List item at line 10 is aligned with its first line, and the item starts with a Stack...` |
| `TABLE_ORDER` | `Header rows`, `Body` y `Footer rows` van en ese orden ([Table](../components/table.md)) | `Body goes after Footer rows: the groups of a Table go in the order Header rows, Body, Footer rows` |
| `TABLE_ROW_CELLS` | Cada fila llena exactamente las columnas de `Column sizes`, contando los `Span` de las filas de arriba (línea de la fila, o de la celda que sobra) | `The row leaves column 2 of 2 (Columns) free: a Cell is missing, empty if needed, or a Span` |
| `TABLE_SPAN` | Un `Span` no se sale de la tabla, no pasa de un grupo a otro ni se solapa con otra celda | `The cell takes up from column 1 to 3, and the table has 2 (Columns)` |
| `TABLE_COLUMN_ALIGN` | `Column align` da una alineación por columna de `Column sizes` (línea del `Column align`) | `Column align gives 2 alignments and the table has 3 columns (Columns): one goes per column` |
| `ROW_HEADER_IN_HEADER_ROWS` | `Row header: true` no se declara en una celda de `Header rows`, cuyas celdas ya son cabeceras de columna (línea del `Row header`) | `Row header has no effect on a cell of Header rows: the header cells are already column headers` |
| `CELL_CONTENT` | Una `Cell` tiene texto en el valor o elementos, no las dos cosas | `A Cell has text in the value (line 12) or elements, not both: the text goes in a Paragraph` |
| `TABLE_ROW_BREAK` | Las filas de `Header rows` y `Footer rows`, que se repiten en cada página, no declaran `Break` | `The rows of Header rows are repeated on each page of the table and do not break: Break is not needed` |
| `COLUMNS_WITHOUT_COUNT` | Un `Columns` declara `Count`, `Min column width` o los dos ([Columns](../components/columns.md)) | `A Columns needs Count, Min column width or both...` |
| `SEPARATOR_WIDTH` | `Separator` no es más ancho que `Gap` | `Separator is wider than Gap, which is 0 when not declared: the rule would be painted over the columns...` |
| `SINGLE_COLUMN` | Un `Columns` con `Count: 1` no declara `Gap` ni `Separator`, que separan columnas ([Columns](../components/columns.md)) | `Gap has no effect with Count: 1, because there are not two columns to separate: it is not needed` |
| `SEPARATOR_COLOR_WITHOUT_SEPARATOR` | `Separator color` va con `Separator` | `Separator color has no effect without Separator, the thickness of the rule between columns...` |
| `COLUMN_BREAK_OUTSIDE_COLUMNS` | `Before` y `After: column` solo dentro de un `Columns` (línea del `Before` o el `After`, también de una regla) | `After: column only has an effect inside a Columns: a page break uses page` |
| `KEEP_CONFLICT` | `Keep with next: true` no va con `After: page` o `column`, ni `Keep with previous: true` con `Before: page` o `column` (línea del `Keep`, también de una regla) ([Cortes](breaks.md#el-grupo-break)) | `Keep with next keeps the box with the next one and After: page separates it from it: one of the two is not needed` |
| `WEIGHT_CONFLICT` | `Bold` y `Weight` no van en el mismo `Typography`, porque son dos formas de escribir el peso (línea del `Bold`) ([Tipografía](typography.md#pesos-weight)) | `Bold and Weight are declared in the same Typography, and Bold is another way of writing Weight...` |
| `OVERLAY_IN_COLUMNS` | Un `Overlay` o un `Page overlay` no va dentro de un `Columns` | `Page overlay cannot be inside a Columns...` |
| `MARKDOWN_UNSUPPORTED` | El Markdown solo usa el subconjunto de [Markdown](../components/markdown.md#el-subconjunto), y una fila de tabla no tiene más celdas que la cabecera; un texto con `Markup: markdown` no tiene, dentro de la línea, imágenes, HTML ni saltos de línea forzados ([Formato del texto](text-formatting.md#formato-en-línea-markup)) (línea del bloque o del párrafo) | `Markdown does not allow images inside a text (an image goes alone in its paragraph): only headings, paragraphs, lists, tables, code blocks, images alone in their paragraph and...` |
| `MARKDOWN_PROPERTY_MISSING` | Un `Markdown` con listas con viñetas declara `Bullet markers`; con listas numeradas, `Number markers`; con tablas, `Table columns` (línea de la lista o la tabla) | `The list needs Bullet markers in its Markdown (line 8): the marker of each level, as in Bullet markers: bullet dash` |
| `CODE_WITHOUT_FONT` | El código, en línea o en un bloque, necesita `Code font` en `Typography`, propia o heredada ([Formato del texto](text-formatting.md#código-en-línea)); línea del texto, del párrafo o del bloque | `The code needs Code font: it is declared in the Typography of the text, of an ancestor or of Styles (For element: Document)...` |
| `LINK_SCHEME` | El destino de un enlace es una URL `https://`, `http://` (con servidor) o `mailto:` (con destinatario) o `#id`; no está vacío ni mal escrito ([Enlaces](links.md)) | `Link: 'javascript:alert(1)' does not have a supported scheme: only https://, http://, mailto: and #id` |
| `LINK_UNKNOWN` | Un enlace `#id` lleva a una caja que existe y que no está en un `Page overlay` ni dentro de uno | `The link #nada leads to an Id that does not exist: no box has the Id nada` |
| `IMAGE_UNKNOWN` | Una `Image` tiene ruta, y es la de un fichero de `images/` (línea de la `Image`) | `The resource logos/acme.png does not exist in .../resources/images (the path of an Image is relative to the images/ folder of the resources)` |
| `IMAGE_FORMAT` | Una `Image` es un PNG, un JPEG, un SVG del subconjunto admitido o un PDF sin cifrar, que se puede leer, y la página que dibuja de un PDF no tiene anotaciones que se vean ni fuentes que no cumplen PDF/UA, como una sin incrustar o sin `ToUnicode`, salvo que quien convierte quite esas comprobaciones (línea de la `Image`) ([Image](../components/image.md#svg)) | `The image dibujo.gif is not PNG, JPEG, SVG or PDF (it is GIF): it has to be converted to one of the four` |
| `IMAGE_PAGE` | `Pdf page` va en la `Image` de un PDF y es una página que el PDF tiene (línea de `Pdf page`) ([Image](../components/image.md#pdf)) | `The PDF planos.pdf has 3 pages and has no page 4` |
| `BREAK_IN_IMAGE` | Una `Image` o un `Barcode` no declara `Keep together` ni `Edges`, tampoco en una regla | `Keep together has no effect on an Image, which does not split across pages: it is not needed` |
| `BARCODE_DATA` | Un `Barcode` tiene sus datos en el valor o en `Content`, y su `Format` los puede codificar ([Barcode](../components/barcode.md#formatos)) | `The check digit of the EAN-13 8412345678901 is 5, not 1` |
| `BARCODE_WITHOUT_ALT` | Un `Barcode` lleva `Alt` ([Barcode](../components/barcode.md#accesibilidad)) | `The Barcode needs Alt, the text that says what it is and where it leads to whoever cannot see it, as in Alt: QR code of the invoice verification` |
| `BARCODE_PROPERTY_WITHOUT_EFFECT` | `Error correction` solo va en un QR, y de la `Typography` de un `Barcode` solo `Color` ([Barcode](../components/barcode.md#los-módulos)) | `Error correction only has an effect on a QR code, and the Barcode is ean-13: it is not needed` |
| `TABLE_OF_CONTENTS_EMPTY` | Un `Table of contents` tiene algún encabezado en los niveles de `Levels` (línea del `Table of contents`) | `The table of contents has no heading of levels 2 to 3 (Levels): it would have no entries` |
| `REFERENCE_UNKNOWN` | Una referencia tiene `Id`, y es el de una caja del documento (línea de la referencia, [Referencias](references.md)) | `{page of: nada}: no box has the Id nada` |
| `REFERENCE_NOT_PLACEABLE` | La página de una caja, que no está en un `Page overlay` ni dentro de uno ni en una página sin número | `{page of: pie}: pie is in a Page overlay, which is repeated on every page` |
| `REFERENCE_NOT_NUMBERED` | El número de una caja que lo tiene: un `List item` de una lista numerada o un encabezado con `Numbered: true` | `{number of: nota}: nota has no number (only the items of a numbered list and the headings with Numbered: true have one)` |
| `REFERENCE_UNSTABLE` | La página de las cajas referidas se estabiliza en 10 pasadas de paginación | `The page of x does not stabilize: after 10 passes it keeps changing...` |
| `STYLE_WITHOUT_RULE` | Cada nombre de `Style` tiene su regla `For style` | `The style aside has no rule: 'For style: aside' is missing in Styles` |
| `COLOR_DUPLICATE` | Dos `Named color` del mismo origen no tienen el mismo nombre ([Styles](styles.md#colores-con-nombre)) | `Repeated Named color: brand (already on line 6)` |
| `COLOR_UNKNOWN` | Un color con forma de nombre es el de un `Named color` del documento o del documento de estilos aparte (línea de la propiedad) | `Unknown color: brand (no Named color in Styles declares it)` |
| `OFFSET_CONFLICT` | Un `Frame` no se desplaza a la vez desde arriba y abajo, ni desde izquierda y derecha | `A Frame cannot be offset both from the top and from the bottom...` |
| `OVERCONSTRAINED` | Un `Overlay` o un `Page overlay` con inicio, tamaño y fin necesita algún margen `auto` | `Left, Width and right, all declared, set the position twice...` |
| `MARGIN_AUTO_WITHOUT_EFFECT` | `Margin: auto` solo donde centra ([La caja](box.md#márgenes-auto)) | `Margin auto does not center a box that takes up the whole width...` |
| `ISOLATE_MARGINS_IN_ITEM` | `Isolate margins: true` no va en un ítem de `Stack` o `Grid`, que ya corta la fusión de márgenes ([Block](../components/block.md#cajas-que-aíslan-los-márgenes)) | `Isolate margins has no effect on an item of Stack or Grid, which already stops margin merging with its children: it is not needed` |
| `START_WITHOUT_RESTART` | `Start` en una `Section` solo con `Page numbering: restart` ([Document](../components/document.md#numeración-de-las-páginas)) | `Start only has an effect with Page numbering: restart...` |
| `ROW_BOX` | El `Box` de una `Row` solo da su aspecto: `Background`, `Border`, `Border color` y `Corner radius` ([Table](../components/table.md#filas)) | `Padding has no effect on a Row, which is not a layout box...` |
| `PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS` | `Page number format` no va en una `Section` con `Page numbering: none` | `Page number format has no effect with Page numbering: none...` |
| `BEGIN_ON_FIRST_SECTION` | `Begin on: odd` no va en la primera `Section`, que ya empieza en la página 1 ([Document](../components/document.md#empezar-en-página-impar)) | `Begin on: odd has no effect in the first section...` |
| `LINE_DISTRIBUTION_WITHOUT_WRAP` | `Line distribution` solo con `Wrap: true` | `Line distribution only has an effect with Wrap: true...` |
| `GAP_WITHOUT_WRAP` | Un `Gap` de dos valores en un `Stack`, solo con `Wrap: true` ([Stack](../components/stack.md#propiedades-del-stack)) | `Gap has two values, but without Wrap there is a single line and the first, the gap between rows, has no effect: one value is enough...` |
| `GROW_WITHOUT_EFFECT` | `Grow` en un `Stack` vertical necesita un alto definido en el contenedor | `Grow has no effect: the vertical Stack of the parent has no definite height to distribute...` |
| `ITEM_MAIN_AXIS` | Un `Item` solo se alinea en el eje cruzado de su `Stack`: `Vertical` en uno horizontal y `Horizontal` en uno vertical ([Stack](../components/stack.md#item)) | `Horizontal has no effect on an item of a Stack with Direction: horizontal, because it is its main axis...` |
| `ASPECT_RATIO_CONFLICT` | `Aspect ratio` no se combina con `Width` y `Height` declarados | `Aspect ratio has no effect if Width and Height are declared` |
| `MIN_MAX_CONFLICT` | Mínimo, máximo y tamaño comparables no se contradicen ([La caja](box.md#mínimos-y-máximos)) | `Min width is greater than Max width (line 7)` |
| `INDEFINITE_PERCENTAGE` | Un porcentaje vertical necesita un alto definido ([Valores](values.md#alto-definido)) | `Height uses a percentage of a height that is not definite...` |
| `ROTATE_ORIGIN_WITHOUT_ROTATE` | `Rotate origin` solo con `Rotate` | `Rotate origin has no effect without Rotate: the rotation is missing or Rotate origin is not needed` |
| `BREAK_IN_ROTATED` | Un `Frame` girado o una caja girada en flujo no declara `Keep together` ni `Edges`, tampoco en una regla ([Capas y giro](layers-and-rotation.md#giro-rotate)) | `Keep together has no effect on a rotated Frame, which does not split across pages: it is not needed` |
| `BREAK_IN_UNBREAKABLE` | Un elemento dentro de un `Overlay`, un `Page overlay`, un `Frame` girado o una caja girada en flujo, que no se parten, no escribe `Break` (tampoco la `Row` de una celda); lo que llega de una regla no se comprueba ([Cortes](breaks.md#cortes-forzados)) | `Before has no effect inside an Overlay, which is not split across pages: it is not needed` |
| `ISOLATE_MARGINS_IN_ROTATED` | `Isolate margins: true` no va en una caja girada en flujo, que ya corta la fusión de márgenes ([Capas y giro](layers-and-rotation.md#giro-en-flujo-block-text-paragraph-e-image)) | `Isolate margins has no effect on a rotated box, which already stops margin merging with its children: it is not needed` |
| `ROTATED_ITEM` | Un `Block`, `Text`, `Paragraph` o `Image` girado no es un ítem de `Stack` o `Grid` ([Capas y giro](layers-and-rotation.md#giro-en-flujo-block-text-paragraph-e-image)) | `A Stack item cannot rotate: it takes up the place its container gives it. What rotates is a box inside the item...` |
| `GRID_PLACEMENT` | `Areas` rectangulares y con filas de igual longitud; valor de `Area` válido y con áreas existentes; un ítem con la fila fija cabe en ella ([Grid](../components/grid.md#colocación)) | `Unknown area in Area: foot` |
| `FONT_UNKNOWN` | `Font` existe en la carpeta de fuentes (línea de la `Font`); también `Code font` (línea de la `Code font`) | `Unknown font: Arial. Families in .../resources/fonts: IBM Plex Mono, IBM Plex Sans, IBM Plex Serif` |
| `FONT_VARIANT_MISSING` | La familia tiene la variante de `Weight` (o `Bold`) e `Italic`, y la de cada tramo con formato (línea del texto, o la del tramo) | `The font IBM Plex Sans has no variant bold italic... (Font on line 6, ...)` |
| `GLYPH_MISSING` | La fuente tiene todos los caracteres del texto (línea del carácter), y el guion si el texto puede partirse | `The font IBM Plex Sans normal has no character '漢' (U+6F22)` |
| `HYPHENATION_LANGUAGE_UNSUPPORTED` | `Hyphenate: true` solo en un texto cuyo `Language` tiene patrones en el motor: `es`, `ca`, `en-US` o `en-GB` ([Tipografía](typography.md#guiones-hyphenate)); línea del `Hyphenate`, aunque esté en una antecesora o en una regla | `Hyphenate: true needs hyphenation patterns for the language 'de', and the engine has them for es, ca, en-US and en-GB: the text needs Hyphenate: false, or one of those languages in Language` |
| `CONTRAST_LOW` | El texto tiene un contraste de 4,5:1 con lo que tiene debajo, o de 3:1 si es grande ([El PDF](pdf.md#contraste)); línea del `Color` del texto, o de su caja si no lo tiene | `The text of box at line 12 has a contrast of 2.84:1 with its background (#999999 on #FFFFFF) and needs 4.5:1...` |
| `TEXT_OVERFLOW` | Ninguna palabra es más ancha que su línea, ni una línea de un texto con `Preformatted: true` (línea de la palabra) | `The word 'supercalifragilística' does not fit on the line: the line measures 138.69 pt and the box leaves 100 pt...` |
| `BOX_OVERFLOW` | Las cajas caben en su contenedor, el contenido en su alto (también en las columnas de un `Columns`), y ninguna caja se sale de la página física | `The content of box at line 7 does not fit in its height: its text reaches 24 pt and the box leaves 20 pt` |
| `LIST_MARKER_OVERFLOW` | El marcador de un ítem, con su espacio, cabe en el `Padding` izquierdo de la lista más el margen izquierdo del ítem (línea del ítem) | `The marker '1.' of List item at line 12 measures 11.45 pt with its space, and the indent leaves 5 pt...` |
| `TABLE_REPEAT_OVERFLOW` | La cabecera y el pie de una tabla dejan sitio al cuerpo en una página vacía (línea de la `Table`) | `The header and footer of the table at line 8 measure 900 pt and leave no room for the body on an empty page...` |
| `PAGE_OVERFLOW` | Lo que no se puede partir cabe en una página | `Box at line 9 does not fit on the page and cannot be broken: it is 900 pt high...` |
| `PAGE_LIMIT` | El documento no ocupa más páginas que el límite, 2000 por defecto ([Límites](limits.md); no es de ninguna línea) | `The document has more than 2000 pages, the maximum the engine allows (Limits.maxPages changes it)` |
| `IMAGE_TOO_LARGE` | Un PNG o un JPEG no tiene más píxeles que el límite, 25 millones por defecto (línea de la `Image`) | `The image test/limit-huge.png has 6000 × 6000 pixels (36.0 Mpx) and the maximum is 25.0 Mpx: its resolution has to be reduced...` |
| `SVG_TOO_COMPLEX` | Un SVG no dibuja más elementos (contando lo que repite cada `<use>`) ni anida más niveles que el límite, 20 000 y 64 por defecto (línea de la `Image`) | `The SVG test/limit-depth.svg nests 72 levels and the maximum is 64: it has to be flattened...` |
| `PDF_TOO_LARGE` | Un PDF no ocupa más bytes que el límite, 20 MB por defecto (línea de la `Image`) | `The PDF planos.pdf takes up 31.4 MB and the maximum is 20.0 MB: a smaller PDF is needed...` |

[`test/errors/`](../../../../test/errors) tiene un documento de ejemplo por cada código, con la línea que debe dar
([Pruebas](../implementation/testing.md#catálogo-de-errores-errorcatalogtest)).

## Tipos de error

Cada error dice, además de su código y su línea, **de quién es la culpa**, que es lo que un servicio necesita para
contestar:

| Tipo | Qué es | Un servicio contesta |
|---|---|---|
| `INVALID_DOCUMENT` | El documento o sus estilos no son válidos: sintaxis, schema, una regla del modelo, un desbordamiento, un recurso que el documento nombra y no existe | un error del cliente (4xx), con la línea |
| `RESOURCE` | La carpeta de recursos no es utilizable (no existe, una fuente o una imagen estropeadas) o no se puede leer el fichero del documento | un fallo de su configuración (5xx) |
| `LIMIT` | El documento es válido pero pide más de lo que se admite ([Límites](limits.md)): más páginas, una imagen con demasiados píxeles, un SVG con demasiados elementos o niveles, un PDF demasiado grande, o un texto STXT demasiado grande o anidado | un error del cliente por exceso (413 o 422) |
| `INTERNAL` | Un fallo del motor y no del documento ni del entorno | un fallo del servidor (500), que conviene informar |

El código es el de la tabla de arriba, el de STXT para los errores de sintaxis y de schema (`CHILD_NOT_DECLARED`),
`RESOURCE`, `IMAGE_FORMAT` o `INTERNAL`. La línea es `0` si el error no es de una línea (un recurso, un límite o un fallo
interno). Si es del [documento de estilos aparte](styles.md#documento-de-estilos-aparte), el mensaje lo dice (`line 8
of the styles: ...`). Cómo se ven desde Java está en [Uso desde Java](../implementation/api.md#errores).

> **Decisión:** un fallo que el motor debería haber evitado (una página que no avanza, un enlace sin destino) es
> `INTERNAL`: es un defecto del motor y no del documento.
