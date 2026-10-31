# Uso desde Java

El motor es una librería Java 17 (Maven: `dev.stxt:stxt-pdf`).

## Convertir un documento

```java
InlineNode doc = new DocumentReader().read(Path.of("informe.stxt"));
try (OutputStream out = Files.newOutputStream(Path.of("informe.pdf"))) {
    Converter.create().toPdf(doc, out);
}
```

`DocumentReader` lee y valida el documento. `Converter` lo maqueta y escribe el PDF. Un `Converter` se crea una vez y se
puede usar desde varios hilos a la vez.

## Opciones

```java
Converter converter = Converter.builder()
        .resources(new Resources(Path.of("mis-recursos")))   // otra carpeta de recursos
        .styles(Path.of("estilos.stxt"))                      // un fichero de estilos aparte
        .limits(Limits.DEFAULT.withMaxPages(500))             // otros límites
        .build();
converter.toPdf(doc, out);
```

| Opción | Por defecto |
|---|---|
| `resources` | La carpeta `resources/` del directorio de trabajo ([Recursos](recursos.md)) |
| `styles` | Ninguno. El fichero de estilos aparte del documento ([Estilos](estilos.md#estilos-en-un-fichero-aparte)) |
| `limits` | 2000 páginas, 25 millones de píxeles por imagen, 20 000 elementos y 64 niveles por SVG y 20 MB por PDF |
| `debug` | `false`. Con `true`, pinta el contorno de cada caja |
| `pdfUa` | `1`: el PDF cumple PDF/UA-1. Con `2`, PDF/UA-2, en PDF 2.0, que algún visor antiguo no abre |
| `exclude` | Ninguna: se comprueba todo. Quita las comprobaciones que se le dicen (abajo) |

Por defecto se comprueba todo. Con `exclude` se quitan comprobaciones, que dejan de ser un error:

```java
Converter converter = Converter.builder().exclude(Check.CONTRAST).build();
```

| Comprobación | Qué es un error si no se quita |
|---|---|
| `Check.CONTRAST` | Un texto que apenas se distingue de su fondo ([Caja](caja.md#contraste)) |
| `Check.PDF_IMAGE_FONTS` | Una página de un PDF con una fuente sin incrustar o que no dice qué carácter es cada glifo ([Image](../componentes/image.md#una-página-de-un-pdf)) |
| `Check.PDF_IMAGE_ANNOTATIONS` | Una página de un PDF con un campo de formulario o un comentario visibles, que no se dibujarían |

## Errores

Todo error es un `StxtPdfException`, con tres datos: `kind()` (de quién es la culpa), `code()` (el código) y `line()` (la
línea, 0 si no es de una línea). Si la línea es del fichero de estilos aparte, `inStyles()` es `true`.

```java
try {
    converter.toPdf(doc, out);
} catch (StxtPdfException e) {
    switch (e.kind()) {
        case INVALID_DOCUMENT -> System.err.println(e.getMessage());   // el documento tiene un error
        case LIMIT -> System.err.println("Demasiado grande: " + e.getMessage());
        case RESOURCE, INTERNAL -> throw e;                            // la configuración o el motor
    }
}
```

## Más

- Para usar las páginas antes de pintarlas, o escribir también un SVG de cada página: `converter.layOut(doc)` y
  `converter.toSvg(...)`.
- En un servidor sin pantalla, la JVM se arranca con `-Djava.awt.headless=true`.

El detalle (hilos, tipos de error, límites) está en la [referencia](../../referencia/implementacion/api.md).
