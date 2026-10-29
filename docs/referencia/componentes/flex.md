# `Flex` e `Item`

Un `Flex` coloca sus ítems en líneas: en el **eje principal** (`Direction`) uno tras otro y alineados en el **eje
cruzado**. Cada hijo en flujo es un `Item`, que envuelve al elemento que hace de ítem; además admite `Absolute` y `Fixed`.
El `Flex` admite `Id`, `Class`, `Link`, `Box`, `Typography` y `Break`, y va donde va un elemento en flujo.

```
Flex:
	Gap: 10
	Item:
		Grow: 1
		Text: Crece
	Item:
		Block:
			Box:
				Width: 100
```

## Propiedades del `Flex`

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Direction` | `row`, `column` | `row` | Eje principal horizontal o vertical. |
| `Wrap` | booleano | `false` | Permite repartir los ítems en varias líneas. |
| `Gap` | 1 o 2 valores (número, %) | `0` | Un valor para ambos ejes, o `filas columnas`. |
| `Main axis` | `start`, `end`, `center`, `between`, `around`, `evenly` | `start` | Reparto del sobrante en el eje principal. |
| `Cross axis` | `start`, `end`, `center`, `stretch` | `stretch` | Alineación de los ítems dentro de su línea. |
| `Lines` | `start`, `end`, `center`, `between`, `around`, `stretch` | `stretch` | Reparto de las líneas en el contenedor. Solo con `Wrap: true` (`LINES_WITHOUT_WRAP`). |

## `Item`

Contiene **exactamente un elemento en flujo** (`SINGLE_ELEMENT`), que es el ítem: su `Box`, su `Typography`, su `Id` y
demás propiedades van en ese elemento. El `Item` solo añade cómo participa en el `Flex`; no es una caja y no admite `Id`
([Estructura](../comun/estructura.md#elementos)).

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Grow` | número ≥ 0 | `0` | Parte del espacio sobrante que recibe. |
| `Shrink` | número ≥ 0 | `1` | Parte del déficit que cede, ponderada por `Basis`. |
| `Basis` | número, %, `auto` | `auto` | Tamaño de partida en el eje principal. |
| `Cross axis` | como en `Flex` | el del contenedor | Sobrescribe la alineación del contenedor. |
| `Layer` | entero | `auto` | Orden de apilamiento del ítem ([Capas y giro](../comun/capas-y-giro.md)). |

## Algoritmo

1. Espacio disponible: la caja de contenido del contenedor en el eje principal.
2. **Base** de cada ítem: `Basis` si tiene valor; si es `auto`, el tamaño declarado en el eje principal; si tampoco lo
   hay, su max-content.
3. **Tamaño hipotético:** la base limitada por el mínimo y el máximo del ítem. Con `Min width: auto` (`Min height: auto` en
   `column`), el mínimo es el **tamaño de su contenido**: el min-content en `row` y el alto del contenido en `column`, sin
   pasar del máximo del ítem y nunca mayor que su `Width` (o `Height`) declarado.
4. **Líneas:** sin `Wrap`, una sola. Con `Wrap`, los ítems se añaden a la línea mientras sus tamaños hipotéticos y los
   huecos quepan.
5. **Tamaños flexibles** (por línea), ver abajo.
6. **Tamaño cruzado:** layout de cada ítem con su tamaño principal final (en `row`, el texto se redistribuye con el ancho
   final). La línea mide lo que el ítem más alto; sin `Wrap` y con tamaño cruzado definido en el contenedor, la única línea
   mide lo que el contenedor.
7. **Estirar:** los ítems con `stretch` y sin tamaño cruzado declarado pasan a medir lo que la línea, y se repite su layout.
   En `column`, si el contenedor tiene una sola línea, estos ítems se miden desde el principio con el ancho del contenedor;
   con `Wrap`, se miden con su ajuste al contenido y después se estiran al ancho de su línea sin recalcular su alto.
8. **Alinear** en el eje principal, en el cruzado y las líneas en el contenedor.

Con tamaño cruzado `auto`, el contenedor mide la suma de sus líneas más los huecos.

