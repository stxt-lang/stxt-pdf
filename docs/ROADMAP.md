# Hoja de ruta: elementos de documento

Plan de trabajo para añadir al motor las reglas de estilo reutilizables y los elementos de
documento: encabezados y párrafos, listas, tablas y bloques en columnas. Todos los hitos están
terminados (2026-10-27). Es el punto de partida de cada sesión: el estado, las decisiones tomadas y
lo que queda por hacer están aquí, no en la memoria de la conversación.

Los hitos 1 a 10 (el motor de cajas) están terminados; ver la tabla de estado del
[README](../README.md). Esta hoja de ruta continúa la numeración.

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

| Hito | Elemento | Estado | Caso golden |
|---|---|---|---|
| 11 | Reglas reutilizables (`Styles`: por elemento, clase e id) | hecho | 08 |
| 12 | Encabezados (`Heading 1`…`Heading 6`) y párrafos (`Paragraph`) | hecho | 09 |
| 13 | Listas (`ul`, `ol`, `li`) | hecho | 10 |
| 14 | Tablas, con cabecera y pie repetidos | hecho | 11 |
| 15 | Bloque en columnas (multicolumna) | hecho | 12 |

**Orden**, de menor a mayor impacto en el motor:

- **Reglas reutilizables** primero: dan a cada tipo de elemento su estilo sin repetirlo en
  cada caja (decisión T1), y los elementos semánticos que vienen después se apoyan en ellas.
  Solo tocan la construcción del árbol, no el layout.
- **Encabezados y párrafos**: son casi un `Text` con significado. Fijan cómo se comporta un
  elemento *semántico* (marcadores en el PDF, cortes), y el resto de hitos reutiliza esa
  decisión.
- **Listas** después: añaden una caja nueva (el marcador) y la numeración, pero el flujo sigue
  siendo de bloque.
- **Tablas**: un algoritmo de layout nuevo, o una variante de grid, y repetir cabecera y pie
  al partir.
- **Columnas** al final: es lo que más toca la fragmentación, porque cada columna es un
  fragmentainer anidado dentro de la página.

El orden se puede cambiar; si se cambia, actualiza esta sección.

## Criterios comunes (definición de hecho)

Cada hito está terminado cuando, además de sus tareas propias, cumple lo de "Al cambiar el
comportamiento" de [`CLAUDE.md`](../CLAUDE.md):

- [ ] Schema [`dev.stxt.boxes.stxt`](../src/main/resources/stxt/dev.stxt.boxes.stxt): elementos y
      propiedades nuevos. **Cada nombre tiene una única definición en todo el namespace**: un
      nombre que ya existe (`Item`, `Cell`, `Columns`, `Gap`…) solo se reutiliza si significa
      lo mismo y admite los mismos valores.
- [ ] Especificación: §2 (tabla de elementos), §4 (referencia de propiedades), §5 (semántica,
      una sección nueva por elemento), §6 (reglas de validación nuevas), §7 (volcado), §8
      (diferencias con CSS/HTML), §9 (resumen del schema) y, si aplica, §10 (ejemplo).
- [ ] `Style`, `BoxTreeBuilder` (lectura y validación del §6) y, si hay layout nuevo, su
      algoritmo en `layout/…`, despachado por `Engine`.
- [ ] Cada regla de validación nueva: su `ErrorCode`, su fila en el §6 y su caso en
      [`errors.stxt`](../src/test/resources/errors/errors.stxt).
- [ ] Tests unitarios de las piezas puras y tests de layout con `LayoutFixture`, incluidos los
      de fragmentación (`*FragmentationTest`).
- [ ] Un caso golden nuevo en `src/test/resources/golden/NN-nombre/`, con el resultado
      calculado a mano.
- [ ] `DocumentReaderTest` valida los documentos nuevos.
- [ ] Salida SVG y PDF revisada a ojo (PNG del caso golden).
- [ ] README (tabla de estado y hoja de ruta), `CLAUDE.md` (tabla de hitos) y este documento
      actualizados; decisiones apuntadas en `DECISIONES.md`.

## Decisiones transversales

Afectan a varios hitos. Hay que resolverlas en el primero que las necesite.

**Abiertas:**

- **T4. Texto con formato dentro de una línea.** Hoy un `Text` tiene un único estilo. Un
  encabezado o un ítem de lista con una palabra en negrita necesitaría tramos (*spans*).
  Propuesta: fuera de alcance de estos hitos; si hace falta, sería un hito propio.

**Tomadas:**

- **T1. Valores por defecto de los elementos semánticos** (2026-10-27). No hay estilos
  implícitos en el motor: el estilo de cada tipo de elemento (un `Heading 1` más grande que un
  `Heading 2`) se declara una vez en las reglas `Styles` del documento (hito 11). Se respeta
  la política de "nada implícito" del §6.
- **T2. Significado en la salida** (2026-10-27). En el hito 12 se hacen los marcadores del PDF
  (*outline*) para los encabezados **y el PDF etiquetado** (*tagged PDF*, accesibilidad) para
  todo el documento. Cada hito siguiente etiqueta sus elementos (listas, tablas…). Queda por
  decidir en el hito 12 si el volcado del §7 incluye el tipo de elemento (G3).
- **T3. Nombres que chocan** (2026-10-27). `Item` (hijo de `Flex`), `Area` (hijo de `Grid`,
  antes `Cell`) y `Columns` (pistas de `Grid`) tienen una única definición en el namespace, y
  cada hito eligió nombres que no chocan o que comparten el significado: las listas, `List
  item` (L1); las tablas, `Cell` (el de `Grid` pasa a `Area`) y `Columns` y `Gap` reutilizados
  (TB1); las columnas, nombres propios (`Count`, `Column gap`…, C1).

---

## Hito 11: reglas reutilizables

**Objetivo.** Declarar una vez, en el `Document`, propiedades que se aplican por defecto a
todos los elementos de un tipo, a los que tienen una clase o a uno con un `Id` concreto. El
elemento puede sobrescribirlas con sus propias propiedades.

**Sintaxis** (nombres decididos; los detalles de `Class`, en S1):

