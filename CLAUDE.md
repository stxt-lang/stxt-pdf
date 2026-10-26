# CLAUDE.md

Guía de referencia para trabajar en este repositorio.

**Idioma:** la documentación (`README.md`, `docs/`, este fichero), los comentarios, el javadoc
y los mensajes de error están en **castellano**. Los **términos técnicos están en inglés**:
identificadores Java (packages, clases, métodos, campos, variables, tests) y nombres de nodos y
valores STXT. El contenido de `Text` en los documentos puede estar en cualquier idioma.

## Qué es

Motor de layout de cajas inspirado en CSS cuyos documentos se escriben en
[STXT](https://stxt.dev). Un mismo documento se maqueta en **modo continuo** (franja de altura
infinita, como un navegador) o **paginado** (páginas, como un PDF). Todas las medidas están en
**puntos tipográficos** (1 pt = 1/72 in; A4 = 595 × 842).

- **Especificación (fuente de verdad del comportamiento):** [`docs/ESPECIFICACION.md`](docs/ESPECIFICACION.md)
- **Estado e hoja de ruta:** [`README.md`](README.md)
- **Template STXT:** [`src/main/resources/stxt/dev.stxt.boxes.stxt`](src/main/resources/stxt/dev.stxt.boxes.stxt)
  (si difiere de la copia del §9 de la especificación, manda el fichero)
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

Es un **esqueleto**: la arquitectura, las interfaces y las piezas puras están hechas y probadas;
los algoritmos de layout completos están pendientes. La tabla de estado del README es la
referencia; al terminar un hito, actualízala.

| Hito | Trabajo | Dónde |
|---|---|---|
| 1 | ~~Conectar con `dev.stxt:stxt-core` y validar contra el template~~ (hecho) | `stxt/DocumentReader` |
| 2 | Flujo de bloque y texto; activar `GoldenTest` (caso 01) | `layout/block/BlockLayout`, `layout/inline/InlineLayout` |
| 3 | `relative`, `absolute`, `fixed` (esta en el `Paginator`) | `layout/position`, `fragment/Paginator` |
| 4 | `intrinsic()` en todos los algoritmos; flex (caso 02) | `layout/flex/FlexLayout` sobre `FlexResolver` |
| 5 | Grid | `layout/grid/GridLayout` sobre `Placement` y `TrackSizing` |
| 6 | `BreakToken` en los algoritmos, PDF con PDFBox, métricas reales | `PdfCanvas` (no existe aún), `TextMeasurer` |
| 7 | `{page}`/`{pages}`, `Decoration: clone`, corte en paralelo, Knuth-Plass | — |

Lo pendiente está marcado con `// TODO(hito N)` y lanza `UnsupportedOperationException` con el
hito en el mensaje. Busca `TODO(hito` para ver qué falta. El javadoc de cada clase pendiente
describe el algoritmo a implementar (por ejemplo, el pseudocódigo de `BlockLayout`).

## Arquitectura

Flujo de datos:

```
texto STXT ──DocumentReader──▶ InlineNode (stxt-core) ──BoxTreeBuilder──▶ Box (árbol) + Media
   ──Engine.layOutDocument──▶ Paginator ──▶ List<Page> (árbol de Fragment por página)
   ──▶ StxtDump (tests) · StackingContext + Canvas (SvgCanvas / PDF)
```

| Package | Contenido |
|---|---|
| `model` | Tipos puros: `Value` (valor *especificado*, sealed), `ValueParser`, `Sides`, `Insets`, `Rect` |
| `stxt` | `DocumentReader`: parsea con `stxt-core`, valida contra el template y exige una única raíz con el namespace `dev.stxt.boxes` |
| `tree` | `Box`, `Style` (propiedades y enums), `Media`, `BoxTreeBuilder`, `InvalidModelException` |
| `layout` | `Engine`, `LayoutAlgorithm`, `ConstraintSpace`, `LayoutResult`, `LayoutCache`, `IntrinsicSizes`, `MarginStrut`, `Sizing`, `PendingAbsolute` |
| `layout.block` / `inline` / `flex` / `grid` / `position` | Un algoritmo por `Layout` y sus piezas puras (`LineBreaker`, `FlexResolver`, `TrackSizing`, `Placement`, `AbsoluteEquation`…) |
| `fragment` | `Fragment`, `BreakToken`, `Paginator`, `Page`, `BreakRules`, `TextLine` |
| `paint` | `Canvas`, `StackingContext`, `SvgCanvas` |
| `output` | `StxtDump` (formato de los tests de referencia, §7) |

### Piezas clave

- **Nodos de `stxt-core`:** el motor trabaja directamente con `dev.stxt.Node`, `InlineNode`
  (valor inline e hijos) y `TextNode` (bloque `>>`); `getText()` da el contenido en los dos
  casos. Los nombres se comparan por `getCanonicalName()`: minúsculas y los espacios convertidos
  en guiones (`Box sizing` → `box-sizing`). Los errores de sintaxis y del template son
  `ParseException` de la librería, con código (`INVALID_VALUE`, `TOO_FEW_CHILDREN`…) y línea.
