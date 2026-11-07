# `Barcode`

Un `Barcode` pone en el documento un código QR o un código de barras que el motor genera a partir de sus datos. Es una
imagen generada: no viene de un fichero, sino de su valor, y el motor la dibuja con rectángulos (los **módulos**). Por lo
demás se maqueta, se corta entre páginas y se pinta como una [`Image`](image.md), con su `Box` (márgenes, relleno, borde,
fondo y esquinas) y su `Break` (sin `Keep together` ni `Edges`, porque no se parte). Admite además `Id`, `Style`, `Link`,
`Rotate` y, de `Typography`, solo `Color`.

```
Document (dev.stxt.pdf):
    Language: es
    Title: Albarán
    Page:
        Paper: A5
        Margin: 30
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
    Section:
        Stack:
            Horizontal: justify
            Vertical: end
            Item:
                Block:
                    Text: Albarán AL-2025-0388
                        Typography:
                            Bold: true
                    Barcode: AL-2025-0388
                        Alt: Código de barras del albarán AL-2025-0388
                        Format: code-128
                        Box:
                            Width: 150
                            Height: 30
                            Margin: 6 0 0 0
            Item:
                Barcode: https://tallernogal.example/albaranes/AL-2025-0388
                    Alt: Código QR con el seguimiento del albarán
                    Error correction: quartile
                    Box:
                        Width: 72
                        Padding: 4
                        Border: 0.5
                        Border color: #A65C1B
                    Typography:
                        Color: #3B2A1E
```

(Un albarán con su número en Code 128 a la izquierda y, a la derecha, un QR con la dirección de seguimiento, con módulos
marrón oscuro dentro de un marco.)