```
Document (dev.stxt.boxes):
	Media:
		Width: 595
		Height: 842
	Typography:
		Font: IBM Plex Sans
		Size: 10
	Styles:
		Element: Heading 1
			Typography:
				Size: 20
				Weight: bold
			Break:
				After: avoid
		Element: Paragraph
			Box:
				Margin: 0 0 6 0
		For class: note
			Box:
				Background: #F1EFE8
				Padding: 8
		For id: summary
			Typography:
				Style: italic
	Heading 1: Resultados
	Paragraph: Texto normal.
	Paragraph: Una nota.
		Class: note
	Paragraph: El resumen.
		Id: summary
```

**Decisiones tomadas** (2026-10-27):

- [x] R1. El contenedor es **`Styles`**, hijo de `Document` (como mucho uno). `Style` ya es
      la propiedad `normal`/`italic` de `Typography`.
- [x] R2. Las reglas son **`Element: <tipo>`**, **`For class: <clase>`** y
      **`For id: <id>`**. `Id` no puede tener hijos: ya es una propiedad.
- [x] R3. Las **clases entran en este hito**. Un elemento puede tener varias.
- [x] R4. **Prioridad**, de más a menos fuerte: lo declarado en el propio elemento, la regla
      `For id`, las reglas `For class`, la regla `Element`, lo heredado del padre y el valor
      por defecto. Una regla gana a la herencia: un `Element: Text` con `Size: 10` se impone
      al `Size: 20` que el `Text` heredaría de su padre.
- [x] R5. **Mezcla por propiedad, no por grupo.** Si la regla declara `Typography / Size` y el
      elemento `Typography / Weight`, se aplican las dos.
- [x] R6. Una regla solo admite los grupos **`Box`, `Typography` y `Break`**; no las
      propiedades propias de cada elemento (`Gap`, `Columns`, `Offset`…).
- [x] R7. **Validación como si se hubiera declarado en el elemento** (política estricta del
      §6): lo que una regla aporta se valida igual, y el error señala la línea de la
      propiedad dentro de la regla. Si un caso concreto molesta, se relaja después.
- [x] R8. Un `For id` con un `Id` que no existe es un error. Un `Element` sin elementos de ese
      tipo en el documento no lo es, para que las reglas se puedan reutilizar.
- [x] R9. `Item` y `Cell` no se pueden nombrar en `Element`: no son cajas.
- [x] R10. Los tipos que se pueden nombrar son los elementos de caja (`Block`, `Flex`, `Grid`,
      `Text`, `Relative`, `Absolute`, `Fixed`) y los que vengan (`Paragraph`, `Heading 1`…
      `Heading 6`, listas, tablas…). Un nombre que no es un elemento es un error.
- [x] R11. Importar reglas de un fichero de la carpeta de recursos queda **para más
      adelante**.

Decididas también (2026-10-27), con la propuesta aceptada:

- [x] S1. `For class: note` en las reglas y `Class: note aside` en el elemento (lista
      separada por espacios, sin repetir). Son dos nodos distintos, porque `Class` no tiene
      hijos y `For class` sí. `Class` se admite en todos los elementos de caja.
- [x] S2. Varias clases de un mismo elemento que declaran la misma propiedad: gana la regla
      que va **después en `Styles`** (como en CSS), no el orden de `Class: a b`.
- [x] S3. Dos reglas con el mismo selector son un error (`STYLE_DUPLICATE`). Los nombres de
      `Element` se comparan en forma canónica: `Element: block` repite `Element: Block`.
- [x] S4. Una clase de `Class` sin regla `For class` es un error (`CLASS_WITHOUT_RULE`). Un
      `For class` sin elementos con esa clase se permite (como R8).
- [x] S5. El `Typography` de `Document` se mantiene como lo que hereda la raíz; las reglas no
      se aplican a `Document`.

**Diseño técnico (implementado):**

- Schema: `Styles` (`GROUP`, hijo de `Document`) con `Element`, `For class` y `For id`
  (`INLINE`: el valor es el selector; hijos `Box`, `Typography` y `Break`). `Class` en los
  siete elementos de caja. El valor de `Element` lo comprueba el builder.
- `BoxTreeBuilder.styles` lee las reglas al empezar (por tipo, por clase en orden del
  documento y por id) y comprueba sus valores sobre un `Style` vacío, aunque no se apliquen.
  `applyRules` las aplica a cada elemento después de `Style.inheriting` y de las propiedades de
  `Item`/`Cell`, y antes de las propias, con las mismas funciones `box`, `typography` y
  `fragmentation`: cada propiedad guarda la línea de la regla, así que la mezcla por propiedad
  y la validación estricta salen sin código específico.
- Errores nuevos: `STYLE_UNKNOWN_ELEMENT`, `STYLE_UNKNOWN_ID` (se comprueba al final, con todos
  los `Id` leídos), `STYLE_DUPLICATE`, `STYLE_NOT_ALLOWED` (`Break` a un `Absolute` o un
  `Fixed`, en la línea del `Break` de la regla) y `CLASS_WITHOUT_RULE`. Una clase repetida en
  `Class` o un selector vacío o con espacios es `VALUE_NOT_ALLOWED`.
- No cambian el layout, el pintado ni el volcado del §7.

**Tareas:**

- [x] Resolver S1–S5.
- [x] Schema: `Styles`, `Element`, `For class`, `For id`, `Class`.
- [x] `BoxTreeBuilder`: lectura de reglas, aplicación con su prioridad, validaciones.
- [x] Tests de las reglas en `tree/StylesTest` (prioridad, mezcla por propiedad, regla frente a
      herencia, clases en orden de `Styles`, ítems, nombres canónicos, reglas sin usar) y de
      layout en `BlockLayoutTest.stylesRulesShapeTheLayout`.
- [x] `ErrorCode` y casos en el catálogo de errores (los cinco códigos nuevos, más una regla
      con `Margin: auto` en un ítem y un valor no válido en una regla sin usar).
- [x] Especificación: §2, §4.2 (`Class`), §4.10 (nueva, reglas), §6, §8 y §9.
- [x] Especificación §10: el ejemplo completo (`report.stxt` y la copia del caso golden 05)
      lleva el estilo en `Styles`. El resultado y el PNG del caso 05 no cambian.
- [x] Caso golden `08-styles` (prioridad, orden de clases, regla frente a herencia), añadido
      a `DocumentReaderTest`.
- [x] README (tabla de estado), `CLAUDE.md` (tabla de hitos) y `DECISIONES.md` (79–82).

---

## Hito 12: encabezados y párrafos

