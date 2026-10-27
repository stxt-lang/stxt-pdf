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
- **Hoja de ruta de los hitos 11 en adelante:** [`docs/ROADMAP.md`](docs/ROADMAP.md). Léela al
  empezar una sesión (estado, decisiones abiertas y registro de sesiones) y actualízala al
  terminar.
- **Schema STXT** (`@stxt.schema`): [`src/main/resources/stxt/dev.stxt.boxes.stxt`](src/main/resources/stxt/dev.stxt.boxes.stxt)
  (si difiere del resumen del §9 de la especificación, manda el fichero)
- **Documento de ejemplo:** [`src/test/resources/examples/report.stxt`](src/test/resources/examples/report.stxt)

## Comandos

Java 17 y Maven (`dev.stxt:stxt-boxes`, package raíz `dev.stxt.boxes`).

```bash
mvn test                                   # compila y ejecuta todos los tests
mvn test -Dtest=ValueParserTest            # una clase
mvn test -Dtest='TrackSizingTest#nombre'   # un método
```

Eclipse: el repositorio incluye `.project`, `.classpath` y `.settings/` (proyecto `stxt-boxes`,
naturaleza Java + m2e, Java 17, UTF-8). Si cambian las dependencias: *Maven → Update Project*.

## Estado

Los algoritmos de layout funcionan, con paginación y salida SVG y PDF; los hitos 1
a 14 están terminados. La tabla de estado del README es la referencia; al terminar un hito,
actualízala.

| Hito | Trabajo | Dónde |
|---|---|---|
| 1 | ~~Conectar con `dev.stxt:stxt-core` y validar contra el template~~ (hecho) | `stxt/DocumentReader` |
| 2 | ~~Flujo de bloque y texto; activar `GoldenTest` (caso 01)~~ (hecho) | `layout/block/BlockLayout`, `layout/inline/InlineLayout` |
| 3 | ~~`relative`, `absolute`, `fixed` (esta en el `Paginator`)~~ (hecho; caso 03) | `layout/position`, `fragment/Paginator` |
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

El hito 15 (bloque en columnas) se sigue en
[`docs/ROADMAP.md`](docs/ROADMAP.md); al terminarlo, añádelo también a esta tabla.

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
| `model` | Tipos puros: `Value` (valor *especificado*, sealed), `ValueParser`, `Sides`, `Insets`, `Rect`, `VerticalMetrics` |
| `stxt` | `DocumentReader`: parsea con `stxt-core`, valida contra el schema y exige una única raíz `Document` con el namespace `dev.stxt.boxes` |
| `resources` | `Resources` (carpeta de recursos y rutas seguras), `FontLibrary` (índice de `fonts/`), `FontFile` (un TTF con sus métricas), `ResourceException` |
| `tree` | `Box`, `Style` (propiedades y enums), `FontFace`, `Media`, `DocumentSchema` (el schema compilado), `BoxTreeBuilder`, `InvalidModelException` |
| `layout` | `Engine`, `LayoutAlgorithm`, `ConstraintSpace`, `LayoutResult`, `LayoutCache`, `IntrinsicSizes`, `BoxModel`, `MarginStrut`, `AdjoiningMargins`, `Sizing`, `PendingAbsolute`, `Slice`, `UnitBreaker`, `LaidOutDocument` |
| `layout.block` / `inline` / `flex` / `grid` / `table` / `position` | Un algoritmo por `Layout` y sus piezas puras (`LineBreaker`, `FlexResolver`, `TrackSizing`, `Placement`, `AbsoluteEquation`…) |
| `fragment` | `Fragment`, `BreakToken`, `Paginator`, `Page`, `BreakRules`, `TextLine` |
| `paint` | `Canvas`, `StackingContext`, `SvgCanvas`, `PdfCanvas` (PDFBox), `PdfStructure` (PDF etiquetado y marcadores), `PdfWriter` (documento → PDF en una llamada) |
| `output` | `StxtDump` (formato de los tests de referencia, §7) |

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
  árbol al empezar, así que el resto del builder da la estructura por buena. `INTERNAL` da el
  nombre interno de los cortos (`Size` → `font-size`), que es el de `Style.line`.
- **`BoxTreeBuilder`** traduce cada elemento a una `Box` (`Style.layout` y `Style.position` salen
  del elemento; las propiedades de `Item` y `Area` pasan al elemento que envuelven), interpreta
  valores, aplica la herencia y comprueba las reglas del §6 que el schema no puede expresar. Los
  errores son `InvalidModelException` con un **código** (`ErrorCode`) y la **línea del nodo
  culpable**: la de la propiedad (`Style.line("height")`, `"grid.rows"`, `"item.grow"`…), no la
  de su caja. La raíz que devuelve es una `Box` anónima que representa el área útil.
- **Reglas reutilizables (`Styles`, §4.10):** `Element`, `For class` y `For id` con `Box`,
  `Typography` y `Break`. `BoxTreeBuilder.applyRules` las aplica entre la herencia y lo propio
  del elemento (prioridad: elemento > id > clases en orden de `Styles` > tipo > herencia), con
  las mismas funciones que leen los grupos del elemento: cada propiedad guarda la línea de la
  regla y se valida como si la declarara el elemento.
