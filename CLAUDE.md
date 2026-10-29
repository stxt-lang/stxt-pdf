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
- **Hoja de ruta de los hitos 16 a 35 (Markdown, imágenes, pruebas, ejemplos, referencias e índice y las mejoras que salieron de los ejemplos):** [`docs/ROADMAP.md`](docs/ROADMAP.md).
  Léela al empezar una sesión (estado, decisiones abiertas y registro de sesiones) y
  actualízala al terminar. Las de los hitos 11 a 15 y de calidad y arquitectura están
  terminadas y solo quedan en el historial de git.
- **Revisión de QA y arquitectura (SOLID, DRY, KISS), hilos y rendimiento:** [`docs/ROADMAP-QA-ARQ.md`](docs/ROADMAP-QA-ARQ.md).
  Tareas con evidencia medida, propuesta y criterio de aceptación, y un plan de sesiones; las sondas con que se
  midió están en [`tools/qa/`](tools/qa/README.md). Léela al empezar una sesión de calidad y actualízala al terminar.
- **Schema STXT** (`@stxt.schema`): [`src/main/resources/stxt/dev.stxt.boxes.stxt`](src/main/resources/stxt/dev.stxt.boxes.stxt)
  (si difiere del resumen del §9 de la especificación, manda el fichero)
- **Ejemplos de documentos completos:** [`test/examples/`](test/examples) (índice en su `README.md`)
- **Documento de la especificación (§10):** [`test/spec/report.stxt`](test/spec/report.stxt)

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
a 35 están terminados. La tabla de estado del README es la referencia; al terminar un hito,
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

| 19 | ~~Los casos de prueba pasan de `src/test/resources` a `test/`~~ (hecho) | `test/{golden,errors,examples,spec}`, `.gitignore`, `.classpath` |
| 20 | ~~Catálogo de errores: un fichero STXT por caso, en carpetas por grupo~~ (hecho) | `test/errors/<grupo>/`, `errors/ErrorCatalogTest` |
| 21 | ~~Ejemplos de verdad: once documentos completos con PDF y PNG limpios; IBM Plex Serif y Mono~~ (hecho) | `test/examples/`, `examples/ExamplesTest`, `golden/ReferenceFiles`, `tools/ExampleImages.java`, `resources/images/examples/` |

| 22 | ~~Referencias a páginas (`{page of: id}`, `{number of: id}`) e índice automático (`Contents`)~~ (hecho; caso 15) | `model/Reference`, `tree/References`, `tree/ContentsBuilder`, `layout/Paginator`, `layout/inline/InlineLayout`, `paint/Leaders` |

| 23 | ~~Enlaces: `Link` en las cajas y `[texto](destino)` en Markdown, a una URL o a otra caja~~ (hecho; caso 16) | `model/Links`, `model/TextFormat`, `tree/LinkTargets`, `tree/MarkdownBuilder`, `paint/StackingContext`, `paint/PdfLinks`, `paint/PdfDestinations`, `paint/PdfStructure` |

| 24 | ~~Primera página distinta: `Pages` en `Fixed` (`all`, `first`, `not-first`, `odd`, `even`, `last`)~~ (hecho; caso 17) | schema, `tree/Style.Pages`, `tree/BoxTreeBuilder`, `layout/Paginator` |

| 25 | ~~Estilo de las tablas: `Element` por posición de la celda (`Head cell`, `Body cell`, `Foot cell`, `Odd row cell`, `Even row cell`), `For column` y `Column align`~~ (hecho; caso 18) | schema, `tree/StyleRules`, `tree/CellPosition`, `tree/CellSelector`, `tree/TableBuilder`, `tree/BoxTreeBuilder` |

| 26 | ~~Formato en línea fuera de `Markdown`: `Inline: markdown` en `Typography` (se hereda), con la sintaxis en línea y los enlaces de `Markdown`~~ (hecho; caso 19) | schema, `tree/Style.Inline`, `tree/PropertyReader`, `tree/MarkdownBuilder.inline`, `tree/BoxTreeBuilder.inlineFormat` |

| 27 | ~~Mayúsculas, interletraje y versalitas: `Transform`, `Letter spacing` y `Small caps` en `Typography` (se heredan); `Language` obligatorio~~ (hecho; caso 20) | schema, `tree/Style`, `tree/PropertyReader`, `layout/inline/TextTransform`, `layout/inline/LineBreaker`, `model/TextFormat`, `paint/PdfCanvas`, `paint/PdfStructure` |

| 28 | ~~Aspecto de las cajas: `Radius` (esquinas redondeadas, 1 a 4 valores), `Border color` de 1 a 4 colores, `Background` con `linear(ángulo, color…)` y colores con opacidad (`#RGBA`, `#RRGGBBAA`); sin sombras~~ (hecho; caso 21) | schema, `model/Colors`, `model/Corners`, `model/Radii`, `model/Paint`, `tree/Style`, `tree/ValueReader`, `paint/Shapes`, `paint/StackingContext`, `paint/PdfCanvas`, `paint/SvgCanvas` |

| 29 | ~~Texto preformateado y código: `White space` (`pre`, `pre-wrap`) y `Tab size` en `Typography` (se heredan); `Code font` en `Markdown` para el código en línea y los bloques de código~~ (hecho; caso 22) | schema, `tree/Style`, `tree/PropertyReader`, `layout/inline/PreformattedText`, `layout/inline/InlineLayout`, `layout/Overflow`, `tree/MarkdownBuilder`, `model/TextFormat`, `layout/inline/PdfMetrics` |

| 30 | ~~Giro (`Rotate`, `Rotate origin`) de `Relative`, `Absolute`, `Fixed` e `Image`, solo pintado y que quepa en la página; papel con nombre (`Paper`, `Orientation`) en `Media`~~ (hecho; caso 23) | schema, `model/Rotation`, `tree/Media`, `tree/BoxTreeBuilder`, `layout/Engine`, `layout/Overflow`, `paint/StackingContext`, `paint/PdfCanvas`, `paint/SvgCanvas`, `paint/PdfDestinations` |

