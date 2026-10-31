# Capas y giro

## Qué se pinta encima de qué

El contenido se pinta en el orden del documento: lo que va después, encima. Dentro de cada caja, primero los fondos y los
bordes de todo lo que hay en el flujo y después los textos y las imágenes, así que un fondo nunca tapa un texto de otra
caja. Las cajas posicionadas (`Frame`, `Overlay`, `Page overlay`) se pintan después, encima del flujo.

**`Layer`** cambia ese orden. Es un entero que admiten los elementos posicionados y los envoltorios `Item` y `Area`:

- **Negativa**: la caja va **detrás** del flujo (una marca de agua, un fondo de página).
- **Positiva**: **delante** de todo (un sello, una etiqueta).
- **A igual `Layer`**, manda el orden del documento.

```
Page overlay:
    Layer: -1
    Offset: 0
    Block:
        Box:
            Height: 100%
            Background: #FBF7EF
```

Una caja con `Layer` agrupa todo lo que contiene: su contenido se pinta junto, al nivel de esa caja, aunque una caja de
dentro tenga una `Layer` mayor.

## Girar una caja (`Rotate`)

`Rotate` gira una caja: un ángulo en grados, en el sentido de las agujas del reloj (entre −360 y 360, sin 0 ni las
vueltas enteras, que no giran nada). Gira todo lo que tiene la caja: fondo, borde, texto, imágenes y lo que contiene. El
texto girado sigue siendo texto en el PDF: se puede seleccionar y se lee igual. Lo girado tiene que caber en la página. Una caja
girada no se parte entre páginas: si no cabe en lo que queda, pasa entera a la siguiente.

Hay dos formas de girar, según la caja.

### En su sitio: `Block`, `Text`, `Paragraph` e `Image`

Una caja del flujo girada **ocupa el rectángulo que la envuelve girada**, y lo de alrededor se aparta. Es lo que hace
falta para un texto en vertical, como las cabeceras de unas columnas estrechas:

```
Document (dev.stxt.pdf):
    Language: es
    Title: Turnos de la semana
    Page:
        Paper: A5
        Orientation: landscape
        Margin: 40
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Cell
            Box:
                Padding: 4 6
                Border: 0 0 0.5 0
                Border color: #C8C4BA
        For element: Head cell
            Typography:
                Bold: true
    Section:
        Table:
            Column sizes: * auto auto auto
            Column align: left center center center
            Head:
                Row:
                    Cell: Persona
                    Cell:
                        Text: Mañana
                            Rotate: -90
                    Cell:
                        Text: Tarde
                            Rotate: -90
                    Cell:
                        Text: Noche
                            Rotate: -90
            Body:
                Row:
                    Cell: Ana Ruiz
                    Cell: sí
                    Cell: no
                    Cell: no
                Row:
                    Cell: Pau Vidal
                    Cell: no
                    Cell: sí
                    Cell: sí
```

- **Sin `Width`, mide su texto en una línea** y no lo parte por el ancho de su columna. Un párrafo largo girado necesita
  un `Width`, que es el ancho de sus líneas antes de girar.
- **Gira sobre su centro.** Con `Margin: 0 auto` se centra.
- **No puede ser directamente un ítem de un `Flex` o un `Grid`**, que le dan su tamaño. Lo que se gira es una caja dentro
  del ítem.

### Solo al pintar: `Frame`, `Overlay` y `Page overlay`

Un posicionado girado **no mueve nada**: la caja ocupa su sitio como si no estuviera girada. Es para lo que va encima de todo, como un sello o una etiqueta en diagonal:

```
Overlay:
    Offset: 10 10 auto auto
    Rotate: 12
    Box:
        Padding: 4 10
        Border: 1
        Border color: #A32D2D
    Text: BORRADOR
        Typography:
            Color: #A32D2D
            Bold: true
```

- **`Rotate origin`** es el punto alrededor del que gira: una o dos de `top`, `bottom`, `left`, `right` y `center` (por
  defecto, el centro), o dos medidas desde la esquina de arriba a la izquierda de la caja. `Rotate origin: top left` gira
  sobre esa esquina, y `Rotate origin: 20 50%`, sobre el punto 20 pt a la derecha de ella y a media altura.
- Un texto en vertical en el margen de la página es un `Page overlay` girado que lo contiene.

Más detalle en la [referencia](../../referencia/comun/capas-y-giro.md).
