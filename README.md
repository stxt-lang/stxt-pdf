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
	Language: es
	Title: Informe
	Media:
		Paper: A4
		Margin: 40
	Styles:
		Element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
	Section:
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
    Converter.create().toPdf(doc, out);   // maqueta con las métricas reales de las fuentes y pinta
}
```

`Converter` reúne la configuración de una conversión (carpeta de recursos, documento de estilos aparte y
contornos de depuración) y es inmutable y seguro entre hilos: se crea una vez y se comparte. Para usar las
páginas antes de pintarlas, o pintarlas a PDF y a SVG a la vez:

```java
LaidOutDocument laidOut = converter.layOut(doc);
converter.toPdf(laidOut, out);
List<String> svg = converter.toSvg(laidOut);   // una cadena por página
```

Las reglas de `Styles` pueden estar en un documento aparte (raíz `Styles`), que se pasa junto al
documento y vale menos que el suyo; es opcional y el documento no dice dónde está:

```java
InlineNode styles = new DocumentReader().readStyles(Path.of("styles.stxt"));
Converter converter = Converter.builder().styles(styles).build();   // también styles(Path) y styles(String)
converter.toPdf(doc, out);
```

## Recursos y fuentes

Las fuentes y las imágenes están en una carpeta de recursos, por defecto `resources/` en el
directorio de trabajo, con las subcarpetas `fonts/` e `images/`. Para usar otra:
`Converter.builder().resources(new Resources(Path.of("otra"))).build()`. Los recursos solo se buscan
hacia abajo: no se admiten rutas absolutas, `..` ni enlaces que salgan de la carpeta.

Las imágenes (`Image: logos/acme.png`) son PNG, JPEG o SVG de `images/`, con la ruta relativa a esa
carpeta; se incrustan una vez en el PDF (los PNG y JPEG con su resolución; un SVG, como dibujo vectorial). Los detalles están en el §5.18 de la
especificación.

Solo se usan las fuentes TTF de `fonts/`, que se incrustan en el PDF; cada fichero se reconoce
por la familia, el peso y el estilo que declara. `Font` y `Size` (de `Typography`) son
obligatorios en el texto y no tienen valor por defecto. El repositorio incluye
[IBM Plex Sans](resources/fonts/IBM%20Plex%20Sans/), [Serif](resources/fonts/IBM%20Plex%20Serif/)
y [Mono](resources/fonts/IBM%20Plex%20Mono/) (normal, negrita, cursiva y negrita cursiva de cada
familia), con licencia SIL Open Font License. `Font` y `Size` se suelen declarar en la regla
`Element: Document` de `Styles`, de donde los heredan todas las cajas. Los detalles están en el
§5.11 de la especificación.

## Estado

Los algoritmos de layout funcionan, con paginación, salida SVG de depuración y PDF con las
fuentes incrustadas, etiquetado y con marcadores. Los hitos 1 a 35 están terminados.

| Pieza | Estado |
|---|---|
| Valores, abreviados, `minmax`, `clamp`, `repeat` | Hecho y probado |
| Elementos (`Block`, `Flex`, `Grid`, `Text`, `Relative`, `Absolute`, `Fixed`) y envoltorios `Item` y `Area`, validados con `@stxt.schema` | Hecho y probado |
| Grupos de propiedades: `Box`, `Typography` (se hereda) y `Break` | Hecho y probado |
| Reglas reutilizables (`Styles`): por tipo de elemento (`Element`), clase (`For class`) e id (`For id`), con prioridad fija; la tipografía base va en `Element: Document` | Hecho y probado |
| Documento de estilos aparte (raíz `Styles`): se lee con `DocumentReader.readStyles` y se pasa al maquetar; sus reglas valen menos que las del documento; opcional, sin búsqueda de ficheros | Hecho y probado |
| Encabezados (`Heading 1`…`Heading 6`, con jerarquía validada y corte evitado) y párrafos (`Paragraph`) | Hecho y probado |
| Numeración de los encabezados (`Numbered` en `Typography`, se hereda): `1`, `1.1`, `1.1.1` escrito delante del título y en `{number of: id}`, el índice y los marcadores | Hecho y probado |
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
| Tramos con formato dentro de una línea (negrita, cursiva, subrayado y tachado), con las métricas de cada variante de la fuente | Hecho y probado; se escriben con `Markdown` o con `Inline: markdown` |
| Formato dentro de la línea en un `Text`, un `Paragraph` o una `Cell` (`Inline: markdown`, propiedad de `Typography`, se hereda): la misma sintaxis en línea y los enlaces de `Markdown`, con escapes de CommonMark; sin ella el texto es literal | Hecho y probado |
| Contenido en Markdown (`Markdown`): encabezados, párrafos, listas, tablas GFM, bloques de código, imágenes solas en su párrafo y formato dentro de la línea (también `código`, con `Code font`), traducidos a los elementos de siempre; el resto, error | Hecho y probado |
| Texto preformateado (`White space: pre` y `pre-wrap`, `Tab size`; propiedades de `Typography`, se heredan): espacios y saltos de línea conservados, tabulaciones a la siguiente columna, `pre` no parte las líneas y `pre-wrap` las parte entre palabras, sin justificar | Hecho y probado |
| Reparto flex con congelación | Hecho y probado |
| Grid: áreas, celdas, colocación automática, dimensionado de pistas | Hecho y probado |
| Reglas de corte: viudas, huérfanas, garantía de progreso | Hecho y probado |
| Paginador, caché, contextos de apilamiento, SVG, volcado STXT | Hecho |
| Lectura con la librería oficial `stxt-core` 1.0.3 y validación del schema | Hecho y probado |
| Imágenes (`Image`): PNG, JPEG y SVG de la carpeta de recursos (el SVG, vectorial en el PDF, en un subconjunto cerrado), tamaño natural a 96 ppp, proporción, `Fit` (`contain`, `cover`, `fill`), sin cortes, `Figure` con `Alt` en el PDF etiquetado; también desde Markdown | Hecho y probado |
| Markdown con commonmark-java 0.30.1 (CommonMark, tablas y tachado de GFM, subrayado `++…++`) | Hecho y probado |
| `BlockLayout`, `InlineLayout` (colapso padre-hijo y cajas vacías incluidos) | Hecho y probado |
| Texto justificado (`Align: justify`) | Hecho y probado |
| Cajas `relative`, `absolute` y `fixed` en el layout (las `fixed`, repetidas en cada página) | Hecho y probado |
| Giro (`Rotate`, `Rotate origin`) de `Relative`, `Absolute`, `Fixed` e `Image`: solo pintado, lo girado debe caber en la página y un `Relative` girado no se parte; enlaces girados con `QuadPoints` en el PDF | Hecho y probado |
| Papel con nombre en `Media` (`Paper`: A3–A6, B4, B5, Letter, Legal; `Orientation: landscape`) | Hecho y probado |
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
| Enlaces: `Link` en una caja (URL `https`, `http` o `mailto`, o `#id` de otra caja) y `[texto](destino)` en Markdown; anotaciones del PDF y `Link` en el PDF etiquetado, que envuelve lo que se pulsa; las entradas de un índice llevan a su encabezado | Hecho y probado |
| Accesibilidad del PDF etiquetado: los once ejemplos pasan veraPDF (PDF/UA-1) y todo PDF lo declara (`pdfuaid:part`); un `Text` es un `P`, `Language` por caja da el `Lang` de la estructura, las celdas con `Header` son `TH` con `Scope: Row`, los encabezados con `In contents: false` quedan fuera del índice y las líneas partidas acaban en un espacio para que el texto no se pegue; `Title` es obligatorio | Hecho y probado |
| Aspecto de las cajas: esquinas redondeadas (`Radius`, de 1 a 4 valores, número o %), un color de borde por lado con las uniones en diagonal, degradados lineales en `Background` y colores con opacidad; el PDF usa curvas, sombreados axiales y máscaras suaves | Hecho y probado |
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
24. ~~**Primera página distinta.** Propiedad `Pages` en `Fixed` (`first`, `not-first`, `odd`, `even`, `last`): una cabecera, un pie o un logotipo solo en las páginas que se quiera (caso 17). Sin margen distinto en la primera página ni numeración que no cuente la portada.~~ Hecho.
25. ~~**Estilo de las tablas.** Reglas `Element` por posición (`Head cell`, `Body cell`, `Foot cell`, `Odd row cell`, `Even row cell`), `For column` en `Styles` y `Column align` en la `Table`, sin repetir una clase en cada celda; también en las tablas de Markdown (caso 18).~~ Hecho.
26. ~~**Formato dentro de la línea fuera de Markdown.** Propiedad `Inline: markdown` en `Typography` (se hereda): negrita, cursiva, subrayado, tachado y enlaces dentro de un `Text`, un `Paragraph` o una `Cell`, con los escapes de CommonMark (caso 19).~~ Hecho.
27. ~~**Texto:** mayúsculas automáticas (`Transform`), interletraje (`Letter spacing`) y versalitas (`Small caps`), propiedades de `Typography` que se heredan; `Language` pasa a ser obligatorio en `Document` (caso 20).~~ Hecho.
28. ~~**Aspecto de las cajas.** `Radius` (esquinas redondeadas, también el recorte de una `Image`), `Border color` con un color por lado, degradados lineales en `Background` (`linear(ángulo, color, color…)`) y colores con opacidad (`#RGBA`, `#RRGGBBAA`); sin sombras (caso 21).~~ Hecho.
29. ~~**Texto preformateado y código.** `White space` (`pre`, `pre-wrap`) y `Tab size` en `Typography`, y `Code font` en `Markdown` para el código en línea y los bloques de código (caso 22).~~ Hecho.
30. ~~**Rotación y orientación de la página.** `Rotate` y `Rotate origin` en `Relative`, `Absolute`, `Fixed` e `Image`; `Paper` y `Orientation` en `Media` (caso 23).~~ Hecho.
31. ~~**Imágenes vectoriales (SVG).** `Image` admite SVG (formas, rutas, grupos, degradados, recortes, trazos y texto con las fuentes de `fonts/`), dibujado con JSVG y `pdfbox-graphics2d` como vectores del PDF; ninguna referencia externa (caso 24).~~ Hecho.
32. ~~**Secciones.** `Section`: partes independientes del documento, cada una con su `Media` (heredado por grupos del del documento), sus `Fixed` y su numeración de páginas (`Page numbering`: `continue`, `restart`, `none`); los `Fixed` del documento salen en todas las secciones que los heredan (`Inherit fixed`). La portada a sangre, sin número y sin pie es una sección (caso 25).~~ Hecho.
33. ~~**Numeración de los encabezados.** Propiedad `Numbered` en `Typography` (se hereda): `1`, `1.1`, `1.1.1` escrito delante del título, que también dan `{number of: id}` («la sección 4.2»), el índice y los marcadores del PDF; con `Numbered: false` un encabezado (la portada) no se numera ni cuenta (caso 26).~~ Hecho.

