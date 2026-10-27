# Hoja de ruta: calidad y arquitectura

Revisión del código desde el punto de vista de QA y de arquitectura (SOLID, DRY, KISS), con
una lista **priorizada** de lo que merece la pena arreglar. Cada tarea cabe en una sesión, no
cambia el comportamiento (salvo las marcadas como *bug latente*) y se verifica con `mvn test`.

Punto de partida (2026-10-27): hitos 1 a 14 terminados, 388 tests en verde (36 clases),
13 451 líneas de Java, 92 casos en el catálogo de errores y 11 casos golden.

## Cómo usar este documento

**Al empezar una sesión:** lee el [estado](#estado), elige la primera tarea `pendiente` cuyas
dependencias estén hechas y ejecuta `mvn test` para partir de verde.

**Al terminar:** marca las tareas (`[x]`), cambia el estado en la tabla y añade una entrada al
[registro](#registro-de-sesiones). Si una tarea cambia una decisión de diseño, apúntala en
[`DECISIONES.md`](DECISIONES.md).

**Regla de oro:** ninguna tarea de esta hoja cambia el resultado del layout. Si un PNG de
`golden/` aparece modificado en git o cambia un `expected.stxt`, para y revisa.

## Diagnóstico general

Lo que está **bien** y hay que conservar:

- Separación clara de fases: lectura → árbol de cajas → layout → páginas → pintado. Las
  entradas y salidas del layout son `record` inmutables (`ConstraintSpace`, `LayoutResult`,
  `Fragment`, `BreakToken` sellado).
- `Engine` como único despachador con caché, y piezas puras pequeñas y probadas
  (`FlexResolver`, `TrackSizing`, `LineBreaker`, `BreakRules`, `AbsoluteEquation`…).
- Estrategia de pruebas muy buena para un motor de layout: catálogo de errores con un caso por
  código (y un test que lo exige), casos golden calculados a mano y PNG versionados.
- Política de errores estricta y con la línea del culpable: hace los fallos reproducibles.

Lo que **más pesa** en contra, de mayor a menor impacto:

1. **Estado mutable donde no debería haberlo.** `Box` guarda la geometría "del último
   layout" (`x`, `y`, `width`, `height`, `margin`, `border`, `padding`), que con la caché y las
   pasadas de medida no tiene un significado único; casi nadie la lee, pero quien la lee puede
   recibir la de otra pasada.
2. **`BoxTreeBuilder` es una clase Dios** (1 400 líneas): lee reglas, traduce elementos,
   interpreta valores, construye tablas y listas y valida el §6. Cada hito nuevo la hace crecer.
3. **Tipos como texto.** Los elementos (`"table"`, `"list-item"`, `"heading-3"`), las claves
   de las líneas (`"item.grow"`, `"flex.gap"`) y la colocación de las celdas
   (`"3 1 1 2"`, que luego se vuelve a parsear) son cadenas repartidas por cuatro packages.
4. **Ramas `default` que asignan en silencio** en los `switch` de lectura de propiedades: si
   el schema gana una propiedad, se escribe en otra sin ningún error.
5. **Ciclos entre packages** (`tree ↔ layout`, `layout ↔ fragment`) y pequeñas duplicaciones
   (épsilon, `before()`, `sum`, alineación, `border()` de los canvas, formateo de mensajes).
6. **Huecos de test en piezas puras críticas para la fragmentación** (`Slice`, `BoxModel`,
   `UnitBreaker`, `LayoutCache`), justo lo que más va a tocar el hito 15 (columnas).

## Estado

Prioridad = valor ÷ coste. **S** ≤ media sesión, **M** una sesión, **L** dos sesiones.

| # | Tarea | Tipo | Coste | Depende de | Estado |
|---|---|---|---|---|---|
| 1 | [Ramas `default` silenciosas en la lectura de propiedades](#1-ramas-default-silenciosas) | QA | S | — | hecho |
| 2 | [`ConstraintSpace`: *withers* que pierden `contentHeight` y booleanos posicionales](#2-constraintspace-withers-y-booleanos) | QA · bug latente | S | — | hecho |
| 3 | [`layOutDocument` devuelve el documento entero; fin de la doble construcción](#3-resultado-del-documento-y-fachada) | ARQ · DRY | S | — | hecho |
| 4 | [Tests de las piezas puras sin test y cobertura con JaCoCo](#4-tests-de-piezas-puras-y-cobertura) | QA | M | — | hecho |
| 5 | [Quitar la geometría mutable de `Box`](#5-quitar-la-geometría-mutable-de-box) | ARQ · bug latente | M | 4 | hecho |
| 6 | [`enum Element` en lugar de nombres de elemento como texto](#6-enum-element) | ARQ · DRY | M | 1 | hecho |
| 7 | [Partir `BoxTreeBuilder`](#7-partir-boxtreebuilder) | ARQ · SRP | L | 1, 6 | hecho |
| 8 | [Romper los ciclos de packages y parsear la colocación de grid una vez](#8-ciclos-de-packages-y-colocación-de-grid) | ARQ | M | 7 | hecho |
| 9 | [Lote de duplicaciones pequeñas](#9-lote-de-duplicaciones) | DRY | S–M | — | hecho |
| 10 | [Unidades de `UnitBreaker` como objetos](#10-unidades-de-unitbreaker) | ARQ · KISS | M | 4 | hecho |
| 11 | [Rendimiento del texto largo (medir antes de tocar)](#11-rendimiento-del-texto-largo) | PERF | M | 4 | hecho |
| 12 | [PNG golden que cambian según la máquina](#12-png-golden-y-antialiasing) | QA | S | — | hecho |

**Orden recomendado:** 1 → 2 → 3 → 4 en una o dos sesiones (red de seguridad barata); después
5 (lo que más reduce riesgo antes del hito 15); 6 → 7 → 8 cuando toque añadir elementos nuevos;
9, 10 y 11 cuando haya hueco. Antes de empezar el **hito 15** conviene tener hechas al menos
1, 2, 4 y 5: las columnas son fragmentainers anidados y se apoyan en `Slice`, `UnitBreaker` y
`ConstraintSpace`.

---

## 1. Ramas `default` silenciosas

**Problema.** Los `switch` que leen propiedades asignan la última propiedad del grupo en la
rama `default`, confiando en que el schema solo deja pasar las demás:

- [`BoxTreeBuilder.java:775`](../src/main/java/dev/stxt/boxes/tree/BoxTreeBuilder.java) `box`:
  cualquier nombre desconocido es `Background`.
- `:798` `typography` → `Orphans`; `:812` `fragmentation` → `Decoration`; `:826` `flex` →
  `Lines`; `:855` `grid` → `Vertical`; `:886` `item` → `Vertical`; `:435` elemento →
  `Isolate`; `:313` `apply` → cualquier grupo es `Break`; `:213` `styles` → `For id`.

Si se añade una propiedad al schema y se olvida el `case` (paso 2 de "Al cambiar el
comportamiento" de `CLAUDE.md`), el valor acaba en otra propiedad o falla con un mensaje
engañoso ("Color no válido" para algo que no es un color). Es el tipo de error que el
catálogo no detecta, porque no hay caso para lo que nadie sabe que falta.

**Cómo.**

- [x] Cada propiedad con su `case` explícito y `default -> throw unread(p)`
      (`IllegalStateException`: es un error del motor, no del documento).
- [x] Test `SchemaCoverageTest`: recorre el schema y, para cada par (nodo, propiedad o grupo),
      construye el árbol de un documento mínimo que la declara con un valor válido (161 pares).
      Más estricto que lo previsto: el documento tiene que construirse sin ningún error, no solo
      sin `IllegalStateException`. Un par nuevo del schema falla hasta que se añade su ejemplo
      (`SAMPLES`) y, si el nodo es nuevo, su plantilla (`TEMPLATES`).

**Verificación.** `mvn test` (549 tests); quitando el `case "orphans"`, el test falla en
`Typography > Orphans`.

**Límite conocido.** Lo que el builder lee con `child(...)` en lugar de un `switch` (los hijos
de `Document` y de `Media`) no lanza nada si falta: ahí el test solo obliga a añadir el ejemplo.
Los `switch` de `layout` y `position` por elemento, que seguían con `default`, desaparecen con
la [tarea 6](#6-enum-element).

## 2. `ConstraintSpace`: *withers* y booleanos

**Problema (bug latente).**
[`ConstraintSpace.withRemaining`](../src/main/java/dev/stxt/boxes/layout/ConstraintSpace.java)
y `fragmented` usan el constructor de 8 argumentos y **ponen `contentHeight` a `false`** sin
decirlo. `Engine.layout` repite el layout con `space.withRemaining(...)`
([`Engine.java:117`](../src/main/java/dev/stxt/boxes/layout/Engine.java)) y `UnitBreaker`
parte en paralelo con `p.space().fragmented(...)`: si algún día llega ahí un espacio de medida
(`measuringContentHeight()`), la segunda pasada maqueta otra cosa. Hoy no se da (los espacios
de medida de flex no fragmentan), pero nada lo impide. Además `withRemaining` ignora en silencio
`remaining` si el espacio no fragmenta.

Las 11 construcciones con `new ConstraintSpace(w, h, false, false, w, h, empty, false)` son una
*boolean trap*: hay que contar posiciones para saber qué significa cada `false`.

**Cómo.**

- [x] `withRemaining` y `fragmented` conservan `contentHeight` (constructor canónico).
      `withRemaining` sobre un espacio sin fragmentación lanza `IllegalStateException` (error
      del motor; sus dos llamadas, en `Engine` y `BlockLayout`, ya lo comprobaban) en lugar de
      ignorar `remaining`; para añadir la fragmentación está `fragmented`.
- [x] Fábricas con nombre: `free(width, baseW, baseH, atStart)` (sondas de `BoxModel` en
      flex, grid y `AbsoluteLayout`), `fixed(width, height, baseW, baseH, atStart)` (ítems de
      flex y grid, `AbsoluteLayout`; el alto se impone si está presente) y
      `forChildren(width, height, remaining, atStart)` (`BoxModel.childSpace`), además de
      `forPage`. Llevan `atStart` porque forma parte de la clave de la caché y quitarlo
      podría cambiar resultados. Se elimina el constructor de 8 argumentos: fuera de
      `ConstraintSpace` nadie construye el `record` directamente, y `FlexLayout.itemSpace` y
      `GridLayout.itemSpace` dejan de tener el booleano `fixedWidth` (el de grid desaparece).
- [x] `ConstraintSpaceTest`: los *withers* conservan todo lo que no cambian, las fábricas y
      `withRemaining` sin fragmentación.

**Verificación.** `mvn test` (555 tests), sin cambios en los PNG ni en los `expected.stxt`;
volviendo a poner `false` en `fragmented`, fallan dos tests de `ConstraintSpaceTest`.

## 3. Resultado del documento y fachada

**Problema.** `Engine.layOutDocument` construye el árbol y devuelve solo las páginas, así que
quien necesita el medio o los metadatos **vuelve a construir el árbol**:
[`PdfCanvas.java:89-90`](../src/main/java/dev/stxt/boxes/paint/PdfCanvas.java),
`GoldenTest:74-75` y otros 11 sitios de los tests. Es trabajo doble y, peor, el segundo árbol
tiene otras `Box` que las de los fragmentos. `PdfCanvas` mezcla dos responsabilidades: es un
`Canvas` y es la fachada "documento → PDF" que maqueta (SRP). Encaja con la idea del
[`TODO.md`](../TODO.md) de una API para integrar el motor con PDFBox.

**Cómo.**

- [x] `record LaidOutDocument(Media media, Metadata metadata, List<Page> pages)` (en
      `layout`, junto a `Engine`) como resultado de `layOutDocument`: se cambia la firma en
      lugar de añadir un método, para que solo haya una forma de maquetar un documento.
- [x] `paint.PdfWriter.write(InlineNode, …)` es la fachada que maqueta con `PdfMetrics` y
      pinta; en `PdfCanvas` queda el pintado y `write(LaidOutDocument, resources, target,
      debug)`, que sustituye al de páginas, medio y metadatos sueltos.
- [x] `GoldenTest` y `PdfCanvasTest` usan el resultado o la fachada, y `LayoutFixture` y
      `StackingContextTest`, `pages()`. Los "otros 11 sitios" eran sobre todo tests de `tree` que
      construyen el árbol sin maquetar (`StylesTest`, `ListsTest`, `PdfMetricsTest`…): no hay
      doble construcción y se quedan como están.
- [x] Quitar `Engine.cache()` (público y sin uso).
- [x] README y `CLAUDE.md` usan `PdfWriter` en los ejemplos y el flujo de datos.

**Verificación.** `mvn test` (555 tests), sin cambios en los PNG ni en los `expected.stxt`.

**Pendiente fuera de esta tarea.** `PdfCanvas.root(pages)` sigue buscando la raíz subiendo por
`parent` desde la primera página; con la [tarea 5](#5-quitar-la-geometría-mutable-de-box) o la
[7](#7-partir-boxtreebuilder) puede ir en `LaidOutDocument` si hace falta.

## 4. Tests de piezas puras y cobertura

**Problema.** `CLAUDE.md` pide un test por pieza pura, pero no tienen ninguno: `Slice`,
`BoxModel`, `ConstraintSpace`, `LayoutCache`, `RelativeOffset` y `MarkerLayout`; `UnitBreaker`
solo se prueba a través de los `*FragmentationTest`. `Slice` concentra las reglas más sutiles
del §5.10 (`slice`/`clone`, `Height` repartido entre páginas, mínimo y máximo sobre el total) y
es lo que más va a usar el hito 15. El javadoc de `LayoutCache.stats()` habla de "los tests de
anidamiento profundo", que no existen: nada protege la regla de que toda recursión pase por
`engine.layout`/`engine.intrinsic` (sin ella el coste es exponencial).

No hay medida de cobertura ni análisis estático en el `pom.xml`.

**Cómo.**

- [x] `SliceTest` (11 casos): sin fragmentación; `slice` con alto `auto` en primer, intermedio
      y último fragmento; `Height` definido repartido (el contenido termina y el alto no cabe, y
      cortado dentro del contenido sin pasar del alto que queda); `Min height` y `Max height`
      sobre el total; `clone` con alto `auto` y con `Height` (240 de caja de contenido en
      90 + 90 + 60); `top`, `contentRemaining` y `contentHeight`.
- [x] `BoxModelTest` (13 casos): porcentajes de margen, borde y relleno; `Width` en
      porcentaje con mínimo y máximo (gana el mínimo); `fixedWidth`/`fixedHeight`; palabras
      clave intrínsecas; `Height` en porcentaje con y sin base; `Aspect ratio` con máximo;
      `measuringContentHeight`; `usedHeight`; `intrinsic` con `Width` fijo, porcentaje,
      mínimo y máximo; `childSpace`; los intrínsecos solo se piden si hacen falta.
- [x] `LayoutCacheTest`: `Flex` y `Grid` alternos de 4, 8 y 12 niveles. Como
      `Engine.cache()` ya no existe ([tarea 3](#3-resultado-del-documento-y-fachada)), el trabajo
      se mide con un `TextMeasurer` que cuenta las mediciones: 38, 66 y 94 (lineal, 7 por
      nivel). Sin caché salen 180, 1 068 y 4 800, y el test falla. `LayoutCache.stats()` se
      queda: lo usa `Engine.toString`; su javadoc ya no cita tests que no existían.
- [x] `UnitBreakerTest` (7 casos) con `Units` y `Placed` sintéticos: sin fragmentación;
      corte entre unidades y continuación; grupo que no se puede separar (`BEFORE` fuera del
      principio de página, `FORCED` al principio); corte forzado; `Repeat` con cabecera y pie
      en los dos fragmentos; partición en paralelo de una unidad más alta que la página (solo
      continúa el ítem que no ha terminado); unidad no partible colocada entera.
- [x] JaCoCo 0.8.13 en el `pom.xml`, solo el informe: `mvn test` lo deja en
      `target/site/jacoco/index.html`.

**Verificación.** `mvn test` (587 tests), sin cambios en los PNG ni en los `expected.stxt`.

**Lo que dice el primer informe** (2026-10-27): 95,1 % de líneas y 86,1 % de ramas. Las piezas
de esta tarea quedan en 92–100 % de ramas. Lo que más ramas deja sin ejecutar:

| Clase | Ramas | Ramas sin cubrir |
|---|---|---|
| `tree.BoxTreeBuilder` | 84,9 % | 101 |
| `layout.Overflow` | 62,7 % | 38 |
| `model.ValueParser` | 75,0 % | 18 |
| `layout.grid.TrackSizing` | 91,0 % | 15 |
| `layout.inline.InlineLayout` | 74,1 % | 15 |
| `layout.grid.Placement` | 71,4 % (76,4 % de líneas) | 8 |

`RelativeOffset` y `MarkerLayout`, que el problema citaba, siguen sin test propio: los tests de
`position` y de listas ejecutan el 100 % y el 67 % de sus ramas, y no se les ha añadido uno
por ahora.

`Overflow` es el hueco más claro: es la última red contra los desbordamientos (§5.12) y el
catálogo de errores solo ejecuta dos tercios de sus ramas. Antes de poner un umbral, conviene
mirar en el informe qué ramas de `Overflow` y de `Placement` son casos reales sin probar y
cuáles son código muerto.

**Revisión de las ramas de `Overflow`** (2026-10-27). Casi todas eran casos reales sin probar;
dos escondían errores:

- [x] **Bug:** un `Relative` con `Offset` vertical en porcentaje dentro de un padre que se
      parte entre páginas daba un `BOX_OVERFLOW` falso: `Overflow` recalculaba el
      desplazamiento con el alto del fragmento, no con el `Height` entero. Ahora el desplazamiento
      usado va en `Fragment.offsetX`/`offsetY` (y en `UnitBreaker.Placed.relative`), como el
      margen en la [tarea 5](#5-quitar-la-geometría-mutable-de-box). Tests en
      `BlockFragmentationTest` y `GridFragmentationTest` (fallan con el `Overflow` anterior).
      `DECISIONES.md` 105.
- [x] **Bug:** el culpable de un desbordamiento horizontal bajaba a los hijos `auto` que solo
      llenan una caja con `Width` (`Hija con Width que se sale y nietas que la llenan`, `Ítem
      de lista más ancho que la lista`). `DECISIONES.md` 106.
- [x] **Código muerto:** `box.text == null` en `textOverflow` (solo las cajas de texto y los
      marcadores tienen líneas) y el alto infinito en la base del `Relative` (desaparece con
      el primer punto).
- [x] 16 casos nuevos en el catálogo: `Absolute` por la izquierda y sin `Offset`, `Fixed` por
      abajo; palabra en texto centrado, dentro de una caja que se sale y número de página;
      ítem flex centrado, nieta, `Min width`, `Max height`, fila de grid; línea dentro de una
      lista, `Size` sin `Line height` y relleno más alto que la página.

Resultado: `Overflow` pasa del 62,7 % al 93,8 % de ramas (100 % de líneas); el total, a
95,6 % de líneas y 87,5 % de ramas. Las 7 ramas que quedan son combinaciones de condiciones que
ya se prueban por otro lado (lado izquierdo y derecho de la misma comparación, `Absolute` o
marcador que se salta al bajar). `Placement`, más abajo.

**Revisión de las ramas de `Placement`** (2026-10-27). Lo que no se ejecutaba eran los ítems con
solo la fila o solo la columna fija (`Area: 2 auto`, `Area: auto 2`), que no tenía ningún test
ni ningún caso golden; y dos comprobaciones de límite de `Occupancy`, código muerto:

- [x] **Bug:** un ítem con la fila fija en una fila sin hueco (`Area: 1 auto` con las dos
      columnas de la fila 1 ocupadas) buscaba columna sin límite: tras dos mil millones de
      vueltas, `ArrayIndexOutOfBoundsException`. Ahora es `GRID_PLACEMENT` con la línea del
      `Area` (decidido con el usuario; CSS crearía columnas implícitas). Con `Flow: column`, lo
      mismo con la columna fija. `Placement.NoRoom` y `GridLayout.noRoom`; dos casos en el
      catálogo; §5.8, §6 y `DECISIONES.md` 109, que apunta también la diferencia con CSS en el
      orden de estos ítems.
- [x] Tests en `PlacementTest`: fila fija (primera columna libre, sin mover el cursor),
      columna fija (detrás del cursor pasa a la fila siguiente; ocupada, baja) y los dos errores.
- [x] **Código muerto:** el límite de columnas de `Occupancy.isFree` y de `mark`: `place`
      cuenta todas las columnas antes de empezar y ninguna área pasa de ellas.

Resultado: `Placement` pasa del 71,4 % al 100 % de ramas y de líneas; 631 tests en verde.

- [x] **Culpable de los desbordamientos verticales** (decidido con el usuario): `BOX_OVERFLOW`
      señala también `Aspect ratio` y `Rows`, y `PAGE_OVERFLOW`, el `Padding` o el `Border` que no
      caben en la página y el `Size` de una línea sin `Line height`; `Line height` se hereda con
      su línea. Cambian el §5.12 y tres casos; cuatro casos nuevos (`Aspect ratio`, borde,
      relleno con `Height`, `Line height` heredado). `DECISIONES.md` 107. Queda fuera la caja
      cuyo alto impone el padre (ítem estirado a una fila fija), que se señala a sí misma.

## 5. Quitar la geometría mutable de `Box`

**Problema (bug latente, el de más peso).** [`Box`](../src/main/java/dev/stxt/boxes/tree/Box.java)
tiene `x`, `y`, `width`, `height`, `margin`, `border` y `padding` "del último layout"; los
escriben `BoxModel.applyTo`, `BlockLayout`, `UnitBreaker.piece`, `AbsoluteLayout`,
`MarkerLayout` e `InlineLayout`. Pero una caja se maqueta muchas veces (sondas, medidas,
estirado, cada página) y la caché devuelve resultados sin volver a escribirla, así que **ese
estado no corresponde a ningún fragmento concreto**. Lo leen dos sitios:

- [`Overflow.java:98`](../src/main/java/dev/stxt/boxes/layout/Overflow.java) usa
  `c.box().margin` para comprobar el desbordamiento horizontal del fragmento `c`.
- `Fragment.complete` copia `box.border` y `box.padding` (hoy solo para el marcador, que
  no tiene).

Con márgenes en porcentaje y una caja maqueta con bases distintas, `Overflow` puede usar el
margen de otra pasada. No he encontrado un documento que falle hoy, pero es una trampa para
el hito 15, en el que la misma caja se maqueta en columnas de anchos distintos. `borderBox()`,
`marginBox()`, `paddingBox()` y `contentBox()` de `Box` no los llama nadie.

**Cómo.**

- [x] El margen usado viaja con el resultado: campo `margin` en `Fragment`, que cada algoritmo
      llena con el de su `BoxModel`. No vale resolverlo en `Overflow` con el ancho de contenido
      del padre: los márgenes de los ítems de grid son del área (test
      `GridLayoutTest.itemMarginsUseTheAreaWidth`).
- [x] `BlockLayout`, `UnitBreaker` y `AbsoluteLayout` usan variables locales en lugar de
      `child.x`/`p.box().x`/`box.x`; nadie escribe `box.height`.
- [x] `MarkerLayout` construye el `Fragment` sin pasar por la caja (sin margen, borde ni
      relleno, como antes); `Fragment.complete` desaparece.
- [x] Borrados los siete campos y los cuatro métodos de geometría de `Box`, y
      `BoxModel.applyTo`. `Box` solo cambia mientras `BoxTreeBuilder` construye el árbol
      (`parent`, `children`, `text`, `marker`, `headRows`…), que es lo que queda para la
      [tarea 7](#7-partir-boxtreebuilder).
- [x] Apuntado en `DECISIONES.md` (104) y en `CLAUDE.md`.

**Verificación.** `mvn test` (588 tests), sin cambios en los PNG ni en los `expected.stxt`.

**Ganancia extra.** Sin estado en `Box`, el mismo árbol se puede maquetar dos veces (SVG y PDF,
o con métricas distintas) sin interferencias, y la caché queda correcta por construcción.

## 6. `enum Element`

**Problema.** Los elementos son cadenas en varias listas paralelas que hay que mantener a mano:
`ELEMENTS`, `ELEMENT_NAMES`, `IN_FLOW`, `TEXTS` y `TABLE_GROUPS` en `BoxTreeBuilder:69-82`, los
`switch` de `layout` y `position` (`:380-394`), `Box.headingLevel` (que lee `element.charAt(8)`),
`"table".equals(...)` en `Engine:150`, `"list".equals(...)` en `Overflow:89` y tres más en
`PdfStructure:100-110`, y `Box.isMarker()` con `"marker"`. Añadir un elemento (hito 15) obliga a
tocar todas.

**Cómo.**

- [x] `tree.Element` con el nombre que se escribe (`label()`, `List item`) y el canónico
      (`canonicalName()`, `list-item`), el `Layout` y la `Position`, si puede ser el elemento de
      un `Item` o un `Area` (`canBeItem()`, lo que era `IN_FLOW`), si lleva el texto en el valor
      o en `Content` (`hasText()`) y el nivel de encabezado. `Element.of(nombre canónico)` solo
      encuentra los que se escriben: `MARKER` es generado, y `marker` es también la propiedad
      `Marker` de `List`. La `Cell` con valor cambia su `Layout` a `INLINE` en el builder, como
      antes.
- [x] `Box.element` pasa a ser `Element`; la raíz sigue con `null` (`PdfStructure` la etiqueta
      como `Document`). `Box.headingLevel(String)` y la lectura de `element.charAt(8)`
      desaparecen.
- [x] Desaparecen `ELEMENTS`, `ELEMENT_NAMES`, `IN_FLOW` y `TEXTS`, y los dos `switch` de
      `element()` con su `default`. Las reglas `Element` van en un `EnumMap`. `Engine`,
      `Overflow` y `PdfStructure` comparan con `Element.TABLE`, `LIST`…; el `switch` del tipo de
      estructura del PDF es sobre el enum. `TABLE_GROUPS` se queda: `Head`, `Body` y `Foot` no
      son elementos de caja.
- [x] `ElementTest`: los elementos que se escriben son los nodos del schema que admiten `Class`, y
      los que pueden ser ítem, los elementos que admiten `Item` y `Area`. Un elemento nuevo en el
      schema falla aquí hasta que se añade al enum.

**Verificación.** `mvn test` (613 tests), sin cambios en los PNG ni en los `expected.stxt`;
marcando `LIST` como no apto para ítem, falla `ElementTest`.

**Para el hito 15.** Añadir un elemento es una constante del enum, el schema y lo propio del
elemento en el builder; lo demás (reglas `Element`, envoltorios, mensajes) sale del enum.

## 7. Partir `BoxTreeBuilder`

**Problema.** [`BoxTreeBuilder`](../src/main/java/dev/stxt/boxes/tree/BoxTreeBuilder.java) tiene
siete responsabilidades y estado mutable de instancia que se reinicia en `build` (no es
reentrante). Es la clase que más cambia en cada hito y la más difícil de revisar.

**Cómo** (solo mover código; el catálogo de errores y los tests de `tree` son la red):

- [x] `ValueReader` (estático, 265 líneas): `value`, `sides`, `tracks`, `enumValue`, `color`,
      `integer`, `aspectRatio`… y `Kind`. `gap` escribe en el `Style` y va en `PropertyReader`.
- [x] `PropertyReader`: los grupos `Box`, `Typography`, `Break` y las propiedades de `Flex`,
      `Grid` e `Item`, compartidos por elementos y reglas. Las propiedades propias de un
      elemento (`Id`, `Offset`, `Layer`, `Marker`, `Start`, `Number`, `Isolate`) se quedan en
      `BoxTreeBuilder.element`, que es quien sabe de qué elemento son.
- [x] `StyleRules`: lee `Styles` en su constructor, `apply` aplica las reglas a un elemento y
      `checkIds` comprueba al final que cada `For id` tiene su elemento.
- [x] `TableBuilder` (tabla → celdas colocadas; la caja de cada celda la construye el builder
      con un *callback*, `TableBuilder.Cells`) y `ListBuilder` (numeración, marcador y las
      reglas de `Start` y `Number`).
- [x] `ModelValidator`: reglas del §6 que miran la caja ya leída (`validateBox`, `Offset`,
      márgenes, mínimo y máximo, porcentajes, `definiteHeight`), `validateFlex` y la fuente de
      los textos.
- [x] Un `BuildContext` por llamada con las reglas, `ids`, `definiteHeight`, `headingLevel`,
      `insideFixed` e `itemNumbers`: `BoxTreeBuilder` ya no tiene campos y es reentrante.
- [x] `Nodes` (no estaba previsto): acceso a los nodos (`child`, `inline`, `isElement`…) y
      errores (`error`, `unread`, `parse`, `placement`), que comparten todas las piezas.
- [x] Las claves de `Style.line(...)` como constantes en `PropertyKey` (`ITEM_GROW`,
      `FLEX_GAP`, `FONT_SIZE`…), con `internal(...)` (el antiguo `INTERNAL`) y los prefijos
      `flex(...)`, `grid(...)` e `item(...)`. Las usan también `Overflow` y `PdfMetrics`.
      `PropertyKeyTest` comprueba que cada constante es una propiedad del schema en su sitio:
      una errata ya no compila o falla en el test, en lugar de saltar solo con el error del
      documento que la usa. Los tests que leen el schema (`SchemaCoverageTest`, `ElementTest`,
      `PropertyKeyTest`) comparten `SchemaNodes`.

`BoxTreeBuilder` queda como orquestador de 346 líneas (antes 1 393): `build`, `Media` y
metadatos, elementos, encabezados y texto.

**Verificación.** `mvn test` (615 tests), sin cambios en los PNG ni en los `expected.stxt`;
con una errata en `PropertyKey.ITEM_BASIS`, falla `PropertyKeyTest`.

## 8. Ciclos de packages y colocación de grid

**Problema.** Dependencias entre packages (de `import`):

```
tree     -> layout          (BoxTreeBuilder usa layout.grid.Area, Cell y GridAreas)
layout   -> fragment -> layout   (BreakRules usa LayoutResult; Paginator usa Engine)
resources -> tree           (FontLibrary usa tree.FontFace)
```

El árbol de cajas depende del layout para validar, y `fragment` y `layout` son un solo
módulo partido en dos. Además la colocación se parsea muchas veces: `GridAreas.parse` corre en
`PropertyReader.grid`, otra vez **por cada ítem** en `ModelValidator.validateBox` y en **cada** layout e intrínseco del
grid (`GridLayout:60`); la tabla escribe la colocación de cada celda como texto
(`"3 1 1 2"`, `TableBuilder.table`) para que `Cell.parse` la vuelva a leer.

**Cómo.**

- [x] `Area`, `Cell` y `GridAreas` (parseo puro) pasan a `model`. `GridAreas` es un `record`
      con las áreas con nombre y las filas y columnas que declara el bloque (`GridAreas.NONE`
      sin `Areas`), y `Cell.parse` busca los nombres en él. Los tests de la sintaxis pasan a
      `model/GridAreasTest`; `AreasAndPlacementTest` se queda con la colocación y se llama
      `PlacementTest`.
- [x] `Style.gridAreas` es un `GridAreas` y `Style.itemArea` un `Cell` (`Cell.AUTO` = colocación
      automática), interpretados una sola vez al leer: `Areas` en `PropertyReader.grid` y el
      valor de `Area` en `PropertyReader.item`, con las `Areas` del grid padre (antes se
      interpretaba otra vez en `ModelValidator.validateBox`). `GridLayout` ya no parsea nada y
      `TableBuilder` crea el `Cell` de cada celda. `Nodes.placement` devuelve el valor
      interpretado. `DECISIONES.md` 108.
- [x] `PdfStructure.placement`, que volvía a partir el texto de la colocación de cada celda de
      tabla (no estaba en la lista), desaparece: lee el `Cell`.
- [x] Las calidades de corte (`PERFECT_BREAK`…`FORCED`) salen de `LayoutResult` a
      `fragment.BreakAppeal`; `Paginator` pasa a `layout`, junto a `Engine`, que es quien lo usa.
- [x] `FontFace` pasa a `model`, y con ella `FontWeight` y `FontStyle`, que eran enums de
      `Style`: sin ellos `resources` seguía dependiendo de `tree` (`FontLibrary` usaba
      `Style.FontWeight`, cosa que el diagnóstico no recogía).
- [x] `ArchitectureTest`, a mano (sin dependencias nuevas): lee los `import` y los nombres
      cualificados fuera de los comentarios de `src/main/java`, cuenta cada package con sus
      subpackages y exige que no haya ciclos, que `model` no dependa de nada y que `tree` solo
      dependa de `model`.

Dependencias que quedan (todas en un sentido):

```
stxt -> tree -> model          resources -> model
layout -> fragment, model, resources, tree      fragment -> model, tree
paint -> layout, fragment, model, resources, tree      output -> fragment, model
```

**Verificación.** `mvn test` (619 tests), sin cambios en los PNG ni en los `expected.stxt`; con
un `import dev.stxt.boxes.layout.Engine` en `tree/Box`, `ArchitectureTest` falla en
`noCyclesBetweenPackages` (`fragment -> tree -> layout -> fragment`) y en
`treeDependsOnlyOnModel`.

## 9. Lote de duplicaciones

Pequeñas, independientes y de bajo riesgo; se han hecho de una en una, con un commit cada una:

- [x] **Épsilon:** siete constantes `EPSILON = 1e-9` (`BlockLayout`, `Engine`, `Slice`,
      `UnitBreaker`, `TableLayout`, `BreakRules`, `TrackSizing` como `EPS`) y seis literales
      `1e-9` (`FlexLayout`, `FlexResolver`, `LineBreaker`). Queda una sola en
      `model.Tolerance`, con `fits(size, space)` y `same(a, b)`; va en `model` y no en `Sizing`
      porque `BreakRules` (`fragment`) también la usa y `fragment` no puede depender de
      `layout` ([tarea 8](#8-ciclos-de-packages-y-colocación-de-grid)). Las comparaciones
      `a > b + EPSILON` pasan a `!fits(a, b)` y `|a - b| > EPSILON` a `!same(a, b)`, que son
      equivalentes; las demás usan la constante. El 0.01 de `Overflow` se llama `TOLERANCE` y
      su javadoc dice que es una tolerancia del modelo, no de redondeo. Con 1e-9 la suite
      también pasa (ningún test depende de ese 0.01), pero se queda: decidido con el usuario.
- [x] **"No cabe nada":** el mismo `LayoutResult` con `BreakToken.BEFORE` en `BlockLayout`,
      `UnitBreaker` e `InlineLayout` → `LayoutResult.before(box)`.
- [x] **Alineación en un eje:** el `switch START/STRETCH/END/CENTER` de `FlexLayout` y
      `GridLayout` → `Sizing.align(alignment, space, size)`. `sum(double[])` estaba en las dos y
      en `TrackSizing` → `Sizing.sum`, con el mismo bucle (`DoubleStream.sum` compensa el
      redondeo y podría cambiar el último decimal). Tests en `SizingTest`.
- [x] **Bordes del canvas:** `border()` era idéntico en `SvgCanvas` y `PdfCanvas` → método por
      defecto en `Canvas` que pinta cuatro `background` y llama a `outline(Rect)`, el contorno de
      depuración, que es lo único que implementan los dos.
- [x] **Mensajes:** `name(Box)` y `pt(double)` de `Overflow` y `TableLayout` → `layout.Messages`.
      De las dos `pt` queda la de `Overflow` (`%.2f` sin ceros finales); la de `TableLayout`
      pasaba a notación científica desde 10⁷. `MessagesTest`.
- [x] **Caja de contenido de un fragmento:** `Fragment.contentWidth()` y `contentHeight()`, en
      `Overflow` (sustituyen a dos métodos privados) e `InlineLayout.number`. El desplazamiento a
      la caja de contenido (`x + f.border().left() + f.padding().left()`, seis veces) no se ha
      tocado: sacarlo cambia el orden de las sumas.
- [x] **Colocar una línea:** medir, alinear, `wordSpacing` y ancho final, dos veces en
      `InlineLayout` (`layout` y `number`) → `LineBreaker.place(...)`, que devuelve la
      `TextLine`. Test en `LineBreakerTest`.

**Verificación.** `mvn test` (623 tests) después de cada punto, sin cambios en los PNG ni en los
`expected.stxt`.

## 10. Unidades de `UnitBreaker`

**Problema.** `UnitBreaker.Units` son cuatro arrays paralelos (`starts`, `ends`, `appeal`,
`forced`) dentro de un `record`, cuyo `equals` compara los arrays por identidad. `FlexLayout`
los monta con tres listas paralelas (`starts`, `ends`, `groups`, líneas 209-266) y
`TableLayout` los recorta con cuatro `Arrays.copyOfRange`. `UnitBreaker.fragment` recibe nueve
parámetros. Es el código más difícil de seguir del motor y el hito 15 lo va a reutilizar.

**Cómo.**

- [x] `record Unit(double start, double end, int appealBefore, boolean forcedBefore)`, con
      `breakableBefore()` y `shifted(origin)`, y `UnitBreaker.Units(List<Unit>, extent,
      splittable)` con `slice(from, to, origin)` para la tabla, que ya no copia cuatro arrays.
      `Unit` va en `fragment` y no en `layout` porque `BreakRules.cutUnits` también la recibe:
      pasa de siete parámetros (cuatro arrays) a cuatro (`units, first, space, mustPlace`). Al
      ser `record` sin arrays, `Units` tiene un `equals` de verdad.
- [x] `GridLayout.Arranged` pasa a `UnitBreaker.Arranged` (modelo, ítems colocados, cajas fuera
      de flujo y unidades), el resultado común de flex y grid, y `fragment` recibe `(engine, box,
      space, from, arranged[, repeat])`: de nueve parámetros a seis.
- [x] `FlexLayout.Run.layOut` (de 106 líneas a 22) separa el paso 6 (`crossSizes`), el 7
      (`stretchItems`) y el 8 (`arrange`), y la construcción de las unidades (`units`, a partir
      de un `Span` por unidad en lugar de tres listas paralelas). `GridLayout.rowUnits` saca a
      `rowUnit` el corte delante de cada fila.
- [x] Tests: `UnitBreakerTest` con `Unit` (y un caso de `slice`), y los de `cutUnits` en
      `BreakRulesTest`.

**Verificación.** `mvn test` (624 tests), sin cambios en los PNG ni en los `expected.stxt`.

## 11. Rendimiento del texto largo

**Medir antes de tocar.** No hay ningún test de rendimiento; esto solo merece la pena si un
documento real lo nota.

- `InlineLayout.layout` vuelve a partir **todo** el texto en cada fragmento (cada página tiene
  otro `from`, así que la caché no ayuda): un texto de N páginas se parte N veces, y con
  `Text wrap: pretty` (Knuth-Plass) es lo más caro del motor.
- `InlineLayout:102` calcula los intrínsecos con `BoxModel.intrinsic(box, contentIntrinsic(box))`
  sin pasar por `engine.intrinsic`, contra la regla de `CLAUDE.md` (no tiene `Engine`).
- `Paginator` vuelve a paginar todo si `{pages}` no cabe en las cifras reservadas (aceptable).

**Cómo.**

- [x] `LongTextTest.measureLongText` (`@Tag("perf")`, fuera de la suite normal: el `pom.xml`
      excluye el grupo `perf`): un solo `Text` de 25, 50 y 100 páginas, con `FixedMetrics` y
      `PdfMetrics` y con `Text wrap` `auto` y `pretty`. Se ejecuta con
      `mvn test -Dgroups=perf -DexcludedGroups=none -Dtest=LongTextTest`.
- [x] Se nota: el coste era **cuadrático**. Con el doble de páginas, el cuádruple de mediciones
      (sin JaCoCo, `-Djacoco.skip=true`):

      | Páginas | Mediciones antes | `PdfMetrics` antes | Mediciones después | `PdfMetrics` después |
      |---|---|---|---|---|
      | 25 | 703 560 | 0,8 s | 29 640 | 0,07 s |
      | 50 | 2 811 120 | 3,7 s | 59 280 | 0,11 s |
      | 100 | 11 238 240 | 13,2 s | 118 560 | 0,16 s |

      (`Text wrap: auto`; con `pretty`, 8,1 s antes y 0,08 s después a 100 páginas.) Ahora
      `InlineLayout` memoriza el reparto en líneas por `(caja, ancho disponible)` y lo vacía en
      `reservePageDigits`, que es cuando cambian las medidas (y `Engine` vacía su caché).
- [x] `InlineLayout` recibe el `Engine` y calcula los intrínsecos con `engine.intrinsic`.
- [x] `LongTextTest.measurementsGrowLinearlyWithThePages`, en la suite normal (31 ms): con el
      doble de texto, menos de 2,5 veces las mediciones. Sin la memoria falla (3,99).

**Verificación.** `mvn test` (625 tests), sin cambios en los PNG ni en los `expected.stxt`: el
reparto es el mismo, solo se calcula una vez. El `Paginator` sigue volviendo a paginar si
`{pages}` no cabe en las cifras reservadas; se deja así.

## 12. PNG golden y antialiasing

**Problema.** Tras un `git pull` de un commit hecho en otro equipo, `mvn test` reescribía los
21 `page-N.png` con el layout intacto (los `expected.stxt` pasaban). `GoldenTest.samePixels`
exigía píxeles idénticos, y el antialiasing de PDFBox/Java2D cambia de una máquina a otra:
de 2 a 30 píxeles por imagen, con diferencias de hasta 10 (sobre 255) por canal, en las
esquinas de los contornos de depuración. Así, "un PNG modificado en git = el resultado ha
cambiado" daba falsos positivos en cada cambio de equipo y enseñaba a ignorar el aviso.

**Cómo.**

- [x] `samePixels` tolera una diferencia por canal de hasta `PIXEL_TOLERANCE` (16). Un cambio
      real de layout mueve líneas y texto, con diferencias cercanas a 255.
- [x] `CLAUDE.md` (sección Tests) lo explica.

**Verificación.** Con los PNG del otro equipo, `GoldenTest` ya no modifica ninguno; con un
cuadrado negro de 11 × 11 píxeles pintado en `04-grid/page-1.png`, lo reescribe.

**Límite conocido.** Si el PNG se reescribe por un cambio real, el fichero nuevo lleva
también el antialiasing de la máquina que lo genera. Un cambio de color de menos de 16 por
canal no reescribe el PNG; las posiciones las protege igualmente `expected.stxt`.

---

## Lo que no merece la pena (KISS)

Para no hacerlo por inercia:

- **Partir `Style` en un estilo por tipo de elemento.** Es una bolsa de campos públicos con
  propiedades de flex, grid y listas en todas las cajas (va contra ISP), pero es lo mismo que
  hace CSS con su estilo calculado y simplifica la herencia y las reglas. Basta con encapsular
  las claves de las líneas ([7](#7-partir-boxtreebuilder)).
- **Interfaces para todo** (`LayoutAlgorithm` ya basta), inyección de dependencias o un
  *framework* de plugins para los algoritmos: `Engine` con un `switch` es claro y suficiente.
- **Hacer `Engine` *thread-safe*.** Con [5](#5-quitar-la-geometría-mutable-de-box) el árbol deja
  de compartir estado; lo razonable es un `Engine` por documento, no sincronizar la caché.
- **Quitar la doble validación del schema** (`DocumentReader` y `BoxTreeBuilder`): es una
  decisión deliberada (la estructura solo vive en el schema) y su coste es despreciable.
- **Umbrales de cobertura** antes de tener el informe y haber visto qué falta: generan tests
  para cumplir el número, no para encontrar errores.

## Registro de sesiones

| Fecha | Tareas | Resultado | Siguiente paso |
|---|---|---|---|
| 2026-10-27 | Revisión inicial | Esta hoja de ruta; suite en verde (388 tests) | Tarea 1 |
| 2026-10-27 | 1 | Ramas `default` explícitas en `BoxTreeBuilder` y `SchemaCoverageTest` (161 pares); 549 tests en verde | Tarea 2 |
| 2026-10-27 | 12 (nueva) | `GoldenTest` tolera el antialiasing entre máquinas al comparar los PNG; 549 tests en verde | Tarea 2 |
| 2026-10-27 | 2 | `ConstraintSpace` con fábricas con nombre y *withers* que conservan `contentHeight`; `ConstraintSpaceTest`; 555 tests en verde | Tarea 3 |
| 2026-10-27 | 3 | `layOutDocument` devuelve `LaidOutDocument`; fachada `PdfWriter`; sin `Engine.cache()`; 555 tests en verde | Tarea 4 |
| 2026-10-27 | 4 | `SliceTest`, `BoxModelTest`, `LayoutCacheTest` y `UnitBreakerTest`; JaCoCo (95,1 % de líneas, 86,1 % de ramas); 587 tests en verde | Tarea 5; revisar las ramas de `Overflow` |
| 2026-10-27 | 5 | `Box` sin geometría; el margen usado va en `Fragment.margin`; sin `BoxModel.applyTo` ni `Fragment.complete`; 588 tests en verde | Revisar las ramas de `Overflow`; tarea 6 o 9 |
| 2026-10-27 | Ramas de `Overflow` | Dos bugs corregidos (`Relative` en padre partido, culpable horizontal), 16 casos en el catálogo; `Overflow` al 93,8 % de ramas; 606 tests en verde | Tarea 6 o 9; revisar `Placement` |
| 2026-10-27 | Culpable vertical de `Overflow` | `Aspect ratio`, `Rows`, `Padding`, `Border` y `Size` como culpables (§5.12); `Line height` heredado con su línea; 609 tests en verde | Tarea 6 o 9; revisar `Placement` |
| 2026-10-27 | 6 | `enum Element` en `Box.element`; sin listas paralelas ni `switch` de `layout` y `position`; `ElementTest` contra el schema; 613 tests en verde | Tarea 7 (partir `BoxTreeBuilder`) o 9; revisar `Placement` |
| 2026-10-27 | 7 | `BoxTreeBuilder` de 1 393 a 346 líneas: `ValueReader`, `PropertyReader`, `StyleRules`, `TableBuilder`, `ListBuilder`, `ModelValidator`, `BuildContext` y `Nodes`; claves en `PropertyKey` con `PropertyKeyTest`; 615 tests en verde | Tarea 8 (ciclos de packages) o 9; revisar `Placement` |
| 2026-10-27 | 8 | Sin ciclos de packages: colocación de grid, `FontFace`, `FontWeight` y `FontStyle` en `model`; `BreakAppeal` en `fragment`; `Paginator` en `layout`; `Areas` y `Area` se interpretan una vez, al leer; `ArchitectureTest`; 619 tests en verde | Tarea 9 (duplicaciones) o 10; revisar `Placement` |
| 2026-10-27 | 9 | `model.Tolerance` (una sola épsilon), `LayoutResult.before`, `Sizing.align`/`sum`, `Canvas.border` por defecto con `outline`, `layout.Messages`, `Fragment.contentWidth`/`contentHeight`, `LineBreaker.place`; 623 tests en verde | Tarea 10 (`UnitBreaker`) u 11; decidir el 0.01 de `Overflow`; revisar `Placement` |
| 2026-10-27 | 10 | `fragment.Unit` y `UnitBreaker.Units(List<Unit>, …)` con `slice`; `UnitBreaker.Arranged` común a flex, grid y tabla; `FlexLayout.Run.layOut` en pasos 6, 7 y 8; el 0.01 de `Overflow` se queda; 624 tests en verde | Tarea 11 (medir el texto largo); revisar `Placement` |
| 2026-10-27 | 11 | Medido: el texto largo costaba de forma cuadrática (100 páginas: 11,2 M mediciones, 13 s con `PdfMetrics`). `InlineLayout` memoriza el reparto en líneas y usa `engine.intrinsic`: 118 560 mediciones y 0,16 s; `LongTextTest` (lineal en la suite, tiempos con `@Tag("perf")`); 625 tests en verde | Revisar las ramas de `Placement`; hoja de ruta terminada |
| 2026-10-27 | Ramas de `Placement` | Bug: fila fija sin hueco (bucle sin límite y `ArrayIndexOutOfBoundsException`) → `GRID_PLACEMENT`; tests de fila y columna fijas; código muerto de `Occupancy`; `Placement` al 100 % de ramas; 631 tests en verde | Hoja de ruta terminada; hito 15 |