**Objetivo.** Seis elementos de encabezado, `Heading 1` … `Heading 6`, equivalentes a
`h1`…`h6`, que aparecen en los marcadores del PDF, y un elemento `Paragraph`. Su estilo sale
de las reglas `Styles` (hito 11).

**Sintaxis** (decidida en H2, H6 y P2):

```
Heading 1:
	Id: results
	Class: chapter
	Text: Resultados del trimestre
Paragraph:
	Content >>
		Texto del párrafo.
```

**Decisiones tomadas** (2026-10-27):

- [x] H1. **Seis elementos**, `Heading 1` … `Heading 6`, no un `Heading` con `Level`: así una
      regla los nombra directamente (`Element: Heading 1`).
- [x] H2. Un encabezado es un **contenedor de bloque** cuyos únicos hijos de contenido son
      elementos **`Text`**, apilados como en un `Block`. No admite texto en el valor ni
      `Content`. El título de su marcador es el texto de sus `Text` unido. Si llegan los tramos
      (T4), caben aquí sin cambiar el elemento.
- [x] H3. El estilo de cada nivel se declara en `Styles` (T1); el motor no tiene estilos
      implícitos.
- [x] H4. **El motor evita siempre el corte** en los encabezados: no se declara, lo aplica él.
      El encabezado no admite `Break`.
- [x] H6. Además de sus `Text`, un encabezado admite **`Id`, `Class`, `Box` y `Typography`**.
      `Box` y `Class` hacen falta para que las reglas `Styles` le den márgenes (se validan como
      si los declarara el elemento, R7).
- [x] H7. Marcadores del PDF y PDF etiquetado en este hito (T2).
- [x] P1. Habrá un elemento **`Paragraph`**.
- [x] P2. `Paragraph` es un **`Text` con significado**: admite lo mismo y se maqueta igual;
      cambia su tipo (reglas `Element: Paragraph`, etiqueta `P` en el PDF). `Text` sigue
      existiendo como caja de texto sin significado.

Decididas también (2026-10-27), con la propuesta aceptada salvo H9:

- [x] H4b. El motor fija `After: avoid` (no queda solo al final de la página) e
      `Inside: avoid` (no se parte), relajables como cualquier `avoid` (§5.10); no `Before`.
      Sin `Break` no hay `Before: page` en un encabezado; se acepta de momento.
- [x] H5. No hay propiedad para excluir un encabezado del índice.
- [x] H8. Los `Text` de un encabezado no admiten `Break` (chocaría con H4) y hay al menos uno.
- [x] H9. **Los saltos de nivel son un error**: la jerarquía de encabezados tiene que ser
      correcta. En orden del documento, el primero es `Heading 1` y cada uno sube como mucho
      un nivel respecto al anterior (bajar, los que quiera).
- [x] H10. Un encabezado dentro de un `Fixed` es un error: el `Fixed` es *artifact* y no
      tendría marcador ni etiqueta.
- [x] P3. Un `Paragraph` es un solo párrafo: líneas en blanco en su `Content` son un error.
- [x] G1. Etiquetas del PDF: `Document` → `Document`; `Block`, `Flex`, `Grid`, `Relative`,
      `Absolute` → `Div`; `Text` → `Span`; `Paragraph` → `P`; `Heading N` → `HN`; `Fixed`
      (repetido en cada página) → *artifact*. Orden de lectura: el del documento.
- [x] G2. Fondos y bordes son *artifact*.
- [x] G3. El volcado del §7 no incluye el tipo de elemento.

**Diseño técnico (implementado):**

- `BoxTreeBuilder` traduce el encabezado a una `Box` de bloque (`Layout.BLOCK`) y el
  `Paragraph` a una de texto (`Layout.INLINE`), con su tipo de elemento en `Box.element`
  (reglas, marcadores, etiquetas). Sin algoritmo nuevo. El corte evitado del encabezado se
  fija en su `Style` al construirlo; la jerarquía se comprueba en orden del documento.
- `paint/PdfStructure`: árbol de estructura (`PDStructureTreeRoot`, `MarkInfo`) con un
  elemento por caja, contenido marcado (`BDC`/`EMC` con `MCID`, desde
  `Canvas.beginText`/`endText`) y árbol de padres por página; los marcadores
  (`PDDocumentOutline`) se anidan por nivel y llevan al primer fragmento. Sin `Lang` (decisión
  87 de `DECISIONES.md`).
- Errores nuevos: `HEADING_LEVEL`, `HEADING_IN_FIXED`, `BREAK_IN_HEADING`,
  `PARAGRAPH_BLANK_LINE`; `STYLE_NOT_ALLOWED` cubre también `Break` en encabezados.

**Tareas:**

- [x] Resolver H2, H4, P2 y T2.
- [x] Resolver H4b, H5, H8, H9, H10, P3 y G1–G3.
- [x] Schema, `Box`, `BoxTreeBuilder` (con el corte evitado del encabezado).
- [x] Marcadores y PDF etiquetado (`PdfStructure`), con sus tests en `PdfCanvasTest`.
- [x] Tests del árbol (`tree/HeadingsTest`) y de fragmentación (`BlockFragmentationTest`).
- [x] Especificación (§2, §4.10, §5.1, §5.13 nueva, §6, §8, §9), catálogo de errores y
      `DECISIONES.md` (83–88).
- [x] Caso golden `09-headings`, añadido a `DocumentReaderTest`.
- [x] README, `CLAUDE.md`.

---

## Hito 13: listas

**Objetivo.** Listas sin numerar y numeradas (`ul`, `ol`, `li`), anidables, con el marcador
fuera del contenido y alineado con la primera línea del ítem.

**Sintaxis** (decidida en L1–L6):

```
Styles:
	Element: List
		Box:
			Padding: 0 0 0 18
List:
	Marker: decimal
	Start: 3
	List item:
		Paragraph: Primer punto
	List item:
		Number: 10
		Paragraph: Segundo punto
		List:
			Marker: disc
			List item:
				Text: Subpunto
```

**Decisiones tomadas** (2026-10-27, con la propuesta aceptada):

