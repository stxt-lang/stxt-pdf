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
9. [Template completo](#9-template-completo)
10. [Ejemplo completo](#10-ejemplo-completo)
11. [Glosario](#11-glosario)

---

## 1. Introducción

### 1.1 Qué es

Un documento describe un **árbol de cajas**. El motor calcula la posición y el tamaño de cada
caja dentro de un **medio**, que puede ser:

- **Continuo** (`continuous`): una franja de ancho fijo y altura infinita, como la ventana de un
  navegador.
- **Paginado** (`paged`): una secuencia de páginas de tamaño fijo, como un PDF. Las cajas que
  no caben se parten entre páginas.

El modelo es un subconjunto de los mecanismos de maquetación de CSS (flujo de bloque, texto,
posicionamiento, flex y grid, fragmentación) sin HTML y sin cascada. Las diferencias se
resumen en el [§8](#8-diferencias-con-css).

### 1.2 Convenciones

- **Todas las medidas están en puntos tipográficos** (1 pt = 1/72 de pulgada). Un A4 mide
  595 × 842 pt.
- **Los nombres de nodos y valores están en inglés** y siguen, cuando existe, el término de CSS:
  `Margin`, `Padding`, `Position: absolute`, `Break inside: avoid`. El contenido de `Text`
  puede estar en cualquier idioma.
- **Los ejes:** *horizontal* es el de `Width` e *izquierda/derecha*; *vertical* es el de
  `Height` y *arriba/abajo*. La escritura es siempre horizontal, de izquierda a derecha.
- **Las cuatro cajas de una caja** (ver [§5.2](#52-modelo-de-caja-y-containing-block)): de
  contenido, de relleno, de borde y de margen.
- **Caja de referencia:** salvo que se diga otra cosa, las posiciones y los tamaños se refieren
  a la **caja de borde**.

---

## 2. Estructura de un documento

Un documento tiene un nodo raíz `Document` con exactamente un `Media` y una o más `Box`.
Cada `Box` puede contener propiedades, `Text` y otras `Box`.

```
Document (dev.stxt.boxes):
	Media:
		Type: paged
		Width: 595
		Height: 842
		Margin: 40
	Box:
		Id: title
		Font size: 20
		Text: Un título
	Box:
		Id: body
		Padding: 10
		Box:
			Text >>
				Primer párrafo.

				Segundo párrafo.
```

**El orden importa.** STXT conserva el orden de los nodos, y el motor coloca los `Text` y las
`Box` hijas en el orden en que aparecen. El orden de las propiedades no importa.

**Texto inline o en bloque.** `Text: una línea` y `Text >>` con un bloque indentado son
equivalentes; el bloque permite párrafos y texto largo sin caracteres de escape.

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

Un valor que depende de una base indefinida (por ejemplo, un porcentaje de una altura
desconocida) se comporta como `auto`. Ver [§5.9](#59-dimensionado).

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
| `Height`, `Min height`, `Max height` | el alto del containing block, **si es definido**; si no, `auto` |
| `Margin`, `Padding` (los cuatro lados) | el **ancho** del containing block, también arriba y abajo |
| `Offset` arriba/abajo | el alto del containing block |
| `Offset` izquierda/derecha | el ancho del containing block |
| `Basis` (ítem flex) | el tamaño del contenedor flex en el eje principal |
| `Gap` | el tamaño de la caja de contenido del contenedor en ese eje |
| Pistas de grid | el tamaño de la caja de contenido del contenedor en ese eje |
| `Line height` | el `Font size` de la propia caja |

`Border` no admite porcentajes.

---

## 4. Referencia de propiedades

En las tablas, **Hereda** indica si una caja que no declara la propiedad toma el valor de su
padre. No hay cascada: solo se heredan las propiedades marcadas.

### 4.1 `Media`

Obligatorio, exactamente uno, hijo de `Document`.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Type` | `continuous`, `paged` | — (obligatorio) | |
| `Width` | número > 0 | — (obligatorio) | Ancho de la página o de la franja. |
| `Height` | número > 0 | — | Obligatorio en modo paginado. En modo continuo se ignora. |
| `Margin` | 1 a 4 números | `0` | Márgenes de página. Solo puntos. |

El **área útil** es el rectángulo del medio menos sus márgenes. Es el containing block de las
cajas de primer nivel y de las cajas `fixed`. En modo continuo su altura es infinita.

### 4.2 Tipo y posición de la caja

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Id` | texto | — | No | Identificador para volcados, tests y mensajes. |
| `Layout` | `block`, `inline`, `flex`, `grid` | `block` | No | Cómo coloca la caja a **sus hijos**. Ver [§5.1](#51-árbol-de-cajas). |
| `Position` | `static`, `relative`, `absolute`, `fixed` | `static` | No | Cómo participa la caja en el flujo de su padre. Ver [§5.5](#55-posicionamiento). |
| `Offset` | 1 a 4 valores (número, %, `auto`) | `auto` | No | Arriba, derecha, abajo, izquierda. No se admite con `Position: static`. |
| `Layer` | entero | `auto` | No | Orden de apilamiento. Ver [§5.6](#56-apilamiento-y-orden-de-pintado). |
| `Isolate` | booleano | `false` | No | Crea un contexto de formato de bloque nuevo: corta el colapso de márgenes con el padre. |

### 4.3 Dimensiones

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Box sizing` | `border`, `content` | `border` | No | Qué miden `Width` y `Height`: la caja de borde o la de contenido. |
| `Width` | número, %, `auto`, intrínsecos, funciones | `auto` | No | |
| `Height` | número, %, `auto`, intrínsecos, funciones | `auto` | No | |
| `Min width`, `Min height` | número, %, `auto` | `auto` | No | `auto` = min-content en ítems de flex y grid; 0 en el resto. |
| `Max width`, `Max height` | número, % | sin máximo | No | |
| `Aspect ratio` | `ancho/alto` o número | — | No | Si un eje es `auto` y el otro es conocido, se deduce. Ejemplo: `16/9`. |

### 4.4 Modelo de caja

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Margin` | 1 a 4 valores (número, %, `auto`) | `0` | No | Puede ser negativo. `auto` centra en flujo de bloque. |
| `Padding` | 1 a 4 valores (número ≥ 0, % ≥ 0) | `0` | No | |
| `Border` | 1 a 4 números ≥ 0 | `0` | No | Grosor. El color va en `Border color`. |

### 4.5 Pintado

No afectan al layout.

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Background` | color | transparente | No | Se pinta en la caja de relleno. |
| `Color` | color | `#000000` | **Sí** | Color del texto. |
| `Border color` | color | el de `Color` | No | |

### 4.6 Texto

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Font` | nombre | `Helvetica` | **Sí** | En la salida PDF, una de las 14 fuentes estándar o una fuente registrada. |
| `Font size` | número > 0 | `10` | **Sí** | Cuerpo de la fuente. |
| `Line height` | número o % | `120%` | **Sí** | **Un número son puntos** (`15` = 15 pt). Para un valor relativo, usa porcentaje (`120%`). |
| `Text align` | `left`, `right`, `center`, `justify` | `left` | **Sí** | La última línea de un párrafo justificado se alinea a la izquierda. |
| `Text wrap` | `auto`, `pretty` | `auto` | **Sí** | Reparto en líneas: voraz (`auto`) o Knuth-Plass (`pretty`). Ver [§5.4](#54-texto). |
| `Text` | texto inline o bloque | — | — | Contenido. Ver [§5.4](#54-texto). |

### 4.7 `Flex`

Solo con `Layout: flex`.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Direction` | `row`, `column` | `row` | Eje principal horizontal o vertical. |
| `Wrap` | booleano | `false` | Permite repartir los ítems en varias líneas. |
| `Gap` | 1 o 2 valores | `0` | Un valor para ambos ejes, o `filas columnas`. |
| `Main axis` | `start`, `end`, `center`, `between`, `around`, `evenly` | `start` | Reparto del sobrante en el eje principal. |
| `Cross axis` | `start`, `end`, `center`, `stretch` | `stretch` | Alineación de los ítems dentro de su línea. |
| `Lines` | `start`, `end`, `center`, `between`, `around`, `stretch` | `stretch` | Reparto de las líneas en el contenedor (solo con `Wrap`). |

### 4.8 `Grid`

Solo con `Layout: grid`.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Columns` | lista de pistas | una columna `auto` | Ver [§5.8](#58-grid). |
| `Rows` | lista de pistas | filas implícitas `auto` | |
| `Gap` | 1 o 2 valores | `0` | Un valor para ambos ejes, o `filas columnas`. |
| `Areas` | bloque de texto | — | Una fila por línea; nombres separados por espacios; `.` = celda vacía. |
| `Flow` | `row`, `column` | `row` | Dirección de la colocación automática. |
| `Horizontal` | `start`, `end`, `center`, `stretch` | `stretch` | Alineación horizontal de los ítems en su celda. |
| `Vertical` | `start`, `end`, `center`, `stretch` | `stretch` | Alineación vertical de los ítems en su celda. |

Pistas válidas: número, %, `auto`, `min-content`, `max-content`, fracción, `minmax(a, b)` y
`repeat(n, patrón)`.

### 4.9 `Item`

Solo en hijos **en flujo** de un contenedor `flex` o `grid`.

| Propiedad | Valores | Por defecto | Aplica a | Notas |
|---|---|---|---|---|
| `Grow` | número ≥ 0 | `0` | flex | Parte del espacio sobrante que recibe. |
| `Shrink` | número ≥ 0 | `1` | flex | Parte del déficit que cede, ponderada por `Basis`. |
| `Basis` | número, %, `auto` | `auto` | flex | Tamaño de partida en el eje principal. |
| `Cross axis` | como en `Flex` | el del contenedor | flex | Sobrescribe la alineación del contenedor. |
| `Cell` | área o `fila columna [filas columnas]` | `auto` | grid | Contando desde 1. Ver [§5.8](#58-grid). |
| `Horizontal` | como en `Grid` | el del contenedor | grid | |
| `Vertical` | como en `Grid` | el del contenedor | grid | |

### 4.10 Fragmentación

Solo tienen efecto en modo paginado.

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Break before` | `auto`, `page`, `avoid` | `auto` | No | `page` fuerza un salto antes de la caja. |
| `Break after` | `auto`, `page`, `avoid` | `auto` | No | `avoid` mantiene la caja con la siguiente (títulos). |
| `Break inside` | `auto`, `avoid` | `auto` | No | `avoid` intenta no partir la caja. |
| `Widows` | entero ≥ 1 | `2` | **Sí** | Mínimo de líneas de un párrafo que pasan a la página siguiente. |
| `Orphans` | entero ≥ 1 | `2` | **Sí** | Mínimo de líneas de un párrafo que quedan al final de una página. |
| `Decoration` | `slice`, `clone` | `slice` | No | Qué pasa con borde y relleno en un corte. |

---

## 5. Semántica del layout

### 5.1 Árbol de cajas

El motor transforma el documento en un **árbol de cajas**, que no coincide exactamente con los
nodos `Box`:

- **Cajas anónimas.** Si una caja mezcla `Text` con `Box` hijas, cada `Text` se envuelve en
  una caja anónima con `Layout: inline`, en su posición dentro del orden. Lo mismo ocurre con un
  `Text` dentro de un contenedor `flex` o `grid`, que se convierte en un ítem anónimo. Las
  cajas anónimas heredan las propiedades heredables y no tienen margen, borde ni relleno.
- **Bloque con solo texto.** Una caja `block` cuyo único contenido es `Text` se maqueta con el
  algoritmo de línea, sin caja anónima.
- **`Layout: inline`** solo puede contener `Text`.

**`Layout` frente a participación.** `Layout` indica cómo coloca la caja a sus hijos. Cómo
participa la caja en su padre lo decide el padre: dentro de un `block` es un bloque apilado;
dentro de un `flex` o un `grid`, un ítem. La excepción son las cajas `absolute` y `fixed`, que
salen del flujo del padre.

### 5.2 Modelo de caja y containing block

Cada caja tiene cuatro rectángulos concéntricos: **contenido**, **relleno** (contenido +
`Padding`), **borde** (relleno + `Border`) y **margen** (borde + `Margin`).

Con `Box sizing: border` (por defecto), `Width` y `Height` miden la caja de borde: una caja con
`Width: 200` ocupa 200 pt antes de los márgenes. Con `Box sizing: content`, miden solo el
contenido.

El **containing block** de una caja es el rectángulo de referencia para porcentajes, `auto` y
`Offset`:

| `Position` de la caja | Containing block |
|---|---|
| `static`, `relative` | caja de **contenido** del padre |
| `absolute` | caja de **relleno** del ancestro más cercano con `Position` distinta de `static`; si no hay ninguno, el área útil |
| `fixed` | área útil del medio (de cada página, en modo paginado) |

### 5.3 Flujo de bloque

Con `Layout: block`, los hijos en flujo se apilan en vertical:

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

**Contextos de formato de bloque.** Aíslan el colapso de márgenes. Los crean: el área útil, las
cajas `absolute` y `fixed`, los ítems de flex y grid, y las cajas con `Isolate: true`. Los
márgenes de los ítems de flex y grid nunca colapsan.

### 5.4 Texto

El texto de una caja `inline` se reparte en **líneas** del ancho de su caja de contenido:

- **Espacios.** Las secuencias de espacios y los saltos de línea simples equivalen a un espacio.
  Una **línea en blanco** separa párrafos y fuerza un salto.
- **Reparto.** Con `Text wrap: auto`, algoritmo voraz: las palabras se añaden a la línea
  mientras quepan. Con `Text wrap: pretty`, **Knuth-Plass**: se eligen los saltos del párrafo
  que minimizan la suma de los deméritos de sus líneas, de modo que las líneas quedan más
  parejas (sin guiones; los espacios se estiran hasta la mitad de su ancho y no se encogen; la
  última línea no cuenta). En los dos casos, una palabra más ancha que la línea ocupa una línea
  propia y **desborda**.
- **Altura.** Cada línea mide `Line height`. La altura del contenido es el número de líneas
  por `Line height`.
- **Alineación.** `left`, `right` y `center` desplazan cada línea; `justify` reparte
  el sobrante entre los espacios, salvo en la última línea de cada párrafo.
- **Tamaños intrínsecos.** min-content = la palabra más ancha; max-content = el párrafo más
  largo en una sola línea.
- **Numeración de páginas.** `{page}` y `{pages}` en el texto se sustituyen por el número de
  página y el total de páginas; en modo continuo, por 1 y 1. Ver [§5.10](#510-fragmentación).

### 5.5 Posicionamiento

**`relative`.** La caja se coloca como si fuera `static` y después se desplaza visualmente según
`Offset`. Su hueco original se conserva y sus hermanos no se mueven. Si se declaran
arriba y abajo, gana arriba; si se declaran izquierda y derecha, gana izquierda. Una caja
`relative` sin desplazamiento sirve como containing block de sus descendientes `absolute`.

**`absolute`.** La caja sale del flujo: no ocupa espacio ni contribuye a la altura `auto` de su
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
| ninguno, sin márgenes `auto` | sobre-restringida: se ignora el fin |
| inicio y tamaño | tamaño automático; se despeja inicio (anclada al fin) |
| inicio y fin | inicio = posición estática; se despeja fin |
| tamaño y fin | tamaño automático; se despeja fin |
| solo inicio | se despeja inicio |
| solo tamaño | se despeja: la caja **se estira** entre inicio y fin |
| solo fin | se despeja fin |

En los casos con algún término `auto`, los márgenes `auto` valen 0. El **tamaño automático** es
el ajuste al contenido (`fit-content`) en horizontal y la altura del contenido en vertical. La
**posición estática** es donde habría quedado la caja si fuera `static`.

**`fixed`.** Como `absolute`, pero su containing block es el área útil. En modo paginado **se
pinta en todas las páginas** (cabeceras, pies, marcas de agua). En modo continuo no hay ventana,
y se comporta como `absolute` respecto al área útil del documento. Las cajas `fixed` no reservan
espacio: el autor debe dejar sitio con el `Margin` del medio. Para colocarlas en el margen de
la página se usan desplazamientos negativos (`Offset: -30 0 auto 0`).

**Orden de cálculo.** Las cajas posicionadas fuera de flujo se resuelven cuando su containing
block ya conoce su tamaño, después del layout en flujo. En modo continuo, cuando el containing
block es el área útil, su alto es el del flujo del documento (el área útil no tiene alto).

### 5.6 Apilamiento y orden de pintado

El pintado se organiza en **contextos de apilamiento**. Crean uno: el área útil, las cajas
`fixed`, las cajas posicionadas (`Position` distinta de `static`) con `Layer` declarada y los
ítems de flex o grid con `Layer` declarada.

Dentro de cada contexto, el orden de pintado de abajo arriba es:

1. Fondo y borde de la caja que crea el contexto.
2. Contextos hijos con `Layer` negativa, de menor a mayor.
3. Cajas en flujo en orden del árbol: primero todos los fondos y bordes, después todo el texto.
4. Cajas posicionadas sin `Layer` o con `Layer: 0`, en orden del árbol.
5. Contextos hijos con `Layer` positiva, de menor a mayor.

A igual `Layer`, se pinta en orden del árbol. **`Layer` solo compara dentro del mismo contexto:**
el contenido de un contexto se pinta como un bloque al nivel de ese contexto, aunque sus hijos
tengan `Layer` mayores.

### 5.7 Flex

Con `Layout: flex`, los ítems se colocan en el **eje principal** (`Direction`) y se alinean en
el **eje cruzado**.

**Algoritmo:**

1. Espacio disponible: la caja de contenido del contenedor en el eje principal.
2. **Base** de cada ítem: `Basis` si tiene valor; si es `auto`, el tamaño declarado en el eje
   principal; si tampoco lo hay, su max-content.
3. **Tamaño hipotético:** la base limitada por el mínimo y el máximo del ítem. Con
   `Min width: auto`, el mínimo es el **min-content** del ítem.
4. **Líneas:** sin `Wrap`, una sola. Con `Wrap`, los ítems se añaden a la línea mientras
   sus tamaños hipotéticos y los huecos quepan.
5. **Tamaños flexibles** (por línea), ver abajo.
6. **Tamaño cruzado:** layout de cada ítem con su tamaño principal final (en `row`, el texto se
   redistribuye con el ancho final). La línea mide lo que el ítem más alto; sin `Wrap` y con
   tamaño cruzado definido en el contenedor, la única línea mide lo que el contenedor.
7. **Estirar:** los ítems con `stretch` y sin tamaño cruzado declarado pasan a medir lo que la
   línea, y se repite su layout.
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

**Comportamientos a tener en cuenta:**

- En `column` con `Height: auto` no hay espacio libre: `Grow` no tiene efecto.
- `Basis: 0` con `Grow: 1` reparte todo el espacio (columnas iguales); `Basis: auto` con
  `Grow: 1` solo reparte el sobrante sobre el tamaño del contenido.
- Un ítem no encoge por debajo de su min-content salvo que declare `Min width: 0`.

### 5.8 Grid

Con `Layout: grid`, el motor resuelve primero **dónde** va cada ítem y después **cuánto** mide
cada pista.

**Colocación:**

1. Se construye la rejilla explícita con `Columns`, `Rows` y `Areas`. Cada nombre de `Areas`
   debe formar un rectángulo y todas las filas deben tener el mismo número de celdas.
2. Se colocan los ítems con `Cell` explícita (fila y columna, o nombre de área).
3. Se colocan los demás en orden del árbol con un **cursor** que recorre la rejilla por filas
   (o por columnas, con `Flow: column`). Cada ítem va al primer hueco donde quepa. El cursor
   solo avanza, de modo que pueden quedar huecos atrás.
4. Si un ítem cae fuera de la rejilla declarada, se crean pistas implícitas `auto`.

**Sintaxis de `Cell`:** un nombre de área (`Cell: side`) o `fila columna [filas columnas]`
contando desde 1, donde fila y columna pueden ser `auto`: `Cell: 1 1 1 2` ocupa la fila 1 y
las columnas 1 y 2; `Cell: auto auto 1 2` se coloca automáticamente ocupando dos columnas.

**Dimensionado de pistas.** Cada pista tiene una función mínima y una máxima: `150` es fija en
ambas; `auto` equivale a `minmax(min-content, max-content)`; `1fr` equivale a
`minmax(auto, 1fr)`, de modo que **una fracción no baja del min-content de su contenido**.

1. **Inicializar:** cada pista recibe una base (su mínimo) y un límite (su máximo; infinito en
   las fracciones).
2. **Aportaciones del contenido:** las pistas intrínsecas toman el min-content o el max-content
   de sus ítems; el exceso de los ítems que ocupan varias pistas se reparte entre las pistas
   intrínsecas que ocupan.
3. **Maximizar:** el espacio libre se reparte a partes iguales entre las pistas no flexibles
   hasta su límite. Una pista `auto` alcanza su max-content antes de que las fracciones reciban
   nada.
4. **Fracciones:** el resto se divide entre las fracciones de forma proporcional. Si una
   fracción necesita más que su parte por su min-content, se fija en su base y se repite el
   reparto.
5. **Estirar:** si no hay fracciones y sobra espacio, se reparte entre las pistas `auto`.

Las **columnas** se dimensionan primero, con los anchos min-content y max-content de los
ítems. Las **filas** después, con la altura de cada ítem maquetado al ancho de sus columnas.
Con `Height: auto` en el contenedor, las filas en fracción no tienen espacio que repartir y
miden en proporción a su contenido: todas las filas `1fr` miden lo que la más alta.

**Alineación en la celda.** Cada ítem ocupa un área formada por sus pistas y los huecos
internos. Con `stretch` (por defecto) y sin `Width`/`Height` declarado, ocupa toda el área y se
repite su layout. Con `start`, `end` o `center`, conserva su tamaño de contenido y se desplaza.

### 5.9 Dimensionado

**Tamaños intrínsecos** (anchos de la caja de margen):

| `Layout` | min-content | max-content |
|---|---|---|
| `inline` | palabra más ancha | párrafo más largo en una línea |
| `block` | máximo del min-content de los hijos | máximo del max-content de los hijos |
| `flex` en `row`, sin `Wrap` | suma del min-content de los ítems + huecos | suma del max-content + huecos |
| `flex` en `row`, con `Wrap` | máximo del min-content de los ítems | suma del max-content + huecos |
| `flex` en `column` | máximo de los ítems | máximo de los ítems |
| `grid` | suma de pistas dimensionadas con restricción min-content | ídem con max-content |

Una caja con `Width` fijo tiene ese valor como min-content y max-content. Las cajas `absolute`
y `fixed` no aportan nada a sus ancestros.

**Definido e indefinido.** Un tamaño es **definido** si se conoce sin hacer el layout del
contenido: un número, el área útil, un porcentaje de algo definido o un ítem estirado por flex
o grid. Los porcentajes de ancho se resuelven siempre; los de alto, solo si el alto del
containing block es definido. En modo paginado el área útil es definida: una caja de primer
nivel con `Height: 100%` ocupa la página. En modo continuo, el mismo valor se comporta como
`auto`.

**Mínimos y máximos.** Se calcula el tamaño provisional, se limita por el máximo y se sube al
mínimo. **Si chocan, gana el mínimo.**

**Proporción.** Con `Aspect ratio` y un eje `auto`, ese eje se deduce del otro.

### 5.10 Fragmentación

**Modelo.** Un *fragmentainer* es el contenedor donde caen los fragmentos de las cajas: en
modo continuo hay uno de altura infinita; en modo paginado, uno por página, con la altura del
área útil. Cada caja produce un **fragmento** por página en la que aparece.

**Oportunidades de corte:**

- entre cajas hermanas en flujo de bloque;
- entre líneas de texto;
- entre líneas de un contenedor flex y entre filas de grid.

Siempre se intenta cortar en el punto más alto del árbol posible; solo si no cabe se corta
dentro de un hijo.

**Elección del corte.** El motor elige el **último** corte de la página que no incumple ninguna
regla. Si no hay ninguno, relaja las reglas en este orden:

1. `Widows` y `Orphans`;
2. `Break before/after: avoid`;
3. `Break inside: avoid`.

Si sigue sin haber corte posible, corta donde se acaba el espacio.

**Garantía de progreso.** Al principio de una página siempre se coloca algo, aunque desborde o
haya que cortarlo a la fuerza. Una caja monolítica más alta que una página no provoca un bucle
infinito.

**Cortes forzados.** `Break before: page` y `Break after: page` fuerzan un salto. Solo se
aplican a cajas en flujo.

**Márgenes en los cortes.** En un corte no forzado, los márgenes que tocan el corte se truncan
a cero. En un corte forzado, se trunca el margen anterior al corte y se conserva el posterior.

**Bordes y relleno en los cortes.** Con `Decoration: slice`, el primer fragmento no tiene
borde ni relleno inferiores y los siguientes no tienen los superiores. Con `Decoration: clone`,
cada fragmento se pinta como una caja completa: el contenido de cada página cabe entre su borde
y relleno superiores e inferiores. Un `Height` fijo es el total de todos los fragmentos, no el
de cada uno; con `clone`, el total del contenido es ese alto menos un borde y un relleno, y
cada fragmento les suma los suyos.

**Flex y grid.** En `column`, el corte se hace entre ítems. En `row` y en grid, entre líneas
o filas. Una línea, fila o ítem que no cabe en lo que queda de página pasa entero a la
siguiente. Si tampoco cabe al principio de una página, sus ítems se fragmentan **en
paralelo**: cada uno se parte por su cuenta y la fila continúa en la página siguiente con los
ítems que no han terminado, todos desde arriba, y con la altura del más largo de ellos; las
filas siguientes van detrás. Las filas que ocupa un mismo ítem se parten juntas. Un contenedor
`column` con `Wrap` no se parte.

**Posicionadas.** Una caja `absolute` se coloca en la página donde cae su posición y no se
parte. Una caja `fixed` se repite en todas las páginas.

**Numeración.** `{page}` y `{pages}` se resuelven en dos fases: se pagina reservando el
ancho de un número de prueba de dos cifras (`00`) y después se sustituyen los valores reales.
Si el texto cambia de ancho, solo se vuelve a maquetar esa caja: sus líneas conservan los
saltos y se vuelven a medir y alinear. Como las cajas `fixed` no ocupan espacio, el flujo
principal no cambia.

---

## 6. Validación

La validación se hace en dos niveles:

1. **Template STXT** ([§9](#9-template-completo)): estructura, cardinalidades, tipos `ENUM`,
   `INTEGER`, `BOOLEAN` y `TEXT`. La realiza la librería STXT con sus códigos de error.
2. **Motor:** reglas que el template no puede expresar. Se informan con la línea del documento.

| Regla | Mensaje (ejemplo) |
|---|---|
| Con `Type: paged`, `Media` necesita `Height` | `Con Type: paged, Media necesita Height` |
| `Width` y `Height` del medio son números de puntos positivos; `Margin` del medio, solo puntos | `Width del medio debe ser un número de puntos positivo` |
| Los márgenes del medio dejan área útil | `Los márgenes del medio no dejan área útil` |
| `Flex` solo con `Layout: flex`; `Grid` solo con `Layout: grid` | `Flex solo se aplica con Layout: flex` |
| `Item` solo en hijos en flujo de un contenedor flex o grid | `Item solo tiene efecto en hijos en flujo…` |
| `Cell` solo dentro de grid y con áreas existentes | `Área desconocida en Cell: foot` |
| `Areas` rectangulares y con filas de igual longitud | `El área 'a' no es rectangular` |
| `Offset` no se admite con `Position: static` | `Offset no tiene efecto con Position: static` |
| `Layout: inline` solo contiene `Text` | `Una caja con Layout: inline solo puede contener Text` |
| Cortes forzados solo en cajas en flujo | `Los cortes forzados solo se aplican a cajas en flujo` |
| `Main axis` no admite `stretch` | `Main axis no admite 'stretch'` |
| Sintaxis de valores ([§3](#3-valores)) | `Valor no válido: 12,5 (usa punto decimal…)` |
| `Padding` ≥ 0 (puntos o %); `Border` ≥ 0 (solo puntos) | `Border solo admite puntos no negativos` |
| `Font size` > 0; `Grow` y `Shrink` ≥ 0 | `Font size debe ser mayor que 0` |
| Colores `#RGB` o `#RRGGBB` | `Color no válido (usa #RGB o #RRGGBB)` |
| `Gap` admite 1 o 2 valores | `Gap admite 1 o 2 valores` |

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
Las cajas sin `Id` pueden incluirse para depurar.

Un **test de referencia** es una carpeta con `input.stxt` (el documento) y `expected.stxt`
(el volcado esperado). Los números de `Rect` se comparan con una tolerancia de 0.01 pt.

---

## 8. Diferencias con CSS

| Tema | CSS | Este modelo |
|---|---|---|
| Documento | HTML + hojas de estilo con cascada y selectores | Un árbol STXT; cada caja declara sus propiedades |
| Herencia | Muchas propiedades | Solo texto (`Color`, `Font`, `Font size`, `Line height`, `Text align`, `Text wrap`) y `Widows`/`Orphans` |
| `display` | Combina tipo exterior e interior | `Layout` (interior); el exterior lo decide el padre. No hay `inline-block`. |
| `box-sizing` | `content-box` por defecto | `Box sizing: border` por defecto |
| Unidades | px, em, rem, vw… | pt, %, fr |
| `line-height` sin unidad | Multiplicador | **Un número son puntos**; el relativo se escribe con % |
| Altura de línea | Calculada por fuente, *half-leading* y *baseline* | Cada línea mide `Line height` |
| Palabras largas | Desbordan (configurable) | Desbordan |
| `float`, tablas, `sticky`, `transform`, `opacity` | Sí | No |
| `position: fixed` en pantalla | Relativo a la ventana | En continuo, relativo al documento |
| `position: fixed` en impresión | Repetido en cada página | Igual: repetido, relativo al área útil |
| `justify-content` | `space-between`, `space-around`, `space-evenly` | `Main axis: between`, `around`, `evenly` |
| `z-index` | Propiedad `z-index` | `Layer` |
| `box-decoration-break` | `slice`, `clone` | `Decoration: slice`, `clone` |
| `text-wrap` | `wrap`, `balance`, `pretty`… (`pretty` según el navegador) | `Text wrap: auto` (voraz) o `pretty` (Knuth-Plass en todo el párrafo) |
| Márgenes `auto` en ítems flex | Absorben espacio | Valen 0 |
| Grid `dense` | Sí | No |
| Subgrid | Sí | No (posible ampliación) |
| Escritura vertical | Sí | No |

---

## 9. Template completo

Fichero: [`src/main/resources/stxt/dev.stxt.boxes.stxt`](../src/main/resources/stxt/dev.stxt.boxes.stxt).
Esta copia es informativa y omite la sección `Description`; si difieren, manda el fichero.

```
Template (@stxt.template): dev.stxt.boxes
	Structure >>
		Document (dev.stxt.boxes):
			Media: (1)
				Type: (1) ENUM [continuous, paged]
				Width: (1)
				Height: (?)
				Margin: (?)
			Box: (+)
				Id: (?)
				Layout: (?) ENUM [block, inline, flex, grid]
				Position: (?) ENUM [static, relative, absolute, fixed]
				Offset: (?)
				Layer: (?) INTEGER
				Isolate: (?) BOOLEAN
				Box sizing: (?) ENUM [border, content]
				Width: (?) @Width
				Height: (?) @Height
				Min width: (?)
				Max width: (?)
				Min height: (?)
				Max height: (?)
				Aspect ratio: (?)
				Margin: (?) @Margin
				Padding: (?)
				Border: (?)
				Background: (?)
				Color: (?)
				Border color: (?)
				Font: (?)
				Font size: (?)
				Line height: (?)
				Text align: (?) ENUM [left, right, center, justify]
				Text wrap: (?) ENUM [auto, pretty]
				Flex: (?)
					Direction: (?) ENUM [row, column]
					Wrap: (?) BOOLEAN
					Gap: (?)
					Main axis: (?) ENUM [start, end, center, between, around, evenly]
					Cross axis: (?) ENUM [start, end, center, stretch]
					Lines: (?) ENUM [start, end, center, between, around, stretch]
				Grid: (?)
					Columns: (?)
					Rows: (?)
					Gap: (?) @Gap
					Areas: (?) TEXT
					Flow: (?) ENUM [row, column]
					Horizontal: (?) ENUM [start, end, center, stretch]
					Vertical: (?) ENUM [start, end, center, stretch]
				Item: (?)
					Grow: (?)
					Shrink: (?)
					Basis: (?)
					Cross axis: (?) @Cross axis
					Cell: (?)
					Horizontal: (?) @Horizontal
					Vertical: (?) @Vertical
				Break before: (?) ENUM [auto, page, avoid]
				Break after: (?) ENUM [auto, page, avoid]
				Break inside: (?) ENUM [auto, avoid]
				Widows: (?) INTEGER
				Orphans: (?) INTEGER
				Decoration: (?) ENUM [slice, clone]
				Text: (?) TEXT
				Box: (*) @Box
```

Las propiedades que se repiten en varios sitios (`Width`, `Height`, `Margin`, `Gap`,
`Cross axis`, `Horizontal`, `Vertical`, `Box`) se definen una vez y se reutilizan con `@`,
como exige STXT para los nodos locales. Por eso `Width` de `Media` y `Width` de `Box` son el
mismo nodo con distinta cardinalidad.

---

## 10. Ejemplo completo

Fichero: [`src/test/resources/examples/report.stxt`](../src/test/resources/examples/report.stxt).

```
Document (dev.stxt.boxes):
	Media:
		Type: paged
		Width: 595
		Height: 842
		Margin: 40
	Box:
		Id: header
		Position: fixed
		Offset: -30 0 auto 0
		Height: 20
		Font size: 8
		Color: #5F5E5A
		Text: Informe trimestral
	Box:
		Id: footer
		Position: fixed
		Offset: auto 0 -30 0
		Height: 20
		Font size: 8
		Text align: right
		Text: Página {page} de {pages}
	Box:
		Id: title
		Font size: 20
		Line height: 24
		Margin: 0 0 16 0
		Break after: avoid
		Text: Resultados del trimestre
	Box:
		Id: body
		Layout: grid
		Grid:
			Columns: 150 1fr
			Gap: 20
		Box:
			Id: side
			Background: #F1EFE8
			Padding: 10
			Break inside: avoid
			Text >>
				Resumen de las cifras clave
				del trimestre.
		Box:
			Id: main
			Text align: justify
			Text >>
				Primer párrafo del informe. Los saltos de línea simples
				dentro del bloque equivalen a espacios.

				Una línea en blanco empieza un párrafo nuevo.
	Box:
		Id: stamp
		Position: absolute
		Offset: 0 0 auto auto
		Width: 80
		Height: 30
		Border: 1
		Border color: #A32D2D
		Text align: center
		Layer: 1
		Text: BORRADOR
```

**Cómo se maqueta:**

- **Área útil:** 595 − 2·40 = **515 pt** de ancho y 842 − 2·40 = **762 pt** de alto, desde
  (40, 40).
- **`header`** (`fixed`): izquierda y derecha 0 con ancho `auto`, así que se estira a 515 pt.
  Arriba −30 la coloca en y = 40 − 30 = **10**, dentro del margen superior. Se repite en cada
  página.
- **`footer`** (`fixed`): abajo −30 con alto 20 da arriba = 762 + 30 − 20 = 772, es decir,
  y = 40 + 772 = **812**, dentro del margen inferior. `{page}` y `{pages}` se sustituyen
  en cada página.
- **`title`**: primera caja en flujo, en y = **40**, con una línea de 24 pt. Su margen inferior
  deja el cuerpo en y = 40 + 24 + 16 = **80**. Con `Break after: avoid`, nunca queda solo al
  final de una página.
- **`body`** (grid): 515 − 20 de hueco = 495 para pistas. La columna fija mide **150** y la
  fracción recibe **345**. `side` empieza en x = 40 y `main` en x = 40 + 150 + 20 = **210**.
  `side` no se parte (`Break inside: avoid`); `main` puede partirse entre páginas
  respetando viudas y huérfanas (2 por defecto).
- **`stamp`** (`absolute`): no hay ancestro posicionado, así que su containing block es el área
  útil. Arriba 0 y derecha 0 con ancho 80 la anclan a la esquina superior derecha:
  x = 40 + 515 − 80 = **475**, y = **40**. Con `Layer: 1` se pinta por encima del título.

---

## 11. Glosario

| Término | Definición |
|---|---|
| **Área útil** | Rectángulo del medio menos sus márgenes; containing block inicial. |
| **Caja anónima** | Caja que crea el motor para envolver texto mezclado con cajas. |
| **Caja de borde / relleno / contenido / margen** | Los cuatro rectángulos concéntricos de una caja ([§5.2](#52-modelo-de-caja-y-containing-block)). |
| **Containing block** | Rectángulo de referencia para porcentajes, `auto` y desplazamientos. |
| **Contexto de apilamiento** | Grupo de cajas que se pinta como una unidad y ordena a sus hijos por `Layer`. |
| **Contexto de formato de bloque** | Región que aísla el colapso de márgenes. |
| **Definido** | Tamaño conocido sin hacer el layout del contenido. |
| **Fragmentainer** | Contenedor de fragmentos: la franja infinita o cada página. |
| **Fragmento** | Parte de una caja que cae en un fragmentainer. |
| **Huérfana** (`Orphans`) | Línea de un párrafo que queda sola al final de una página. |
| **Min-content / max-content** | Tamaño mínimo sin desbordar / tamaño sin saltos de línea. |
| **Pista** | Columna o fila de una rejilla grid. |
| **Posición estática** | Donde habría quedado una caja posicionada si estuviera en flujo. |
| **Viuda** (`Widows`) | Línea de un párrafo que pasa sola a la página siguiente. |
