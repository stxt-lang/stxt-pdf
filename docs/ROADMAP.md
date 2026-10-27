# Hoja de ruta: elementos de documento

Plan de trabajo para añadir al motor las reglas de estilo reutilizables y los elementos de
documento: encabezados y párrafos, listas, tablas y bloques en columnas. Es el punto de partida de cada sesión: el estado, las decisiones tomadas y
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
| 12 | Encabezados (`Heading 1`…`Heading 6`) y párrafos (`Paragraph`) | pendiente | — |
| 13 | Listas (`ul`, `ol`, `li`) | pendiente | — |
| 14 | Tablas, con cabecera y pie repetidos | pendiente | — |
| 15 | Bloque en columnas (multicolumna) | pendiente | — |

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

- **T2. Significado en la salida.** Qué produce el significado de un elemento, además del
  layout: marcadores del PDF (*outline*) para los encabezados, PDF etiquetado (*tagged PDF*,
  accesibilidad) para todos, y el volcado del §7 (si incluye el tipo de elemento).
  Propuesta: *outline* en el hito 12 y PDF etiquetado fuera de alcance por ahora.
- **T3. Nombres que chocan.** `Item` (hijo de `Flex`), `Cell` (hijo de `Grid`) y `Columns`
  (pistas de `Grid`) ya existen y tienen una única definición en el namespace. Las listas,
  las tablas y las columnas tienen que usar otros nombres o compartir el significado. Se
  decide en cada hito; ver sus decisiones abiertas.
- **T4. Texto con formato dentro de una línea.** Hoy un `Text` tiene un único estilo. Un
  encabezado o un ítem de lista con una palabra en negrita necesitaría tramos (*spans*).
  Propuesta: fuera de alcance de estos hitos; si hace falta, sería un hito propio.

**Tomadas:**

- **T1. Valores por defecto de los elementos semánticos** (2026-10-27). No hay estilos
  implícitos en el motor: el estilo de cada tipo de elemento (un `Heading 1` más grande que un
  `Heading 2`) se declara una vez en las reglas `Styles` del documento (hito 11). Se respeta
  la política de "nada implícito" del §6.

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

**Borrador de sintaxis** (a confirmar):

```
Heading 1: Resultados del trimestre
	Id: results
Paragraph:
	Content >>
		Texto del párrafo.
```

**Decisiones tomadas** (2026-10-27):

- [x] H1. **Seis elementos**, `Heading 1` … `Heading 6`, no un `Heading` con `Level`: así una
      regla los nombra directamente (`Element: Heading 1`).
- [x] H3. El estilo de cada nivel se declara en `Styles` (T1); el motor no tiene estilos
      implícitos.
- [x] P1. Habrá un elemento **`Paragraph`**.

**Decisiones abiertas:**

- [ ] H2. ¿Un encabezado es un `Text` con otro nombre (mismas propiedades, texto en el valor
      o en `Content`) o puede contener otros elementos?
- [ ] H4. `Break After: avoid` (un encabezado no se queda solo al final de la página): con
      `Styles` lo puede declarar el autor (`Element: Heading 1` / `Break` / `After: avoid`).
      ¿Basta, o el motor lo aplica siempre?
- [ ] H5. Marcadores del PDF: ver T2. ¿Hace falta una propiedad para excluir un encabezado
      del índice?
- [ ] P2. ¿En qué se diferencia `Paragraph` de `Text`? ¿`Text` sigue existiendo como caja de
      texto sin significado, o `Paragraph` lo sustituye? ¿Un `Content` con líneas en blanco
      sigue dando varios párrafos dentro de un `Paragraph`?

**Diseño técnico (previsto):**

- `BoxTreeBuilder` los traduce a una `Box` de texto (`Layout.INLINE`) con su tipo de
  elemento (para las reglas y los marcadores); el layout es el de `InlineLayout`, sin
  algoritmo nuevo.
- Marcadores: `PdfCanvas` recoge los encabezados al pintar (página y posición del primer
  fragmento) y construye el *outline* de PDFBox (`PDDocumentOutline`) anidando por nivel.

**Tareas:**

- [ ] Resolver H2, H4, H5, P2 (y T2).
- [ ] Schema, `Style`, `BoxTreeBuilder`.
- [ ] Marcadores en `PdfCanvas`, con su test en `PdfCanvasTest`.
- [ ] Especificación y casos de error.
- [ ] Caso golden.

---

## Hito 13: listas

**Objetivo.** Listas sin numerar y numeradas (`ul`, `ol`, `li`), anidables, con el marcador
fuera del contenido y alineado con la primera línea del ítem.

**Borrador de sintaxis** (a confirmar; el nombre del ítem no puede ser `Item`, ver T3):

```
List:
	Marker: decimal
	Start: 1
	Entry:
		Text: Primer punto
	Entry:
		Text: Segundo punto
		List:
			Marker: disc
			Entry:
				Text: Subpunto
```