| 31 | ~~Imágenes vectoriales: `Image` admite SVG (subconjunto cerrado, texto con las fuentes de `fonts/`, sin referencias externas), vectorial en el PDF~~ (hecho; caso 24) | `resources/SvgInspector`, `resources/ImageFile`, `paint/PdfImages`, `paint/PdfCanvas`, `paint/SvgCanvas`, `tools/ExampleImages.java` |

| 32 | ~~Secciones: `Section` con su `Media` (heredado por grupos), sus `Fixed` y su numeración de páginas (`Page numbering`, `Inherit fixed`); todo el contenido va dentro de una sección~~ (hecho; caso 25) | schema, `tree/Section`, `tree/BoxTreeBuilder`, `tree/StyleRules.applySection`, `layout/Paginator`, `layout/Overflow`, `fragment/Page`, `paint/PdfCanvas`, `paint/PdfStructure`, `paint/PdfDestinations`, `output/StxtDump` |

| 33 | ~~Numeración de los encabezados: `Numbered` en `Typography` (se hereda); `1`, `1.1`, `1.1.1` delante del título, en `{number of: id}`, el índice y los marcadores; `Numbered: false` deja un encabezado fuera~~ (hecho; caso 26) | schema, `tree/Style`, `tree/PropertyReader`, `tree/HeadingNumbers`, `tree/BoxTreeBuilder`, `tree/References` |

| 34 | ~~Estilo del bloque de código: el bloque de código de un `Markdown` es el elemento generado `Code block`, que las reglas `Element: Code block` estilan aparte de los `Text`~~ (hecho; caso 27) | `tree/Element`, `tree/MarkdownBuilder`, `tree/BuildContext`, `tree/BoxTreeBuilder`, `paint/PdfStructure` |

| 35 | ~~Estilo de una fila de tabla: `Class` en la `Row`, cuyas reglas `For class` se aplican a cada una de sus celdas~~ (hecho; caso 28) | schema, `tree/CellPosition`, `tree/TableBuilder`, `tree/StyleRules` |

Los hitos 16 a 35 están terminados; la hoja de ruta ([`docs/ROADMAP.md`](docs/ROADMAP.md)) queda como
registro. Si se abre un hito nuevo, apúntalo allí y en esta tabla.

No queda ningún `// TODO(hito N)` en el código. Si se abre un hito nuevo, marca lo pendiente
así y lanza `UnsupportedOperationException` con el hito en el mensaje; el javadoc de cada clase
pendiente describe el algoritmo a implementar (por ejemplo, el pseudocódigo de `BlockLayout`).

## Arquitectura

Flujo de datos:

```
texto STXT ──DocumentReader──▶ InlineNode (stxt-core) ──BoxTreeBuilder──▶ Box (árbol) + Media + List<Section>
   ──Engine.layOutDocument──▶ Paginator ──▶ Overflow ──▶ LaidOutDocument (Media, Metadata, raíz, List<Page>)
   ──▶ StxtDump (tests) · StackingContext + Canvas (SvgCanvas / PdfCanvas)
```

| Package | Contenido |
|---|---|
| `model` | Tipos puros: `StxtPdfException` (la raíz de los errores del motor, con su `Kind`), `InternalException` y `Limits` (páginas, píxeles de una imagen, elementos y niveles de un SVG); `Rotation` (`Rotate` y `Rotate origin`: el giro, su centro y el rectángulo que envuelve lo girado), `Value` (valor *especificado*, sealed), `ValueParser`, `Sides`, `Insets`, `Rect`, `VerticalMetrics`, `DecorationMetrics`, `FontFace` (con `FontWeight` y `FontStyle`), `TextRun` y `TextFormat` (tramos con formato), `Reference` (`{page of: id}` y `{number of: id}` dentro de un texto), `Links` (los destinos de un enlace: qué es válido), `GridAreas`, `Area` y `Cell` (la colocación de grid ya interpretada), `Tolerance` (la épsilon de redondeo), `Colors` (los colores `#RGB`…`#RRGGBBAA` y su opacidad), `Corners` y `Radii` (`Radius`: el abreviado por esquina y los radios ya resueltos) y `Paint` (el fondo: un color o un `linear(…)`) |
| `stxt` | `DocumentReader`: parsea con `stxt-core`, valida contra el schema y exige una única raíz `Document` con el namespace `dev.stxt.boxes` |
| `resources` | `Resources` (carpeta de recursos y rutas seguras), `FontLibrary` (índice de `fonts/`), `FontFile` (un TTF con sus métricas), `ImageLibrary` e `ImageFile` (las imágenes de `images/`, con su formato y su tamaño), `SvgInspector` (lee un SVG y comprueba el subconjunto), `ResourceException` e `ImageFormatException` |
| `tree` | `Box`, `Element` (los elementos de caja), `Style` (propiedades y enums), `Media`, `DocumentSchema` (el schema compilado), `InvalidModelException`; `BoxTreeBuilder` (documento → árbol) y sus piezas: `ValueReader`, `PropertyReader`, `StyleRules`, `TableBuilder`, `ListBuilder`, `MarkdownBuilder`, `ContentsBuilder`, `ModelValidator`, `HeadingNumbers`, `BuildContext`, `Nodes`; `References` (las referencias del árbol y su comprobación), `LinkTargets` (la comprobación de los enlaces `#id`); `PropertyKey` (las claves de `Style.line`) |
| `layout` | `Engine`, `LayoutAlgorithm`, `ConstraintSpace`, `LayoutResult`, `LayoutCache`, `IntrinsicSizes`, `BoxModel`, `MarginStrut`, `AdjoiningMargins`, `Sizing`, `PendingAbsolute`, `Slice`, `UnitBreaker`, `Paginator`, `Numbering` (el número impreso de cada página), `LaidOutDocument`, `Messages` (piezas de los mensajes de error) |
| `layout.block` / `inline` / `flex` / `grid` / `table` / `multicol` / `position` / `image` | Un algoritmo por `Layout` y sus piezas puras (`LineBreaker`, `FlexResolver`, `TrackSizing`, `Placement`, `Columns`, `AbsoluteEquation`…) |
| `fragment` | `Fragment`, `BreakToken`, `BreakAppeal` (calidades de corte), `Unit` (unidad de corte de flex y grid), `Page`, `BreakRules`, `TextLine` |
| `paint` | `Canvas`, `StackingContext`, `SvgCanvas`, `PdfCanvas` (PDFBox), `PdfImages` (las imágenes que lleva un PDF: PNG y JPEG incrustados, SVG dibujados como vectores) y `SvgFonts` (las fuentes de `fonts/` con que JSVG dibuja el texto de un SVG), `PdfStructure` (PDF etiquetado y marcadores), `ImageFit` (dónde se dibuja una imagen según `Fit`), `Leaders` (la guía de las entradas de un índice), `PdfLinks` (las anotaciones de enlace) y `PdfDestinations` (la esquina de cada caja con nombre, para marcadores y enlaces) y `Shapes` (los caminos de las cajas redondeadas y de los lados de un borde, que dibujan los dos lienzos) |
| `output` | `StxtDump` (formato de los tests de referencia, §7) |
| `convert` | `Converter`: la entrada del motor. Reúne la carpeta de recursos, los estilos aparte y la depuración en un objeto inmutable y seguro entre hilos (`builder()`), y da `layOut`, `toPdf` y `toSvg`; cada conversión crea su propio `Engine`. Es la capa de arriba: nada del motor depende de ella (`ArchitectureTest`) |

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
  hereda), `Typography` (texto, viudas y huérfanas; se hereda; la base del documento va en `Styles`, con `Element: Document`) y
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
- **Documento de estilos aparte (§4.10):** las reglas de `Styles` pueden estar también en un documento de
  raíz `Styles` (`DocumentReader.readStyles`) que se pasa al maquetar (`BoxTreeBuilder.build(documento,
  estilos)`, `Engine.layOutDocument(documento, estilos)`, `Converter.builder().styles(estilos)`); es
  opcional, puede ser `null` y el documento no lo nombra: el motor no busca ficheros. `StyleRules` lee los
  dos orígenes por separado (un duplicado es un error dentro de un origen, no entre los dos; una regla
  `For id` sin elemento nunca es un error, porque las plantillas pueden rellenarlo después) y en cada nivel de prioridad aplica antes las reglas del
  documento aparte, así que el documento gana. `Element: Document` es la regla de la raíz: solo admite
  `Typography`, que hereda todo (`StyleRules.applyDocument`), y `Document` ya no admite `Typography`. Un
  error de una regla del documento aparte lleva su línea y lo dice (`InvalidModelException.inSource`:
  `línea 8 de los estilos: …`); `ExamplesTest` y `GoldenTest` leen el `styles.stxt` de la carpeta si lo hay.
