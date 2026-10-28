# Hoja de ruta: Markdown, imágenes, pruebas y ejemplos

Plan de trabajo para escribir el contenido de un documento en Markdown (un subconjunto cerrado
que se traduce a los elementos que el motor ya conoce), para poner imágenes en él y para
ordenar las pruebas y hacer ejemplos de documentos completos. Es el punto
de partida de cada sesión: el estado, las decisiones tomadas y lo que queda por hacer están
aquí, no en la memoria de la conversación.

Los hitos 1 a 15 están terminados; ver la tabla de estado del [README](../README.md). Las
hojas de ruta de los hitos 11 a 15 y de calidad y arquitectura están en el historial de git
(commit `1b808ac`). Esta hoja de ruta continúa la numeración.

## Cómo usar este documento

**Al empezar una sesión:**

1. Lee el [estado](#estado) y el [registro de sesiones](#registro-de-sesiones) para saber
   dónde se quedó el trabajo.
2. Abre el hito en curso: sus **decisiones abiertas** se resuelven con el usuario antes de
   escribir código; sus **tareas** dicen qué falta.
3. Ejecuta `mvn test` para comprobar que se parte de un estado verde.

**Al terminar una sesión (o un bloque de trabajo):**

1. Marca las tareas hechas (`[x]`) y actualiza la columna de estado del hito.
2. Mueve las decisiones resueltas de "abiertas" a "tomadas", con la respuesta. Las que sean
   interpretaciones de la especificación van además a [`DECISIONES.md`](DECISIONES.md).
3. Añade una entrada al [registro de sesiones](#registro-de-sesiones): fecha, qué se hizo, en
   qué se quedó y cuál es el siguiente paso concreto.

**Estados:** `pendiente` · `diseño` (resolviendo decisiones) · `en curso` · `hecho`.

## Estado

| Hito | Trabajo | Estado | Caso golden |
|---|---|---|---|
| 16 | Tramos con formato dentro del texto (negrita, cursiva, subrayado, tachado) | hecho | 13 (con el 17) |
| 17 | Elemento `Markdown`, traducido a elementos conocidos | hecho | 13 |
| 18 | Imágenes (`Image`): PNG y JPEG de la carpeta de recursos | hecho | 14 |
| 19 | Los casos de prueba pasan de `src/test/resources` a `test/` | hecho | — |
| 20 | Catálogo de errores: un fichero STXT por caso, en carpetas por grupo | hecho | — |
| 21 | Ejemplos de verdad: once documentos completos, con PDF y PNG limpios | hecho | `examples/` |

Los hitos 16 y 17 están terminados (2026-10-27), y el 18, el 2026-10-28.

**Orden:** primero los tramos, porque Markdown los necesita para el formato dentro de una
línea y son lo único que toca el layout. `Markdown` solo toca la construcción del árbol: todo
lo que genera ya se maqueta, se valida y se etiqueta.

## Criterios comunes (definición de hecho)

Cada hito está terminado cuando, además de sus tareas propias, cumple lo de "Al cambiar el
comportamiento" de [`CLAUDE.md`](../CLAUDE.md):

- [ ] Schema [`dev.stxt.boxes.stxt`](../src/main/resources/stxt/dev.stxt.boxes.stxt): elementos y
      propiedades nuevos. **Cada nombre tiene una única definición en todo el namespace**: un
      nombre que ya existe solo se reutiliza si significa lo mismo y admite los mismos valores.
- [ ] Especificación: §2 (tabla de elementos), §4 (referencia de propiedades), §5 (semántica),
      §6 (reglas de validación nuevas), §7 (volcado), §8 (diferencias con CSS/HTML), §9
      (resumen del schema) y, si aplica, §10 (ejemplo).
- [ ] `Style`, el builder (lectura y validación del §6) y, si hay layout nuevo, su algoritmo.
- [ ] Cada regla de validación nueva: su `ErrorCode`, su fila en el §6 y su caso en
      [`test/errors/`](../test/errors).
- [ ] Tests unitarios de las piezas puras y tests de layout con `LayoutFixture`, incluidos los
      de fragmentación si el hito toca el corte.
- [ ] Un caso golden nuevo en `test/golden/NN-nombre/`, con el resultado
      calculado a mano.
- [ ] `DocumentReaderTest` valida los documentos nuevos.
- [ ] Salida SVG y PDF revisada a ojo (PNG del caso golden).
- [ ] README (tabla de estado y hoja de ruta), `CLAUDE.md` (tabla de hitos) y este documento
      actualizados; decisiones apuntadas en `DECISIONES.md`.

## Decisiones transversales

**Tomadas** (2026-10-27, todas con la propuesta aceptada):

- [x] **T4. Texto con formato dentro de una línea** (abierta en la hoja de ruta anterior). Se
      hace en el hito 16, como tramos (*runs*) dentro del texto de una caja.
- [x] **M6. Sintaxis de los tramos.** El formato dentro de una línea solo se escribe en
      `Markdown`: `Text` y `Paragraph` siguen siendo literales (no hay que escapar los
      asteriscos de los documentos que ya existen). El modelo de tramos es general, por si más
      adelante se quiere una sintaxis STXT propia.

---

## Hito 16: tramos con formato dentro del texto

**Objetivo.** Que el texto de una caja pueda mezclar negrita, cursiva, subrayado y tachado
dentro de una misma línea, con las métricas reales de cada variante de la fuente.

**Sintaxis.** Ninguna nueva en el documento (M6): los tramos los crea el hito 17. Hasta
entonces se prueban construyendo el árbol en los tests.

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-27, todas con la propuesta aceptada; detalle en
[`DECISIONES.md`](DECISIONES.md), 120–126):

- [x] R1. **Qué cambia de un tramo a otro:** solo la negrita, la cursiva, el subrayado y el
      tachado, que se suman a la fuente de la caja. `Font`, `Size`, `Color` y `Line height`
      son los de la caja, así que la altura de la línea y la línea base no cambian.
- [x] R2. **Subrayado y tachado:** con el color del texto; posición y grosor de la fuente
      (`underlinePosition` y `underlineThickness` de la tabla `post`; `yStrikeoutPosition` y
      `yStrikeoutSize` de `OS/2`), con una aproximación en SVG. Como `text-decoration` de CSS,
      cubren también los espacios de dentro del tramo, pero no el espacio entre dos tramos que
      no lo llevan.
- [x] R3. **Palabras partidas entre tramos** (`**neg**rita`): son una sola palabra para el
      reparto en líneas; su ancho es la suma de las partes.
- [x] R4. **Volcado (§7):** ni las líneas ni sus tramos se vuelcan. Se comprueban con tests de
      layout y de pintado.
- [x] R5. **PDF etiquetado:** los tramos son contenido del mismo `P` (o `Span`, `H1`…,
      `LBody`, `TD`), sin elemento propio; las rayas son *artifacts*.
- [x] R6. **Errores:** la variante de cada tramo debe existir (`FONT_VARIANT_MISSING`, con la
      línea del tramo) y tener todos sus caracteres (`GLYPH_MISSING`).

**Diseño técnico** (como quedó):

- **Modelo.** `model.TextRun` (texto y `TextFormat`) y `model.TextFormat` (cuatro marcas;
  `face(base)` da la fuente del tramo). `Box.text` sigue siendo el texto entero y `Box.runs`
  lo parte (`null` sin formato; `Box.runs()` da entonces un único tramo).
- **Reparto.** `LineBreaker.paragraphs` saca las palabras (`LineBreaker.Word`: sus partes y el
  formato del espacio que la sigue) y el voraz y Knuth-Plass trabajan sobre ellas; cada línea es
  un `LineBreaker.Line` con los tramos seguidos del mismo formato unidos. Un texto sin formato
  hace las mismas mediciones que antes. La memoria del reparto de `InlineLayout` sigue sirviendo.
- **Líneas.** `TextLine.runs` (`TextLine.Run`: desplazamiento, ancho con el espaciado de
  `justify`, texto y formato). `InlineLayout.number` sustituye `{page}`/`{pages}` en cada tramo.
- **Pintado.** `StackingContext.paintText` pinta cada tramo con su fuente y
  `paintDecorations`, las rayas (`Canvas.decoration`, de `FontFile.decoration` en PDF y
  `DecorationMetrics.APPROXIMATE` en SVG).
- **Validación.** `PdfMetrics.check` recorre los tramos con su variante.
- **Tests.** Hasta el hito 17 no hay sintaxis: `TextRuns` (en los tests) pone los tramos en el
  árbol construido, y `Engine.layOutDocument(BoxTreeBuilder.Result)` lo maqueta.

**Tareas:**

- [x] Resolver R1–R6.
- [x] Tramos en `model` y en `Box`; los textos de hoy, un único tramo.
- [x] `LineBreaker` e `InlineLayout` con palabras de varias partes; `TextLine` con tramos.
- [x] Métricas de subrayado y tachado en `FontFile`; pintado en `StackingContext` (y
      `Canvas.decoration` en `PdfCanvas`).
- [x] Validación de las variantes de cada tramo.
- [x] Tests de las piezas puras y de layout (justificado, Knuth-Plass, palabras partidas
      entre tramos, numeración, cortes de página) y de pintado (SVG y PDF).
- [x] Especificación (§5.4, §5.11, §6, §7, §8 y glosario); PDF revisado a ojo
      (`PdfCanvasTest.runsUseTheirFontsAndDecorationsAreArtifacts`, `target/pdf/runs.pdf`).

**Caso golden:** pasa al hito 17, porque `input.stxt` necesita la sintaxis de `Markdown`.

---

## Hito 17: elemento `Markdown`

**Objetivo.** Escribir el contenido en Markdown y que se traduzca, al construir el árbol, a
los elementos que ya existen. Nada nuevo en el layout: lo generado pasa por el schema,
`Styles`, la validación del §6, el PDF etiquetado y los marcadores como si lo hubiera escrito
el autor.

**Sintaxis.** Como `Text`: las propiedades en el elemento y el contenido en un bloque
`Content >>` (un nodo `>>` no admite hijos).

```
Markdown:
	Class: informe
	Bullet: disc dash
	Numbering: decimal
	Table columns: equal
	Content >>
		# Resultados
		Texto con **negrita**, *cursiva*, ++subrayado++ y ~~tachado~~.

		- uno
		- dos
		  1. dos punto uno

		| Mes | Total |
		|-----|------:|
		| Ene |   120 |
```

**Traducción:**

| Markdown | Elemento |
|---|---|
| `#` … `######` | `Heading 1` … `Heading 6` (un `Text` hijo) |
| Párrafo | `Paragraph` |
| `-`, `*`, `+` | `List` con el `Marker` de `Bullet` para su nivel, y un `List item` por ítem |
| `1.`, `1)` | `List` con el `Marker` de `Numbering` y `Start` = el primer número |
| Tabla GFM | `Table` con `Columns` de `Table columns`; la fila de cabecera en `Head`, el resto en `Body` |
| `**x**`, `__x__` | tramo en negrita |
| `*x*`, `_x_` | tramo en cursiva |
| `++x++` | tramo subrayado |
| `~~x~~` | tramo tachado |

**Decisiones tomadas** (2026-10-27, todas con la propuesta aceptada):

- [x] M1. **Subrayado con `++x++`**, la sintaxis de markdown-it y de la extensión `ins` de
      commonmark-java. `__x__` es negrita en CommonMark y `<u>` sería HTML.
- [x] M2. **Marcadores en propiedades de `Markdown`:** `List` exige `Marker` y `Styles` no
      puede fijarlo (es propiedad del elemento, no de un grupo). `Bullet` es una lista de
      marcadores, uno por nivel de anidamiento (el último se repite en los niveles más
      profundos), y `Numbering`, igual para las listas numeradas. `Start` sale del primer
      número de la lista.
- [x] M3. **Columnas de las tablas en `Table columns`:** `equal` (`repeat(n, 1fr)`) o `auto`
      (`repeat(n, auto)`). La alineación de la fila de separación (`:--`, `:-:`, `--:`) es el
      `Align` de las celdas de esa columna.
- [x] M4. **`#` es siempre `Heading 1`**, con la validación de jerarquía de siempre. Si hace
      falta meter Markdown debajo de un encabezado del documento, se añadirá un
      `Heading offset`.
- [x] M5. **Estilo:** lo generado no lleva `Id` ni `Class`; se estila con las reglas `Element`
      de `Styles` (T1). `Markdown` es un bloque con `Id`, `Class`, `Box`, `Typography` y
      `Break`, y lo que contiene hereda de él.
- [x] **Parser: commonmark-java** (`org.commonmark:commonmark`, con las extensiones de tablas
      GFM, tachado e `ins`) en lugar de uno propio: las reglas de énfasis de CommonMark son
      difíciles de reproducir, da la línea de cada nodo (*source spans*) y no tiene
      dependencias.
- [x] **Subconjunto cerrado:** se aceptan solo los nodos de la tabla de traducción. Enlaces,
      imágenes, código, citas, HTML, líneas horizontales y saltos de línea forzados son un
      error (`MARKDOWN_UNSUPPORTED`) con su línea.

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** en el diseño (2026-10-27, todas con la propuesta aceptada; las
interpretaciones, en [`DECISIONES.md`](DECISIONES.md), 127–134):

- [x] K1. **Dónde se admite `Markdown`:** donde va un elemento de flujo (como `List`): en
      `Document`, `Block`, los posicionados, los envoltorios, `List item`, `Cell` y
      `Multicol`. Dentro de un `Fixed`, solo sin encabezados (los encabezados en `Fixed` ya son
      un error).
- [x] K2. **Ítems de lista:** cada párrafo de un ítem es un `Paragraph`; una lista anidada,
      una `List` dentro del `List item`. Listas compactas y separadas (*tight* y *loose*) se
      traducen igual: el espaciado lo dan las reglas de `Styles`.
- [x] K3. **Celdas:** una celda con texto es un `Cell` con valor y tramos; una celda vacía,
      un `Cell` vacío.
- [x] K4. **Saltos de línea simples** dentro de un párrafo equivalen a un espacio, como en
      `Text` (§5.4).
- [x] K5. **Líneas de los errores:** las de lo generado son la línea del documento donde
      empieza su bloque de Markdown (la del `Content >>` más el desplazamiento).
- [x] K6. **Propiedades obligatorias:** `Bullet`, `Numbering` y `Table columns` solo son
      obligatorias si el contenido tiene listas de ese tipo o tablas; si faltan, error con la
      línea de la lista o la tabla. Si sobran, no es error (decisión 130).

Al implementar salió además que `Marker` solo tiene `disc`, `dash` y `none` como viñetas (el
ejemplo de arriba usaba `circle` y `square`, que no existen), y que una tabla GFM sin filas
debajo de la cabecera no tiene traducción (una `Table` necesita su `Body`).

**Diseño técnico** (como quedó):

- **Traducción a nodos STXT.** `tree/MarkdownBuilder` parsea el `Content` con commonmark-java
  0.30.1 (tablas y tachado de GFM, con dos tildes, y la extensión `ins`) y crea `InlineNode`
  de `stxt-core` con la línea de su bloque (`Heading N` con un `Text`, `Paragraph`, `List` con
  `Marker` y `Start`, `List item`, `Table` con `Columns`, `Head`, `Body`, `Row`, `Cell` con su
  `Typography`/`Align`). Lo generado se valida con el schema y `BoxTreeBuilder.markdown` lo
  construye con `element(...)` como hijos de la caja del `Markdown` (`Element.MARKDOWN`, un
  bloque), así que `Styles`, `ModelValidator`, la jerarquía de encabezados, el PDF etiquetado y
  los marcadores lo tratan como cualquier otro elemento.
- **Tramos.** `Translation.runs` guarda aparte los tramos de cada `Text`, `Paragraph` y `Cell`
  generado con formato; van a `BuildContext.runs` y el builder los pone en `Box.runs`.
  `TextRun.Joiner` une los tramos seguidos del mismo formato (lo usa también `LineBreaker`).
- **Errores.** `MARKDOWN_UNSUPPORTED` y `MARKDOWN_PROPERTY_MISSING`, nuevos; `TEXT_CONTENT`
  para los textos vacíos y `VALUE_NOT_ALLOWED` para los marcadores de `Bullet` y `Numbering`.

**Tareas:**

- [x] Resolver K1–K6.
- [x] Dependencia de commonmark-java y sus extensiones en `pom.xml`.
- [x] Schema (`Markdown`, `Bullet`, `Numbering`, `Table columns`) y `Element`.
- [x] `MarkdownBuilder`: bloques (encabezados, párrafos, listas anidadas, tablas) y tramos.
- [x] `MARKDOWN_UNSUPPORTED` y los errores de K6, con sus casos en el catálogo (11 casos).
- [x] Tests de la traducción (`MarkdownBuilderTest`), del árbol (`MarkdownTest`) y del PDF
      etiquetado (`PdfCanvasTest.markdownIsTaggedAndItsHeadingsAreBookmarks`).
- [x] Especificación (§2, §4.14, §5.4, §5.17 nueva, §6, §8, §9, glosario y un ejemplo);
      caso golden `13-markdown` de dos páginas, calculado a mano y con los PNG revisados.

---

## Hito 18: imágenes

**Objetivo.** Poner imágenes de la carpeta `images/` de los recursos (§5.11, hasta ahora reservada) en
el documento: en el flujo, como ítem de un `Flex` o un `Grid`, en una celda y en un `Absolute`
o un `Fixed` (el logotipo de la cabecera de cada página). Una imagen es una caja hoja, como un
`Text`, con un tamaño y una proporción propios que salen del fichero.

**Sintaxis.**

```
Image: logos/acme.png
	Id: logo
	Alt: Logotipo de Acme
	Fit: contain
	Box:
		Width: 120
	Break:
		After: avoid
```

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-28, todas con la propuesta aceptada; las interpretaciones que
salieron al implementarlas, en [`DECISIONES.md`](DECISIONES.md), 135–143):

