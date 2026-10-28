# Hoja de ruta: Markdown, imágenes, pruebas, ejemplos y mejoras que salieron de ellos

Plan de trabajo para escribir el contenido de un documento en Markdown (un subconjunto cerrado
que se traduce a los elementos que el motor ya conoce), para poner imágenes en él y para
ordenar las pruebas, hacer ejemplos de documentos completos y mejorar el motor con lo que se
echó en falta al escribirlos (hitos 22 a 31). Es el punto de partida de cada sesión: el estado, las decisiones tomadas y lo que queda por hacer están
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
| 21 | Ejemplos de verdad: once documentos completos, con PNG limpios | hecho | `examples/` |
| 22 | Referencias a páginas e índice automático (`Contents`) | hecho | 15 |
| 23 | Enlaces (URL y destinos del documento) | hecho | 16 |
| 24 | Primera página distinta y páginas con nombre | pendiente | 17 (previsto) |
| 25 | Estilo de las tablas (cebra, cabecera, columnas) | hecho | 18 |
| 26 | Formato dentro de la línea fuera de Markdown | pendiente | 19 (previsto) |
| 27 | Texto: mayúsculas automáticas, interletraje y versalitas | pendiente | 20 (previsto) |
| 28 | Aspecto de las cajas: esquinas, bordes, sombras y degradados | pendiente | 21 (previsto) |
| 29 | Texto preformateado y código | pendiente | 22 (previsto) |
| 30 | Rotación y orientación de la página | pendiente | 23 (previsto) |
| 31 | Imágenes vectoriales (SVG) | pendiente | 24 (previsto) |