- [x] L1. Un único elemento **`List`** con **`Marker`** (no `Bullets` y `Numbered`: una
      regla `Element: List` vale para todas, y el PDF etiquetado tiene un solo `L`). El ítem
      es **`List item`**, el término de CSS (`display: list-item`), que no choca con `Item`.
      `Marker` es **obligatorio**, sin valor por defecto (si la lista es numerada lo decide el
      autor, T1). `Marker`, `Start` y `Number` son propiedades del elemento: no van en
      `Styles` (R6).
- [x] L2. `List item` es un **contenedor de bloque con varios hijos**, como un `Block`:
      `Block`, `Flex`, `Grid`, `Text`, `Paragraph`, `List`, `Relative`, `Absolute` y
      `Fixed`, además de `Id`, `Class`, `Box`, `Typography`, `Break` y `Number`. Sin
      encabezados (chocarían con H9 y los marcadores del PDF) ni texto en el valor (sería una
      caja implícita). `List` solo admite `List item` (al menos uno, lo comprueba el schema) y
      va donde va un `Block`.
- [x] L3. Marcadores: `disc` (`•`), `dash` (`–`), `none`, `decimal`, `lower-alpha`,
      `upper-alpha`, `lower-roman` y `upper-roman`. El marcador es **texto** en la fuente del
      ítem (etiqueta `Lbl`, validado por `PdfMetrics`). Sin `circle` ni `square`: IBM Plex
      Sans no tiene `◦` ni `▪` y no hay fuente de sustitución (dibujarlos como formas
      necesitaría un primitivo nuevo en `Canvas`). Sufijo fijo `.`; sin marcador libre.
      Los enums leen el guion como guion bajo (`lower-alpha` ↔ `LOWER_ALPHA`); se apunta en
      `CLAUDE.md`.
- [x] L4. Marcador solo **fuera** (`outside`), sin propiedad.
- [x] L5. Sin `Indent`: la sangría es el **`Padding` izquierdo de la `List`** (como en CSS),
      normalmente en `Element: List`. El marcador es `"1. "` o `"• "`, con el espacio final
      como separación (sin `Gap`), alineado a la derecha contra el borde izquierdo del ítem;
      su ancho es el suyo (`9.` y `10.` quedan alineados a la derecha).
- [x] L6. `Start` en `List` (entero, por defecto 1) y `Number` en `List item` (fija su
      número; los siguientes continúan desde él; `Number` y no `Value` para no confundirlo
      con el del metaschema). Sin orden inverso. Se numera en `BoxTreeBuilder`. Rangos:
      `decimal` desde 0; alfabéticos y romanos desde 1; romanos hasta 3999; tras `z`, `aa`.
- [x] L7. El marcador usa la **tipografía del `List item`** (propia o heredada), sin
      propiedades propias; se cambia con `Element: List item`. Su línea base es la de la
      primera línea que se encuentra bajando por los primeros hijos de bloque del ítem (con
      una lista anidada al principio, los dos marcadores quedan en la misma línea).
- [x] L8. Errores nuevos, con la línea del nodo culpable: `LIST_ITEM_EMPTY` (un `List item`
      sin elementos; el schema no lo expresa, su `Min` es por tipo de hijo),
      `LIST_MARKER_OVERFLOW` (el marcador no cabe en el padding izquierdo de la lista más el
      margen izquierdo del ítem), `LIST_NUMBER_NOT_ALLOWED` (`Start` o `Number` con `disc`,
      `dash` o `none`), `LIST_NUMBER_RANGE` y `LIST_ITEM_WITHOUT_LINE` (marcador sin primera
      línea con la que alinearse, por ejemplo un ítem que empieza con `Flex` o `Grid`).

**Diseño técnico (previsto):**

- La lista es un flujo de bloque (`BlockLayout`); cada ítem, una caja de bloque con una caja
  de marcador generada (no está en el documento) colgada a su izquierda, en la sangría.
- El marcador se alinea con la línea base de la primera línea del ítem: hace falta que el
  layout de un bloque devuelva la línea base de su primera línea (hoy no la devuelve).
- Fragmentación: el marcador va pegado a la primera línea, así que va en su fragmento y pasa
  de página con ella; los fragmentos siguientes no lo repiten.
- Numeración: se resuelve al construir el árbol (`BoxTreeBuilder`), no en el layout.
- PDF etiquetado: `List` → `L` (con `ListNumbering`: `Disc`, `Decimal`, `LowerRoman`…;
  `None` para `dash` y `none`), `List item` → `LI`, marcador → `Lbl`, contenido → `LBody`.
- Volcado del §7: el `Rect` del marcador se vuelca como hijo `Marker` de los ítems con `Id`.

**Tareas:**

- [x] Resolver L1–L8.
- [x] Schema: `List`, `List item`, `Marker`, `Start`, `Number`.
- [x] Línea base de la primera línea: no va en el `LayoutResult`; `MarkerLayout.firstBaseline`
      la saca de los fragmentos hijos ya colocados, con `TextMeasurer.vertical` (las métricas
      verticales del pintado: `hhea` en PDF, 0,8/0,2 en el resto).
- [x] `Style`, `BoxTreeBuilder` (con la numeración y los enums con guion): `Box.marker` es la
      caja generada del marcador (texto con la tipografía del ítem, fuera de `children`);
      `ListMarkers` escribe su texto. Errores del árbol `LIST_ITEM_EMPTY`,
      `LIST_NUMBER_NOT_ALLOWED`, `LIST_NUMBER_RANGE` y `LIST_ITEM_WITHOUT_LINE` (este se
      comprueba en el árbol: bajando por el primer hijo en flujo hay que llegar a un texto),
      con sus casos en el catálogo. Tests en `tree/ListsTest` y `tree/ListMarkersTest`.
- [x] Especificación: §2, §4 (§4.11 nueva), §5.13 (etiquetas), §5.14 nueva, §6, §7, §8 y §9.
- [x] `LIST_MARKER_OVERFLOW` (necesita las métricas: en `Overflow`) con su caso.
- [x] Caja de marcador en el layout (`layout/block/MarkerLayout`, desde `BlockLayout` en el
      primer fragmento del ítem) y en el pintado: es un fragmento hijo del ítem con una línea,
      que se pinta como cualquier texto. `Overflow` lo salta y lo comprueba contra la sangría.