- **Reglas reutilizables (`Styles`, §4.10):** `Element`, `For class`, `For id` y `For column` con
  `Box`, `Typography` y `Break`. `StyleRules.apply` las aplica entre la herencia y lo propio
  del elemento (prioridad: elemento > id > clases en orden de `Styles` > clases de la `Row` de la celda
  (en orden de `Styles`) > `For column` en orden de `Styles` > posición de la celda > `Column align` > tipo > herencia), con
  las mismas funciones que leen los grupos del elemento: cada propiedad guarda la línea de la
  regla y se valida como si la declarara el elemento. Las reglas de las celdas dependen de dónde
  están: `TableBuilder` da a `BoxTreeBuilder.element` una `CellPosition` (grupo, fila dentro del
  grupo, columna donde empieza, el `Column align` de su columna y el nodo `Class` de su `Row`, que
  `StyleRules.classRules` resuelve igual que el de la celda) y `Element: Head cell`,
  `Body cell`, `Foot cell`, `Odd row cell` y `Even row cell` (`CellSelector`, que no son
  elementos: el de la caja sigue siendo `Cell`) y `For column: n` o `n m` las seleccionan con ella.
- **Elementos con significado (§5.13):** `Paragraph` (un `Text` de un solo párrafo) y
  `Heading 1`…`Heading 6` (un bloque con hijos `Text`). `Box.element` guarda el `Element`
  del que procede (`HEADING_2`) para las reglas, los marcadores y las etiquetas; el enum da
  también su `Layout`, su `Position` y si puede ser un ítem, y `ElementTest` lo compara con el
  schema. El builder fija `After: avoid` e `Inside: avoid` en los encabezados y comprueba su
  jerarquía en orden del documento. **Numeración:** `Numbered` (`Style.numbered`, de `Typography`, así que
  se hereda y vale en `Styles`) numera los encabezados: `HeadingNumbers` (en `BuildContext`) lleva un contador
  por nivel en orden del documento, a través de secciones y `Markdown`, y `BoxTreeBuilder.element` escribe el
  número delante del primer `Text` del encabezado cuando ya están leídos sus textos (`HeadingNumbers.prefix`:
  `text` y, si los hay, un tramo `PLAIN` al principio de `runs`), de modo que lo miden el layout y lo ven el
  índice (`headingText`), los marcadores y el PDF etiquetado sin tratarlo aparte. `Box.number` guarda el número
  completo (`4.2`) para `{number of: id}`. Los niveles sin numerar no aportan al número (un título `Heading 1` sin número deja a los `Heading 2` como `1`, `2`…). En el PDF etiquetado un `Text` es un `P` (un `Span` si es hijo de un
  encabezado) y una caja que declara otro `Language` lleva su `Lang`. `PdfStructure` construye el árbol de estructura del PDF etiquetado
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
  se aplica a sus celdas como herencia, su `Break`, como el de las celdas, y su `Class` da a cada
  celda las reglas `For class` de sus clases (`CellPosition.rowClass`; una `Row` no tiene `Id` ni `Box`). `Engine` despacha
  las tablas a `TableLayout`, que maqueta el grid sin fragmentar (`GridLayout.arrange`) y corta
  solo el cuerpo con `UnitBreaker`, repitiendo enteros la cabecera y el pie
  (`UnitBreaker.Repeat`). `PdfStructure` las etiqueta (`Table`, `THead`/`TBody`/`TFoot`, `TR`,
  `TH`/`TD`; una `Cell` con `Header: true` en `Body` o `Foot` es una `TH` con `Scope: Row`, y en `Head` es el error
  `TABLE_HEADER`); las filas repetidas son contenido solo en su primera página.
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
- **Secciones (§5.21):** un `Document` solo tiene `Media`, `Title`, `Language`, `Styles`, `Fixed` y una o más
  `Section`; todo el contenido va dentro. `BoxTreeBuilder.Result` da la raíz del documento y la lista de
  `Section` (`tree/Section`: caja, `Media`, `Page numbering` e `Inherit fixed`). La **caja de cada sección es la
  raíz de su layout** (`Element.SECTION`, una `Box` **sin `parent`**, como la raíz anónima de antes) y **cuelga de
  `root.children` sin ser hija** (`root.children.add`, no `addChild`): los recorridos de todo el árbol (referencias,
  enlaces, índices, fuentes, imágenes, `Id`) parten de la raíz y las ven, y el layout, los `Fixed` y subir por
  `parent` se quedan dentro de la sección. Los `Fixed` hijos de `Document` también son hijos de la raíz (con
  `parent`) y no se maquetan en el flujo: los coloca el `Paginator`. `Media` de una sección sale de
  `BoxTreeBuilder.media(n, base)`, que hereda por grupos (tamaño y `Margin`) y recuerda el `Paper` para
  `Orientation` sola (`MediaSpec`). `Paginator` hace una pasada de flujo por sección (`flow`) con **una
  sola convergencia** de las referencias, y calcula con `Numbering` el número impreso de cada página (el
  contador, `restart` y `none`) y el último de su tanda (`{pages}`); `Page` lleva su `Media`, su sección y su
  `printed` (0 = sin número). Un `Fixed` del documento se pone con `PendingAbsolute(box, 0, 0)` en cada página
  de cada sección que lo hereda, y `Pages.includes` se evalúa con las páginas de su alcance (documento o
  sección). `{page of}` y los índices dan el número impreso; un texto con `{page}` en una página sin número es
  `PAGE_NUMBER_UNAVAILABLE`. `StyleRules.applySection` solo deja pasar `Typography` a una sección.
  `Overflow`, `StxtDump`, `PdfCanvas` y `PdfDestinations` usan el `Media` de cada página (`Page.media()`), y
  `LaidOutDocument.root()` es la raíz para `PdfStructure` (`Sect` por sección, `PageLabels` del PDF).
  Los tests escriben el contenido con `Docs.sectioned` (envuelve en una `Section` lo que no es cabecera).