- [x] I1. **Elemento y dónde va.** Un elemento `Image` con la ruta en el valor, hoja (sin
      elementos hijos), que admite `Id`, `Class`, `Box`, `Break`, `Alt` y `Fit`. Va donde va
      un elemento en flujo (como `Markdown`): en `Document`, `Block`, los posicionados,
      `Item`, `Area`, `List item`, `Cell` y `Multicol`. *Propuesta:* así. No admite
      `Typography` propia (no tiene texto), pero hereda `Color` para `Border color`.
- [x] I2. **Ruta.** *Propuesta:* relativa a `images/` (`Image: logos/acme.png`), no a la raíz de
      los recursos: es más corta y no puede señalar a `fonts/`. Pasa por `Resources.resolve`
      (nunca hacia atrás). *Alternativa:* relativa a la raíz (`images/logos/acme.png`), como
      dice hoy el ejemplo del §5.11.
- [x] I3. **Formatos.** *Propuesta:* PNG (con transparencia) y JPEG, que PDFBox incrusta sin
      dependencias nuevas (el JPEG tal cual, sin recomprimir). El formato se reconoce por el
      contenido, no por la extensión. GIF, WebP, SVG y el resto son un error. Sin
      remuestreo: se incrusta la resolución del fichero, una sola vez aunque se use en varias
      páginas.
