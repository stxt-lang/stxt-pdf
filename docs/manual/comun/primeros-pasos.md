# Primeros pasos

## Un documento completo

```
Document (dev.stxt.pdf):
	Language: es
	Title: Informe del trimestre
	Media:
		Paper: A4
		Margin: 50
	Styles:
		Element: Document
			Typography:
				Font: IBM Plex Sans
				Size: 10
		Element: Heading 1
			Typography:
				Size: 18
				Weight: bold
			Box:
				Margin: 0 0 12 0
		Element: Paragraph
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
- **`Media`** es el papel: un A4 con 50 puntos de margen por los cuatro lados. Todas las medidas están en
  **puntos** (un punto es 1/72 de pulgada, unos 0,35 mm); un A4 mide 595 × 842.
- **`Styles`** tiene las reglas de estilo. `Element: Document` da la fuente y el tamaño a todo el documento: no hay fuente
  por defecto, así que se declara siempre. Las otras dos reglas dan tamaño a los títulos y separación a los párrafos.
- **`Section`** tiene el contenido. Todo lo que se ve va dentro de una sección.
- **`Heading 1`** es un título y **`Paragraph`**, un párrafo. Un texto corto va en el valor (`Paragraph: Los costes…`) y
  uno largo, en un bloque `Content >>`, donde los saltos de línea simples son espacios.

El motor coloca el título arriba del área de la página que dejan los márgenes, cada párrafo debajo del anterior, reparte
el texto en líneas del ancho disponible y, si el contenido no cabe en una página, sigue en la siguiente.

## Convertirlo en PDF

El motor es una librería Java. Con el documento en `informe.stxt`:

```java
InlineNode doc = new DocumentReader().read(Path.of("informe.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("informe.pdf"))) {
    Converter.create().toPdf(doc, out);
}
```

Las fuentes y las imágenes se buscan en la carpeta `resources/` del directorio de trabajo ([Recursos](recursos.md)), que
en el repositorio ya trae IBM Plex Sans, Serif y Mono. Más opciones en [Uso desde Java](uso-desde-java.md).

Si el documento tiene un error, la conversión se para y dice qué y en qué línea: `línea 18: …`
([Errores](errores.md)).

## Qué tiene el PDF

- Las fuentes incrustadas: se ve igual en cualquier ordenador, y el texto se puede seleccionar y buscar.
- Un marcador por encabezado, que el visor muestra en su panel lateral.
- El etiquetado para accesibilidad (PDF/UA-1): el orden de lectura, qué es un título, un párrafo, una lista o una tabla, y
  el texto alternativo de las imágenes.

## Siguientes pasos

- [Conceptos](conceptos.md): cómo piensa el motor.
- [Estilos](estilos.md): dar estilo sin repetirlo en cada elemento.
- Los [ejemplos](../../../test/examples/README.md): once documentos reales con su PDF.
