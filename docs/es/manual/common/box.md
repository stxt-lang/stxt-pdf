# La caja: `Box`

El grupo `Box` lo admite cualquier elemento y no se hereda. Controla el tamaño, los márgenes, el relleno, el borde, el
fondo y las esquinas.

```
Block:
    Box:
        Width: 300
        Margin: 0 0 12 0
        Padding: 10 14
        Border: 0 0 0 3
        Border color: #B5651D
        Background: #F1EFE8
        Corner radius: 0 6 6 0
    Paragraph: Una nota con una franja de color a la izquierda.
```

| Propiedad | Qué hace |
|---|---|
| `Width`, `Height` | El ancho y el alto de la caja, con su relleno y su borde. Por defecto `auto`: el ancho que deja el padre y el alto del contenido |
| `Min width`, `Max width`, `Min height`, `Max height` | Límites del tamaño |
| `Aspect ratio` | La proporción: `16/9` o un número. Con un lado conocido, el otro sale de ella |
| `Margin` | El espacio de fuera, de 1 a 4 valores. Puede ser negativo o `auto` (para centrar) |
| `Padding` | El espacio de dentro, de 1 a 4 valores |
| `Border` | El grosor del borde, de 1 a 4 valores |
| `Border color` | El color del borde, de 1 a 4 colores. Por defecto, el del texto |
| `Corner radius` | Las esquinas redondeadas, de 1 a 4 valores |
| `Background` | El fondo: un color o un degradado |

## Tamaño

Sin `Width`, una caja ocupa todo el ancho que le deja su padre, menos sus márgenes. Sin `Height`, mide lo que su
contenido. El ancho va de fuera adentro y el alto, de dentro afuera.

`Width` y `Height` incluyen el relleno y el borde. Los porcentajes son del ancho o del alto de su contenedor
([Valores](values.md#porcentajes)).

**Mínimos y máximos.** Primero se aplica el máximo y luego el mínimo. Un mínimo mayor que el máximo es un error si los dos
están en las mismas unidades. Si uno es en puntos y otro en porcentaje y chocan al maquetar, gana el mínimo.

**Proporción.** Con `Aspect ratio: 16/9` y `Width: 320`, la caja mide 180 de alto. Con `Width` y `Height` a la vez es un
error, porque la proporción no tendría efecto.

## Márgenes

Los márgenes separan una caja de las de alrededor. Entre dos cajas una debajo de otra, los márgenes verticales **no se
suman: se queda el mayor**. Con 20 debajo de una caja y 30 encima de la siguiente, las separa 30, no 50. Lo mismo pasa
entre una caja y su primer o su último hijo si la caja no tiene borde ni relleno en ese lado: el margen del hijo sale fuera.
Para que no salga, la caja necesita un relleno, un borde o `Isolate margins: true` ([Block](../components/block.md)). Un margen
negativo junta las cajas, pero no puede sacar una caja de la página ni quitar a su padre más alto del que mide su
contenido: las dos cosas son un error (`BOX_OVERFLOW`).

**Centrar.** `Margin: 0 auto` centra en horizontal una caja con `Width` (o `Max width`): los dos márgenes laterales se
reparten lo que sobra. Un `auto` donde no hay nada que centrar es un error: en una caja sin ancho, que ya ocupa todo, o en
vertical. Los ítems de un `Stack` o un `Grid` se centran con sus propias propiedades de alineación.

## Borde y fondo

El borde se pinta entre el relleno y el margen. Con un color por lado, los lados se juntan en diagonal en las esquinas. Un
lado con grosor 0 no se ve, así que `Border: 0 0 0 3` es una franja a la izquierda. El fondo se pinta detrás del contenido
y del relleno, hasta el borde. Un fondo o un color con opacidad deja ver lo que hay debajo.

### Contraste

El texto tiene que distinguirse de su fondo, para que lo pueda leer quien ve peor: al convertir, un texto con poco
contraste es un error (`CONTRAST_LOW`) que dice cuánto tiene y cuánto necesita. Hace falta al menos **4,5:1**, o **3:1** en
el texto grande (`Size` de 18 o más, o de 14 en negrita). Como referencia, el gris `#767676` sobre blanco llega justo a
4,5:1, y el negro sobre blanco tiene 21:1.

Lo que cuenta es lo que hay de verdad debajo del texto: el fondo de su caja, el de las de fuera o el de una caja que tenga
detrás, mezclados si tienen opacidad. De un degradado cuenta el peor de sus colores. El texto encima de una imagen no se
comprueba, porque no se sabe qué hay debajo. Quien convierte puede desactivar la comprobación
([Uso desde Java](java-usage.md)).

## Esquinas

`Corner radius: 6` redondea las cuatro esquinas con un radio de 6 puntos. Con cuatro valores, cada esquina tiene el suyo.
`Corner radius: 50%` en una caja cuadrada es un círculo (una foto de perfil, una insignia). El fondo y el borde siguen la curva.
Una imagen se recorta con las esquinas de su caja. Lo que hay dentro de una caja redondeada también sigue su curva: el
fondo, el borde o la imagen de un hijo que llega a la esquina se recorta, sin tener que redondearlo. El texto no se recorta,
ni un `Overlay`, que puede asomar por la esquina a propósito.

## Lo que no cambia el tamaño

El fondo, el color del borde y las esquinas solo cambian cómo se pinta la caja, nunca dónde va ni cuánto mide.

Más detalle en la [referencia](../../reference/common/box.md).
