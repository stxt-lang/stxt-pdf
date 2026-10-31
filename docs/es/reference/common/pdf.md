# El PDF

Lo que lleva todo PDF que escribe el motor. Cómo queda por dentro (el árbol de estructura, las anotaciones, los metadatos)
está en [El PDF por dentro](../implementation/pdf.md).

## Páginas

Cada página tiene el tamaño del `Page` de su sección ([Document](../components/document.md#page)). El visor muestra el
número impreso de cada página (el de `{page}`, también en romanos) en lugar de su posición en el fichero, y ninguno en las
páginas sin número. Una página en blanco de `Begin on: odd` muestra el número que le toca, aunque no lo imprima.

## Fuentes

Las fuentes son las de la carpeta de recursos ([Recursos](resources.md#fuentes)), incrustadas con los caracteres que se
usan: el PDF se ve igual en cualquier sitio. El texto es texto: se puede seleccionar, copiar y buscar.

## Accesibilidad

El PDF va **etiquetado**: además de lo que se ve, lleva la estructura del documento, que es lo que usa un lector de
pantalla y lo que da el texto al copiarlo o al cambiar el tamaño de la vista.

- **Orden de lectura:** el del documento. Cada caja es un elemento: los encabezados se anuncian como encabezados de su
  nivel, las listas como listas con su número de ítems, las tablas como tablas con sus cabeceras, las imágenes con `Alt`
  como figuras y un `Table of contents` como un índice.
- **Decoración:** los fondos, los bordes, las rayas del texto, las guías de un índice, las líneas entre columnas, las
  imágenes sin `Alt`, las filas repetidas de una tabla y todo lo que hay en un `Page overlay` no se leen.
- **Idioma:** el `Language` de `Document` es el de todo el contenido. Una caja que declara otro se lee con ese
  ([Tipografía](typography.md#idioma-language)).
- **Texto que se lee:** el escrito, también con `Case` y `Small caps`
  ([Formato del texto](text-formatting.md#mayúsculas-case)), y las palabras de dos líneas seguidas no se pegan al
  copiar el texto.
- **Enlaces:** se anuncian con lo que enlazan ([Enlaces](links.md#accesibilidad)).
- **Contraste:** el texto se distingue de lo que tiene debajo ([abajo](#contraste)).

`Title` y `Language` son obligatorios en `Document` ([Document](../components/document.md#title-y-language)), así que todo
PDF declara cumplir **PDF/UA-1**, la norma de PDF accesible. Quien convierte puede pedir **PDF/UA-2**, su versión nueva, que
escribe un PDF 2.0 ([Uso desde Java](../implementation/api.md#convertir)). Qué cambia por dentro está en
[El PDF por dentro](../implementation/pdf.md#pdfua-2). Los tests comprueban los dos con veraPDF.

> **Decisión:** `Title` y `Language` son obligatorios: el idioma es lo que un lector de pantalla necesita para leer el
> contenido, y PDF/UA exige los dos. *Alternativa descartada:* dejarlos opcionales y no declarar PDF/UA sin ellos.

## Contraste

Al convertir, el motor comprueba que cada texto se distingue de lo que tiene debajo, con el contraste de las pautas de
accesibilidad WCAG (nivel AA): al menos **4,5:1**, o **3:1** en el texto grande (`Size` de 18 o más, o de 14 en negrita).
Si no llega, es un error (`CONTRAST_LOW`) con la línea del `Color` del texto, también si lo hereda (sin `Color`, el negro
de siempre, la línea de la caja).

- **Lo que hay debajo** es lo que se ha pintado antes que el texto en el punto donde empieza: los fondos (`Background`) de
  su caja, de las de fuera y de las que tiene detrás, mezclados con su opacidad sobre el blanco de la página. Las cajas que
  no están debajo de ese punto no cuentan, ni los bordes.
- **Un degradado:** cuenta cada uno de sus colores, y vale el peor.
- **Un texto con opacidad** (`Color: #00000080`) se mezcla con lo que tiene debajo antes de comparar.
- **Encima de una imagen** no se sabe qué hay debajo, así que ese texto no se comprueba, salvo que un fondo opaco tape la
  imagen.
- Se comprueba todo el texto que se pinta, también el de un `Page overlay` y el de las entradas de un índice.

Quien convierte puede desactivar la comprobación ([Uso desde Java](../implementation/api.md#convertir)). Por defecto
está activa.

> **Decisión:** el contraste se mira en lo que se pinta y no en el árbol de cajas: un texto sobre una caja que no es la suya
> (un `Overlay` encima de un fondo) tiene debajo ese fondo. *Alternativa descartada:* mirar solo los fondos de la caja y
> de las de fuera, que daba por blanco lo que hay debajo de un `Overlay` sobre una foto.

> **Decisión:** el texto sobre una imagen no se comprueba. *Alternativa descartada:* tratarlo como un error, que impide la
> portada con el título sobre una foto aunque se lea bien.

> **Decisión:** se mira el punto donde empieza cada tramo de texto y no toda su superficie. *Alternativa descartada:* el
> peor color de todo lo que el tramo toca, que obliga a medir el texto y cuenta como fondo el subrayado del propio texto.

> **Decisión:** la comprobación está activa por defecto y se desactiva al convertir y no en el documento. *Alternativa
> descartada:* una propiedad del documento, que lo dejaría a quien escribe y no a quien publica el PDF.

## Marcadores

Uno por encabezado, con su texto (y su número, si lo lleva), anidados por nivel y que llevan al encabezado. El visor los
muestra al abrir el documento.

## Título

`Title` es lo que el visor muestra en lugar del nombre del fichero.

## Lo que no hay

- PDF/A.
- Idioma por tramo dentro de una línea ([Tipografía](typography.md#idioma-language)).
- Formularios, adjuntos, firmas digitales y cifrado.
- Capas opcionales propias: solo se conservan las de la página de un PDF usada como imagen
  ([Image](../components/image.md#pdf)).
