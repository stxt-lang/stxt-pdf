# CLAUDE.md

Guía de referencia para trabajar en este repositorio.

**Idioma:** la documentación (`README.md`, `docs/`, este fichero), los comentarios, el javadoc
y los mensajes de error están en **castellano**. Los **términos técnicos están en inglés**:
identificadores Java (packages, clases, métodos, campos, variables, tests) y nombres de nodos y
valores STXT. El contenido de `Text` en los documentos puede estar en cualquier idioma.

## Qué es

Motor de layout de cajas inspirado en CSS cuyos documentos se escriben en
[STXT](https://stxt.dev). Los documentos se maquetan siempre en **páginas** de tamaño fijo
(como un PDF), definidas por `Media` (`Width` y `Height`). Todas las medidas están en
**puntos tipográficos** (1 pt = 1/72 in; A4 = 595 × 842).

- **Especificación (fuente de verdad del comportamiento):** [`docs/ESPECIFICACION.md`](docs/ESPECIFICACION.md)
- **Estado:** [`README.md`](README.md)
- **Hoja de ruta de los hitos 16 a 18 (Markdown e imágenes):** [`docs/ROADMAP.md`](docs/ROADMAP.md).
  Léela al empezar una sesión (estado, decisiones abiertas y registro de sesiones) y
  actualízala al terminar. Las de los hitos 11 a 15 y de calidad y arquitectura están
  terminadas y solo quedan en el historial de git.
- **Schema STXT** (`@stxt.schema`): [`src/main/resources/stxt/dev.stxt.boxes.stxt`](src/main/resources/stxt/dev.stxt.boxes.stxt)
  (si difiere del resumen del §9 de la especificación, manda el fichero)
- **Documento de ejemplo:** [`test/examples/report.stxt`](test/examples/report.stxt)

## Comandos

Java 17 y Maven (`dev.stxt:stxt-boxes`, package raíz `dev.stxt.boxes`).

```bash
mvn test                                   # compila, ejecuta todos los tests y deja la cobertura
                                           # (JaCoCo) en target/site/jacoco/index.html
mvn test -Dtest=ValueParserTest            # una clase
mvn test -Dtest='TrackSizingTest#nombre'   # un método
```

Eclipse: el repositorio incluye `.project`, `.classpath` y `.settings/` (proyecto `stxt-boxes`,
naturaleza Java + m2e, Java 17, UTF-8). Si cambian las dependencias: *Maven → Update Project*.

## Estado

Los algoritmos de layout funcionan, con paginación y salida SVG y PDF; los hitos 1
a 18 están terminados. La tabla de estado del README es la referencia; al terminar un hito,
actualízala.

| Hito | Trabajo | Dónde |
|---|---|---|
| 1 | ~~Conectar con `dev.stxt:stxt-core` y validar contra el template~~ (hecho) | `stxt/DocumentReader` |
| 2 | ~~Flujo de bloque y texto; activar `GoldenTest` (caso 01)~~ (hecho) | `layout/block/BlockLayout`, `layout/inline/InlineLayout` |
| 3 | ~~`relative`, `absolute`, `fixed` (esta en el `Paginator`)~~ (hecho; caso 03) | `layout/position`, `layout/Paginator` |
| 4 | ~~`intrinsic()` en todos los algoritmos; flex (caso 02)~~ (hecho) | `layout/flex/FlexLayout` sobre `FlexResolver` |
| 5 | ~~Grid~~ (hecho; casos 04 y 05) | `layout/grid/GridLayout` sobre `Placement` y `TrackSizing` |
| 6 | ~~`BreakToken` en los algoritmos, PDF con PDFBox, métricas reales~~ (hecho; caso 06) | `layout/Slice`, `layout/UnitBreaker`, `fragment/BreakRules`, `paint/PdfCanvas`, `layout/inline/PdfMetrics` |
| 7 | ~~`{page}`/`{pages}`, `Decoration: clone`, corte en paralelo, Knuth-Plass~~ (hecho; caso 07) | `layout/Slice`, `layout/UnitBreaker`, `layout/inline/PageNumbers`, `layout/inline/LineBreaker` |
| 8 | ~~Carpeta de recursos, fuentes TTF incrustadas, `Font weight`/`Font style`, línea base real~~ (hecho) | `resources`, `layout/inline/PdfMetrics`, `paint/PdfCanvas` |
| 9 | ~~Elementos (`Block`, `Flex`, `Grid`, `Text`, `Relative`, `Absolute`, `Fixed`, `Item`, `Cell` (hoy `Area`)) en lugar de `Box`, validados con `@stxt.schema`~~ (hecho) | schema, `stxt/DocumentReader`, `tree/BoxTreeBuilder` |
| 10 | ~~Grupos `Box`, `Typography` y `Break`, con nombres cortos~~ (hecho) | schema, `tree/BoxTreeBuilder` |
| 11 | ~~Reglas reutilizables `Styles` (`Element`, `For class`, `For id`) y `Class`~~ (hecho; caso 08) | schema, `tree/BoxTreeBuilder` |
| 12 | ~~Encabezados (`Heading 1`…`Heading 6`) y `Paragraph`; PDF etiquetado y marcadores~~ (hecho; caso 09) | schema, `tree/BoxTreeBuilder`, `paint/PdfStructure` |
| 13 | ~~Listas (`List`, `List item`) con marcador y numeración~~ (hecho; caso 10) | schema, `tree/BoxTreeBuilder`, `tree/ListMarkers`, `layout/block/MarkerLayout`, `paint/PdfStructure` |
| 14 | ~~Tablas (`Table`, `Head`, `Body`, `Foot`, `Row`, `Cell`) con cabecera y pie repetidos; `Area` en lugar del `Cell` de `Grid`~~ (hecho; caso 11) | schema, `tree/BoxTreeBuilder`, `layout/table/TableLayout`, `layout/UnitBreaker`, `paint/PdfStructure` |
| 15 | ~~Bloque en columnas (`Multicol`), equilibrado y cortes de columna~~ (hecho; caso 12) | schema, `tree/BoxTreeBuilder`, `layout/multicol/MulticolLayout`, `fragment/BreakRules`, `paint/StackingContext` |
| 16 | ~~Tramos con formato dentro del texto (negrita, cursiva, subrayado, tachado)~~ (hecho; caso 13) | `model/TextRun`, `layout/inline/LineBreaker`, `fragment/TextLine`, `paint/StackingContext`, `layout/inline/PdfMetrics` |
| 17 | ~~Elemento `Markdown`, traducido a elementos conocidos~~ (hecho; caso 13) | schema, `tree/MarkdownBuilder`, `tree/BoxTreeBuilder` |
| 18 | ~~Imágenes (`Image`): PNG y JPEG de la carpeta de recursos~~ (hecho; caso 14) | schema, `resources/ImageLibrary`, `layout/image/ImageLayout`, `layout/BoxModel`, `paint/PdfCanvas`, `paint/PdfStructure`, `tree/MarkdownBuilder` |

Si se abre un hito nuevo, apúntalo en [`docs/ROADMAP.md`](docs/ROADMAP.md) y en esta tabla.

No queda ningún `// TODO(hito N)` en el código. Si se abre un hito nuevo, marca lo pendiente
así y lanza `UnsupportedOperationException` con el hito en el mensaje; el javadoc de cada clase
pendiente describe el algoritmo a implementar (por ejemplo, el pseudocódigo de `BlockLayout`).

## Arquitectura

Flujo de datos:

```
texto STXT ──DocumentReader──▶ InlineNode (stxt-core) ──BoxTreeBuilder──▶ Box (árbol) + Media
   ──Engine.layOutDocument──▶ Paginator ──▶ Overflow ──▶ LaidOutDocument (Media, Metadata, List<Page>)
   ──▶ StxtDump (tests) · StackingContext + Canvas (SvgCanvas / PdfCanvas)
```

| Package | Contenido |
|---|---|
| `model` | Tipos puros: `Value` (valor *especificado*, sealed), `ValueParser`, `Sides`, `Insets`, `Rect`, `VerticalMetrics`, `DecorationMetrics`, `FontFace` (con `FontWeight` y `FontStyle`), `TextRun` y `TextFormat` (tramos con formato), `GridAreas`, `Area` y `Cell` (la colocación de grid ya interpretada), `Tolerance` (la épsilon de redondeo) |
| `stxt` | `DocumentReader`: parsea con `stxt-core`, valida contra el schema y exige una única raíz `Document` con el namespace `dev.stxt.boxes` |
| `resources` | `Resources` (carpeta de recursos y rutas seguras), `FontLibrary` (índice de `fonts/`), `FontFile` (un TTF con sus métricas), `ImageLibrary` e `ImageFile` (las imágenes de `images/`, con su formato y su tamaño), `ResourceException` e `ImageFormatException` |
| `tree` | `Box`, `Element` (los elementos de caja), `Style` (propiedades y enums), `Media`, `DocumentSchema` (el schema compilado), `InvalidModelException`; `BoxTreeBuilder` (documento → árbol) y sus piezas: `ValueReader`, `PropertyReader`, `StyleRules`, `TableBuilder`, `ListBuilder`, `MarkdownBuilder`, `ModelValidator`, `BuildContext`, `Nodes`; `PropertyKey` (las claves de `Style.line`) |
| `layout` | `Engine`, `LayoutAlgorithm`, `ConstraintSpace`, `LayoutResult`, `LayoutCache`, `IntrinsicSizes`, `BoxModel`, `MarginStrut`, `AdjoiningMargins`, `Sizing`, `PendingAbsolute`, `Slice`, `UnitBreaker`, `Paginator`, `LaidOutDocument`, `Messages` (piezas de los mensajes de error) |
| `layout.block` / `inline` / `flex` / `grid` / `table` / `multicol` / `position` / `image` | Un algoritmo por `Layout` y sus piezas puras (`LineBreaker`, `FlexResolver`, `TrackSizing`, `Placement`, `Columns`, `AbsoluteEquation`…) |
| `fragment` | `Fragment`, `BreakToken`, `BreakAppeal` (calidades de corte), `Unit` (unidad de corte de flex y grid), `Page`, `BreakRules`, `TextLine` |
| `paint` | `Canvas`, `StackingContext`, `SvgCanvas`, `PdfCanvas` (PDFBox), `PdfStructure` (PDF etiquetado y marcadores), `PdfWriter` (documento → PDF en una llamada), `ImageFit` (dónde se dibuja una imagen según `Fit`) |
| `output` | `StxtDump` (formato de los tests de referencia, §7) |

Las dependencias entre packages van en un solo sentido: `model` no depende de nada, `tree` solo
de `model`, y no hay ciclos (`ArchitectureTest`, que cuenta cada package con sus subpackages).
Un tipo que necesitan `tree` y `layout` va en `model`.

### Piezas clave

- **Nodos de `stxt-core`:** el motor trabaja directamente con `dev.stxt.Node`, `InlineNode`
  (valor inline e hijos) y `TextNode` (bloque `>>`); `getText()` da el contenido en los dos
  casos. Los nombres se comparan por `getCanonicalName()`: minúsculas y los espacios convertidos
  en guiones (`Min width` → `min-width`). Los errores de sintaxis y del schema son
  `ParseException` de la librería, con código (`INVALID_VALUE`, `CHILD_NOT_DECLARED`…) y línea.
- **Elementos y schema (§2, §9):** el documento se escribe con elementos que solo admiten sus
  propiedades: `Block`, `Flex`, `Grid`, `Text` (texto en el valor o en `Content >>`),
  `Relative`, `Absolute` y `Fixed` (bloques posicionados), y los envoltorios `Item` (hijo de
  `Flex`) y `Area` (hijo de `Grid`, con la colocación en el valor), que contienen exactamente un
  elemento. Las propiedades de cada caja van en tres grupos (§4): `Box` (modelo de caja, no se
  hereda), `Typography` (texto, viudas y huérfanas; se hereda y se admite en `Document`) y
  `Break` (cortes de página; no en `Absolute` ni `Fixed`), con nombres cortos (`Size`,
  `Weight`, `Align`, `Before`…). La estructura solo está en el schema: `DocumentSchema` lo
  compila una vez, `DocumentReader` valida con él al leer y `BoxTreeBuilder` vuelve a validar el
  árbol al empezar, así que el resto del builder da la estructura por buena.
  `PropertyKey.internal` da el nombre interno de los cortos (`Size` → `font-size`), que es la
  clave de `Style.line`.
- **`BoxTreeBuilder`** traduce cada elemento a una `Box` (`Style.layout` y `Style.position` salen
  del elemento; las propiedades de `Item` y `Area` pasan al elemento que envuelven), interpreta
  valores, aplica la herencia y comprueba las reglas del §6 que el schema no puede expresar. Los
  errores son `InvalidModelException` con un **código** (`ErrorCode`) y la **línea del nodo
  culpable**: la de la propiedad (`Style.line(HEIGHT)`, `GRID_ROWS`, `ITEM_GROW`…, con las
  claves de `PropertyKey`), no la de su caja. La raíz que devuelve es una `Box` anónima que representa el área útil.
  `BoxTreeBuilder` es el orquestador (elementos, encabezados y texto) y no guarda estado: lo de
  cada llamada va en un `BuildContext`. Los valores los interpreta `ValueReader`; los grupos y
  las propiedades de `Flex`, `Grid` e `Item`, `PropertyReader`; las reglas de `Styles`,
  `StyleRules`; tablas y listas, `TableBuilder` y `ListBuilder`; y las reglas del §6 que miran
  la caja ya leída, `ModelValidator`. `Nodes` tiene el acceso a los nodos y los errores.
- **Reglas reutilizables (`Styles`, §4.10):** `Element`, `For class` y `For id` con `Box`,
  `Typography` y `Break`. `StyleRules.apply` las aplica entre la herencia y lo propio
  del elemento (prioridad: elemento > id > clases en orden de `Styles` > tipo > herencia), con
  las mismas funciones que leen los grupos del elemento: cada propiedad guarda la línea de la
  regla y se valida como si la declarara el elemento.
- **Elementos con significado (§5.13):** `Paragraph` (un `Text` de un solo párrafo) y
  `Heading 1`…`Heading 6` (un bloque con hijos `Text`). `Box.element` guarda el `Element`
  del que procede (`HEADING_2`) para las reglas, los marcadores y las etiquetas; el enum da
  también su `Layout`, su `Position` y si puede ser un ítem, y `ElementTest` lo compara con el
  schema. El builder fija `After: avoid` e `Inside: avoid` en los encabezados y comprueba su
  jerarquía en orden del documento. `PdfStructure` construye el árbol de estructura del PDF etiquetado
  (contenido marcado con `Canvas.beginText`/`endText`; fondos, bordes y `Fixed` son
  *artifacts*) y los marcadores.
- **Listas (§5.14):** `List` (con `Marker` obligatorio y `Start`) y `List item` (con `Number`)
  son cajas de bloque. El builder numera los ítems y crea en `Box.marker` la caja del marcador,
  generada y fuera de `children` (`Element.MARKER`, `Box.isMarker()`): texto con la tipografía
  del ítem y el espacio final incluido (`"3. "`). `MarkerLayout` la coloca en el primer fragmento del ítem,
  como su primer hijo, a la izquierda de su caja de borde y con la línea base de su primera
  línea (`TextMeasurer.vertical`). Quien recorra los hijos de un fragmento debe saltar los
  marcadores si busca contenido en flujo; `Overflow` los comprueba contra la sangría de la
  lista (`LIST_MARKER_OVERFLOW`).
- **Tablas (§5.15):** el builder traduce una `Table` a una caja `GRID` (`Box.element` =
  `TABLE`) cuyos ítems son sus `Cell`, con la colocación explícita en `Style.itemArea` (fila y
  columna por orden y `Span`); `Head`, `Body`, `Foot` y `Row` no son cajas. `Box.headRows` y
  `Box.footRows` dicen qué filas de la rejilla son cabecera y pie. El `Typography` de la `Row`
  se aplica a sus celdas como herencia y su `Break`, como el de las celdas. `Engine` despacha
  las tablas a `TableLayout`, que maqueta el grid sin fragmentar (`GridLayout.arrange`) y corta
  solo el cuerpo con `UnitBreaker`, repitiendo enteros la cabecera y el pie
  (`UnitBreaker.Repeat`). `PdfStructure` las etiqueta (`Table`, `THead`/`TBody`/`TFoot`, `TR`,
  `TH`/`TD`); las filas repetidas son contenido solo en su primera página.
- **Columnas (§5.16):** el builder mete el contenido de un `Multicol` en el **flujo de
  columnas**, una caja de bloque generada (`Element.COLUMNS`, fuera del documento como el
  marcador) que es su único hijo: crea un contexto de formato de bloque y su alto no es
  definido. `MulticolLayout` maqueta ese flujo una vez por columna con `engine.layout`, con el
  alto de la columna como `fragmentainerRemaining` y el token de la columna anterior como
  `from`; cada columna es un fragmento suyo, hijo del fragmento del `Multicol`. Llena en orden
  y, si el contenido termina y `Fill` es `balance`, busca el alto mínimo (búsqueda binaria) sin
  cortes entre columnas peores que los del reparto en orden. Los cortes forzados son `PAGE` o
  `COLUMN` (`BreakRules.forcedBetween` devuelve el tipo): el de columna es un corte perfecto
  dentro del flujo; el de página tiene la calidad `BreakAppeal.FORCED_PAGE`, por encima de
  `PERFECT_BREAK`, para que el `Multicol` deje de llenar columnas y lo pase al padre. Quien
  recorra los hijos de un `Multicol` (o sus fragmentos) encuentra el flujo, no el contenido.
- **Política de errores (§6):** más estricta que CSS. Lo que no tendría efecto o se contradice
  es un error, nunca un valor por defecto silencioso; se para en el primer error. Los
  porcentajes verticales necesitan un alto definido, que se decide al leer (§3.6,
  `ModelValidator.definiteHeight`). Los desbordamientos (§5.12) son errores: `Overflow` los
  busca en el resultado ya paginado y señala al culpable más interior. `Font` y `Size` no
  tienen valor por defecto y se suelen declarar en el `Typography` de `Document`. No
  generalices una regla nueva sin comentarlo con el usuario.
- **Enums de `Style`** (y `FontWeight` y `FontStyle`, que están en `model`): los valores STXT se convierten con `Enum.valueOf` sobre el texto en
  mayúsculas y con el guion como guion bajo, así que **cada constante debe llamarse igual que
  su valor en el documento** (`TextAlign.JUSTIFY` ↔ `justify`, `Marker.LOWER_ALPHA` ↔
  `lower-alpha`). Si cambias un valor, cambia la constante y el schema.
  `Layout` y `Position` no son valores del documento: los fija el elemento.
- **`Engine`** despacha por `Style.layout` al algoritmo correspondiente. **Toda llamada
  recursiva de layout o de tamaños intrínsecos pasa por `engine.layout(...)` /
  `engine.intrinsic(...)`**, nunca directamente al algoritmo, para usar la `LayoutCache` (sin
  ella, flex y grid anidados crecen de forma exponencial).
- **`LayoutAlgorithm`**: `contentIntrinsic(box)` (min/max-content del contenido, sin borde,
  relleno, margen ni `Width`; independiente del espacio) y `layout(box, space, from)`. Los de la
  caja de margen los da `engine.intrinsic(box)` con `BoxModel.intrinsic`; los del contenido,
  `engine.contentIntrinsic(box)` (el mínimo automático de un ítem flex).
- **`ConstraintSpace`** es la entrada inmutable del layout y parte de la clave de la caché:
  ancho disponible, alto (vacío = indefinido), tamaños impuestos por el padre, bases de
  porcentajes, espacio restante en la página desde el borde superior de la caja (vacío = sin
  fragmentación), si la caja empieza la página y si se mide el alto de su contenido ignorando
  su `Height` (`measuringContentHeight()`).
- **`LayoutResult`**: el `Fragment`, el `BreakToken` (`null` = la caja ha terminado), la calidad
  del corte (`BreakAppeal.PERFECT_BREAK` … `FORCED`), las `PendingAbsolute` que suben hasta su
  containing block (con la posición estática relativa a la caja de contenido de quien devuelve
  el resultado) y los `AdjoiningMargins`.
- **`BoxModel`**: margen, borde, relleno, ancho de la caja de borde y alto si es definido, a
  partir del `ConstraintSpace`. Es la parte común de todos los algoritmos; `childSpace()` da el
  espacio de los hijos en flujo y `BoxModel.intrinsic(...)` pasa de tamaños intrínsecos del
  contenido a los de la caja de margen. Los márgenes `auto` valen 0: el centrado lo hace el
  padre, que es quien coloca al hijo.
- **Colapso de márgenes:** cada caja devuelve en `AdjoiningMargins` los márgenes de sus
  descendientes que salen por arriba o por abajo (y si está vacía y deja pasar los suyos); el
  padre los colapsa con los márgenes propios del hijo, que resuelve él. `MarginStrut` es
  inmutable y guarda el mayor positivo y el más negativo por separado.
  `Box.establishesBlockFormattingContext()` dice qué cajas lo cortan.
- **Cajas posicionadas:** los algoritmos no maquetan los hijos `absolute`/`fixed`; los
  devuelven como `PendingAbsolute` con su posición estática, y los hijos `relative` se
  desplazan con `RelativeOffset` después de colocarlos. `Engine.layout` resuelve con
  `AbsoluteLayout` las `absolute` cuyo containing block es la caja recién maquetada (sus
  fragmentos cuelgan del de esa caja); las `fixed` suben hasta la raíz y el `Paginator` las
  coloca en `Page.fixed` de cada página.
- **`BreakToken`** (sealed, records con `equals`): `Block`, `Line`, `Container`, con el alto ya
  consumido por la caja (un `Height` definido es el total de todos los fragmentos), y `BEFORE`:
  "no cabe nada, corta antes de mí", que nunca se pasa como `from`. El `Paginator` detecta la
  falta de progreso comparando tokens. El token de la raíz no acumula alto: cada página tiene
  el área útil entera.
- **Fragmentación (§5.10):** cada algoritmo coloca lo que cabe en `fragmentainerRemaining` y
  devuelve el corte con su calidad; el padre lo compara con los cortes entre sus hijos y se
  queda con el **último de mayor calidad** (`BreakRules.best`). Relajar las reglas en el orden
  de la especificación sale solo de esa comparación: por eso un hijo que no puede respetar
  viudas y huérfanas devuelve igualmente un corte (`VIOLATES_WIDOWS`) en lugar de `BEFORE`.
  `Slice` calcula alto, borde y relleno de cada fragmento (`Decoration: slice` y `clone`; usa
  `Slice.contentRemaining` para lo que queda al contenido). Flex y grid cortan entre unidades
  (líneas, ítems o filas; `fragment.Unit`) con `UnitBreaker`: se maquetan enteros sin
  fragmentar (`UnitBreaker.Arranged`: ítems colocados y `Units`) y `UnitBreaker.fragment` se
  queda con lo que cabe en la página. Una unidad que no cabe ni al principio de página
  parte sus ítems en paralelo, repitiendo su layout con `ConstraintSpace.fragmented` y el
  espacio guardado en `UnitBreaker.Placed`, y el `BreakToken.Container` lleva el token de cada
  ítem. Con `atFragmentainerStart` hay que colocar algo aunque desborde (garantía de progreso).
- **Texto largo:** cada página maqueta el texto desde otra línea, así que la `LayoutCache` no
  ayuda; `InlineLayout` memoriza el reparto en líneas por caja y ancho disponible (si no, el
  coste crece con el cuadrado de las páginas; lo vigila `LongTextTest`). Se vacía en
  `reservePageDigits`, como la caché.
- **Numeración (`{page}`/`{pages}`):** `InlineLayout` mide los marcadores como `00` y las
  líneas los conservan; al terminar de paginar, el `Paginator` llama a `Engine.number`, que
  rehace solo esas líneas con los valores de cada página (`PageNumbers`).
- **`Fragment`**: posición relativa a la caja de **contenido** del padre; `StxtDump` la pasa
  a coordenadas absolutas de página. Lleva su propio `border` y `padding`, que en un fragmento
  partido no son los de la caja: usa siempre los del fragmento para llegar a su caja de contenido.
  Lleva también el `margin` usado (con la base de porcentajes de su layout) y el desplazamiento
  de `Relative` ya sumado a su posición (`offsetX`/`offsetY`), que `Overflow` necesita. La geometría solo existe en los fragmentos: `Box` no guarda posición ni tamaño.
- **`TextMeasurer`**: `FixedMetrics` (cada carácter = `0.5 × tamaño`) es el de `new Engine()` y
  el de todos los tests de layout, y no necesita fuentes; `PdfMetrics` da las métricas reales de
  las fuentes TTF de `Resources` y es el que usa `PdfWriter.write(document, out)`.
  `Engine.layOutDocument` llama antes a `validate(root)`, que en `PdfMetrics` comprueba que
  existen las fuentes y tienen todos los caracteres (errores con línea).
- **Recursos (§5.11):** carpeta fija, por defecto `resources/` del directorio de trabajo, con
  `fonts/` e `images/`; `Resources` solo sirve para cambiarla. Toda ruta pasa por
  `Resources.resolve`, que **nunca va hacia atrás** (rechaza absolutas, `..` y enlaces que
  salen). Las fuentes se identifican por familia, peso (400/700) y estilo leídos del TTF
  (`FontLibrary`); no hay fuente por defecto ni de sustitución: `Font` es obligatoria en el
  texto. El repositorio incluye IBM Plex Sans (OFL).
- **Pintado del texto:** `Canvas.text` recibe un `FontFace`; la línea base la calcula
  `StackingContext` con `Canvas.vertical(font)` (`hhea` en PDF, la aproximación 0,8/0,2 en
  SVG). En PDF las fuentes son Type 0 Identity-H, así que `justify` usa `TJ`, no `Tw`.
- **Tramos (§5.4):** `Box.text` es siempre el texto entero y `Box.runs` (`null` sin formato) lo
  parte en `TextRun` con un `TextFormat` (negrita, cursiva, subrayado, tachado, que se suman a
  la fuente de la caja con `TextFormat.face`); usa `Box.runs()`, que da un único tramo si no
  hay formato. `LineBreaker` reparte palabras que pueden tener partes de varios tramos
  (`LineBreaker.Word`, `LineBreaker.Line`) y `TextLine.runs` guarda cada tramo colocado, con el
  espaciado de `justify` incluido. `StackingContext` pinta cada tramo con su fuente y las rayas
  con `Canvas.decoration(font)` (`post` y `OS/2` en PDF). Un texto sin formato debe medirse y
  pintarse exactamente igual que una cadena: los PNG golden lo vigilan. El documento solo
  escribe tramos con `Markdown`; los tests de layout los ponen en el árbol con `TextRuns` y
  `Engine.layOutDocument(BoxTreeBuilder.Result)` (`LayoutFixture.layOutRuns`).
- **Markdown (§5.17):** `MarkdownBuilder` parsea el `Content` con commonmark-java y lo traduce
  a nodos STXT (`InlineNode` con la línea de su bloque: `Heading N` con un `Text`, `Paragraph`,
  `List` con `Marker` de `Bullet`/`Numbering`, `Table` con `Columns` de `Table columns`), que
  valida con el schema; los tramos de cada texto van aparte, por nodo
  (`Translation.runs`, en `BuildContext.runs`). `BoxTreeBuilder.markdown` construye esos nodos
  con `element(...)` como hijos de la caja del `Markdown` (un bloque, `Element.MARKDOWN`), y el
  texto de cada uno recibe sus tramos. Lo que no está en el subconjunto es
  `MARKDOWN_UNSUPPORTED`: amplíalo solo traduciendo a elementos que ya existen. Una imagen
  sola en su párrafo (`![Alt](ruta)`) es una `Image`.
- **Imágenes (§5.18):** `Image` es una caja sin hijos (`Element.IMAGE`, `Layout.IMAGE`) con la
  ruta, relativa a `images/`, en `Box.image`, y `Alt` y `Fit` en `Style`. `tree` no lee
  ficheros: `Engine` tiene las imágenes de una carpeta de recursos (`layout/image/Images`, la
  por defecto con `new Engine()`, también con `FixedMetrics`), las comprueba antes de maquetar
  (`IMAGE_UNKNOWN`, `IMAGE_FORMAT`, con la línea de la `Image`) y da su tamaño natural
  (`Engine.natural`, a 96 ppp). El dimensionado de elemento reemplazado está en
  `BoxModel.replaced`, para que las sondas de flex, grid y absolute midan bien la imagen: usa
  siempre `BoxModel.resolve(box, space, engine)`, que le pasa el tamaño natural. La proporción
  es la de la caja de contenido y el alto sale siempre de ella (es definido). `ImageLayout` solo
  coloca y no parte: si no cabe, `BEFORE`. `StackingContext` la pinta con `Canvas.image` en el
  paso del texto; `PdfCanvas` incrusta cada fichero una vez y `PdfStructure` le da un `Figure`
  si tiene `Alt` (si no, es un *artifact*). Las imágenes de los tests están en
  `resources/images/test/` (`wide.png` 150 × 75 pt, `tall.jpg` 90 × 120, `square.png` 30 × 30,
  y `drawing.gif` y `broken.png` para los errores).

## Convenciones

- **Nombres STXT:** en inglés y, cuando existe, con el término de CSS (`Margin`, `Padding`,
  `Absolute`, `Decoration: slice`). Dentro de un grupo, el nombre corto (`Size`, `Before`) si
  no choca con otro nodo del namespace. Las propiedades de varias palabras van separadas por
  espacio y con mayúscula solo la primera (`Line height`, `Min width`, `Border color`). En el código se comparan en forma canónica (minúsculas).
- **Términos del dominio en castellano en la documentación** ("área útil", "caja de borde",
  "viudas"); en el código, su equivalente inglés (`pageArea`, `borderBox`, `widows`).
- **Valores especificados frente a usados:** `Value` conserva lo que escribió el autor (flex y
  grid necesitan saber si era `auto`); se convierte a puntos con `resolve(...)` durante el
  layout.
- **Geometría:** posiciones y tamaños se refieren a la **caja de borde** salvo que se diga otra
  cosa; `Width` y `Height` la miden siempre (no hay `Box sizing`). El ancho baja de padres a
  hijos y el alto sube de hijos a padres.
- **Inmutabilidad:** las entradas y salidas del layout son `record` con listas copiadas
  (`List.copyOf`). `Box` es mutable solo mientras `BoxTreeBuilder` construye el árbol; el layout no la
  escribe nunca. Su igualdad es por identidad (clave de la caché).
- **Comparar medidas:** con `Tolerance.fits(size, space)` y `Tolerance.same(a, b)` (o
  `Tolerance.EPSILON`), nunca con un `1e-9` propio. `Overflow` usa su propia tolerancia, más
  ancha (`TOLERANCE`, 0,01 pt).
- **Min y max:** primero se aplica el máximo y luego el mínimo; si chocan, gana el mínimo
  (`Sizing.constrain`).
- **Comentarios y javadoc** en castellano, con referencias a la especificación del tipo
  `ESPECIFICACION.md §5.8`. Las referencias "módulo N" que aparecen en algunos javadoc remiten a
  un documento de diseño que no está en el repositorio; no las uses para trabajo nuevo.
- Indentación de 4 espacios en Java; tabuladores en los ficheros `.stxt`.

## Tests

- JUnit 5. Un test por pieza pura, en el mismo package que la clase (`src/test/java/dev/stxt/boxes/...`).
  Los nombres de los métodos de test están en inglés y en camelCase (`autoMarginsCenter`).
- Los documentos de test se leen con `DocumentReader`, que valida contra el schema: un test
  de una regla del §6 comprueba así que el schema no la detecta antes. Los volcados de §7
  (`expected.stxt`) no tienen namespace y se leen con `STXT.rawParser()`.
  `DocumentReaderTest` comprueba que el ejemplo y las entradas de los casos golden validan
  (añade ahí los documentos nuevos) y que los ejemplos de la especificación se maquetan sin
  errores.
- **Medidas de rendimiento:** los tests con `@Tag("perf")` no van en `mvn test` (el `pom.xml`
  excluye el grupo); se ejecutan con `mvn test -Dgroups=perf -DexcludedGroups=none -Dtest=…`.
- **Catálogo de errores** (`errors/ErrorCatalogTest`): `test/errors/<grupo>/<código>.<descripción>.stxt`
  es un documento STXT de verdad, un fichero por caso, en la carpeta de su familia (`document`,
  `schema`, `values`, `structure`, `headings`, `lists`, `tables`, `columns`, `markdown`,
  `images`, `styles`, `combinations`, `overflow`, `fonts`; los códigos de `stxt-core` van en
  `schema`). Las tres primeras líneas son comentarios `# Case:` (descripción), `# Code:` y
  `# Line:`, y la línea esperada es la del propio fichero, cabecera incluida (la primera línea
  del documento es la 4). Se maqueta con las métricas reales. `everyCodeHasACase` exige un caso
  por cada `ErrorCode` salvo los de `UNCOVERED`. Los tests de layout prueban los casos válidos;
  los inválidos van al catálogo.
- **Tests de referencia** (`golden/GoldenTest`): cada carpeta de
  `test/golden/NN-nombre/` tiene `input.stxt` y `expected.stxt` (volcado de
  §7); los `Rect` se comparan con tolerancia de 0.01 pt. Solo se vuelcan las cajas con `Id`.
  Los casos que necesitan un hito pendiente están en `GoldenTest.PENDING` y se saltan con el
  motivo: al completar el hito, quítalos de ahí. Cada caso escribe un SVG de depuración en
  `target/golden-svg/` y, en su carpeta del código fuente, `output.pdf` (ignorado por git) y
  `page-N.png` por página (versionados, con los contornos de las cajas y las métricas reales).
  Un PNG solo se reescribe si cambian sus píxeles más allá del antialiasing, que varía entre
  máquinas (`GoldenTest.PIXEL_TOLERANCE`, 16 por canal): si aparece modificado en git, el
  resultado ha cambiado; míralo antes de dar el cambio por bueno.
- Los tests de un algoritmo maquetan documentos pequeños con `LayoutFixture` (A4: área útil de
  515 × 762 pt desde (40, 40); `rect(id)` da la caja de borde en coordenadas de página). La fixture
  declara en el `Typography` de `Document` `Font: IBM Plex Sans` y `Size: 10`; los demás
  documentos de test con texto deben declararlos ellos mismos. Los documentos se escriben como
  listas de líneas con tabuladores (`"\tFlex:", "\t\tItem:", "\t\t\tBlock:", "\t\t\t\tId: A",
  "\t\t\t\tBox:", "\t\t\t\t\tHeight: 10"`); el `Id` de un ítem va en su elemento, no en el
  `Item` o el `Area`. Un test de layout no puede desbordar: los
  desbordamientos van al catálogo de errores. Con
  `FixedMetrics`, cada carácter mide 5 pt con `Size: 10`. `layOutPaged(alto, …)` pagina
  (con 180 pt de alto, el área útil mide 100) y `rect(página, id)` / `has(página, id)` miran
  una página concreta; los tests `*FragmentationTest` lo usan.
- Para un caso nuevo, calcula el resultado esperado a mano siguiendo la especificación (como en
  el "Cómo se maqueta" del §10) y añade una carpeta numerada.

## Al cambiar el comportamiento

1. La especificación manda: si el código y `ESPECIFICACION.md` discrepan, aclara cuál es el
   correcto antes de cambiar ninguno de los dos.
2. Si añades o cambias una propiedad, actualiza a la vez: el schema `.stxt` (en su grupo o
   en cada elemento que la admite), el resumen del §9, la tabla del §4, `Style`,
   su lectura (`PropertyReader`, o `BoxTreeBuilder.element` si es propia de un elemento) y, si
   aplica, la validación del §6 (`ModelValidator`).
   Una regla de validación nueva lleva su `ErrorCode`, su fila en la tabla del §6 y un caso en
   el catálogo de errores.
3. Al completar un hito, actualiza la tabla de estado y la hoja de ruta del README y elimina sus
   `TODO(hito N)`.
4. Cada decisión de interpretación (hueco de la especificación, diferencia con CSS,
   simplificación) se apunta en [`docs/DECISIONES.md`](docs/DECISIONES.md) con el hito, dónde
   está y la alternativa. Se repasan todas juntas al terminar los hitos.
