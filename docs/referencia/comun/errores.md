# Errores

El motor es **estricto**: un documento que pide algo que no tiene efecto, o dos cosas que se contradicen, es un error, no
un caso que se resuelve con una regla por defecto. Así el resultado es predecible: lo que se lee en el documento es lo que
se obtiene.

- **Cada error tiene un código estable** y un mensaje que dice qué falla y cómo arreglarlo. Los mensajes pueden mejorar;
  los códigos no cambian.
- **La línea es la del nodo culpable:** la de la propiedad, no la de su caja ni la de su grupo; la del carácter dentro de
  un bloque `Content >>`; la de la `Font` declarada en una antecesora, si la familia no existe. Si el problema es la
  combinación de dos propiedades, el mensaje nombra la línea de la otra. Los mensajes con línea empiezan por `línea N:`.
- **Se para en el primer error**: acumularlos daría errores en cascada (una caja mal definida provoca otros en sus
  hijos).

## Lo que no tiene efecto

Lo que un elemento declara y no puede tener efecto por cómo está escrito es un error: `Lines` o un `Gap` de dos valores
sin `Wrap`, `Rotate origin` sin `Rotate`, `Rule color` sin `Rule`, un margen `auto` que no centra, un `Break` dentro de un
`Absolute`… No lo es, a propósito:

- **Escribir el valor por defecto:** `Span: 1 1`, `Isolate: false`, `Start: 1`. No contradice nada.
- **Lo que se hereda:** una propiedad de `Typography` vale para los descendientes, así que no se comprueba si la caja que
  la declara la usa (`Numbered` en un `Block`, `Small caps` con `Transform: uppercase`).
- **El aspecto que depende de otra propiedad:** `Border color` sin `Border` y `Radius` sin fondo, borde ni imagen. Suelen
  venir de una regla de clase que comparten cajas con y sin borde.