- [x] I4. **Tamaño natural.** Cuántos puntos mide un píxel. *Propuesta:* como CSS, 96 ppp
      (1 px = 0,75 pt), sin mirar la resolución que declare el fichero (`pHYs`, JFIF), que
      suele faltar o estar mal. *Alternativas:* 72 ppp (1 px = 1 pt), o la resolución del
      fichero con 96 (o 72) si no la declara.
- [x] I5. **Dimensionado.** La imagen es una caja con proporción propia (la del fichero):
      - con `Width` y `Height` en `auto`, mide su tamaño natural, **también en flujo de
        bloque** (no se estira al ancho del padre, como un `<img>`); si no cabe, es un
        `BOX_OVERFLOW`, como cualquier caja, y se arregla declarando `Width: 100%`;
      - con un eje declarado, el otro sale de la proporción; min-content y max-content son el
        ancho natural (o el declarado);
      - un `Aspect ratio` declarado sustituye a la proporción del fichero (no es error).
      *Propuesta:* así, como un elemento reemplazado de CSS.
- [x] I6. **Imagen en una caja de otra proporción** (`Width` y `Height` declarados, un ítem
      estirado, un `Aspect ratio` distinto). *Propuesta:* propiedad `Fit` del elemento, con
      `contain` (cabe entera, centrada; por defecto), `cover` (llena la caja, centrada y
      recortada) y `fill` (se deforma). *Alternativas:* deformar siempre (el `object-fit: fill`
      por defecto de CSS) o que una proporción distinta sea un error.
