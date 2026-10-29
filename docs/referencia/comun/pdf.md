# El PDF

Lo que lleva todo PDF que escribe el motor. Lo propio de cada elemento (cómo se etiqueta una tabla, una lista o una imagen)
está en su página.

## Páginas

Cada página tiene el tamaño del `Media` de su sección ([Document](../componentes/document.md#media)). Las etiquetas de
página (`PageLabels`) llevan el número impreso, o ninguno en las páginas sin número, y solo se escriben si alguna página
no es su número físico.

## Fuentes

Las fuentes son las TTF de la carpeta de recursos, incrustadas como subconjunto con los glifos que se usan
([Recursos](recursos.md#fuentes)). El texto es texto del PDF: se puede seleccionar, copiar y buscar.

## PDF etiquetado

El PDF va **etiquetado** (*tagged PDF*, para accesibilidad):

- **Árbol de estructura:** un elemento por caja, en orden del documento, que es el orden de lectura: `Document` (la raíz),
  `Sect` (cada sección, con `Lang` si cambia su idioma), `H1` … `H6` (los encabezados), `P` (`Paragraph` y `Text`; el `Text`
  de un encabezado es un `Span` de su texto), `L` y `LI` ([List](../componentes/list.md#salida-pdf)), `Table` y sus partes
  ([Table](../componentes/table.md#salida-pdf)), `TOC` y `TOCI` ([Contents](../componentes/contents.md#pdf-etiquetado)),
  `Figure` ([Image](../componentes/image.md#accesibilidad)), `Link` ([Enlaces](enlaces.md#pdf-etiquetado)) y `Div` (el
  resto). El texto de cada caja es contenido marcado de su elemento; una caja partida entre páginas tiene contenido en
  varias.
- ***Artifacts*:** los fondos, los bordes, las rayas del texto, las guías de un índice, las imágenes sin `Alt`, las filas
  repetidas de una tabla desde su segunda página y todo lo que hay dentro de un `Fixed` no forman parte del contenido.
- **Idioma:** el `Language` de `Document` es el idioma de todo el contenido; una caja que declara otro lleva su `Lang`
  ([Tipografía](tipografia.md#idioma-language)).
- **Texto que se lee:** un elemento con `Transform` o `Small caps` lleva `ActualText` con el texto escrito
  ([Formato del texto](formato-del-texto.md#mayúsculas-transform)).
- **Espacio al final de la línea.** Una línea de texto normal que no es la última de su caja (o de su fragmento) acaba, en
  el contenido del PDF, con un espacio invisible tras la última palabra: el corte de línea se come el espacio, y sin él
  quien une el texto de las líneas (un lector de pantalla, la extracción, la búsqueda) pegaría las palabras. No cambia el
  layout. El texto de `White space: pre` y `pre-wrap` no lo lleva: sus líneas son líneas.

> **Decisión:** un `Text` es un `P` en el PDF etiquetado, no un `Span`: un `Span` es texto en línea y un lector de pantalla
> no distingue dónde acaba. Solo es un `Span` el `Text` hijo de un encabezado (parte del texto del `H1`…`H6`) y las dos
> piezas de una entrada de índice. Un `Text` con varios párrafos es un solo `P`. *Alternativa descartada:* que lo decida
> una propiedad del `Text`.

> **Decisión:** cada línea partida acaba con un espacio invisible, que no cambia el ancho de la línea ni el layout, para
> que el texto extraído no pegue la última palabra de una línea con la primera de la siguiente. *Alternativa descartada:*
> dejarlo a la geometría, como hacen algunos extractores.

## Marcadores

Uno por encabezado, con el texto de sus `Text` unido por espacios (con su número, si lo lleva), anidados por nivel y que
llevan a la esquina superior izquierda de su primer fragmento. El visor los muestra al abrir el documento.

## Título, idioma y PDF/UA-1

`Title` va a la información del documento y a los metadatos XMP, y el visor lo muestra en lugar del nombre del fichero;
`Language` es el idioma de todo el contenido ([Document](../componentes/document.md#title-y-language)). Los dos son
obligatorios, así que todo PDF declara que es PDF/UA-1 (`pdfuaid:part`) en sus metadatos XMP.

> **Decisión:** `Title` y `Language` son obligatorios en `Document`. El idioma es lo que un lector de pantalla necesita
> para leer el contenido y PDF/UA lo exige; el título lo muestra el visor y PDF/UA-1 (7.1) lo exige. *Alternativa
> descartada:* dejarlos opcionales y no declarar PDF/UA sin ellos.

> **Decisión:** todo PDF declara PDF/UA-1. Es una afirmación que veraPDF solo comprueba en parte (no el orden de lectura ni
> el sentido de los textos), así que se apoya además en el repaso a mano del etiquetado: un documento puede cumplir
> PDF/UA-1 según veraPDF y ser mejorable para un lector de pantalla. *Alternativa descartada:* no declararlo.

> **Decisión:** el PDF escribe un `Sect` por sección y las `PageLabels` solo si alguna página no es su número físico; un
> documento de una sola sección que numera como siempre no cambia.

## Lo que no hay

- PDF/UA-2 y PDF 2.0 (`TOCI` con `Reference`, destinos de estructura, `ListNumbering` para las listas con guion).
- Idioma por tramo dentro de una línea, y comprobación del contraste entre el texto y su fondo.
- Formularios, adjuntos, firmas digitales, capas opcionales y cifrado.