Los hitos 16 y 17 están terminados (2026-10-27), y del 18 al 23 y el 25, el 2026-10-28. Los hitos
24 y 26 a 31 están pendientes, ordenados por impacto (ver [Hitos 22 a 31](#hitos-22-a-31-lo-que-se-echó-en-falta-al-escribir-los-ejemplos)).

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
      compara el volcado con tolerancia, como el golden. Los PNG se versionan y el PDF no (se
      ignora en git y se regenera en cada ejecución, como el de los golden; corregido el
      2026-10-28, ver el registro).
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
  genera con `-Dexamples.update=true`. El PDF se escribe siempre y no se versiona
  (`.gitignore`); los PNG sí, y solo se reescriben si cambian sus píxeles.
  `-Dexamples.only=NN-nombre` prueba uno solo.
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

## Hitos 22 a 31: lo que se echó en falta al escribir los ejemplos

Al hacer los [once ejemplos](../test/examples/README.md) del hito 21 salieron diez huecos del
motor (antes en `TODO.md`). Cada uno es un hito, **ordenados por el impacto que tienen en
documentos reales** (cuántos ejemplos mejoran y cuánto se nota) y, a igual impacto, por lo poco
que cuestan. El orden se puede cambiar: lo único que no es libre es que **el hito 23 (enlaces)
va después del 22**, porque el índice se vuelve clicable con él.

| Hito | Tema | Impacto | Esfuerzo | Ejemplos que mejoran |
|---|---|---|---|---|
| 22 | Referencias a páginas e índice automático | Alto | Medio | 04, 08, 09 |
| 23 | Enlaces (URL y destinos del documento) | Alto | Medio | 01, 02, 03, 04, 05, 08, 09, 10 |
| 24 | Primera página distinta y páginas con nombre | Alto | Medio-alto | 02, 04, 05, 06, 07, 09 |
| 25 | Estilo de las tablas (cebra, cabecera, columnas) | Medio-alto | Bajo | 01, 04, 05, 07, 08, 09, 11 |
| 26 | Formato dentro de la línea fuera de Markdown | Medio | Bajo-medio | 01, 03, 04, 06, 07, 08 |
| 27 | Texto: mayúsculas automáticas, interletraje y versalitas | Medio | Medio | 01, 03, 04, 05, 06, 07, 08, 09, 10 |
| 28 | Aspecto de las cajas: esquinas, bordes, sombras y degradados | Medio | Alto | 01, 03, 04, 06, 08, 09, 10, 11 |
| 29 | Texto preformateado y código | Medio-bajo | Bajo-medio | 01, 08 |
| 30 | Rotación y orientación de la página | Medio-bajo | Medio | 09, 10, 11 |
| 31 | Imágenes vectoriales (SVG) | Medio | Alto | 01, 02, 04, 06, 07, 08, 09, 10 |

**Por qué este orden.** El índice (22) es lo que se ha pedido y arrastra las referencias a
páginas, que son una pieza reutilizable. Los enlaces (23) están en casi todos los documentos
(correos, webs, el propio índice) y en un PDF se esperan. La primera página distinta (24) es lo
que hace falta en cuanto hay portada o membrete, y hoy se resuelve con un truco. Las tablas
(25) son lo más repetido: siete ejemplos escriben la misma clase en cada celda de cabecera. El
formato en línea (26), las mayúsculas (27) y el aspecto de las cajas (28) son los que más
afinan el resultado visual; el 28 es el más caro, porque toca el pintado en PDF. El texto
preformateado (29) y la rotación (30) mejoran un par de ejemplos cada uno. SVG (31) es el más
caro (probablemente una dependencia nueva) y se puede vivir sin él: los PNG funcionan.

**Cada hito cierra también los ejemplos que mejora.** Además de lo de [Criterios
comunes](#criterios-comunes-definición-de-hecho), al terminarlo:

- [ ] Se reescriben, con lo nuevo, los ejemplos que su sección lista en **«Se aplica en»**; se
      revisan los PNG y se regenera su `expected.stxt` (`-Dexamples.update=true`). Si el
      resultado se ve peor que antes, el hito no está terminado.
- [ ] Se tacha o se borra su línea de [`TODO.md`](../TODO.md) y se actualiza el índice de
      [`test/examples/README.md`](../test/examples/README.md) (y su apartado «Lo que se
      aprende al escribirlos»).
- [ ] Si hace falta una imagen nueva, se añade a `tools/ExampleImages.java`.

---

## Hito 22: referencias a páginas e índice automático

**Objetivo.** Poder escribir en un texto el número de página en que está otra caja, y generar
un índice (tabla de contenido) con los encabezados del documento y sus páginas, que se
mantiene solo al cambiar el contenido. Hoy el contenido del informe anual lleva los números a
mano y la tabla de averías del manual dice «página 2» a mano.

**Qué se echó en falta.**

- **Ejemplo 04 (informe anual):** la página «Contenido» tiene seis filas
  (`Block` + `Flex` + dos `Text`) con el número de página escrito; si el texto crece, hay que
  revisarlas. Con el hito, un solo `Contents`.
- **Ejemplo 08 (manual):** la tabla de averías dice «Comprueba el esquema de la página 2», y
  el pie de la hoja del esquema podría decir «véase la página N» desde la sección de
  instalación.
- **Ejemplo 09 (catálogo):** una página «Índice de productos» (ficha → página) que hoy no
  se puede hacer.

**Sintaxis (propuesta).**

```
Contents:
	Levels: 1 2
	Leader: dots

Text: El esquema de conexión está en la página {page of: wiring}.
```

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-28, todas con la propuesta aceptada salvo X3 y X9; las
interpretaciones que salgan al implementarlas, en [`DECISIONES.md`](DECISIONES.md)):

- [x] X1. **Sintaxis de la referencia:** `{page of: id}` dentro del texto de un `Text`,
      `Paragraph` o `Cell`, como `{page}` y `{pages}`, con el `Id` de una caja.
- [x] X2. **Página de una caja partida:** la de su primer fragmento (donde empieza).
- [x] X3. **Cómo se resuelve: paginando hasta que converja** (*alternativa elegida*, en lugar de
      reservar dígitos y rehacer las líneas como `{pages}`). Así el texto de una referencia
      puede tener longitud variable («página 12 de este manual») y cambiar el reparto en
      líneas y en páginas. Ver X12 para el criterio de parada.
- [x] X4. **Errores nuevos:** `REFERENCE_UNKNOWN` (el `Id` no existe; línea del texto) y
      `REFERENCE_NOT_PLACEABLE` (la caja está en un `Fixed` o es generada y no tiene una única
      página). Nada de dejar referencias en blanco (contra el §6).
- [x] X5. **Índice automático:** elemento `Contents` en flujo, sin hijos, con una entrada por
      encabezado de los niveles de `Levels` (por defecto `1 3`), en orden del documento,
      también los de un `Markdown`. Cada entrada es una caja generada (como el marcador de una
      lista): un `Flex` con el texto del encabezado, la guía y el número.
- [x] X6. **Estilo de las entradas:** reglas `Element: Contents entry 1` … `Contents entry 6`,
      una por nivel, con `Box` y `Typography`; la sangría de cada nivel es su `Padding`
      izquierdo.
- [x] X7. **Puntos guía:** `Leader: dots | rule | none` en `Contents`, entre el título y el
      número, con el color del texto.
- [x] X8. **Texto de la entrada:** el del encabezado, con sus `Text` unidos por un espacio y sin
      formato en línea.
- [x] X9. **Otros números (cláusula 4, figura 2): se incluyen en este hito** (`{number of: id}`),
      porque el contrato (ejemplo 07) cita «el Anexo I» y «la cláusula 4». Qué se numera, en X11.
- [x] X10. **PDF etiquetado:** `TOC` con un `TOCI` por entrada; los marcadores no cambian. El
      enlace al destino llega con el hito 23.
- [x] X11. **Qué se numera** (aceptada con la propuesta; al implementarla salió que los
      encabezados no llevan número en el modelo): los ítems de las listas numeradas, con el
      número que escribe su marcador sin sufijo (`3`, `c`, `IV`). Los encabezados no tienen
      número; «4.2» se escribe con dos referencias (decisión 156 de `DECISIONES.md`).
- [x] X12. **Convergencia:** se repagina mientras alguna página supuesta difiera de la real, con
      un máximo de 10 pasadas que cambian alguna página; si no, `REFERENCE_UNSTABLE` con la línea
      de la referencia que sigue cambiando.

**Tareas:**

- [x] Resolver X1–X10 (2026-10-28).
- [x] Resolver X11 y X12 (2026-10-28).
- [x] Schema (`Contents`, `Levels`, `Leader`) y `Element`; especificación (§2, §4.16, §5.19,
      §6, §8, §9 y glosario; el ejemplo, con el caso golden).
- [x] `{page of: id}` y `{number of: id}`: lectura en el texto; paginar de nuevo hasta que
      converjan (X3, X12); página de la primera aparición de cada `Id` en el `Paginator`.
- [x] `Contents`: entradas generadas en el builder, con los encabezados (también los de
      `Markdown`), y su pintado con guía.
- [x] Errores nuevos, con su fila en el §6 y sus casos en `test/errors/references/`
      (`REFERENCE_UNSTABLE` sin caso: lo prueba `PageReferencesTest`).
- [x] Tests de las piezas puras y de layout (referencia hacia delante y hacia atrás, cambio de
      dígitos, índice que ocupa páginas, encabezado en una página siguiente); caso golden
      `15-contents`.
- [x] Ejemplos 04, 08 y 09 reescritos con lo nuevo (ver arriba).

---

## Hito 23: enlaces

**Objetivo.** Que un texto, una imagen o una caja entera lleve a una URL o a otro sitio del
documento, con anotaciones de enlace en el PDF. Antes del hito, un enlace de Markdown era
`MARKDOWN_UNSUPPORTED` y los correos y las webs de los ejemplos eran texto plano.

**Qué se echó en falta.**

- **Ejemplos 01, 02, 03, 08, 10:** correos y webs (`administracion@tallernogal.example`,
  `escuelaceramica.example/verificar`, el soporte del manual) sin poder pulsarlos.
- **Ejemplo 04 (informe anual):** las entradas del índice del hito 22 y la web de contacto.
- **Ejemplos 05 y 09:** la web del boletín y la del taller en el pie de cada página.
- **Ejemplo 08 (manual):** «consulta la sección Especificaciones técnicas» como enlace interno.

**Sintaxis (propuesta).**

```
Text: soporte@airemas.example
	Link: mailto:soporte@airemas.example

Image: examples/brand/nogal.png
	Link: https://tallernogal.example

Markdown: ... [la web](https://tallernogal.example) ...
```

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-28, todas con la propuesta aceptada; las interpretaciones que
salieron al implementarlas, en [`DECISIONES.md`](DECISIONES.md), 163–173):

- [x] L1. **Dónde se declara.** Una propiedad `Link` en cada elemento de caja con contenido (todos
      salvo `Markdown` y `Contents`), que hace clicable la caja entera, y el enlace de Markdown
      (`[texto](destino)`), que se traduce a un tramo con enlace (`TextFormat.link`).
- [x] L2. **Destinos.** `https://…`, `http://…` y `mailto:…` hacia fuera y `#id` hacia una caja del
      documento (se resuelve al terminar de paginar). Otro esquema o un destino mal escrito es
      `LINK_SCHEME` (decisión 166) y un `#id` que no existe, `LINK_UNKNOWN`.
- [x] L3. **Aspecto.** Un enlace no cambia el aspecto por sí mismo; en Markdown, el tramo sale
      subrayado.
- [x] L4. **Cajas partidas.** Una anotación por fragmento.
- [x] L5. **PDF etiquetado.** Un `Link` con su `OBJR` por enlace; en un `Fixed` o en las filas
      repetidas de una tabla desde la segunda página, el enlace no es contenido (decisión 171).
- [x] L6. **Seguridad.** Ni `javascript:` ni `file:` ni rutas relativas; el motor solo escribe la
      anotación.

**Diseño técnico** (como quedó):

- **Modelo.** `model/Links` (qué destino es válido) y `TextFormat.link`, que no cambia cómo se
  mide ni cómo se pinta el texto. `Style.link` (propiedad del elemento, no se hereda) y
  `ErrorCode.LINK_SCHEME` y `LINK_UNKNOWN`.
- **Construcción.** `BoxTreeBuilder.link` lee y valida el destino; `LinkTargets` comprueba al final
  que cada `#id` existe (también los de los tramos); `MarkdownBuilder` traduce `[texto](destino)` y
  `<destino>`; `ContentsBuilder` pone `Link` a cada entrada, hacia su encabezado.
- **Pintado.** `StackingContext.paintLinks` calcula las zonas (la caja de borde de cada fragmento con
  `Link`; el rectángulo de cada tramo con enlace de cada línea) y llama a `Canvas.link`, que el SVG
  ignora.
- **PDF.** `PdfCanvas.write` crea todas las páginas antes de pintar; `PdfLinks` crea las
  anotaciones (acción `URI` o destino), `PdfDestinations` da la esquina del primer fragmento de cada
  caja con nombre (la misma de los marcadores, que antes solo buscaba encabezados) y
  `PdfStructure.link` cuelga el `Link` con su `OBJR`.

**Tareas:**

- [x] Resolver L1–L6 (2026-10-28).
- [x] Schema (`Link`) y `Style`; Markdown (`[texto](url)` deja de ser `MARKDOWN_UNSUPPORTED`; con
      título o con definición aparte, lo sigue siendo).
- [x] Zonas de enlace por fragmento al pintar; `PdfCanvas` escribe `PDAnnotationLink` (URI y destino con
      página y posición); el índice del hito 22 usa los destinos.
- [x] Especificación (§4.2, §5.4, §5.17, §5.19, §5.20 nueva, §6, §7, §8, §9 y glosario), errores (6
      casos en `test/errors/links/` y uno en `markdown/` que sustituye al del enlace), tests
      (`LinksTest` en `model` y en `tree`, `TextFormatTest`, `MarkdownBuilderTest`,
      `StackingContextTest`, `PdfCanvasTest`) y caso golden `16-links`.
- [x] Ejemplos 01, 02, 03, 04, 05, 08, 09 y 10 con enlaces (en el 04, el 08 y el 09, las entradas
      del índice; en el 08, un enlace de Markdown a la tabla de especificaciones).

---

## Hito 24: primera página distinta y páginas con nombre

**Objetivo.** Decir en qué páginas va un `Fixed` y poder tener una primera página con otro
margen, para portadas y membretes sin trucos.

**Qué se echó en falta.**

- **Ejemplos 04 y 09:** la portada es un `Absolute` con `Layer: 5` del tamaño de la página que
  tapa la cabecera y el pie de los `Fixed`, más un bloque de 1 pt con `Break: After: page` para
  forzar el salto. Con el hito, la portada es un bloque normal y el pie no sale en ella.
- **Ejemplo 02 (carta):** el membrete grande solo debería salir en la primera hoja, y una
  cabecera corta en las siguientes (hoy, si la carta pasa de una página, repite o no hay nada).
- **Ejemplo 05 (boletín):** la cabecera del periódico solo en la portada, y un titulillo corrido
  en la segunda.
- **Ejemplo 06 (menú):** el pie «La Cala · 1 / 2» no pega en la primera página, que es la de
  presentación.
- **Ejemplo 07 (contrato):** el logotipo en la cabecera de todas las hojas menos en la primera,
  que ya lleva el título grande.
- **Ejemplos 04 y 09:** que la portada no cuente en la numeración (`Página 1 de 6` en la
  primera interior).

**Sintaxis (propuesta).**

```
Fixed:
	Pages: not-first
	Offset: -34 0 auto 0
	...

Media:
	Width: 595
	Height: 842
	Margin: 56
	First margin: 0
```

**Decisiones abiertas:**

- [ ] **N1. Qué páginas.** *Propuesta:* una propiedad `Pages` en `Fixed` con `all` (por
      defecto), `first`, `not-first`, `odd`, `even` y `last`. *Alternativa:* páginas con
      nombre como en CSS (`@page :first`), más general pero más pesado.
- [ ] **N2. Margen de la primera página.** *Propuesta:* `First margin` en `Media`, para una
      portada a sangre o un membrete alto; el flujo se maqueta con el área útil de esa página.
      *Alternativa:* no tocarlo y que la portada sea un `Absolute`, como hoy. Es lo más caro
      del hito: el `Paginator` maqueta hoy todas las páginas con el mismo ancho.
- [ ] **N3. Numeración.** *Propuesta:* `Start` en `Media` (o `Count from`) que fija desde qué
      página se numera: `{page}` y `{pages}` no cuentan la portada. *Alternativa:* `{page}`
      sigue siendo la página física.
- [ ] **N4. Lo que va debajo.** *Propuesta:* un `Fixed` con `Pages` distinto no reserva
      espacio, como hoy; el autor sigue dejando sitio con el margen.
- [ ] **N5. Página en blanco para imprimir a doble cara.** *Propuesta:* fuera del hito.
      *Alternativa:* incluirla (`Break: Before: odd-page`).

**Tareas:**

- [ ] Resolver N1–N5.
- [ ] Schema (`Pages`, y `First margin` y `Start` si se aceptan), `Style`, `Media`.
- [ ] `Paginator`: qué `Fixed` pintar en cada página; margen de la primera página si se
      acepta.
- [ ] Especificación, errores, tests y caso golden `17-page-types`.
- [ ] Ejemplos 02, 04, 05, 06, 07 y 09 sin el truco (ver arriba).

---

## Hito 25: estilo de las tablas

**Objetivo.** Dar estilo a la cabecera, al pie, a las filas alternas y a las columnas de una
tabla **sin repetir una clase en cada celda**, también en las tablas que genera `Markdown`.

**Qué se echó en falta.**

- **Ejemplos 01, 04, 05, 07, 08, 09 y 11:** cada celda de cabecera lleva `Class: th`
  (de cuatro a seis por tabla; en el informe anual, cuatro tablas) y cada celda numérica,
  `Class: num`. Es lo más repetido de todos los documentos.
- **Ejemplo 01 (factura):** el pie con los totales exige `Class: total` en las dos celdas de
  cada fila.
- **Ejemplos 04 y 08:** la cabecera de las tablas de Markdown (la inversión del informe, las
  averías del manual) **no se puede estilar**: sale igual que el cuerpo.
- **Ejemplos 01 y 05:** una cebra (filas alternas) hoy obliga a una clase por fila.
- **Ejemplo 11 (horario):** la columna de las horas, con otra alineación.

**Sintaxis (propuesta).**

```
Styles:
	Element: Head cell
		Typography:
			Weight: bold
		Box:
			Background: #3B2A1E
	Element: Even row cell
		Box:
			Background: #F8F3EA
	For column: 3
		Typography:
			Align: right
```

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-28, con la propuesta aceptada salvo B2, que añade además `Align` por
columna; las interpretaciones, en [`DECISIONES.md`](DECISIONES.md), 174–179):