- [x] I7. **Cortes de página.** La imagen no se parte: si no cabe en lo que queda de página, pasa
      entera a la siguiente, y si es más alta que el área útil, `PAGE_OVERFLOW`. `Before` y
      `After` funcionan como en cualquier caja. *Propuesta:* `Inside` y `Decoration` en una
      `Image` son un error (no tienen efecto en algo que no se parte), con un código nuevo.
      *Alternativa:* no admitirlos sin más (se ignoran).
- [x] I8. **Accesibilidad.** *Propuesta:* `Alt` opcional. Con `Alt`, la imagen es un `Figure`
      del PDF etiquetado con ese texto alternativo; sin `Alt`, es decorativa y se pinta como
      *artifact*. *Alternativa:* `Alt` obligatorio, con un `Alt` vacío o una propiedad
      `Decorative` para las decorativas.
- [x] I9. **Markdown.** *Propuesta:* una imagen sola en su párrafo (`![Alt](logos/acme.png)`) se
      traduce a una `Image` con ese `Alt`; una imagen mezclada con texto sigue siendo
      `MARKDOWN_UNSUPPORTED` (no hay cajas dentro de una línea). El título (`"…"`) se ignora o
      es un error (quedó como error, decisión 140). *Alternativa:* dejar las imágenes de Markdown para otro hito.
