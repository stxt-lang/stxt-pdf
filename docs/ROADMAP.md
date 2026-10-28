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
| 24 | Primera página distinta (`Pages` en `Fixed`) | hecho | 17 |
| 25 | Estilo de las tablas (cebra, cabecera, columnas) | hecho | 18 |
| 26 | Formato dentro de la línea fuera de Markdown | hecho | 19 |
| 27 | Texto: mayúsculas automáticas, interletraje y versalitas | hecho | 20 |
| 28 | Aspecto de las cajas: esquinas, color de borde por lado, degradados y opacidad (sin sombras) | hecho | 21 |
| 29 | Texto preformateado y código | hecho | 22 |
| 30 | Rotación y orientación de la página | hecho | 23 |
| 31 | Imágenes vectoriales (SVG) | hecho | 24 |
| 32 | Secciones (`Section`): `Media`, `Fixed` y numeración propios | hecho | 25 |

Los hitos 16 y 17 están terminados (2026-10-27), y del 18 al 32, el 2026-10-28 (varios, en otras
sesiones del mismo día). Los hitos se ordenaron por impacto (ver [Hitos 22 a 31](#hitos-22-a-31-lo-que-se-echó-en-falta-al-escribir-los-ejemplos)).

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
      adelante se quiere una sintaxis STXT propia. *Revisada en el hito 26: ver F1.*

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
margen, para portadas y membretes sin trucos. *Resultado: se hizo lo primero; el margen de la primera
página y la numeración sin portada se dejaron fuera (N2 y N3, abajo), así que las portadas a sangre
siguen siendo un `Absolute` y «Página 1 de 6» en la primera interior no se puede escribir.*

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

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-28, con la propuesta aceptada salvo N2 y N3, donde se eligió la
opción barata; las interpretaciones, en [`DECISIONES.md`](DECISIONES.md), 180–185):

- [x] N1. **Qué páginas:** una propiedad `Pages` en `Fixed` con `all` (por defecto), `first`, `not-first`,
      `odd`, `even` y `last`.
- [x] N2. **Margen de la primera página:** *no se toca.* Sin `First margin`: la portada a sangre sigue
      siendo un `Absolute`. Se pierde el hueco de la cabecera en la primera hoja de un contrato.
- [x] N3. **Numeración:** *la página física.* Sin `Start` en `Media`; `{page}` y `{pages}` cuentan la
      portada.
- [x] N4. **Lo que va debajo:** un `Fixed` con `Pages` limitado no reserva espacio, como hoy.
- [x] N5. **Página en blanco para doble cara:** fuera del hito (`TODO.md`).

Como se renunció a N2 y a N3, el objetivo del hito se redujo a decir en qué páginas va un `Fixed`; el
título conserva «páginas con nombre» solo en la tabla de impacto de más arriba.

**Diseño técnico** (como quedó):

- **Modelo.** `Style.pages` (`Style.Pages`, con `includes(página, total)`), leído como `Layer` en el
  elemento (`BoxTreeBuilder`); el schema lo admite solo en `Fixed`.
- **Paginación.** El `Paginator` ya colocaba cada `Fixed` en cada página al terminar de paginar el
  flujo; ahora salta los que `Pages` no incluye en esa página (y los que cuelgan de ellos). Nada más
  cambia: `Page.fixed` solo tiene lo que sale, y el volcado, el pintado y el PDF lo recorren como siempre.
- **Errores.** Ninguno nuevo: un valor que no es de la lista es `INVALID_VALUE` del schema y `Pages` fuera
  de un `Fixed`, `CHILD_NOT_DECLARED` (dos casos en `test/errors/schema/`).

**Tareas:**

- [x] Resolver N1–N5 (2026-10-28).
- [x] Schema (`Pages`), `Style` y `BoxTreeBuilder`; `Paginator`.
- [x] Especificación (§2, §4.9, §5.5, §5.10, §8 y §9), errores (dos casos), tests (`FixedPagesTest`,
      `SchemaCoverageTest`) y caso golden `17-page-types`, calculado a mano.
- [x] Ejemplos 04, 05, 06, 07 y 09 con `Pages` en vez del truco o la excepción (el 02 es de una página y no
      cambia; ver `DECISIONES.md`, 185).

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

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-28, todas con la propuesta aceptada, elegidas en un cuestionario con
el usuario; las interpretaciones, en [`DECISIONES.md`](DECISIONES.md), 186–192):

- [x] F1. **Mecanismo.** Una propiedad `Inline: markdown` (con `none` por defecto) en `Text`,
      `Paragraph` y `Cell`, que interpreta su texto con la misma sintaxis y el mismo subconjunto que
      `Markdown` (**negrita**, *cursiva*, `++subrayado++`, `~~tachado~~`); sin ella, el texto sigue
      siendo literal.
- [x] F2. **Escapes.** Los de CommonMark (`\*`).
- [x] F3. **Herencia.** `Inline` es una propiedad de `Typography` (se hereda): se declara una vez en
      `Document` o en una regla de `Styles`, y `Inline: none` vuelve al literal.
- [x] F4. **Qué más entra.** Los enlaces `[texto](destino)` del hito 23; ni listas ni encabezados (eso
      es `Markdown`). Un texto con varios párrafos (línea en blanco) los une como un `Text`.
- [x] F5. **Errores.** Lo que no está en el subconjunto es `MARKDOWN_UNSUPPORTED`, con la línea del
      párrafo y un mensaje propio de este modo.

**Diseño técnico** (como quedó):

- **Propiedad.** `Style.Inline` (`NONE`, `MARKDOWN`), campo heredable de `Style`; el schema la tiene en
  el grupo `Typography` y `PropertyReader.typography` la lee, así que vale también en `Styles`.
- **Traducción.** `MarkdownBuilder.inline(texto, primeraLínea)` parsea con el mismo `PARSER` y el
  mismo `Translator` (ahora con `inlineOnly`, sin las propiedades de `Markdown`): cada párrafo da sus
  tramos y se unen con `\n\n`.
- **Árbol.** `BoxTreeBuilder.inlineFormat`, después de leer el texto de un `Text`/`Paragraph` o el valor
  de una `Cell`: `Box.text` es el texto sin marcas, `Box.runs` los tramos (`null` sin formato) y
  `Box.textBlock` es falso, con `Box.textLine` la línea donde empieza el texto. No actúa dentro de un
  `Markdown` (`BuildContext.insideMarkdown`).
- **Layout y pintado.** Nada nuevo: los tramos son los del hito 16 y pasan por `LineBreaker`,
  `PdfMetrics` y `StackingContext` como los de `Markdown`.

**Tareas:**

