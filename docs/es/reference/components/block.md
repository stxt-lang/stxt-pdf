# `Block`

Una caja que apila sus hijos en vertical. Admite `Id`, `Style`, `Link`, `Box`, `Typography`, `Break`, `Isolate margins` y
`Rotate`, y como hijos cualquier elemento. Va donde va un elemento en flujo. Con `Rotate`, gira con todo lo que contiene
y ocupa el rectángulo que la envuelve ([Capas y giro](../common/layers-and-rotation.md#giro-en-flujo-block-text-paragraph-e-image)).

```
Block:
    Box:
        Padding: 10
        Background: #F1EFE8
    Paragraph: Primer párrafo.
    Paragraph: Segundo párrafo.
```

## El flujo de bloque

Es como colocan a sus hijos en flujo un `Block`, cada sección (el área útil), `Frame`, `Overlay`, `Page overlay`, los
encabezados, `List`, `List item`, `Markdown`, `Table of contents`, una `Cell` con elementos y cada columna de un `Columns`. Los hijos
en flujo se apilan en vertical:

1. **Ancho.** Con `Width: auto`, la caja ocupa el ancho del contenedor de referencia menos sus márgenes horizontales. Con
   ancho conocido y los dos márgenes horizontales `auto`, el sobrante se reparte entre ambos y la caja queda **centrada**,
   y si solo uno es `auto`, ese absorbe el sobrante ([La caja](../common/box.md#márgenes-auto)).
2. **Posición.** La caja se coloca en la posición del cursor vertical, tras aplicar su margen superior fusionado con el
   anterior.
3. **Altura.** Se calcula su layout y el cursor avanza.

Con `Height: auto`, la altura del contenedor es la posición final del cursor más su relleno y su borde. **El ancho se
propaga de padres a hijos, y la altura, de hijos a padres.** Un `Overlay` o un `Page overlay` entre los hijos no ocupa sitio
([posicionados](frame-and-overlays.md)).

## Fusión de márgenes

Los márgenes **verticales** adyacentes en flujo de bloque se fusionan:

- entre hermanos consecutivos;
- entre un padre y su primer o último hijo, si el padre no tiene borde ni relleno en ese lado y no aísla los márgenes
  (el margen del hijo «sale» del padre);
- a través de cajas vacías (sin altura, borde, relleno ni contenido).

El resultado de fusionar varios márgenes es **el mayor positivo más el más negativo**: `20` y `30` dan 30, `20` y `−5`
dan 15, y `−10` y `−4` dan −10. En un corte de página, los márgenes que tocan el corte se truncan
([Cortes](../common/breaks.md#márgenes-en-los-cortes)).

El alto del contenido de una caja no baja de 0: si los márgenes negativos quitan más de lo que miden los hijos, la caja
mide su borde y su relleno, y el hijo que se sale por abajo es `BOX_OVERFLOW` en la línea del `Margin` negativo
([Desbordamiento](../common/overflow.md)).

> **Decisión:** el alto del contenido nunca es negativo. *Alternativa descartada:* dejar que la caja mida menos que su
> borde y su relleno, que pinta una caja más baja que su propio borde.

## Cajas que aíslan los márgenes

Una caja que aísla los márgenes corta su fusión: los márgenes de dentro no salen. Los aíslan: cada sección, `Overlay` y `Page overlay`, los ítems
de `Stack` y `Grid` (también las celdas de una `Table`), las columnas de un `Columns` y los `Block` y `Frame` con
`Isolate margins: true`. Los márgenes de los ítems de `Stack` y `Grid` nunca se fusionan.

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| `Isolate margins` | booleano | `false` | Aísla los márgenes de la caja: corta la fusión de márgenes con los hijos. Solo en `Block` y `Frame`, y no en un ítem de `Stack` o `Grid` (`ISOLATE_MARGINS_IN_ITEM`). |

> **Decisión:** `Isolate margins` solo está donde puede tener efecto: `Overlay`, `Page overlay` y los ítems ya aíslan, y `Stack`, `Grid`
> y `Text` no tienen hijos en flujo de bloque. En un `Block` o un `Frame` que es ítem, `Isolate margins: true` es un error.

## Tamaños intrínsecos

El ancho más estrecho de un `Block` es el máximo de los anchos más estrechos de sus hijos, y su ancho natural, el máximo de
sus anchos naturales ([La caja](../common/box.md#tamaños-intrínsecos)).

## Cortes

Un `Block` se parte entre sus hijos, o dentro del hijo que no cabe ([Cortes](../common/breaks.md)).

## Accesibilidad

Un `Block` agrupa su contenido sin anunciarse: un lector de pantalla lee lo que contiene ([El PDF](../common/pdf.md#accesibilidad)).

## Lo que no hay

- Cajas que floten a un lado del texto, o texto que rodee a otra caja.
- Recorte de lo que se sale (lo que se sale es un [desbordamiento](../common/overflow.md)).
