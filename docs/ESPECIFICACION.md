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

- **Continuo:** una franja de ancho fijo y altura infinita, como la ventana de un navegador.
- **Paginado:** una secuencia de páginas de tamaño fijo, como un PDF. Las cajas que no caben
  se parten entre páginas.

El modelo es un subconjunto de los mecanismos de maquetación de CSS (flujo de bloque, texto,
posicionamiento, flex y grid, fragmentación) sin HTML y sin cascada. Las diferencias se
resumen en el [§8](#8-diferencias-con-css).

### 1.2 Convenciones

- **Todas las medidas están en puntos tipográficos** (1 pt = 1/72 de pulgada). Un A4 mide
  595 × 842 pt.
- **Los nombres de nodos y valores no llevan tildes ni eñes:** `Posicion`, `Tamano`,
  `Huerfanas`, `despues`. El contenido de `Texto` sí puede llevarlas.
- **Los ejes:** *horizontal* es el de `Ancho` e *izquierda/derecha*; *vertical* es el de
  `Alto` y *arriba/abajo*. La escritura es siempre horizontal, de izquierda a derecha.
- **Las cuatro cajas de una caja** (ver [§5.2](#52-modelo-de-caja-y-containing-block)): de
  contenido, de relleno, de borde y de margen.
- **Caja de referencia:** salvo que se diga otra cosa, las posiciones y los tamaños se refieren
  a la **caja de borde**.

---

## 2. Estructura de un documento

Un documento tiene un nodo raíz `Documento` con exactamente un `Medio` y una o más `Caja`.
Cada `Caja` puede contener propiedades, `Texto` y otras `Caja`.

```
Documento (dev.stxt.boxes):
	Medio:
		Tipo: paginado
		Ancho: 595
		Alto: 842
		Margen: 40
	Caja:
		Id: titulo
		Tamano: 20
		Texto: Un título
	Caja:
		Id: cuerpo
		Relleno: 10
		Caja:
			Texto >>
				Primer párrafo.

				Segundo párrafo.
```

**El orden importa.** STXT conserva el orden de los nodos, y el motor coloca los `Texto` y las
`Caja` hijas en el orden en que aparecen. El orden de las propiedades no importa.

**Texto inline o en bloque.** `Texto: una línea` y `Texto >>` con un bloque indentado son
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

### 3.2 Listas y `repetir`

Algunas propiedades admiten varios valores separados por espacios (`Columnas`, `Filas`,
`Margen`…). En las listas de pistas, `repetir(n, patrón)` repite un patrón:

```
Columnas: 150 repetir(3, 1fr) minmax(100, 2fr)
```

equivale a `150 1fr 1fr 1fr minmax(100, 2fr)`.

### 3.3 Abreviados de cuatro lados

`Margen`, `Relleno`, `Borde` y `Desplazamiento` aceptan de 1 a 4 valores en el orden
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

Los booleanos se escriben `true` o `false`. Los enteros, sin decimales: `Capa: -1`, `Viudas: 3`.

### 3.6 Bases de los porcentajes

| Propiedad | Porcentaje de… |
|---|---|
| `Ancho`, `Min ancho`, `Max ancho` | el ancho del containing block |
| `Alto`, `Min alto`, `Max alto` | el alto del containing block, **si es definido**; si no, `auto` |
| `Margen`, `Relleno` (los cuatro lados) | el **ancho** del containing block, también arriba y abajo |
| `Desplazamiento` arriba/abajo | el alto del containing block |
| `Desplazamiento` izquierda/derecha | el ancho del containing block |
| `Base` (ítem flex) | el tamaño del contenedor flex en el eje principal |
| `Hueco` | el tamaño de la caja de contenido del contenedor en ese eje |
| Pistas de grid | el tamaño de la caja de contenido del contenedor en ese eje |
| `Interlineado` | el `Tamano` de la propia caja |

`Borde` no admite porcentajes.

---

## 4. Referencia de propiedades

En las tablas, **Hereda** indica si una caja que no declara la propiedad toma el valor de su
padre. No hay cascada: solo se heredan las propiedades marcadas.

### 4.1 `Medio`

Obligatorio, exactamente uno, hijo de `Documento`.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Tipo` | `continuo`, `paginado` | — (obligatorio) | |
| `Ancho` | número > 0 | — (obligatorio) | Ancho de la página o de la franja. |
| `Alto` | número > 0 | — | Obligatorio en modo paginado. En modo continuo se ignora. |
| `Margen` | 1 a 4 números | `0` | Márgenes de página. Solo puntos. |

El **área útil** es el rectángulo del medio menos sus márgenes. Es el containing block de las
cajas de primer nivel y de las cajas `fija`. En modo continuo su altura es infinita.

### 4.2 Tipo y posición de la caja

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Id` | texto | — | No | Identificador para volcados, tests y mensajes. |
| `Layout` | `bloque`, `linea`, `flex`, `grid` | `bloque` | No | Cómo coloca la caja a **sus hijos**. Ver [§5.1](#51-árbol-de-cajas). |
| `Posicion` | `flujo`, `relativa`, `absoluta`, `fija` | `flujo` | No | Cómo participa la caja en el flujo de su padre. Ver [§5.5](#55-posicionamiento). |
| `Desplazamiento` | 1 a 4 valores (número, %, `auto`) | `auto` | No | Arriba, derecha, abajo, izquierda. No se admite con `Posicion: flujo`. |
| `Capa` | entero | `auto` | No | Orden de apilamiento. Ver [§5.6](#56-apilamiento-y-orden-de-pintado). |
| `Aislar` | booleano | `false` | No | Crea un contexto de formato de bloque nuevo: corta el colapso de márgenes con el padre. |

### 4.3 Dimensiones

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Medida` | `borde`, `contenido` | `borde` | No | Qué miden `Ancho` y `Alto`: la caja de borde o la de contenido. |
| `Ancho` | número, %, `auto`, intrínsecos, funciones | `auto` | No | |
| `Alto` | número, %, `auto`, intrínsecos, funciones | `auto` | No | |
| `Min ancho`, `Min alto` | número, %, `auto` | `auto` | No | `auto` = min-content en ítems de flex y grid; 0 en el resto. |
| `Max ancho`, `Max alto` | número, % | sin máximo | No | |
| `Proporcion` | `ancho/alto` o número | — | No | Si un eje es `auto` y el otro es conocido, se deduce. Ejemplo: `16/9`. |

### 4.4 Modelo de caja

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Margen` | 1 a 4 valores (número, %, `auto`) | `0` | No | Puede ser negativo. `auto` centra en flujo de bloque. |
| `Relleno` | 1 a 4 valores (número ≥ 0, % ≥ 0) | `0` | No | |
| `Borde` | 1 a 4 números ≥ 0 | `0` | No | Grosor. El color va en `Color borde`. |

### 4.5 Pintado

No afectan al layout.

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Fondo` | color | transparente | No | Se pinta en la caja de relleno. |
| `Color` | color | `#000000` | **Sí** | Color del texto. |
| `Color borde` | color | el de `Color` | No | |

### 4.6 Texto

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Fuente` | nombre | `Helvetica` | **Sí** | En la salida PDF, una de las 14 fuentes estándar o una fuente registrada. |
| `Tamano` | número > 0 | `10` | **Sí** | Cuerpo de la fuente. |
| `Interlineado` | número o % | `120%` | **Sí** | **Un número son puntos** (`15` = 15 pt). Para un valor relativo, usa porcentaje (`120%`). |
| `Alinear` | `izquierda`, `derecha`, `centro`, `justificado` | `izquierda` | **Sí** | La última línea de un párrafo justificado se alinea a la izquierda. |
| `Texto` | texto inline o bloque | — | — | Contenido. Ver [§5.4](#54-texto). |

### 4.7 `Flex`

Solo con `Layout: flex`.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Direccion` | `fila`, `columna` | `fila` | Eje principal horizontal o vertical. |
| `Envolver` | booleano | `false` | Permite repartir los ítems en varias líneas. |
| `Hueco` | 1 o 2 valores | `0` | Un valor para ambos ejes, o `filas columnas`. |
| `Eje principal` | `inicio`, `fin`, `centro`, `entre`, `alrededor`, `uniforme` | `inicio` | Reparto del sobrante en el eje principal. |
| `Eje cruzado` | `inicio`, `fin`, `centro`, `estirar` | `estirar` | Alineación de los ítems dentro de su línea. |
| `Lineas` | `inicio`, `fin`, `centro`, `entre`, `alrededor`, `estirar` | `estirar` | Reparto de las líneas en el contenedor (solo con `Envolver`). |

### 4.8 `Grid`

Solo con `Layout: grid`.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Columnas` | lista de pistas | una columna `auto` | Ver [§5.8](#58-grid). |
| `Filas` | lista de pistas | filas implícitas `auto` | |
| `Hueco` | 1 o 2 valores | `0` | Un valor para ambos ejes, o `filas columnas`. |
| `Areas` | bloque de texto | — | Una fila por línea; nombres separados por espacios; `.` = celda vacía. |
| `Flujo` | `fila`, `columna` | `fila` | Dirección de la colocación automática. |
| `Horizontal` | `inicio`, `fin`, `centro`, `estirar` | `estirar` | Alineación horizontal de los ítems en su celda. |
| `Vertical` | `inicio`, `fin`, `centro`, `estirar` | `estirar` | Alineación vertical de los ítems en su celda. |

Pistas válidas: número, %, `auto`, `min-content`, `max-content`, fracción, `minmax(a, b)` y
`repetir(n, patrón)`.

### 4.9 `Item`

Solo en hijos **en flujo** de un contenedor `flex` o `grid`.

| Propiedad | Valores | Por defecto | Aplica a | Notas |
|---|---|---|---|---|
| `Crecer` | número ≥ 0 | `0` | flex | Parte del espacio sobrante que recibe. |
| `Encoger` | número ≥ 0 | `1` | flex | Parte del déficit que cede, ponderada por `Base`. |
| `Base` | número, %, `auto` | `auto` | flex | Tamaño de partida en el eje principal. |
| `Eje cruzado` | como en `Flex` | el del contenedor | flex | Sobrescribe la alineación del contenedor. |
| `Celda` | área o `fila columna [filas columnas]` | `auto` | grid | Contando desde 1. Ver [§5.8](#58-grid). |
| `Horizontal` | como en `Grid` | el del contenedor | grid | |
| `Vertical` | como en `Grid` | el del contenedor | grid | |

### 4.10 Fragmentación

Solo tienen efecto en modo paginado.

| Propiedad | Valores | Por defecto | Hereda | Notas |
|---|---|---|---|---|
| `Corte antes` | `auto`, `pagina`, `evitar` | `auto` | No | `pagina` fuerza un salto antes de la caja. |
| `Corte despues` | `auto`, `pagina`, `evitar` | `auto` | No | `evitar` mantiene la caja con la siguiente (títulos). |
| `Corte dentro` | `auto`, `evitar` | `auto` | No | `evitar` intenta no partir la caja. |
| `Viudas` | entero ≥ 1 | `2` | **Sí** | Mínimo de líneas de un párrafo que pasan a la página siguiente. |
| `Huerfanas` | entero ≥ 1 | `2` | **Sí** | Mínimo de líneas de un párrafo que quedan al final de una página. |
| `Decoracion` | `cortar`, `repetir` | `cortar` | No | Qué pasa con borde y relleno en un corte. |

---

## 5. Semántica del layout

### 5.1 Árbol de cajas

El motor transforma el documento en un **árbol de cajas**, que no coincide exactamente con los
nodos `Caja`:

- **Cajas anónimas.** Si una caja mezcla `Texto` con `Caja` hijas, cada `Texto` se envuelve en
  una caja anónima con `Layout: linea`, en su posición dentro del orden. Lo mismo ocurre con un
  `Texto` dentro de un contenedor `flex` o `grid`, que se convierte en un ítem anónimo. Las
  cajas anónimas heredan las propiedades heredables y no tienen margen, borde ni relleno.
- **Bloque con solo texto.** Una caja `bloque` cuyo único contenido es `Texto` se maqueta con el
  algoritmo de línea, sin caja anónima.
- **`Layout: linea`** solo puede contener `Texto`.

**`Layout` frente a participación.** `Layout` indica cómo coloca la caja a sus hijos. Cómo
participa la caja en su padre lo decide el padre: dentro de un `bloque` es un bloque apilado;
dentro de un `flex` o un `grid`, un ítem. La excepción son las cajas `absoluta` y `fija`, que
salen del flujo del padre.

### 5.2 Modelo de caja y containing block

Cada caja tiene cuatro rectángulos concéntricos: **contenido**, **relleno** (contenido +
`Relleno`), **borde** (relleno + `Borde`) y **margen** (borde + `Margen`).

Con `Medida: borde` (por defecto), `Ancho` y `Alto` miden la caja de borde: una caja con
`Ancho: 200` ocupa 200 pt antes de los márgenes. Con `Medida: contenido`, miden solo el
contenido.

El **containing block** de una caja es el rectángulo de referencia para porcentajes, `auto` y
`Desplazamiento`:

| `Posicion` de la caja | Containing block |
|---|---|
| `flujo`, `relativa` | caja de **contenido** del padre |
| `absoluta` | caja de **relleno** del ancestro más cercano con `Posicion` distinta de `flujo`; si no hay ninguno, el área útil |
| `fija` | área útil del medio (de cada página, en modo paginado) |

### 5.3 Flujo de bloque

Con `Layout: bloque`, los hijos en flujo se apilan en vertical:

1. **Ancho.** Con `Ancho: auto`, la caja ocupa el ancho del containing block menos sus márgenes
   horizontales. Con ancho conocido y los dos márgenes horizontales `auto`, el sobrante se
   reparte entre ambos y la caja queda **centrada**; si solo uno es `auto`, absorbe el
   sobrante.
2. **Posición.** La caja se coloca en la posición del cursor vertical, tras aplicar su margen
   superior colapsado con el anterior.
3. **Altura.** Se calcula su layout y el cursor avanza.

Con `Alto: auto`, la altura del contenedor es la posición final del cursor más su relleno y su
borde. **El ancho se propaga de padres a hijos; la altura, de hijos a padres.**

**Colapso de márgenes.** Los márgenes **verticales** adyacentes en flujo de bloque se fusionan:

- entre hermanos consecutivos;
- entre un padre y su primer o último hijo, si el padre no tiene borde ni relleno en ese lado
  y no crea un contexto de formato nuevo (el margen del hijo "sale" del padre);
- a través de cajas vacías (sin altura, borde, relleno ni contenido).

El resultado de colapsar varios márgenes es **el mayor positivo más el más negativo**:
`20` y `30` dan 30; `20` y `−5` dan 15; `−10` y `−4` dan −10.

**Contextos de formato de bloque.** Aíslan el colapso de márgenes. Los crean: el área útil, las
cajas `absoluta` y `fija`, los ítems de flex y grid, y las cajas con `Aislar: true`. Los
márgenes de los ítems de flex y grid nunca colapsan.

### 5.4 Texto

El texto de una caja `linea` se reparte en **líneas** del ancho de su caja de contenido:

- **Espacios.** Las secuencias de espacios y los saltos de línea simples equivalen a un espacio.
  Una **línea en blanco** separa párrafos y fuerza un salto.
- **Reparto.** Algoritmo voraz: las palabras se añaden a la línea mientras quepan. Una palabra
  más ancha que la línea ocupa una línea propia y **desborda**.
- **Altura.** Cada línea mide `Interlineado`. La altura del contenido es el número de líneas
  por `Interlineado`.
- **Alineación.** `izquierda`, `derecha` y `centro` desplazan cada línea; `justificado` reparte
  el sobrante entre los espacios, salvo en la última línea de cada párrafo.
- **Tamaños intrínsecos.** min-content = la palabra más ancha; max-content = el párrafo más
  largo en una sola línea.
- **Numeración de páginas.** En modo paginado, `{pagina}` y `{paginas}` en el texto se
  sustituyen por el número de página y el total de páginas. Ver [§5.10](#510-fragmentación).

### 5.5 Posicionamiento

**`relativa`.** La caja se coloca como si fuera `flujo` y después se desplaza visualmente según
`Desplazamiento`. Su hueco original se conserva y sus hermanos no se mueven. Si se declaran
arriba y abajo, gana arriba; si se declaran izquierda y derecha, gana izquierda. Una caja
`relativa` sin desplazamiento sirve como containing block de sus descendientes `absoluta`.

**`absoluta`.** La caja sale del flujo: no ocupa espacio ni contribuye a la altura `auto` de su
padre. En cada eje se cumple:

```
inicio + margen inicio + tamaño + margen fin + fin = tamaño del containing block
```

donde *inicio/fin* son izquierda/derecha o arriba/abajo del `Desplazamiento`. Según qué
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
**posición estática** es donde habría quedado la caja si fuera `flujo`.

**`fija`.** Como `absoluta`, pero su containing block es el área útil. En modo paginado **se
pinta en todas las páginas** (cabeceras, pies, marcas de agua). En modo continuo no hay ventana,
y se comporta como `absoluta` respecto al área útil del documento. Las cajas `fija` no reservan
espacio: el autor debe dejar sitio con el `Margen` del medio. Para colocarlas en el margen de
la página se usan desplazamientos negativos (`Desplazamiento: -30 0 auto 0`).

**Orden de cálculo.** Las cajas posicionadas fuera de flujo se resuelven cuando su containing
block ya conoce su tamaño, después del layout en flujo.

### 5.6 Apilamiento y orden de pintado

El pintado se organiza en **contextos de apilamiento**. Crean uno: el área útil, las cajas
`fija`, las cajas posicionadas (`Posicion` distinta de `flujo`) con `Capa` declarada y los
ítems de flex o grid con `Capa` declarada.

Dentro de cada contexto, el orden de pintado de abajo arriba es:

1. Fondo y borde de la caja que crea el contexto.
2. Contextos hijos con `Capa` negativa, de menor a mayor.
3. Cajas en flujo en orden del árbol: primero todos los fondos y bordes, después todo el texto.
4. Cajas posicionadas sin `Capa` o con `Capa: 0`, en orden del árbol.
5. Contextos hijos con `Capa` positiva, de menor a mayor.

A igual `Capa`, se pinta en orden del árbol. **`Capa` solo compara dentro del mismo contexto:**
el contenido de un contexto se pinta como un bloque al nivel de ese contexto, aunque sus hijos
tengan `Capa` mayores.

### 5.7 Flex

Con `Layout: flex`, los ítems se colocan en el **eje principal** (`Direccion`) y se alinean en
el **eje cruzado**.

**Algoritmo:**

1. Espacio disponible: la caja de contenido del contenedor en el eje principal.
2. **Base** de cada ítem: `Base` si tiene valor; si es `auto`, el tamaño declarado en el eje
   principal; si tampoco lo hay, su max-content.
3. **Tamaño hipotético:** la base limitada por el mínimo y el máximo del ítem. Con
   `Min ancho: auto`, el mínimo es el **min-content** del ítem.
4. **Líneas:** sin `Envolver`, una sola. Con `Envolver`, los ítems se añaden a la línea mientras
   sus tamaños hipotéticos y los huecos quepan.
5. **Tamaños flexibles** (por línea), ver abajo.
6. **Tamaño cruzado:** layout de cada ítem con su tamaño principal final (en `fila`, el texto se
   redistribuye con el ancho final). La línea mide lo que el ítem más alto.
7. **Estirar:** los ítems con `estirar` y sin tamaño cruzado declarado pasan a medir lo que la
   línea, y se repite su layout.
8. **Alinear** en el eje principal, en el cruzado y las líneas en el contenedor.

Con tamaño cruzado `auto`, el contenedor mide la suma de sus líneas más los huecos.

**Reparto del espacio.** Espacio libre = disponible − suma de tamaños hipotéticos − huecos.

- **Positivo:** cada ítem recibe una parte proporcional a su `Crecer`.
- **Negativo:** cada ítem cede una parte proporcional a **`Encoger × Base`**.
- Un ítem que choca con su mínimo o máximo se **congela** en ese límite y el reparto se repite
  entre los demás.
- Si la suma de factores de los ítems no congelados es menor que 1, solo se reparte esa
  fracción del espacio libre.

**Alineación en el eje principal** (sobrante `L`, `n` ítems; solo actúa si nadie crece):

| `Eje principal` | Antes del primero | Entre ítems | Después del último |
|---|---|---|---|
| `inicio` | 0 | hueco | L |
| `fin` | L | hueco | 0 |
| `centro` | L/2 | hueco | L/2 |
| `entre` | 0 | hueco + L/(n−1) | 0 |
| `alrededor` | L/2n | hueco + L/n | L/2n |
| `uniforme` | L/(n+1) | hueco + L/(n+1) | L/(n+1) |

**Comportamientos a tener en cuenta:**

- En `columna` con `Alto: auto` no hay espacio libre: `Crecer` no tiene efecto.
- `Base: 0` con `Crecer: 1` reparte todo el espacio (columnas iguales); `Base: auto` con
  `Crecer: 1` solo reparte el sobrante sobre el tamaño del contenido.
- Un ítem no encoge por debajo de su min-content salvo que declare `Min ancho: 0`.

### 5.8 Grid

Con `Layout: grid`, el motor resuelve primero **dónde** va cada ítem y después **cuánto** mide
cada pista.

**Colocación:**

1. Se construye la rejilla explícita con `Columnas`, `Filas` y `Areas`. Cada nombre de `Areas`
   debe formar un rectángulo y todas las filas deben tener el mismo número de celdas.
2. Se colocan los ítems con `Celda` explícita (fila y columna, o nombre de área).
3. Se colocan los demás en orden del árbol con un **cursor** que recorre la rejilla por filas
   (o por columnas, con `Flujo: columna`). Cada ítem va al primer hueco donde quepa. El cursor
   solo avanza, de modo que pueden quedar huecos atrás.
4. Si un ítem cae fuera de la rejilla declarada, se crean pistas implícitas `auto`.

**Sintaxis de `Celda`:** un nombre de área (`Celda: lat`) o `fila columna [filas columnas]`
contando desde 1, donde fila y columna pueden ser `auto`: `Celda: 1 1 1 2` ocupa la fila 1 y
las columnas 1 y 2; `Celda: auto auto 1 2` se coloca automáticamente ocupando dos columnas.

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
Con `Alto: auto` en el contenedor, las filas en fracción no tienen espacio que repartir y
miden en proporción a su contenido: todas las filas `1fr` miden lo que la más alta.

**Alineación en la celda.** Cada ítem ocupa un área formada por sus pistas y los huecos
internos. Con `estirar` (por defecto) y sin `Ancho`/`Alto` declarado, ocupa toda el área y se
repite su layout. Con `inicio`, `fin` o `centro`, conserva su tamaño de contenido y se desplaza.

### 5.9 Dimensionado

**Tamaños intrínsecos** (anchos de la caja de margen):

| `Layout` | min-content | max-content |
|---|---|---|
| `linea` | palabra más ancha | párrafo más largo en una línea |
| `bloque` | máximo del min-content de los hijos | máximo del max-content de los hijos |
| `flex` en `fila`, sin envolver | suma del min-content de los ítems + huecos | suma del max-content + huecos |
| `flex` en `fila`, con envolver | máximo del min-content de los ítems | suma del max-content + huecos |
| `flex` en `columna` | máximo de los ítems | máximo de los ítems |
| `grid` | suma de pistas dimensionadas con restricción min-content | ídem con max-content |

Una caja con `Ancho` fijo tiene ese valor como min-content y max-content. Las cajas `absoluta`
y `fija` no aportan nada a sus ancestros.

**Definido e indefinido.** Un tamaño es **definido** si se conoce sin hacer el layout del
contenido: un número, el área útil, un porcentaje de algo definido o un ítem estirado por flex
o grid. Los porcentajes de ancho se resuelven siempre; los de alto, solo si el alto del
containing block es definido. En modo paginado el área útil es definida: una caja de primer
nivel con `Alto: 100%` ocupa la página. En modo continuo, el mismo valor se comporta como
`auto`.

**Mínimos y máximos.** Se calcula el tamaño provisional, se limita por el máximo y se sube al
mínimo. **Si chocan, gana el mínimo.**

**Proporción.** Con `Proporcion` y un eje `auto`, ese eje se deduce del otro.

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

1. `Viudas` y `Huerfanas`;
2. `Corte antes/despues: evitar`;
3. `Corte dentro: evitar`.

Si sigue sin haber corte posible, corta donde se acaba el espacio.

**Garantía de progreso.** Al principio de una página siempre se coloca algo, aunque desborde o
haya que cortarlo a la fuerza. Una caja monolítica más alta que una página no provoca un bucle
infinito.

**Cortes forzados.** `Corte antes: pagina` y `Corte despues: pagina` fuerzan un salto. Solo se
aplican a cajas en flujo.

**Márgenes en los cortes.** En un corte no forzado, los márgenes que tocan el corte se truncan
a cero. En un corte forzado, se trunca el margen anterior al corte y se conserva el posterior.

**Bordes y relleno en los cortes.** Con `Decoracion: cortar`, el primer fragmento no tiene
borde ni relleno inferiores y los siguientes no tienen los superiores. Con `Decoracion: repetir`,
cada fragmento se pinta como una caja completa. Un `Alto` fijo es el total de todos los
fragmentos, no el de cada uno.

**Flex y grid.** En `columna`, el corte se hace entre ítems. En `fila` y en grid, entre líneas
o filas; si una fila no cabe en una página, sus ítems se fragmentan **en paralelo** y la fila
continúa en la página siguiente con la altura del ítem más largo.

**Posicionadas.** Una caja `absoluta` se coloca en la página donde cae su posición y no se
parte. Una caja `fija` se repite en todas las páginas.

**Numeración.** `{pagina}` y `{paginas}` se resuelven en dos fases: se pagina reservando el
ancho de un número de prueba y después se sustituyen los valores reales. Si el texto cambia de
ancho, solo se vuelve a maquetar esa caja; como las cajas `fija` no ocupan espacio, el flujo
principal no cambia.

---

## 6. Validación

La validación se hace en dos niveles:

1. **Template STXT** ([§9](#9-template-completo)): estructura, cardinalidades, tipos `ENUM`,
   `INTEGER`, `BOOLEAN` y `TEXT`. La realiza la librería STXT con sus códigos de error.
2. **Motor:** reglas que el template no puede expresar. Se informan con la línea del documento.

| Regla | Mensaje (ejemplo) |
|---|---|
| Con `Tipo: paginado`, `Medio` necesita `Alto` | `Con Tipo: paginado, Medio necesita Alto` |
| `Ancho` y `Alto` del medio son números de puntos positivos; `Margen` del medio, solo puntos | `Ancho del medio debe ser un número de puntos positivo` |
| Los márgenes del medio dejan área útil | `Los márgenes del medio no dejan área útil` |
| `Flex` solo con `Layout: flex`; `Grid` solo con `Layout: grid` | `Flex solo se aplica con Layout: flex` |
| `Item` solo en hijos en flujo de un contenedor flex o grid | `Item solo tiene efecto en hijos en flujo…` |
| `Celda` solo dentro de grid y con áreas existentes | `Área desconocida en Celda: pie` |
| `Areas` rectangulares y con filas de igual longitud | `El área 'a' no es rectangular` |
| `Desplazamiento` no se admite con `Posicion: flujo` | `Desplazamiento no tiene efecto con Posicion: flujo` |
| `Layout: linea` solo contiene `Texto` | `Una caja con Layout: linea solo puede contener Texto` |
| Cortes forzados solo en cajas en flujo | `Los cortes forzados solo se aplican a cajas en flujo` |
| `Eje principal` no admite `estirar` | `Eje principal no admite 'estirar'` |
| Sintaxis de valores ([§3](#3-valores)) | `Valor no válido: 12,5 (usa punto decimal…)` |
| `Relleno` ≥ 0 (puntos o %); `Borde` ≥ 0 (solo puntos) | `Borde solo admite puntos no negativos` |
| `Tamano` > 0; `Crecer` y `Encoger` ≥ 0 | `Tamano debe ser mayor que 0` |
| Colores `#RGB` o `#RRGGBB` | `Color no válido (usa #RGB o #RRGGBB)` |
| `Hueco` admite 1 o 2 valores | `Hueco admite 1 o 2 valores` |

---

## 7. Formato de resultados

El motor puede volcar el resultado en STXT, que es el formato de los tests de referencia. Cada
`Rect` es la caja de borde en **coordenadas absolutas de página**: `x y ancho alto`, en pt, con
hasta dos decimales.

```
Resultado:
	Pagina: 1
		Fragmento: cabecera
			Rect: 40 10 515 20
		Fragmento: cuerpo
			Rect: 40 80 515 300
			Fragmento: lateral
				Rect: 40 80 150 120
```

Por defecto solo se vuelcan las cajas con `Id`; las descendientes con `Id` se anidan dentro.
Las cajas sin `Id` pueden incluirse para depurar.

Un **test de referencia** es una carpeta con `entrada.stxt` (el documento) y `esperado.stxt`
(el volcado esperado). Los números de `Rect` se comparan con una tolerancia de 0.01 pt.

---

## 8. Diferencias con CSS

| Tema | CSS | Este modelo |
|---|---|---|
| Documento | HTML + hojas de estilo con cascada y selectores | Un árbol STXT; cada caja declara sus propiedades |
| Herencia | Muchas propiedades | Solo texto (`Color`, `Fuente`, `Tamano`, `Interlineado`, `Alinear`) y `Viudas`/`Huerfanas` |
| `display` | Combina tipo exterior e interior | `Layout` (interior); el exterior lo decide el padre. No hay `inline-block`. |
| `box-sizing` | `content-box` por defecto | `Medida: borde` por defecto |
| Unidades | px, em, rem, vw… | pt, %, fr |
| `line-height` sin unidad | Multiplicador | **Un número son puntos**; el relativo se escribe con % |
| Altura de línea | Calculada por fuente, *half-leading* y *baseline* | Cada línea mide `Interlineado` |
| Palabras largas | Desbordan (configurable) | Desbordan |
| `float`, tablas, `sticky`, `transform`, `opacity` | Sí | No |
| `position: fixed` en pantalla | Relativo a la ventana | En continuo, relativo al documento |
| `position: fixed` en impresión | Repetido en cada página | Igual: repetido, relativo al área útil |
| Márgenes `auto` en ítems flex | Absorben espacio | Valen 0 |
| Grid `dense` | Sí | No |
| Subgrid | Sí | No (posible ampliación) |
| Escritura vertical | Sí | No |

---

## 9. Template completo

Fichero: [`src/main/resources/stxt/dev.stxt.boxes.stxt`](../src/main/resources/stxt/dev.stxt.boxes.stxt).
Esta copia es informativa; si difieren, manda el fichero.

```
Template (@stxt.template): dev.stxt.boxes
	Structure >>
		Documento (dev.stxt.boxes):
			Medio: (1)
				Tipo: (1) ENUM [continuo, paginado]
				Ancho: (1)
				Alto: (?)
				Margen: (?)
			Caja: (+)
				Id: (?)
				Layout: (?) ENUM [bloque, linea, flex, grid]
				Posicion: (?) ENUM [flujo, relativa, absoluta, fija]
				Desplazamiento: (?)
				Capa: (?) INTEGER
				Aislar: (?) BOOLEAN
				Medida: (?) ENUM [borde, contenido]
				Ancho: (?) @Ancho
				Alto: (?) @Alto
				Min ancho: (?)
				Max ancho: (?)
				Min alto: (?)
				Max alto: (?)
				Proporcion: (?)
				Margen: (?) @Margen
				Relleno: (?)
				Borde: (?)
				Fondo: (?)
				Color: (?)
				Color borde: (?)
				Fuente: (?)
				Tamano: (?)
				Interlineado: (?)
				Alinear: (?) ENUM [izquierda, derecha, centro, justificado]
				Flex: (?)
					Direccion: (?) ENUM [fila, columna]
					Envolver: (?) BOOLEAN
					Hueco: (?)
					Eje principal: (?) ENUM [inicio, fin, centro, entre, alrededor, uniforme]
					Eje cruzado: (?) ENUM [inicio, fin, centro, estirar]
					Lineas: (?) ENUM [inicio, fin, centro, entre, alrededor, estirar]
				Grid: (?)
					Columnas: (?)
					Filas: (?)
					Hueco: (?) @Hueco
					Areas: (?) TEXT
					Flujo: (?) ENUM [fila, columna]
					Horizontal: (?) ENUM [inicio, fin, centro, estirar]
					Vertical: (?) ENUM [inicio, fin, centro, estirar]
				Item: (?)
					Crecer: (?)
					Encoger: (?)
					Base: (?)
					Eje cruzado: (?) @Eje cruzado
					Celda: (?)
					Horizontal: (?) @Horizontal
					Vertical: (?) @Vertical
				Corte antes: (?) ENUM [auto, pagina, evitar]
				Corte despues: (?) ENUM [auto, pagina, evitar]
				Corte dentro: (?) ENUM [auto, evitar]
				Viudas: (?) INTEGER
				Huerfanas: (?) INTEGER
				Decoracion: (?) ENUM [cortar, repetir]
				Texto: (?) TEXT
				Caja: (*) @Caja
```

Las propiedades que se repiten en varios sitios (`Ancho`, `Alto`, `Margen`, `Hueco`,
`Eje cruzado`, `Horizontal`, `Vertical`, `Caja`) se definen una vez y se reutilizan con `@`,
como exige STXT para los nodos locales. Por eso `Ancho` de `Medio` y `Ancho` de `Caja` son el
mismo nodo con distinta cardinalidad.

---

## 10. Ejemplo completo

Fichero: [`src/test/resources/ejemplos/informe.stxt`](../src/test/resources/ejemplos/informe.stxt).

```
Documento (dev.stxt.boxes):
	Medio:
		Tipo: paginado
		Ancho: 595
		Alto: 842
		Margen: 40
	Caja:
		Id: cabecera
		Posicion: fija
		Desplazamiento: -30 0 auto 0
		Alto: 20
		Tamano: 8
		Color: #5F5E5A
		Texto: Informe trimestral
	Caja:
		Id: pie
		Posicion: fija
		Desplazamiento: auto 0 -30 0
		Alto: 20
		Tamano: 8
		Alinear: derecha
		Texto: Página {pagina} de {paginas}
	Caja:
		Id: titulo
		Tamano: 20
		Interlineado: 24
		Margen: 0 0 16 0
		Corte despues: evitar
		Texto: Resultados del trimestre
	Caja:
		Id: cuerpo
		Layout: grid
		Grid:
			Columnas: 150 1fr
			Hueco: 20
		Caja:
			Id: lateral
			Fondo: #F1EFE8
			Relleno: 10
			Corte dentro: evitar
			Texto >>
				Resumen de las cifras clave
				del trimestre.
		Caja:
			Id: principal
			Alinear: justificado
			Texto >>
				Primer párrafo del informe. Los saltos de línea simples
				dentro del bloque equivalen a espacios.

				Una línea en blanco empieza un párrafo nuevo.
	Caja:
		Id: sello
		Posicion: absoluta
		Desplazamiento: 0 0 auto auto
		Ancho: 80
		Alto: 30
		Borde: 1
		Color borde: #A32D2D
		Alinear: centro
		Capa: 1
		Texto: BORRADOR
```

**Cómo se maqueta:**

- **Área útil:** 595 − 2·40 = **515 pt** de ancho y 842 − 2·40 = **762 pt** de alto, desde
  (40, 40).
- **`cabecera`** (`fija`): izquierda y derecha 0 con ancho `auto`, así que se estira a 515 pt.
  Arriba −30 la coloca en y = 40 − 30 = **10**, dentro del margen superior. Se repite en cada
  página.
- **`pie`** (`fija`): abajo −30 con alto 20 da arriba = 762 + 30 − 20 = 772, es decir,
  y = 40 + 772 = **812**, dentro del margen inferior. `{pagina}` y `{paginas}` se sustituyen
  en cada página.
- **`titulo`**: primera caja en flujo, en y = **40**, con una línea de 24 pt. Su margen inferior
  deja el cuerpo en y = 40 + 24 + 16 = **80**. Con `Corte despues: evitar`, nunca queda solo al
  final de una página.
- **`cuerpo`** (grid): 515 − 20 de hueco = 495 para pistas. La columna fija mide **150** y la
  fracción recibe **345**. `lateral` empieza en x = 40 y `principal` en x = 40 + 150 + 20 = **210**.
  `lateral` no se parte (`Corte dentro: evitar`); `principal` puede partirse entre páginas
  respetando viudas y huérfanas (2 por defecto).
- **`sello`** (`absoluta`): no hay ancestro posicionado, así que su containing block es el área
  útil. Arriba 0 y derecha 0 con ancho 80 la anclan a la esquina superior derecha:
  x = 40 + 515 − 80 = **475**, y = **40**. Con `Capa: 1` se pinta por encima del título.

---

## 11. Glosario

| Término | Definición |
|---|---|
| **Área útil** | Rectángulo del medio menos sus márgenes; containing block inicial. |
| **Caja anónima** | Caja que crea el motor para envolver texto mezclado con cajas. |
| **Caja de borde / relleno / contenido / margen** | Los cuatro rectángulos concéntricos de una caja ([§5.2](#52-modelo-de-caja-y-containing-block)). |
| **Containing block** | Rectángulo de referencia para porcentajes, `auto` y desplazamientos. |
| **Contexto de apilamiento** | Grupo de cajas que se pinta como una unidad y ordena a sus hijos por `Capa`. |
| **Contexto de formato de bloque** | Región que aísla el colapso de márgenes. |
| **Definido** | Tamaño conocido sin hacer el layout del contenido. |
| **Fragmentainer** | Contenedor de fragmentos: la franja infinita o cada página. |
| **Fragmento** | Parte de una caja que cae en un fragmentainer. |
| **Huérfana** | Línea de un párrafo que queda sola al final de una página. |
| **Min-content / max-content** | Tamaño mínimo sin desbordar / tamaño sin saltos de línea. |
| **Pista** | Columna o fila de una rejilla grid. |
| **Posición estática** | Donde habría quedado una caja posicionada si estuviera en flujo. |
| **Viuda** | Línea de un párrafo que pasa sola a la página siguiente. |