| Propiedad | Valores | Por defecto | Notas |
|---|---|---|---|
| *(valor)* | los datos | obligatorio (o `Content`) | Lo que codifica: `Barcode: https://example.com/verify/1234`. |
| `Content` | bloque `>>` | sin valor | Los datos de varias líneas, con las líneas tal cual. O el valor o `Content`, uno de los dos. |
| `Format` | `qr`, `code-128`, `ean-13` | `qr` | El tipo de código ([Formatos](#formatos)). |
| `Error correction` | `low`, `medium`, `quartile`, `high` | `medium` | Solo en un QR: la parte de los módulos que puede estropearse y el código sigue leyéndose, un 7 %, 15 %, 25 % y 30 %. En otro formato es un error. |
| `Quiet zone` | entero de 0 a 40 | la del formato | Los módulos claros a cada lado del código ([Los módulos](#los-módulos)). |
| `Alt` | texto no vacío | obligatorio | Qué es y adónde lleva, para quien no lo ve ([Accesibilidad](#accesibilidad)). |
| `Rotate` | ver [Capas y giro](../common/layers-and-rotation.md#giro-en-flujo-block-text-paragraph-e-image) | sin valor | Lo gira sobre su centro, como una `Image`. |
| `Side` | `left`, `right` | sin valor | Lo hace una caja lateral, como una `Image`: el texto que lo sigue corre a su lado ([Block](block.md#cajas-laterales-side)). |
| `Clear` | booleano | `false` | Lo baja por debajo de las cajas laterales que hay antes ([Block](block.md#cajas-laterales-side)). |

`Format`, `Error correction`, `Quiet zone`, `Side` y `Clear` también pueden venir de una regla de `Styles`
([Propiedades de elemento](../common/styles.md#propiedades-de-elemento)); los datos, `Alt` y `Rotate`, no.

## Formatos

| `Format` | Qué es | Datos |
|---|---|---|
| `qr` | Código QR (ISO/IEC 18004): una matriz cuadrada de módulos | Cualquier texto. Si es todo ASCII va tal cual; si no, en UTF-8, con el indicador ECI que se lo dice al lector. Caben 2331 bytes con `Error correction: medium`, 2953 con `low` y 1273 con `high` (más si son solo cifras o mayúsculas). |
| `code-128` | Code 128 (ISO/IEC 15417): una fila de barras | Los caracteres ASCII imprimibles, del espacio a `~`, de 1 a 80. |
| `ean-13` | EAN-13 (ISO/IEC 15420), el código de los productos | 12 cifras, a las que el motor añade la de control, o 13 con la de control, que tiene que cuadrar. |

Unos datos vacíos, en el valor y en `Content` a la vez, o que el formato no puede codificar son un error (`BARCODE_DATA`,
con la línea del `Barcode`), con lo que falla: el carácter que no admite el Code 128, la cifra de control que tocaría en
un EAN-13 o los bytes que no caben en el QR. `Error correction` en un `code-128` o un `ean-13` es
`BARCODE_PROPERTY_WITHOUT_EFFECT`.

El valor de una línea se toma sin los blancos de los extremos. En `Content` las líneas van separadas por un salto de línea
(`LF`), con su sangría relativa y las líneas vacías de en medio: es lo que necesitan los datos con un formato por líneas,
como el código QR de pago por transferencia (EPC) o una tarjeta de contacto.

> **Decisión:** los datos de varias líneas van en `Content`, como el texto de un `Text`. *Alternativa descartada:* una
> secuencia de escape en el valor (`\n`), que STXT no tiene y que habría que inventar solo aquí.

> **Decisión:** un QR con datos que no son ASCII va en UTF-8 con su indicador ECI, y uno que lo es, sin él. *Alternativa
> descartada:* el indicador siempre, que algunos lectores antiguos no entienden y que no cambia nada en ASCII.

## Los módulos

El dibujo de un `Barcode` es una matriz de módulos, cada uno oscuro o claro, que incluye la **zona de silencio**: el margen
claro que el lector necesita alrededor del código. Es parte de la caja de contenido.

| `Format` | Zona de silencio | Módulo natural |
|---|---|---|
| `qr` | 4 módulos a cada lado | 0,5 × 0,5 mm |
| `code-128` | 10 módulos a cada lado | 0,33 mm de ancho; las barras, 22,85 mm de alto |
| `ean-13` | 11 módulos a la izquierda y 7 a la derecha | 0,33 mm de ancho; las barras, 22,85 mm de alto |

Esas zonas son las de cada norma, y valen si no se declara otra. `Quiet zone: N` pone `N` módulos claros a cada lado, en
los tres formatos (en un EAN-13, los mismos a la izquierda y a la derecha), de 0 a 40; fuera de ese intervalo es
`VALUE_NOT_ALLOWED`. Como la zona es parte del tamaño natural, en una caja de ancho dado menos módulos de zona dan módulos
más anchos: un Code 128 de 150 puntos de ancho con `Quiet zone: 5` tiene las barras más anchas que con los 10 de la
norma. Con `Quiet zone: 0`, el código llega al borde de la caja de contenido, y el margen claro tiene que darlo el
`Padding` con un fondo claro o lo que haya alrededor. Una zona menor que la de la norma puede impedir la lectura con
algunos lectores: el motor no lo comprueba.

> **Decisión:** `Quiet zone` cuenta módulos, no milímetros, y vale lo mismo en los tres formatos. Las normas la dan en
> módulos, y así la zona se escala con el código. *Alternativa descartada:* una longitud (`Quiet zone: 2mm`), que con el
> mismo valor daría una zona distinta según el ancho de la caja, y una propiedad solo para el Code 128, que dejaría el QR
> y el EAN-13 sin la misma posibilidad.

> **Decisión:** se admite una zona menor que la de la norma, sin aviso. Muchos lectores leen con menos (ZXing, al
> dibujar un Code 128, deja 5 módulos a cada lado), y quien imprime el código es quien sabe con qué se lee.
> *Alternativa descartada:* un mínimo por formato, que impediría reproducir documentos que ya se imprimen con una zona
> menor.

Los módulos oscuros se pintan del `Color` de la tipografía, el suyo o el heredado (negro si nadie lo declara), y los claros
no se pintan: se ve el fondo de la caja (`Background`) o, sin él, lo que haya detrás. De su `Typography`, un `Barcode`
solo admite `Color`: cualquier otra propiedad es `BARCODE_PROPERTY_WITHOUT_EFFECT`. Lo que dé una regla de `Styles` no se
comprueba, como en cualquier caja sin texto.

El motor no comprueba el contraste de los módulos con el fondo ([Contraste](../common/pdf.md#contraste) es solo del texto):
un código claro sobre un fondo claro no se lee.

> **Decisión:** el color de los módulos es el `Color` de `Typography`, que se hereda y se pone en las reglas como el del
> texto. *Alternativa descartada:* una propiedad propia (`Module color`), otro nombre para lo mismo.

> **Decisión:** el motor genera los códigos con ZXing (Apache 2.0), que es Java puro y no dibuja: da la matriz, y el lienzo
> la pinta con rectángulos. *Alternativa descartada:* un codificador QR propio (Reed-Solomon, máscaras y versiones), unas
> quinientas líneas que mantener para lo mismo.

## Tamaño

Un `Barcode` se dimensiona como una [`Image`](image.md#dimensionado) cuyo tamaño natural es el de sus módulos, con la zona
de silencio (la de la norma o la de `Quiet zone`): un QR de 25 módulos (33 con la zona) mide 16,5 × 16,5 mm, y un EAN-13 (113 módulos), 37,29 × 22,85 mm. Con
`Width` o `Height`, el otro lado sale de esa proporción, y `Aspect ratio`, si se declara, la sustituye. Los tamaños
intrínsecos son los de una imagen.

La caja de contenido no tiene por qué tener la proporción del código. Si no la tiene:

- un **QR** se dibuja entero, lo más grande que cabe y centrado, con los módulos cuadrados (como `Fit: inside`);
- un **código de barras** se estira a la caja (como `Fit: stretch`): las barras ocupan todo el alto, y todos los módulos
  tienen el mismo ancho.

Un `Barcode` no admite `Fit`.

> **Decisión:** el tamaño natural sale de un módulo fijo: 0,5 mm en un QR y el tamaño nominal del EAN-13 (0,33 × 22,85 mm)
> en los códigos de barras. Así un `Barcode` sin tamaño se ve y se lee, y se mide en un `Stack` o un `Grid` como una imagen.
> *Alternativa descartada:* exigir `Width` o `Height`, que deja sin tamaño intrínseco al que lo declara en porcentaje.

> **Decisión:** un QR en una caja que no es cuadrada se dibuja centrado, sin error mientras sus módulos no bajen del
> [mínimo](#módulo-mínimo). *Alternativa descartada:* un error si `Width` y `Height` no son iguales, que con porcentajes no
> se puede saber antes de maquetar.

### Módulo mínimo

Un código encogido hasta que no se puede leer es un error. Cada formato tiene un módulo mínimo:

| `Format` | Módulo mínimo | De dónde sale |
|---|---|---|
| `qr` | 0,25 mm | Unos tres puntos de una impresora de 300 ppp. ISO/IEC 18004 no fija ninguno |
| `code-128` | 0,25 mm | El mismo criterio. ISO/IEC 15417 no fija ninguno |
| `ean-13` | 0,264 mm | El 80 % del módulo nominal (0,33 mm), el menor que admite ISO/IEC 15420 |

Si la caja de contenido, una vez maquetada, deja los módulos más estrechos, es un error (`BARCODE_TOO_SMALL`, con la línea
del `Barcode`) que da el módulo que queda y el tamaño mínimo de la caja de contenido. En un QR cuenta el lado más corto (en
una caja ancha y baja, el alto); en un código de barras, solo el ancho, porque estirar las barras a lo alto no cambia el
módulo. La zona de silencio forma parte del dibujo: un QR de 25 módulos más 4 de zona a cada lado (33) necesita una caja de
contenido de 8,25 × 8,25 mm (23,39 pt), y el `Padding` no cuenta. Se comprueba con el tamaño final, después de paginar: en
un `Stack` o un `Grid` un código se mide varias veces, y solo cuenta la última.

Quien convierte puede quitar la comprobación (`Check.BARCODE_SIZE`, [Uso desde Java](../implementation/api.md#comprobaciones)),
y entonces el código se dibuja tan pequeño como su caja.

> **Decisión:** el mínimo es fijo por formato y no hay propiedad en el documento para cambiarlo; quien necesita dibujar un
> código más pequeño quita la comprobación al convertir. *Alternativa descartada:* una propiedad del `Barcode`
> (`Min module`), otro valor que escribir en cada documento para un caso raro; y dibujar el código encogido sin aviso, como
> hasta la 0.9.0, que da un resultado a medias.

> **Decisión:** 0,25 mm en QR y Code 128, el módulo más pequeño que una impresora de oficina de 300 ppp dibuja con tres
> puntos. *Alternativa descartada:* los mínimos de GS1 (0,396 mm en un QR de punto de venta y 0,495 mm en un GS1-128 de
> logística), pensados para los lectores de caja y de almacén, que dejarían fuera los códigos que llevan las facturas y los
> certificados.

## Cortes de página

Como una imagen: no se parte, y si no cabe en lo que queda de la página (o de la columna), pasa entero a la siguiente.
`Keep together` y `Edges` son un error (`BREAK_IN_IMAGE`), también si los da una regla de `Styles`.

## Dónde va

Donde va una `Image`: en una sección, `Block`, `Frame`, `Overlay`, `Page overlay`, `Columns`, `List item` (pero no al
principio, `LIST_ITEM_WITHOUT_LINE`), `Cell` y como elemento de un `Item` o un `Area`. Un `Markdown` no los genera.

## Accesibilidad

`Alt` es obligatorio (`BARCODE_WITHOUT_ALT`, con la línea del `Barcode`): con él, el código es una figura (`Figure`) que un
lector de pantalla anuncia con ese texto. Dentro de un `Page overlay` es decorativo siempre, como una imagen, y en las filas
repetidas de una tabla solo se lee la primera vez.

> **Decisión:** `Alt` es obligatorio. *Alternativa descartada:* un `Barcode` decorativo sin `Alt`, como una `Image`: un
> código lleva información que solo dicen sus datos, y quien no lo ve no tiene otra forma de saber qué es.

## Salida

En el PDF, cada fila de módulos son rectángulos (uno por cada tramo de módulos oscuros seguidos), todos en un mismo trazado
que se rellena una vez, para que no se vea ninguna línea entre módulos vecinos. Es un dibujo vectorial, nítido a
cualquier tamaño y a cualquier resolución de impresión. El mismo dato en el mismo formato da siempre los mismos módulos, y
el mismo PDF.

## Lo que no hay

- Otros formatos: Data Matrix, PDF417, Aztec, Code 39, EAN-8, UPC-A, ni los caracteres de función de GS1-128.
- El texto legible bajo las barras de un EAN-13 o un Code 128: va en un `Text`.
- Elegir la versión o la máscara del QR.
- Una zona de silencio distinta a cada lado: `Quiet zone` es la misma en los dos.
- Un alto mínimo de las barras: un código de barras bajo se dibuja sin aviso.
- Cambiar el módulo mínimo desde el documento.
- Un logotipo en el centro de un QR.
- Comprobar el contraste de los módulos con el fondo.
