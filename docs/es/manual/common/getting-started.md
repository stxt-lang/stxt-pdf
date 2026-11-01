# Primeros pasos

## Un documento completo

```
Document (dev.stxt.pdf):
    Language: es
    Title: Informe del trimestre
    Page:
        Paper: A4
        Margin: 50
    Styles:
        For element: Document
            Typography:
                Font: IBM Plex Sans
                Size: 10
        For element: Heading 1
            Typography:
                Size: 18
                Bold: true
            Box:
                Margin: 0 0 12 0
        For element: Paragraph
            Box:
                Margin: 0 0 8 0
    Section:
        Heading 1:
            Text: Informe del trimestre
        Paragraph:
            Content >>
                Las ventas han crecido un 12 % respecto al trimestre anterior,
                sobre todo en la zona norte.
        Paragraph: Los costes se mantienen estables.
```

Línea a línea:

- **`Document (dev.stxt.pdf):`** es la raíz. El namespace `dev.stxt.pdf` dice que es un documento de stxt-pdf.
- **`Language`** es el idioma del documento y **`Title`**, su título. Los dos son obligatorios: el visor de PDF muestra el
  título, y un lector de pantalla necesita el idioma para leer el texto en voz alta.
- **`Page`** es el papel: un A4 con 50 puntos de margen por los cuatro lados. Un número solo son **puntos**: un punto
  es 1/72 de pulgada, unos 0,35 mm. Con `mm` pegado al número son milímetros: `Margin: 18mm` da casi el mismo margen.
- **`Styles`** tiene las reglas de estilo. `For element: Document` da la fuente y el tamaño a todo el documento. No hay
  fuente por defecto, así que se declara siempre. Las otras dos reglas dan tamaño a los títulos y separación a los párrafos.
- **`Section`** tiene el contenido. Todo lo que se ve va dentro de una sección.
- **`Heading 1`** es un título y **`Paragraph`**, un párrafo. Un texto corto va en el valor (`Paragraph: Los costes...`) y
  uno largo, en un bloque `Content >>`, donde los saltos de línea simples son espacios.

El motor coloca el título arriba del área que dejan los márgenes y cada párrafo debajo del anterior. Reparte el texto en
líneas del ancho disponible y, si el contenido no cabe en una página, sigue en la siguiente.

## Convertirlo en PDF

El motor es una librería Java. Con el documento en `informe.stxt`:

```java
InlineNode doc = new DocumentReader().read(Path.of("informe.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("informe.pdf"))) {
    Converter.create().toPdf(doc, out);
}
```

Las fuentes y las imágenes se buscan en la carpeta `resources/` del directorio de trabajo ([Recursos](resources.md)). La
del repositorio ya trae IBM Plex Sans, Serif y Mono. Más opciones en [Uso desde Java](java-usage.md).

Si el documento tiene un error, la conversión se para con un mensaje que dice qué falla y en qué línea
([Errores](errors.md)):

```
line 18: Margin auto does not center a box that takes up the whole width: Width or Max width is missing
```

## Qué tiene el PDF

- Las fuentes incrustadas: se ve igual en cualquier ordenador, y el texto se puede seleccionar y buscar.
- Un marcador por encabezado, que el visor muestra en su panel lateral.
- El etiquetado para accesibilidad (PDF/UA-1): el orden de lectura, qué es un título, un párrafo, una lista o una tabla, y
  el texto alternativo de las imágenes.

## Siguientes pasos

- [Conceptos](concepts.md): las ideas que usa el resto del manual.
- [Estilos](styles.md): reglas para no repetir el estilo en cada elemento.
- Los [ejemplos](../../../../test/examples/README.es.md): once documentos reales con su PDF.