- **Tipos de error (§6.1):** todo error del motor es un `StxtPdfException` (`model`; `kind()`, `code()` en texto y
  `line()`, `NO_LINE` = 0 si no es de una línea), así que una sola `catch` basta. Lo extienden
  `InvalidModelException` (reglas del modelo, desbordamientos y recursos que nombra el documento; `errorCode()` da el
  `ErrorCode` y `ErrorCode.kind()` su `Kind`: `INVALID_DOCUMENT` salvo `PAGE_LIMIT`, `IMAGE_TOO_LARGE` y
  `SVG_TOO_COMPLEX`, que son `LIMIT`),
  `DocumentSyntaxException` (sintaxis y schema de `stxt-core`, con su código y su línea y la excepción original como
  causa; `LimitException` de la librería es `LIMIT`), `ResourceException` (la carpeta de recursos, `RESOURCE`; con
  `ImageFormatException`) e `InternalException` (un fallo del motor). `DocumentReader` y `DocumentSchema` convierten
  los errores de la librería; `Converter` deja pasar los del motor, convierte cualquier otra excepción de ejecución
  en `InternalException` y no toca la `IOException` del destino. Un `IllegalStateException` o `IllegalArgumentException`
  solo es válido dentro del motor para un mal uso interno; lo que puede ocurrirle a quien convierte va con uno de
  estos tipos.
- **Límites (§6.2):** `Limits` (`model`, inmutable; `DEFAULT`: 2000 páginas, 25 Mpx por imagen, 20 000 elementos y 64
  niveles por SVG; `withMax…`) se da con `Converter.builder().limits(…)` y llega al `Engine` (que lo pasa a `Images` y
  al `Paginator`) y a `PdfCanvas.write` (los límites del dibujo de JSVG). `Images.validate` los comprueba antes de
  maquetar con lo que `ImageFile` leyó sin decodificar nada: `pixels()` de la cabecera y, en un SVG, `svgElements()`
  y `svgDepth()` de `SvgInspector`, que cuenta sin recursión lo que dibuja (cada `<use>` repite lo que referencia; satura
  en `SvgInspector.SATURATED`; un `<use>` circular es `IMAGE_FORMAT`). JSVG trae sus propios límites (2000 elementos y
  30 niveles) y por eso `PdfImages` los iguala a `Limits`: así lo que el motor acepta es lo que dibuja, y si JSVG no
  carga un SVG sale un `ImageFormatException`, no una `IOException`. Los límites del texto STXT son los de `stxt-core`.
- **Política de errores (§6):** más estricta que CSS. Lo que no tendría efecto o se contradice
  es un error, nunca un valor por defecto silencioso; se para en el primer error. Los
  porcentajes verticales necesitan un alto definido, que se decide al leer (§3.6,
  `ModelValidator.definiteHeight`). Los desbordamientos (§5.12) son errores: `Overflow` los
  busca en el resultado ya paginado y señala al culpable más interior. `Font` y `Size` no
  tienen valor por defecto y se suelen declarar en la regla `Element: Document` de `Styles`. No
  generalices una regla nueva sin comentarlo con el usuario.
- **Enums de `Style`** (y `FontWeight` y `FontStyle`, que están en `model`): los valores STXT se convierten con `Enum.valueOf` sobre el texto en
  mayúsculas y con el guion como guion bajo, así que **cada constante debe llamarse igual que
  su valor en el documento** (`TextAlign.JUSTIFY` ↔ `justify`, `Marker.LOWER_ALPHA` ↔
  `lower-alpha`). Si cambias un valor, cambia la constante y el schema.
  `Layout` y `Position` no son valores del documento: los fija el elemento.