- [x] B1. **Cómo se nombra:** nuevos valores de `Element` en `Styles`, que son una celda en una
      posición: `Head cell`, `Body cell`, `Foot cell`, `Odd row cell` y `Even row cell` (la primera
      fila del cuerpo es la 1; la cabecera y el pie no entran en la cebra).
- [x] B2. **Columnas:** una regla `For column: n` (o `n m`, un rango) en `Styles`, que se aplica a las
      celdas que empiezan en esas columnas, **y además** un `Align` por columna en la tabla. Como
      `Columns` es la lista de pistas y la comparte `Grid`, queda como propiedad nueva de `Table`:
      `Column align: left right right` (una alineación por columna), valor por defecto que
      sobrescribe `For column`.
- [x] B3. **Prioridad:** se mezclan con las reglas de siempre: elemento (`Cell`) < `Column align` <
      posición < columna < clase < id < lo propio de la celda.
- [x] B4. **Markdown:** nada que añadir: lo generado ya lleva `Head` y `Body`, y las reglas nuevas se
      aplican solas. La alineación de la fila de separación (`:--`, `--:`) sigue ganando a todo.
- [x] B5. **Cebra con celdas de varias filas:** la fila de una celda es la de su primera fila.

**Diseño técnico** (como quedó):

- **Posición.** `TableBuilder` calcula, al colocar cada celda, su `CellPosition` (grupo, fila dentro
  del grupo, columna donde empieza y su `Column align`) y se la pasa a `BoxTreeBuilder.element`, que
  la pasa a `StyleRules.apply`.
