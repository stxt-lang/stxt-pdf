# `Barcode`

Un código QR o un código de barras que el motor genera a partir de sus datos: la dirección de verificación de un
certificado, el pago de una factura, la referencia de un pedido, el código de un producto o la etiqueta de un envío. No es un fichero de `images/`:
el motor lo dibuja con rectángulos, nítido a cualquier tamaño. Por lo demás es una imagen como otra: tiene su tamaño, sus
márgenes, su borde y su fondo, y se coloca en el flujo o en un `Stack`, un `Grid` o un posicionado.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Entrada
    Page:
        Width: 420
        Height: 200
        Margin: 24
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
    Section:
        Stack:
            Gap: 20
            Vertical: center
            Item:
                Barcode: https://teatro.example/entradas/2025-1187
                    Alt: Código QR de la entrada 2025-1187
                    Box:
                        Width: 110
            Item:
                Grow: 1
                Block:
                    Text: Teatro del Valle
                        Typography:
                            Size: 16
                            Bold: true
                    Text: Sábado 15 de noviembre de 2025, 20:30
                    Text: Platea, fila 7, butaca 12
                    Barcode: TV-2025-1187
                        Alt: Código de barras de la entrada TV-2025-1187
                        Barcode format: code-128
                        Box:
                            Width: 180
                            Height: 36
                            Margin: 12 0 0 0
```

## Propiedades

| Propiedad | Qué hace |
|---|---|
| *(valor)* | Los datos: una dirección, una referencia, un número. Si tienen varias líneas, van en un bloque `Content >>` |
| `Barcode format` | El tipo de código: `qr` (por defecto), `data-matrix`, `gs1-data-matrix`, `pdf417`, `code-128`, `gs1-128` o `ean-13` ([Los formatos](#los-formatos)) |
| `Error correction` | Solo en un QR: cuánto puede estropearse y seguir leyéndose. `low`, `medium` (por defecto), `quartile` o `high`. Cuanto más alto, más módulos para los mismos datos |
| `Quiet zone` | Los módulos en blanco a cada lado del código, de 0 a 40. Por defecto, los de la norma de cada formato ([El tamaño](#el-tamaño)) |
| `Readable text` | `true` pone los datos, como texto, bajo las barras de un `code-128`, un `gs1-128` o un `ean-13` ([El texto bajo las barras](#el-texto-bajo-las-barras)) |
| `Alt` | Lo que es y adónde lleva, para quien no lo ve. Obligatorio |
| `Rotate` | Girarlo sobre su centro, como una imagen ([Capas y giro](../common/layers-and-rotation.md#girar-una-caja-rotate)) |

Admite además `Id`, `Style`, `Link`, `Box`, `Break` (sin `Keep together` ni `Edges`: no se parte) y, de `Typography`, solo
`Color`, que es el color de los módulos (con `Readable text`, también la fuente del texto). El fondo es el de `Box`; sin
él, se ve lo que hay detrás.

## Los formatos

| `Barcode format` | Dónde se usa | Datos |
|---|---|---|
| `qr` | Direcciones, pagos, verificaciones: lo que lee un móvil | Cualquier texto, hasta unos 2300 caracteres con la corrección `medium` (menos si llevan acentos) |
| `data-matrix` | Etiquetas industriales y postales, piezas pequeñas | Cualquier texto, hasta unos 1500 caracteres |
| `gs1-data-matrix` | Medicamentos y material sanitario | Datos GS1 |
| `pdf417` | Tarjetas de embarque, impresos de la administración, documentos de identidad | Cualquier texto, hasta unos 1800 caracteres |
| `code-128` | Referencias de pedidos, albaranes, expedientes | Los caracteres ASCII imprimibles (letras sin acentos, cifras, el espacio y los signos), hasta 80 |
| `gs1-128` | Etiquetas logísticas: bultos, palés, cajas | Datos GS1, hasta 48 caracteres |
| `ean-13` | Productos | 12 cifras, y el motor añade la de control, o las 13 con la de control, que se comprueba |

Unos datos que el formato no puede codificar son un error (`BARCODE_DATA`) que dice qué falla: una `ñ` en un Code 128, una
cifra de control que no cuadra, un texto demasiado largo para un QR.

Los **datos GS1** se escriben como se leen en la etiqueta, con cada identificador de aplicación entre paréntesis delante
de su valor. Los paréntesis no se codifican, y el motor pone los caracteres de función (`FNC1`) que separan los valores:

```
Barcode: (01)09501101530003(17)261231(10)AB-123
    Alt: Código del producto 09501101530003, lote AB-123, caducidad 31-12-2026
    Barcode format: gs1-data-matrix
    Box:
        Width: 50