- **Hilos (TS-01):** un `Engine` **no** se comparte entre hilos (caché del layout, reparto en líneas y páginas
  supuestas sin sincronizar): uno por conversión, que es lo que hace `Converter`. Sí se comparten `Converter`,
  `Resources` y `DocumentReader`, y un `LaidOutDocument` se puede pintar desde varios hilos (los fragmentos son
  `record`; las cajas solo se escriben mientras `BoxTreeBuilder` construye). Un `Engine` guardado se puede reutilizar en
  secuencia: `layOutDocument` suelta su caché al terminar. El protocolo con el `Paginator` (`reservePageDigits`,
  `assume`, `number`, `absolute()`) es de package. El schema compartido de `stxt-core` 1.0.3 solo se lee (ver
  `DocumentSchema`); al cambiar la versión hay que repasarlo. `ConcurrentConversionTest` y `EngineContractTest` lo vigilan.
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
  coloca en `Page.fixed` de cada página, solo en las que incluye su `Style.pages`
  (`Style.Pages.includes(página, total)`: `Pages` en el documento, `all` por defecto; la página es
  la física, la misma de `{page}`). Lo filtra el `Paginator` al colocarlas: el resto del motor
  (`Overflow`, `StxtDump`, el pintado y el PDF) recorre lo que hay en `Page.fixed`.
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
  coste crece con el cuadrado de las páginas; lo vigila `LongTextTest`). **Volver a paginar no vacía la caché**
  (PF-03): cuando el total de páginas necesita otra cifra (`Engine.reservePageDigits`) o cambia la página de una
  referencia (`Engine.assume`), el `Paginator` pasa los textos que cambian de ancho y el motor olvida solo lo que la
  `LayoutCache` y el reparto en líneas guardan de ellos y de sus antepasados (`Engine.forget`), porque el resultado de
  una caja solo depende de su subárbol. Lo vigila `PaginationCacheTest`. Al terminar el documento se vacía todo.
- **Numeración (`{page}`/`{pages}`):** `InlineLayout` mide los marcadores como `00` y las
  líneas los conservan; al terminar de paginar, el `Paginator` llama a `Engine.number`, que
  rehace solo esas líneas con los valores de cada página (`PageNumbers`).
- **Referencias (§5.19):** `{page of: id}` y `{number of: id}` (`model/Reference`) **no** son
  marcadores: `InlineLayout.runs` los sustituye por su valor antes de repartir el texto en
  líneas, así que las líneas ya no los contienen y el texto mide lo que escribe. El número sale
  del árbol (`Box.number`, que pone el builder a los `List item` numerados); la página es una
  suposición que el `Paginator` repite hasta que converge: `Engine.assume` fija las páginas
  supuestas (la primera vez, `00`), se pagina, se mira en qué página ha quedado el primer
  fragmento de cada caja referida (`Box.key()`: su `Id` o, en un encabezado de un índice, un
  `Box.anchor` generado) y se vuelve a paginar si alguna difiere, hasta 10 pasadas que cambien
  algo (`REFERENCE_UNSTABLE`). `References.validate` (al construir el árbol) rechaza los `Id` que
  no existen, las páginas de un `Fixed` y los números de lo que no tiene.
- **Índice (§5.19):** `Contents` es un bloque cuyas entradas genera `ContentsBuilder` al final
  de la construcción, con todos los encabezados ya conocidos (también los de un `Markdown`; salvo los que
  declaran `In contents: false`, que siguen siendo encabezados pero no se listan): una
  caja `Flex` por encabezado (`Element.CONTENTS_ENTRY_1`…`6`, que las reglas `Element` nombran
  aunque no se escriban: `Element.selectable()`) con el título, la guía (una caja vacía que
  `StackingContext.paintLeader` pinta con `Leaders`, sin texto) y el número, que es un
  `{page of: …}` del encabezado. `PdfStructure` lo etiqueta como `TOC` y `TOCI`.
- **Enlaces (§5.20):** `Link` es una propiedad del elemento (`Style.link`, como `Id`; no se hereda ni
  va en `Styles`) con un destino `https://…`, `http://…`, `mailto:…` o `#id` (`model/Links`); en
  Markdown, `[texto](destino)` es un `TextFormat` con `link` y subrayado (el destino va con el
  formato, así que dos enlaces distintos no se unen en un tramo). `LinkTargets` comprueba al final
  de la construcción que cada `#id` existe y no está en un `Fixed` (`LINK_UNKNOWN`); `LINK_SCHEME`
  es el destino mal escrito, al leerlo. Cada entrada de un `Contents` lleva `Link` a su encabezado
  (`Box.key()`). El layout no sabe nada de los enlaces: `StackingContext.paintLinks` calcula las
  zonas al pintar (la caja de borde de cada fragmento con `Link`; el rectángulo de cada tramo con
  enlace de cada línea, uniendo los seguidos con el mismo destino) y llama a `Canvas.link`.
  `PdfCanvas.write` crea todas las páginas antes de pintar, `PdfLinks` crea las anotaciones
  (`URI`, o el destino que resuelve `PdfDestinations` al final: la esquina del primer fragmento
  de la caja, la misma de los marcadores). El `Link` del PDF etiquetado envuelve lo que se pulsa:
  `PdfStructure.build` crea el de una caja con `Link` como hijo de su elemento y padre de su contenido
  (una `Image`, al revés: `Link → Figure`), y `PdfStructure.link` le cuelga la `OBJR` de cada
  fragmento. Un enlace de texto es otra secuencia de contenido marcado: `StackingContext.paintText`
  parte el texto en secuencias de tramos con el mismo destino, `Canvas.beginLinkText` abre la de un
  enlace (`PdfStructure.markLink` crea su `Link`, hijo del elemento de la caja, con un `MCID` aunque
  ocupe varias líneas) y las zonas de cada línea (`Canvas.link`, dentro de la secuencia) le cuelgan una
  `OBJR` (`inlineLink`). Un `Link` con algo que leer no lleva `Alt`. Lo que no es contenido (un `Fixed`,
  una imagen decorativa, las filas repetidas de una tabla) tiene un `Link` suelto, hijo del `Document`,
  con el texto como `Alt` (`PdfStructure.outside`): PDF/UA exige que toda anotación esté en un `Link`.
  Las páginas con anotaciones llevan `Tabs: S`. El SVG no pinta enlaces.