- **Reglas.** `StyleRules` lee `Element` con un valor de `CellSelector` (5 posiciones) además de los
  elementos, y `For column` (`ColumnRule`: desde, hasta). Al aplicarlas va primero `Element: Cell`,
  luego el `Column align` (con la línea del `Column align`), las reglas de posición, las de columna,
  las de clase y la de `Id`.
- **Errores.** `TABLE_COLUMN_ALIGN` nuevo (seis casos nuevos en el catálogo, con los de `For column`
  mal escrito o repetido, un `Column align` con un valor que no es una alineación y una regla de
  posición con `Break`); `STYLE_UNKNOWN_ELEMENT` nombra ahora también las posiciones.

**Tareas:**

- [x] Resolver B1–B5 (2026-10-28).
- [x] Schema (`For column`, `Column align`), `Element`/`CellSelector`, `StyleRules` (posición y columna) y
      `TableBuilder`.
- [x] Especificación (§4.10, tabla de §4, §5.15, §6, §8 y §9), errores (`TABLE_COLUMN_ALIGN`, seis
      casos en el catálogo), tests (`TableStylesTest`, `SchemaCoverageTest`) y caso golden
      `18-table-styles` (calculado a mano).
- [x] Ejemplos 01, 04, 05, 07, 08, 09 y 11 con una regla en vez de una clase por celda; el 05 estrena la
      cebra (ver `DECISIONES.md`, 179).