- **Un `Break` que llega de una regla** a un elemento dentro de una caja que no se parte: una regla por tipo
  (`Element: Paragraph`) daría un error en cada párrafo de un pie ([Cortes](cortes.md#cortes-forzados)).
- **Lo que puede llegar después:** una regla que no se aplica a ningún elemento e `Inherit fixed: false` sin `Fixed` en el
  documento. Una plantilla puede añadirlos.
- **Lo que depende de la maquetación:** un mínimo y un máximo en unidades distintas que chocan (gana el mínimo,
  [La caja](caja.md#mínimos-y-máximos)) y `Fill: auto` en un `Multicol` sin alto que llenar.
- **Las propiedades de un `Markdown` que su contenido no usa** (`Bullet` sin listas): el contenido cambia a menudo
  ([Markdown](../componentes/markdown.md)).

## Niveles de validación

1. **Sintaxis STXT y schema** ([Estructura](estructura.md#schema)): qué hijos admite cada elemento, cuántos, valores
   vacíos, valores cerrados, enteros y booleanos. Es donde se detecta una propiedad fuera de su elemento (`Offset` en un
   `Block`, `Columns` en un `Flex`), con los códigos de STXT (`CHILD_NOT_DECLARED`, `TOO_MANY_CHILDREN`,
   `TOO_FEW_CHILDREN`, `INVALID_VALUE`…).
2. **Modelo:** las reglas que el schema no puede expresar, con los códigos de abajo.
3. **Recursos** ([Recursos](recursos.md)): las fuentes y las imágenes ([Image](../componentes/image.md)).
4. **Desbordamientos** ([Desbordamiento](desbordamiento.md)): sobre el resultado maquetado.
5. **Contraste** ([El PDF](pdf.md#contraste)): sobre lo que se pinta, al convertir; quien convierte lo puede desactivar.

## Códigos

| Código | Regla | Mensaje (ejemplo) |
|---|---|---|
| `DOCUMENT_ROOT` | Un único nodo raíz, `Document`, con el namespace `dev.stxt.pdf` (el schema acepta cualquier elemento como raíz); en un documento de estilos aparte, `Styles` | `El nodo raíz debe ser Document (dev.stxt.pdf):` |
| `MEDIA_SIZE` | El tamaño del medio, con `Paper` (y `Orientation`) o con `Width` y `Height`, nunca las dos cosas; `Width` y `Height`, puntos positivos; `Margin`, solo puntos, y deja área útil; una `Section` que solo gira (`Orientation`) un documento con papel ([Document](../componentes/document.md#media)) | `Los márgenes del medio no dejan área útil` |
| `SECTION_EMPTY` | Una `Section` tiene contenido: algún elemento que no sea un `Fixed` (línea de la sección) | `La sección no tiene contenido: sin elementos (los Fixed no cuentan…) no tiene ninguna página` |
| `PAGE_NUMBER_UNAVAILABLE` | Un texto con `{page}` o `{pages}` no sale en una página sin número (`Page numbering: none`); línea del texto | `El texto de la caja pie tiene {page} o {pages} y sale en una página sin número…` |
| `VALUE_SYNTAX` | Sintaxis de los valores ([Valores](valores.md)), colores (`#RGB`, `#RGBA`, `#RRGGBB` o `#RRGGBBAA`), `linear(…)` mal escrito, más de cuatro colores en `Border color` o de cuatro valores en `Radius`, `Aspect ratio`, `For column` que no es un número | `Valor no válido: 12,5 (usa punto decimal…)` |
| `VALUE_NOT_ALLOWED` | Formas y signos que admite cada propiedad ([Valores](valores.md#tipos-de-valor)); `Gap` con 1 o 2 valores; `Size` > 0; `Count` ≥ 1; `Column width` y `Rule` > 0; `Letter spacing` en puntos; `Tab size` ≥ 1; `Title` y `Alt` no vacíos y `Language` una etiqueta BCP 47; `Bullet` con marcadores sin numerar y `Numbering` con numeraciones; `Levels` con dos niveles de 1 a 6, el primero no mayor que el último; `Rotate` entre −360 y 360, sin 0 ni las vueltas enteras; `Rotate origin` con una o dos palabras clave, como mucho una por eje; `For column` de menor a mayor; `Number label` con `{number}` una vez | `Width no admite 1fr: usa un número…` |
| `FIT_CONTENT_TRACK` | `fit-content` no es una pista de grid | `fit-content no es una pista de grid: usa auto…` |
| `DUPLICATE_ID` | Los `Id` son únicos | `Id repetido: a (ya se usa en la línea 6)` |
| `SINGLE_ELEMENT` | `Item` y `Area` contienen exactamente un elemento | `Un Area contiene exactamente un elemento, y este tiene Block (línea 8) y Text (línea 10)` |
| `TEXT_CONTENT` | Un `Text` tiene su texto en el valor o en `Content`, uno de los dos; un `Markdown` tiene contenido, y sus encabezados y párrafos, texto | `Text tiene el texto en el valor y en Content (línea 9): deja solo uno` |
| `TEXT_WITHOUT_FONT` | El texto, y un `Contents`, necesitan `Font` en `Typography`, propia o heredada | `El texto necesita Font: declárala en el Typography del Text, de un antecesor o de Styles (Element: Document)` |
| `TEXT_WITHOUT_FONT_SIZE` | El texto necesita `Size` en `Typography`, propio o heredado | `El texto necesita Size: decláralo en el Typography del Text, de un antecesor o de Styles (Element: Document)` |
| `STYLE_UNKNOWN_ELEMENT` | `Element` nombra un elemento de caja, un elemento generado o una posición en una tabla ([Styles](estilos.md)) | `Element: 'Item' no es un elemento de caja (Block, Flex…)` |
| `STYLE_DUPLICATE` | Dos reglas del mismo origen no tienen el mismo selector | `Regla repetida: Element: block (ya está en la línea 6)` |
| `STYLE_NOT_ALLOWED` | Una regla que llega a una `Section` solo da `Typography`; una regla (también de posición, de columna o de la clase de una `Row`) no da `Break` a un `Absolute`, un `Fixed`, un encabezado, un `Text` de un encabezado o una `Cell`; `Element: Document` solo admite `Typography`, y en ella no `Language` | `La regla For class: page aplica Break a un Absolute (línea 11)…` |
| `HEADING_LEVEL` | El primer encabezado es `Heading 1` y cada uno sube como mucho un nivel ([Encabezados](../componentes/heading.md#jerarquía)) | `Salto de nivel: Heading 3 después de Heading 1; como mucho, Heading 2` |
| `HEADING_IN_FIXED` | Un encabezado no va dentro de un `Fixed` | `Un Heading 1 no puede estar dentro de un Fixed…: usa un Text` |
| `BREAK_IN_HEADING` | Un `Text` de un encabezado no declara `Break` | `Un Text de un encabezado no admite Break: el motor ya evita cortar…` |
| `PARAGRAPH_BLANK_LINE` | Un `Paragraph` no tiene líneas en blanco en su `Content` (línea de la línea en blanco) | `Un Paragraph es un solo párrafo: quita la línea en blanco…` |
| `LIST_ITEM_EMPTY` | Un `List item` contiene al menos un elemento ([List](../componentes/list.md)) | `Un List item necesita al menos un elemento: un Paragraph, un Text, un Block…` |
| `LIST_NUMBER_NOT_ALLOWED` | `Start` y `Number` solo en las listas numeradas | `Start no tiene efecto con Marker: disc, que no numera los ítems` |
| `LIST_NUMBER_RANGE` | El número cabe en la numeración: `decimal` desde 0, letras desde 1, romanos de 1 a 3999 (línea del `Start`, del `Number` o del ítem) | `El número 4000 no se puede escribir con Marker: upper-roman, que numera de 1 a 3999` |
| `LIST_ITEM_WITHOUT_LINE` | Un ítem con marcador empieza con un texto con el que alinearlo (línea de la caja que lo impide) | `El marcador del List item de la línea 10 se alinea con su primera línea, y el ítem empieza con un Flex…` |
| `TABLE_ORDER` | `Head`, `Body` y `Foot` van en ese orden ([Table](../componentes/table.md)) | `Body va después de Foot: los grupos de una Table van en el orden Head, Body, Foot` |
| `TABLE_ROW_CELLS` | Cada fila llena exactamente las columnas de `Columns`, contando los `Span` de las filas de arriba (línea de la fila, o de la celda que sobra) | `La fila deja libre la columna 2 de 2 (Columns): añade una Cell, vacía si hace falta, o usa Span` |
| `TABLE_SPAN` | Un `Span` no se sale de la tabla, no pasa de un grupo a otro ni se solapa con otra celda | `La celda ocupa de la columna 1 a la 3, y la tabla tiene 2 (Columns)` |
| `TABLE_COLUMN_ALIGN` | `Column align` da una alineación por columna de `Columns` (línea del `Column align`) | `Column align da 2 alineaciones y la tabla tiene 3 columnas (Columns): escribe una por columna` |
| `TABLE_HEADER` | `Header: true` no se declara en una celda de `Head`, cuyas celdas ya son cabeceras de columna (línea del `Header`) | `Header no tiene efecto en una celda de Head: las celdas de la cabecera ya son cabeceras de columna` |
| `CELL_CONTENT` | Una `Cell` tiene texto en el valor o elementos, no las dos cosas | `Una Cell tiene texto en el valor (línea 12) o elementos, no las dos cosas: pon el texto en un Paragraph` |
| `TABLE_ROW_BREAK` | Las filas de `Head` y `Foot`, que se repiten en cada página, no declaran `Break` | `Las filas de Head se repiten en cada página de la tabla y no se cortan: quita el Break` |
| `MULTICOL_COLUMNS` | Un `Multicol` declara `Count`, `Column width` o los dos ([Multicol](../componentes/multicol.md)) | `Un Multicol necesita Count, Column width o los dos…` |
| `COLUMN_RULE_WIDTH` | `Rule` no es más ancha que `Column gap` | `Rule es más ancha que Column gap, que sin declarar es 0: la línea se pintaría sobre las columnas…` |
| `SINGLE_COLUMN` | Un `Multicol` con `Count: 1` no declara `Column gap` ni `Rule`, que separan columnas ([Multicol](../componentes/multicol.md)) | `Column gap no tiene efecto con Count: 1, porque no hay dos columnas que separar: quítalo` |
| `RULE_COLOR_WITHOUT_RULE` | `Rule color` va con `Rule` | `Rule color no tiene efecto sin Rule, que es la que pinta la línea entre columnas…` |
| `COLUMN_BREAK_OUTSIDE_MULTICOL` | `Before` y `After: column` solo dentro de un `Multicol` (línea del `Before` o el `After`, también de una regla) | `After: column solo tiene efecto dentro de un Multicol: usa page para cortar la página` |
| `POSITIONED_IN_MULTICOL` | Un `Absolute` o un `Fixed` no va dentro de un `Multicol` | `Un Fixed no puede estar dentro de un Multicol…` |
| `MARKDOWN_UNSUPPORTED` | El Markdown solo usa el subconjunto de [Markdown](../componentes/markdown.md#el-subconjunto), y una fila de tabla no tiene más celdas que la cabecera; un texto con `Inline: markdown` no tiene, dentro de la línea, imágenes, HTML ni saltos de línea forzados ([Formato del texto](formato-del-texto.md#formato-en-línea-inline)) (línea del bloque o del párrafo) | `Markdown no admite imágenes dentro de un texto (una imagen va sola en su párrafo): solo admite encabezados, párrafos, listas, tablas, bloques de código y…` |
| `MARKDOWN_PROPERTY_MISSING` | Un `Markdown` con listas con viñetas declara `Bullet`; con listas numeradas, `Numbering`; con tablas, `Table columns` (línea de la lista o la tabla) | `La lista necesita Bullet en su Markdown (línea 8): el marcador de cada nivel, como Bullet: disc dash` |
| `CODE_WITHOUT_FONT` | El código, en línea o en un bloque, necesita `Code font` en `Typography`, propia o heredada ([Formato del texto](formato-del-texto.md#código-en-línea)); línea del texto, del párrafo o del bloque | `El código necesita Code font: declárala en el Typography del texto, de un antecesor o de Styles (Element: Document)…` |
| `LINK_SCHEME` | El destino de un enlace es una URL `https://`, `http://` (con servidor) o `mailto:` (con destinatario) o `#id`; no está vacío ni mal escrito ([Enlaces](enlaces.md)) | `Link: 'javascript:alert(1)' no tiene un esquema admitido: solo https://, http://, mailto: y #id` |
| `LINK_UNKNOWN` | Un enlace `#id` lleva a una caja que existe y que no está en un `Fixed` ni dentro de uno | `El enlace #nada lleva a un Id que no existe: ninguna caja tiene el Id nada` |
| `IMAGE_UNKNOWN` | Una `Image` tiene ruta, y es la de un fichero de `images/` (línea de la `Image`) | `No existe el recurso logos/acme.png en …/resources/images` |
| `IMAGE_FORMAT` | Una `Image` es un PNG, un JPEG, un SVG del subconjunto admitido o un PDF sin cifrar, que se puede leer, y la página que dibuja de un PDF no tiene anotaciones que se vean ni fuentes sin incrustar o sin `ToUnicode`, salvo que quien convierte quite esas comprobaciones (línea de la `Image`) ([Image](../componentes/image.md#svg)) | `La imagen dibujo.gif no es PNG, JPEG, SVG ni PDF (es GIF): conviértela a uno de los cuatro` |
| `IMAGE_PAGE` | `Page` va en la `Image` de un PDF y es una página que el PDF tiene (línea de `Page`) ([Image](../componentes/image.md#pdf)) | `El PDF planos.pdf tiene 3 páginas y no tiene la 4` |
| `BREAK_IN_IMAGE` | Una `Image` no declara `Inside` ni `Decoration`, tampoco en una regla | `Inside no tiene efecto en una Image, que no se parte entre páginas: quítalo` |
| `CONTENTS_EMPTY` | Un `Contents` tiene algún encabezado en los niveles de `Levels` (línea del `Contents`) | `El índice no tiene ningún encabezado de los niveles 2 a 3 (Levels): no tendría entradas` |
| `REFERENCE_UNKNOWN` | Una referencia tiene `Id`, y es el de una caja del documento (línea de la referencia, [Referencias](referencias.md)) | `{page of: nada}: ninguna caja tiene el Id nada` |
| `REFERENCE_NOT_PLACEABLE` | La página de una caja, que no está en un `Fixed` ni dentro de uno ni en una página sin número | `{page of: pie}: pie está en un Fixed, que se repite en todas las páginas` |
| `REFERENCE_NOT_NUMBERED` | El número de una caja que lo tiene: un `List item` de una lista numerada o un encabezado con `Numbered: true` | `{number of: nota}: nota no tiene número (solo lo tienen los ítems de una lista numerada y los encabezados con Numbered: true)` |
| `REFERENCE_UNSTABLE` | La página de las cajas referidas se estabiliza en 10 pasadas de paginación | `La página de x no se estabiliza: tras 10 pasadas sigue cambiando…` |
| `CLASS_WITHOUT_RULE` | Cada clase de `Class` tiene su regla `For class` | `La clase aside no tiene regla: añade 'For class: aside' en Styles` |
| `OFFSET_CONFLICT` | Un `Relative` no se desplaza a la vez desde arriba y abajo, ni desde izquierda y derecha | `Un Relative no puede desplazarse a la vez desde arriba y desde abajo…` |
| `OVERCONSTRAINED` | Un `Absolute` o un `Fixed` con inicio, tamaño y fin necesita algún margen `auto` | `Izquierda, Width y derecha declarados fijan la posición dos veces…` |
| `MARGIN_AUTO_WITHOUT_EFFECT` | `Margin: auto` solo donde centra ([La caja](caja.md#márgenes-auto)) | `Margin auto no centra una caja que ocupa todo el ancho…` |
| `ISOLATE_IN_ITEM` | `Isolate: true` no va en un ítem de `Flex` o `Grid`, que ya corta el colapso de márgenes ([Block](../componentes/block.md#contextos-de-formato-de-bloque)) | `Isolate no tiene efecto en un ítem de Flex o Grid, que ya corta el colapso de márgenes con sus hijos: quítalo` |
| `START_WITHOUT_RESTART` | `Start` en una `Section` solo con `Page numbering: restart` ([Document](../componentes/document.md#numeración-de-las-páginas)) | `Start solo tiene efecto con Page numbering: restart…` |
| `ROW_BOX` | El `Box` de una `Row` solo da su aspecto: `Background`, `Border`, `Border color` y `Radius` ([Table](../componentes/table.md#filas)) | `Padding no tiene efecto en una Row, que no es una caja del layout…` |
| `PAGE_STYLE_WITHOUT_NUMBERS` | `Page style` no va en una `Section` con `Page numbering: none` | `Page style no tiene efecto con Page numbering: none…` |
| `BEGIN_ON_FIRST_SECTION` | `Begin on: odd` no va en la primera `Section`, que ya empieza en la página 1 ([Document](../componentes/document.md#empezar-en-página-impar)) | `Begin on: odd no tiene efecto en la primera sección…` |
| `LINES_WITHOUT_WRAP` | `Lines` solo con `Wrap: true` | `Lines solo tiene efecto con Wrap: true…` |
| `GAP_WITHOUT_WRAP` | Un `Gap` de dos valores en un `Flex`, solo con `Wrap: true` ([Flex](../componentes/flex.md#propiedades-del-flex)) | `Gap tiene dos valores, pero sin Wrap hay una sola línea y el primero, el hueco entre filas, no tiene efecto: escribe un solo valor…` |
| `GROW_WITHOUT_EFFECT` | `Grow` en `column` necesita un alto definido en el contenedor | `Grow no tiene efecto: el flex column del padre no tiene un alto definido…` |
| `ASPECT_RATIO_CONFLICT` | `Aspect ratio` no se combina con `Width` y `Height` declarados | `Aspect ratio no tiene efecto si Width y Height están declarados` |
| `MIN_MAX_CONFLICT` | Mínimo, máximo y tamaño comparables no se contradicen ([La caja](caja.md#mínimos-y-máximos)) | `Min width es mayor que Max width (línea 7)` |
| `INDEFINITE_PERCENTAGE` | Un porcentaje vertical necesita un alto definido ([Valores](valores.md#alto-definido)) | `Height usa un porcentaje de un alto que no es definido…` |
| `ROTATE_ORIGIN_WITHOUT_ROTATE` | `Rotate origin` solo con `Rotate` | `Rotate origin no tiene efecto sin Rotate: declara el giro o quítalo` |
| `BREAK_IN_ROTATED` | Un `Relative` girado o una caja girada en flujo no declara `Inside` ni `Decoration`, tampoco en una regla ([Capas y giro](capas-y-giro.md#giro-rotate)) | `Inside no tiene efecto en un Relative girado, que no se parte entre páginas: quítalo` |
| `BREAK_IN_UNBREAKABLE` | Un elemento dentro de un `Absolute`, un `Fixed`, un `Relative` girado o una caja girada en flujo, que no se parten, no escribe `Break` (tampoco la `Row` de una celda); lo que llega de una regla no se comprueba ([Cortes](cortes.md#cortes-forzados)) | `Before no tiene efecto dentro de un Absolute, que no se parte entre páginas: quítalo` |
| `ISOLATE_IN_ROTATED` | `Isolate: true` no va en una caja girada en flujo, que ya corta el colapso de márgenes ([Capas y giro](capas-y-giro.md#giro-en-flujo-block-text-paragraph-e-image)) | `Isolate no tiene efecto en una caja girada, que ya corta el colapso de márgenes con sus hijos: quítalo` |
| `ROTATED_ITEM` | Un `Block`, `Text`, `Paragraph` o `Image` girado no es un ítem de `Flex` o `Grid` ([Capas y giro](capas-y-giro.md#giro-en-flujo-block-text-paragraph-e-image)) | `Un ítem de Flex no puede girar: ocupa el sitio que le da su contenedor; gira una caja dentro del ítem…` |
| `GRID_PLACEMENT` | `Areas` rectangulares y con filas de igual longitud; valor de `Area` válido y con áreas existentes; un ítem con la fila fija cabe en ella ([Grid](../componentes/grid.md#colocación)) | `Área desconocida en Area: foot` |
| `FONT_UNKNOWN` | `Font` existe en la carpeta de fuentes (línea de la `Font`); también `Code font` (línea de la `Code font`) | `Fuente desconocida: Arial. Familias en …/resources/fonts: IBM Plex Mono, IBM Plex Sans, IBM Plex Serif` |
| `FONT_VARIANT_MISSING` | La familia tiene la variante de `Weight` y `Style`, y la de cada tramo con formato (línea del texto, o la del tramo) | `La fuente IBM Plex Sans no tiene la variante bold italic… (Font en la línea 6, …)` |
| `GLYPH_MISSING` | La fuente tiene todos los caracteres del texto (línea del carácter) | `La fuente IBM Plex Sans normal no tiene el carácter '漢' (U+6F22)` |
| `CONTRAST_LOW` | El texto tiene un contraste de 4,5:1 con lo que tiene debajo, o de 3:1 si es grande ([El PDF](pdf.md#contraste)); línea del `Color` del texto, o de su caja si no lo tiene | `El texto de la caja de la línea 12 tiene un contraste de 2,84:1 con su fondo (#999999 sobre #FFFFFF) y necesita 4,5:1…` |
| `TEXT_OVERFLOW` | Ninguna palabra es más ancha que su línea, ni una línea de un texto con `White space: pre` (línea de la palabra) | `La palabra 'supercalifragilística' no cabe en la línea: la línea mide 138.69 y la caja deja 100…` |
| `BOX_OVERFLOW` | Las cajas caben en su contenedor, el contenido en su alto (también en las columnas de un `Multicol`), y los `Absolute`, los `Fixed` y lo girado en la página | `El contenido de la caja de la línea 7 no cabe en su alto: su texto llega a 24 y la caja deja 20` |
| `LIST_MARKER_OVERFLOW` | El marcador de un ítem, con su espacio, cabe en el `Padding` izquierdo de la lista más el margen izquierdo del ítem (línea del ítem) | `El marcador '1.' del List item de la línea 12 mide 11.45 con su espacio, y la sangría deja 5…` |
| `TABLE_REPEAT_OVERFLOW` | La cabecera y el pie de una tabla dejan sitio al cuerpo en una página vacía (línea de la `Table`) | `La cabecera y el pie de la tabla de la línea 8 miden 900 y no dejan sitio al cuerpo en una página vacía…` |
| `PAGE_OVERFLOW` | Lo que no se puede partir cabe en una página | `La caja de la línea 9 no cabe en la página y no se puede partir: mide 900 de alto…` |
| `PAGE_LIMIT` | El documento no ocupa más páginas que el límite, 2000 por defecto ([Límites](limites.md); no es de ninguna línea) | `El documento tiene más de 2000 páginas, el máximo que admite el motor (Limits.maxPages lo cambia)` |
| `IMAGE_TOO_LARGE` | Un PNG o un JPEG no tiene más píxeles que el límite, 25 millones por defecto (línea de la `Image`) | `La imagen test/limit-huge.png tiene 6000 × 6000 píxeles (36,0 Mpx) y el máximo es 25,0 Mpx: reduce su resolución…` |
| `SVG_TOO_COMPLEX` | Un SVG no dibuja más elementos (contando lo que repite cada `<use>`) ni anida más niveles que el límite, 20 000 y 64 por defecto (línea de la `Image`) | `El SVG test/limit-depth.svg anida 72 niveles y el máximo es 64: aplánalo…` |
| `PDF_TOO_LARGE` | Un PDF no ocupa más bytes que el límite, 20 MB por defecto (línea de la `Image`) | `El PDF planos.pdf ocupa 31,4 MB y el máximo es 20,0 MB: usa un PDF más pequeño…` |

[`test/errors/`](../../../test/errors) tiene un documento de ejemplo por cada código, con la línea que debe dar
([Pruebas](../implementacion/pruebas.md#catálogo-de-errores-errorcatalogtest)).

## Tipos de error

Cada error dice, además de su código y su línea, **de quién es la culpa**, que es lo que un servicio necesita para
contestar:

| Tipo | Qué es | Un servicio contesta |
|---|---|---|
| `INVALID_DOCUMENT` | El documento o sus estilos no son válidos: sintaxis, schema, una regla del modelo, un desbordamiento, un recurso que el documento nombra y no existe | un error del cliente (4xx), con la línea |
| `RESOURCE` | La carpeta de recursos no es utilizable (no existe, una fuente o una imagen estropeadas) o no se puede leer el fichero del documento | un fallo de su configuración (5xx) |
| `LIMIT` | El documento es válido pero pide más de lo que se admite ([Límites](limites.md)): más páginas, una imagen con demasiados píxeles, un SVG con demasiados elementos o niveles, un PDF demasiado grande, o un texto STXT demasiado grande o anidado | un error del cliente por exceso (413 o 422) |
| `INTERNAL` | Un fallo del motor, no del documento ni del entorno | un fallo del servidor (500), que conviene informar |

El código es el de la tabla de arriba, el de STXT para los errores de sintaxis y de schema (`CHILD_NOT_DECLARED`),
`RESOURCE`, `IMAGE_FORMAT` o `INTERNAL`. La línea es `0` si el error no es de una línea (un recurso, un límite o un fallo
interno). Si es del [documento de estilos aparte](estilos.md#documento-de-estilos-aparte), el mensaje lo dice (`línea 8
de los estilos: …`). Cómo se ven desde Java está en [Uso desde Java](../implementacion/api.md#errores).

> **Decisión:** un fallo que el motor debería haber evitado (una página que no avanza, un enlace sin destino) es
> `INTERNAL`: es un defecto del motor, no del documento.
