# El PDF

Lo que lleva todo PDF que escribe el motor. Cómo queda por dentro (el árbol de estructura, las anotaciones, los metadatos)
está en [El PDF por dentro](../implementacion/pdf.md).

## Páginas

Cada página tiene el tamaño del `Media` de su sección ([Document](../componentes/document.md#media)). El visor muestra el
número impreso de cada página (el de `{page}`), no su posición en el fichero, y ninguno en las páginas sin número.

## Fuentes

Las fuentes son las de la carpeta de recursos ([Recursos](recursos.md#fuentes)), incrustadas con los caracteres que se
usan: el PDF se ve igual en cualquier sitio. El texto es texto: se puede seleccionar, copiar y buscar.

## Accesibilidad

El PDF va **etiquetado**: además de lo que se ve, lleva la estructura del documento, que es lo que usa un lector de
pantalla y lo que da el texto al copiarlo o al cambiar el tamaño de la vista.

- **Orden de lectura:** el del documento. Cada caja es un elemento: los encabezados se anuncian como encabezados de su
  nivel, las listas como listas con su número de ítems, las tablas como tablas con sus cabeceras, las imágenes con `Alt`
  como figuras y un `Contents` como un índice.
- **Decoración:** los fondos, los bordes, las rayas del texto, las guías de un índice, las líneas entre columnas, las
  imágenes sin `Alt`, las filas repetidas de una tabla y todo lo que hay en un `Fixed` no se leen.
- **Idioma:** el `Language` de `Document` es el de todo el contenido; una caja que declara otro se lee con ese
  ([Tipografía](tipografia.md#idioma-language)).
- **Texto que se lee:** el escrito, también con `Transform` y `Small caps`
  ([Formato del texto](formato-del-texto.md#mayúsculas-transform)), y las palabras de dos líneas seguidas no se pegan al
  copiar el texto.
- **Enlaces:** se anuncian con lo que enlazan ([Enlaces](enlaces.md#accesibilidad)).

`Title` y `Language` son obligatorios en `Document` ([Document](../componentes/document.md#title-y-language)), así que todo
PDF declara cumplir **PDF/UA-1**, la norma de PDF accesible. Los tests lo comprueban con veraPDF.

> **Decisión:** `Title` y `Language` son obligatorios: el idioma es lo que un lector de pantalla necesita para leer el
> contenido, y PDF/UA exige los dos. *Alternativa descartada:* dejarlos opcionales y no declarar PDF/UA sin ellos.

## Marcadores

Uno por encabezado, con su texto (y su número, si lo lleva), anidados por nivel y que llevan al encabezado. El visor los
muestra al abrir el documento.

## Título

`Title` es lo que el visor muestra en lugar del nombre del fichero.

## Lo que no hay

- PDF/UA-2 y PDF 2.0.
- Idioma por tramo dentro de una línea, y comprobación del contraste entre el texto y su fondo.
- Formularios, adjuntos, firmas digitales, capas opcionales y cifrado.