---

## Hito 26: formato dentro de la línea fuera de Markdown

**Objetivo.** Poder poner una negrita, una cursiva o un subrayado en un `Text`, un
`Paragraph` o una `Cell` sin envolverlo en un `Markdown`. Revisa la decisión **M6** (el
formato solo se escribe en `Markdown`), que se tomó cuando no había ejemplos reales.

**Qué se echó en falta.**

- **Ejemplo 08 (manual):** la leyenda de la figura era una `List` de `Paragraph` con `**` y
  salieron los asteriscos literales; hubo que reescribirla como `Markdown`.
- **Ejemplo 07 (contrato):** las dos partes («**Lumen Estudio de Diseño, S.L.P.**») son un
  `Block` con un `Markdown` dentro, solo para una negrita.
- **Ejemplos 01, 03, 04 y 06:** un nombre en negrita dentro de una frase («Garantía de **cinco
  años**», el plato con su descripción en cursiva, el premio del currículum) obliga a partir el
  texto en dos `Text` o a usar `Markdown`.

**Decisiones abiertas:**

- [ ] **F1. Mecanismo.** *Propuesta:* una propiedad `Inline: markdown` (o `Format: markdown`)
      en `Text`, `Paragraph` y `Cell`, que interpreta su texto con la misma sintaxis y el
      mismo subconjunto que `Markdown` (**negrita**, *cursiva*, `++subrayado++`, `~~tachado~~`);
      sin ella, el texto sigue siendo literal. *Alternativa:* hijos `Run` con su propio
      `Typography` dentro del texto, más STXT pero más largo de escribir.
- [ ] **F2. Escapes.** *Propuesta:* los de CommonMark (`\*`). *Alternativa:* ninguno, porque
      hay que pedirlo con la propiedad.
- [ ] **F3. Herencia.** *Propuesta:* `Inline` es una propiedad de `Typography` (se hereda), así
      que se declara una vez en `Document` o en una regla de `Styles`. *Alternativa:* solo en
      el elemento.
- [ ] **F4. Qué más entra.** *Propuesta:* los enlaces del hito 23 cuando estén; ni listas ni
      encabezados (eso es `Markdown`). Un párrafo con líneas en blanco sigue siendo un error.
- [ ] **F5. Errores.** *Propuesta:* lo que no está en el subconjunto es
      `MARKDOWN_UNSUPPORTED`, como ahora, con la línea del texto.

**Tareas:**

- [ ] Resolver F1–F5.
- [ ] Schema (`Inline`), `Style`, y reutilizar `MarkdownBuilder` para un texto en línea.
- [ ] Especificación (§4.4, §5.4, §5.17, §8), errores, tests y caso golden `19-inline`.
- [ ] Ejemplos 01, 03, 04, 06, 07 y 08 sin los `Markdown` de relleno (ver arriba).

---

## Hito 27: texto — mayúsculas automáticas, interletraje y versalitas

**Objetivo.** Las tres cosas que dan carácter a un rótulo y que hoy se hacen a mano:
transformar a mayúsculas, separar las letras y las versalitas.

**Qué se echó en falta.**

- **Todos los ejemplos con rótulos** (01, 03, 04, 05, 06, 07, 08, 09, 10): `EMISOR`,
  `CONTACTO`, `INFORME ANUAL · EJERCICIO 2025`, `PARA EMPEZAR`, `REUNIDOS`, `ATENCIÓN`,
  `NOVEDAD`… están **escritos en mayúsculas en el documento** (con sus tildes, que hay que
  acordarse de poner: «ÓN»), y el PDF etiquetado los lee así.
- **Ejemplos 06 y 10 (carta de restaurante y certificado):** los rótulos pequeños en
  mayúsculas pedirían unos puntos de separación entre letras, como en cualquier impreso cuidado.
- **Ejemplo 04 y 09 (portadas):** «INFORME ANUAL» y «CATÁLOGO» con interletraje.

**Sintaxis (propuesta).**

```
Typography:
	Transform: uppercase
	Letter spacing: 1.2
	Small caps: true
```

**Decisiones abiertas:**

- [ ] **Y1. Transformación.** *Propuesta:* `Transform: none | uppercase | lowercase |
      capitalize` en `Typography` (se hereda); el texto del documento y el del PDF etiquetado
      siguen siendo los escritos. *Alternativa:* solo `uppercase`.
- [ ] **Y2. Interletraje.** *Propuesta:* `Letter spacing` en puntos (como las demás medidas)
      que se suma al avance de cada carácter, en el reparto en líneas (la palabra mide más), en
      Knuth-Plass y en `justify`; el último carácter de la línea no lo lleva. *Alternativa:*
      una fracción del `Size`.
- [ ] **Y3. Versalitas.** *Propuesta:* sintetizadas (mayúsculas al 80 % del tamaño); las
      fuentes del repositorio no tienen la característica `smcp`. *Alternativa:* usarla si la
      fuente la tiene, con un error si no.