- [x] I10. **Errores nuevos.** *Propuesta:* `IMAGE_UNKNOWN` (no existe o la ruta sale de la
      carpeta; línea del `Image`) e `IMAGE_FORMAT` (no es PNG ni JPEG o no se puede leer), y
      el de I7. Se comprueban al empezar a maquetar, con las fuentes, porque el tamaño natural
      hace falta para el layout.

**Diseño técnico** (como quedó):

- **Modelo.** `Element.IMAGE` y `Style.Layout.IMAGE`, una caja sin hijos; la ruta en
  `Box.image` y `Alt` y `Fit` en `Style`. El builder solo comprueba que hay ruta y que no hay
  `Inside` ni `Decoration` (`BREAK_IN_IMAGE`, también de una regla).
- **Recursos.** `Resources.images()` da la `ImageLibrary`, que lee una vez cada fichero
  (`ImageFile`: formato por el contenido y tamaño en píxeles, con `ImageIO`); `Resources.resolve`
  admite una subcarpeta y no deja salir de ella. Los errores del fichero son
  `ImageFormatException` o `ResourceException`, sin línea.
- **Layout.** `Engine` tiene las imágenes de una carpeta de recursos (`layout/image/Images`), las
  comprueba antes de maquetar con la línea de la `Image` y da su tamaño natural
  (`Engine.natural`). `BoxModel.resolve(box, space, engine)` dimensiona la imagen como un
  elemento reemplazado (`BoxModel.replaced`), así que las sondas de flex, grid y absolute la
  miden bien; `ImageLayout` da los intrínsecos y la coloca sin partirla.