**Decisiones abiertas:**

- [ ] L1. Nombres: `List` + (`Entry` | `Point` | `Li`…). ¿Un solo `List` con `Marker`, o
      `Bullets` y `Numbered` por separado?
- [ ] L2. ¿El ítem contiene exactamente un elemento (como `Item`/`Cell`) o varios
      (como un `Block`)? Un `li` de HTML admite varios párrafos.
- [ ] L3. Tipos de marcador: `disc`, `circle`, `square`, `dash`, `none`, `decimal`,
      `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman`. ¿Marcador libre (un texto
      cualquiera, `Marker: "→"`)? ¿Sufijo (`1.` frente a `1)`)?
- [ ] L4. Posición del marcador: solo fuera (`outside`) o también dentro (`inside`).
- [ ] L5. Sangría: ¿propiedad propia (`Indent`) o el `Padding` del ítem? ¿Ancho del marcador
      automático (el del marcador más ancho de la lista) o fijo?
- [ ] L6. Numeración: `Start`, orden inverso, y si un ítem puede fijar su número (`Value`).
- [ ] L7. Tipografía del marcador: la del ítem, o una propia (`Marker color`…).
- [ ] L8. Errores: un ítem vacío, un marcador que no cabe en la sangría.

**Decisiones tomadas:** *(ninguna todavía)*

**Diseño técnico (previsto):**

- La lista es un flujo de bloque (`BlockLayout`); cada ítem, una caja de bloque con una caja
  de marcador generada (no está en el documento) colocada en la sangría.
- El marcador se alinea con la línea base de la primera línea del ítem: hace falta que el
  layout de un bloque devuelva la línea base de su primera línea (hoy no la devuelve).
- Fragmentación: el marcador va en el primer fragmento del ítem; si el ítem se parte, los
  siguientes no lo repiten. Huérfanas: no separar el marcador de su primera línea.
- Numeración: se resuelve al construir el árbol (`BoxTreeBuilder`), no en el layout.
- Volcado del §7: decidir si el marcador aparece (sin `Id`, no se vuelca hoy).

**Tareas:**

- [ ] Resolver L1–L8.
- [ ] Línea base de la primera línea en el resultado del layout de bloque.
- [ ] Schema, `Style`, `BoxTreeBuilder` (con la numeración).
- [ ] Caja de marcador en el layout y en el pintado.
- [ ] Tests de layout y de fragmentación; casos de error.
- [ ] Especificación; caso golden con listas anidadas y una lista partida entre páginas.

---

## Hito 14: tablas

**Objetivo.** Tablas al estilo HTML: filas y celdas, celdas que ocupan varias filas o
columnas, y una cabecera y un pie que se repiten en cada página cuando la tabla se parte.

**Borrador de sintaxis** (a confirmar; `Cell` y `Columns` ya existen, ver T3):

```
Table:
	Columns: 120 1fr 1fr
	Head:
		Row:
			Data: Concepto
			Data: T1
			Data: T2
	Body:
		Row:
			Data: Ventas
			Data: 100
			Data: 120
	Foot:
		Row:
			Data: Total
				Span: 1 3
```

**Decisiones abiertas:**

- [ ] TB1. Nombres: `Table`, `Head`/`Body`/`Foot`, `Row`, y la celda (`Data`, `Td`,
      `Field`…; no puede ser `Cell` salvo que se unifique con la de `Grid`).
- [ ] TB2. Contenido de la celda: texto en el valor (`Data: Ventas`) y, además, elementos
      (un `Block` con varios párrafos, una lista…).
- [ ] TB3. Algoritmo de anchos: el de HTML (`table-layout: auto`, basado en el contenido) o
      el dimensionado de pistas de grid (`Columns` con `auto`, `fr`, `minmax`…). Reutilizar
      grid es más simple y coherente con el resto del motor.
- [ ] TB4. Bordes: separados (cada celda con su `Box`) o colapsados (`border-collapse`).
      Bordes colapsados es lo típico en tablas editoriales, pero es un modelo nuevo.
- [ ] TB5. Estilo por fila, por columna o para toda la tabla (filas alternas con fondo):
      ¿solo en cada celda o con herencia desde `Row`/`Table`? `Box` no se hereda (§4).
- [ ] TB6. Repetición: ¿cabecera y pie siempre se repiten, o con una propiedad
      (`Repeat: true`)? ¿El pie se repite en todas las páginas o solo al final?
- [ ] TB7. Corte dentro de una fila: ¿una fila puede partirse entre páginas (como las filas
      de grid, en paralelo) o una fila no se parte nunca?
- [ ] TB8. Título (`caption`): ¿hace falta, o basta con un `Heading`/`Text` antes?
- [ ] TB9. Errores: filas con distinto número de celdas, `Span` que se sale de la tabla,
      cabecera y pie que no caben en una página.