- **Aspecto de las cajas (§5.6):** `Radius`, `Border color` (de 1 a 4 colores: `Style.borderColor` es un
  `Sides<String>`, `null` = el `Color` del texto) y `Background` (un `Paint`: `Solid` o `Linear`) solo cambian
  el pintado; el layout no los lee. Los colores siguen siendo texto (`#RRGGBBAA`, `Colors` los valida y los
  pasa a componentes). `StackingContext` resuelve los radios de cada fragmento (`Radii.resolve` sobre su
  caja de borde; con `Decoration: slice`, `forFragment(first, last)` deja solo las esquinas de su lado) y
  llama a `Canvas.fill` (el fondo, con `radii.inset(borde)`), `Canvas.border` (un color por lado, ya
  resuelto) e `image` (con los radios de la caja de contenido); `Canvas.background(Rect, String)` queda para
  los rectángulos lisos (guías, líneas entre columnas, rayas del texto). `Shapes` da el camino de una caja
  redondeada (cuartos de elipse como Bézier) y el trapecio de cada lado del borde, cuyas diagonales siguen
  hacia dentro cuando la curva de dentro es redonda. `PdfCanvas`: un borde liso, opaco y de esquinas
  cuadradas son cuatro rectángulos (como antes de `Radius`; no cambia ningún PNG); si no, un anillo
  par-impar, recortado a cada trapecio con colores distintos. El degradado es un sombreado axial recortado
  a la caja (`gradient`), con una máscara suave de luminosidad si algún color no es opaco; la opacidad de un
  color liso, un estado gráfico `ca` (`painting`). `SvgCanvas` escribe `<path>`, `linearGradient` y
  `clipPath`, y deja `<rect>` lo liso y cuadrado.
- **Giro (§5.6):** `Rotate` y `Rotate origin` son propiedades del elemento (`Style.rotation`, un
  `model/Rotation`; no van en `Styles` ni se heredan) de `Relative`, `Absolute`, `Fixed` e `Image`. Solo
  cambian el pintado: el layout, el volcado y la comprobación en su sitio son los de la caja sin girar.
  Hay tres excepciones: `Engine.layout` no parte una caja girada (la maqueta sin fragmentación y, si no
  cabe, `BEFORE`); `Overflow` comprueba contra la página el rectángulo que envuelve lo girado, componiendo
  los giros de los ancestros con un `AffineTransform` (`BOX_OVERFLOW` en la línea de `Rotate`); y
  `PdfDestinations` usa ese envolvente. `StackingContext.paint` envuelve el contexto de la caja girada en
  `Canvas.beginRotation`/`endRotation` (una `Image` en flujo, cada uno de sus dos pasos): `PdfCanvas`
  escribe un `cm` dentro de `q`/`Q` y guarda el giro compuesto para los `QuadPoints` de los enlaces y el
  `BBox` de las figuras; `SvgCanvas`, un `<g transform>`. Las coordenadas de `Rotation` son las de la
  página, con la y hacia abajo: un ángulo positivo gira en el sentido de las agujas del reloj (en el PDF,
  con el origen abajo, el ángulo cambia de signo).
- **Papel (§4.1):** `Media` es `Paper` (`Media.Paper`, en vertical) con `Orientation`, o `Width` y
  `Height`; `BoxTreeBuilder.media` exige una sola de las dos formas (`MEDIA_SIZE`).
- **`Fragment`**: posición relativa a la caja de **contenido** del padre; `StxtDump` la pasa
  a coordenadas absolutas de página. Lleva su propio `border` y `padding`, que en un fragmento
  partido no son los de la caja: usa siempre los del fragmento para llegar a su caja de contenido.
  Lleva también el `margin` usado (con la base de porcentajes de su layout) y el desplazamiento
  de `Relative` ya sumado a su posición (`offsetX`/`offsetY`), que `Overflow` necesita. La geometría solo existe en los fragmentos: `Box` no guarda posición ni tamaño.
- **`TextMeasurer`**: `FixedMetrics` (cada carácter = `0.5 × tamaño`) es el de `new Engine()` y
  el de todos los tests de layout, y no necesita fuentes; `PdfMetrics` da las métricas reales de
  las fuentes TTF de `Resources` y es el que usa `Converter`.
  `Engine.layOutDocument` llama antes a `validate(root)`, que en `PdfMetrics` comprueba que
  existen las fuentes y tienen todos los caracteres (errores con línea).
- **Recursos (§5.11):** carpeta fija, por defecto `resources/` del directorio de trabajo, con
  `fonts/` e `images/`; `Resources` solo sirve para cambiarla. Toda ruta pasa por
  `Resources.resolve`, que **nunca va hacia atrás** (rechaza absolutas, `..` y enlaces que
  salen). Las fuentes se identifican por familia, peso (400/700) y estilo leídos del TTF
  (`FontLibrary`); no hay fuente por defecto ni de sustitución: `Font` es obligatoria en el
  texto. El repositorio incluye IBM Plex Sans, Serif y Mono (OFL).
- **Pintado del texto:** `Canvas.text` recibe un `FontFace`; la línea base la calcula
  `StackingContext` con `Canvas.vertical(font)` (`hhea` en PDF, la aproximación 0,8/0,2 en
  SVG). En PDF las fuentes son Type 0 Identity-H, así que `justify` usa `TJ`, no `Tw`.
  **Ligaduras:** `Ligatures` (`Style.ligatures`, de `Typography`, se hereda; `false` por defecto) va en `FontFace.ligatures`
  (`Style.fontFace()` lo apaga con `Letter spacing` distinto de 0). `FontLibrary.find` identifica el fichero con
  `FontFace.base()`; `FontFile.width(texto, tamaño, ligaturas)` mide cada palabra con el `GsubWorker` de fontbox, el mismo que
  PDFBox aplica al pintar, y `PdfCanvas.pdfFont` incrusta una fuente por combinación con
  `TrueTypeFont.setEnableGsub(font.ligatures())`: sin la opción PDFBox no aplica `GSUB` (un glifo por carácter,
  17 a 30 veces más rápido) y lo medido es lo pintado.
