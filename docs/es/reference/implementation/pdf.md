# El PDF por dentro

Qué escribe el motor en el PDF y por qué: el árbol de estructura del PDF etiquetado, los *artifacts*, las anotaciones de
enlace, los marcadores y los metadatos. Lo que nota quien lee el documento está en [El PDF](../common/pdf.md). Cómo lo
escribe el código está en [El pintado](painting.md#el-pdf).

## Páginas, fuentes y metadatos

- Cada página tiene el tamaño del `Page` de su sección. Las etiquetas de página (`PageLabels`) llevan el número impreso
  con su estilo (`D`, `r` o `R`), o ninguno en las páginas sin número, y solo se escriben si alguna página no es su
  número físico en cifras: un documento que numera como siempre no las lleva.
- Las fuentes son las de la carpeta de recursos, en Type 0 con Identity-H. Las de contornos TrueType (los `.ttf` y los
  `.otf` que los llevan) se incrustan como subconjunto, con los glifos que se usan, en un `CIDFontType2` con el fichero en
  `FontFile2`. Las de contornos CFF (los `.otf` con tabla `CFF `) se incrustan enteras, en un `CIDFontType0` con el
  fichero en `FontFile3` de subtipo `OpenType`: PDFBox no sabe hacer subconjuntos de CFF, y las declara como TrueType,
  así que `PdfFonts` cambia el subtipo y la clave del fichero al cargarlas. Los códigos del texto son los números de glifo
  en las dos: en un CFF que no es CID, como el de un `.otf`, un CID es el número de glifo.
- `Title` va a la información del documento y a los metadatos XMP, con `DisplayDocTitle` para que el visor lo muestre,
  y `Language` es el `Lang` del catálogo. Todo PDF declara PDF/UA-1 (`pdfuaid:part`) en sus metadatos XMP, o PDF/UA-2 si
  quien convierte lo pide ([abajo](#pdfua-2)).
- El PDF no lleva fechas, y su identificador (`/ID` del tráiler) sale del contenido: el mismo documento, con los mismos
  recursos y opciones, da los mismos bytes. `PdfId` guarda el documento con un identificador provisional de ceros, calcula
  el SHA-256 de lo guardado y pone sus 16 primeros bytes en lugar de los ceros, en las dos mitades, sin mover nada más.

> **Decisión:** el identificador sale del contenido y no de la hora. Así una conversión se puede repetir y comparar byte a
> byte, y dos documentos distintos tienen identificadores distintos, que es lo que pide la norma. *Alternativa
> descartada:* el identificador que PDFBox calcula con la hora, que cambia en cada conversión.

> **Decisión:** las fuentes CFF se incrustan enteras y se declaran como lo que son. El SemiBold de IBM Plex Sans en
> `.otf` (143 KB) añade unos 90 KB al PDF que lo usa, frente a unos pocos KB de un subconjunto, y por eso las fuentes del
> repositorio son TTF. *Alternativa
> descartada:* hacer el subconjunto del CFF en el motor, que es reescribir la tabla `CFF ` entera, o dejarlas como las
> declara PDFBox, que no es lo que dice la norma (ISO 32000-1, 9.9) aunque los visores lo toleren.

> **Decisión:** todo PDF declara PDF/UA-1. veraPDF lo comprueba solo en parte (no el orden de lectura ni el sentido de los
> textos), así que se apoya además en el repaso a mano del etiquetado. *Alternativa descartada:* no declararlo.

## Árbol de estructura

Un elemento por caja, en el orden del documento, que es el orden de lectura:

| Caja | Elemento |
|---|---|
| El documento | `Document` (la raíz) |
| `Section` | `Sect`, con `Lang` si cambia su idioma |
| `Heading 1` a `Heading 6` | `H1` a `H6`, y sus `Text`, `Span` |
| `Text`, `Paragraph` | `P` |
| `List`, `List item` | `L` y `LI`, con el marcador en `Lbl` y el contenido en `LBody` |
| `Table` y sus partes | `Table`, `THead`, `TBody`, `TFoot`, `TR`, y `TH` o `TD` |
| `Table of contents` y sus entradas | `TOC` y `TOCI`, con dos `Span` (el título y el número) |
| `Image` con `Alt`, `Barcode` | `Figure`, con `Alt` y `BBox` |
| Una caja con `Link`, un enlace de texto | `Link` ([abajo](#enlaces)) |
| El resto (como `Block`, `Stack`, `Grid`, `Columns`, `Markdown` o los posicionados) | `Div` |

Lo que genera un `Markdown` cuelga de su `Div` con sus propios elementos. El contenido de un `Columns` cuelga de su `Div`
de columna en columna. El texto de cada caja es contenido marcado de su elemento, también sus tramos con formato, que no
tienen elemento propio. Una caja partida entre páginas tiene contenido marcado en varias.

> **Decisión:** un `Text` es un `P` y no un `Span`: un `Span` es texto en línea y un lector de pantalla no distingue dónde
> acaba. Solo son `Span` el `Text` de un encabezado y las dos piezas de una entrada de índice. *Alternativa descartada:*
> que lo decida una propiedad del `Text`.

> **Decisión:** el `Markdown` es un `Div`, como un `Block`, y lo generado cuelga de él. *Alternativa descartada:* que lo
> generado cuelgue de su padre.

### Idioma

El `Language` de `Document` es el `Lang` del catálogo. Una caja que declara otro lleva `Lang` en su elemento, solo donde el
idioma cambia respecto al de fuera, y los elementos de dentro lo heredan.

> **Decisión:** el idioma es de la caja y no de un tramo: un idioma por tramo partiría el contenido marcado.

### Texto que se lee

Un elemento con `Case` o `Small caps` lleva `ActualText` con el texto escrito, para que un lector de pantalla lea
«Emisor» y no «EMISOR», salvo si su texto contiene `{page}`, `{pages}` o una referencia, que cambian con la página.

> **Decisión:** sin `ActualText` en un texto con `{page}` o referencias. *Alternativa descartada:* dar el texto sustituido
> de cada fragmento, que no encaja en un elemento partido entre páginas.

Una línea de texto normal que no es la última de su caja (o de su fragmento) acaba con un espacio invisible tras la última
palabra: el corte de línea se come el espacio, y sin él quien une el texto de las líneas (un lector de pantalla, la
extracción, la búsqueda) pegaría las palabras. No cambia el ancho de la línea ni el layout. El texto de `Preformatted: true`
y `wrap` no lo lleva: sus líneas son líneas.

> **Decisión:** el espacio invisible al final de cada línea partida. *Alternativa descartada:* dejarlo a la geometría,
> como hacen algunos extractores.

Una línea partida dentro de una palabra (`Hyphenate` o un guion blando,
[Tipografía](../common/typography.md#guiones-hyphenate)) acaba con el guion pintado y **sin** el espacio invisible, y la
secuencia de tramos que la termina va en un `Span` con `ActualText` donde el guion es el guion blando U+00AD: es como un
PDF etiquetado dice que el guion lo puso el reparto, y así quien extrae el texto no ve el guion y puede unir las dos
partes. PDFBox lo descarta al extraer y da cada parte en su línea; Acrobat copia el guion blando, que no se ve.

> **Decisión:** `ActualText` con el guion blando en la secuencia que acaba la línea, y no un `ActualText` vacío ni la
> palabra entera en los dos trozos: el primero perdería la señal del corte y el segundo la duplicaría al extraer.

Con `Letter spacing`, los caracteres de un texto se pintan separados, y quien extrae el texto mirando dónde está cada
uno adivina espacios entre ellos (con PDFBox, `I n f o r m e` desde 1,5 pt en un cuerpo de 10). Por eso cada secuencia
de tramos seguidos de una línea con el mismo enlace va en un contenido marcado `Span` con `ActualText`: su texto, con el
espacio invisible del final si llega al final de la línea. Lo respetan Acrobat, poppler (`pdftotext`) y PDFBox, que deja
espacios tras él. No está en la estructura: la de un elemento con `Case` sigue siendo la del texto escrito.

> **Decisión:** un `ActualText` por secuencia de tramos y línea. *Alternativa descartada:* uno por tramo, que deja entre
> dos tramos el hueco del interletraje y hace adivinar un espacio de más, o `Tc` en vez de `TJ`, que separa los
> caracteres igual y los extractores tratan del mismo modo.

### Listas

`L` lleva el atributo `ListNumbering` según el `Marker`: `Disc`, `Decimal`, `LowerAlpha`, `UpperAlpha`, `LowerRoman`,
`UpperRoman`, y `None` para `dash` y `none`.

> **Decisión:** `dash` es `ListNumbering: None`, porque PDF/UA-1 no tiene un valor para el guion. *Alternativa
> descartada:* `Unordered`, de PDF 2.0, que sí se usa con PDF/UA-2 ([abajo](#pdfua-2)).

### Tablas

Una celda de `Header rows` es `TH` con `Scope: Column`, una de `Body` o `Footer rows` con `Row header: true` es `TH` con
`Scope: Row`, y el resto, `TD`. Una celda que ocupa varias filas o columnas lleva `RowSpan` y `ColSpan`. La cabecera y
el pie son contenido en la primera página en que sale cada celda. Donde se repiten, son *artifacts*, así que el árbol
tiene una sola `THead` y una sola `TFoot`.

> **Decisión:** las repeticiones son *artifacts*, y un lector de pantalla no lee la cabecera en cada página.
> *Alternativa descartada:* marcar cada repetición como contenido de la misma celda.

### Figuras

El `BBox` de la `Figure` de una imagen es su caja de contenido en la página y no la parte visible con `Fit: inside`. El de
una imagen girada es el rectángulo que la envuelve.

### Índices

Cada entrada es un `TOCI` con el título y el número en dos `Span`, y la guía es un *artifact*. El `Link` de la entrada
envuelve los dos `Span`.

## *Artifacts*

No forman parte del contenido: los fondos, los bordes, los degradados, las rayas del texto (subrayado y tachado), las guías
de un índice, las líneas entre columnas, las imágenes sin `Alt`, las filas repetidas de una tabla desde su segunda página
y todo lo que hay dentro de un `Page overlay`.

## Enlaces

Cada zona es una anotación `Link` sin borde, con la acción `URI` o el destino de la página, y como texto (`Contents`) lo que
dice la caja o el tramo: el texto de la caja y de lo que contiene unido por espacios, o el `Alt` de una imagen. Sin
texto, lleva el propio destino. Las referencias y los marcadores de página del texto salen sin sustituir. Los destinos internos
se resuelven después de pintar todas las páginas, así que un enlace puede ir hacia delante. Las páginas con anotaciones
llevan `Tabs: S`, para que el visor recorra los enlaces en el orden de la estructura.

En una caja girada, la anotación tiene como `Rect` el rectángulo que envuelve la zona girada y como `QuadPoints` la zona
girada. El destino de una caja girada es el rectángulo que la envuelve.

El elemento `Link` envuelve lo que se pulsa y lleva cada anotación como referencia a objeto (`OBJR`), como pide PDF/UA:

- *Caja con `Link`:* el `Link` es hijo del elemento de la caja y contiene su contenido (`P → Link → texto`,
  `Div → Link → P, P` o `TOCI → Link → título, número`). Una imagen es la excepción: su `Link` es el padre de su `Figure`
  (`Link → Figure`), porque una `Figure` no puede contener a su `Link`. Una caja partida tiene un solo `Link` con una `OBJR`
  por fragmento.
- *Enlace dentro del texto:* el `Link` es hijo del elemento del texto (`P → texto, Link, texto`) y contiene el contenido
  marcado de las palabras del enlace, que se pintan en secuencias aparte. Es uno por enlace y fragmento, aunque ocupe
  varias líneas: un contenido marcado y una `OBJR` por línea. Dos enlaces seguidos con el mismo destino son uno.
- *`Alt`:* un `Link` que contiene algo que leer (texto o una figura con su `Alt`) no lleva `Alt`, porque el lector lo leería
  en lugar del contenido. Uno sin nada dentro lo lleva con el texto del enlace.
- *Lo que no es contenido* (un `Page overlay`, una imagen decorativa) no tiene a qué envolver, pero PDF/UA exige que toda
  anotación de enlace esté en un `Link`. Cada enlace suyo (el de una caja y uno por destino de sus enlaces de texto) es un
  `Link` sin contenido en el sitio de la caja en el árbol, con el texto del enlace como `Alt` y una `OBJR` por página en
  que sale: un pie con una web se anuncia una vez, donde está el `Page overlay` en el documento. Si el `Page overlay` no sale en
  ninguna página, no hay `Link`.
- *Las filas repetidas de una tabla:* sus anotaciones van al `Link` de la primera página, el que envuelve el texto de la
  celda.

> **Decisión:** el `Link` envuelve lo que se pulsa. *Alternativa descartada:* un `Link` con solo la referencia a la
> anotación, que no cumple PDF/UA.

> **Decisión:** un enlace de varias líneas es un solo `Link` (las anotaciones siguen siendo una por línea, que es lo que el
> visor necesita): un lector de pantalla anuncia un enlace y no uno por línea.

> **Decisión:** el enlace de lo que no es contenido es un `Link` sin contenido en el sitio de la caja, con las anotaciones
> de todas las páginas. *Alternativas descartadas:* un `Link` suelto del `Document` por página, que un lector de pantalla
> anuncia al final de la estructura y tantas veces como páginas, o marcar esas anotaciones como ocultas, que las quita del
> ratón.

## Marcadores

Uno por encabezado (también los de un `Markdown`), con el texto de sus `Text` unido por espacios (con su número, si lo
lleva), anidados por nivel y con destino en la esquina superior izquierda de su primer fragmento. El catálogo pide al visor
que los muestre al abrir el documento (`PageMode: UseOutlines`).

## PDF/UA-2

Con `Converter.builder().pdfUa(2)`, el PDF es PDF 2.0 (en la cabecera del fichero y en el `Version` del catálogo) y cumple
PDF/UA-2 (ISO 14289-2), con `pdfuaid:part` 2 y `pdfuaid:rev` 2024 en los metadatos XMP. Todo lo de arriba vale igual, con
estas diferencias:

- **Espacio de nombres:** el árbol de estructura declara el de PDF 2.0 (`Namespaces`, `http://iso.org/pdf2/ssn`) y cada
  elemento lo lleva en `NS`, salvo `TOC`, `TOCI` y `Reference`, que no existen en PDF 2.0 y se quedan en el de PDF 1.7, el
  de omisión.
- **Índices:** un `TOCI` no puede contener un `Link`: es `TOCI → Reference → Link → título, número`, y el `TOCI` señala con
  `Ref` el elemento del encabezado al que lleva.
- **Destinos de estructura:** un enlace a una caja del documento y un marcador son una acción `GoTo` con el elemento de la
  caja en `SD` (o el de su antecesor más cercano que tenga elemento) y la misma posición. `D`, con la página, queda para los
  visores que no entienden `SD`.
- **Listas:** `dash` es `ListNumbering: Unordered`.
- **Enlaces sin nada que leer** (los que llevan `Alt`): su anotación no lleva `Contents`, porque lo que se lee es el `Alt`.

veraPDF lo valida con su perfil de PDF/UA-2, que añade las reglas de ISO/TS 32005 sobre qué elemento puede contener a
cuál.

> **Decisión:** PDF/UA-2 es una opción y PDF/UA-1 lo de siempre: PDF 2.0 aún no lo abre bien cualquier visor.
> *Alternativa descartada:* pasar todo a PDF/UA-2.

> **Decisión:** los enlaces con `Alt` no llevan `Contents` en PDF/UA-2. La norma pide que, si están los dos, digan lo
> mismo, y lo dicen, pero veraPDF compara el `Alt` sin descodificar cuando el texto tiene acentos y lo da por distinto.
> *Alternativa descartada:* quitar el `Alt`, que es lo que lee un lector de pantalla en un `Link` vacío.

## Imágenes

El PDF incrusta cada fichero una sola vez, aunque se use en varias páginas, con la resolución del fichero: el JPEG tal
cual y el PNG sin pérdida y con su transparencia. Un SVG es un formulario (*XObject*) de su tamaño natural, dibujado una vez
por fichero, que se escala a su caja con `Fit` como un PNG ([El pintado](painting.md#imágenes)). La página de un PDF también
es un formulario, copiado una vez por página: su contenido y sus recursos, sin sus anotaciones ni su etiquetado.
