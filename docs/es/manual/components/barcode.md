# `Barcode`

Un código QR o un código de barras que el motor genera a partir de sus datos: la dirección de verificación de un
certificado, el pago de una factura, la referencia de un pedido o el código de un producto. No es un fichero de `images/`:
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
                        Format: code-128
                        Box:
                            Width: 180
                            Height: 36
                            Margin: 12 0 0 0
```

## Propiedades

| Propiedad | Qué hace |
|---|---|
| *(valor)* | Los datos: una dirección, una referencia, un número. Si tienen varias líneas, van en un bloque `Content >>` |
| `Format` | `qr` (por defecto), un código QR; `code-128`, un código de barras de letras, cifras y signos; `ean-13`, el código de barras de los productos |
| `Error correction` | Solo en un QR: cuánto puede estropearse y seguir leyéndose. `low`, `medium` (por defecto), `quartile` o `high`. Cuanto más alto, más módulos para los mismos datos |
| `Alt` | Lo que es y adónde lleva, para quien no lo ve. Obligatorio |
| `Rotate` | Girarlo sobre su centro, como una imagen ([Capas y giro](../common/layers-and-rotation.md#girar-una-caja-rotate)) |

Admite además `Id`, `Style`, `Link`, `Box`, `Break` (sin `Keep together` ni `Edges`: no se parte) y, de `Typography`, solo
`Color`, que es el color de los módulos. El fondo es el de `Box`; sin él, se ve lo que hay detrás.

## Los datos

- **QR:** cualquier texto, hasta unos 2300 caracteres con la corrección `medium` (menos si llevan acentos). Una
  dirección es lo más habitual.
- **Code 128:** los caracteres ASCII imprimibles (letras sin acentos, cifras, el espacio y los signos), hasta 80.
- **EAN-13:** 12 cifras, y el motor añade la de control, o las 13 con la de control, que se comprueba.

Unos datos que el formato no puede codificar son un error (`BARCODE_DATA`) que dice qué falla: una `ñ` en un Code 128, una
cifra de control que no cuadra, un texto demasiado largo para un QR.

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

## El tamaño

- Sin `Width` ni `Height`, mide su **tamaño natural**, el de sus módulos: 0,5 mm cada módulo de un QR, y 0,33 mm de ancho
  por 22,85 mm de alto las barras de un Code 128 o un EAN-13. El margen en blanco que el lector necesita alrededor (la
  zona de silencio) va incluido.
- Con `Width` o `Height`, el otro lado sale de la proporción, como en una imagen. Lo habitual es dar solo `Width`.
- Un QR es siempre cuadrado: en una caja que no lo es, se dibuja lo más grande que cabe, centrado. Un código de barras se
  estira a la caja: con `Width` y `Height`, las barras ocupan todo el alto.
- No se parte entre páginas: si no cabe en lo que queda, pasa entero a la siguiente.

Para que un móvil lo lea bien, un QR impreso no debería medir menos de 2 cm (unos 57 puntos) y los módulos tienen que
contrastar con el fondo: oscuros sobre claro.

## Accesibilidad

`Alt` es obligatorio: un lector de pantalla no puede leer el código, y el texto alternativo dice qué es y adónde lleva.
Dentro de un `Page overlay` es decorativo, como una imagen, y conviene que el texto de al lado diga lo mismo.

Más detalle en la [referencia de `Barcode`](../../reference/components/barcode.md).