**Decisiones tomadas:** *(ninguna todavía)*

**Diseño técnico (previsto):**

- Si se reutiliza grid (TB3): `BoxTreeBuilder` traduce la tabla a un grid con colocación
  explícita (fila y columna salen del orden de `Row` y `Data`, más los `Span`), y un
  `TableLayout` envuelve `GridLayout` para la repetición. `Placement` y `TrackSizing` no
  cambian.
- Repetición: al partir, cada fragmento de la tabla empieza con las filas de la cabecera y
  termina con las del pie; el espacio de la página que queda para el cuerpo es el restante
  menos el alto de los dos. El `BreakToken.Container` de la tabla solo lleva la posición
  del cuerpo. Se parece a `Decoration: clone`, pero con filas en lugar de borde y relleno.
- Garantía de progreso: si la cabecera, el pie y una fila del cuerpo no caben en una página
  vacía, se deja de repetir (o es un error, según TB9).

**Tareas:**

- [ ] Resolver TB1–TB9.
- [ ] Schema, `Style`, `BoxTreeBuilder` (traducción a filas y columnas, `Span`).
- [ ] Layout y repetición de cabecera y pie al fragmentar.
- [ ] Bordes (según TB4) en el pintado.
- [ ] Tests de layout, de fragmentación (cabecera repetida, pie, fila partida) y casos de
      error.
- [ ] Especificación; caso golden con una tabla de tres páginas.

---

## Hito 15: bloque en columnas

**Objetivo.** Un bloque cuyo contenido fluye por varias columnas seguidas, como en una
revista: el texto llena la primera columna, sigue en la segunda, y al acabar la página
continúa en la primera columna de la siguiente.

**Borrador de sintaxis** (a confirmar; `Columns` ya es la propiedad de pistas de `Grid`):

```
Multicol:
	Count: 2
	Gap: 20
	Rule: 0.5 #999
	Text:
		Content >>
			Texto largo que fluye por las dos columnas…
```

**Decisiones abiertas:**

- [ ] C1. Nombre del elemento (`Multicol`, `Columned`, `Article`…) y de sus propiedades
      (`Count`, `Column width`, `Gap` —reutilizable si admite lo mismo—, `Rule`).
- [ ] C2. ¿Número fijo de columnas, ancho mínimo de columna (`column-width`: tantas como
      quepan) o los dos?
- [ ] C3. Equilibrado: ¿se igualan las columnas al final del bloque (`balance`, por defecto
      en CSS) o se llenan en orden (`auto`)? ¿Configurable?
- [ ] C4. Elementos que ocupan todas las columnas (`column-span: all`), como un título en
      medio del texto. Complica bastante el algoritmo.
- [ ] C5. Cortes de columna: ¿`Before: column` en `Break`, además de `page`?
- [ ] C6. Qué elementos admite dentro: ¿todos los de flujo, o solo texto, encabezados y
      listas? ¿`Absolute`/`Fixed` dentro de columnas?
- [ ] C7. `Height` del bloque: con alto definido, las columnas se llenan hasta ese alto y lo
      que sobra es error o pasa a la siguiente página.
- [ ] C8. Línea entre columnas (`column-rule`): grosor, color, ¿estilo?

**Decisiones tomadas:** *(ninguna todavía)*

**Diseño técnico (previsto):**

- Algoritmo nuevo `layout/multicol/MulticolLayout`. Cada columna es un fragmentainer anidado:
  el contenido se maqueta como un flujo de bloque con `fragmentainerRemaining` igual al alto
  de la columna, y el `BreakToken` de una columna es el `from` de la siguiente. Los
  algoritmos existentes ya saben partir, así que no cambian.
- Alto de las columnas en una página: el que queda en la página. Con equilibrado, la
  búsqueda del alto mínimo en que todo cabe en las columnas disponibles (búsqueda binaria
  sobre el alto, repitiendo el layout; la `LayoutCache` ayuda).
- Al acabar la página, el token de la última columna pasa al `BreakToken` del bloque
  multicolumna y la siguiente página empieza otra vez en la primera columna.
- Intrínsecos: min-content = el de la columna × número de columnas + huecos.
- Calidad del corte: el corte de página del bloque es el de su última columna; las reglas de
  viudas y huérfanas se aplican en cada columna.

**Tareas:**

- [ ] Resolver C1–C8.
- [ ] `MulticolLayout` sin equilibrado (llenado en orden), en una y en varias páginas.
- [ ] Equilibrado.
- [ ] `column-span` (si se decide en C4).
- [ ] Línea entre columnas en el pintado.
- [ ] Tests de layout y de fragmentación; casos de error.
- [ ] Especificación; caso golden con texto en dos columnas a lo largo de dos páginas.

---

## Registro de sesiones

Una entrada por sesión, la más reciente arriba: fecha, qué se hizo, dónde se quedó y el
siguiente paso concreto.

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