- [x] PDF etiquetado: `L` (con `ListNumbering`), `LI`, `Lbl` y `LBody` (`PdfCanvasTest`).
- [x] Volcado del §7: `Marker` dentro de los ítems con `Id` (`StxtDump`).
- [x] Tests de layout y de fragmentación (`layout/block/ListLayoutTest`); casos de error.
- [x] Caso golden `10-lists` (numeración desde `Start`, marcadores de distinto ancho, lista
      anidada al principio de un ítem, `Number`, y una lista romana partida entre páginas),
      añadido a `DocumentReaderTest`; PNG revisados.
- [x] README, `CLAUDE.md` (tabla de hitos y piezas clave) y `DECISIONES.md` (89–95).

---

## Hito 14: tablas

**Objetivo.** Tablas al estilo HTML: filas y celdas, celdas que ocupan varias filas o
columnas, y una cabecera y un pie que se repiten en cada página cuando la tabla se parte.

**Sintaxis** (nombres decididos en TB1; el contenido de la celda, en TB2):

```
Table:
	Columns: 120 1fr 1fr
	Head:
		Row:
			Cell: Concepto
			Cell: T1
			Cell: T2
	Body:
		Row:
			Cell: Ventas
			Cell: 100
			Cell: 120
	Foot:
		Row:
			Cell: Total
				Span: 1 3
```

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-27, TB2 y TB4–TB10 con la propuesta aceptada):

- [x] TB1. Nombres: **`Table`**, **`Head`**, **`Body`** y **`Foot`** (con `Body` explícito;
      `Header`/`Footer` quedan libres para cabeceras de página), **`Row`** y **`Cell`**.
      Para liberar `Cell`, el envoltorio de los ítems de `Grid` pasa a llamarse **`Area`**
      (el término de CSS, `grid-area`; mismos valores; decisión 96 de `DECISIONES.md`).
      La extensión de una celda es **`Span: filas columnas`**, en el orden del valor de
      `Area` y de `Gap`; no `Column span`, que sería el `column-span` del hito 15.
- [x] TB3. Los anchos se calculan con el **dimensionado de pistas de grid**: `Columns` y
      `Gap` de la tabla son los mismos nodos que en `Grid` (mismo significado y valores).
- [x] TB2. Una **`Cell` es una caja** con dos formas, como `Text`: con valor
      (`Cell: 100`), una caja de texto de una línea; sin valor, un bloque con hijos
      (`Block`, `Flex`, `Grid`, `Text`, `Paragraph`, `List`, `Table`, `Relative`, `Absolute`,
      `Fixed`). Las dos cosas a la vez son un error; `Cell:` sin nada es una celda vacía y se
      permite. Sin `Content` (para texto largo, un `Paragraph` dentro) ni encabezados (como
      `List item`: H9 y los marcadores). Admite `Id`, `Class`, `Box`, `Typography` y `Span`.
- [x] TB4. **Bordes separados**, con el `Box` de cada celda; `Gap` hace de
      `border-spacing`. Las líneas simples se dibujan con bordes de un lado
      (`Border: 0 0 0.5 0`). Bordes colapsados, más adelante.
- [x] TB5. **`Row` no es una caja** (como `Item` y `Area`): admite `Typography`, que heredan
      sus celdas, y `Break`. El estilo de las celdas va en `Element: Cell` o en `Class`. Sin
      estilo por columna; el fondo de fila (filas alternas), más adelante. `Table` y `Cell`
      se pueden nombrar en `Element`; `Head`, `Body`, `Foot` y `Row`, no (R9).
- [x] TB6. **`Head` y `Foot` se repiten siempre** en cada fragmento de la tabla, sin
      propiedad (como la impresión de HTML).
- [x] TB7. **Una fila se corta como las de grid:** si no cabe pasa entera a la página
      siguiente, y solo se parte en paralelo si no cabe ni en una página vacía. Las filas de
      `Head` y `Foot` no se parten nunca.
- [x] TB8. **Sin `caption`:** un `Heading` o un `Paragraph` antes de la tabla.
- [x] TB9. **Errores:** `Columns` es obligatorio en `Table`; cada fila llena exactamente
      sus columnas, contando los `Span` de las filas de arriba; un `Span` no se sale de la
      tabla ni de su grupo (`Head`, `Body`, `Foot`); los grupos van en ese orden, con un solo
      `Body`, obligatorio; que la cabecera, el pie y una fila del cuerpo no quepan en una
      página vacía es un error, no se deja de repetir. Precisado al implementarlo (decisión
      100): el error es cuando la cabecera y el pie no dejan sitio al cuerpo; una fila del
      cuerpo más alta que ese sitio se parte en paralelo (TB7).
- [x] TB10. **PDF etiquetado:** `Table` → `Table`; `Head`/`Body`/`Foot` →
      `THead`/`TBody`/`TFoot`; `Row` → `TR`; `Cell` → `TH` en `Head` y `TD` en el resto, con
      `RowSpan` y `ColSpan`.

**Diseño técnico (previsto):**

- Con grid (TB3): `BoxTreeBuilder` traduce la tabla a un grid con colocación
  explícita (fila y columna salen del orden de `Row` y `Cell`, más los `Span`), y un
  `TableLayout` envuelve `GridLayout` para la repetición. `Placement` y `TrackSizing` no
  cambian.
- Repetición: al partir, cada fragmento de la tabla empieza con las filas de la cabecera y
  termina con las del pie; el espacio de la página que queda para el cuerpo es el restante
  menos el alto de los dos. El `BreakToken.Container` de la tabla solo lleva la posición
  del cuerpo. Se parece a `Decoration: clone`, pero con filas en lugar de borde y relleno.
- Garantía de progreso: si la cabecera, el pie y una fila del cuerpo no caben en una página
  vacía, es un error (TB9).

**Tareas:**

- [x] Resolver TB1 y TB3.
- [x] Renombrar el `Cell` de `Grid` a `Area` (schema, builder, especificación, tests,
      catálogo y casos golden; sin cambios en los resultados ni en los PNG).
- [x] Resolver TB2 y TB4–TB10.
- [x] Schema (`Table`, `Head`, `Body`, `Foot`, `Row`, `Cell`, `Span`; `Table` donde va un
      `Block`) y resumen del §9; `DocumentReaderTest.tablesValidateAgainstTheSchema`.
