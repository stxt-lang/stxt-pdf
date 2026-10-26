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

## Estado

Es un **esqueleto**: la arquitectura, las interfaces y las piezas puras del motor están
implementadas y probadas; los algoritmos de layout completos están por hacer, siguiendo la hoja
de ruta.

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
| `BlockLayout`, `InlineLayout` | Pendiente (hito 2) |
| Cajas `absolute` y `fixed` en el layout | Pendiente (hito 3) |
| `FlexLayout` | Pendiente (hito 4) |
| `GridLayout` | Pendiente (hito 5) |
| Fragmentación en los algoritmos, PDF con PDFBox | Pendiente (hito 6) |

Los tests de referencia (`GoldenTest`) están desactivados hasta completar los hitos 2 y 4.

## Hoja de ruta

1. ~~**Árbol de cajas.** Conectar `DocumentReader` con `dev.stxt:stxt-core` y validar contra el template.~~ Hecho.
2. **Bloque y texto.** `BlockLayout` e `InlineLayout` con `FixedMetrics`; salida SVG de depuración. Activar `GoldenTest` (caso 01).
3. **Posicionamiento y capas.** `relative`, `absolute` con `AbsoluteEquation`, `fixed` en el `Paginator`.
4. **Intrínsecos y flex.** `intrinsic()` en todos los algoritmos; `FlexLayout` sobre `FlexResolver` (caso 02).
5. **Grid.** `GridLayout` sobre `Placement` y `TrackSizing`.
6. **Paginación y PDF.** `BreakToken` en los algoritmos, reglas de corte, `PdfCanvas` y métricas reales.
7. **Pulido.** `{page}`/`{pages}`, `Decoration: clone`, fragmentación en paralelo, Knuth-Plass.

## Estructura

```
src/main/java/dev/stxt/boxes/
  model/        Value, ValueParser, Sides, Insets, Rect
  stxt/         DocumentReader (stxt-core + template)
  tree/         Box, Style, Media, BoxTreeBuilder, InvalidModelException
  layout/       Engine, LayoutAlgorithm, ConstraintSpace, LayoutResult, LayoutCache,
                IntrinsicSizes, MarginStrut, Sizing, PendingAbsolute
    block/      BlockLayout
    inline/     InlineLayout, LineBreaker, TextMeasurer, FixedMetrics
    flex/       FlexLayout, FlexResolver, FlexItem
    grid/       GridLayout, TrackSizing, Track, Placement, Cell, GridAreas, Area, ItemContribution
    position/   AbsoluteEquation
  fragment/     Fragment, BreakToken, Paginator, Page, BreakRules, TextLine
  paint/        Canvas, StackingContext, SvgCanvas
  output/       StxtDump
```

## Validación en el editor

La extensión de STXT para VS Code carga los templates de la carpeta `.stxt/` del espacio de
trabajo. Copia allí el template para tener validación y ayuda contextual mientras escribes
documentos.
