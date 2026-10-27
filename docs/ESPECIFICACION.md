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

El nodo raíz es `Document`, con exactamente un `Media`, un `Typography` opcional que heredan
todas las cajas (la fuente del documento), un `Styles` opcional con las reglas reutilizables
([§4.10](#410-reglas-reutilizables-styles)) y los elementos de primer nivel.

| Elemento | Qué es | Contiene |
|---|---|---|
| `Block` | Caja que apila sus hijos en vertical ([§5.3](#53-flujo-de-bloque)) | Elementos |
| `Flex` | Contenedor flex ([§5.7](#57-flex)) | `Item`, `Absolute` y `Fixed` |
| `Grid` | Contenedor grid ([§5.8](#58-grid)) | `Cell`, `Absolute` y `Fixed` |
| `Text` | Caja de texto ([§5.4](#54-texto)) | Su texto: en el valor o en `Content` |
| `Relative` | Bloque que se desplaza con `Offset` sin mover a sus hermanos ([§5.5](#55-posicionamiento)) | Elementos |
| `Absolute` | Bloque fuera de flujo, colocado con `Offset` en su containing block | Elementos |
| `Fixed` | Bloque fuera de flujo, colocado en el área útil y repetido en cada página | Elementos |
| `Item` | Ítem de un `Flex`: cómo crece, encoge y se alinea | Exactamente un elemento en flujo |
| `Cell` | Celda de un `Grid`: dónde va y cómo se alinea | Exactamente un elemento en flujo |

**Elementos** son los siete primeros, y **elementos en flujo**, los cinco que pueden ser un ítem
(`Block`, `Flex`, `Grid`, `Text` y `Relative`); `Absolute` y `Fixed` salen del flujo y nunca
son ítems. Los siete son **elementos de caja**: cada uno es una caja del árbol
([§5.1](#51-árbol-de-cajas)). `Item` y `Cell` no son cajas: envuelven al elemento que hace de
ítem y le añaden lo que solo tiene sentido dentro de su contenedor.

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
| `Typography` ([§4.4](#44-typography)) | Fuente, tamaño, interlineado, alineación y color del texto, viudas y huérfanas | **Sí** | Todos los de caja, y `Document` |
| `Break` ([§4.5](#45-break)) | Cortes de página y decoración en los cortes | No | Todos los de caja salvo `Absolute` y `Fixed` |

Lo que hay en `Typography` lo hereda toda la descendencia; lo de `Box` y `Break` es solo de la
caja que lo declara. No hay cascada: las reglas de `Styles`
([§4.10](#410-reglas-reutilizables-styles)) dan valores por defecto a los elementos de un tipo,
de una clase o con un `Id`, y el elemento los puede sobrescribir. Dentro de cada grupo, una propiedad aparece como mucho una
vez, y los grupos, como mucho uno por elemento. Fuera de los grupos quedan las propiedades que
definen el propio elemento: `Id`, `Class` e `Isolate` ([§4.2](#42-propiedades-del-elemento)), las de
`Flex` y `Grid` ([§4.6](#46-flex), [§4.7](#47-grid)), las de `Item` y `Cell`
([§4.8](#48-item-y-cell)) y las de posicionamiento ([§4.9](#49-posicionamiento)).

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

### 4.2 Propiedades del elemento

| Propiedad | Valores | Por defecto | Elementos | Notas |
|---|---|---|---|---|
| `Id` | texto | — | De caja | Identificador para volcados, tests, mensajes y reglas `For id`. Único en el documento. |
| `Class` | nombres separados por espacios | — | De caja | Clases del elemento: le aplican las reglas `For class` con esos nombres ([§4.10](#410-reglas-reutilizables-styles)). Cada clase necesita su regla y no se repite. |
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
| `Before` | `auto`, `page`, `avoid` | `auto` | `page` fuerza un salto antes de la caja. |
| `After` | `auto`, `page`, `avoid` | `auto` | `avoid` mantiene la caja con la siguiente (títulos). |
| `Inside` | `auto`, `avoid` | `auto` | `avoid` intenta no partir la caja. |
| `Decoration` | `slice`, `clone` | `slice` | Qué pasa con borde y relleno en un corte. |

### 4.6 `Flex`

Propiedades del elemento `Flex`, hijas directas suyas. Sus hijos en flujo son `Item`
([§4.8](#48-item-y-cell)).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Direction` | `row`, `column` | `row` | Eje principal horizontal o vertical. |
| `Wrap` | booleano | `false` | Permite repartir los ítems en varias líneas. |
| `Gap` | 1 o 2 valores (número, %) | `0` | Un valor para ambos ejes, o `filas columnas`. |
| `Main axis` | `start`, `end`, `center`, `between`, `around`, `evenly` | `start` | Reparto del sobrante en el eje principal. |
| `Cross axis` | `start`, `end`, `center`, `stretch` | `stretch` | Alineación de los ítems dentro de su línea. |
| `Lines` | `start`, `end`, `center`, `between`, `around`, `stretch` | `stretch` | Reparto de las líneas en el contenedor. Solo con `Wrap: true`. |

### 4.7 `Grid`

Propiedades del elemento `Grid`, hijas directas suyas. Sus hijos en flujo son `Cell`
([§4.8](#48-item-y-cell)).

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

### 4.8 `Item` y `Cell`

Cada hijo en flujo de un `Flex` es un `Item`, y cada uno de un `Grid`, un `Cell`. Contienen
**exactamente un elemento en flujo** (`Block`, `Flex`, `Grid`, `Text` o `Relative`), que es el
ítem: su `Box`, su `Typography` y demás propiedades van en ese elemento. El `Item` o el `Cell`
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
	Cell: 1 2
		Text: Fila 1, columna 2
	Cell:
		Text: Se coloca automáticamente
```

| Elemento | Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|---|
| `Item` | `Grow` | número ≥ 0 | `0` | Parte del espacio sobrante que recibe. |
| `Item` | `Shrink` | número ≥ 0 | `1` | Parte del déficit que cede, ponderada por `Basis`. |
| `Item` | `Basis` | número, %, `auto` | `auto` | Tamaño de partida en el eje principal. |
| `Item` | `Cross axis` | como en `Flex` | el del contenedor | Sobrescribe la alineación del contenedor. |
| `Cell` | *(valor)* | área o `fila columna [filas columnas]` | `auto` | Dónde va, contando desde 1. Sin valor, se coloca automáticamente. Ver [§5.8](#58-grid). |
| `Cell` | `Horizontal` | como en `Grid` | el del contenedor | |
| `Cell` | `Vertical` | como en `Grid` | el del contenedor | |
| Los dos | `Layer` | entero | `auto` | Orden de apilamiento del ítem. Ver [§5.6](#56-apilamiento-y-orden-de-pintado). |

### 4.9 Posicionamiento

Propiedades de `Relative`, `Absolute` y `Fixed`, los **elementos posicionados**, hijas directas
suyas. Son bloques (apilan sus hijos como un `Block`); para posicionar un flex o un grid, se pone
dentro.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Offset` | 1 a 4 valores (número, %, `auto`) | `auto` | Arriba, derecha, abajo, izquierda. Ver [§5.5](#55-posicionamiento). |
| `Layer` | entero | `auto` | Orden de apilamiento. También en `Item` y `Cell`. Ver [§5.6](#56-apilamiento-y-orden-de-pintado). |

### 4.10 Reglas reutilizables (`Styles`)

`Styles`, hijo opcional de `Document` (como mucho uno), declara una vez propiedades que se
aplican por defecto a muchos elementos. Contiene reglas de tres clases:

| Regla | Valor | Se aplica a |
|---|---|---|
| `Element` | nombre de un elemento de caja (`Block`, `Text`…) | todos los elementos de ese tipo |
| `For class` | un nombre, sin espacios | los elementos con ese nombre en su `Class` |
| `For id` | un `Id`, sin espacios | el elemento con ese `Id` |

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
4. la regla `Element` de su tipo;
5. lo heredado del padre (solo `Typography`);
6. el valor por defecto.

Una regla gana a la herencia: con `Element: Text` y `Size: 10`, un `Text` mide 10 aunque su
padre declare `Size: 20`. Las reglas se mezclan **propiedad a propiedad**, no por grupo: si la
regla declara `Size` y el elemento `Weight`, se aplican las dos. Lo que una regla da a un
elemento en `Typography` lo heredan sus descendientes, como si lo hubiera declarado él.
`Item` y `Cell` no son cajas: las reglas se aplican al elemento que envuelven.

**Validación.** Lo que aporta una regla se valida igual que si lo declarara el elemento
([§6](#6-validación)), y el error señala la línea de la propiedad dentro de la regla. Los valores
de una regla se comprueban aunque no se aplique a ningún elemento. Son errores: un `Element` que
no nombra un elemento de caja, un `For id` sin elemento con ese `Id`, dos reglas con el mismo
selector, una regla que da `Break` a un `Absolute` o un `Fixed`, y una clase de `Class` sin su
regla `For class`. Un `Element` o un `For class` que no se aplican a ningún elemento no son
errores: así las reglas se pueden reutilizar.

El `Typography` de `Document` sigue siendo lo que hereda la raíz; las reglas no se aplican a
`Document`.

---

## 5. Semántica del layout

### 5.1 Árbol de cajas

El motor transforma el documento en un **árbol de cajas**: cada elemento de caja es una caja, y
el área útil es la raíz. `Item` y `Cell` no crean caja: sus propiedades pasan al elemento que
envuelven. No hay cajas anónimas, porque el texto va siempre en su propio `Text`.

Cada elemento determina **cómo coloca a sus hijos**:

| Elemento | Coloca a sus hijos en flujo |
|---|---|
| `Document` (el área útil), `Block`, `Relative`, `Absolute`, `Fixed` | En flujo de bloque ([§5.3](#53-flujo-de-bloque)) |
| `Flex` | En líneas flex ([§5.7](#57-flex)) |
| `Grid` | En una rejilla ([§5.8](#58-grid)) |
| `Text` | No tiene hijos: reparte su texto en líneas ([§5.4](#54-texto)) |

**Participación.** Cómo participa una caja en su padre lo decide el padre: dentro de un bloque
es un bloque apilado; dentro de un `Flex` o un `Grid` (envuelta en `Item` o `Cell`), un ítem.
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
`Absolute` y `Fixed`, los ítems de flex y grid, y los `Block` y `Relative` con `Isolate: true`.
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
  página y el total de páginas. Ver [§5.10](#510-fragmentación).

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
todas las páginas** (cabeceras, pies, marcas de agua). Los `Fixed` no reservan
espacio: el autor debe dejar sitio con el `Margin` del medio. Para colocarlos en el margen de
la página se usan desplazamientos negativos (`Offset: -30 0 auto 0`). Si un `Fixed` depende de
su posición estática (desplazamientos `auto`), se toma la de la primera página en la que
aparece y se repite igual en todas.

**Orden de cálculo.** Las cajas posicionadas fuera de flujo se resuelven cuando su containing
block ya conoce su tamaño, después del layout en flujo.

### 5.6 Apilamiento y orden de pintado

El pintado se organiza en **contextos de apilamiento**. Crean uno: el área útil, los `Fixed`,
los `Relative` y `Absolute` con `Layer` declarada y los ítems de flex o grid con `Layer`
declarada en su `Item` o su `Cell`.

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

En un `Grid`, cuyos ítems son el elemento de cada `Cell`, el motor resuelve primero **dónde** va
cada ítem y después **cuánto** mide cada pista.

**Colocación:**

1. Se construye la rejilla explícita con `Columns`, `Rows` y `Areas`. Cada nombre de `Areas`
   debe formar un rectángulo y todas las filas deben tener el mismo número de celdas.
2. Se colocan los ítems cuyo `Cell` tiene fila y columna, o nombre de área.
3. Se colocan los demás en orden del árbol con un **cursor** que recorre la rejilla por filas
   (o por columnas, con `Flow: column`). Cada ítem va al primer hueco donde quepa. El cursor
   solo avanza, de modo que pueden quedar huecos atrás.
4. Si un ítem cae fuera de la rejilla declarada, se crean pistas implícitas `auto`.

**Valor de `Cell`:** un nombre de área (`Cell: side`) o `fila columna [filas columnas]`
contando desde 1, donde fila y columna pueden ser `auto`: `Cell: 1 1 1 2` ocupa la fila 1 y
las columnas 1 y 2; `Cell: auto auto 1 2` se coloca automáticamente ocupando dos columnas.
`Cell:` sin valor equivale a `Cell: auto auto`.

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

**Proporción.** Con `Aspect ratio` y un eje `auto`, ese eje se deduce del otro.

### 5.10 Fragmentación

**Modelo.** Un *fragmentainer* es el contenedor donde caen los fragmentos de las cajas: uno
por página, con la altura del área útil. Cada caja produce un **fragmento** por página en la que aparece.

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
alta que el área útil, una fila con un ítem que no puede colocar nada) es un error
([§5.12](#512-desbordamiento)).

**Cortes forzados.** `Before: page` y `After: page` (de `Break`) fuerzan un salto. Solo se
aplican a cajas en flujo: `Absolute` y `Fixed` no admiten `Break`. El `Before: page` del primer hijo de un bloque y el
`After: page` del último se propagan al bloque. Un corte forzado al principio de una
página se ignora (no deja páginas en blanco), igual que el `After: page` de la última
caja del documento.

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

**Posicionadas.** Un `Absolute` se coloca en la página donde cae su posición y no se parte. Si
su containing block está partido, se resuelve contra el fragmento de esa página: su alto es el
de ese fragmento, no el total. Un `Fixed` se repite en todas las páginas. Un `Relative` se
parte como cualquier bloque.

**Numeración.** `{page}` y `{pages}` se resuelven en dos fases: se pagina reservando el
ancho de un número de prueba de dos cifras (`00`) y después se sustituyen los valores reales.
Si el total de páginas tiene más cifras (100 páginas o más), antes se vuelve a paginar
reservando tantas cifras como tenga el total, y así hasta que basten.
Si el texto cambia de ancho, solo se vuelve a maquetar esa caja: sus líneas conservan los
saltos y se vuelven a medir y alinear. Como los `Fixed` no ocupan espacio, el flujo principal
no cambia.

### 5.11 Recursos y fuentes

**Carpeta de recursos.** Los ficheros que usa un documento están en una única carpeta, por
defecto `resources/` (relativa al directorio de trabajo del motor), que la aplicación puede
cambiar. Dentro hay dos subcarpetas fijas, que pueden organizarse a su vez en subcarpetas:

```
resources/
	fonts/      fuentes TTF
	images/     imágenes (reservada)
```

Los recursos se buscan **siempre hacia abajo**, desde la raíz de la carpeta. Una ruta de
recurso es relativa a esa raíz (`images/logo.png`), admite `/` y `\` como separadores y no
puede:

- ser absoluta (`/…`, `\…`, `C:…`) ni empezar por `~`;
- contener un segmento `..`, aunque al normalizarla volviera a quedar dentro;
- llegar, a través de un enlace simbólico, a un fichero fuera de la carpeta.

**Fuentes.** Solo se usan las fuentes TTF de `fonts/`; no hay fuentes del sistema, ni las 14
estándar de PDF, ni fuentes de sustitución. La salida PDF las incrusta (solo los glifos usados),
de modo que el documento se ve igual en cualquier máquina; por eso deben tener una licencia que
permita incrustarlas, como la SIL Open Font License. El repositorio incluye IBM Plex Sans.

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
caracteres de su texto (los espacios en blanco se pintan como espacios; `{page}` y `{pages}`,
como cifras). Una fuente desconocida, una variante que falta o un carácter que la fuente no
tiene son errores con la línea de la caja ([§6](#6-validación)). También son errores, sin línea
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
- **contenido que pasa del alto de su caja**: líneas o hijos que no caben en un `Height` o un
  `Max height` (`BOX_OVERFLOW`), en la línea de esa propiedad;
- **algo del flujo que no cabe en el área útil de una página y no se puede
  partir** (`PAGE_OVERFLOW`), en la línea de su `Height` o su `Line height` si los declara;
- **un `Absolute` o un `Fixed` que se sale de la página física** (`BOX_OVERFLOW`), en la
  línea de su `Offset`. Pueden ocupar los márgenes del medio, pero no salir de la página.

El error señala al culpable **más interior**: si una caja se sale porque se sale algo de dentro,
se nombra lo de dentro. Los `Relative` se comprueban en su sitio, sin el desplazamiento de
`Offset`, y un margen negativo cuenta como espacio ganado: no son desbordamientos, sino
decisiones del autor. Tampoco cuentan los márgenes verticales, que colapsan.

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
   elemento: `Offset` en un `Block`, `Grow` en un `Cell`, `Columns` en un `Flex`. El motor
   valida también los documentos que no se han leído con el lector, así que estos errores salen
   siempre con los códigos de la librería.
2. **Modelo:** las reglas que el schema no puede expresar, al construir el árbol de cajas.
3. **Fuentes** ([§5.11](#511-recursos-y-fuentes)): con las métricas reales (la salida PDF),
   antes de maquetar, porque necesitan la carpeta de fuentes.
4. **Desbordamientos** ([§5.12](#512-desbordamiento)): sobre el resultado, después de maquetar
   y paginar.

| Código | Regla | Mensaje (ejemplo) |
|---|---|---|
| `DOCUMENT_ROOT` | Un único nodo raíz, `Document`, con el namespace `dev.stxt.boxes` (el schema acepta cualquier elemento como raíz) | `El nodo raíz debe ser Document (dev.stxt.boxes):` |
| `MEDIA_SIZE` | `Width` y `Height` del medio, puntos positivos; `Margin`, solo puntos, y deja área útil | `Los márgenes del medio no dejan área útil` |
| `VALUE_SYNTAX` | Sintaxis de los valores ([§3](#3-valores)), colores `#RGB` o `#RRGGBB`, `Aspect ratio` | `Valor no válido: 12,5 (usa punto decimal…)` |
| `VALUE_NOT_ALLOWED` | Formas y signos que admite cada propiedad ([§3.1](#31-tipos-de-valor), [§4](#4-referencia-de-propiedades)); `Gap` con 1 o 2 valores; `Size` > 0 | `Width no admite 1fr: usa un número…` |
| `FIT_CONTENT_TRACK` | `fit-content` no es una pista de grid | `fit-content no es una pista de grid: usa auto…` |
| `DUPLICATE_ID` | Los `Id` son únicos | `Id repetido: a (ya se usa en la línea 6)` |
| `SINGLE_ELEMENT` | `Item` y `Cell` contienen exactamente un elemento | `Un Cell contiene exactamente un elemento, y este tiene Block (línea 8) y Text (línea 10)` |
| `TEXT_CONTENT` | Un `Text` tiene su texto en el valor o en `Content`, uno de los dos | `Text tiene el texto en el valor y en Content (línea 9): deja solo uno` |
| `TEXT_WITHOUT_FONT` | El texto necesita `Font` en `Typography`, propia o heredada (también de `Document`) | `El texto necesita Font: declárala en el Typography del Text, de un antecesor o de Document` |
| `TEXT_WITHOUT_FONT_SIZE` | El texto necesita `Size` en `Typography`, propio o heredado (también de `Document`) | `El texto necesita Size: decláralo en el Typography del Text, de un antecesor o de Document` |
| `STYLE_UNKNOWN_ELEMENT` | `Element` nombra un elemento de caja ([§4.10](#410-reglas-reutilizables-styles)) | `Element: 'Item' no es un elemento de caja (Block, Flex…)` |
| `STYLE_UNKNOWN_ID` | Un `For id` tiene un elemento con ese `Id` | `Ningún elemento tiene el Id summary de esta regla For id` |
| `STYLE_DUPLICATE` | Dos reglas no tienen el mismo selector | `Regla repetida: Element: block (ya está en la línea 6)` |
| `STYLE_NOT_ALLOWED` | Una regla no da `Break` a un `Absolute` o un `Fixed` | `La regla For class: page aplica Break a un Absolute (línea 11)…` |
| `CLASS_WITHOUT_RULE` | Cada clase de `Class` tiene su regla `For class` | `La clase aside no tiene regla: añade 'For class: aside' en Styles` |
| `OFFSET_CONFLICT` | Un `Relative` no se desplaza a la vez desde arriba y abajo, ni desde izquierda y derecha | `Un Relative no puede desplazarse a la vez desde arriba y desde abajo…` |
| `OVERCONSTRAINED` | Un `Absolute` o un `Fixed` con inicio, tamaño y fin necesita algún margen `auto` | `Izquierda, Width y derecha declarados fijan la posición dos veces…` |
| `MARGIN_AUTO_WITHOUT_EFFECT` | `Margin: auto` solo donde centra ([§4.3](#43-box)) | `Margin auto no centra una caja que ocupa todo el ancho…` |
| `LINES_WITHOUT_WRAP` | `Lines` solo con `Wrap: true` | `Lines solo tiene efecto con Wrap: true…` |
| `GROW_WITHOUT_EFFECT` | `Grow` en `column` necesita un alto definido en el contenedor | `Grow no tiene efecto: el flex column del padre no tiene un alto definido…` |
| `ASPECT_RATIO_CONFLICT` | `Aspect ratio` no se combina con `Width` y `Height` declarados | `Aspect ratio no tiene efecto si Width y Height están declarados` |
| `MIN_MAX_CONFLICT` | Mínimo, máximo y tamaño comparables no se contradicen ([§5.9](#59-dimensionado)) | `Min width es mayor que Max width (línea 7)` |
| `INDEFINITE_PERCENTAGE` | Un porcentaje vertical necesita un alto definido ([§3.6](#36-bases-de-los-porcentajes)) | `Height usa un porcentaje de un alto que no es definido…` |
| `GRID_PLACEMENT` | `Areas` rectangulares y con filas de igual longitud; valor de `Cell` válido y con áreas existentes | `Área desconocida en Cell: foot` |
| `FONT_UNKNOWN` | `Font` existe en la carpeta de fuentes (línea de la `Font`) | `Fuente desconocida: Arial. Familias en …/resources/fonts: IBM Plex Sans` |
| `FONT_VARIANT_MISSING` | La familia tiene la variante de `Weight` y `Style` (línea del texto) | `La fuente IBM Plex Sans no tiene la variante bold italic… (Font en la línea 6, …)` |
| `GLYPH_MISSING` | La fuente tiene todos los caracteres del texto (línea del carácter) | `La fuente IBM Plex Sans normal no tiene el carácter '漢' (U+6F22)` |
| `TEXT_OVERFLOW` | Ninguna palabra es más ancha que su línea (línea de la palabra) | `La palabra 'supercalifragilística' no cabe en la línea: la línea mide 138.69 y la caja deja 100…` |
| `BOX_OVERFLOW` | Las cajas caben en su contenedor, el contenido en su alto, y los `Absolute` y `Fixed` en la página | `El contenido de la caja de la línea 7 no cabe en su alto: su texto llega a 24 y la caja deja 20` |
| `PAGE_OVERFLOW` | Lo que no se puede partir cabe en una página | `La caja de la línea 9 no cabe en la página y no se puede partir: mide 900 de alto…` |

**Catálogo de errores.** [`src/test/resources/errors/errors.stxt`](../src/test/resources/errors/errors.stxt)
tiene un documento de ejemplo por regla, con el código y la línea que debe dar.

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
Un `Absolute` se anida dentro de su containing block, detrás de sus hijos en flujo, y no dentro
de su padre.
Las cajas sin `Id` pueden incluirse para depurar.

Un **test de referencia** es una carpeta con `input.stxt` (el documento) y `expected.stxt`
(el volcado esperado). Los números de `Rect` se comparan con una tolerancia de 0.01 pt.

---

## 8. Diferencias con CSS

| Tema | CSS | Este modelo |
|---|---|---|
| Documento | HTML + hojas de estilo con cascada y selectores | Un árbol STXT de elementos; cada uno declara sus propiedades y solo admite las que tienen sentido en él |
| Reglas de estilo | Selectores compuestos, combinadores, especificidad calculada, `!important` | Tres selectores simples (`Element`, `For class`, `For id`) con prioridad fija, solo con `Box`, `Typography` y `Break` ([§4.10](#410-reglas-reutilizables-styles)) |
| Errores | Muy permisivo: lo que no tiene efecto se ignora y los conflictos se resuelven con reglas | Estricto: lo que no tiene efecto o se contradice es un error con código y línea ([§6](#6-validación)) |
| Texto suelto entre cajas | Cajas anónimas | No existe: el texto va siempre en un `Text` |
| Herencia | Muchas propiedades | Solo lo que va en `Typography` (fuente, tamaño, interlineado, alineación, color, reparto, viudas y huérfanas) |
| `display` | Combina tipo exterior e interior | El elemento (`Block`, `Flex`, `Grid`, `Text`) fija el interior; el exterior lo decide el padre. No hay `inline-block`. |
| `position` | Propiedad de cualquier caja | Elementos `Relative`, `Absolute` y `Fixed`, que son bloques: un flex o grid posicionado va dentro de uno de ellos |
| Propiedades de ítem (`flex-grow`, `grid-area`…) | En el propio ítem, con efecto solo si el padre es flex o grid | En el envoltorio `Item` o `Cell`, que solo existe dentro de su contenedor |
| `box-sizing` | `content-box` por defecto; configurable | No existe: `Width` y `Height` miden siempre la caja de borde |
| Unidades | px, em, rem, vw… | pt, %, fr |
| `line-height` sin unidad | Multiplicador | **Un número son puntos**; el relativo se escribe con % |
| Altura de línea | Calculada por fuente, *half-leading* y *baseline* | Cada línea mide `Line height`; la línea base se coloca con el *half-leading* de su fuente |
| Fuentes | Del sistema o con `@font-face`, con una lista de alternativas | Solo TTF de la carpeta de recursos, sin alternativas; la fuente es obligatoria y un carácter que no tiene es un error |
| `font-weight`, `font-style` | Pesos de 100 a 900, `oblique`, síntesis de negrita y cursiva | `Weight: normal` o `bold` y `Style: normal` o `italic`, sin síntesis: el fichero debe existir |
| Kerning y ligaduras | Sí | No |
| Palabras largas | Desbordan (configurable) | Error |
| Desbordamiento | Visible por defecto (`overflow: visible`) | Error ([§5.12](#512-desbordamiento)) |
| `float`, tablas, `sticky`, `transform`, `opacity` | Sí | No |
| Medio continuo (pantalla) | Sí | No: siempre páginas |
| `position: fixed` en impresión | Repetido en cada página | Igual: repetido, relativo al área útil |
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
| `z-index` sin contexto de apilamiento | Se ignora | `Layer` solo existe en los elementos posicionados, `Item` y `Cell` |
| `id` repetido | Se tolera | Error |
| Posición estática de un `Absolute` en flex o grid | Como si fuera el único ítem, con la alineación del contenedor | Esquina de la caja de contenido del contenedor |
| Pista `fit-content` en grid | Función `fit-content(límite)`: como `auto`, sin pasar del límite | Error: se usa `auto` |
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
| `Document` | `GROUP` | `Media` (exactamente uno), `Typography`, `Styles`; elementos |
| `Media` | `GROUP` | `Width` y `Height` (obligatorios), `Margin` |
| `Styles` | `GROUP` | `Element`, `For class`, `For id` (varios de cada) |
| `Element`, `For class`, `For id` | `INLINE` | `Box`, `Typography`, `Break` |
| `Block` | `GROUP` | `Id`, `Class`, `Box`, `Typography`, `Break`, `Isolate`; elementos |
| `Flex` | `GROUP` | `Id`, `Class`, `Box`, `Typography`, `Break`, `Direction`, `Wrap`, `Gap`, `Main axis`, `Cross axis`, `Lines`; `Item`, `Absolute`, `Fixed` |
| `Grid` | `GROUP` | `Id`, `Class`, `Box`, `Typography`, `Break`, `Columns`, `Rows`, `Gap`, `Areas`, `Flow`, `Horizontal`, `Vertical`; `Cell`, `Absolute`, `Fixed` |
| `Text` | `INLINE` | `Id`, `Class`, `Box`, `Typography`, `Break`, `Content` |
| `Relative` | `GROUP` | `Id`, `Class`, `Box`, `Typography`, `Break`, `Isolate`, `Offset`, `Layer`; elementos |
| `Absolute`, `Fixed` | `GROUP` | `Id`, `Class`, `Box`, `Typography`, `Offset`, `Layer`; elementos |
| `Item` | `GROUP` | `Grow`, `Shrink`, `Basis`, `Cross axis`, `Layer`; un elemento en flujo |
| `Cell` | `INLINE` | `Horizontal`, `Vertical`, `Layer`; un elemento en flujo |
| `Box` | `GROUP` | `Width`, `Height`, `Min width`, `Max width`, `Min height`, `Max height`, `Aspect ratio`, `Margin`, `Padding`, `Border`, `Border color`, `Background` |
| `Typography` | `GROUP` | `Font`, `Weight`, `Style`, `Size`, `Line height`, `Align`, `Color`, `Text wrap`, `Widows`, `Orphans` |
| `Break` | `GROUP` | `Before`, `After`, `Inside`, `Decoration` |

*Elementos* son `Block`, `Flex`, `Grid`, `Text`, `Relative`, `Absolute` y `Fixed`; *elementos
en flujo*, los cinco primeros.

Tipos de las propiedades: `Content` es `BLOCK` (solo `>>`); `Areas`, `TEXT` (en línea o en
bloque); `Isolate` y `Wrap`, `BOOLEAN`; `Layer`, `Widows` y `Orphans`, `INTEGER`; las de valores
cerrados, `ENUM` con los valores del §4; el resto, `INLINE` sin hijos, y su forma la comprueba el
motor ([§3](#3-valores)). Los `GROUP` no admiten valor: `Block: algo` es un error.

Lo que el schema no puede expresar lo comprueba el motor al construir el árbol ([§6](#6-validación)):
que `Item` y `Cell` tengan exactamente un elemento (el schema solo limita cada tipo a uno), que
un `Text` tenga su texto en un sitio y solo uno, y que la raíz sea `Document` (en un schema
cualquier nodo puede ser raíz).

Un mismo nombre es el mismo nodo en todo el namespace: el `Width` de `Media` y el de `Box`
comparten definición, y solo cambia la cardinalidad que fija cada padre. Por eso `Cross axis`,
`Horizontal`, `Vertical` y `Gap` tienen los mismos valores en el contenedor y en `Item` o
`Cell`, y por eso `Text wrap` no se acorta a `Wrap`, que ya es de `Flex`.

---

## 10. Ejemplo completo

Fichero: [`src/test/resources/examples/report.stxt`](../src/test/resources/examples/report.stxt).

```
Document (dev.stxt.boxes):
	Media:
		Width: 595
		Height: 842
		Margin: 40
	Typography:
		Font: IBM Plex Sans
		Size: 10
	Fixed:
		Id: header
		Offset: -30 0 auto 0
		Box:
			Height: 20
		Typography:
			Size: 8
			Color: #5F5E5A
		Text: Informe trimestral
	Fixed:
		Id: footer
		Offset: auto 0 -30 0
		Box:
			Height: 20
		Typography:
			Size: 8
		Text: Página {page} de {pages}
			Typography:
				Align: right
	Text: Resultados del trimestre
		Id: title
		Typography:
			Weight: bold
			Size: 20
			Line height: 24
		Box:
			Margin: 0 0 16 0
		Break:
			After: avoid
	Grid:
		Id: body
		Columns: 150 1fr
		Gap: 20
		Cell:
			Text:
				Id: side
				Box:
					Background: #F1EFE8
					Padding: 10
				Break:
					Inside: avoid
				Content >>
					Resumen de las cifras clave
					del trimestre.
		Cell:
			Text:
				Id: main
				Typography:
					Align: justify
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
  fracción recibe **345**. Los dos `Cell` no tienen valor y se colocan automáticamente, uno en
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
| **Containing block** | Rectángulo de referencia para porcentajes, `auto` y desplazamientos. |
| **Contexto de apilamiento** | Grupo de cajas que se pinta como una unidad y ordena a sus hijos por `Layer`. |
| **Contexto de formato de bloque** | Región que aísla el colapso de márgenes. |
| **Definido** | Tamaño conocido sin hacer el layout del contenido. |
| **Elemento** | `Block`, `Flex`, `Grid`, `Text`, `Relative`, `Absolute` o `Fixed`: un nodo del documento que es una caja ([§2](#2-estructura-de-un-documento)). |
| **Elemento en flujo** | Elemento que puede ser un ítem: todos menos `Absolute` y `Fixed`. |
| **Elemento posicionado** | `Relative`, `Absolute` o `Fixed`. |
| **Envoltorio** | `Item` o `Cell`: no es una caja; añade al elemento que contiene cómo participa en su `Flex` o su `Grid`. |
| **Fragmentainer** | Contenedor de fragmentos: cada página. |
| **Fragmento** | Parte de una caja que cae en un fragmentainer. |
| **Huérfana** (`Orphans`) | Línea de un párrafo que queda sola al final de una página. |
| **Min-content / max-content** | Tamaño mínimo sin desbordar / tamaño sin saltos de línea. |
| **Pista** | Columna o fila de una rejilla grid. |
| **Posición estática** | Donde habría quedado una caja posicionada si estuviera en flujo. |
| **Viuda** (`Widows`) | Línea de un párrafo que pasa sola a la página siguiente. |