- [x] `BoxTreeBuilder`: la `Table` es una caja `GRID` y cada `Cell` un ítem con colocación
      explícita (`Style.itemArea`, en la línea de la celda); `Box.headRows` y `Box.footRows`
      dicen qué filas de la rejilla son cabecera y pie. Una `Cell` con valor es `INLINE`; sin
      valor, `BLOCK`. El `Typography` de la `Row` se aplica como herencia (antes de las
      reglas) y su `Break`, a sus celdas. `ErrorCode` nuevos `TABLE_ORDER`,
      `TABLE_ROW_CELLS`, `TABLE_SPAN`, `CELL_CONTENT` y `TABLE_ROW_BREAK`, con sus casos en el
      catálogo; `STYLE_NOT_ALLOWED` cubre `Break` en una `Cell` desde una regla. Tests en
      `tree/TablesTest` y `GridLayoutTest.tableCellsAreGridItems`.
- [x] Especificación: §2, §4 (§4.12 nueva), §5.15 nueva, §6 y §8.
- [x] Layout y repetición de cabecera y pie al fragmentar: `layout/table/TableLayout`
      (despachado por `Engine` para las cajas `GRID` del elemento `table`) maqueta el grid con
      `GridLayout.arrange` y corta solo el cuerpo con `UnitBreaker`, que repite la cabecera y
      el pie enteros (`UnitBreaker.Repeat`). `TABLE_REPEAT_OVERFLOW` (al maquetar) cuando no
      dejan sitio al cuerpo en una página vacía (decisión 100, que precisa TB9). Tests en
      `layout/table/TableFragmentationTest`; caso en el catálogo.
- [x] PDF etiquetado del TB10 en `PdfStructure`: `Table`, `THead`/`TBody`/`TFoot`, `TR`,
      `TH` (con `Scope: Column`) y `TD`, con `RowSpan` y `ColSpan`; la cabecera y el pie
      repetidos son *artifacts* (decisión 103). Test en `PdfCanvasTest`.
- [x] Bordes (según TB4) en el pintado: los de cada celda, sin código nuevo.
- [x] Tests de layout, de fragmentación (cabecera repetida, pie, fila partida) y casos de
      error.
- [x] Especificación §7 y §9 (sin cambios: el volcado no conoce las tablas); caso golden
      `11-tables` (tabla de tres páginas, con una fila de dos líneas y el pie en dos
      columnas), añadido a `DocumentReaderTest`; PNG revisados.
- [x] README (tabla de estado y hoja de ruta), `CLAUDE.md` (tabla de hitos y piezas clave) y
      `DECISIONES.md` (96–103).

---

## Hito 15: bloque en columnas

**Objetivo.** Un bloque cuyo contenido fluye por varias columnas seguidas, como en una
revista: el texto llena la primera columna, sigue en la segunda, y al acabar la página
continúa en la primera columna de la siguiente.

**Sintaxis** (decidida en C1):

```
Multicol:
	Count: 2
	Column gap: 20
	Rule: 0.5
	Rule color: #999999
	Fill: balance
	Text:
		Content >>
			Texto largo que fluye por las dos columnas…
```

**Decisiones abiertas:** *(ninguna)*

**Decisiones tomadas** (2026-10-27, todas con la propuesta aceptada):

- [x] C1. Elemento **`Multicol`**, con **`Count`**, **`Column width`**, **`Column gap`**,
      **`Rule`** y **`Rule color`** (como `Border` y `Border color`) y **`Fill`**. No se
      reutiliza `Gap`, que en `Flex`, `Grid` y `Table` admite dos valores: un nombre
      compartido debe admitir los mismos valores. Admite además `Id`, `Class`, `Box`,
      `Typography` y `Break`, como un bloque.
- [x] C2. **Las dos formas, como en CSS:** `Count` fijo, `Column width` mínimo (tantas
      columnas como quepan) o las dos (`Count` es entonces el máximo). Sin ninguna, error.
- [x] C3. **`Fill: balance | auto`**, `balance` por defecto: con `balance` se equilibra el
      último fragmento (como `column-fill: balance` en CSS); con `auto`, las columnas se
      llenan en orden.
- [x] C4. **Sin `column-span`** en este hito: un título en medio del texto se escribe partiendo
      el `Multicol` en dos.
- [x] C5. **`Before: column` y `After: column`** en `Break`. Fuera de un `Multicol`, error.
- [x] C6. Dentro admite **todo el flujo** (`Block`, `Flex`, `Grid`, `Text`, `Paragraph`,
      encabezados, `List`, `Table`, `Relative` y otro `Multicol`). `Absolute` y `Fixed`
      dentro de un `Multicol`, a cualquier profundidad, son un error.
- [x] C7. Con **`Height` definido**, las columnas se llenan hasta ese alto; lo que no cabe es
      un desbordamiento (`BOX_OVERFLOW`).
- [x] C8. **Línea entre columnas** continua, con grosor (`Rule`) y color (`Rule color`, por
      defecto el `Color` del texto). Solo entre dos columnas con contenido.

**Diseño técnico:**

- **Flujo de columnas.** `BoxTreeBuilder` mete los hijos del `Multicol` en una caja de bloque
  generada (`Element.COLUMNS`, como el marcador de las listas), su único hijo: crea un contexto
  de formato de bloque (los márgenes de dentro no salen) y su alto no es definido (los
  porcentajes verticales de dentro son un error). Cada columna es un fragmento de esa caja:
  `MulticolLayout` la maqueta con `engine.layout` una vez por columna, con el alto de la
  columna como `fragmentainerRemaining` y el token de la columna anterior como `from`. Los
  algoritmos que ya saben partir no cambian, y la caché sirve.
- **Número y ancho de las columnas:** el pseudoalgoritmo de CSS (`column-count`,
  `column-width`) sobre el ancho de contenido.
- **Alto de las columnas** en un fragmento: lo que queda de página para el contenido y, si el
  `Multicol` tiene `Height` (o `Max height`), lo que le queda de él. Se llenan en orden; si el
  contenido termina en este fragmento y `Fill` es `balance`, se busca el alto mínimo en que
  cabe en las columnas (búsqueda binaria repitiendo el layout). Fuera de una página y sin
  alto definido (un ítem de flex, un `Absolute`) no hay nada que llenar: se equilibra siempre.