- [x] Resolver F1–F5 (2026-10-28).
- [x] Schema (`Inline`), `Style`, `PropertyReader` y reutilizar `MarkdownBuilder` para un texto en línea.
- [x] Especificación (§4.4, §5.4, §5.17, §6, §8, §9 y glosario), errores (seis casos nuevos de
      `MARKDOWN_UNSUPPORTED` en `test/errors/markdown/`), tests (`InlineFormatTest`,
      `SchemaCoverageTest`) y caso golden `19-inline`, calculado a mano.
- [x] Ejemplos 01, 03, 04, 06, 07 y 08 (ver arriba): 04, 07 y 08 sin los `Markdown` de relleno y sin
      cambiar un píxel; 01, 03 y 06 con una negrita o una cursiva dentro de la frase.

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

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-28, con un cuestionario al usuario; las interpretaciones, en
[`DECISIONES.md`](DECISIONES.md)):

- [x] Y1. **Transformación.** `Transform: none | uppercase | lowercase | capitalize` en `Typography`
      (se hereda); el texto del documento y el del PDF etiquetado siguen siendo los escritos.
- [x] Y2. **Interletraje.** `Letter spacing` en puntos, sumado al avance de cada carácter, en el reparto
      en líneas (la palabra mide más), en Knuth-Plass y en `justify`; el último carácter de la línea no
      lo lleva.
- [x] Y3. **Versalitas.** Sintetizadas: las minúsculas pasan a mayúsculas al 80 % del tamaño (las
      fuentes del repositorio no tienen `smcp`).
- [x] Y4. **Idioma.** Las reglas de `String` de Java con la configuración regional de `Language`. El
      usuario añadió: si el idioma es una característica de accesibilidad, que sea **obligatorio en el
      schema**. Lo es (PDF/UA exige el `Lang`), así que `Language` pasó a ser obligatorio en `Document`
      antes de empezar el hito (`DECISIONES.md`, 193).
- [x] Y5. **PDF** *(detalle técnico, sin consultar)*. Ajustes de `TJ` entre glifos, como `justify`
      (las fuentes son Type 0 Identity-H), para que el texto seleccionable no se parta en letras.

**Diseño técnico** (como quedó):

- **Propiedades.** `Style.transform` (`Style.Transform`), `Style.letterSpacing` (puntos) y
  `Style.smallCaps`, heredables y en el grupo `Typography` del schema, así que valen en `Styles`.
  `Style.locale` es la configuración regional de `Language` (obligatorio, ver abajo); no es una
  propiedad de la caja: el builder la pone en la raíz y se hereda.
- **Transformación.** `layout/inline/TextTransform` pasa los tramos de la caja por `Transform` y
  luego por las versalitas (las minúsculas pasan a mayúsculas con `TextFormat.smallCaps`), sin tocar
  los marcadores `{page}`/`{pages}`. `InlineLayout.runs` lo aplica después de sustituir las
  referencias y antes de repartir en líneas.
- **Medida.** `LineBreaker.advance` da el avance de cada tramo (`fontSize × format.scale()` más el
  interletraje de cada carácter) y `LineBreaker.width`, el ancho como línea (sin el del último);
  los usan el voraz, Knuth-Plass, los tamaños intrínsecos y `place`.
- **Pintado.** `StackingContext` pasa a `Canvas.text` el tamaño del tramo y el interletraje;
  `PdfCanvas.spaced` los escribe con `TJ` y `SvgCanvas`, con `letter-spacing`. `PdfStructure` da
  `ActualText` (el texto escrito) a lo que tiene `Transform` o `Small caps` y no lleva números de
  página ni referencias.
- **`Language` obligatorio.** Se hizo antes del hito (decisión 193): era la condición de Y4.

**Tareas:**

- [x] Resolver Y1–Y5 (2026-10-28).
- [x] `Language` obligatorio en `Document`, con todos los documentos de prueba y de ejemplo (Y4).
- [x] Schema (`Transform`, `Letter spacing`, `Small caps`), `Style`, `PropertyReader`.
- [x] `TextTransform`, `LineBreaker` (avance con interletraje y versalitas), `InlineLayout`.
- [x] `Canvas`, `StackingContext`, `PdfCanvas`, `SvgCanvas` y `PdfStructure` (`ActualText`).
- [x] Especificación (§2, §4.1, §4.4, §5.4, §5.13, §6, §8, §9 y glosario), cuatro casos en el catálogo
      de errores, tests (`TextTransformTest`, `TextTypographyTest`, `TextCaseTest`, `LineBreakerTest`,
      `StackingContextTest`, `SvgCanvasTest`, `PdfCanvasTest`) y caso golden `20-typography`.
- [x] Ejemplos 01, 03, 04, 05, 06, 07, 08, 09 y 10 con los rótulos en minúscula y la transformación
      en una regla de `Styles` (decisión 202).

---

## Hito 28: aspecto de las cajas — esquinas, bordes, degradados y opacidad

**Objetivo.** Lo que hace que un documento deje de parecer «de rectángulos»: esquinas
redondeadas, un color de borde por lado y degradados. Es el hito más caro, porque toca el
pintado en PDF. *Resultado: se hizo todo menos las sombras (A3).*

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

**Sintaxis.**

```
Box:
	Radius: 8
	Border: 0 0 0 3
	Border color: #E9A23B #E9A23B #E9A23B #B5651D
	Background: linear(180, #12315A00, #12315A)
```

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-28, con un cuestionario; A1, A2 y A4 con la propuesta, A3 con la
alternativa y A5 y A6 por defecto; las interpretaciones, en [`DECISIONES.md`](DECISIONES.md),
216–225):

- [x] A1. **`Radius`** en `Box`, de 1 a 4 valores (número o %; `50%` hace un círculo o una elipse), que
      redondea el fondo, el borde y el recorte de una `Image`. Solo pintado.
- [x] A2. **Color de borde por lado:** `Border color` admite de 1 a 4 colores, como `Border`.
- [x] A3. **Sombras:** *no se hacen* (la alternativa): son lo más caro y lo que menos aporta. Quedan en
      `TODO.md`.
- [x] A4. **Degradados:** `Background` admite `linear(ángulo, color, color…)` (sombreado axial de PDF) y
      nada más; sin radiales ni repetidos.
- [x] A5. **Transparencia:** los colores admiten `#RGBA` y `#RRGGBBAA`, que hacen falta para el degradado
      hacia transparente (y valen también para el texto).
- [x] A6. **SVG de depuración:** lo mismo, con `<path>`, `linearGradient` y `clipPath`.

**Diseño técnico** (como quedó):

- **Modelo.** `model/Colors` (colores y su opacidad), `model/Corners` y `model/Radii` (el abreviado por
  esquina y los radios resueltos: porcentajes, reducción si no caben, `inset` hacia dentro y
  `forFragment` para cajas partidas) y `model/Paint` (`Solid` o `Linear`, con la línea del degradado).
  `Style.background` es un `Paint`, `Style.borderColor` un `Sides<String>` y `Style.radius` un `Corners<Value>`.
