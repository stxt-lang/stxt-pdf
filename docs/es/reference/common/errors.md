# Errores

El motor es **estricto**: un documento que pide algo que no tiene efecto, o dos cosas que se contradicen, es un error y no
un caso que se resuelve con una regla por defecto. Así el resultado es predecible: lo que se lee en el documento es lo que
se obtiene.

- **Cada error tiene un código estable** y un mensaje que dice qué falla y cómo arreglarlo. Los mensajes pueden mejorar,
  pero los códigos no cambian.
- **La línea es la del nodo culpable:** la de la propiedad (y no la de su caja ni la de su grupo), la del carácter
  dentro de un bloque `Content >>` o, si la familia no existe, la de la `Font` declarada en una antecesora. Si el problema es la
  combinación de dos propiedades, el mensaje nombra la línea de la otra. Los mensajes con línea empiezan por `línea N:`.
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
| `DOCUMENT_ROOT` | Un único nodo raíz, `Document`, con el namespace `dev.stxt.pdf` (el schema acepta cualquier elemento como raíz); en un documento de estilos aparte, `Styles` | `El nodo raíz debe ser Document (dev.stxt.pdf):` |
| `PAGE_SIZE` | El tamaño de `Page`, con `Paper` (y `Orientation`) o con `Width` y `Height`, nunca las dos cosas; `Width` y `Height`, puntos positivos; `Margin`, solo puntos, y deja área útil; una `Section` que solo gira (`Orientation`) un documento con papel ([Document](../components/document.md#page)) | `Los márgenes de la página no dejan área útil` |
| `SECTION_EMPTY` | Una `Section` tiene contenido: algún elemento que no sea un `Page overlay` (línea de la sección) | `La sección no tiene contenido: sin elementos (los Page overlay no cuentan...) no tiene ninguna página` |
| `PAGE_NUMBER_UNAVAILABLE` | Un texto con `{page}` o `{pages}` no sale en una página sin número (`Page numbering: none`); línea del texto | `El texto de la caja pie tiene {page} o {pages} y sale en una página sin número...` |
| `VALUE_SYNTAX` | Sintaxis de los valores ([Valores](values.md)), colores (`#RGB`, `#RGBA`, `#RRGGBB` o `#RRGGBBAA`), `linear(...)` mal escrito, más de cuatro colores en `Border color` o de cuatro valores en `Corner radius`, `Aspect ratio`, `For column` que no es un número | `Valor no válido: 12,5 (el decimal se escribe con punto...)` |
| `VALUE_NOT_ALLOWED` | Formas y signos que admite cada propiedad ([Valores](values.md#tipos-de-valor)); `Gap` con 1 o 2 valores (en `Columns`, 1); `Size` > 0; `Count` ≥ 1; `Min column width` y `Separator` > 0; `Letter spacing` en puntos; `Tab size` ≥ 1; `Title` y `Alt` no vacíos y `Language` una etiqueta BCP 47; `Bullet markers` con marcadores sin numerar y `Number markers` con numeraciones; `Levels` con dos niveles de 1 a 6, el primero no mayor que el último; `Rotate` entre −360 y 360, sin 0 ni las vueltas enteras; `Rotate origin` con una o dos palabras clave, como mucho una por eje; `For column` de menor a mayor; `Number label` con `{number}` una vez | `Width no admite *: admite un número...` |
| `FIT_TRACK` | `fit` no es una pista de grid | `fit no es una pista de grid: la que se ajusta al contenido es auto` |
| `DUPLICATE_ID` | Los `Id` son únicos | `Id repetido: a (ya se usa en la línea 6)` |
| `SINGLE_ELEMENT` | `Item` y `Area` contienen exactamente un elemento | `Un Area contiene exactamente un elemento, y este tiene Block (línea 8) y Text (línea 10)` |
| `TEXT_CONTENT` | Un `Text` tiene su texto en el valor o en `Content`, uno de los dos; un `Markdown` tiene contenido, y sus encabezados y párrafos, texto | `Text tiene el texto en el valor y en Content (línea 9): va en uno solo` |
| `TEXT_WITHOUT_FONT` | El texto, y un `Table of contents`, necesitan `Font` en `Typography`, propia o heredada | `El texto necesita Font: se declara en el Typography del Text, de un antecesor o de Styles (For element: Document)` |
| `TEXT_WITHOUT_FONT_SIZE` | El texto necesita `Size` en `Typography`, propio o heredado | `El texto necesita Size: se declara en el Typography del Text, de un antecesor o de Styles (For element: Document)` |
| `STYLE_UNKNOWN_ELEMENT` | `For element` nombra un elemento de caja, un elemento generado o una posición en una tabla ([Styles](styles.md)) | `For element: 'Item' no es un elemento de caja (Block, Stack...)` |
| `STYLE_DUPLICATE` | Dos reglas del mismo origen no tienen el mismo selector | `Regla repetida: For element: block (ya está en la línea 6)` |
| `STYLE_NOT_ALLOWED` | Una regla que llega a una `Section` solo da `Typography`; una regla (también de posición, de columna o del estilo de una `Row`) no da `Break` a un `Overlay`, un `Page overlay`, un encabezado, un `Text` de un encabezado o una `Cell`; `For element: Document` solo admite `Typography`, y en ella no `Language` | `La regla For style: page aplica Break a un Overlay (línea 11)...` |
| `HEADING_LEVEL` | El primer encabezado es `Heading 1` y cada uno sube como mucho un nivel ([Encabezados](../components/heading.md#jerarquía)) | `Salto de nivel: Heading 3 después de Heading 1; como mucho, Heading 2` |
| `HEADING_IN_PAGE_OVERLAY` | Un encabezado no va dentro de un `Page overlay` | `Un Heading 1 no puede estar dentro de un Page overlay...: ahí va un Text` |
| `BREAK_IN_HEADING` | Un `Text` de un encabezado no declara `Break` | `Un Text de un encabezado no admite Break: el motor ya evita cortar...` |
| `TEXT_BLANK_LINE` | Un `Paragraph` no tiene líneas en blanco en su `Content`, ni un `Text` que no es preformateado (línea de la línea en blanco) ([Text](../components/text.md)) | `Un Paragraph es un solo párrafo: sobra la línea en blanco...` |
| `LIST_ITEM_EMPTY` | Un `List item` contiene al menos un elemento ([List](../components/list.md)) | `Un List item necesita al menos un elemento: un Paragraph, un Text, un Block...` |
| `LIST_NUMBER_NOT_ALLOWED` | `Start` y `Number` solo en las listas numeradas | `Start no tiene efecto con Marker: bullet, que no numera los ítems` |
| `LIST_NUMBER_RANGE` | El número cabe en la numeración: `decimal` desde 0, letras desde 1, romanos de 1 a 3999 (línea del `Start`, del `Number` o del ítem) | `El número 4000 no se puede escribir con Marker: upper-roman, que numera de 1 a 3999` |
| `LIST_ITEM_WITHOUT_LINE` | Un ítem con marcador empieza con un texto con el que alinearlo (línea de la caja que lo impide) | `El marcador del List item de la línea 10 se alinea con su primera línea, y el ítem empieza con un Stack...` |
| `TABLE_ORDER` | `Header rows`, `Body` y `Footer rows` van en ese orden ([Table](../components/table.md)) | `Body va después de Footer rows: los grupos de una Table van en el orden Header rows, Body, Footer rows` |
| `TABLE_ROW_CELLS` | Cada fila llena exactamente las columnas de `Column sizes`, contando los `Span` de las filas de arriba (línea de la fila, o de la celda que sobra) | `La fila deja libre la columna 2 de 2 (Columns): falta una Cell, vacía si hace falta, o un Span` |
| `TABLE_SPAN` | Un `Span` no se sale de la tabla, no pasa de un grupo a otro ni se solapa con otra celda | `La celda ocupa de la columna 1 a la 3, y la tabla tiene 2 (Columns)` |
| `TABLE_COLUMN_ALIGN` | `Column align` da una alineación por columna de `Column sizes` (línea del `Column align`) | `Column align da 2 alineaciones y la tabla tiene 3 columnas (Columns): va una por columna` |
| `ROW_HEADER_IN_HEADER_ROWS` | `Row header: true` no se declara en una celda de `Header rows`, cuyas celdas ya son cabeceras de columna (línea del `Row header`) | `Row header no tiene efecto en una celda de Header rows: las celdas de la cabecera ya son cabeceras de columna` |
| `CELL_CONTENT` | Una `Cell` tiene texto en el valor o elementos, no las dos cosas | `Una Cell tiene texto en el valor (línea 12) o elementos y no las dos cosas: el texto va en un Paragraph` |
| `TABLE_ROW_BREAK` | Las filas de `Header rows` y `Footer rows`, que se repiten en cada página, no declaran `Break` | `Las filas de Header rows se repiten en cada página de la tabla y no se cortan: sobra el Break` |
| `COLUMNS_WITHOUT_COUNT` | Un `Columns` declara `Count`, `Min column width` o los dos ([Columns](../components/columns.md)) | `Un Columns necesita Count, Min column width o los dos...` |
| `SEPARATOR_WIDTH` | `Separator` no es más ancho que `Gap` | `Separator es más ancho que Gap, que sin declarar es 0: la línea se pintaría sobre las columnas...` |
| `SINGLE_COLUMN` | Un `Columns` con `Count: 1` no declara `Gap` ni `Separator`, que separan columnas ([Columns](../components/columns.md)) | `Gap no tiene efecto con Count: 1, porque no hay dos columnas que separar: sobra` |
| `SEPARATOR_COLOR_WITHOUT_SEPARATOR` | `Separator color` va con `Separator` | `Separator color no tiene efecto sin Separator, el grosor de la línea entre columnas...` |
| `COLUMN_BREAK_OUTSIDE_COLUMNS` | `Before` y `After: column` solo dentro de un `Columns` (línea del `Before` o el `After`, también de una regla) | `After: column solo tiene efecto dentro de un Columns: para cortar la página se usa page` |
| `KEEP_CONFLICT` | `Keep with next: true` no va con `After: page` o `column`, ni `Keep with previous: true` con `Before: page` o `column` (línea del `Keep`, también de una regla) ([Cortes](breaks.md#el-grupo-break)) | `Keep with next mantiene la caja con la siguiente y After: page la separa de ella: sobra uno de los dos` |
| `OVERLAY_IN_COLUMNS` | Un `Overlay` o un `Page overlay` no va dentro de un `Columns` | `Un Page overlay no puede estar dentro de un Columns...` |
| `MARKDOWN_UNSUPPORTED` | El Markdown solo usa el subconjunto de [Markdown](../components/markdown.md#el-subconjunto), y una fila de tabla no tiene más celdas que la cabecera; un texto con `Markup: markdown` no tiene, dentro de la línea, imágenes, HTML ni saltos de línea forzados ([Formato del texto](text-formatting.md#formato-en-línea-markup)) (línea del bloque o del párrafo) | `Markdown no admite imágenes dentro de un texto (una imagen va sola en su párrafo): solo admite encabezados, párrafos, listas, tablas, bloques de código y...` |
| `MARKDOWN_PROPERTY_MISSING` | Un `Markdown` con listas con viñetas declara `Bullet markers`; con listas numeradas, `Number markers`; con tablas, `Table columns` (línea de la lista o la tabla) | `La lista necesita Bullet markers en su Markdown (línea 8): el marcador de cada nivel, como Bullet markers: bullet dash` |
| `CODE_WITHOUT_FONT` | El código, en línea o en un bloque, necesita `Code font` en `Typography`, propia o heredada ([Formato del texto](text-formatting.md#código-en-línea)); línea del texto, del párrafo o del bloque | `El código necesita Code font: se declara en el Typography del texto, de un antecesor o de Styles (For element: Document)...` |
| `LINK_SCHEME` | El destino de un enlace es una URL `https://`, `http://` (con servidor) o `mailto:` (con destinatario) o `#id`; no está vacío ni mal escrito ([Enlaces](links.md)) | `Link: 'javascript:alert(1)' no tiene un esquema admitido: solo https://, http://, mailto: y #id` |
| `LINK_UNKNOWN` | Un enlace `#id` lleva a una caja que existe y que no está en un `Page overlay` ni dentro de uno | `El enlace #nada lleva a un Id que no existe: ninguna caja tiene el Id nada` |
| `IMAGE_UNKNOWN` | Una `Image` tiene ruta, y es la de un fichero de `images/` (línea de la `Image`) | `No existe el recurso logos/acme.png en .../resources/images` |
| `IMAGE_FORMAT` | Una `Image` es un PNG, un JPEG, un SVG del subconjunto admitido o un PDF sin cifrar, que se puede leer, y la página que dibuja de un PDF no tiene anotaciones que se vean ni fuentes sin incrustar o sin `ToUnicode`, salvo que quien convierte quite esas comprobaciones (línea de la `Image`) ([Image](../components/image.md#svg)) | `La imagen dibujo.gif no es PNG, JPEG, SVG ni PDF (es GIF): hay que convertirla a uno de los cuatro` |
| `IMAGE_PAGE` | `Pdf page` va en la `Image` de un PDF y es una página que el PDF tiene (línea de `Pdf page`) ([Image](../components/image.md#pdf)) | `El PDF planos.pdf tiene 3 páginas y no tiene la 4` |
| `BREAK_IN_IMAGE` | Una `Image` no declara `Keep together` ni `Edges`, tampoco en una regla | `Keep together no tiene efecto en una Image, que no se parte entre páginas: sobra` |
| `TABLE_OF_CONTENTS_EMPTY` | Un `Table of contents` tiene algún encabezado en los niveles de `Levels` (línea del `Table of contents`) | `El índice no tiene ningún encabezado de los niveles 2 a 3 (Levels): no tendría entradas` |
| `REFERENCE_UNKNOWN` | Una referencia tiene `Id`, y es el de una caja del documento (línea de la referencia, [Referencias](references.md)) | `{page of: nada}: ninguna caja tiene el Id nada` |
| `REFERENCE_NOT_PLACEABLE` | La página de una caja, que no está en un `Page overlay` ni dentro de uno ni en una página sin número | `{page of: pie}: pie está en un Page overlay, que se repite en todas las páginas` |
| `REFERENCE_NOT_NUMBERED` | El número de una caja que lo tiene: un `List item` de una lista numerada o un encabezado con `Numbered: true` | `{number of: nota}: nota no tiene número (solo lo tienen los ítems de una lista numerada y los encabezados con Numbered: true)` |
| `REFERENCE_UNSTABLE` | La página de las cajas referidas se estabiliza en 10 pasadas de paginación | `La página de x no se estabiliza: tras 10 pasadas sigue cambiando...` |
| `STYLE_WITHOUT_RULE` | Cada nombre de `Style` tiene su regla `For style` | `El estilo aside no tiene regla: falta 'For style: aside' en Styles` |
| `OFFSET_CONFLICT` | Un `Frame` no se desplaza a la vez desde arriba y abajo, ni desde izquierda y derecha | `Un Frame no puede desplazarse a la vez desde arriba y desde abajo...` |
| `OVERCONSTRAINED` | Un `Overlay` o un `Page overlay` con inicio, tamaño y fin necesita algún margen `auto` | `Izquierda, Width y derecha declarados fijan la posición dos veces...` |
| `MARGIN_AUTO_WITHOUT_EFFECT` | `Margin: auto` solo donde centra ([La caja](box.md#márgenes-auto)) | `Margin auto no centra una caja que ocupa todo el ancho...` |
| `ISOLATE_MARGINS_IN_ITEM` | `Isolate margins: true` no va en un ítem de `Stack` o `Grid`, que ya corta la fusión de márgenes ([Block](../components/block.md#cajas-que-aíslan-los-márgenes)) | `Isolate margins no tiene efecto en un ítem de Stack o Grid, que ya corta la fusión de márgenes con sus hijos: sobra` |
| `START_WITHOUT_RESTART` | `Start` en una `Section` solo con `Page numbering: restart` ([Document](../components/document.md#numeración-de-las-páginas)) | `Start solo tiene efecto con Page numbering: restart...` |
| `ROW_BOX` | El `Box` de una `Row` solo da su aspecto: `Background`, `Border`, `Border color` y `Corner radius` ([Table](../components/table.md#filas)) | `Padding no tiene efecto en una Row, que no es una caja del layout...` |
| `PAGE_NUMBER_FORMAT_WITHOUT_NUMBERS` | `Page number format` no va en una `Section` con `Page numbering: none` | `Page number format no tiene efecto con Page numbering: none...` |
| `BEGIN_ON_FIRST_SECTION` | `Begin on: odd` no va en la primera `Section`, que ya empieza en la página 1 ([Document](../components/document.md#empezar-en-página-impar)) | `Begin on: odd no tiene efecto en la primera sección...` |
| `LINE_DISTRIBUTION_WITHOUT_WRAP` | `Line distribution` solo con `Wrap: true` | `Line distribution solo tiene efecto con Wrap: true...` |
| `GAP_WITHOUT_WRAP` | Un `Gap` de dos valores en un `Stack`, solo con `Wrap: true` ([Stack](../components/stack.md#propiedades-del-stack)) | `Gap tiene dos valores, pero sin Wrap hay una sola línea y el primero, el hueco entre filas, no tiene efecto: va un solo valor...` |
| `GROW_WITHOUT_EFFECT` | `Grow` en un `Stack` vertical necesita un alto definido en el contenedor | `Grow no tiene efecto: el Stack vertical del padre no tiene un alto definido...` |
| `ITEM_MAIN_AXIS` | Un `Item` solo se alinea en el eje cruzado de su `Stack`: `Vertical` en uno horizontal y `Horizontal` en uno vertical ([Stack](../components/stack.md#item)) | `Horizontal no tiene efecto en un ítem de un Stack con Direction: horizontal, porque es su eje principal...` |
| `ASPECT_RATIO_CONFLICT` | `Aspect ratio` no se combina con `Width` y `Height` declarados | `Aspect ratio no tiene efecto si Width y Height están declarados` |
| `MIN_MAX_CONFLICT` | Mínimo, máximo y tamaño comparables no se contradicen ([La caja](box.md#mínimos-y-máximos)) | `Min width es mayor que Max width (línea 7)` |
| `INDEFINITE_PERCENTAGE` | Un porcentaje vertical necesita un alto definido ([Valores](values.md#alto-definido)) | `Height usa un porcentaje de un alto que no es definido...` |
| `ROTATE_ORIGIN_WITHOUT_ROTATE` | `Rotate origin` solo con `Rotate` | `Rotate origin no tiene efecto sin Rotate: falta el giro o sobra Rotate origin` |
| `BREAK_IN_ROTATED` | Un `Frame` girado o una caja girada en flujo no declara `Keep together` ni `Edges`, tampoco en una regla ([Capas y giro](layers-and-rotation.md#giro-rotate)) | `Keep together no tiene efecto en un Frame girado, que no se parte entre páginas: sobra` |
| `BREAK_IN_UNBREAKABLE` | Un elemento dentro de un `Overlay`, un `Page overlay`, un `Frame` girado o una caja girada en flujo, que no se parten, no escribe `Break` (tampoco la `Row` de una celda); lo que llega de una regla no se comprueba ([Cortes](breaks.md#cortes-forzados)) | `Before no tiene efecto dentro de un Overlay, que no se parte entre páginas: sobra` |
| `ISOLATE_MARGINS_IN_ROTATED` | `Isolate margins: true` no va en una caja girada en flujo, que ya corta la fusión de márgenes ([Capas y giro](layers-and-rotation.md#giro-en-flujo-block-text-paragraph-e-image)) | `Isolate margins no tiene efecto en una caja girada, que ya corta la fusión de márgenes con sus hijos: sobra` |
| `ROTATED_ITEM` | Un `Block`, `Text`, `Paragraph` o `Image` girado no es un ítem de `Stack` o `Grid` ([Capas y giro](layers-and-rotation.md#giro-en-flujo-block-text-paragraph-e-image)) | `Un ítem de Stack no puede girar: ocupa el sitio que le da su contenedor. Lo que gira es una caja dentro del ítem...` |
| `GRID_PLACEMENT` | `Areas` rectangulares y con filas de igual longitud; valor de `Area` válido y con áreas existentes; un ítem con la fila fija cabe en ella ([Grid](../components/grid.md#colocación)) | `Área desconocida en Area: foot` |
| `FONT_UNKNOWN` | `Font` existe en la carpeta de fuentes (línea de la `Font`); también `Code font` (línea de la `Code font`) | `Fuente desconocida: Arial. Familias en .../resources/fonts: IBM Plex Mono, IBM Plex Sans, IBM Plex Serif` |
| `FONT_VARIANT_MISSING` | La familia tiene la variante de `Bold` e `Italic`, y la de cada tramo con formato (línea del texto, o la del tramo) | `La fuente IBM Plex Sans no tiene la variante bold italic... (Font en la línea 6, ...)` |
| `GLYPH_MISSING` | La fuente tiene todos los caracteres del texto (línea del carácter) | `La fuente IBM Plex Sans normal no tiene el carácter '漢' (U+6F22)` |
| `CONTRAST_LOW` | El texto tiene un contraste de 4,5:1 con lo que tiene debajo, o de 3:1 si es grande ([El PDF](pdf.md#contraste)); línea del `Color` del texto, o de su caja si no lo tiene | `El texto de la caja de la línea 12 tiene un contraste de 2,84:1 con su fondo (#999999 sobre #FFFFFF) y necesita 4,5:1...` |
| `TEXT_OVERFLOW` | Ninguna palabra es más ancha que su línea, ni una línea de un texto con `Preformatted: true` (línea de la palabra) | `La palabra 'supercalifragilística' no cabe en la línea: la línea mide 138.69 y la caja deja 100...` |
| `BOX_OVERFLOW` | Las cajas caben en su contenedor, el contenido en su alto (también en las columnas de un `Columns`), y los `Overlay`, los `Page overlay` y lo girado en la página | `El contenido de la caja de la línea 7 no cabe en su alto: su texto llega a 24 y la caja deja 20` |
| `LIST_MARKER_OVERFLOW` | El marcador de un ítem, con su espacio, cabe en el `Padding` izquierdo de la lista más el margen izquierdo del ítem (línea del ítem) | `El marcador '1.' del List item de la línea 12 mide 11.45 con su espacio, y la sangría deja 5...` |
| `TABLE_REPEAT_OVERFLOW` | La cabecera y el pie de una tabla dejan sitio al cuerpo en una página vacía (línea de la `Table`) | `La cabecera y el pie de la tabla de la línea 8 miden 900 y no dejan sitio al cuerpo en una página vacía...` |
| `PAGE_OVERFLOW` | Lo que no se puede partir cabe en una página | `La caja de la línea 9 no cabe en la página y no se puede partir: mide 900 de alto...` |
| `PAGE_LIMIT` | El documento no ocupa más páginas que el límite, 2000 por defecto ([Límites](limits.md); no es de ninguna línea) | `El documento tiene más de 2000 páginas, el máximo que admite el motor (Limits.maxPages lo cambia)` |
| `IMAGE_TOO_LARGE` | Un PNG o un JPEG no tiene más píxeles que el límite, 25 millones por defecto (línea de la `Image`) | `La imagen test/limit-huge.png tiene 6000 × 6000 píxeles (36,0 Mpx) y el máximo es 25,0 Mpx: hay que reducir su resolución...` |
| `SVG_TOO_COMPLEX` | Un SVG no dibuja más elementos (contando lo que repite cada `<use>`) ni anida más niveles que el límite, 20 000 y 64 por defecto (línea de la `Image`) | `El SVG test/limit-depth.svg anida 72 niveles y el máximo es 64: hay que aplanarlo...` |
| `PDF_TOO_LARGE` | Un PDF no ocupa más bytes que el límite, 20 MB por defecto (línea de la `Image`) | `El PDF planos.pdf ocupa 31,4 MB y el máximo es 20,0 MB: hace falta un PDF más pequeño...` |

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
interno). Si es del [documento de estilos aparte](styles.md#documento-de-estilos-aparte), el mensaje lo dice (`línea 8
de los estilos: ...`). Cómo se ven desde Java está en [Uso desde Java](../implementation/api.md#errores).

> **Decisión:** un fallo que el motor debería haber evitado (una página que no avanza, un enlace sin destino) es
> `INTERNAL`: es un defecto del motor y no del documento.