- **Cortes.** Un corte forzado de columna es un corte forzado más dentro del flujo; uno de
  página debe atravesar las columnas: su calidad es `BreakAppeal.FORCED_PAGE`, por encima de
  `PERFECT_BREAK`, y `MulticolLayout` deja de llenar columnas al verlo. El token del `Multicol`
  es un `BreakToken.Block` con el token del flujo; la página siguiente empieza otra vez en la
  primera columna. Si la primera columna no coloca nada, el `Multicol` corta antes de él; si
  no coloca nada otra columna, se pasa a la página siguiente. La calidad del corte de página
  es la de la última columna.
- **Intrínsecos:** con `Count`, `Count` columnas de min-content o max-content del contenido más
  los huecos; con solo `Column width`, una columna.
- **Pintado:** la línea se pinta con el fondo del `Multicol`, centrada en el hueco y con el
  alto de la columna más alta del fragmento. En el PDF etiquetado, el `Multicol` es un `Div` y
  el flujo de columnas no tiene elemento propio.

**Tareas:**

- [x] Resolver C1–C8.
- [x] Schema, `Element`, `Style`, builder (con el flujo de columnas) y los errores nuevos, con
      sus casos en el catálogo.
- [x] `MulticolLayout` sin equilibrado (llenado en orden), en una y en varias páginas.
- [x] Equilibrado.
- [x] Línea entre columnas en el pintado.
- [x] Tests de layout y de fragmentación; casos de error.
- [x] Especificación; caso golden con texto en dos columnas a lo largo de dos páginas.

---

## Registro de sesiones

Una entrada por sesión, la más reciente arriba: fecha, qué se hizo, dónde se quedó y el
siguiente paso concreto.

- **2026-10-27 (16).** Hito 15 terminado. Aceptadas las propuestas C1–C8. Schema y builder
  (`Multicol` con `Count`, `Column width`, `Column gap`, `Rule`, `Rule color` y `Fill`;
  `column` en `Before` y `After`; el contenido en un flujo de columnas generado; cuatro
  `ErrorCode` nuevos con sus casos), `MulticolLayout` (llenado en orden, equilibrado, cortes de
  columna y de página con `BreakAppeal.FORCED_PAGE`), línea entre columnas y PDF etiquetado.
  Caso golden `12-multicol` de dos páginas, calculado a mano y con los PNG revisados.
  Especificación (§4.13 y §5.16 nuevas; §2, §4.5, §5.1, §5.3, §5.10, §5.12, §6, §7, §8, §9 y
  glosario), README, `CLAUDE.md` y `DECISIONES.md` (110–119). `mvn test` en verde.
  *Siguiente paso:* los hitos de esta hoja de ruta están terminados. Pendiente: repasar las
  decisiones de `DECISIONES.md` y elegir el siguiente trabajo (ideas en `TODO.md`).

- **2026-10-27 (15).** Hito 14 terminado. PDF etiquetado de las tablas (`Table`,
  `THead`/`TBody`/`TFoot`, `TR`, `TH`/`TD` con `RowSpan`/`ColSpan`; la cabecera y el pie
  repetidos, *artifacts*). Caso golden `11-tables` de tres páginas, calculado a mano y con los
  PNG revisados. Especificación §5.15, README, `CLAUDE.md` y `DECISIONES.md` (103).
  `mvn test` en verde.
  *Siguiente paso:* hito 15 (bloque en columnas), resolver C1–C8 (y T3 para `Columns`, que ya
  es la lista de pistas de `Grid` y `Table`).
- **2026-10-27 (14).** Hito 14: `TableLayout` repite la cabecera y el pie en cada fragmento
  (`UnitBreaker.Repeat`; `GridLayout.arrange` separa el layout sin fragmentar del corte), sin
  partirlos, y `TABLE_REPEAT_OVERFLOW` cuando no dejan sitio al cuerpo. Especificación §5.12,
  §5.15 y §6; `DECISIONES.md` (100–102). `mvn test` en verde; los PNG no cambian.
  *Siguiente paso:* PDF etiquetado del TB10 (`Table`, `THead`/`TBody`/`TFoot`, `TR`,
  `TH`/`TD` con `RowSpan`/`ColSpan`; las filas repetidas, *artifact*), el caso golden de tres
  páginas, README y `CLAUDE.md`.
- **2026-10-27 (13).** Hito 14 en curso. `BoxTreeBuilder` traduce la tabla a un grid con
  colocación explícita (`Cell` con valor como texto, sin valor como bloque; `Typography` y
  `Break` de la `Row` a sus celdas) y comprueba los errores del TB9 que no dependen del layout
  (cinco `ErrorCode` nuevos, con sus casos). Especificación §2, §4.12, §5.15, §6 y §8;
  `DECISIONES.md` (97–99). `mvn test` en verde. Una tabla ya se maqueta, como un grid.
  *Siguiente paso:* `TableLayout`, que envuelve `GridLayout` y repite las filas de `Head`
  (arriba) y `Foot` (abajo) en cada fragmento, sin partirlas, y el error de cabecera y pie que
  no caben en una página vacía.
- **2026-10-27 (12).** Hito 14 en diseño. Decididos TB1 (nombres: `Table`, `Head`, `Body`,
  `Foot`, `Row`, `Cell`, `Span`) y TB3 (anchos con las pistas de grid; `Columns` y `Gap`
  reutilizados). Para liberar `Cell`, el envoltorio de `Grid` pasa a llamarse `Area` en
  schema, builder (`Style.itemArea`), especificación, README, `CLAUDE.md`, tests, catálogo y
  casos golden (decisión 96). Aceptadas las propuestas TB2 y TB4–TB10. Hecho el schema de la
  tabla y el resumen del §9; el builder lanza `UnsupportedOperationException` con una
  `Table`. `mvn test` en verde; los PNG no cambian.
  *Siguiente paso:* `BoxTreeBuilder`: traducir la tabla a un grid con colocación explícita
  (`Cell` con valor como caja de texto, sin valor como bloque) y los errores del TB9, con sus
  `ErrorCode` y casos en el catálogo.
- **2026-10-27 (11).** Hito 13 terminado. Caso golden `10-lists`, que pasa con el resultado
  calculado a mano; PNG revisados (marcadores alineados a la derecha, el de una lista anidada
  en la misma línea que el del ítem, y el de un ítem partido solo en la página 1). El marcador
  pasa a ser el primer hijo del fragmento del ítem, para que el volcado lo muestre tras su
  `Rect`. Especificación §8, `DECISIONES.md` (89–95), README y `CLAUDE.md`. `mvn test` en
  verde.
  *Siguiente paso:* hito 14 (tablas), resolver TB1–TB9 (y T3 para `Cell`, `Columns` y
  `Span`).