- **Lectura.** `ValueReader.paint`, `radius` y `colorSides`; el schema tiene `Radius` en `Box`.
  Sin códigos de error nuevos: todo es `VALUE_SYNTAX` o `VALUE_NOT_ALLOWED`, con nueve casos en
  `test/errors/values/`.
- **Pintado.** `StackingContext` resuelve los radios del fragmento y llama a `Canvas.fill`, `border` e
  `image`, que reciben los radios. `paint/Shapes` da los caminos (cuartos de elipse como Bézier y el
  trapecio de cada lado del borde, con las diagonales hacia dentro cuando la curva de dentro es redonda).
  `PdfCanvas` rellena el anillo del borde con la regla par-impar (recortado a cada trapecio con colores
  distintos), el degradado es un sombreado axial recortado (con una máscara suave de luminosidad si hay
  opacidad) y la opacidad de un color, un estado gráfico `ca`. `SvgCanvas` escribe lo mismo con `<path>`,
  `linearGradient` y `clipPath`.
- **Sin cambios en los PNG existentes.** Un borde liso, opaco y cuadrado se pinta como antes, con cuatro
  rectángulos (decisión 223).

**Tareas:**

- [x] Resolver A1–A6 (2026-10-28).
- [x] Schema (`Radius`, descripciones de `Border color` y `Background`), `Style`, `ValueReader`.
- [x] `Canvas`, `StackingContext`, `Shapes`, `PdfCanvas` y `SvgCanvas`.
- [x] Especificación (§3.3, §3.4, §4.3, §5.6, §6, §8, §9 y glosario), `DECISIONES.md` (216–225).
- [x] Tests: `ColorsTest`, `RadiiTest`, `PaintTest`, `BoxLooksTest`, `ShapesTest`, `StackingContextTest`,
      `BoxLooksPaintTest` (píxeles del PDF renderizado y texto del SVG), nueve casos de error y el caso
      golden `21-box-looks` (calculado a mano; pasó a la primera).
- [x] Ejemplos 01, 03, 04, 06, 08, 09, 10 y 11 con lo nuevo (decisión 225); `test/examples/README.md`,
      `TODO.md`, README y `CLAUDE.md`.

---

## Hito 29: texto preformateado y código

**Objetivo.** Texto que conserva sus espacios y saltos de línea, y código en Markdown.

**Qué se echó en falta.**

- **Ejemplo 08 (manual):** el bloque de terminal con los bornes (`L   230 V   [ OK ]`) pierde
  las columnas porque los espacios se colapsan; hoy es un `Grid` de doce `Area`.
- **Ejemplo 01 (factura):** el IBAN en monoespaciada, que en Markdown sería `` `ES91 …` ``
  (hoy `MARKDOWN_UNSUPPORTED`).
- **Ejemplo 08:** los valores de las especificaciones técnicas en monoespaciada. *No cambian:* ya
  lo están con `Class: mono` en cada celda de una `Table` normal, y la tabla de Markdown no puede
  tener las columnas de ancho fijo (`Table columns` solo admite `equal` y `auto`).

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

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-28, todas con la propuesta aceptada salvo W3, que el usuario
amplió; las interpretaciones que salieron al implementarlas, en [`DECISIONES.md`](DECISIONES.md),
203–214):

- [x] W1. **Forma:** `White space: normal | pre | pre-wrap` en `Typography` (se hereda y vale en
      `Styles`), no un elemento `Preformatted`.
- [x] W2. **Líneas más anchas que la caja:** con `pre`, `TEXT_OVERFLOW` (con un mensaje que
      sugiere `pre-wrap`); con `pre-wrap`, se parten entre palabras.
- [x] W3. **Tabuladores:** *no* ocho espacios ni un error: una propiedad `Tab size`, en
      `Typography`, con las columnas de una tabulación y **4 por defecto** (el valor de los ficheros
      STXT). Interpretación: cada tabulación va a la siguiente columna múltiplo de `Tab size`
      (decisión 208).
- [x] W4. **Markdown:** una propiedad `Code font` en `Markdown` (como `Bullet`, obligatoria solo si hay
      código): el código en línea es un tramo con esa fuente y un bloque de código, un `Text` con
      `pre` y esa fuente.
- [x] W5. **Cortes:** un bloque preformateado se parte entre líneas, con viudas y huérfanas como
      cualquier texto (todo el texto es un solo párrafo).

**Diseño técnico** (como quedó):

- **Propiedades.** `Style.WhiteSpace` y `Style.tabSize`, leídas en `PropertyReader.typography` y heredadas.
- **Reparto.** `layout/inline/PreformattedText`: las líneas de origen con las tabulaciones expandidas
  (`sourceLines`), cada una como una línea de la caja (`pre`) o partida voraz entre palabras
  (`pre-wrap`), y los tamaños intrínsecos. `InlineLayout` lo usa en lugar de `LineBreaker.breakParagraphs`
  y no justifica. `LineBreaker.place` admite una línea sin tramos.
- **Errores.** Ninguno nuevo: el mensaje de `TEXT_OVERFLOW` de `pre` es propio (`Overflow`); un `Tab size`
  menor que 1 es `VALUE_NOT_ALLOWED`. `SvgCanvas` conserva los espacios con `xml:space`.
- **Código en Markdown.** `TextFormat.family` (familia del tramo, `null` = la de la caja), que
  `PdfMetrics` comprueba (`FONT_UNKNOWN`); `MarkdownBuilder.codeBlock` y el tramo de `Code`.

**Tareas:**

- [x] Resolver W1–W5.
- [x] Schema (`White space`, `Tab size`, `Code font`), `Style`, `PropertyReader` y `BoxTreeBuilder`.
- [x] `PreformattedText`, `InlineLayout`, `Overflow`, `SvgCanvas`.
- [x] `TextFormat.family`, `MarkdownBuilder` (código en línea y bloques) y `PdfMetrics`.
- [x] Especificación (§4.4, §4.14, §5.4, §5.12, §5.17, §6, §8, §9 y glosario), errores (4 casos para
      `White space` y `Tab size`, 4 para el código), tests (`PreformattedTextTest`, `TextFormatTest`,
      `MarkdownBuilderTest`, `MarkdownTest`, `PdfMetricsTest`, `SchemaCoverageTest`) y caso golden
      `22-preformatted`, calculado a mano.
- [x] Ejemplos 01 (el IBAN y el BIC, en un `Markdown` con `Code font`) y 08 (el terminal es un `Text`
      con `White space: pre` en lugar de un `Grid` de doce `Area`, y el borne `N` en código en línea).
