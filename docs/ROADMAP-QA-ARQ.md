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
| 1 | [Ramas `default` silenciosas en la lectura de propiedades](#1-ramas-default-silenciosas) | QA | S | — | pendiente |
| 2 | [`ConstraintSpace`: *withers* que pierden `contentHeight` y booleanos posicionales](#2-constraintspace-withers-y-booleanos) | QA · bug latente | S | — | pendiente |
| 3 | [`layOutDocument` devuelve el documento entero; fin de la doble construcción](#3-resultado-del-documento-y-fachada) | ARQ · DRY | S | — | pendiente |
| 4 | [Tests de las piezas puras sin test y cobertura con JaCoCo](#4-tests-de-piezas-puras-y-cobertura) | QA | M | — | pendiente |
| 5 | [Quitar la geometría mutable de `Box`](#5-quitar-la-geometría-mutable-de-box) | ARQ · bug latente | M | 4 | pendiente |
| 6 | [`enum Element` en lugar de nombres de elemento como texto](#6-enum-element) | ARQ · DRY | M | 1 | pendiente |
| 7 | [Partir `BoxTreeBuilder`](#7-partir-boxtreebuilder) | ARQ · SRP | L | 1, 6 | pendiente |
| 8 | [Romper los ciclos de packages y parsear la colocación de grid una vez](#8-ciclos-de-packages-y-colocación-de-grid) | ARQ | M | 7 | pendiente |
| 9 | [Lote de duplicaciones pequeñas](#9-lote-de-duplicaciones) | DRY | S–M | — | pendiente |
| 10 | [Unidades de `UnitBreaker` como objetos](#10-unidades-de-unitbreaker) | ARQ · KISS | M | 4 | pendiente |
| 11 | [Rendimiento del texto largo (medir antes de tocar)](#11-rendimiento-del-texto-largo) | PERF | M | 4 | pendiente |

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

- [ ] Cada propiedad con su `case` explícito y `default -> throw new IllegalStateException(
      "Propiedad sin leer: " + name)` (es un error del motor, no del documento).
- [ ] Test `SchemaCoverageTest`: recorre las definiciones de `DocumentSchema` y, para cada
      propiedad de cada grupo y elemento, construye un documento mínimo que la declare y
      comprueba que `BoxTreeBuilder` no lanza `IllegalStateException` (los errores de valor del
      §6 sí se admiten). Así un nombre nuevo del schema sin lectura rompe un test.

**Verificación.** `mvn test`; el nuevo test falla si se comenta un `case`.

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

- [ ] `withRemaining` y `fragmented` conservan `contentHeight` (constructor canónico).
- [ ] Fábricas con nombre para los casos que se repiten: `ConstraintSpace.free(width, baseW,
      baseH)` (sondas de `BoxModel`), `ConstraintSpace.fixed(width, height, baseW, baseH)`
      (ítems de flex y grid, `AbsoluteLayout`), además de `forPage`.
- [ ] `ConstraintSpaceTest`: los *withers* conservan todo lo que no cambian.

## 3. Resultado del documento y fachada

**Problema.** `Engine.layOutDocument` construye el árbol y devuelve solo las páginas, así que
quien necesita el medio o los metadatos **vuelve a construir el árbol**:
[`PdfCanvas.java:89-90`](../src/main/java/dev/stxt/boxes/paint/PdfCanvas.java),
`GoldenTest:74-75` y otros 11 sitios de los tests. Es trabajo doble y, peor, el segundo árbol
tiene otras `Box` que las de los fragmentos. `PdfCanvas` mezcla dos responsabilidades: es un
`Canvas` y es la fachada "documento → PDF" que maqueta (SRP). Encaja con la idea del
[`TODO.md`](../TODO.md) de una API para integrar el motor con PDFBox.

**Cómo.**

- [ ] `record LaidOutDocument(Media media, Metadata metadata, List<Page> pages)` como
      resultado de `layOutDocument` (o un método nuevo, si se quiere conservar la firma).
- [ ] Sacar `PdfCanvas.write(InlineNode, …)` a una fachada (`PdfWriter` o `Boxes.toPdf`), y
      dejar en `PdfCanvas` solo el pintado y el `write(LaidOutDocument, …)`.
- [ ] Tests y `GoldenTest` usan el resultado en lugar de reconstruir.
- [ ] Quitar `Engine.cache()` (público y sin uso).

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

- [ ] `SliceTest`: tabla de casos (primer/intermedio/último fragmento × `slice`/`clone` × alto
      `auto`/definido/con mínimo y máximo × el contenido termina o no).
- [ ] `BoxModelTest`: porcentajes, `Aspect ratio`, `fixedWidth`/`fixedHeight`,
      `measuringContentHeight`, `intrinsic` con `Width` fijo y con mínimo y máximo.
- [ ] `LayoutCacheTest` / test de regresión de anidamiento: 8–10 niveles de `Flex` y `Grid`
      anidados; el número de *misses* crece de forma lineal (o se acota con `assertTimeout`).
- [ ] `UnitBreakerTest` directo con `Units` sintéticas: corte entre unidades, grupo que no se
      puede cortar, partición en paralelo, `Repeat`.
- [ ] JaCoCo en el `pom.xml`, solo el informe (sin umbral al principio), para ver qué ramas
      de `BoxTreeBuilder`, `Overflow` y `UnitBreaker` no ejecuta nadie.

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

- [ ] El margen usado viaja con el resultado: campo `margin` en `Fragment` (o que `Overflow`
      lo resuelva con la base que ya conoce, el ancho de contenido del padre).
- [ ] `BlockLayout` y `UnitBreaker` usan variables locales en lugar de `child.x`/`p.box().x`.
- [ ] `MarkerLayout` construye el `Fragment` sin pasar por la caja; quitar
      `Fragment.complete` o hacer que reciba borde y relleno.
- [ ] Borrar los siete campos y los cuatro métodos de geometría de `Box`, y
      `BoxModel.applyTo`. `Box` queda inmutable salvo `parent`/`children` durante la
      construcción (ver [7](#7-partir-boxtreebuilder)).
- [ ] Apuntar en `DECISIONES.md` que la geometría solo existe en los fragmentos.

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

- [ ] `enum Element { BLOCK("Block", BLOCK, STATIC, inFlow), …, HEADING_1("Heading 1", …, level 1),
      MARKER(…) }` con el nombre canónico, el que se escribe, el `Layout` y la `Position` por
      defecto, si está en flujo, si lleva texto y el nivel de encabezado.
- [ ] `Box.element` pasa a ser `Element` (la raíz, `null` o `Element.ROOT`).
- [ ] Desaparecen las listas paralelas y los dos `switch` de `element()`.

## 7. Partir `BoxTreeBuilder`

**Problema.** [`BoxTreeBuilder`](../src/main/java/dev/stxt/boxes/tree/BoxTreeBuilder.java) tiene
siete responsabilidades y estado mutable de instancia que se reinicia en `build` (no es
reentrante). Es la clase que más cambia en cada hito y la más difícil de revisar.

**Cómo** (solo mover código; el catálogo de errores y los tests de `tree` son la red):

- [ ] `ValueReader` (estático): `value`, `sides`, `tracks`, `gap`, `enumValue`, `color`,
      `integer`, `aspectRatio`… y `Kind` (≈ 300 líneas).
- [ ] `PropertyReader`: los grupos `Box`, `Typography`, `Break` y las propiedades de `Flex`,
      `Grid` e `Item`, compartidos por elementos y reglas.
- [ ] `StyleRules`: lectura de `Styles` y `applyRules`.
- [ ] `TableBuilder` (tabla → celdas colocadas) y `ListBuilder` (numeración y marcador).
- [ ] `ModelValidator`: reglas del §6 que miran la caja ya leída (`validateBox`, `Offset`,
      márgenes, mínimo y máximo, porcentajes, `definiteHeight`).
- [ ] Un `BuildContext` por llamada con `ids`, `definiteHeight`, `headingLevel`,
      `insideFixed` e `itemNumbers`, en lugar de campos que se reinician.
- [ ] Las claves de `Style.line(...)` (`"item.grow"`, `"flex.gap"`, `"font-size"` y el mapa
      `INTERNAL`) como constantes en un solo sitio: un error tipográfico hoy solo salta
      (`IllegalStateException`) cuando se produce el error del documento que la usa.

`BoxTreeBuilder` queda como orquestador de unas 300 líneas.

## 8. Ciclos de packages y colocación de grid

**Problema.** Dependencias entre packages (de `import`):

```
tree     -> layout          (BoxTreeBuilder usa layout.grid.Area, Cell y GridAreas)
layout   -> fragment -> layout   (BreakRules usa LayoutResult; Paginator usa Engine)
resources -> tree           (FontLibrary usa tree.FontFace)
```

El árbol de cajas depende del layout para validar, y `fragment` y `layout` son un solo
módulo partido en dos. Además la colocación se parsea muchas veces: `GridAreas.parse` corre en
`BoxTreeBuilder:851`, otra vez **por cada ítem** en `:904` y en **cada** layout e intrínseco del
grid (`GridLayout:60`); la tabla escribe la colocación de cada celda como texto
(`"3 1 1 2"`, `BoxTreeBuilder:651`) para que `Cell.parse` la vuelva a leer.

**Cómo.**

- [ ] `Area`, `Cell` y `GridAreas` (parseo puro) pasan a `model` (o a `tree.grid`).
- [ ] `Style` guarda la colocación ya parseada (`Cell`, y las áreas con nombre como
      `Map<String, Area>`); la tabla crea el `Cell` directamente.
- [ ] Las calidades de corte (`PERFECT_BREAK`…`FORCED`) salen de `LayoutResult` a
      `fragment.BreakAppeal`; `Paginator` pasa a `layout` (o `layout.page`).
- [ ] `FontFace` pasa a `model`.
- [ ] Test de arquitectura (ArchUnit, o uno a mano que lea los `import`) que prohíba los
      ciclos y que `tree` dependa de `layout` o `paint`.

## 9. Lote de duplicaciones

Pequeñas, independientes y de bajo riesgo; se pueden hacer de una en una:

- [ ] **Épsilon:** siete constantes `EPSILON = 1e-9` (`BlockLayout`, `Engine`, `Slice`,
      `UnitBreaker`, `TableLayout`, `BreakRules`, `TrackSizing` como `EPS`) y seis literales
      `1e-9` (`FlexLayout:366,429`, `FlexResolver:98`, `LineBreaker:60,109,130,132`). Una sola
      constante en `Sizing` con `fits(a, b)`/`same(a, b)`. `Overflow` usa 0.01 a propósito
      (tolerancia del documento): que se vea en el nombre.
- [ ] **"No cabe nada":** el mismo `LayoutResult` con `BreakToken.BEFORE` en
      `BlockLayout:277`, `UnitBreaker:309` e `InlineLayout:127` → `LayoutResult.before(box)`.
- [ ] **Alineación en un eje:** el `switch START/STRETCH/END/CENTER` de `FlexLayout:232` y
      `GridLayout:300` → `Sizing.align(alignment, space, size)`. `sum(double[])` está en las dos.
- [ ] **Bordes del canvas:** `border()` es idéntico en `SvgCanvas:47` y `PdfCanvas:171` →
      método por defecto en `Canvas` que pinta cuatro `background`, con un `outline(Rect)` para
      el contorno de depuración.
- [ ] **Mensajes:** `name(Box)` y `pt(double)` en `Overflow:226-230` y `TableLayout:99-103`, con
      **formatos distintos** (`2.5` frente a `2.50` según la clase) → un `Messages` compartido.
- [ ] **Caja de contenido de un fragmento:** `f.width() - f.border().horizontal() -
      f.padding().horizontal()` en `Overflow`, `InlineLayout.number` y otros →
      `Fragment.contentWidth()`, `contentHeight()`.
- [ ] **Colocar una línea:** medir, alinear, `wordSpacing` y ancho final está dos veces en
      `InlineLayout` (`layout` y `number`) → un método de `LineBreaker` que devuelva la
      `TextLine`.

## 10. Unidades de `UnitBreaker`

**Problema.** `UnitBreaker.Units` son cuatro arrays paralelos (`starts`, `ends`, `appeal`,
`forced`) dentro de un `record`, cuyo `equals` compara los arrays por identidad. `FlexLayout`
los monta con tres listas paralelas (`starts`, `ends`, `groups`, líneas 209-266) y
`TableLayout` los recorta con cuatro `Arrays.copyOfRange`. `UnitBreaker.fragment` recibe nueve
parámetros. Es el código más difícil de seguir del motor y el hito 15 lo va a reutilizar.

**Cómo.**

- [ ] `record Unit(double start, double end, int appealBefore, boolean forcedBefore)` y
      `Units(List<Unit>, extent, splittable)` con `slice(from, to, origin)` para la tabla.
- [ ] `GridLayout.Arranged` pasa a `layout` como resultado común de "contenedor maquetado sin
      fragmentar" (lo producen flex y grid y lo consume `UnitBreaker`), y `fragment` recibe
      `(engine, box, space, from, arranged, repeat)`.
- [ ] `FlexLayout.Run.layOut` (110 líneas) separa los pasos 6-8 de la construcción de las
      unidades.

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

- [ ] Documento de prueba de 100 páginas de texto (no en la suite normal, o con
      `@Tag("perf")`) y medir.
- [ ] Si se nota: memorizar el reparto en líneas por `(box, ancho disponible)` dentro de
      `InlineLayout`, vaciado con la caché en `reservePageDigits`.
- [ ] `InlineLayout` recibe el `Engine` (como los demás algoritmos) para usar su caché.

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