- **2026-10-27 (10).** Hito 13: el marcador en el layout. `Box.marker` pasa a ser la caja
  generada del marcador; `MarkerLayout` la coloca en el primer fragmento del ítem, a la
  izquierda de su caja de borde y con la línea base de su primera línea (`TextMeasurer.vertical`
  nuevo, con las métricas `hhea` en `PdfMetrics`). `Overflow` comprueba la sangría
  (`LIST_MARKER_OVERFLOW`), `PdfStructure` etiqueta `L`/`LI`/`Lbl`/`LBody` y `StxtDump` vuelca
  `Marker`. Revisado a ojo el PDF de `PdfCanvasTest.listsAreTaggedWithLabelAndBody`.
  `mvn test` en verde.
  *Siguiente paso:* caso golden `10-lists` (listas anidadas, numeraciones y una lista partida
  entre páginas), §8, `DECISIONES.md`, README y `CLAUDE.md` para cerrar el hito.
- **2026-10-27 (9).** Hito 13: `Style` (`Marker`, `listMarker`, `listStart`, `listNumber`),
  `Box.marker`, `ListMarkers` (símbolos, letras, romanos y rangos) y `BoxTreeBuilder` (`List` y
  `List item` como cajas de bloque, numeración, enums con guion como guion bajo, cuatro
  `ErrorCode` nuevos). `CLAUDE.md` recoge la convención de los enums. Especificación §2, §4.11,
  §5.14 y §6. `mvn test` en verde (348 tests). El layout aún no coloca ni pinta el marcador.
  *Siguiente paso:* que el layout de bloque devuelva la línea base de su primera línea, y la
  caja del marcador en `BlockLayout` (colgada a la izquierda del ítem, en su primer fragmento),
  con `LIST_MARKER_OVERFLOW`.
- **2026-10-27 (8).** Hito 13 en curso. Aceptadas las propuestas L1–L8. Hecho el schema
  (`List`, `List item`, `Marker`, `Start`, `Number`; `List` admitido donde va un elemento) y
  el resumen del §9. Un documento con listas valida contra el schema, pero `BoxTreeBuilder`
  todavía no las traduce. `mvn test` en verde.
  *Siguiente paso:* `Style` y `BoxTreeBuilder` (enums con guion, numeración, `LIST_ITEM_EMPTY`,
  `LIST_NUMBER_NOT_ALLOWED`, `LIST_NUMBER_RANGE`) y la línea base de la primera línea en el
  resultado del layout de bloque. Las interpretaciones de L1–L8 van a `DECISIONES.md` al
  cerrar el hito.
- **2026-10-27 (7).** `Title` y `Language` opcionales en `Document` (`tree/Metadata`), escritos
  en el PDF por `PdfStructure.metadata`; casos de error y tests en `PdfCanvasTest`. `mvn test`
  en verde. Especificación: §2, §4.1, §5.13, §6 y §9.
  *Siguiente paso:* hito 13 (listas), resolver L1–L8.
- **2026-10-27 (6).** Hito 12 terminado. Aceptadas las propuestas H4b, H5, H8, P3 y G1–G3;
  H9 al revés (los saltos de nivel son error, y el primero es `Heading 1`) y H10 (encabezado
  en `Fixed`, error). Hechos schema, builder, `PdfStructure` (PDF etiquetado y marcadores),
  tests, catálogo, caso golden `09-headings`, especificación (§5.13 nueva), README,
  `CLAUDE.md` y `DECISIONES.md` (83–88). `mvn test` en verde.
  *Siguiente paso:* hito 13 (listas), resolver L1–L8. Pendiente opcional: usar `Heading 1` en
  el ejemplo del §10 (`report.stxt`).
- **2026-10-27 (5).** Hito 12 en diseño. Decididos H2 (encabezado = bloque con hijos `Text`),
  H4 (el motor evita el corte), H6 (admite `Id`, `Class`, `Box`, `Typography`), P2
  (`Paragraph` = `Text` con significado) y T2 (*outline* y PDF etiquetado en este hito).
  *Siguiente paso:* resolver H4b, H5, H8, H9, P3 y G1–G3 y empezar por el schema.
- **2026-10-27 (4).** Hito 11 terminado. El ejemplo del §10 (`report.stxt` y la copia del
  caso 05) usa `Styles` con el mismo resultado; caso golden `08-styles` nuevo; README,
  `CLAUDE.md` y `DECISIONES.md` (79–82) actualizados. `mvn test` en verde.
  *Siguiente paso:* hito 12, resolver H2, H4, H5 y P2 (en qué se diferencia `Paragraph` de
  `Text`) y T2.
- **2026-10-27 (3).** Hito 11: aceptadas las propuestas S1–S5. Hechos el schema,
  `BoxTreeBuilder` (lectura y aplicación de reglas con su prioridad), los cinco `ErrorCode`
  nuevos con sus casos en el catálogo, `StylesTest`, un test de layout y la especificación
  (§2, §4.2, §4.10, §6, §8, §9). `mvn test` en verde.
  *Siguiente paso:* caso golden con el estilo en `Styles`, decidir si el §10 usa `Styles`, y
  cerrar el hito (README, `CLAUDE.md`, `DECISIONES.md`).
- **2026-10-27 (2).** Se añade el hito 11, reglas reutilizables (`Styles`), y los demás pasan
  a ser del 12 al 15. Decididos los nombres (`Styles`, `Element`, `For class`, `For id`), la
  prioridad, la mezcla por propiedad, la validación estricta y que las clases entran en este
  hito (R1–R11). Resuelve T1. Decidido también que los encabezados son seis elementos
  (`Heading 1`…`Heading 6`) y que habrá `Paragraph`.
  *Siguiente paso:* resolver S1–S5 del hito 11 y empezar por el schema y `BoxTreeBuilder`.
- **2026-10-27.** Se crea esta hoja de ruta. Antes, en la misma sesión, se eliminó el modo
  continuo (decisión 78 de `DECISIONES.md`): el medio es siempre paginado.
  *Siguiente paso:* resolver las decisiones transversales T1–T4 y las del hito de encabezados (H1–H5; entonces era el 11, hoy el 12).