- [x] README, `CLAUDE.md`, `DECISIONES.md` (203–214) y este documento.

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
  `Paper: A4` y `Orientation: landscape`.

**Sintaxis (propuesta).**

```
Image: examples/certificate/seal.png
	Rotate: -8
	Rotate origin: bottom right

Media:
	Paper: A4
	Orientation: landscape
```

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-28, con un cuestionario; con la propuesta salvo O1, donde se
quitó `Text` y se pide que lo girado quepa en la página, y O3, donde entra ya `Rotate origin`):

- [x] **O1. Qué se gira.** `Rotate` (grados, en el sentido de las agujas del reloj) es una
      propiedad del elemento, como `Offset`, en `Relative`, `Absolute`, `Fixed` e `Image` (no en
      `Text`: un texto girado va dentro de uno de ellos; ni en `Styles`). Es solo pintado: la caja
      ocupa su sitio sin girar y los hermanos no se mueven, **pero lo girado tiene que caber en la
      página**: el rectángulo que envuelve la caja de borde girada (con los giros de sus
      ancestros) que se sale de la página es un `BOX_OVERFLOW`. Un `Relative` girado no se parte
      entre páginas (como una `Image`); `Inside` y `Decoration` en él son un error.
- [x] **O2. Ángulos.** Cualquier número entre −360 y 360 (con decimales); fuera, o `0`, que no
      tendría efecto, `VALUE_NOT_ALLOWED`. El texto sigue siendo texto en el PDF.
- [x] **O3. Origen.** El centro de la caja de borde por defecto, y una propiedad
      `Rotate origin` con una o dos palabras clave (`top`, `bottom`, `left`, `right`, `center`:
      `top left`, `bottom`…). Sin `Rotate`, es un error (no tiene efecto).
- [x] **O4. Páginas con nombre.** `Size` ya es el tamaño de la fuente y un nombre tiene una
      sola definición, así que se llama **`Paper`**: `A3`, `A4`, `A5`, `A6`, `B4`, `B5`, `Letter`
      y `Legal`, con `Orientation: portrait | landscape` (por defecto `portrait`) en `Media`.
      `Width` y `Height` siguen valiendo; declarar las dos cosas, o `Orientation` sin `Paper`, es
      un error.
- [x] **O5. Accesibilidad y enlaces.** El texto girado se etiqueta igual y la rotación no cambia
      el orden de lectura. Un enlace en una caja girada se anota con el polígono girado
      (`QuadPoints`) y su rectángulo envolvente para los visores que no lo entienden.

**Tareas:**

- [x] Resolver O1–O5 (2026-10-28).
- [x] Schema, `Style`, `Media`, `StackingContext`, `Canvas`/`PdfCanvas`/`SvgCanvas`; además
      `model/Rotation`, `Engine.layout` (un `Relative` girado no se parte), `Overflow` (lo girado, contra
      la página) y `PdfDestinations`.
- [x] Especificación (§4.1, §4.9, §4.15, §5.6, §5.10, §5.12, §6, §7, §8, §9 y glosario), errores (18 casos
      nuevos o movidos en el catálogo: `MEDIA_SIZE`, `ROTATE_ORIGIN_WITHOUT_ROTATE`, `BREAK_IN_ROTATED`,
      `VALUE_NOT_ALLOWED`, `BOX_OVERFLOW`, `PAGE_OVERFLOW` y del schema), tests (`RotationTest`,
      `RotationLayoutTest`, `BoxTreeBuilderTest`, `StackingContextTest`, `SvgCanvasTest`, `PdfCanvasTest`) y
      caso golden `23-rotation`, calculado a mano.
- [x] Ejemplos 09 (etiqueta «Novedad» en diagonal), 10 (sello girado y número en vertical en el margen) y
      11 (`Paper` y `Orientation`).

**Diseño técnico** (como quedó):

- **Modelo.** `model/Rotation` (grados y origen como fracción de la caja de borde; `transform`,
  `corners` y `envelope` con `AffineTransform`, en coordenadas de la página con la y hacia abajo);
  `Style.rotation`, leído en `BoxTreeBuilder.element` (`ValueReader.angle` y `rotateOrigin`) y validado
  en `ModelValidator.validateRotation`. `Media.Paper` y `Media.Orientation`, en `BoxTreeBuilder.media`.
- **Layout.** Nada cambia salvo que `Engine.layout` maqueta sin fragmentación una caja girada y devuelve
  `BEFORE` si no cabe (`ConstraintSpace.unfragmented`).
- **Desbordamiento.** `Overflow.fragment` compone los giros de los ancestros y comprueba el envolvente
  de lo girado (y de los fuera de flujo de dentro de algo girado) contra la página; `pageOverflow` señala
  el `Rotate` de un `Relative` girado que no cabe en una página.
- **Pintado.** `Canvas.beginRotation`/`endRotation`; `StackingContext.paint` envuelve el contexto de la
  caja girada y los dos pasos de una `Image` girada en flujo. `PdfCanvas`: `q`, `cm` y `Q`, y el giro
  compuesto para los `QuadPoints` y el envolvente de los enlaces y el `BBox` de las figuras;
  `PdfDestinations`, el envolvente. `SvgCanvas`: `<g transform="rotate(…)">`.

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

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-28, todas con la propuesta aceptada; las interpretaciones que salgan al
implementarlas, en [`DECISIONES.md`](DECISIONES.md)):

- [x] V1. **Cómo se incrusta:** vectorial. Un SVG se dibuja en un `Graphics2D` y un adaptador lo convierte
      en rutas y rellenos del PDF; en el SVG de salida se puede incrustar el original.
- [x] V2. **Librería:** JSVG (`com.github.weisj:jsvg`, pequeña y sin dependencias) para leer y dibujar el SVG
      y `pdfbox-graphics2d` (`de.rototor.pdfbox:graphics2d`) como adaptador de `Graphics2D` a PDFBox.
      Dos dependencias nuevas en el `pom.xml`.
- [x] V3. **Subconjunto:** formas básicas, `path`, `g`, `transform`, rellenos sólidos y degradados, trazos,
      `viewBox`, recorte y opacidad; sin scripts, animación ni filtros. Lo que queda fuera del
      subconjunto es un error (`IMAGE_FORMAT`), no se ignora en silencio.
- [x] V4. **Texto dentro del SVG:** solo con las fuentes de `resources/fonts/` (misma política que el
      resto: sin sustitución); si la fuente no existe, error. Con el adaptador, el texto queda como texto
      del PDF, seleccionable.
- [x] V5. **Tamaño natural:** el de `width` y `height` absolutos o, si faltan, el del `viewBox`, a 96 ppp
      como los PNG (§5.18). Sin ninguno de los dos, `IMAGE_FORMAT`.
- [x] V6. **Seguridad:** ninguna referencia externa (`href` a ficheros o URL, `<image>` externas,
      entidades DTD): es `IMAGE_FORMAT`.

