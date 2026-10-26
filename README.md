# Motor de cajas

Motor de layout de cajas descrito en [STXT](https://stxt.dev), inspirado en el modelo de
maquetación de CSS. Un mismo documento se maqueta en **modo continuo** (una franja de altura
infinita, como un navegador) o en **modo paginado** (páginas, como un PDF). Todas las medidas
están en **puntos tipográficos**.

- **Especificación y referencia de uso:** [`docs/ESPECIFICACION.md`](docs/ESPECIFICACION.md)
- **Template STXT del modelo:** [`src/main/resources/stxt/dev.stxt.boxes.stxt`](src/main/resources/stxt/dev.stxt.boxes.stxt)
- **Documento de ejemplo:** [`src/test/resources/examples/report.stxt`](src/test/resources/examples/report.stxt)

Los nombres de nodos y valores de los documentos, y los identificadores del código, están en
inglés; la documentación y los comentarios, en castellano.

## Requisitos y uso

Java 17 o superior y Maven.

```bash
mvn test
```

Para obtener un PDF, el layout debe usar las métricas reales de las fuentes:

```java
InlineNode doc = new DocumentReader().read(Path.of("report.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("report.pdf"))) {
    PdfCanvas.write(doc, out);   // maqueta con new Engine(new PdfMetrics()) y pinta
}
```

## Recursos y fuentes

Las fuentes y las imágenes están en una carpeta de recursos, por defecto `resources/` en el
directorio de trabajo, con las subcarpetas `fonts/` e `images/`. Para usar otra:
`PdfCanvas.write(doc, new Resources(Path.of("otra")), out, false)`. Los recursos solo se buscan
hacia abajo: no se admiten rutas absolutas, `..` ni enlaces que salgan de la carpeta.

Solo se usan las fuentes TTF de `fonts/`, que se incrustan en el PDF; cada fichero se reconoce
por la familia, el peso y el estilo que declara. `Font` es obligatoria en toda caja con texto
y no tiene valor por defecto. El repositorio incluye
[IBM Plex Sans](resources/fonts/IBM%20Plex%20Sans/) (normal, negrita, cursiva y negrita
cursiva), con licencia SIL Open Font License. Los detalles están en el §5.11 de la
especificación.

## Estado

Los algoritmos de layout funcionan en los dos modos, continuo y paginado, con salida SVG de
depuración y PDF con las fuentes incrustadas. Los ocho hitos de la hoja de ruta están
terminados.

| Pieza | Estado |
|---|---|
| Valores, abreviados, `minmax`, `clamp`, `repeat` | Hecho y probado |
| Árbol de cajas: herencia, cajas anónimas, validaciones | Hecho y probado |
| Colapso de márgenes, mínimos y máximos, centrado | Hecho y probado |
| Ecuación de cajas absolutas | Hecho y probado |
| Partición de líneas y tamaños intrínsecos del texto | Hecho y probado |
| Reparto flex con congelación | Hecho y probado |
| Grid: áreas, celdas, colocación automática, dimensionado de pistas | Hecho y probado |
| Reglas de corte: viudas, huérfanas, garantía de progreso | Hecho y probado |
| Paginador, caché, contextos de apilamiento, SVG, volcado STXT | Hecho |
| Lectura con la librería oficial `stxt-core` 1.0.3 y validación del template | Hecho y probado |
| `BlockLayout`, `InlineLayout` (colapso padre-hijo y cajas vacías incluidos) | Hecho y probado |
| Texto justificado (`Text align: justify`) | Hecho y probado |
| Cajas `relative`, `absolute` y `fixed` en el layout (las `fixed`, repetidas en cada página) | Hecho y probado |
| `FlexLayout` e intrínsecos de flex | Hecho y probado |
| `GridLayout` (con `Flow: column`) e intrínsecos de grid | Hecho y probado |
| Fragmentación de bloque y texto: elección del corte, viudas y huérfanas, `avoid`, cortes forzados, truncado de márgenes, `Decoration: slice`, `Height` repartido entre fragmentos | Hecho y probado |
| Fragmentación de flex (entre líneas o ítems) y grid (entre filas), con los ítems de una fila más alta que la página partidos en paralelo | Hecho y probado |
| Salida PDF con PDFBox (`PdfCanvas`) y métricas reales de las fuentes (`PdfMetrics`) | Hecho y probado |
| Numeración de páginas (`{page}`/`{pages}`) y `Decoration: clone` | Hecho y probado |
| Reparto de líneas con Knuth-Plass (`Text wrap: pretty`) | Hecho y probado |
| Carpeta de recursos (`Resources`), fuentes TTF incrustadas, `Font weight` y `Font style`, línea base real | Hecho y probado |

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

## Estructura

```
src/main/java/dev/stxt/boxes/
  model/        Value, ValueParser, Sides, Insets, Rect, VerticalMetrics
  stxt/         DocumentReader (stxt-core + template)
  resources/    Resources, FontLibrary, FontFile, ResourceException
  tree/         Box, Style, FontFace, Media, BoxTreeBuilder, InvalidModelException
  layout/       Engine, LayoutAlgorithm, ConstraintSpace, LayoutResult, LayoutCache,
                IntrinsicSizes, BoxModel, MarginStrut, AdjoiningMargins, Sizing, PendingAbsolute,
                Slice, UnitBreaker
    block/      BlockLayout
    inline/     InlineLayout, LineBreaker, PageNumbers, TextMeasurer, FixedMetrics, PdfMetrics
    flex/       FlexLayout, FlexResolver, FlexItem
    grid/       GridLayout, TrackSizing, Track, Placement, Cell, GridAreas, Area, ItemContribution
    position/   AbsoluteEquation, AbsoluteLayout, RelativeOffset
  fragment/     Fragment, BreakToken, Paginator, Page, BreakRules, TextLine
  paint/        Canvas, StackingContext, SvgCanvas, PdfCanvas
  output/       StxtDump
```

## Validación en el editor

La extensión de STXT para VS Code carga los templates de la carpeta `.stxt/` del espacio de
trabajo. Copia allí el template para tener validación y ayuda contextual mientras escribes
documentos.
