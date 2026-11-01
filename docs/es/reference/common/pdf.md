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
accesibilidad WCAG (nivel AA): al menos **4,5:1**, o **3:1** en el texto grande (`Size` de 18 o más, o de 14 en negrita,
que es el peso 700 o más).
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

## PDF/A

PDF/A es la norma del PDF para archivo (ISO 19005), la que piden los archivos, las administraciones y la facturación
electrónica. Quien convierte la pide con `Converter.builder().pdfA(PdfA.A3B)`
([Uso desde Java](../implementation/api.md#pdfa-y-adjuntos)); por defecto el PDF no la declara. Hay cuatro opciones,
las partes 2 y 3 en sus niveles `b` y `a` (`A2B`, `A2A`, `A3B`, `A3A`):

- **PDF/A-2 y PDF/A-3** son PDF 1.7, como PDF/UA-1, así que el PDF cumple las dos normas a la vez. PDF/A-3 es PDF/A-2
  más los [ficheros adjuntos](#ficheros-adjuntos-factur-x).
- **El nivel `b`** promete que el PDF se verá igual dentro de años: fuentes incrustadas, colores definidos, sin cifrado
  ni código. **El nivel `a`** añade el etiquetado y el texto legible por máquina, que todo PDF del motor ya lleva: los
  dos niveles dan el mismo PDF con otra declaración.

PDF/A añade al PDF un perfil de color sRGB de 3 KB y los metadatos que la norma pide
([El PDF por dentro](../implementation/pdf.md#pdfa)). Lo demás ya lo cumple todo PDF del motor: las fuentes van
incrustadas, los colores son definidos, y no hay JavaScript, cifrado ni formularios. Los tests validan los once
ejemplos con veraPDF en las cuatro variantes.

PDF/UA-2 escribe un PDF 2.0, y PDF/A-2 y PDF/A-3 son PDF 1.7: pedir los dos es un `IllegalArgumentException`. PDF/A-4,
sobre PDF 2.0, no está hecho.

> **Decisión:** PDF/A se pide al convertir y no en el documento, como PDF/UA-2 y el contraste: es una decisión de quien
> publica. *Alternativa descartada:* una propiedad de `Document`.

> **Decisión:** solo las partes 2 y 3. PDF/A-1 no admite transparencia, que el motor usa en las opacidades y los
> degradados, y PDF/A-4 es PDF 2.0, que aún no abre bien cualquier visor. *Alternativa descartada:* PDF/A-1b, que
> obligaría a rechazar los documentos con opacidad.

### Ficheros adjuntos (Factur-X)

Con PDF/A-3, el PDF lleva ficheros dentro. Es cómo funciona la factura electrónica Factur-X / ZUGFeRD: un PDF/A-3 con
el XML de la factura adjunto. Los adjuntos se pasan en cada llamada, porque son de cada documento y no de la
configuración:

```java
Attachment xml = Attachment.of("factur-x.xml", bytes, "text/xml", Attachment.Relationship.ALTERNATIVE)
        .withDescription("Factur-X invoice");
converter.toPdf(document, out, Attachments.of(xml).withXmp(facturXmp));
```

Cada adjunto tiene un nombre (único en el PDF; el visor lo muestra), su contenido, su tipo (`text/xml`,
`application/pdf`…) y su relación con el documento: `DATA` (los datos que el documento muestra), `ALTERNATIVE` (otra
representación del documento, como el XML de una factura), `SOURCE` (de lo que se hizo) o `SUPPLEMENT` (lo que lo
completa). Puede llevar una descripción y una fecha de modificación; sin fecha, el PDF sigue sin llevar ninguna y el
mismo documento da los mismos bytes. `withXmp` añade los metadatos XMP que acompañan a los adjuntos: Factur-X pide
unos (el nombre del fichero, el tipo de documento, la versión y el perfil) y su especificación da el fragmento, que se
pasa tal cual. El motor no sabe de facturas: no genera el XML ni sus metadatos, solo los incrusta.

Un adjunto en un PDF que no es PDF/A-3 es un error, `ATTACHMENT_REQUIRES_PDFA3` ([Errores](errors.md#códigos)):
PDF/A-2 solo admite adjuntar otros PDF/A.

> **Decisión:** los adjuntos van en la llamada a `toPdf` y no en el `Converter`: el `Converter` se crea una vez y se
> comparte, y el XML de una factura es de esa factura. *Alternativa descartada:* `attach(...)` en el `builder`, que
> obliga a un `Converter` por documento.

> **Decisión:** un adjunto sin PDF/A-3 es un error y no un PDF con el fichero dentro: en PDF/A-2 está prohibido, y
> fuera de PDF/A sería un caso más que no pide nadie. *Alternativa descartada:* admitirlo en cualquier PDF, que PDF 1.7
> permite.

> **Decisión:** el motor incrusta el XML que le dan y escribe los metadatos que le dan: Factur-X tiene perfiles y
> versiones, y las facturas son de quien convierte. *Alternativa descartada:* `facturX(xml, perfil)`, que ataría el
> motor a una versión de la especificación.

## Lo que no hay

- PDF/A-1 y PDF/A-4 ([arriba](#pdfa)).
- Idioma por tramo dentro de una línea ([Tipografía](typography.md#idioma-language)).
- Formularios, firmas digitales y cifrado. Adjuntos fuera de PDF/A-3.
- Capas opcionales propias: solo se conservan las de la página de un PDF usada como imagen
  ([Image](../components/image.md#pdf)).
