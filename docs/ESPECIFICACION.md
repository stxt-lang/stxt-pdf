# Especificación del modelo de cajas en STXT

**Versión:** 0.1 · **Namespace:** `dev.stxt.boxes` · **Unidades:** puntos tipográficos (pt)

Esta especificación es la referencia de uso del motor de cajas: describe cómo se escribe un
documento, qué significa cada propiedad y cómo calcula el motor la posición y el tamaño de
cada caja. Describe el modelo completo; el estado de implementación de cada parte está en el
[`README`](../README.md).

## Índice

1. [Introducción](#1-introducción)
2. [Estructura de un documento](#2-estructura-de-un-documento)
3. [Valores](#3-valores)
4. [Referencia de propiedades](#4-referencia-de-propiedades)
5. [Semántica del layout](#5-semántica-del-layout)
6. [Validación](#6-validación)
7. [Formato de resultados](#7-formato-de-resultados)
8. [Diferencias con CSS](#8-diferencias-con-css)
9. [Schema](#9-schema)
10. [Ejemplo completo](#10-ejemplo-completo)
11. [Glosario](#11-glosario)

---

## 1. Introducción

### 1.1 Qué es

Un documento describe un **árbol de cajas** con elementos (`Block`, `Flex`, `Grid`, `Text`…)
que solo admiten las propiedades que tienen sentido en ellos. El motor calcula la posición y el
tamaño de cada caja dentro de un **medio**: una secuencia de páginas de tamaño fijo, como un
PDF. Las cajas que no caben en una página se parten entre páginas.

El modelo es un subconjunto de los mecanismos de maquetación de CSS (flujo de bloque, texto,
posicionamiento, flex y grid, fragmentación) sin HTML y sin cascada. Las diferencias se
resumen en el [§8](#8-diferencias-con-css).

### 1.2 Convenciones

- **Todas las medidas están en puntos tipográficos** (1 pt = 1/72 de pulgada). Un A4 mide
  595 × 842 pt.
- **Los nombres de nodos y valores están en inglés** y siguen, cuando existe, el término de CSS:
  `Margin`, `Padding`, `Absolute`, `Decoration: slice`. El texto de un `Text` puede estar en
  cualquier idioma.
- **Los ejes:** *horizontal* es el de `Width` e *izquierda/derecha*; *vertical* es el de
  `Height` y *arriba/abajo*. La escritura es siempre horizontal, de izquierda a derecha.
- **Las cuatro cajas de una caja** (ver [§5.2](#52-modelo-de-caja-y-containing-block)): de
  contenido, de relleno, de borde y de margen.
- **Caja de referencia:** salvo que se diga otra cosa, las posiciones y los tamaños se refieren
  a la **caja de borde**.

---

## 2. Estructura de un documento

Un documento se escribe con **elementos**, y cada elemento solo admite los hijos que tienen
sentido en él: sus propiedades y, si es un contenedor, otros elementos. El schema
([§9](#9-schema)) define qué admite cada uno, así que una propiedad fuera de su sitio (`Columns`
en un `Flex`, `Offset` en un `Block`) es un error de estructura que se detecta al leer, también
en el editor.

El nodo raíz es `Document`, con exactamente un `Media`, un `Title` y un `Language` opcionales
(el título y el idioma del documento, [§4.1](#41-media)), un `Typography` opcional que heredan
todas las cajas (la fuente del documento), un `Styles` opcional con las reglas reutilizables
([§4.10](#410-reglas-reutilizables-styles)) y los elementos de primer nivel.

| Elemento | Qué es | Contiene |
|---|---|---|
| `Block` | Caja que apila sus hijos en vertical ([§5.3](#53-flujo-de-bloque)) | Elementos |
| `Flex` | Contenedor flex ([§5.7](#57-flex)) | `Item`, `Absolute` y `Fixed` |
| `Grid` | Contenedor grid ([§5.8](#58-grid)) | `Area`, `Absolute` y `Fixed` |
| `Text` | Caja de texto ([§5.4](#54-texto)) | Su texto: en el valor o en `Content` |
| `Paragraph` | Un párrafo: un `Text` con significado ([§5.13](#513-encabezados-y-párrafos)) | Su texto: en el valor o en `Content`, sin líneas en blanco |
| `Heading 1` … `Heading 6` | Encabezado de nivel 1 a 6 ([§5.13](#513-encabezados-y-párrafos)) | Uno o varios `Text` |
| `List` | Lista con marcadores o numerada ([§5.14](#514-listas)) | Uno o varios `List item` |
| `List item` | Ítem de una `List`: un bloque con su marcador | Elementos, salvo los encabezados |
| `Table` | Tabla, con cabecera y pie que se repiten en cada página ([§5.15](#515-tablas)) | `Head`, `Body` y `Foot`, en ese orden |
| `Head`, `Body`, `Foot` | Cabecera, cuerpo y pie de una `Table` | Una o varias `Row` |
| `Row` | Fila de una tabla | Una o varias `Cell` |
| `Cell` | Celda de una fila: un texto o un bloque | Su texto en el valor, o elementos salvo los encabezados |
| `Multicol` | Bloque cuyo contenido fluye por varias columnas ([§5.16](#516-bloques-en-columnas)) | Elementos en flujo |
| `Markdown` | Bloque con contenido escrito en Markdown, traducido a encabezados, párrafos, listas, tablas e imágenes, con enlaces en sus textos ([§5.17](#517-markdown)) | Su texto en `Content` |
| `Image` | Imagen PNG o JPEG de la carpeta de recursos, con el tamaño y la proporción del fichero ([§5.18](#518-imágenes)) | Nada: la ruta del fichero va en el valor |
| `Contents` | Índice automático: una entrada por encabezado, con su página ([§5.19](#519-referencias-a-otras-cajas)) | Nada: las entradas las genera el motor |
| `Relative` | Bloque que se desplaza con `Offset` sin mover a sus hermanos ([§5.5](#55-posicionamiento)) | Elementos |
| `Absolute` | Bloque fuera de flujo, colocado con `Offset` en su containing block | Elementos |
| `Fixed` | Bloque fuera de flujo, colocado en el área útil y repetido en las páginas que dice `Pages` (todas por defecto) | Elementos |
| `Item` | Ítem de un `Flex`: cómo crece, encoge y se alinea | Exactamente un elemento en flujo |
| `Area` | Ítem de un `Grid`: el área que ocupa y cómo se alinea en ella | Exactamente un elemento en flujo |

**Elementos** son todos menos `Item`, `Area`, `Head`, `Body`, `Foot` y `Row`, y **elementos en
flujo**, los que pueden ser un ítem (`Block`, `Flex`, `Grid`, `Text`, `Paragraph`, los
encabezados, `List`, `Table`, `Multicol`, `Markdown`, `Contents`, `Image` y `Relative`); `Absolute` y `Fixed` salen del flujo y nunca son
ítems. `List item` solo va dentro de una `List`, y `Cell`, dentro de una `Row`. Todos son **elementos de caja**: cada uno es una caja
del árbol ([§5.1](#51-árbol-de-cajas)). `Item` y `Area` no son cajas: envuelven al elemento que hace de
ítem y le añaden lo que solo tiene sentido dentro de su contenedor. `Head`, `Body`, `Foot` y
`Row` tampoco: agrupan las celdas de una tabla, que sí son cajas.

Las propiedades de una caja van agrupadas por su significado ([§4](#4-referencia-de-propiedades)):
`Box` (el modelo de caja), `Typography` (el texto, que se hereda) y `Break` (los cortes de
página).

```
Document (dev.stxt.boxes):
	Media:
		Width: 595
		Height: 842
		Margin: 40
	Typography:
		Font: IBM Plex Sans
		Size: 10
	Text: Un título
		Id: title
		Typography:
			Size: 20
	Flex:
		Id: body
		Gap: 10
		Item:
			Grow: 1
			Text:
				Content >>
					Primer párrafo.

					Segundo párrafo.
		Item:
			Block:
				Box:
					Width: 100
					Background: #F1EFE8
```

**El orden importa.** STXT conserva el orden de los nodos, y el motor coloca los elementos en el
orden en que aparecen. El orden de las propiedades, y su posición respecto a los elementos, no
importan.

**Texto en línea o en bloque.** `Text: una línea` lleva el texto en su valor; para párrafos y
texto largo, el texto va en un bloque `Content >>`, sin caracteres de escape. Un `Text` tiene uno
de los dos, nunca ambos.

---

## 3. Valores

### 3.1 Tipos de valor

| Sintaxis | Significado | Ejemplo |
|---|---|---|
| Número | Puntos. Punto decimal, nunca coma. Puede ser negativo donde se indique. | `12`, `12.5`, `-30` |
| Porcentaje | Porcentaje de una base (ver [§3.6](#36-bases-de-los-porcentajes)). | `50%` |
| Fracción | Fracción del espacio libre. Solo en pistas de grid. | `1fr`, `2.5fr` |
| `auto` | El motor calcula el valor según el contexto. | `auto` |
| `min-content` | El menor tamaño posible sin desbordar (la palabra más larga). | `min-content` |
| `max-content` | El tamaño sin ningún salto de línea. | `max-content` |
| `fit-content` | `min(max-content, max(min-content, disponible))`. | `fit-content` |
| `minmax(a, b)` | Pista de grid entre `a` y `b`. `a` no puede ser fracción. | `minmax(100, 1fr)` |
| `min(a, b, …)` | El menor de los valores. | `min(300, 50%)` |
| `max(a, b, …)` | El mayor de los valores. | `max(200, 30%)` |
| `clamp(a, b, c)` | `b`, pero nunca menos que `a` ni más que `c`. | `clamp(200, 50%, 400)` |

Cada propiedad admite solo algunas de estas formas (la columna *Valores* del §4); cualquier
otra es un error: `Width: 1fr`, `Max width: max-content`, `Gap: auto`. Los argumentos de
`min()`, `max()` y `clamp()` son números o porcentajes. Solo `Margin` y `Offset` admiten
valores negativos.

Un porcentaje cuya base es un alto que no es definido (por ejemplo, el `Height: 50%` de una
caja cuyo padre tiene alto `auto`) es un error. Ver [§3.6](#36-bases-de-los-porcentajes).

### 3.2 Listas y `repeat`

Algunas propiedades admiten varios valores separados por espacios (`Columns`, `Rows`,
`Margin`…). En las listas de pistas, `repeat(n, patrón)` repite un patrón:

```
Columns: 150 repeat(3, 1fr) minmax(100, 2fr)
```

equivale a `150 1fr 1fr 1fr minmax(100, 2fr)`.

### 3.3 Abreviados de cuatro lados

`Margin`, `Padding`, `Border` y `Offset` aceptan de 1 a 4 valores en el orden
**arriba, derecha, abajo, izquierda**:

| Valor | Arriba | Derecha | Abajo | Izquierda |
|---|---|---|---|---|
| `10` | 10 | 10 | 10 | 10 |
| `10 20` | 10 | 20 | 10 | 20 |
| `10 20 5` | 10 | 20 | 5 | 20 |
| `10 20 5 0` | 10 | 20 | 5 | 0 |

### 3.4 Colores

`#RGB` o `#RRGGBB` en hexadecimal: `#000`, `#A32D2D`.

### 3.5 Booleanos y enteros

Los booleanos se escriben `true` o `false`. Los enteros, sin decimales: `Layer: -1`, `Widows: 3`.

### 3.6 Bases de los porcentajes

| Propiedad | Porcentaje de… |
|---|---|
| `Width`, `Min width`, `Max width` | el ancho del containing block |
| `Height`, `Min height`, `Max height` | el alto del containing block, que **debe ser definido** |
| `Margin`, `Padding` (los cuatro lados) | el **ancho** del containing block, también arriba y abajo |
| `Offset` arriba/abajo | el alto del containing block, que **debe ser definido** |
| `Offset` izquierda/derecha | el ancho del containing block |
| `Basis` (ítem flex) | el tamaño del contenedor flex en el eje principal (en `column`, su alto, que debe ser definido) |
| `Gap` | el tamaño de la caja de contenido del contenedor en ese eje (entre filas, su alto, que debe ser definido) |
| Pistas de grid | el tamaño de la caja de contenido del contenedor en ese eje, sin descontar los huecos (`50% 50%` con `Gap` desborda, y es un error); las filas, su alto, que debe ser definido |
| `Line height` | el `Size` de la propia caja |

`Border` no admite porcentajes.

**Alto definido.** Los anchos se conocen siempre al maquetar; los altos, no. Un porcentaje
vertical es un error si su base no es un **alto definido**, que se decide al leer el documento:

- el área útil;
- el de una caja con `Height` en puntos, o en porcentaje de un alto definido;
- el de una caja con `Height: auto` y `Aspect ratio`;
- el de un `Absolute` o un `Fixed` con `Height: auto` estirado entre arriba y abajo;
- el de un ítem de grid con `Height: auto` estirado en su área (`Vertical: stretch`).

La base de `Height`, `Min height` y `Max height` de un `Absolute` o un `Fixed` es su
containing block, y la de un ítem de grid, su área: las dos son siempre definidas. Un ítem de
flex estirado **no** da un alto definido a sus hijos: si lo necesitan, declara su `Height`.

---

## 4. Referencia de propiedades

Casi todas las propiedades van en uno de tres **grupos**, hijos de cada elemento de caja:

| Grupo | Contenido | Se hereda | Elementos |
|---|---|---|---|
| `Box` ([§4.3](#43-box)) | El modelo de caja: tamaño, márgenes, relleno, borde y fondo | No | Todos los de caja |
| `Typography` ([§4.4](#44-typography)) | Fuente, tamaño, interlineado, alineación y color del texto, viudas y huérfanas | **Sí** | Todos los de caja salvo `Image`, y `Document` |
| `Break` ([§4.5](#45-break)) | Cortes de página y decoración en los cortes | No | Todos los de caja salvo `Absolute` y `Fixed` |

Lo que hay en `Typography` lo hereda toda la descendencia; lo de `Box` y `Break` es solo de la
caja que lo declara. No hay cascada: las reglas de `Styles`
([§4.10](#410-reglas-reutilizables-styles)) dan valores por defecto a los elementos de un tipo,
de una clase o con un `Id`, y el elemento los puede sobrescribir. Dentro de cada grupo, una propiedad aparece como mucho una
vez, y los grupos, como mucho uno por elemento. Fuera de los grupos quedan las propiedades que
definen el propio elemento: `Id`, `Class` e `Isolate` ([§4.2](#42-propiedades-del-elemento)), las de
`Flex` y `Grid` ([§4.6](#46-flex), [§4.7](#47-grid)), las de `Item` y `Area`
([§4.8](#48-item-y-area)), las de posicionamiento ([§4.9](#49-posicionamiento)), las de las
listas ([§4.11](#411-listas)), las de las tablas ([§4.12](#412-tablas)), las de los bloques en
columnas ([§4.13](#413-bloques-en-columnas)), las de `Markdown` ([§4.14](#414-markdown)), las de las
imágenes ([§4.15](#415-imágenes)) y las del índice ([§4.16](#416-índice)).

```
Text: Resultados del trimestre
	Id: title
	Box:
		Margin: 0 0 16 0
	Typography:
		Weight: bold
		Size: 20
	Break:
		After: avoid
```

### 4.1 `Media`

Obligatorio, exactamente uno, hijo de `Document`.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Width` | número > 0 | — (obligatorio) | Ancho de la página. |
| `Height` | número > 0 | — (obligatorio) | Alto de la página. |
| `Margin` | 1 a 4 números | `0` | Márgenes de página. Solo puntos. |

El **área útil** es el rectángulo de la página menos sus márgenes. Es el containing block de los
elementos de primer nivel y de los `Fixed`, y su alto es siempre definido.

**Título e idioma.** `Document` admite también, como mucho uno de cada, `Title` (un texto no
vacío) y `Language` (una etiqueta BCP 47: `es`, `en-US`, `ca`…; si no, `VALUE_NOT_ALLOWED`). No
afectan al layout: van al PDF ([§5.13](#513-encabezados-y-párrafos)) como título del documento
(información, metadatos XMP y el que muestra el visor) e idioma del contenido etiquetado. Son
opcionales; sin ellos, el PDF no los declara.

### 4.2 Propiedades del elemento

| Propiedad | Valores | Por defecto | Elementos | Notas |
|---|---|---|---|---|
| `Id` | texto | — | De caja | Identificador para volcados, tests, mensajes y reglas `For id`. Único en el documento. |
| `Class` | nombres separados por espacios | — | De caja | Clases del elemento: le aplican las reglas `For class` con esos nombres ([§4.10](#410-reglas-reutilizables-styles)). Cada clase necesita su regla y no se repite. |
| `Link` | `https://…`, `http://…`, `mailto:…` o `#id` | — | Todos los elementos de caja salvo `Markdown` y `Contents` | Hace clicable la caja entera: lleva a una URL o a la caja con ese `Id` ([§5.20](#520-enlaces)). No se hereda y no cambia el aspecto. |
| `Isolate` | booleano | `false` | `Block`, `Relative` | Crea un contexto de formato de bloque nuevo: corta el colapso de márgenes con los hijos. Los demás elementos ya lo crean o no tienen hijos en flujo de bloque. |

### 4.3 `Box`

El modelo de caja. No se hereda.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Width` | número, %, `auto`, intrínsecos, funciones | `auto` | |
| `Height` | número, %, `auto`, intrínsecos, funciones | `auto` | |
| `Min width`, `Min height` | número, %, `auto` | `auto` | `auto` = min-content en ítems de flex y grid; 0 en el resto. |
| `Max width`, `Max height` | número, % | sin máximo | |
| `Aspect ratio` | `ancho/alto` o número | — | Si un eje es `auto` y el otro es conocido, se deduce. Ejemplo: `16/9`. Con `Width` y `Height` declarados es un error. |
| `Margin` | 1 a 4 valores (número, %, `auto`) | `0` | Puede ser negativo. `auto` solo centra: en flujo de bloque, en horizontal y con `Width` o `Max width`; en `Absolute` y `Fixed`, con inicio, tamaño y fin declarados. En cualquier otro caso es un error. |
| `Padding` | 1 a 4 valores (número ≥ 0, % ≥ 0) | `0` | |
| `Border` | 1 a 4 números ≥ 0 | `0` | Grosor. |
| `Border color` | color | el `Color` del texto | No afecta al layout. |
| `Background` | color | transparente | Se pinta en la caja de relleno. No afecta al layout. |

### 4.4 `Typography`

El texto y cómo se reparte en líneas. **Se hereda**: lo que declara un elemento vale para él y
para todos sus descendientes que no lo cambien. También se admite en `Document`, y es lo
habitual para la fuente del documento: cada elemento declara solo lo que cambia.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Font` | nombre de familia | — | **Obligatoria** en todo `Text`, propia o heredada (también de `Document`). Es la familia que declara el fichero TTF de la carpeta de fuentes. Ver [§5.11](#511-recursos-y-fuentes). |
| `Weight` | `normal`, `bold` | `normal` | Peso: `normal` es el 400 de la fuente y `bold`, el 700. |
| `Style` | `normal`, `italic` | `normal` | |
| `Size` | número > 0 | — | Cuerpo de la fuente. **Obligatorio** en todo `Text`, propio o heredado (también de `Document`). |
| `Line height` | número o % | `120%` | **Un número son puntos** (`15` = 15 pt). Para un valor relativo, usa porcentaje (`120%`). |
| `Align` | `left`, `right`, `center`, `justify` | `left` | La última línea de un párrafo justificado se alinea a la izquierda. |
| `Color` | color | `#000000` | Color del texto. |
| `Text wrap` | `auto`, `pretty` | `auto` | Reparto en líneas: voraz (`auto`) o Knuth-Plass (`pretty`). Ver [§5.4](#54-texto). |
| `Widows` | entero ≥ 1 | `2` | Mínimo de líneas de un párrafo que pasan a la página siguiente. Ver [§5.10](#510-fragmentación). |
| `Orphans` | entero ≥ 1 | `2` | Mínimo de líneas de un párrafo que quedan al final de una página. |

`Text wrap` conserva el nombre largo porque `Wrap` ya es una propiedad de `Flex`, y un nombre
es el mismo nodo en todo el schema.

**El texto de un `Text`** va en su valor (`Text: Un título`) o en un hijo `Content >>` con un
bloque indentado, que admite párrafos. Uno de los dos es obligatorio, y no se admiten ambos. Ver
[§5.4](#54-texto).

### 4.5 `Break`

Los cortes de página. No se heredan. No se admite en
`Absolute` ni en `Fixed`, que no se parten ni están en el flujo de la página.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Before` | `auto`, `page`, `column`, `avoid` | `auto` | `page` fuerza un salto de página antes de la caja; `column`, un salto a la columna siguiente (solo dentro de un `Multicol`, [§5.16](#516-bloques-en-columnas)). |
| `After` | `auto`, `page`, `column`, `avoid` | `auto` | `avoid` mantiene la caja con la siguiente (títulos). |
| `Inside` | `auto`, `avoid` | `auto` | `avoid` intenta no partir la caja. |
| `Decoration` | `slice`, `clone` | `slice` | Qué pasa con borde y relleno en un corte. |

### 4.6 `Flex`

Propiedades del elemento `Flex`, hijas directas suyas. Sus hijos en flujo son `Item`
([§4.8](#48-item-y-area)).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Direction` | `row`, `column` | `row` | Eje principal horizontal o vertical. |
| `Wrap` | booleano | `false` | Permite repartir los ítems en varias líneas. |
| `Gap` | 1 o 2 valores (número, %) | `0` | Un valor para ambos ejes, o `filas columnas`. |
| `Main axis` | `start`, `end`, `center`, `between`, `around`, `evenly` | `start` | Reparto del sobrante en el eje principal. |
| `Cross axis` | `start`, `end`, `center`, `stretch` | `stretch` | Alineación de los ítems dentro de su línea. |
| `Lines` | `start`, `end`, `center`, `between`, `around`, `stretch` | `stretch` | Reparto de las líneas en el contenedor. Solo con `Wrap: true`. |

### 4.7 `Grid`

Propiedades del elemento `Grid`, hijas directas suyas. Sus hijos en flujo son `Area`
([§4.8](#48-item-y-area)).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Columns` | lista de pistas | una columna `auto` | Ver [§5.8](#58-grid). |
| `Rows` | lista de pistas | filas implícitas `auto` | |
| `Gap` | 1 o 2 valores (número, %) | `0` | Un valor para ambos ejes, o `filas columnas`. |
| `Areas` | bloque de texto | — | Una fila por línea; nombres separados por espacios; `.` = celda vacía. |
| `Flow` | `row`, `column` | `row` | Dirección de la colocación automática. |
| `Horizontal` | `start`, `end`, `center`, `stretch` | `stretch` | Alineación horizontal de los ítems en su celda. |
| `Vertical` | `start`, `end`, `center`, `stretch` | `stretch` | Alineación vertical de los ítems en su celda. |

Pistas válidas: número, %, `auto`, `min-content`, `max-content`, fracción, `minmax(a, b)` y
`repeat(n, patrón)`.

### 4.8 `Item` y `Area`

Cada hijo en flujo de un `Flex` es un `Item`, y cada uno de un `Grid`, un `Area`. Contienen
**exactamente un elemento en flujo** (`Block`, `Flex`, `Grid`, `Text` o `Relative`), que es el
ítem: su `Box`, su `Typography` y demás propiedades van en ese elemento. El `Item` o el `Area`
solo añade cómo participa en su contenedor. No son cajas y no admiten `Id`.

```
Flex:
	Item:
		Grow: 1
		Text: Crece
	Item:
		Block:
			Box:
				Width: 100
Grid:
	Columns: 1fr 1fr
	Area: 1 2
		Text: Fila 1, columna 2
	Area:
		Text: Se coloca automáticamente
```

| Elemento | Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|---|
| `Item` | `Grow` | número ≥ 0 | `0` | Parte del espacio sobrante que recibe. |
| `Item` | `Shrink` | número ≥ 0 | `1` | Parte del déficit que cede, ponderada por `Basis`. |
| `Item` | `Basis` | número, %, `auto` | `auto` | Tamaño de partida en el eje principal. |
| `Item` | `Cross axis` | como en `Flex` | el del contenedor | Sobrescribe la alineación del contenedor. |
| `Area` | *(valor)* | área o `fila columna [filas columnas]` | `auto` | Dónde va, contando desde 1. Sin valor, se coloca automáticamente. Ver [§5.8](#58-grid). |
| `Area` | `Horizontal` | como en `Grid` | el del contenedor | |
| `Area` | `Vertical` | como en `Grid` | el del contenedor | |
| Los dos | `Layer` | entero | `auto` | Orden de apilamiento del ítem. Ver [§5.6](#56-apilamiento-y-orden-de-pintado). |

### 4.9 Posicionamiento

Propiedades de `Relative`, `Absolute` y `Fixed`, los **elementos posicionados**, hijas directas
suyas. Son bloques (apilan sus hijos como un `Block`); para posicionar un flex o un grid, se pone
dentro.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Offset` | 1 a 4 valores (número, %, `auto`) | `auto` | Arriba, derecha, abajo, izquierda. Ver [§5.5](#55-posicionamiento). |
| `Layer` | entero | `auto` | Orden de apilamiento. También en `Item` y `Area`. Ver [§5.6](#56-apilamiento-y-orden-de-pintado). |
| `Pages` | `all`, `first`, `not-first`, `odd`, `even`, `last` | `all` | Solo en `Fixed`: las páginas en que sale. Ver [§5.5](#55-posicionamiento). |

### 4.10 Reglas reutilizables (`Styles`)

`Styles`, hijo opcional de `Document` (como mucho uno), declara una vez propiedades que se
aplican por defecto a muchos elementos. Contiene reglas de cuatro clases:

| Regla | Valor | Se aplica a |
|---|---|---|
| `Element` | nombre de un elemento de caja (`Block`, `Text`, `Heading 1`…) | todos los elementos de ese tipo |
| `Element` | una posición en una tabla: `Head cell`, `Body cell`, `Foot cell`, `Odd row cell` o `Even row cell` | las celdas de una `Table` en esa posición ([§5.15](#515-tablas)) |
| `For class` | un nombre, sin espacios | los elementos con ese nombre en su `Class` |
| `For id` | un `Id`, sin espacios | el elemento con ese `Id` |
| `For column` | una columna (`3`) o un rango de columnas (`2 4`, ambas incluidas) | las celdas de una `Table` que **empiezan** en esas columnas ([§5.15](#515-tablas)) |

Cada regla admite los grupos `Box`, `Typography` y `Break`, como mucho uno de cada; no admite
las propiedades propias de un elemento (`Gap`, `Columns`, `Offset`…).

```
Styles:
	Element: Text
		Typography:
			Align: justify
	For class: note
		Box:
			Background: #F1EFE8
			Padding: 8
	For id: title
		Typography:
			Size: 20
			Weight: bold
Text: Resultados del trimestre
	Id: title
	Typography:
		Align: left
Text: Una nota al margen.
	Class: note
```

**Prioridad.** Cada propiedad toma, de más a menos fuerte:

1. lo que declara el propio elemento;
2. la regla `For id` de su `Id`;
3. las reglas `For class` de sus clases; si varias declaran la misma propiedad, gana la que va
   **después en `Styles`**, no el orden de `Class`;
4. las reglas `For column` de la columna de una celda, con el mismo criterio de orden;
5. las reglas de posición de una celda (`Head cell`, `Body cell`, `Foot cell`, y después
   `Odd row cell` o `Even row cell`);
6. el `Column align` de su tabla, solo para `Align` ([§5.15](#515-tablas));
7. la regla `Element` de su tipo (`Cell`);
8. lo heredado del padre (solo `Typography`);
9. el valor por defecto.

Las reglas de los puntos 4 a 6 solo se aplican a las celdas de una `Table`.

Una regla gana a la herencia: con `Element: Text` y `Size: 10`, un `Text` mide 10 aunque su
padre declare `Size: 20`. Las reglas se mezclan **propiedad a propiedad**, no por grupo: si la
regla declara `Size` y el elemento `Weight`, se aplican las dos. Lo que una regla da a un
elemento en `Typography` lo heredan sus descendientes, como si lo hubiera declarado él.
`Item` y `Area` no son cajas: las reglas se aplican al elemento que envuelven. Tampoco `Head`,
`Body`, `Foot` y `Row`, que no se pueden nombrar en `Element` (para dar estilo a las celdas de
cada grupo están las reglas de posición); `Table` y `Cell`, sí.

**Validación.** Lo que aporta una regla se valida igual que si lo declarara el elemento
([§6](#6-validación)), y el error señala la línea de la propiedad dentro de la regla. Los valores
de una regla se comprueban aunque no se aplique a ningún elemento. Son errores: un `Element` que
no nombra un elemento de caja ni una posición en una tabla, un `For id` sin elemento con ese `Id`,
un `For column` que no es una columna o un rango de menor a mayor, dos reglas con el mismo
selector (también dos `For column` con el mismo rango), una regla que da `Break` a un elemento que no lo admite (`Absolute`, `Fixed`, un
encabezado, un `Text` de un encabezado o una celda, también con una regla de posición o de columna,
[§5.13](#513-encabezados-y-párrafos)), y una clase de `Class` sin su regla `For class`. Un
`Element`, un `For class` o un `For column` que no se aplican a ningún elemento no son errores: así
las reglas se pueden reutilizar.

El `Typography` de `Document` sigue siendo lo que hereda la raíz; las reglas no se aplican a
`Document`.

### 4.11 Listas

| Propiedad | Valores | Por defecto | Elementos | Notas |
|---|---|---|---|---|
| `Marker` | `disc`, `dash`, `none`, `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | — (obligatorio) | `List` | Marcador de los ítems: `•`, `–`, ninguno, o una numeración (`1.`, `a.`, `A.`, `i.`, `I.`). |
| `Start` | entero | `1` | `List` | Número del primer ítem. Solo en las numeraciones. |
| `Number` | entero | — | `List item` | Número de este ítem; los siguientes continúan desde él. Solo en las numeraciones. |

No van en las reglas de `Styles`, que solo admiten los grupos. La sangría de los ítems es el
`Padding` izquierdo de la `List` ([§5.14](#514-listas)).

### 4.12 Tablas

| Propiedad | Valores | Por defecto | Elementos | Notas |
|---|---|---|---|---|
| `Columns` | lista de pistas | — (obligatorio) | `Table` | Las columnas, como en `Grid` ([§4.7](#47-grid)). |
| `Column align` | una alineación por columna: `left`, `center`, `right` o `justify` | — | `Table` | La alineación por defecto de las celdas de cada columna ([§5.15](#515-tablas)). Tantas como `Columns`. |
| `Gap` | 1 o 2 longitudes (filas columnas) | `0` | `Table` | Separación entre celdas, como en `Grid`. |
| `Span` | `filas columnas`, dos enteros positivos | `1 1` | `Cell` | Filas y columnas que ocupa la celda ([§5.15](#515-tablas)). |

Una `Row` admite `Typography`, que heredan sus celdas, y `Break`, que es el de sus celdas; no
tiene `Box`. Las filas de la tabla no tienen propiedad de tamaño: miden lo que su celda más
alta.

### 4.13 Bloques en columnas

Propiedades del elemento `Multicol`, hijas directas suyas ([§5.16](#516-bloques-en-columnas)).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Count` | entero ≥ 1 | — | Número de columnas; con `Column width`, el máximo. |
| `Column width` | número > 0 | — | Ancho mínimo de las columnas: caben tantas como permita el ancho. Solo puntos. |
| `Column gap` | número ≥ 0 | `0` | Separación entre columnas. Solo puntos. |
| `Rule` | número > 0 | sin línea | Grosor de la línea entre columnas, centrada en el hueco. No pasa de `Column gap`. |
| `Rule color` | color | el `Color` del texto | No afecta al layout. |
| `Fill` | `balance`, `auto` | `balance` | `balance` iguala las columnas del último fragmento; `auto` las llena en orden. |

`Count`, `Column width` o los dos son obligatorios. No van en las reglas de `Styles`, que solo
admiten los grupos.

### 4.14 Markdown

Propiedades del elemento `Markdown`, hijas directas suyas ([§5.17](#517-markdown)). Admite
además `Id`, `Class`, `Box`, `Typography` y `Break`, como un `Block`.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Content` | bloque `>>` | — | El Markdown. Obligatorio. |
| `Bullet` | lista de `disc`, `dash`, `none` | — | El `Marker` de las listas con viñetas (`-`, `*`, `+`), uno por nivel de anidamiento; el último sirve para los niveles más profundos. Obligatorio si hay listas con viñetas. |
| `Numbering` | lista de `decimal`, `lower-alpha`, `upper-alpha`, `lower-roman`, `upper-roman` | — | Igual, para las listas numeradas (`1.`, `1)`). Obligatorio si las hay. |
| `Table columns` | `equal`, `auto` | — | Las `Columns` de las tablas: `equal`, `repeat(n, 1fr)`; `auto`, `repeat(n, auto)`. Obligatorio si hay tablas. |

No van en las reglas de `Styles`, que solo admiten los grupos.

### 4.15 Imágenes

Propiedades del elemento `Image`, hijas directas suyas ([§5.18](#518-imágenes)). Admite además
`Id`, `Class`, `Box` y `Break` (sin `Inside` ni `Decoration`, porque no se parte); no admite
`Typography`, porque no tiene texto.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| *(valor)* | ruta relativa a `images/` | — (obligatorio) | El fichero PNG o JPEG: `Image: logos/acme.png`. |
| `Alt` | texto no vacío | — | Texto alternativo: con él, la imagen es una figura del PDF etiquetado; sin él, es decorativa. |
| `Fit` | `contain`, `cover`, `fill` | `contain` | Cómo llena la imagen su caja de contenido si tienen proporciones distintas. |

No van en las reglas de `Styles`, que solo admiten los grupos.

### 4.16 Índice

Propiedades del elemento `Contents`, hijas directas suyas ([§5.19](#519-referencias-a-otras-cajas)).
Admite además `Id`, `Class`, `Box`, `Typography` y `Break`, como un `Block`, y no tiene hijos:
sus entradas las genera el motor.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Levels` | dos enteros de 1 a 6: el primer y el último nivel | `1 3` | Los encabezados que lista: `Levels: 1 2` lista los `Heading 1` y `Heading 2`. Sin ningún encabezado en esos niveles es un error. |
| `Leader` | `dots`, `rule`, `none` | `dots` | Lo que llena el hueco entre el título y la página de cada entrada: puntos, nada o una línea. |

Las entradas se estilan con reglas `Element: Contents entry 1` … `Contents entry 6`, una por nivel
de encabezado ([§4.10](#410-reglas-reutilizables-styles)).

No van en las reglas de `Styles`, que solo admiten los grupos.

---

## 5. Semántica del layout

### 5.1 Árbol de cajas

El motor transforma el documento en un **árbol de cajas**: cada elemento de caja es una caja, y
el área útil es la raíz. `Item` y `Area` no crean caja: sus propiedades pasan al elemento que
envuelven. No hay cajas anónimas, porque el texto va siempre en su propio `Text`.

Cada elemento determina **cómo coloca a sus hijos**:

| Elemento | Coloca a sus hijos en flujo |
|---|---|
| `Document` (el área útil), `Block`, `Relative`, `Absolute`, `Fixed`, `Heading 1` … `Heading 6` | En flujo de bloque ([§5.3](#53-flujo-de-bloque)) |
| `Flex` | En líneas flex ([§5.7](#57-flex)) |
| `Grid` | En una rejilla ([§5.8](#58-grid)) |
| `Multicol` | En columnas, cada una en flujo de bloque ([§5.16](#516-bloques-en-columnas)) |
| `Text`, `Paragraph` | No tiene hijos: reparte su texto en líneas ([§5.4](#54-texto)) |
| `Image` | No tiene hijos: su contenido es la imagen ([§5.18](#518-imágenes)) |

**Participación.** Cómo participa una caja en su padre lo decide el padre: dentro de un bloque
es un bloque apilado; dentro de un `Flex` o un `Grid` (envuelta en `Item` o `Area`), un ítem.
La excepción son `Absolute` y `Fixed`, que salen del flujo del padre. Un `Relative` participa
como cualquier caja en flujo y después se desplaza.

### 5.2 Modelo de caja y containing block

Cada caja tiene cuatro rectángulos concéntricos: **contenido**, **relleno** (contenido +
`Padding`), **borde** (relleno + `Border`) y **margen** (borde + `Margin`).

`Width` y `Height` (y sus mínimos y máximos, y `Basis`) miden siempre la **caja de borde**: una
caja con `Width: 200` ocupa 200 pt antes de los márgenes, con el relleno y el borde dentro.

El **containing block** de una caja es el rectángulo de referencia para porcentajes, `auto` y
`Offset`:

| Caja | Containing block |
|---|---|
| En flujo (también `Relative`) | caja de **contenido** del padre |
| `Absolute` | caja de **relleno** del elemento posicionado (`Relative`, `Absolute` o `Fixed`) más cercano entre sus ancestros; si no hay ninguno, el área útil |
| `Fixed` | área útil de cada página |

### 5.3 Flujo de bloque

En un `Block` (y en el área útil, `Relative`, `Absolute` y `Fixed`), los hijos en flujo se
apilan en vertical:

1. **Ancho.** Con `Width: auto`, la caja ocupa el ancho del containing block menos sus márgenes
   horizontales. Con ancho conocido y los dos márgenes horizontales `auto`, el sobrante se
   reparte entre ambos y la caja queda **centrada**; si solo uno es `auto`, absorbe el
   sobrante.
2. **Posición.** La caja se coloca en la posición del cursor vertical, tras aplicar su margen
   superior colapsado con el anterior.
3. **Altura.** Se calcula su layout y el cursor avanza.

Con `Height: auto`, la altura del contenedor es la posición final del cursor más su relleno y su
borde. **El ancho se propaga de padres a hijos; la altura, de hijos a padres.**

**Colapso de márgenes.** Los márgenes **verticales** adyacentes en flujo de bloque se fusionan:

- entre hermanos consecutivos;
- entre un padre y su primer o último hijo, si el padre no tiene borde ni relleno en ese lado
  y no crea un contexto de formato nuevo (el margen del hijo "sale" del padre);
- a través de cajas vacías (sin altura, borde, relleno ni contenido).

El resultado de colapsar varios márgenes es **el mayor positivo más el más negativo**:
`20` y `30` dan 30; `20` y `−5` dan 15; `−10` y `−4` dan −10.

**Contextos de formato de bloque.** Aíslan el colapso de márgenes. Los crean: el área útil,
`Absolute` y `Fixed`, los ítems de flex y grid, las columnas de un `Multicol` y los `Block` y
`Relative` con `Isolate: true`.
Los márgenes de los ítems de flex y grid nunca colapsan.

### 5.4 Texto

El texto de un `Text` se reparte en **líneas** del ancho de su caja de contenido:

- **Espacios.** Las secuencias de espacios y los saltos de línea simples equivalen a un espacio.
  Una **línea en blanco** separa párrafos y fuerza un salto.
- **Reparto.** Con `Text wrap: auto`, algoritmo voraz: las palabras se añaden a la línea
  mientras quepan. Con `Text wrap: pretty`, **Knuth-Plass**: se eligen los saltos del párrafo
  que minimizan la suma de los deméritos de sus líneas, de modo que las líneas quedan más
  parejas (sin guiones; los espacios se estiran hasta la mitad de su ancho y no se encogen; la
  última línea no cuenta). En los dos casos, una palabra más ancha que la línea es un error
  ([§5.12](#512-desbordamiento)).
- **Altura.** Cada línea mide `Line height`. La altura del contenido es el número de líneas
  por `Line height`.
- **Línea base.** La caja de la fuente (su ascendente más su descendente, de la tabla `hhea`
  del fichero TTF) se centra en la línea, como el *half-leading* de CSS:
  `línea base = arriba + (Line height − (ascendente + descendente) · Size) / 2 + ascendente · Size`.
  No afecta al layout, solo al pintado.
- **Alineación.** `left`, `right` y `center` desplazan cada línea; `justify` reparte
  el sobrante a partes iguales entre los espacios de la línea. No se justifican la última
  línea de cada párrafo, las líneas de una sola palabra ni las que desbordan (los espacios no
  se comprimen). La última línea de una página no es la última de su párrafo si este continúa
  en la página siguiente, así que se justifica.
- **Tamaños intrínsecos.** min-content = la palabra más ancha; max-content = el párrafo más
  largo en una sola línea.
- **Numeración de páginas.** `{page}` y `{pages}` en el texto se sustituyen por el número de
  página y el total de páginas. Ver [§5.10](#510-fragmentación). `{page of: id}` da la página de
  otra caja ([§5.19](#519-referencias-a-otras-cajas)).
- **Tramos.** El texto puede tener partes con formato dentro de una misma línea: **negrita**,
  *cursiva*, subrayado y tachado, combinables. Cada tramo mide y se pinta con su variante de la
  fuente; la negrita y la cursiva se suman a las de la caja (una palabra en negrita de un texto
  ya en negrita sigue en negrita). La familia, el tamaño, el color y `Line height` son los de la
  caja, así que la altura de las líneas y su línea base no cambian. Una palabra con partes de
  varios tramos (**neg**rita) es una sola palabra para el reparto, y mide lo que suman sus
  partes; el espacio entre dos palabras tiene el formato del tramo en que está su primer espacio
  en blanco. Con `justify`, el espacio extra de cada espacio pertenece a su tramo. Un texto sin
  formato es un único tramo. Un `Text` no tiene sintaxis para escribirlos: los genera el
  elemento `Markdown` ([§5.17](#517-markdown)). Un tramo puede llevar además el destino de un
  enlace ([§5.20](#520-enlaces)), que no cambia cómo se mide ni cómo se pinta.
- **Subrayado y tachado.** Una raya con el color del texto, a lo ancho del tramo (con sus
  espacios, pero no el espacio entre dos tramos que no la llevan). Su posición y su grosor son
  los de la variante del tramo: `underlinePosition` y `underlineThickness` de la tabla `post`
  para el subrayado y `yStrikeoutPosition` y `yStrikeoutSize` de la tabla `OS/2` para el
  tachado, medidos desde la línea base hasta la parte de arriba de la raya. El subrayado se
  pinta antes que el texto y el tachado después. En el PDF etiquetado, los tramos son contenido
  de su elemento (`P`, `Span`, `H1`…) sin elemento propio, y las rayas son *artifacts*.

### 5.5 Posicionamiento

Los tres elementos posicionados son bloques: apilan a sus hijos en flujo de bloque. Para
posicionar un flex o un grid, se pone dentro de uno de ellos.

**`Relative`.** La caja se coloca como cualquier caja en flujo y después se desplaza visualmente
según `Offset`. Su hueco original se conserva y sus hermanos no se mueven. Declarar a la vez
arriba y abajo, o izquierda y derecha, es un error: deja uno de los dos en `auto`. Un
`Relative` sin `Offset` no se desplaza y sirve como containing block de sus descendientes
`Absolute`. Los porcentajes del `Offset` se resuelven contra el ancho o el alto del containing
block; uno vertical necesita que el alto del containing block sea definido
([§3.6](#36-bases-de-los-porcentajes)).

**`Absolute`.** La caja sale del flujo: no ocupa espacio ni contribuye a la altura `auto` de su
padre. En cada eje se cumple:

```
inicio + margen inicio + tamaño + margen fin + fin = tamaño del containing block
```

donde *inicio/fin* son izquierda/derecha o arriba/abajo del `Offset`. Según qué
términos sean `auto`:

| `auto` | Resultado |
|---|---|
| inicio, tamaño y fin | inicio = **posición estática**; tamaño automático; se despeja fin |
| ninguno, con ambos márgenes `auto` | los márgenes se reparten el sobrante: **centrada** |
| ninguno, sin márgenes `auto` | **error**: la posición se fija dos veces |
| inicio y tamaño | tamaño automático; se despeja inicio (anclada al fin) |
| inicio y fin | inicio = posición estática; se despeja fin |
| tamaño y fin | tamaño automático; se despeja fin |
| solo inicio | se despeja inicio |
| solo tamaño | se despeja: la caja **se estira** entre inicio y fin |
| solo fin | se despeja fin |

Un margen `auto` solo tiene sentido cuando inicio, tamaño y fin están declarados (centra la
caja o, si solo es `auto` uno de los dos, absorbe el sobrante); en los demás casos es un error.
El **tamaño automático** es
el ajuste al contenido (`fit-content`) en horizontal y la altura del contenido en vertical. La
**posición estática** es donde habría quedado la caja si estuviera en flujo:

- en flujo de bloque, el cursor vertical más los márgenes colapsados de las cajas anteriores,
  sin colapsar con el margen superior de la propia caja, que se suma después según la ecuación;
- dentro de un contenedor flex o grid, la esquina superior izquierda de su caja de contenido.

**`Fixed`.** Como `Absolute`, pero su containing block es el área útil, y **se pinta en
las páginas que dice `Pages`**, todas por defecto (cabeceras, pies, marcas de agua). Los `Fixed` no
reservan espacio: el autor debe dejar sitio con el `Margin` del medio. Para colocarlos en el margen de
la página se usan desplazamientos negativos (`Offset: -30 0 auto 0`). Si un `Fixed` depende de
su posición estática (desplazamientos `auto`), se toma la de la primera página en la que
aparece el flujo que lo contiene y se repite igual en todas en las que sale.

**`Pages`.** Propiedad solo del `Fixed` (en otro elemento es un error del schema, y un valor que no
está en la lista, también). Cuenta las páginas desde la 1 en el **orden físico** del documento, con
el mismo número que `{page}` ([§5.4](#54-texto)):

| Valor | Páginas |
|---|---|
| `all` | todas (por defecto) |
| `first` | solo la primera |
| `not-first` | todas menos la primera |
| `odd` / `even` | las impares / las pares |
| `last` | solo la última |

Es una condición de pintado, no de espacio: un `Fixed` no reserva sitio ni donde sale ni donde no,
así que el flujo es el mismo con él o sin él. La primera página con otro margen, o una numeración
que no cuente la portada, no existen: una portada a sangre sigue siendo un `Absolute` y la
numeración, la física. Un `Fixed` que no sale en ninguna página (`not-first` en un documento de una
sola) es válido y no se comprueba (`BOX_OVERFLOW` solo mira los que salen). Si un `Fixed` contiene
otro, el de dentro sale solo donde salen los dos, cada uno con su `Pages`.

**Orden de cálculo.** Las cajas posicionadas fuera de flujo se resuelven cuando su containing
block ya conoce su tamaño, después del layout en flujo.

### 5.6 Apilamiento y orden de pintado

El pintado se organiza en **contextos de apilamiento**. Crean uno: el área útil, los `Fixed`,
los `Relative` y `Absolute` con `Layer` declarada y los ítems de flex o grid con `Layer`
declarada en su `Item` o su `Area`.

Dentro de cada contexto, el orden de pintado de abajo arriba es:

1. Fondo y borde de la caja que crea el contexto.
2. Contextos hijos con `Layer` negativa, de menor a mayor.
3. Cajas en flujo en orden del árbol: primero todos los fondos y bordes, después todo el texto.
4. Cajas posicionadas sin `Layer` o con `Layer: 0`, en orden del árbol.
5. Contextos hijos con `Layer` positiva, de menor a mayor.

A igual `Layer`, se pinta en orden del árbol. El orden del árbol es el del documento: un
`Absolute` se pinta en su sitio entre sus hermanos aunque su containing block esté más arriba.
**`Layer` solo compara dentro del mismo contexto:**
el contenido de un contexto se pinta como un bloque al nivel de ese contexto, aunque sus hijos
tengan `Layer` mayores.

### 5.7 Flex

En un `Flex`, los ítems (el elemento de cada `Item`) se colocan en el **eje principal**
(`Direction`) y se alinean en el **eje cruzado**.

**Algoritmo:**

1. Espacio disponible: la caja de contenido del contenedor en el eje principal.
2. **Base** de cada ítem: `Basis` si tiene valor; si es `auto`, el tamaño declarado en el eje
   principal; si tampoco lo hay, su max-content.
3. **Tamaño hipotético:** la base limitada por el mínimo y el máximo del ítem. Con
   `Min width: auto` (`Min height: auto` en `column`), el mínimo es el **tamaño de su
   contenido**: el min-content en `row` y el alto del contenido en `column`, sin pasar del
   máximo del ítem y nunca mayor que su `Width` (o `Height`) declarado.
4. **Líneas:** sin `Wrap`, una sola. Con `Wrap`, los ítems se añaden a la línea mientras
   sus tamaños hipotéticos y los huecos quepan.
5. **Tamaños flexibles** (por línea), ver abajo.
6. **Tamaño cruzado:** layout de cada ítem con su tamaño principal final (en `row`, el texto se
   redistribuye con el ancho final). La línea mide lo que el ítem más alto; sin `Wrap` y con
   tamaño cruzado definido en el contenedor, la única línea mide lo que el contenedor.
7. **Estirar:** los ítems con `stretch` y sin tamaño cruzado declarado pasan a medir lo que la
   línea, y se repite su layout. En `column`, si el contenedor tiene una sola línea, estos
   ítems se miden desde el principio con el ancho del contenedor; con `Wrap`, se miden con su
   ajuste al contenido y después se estiran al ancho de su línea sin recalcular su alto.
8. **Alinear** en el eje principal, en el cruzado y las líneas en el contenedor.

Con tamaño cruzado `auto`, el contenedor mide la suma de sus líneas más los huecos.

**Reparto del espacio.** Espacio libre = disponible − suma de tamaños hipotéticos − huecos.

- **Positivo:** cada ítem recibe una parte proporcional a su `Grow`.
- **Negativo:** cada ítem cede una parte proporcional a **`Shrink × Basis`**.
- Un ítem que choca con su mínimo o máximo se **congela** en ese límite y el reparto se repite
  entre los demás.
- Si la suma de factores de los ítems no congelados es menor que 1, solo se reparte esa
  fracción del espacio libre.

**Alineación en el eje principal** (sobrante `L`, `n` ítems; solo actúa si nadie crece):

| `Main axis` | Antes del primero | Entre ítems | Después del último |
|---|---|---|---|
| `start` | 0 | hueco | L |
| `end` | L | hueco | 0 |
| `center` | L/2 | hueco | L/2 |
| `between` | 0 | hueco + L/(n−1) | 0 |
| `around` | L/2n | hueco + L/n | L/2n |
| `evenly` | L/(n+1) | hueco + L/(n+1) | L/(n+1) |

Un sobrante negativo después de encoger significa que los ítems no caben: es un error
([§5.12](#512-desbordamiento)). Los márgenes `auto` de un
ítem son un error: la alineación se hace con `Main axis` y `Cross axis`.

**Comportamientos a tener en cuenta:**

- En `column`, `Grow` necesita que el contenedor tenga un alto definido ([§3.6](#36-bases-de-los-porcentajes)):
  si no, no hay espacio libre que repartir y es un error.
- `Basis: 0` con `Grow: 1` reparte todo el espacio (columnas iguales); `Basis: auto` con
  `Grow: 1` solo reparte el sobrante sobre el tamaño del contenido.
- Un ítem no encoge por debajo del min-content de su contenido salvo que declare
  `Min width: 0`. Un `Width` declarado no lo frena: un ítem con `Width: 400` y poco contenido
  encoge si no cabe.

### 5.8 Grid

En un `Grid`, cuyos ítems son el elemento de cada `Area`, el motor resuelve primero **dónde** va
cada ítem y después **cuánto** mide cada pista.

**Colocación:**

1. Se construye la rejilla explícita con `Columns`, `Rows` y `Areas`. Cada nombre de `Areas`
   debe formar un rectángulo y todas las filas deben tener el mismo número de celdas.
2. Se colocan los ítems cuyo `Area` tiene fila y columna, o nombre de área.
3. Se colocan los demás en orden del árbol con un **cursor** que recorre la rejilla por filas
   (o por columnas, con `Flow: column`). Cada ítem va al primer hueco donde quepa. El cursor
   solo avanza, de modo que pueden quedar huecos atrás.
4. Si un ítem cae fuera de la rejilla declarada, se crean pistas implícitas `auto`.

Un ítem con **solo la fila fija** (`Area: 2 auto`) va a la primera columna de esa fila donde
quepa, sin mover el cursor; si no queda ninguna, es un error (`GRID_PLACEMENT`): la fila no crea
columnas implícitas. Uno con **solo la columna fija** (`Area: auto 2`) va a la primera fila
libre desde el cursor, en la siguiente si la columna queda detrás de él, y mueve el cursor
detrás de sí. Con `Flow: column` se intercambian filas y columnas. A diferencia de CSS, estos
ítems se colocan en el paso 3, en orden del árbol con los automáticos, y no antes que ellos.

**Valor de `Area`:** un nombre de área (`Area: side`) o `fila columna [filas columnas]`
contando desde 1, donde fila y columna pueden ser `auto`: `Area: 1 1 1 2` ocupa la fila 1 y
las columnas 1 y 2; `Area: auto auto 1 2` se coloca automáticamente ocupando dos columnas.
`Area:` sin valor equivale a `Area: auto auto`.

**Dimensionado de pistas.** Cada pista tiene una función mínima y una máxima: `150` es fija en
ambas; `auto` equivale a `minmax(min-content, max-content)`; `1fr` equivale a
`minmax(auto, 1fr)`, de modo que **una fracción no baja del min-content de su contenido**.

1. **Inicializar:** cada pista recibe una base (su mínimo) y un límite (su máximo; infinito en
   las fracciones).
2. **Aportaciones del contenido:** las pistas intrínsecas toman el min-content o el max-content
   de sus ítems; el exceso de los ítems que ocupan varias pistas se reparte entre las pistas
   intrínsecas que ocupan. Si alguna de ellas es una fracción, lo que le falta al ítem hasta su
   min-content se reparte solo entre las fracciones, en proporción a su `fr`: un ítem que cruza
   columnas `1fr` no desborda su área.
3. **Maximizar:** el espacio libre se reparte a partes iguales entre las pistas no flexibles
   hasta su límite. Una pista `auto` alcanza su max-content antes de que las fracciones reciban
   nada.
4. **Fracciones:** el resto se divide entre las fracciones de forma proporcional. Si una
   fracción necesita más que su parte por su min-content, se fija en su base y se repite el
   reparto.
5. **Estirar:** si no hay fracciones y sobra espacio, se reparte entre las pistas `auto`.

`fit-content` no es una pista: es un error, porque `auto` ya se ajusta al contenido. Las filas
en porcentaje necesitan un alto definido en el contenedor ([§3.6](#36-bases-de-los-porcentajes));
las columnas en porcentaje se comportan como `auto` solo al calcular los tamaños intrínsecos del
contenedor, cuando todavía no hay ancho. Los márgenes `auto` de un ítem son un error: la
alineación se hace con `Horizontal` y `Vertical`.

Las **columnas** se dimensionan primero, con los anchos min-content y max-content de los
ítems. Las **filas** después, con la altura de cada ítem maquetado al ancho de sus columnas.
Con `Height: auto` en el contenedor, las filas en fracción no tienen espacio que repartir y
miden en proporción a su contenido: todas las filas `1fr` miden lo que la más alta.

**Alineación en la celda.** Cada ítem ocupa un área formada por sus pistas y los huecos
internos. Con `stretch` (por defecto) y sin `Width`/`Height` declarado, ocupa toda el área y se
repite su layout. Con `start`, `end` o `center`, conserva su tamaño de contenido y se desplaza.
Los porcentajes de alto de un ítem se resuelven contra el alto de su área en el layout final;
al dimensionar las filas, todavía sin área, cuentan como `auto`.

### 5.9 Dimensionado

**Tamaños intrínsecos** (anchos de la caja de margen):

| Elemento | min-content | max-content |
|---|---|---|
| `Text` | palabra más ancha | párrafo más largo en una línea |
| `Image` | ancho natural o, con `Height` en puntos, el que sale de él con la proporción | igual |
| `Block`, `Relative`, `Absolute`, `Fixed` | máximo del min-content de los hijos | máximo del max-content de los hijos |
| `Flex` en `row`, sin `Wrap` | suma del min-content de los ítems + huecos | suma del max-content + huecos |
| `Flex` en `row`, con `Wrap` | máximo del min-content de los ítems | suma del max-content + huecos |
| `Flex` en `column` | máximo de los ítems | máximo de los ítems |
| `Grid` | suma de las columnas dimensionadas sin espacio libre, más los huecos | suma de las columnas dimensionadas con espacio infinito y cada ítem aportando su max-content también como mínimo, más los huecos |

Una caja con `Width` fijo tiene ese valor como min-content y max-content (el mínimo
automático de un ítem flex mira en cambio su contenido, [§5.7](#57-flex)). `Absolute` y `Fixed`
no aportan nada a sus ancestros. En flex se usan los tamaños intrínsecos de cada ítem
tal cual, sin tener en cuenta `Basis`, `Grow` ni `Shrink`. En grid, el max-content hace que
las fracciones lleguen al max-content de su contenido.

**Definido e indefinido.** Un tamaño es **definido** si se conoce sin hacer el layout del
contenido. Los anchos lo son siempre al maquetar; qué altos lo son está en el
[§3.6](#36-bases-de-los-porcentajes), y un porcentaje de un alto que no lo es, un error. El área
útil es definida: una caja de primer nivel con `Height: 100%` ocupa la página.

**Mínimos y máximos.** Se calcula el tamaño provisional, se limita por el máximo y se sube al
mínimo. Si se pueden comparar al leer el documento (los dos en puntos o los dos en
porcentaje), un mínimo mayor que el máximo, o un `Width`/`Height` fuera de ellos, es un error.
Si no se pueden comparar hasta maquetar (`Min width: 150` y `Max width: 20%`) y chocan, **gana
el mínimo**.

**Proporción.** Con `Aspect ratio` y un eje `auto`, ese eje se deduce del otro. Una `Image`
tiene siempre proporción: la de su fichero, o la de `Aspect ratio` ([§5.18](#518-imágenes)).

### 5.10 Fragmentación

**Modelo.** Un *fragmentainer* es el contenedor donde caen los fragmentos de las cajas: uno
por página, con la altura del área útil, y uno por columna dentro de un `Multicol`
([§5.16](#516-bloques-en-columnas)). Cada caja produce un **fragmento** por fragmentainer en el
que aparece.

**Oportunidades de corte:**

- entre cajas hermanas en flujo de bloque;
- entre líneas de texto;
- entre líneas de un contenedor flex y entre filas de grid.

Siempre se intenta cortar en el punto más alto del árbol posible; solo si no cabe se corta
dentro de un hijo.

**Elección del corte.** El motor elige el **último** corte de la página que no incumple ninguna
regla. Si no hay ninguno, relaja las reglas en este orden:

1. `Widows` y `Orphans`;
2. `Before` y `After: avoid` (de `Break`);
3. `Inside: avoid` (de `Break`).

Si sigue sin haber corte posible, corta donde se acaba el espacio.

Un párrafo que no puede respetar `Widows` y `Orphans` y no tiene nada antes en la página ofrece
igualmente un corte que las incumple, para que su padre pueda preferirlo a otro que incumpla
`avoid`. En ese corte se respetan las viudas si se puede y, si no, se llena el espacio.

Solo se vuelve atrás a cortes entre hijos: si el mejor corte es anterior al hijo que no cabe,
se descarta lo colocado después, pero nunca se repite el layout de un hijo anterior con menos
espacio para partirlo por dentro.

**Garantía de progreso.** Al principio de una página siempre se coloca algo, aunque haya que
cortarlo a la fuerza. Lo que no se puede partir y no cabe en una página entera (una línea más
alta que el área útil, una imagen, una fila con un ítem que no puede colocar nada) es un error
([§5.12](#512-desbordamiento)).

**Cortes forzados.** `Before: page` y `After: page` (de `Break`) fuerzan un salto de página, y
`Before: column` y `After: column`, dentro de un `Multicol`, un salto a la columna siguiente
(que, en la última columna, es también de página). Solo se
aplican a cajas en flujo: `Absolute` y `Fixed` no admiten `Break`. El `Before` del primer hijo de un bloque y el
`After` del último se propagan al bloque; de un `Multicol` solo sale el de página. Si una caja
tiene los dos, gana el de página. Un corte forzado al principio de una
página (o de una columna) se ignora (no deja páginas en blanco), igual que el `After: page` de la última
caja del documento. Un corte de página dentro de un `Multicol` deja sin llenar las columnas
que quedan en esa página.

**Márgenes en los cortes.** En un corte no forzado, los márgenes que tocan el corte se truncan
a cero: tras el corte, el margen superior del primer hijo de la página y los que salen de él
por arriba; un fragmento de continuación no deja salir márgenes por arriba. En un corte forzado,
se trunca el margen anterior al corte y se conserva el posterior.

**Bordes y relleno en los cortes.** Con `Decoration: slice`, el primer fragmento no tiene
borde ni relleno inferiores y los siguientes no tienen los superiores. Con `Decoration: clone`,
cada fragmento se pinta como una caja completa: el contenido de cada página cabe entre su borde
y relleno superiores e inferiores. Un `Height` fijo es el total de todos los fragmentos, no el
de cada uno; con `clone`, el total del contenido es ese alto menos un borde y un relleno, y
cada fragmento les suma los suyos. `Min height` y `Max height` también se aplican al total (con
`clone`, al contenido total más un borde y un relleno).

Con `slice`, el borde y el relleno inferiores solo se reservan cuando hacen falta: si la caja
terminaría en la página pero ellos no caben, se maqueta de nuevo con ese espacio reservado, de
modo que se corta antes (o pasa entera a la página siguiente) y termina allí con su borde y su
relleno. Si la caja se corta de todos modos, su fragmento llega hasta el final de la página.

Una caja con `Height` definido se parte como cualquier otra, aunque no tenga contenido: lo que
no cabe de su alto pasa a la página siguiente. Para evitarlo se declara `Inside: avoid`,
que no impide partirla si no cabe en una página entera. Un fragmento cortado de una caja con
alto `auto` mide hasta el corte; con alto definido, llega hasta el final de la página sin pasar
del alto que le queda.

**Flex y grid.** En `column`, el corte se hace entre ítems. En `row` y en grid, entre líneas
o filas. Una línea, fila o ítem que no cabe en lo que queda de página pasa entero a la
siguiente. Si tampoco cabe al principio de una página, sus ítems se fragmentan **en
paralelo**: cada uno se parte por su cuenta y la fila continúa en la página siguiente con los
ítems que no han terminado, todos desde arriba, y con la altura del más largo de ellos; las
filas siguientes van detrás, con el hueco que las separaba. Las filas que ocupa un mismo ítem
se parten juntas, cada ítem desde su posición. Un ítem estirado reparte entre las páginas el
alto que le impone su línea o su área, así que puede terminar antes o después que el texto de
sus hermanos. Si algún ítem no puede colocar nada, la fila no se parte, y si no cabe es un error.
Al elegir el
corte, una fila partida cuenta como el peor de los cortes de sus ítems. Un contenedor `column`
con `Wrap` no se parte.

**Imágenes.** Una `Image` no se parte: si no cabe en lo que queda de la página (o de la
columna), pasa entera a la siguiente ([§5.18](#518-imágenes)).

**Posicionadas.** Un `Absolute` se coloca en la página donde cae su posición y no se parte. Si
su containing block está partido, se resuelve contra el fragmento de esa página: su alto es el
de ese fragmento, no el total. Un `Fixed` se repite en las páginas que dice su `Pages`. Un `Relative` se
parte como cualquier bloque.

**Numeración.** `{page}` y `{pages}` se resuelven en dos fases: se pagina reservando el
ancho de un número de prueba de dos cifras (`00`) y después se sustituyen los valores reales.
Si el total de páginas tiene más cifras (100 páginas o más), antes se vuelve a paginar
reservando tantas cifras como tenga el total, y así hasta que basten.
Si el texto cambia de ancho, solo se vuelve a maquetar esa caja: sus líneas conservan los
saltos y se vuelven a medir y alinear. Como los `Fixed` no ocupan espacio, el flujo principal
no cambia.

`{page of: id}`, en cambio, se resuelve paginando hasta que el valor supuesto coincida con el
real ([§5.19](#519-referencias-a-otras-cajas)): el texto se mide ya con la página escrita, así
que puede cambiar el reparto en líneas y en páginas.

### 5.11 Recursos y fuentes

**Carpeta de recursos.** Los ficheros que usa un documento están en una única carpeta, por
defecto `resources/` (relativa al directorio de trabajo del motor), que la aplicación puede
cambiar. Dentro hay dos subcarpetas fijas, que pueden organizarse a su vez en subcarpetas:

```
resources/
	fonts/      fuentes TTF
	images/     imágenes PNG y JPEG
```

Los recursos se buscan **siempre hacia abajo**, desde la raíz de su subcarpeta. Una ruta de
recurso es relativa a esa subcarpeta: la de una `Image`, a `images/` (`logos/acme.png` es
`resources/images/logos/acme.png`, [§5.18](#518-imágenes)). Admite `/` y `\` como separadores y no
puede:

- ser absoluta (`/…`, `\…`, `C:…`) ni empezar por `~`;
- contener un segmento `..`, aunque al normalizarla volviera a quedar dentro;
- llegar, a través de un enlace simbólico, a un fichero fuera de su subcarpeta.

**Fuentes.** Solo se usan las fuentes TTF de `fonts/`; no hay fuentes del sistema, ni las 14
estándar de PDF, ni fuentes de sustitución. La salida PDF las incrusta (solo los glifos usados),
de modo que el documento se ve igual en cualquier máquina; por eso deben tener una licencia que
permita incrustarlas, como la SIL Open Font License. El repositorio incluye IBM Plex Sans, IBM Plex Serif e IBM Plex Mono.

Cada fichero se identifica por lo que declara su interior, no por su nombre:

- **familia:** la familia tipográfica de la tabla `name` (`nameID` 16) o, si no la tiene, la
  familia (`nameID` 1). Se compara exactamente, con mayúsculas y espacios;
- **peso:** `usWeightClass` de la tabla `OS/2`: 400 es `normal` y 700 es `bold`. Los ficheros
  de otros pesos (300, 500…) se ignoran;
- **estilo:** el bit de cursiva de `fsSelection` (`OS/2`).

Las fuentes variables no se admiten: se usan los ficheros estáticos de cada variante. El
kerning y las ligaduras no se aplican: el ancho de un texto es la suma de los avances de sus
glifos.

**Comprobaciones.** Con las métricas reales, antes de maquetar se comprueba que cada caja con
texto tiene su fuente (familia, peso y estilo) en `fonts/` y que la fuente tiene todos los
caracteres de su texto (los espacios en blanco se pintan como espacios; `{page}`, `{pages}` y las
referencias a otras cajas, como cifras). Un texto con tramos ([§5.4](#54-texto)) necesita además la variante de cada
tramo (`Weight: normal` con un tramo en negrita necesita la negrita), y cada tramo, sus
caracteres en ella. Una fuente desconocida, una variante que falta o un carácter que la fuente
no tiene son errores con la línea de la caja, o la del tramo si es la variante de un tramo
([§6](#6-validación)). También son errores, sin línea
porque no son del documento, que no exista `fonts/`, un fichero que no se puede leer, una fuente
variable o dos ficheros con la misma familia, peso y estilo.

### 5.12 Desbordamiento

**Nada puede desbordar.** Después de maquetar y paginar, el motor comprueba el resultado y da
un error con la primera de estas situaciones:

- **una palabra más ancha que su línea** (`TEXT_OVERFLOW`), en la línea del documento donde
  está la palabra;
- **una caja que no cabe a lo ancho en la caja de contenido de su padre**, contando sus
  márgenes horizontales (`BOX_OVERFLOW`), en la línea de su `Width` (o `Min width`) si lo
  declara;
- **contenido que pasa del alto de su caja**: líneas o hijos que no caben en el alto que fija
  su `Height`, su `Max height`, su `Aspect ratio` o, en un `Grid`, sus filas de tamaño fijo
  (`BOX_OVERFLOW`), en la línea de esa propiedad (`Rows` para las filas; si declara varias, la
  primera de esta lista);
- **algo del flujo que no cabe en el área útil de una página y no se puede
  partir** (`PAGE_OVERFLOW`), en la línea de lo que impide partirlo: su `Padding` o su
  `Border` si el relleno y el borde del fragmento ya no caben en la página; si no, su
  `Line height` (o, sin él, su `Size`), propios o heredados, si es una línea de texto; si no,
  su `Height` (en una `Image` sin él, su `Width`, del que sale el alto);
- **un `Absolute` o un `Fixed` que se sale de la página física** (`BOX_OVERFLOW`), en la
  línea de su `Offset`. Pueden ocupar los márgenes del medio, pero no salir de la página;
- **una tabla cuya cabecera y pie no dejan sitio al cuerpo** al principio de una página
  (`TABLE_REPEAT_OVERFLOW`, [§5.15](#515-tablas)), en la línea de la `Table`. Se detecta al
  maquetar, porque sin sitio la tabla no avanza;
- **el contenido de un `Multicol` que no cabe en sus columnas** y no puede seguir en otra
  página (`BOX_OVERFLOW`, [§5.16](#516-bloques-en-columnas)): en la línea de su `Height` (o
  `Max height`, o `Aspect ratio`) si es su alto el que se acaba, o en la de su `Count` si,
  sin página ni alto que llenar, tiene más cortes de columna que columnas. También se detecta
  al maquetar.

El error señala al culpable **más interior**: si una caja se sale porque se sale algo de dentro,
se nombra lo de dentro. Los `Relative` se comprueban en su sitio, sin el desplazamiento de
`Offset`, y un margen negativo cuenta como espacio ganado: no son desbordamientos, sino
decisiones del autor. Tampoco cuentan los márgenes verticales, que colapsan.

### 5.13 Encabezados y párrafos

Algunos elementos tienen **significado**, además de su layout: `Paragraph` y los encabezados
`Heading 1` … `Heading 6`. Se maquetan con los algoritmos de siempre; el significado se ve en las
reglas `Element` ([§4.10](#410-reglas-reutilizables-styles)), en los cortes de página de los
encabezados y en la salida PDF.

**Sin estilos implícitos.** Un `Heading 1` no es más grande que un `Heading 2` por sí mismo: el
estilo de cada tipo se declara una vez en `Styles`, igual que el de cualquier otro elemento.

```
Styles:
	Element: Heading 1
		Typography:
			Size: 20
			Weight: bold
		Box:
			Margin: 0 0 10 0
	Element: Paragraph
		Box:
			Margin: 0 0 6 0
Heading 1:
	Id: results
	Text: Resultados del trimestre
Paragraph:
	Content >>
		Las ventas crecieron un 12 %.
```

**`Paragraph`** es un `Text` con significado: admite lo mismo (`Id`, `Class`, `Box`,
`Typography`, `Break` y el texto en el valor o en `Content`) y se maqueta igual. Es **un solo
párrafo**: una línea en blanco en su `Content` es un error (`PARAGRAPH_BLANK_LINE`); para varios
párrafos se usan varios `Paragraph`. `Text` sigue existiendo como caja de texto sin significado.

**Encabezados.** Un `Heading N` es un **bloque** cuyo contenido son uno o varios `Text`, apilados
como en un `Block`; además admite `Id`, `Class`, `Box` y `Typography`, y nada más. Su
`Typography` lo heredan sus `Text`.

- **Cortes:** el motor **evita siempre** cortar dentro de un encabezado y después de él, como si
  declarara `Inside: avoid` y `After: avoid` ([§5.10](#510-fragmentación)): un título no queda
  solo al final de una página ni se parte. Son `avoid`, así que se relajan como cualquier otro
  cuando no hay un corte mejor. Por eso un encabezado no admite `Break`, ni sus `Text` tampoco
  (`BREAK_IN_HEADING`), ni de una regla (`STYLE_NOT_ALLOWED`). El corte antes del encabezado no
  cambia.
- **Jerarquía:** los niveles tienen que ser correctos (`HEADING_LEVEL`). En orden del documento,
  el primer encabezado es `Heading 1`, y cada uno sube como mucho un nivel respecto al anterior:
  a un `Heading 1` le puede seguir un `Heading 2`, pero no un `Heading 3`. Se puede bajar
  los niveles que se quiera.
- **Dónde:** donde va un elemento en flujo, también como ítem de `Flex` o `Grid`; nunca dentro de
  un `Fixed` (`HEADING_IN_FIXED`), que se repite en cada página y no forma parte del contenido.

**Salida PDF.** El PDF va **etiquetado** (*tagged PDF*, para accesibilidad) y lleva
**marcadores** (*outline*):

- **Árbol de estructura:** un elemento por caja, en orden del documento, que es el orden de
  lectura: `Document` (la raíz), `H1` … `H6` (los encabezados), `P` (`Paragraph`), `Span`
  (`Text`), `L` y `LI` (las listas, [§5.14](#514-listas)), `Figure` (las `Image` con `Alt`, con su
  texto alternativo y su `BBox`, [§5.18](#518-imágenes)) y `Div` (el resto). El texto de cada caja es contenido marcado de su elemento; una
  caja partida entre páginas tiene contenido en varias.
- ***Artifacts*:** los fondos, los bordes, las imágenes sin `Alt` y todo lo que hay dentro de un
  `Fixed` no forman parte del contenido.
- **Marcadores:** uno por encabezado, con el texto de sus `Text` unido por espacios, anidados por
  nivel y que llevan a la esquina superior izquierda de su primer fragmento. El visor los
  muestra al abrir el documento.
- **Título e idioma:** `Title` va a la información del documento y a los metadatos XMP, y el
  visor lo muestra en lugar del nombre del fichero; `Language` es el idioma de todo el
  contenido ([§4.1](#41-media)). Sin ellos, el PDF no los declara.

El volcado de resultados ([§7](#7-formato-de-resultados)) no cambia: no incluye el tipo de
elemento.

### 5.14 Listas

Una `List` apila sus ítems (`List item`) en vertical, como un `Block`, y cada ítem lleva un
**marcador** generado por el motor, que no está en el documento. Las dos son cajas de bloque y
admiten `Id`, `Class`, `Box`, `Typography` y `Break`; un ítem contiene uno o varios elementos
(`Block`, `Flex`, `Grid`, `Text`, `Paragraph`, `List`, `Relative`, `Absolute` o `Fixed`), y
nunca encabezados. Una `List` va donde va un elemento en flujo, también dentro de otro ítem.

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

**Marcadores.** `Marker` es obligatorio: si la lista es numerada lo decide el autor. El marcador
es texto en la tipografía del `List item` (propia, heredada o de `Element: List item`): `•`
(`disc`), `–` (`dash`), ninguno (`none`), o el número del ítem con un punto: `3.`, `c.`, `C.`,
`iii.`, `III.`. Siempre va seguido de un espacio, que lo separa del contenido.

**Numeración.** El primer ítem lleva el número `Start` (1 si no se declara); cada uno, el
siguiente al anterior, salvo el que fija el suyo con `Number`, desde el que continúan los
demás. Las letras siguen tras la `z` como en CSS (`aa`, `ab`…). `decimal` escribe desde 0; las
letras y los romanos, desde 1, y los romanos, hasta 3999 (`LIST_NUMBER_RANGE`). `Start` y
`Number` solo tienen efecto en las numeraciones (`LIST_NUMBER_NOT_ALLOWED`). Cada lista numera
sus ítems; una lista anidada empieza de nuevo.

**Posición.** El marcador cuelga **fuera** del ítem, a su izquierda: su borde derecho (con el
espacio final) toca el borde izquierdo de la caja de borde del ítem, así que los marcadores de
distinto ancho quedan alineados a la derecha. La sangría que lo aloja es el `Padding` izquierdo
de la `List`, más el margen izquierdo del ítem. Su línea base es la de la **primera línea** del
ítem: la que se encuentra bajando por el primer hijo en flujo de cada bloque hasta un texto
(con una lista anidada al principio, los dos marcadores quedan en la misma línea). Un ítem con
marcador cuyo contenido no lleva así a un texto (empieza con un `Flex`, un `Grid` o un bloque
vacío) es un error (`LIST_ITEM_WITHOUT_LINE`). Un marcador más ancho que su sangría también
(`LIST_MARKER_OVERFLOW`, al maquetar, porque depende de las métricas): no se sale de la lista.

**Cortes.** El marcador va pegado a la primera línea: si no cabe en lo que queda de página,
pasan los dos a la siguiente, y si el ítem se parte, el marcador solo está en su primer
fragmento. No cambia las reglas de corte (§5.10).

**Salida PDF.** Una `List` es `L`, con el atributo `ListNumbering` (`Disc`, `Decimal`,
`LowerAlpha`, `UpperAlpha`, `LowerRoman`, `UpperRoman`; `None` para `dash` y `none`); cada
ítem es `LI`, con el marcador en `Lbl` y su contenido en `LBody`. En el volcado
([§7](#7-formato-de-resultados)), el marcador de un ítem con `Id` aparece dentro de él como
`Marker`, con su texto y su `Rect` (el espacio final incluido en el ancho).

### 5.15 Tablas

Una `Table` coloca sus celdas en filas y columnas: sus filas (`Row`) van en una cabecera
(`Head`, opcional), un cuerpo (`Body`, obligatorio) y un pie (`Foot`, opcional), **en ese
orden** (`TABLE_ORDER`), y cada fila tiene sus celdas (`Cell`) de izquierda a derecha. La
`Table` admite `Id`, `Class`, `Box`, `Typography` y `Break`, y va donde va un elemento en flujo,
también dentro de una celda.

```
Styles:
	Element: Cell
		Box:
			Padding: 2 4
			Border: 0 0 0.5 0
Table:
	Columns: 120 1fr 1fr
	Head:
		Row:
			Typography:
				Weight: bold
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

**Una tabla es un grid.** `Columns` (obligatorio) y `Gap` son los de `Grid` ([§5.8](#58-grid)):
las columnas se dimensionan igual, con `auto`, fracciones, `minmax`…, y `Gap` separa las celdas
(el `border-spacing` de CSS). Las filas son pistas `auto`: cada una mide lo que su celda más
alta, y las celdas se estiran a su fila. Cada celda es un ítem del grid colocado
explícitamente: va en la primera columna libre de su fila, y ocupa las filas y columnas de su
`Span` (`filas columnas`, 1 1 por defecto; `Span: 1 3` ocupa tres columnas). Una columna está
ocupada si la ocupa una celda de una fila de arriba con un `Span` de varias filas. Cada fila
llena **exactamente** todas las columnas (`TABLE_ROW_CELLS`): una celda vacía se escribe
`Cell:`. Un `Span` no se sale de la tabla, no pasa de un grupo a otro (de `Head` a `Body`, de
`Body` a `Foot`) ni se solapa con otra celda (`TABLE_SPAN`).

**Celdas.** Una `Cell` es una caja con dos formas, como un `Text`: con valor (`Cell: 100`), una
caja de texto de una línea; sin valor, un bloque con elementos (`Block`, `Flex`, `Grid`,
`Text`, `Paragraph`, `List`, `Table`, `Relative`, `Absolute` o `Fixed`; nunca encabezados), o
vacía si no tiene ninguno. Las dos cosas a la vez son un error (`CELL_CONTENT`); un texto largo
va en un `Paragraph`. Admite `Id`, `Class`, `Box`, `Typography` y `Span`. Los bordes son los
de cada celda, separados ([§8](#8-diferencias-con-css)): una línea entre filas se dibuja con
el borde inferior de las celdas.

**Estilo de las celdas.** Una regla de `Styles` ([§4.10](#410-reglas-reutilizables-styles)) da
estilo a las celdas de una tabla sin repetir una clase en cada una:

- **Por posición.** `Element: Head cell`, `Body cell` y `Foot cell` son las celdas de cada grupo;
  `Odd row cell` y `Even row cell`, las de las filas impares y pares del **cuerpo**, contando
  desde la primera fila de `Body` como la 1 (la cebra; la cabecera y el pie no entran). La fila
  de una celda que ocupa varias es la **primera** de las que ocupa. Una celda cumple a la vez la
  de su grupo y la de su fila, y la regla `Element: Cell` de siempre.
- **Por columna.** `For column: 3` (o `For column: 2 4`, de la 2 a la 4) se aplica a las celdas
  que **empiezan** en esas columnas, en cualquier grupo: una celda con `Span` de varias columnas
  es de la columna donde empieza.
- **`Column align`.** Un `Column align: left right right` de la `Table` da una alineación a cada
  columna (tantas como `Columns`, `TABLE_COLUMN_ALIGN`). Es solo el valor por defecto del `Align`
  de las celdas: lo sobrescriben las reglas de posición, de columna, de clase y de `Id`, y lo
  que declara la propia celda.

Las celdas de las tablas de un `Markdown` ([§5.17](#517-markdown)) reciben estas mismas reglas; su
`Align` sale de la fila de separación de la tabla (`:--`, `--:`), que lo declara cada celda y
gana a todo lo demás.

**Filas.** Una `Row` no es una caja: no tiene `Box` ni se nombra en `Element`, como `Head`,
`Body` y `Foot`. Su `Typography` lo heredan sus celdas (las reglas `Styles` de la celda le
ganan, como a cualquier herencia), y su `Break` es el de sus celdas: con él se fuerza o se
evita el corte antes o después de la fila, o dentro de ella. Una celda no declara `Break`, ni
siquiera desde una regla (`STYLE_NOT_ALLOWED`).

**Cortes.** Una tabla se corta entre filas, como un grid ([§5.10](#510-fragmentación)): una fila
que no cabe en lo que queda de página pasa entera a la siguiente, y solo se parte, con sus
celdas en paralelo, si no cabe ni en una página vacía. Las filas de `Head` y `Foot` se
**repiten** en cada página de la tabla: la cabecera arriba y el pie justo detrás de la última
fila del cuerpo de esa página, cada uno con el `Gap` que lo separa del cuerpo. No se parten
nunca y no admiten `Break` (`TABLE_ROW_BREAK`). Las filas del cuerpo se cortan en lo que dejan
la cabecera y el pie: si no cabe ninguna, la tabla entera pasa a la página siguiente, y la
cabecera no se queda sola. Si al principio de una página la cabecera y el pie no dejan sitio al
cuerpo, la tabla no puede avanzar y es un error (`TABLE_REPEAT_OVERFLOW`); una fila del cuerpo
que no cabe en el sitio que dejan se parte en paralelo, como cualquier fila, y si no se puede
partir desborda la página (`PAGE_OVERFLOW`, [§5.12](#512-desbordamiento)). Las cajas
`Absolute` y `Fixed` de las celdas de la cabecera y el pie solo van en el primer fragmento de
la tabla.

**Salida PDF.** Una `Table` es `Table`; `Head`, `Body` y `Foot` son `THead`, `TBody` y `TFoot`;
cada fila, `TR`, y cada celda, `TH` en `Head` (con `Scope: Column`) y `TD` en el resto, con
los atributos `RowSpan` y `ColSpan` si ocupa más de una fila o columna. La cabecera y el pie
son contenido en la primera página en que salen; donde se repiten, son *artifacts*, como los
`Fixed`, y el texto de la tabla se lee una sola vez.

### 5.16 Bloques en columnas

Un `Multicol` reparte su contenido en **columnas** del mismo ancho, como en una revista: el
contenido llena la primera columna, sigue en la segunda y, al acabar las columnas de una
página, continúa en la primera columna de la página siguiente. Admite `Id`, `Class`, `Box`,
`Typography` y `Break`, como un bloque, y sus propiedades ([§4.13](#413-bloques-en-columnas));
va donde va un elemento en flujo, también dentro de otro `Multicol`.

```
Multicol:
	Count: 2
	Column gap: 20
	Rule: 0.5
	Rule color: #999999
	Paragraph:
		Content >>
			Texto largo que fluye por las dos columnas…
	Paragraph:
		Content >>
			…y sigue en la página siguiente.
```

**Columnas.** El número y el ancho salen de `Count` y `Column width`, sobre el ancho de la caja
de contenido (`W`) y con `Column gap` (`g`) entre cada dos:

- solo `Count: n`: `n` columnas;
- solo `Column width: w`: tantas como quepan, `⌊(W + g) / (w + g)⌋`, y al menos una;
- los dos: las que quepan, sin pasar de `Count`.

Las columnas se reparten el ancho que dejan los huecos: miden `(W − (n − 1) · g) / n`. Sin
`Count` ni `Column width`, es un error (`MULTICOL_COLUMNS`).

**Contenido.** Dentro de las columnas los hijos se apilan en flujo de bloque
([§5.3](#53-flujo-de-bloque)): cada columna es un contexto de formato de bloque, así que los
márgenes de dentro no salen del `Multicol`. Admite todos los elementos en flujo; un `Absolute`
o un `Fixed` dentro de un `Multicol`, a cualquier profundidad, es un error
(`POSITIONED_IN_MULTICOL`). El alto de las columnas no es definido: un porcentaje vertical de
dentro es un error (`INDEFINITE_PERCENTAGE`), aunque el `Multicol` tenga `Height`.

**Alto de las columnas.** Cada columna es un *fragmentainer*
([§5.10](#510-fragmentación)), y el contenido se parte entre columnas igual que entre páginas,
con las mismas reglas (viudas y huérfanas, `avoid`, garantía de progreso). En cada página, las
columnas miden lo que queda de página para el contenido del `Multicol` y, si tiene `Height`
(o `Max height`), no pasan de lo que le queda de él:

1. Se llenan **en orden**: la primera columna, desde donde se quedó el contenido; cada una de
   las siguientes, desde donde termina la anterior. Se para al acabar el contenido, las
   columnas o en un corte forzado de página.
2. Si el contenido termina en esta página y `Fill` es `balance`, las columnas se
   **equilibran**: se busca el alto mínimo con el que el contenido cabe en ellas sin cortes
   entre columnas que incumplan más reglas que los del reparto en orden. Con `Fill: auto` se
   quedan como en el paso 1. Las páginas en las que el contenido no termina no se equilibran.
3. El `Multicol` mide en esa página lo que su columna más alta (con `Height`, lo de cualquier
   caja con alto definido, [§5.10](#510-fragmentación)).

Fuera de una página y sin alto definido (un `Multicol` que es un ítem de flex o de grid, o que
está en un `Absolute`) no hay alto que llenar: las columnas se equilibran siempre, también con
`Fill: auto`.

**Cortes.** Un `Multicol` se parte entre páginas: lo que no cabe en las columnas de una página
sigue en la primera columna de la siguiente. Si la primera columna no puede colocar nada, el
`Multicol` entero pasa a la página siguiente, y si no puede otra columna, el resto. El corte de
página del `Multicol` tiene la calidad del de su última columna. `Before: column` y
`After: column` (de `Break`) fuerzan un salto a la columna siguiente; fuera de un `Multicol`
son un error (`COLUMN_BREAK_OUTSIDE_MULTICOL`). Un `Before: page` o `After: page` de dentro
termina las columnas de la página.

**Lo que no cabe.** Si el alto propio del `Multicol` (`Height`, `Max height`) termina en una
página y su contenido no cabe en las columnas de esa página, es un error (`BOX_OVERFLOW`); lo
mismo si, sin página ni alto que llenar, sus cortes de columna necesitan más columnas de las
que tiene ([§5.12](#512-desbordamiento)).

**Línea entre columnas.** Con `Rule`, entre cada dos columnas **con contenido** se pinta una
línea de ese grosor, centrada en el hueco, desde arriba de la caja de contenido y con el alto
de la columna más alta de esa página. Su color es `Rule color` o, sin él, el `Color` del texto.
Se pinta con el fondo del `Multicol`, debajo del contenido, y no puede ser más ancha que
`Column gap` (`COLUMN_RULE_WIDTH`).

**Tamaños intrínsecos** ([§5.9](#59-dimensionado)). Con `Count` y sin `Column width`, el
min-content es `Count` columnas del min-content del contenido más los huecos; con
`Column width`, una columna, porque el número de columnas se adapta al ancho. El max-content
es `Count` columnas (o una, sin `Count`) del max-content del contenido, o de `Column width` si
es mayor, más los huecos.

**Salida.** En el volcado del [§7](#7-formato-de-resultados), las columnas no tienen
fragmento propio: una caja partida entre dos columnas tiene dos fragmentos en la misma página.
En el PDF etiquetado, un `Multicol` es un `Div` y su contenido cuelga de él en orden de
lectura, de columna en columna.


### 5.17 Markdown

Un `Markdown` es un bloque cuyo contenido se escribe en Markdown, en su `Content`. Al construir
el árbol, ese contenido se **traduce a los elementos que ya existen**, y lo generado se trata
como si lo hubiera escrito el autor: pasa por el schema, por las reglas de `Styles`, por la
validación del [§6](#6-validación) y por el PDF etiquetado y los marcadores. El `Markdown` es la
caja que lo contiene, un bloque como un `Block`: sus `Box`, `Typography` y `Break` son los
suyos, y lo generado hereda su `Typography`.

```
Document (dev.stxt.boxes):
	Media:
		Width: 595
		Height: 842
		Margin: 40
	Typography:
		Font: IBM Plex Sans
		Size: 10
	Styles:
		Element: Heading 1
			Typography:
				Size: 18
		Element: List
			Box:
				Padding: 0 0 0 20
	Markdown:
		Bullet: disc dash
		Numbering: decimal
		Table columns: equal
		Content >>
			# Resultados

			Las ventas **suben** y los costes *bajan*: un trimestre ++muy bueno++,
			con ~~pérdidas~~ beneficios.

			- Ventas en alza
			- Costes:
			  - personal
			  - material

			1. Revisar
			2. Aprobar

			| Región | Ventas |
			|--------|-------:|
			| Norte  | 120    |
			| Sur    | **95** |
```

**El subconjunto.** El Markdown es [CommonMark](https://commonmark.org) con las tablas y el
tachado de GFM y el subrayado `++…++`. Se traduce así:

| Markdown | Elemento |
|---|---|
| `#` … `######` (y los subrayados con `===` y `---`) | `Heading 1` … `Heading 6`, con un `Text` |
| Párrafo | `Paragraph` |
| `-`, `*`, `+` | `List` con el `Marker` de `Bullet` para su nivel; un `List item` por ítem |
| `1.`, `1)` | `List` con el `Marker` de `Numbering` para su nivel y `Start`, el primer número si no es 1 |
| Tabla | `Table` con las `Columns` de `Table columns`; la fila de cabecera en `Head` y el resto en `Body` |
| `**x**`, `__x__` | tramo en negrita ([§5.4](#54-texto)) |
| `*x*`, `_x_` | tramo en cursiva |
| `++x++` | tramo subrayado |
| `~~x~~` | tramo tachado (con una sola tilde no se tacha) |
| `[x](destino)`, `<destino>` | tramo subrayado con un enlace a ese destino ([§5.20](#520-enlaces)) |
| `![Alt](ruta)`, sola en su párrafo | `Image` con esa ruta y su `Alt`; con `![](ruta)`, decorativa ([§5.18](#518-imágenes)) |

Todo lo demás es un error (`MARKDOWN_UNSUPPORTED`): enlaces con título (`[x](destino "título")`),
imágenes dentro de un texto (también dentro de un enlace) o con título (`![Alt](ruta "título")`),
código (en línea o en bloque), citas, HTML, líneas horizontales, saltos de línea forzados (dos espacios o `\` al final
de la línea) y definiciones de enlaces (`[x][ref]` con `[ref]: destino`: el destino se escribe en el propio enlace). Un encabezado tampoco puede ir dentro de un ítem de
lista, como en un `List item`, ni una tabla quedarse sin filas debajo de la cabecera.

**Texto.** Un salto de línea simple dentro de un párrafo vale un espacio, como en un `Text`. El
formato dentro de la línea se suma: `***x***` es negrita y cursiva. Un encabezado o un párrafo
sin texto es un error (`TEXT_CONTENT`).

**Listas.** El nivel de una lista es el número de listas de su mismo tipo que la contienen: la
primera lista con viñetas usa el primer marcador de `Bullet`; una con viñetas dentro de esa, el
segundo, y así hasta el último, que sirve para los niveles más profundos. Las numeradas cuentan
aparte, con `Numbering`. Cada párrafo de un ítem es un `Paragraph`, y una lista anidada, una
`List` dentro del `List item`. Las listas compactas y las separadas por líneas en blanco se
traducen igual: el espacio entre ítems lo dan las reglas de `Styles`. Como cualquier lista, la
sangría que deja sitio al marcador es su `Padding` izquierdo, que se suele declarar en la regla
`Element: List` ([§5.14](#514-listas)).

**Tablas.** Cada columna tiene la alineación de la fila de separación (`:--`, `:-:`, `--:`) como
`Align` de sus celdas; sin alineación, la de la caja. Una celda con texto es una `Cell` con
valor; una vacía, una `Cell` vacía. Como en GFM, a una fila con menos celdas que la cabecera se
le añaden celdas vacías y las que sobran se descartan.

**Estilo.** Lo generado no tiene `Id` ni `Class`: se le da estilo con las reglas `Element` de
`Styles` (`Element: Heading 1`, `Element: Paragraph`…), que se aplican a todo el documento, y
con el `Typography` del `Markdown`, que hereda.

**Encabezados.** `#` es siempre `Heading 1`, y la jerarquía se comprueba en orden del documento
junto con los encabezados escritos fuera del `Markdown` ([§5.13](#513-encabezados-y-párrafos)):
después de un `Heading 1`, un Markdown puede empezar por `##`. Dentro de un `Fixed`, un Markdown
no puede tener encabezados.

**Dónde va.** Donde va un elemento en flujo: en `Document`, `Block`, `Relative`, `Absolute`,
`Fixed`, `Multicol`, `List item`, `Cell` y como elemento de un `Item` o un `Area`.

**Errores.** Los errores de lo generado señalan la línea del documento donde empieza su bloque
de Markdown (la del ítem, la de la fila de la tabla); los del marcador de una lista, la de
`Bullet` o `Numbering`, y los de las columnas de una tabla, la de `Table columns`. Una lista o
una tabla sin su propiedad es un error con la línea de la lista o la tabla
(`MARKDOWN_PROPERTY_MISSING`); una propiedad que no se usa no lo es.

**Salida.** En el PDF etiquetado, el `Markdown` es un `Div` y lo generado cuelga de él con sus
etiquetas (`H1`, `P`, `L`, `Table`…); sus encabezados son marcadores. En el volcado del
[§7](#7-formato-de-resultados) solo sale el `Markdown`, si tiene `Id`: lo generado no lo tiene.

### 5.18 Imágenes

Una `Image` pone en el documento una imagen PNG o JPEG de la carpeta `images/` de los recursos
([§5.11](#511-recursos-y-fuentes)). Es una caja sin hijos, como un `Text`, cuyo contenido es la
imagen: se coloca, se dimensiona, se corta entre páginas y se pinta como cualquier caja, con su
`Box` (márgenes, relleno, borde y fondo) y su `Break`.

```
Document (dev.stxt.boxes):
	Media:
		Width: 595
		Height: 842
		Margin: 40
	Fixed:
		Offset: -30 0 auto auto
		Image: test/square.png
			Box:
				Width: 20
	Image: test/wide.png
		Alt: Un paisaje con montañas y un sol
		Box:
			Width: 100%
			Margin: 0 0 10 0
	Flex:
		Gap: 10
		Item:
			Image: test/tall.jpg
				Alt: Un retrato
				Box:
					Height: 60
		Item:
			Grow: 1
			Image: test/wide.png
				Fit: cover
				Box:
					Height: 60
```

(El ejemplo usa las imágenes de prueba del repositorio, en `resources/images/test/`: un logotipo
en la esquina de cada página, una imagen a todo el ancho y, debajo, un retrato de 45 × 60 y la
misma imagen recortada a lo que queda de ancho.)

**Fichero.** El valor es la ruta del fichero, relativa a `images/` y siempre hacia abajo
([§5.11](#511-recursos-y-fuentes)): `Image: logos/acme.png` es `resources/images/logos/acme.png`.
Se admiten PNG (con transparencia) y JPEG, y el formato se reconoce por el contenido, no por la
extensión. Una `Image` sin ruta, un fichero que no existe o una ruta que sale de `images/` son un
error (`IMAGE_UNKNOWN`); un fichero que no es PNG ni JPEG (GIF, WebP, SVG…) o que no se puede
leer, también (`IMAGE_FORMAT`). Los ficheros se comprueban antes de maquetar, con las fuentes, y
los errores señalan la línea de la `Image`.

**Tamaño natural.** Cada píxel mide 0,75 pt, como en CSS (96 píxeles por pulgada), sin mirar la
resolución que declare el fichero: una imagen de 200 × 100 píxeles mide 150 × 75 pt. Para otro
tamaño se declara `Width` o `Height`.

**Dimensionado.** La caja de contenido de una imagen tiene la **proporción** del fichero, o la de
`Aspect ratio` si se declara, que la sustituye (no es un error). El relleno y el borde van
fuera: no la deforman.

- Con `Width` y `Height` en `auto`, mide su tamaño natural, **también en flujo de bloque**: no se
  estira al ancho del padre. Si no cabe, es un desbordamiento (`BOX_OVERFLOW`); para que ocupe
  el ancho disponible, se declara `Width: 100%`. `Margin: 0 auto` la centra, porque no ocupa
  todo el ancho.
- Con un eje declarado, o impuesto por el padre (un ítem flex con `Grow`, una columna de grid
  con `Horizontal: stretch`), el otro sale de la proporción. Con los dos, la caja mide lo
  declarado y `Fit` decide cómo la llena la imagen.
- `Min width`, `Max width`, `Min height` y `Max height` se aplican como en cualquier caja. Si los
  dos ejes son `auto` y el alto choca con su mínimo o su máximo, el ancho se vuelve a deducir del
  alto limitado: la proporción se conserva.
- Su alto es siempre definido, así que un flex no la estira en el eje cruzado de una fila, ni un
  grid en vertical; en horizontal sí se estira, y el alto sigue a la proporción.
- **Tamaños intrínsecos:** min-content y max-content son el ancho natural o, si declara `Height`
  en puntos, el que sale de él con la proporción. Es un mínimo de verdad: en una pista `1fr` de
  grid o en un ítem flex sin `Min width`, la imagen no encoge por debajo de él.

**`Fit`.** Si la caja de contenido y la imagen tienen proporciones distintas, `contain` (por
defecto) dibuja la imagen entera, lo más grande que cabe y centrada, y deja ver el fondo a los
lados; `cover` la dibuja lo más pequeña que cubre la caja, centrada, y recorta lo que sobra;
`fill` la estira a la caja y la deforma. Con la misma proporción, las tres dan lo mismo.

**Cortes de página.** Una imagen no se parte: si no cabe en lo que queda de la página (o de la
columna de un `Multicol`), pasa entera a la siguiente, como una línea de texto. Si no cabe ni en
una página entera, es `PAGE_OVERFLOW`, con la línea de su `Height` (o, sin él, de su `Width`).
`Before` y `After` funcionan como en cualquier caja; `Inside` y `Decoration`, que solo cuentan al
partir una caja, son un error (`BREAK_IN_IMAGE`), también si los da una regla de `Styles`.

**Dónde va.** Donde va un elemento en flujo: en `Document`, `Block`, `Relative`, `Absolute`,
`Fixed` (un logotipo que se repite en cada página), `Multicol`, `List item` (pero no al
principio: el marcador necesita una línea con la que alinearse, `LIST_ITEM_WITHOUT_LINE`), `Cell`
y como elemento de un `Item` o un `Area`. En un `Markdown`, una imagen sola en su párrafo
(`![Alt](ruta)`) es una `Image` ([§5.17](#517-markdown)).

**Accesibilidad.** Con `Alt`, un texto no vacío, la imagen es un elemento `Figure` del PDF
etiquetado ([§5.13](#513-encabezados-y-párrafos)) con ese texto alternativo y su `BBox` (su caja
de contenido en la página). Sin `Alt` es decorativa y se pinta como un *artifact*, igual que los
fondos. Dentro de un `Fixed` es siempre un *artifact*, y en las filas repetidas de una tabla, solo
es contenido la primera vez.

**Salida.** El PDF incrusta cada fichero una sola vez, aunque se use en varias páginas, con la
resolución del fichero: el JPEG tal cual y el PNG sin pérdida y con su transparencia. El SVG de
depuración la incrusta como `data:`. En el volcado del [§7](#7-formato-de-resultados) es una caja
más.

### 5.19 Referencias a otras cajas

Un texto puede escribir un dato de otra caja del documento: `{page of: id}` es el número de la
página donde empieza la caja con ese `Id`, y `{number of: id}`, su número en el documento. Se
escriben en el texto de un `Text`, un `Paragraph` o una `Cell` con valor, como `{page}` y
`{pages}`, y se pueden mezclar con ellos:

```
Text: El esquema de conexión está en la página {page of: wiring}; véase la cláusula {number of: pago}.
Block:
	Id: wiring
List:
	Marker: decimal
	List item:
		Id: pago
		Paragraph: …
```

- **Qué página es.** La de la **primera** caja que sale de la caja referida: si está partida entre
  varias páginas, la página donde empieza. Si la caja es una fila repetida de una tabla (cabecera
  o pie), la primera de sus páginas.
- **Dónde se puede referir.** Cualquier caja con `Id` que no sea un `Fixed` ni esté dentro de uno,
  que se repiten en todas las páginas (`REFERENCE_NOT_PLACEABLE`). La referencia puede ir antes o
  después de la caja, y también en un `Fixed` (que escribe lo mismo en todas las páginas).
- **El `Id` es el de la caja**, igual que en `For id`: el de un `Item` o un `Area` va en su
  elemento. Un `Id` que no existe, o una referencia sin `Id` (`{page of: }`), es un error
  (`REFERENCE_UNKNOWN`) con la línea del texto, o la del carácter si el texto es un bloque
  `Content >>`.
- **El texto mide lo que se escribe.** La referencia se sustituye antes de repartir el texto en
  líneas, así que `página 9` y `página 10` miden distinto y el texto puede pasar a una línea más.
  Eso mueve el contenido y puede cambiar la página de la caja referida; por eso la página se
  resuelve **paginando hasta que converja**: la primera pasada supone un número de prueba de dos
  cifras (como `{page}`), cada pasada comprueba en qué página ha quedado cada caja referida, y si
  no coincide con la supuesta, se vuelve a paginar con la que ha salido. Termina cuando coinciden.
  Si tras 10 pasadas que cambian alguna página no han coincidido, es un error
  (`REFERENCE_UNSTABLE`, con la línea de la referencia que sigue cambiando): el texto de una
  referencia cambia de ancho con su página y mueve la caja de página en las dos direcciones. No
  pasa con documentos normales, porque un número solo cambia de ancho al cambiar de cifras
  (9 → 10, 99 → 100).
- **Número.** Lo tienen los `List item` de una lista numerada, y es lo que escribe su marcador
  sin el sufijo: `3`, `c`, `IV` (`decimal`, `lower-alpha`, `upper-roman`…), con el `Start` y el
  `Number` que haya. Cada ítem da solo su propio número: para escribir «4.2», se escriben
  los dos, `{number of: clausula}.{number of: apartado}`. Un `List item` de una lista con viñetas
  o sin marcador, o cualquier otra caja (los encabezados no se numeran), no tiene número
  (`REFERENCE_NOT_NUMBERED`). El número no depende de la paginación: se sustituye igual desde la
  primera pasada. No se puede referir a un ítem escrito en un `Markdown`, porque lo generado no
  lleva `Id`.
- **Accesibilidad.** El texto es contenido de su elemento (`P`, `Span`…), como cualquier número.

**Índice.** Un `Contents` es un bloque en flujo, sin hijos, que genera una **entrada por
encabezado** de los niveles de `Levels` (por defecto 1 a 3), en orden del documento, también los
de un `Markdown` y los que están después del propio índice. Se admite donde va un `Markdown`:

```
Contents:
	Levels: 1 2
	Leader: dots
```

- **Cada entrada** es una caja `Flex` generada, en una fila, con tres cajas: el **título**, que
  es el texto del encabezado (sus `Text` unidos por un espacio y sin formato en línea); la
  **guía**, que crece hasta llenar el hueco, con 4 pt a cada lado; y el **número de página**, que
  es un `{page of: …}` del encabezado, y se resuelve igual que el de cualquier otra referencia
  (paginando hasta que converja). Un encabezado sin `Id` se refiere con un nombre generado.
  Sin guía (`Leader: none`), el título llena el hueco. Si el título no cabe en una línea, se
  parte, y el número queda en la primera línea.
- **Guía.** Con `dots`, puntos cuadrados de 0,09 veces el cuerpo, cada medio cuerpo y pegados al
  número; con `rule`, una línea de 0,05 veces el cuerpo, de un lado al otro del hueco. Se pintan
  sobre la línea base de la entrada y con el color del texto, como fondos (*artifacts*).
- **Estilo.** La entrada hereda la tipografía del `Contents`; sus cajas, la de la entrada. Las
  reglas `Element: Contents entry 1` … `Contents entry 6` dan `Box`, `Typography` y `Break` a las
  entradas de un nivel: la sangría de cada nivel es su `Padding` izquierdo, y el espacio entre
  entradas, su `Margin` o su `Padding` vertical. Las propias entradas no llevan `Id` ni `Class`.
- **Errores.** Un `Contents` sin ningún encabezado en sus niveles (`CONTENTS_EMPTY`, con la línea del
  `Contents`); un `Levels` que no son dos niveles de 1 a 6 en orden (`VALUE_NOT_ALLOWED`); un
  `Contents` sin `Font` o sin `Size` propios o heredados (`TEXT_WITHOUT_FONT` y
  `TEXT_WITHOUT_FONT_SIZE`, con la línea del `Contents`).
- **PDF etiquetado.** Un `TOC` con un `TOCI` por entrada, cada uno con dos `Span` (el título y el
  número); la guía no es contenido. Los marcadores no cambian ([§5.13](#513-encabezados-y-párrafos)).
- **Enlace.** Cada entrada es clicable entera y lleva a su encabezado, como un `Link: #id`
  ([§5.20](#520-enlaces)); con el `Link` etiquetado, cada `TOCI` tiene además un `Link`.
- **Volcado ([§7](#7-formato-de-resultados)).** Solo sale el `Contents`, si tiene `Id`: sus
  entradas no.

### 5.20 Enlaces

Un enlace lleva a quien lo pulsa a una URL de fuera del documento o a otra caja de él. Se escribe
de dos maneras, que producen lo mismo en el PDF: la propiedad `Link` de una caja, que hace
clicable la caja entera, y el enlace de Markdown (`[texto](destino)`), que hace clicable una parte
de una línea.

```
Text: Escribe a soporte@airemas.example
	Link: mailto:soporte@airemas.example

Image: examples/brand/nogal.png
	Link: https://tallernogal.example

Markdown:
	Content >>
		Consulta [las especificaciones](#specs) o [la web](https://airemas.example).
```

- **Destino.** Una URL `https://…`, `http://…` o `mailto:…` (con servidor o destinatario), o `#id`:
  el `Id` de una caja del documento. Cualquier otra cosa es un error (`LINK_SCHEME`, con la línea
  del `Link` o, en Markdown, la del bloque): un destino vacío, mal escrito, sin servidor, con otro
  esquema (`javascript:`, `file:`, `tel:`…) o una ruta relativa. El motor no abre nada: solo escribe
  la anotación, y un visor no se encuentra con esquemas que ejecuten o lean nada.
- **Destinos del documento.** `#id` debe ser el `Id` de una caja (`LINK_UNKNOWN` si no existe, con
  la línea del `Link`), que puede estar antes o después del enlace, y que no esté en un `Fixed` ni
  dentro de uno, que se repite en todas las páginas y no tiene un único sitio (`LINK_UNKNOWN`).
  En el PDF lleva a la página y a la esquina superior izquierda del primer fragmento de la
  caja, como un marcador. Un encabezado de un `Markdown` no tiene `Id`, así que no se puede
  enlazar con `#id`; lo enlaza el índice que lo lista ([§5.19](#519-referencias-a-otras-cajas)).
- **Dónde se admite `Link`.** En todos los elementos de caja salvo `Markdown` (su contenido lleva sus
  propios enlaces) y `Contents` (sus entradas ya llevan el suyo): `Block`, `Flex`, `Grid`,
  `Text`, `Paragraph`, los encabezados, `List`, `List item`, `Table`, `Cell`, `Multicol`, `Image`,
  `Relative`, `Absolute` y `Fixed`. No es una propiedad de `Box`, `Typography` ni `Break` y no va
  en las reglas de `Styles`: es del elemento, como `Id`. No se hereda: lo que hay dentro de una
  caja con enlace es clicable por ella, pero no lleva enlace propio. Si lo lleva, las dos zonas se
  solapan y el visor decide cuál se activa.
- **Aspecto.** Un enlace no cambia el aspecto de lo que enlaza: el color y el subrayado los da el
  autor con su estilo. La excepción es el enlace de Markdown, que sale **subrayado**, porque no
  hay otra manera de darle estilo a una parte de una línea. El subrayado es el de un tramo
  ([§5.4](#54-texto)) y se puede combinar con negrita y cursiva (`[la **web**](…)`).
- **Zonas.** La zona clicable de una caja con `Link` es su **caja de borde**, una por fragmento: una
  caja partida entre páginas es clicable en cada página. La de un enlace de Markdown es el
  rectángulo de cada tramo de cada línea, del alto de la línea: un enlace que ocupa dos líneas
  tiene dos zonas, y los tramos seguidos con el mismo destino (una parte en negrita dentro del
  enlace) son una. Un `Fixed` con `Link` es clicable en todas las páginas; las filas repetidas de
  una tabla, también.
- **PDF.** Cada zona es una anotación `Link` sin borde, con la acción `URI` o el destino de la
  página, y como texto (`Contents`) lo que dice la caja o el tramo, para quien no lo ve (el texto de
  la caja y de lo que contiene, o el `Alt` de una imagen; sin texto, el propio destino).
- **PDF etiquetado.** Cada anotación es un elemento `Link` hijo del elemento de la caja, con la
  anotación como referencia a objeto (`OBJR`) y su texto como `Alt`. No tiene `Link` lo que no es
  contenido: lo que hay dentro de un `Fixed`, una imagen decorativa y las filas repetidas de una
  tabla desde su segunda página; la anotación sigue siendo clicable.
- **Volcado ([§7](#7-formato-de-resultados)).** Los enlaces no se vuelcan.

---

## 6. Validación

El modelo es **más estricto que CSS**: un documento que pide algo que no tiene efecto, o dos
cosas que se contradicen, es un error, no un caso que se resuelve con una regla por defecto. Así
el resultado del layout es predecible: lo que se lee en el documento es lo que se obtiene.

- **Cada error tiene un código estable** y un mensaje que dice qué falla y cómo arreglarlo. Los
  mensajes pueden mejorar; los códigos no cambian.
- **La línea es la del nodo culpable:** la de la propiedad, no la de su caja; la del carácter
  dentro de un bloque `Content >>`; la de la `Font` declarada en una antecesora, si la familia no
  existe. Si el problema es la combinación de dos propiedades, el mensaje nombra la línea de la
  otra.
- **Se para en el primer error.**

La validación se hace en cuatro niveles:

1. **Sintaxis STXT y schema** ([§9](#9-schema)): qué hijos admite cada elemento,
   cardinalidades, valores vacíos donde no se admiten, bloques `>>` y tipos `ENUM`, `INTEGER` y
   `BOOLEAN`. La realiza la librería STXT con sus propios códigos (`CHILD_NOT_DECLARED`,
   `TOO_MANY_CHILDREN`, `INVALID_VALUE`…). Es donde se detecta una propiedad fuera de su
   elemento: `Offset` en un `Block`, `Grow` en un `Area`, `Columns` en un `Flex`. El motor
   valida también los documentos que no se han leído con el lector, así que estos errores salen
   siempre con los códigos de la librería.
2. **Modelo:** las reglas que el schema no puede expresar, al construir el árbol de cajas.
3. **Recursos** ([§5.11](#511-recursos-y-fuentes)): las fuentes, con las métricas reales (la
   salida PDF), y las imágenes ([§5.18](#518-imágenes)), antes de maquetar, porque necesitan la
   carpeta de recursos.
4. **Desbordamientos** ([§5.12](#512-desbordamiento)): sobre el resultado, después de maquetar
   y paginar.

| Código | Regla | Mensaje (ejemplo) |
|---|---|---|
| `DOCUMENT_ROOT` | Un único nodo raíz, `Document`, con el namespace `dev.stxt.boxes` (el schema acepta cualquier elemento como raíz) | `El nodo raíz debe ser Document (dev.stxt.boxes):` |
| `MEDIA_SIZE` | `Width` y `Height` del medio, puntos positivos; `Margin`, solo puntos, y deja área útil | `Los márgenes del medio no dejan área útil` |
| `VALUE_SYNTAX` | Sintaxis de los valores ([§3](#3-valores)), colores `#RGB` o `#RRGGBB`, `Aspect ratio` | `Valor no válido: 12,5 (usa punto decimal…)` |
| `VALUE_NOT_ALLOWED` | Formas y signos que admite cada propiedad ([§3.1](#31-tipos-de-valor), [§4](#4-referencia-de-propiedades)); `Gap` con 1 o 2 valores; `Size` > 0; `Count` ≥ 1; `Column width` y `Rule` > 0; `Title` y `Alt` no vacíos y `Language` una etiqueta BCP 47; `Bullet` con marcadores sin numerar y `Numbering` con numeraciones; `Levels` con dos niveles de 1 a 6, el primero no mayor que el último | `Width no admite 1fr: usa un número…` |
| `FIT_CONTENT_TRACK` | `fit-content` no es una pista de grid | `fit-content no es una pista de grid: usa auto…` |
| `DUPLICATE_ID` | Los `Id` son únicos | `Id repetido: a (ya se usa en la línea 6)` |
| `SINGLE_ELEMENT` | `Item` y `Area` contienen exactamente un elemento | `Un Area contiene exactamente un elemento, y este tiene Block (línea 8) y Text (línea 10)` |
| `TEXT_CONTENT` | Un `Text` tiene su texto en el valor o en `Content`, uno de los dos; un `Markdown` tiene contenido, y sus encabezados y párrafos, texto | `Text tiene el texto en el valor y en Content (línea 9): deja solo uno` |
| `TEXT_WITHOUT_FONT` | El texto, y un `Contents`, necesitan `Font` en `Typography`, propia o heredada (también de `Document`) | `El texto necesita Font: declárala en el Typography del Text, de un antecesor o de Document` |
| `TEXT_WITHOUT_FONT_SIZE` | El texto necesita `Size` en `Typography`, propio o heredado (también de `Document`) | `El texto necesita Size: decláralo en el Typography del Text, de un antecesor o de Document` |
| `STYLE_UNKNOWN_ELEMENT` | `Element` nombra un elemento de caja ([§4.10](#410-reglas-reutilizables-styles)) | `Element: 'Item' no es un elemento de caja (Block, Flex…)` |
| `STYLE_UNKNOWN_ID` | Un `For id` tiene un elemento con ese `Id` | `Ningún elemento tiene el Id summary de esta regla For id` |
| `STYLE_DUPLICATE` | Dos reglas no tienen el mismo selector | `Regla repetida: Element: block (ya está en la línea 6)` |
| `STYLE_NOT_ALLOWED` | Una regla (también de posición o de columna) no da `Break` a un `Absolute`, un `Fixed`, un encabezado, un `Text` de un encabezado o una `Cell` | `La regla For class: page aplica Break a un Absolute (línea 11)…` |
| `HEADING_LEVEL` | El primer encabezado es `Heading 1` y cada uno sube como mucho un nivel ([§5.13](#513-encabezados-y-párrafos)) | `Salto de nivel: Heading 3 después de Heading 1; como mucho, Heading 2` |
| `HEADING_IN_FIXED` | Un encabezado no va dentro de un `Fixed` | `Un Heading 1 no puede estar dentro de un Fixed…: usa un Text` |
| `BREAK_IN_HEADING` | Un `Text` de un encabezado no declara `Break` | `Un Text de un encabezado no admite Break: el motor ya evita cortar…` |
| `PARAGRAPH_BLANK_LINE` | Un `Paragraph` no tiene líneas en blanco en su `Content` (línea de la línea en blanco) | `Un Paragraph es un solo párrafo: quita la línea en blanco…` |
| `LIST_ITEM_EMPTY` | Un `List item` contiene al menos un elemento ([§5.14](#514-listas)) | `Un List item necesita al menos un elemento: un Paragraph, un Text, un Block…` |
| `LIST_NUMBER_NOT_ALLOWED` | `Start` y `Number` solo en las listas numeradas | `Start no tiene efecto con Marker: disc, que no numera los ítems` |
| `LIST_NUMBER_RANGE` | El número cabe en la numeración: `decimal` desde 0, letras desde 1, romanos de 1 a 3999 (línea del `Start`, del `Number` o del ítem) | `El número 4000 no se puede escribir con Marker: upper-roman, que numera de 1 a 3999` |
| `LIST_ITEM_WITHOUT_LINE` | Un ítem con marcador empieza con un texto con el que alinearlo (línea de la caja que lo impide) | `El marcador del List item de la línea 10 se alinea con su primera línea, y el ítem empieza con un Flex…` |
| `TABLE_ORDER` | `Head`, `Body` y `Foot` van en ese orden ([§5.15](#515-tablas)) | `Body va después de Foot: los grupos de una Table van en el orden Head, Body, Foot` |
| `TABLE_ROW_CELLS` | Cada fila llena exactamente las columnas de `Columns`, contando los `Span` de las filas de arriba (línea de la fila, o de la celda que sobra) | `La fila deja libre la columna 2 de 2 (Columns): añade una Cell, vacía si hace falta, o usa Span` |
| `TABLE_SPAN` | Un `Span` no se sale de la tabla, no pasa de un grupo a otro ni se solapa con otra celda | `La celda ocupa de la columna 1 a la 3, y la tabla tiene 2 (Columns)` |
| `TABLE_COLUMN_ALIGN` | `Column align` da una alineación por columna de `Columns` (línea del `Column align`) | `Column align da 2 alineaciones y la tabla tiene 3 columnas (Columns): escribe una por columna` |
| `CELL_CONTENT` | Una `Cell` tiene texto en el valor o elementos, no las dos cosas | `Una Cell tiene texto en el valor (línea 12) o elementos, no las dos cosas: pon el texto en un Paragraph` |
| `TABLE_ROW_BREAK` | Las filas de `Head` y `Foot`, que se repiten en cada página, no declaran `Break` | `Las filas de Head se repiten en cada página de la tabla y no se cortan: quita el Break` |
| `MULTICOL_COLUMNS` | Un `Multicol` declara `Count`, `Column width` o los dos ([§5.16](#516-bloques-en-columnas)) | `Un Multicol necesita Count, Column width o los dos…` |
| `COLUMN_RULE_WIDTH` | `Rule` no es más ancha que `Column gap` | `Rule es más ancha que Column gap, que sin declarar es 0: la línea se pintaría sobre las columnas…` |
| `COLUMN_BREAK_OUTSIDE_MULTICOL` | `Before` y `After: column` solo dentro de un `Multicol` (línea del `Before` o el `After`, también de una regla) | `After: column solo tiene efecto dentro de un Multicol: usa page para cortar la página` |
| `POSITIONED_IN_MULTICOL` | Un `Absolute` o un `Fixed` no va dentro de un `Multicol` | `Un Fixed no puede estar dentro de un Multicol…` |
| `MARKDOWN_UNSUPPORTED` | El Markdown solo usa el subconjunto del [§5.17](#517-markdown): ni enlaces con título, ni definiciones de enlaces, ni imágenes dentro de un texto o con título, ni código, ni citas, ni HTML, ni líneas horizontales, ni saltos forzados; ni encabezados en un ítem de lista, ni tablas sin filas debajo de la cabecera (línea del bloque) | `Markdown no admite imágenes dentro de un texto (una imagen va sola en su párrafo): solo admite encabezados, párrafos, listas, tablas y…` |
| `LINK_SCHEME` | El destino de un enlace (`Link` o enlace de Markdown, [§5.20](#520-enlaces)) es una URL `https://`, `http://` (con servidor) o `mailto:` (con destinatario) o `#id`; no está vacío ni mal escrito (línea del `Link`, o la del bloque en Markdown) | `Link: 'javascript:alert(1)' no tiene un esquema admitido: solo https://, http://, mailto: y #id` |
| `LINK_UNKNOWN` | Un enlace `#id` lleva a una caja que existe y que no está en un `Fixed` ni dentro de uno (línea del `Link`, o la del bloque en Markdown) | `El enlace #nada lleva a un Id que no existe: ninguna caja tiene el Id nada` |
| `IMAGE_UNKNOWN` | Una `Image` tiene ruta, y es la de un fichero de `images/` (línea de la `Image`) | `No existe el recurso logos/acme.png en …/resources/images` |
| `IMAGE_FORMAT` | Una `Image` es un PNG o un JPEG que se puede leer (línea de la `Image`) | `La imagen dibujo.gif no es PNG ni JPEG (es GIF): conviértela a uno de los dos` |
| `BREAK_IN_IMAGE` | Una `Image` no declara `Inside` ni `Decoration`, tampoco en una regla ([§5.18](#518-imágenes)) | `Inside no tiene efecto en una Image, que no se parte entre páginas: quítalo` |
| `MARKDOWN_PROPERTY_MISSING` | Un Markdown con listas con viñetas declara `Bullet`; con listas numeradas, `Numbering`; con tablas, `Table columns` (línea de la lista o la tabla) | `La lista necesita Bullet en su Markdown (línea 8): el marcador de cada nivel, como Bullet: disc dash` |
| `CONTENTS_EMPTY` | Un `Contents` tiene algún encabezado en los niveles de `Levels` (línea del `Contents`) | `El índice no tiene ningún encabezado de los niveles 2 a 3 (Levels): no tendría entradas` |
| `REFERENCE_UNKNOWN` | Una referencia (`{page of: id}`, [§5.19](#519-referencias-a-otras-cajas)) tiene `Id`, y es el de una caja del documento (línea de la referencia) | `{page of: nada}: ninguna caja tiene el Id nada` |
| `REFERENCE_NOT_PLACEABLE` | La página de una caja, que no está en un `Fixed` ni dentro de uno | `{page of: pie}: pie está en un Fixed, que se repite en todas las páginas` |
| `REFERENCE_NOT_NUMBERED` | El número de una caja que lo tiene: un `List item` de una lista numerada (línea de la referencia) | `{number of: nota}: nota no tiene número (solo lo tienen los ítems de una lista numerada)` |
| `REFERENCE_UNSTABLE` | La página de las cajas referidas se estabiliza en 10 pasadas de paginación (línea de la referencia) | `La página de x no se estabiliza: tras 10 pasadas sigue cambiando…` |
| `CLASS_WITHOUT_RULE` | Cada clase de `Class` tiene su regla `For class` | `La clase aside no tiene regla: añade 'For class: aside' en Styles` |
| `OFFSET_CONFLICT` | Un `Relative` no se desplaza a la vez desde arriba y abajo, ni desde izquierda y derecha | `Un Relative no puede desplazarse a la vez desde arriba y desde abajo…` |
| `OVERCONSTRAINED` | Un `Absolute` o un `Fixed` con inicio, tamaño y fin necesita algún margen `auto` | `Izquierda, Width y derecha declarados fijan la posición dos veces…` |
| `MARGIN_AUTO_WITHOUT_EFFECT` | `Margin: auto` solo donde centra ([§4.3](#43-box)) | `Margin auto no centra una caja que ocupa todo el ancho…` |
| `LINES_WITHOUT_WRAP` | `Lines` solo con `Wrap: true` | `Lines solo tiene efecto con Wrap: true…` |
| `GROW_WITHOUT_EFFECT` | `Grow` en `column` necesita un alto definido en el contenedor | `Grow no tiene efecto: el flex column del padre no tiene un alto definido…` |
| `ASPECT_RATIO_CONFLICT` | `Aspect ratio` no se combina con `Width` y `Height` declarados | `Aspect ratio no tiene efecto si Width y Height están declarados` |
| `MIN_MAX_CONFLICT` | Mínimo, máximo y tamaño comparables no se contradicen ([§5.9](#59-dimensionado)) | `Min width es mayor que Max width (línea 7)` |
| `INDEFINITE_PERCENTAGE` | Un porcentaje vertical necesita un alto definido ([§3.6](#36-bases-de-los-porcentajes)) | `Height usa un porcentaje de un alto que no es definido…` |
| `GRID_PLACEMENT` | `Areas` rectangulares y con filas de igual longitud; valor de `Area` válido y con áreas existentes; un ítem con la fila fija cabe en ella (§5.8) | `Área desconocida en Area: foot` |
| `FONT_UNKNOWN` | `Font` existe en la carpeta de fuentes (línea de la `Font`) | `Fuente desconocida: Arial. Familias en …/resources/fonts: IBM Plex Mono, IBM Plex Sans, IBM Plex Serif` |
| `FONT_VARIANT_MISSING` | La familia tiene la variante de `Weight` y `Style`, y la de cada tramo con formato (línea del texto, o la del tramo) | `La fuente IBM Plex Sans no tiene la variante bold italic… (Font en la línea 6, …)` |
| `GLYPH_MISSING` | La fuente tiene todos los caracteres del texto (línea del carácter) | `La fuente IBM Plex Sans normal no tiene el carácter '漢' (U+6F22)` |
| `TEXT_OVERFLOW` | Ninguna palabra es más ancha que su línea (línea de la palabra) | `La palabra 'supercalifragilística' no cabe en la línea: la línea mide 138.69 y la caja deja 100…` |
| `BOX_OVERFLOW` | Las cajas caben en su contenedor, el contenido en su alto (también en las columnas de un `Multicol`), y los `Absolute` y `Fixed` en la página | `El contenido de la caja de la línea 7 no cabe en su alto: su texto llega a 24 y la caja deja 20` |
| `LIST_MARKER_OVERFLOW` | El marcador de un ítem, con su espacio, cabe en el `Padding` izquierdo de la lista más el margen izquierdo del ítem (línea del ítem) | `El marcador '1.' del List item de la línea 12 mide 11.45 con su espacio, y la sangría deja 5…` |
| `TABLE_REPEAT_OVERFLOW` | La cabecera y el pie de una tabla dejan sitio al cuerpo en una página vacía (línea de la `Table`) | `La cabecera y el pie de la tabla de la línea 8 miden 900 y no dejan sitio al cuerpo en una página vacía…` |
| `PAGE_OVERFLOW` | Lo que no se puede partir cabe en una página | `La caja de la línea 9 no cabe en la página y no se puede partir: mide 900 de alto…` |

**Catálogo de errores.** [`test/errors/`](../test/errors) tiene un documento de ejemplo por
regla (a veces varios), en un fichero cada uno y agrupados en carpetas por familia de errores,
con el código y la línea que debe dar.

---

## 7. Formato de resultados

El motor puede volcar el resultado en STXT, que es el formato de los tests de referencia. Cada
`Rect` es la caja de borde en **coordenadas absolutas de página**: `x y ancho alto`, en pt, con
hasta dos decimales.

```
Result:
	Page: 1
		Fragment: header
			Rect: 40 10 515 20
		Fragment: body
			Rect: 40 80 515 300
			Fragment: side
				Rect: 40 80 150 120
```

Por defecto solo se vuelcan las cajas con `Id`; las descendientes con `Id` se anidan dentro.
El marcador de un `List item` con `Id` se vuelca dentro del ítem como `Marker: 3.`, con su
`Rect` ([§5.14](#514-listas)).
Un `Absolute` se anida dentro de su containing block, detrás de sus hijos en flujo, y no dentro
de su padre. Las columnas de un `Multicol` no tienen fragmento: sus hijos se anidan dentro del
`Multicol`, y una caja partida entre dos columnas sale dos veces en la misma página
([§5.16](#516-bloques-en-columnas)).
Las cajas sin `Id` pueden incluirse para depurar. Las líneas de texto y sus tramos no se
vuelcan, ni los enlaces ([§5.20](#520-enlaces)): no cambian el layout.

Un **test de referencia** es una carpeta con `input.stxt` (el documento) y `expected.stxt`
(el volcado esperado). Los números de `Rect` se comparan con una tolerancia de 0.01 pt.

---

## 8. Diferencias con CSS

| Tema | CSS | Este modelo |
|---|---|---|
| Documento | HTML + hojas de estilo con cascada y selectores | Un árbol STXT de elementos; cada uno declara sus propiedades y solo admite las que tienen sentido en él |
| Reglas de estilo | Selectores compuestos, combinadores, especificidad calculada, `!important` | Selectores simples (`Element`, que también nombra la posición de una celda de tabla, `For class`, `For id` y `For column`) con prioridad fija, solo con `Box`, `Typography` y `Break` ([§4.10](#410-reglas-reutilizables-styles)) |
| Errores | Muy permisivo: lo que no tiene efecto se ignora y los conflictos se resuelven con reglas | Estricto: lo que no tiene efecto o se contradice es un error con código y línea ([§6](#6-validación)) |
| Texto suelto entre cajas | Cajas anónimas | No existe: el texto va siempre en un `Text` |
| Herencia | Muchas propiedades | Solo lo que va en `Typography` (fuente, tamaño, interlineado, alineación, color, reparto, viudas y huérfanas) |
| `display` | Combina tipo exterior e interior | El elemento (`Block`, `Flex`, `Grid`, `Text`) fija el interior; el exterior lo decide el padre. No hay `inline-block`. |
| `position` | Propiedad de cualquier caja | Elementos `Relative`, `Absolute` y `Fixed`, que son bloques: un flex o grid posicionado va dentro de uno de ellos |
| Propiedades de ítem (`flex-grow`, `grid-area`…) | En el propio ítem, con efecto solo si el padre es flex o grid | En el envoltorio `Item` o `Area`, que solo existe dentro de su contenedor |
| `box-sizing` | `content-box` por defecto; configurable | No existe: `Width` y `Height` miden siempre la caja de borde |
| Unidades | px, em, rem, vw… | pt, %, fr |
| `line-height` sin unidad | Multiplicador | **Un número son puntos**; el relativo se escribe con % |
| Altura de línea | Calculada por fuente, *half-leading* y *baseline* | Cada línea mide `Line height`; la línea base se coloca con el *half-leading* de su fuente |
| Fuentes | Del sistema o con `@font-face`, con una lista de alternativas | Solo TTF de la carpeta de recursos, sin alternativas; la fuente es obligatoria y un carácter que no tiene es un error |
| `font-weight`, `font-style` | Pesos de 100 a 900, `oblique`, síntesis de negrita y cursiva | `Weight: normal` o `bold` y `Style: normal` o `italic`, sin síntesis: el fichero debe existir |
| Kerning y ligaduras | Sí | No |
| Formato dentro de una línea | Elementos en línea (`<strong>`, `<em>`, `<span>`…) con cualquier propiedad | Tramos con negrita, cursiva, subrayado y tachado, que se suman a la fuente de la caja; no pueden quitar la negrita ni cambiar la familia, el tamaño o el color ([§5.4](#54-texto)) |
| `text-decoration` | Estilos (`wavy`, `dotted`…), color, grosor y desplazamiento propios; el subrayado se interrumpe en los descendentes (`text-decoration-skip-ink`) | Una raya continua del color del texto, con la posición y el grosor de la fuente |
| Palabras largas | Desbordan (configurable) | Error |
| Desbordamiento | Visible por defecto (`overflow: visible`) | Error ([§5.12](#512-desbordamiento)) |
| `h1` … `h6` | Tamaño, peso, márgenes y `break-after: avoid` de la hoja de estilo del navegador; cualquier contenido en línea; los niveles se pueden saltar | Sin estilo implícito (se declara en `Styles`); el motor evita siempre el corte dentro y después; solo contienen `Text`; los saltos de nivel son un error ([§5.13](#513-encabezados-y-párrafos)) |
| `p` | Varios bloques de texto separados por elementos | `Paragraph`: un `Text` con significado, un solo párrafo |
| Accesibilidad y marcadores | Los da el navegador al imprimir, según el caso | PDF etiquetado y marcadores de los encabezados, siempre |
| `float`, tablas, `sticky`, `transform`, `opacity` | Sí | No |
| Medio continuo (pantalla) | Sí | No: siempre páginas |
| `position: fixed` en impresión | Repetido en cada página | Repetido, relativo al área útil, en las páginas que dice `Pages` (todas por defecto); no hay `@page :first` ni márgenes distintos por página |
| `justify-content` | `space-between`, `space-around`, `space-evenly` | `Main axis: between`, `around`, `evenly` |
| `z-index` | Propiedad `z-index` | `Layer` |
| `box-decoration-break` | `slice`, `clone` | `Decoration: slice`, `clone` |
| Elección del corte de página | Puede volver a maquetar un hijo ya colocado con menos espacio para partirlo por dentro | Solo vuelve atrás a cortes entre hijos |
| `text-wrap` | `wrap`, `balance`, `pretty`… (`pretty` según el navegador) | `Text wrap: auto` (voraz) o `pretty` (Knuth-Plass en todo el párrafo) |
| Márgenes `auto` en ítems flex y grid | Absorben espacio (en grid, alinean el ítem en su área) | Error: se alinea con `Main axis`, `Cross axis`, `Horizontal` y `Vertical` |
| Márgenes `auto` sin efecto | Valen 0 (verticales en bloque, ancho `auto`, `absolute` sin inicio y fin) | Error |
| `top` y `bottom` (o `left` y `right`) en una `relative` | Gana `top` (o `left`) | Error |
| `absolute` sobre-restringida | Se ignora `right` (o `bottom`) | Error |
| Porcentaje de un alto indefinido | Se comporta como `auto` | Error ([§3.6](#36-bases-de-los-porcentajes)) |
| Un mínimo mayor que el máximo | Gana el mínimo | Error si se comparan al leer; si no, gana el mínimo |
| `z-index` sin contexto de apilamiento | Se ignora | `Layer` solo existe en los elementos posicionados, `Item` y `Area` |
| `id` repetido | Se tolera | Error |
| Posición estática de un `Absolute` en flex o grid | Como si fuera el único ítem, con la alineación del contenedor | Esquina de la caja de contenido del contenedor |
| Pista `fit-content` en grid | Función `fit-content(límite)`: como `auto`, sin pasar del límite | Error: se usa `auto` |
| Listas (`ul`, `ol`) | Dos elementos y `list-style-type`, que se hereda y tiene valor por defecto | Un solo `List` con `Marker` obligatorio, que no se hereda ni va en `Styles` ([§5.14](#514-listas)) |
| Marcadores de lista | `outside` o `inside`; `::marker` con estilo propio; cualquier texto o imagen; `circle` y `square` | Solo fuera, con la tipografía del ítem; `disc`, `dash`, `none` y cinco numeraciones con sufijo `.`; sin `circle` ni `square` |
| Marcador que no cabe en la sangría | Se sale de la lista | Error (`LIST_MARKER_OVERFLOW`) |
| Ítem sin primera línea (empieza con un flex, un grid o un bloque vacío) | El marcador se alinea con lo que haya | Error (`LIST_ITEM_WITHOUT_LINE`) |
| `start` y `value` en una lista con viñetas | Se ignoran | Error (`LIST_NUMBER_NOT_ALLOWED`) |
| `reversed` | Sí | No |
| Anchos de las columnas de una tabla | `table-layout: auto` (o `fixed`), con su propio algoritmo | El dimensionado de pistas de grid, con `Columns` obligatorio ([§5.15](#515-tablas)) |
| Bordes de tabla | `border-collapse: separate` o `collapse` | Solo separados, los de cada celda |
| Fila con menos celdas que columnas, o `rowspan` que pasa de `tbody` a `tfoot` | Se completa con huecos; el `rowspan` se recorta | Error (`TABLE_ROW_CELLS`, `TABLE_SPAN`) |
| Estilo de `tr`, `col` y `colgroup` | Fondo y bordes de fila y columna | `Row` solo da `Typography` y `Break` a sus celdas; sin columnas con estilo |
| `caption` | Sí | No: un `Heading` o un `Paragraph` antes de la tabla |
| `thead` y `tfoot` al imprimir | Se repiten según el navegador | Se repiten siempre en cada página |
| Multicolumna (`column-count`, `column-width`) | Propiedades de cualquier bloque; `column-gap` por defecto `1em` y con porcentajes | Elemento `Multicol` con `Count` y `Column width`; `Column gap` por defecto 0 y solo en puntos ([§5.16](#516-bloques-en-columnas)) |
| `column-span: all` | Sí | No: se parte el `Multicol` en dos |
| Markdown (CommonMark y GFM) | Se acepta todo; lo que no tiene traducción se deja como texto o se ignora | Un subconjunto cerrado; el resto es un error. El subrayado (`++x++`) no es de CommonMark ni de GFM ([§5.17](#517-markdown)) |
| Estilo por defecto del Markdown | La hoja de estilo del navegador da tamaño a los encabezados, sangría a las listas… | Ninguno: se declara en `Styles` |
| `column-fill` | `balance`, `balance-all`, `auto` | `Fill: balance` o `auto`, sin `balance-all`. Sin página ni alto que llenar se equilibra siempre, y el equilibrado no incumple reglas de corte que el reparto en orden respeta |
| `column-rule` | Grosor, estilo y color; se pinta sobre el contenido si es más ancha que el hueco | `Rule` y `Rule color`, siempre continua; más ancha que `Column gap` es un error |
| Columnas que no caben en el alto del contenedor | Columnas de desbordamiento a un lado | Error (`BOX_OVERFLOW`) |
| `position: absolute` o `fixed` dentro de un multicolumna | Sí | Error (`POSITIONED_IN_MULTICOL`) |
| `break-before: column` fuera de un multicolumna | Se ignora | Error (`COLUMN_BREAK_OUTSIDE_MULTICOL`) |
| Imágenes (`<img>`) | Elemento en línea, dentro del texto; muchos formatos (GIF, WebP, SVG…); `srcset` y `sizes` | Una caja `Image` sola, con un PNG o un JPEG de la carpeta de recursos ([§5.18](#518-imágenes)) |
| Tamaño de un píxel | 1/96 de pulgada | Igual (0,75 pt), sin mirar la resolución que declare el fichero |
| `object-fit` | `fill` por defecto; también `none` y `scale-down`, con `object-position` | `Fit`, con `contain` por defecto; sin `none` ni `scale-down`, y siempre centrada |
| Imagen más ancha que su contenedor | Desborda (se suele evitar con `max-width: 100%`) | Error (`BOX_OVERFLOW`): se declara `Width` |
| Imagen en un ítem estirado de flex o grid | Se estira en los dos ejes y se deforma | Su alto sale de la proporción y no se estira; su ancho, sí |
| `alt=""` | Imagen decorativa | Sin `Alt`; un `Alt` vacío es un error |
| Tabla de contenido | La genera un script o una librería a partir del HTML; con CSS paginado, `target-counter()` para los números | `Contents`: una entrada por encabezado, con guía y páginas, generada por el motor ([§5.19](#519-referencias-a-otras-cajas)) |
| Número de un ítem de lista referido desde otro sitio | `counter()` y `target-counter()` con contadores propios | `{number of: id}`: el número de su marcador, sin contadores propios ([§5.19](#519-referencias-a-otras-cajas)) |
| Enlaces | `<a href>` en línea, con cualquier URL (también relativas, `javascript:`, `tel:`…), con aspecto propio del navegador | Propiedad `Link` de una caja, que la hace clicable entera, o `[texto](destino)` de Markdown, que subraya el tramo; solo `https://`, `http://`, `mailto:` y `#id`; sin aspecto por defecto en una caja ([§5.20](#520-enlaces)) |
| Número de la página de otro elemento | Contadores (`target-counter`, solo en impresión paginada y con poco soporte) | `{page of: id}` en el texto, resuelto paginando hasta que converja ([§5.19](#519-referencias-a-otras-cajas)) |
| Grid `dense` | Sí | No |
| Subgrid | Sí | No (posible ampliación) |
| Escritura vertical | Sí | No |

---

## 9. Schema

Fichero: [`src/main/resources/stxt/dev.stxt.boxes.stxt`](../src/main/resources/stxt/dev.stxt.boxes.stxt),
un schema `@stxt.schema` con el namespace `dev.stxt.boxes`. Define cada nodo una sola vez, con
su tipo, su descripción y los hijos que admite (cada propiedad, como mucho una vez). Un
elemento solo puede tener los hijos que declara su definición, así que el schema es lo que
garantiza que cada propiedad está en un elemento donde tiene efecto.

Este resumen da los hijos de cada nodo con hijos; si difiere del fichero, manda el fichero.
Todos los hijos, salvo los elementos, admiten como mucho uno.

| Nodo | Tipo | Hijos |
|---|---|---|
| `Document` | `GROUP` | `Media` (exactamente uno), `Title`, `Language`, `Typography`, `Styles`; elementos |
| `Media` | `GROUP` | `Width` y `Height` (obligatorios), `Margin` |
| `Styles` | `GROUP` | `Element`, `For class`, `For id`, `For column` (varios de cada) |
| `Element`, `For class`, `For id`, `For column` | `INLINE` | `Box`, `Typography`, `Break` |
| `Block` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Isolate`; elementos |
| `Flex` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Direction`, `Wrap`, `Gap`, `Main axis`, `Cross axis`, `Lines`; `Item`, `Absolute`, `Fixed` |
| `Grid` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Columns`, `Rows`, `Gap`, `Areas`, `Flow`, `Horizontal`, `Vertical`; `Area`, `Absolute`, `Fixed` |
| `Text`, `Paragraph` | `INLINE` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Content` |
| `Heading 1` … `Heading 6` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`; `Text` (al menos uno, varios) |
| `List` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Marker` (obligatorio), `Start`; `List item` (al menos uno, varios) |
| `List item` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Number`; elementos salvo los encabezados (al menos uno) |
| `Table` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Columns` (obligatorio), `Column align`, `Gap`; `Head`, `Body` (obligatorio), `Foot` |
| `Head`, `Body`, `Foot` | `GROUP` | `Row` (al menos uno, varios) |
| `Row` | `GROUP` | `Typography`, `Break`; `Cell` (al menos uno, varios) |
| `Cell` | `INLINE` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Span`; elementos salvo los encabezados |
| `Multicol` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Count`, `Column width`, `Column gap`, `Rule`, `Rule color`, `Fill`; elementos en flujo |
| `Markdown` | `GROUP` | `Id`, `Class`, `Box`, `Typography`, `Break`, `Bullet`, `Numbering`, `Table columns`, `Content` (obligatorio) |
| `Contents` | `GROUP` | `Id`, `Class`, `Box`, `Typography`, `Break`, `Levels`, `Leader` |
| `Image` | `INLINE` | `Id`, `Class`, `Link`, `Box`, `Break`, `Alt`, `Fit` |
| `Relative` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Break`, `Isolate`, `Offset`, `Layer`; elementos |
| `Absolute` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Offset`, `Layer`; elementos |
| `Fixed` | `GROUP` | `Id`, `Class`, `Link`, `Box`, `Typography`, `Offset`, `Layer`, `Pages`; elementos |
| `Item` | `GROUP` | `Grow`, `Shrink`, `Basis`, `Cross axis`, `Layer`; un elemento en flujo |
| `Area` | `INLINE` | `Horizontal`, `Vertical`, `Layer`; un elemento en flujo |
| `Box` | `GROUP` | `Width`, `Height`, `Min width`, `Max width`, `Min height`, `Max height`, `Aspect ratio`, `Margin`, `Padding`, `Border`, `Border color`, `Background` |
| `Typography` | `GROUP` | `Font`, `Weight`, `Style`, `Size`, `Line height`, `Align`, `Color`, `Text wrap`, `Widows`, `Orphans` |
| `Break` | `GROUP` | `Before`, `After`, `Inside`, `Decoration` |

*Elementos* son `Block`, `Flex`, `Grid`, `Text`, `Paragraph`, `Heading 1` … `Heading 6`,
`List`, `Table`, `Multicol`, `Markdown`, `Contents`, `Image`, `Relative`, `Absolute` y `Fixed`; *elementos en flujo*, todos menos `Absolute` y `Fixed`.

Tipos de las propiedades: `Content` es `BLOCK` (solo `>>`); `Areas`, `TEXT` (en línea o en
bloque); `Isolate` y `Wrap`, `BOOLEAN`; `Layer`, `Widows`, `Orphans`, `Start`, `Number` y
`Count`, `INTEGER`; las de valores
cerrados (`Table columns`, `Fit`, `Leader` y `Pages` entre ellas), `ENUM` con los valores del §4; el resto (`Bullet`, `Numbering`, `Levels`, `Alt` y `Link` entre ellos), `INLINE` sin hijos, y su forma la comprueba el
motor ([§3](#3-valores)). Los `GROUP` no admiten valor: `Block: algo` es un error.

Lo que el schema no puede expresar lo comprueba el motor al construir el árbol ([§6](#6-validación)):
que `Item` y `Area` tengan exactamente un elemento (el schema solo limita cada tipo a uno) y un
`List item` al menos uno (el schema solo cuenta por tipo), que
un `Text` tenga su texto en un sitio y solo uno, que la raíz sea `Document` (en un schema
cualquier nodo puede ser raíz) y lo que depende de dónde está un elemento: que un `Text` de un
encabezado no tenga `Break`, y la jerarquía y el sitio de los encabezados
([§5.13](#513-encabezados-y-párrafos)). En una `Table`, el motor comprueba además el orden
`Head`, `Body`, `Foot`, que una `Cell` no tenga a la vez texto en el valor y elementos, y que
cada fila llene exactamente sus columnas. El contenido de un `Markdown` lo traduce el motor, y
lo generado pasa por el schema como lo que escribe el autor.

Un mismo nombre es el mismo nodo en todo el namespace: el `Width` de `Media` y el de `Box`
comparten definición, y solo cambia la cardinalidad que fija cada padre. Por eso `Cross axis`,
`Horizontal`, `Vertical` y `Gap` tienen los mismos valores en el contenedor y en `Item` o
`Area`; `Columns` y `Gap` significan lo mismo en `Grid` y en `Table`, y `Text wrap` no se
acorta a `Wrap`, que ya es de `Flex`.

---

## 10. Ejemplo completo

Fichero: [`test/spec/report.stxt`](../test/spec/report.stxt).

```
Document (dev.stxt.boxes):
	Media:
		Width: 595
		Height: 842
		Margin: 40
	Typography:
		Font: IBM Plex Sans
		Size: 10
	Styles:
		Element: Fixed
			Box:
				Height: 20
			Typography:
				Size: 8
		Element: Text
			Typography:
				Align: justify
		For class: panel
			Box:
				Background: #F1EFE8
				Padding: 10
			Typography:
				Align: left
			Break:
				Inside: avoid
		For id: title
			Box:
				Margin: 0 0 16 0
			Typography:
				Weight: bold
				Size: 20
				Line height: 24
			Break:
				After: avoid
	Fixed:
		Id: header
		Offset: -30 0 auto 0
		Typography:
			Color: #5F5E5A
		Text: Informe trimestral
	Fixed:
		Id: footer
		Offset: auto 0 -30 0
		Text: Página {page} de {pages}
			Typography:
				Align: right
	Text: Resultados del trimestre
		Id: title
	Grid:
		Id: body
		Columns: 150 1fr
		Gap: 20
		Area:
			Text:
				Id: side
				Class: panel
				Content >>
					Resumen de las cifras clave
					del trimestre.
		Area:
			Text:
				Id: main
				Content >>
					Primer párrafo del informe. Los saltos de línea simples
					dentro del bloque equivalen a espacios.

					Una línea en blanco empieza un párrafo nuevo.
	Absolute:
		Id: stamp
		Offset: 0 0 auto auto
		Box:
			Width: 80
			Height: 30
			Border: 1
			Border color: #A32D2D
		Layer: 1
		Text: BORRADOR
			Typography:
				Align: center
```

**Cómo se maqueta:**

- **Área útil:** 595 − 2·40 = **515 pt** de ancho y 842 − 2·40 = **762 pt** de alto, desde
  (40, 40).
- **Fuentes:** el `Typography` de `Document` declara `Font` y `Size`, que heredan todas las cajas. El título
  cambia el tamaño y va en negrita; la cabecera y el pie declaran un cuerpo menor, que heredan
  sus `Text`.
- **Reglas** ([§4.10](#410-reglas-reutilizables-styles)): `Element: Fixed` da a la cabecera y
  al pie 20 de alto y el cuerpo 8, que heredan sus `Text`. `Element: Text` justifica todos los
  textos; en los de una sola línea (título, cabecera) no se nota, porque la última línea de un
  párrafo no se justifica. El pie y el sello lo sobrescriben con su propio `Align`, y la clase
  `panel` de `side` lo devuelve a `left`: una regla `For class` gana a la de `Element`. La
  regla `For id: title` lleva todo el estilo del título, que en el documento solo declara su
  texto y su `Id`. El resultado es el mismo que si cada elemento declarase esas propiedades.
- **`header`** (`Fixed`): izquierda y derecha 0 con ancho `auto`, así que se estira a 515 pt.
  Arriba −30 la coloca en y = 40 − 30 = **10**, dentro del margen superior. Se repite en cada
  página.
- **`footer`** (`Fixed`): abajo −30 con alto 20 da arriba = 762 + 30 − 20 = 772, es decir,
  y = 40 + 772 = **812**, dentro del margen inferior. `{page}` y `{pages}` se sustituyen
  en cada página.
- **`title`**: primer elemento en flujo, en y = **40**, con una línea de 24 pt. Su margen
  inferior deja el cuerpo en y = 40 + 24 + 16 = **80**. Con `After: avoid` en su `Break`, nunca queda
  solo al final de una página.
- **`body`** (`Grid`): 515 − 20 de hueco = 495 para pistas. La columna fija mide **150** y la
  fracción recibe **345**. Los dos `Area` no tienen valor y se colocan automáticamente, uno en
  cada columna: `side` empieza en x = 40 y `main` en x = 40 + 150 + 20 = **210**. `side` no se
  parte (`Inside: avoid`); `main` puede partirse entre páginas respetando viudas y
  huérfanas (2 por defecto).
- **`stamp`** (`Absolute`): no hay ancestro posicionado, así que su containing block es el área
  útil. Arriba 0 y derecha 0 con ancho 80 la anclan a la esquina superior derecha:
  x = 40 + 515 − 80 = **475**, y = **40**. Con `Layer: 1` se pinta por encima del título.

---

## 11. Glosario

| Término | Definición |
|---|---|
| **Área útil** | Rectángulo del medio menos sus márgenes; containing block inicial. |
| **Carpeta de recursos** | Carpeta de las fuentes y las imágenes; los recursos se buscan solo hacia abajo ([§5.11](#511-recursos-y-fuentes)). |
| **Caja de borde / relleno / contenido / margen** | Los cuatro rectángulos concéntricos de una caja ([§5.2](#52-modelo-de-caja-y-containing-block)). |
| **Columna** | Cada fragmentainer en que un `Multicol` reparte su contenido en una página ([§5.16](#516-bloques-en-columnas)). |
| **Containing block** | Rectángulo de referencia para porcentajes, `auto` y desplazamientos. |
| **Contexto de apilamiento** | Grupo de cajas que se pinta como una unidad y ordena a sus hijos por `Layer`. |
| **Contexto de formato de bloque** | Región que aísla el colapso de márgenes. |
| **Definido** | Tamaño conocido sin hacer el layout del contenido. |
| **Elemento** | `Block`, `Flex`, `Grid`, `Text`, `Relative`, `Absolute` o `Fixed`: un nodo del documento que es una caja ([§2](#2-estructura-de-un-documento)). |
| **Elemento en flujo** | Elemento que puede ser un ítem: todos menos `Absolute` y `Fixed`. |
| **Elemento posicionado** | `Relative`, `Absolute` o `Fixed`. |
| **Enlace** | `Link` de una caja o `[texto](destino)` de Markdown: una zona del PDF que lleva a una URL o a otra caja ([§5.20](#520-enlaces)). |
| **Envoltorio** | `Item` o `Area`: no es una caja; añade al elemento que contiene cómo participa en su `Flex` o su `Grid`. |
| **Fragmentainer** | Contenedor de fragmentos: cada página y, dentro de un `Multicol`, cada columna. |
| **Fragmento** | Parte de una caja que cae en un fragmentainer. |
| **Huérfana** (`Orphans`) | Línea de un párrafo que queda sola al final de una página. |
| **Imagen** | Elemento `Image`: una caja sin hijos con un PNG o un JPEG de la carpeta de recursos, su tamaño natural y su proporción ([§5.18](#518-imágenes)). |
| **Markdown** | Elemento cuyo contenido se escribe en un subconjunto de Markdown y se traduce a encabezados, párrafos, listas, tablas e imágenes ([§5.17](#517-markdown)). |
| **Min-content / max-content** | Tamaño mínimo sin desbordar / tamaño sin saltos de línea. |
| **Índice** | Elemento `Contents`: una entrada por encabezado con su título, una guía y su página ([§5.19](#519-referencias-a-otras-cajas)). |
| **Referencia** | `{page of: id}` o `{number of: id}` en un texto: la página donde empieza la caja con ese `Id`, o su número ([§5.19](#519-referencias-a-otras-cajas)). |
| **Pista** | Columna o fila de una rejilla grid. |
| **Posición estática** | Donde habría quedado una caja posicionada si estuviera en flujo. |
| **Tramo** | Parte del texto de una caja con su formato (negrita, cursiva, subrayado, tachado y enlace) ([§5.4](#54-texto)). |
| **Viuda** (`Widows`) | Línea de un párrafo que pasa sola a la página siguiente. |