> **Decisión:** el mínimo automático de un ítem es el tamaño de su contenido, nunca mayor que su `Width` (o `Height`)
> declarado: un ítem con `Width: 400` y poco contenido encoge si no cabe.

### Reparto del espacio

Espacio libre = disponible − suma de tamaños hipotéticos − huecos.

- **Positivo:** cada ítem recibe una parte proporcional a su `Grow`.
- **Negativo:** cada ítem cede una parte proporcional a **`Shrink × Basis`**, sin sus márgenes.
- Un ítem que choca con su mínimo o máximo se **congela** en ese límite y el reparto se repite entre los demás.
- Si la suma de factores de los ítems no congelados es menor que 1, solo se reparte esa fracción del espacio libre.

> **Decisión:** al encoger, cada ítem cede en proporción a `Shrink × Basis` sin sus márgenes: dos ítems con `Basis: 300`,
> uno con 10 de margen a cada lado, en 515, ceden lo mismo (52,5 cada uno). *Alternativa descartada:* ponderar con la base
> más los márgenes, que hacía que un ítem con márgenes cediera de más.

### Alineación en el eje principal

Sobrante `L`, `n` ítems; solo actúa si nadie crece:

| `Main axis` | Antes del primero | Entre ítems | Después del último |
|---|---|---|---|
| `start` | 0 | hueco | L |
| `end` | L | hueco | 0 |
| `center` | L/2 | hueco | L/2 |
| `between` | 0 | hueco + L/(n−1) | 0 |
| `around` | L/2n | hueco + L/n | L/2n |
| `evenly` | L/(n+1) | hueco + L/(n+1) | L/(n+1) |

Un sobrante negativo después de encoger significa que los ítems no caben: es un error
([Desbordamiento](../comun/desbordamiento.md)). Los márgenes `auto` de un ítem son un error: la alineación se hace con
`Main axis` y `Cross axis`.

> **Decisión:** el hueco entre filas de un `Flex` `row` sin `Wrap` no se comprueba: no se usa, así que `Gap: 5%` en un
> `Flex` de una línea sin alto definido no es un error.

## Comportamientos a tener en cuenta

- En `column`, `Grow` necesita que el contenedor tenga un alto definido ([Valores](../comun/valores.md#alto-definido)): si
  no, no hay espacio libre que repartir y es un error (`GROW_WITHOUT_EFFECT`).
- `Basis: 0` con `Grow: 1` reparte todo el espacio (columnas iguales); `Basis: auto` con `Grow: 1` solo reparte el
  sobrante sobre el tamaño del contenido.
- Un ítem no encoge por debajo del min-content de su contenido salvo que declare `Min width: 0`. Un `Width` declarado no
  lo frena: un ítem con `Width: 400` y poco contenido encoge si no cabe.
- Un ítem estirado **no** da un alto definido a sus hijos ([Valores](../comun/valores.md#alto-definido)).
- Una `Image` no se estira en el eje cruzado de una fila: su alto sale de su proporción
  ([Image](image.md#dimensionado)).

## Tamaños intrínsecos

En `row` sin `Wrap`, el min-content es la suma del min-content de los ítems más los huecos, y con `Wrap`, el máximo de
ellos; el max-content es la suma del max-content más los huecos. En `column`, los dos son el máximo de los ítems. Se usan
los intrínsecos de cada ítem tal cual, sin `Basis`, `Grow` ni `Shrink` ([La caja](../comun/caja.md#tamaños-intrínsecos)).

## Cortes

En `column`, el corte se hace entre ítems; en `row`, entre líneas. Una línea o un ítem que no cabe en una página entera se
parte en paralelo ([Cortes](../comun/cortes.md#cortes-en-flex-grid-y-table)). Un `Flex` en `column` con `Wrap` no se
parte.

## Posicionadas

La posición estática de un `Absolute` o un `Fixed` hijo de un `Flex` es la esquina superior izquierda de su caja de
contenido ([posicionados](relative-absolute-fixed.md#absolute)).

## Lo que no hay

- Orden visual distinto del orden del documento, direcciones invertidas y alineación por línea base.
- Márgenes `auto` en los ítems: son errores.
