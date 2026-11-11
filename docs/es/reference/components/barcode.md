# `Barcode`

Un `Barcode` pone en el documento un código QR o un código de barras que el motor genera a partir de sus datos. Es una
imagen generada: no viene de un fichero, sino de su valor, y el motor la dibuja con rectángulos (los **módulos**). Por lo
demás se maqueta, se corta entre páginas y se pinta como una [`Image`](image.md), con su `Box` (márgenes, relleno, borde,
fondo y esquinas) y su `Break` (sin `Keep together` ni `Edges`, porque no se parte). Admite además `Id`, `Style`, `Link`,
`Rotate` y, de `Typography`, solo `Color` (y con [`Readable text`](#texto-legible), las de la fuente de ese texto).

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
            Vertical: bottom
            Item:
                Block:
                    Text: Albarán AL-2025-0388
                        Typography:
                            Bold: true
                    Barcode: AL-2025-0388
                        Alt: Código de barras del albarán AL-2025-0388
                        Barcode format: code-128
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
| `Barcode format` | `qr`, `data-matrix`, `gs1-data-matrix`, `pdf417`, `code-128`, `gs1-128`, `ean-13` | `qr` | El tipo de código ([Formatos](#formatos)). |
| `Error correction` | `low`, `medium`, `quartile`, `high` | `medium` | Solo en un QR: la parte de los módulos que puede estropearse y el código sigue leyéndose, un 7 %, 15 %, 25 % y 30 %. En otro formato es un error. |
| `Quiet zone` | entero de 0 a 40 | la del formato | Los módulos claros a cada lado del código ([Los módulos](#los-módulos)). |
| `Readable text` | booleano | `false` | Solo en `code-128`, `gs1-128` y `ean-13`: los datos, como texto, bajo las barras ([Texto legible](#texto-legible)). En otro formato es un error. |
| `Alt` | texto no vacío | obligatorio | Qué es y adónde lleva, para quien no lo ve ([Accesibilidad](#accesibilidad)). |
| `Rotate` | ver [Capas y giro](../common/layers-and-rotation.md#giro-en-flujo-block-text-paragraph-e-image) | sin valor | Lo gira sobre su centro, como una `Image`. |
| `Side` | `left`, `right` | sin valor | Lo hace una caja lateral, como una `Image`: el texto que lo sigue corre a su lado ([Block](block.md#cajas-laterales-side)). |
| `Below side boxes` | booleano | `false` | Lo baja por debajo de las cajas laterales que hay antes ([Block](block.md#cajas-laterales-side)). |

`Barcode format`, `Error correction`, `Quiet zone`, `Readable text`, `Side` y `Below side boxes` también pueden venir de una regla de `Styles`
([Propiedades de elemento](../common/styles.md#propiedades-de-elemento)); los datos, `Alt` y `Rotate`, no.

## Formatos

| `Barcode format` | Qué es | Datos |
|---|---|---|
| `qr` | Código QR (ISO/IEC 18004): una matriz cuadrada de módulos | Cualquier texto. Si es todo ASCII va tal cual; si no, en UTF-8, con el indicador ECI que se lo dice al lector. Caben 2331 bytes con `Error correction: medium`, 2953 con `low` y 1273 con `high` (más si son solo cifras o mayúsculas). |
| `data-matrix` | Data Matrix ECC 200 (ISO/IEC 16022), cuadrado: el código de las etiquetas industriales y postales | Cualquier texto, con la misma regla que el QR: ASCII tal cual y, si no, UTF-8 con su indicador ECI. Caben 1555 bytes (más si son solo cifras o mayúsculas). |
| `gs1-data-matrix` | GS1 DataMatrix: un Data Matrix con datos GS1, el de los medicamentos y el material sanitario | Una lista de identificadores de aplicación ([Datos GS1](#datos-gs1)). |
| `pdf417` | PDF417 (ISO/IEC 15438): filas de barras apiladas, el de las tarjetas de embarque y los impresos de la administración | Cualquier texto, con la misma regla: ASCII tal cual y, si no, UTF-8 con su indicador ECI. Caben unos 1800 caracteres de texto. |
| `code-128` | Code 128 (ISO/IEC 15417): una fila de barras | Los caracteres ASCII imprimibles, del espacio a `~`, de 1 a 80. |
| `gs1-128` | GS1-128: un Code 128 con datos GS1, el de las etiquetas logísticas | Una lista de identificadores de aplicación ([Datos GS1](#datos-gs1)), de 48 caracteres como mucho. |
| `ean-13` | EAN-13 (ISO/IEC 15420), el código de los productos | 12 cifras, a las que el motor añade la de control, o 13 con la de control, que tiene que cuadrar. |

Unos datos vacíos, en el valor y en `Content` a la vez, o que el formato no puede codificar son un error (`BARCODE_DATA`,
con la línea del `Barcode`), con lo que falla: el carácter que no admite el Code 128, la cifra de control que tocaría en
un EAN-13, los bytes que no caben en el QR o el identificador GS1 mal escrito. `Error correction` fuera de un `qr` es
`BARCODE_PROPERTY_WITHOUT_EFFECT`: un Data Matrix tiene la suya, fija, y un PDF417 lleva el nivel que su norma recomienda
para el largo de los datos (2 hasta 40 caracteres, 3 hasta 160, 4 hasta 320 y 5 a partir de ahí).

> **Decisión:** un Data Matrix es siempre cuadrado, y el motor elige el más pequeño en que caben los datos. *Alternativa
> descartada:* los símbolos rectangulares de la norma y una propiedad para pedirlos, que solo usan las etiquetas muy
> estrechas.

> **Decisión:** `Error correction` es solo del QR, y el nivel de un PDF417 lo pone el motor con la tabla de su norma.
> *Alternativa descartada:* los nueve niveles del PDF417 (0 a 8) en la misma propiedad, que tiene cuatro valores con
> nombre, o en otra.

El valor de una línea se toma sin los blancos de los extremos. En `Content` las líneas van separadas por un salto de línea
(`LF`), con su sangría relativa y las líneas vacías de en medio: es lo que necesitan los datos con un formato por líneas,
como el código QR de pago por transferencia (EPC) o una tarjeta de contacto.

> **Decisión:** los datos de varias líneas van en `Content`, como el texto de un `Text`. *Alternativa descartada:* una
> secuencia de escape en el valor (`\n`), que STXT no tiene y que habría que inventar solo aquí.

> **Decisión:** un QR con datos que no son ASCII va en UTF-8 con su indicador ECI, y uno que lo es, sin él. *Alternativa
> descartada:* el indicador siempre, que algunos lectores antiguos no entienden y que no cambia nada en ASCII.

### Datos GS1

Los datos de un `gs1-128` y de un `gs1-data-matrix` son una lista de **identificadores de aplicación**, cada uno entre
paréntesis delante de su valor, como se leen en la etiqueta:

```
Barcode: (01)09501101530003(17)261231(10)AB-123
    Alt: Código del producto 09501101530003, lote AB-123, caducidad 31-12-2026
    Barcode format: gs1-data-matrix
```

Los paréntesis no se codifican. El motor pone el carácter de función `FNC1` al principio del símbolo, que es lo que le
dice al lector que los datos son GS1, y otro detrás de cada valor de longitud variable que no es el último; los
identificadores de longitud predefinida no lo llevan. Se comprueba:

- la forma: cada identificador, de 2 a 4 cifras entre paréntesis, con un valor detrás;
- los de **longitud predefinida** (los que empiezan por `00` a `04`, `11` a `20`, `31` a `36` y `41`): las cifras del
  identificador y las de su valor, que son solo cifras. `(01)`, un GTIN, tiene 14; `(00)`, un SSCC, 18; `(17)`, una
  fecha, 6;
- la **cifra de control** de `(00)`, `(01)` y `(02)`, que tiene que cuadrar;
- los demás valores: de 1 a 90 caracteres, que son cifras, letras sin acentos y los signos `!"%&'*+,-./:;<=>?_`.

Lo que no cuadra es `BARCODE_DATA`, con lo que falla. El formato del valor de cada identificador de longitud variable
(que `(10)` tenga 20 caracteres como mucho, o que `(15)` sea una fecha que existe) no se comprueba.

> **Decisión:** los datos GS1 se escriben con los identificadores entre paréntesis, que es como los imprime la propia
> etiqueta y como los da quien los genera, y el motor pone los `FNC1`. *Alternativa descartada:* un escape para el
> `FNC1` en el valor de un `code-128`, que obliga a quien escribe a saber qué identificadores lo llevan detrás.

> **Decisión:** `gs1-128` y `gs1-data-matrix` son formatos, con los nombres de GS1. *Alternativa descartada:* una
> propiedad booleana (`GS1: true`) sobre `code-128` y `data-matrix`, que sería un error en los demás formatos y deja el
> formato a medio decir.

> **Decisión:** un `gs1-data-matrix` se codifica en el modo ASCII de la norma (un carácter por palabra de código y dos
> cifras por palabra), en el motor, y ZXing pone la corrección de errores y coloca los módulos. *Alternativa
> descartada:* el codificador compacto de ZXing 3.5.4, el único suyo que conoce el `FNC1`, que lo escribe mal dentro
> de un tramo de mayúsculas y cifras. Los datos GS1 son sobre todo cifras, que el modo ASCII empaqueta de dos en dos.

## Los módulos

El dibujo de un `Barcode` es una matriz de módulos, cada uno oscuro o claro, que incluye la **zona de silencio**: el margen
claro que el lector necesita alrededor del código. Es parte de la caja de contenido.

| `Barcode format` | Zona de silencio | Módulo natural |
|---|---|---|
| `qr` | 4 módulos a cada lado | 0,5 × 0,5 mm |
| `data-matrix`, `gs1-data-matrix` | 1 módulo a cada lado | 0,5 × 0,5 mm |
| `pdf417` | 2 módulos a cada lado | 0,33 mm de ancho; cada fila, 1 mm de alto (tres módulos) |
| `code-128`, `gs1-128` | 10 módulos a cada lado | 0,33 mm de ancho; las barras, 22,85 mm de alto |
| `ean-13` | 11 módulos a la izquierda y 7 a la derecha | 0,33 mm de ancho; las barras, 22,85 mm de alto |

Esas zonas son las de cada norma, y valen si no se declara otra. `Quiet zone: N` pone `N` módulos claros a cada lado, en
todos los formatos (en un EAN-13, los mismos a la izquierda y a la derecha; en los de matriz y el PDF417, también
arriba y abajo), de 0 a 40; fuera de ese intervalo es
`VALUE_NOT_ALLOWED`. Como la zona es parte del tamaño natural, en una caja de ancho dado menos módulos de zona dan módulos
más anchos: un Code 128 de 150 puntos de ancho con `Quiet zone: 5` tiene las barras más anchas que con los 10 de la
norma. Con `Quiet zone: 0`, el código llega al borde de la caja de contenido, y el margen claro tiene que darlo el
`Padding` con un fondo claro o lo que haya alrededor. Una zona menor que la de la norma puede impedir la lectura con
algunos lectores: el motor no lo comprueba.

> **Decisión:** `Quiet zone` cuenta módulos, no milímetros, y vale lo mismo en todos los formatos. Las normas la dan en
> módulos, y así la zona se escala con el código. *Alternativa descartada:* una longitud (`Quiet zone: 2mm`), que con el
> mismo valor daría una zona distinta según el ancho de la caja, y una propiedad solo para el Code 128, que dejaría el QR
> y el EAN-13 sin la misma posibilidad.

> **Decisión:** se admite una zona menor que la de la norma, sin aviso. Muchos lectores leen con menos (ZXing, al
> dibujar un Code 128, deja 5 módulos a cada lado), y quien imprime el código es quien sabe con qué se lee.
> *Alternativa descartada:* un mínimo por formato, que impediría reproducir documentos que ya se imprimen con una zona
> menor.

Los módulos oscuros se pintan del `Color` de la tipografía, el suyo o el heredado (negro si nadie lo declara), y los claros
no se pintan: se ve el fondo de la caja (`Background`) o, sin él, lo que haya detrás. De su `Typography`, un `Barcode`
solo admite `Color` y, con [`Readable text`](#texto-legible), las de la fuente del texto: cualquier otra propiedad es
`BARCODE_PROPERTY_WITHOUT_EFFECT`. Lo que dé una regla de `Styles` no se
comprueba, como en cualquier caja sin texto.

El motor no comprueba el contraste de los módulos con el fondo ([Contraste](../common/pdf.md#contraste) es solo del texto):
un código claro sobre un fondo claro no se lee.

> **Decisión:** el color de los módulos es el `Color` de `Typography`, que se hereda y se pone en las reglas como el del
> texto. *Alternativa descartada:* una propiedad propia (`Module color`), otro nombre para lo mismo.

> **Decisión:** el motor genera los códigos con ZXing (Apache 2.0), que es Java puro y no dibuja: da la matriz, y el lienzo
> la pinta con rectángulos. *Alternativa descartada:* un codificador QR propio (Reed-Solomon, máscaras y versiones), unas
> quinientas líneas que mantener para lo mismo.

## Texto legible

`Readable text: true` pone los datos, como texto, bajo las barras de un `code-128`, un `gs1-128` o un `ean-13`. En un
`qr`, un `data-matrix`, un `gs1-data-matrix` o un `pdf417` es `BARCODE_PROPERTY_WITHOUT_EFFECT` (con `false`, que es el
valor por defecto, no).

```
Document (dev.stxt.pdf):
    Language: es
    Title: Etiqueta
    Page:
        Width: 300
        Height: 220
        Margin: 20
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 9
        For element: Barcode
            Readable text: true
    Section:
        Barcode: 8412345678905
            Alt: Código del producto 8412345678905
            Barcode format: ean-13
        Barcode: (00)395011015300000011
            Alt: Código del bulto 395011015300000011
            Barcode format: gs1-128
            Box:
                Width: 220
                Height: 50
                Margin: 12 0 0 0
            Typography:
                Size: 8
                Font: IBM Plex Mono
```

(Un EAN-13 con sus cifras como en un producto y, debajo, un GS1-128 con su texto centrado en una fuente monoespaciada.)

Qué texto es y dónde va:

| `Barcode format` | Texto | Disposición |
|---|---|---|
| `ean-13` | Las 13 cifras, con la de control | Como en un producto: la primera cifra a la izquierda de las barras, en la zona de silencio (centrada en sus 7 últimos módulos), y cada una de las otras doce centrada bajo los 7 módulos que la codifican. Las barras de guarda (las del principio, el centro y el final) bajan hasta la mitad del texto. |
| `code-128` | Los datos | Una línea centrada bajo el código, zona de silencio incluida. |
| `gs1-128` | Los datos, con los paréntesis de sus identificadores | Una línea centrada, igual. |

El texto se pinta con la fuente de la caja: `Font`, `Size`, `Bold` o `Weight`, `Italic` y `Color`, de su `Typography` o
heredadas. Con `Readable text`, la `Typography` del `Barcode` admite esas propiedades; las demás (`Align`,
`Letter spacing`, `Case`...) siguen siendo `BARCODE_PROPERTY_WITHOUT_EFFECT`. Hace falta una fuente y un tamaño, como
en cualquier texto (`TEXT_WITHOUT_FONT`, `TEXT_WITHOUT_FONT_SIZE`, con la línea de `Readable text`), y la fuente tiene que
tener sus caracteres (`GLYPH_MISSING`).

El texto ocupa, al pie de la caja de contenido, el alto de su fuente (el ascendente más el descendente, a su `Size`), y
las barras, el resto:

- sin `Height`, ese alto **se suma** al natural del código: un EAN-13 con un texto de 9 pt en IBM Plex Sans mide
  37,29 mm de ancho y 22,85 mm más el alto del texto, y sus barras siguen midiendo 22,85 mm;
- con `Height`, o con `Width` y la proporción, el texto **se toma** de la caja y las barras se acortan.

Un texto que no cabe es un error (`BARCODE_TEXT_OVERFLOW`, con la línea del `Barcode`), que se comprueba con el tamaño
final de la caja, como el [módulo mínimo](#módulo-mínimo), y que ninguna comprobación quita:

- un tramo más ancho que la caja de contenido o que se sale de ella (el texto de un Code 128 más ancho que el código);
- una cifra de un EAN-13 más ancha que sus 7 módulos, que pisa a la anterior;
- la primera cifra de un EAN-13 con una `Quiet zone` de menos de 7 módulos, que no le deja sitio;
- un texto tan alto como la caja de contenido, que no deja alto a las barras.

Se arregla con un `Size` menor o una caja mayor. En un EAN-13 de tamaño natural, cada cifra tiene 2,31 mm (6,55 pt): caben
las de IBM Plex Sans hasta 10 pt.

En el PDF etiquetado, el texto es parte de la figura del código, que se lee con su `Alt`: un lector de pantalla no lo
anuncia aparte. Se puede seleccionar y buscar.

> **Decisión:** `Readable text` es un booleano y la disposición la pone el formato: la de la norma en un EAN-13 y una
> línea centrada en los demás. *Alternativa descartada:* una propiedad con la posición (`above`, `below`) y la
> alineación, que las normas no dejan elegir en un EAN-13 y que en un Code 128 se resuelve con un `Text` al lado.

> **Decisión:** el texto se pinta con la fuente de la caja, la que elige el documento. *Alternativa descartada:* la
> OCR-B de la norma del EAN-13, una fuente más que traer con el motor; quien la necesite la pone en `fonts/` y la
> declara en `Font`.

> **Decisión:** el alto del texto se suma al tamaño natural, y las barras conservan el suyo. *Alternativa descartada:*
> quitárselo siempre a las barras, que cambia el alto de las barras de un código al ponerle el texto.

> **Decisión:** un texto que no cabe es un error. *Alternativa descartada:* encogerlo hasta que quepa, que da cifras
> de un tamaño que el documento no ha pedido, y recortarlo, que es un resultado a medias.

## Tamaño

Un `Barcode` se dimensiona como una [`Image`](image.md#dimensionado) cuyo tamaño natural es el de sus módulos, con la zona
de silencio (la de la norma o la de `Quiet zone`): un QR de 25 módulos (33 con la zona) mide 16,5 × 16,5 mm, y un EAN-13 (113 módulos), 37,29 × 22,85 mm. Con
`Width` o `Height`, el otro lado sale de esa proporción, y `Aspect ratio`, si se declara, la sustituye. Los tamaños
intrínsecos son los de una imagen.

La caja de contenido no tiene por qué tener la proporción del código. Si no la tiene:

- un **QR**, un **Data Matrix** y un **PDF417** se dibujan enteros, lo más grande que caben y centrados, con la
  proporción de sus módulos (como `Fit: inside`): cuadrados en los dos primeros, y tres veces más altos que anchos en
  un PDF417;
- un **código de barras de una fila** (`code-128`, `gs1-128`, `ean-13`) se estira a la caja (como `Fit: stretch`): las
  barras ocupan todo el alto, menos el de su [texto legible](#texto-legible) si lo lleva, y todos los módulos tienen el
  mismo ancho.

Un `Barcode` no admite `Fit`.

> **Decisión:** el tamaño natural sale de un módulo fijo: 0,5 mm en un QR y un Data Matrix, y el tamaño nominal del
> EAN-13 (0,33 × 22,85 mm) en los códigos de barras; en un PDF417, ese ancho y filas de tres módulos de alto, el mínimo
> de su norma. Así un `Barcode` sin tamaño se ve y se lee, y se mide en un `Stack` o un `Grid` como una imagen.
> *Alternativa descartada:* exigir `Width` o `Height`, que deja sin tamaño intrínseco al que lo declara en porcentaje.

> **Decisión:** un QR en una caja que no es cuadrada se dibuja centrado, sin error mientras sus módulos no bajen del
> [mínimo](#módulo-mínimo). *Alternativa descartada:* un error si `Width` y `Height` no son iguales, que con porcentajes no
> se puede saber antes de maquetar.

### Módulo mínimo

Un código encogido hasta que no se puede leer es un error. Cada formato tiene un módulo mínimo:

| `Barcode format` | Módulo mínimo | De dónde sale |
|---|---|---|
| `qr` | 0,25 mm | Unos tres puntos de una impresora de 300 ppp. ISO/IEC 18004 no fija ninguno |
| `data-matrix`, `gs1-data-matrix`, `pdf417` | 0,25 mm | El mismo criterio |
| `code-128`, `gs1-128` | 0,25 mm | El mismo criterio. ISO/IEC 15417 no fija ninguno |
| `ean-13` | 0,264 mm | El 80 % del módulo nominal (0,33 mm), el menor que admite ISO/IEC 15420 |

Si la caja de contenido, una vez maquetada, deja los módulos más estrechos, es un error (`BARCODE_TOO_SMALL`, con la línea
del `Barcode`) que da el módulo que queda y el tamaño mínimo de la caja de contenido. En un QR, un Data Matrix y un PDF417 cuenta el lado más corto (en
una caja ancha y baja, el alto); en un código de barras de una fila, solo el ancho, porque estirar las barras a lo alto
no cambia el módulo. La zona de silencio forma parte del dibujo: un QR de 25 módulos más 4 de zona a cada lado (33) necesita una caja de
contenido de 8,25 × 8,25 mm (23,39 pt), y el `Padding` no cuenta. Se comprueba con el tamaño final, después de paginar: en
un `Stack` o un `Grid` un código se mide varias veces, y solo cuenta la última.

Quien convierte puede quitar la comprobación (`Check.BARCODE_SIZE`, [Uso desde Java](../implementation/api.md#comprobaciones)),
y entonces el código se dibuja tan pequeño como su caja.

> **Decisión:** el mínimo es fijo por formato y no hay propiedad en el documento para cambiarlo; quien necesita dibujar un
> código más pequeño quita la comprobación al convertir. *Alternativa descartada:* una propiedad del `Barcode`
> (`Min module`), otro valor que escribir en cada documento para un caso raro; y dibujar el código encogido sin aviso, como
> hasta la 0.9.0, que da un resultado a medias.

> **Decisión:** 0,25 mm en todos los formatos menos el EAN-13, el módulo más pequeño que una impresora de oficina de 300 ppp dibuja con tres
> puntos. *Alternativa descartada:* los mínimos de GS1 (0,396 mm en un QR de punto de venta y 0,495 mm en un GS1-128 de
> logística), pensados para los lectores de caja y de almacén, que dejarían fuera los códigos que llevan las facturas y los
> certificados. Quien imprime etiquetas para esos lectores da a la caja el tamaño que piden.

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

- Otros formatos: Aztec, Code 39, EAN-8, UPC-A, ni los datos GS1 en un QR.
- Un Data Matrix rectangular, y elegir el tamaño del símbolo.
- Elegir las columnas, las filas o el nivel de corrección de un PDF417, ni sus variantes compacta y Macro.
- Comprobar el formato del valor de cada identificador GS1 de longitud variable, y un paréntesis dentro de un valor.
- El texto legible en otro sitio que bajo las barras, con otra alineación o en un código de matriz: va en un `Text`.
- El signo `>` que algunos EAN-13 llevan a la derecha para guardar la zona de silencio.
- Elegir la versión o la máscara del QR.
- Una zona de silencio distinta a cada lado: `Quiet zone` es la misma en los dos.
- Un alto mínimo de las barras: un código de barras bajo se dibuja sin aviso.
- Cambiar el módulo mínimo desde el documento.
- Un logotipo en el centro de un QR.
- Comprobar el contraste de los módulos con el fondo.