- **Tramos (§5.4):** `Box.text` es siempre el texto entero y `Box.runs` (`null` sin formato) lo
  parte en `TextRun` con un `TextFormat` (negrita, cursiva, subrayado, tachado, que se suman a
  la fuente de la caja con `TextFormat.face`); usa `Box.runs()`, que da un único tramo si no
  hay formato. `LineBreaker` reparte palabras que pueden tener partes de varios tramos
  (`LineBreaker.Word`, `LineBreaker.Line`) y `TextLine.runs` guarda cada tramo colocado, con el
  espaciado de `justify` incluido. `StackingContext` pinta cada tramo con su fuente y las rayas
  con `Canvas.decoration(font)` (`post` y `OS/2` en PDF). Un texto sin formato debe medirse y
  pintarse exactamente igual que una cadena: los PNG golden lo vigilan. El documento escribe
  tramos con `Markdown` o con `Inline: markdown` (ver abajo); los tests de layout los ponen en el
  árbol con `TextRuns` y `Engine.layOutDocument(BoxTreeBuilder.Result)` (`LayoutFixture.layOutRuns`).
- **Formato en línea (§5.4):** `Inline` (`Style.Inline`, `none` o `markdown`) es una propiedad de
  `Typography` que se hereda y se admite en `Styles`. Con `markdown`, `BoxTreeBuilder.inlineFormat`
  pasa el texto de un `Text`, un `Paragraph` o una `Cell` con valor por `MarkdownBuilder.inline`
  (el mismo parser y el mismo `Translator` que `Markdown`, en modo `inlineOnly`: solo párrafos con
  formato y enlaces; lo demás, `MARKDOWN_UNSUPPORTED` con la línea del párrafo). `Box.text` pasa a
  ser el texto sin las marcas (los párrafos, separados por `\n\n`), `Box.runs` los tramos y
  `Box.textBlock` deja de ser cierto: la línea de sus errores es la de donde empieza el texto. Dentro
  de un `Markdown` no actúa (`BuildContext.insideMarkdown`): sus textos ya vienen interpretados.
- **Mayúsculas e interletraje (§4.4, §5.4):** `Transform`, `Letter spacing` y `Small caps` son
  propiedades de `Typography` (se heredan y valen en `Styles`). `Document` exige un `Language`
  (accesibilidad): el builder lo pone en `Style.locale` de la raíz y se hereda, y es la
  configuración regional de `Transform`; una caja puede declarar el suyo (`Language` en `Typography`, que
  `PdfStructure` escribe como `Lang`; `Element: Document` no). `InlineLayout.runs` aplica `TextTransform` a los tramos
  **antes** de repartirlos en líneas (cambia lo que miden: `ß` → `SS`) y después de sustituir las
  referencias; `Box.text` no cambia y las líneas (`TextLine`) llevan el texto ya transformado. Los
  marcadores `{page}`/`{pages}` no se transforman (`PageNumbers` los sustituye al final). Las
  versalitas son tramos con `TextFormat.smallCaps` (texto en mayúsculas, al `SMALL_CAPS_SCALE` del
  tamaño): `LineBreaker.advance` mide cada tramo con `fontSize × format.scale()` más el
  interletraje de cada carácter, y el ancho de una línea resta el del último
  (`LineBreaker.width`). `StackingContext` pasa el tamaño del tramo y `Style.letterSpacing` a
  `Canvas.text`; `PdfCanvas.spaced` lo escribe con ajustes de `TJ`. `StackingContext.paintText` añade un
  espacio invisible a la última palabra de cada línea que no es la última del fragmento (salvo con `pre` y
  `pre-wrap`), porque el corte se come el espacio y quien une las líneas pegaría las palabras.
  `PdfStructure` da `ActualText` (el texto escrito) a los elementos con `Transform` o `Small caps` cuyo texto no lleva
  `{page}` ni referencias.
- **Texto preformateado (§4.4, §5.4):** `White space` (`Style.WhiteSpace`: `normal`, `pre`, `pre-wrap`) y
  `Tab size` (columnas de una tabulación, 4 por defecto) son propiedades de `Typography` (se heredan y
  valen en `Styles`). Con `pre` o `pre-wrap`, `InlineLayout` no usa `LineBreaker.breakParagraphs` sino
  `PreformattedText`: cada línea del texto (con las tabulaciones ya llevadas a la siguiente columna
  múltiplo de `Tab size`) es una línea de la caja, también las vacías; **todo el texto es un solo
  párrafo** (las viudas y huérfanas cuentan sobre el bloque) y las líneas **no se justifican**.
  `pre` no parte las líneas (`Overflow` da `TEXT_OVERFLOW` con un mensaje propio) y `pre-wrap` las parte
  entre palabras, perdiendo los espacios del corte. Los tamaños intrínsecos también los da
  `PreformattedText`. Una línea vacía no tiene tramos (`LineBreaker.place` no le resta interletraje).
- **Markdown (§5.17):** `MarkdownBuilder` parsea el `Content` con commonmark-java y lo traduce
  a nodos STXT (`InlineNode` con la línea de su bloque: `Heading N` con un `Text`, `Paragraph`,
  `List` con `Marker` de `Bullet`/`Numbering`, `Table` con `Columns` de `Table columns`), que
  valida con el schema; los tramos de cada texto van aparte, por nodo
  (`Translation.runs`, en `BuildContext.runs`). `BoxTreeBuilder.markdown` construye esos nodos
  con `element(...)` como hijos de la caja del `Markdown` (un bloque, `Element.MARKDOWN`), y el
  texto de cada uno recibe sus tramos. Lo que no está en el subconjunto es
  `MARKDOWN_UNSUPPORTED`: amplíalo solo traduciendo a elementos que ya existen. Una imagen
  sola en su párrafo (`![Alt](ruta)`) es una `Image`. El código necesita `Code font` en el `Markdown`
  (`MARKDOWN_PROPERTY_MISSING` si falta): el código en línea es un tramo con `TextFormat.family` (la
  familia de la caja si es `null`) y un bloque de código, un `Text` con `White space: pre`, la
  fuente de `Code font` y el código en un `Content` (sin `Id` ni `Class`). Es el elemento generado
  `Element.CODE_BLOCK` (`Code block`, nombrable en las reglas `Element` como las entradas de un `Contents`, y no
  alcanzado por `Element: Text`): `MarkdownBuilder` marca el nodo `Text` que genera (`Translation.codeBlocks`),
  `BuildContext.codeBlocks` lo pasa al builder y `BoxTreeBuilder.element` le da el elemento; sigue siendo un
  `P` en el PDF etiquetado.
  Un texto con `Inline: markdown` no tiene `Code font`, así que su código sigue sin admitirse.
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
  `shapes.svg` 150 × 75 y `label.svg` 120 × 36, y `drawing.gif`, `broken.png` y los `bad-*.svg` para
  los errores).