```

`(01)` es el código del producto (GTIN), `(17)` la fecha de caducidad y `(10)` el lote; `(00)` es el número de un bulto
(SSCC). El motor comprueba las cifras de los identificadores de longitud fija y la cifra de control de `(00)`, `(01)` y
`(02)`.

Unos datos de varias líneas (el pago de una factura, una tarjeta de contacto) van en un bloque `Content`, con las líneas tal
cual. Es el código QR de pago por transferencia que leen las aplicaciones de los bancos:

```
Barcode:
    Alt: Código QR para pagar la factura 2025-0147 por transferencia
    Box:
        Width: 80
    Content >>
        BCD
        002
        1
        SCT
        CAIXESBBXXX
        Taller Nogal S.L.
        ES9121000418450200051332
        EUR13360.82


        Factura 2025-0147
```

## El texto bajo las barras

`Readable text: true` escribe los datos bajo las barras de un `code-128`, un `gs1-128` o un `ean-13`, con la fuente, el
tamaño y el color de la caja:

```
Document (dev.stxt.pdf):
    Language: es
    Title: Etiqueta de producto
    Page:
        Width: 260
        Height: 190
        Margin: 20
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 9
    Section:
        Text: Aceite de oliva virgen extra, 500 ml
            Typography:
                Bold: true
        Barcode: 8412345678905
            Alt: Código del producto 8412345678905
            Barcode format: ean-13
            Readable text: true
            Box:
                Margin: 8 0 0 0
```

- En un **EAN-13** las cifras van como en un producto: la primera a la izquierda de las barras y las demás debajo, en dos
  grupos de seis, entre las barras de guarda, que son más largas.
- En un **Code 128** y un **GS1-128** es una línea centrada. La del GS1-128 lleva los paréntesis.
- El texto añade su alto al del código. Si la caja tiene `Height`, se lo quita a las barras.
- La fuente se cambia en la `Typography` del `Barcode`: `Font`, `Size`, `Bold`, `Weight`, `Italic` y `Color`.
- Si el texto no cabe bajo sus barras (una cifra más ancha que su hueco en un EAN-13 pequeño, o el texto de un Code 128
  más ancho que el código), es un error (`BARCODE_TEXT_OVERFLOW`): se arregla con un `Size` menor o una caja más ancha.
- En un QR, un Data Matrix o un PDF417 no hay texto: si hace falta, va en un `Text` al lado.

## El tamaño

- Sin `Width` ni `Height`, mide su **tamaño natural**, el de sus módulos: 0,5 mm cada módulo de un QR o un Data Matrix,
  0,33 mm de ancho por 1 mm de alto los de un PDF417, y 0,33 mm de ancho por 22,85 mm de alto las barras de un Code 128,
  un GS1-128 o un EAN-13. El margen en blanco que el lector necesita alrededor (la zona de silencio) va incluido: 4
  módulos a cada lado en un QR, 1 en un Data Matrix, 2 en un PDF417, 10 en un Code 128 y un GS1-128, y 11 a la izquierda
  y 7 a la derecha en un EAN-13.
- `Quiet zone` cambia esa zona por otro número de módulos a cada lado. En una caja del mismo ancho, menos zona deja más
  sitio a las barras, que salen más anchas: `Quiet zone: 5` en un Code 128 es lo que dejan muchos generadores de
  códigos. Con menos zona que la norma, algunos lectores pueden no leerlo.
- Con `Width` o `Height`, el otro lado sale de la proporción, como en una imagen. Lo habitual es dar solo `Width`.
- Un QR y un Data Matrix son siempre cuadrados, y un PDF417 guarda su proporción: en una caja que no la tiene, se dibujan
  lo más grandes que caben, centrados. Un código de barras de una fila se estira a la caja: con `Width` y `Height`, las
  barras ocupan todo el alto.
- No se parte entre páginas: si no cabe en lo que queda, pasa entero a la siguiente.
- No puede encoger hasta no leerse: si la caja deja los módulos por debajo de 0,25 mm, o de 0,264 mm en un EAN-13, es un
  error (`BARCODE_TOO_SMALL`) que dice qué tamaño necesita. Un QR de una dirección corta
  (33 módulos con la zona de silencio) necesita al menos 8,25 mm de lado, sin contar el `Padding`. En un código de barras
  solo cuenta el ancho.

Para que un móvil lo lea bien, un QR impreso no debería medir menos de 2 cm (unos 57 puntos) y los módulos tienen que
contrastar con el fondo: oscuros sobre claro.

## Accesibilidad

`Alt` es obligatorio: un lector de pantalla no puede leer el código, y el texto alternativo dice qué es y adónde lleva.
Dentro de un `Page overlay` es decorativo, como una imagen, y conviene que el texto de al lado diga lo mismo.

Más detalle en la [referencia de `Barcode`](../../reference/components/barcode.md).