34. ~~**Estilo del bloque de código.** El bloque de código de un `Markdown` es el elemento generado `Code block`, que las reglas `Element: Code block` estilan (fondo, relleno, tamaño…) sin tocar los demás `Text` (caso 27).~~ Hecho.

35. ~~**Estilo de una fila de tabla.** `Class` en la `Row`: sus reglas `For class` se aplican a cada una de sus celdas, con prioridad sobre `For column`, las reglas de posición y `Column align` y por debajo de las clases de la propia celda; el pie de totales de una factura ya no repite la clase en cada celda (caso 28).~~ Hecho.

El plan, las decisiones y el registro de sesiones de los hitos 16 a 35 están en
[`docs/ROADMAP.md`](docs/ROADMAP.md).

Las decisiones de interpretación de cada hito están en [`docs/DECISIONES.md`](docs/DECISIONES.md).

## Estructura

```
src/main/java/dev/stxt/boxes/
  model/        Value, ValueParser, Sides, Insets, Rect, VerticalMetrics, DecorationMetrics,
                FontFace, FontWeight, FontStyle, TextFormat, TextRun, Reference, GridAreas, Area,
                Cell, Tolerance, Colors, Corners, Radii, Paint
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
  paint/        Canvas, StackingContext, SvgCanvas, PdfCanvas, PdfImages, PdfStructure, ImageFit,
                Leaders
  convert/      Converter
  output/       StxtDump

test/           Casos de prueba que no son código: golden/ (resultado calculado a mano), errors/
                (un documento por error, en carpetas por grupo), examples/ (documentos
                completos con PDF e imágenes) y spec/ (el ejemplo de la especificación)
resources/      Carpeta de recursos: fonts/ (IBM Plex Sans, Serif y Mono) e images/
tools/          ExampleImages.java, que genera las imágenes de los ejemplos (SVG las vectoriales)
```

## Validación en el editor

La extensión de STXT para VS Code carga los schemas de la carpeta `.stxt/` del espacio de
trabajo. Copia allí el schema para tener validación y ayuda contextual mientras escribes
documentos.