**Decisiones abiertas:** *(ninguna)*

**Diseño técnico** (como quedó; las interpretaciones, en [`DECISIONES.md`](DECISIONES.md), 242–251):

- **Lectura y validación.** `resources/SvgInspector` lee el SVG con SAX (sin DTD ni entidades), comprueba el
  subconjunto (V3) y que no hay referencias externas (V6), saca el tamaño natural (V5) y los textos con su
  fuente; `ImageFile.read` reconoce el SVG por el contenido, comprueba las fuentes contra `fonts/` (V4) y
  guarda el tamaño (ahora `double`) y si hay texto. Todo error es `IMAGE_FORMAT` con la línea del SVG.
- **PDF.** `paint/PdfImages` agrupa lo que se incrusta: el PNG y el JPEG como antes y el SVG, que JSVG dibuja
  en un `PdfBoxGraphics2D` (un `PDFormXObject` del tamaño natural, una vez por fichero). `PdfCanvas.image` lo
  coloca con una matriz de escala dentro del recorte de la caja y restablece los guiones.
- **SVG de depuración.** `SvgCanvas` incrusta el SVG como `data:image/svg+xml`.
- **Texto.** JSVG lo dibuja como contornos de la fuente: se ve bien pero no se puede seleccionar (decisión 247;
  la V4 suponía lo contrario).

**Tareas:**

- [x] Resolver V1–V6 (2026-10-28).
- [x] Dependencias (`jsvg` 2.2.0 y `graphics2d` 3.0.5) en el `pom.xml`.
- [x] `ImageLibrary` e `ImageFile` con el formato SVG, tamaño natural y validación (`SvgInspector`; 3 casos
      nuevos en `test/errors/images/`).
- [x] `PdfCanvas` (`PdfImages`) y `SvgCanvas`; `PdfStructure`, igual que las demás imágenes.
- [x] Especificación (§2, §4.15, §5.18, §6 y §8), tests (`SvgImageTest`, `ImageLayoutTest`, `SvgCanvasTest`,
      `PdfCanvasTest.svgIsDrawnAsOneVectorFormAndScaledToItsBox`) y caso golden `24-svg`, calculado a mano.
- [x] `tools/ExampleImages.java` (clase `Svg`) y los ejemplos 01, 02, 04, 06, 07, 08, 09 y 10 (con 03 y 05 sin
      cambios: solo llevan fotos); PNG revisados a ojo y volcados sin cambios.

---

## Hito 32: secciones

**Objetivo.** Que un documento tenga varias secciones independientes entre sí, cada una con su
`Media` (papel, orientación y márgenes), sus propios `Fixed` y su propia numeración de páginas. Una
sección empieza siempre una página nueva. Generaliza varios pendientes de `TODO.md`: el margen de la
primera página (la portada es una sección con otro margen), la numeración que no cuenta la portada,
una página apaisada dentro de un documento vertical y las páginas con nombre al estilo de `@page`.

**Sintaxis.**

```
Document (dev.stxt.boxes):
	Media:
		Paper: A4
		Margin: 44 44 60 44
	Title: Factura 2025-0147
	Language: es
	Fixed:                       # de todas las secciones
		Pages: not-first
		...
	Section:
		Id: cover
		Page numbering: none
		Inherit fixed: false
		Media:
			Margin: 0
		Block: ...
	Section:
		Id: body
		Page numbering: restart
		Media:
			Orientation: landscape
		Fixed: ...               # solo de esta sección
		Block: ...
```

**Decisiones tomadas** (2026-10-28, todas con la propuesta aceptada; las interpretaciones, en
[`DECISIONES.md`](DECISIONES.md), 265–274):

- [x] S1. **`Section` es obligatoria.** `Document` tiene `Media`, `Title`, `Language`, `Styles`, `Fixed` y
      una o más `Section` (`Min: 1`); el contenido en flujo y los demás elementos posicionados van dentro de
      una sección. Un `Document` con contenido suelto es un error del schema.
- [x] S2. **El `Id` es un hijo**, como en el resto de elementos (`Section` no tiene valor): sirve para
      `Link: #id`, `{page of: id}` y `For id`.
- [x] S3. **`Media` de la sección** hereda del `Media` del documento por grupos: el tamaño (`Paper` +
      `Orientation`, o `Width` + `Height`) se hereda entero o se reemplaza entero, y `Margin` se hereda o se
      reemplaza. `Orientation` sola gira el `Paper` del documento; si el documento da `Width` y `Height`, es
      un error (`MEDIA_SIZE`).
- [x] S4. **`Fixed` por alcance.** El `Fixed` del documento sale en todas las secciones; el de una sección,
      solo en las suyas. `Pages` cuenta dentro del alcance donde se declara (`first` de una sección es su
      primera página; `first` del documento, la primera del documento). `Inherit fixed: false` en una
      sección la deja sin los `Fixed` del documento.
- [x] S5. **Numeración.** `Page numbering: continue` (por defecto), `restart` o `none`. Un contador: `continue`
      sigue, `restart` vuelve a 1, `none` no numera ni avanza. `{page}` es el contador y `{pages}`, su último
      valor en la tanda (las páginas desde el último `restart`).
- [x] S6. **Lo impreso manda.** `{page of: id}` y las entradas de un índice dan el número impreso, no el
      físico; una referencia a algo de una sección `none` es `REFERENCE_NOT_PLACEABLE`, y un `{page}` que cae
      en una página sin número (un `Fixed` del documento en una portada), `PAGE_NUMBER_UNAVAILABLE`. Los
      marcadores, los enlaces y `Pages` siguen la página física del PDF; las etiquetas de página del PDF
      (`PageLabels`) llevan la numeración impresa.
- [x] S7. **Qué admite una `Section`:** `Id`, `Class` (las reglas `For class` y `For id` que le llegan solo
      pueden ser `Typography`, que hereda todo su contenido), `Media`, `Page numbering`, `Inherit fixed` y su
      contenido. No hay `Background` de página ni `Start` (ver `TODO.md`).
- [x] S8. **Sección vacía:** `SECTION_EMPTY` (sin elementos, o solo con `Fixed`, no tiene ninguna página; un
      `Absolute` sí cuenta).
- [x] S9. **Volcado (§7):** una sección con `Id` no se vuelca como caja; sí sus cajas con `Id`, con las
      coordenadas de la página de su sección.
- [x] S10. **El documento sigue siendo uno** para la jerarquía de encabezados, los índices (`Contents`) y el
      `Styles`: se comprueban en el orden del documento, atravesando las secciones.

**Diseño técnico** (como quedó; las interpretaciones, en [`DECISIONES.md`](DECISIONES.md), 275–279):