- **Elementos con significado (§5.13):** `Paragraph` (un `Text` de un solo párrafo) y
  `Heading 1`…`Heading 6` (un bloque con hijos `Text`). `Box.element` guarda el nombre
  canónico del elemento (`heading-2`) para las reglas, los marcadores y las etiquetas. El
  builder fija `After: avoid` e `Inside: avoid` en los encabezados y comprueba su jerarquía
  en orden del documento. `PdfStructure` construye el árbol de estructura del PDF etiquetado
  (contenido marcado con `Canvas.beginText`/`endText`; fondos, bordes y `Fixed` son
  *artifacts*) y los marcadores.
- **Listas (§5.14):** `List` (con `Marker` obligatorio y `Start`) y `List item` (con `Number`)
  son cajas de bloque. El builder numera los ítems y crea en `Box.marker` la caja del marcador,
  generada y fuera de `children` (`Box.isMarker()`): texto con la tipografía del ítem y el
  espacio final incluido (`"3. "`). `MarkerLayout` la coloca en el primer fragmento del ítem,
  como su primer hijo, a la izquierda de su caja de borde y con la línea base de su primera
  línea (`TextMeasurer.vertical`). Quien recorra los hijos de un fragmento debe saltar los
  marcadores si busca contenido en flujo; `Overflow` los comprueba contra la sangría de la
  lista (`LIST_MARKER_OVERFLOW`).
- **Tablas (§5.15):** el builder traduce una `Table` a una caja `GRID` (`Box.element` =
  `table`) cuyos ítems son sus `Cell`, con la colocación explícita en `Style.itemArea` (fila y
  columna por orden y `Span`); `Head`, `Body`, `Foot` y `Row` no son cajas. `Box.headRows` y
  `Box.footRows` dicen qué filas de la rejilla son cabecera y pie. El `Typography` de la `Row`
  se aplica a sus celdas como herencia y su `Break`, como el de las celdas. `Engine` despacha
  las tablas a `TableLayout`, que maqueta el grid sin fragmentar (`GridLayout.arrange`) y corta
  solo el cuerpo con `UnitBreaker`, repitiendo enteros la cabecera y el pie
  (`UnitBreaker.Repeat`). `PdfStructure` las etiqueta (`Table`, `THead`/`TBody`/`TFoot`, `TR`,
  `TH`/`TD`); las filas repetidas son contenido solo en su primera página.
- **Política de errores (§6):** más estricta que CSS. Lo que no tendría efecto o se contradice
  es un error, nunca un valor por defecto silencioso; se para en el primer error. Los
  porcentajes verticales necesitan un alto definido, que se decide al leer (§3.6,
  `BoxTreeBuilder.definiteHeight`). Los desbordamientos (§5.12) son errores: `Overflow` los
  busca en el resultado ya paginado y señala al culpable más interior. `Font` y `Size` no
  tienen valor por defecto y se suelen declarar en el `Typography` de `Document`. No
  generalices una regla nueva sin comentarlo con el usuario.
- **Enums de `Style`:** los valores STXT se convierten con `Enum.valueOf` sobre el texto en
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
  del corte (`PERFECT_BREAK` … `FORCED`), las `PendingAbsolute` que suben hasta su
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
  (líneas, ítems o filas) con `UnitBreaker`; una unidad que no cabe ni al principio de página
  parte sus ítems en paralelo, repitiendo su layout con `ConstraintSpace.fragmented` y el
  espacio guardado en `UnitBreaker.Placed`, y el `BreakToken.Container` lleva el token de cada
  ítem. Con `atFragmentainerStart` hay que colocar algo aunque desborde (garantía de progreso).
- **Numeración (`{page}`/`{pages}`):** `InlineLayout` mide los marcadores como `00` y las
  líneas los conservan; al terminar de paginar, el `Paginator` llama a `Engine.number`, que
  rehace solo esas líneas con los valores de cada página (`PageNumbers`).
- **`Fragment`**: posición relativa a la caja de **contenido** del padre; `StxtDump` la pasa
  a coordenadas absolutas de página. Lleva su propio `border` y `padding`, que en un fragmento
  partido no son los de la caja: usa siempre los del fragmento para llegar a su caja de contenido.
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
  (`List.copyOf`). `Box` es mutable y su igualdad es por identidad (clave de la caché).
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
- **Catálogo de errores** (`errors/ErrorCatalogTest`): `src/test/resources/errors/errors.stxt`
  tiene un `Case` por regla, con `Code`, `Line` (contada desde la primera línea del documento
  incrustado) y el documento en un bloque `Document >>`. Se maqueta con las métricas reales.
  `everyCodeHasACase` exige un caso por cada `ErrorCode` salvo los de `UNCOVERED`. Los tests de
  layout prueban los casos válidos; los inválidos van al catálogo.
- **Tests de referencia** (`golden/GoldenTest`): cada carpeta de
  `src/test/resources/golden/NN-nombre/` tiene `input.stxt` y `expected.stxt` (volcado de
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
   en cada elemento que la admite), el resumen del §9, la tabla del §4, `Style` y
   `BoxTreeBuilder` (su lectura y, si aplica, la validación del §6).
   Una regla de validación nueva lleva su `ErrorCode`, su fila en la tabla del §6 y un caso en
   el catálogo de errores.
3. Al completar un hito, actualiza la tabla de estado y la hoja de ruta del README y elimina sus
   `TODO(hito N)`.
4. Cada decisión de interpretación (hueco de la especificación, diferencia con CSS,
   simplificación) se apunta en [`docs/DECISIONES.md`](docs/DECISIONES.md) con el hito, dónde
   está y la alternativa. Se repasan todas juntas al terminar los hitos.