- **Pintado.** `Canvas.image(box, content)`, en el paso del texto de `StackingContext`;
  `ImageFit` calcula dónde va la imagen. `PdfCanvas` incrusta cada fichero una vez
  (`PDImageXObject.createFromFileByContent`) y recorta a la caja de contenido; `SvgCanvas` la
  incrusta como `data:` si recibe los recursos. `PdfStructure` crea un `Figure` con `Alt` y
  `BBox`; sin `Alt`, la imagen es un *artifact*.
- **Markdown.** `MarkdownBuilder.Translator.image`: un párrafo que solo tiene una imagen.

**Tareas:**

- [x] Resolver I1–I10.
- [x] Schema (`Image`, `Alt`, `Fit`) y `Element`; especificación (§2, §4, nueva §4.15, §5.1,
      §5.9–§5.13, §5.17, nueva §5.18, §6, §8, §9 y glosario).
- [x] `ImageLibrary` y su validación (errores con línea, casos en el catálogo: 14 nuevos).
- [x] Dimensionado en `BoxModel` e `ImageLayout` (tamaño natural, proporción, `Fit`, mínimos y
      máximos, flex, grid, absolute, tablas, columnas, cortes).
- [x] Pintado en PDF y SVG; `Figure` y *artifacts* en el PDF etiquetado.
- [x] Imágenes en `Markdown`.
- [x] Tests (`ImageLibraryTest`, `ImageLayoutTest`, `ImageFitTest`, `SvgCanvasTest`,
      `PdfCanvasTest.imagesAreFiguresOrArtifactsAndAreEmbeddedOnce`, `MarkdownBuilderTest`); caso
      golden `14-images` de dos páginas, calculado a mano y con los PNG revisados;
      `DocumentReaderTest`.
- [x] README, `CLAUDE.md`, `DECISIONES.md` (135–143) y este documento.

---

## Hitos 19 a 21: pruebas y ejemplos