- **Árbol.** Cada sección es la raíz de su propio layout: una `Box` (`Element.SECTION`, bloque) sin `parent`,
  igual que la raíz anónima de antes, que cuelga de `root.children` sin ser hija (la raíz del documento ya
  solo guarda las secciones y los `Fixed` del documento, y no se maqueta). Los recorridos de todo el árbol
  (referencias, enlaces, índices, fuentes e imágenes) siguen funcionando desde esa raíz.
  `BoxTreeBuilder.Result` da la raíz y la lista de `Section` (caja, `Media`, numeración e `Inherit fixed`).
- **`Media`.** `BoxTreeBuilder.media(n, base)` hereda por grupos y guarda el `Paper` para `Orientation` sola.
- **Paginación.** `Paginator` recorre las secciones: una pasada de flujo por sección con su `Media`, con
  una sola convergencia de las referencias para todo el documento y el número impreso de cada página
  (`Numbering`). `Page` lleva su `Media`, su sección y su número impreso; `Overflow`, el volcado y los
  lienzos usan el de cada página; `LaidOutDocument` lleva la raíz.
- **`Fixed`.** Los del documento los coloca el `Paginator` en cada página de cada sección que los hereda,
  con el área útil de esa página; los de la sección salen como siempre, de su flujo.
- **PDF.** `Sect` por sección, páginas con el tamaño de su `Media` y `PageLabels` con los números impresos.

**Tareas:**

- [x] Resolver S1–S10 (2026-10-28).
- [x] Schema (`Section`, `Page numbering`, `Inherit fixed`) y especificación (§2, §4, §5.21, §6, §7, §8, §9 y glosario).
- [x] `Section` en el builder; `Media` por sección; `Paginator`, `Page`, `Overflow`, volcado y lienzos por sección.
- [x] Migrar todos los documentos (tests, golden, catálogo, ejemplos, especificación y README) a `Section`.
- [x] `Fixed` del documento y de la sección; `Inherit fixed`.
- [x] Numeración (`Page numbering`, `{page}`, `{pages}`, `{page of}`) y `PageLabels` en el PDF.
- [x] Errores nuevos (`SECTION_EMPTY`, `PAGE_NUMBER_UNAVAILABLE`) con su fila en el §6 y sus casos en `test/errors/sections/`
      (también `MEDIA_SIZE`, `REFERENCE_NOT_PLACEABLE` y `STYLE_NOT_ALLOWED` con una sección, y dos de schema).
- [x] Caso golden `25-sections`, calculado a mano; ejemplos 04 y 09 con la portada como sección (el 07 no tiene portada y no cambia).
- [x] README, `CLAUDE.md`, `DECISIONES.md`, `TODO.md` y este documento.

---

## Registro de sesiones

Una entrada por sesión, la más reciente arriba: fecha, qué se hizo, dónde se quedó y el
siguiente paso concreto.

- **2026-10-28 (21).** Hito 32 terminado, en cuatro commits (decisiones; `Section`, `Media` por página y migración de
  los documentos; tests, PDF y caso golden; ejemplos y documentación). Resueltas S1–S10 con una discusión previa y
  «todas con la propuesta»: `Section` obligatoria (nada de contenido suelto en `Document`), `Id` como hijo, `Media`
  heredado por grupos, `Fixed` del documento y de la sección con `Pages` en su alcance e `Inherit fixed`,
  numeración por contador (`Page numbering`: `continue`, `restart`, `none`) con `{page of}` y los índices en el
  número impreso, `Class` solo con `Typography`, sección vacía como error y volcado sin la sección. Cada sección es
  una `Box` sin padre que cuelga de la raíz sin ser hija, así que no se tocó ningún algoritmo de layout y los
  recorridos del árbol siguen igual. Se migraron los 306 `.stxt` (script que envuelve y recalcula las líneas
  esperadas), los ejemplos de la especificación y los tests Java (`Docs.sectioned`), sin que cambie ningún PNG ni
  volcado. Al corregir la regla de la sección vacía salió que una portada solo de `Absolute` es legítima (cuenta).
  Tests nuevos: `SectionsTest`, dos de PDF (`PageLabels`, `Sect` y tamaño por página), nueve casos del catálogo y
  el caso golden `25-sections` (calculado a mano; dos bloques se partían porque un `Height` es el total de todos los
  fragmentos, y se arregló con `Inside: avoid`). Ejemplos 04 y 09 con la portada como sección (`Relative` a sangre,
  sin el bloque de 1 pt; «Página 1 de 6» y el índice con los números impresos); el 07 no cambia. Decisiones
  275–279 de `DECISIONES.md`. `mvn test` en verde.
  *Siguiente paso:* los hitos 16 a 32 están terminados. Pendiente de la sección en `TODO.md`: `Start: odd` con página en
  blanco para la doble cara, un número inicial, otros formatos de número y un `Background` de página.
- **2026-10-28 (20).** Continúa el repaso de accesibilidad con los dos huecos de contenido que quedaron, y `Title`
  obligatorio (pedido por el usuario: `Min: 1`, `Max: 1`; con la declaración de PDF/UA-1 en todo PDF, un
  documento sin título no podría afirmarla). Todos los documentos de tests, golden, catálogo de errores y ejemplos
  de la documentación llevan `Title`; los tests que miran líneas se corrigieron con un script que sube una línea
  donde el error cae después (y el `# Line:` del catálogo). `Header: true` en una `Cell` (`TH` con `Scope: Row`; en
  `Head` es el error `TABLE_HEADER`) y `In contents: false` en un encabezado, por cuestionario (la alternativa
  `Header columns: N` en la `Table` y `Listed: false` se descartaron). Ejemplos 01, 04, 05, 07, 08, 09 y 11 con
  cabeceras de fila; en el 04 y el 09 la portada y el título del índice son `Heading 1` fuera del índice, sin cambiar
  un píxel. Decisiones 262–264 de `DECISIONES.md`. `mvn test` en verde.
  *Siguiente paso:* elegir otro trabajo de `TODO.md` (queda de accesibilidad el `Alt` obligatorio, el idioma por
  tramo, el contraste y probar con lectores de pantalla reales).
- **2026-10-28 (19).** Repaso de accesibilidad del PDF etiquetado (primer punto de «Accesibilidad» de
  `TODO.md`; no es un hito). Se leyeron los once PDF como un lector de pantalla (estructura más el texto de cada
  contenido marcado, y veraPDF con PDF/UA-1, PDF/UA-2 y WTPDF). Resuelto con un cuestionario: `Text` de bloque como
  `P`, `Language` en `Typography` con su `Lang`, espacio al final de las líneas partidas (las palabras salían
  pegadas, «vientoprestado») y declarar PDF/UA-1 al final. Se descartaron por ahora las cabeceras de fila en
  tablas y los títulos reales de portada e índice (apuntados en `TODO.md`) y el `Alt` obligatorio. Dos casos
  nuevos de error (`Language` mal escrito y `Language` en `Element: Document`). Decisiones 258–261 de
  `DECISIONES.md`. Ningún PNG ni volcado cambia. `mvn test` en verde.
  *Siguiente paso:* cabeceras de fila en tablas y títulos reales de portada e índice (los dos huecos de
  contenido que quedan), o elegir otro trabajo de las ideas.