- [ ] **Y4. Mayúsculas y caracteres especiales.** *Propuesta:* las reglas de `String` de Java
      con la configuración regional del documento (`Language`): la «ß» o la «i» turca.
      *Alternativa:* siempre `Locale.ROOT`.
- [ ] **Y5. PDF.** *Propuesta:* `Tc` por carácter o ajuste de `TJ`, para que el texto
      seleccionable no se parta en letras.

**Tareas:**

- [ ] Resolver Y1–Y5.
- [ ] Schema, `Style`, `LineBreaker`, `InlineLayout`, `PdfMetrics` y `PdfCanvas`.
- [ ] Especificación (§4.4, §5.4, §8), tests y caso golden `20-typography`.
- [ ] Ejemplos con rótulos en minúscula y la transformación en una regla de `Styles`.

---

## Hito 28: aspecto de las cajas — esquinas, bordes, sombras y degradados

**Objetivo.** Lo que hace que un documento deje de parecer «de rectángulos»: esquinas
redondeadas, un color de borde por lado, sombras y degradados. Es el hito más caro, porque
toca el pintado en PDF.

**Qué se echó en falta.**

- **Ejemplo 03 (currículum):** la foto debería ser circular y las barras de nivel,
  redondeadas.
- **Ejemplo 08 (manual):** los avisos y los números sobre la figura (`1`, `2`, `3`, `4`) son
  cuadrados; deberían ser círculos.
- **Ejemplo 09 (catálogo):** las fichas con una sombra suave y la etiqueta «Novedad» con
  esquinas redondeadas.
- **Ejemplos 01 y 11:** las tarjetas de datos y las celdas del horario, con esquinas
  redondeadas.
- **Ejemplo 04 (informe anual):** un degradado oscuro sobre la foto de la portada para dar
  contraste al título.
- **Ejemplos 06 y 10:** el marco doble y los adornos de las esquinas se dibujan con dos o
  tres `Fixed` y cuatro cuadrados; con esquinas y un borde por lado sería una caja.
- **Ejemplos 01, 04, 07 y 08:** un borde de un solo lado con **otro color** que el resto (la
  franja de color a la izquierda de un aviso) hoy solo se consigue con `Border: 0 0 0 3`.

**Sintaxis (propuesta).**

```
Box:
	Radius: 8
	Border: 0 0 0 3
	Border color: #E9A23B #E9A23B #E9A23B #B5651D
	Shadow: 0 2 6 #00000033
	Background: linear(180, #12315A00, #12315A)
```

**Decisiones abiertas:**

- [ ] **A1. `Radius`.** *Propuesta:* en `Box`, de 1 a 4 valores (número o `%`; `50%` hace un
      círculo o una elipse), que redondea el fondo, el borde y el recorte de lo que lleva
      dentro (una `Image`). Es solo pintado: no cambia el layout. *Alternativa:* un solo
      valor.
- [ ] **A2. Color de borde por lado.** *Propuesta:* `Border color` admite de 1 a 4 colores,
      como `Border`. *Alternativa:* propiedades `Border top color`….
- [ ] **A3. Sombras.** *Propuesta:* `Shadow: x y desenfoque color`, una sola y solo exterior,
      como aproximación con capas semitransparentes en PDF (PDFBox no tiene desenfoque).
      *Alternativa:* no hacerlas; son lo más caro y lo que menos aporta.
- [ ] **A4. Degradados.** *Propuesta:* `Background` admite `linear(ángulo, color, color…)`
      (sombreado axial de PDF) y nada más; sin radiales ni repetidos. *Alternativa:* solo
      dos colores.
- [ ] **A5. Transparencia.** *Propuesta:* los colores admiten `#RRGGBBAA` (hoy no), que hace
      falta para las sombras y el degradado hacia transparente. *Alternativa:* una propiedad
      `Opacity`.
- [ ] **A6. SVG de depuración.** *Propuesta:* lo mismo, con `rx` y `linearGradient`;
      las sombras, con un filtro.

**Tareas:**

- [ ] Resolver A1–A6.
- [ ] Schema, `Style`, `Canvas`, `StackingContext`, `PdfCanvas` y `SvgCanvas`.
- [ ] Especificación (§4.3, §5.6, §8), tests (incluido el PNG de un caso golden) y caso
      golden `21-box-looks`.
- [ ] Ejemplos 01, 03, 04, 06, 08, 09, 10 y 11 con lo nuevo (ver arriba).

---

## Hito 29: texto preformateado y código

**Objetivo.** Texto que conserva sus espacios y saltos de línea, y código en Markdown.

**Qué se echó en falta.**

- **Ejemplo 08 (manual):** el bloque de terminal con los bornes (`L   230 V   [ OK ]`) pierde
  las columnas porque los espacios se colapsan; hoy es un `Grid` de doce `Area`.
- **Ejemplo 01 (factura):** el IBAN en monoespaciada, que en Markdown sería `` `ES91 …` ``
  (hoy `MARKDOWN_UNSUPPORTED`).
- **Ejemplo 08:** los valores de las especificaciones técnicas en monoespaciada.

**Sintaxis (propuesta).**

```
Text:
	Typography:
		Font: IBM Plex Mono
		White space: pre
	Content >>
		> Ajustes > Servicio > Bornes
		L   230 V     [ OK ]
```

**Decisiones abiertas:**

- [ ] **W1. Forma.** *Propuesta:* `White space: normal | pre | pre-wrap` en `Typography` (se
      hereda). *Alternativa:* un elemento `Preformatted`, que no admitiría un `Cell` con
      valor.
- [ ] **W2. Líneas más anchas que la caja.** *Propuesta:* con `pre`, sigue siendo
      `TEXT_OVERFLOW`, como una palabra muy ancha; con `pre-wrap`, se parte. *Alternativa:*
      recortar.