Decisiones tomadas (2026-10-28, todas con la propuesta aceptada):

- [x] **P1. Estructura.** `test/{golden,errors,examples}` en la raíz del repositorio. Los tests
      Java siguen en `src/test/java` y `resources/` (fuentes e imágenes) se queda donde está.
      Los tests leen de disco (`Path.of("test", …)`), no del classpath; `src/test/resources`
      desaparece, y con él la entrada de `.classpath` y de `.settings`.
- [x] **P2. Errores.** Un fichero por caso con el documento real, en `test/errors/<grupo>/`, un
      grupo por familia de `ErrorCode`. El código y la línea esperados van en comentarios al
      principio del fichero, y la línea es la del propio fichero.
- [x] **P3. Ejemplos.** Cada carpeta de `test/examples/` lleva `input.stxt`, `expected.stxt`
      (volcado de las cajas con `Id`), `output.pdf` y un `page-N.png` sin contornos. El test
      compara el volcado y los PNG con tolerancia, como el golden. El PDF se versiona, pero solo
      se reescribe si cambia algún PNG (PDFBox mete fechas e identificadores).
- [x] **P4. Material.** IBM Plex Serif y Plex Mono (OFL) además de Plex Sans, e imágenes
      generadas por código (logotipos, fotos sintéticas, gráficos), sin derechos de terceros.

**Hito 19.** Mover los ficheros (`git mv`, con el historial), cambiar los tests, `.gitignore`,
Eclipse y los enlaces de la documentación.

**Hito 20.** Convertir `errors.stxt` en un fichero por caso y adaptar `ErrorCatalogTest`
(`everyCodeHasACase` sigue exigiendo un caso por código).

**Hito 21.** Diez documentos completos como mínimo (salieron once) que cubran necesidades
documentales distintas. Lo que falte en el motor para hacerlos bien (por ejemplo, índices o
enlaces) se apunta como idea en `TODO.md`, no se implementa dentro del hito.

**Los ejemplos:** factura, carta comercial, currículum, informe anual (7 páginas), boletín,
carta de restaurante (tamaño propio), contrato, manual, catálogo, certificado (apaisado) y horario
(apaisado). Cada uno enseña algo distinto; el índice, con miniaturas y lo que muestra cada uno,
está en [`test/examples/README.md`](../test/examples/README.md).

**Diseño técnico** (como quedó):

- **`ExamplesTest`** recorre `test/examples/*/`, lee y maqueta `input.stxt` con las métricas
  reales (`PdfMetrics`) y la carpeta `resources/`, compara el volcado con `expected.stxt` y deja
  el PDF y una imagen por página (110 ppp, sin contornos). El volcado es una instantánea: se
  genera con `-Dexamples.update=true`. El PDF solo se reescribe si cambian los píxeles de alguna
  página (PDFBox mete fechas e identificadores). `-Dexamples.only=NN-nombre` prueba uno solo.
- **`golden/ReferenceFiles`** reúne lo que comparten `GoldenTest` y `ExamplesTest`: comparar
  volcados (`compare`) y escribir las imágenes de un PDF sin ruido (`writePageImages`).
