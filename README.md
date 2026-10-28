# Motor de cajas

Motor de layout de cajas descrito en [STXT](https://stxt.dev), inspirado en el modelo de
maquetación de CSS. Los documentos se maquetan en **páginas** de tamaño fijo, como un PDF, y
las cajas que no caben se parten entre páginas. Todas las medidas están en **puntos
tipográficos**.

- **Especificación y referencia de uso:** [`docs/ESPECIFICACION.md`](docs/ESPECIFICACION.md)
- **Schema STXT del modelo** (`@stxt.schema`): [`src/main/resources/stxt/dev.stxt.boxes.stxt`](src/main/resources/stxt/dev.stxt.boxes.stxt)
- **Ejemplos:** [`test/examples/`](test/examples): once documentos completos (factura, carta, currículum, informe anual, boletín, carta de restaurante, contrato, manual, catálogo, certificado y horario), con una imagen por página y su PDF (que se genera al ejecutar los tests)
- **Documento de la especificación:** [`test/spec/report.stxt`](test/spec/report.stxt)

Los nombres de nodos y valores de los documentos, y los identificadores del código, están en
inglés; la documentación y los comentarios, en castellano.

Un documento se escribe con elementos (`Block`, `Flex`, `Grid`, `Text`, `Paragraph`,
`Heading 1`…`Heading 6`, `List`, `Table`, `Multicol`, `Relative`, `Absolute`, `Fixed`) que solo admiten las propiedades que tienen sentido en ellos; los hijos de un `Flex` van
en `Item` y los de un `Grid`, en `Area`. Las propiedades de cada caja se agrupan en `Box` (el
modelo de caja), `Typography` (el texto, que se hereda) y `Break` (los cortes de página):

```
Document (dev.stxt.boxes):
	Media:
		Width: 595
		Height: 842
		Margin: 40
	Typography:
		Font: IBM Plex Sans
		Size: 10
	Text: Un título
		Box:
			Margin: 0 0 10 0
		Typography:
			Size: 20
	Grid:
		Columns: 150 1fr
		Gap: 20
		Area:
			Text: Columna lateral
		Area:
			Text:
				Content >>
					Texto largo, en un bloque.
```

## Requisitos y uso

Java 17 o superior y Maven.

```bash
mvn test
```

Para obtener un PDF, el layout debe usar las métricas reales de las fuentes:

```java
InlineNode doc = new DocumentReader().read(Path.of("report.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("report.pdf"))) {
    PdfWriter.write(doc, out);   // maqueta con new Engine(new PdfMetrics()) y pinta
}
```

## Recursos y fuentes

Las fuentes y las imágenes están en una carpeta de recursos, por defecto `resources/` en el
directorio de trabajo, con las subcarpetas `fonts/` e `images/`. Para usar otra:
`PdfWriter.write(doc, new Resources(Path.of("otra")), out, false)`. Los recursos solo se buscan
hacia abajo: no se admiten rutas absolutas, `..` ni enlaces que salgan de la carpeta.

Las imágenes (`Image: logos/acme.png`) son PNG o JPEG de `images/`, con la ruta relativa a esa
carpeta; se incrustan una vez en el PDF con su resolución. Los detalles están en el §5.18 de la
especificación.

Solo se usan las fuentes TTF de `fonts/`, que se incrustan en el PDF; cada fichero se reconoce
por la familia, el peso y el estilo que declara. `Font` y `Size` (de `Typography`) son
obligatorios en el texto y no tienen valor por defecto. El repositorio incluye
[IBM Plex Sans](resources/fonts/IBM%20Plex%20Sans/), [Serif](resources/fonts/IBM%20Plex%20Serif/)
y [Mono](resources/fonts/IBM%20Plex%20Mono/) (normal, negrita, cursiva y negrita cursiva de cada
familia), con licencia SIL Open Font License. `Font` y `Size` se suelen declarar en el
`Typography` de `Document`, de donde los heredan todas las cajas. Los detalles están en el
§5.11 de la especificación.

## Estado

Los algoritmos de layout funcionan, con paginación, salida SVG de depuración y PDF con las
fuentes incrustadas, etiquetado y con marcadores. Los hitos 1 a 23 están terminados.

| Pieza | Estado |
|---|---|
| Valores, abreviados, `minmax`, `clamp`, `repeat` | Hecho y probado |
| Elementos (`Block`, `Flex`, `Grid`, `Text`, `Relative`, `Absolute`, `Fixed`) y envoltorios `Item` y `Area`, validados con `@stxt.schema` | Hecho y probado |
| Grupos de propiedades: `Box`, `Typography` (se hereda) y `Break` | Hecho y probado |
| Reglas reutilizables (`Styles`): por tipo de elemento (`Element`), clase (`For class`) e id (`For id`), con prioridad fija | Hecho y probado |
| Encabezados (`Heading 1`…`Heading 6`, con jerarquía validada y corte evitado) y párrafos (`Paragraph`) | Hecho y probado |
| Listas (`List`, `List item`): viñetas y numeraciones, marcador alineado con la primera línea, listas anidadas y partidas entre páginas | Hecho y probado |
| Tablas (`Table`, `Head`, `Body`, `Foot`, `Row`, `Cell`): columnas con las pistas de grid, `Span`, cabecera y pie repetidos en cada página | Hecho y probado |
| Bloques en columnas (`Multicol`): `Count`, `Column width`, `Column gap`, línea entre columnas, equilibrado (`Fill`), cortes de columna y continuación en la página siguiente | Hecho y probado |
| PDF etiquetado (árbol de estructura, *artifacts*, listas con `Lbl` y `LBody`, tablas con `THead`, `TBody`, `TFoot`, `TH` y `TD`) y marcadores de los encabezados | Hecho y probado |
| Árbol de cajas: herencia, validaciones | Hecho y probado |
| Validación estricta: códigos de error, línea del nodo culpable y catálogo de errores (`test/errors`) | Hecho y probado |
| Desbordamientos como error (texto, cajas, alto fijado, página) | Hecho y probado |
| Colapso de márgenes, mínimos y máximos, centrado | Hecho y probado |
| Ecuación de cajas absolutas | Hecho y probado |
| Partición de líneas y tamaños intrínsecos del texto | Hecho y probado |
| Tramos con formato dentro de una línea (negrita, cursiva, subrayado y tachado), con las métricas de cada variante de la fuente | Hecho y probado; se escriben con `Markdown` |
| Contenido en Markdown (`Markdown`): encabezados, párrafos, listas, tablas GFM, imágenes solas en su párrafo y formato dentro de la línea, traducidos a los elementos de siempre; el resto, error | Hecho y probado |
| Reparto flex con congelación | Hecho y probado |
| Grid: áreas, celdas, colocación automática, dimensionado de pistas | Hecho y probado |
| Reglas de corte: viudas, huérfanas, garantía de progreso | Hecho y probado |
| Paginador, caché, contextos de apilamiento, SVG, volcado STXT | Hecho |
| Lectura con la librería oficial `stxt-core` 1.0.3 y validación del schema | Hecho y probado |
| Imágenes (`Image`): PNG y JPEG de la carpeta de recursos, tamaño natural a 96 ppp, proporción, `Fit` (`contain`, `cover`, `fill`), sin cortes, `Figure` con `Alt` en el PDF etiquetado; también desde Markdown | Hecho y probado |
| Markdown con commonmark-java 0.30.1 (CommonMark, tablas y tachado de GFM, subrayado `++…++`) | Hecho y probado |
| `BlockLayout`, `InlineLayout` (colapso padre-hijo y cajas vacías incluidos) | Hecho y probado |
| Texto justificado (`Align: justify`) | Hecho y probado |
| Cajas `relative`, `absolute` y `fixed` en el layout (las `fixed`, repetidas en cada página) | Hecho y probado |
| `FlexLayout` e intrínsecos de flex | Hecho y probado |
| `GridLayout` (con `Flow: column`) e intrínsecos de grid | Hecho y probado |
| Fragmentación de bloque y texto: elección del corte, viudas y huérfanas, `avoid`, cortes forzados, truncado de márgenes, `Decoration: slice`, `Height` repartido entre fragmentos | Hecho y probado |
| Fragmentación de flex (entre líneas o ítems) y grid (entre filas), con los ítems de una fila más alta que la página partidos en paralelo | Hecho y probado |
| Salida PDF con PDFBox (`PdfCanvas`) y métricas reales de las fuentes (`PdfMetrics`) | Hecho y probado |
| Numeración de páginas (`{page}`/`{pages}`) y `Decoration: clone` | Hecho y probado |
| Reparto de líneas con Knuth-Plass (`Text wrap: pretty`) | Hecho y probado |
| Carpeta de recursos (`Resources`), fuentes TTF incrustadas, `Weight` y `Style`, línea base real | Hecho y probado |
| Referencias a otras cajas en el texto: `{page of: id}` (la página donde empieza la caja, resuelta paginando hasta que converja) y `{number of: id}` (el número de un ítem de una lista numerada) | Hecho y probado |
| Índice automático (`Contents`): una entrada por encabezado de los niveles de `Levels` (también los de un `Markdown`), con título, guía (`dots`, `rule`, `none`) y página; se estila con `Element: Contents entry 1`…`6`; `TOC` y `TOCI` en el PDF etiquetado | Hecho y probado |
| Enlaces: `Link` en una caja (URL `https`, `http` o `mailto`, o `#id` de otra caja) y `[texto](destino)` en Markdown; anotaciones del PDF y `Link` en el PDF etiquetado; las entradas de un índice llevan a su encabezado | Hecho y probado |
| Ejemplos de documentos completos (`test/examples/`): once documentos con una imagen por página versionada, comprobados por `ExamplesTest` | Hecho y probado |

Los tests de referencia (`GoldenTest`) están activos y no se salta ninguno. Cada
caso deja un SVG de depuración en `target/golden-svg/` y, en su propia carpeta de
`test/golden/`, el PDF (`output.pdf`, ignorado por git) y una imagen por página
(`page-N.png`, versionada): si un cambio altera el resultado, la imagen aparece modificada en
git. Como las fuentes van incrustadas, las imágenes no dependen de las fuentes del sistema;
aún pueden variar detalles del suavizado con otra versión de Java. `PdfCanvasTest` deja sus PDF
en `target/pdf/`.

Los [ejemplos](test/examples) (`ExamplesTest`) funcionan igual, pero son documentos completos
maquetados con las fuentes y las imágenes reales y sin contornos. Dejan el PDF (`output.pdf`,
ignorado por git) y una imagen por página (`page-N.png`, versionada) en su carpeta.

## Hoja de ruta

1. ~~**Árbol de cajas.** Conectar `DocumentReader` con `dev.stxt:stxt-core` y validar contra el template.~~ Hecho.
2. ~~**Bloque y texto.** `BlockLayout` e `InlineLayout` con `FixedMetrics`; salida SVG de depuración. Activar `GoldenTest` (caso 01).~~ Hecho.
3. ~~**Posicionamiento y capas.** `relative`, `absolute` con `AbsoluteEquation`, `fixed` en el `Paginator` (caso 03).~~ Hecho.
4. ~~**Intrínsecos y flex.** `intrinsic()` en todos los algoritmos; `FlexLayout` sobre `FlexResolver` (caso 02).~~ Hecho.
5. ~~**Grid.** `GridLayout` sobre `Placement` y `TrackSizing` (casos 04 y 05, el ejemplo del §10).~~ Hecho.
6. ~~**Paginación y PDF.** `BreakToken` en los algoritmos, reglas de corte, `PdfCanvas` y métricas reales (caso 06).~~ Hecho.
7. ~~**Pulido.** `{page}`/`{pages}`, `Decoration: clone`, fragmentación en paralelo, Knuth-Plass (caso 07).~~ Hecho.
8. ~~**Fuentes.** Carpeta de recursos, fuentes TTF incrustadas, `Font weight` y `Font style`, línea base real.~~ Hecho.
9. ~~**Elementos.** `Block`, `Flex`, `Grid`, `Text`, `Relative`, `Absolute`, `Fixed`, `Item` y `Cell` (hoy `Area`, hito 14) en lugar de `Box` con `Layout` y `Position`; `@stxt.schema` en lugar de `@stxt.template`.~~ Hecho.
10. ~~**Grupos.** Las propiedades de cada caja en `Box`, `Typography` (se hereda) y `Break`, con nombres cortos.~~ Hecho.
11. ~~**Reglas reutilizables.** `Styles` con reglas `Element`, `For class` y `For id`; `Class` en los elementos (caso 08).~~ Hecho.
12. ~~**Encabezados y párrafos.** `Heading 1`…`Heading 6` y `Paragraph`; PDF etiquetado y marcadores (caso 09).~~ Hecho.
13. ~~**Listas.** `List` con `Marker`, `Start` y `List item` con `Number`; el marcador cuelga en la sangría, alineado con la primera línea (caso 10).~~ Hecho.
14. ~~**Tablas.** `Table` con `Head`, `Body` y `Foot`, filas `Row` y celdas `Cell` con `Span`, maquetada como un grid; cabecera y pie repetidos en cada página (caso 11). El `Cell` de `Grid` pasa a llamarse `Area`.~~ Hecho.

15. ~~**Columnas.** `Multicol` con `Count`, `Column width`, `Column gap`, `Rule`, `Rule color` y `Fill`; las columnas son fragmentainers dentro de la página, equilibradas al final, y `Before`/`After: column` (caso 12).~~ Hecho.
16. ~~**Tramos.** Negrita, cursiva, subrayado y tachado dentro de una misma línea, con las métricas de cada variante de la fuente.~~ Hecho (caso 13, con el hito 17).
17. ~~**Markdown.** Elemento `Markdown` con un subconjunto cerrado (formato dentro de la línea, encabezados, listas y tablas) que se traduce a los elementos que ya existen (caso 13).~~ Hecho.
18. ~~**Imágenes.** Elemento `Image` con PNG y JPEG de `images/`, dimensionado como un elemento reemplazado, `Alt` y `Fit`, sin cortes; imágenes en Markdown (caso 14).~~ Hecho.

19. ~~**Pruebas en `test/`.** Los casos de prueba pasan de `src/test/resources` a `test/` (`golden/`, `errors/`, `examples/`, `spec/`).~~ Hecho.
20. ~~**Catálogo de errores.** Un fichero STXT por caso, con el documento real, en carpetas por grupo de errores (`test/errors/<grupo>/`).~~ Hecho.
21. ~~**Ejemplos.** Once documentos completos en `test/examples/`, con PDF e imágenes limpios; IBM Plex Serif y Mono, e imágenes generadas con `tools/ExampleImages.java`.~~ Hecho.

22. ~~**Referencias a páginas e índice.** `{page of: id}` y `{number of: id}` en el texto, paginando hasta que las páginas converjan, y el elemento `Contents`, que genera el índice con guía y páginas (caso 15).~~ Hecho.
23. ~~**Enlaces.** Propiedad `Link` en las cajas y enlaces de Markdown, a una URL o a otra caja del documento, con anotaciones de enlace en el PDF; las entradas del índice llevan a su encabezado (caso 16).~~ Hecho.
24. **Primera página distinta y páginas con nombre.** Pendiente.
25. ~~**Estilo de las tablas.** Reglas `Element` por posición (`Head cell`, `Body cell`, `Foot cell`, `Odd row cell`, `Even row cell`), `For column` en `Styles` y `Column align` en la `Table`, sin repetir una clase en cada celda; también en las tablas de Markdown (caso 18).~~ Hecho.
26. **Formato dentro de la línea fuera de Markdown.** Pendiente.
27. **Texto:** mayúsculas automáticas, interletraje y versalitas. Pendiente.
28. **Aspecto de las cajas:** esquinas, bordes, sombras y degradados. Pendiente.
29. **Texto preformateado y código.** Pendiente.
30. **Rotación y orientación de la página.** Pendiente.
31. **Imágenes vectoriales (SVG).** Pendiente.

El plan, las decisiones y el registro de sesiones de los hitos 16 a 31 están en
[`docs/ROADMAP.md`](docs/ROADMAP.md).

Las decisiones de interpretación de cada hito están en [`docs/DECISIONES.md`](docs/DECISIONES.md).

## Estructura

```
src/main/java/dev/stxt/boxes/
  model/        Value, ValueParser, Sides, Insets, Rect, VerticalMetrics, DecorationMetrics,
                FontFace, FontWeight, FontStyle, TextFormat, TextRun, Reference, GridAreas, Area,
                Cell, Tolerance
  stxt/         DocumentReader (stxt-core + schema)
  resources/    Resources, FontLibrary, FontFile, ImageLibrary, ImageFile, ResourceException,
                ImageFormatException
  tree/         Box, Element, Style, Media, BoxTreeBuilder (y ValueReader, PropertyReader,
                StyleRules, TableBuilder, ListBuilder, MarkdownBuilder, ContentsBuilder,
                ModelValidator…), References,
                InvalidModelException
  layout/       Engine, LayoutAlgorithm, ConstraintSpace, LayoutResult, LayoutCache,
                IntrinsicSizes, BoxModel, MarginStrut, AdjoiningMargins, Sizing, PendingAbsolute,
                Slice, UnitBreaker, Paginator, Messages
    block/      BlockLayout
    inline/     InlineLayout, LineBreaker, PageNumbers, TextMeasurer, FixedMetrics, PdfMetrics
    flex/       FlexLayout, FlexResolver, FlexItem
    grid/       GridLayout, TrackSizing, Track, Placement, ItemContribution
    position/   AbsoluteEquation, AbsoluteLayout, RelativeOffset
    table/      TableLayout
    multicol/   MulticolLayout, Columns
    image/      ImageLayout, Images, NaturalSize
  fragment/     Fragment, BreakToken, BreakAppeal, Unit, Page, BreakRules, TextLine
  paint/        Canvas, StackingContext, SvgCanvas, PdfCanvas, PdfStructure, ImageFit, Leaders,
                PdfWriter
  output/       StxtDump

test/           Casos de prueba que no son código: golden/ (resultado calculado a mano), errors/
                (un documento por error, en carpetas por grupo), examples/ (documentos
                completos con PDF e imágenes) y spec/ (el ejemplo de la especificación)
resources/      Carpeta de recursos: fonts/ (IBM Plex Sans, Serif y Mono) e images/
tools/          ExampleImages.java, que genera las imágenes de los ejemplos
```

## Validación en el editor

La extensión de STXT para VS Code carga los schemas de la carpeta `.stxt/` del espacio de
trabajo. Copia allí el schema para tener validación y ayuda contextual mientras escribes
documentos.
