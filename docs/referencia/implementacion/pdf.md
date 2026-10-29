# El PDF por dentro

Qué escribe el motor en el PDF y por qué: el árbol de estructura del PDF etiquetado, los *artifacts*, las anotaciones de
enlace, los marcadores y los metadatos. Lo que nota quien lee el documento está en [El PDF](../comun/pdf.md); cómo lo
escribe el código, en [El pintado](pintado.md#el-pdf).

## Páginas, fuentes y metadatos

- Cada página tiene el tamaño del `Media` de su sección. Las etiquetas de página (`PageLabels`) llevan el número impreso,
  o ninguno en las páginas sin número, y solo se escriben si alguna página no es su número físico: un documento que
  numera como siempre no las lleva.
- Las fuentes son las TTF de la carpeta de recursos, incrustadas como subconjunto (Type 0, Identity-H) con los glifos que
  se usan.
- `Title` va a la información del documento y a los metadatos XMP, con `DisplayDocTitle` para que el visor lo muestre;
  `Language` es el `Lang` del catálogo. Todo PDF declara PDF/UA-1 (`pdfuaid:part`) en sus metadatos XMP.

> **Decisión:** todo PDF declara PDF/UA-1. veraPDF lo comprueba solo en parte (no el orden de lectura ni el sentido de los
> textos), así que se apoya además en el repaso a mano del etiquetado. *Alternativa descartada:* no declararlo.

## Árbol de estructura

Un elemento por caja, en el orden del documento, que es el orden de lectura:

| Caja | Elemento |
|---|---|
| El documento | `Document` (la raíz) |
| `Section` | `Sect`, con `Lang` si cambia su idioma |
| `Heading 1` … `Heading 6` | `H1` … `H6`; sus `Text` son `Span` |
| `Text`, `Paragraph` | `P`; un `Text` con varios párrafos es un solo `P` |
| `List`, `List item` | `L` y `LI`, con el marcador en `Lbl` y el contenido en `LBody` |
| `Table` y sus partes | `Table`, `THead`, `TBody`, `TFoot`, `TR`, y `TH` o `TD` |
| `Contents` y sus entradas | `TOC` y `TOCI`, con dos `Span` (el título y el número) |
| `Image` con `Alt` | `Figure`, con `Alt` y `BBox` |
| Una caja con `Link`, un enlace de texto | `Link` ([abajo](#enlaces)) |
| El resto (`Block`, `Flex`, `Grid`, `Multicol`, `Markdown`, los posicionados…) | `Div` |

Lo que genera un `Markdown` cuelga de su `Div` con sus propios elementos. El contenido de un `Multicol` cuelga de su `Div`
de columna en columna. El texto de cada caja es contenido marcado de su elemento, también sus tramos con formato, que no
tienen elemento propio; una caja partida entre páginas tiene contenido marcado en varias.

> **Decisión:** un `Text` es un `P`, no un `Span`: un `Span` es texto en línea y un lector de pantalla no distingue dónde
> acaba. Solo son `Span` el `Text` de un encabezado y las dos piezas de una entrada de índice. *Alternativa descartada:*
> que lo decida una propiedad del `Text`.

> **Decisión:** el `Markdown` es un `Div`, como un `Block`, y lo generado cuelga de él. *Alternativa descartada:* que lo
> generado cuelgue de su padre.

### Idioma

El `Language` de `Document` es el `Lang` del catálogo. Una caja que declara otro lleva `Lang` en su elemento, solo donde el
idioma cambia respecto al de fuera; los elementos de dentro lo heredan.

> **Decisión:** el idioma es de la caja, no de un tramo: un idioma por tramo partiría el contenido marcado.

### Texto que se lee

Un elemento con `Transform` o `Small caps` lleva `ActualText` con el texto escrito, para que un lector de pantalla lea
«Emisor» y no «EMISOR», salvo si su texto contiene `{page}`, `{pages}` o una referencia, que cambian con la página.

> **Decisión:** sin `ActualText` en un texto con `{page}` o referencias. *Alternativa descartada:* dar el texto sustituido
> de cada fragmento, que no encaja en un elemento partido entre páginas.

Una línea de texto normal que no es la última de su caja (o de su fragmento) acaba con un espacio invisible tras la última
palabra: el corte de línea se come el espacio, y sin él quien une el texto de las líneas (un lector de pantalla, la
extracción, la búsqueda) pegaría las palabras. No cambia el ancho de la línea ni el layout. El texto de `White space: pre`
y `pre-wrap` no lo lleva: sus líneas son líneas.

> **Decisión:** el espacio invisible al final de cada línea partida. *Alternativa descartada:* dejarlo a la geometría,
> como hacen algunos extractores.

### Listas

`L` lleva el atributo `ListNumbering` según el `Marker`: `Disc`, `Decimal`, `LowerAlpha`, `UpperAlpha`, `LowerRoman`,
`UpperRoman`, y `None` para `dash` y `none`.

> **Decisión:** `dash` es `ListNumbering: None`, porque PDF/UA-1 no tiene un valor para el guion. *Alternativa
> descartada:* `Unordered`, de PDF 2.0.

### Tablas

Una celda de `Head` es `TH` con `Scope: Column`; una de `Body` o `Foot` con `Header: true`, `TH` con `Scope: Row`; el resto,
`TD`. Una celda que ocupa varias filas o columnas lleva `RowSpan` y `ColSpan`. La cabecera y el pie son contenido en la
primera página en que sale cada celda; donde se repiten, son *artifacts*, así que el árbol tiene una sola `THead` y una
sola `TFoot`.

> **Decisión:** las repeticiones son *artifacts*, y un lector de pantalla no lee la cabecera en cada página.
> *Alternativa descartada:* marcar cada repetición como contenido de la misma celda.

### Figuras

El `BBox` de la `Figure` de una imagen es su caja de contenido en la página, no la parte visible con `contain`; el de una
imagen girada, el rectángulo que la envuelve.

### Índices

Cada entrada es un `TOCI` con el título y el número en dos `Span`; la guía es un *artifact*. El `Link` de la entrada
envuelve los dos `Span`.

## *Artifacts*

No forman parte del contenido: los fondos, los bordes, los degradados, las rayas del texto (subrayado y tachado), las guías
de un índice, las líneas entre columnas, las imágenes sin `Alt`, las filas repetidas de una tabla desde su segunda página
y todo lo que hay dentro de un `Fixed`.

## Enlaces

Cada zona es una anotación `Link` sin borde, con la acción `URI` o el destino de la página, y como texto (`Contents`) lo
que dice la caja o el tramo: el texto de la caja y de lo que contiene unido por espacios, o el `Alt` de una imagen; sin
texto, el propio destino. Las referencias y los marcadores de página del texto salen sin sustituir. Los destinos internos
se resuelven después de pintar todas las páginas, así que un enlace puede ir hacia delante. Las páginas con anotaciones
llevan `Tabs: S`, para que el visor recorra los enlaces en el orden de la estructura.

En una caja girada, la anotación tiene como `Rect` el rectángulo que envuelve la zona girada y como `QuadPoints` la zona
girada; el destino de una caja girada es el rectángulo que la envuelve.

El elemento `Link` envuelve lo que se pulsa y lleva cada anotación como referencia a objeto (`OBJR`), como pide PDF/UA:

- *Caja con `Link`:* el `Link` es hijo del elemento de la caja y contiene su contenido (`P → Link → texto`;
  `Div → Link → P, P`; `TOCI → Link → título, número`). Una imagen es la excepción: su `Link` es el padre de su `Figure`
  (`Link → Figure`), porque una `Figure` no puede contener a su `Link`. Una caja partida tiene un solo `Link` con una `OBJR`
  por fragmento.
- *Enlace dentro del texto:* el `Link` es hijo del elemento del texto (`P → texto, Link, texto`) y contiene el contenido
  marcado de las palabras del enlace, que se pintan en secuencias aparte. Es uno por enlace y fragmento, aunque ocupe
  varias líneas: un contenido marcado y una `OBJR` por línea. Dos enlaces seguidos con el mismo destino son uno.
- *`Alt`:* un `Link` que contiene algo que leer (texto o una figura con su `Alt`) no lleva `Alt`, porque el lector lo leería
  en lugar del contenido; uno sin nada dentro lo lleva con el texto del enlace.
- *Lo que no es contenido* (un `Fixed`, una imagen decorativa, las filas repetidas de una tabla) no tiene a qué envolver:
  su anotación va a un `Link` suelto, hijo del `Document`, sin contenido y con el texto del enlace como `Alt`, porque
  PDF/UA exige que toda anotación de enlace esté en un `Link`. Por eso un pie con una web se anuncia al final de la
  estructura, una vez por página.

> **Decisión:** el `Link` envuelve lo que se pulsa. *Alternativa descartada:* un `Link` con solo la referencia a la
> anotación, que no cumple PDF/UA.

> **Decisión:** un enlace de varias líneas es un solo `Link` (las anotaciones siguen siendo una por línea, que es lo que el
> visor necesita): un lector de pantalla anuncia un enlace, no uno por línea.

> **Decisión:** el `Link` suelto de lo que no es contenido. *Alternativa descartada:* marcar esas anotaciones como ocultas,
> que las quita del ratón.

## Marcadores

Uno por encabezado (también los de un `Markdown`), con el texto de sus `Text` unido por espacios (con su número, si lo
lleva), anidados por nivel y con destino en la esquina superior izquierda de su primer fragmento. El catálogo pide al visor
que los muestre al abrir el documento (`PageMode: UseOutlines`).

## Imágenes

El PDF incrusta cada fichero una sola vez, aunque se use en varias páginas, con la resolución del fichero: el JPEG tal
cual y el PNG sin pérdida y con su transparencia. Un SVG es un formulario (*XObject*) de su tamaño natural, dibujado una vez
por fichero, que se escala a su caja con `Fit` como un PNG ([El pintado](pintado.md#imágenes)).