- **Material.** IBM Plex Serif y Mono (los TTF de [IBM/plex](https://github.com/IBM/plex), con su
  licencia OFL) junto a Plex Sans, e imágenes en `resources/images/examples/`, generadas por
  `tools/ExampleImages.java` con Java2D (logotipos, retrato, fotos, gráficos, productos, sello y
  firmas), sin ficheros de terceros. `FontLibraryTest` comprueba las tres familias.
- **`test/spec/report.stxt`**: el ejemplo del §10 de la especificación, que antes era
  `examples/report.stxt` y no es un ejemplo de los nuevos.

**Tareas:**

- [x] `ExamplesTest`, `ReferenceFiles` y el movimiento de `report.stxt` a `test/spec/`.
- [x] Fuentes IBM Plex Serif y Mono, y el generador de imágenes (`tools/ExampleImages.java`).
- [x] Los once documentos, revisados a ojo en sus PNG, cada uno con su `expected.stxt`.
- [x] `test/examples/README.md`, README, `CLAUDE.md`, `DECISIONES.md` (144–150) y `TODO.md`
      (huecos del motor que salieron al escribirlos).

---

## Registro de sesiones

Una entrada por sesión, la más reciente arriba: fecha, qué se hizo, dónde se quedó y el
siguiente paso concreto.

- **2026-10-28 (3).** Se abren los hitos 19 a 21 (P1–P4 aceptadas) y se terminan los tres.
  Hito 19: `test/` sustituye a `src/test/resources`. Hito 20: los 148 casos de `errors.stxt` son
  148 ficheros en 14 carpetas por grupo, con cabecera `# Case/# Code/# Line` (la línea es la del
  fichero); `ErrorCatalogTest` recorre `test/errors`. Hito 21: `ExamplesTest`, Plex Serif y Mono,
  el generador de imágenes y once documentos completos (el de más de una página con cabecera
  repetida, columnas, portada a sangre, tablas con `Span`…). El motor no necesitó ningún
  cambio: lo que se echó en falta está en `TODO.md`. `mvn test` en verde (809 tests).
  *Siguiente paso:* repasar las decisiones de `DECISIONES.md` y elegir el siguiente trabajo
  (ideas en `TODO.md`, empezando por las que salieron de los ejemplos).
- **2026-10-28 (2).** Hito 18 terminado. Aceptadas I1–I10. Schema (`Image`, `Alt`, `Fit`);
  `ImageLibrary` e `ImageFile`; dimensionado de elemento reemplazado en `BoxModel` e
  `ImageLayout`; pintado en PDF (una copia por fichero, `Figure` con `Alt` y `BBox`) y SVG;
  imágenes solas en su párrafo de Markdown; tres `ErrorCode` nuevos y 14 casos en el catálogo.
  Imágenes de prueba en `resources/images/test/`. Caso golden `14-images`, que pasa con el
  resultado calculado a mano. Especificación (§4.15 y §5.18 nuevas), `DECISIONES.md` (135–143),
  README y `CLAUDE.md`. `mvn test` en verde.
  *Siguiente paso:* los hitos de esta hoja de ruta están terminados. Pendiente: repasar las
  decisiones de `DECISIONES.md` y elegir el siguiente trabajo (ideas en `TODO.md`).
- **2026-10-28.** Se abre el hito 18 (imágenes), con las decisiones I1–I10 y su propuesta.
  `mvn test` en verde (746 tests).
  *Siguiente paso:* resolver I1–I10 con el usuario y empezar por el schema y `ImageLibrary`.

- **2026-10-27 (3).** Hito 17 terminado. Aceptadas K1–K6. commonmark-java 0.30.1; schema
  (`Markdown`, `Bullet`, `Numbering`, `Table columns`, admitido donde va un `List`);
  `MarkdownBuilder` traduce a nodos STXT y `BoxTreeBuilder` los construye; dos `ErrorCode`
  nuevos y 11 casos en el catálogo. Caso golden `13-markdown` (también el del hito 16), que pasa
  con el resultado calculado a mano. Especificación, `DECISIONES.md` (127–134), README y
  `CLAUDE.md`. `mvn test` en verde (746 tests).
  *Siguiente paso:* los hitos de esta hoja de ruta están terminados. Pendiente: repasar las
  decisiones de `DECISIONES.md` y elegir el siguiente trabajo (ideas en `TODO.md`).
- **2026-10-27 (2).** Hito 16 terminado. Aceptadas R1–R6. Tramos en `model` (`TextRun`,
  `TextFormat`, `DecorationMetrics`) y en `Box`; `LineBreaker` con palabras de varias partes;
  `TextLine` con tramos; pintado de cada tramo, subrayado y tachado con las métricas de
  `post` y `OS/2`; validación por tramo. Un texto sin formato se maqueta igual que antes (los
  PNG golden no cambian). Especificación, `DECISIONES.md` (120–126), README y `CLAUDE.md`.
  `mvn test` en verde (710 tests). El caso golden pasa al hito 17.
  *Siguiente paso:* hito 17, resolver K1–K6 y empezar por la dependencia de commonmark-java y
  el schema de `Markdown`.
- **2026-10-27.** Se borran las hojas de ruta terminadas y se crea esta. Aceptado el enfoque en
  dos hitos (tramos y `Markdown`) y las decisiones T4, M1–M6, el parser (commonmark-java) y
  el subconjunto cerrado. `mvn test` en verde (692 tests).
  *Siguiente paso:* hito 16, resolver R1–R6 y empezar por el modelo de tramos.