- **`BoxTreeBuilder`** interpreta valores, aplica la herencia, crea las cajas anónimas (§5.1) y
  comprueba las reglas del §6 que el template no puede expresar. Los errores son
  `InvalidModelException` con la **línea del documento**; los mensajes siguen la tabla del §6.
  La raíz que devuelve es una `Box` anónima que representa el área útil.
- **Enums de `Style`:** los valores STXT se convierten con `Enum.valueOf` sobre el texto en
  mayúsculas, así que **cada constante debe llamarse igual que su valor en el documento**
  (`Position.ABSOLUTE` ↔ `absolute`). Si cambias un valor, cambia la constante y el template.
- **`Engine`** despacha por `Style.layout` al algoritmo correspondiente. **Toda llamada
  recursiva de layout o de tamaños intrínsecos pasa por `engine.layout(...)` /
  `engine.intrinsic(...)`**, nunca directamente al algoritmo, para usar la `LayoutCache` (sin
  ella, flex y grid anidados crecen de forma exponencial).
- **`LayoutAlgorithm`**: `intrinsic(box)` (min/max-content de la caja de margen, independiente
  del espacio) y `layout(box, space, from)`.
- **`ConstraintSpace`** es la entrada inmutable del layout y parte de la clave de la caché:
  ancho disponible, alto (vacío = indefinido), tamaños impuestos por el padre, bases de
  porcentajes y espacio restante en la página (vacío = modo continuo).
- **`LayoutResult`**: el `Fragment`, el `BreakToken` (`null` = la caja ha terminado), la calidad
  del corte (`PERFECT_BREAK` … `FORCED`) y las `PendingAbsolute` que suben hasta su
  containing block.
- **`BreakToken`** (sealed, records con `equals`): `Block`, `Line`, `Container`. El
  `Paginator` detecta la falta de progreso comparando tokens; el mismo bucle sirve para los dos
  modos (en continuo da una sola vuelta).
- **`Fragment`**: posición relativa a la caja de **contenido** del padre; `StxtDump` la pasa
  a coordenadas absolutas de página.
- **`TextMeasurer`**: `FixedMetrics` (cada carácter = `0.5 × tamaño`) para tests deterministas;
  en el hito 6 se sustituye por métricas de PDFBox.

## Convenciones

- **Nombres STXT:** en inglés y, cuando existe, con el término de CSS (`Margin`, `Padding`,
  `Position: static`, `Break inside: avoid`, `Decoration: slice`). Las propiedades de varias
  palabras van separadas por espacio y con mayúscula solo la primera (`Font size`,
  `Min width`, `Border color`). En el código se comparan en forma canónica (minúsculas).
- **Términos del dominio en castellano en la documentación** ("área útil", "caja de borde",
  "viudas"); en el código, su equivalente inglés (`pageArea`, `borderBox`, `widows`).
- **Valores especificados frente a usados:** `Value` conserva lo que escribió el autor (flex y
  grid necesitan saber si era `auto`); se convierte a puntos con `resolve(...)` durante el
  layout.
- **Geometría:** posiciones y tamaños se refieren a la **caja de borde** salvo que se diga otra
  cosa; `Box sizing: border` es el valor por defecto. El ancho baja de padres a hijos y el alto
  sube de hijos a padres.
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
- Los documentos de test se leen con `DocumentReader`, que valida contra el template: un test
  de una regla del §6 comprueba así que el template no la detecta antes. Los volcados de §7
  (`expected.stxt`) no tienen namespace y se leen con `STXT.rawParser()`.
  `DocumentReaderTest` comprueba que el ejemplo y las entradas de los casos golden validan:
  añade ahí los documentos nuevos.
- **Tests de referencia** (`golden/GoldenTest`): cada carpeta de
  `src/test/resources/golden/NN-nombre/` tiene `input.stxt` y `expected.stxt` (volcado de
  §7); los `Rect` se comparan con tolerancia de 0.01 pt. Solo se vuelcan las cajas con `Id`.
  Está `@Disabled` hasta completar los hitos 2 y 4: al terminar el 2, actívalo (o filtra al
  caso 01).
- Para un caso nuevo, calcula el resultado esperado a mano siguiendo la especificación (como en
  el "Cómo se maqueta" del §10) y añade una carpeta numerada.

## Al cambiar el comportamiento

1. La especificación manda: si el código y `ESPECIFICACION.md` discrepan, aclara cuál es el
   correcto antes de cambiar ninguno de los dos.
2. Si añades o cambias una propiedad, actualiza a la vez: el template `.stxt`, la copia del §9,
   la tabla del §4, `Style` y `BoxTreeBuilder` (y la validación del §6 si aplica).
3. Al completar un hito, actualiza la tabla de estado y la hoja de ruta del README y elimina sus
   `TODO(hito N)`.
