# Motor de cajas

Motor de layout de cajas descrito en [STXT](https://stxt.dev), inspirado en el modelo de
maquetación de CSS. Los documentos se maquetan en **páginas** de tamaño fijo, como un PDF, y
las cajas que no caben se parten entre páginas. Todas las medidas están en **puntos
tipográficos**.

- **Especificación y referencia de uso:** [`docs/ESPECIFICACION.md`](docs/ESPECIFICACION.md)
- **Schema STXT del modelo** (`@stxt.schema`): [`src/main/resources/stxt/dev.stxt.boxes.stxt`](src/main/resources/stxt/dev.stxt.boxes.stxt)
- **Documento de ejemplo:** [`src/test/resources/examples/report.stxt`](src/test/resources/examples/report.stxt)

Los nombres de nodos y valores de los documentos, y los identificadores del código, están en
inglés; la documentación y los comentarios, en castellano.

Un documento se escribe con elementos (`Block`, `Flex`, `Grid`, `Text`, `Paragraph`,
`Heading 1`…`Heading 6`, `Relative`, `Absolute`, `Fixed`) que solo admiten las propiedades que tienen sentido en ellos; los hijos de un `Flex` van
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

Solo se usan las fuentes TTF de `fonts/`, que se incrustan en el PDF; cada fichero se reconoce
por la familia, el peso y el estilo que declara. `Font` y `Size` (de `Typography`) son
obligatorios en el texto y no tienen valor por defecto. El repositorio incluye
[IBM Plex Sans](resources/fonts/IBM%20Plex%20Sans/) (normal, negrita, cursiva y negrita
cursiva), con licencia SIL Open Font License. `Font` y `Size` se suelen declarar en el
`Typography` de `Document`, de donde los heredan todas las cajas. Los detalles están en el
§5.11 de la especificación.

## Estado

Los algoritmos de layout funcionan, con paginación, salida SVG de depuración y PDF con las
fuentes incrustadas, etiquetado y con marcadores. Los hitos 1 a 14 están terminados; el
siguiente, en
[`docs/ROADMAP.md`](docs/ROADMAP.md).

| Pieza | Estado |
|---|---|
| Valores, abreviados, `minmax`, `clamp`, `repeat` | Hecho y probado |
| Elementos (`Block`, `Flex`, `Grid`, `Text`, `Relative`, `Absolute`, `Fixed`) y envoltorios `Item` y `Area`, validados con `@stxt.schema` | Hecho y probado |
| Grupos de propiedades: `Box`, `Typography` (se hereda) y `Break` | Hecho y probado |
| Reglas reutilizables (`Styles`): por tipo de elemento (`Element`), clase (`For class`) e id (`For id`), con prioridad fija | Hecho y probado |
| Encabezados (`Heading 1`…`Heading 6`, con jerarquía validada y corte evitado) y párrafos (`Paragraph`) | Hecho y probado |
| Listas (`List`, `List item`): viñetas y numeraciones, marcador alineado con la primera línea, listas anidadas y partidas entre páginas | Hecho y probado |
| Tablas (`Table`, `Head`, `Body`, `Foot`, `Row`, `Cell`): columnas con las pistas de grid, `Span`, cabecera y pie repetidos en cada página | Hecho y probado |
| PDF etiquetado (árbol de estructura, *artifacts*, listas con `Lbl` y `LBody`, tablas con `THead`, `TBody`, `TFoot`, `TH` y `TD`) y marcadores de los encabezados | Hecho y probado |
| Árbol de cajas: herencia, validaciones | Hecho y probado |
| Validación estricta: códigos de error, línea del nodo culpable y catálogo de errores (`src/test/resources/errors`) | Hecho y probado |
| Desbordamientos como error (texto, cajas, alto fijado, página) | Hecho y probado |
| Colapso de márgenes, mínimos y máximos, centrado | Hecho y probado |
| Ecuación de cajas absolutas | Hecho y probado |
| Partición de líneas y tamaños intrínsecos del texto | Hecho y probado |
| Reparto flex con congelación | Hecho y probado |
| Grid: áreas, celdas, colocación automática, dimensionado de pistas | Hecho y probado |
| Reglas de corte: viudas, huérfanas, garantía de progreso | Hecho y probado |
| Paginador, caché, contextos de apilamiento, SVG, volcado STXT | Hecho |
| Lectura con la librería oficial `stxt-core` 1.0.3 y validación del schema | Hecho y probado |
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

Los tests de referencia (`GoldenTest`) están activos y no se salta ninguno. Cada
caso deja un SVG de depuración en `target/golden-svg/` y, en su propia carpeta de
`src/test/resources/golden/`, el PDF (`output.pdf`, ignorado por git) y una imagen por página
(`page-N.png`, versionada): si un cambio altera el resultado, la imagen aparece modificada en
git. Como las fuentes van incrustadas, las imágenes no dependen de las fuentes del sistema;
aún pueden variar detalles del suavizado con otra versión de Java. `PdfCanvasTest` deja sus PDF
en `target/pdf/`.

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

El siguiente hito (15: bloque en columnas), con sus decisiones abiertas, tareas y el registro de cada sesión, está en
[`docs/ROADMAP.md`](docs/ROADMAP.md).

## Estructura

```
src/main/java/dev/stxt/boxes/
  model/        Value, ValueParser, Sides, Insets, Rect, VerticalMetrics, FontFace, FontWeight,
                FontStyle, GridAreas, Area, Cell, Tolerance
  stxt/         DocumentReader (stxt-core + schema)
  resources/    Resources, FontLibrary, FontFile, ResourceException
  tree/         Box, Element, Style, Media, BoxTreeBuilder (y ValueReader, PropertyReader,
                StyleRules, TableBuilder, ListBuilder, ModelValidator…), InvalidModelException
  layout/       Engine, LayoutAlgorithm, ConstraintSpace, LayoutResult, LayoutCache,
                IntrinsicSizes, BoxModel, MarginStrut, AdjoiningMargins, Sizing, PendingAbsolute,
                Slice, UnitBreaker, Paginator, Messages
    block/      BlockLayout
    inline/     InlineLayout, LineBreaker, PageNumbers, TextMeasurer, FixedMetrics, PdfMetrics
    flex/       FlexLayout, FlexResolver, FlexItem
    grid/       GridLayout, TrackSizing, Track, Placement, ItemContribution
    position/   AbsoluteEquation, AbsoluteLayout, RelativeOffset
    table/      TableLayout
  fragment/     Fragment, BreakToken, BreakAppeal, Unit, Page, BreakRules, TextLine
  paint/        Canvas, StackingContext, SvgCanvas, PdfCanvas, PdfStructure
  output/       StxtDump
```

## Validación en el editor

La extensión de STXT para VS Code carga los schemas de la carpeta `.stxt/` del espacio de
trabajo. Copia allí el schema para tener validación y ayuda contextual mientras escribes
documentos.