- **2026-10-28 (18).** `Link` del PDF etiquetado con su contenido marcado (deuda de la decisión 171, primer
  punto de «Accesibilidad» de `TODO.md`; no es un hito de esta hoja de ruta). Resuelto con un cuestionario
  (todas las propuestas, y veraPDF como test del proyecto): un `Link` por enlace y fragmento aunque ocupe
  varias líneas, el `Link` de una caja envuelve su contenido (una imagen cuelga de él, no al revés) y sin
  `Alt` si ya contiene algo que leer. El texto se pinta ahora en secuencias de contenido marcado seguidas
  con el mismo destino (`Canvas.beginLinkText`). Al validar los once ejemplos con veraPDF salieron dos
  reglas más ligadas a los enlaces que se arreglaron en el mismo cambio: las anotaciones de lo que no es
  contenido (un `Fixed`, las filas repetidas de una tabla) tampoco estaban en un `Link` (ahora van a un
  `Link` suelto del `Document`) y faltaba `Tabs: S`. `ExamplesTest` falla con cualquier regla de PDF/UA-1
  incumplida salvo la 5-1 (el XMP no declara `pdfuaid:part`; afirmar la conformidad queda como decisión
  pendiente, en `TODO.md`). Decisiones 252–257 de `DECISIONES.md`. Ningún PNG ni volcado cambia. `mvn test`
  en verde (1230 tests).
  *Siguiente paso:* decidir cuándo declarar la conformidad PDF/UA y repasar el resto de la accesibilidad
  (`TODO.md`), o elegir otro trabajo de las ideas.

- **2026-10-28 (17).** Hito 31 terminado, en cuatro commits (decisiones, código y pruebas, ejemplos y
  documentación). Resueltas V1–V6 con un cuestionario (todas las propuestas): SVG vectorial con JSVG 2.2.0 y
  `pdfbox-graphics2d` 3.0.5, subconjunto cerrado (formas, rutas, grupos, degradados, recortes, trazos y texto;
  lo demás es `IMAGE_FORMAT`), texto solo con las fuentes de `fonts/`, tamaño natural de `width`/`height` o
  `viewBox` a 96 ppp y ninguna referencia externa. Al probar JSVG salió que dibuja el texto como contornos: se
  ve igual pero no es seleccionable (decisión 247; ver `TODO.md`). Al mirar el PNG del caso golden salieron
  los trazos discontinuos: el modo `debug` deja un patrón de guiones y los formularios heredan el estado
  gráfico (decisión 249). Caso golden `24-svg` (calculado a mano, pasó a la primera), tres casos en el
  catálogo y `SvgImageTest`. `tools/ExampleImages.java` escribe SVG (clase `Svg`) para los logotipos, las
  firmas, el sello, los gráficos, el emblema, el adorno, el termostato y el esquema: ningún volcado cambia y
  los PNG se ven igual o más nítidos. Decisiones 242–251 de `DECISIONES.md`. `mvn test` en verde.
  *Siguiente paso:* los hitos 16 a 31 de esta hoja de ruta están terminados. Pendiente: repasar las
  decisiones de `DECISIONES.md` y elegir el siguiente trabajo (ideas en `TODO.md`).
- **2026-10-28 (16).** Hito 30 terminado, en tres commits (decisiones, código y documentación con el caso
  golden y los ejemplos). Resueltas O1–O5 con un cuestionario: `Rotate` en `Relative`, `Absolute`, `Fixed` e
  `Image` (no en `Text`), solo pintado pero lo girado debe caber en la página y un `Relative` girado no se
  parte; cualquier ángulo de −360 a 360 sin 0; `Rotate origin` con palabras clave; `Paper` (no `Size`, que
  ya es el tamaño de la fuente) y `Orientation` en `Media`; enlaces girados con `QuadPoints`. Dos códigos
  nuevos (`ROTATE_ORIGIN_WITHOUT_ROTATE`, `BREAK_IN_ROTATED`) y 18 casos en el catálogo; el de «medio sin
  alto» pasa de `TOO_FEW_CHILDREN` a `MEDIA_SIZE`. Caso golden `23-rotation` (calculado a mano, pasó a la
  primera; solo se ensanchó el relleno de un bloque para que la etiqueta no tapara el texto). Ejemplos 09,
  10 y 11; ningún PNG golden ni volcado de los demás ejemplos cambia. Decisiones 232–241 de
  `DECISIONES.md`. `mvn test` en verde (1205 tests).
  *Siguiente paso:* hito 31 (SVG), en otra sesión; resolver V1–V6 con el usuario antes de escribir código
  (la V2, la librería, es la que más decide el coste). Lo que quedó fuera del 30 está en `TODO.md`.
  Después, a petición del usuario, los demás documentos de muestra en A4 (ejemplos 01–05, 07 y 08,
  `test/spec/report.stxt` y los ejemplos de la especificación y del README) pasan a `Paper: A4`; el 06 sigue
  con su tamaño propio. Ningún PNG ni volcado cambia.
- **2026-10-28 (15).** Documento de estilos aparte (primer punto de `TODO.md`; no es un hito de esta hoja de
  ruta). Resuelto con un cuestionario: la tipografía base pasa a `Element: Document` (y `Document` ya no admite
  `Typography`), las reglas del documento aparte valen menos que las del documento en cada nivel de
  prioridad, y las reglas `For id` sin elemento no son nunca un error (se quitó `STYLE_UNKNOWN_ID`: las plantillas pueden rellenar el elemento después). `DocumentReader.readStyles`,
  `BoxTreeBuilder.build(documento, estilos)`, `Engine.layOutDocument(documento, estilos)` y
  `PdfWriter.write(documento, estilos, …)`; `StyleRules` lee los dos orígenes; los errores del documento aparte dicen
  `de los estilos`. Migrados todos los documentos de test, los once ejemplos (cada uno con su `styles.stxt`; los PNG y
  los volcados no cambian), la especificación (§4.10, nueva subsección) y el README. `StylesFileTest`, dos casos más
  en el catálogo. Decisiones 226–231 de `DECISIONES.md`. `mvn test` en verde.
  *Siguiente paso:* hito 30 (rotación y orientación de la página) o 31 (SVG), en otra sesión; resolver sus
  decisiones con el usuario antes de escribir código.
