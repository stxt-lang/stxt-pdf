# Motor de cajas

Motor de layout de cajas descrito en [STXT](https://stxt.dev), inspirado en el modelo de
maquetación de CSS. Un mismo documento se maqueta en **modo continuo** (una franja de altura
infinita, como un navegador) o en **modo paginado** (páginas, como un PDF). Todas las medidas
están en **puntos tipográficos**.

- **Especificación y referencia de uso:** [`docs/ESPECIFICACION.md`](docs/ESPECIFICACION.md)
- **Template STXT del modelo:** [`src/main/resources/stxt/dev.stxt.boxes.stxt`](src/main/resources/stxt/dev.stxt.boxes.stxt)
- **Documento de ejemplo:** [`src/test/resources/ejemplos/informe.stxt`](src/test/resources/ejemplos/informe.stxt)

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
| Valores, abreviados, `minmax`, `clamp`, `repetir` | Hecho y probado |
| Lector STXT mínimo (`LectorStxtMinimo`) | Hecho y probado |
| Árbol de cajas: herencia, cajas anónimas, validaciones | Hecho y probado |
| Colapso de márgenes, mínimos y máximos, centrado | Hecho y probado |
| Ecuación de cajas absolutas | Hecho y probado |
| Partición de líneas y tamaños intrínsecos del texto | Hecho y probado |
| Reparto flex con congelación | Hecho y probado |
| Grid: áreas, celdas, colocación automática, dimensionado de pistas | Hecho y probado |
| Reglas de corte: viudas, huérfanas, garantía de progreso | Hecho y probado |
| Paginador, caché, contextos de apilamiento, SVG, volcado STXT | Hecho |
| Adaptador a la librería oficial `stxt-core` | Pendiente (hito 1) |
| `BlockLayout`, `InlineLayout` | Pendiente (hito 2) |
| Cajas `absoluta` y `fija` en el layout | Pendiente (hito 3) |
| `FlexLayout` | Pendiente (hito 4) |
| `GridLayout` | Pendiente (hito 5) |
| Fragmentación en los algoritmos, PDF con PDFBox | Pendiente (hito 6) |

Los tests de referencia (`GoldenTest`) están desactivados hasta completar los hitos 2 y 4.

## Hoja de ruta

1. **Árbol de cajas.** Conectar `LectorStxtCore` con `dev.stxt:stxt-core` y validar contra el template.
2. **Bloque y texto.** `BlockLayout` e `InlineLayout` con `MetricaFija`; salida SVG de depuración. Activar `GoldenTest` (caso 01).
3. **Posicionamiento y capas.** `relativa`, `absoluta` con `EcuacionAbsoluta`, `fija` en el `Paginator`.
4. **Intrínsecos y flex.** `intrinsic()` en todos los algoritmos; `FlexLayout` sobre `ResolutorFlex` (caso 02).
5. **Grid.** `GridLayout` sobre `Colocacion` y `TrackSizing`.
6. **Paginación y PDF.** `BreakToken` en los algoritmos, reglas de corte, `PdfCanvas` y métricas reales.
7. **Pulido.** `{pagina}`/`{paginas}`, `Decoracion: repetir`, fragmentación en paralelo, Knuth-Plass.

## Estructura

```
src/main/java/dev/stxt/boxes/
  modelo/       Valor, ValorParser, Lados, Insets, Rect
  stxt/         NodoStxt, LectorDocumento, LectorStxtMinimo, LectorStxtCore
  arbol/        Box, Estilo, Medio, BoxTreeBuilder
  layout/       Motor, LayoutAlgorithm, ConstraintSpace, LayoutResult, LayoutCache,
                IntrinsicSizes, MarginStrut, Dimensiones
    bloque/     BlockLayout
    linea/      InlineLayout, LineBreaker, TextMeasurer, MetricaFija
    flex/       FlexLayout, ResolutorFlex, ItemFlex
    grid/       GridLayout, TrackSizing, Pista, Colocacion, Celda, AreasGrid
    posicion/   EcuacionAbsoluta
  fragmentos/   Fragment, BreakToken, Paginator, Pagina, ReglasCorte
  pintado/      Canvas, StackingContext, SvgCanvas
  salida/       VolcadoStxt
```

## Validación en el editor

La extensión de STXT para VS Code carga los templates de la carpeta `.stxt/` del espacio de
trabajo. Copia allí el template para tener validación y ayuda contextual mientras escribes
documentos.
