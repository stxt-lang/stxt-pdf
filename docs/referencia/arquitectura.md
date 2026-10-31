# Arquitectura

Qué hay en el código, cómo se relaciona y cuáles son las clases principales. Es un mapa: el detalle de cada parte está en
las páginas de [implementación](README.md#implementación) y en el javadoc.

Java 17 y Maven: artefacto `dev.stxt:stxt-pdf`, package raíz `dev.stxt.pdf`. Las dependencias de terceros son
`stxt-core` (leer y validar STXT), PDFBox (escribir el PDF), commonmark-java (Markdown), JSVG y `pdfbox-graphics2d`
(dibujar SVG en el PDF). En los tests, JUnit 5, veraPDF y PIT.

## Flujo de datos

```
texto STXT ──DocumentReader──▶ InlineNode (stxt-core)
           ──BoxTreeBuilder──▶ árbol de Box (con su Style), PageSetup y las Section
           ──Engine.layOutDocument──▶ Paginator ──▶ Overflow ──▶ LaidOutDocument (páginas con sus Fragment)
           ──▶ StackingContext + Canvas ──▶ PdfCanvas (PDF) · SvgCanvas (SVG de depuración)
           ──▶ StxtDump (volcado de los tests)
```

1. **Leer.** `DocumentReader` parsea el texto con `stxt-core`, lo valida contra el schema y exige una única raíz
   `Document` (o `Styles`, en un documento de estilos aparte).
2. **Construir el árbol.** `BoxTreeBuilder` traduce cada elemento a una `Box` con su `Style`, aplica la herencia y las
   reglas de `Styles`, comprueba las reglas que el schema no puede expresar y cierra el árbol, que desde ahí es de solo
   lectura.
3. **Maquetar.** `Engine` despacha cada caja a su algoritmo de layout. El `Paginator` reparte cada sección en páginas,
   repite la paginación hasta que las referencias convergen y coloca los `Page overlay`. `Overflow` comprueba que nada desborda.
   El resultado es un `LaidOutDocument`: una lista de `Page` con su árbol de `Fragment`.
4. **Pintar.** `StackingContext` recorre los fragmentos de cada página en orden de pintado y llama a un `Canvas`:
   `PdfCanvas` escribe el PDF (con su etiquetado, marcadores y enlaces) y `SvgCanvas`, el SVG de depuración.
   `ContrastCheck` es un lienzo que no dibuja: `Converter.layOut` pinta con él cada página para comprobar el contraste del
   texto ([El PDF](comun/pdf.md#contraste)).

`Converter` envuelve los cuatro pasos para quien convierte ([Uso desde Java](implementacion/api.md)).

## Packages

Las dependencias van en un solo sentido y sin ciclos. `ArchitectureTest` tiene escrito qué puede usar cada package:

| Package | Puede usar | Qué hay |
|---|---|---|
| `model` | nada | Tipos puros y compartidos: los valores (`Value`, `ValueParser`), los enums de los valores (uno por fichero, como `TextAlign`, `Marker` o `Break`), la geometría (`Rect`, `Sides`, `Insets`), el texto con formato (`TextRun`, `TextFormat`, `FontFace`), las referencias (`Reference`), los enlaces (`Links`), la colocación de grid (`GridAreas`, `Area`, `Cell`), los colores y los fondos (`Colors`, `Paint`, `Radii`), el giro (`Rotation`), la tolerancia (`Tolerance`), los límites (`Limits`) y la raíz de los errores (`StxtPdfException`) |
| `resources` | `model` | La carpeta de recursos (`Resources`), las fuentes (`FontLibrary`, `FontFile`) y las imágenes (`ImageLibrary`, `ImageFile`, `SvgInspector`, `PdfInspector`) |
| `tree` | `model` | El árbol de cajas (`Box`, `Element`, `Style` y sus grupos) y su construcción (`BoxTreeBuilder` y sus piezas), los errores del modelo (`InvalidModelException`, `ErrorCode`) |
| `stxt` | `model`, `resources`, `tree` | `DocumentReader` |
| `fragment` | `model`, `tree` | El resultado del layout: `Fragment`, `Page`, `TextLine`, los tokens de corte (`BreakToken`, `BreakAppeal`, `Unit`) y las reglas de corte (`BreakRules`) |
| `layout` | `fragment`, `model`, `resources`, `tree` | El motor (`Engine`, `Paginator`, `Overflow`) y un subpackage por algoritmo: `block`, `inline`, `stack`, `grid`, `table`, `columns`, `position` e `image` |
| `output` | `fragment`, `model` | `StxtDump`, el volcado de los tests |
| `paint` | `fragment`, `layout`, `model`, `resources`, `tree` | El pintado (`StackingContext`, `Canvas`) y los dos lienzos: el PDF (`PdfDocumentWriter`, `PdfCanvas` y sus piezas) y el SVG (`SvgCanvas`) |
| `convert` | todos menos `tree` y `output` | `Converter`, la entrada del motor. Nada depende de él |

Un tipo que necesitan `tree` y `layout` va en `model`. Entre los algoritmos de `layout`, el bloque usa `inline` (los
marcadores de las listas son texto) y `position`, `stack` y `grid` usan `position`, y la tabla usa `grid`. Ningún algoritmo
nombra `Engine`: dependen de la interfaz `LayoutContext`.

## Las clases principales

### Entrada

- **`Converter`** (`convert`): la fachada. Inmutable y segura entre hilos. Reúne la carpeta de recursos, los estilos
  aparte, los límites y el modo de depuración (`Converter.builder()`), y da `layOut`, `toPdf` y `toSvg`. Cada conversión crea
  su propio `Engine`.
- **`DocumentReader`** (`stxt`): lee un documento o un documento de estilos y lo valida contra el schema.

### Árbol de cajas (`tree`)

- **`Box`**: una caja del árbol. Guarda su `Element`, su `Style`, sus hijos, su texto y sus tramos, su imagen, su marcador
  de lista, su número y su `Id`. No guarda geometría: la posición y el tamaño solo existen en los fragmentos. Se cierra al
  terminar la construcción.
- **`Element`**: el enum de los elementos (como `BLOCK`, `HEADING_2` o `CELL`), con su `Layout`, su `Position` y su papel
  (`Element.Role`), que es lo único que el layout y el pintado miran.
- **`Style`** y sus grupos: las propiedades de una caja, separadas como en el documento (`TypographyStyle`, el único que
  se hereda, `BoxStyle`, `BreakStyle`) y por tipo de elemento (`StackStyle`, `GridStyle`, `ItemStyle`, `ListStyle`,
  `ColumnsStyle`, `ImageStyle`, `TableOfContentsStyle`), con la línea del documento de cada propiedad.
- **`BoxTreeBuilder`**: el orquestador de la construcción ([El árbol de cajas](implementacion/arbol.md)). Reparte el
  trabajo entre sus piezas:
  - `ElementReader`, lo propio de cada familia de elementos;
  - `ValueReader`, los valores, y `PropertyReader`, las propiedades;
  - `StyleRules`, las reglas de `Styles`;
  - `TableBuilder`, `ListBuilder`, `MarkdownBuilder` y `TableOfContentsBuilder`, las tablas, las listas, el Markdown y
    los índices;
  - `ModelValidator`, las reglas que miran la caja ya leída.

### Layout (`layout`)

- **`Engine`**: despacha cada caja al algoritmo de su `Layout` y guarda la caché del layout. Implementa `LayoutContext`, que
  es lo que ven los algoritmos. No se comparte entre hilos.
- **`LayoutAlgorithm`**: la interfaz de los algoritmos (`BlockLayout`, `InlineLayout`, `StackLayout`, `GridLayout`,
  `TableLayout`, `ColumnsLayout`, `ImageLayout`): `layout(box, space, from)` y `contentIntrinsic(box)`.
- **`ConstraintSpace`** y **`LayoutResult`**: la entrada y la salida inmutables del layout de una caja. El resultado lleva
  el `Fragment`, el `BreakToken` para continuar en la página siguiente y la calidad del corte.
- **`Paginator`**: reparte cada sección en páginas, hace converger las referencias, numera las páginas y coloca los
  `Page overlay`.
- **`Overflow`**: busca los desbordamientos en el resultado ya paginado.
- Piezas puras de cada algoritmo: `LineBreaker` (reparto en líneas), `StackResolver`, `TrackSizing` y `Placement` (grid),
  `Columns` (cuántas columnas y de qué ancho), `OverlayEquation`, `UnitBreaker` (cortes de `Stack`, `Grid` y tablas), `Slice` (alto, borde y
  relleno de cada fragmento) ([El layout](implementacion/layout.md)).
- **`TextMeasurer`**: mide el texto. `FixedMetrics` (cada carácter mide medio cuerpo) es el de los tests de layout.
  `PdfMetrics` da las métricas reales de las fuentes y es el del PDF.

### Pintado (`paint`)

- **`StackingContext`**: ordena el pintado de una página en contextos de apilamiento y llama al lienzo. Calcula también las
  zonas de los enlaces.
- **`Canvas`**: la interfaz de los lienzos, que solo dibujan. Cada lienzo da su `Tagging`: el etiquetado del PDF, y en el
  SVG, nada.
- **`PdfDocumentWriter`**: escribe el documento PDF: páginas, etiquetado (`PdfStructure`), marcadores, destinos
  (`PdfDestinations`), enlaces (`PdfLinks`) y guardado. `PdfCanvas` dibuja una página, con `PdfContent`, `PdfFonts`,
  `PdfGradients` y `PdfImages`.
- **`SvgCanvas`**: el SVG de depuración de los tests ([El pintado](implementacion/pintado.md)).
- **`ContrastCheck`**: el contraste del texto con lo que tiene debajo ([El pintado](implementacion/pintado.md#el-contraste)).

## Reglas de la arquitectura

- **Dependencias en un solo sentido**, comprobadas por `ArchitectureTest`. Una dependencia nueva se decide y se apunta
  allí.
- **El árbol de cajas es de solo lectura fuera de `tree`**: `Box` y `Style` se escriben solo en `tree` y se cierran al
  terminar la construcción.
- **La geometría solo existe en los fragmentos**: una caja se maqueta muchas veces (medidas, sondas, cada página), así que no
  guarda «su» posición.
- **Toda llamada recursiva de layout pasa por `LayoutContext`** (`context.layout`, `context.intrinsic`), para usar la caché.
- **Fuera de `tree`, el layout y el pintado preguntan el papel de una caja, nunca su elemento** (salvo el despacho de la
  tabla en `Engine`), así que un elemento nuevo con un papel que ya existe no toca `paint`.
- **Las entradas y salidas del layout son `record` inmutables.**