- **2026-10-28 (14).** Hito 28 terminado. Resueltas A1–A6 con un cuestionario (A1, A2 y A4 con la propuesta;
  A3, sin sombras; A5 y A6 por defecto). `Radius` (1 a 4 valores, número o %), `Border color` de 1 a 4
  colores con las uniones en diagonal, `Background` con `linear(ángulo, color…)` y colores con opacidad
  (`#RGBA`, `#RRGGBBAA`, también en el texto). `PdfCanvas`: curvas de Bézier, anillo par-impar, sombreado
  axial y máscara suave de luminosidad para el degradado con transparencia; `SvgCanvas` con `<path>`,
  `linearGradient` y `clipPath`. Al mirar el PNG del caso golden salió un fallo: con el borde de colores
  distintos y la curva de dentro redonda quedaba una cuña en blanco en cada esquina (los trapecios llegaban
  solo a la esquina de relleno); las diagonales siguen ahora hacia dentro y un test de píxeles lo vigila.
  El borde liso, opaco y cuadrado se pinta como antes para no mover ningún PNG existente. Nueve casos en el
  catálogo, siete clases de tests nuevas y el caso golden `21-box-looks`. Ocho ejemplos reescritos (solo
  cambia el volcado del 04, por la caja `cover-fade`). Decisiones 216–225 de `DECISIONES.md`.
  `mvn test` en verde.
  *Siguiente paso:* hito 30 (rotación y orientación de la página) o 31 (SVG), en otra sesión; resolver sus
  decisiones con el usuario antes de escribir código. Quedó fuera del 28: las sombras, los degradados con
  paradas, radiales o repetidos y los dos radios por esquina (`TODO.md`).
- **2026-10-28 (13).** Hito 29 terminado, en tres commits. Resueltas W1–W5 con un cuestionario (todas las
  propuestas, y W3 ampliada por el usuario: una propiedad `Tab size` con 4 por defecto, que se tradujo en
  tabuladores de columna). `White space` (`normal`, `pre`, `pre-wrap`) y `Tab size` en `Typography`:
  `PreformattedText` reparte el texto en las líneas escritas, sin justificar y con todo el texto como un
  solo párrafo para las viudas y huérfanas; `pre` desborda con `TEXT_OVERFLOW` y `pre-wrap` parte entre
  palabras. `Code font` en `Markdown`: el código en línea es un tramo con otra familia
  (`TextFormat.family`) y un bloque de código, un `Text` con `pre`. Ocho casos nuevos en el catálogo,
  `PreformattedTextTest` y el caso golden `22-preformatted` (calculado a mano, pasó a la primera).
  Ejemplos 01 (IBAN y BIC en código) y 08 (terminal preformateado). Decisiones 203–214 de
  `DECISIONES.md`. `mvn test` en verde.
  *Siguiente paso:* hito 28 (aspecto de las cajas, el más caro y el que más se nota), 30 (rotación) o 31
  (SVG), en otra sesión; resolver sus decisiones con el usuario antes de escribir código. Lo que quedó
  fuera del 29 (un elemento `Code block` para dar estilo aparte al código, `Code font` fuera de
  `Markdown`) está en `TODO.md`.
- **2026-10-28 (12).** Hito 27 terminado. Resueltas Y1–Y4 con un cuestionario (todas las propuestas,
  con una condición en Y4: si el idioma es una característica de accesibilidad, que sea obligatorio en el
  schema; lo es, así que `Language` pasó a ser obligatorio en `Document` en un commit aparte, con
  todos los documentos de prueba, los ejemplos y los de la especificación) y Y5 por mi cuenta (`TJ`).
  `Transform`, `Letter spacing` y `Small caps` en `Typography`: `TextTransform` sobre los tramos antes
  de repartir, avance con interletraje y versalitas en `LineBreaker` (el último carácter de la línea no
  lo lleva), `TJ` en el PDF, `letter-spacing` en el SVG y `ActualText` en el PDF etiquetado. Cuatro
  casos nuevos en el catálogo, `TextTransformTest`, `TextTypographyTest`, `TextCaseTest` y el caso
  golden `20-typography` (calculado a mano, pasó a la primera). Nueve ejemplos con los rótulos en
  minúscula y una regla `Transform` (ningún volcado cambia; los PNG solo en los rótulos). Decisiones
  193–202 de `DECISIONES.md`. `mvn test` en verde.
  *Siguiente paso:* hito 29 (texto preformateado, de esfuerzo bajo-medio) o el 28 (aspecto de las
  cajas, el más caro), en otra sesión; resolver sus decisiones con el usuario antes de escribir código.
- **2026-10-28 (11).** Hito 26 terminado. Resueltas F1–F5 con un cuestionario (todas las propuestas):
  `Inline: markdown` en `Typography` (se hereda y vale en `Styles`), con la sintaxis y los enlaces de
  `Markdown`, escapes de CommonMark y solo párrafos. Reutiliza el parser y el `Translator` de
  `MarkdownBuilder` (`inlineOnly`); `Box.text` es el texto sin marcas y no actúa dentro de un
  `Markdown`. Seis casos nuevos de `MARKDOWN_UNSUPPORTED` en el catálogo, `InlineFormatTest` y el caso
  golden `19-inline` (calculado a mano; al pasar se corrigió una cuenta mía, no el código). Ejemplos 04,
  07 y 08 con `Paragraph` en vez de `Markdown` de relleno (los PNG y los volcados no cambian) y 01, 03 y
  06 con una negrita o una cursiva. Decisiones 186–192 de `DECISIONES.md`. `mvn test` en verde.
  *Siguiente paso:* hito 27 (mayúsculas, interletraje y versalitas), en otra sesión; resolver Y1–Y5
  con el usuario antes de escribir código.
- **2026-10-28 (10).** Hito 24 terminado. Resueltas N1–N5: `Pages` en `Fixed` y nada más (sin
  `First margin`, numeración física, sin reservar espacio, sin página en blanco). `Style.Pages`, el filtro
  del `Paginator`, dos casos de schema, `FixedPagesTest` y el caso golden `17-page-types`
  (calculado a mano, pasó a la primera). Ejemplos 04, 06, 07 y 09 con el pie o el logotipo en
  `not-first` (y sin el `Layer` de las portadas) y el boletín con un titulillo corrido; los
  volcados solo pierden o ganan las entradas de esos `Fixed` y los PNG de 04 y 09 no cambian.
  Decisiones 180–185 de `DECISIONES.md`. `mvn test` en verde.
  *Siguiente paso:* hito 26 (formato en línea fuera de Markdown, esfuerzo bajo-medio), en otra
  sesión; resolver sus decisiones con el usuario antes de escribir código. Lo que quedó fuera
  del 24 (margen de la primera página, numeración sin portada, doble cara) está en `TODO.md`.
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