- [ ] **W3. Tabuladores.** *Propuesta:* ocho espacios. *Alternativa:* error.
- [ ] **W4. Markdown.** *Propuesta:* el código en línea es un tramo con la fuente de una
      propiedad `Code font` de `Markdown` (como `Bullet`), y un bloque de código, un `Text` con
      `pre` y esa fuente. *Alternativa:* seguir sin código.
- [ ] **W5. Cortes.** *Propuesta:* un bloque preformateado se parte entre líneas, con viudas y
      huérfanas como cualquier texto.

**Tareas:**

- [ ] Resolver W1–W5.
- [ ] Schema, `Style`, `LineBreaker`/`InlineLayout` (líneas de origen) y `MarkdownBuilder`.
- [ ] Especificación (§4.4, §5.4, §5.17, §6), errores, tests y caso golden
      `22-preformatted`.
- [ ] Ejemplos 01 y 08 (ver arriba).

---

## Hito 30: rotación y orientación de la página

**Objetivo.** Girar una caja (un sello, una etiqueta en diagonal, un texto vertical) y poder
declarar la página por nombre y orientación en vez de por sus puntos. Recoge también la idea
de `TODO.md` «tamaños de página estilo A4, Letter + rotación».

**Qué se echó en falta.**

- **Ejemplo 10 (certificado):** el sello debería ir ligeramente girado, y un texto vertical
  en el margen con el número de certificado.
- **Ejemplo 09 (catálogo):** la etiqueta «Novedad» en diagonal en la esquina de la foto.
- **Ejemplos 10 y 11 (apaisados):** hoy `Width: 842` y `Height: 595`; con el hito,
  `Size: A4 landscape`.

**Sintaxis (propuesta).**

```
Image: examples/certificate/seal.png
	Rotate: -8

Media:
	Size: A4
	Orientation: landscape
```

**Decisiones abiertas:**

- [ ] **O1. Qué se gira.** *Propuesta:* `Rotate` (grados, en el sentido de las agujas del
      reloj) en `Relative`, `Absolute`, `Fixed`, `Image` y `Text`, alrededor del centro de su
      caja de borde; es solo pintado, como `Relative`: el hueco no cambia, y lo que se sale de
      su sitio sigue la política de desbordamientos. *Alternativa:* también cambiar el layout
      (la caja girada ocupa su rectángulo envolvente).
- [ ] **O2. Ángulos.** *Propuesta:* cualquier número entre −360 y 360; con 90, 180 y 270 el
      texto es nítido en el PDF. *Alternativa:* solo múltiplos de 90.
- [ ] **O3. Origen.** *Propuesta:* el centro; una propiedad `Rotate origin` si hace falta.
- [ ] **O4. Páginas con nombre.** *Propuesta:* `Size` con `A3`, `A4`, `A5`, `Letter`, `Legal`…
      y `Orientation: portrait | landscape` en `Media`; `Width` y `Height` siguen
      valiendo, y declarar las dos cosas es un error. *Alternativa:* dejarlo fuera del hito.
- [ ] **O5. Accesibilidad.** *Propuesta:* el texto girado se etiqueta igual; la rotación no
      cambia el orden de lectura.

**Tareas:**

- [ ] Resolver O1–O5.
- [ ] Schema, `Style`, `Media`, `StackingContext`, `Canvas`/`PdfCanvas`/`SvgCanvas`.
- [ ] Especificación (§4.1, §4.9, §5.6, §8), tests y caso golden `23-rotation`.
- [ ] Ejemplos 09, 10 y 11 (ver arriba).

---

## Hito 31: imágenes vectoriales (SVG)

**Objetivo.** Poder poner un SVG en un `Image`, nítido a cualquier tamaño y con texto
seleccionable si se puede. Hoy los logotipos y los gráficos de los ejemplos son PNG generados.

**Qué se echó en falta.**

- **Ejemplo 04 (informe anual):** los tres gráficos (barras, anillo y línea) son PNG de
  1400 px; en vectorial pesarían menos y se verían bien al ampliar.
- **Ejemplos 01, 02, 07, 09 y 10:** los logotipos (`nogal.png`, `lumen.png`, el sello y las
  firmas).
- **Ejemplos 06 y 08:** el emblema y el ornamento del menú, el termostato y el esquema de
  conexión.
- **`tools/ExampleImages.java`** generaría SVG en vez de dibujar píxeles.

**Decisiones abiertas:**

- [ ] **V1. Cómo se incrusta.** *Propuesta:* convertir el SVG a gráficos vectoriales del PDF
      (rutas y rellenos), con una librería pequeña que dibuje a `Graphics2D` y un adaptador a
      PDFBox. *Alternativa:* rasterizar a la resolución del fichero al incrustar (más sencillo,
      pero no es vectorial) o pasar por Apache Batik (completa pero enorme).
- [ ] **V2. Librería.** *Propuesta:* una dependencia nueva, la más pequeña que cubra el
      subconjunto de V3; es lo que más decide el coste del hito. *Alternativa:* escribir un
      intérprete del subconjunto.
- [ ] **V3. Subconjunto.** *Propuesta:* formas básicas, rutas, grupos, transformaciones, rellenos
      sólidos y degradados, trazos y `viewBox`; sin scripts, sin animación, sin filtros.
      *Alternativa:* lo que dé la librería.
- [ ] **V4. Texto dentro del SVG.** *Propuesta:* solo con las fuentes de `resources/fonts/`
      (misma política que el resto: sin sustitución), o error. *Alternativa:* convertirlo a
      rutas al exportar.
- [ ] **V5. Tamaño natural.** *Propuesta:* el de `width` y `height` (o el `viewBox`) a 96 ppp,
      como los PNG (§5.18). *Alternativa:* exigirlos.
- [ ] **V6. Seguridad.** *Propuesta:* ninguna referencia externa (`href` a ficheros o URL,
      `<image>` externas, entidades): son un error (`IMAGE_FORMAT`).