- **SVG (§5.18):** `ImageFile.read` reconoce un SVG por el contenido (empieza por `<`) y lo pasa por
  `SvgInspector`, un lector SAX (sin DTD ni entidades) que comprueba el subconjunto cerrado de elementos
  y propiedades, que no hay referencias fuera del fichero (`href` y `url(…)` solo a `#id`), el tamaño
  natural (`width`/`height` o `viewBox`, en píxeles CSS: `ImageFile.pixelWidth()` es un `double`) y los
  textos con su fuente (`ImageFile.checkText`: familia, `bold` e `italic` de `fonts/`, y todos los
  caracteres; sin sustitución). Todo error es `IMAGE_FORMAT` con la línea del SVG en el mensaje.
  `PdfImages` es quien incrusta las imágenes de un PDF: el PNG y el JPEG, con PDFBox; el SVG, con JSVG
  (`ResourcePolicy.DENY_ALL`) dibujando en un `PdfBoxGraphics2D`, que da un `PDFormXObject` del tamaño
  natural, una vez por fichero. `PdfCanvas.image` lo coloca con una matriz `cm` de escala a donde
  manda `ImageFit` y **restablece los guiones antes de dibujarlo** (un formulario hereda el estado gráfico
  y el modo `debug` deja un patrón de guiones activo). JSVG dibuja el texto como contornos, no como
  texto del PDF: no se puede seleccionar. **Nada se da de alta en AWT** (QA-02, TS-03): `SvgInspector` apunta
  la fuente de cada `<text>` y `<tspan>` con la herencia del fichero (`ImageFile.textFonts`, en orden del
  documento) y `SvgFonts`, el `DomProcessor` de JSVG, le pone una familia clave (`stxt-font-` y
  `FontFile.digest`, la huella del contenido) con su peso y su estilo explícitos, y la da
  `PlatformSupport.customFont` con `Font.createFont`; un texto que repite un `<use>` conserva la fuente de su sitio.
  La caché de fuentes de JSVG es de toda la JVM y no está sincronizada: un SVG con texto se dibuja dentro de
  `PdfImages.JSVG_FONTS`. `SvgCanvas` incrusta el SVG como `data:image/svg+xml`.

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
  `Tolerance.EPSILON`), nunca con un `1e-9` propio. Lo que se ve tiene su propia tolerancia, más
  ancha: `Tolerance.MODEL` (0,01 pt), lo que una caja puede salirse sin desbordar (`Overflow`) y la
  precisión de los volcados (`ReferenceFiles`).
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
  errores; los de `test/examples/` los lee y los maqueta `ExamplesTest`.
- **Medidas de rendimiento:** los tests con `@Tag("perf")` no van en `mvn test` (el `pom.xml`
  excluye el grupo); se ejecutan con `mvn test -Dgroups=perf -DexcludedGroups=none -Dtest=…`.
- **Catálogo de errores** (`errors/ErrorCatalogTest`): `test/errors/<grupo>/<código>.<descripción>.stxt`
  es un documento STXT de verdad, un fichero por caso, en la carpeta de su familia (`document`,
  `schema`, `values`, `structure`, `headings`, `lists`, `tables`, `columns`, `markdown`,
  `images`, `references`, `links`, `contents`, `sections`, `styles`, `combinations`, `overflow`, `fonts`, `limits`; los códigos
  de `stxt-core` van en `schema`). Las tres primeras líneas son comentarios `# Case:`
  (descripción), `# Code:` y `# Line:`, y la línea esperada es la del propio fichero, cabecera incluida (la primera línea
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
  máquinas (`ReferenceFiles.PIXEL_TOLERANCE`, 16 por canal): si aparece modificado en git, el
  resultado ha cambiado; míralo antes de dar el cambio por bueno.
- **Ejemplos** (`examples/ExamplesTest`): cada carpeta de `test/examples/NN-nombre/` es un
  documento completo (`input.stxt`) con su `output.pdf` (ignorado por git, como el de los
  golden), un `page-N.png` por página (versionados, sin contornos de depuración, a 110 ppp) y un
  `expected.stxt` con el volcado de las cajas con `Id`.
  Se maquetan con las **métricas reales** y la carpeta `resources/` (no con `FixedMetrics`). El
  volcado no se calcula a mano: es una instantánea que se genera con `-Dexamples.update=true` y
  se revisa mirando las imágenes; sin esa opción, un `expected.stxt` que falta es un error. Con
  `-Dexamples.only=NN-nombre` se prueba un solo ejemplo. Un PNG solo se reescribe si cambian
  sus píxeles (`golden/ReferenceFiles`, que comparten con `GoldenTest`). Un ejemplo nuevo se escribe como un documento real, con sentido, y se
  añade con su índice en `test/examples/README.md`. Las imágenes de los ejemplos están en
  `resources/images/examples/` y las genera `java tools/ExampleImages.java` (Java2D, sin
  ficheros de terceros); si necesitas una, añádela al generador en vez de subir un binario.
- Los tests de un algoritmo maquetan documentos pequeños con `LayoutFixture` (A4: área útil de
  515 × 762 pt desde (40, 40); `rect(id)` da la caja de borde en coordenadas de página). La fixture
  declara en `Element: Document` `Font: IBM Plex Sans` y `Size: 10` (`Docs.mergeStyles` junta los `Styles` del test con ese y envuelve el contenido en una `Section`); los demás
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
