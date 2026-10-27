# Hoja de ruta: elementos de documento

Plan de trabajo para añadir al motor los elementos de documento: encabezados, listas, tablas y
bloques en columnas. Es el punto de partida de cada sesión: el estado, las decisiones tomadas y
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
| 11 | Encabezados (`h1`…`h6`) | pendiente | — |
| 12 | Listas (`ul`, `ol`, `li`) | pendiente | — |
| 13 | Tablas, con cabecera y pie repetidos | pendiente | — |
| 14 | Bloque en columnas (multicolumna) | pendiente | — |

**Orden propuesto**, de menor a mayor impacto en el motor:

- **Encabezados** primero: son casi un `Text` con significado. Sirven para fijar cómo se
  comporta un elemento *semántico* (valores por defecto, marcadores en el PDF), y el resto de
  hitos reutiliza esa decisión.
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

Afectan a varios hitos. Hay que resolverlas en el hito 11, o en el primero que las necesite.

**Abiertas:**

- **T1. Valores por defecto de los elementos semánticos.** No hay cascada ni hoja de estilos
  del agente de usuario. Hay tres opciones para que un `h1` sea más grande que un `h2`:
  (a) ningún estilo implícito: el autor declara `Typography` en cada encabezado;
  (b) valores por defecto fijos en el motor (como `Widows: 2`);
  (c) estilos por nivel declarados una vez en el `Document` (por ejemplo, un grupo
  `Headings` con `Level 1`, `Level 2`…), que se heredan como `Typography`.
  Con (a) o (c) se respeta la política de "nada implícito" del §6.
- **T2. Significado en la salida.** Qué produce el significado de un elemento, además del
  layout: marcadores del PDF (*outline*) para los encabezados, PDF etiquetado (*tagged PDF*,
  accesibilidad) para todos, y el volcado del §7 (si incluye el tipo de elemento).
  Propuesta: *outline* en el hito 11 y PDF etiquetado fuera de alcance por ahora.
- **T3. Nombres que chocan.** `Item` (hijo de `Flex`), `Cell` (hijo de `Grid`) y `Columns`
  (pistas de `Grid`) ya existen y tienen una única definición en el namespace. Las listas,
  las tablas y las columnas tienen que usar otros nombres o compartir el significado. Se
  decide en cada hito; ver sus decisiones abiertas.
- **T4. Texto con formato dentro de una línea.** Hoy un `Text` tiene un único estilo. Un
  encabezado o un ítem de lista con una palabra en negrita necesitaría tramos (*spans*).
  Propuesta: fuera de alcance de estos hitos; si hace falta, sería un hito propio.

**Tomadas:** *(ninguna todavía)*

---

## Hito 11: encabezados

**Objetivo.** Un elemento de texto con nivel (1 a 6), equivalente a `h1`…`h6`, que no se separa
del contenido que le sigue y que aparece en los marcadores del PDF.

**Borrador de sintaxis** (a confirmar):

```
Heading: Resultados del trimestre
	Level: 1
	Id: results
```

**Decisiones abiertas:**

- [ ] H1. ¿Un elemento con `Level` (`Heading` + `Level: 1`) o seis elementos (`H1`…`H6`)?
- [ ] H2. ¿Es un `Text` con otro nombre (mismas propiedades, texto en el valor o en
      `Content`) o puede contener otros elementos?
- [ ] H3. Estilo por nivel: ver T1.
- [ ] H4. `Break After: avoid` implícito (un encabezado no se queda solo al final de la
      página). ¿Por defecto, o lo declara el autor?
- [ ] H5. Marcadores del PDF: ver T2. ¿Hace falta una propiedad para excluir un encabezado
      del índice?

**Decisiones tomadas:** *(ninguna todavía)*

**Diseño técnico (previsto):**

- `BoxTreeBuilder` lo traduce a una `Box` de texto (`Layout.INLINE`) con el nivel en `Style`
  (o en `Box`); el layout es el de `InlineLayout`, sin algoritmo nuevo.
- Marcadores: `PdfCanvas` recoge los encabezados al pintar (página y posición del primer
  fragmento) y construye el *outline* de PDFBox (`PDDocumentOutline`) anidando por nivel.

**Tareas:**

- [ ] Resolver H1–H5 (y T1, T2).
- [ ] Schema, `Style`, `BoxTreeBuilder`.
- [ ] Marcadores en `PdfCanvas`, con su test en `PdfCanvasTest`.
- [ ] Especificación y casos de error (nivel fuera de 1–6…).
- [ ] Caso golden.

---

## Hito 12: listas

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

## Hito 13: tablas

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

## Hito 14: bloque en columnas

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

- **2026-10-27.** Se crea esta hoja de ruta. Antes, en la misma sesión, se eliminó el modo
  continuo (decisión 78 de `DECISIONES.md`): el medio es siempre paginado.
  *Siguiente paso:* resolver las decisiones transversales T1–T4 y las del hito 11 (H1–H5).