**Tareas:**

- [ ] Resolver V1–V6.
- [ ] `ImageLibrary` e `ImageFile` con el formato SVG, tamaño natural y validación.
- [ ] `PdfCanvas` y `SvgCanvas`; `PdfStructure`, igual que las demás imágenes.
- [ ] Especificación (§5.18, §6, §8), errores, tests y caso golden `24-svg`.
- [ ] `tools/ExampleImages.java` y los ejemplos 01, 02, 04, 06, 07, 08, 09 y 10.

---

## Registro de sesiones

Una entrada por sesión, la más reciente arriba: fecha, qué se hizo, dónde se quedó y el
siguiente paso concreto.

- **2026-10-28 (9).** Hito 25 terminado, antes que el 24: era el siguiente paso del registro (el de menor
  esfuerzo). Resueltas B1–B5, con una ampliación en B2 (`Align` por columna además de
  `For column`, como `Column align` en la tabla). Reglas `Element` por posición, `For column` y
  `Column align`, con `CellPosition` desde `TableBuilder`; `TABLE_COLUMN_ALIGN` y seis casos nuevos
  en el catálogo; `TableStylesTest`; caso golden `18-table-styles`, calculado a mano y que pasó a la
  primera. Ejemplos 01, 04, 05, 07, 08, 09 y 11 reescritos: los PNG no cambian salvo en las tablas de
  Markdown de 04 y 08 (ahora con la cabecera estilada), la esquina del horario (11) y la cebra del
  boletín (05). Decisiones 174–179 de `DECISIONES.md`. `mvn test` en verde.
  *Siguiente paso:* hito 24 (primera página distinta, esfuerzo medio-alto), en otra sesión; resolver
  N1–N5 con el usuario antes de escribir código. El pie de totales sigue necesitando una clase.
- **2026-10-28 (8).** Hito 23 terminado. Resueltas L1–L6 con las propuestas. `Link` en las cajas y
  `[texto](destino)` en Markdown, a una URL (`https`, `http`, `mailto`) o a otra caja (`#id`), con
  anotaciones del PDF (una por fragmento o por tramo y línea), destinos con página y posición, y
  `Link` con `OBJR` en el PDF etiquetado; las entradas de los índices llevan a sus encabezados.
  `LINK_SCHEME` y `LINK_UNKNOWN`, con seis casos en `test/errors/links/`; el caso del enlace de
  Markdown del catálogo pasa a ser el del título de un enlace. Decisiones 163–173 de
  `DECISIONES.md`. Caso golden `16-links`; ejemplos 01–05, 08, 09 y 10 con enlaces (los volcados no
  cambian: el único PNG que cambia es la página 2 del manual, con su enlace de Markdown
  subrayado). `mvn test` en verde.
  *Siguiente paso:* hito 25 (estilo de las tablas, de esfuerzo bajo), en otra sesión; resolver
  B1–B5 con el usuario antes de escribir código. *Deuda anotada:* el `Link` del PDF etiquetado no
  envuelve el contenido marcado de su texto (decisión 171); la accesibilidad es básica, así que está
  en la sección «Accesibilidad» de [`TODO.md`](../TODO.md) para no perderla.
- **2026-10-28 (7).** Hito 22 terminado, en cuatro commits: `{page of: id}` con paginación hasta
  converger (X1–X4, X12), `{number of: id}` (X9, X11), `Contents` (X5–X8, X10) y el caso golden
  `15-contents` con los ejemplos. Resueltas X11 y X12 con las propuestas; al implementar X11 salió
  que los encabezados no llevan número en el modelo (solo los ítems de listas numeradas), y al
  reescribir los ejemplos 04 y 09 que un `Contents` lista todos los encabezados, así que la
  portada y el título del índice son un `Text` (ambas cosas, en `TODO.md`). Decisiones 151–162 de
  `DECISIONES.md`. Ejemplo 04: el índice son seis entradas generadas (las páginas coinciden con las
  que estaban escritas a mano); ejemplo 08: la tabla de averías usa `{page of: wiring}`; ejemplo 09:
  una página nueva de índice de productos (ahora 5 páginas). `mvn test` en verde.
  *Siguiente paso:* hito 23 (enlaces), que vuelve clicable el índice; resolver L1–L6 con el usuario.
- **2026-10-28 (6).** Hito 22: resueltas X1–X10 (solo documentación, sin código). Se aceptan
  todas las propuestas salvo X3 (se pagina hasta que converja, en vez de reservar dígitos) y
  X9 (`{number of: id}` entra en el hito). Quedan X11 (qué se numera) y X12 (criterio de
  convergencia y error si oscila), que salen de esos dos cambios. `mvn test` en verde (809
  tests).
  *Siguiente paso:* hito 22, empezar por `{page of: id}` con paginación hasta converger (X1–X4,
  X12).
- **2026-10-28 (5).** Se abren los hitos 22 a 31 (solo la hoja de ruta, sin código): el 22 es el
  índice, pedido por el usuario, y los nueve siguientes son los huecos que salieron al escribir
  los ejemplos, ordenados por impacto, con el ejemplo donde se echó en falta cada uno. Las
  decisiones de cada hito están abiertas, con su propuesta, y se resuelven con el usuario antes
  de escribir código. `TODO.md`, el README y `CLAUDE.md` no se han tocado: al empezar el hito 22
  hay que actualizar la tabla de hitos de los dos últimos.
  *Siguiente paso:* hito 22, resolver X1–X10 y empezar por la referencia `{page of: id}`.
- **2026-10-28 (4).** Los PDF de los ejemplos dejan de versionarse (`git rm --cached`,
  `.gitignore`: `test/examples/**/*.pdf`) y `ExamplesTest` los escribe siempre; los PNG siguen
  versionados. Decisión 147 corregida.
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
